package co.edu.iub.myfirtsproyect.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
open class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val securityExceptionHandler: SecurityExceptionHandler,
) {
    @Bean
    open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(securityExceptionHandler)
                it.accessDeniedHandler(securityExceptionHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    HttpMethod.GET,
                    "/", "/index.html", "/dashboard.html", "/agenda.html", "/pets.html",
                    "/caregivers.html", "/recurrences.html", "/notifications.html",
                    "/history.html", "/reports.html", "/profile.html", "/reset.html",
                    "/admin.html", "/admin.css", "/admin.js",
                    "/styles.css", "/app.js", "/portal.js", "/favicon.ico", "/api/health",
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                    "/swagger-resources/**", "/webjars/**",
                ).permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/forgot-password").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/reset-password").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/notifications/channels/gmail/callback").permitAll()
                it.requestMatchers("/api/admin/**").hasRole("ADMIN")
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
    }
}
