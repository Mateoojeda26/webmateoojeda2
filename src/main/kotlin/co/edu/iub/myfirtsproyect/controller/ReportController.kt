package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.report.ReportSummaryResponse
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.service.ReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService,
) {
    @GetMapping("/summary")
    fun summary(
        @RequestParam(required = false) petId: Long?,
        @RequestParam(required = false) status: CareTaskStatus?,
        @RequestParam(required = false) careType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) completedById: Long?,
        authentication: Authentication,
    ): ReportSummaryResponse =
        reportService.summary(authentication.name, petId, status, careType, from, to, completedById)

    @GetMapping("/export")
    fun export(
        @RequestParam(required = false) petId: Long?,
        @RequestParam(required = false) status: CareTaskStatus?,
        @RequestParam(required = false) careType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) completedById: Long?,
        authentication: Authentication,
    ): ResponseEntity<ByteArray> {
        val csv = reportService.exportCsv(authentication.name, petId, status, careType, from, to, completedById)
        val bom = Char(0xFEFF).toString()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"taskora-pet-reporte.csv\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body((bom + csv).toByteArray(StandardCharsets.UTF_8))
    }
}
