package co.edu.iub.myfirtsproyect.controller

import co.edu.iub.myfirtsproyect.dto.dashboard.DashboardResponse
import co.edu.iub.myfirtsproyect.service.CareTaskService
import co.edu.iub.myfirtsproyect.service.NotificationChannelService
import co.edu.iub.myfirtsproyect.service.PetService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val petService: PetService,
    private val careTaskService: CareTaskService,
    private val channelService: NotificationChannelService,
) {
    @GetMapping
    fun get(authentication: Authentication): DashboardResponse {
        val tasks = careTaskService.list(authentication.name, null)
            .filter { it.status == co.edu.iub.myfirtsproyect.model.CareTaskStatus.PENDING }
        return DashboardResponse(
            pets = petService.list(authentication.name),
            pendingTasks = tasks,
            channels = channelService.list(authentication.name),
        )
    }
}
