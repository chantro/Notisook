package com.example.fdea.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.fdea.data.IdToken
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.suspendCoroutine

class Auth(private val context: Context) {

    //로그인 상태 여부
    suspend fun LogIdToken(currentUser: FirebaseUser): String? {
        return suspendCoroutine { continuation ->
            if (currentUser != null) {
                currentUser.getIdToken(true)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val idToken = task.result?.token
                            Log.d("IdToken", idToken.toString())
                            continuation.resume(idToken)
                        } else {
                            Log.d("IdToken", task.exception.toString())
                            continuation.resume(null)
                        }
                    }
            } else {
                Toast.makeText(context, "현재 로그인 상태가 아닙니다", Toast.LENGTH_SHORT).show()
                continuation.resume(null)
            }
        }
    }


    //api 연동
    suspend fun createTokenApi(idToken: String) : Boolean {
        return suspendCoroutine {continuation ->
            val retrofitClient = RetrofitClient.getRetrofit()
            val tokenApi = retrofitClient.create(TokenApiInterface::class.java)
            val postToken = IdToken()
            var msg: String?
            var success = false

            postToken.idToken = idToken

            tokenApi.postIdToken(postToken).enqueue(object : Callback<IdToken> {
                override fun onResponse(call: Call<IdToken>, response: Response<IdToken>) {
                    if (response.isSuccessful) {
                        msg = response.body()?.msg.toString()
                        success = response.body()?.success.toBoolean()
                    } else {
                        msg = "Request failed with code ${response.code()}"
                        success = false
                    }

                    // 백그라운드 스레드에서 메인 스레드로 전환하여 Toast 표시
                    CoroutineScope(Dispatchers.Main).launch {
                        msg?.let {
                            Log.d("getIdToken", it)
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                    continuation.resume(success)
                }
                //api 연동 실패 시
                override fun onFailure(call: Call<IdToken>, t: Throwable) {
                    msg = t.message.toString()
                    success = false

                    // 백그라운드 스레드에서 메인 스레드로 전환하여 Toast 표시
                    CoroutineScope(Dispatchers.Main).launch {
                        msg?.let {
                            Log.d("getIdToken", it)
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                    continuation.resume(success)
                }
            })
        }
    }

    //id토큰을 받아 api 서비스로 넘기는 함수
    suspend fun verifyToken(currentUser : FirebaseUser){
        return withContext(Dispatchers.IO) {
            val idToken = LogIdToken(currentUser)

            if (idToken != null) {
                val result = createTokenApi(idToken)
                if (result) {
                    //토큰 refresh
                    currentUser.getIdToken(true)
                }
            }
        }
    }

    //관리자인지 아닌지 확인하는 함수
    suspend fun getRole(currentUser : FirebaseUser) : String? = suspendCancellableCoroutine { continuation ->
            if (currentUser != null) {
                currentUser.getIdToken(true)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val claims = task.result?.claims
                        if (claims != null) {
                            val role = claims["role"].toString()
                            continuation.resume(role)
                        }else{
                            continuation.resume(null)
                        }
                    } else {
                        Log.d("IdToken", task.exception.toString())
                        continuation.resume(null)
                    }
                }

            } else {
                Toast
                    .makeText(context, "현재 로그인 상태가 아닙니다", Toast.LENGTH_SHORT)
                    .show()
                continuation.resume(null)
            }
        }
}
