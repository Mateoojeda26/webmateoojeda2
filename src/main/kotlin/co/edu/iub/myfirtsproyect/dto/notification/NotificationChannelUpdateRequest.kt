package co.edu.iub.myfirtsproyect.dto.notification

data class NotificationChannelUpdateRequest(
    val destination: String? = null,
    val label: String? = null,
    val active: Boolean? = null,
    val verified: Boolean? = null,
    val reminderMinutesBefore: Int? = null,
)
