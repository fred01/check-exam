package ru.org.fred.exam

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.org.fred.exam.plugins.*

fun main() {
    val questionService = QuestionService()
    questionService.loadQuestions()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting(questionService)
        configureMonitoring()
    }.start(wait = true)
}
