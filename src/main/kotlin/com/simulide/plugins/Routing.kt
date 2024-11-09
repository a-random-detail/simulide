package com.simulide.plugins

import com.github.mustachejava.DefaultMustacheFactory
import com.simulide.domain.ExecutableCode
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.util.UUID

data class MustacheUser(val id: Int, val name: String)
data class IndexData(val user: MustacheUser, val csrfToken: String)

fun Application.configureRouting() {
    install(Mustache) {
        mustacheFactory = DefaultMustacheFactory("templates")
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    routing {
        get("/") {
            val payload = mapOf("data" to IndexData(MustacheUser(1, "user1"), UUID.randomUUID().toString()))
            call.respond(MustacheContent("htmx/index.hbs", payload))
        }

        post("/save-code" ) {
            val payload = call.receive<ExecutableCode>()
            println(">>>> payload parsed: $payload")
            call.respond(HttpStatusCode.Created, payload)
        }
    }
}
