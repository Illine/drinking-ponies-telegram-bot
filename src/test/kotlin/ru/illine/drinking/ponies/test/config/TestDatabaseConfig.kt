package ru.illine.drinking.ponies.test.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

@TestConfiguration
class TestDatabaseConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    fun postgresContainer(dockerPostgres: DockerImageName): PostgreSQLContainer<*> =
        PostgreSQLContainer(dockerPostgres)
            .withReuse(true)
            .withInitScript(INIT_DATABASE_FILE_PATH)
            .waitingFor(Wait.forListeningPort())

    @Bean
    fun dockerPostgres(): DockerImageName =
        DockerImageName
            .parse(POSTGRES_IMAGE)
            .asCompatibleSubstituteFor(COMPATIBLE_POSTGRES)

    @Bean
    fun dataSource(postgresContainer: PostgreSQLContainer<*>): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = postgresContainer.getJdbcUrl()
        hikariConfig.username = postgresContainer.username
        hikariConfig.password = postgresContainer.password
        return HikariDataSource(hikariConfig)
    }

    companion object {
        private const val POSTGRES_IMAGE = "postgres:14-alpine"
        private const val INIT_DATABASE_FILE_PATH = "sql/init.sql"
        private const val COMPATIBLE_POSTGRES = "postgres"
    }
}
