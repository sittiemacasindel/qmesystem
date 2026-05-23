package com.qme.mobile.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*

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

import com.qme.mobile.ui.splash.SplashScreen
import com.qme.mobile.util.SessionManager


object Routes {

    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val MAIN = "main"

    const val HOME = "home"
    const val JOIN_QUEUE = "join_queue"
    const val QUEUE_DETAIL = "queue_detail"

    const val HISTORY = "history"
    const val PROFILE = "profile"

}



private data class BottomNavItem(

    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector

)



private val bottomNavItems = listOf(

    BottomNavItem(
        Routes.JOIN_QUEUE,
        "Join Queue",
        Icons.Default.AddCircle
    ),

    BottomNavItem(
        Routes.HOME,
        "My Queue",
        Icons.Default.Storefront
    ),

    BottomNavItem(
        Routes.HISTORY,
        "History",
        Icons.Default.History
    ),

    BottomNavItem(
        Routes.PROFILE,
        "Profile",
        Icons.Default.Person
    )

)



@Composable
fun AppNavigation(

    api: ApiService,
    session: SessionManager

) {

    val rootNav =
        rememberNavController()

    val authRepo =
        AuthRepository(api, session)

    val queueRepo =
        QueueRepository(api)

    val userRepo =
        UserRepository(api, session)



    NavHost(

        navController =
        rootNav,

        startDestination =
        Routes.SPLASH

    ) {

        composable(Routes.SPLASH) {

            SplashScreen(

                onTimeout = {

                    val destination =
                        if (session.isLoggedIn())
                            Routes.MAIN
                        else
                            Routes.LOGIN


                    rootNav.navigate(
                        destination
                    ) {

                        popUpTo(
                            Routes.SPLASH
                        ) {

                            inclusive =
                                true

                        }

                    }

                }

            )

        }



        composable(Routes.LOGIN) {

            LoginScreen(

                viewModel =
                LoginViewModel(authRepo),

                onLoginSuccess = {

                    rootNav.navigate(
                        Routes.MAIN
                    ) {

                        popUpTo(
                            Routes.LOGIN
                        ) {

                            inclusive =
                                true

                        }

                    }

                },

                onNavigateToRegister = {

                    rootNav.navigate(
                        Routes.REGISTER
                    )

                }

            )

        }



        composable(Routes.REGISTER) {

            RegisterScreen(

                viewModel =
                RegisterViewModel(authRepo),

                onRegisterSuccess = {

                    rootNav.navigate(
                        Routes.MAIN
                    ) {

                        popUpTo(
                            Routes.LOGIN
                        ) {

                            inclusive =
                                true

                        }

                    }

                },

                onNavigateToLogin = {

                    rootNav.popBackStack()

                }

            )

        }



        composable(Routes.MAIN) {

            MainScreen(

                queueRepo =
                queueRepo,

                userRepo =
                userRepo,

                session =
                session,

                onLogout = {

                    authRepo.logout()

                    rootNav.navigate(
                        Routes.LOGIN
                    ) {

                        popUpTo(
                            Routes.MAIN
                        ) {

                            inclusive =
                                true

                        }

                    }

                }

            )

        }

    }

}



@Composable
private fun MainScreen(

    queueRepo: QueueRepository,
    userRepo: UserRepository,
    session: SessionManager,
    onLogout: () -> Unit

) {

    val mainNav =
        rememberNavController()

    val navBackEntry by
    mainNav.currentBackStackEntryAsState()

    val currentDest =
        navBackEntry?.destination



    val showBottomBar =
        currentDest?.route != Routes.QUEUE_DETAIL



    Scaffold(

        bottomBar = {

            if (showBottomBar) {

                NavigationBar(

                    modifier =
                    Modifier
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 22.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                18.dp
                            )
                        )
                        .height(68.dp),

                    containerColor =
                    Color(
                        0xFF4A90E2
                    ),

                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)

                ) {

                    bottomNavItems.forEach {

                            item ->

                        NavigationBarItem(

                            selected =
                            currentDest
                                ?.hierarchy
                                ?.any {

                                    it.route ==
                                            item.route

                                } == true,


                            onClick = {

                                mainNav.navigate(
                                    item.route
                                ) {

                                    popUpTo(
                                        mainNav.graph
                                            .findStartDestination()
                                            .id
                                    ) {

                                        saveState =
                                            true

                                    }

                                    restoreState =
                                        true

                                    launchSingleTop =
                                        true

                                }

                            },

                            colors =
                            NavigationBarItemDefaults.colors(

                                selectedIconColor =
                                Color.White,

                                selectedTextColor =
                                Color.White,

                                unselectedIconColor =
                                Color.White,

                                unselectedTextColor =
                                Color.White,

                                indicatorColor =
                                Color.Transparent

                            ),

                            icon = {

                                Icon(

                                    imageVector =
                                    item.icon,

                                    contentDescription =
                                    item.label

                                )

                            },

                            label = {

                                Text(

                                    text =
                                    item.label,

                                    fontSize =
                                    12.sp

                                )

                            }

                        )

                    }

                }

            }

        }

    ) { innerPadding ->



        NavHost(

            navController =
            mainNav,

            startDestination =
            Routes.HOME,

            modifier =
            Modifier.padding(
                innerPadding
            )

        ) {



            composable(Routes.HOME) {

                HomeScreen(

                    viewModel =
                    HomeViewModel(queueRepo),

                    userName =
                    session.getName()
                        ?: "User",

                    onJoinQueue = {

                        mainNav.navigate(
                            Routes.JOIN_QUEUE
                        )

                    },

                    onViewQueueDetail = {

                        mainNav.navigate(
                            Routes.QUEUE_DETAIL
                        )

                    }

                )

            }



            composable(Routes.JOIN_QUEUE) {

                JoinQueueScreen(

                    viewModel =
                    JoinQueueViewModel(queueRepo),

                    onJoinSuccess = {

                        mainNav.navigate(
                            Routes.QUEUE_DETAIL
                        )

                    },

                    onViewHistory = {

                        mainNav.navigate(
                            Routes.HISTORY
                        )

                    }

                )

            }



            composable(
                Routes.QUEUE_DETAIL
            ) {

                QueueDetailScreen(

                    viewModel =
                    QueueDetailViewModel(
                        queueRepo
                    ),

                    onBack = {

                        mainNav.popBackStack()

                    },

                    onCancelled = {

                        mainNav.navigate(
                            Routes.HOME
                        )

                    },

                    onJoinAnother = {

                        mainNav.navigate(
                            Routes.JOIN_QUEUE
                        )
                    },
                    onDone = {
                        mainNav.navigate(
                            Routes.HISTORY
                        )
                    }
                )
            }

            composable(
                Routes.HISTORY
            ) {
                HistoryScreen(
                    viewModel =
                    HistoryViewModel(
                        queueRepo
                    )
                )
            }

            composable(
                Routes.PROFILE
            ) {
                ProfileScreen(
                    viewModel =
                    ProfileViewModel(
                        userRepo
                    ),
                    onLogout =
                    onLogout
                )
            }
        }
    }
}