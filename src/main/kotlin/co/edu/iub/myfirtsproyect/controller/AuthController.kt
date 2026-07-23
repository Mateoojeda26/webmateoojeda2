package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.auth.ForgotPasswordRequest
import co.edu.iub.myfirtsproyect.dto.auth.LoginRequest
import co.edu.iub.myfirtsproyect.dto.auth.PasswordResetMessageResponse
import co.edu.iub.myfirtsproyect.dto.auth.RegisterRequest
import co.edu.iub.myfirtsproyect.dto.auth.ResetPasswordRequest
import co.edu.iub.myfirtsproyect.dto.auth.TokenResponse
import co.edu.iub.myfirtsproyect.dto.user.UserResponse
import co.edu.iub.myfirtsproyect.service.AuthService
import co.edu.iub.myfirtsproyect.service.PasswordResetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
open class AuthController(
    private val authService: AuthService,
    private val passwordResetService: PasswordResetService,
) {
    @PostMapping("/register")
    open fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(authService.register(request))
    }

    @PostMapping("/login")
    open fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/forgot-password")
    open fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<PasswordResetMessageResponse> {
        passwordResetService.requestReset(request.email)
        return ResponseEntity.ok(
            PasswordResetMessageResponse(
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.",
            ),
        )
    }

    @PostMapping("/reset-password")
    open fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<PasswordResetMessageResponse> {
        passwordResetService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.ok(
            PasswordResetMessageResponse("Tu contraseña fue actualizada. Ya puedes iniciar sesión."),
        )
    }
}
