package co.edu.iub.myfirtsproyect.dto.care

import co.edu.iub.myfirtsproyect.model.CareTaskPriority
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class DailyRoutineSlotRequest(
    @field:NotBlank
    @field:Size(max = 40)
    val label: String,

    @field:NotBlank
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    val time: String,
)

data class DailyRoutineCreateRequest(
    @field:NotNull
    val petId: Long,

    @field:NotBlank
    val title: String,

    val description: String? = null,

    @field:NotBlank
    val careType: String,

    val priority: CareTaskPriority = CareTaskPriority.MEDIUM,

    @field:NotNull
    val startDate: LocalDate,

    val recurrenceEndDate: LocalDate? = null,

    @field:Valid
    @field:NotEmpty
    @field:Size(max = 6)
    val slots: List<DailyRoutineSlotRequest>,
)
