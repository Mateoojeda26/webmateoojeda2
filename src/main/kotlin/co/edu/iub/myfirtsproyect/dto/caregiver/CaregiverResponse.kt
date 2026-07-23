package co.edu.iub.myfirtsproyect.dto.caregiver

import java.time.LocalDateTime
import java.time.LocalDate

data class CaregiverResponse(
    val id: Long,
    val petId: Long,
    val petName: String,
    val ownerId: Long,
    val ownerName: String,
    val caregiverId: Long,
    val caregiverName: String,
    val caregiverEmail: String,
    val permission: String,
    val active: Boolean,
    val status: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val createdAt: LocalDateTime,
)
