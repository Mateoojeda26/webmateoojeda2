package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelCreateRequest
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelResponse
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelUpdateRequest
import co.edu.iub.myfirtsproyect.dto.notification.GmailLinkResponse
import co.edu.iub.myfirtsproyect.dto.notification.NotificationTestResponse
import co.edu.iub.myfirtsproyect.dto.notification.TelegramLinkResponse
import co.edu.iub.myfirtsproyect.service.NotificationChannelService
import co.edu.iub.myfirtsproyect.service.GmailOAuthService
import co.edu.iub.myfirtsproyect.service.TelegramLinkService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/notifications/channels")
class NotificationChannelController(
    private val channelService: NotificationChannelService,
    private val telegramLinkService: TelegramLinkService,
    private val gmailOAuthService: GmailOAuthService,
) {
    @GetMapping
    fun list(authentication: Authentication): List<NotificationChannelResponse> =
        channelService.list(authentication.name)

    @PostMapping
    fun create(
        @Valid @RequestBody request: NotificationChannelCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<NotificationChannelResponse> = ResponseEntity.status(HttpStatus.CREATED)
        .body(channelService.create(authentication.name, request))

    @PostMapping("/telegram/link")
    fun createTelegramLink(authentication: Authentication): TelegramLinkResponse =
        telegramLinkService.createLink(authentication.name)

    @PostMapping("/telegram/link/{code}/confirm")
    fun confirmTelegramLink(
        @PathVariable code: String,
        authentication: Authentication,
    ): NotificationTestResponse {
        telegramLinkService.confirmLink(authentication.name, code)
        return NotificationTestResponse("Telegram vinculado correctamente")
    }

    @PostMapping("/gmail/link")
    fun createGmailLink(authentication: Authentication): GmailLinkResponse =
        gmailOAuthService.createLink(authentication.name)

    @GetMapping("/gmail/callback")
    fun completeGmailLink(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
    ): RedirectView {
        if (error != null || code.isNullOrBlank() || state.isNullOrBlank()) {
            return RedirectView("/notifications.html?gmail=cancelled")
        }
        return try {
            gmailOAuthService.completeLink(code, state)
            RedirectView("/notifications.html?gmail=connected")
        } catch (_: Exception) {
            RedirectView("/notifications.html?gmail=error")
        }
    }

    @PostMapping("/{id}/test")
    fun sendTest(
        @PathVariable id: Long,
        authentication: Authentication,
    ): NotificationTestResponse {
        channelService.sendTest(id, authentication.name)
        return NotificationTestResponse("Mensaje de prueba enviado")
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: NotificationChannelUpdateRequest,
        authentication: Authentication,
    ): NotificationChannelResponse = channelService.update(id, authentication.name, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        channelService.delete(id, authentication.name)
        return ResponseEntity.noContent().build()
    }
}
