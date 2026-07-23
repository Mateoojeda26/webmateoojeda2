package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.task.TaskCreateRequest
import co.edu.iub.myfirtsproyect.dto.task.TaskResponse
import co.edu.iub.myfirtsproyect.dto.task.TaskUpdateRequest
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidStatusTransitionException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.Task
import co.edu.iub.myfirtsproyect.model.TaskStatus
import co.edu.iub.myfirtsproyect.repository.TaskRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
) {
    fun createTask(ownerEmail: String, request: TaskCreateRequest): TaskResponse {
        val owner = findUserByEmail(ownerEmail)
        val task = Task(
            title = request.title,
            description = request.description,
            priority = request.priority,
            owner = owner,
        )
        return taskRepository.save(task).toResponse()
    }

    fun getTasks(ownerEmail: String): List<TaskResponse> {
        val owner = findUserByEmail(ownerEmail)
        return taskRepository.findAllByOwnerId(requireNotNull(owner.id)).map { it.toResponse() }
    }

    fun getTask(id: Long, ownerEmail: String): TaskResponse {
        val owner = findUserByEmail(ownerEmail)
        val task = taskRepository.findByIdAndOwnerId(id, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Task not found")
        return task.toResponse()
    }

    fun updateTask(id: Long, ownerEmail: String, request: TaskUpdateRequest): TaskResponse {
        val owner = findUserByEmail(ownerEmail)
        val task = taskRepository.findByIdAndOwnerId(id, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Task not found")

        request.status?.let { validateStatusTransition(task.status, it) }
        request.title?.let { task.title = it }
        task.description = request.description ?: task.description
        request.priority?.let { task.priority = it }
        request.status?.let { task.status = it }
        task.updatedAt = LocalDateTime.now()

        return taskRepository.save(task).toResponse()
    }

    fun deleteTask(id: Long, ownerEmail: String) {
        val owner = findUserByEmail(ownerEmail)
        val task = taskRepository.findByIdAndOwnerId(id, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Task not found")
        taskRepository.delete(task)
    }

    private fun findUserByEmail(email: String) =
        userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException("User not found")

    private fun validateStatusTransition(current: TaskStatus, next: TaskStatus) {
        if (current == next) return

        val allowed = when (current) {
            TaskStatus.PENDING -> next == TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> next == TaskStatus.DONE
            TaskStatus.DONE -> false
        }

        if (!allowed) {
            throw InvalidStatusTransitionException("Invalid task status transition")
        }
    }

    private fun Task.toResponse(): TaskResponse {
        return TaskResponse(
            id = requireNotNull(id),
            title = title,
            description = description,
            status = status,
            priority = priority,
            userId = requireNotNull(owner.id),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
