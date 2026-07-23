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
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

data class TelegramIncomingMessage(
    val updateId: Long,
    val chatId: String,
    val text: String,
    val displayName: String,
    val callbackQueryId: String? = null,
)

data class TelegramInlineButton(
    val text: String,
    val callbackData: String,
)

@Component
class TelegramBotClient(
    private val objectMapper: ObjectMapper,
    @Value("\${app.telegram.bot-token:}") private val botToken: String,
    @Value("\${app.telegram.bot-username:}") private val configuredBotUsername: String,
) {
    private val httpClient = buildHttpClient()

    @Volatile
    private var cachedBotUsername: String? = null

    fun isConfigured(): Boolean = botToken.isNotBlank()

    fun botUsername(): String {
        ensureConfigured()
        configuredBotUsername.trim().removePrefix("@").takeIf { it.isNotBlank() }?.let { return it }
        cachedBotUsername?.let { return it }
        val result = request("getMe")
        return result.path("result").path("username").asText().takeIf { it.isNotBlank() }
            ?.also { cachedBotUsername = it }
            ?: throw InvalidRequestException("Telegram no devolvió el nombre del bot")
    }

    fun sendMessage(chatId: String, message: String) {
        sendMessage(chatId, message, emptyList())
    }

    fun sendMessage(chatId: String, message: String, buttons: List<List<TelegramInlineButton>>) {
        ensureConfigured()
        val values = mutableMapOf("chat_id" to chatId, "text" to message)
        if (buttons.isNotEmpty()) {
            values["reply_markup"] = objectMapper.writeValueAsString(
                mapOf(
                    "inline_keyboard" to buttons.map { row ->
                        row.map { button ->
                            mapOf("text" to button.text, "callback_data" to button.callbackData)
                        }
                    },
                ),
            )
        }
        request(
            method = "sendMessage",
            body = formBody(values),
        )
    }

    fun answerCallbackQuery(callbackQueryId: String, message: String) {
        ensureConfigured()
        request(
            method = "answerCallbackQuery",
            body = formBody(
                mapOf(
                    "callback_query_id" to callbackQueryId,
                    "text" to message.take(180),
                ),
            ),
        )
    }

    fun getUpdates(offset: Long): List<TelegramIncomingMessage> {
        ensureConfigured()
        val result = request("getUpdates?offset=$offset&timeout=0")
        return result.path("result").mapNotNull { update ->
            val callback = update.path("callback_query")
            val isCallback = !callback.isMissingNode && !callback.isNull
            val message = if (isCallback) callback.path("message") else update.path("message")
            val text = if (isCallback) callback.path("data").asText() else message.path("text").asText()
            val chat = message.path("chat")
            if (text.isBlank() || chat.path("id").isMissingNode) return@mapNotNull null
            val from = if (isCallback) callback.path("from") else message.path("from")
            val firstName = from.path("first_name").asText()
            val username = from.path("username").asText()
            TelegramIncomingMessage(
                updateId = update.path("update_id").asLong(),
                chatId = chat.path("id").asText(),
                text = text,
                displayName = when {
                    username.isNotBlank() -> "@$username"
                    firstName.isNotBlank() -> firstName
                    else -> "Telegram"
                },
                callbackQueryId = callback.path("id").asText().takeIf { isCallback && it.isNotBlank() },
            )
        }
    }

    private fun request(method: String, body: String? = null): JsonNode {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("https://api.telegram.org/bot$botToken/$method"))
            .timeout(Duration.ofSeconds(15))
        if (body == null) {
            requestBuilder.GET()
        } else {
            requestBuilder.header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
        }
        val response = try {
            httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InvalidRequestException("La conexión con Telegram fue interrumpida")
        } catch (ex: Exception) {
            throw InvalidRequestException("No fue posible conectar con Telegram")
        }
        val json = runCatching { objectMapper.readTree(response.body()) }
            .getOrElse { throw InvalidRequestException("Telegram devolvió una respuesta inválida") }
        if (response.statusCode() !in 200..299 || !json.path("ok").asBoolean()) {
            val description = json.path("description").asText().ifBlank { "Error de Telegram" }
            throw InvalidRequestException(description.take(240))
        }
        return json
    }

    private fun formBody(values: Map<String, String>): String = values.entries.joinToString("&") { (key, value) ->
        "${encode(key)}=${encode(value)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun ensureConfigured() {
        if (!isConfigured()) {
            throw InvalidRequestException("Configura TELEGRAM_BOT_TOKEN para activar Telegram")
        }
    }

    private fun buildHttpClient(): HttpClient {
        val builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))

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
