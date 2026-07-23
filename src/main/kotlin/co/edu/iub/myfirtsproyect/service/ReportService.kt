package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.care.CareTaskResponse
import co.edu.iub.myfirtsproyect.dto.report.PetReportRow
import co.edu.iub.myfirtsproyect.dto.report.ReportSummaryResponse
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ReportService(
    private val careTaskService: CareTaskService,
    private val clock: Clock,
) {
    private val csvDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    @Transactional(readOnly = true)
    fun summary(
        email: String,
        petId: Long? = null,
        status: CareTaskStatus? = null,
        careType: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
        completedById: Long? = null,
    ): ReportSummaryResponse {
        val tasks = filteredTasks(email, petId, status, careType, from, to, completedById)
        val perPet = tasks.groupBy { it.petId to it.petName }.map { (key, petTasks) ->
            val completed = petTasks.count { it.status == CareTaskStatus.COMPLETED }
            val skipped = petTasks.count { it.status == CareTaskStatus.SKIPPED }
            PetReportRow(
                petId = key.first,
                petName = key.second,
                total = petTasks.size,
                pending = petTasks.count { it.status == CareTaskStatus.PENDING },
                overdue = petTasks.count { it.overdue },
                completed = completed,
                skipped = skipped,
                complianceRate = complianceRate(completed, skipped),
            )
        }.sortedBy { it.petName.lowercase() }
        val completed = tasks.count { it.status == CareTaskStatus.COMPLETED }
        val skipped = tasks.count { it.status == CareTaskStatus.SKIPPED }
        return ReportSummaryResponse(
            total = tasks.size,
            pending = tasks.count { it.status == CareTaskStatus.PENDING },
            overdue = tasks.count { it.overdue },
            completed = completed,
            skipped = skipped,
            complianceRate = complianceRate(completed, skipped),
            perPet = perPet,
        )
    }

    @Transactional(readOnly = true)
    fun exportCsv(
        email: String,
        petId: Long? = null,
        status: CareTaskStatus? = null,
        careType: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
        completedById: Long? = null,
    ): String {
        val tasks = filteredTasks(email, petId, status, careType, from, to, completedById)
        val header = listOf(
            "Mascota", "Cuidado", "Tipo", "Prioridad", "Estado",
            "Programado", "Realizado por", "Fecha de la acción",
        )
        val rows = tasks.map { task ->
            listOf(
                task.petName,
                task.title,
                task.careType,
                task.priority.name,
                statusLabel(task.status),
                task.scheduledAt.format(csvDateFormatter),
                task.completedByName ?: "",
                task.completedAt?.format(csvDateFormatter) ?: "",
            )
        }
        return (listOf(header) + rows).joinToString("\r\n") { row ->
            row.joinToString(",") { escapeCsv(it) }
        }
    }

    private fun filteredTasks(
        email: String,
        petId: Long?,
        status: CareTaskStatus?,
        careType: String?,
        from: LocalDate?,
        to: LocalDate?,
        completedById: Long?,
    ): List<CareTaskResponse> {
        if (from != null && to != null && from.isAfter(to)) {
            throw InvalidRequestException("La fecha inicial no puede ser posterior a la final")
        }
        val tasks = careTaskService.list(email, petId, status, careType, from, to)
        return if (completedById == null) tasks else tasks.filter { it.completedById == completedById }
    }

    private fun complianceRate(completed: Int, skipped: Int): Int {
        val closed = completed + skipped
        return if (closed == 0) 0 else Math.round(completed * 100f / closed)
    }

    private fun statusLabel(status: CareTaskStatus): String = when (status) {
        CareTaskStatus.PENDING -> "Pendiente"
        CareTaskStatus.COMPLETED -> "Realizado"
        CareTaskStatus.SKIPPED -> "No realizado"
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\r", " ").replace("\n", " ")
        if (clean.isNotEmpty() && clean.first() in charArrayOf('=', '+', '-', '@', '\t')) {
            clean = "'$clean"
        }
        return "\"${clean.replace("\"", "\"\"")}\""
    }
}
