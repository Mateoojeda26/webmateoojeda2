package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.notification.TelegramLinkResponse
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.TelegramLinkRequest
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.TelegramLinkRequestRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class TelegramLinkService(
    private val telegramBotClient: TelegramBotClient,
    private val linkRepository: TelegramLinkRequestRepository,
    private val channelRepository: NotificationChannelRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createLink(ownerEmail: String): TelegramLinkResponse {
        val owner = userRepository.findByEmail(ownerEmail)
            ?: throw InvalidCredentialsException("User not found")
        val botUsername = telegramBotClient.botUsername()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        val request = linkRepository.save(
            TelegramLinkRequest(
                code = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(),
                owner = owner,
                expiresAt = expiresAt,
            ),
        )
        return TelegramLinkResponse(
            code = request.code,
            botUrl = "https://t.me/$botUsername?start=${request.code}",
            webBotUrl = "https://web.telegram.org/k/#@$botUsername",
            startCommand = "/start ${request.code}",
            expiresAt = expiresAt,
        )
    }

    @Transactional
    fun confirmLink(ownerEmail: String, code: String) {
        val owner = userRepository.findByEmail(ownerEmail)
            ?: throw InvalidCredentialsException("User not found")
        val cleanCode = code.trim().uppercase()
        val request = linkRepository.findByCode(cleanCode)
            ?: throw InvalidRequestException("El código no existe. Genera uno nuevo")
        if (request.owner.id != owner.id) {
            throw InvalidRequestException("El código no pertenece a tu cuenta")
        }
        if (request.usedAt != null) {
            val channel = channelRepository.findFirstByOwnerIdAndType(
                requireNotNull(owner.id),
                NotificationChannelType.TELEGRAM,
            )
            if (channel?.verified == true && channel.active) return
            throw InvalidRequestException("El código ya fue usado. Genera uno nuevo")
        }
        if (request.expiresAt.isBefore(LocalDateTime.now())) {
            throw InvalidRequestException("El código venció. Genera uno nuevo")
        }

        val incoming = telegramBotClient.getUpdates(0)
            .lastOrNull { message -> extractStartCode(message.text) == cleanCode }
            ?: throw InvalidRequestException("Aún no encontramos el comando en Telegram. Envíalo al bot y vuelve a intentar")

        if (!claim(cleanCode, incoming.chatId, incoming.displayName)) {
            throw InvalidRequestException("No fue posible vincular este chat. Genera un código nuevo")
        }
        // Confirm only the update that was persisted successfully.
        telegramBotClient.getUpdates(incoming.updateId + 1)
        runCatching {
            telegramBotClient.sendMessage(
                incoming.chatId,
                "🐾 Telegram quedó vinculado con Taskora Pet. Recibirás aquí los recordatorios de cuidado.",
            )
        }
    }

    @Transactional
    fun claim(code: String, chatId: String, displayName: String): Boolean {
        val request = linkRepository.findByCodeAndUsedAtIsNull(code.uppercase()) ?: return false
        if (request.expiresAt.isBefore(LocalDateTime.now())) return false
        val ownerId = requireNotNull(request.owner.id)
        val linkedElsewhere = channelRepository.findByTypeAndDestination(NotificationChannelType.TELEGRAM, chatId)
        if (linkedElsewhere != null && linkedElsewhere.owner.id != ownerId) return false

        val channel = channelRepository.findFirstByOwnerIdAndType(ownerId, NotificationChannelType.TELEGRAM)
            ?: NotificationChannel(
                type = NotificationChannelType.TELEGRAM,
                destination = chatId,
                owner = request.owner,
            )
        channel.destination = chatId
        channel.label = displayName.take(100)
        channel.verified = true
        channel.active = true
        channel.updatedAt = LocalDateTime.now()
        channelRepository.save(channel)
        request.usedAt = LocalDateTime.now()
        linkRepository.save(request)
        return true
    }

    private fun extractStartCode(text: String): String =
        text.substringAfter("/start", "").trim().substringBefore(' ').uppercase()
}
