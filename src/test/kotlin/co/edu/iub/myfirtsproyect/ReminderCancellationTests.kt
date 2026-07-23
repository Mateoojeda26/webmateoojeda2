package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.NotificationDeliveryStatus
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.NotificationDeliveryRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderCancellationTests : IntegrationTestBase() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var channelRepository: NotificationChannelRepository

    @Autowired
    lateinit var deliveryRepository: NotificationDeliveryRepository

    private fun verifiedChannel(email: String): NotificationChannel {
        val owner = userRepository.findByEmail(email)!!
        return channelRepository.save(
            NotificationChannel(
                type = NotificationChannelType.EMAIL,
                destination = "avisos@test.local",
                verified = true,
                active = true,
                owner = owner,
            ),
        )
    }

    @Test
    fun `deleting a task records its pending reminder as cancelled`() {
        val (email, token) = registerAndLogin("cancel-task")
        val petId = createPet(token)
        val taskId = createCareTask(token, petId)
        val channel = verifiedChannel(email)

        authDelete("/api/care-tasks/$taskId", token).andExpect(status().isNoContent)

        val cancelled = deliveryRepository.findAll().filter {
            it.careTask.id == taskId && it.channel.id == channel.id &&
                it.status == NotificationDeliveryStatus.CANCELLED
        }
        assertEquals(1, cancelled.size)
        assertTrue(cancelled.single().errorMessage!!.contains("eliminado"))
    }

    @Test
    fun `disabling a channel cancels reminders for active pending tasks`() {
        val (email, token) = registerAndLogin("cancel-channel")
        val petId = createPet(token)
        val taskId = createCareTask(token, petId)
        val channel = verifiedChannel(email)

        authDelete("/api/notifications/channels/${channel.id}", token).andExpect(status().isNoContent)

        assertTrue(
            deliveryRepository.findAll().any {
                it.careTask.id == taskId && it.channel.id == channel.id &&
                    it.status == NotificationDeliveryStatus.CANCELLED
            },
        )
    }

    @Test
    fun `cancelling a recurring series records every pending occurrence`() {
        val (email, token) = registerAndLogin("cancel-series")
        val petId = createPet(token)
        val channel = verifiedChannel(email)
        val today = java.time.LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("America/Bogota")))
        val series = readJson(
            authPost(
                "/api/series",
                token,
                """{"petId":$petId,"title":"Comidas","careType":"Alimentación","frequency":"DAILY","timesOfDay":["08:00"],"startDate":"$today"}""",
            ).andExpect(status().isCreated),
        )
        val seriesId = series.path("id").asLong()

        authDelete("/api/series/$seriesId", token).andExpect(status().isNoContent)

        val cancelled = deliveryRepository.findAll().filter {
            it.channel.id == channel.id && it.status == NotificationDeliveryStatus.CANCELLED
        }
        assertEquals(4, cancelled.size)
    }
}
