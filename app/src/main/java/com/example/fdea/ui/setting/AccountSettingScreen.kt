package com.example.fdea.ui.setting

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.login.Auth
import com.example.fdea.login.AuthViewModel
import com.example.fdea.ui.form.CustomYesOrNoDialog
import com.example.fdea.ui.theme.LightBlue
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AccountSettingsScreen(navController: NavHostController,authViewModel:AuthViewModel) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val auth = Auth(context)
    var role by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    BackHandler {
        navController.navigate("setting_screen") {
            popUpTo("setting_screen") { inclusive = true }
        }
    }

    LaunchedEffect(user) {
        user?.let {
            role = auth.getRole(it)
            Log.d("role", role.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            MyTopBar(navController = navController, title = "계정설정")
            Spacer(modifier = Modifier.height(16.dp))
            SettingOption(text = "개인정보 변경") {
                val stringArg = "개인정보 변경"
                navController.navigate("account_info_screen/$stringArg")
            }
            if(role.equals("FA")){
                SettingOption(text = "회원 수락") {   //관리자만 쓸 수 있는 기능
                    Toast.makeText(context, "관리자입니다:)", Toast.LENGTH_SHORT).show()
                    navController.navigate("approval_screen")
                }
            }
            SettingOption(text = "탈퇴하기") {
                showDialog=true
            }
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = LightBlue,
                    strokeWidth = 5.dp
                )
            }
        }

        //탈퇴 여부 묻는 다이얼로그
        CustomYesOrNoDialog(
            showAlert = showDialog,
            onConfirm = {
                isLoading = true
                authViewModel.revokeAccess { isSuccess ->
                    isLoading = false
                    if (isSuccess) {
                        Log.d("Auth", "Logout and delete successful")
                        navController.navigate("welcome_screen") {
                            popUpTo("home_screen") {
                                inclusive = true
                            }
                        }
                    } else {
                        Log.d("Auth", "Logout or delete failed")
                    }
                }
                showDialog = false
            },
            onCancel = { showDialog = false },
            title = "정말 탈퇴하시겠습니까?",
            alertMessage = "탈퇴하시는 경우,\n" +
                    "기존에 앱에서 했던 활동들이 모두 지워집니다.\n" +
                    "스크랩, 전자 증명서 등 개인정보들을 삭제하고 저희 앱의 회원이 아니게 됩니다."
        )
    }




}

@Composable
fun SettingOption(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0E0))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSettingsScreenPreview() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    AccountSettingsScreen(navController = navController,authViewModel)
}

