package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.viewmodel.QuestionReview
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel
import java.text.SimpleDateFormat
import java.util.*

// Vivid neon colors matching custom game-style dark mode
val DarkBg = Color(0xFF0F111A)
val SurfaceCard = Color(0xFF1B1E30)
val NeonCyan = Color(0xFF00F5D4)
val NeonPurple = Color(0xFF9B5DE5)
val NeonRose = Color(0xFFF15BB5)
val CorrectGreen = Color(0xFF2ECC71)
val ErrorRed = Color(0xFFE74C3C)
val TextSecondary = Color(0xFF90A4AE)

@Composable
fun QuizAppMain(
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val history by viewModel.quizHistory.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBg
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.98f, animationSpec = tween(150)))
            },
            label = "ScreenTransition"
        ) { state ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter
            ) {
                // Adaptive layout: Constrain maximum width on wide displays (tablet scaling)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (state) {
                        is QuizUiState.Welcome -> {
                            WelcomeScreen(
                                categories = viewModel.categories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.setCategory(it) },
                                difficulties = viewModel.difficulties,
                                selectedDifficulty = selectedDifficulty,
                                onDifficultySelected = { viewModel.setDifficulty(it) },
                                onStartQuiz = { viewModel.startQuiz() },
                                history = history,
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }
                        is QuizUiState.Loading -> {
                            LoadingScreen()
                        }
                        is QuizUiState.ActiveQuiz -> {
                            ActiveQuizScreen(
                                state = state,
                                onOptionSelected = { viewModel.selectOption(it) },
                                onSubmitAnswer = { viewModel.submitAnswer() },
                                onNextQuestion = { viewModel.nextQuestion() }
                            )
                        }
                        is QuizUiState.Results -> {
                            ResultsScreen(
                                state = state,
                                onRestart = { viewModel.selectWelcome() }
                            )
                        }
                        is QuizUiState.Error -> {
                            ErrorScreen(
                                message = state.message,
                                onBack = { viewModel.selectWelcome() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 1: Welcome/Start screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    categories: List<com.example.viewmodel.MenuCategory>,
    selectedCategory: com.example.viewmodel.MenuCategory,
    onCategorySelected: (com.example.viewmodel.MenuCategory) -> Unit,
    difficulties: List<String>,
    selectedDifficulty: String,
    onDifficultySelected: (String) -> Unit,
    onStartQuiz: () -> Unit,
    history: List<QuizAttempt>,
    onClearHistory: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Visual Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonPurple, NeonCyan)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Quiz Launch",
                        tint = DarkBg,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "STUDENT TRIVIA CHALLENGE",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = DarkBg,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "5 questions to test your knowledge",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkBg.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Configuration Section Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Quiz Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                    )

                    // Category Selection Exposed Dropdown-like
                    Column {
                        Text(
                            text = "Select Category",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { dropdownExpanded = true },
                                color = DarkBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("category_dropdown_trigger")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedCategory.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Category dropdown",
                                        tint = NeonCyan
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(SurfaceCard)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = cat.name,
                                                color = if (cat == selectedCategory) NeonCyan else Color.White,
                                                fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onCategorySelected(cat)
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("category_item_${cat.id ?: "any"}")
                                    )
                                }
                            }
                        }
                    }

                    // Difficulty Selection Chips
                    Column {
                        Text(
                            text = "Difficulty Level",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            difficulties.forEach { diff ->
                                val isSelected = diff.lowercase() == selectedDifficulty.lowercase()
                                val chipBorder = if (isSelected) BorderStroke(1.5.dp, NeonCyan) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                                val chipBg = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkBg

                                Surface(
                                    onClick = { onDifficultySelected(diff) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("difficulty_chip_${diff}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = chipBorder,
                                    color = chipBg
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = diff.lowercase().replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) NeonCyan else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Huge Vibrant Start button
                    Button(
                        onClick = onStartQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_quiz_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "LET'S PLAY!",
                                color = DarkBg,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = DarkBg
                            )
                        }
                    }
                }
            }
        }

        // History / Stats Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attempt History",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp,
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // History items
        if (history.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No history logged yet.",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Complete your first quiz to track your progress!",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(history) { attempt ->
                HistoryCard(attempt = attempt)
            }
        }
    }
}

