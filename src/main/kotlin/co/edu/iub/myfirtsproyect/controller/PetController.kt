package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.pet.PetCreateRequest
import co.edu.iub.myfirtsproyect.dto.pet.PetResponse
import co.edu.iub.myfirtsproyect.dto.pet.PetUpdateRequest
import co.edu.iub.myfirtsproyect.service.PetService
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/pets")
class PetController(
    private val petService: PetService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: PetCreateRequest, authentication: Authentication): ResponseEntity<PetResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(petService.create(authentication.name, request))

    @GetMapping
    fun list(authentication: Authentication): List<PetResponse> = petService.list(authentication.name)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, authentication: Authentication): PetResponse =
        petService.get(id, authentication.name)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: PetUpdateRequest,
        authentication: Authentication,
    ): PetResponse = petService.update(id, authentication.name, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        petService.archive(id, authentication.name)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication,
    ): PetResponse = petService.uploadImage(id, authentication.name, file)

    @DeleteMapping("/{id}/image")
    fun removeImage(@PathVariable id: Long, authentication: Authentication): PetResponse =
        petService.removeImage(id, authentication.name)

    @GetMapping("/{id}/image")
    fun image(@PathVariable id: Long, authentication: Authentication): ResponseEntity<ByteArray> {
        val image = petService.loadImage(id, authentication.name)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .body(image.bytes)
    }
}
