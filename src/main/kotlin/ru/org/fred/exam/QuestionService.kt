package ru.org.fred.exam

import org.slf4j.LoggerFactory
import java.security.InvalidParameterException

data class Exam(
    val questions: MutableList<Question>,
    val questionMap: MutableMap<Int, Question> = mutableMapOf()
)

data class Question(
    val num: Int,
    val questionText: String,
    val answers: MutableList<Answer>
) {
    @Suppress("unused")
    val multipleCorrectAnswers: Boolean
        get() = answers.count { it.correctAnswer } > 1
}

data class Answer(
    val id:Int,
    val answerText: String,
    val correctAnswer: Boolean
)

data class UserAnswers(
    val answerIds:List<Int>,
) {
    @Suppress("unused")
    fun hasAnswer(answerId: Int): Boolean {
        LoggerFactory.getLogger(UserAnswers::class.java).info("Answers = $answerIds, check $answerId")
        return answerIds.contains(answerId)
    }
}

data class FinalStat(
    val totalQuestions:Int,
    val answered:Int,
    val correctAnswers: Int,
    val finalAnswers: List<FinalAnswer>
)

data class FinalAnswer(
    val questionNum: Int,
    val text:String,
    val correct:Boolean,
    val notAnswered:Boolean,
    val wrong:Boolean,
    val question: Question,
    val userAnswers: UserAnswers?
)

class QuestionService {
    private val logger = LoggerFactory.getLogger(QuestionService::class.java)
    val exam = Exam(mutableListOf())
    private val questionRegex = Regex("(\\d+)\\.(.+)")
    private val answersResult:MutableMap<Int, Boolean?> = mutableMapOf()
    private val userAnswers:MutableMap<Int, UserAnswers> = mutableMapOf()
    fun loadQuestions() {
        val text = ClassLoader.getSystemResource("exam1.md").readText()

        var currentQuestion: Question? = null
        var currentAnswerId = 1
        text.lines().forEach { line ->
            if (line.isBlank().not()) {
                if (questionRegex.matches(line)) {
                    val m = questionRegex.find(line)!!
                    val num = m.groupValues[1].toInt()
                    val questionText = m.groupValues[2]
                    currentQuestion = Question(
                        num, questionText, mutableListOf()
                    )
                    exam.questions.add(currentQuestion!!)
                    currentAnswerId = 1
                    logger.info("Found question = $questionText")
                } else {
                    if (currentQuestion == null) {
                        throw InvalidParameterException("Answer w/o question on $line")
                    } else {
                        val answerText = line
                        val correct = answerText.startsWith("*")
                        val answer = Answer(currentAnswerId, answerText.trim('*'), correct)
                        currentQuestion!!.answers.add(answer)
                        logger.info("Found answer = $answer")
                        currentAnswerId++
                    }
                }
            }
        }
        exam.questionMap.putAll(exam.questions.associateBy {
            it.num
        })
        resetStat()
        logger.info("Loaded exam = $exam")

    }

    fun getQuestion(questionNum: Int): Question {
        return exam.questionMap[questionNum]!!
    }

    fun lastQuestionNum() = exam.questions.maxOf { it.num }
    fun firstQuestionNum() = exam.questions.minOf { it.num }

    fun checkAnswer(questionNum: Int, userAnswer: UserAnswers):Boolean {
        val correctAnswers = getQuestion(questionNum).answers.filter { it.correctAnswer }.map { it.id }.sorted()
        val result = userAnswer.answerIds.sorted() == correctAnswers
        answersResult[questionNum] = result
        userAnswers[questionNum] = userAnswer
        return result
    }

    fun resetStat() {
        answersResult.clear()
        answersResult.putAll(exam.questions.associate { it.num to null })
    }

    fun finalStat():FinalStat {
        val totalQuestions = exam.questions.size
        val answered = answersResult.count { it.value != null }
        val correctAnswers = answersResult.count {it.value == true}
        val finalAnswers = answersResult.map { (questionNum, result) ->
            FinalAnswer(
                questionNum,
                getQuestion(questionNum).questionText,
                answersResult[questionNum] == true,
                answersResult[questionNum] == null,
                answersResult[questionNum] == false,
                getQuestion(questionNum),
                userAnswers[questionNum]
            )
        }
        return FinalStat(
            totalQuestions, answered, correctAnswers, finalAnswers
        )
    }
}
