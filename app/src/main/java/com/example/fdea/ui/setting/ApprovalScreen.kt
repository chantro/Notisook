package com.example.fdea.ui.setting

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.data.AppUser
import com.example.fdea.login.UserService
import com.example.fdea.ui.setting.MyTopBar
import com.example.fdea.ui.theme.DarkBlue

@Composable
fun ApprovalScreen(navController: NavHostController) {
    val users = remember { mutableStateListOf<AppUser>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        UserService.loadPendingUsers { fetchedUsers ->
            users.clear()
            users.addAll(fetchedUsers)
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MyTopBar(navController = navController, title = "회원 수락")
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(users) { user ->
                    UserApprovalCard(
                        user = user,
                        onApprove = {
                            UserService.approveUser(user.studentNum) { success ->
                                if (success) {
                                    users.remove(user)
                                }
                            }
                        },
                        onReject = {
                            UserService.rejectUser(user.studentNum) { success ->
                                if (success) {
                                    users.remove(user)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun UserApprovalCard(user: AppUser, onApprove: () -> Unit, onReject: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 16.dp)
        .background(Color.LightGray)
        .padding(16.dp)
    ) {
        Text("이름: ${user.username}", fontSize = 18.sp)
        Text("학번: ${user.studentNum}", fontSize = 18.sp)
        Text("전화번호: ${user.phoneNum}", fontSize = 18.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Log.d("didid","${user.studentNum}")
            Button(
                onClick = {onApprove()},
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text("수락", color = Color.White)
            }
            Button(
                onClick = {onReject()},
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text("거절", color = Color.White)
            }


        }
    }
}
@Preview(showBackground = true)
@Composable
fun ApprovalScreenPreview() {
    val navController = rememberNavController()
    ApprovalScreen(navController)
}