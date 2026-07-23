package co.edu.iub.myfirtsproyect.service

import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesCreateRequest
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesResponse
import co.edu.iub.myfirtsproyect.dto.series.RecurringSeriesUpdateRequest
import co.edu.iub.myfirtsproyect.exception.ForbiddenOperationException
import co.edu.iub.myfirtsproyect.exception.InvalidCredentialsException
import co.edu.iub.myfirtsproyect.exception.InvalidRequestException
import co.edu.iub.myfirtsproyect.exception.ResourceNotFoundException
import co.edu.iub.myfirtsproyect.model.CareTask
import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import co.edu.iub.myfirtsproyect.model.RecurringSeries
import co.edu.iub.myfirtsproyect.model.SeriesStatus
import co.edu.iub.myfirtsproyect.model.UserRole
import co.edu.iub.myfirtsproyect.model.User
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.CaregiverAccessRepository
import co.edu.iub.myfirtsproyect.repository.PetRepository
import co.edu.iub.myfirtsproyect.repository.RecurringSeriesRepository
import co.edu.iub.myfirtsproyect.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Service
class RecurringSeriesService(
    private val seriesRepository: RecurringSeriesRepository,
    private val careTaskRepository: CareTaskRepository,
    private val caregiverAccessRepository: CaregiverAccessRepository,
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
    private val reminderCancellationService: ReminderCancellationService,
    private val clock: Clock,
    @param:Value("\${app.series.horizon-days:3}") private val horizonDays: Long,
) {
    @Transactional
    fun create(ownerEmail: String, request: RecurringSeriesCreateRequest): RecurringSeriesResponse {
        val owner = findUser(ownerEmail)
        val pet = petRepository.findByIdAndOwnerIdAndArchivedFalse(request.petId, requireNotNull(owner.id))
            ?: throw ResourceNotFoundException("Pet not found")
        val frequency = validateFrequency(request.frequency, request.intervalDays, request.daysOfWeek)
        val times = validateTimes(request.timesOfDay)
        if (request.endDate?.isBefore(request.startDate) == true) {
            throw InvalidRequestException("La fecha final no puede ser anterior al inicio")
        }
        val now = LocalDateTime.now(clock)
        val series = seriesRepository.save(
            RecurringSeries(
                title = request.title.trim(),
                description = request.description?.trim()?.ifBlank { null },
                careType = request.careType.trim(),
                priority = request.priority,
                frequency = frequency,
                intervalDays = request.intervalDays,
                daysOfWeek = normalizeDays(request.daysOfWeek),
                timesOfDay = times.joinToString(","),
                startDate = request.startDate,
                endDate = request.endDate,
                pet = pet,
                createdAt = now,
                updatedAt = now,
            ),
        )
        generateOccurrences(series)
        return series.toResponse(canManage = true)
    }

    @Transactional(readOnly = true)
    fun list(email: String): List<RecurringSeriesResponse> {
        val user = findUser(email)
        val userId = requireNotNull(user.id)
        val owned = seriesRepository.findAllByPetOwnerIdOrderByCreatedAtDesc(userId)
        val sharedPetIds = caregiverAccessRepository
            .findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
            .filter { it.isAvailable(LocalDate.now(clock)) }
            .mapNotNull { it.pet.id }
        val shared = if (sharedPetIds.isEmpty()) emptyList() else {
            seriesRepository.findAllByPetIdInOrderByCreatedAtDesc(sharedPetIds)
        }
        val ownedIds = owned.mapNotNull { it.id }.toSet()
        return owned.map { it.toResponse(canManage = true) } +
            shared.filter { it.id !in ownedIds }.map { it.toResponse(canManage = false) }
    }

    @Transactional(readOnly = true)
    fun get(id: Long, email: String): RecurringSeriesResponse {
        val access = findSeriesAccess(id, email)
        return access.series.toResponse(access.owner)
    }

    @Transactional
    fun update(id: Long, email: String, request: RecurringSeriesUpdateRequest): RecurringSeriesResponse {
        val series = requireOwnedSeries(id, email)
        if (series.status == SeriesStatus.CANCELLED) {
            throw InvalidRequestException("Una serie cancelada no se puede editar")
        }
        if (request.title?.isBlank() == true || request.careType?.isBlank() == true) {
            throw InvalidRequestException("El título y el tipo de cuidado no pueden quedar vacíos")
        }
        if (request.clearEndDate && request.endDate != null) {
            throw InvalidRequestException("No se puede indicar y eliminar la fecha final al mismo tiempo")
        }
        val frequency = request.frequency ?: series.frequency
        val intervalDays = request.intervalDays ?: series.intervalDays
        val daysOfWeek = request.daysOfWeek ?: series.parsedDaysOfWeek().map { it.name }
        validateFrequency(frequency, intervalDays, daysOfWeek)
        val times = request.timesOfDay?.let { validateTimes(it) }

        request.title?.let { series.title = it.trim() }
        request.description?.let { series.description = it.trim().ifBlank { null } }
        request.careType?.let { series.careType = it.trim() }
        request.priority?.let { series.priority = it }
        series.frequency = frequency
        series.intervalDays = if (frequency == RecurrenceType.INTERVAL) intervalDays else null
        series.daysOfWeek = if (frequency == RecurrenceType.WEEKLY) normalizeDays(daysOfWeek) else null
        times?.let { series.timesOfDay = it.joinToString(",") }
        if (request.clearEndDate) {
            series.endDate = null
        } else request.endDate?.let {
            if (it.isBefore(series.startDate)) {
                throw InvalidRequestException("La fecha final no puede ser anterior al inicio")
            }
            series.endDate = it
        }
        series.updatedAt = LocalDateTime.now(clock)
        seriesRepository.save(series)

        val applyFrom = (request.applyFrom ?: LocalDate.now(clock)).atStartOfDay()
        deactivatePendingOccurrences(series, applyFrom)
        generateOccurrences(series, applyFrom.toLocalDate())
        return series.toResponse(canManage = true)
    }

    @Transactional
    fun pause(id: Long, email: String): RecurringSeriesResponse {
        val series = requireOwnedSeries(id, email)
        if (series.status != SeriesStatus.ACTIVE) {
            throw InvalidRequestException("Solo se puede pausar una serie activa")
        }
        series.status = SeriesStatus.PAUSED
        series.updatedAt = LocalDateTime.now(clock)
        seriesRepository.save(series)
        deactivatePendingOccurrences(series, LocalDateTime.now(clock))
        return series.toResponse(canManage = true)
    }

    @Transactional
    fun resume(id: Long, email: String): RecurringSeriesResponse {
        val series = requireOwnedSeries(id, email)
        if (series.status != SeriesStatus.PAUSED) {
            throw InvalidRequestException("Solo se puede reanudar una serie pausada")
        }
        series.status = SeriesStatus.ACTIVE
        series.updatedAt = LocalDateTime.now(clock)
        seriesRepository.save(series)
        generateOccurrences(series)
        return series.toResponse(canManage = true)
    }

    @Transactional
    fun cancel(id: Long, email: String) {
        val series = requireOwnedSeries(id, email)
        careTaskRepository.findAllBySeriesId(requireNotNull(series.id))
            .filter { it.active && it.status == CareTaskStatus.PENDING }
            .forEach { task ->
                reminderCancellationService.cancelTask(task, "Cancelado porque la serie recurrente fue eliminada")
            }
        series.status = SeriesStatus.CANCELLED
        series.updatedAt = LocalDateTime.now(clock)
        seriesRepository.save(series)
        deactivatePendingOccurrences(series, LocalDateTime.MIN)
    }

    @Transactional
    fun generateForAllActive() {
        seriesRepository.findAllByStatus(SeriesStatus.ACTIVE).forEach { generateOccurrences(it) }
    }

    fun generateOccurrences(series: RecurringSeries, fromDate: LocalDate? = null) {
        if (series.status != SeriesStatus.ACTIVE) return
        val today = LocalDate.now(clock)
        val start = maxOf(series.startDate, fromDate ?: today, today)
        val end = minOf(today.plusDays(horizonDays), series.endDate ?: LocalDate.MAX)
        if (end.isBefore(start)) return
        val times = series.parsedTimes()
        if (times.isEmpty()) return
        var date = start
        while (!date.isAfter(end)) {
            if (matchesFrequency(series, date)) {
                times.forEach { time -> ensureOccurrence(series, LocalDateTime.of(date, time)) }
            }
            date = date.plusDays(1)
        }
    }

    private fun ensureOccurrence(series: RecurringSeries, scheduledAt: LocalDateTime) {
        val seriesId = requireNotNull(series.id)
        val existing = careTaskRepository.findBySeriesIdAndScheduledAt(seriesId, scheduledAt)
        if (existing != null) {
            if (existing.status == CareTaskStatus.PENDING) {
                existing.title = series.title
                existing.description = series.description
                existing.careType = series.careType
                existing.priority = series.priority
                existing.active = true
                existing.updatedAt = LocalDateTime.now(clock)
                careTaskRepository.save(existing)
            }
            return
        }
        careTaskRepository.save(
            CareTask(
                title = series.title,
                description = series.description,
                careType = series.careType,
                priority = series.priority,
                scheduledAt = scheduledAt,
                recurrence = RecurrenceType.NONE,
                pet = series.pet,
                series = series,
                createdAt = LocalDateTime.now(clock),
                updatedAt = LocalDateTime.now(clock),
            ),
        )
    }

    private fun deactivatePendingOccurrences(series: RecurringSeries, from: LocalDateTime) {
        val seriesId = requireNotNull(series.id)
        val pending = if (from == LocalDateTime.MIN) {
            careTaskRepository.findAllBySeriesId(seriesId).filter { it.status == CareTaskStatus.PENDING }
        } else {
            careTaskRepository.findAllBySeriesIdAndStatusAndScheduledAtGreaterThanEqual(
                seriesId,
                CareTaskStatus.PENDING,
                from,
            )
        }
        val now = LocalDateTime.now(clock)
        pending.forEach { task ->
            task.active = false
            task.updatedAt = now
        }
        careTaskRepository.saveAll(pending)
    }

    private fun matchesFrequency(series: RecurringSeries, date: LocalDate): Boolean = when (series.frequency) {
        RecurrenceType.DAILY -> true
        RecurrenceType.WEEKLY -> {
            val days = series.parsedDaysOfWeek().ifEmpty { setOf(series.startDate.dayOfWeek) }
            date.dayOfWeek in days
        }
        RecurrenceType.INTERVAL -> {
            val interval = (series.intervalDays ?: 1).coerceAtLeast(1)
            ChronoUnit.DAYS.between(series.startDate, date) % interval == 0L
        }
        RecurrenceType.NONE -> false
    }

    private fun validateFrequency(
        frequency: RecurrenceType,
        intervalDays: Int?,
        daysOfWeek: List<String>?,
    ): RecurrenceType {
        when (frequency) {
            RecurrenceType.NONE -> throw InvalidRequestException("La serie debe ser diaria, semanal o por intervalo")
            RecurrenceType.INTERVAL -> {
                if (intervalDays == null || intervalDays < 1) {
                    throw InvalidRequestException("El intervalo en días debe ser mayor o igual a 1")
                }
            }
            RecurrenceType.WEEKLY -> {
                val requestedDays = daysOfWeek.orEmpty()
                val valid = requestedDays.isNotEmpty() && requestedDays.all { day ->
                    runCatching { DayOfWeek.valueOf(day.trim().uppercase()) }.isSuccess
                }
                if (!valid) throw InvalidRequestException("Selecciona al menos un día de la semana válido")
            }
            RecurrenceType.DAILY -> Unit
        }
        return frequency
    }

    private fun validateTimes(times: List<String>): List<LocalTime> {
        val parsed = times.map { value ->
            runCatching { LocalTime.parse(value.trim()) }.getOrElse {
                throw InvalidRequestException("Las horas deben tener formato HH:mm")
            }
        }
        if (parsed.isEmpty()) throw InvalidRequestException("La serie necesita al menos un horario")
        if (parsed.size != parsed.distinct().size) {
            throw InvalidRequestException("Los horarios no pueden repetirse")
        }
        return parsed.sorted()
    }

    private fun normalizeDays(days: List<String>?): String? = days
        ?.mapNotNull { day -> runCatching { DayOfWeek.valueOf(day.trim().uppercase()).name }.getOrNull() }
        ?.distinct()
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(",")

    private fun requireOwnedSeries(id: Long, email: String): RecurringSeries {
        val access = findSeriesAccess(id, email)
        if (!access.owner) {
            throw ForbiddenOperationException("Solo el propietario puede administrar la serie")
        }
        return access.series
    }

    private fun findSeriesAccess(id: Long, email: String): SeriesAccess {
        val user = findUser(email)
        val userId = requireNotNull(user.id)
        val series = seriesRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Serie no encontrada")
        }
        if (user.role == UserRole.ADMIN) return SeriesAccess(series, user, owner = true)
        if (series.pet.owner.id == userId) return SeriesAccess(series, user, owner = true)
        val shared = caregiverAccessRepository
            .findAllByCaregiverIdAndActiveTrueOrderByCreatedAtDesc(userId)
            .any { it.pet.id == series.pet.id && it.isAvailable(LocalDate.now(clock)) }
        if (!shared) throw ResourceNotFoundException("Serie no encontrada")
        return SeriesAccess(series, user, owner = false)
    }

    private fun findUser(email: String) = userRepository.findByEmail(email)
        ?: throw InvalidCredentialsException("User not found")

    private fun RecurringSeries.toResponse(canManage: Boolean): RecurringSeriesResponse {
        val occurrences = id?.let { careTaskRepository.findAllBySeriesId(it) }.orEmpty()
        return RecurringSeriesResponse(
            id = requireNotNull(id),
            title = title,
            description = description,
            careType = careType,
            priority = priority,
            frequency = frequency,
            intervalDays = intervalDays,
            daysOfWeek = parsedDaysOfWeek().map { it.name },
            timesOfDay = parsedTimes().map { it.toString() },
            startDate = startDate,
            endDate = endDate,
            status = status,
            petId = requireNotNull(pet.id),
            petName = pet.name,
            canManage = canManage,
            pendingOccurrences = occurrences.count { it.active && it.status == CareTaskStatus.PENDING },
            completedOccurrences = occurrences.count { it.status == CareTaskStatus.COMPLETED },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private data class SeriesAccess(val series: RecurringSeries, val user: User, val owner: Boolean)
}
