package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ApiDocumentationTests : IntegrationTestBase() {
    @Test
    fun `OpenAPI documentation is public and describes Taskora Pet`() {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.info.title").value("Taskora Pet API"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
    }
}
