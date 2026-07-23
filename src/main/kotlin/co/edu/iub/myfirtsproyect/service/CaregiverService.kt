package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverCreateRequest
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverResponse
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverUpdateRequest
import co.edu.iub.myfirtsproyect.exception.DuplicateResourceException
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.CaregiverAccess
import co.edu.iub.myfirtsproyect.model.CaregiverPermission
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CaregiverService(
    private val accessRepository: CaregiverAccessRepository,
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun list(ownerEmail: String): List<CaregiverResponse> {
        val owner = findUser(ownerEmail)
        return accessRepository.findAllByPetOwnerIdAndActiveTrueOrderByCreatedAtDesc(requireNotNull(owner.id))
            .map { it.toResponse() }
    }

    @Transactional
    fun add(ownerEmail: String, request: CaregiverCreateRequest): CaregiverResponse {
        val owner = findUser(ownerEmail)
        val permission = parsePermission(request.permission)
        val pet = petRepository.findByIdAndOwnerIdAndArchivedFalse(request.petId, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Pet not found")
        val caregiver = userRepository.findByEmail(request.caregiverEmail.trim().lowercase())
            ?: throw ResourceNotFoundException("Caregiver user not found")
        if (!caregiver.canAccess()) throw InvalidRequestException("El cuidador debe tener una cuenta activa")
        if (caregiver.id == owner.id) throw DuplicateResourceException("Owner cannot be their own caregiver")
        if (accessRepository.existsByPetIdAndCaregiverIdAndActiveTrue(pet.id!!, caregiver.id!!)) {
            throw DuplicateResourceException("Caregiver already assigned")
        }
        validateDates(request.startDate, request.endDate)
        return accessRepository.save(
            CaregiverAccess(
                pet = pet,
                caregiver = caregiver,
                permission = permission,
                startDate = request.startDate,
                endDate = request.endDate,
            ),
        ).toResponse()
    }

    @Transactional
    fun updatePermission(id: Long, ownerEmail: String, request: CaregiverUpdateRequest): CaregiverResponse {
        val owner = findUser(ownerEmail)
        val access = accessRepository.findByIdAndPetOwnerId(id, requireNotNull(owner.id))
            ?.takeIf { it.active }
            ?: throw ResourceNotFoundException("Caregiver access not found")
        access.permission = parsePermission(request.permission)
        validateDates(request.startDate, request.endDate)
        access.startDate = request.startDate
        access.endDate = request.endDate
        return accessRepository.save(access).toResponse()
    }

    @Transactional
    fun remove(id: Long, ownerEmail: String) {
        val owner = findUser(ownerEmail)
        val access = accessRepository.findByIdAndPetOwnerId(id, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Caregiver access not found")
        access.active = false
        accessRepository.save(access)
    }

    private fun parsePermission(value: String): CaregiverPermission =
        runCatching { CaregiverPermission.valueOf(value.trim().uppercase()) }.getOrElse {
            throw InvalidRequestException("El permiso debe ser VIEWER o EDITOR")
        }

    private fun findUser(email: String) = userRepository.findByEmail(email)
        ?: throw InvalidCredentialsException("User not found")

    private fun validateDates(startDate: java.time.LocalDate?, endDate: java.time.LocalDate?) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw InvalidRequestException("La fecha final no puede ser anterior a la fecha inicial")
        }
    }

    private fun CaregiverAccess.toResponse() = CaregiverResponse(
        id = requireNotNull(id),
        petId = requireNotNull(pet.id),
        petName = pet.name,
        ownerId = requireNotNull(pet.owner.id),
        ownerName = pet.owner.fullName,
        caregiverId = requireNotNull(caregiver.id),
        caregiverName = caregiver.fullName,
        caregiverEmail = caregiver.email,
        permission = permission.name,
        active = active,
        status = if (active) "ACTIVE" else "INACTIVE",
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
    )
}
