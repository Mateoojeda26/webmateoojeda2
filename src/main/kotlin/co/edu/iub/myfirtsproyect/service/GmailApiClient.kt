package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.time.Duration
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

data class GmailTokens(val accessToken: String, val refreshToken: String)

@Component
class GmailApiClient(
    private val objectMapper: ObjectMapper,
    @Value("\${app.gmail.client-id:}") private val clientId: String,
    @Value("\${app.gmail.client-secret:}") private val clientSecret: String,
    @Value("\${app.gmail.redirect-uri:http://localhost:8080/api/notifications/channels/gmail/callback}")
    val redirectUri: String,
) {
    private val httpClient = buildHttpClient()
    private val sendScope = "https://www.googleapis.com/auth/gmail.send"

    fun isConfigured(): Boolean = clientId.isNotBlank() && clientSecret.isNotBlank()

    fun authorizationUrl(state: String): String {
        ensureConfigured()
        val params = linkedMapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "openid email $sendScope",
            "access_type" to "offline",
            "include_granted_scopes" to "true",
            "prompt" to "consent",
            "state" to state,
        )
        return "https://accounts.google.com/o/oauth2/v2/auth?${formBody(params)}"
    }

    fun exchangeCode(code: String): GmailTokens {
        val json = postForm(
            "https://oauth2.googleapis.com/token",
            linkedMapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "code" to code,
                "grant_type" to "authorization_code",
                "redirect_uri" to redirectUri,
            ),
        )
        val accessToken = json.path("access_token").asText()
        val refreshToken = json.path("refresh_token").asText()
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw InvalidRequestException("Google no entregó acceso offline. Revoca el permiso y conecta Gmail nuevamente")
        }
        return GmailTokens(accessToken, refreshToken)
    }

    fun refreshAccessToken(refreshToken: String): String {
        val json = postForm(
            "https://oauth2.googleapis.com/token",
            linkedMapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "refresh_token" to refreshToken,
                "grant_type" to "refresh_token",
            ),
        )
        return json.path("access_token").asText().takeIf { it.isNotBlank() }
            ?: throw InvalidRequestException("No fue posible renovar el acceso a Gmail")
    }

    fun accountEmail(accessToken: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()
        val json = send(request, "No fue posible consultar la cuenta de Google")
        return json.path("email").asText().takeIf { it.isNotBlank() }
            ?: throw InvalidRequestException("Google no devolvió el correo autorizado")
    }

    fun sendEmail(accessToken: String, sender: String, recipient: String, subject: String, body: String) {
        val safeSender = sender.replace(Regex("[\r\n]"), "")
        val safeRecipient = recipient.replace(Regex("[\r\n]"), "")
        val encodedSubject = Base64.getEncoder().encodeToString(subject.toByteArray(Charsets.UTF_8))
        val mime = buildString {
            appendLine("From: Taskora Pet <$safeSender>")
            appendLine("To: $safeRecipient")
            appendLine("Subject: =?UTF-8?B?$encodedSubject?=")
            appendLine("MIME-Version: 1.0")
            appendLine("Content-Type: text/plain; charset=UTF-8")
            appendLine("Content-Transfer-Encoding: 8bit")
            appendLine()
            append(body)
        }
        val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(mime.toByteArray(Charsets.UTF_8))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"raw\":\"$raw\"}"))
            .build()
        send(request, "No fue posible enviar el correo con Gmail")
    }

    private fun postForm(uri: String, values: Map<String, String>): JsonNode {
        ensureConfigured()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody(values)))
            .build()
        return send(request, "Google rechazó la autorización")
    }

    private fun send(request: HttpRequest, fallbackMessage: String): JsonNode {
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InvalidRequestException("La conexión con Google fue interrumpida")
        } catch (ex: Exception) {
            throw InvalidRequestException("No fue posible conectar con Google")
        }
        val json = runCatching { objectMapper.readTree(response.body()) }
            .getOrElse { throw InvalidRequestException(fallbackMessage) }
        if (response.statusCode() !in 200..299) {
            val message = json.path("error").path("message").asText()
                .ifBlank { json.path("error_description").asText() }
                .ifBlank { fallbackMessage }
            throw InvalidRequestException(message.take(240))
        }
        return json
    }

    private fun formBody(values: Map<String, String>): String = values.entries.joinToString("&") { (key, value) ->
        "${encode(key)}=${encode(value)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun ensureConfigured() {
        if (!isConfigured()) {
            throw InvalidRequestException("Configura GMAIL_CLIENT_ID y GMAIL_CLIENT_SECRET para conectar Gmail")
        }
    }

    private fun buildHttpClient(): HttpClient {
        val builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))

        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            runCatching {
                val windowsCertificates = KeyStore.getInstance("Windows-ROOT")
                windowsCertificates.load(null, null)

                val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManager.init(windowsCertificates)

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustManager.trustManagers, null)
                builder.sslContext(sslContext)
            }
        }

        return builder.build()
    }
}
