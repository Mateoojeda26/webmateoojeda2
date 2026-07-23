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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(prefix = "app.reminders", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ReminderDispatchService(
    private val notificationMessageService: NotificationMessageService,
    private val careTaskRepository: CareTaskRepository,
    private val channelRepository: NotificationChannelRepository,
    private val deliveryRepository: NotificationDeliveryRepository,
    private val clock: Clock,
    @param:Value("\${app.reminders.late-window-minutes:120}") private val lateWindowMinutes: Long,
) {
    private val logger = LoggerFactory.getLogger(ReminderDispatchService::class.java)
    private val timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")

    @Scheduled(fixedDelayString = "\${app.reminders.delay-ms:60000}")
    @Transactional
    fun sendDueReminders() {
        val now = LocalDateTime.now(clock)
        val tasks = careTaskRepository.findAllByStatusAndActiveTrueAndScheduledAtBetweenOrderByScheduledAtAsc(
            CareTaskStatus.PENDING,
            now.minusHours(24),
            now.plusHours(24).plusMinutes(1),
        )
        tasks.forEach { sendTaskReminder(it, now) }
    }

    private fun sendTaskReminder(task: CareTask, now: LocalDateTime) {
        val ownerId = requireNotNull(task.pet.owner.id)
        val channels = channelRepository.findAllByOwnerIdAndVerifiedTrueAndActiveTrue(ownerId)
            .filter { it.type == NotificationChannelType.TELEGRAM || it.type == NotificationChannelType.EMAIL }
        channels.forEach { channel ->
            val leadMinutes = (channel.reminderMinutesBefore ?: 10).coerceIn(0, 1440)
            val notificationAt = task.scheduledAt.minusMinutes(leadMinutes.toLong())
            if (notificationAt.isAfter(now)) return@forEach
            val taskId = requireNotNull(task.id)
            val channelId = requireNotNull(channel.id)
            val alreadyHandled = deliveryRepository.existsByCareTaskIdAndChannelIdAndScheduledForAndStatusIn(
                taskId,
                channelId,
                notificationAt,
                listOf(
                    NotificationDeliveryStatus.SENT,
                    NotificationDeliveryStatus.DISCARDED,
                    NotificationDeliveryStatus.CANCELLED,
                ),
            )
            val failedAttempts = deliveryRepository.countByCareTaskIdAndChannelIdAndScheduledForAndStatus(
                taskId,
                channelId,
                notificationAt,
                NotificationDeliveryStatus.FAILED,
            )
            if (alreadyHandled || failedAttempts >= 3) return@forEach
            if (notificationAt.isBefore(now.minusMinutes(lateWindowMinutes))) {
                deliveryRepository.save(
                    NotificationDelivery(
                        careTask = task,
                        channel = channel,
                        scheduledFor = notificationAt,
                        status = NotificationDeliveryStatus.DISCARDED,
                        errorMessage = "Descartado: fuera de la ventana de recuperación de $lateWindowMinutes minutos",
                    ),
                )
                return@forEach
            }
            try {
                notificationMessageService.sendCareReminder(
                    channel,
                    "Recordatorio: ${task.pet.name} · ${task.title}",
                    buildMessage(task, now),
                    taskId,
                )
                deliveryRepository.save(
                    NotificationDelivery(
                        careTask = task,
                        channel = channel,
                        scheduledFor = notificationAt,
                        status = NotificationDeliveryStatus.SENT,
                        sentAt = LocalDateTime.now(clock),
                    ),
                )
            } catch (ex: Exception) {
                logger.warn("Reminder could not be sent for care task {} through {}", task.id, channel.type)
                deliveryRepository.save(
                    NotificationDelivery(
                        careTask = task,
                        channel = channel,
                        scheduledFor = notificationAt,
                        status = NotificationDeliveryStatus.FAILED,
                        errorMessage = (ex.message ?: "Error de envío").take(500),
                    ),
                )
            }
        }
    }

    internal fun buildMessage(task: CareTask, now: LocalDateTime): String = buildString {
        appendLine("🐾 Recordatorio de Taskora Pet")
        appendLine()
        appendLine("${task.pet.name}: ${task.title}")
        appendLine("Tipo: ${task.careType}")
        append("Programado: ${task.scheduledAt.format(timeFormatter)}")
        append("\n${timingLine(task.scheduledAt, now)}")
        task.description?.takeIf { it.isNotBlank() }?.let { append("\nNota: $it") }
    }

    internal fun timingLine(scheduledAt: LocalDateTime, now: LocalDateTime): String {
        val minutes = Duration.between(now, scheduledAt).toMinutes()
        return when {
            minutes > 1 -> {
                val leadingQuantity = if (minutes >= 60) minutes / 60 else minutes
                val verb = if (leadingQuantity == 1L) "falta" else "faltan"
                "Aviso: $verb ${humanize(minutes)}"
            }
            minutes >= -1 -> "Aviso: el cuidado corresponde ahora"
            else -> "Aviso: estaba programado hace ${humanize(-minutes)}"
        }
    }

    private fun humanize(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours <= 0 -> "$minutes ${if (minutes == 1L) "minuto" else "minutos"}"
            minutes == 0L -> "$hours ${if (hours == 1L) "hora" else "horas"}"
            else -> "$hours ${if (hours == 1L) "hora" else "horas"} y $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
        }
    }
}
