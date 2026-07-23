package co.edu.iub.myfirtsproyect.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class NotificationDeliveryStatus { PENDING, SENT, FAILED, DISCARDED, CANCELLED }

@Entity
@Table(name = "notification_deliveries")
class NotificationDelivery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_task_id", nullable = false)
    var careTask: CareTask,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    var channel: NotificationChannel,

    @Column(nullable = false)
    var scheduledFor: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: NotificationDeliveryStatus,

    var sentAt: LocalDateTime? = null,

    @Column(length = 500)
    var errorMessage: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
