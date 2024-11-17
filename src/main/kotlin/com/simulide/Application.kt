package com.simulide

import com.simulide.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSockets()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
