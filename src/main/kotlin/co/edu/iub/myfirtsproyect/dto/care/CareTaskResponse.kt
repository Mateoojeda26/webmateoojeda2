package co.edu.iub.myfirtsproyect.dto.care

import co.edu.iub.myfirtsproyect.model.CareTaskPriority
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalDateTime

data class CareTaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val careType: String,
    val priority: CareTaskPriority,
    val status: CareTaskStatus,
    val displayStatus: String,
    val overdue: Boolean,
    val warning: String?,
    val scheduledAt: LocalDateTime,
    val recurrence: RecurrenceType,
    val recurrenceIntervalDays: Int?,
    val recurrenceDays: String?,
    val recurrenceEndDate: LocalDate?,
    val active: Boolean,
    val petId: Long,
    val petName: String,
    val seriesId: Long?,
    val accessLevel: String,
    val completedById: Long?,
    val completedByName: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
)
