package com.example.fdea.ui.login

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.ui.RegistrationViewModel
import com.example.fdea.ui.form.CustomAlertDialog
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue

// Modifier 설정을 함수로 추출
fun customButtonModifier(): Modifier {
    return Modifier
        .width(80.dp)
        .height(40.dp)
}
fun customFullWidthButtonModifier(): Modifier {
    return Modifier
        .fillMaxWidth(0.8f)
        .height(50.dp)
}
// Button 색상 설정을 함수로 추출
@Composable
fun customButtonColors(): ButtonColors {
    return ButtonDefaults.buttonColors(
        backgroundColor = LightBlue,
        contentColor = Color.White
    )
}
@Composable
fun AccountInfoScreen(navController: NavController, registrationViewModel: RegistrationViewModel = viewModel(),
                      stringArg: String?,) {
    var step by remember { mutableStateOf(0) }  // Keeps track of the current step
    var cameFromYes by remember { mutableStateOf(false) }  // "예"를 눌렀는지 여부 저장

    // BackHandler 구현
    BackHandler(enabled = step > 0) {
        when {
            /*step == 5 && cameFromYes -> {
                step = 4  // "예"를 눌러서 6단계에 왔다면 뒤로가면 5단계로 이동
            }
            step == 5 && !cameFromYes -> {
                step =3 // "아니요"를 눌러서 6단계에 왔다면 뒤로가면 4단계로 이동
            }*/
            step > 0 -> {
                step -= 1  // 그 외의 경우 일반적인 뒤로가기 동작
            }
            else -> {
                navController.popBackStack()  // 스텝이 0일 때 뒤로가기 스택에서 화면 제거
            }
        }
    }

    // 단계별 화면 전환
    when (step) {
        0 -> PersonalInfoInputScreen(
            navController = navController,
            registrationViewModel = registrationViewModel,
            stringArg = stringArg,
            onContinue = { step = 1 }  // 다음 단계로 진행
        )
        1 -> SignUpScreenStep2(
            onContinue = { step = 2 },
            onNo = { /* 경고를 표시하거나 다른 처리, 단계를 유지 */ }
        )
        2 -> SignUpScreenStep3(onContinue = { navController.navigate("registration_complete_screen") },registrationViewModel)

    }
}

