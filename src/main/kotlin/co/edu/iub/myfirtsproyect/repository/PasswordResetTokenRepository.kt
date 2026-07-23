package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): PasswordResetToken?
    fun findAllByUserIdAndUsedAtIsNull(userId: Long): List<PasswordResetToken>
}
