package co.edu.iub.myfirtsproyect.dto.pet

import java.time.LocalDate

data class PetUpdateRequest(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val color: String? = null,
    val birthDate: LocalDate? = null,
    val sex: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
)
