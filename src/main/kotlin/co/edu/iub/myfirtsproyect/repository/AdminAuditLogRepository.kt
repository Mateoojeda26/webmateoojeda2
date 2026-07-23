package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.AdminAuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAuditLogRepository : JpaRepository<AdminAuditLog, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<AdminAuditLog>
}
