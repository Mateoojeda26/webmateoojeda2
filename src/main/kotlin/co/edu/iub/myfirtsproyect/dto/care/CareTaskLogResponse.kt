package co.edu.iub.myfirtsproyect.dto.care

import java.time.LocalDateTime

data class CareTaskLogResponse(
    val id: Long,
    val action: String,
    val actorName: String,
    val detail: String?,
    val createdAt: LocalDateTime,
)
