package com.simulide.plugins

import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class MustacheUser(val id: Int, val name: String)
data class LineOfCode(val code: String)

val test = """
    function hello_world() {
        console.log("hello, world!");
    }
    
    hello_world();
""".trimIndent()

fun Application.configureRouting() {
    install(Mustache) {
        mustacheFactory = DefaultMustacheFactory("templates")
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respond(MustacheContent("htmx/index.hbs", mapOf("user" to MustacheUser(1, "user1"))))
        }

        get("/code/new") {
            val lines = test.split("\n").map { x -> LineOfCode(x)}
            val codeContent = mapOf("lines" to lines)
            call.respond(MustacheContent("htmx/code.hbs", codeContent))
        }
    }
}
