package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.repository.UserRepository
import co.edu.iub.myfirtsproyect.service.JwtService
import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthAndUserTests : IntegrationTestBase() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `registers a user and stores the password hashed`() {
        val email = uniqueEmail("rf01")
        registerUser(email, password = "clave-segura-1")
        val user = userRepository.findByEmail(email)!!
        assertNotEquals("clave-segura-1", user.passwordHash)
        assertTrue(user.passwordHash.startsWith("$2"), "Debe usar BCrypt")
    }

    @Test
    fun `rejects duplicated emails on register`() {
        val email = uniqueEmail("rf01dup")
        registerUser(email)
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","fullName":"Otro","password":"password123"}"""),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `logs in with valid credentials and rejects wrong password`() {
        val email = uniqueEmail("rf02")
        registerUser(email)
        login(email)
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"incorrecta1"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `rejects requests without token, with invalid token and with expired token`() {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer no-es-un-jwt"))
            .andExpect(status().isUnauthorized)

        val email = uniqueEmail("rf02exp")
        registerUser(email)
        val expiredJwtService = JwtService("this-is-a-test-secret-must-be-at-least-32-bytes", -5)
        val expiredToken = expiredJwtService.generateToken(userRepository.findByEmail(email)!!)
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer $expiredToken"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `reads and updates the profile`() {
        val (email, token) = registerAndLogin("rf01prof")
        authGet("/users/me", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(email))
        authPut("/users/me", token, """{"fullName":"Nombre Nuevo","phoneNumber":"3001234567"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Nombre Nuevo"))
            .andExpect(jsonPath("$.phoneNumber").value("3001234567"))
    }

    @Test
    fun `authenticated user changes password using the current password`() {
        val email = uniqueEmail("change-password")
        registerUser(email, password = "clave-anterior")
        val token = login(email, "clave-anterior")

        authPost(
            "/users/me/change-password",
            token,
            """{"currentPassword":"incorrecta","newPassword":"clave-nueva-2026"}""",
        ).andExpect(status().isUnauthorized)

        authPost(
            "/users/me/change-password",
            token,
            """{"currentPassword":"clave-anterior","newPassword":"clave-nueva-2026"}""",
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"clave-anterior"}"""),
        ).andExpect(status().isUnauthorized)
        login(email, "clave-nueva-2026")
    }

    @Test
    fun `rejects a profile update with an email that belongs to someone else`() {
        val (otherEmail, _) = registerAndLogin("rf01other")
        val (_, token) = registerAndLogin("rf01mine")
        authPut("/users/me", token, """{"email":"$otherEmail"}""").andExpect(status().isConflict)
    }

    @Test
    fun `deactivated accounts cannot log in and their tokens stop working`() {
        val (email, token) = registerAndLogin("rf01del")
        authDelete("/users/me", token).andExpect(status().isNoContent)
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"password123"}"""),
        ).andExpect(status().isUnauthorized)
        authGet("/users/me", token).andExpect(status().isUnauthorized)
    }
}
