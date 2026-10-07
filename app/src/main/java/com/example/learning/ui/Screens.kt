package com.example.learning.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.learning.LearningApplication
import com.example.learning.data.remote.ApiScenario
import com.example.learning.domain.Course

private fun <T : ViewModel> factory(create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}

@Composable
fun LearningApp(app: LearningApplication) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            val vm: LoginViewModel = viewModel(factory = factory { LoginViewModel(app.auth) })
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.authenticated) {
                if (state.authenticated) {
                    nav.navigate("dashboard") { popUpTo("login") { inclusive = true }; launchSingleTop = true }
                }
            }
            LoginScreen(state, vm::emailChanged, vm::passwordChanged, vm::login)
        }
        composable("dashboard") {
            val vm: DashboardViewModel = viewModel(factory = factory { DashboardViewModel(app.courses) })
            val state by vm.state.collectAsStateWithLifecycle()
            DashboardScreen(state, vm::refresh, vm::setScenario) { nav.navigate("course/$it") }
        }
        composable("course/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = requireNotNull(entry.arguments).getLong("id")
            val vm: DetailsViewModel = viewModel(factory = factory { DetailsViewModel(app.courses, id) })
            val state by vm.state.collectAsStateWithLifecycle()
            DetailsScreen(state, { nav.popBackStack() }, vm::completeLesson)
        }
    }
}

@Composable
private fun LoginScreen(state: LoginState, email: (String) -> Unit, password: (String) -> Unit, login: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("LEARNING DASHBOARD", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Welcome back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("A little progress, every day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(state.email, email, Modifier.fillMaxWidth(), enabled = !state.loading,
                label = { Text("Email") }, singleLine = true, isError = state.emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                supportingText = { state.emailError?.let { Text(it) } })
            OutlinedTextField(state.password, password, Modifier.fillMaxWidth(), enabled = !state.loading,
                label = { Text("Password") }, singleLine = true, isError = state.passwordError != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = { state.passwordError?.let { Text(it) } })
            state.error?.let { Notice(it, error = true) }
            Button(login, Modifier.fillMaxWidth().height(52.dp), enabled = !state.loading) {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Log in")
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Try the demo", fontWeight = FontWeight.SemiBold)
                    Text("demo@example.com\npassword123")
                    Text("Mock authentication. Course completion is saved on this device.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(state: DashboardState, refresh: () -> Unit, scenario: (ApiScenario) -> Unit, open: (Long) -> Unit) {
    var showDemoOptions by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("My learning", fontWeight = FontWeight.Bold) }, actions = {
            TextButton(refresh, enabled = !state.refreshing) { Text("Refresh") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Keep moving forward", style = MaterialTheme.typography.headlineSmall)
                Text("Your courses and progress, in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.error != null) item {
                Notice(if (state.courses.isEmpty()) state.error else "${state.error}\nShowing saved courses.", true)
            }
            if (state.emptyResponse && state.courses.isNotEmpty()) item { Notice("No new courses returned. Showing saved courses.") }
            if (state.courses.isEmpty()) item {
                when {
                    state.loading || state.refreshing -> StatusCard("Loading your courses…", "This will only take a moment.")
                    state.error != null -> StatusCard("Couldn't load courses", "Connect to the internet and tap Refresh to try again.")
                    else -> StatusCard("No courses yet", "Your courses will appear here when available.")
                }
            }
            items(state.courses, key = { it.id }) { course -> CourseCard(course) { open(course.id) } }
            item {
                TextButton({ showDemoOptions = !showDemoOptions }) { Text(if (showDemoOptions) "Hide demo API controls" else "Demo API controls") }
                if (showDemoOptions) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose a response, then tap Refresh. Empty responses retain existing saved courses.", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ApiScenario.entries.forEach { mode ->
                                FilterChip(selected = state.scenario == mode, onClick = { scenario(mode) },
                                    enabled = !state.refreshing, label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) })
                            }
                        }
                        Text("To see an empty screen, choose Empty before the first successful load. Offline behavior uses the device network state.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, open: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(course.instructor, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Progress(course)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${course.lessons.size} lessons", style = MaterialTheme.typography.bodyMedium)
                Button(open) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun Progress(course: Course) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${course.completedLessons} of ${course.lessons.size} completed", style = MaterialTheme.typography.bodySmall)
        Text("${course.progress}%", fontWeight = FontWeight.SemiBold)
    }
    LinearProgressIndicator(progress = { course.progress / 100f }, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsScreen(state: DetailsState, back: () -> Unit, complete: (Long) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Course details") }, navigationIcon = { TextButton(back) { Text("Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { message -> item { Notice(message, true) } }
            val course = state.course
            if (course == null) item {
                StatusCard(if (state.loading) "Loading course…" else "Course unavailable", "Go back to your courses to try again.")
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(course.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(course.instructor, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Progress(course)
                        Text("Lessons", style = MaterialTheme.typography.titleLarge)
                    }
                }
                items(course.lessons, key = { it.id }) { lesson ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${lesson.position + 1}. ${lesson.title}", style = MaterialTheme.typography.titleMedium)
                            if (lesson.completed) Text("✓ Completed", color = MaterialTheme.colorScheme.primary)
                            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("○ Pending", style = MaterialTheme.typography.bodySmall)
                                TextButton({ complete(lesson.id) }, enabled = lesson.id !in state.saving) {
                                    Text(if (lesson.id in state.saving) "Saving…" else "Mark completed")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(message: String, error: Boolean = false) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor =
        if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
        Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
