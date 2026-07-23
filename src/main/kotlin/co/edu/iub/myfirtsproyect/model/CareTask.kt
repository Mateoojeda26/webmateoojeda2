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
import java.time.LocalDate
import java.time.LocalDateTime

enum class CareTaskStatus { PENDING, COMPLETED, SKIPPED }
enum class CareTaskPriority { LOW, MEDIUM, HIGH }
enum class RecurrenceType { NONE, DAILY, WEEKLY, INTERVAL }

@Entity
@Table(name = "care_tasks")
class CareTask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(length = 2000)
    var description: String? = null,

    @Column(nullable = false)
    var careType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var priority: CareTaskPriority = CareTaskPriority.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CareTaskStatus = CareTaskStatus.PENDING,

    @Column(nullable = false)
    var scheduledAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var recurrence: RecurrenceType = RecurrenceType.NONE,

    var recurrenceIntervalDays: Int? = null,
    var recurrenceDays: String? = null,
    var recurrenceEndDate: LocalDate? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    var series: RecurringSeries? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    var completedBy: User? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    var completedAt: LocalDateTime? = null,
)
