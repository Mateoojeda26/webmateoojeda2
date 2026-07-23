package co.edu.iub.myfirtsproyect.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Updates old SQLite databases. Hibernate cannot alter an existing CHECK
 * constraint, so the affected table must be rebuilt once.
 */
@Component
@Order(0)
class SQLiteSchemaMigration(
    private val dataSource: DataSource,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(SQLiteSchemaMigration::class.java)

    override fun run(args: ApplicationArguments) {
        migrateNotificationChannels()
        migrateNotificationDeliveries()
        migrateUsers()
        createSingleAdminIndex()
    }

    private fun migrateUsers() {
        val schema = tableSchema("users") ?: return
        if (schema.contains("'ADMIN'")) return

        logger.info("Migrating users to support the administrator role")
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement -> statement.execute("PRAGMA foreign_keys = OFF") }
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.execute("DROP TABLE IF EXISTS users_new")
                    statement.execute(
                        """
                        CREATE TABLE users_new (
                            id INTEGER PRIMARY KEY,
                            active BOOLEAN NOT NULL,
                            created_at TIMESTAMP NOT NULL,
                            email VARCHAR(255) NOT NULL UNIQUE,
                            full_name VARCHAR(255) NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            phone_number VARCHAR(255),
                            role VARCHAR(255) NOT NULL CHECK (role IN ('USER','ADMIN')),
                            account_status VARCHAR(255) DEFAULT 'ACTIVE' NOT NULL
                                CHECK (account_status IN ('ACTIVE','SUSPENDED')),
                            suspended_at TIMESTAMP,
                            suspended_by VARCHAR(255),
                            suspension_reason VARCHAR(500)
                        )
                        """.trimIndent(),
                    )
                    statement.execute(
                        """
                        INSERT INTO users_new
                            (id, active, created_at, email, full_name, password_hash, phone_number,
                             role, account_status, suspended_at, suspended_by, suspension_reason)
                        SELECT id, active, created_at, email, full_name, password_hash, phone_number,
                               role, account_status, suspended_at, suspended_by, suspension_reason
                        FROM users
                        """.trimIndent(),
                    )
                    statement.execute("DROP TABLE users")
                    statement.execute("ALTER TABLE users_new RENAME TO users")
                }
                connection.commit()
                logger.info("users migration completed")
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
                connection.createStatement().use { statement -> statement.execute("PRAGMA foreign_keys = ON") }
            }
        }
    }

    private fun createSingleAdminIndex() {
        if (tableSchema("users") == null) return
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS ux_users_single_admin " +
                        "ON users(role) WHERE role = 'ADMIN'",
                )
            }
        }
    }

    private fun tableSchema(name: String): String? = dataSource.connection.use { connection ->
        connection.prepareStatement(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
        ).use { statement ->
            statement.setString(1, name)
            statement.executeQuery().use { result -> if (result.next()) result.getString(1) else null }
        }
    }

    private fun migrateNotificationDeliveries() {
        val schema = tableSchema("notification_deliveries") ?: return
        if (schema.contains("'PENDING'")) return

        logger.info("Migrating notification_deliveries to support all reminder statuses")
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement -> statement.execute("PRAGMA foreign_keys = OFF") }
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.execute("DROP TABLE IF EXISTS notification_deliveries_new")
                    statement.execute(
                        """
                        CREATE TABLE notification_deliveries_new (
                            id INTEGER PRIMARY KEY,
                            created_at TIMESTAMP NOT NULL,
                            error_message VARCHAR(500),
                            scheduled_for TIMESTAMP NOT NULL,
                            sent_at TIMESTAMP,
                            status VARCHAR(255) NOT NULL
                                CHECK (status IN ('PENDING','SENT','FAILED','DISCARDED','CANCELLED')),
                            care_task_id BIGINT NOT NULL,
                            channel_id BIGINT NOT NULL
                        )
                        """.trimIndent(),
                    )
                    statement.execute(
                        """
                        INSERT INTO notification_deliveries_new
                            (id, created_at, error_message, scheduled_for, sent_at, status, care_task_id, channel_id)
                        SELECT id, created_at, error_message, scheduled_for, sent_at, status, care_task_id, channel_id
                        FROM notification_deliveries
                        """.trimIndent(),
                    )
                    statement.execute("DROP TABLE notification_deliveries")
                    statement.execute("ALTER TABLE notification_deliveries_new RENAME TO notification_deliveries")
                }
                connection.commit()
                logger.info("notification_deliveries migration completed")
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
                connection.createStatement().use { statement -> statement.execute("PRAGMA foreign_keys = ON") }
            }
        }
    }

    private fun migrateNotificationChannels() {
        val schema = tableSchema("notification_channels") ?: return
        if (!schema.contains("'WHATSAPP'") || schema.contains("'TELEGRAM'")) return

        dataSource.connection.use { connection ->
            logger.info("Migrating notification_channels to support Telegram")
            connection.createStatement().use { statement -> statement.execute("PRAGMA foreign_keys = OFF") }
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.execute("DROP TABLE IF EXISTS notification_channels_new")
                    statement.execute(
                        """
                        CREATE TABLE notification_channels_new (
                            id INTEGER PRIMARY KEY,
                            active BOOLEAN NOT NULL,
                            created_at TIMESTAMP NOT NULL,
                            destination VARCHAR(255) NOT NULL,
                            label VARCHAR(255),
                            type VARCHAR(255) NOT NULL,
                            updated_at TIMESTAMP NOT NULL,
                            verified BOOLEAN NOT NULL,
                            owner_id BIGINT NOT NULL
                        )
                        """.trimIndent(),
                    )
                    statement.execute(
                        """
                        INSERT INTO notification_channels_new
                            (id, active, created_at, destination, label, type, updated_at, verified, owner_id)
                        SELECT id, active, created_at, destination, label, type, updated_at, verified, owner_id
                        FROM notification_channels
                        """.trimIndent(),
                    )
                    statement.execute("DROP TABLE notification_channels")
                    statement.execute("ALTER TABLE notification_channels_new RENAME TO notification_channels")
                }
                connection.commit()
                logger.info("Telegram database migration completed")
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
                connection.createStatement().use { statement -> statement.execute("PRAGMA foreign_keys = ON") }
            }
        }
    }
}
