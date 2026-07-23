package co.edu.iub.myfirtsproyect.dto.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "La contraseña actual es obligatoria")
    val currentPassword: String,

    @field:Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
    val newPassword: String,
)
