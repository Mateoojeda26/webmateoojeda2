package co.edu.iub.myfirtsproyect.dto.caregiver

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CaregiverUpdateRequest(
    @field:NotBlank val permission: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)
