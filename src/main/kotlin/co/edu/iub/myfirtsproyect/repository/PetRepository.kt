package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.Pet
import org.springframework.data.jpa.repository.JpaRepository

interface PetRepository : JpaRepository<Pet, Long> {
    fun findAllByOwnerIdAndArchivedFalseOrderByNameAsc(ownerId: Long): List<Pet>
    fun findAllByOwnerIdOrderByNameAsc(ownerId: Long): List<Pet>
    fun countByArchivedFalse(): Long
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Pet?
    fun findByIdAndOwnerIdAndArchivedFalse(id: Long, ownerId: Long): Pet?
}
