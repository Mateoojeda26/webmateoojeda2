package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.CaregiverAccess
import org.springframework.data.jpa.repository.JpaRepository

interface CaregiverAccessRepository : JpaRepository<CaregiverAccess, Long> {
    fun findAllByPetOwnerIdAndActiveTrueOrderByCreatedAtDesc(ownerId: Long): List<CaregiverAccess>
    fun findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(caregiverId: Long): List<CaregiverAccess>
    fun findByIdAndPetOwnerId(id: Long, ownerId: Long): CaregiverAccess?
    fun existsByPetIdAndCaregiverIdAndActiveTrue(petId: Long, caregiverId: Long): Boolean
    fun countByActiveTrue(): Long
}
