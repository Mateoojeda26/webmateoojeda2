package co.edu.iub.myfirtsproyect.repository

import co.edu.iub.myfirtsproyect.model.NotificationDelivery
import co.edu.iub.myfirtsproyect.model.NotificationDeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface NotificationDeliveryRepository : JpaRepository<NotificationDelivery, Long> {
    fun findAllByStatusOrderByCreatedAtDesc(status: NotificationDeliveryStatus): List<NotificationDelivery>
    fun countByStatus(status: NotificationDeliveryStatus): Long
    fun existsByCareTaskIdAndChannelIdAndScheduledForAndStatus(
        careTaskId: Long,
        channelId: Long,
        scheduledFor: LocalDateTime,
        status: NotificationDeliveryStatus,
    ): Boolean

    fun existsByCareTaskIdAndChannelIdAndScheduledForAndStatusIn(
        careTaskId: Long,
        channelId: Long,
        scheduledFor: LocalDateTime,
        statuses: Collection<NotificationDeliveryStatus>,
    ): Boolean

    fun countByCareTaskIdAndChannelIdAndScheduledForAndStatus(
        careTaskId: Long,
        channelId: Long,
        scheduledFor: LocalDateTime,
        status: NotificationDeliveryStatus,
    ): Long

    fun existsByCareTaskIdAndChannelIdAndStatus(
        careTaskId: Long,
        channelId: Long,
        status: NotificationDeliveryStatus,
    ): Boolean

    fun findAllByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(
        status: NotificationDeliveryStatus,
        scheduledFor: LocalDateTime,
    ): List<NotificationDelivery>

    fun findAllByCareTaskIdAndChannelIdAndStatus(
        careTaskId: Long,
        channelId: Long,
        status: NotificationDeliveryStatus,
    ): List<NotificationDelivery>
}
