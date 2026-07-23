package co.edu.iub.myfirtsproyect.config

import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(1)
class AdminInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${TASKORA_ADMIN_NAME:}") private val adminName: String,
    @param:Value("\${TASKORA_ADMIN_EMAIL:}") private val adminEmail: String,
    @param:Value("\${TASKORA_ADMIN_PASSWORD:}") private val adminPassword: String,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(AdminInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (userRepository.existsByRole(UserRole.ADMIN)) return

        if (adminName.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
            logger.info("Administrador no creado: configura TASKORA_ADMIN_NAME, TASKORA_ADMIN_EMAIL y TASKORA_ADMIN_PASSWORD")
            return
        }

        val email = adminEmail.trim().lowercase()
        if (userRepository.existsByEmail(email)) {
            logger.warn("No se creó el administrador porque el correo configurado ya pertenece a otra cuenta")
            return
        }

        if (adminPassword.length < 8) {
            logger.warn("No se creó el administrador porque TASKORA_ADMIN_PASSWORD debe tener al menos 8 caracteres")
            return
        }

        userRepository.save(
            User(
                email = email,
                fullName = adminName.trim(),
                passwordHash = passwordEncoder.encode(adminPassword)!!,
                role = UserRole.ADMIN,
            ),
        )
        logger.info("Cuenta administradora inicial creada para {}", email)
    }
}
