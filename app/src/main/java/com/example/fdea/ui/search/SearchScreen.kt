package com.example.fdea.ui.search

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.fdea.ui.MainViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: MainViewModel, algoliaIndex: Index) {
    var searchText by remember { mutableStateOf("") }
    val recommendTags by viewModel.recommendTags.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "무엇을 찾으시나요?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Search TextField
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
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
                        if (searchText.isNotBlank()) { // 검색어가 비어 있지 않은 경우에만 검색 실행
                            navController.navigate("search_result_screen/${searchText}/title_content")
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (searchText.isNotBlank()) {
                            navController.navigate("search_result_screen/$searchText/title_content")
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

        Spacer(modifier = Modifier.height(24.dp))

        // Recommended Search Tags
        Text(
            text = "추천 키워드",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        SearchTags(
            tags = recommendTags,
            navController = navController
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchTags(tags: List<String>, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        //verticalAlignment = Alignment.CenterVertically
    ) {
        tags.forEach { tag ->
            Text(
                text = "#$tag",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF0F0F0))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        // 태그 클릭 시 Algolia에서 keywords 필드로 검색
                        coroutineScope.launch {
                            navController.navigate("search_result_screen/${tag}/keywords")
                            // 예: 검색 결과를 NavController를 통해 넘겨주기 위해 navigate를 사용
                        }
                    }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}