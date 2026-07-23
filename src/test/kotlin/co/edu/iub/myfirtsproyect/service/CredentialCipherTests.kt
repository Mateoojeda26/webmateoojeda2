package co.edu.iub.myfirtsproyect.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CredentialCipherTests {
    @Test
    fun `encrypts and decrypts credentials`() {
        val cipher = CredentialCipher("a-local-test-secret")
        val encrypted = cipher.encrypt("refresh-token")

        assertNotEquals("refresh-token", encrypted)
        assertEquals("refresh-token", cipher.decrypt(encrypted))
    }
}
