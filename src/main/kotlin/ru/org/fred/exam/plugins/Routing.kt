@file:OptIn(KtorExperimentalLocationsAPI::class)
@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package ru.org.fred.exam.plugins

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Location
import io.ktor.server.locations.Locations
import io.ktor.server.locations.get
import io.ktor.server.pebble.Pebble
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import ru.org.fred.exam.QuestionService
import ru.org.fred.exam.UserAnswers

fun Application.configureRouting(questionService: QuestionService) {

    install(Locations) {
    }
    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
        })
    }


    routing {
        get("/") {
            questionService.resetStat()
            call.respond(PebbleContent("intro.html", emptyMap()))
        }
        get<QuestionLocation> { location ->
            val q = questionService.getQuestion(location.questionNum)
            call.respond(PebbleContent("question.html", mapOf("question" to q)))
        }
        get("/final") {
            val stat = questionService.finalStat()
            call.respond(
                PebbleContent(
                    "final.html",
                    mapOf(
                        "questionCount" to stat.totalQuestions,
                        "answered" to stat.answered,
                        "answers" to stat.finalAnswers,
                        "correctAnswers" to stat.correctAnswers
                    )
                )
            )
        }
        get("/mustdie") {
            call.respond(PebbleContent("mustdie.html", emptyMap()))
        }
        get<CheckQuestionLocation> { location ->
            log.info("Location = {}", location)
            val hasPrevQuestion = location.questionNum != questionService.firstQuestionNum()
            val hasNextQuestion = location.questionNum != questionService.lastQuestionNum()
            val userAnswer = UserAnswers(
                if (location.listGroupRadios == -1) {
                    location.listGroupCheck
                } else {
                    listOf(location.listGroupRadios)
                }
            )
            val correctAnswer = questionService.checkAnswer(location.questionNum, userAnswer)
            val question = questionService.getQuestion(location.questionNum)
            call.respond(
                PebbleContent(
                    "check.html", mapOf(
                        "question" to question,
                        "hasPrevious" to hasPrevQuestion,
                        "hasNext" to hasNextQuestion,
                        "prevQuestionNum" to location.questionNum - 1,
                        "nextQuestionNum" to location.questionNum + 1,
                        "correctAnswer" to correctAnswer,
                        "userAnswers" to userAnswer
                    )
                )
            )
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}

@Location("/question/{questionNum}")
class QuestionLocation(val questionNum: Int)

@Location("/question/{questionNum}/check")
data class CheckQuestionLocation(
    val questionNum: Int,
    val listGroupCheck: List<Int> = emptyList(),
    val listGroupRadios: Int = -1
)

