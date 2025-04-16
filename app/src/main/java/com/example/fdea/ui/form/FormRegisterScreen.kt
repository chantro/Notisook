package com.example.fdea.ui.form

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.data.Form
import com.example.fdea.ui.FormViewModel
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.notice.MyTopBar2
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue
import com.example.fdea.ui.theme.Orange
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FormRegisterScreen(navController: NavHostController, formId: String, formViewModel: FormViewModel) {
    val viewModel: MainViewModel = viewModel()
    val selectedForm = viewModel.getFormById(formId).collectAsState(initial = null).value
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    if (showAlert) {
        CustomAlertDialog(
            showAlert = showAlert,
            onDismiss = { showAlert = false },
            alertMessage = alertMessage
        )
    }

    selectedForm?.let {
    } ?: Text("Loading or no data available")  // Form 객체가 null인 경우 처리
    if (selectedForm != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    MyTopBar2(
                        navController = navController,
                        toastMessage = "폼",
                        title = "폼 신청",
                        major = selectedForm.major,
                        onDeleteAllClicked = {
                            formViewModel.deleteForm(selectedForm)
                        },
                        onViewFirstComeFirstServedList = {
                            navController.navigate("view_first_served_list_screen/${formId}")
                        }
                    )
                }
                item {
                    FormInfoCard(selectedForm = selectedForm)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Column {
                        WarningMessageWithIcon(message = "학생회비를 납부하였는지 확인해주세요")
                        WarningMessageWithIcon(message = "재학 증명서를 발급하였는지 확인해주세요")
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { navController.navigate("certificate_screen") },
                            colors = ButtonDefaults.buttonColors(DarkBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable {}
                                .shadow(9.dp, CircleShape, true, Color.Magenta, Color.Black)
                        ) {
                            Text(
                                text = "재학증명서\n발급하러 가기",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Arrow Icon", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
                item {
                    Button(
                        onClick = {
                            val formattedEndDateTime = formatDateTime(selectedForm.endDate, selectedForm.endTime)
                            val formattedStartDateTime = formatDateTime(selectedForm.startDate, selectedForm.startTime)
                            val currentDateTime = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()))
                            if (selectedForm.type == "locker") {
                                when{
                                    (!validateDateTimeOrder(formattedStartDateTime, currentDateTime)) -> {
                                        alertMessage = "아직 신청 시간이 아닙니다."
                                        showAlert = true
                                    }
                                    (!validateDateTimeOrder(currentDateTime, formattedEndDateTime)) -> {
                                        alertMessage = "신청 기한이 끝났습니다."
                                        showAlert = true
                                    }
                                    /*!formViewModel.isCertificateAvailable() -> {
                                        alertMessage = "재학 증명서가 발급되지 않았습니다.\n재학 증명서를 발급한 뒤 시도해주세요."
                                        showAlert = true
                                    }*/
                                    else ->{
                                        navController.navigate("locker_display_screen/${formId}")
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    checkFormConditions(
                                        selectedForm = selectedForm,
                                        formViewModel = formViewModel,
                                        currentDateTime = currentDateTime,
                                        formattedStartDateTime = formattedStartDateTime,
                                        formattedEndDateTime = formattedEndDateTime,
                                        setAlertMessage = { msg -> alertMessage = msg },
                                        setShowAlert = { show -> showAlert = show }
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(LightBlue),
                        shape = RoundedCornerShape(8.dp),
                         modifier = Modifier
                            //.align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .padding(horizontal = 70.dp)
                            .height(70.dp)
                            .shadow(12.dp),
                        contentPadding = PaddingValues(vertical = 13.dp)
                    ) {
                        Text("신청하기", fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun checkFormConditions(
    selectedForm: Form,
    formViewModel: FormViewModel,
    currentDateTime: String,
    formattedStartDateTime: String,
    formattedEndDateTime: String,
    setAlertMessage: (String) -> Unit,
    setShowAlert: (Boolean) -> Unit
) {
    when {
        (!validateDateTimeOrder(formattedStartDateTime, currentDateTime)) -> {
            setAlertMessage("아직 신청 시간이 아닙니다.")
            setShowAlert(true)
        }
        selectedForm.applicantInfos.any { it?.user?.studentNum == formViewModel.currentUser.value?.studentNum } -> {
            setAlertMessage("신청은 1회만 가능합니다.")
            setShowAlert(true)
        }
        (!validateDateTimeOrder(currentDateTime, formattedEndDateTime)) -> {
            setAlertMessage("신청 기한이 끝났습니다.")
            setShowAlert(true)
        }
        (formViewModel.isFormFull(selectedForm)) -> {
            setAlertMessage("선착순 마감되었습니다")
            setShowAlert(true)
        }
        else -> {
            val success = formViewModel.addApplicantToForm(formViewModel, selectedForm, null,
                null
            )
            if (success) {
                setAlertMessage("신청이 완료되었습니다")
            } else {
                setAlertMessage("선착순 마감되었습니다.")
            }
            setShowAlert(true)
        }
    }
}

//폼 정보 박스
@Composable
fun FormInfoCard(selectedForm: Form) {
    val formattedStartDateTime = formatDateTime(selectedForm.startDate, selectedForm.startTime)
    val formattedEndDateTime = formatDateTime(selectedForm.endDate, selectedForm.endTime)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(DarkBlue),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 25.dp)
            .shadow(82.dp, shape = RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 30.dp, horizontal = 10.dp)
        ) {
            Text(
                text = selectedForm.formName,
                fontSize = 25.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$formattedStartDateTime\n~\n$formattedEndDateTime",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            if(selectedForm.type != "locker"){
                Text(
                    text = "인원: ${selectedForm.persons}명",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = selectedForm.content,
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

//학생회비 납부, 재학 증명서 발급하였는지 확인해달라는 문구
@Composable
fun WarningMessageWithIcon(message: String){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = "Warning Icon",
            tint = Orange
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun FormRegisterScreenPreview() {
    val navController = rememberNavController()
    val formId = ""
    val viewModel =FormViewModel()
    FormRegisterScreen(navController, formId, viewModel)
}