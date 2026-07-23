package co.edu.iub.myfirtsproyect.dto.series

import co.edu.iub.myfirtsproyect.model.CareTaskPriority
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import co.edu.iub.myfirtsproyect.model.SeriesStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

data class RecurringSeriesCreateRequest(
    @field:NotNull
    val petId: Long,

    @field:NotBlank
    @field:Size(max = 120)
    val title: String,

    @field:Size(max = 2000)
    val description: String? = null,

    @field:NotBlank
    @field:Size(max = 60)
    val careType: String,

    val priority: CareTaskPriority = CareTaskPriority.MEDIUM,

    @field:NotNull
    val frequency: RecurrenceType,

    val intervalDays: Int? = null,

    val daysOfWeek: List<String>? = null,

    @field:NotEmpty
    @field:Size(max = 6)
    val timesOfDay: List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String>,

    @field:NotNull
    val startDate: LocalDate,

    val endDate: LocalDate? = null,
)

data class RecurringSeriesUpdateRequest(
    @field:Size(max = 120)
    val title: String? = null,

    @field:Size(max = 2000)
    val description: String? = null,

    @field:Size(max = 60)
    val careType: String? = null,

    val priority: CareTaskPriority? = null,

    val frequency: RecurrenceType? = null,

    val intervalDays: Int? = null,

    val daysOfWeek: List<String>? = null,

    @field:Size(max = 6)
    val timesOfDay: List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String>? = null,

    val endDate: LocalDate? = null,

    val clearEndDate: Boolean = false,

    val applyFrom: LocalDate? = null,
)

data class RecurringSeriesResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val careType: String,
    val priority: CareTaskPriority,
    val frequency: RecurrenceType,
    val intervalDays: Int?,
    val daysOfWeek: List<String>,
    val timesOfDay: List<String>,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: SeriesStatus,
    val petId: Long,
    val petName: String,
    val canManage: Boolean,
    val pendingOccurrences: Int,
    val completedOccurrences: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
