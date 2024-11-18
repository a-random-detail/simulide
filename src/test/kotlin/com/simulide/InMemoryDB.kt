package com.simulide

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun createTestDataSource(): DataSource {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        username = "sa"
        password = ""
        driverClassName = "org.h2.Driver"
    }
    return HikariDataSource(config)
}

fun migrateTestDatabase(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()
}

fun setupTestDatabase(): DataSource {
    val dataSource = createTestDataSource()
    migrateTestDatabase(dataSource)
    return dataSource
}

fun cleanupDatabase(dataSource: DataSource) {
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE")
            statement.execute("TRUNCATE TABLE operations")
            statement.execute("TRUNCATE TABLE documents")
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE")
        }
    }
}