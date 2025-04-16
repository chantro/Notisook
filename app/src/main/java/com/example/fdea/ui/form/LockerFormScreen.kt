package com.example.fdea.ui.form

//noinspection UsingMaterialAndMaterial3Libraries
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.data.LockerDetail
import com.example.fdea.login.Auth
import com.example.fdea.login.UserService
import com.example.fdea.ui.FormViewModel
import com.example.fdea.ui.setting.MyTopBar
import com.example.fdea.ui.theme.LightBlue
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val myModifier2= Modifier
    .fillMaxWidth()
    .heightIn(min = 58.dp, max = 600.dp)
    .padding(horizontal = 12.dp) // 좌우 여백 설정
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LockerFormScreen(navController:NavHostController,viewModel: FormViewModel) {
    val user = FirebaseAuth.getInstance().currentUser
    var role by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val auth = Auth(context)
    val major by UserService.major.collectAsState()
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var enrolled by remember { mutableStateOf(true) }
    var paidFee by remember { mutableStateOf(true) }
    var content by remember { mutableStateOf("") }
    var lockerCount by remember { mutableStateOf("") }
    val lockerDetails = remember { mutableStateListOf<LockerDetail>() }

    LaunchedEffect(Unit) {
        UserService.loadUserData()
    }

    LaunchedEffect(user) {
        user?.let {
            val userRole = auth.getRole(it)
            role = userRole
            role="FA"  //TODO 임의로 권한 부여
        }
    }

    // 사물함 개수가 변경될 때 호출될 함수
    fun handleLockerCountChange(newCount: String) {
        lockerCount = newCount
        val count = newCount.toIntOrNull() ?: 0
        if (lockerDetails.size < count) {
            // 더 많은 사물함 정보 필드를 추가
            while (lockerDetails.size < count) {
                lockerDetails.add(LockerDetail())
            }
        } else if (lockerDetails.size > count) {
            // 불필요한 사물함 정보 필드를 제거
            lockerDetails.subList(count, lockerDetails.size).clear()
        }
    }

    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    CustomAlertDialog(showAlert = showAlert, onDismiss = { showAlert = false }, alertMessage = alertMessage)
    Box(
        modifier= Modifier
            .fillMaxSize()
    ){

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MyTopBar(navController, "사물함 신청폼")

            DateTimeRow(
                label = "시작 날짜",
                placeholderDate = "날짜",
                placeholderTime = "시작시간",
                date = startDate,
                time= startTime,
                onDateClick = { showDatePicker(context) { startDate = it } },
                onTimeClick = { showTimePicker(context) { startTime = it } },
            )
            Spacer(modifier = Modifier.height(7.dp))
            DateTimeRow(
                label = "종료 날짜",
                placeholderDate = "날짜",
                placeholderTime = "종료시간",
                date = endDate,
                time = endTime,
                onDateClick = { showDatePicker(context) { endDate = it } },
                onTimeClick = { showTimePicker(context) { endTime = it } }
            )
            Spacer(modifier = Modifier.height(7.dp))
            SwitchRow(
                enrolled = enrolled,
                onEnrolledChange = { enrolled = it },
                paidFee = paidFee,
                onPaidFeeChange = { paidFee = it }
            )
            ContentField(
                label = "내용",
                placeholder = "주의사항 등을 써주세요",
                value = content,
                onValueChange = { content = it })
            Spacer(modifier = Modifier.height(16.dp))
            //사물함 정보 입력
            Text(
                "사물함 정보",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 12.dp)
            )
            DynamicLockerInputFields(
                lockerCount = lockerCount,
                onLockerCountChange = ::handleLockerCountChange,
                lockerDetails = lockerDetails
            )
            //Spacer(modifier = Modifier.weight(1f))
            // 저장 버튼
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (startDate.isEmpty() || endDate.isEmpty() || startTime.isEmpty() ||
                            endTime.isEmpty() ||lockerCount.isEmpty()||
                            lockerDetails.any{it.location.isBlank()|| it.width.isBlank() || it.height.isBlank()}) {
                            alertMessage = "모든 폼을 채워주세요."
                            showAlert = true
                        } else {
                            // 날짜와 시간을 올바른 포맷으로 조정
                            val formattedStartDateTime = formatDateTime(startDate, startTime)
                            val formattedEndDateTime = formatDateTime(endDate, endTime)

                            if (!validateDateTimeOrder(formattedStartDateTime,formattedEndDateTime)) {
                                alertMessage = "종료 날짜는 시작 날짜보다 늦거나 같아야 합니다. 올바르게 입력해주세요."
                                showAlert = true
                            } else {
                                val currentDateTime = LocalDateTime.now()
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                val startDateTime = LocalDateTime.parse(formattedStartDateTime, formatter)

                                if (startDateTime.isBefore(currentDateTime)) {
                                    alertMessage = "시작 날짜와 시간은 현재 시각 이후여야 합니다."
                                    showAlert = true
                                } else {
                                    if (role.equals("FA")) {
                                        viewModel.saveLockerForm(
                                            major = "FA",
                                            startDate = startDate,
                                            endDate = endDate,
                                            startTime = startTime,
                                            endTime = endTime,
                                            enrolled = enrolled,
                                            paidFee = paidFee,
                                            content = content,
                                            lockerDetails
                                        )
                                    }  else if (role.equals("DA")) {
                                        major?.let {
                                            viewModel.saveLockerForm(
                                                major = it,
                                                startDate = startDate,
                                                endDate = endDate,
                                                startTime = startTime,
                                                endTime = endTime,
                                                enrolled = enrolled,
                                                paidFee = paidFee,
                                                content = content,
                                                lockerDetails
                                            )
                                        }
                                    }
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = LightBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 16.dp)
                        .size(width = 150.dp, height = 45.dp)
                        .shadow(8.dp, shape = MaterialTheme.shapes.medium)
                ) {
                    Text(text = "저장하기", fontSize = 16.sp)
                }

            }
        }
    }


}
@Composable
fun myColor2(): androidx.compose.material3.TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = Color.LightGray,
        unfocusedContainerColor= Color.LightGray,
        cursorColor = Color.Black,
        focusedIndicatorColor = LightBlue,
        unfocusedIndicatorColor = Color.Gray
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicLockerInputFields(
    lockerCount: String,
    onLockerCountChange: (String) -> Unit,
    lockerDetails: MutableList<LockerDetail>
) {
    Column{
        TextField(
            value = lockerCount,
            onValueChange = onLockerCountChange,
            label = { Text("사물함의 구역 개수") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            colors = myColor2(),
            modifier = myModifier
        )
        Spacer(modifier = Modifier.height(3.dp))
        lockerDetails.forEachIndexed { index, lockerDetail  ->
            var location by remember { mutableStateOf(lockerDetail.location) }
            var width by remember { mutableStateOf(lockerDetail.width) }
            var height by remember { mutableStateOf(lockerDetail.height) }
            TextField(
                value =location,
                onValueChange = {
                    location = it
                    lockerDetails[index].location = it  // lockerDetails 업데이트
                },
                label = { Text("사물함${index+1} 상세 내용(위치)") },
                modifier = myModifier2,
                colors = myColor2()
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row {
                TextField(
                    value = width,
                    onValueChange = {
                        width = it
                        lockerDetails[index].width = it  // lockerDetails 업데이트
                    },
                    label = { Text("사물함 가로(열) 개수") },
                    singleLine = true,
                    modifier= myModifier2.weight(0.5f),
                    colors = myColor2(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = height,
                    onValueChange = {
                        height = it
                        lockerDetails[index].height = it  // lockerDetails 업데이트
                    },
                    label = { Text("사물함 세로(행) 개수") },
                    singleLine = true,
                    modifier= myModifier2.weight(0.5f),
                    colors = myColor2(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PreviewLockerFormScreen() {

    val navController = rememberNavController()
    val viewModel = FormViewModel()
    LockerFormScreen(navController, viewModel)

}