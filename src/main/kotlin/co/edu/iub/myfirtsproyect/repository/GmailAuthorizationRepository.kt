package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.GmailAuthorization
import org.springframework.data.jpa.repository.JpaRepository

interface GmailAuthorizationRepository : JpaRepository<GmailAuthorization, Long> {
    fun findByOwnerIdAndActiveTrue(ownerId: Long): GmailAuthorization?
    fun findFirstByActiveTrueOrderByUpdatedAtDesc(): GmailAuthorization?
}
