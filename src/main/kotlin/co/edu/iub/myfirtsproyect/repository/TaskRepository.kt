package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.Task
import org.springframework.data.jpa.repository.JpaRepository

interface TaskRepository : JpaRepository<Task, Long> {
    fun findAllByOwnerId(ownerId: Long): List<Task>
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Task?
}
