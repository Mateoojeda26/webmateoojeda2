package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationChannelRepository : JpaRepository<NotificationChannel, Long> {
    fun findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(ownerId: Long): List<NotificationChannel>
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<NotificationChannel>
    fun findByIdAndOwnerId(id: Long, ownerId: Long): NotificationChannel?
    fun findFirstByOwnerIdAndType(ownerId: Long, type: NotificationChannelType): NotificationChannel?
    fun findByTypeAndDestination(type: NotificationChannelType, destination: String): NotificationChannel?
    fun findAllByOwnerIdAndTypeAndVerifiedTrueAndActiveTrue(
        ownerId: Long,
        type: NotificationChannelType,
    ): List<NotificationChannel>
    fun findAllByOwnerIdAndVerifiedTrueAndActiveTrue(ownerId: Long): List<NotificationChannel>
}
