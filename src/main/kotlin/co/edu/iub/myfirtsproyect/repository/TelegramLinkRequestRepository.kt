package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.TelegramLinkRequest
import org.springframework.data.jpa.repository.JpaRepository

interface TelegramLinkRequestRepository : JpaRepository<TelegramLinkRequest, Long> {
    fun findByCode(code: String): TelegramLinkRequest?
    fun findByCodeAndUsedAtIsNull(code: String): TelegramLinkRequest?
}
