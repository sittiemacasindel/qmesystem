package com.qme.mobile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qme.mobile.data.api.ApiService
import com.qme.mobile.data.repository.AuthRepository
import com.qme.mobile.data.repository.QueueRepository
import com.qme.mobile.data.repository.UserRepository
import com.qme.mobile.ui.auth.LoginScreen
import com.qme.mobile.ui.auth.LoginViewModel
import com.qme.mobile.ui.auth.RegisterScreen
import com.qme.mobile.ui.auth.RegisterViewModel
import com.qme.mobile.ui.history.HistoryScreen
import com.qme.mobile.ui.history.HistoryViewModel
import com.qme.mobile.ui.home.HomeScreen
import com.qme.mobile.ui.home.HomeViewModel
import com.qme.mobile.ui.profile.ProfileScreen
import com.qme.mobile.ui.profile.ProfileViewModel
import com.qme.mobile.ui.queue.JoinQueueScreen
import com.qme.mobile.ui.queue.JoinQueueViewModel
import com.qme.mobile.ui.queue.QueueDetailScreen
import com.qme.mobile.ui.queue.QueueDetailViewModel
import com.qme.mobile.util.SessionManager
import com.qme.mobile.ui.theme.QmeBlue

// ── Route constants ───────────────────────────────────────────────────────────
object Routes {
    const val LOGIN         = "login"
    const val REGISTER      = "register"
    const val MAIN          = "main"
    const val HOME          = "home"
    const val JOIN_QUEUE    = "join_queue"
    const val QUEUE_DETAIL  = "queue_detail"
    const val HISTORY       = "history"
    const val PROFILE       = "profile"
}

// ── Bottom nav items ──────────────────────────────────────────────────────────
private data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.JOIN_QUEUE, "Join Queue", Icons.Default.AddCircleOutline),
    BottomNavItem(Routes.HOME,       "My Queue",   Icons.Default.QueuePlayNext),
    BottomNavItem(Routes.HISTORY,    "History",    Icons.Default.History),
    BottomNavItem(Routes.PROFILE,    "Profile",    Icons.Default.Person)
)

// ── Root navigation ───────────────────────────────────────────────────────────
@Composable
fun AppNavigation(api: ApiService, session: SessionManager) {
    val rootNav     = rememberNavController()
    val authRepo    = AuthRepository(api, session)
    val queueRepo   = QueueRepository(api)
    val userRepo    = UserRepository(api, session)
    val startRoute  = if (session.isLoggedIn()) Routes.MAIN else Routes.LOGIN

    NavHost(navController = rootNav, startDestination = startRoute) {

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel           = LoginViewModel(authRepo),
                onLoginSuccess      = {
                    rootNav.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { rootNav.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel           = RegisterViewModel(authRepo),
                onRegisterSuccess   = {
                    rootNav.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin   = { rootNav.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                queueRepo = queueRepo,
                userRepo  = userRepo,
                session   = session,
                onLogout  = {
                    authRepo.logout()
                    rootNav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }
    }
}

// ── Main screen with bottom navigation ───────────────────────────────────────
@Composable
private fun MainScreen(
    queueRepo: QueueRepository,
    userRepo: UserRepository,
    session: SessionManager,
    onLogout: () -> Unit
) {
    val mainNav = rememberNavController()
    val navBackEntry by mainNav.currentBackStackEntryAsState()
    val currentDest  = navBackEntry?.destination

    // Hide bottom bar on queue-detail screen
    val showBottomBar = currentDest?.route != Routes.QUEUE_DETAIL

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDest?.hierarchy?.any { it.route == item.route } == true,
                            onClick  = {
                                mainNav.navigate(item.route) {
                                    popUpTo(mainNav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = mainNav,
            startDestination = Routes.HOME,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel        = HomeViewModel(queueRepo),
                    userName         = session.getName() ?: "User",
                    onJoinQueue      = { mainNav.navigate(Routes.JOIN_QUEUE) },
                    onViewQueueDetail = { mainNav.navigate(Routes.QUEUE_DETAIL) }
                )
            }

            composable(Routes.JOIN_QUEUE) {
                JoinQueueScreen(
                    viewModel    = JoinQueueViewModel(queueRepo),
                    onJoinSuccess = {
                        mainNav.navigate(Routes.QUEUE_DETAIL) {
                            popUpTo(Routes.JOIN_QUEUE) { inclusive = false }
                        }
                    }
                )
            }

            composable(Routes.QUEUE_DETAIL) {
                QueueDetailScreen(
                    viewModel  = QueueDetailViewModel(queueRepo),
                    onBack     = { mainNav.popBackStack() },
                    onCancelled = {
                        mainNav.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(viewModel = HistoryViewModel(queueRepo))
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = ProfileViewModel(userRepo),
                    onLogout  = onLogout
                )
            }
        }
    }
}
