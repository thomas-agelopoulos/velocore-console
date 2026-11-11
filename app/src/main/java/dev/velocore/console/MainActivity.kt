package dev.velocore.console

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.velocore.console.bike.BikeSource
import dev.velocore.console.bike.SimulatedBike
import dev.velocore.console.data.RideRecord
import dev.velocore.console.data.RideStore
import dev.velocore.console.data.Workout
import dev.velocore.console.data.Workouts
import dev.velocore.console.ride.RideSummary
import dev.velocore.console.ui.DesignCanvas
import dev.velocore.console.ui.GlassChip
import dev.velocore.console.ui.HomeScreen
import dev.velocore.console.ui.RideScreen
import dev.velocore.console.ui.SummaryScreen
import dev.velocore.console.ui.WorkoutsScreen
import kotlinx.coroutines.delay
import java.io.File

sealed interface Screen {
    data object Home : Screen
    data class Browse(val selectedId: String) : Screen
    data class Ride(val workout: Workout?, val startedAt: Long = System.currentTimeMillis()) : Screen
    data class Summary(val summary: RideSummary, val previous: RideRecord?, val saved: Boolean) : Screen
}

class MainActivity : ComponentActivity() {

    private val bike: BikeSource = SimulatedBike()
    private lateinit var store: RideStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = RideStore(File(filesDir, "rides.json"))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        bike.start(lifecycleScope)

        setContent {
            DesignCanvas {
                ConsoleApp(bike, store)
            }
        }
    }

    override fun onDestroy() {
        bike.stop()
        super.onDestroy()
    }
}

@Composable
private fun ConsoleApp(bike: BikeSource, store: RideStore) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var rides by remember { mutableStateOf(store.load()) }
    var message by remember { mutableStateOf<String?>(null) }
    var lastMessage by remember { mutableStateOf("") }
    val sample by bike.samples.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(message) {
        message?.let { lastMessage = it }
        if (message != null) {
            delay(2_600)
            message = null
        }
    }

    fun finishRide(summary: RideSummary) {
        if (summary.seconds < 60) {
            screen = Screen.Home
            return
        }
        val previous = rides.lastOrNull { it.workoutId == summary.workoutId }
        val saved = runCatching { store.add(summary) }.isSuccess
        rides = store.load()
        screen = Screen.Summary(summary, previous, saved)
    }

    BackHandler(enabled = screen is Screen.Browse || screen is Screen.Summary) {
        screen = Screen.Home
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = screen, animationSpec = tween(220), label = "screen") { s ->
            when (s) {
                Screen.Home -> HomeScreen(
                    now = now,
                    rides = rides,
                    heartConnected = sample.heartRate != null,
                    onFreeRide = { screen = Screen.Ride(null) },
                    onBrowse = { screen = Screen.Browse(it) },
                    onStart = { screen = Screen.Ride(it) },
                    onNotYet = { message = it },
                )
                is Screen.Browse -> WorkoutsScreen(
                    initialSelection = s.selectedId,
                    onBack = { screen = Screen.Home },
                    onStart = { screen = Screen.Ride(it) },
                    onNotYet = { message = it },
                )
                is Screen.Ride -> RideScreen(
                    workout = s.workout,
                    bike = bike,
                    onFinish = { finishRide(it) },
                )
                is Screen.Summary -> SummaryScreen(
                    summary = s.summary,
                    workout = Workouts.byId(s.summary.workoutId),
                    previous = s.previous,
                    saved = s.saved,
                    onDone = { screen = Screen.Home },
                    onRideAgain = { screen = Screen.Browse(s.summary.workoutId ?: Workouts.hill.id) },
                    onNotYet = { message = it },
                )
            }
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
        ) {
            GlassChip(lastMessage, height = 64, fontSize = 22)
        }
    }
}
