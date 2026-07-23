package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.MessageResponse
import co.edu.iub.myfirtsproyect.dto.user.ChangePasswordRequest
import co.edu.iub.myfirtsproyect.dto.user.UpdateProfileRequest
import co.edu.iub.myfirtsproyect.dto.user.UserResponse
import co.edu.iub.myfirtsproyect.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getProfile(authentication.name))
    }

    @PutMapping("/me")
    fun update(
        @Valid @RequestBody request: UpdateProfileRequest,
        authentication: Authentication,
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.updateProfile(authentication.name, request))
    }

    @PostMapping("/me/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        authentication: Authentication,
    ): ResponseEntity<MessageResponse> {
        return ResponseEntity.ok(userService.changePassword(authentication.name, request))
    }

    @DeleteMapping("/me")
    fun delete(authentication: Authentication): ResponseEntity<Void> {
        userService.deleteProfile(authentication.name)
        return ResponseEntity.noContent().build()
    }
}
