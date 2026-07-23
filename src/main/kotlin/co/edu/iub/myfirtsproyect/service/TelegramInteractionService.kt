package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.care.CareTaskUpdateRequest
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
import java.time.format.DateTimeFormatter

@Service
class TelegramInteractionService(
    private val telegramBotClient: TelegramBotClient,
    private val telegramLinkService: TelegramLinkService,
    private val channelRepository: NotificationChannelRepository,
    private val careTaskRepository: CareTaskRepository,
    private val careTaskService: CareTaskService,
    private val deliveryRepository: NotificationDeliveryRepository,
    private val notificationMessageService: NotificationMessageService,
    private val clock: Clock,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")

    @Transactional
    fun handle(incoming: TelegramIncomingMessage) {
        if (incoming.callbackQueryId != null) {
            handleButton(incoming)
        } else {
            handleCommand(incoming)
        }
    }

    @Transactional
    fun sendDueSnoozes() {
        val now = LocalDateTime.now(clock)
        val pending = deliveryRepository.findAllByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(
            NotificationDeliveryStatus.PENDING,
            now,
        )
        pending.forEach { delivery ->
            val task = delivery.careTask
            val channel = delivery.channel
            if (!task.active || task.status != CareTaskStatus.PENDING || !channel.active || !channel.verified) {
                delivery.status = NotificationDeliveryStatus.CANCELLED
                delivery.errorMessage = "El cuidado o el canal ya no está disponible"
                deliveryRepository.save(delivery)
                return@forEach
            }
            try {
                val taskId = requireNotNull(task.id)
                val body = buildString {
                    appendLine("🐾 Recordatorio pospuesto de Taskora Pet")
                    appendLine()
                    appendLine("${task.pet.name}: ${task.title}")
                    append("Programado: ${task.scheduledAt.format(dateFormatter)}")
                }
                notificationMessageService.sendCareReminder(
                    channel,
                    "Recordatorio: ${task.pet.name} · ${task.title}",
                    body,
                    taskId,
                )
                delivery.status = NotificationDeliveryStatus.SENT
                delivery.sentAt = now
                delivery.errorMessage = null
            } catch (ex: Exception) {
                delivery.status = NotificationDeliveryStatus.FAILED
                delivery.errorMessage = (ex.message ?: "No fue posible enviar el recordatorio pospuesto").take(500)
            }
            deliveryRepository.save(delivery)
        }
    }

    private fun handleCommand(incoming: TelegramIncomingMessage) {
        val cleanText = incoming.text.trim()
        val command = cleanText.substringBefore(' ').substringBefore('@').lowercase()
        when (command) {
            "/start" -> handleStart(incoming, cleanText.substringAfter(' ', "").trim())
            "/help" -> telegramBotClient.sendMessage(incoming.chatId, helpMessage())
            "/estado" -> sendStatus(incoming.chatId)
            "/proximos" -> sendUpcoming(incoming.chatId)
            else -> telegramBotClient.sendMessage(
                incoming.chatId,
                "No conozco ese comando todavía.\n\n${helpMessage()}",
            )
        }
    }

    private fun handleStart(incoming: TelegramIncomingMessage, code: String) {
        if (code.isBlank()) {
            telegramBotClient.sendMessage(
                incoming.chatId,
                "🐾 ¡Hola! Soy Taskora Pet.\n\n${helpMessage()}",
            )
            return
        }
        val linked = telegramLinkService.claim(code, incoming.chatId, incoming.displayName)
        if (linked) {
            telegramBotClient.sendMessage(
                incoming.chatId,
                "✅ Telegram quedó vinculado con Taskora Pet. Recibirás aquí los recordatorios de cuidado.",
            )
        } else {
            telegramBotClient.sendMessage(
                incoming.chatId,
                "El código no existe, ya fue usado o venció. Genera uno nuevo desde Taskora Pet.",
            )
        }
    }

    private fun sendStatus(chatId: String) {
        val channel = linkedChannel(chatId)
        val message = if (channel == null) {
            "⚠️ Este chat todavía no está vinculado. Abre Taskora Pet y usa el botón Vincular Telegram."
        } else {
            "✅ Tu cuenta está vinculada y lista para recibir recordatorios por Telegram."
        }
        telegramBotClient.sendMessage(chatId, message)
    }

    private fun sendUpcoming(chatId: String) {
        val channel = linkedChannel(chatId)
        if (channel == null) {
            telegramBotClient.sendMessage(
                chatId,
                "Primero vincula este chat desde la sección Canales de Taskora Pet.",
            )
            return
        }
        val now = LocalDateTime.now(clock)
        val ownerId = requireNotNull(channel.owner.id)
        val tasks = careTaskRepository.findAllByPetOwnerIdAndStatusAndActiveTrue(
            ownerId,
            CareTaskStatus.PENDING,
        ).filter { !it.scheduledAt.isBefore(now) }
            .sortedBy { it.scheduledAt }
            .take(5)
        if (tasks.isEmpty()) {
            telegramBotClient.sendMessage(chatId, "🎉 No tienes cuidados próximos pendientes.")
            return
        }
        val message = buildString {
            appendLine("🐾 Tus próximos cuidados")
            appendLine()
            tasks.forEachIndexed { index, task ->
                appendLine("${index + 1}. ${task.pet.name}: ${task.title}")
                appendLine("   ${task.scheduledAt.format(dateFormatter)}")
            }
        }.trim()
        telegramBotClient.sendMessage(chatId, message)
    }

    private fun handleButton(incoming: TelegramIncomingMessage) {
        val callbackId = requireNotNull(incoming.callbackQueryId)
        val action = incoming.text.substringBefore(':')
        val taskId = incoming.text.substringAfter(':', "").toLongOrNull()
        val channel = linkedChannel(incoming.chatId)
        if (taskId == null || channel == null) {
            telegramBotClient.answerCallbackQuery(callbackId, "Este botón ya no está disponible")
            return
        }
        val ownerId = requireNotNull(channel.owner.id)
        val task = careTaskRepository.findByIdAndPetOwnerId(taskId, ownerId)
        if (task == null || !task.active || task.status != CareTaskStatus.PENDING) {
            telegramBotClient.answerCallbackQuery(callbackId, "Este cuidado ya fue atendido")
            return
        }
        when (action) {
            "done" -> markCompleted(callbackId, channel, taskId)
            "later" -> snooze(callbackId, channel, taskId)
            else -> telegramBotClient.answerCallbackQuery(callbackId, "Acción no reconocida")
        }
    }

    private fun markCompleted(callbackId: String, channel: NotificationChannel, taskId: Long) {
        careTaskService.update(
            taskId,
            channel.owner.email,
            CareTaskUpdateRequest(
                status = CareTaskStatus.COMPLETED,
                reason = "Marcado como realizado desde Telegram",
            ),
        )
        cancelPendingSnoozes(taskId, requireNotNull(channel.id))
        telegramBotClient.answerCallbackQuery(callbackId, "Cuidado marcado como realizado")
        telegramBotClient.sendMessage(channel.destination, "✅ Cuidado marcado como realizado en Taskora Pet.")
    }

    private fun snooze(callbackId: String, channel: NotificationChannel, taskId: Long) {
        val channelId = requireNotNull(channel.id)
        val alreadyPending = deliveryRepository.existsByCareTaskIdAndChannelIdAndStatus(
            taskId,
            channelId,
            NotificationDeliveryStatus.PENDING,
        )
        if (!alreadyPending) {
            val task = requireNotNull(careTaskRepository.findByIdAndPetOwnerId(taskId, requireNotNull(channel.owner.id)))
            deliveryRepository.save(
                NotificationDelivery(
                    careTask = task,
                    channel = channel,
                    scheduledFor = LocalDateTime.now(clock).plusMinutes(30),
                    status = NotificationDeliveryStatus.PENDING,
                ),
            )
        }
        telegramBotClient.answerCallbackQuery(callbackId, "Te recordaré en 30 minutos")
        val message = if (alreadyPending) {
            "⏰ Ya tenías un recordatorio pendiente para este cuidado."
        } else {
            "⏰ Listo. Te recordaré este cuidado dentro de 30 minutos."
        }
        telegramBotClient.sendMessage(channel.destination, message)
    }

    private fun cancelPendingSnoozes(taskId: Long, channelId: Long) {
        deliveryRepository.findAllByCareTaskIdAndChannelIdAndStatus(
            taskId,
            channelId,
            NotificationDeliveryStatus.PENDING,
        ).forEach { delivery ->
            delivery.status = NotificationDeliveryStatus.CANCELLED
            delivery.errorMessage = "Cancelado porque el cuidado fue realizado"
            deliveryRepository.save(delivery)
        }
    }

    private fun linkedChannel(chatId: String): NotificationChannel? =
        channelRepository.findByTypeAndDestination(NotificationChannelType.TELEGRAM, chatId)
            ?.takeIf { it.active && it.verified }

    private fun helpMessage(): String = """
        Comandos disponibles:
        /estado - Ver si tu cuenta está vinculada
        /proximos - Consultar los próximos cuidados
        /help - Mostrar esta ayuda
    """.trimIndent()
}
