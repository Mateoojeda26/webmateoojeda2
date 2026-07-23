package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import co.edu.iub.myfirtsproyect.support.anyChannel
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals

class NotificationChannelTests : IntegrationTestBase() {
    @Autowired
    lateinit var channelRepository: NotificationChannelRepository

    private fun createEmailChannel(token: String, destination: String = "correo@test.local"): Long {
        val result = authPost(
            "/api/notifications/channels",
            token,
            """{"type":"EMAIL","destination":"$destination","label":"Correo personal"}""",
        ).andExpect(status().isCreated)
        return readJson(result).path("id").asLong()
    }

    @Test
    fun `creates, updates, configures and deactivates a channel`() {
        val (_, token) = registerAndLogin("ch")
        val channelId = createEmailChannel(token)

        authGet("/api/notifications/channels", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].type").value("EMAIL"))

        authPut("/api/notifications/channels/$channelId", token, """{"reminderMinutesBefore":30,"label":"Principal"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reminderMinutesBefore").value(30))
            .andExpect(jsonPath("$.label").value("Principal"))

        authPut("/api/notifications/channels/$channelId", token, """{"reminderMinutesBefore":2000}""")
            .andExpect(status().isBadRequest)

        authDelete("/api/notifications/channels/$channelId", token).andExpect(status().isNoContent)
        val list = readJson(authGet("/api/notifications/channels", token).andExpect(status().isOk))
        assertEquals(0, list.count { it.path("id").asLong() == channelId })
    }

    @Test
    fun `sends a test message through the messaging adapter without touching real services`() {
        val (_, token) = registerAndLogin("chtest")
        val channelId = createEmailChannel(token)
        val channel = channelRepository.findById(channelId).orElseThrow()
        channel.verified = true
        channelRepository.save(channel)

        authPost("/api/notifications/channels/$channelId/test", token).andExpect(status().isOk)
        verify(notificationMessageService).send(anyChannel(), anyString(), anyString())
    }

    @Test
    fun `rejects test messages on unverified channels`() {
        val (_, token) = registerAndLogin("chunv")
        val channelId = createEmailChannel(token)
        authPost("/api/notifications/channels/$channelId/test", token).andExpect(status().isBadRequest)
    }

    @Test
    fun `telegram channels can only be linked through the secure flow`() {
        val (_, token) = registerAndLogin("chtg")
        authPost("/api/notifications/channels", token, """{"type":"TELEGRAM","destination":"12345"}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `channels are isolated between users`() {
        val (_, ownerToken) = registerAndLogin("chiso")
        val (_, otherToken) = registerAndLogin("chiso2")
        val channelId = createEmailChannel(ownerToken)
        authPut("/api/notifications/channels/$channelId", otherToken, """{"label":"Ajeno"}""")
            .andExpect(status().isNotFound)
        authDelete("/api/notifications/channels/$channelId", otherToken).andExpect(status().isNotFound)
    }
}
