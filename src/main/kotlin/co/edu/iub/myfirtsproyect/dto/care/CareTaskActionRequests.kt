package co.edu.iub.myfirtsproyect.dto.care

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CareTaskReasonRequest(
    @field:NotBlank val reason: String,
)

data class CareTaskRescheduleRequest(
    @field:NotNull val scheduledAt: LocalDateTime,
    val reason: String? = null,
)
