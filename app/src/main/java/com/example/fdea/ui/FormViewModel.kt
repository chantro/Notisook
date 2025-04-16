package com.example.fdea.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fdea.data.AppUser
import com.example.fdea.data.ApplicantInfo
import com.example.fdea.data.Form
import com.example.fdea.data.LockerDetail
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val ref = database.getReference("forms")
    private val _forms = MutableStateFlow<List<Form>>(emptyList())
    val forms: StateFlow<List<Form>> = _forms
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> get() = _currentUser
    private var certificateAvailable = false

    init {
        fetchCurrentUser()
    }

    suspend fun addApplicantToForm(
        formViewModel: FormViewModel,
        selectedForm: Form,
        lockerNumber: String?,
        lockerLocation: String?,
    ): Boolean {
        val currentUser = formViewModel.currentUser.value
        return currentUser?.let {
            val newApplicantInfo = ApplicantInfo(
                user = it,
                appliedTime = System.currentTimeMillis(),
                lockerNum = lockerNumber,
                lockerLocation = lockerLocation ?: ""
            )
            val updatedForm = selectedForm.copy(
                applicantInfos = selectedForm.applicantInfos + newApplicantInfo
            )

            val success =
                updateFormAtomically(formViewModel, updatedForm, lockerNumber, lockerLocation ?: "")
            Log.d("chchdd", "success: $success")
            if (success) {
                updateLockerInfoInFirebase(lockerNumber, lockerLocation)
            }
            return success
        } ?: false
    }

    // 사물함 신청 취소 함수
    /*suspend fun cancelApplicantFromForm(
        formViewModel: FormViewModel,
        selectedForm: Form,
        lockerNumber: String?,
        lockerLocation: String?
    ): Boolean {
        val currentUser = formViewModel.currentUser.value
        return currentUser?.let {
            val updatedForm = selectedForm.copy(
                applicantInfos = selectedForm.applicantInfos.filterNot { applicant ->
                    applicant?.user?.studentNum == currentUser.studentNum &&
                            applicant.lockerNum == lockerNumber &&
                            applicant.lockerLocation == lockerLocation
                }
            )
            Log.d("cancelApplicantFromForm", "Updated Form: ${updatedForm.applicantInfos}")

            return try {
                val result = updateFormAtomically(formViewModel, updatedForm, lockerNumber, lockerLocation ?: "")
                if (result) {
                    Log.d("cancelApplicantFromForm", "Form updated successfully")
                    // Firestore에서 사용자의 사물함 정보 제거
                    updateLockerInfoInFirebase("", "")
                }
                result
            } catch (e: Exception) {
                Log.e("cancelApplicantFromForm", "Error updating form: ${e.message}")
                false
            }
        } ?: false
    }
*/
    private suspend fun updateLockerInfoInFirebase(lockerNumber: String?, lockerLocation: String?) {
        val firestore: FirebaseFirestore = Firebase.firestore
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val userRef = firestore.collection("users").document(userId)
            val lockerData = hashMapOf(
                "lockerNum" to lockerNumber,
                "lockerLocation" to lockerLocation
            )
            try {
                userRef.update(lockerData as Map<String, Any>).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    suspend fun updateFormAtomically(
        formViewModel: FormViewModel,
        updatedForm: Form,
        lockerNumber: String?,
        lockerLocation: String
    ): Boolean {
        val database = FirebaseDatabase.getInstance().reference
        val deferred = CompletableDeferred<Boolean>()
        val currentUserStudentNum = updatedForm.applicantInfos.last()?.user?.studentNum

        database.child("forms").child(updatedForm.id).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val form = currentData.getValue(Form::class.java) ?: return Transaction.success(
                    currentData
                )
                val mutableApplicantInfos = form.applicantInfos.toMutableList()
                val maxApplicants = form.persons.toIntOrNull() ?: 0

                // Skip isFormFull check if the formName indicates a locker application
                val isLockerApplication = form.formName.contains("사물함 신청", ignoreCase = true)

                // Check if the form is full, but only if it's not a locker application
                if (!isLockerApplication && mutableApplicantInfos.size >= maxApplicants) {
                    return Transaction.abort()
                }

                // Check if the user has already applied
                if (mutableApplicantInfos.any { it?.user?.studentNum == currentUserStudentNum }) {
                    return Transaction.abort()
                }

                // Check if the locker has already been assigned using dataSnapshot
                if (isLockerApplication) {
                    val isAlreadyReserved = form.applicantInfos.any {
                        it?.lockerNum == lockerNumber && it?.lockerLocation == lockerLocation
                    }
                    if (isAlreadyReserved) {
                        return Transaction.abort()
                    }
                }

                mutableApplicantInfos.add(updatedForm.applicantInfos.last())
                currentData.value = form.copy(applicantInfos = mutableApplicantInfos)
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    deferred.completeExceptionally(error.toException())
                } else {
                    deferred.complete(committed)
                }
            }
        })

        return try {
            deferred.await()
        } catch (e: Exception) {
            false
        }
    }


    private fun fetchCurrentUser() {
        viewModelScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val userId = firebaseUser.uid
                try {
                    val userDocument =
                        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                            .await()
                    val user = userDocument.toObject(AppUser::class.java)
                    _currentUser.value = user
                } catch (e: Exception) {
                    Log.e("FormViewModel", "Error fetching user data: ${e.message}")
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    fun saveNonLockerForm(
        major: String,
        formName: String,
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        persons: String,
        enrolled: Boolean,
        paidFee: Boolean,
        content: String,
        lockerDetails: List<LockerDetail>
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        val currentDate = sdf.format(Date())
        val currentTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            val formId = ref.push().key
            if (formId != null) {
                val newForm =
                    Form(
                        formId,
                        major,
                        "nonLocker",
                        formName,
                        startDate,
                        endDate,
                        startTime,
                        endTime,
                        persons,
                        enrolled,
                        paidFee,
                        content,
                        applicantInfos = listOf(), // 초기값 설정
                        lockerDetails
                    )
                _forms.value = listOf(newForm) + _forms.value

                // Firebase에 form 저장
                ref.child(formId).setValue(newForm)
                    .addOnSuccessListener {
                        Log.d(
                            "FormViewModel",
                            "NonLocker Form Data Saved: $formName, $startDate, $endDate, $startTime, $endTime, $persons, $enrolled, $paidFee, $content"
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("FormViewModel", "Failed to save NonLocker Form", e)
                    }
            }
        }
    }

    fun saveLockerForm(
        major: String,
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        enrolled: Boolean,
        paidFee: Boolean,
        content: String,
        lockerDetails: List<LockerDetail>
    ) {

        // 저장 로직 추가
        println("Form Data Saved:  $major, $startDate, $endDate, $startTime, $endTime, $enrolled, $paidFee, $content")
        Log.d(
            "chae",
            "Form Data Saved: $major, $startDate, ${lockerDetails.first().location} $endDate, $startTime, $endTime, $enrolled, $paidFee, $content"
        )


        viewModelScope.launch {
            val formId = ref.push().key
            if (formId != null) {
                val newForm =
                    Form(
                        formId,
                        major,
                        "locker",
                        "사물함 신청",
                        startDate,
                        endDate,
                        startTime,
                        endTime,
                        "",
                        enrolled,
                        paidFee,
                        content,
                        applicantInfos = listOf(),
                        lockerDetails,

                        )
                _forms.value = listOf(newForm) + _forms.value


                // Firebase에 form 저장
                ref.child(formId).setValue(newForm)
                    .addOnSuccessListener {
                        Log.d(
                            "FormViewModel",
                            "Locker Form Data Saved: $startDate, $endDate, $startTime, $endTime, $enrolled, $paidFee, $content"
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("FormViewModel", "Failed to save Locker Form", e)
                    }
            }
        }
    }

    fun deleteForm(form: Form) {
        viewModelScope.launch {
            // Firebase에서 form 삭제
            ref.child(form.id).removeValue().addOnSuccessListener {
                Log.d("FormViewModel", "Form deleted successfully: $form.id")
                // 로컬 상태에서도 해당 폼 삭제
                _forms.update { currentForms ->
                    currentForms.filterNot { it.id == form.id }
                }
            }.addOnFailureListener { e ->
                Log.e("FormViewModel", "Failed to delete form: $form.id", e)
            }
        }
    }

    // 신청이 마감되었는지 확인하는 로직
    fun isFormFull(selectedForm: Form): Boolean {
        return selectedForm.applicantInfos.size >= (selectedForm.persons.toIntOrNull() ?: 0)
    }

    fun isCertificateAvailable(): Boolean {
        // 실제로 재학증명서 발급 상태 확인 로직
        return certificateAvailable
    }


    fun applyToForm(selectedForm: Form) {
        // 신청 로직 구현
        Log.d("FormViewModel", "Form Applied: ${selectedForm.formName}")
    }
}