package com.example.fdea.ui

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fdea.data.Form
import com.example.fdea.data.AppUser
import com.example.fdea.login.UserService

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

//로그인
class RegistrationViewModel : ViewModel() {
    private val database = Firebase.firestore
    // 임시 저장용 변수
    var tempUsername = mutableStateOf("")
    var tempStudentNum = mutableStateOf("")
    var tempPhoneNum = mutableStateOf("")
    var tempMajor = mutableStateOf("")
    var tempIdStep3 = mutableStateOf("")   // Step 3에서 아이디 임시 저장
    var tempPasswordStep3 = mutableStateOf("") // Step 3에서 비밀번호 임시 저장
    /*var tempIdStep5 = mutableStateOf("")   // Step 5에서 아이디 임시 저장
    var tempPasswordStep5 = mutableStateOf("") // Step 5에서 비밀번호 임시 저장
    var tempIdStep7 = mutableStateOf("")   // Step 7에서 아이디 임시 저장
    var tempPasswordStep7 = mutableStateOf("") // Step 7에서 비밀번호 임시 저장*/

    // 모든 필드를 임시로 저장하는 함수
    fun saveTempUserInfo(username: String, studentNum: String, phoneNum: String, major: String) {
        tempUsername.value = username
        tempStudentNum.value = studentNum
        tempPhoneNum.value = phoneNum
        tempMajor.value = major
    }
    // 모든 필드를 임시로 저장하는 함수
    fun saveTempUserInfo2(step: Int, id: String, password: String) {
        when (step) {
            3 -> {
                tempIdStep3.value = id
                tempPasswordStep3.value = password
                Log.d("UserInfo", "3단계: id = $id, password = $password")
                saveAllUserDataToFirebase()
            }
           /* 5 -> {
                tempIdStep5.value = id
                tempPasswordStep5.value = password
                Log.d("UserInfo", "5단계: id = $id, password = $password")
            }
            7 -> {
                tempIdStep7.value = id
                tempPasswordStep7.value = password
                Log.d("UserInfo", "7단계: id = $id, password = $password")
                //TODO 파이어베이스에 저장
                saveAllUserDataToFirebase()
            }*/
            else -> {
                Log.d("UserInfo", "잘못된 단계: $step")
            }
        }
    }
    // 모든 임시 데이터를 파이어베이스에 저장하는 함수
    private fun saveAllUserDataToFirebase() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val userData = mapOf(
                "username" to tempUsername.value,
                "studentNum" to tempStudentNum.value,
                "phoneNum" to tempPhoneNum.value,
                "major" to tempMajor.value,
                "step3Id" to tempIdStep3.value,
                "step3Password" to tempPasswordStep3.value,
               /* "step5Id" to tempIdStep5.value,
                "step5Password" to tempPasswordStep5.value,
                "step7Id" to tempIdStep7.value,
                "step7Password" to tempPasswordStep7.value*/
            )

            viewModelScope.launch {
                try {
                    database.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            UserService.loadUserData()
                            Log.d("Firestore", "User data successfully saved to Firestore!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error saving user data", e)
                        }
                } catch (e: Exception) {
                    Log.w("Firestore", "Error accessing Firestore", e)
                }
            }
        } else {
            Log.w("Firestore", "No user logged in")
        }
    }



    fun saveUserInfo(username: String, studentNum: String, phoneNum: String, major: String) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val newUser=AppUser(username,studentNum,phoneNum,major)
            viewModelScope.launch {
                try {
                    database.collection("users").document(user.uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            UserService.loadUserData()
                            Log.d("Firestore", "User information successfully written!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error writing document", e)
                        }
                } catch (e: Exception) {
                    Log.w("Firestore", "Error accessing Firestore", e)
                }
            }
        } else {
            Log.w("Firestore", "No user logged in")
        }
    }
    fun updateUserInfo(username: String, studentNum: String, phoneNum: String, major: String) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val updatedFields = mapOf(
                "username" to username,
                "studentNum" to studentNum,
                "phoneNum" to phoneNum,
                "major" to major,
                "approved" to false,
                "infoUpdatedAfterRejection" to true
            )

            viewModelScope.launch {
                try {
                    database.collection("users").document(user.uid)
                        .update(updatedFields)
                        .addOnSuccessListener {
                            UserService.loadUserData()
                            Log.d("Firestore", "User information successfully updated!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error updating document", e)
                        }
                } catch (e: Exception) {
                    Log.w("Firestore", "Error accessing Firestore", e)
                }
            }
        } else {
            Log.w("Firestore", "No user logged in")
        }
    }

}


