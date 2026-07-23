package co.edu.iub.myfirtsproyect.dto.user

import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val phoneNumber: String?,
    val active: Boolean,
    val role: String,
    val createdAt: LocalDateTime,
)
