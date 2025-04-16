package com.example.fdea.login

import android.util.Log
import com.example.fdea.data.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object UserService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _username = MutableStateFlow<String?>(null)
    private val _major = MutableStateFlow<String?>(null)
    private val _approved = MutableStateFlow<Boolean?>(null)
    private val _lockerLocation = MutableStateFlow<String?>(null)
    private val _lockerNum = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()
    val major: StateFlow<String?> = _major.asStateFlow()
    val approved: StateFlow<Boolean?> = _approved.asStateFlow()
    val lockerLocation: StateFlow<String?> = _lockerLocation.asStateFlow()
    val lockerNum: StateFlow<String?> = _lockerNum.asStateFlow()

    fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val docSnapshot = firestore.collection("users").document(user.uid).get().await()
                    _username.value = docSnapshot.getString("username")
                    _major.value = docSnapshot.getString("major")
                    _approved.value = docSnapshot.getBoolean("approved")
                    _lockerLocation.value = docSnapshot.getString("lockerLocation")
                    _lockerNum.value = docSnapshot.getString("lockerNum")
                } catch (e: Exception) {
                    Log.e("UserService", "Error loading user data", e)
                }
            }
        }
    }
    //조건에 맞는 user 들만 회원수락 목록에 보여지게 함.- 회원가입 한 후 , 또는 개인 정보 수정한 후
    fun loadPendingUsers(callback: (List<AppUser>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val firstQuery = firestore.collection("users")
            .whereEqualTo("approved", false)
            .whereEqualTo("rejected", false)
            .whereEqualTo("infoUpdatedAfterRejection", false)

        val secondQuery = firestore.collection("users")
            .whereEqualTo("approved", false)
            .whereEqualTo("rejected", true)
            .whereEqualTo("infoUpdatedAfterRejection", true)

        val thirdQuery = firestore.collection("users")
            .whereEqualTo("approved", false)
            .whereEqualTo("rejected", false)
            .whereEqualTo("infoUpdatedAfterRejection", true)

        firstQuery.get()
            .addOnSuccessListener { firstResult ->
                val users = firstResult.mapNotNull { document ->
                    document.toObject(AppUser::class.java)
                }.toMutableList()

                secondQuery.get()
                    .addOnSuccessListener { secondResult ->
                        val secondUsers = secondResult.mapNotNull { document ->
                            document.toObject(AppUser::class.java)
                        }

                        users.addAll(secondUsers)

                        thirdQuery.get()
                            .addOnSuccessListener { thirdResult ->
                                val thirdUsers = thirdResult.mapNotNull { document ->
                                    document.toObject(AppUser::class.java)
                                }

                                users.addAll(thirdUsers)
                                callback(users)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("UserService", "Error loading third set of pending users", exception)
                                callback(emptyList())
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("UserService", "Error loading second set of pending users", exception)
                        callback(emptyList())
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("UserService", "Error loading first set of pending users", exception)
                callback(emptyList())
            }
    }




    fun approveUser(studentNum: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userQuery = firestore.collection("users")
                    .whereEqualTo("studentNum", studentNum)
                    .get()
                    .await()
                Log.d("didid","$userQuery")

                val userDoc = userQuery.documents.firstOrNull()
                if (userDoc != null) {
                    userDoc.reference.update(
                        "approved", true,
                        "rejected",false,
                        "infoUpdatedAfterRejection",false).await()
                    callback(true)
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                Log.e("UserService", "Error approving user", e)
                callback(false)
            }
        }
    }
    fun rejectUser(studentNum: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userQuery = firestore.collection("users")
                    .whereEqualTo("studentNum", studentNum)
                    .get()
                    .await()

                val userDoc = userQuery.documents.firstOrNull()
                if (userDoc != null) {
                    userDoc.reference.update("rejected", true).await()
                    userDoc.reference.update("infoUpdatedAfterRejection", false).await()
                    callback(true)
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                Log.e("UserService", "Error rejecting user", e)
                callback(false)
            }
        }
    }


    /*fun rejectUser(studentNum: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userQuery = firestore.collection("users")
                    .whereEqualTo("studentNum", studentNum)
                    .get()
                    .await()

                val userDoc = userQuery.documents.firstOrNull()
                if (userDoc != null) {
                    userDoc.reference.delete().await()
                    callback(true)
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                Log.e("UserService", "Error rejecting user", e)
                callback(false)
            }
        }
    }*/
}