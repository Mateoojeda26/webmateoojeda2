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
import java.time.LocalDate

@Entity
@Table(name = "caregiver_access")
class CaregiverAccess(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id", nullable = false)
    var caregiver: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var permission: CaregiverPermission = CaregiverPermission.EDITOR,

    @Column(nullable = false)
    var active: Boolean = true,

    var startDate: LocalDate? = null,

    var endDate: LocalDate? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isAvailable(on: LocalDate = LocalDate.now()): Boolean {
        if (!active) return false
        if (startDate?.isAfter(on) == true) return false
        if (endDate?.isBefore(on) == true) return false
        return true
    }
}
