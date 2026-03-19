package com.club360fit.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.screens.welcome.WelcomeScreen
import com.club360fit.app.ui.screens.auth.AuthScreen
import com.club360fit.app.ui.screens.auth.ResetPasswordScreen
import com.club360fit.app.ui.screens.client.ClientHomeScreen
import com.club360fit.app.ui.screens.client.MyMealsScreen
import com.club360fit.app.ui.screens.client.MyPaymentsScreen
import com.club360fit.app.ui.screens.client.MyProgressScreen
import com.club360fit.app.ui.screens.client.MyScheduleScreen
import com.club360fit.app.ui.screens.client.MyWorkoutsScreen
import com.club360fit.app.ui.screens.admin.AdminHomeScreen
import com.club360fit.app.ui.screens.admin.ClientDetailScreen
import com.club360fit.app.ui.screens.admin.ClientProfileScreen
import com.club360fit.app.ui.screens.admin.ClientMealsScreen
import com.club360fit.app.ui.screens.admin.ClientPaymentsScreen
import com.club360fit.app.ui.screens.admin.ClientProgressScreen
import com.club360fit.app.ui.screens.admin.ClientScheduleScreen
import com.club360fit.app.ui.screens.admin.ClientWorkoutsScreen
import com.club360fit.app.ui.screens.profile.UserProfileScreen
import com.club360fit.app.ui.screens.gallery.TransformationGalleryScreen
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

/**
 * Root navigation graph. Welcome -> Auth (Sign In / Create Account) -> Client or Admin home.
 */
@Composable
fun Club360FitNavHost(startDestination: String = Routes.WELCOME) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onSignIn = { navController.navigate(Routes.SIGN_IN) },
                onCreateAccount = { navController.navigate(Routes.CREATE_ACCOUNT) }
            )
        }
        composable(Routes.SIGN_IN) {
            AuthScreen(
                isSignIn = true,
                onAuthSuccess = { isAdmin -> navController.navigate(if (isAdmin) Routes.ADMIN_HOME else Routes.CLIENT_HOME) { popUpTo(Routes.WELCOME) { inclusive = true } } },
                onNavigateToCreateAccount = { navController.navigate(Routes.CREATE_ACCOUNT) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CREATE_ACCOUNT) {
            AuthScreen(
                isSignIn = false,
                onAuthSuccess = { isAdmin -> navController.navigate(if (isAdmin) Routes.ADMIN_HOME else Routes.CLIENT_HOME) { popUpTo(Routes.WELCOME) { inclusive = true } } },
                onNavigateToSignIn = { navController.navigate(Routes.SIGN_IN) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                onPasswordResetDone = { isAdmin ->
                    navController.navigate(if (isAdmin) Routes.ADMIN_HOME else Routes.CLIENT_HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.navigate(Routes.SIGN_IN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CLIENT_HOME) {
            ClientHomeScreen(
                onOpenProfile = { navController.navigate(Routes.MY_PROFILE) },
                onOpenGallery = { navController.navigate(Routes.TRANSFORMATION_GALLERY) },
                onOpenWorkouts = { id -> navController.navigate("${Routes.MY_WORKOUTS}/$id") },
                onOpenMeals = { id -> navController.navigate("${Routes.MY_MEALS}/$id") },
                onOpenProgress = { id -> navController.navigate("${Routes.MY_PROGRESS}/$id") },
                onOpenSchedule = { id -> navController.navigate("${Routes.MY_SCHEDULE}/$id") },
                onOpenPayments = { id -> navController.navigate("${Routes.MY_PAYMENTS}/$id") }
            )
        }
        composable("${Routes.MY_WORKOUTS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            MyWorkoutsScreen(clientId = clientId, onBack = { navController.popBackStack() })
        }
        composable("${Routes.MY_MEALS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            MyMealsScreen(clientId = clientId, onBack = { navController.popBackStack() })
        }
        composable("${Routes.MY_PROGRESS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            MyProgressScreen(clientId = clientId, onBack = { navController.popBackStack() })
        }
        composable("${Routes.MY_SCHEDULE}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            MyScheduleScreen(clientId = clientId, onBack = { navController.popBackStack() })
        }
        composable("${Routes.MY_PAYMENTS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            MyPaymentsScreen(clientId = clientId, onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                onOpenProfile = { navController.navigate(Routes.MY_PROFILE) },
                onOpenClientDetails = { clientId ->
                    navController.navigate("${Routes.CLIENT_DETAIL}/$clientId")
                },
                onOpenClientProfile = { clientId ->
                    navController.navigate("${Routes.CLIENT_PROFILE}/${clientId ?: "new"}")
                },
                onOpenGallery = { navController.navigate(Routes.TRANSFORMATION_GALLERY) }
            )
        }
        composable("${Routes.CLIENT_DETAIL}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            ClientDetailScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("${Routes.CLIENT_PROFILE}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")
            ClientProfileScreen(
                clientId = clientId.takeUnless { it == "new" },
                onBack = { navController.popBackStack() },
                onOpenWorkouts = { id -> navController.navigate("${Routes.CLIENT_WORKOUTS}/$id") },
                onOpenMeals = { id -> navController.navigate("${Routes.CLIENT_MEALS}/$id") },
                onOpenProgress = { id -> navController.navigate("${Routes.CLIENT_PROGRESS}/$id") },
                onOpenSchedule = { id -> navController.navigate("${Routes.CLIENT_SCHEDULE}/$id") },
                onOpenPayments = { id -> navController.navigate("${Routes.CLIENT_PAYMENTS}/$id") }
            )
        }
        composable("${Routes.CLIENT_WORKOUTS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            ClientWorkoutsScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("${Routes.CLIENT_MEALS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            ClientMealsScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("${Routes.CLIENT_PROGRESS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            ClientProgressScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("${Routes.CLIENT_SCHEDULE}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            ClientScheduleScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("${Routes.CLIENT_PAYMENTS}/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            ClientPaymentsScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.MY_PROFILE) {
            UserProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { /* TODO: navigate to edit profile screen */ navController.popBackStack() },
                onSignOut = {
                    scope.launch {
                        SupabaseClient.client.auth.signOut()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.TRANSFORMATION_GALLERY) {
            TransformationGalleryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
