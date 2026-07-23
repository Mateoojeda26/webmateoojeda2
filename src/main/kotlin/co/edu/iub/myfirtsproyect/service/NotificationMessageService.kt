package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import org.springframework.stereotype.Service

@Service
class NotificationMessageService(
    private val telegramBotClient: TelegramBotClient,
    private val gmailOAuthService: GmailOAuthService,
) {
    fun send(channel: NotificationChannel, subject: String, body: String) {
        when (channel.type) {
            NotificationChannelType.TELEGRAM -> telegramBotClient.sendMessage(channel.destination, body)
            NotificationChannelType.EMAIL -> gmailOAuthService.send(
                requireNotNull(channel.owner.id),
                channel.destination,
                subject,
                body,
            )
            NotificationChannelType.WHATSAPP ->
                throw InvalidRequestException("WhatsApp está configurado como canal manual")
        }
    }

    fun sendCareReminder(channel: NotificationChannel, subject: String, body: String, careTaskId: Long) {
        if (channel.type == NotificationChannelType.TELEGRAM) {
            telegramBotClient.sendMessage(
                channel.destination,
                body,
                listOf(
                    listOf(
                        TelegramInlineButton("✅ Marcar realizado", "done:$careTaskId"),
                        TelegramInlineButton("⏰ Recordar en 30 min", "later:$careTaskId"),
                    ),
                ),
            )
            return
        }
        send(channel, subject, body)
    }
}
