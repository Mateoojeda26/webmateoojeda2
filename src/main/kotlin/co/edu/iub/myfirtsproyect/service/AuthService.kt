package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.auth.LoginRequest
import co.edu.iub.myfirtsproyect.dto.auth.RegisterRequest
import co.edu.iub.myfirtsproyect.dto.auth.TokenResponse
import co.edu.iub.myfirtsproyect.dto.user.UserResponse
import co.edu.iub.myfirtsproyect.exception.DuplicateResourceException
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
open class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    open fun register(request: RegisterRequest): UserResponse {
        val email = request.email.trim().lowercase()

        if (userRepository.existsByEmail(email)) {
            throw DuplicateResourceException("Email already exists")
        }

        val user = User(
            email = email,
            fullName = request.fullName.trim(),
            phoneNumber = request.phoneNumber?.trim()?.ifBlank { null },
            passwordHash = passwordEncoder.encode(request.password)!!,
            role = UserRole.USER,
        )

        return userRepository.save(user).toResponse()
    }

    open fun login(request: LoginRequest): TokenResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException("Invalid credentials")

        if (!user.canAccess() || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid credentials")
        }

        val token = jwtService.generateToken(user)
        return TokenResponse(
            accessToken = token,
            expiresIn = jwtService.expirationMinutes * 60,
            role = user.role.name,
            redirectTo = if (user.role == UserRole.ADMIN) "/admin.html" else "/dashboard.html",
        )
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
