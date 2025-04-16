package com.example.fdea.ui.notice
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.R
import com.example.fdea.ui.BoardViewModel
import com.example.fdea.ui.home.PostCard
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.setting.MyTopBar
import com.example.fdea.ui.theme.LightBlue
import kotlinx.coroutines.flow.filter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NoticeScreen(viewModel: MainViewModel, navController: NavController,boardViewModel:BoardViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSubCategory by viewModel.selectedSubCategory.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isFetchingPosts by viewModel.isFetchingPosts.collectAsState()

    val scrollStateKey = "$selectedCategory:$selectedSubCategory:$sortOrder"
    val listState = rememberLazyListState()
    val scrollPositionKey = "SCROLL_POSITION_$scrollStateKey"
    val savedScrollPosition = rememberSaveable(key = scrollPositionKey) { mutableIntStateOf(0) }

    val koreaZoneId = ZoneId.of("Asia/Seoul")

    val groupedPosts = if (sortOrder == "최신순") {
        posts.groupBy { post ->
            post.uploadTime?.let {
                val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
                formatter.format(it.toDate().toInstant().atZone(koreaZoneId).toLocalDate())
            } ?: "등록일 없음"
        }
    } else {
        posts.groupBy { post ->
            post.deadline?.let {
                val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
                formatter.format(it.toDate().toInstant().atZone(koreaZoneId).toLocalDate())
            } ?: "마감일 없음"
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                savedScrollPosition.intValue = index
            }
    }

    LaunchedEffect(scrollStateKey) {
        listState.scrollToItem(savedScrollPosition.intValue)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filter { it != null && it >= posts.size - 1 }
            .collect {
                viewModel.fetchMorePosts()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MyTopBar(navController = navController, title = "공지사항")

        TopSection(selectedCategory) { category ->
            viewModel.updateSelectedCategory(category)
        }

        if (selectedCategory in listOf("에브리타임", "스노위", "스노우보드")) {
            SubCategorySection(selectedCategory, selectedSubCategory) { subCategory ->
                viewModel.updateSelectedSubCategory(subCategory)
            }
        }

        if (selectedCategory == "스노우보드" && selectedSubCategory !in listOf("공지사항", "기타") ||
            selectedCategory == "스노위" && selectedSubCategory != "사회봉사공고" ||
            selectedCategory == "전체"
        ) {
            SortOrderDropdown(
                selectedSortOrder = sortOrder,
                onSortOrderSelected = { newSortOrder ->
                    viewModel.updateSortOrder(newSortOrder)
                }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (sortOrder == "인기순") {
                // 인기순일 때 개별 날짜를 각 게시물 위에 표시
                items(posts) { post ->
                    val date = post.uploadTime?.let {
                        val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
                        formatter.format(it.toDate().toInstant().atZone(koreaZoneId).toLocalDate())
                    } ?: "등록일 없음"

                    // 개별 날짜 텍스트
                    Text(
                        text = date,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 게시물 카드
                    PostCard(
                        navController = navController,
                        post = post,
                        boardViewModel = boardViewModel,
                        showSiteName = selectedCategory == "전체"
                    )
                }
            } else {
                // 최신순이나 마감일순일 때 기존 그룹화 방식 유지
                groupedPosts.forEach { (date, posts) ->
                    if (sortOrder != "인기순" && date != "마감일 없음") {
                        item {
                            Text(
                                text = date,
                                color = Color(0xFF333333),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    items(posts) { post ->
                        PostCard(
                            navController = navController,
                            post = post,
                            boardViewModel,
                            showSiteName = selectedCategory == "전체"
                        )
                    }
                }

                if (isFetchingPosts) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortOrderDropdown(selectedSortOrder: String, onSortOrderSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }  // 드롭다운이 열려 있는지 여부를 관리
    val sortOptions = listOf("최신순", "마감일순", "인기순")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd)  // 드롭다운 메뉴 위치 설정
    ) {
        TextButton(
            onClick = { expanded = true },  // 버튼을 클릭하면 드롭다운이 열리도록 설정
            modifier = Modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = selectedSortOrder, color = Color(0xFF555555), fontSize = 12.sp)
                // 아래로 향하는 화살표 아이콘 추가
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Icon",
                    tint = Color(0xFF555555)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },  // 외부를 클릭하면 드롭다운이 닫히도록 설정
            modifier = Modifier
                .background(Color.White)
        ) {
            sortOptions.forEach { sortOption ->
                DropdownMenuItem(
                    text = { Text(sortOption, color = Color(0xFF333333)) },
                    onClick = {
                        onSortOrderSelected(sortOption)  // 선택된 정렬 기준 업데이트
                        expanded = false  // 항목 선택 후 드롭다운 닫기
                    }
                )
            }
        }
    }
}

@Composable
fun TopSection(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("전체", "스노우보드", "스노위", "스노웨이", "에브리타임").forEach { category ->
            CategoryItem(
                name = category,
                iconRes = when(category) {
                    "스노우보드" -> R.drawable.ic_snowboard
                    "스노위" -> R.drawable.ic_snowe
                    "스노웨이" -> R.drawable.ic_snoway
                    "에브리타임" -> R.drawable.ic_everytime
                    else -> R.drawable.ic_view_all // 기본 아이콘
                },
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun SubCategorySection(
    selectedCategory: String,
    selectedSubCategory: String,
    onSubCategorySelected: (String) -> Unit
) {
    val subCategories = when (selectedCategory) {
        "에브리타임" -> listOf("전체", "자유게시판", "정보게시판", "홍보게시판", "동아리,학회")
        "스노우보드" -> listOf("전체", "퀴즈","강의","공지사항","과제","기타")
        "스노위" -> listOf("전체", "공지사항", "교내채용공고", "외부기관공고", "사회봉사공고", "취업경력개발")
        else -> emptyList()
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(subCategories) { subCategory ->
            SubCategoryItem(
                name = subCategory,
                isSelected = selectedSubCategory == subCategory,
                onClick = { onSubCategorySelected(subCategory) }
            )
        }
    }
}

@Composable
fun CategoryItem(name: String, iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF699BF7) else Color(0xFFBAB8B9) // 선택 시 주황색, 비선택 시 회색
    val textColor = if (isSelected) Color(0xFF333333) else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(width = 3.dp, color = borderColor, shape = CircleShape) // 테두리 추가
                .padding(1.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

    }
}

@Composable
fun SubCategoryItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF699BF7) else Color(0xFFEBEBEB)
    val textColor = if (isSelected) Color.White else Color(0xFF333333)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(
            text = name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PaginationControls(totalPages: Int, currentPage: Int, onPageSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        for (page in 1..totalPages) {
            Button(
                onClick = { onPageSelected(page) },
                colors = ButtonDefaults.buttonColors(

                    containerColor = Color.Transparent,
                    contentColor = if (page == currentPage) LightBlue else Color.Black
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(30.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = page.toString(), style = TextStyle(fontSize = 19.sp))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PreviewNoticeScreen() {
    val navController = rememberNavController()
    val viewModel = MainViewModel()
    val boardViewModel=BoardViewModel()
    NoticeScreen(viewModel,navController,boardViewModel)
}
