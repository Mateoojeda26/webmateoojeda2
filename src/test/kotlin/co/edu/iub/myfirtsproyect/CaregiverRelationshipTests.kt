package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaregiverRelationshipTests : IntegrationTestBase() {
    @Test
    fun `one caregiver works with several owners but sees only assigned pets`() {
        val (caregiverEmail, caregiverToken) = registerAndLogin("multi-caregiver")
        val (_, ownerAToken) = registerAndLogin("multi-owner-a")
        val (_, ownerBToken) = registerAndLogin("multi-owner-b")
        val petA = createPet(ownerAToken, "Conejo compartido")
        val privatePet = createPet(ownerAToken, "Mascota privada")
        val petB = createPet(ownerBToken, "Ave compartida")
        val taskA = createCareTask(ownerAToken, petA, "Cuidado A")
        val privateTask = createCareTask(ownerAToken, privatePet, "Cuidado privado")
        val taskB = createCareTask(ownerBToken, petB, "Cuidado B")

        authPost("/api/caregivers", ownerAToken, """{"petId":$petA,"caregiverEmail":"$caregiverEmail","permission":"VIEWER"}""")
            .andExpect(status().isCreated)
        authPost("/api/caregivers", ownerBToken, """{"petId":$petB,"caregiverEmail":"$caregiverEmail","permission":"EDITOR"}""")
            .andExpect(status().isCreated)

        val tasks = readJson(authGet("/api/care-tasks", caregiverToken).andExpect(status().isOk))
        val ids = tasks.values().asSequence().map { it.path("id").asLong() }.toSet()
        assertTrue(taskA in ids)
        assertTrue(taskB in ids)
        assertTrue(privateTask !in ids)
        authGet("/api/care-tasks/$privateTask", caregiverToken).andExpect(status().isNotFound)
    }

    @Test
    fun `one pet can have several caregivers with different permissions`() {
        val (_, ownerToken) = registerAndLogin("many-caregivers-owner")
        val (viewerEmail, _) = registerAndLogin("many-caregivers-viewer")
        val (editorEmail, _) = registerAndLogin("many-caregivers-editor")
        val petId = createPet(ownerToken)

        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$viewerEmail","permission":"VIEWER"}""")
            .andExpect(status().isCreated)
        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$editorEmail","permission":"EDITOR"}""")
            .andExpect(status().isCreated)

        val assignments = readJson(authGet("/api/caregivers", ownerToken).andExpect(status().isOk))
        assertEquals(2, assignments.size())
        val values = assignments.values().asSequence().toList()
        assertEquals(setOf("VIEWER", "EDITOR"), values.map { it.path("permission").asText() }.toSet())
        assertTrue(values.all { it.path("ownerId").asLong() > 0 })
    }
}
