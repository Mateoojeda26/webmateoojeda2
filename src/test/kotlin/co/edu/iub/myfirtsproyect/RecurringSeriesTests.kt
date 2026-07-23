package co.edu.iub.myfirtsproyect

import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.service.RecurringSeriesService
import co.edu.iub.myfirtsproyect.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurringSeriesTests : IntegrationTestBase() {
    @Autowired
    lateinit var careTaskRepository: CareTaskRepository

    @Autowired
    lateinit var seriesService: RecurringSeriesService

    @Autowired
    lateinit var clock: Clock

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun createSeries(
        token: String,
        petId: Long,
        times: String = """"08:00","14:00","20:00"""",
        frequency: String = "DAILY",
        extra: String = "",
    ): Long {
        val result = authPost(
            "/api/series",
            token,
            """{"petId":$petId,"title":"Alimentar","careType":"Alimentación","frequency":"$frequency","timesOfDay":[$times],"startDate":"${today()}"$extra}""",
        ).andExpect(status().isCreated)
        return readJson(result).path("id").asLong()
    }

    @Test
    fun `creates a daily series with several times per day and generates occurrences without duplicates`() {
        val (_, token) = registerAndLogin("srs")
        val petId = createPet(token)
        val seriesId = createSeries(token, petId)

        val occurrences = careTaskRepository.findAllBySeriesId(seriesId)
        assertEquals(12, occurrences.size, "3 horarios x (hoy + 3 días de horizonte)")
        assertEquals(occurrences.size, occurrences.map { it.scheduledAt }.distinct().size)

        seriesService.generateForAllActive()
        assertEquals(12, careTaskRepository.findAllBySeriesId(seriesId).size, "La generación debe ser idempotente")
    }

    @Test
    fun `pause removes future pending occurrences and resume brings them back`() {
        val (_, token) = registerAndLogin("srspause")
        val petId = createPet(token)
        val seriesId = createSeries(token, petId, times = """"08:00"""")

        authPost("/api/series/$seriesId/pause", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAUSED"))
        val paused = readJson(authGet("/api/care-tasks", token).andExpect(status().isOk))
        val activeSeriesIds = paused.values().asSequence()
            .filter { it.path("seriesId").asLong() == seriesId }
            .map { java.time.LocalDateTime.parse(it.path("scheduledAt").asText()) }
            .toList()
        assertTrue(
            activeSeriesIds.none { it.isAfter(java.time.LocalDateTime.now()) },
            "Pausar debe quitar las ocurrencias futuras y puede conservar una vencida para el historial",
        )

        authPost("/api/series/$seriesId/resume", token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
        val resumed = readJson(authGet("/api/care-tasks", token).andExpect(status().isOk))
        assertEquals(4, resumed.count { it.path("seriesId").asLong() == seriesId })

        assertEquals(4, careTaskRepository.findAllBySeriesId(seriesId).size, "Reanudar no debe duplicar ocurrencias")
    }

    @Test
    fun `editing a series applies changes from the requested date and keeps history`() {
        val (_, token) = registerAndLogin("srsedit")
        val petId = createPet(token)
        val seriesId = createSeries(token, petId, times = """"08:00"""")

        val first = careTaskRepository.findAllBySeriesId(seriesId).minByOrNull { it.scheduledAt }!!
        authPut("/api/care-tasks/${first.id}", token, """{"status":"COMPLETED"}""").andExpect(status().isOk)

        authPut(
            "/api/series/$seriesId",
            token,
            """{"title":"Alimentación especial","description":"Dieta nueva","careType":"Tratamiento","priority":"HIGH","timesOfDay":["09:30","18:30"],"applyFrom":"${today().plusDays(1)}"}""",
        ).andExpect(status().isOk)

        val occurrences = careTaskRepository.findAllBySeriesId(seriesId)
        assertTrue(
            occurrences.any { it.status == CareTaskStatus.COMPLETED },
            "El historial completado se conserva",
        )
        val futureActive = occurrences.filter {
            it.active && it.status == CareTaskStatus.PENDING && !it.scheduledAt.toLocalDate().isBefore(today().plusDays(1))
        }
        assertTrue(futureActive.isNotEmpty())
        assertTrue(
            futureActive.all { it.scheduledAt.toLocalTime().toString() in listOf("09:30", "18:30") },
            "Las ocurrencias futuras deben usar los nuevos horarios",
        )
        assertTrue(futureActive.all { it.title == "Alimentación especial" })
        assertTrue(futureActive.all { it.description == "Dieta nueva" })
        assertTrue(futureActive.all { it.careType == "Tratamiento" })
        assertTrue(futureActive.all { it.priority.name == "HIGH" })
    }

    @Test
    fun `cancelling a series removes pending occurrences but keeps completed history`() {
        val (_, token) = registerAndLogin("srsdel")
        val petId = createPet(token)
        val seriesId = createSeries(token, petId, times = """"08:00"""")
        val first = careTaskRepository.findAllBySeriesId(seriesId).minByOrNull { it.scheduledAt }!!
        authPut("/api/care-tasks/${first.id}", token, """{"status":"COMPLETED"}""").andExpect(status().isOk)

        authDelete("/api/series/$seriesId", token).andExpect(status().isNoContent)

        val agenda = readJson(authGet("/api/care-tasks", token).andExpect(status().isOk))
        assertEquals(0, agenda.count { it.path("seriesId").asLong() == seriesId && it.path("status").asText() == "PENDING" })
        val history = readJson(authGet("/api/care-tasks?status=COMPLETED", token).andExpect(status().isOk))
        assertEquals(1, history.count { it.path("seriesId").asLong() == seriesId })

        seriesService.generateForAllActive()
        val regenerated = careTaskRepository.findAllBySeriesId(seriesId)
            .count { it.active && it.status == CareTaskStatus.PENDING }
        assertEquals(0, regenerated, "Una serie cancelada no genera ocurrencias")
    }

    @Test
    fun `weekly and interval frequencies only generate matching dates`() {
        val (_, token) = registerAndLogin("srsfreq")
        val petId = createPet(token)
        val weekday = today().dayOfWeek.name
        val weeklyId = createSeries(
            token,
            petId,
            times = """"07:00"""",
            frequency = "WEEKLY",
            extra = ""","daysOfWeek":["$weekday"]""",
        )
        val weekly = careTaskRepository.findAllBySeriesId(weeklyId)
        assertEquals(1, weekly.size, "Con horizonte de 3 días solo cae el día de hoy")
        assertEquals(today(), weekly[0].scheduledAt.toLocalDate())

        val intervalId = createSeries(
            token,
            petId,
            times = """"07:00"""",
            frequency = "INTERVAL",
            extra = ""","intervalDays":2""",
        )
        val interval = careTaskRepository.findAllBySeriesId(intervalId).map { it.scheduledAt.toLocalDate() }.sorted()
        assertEquals(listOf(today(), today().plusDays(2)), interval)
    }

    @Test
    fun `respects the series end date`() {
        val (_, token) = registerAndLogin("srsend")
        val petId = createPet(token)
        val seriesId = createSeries(
            token,
            petId,
            times = """"08:00"""",
            extra = ""","endDate":"${today().plusDays(1)}"""",
        )
        assertEquals(2, careTaskRepository.findAllBySeriesId(seriesId).size, "Hoy y mañana solamente")

        authPut(
            "/api/series/$seriesId",
            token,
            """{"clearEndDate":true,"applyFrom":"${today()}"}""",
        ).andExpect(status().isOk).andExpect(jsonPath("$.endDate").doesNotExist())
        assertEquals(4, careTaskRepository.findAllBySeriesId(seriesId).count { it.active })
    }

    @Test
    fun `validates series input`() {
        val (_, token) = registerAndLogin("srsval")
        val petId = createPet(token)
        authPost(
            "/api/series",
            token,
            """{"petId":$petId,"title":"X","careType":"Paseo","frequency":"NONE","timesOfDay":["08:00"],"startDate":"${today()}"}""",
        ).andExpect(status().isBadRequest)
        authPost(
            "/api/series",
            token,
            """{"petId":$petId,"title":"X","careType":"Paseo","frequency":"DAILY","timesOfDay":["08:00","08:00"],"startDate":"${today()}"}""",
        ).andExpect(status().isBadRequest)
        authPost(
            "/api/series",
            token,
            """{"petId":$petId,"title":"X","careType":"Paseo","frequency":"INTERVAL","timesOfDay":["08:00"],"startDate":"${today()}"}""",
        ).andExpect(status().isBadRequest)
        authPost(
            "/api/series",
            token,
            """{"petId":$petId,"title":"X","careType":"Paseo","frequency":"DAILY","timesOfDay":["08:00"],"startDate":"${today()}","endDate":"${today().minusDays(1)}"}""",
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `only the owner manages a series while caregivers can read shared ones`() {
        val (_, ownerToken) = registerAndLogin("srsown")
        val (caregiverEmail, caregiverToken) = registerAndLogin("srscg")
        val (_, strangerToken) = registerAndLogin("srsstr")
        val petId = createPet(ownerToken)
        val seriesId = createSeries(ownerToken, petId, times = """"08:00"""")
        authPost("/api/caregivers", ownerToken, """{"petId":$petId,"caregiverEmail":"$caregiverEmail","permission":"EDITOR"}""")
            .andExpect(status().isCreated)

        authGet("/api/series/$seriesId", caregiverToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.canManage").value(false))
        authPost("/api/series/$seriesId/pause", caregiverToken).andExpect(status().isForbidden)
        authPut("/api/series/$seriesId", caregiverToken, """{"title":"Hackeada"}""").andExpect(status().isForbidden)
        authDelete("/api/series/$seriesId", caregiverToken).andExpect(status().isForbidden)

        authGet("/api/series/$seriesId", strangerToken).andExpect(status().isNotFound)
    }
}
