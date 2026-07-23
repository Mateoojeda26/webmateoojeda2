package co.edu.iub.myfirtsproyect.dto.pet

import java.time.LocalDate
import java.time.LocalDateTime

data class PetResponse(
    val id: Long,
    val name: String,
    val species: String,
    val breed: String?,
    val color: String?,
    val birthDate: LocalDate?,
    val sex: String?,
    val photoUrl: String?,
    val notes: String?,
    val archived: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val accessLevel: String = "OWNER",
)
