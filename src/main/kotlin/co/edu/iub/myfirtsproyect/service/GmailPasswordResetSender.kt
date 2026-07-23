package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.model.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GmailPasswordResetSender(
    private val gmailOAuthService: GmailOAuthService,
    @param:Value("\${app.password-reset.sender-user-email:}") private val senderUserEmail: String,
    @param:Value("\${app.password-reset.expiration-minutes:30}") private val expirationMinutes: Long,
) : PasswordResetSender {
    override fun send(user: User, resetLink: String) {
        val body = buildString {
            appendLine("Hola ${user.fullName},")
            appendLine()
            appendLine("Recibimos una solicitud para restablecer tu contraseña de Taskora Pet.")
            appendLine("Abre este enlace para crear una contraseña nueva (vence en $expirationMinutes minutos):")
            appendLine()
            appendLine(resetLink)
            appendLine()
            appendLine("Si no solicitaste este cambio puedes ignorar este mensaje.")
        }
        gmailOAuthService.sendFromApplicationAccount(
            senderUserEmail,
            user.email,
            "Restablece tu contraseña de Taskora Pet",
            body,
        )
    }
}
