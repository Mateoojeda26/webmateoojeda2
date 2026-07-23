package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportTests : IntegrationTestBase() {
    @Test
    fun `summary shows totals, compliance and per pet breakdown respecting filters`() {
        val (_, token) = registerAndLogin("rep")
        val petA = createPet(token, "Ana")
        val petB = createPet(token, "Bruno")
        val t1 = createCareTask(token, petA, title = "Comida", careType = "Alimentación", scheduledAt = "2026-07-20T08:00:00")
        val t2 = createCareTask(token, petA, title = "Paseo", careType = "Paseo", scheduledAt = "2026-07-21T08:00:00")
        createCareTask(token, petB, title = "Comida B", careType = "Alimentación", scheduledAt = "2026-07-22T08:00:00")
        authPut("/api/care-tasks/$t1", token, """{"status":"COMPLETED"}""").andExpect(status().isOk)
        authPut("/api/care-tasks/$t2", token, """{"status":"SKIPPED","reason":"Lluvia fuerte"}""").andExpect(status().isOk)

        val summary = readJson(authGet("/api/reports/summary", token).andExpect(status().isOk))
        assertEquals(3, summary.path("total").asInt())
        assertEquals(1, summary.path("completed").asInt())
        assertEquals(1, summary.path("skipped").asInt())
        assertEquals(1, summary.path("pending").asInt())
        assertEquals(50, summary.path("complianceRate").asInt())
        assertEquals(2, summary.path("perPet").size())

        val filtered = readJson(authGet("/api/reports/summary?petId=$petA", token).andExpect(status().isOk))
        assertEquals(2, filtered.path("total").asInt())
        assertEquals(1, filtered.path("perPet").size())
        assertEquals("Ana", filtered.path("perPet").get(0).path("petName").asText())

        val byType = readJson(authGet("/api/reports/summary?careType=Alimentación", token).andExpect(status().isOk))
        assertEquals(2, byType.path("total").asInt())
    }

    @Test
    fun `caregiver filter reports only what that person completed`() {
        val (_, ownerToken) = registerAndLogin("repcg")
        val (caregiverEmail, caregiverToken) = registerAndLogin("repcg2")
        val petId = createPet(ownerToken)
        val t1 = createCareTask(ownerToken, petId, title = "Por cuidador", scheduledAt = "2026-07-20T08:00:00")
        createCareTask(ownerToken, petId, title = "Pendiente", scheduledAt = "2026-07-21T08:00:00")
        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"EDITOR"}""")
            .andExpect(status().isCreated)
        authPut("/api/care-tasks/$t1", caregiverToken, """{"status":"COMPLETED"}""").andExpect(status().isOk)

        val caregivers = readJson(authGet("/api/caregivers", ownerToken).andExpect(status().isOk))
        val caregiverId = caregivers.get(0).path("caregiverId").asLong()
        val summary = readJson(
            authGet("/api/reports/summary?completedById=$caregiverId", ownerToken).andExpect(status().isOk),
        )
        assertEquals(1, summary.path("total").asInt())
        assertEquals(1, summary.path("completed").asInt())
    }

    @Test
    fun `csv export respects filters and escapes dangerous content`() {
        val (_, token) = registerAndLogin("repcsv")
        val petId = createPet(token, "Kira")
        val result = authPost(
            "/api/care-tasks",
            token,
            """{"petId":$petId,"title":"=SUMA(1;2) con \"comillas\"","careType":"Paseo","scheduledAt":"2026-07-20T08:00:00"}""",
        ).andExpect(status().isCreated)
        readJson(result)
        createCareTask(token, petId, title = "Fuera de rango", scheduledAt = "2026-09-01T08:00:00")

        val response = authGet("/api/reports/export?from=2026-07-01&to=2026-07-31", token)
            .andExpect(status().isOk)
            .andReturn().response
        val csv = response.getContentAsString(Charsets.UTF_8)
        assertTrue(response.contentType!!.contains("text/csv"))
        assertTrue(csv.contains("Mascota"), "Debe incluir encabezados")
        assertTrue(csv.contains("'=SUMA(1;2)"), "Debe neutralizar fórmulas con apóstrofo")
        assertTrue(csv.contains("\"\"comillas\"\""), "Debe escapar comillas dobles")
        assertTrue(!csv.contains("Fuera de rango"), "Debe respetar el filtro de fechas")
    }

    @Test
    fun `rejects an inverted date range and returns empty summaries gracefully`() {
        val (_, token) = registerAndLogin("repbad")
        authGet("/api/reports/summary?from=2026-08-01&to=2026-07-01", token).andExpect(status().isBadRequest)
        val empty = readJson(authGet("/api/reports/summary", token).andExpect(status().isOk))
        assertEquals(0, empty.path("total").asInt())
        assertEquals(0, empty.path("complianceRate").asInt())
        assertEquals(0, empty.path("perPet").size())
    }
}
