package com.example.fdea.ui.setting

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.R
import com.example.fdea.login.AuthViewModel
import com.example.fdea.login.UserService
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.DarkGray
import com.example.fdea.ui.theme.LightBlue

@Composable
fun SettingScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    LaunchedEffect(Unit) {
        UserService.loadUserData()
    }


    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileSection(navController, authViewModel)
        MenuItems(navController)
        Spacer(modifier = Modifier.height(16.dp))
        ActionButton(navController,"scrapped_notice_screen")

    }
}

@Composable
fun ProfileSection(navController: NavHostController, authViewModel: AuthViewModel) {
    val username by UserService.username.collectAsState()
    val lockerLocation by UserService.lockerLocation.collectAsState()
    val lockerNum by UserService.lockerNum.collectAsState()
    Log.d("dfids","$username + $lockerLocation")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(LightBlue)
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "마이페이지",
            color = Color.White,
            fontSize = 27.sp,
            modifier = Modifier.padding(vertical = 20.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${username ?: "눈송이"}님,\n안녕하세요!",
                    color = Color.White,
                    fontSize = 20.sp,
                )
                Spacer(modifier=Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            authViewModel.signOut { isSuccess ->
                                if (isSuccess) {
                                    navController.navigate("welcome_screen") {
                                        popUpTo("home_screen") { inclusive = true }
                                    }
                                } else {
                                    Log.d("Logout", "Logout failed")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                    ) {
                        Text("로그아웃", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            navController.navigate("account_setting_screen")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                    ) {
                        Text("계정설정", color = Color.White)
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_locker),
                        contentDescription = "Locker Icon",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                    Text("나의 사물함", color = Color.White, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Gray)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    if (lockerLocation != null && lockerNum != null) {
                        Text(
                            "$lockerLocation\n${lockerNum}번",
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "아직 신청한\n 사물함이\n 없습니다",
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }


        }
    }
}

@Composable
fun MenuItems(navController: NavHostController) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        MenuButton(
            text = "사물함 & 간식 신청",
            icon = Icons.Filled.Lock,
            backgroundColor = Color(0xFF5DBBE6) // 하늘색
        ) {
            navController.navigate("form_screen")
        }
        MenuButton(
            text = "나의 혜택",
            icon = Icons.Filled.Percent,
            backgroundColor = Color(0xFFABC4CF)// 하얀색
        ) {
            navController.navigate("my_benefit_screen")
        }
    }
}

@Composable
fun MenuButton(text: String, icon: ImageVector, backgroundColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(8.dp)
            .width(160.dp)
            .height(160.dp)
            .shadow(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(48.dp),
                tint = Color.Black
            )
            Text(
                text,
                color = Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ActionButton(navController: NavHostController,navigateRoute:String) {
    Button(
        onClick = { navController.navigate(navigateRoute) },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .height(130.dp)
            .shadow(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkGray)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if(navigateRoute=="scrapped_notice_screen"){
                Text(
                    "관심있는 공지사항 모아보기!",
                    color = Color.White,
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_memoji),
                    contentDescription = "Document Icon",
                    tint = Color.Unspecified
                )
            }else{
                Text(
                    "사물함 & 간식 신청",
                    color = Color.White,
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_memoji),
                    contentDescription = "Document Icon",
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewSettingScreen() {
    val authViewModel: AuthViewModel = viewModel()
    val navController = rememberNavController()
    SettingScreen(navController, authViewModel)
}
