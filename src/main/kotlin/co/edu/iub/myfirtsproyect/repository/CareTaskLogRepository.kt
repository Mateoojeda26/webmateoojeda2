package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.CareTaskLog
import org.springframework.data.jpa.repository.JpaRepository

interface CareTaskLogRepository : JpaRepository<CareTaskLog, Long> {
    fun findAllByCareTaskIdOrderByCreatedAtDesc(careTaskId: Long): List<CareTaskLog>
}
