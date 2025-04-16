package com.example.fdea.ui.form

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fdea.data.Form
import com.example.fdea.login.Auth
import com.example.fdea.login.UserService
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.setting.MyTopBar
import com.example.fdea.ui.theme.LightBlue
import com.google.firebase.auth.FirebaseAuth

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FormScreen(viewModel: MainViewModel, navController: NavController) {
    val approved by UserService.approved.collectAsState()
    val forms by viewModel.forms.collectAsState()
    Log.d("FormScreen", forms.toString())
    var showAddEvent by remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val auth = Auth(context)
    var role by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        user?.let {
            role = auth.getRole(it)
            role="FA" //TODO 임의로 role FA로 설정
            Log.d("role", role.toString())
        }
    }

    LaunchedEffect(Unit) {
        UserService.loadUserData()
    }
    Log.d("dfdfd","$forms")

    if (approved == true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                MyTopBar(navController = navController, title = "Form")
                LazyColumn {
                    items(forms) { form ->
                        FormCard(navController, form)
                    }
                }
            }
            if(role.equals("DA") || role.equals("FA")){
            FloatingActionButton(
                onClick = { showAddEvent = !showAddEvent },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = LightBlue,
                shape = CircleShape,
            ) {
                Icon(if (showAddEvent) Icons.Filled.Close else Icons.Filled.Add, "Toggle Buttons")
            }

            // 추가 버튼들 표시
            AnimatedVisibility(
                visible = showAddEvent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 82.dp, end = 26.dp),

                ) {
                Column(horizontalAlignment = Alignment.End) {
                    ButtonRow("사물함 신청폼", onClick = {navController.navigate("locker_form_screen")}, Icons.Default.Lock)
                    ButtonRow("사물함 외 선착순 신청폼", onClick = { navController.navigate(("non_locker_form_screen")) }, Icons.Outlined.AccountBox)
                }
            }
            }
        }
    }else {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ){
                MyTopBar(navController = navController, title = "Form")
                HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                Text("승인된 사용자만 접근할 수 있습니다.", fontSize = 20.sp, textAlign = TextAlign.Center)
            }

        }
    }
}

@Composable
fun ButtonRow(text: String, onClick: () -> Unit, icon: ImageVector) {
    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            modifier = Modifier.padding(end = 8.dp)
        )
        Button(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text)
        }
    }
}

@Composable
fun FormCard(navController: NavController, form: Form) {
    // Replace FormModel with your actual form data class
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate("form_register_screen/${form.id}")
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = form.formName,
                fontSize = 19.sp
            )
            Text(
                text = "${form.startDate} - ${form.endDate}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End

            ) // Replace with actual date range property
        }
    }
}

