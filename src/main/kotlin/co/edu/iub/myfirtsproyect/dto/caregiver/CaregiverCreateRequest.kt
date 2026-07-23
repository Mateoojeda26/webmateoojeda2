package co.edu.iub.myfirtsproyect.dto.caregiver

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CaregiverCreateRequest(
    @field:NotNull val petId: Long,
    @field:Email val caregiverEmail: String,
    val permission: String = "EDITOR",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)
