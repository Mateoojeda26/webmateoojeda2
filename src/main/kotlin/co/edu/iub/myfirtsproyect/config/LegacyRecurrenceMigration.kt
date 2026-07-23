package co.edu.iub.myfirtsproyect.config

import co.edu.iub.myfirtsproyect.model.CareTaskStatus
import co.edu.iub.myfirtsproyect.model.RecurrenceType
import co.edu.iub.myfirtsproyect.model.RecurringSeries
import co.edu.iub.myfirtsproyect.model.SeriesStatus
import co.edu.iub.myfirtsproyect.repository.CareTaskRepository
import co.edu.iub.myfirtsproyect.repository.RecurringSeriesRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

/**
 * Converts care tasks created before recurring series existed (recurrence stored
 * directly on the task) into real RecurringSeries rows, linking every legacy
 * occurrence to its series so the history is preserved.
 */
@Component
@Order(10)
class LegacyRecurrenceMigration(
    private val careTaskRepository: CareTaskRepository,
    private val seriesRepository: RecurringSeriesRepository,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(LegacyRecurrenceMigration::class.java)

    override fun run(args: ApplicationArguments) {
        transactionTemplate.execute {
            val legacyTasks = careTaskRepository.findAll()
                .filter { it.recurrence != RecurrenceType.NONE && it.series == null }
            if (legacyTasks.isEmpty()) return@execute
            logger.info("Migrating {} legacy recurring care tasks into series", legacyTasks.size)

            val groups = legacyTasks.groupBy { task ->
                listOf(
                    task.pet.id,
                    task.title,
                    task.careType,
                    task.recurrence.name,
                    task.recurrenceIntervalDays,
                    task.recurrenceDays,
                    task.recurrenceEndDate,
                )
            }
            groups.values.forEach { tasks ->
                val sample = tasks.minByOrNull { it.scheduledAt } ?: return@forEach
                val times = tasks.map { it.scheduledAt.toLocalTime() }.distinct().sorted()
                val hasPendingActive = tasks.any { it.active && it.status == CareTaskStatus.PENDING }
                val series = seriesRepository.save(
                    RecurringSeries(
                        title = sample.title,
                        description = sample.description,
                        careType = sample.careType,
                        priority = sample.priority,
                        frequency = sample.recurrence,
                        intervalDays = sample.recurrenceIntervalDays,
                        daysOfWeek = sample.recurrenceDays,
                        timesOfDay = times.joinToString(","),
                        startDate = sample.scheduledAt.toLocalDate(),
                        endDate = sample.recurrenceEndDate,
                        status = if (hasPendingActive) SeriesStatus.ACTIVE else SeriesStatus.PAUSED,
                        pet = sample.pet,
                        createdAt = sample.createdAt,
                        updatedAt = LocalDateTime.now(),
                    ),
                )
                tasks.forEach { task -> task.series = series }
                careTaskRepository.saveAll(tasks)
            }
            logger.info("Legacy recurrence migration completed: {} series created", groups.size)
        }
    }
}
