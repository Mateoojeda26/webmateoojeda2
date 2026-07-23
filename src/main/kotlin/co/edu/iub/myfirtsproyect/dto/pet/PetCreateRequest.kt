package co.edu.iub.myfirtsproyect.dto.pet

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class PetCreateRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val species: String,
    val breed: String? = null,
    val color: String? = null,
    val birthDate: LocalDate? = null,
    val sex: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
)
