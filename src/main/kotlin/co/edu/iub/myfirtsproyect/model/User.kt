package co.edu.iub.myfirtsproyect.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var email: String = "",

    @Column(nullable = false)
    var fullName: String = "",

    @Column
    var phoneNumber: String? = null,

    @Column(nullable = false)
    var passwordHash: String = "",

    @Column(nullable = false)
    var active: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    var accountStatus: AccountStatus = AccountStatus.ACTIVE,

    var suspendedAt: LocalDateTime? = null,

    @Column(length = 500)
    var suspensionReason: String? = null,

    var suspendedBy: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun canAccess(): Boolean = active && accountStatus == AccountStatus.ACTIVE
}