@Composable
fun SignUpScreenStep2(onContinue: () -> Unit, onNo: () -> Unit) {
    SignUpScreen(
        title = "Sign Up",
        progressIndex = 1,
        description = "스노우보드 데이터를 수집하기 위해\n귀하의 계정이 필요합니다. 동의하십니까?",
        alertMessage = "앱 이용이 어렵습니다.",
        onContinue = onContinue,
        onNo = onNo
    )
}
@Composable
fun SignUpScreenStep3(onContinue: () -> Unit,registrationViewModel: RegistrationViewModel = viewModel(),) {
    SignUpStepContent(
        step=3,
        title = "Sign Up",
        progressIndex = 1,
        description = "스노우보드 계정을 알려주세요!",
        onContinue = onContinue,
        registrationViewModel

    )
}
/*@Composable
fun SignUpScreenStep4(onContinue: () -> Unit, onNo: () -> Unit) {
    SignUpScreen(
        title = "Sign Up",
        progressIndex = 2,
        description = "에브리타임 게시판 데이터를 수집하기 위해\n귀하의 계정이 필요합니다. 동의하십니까?\n만약 에브리타임 계정이 없다면 '아니요'를 눌러주세요.",
        alertMessage = "앱 이용이 어렵습니다.",
        onContinue = onContinue,
        onNo = onNo
    )
}
@Composable
fun SignUpScreenStep5(onContinue: () -> Unit,registrationViewModel: RegistrationViewModel = viewModel(),) {
    SignUpStepContent(
        step=5,
        title = "Sign Up",
        progressIndex = 2,
        description = "에브리타임 계정을 알려주세요!",
        onContinue = onContinue,
        registrationViewModel
    )
}

@Composable
fun SignUpScreenStep6(onContinue: () -> Unit,onNo: () -> Unit) {
    SignUpScreen(
        title = "Sign Up",
        progressIndex = 3 , // 프로그레스 바에서 현재 위치를 나타냄
        description = "스노위 게시판 데이터를 수집하기 위해\n귀하의 계정이 필요합니다. 동의하십니까?",
        alertMessage = "앱 이용이 어렵습니다.",
        onContinue = onContinue,
        onNo = onNo
    )
}
@Composable
fun SignUpScreenStep7(onContinue: () -> Unit,registrationViewModel: RegistrationViewModel = viewModel(),) {
    SignUpStepContent(
        step=7,
        title = "Sign Up",
        progressIndex = 3,
        description = "스노위 계정을 알려주세요!",
        onContinue = onContinue,
        registrationViewModel
    )
}*/
//아이디, 비번 입력 필드
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPasswordField: Boolean = false // 비밀번호 필드인지 여부를 설정
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (isPasswordField) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
}
@Composable
fun SignUpStepContent(
    step:Int,
    title: String,
    progressIndex: Int,
    description: String,
    onContinue: () -> Unit,
    registrationViewModel: RegistrationViewModel = viewModel()
) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    if (showAlert){
        CustomAlertDialog(
            showAlert = showAlert,
            onDismiss = { showAlert = false },
            alertMessage = "아이디와 비밀번호를 모두 입력해주세요"
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // SignUpHeader 호출
        SignUpHeader(
            title = title,
            progressIndex = progressIndex,
            description = description
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 아이디 입력 필드
        CustomTextField(
            value = id,
            onValueChange = { id = it },
            label = "아이디"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력 필드
        CustomTextField(
            value = password,
            onValueChange = { password = it },
            label = "비밀번호",
            isPasswordField = true // 비밀번호 필드로 설정
        )

        Spacer(modifier = Modifier.height(16.dp))

        // NEXT 버튼
        Button(
            onClick = {
                if (id.isEmpty() || password.isEmpty()) {
                    showAlert = true  // 아이디나 비밀번호가 비어있으면 알림 표시
                } else {
                    registrationViewModel.saveTempUserInfo2(step, id, password)
                    onContinue()  // 둘 다 입력된 경우에만 다음 단계로 진행

                }
            },
            modifier = customFullWidthButtonModifier(),
            colors = customButtonColors()
        ) {
            Text("NEXT")
        }
    }
}

@Composable
fun SignUpScreen(
    title: String,
    progressIndex: Int,
    description: String,
    alertMessage: String,
    onContinue: () -> Unit,
    onNo: () -> Unit
) {
    var showAlert by remember { mutableStateOf(false) }
    var alertMessageState by remember { mutableStateOf("") }
    var isYesChecked by remember { mutableStateOf(false) }
    var isNoChecked by remember { mutableStateOf(false) }

    if (showAlert) {
        CustomAlertDialog(
            showAlert = showAlert,
            onDismiss = { showAlert = false },
            alertMessage = alertMessageState
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // SignUpHeader 호출
        SignUpHeader(
            title = title,
            progressIndex = progressIndex,
            description = description
        )

        Text(
            text = "*앱 이외의 용도로 사용하지 않습니다.",
            color = Color.Red,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 체크박스 선택
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // "예" 체크박스
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isYesChecked,
                    onCheckedChange = {
                        isYesChecked = it
                        if (it) isNoChecked = false  // "예"를 선택하면 "아니요"는 자동으로 해제
                    }
                )
                Text(text = "예")
            }

            Spacer(modifier = Modifier.width(20.dp))

            // "아니요" 체크박스
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isNoChecked,
                    onCheckedChange = {
                        isNoChecked = it
                        if (it) isYesChecked = false  // "아니요"를 선택하면 "예"는 자동으로 해제
                    }
                )
                Text(text = "아니요")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // CONTINUE 버튼
        Button(
            onClick = {
                if (isYesChecked) {
                    onContinue()  // "예"가 선택된 경우 onContinue 호출
                } else if (isNoChecked) {
                    alertMessageState = alertMessage
                    showAlert = true  // "아니요"가 선택된 경우 AlertDialog 표시
                    onNo()
                }
            },
            modifier = customFullWidthButtonModifier(),
            colors = customButtonColors()
        ) {
            Text("CONTINUE")
        }
    }
}
@Composable
fun SignUpHeader(
    title: String,
    progressIndex: Int,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title 텍스트
        Text(
            text = title,
            style = MaterialTheme.typography.h4,
            color = Color.Black,
        )

        // 프로그레스 바
        Row(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .background(if (index <= progressIndex) LightBlue else Color.Gray, shape = MaterialTheme.shapes.small)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 설명 텍스트
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val navController = rememberNavController()
    val registrationViewModel = RegistrationViewModel()
    AccountInfoScreen(
        navController = navController,
        registrationViewModel = registrationViewModel,
        stringArg = "회원가입"
    )
}