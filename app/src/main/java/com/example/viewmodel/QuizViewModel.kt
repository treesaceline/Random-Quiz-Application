package com.example.viewmodel

import android.os.Build
import android.text.Html
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.TriviaService
import com.example.data.db.QuizDatabase
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Robust HTML Decoder for Trivia questions and responses.
 */
fun String.htmlDecode(): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(this).toString()
        }
    } catch (e: Exception) {
        this.replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&eacute;", "é")
            .replace("&oacute;", "ó")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace("&deg;", "°")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
    }
}

data class MenuCategory(val id: Int?, val name: String)

sealed interface QuizUiState {
    object Welcome : QuizUiState
    object Loading : QuizUiState
    data class ActiveQuiz(
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val selectedAnswer: String?,
        val isSubmitted: Boolean,
        val timeLeft: Int,
        val isTimeUp: Boolean
    ) : QuizUiState
    data class Results(
        val totalQuestions: Int,
        val score: Int,
        val category: String,
        val difficulty: String,
        val questionBreakdown: List<QuestionReview>
    ) : QuizUiState
    data class Error(val message: String) : QuizUiState
}

data class QuestionReview(
    val questionText: String,
    val selectedAnswer: String?,
    val correctAnswer: String,
    val isCorrect: Boolean
)

class QuizViewModel(private val database: QuizDatabase) : ViewModel() {

    private val triviaService = TriviaService.create()

    // Available categories mapped to OpenTDB IDs
    val categories = listOf(
        MenuCategory(null, "Any Category"),
        MenuCategory(9, "General Knowledge"),
        MenuCategory(17, "Science & Nature"),
        MenuCategory(18, "Computers & Tech"),
        MenuCategory(21, "Sports"),
        MenuCategory(22, "Geography"),
        MenuCategory(23, "History"),
        MenuCategory(24, "Politics"),
        MenuCategory(25, "Art"),
        MenuCategory(27, "Animals")
    )

    val difficulties = listOf("easy", "medium", "hard")

    // Welcome Screen preferences state
    private val _selectedCategory = MutableStateFlow<MenuCategory>(categories[0])
    val selectedCategory: StateFlow<MenuCategory> = _selectedCategory.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<String>("medium")
    val selectedDifficulty: StateFlow<String> = _selectedDifficulty.asStateFlow()

