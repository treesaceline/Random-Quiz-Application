package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Room Entity representing a completed quiz game.
 */
@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val categoryName: String,
    val difficulty: String,
    val score: Int,
    val totalQuestions: Int = 5
)

/**
 * OpenTDB API response models.
 */
@JsonClass(generateAdapter = true)
data class TriviaResponse(
    @Json(name = "response_code") val responseCode: Int,
    val results: List<TriviaResult>
)

@JsonClass(generateAdapter = true)
data class TriviaResult(
    val category: String,
    val type: String,
    val difficulty: String,
    val question: String,
    @Json(name = "correct_answer") val correctAnswer: String,
    @Json(name = "incorrect_answers") val incorrectAnswers: List<String>
)

/**
 * UI-level data structure representing a parsed and HTML-decoded question.
 */
data class QuizQuestion(
    val questionText: String,
    val correctAnswer: String,
    val options: List<String>,
    val categoryName: String,
    val difficulty: String
)
