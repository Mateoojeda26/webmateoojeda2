package co.edu.iub.myfirtsproyect.dto.notification

import java.time.LocalDateTime

data class TelegramLinkResponse(
    val code: String,
    val botUrl: String,
    val webBotUrl: String,
    val startCommand: String,
    val expiresAt: LocalDateTime,
)

data class NotificationTestResponse(val message: String)
