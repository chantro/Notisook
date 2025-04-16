package com.example.fdea.ui.home

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fdea.login.UserService
import com.example.fdea.ui.BoardViewModel
import com.example.fdea.ui.MainViewModel
import com.example.fdea.ui.home.PostCard
import com.example.fdea.ui.setting.MyTopBar


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RecommendedNoticeScreen(navController: NavHostController, viewModel:MainViewModel,boardViewModel:BoardViewModel) {
    val userSimilarPosts by viewModel.userSimilarPosts.collectAsState()
    val contentSimilarPosts by viewModel.contentSimilarPosts.collectAsState()

    val recommendedPosts = userSimilarPosts + contentSimilarPosts

    LaunchedEffect(Unit) {
        viewModel.fetchRecommendedPosts()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            MyTopBar(navController = navController, title = "추천 공지사항")
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
            ) {
                items(recommendedPosts){ post ->
                    PostCard(
                        navController,
                        post,
                        boardViewModel,
                        true
                    )
                }
            }
        }
    }
}
