package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.RecurringSeries
import co.edu.iub.myfirtsproyect.model.SeriesStatus
import org.springframework.data.jpa.repository.JpaRepository

interface RecurringSeriesRepository : JpaRepository<RecurringSeries, Long> {
    fun findAllByPetOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<RecurringSeries>
    fun findAllByPetIdInOrderByCreatedAtDesc(petIds: Collection<Long>): List<RecurringSeries>
    fun findAllByStatus(status: SeriesStatus): List<RecurringSeries>
}
