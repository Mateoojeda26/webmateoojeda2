package co.edu.iub.myfirtsproyect.dto.care

import java.time.LocalDateTime

data class CareEvidenceResponse(
    val id: Long,
    val taskId: Long,
    val imageUrl: String,
    val note: String?,
    val uploaderName: String,
    val createdAt: LocalDateTime,
    val canDelete: Boolean,
)
