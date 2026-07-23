package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelCreateRequest
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelResponse
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelUpdateRequest
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationChannelService(
    private val channelRepository: NotificationChannelRepository,
    private val userRepository: UserRepository,
    private val notificationMessageService: NotificationMessageService,
    private val reminderCancellationService: ReminderCancellationService,
) {
    @Transactional(readOnly = true)
    fun list(ownerEmail: String): List<NotificationChannelResponse> {
        val owner = findOwner(ownerEmail)
        return channelRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(requireNotNull(owner.id))
            .map { it.toResponse() }
    }

    @Transactional
    fun create(ownerEmail: String, request: NotificationChannelCreateRequest): NotificationChannelResponse {
        if (request.type == NotificationChannelType.TELEGRAM) {
            throw InvalidRequestException("Vincula Telegram con el botón de conexión segura")
        }
        val owner = findOwner(ownerEmail)
        val channel = NotificationChannel(
            type = request.type,
            destination = request.destination.trim(),
            label = request.label?.trim()?.ifBlank { null },
            owner = owner,
            verified = false,
        )
        return channelRepository.save(channel).toResponse()
    }

    @Transactional
    fun update(id: Long, ownerEmail: String, request: NotificationChannelUpdateRequest): NotificationChannelResponse {
        val channel = findChannel(id, ownerEmail)
        request.destination?.let {
            if (channel.type == NotificationChannelType.TELEGRAM) {
                throw InvalidRequestException("El destino de Telegram no se modifica manualmente")
            }
            channel.destination = it.trim()
            channel.verified = false
        }
        request.label?.let { channel.label = it.trim().ifBlank { null } }
        request.active?.let { channel.active = it }
        request.reminderMinutesBefore?.let {
            if (it !in 0..1440) throw InvalidRequestException("El aviso debe estar entre 0 y 1440 minutos")
            channel.reminderMinutesBefore = it
        }
        channel.updatedAt = LocalDateTime.now()
        return channelRepository.save(channel).toResponse()
    }

    @Transactional(readOnly = true)
    fun sendTest(id: Long, ownerEmail: String) {
        val channel = findChannel(id, ownerEmail)
        if (channel.type == NotificationChannelType.WHATSAPP || !channel.verified || !channel.active) {
            throw InvalidRequestException("El canal no está activo o verificado")
        }
        notificationMessageService.send(
            channel,
            "Prueba de recordatorios de Taskora Pet",
            "🐾 Mensaje de prueba de Taskora Pet. ¡Tu canal de recordatorios funciona correctamente!",
        )
    }

    @Transactional
    fun delete(id: Long, ownerEmail: String) {
        val channel = findChannel(id, ownerEmail)
        reminderCancellationService.cancelChannel(channel, "Cancelado porque el canal fue desactivado")
        channel.active = false
        channel.updatedAt = LocalDateTime.now()
        channelRepository.save(channel)
    }

    private fun findOwner(email: String) = userRepository.findByEmail(email)
        ?: throw InvalidCredentialsException("User not found")

    private fun findChannel(id: Long, ownerEmail: String): NotificationChannel {
        val owner = findOwner(ownerEmail)
        return channelRepository.findByIdAndOwnerId(id, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Notification channel not found")
    }

    private fun NotificationChannel.toResponse() = NotificationChannelResponse(
        id = requireNotNull(id),
        type = type,
        destination = if (type == NotificationChannelType.TELEGRAM) "Telegram conectado" else destination,
        label = label,
        verified = verified,
        active = active,
        reminderMinutesBefore = reminderMinutesBefore ?: 10,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
