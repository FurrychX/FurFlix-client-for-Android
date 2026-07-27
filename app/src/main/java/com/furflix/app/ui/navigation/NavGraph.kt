package com.furflix.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.furflix.app.ui.screens.*
import com.furflix.app.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val POST_DETAIL = "post/{postId}/{source}"
    const val SETTINGS = "settings"
    const val DOWNLOADS = "downloads"
    const val APPEARANCE_SETTINGS = "appearance_settings"
    const val FILTERS_SETTINGS = "filters_settings"
    const val LOGIN = "login"
    const val USER_PROFILE = "user/{username}"
    const val TAG_SEARCH = "tag_search/{tag}"
    const val ABOUT_DEVELOPER = "about_developer"

    fun postDetail(postId: String, source: String = "main") = "post/$postId/$source"
    fun userProfile(username: String) = "user/$username"
    fun tagSearch(tag: String) = "tag_search/${tag.replace("#", "%23")}"
}

@Composable
fun FurFlixNavGraph() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val watchlistViewModel: com.furflix.app.viewmodel.WatchlistViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToPost = { postId -> navController.navigate(Routes.postDetail(postId, "main")) },
                onNavigateToUser = { username -> navController.navigate(Routes.userProfile(username)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToDownloads = { navController.navigate(Routes.DOWNLOADS) },
                onNavigateToFavoritesPost = { postId -> navController.navigate(Routes.postDetail(postId, "favorites")) },
                viewModel = mainViewModel,
                watchlistViewModel = watchlistViewModel
            )
        }

        composable(
            route = Routes.POST_DETAIL,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            val source = backStackEntry.arguments?.getString("source") ?: "main"
            
            val submissions: List<com.furflix.app.data.model.Submission>
            val onLoadMore: () -> Unit
            
            if (source.startsWith("user_")) {
                val username = source.removePrefix("user_")
                val userProfileEntry = androidx.compose.runtime.remember(backStackEntry) { navController.getBackStackEntry(Routes.userProfile(username)) }
                val userViewModel: com.furflix.app.viewmodel.UserViewModel = viewModel(
                    viewModelStoreOwner = userProfileEntry,
                    factory = com.furflix.app.viewmodel.UserViewModel.Factory(
                        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
                        username
                    )
                )
                submissions = userViewModel.submissions.collectAsState().value
                onLoadMore = { userViewModel.loadMore() }
            } else if (source == "favorites") {
                submissions = watchlistViewModel.favoritesList.collectAsState().value
                onLoadMore = { watchlistViewModel.loadMore() }
            } else if (source.startsWith("tag_")) {
                val tag = source.removePrefix("tag_")
                val searchRoute = Routes.tagSearch("@keywords $tag")
                val tagSearchEntry = androidx.compose.runtime.remember(backStackEntry) { navController.getBackStackEntry(searchRoute) }
                val tagViewModel: com.furflix.app.viewmodel.TagSearchViewModel = viewModel(
                    viewModelStoreOwner = tagSearchEntry,
                    factory = com.furflix.app.viewmodel.TagSearchViewModel.Factory(
                        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
                        "@keywords $tag"
                    )
                )
                submissions = tagViewModel.submissions.collectAsState().value
                onLoadMore = { tagViewModel.loadMore() }
            } else {
                val selTab = mainViewModel.selectedTab.collectAsState().value
                submissions = when (selTab) {
                    2 -> mainViewModel.searchSubmissions.collectAsState().value
                    1 -> mainViewModel.latestSubmissions.collectAsState().value
                    else -> mainViewModel.browseSubmissions.collectAsState().value
                }
                onLoadMore = { mainViewModel.loadMore() }
            }

            PostDetailScreen(
                initialPostId = postId,
                submissions = submissions,
                onLoadMore = onLoadMore,
                onBack = { navController.popBackStack() },
                onAuthorClick = { username -> navController.navigate(Routes.userProfile(username)) },
                onNavigateToPost = { id -> navController.navigate(Routes.postDetail(id, source)) },
                onTagClick = { tag ->
                    navController.navigate(Routes.tagSearch("@keywords $tag"))
                },
                viewModel = mainViewModel
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                onNavigateToAppearance = { navController.navigate(Routes.APPEARANCE_SETTINGS) },
                onNavigateToFilters = { navController.navigate(Routes.FILTERS_SETTINGS) },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT_DEVELOPER) },
                viewModel = mainViewModel
            )
        }

        composable(Routes.ABOUT_DEVELOPER) {
            AboutDeveloperScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DOWNLOADS) {
            com.furflix.app.ui.screens.DownloadsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPost = { postId -> navController.navigate(Routes.postDetail(postId, "main")) },
                onNavigateToUser = { username -> navController.navigate(Routes.userProfile(username)) }
            )
        }

        composable(Routes.APPEARANCE_SETTINGS) {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FILTERS_SETTINGS) {
            FiltersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                viewModel = mainViewModel
            )
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: return@composable
            val userViewModel: com.furflix.app.viewmodel.UserViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = com.furflix.app.viewmodel.UserViewModel.Factory(
                    androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
                    username
                )
            )
            UserProfileScreen(
                username = username,
                viewModel = userViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPost = { postId -> navController.navigate(Routes.postDetail(postId, "user_$username")) },
                onNavigateToUser = { user -> navController.navigate(Routes.userProfile(user)) }
            )
        }

        composable(
            route = Routes.TAG_SEARCH,
            arguments = listOf(navArgument("tag") { type = NavType.StringType })
        ) { backStackEntry ->
            val tag = backStackEntry.arguments?.getString("tag") ?: return@composable
            val tagViewModel: com.furflix.app.viewmodel.TagSearchViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = com.furflix.app.viewmodel.TagSearchViewModel.Factory(
                    androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
                    tag
                )
            )
            TagSearchScreen(
                tag = tag.replace("@keywords ", "").replace("%23", "#"),
                viewModel = tagViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPost = { postId -> navController.navigate(Routes.postDetail(postId, "tag_${tag.replace("@keywords ", "")}")) },
                onNavigateToUser = { user -> navController.navigate(Routes.userProfile(user)) }
            )
        }
    }
}
