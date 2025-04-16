package com.example.fdea.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fdea.data.Comment
import com.example.fdea.data.Post
import com.example.fdea.data.Schedule
import com.example.fdea.data.ScrapRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class BoardViewModel : ViewModel() {
    private val userId = Firebase.auth.currentUser?.uid
    private val firestore = Firebase.firestore

    // 댓글
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    //스크랩
    private val scrapRepository = ScrapRepository()

    private val _scrappedPosts = MutableStateFlow<List<Post>>(emptyList())
    val scrappedPosts: StateFlow<List<Post>> = _scrappedPosts
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost
    private val _hasSelectedPost = MutableStateFlow(false)
    val hasSelectedPost: StateFlow<Boolean> = _hasSelectedPost
    // 스크랩 수와 조회수를 위한 StateFlow 추가
    private val _scrapCount = MutableStateFlow(0)
    val scrapCount: StateFlow<Int> = _scrapCount

    private val _viewCount = MutableStateFlow(0)
    val viewCount: StateFlow<Int> = _viewCount

    fun incrementScrapCount(postId: String) {
        val postRef = firestore.collection("posts").document(postId)

        postRef.update("scraps", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnSuccessListener {
                Log.d("BoardViewModel", "Scrap count incremented for post ID: $postId")
            }
            .addOnFailureListener { e ->
                Log.e("BoardViewModel", "Failed to increment scrap count for post ID: $postId", e)
            }
    }

    fun decrementScrapCount(postId: String) {
        val postRef = firestore.collection("posts").document(postId)

        postRef.update("scraps", com.google.firebase.firestore.FieldValue.increment(-1))
            .addOnSuccessListener {
                Log.d("BoardViewModel", "Scrap count decremented for post ID: $postId")
            }
            .addOnFailureListener { e ->
                Log.e("BoardViewModel", "Failed to decrement scrap count for post ID: $postId", e)
            }
    }


    fun incrementViewCount(postId: String) {
        val postRef = firestore.collection("posts").document(postId)

        // Firestore의 views 필드를 1 증가시킵니다.
        postRef.update("views", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnSuccessListener {
                Log.d("BoardViewModel", "View count incremented for post ID: $postId")
            }
            .addOnFailureListener { e ->
                Log.e("BoardViewModel", "Failed to increment view count for post ID: $postId", e)
            }
    }

    fun loadPostDetails(post: Post) {
        val postRef = firestore.collection("posts").document(post.id)

        postRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("BoardViewModel", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // 전체 데이터를 출력하여 실제 필드 이름과 값을 확인
                Log.d("BoardViewModel", "Document data: ${snapshot.id} - ${snapshot.data}")

                // 스크랩 수와 조회 수를 업데이트
                val scrapCount = snapshot.getLong("scraps")?.toInt() ?: 0
                val viewCount = snapshot.getLong("views")?.toInt() ?: 0
                _scrapCount.value = scrapCount
                _viewCount.value = viewCount
                Log.d("BoardViewModel", "Scrap count: $scrapCount, View count: $viewCount")
            } else {
                Log.d("BoardViewModel", "Document does not exist")
            }
        }
    }






    suspend fun getScrappedPosts() {
        _scrappedPosts.value = scrapRepository.getScrappedPosts()
    }

    suspend fun addScrap(post: Post) { // 스크랩 버튼 누르면 스크랩 되게
        scrapRepository.addScrap(post)
    }

    suspend fun removeScrap(postId: String) {
        scrapRepository.removeScrap(postId)
    }

    suspend fun isScrapped(post: Post): Boolean {
        return scrapRepository.isScrapped(post)
    }

    // 단일 선택을 관리하는 toggleSelection 함수
    fun toggleSelection(post: Post) {
        _selectedPost.value = if (_selectedPost.value == post) null else post
        _hasSelectedPost.value = _selectedPost.value != null
        Log.d("BoardViewModel", "Selected Post: ${_selectedPost.value?.title}, hasSelectedPost: ${_hasSelectedPost.value}")
    }

    // 선택된 단일 게시물을 캘린더에 추가
    suspend fun addSelectedPostToCalendar(mainViewModel: MainViewModel, startDate: String, endDate: String) {
        val selectedPost = _selectedPost.value
        if (selectedPost != null) {
            addEventToCalendar(selectedPost, mainViewModel, startDate, endDate)
            _selectedPost.value = null
            _hasSelectedPost.value = false
        }
    }

    // 스크랩 후 캘린더에 추가하는 함수
    suspend fun addEventToCalendar(post: Post, mainViewModel: MainViewModel, startDate: String, endDate: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val schedule = Schedule(
                id = post.id,
                title = post.title,
                startDate = startDate,
                endDate = endDate
            )

            firestore.collection("users")
                .document(userId)
                .collection("schedules")
                .document(schedule.id)
                .set(schedule)
                .await()

            mainViewModel.addSchedulePostId(post.id)
        }
    }

    fun loadComments(post: Post) {
        val commentRef = firestore.collection("posts")
            .document(post.id)  // 게시물 ID
            .collection("comments")  // 댓글 컬렉션

        // 실시간으로 댓글 데이터를 가져오는 리스너 추가
        commentRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("BoardViewModel", "Error loading comments", e)
                return@addSnapshotListener
            }

            // 댓글 목록 초기화
            val newComments = mutableListOf<Comment>()

            snapshot?.documents?.forEach { document ->
                document.toObject(Comment::class.java)?.let {
                    newComments.add(it)
                }
            }

            _comments.value = newComments // LiveData로 댓글 리스트 업데이트
        }
    }

    fun addComment(post: Post, content: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        val currentDate = sdf.format(Date())

        viewModelScope.launch {
            val commentId = firestore.collection("posts")
                .document(post.id)
                .collection("comments")
                .document().id // Firestore에서 새로운 댓글 ID 생성

            if (commentId.isNotEmpty() && userId != null) {
                val newComment = Comment(commentId, userId, content, currentDate)

                // Firestore에 댓글 저장
                firestore.collection("posts")
                    .document(post.id) // 사이트 ID
                    .collection("comments") // 댓글 컬렉션
                    .document(commentId) // 새 댓글 ID
                    .set(newComment) // 댓글 데이터를 Firestore에 저장
                    .addOnSuccessListener {
                        Log.d("BoardViewModel", "Successfully added comment")
                    }.addOnFailureListener { e ->
                        Log.e("BoardViewModel", "Failed to add comment", e)
                    }
            }
        }
    }

    fun deleteComment(post: Post, comment: Comment) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid  // 현재 로그인된 사용자의 UID

        // Firestore의 댓글 경로 참조
        val commentRef = firestore.collection("posts")
            .document(post.id)
            .collection("comments")
            .document(comment.id)

        // Firestore에서 댓글 데이터를 불러와서 사용자 확인 후 삭제
        commentRef.get().addOnSuccessListener { documentSnapshot ->
            val commentData = documentSnapshot.toObject(Comment::class.java)
            if (commentData != null && commentData.userId == currentUserUid) {
                // UserID가 일치하면 댓글 삭제
                commentRef.delete().addOnSuccessListener {
                    // 댓글 삭제 후 UI 업데이트
                    _comments.value = _comments.value?.filter { it.id != comment.id }!!
                    Log.d("BoardViewModel", "Comment successfully deleted.")
                }.addOnFailureListener { e ->
                    Log.e("BoardViewModel", "Failed to delete comment.", e)
                }
            } else {
                Log.d("BoardViewModel", "Unauthorized attempt to delete comment.")
            }
        }.addOnFailureListener { e ->
            Log.e("BoardViewModel", "Error accessing Firestore", e)
        }
    }
}
