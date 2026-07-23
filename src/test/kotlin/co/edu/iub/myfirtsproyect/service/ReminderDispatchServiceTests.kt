package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.model.CareTask
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.NotificationDelivery
import co.edu.iub.myfirtsproyect.model.NotificationDeliveryStatus
import co.edu.iub.myfirtsproyect.model.Pet
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.NotificationDeliveryRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderDispatchServiceTests {
    private val zone = ZoneId.of("America/Bogota")
    private val clock = Clock.fixed(Instant.parse("2026-07-19T15:00:00Z"), zone)
    private val now = LocalDateTime.now(clock)

    private lateinit var messageService: NotificationMessageService
    private lateinit var taskRepository: CareTaskRepository
    private lateinit var channelRepository: NotificationChannelRepository
    private lateinit var deliveryRepository: NotificationDeliveryRepository
    private lateinit var service: ReminderDispatchService

    @BeforeEach
    fun setUp() {
        messageService = mock(NotificationMessageService::class.java)
        taskRepository = mock(CareTaskRepository::class.java)
        channelRepository = mock(NotificationChannelRepository::class.java)
        deliveryRepository = mock(NotificationDeliveryRepository::class.java)
        service = ReminderDispatchService(
            messageService,
            taskRepository,
            channelRepository,
            deliveryRepository,
            clock,
            120,
        )
    }

    private fun prepare(task: CareTask, vararg channels: NotificationChannel) {
        `when`(
            taskRepository.findAllByStatusAndActiveTrueAndScheduledAtBetweenOrderByScheduledAtAsc(
                CareTaskStatus.PENDING, now.minusHours(24), now.plusHours(24).plusMinutes(1),
            ),
        ).thenReturn(listOf(task))
        `when`(channelRepository.findAllByOwnerIdAndVerifiedTrueAndActiveTrue(4)).thenReturn(channels.toList())
    }

    @Test
    fun `sends an anticipated reminder saying how much time is left`() {
        val task = taskAt(now.plusMinutes(10))
        val telegram = channel(10, NotificationChannelType.TELEGRAM)
        prepare(task, telegram)

        service.sendDueReminders()

        val expected = service.buildMessage(task, now)
        assertTrue(expected.contains("Aviso: faltan 10 minutos"), expected)
        verify(messageService).sendCareReminder(telegram, "Recordatorio: Thor · Dar comida", expected, 20)
        val deliveries = ArgumentCaptor.forClass(NotificationDelivery::class.java)
        verify(deliveryRepository).save(deliveries.capture())
        assertEquals(NotificationDeliveryStatus.SENT, deliveries.value.status)
        assertEquals(now, deliveries.value.scheduledFor)
    }

    @Test
    fun `an on-time reminder says the care is due now`() {
        val task = taskAt(now)
        val telegram = channel(10, NotificationChannelType.TELEGRAM)
        prepare(task, telegram)

        service.sendDueReminders()

        val expected = service.buildMessage(task, now)
        assertTrue(expected.contains("el cuidado corresponde ahora"), expected)
        verify(messageService).sendCareReminder(telegram, "Recordatorio: Thor · Dar comida", expected, 20)
    }

    @Test
    fun `a late reminder inside the recovery window says how long ago it was scheduled`() {
        val task = taskAt(now.minusMinutes(90))
        val telegram = channel(10, NotificationChannelType.TELEGRAM)
        prepare(task, telegram)

        service.sendDueReminders()

        val expected = service.buildMessage(task, now)
        assertTrue(expected.contains("estaba programado hace 1 hora y 30 minutos"), expected)
        assertTrue(!expected.contains("faltan"), "No debe informar tiempo restante falso")
        verify(messageService).sendCareReminder(telegram, "Recordatorio: Thor · Dar comida", expected, 20)
    }

    @Test
    fun `reminders older than the recovery window are discarded and never sent`() {
        val task = taskAt(now.minusHours(10))
        val telegram = channel(10, NotificationChannelType.TELEGRAM)
        prepare(task, telegram)

        service.sendDueReminders()

        verifyNoInteractions(messageService)
        val deliveries = ArgumentCaptor.forClass(NotificationDelivery::class.java)
        verify(deliveryRepository).save(deliveries.capture())
        assertEquals(NotificationDeliveryStatus.DISCARDED, deliveries.value.status)
    }

    @Test
    fun `does not send a reminder before its notification time`() {
        val task = taskAt(now.plusMinutes(11))
        prepare(task, channel(10, NotificationChannelType.TELEGRAM))

        service.sendDueReminders()

        verifyNoInteractions(messageService)
        verify(deliveryRepository, never()).save(any(NotificationDelivery::class.java))
    }

    @Test
    fun `does not repeat reminders already sent or discarded`() {
        val task = taskAt(now.plusMinutes(10))
        prepare(task, channel(10, NotificationChannelType.TELEGRAM))
        `when`(
            deliveryRepository.existsByCareTaskIdAndChannelIdAndScheduledForAndStatusIn(
                20,
                10,
                now,
                listOf(
                    NotificationDeliveryStatus.SENT,
                    NotificationDeliveryStatus.DISCARDED,
                    NotificationDeliveryStatus.CANCELLED,
                ),
            ),
        ).thenReturn(true)

        service.sendDueReminders()

        verifyNoInteractions(messageService)
        verify(deliveryRepository, never()).save(any(NotificationDelivery::class.java))
    }

    @Test
    fun `stops retrying after three failed attempts`() {
        val task = taskAt(now.plusMinutes(10))
        prepare(task, channel(10, NotificationChannelType.TELEGRAM))
        `when`(
            deliveryRepository.countByCareTaskIdAndChannelIdAndScheduledForAndStatus(
                20, 10, now, NotificationDeliveryStatus.FAILED,
            ),
        ).thenReturn(3)

        service.sendDueReminders()

        verifyNoInteractions(messageService)
    }

    @Test
    fun `registers a failed attempt when the channel throws`() {
        val task = taskAt(now.plusMinutes(10))
        val telegram = channel(10, NotificationChannelType.TELEGRAM)
        prepare(task, telegram)
        doThrow(RuntimeException("Telegram caído")).`when`(messageService)
            .sendCareReminder(telegram, "Recordatorio: Thor · Dar comida", service.buildMessage(task, now), 20)

        service.sendDueReminders()

        val deliveries = ArgumentCaptor.forClass(NotificationDelivery::class.java)
        verify(deliveryRepository).save(deliveries.capture())
        assertEquals(NotificationDeliveryStatus.FAILED, deliveries.value.status)
        assertEquals("Telegram caído", deliveries.value.errorMessage)
    }

    @Test
    fun `timing line uses hours and minutes in a human format`() {
        assertEquals("Aviso: faltan 2 horas", service.timingLine(now.plusHours(2), now))
        assertEquals("Aviso: falta 1 hora y 5 minutos", service.timingLine(now.plusMinutes(65), now))
        assertEquals("Aviso: el cuidado corresponde ahora", service.timingLine(now.minusMinutes(1), now))
        assertEquals("Aviso: estaba programado hace 3 horas", service.timingLine(now.minusHours(3), now))
    }

    private fun taskAt(scheduledAt: LocalDateTime): CareTask {
        val owner = User(id = 4, email = "mateo@example.com", fullName = "Mateo", passwordHash = "hash")
        val pet = Pet(id = 8, name = "Thor", species = "Gato", owner = owner)
        return CareTask(
            id = 20,
            title = "Dar comida",
            description = "Porción de la mañana",
            careType = "ALIMENTACION",
            scheduledAt = scheduledAt,
            pet = pet,
        )
    }

    private fun channel(id: Long, type: NotificationChannelType) = NotificationChannel(
        id = id,
        type = type,
        destination = "6251520493",
        verified = true,
        reminderMinutesBefore = 10,
        owner = User(id = 4, email = "mateo@example.com", fullName = "Mateo", passwordHash = "hash"),
    )
}
