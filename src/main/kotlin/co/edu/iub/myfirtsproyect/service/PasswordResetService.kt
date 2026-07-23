package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.model.PasswordResetToken
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.repository.PasswordResetTokenRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDateTime
import java.util.Base64

interface PasswordResetSender {
    fun send(user: User, resetLink: String)
}

@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val tokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val resetSender: PasswordResetSender,
    private val clock: Clock,
    @param:Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @param:Value("\${app.password-reset.expiration-minutes:30}") private val expirationMinutes: Long,
) {
    private val logger = LoggerFactory.getLogger(PasswordResetService::class.java)
    private val random = SecureRandom()

    @Transactional
    fun requestReset(email: String) {
        val normalized = email.trim().lowercase()
        val user = userRepository.findByEmail(normalized)
        if (user == null || !user.canAccess()) {
            logger.info("Password reset requested for an unknown or inactive account")
            return
        }
        val now = LocalDateTime.now(clock)
        tokenRepository.findAllByUserIdAndUsedAtIsNull(requireNotNull(user.id)).forEach { previous ->
            previous.usedAt = now
            tokenRepository.save(previous)
        }
        val rawToken = generateToken()
        tokenRepository.save(
            PasswordResetToken(
                user = user,
                tokenHash = hash(rawToken),
                expiresAt = now.plusMinutes(expirationMinutes),
                createdAt = now,
            ),
        )
        val resetLink = "$baseUrl/reset.html?token=$rawToken"
        try {
            resetSender.send(user, resetLink)
        } catch (ex: Exception) {
            logger.warn("Password reset email could not be sent: {}", ex.message)
        }
    }

    @Transactional
    fun resetPassword(rawToken: String, newPassword: String) {
        val token = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(rawToken.trim()))
            ?: throw InvalidRequestException("El enlace de recuperación no es válido o ya fue utilizado")
        val now = LocalDateTime.now(clock)
        if (token.expiresAt.isBefore(now)) {
            throw InvalidRequestException("El enlace de recuperación venció. Solicita uno nuevo")
        }
        val user = token.user
        if (!user.canAccess()) {
            throw InvalidRequestException("El enlace de recuperación no es válido o ya fue utilizado")
        }
        user.passwordHash = passwordEncoder.encode(newPassword)!!
        userRepository.save(user)
        token.usedAt = now
        tokenRepository.save(token)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }
}
