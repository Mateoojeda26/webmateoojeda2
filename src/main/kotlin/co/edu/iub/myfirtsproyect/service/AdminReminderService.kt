package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.admin.AdminReminderResponse
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.NotificationDelivery
import co.edu.iub.myfirtsproyect.model.NotificationDeliveryStatus
import co.edu.iub.myfirtsproyect.repository.NotificationDeliveryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminReminderService(
    private val deliveryRepository: NotificationDeliveryRepository,
    private val notificationMessageService: NotificationMessageService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun listFailed(): List<AdminReminderResponse> =
        deliveryRepository.findAllByStatusOrderByCreatedAtDesc(NotificationDeliveryStatus.FAILED)
            .map { it.toResponse() }

    @Transactional
    fun retry(id: Long): AdminReminderResponse {
        val delivery = deliveryRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Recordatorio fallido no encontrado")
        }
        if (delivery.status != NotificationDeliveryStatus.FAILED) {
            throw InvalidRequestException("Solo se pueden reintentar recordatorios fallidos")
        }
        val task = delivery.careTask
        val channel = delivery.channel
        if (!task.active || task.status != CareTaskStatus.PENDING) {
            throw InvalidRequestException("El cuidado ya no está pendiente")
        }
        if (!channel.active || !channel.verified || channel.type == NotificationChannelType.WHATSAPP) {
            throw InvalidRequestException("El canal ya no está conectado y activo")
        }

        notificationMessageService.send(
            channel,
            "Recordatorio: ${task.pet.name} · ${task.title}",
            "🐾 ${task.pet.name}: ${task.title}\nProgramado para ${task.scheduledAt}",
        )
        delivery.status = NotificationDeliveryStatus.SENT
        delivery.sentAt = LocalDateTime.now(clock)
        delivery.errorMessage = null
        return deliveryRepository.save(delivery).toResponse()
    }

    private fun NotificationDelivery.toResponse() = AdminReminderResponse(
        id = requireNotNull(id),
        userEmail = careTask.pet.owner.email,
        petName = careTask.pet.name,
        taskTitle = careTask.title,
        channelType = channel.type.name,
        scheduledFor = scheduledFor,
        errorMessage = errorMessage,
        createdAt = createdAt,
    )
}
