package com.example.fdea.ui.setting

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.fdea.R
import com.example.fdea.ui.CertificateViewModel
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.DarkGray

@Composable
fun CertificateScreen(navController: NavController, viewModel: CertificateViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
    ) {
        Column {
            CertificateTopBar(navController)
            CertificateContent(viewModel)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateTopBar(navController: NavController) {
    SmallTopAppBar(
        title = { Text("", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = DarkBlue
        ),
        modifier = Modifier.shadow(10.dp) // Elevation을 별도의 modifier로 적용
    )
}

@Composable
fun CertificateContent(viewModel: CertificateViewModel) {
    var showDialog by remember { mutableStateOf(false) }  //이미지 크게보기
    var showDialog2 by remember { mutableStateOf(false) }  // 증명서 추가하기


    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "나의 전자 증명서",
            color = Color.White,
            fontSize = 25.sp,
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),

            ) {
            Column(
                modifier = Modifier.padding(50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "재학 증명 캡처",
                    color = Color.Black,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 17.dp)
                )
                Text(
                    "등록 날짜 : ${viewModel.registrationDate.value}",
                    color = Color.Black,
                    fontSize = 23.sp,
                    modifier = Modifier.padding(bottom = 25.dp)
                )

                Image(
                    painter = rememberImagePainter(viewModel.imageUri.value),
                    contentDescription = "증명서 이미지",
                    modifier = Modifier
                        .height(75.dp)
                        .fillMaxWidth()
                )
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        "이미지 크게 보기",
                        textDecoration = TextDecoration.Underline,
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                }
                if (showDialog) {
                    ImageZoomDialog(
                        showDialog = showDialog,
                        onDismiss = { showDialog = false },
                        imageUri = viewModel.imageUri.value,
                        DpSize(300.dp, 300.dp), 2
                    )
                }
            }

        }
        Button(
            onClick = {
                showDialog2 = true
            },
            modifier = Modifier.padding(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "재학 증명서 캡처",
                    modifier = Modifier
                        .size(75.dp)
                        .padding(bottom = 10.dp)
                )
                Text(
                    "증명서 추가하기",
                    color = Color.White,
                    fontSize = 23.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(bottom = 25.dp)
                )
            }
        }
        if (showDialog2) {
            RegisterCertificateDialog(viewModel, onDismiss = { showDialog2 = false })
        }
    }
}

@Composable
fun RegisterCertificateDialog(
    viewModel: CertificateViewModel,
    onDismiss: () -> Unit/*,onRegister: () -> Unit*/
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog3 by remember { mutableStateOf(false) }  // 업로드한 증명서 등록하기
    var showDialog4 by remember { mutableStateOf(false) }  // 제대로 인식하지 못했을 때 다이얼로그
    var showDialog5 by remember { mutableStateOf(false) }  // 제대로 인식했을 떄 다이얼로그
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
        }
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(DarkBlue),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 30.dp,
                    end = 20.dp,
                    bottom = 30.dp
                )
            ) {

                Text(
                    text = "이미지 업로드",
                    color = Color.Black,
                    fontSize = 25.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                //이미지 화면에 업로드
                imageUri?.let {
                    Image(
                        painter = rememberImagePainter(it),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
                //업로드, 등록 버튼
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGray)
                    ) {
                        Text("업로드하기", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Button(
                        onClick = {
                            viewModel.registerCertificate() // Ai로 검사하는 함수 - CertificationViewModel에 있음
                            showDialog3 = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGray)

                    ) {
                        Text("등록하기", color = Color.White)
                    }

                    /*// 등록 중에 로딩 다이얼로그 표시
                    LoadingDialog(
                        showDialog = showDialog3,
                        text = "AI가 올바른 이미지인지\n검사하는 중이에요!",
                        onDismiss = { showDialog3 = false })*/

                    //업로드 실패 다이얼로그 - showDialog4로 바꿔야함
                    /*ErrorAndSuccessMessageDialog(
                        showDialog3, onDismiss = { showDialog3 = false },
                        "제대로 인식하지 못했어요!\n올바른 이미지가 아니거나 뚜렷한 이미지가 아닐 수 있습니다.",
                        R.drawable.ic_error_outline)*/

                    //업로드 성공 다이얼로그 - showDialog5로 바꿔야함.
                    ErrorAndSuccessMessageDialog(
                        showDialog3, onDismiss = { showDialog3 = false },
                        "업로드 완료 :)", R.drawable.ic_face)

                    // TODO 예를 들어, 등록 작업이 완료되면:
                    LaunchedEffect(viewModel.isRegistered) {
                        if (viewModel.isRegistered.value) {
                            showDialog3 = false  // 로딩 다이얼로그 숨기기
                            // TODO 등록 성공 알림 등 추가 작업
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun ErrorAndSuccessMessageDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    text: String,
    icon: Int
) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = text,
                        color = Color.Black,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = "Notification Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(60.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(DarkBlue)
                        ) {
                            Text("확인", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingDialog(showDialog: Boolean, text: String, onDismiss: () -> Unit) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.size(300.dp, 260.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        color = Color.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    CircularProgressIndicator(
                        color = DarkBlue,
                        modifier = Modifier.size(50.dp),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun ImageZoomDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    imageUri: Uri?,
    dialogSize: DpSize,
    flag: Int
) {
    if (showDialog) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Box(
                modifier = Modifier
                    .size(dialogSize) // 다이얼로그 크기를 300dp x 300dp로 설정
                    .background(Color.White)
            ) {
                if (flag == 1) {
                    ZoomableImage(
                        imageUri = imageUri,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                } else if (flag == 2) {
                    Image(
                        painter = rememberImagePainter(data = imageUri),
                        contentDescription = "Zoomed Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                }

                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(50.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewCertificateScreen() {
    val certificateViewModel: CertificateViewModel = CertificateViewModel()
    val navController = rememberNavController()
    CertificateScreen(navController = navController, certificateViewModel)
}