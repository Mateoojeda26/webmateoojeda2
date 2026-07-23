package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.CareTask
import org.springframework.data.jpa.repository.JpaRepository

interface CareTaskRepository : JpaRepository<CareTask, Long> {
    fun findAllByPetOwnerIdAndActiveTrueOrderByScheduledAtAsc(ownerId: Long): List<CareTask>
    fun findAllByPetOwnerIdOrderByScheduledAtDesc(ownerId: Long): List<CareTask>
    fun countByStatusAndActiveTrue(status: co.edu.iub.myfirtsproyect.model.CareTaskStatus): Long
    fun findAllByPetIdInAndActiveTrueOrderByScheduledAtAsc(petIds: Collection<Long>): List<CareTask>
    fun findByIdAndPetOwnerId(id: Long, ownerId: Long): CareTask?
    fun findAllByPetIdAndPetOwnerIdAndActiveTrueOrderByScheduledAtAsc(petId: Long, ownerId: Long): List<CareTask>
    fun findAllByPetOwnerIdAndStatusAndActiveTrue(
        ownerId: Long,
        status: co.edu.iub.myfirtsproyect.model.CareTaskStatus,
    ): List<CareTask>
    fun existsByPetIdAndTitleAndScheduledAtAndActiveTrue(petId: Long, title: String, scheduledAt: java.time.LocalDateTime): Boolean
    fun findAllBySeriesId(seriesId: Long): List<CareTask>
    fun findBySeriesIdAndScheduledAt(seriesId: Long, scheduledAt: java.time.LocalDateTime): CareTask?
    fun findAllBySeriesIdAndStatusAndScheduledAtGreaterThanEqual(
        seriesId: Long,
        status: co.edu.iub.myfirtsproyect.model.CareTaskStatus,
        from: java.time.LocalDateTime,
    ): List<CareTask>
    fun findAllByStatusAndActiveTrueAndScheduledAtBetweenOrderByScheduledAtAsc(
        status: co.edu.iub.myfirtsproyect.model.CareTaskStatus,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime,
    ): List<CareTask>
}
