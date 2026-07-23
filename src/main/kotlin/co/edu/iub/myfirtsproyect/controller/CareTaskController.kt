package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.care.CareTaskCreateRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskLogResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskResponse
import co.edu.iub.myfirtsproyect.dto.care.CareTaskUpdateRequest
import co.edu.iub.myfirtsproyect.dto.care.DailyRoutineCreateRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskReasonRequest
import co.edu.iub.myfirtsproyect.dto.care.CareTaskRescheduleRequest
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.service.CareTaskService
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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

@RestController
@RequestMapping("/api/care-tasks")
class CareTaskController(
    private val careTaskService: CareTaskService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody request: CareTaskCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<CareTaskResponse> = ResponseEntity.status(HttpStatus.CREATED)
        .body(careTaskService.create(authentication.name, request))

    @PostMapping("/daily-routines")
    fun createDailyRoutine(
        @Valid @RequestBody request: DailyRoutineCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<List<CareTaskResponse>> = ResponseEntity.status(HttpStatus.CREATED)
        .body(careTaskService.createDailyRoutine(authentication.name, request))

    @GetMapping
    fun list(
        @RequestParam(required = false) petId: Long?,
        @RequestParam(required = false) status: CareTaskStatus?,
        @RequestParam(required = false) careType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        authentication: Authentication,
    ): List<CareTaskResponse> = careTaskService.list(authentication.name, petId, status, careType, from, to)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, authentication: Authentication): CareTaskResponse =
        careTaskService.get(id, authentication.name)

    @GetMapping("/{id}/logs")
    fun logs(@PathVariable id: Long, authentication: Authentication): List<CareTaskLogResponse> =
        careTaskService.logs(id, authentication.name)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: CareTaskUpdateRequest,
        authentication: Authentication,
    ): CareTaskResponse = careTaskService.update(id, authentication.name, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        careTaskService.delete(id, authentication.name)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: Long,
        @Valid @RequestBody request: CareTaskReasonRequest,
        authentication: Authentication,
    ): CareTaskResponse = careTaskService.cancel(id, authentication.name, request.reason)

    @PostMapping("/{id}/reschedule")
    fun reschedule(
        @PathVariable id: Long,
        @Valid @RequestBody request: CareTaskRescheduleRequest,
        authentication: Authentication,
    ): CareTaskResponse = careTaskService.update(
        id,
        authentication.name,
        CareTaskUpdateRequest(scheduledAt = request.scheduledAt, reason = request.reason),
    )
}
