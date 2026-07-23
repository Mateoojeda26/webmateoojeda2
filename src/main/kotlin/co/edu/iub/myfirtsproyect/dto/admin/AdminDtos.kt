package co.edu.iub.myfirtsproyect.dto.admin

import co.edu.iub.myfirtsproyect.dto.care.CareTaskResponse
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverResponse
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelResponse
import co.edu.iub.myfirtsproyect.dto.pet.PetResponse
import co.edu.iub.myfirtsproyect.dto.report.ReportSummaryResponse
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class AdminDashboardResponse(
    val activeUsers: Long,
    val suspendedUsers: Long,
    val registeredPets: Long,
    val activeCaregivers: Long,
    val pendingCareTasks: Long,
    val overdueCareTasks: Long,
    val failedReminders: Long,
)

data class AdminUserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val phoneNumber: String?,
    val status: String,
    val role: String,
    val suspendedAt: LocalDateTime?,
    val suspensionReason: String?,
    val suspendedBy: String?,
    val createdAt: LocalDateTime,
)

data class AdminUpdateUserRequest(
    @field:Email val email: String? = null,
    val fullName: String? = null,
    val phoneNumber: String? = null,
)

data class AdminSuspendUserRequest(
    @field:NotBlank val reason: String,
)

data class AdminDeleteUserRequest(
    @field:Email
    @field:NotBlank
    val confirmationEmail: String,
)

data class AdminChannelConfiguration(
    val telegramConnected: Boolean,
    val gmailConnected: Boolean,
)

data class AdminHistoryItem(
    val taskId: Long,
    val taskTitle: String,
    val action: String,
    val actorName: String,
    val detail: String?,
    val createdAt: LocalDateTime,
)

data class AdminUserDetailResponse(
    val user: AdminUserResponse,
    val pets: List<PetResponse>,
    val caregivers: List<CaregiverResponse>,
    val routines: List<RecurringSeriesResponse>,
    val cares: List<CareTaskResponse>,
    val channels: List<NotificationChannelResponse>,
    val history: List<AdminHistoryItem>,
    val report: ReportSummaryResponse,
    val configuration: AdminChannelConfiguration,
)

data class AdminAuditResponse(
    val id: Long,
    val adminEmail: String,
    val action: String,
    val resourceType: String,
    val resourceIdentifier: String,
    val description: String,
    val createdAt: LocalDateTime,
)

data class AdminReminderResponse(
    val id: Long,
    val userEmail: String,
    val petName: String,
    val taskTitle: String,
    val channelType: String,
    val scheduledFor: LocalDateTime,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
)
