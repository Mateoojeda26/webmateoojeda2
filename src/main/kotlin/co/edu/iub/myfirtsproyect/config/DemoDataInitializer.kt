package co.edu.iub.myfirtsproyect.config

import co.edu.iub.myfirtsproyect.model.CaregiverAccess
import co.edu.iub.myfirtsproyect.model.CaregiverPermission
import co.edu.iub.myfirtsproyect.model.Pet
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.demo", name = ["enabled"], havingValue = "true")
class DemoDataInitializer(
    private val userRepository: UserRepository,
    private val petRepository: PetRepository,
    private val accessRepository: CaregiverAccessRepository,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${TASKORA_DEMO_PASSWORD:}") private val demoPassword: String,
    @param:Value("\${TASKORA_DEMO_MATEO_EMAIL:}") private val mateoEmail: String,
    @param:Value("\${TASKORA_DEMO_MATEO_NAME:Mateo}") private val mateoName: String,
    @param:Value("\${TASKORA_DEMO_OWNER_EMAIL:propietario.demo@taskora.local}") private val ownerEmail: String,
    @param:Value("\${TASKORA_DEMO_CAREGIVER_EMAIL:cuidador.demo@taskora.local}") private val caregiverEmail: String,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(DemoDataInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (demoPassword.length < 8) {
            logger.warn("Datos demo omitidos: TASKORA_DEMO_PASSWORD debe tener al menos 8 caracteres")
            return
        }

        if (mateoEmail.isNotBlank()) createUserIfMissing(mateoEmail, mateoName)
        val owner = createUserIfMissing(ownerEmail, "Propietario de ejemplo")
        val caregiver = createUserIfMissing(caregiverEmail, "Cuidador de ejemplo")

        val pets = listOf(
            "Conejo demo" to "Conejo",
            "Ave demo" to "Ave",
            "Tortuga demo" to "Tortuga",
        ).map { (name, species) -> createPetIfMissing(owner, name, species) }

        pets.forEach { pet ->
            if (!accessRepository.existsByPetIdAndCaregiverIdAndActiveTrue(requireNotNull(pet.id), requireNotNull(caregiver.id))) {
                accessRepository.save(
                    CaregiverAccess(pet = pet, caregiver = caregiver, permission = CaregiverPermission.EDITOR),
                )
            }
        }
        logger.info("Datos de demostración preparados sin crear mascotas reales para Mateo")
    }

    private fun createUserIfMissing(emailValue: String, name: String): User {
        val email = emailValue.trim().lowercase()
        return userRepository.findByEmail(email) ?: userRepository.save(
            User(
                email = email,
                fullName = name,
                passwordHash = passwordEncoder.encode(demoPassword)!!,
            ),
        )
    }

    private fun createPetIfMissing(owner: User, name: String, species: String): Pet {
        val existing = petRepository.findAllByOwnerIdOrderByNameAsc(requireNotNull(owner.id))
            .firstOrNull { it.name == name }
        return existing ?: petRepository.save(Pet(name = name, species = species, owner = owner))
    }
}
