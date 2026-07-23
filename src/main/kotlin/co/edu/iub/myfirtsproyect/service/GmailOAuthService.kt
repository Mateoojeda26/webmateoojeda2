package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.notification.GmailLinkResponse
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.model.GmailAuthorization
import co.edu.iub.myfirtsproyect.model.GmailOAuthState
import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.repository.GmailAuthorizationRepository
import co.edu.iub.myfirtsproyect.repository.GmailOAuthStateRepository
import co.edu.iub.myfirtsproyect.repository.NotificationChannelRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class GmailOAuthService(
    private val gmailApiClient: GmailApiClient,
    private val credentialCipher: CredentialCipher,
    private val authorizationRepository: GmailAuthorizationRepository,
    private val stateRepository: GmailOAuthStateRepository,
    private val channelRepository: NotificationChannelRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createLink(ownerEmail: String): GmailLinkResponse {
        val owner = userRepository.findByEmail(ownerEmail)
            ?: throw InvalidCredentialsException("User not found")
        val state = GmailOAuthState(
            state = UUID.randomUUID().toString().replace("-", ""),
            owner = owner,
            expiresAt = LocalDateTime.now().plusMinutes(10),
        )
        stateRepository.save(state)
        return GmailLinkResponse(gmailApiClient.authorizationUrl(state.state))
    }

    @Transactional
    fun completeLink(code: String, stateValue: String) {
        val state = stateRepository.findByStateAndUsedAtIsNull(stateValue)
            ?: throw InvalidRequestException("La autorización de Gmail no existe o ya fue utilizada")
        if (state.expiresAt.isBefore(LocalDateTime.now())) {
            throw InvalidRequestException("La autorización de Gmail venció. Intenta conectarla nuevamente")
        }
        val tokens = gmailApiClient.exchangeCode(code)
        val email = gmailApiClient.accountEmail(tokens.accessToken).lowercase()
        val ownerId = requireNotNull(state.owner.id)
        val authorization = authorizationRepository.findByOwnerIdAndActiveTrue(ownerId)
            ?: GmailAuthorization(
                owner = state.owner,
                email = email,
                encryptedRefreshToken = credentialCipher.encrypt(tokens.refreshToken),
            )
        authorization.email = email
        authorization.encryptedRefreshToken = credentialCipher.encrypt(tokens.refreshToken)
        authorization.active = true
        authorization.updatedAt = LocalDateTime.now()
        authorizationRepository.save(authorization)

        val channel = channelRepository.findFirstByOwnerIdAndType(ownerId, NotificationChannelType.EMAIL)
            ?: NotificationChannel(
                type = NotificationChannelType.EMAIL,
                destination = email,
                owner = state.owner,
            )
        channel.destination = email
        channel.label = email
        channel.verified = true
        channel.active = true
        channel.updatedAt = LocalDateTime.now()
        channelRepository.save(channel)
        state.usedAt = LocalDateTime.now()
        stateRepository.save(state)
    }

    @Transactional(readOnly = true, noRollbackFor = [Exception::class])
    fun send(ownerId: Long, recipient: String, subject: String, body: String) {
        val authorization = authorizationRepository.findByOwnerIdAndActiveTrue(ownerId)
            ?: throw InvalidRequestException("Gmail no está autorizado para esta cuenta")
        sendWithAuthorization(authorization, recipient, subject, body)
    }

    /**
     * Password recovery cannot depend on the recipient having linked Gmail before
     * losing access. A configured Taskora Pet account is preferred; for the local
     * single-owner installation, the most recently connected Gmail account is the
     * safe operational fallback.
     */
    @Transactional(readOnly = true, noRollbackFor = [Exception::class])
    fun sendFromApplicationAccount(
        senderUserEmail: String?,
        recipient: String,
        subject: String,
        body: String,
    ) {
        val configuredAuthorization = senderUserEmail
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?.let { email -> userRepository.findByEmail(email) }
            ?.id
            ?.let { ownerId -> authorizationRepository.findByOwnerIdAndActiveTrue(ownerId) }
        val authorization = configuredAuthorization
            ?: authorizationRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
            ?: throw InvalidRequestException(
                "Conecta una cuenta Gmail de Taskora Pet antes de habilitar la recuperación de contraseña",
            )
        sendWithAuthorization(authorization, recipient, subject, body)
    }

    private fun sendWithAuthorization(
        authorization: GmailAuthorization,
        recipient: String,
        subject: String,
        body: String,
    ) {
        val refreshToken = credentialCipher.decrypt(authorization.encryptedRefreshToken)
        val accessToken = gmailApiClient.refreshAccessToken(refreshToken)
        gmailApiClient.sendEmail(accessToken, authorization.email, recipient, subject, body)
    }
}