@Composable
fun HistoryCard(attempt: QuizAttempt) {
    val dateStr = remember(attempt.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        sdf.format(Date(attempt.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.categoryName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonPurple.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = attempt.difficulty,
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = dateStr,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Score Circle Accent
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (attempt.score >= 4) CorrectGreen.copy(alpha = 0.15f)
                        else if (attempt.score >= 2) NeonPurple.copy(alpha = 0.15f)
                        else ErrorRed.copy(alpha = 0.15f)
                    )
            ) {
                Text(
                    text = "${attempt.score}/${attempt.totalQuestions}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (attempt.score >= 4) CorrectGreen
                    else if (attempt.score >= 2) NeonPurple
                    else ErrorRed
                )
            }
        }
    }
}

/**
 * SCREEN 2: Loading State
 */
@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loading_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = NeonCyan,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Retrieving Trivia Challenges...",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Preparing questions & answers",
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

/**
 * SCREEN 3: Active Quiz Display
 */
@Composable
fun ActiveQuizScreen(
    state: QuizUiState.ActiveQuiz,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit
) {
    val currentQuestion = state.questions[state.currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("active_quiz_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER: Progress Indicator
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentQuestion.categoryName.uppercase(),
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Q: ${state.currentIndex + 1} of ${state.questions.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Custom Linear Progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = (state.currentIndex + 1).toFloat() / state.questions.size,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "Progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = SurfaceCard
            )
        }

        // TIMER & QUESTION CARD ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Countdown Timer Indicator
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                val progressFraction = state.timeLeft.toFloat() / 20f
                val progressColor = when {
                    state.timeLeft > 10 -> NeonCyan
                    state.timeLeft > 5 -> NeonPurple
                    else -> NeonRose
                }

                // Smoothly animated progress stroke
                val sweepAngle by animateFloatAsState(
                    targetValue = progressFraction * 360f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                    label = "TimerSweep"
                )

                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = SurfaceCard, style = Stroke(width = 4.dp.toPx()))
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = state.timeLeft.toString(),
                    color = progressColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            // Difficulty Label Capsule
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonPurple.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Difficulty: " + currentQuestion.difficulty.replaceFirstChar { it.uppercase() },
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // THE QUESTION CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = currentQuestion.questionText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 24.sp
                )
            }
        }

        // OPTION BUTTONS LIST
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            currentQuestion.options.forEachIndexed { optIndex, optionText ->
                val isSelected = state.selectedAnswer == optionText
                val optionTag = "option_${optIndex}"

                // Determine option border tint and backgrounds under different validation states
                val borderStroke: BorderStroke
                val containerColor: Color
                val textColor: Color
                var trailingIcon: (@Composable () -> Unit)? = null

                if (state.isSubmitted) {
                    val isCorrectValue = optionText == currentQuestion.correctAnswer
                    val isUserChosenValue = state.selectedAnswer == optionText

                    when {
                        isCorrectValue -> {
                            borderStroke = BorderStroke(1.5.dp, CorrectGreen)
                            containerColor = CorrectGreen.copy(alpha = 0.12f)
                            textColor = CorrectGreen
                            trailingIcon = { Icon(Icons.Default.Check, contentDescription = "Correct Choice", tint = CorrectGreen) }
                        }
                        isUserChosenValue -> {
                            borderStroke = BorderStroke(1.5.dp, ErrorRed)
                            containerColor = ErrorRed.copy(alpha = 0.12f)
                            textColor = ErrorRed
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Wrong Choice", tint = ErrorRed) }
                        }
                        else -> {
                            // Dim all non-matching options to highlight results
                            borderStroke = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                            containerColor = SurfaceCard.copy(alpha = 0.4f)
                            textColor = Color.White.copy(alpha = 0.35f)
                        }
                    }
                } else {
                    if (isSelected) {
                        borderStroke = BorderStroke(1.5.dp, NeonCyan)
                        containerColor = NeonCyan.copy(alpha = 0.12f)
                        textColor = NeonCyan
                    } else {
                        borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        containerColor = SurfaceCard
                        textColor = Color.White
                    }
                }

                Surface(
                    onClick = {
                        if (!state.isSubmitted) {
                            onOptionSelected(optionText)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = borderStroke,
                    color = containerColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(optionTag)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = optionText,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected || (state.isSubmitted && optionText == currentQuestion.correctAnswer)) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (trailingIcon != null) {
                            trailingIcon()
                        } else if (isSelected && !state.isSubmitted) {
                            // Small indicator dot when selected but not validated
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeonCyan, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // FEEDBACK MESSAGE (Time up alerts, etc.)
        if (state.isSubmitted) {
            val wasCorrect = state.selectedAnswer == currentQuestion.correctAnswer
            val feedbackText = when {
                state.isTimeUp -> "TIME'S UP! The correct answer was: ${currentQuestion.correctAnswer}"
                wasCorrect -> "EXCELLENT! That's correct!"
                else -> "INCORRECT. The right answer was: ${currentQuestion.correctAnswer}"
            }
            val feedbackColor = if (wasCorrect) CorrectGreen else ErrorRed

            Text(
                text = feedbackText,
                color = feedbackColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("quiz_feedback_msg")
            )
        }

        // BOTTOM ACTION BUTTON
        val actionText = if (state.isSubmitted) {
            if (state.currentIndex + 1 >= state.questions.size) "SEE QUIZ RESULTS" else "NEXT QUESTION"
        } else {
            "SUBMIT CHOICE"
        }
        val actionBtnTag = if (state.isSubmitted) "quiz_next_button" else "quiz_submit_button"
        val isEnabled = state.selectedAnswer != null || state.isSubmitted

        Button(
            onClick = {
                if (state.isSubmitted) {
                    onNextQuestion()
                } else {
                    onSubmitAnswer()
                }
            },
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isSubmitted) NeonPurple else NeonRose,
                disabledContainerColor = SurfaceCard.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(actionBtnTag)
        ) {
            Text(
                text = actionText,
                color = if (isEnabled) Color.White else Color.Gray,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * SCREEN 4: Results and Summary Section
 */
@Composable
fun ResultsScreen(
    state: QuizUiState.Results,
    onRestart: () -> Unit
) {
    val pct = (state.score.toFloat() / state.totalQuestions.toFloat() * 100f).toInt()
    val (congratsMsg, congratsColor) = when {
        state.score == 5 -> "PERFECT SCORE! QUIZ MASTER! 🏆" to NeonCyan
        state.score >= 4 -> "FABULOUS JOB! SO CLOSE! 🌟" to CorrectGreen
        state.score >= 2 -> "GOOD TRIAL! KEEP PRACTICING! 👍" to NeonPurple
        else -> "STUDY UP AND TRY AGAIN! 📚" to NeonRose
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("results_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // High Score Showcase Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUIZ CHALLENGE COMPLETE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Circular animated score donut
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = DarkBg, style = Stroke(width = 8.dp.toPx()))
                            drawArc(
                                color = congratsColor,
                                startAngle = -90f,
                                sweepAngle = (state.score.toFloat() / state.totalQuestions.toFloat()) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.score}/${state.totalQuestions}",
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = congratsColor
                            )
                            Text(
                                text = "$pct%",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = congratsMsg,
                        color = congratsColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Topic stats capsule
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = state.category, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = state.difficulty, fontSize = 11.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Question Breakdown review section header
        item {
            Text(
                text = "Question Review Breakdown",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Render each question's review details with helpful indicators
        items(state.questionBreakdown) { review ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = review.questionText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (review.isCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (review.isCorrect) "Correct" else "Incorrect",
                            tint = if (review.isCorrect) CorrectGreen else ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkBg)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Correct: ${review.correctAnswer}",
                            fontSize = 11.sp,
                            color = CorrectGreen,
                            fontWeight = FontWeight.Bold
                        )
                        if (!review.isCorrect) {
                            Text(
                                text = "Your Ans: " + (review.selectedAnswer ?: "Time Expired"),
                                fontSize = 11.sp,
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Return Home trigger
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("play_again_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DarkBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRY ANOTHER QUIZ",
                        color = DarkBg,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * SCREEN 5: Error / Alert screen
 */
@Composable
fun ErrorScreen(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("error_screen")
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error Occurred",
            tint = ErrorRed,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "OPPS! NETWORK ERROR",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("error_back_button")
        ) {
            Text("GO BACK HOME", color = DarkBg, fontWeight = FontWeight.Bold)
        }
    }
}
