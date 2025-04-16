package com.example.fdea.ui.form

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fdea.data.ApplicantInfo
import com.example.fdea.ui.FormViewModel
import com.example.fdea.ui.setting.MyTopBar
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import com.example.fdea.data.Form
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ViewFirstServedListScreen(navController: NavHostController, formViewModel: FormViewModel, formId: String) {
    var applicantList by remember { mutableStateOf<List<ApplicantInfo>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val formName = remember { mutableStateOf("") }

    LaunchedEffect(formId) {
        coroutineScope.launch {
            val database = FirebaseDatabase.getInstance().reference
            val formRef = database.child("forms").child(formId)

            formRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val form = dataSnapshot.getValue(Form::class.java)
                    if (form != null) {
                        formName.value = form.formName
                        val newApplicantList = form.applicantInfos.filterNotNull()
                        applicantList = newApplicantList
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors.
                }
            })
        }
    }

    Scaffold(
        topBar = { MyTopBar(navController, "선착순 목록") }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .horizontalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        item {
                            ApplicantInfoHeader(formName = formName.value)
                        }
                        itemsIndexed(applicantList) { index, applicant ->
                            ApplicantInfoRow(index + 1, applicant, formName.value)
                        }
                    }

                }

            }
        }
    }
}


@Composable
fun ApplicantInfoHeader(formName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("#", modifier = Modifier.width(50.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        Text("이름", modifier = Modifier.width(80.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        Text("학번", modifier = Modifier.width(90.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        Text("전화번호", modifier = Modifier.width(150.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        Text("전공", modifier = Modifier.width(110.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        Text("신청 시간", modifier = Modifier.width(180.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        if (formName == "사물함 신청") {
            Text("사물함 번호", modifier = Modifier.width(100.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
            Text("사물함 위치", modifier = Modifier.width(90.dp), fontSize = 18.sp,fontWeight = FontWeight.Bold)
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ApplicantInfoRow(number: Int, applicantInfo: ApplicantInfo, formName: String) {
    val clipboardManager = LocalClipboardManager.current
    val appliedTimeFormatted = remember(applicantInfo.appliedTime) {
        val instant = Instant.ofEpochMilli(applicantInfo.appliedTime)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    }

    val rowContent = buildString {
        append("Number: $number\n")
        append("Name: ${applicantInfo.user.username}\n")
        append("Student Number: ${applicantInfo.user.studentNum}\n")
        append("Phone Number: ${applicantInfo.user.phoneNum}\n")
        append("Major: ${applicantInfo.user.major}\n")
        append("Applied Time: $appliedTimeFormatted\n")
        if (formName == "사물함 신청") {
            append("Locker Number: ${applicantInfo.lockerNum?.toString() ?: "N/A"}\n")
            append("Locker Zone: ${applicantInfo.lockerLocation ?: "N/A"}\n")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        clipboardManager.setText(AnnotatedString(rowContent))
                    }
                )
            }
    ) {
        Text(number.toString(), modifier = Modifier.width(50.dp), fontSize = 16.sp,)
        Text(applicantInfo.user.username, modifier = Modifier.width(80.dp), fontSize = 16.sp)
        Text(applicantInfo.user.studentNum, modifier = Modifier.width(90.dp), fontSize = 16.sp)
        Text(applicantInfo.user.phoneNum, modifier = Modifier.width(150.dp), fontSize = 16.sp)
        Text(applicantInfo.user.major, modifier = Modifier.width(110.dp), fontSize = 16.sp)
        Text(appliedTimeFormatted, modifier = Modifier.width(180.dp), fontSize = 16.sp)
        if (formName == "사물함 신청") {
            Text(applicantInfo.lockerNum?.toString() ?: "N/A", modifier = Modifier.width(100.dp), fontSize = 16.sp)
            Text(applicantInfo.lockerLocation, modifier = Modifier.width(90.dp), fontSize = 16.sp)
        }
    }
}
