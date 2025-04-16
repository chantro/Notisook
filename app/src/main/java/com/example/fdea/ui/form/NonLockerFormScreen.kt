package com.example.fdea.ui.form
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar


val myModifier= Modifier
    .padding(horizontal = 12.dp,)
    .height(58.dp)
@Composable
fun myColor():TextFieldColors{
    return TextFieldDefaults.textFieldColors(
        focusedIndicatorColor = LightBlue,
        unfocusedIndicatorColor = Color.Gray,
        cursorColor = Color.Black
    )
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NonLockerFormScreen(navController: NavHostController, viewModel: FormViewModel) {
    val user = FirebaseAuth.getInstance().currentUser
    var role by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val auth = Auth(context)
    val major by UserService.major.collectAsState()
    var formName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var persons by remember { mutableStateOf("") }
    var enrolled by remember { mutableStateOf(true) }
    var paidFee by remember { mutableStateOf(true) }
    var content by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    val lockerDetails = remember { mutableStateListOf<LockerDetail>() }

    LaunchedEffect(Unit) {
        UserService.loadUserData()
    }

    LaunchedEffect(user) {
        user?.let {
            val userRole = auth.getRole(it)
            role = userRole
            role="FA"  //TODO 임의로 role 설정
        }
    }

    CustomAlertDialog(showAlert = showAlert, onDismiss = { showAlert = false }, alertMessage = alertMessage)
    Box(
        modifier= Modifier
            .fillMaxSize()

    ){
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MyTopBar(navController, "선착순 신청폼")
            FormField(
                label = "폼 이름",
                placeholder = "Enter Form name...",
                value = formName,
                onValueChange = { formName = it })
            Spacer(modifier = Modifier.height(7.dp))
            DateTimeRow(
                label = "시작 날짜",
                placeholderDate = "날짜",
                placeholderTime = "시작시간",
                date = startDate,
                time = startTime,
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
            PersonCountRow(value = persons, onValueChange = { persons = it })
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

            // 저장 버튼
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 15.dp)
            ) {
                Button(
                    onClick = {
                        if (formName.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || persons.isEmpty()) {
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
                                        viewModel.saveNonLockerForm(
                                            major = "FA",
                                            formName = formName,
                                            startDate = startDate,
                                            endDate = endDate,
                                            startTime = startTime,
                                            endTime = endTime,
                                            persons = persons,
                                            enrolled = enrolled,
                                            paidFee = paidFee,
                                            content = content,
                                            lockerDetails
                                        )
                                    }  else if (role.equals("DA")) {
                                        major?.let {
                                            viewModel.saveNonLockerForm(
                                                major = it,
                                                formName = formName,
                                                startDate = startDate,
                                                endDate = endDate,
                                                startTime = startTime,
                                                endTime = endTime,
                                                persons = persons,
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
                    colors = ButtonDefaults.buttonColors(LightBlue),

                    shape = MaterialTheme.shapes.medium,
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
@RequiresApi(Build.VERSION_CODES.O)
fun validateDateTimeOrder(startDateTimeStr: String, endDateTimeStr: String): Boolean {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val startDateTime = LocalDateTime.parse(startDateTimeStr, formatter)
        val endDateTime = LocalDateTime.parse(endDateTimeStr, formatter)

        // startDateTime이 endDateTime보다 늦거나 같은 경우 true를 반환
        !startDateTime.isAfter(endDateTime)
    } catch (e: DateTimeParseException) {
        Log.e("DateTimeValidation", "Error parsing dates: ${e.message}")
        false
    }
}

fun formatDateTime(date: String, time: String): String {
    // 날짜 파싱
    val dateParts = date.split("-")
    val year = dateParts[0]
    val month = dateParts[1].padStart(2, '0') // 월을 두 자리로 포맷팅
    val day = dateParts[2].padStart(2, '0')   // 일을 두 자리로 포맷팅

    // 시간 파싱
    val timeParts = time.split(":")
    val hours = timeParts[0].padStart(2, '0') // 시를 두 자리로 포맷팅
    val minutes = timeParts[1].padStart(2, '0') // 분을 두 자리로 포맷팅

    return "$year-$month-$day $hours:$minutes"
}
@Composable
fun CustomYesOrNoDialog(
    showAlert: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title:String,
    alertMessage: String
) {
    if(showAlert){
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(title, fontWeight=FontWeight.Bold ,fontSize=23.sp) },
            text = { Text(alertMessage, fontSize = 16.sp) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(DarkBlue)
                ) {
                    Text("예", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(DarkBlue)
                ) {
                    Text("아니요", color = Color.White)
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }

}
@Composable
fun CustomAlertDialog(
    showAlert: Boolean,
    onDismiss: () -> Unit,
    alertMessage: String
) {
    if (showAlert) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {Text(alertMessage, fontSize = 18.sp) },
            confirmButton = {
                Button(
                    onClick =onDismiss,
                    colors = ButtonDefaults.buttonColors(DarkBlue)
                ) {
                    Text("확인", color = Color.White)
                }
            },
            modifier = Modifier.padding(16.dp),

        )
    }
}

//폼 이름 작성
@Composable
fun FormField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column {
        Text(
            "폼 이름",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 12.dp,top=8.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            colors= myColor(),
            textStyle = TextStyle(fontSize = 17.sp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus() // 키보드 내리기
            }),
            singleLine = true, // 한 줄로 표시
            modifier = myModifier
                .fillMaxWidth()
                .widthIn(max = 200.dp) // 최대 너비 설정*/
        )
    }
}


//시작 날짜, 종료날짜 입력받음
@Composable
fun DateTimeRow(
    label: String,
    placeholderDate: String,
    placeholderTime: String,
    date: String,
    time: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column {
        Text(
            label,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 12.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            //modifier = Modifier.padding(end = 15.dp)
        ) {
            //날짜
            TextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholderDate) },
                leadingIcon = {
                    IconButton(onClick = onDateClick) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar Icon")
                    }
                },
                colors= myColor(),
                textStyle = TextStyle(fontSize = 17.sp),
                singleLine = true,
                modifier= myModifier
                    .weight(1f)
                    .clickable(onClick = onDateClick)
            )
            //시간
            TextField(
                value = time,
                readOnly = true,
                onValueChange = {},
                colors = myColor(),
                textStyle = TextStyle(fontSize = 17.sp),
                placeholder = { Text(placeholderTime) },
                modifier= myModifier
                    .weight(1f)
                    .clickable(onClick = onTimeClick),
                leadingIcon = {
                    IconButton(onClick = onTimeClick) {
                        Icon(Icons.Filled.AccessTime, contentDescription = "Time Icon")
                    }
                },

            )
        }
    }
}

