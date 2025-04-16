package com.example.fdea.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf

class CertificateViewModel : ViewModel() {
    var registrationDate = mutableStateOf("")
    var imageUri = mutableStateOf<Uri?>(null)
    var isRegistered = mutableStateOf(false) // 등록 상태를 추적하는 상태 변수

    init {
        loadCertificateData()
    }

    private fun loadCertificateData() {
        viewModelScope.launch {
            // 여기에서 데이터베이스 또는 서버에서 데이터를 로드하는 로직 구현
            registrationDate.value = "2024.00.00"  // 예시 데이터
            imageUri.value =
                Uri.parse("android.resource://com.example.fdea/drawable/ic_gallery")  // 예시 URI
        }
    }

    fun registerCertificate() {
        viewModelScope.launch {
            // 등록 로직 구현
            try {
                // 이미지 등록 로직(예: 서버에 업로드)
                Log.d("chae", "$isRegistered")

                isRegistered.value = true // 등록 성공
            } catch (e: Exception) {
                isRegistered.value = false // 등록 실패
            }
        }
    }
}
