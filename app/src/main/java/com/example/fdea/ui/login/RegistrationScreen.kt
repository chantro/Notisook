package com.example.fdea.ui.login

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.R
import com.example.fdea.ui.form.CustomAlertDialog
import com.example.fdea.ui.RegistrationViewModel
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue
import com.example.fdea.ui.theme.SkyBlue
import com.example.fdea.ui.theme.Yellow

//회원 가입 완료 Screen
@Composable
fun RegistrationCompleteScreen(navController: NavHostController) {
    BackHandler {
        navController.navigate("welcome_screen") {
            popUpTo("welcome_screen") { inclusive = true }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(top = 70.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(90.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_check_circle),
                contentDescription = "Confirmed",
                modifier = Modifier.size(70.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "회원가입이 완료되었습니다!\n사용자 인증이 될 때까지 기다려주세요",
                fontSize = 23.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "인증이 되기 전까지는 일부 기능을 이용하실 수 없습니다.",
                fontSize = 17.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(80.dp))
            Button(
                onClick = {
                    navController.navigate("home_screen") {
                        val stringArg="회원가입"
                        popUpTo("welcome_screen") { inclusive = false}
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(horizontal = 32.dp)
                    .height(48.dp)
            ) {
                Text("확인", fontSize = 20.sp, color = Color.Black)
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationCompleteScreen() {
    val navController = rememberNavController()
    RegistrationCompleteScreen(navController)
}

