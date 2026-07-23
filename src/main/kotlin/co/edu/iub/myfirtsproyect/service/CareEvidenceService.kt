package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.care.CareEvidenceResponse
import co.edu.iub.myfirtsproyect.exception.ForbiddenOperationException
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.CareEvidence
import co.edu.iub.myfirtsproyect.model.CareTask
import co.edu.iub.myfirtsproyect.model.CaregiverPermission
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.CareEvidenceRepository
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class CareEvidenceService(
    private val evidenceRepository: CareEvidenceRepository,
    private val careTaskRepository: CareTaskRepository,
    private val caregiverAccessRepository: CaregiverAccessRepository,
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
) {
    @Transactional(readOnly = true)
    fun list(taskId: Long, email: String): List<CareEvidenceResponse> {
        val access = requireAccess(taskId, email)
        val owner = access.level == CareAccessLevel.OWNER
        return evidenceRepository.findAllByCareTaskIdOrderByCreatedAtDesc(taskId)
            .map { it.toResponse(owner || it.uploader.id == access.user.id) }
    }

    @Transactional
    fun upload(taskId: Long, email: String, file: MultipartFile, note: String?): CareEvidenceResponse {
        val access = requireAccess(taskId, email)
        if (access.level == CareAccessLevel.VIEWER) {
            throw ForbiddenOperationException("Tu permiso de solo lectura no permite subir evidencias")
        }
        val cleanNote = note?.trim()?.ifBlank { null }
        if ((cleanNote?.length ?: 0) > 500) throw InvalidRequestException("La nota no puede superar 500 caracteres")
        val imageUrl = fileStorageService.storeImage(file)
        return try {
            evidenceRepository.saveAndFlush(
                CareEvidence(careTask = access.task, uploader = access.user, imageUrl = imageUrl, note = cleanNote),
            ).toResponse(true)
        } catch (ex: Exception) {
            fileStorageService.delete(imageUrl)
            throw ex
        }
    }

    @Transactional
    fun delete(taskId: Long, evidenceId: Long, email: String) {
        val access = requireAccess(taskId, email)
        val evidence = evidenceRepository.findByIdAndCareTaskId(evidenceId, taskId)
            ?: throw ResourceNotFoundException("Evidencia no encontrada")
        val canDelete = access.level == CareAccessLevel.OWNER || evidence.uploader.id == access.user.id
        if (!canDelete) throw ForbiddenOperationException("No tienes permiso para eliminar esta evidencia")
        evidenceRepository.delete(evidence)
        fileStorageService.delete(evidence.imageUrl)
    }

    @Transactional(readOnly = true)
    fun loadImage(taskId: Long, evidenceId: Long, email: String): StoredImage {
        requireAccess(taskId, email)
        val evidence = evidenceRepository.findByIdAndCareTaskId(evidenceId, taskId)
            ?: throw ResourceNotFoundException("Evidencia no encontrada")
        return fileStorageService.loadImage(evidence.imageUrl)
    }

    private fun requireAccess(taskId: Long, email: String): EvidenceAccess {
        val user = userRepository.findByEmail(email) ?: throw InvalidCredentialsException("Usuario no encontrado")
        val task = careTaskRepository.findById(taskId).orElseThrow {
            ResourceNotFoundException("Cuidado no encontrado")
        }
        val userId = requireNotNull(user.id)
        if (user.role == UserRole.ADMIN) return EvidenceAccess(task, user, CareAccessLevel.OWNER)
        if (task.pet.owner.id == userId) return EvidenceAccess(task, user, CareAccessLevel.OWNER)
        val caregiverAccess = caregiverAccessRepository
            .findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
            .filter { it.isAvailable() }
            .firstOrNull { it.pet.id == task.pet.id }
            ?: throw ForbiddenOperationException("No tienes acceso a este cuidado")
        val level = if (caregiverAccess.permission == CaregiverPermission.EDITOR) {
            CareAccessLevel.EDITOR
        } else {
            CareAccessLevel.VIEWER
        }
        return EvidenceAccess(task, user, level)
    }

    private fun CareEvidence.toResponse(canDelete: Boolean) = CareEvidenceResponse(
        id = requireNotNull(id),
        taskId = requireNotNull(careTask.id),
        imageUrl = "/api/care-tasks/${requireNotNull(careTask.id)}/images/${requireNotNull(id)}/content",
        note = note,
        uploaderName = uploader.fullName,
        createdAt = createdAt,
        canDelete = canDelete,
    )

    private data class EvidenceAccess(val task: CareTask, val user: User, val level: CareAccessLevel)
}
