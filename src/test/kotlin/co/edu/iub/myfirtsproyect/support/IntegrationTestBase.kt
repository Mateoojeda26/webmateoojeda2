package co.edu.iub.myfirtsproyect.support

import co.edu.iub.myfirtsproyect.service.GmailApiClient
import co.edu.iub.myfirtsproyect.service.NotificationMessageService
import co.edu.iub.myfirtsproyect.service.PasswordResetSender
import co.edu.iub.myfirtsproyect.service.TelegramBotClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest(
    properties = [
        "TASKORA_ADMIN_NAME=",
        "TASKORA_ADMIN_EMAIL=",
        "TASKORA_ADMIN_PASSWORD=",
    ],
)
@AutoConfigureMockMvc
abstract class IntegrationTestBase {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var telegramBotClient: TelegramBotClient

    @MockitoBean
    lateinit var gmailApiClient: GmailApiClient

    @MockitoBean
    lateinit var notificationMessageService: NotificationMessageService

    @MockitoBean
    lateinit var passwordResetSender: PasswordResetSender

    protected fun uniqueEmail(prefix: String = "user"): String =
        "$prefix.${UUID.randomUUID().toString().take(10)}@test.local"

    protected fun registerUser(email: String, password: String = "password123", name: String = "Test User"): JsonNode {
        val result = mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","fullName":"$name","password":"$password"}"""),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString)
    }

    protected fun login(email: String, password: String = "password123"): String {
        val result = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}"""),
        ).andExpect(status().isOk).andReturn()
        return objectMapper.readTree(result.response.contentAsString).path("accessToken").asText()
    }

    protected fun registerAndLogin(prefix: String = "user"): Pair<String, String> {
        val email = uniqueEmail(prefix)
        registerUser(email)
        return email to login(email)
    }

    protected fun authGet(path: String, token: String) =
        mockMvc.perform(get(path).header("Authorization", "Bearer $token"))

    protected fun authPost(path: String, token: String, body: String? = null) =
        mockMvc.perform(
            post(path).header("Authorization", "Bearer $token").let {
                if (body != null) it.contentType(MediaType.APPLICATION_JSON).content(body) else it
            },
        )

    protected fun authPut(path: String, token: String, body: String) =
        mockMvc.perform(
            put(path).header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )

    protected fun authDelete(path: String, token: String) =
        mockMvc.perform(delete(path).header("Authorization", "Bearer $token"))

    protected fun createPet(token: String, name: String = "Firulais"): Long {
        val result = authPost("/api/pets", token, """{"name":"$name","species":"Perro"}""")
            .andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).path("id").asLong()
    }

    protected fun createCareTask(
        token: String,
        petId: Long,
        title: String = "Dar comida",
        careType: String = "Alimentación",
        scheduledAt: String = "2026-07-20T08:00:00",
    ): Long {
        val result = authPost(
            "/api/care-tasks",
            token,
            """{"petId":$petId,"title":"$title","careType":"$careType","scheduledAt":"$scheduledAt"}""",
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).path("id").asLong()
    }

    protected fun readJson(result: org.springframework.test.web.servlet.ResultActions): JsonNode =
        objectMapper.readTree(result.andReturn().response.contentAsString)
}
