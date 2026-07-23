package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.admin.AdminAuditResponse
import co.edu.iub.myfirtsproyect.dto.admin.AdminChannelConfiguration
import co.edu.iub.myfirtsproyect.dto.admin.AdminDashboardResponse
import co.edu.iub.myfirtsproyect.dto.admin.AdminDeleteUserRequest
import co.edu.iub.myfirtsproyect.dto.admin.AdminHistoryItem
import co.edu.iub.myfirtsproyect.dto.admin.AdminSuspendUserRequest
import co.edu.iub.myfirtsproyect.dto.admin.AdminUpdateUserRequest
import co.edu.iub.myfirtsproyect.dto.admin.AdminUserDetailResponse
import co.edu.iub.myfirtsproyect.dto.admin.AdminUserResponse
import co.edu.iub.myfirtsproyect.dto.care.CareEvidenceResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskCreateRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskLogResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskUpdateRequest
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverCreateRequest
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverResponse
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverUpdateRequest
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelResponse
import co.edu.iub.myfirtsproyect.dto.pet.PetCreateRequest
import co.edu.iub.myfirtsproyect.dto.pet.PetResponse
import co.edu.iub.myfirtsproyect.dto.pet.PetUpdateRequest
import co.edu.iub.myfirtsproyect.dto.report.ReportSummaryResponse
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesCreateRequest
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesResponse
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesUpdateRequest
import co.edu.iub.myfirtsproyect.exception.DuplicateResourceException
import co.edu.iub.myfirtsproyect.exception.ForbiddenOperationException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.AccountStatus
import co.edu.iub.myfirtsproyect.model.AdminAuditLog
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.NotificationDeliveryStatus
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.AdminAuditLogRepository
import co.edu.iub.myfirtsproyect.repository.CareTaskLogRepository
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.NotificationDeliveryRepository
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.RecurringSeriesRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val petRepository: PetRepository,
    private val caregiverAccessRepository: CaregiverAccessRepository,
    private val careTaskRepository: CareTaskRepository,
    private val careTaskLogRepository: CareTaskLogRepository,
    private val recurringSeriesRepository: RecurringSeriesRepository,
    private val deliveryRepository: NotificationDeliveryRepository,
    private val channelRepository: NotificationChannelRepository,
    private val auditRepository: AdminAuditLogRepository,
    private val petService: PetService,
    private val caregiverService: CaregiverService,
    private val careTaskService: CareTaskService,
    private val careEvidenceService: CareEvidenceService,
    private val recurringSeriesService: RecurringSeriesService,
    private val channelService: NotificationChannelService,
    private val reportService: ReportService,
    private val passwordResetService: PasswordResetService,
    private val userDeletionService: UserDeletionService,
    private val reminderService: AdminReminderService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun dashboard(): AdminDashboardResponse {
        val now = LocalDateTime.now(clock)
        val users = userRepository.findAll()
        val pending = careTaskRepository.findAll().filter { it.active && it.status == CareTaskStatus.PENDING }
        return AdminDashboardResponse(
            activeUsers = users.count { it.canAccess() }.toLong(),
            suspendedUsers = users.count { !it.canAccess() }.toLong(),
            registeredPets = petRepository.countByArchivedFalse(),
            activeCaregivers = caregiverAccessRepository.countByActiveTrue(),
            pendingCareTasks = pending.size.toLong(),
            overdueCareTasks = pending.count { it.scheduledAt.isBefore(now) }.toLong(),
            failedReminders = deliveryRepository.countByStatus(NotificationDeliveryStatus.FAILED),
        )
    }

    @Transactional(readOnly = true)
    fun listUsers(search: String?, status: String?): List<AdminUserResponse> {
        val cleanSearch = search?.trim()?.lowercase().orEmpty()
        val cleanStatus = status?.trim()?.uppercase().orEmpty()
        return userRepository.findAll()
            .asSequence()
            .filter { cleanSearch.isBlank() || it.fullName.lowercase().contains(cleanSearch) || it.email.contains(cleanSearch) }
            .filter {
                cleanStatus.isBlank() || cleanStatus == it.adminStatus()
            }
            .sortedWith(compareBy<User> { it.role != UserRole.ADMIN }.thenBy { it.fullName.lowercase() })
            .map { it.toAdminResponse() }
            .toList()
    }

    @Transactional(readOnly = true)
    fun userDetail(id: Long): AdminUserDetailResponse {
        val user = findUser(id)
        val userId = requireNotNull(user.id)
        val pets = petService.list(user.email)
        val caregivers = caregiverService.list(user.email)
        val routines = recurringSeriesService.list(user.email)
        val cares = careTaskService.list(user.email)
        val channels = listChannels(userId)
        val history = careTaskRepository.findAllByPetOwnerIdOrderByScheduledAtDesc(userId)
            .flatMap { task ->
                careTaskLogRepository.findAllByCareTaskIdOrderByCreatedAtDesc(requireNotNull(task.id)).map { log ->
                    AdminHistoryItem(
                        taskId = requireNotNull(task.id),
                        taskTitle = task.title,
                        action = log.action,
                        actorName = log.actor.fullName,
                        detail = log.detail,
                        createdAt = log.createdAt,
                    )
                }
            }
            .sortedByDescending { it.createdAt }
        val configuration = AdminChannelConfiguration(
            telegramConnected = channels.any { it.type == NotificationChannelType.TELEGRAM && it.active && it.verified },
            gmailConnected = channels.any { it.type == NotificationChannelType.EMAIL && it.active && it.verified },
        )
        return AdminUserDetailResponse(
            user = user.toAdminResponse(),
            pets = pets,
            caregivers = caregivers,
            routines = routines,
            cares = cares,
            channels = channels,
            history = history,
            report = reportService.summary(user.email),
            configuration = configuration,
        )
    }

    @Transactional
    fun updateUser(adminEmail: String, id: Long, request: AdminUpdateUserRequest): AdminUserResponse {
        val user = findUser(id)
        request.email?.let { value ->
            val email = value.trim().lowercase()
            if (email.isBlank()) throw InvalidRequestException("El correo no puede quedar vacío")
            if (userRepository.existsByEmailAndIdNot(email, id)) throw DuplicateResourceException("El correo ya existe")
            user.email = email
        }
        request.fullName?.let {
            if (it.isBlank()) throw InvalidRequestException("El nombre no puede quedar vacío")
            user.fullName = it.trim()
        }
        if (request.phoneNumber != null) user.phoneNumber = request.phoneNumber.trim().ifBlank { null }
        val saved = userRepository.save(user)
        record(adminEmail, "EDIT_USER", "USER", saved.email, "Se actualizaron los datos permitidos del usuario")
        return saved.toAdminResponse()
    }

    @Transactional
    fun suspendUser(adminEmail: String, id: Long, request: AdminSuspendUserRequest): AdminUserResponse {
        val user = findUser(id)
        protectAdmin(user, "suspender")
        val now = LocalDateTime.now(clock)
        user.active = false
        user.accountStatus = AccountStatus.SUSPENDED
        user.suspendedAt = now
        user.suspensionReason = request.reason.trim()
        user.suspendedBy = adminEmail
        val saved = userRepository.save(user)
        record(adminEmail, "SUSPEND_USER", "USER", saved.email, "Motivo: ${request.reason.trim()}")
        return saved.toAdminResponse()
    }

    @Transactional
    fun reactivateUser(adminEmail: String, id: Long): AdminUserResponse {
        val user = findUser(id)
        protectAdmin(user, "reactivar")
        user.active = true
        user.accountStatus = AccountStatus.ACTIVE
        user.suspendedAt = null
        user.suspensionReason = null
        user.suspendedBy = null
        val saved = userRepository.save(user)
        record(adminEmail, "REACTIVATE_USER", "USER", saved.email, "La cuenta recuperó el acceso")
        return saved.toAdminResponse()
    }

    @Transactional
    fun startPasswordReset(adminEmail: String, id: Long) {
        val user = findUser(id)
        passwordResetService.requestReset(user.email)
        record(adminEmail, "START_PASSWORD_RESET", "USER", user.email, "Se inició la recuperación de contraseña")
    }

    @Transactional
    fun deleteUser(adminEmail: String, id: Long, request: AdminDeleteUserRequest) {
        val user = findUser(id)
        protectAdmin(user, "eliminar")
        if (!user.email.equals(request.confirmationEmail.trim(), ignoreCase = true)) {
            throw InvalidRequestException("La confirmación debe coincidir con el correo exacto del usuario")
        }
        record(adminEmail, "DELETE_USER", "USER", user.email, "Eliminación definitiva del usuario y sus datos asociados")
        auditRepository.flush()
        userDeletionService.deleteUserAndOwnedData(id)
    }

    @Transactional
    fun createPet(adminEmail: String, userId: Long, request: PetCreateRequest): PetResponse {
        val owner = findUser(userId)
        val response = petService.create(owner.email, request)
        record(adminEmail, "CREATE_PET", "PET", response.id.toString(), "Mascota creada para ${owner.email}")
        return response
    }

    @Transactional(readOnly = true)
    fun listPets(userId: Long): List<PetResponse> = petService.list(findUser(userId).email)

    @Transactional(readOnly = true)
    fun getPet(id: Long): PetResponse {
        val pet = findPet(id)
        return petService.get(id, pet.owner.email)
    }

    @Transactional
    fun updatePet(adminEmail: String, id: Long, request: PetUpdateRequest): PetResponse {
        val pet = findPet(id)
        val response = petService.update(id, pet.owner.email, request)
        record(adminEmail, "EDIT_PET", "PET", id.toString(), "Mascota de ${pet.owner.email} actualizada")
        return response
    }

    @Transactional
    fun deletePet(adminEmail: String, id: Long) {
        val pet = findPet(id)
        petService.archive(id, pet.owner.email)
        record(adminEmail, "DELETE_PET", "PET", id.toString(), "Mascota de ${pet.owner.email} archivada")
    }

    @Transactional
    fun createCaregiver(adminEmail: String, userId: Long, request: CaregiverCreateRequest): CaregiverResponse {
        val owner = findUser(userId)
        val response = caregiverService.add(owner.email, request)
        record(adminEmail, "CREATE_CAREGIVER", "CAREGIVER_ACCESS", response.id.toString(), "Cuidador asignado a ${response.petName}")
        return response
    }

    @Transactional
    fun updateCaregiver(adminEmail: String, id: Long, request: CaregiverUpdateRequest): CaregiverResponse {
        val access = caregiverAccessRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Asignación de cuidador no encontrada")
        }
        val response = caregiverService.updatePermission(id, access.pet.owner.email, request)
        record(adminEmail, "EDIT_CAREGIVER_PERMISSION", "CAREGIVER_ACCESS", id.toString(), "Permiso actualizado a ${response.permission}")
        return response
    }

    @Transactional
    fun deleteCaregiver(adminEmail: String, id: Long) {
        val access = caregiverAccessRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Asignación de cuidador no encontrada")
        }
        caregiverService.remove(id, access.pet.owner.email)
        record(adminEmail, "DELETE_CAREGIVER", "CAREGIVER_ACCESS", id.toString(), "Asignación de cuidador revocada")
    }

    @Transactional
    fun createCareTask(adminEmail: String, userId: Long, request: CareTaskCreateRequest): CareTaskResponse {
        val owner = findUser(userId)
        val response = careTaskService.create(owner.email, request)
        record(adminEmail, "CREATE_CARE_TASK", "CARE_TASK", response.id.toString(), "Cuidado creado para ${owner.email}")
        return response
    }

    @Transactional(readOnly = true)
    fun listCareTasks(userId: Long): List<CareTaskResponse> = careTaskService.list(findUser(userId).email)

    @Transactional(readOnly = true)
    fun getCareTask(id: Long): CareTaskResponse {
        val task = findCareTask(id)
        return careTaskService.get(id, task.pet.owner.email)
    }

    @Transactional
    fun updateCareTask(adminEmail: String, id: Long, request: CareTaskUpdateRequest): CareTaskResponse {
        val task = findCareTask(id)
        val response = careTaskService.update(id, task.pet.owner.email, request)
        record(adminEmail, "EDIT_CARE_TASK", "CARE_TASK", id.toString(), "Cuidado actualizado")
        return response
    }

    @Transactional
    fun deleteCareTask(adminEmail: String, id: Long) {
        val task = findCareTask(id)
        careTaskService.delete(id, task.pet.owner.email)
        record(adminEmail, "DELETE_CARE_TASK", "CARE_TASK", id.toString(), "Cuidado cancelado desde administración")
    }

    @Transactional(readOnly = true)
    fun careTaskLogs(id: Long): List<CareTaskLogResponse> {
        val task = findCareTask(id)
        return careTaskService.logs(id, task.pet.owner.email)
    }

    @Transactional(readOnly = true)
    fun careTaskEvidence(id: Long): List<CareEvidenceResponse> {
        val task = findCareTask(id)
        return careEvidenceService.list(id, task.pet.owner.email)
    }

    @Transactional
    fun createSeries(adminEmail: String, userId: Long, request: RecurringSeriesCreateRequest): RecurringSeriesResponse {
        val owner = findUser(userId)
        val response = recurringSeriesService.create(owner.email, request)
        record(adminEmail, "CREATE_SERIES", "RECURRING_SERIES", response.id.toString(), "Rutina creada para ${owner.email}")
        return response
    }

    @Transactional
    fun updateSeries(adminEmail: String, id: Long, request: RecurringSeriesUpdateRequest): RecurringSeriesResponse {
        val series = recurringSeriesRepository.findById(id).orElseThrow { ResourceNotFoundException("Rutina no encontrada") }
        val response = recurringSeriesService.update(id, series.pet.owner.email, request)
        record(adminEmail, "EDIT_SERIES", "RECURRING_SERIES", id.toString(), "Rutina actualizada")
        return response
    }

    @Transactional
    fun deleteSeries(adminEmail: String, id: Long) {
        val series = recurringSeriesRepository.findById(id).orElseThrow { ResourceNotFoundException("Rutina no encontrada") }
        recurringSeriesService.cancel(id, series.pet.owner.email)
        record(adminEmail, "DELETE_SERIES", "RECURRING_SERIES", id.toString(), "Rutina cancelada")
    }

    @Transactional(readOnly = true)
    fun listChannels(userId: Long): List<NotificationChannelResponse> {
        findUser(userId)
        return channelRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId).map { channel ->
            NotificationChannelResponse(
                id = requireNotNull(channel.id),
                type = channel.type,
                destination = if (channel.type == NotificationChannelType.TELEGRAM) {
                    if (channel.active) "Telegram conectado" else "Telegram desconectado"
                } else channel.destination,
                label = channel.label,
                verified = channel.verified,
                active = channel.active,
                reminderMinutesBefore = channel.reminderMinutesBefore ?: 10,
                createdAt = channel.createdAt,
                updatedAt = channel.updatedAt,
            )
        }
    }

    @Transactional
    fun unlinkChannel(adminEmail: String, id: Long) {
        val channel = channelServiceOwner(id)
        channelService.delete(id, channel.email)
        record(adminEmail, "UNLINK_CHANNEL", "NOTIFICATION_CHANNEL", id.toString(), "Canal desvinculado de ${channel.email}")
    }

    @Transactional(readOnly = true)
    fun report(userId: Long): ReportSummaryResponse = reportService.summary(findUser(userId).email)

    @Transactional(readOnly = true)
    fun exportReport(userId: Long): String = reportService.exportCsv(findUser(userId).email)

    @Transactional(readOnly = true)
    fun failedReminders() = reminderService.listFailed()

    @Transactional
    fun retryReminder(adminEmail: String, id: Long) = reminderService.retry(id).also {
        record(adminEmail, "RETRY_REMINDER", "NOTIFICATION_DELIVERY", id.toString(), "Recordatorio reenviado por ${it.channelType}")
    }

    @Transactional(readOnly = true)
    fun audit(): List<AdminAuditResponse> = auditRepository.findAllByOrderByCreatedAtDesc().map {
        AdminAuditResponse(
            id = requireNotNull(it.id),
            adminEmail = it.adminEmail,
            action = it.action,
            resourceType = it.resourceType,
            resourceIdentifier = it.resourceIdentifier,
            description = it.description,
            createdAt = it.createdAt,
        )
    }

    private fun record(
        adminEmail: String,
        action: String,
        resourceType: String,
        identifier: String,
        description: String,
    ) {
        auditRepository.save(
            AdminAuditLog(
                adminEmail = adminEmail,
                action = action,
                resourceType = resourceType,
                resourceIdentifier = identifier,
                description = description,
                createdAt = LocalDateTime.now(clock),
            ),
        )
    }

    private fun findUser(id: Long): User = userRepository.findById(id).orElseThrow {
        ResourceNotFoundException("Usuario no encontrado")
    }

    private fun findPet(id: Long) = petRepository.findById(id).orElseThrow {
        ResourceNotFoundException("Mascota no encontrada")
    }

    private fun findCareTask(id: Long) = careTaskRepository.findById(id).orElseThrow {
        ResourceNotFoundException("Cuidado no encontrado")
    }

    private fun protectAdmin(user: User, action: String) {
        if (user.role == UserRole.ADMIN) {
            throw ForbiddenOperationException("La cuenta administradora no se puede $action")
        }
    }

    private fun channelServiceOwner(channelId: Long): User {
        return channelRepository.findById(channelId).orElseThrow {
            ResourceNotFoundException("Canal no encontrado")
        }.owner
    }

    private fun User.adminStatus(): String = if (canAccess()) "ACTIVE" else "SUSPENDED"

    private fun User.toAdminResponse() = AdminUserResponse(
        id = requireNotNull(id),
        email = email,
        fullName = fullName,
        phoneNumber = phoneNumber,
        status = adminStatus(),
        role = role.name,
        suspendedAt = suspendedAt,
        suspensionReason = suspensionReason,
        suspendedBy = suspendedBy,
        createdAt = createdAt,
    )
}
