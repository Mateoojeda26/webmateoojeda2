package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.care.CareTaskUpdateRequest
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
import co.edu.iub.myfirtsproyect.support.anyChannel
import co.edu.iub.myfirtsproyect.support.captureString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramInteractionServiceTests {
    private val zone = ZoneId.of("America/Bogota")
    private val clock = Clock.fixed(Instant.parse("2026-07-19T15:00:00Z"), zone)
    private val now = LocalDateTime.now(clock)

    private lateinit var botClient: TelegramBotClient
    private lateinit var linkService: TelegramLinkService
    private lateinit var channelRepository: NotificationChannelRepository
    private lateinit var taskRepository: CareTaskRepository
    private lateinit var careTaskService: CareTaskService
    private lateinit var deliveryRepository: NotificationDeliveryRepository
    private lateinit var messageService: NotificationMessageService
    private lateinit var service: TelegramInteractionService

    @BeforeEach
    fun setUp() {
        botClient = mock(TelegramBotClient::class.java)
        linkService = mock(TelegramLinkService::class.java)
        channelRepository = mock(NotificationChannelRepository::class.java)
        taskRepository = mock(CareTaskRepository::class.java)
        careTaskService = mock(CareTaskService::class.java)
        deliveryRepository = mock(NotificationDeliveryRepository::class.java)
        messageService = mock(NotificationMessageService::class.java)
        service = TelegramInteractionService(
            botClient,
            linkService,
            channelRepository,
            taskRepository,
            careTaskService,
            deliveryRepository,
            messageService,
            clock,
        )
    }

    @Test
    fun `help command explains the available commands`() {
        service.handle(message("/help"))

        val chat = ArgumentCaptor.forClass(String::class.java)
        val text = ArgumentCaptor.forClass(String::class.java)
        verify(botClient).sendMessage(captureString(chat), captureString(text))
        assertEquals("123", chat.value)
        assertTrue(text.value.contains("/estado"))
        assertTrue(text.value.contains("/proximos"))
    }

    @Test
    fun `status command confirms a linked account`() {
        `when`(
            channelRepository.findByTypeAndDestination(NotificationChannelType.TELEGRAM, "123"),
        ).thenReturn(channel())

        service.handle(message("/estado"))

        verify(botClient).sendMessage("123", "✅ Tu cuenta está vinculada y lista para recibir recordatorios por Telegram.")
    }

    @Test
    fun `upcoming command lists at most the next care tasks`() {
        val channel = channel()
        `when`(
            channelRepository.findByTypeAndDestination(NotificationChannelType.TELEGRAM, "123"),
        ).thenReturn(channel)
        `when`(
            taskRepository.findAllByPetOwnerIdAndStatusAndActiveTrue(4, CareTaskStatus.PENDING),
        ).thenReturn(listOf(task(now.plusHours(2))))

        service.handle(message("/proximos"))

        val chat = ArgumentCaptor.forClass(String::class.java)
        val text = ArgumentCaptor.forClass(String::class.java)
        verify(botClient).sendMessage(captureString(chat), captureString(text))
        assertEquals("123", chat.value)
        assertTrue(text.value.contains("Thor: Dar comida"))
        assertTrue(text.value.contains("19/07/2026 a las 12:00"))
    }

    @Test
    fun `done button completes only a task owned by the linked account`() {
        val channel = channel()
        val task = task(now.plusHours(2))
        prepareButton(channel, task)
        `when`(
            deliveryRepository.findAllByCareTaskIdAndChannelIdAndStatus(
                20,
                10,
                NotificationDeliveryStatus.PENDING,
            ),
        ).thenReturn(emptyList())

        service.handle(message("done:20", callbackId = "callback-1"))

        verify(careTaskService).update(
            20,
            "mateo@example.com",
            CareTaskUpdateRequest(
                status = CareTaskStatus.COMPLETED,
                reason = "Marcado como realizado desde Telegram",
            ),
        )
        verify(botClient).answerCallbackQuery("callback-1", "Cuidado marcado como realizado")
    }

    @Test
    fun `later button creates one reminder for thirty minutes later`() {
        val channel = channel()
        val task = task(now.plusHours(2))
        prepareButton(channel, task)
        `when`(
            deliveryRepository.existsByCareTaskIdAndChannelIdAndStatus(
                20,
                10,
                NotificationDeliveryStatus.PENDING,
            ),
        ).thenReturn(false)

        service.handle(message("later:20", callbackId = "callback-2"))

        val delivery = ArgumentCaptor.forClass(NotificationDelivery::class.java)
        verify(deliveryRepository).save(delivery.capture())
        assertEquals(NotificationDeliveryStatus.PENDING, delivery.value.status)
        assertEquals(now.plusMinutes(30), delivery.value.scheduledFor)
        verify(botClient).answerCallbackQuery("callback-2", "Te recordaré en 30 minutos")
    }

    @Test
    fun `due snooze sends a new interactive reminder`() {
        val pending = NotificationDelivery(
            id = 30,
            careTask = task(now.plusHours(2)),
            channel = channel(),
            scheduledFor = now,
            status = NotificationDeliveryStatus.PENDING,
        )
        `when`(
            deliveryRepository.findAllByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(
                NotificationDeliveryStatus.PENDING,
                now,
            ),
        ).thenReturn(listOf(pending))

        service.sendDueSnoozes()

        val subject = ArgumentCaptor.forClass(String::class.java)
        val body = ArgumentCaptor.forClass(String::class.java)
        verify(messageService).sendCareReminder(
            anyChannel(),
            captureString(subject),
            captureString(body),
            org.mockito.Mockito.eq(20L),
        )
        assertEquals("Recordatorio: Thor · Dar comida", subject.value)
        assertTrue(body.value.contains("Recordatorio pospuesto"))
        assertEquals(NotificationDeliveryStatus.SENT, pending.status)
        assertEquals(now, pending.sentAt)
    }

    private fun prepareButton(channel: NotificationChannel, task: CareTask) {
        `when`(
            channelRepository.findByTypeAndDestination(NotificationChannelType.TELEGRAM, "123"),
        ).thenReturn(channel)
        `when`(taskRepository.findByIdAndPetOwnerId(20, 4)).thenReturn(task)
    }

    private fun message(text: String, callbackId: String? = null) = TelegramIncomingMessage(
        updateId = 1,
        chatId = "123",
        text = text,
        displayName = "Mateo",
        callbackQueryId = callbackId,
    )

    private fun channel() = NotificationChannel(
        id = 10,
        type = NotificationChannelType.TELEGRAM,
        destination = "123",
        verified = true,
        active = true,
        owner = User(
            id = 4,
            email = "mateo@example.com",
            fullName = "Mateo",
            passwordHash = "hash",
        ),
    )

    private fun task(scheduledAt: LocalDateTime): CareTask {
        val owner = User(
            id = 4,
            email = "mateo@example.com",
            fullName = "Mateo",
            passwordHash = "hash",
        )
        return CareTask(
            id = 20,
            title = "Dar comida",
            careType = "Alimentación",
            scheduledAt = scheduledAt,
            pet = Pet(id = 8, name = "Thor", species = "Gato", owner = owner),
        )
    }
}
