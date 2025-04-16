
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.fdea.data.Gifticon
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Locale

fun recognizeGifticon(context: Context, uri: Uri?, imageUrl: String, id: String, onComplete: (String?) -> Unit = {}) {
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    if (uri != null) {
        val image: InputImage = InputImage.fromFilePath(context, uri)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("DateExtractionUtils", "Text recognized: ${visionText.text}")
                val expiryDate = extractExpiryDate(visionText.text)
                expiryDate?.let {
                    Log.d("DateExtractionUtils", "Expiry Date: $expiryDate")
                    saveGifticonData(imageUrl, expiryDate, id) {
                        onComplete(expiryDate)
                    }
                } ?: onComplete(null) // No expiry date found, notify with null
            }
            .addOnFailureListener { e ->
                Log.e("DateExtractionUtils", "Error recognizing text", e)
                onComplete(null) // Recognition failed, notify with null
            }
    } else {
        onComplete(null) // No URI provided, notify with null
    }
}


fun extractExpiryDate(text: String): String? {
    val regex = "(\\d{4}\\.\\d{2}\\.\\d{2})|(\\d{4}년 \\d{2}월 \\d{2}일)".toRegex()
    val matchResult = regex.find(text)?.value ?: return null

    return if (matchResult.contains(".")) {
        matchResult // 이미 yyyy.MM.dd 형식이면 그대로 반환
    } else {
        // yyyy년 MM월 dd일 형식을 yyyy.MM.dd 형식으로 변환
        val inputFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
        val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        val date = inputFormat.parse(matchResult)
        outputFormat.format(date)
    }
}

fun saveGifticonData(imageUrl: String, expiryDate: String, id: String, onComplete: () -> Unit = {}) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val gifticon = Gifticon(id, imageUrl, expiryDate)

    // Firebase Firestore에 gifticon 저장
    FirebaseFirestore.getInstance().collection("users").document(userId)
        .collection("gifticons").document(id)
        .set(gifticon)
        .addOnSuccessListener {
            Log.d(
                "DateExtractionUtils",
                "Gifticon data saved successfully with fileName as document ID"
            )
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e("DateExtractionUtils", "Failed to save gifticon data", e)
            onComplete()
        }

}