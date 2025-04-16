package com.example.fdea.ui.alarm
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

fun subscribeToUserNotifications() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    currentUser?.let {
        val uid = it.uid
        FirebaseMessaging.getInstance().subscribeToTopic("recommendUpdates_$uid")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "유저 추천 업데이트 토픽 구독 성공")
                } else {
                    Log.d("FCM", "유저 추천 업데이트 토픽 구독 실패")
                }
            }
    }
}