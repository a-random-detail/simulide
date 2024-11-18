package com.simulide

import com.simulide.plugins.*
import com.simulide.plugins.domain.DocumentService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val dataSource: DataSource = createHikariDataSource()
    val documentService: DocumentService = DocumentService(dataSource)
    runMigrations()
    configureSockets()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    documentRoutes(documentService)
}

fun Application.runMigrations() {
    log.info("Running Flyway migrations")
    val host = environment.config.property("postgres.host").getString()
    val port = environment.config.property("postgres.port").getString()
    val dbName = environment.config.property("postgres.db_name").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()
    val url = "jdbc:postgresql://$host:$port/$dbName"
    val flyway = Flyway.configure()
        .dataSource(url, user, password)
        .load()
    flyway.migrate()
    log.info("Flyway migrations executed successfully")
}

fun Application.createHikariDataSource() : DataSource {
    val host = environment.config.property("postgres.host").getString()
    val port = environment.config.property("postgres.port").getString()
    val dbName = environment.config.property("postgres.db_name").getString()
    val user = environment.config.property("postgres.user").getString()
    var configPass = environment.config.property("postgres.password").getString()
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"
        username = user
        password = configPass
        driverClassName = "org.postgresql.Driver"

        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 10000
        connectionTimeout = 30000
        maxLifetime = 180000
    }

    return HikariDataSource(config)
}
