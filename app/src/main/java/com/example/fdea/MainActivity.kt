package com.example.fdea

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fdea.login.AuthViewModel
import com.example.fdea.ui.BottomBar
import com.example.fdea.ui.Destinations
import com.example.fdea.ui.NavigationGraph
import com.example.fdea.ui.theme.FdeaTheme
import com.google.android.libraries.places.api.Places
import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.example.fdea.ui.alarm.subscribeToUserNotifications
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val authViewModel by viewModels<AuthViewModel>()

    // Algolia 클라이언트 초기화
    private val algoliaClient = ClientSearch(
        ApplicationID("QNU3HV0F11"),  // ApplicationID로 변환
        APIKey("3bdabe6f9e1d6fc228707c40ee793ab9")  // APIKey로 변환
    )
    private val algoliaIndex = algoliaClient.initIndex(IndexName("posts_data"))

    // 권한 요청 런처 생성
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){isGranted: Boolean ->
        if(isGranted){
            //권한이 승인된 경우 알림 전송 가능
            showToast("알림 권한이 승인되었습니다.")
        }else{
            //권한이 거부된 경우 처리
            showToast("알림 권한이 거부되었습니다.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 시작 시 로그인 상태 확인 후 사용자 토픽 구독
        if (FirebaseAuth.getInstance().currentUser != null) {
            subscribeToUserNotifications()
        }

        installSplashScreen()
        authViewModel.init(this)
        // Google Places API 초기화
        Places.initialize(applicationContext, getString(R.string.maps_api_key))
        setContent {
            FdeaTheme {
                val navController = rememberNavController()
                MainSetup(navController, authViewModel, algoliaIndex)
            }
        }
        // Android 13 이상에서만 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestNotificationPermission()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSetup(navController: NavHostController, authViewModel: AuthViewModel, algoliaIndex: com.algolia.search.client.Index) {


    // 항상 "home_screen"을 시작 경로로 설정
    LaunchedEffect(Unit) {
        navController.navigate(Destinations.HomeScreen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }

    // MainScreen 렌더링
    MainScreen(navController, algoliaIndex)
    /*val startDestination = remember { mutableStateOf("welcome_screen") }

    // 로그인 상태와 신규 사용자 여부 확인
    val loading = remember { mutableStateOf(true) }

    LaunchedEffect(key1 = true) {
        startDestination.value = authViewModel.navigateBasedOnUserData()
        loading.value = false  // 작업이 끝나면 로딩을 종료
    }

    if (loading.value) {
        // 로딩 중일 때 스플래시 또는 로딩 표시
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        MainScreen(navController,algoliaIndex)
    }

    // MainScreen 렌더링
    MainScreen(navController,algoliaIndex)*/
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, algoliaIndex: com.algolia.search.client.Index) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val buttonsVisible = remember(currentRoute) {
        mutableStateOf(
            currentRoute in listOf(
                Destinations.HomeScreen.route,
                Destinations.NoticeScreen.route,
                Destinations.SearchScreen.route,
                Destinations.SettingScreen.route,
                Destinations.FormScreen.route,
                Destinations.MyBenefitScreen.route,
                Destinations.ScrappedNoticeScreen.route
            )|| currentRoute?.startsWith("search_result_screen") == true
                    || currentRoute?.startsWith("notice_detail_screen")==true
        )
    }

    Scaffold(
        bottomBar = {
            if (buttonsVisible.value) {
                BottomBar(
                    navController = navController,
                    state = buttonsVisible,
                    modifier = Modifier
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavigationGraph(navController = navController, algoliaIndex = algoliaIndex)
        }
    }
}
