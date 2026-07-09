package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.VaultViewModel
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.vault.DailyJournalScreen
import com.example.ui.vault.MediaVaultScreen
import com.example.ui.vault.MovieJournalScreen
import com.example.ui.vault.NoteEditorScreen
import com.example.ui.vault.SettingsScreen
import com.example.ui.vault.VaultDashboard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VaultViewModel = viewModel()
            val themeSelection by viewModel.themeSelection.collectAsState()
            val screenshotProtection by viewModel.screenshotProtection.collectAsState()

            // Dynamic Dark Theme resolution
            val darkTheme = when (themeSelection) {
                "LIGHT" -> false
                "DARK", "AMOLED" -> true
                else -> isSystemInDarkTheme()
            }

            // Dynamic Window FLAG_SECURE handling for screenshot/recording protection
            val context = LocalContext.current
            val activity = context as? ComponentActivity
            LaunchedEffect(screenshotProtection) {
                activity?.let {
                    if (screenshotProtection) {
                        it.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        it.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isUnlocked by viewModel.isUnlocked.collectAsState()

                    if (!isUnlocked) {
                        // Secret lock screen disguised as calculator
                        CalculatorScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = {
                                // If PIN is not set, allow visiting secure settings to configure it
                            }
                        )
                    } else {
                        // Decrypted Primary Vault Navigation Flow
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "dashboard") {
                            composable("dashboard") {
                                VaultDashboard(
                                    viewModel = viewModel,
                                    onNavigateToCategory = { type ->
                                        navController.navigate("media_vault/$type")
                                    },
                                    onNavigateToNotes = {
                                        navController.navigate("notes")
                                    },
                                    onNavigateToMovies = {
                                        navController.navigate("movies")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onNavigateToDailyJournal = {
                                        navController.navigate("daily_journal")
                                    }
                                )
                            }

                            composable(
                                route = "media_vault/{type}",
                                arguments = listOf(navArgument("type") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val type = backStackEntry.arguments?.getString("type") ?: "PHOTO"
                                MediaVaultScreen(
                                    viewModel = viewModel,
                                    categoryType = type,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("notes") {
                                NoteEditorScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("movies") {
                                MovieJournalScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("daily_journal") {
                                DailyJournalScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
