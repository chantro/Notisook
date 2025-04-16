package com.example.fdea.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fdea.data.Gifticon
import com.example.fdea.ui.BenefitViewModel
import com.example.fdea.ui.theme.LightBlue

@Composable
fun ImminentGifticonsScreen(navController: NavController, viewModel: BenefitViewModel) {
    var imminentGifticons by remember { mutableStateOf(emptyList<Gifticon>()) }

    LaunchedEffect(Unit) {
        viewModel.getImminentGifticons { gifticons ->
            imminentGifticons = gifticons
        }
    }

    Scaffold(
        topBar = { MyTopBar(navController, "임박한 기프티콘") }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            if (imminentGifticons.isEmpty()) {
            } else {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBlue)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "기프티콘을 쓴 후 꾹 눌러 삭제해주세요!",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    GifticonGrid(imminentGifticons, viewModel = viewModel) { gifticon ->
                        // Update the list after a gifticon is deleted
                        viewModel.removeGifticonImage(gifticon) {
                            imminentGifticons = imminentGifticons.filter { it.id != gifticon.id }
                        }
                    }
                }
            }
        }
    }
}