    // Primary UI flow
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Welcome)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    // Database flow for attempts history
    val quizHistory: StateFlow<List<QuizAttempt>> = database.quizAttemptDao()
        .getAllAttempts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var timerJob: Job? = null
    private var quizQuestionsList = listOf<QuizQuestion>()
    private var activeQuestionIndex = 0
    private var correctAnswersCount = 0
    private var chosenAnswersList = mutableListOf<String?>() // Track answers indexed by question index

    fun setCategory(category: MenuCategory) {
        _selectedCategory.value = category
    }

    fun setDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty.lowercase()
    }

    fun selectWelcome() {
        cancelTimer()
        _uiState.value = QuizUiState.Welcome
    }

    fun startQuiz() {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            try {
                val categoryId = _selectedCategory.value.id
                val difficultyLevel = _selectedDifficulty.value

                val response = triviaService.getQuestions(
                    amount = 5,
                    category = categoryId,
                    difficulty = difficultyLevel
                )

                if (response.responseCode == 0 && response.results.isNotEmpty()) {
                    // Turn API response to QuizQuestion list with HTML decode and options shuffled
                    quizQuestionsList = response.results.map { item ->
                        val options = (item.incorrectAnswers + item.correctAnswer)
                            .map { it.htmlDecode() }
                            .shuffled()

                        QuizQuestion(
                            questionText = item.question.htmlDecode(),
                            correctAnswer = item.correctAnswer.htmlDecode(),
                            options = options,
                            categoryName = item.category.htmlDecode(),
                            difficulty = item.difficulty.htmlDecode()
                        )
                    }

                    activeQuestionIndex = 0
                    correctAnswersCount = 0
                    chosenAnswersList = MutableList(quizQuestionsList.size) { null }

                    startQuestion(activeQuestionIndex)
                } else {
                    _uiState.value = QuizUiState.Error("No questions returned from Trivia database. Please try another selection.")
                }
            } catch (e: Exception) {
                _uiState.value = QuizUiState.Error("Failed to fetch questions. Please check your internet connection and try again.")
            }
        }
    }

    private fun startQuestion(index: Int) {
        cancelTimer()
        activeQuestionIndex = index
        val currentQuestion = quizQuestionsList[index]

        _uiState.value = QuizUiState.ActiveQuiz(
            questions = quizQuestionsList,
            currentIndex = index,
            selectedAnswer = null,
            isSubmitted = false,
            timeLeft = 20,
            isTimeUp = false
        )

        startTimer()
    }

    private fun startTimer() {
        cancelTimer()
        timerJob = viewModelScope.launch {
            var seconds = 20
            while (seconds > 0) {
                delay(1000)
                seconds--
                val currentState = _uiState.value
                if (currentState is QuizUiState.ActiveQuiz && !currentState.isSubmitted) {
                    _uiState.value = currentState.copy(timeLeft = seconds)
                } else {
                    break
                }
            }
            // Timer expired
            val currentState = _uiState.value
            if (currentState is QuizUiState.ActiveQuiz && !currentState.isSubmitted) {
                // Auto-submit
                submitAnswer(null)
            }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun selectOption(option: String) {
        val currentState = _uiState.value
        if (currentState is QuizUiState.ActiveQuiz && !currentState.isSubmitted) {
            _uiState.value = currentState.copy(selectedAnswer = option)
        }
    }

    fun submitAnswer(forcedAnswer: String? = "NO_OVERRIDE") {
        cancelTimer()
        val currentState = _uiState.value
        if (currentState is QuizUiState.ActiveQuiz) {
            // Decide what answer to use based on override or timer expiration
            val answer = if (forcedAnswer == "NO_OVERRIDE") currentState.selectedAnswer else forcedAnswer
            
            chosenAnswersList[activeQuestionIndex] = answer
            val currentQuestion = quizQuestionsList[activeQuestionIndex]
            val isCorrect = answer == currentQuestion.correctAnswer
            if (isCorrect) {
                correctAnswersCount++
            }

            _uiState.value = currentState.copy(
                selectedAnswer = answer,
                isSubmitted = true,
                isTimeUp = (answer == null)
            )
        }
    }

    fun nextQuestion() {
        if (activeQuestionIndex + 1 < quizQuestionsList.size) {
            startQuestion(activeQuestionIndex + 1)
        } else {
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        cancelTimer()
        val catName = _selectedCategory.value.name
        val diffName = _selectedDifficulty.value.replaceFirstChar { it.uppercase() }

        // Build question breakdown
        val breakdown = quizQuestionsList.mapIndexed { idx, q ->
            val ans = chosenAnswersList[idx]
            QuestionReview(
                questionText = q.questionText,
                selectedAnswer = ans,
                correctAnswer = q.correctAnswer,
                isCorrect = ans == q.correctAnswer
            )
        }

        val score = correctAnswersCount
        val total = quizQuestionsList.size

        // Save result in Room database
        viewModelScope.launch {
            try {
                database.quizAttemptDao().insertAttempt(
                    QuizAttempt(
                        timestamp = System.currentTimeMillis(),
                        categoryName = catName,
                        difficulty = diffName,
                        score = score,
                        totalQuestions = total
                    )
                )
            } catch (e: Exception) {
                // Ignore db saving errors
            }
        }

        _uiState.value = QuizUiState.Results(
            totalQuestions = total,
            score = score,
            category = catName,
            difficulty = diffName,
            questionBreakdown = breakdown
        )
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                database.quizAttemptDao().clearHistory()
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    companion object {
        fun provideFactory(context: android.content.Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = QuizDatabase.getDatabase(context)
                    return QuizViewModel(db) as T
                }
            }
        }
    }
}
