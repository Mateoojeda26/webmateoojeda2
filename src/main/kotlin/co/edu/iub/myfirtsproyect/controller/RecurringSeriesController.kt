package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesCreateRequest
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesResponse
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesUpdateRequest
import co.edu.iub.myfirtsproyect.service.RecurringSeriesService
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
@RequestMapping("/api/series")
class RecurringSeriesController(
    private val seriesService: RecurringSeriesService,
) {
    @GetMapping
    fun list(authentication: Authentication): List<RecurringSeriesResponse> =
        seriesService.list(authentication.name)

    @PostMapping
    fun create(
        @Valid @RequestBody request: RecurringSeriesCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<RecurringSeriesResponse> = ResponseEntity.status(HttpStatus.CREATED)
        .body(seriesService.create(authentication.name, request))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, authentication: Authentication): RecurringSeriesResponse =
        seriesService.get(id, authentication.name)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: RecurringSeriesUpdateRequest,
        authentication: Authentication,
    ): RecurringSeriesResponse = seriesService.update(id, authentication.name, request)

    @PostMapping("/{id}/pause")
    fun pause(@PathVariable id: Long, authentication: Authentication): RecurringSeriesResponse =
        seriesService.pause(id, authentication.name)

    @PostMapping("/{id}/resume")
    fun resume(@PathVariable id: Long, authentication: Authentication): RecurringSeriesResponse =
        seriesService.resume(id, authentication.name)

    @DeleteMapping("/{id}")
    fun cancel(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        seriesService.cancel(id, authentication.name)
        return ResponseEntity.noContent().build()
    }
}
