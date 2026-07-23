package co.edu.iub.myfirtsproyect.dto.care

import co.edu.iub.myfirtsproyect.model.CareTaskPriority
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalDateTime

data class CareTaskUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val careType: String? = null,
    val scheduledAt: LocalDateTime? = null,
    val priority: CareTaskPriority? = null,
    val status: CareTaskStatus? = null,
    val recurrence: RecurrenceType? = null,
    val recurrenceIntervalDays: Int? = null,
    val recurrenceDays: String? = null,
    val recurrenceEndDate: LocalDate? = null,
    val active: Boolean? = null,
    val reason: String? = null,
)
