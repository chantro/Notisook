package com.example.fdea.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.util.Log
import android.widget.EditText
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fdea.data.BenefitLocation
import com.example.fdea.data.Gifticon
import com.example.fdea.login.UserService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import recognizeGifticon
import saveGifticonData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class BenefitViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    var imminentExpiryCount = mutableStateOf(0)  // 유효기간 10일 이내인 기프티콘 수 저장하는 변수
    private val _gifticons = MutableStateFlow<List<Gifticon>>(emptyList()) // 저장된 기프티콘 이미지 목록
    val gifticons: StateFlow<List<Gifticon>> = _gifticons

    // 혜택이 있는 장소 목록을 저장할 MutableStateFlow
    private val _locations = MutableStateFlow<List<BenefitLocation>>(emptyList())
    val locations: StateFlow<List<BenefitLocation>> = _locations.asStateFlow()
    private val _major = MutableStateFlow<String?>(null)
    val major: StateFlow<String?> = _major.asStateFlow()

    init {
        viewModelScope.launch {
            UserService.major.collect { value ->
                _major.value = value
            }
        }
        viewModelScope.launch {
            _major.collect { major ->
                major?.let {
                    fetchLocations(it)
                }
            }
        }
        loadGifticonImages() // ViewModel 초기화 시 이미지 로드
        checkImminentExpiries()
    }
    fun getImminentGifticons(onComplete: (List<Gifticon>) -> Unit) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.e("BenefitViewModel", "User ID is null, cannot get imminent gifticons")
                onComplete(emptyList())
                return@launch
            }
            firestore.collection("users").document(userId)
                .collection("gifticons")
                .whereEqualTo("isImminent", true)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val imminentGifticons = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Gifticon::class.java)
                    }
                    onComplete(imminentGifticons)
                }
                .addOnFailureListener { e ->
                    Log.e("BenefitViewModel", "Failed to fetch imminent gifticons from Firestore", e)
                    onComplete(emptyList())
                }
        }
    }




    private fun loadGifticonImages(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.e("BenefitViewModel", "User ID is null, cannot load images")
                return@launch
            }

            firestore.collection("users").document(userId)
                .collection("gifticons").orderBy("expiryDate") // 정렬 기준 변경 가능
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val gifticons = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Gifticon::class.java)
                    }
                    _gifticons.value = gifticons
                    Log.d("BenefitViewModel", "loadGifticonImages completed")
                    onComplete()  // 데이터 로드 완료 후 콜백 호출
                }
                .addOnFailureListener { e ->
                    Log.e("BenefitViewModel", "Failed to load gifticon images from Firestore", e)
                    onComplete()  // 실패 시에도 콜백 호출
                }
        }
    }

    private fun checkImminentExpiries(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.e("BenefitViewModel", "User ID is null, cannot check expiries")
                return@launch
            }

            val gifticonCollectionRef = firestore.collection("users").document(userId)
                .collection("gifticons")

            gifticonCollectionRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                    val currentDate = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    var imminentCount = 0

                    for (document in querySnapshot.documents) {
                        val expiryDateStr = document.getString("expiryDate") ?: continue
                        val expiryDateParsed = sdf.parse(expiryDateStr)?.let {
                            Calendar.getInstance().apply {
                                time = it
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time
                        } ?: continue

                        // 날짜 차이 계산
                        val diff = expiryDateParsed.time - currentDate.time
                        val daysDiff = diff / (1000 * 60 * 60 * 24)

                        val isImminent = daysDiff <= 10
                        if (isImminent) imminentCount++

                        // Firestore 데이터 업데이트
                        document.reference.update("isImminent", isImminent)
                            .addOnSuccessListener {
                                Log.d("BenefitViewModel", "Gifticon data updated successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("BenefitViewModel", "Failed to update gifticon data", e)
                            }
                    }

                    imminentExpiryCount.value = imminentCount
                    Log.d("BenefitViewModel", "checkImminentExpiries completed")
                    onComplete()  // 데이터 업데이트 후 콜백 호출
                }
                .addOnFailureListener { e ->
                    Log.e("BenefitViewModel", "Failed to fetch gifticon data", e)
                    onComplete()  // 실패 시에도 콜백 호출
                }
        }
    }

    fun registerGifticonImage(context: Context, uri: Uri?, onComplete: () -> Unit = {}) {
        uri?.let {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val id = UUID.randomUUID().toString()

            val storageRef = FirebaseStorage.getInstance().reference.child("users/$userId/gifticons/$id")

            storageRef.putFile(uri).addOnSuccessListener {
                Log.d("BenefitViewModel", "Image uploaded successfully: $id")
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    recognizeGifticon(context, uri, downloadUrl.toString(), id) { expiryDate ->
                        if (expiryDate != null) {
                            saveGifticonData(downloadUrl.toString(), expiryDate, id) {
                                checkImminentExpiries {
                                    loadGifticonImages {
                                        Log.d("BenefitViewModel", "check&load completed")
                                        onComplete()
                                    }
                                }
                            }
                        } else {
                            // Show dialog to manually input expiration date
                            showManualInputDialog(context) { inputDate ->
                                if (inputDate != null) {
                                    saveGifticonData(downloadUrl.toString(), inputDate, id) {
                                        checkImminentExpiries {
                                            loadGifticonImages {
                                                Log.d("BenefitViewModel", "check&load completed")
                                                onComplete()
                                            }
                                        }
                                    }
                                } else {
                                    onComplete()
                                }
                            }
                        }
                    }
                }.addOnFailureListener {
                    Log.e("BenefitViewModel", "Failed to retrieve image URL: ${it.message}")
                    onComplete()
                }
            }.addOnFailureListener {
                Log.e("BenefitViewModel", "Failed to upload image: ${it.message}")
                onComplete()
            }
        }
    }

    private fun showManualInputDialog(context: Context, onInput: (String?) -> Unit) {
        val editText = EditText(context).apply {
            hint = "YYYY.MM.DD"
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("유효기간을 추출할 수 없습니다")
            .setMessage("유효기간을 입력하세요 (예: 2024.09.02)")
            .setView(editText)
            .setPositiveButton("확인", null) // Set null initially, we'll override it later
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                onInput(null)
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val inputDate = editText.text.toString()
                if (inputDate.matches(Regex("""\d{4}\.\d{2}\.\d{2}"""))) {
                    onInput(inputDate)
                    dialog.dismiss()
                } else {
                    editText.background.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                }
            }
        }

        dialog.show()
    }
    fun updateGifticonExpiryDate(gifticon: Gifticon, newExpiryDate: String, onComplete: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val gifticonDocRef = firestore.collection("users").document(userId)
            .collection("gifticons").document(gifticon.id)

        gifticonDocRef.update("expiryDate", newExpiryDate)
            .addOnSuccessListener {
                Log.d("BenefitViewModel", "Gifticon expiry date successfully updated")
                checkImminentExpiries {
                    loadGifticonImages {
                        Log.d("BenefitViewModel", "check&load completed")
                        onComplete()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BenefitViewModel", "Failed to update gifticon expiry date", e)
                onComplete()
            }
    }

    fun removeGifticonImage(gifticon: Gifticon, onComplete: () -> Unit = {}) {
        _gifticons.value = _gifticons.value.filter { it.id != gifticon.id }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fileName = gifticon.id
        val storageRef = storage.reference.child("users/$userId/gifticons/$fileName")

        // Firebase Storage에서 삭제
        storageRef.delete().addOnSuccessListener {
            Log.d("BenefitViewModel", "Gifticon successfully deleted from Storage: $fileName")

            val firestore = FirebaseFirestore.getInstance()
            val gifticonDocRef = firestore.collection("users").document(userId)
                .collection("gifticons").document(fileName)

            // Firebase Firestore에서 삭제
            gifticonDocRef.delete()
                .addOnSuccessListener {
                    Log.d("BenefitViewModel", "Gifticon data successfully deleted from Firestore")
                    // 데이터 삭제 후 imminentExpiryCount 갱신
                    checkImminentExpiries {
                        loadGifticonImages {
                            onComplete() // Notify completion
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BenefitViewModel", "Failed to delete gifticon data from Firestore", e)
                    onComplete()
                }
        }.addOnFailureListener { e ->
            Log.e("BenefitViewModel", "Failed to delete gifticon from Storage", e)
            onComplete()
        }
    }

    fun fetchLocations(major: String) {
        firestore.collection("places")
            .get()
            .addOnSuccessListener { result ->
                val fetchedLocations = result.mapNotNull { document ->
                    val documentMajor = document.getString("major") ?: ""
                    if (documentMajor == "FA" || documentMajor == major) {
                        val name = document.getString("name") ?: ""
                        val lat = document.getDouble("lat") ?: 0.0
                        val lng = document.getDouble("lng") ?: 0.0
                        val benefit = document.getString("benefit") ?: ""
                        BenefitLocation(name, documentMajor, lat, lng, benefit, document.id)
                    } else {
                        null
                    }
                }
                _locations.value = fetchedLocations
            }
            .addOnFailureListener {
                // 오류 처리
            }
    }

    fun saveLocation(location: BenefitLocation) {
        val id = generateIdFromLatLng(location.major, location.lat, location.lng)
        val document = firestore.collection("places").document(id)
        val locationWithId = location.copy(id = id)
        document.set(locationWithId)
            .addOnSuccessListener {
                // 저장 후 상태에 새 위치 추가
                _locations.value = _locations.value + locationWithId
            }
            .addOnFailureListener {
                // 오류처리
            }

    }

    fun updateLocation(location: BenefitLocation) {
        val id = generateIdFromLatLng(location.major, location.lat, location.lng)
        firestore.collection("places").document(id)
            .set(location)
            .addOnSuccessListener {
                // 수정 후 상태에서 해당 위치 업데이트
                _locations.value = _locations.value.map {
                    if (it.id == id) location else it
                }
            }
            .addOnFailureListener {
                // 오류처리
            }
    }

    fun deleteLocation(location: BenefitLocation) {
        firestore.collection("places").document(location.id)
            .delete()
            .addOnSuccessListener {
                // 삭제 후 상태에서 해당 위치 제거
                _locations.value = _locations.value.filter { it.id != location.id }
            }
            .addOnFailureListener {
                // 오류처리
            }
    }

    private fun generateIdFromLatLng(major: String, lat: Double, lng: Double): String {
        return "${major}_${lat}_${lng}"
    }
}