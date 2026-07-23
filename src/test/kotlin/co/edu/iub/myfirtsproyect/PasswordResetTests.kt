package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.repository.PasswordResetTokenRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import co.edu.iub.myfirtsproyect.support.anyUser
import co.edu.iub.myfirtsproyect.support.captureString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import kotlin.test.assertTrue

class PasswordResetTests : IntegrationTestBase() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var tokenRepository: PasswordResetTokenRepository

    private fun requestReset(email: String): String {
        mockMvc.perform(
            post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email"}"""),
        ).andExpect(status().isOk)
        val linkCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(passwordResetSender, atLeastOnce()).send(anyUser(), captureString(linkCaptor))
        return linkCaptor.allValues.last().substringAfter("token=")
    }

    private fun resetWith(token: String, newPassword: String) = mockMvc.perform(
        post("/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"token":"$token","newPassword":"$newPassword"}"""),
    )

    @Test
    fun `completes the full recovery flow and invalidates the token after use`() {
        val email = uniqueEmail("reset")
        registerUser(email)
        val token = requestReset(email)

        resetWith(token, "nueva-clave-123").andExpect(status().isOk)
        login(email, "nueva-clave-123")
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"password123"}"""),
        ).andExpect(status().isUnauthorized)

        resetWith(token, "otra-clave-123").andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects invalid and expired tokens`() {
        resetWith("token-que-no-existe", "clave-nueva-12").andExpect(status().isBadRequest)

        val email = uniqueEmail("resetexp")
        registerUser(email)
        val token = requestReset(email)
        val userId = userRepository.findByEmail(email)!!.id!!
        tokenRepository.findAllByUserIdAndUsedAtIsNull(userId).forEach { stored ->
            stored.expiresAt = LocalDateTime.now().minusMinutes(1)
            tokenRepository.save(stored)
        }
        resetWith(token, "clave-nueva-12").andExpect(status().isBadRequest)
        login(email, "password123")
    }

    @Test
    fun `does not reveal whether an email is registered`() {
        val result = mockMvc.perform(
            post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nadie.${System.nanoTime()}@test.local"}"""),
        ).andExpect(status().isOk).andReturn()
        assertTrue(result.response.contentAsString.contains("Si el correo está registrado"))
    }

    @Test
    fun `rejects passwords shorter than 8 characters`() {
        resetWith("cualquiera", "corta").andExpect(status().isBadRequest)
    }
}