//학생 수
@Composable
fun PersonCountRow(value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var textValue by remember { mutableStateOf(value.toString()) }
    Column() {
        Text(
            "인원",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 12.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            TextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    onValueChange(it)
                },
                colors = myColor(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus() // 키보드 내리기
                }),
                textStyle = TextStyle(fontSize = 17.sp),
                singleLine = true,
                modifier = myModifier
                    .widthIn(max = 80.dp) // 최대 너비 설정
            )
            Text(
                "명",
                fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 12.dp)
            )

        }

    }
}

@Composable
fun SwitchRow(
    enrolled: Boolean,
    onEnrolledChange: (Boolean) -> Unit,
    paidFee: Boolean,
    onPaidFeeChange: (Boolean) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "재학 여부",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
            )
            Switch(
                checked = enrolled,
                onCheckedChange = onEnrolledChange,
                colors = SwitchDefaults.colors(LightBlue),
                modifier = Modifier.scale(1.3f) // 크기 조절
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "학생회비 납부 여부",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
            )
            Switch(
                checked = paidFee,
                onCheckedChange = onPaidFeeChange,
                colors = SwitchDefaults.colors(LightBlue),
                modifier = Modifier.scale(1.3f) // 크기 조절
            )
        }
    }
}

@Composable
fun ContentField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column {
        Text(
            "주의사항",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 12.dp)
        )
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            colors= myColor(),
            textStyle = TextStyle(fontSize = 17.sp),
            maxLines = 10,
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp, max = 600.dp)
                .padding(horizontal = 12.dp) // 좌우 여백 설정
                .widthIn(max = 200.dp) // 최대 너비 설정
                .verticalScroll(rememberScrollState()),
        )

    }
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        onDateSelected("$year-${month + 1}-$day")
    }
    DatePickerDialog(
        context,
        dateSetListener,
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun showTimePicker(context: Context, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
        onTimeSelected("$hour:$minute")
    }
    TimePickerDialog(
        context,
        timeSetListener,
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PreviewNonLockerFormScreen() {

    val navController = rememberNavController()
    val viewModel = FormViewModel()
    NonLockerFormScreen(navController, viewModel)

}