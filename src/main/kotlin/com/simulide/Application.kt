package com.simulide

import com.simulide.plugins.*
import io.ktor.server.application.*
import org.flywaydb.core.Flyway

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    runMigrations()
    configureSockets()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
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
