package co.edu.iub.myfirtsproyect.dto.notification

import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import java.time.LocalDateTime

data class NotificationChannelResponse(
    val id: Long,
    val type: NotificationChannelType,
    val destination: String,
    val label: String?,
    val verified: Boolean,
    val active: Boolean,
    val reminderMinutesBefore: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
