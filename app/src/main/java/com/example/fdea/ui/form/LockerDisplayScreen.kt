package com.example.fdea.ui.form

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.data.ApplicantInfo
import com.example.fdea.data.Form
import com.example.fdea.data.LockerDetail
import com.example.fdea.ui.FormViewModel
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.setting.MyTopBar
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue
import com.example.fdea.ui.theme.Yellow
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LockerDisplayScreen(
    navController: NavController,
    formViewModel: FormViewModel,
    formId: String
) {
    val viewModel: MainViewModel = viewModel()
    val selectedForm = viewModel.getFormById(formId).collectAsState(initial = null).value
    val lockerList = selectedForm?.lockerInfo
    var isRefreshing by remember { mutableStateOf(false) }

    // 단순 새로고침 상태 관리 함수
    val onRefresh = {
        isRefreshing = true
        navController.popBackStack() // Navigate back
        if (selectedForm != null) {
            navController.navigate("locker_display_screen/${selectedForm.id}")
        } // Navigate to the screen again
        isRefreshing = false
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            navController.popBackStack() // Navigate back
            navController.navigate("locker_display_screen/$formId") // Navigate to the screen again
            isRefreshing = false
        }
    )

    Scaffold(
        topBar = {
            MyTopBar(navController, "사물함 신청")
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                lockerList?.forEachIndexed { index, locker ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f / lockerList.size)
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(LightBlue)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "사물함 ${index + 1} : ${locker.location}",
                                fontSize = 24.sp,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        LockerGrid(formViewModel, selectedForm, locker, locker.location, onRefresh)
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun LockerGrid(
    formViewModel: FormViewModel,
    selectedForm: Form,
    locker: LockerDetail,
    lockerLocation: String,
    onRefresh: () -> Unit
) {
    val lockerWidth = locker.width.toIntOrNull() ?: 0
    val lockerHeight = locker.height.toIntOrNull() ?: 0
    LazyVerticalGrid(
        columns = GridCells.Fixed(lockerWidth),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(lockerHeight * lockerWidth) { index ->
            LockerButton(
                formViewModel, selectedForm,
                (index + 1).toString(), lockerLocation, onRefresh
            )
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun LockerButton(
    formViewModel: FormViewModel,
    selectedForm: Form,
    lockerNumber: String,
    lockerLocation: String,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var reservedSuccess by remember { mutableStateOf(false) }
    var reservedFailed by remember { mutableStateOf(false) }
    var reserved by remember { mutableStateOf(false) }
    var isReserved by remember { mutableStateOf(false) }
    var alreadyAppliedDialog by remember { mutableStateOf(false) }
    var isCurrentUserLocker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance().reference
    val lockerRef = database.child("forms").child(selectedForm.id).child("applicantInfos")
    val currentUser = formViewModel.currentUser.value

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            lockerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var currentUserLocker = false
                    isReserved = dataSnapshot.children.any { snapshot ->
                        val applicantInfo = snapshot.getValue(ApplicantInfo::class.java)
                        if (applicantInfo?.lockerNum == lockerNumber && applicantInfo.lockerLocation == lockerLocation) {
                            if (applicantInfo.user.studentNum == currentUser.studentNum) {
                                currentUserLocker = true
                            }
                            true
                        } else {
                            false
                        }
                    }
                    isCurrentUserLocker = currentUserLocker
                    reserved = isReserved
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    Button(
        onClick = {
            if (isCurrentUserLocker) {
                showCancelDialog = true
            } else {
                coroutineScope.launch {
                    lockerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val isUserAlreadyApplied = dataSnapshot.children.any {
                                val applicantInfo = it.getValue(ApplicantInfo::class.java)
                                applicantInfo?.user?.studentNum == currentUser?.studentNum
                            }

                            if (isUserAlreadyApplied) {
                                alreadyAppliedDialog = true
                            } else {
                                showDialog = true
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            if (isCurrentUserLocker) Yellow else if (reserved) Color.Gray else DarkBlue,
            disabledContainerColor = if (isCurrentUserLocker) Yellow else Color.Gray
        ),
        shape = RectangleShape,
        enabled = !reserved || isCurrentUserLocker,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        contentPadding = PaddingValues()
    ) {
        Text(
            text = "$lockerNumber",
            color = Color.White
        )
    }

    CustomYesOrNoDialog(
        showAlert = showDialog,
        onConfirm = {
            coroutineScope.launch {
                lockerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val isAlreadyReserved = dataSnapshot.children.any {
                            val applicantInfo = it.getValue(ApplicantInfo::class.java)
                            applicantInfo?.lockerNum == lockerNumber && applicantInfo.lockerLocation == lockerLocation
                        }
                        // 만약 사물함이 아무도 신청되지 않았다면
                        if (!isAlreadyReserved) {
                            coroutineScope.launch {
                                val success = formViewModel.addApplicantToForm(
                                    formViewModel,
                                    selectedForm,
                                    lockerNumber,
                                    lockerLocation
                                )
                                if (success) {
                                    reservedSuccess = true
                                    isCurrentUserLocker = true
                                } else {
                                    reservedFailed = true
                                }
                                reserved = true
                                showDialog = false
                            }
                        } else {
                            reservedFailed = true
                            showDialog = false
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        reservedFailed = true
                        showDialog = false
                    }
                })
            }
        },
        onCancel = {
            showDialog = false
            onRefresh() // Refresh the screen
        },
        title = "${lockerNumber}번 사물함",
        alertMessage = "해당 사물함을 신청하시겠습니까?"
    )

   /* CustomYesOrNoDialog(
        showAlert = showCancelDialog,
        onConfirm = {
            coroutineScope.launch {
                val success = formViewModel.cancelApplicantFromForm(
                    formViewModel,
                    selectedForm,
                    lockerNumber,
                    lockerLocation
                )
                if (success) {
                    reservedSuccess = false
                    isCurrentUserLocker = false
                    reserved = false
                } else {
                    reservedFailed = true
                }
                showCancelDialog = false
                onRefresh()
            }
        },
        onCancel = {
            showCancelDialog = false
        },
        title = "${lockerNumber}번 사물함",
        alertMessage = "해당 사물함 신청을 취소하시겠습니까?"
    )*/

    CustomAlertDialog(
        showAlert = reservedSuccess,
        onDismiss = {
            reservedSuccess = false
            onRefresh()
        },
        alertMessage = "${lockerNumber}번 사물함 신청을 성공하였습니다.",
    )

    CustomAlertDialog(
        showAlert = reservedFailed,
        onDismiss = {
            reservedFailed = false
            onRefresh()
        },
        alertMessage = "이미 신청된 사물함입니다.\n다른 사물함을 신청해주세요"
    )

    CustomAlertDialog(
        showAlert = alreadyAppliedDialog,
        onDismiss = {
            alreadyAppliedDialog = false
            //onRefresh()
        },
        alertMessage = "이미 사물함을 신청하셨습니다.\n하나의 사물함만 신청 가능합니다."
    )
}


@Preview(showBackground = true)
@Composable
fun PreviewLockerDisplayScreen() {
    val navController = rememberNavController()
    val viewModel = FormViewModel()
    val formId: String = ""
    LockerDisplayScreen(navController, viewModel, formId)
}