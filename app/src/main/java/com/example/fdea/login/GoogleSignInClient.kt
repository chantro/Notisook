package com.example.fdea.login

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fdea.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class GoogleSignInClient(context: Context) {
    private val googleSignInClient: GoogleSignInClient
    private val auth: Auth
    //private val firebaseAuth = FirebaseAuth.getInstance()

    init {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, options)
        auth = Auth(context)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent


    private suspend fun getRole(user: FirebaseUser):String{
        val role = auth.getRole(user)
        if(role.equals("FA")){
            return "총 학생회님"
        }
        else if(role.equals("DA")){
            return "학부 학생회님"
        }
        else if(role.equals("M")){
            return user.email.toString()
        }
        else{
            return "권한이 없습니다."
        }
    }

    suspend fun handleSignInResult(data: Intent?, onResult: (Boolean, String?) -> Unit) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val email = account.email ?: ""
                if (email.endsWith("@sookmyung.ac.kr")) {
                //if (email.endsWith("@gmail.com")) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val firebaseAuth = FirebaseAuth.getInstance()
                    val firebaseTask = firebaseAuth.signInWithCredential(credential).await()
                    val user = firebaseTask.user
                    if (user != null) {
                        auth.verifyToken(user)
                        val isNewUser = firebaseTask.additionalUserInfo?.isNewUser ?: false
                        val role = getRole(user)
                        if (isNewUser) {
                            onResult(true, "환영합니다.$role")
                        } else {
                            onResult(true, "로그인에 성공했습니다.($role)")
                        }
                    } else {
                        onResult(false, "Firebase 인증에 실패했습니다.")
                    }
                } else {
                    signOut { success ->
                        if (success) {
                            onResult(false, "숙명 메일로 다시 시도해주세요.")
                        } else {
                            onResult(false, "로그아웃에 실패했습니다.")
                        }
                    }
                }
            } else {
                onResult(false, "로그인에 실패했습니다.1111")
            }
        } catch (e: ApiException) {
            onResult(false, "로그인에 실패했습니다.2222")
        }
    }


    suspend fun signOut(onResult: (Boolean) -> Unit) {
        try {
            googleSignInClient.signOut().await() // Google 로그아웃
            FirebaseAuth.getInstance().signOut() // Firebase 로그아웃
            onResult(true)
        } catch (e: Exception) {
            Log.e("GoogleSignInClient", "Error signing out: ${e.localizedMessage}")
            onResult(false)
        }
    }

    // 회원 탈퇴
    suspend fun revokeAccess(onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                val refreshedAccount = googleSignInClient.silentSignIn().await()

                if (refreshedAccount != null && refreshedAccount.idToken != null) {
                    val credential =
                        GoogleAuthProvider.getCredential(refreshedAccount.idToken, null)
                    user.reauthenticate(credential).await()

                    // Firestore 데이터 삭제
                    deleteUserData(user.uid)

                    // Storage 데이터 삭제
                    val userStorageRef =
                        FirebaseStorage.getInstance().reference.child("users/${user.uid}/gifticons")
                    deleteAllFilesInFolder(userStorageRef) { success ->
                        if (!success) {
                            Log.d("GoogleSignInClient", "Failed to delete files in the folder.")
                        }
                    }

                    // Auth 계정 삭제
                    user.delete().await()

                    // 로그아웃 처리
                    googleSignInClient.signOut().await()
                    FirebaseAuth.getInstance().signOut()
                    onResult(true)
                } else {
                    Log.e("GoogleSignInClient", "Failed to refresh ID token for reauthentication.")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e(
                    "GoogleSignInClient",
                    "Error during account deletion or re-authentication: ${e.localizedMessage}"
                )
                onResult(false)
            }
        } else {
            Log.d("GoogleSignInClient", "No signed-in user found")
            onResult(false)
        }
    }

    private suspend fun deleteUserData(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)

        // 사용자 문서 삭제
        userDocRef.delete().await()

        // 서브컬렉션 삭제
        deleteCollection(userDocRef.collection("gifticons"))
        deleteCollection(userDocRef.collection("scrappedPosts"))
        deleteCollection(userDocRef.collection("schedules"))
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        val batch = FirebaseFirestore.getInstance().batch()
        val documents = collection.get().await().documents
        for (document in documents) {
            batch.delete(document.reference)
        }
        batch.commit().await()
    }

    // Storage 내 사용자 이미지 및 파일 삭제
    private suspend fun deleteAllFilesInFolder(storageRef: StorageReference, onResult: (Boolean) -> Unit) {
        try {
            val listResult = storageRef.listAll().await()  // 경로에서 모든 항목 나열

            listResult.items.forEach { fileRef ->  // 나열된 각 항목에 대해 삭제 실행
                fileRef.delete().await()
            }

            onResult(true)  // 모든 파일 삭제 후 결과 콜백 호출
        } catch (e: Exception) {
            Log.e("GoogleSignInClient", "Failed to delete storage contents: ${e.localizedMessage}")
            onResult(false)
        }
    }
}