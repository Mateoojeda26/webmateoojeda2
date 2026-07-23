package co.edu.iub.myfirtsproyect.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "admin_audit_logs")
class AdminAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var adminEmail: String,

    @Column(nullable = false, length = 80)
    var action: String,

    @Column(nullable = false, length = 80)
    var resourceType: String,

    @Column(nullable = false)
    var resourceIdentifier: String,

    @Column(nullable = false, length = 1000)
    var description: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
