package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetTests : IntegrationTestBase() {
    @Test
    fun `owner can create, read, update and archive pets`() {
        val (_, token) = registerAndLogin("pets")
        val petId = createPet(token, "Luna")

        authGet("/api/pets/$petId", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Luna"))

        authPut("/api/pets/$petId", token, """{"name":"Luna Actualizada","breed":"Criolla","color":"Café"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Luna Actualizada"))
            .andExpect(jsonPath("$.breed").value("Criolla"))
            .andExpect(jsonPath("$.color").value("Café"))

        authDelete("/api/pets/$petId", token).andExpect(status().isNoContent)
        authGet("/api/pets/$petId", token).andExpect(status().isNotFound)
        val list = readJson(authGet("/api/pets", token).andExpect(status().isOk))
        assertTrue(list.none { it.path("id").asLong() == petId })
    }

    @Test
    fun `pets are isolated between users`() {
        val (_, ownerToken) = registerAndLogin("petowner")
        val (_, strangerToken) = registerAndLogin("petstranger")
        val petId = createPet(ownerToken, "Thor")

        authGet("/api/pets/$petId", strangerToken).andExpect(status().isNotFound)
        authPut("/api/pets/$petId", strangerToken, """{"name":"Robado"}""").andExpect(status().isNotFound)
        authDelete("/api/pets/$petId", strangerToken).andExpect(status().isNotFound)
        val list = readJson(authGet("/api/pets", strangerToken).andExpect(status().isOk))
        assertEquals(0, list.count { it.path("id").asLong() == petId })
    }

    @Test
    fun `owner can attach and remove the pet photo`() {
        val (_, token) = registerAndLogin("petphoto")
        val (caregiverEmail, caregiverToken) = registerAndLogin("petphotocaregiver")
        val (_, strangerToken) = registerAndLogin("petphotostranger")
        val petId = createPet(token, "Nala")
        val image = MockMultipartFile("file", "nala.png", "image/png", byteArrayOf(1, 2, 3, 4))

        val uploaded = readJson(
            mockMvc.perform(
                multipart("/api/pets/$petId/image").file(image).header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk),
        )
        assertEquals("/api/pets/$petId/image", uploaded.path("photoUrl").asText())
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pets/$petId/image"))
            .andExpect(status().isUnauthorized)
        authGet("/api/pets/$petId/image", token)
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
        authGet("/api/pets/$petId/image", strangerToken).andExpect(status().isNotFound)
        authPost(
            "/api/caregivers",
            token,
            """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"VIEWER"}""",
        ).andExpect(status().isCreated)
        authGet("/api/pets/$petId/image", caregiverToken).andExpect(status().isOk)

        authDelete("/api/pets/$petId/image", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.photoUrl").doesNotExist())
    }

    @Test
    fun `rejects invalid pet payloads`() {
        val (_, token) = registerAndLogin("petbad")
        authPost("/api/pets", token, """{"name":"","species":""}""").andExpect(status().isBadRequest)
    }
}
