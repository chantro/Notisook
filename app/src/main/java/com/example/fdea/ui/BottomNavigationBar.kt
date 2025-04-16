package com.example.fdea.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.algolia.search.client.Index
import com.example.fdea.login.AuthViewModel
import com.example.fdea.ui.home.RecommendedNoticeScreen

import com.example.fdea.ui.form.FormRegisterScreen
import com.example.fdea.ui.form.FormScreen
import com.example.fdea.ui.form.LockerDisplayScreen
import com.example.fdea.ui.form.LockerFormScreen
import com.example.fdea.ui.form.NonLockerFormScreen
import com.example.fdea.ui.form.ViewFirstServedListScreen
import com.example.fdea.ui.home.HomeScreen
import com.example.fdea.ui.notice.NoticeDetailScreen
import com.example.fdea.ui.notice.NoticeScreen
import com.example.fdea.ui.login.AccountInfoScreen
import com.example.fdea.ui.search.SearchResultScreen
import com.example.fdea.ui.search.SearchScreen
import com.example.fdea.ui.setting.AccountSettingsScreen
import com.example.fdea.ui.setting.CertificateScreen
import com.example.fdea.ui.setting.ImminentGifticonsScreen
import com.example.fdea.ui.setting.MyBenefitScreen
import com.example.fdea.ui.setting.ScrappedNoticeScreen
import com.example.fdea.ui.setting.SettingScreen
import com.example.fdea.ui.login.RegistrationCompleteScreen
import com.example.fdea.ui.login.WelcomeScreen
import com.example.fdea.ui.setting.ApprovalScreen

