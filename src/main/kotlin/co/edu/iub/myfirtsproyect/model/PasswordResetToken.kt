package co.edu.iub.myfirtsproyect.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false, unique = true, length = 64)
    var tokenHash: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    var usedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
