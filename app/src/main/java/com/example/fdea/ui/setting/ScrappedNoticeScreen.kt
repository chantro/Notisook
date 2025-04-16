package com.example.fdea.ui.setting


import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.data.Post
import com.example.fdea.ui.BoardViewModel
import com.example.fdea.ui.form.showDatePicker
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.home.PostCard
import com.example.fdea.ui.theme.LightBlue
import com.example.fdea.ui.theme.Yellow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScrappedNoticeScreen(navController: NavController, boardViewModel: BoardViewModel, mainViewModel: MainViewModel) {
    var showConfirmationDialog by remember { mutableStateOf(false) } //캘린더 추가 여부 묻는 다이얼로그 변수
    var showCompletionDialog by remember { mutableStateOf(false) }  //캘린더 추가 완료
    val post by mainViewModel.post.collectAsState()
    val scrappedPosts by boardViewModel.scrappedPosts.collectAsState()
    // 선택된 공지가 있는지 여부
    val hasSelectedPost by boardViewModel.hasSelectedPost.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(hasSelectedPost) {
        Log.d("ButtonState", "hasSelectedPost changed: $hasSelectedPost")  // 항목 선택 시 로그 출력
    }
    LaunchedEffect(Unit) {
        boardViewModel.getScrappedPosts()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        Column {
            MyTopBar(navController, "나의 스크랩")

            ScrappedNoticesList(navController, posts = scrappedPosts, boardViewModel, mainViewModel,)
        }
        Button(
            onClick = {
                Log.d("ButtonClick", "Button clicked")  // 버튼이 클릭되었는지 확인하는 로그 추가
                showConfirmationDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp)
                .width(130.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF699BF7),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
            ),
            enabled = hasSelectedPost.also { Log.d("ButtonState222", "hasSelectedPost222: $hasSelectedPost") }  // 로그 추가
        ) {
            Text("캘린더 추가", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // 캘린더 추가할지 물어봄
        post?.let {
            Log.d("CalendarDialog", "Post is not null, showing CalendarAddDialog")  // 로그 추가
            CalendarAddDialog(
                showDialog = showConfirmationDialog.also { Log.d("CalendarDialog", "showConfirmationDialog: $showConfirmationDialog") },
                post = it,
                onConfirm = { startDate, endDate ->
                    Log.d("CalendarDialog", "onConfirm clicked, startDate: $startDate, endDate: $endDate")  // 로그 추가
                    coroutineScope.launch {
                        boardViewModel.addSelectedPostToCalendar(mainViewModel, startDate, endDate)
                        showConfirmationDialog = false
                        showCompletionDialog = true
                        Log.d("CalendarDialog", "Calendar added successfully")  // 로그 추가
                        Toast.makeText(context, "캘린더에 추가가 완료되었습니다!", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = {
                    Log.d("CalendarDialog", "onCancel clicked")  // 로그 추가
                    showConfirmationDialog = false
                },
                title = "캘린더 추가"
            )
        } ?: Log.d("CalendarDialog", "Post is null, skipping CalendarAddDialog")  // post가 null인 경우

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScrappedNoticesList(navController: NavController,posts: List<Post>, viewModel: BoardViewModel, mainViewModel: MainViewModel) {
    val schedulePostIds by mainViewModel.schedulePostIds.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        items(posts) { post ->
            ScrappedPostsItem(navController, post, viewModel,mainViewModel, schedulePostIds.contains(post.id))
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScrappedPostsItem(navController: NavController,
                      post: Post, viewModel: BoardViewModel,
                      mainViewModel: MainViewModel, isScheduled: Boolean) {
    val selectedPost by viewModel.selectedPost.collectAsState()  // 단일 선택된 포스트 관리
    val isSelected = selectedPost == post  // 현재 포스트가 선택된 포스트인지 여부

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 스케줄에 추가되지 않은 경우 라디오 버튼 표시
        Box(
            modifier = Modifier
                .size(48.dp)
        ) {
            if (!isScheduled) {
                RadioButton(
                    selected = isSelected,
                    onClick = {
                        viewModel.toggleSelection(post)
                        mainViewModel.selectPost(post)

                    },
                    colors = RadioButtonDefaults.colors(selectedColor = LightBlue)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            PostCard(
                navController = navController,
                post = post,
                boardViewModel = viewModel,
                showSiteName = true,
            )
        }
    }
}

@Composable
fun CalendarAddDialog(
    showDialog: Boolean,
    post: Post,
    title: String,
    onConfirm: (startDate: String, endDate: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    if (showDialog) {
        var startDate by remember { mutableStateOf<String?>(null) }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var endDate by remember {
            mutableStateOf(
                post.deadline?.toDate()?.let { dateFormat.format(it) } ?: null
            )
        }

        AlertDialog(
            onDismissRequest = { onCancel() },
            title = {
                Text(text = title)
            },
            text = {
                Column {
                    DatePickerRow(
                        label = "시작 날짜",
                        date = startDate,
                        onDateClick = { showDatePicker(context) { date ->
                            startDate = date  // 종료 날짜는 수정되지 않도록 변경
                        } }
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    DatePickerRow(
                        label = "종료 날짜",
                        date = endDate,
                        onDateClick = { showDatePicker(context) { date ->
                            endDate = date
                        } }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (startDate != null && endDate != null) {
                            val startDateObj = dateFormat.parse(startDate)
                            val endDateObj = dateFormat.parse(endDate)
                            if (endDateObj.before(startDateObj)) {
                                Toast.makeText(context, "종료 날짜는 시작 날짜 이후여야 합니다", Toast.LENGTH_SHORT).show()
                            } else {
                                onConfirm(dateFormat.format(startDateObj), dateFormat.format(endDateObj))
                            }
                        } else {
                            Toast.makeText(context, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                Button(onClick = { onCancel() }) {
                    Text("취소")
                }
            }
        )
    }
}


@Composable
fun DatePickerRow(
    label: String,
    date: String?,
    onDateClick: () -> Unit,
    placeholderDate: String = "날짜 선택",
    myColor: TextFieldColors = TextFieldDefaults.textFieldColors(),
    myModifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(end = 8.dp)
        )
        TextField(
            value = date ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholderDate) },
            leadingIcon = {
                IconButton(onClick = onDateClick) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar Icon")
                }
            },
            colors = myColor,
            textStyle = TextStyle(fontSize = 17.sp),
            singleLine = true,
            modifier = myModifier
                .weight(1f)
                .clickable(onClick = onDateClick)
        )
    }
}
