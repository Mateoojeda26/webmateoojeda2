package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.TelegramLinkRequest
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.TelegramLinkRequestRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

class TelegramLinkServiceTests {
    @Test
    fun `confirmation succeeds when polling already linked the same account`() {
        val botClient = mock(TelegramBotClient::class.java)
        val linkRepository = mock(TelegramLinkRequestRepository::class.java)
        val channelRepository = mock(NotificationChannelRepository::class.java)
        val userRepository = mock(UserRepository::class.java)
        val owner = User(
            id = 4,
            email = "mateo@example.com",
            fullName = "Mateo",
            passwordHash = "hash",
        )
        val request = TelegramLinkRequest(
            code = "ABC12345",
            owner = owner,
            expiresAt = LocalDateTime.now().plusMinutes(10),
            usedAt = LocalDateTime.now(),
        )
        val channel = NotificationChannel(
            id = 10,
            type = NotificationChannelType.TELEGRAM,
            destination = "123",
            verified = true,
            active = true,
            owner = owner,
        )
        `when`(userRepository.findByEmail(owner.email)).thenReturn(owner)
        `when`(linkRepository.findByCode("ABC12345")).thenReturn(request)
        `when`(
            channelRepository.findFirstByOwnerIdAndType(4, NotificationChannelType.TELEGRAM),
        ).thenReturn(channel)
        val service = TelegramLinkService(botClient, linkRepository, channelRepository, userRepository)

        service.confirmLink(owner.email, "abc12345")

        verifyNoInteractions(botClient)
    }
}
