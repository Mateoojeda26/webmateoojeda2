package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaregiverPermissionTests : IntegrationTestBase() {
    private fun assign(ownerToken: String, petId: Long, caregiverEmail: String, permission: String): Long {
        val result = authPost(
            "/api/caregivers",
            ownerToken,
            """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"$permission"}""",
        ).andExpect(status().isCreated)
        return readJson(result).path("id").asLong()
    }

    @Test
    fun `owner assigns, edits permission and revokes a caregiver`() {
        val (_, ownerToken) = registerAndLogin("cgowner")
        val (caregiverEmail, _) = registerAndLogin("cgeditor")
        val petId = createPet(ownerToken)
        val accessId = assign(ownerToken, petId, caregiverEmail, "VIEWER")

        authGet("/api/caregivers", ownerToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].permission").value("VIEWER"))

        authPut("/api/caregivers/$accessId", ownerToken, """{"permission":"EDITOR"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.permission").value("EDITOR"))

        authDelete("/api/caregivers/$accessId", ownerToken).andExpect(status().isNoContent)
        val list = readJson(authGet("/api/caregivers", ownerToken).andExpect(status().isOk))
        assertEquals(0, list.size())
    }

    @Test
    fun `rejects invalid permissions and duplicated or self assignments`() {
        val (ownerEmail, ownerToken) = registerAndLogin("cgbad")
        val (caregiverEmail, _) = registerAndLogin("cgbad2")
        val petId = createPet(ownerToken)

        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"ADMIN"}""")
            .andExpect(status().isBadRequest)
        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$ownerEmail","permission":"EDITOR"}""")
            .andExpect(status().isConflict)
        assign(ownerToken, petId, caregiverEmail, "EDITOR")
        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"EDITOR"}""")
            .andExpect(status().isConflict)
    }

    @Test
    fun `viewer can read shared data but cannot modify anything`() {
        val (_, ownerToken) = registerAndLogin("vwowner")
        val (viewerEmail, viewerToken) = registerAndLogin("viewer")
        val petId = createPet(ownerToken)
        val taskId = createCareTask(ownerToken, petId)
        assign(ownerToken, petId, viewerEmail, "VIEWER")

        val tasks = readJson(authGet("/api/care-tasks", viewerToken).andExpect(status().isOk))
        assertTrue(tasks.any { it.path("id").asLong() == taskId })
        authGet("/api/pets", viewerToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(petId))
            .andExpect(jsonPath("$[0].accessLevel").value("VIEWER"))
        authGet("/api/care-tasks/$taskId", viewerToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessLevel").value("VIEWER"))
        authGet("/api/care-tasks/$taskId/images", viewerToken).andExpect(status().isOk)
        authGet("/api/care-tasks/$taskId/logs", viewerToken).andExpect(status().isOk)

        authPut("/api/care-tasks/$taskId", viewerToken, """{"status":"COMPLETED"}""").andExpect(status().isForbidden)
        authPut("/api/care-tasks/$taskId", viewerToken, """{"scheduledAt":"2026-07-21T10:00:00"}""").andExpect(status().isForbidden)
        authPut("/api/care-tasks/$taskId", viewerToken, """{"title":"Cambiado"}""").andExpect(status().isForbidden)
        authDelete("/api/care-tasks/$taskId", viewerToken).andExpect(status().isForbidden)

        val image = MockMultipartFile("file", "foto.png", "image/png", byteArrayOf(9, 9))
        mockMvc.perform(
            multipart("/api/care-tasks/$taskId/images").file(image).header("Authorization", "Bearer $viewerToken"),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `editor can complete and reschedule but cannot edit or delete the task`() {
        val (_, ownerToken) = registerAndLogin("edowner")
        val (editorEmail, editorToken) = registerAndLogin("editor")
        val petId = createPet(ownerToken)
        val taskId = createCareTask(ownerToken, petId)
        val secondTaskId = createCareTask(ownerToken, petId, title = "Paseo", scheduledAt = "2026-07-20T17:00:00")
        assign(ownerToken, petId, editorEmail, "EDITOR")

        authGet("/api/pets", editorToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(petId))
            .andExpect(jsonPath("$[0].accessLevel").value("EDITOR"))

        val rescheduled = readJson(
            authPut("/api/care-tasks/$taskId", editorToken, """{"scheduledAt":"2026-07-20T09:30:00"}""")
                .andExpect(status().isOk),
        )
        assertTrue(rescheduled.path("scheduledAt").asText().startsWith("2026-07-20T09:30"))
        authPut("/api/care-tasks/$taskId", editorToken, """{"status":"COMPLETED"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))

        authPut("/api/care-tasks/$secondTaskId", editorToken, """{"title":"No permitido"}""").andExpect(status().isForbidden)
        authDelete("/api/care-tasks/$secondTaskId", editorToken).andExpect(status().isForbidden)

        val image = MockMultipartFile("file", "evidencia.png", "image/png", byteArrayOf(5, 5))
        mockMvc.perform(
            multipart("/api/care-tasks/$secondTaskId/images").file(image).header("Authorization", "Bearer $editorToken"),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `revoked caregivers and strangers lose access immediately`() {
        val (_, ownerToken) = registerAndLogin("rvowner")
        val (caregiverEmail, caregiverToken) = registerAndLogin("revoked")
        val (_, strangerToken) = registerAndLogin("stranger")
        val petId = createPet(ownerToken)
        val taskId = createCareTask(ownerToken, petId)
        val accessId = assign(ownerToken, petId, caregiverEmail, "EDITOR")

        authGet("/api/care-tasks/$taskId", caregiverToken).andExpect(status().isOk)
        authDelete("/api/caregivers/$accessId", ownerToken).andExpect(status().isNoContent)

        val pets = readJson(authGet("/api/pets", caregiverToken).andExpect(status().isOk))
        assertEquals(0, pets.count { it.path("id").asLong() == petId })
        authGet("/api/care-tasks/$taskId", caregiverToken).andExpect(status().isNotFound)
        authPut("/api/care-tasks/$taskId", caregiverToken, """{"status":"COMPLETED"}""").andExpect(status().isNotFound)
        val tasks = readJson(authGet("/api/care-tasks", caregiverToken).andExpect(status().isOk))
        assertEquals(0, tasks.count { it.path("id").asLong() == taskId })

        authGet("/api/care-tasks/$taskId", strangerToken).andExpect(status().isNotFound)
    }

    @Test
    fun `only the owner manages caregivers`() {
        val (_, ownerToken) = registerAndLogin("mgowner")
        val (caregiverEmail, caregiverToken) = registerAndLogin("mgcaregiver")
        val petId = createPet(ownerToken)
        val accessId = assign(ownerToken, petId, caregiverEmail, "EDITOR")

        authPut("/api/caregivers/$accessId", caregiverToken, """{"permission":"EDITOR"}""").andExpect(status().isNotFound)
        authDelete("/api/caregivers/$accessId", caregiverToken).andExpect(status().isNotFound)
    }
}
