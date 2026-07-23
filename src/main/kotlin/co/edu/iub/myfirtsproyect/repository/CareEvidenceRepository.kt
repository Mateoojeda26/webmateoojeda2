package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.CareEvidence
import org.springframework.data.jpa.repository.JpaRepository

interface CareEvidenceRepository : JpaRepository<CareEvidence, Long> {
    fun findAllByCareTaskIdOrderByCreatedAtDesc(careTaskId: Long): List<CareEvidence>
    fun findByIdAndCareTaskId(id: Long, careTaskId: Long): CareEvidence?
}
