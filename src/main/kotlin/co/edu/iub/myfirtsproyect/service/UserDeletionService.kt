package co.edu.iub.myfirtsproyect.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDeletionService(
    private val jdbcTemplate: JdbcTemplate,
    private val fileStorageService: FileStorageService,
) {
    @Transactional
    fun deleteUserAndOwnedData(userId: Long) {
        val files = findAssociatedFiles(userId)

        jdbcTemplate.update(
            """
            DELETE FROM notification_deliveries
            WHERE care_task_id IN (
                SELECT care_tasks.id FROM care_tasks
                JOIN pets ON pets.id = care_tasks.pet_id
                WHERE pets.owner_id = ?
            ) OR channel_id IN (
                SELECT id FROM notification_channels WHERE owner_id = ?
            )
            """.trimIndent(),
            userId,
            userId,
        )
        jdbcTemplate.update(
            """
            DELETE FROM care_evidence
            WHERE uploader_id = ? OR care_task_id IN (
                SELECT care_tasks.id FROM care_tasks
                JOIN pets ON pets.id = care_tasks.pet_id
                WHERE pets.owner_id = ?
            )
            """.trimIndent(),
            userId,
            userId,
        )
        jdbcTemplate.update(
            """
            DELETE FROM care_task_logs
            WHERE actor_id = ? OR care_task_id IN (
                SELECT care_tasks.id FROM care_tasks
                JOIN pets ON pets.id = care_tasks.pet_id
                WHERE pets.owner_id = ?
            )
            """.trimIndent(),
            userId,
            userId,
        )
        jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId)
        jdbcTemplate.update("DELETE FROM telegram_link_requests WHERE owner_id = ?", userId)
        jdbcTemplate.update("DELETE FROM gmail_oauth_states WHERE owner_id = ?", userId)
        jdbcTemplate.update("DELETE FROM gmail_authorizations WHERE owner_id = ?", userId)
        jdbcTemplate.update(
            "DELETE FROM caregiver_access WHERE caregiver_id = ? OR pet_id IN (SELECT id FROM pets WHERE owner_id = ?)",
            userId,
            userId,
        )
        jdbcTemplate.update("UPDATE care_tasks SET completed_by_id = NULL WHERE completed_by_id = ?", userId)
        jdbcTemplate.update("DELETE FROM care_tasks WHERE pet_id IN (SELECT id FROM pets WHERE owner_id = ?)", userId)
        jdbcTemplate.update("DELETE FROM recurring_series WHERE pet_id IN (SELECT id FROM pets WHERE owner_id = ?)", userId)
        jdbcTemplate.update("DELETE FROM notification_channels WHERE owner_id = ?", userId)
        jdbcTemplate.update("DELETE FROM pets WHERE owner_id = ?", userId)
        jdbcTemplate.update("DELETE FROM tasks WHERE user_id = ?", userId)
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId)

        files.forEach(fileStorageService::delete)
    }

    private fun findAssociatedFiles(userId: Long): Set<String> {
        val petPhotos = jdbcTemplate.queryForList(
            "SELECT photo_url FROM pets WHERE owner_id = ? AND photo_url IS NOT NULL",
            String::class.java,
            userId,
        )
        val evidenceFiles = jdbcTemplate.queryForList(
            """
            SELECT image_url FROM care_evidence
            WHERE uploader_id = ? OR care_task_id IN (
                SELECT care_tasks.id FROM care_tasks
                JOIN pets ON pets.id = care_tasks.pet_id
                WHERE pets.owner_id = ?
            )
            """.trimIndent(),
            String::class.java,
            userId,
            userId,
        )
        return (petPhotos + evidenceFiles).filterNotNull().toSet()
    }
}
