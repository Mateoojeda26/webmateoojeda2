package co.edu.iub.myfirtsproyect.dto.dashboard

import co.edu.iub.myfirtsproyect.dto.care.CareTaskResponse
import co.edu.iub.myfirtsproyect.dto.pet.PetResponse
import co.edu.iub.myfirtsproyect.dto.notification.NotificationChannelResponse

data class DashboardResponse(
    val pets: List<PetResponse>,
    val pendingTasks: List<CareTaskResponse>,
    val channels: List<NotificationChannelResponse>,
)
