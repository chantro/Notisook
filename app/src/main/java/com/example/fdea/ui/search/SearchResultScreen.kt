package com.example.fdea.ui.search
import java.util.Date
import java.util.concurrent.TimeUnit
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.algolia.search.client.Index
import com.algolia.search.model.Attribute
import com.algolia.search.model.search.Query
import com.algolia.search.model.search.QueryType
import com.example.fdea.data.Post
import com.example.fdea.ui.BoardViewModel
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.home.PostCard
import com.example.fdea.ui.notice.PaginationControls
import com.example.fdea.ui.notice.SortOrderDropdown
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(navController: NavController, viewModel: MainViewModel, searchText: String, searchScope: String, algoliaIndex: Index,boardViewModel: BoardViewModel) {

    var searchResults = remember { mutableStateListOf<Post>() }
    val coroutineScope = rememberCoroutineScope()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var searchTextState by remember { mutableStateOf(searchText) }

    // 날짜별로 그룹화된 게시물 저장을 위한 상태
    val groupedPosts = remember { mutableStateMapOf<String, List<Post>>() }

    // 정렬 및 그룹화 로직 추가
    fun sortAndGroupPosts(posts: List<Post>): Map<String, List<Post>> {
        return when (sortOrder) {
            "마감일순" -> {
                val sortedPosts = posts.sortedBy { post ->
                    post.deadline?.seconds?.let { deadlineInSeconds ->
                        val currentTimeInSeconds = Timestamp.now().seconds
                        deadlineInSeconds - currentTimeInSeconds
                    } ?: Long.MAX_VALUE
                }

                // 로그 추가: 마감일순으로 정렬된 게시물 확인
                sortedPosts.forEach { post ->
                    Log.d("SearchResult", "마감일순 Post ID: ${post.id}, deadline: ${post.deadline?.toDate()}")
                }

                sortedPosts.groupBy { "전체" }
            }
            "최신순" -> {
                // 최신순 정렬
                val sortedPosts = posts.sortedByDescending { post ->
                    post.uploadTime?.seconds ?: 0L  // 최신순 정렬
                }

                // 로그 추가: 최신순으로 정렬된 게시물 확인
                sortedPosts.forEach { post ->
                    Log.d("SearchResult", "최신순 Post ID: ${post.id}, uploadTime: ${post.uploadTime?.toDate()}")
                }

                val groupedPosts = sortedPosts.groupBy { post ->
                    post.uploadTime?.let {
                        // 날짜별 그룹화를 위한 포맷
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일")
                        formatter.format(it.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                    } ?: "날짜 미정"
                }

                // 로그 추가: 그룹화된 게시물 확인
                groupedPosts.forEach { (date, posts) ->
                    Log.d("SearchResult222", "그룹: $date, 게시물 수: ${posts.size}")
                    posts.forEach { post ->
                        Log.d("SearchResult222", "그룹에 포함된 Post ID: ${post.id}, uploadTime: ${post.uploadTime?.toDate()}")
                    }
                }

                groupedPosts
            }
            else -> mapOf("전체" to posts)
        }
    }

    // 검색 및 정렬 적용
    LaunchedEffect(searchText, sortOrder) {
        coroutineScope.launch {
            val query = Query(searchText).apply {
                when (searchScope) {
                    "keywords" -> {
                        restrictSearchableAttributes = listOf(Attribute("keywords"))
                        queryType = QueryType.PrefixNone
                    }
                    "title_content" -> {
                        restrictSearchableAttributes = listOf(Attribute("title"), Attribute("content"))
                    }
                }
            }
            val result = algoliaIndex.search(query)
            val fetchedPosts = result.hits.map { jsonToPost(it.json) }

            // 검색된 게시물을 정렬한 후 그룹화된 상태로 저장
            val grouped = sortAndGroupPosts(fetchedPosts)
            groupedPosts.clear()
            groupedPosts.putAll(grouped)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextField(
                value = searchTextState,
                onValueChange = { searchTextState = it },
                placeholder = { Text("검색어를 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchTextState.isNotBlank()) {
                            coroutineScope.launch {
                                val query = Query(searchTextState)
                                val result = algoliaIndex.search(query)
                                val fetchedPosts = result.hits.map { jsonToPost(it.json) }
                                val grouped = sortAndGroupPosts(fetchedPosts)
                                groupedPosts.clear()
                                groupedPosts.putAll(grouped)
                            }
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (searchTextState.isNotBlank()) {
                            coroutineScope.launch {
                                val query = Query(searchTextState)
                                val result = algoliaIndex.search(query)
                                val fetchedPosts = result.hits.map { jsonToPost(it.json) }
                                val grouped = sortAndGroupPosts(fetchedPosts)
                                groupedPosts.clear()
                                groupedPosts.putAll(grouped)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.Blue
                        )
                    }
                }
            )
        }

        // 검색 결과 UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "'$searchTextState'에 대한 검색 결과",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 13.dp)
            )

            SortOrderDropdown(
                selectedSortOrder = sortOrder,
                onSortOrderSelected = { newSortOrder ->
                    viewModel.updateSortOrder(newSortOrder)
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedPosts
                    // 최신순 정렬을 위해 날짜를 기준으로 내림차순으로 정렬
                    .toList()
                    .sortedByDescending { (date, posts) ->
                        posts.maxOfOrNull { post -> post.uploadTime?.seconds ?: 0L }
                    }
                    .forEach { (date, posts) ->
                        // 로그 추가: 날짜별로 그룹화된 게시물 확인
                        Log.d("SearchResult123", "날짜: $date, 게시물 수: ${posts.size}")

                        if (sortOrder == "최신순" && date != "날짜 미정") {
                            // 날짜별로 헤더 표시
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

                        // 로그 추가: LazyColumn에 전달되는 게시물 정보 확인
                        posts.forEach { post ->
                            Log.d("SearchResult123", "LazyColumn에 표시되는 Post ID: ${post.id}, uploadTime: ${post.uploadTime?.toDate()}")
                        }

                        // 게시물 목록 표시
                        items(posts) { post ->
                            PostCard(
                                navController = navController,
                                post = post,
                                boardViewModel,
                                showSiteName = true
                            )
                        }
                    }
            }



        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            PaginationControls(3, 1, onPageSelected = { /* Handle page change */ })
        }
    }
}


fun jsonToPost(json: Map<String, Any?>): Post {
    // Helper function to convert timestamp from _seconds and _nanoseconds to Firebase Timestamp
    fun convertTimestamp(timestamp: Map<String, Any?>?): Timestamp? {
        val seconds = timestamp?.get("_seconds")?.toString()?.toLongOrNull()
        val nanoseconds = timestamp?.get("_nanoseconds")?.toString()?.toIntOrNull() ?: 0
        return if (seconds != null) {
            Timestamp(seconds, nanoseconds)
        } else {
            null
        }
    }

    val post = Post(
        id = json["objectID"]?.toString()?.trim('"') ?: "",
        category = json["category"]?.toString()?.trim('"') ?: "EMPTY",
        content = json["content"]?.toString()?.trim('"') ?: "",
        deadline = convertTimestamp(json["deadline"] as? Map<String, Any?>),
        keywords = (json["keywords"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        scraps = json["scraps"]?.toString()?.toIntOrNull() ?: 0,
        title = json["title"]?.toString()?.trim('"') ?: "",
        uploadTime = convertTimestamp(json["uploadTime"] as? Map<String, Any?>),
        url = json["url"]?.toString()?.trim('"') ?: "",
        views = json["views"]?.toString()?.toIntOrNull() ?: 0,
        writer = json["writer"]?.toString()?.trim('"') ?: "",
        siteName = json["site"]?.toString()?.trim('"') ?: "",
        boardName = json["board"]?.toString()?.trim('"') ?: ""
    )

    // Log post details
    Log.d("SearchResult", "Post 객체: ${post.uploadTime} 데드라인: ${post.deadline}")

    return post
}