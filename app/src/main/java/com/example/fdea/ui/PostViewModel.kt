/*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fdea.data.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class PostViewModel : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts
    private val _postStateFlow = MutableStateFlow<Post?>(null)
    val postStateFlow: StateFlow<Post?> = _postStateFlow
    init {
        // 기본 카테고리를 설정하여 초기 데이터를 로드합니다.
        fetchPosts(category = "snowe", subCategory = "notice")
    }

    // 카테고리와 서브 카테고리를 매개변수로 받아 Firestore에서 데이터를 로드하는 함수
    private fun fetchPosts(category: String, subCategory: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("sites").document(category).collection(subCategory)
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.map { document ->
                    // 'deadline' 필드의 타입을 유연하게 처리
                    val deadline = when (val deadlineValue = document.get("deadline")) {
                        is String -> deadlineValue
                        is Timestamp -> deadlineValue.toDate().toString()  // Timestamp일 경우 String으로 변환
                        else -> null
                    }

                    // 'uploadTime' 필드의 타입을 유연하게 처리
                    val uploadTime = when (val uploadTimeValue = document.get("uploadTime")) {
                        is String -> uploadTimeValue
                        is Timestamp -> uploadTimeValue.toDate().toString()  // Timestamp일 경우 String으로 변환
                        else -> null
                    }

                    Post(
                        id = document.id,
                        category = document.getString("category") ?: "",
                        content = document.getString("content") ?: "",
                        deadline = deadline,
                        keywords = document.get("keywords") as? List<String> ?: listOf(),
                        scraps = document.getLong("scraps")?.toInt() ?: 0,
                        title = document.getString("title") ?: "",
                        uploadTime = uploadTime ?: "",  // 'uploadTime'을 String으로 변환된 값 사용
                        url = document.getString("url") ?: "",
                        views = document.getLong("views")?.toInt() ?: 0,
                        writer = document.getString("writer") ?: ""
                    )
                }
                _posts.value = postList
            }
            .addOnFailureListener { exception ->
                // Firestore 접근 실패 시 로그 출력 또는 예외 처리
                Log.e("PostViewModel", "Error fetching posts", exception)
            }
    }

    // 카테고리 선택 시 호출할 함수
    fun onCategorySelected(category: String, subCategory: String) {
        fetchPosts(category, subCategory)
    }

    fun getPostById(postId: String) {
        viewModelScope.launch {
            val post = posts.value.find { it.id == postId }
            _postStateFlow.value = post
        }
    }
}
*/
