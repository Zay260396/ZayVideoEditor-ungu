package com.zay.videoeditor

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zay.videoeditor.data.ProjectStore
import com.zay.videoeditor.ui.screens.MainScreen
import com.zay.videoeditor.ui.screens.OnboardingScreen
import com.zay.videoeditor.ui.screens.SplashScreen
import com.zay.videoeditor.ui.screens.editor.EditorScreen
import com.zay.videoeditor.ui.screens.ExportScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val EDITOR = "editor"
    const val EXPORT = "export"
}

@Composable
fun ZayApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zay_prefs", Context.MODE_PRIVATE) }

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                val next = if (prefs.getBoolean("onboarded", false)) Routes.MAIN else Routes.ONBOARDING
                nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                prefs.edit().putBoolean("onboarded", true).apply()
                nav.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) {
            MainScreen(
                onNewProject = {
                    ProjectStore.openNew()
                    nav.navigate(Routes.EDITOR)
                },
                onOpenProject = { id ->
                    if (ProjectStore.open(context, id)) {
                        nav.navigate(Routes.EDITOR)
                    } else {
                        Toast.makeText(context, "Proyek tidak bisa dibuka", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
        composable(Routes.EDITOR) {
            EditorScreen(
                onBack = { nav.popBackStack() },
                onExport = { nav.navigate(Routes.EXPORT) },
            )
        }
        composable(Routes.EXPORT) {
            ExportScreen(onBack = { nav.popBackStack() })
        }
    }
}
