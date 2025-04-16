package com.example.fdea.ui.home

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.algolia.search.model.rule.Alternatives
import com.example.fdea.MainActivity
import com.example.fdea.data.Post
import com.example.fdea.data.Schedule
import com.example.fdea.login.UserService
import com.example.fdea.ui.BoardViewModel
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue
import com.example.fdea.ui.theme.SkyBlue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController,
    boardViewModel:BoardViewModel
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var clickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var backPressCount by remember { mutableIntStateOf(0) }
    var backPressJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UserService.loadUserData()
        viewModel.fetchRecommendedPosts()
    }

    BackHandler {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == "registration_complete_screen") {
            navController.navigate("welcome_screen") {
                popUpTo("welcome_screen") { inclusive = true }
            }
        } else {
            if (backPressCount == 1) {
                backPressJob?.cancel()
                navController.popBackStack()
                (context as? MainActivity)?.finish() // 앱 종료
            } else {
                backPressCount++
                Toast.makeText(context, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                backPressJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    backPressCount = 0
                }
            }
        }
    }


    val schedulesForSelectedDate by viewModel.selectedDateSchedules.observeAsState(emptyList())
    val allSchedules by viewModel.allSchedules.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAllSchedules()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            CalendarHeader(
                selectedDate = selectedDate,
                onDateChanged = { newDate ->
                    selectedDate = newDate
                    clickedDate = null
                },
                navController = navController
            )

            CalendarGrid(
                selectedDate = selectedDate,
                clickedDate = clickedDate,
                onDateSelected = { date ->
                    selectedDate = date
                    clickedDate = date
                    showBottomSheet = true
                    viewModel.updateSelectedDate(date)

                },
                onClearClickedDate = { clickedDate = null },
                schedules = allSchedules
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color.Gray
            )
            NoticesBoard(viewModel = viewModel, navController = navController, boardViewModel = boardViewModel)
        }
        //클릭한 날짜의 스케줄 보여지도록하기 - bottomsheet
        if (showBottomSheet) {
            ScheduleBottomSheet(
                closeSheet = { showBottomSheet = false },
                clickedDate = clickedDate,
                events = schedulesForSelectedDate,
                navController = navController
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    closeSheet: () -> Unit,
    clickedDate: LocalDate?,
    events: List<Schedule>,
    navController: NavController,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayDate = clickedDate ?: LocalDate.now()
    val dayOfWeek = displayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()

    ModalBottomSheet(
        onDismissRequest = { closeSheet() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp),
        containerColor = DarkBlue,
        dragHandle = {
            Spacer(
                modifier = Modifier
                    .padding(top = 15.dp, bottom = 25.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .width(50.dp)
                    .height(5.dp)
                    .background(Color.LightGray)

            )
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val maxHeight = maxHeight
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 9.dp, end = 7.dp)
                    .fillMaxWidth()
                    .heightIn(min = 340.dp, max = maxHeight * 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$dayOfWeek ${displayDate.dayOfMonth}",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Divider(
                        color = Color.White,
                        thickness = 3.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(events) { event ->
                        EventRow(event, closeSheet, navController)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TextUnit.toPx(): Float {
    return with(LocalDensity.current) {
        this@toPx.toPx()
    }
}

@Composable
fun EventRow(event: Schedule ,closeSheet: () -> Unit, navController: NavController) {
    val titleFontSize = 19.sp
    var titleHeight by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                closeSheet()
                navController.navigate("notice_detail_screen/${event.id}")
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date information column
            Column(
                modifier = Modifier.width(100.dp)
            ) {
                Text(
                    text = "${event.startDate}~",
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = event.endDate,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            // 계산된 높이로 divider 생성
            VerticalDivider(
                color = Color.White,
                thickness = 2.dp,
                modifier = Modifier
                    .height(with(LocalDensity.current) { titleHeight.toDp() })
            )

            Spacer(modifier = Modifier.width(5.dp))

            //title이 차지하는 세로 공간 길이 재기
            Layout(
                content = {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = titleFontSize
                    )
                }
            ) { measurables, constraints ->
                val titlePlaceable = measurables[0].measure(constraints)
                titleHeight = titlePlaceable.height // Measure the title's height

                layout(width = titlePlaceable.width, height = titlePlaceable.height) {
                    titlePlaceable.placeRelative(0, 0)
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NoticesBoard(viewModel: MainViewModel, navController: NavController,boardViewModel: BoardViewModel) {
    val userSimilarPosts by viewModel.userSimilarPosts.collectAsState()
    val contentSimilarPosts by viewModel.contentSimilarPosts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBlue)
            ) {
                RecommendationSection(
                    title = "나와 비슷한 사용자들이 읽은 글이에요!",
                    posts = userSimilarPosts,
                    navController = navController,
                    boardViewModel = boardViewModel
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBlue)
            ) {
                RecommendationSection(
                    title = "최근 스크랩한 공지사항과 관련된 글",
                    posts = contentSimilarPosts,
                    navController = navController,
                    boardViewModel = boardViewModel
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecommendationSection(title: String, posts: List<Post>, navController: NavController,boardViewModel: BoardViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    navController.navigate("recommended_notice_screen")
                }
            ) {
                Text(
                    text = "전체보기",
                    color = SkyBlue,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Column {
            posts.take(3).forEach { post ->
                PostCard(
                    navController = navController,
                    post = post,
                    boardViewModel = boardViewModel,
                    true
                )
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostCard(
    navController: NavController,
    post: Post,
    boardViewModel: BoardViewModel,
    showSiteName: Boolean = false
) {
    val currentDate = System.currentTimeMillis()
    var isScrapped by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current


    // 스크랩 상태를 초기화
    LaunchedEffect(post) {
        isScrapped = boardViewModel.isScrapped(post)
    }

    // 사이트별 색상 맵 설정
    val siteColorMap = mapOf(
        "스노위" to Pair(Color(0xFF699BF7), Color(0xB3B3D4FC)),
        "에브리타임" to Pair(Color(0xFF68A567), Color(0xB3D2E8D1)),
        "스노웨이" to Pair(Color(0xFFD17CFC), Color(0xB3F3E3FC)),
        "스노우보드" to Pair(Color(0xFFE57373), Color(0xB3FFCDD2))
    )
    val siteColors = siteColorMap[post.siteName] ?: Pair(Color(0xFF699BF7), Color(0xFFE1EBFF)) // 기본 색상


    val daysLeft = post.deadline?.let { deadline ->
        val deadlineDate = Instant.ofEpochMilli(deadline.toDate().time).atZone(ZoneId.systemDefault()).toLocalDate()
        val currentDateOnly = Instant.ofEpochMilli(currentDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val daysRemaining = ChronoUnit.DAYS.between(currentDateOnly, deadlineDate)

        when {
            daysRemaining > 0 -> "D-$daysRemaining"
            daysRemaining.toInt() == 0 -> "D-Day"
            else -> "만료"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFEBEBEB).copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                navController.navigate("notice_detail_screen/${post.id}") {
                    popUpTo("notice_detail_screen") { inclusive = true }
                    launchSingleTop = true
                }


            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSiteName) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(siteColors.second)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = post.siteName,
                            color = siteColors.first,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }

                if (post.category.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEBEBEB))
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = post.category,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                }

                if (daysLeft != null) {
                    Text(
                        text = daysLeft,
                        color = when {
                            daysLeft == "D-Day" || daysLeft == "D-1" || daysLeft == "D-2" || daysLeft == "D-3" -> Color(0xFFFF7368)
                            else -> Color(0xFF333333)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // 스크랩 아이콘 추가
                Icon(
                    imageVector = if (isScrapped) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isScrapped) "스크랩됨" else "스크랩",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            coroutineScope.launch {
                                if (isScrapped) {
                                    boardViewModel.removeScrap(post.id)
                                    boardViewModel.decrementScrapCount(post.id)
                                    Toast.makeText(context, "스크랩이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    boardViewModel.addScrap(post)
                                    boardViewModel.incrementScrapCount(post.id)
                                    Toast.makeText(context, "스크랩하였습니다.", Toast.LENGTH_SHORT).show()
                                }
                                isScrapped = !isScrapped
                            }
                        },
                    tint = if (isScrapped) Color(0xFFFF7368) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.title,
                fontWeight = FontWeight.SemiBold,
                color = if (!post.isViewed) Color(0xFF333333) else Color.LightGray,
                fontSize = 16.sp
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarHeader(
    selectedDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            onDateChanged(selectedDate.minusMonths(1))
        }) {
            Icon(Icons.Filled.ArrowBack, "Previous Month")
        }
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = {
            onDateChanged(selectedDate.plusMonths(1))
        }) {
            Icon(Icons.Filled.ArrowForward, "Next Month")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarGrid(
    selectedDate: LocalDate,
    clickedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onClearClickedDate: () -> Unit,
    schedules: List<Schedule>
) {
    val yearMonth = YearMonth.from(selectedDate)
    val totalDays = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysOffset = firstDayOfMonth.dayOfWeek.value % 7

    Column {
        WeekDaysHeader()
        LazyColumn {
            items((0 until 6).toList()) { week ->
                WeekRow(
                    week,
                    daysOffset,
                    totalDays,
                    clickedDate,
                    yearMonth,
                    onDateSelected,
                    schedules
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeekDaysHeader() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val daysOfWeek = DayOfWeek.values().toList().let {
            it.subList(6, it.size) + it.subList(0, 6)
        }

        items(daysOfWeek) { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier
                    .padding(4.dp)
                    .width(40.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeekRow(
    week: Int,
    daysOffset: Int,
    totalDays: Int,
    clickedDate: LocalDate?,
    yearMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    schedules: List<Schedule>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        (0 until 7).forEach { dayOfWeek ->
            val dayOfMonth = week * 7 + dayOfWeek - daysOffset + 1
            if (dayOfMonth in 1..totalDays) {
                val date = yearMonth.atDay(dayOfMonth)
                val isToday = LocalDate.now() == date
                DateBox(
                    dayOfMonth,
                    yearMonth,
                    onDateSelected,
                    clickedDate,
                    isToday,
                    schedules
                )
            } else {
                EmptyBox()
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateBox(
    dayOfMonth: Int,
    yearMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    clickedDate: LocalDate?,
    isToday: Boolean,
    schedules: List<Schedule>
) {
    val date = yearMonth.atDay(dayOfMonth)
    val mySchedules = schedules.filter {
        val startDate = LocalDate.parse(it.startDate)
        val endDate = LocalDate.parse(it.endDate)
        date in startDate..endDate
    }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(35.dp)
            .background(
                if (clickedDate?.isEqual(date) == true)
                    LightBlue.copy(alpha = 0.7f)
                else Color.Transparent, shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (isToday) LightBlue else Color.Transparent,
                shape = CircleShape
            )
            .clickable {
                onDateSelected(date)

            },
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(
                text = dayOfMonth.toString(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                mySchedules.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(LightBlue, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
   

        }
    }
}

@Composable
fun EmptyBox() {
    Spacer(
        modifier = Modifier
            .padding(8.dp)
            .size(35.dp)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    val viewModel = MainViewModel()
    val boardViewModel=BoardViewModel()

    HomeScreen(
        viewModel = viewModel,
        navController = navController,
        boardViewModel = boardViewModel

    )
}