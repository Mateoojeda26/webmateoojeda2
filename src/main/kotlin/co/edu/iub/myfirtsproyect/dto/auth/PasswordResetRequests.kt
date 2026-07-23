package co.edu.iub.myfirtsproyect.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ForgotPasswordRequest(
    @field:Email
    @field:NotBlank
    val email: String,
)

data class ResetPasswordRequest(
    @field:NotBlank
    val token: String,

    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String,
)

data class PasswordResetMessageResponse(val message: String)
