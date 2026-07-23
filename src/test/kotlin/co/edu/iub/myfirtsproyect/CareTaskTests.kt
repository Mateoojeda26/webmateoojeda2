package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CareTaskTests : IntegrationTestBase() {
    @Test
    fun `owner runs the full task lifecycle with audit trail`() {
        val (_, token) = registerAndLogin("ct")
        val petId = createPet(token)
        val taskId = createCareTask(token, petId, title = "Dar medicina")

        authPut("/api/care-tasks/$taskId", token, """{"scheduledAt":"2026-07-20T10:30:00"}""")
            .andExpect(status().isOk)

        val completed = readJson(
            authPut("/api/care-tasks/$taskId", token, """{"status":"COMPLETED"}""").andExpect(status().isOk),
        )
        assertEquals("COMPLETED", completed.path("status").asText())
        assertTrue(!completed.path("completedByName").asText().isNullOrBlank(), "Debe registrar quién completó")
        assertTrue(!completed.path("completedAt").asText().isNullOrBlank(), "Debe registrar cuándo")

        val logs = readJson(authGet("/api/care-tasks/$taskId/logs", token).andExpect(status().isOk))
        val actions = logs.values().map { it.path("action").asText() }
        assertTrue("COMPLETED" in actions, "Debe existir log de completado")
        assertTrue("RESCHEDULED" in actions, "Debe existir log de reprogramación")
    }

    @Test
    fun `rejects invalid status transitions and rescheduling closed tasks`() {
        val (_, token) = registerAndLogin("cttrans")
        val petId = createPet(token)
        val taskId = createCareTask(token, petId)

        authPut("/api/care-tasks/$taskId", token, """{"status":"SKIPPED","reason":"La mascota no quiso comer"}""").andExpect(status().isOk)
        authPut("/api/care-tasks/$taskId", token, """{"status":"COMPLETED"}""").andExpect(status().isBadRequest)
        authPut("/api/care-tasks/$taskId", token, """{"scheduledAt":"2026-08-01T08:00:00"}""").andExpect(status().isBadRequest)
    }

    @Test
    fun `soft deletes a task keeping it out of the agenda`() {
        val (_, token) = registerAndLogin("ctdel")
        val petId = createPet(token)
        val taskId = createCareTask(token, petId)
        authDelete("/api/care-tasks/$taskId", token).andExpect(status().isNoContent)
        val tasks = readJson(authGet("/api/care-tasks", token).andExpect(status().isOk))
        assertEquals(0, tasks.count { it.path("id").asLong() == taskId })
    }

    @Test
    fun `filters tasks by pet, status, type and date range`() {
        val (_, token) = registerAndLogin("ctfilter")
        val petA = createPet(token, "Ana")
        val petB = createPet(token, "Bruno")
        val feedTask = createCareTask(token, petA, title = "Comida A", careType = "Alimentación", scheduledAt = "2026-07-20T08:00:00")
        createCareTask(token, petA, title = "Paseo A", careType = "Paseo", scheduledAt = "2026-07-22T08:00:00")
        createCareTask(token, petB, title = "Comida B", careType = "Alimentación", scheduledAt = "2026-07-25T08:00:00")
        authPut("/api/care-tasks/$feedTask", token, """{"status":"COMPLETED"}""").andExpect(status().isOk)

        val byPet = readJson(authGet("/api/care-tasks?petId=$petA", token).andExpect(status().isOk))
        assertEquals(2, byPet.size())

        val byStatus = readJson(authGet("/api/care-tasks?status=COMPLETED", token).andExpect(status().isOk))
        assertEquals(1, byStatus.size())
        assertEquals("Comida A", byStatus[0].path("title").asText())

        val byType = readJson(authGet("/api/care-tasks?careType=Paseo", token).andExpect(status().isOk))
        assertEquals(1, byType.size())

        val byRange = readJson(
            authGet("/api/care-tasks?from=2026-07-21&to=2026-07-23", token).andExpect(status().isOk),
        )
        assertEquals(1, byRange.size())
        assertEquals("Paseo A", byRange[0].path("title").asText())
    }

    @Test
    fun `tasks are isolated between unrelated users`() {
        val (_, ownerToken) = registerAndLogin("ctiso")
        val (_, otherToken) = registerAndLogin("ctiso2")
        val petId = createPet(ownerToken)
        val taskId = createCareTask(ownerToken, petId)

        authGet("/api/care-tasks/$taskId", otherToken).andExpect(status().isNotFound)
        val tasks = readJson(authGet("/api/care-tasks", otherToken).andExpect(status().isOk))
        assertEquals(0, tasks.count { it.path("id").asLong() == taskId })
    }

    @Test
    fun `uploads, lists and deletes photographic evidence`() {
        val (_, token) = registerAndLogin("ctev")
        val (_, strangerToken) = registerAndLogin("ctevstranger")
        val petId = createPet(token)
        val taskId = createCareTask(token, petId)
        val image = MockMultipartFile("file", "prueba.png", "image/png", byteArrayOf(1, 2, 3))

        val uploaded = readJson(
            mockMvc.perform(
                multipart("/api/care-tasks/$taskId/images").file(image).param("note", "Comió todo")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isCreated),
        )
        assertEquals("Comió todo", uploaded.path("note").asText())

        val list = readJson(authGet("/api/care-tasks/$taskId/images", token).andExpect(status().isOk))
        assertEquals(1, list.size())
        val contentPath = list[0].path("imageUrl").asText()
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(contentPath))
            .andExpect(status().isUnauthorized)
        authGet(contentPath, token)
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
        authGet(contentPath, strangerToken).andExpect(status().isForbidden)

        authDelete("/api/care-tasks/$taskId/images/${uploaded.path("id").asLong()}", token)
            .andExpect(status().isNoContent)
        val after = readJson(authGet("/api/care-tasks/$taskId/images", token).andExpect(status().isOk))
        assertEquals(0, after.size())
    }

    @Test
    fun `rejects invalid task payloads`() {
        val (_, token) = registerAndLogin("ctbad")
        val petId = createPet(token)
        authPost("/api/care-tasks", token, """{"petId":$petId,"title":"","careType":"Paseo","scheduledAt":"2026-07-20T08:00:00"}""")
            .andExpect(status().isBadRequest)
        authPost("/api/care-tasks", token, """{"petId":$petId,"title":"Sin fecha","careType":"Paseo"}""")
            .andExpect(status().isBadRequest)
    }
}
