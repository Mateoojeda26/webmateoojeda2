package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.MessageResponse
import co.edu.iub.myfirtsproyect.dto.user.ChangePasswordRequest
import co.edu.iub.myfirtsproyect.dto.user.UpdateProfileRequest
import co.edu.iub.myfirtsproyect.dto.user.UserResponse
import co.edu.iub.myfirtsproyect.exception.DuplicateResourceException
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.AccountStatus
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.exception.ForbiddenOperationException
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun getProfile(currentEmail: String): UserResponse {
        return findUserByEmail(currentEmail).toResponse()
    }

    fun updateProfile(currentEmail: String, request: UpdateProfileRequest): UserResponse {
        val user = findUserByEmail(currentEmail)

        request.email?.let { newEmail ->
            val normalizedEmail = newEmail.trim().lowercase()
            if (userRepository.existsByEmailAndIdNot(normalizedEmail, requireNotNull(user.id))) {
                throw DuplicateResourceException("Email already exists")
            }
            user.email = normalizedEmail
        }

        request.fullName?.let { user.fullName = it.trim() }
        if (request.phoneNumber != null) {
            user.phoneNumber = request.phoneNumber.trim().ifBlank { null }
        }

        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun changePassword(currentEmail: String, request: ChangePasswordRequest): MessageResponse {
        val user = findUserByEmail(currentEmail)
        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw InvalidCredentialsException("La contraseña actual no es correcta")
        }
        user.passwordHash = passwordEncoder.encode(request.newPassword)!!
        userRepository.save(user)
        return MessageResponse("Contraseña actualizada correctamente")
    }

    fun deleteProfile(currentEmail: String) {
        val user = findUserByEmail(currentEmail)
        if (user.role == UserRole.ADMIN) {
            throw ForbiddenOperationException("La cuenta administradora no se puede desactivar")
        }
        user.active = false
        user.accountStatus = AccountStatus.SUSPENDED
        user.suspendedAt = java.time.LocalDateTime.now()
        user.suspensionReason = "Cuenta desactivada por el usuario"
        userRepository.save(user)
    }

    private fun findUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException("User not found")
    }

    private fun User.toResponse(): UserResponse {
        return UserResponse(
            id = requireNotNull(id),
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            active = active,
            role = role.name,
            createdAt = createdAt,
        )
    }
}
