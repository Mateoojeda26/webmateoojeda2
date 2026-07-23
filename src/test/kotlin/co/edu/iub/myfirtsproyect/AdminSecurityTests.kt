package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.AdminAuditLogRepository
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.dao.DataAccessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AdminSecurityTests : IntegrationTestBase() {
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var careTaskRepository: CareTaskRepository
    @Autowired lateinit var caregiverAccessRepository: CaregiverAccessRepository
    @Autowired lateinit var auditRepository: AdminAuditLogRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private val adminEmail = "admin.pruebas@taskora.local"
    private val adminPassword = "admin-test-123"

    private fun adminToken(): String {
        if (userRepository.findByRole(UserRole.ADMIN) == null) {
            userRepository.saveAndFlush(
                User(
                    email = adminEmail,
                    fullName = "Administrador de pruebas",
                    passwordHash = passwordEncoder.encode(adminPassword)!!,
                    role = UserRole.ADMIN,
                ),
            )
        }
        return login(adminEmail, adminPassword)
    }

    @Test
    fun `public registration always creates USER and only one ADMIN exists`() {
        adminToken()
        val email = uniqueEmail("public-role")
        val response = registerUser(email)
        assertEquals("USER", response.path("role").asText())
        assertEquals(UserRole.USER, userRepository.findByEmail(email)!!.role)
        assertEquals(1, userRepository.countByRole(UserRole.ADMIN))
        assertFailsWith<DataAccessException> {
            jdbcTemplate.update(
                """
                INSERT INTO users (email, full_name, password_hash, active, account_status, role, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                uniqueEmail("second-admin"),
                "Segundo administrador",
                passwordEncoder.encode("second-admin-123"),
                true,
                "ACTIVE",
                "ADMIN",
                java.time.LocalDateTime.now().toString(),
            )
        }
        assertEquals(1, userRepository.countByRole(UserRole.ADMIN))
    }

    @Test
    fun `normal users receive forbidden on every admin route`() {
        val (_, userToken) = registerAndLogin("admin-forbidden")
        authGet("/api/admin/dashboard", userToken).andExpect(status().isForbidden)
        authGet("/api/admin/users", userToken).andExpect(status().isForbidden)
        authGet("/api/admin/audit", userToken).andExpect(status().isForbidden)
    }

    @Test
    fun `admin lists every user and pets from any owner`() {
        val token = adminToken()
        val (ownerEmail, ownerToken) = registerAndLogin("admin-owner")
        val petId = createPet(ownerToken, "Mascota visible")

        authGet("/api/admin/users?search=$ownerEmail", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].email").value(ownerEmail))
        authGet("/api/admin/pets/$petId", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Mascota visible"))
    }

    @Test
    fun `suspension blocks login and old JWT and reactivation restores access`() {
        val token = adminToken()
        val (email, userToken) = registerAndLogin("suspension")
        val userId = userRepository.findByEmail(email)!!.id!!

        authPost("/api/admin/users/$userId/suspend", token, """{"reason":"Prueba de seguridad"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUSPENDED"))
        authGet("/users/me", userToken).andExpect(status().isUnauthorized)
        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"password123"}"""),
        ).andExpect(status().isUnauthorized)

        authPost("/api/admin/users/$userId/reactivate", token).andExpect(status().isOk)
        login(email)
        assertTrue(userRepository.findByEmail(email)!!.canAccess())
    }

    @Test
    fun `admin cannot be suspended deleted or self deactivated`() {
        val token = adminToken()
        val admin = userRepository.findByRole(UserRole.ADMIN)!!
        authPost("/api/admin/users/${admin.id}/suspend", token, """{"reason":"No permitido"}""")
            .andExpect(status().isForbidden)
        mockMvc.perform(
            delete("/api/admin/users/${admin.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationEmail\":\"${admin.email}\"}"),
        ).andExpect(status().isForbidden)
        authDelete("/users/me", token).andExpect(status().isForbidden)
        assertEquals(1, userRepository.countByRole(UserRole.ADMIN))
    }

    @Test
    fun `permanent deletion requires exact email removes relations and is audited`() {
        val token = adminToken()
        val (ownerEmail, ownerToken) = registerAndLogin("delete-owner")
        val (caregiverEmail, _) = registerAndLogin("delete-caregiver")
        val ownerId = userRepository.findByEmail(ownerEmail)!!.id!!
        val petId = createPet(ownerToken, "Dato para eliminar")
        val taskId = createCareTask(ownerToken, petId)
        val accessId = readJson(
            authPost(
                "/api/caregivers",
                ownerToken,
                """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"EDITOR"}""",
            ).andExpect(status().isCreated),
        ).path("id").asLong()

        mockMvc.perform(
            delete("/api/admin/users/$ownerId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationEmail\":\"correo.incorrecto@test.local\"}"),
        ).andExpect(status().isBadRequest)
        assertNotNull(userRepository.findByEmail(ownerEmail))

        mockMvc.perform(
            delete("/api/admin/users/$ownerId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationEmail\":\"$ownerEmail\"}"),
        ).andExpect(status().isNoContent)

        assertEquals(null, userRepository.findByEmail(ownerEmail))
        assertFalse(petRepository.findById(petId).isPresent)
        assertFalse(careTaskRepository.findById(taskId).isPresent)
        assertFalse(caregiverAccessRepository.findById(accessId).isPresent)
        assertTrue(auditRepository.findAllByOrderByCreatedAtDesc().any {
            it.action == "DELETE_USER" && it.resourceIdentifier == ownerEmail
        })
    }

    @Test
    fun `administrative changes are recorded in audit`() {
        val token = adminToken()
        val (email, _) = registerAndLogin("audit-user")
        val id = userRepository.findByEmail(email)!!.id!!
        authPut("/api/admin/users/$id", token, """{"fullName":"Nombre auditado"}""")
            .andExpect(status().isOk)
        assertTrue(auditRepository.findAllByOrderByCreatedAtDesc().any {
            it.action == "EDIT_USER" && it.resourceIdentifier == email
        })
    }
}
