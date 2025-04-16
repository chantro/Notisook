package com.example.fdea.ui.login
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.R
import com.example.fdea.ui.form.CustomYesOrNoDialog
import com.example.fdea.ui.RegistrationViewModel
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.Yellow

@Composable
fun PersonalInfoInputScreen(
    navController: NavController,
    registrationViewModel: RegistrationViewModel= viewModel(),
    stringArg: String?,
    onContinue: () -> Unit // 추가된 onContinue 콜백
) {
    var showDialog by remember { mutableStateOf(false) }
    // 입력 필드의 상태 변수
    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf(false) }
    var studentNum by remember { mutableStateOf("") }
    var studentNumError by remember { mutableStateOf(false) }
    var phoneNumPart1 by remember { mutableStateOf("") }
    var phoneNumPart2 by remember { mutableStateOf("") }
    var phoneNumPart3 by remember { mutableStateOf("") }
    var phoneNumPart1Error by remember { mutableStateOf(false) }
    var phoneNumPart2Error by remember { mutableStateOf(false) }
    var phoneNumPart3Error by remember { mutableStateOf(false) }
    var major by remember { mutableStateOf("") }
    val majors = listOf("소프트웨어학부", "경영학부", "피아노과")
    var majorError by remember { mutableStateOf(false) }
    val welcomeText = when (stringArg) {
        "회원가입" -> "NOTISOOK에 오신 것을 환영합니다!"
        "개인정보 변경" -> "개인정보 변경"
        else -> "OO에 오신 것을 환영합니다!"
    }
    val buttonText = when (stringArg) {
        "회원가입" -> "등록하기"
        "개인정보 변경" -> "완료"
        else -> "등록하기"
    }

    Log.d("disod","$stringArg")

    fun validateFields() {
        val phoneNum = "$phoneNumPart1-$phoneNumPart2-$phoneNumPart3"
        usernameError = username.isBlank() // username 필드가 비어 있는지 확인합니다.
        studentNumError = studentNum.length != 7
        phoneNumPart1Error=phoneNumPart1.length!=3
        phoneNumPart2Error=phoneNumPart2.length!=4
        phoneNumPart3Error=phoneNumPart3.length!=4
        majorError=major.isEmpty()
        Log.d("chaedd","ed$major ee $usernameError + $studentNumError + $phoneNumPart1Error  + $phoneNumPart2Error + $phoneNumPart3Error + $majorError")
        if (!usernameError && !studentNumError && !phoneNumPart1Error && !phoneNumPart2Error
            && !phoneNumPart3Error&& !majorError ) {
            val stringArg2="회원가입"
            if(stringArg=="회원가입"){
                registrationViewModel.saveTempUserInfo(username, studentNum, phoneNum, major)
                //registrationViewModel.saveUserInfo(username,studentNum,phoneNum,major) // 사용자 정보를 ViewModel에 저장
                onContinue() // 조건을 만족하면 onContinue() 호출하여 다음 단계로 이동
            }else{
                showDialog=true

            }
        }
    }
    if(showDialog){
        CustomYesOrNoDialog(
            showAlert = showDialog,
            onConfirm = {
                registrationViewModel.updateUserInfo(username, studentNum, "$phoneNumPart1-$phoneNumPart2-$phoneNumPart3", major)
                navController.navigate("account_setting_screen")
            },
            onCancel = { showDialog = false },
            title = "개인정보 변경",
            alertMessage = "개인정보를 변경하시면 인증을 받을 때까지 일부 기능만 사용 가능합니다. 계속 진행하시겠습니까?"
        )
    }


    BackHandler(enabled = true) {
        if(stringArg=="회원가입") navController.navigate("welcome_screen")
        else if(stringArg=="개인정보 변경") navController.navigate("account_setting_screen")
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
            modifier = Modifier
                .padding(top = 70.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 벨 아이콘
            Icon(
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 로그인 텍스트
            Text(
                text = welcomeText,
                fontSize = 23.sp,
                modifier = Modifier.padding(start = 24.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(46.dp))
        }
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(top = 270.dp, start = 30.dp)
                .navigationBarsPadding()
                .imePadding()
        ){

            //사용자 이름
            CustomInputTextField(
                value = username,
                onValueChange = { username = it },
                label = "이름:     ",
                isError=usernameError,
                errorMessage = "이름을 입력해주세요"
            )
            //학번
            CustomInputTextField(
                value = studentNum,
                onValueChange = { studentNum = it },
                label = "학번:     ",
                isError=studentNumError,
                errorMessage = "학번 7자리를 입력해주세요",
                isStudentNum = true
            )
            //전화번호
            Row {
                CustomInputTextField(
                    value = phoneNumPart1,
                    onValueChange = { phoneNumPart1 = it },
                    label = "전화번호:   ",
                    isError = phoneNumPart1Error,
                    isPhoneNum2=true,
                    width = 80.dp)
                Text("-", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(vertical = 12.dp))
                CustomInputTextField(
                    value = phoneNumPart2,
                    onValueChange = { phoneNumPart2 = it },
                    isError = phoneNumPart2Error,
                    isPhoneNum = true,
                    width = 80.dp)
                Text("-", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(vertical = 12.dp))
                CustomInputTextField(
                    value = phoneNumPart3,
                    onValueChange = { phoneNumPart3 = it },
                    isPhoneNum = true,
                    isError = phoneNumPart3Error,
                    width = 80.dp)
            }
            //전공 선택
            MajorDropdown(majorError,major, majors) { selectedMajor ->
                major = selectedMajor
            }
            //등록하기 버튼
            Button(
                onClick = { validateFields() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.5f),
                contentPadding = PaddingValues(vertical = 15.dp)

            ) {
                Text(buttonText, fontSize = 20.sp, color = Color.Black)
            }
        }

    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MajorDropdown(isError: Boolean,selectedMajor: String, majors: List<String>, onMajorSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedMajor.isEmpty()) "전공 선택" else selectedMajor

    Spacer(modifier = Modifier.height(10.dp))
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = label,
            onValueChange = { },
            label = { Text("카테고리 선택") },
            readOnly = true,
            modifier = Modifier.menuAnchor()
                .border(1.dp, if (isError) Color.Red else Color.Transparent, MaterialTheme.shapes.small),

            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },

        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            majors.forEach { major ->
                DropdownMenuItem(
                    onClick = {
                        onMajorSelected(major)
                        expanded = false
                    },
                    text={Text(major)}
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(40.dp))

}

@Composable
fun CustomInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String="",
    isError: Boolean = false, // 입력 필드의 오류 상태
    errorMessage: String = "", // 표시할 오류 메시지
    modifier: Modifier = Modifier,
    isStudentNum: Boolean = false,
    isPhoneNum:Boolean=false,
    isPhoneNum2:Boolean=false,
    isMajor:Boolean=false,
    width: Dp =230.dp
) {
    var localValue by remember { mutableStateOf(value) }
    val keyboardOptions = if (isStudentNum||isPhoneNum||isPhoneNum2) {
        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
    } else {
        KeyboardOptions.Default
    }

    fun handleValueChange(newValue: String) {
        if (isStudentNum) {
            localValue = newValue.filter { it.isDigit() }.take(7)  // Ensure only 7 digits for student numbers
        }else if (isPhoneNum){
            localValue = newValue.filter { it.isDigit() }.take(4)
        }else if (isPhoneNum2){
            localValue = newValue.filter { it.isDigit() }.take(3)
        }
        else {
            localValue = newValue
        }
        onValueChange(localValue)
    }


    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label",
            fontSize = 16.sp,
            color = Color.White
        )
        OutlinedTextField(
            value = localValue,
            //onValueChange=onValueChange,
            onValueChange = ::handleValueChange,
            modifier
                .width(width)
                .height(50.dp),
            singleLine = true,
            isError = isError,
            textStyle = TextStyle(color = Color.White),
            keyboardOptions = keyboardOptions,
            placeholder = {
                if ((isError && localValue.isEmpty()) ) {
                    // 오류 상태일 때, 입력 필드가 비어 있으면 오류 메시지를 placeholder로 표시
                    Text(text = errorMessage,style=TextStyle(color=Color.Red))
                }
            },
        )

    }
    Spacer(modifier = Modifier.height(12.dp))

}
@Preview(showBackground = true)
@Composable
fun PersonalInfoInputScreenPreview() {
    val viewModel = RegistrationViewModel()
    val navController = rememberNavController()
    val stringArg: String = ""
    PersonalInfoInputScreen(navController, viewModel, stringArg, onContinue = {})
}