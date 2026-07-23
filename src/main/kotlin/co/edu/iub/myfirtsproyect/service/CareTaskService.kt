package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.care.CareTaskCreateRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskLogResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskUpdateRequest
import co.edu.iub.myfirtsproyect.dto.care.DailyRoutineCreateRequest
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.ForbiddenOperationException
import co.edu.iub.myfirtsproyect.exception.InvalidStatusTransitionException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.CareTask
import co.edu.iub.myfirtsproyect.model.CareTaskLog
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.CaregiverPermission
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.CareTaskLogRepository
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

enum class CareAccessLevel { OWNER, EDITOR, VIEWER }

@Service
class CareTaskService(
    private val careTaskRepository: CareTaskRepository,
    private val caregiverAccessRepository: CaregiverAccessRepository,
    private val logRepository: CareTaskLogRepository,
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
    private val reminderCancellationService: ReminderCancellationService,
    private val clock: Clock,
) {
    private val logFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    @Transactional
    fun create(ownerEmail: String, request: CareTaskCreateRequest): CareTaskResponse {
        val owner = findOwner(ownerEmail)
        val pet = petRepository.findByIdAndOwnerIdAndArchivedFalse(request.petId, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Pet not found")
        val task = CareTask(
            title = request.title.trim(),
            description = request.description?.trim()?.ifBlank { null },
            careType = request.careType.trim(),
            scheduledAt = request.scheduledAt,
            priority = request.priority,
            recurrence = request.recurrence,
            recurrenceIntervalDays = request.recurrenceIntervalDays,
            recurrenceDays = request.recurrenceDays,
            recurrenceEndDate = request.recurrenceEndDate,
            pet = pet,
        )
        return careTaskRepository.save(task).toResponse(CareAccessLevel.OWNER)
    }

    @Transactional
    fun createDailyRoutine(ownerEmail: String, request: DailyRoutineCreateRequest): List<CareTaskResponse> {
        val owner = findOwner(ownerEmail)
        val pet = petRepository.findByIdAndOwnerIdAndArchivedFalse(request.petId, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Pet not found")
        if (request.recurrenceEndDate?.isBefore(request.startDate) == true) {
            throw InvalidRequestException("La fecha final no puede ser anterior al inicio")
        }
        val cleanSlots = request.slots
            .map { it.label.trim() to LocalTime.parse(it.time) }
            .distinctBy { it.second }
        if (cleanSlots.size != request.slots.size) {
            throw InvalidRequestException("Los horarios de la rutina no pueden repetirse")
        }
        val baseTitle = request.title.trim()
        val tasks = cleanSlots.sortedBy { it.second }.map { (label, time) ->
            CareTask(
                title = "$baseTitle · $label",
                description = request.description?.trim()?.ifBlank { null },
                careType = request.careType.trim(),
                scheduledAt = LocalDateTime.of(request.startDate, time),
                priority = request.priority,
                recurrence = RecurrenceType.DAILY,
                recurrenceEndDate = request.recurrenceEndDate,
                pet = pet,
            )
        }
        return careTaskRepository.saveAll(tasks).map { it.toResponse(CareAccessLevel.OWNER) }
    }

    @Transactional(readOnly = true)
    fun list(
        ownerEmail: String,
        petId: Long? = null,
        status: CareTaskStatus? = null,
        careType: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): List<CareTaskResponse> {
        val user = findOwner(ownerEmail)
        val userId = requireNotNull(user.id)
        val ownedTasks = if (petId == null) {
            careTaskRepository.findAllByPetOwnerIdAndActiveTrueOrderByScheduledAtAsc(userId)
        } else {
            careTaskRepository.findAllByPetIdAndPetOwnerIdAndActiveTrueOrderByScheduledAtAsc(petId, userId)
        }
        val sharedAccess = caregiverAccessRepository.findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
            .filter { it.isAvailable(LocalDate.now(clock)) }
        val sharedPermissionByPet = sharedAccess.mapNotNull { access ->
            access.pet.id?.let { it to access.permission }
        }.toMap()
        val sharedPetIds = sharedPermissionByPet.keys.filter { petId == null || it == petId }
        val sharedTasks = if (sharedPetIds.isEmpty()) emptyList() else {
            careTaskRepository.findAllByPetIdInAndActiveTrueOrderByScheduledAtAsc(sharedPetIds)
        }
        val ownedIds = ownedTasks.mapNotNull { it.id }.toSet()
        val results = ownedTasks.map { it to CareAccessLevel.OWNER } +
            sharedTasks.filter { it.id !in ownedIds }.map { task ->
                task to sharedPermissionByPet[task.pet.id].toAccessLevel()
            }
        return results
            .filter { (task, _) -> matchesFilters(task, status, careType, from, to) }
            .sortedBy { (task, _) -> task.scheduledAt }
            .map { (task, level) -> task.toResponse(level) }
    }

    @Transactional(readOnly = true)
    fun get(id: Long, ownerEmail: String): CareTaskResponse {
        val access = findTaskAccess(id, ownerEmail)
        return access.task.toResponse(access.level)
    }

    @Transactional(readOnly = true)
    fun logs(id: Long, ownerEmail: String): List<CareTaskLogResponse> {
        findTaskAccess(id, ownerEmail)
        return logRepository.findAllByCareTaskIdOrderByCreatedAtDesc(id).map {
            CareTaskLogResponse(
                id = requireNotNull(it.id),
                action = it.action,
                actorName = it.actor.fullName,
                detail = it.detail,
                createdAt = it.createdAt,
            )
        }
    }

    @Transactional
    fun update(id: Long, ownerEmail: String, request: CareTaskUpdateRequest): CareTaskResponse {
        val access = findTaskAccess(id, ownerEmail)
        val task = access.task
        if (access.level == CareAccessLevel.VIEWER) {
            throw ForbiddenOperationException("Tu permiso de solo lectura no permite modificar cuidados")
        }
        if (access.level == CareAccessLevel.EDITOR && request.hasOwnerOnlyChanges()) {
            throw ForbiddenOperationException("El cuidador solo puede actualizar el estado o reprogramar el cuidado")
        }
        val now = LocalDateTime.now(clock)
        var generateNextOccurrence = false
        request.status?.let { next ->
            validateStatusTransition(task.status, next)
            if (next == CareTaskStatus.SKIPPED && request.reason.isNullOrBlank()) {
                throw InvalidRequestException("Debes indicar por qué el cuidado no fue realizado")
            }
            if (task.status == CareTaskStatus.PENDING && next != CareTaskStatus.PENDING) {
                generateNextOccurrence = task.series == null && task.recurrence != RecurrenceType.NONE
                task.completedAt = now
                task.completedBy = access.user
                logAction(task, access.user, next.name, request.reason?.trim()?.ifBlank { null }, now)
            }
            task.status = next
        }
        request.scheduledAt?.let { newDate ->
            if (newDate != task.scheduledAt) {
                if (task.status != CareTaskStatus.PENDING) {
                    throw InvalidRequestException("Solo se puede reprogramar un cuidado pendiente")
                }
                logAction(
                    task,
                    access.user,
                    "RESCHEDULED",
                    buildString {
                        append("De ${task.scheduledAt.format(logFormatter)} a ${newDate.format(logFormatter)}")
                        request.reason?.trim()?.takeIf { it.isNotBlank() }?.let { append(". Motivo: $it") }
                    },
                    now,
                )
                task.scheduledAt = newDate
            }
        }
        request.title?.let { task.title = it.trim() }
        request.description?.let { task.description = it.trim().ifBlank { null } }
        request.careType?.let { task.careType = it.trim() }
        request.priority?.let { task.priority = it }
        request.recurrence?.let { task.recurrence = it }
        request.recurrenceIntervalDays?.let { task.recurrenceIntervalDays = it }
        request.recurrenceDays?.let { task.recurrenceDays = it }
        request.recurrenceEndDate?.let { task.recurrenceEndDate = it }
        request.active?.let { task.active = it }
        task.updatedAt = now
        val saved = careTaskRepository.save(task)
        if (generateNextOccurrence && saved.active) createNextOccurrence(saved)
        return saved.toResponse(access.level)
    }

    @Transactional
    fun delete(id: Long, ownerEmail: String) {
        val access = findTaskAccess(id, ownerEmail)
        if (access.level != CareAccessLevel.OWNER) {
            throw ForbiddenOperationException("Solo el propietario puede eliminar el cuidado")
        }
        val task = access.task
        val now = LocalDateTime.now(clock)
        reminderCancellationService.cancelTask(task, "Cancelado porque el cuidado fue eliminado")
        task.active = false
        task.updatedAt = now
        logAction(task, access.user, "CANCELLED", null, now)
        careTaskRepository.save(task)
    }

    @Transactional
    fun cancel(id: Long, ownerEmail: String, reason: String): CareTaskResponse {
        if (reason.isBlank()) throw InvalidRequestException("Debes indicar el motivo de la cancelación")
        val access = findTaskAccess(id, ownerEmail)
        if (access.level != CareAccessLevel.OWNER) {
            throw ForbiddenOperationException("Solo el propietario puede cancelar el cuidado")
        }
        val task = access.task
        if (task.status != CareTaskStatus.PENDING || !task.active) {
            throw InvalidRequestException("Solo se puede cancelar un cuidado pendiente")
        }
        val now = LocalDateTime.now(clock)
        reminderCancellationService.cancelTask(task, "Cancelado: ${reason.trim()}")
        task.active = false
        task.updatedAt = now
        logAction(task, access.user, "CANCELLED", reason.trim(), now)
        return careTaskRepository.save(task).toResponse(access.level)
    }

    private fun logAction(task: CareTask, actor: User, action: String, detail: String?, now: LocalDateTime) {
        logRepository.save(
            CareTaskLog(careTask = task, actor = actor, action = action, detail = detail, createdAt = now),
        )
    }

    private fun matchesFilters(
        task: CareTask,
        status: CareTaskStatus?,
        careType: String?,
        from: LocalDate?,
        to: LocalDate?,
    ): Boolean {
        if (status != null && task.status != status) return false
        if (!careType.isNullOrBlank() && !task.careType.equals(careType.trim(), ignoreCase = true)) return false
        val date = task.scheduledAt.toLocalDate()
        if (from != null && date.isBefore(from)) return false
        if (to != null && date.isAfter(to)) return false
        return true
    }

    private fun CaregiverPermission?.toAccessLevel(): CareAccessLevel = when (this) {
        CaregiverPermission.EDITOR -> CareAccessLevel.EDITOR
        else -> CareAccessLevel.VIEWER
    }

    private fun findOwner(email: String) = userRepository.findByEmail(email)
        ?: throw InvalidCredentialsException("User not found")

    private fun findTaskAccess(id: Long, ownerEmail: String): TaskAccess {
        val user = findOwner(ownerEmail)
        val userId = requireNotNull(user.id)
        val task = careTaskRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Care task not found")
        }
        if (user.role == UserRole.ADMIN) return TaskAccess(task, user, CareAccessLevel.OWNER)
        if (task.pet.owner.id == userId) return TaskAccess(task, user, CareAccessLevel.OWNER)
        val access = caregiverAccessRepository
            .findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
            .filter { it.isAvailable(LocalDate.now(clock)) }
            .firstOrNull { it.pet.id == task.pet.id }
            ?: throw ResourceNotFoundException("Care task not found")
        return TaskAccess(task, user, access.permission.toAccessLevel())
    }

    private fun CareTaskUpdateRequest.hasOwnerOnlyChanges() = title != null || description != null ||
        careType != null || priority != null || recurrence != null ||
        recurrenceIntervalDays != null || recurrenceDays != null || recurrenceEndDate != null || active != null

    private fun validateStatusTransition(current: CareTaskStatus, next: CareTaskStatus) {
        if (current == next) return
        val allowed = current == CareTaskStatus.PENDING &&
            (next == CareTaskStatus.COMPLETED || next == CareTaskStatus.SKIPPED)
        if (!allowed) throw InvalidStatusTransitionException("Invalid care task status transition")
    }

    private fun createNextOccurrence(task: CareTask) {
        val nextScheduledAt = when (task.recurrence) {
            RecurrenceType.NONE -> return
            RecurrenceType.DAILY -> task.scheduledAt.plusDays(1)
            RecurrenceType.INTERVAL -> task.scheduledAt.plusDays((task.recurrenceIntervalDays ?: 1).coerceAtLeast(1).toLong())
            RecurrenceType.WEEKLY -> nextWeeklyDate(task)
        }
        if (task.recurrenceEndDate?.let { nextScheduledAt.toLocalDate().isAfter(it) } == true) return
        val petId = requireNotNull(task.pet.id)
        if (careTaskRepository.existsByPetIdAndTitleAndScheduledAtAndActiveTrue(petId, task.title, nextScheduledAt)) return
        careTaskRepository.save(
            CareTask(
                title = task.title,
                description = task.description,
                careType = task.careType,
                priority = task.priority,
                scheduledAt = nextScheduledAt,
                recurrence = task.recurrence,
                recurrenceIntervalDays = task.recurrenceIntervalDays,
                recurrenceDays = task.recurrenceDays,
                recurrenceEndDate = task.recurrenceEndDate,
                pet = task.pet,
            ),
        )
    }

    private fun nextWeeklyDate(task: CareTask): LocalDateTime {
        val selectedDays = task.recurrenceDays
            ?.split(',')
            ?.mapNotNull { value -> runCatching { DayOfWeek.valueOf(value.trim().uppercase()) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        if (selectedDays.isEmpty()) return task.scheduledAt.plusWeeks(1)
        return (1L..7L)
            .map { task.scheduledAt.plusDays(it) }
            .first { it.dayOfWeek in selectedDays }
    }

    private fun CareTask.toResponse(level: CareAccessLevel) = CareTaskResponse(
        id = requireNotNull(id),
        title = title,
        description = description,
        careType = careType,
        priority = priority,
        status = status,
        displayStatus = displayStatus(),
        overdue = status == CareTaskStatus.PENDING && scheduledAt.isBefore(LocalDateTime.now(clock)),
        warning = medicationWarning(),
        scheduledAt = scheduledAt,
        recurrence = recurrence,
        recurrenceIntervalDays = recurrenceIntervalDays,
        recurrenceDays = recurrenceDays,
        recurrenceEndDate = recurrenceEndDate,
        active = active,
        petId = requireNotNull(pet.id),
        petName = pet.name,
        seriesId = series?.id,
        accessLevel = level.name,
        completedById = completedBy?.id,
        completedByName = completedBy?.fullName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
    )

    private fun CareTask.displayStatus(): String = when {
        status == CareTaskStatus.PENDING && scheduledAt.isBefore(LocalDateTime.now(clock)) -> "Vencido"
        status == CareTaskStatus.PENDING -> "Pendiente"
        status == CareTaskStatus.COMPLETED -> "Realizado"
        else -> "No realizado"
    }

    private fun CareTask.medicationWarning(): String? {
        val medication = careType.contains("medic", ignoreCase = true) ||
            careType.contains("tratamiento", ignoreCase = true)
        return if (medication && status == CareTaskStatus.SKIPPED) {
            "Atención: un medicamento o tratamiento quedó sin realizar"
        } else null
    }

    private data class TaskAccess(val task: CareTask, val user: User, val level: CareAccessLevel)
}
