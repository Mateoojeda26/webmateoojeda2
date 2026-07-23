package co.edu.iub.myfirtsproyect.dto.notification

import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class NotificationChannelCreateRequest(
    @field:NotNull val type: NotificationChannelType,
    @field:NotBlank val destination: String,
    val label: String? = null,
)
