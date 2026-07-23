package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.exception.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class TelegramUpdatePollingService(
    private val telegramBotClient: TelegramBotClient,
    private val telegramInteractionService: TelegramInteractionService,
    @Value("\${app.telegram.link-polling-enabled:false}") private val linkPollingEnabled: Boolean,
) {
    private val logger = LoggerFactory.getLogger(TelegramUpdatePollingService::class.java)
    private val nextOffset = AtomicLong(0)

    @Scheduled(fixedDelayString = "\${app.telegram.poll-delay-ms:3000}")
    fun poll() {
        if (!linkPollingEnabled || !telegramBotClient.isConfigured()) return
        try {
            telegramBotClient.getUpdates(nextOffset.get()).forEach { incoming ->
                telegramInteractionService.handle(incoming)
                nextOffset.updateAndGet { current -> maxOf(current, incoming.updateId + 1) }
            }
            telegramInteractionService.sendDueSnoozes()
        } catch (ex: ApiException) {
            logger.warn("Telegram polling unavailable: {}", ex.message)
        } catch (ex: Exception) {
            logger.error("Unexpected Telegram polling error", ex)
        }
    }
}
