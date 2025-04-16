package com.example.fdea.login

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var context: Context
    private lateinit var auth : Auth
    private val firestore = FirebaseFirestore.getInstance()
    private val fireStoreAuth = Firebase.auth

    fun init(context: Context) {
        this.context = context
        this.googleSignInClient = GoogleSignInClient(context)
        this.auth = Auth(context)
        if (fireStoreAuth.currentUser != null) { // Firebase 사용자가 로그인된 상태라면
            UserService.loadUserData()  // 사용자 이름 불러오기

            // verifyToken 호출
            viewModelScope.launch {
                auth.verifyToken(fireStoreAuth.currentUser!!)
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    // 현재 로그인한 사용자의 UID로 Firestore에서 users 문서 검사
    suspend fun navigateBasedOnUserData(): String {
        val user = fireStoreAuth.currentUser
        if (user != null) {
            val userDocRef = firestore.collection("users").document(user.uid)
            val docSnapshot = userDocRef.get().await()
            if (docSnapshot.exists()) {
                Auth(context).verifyToken(user)
                // 기존에 tags 관련 검사를 제거하고 바로 home_screen으로 이동
                return "home_screen"
            } else {
                val stringArg = "회원가입"
                return "account_info_screen/$stringArg"
            }
        } else {
            return "welcome_screen"
        }
    }


    fun signOut(onResult: (Boolean) -> Unit) {
        // 비동기 로그아웃을 처리하기 위해 viewModelScope.launch 사용
        viewModelScope.launch {
            try {
                googleSignInClient.signOut { success ->
                    onResult(success)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing out: ${e.localizedMessage}")
                onResult(false)
            }
        }
    }

    //회원 탈퇴 비동기 처리
    fun revokeAccess(onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            try{
                googleSignInClient.revokeAccess{success ->
                    onResult(success)
                }
            }catch(e:Exception){
                Log.e("AuthViewModel", "Error Revoking Access: ${e.localizedMessage}")
                onResult(false)
            }
        }
    }

    fun getGoogleSignInIntent() = googleSignInClient.getSignInIntent()

    fun handleSignInResult(data: Intent?, onResult: (Boolean, String?) -> Unit) {
        // handleSignInResult 메서드는 suspend 함수이므로 코루틴에서 실행
        viewModelScope.launch {
            googleSignInClient.handleSignInResult(data, onResult)
        }
    }

}