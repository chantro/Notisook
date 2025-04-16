package com.example.fdea.ui.login

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.MainActivity
import com.example.fdea.R
import com.example.fdea.login.AuthViewModel
import com.example.fdea.ui.theme.DarkBlue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(true) }
    var backPressCount by remember { mutableStateOf(0) }
    var backPressJob by remember { mutableStateOf<Job?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.handleSignInResult(result.data) { isSuccess, message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (isSuccess) {
                // 성공 시 navigateBasedOnUserData를 호출하여 동적으로 네비게이션 결정
                CoroutineScope(Dispatchers.Main).launch {
                    val destination = authViewModel.navigateBasedOnUserData()
                    navController.navigate(destination) {
                        popUpTo("welcome_screen") { inclusive = true }
                    }
                }
            }
        }
    }
    BackHandler {
        if (backPressCount == 1) {
            backPressJob?.cancel()
            navController.popBackStack()
            (context as? MainActivity)?.finish() // 앱 종료
        } else {
            backPressCount++
            Toast.makeText(context, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            backPressJob = CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(2000)
                backPressCount = 0
            }
        }
    }

    // 전체 화면을 채우는 Box, 배경색을 파란색으로 설정
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue),
        contentAlignment = Alignment.Center
    ) {
        // 중앙 정렬을 위한 Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spacer를 사용하여 아이콘과 버튼 사이의 간격을 추가
            Icon(
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Logo",
                modifier = Modifier.size(280.dp),
                tint = Color.Unspecified

            )
            Spacer(modifier = Modifier.height(32.dp))

            // 로그인 버튼
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        //한번 클릭 시, 로그인 버튼 비활성화
                        isEnabled = false

                        //로그인 실행(비동기 작업)
                        signInLauncher.launch(authViewModel.getGoogleSignInIntent())

                        //비동기 작업 완료 후, 다시 버튼 활성화
                        isEnabled = true
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    "로그인/회원가입",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            // 버튼 사이의 간격
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "숙명 이메일만 이용 가능합니다.",
                fontSize = 18.sp,
                color = Color.Yellow
            )
        }
    }
}

//미리보기
@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    WelcomeScreen(navController, authViewModel)
}