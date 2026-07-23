package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverCreateRequest
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverResponse
import co.edu.iub.myfirtsproyect.dto.caregiver.CaregiverUpdateRequest
import co.edu.iub.myfirtsproyect.service.CaregiverService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/caregivers")
class CaregiverController(
    private val caregiverService: CaregiverService,
) {
    @GetMapping
    fun list(authentication: Authentication): List<CaregiverResponse> = caregiverService.list(authentication.name)

    @PostMapping
    fun add(
        @Valid @RequestBody request: CaregiverCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<CaregiverResponse> = ResponseEntity.status(HttpStatus.CREATED)
        .body(caregiverService.add(authentication.name, request))

    @PutMapping("/{id}")
    fun updatePermission(
        @PathVariable id: Long,
        @Valid @RequestBody request: CaregiverUpdateRequest,
        authentication: Authentication,
    ): CaregiverResponse = caregiverService.updatePermission(id, authentication.name, request)

    @DeleteMapping("/{id}")
    fun remove(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        caregiverService.remove(id, authentication.name)
        return ResponseEntity.noContent().build()
    }
}
