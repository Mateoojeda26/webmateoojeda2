package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.task.TaskCreateRequest
import co.edu.iub.myfirtsproyect.dto.task.TaskResponse
import co.edu.iub.myfirtsproyect.dto.task.TaskUpdateRequest
import co.edu.iub.myfirtsproyect.service.TaskService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody request: TaskCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTask(authentication.name, request))
    }

    @GetMapping
    fun list(authentication: Authentication): List<TaskResponse> {
        return taskService.getTasks(authentication.name)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, authentication: Authentication): TaskResponse {
        return taskService.getTask(id, authentication.name)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: TaskUpdateRequest,
        authentication: Authentication,
    ): TaskResponse {
        return taskService.updateTask(id, authentication.name, request)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        taskService.deleteTask(id, authentication.name)
        return ResponseEntity.noContent().build()
    }
}
