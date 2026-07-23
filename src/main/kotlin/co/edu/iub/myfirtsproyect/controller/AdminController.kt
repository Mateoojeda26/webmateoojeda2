package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.admin.AdminDeleteUserRequest
import co.edu.iub.myfirtsproyect.dto.admin.AdminSuspendUserRequest
import co.edu.iub.myfirtsproyect.dto.admin.AdminUpdateUserRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskCreateRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskUpdateRequest
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverCreateRequest
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverUpdateRequest
import co.edu.iub.myfirtsproyect.dto.pet.PetCreateRequest
import co.edu.iub.myfirtsproyect.dto.pet.PetUpdateRequest
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesCreateRequest
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesUpdateRequest
import co.edu.iub.myfirtsproyect.service.AdminService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
) {
    @GetMapping("/dashboard")
    fun dashboard() = adminService.dashboard()

    @GetMapping("/users")
    fun users(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?,
    ) = adminService.listUsers(search, status)

    @GetMapping("/users/{id}")
    fun user(@PathVariable id: Long) = adminService.userDetail(id)

    @PutMapping("/users/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminUpdateUserRequest,
        authentication: Authentication,
    ) = adminService.updateUser(authentication.name, id, request)

    @PostMapping("/users/{id}/suspend")
    fun suspendUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminSuspendUserRequest,
        authentication: Authentication,
    ) = adminService.suspendUser(authentication.name, id, request)

    @PostMapping("/users/{id}/reactivate")
    fun reactivateUser(@PathVariable id: Long, authentication: Authentication) =
        adminService.reactivateUser(authentication.name, id)

    @PostMapping("/users/{id}/password-reset")
    fun passwordReset(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminService.startPasswordReset(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/users/{id}")
    fun deleteUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminDeleteUserRequest,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        adminService.deleteUser(authentication.name, id, request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/{userId}/pets")
    fun pets(@PathVariable userId: Long) = adminService.listPets(userId)

    @PostMapping("/users/{userId}/pets")
    fun createPet(
        @PathVariable userId: Long,
        @Valid @RequestBody request: PetCreateRequest,
        authentication: Authentication,
    ) = ResponseEntity.status(HttpStatus.CREATED).body(adminService.createPet(authentication.name, userId, request))

    @GetMapping("/pets/{id}")
    fun pet(@PathVariable id: Long) = adminService.getPet(id)

    @PutMapping("/pets/{id}")
    fun updatePet(
        @PathVariable id: Long,
        @Valid @RequestBody request: PetUpdateRequest,
        authentication: Authentication,
    ) = adminService.updatePet(authentication.name, id, request)

    @DeleteMapping("/pets/{id}")
    fun deletePet(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminService.deletePet(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/{userId}/caregivers")
    fun caregivers(@PathVariable userId: Long) = adminService.userDetail(userId).caregivers

    @PostMapping("/users/{userId}/caregivers")
    fun createCaregiver(
        @PathVariable userId: Long,
        @Valid @RequestBody request: CaregiverCreateRequest,
        authentication: Authentication,
    ) = ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCaregiver(authentication.name, userId, request))

    @PutMapping("/caregivers/{id}")
    fun updateCaregiver(
        @PathVariable id: Long,
        @Valid @RequestBody request: CaregiverUpdateRequest,
        authentication: Authentication,
    ) = adminService.updateCaregiver(authentication.name, id, request)

    @DeleteMapping("/caregivers/{id}")
    fun deleteCaregiver(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminService.deleteCaregiver(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/{userId}/care-tasks")
    fun careTasks(@PathVariable userId: Long) = adminService.listCareTasks(userId)

    @PostMapping("/users/{userId}/care-tasks")
    fun createCareTask(
        @PathVariable userId: Long,
        @Valid @RequestBody request: CareTaskCreateRequest,
        authentication: Authentication,
    ) = ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCareTask(authentication.name, userId, request))

    @GetMapping("/care-tasks/{id}")
    fun careTask(@PathVariable id: Long) = adminService.getCareTask(id)

    @PutMapping("/care-tasks/{id}")
    fun updateCareTask(
        @PathVariable id: Long,
        @RequestBody request: CareTaskUpdateRequest,
        authentication: Authentication,
    ) = adminService.updateCareTask(authentication.name, id, request)

    @DeleteMapping("/care-tasks/{id}")
    fun deleteCareTask(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminService.deleteCareTask(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/care-tasks/{id}/logs")
    fun careTaskLogs(@PathVariable id: Long) = adminService.careTaskLogs(id)

    @GetMapping("/care-tasks/{id}/evidence")
    fun careTaskEvidence(@PathVariable id: Long) = adminService.careTaskEvidence(id)

    @GetMapping("/users/{userId}/series")
    fun series(@PathVariable userId: Long) = adminService.userDetail(userId).routines

    @PostMapping("/users/{userId}/series")
    fun createSeries(
        @PathVariable userId: Long,
        @Valid @RequestBody request: RecurringSeriesCreateRequest,
        authentication: Authentication,
    ) = ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSeries(authentication.name, userId, request))

    @PutMapping("/series/{id}")
    fun updateSeries(
        @PathVariable id: Long,
        @Valid @RequestBody request: RecurringSeriesUpdateRequest,
        authentication: Authentication,
    ) = adminService.updateSeries(authentication.name, id, request)

    @DeleteMapping("/series/{id}")
    fun deleteSeries(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminService.deleteSeries(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/{userId}/channels")
    fun channels(@PathVariable userId: Long) = adminService.listChannels(userId)

    @DeleteMapping("/channels/{id}")
    fun unlinkChannel(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminService.unlinkChannel(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/{userId}/report")
    fun report(@PathVariable userId: Long) = adminService.report(userId)

    @GetMapping("/users/{userId}/report/export")
    fun exportReport(@PathVariable userId: Long): ResponseEntity<ByteArray> {
        val csv = adminService.exportReport(userId)
        val bom = Char(0xFEFF).toString()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"taskora-pet-admin-reporte.csv\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body((bom + csv).toByteArray(StandardCharsets.UTF_8))
    }

    @GetMapping("/reminders/failed")
    fun failedReminders() = adminService.failedReminders()

    @PostMapping("/reminders/{id}/retry")
    fun retryReminder(@PathVariable id: Long, authentication: Authentication) =
        adminService.retryReminder(authentication.name, id)

    @GetMapping("/audit")
    fun audit() = adminService.audit()
}
