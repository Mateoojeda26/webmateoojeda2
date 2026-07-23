package co.edu.iub.myfirtsproyect.dto.task

import co.edu.iub.myfirtsproyect.model.TaskPriority
import co.edu.iub.myfirtsproyect.model.TaskStatus
import java.time.LocalDateTime

data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val userId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
