package co.edu.iub.myfirtsproyect.dto.user

import jakarta.validation.constraints.Email

data class UpdateProfileRequest(
    @field:Email
    val email: String?,
    val fullName: String?,
    val phoneNumber: String? = null,
)
