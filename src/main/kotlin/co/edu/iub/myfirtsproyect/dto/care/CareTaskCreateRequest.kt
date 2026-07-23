package co.edu.iub.myfirtsproyect.dto.care

import co.edu.iub.myfirtsproyect.model.CareTaskPriority
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime

data class CareTaskCreateRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    @field:NotBlank val careType: String,
    @field:NotNull val scheduledAt: LocalDateTime,
    val priority: CareTaskPriority = CareTaskPriority.MEDIUM,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceIntervalDays: Int? = null,
    val recurrenceDays: String? = null,
    val recurrenceEndDate: LocalDate? = null,
    @field:NotNull val petId: Long,
)
