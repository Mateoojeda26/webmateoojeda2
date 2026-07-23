package co.edu.iub.myfirtsproyect.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun taskoraOpenApi(): OpenAPI {
        val bearerName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Taskora Pet API")
                    .description("API para gestionar mascotas, cuidadores, rutinas y recordatorios.")
                    .version("1.0"),
            )
            .components(
                Components().addSecuritySchemes(
                    bearerName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
            .addSecurityItem(SecurityRequirement().addList(bearerName))
    }
}
