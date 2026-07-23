package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.model.CareTask
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.NotificationDelivery
import co.edu.iub.myfirtsproyect.model.NotificationDeliveryStatus
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.NotificationDeliveryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class ReminderCancellationService(
    private val taskRepository: CareTaskRepository,
    private val channelRepository: NotificationChannelRepository,
    private val deliveryRepository: NotificationDeliveryRepository,
    private val clock: Clock,
) {
    @Transactional
    fun cancelTask(task: CareTask, reason: String) {
        val ownerId = requireNotNull(task.pet.owner.id)
        channelRepository.findAllByOwnerIdAndVerifiedTrueAndActiveTrue(ownerId)
            .filter { it.supportsAutomaticDelivery() }
            .forEach { channel -> recordCancellation(task, channel, reason) }
    }

    @Transactional
    fun cancelChannel(channel: NotificationChannel, reason: String) {
        val ownerId = requireNotNull(channel.owner.id)
        taskRepository.findAllByPetOwnerIdAndStatusAndActiveTrue(ownerId, CareTaskStatus.PENDING)
            .forEach { task -> recordCancellation(task, channel, reason) }
    }

    private fun recordCancellation(task: CareTask, channel: NotificationChannel, reason: String) {
        if (!channel.supportsAutomaticDelivery()) return
        val taskId = requireNotNull(task.id)
        val channelId = requireNotNull(channel.id)
        val leadMinutes = (channel.reminderMinutesBefore ?: 10).coerceIn(0, 1440)
        val scheduledFor = task.scheduledAt.minusMinutes(leadMinutes.toLong())
        val handled = deliveryRepository.existsByCareTaskIdAndChannelIdAndScheduledForAndStatusIn(
            taskId,
            channelId,
            scheduledFor,
            listOf(
                NotificationDeliveryStatus.SENT,
                NotificationDeliveryStatus.DISCARDED,
                NotificationDeliveryStatus.CANCELLED,
            ),
        )
        if (handled) return
        deliveryRepository.save(
            NotificationDelivery(
                careTask = task,
                channel = channel,
                scheduledFor = scheduledFor,
                status = NotificationDeliveryStatus.CANCELLED,
                errorMessage = reason.take(500),
                createdAt = LocalDateTime.now(clock),
            ),
        )
    }

    private fun NotificationChannel.supportsAutomaticDelivery() =
        type == NotificationChannelType.TELEGRAM || type == NotificationChannelType.EMAIL
}
