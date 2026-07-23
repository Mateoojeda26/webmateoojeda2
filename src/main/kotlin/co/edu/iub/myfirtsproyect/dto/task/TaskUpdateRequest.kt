package co.edu.iub.myfirtsproyect.dto.task

import co.edu.iub.myfirtsproyect.model.TaskPriority
import co.edu.iub.myfirtsproyect.model.TaskStatus

data class TaskUpdateRequest(
    val title: String?,
    val description: String?,
    val status: TaskStatus?,
    val priority: TaskPriority?,
)
