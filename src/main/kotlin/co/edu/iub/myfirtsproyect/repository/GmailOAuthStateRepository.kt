package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.GmailOAuthState
import org.springframework.data.jpa.repository.JpaRepository

interface GmailOAuthStateRepository : JpaRepository<GmailOAuthState, Long> {
    fun findByStateAndUsedAtIsNull(state: String): GmailOAuthState?
}
