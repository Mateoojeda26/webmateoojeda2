package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.model.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByEmailAndIdNot(email: String, id: Long): Boolean
    fun existsByRole(role: UserRole): Boolean
    fun countByRole(role: UserRole): Long
    fun findByRole(role: UserRole): User?
}
