package co.edu.iub.myfirtsproyect.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity

@RestController
class HealthController {

    @GetMapping("/api/health")
    fun home(): Map<String, String> {
        return mapOf("message" to "Taskora Pet API", "status" to "ok")
    }

    @GetMapping("/favicon.ico")
    fun favicon(): ResponseEntity<Void> = ResponseEntity.noContent().build()
}
