package com.example.fdea.ui.notice
import java.text.SimpleDateFormat
import java.util.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.fdea.R
import com.example.fdea.data.Post
import com.example.fdea.login.Auth
import com.example.fdea.login.UserService
import com.example.fdea.ui.BoardViewModel
import com.example.fdea.ui.form.CustomYesOrNoDialog
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.search.SearchTags
import com.example.fdea.ui.setting.CalendarAddDialog
import com.example.fdea.ui.setting.MyTopBar
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.DarkGray
import com.example.fdea.ui.theme.Yellow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoticeDetailScreen(
    navController: NavHostController,
    postId: String,
    mainViewModel: MainViewModel,
    boardViewModel: BoardViewModel,
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var isScrapped by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val comments by boardViewModel.comments.collectAsState()
    var commentContent by remember { mutableStateOf("") }
    //다이얼로그 변수
    var showScrapDialog by remember { mutableStateOf(false) }
    var showAlreadyScrappedDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showCalendarSelectionDialog by remember { mutableStateOf(false) }
    val selectedCalendars = remember { mutableStateListOf<String>() }

    val post by mainViewModel.post.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()   //댓글 달면 댓글 목록의 하단으로 스크롤되게
    var isLoading by remember { mutableStateOf(true) }
    val scrapCount by boardViewModel.scrapCount.collectAsState()
    val viewCount by boardViewModel.viewCount.collectAsState()


    LaunchedEffect(postId) {
        isLoading = true // 작업 시작 전 로딩 상태를 true로 설정
        try {
            // 게시물 정보 가져옴
            mainViewModel.getPostById(postId)
            boardViewModel.loadPostDetails(Post(id = postId))
            boardViewModel.incrementViewCount(postId)
        } finally {
            isLoading = false // 모든 작업이 완료되면 로딩 상태를 false로 변경
        }
    }

    LaunchedEffect(key1 = post) {
        post?.let { post ->
            mainViewModel.markPostAsViewed(post)
            if (post.category != "snowboard") {
                boardViewModel.loadComments(post)
            }
            isScrapped = boardViewModel.isScrapped(post)
        }
    }

    val currentPost = post

    if (isLoading) {
        // 로딩 중일 때 로딩 스피너를 표시
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (currentPost == null) {
        // post가 null인 경우 알림 메시지를 보여줌
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("알림") },
            text = { Text("게시물이 삭제되었습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            boardViewModel.removeScrap(postId)
                            navController.popBackStack() // 이전 페이지로 이동
                        }
                    }
                ) {
                    Text("닫기")
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                Log.d("difdf","$currentRoute")
                MyTopBar(navController,"공지사항")

            },
            bottomBar = {
            if (currentPost.siteName != "스노우보드")
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        OutlinedTextField(
                            value = commentContent,
                            onValueChange = { commentContent = it },
                            label = { Text("댓글을 입력하세요.") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = DarkGray,  // 포커스가 있는 상태의 테두리 색상
                                unfocusedBorderColor = DarkGray, // 포커스가 없는 상태의 테두리 색상
                                focusedLabelColor = Color.Black, // 포커스가 있는 상태의 레이블 색상
                                cursorColor = Color.Black // 커서 색상
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (commentContent.isNotBlank()) {
                                            boardViewModel.addComment(currentPost, commentContent)
                                            commentContent = ""  // 입력 필드 초기화
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_send),
                                        contentDescription = "댓글 달기",
                                        tint = DarkBlue,
                                        modifier = Modifier.size(24.dp)  // 크기 조정
                                    )
                                }
                            }
                        )

                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState
            ) {
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color.LightGray
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "스크랩 수: $scrapCount",
                                fontSize = 16.sp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "조회 수: $viewCount",
                                fontSize = 16.sp,
                            )
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (boardViewModel.isScrapped(currentPost)) {
                                        showAlreadyScrappedDialog = true
                                    } else {
                                        showScrapDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 100.dp),  // 버튼의 최소 크기 설정
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                        ) {
                            Icon(
                                imageVector = if (isScrapped) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "스크랩하기",
                                tint = Yellow
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("스크랩하기", color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    WebViewContent(currentPost.url)
                    Spacer(modifier = Modifier.height(15.dp))
                    // Keywords를 해시태그 형태로 버튼처럼 표시
                    if (currentPost.keywords.isNotEmpty()) {
                        SearchTags(tags = currentPost.keywords, navController = navController)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (currentPost.siteName != "스노우보드") {
                        Text(
                            text = "댓글",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = Color.LightGray
                        )
                    }
                }
                if (currentPost.siteName != "스노우보드") {
                    items(comments) { comment ->
                        var expanded by remember { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "익명",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,

                                        )
                                    Text(
                                        text = "${comment.content}",
                                        fontSize = 15.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(start = 20.dp)
                                    )
                                    Text(
                                        text = comment.date,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                // 현재 로그인한 사용자가 댓글 작성자와 같은 경우에만 드롭다운 메뉴 보여줌
                                if (comment.userId == userId) {
                                    Box {// 댓글 삭제 드롭박스
                                        IconButton(onClick = { expanded = !expanded }) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "더보기",
                                                tint = DarkBlue
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("삭제") },
                                                onClick = {
                                                    boardViewModel.deleteComment(
                                                        currentPost,
                                                        comment
                                                    )
                                                    expanded = false
                                                    Toast.makeText(
                                                        context,
                                                        "댓글이 삭제되었습니다.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = Color.LightGray)
                        }
                    }
                }
            }

            //스크랩 여부 다이얼로그
            CustomYesOrNoDialog(
                showAlert = showScrapDialog,
                onConfirm = {
                    coroutineScope.launch {
                        mainViewModel.updateUserKeywords(currentPost.keywords)
                        boardViewModel.addScrap(currentPost)
                        boardViewModel.incrementScrapCount(currentPost.id)
                        isScrapped = true
                        showScrapDialog = false
                        showCalendarSelectionDialog = true
                        Toast.makeText(context, "이 글을 스크랩하였습니다.", Toast.LENGTH_SHORT).show()

                    }
                },
                onCancel = { showScrapDialog = false },
                title = "스크랩하기",
                alertMessage = "이 게시물을 스크랩하시겠습니까?"
            )
            // 캘린더 선택 다이얼로그
            CalendarSelectionDialog(
                showDialog = showCalendarSelectionDialog,
                onConfirm = { selectedCalendarsList ->
                    selectedCalendars.clear()
                    selectedCalendars.addAll(selectedCalendarsList)
                    showCalendarDialog = true // 캘린더 다이얼로그 표시
                    showCalendarSelectionDialog = false
                },
                onCancel = { showCalendarSelectionDialog = false },context
            )

            //캘린더 추가 여부 다이얼로그
            post?.let {
                CalendarAddDialog(
                    showDialog = showCalendarDialog, it,
                    onConfirm = { startDate, endDate ->
                        coroutineScope.launch {
                            if (selectedCalendars.contains("앱 내 캘린더")) {
                                boardViewModel.addEventToCalendar(
                                    currentPost,
                                    mainViewModel,
                                    startDate,
                                    endDate
                                )
                            }
                            val title = currentPost.title
                            val location = "Some location" // 위치 예시
                            val description = "Some description" // 설명 예시

                            // 캘린더에 일정 추가
                            if (selectedCalendars.contains("기기 내 캘린더")) {
                                addEventToSamsungCalendar(
                                    title = title,
                                    location = location,
                                    description = description,
                                    startDate = startDate,
                                    endDate = endDate,
                                    context = context
                                )
                            }
                            Toast.makeText(context, "캘린더에 추가되었습니다", Toast.LENGTH_SHORT).show()
                            showCalendarDialog = false // 다이얼로그 닫기
                        }
                    },
                    onCancel = { showCalendarDialog = false },
                    title = "캘린더 추가"
                )
            }

            CustomYesOrNoDialog(
                showAlert = showAlreadyScrappedDialog,
                onConfirm = {
                    coroutineScope.launch {
                        boardViewModel.removeScrap(currentPost.id)
                        boardViewModel.decrementScrapCount(currentPost.id)
                        isScrapped = false
                        showAlreadyScrappedDialog = false
                        Toast.makeText(context, "스크랩이 취소되었습니다.", Toast.LENGTH_SHORT).show()

                    }
                },
                onCancel = { showAlreadyScrappedDialog = false },
                title = "스크랩 취소",
                alertMessage = "스크랩을 취소하시겠습니까?"
            )





        }

    }
}

// 1. 캘린더 선택 다이얼로그 추가
@Composable
fun CalendarSelectionDialog(
    showDialog: Boolean,
    onConfirm: (selectedCalendars: List<String>) -> Unit,
    onCancel: () -> Unit,
    context: Context
) {
    if (showDialog) {
        val calendarOptions = listOf("앱 내 캘린더", "기기 내 캘린더")
        val selectedCalendars = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text(text = "캘린더 선택") },
            text = {
                Column {
                    calendarOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedCalendars.contains(option),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedCalendars.add(option)
                                    } else {
                                        selectedCalendars.remove(option)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedCalendars.isNotEmpty()) {
                            onConfirm(selectedCalendars)
                        } else {
                           // Toast.makeText(LocalContext.current, "캘린더를 선택해주세요", Toast.LENGTH_SHORT).show()
                            Toast.makeText(context, "캘린더를 선택해주세요", Toast.LENGTH_SHORT).show()
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
fun addEventToSamsungCalendar(
    title: String,
    location: String,
    description: String,
    startDate: String,
    endDate: String,
    context: Context
) {
    Log.d("ddid", "시작 ; $startDate $endDate")

    // SimpleDateFormat을 사용하여 String을 Date로 변환
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 날짜 형식에 맞게 설정
    val startDateObj: Date? = dateFormat.parse(startDate)
    val endDateObj: Date? = dateFormat.parse(endDate)

    if (startDateObj != null && endDateObj != null) {
        // Date 객체를 밀리초 단위로 변환
        val startDateMillis = startDateObj.time
        val endDateMillis = endDateObj.time

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title) // 일정 제목
            putExtra(CalendarContract.Events.EVENT_LOCATION, location) // 일정 위치
            putExtra(CalendarContract.Events.DESCRIPTION, description) // 일정 설명
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDateMillis) // 시작 시간 (Long)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDateMillis) // 종료 시간 (Long)
            putExtra(CalendarContract.Events.ALL_DAY, false) // 종일 여부
        }

        // 캘린더 앱이 있는지 확인하고 실행
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "캘린더 앱을 찾을 수 없습니다. \n어플 내 캘린더에 날짜가 추가되었습니다.", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "날짜 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show()
    }
}
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContent(url: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()

                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar2(
    navController: NavHostController, toastMessage: String, title: String, major: String,
    onDeleteAllClicked: () -> Unit, onViewFirstComeFirstServedList: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val auth = Auth(context)
    val userMajor by UserService.major.collectAsState()
    var role by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UserService.loadUserData()
    }

    LaunchedEffect(user) {
        user?.let {
            role = auth.getRole(it)
            role="FA"  //TODO role 임의 설정
            Log.d("role", role.toString())
        }
    }

    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 30.sp
            )
        },
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "뒤로 가기",
                    tint = Color.Black,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        },
        actions = {
            if ((role.equals("FA") && major == "FA") || (role.equals("DA") && major == userMajor)) {
                Box {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.Black
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = {
                                onDeleteAllClicked()
                                expanded = false
                                navController.popBackStack()  // 삭제 후 이전 화면으로 자동 이동
                                Toast.makeText(
                                    context,
                                    "${toastMessage}이 삭제되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        if (title == "폼 신청") {
                            DropdownMenuItem(
                                text = { Text("선착순 목록") },
                                onClick = {
                                    onViewFirstComeFirstServedList() // 선착순 목록 보기 함수 호출
                                    expanded = false

                                }
                            )
                        }
                    }
                }
            }
        }
    )
    HorizontalDivider()
}