sealed class Destinations(
    val route: String,
    val icon: ImageVector? = null
) {
    //Bottom navigation
    object HomeScreen : Destinations(
        route = "home_screen",
        icon = Icons.Outlined.Home
    )

    object NoticeScreen : Destinations(
        route = "notice_screen",
        icon = Icons.Outlined.Info
    )

    object FormScreen : Destinations(
        route = "form_screen",
    )

    object SearchScreen:Destinations(
        route="search_screen" ,
        icon = Icons.Outlined.Search
    )
    object SearchResultScreen:Destinations(
        route="search_result_screen",

    )

    object SettingScreen : Destinations(
        route = "setting_screen",
        icon = Icons.Outlined.Person
    )


    //Login
    object WelcomeScreen : Destinations(
        route = "welcome_screen"
    )


    object RegistrationCompleteScreen : Destinations(
        route = "registration_complete_screen"
    )

    object CertificateScreen : Destinations(
        route = "certificate_screen"
    )

    object MyBenefitScreen : Destinations(
        route = "my_benefit_screen"
    )

    object ScrappedNoticeScreen : Destinations(
        route = "scrapped_notice_screen"
    )

    object NoticeDetailScreen:Destinations(
        "notice_detail_screen/{postId}"
    )
    object NonLockerFormScreen: Destinations(
        route="non_locker_form_screen"
    )
    object LockerFormScreen:Destinations(
        route="locker_form_screen"
    )
    object RecommendedNoticeScreen:Destinations(
        route="recommended_notice_screen"
    )
    object FormRegisterScreen:Destinations(
        route="form_register_screen/{formId}"
    )
    object LockerDisplayScreen:Destinations(
        route="locker_display_screen/{formId}"
    )
    object ViewFirstServedListScreen:Destinations(
        route="view_first_served_list_screen/{formId}"
    )
    object AccountInfoScreen:Destinations(
        route="account_info_screen/{stringArg}"
    )
    object ImminentGifticonsScreen:Destinations(
        route="imminent_gifticons_screen"
    )

    object AccountSettingScreen:Destinations(
        route="account_setting_screen"
    )
    object ApprovalScreen:Destinations(
        route="approval_screen"
    )
}
@Composable
fun BottomBar(
    navController: NavHostController, state: MutableState<Boolean>, modifier: Modifier = Modifier
) {

    val screens = listOf(
        Destinations.HomeScreen, Destinations.NoticeScreen,
        Destinations.SearchScreen, Destinations.SettingScreen
    )
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(imageVector = screen.icon!!, contentDescription = "")
                },
                selected = currentRoute == screen.route ||
                        (currentRoute in listOf(
                            Destinations.FormScreen.route,
                            Destinations.MyBenefitScreen.route,
                            Destinations.ScrappedNoticeScreen.route
                        ) && screen == Destinations.SettingScreen) ||
                        (currentRoute?.startsWith("search_result_screen") == true && screen == Destinations.SearchScreen) ||
                        (currentRoute?.startsWith("notice_detail_screen") == true && screen == Destinations.NoticeScreen),
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = LightGray,
                    selectedIconColor = Color.Black,
                    indicatorColor = Gray
                ),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(navController: NavHostController, algoliaIndex: Index) {
    //val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val certificateViewModel: CertificateViewModel = viewModel()
    val registrationViewModel: RegistrationViewModel = viewModel()
    val boardViewModel: BoardViewModel = viewModel()
    val formViewModel:FormViewModel = viewModel()
    val benefitViewModel: BenefitViewModel = viewModel()


    NavHost(navController = navController, startDestination = "welcome_screen") {
        // Algolia 인덱스를 SearchScreen에 전달
        composable(Destinations.SearchScreen.route) { SearchScreen(navController, mainViewModel, algoliaIndex) }
        composable(Destinations.WelcomeScreen.route) { WelcomeScreen(navController, authViewModel) }
        composable(Destinations.RegistrationCompleteScreen.route) { RegistrationCompleteScreen(navController) }
        composable(Destinations.HomeScreen.route) { HomeScreen(mainViewModel, navController,boardViewModel) }
        composable(Destinations.NoticeScreen.route) { NoticeScreen(mainViewModel,navController,boardViewModel) }

        composable(Destinations.FormScreen.route) { FormScreen(mainViewModel, navController) }
        composable(Destinations.SettingScreen.route) { SettingScreen(navController, authViewModel) }
        composable(Destinations.CertificateScreen.route) { CertificateScreen(navController, certificateViewModel)}
        composable(Destinations.MyBenefitScreen.route) { MyBenefitScreen(navController, benefitViewModel) }
        composable( Destinations.ScrappedNoticeScreen.route) { ScrappedNoticeScreen(navController, boardViewModel, mainViewModel) }
        composable(Destinations.NonLockerFormScreen.route) { NonLockerFormScreen(navController, formViewModel) }
        composable(Destinations.LockerFormScreen.route) { LockerFormScreen(navController, formViewModel) }
        composable(Destinations.ImminentGifticonsScreen.route){ ImminentGifticonsScreen(navController,benefitViewModel) }
        composable(Destinations.AccountSettingScreen.route){ AccountSettingsScreen(navController,authViewModel) }
        composable(Destinations.ApprovalScreen.route){ ApprovalScreen(navController) }
        composable(Destinations.RecommendedNoticeScreen.route) { RecommendedNoticeScreen(navController, mainViewModel,boardViewModel) }
        // Algolia 인덱스를 SearchResultScreen에 전달
        composable(
            route = "search_result_screen/{searchText}/{searchScope}",
            arguments = listOf(
                navArgument("searchText") { type = NavType.StringType },
                navArgument("searchScope") { type = NavType.StringType } // searchScope 인자를 추가
            )
        ) { backStackEntry ->
            val searchText = backStackEntry.arguments?.getString("searchText") ?: ""
            val searchScope = backStackEntry.arguments?.getString("searchScope") ?: "" // searchScope 가져오기
            SearchResultScreen(navController = navController,mainViewModel, searchText = searchText, searchScope = searchScope, algoliaIndex = algoliaIndex,boardViewModel)
        }
        composable(
            Destinations.FormRegisterScreen.route,
            arguments = listOf(navArgument("formId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId") ?: ""
            FormRegisterScreen(navController, formId, formViewModel)
        }

        composable(
            Destinations.NoticeDetailScreen.route,
            arguments = listOf(navArgument("postId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            NoticeDetailScreen(navController = navController, postId = postId, mainViewModel, boardViewModel)
        }
        composable(
            Destinations.LockerDisplayScreen.route,
            arguments = listOf(navArgument("formId") {
                type = NavType.StringType
            })
        ){backStackEntry->
            val formId = backStackEntry.arguments?.getString("formId") ?: ""
            LockerDisplayScreen(navController = navController, formViewModel, formId =formId )
        }
        composable(
            Destinations.AccountInfoScreen.route,
            arguments = listOf(navArgument("stringArg") { type = NavType.StringType })
        ) { backStackEntry ->
            val stringArg = backStackEntry.arguments?.getString("stringArg")
            AccountInfoScreen(
                navController = navController,
                registrationViewModel = registrationViewModel,
                stringArg = stringArg
            )
        }
        composable(
            Destinations.ViewFirstServedListScreen.route,
            arguments = listOf(navArgument("formId") {
                type = NavType.StringType
            })
        ){backStackEntry->
            val formId = backStackEntry.arguments?.getString("formId") ?: ""
            ViewFirstServedListScreen(navController = navController, formViewModel, formId =formId )
        }


    }
}

