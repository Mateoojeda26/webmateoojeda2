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
import java.time.LocalTime

enum class SeriesStatus { ACTIVE, PAUSED, CANCELLED }

@Entity
@Table(name = "recurring_series")
class RecurringSeries(
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
    var frequency: RecurrenceType,

    var intervalDays: Int? = null,

    var daysOfWeek: String? = null,

    @Column(nullable = false)
    var timesOfDay: String,

    @Column(nullable = false)
    var startDate: LocalDate,

    var endDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SeriesStatus = SeriesStatus.ACTIVE,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun parsedTimes(): List<LocalTime> = timesOfDay.split(',')
        .mapNotNull { value -> runCatching { LocalTime.parse(value.trim()) }.getOrNull() }
        .distinct()
        .sorted()

    fun parsedDaysOfWeek(): Set<java.time.DayOfWeek> = daysOfWeek
        ?.split(',')
        ?.mapNotNull { value -> runCatching { java.time.DayOfWeek.valueOf(value.trim().uppercase()) }.getOrNull() }
        ?.toSet()
        .orEmpty()
}
