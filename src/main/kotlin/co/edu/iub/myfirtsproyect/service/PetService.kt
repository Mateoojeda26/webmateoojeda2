package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.pet.PetCreateRequest
import co.edu.iub.myfirtsproyect.dto.pet.PetResponse
import co.edu.iub.myfirtsproyect.dto.pet.PetUpdateRequest
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.Pet
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
class PetService(
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
    private val caregiverAccessRepository: CaregiverAccessRepository,
    private val fileStorageService: FileStorageService,
) {
    @Transactional
    fun create(ownerEmail: String, request: PetCreateRequest): PetResponse {
        val owner = findOwner(ownerEmail)
        return petRepository.save(
            Pet(
                name = request.name.trim(),
                species = request.species.trim(),
                breed = request.breed?.trim()?.ifBlank { null },
                color = request.color?.trim()?.ifBlank { null },
                birthDate = request.birthDate,
                sex = request.sex?.trim()?.ifBlank { null },
                photoUrl = request.photoUrl?.trim()?.ifBlank { null },
                notes = request.notes?.trim()?.ifBlank { null },
                owner = owner,
            ),
        ).toResponse()
    }

    @Transactional(readOnly = true)
    fun list(ownerEmail: String): List<PetResponse> {
        val user = findOwner(ownerEmail)
        val userId = requireNotNull(user.id)
        val ownPets = petRepository.findAllByOwnerIdAndArchivedFalseOrderByNameAsc(userId)
            .map { it.toResponse("OWNER") }
        val ownPetIds = ownPets.map { it.id }.toSet()
        val sharedPets = caregiverAccessRepository.findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
            .filter { it.isAvailable() && !it.pet.archived && it.pet.id !in ownPetIds }
            .distinctBy { it.pet.id }
            .map { it.pet.toResponse(it.permission.name) }

        return (ownPets + sharedPets).sortedBy { it.name.lowercase() }
    }

    @Transactional(readOnly = true)
    fun get(id: Long, ownerEmail: String): PetResponse {
        return findPet(id, ownerEmail).toResponse()
    }

    @Transactional
    fun update(id: Long, ownerEmail: String, request: PetUpdateRequest): PetResponse {
        val pet = findPetEntity(id, ownerEmail)
        if (request.name?.isBlank() == true || request.species?.isBlank() == true) {
            throw InvalidRequestException("El nombre y la especie no pueden quedar vacíos")
        }
        request.name?.let { pet.name = it.trim() }
        request.species?.let { pet.species = it.trim() }
        request.breed?.let { pet.breed = it.trim().ifBlank { null } }
        request.color?.let { pet.color = it.trim().ifBlank { null } }
        request.birthDate?.let { pet.birthDate = it }
        request.sex?.let { pet.sex = it.trim().ifBlank { null } }
        request.photoUrl?.let { pet.photoUrl = it.trim().ifBlank { null } }
        request.notes?.let { pet.notes = it.trim().ifBlank { null } }
        pet.updatedAt = LocalDateTime.now()
        return petRepository.save(pet).toResponse()
    }

    @Transactional
    fun archive(id: Long, ownerEmail: String) {
        val pet = findPetEntity(id, ownerEmail)
        pet.archived = true
        pet.updatedAt = LocalDateTime.now()
        petRepository.save(pet)
    }

    @Transactional
    fun uploadImage(id: Long, ownerEmail: String, file: MultipartFile): PetResponse {
        val pet = findPetEntity(id, ownerEmail)
        val previousImage = pet.photoUrl
        val imageUrl = fileStorageService.storeImage(file)
        return try {
            pet.photoUrl = imageUrl
            pet.updatedAt = LocalDateTime.now()
            val response = petRepository.save(pet).toResponse()
            fileStorageService.delete(previousImage)
            response
        } catch (ex: Exception) {
            fileStorageService.delete(imageUrl)
            throw ex
        }
    }

    @Transactional
    fun removeImage(id: Long, ownerEmail: String): PetResponse {
        val pet = findPetEntity(id, ownerEmail)
        val previousImage = pet.photoUrl
        pet.photoUrl = null
        pet.updatedAt = LocalDateTime.now()
        val response = petRepository.save(pet).toResponse()
        fileStorageService.delete(previousImage)
        return response
    }

    @Transactional(readOnly = true)
    fun loadImage(id: Long, email: String): StoredImage {
        val user = findOwner(email)
        val userId = requireNotNull(user.id)
        val pet = petRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Pet not found")
        }
        if (pet.archived) throw ResourceNotFoundException("Pet not found")
        val authorized = user.role == UserRole.ADMIN || pet.owner.id == userId ||
            caregiverAccessRepository.findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .any { it.pet.id == id && it.isAvailable() }
        if (!authorized) throw ResourceNotFoundException("Pet not found")
        return fileStorageService.loadImage(pet.photoUrl)
    }

    private fun findOwner(email: String) = userRepository.findByEmail(email)
        ?: throw InvalidCredentialsException("User not found")

    private fun findPet(id: Long, ownerEmail: String): Pet = findPetEntity(id, ownerEmail)

    private fun findPetEntity(id: Long, ownerEmail: String): Pet {
        val owner = findOwner(ownerEmail)
        return petRepository.findByIdAndOwnerIdAndArchivedFalse(id, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Pet not found")
    }

    private fun Pet.toResponse(accessLevel: String = "OWNER") = PetResponse(
        id = requireNotNull(id),
        name = name,
        species = species,
        breed = breed,
        color = color,
        birthDate = birthDate,
        sex = sex,
        photoUrl = photoUrl?.let { "/api/pets/${requireNotNull(id)}/image" },
        notes = notes,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        accessLevel = accessLevel,
    )
}
