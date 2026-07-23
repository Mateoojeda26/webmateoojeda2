package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.care.CareEvidenceResponse
import co.edu.iub.myfirtsproyect.service.CareEvidenceService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/care-tasks/{taskId}/images")
class CareEvidenceController(
    private val careEvidenceService: CareEvidenceService,
) {
    @GetMapping
    fun list(@PathVariable taskId: Long, authentication: Authentication): List<CareEvidenceResponse> =
        careEvidenceService.list(taskId, authentication.name)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable taskId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) note: String?,
        authentication: Authentication,
    ): ResponseEntity<CareEvidenceResponse> = ResponseEntity.status(HttpStatus.CREATED)
        .body(careEvidenceService.upload(taskId, authentication.name, file, note))

    @DeleteMapping("/{evidenceId}")
    fun delete(
        @PathVariable taskId: Long,
        @PathVariable evidenceId: Long,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        careEvidenceService.delete(taskId, evidenceId, authentication.name)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{evidenceId}/content")
    fun content(
        @PathVariable taskId: Long,
        @PathVariable evidenceId: Long,
        authentication: Authentication,
    ): ResponseEntity<ByteArray> {
        val image = careEvidenceService.loadImage(taskId, evidenceId, authentication.name)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .body(image.bytes)
    }
}
