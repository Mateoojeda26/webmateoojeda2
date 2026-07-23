package co.edu.iub.myfirtsproyect.dto.auth

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val role: String,
    val redirectTo: String,
)
