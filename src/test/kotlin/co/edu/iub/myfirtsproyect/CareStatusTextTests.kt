package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CareStatusTextTests : IntegrationTestBase() {
    @Test
    fun `pending past care is overdue and skipped medicine requires a reason and warning`() {
        val (_, token) = registerAndLogin("status-text")
        val petId = createPet(token)
        val taskId = createCareTask(
            token,
            petId,
            title = "Dar medicamento",
            careType = "Medicamento",
            scheduledAt = "2020-01-01T08:00:00",
        )

        authGet("/api/care-tasks/$taskId", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overdue").value(true))
            .andExpect(jsonPath("$.displayStatus").value("Vencido"))
        authPut("/api/care-tasks/$taskId", token, """{"status":"SKIPPED"}""")
            .andExpect(status().isBadRequest)
        authPut(
            "/api/care-tasks/$taskId",
            token,
            """{"status":"SKIPPED","reason":"La mascota rechazó la dosis"}""",
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.displayStatus").value("No realizado"))
            .andExpect(jsonPath("$.warning").isNotEmpty)

        val logs = readJson(authGet("/api/care-tasks/$taskId/logs", token).andExpect(status().isOk))
        assertTrue(logs.any { it.path("detail").asText().contains("rechazó") })
    }

    @Test
    fun `rescheduled and cancelled cares do not reduce compliance`() {
        val (_, token) = registerAndLogin("status-report")
        val petId = createPet(token)
        val completed = createCareTask(token, petId, "Realizado", scheduledAt = "2026-07-20T08:00:00")
        val rescheduled = createCareTask(token, petId, "Reprogramado", scheduledAt = "2026-07-20T09:00:00")
        val cancelled = createCareTask(token, petId, "Cancelado", scheduledAt = "2026-07-20T10:00:00")
        authPut("/api/care-tasks/$completed", token, """{"status":"COMPLETED"}""").andExpect(status().isOk)
        authPost(
            "/api/care-tasks/$rescheduled/reschedule",
            token,
            """{"scheduledAt":"2026-07-25T09:00:00","reason":"Cambio de horario"}""",
        ).andExpect(status().isOk)
        authPost(
            "/api/care-tasks/$cancelled/cancel",
            token,
            """{"reason":"Ya no se necesita"}""",
        ).andExpect(status().isOk)

        val report = readJson(authGet("/api/reports/summary", token).andExpect(status().isOk))
        assertEquals(100, report.path("complianceRate").asInt())
        assertEquals(0, report.path("skipped").asInt())
    }
}
