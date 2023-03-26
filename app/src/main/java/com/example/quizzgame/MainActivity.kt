package com.example.quizzgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizzgame.ui.theme.QuizzGameTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


sealed class Screen (val route: String){
    object Game: Screen("game")
    object Greet: Screen("greet")
    object Result: Screen("result")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizzGameTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = Screen.Greet.route) {
                        composable(Screen.Greet.route) {
                            GreetingScreen(navController = navController)
                        }
                        composable(Screen.Game.route) {
                            GameScreen(navController = navController)
                        }
                        composable(Screen.Result.route+"/{score}",arguments = listOf(navArgument("score"){
                            type = NavType.IntType
                        })){
                            val score = requireNotNull(it.arguments).getInt("score")
                            if( score != null)
                                ResultScreen(score, navController = navController)
                        }
                    }
                }
            }
        }
    }

    data class statusData(
        val currentQuestion: List<String> = listOf(),
        val currentCount: Int = 1,
        val score: Int = 0,
        val isWrong: Boolean = false,
        val isOver: Boolean = false
    )

    class GameView : ViewModel() {
        val state = MutableStateFlow(statusData())
        val State: StateFlow<statusData> = state.asStateFlow()
        var userAnswer by mutableStateOf("")
        var usedQuestion: MutableSet<List<String>> = mutableSetOf()
        lateinit var currentQuiz: List<String>

        fun shuffleQuestion(question: List<String>): List<String> {
            val quiz = question
            val quizQuestion = question[0].toCharArray()
            quizQuestion.shuffle()
            while (String(quizQuestion).equals(question)) {
                quizQuestion.shuffle()
            }
            return quiz
        }

        fun randomQuestion(): List<String> {
            currentQuiz = questions.random()
            if (usedQuestion.contains(currentQuiz)) {
                return randomQuestion()
            } else {
                usedQuestion.add(currentQuiz)
                return shuffleQuestion(currentQuiz)
            }
        }

        init {
            resetGame()
        }

        fun resetGame() {
            usedQuestion.clear()
            state.value = statusData(currentQuestion = randomQuestion())
        }

        fun getAnswer(answer: String) {
            userAnswer = answer
        }

        fun nextQuestion() {
            updateGameState(state.value.score)
            getAnswer("")
        }

        fun checkUserAnswer() {
            if (userAnswer == currentQuiz[1]) {
                val updatedScore = state.value.score.plus(score)
                updateGameState(updatedScore)
            } else {
                // User's guess is wrong, show an error
                state.update { currentState ->
                    currentState.copy(isWrong = true)

                }
                nextQuestion()
            }
            // Reset user guess
            getAnswer("")
        }

        fun updateGameState(updatedScore: Int) {
            if (usedQuestion.size == total_question) {
                //Last round in the game, update isGameOver to true, don't pick a new word
                state.update { currentState ->
                    currentState.copy(
                        isWrong = false,
                        score = updatedScore,
                        isOver = true
                    )
                }
            } else {
                // Normal round in the game
                state.update { currentState ->
                    currentState.copy(
                        isWrong = false,
                        currentQuestion = randomQuestion(),
                        currentCount = currentState.currentCount.inc(),
                        score = updatedScore
                    )
                }
            }
        }
    }

    @Composable
    fun GameScreen(
        navController: NavController,
        modifier: Modifier = Modifier,
        gameView: GameView = viewModel()
    ) {
        val gameState by gameView.State.collectAsState()
        val currentQuestion = gameState.currentQuestion
        val quizCount = gameState.currentCount
        val score = gameState.score
        val randomQuestion = remember(gameView.State.value.currentQuestion) {
            gameView.State.value.currentQuestion.slice(1..4).shuffled()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .background(color = Color.LightGray)
                    .fillMaxSize()
                    .padding(all = 12.dp),

                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentWidth(Alignment.End),
                    text = stringResource(R.string.score, score),
                    fontSize = 18.sp,
                )
                Text(
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    text = stringResource(R.string.quiz_count, quizCount),
                    fontSize = 18.sp,
                    maxLines = 1,
                )
            }
            Text(
                text = (currentQuestion[0]),
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(start = 8.dp, top = 25.dp),
            )
            Button(
                shape = RoundedCornerShape(23.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 15.dp),

                onClick = { gameView.getAnswer(randomQuestion[0]);gameView.checkUserAnswer() }
            ) {
                Text(
                    text = randomQuestion[0],
                    fontSize = 20.sp,
                )
            }
            Button(
                shape = RoundedCornerShape(23.dp),
                modifier = modifier
                    .fillMaxWidth()

                    .padding(start = 8.dp, top = 12.dp),
                onClick = { gameView.getAnswer(randomQuestion[1]);gameView.checkUserAnswer() }
            ) {
                Text(
                    text = randomQuestion[1],
                    fontSize = 20.sp,
                )
            }
            Button(
                shape = RoundedCornerShape(23.dp),
                modifier = modifier
                    .fillMaxWidth()

                    .padding(start = 8.dp, top = 12.dp),
                onClick = { gameView.getAnswer(randomQuestion[2]);gameView.checkUserAnswer() }
            ) {
                Text(
                    text = randomQuestion[2],
                    fontSize = 20.sp,
                )
            }
            Button(
                shape = RoundedCornerShape(23.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 12.dp),

                onClick = { gameView.getAnswer(randomQuestion[3]);gameView.checkUserAnswer() }
            ) {
                Text(
                    text = randomQuestion[3],
                    fontSize = 20.sp,
                )
            }
        if (gameState.isOver) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Result.route + "/$score")
            }
        }
        }
    }

    @Composable
    fun GreetingScreen(navController: NavController, modifier: Modifier = Modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Text(
                text = "Quizz Game",
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(all = 25.dp),
            )

            Button(
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                onClick = { navController.navigate(Screen.Game.route) }
            ) {
                Text(
                    text = "Let's GO!",
                    fontSize = 20.sp,
                )
            }
        }
    }

    @Composable
    fun ResultScreen(score: Int, navController: NavController, modifier: Modifier = Modifier)
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Text(
                text = stringResource(R.string.result),
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(all = 25.dp),
            )

            Text(
                text = stringResource(R.string.you_scored, score),
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(all = 25.dp),
            )


            Button(
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                onClick = { navController.navigate(Screen.Game.route) }
            ) {
                Text(
                    text = "Play Again",
                    fontSize = 20.sp,
                )
            }

            Button(
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                onClick = { navController.navigate(Screen.Greet.route) }
            ) {
                Text(
                    text = "Quit",
                    fontSize = 20.sp,
                )
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        QuizzGameTheme {
            val navController = rememberNavController()
            GreetingScreen(navController, modifier = Modifier)
        }
    }
}