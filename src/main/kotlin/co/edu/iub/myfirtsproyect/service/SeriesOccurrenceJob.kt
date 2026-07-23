package co.edu.iub.myfirtsproyect.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SeriesOccurrenceJob(
    private val seriesService: RecurringSeriesService,
    @param:Value("\${app.series.auto-generate:true}") private val autoGenerate: Boolean,
) {
    private val logger = LoggerFactory.getLogger(SeriesOccurrenceJob::class.java)

    @Scheduled(fixedDelayString = "\${app.series.generate-delay-ms:300000}")
    fun generate() {
        if (!autoGenerate) return
        try {
            seriesService.generateForAllActive()
        } catch (ex: Exception) {
            logger.error("No fue posible generar las ocurrencias de las series", ex)
        }
    }
}
