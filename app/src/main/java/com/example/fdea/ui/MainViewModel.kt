package com.example.fdea.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fdea.data.Form
import com.example.fdea.data.Post
import com.example.fdea.data.Schedule
import com.example.fdea.login.UserService
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//스케줄
@RequiresApi(Build.VERSION_CODES.O)
class MainViewModel : ViewModel() {
    private val userId = Firebase.auth.currentUser?.uid
    private val database = FirebaseDatabase.getInstance().reference
    private val firestore = Firebase.firestore
    private val _major = MutableStateFlow<String?>(null)
    val major: StateFlow<String?> = _major.asStateFlow()

    private val _recommendTags = MutableStateFlow<List<String>>(emptyList())
    val recommendTags: StateFlow<List<String>> = _recommendTags

    private val _forms = MutableStateFlow<List<Form>>(emptyList())
    val forms: StateFlow<List<Form>> = _forms

    // 스케줄
    private val _selectedDateSchedules = MutableLiveData<List<Schedule>>()
    val selectedDateSchedules: LiveData<List<Schedule>> = _selectedDateSchedules
    private val _allSchedules = MutableStateFlow<List<Schedule>>(emptyList())
    val allSchedules: StateFlow<List<Schedule>> = _allSchedules
    private val _schedulePostIds = MutableStateFlow<Set<String>>(emptySet())
    val schedulePostIds: StateFlow<Set<String>> = _schedulePostIds

    // 추천 공지사항
    private val _userSimilarPosts = MutableStateFlow<List<Post>>(emptyList())
    val userSimilarPosts: StateFlow<List<Post>> = _userSimilarPosts

    private val _contentSimilarPosts = MutableStateFlow<List<Post>>(emptyList())
    val contentSimilarPosts: StateFlow<List<Post>> = _contentSimilarPosts

    // 공지사항
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts
    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post

    private val _selectedCategory = MutableStateFlow("전체")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _selectedSubCategory = MutableStateFlow("전체")
    val selectedSubCategory: StateFlow<String> = _selectedSubCategory

    private val _sortOrder = MutableStateFlow("최신순")
    val sortOrder: StateFlow<String> = _sortOrder

    private val _isFetchingPosts = MutableStateFlow(true)
    val isFetchingPosts: StateFlow<Boolean> = _isFetchingPosts

    private var lastDocumentSnapshot: DocumentSnapshot? = null
    private var hasMorePosts = true
    private val pageSize = 5
    private var isResetting = false


    // 선택한 Post를 업데이트하는 함수
    fun selectPost(selectedPost: Post) {
        _post.value = selectedPost
    }

    init {
        viewModelScope.launch {
            UserService.major.collect { value ->
                _major.value = value
                Log.d("MainViewModel", "UserService.major collected: $value")
                value?.let {
                    loadForms(it)  // 전공 정보를 기반으로 폼 데이터 로드
                }
            }
        }
        viewModelScope.launch {
            fetchRecommendedPosts()
            getAllSchedules()
            getUserKeywords()

            combine(_selectedCategory, _selectedSubCategory, _sortOrder) { category, subCategory, order ->
                Triple(category, subCategory, order)
            }.distinctUntilChanged().collect { (category, subCategory, order) ->
                fetchPosts(category, subCategory, order, true)
            }
        }
    }

    private suspend fun fetchPosts(category: String, subCategory: String, sortOrder: String, isInitialFetch: Boolean) {
        _isFetchingPosts.value = true

        if (isInitialFetch) {
            lastDocumentSnapshot = null  // 조건 변경 시 lastDocumentSnapshot 초기화
        }

        try {
            val query = createQuery(category, subCategory, sortOrder, lastDocumentSnapshot)
            val snapshot = query.get().await()
            val newPosts = snapshot.documents.mapNotNull { createPostObject(it) }

            if (isInitialFetch) {
                _posts.value = newPosts
                lastDocumentSnapshot = snapshot.documents.lastOrNull()
                hasMorePosts = newPosts.size == pageSize
            } else {
                _posts.value += newPosts  // 추가 로드 시 기존 posts에 더함
                lastDocumentSnapshot = snapshot.documents.lastOrNull()
                hasMorePosts = newPosts.size == pageSize
            }
        } catch (e: Exception) {
            Log.e("FetchPosts", "Error fetching posts", e)
        } finally {
            _isFetchingPosts.value = false
        }
    }

    fun fetchMorePosts() {
        if (_isFetchingPosts.value || !hasMorePosts) return
        viewModelScope.launch {
            fetchPosts(_selectedCategory.value, _selectedSubCategory.value, _sortOrder.value, false)
        }
    }

    private fun mapCategoryToSiteId(category: String): String {
        return when (category) {
            "에브리타임" -> "everytime"
            "스노웨이" -> "snoway"
            "스노위" -> "snowe"
            "스노우보드" -> "snowboard"
            else -> category // 기본값으로 원래 문자열 반환
        }
    }

    private fun mapSubCategoryToBoardId(category: String, subCategory: String): String {
        // 스노우보드 카테고리의 경우, 공지사항만 매핑하고 나머지는 그대로 반환
        if (category == "스노우보드") {
            return subCategory
        }

        return when (subCategory) {
            "공지사항" -> "notice"
            "교내채용공고" -> "internalJob"
            "외부기관공고" -> "externalJob"
            "사회봉사공고" -> "cmtyService"
            "취업경력개발" -> "jobcareer"
            "자유게시판" -> "free"
            "정보게시판" -> "info"
            "홍보게시판" -> "promo"
            "동아리,학회" -> "club"
            else -> subCategory
        }
    }

    private fun createQuery(category: String, subCategory: String, sortOrder: String, lastDocument: DocumentSnapshot?): Query {
        val mappedCategory = mapCategoryToSiteId(category)
        val mappedSubCategory = mapSubCategoryToBoardId(category, subCategory)

        var query = firestore.collectionGroup("posts")
            .whereIn("postType", listOf("root", userId))

        when (sortOrder) {
            "마감일순" -> {
                when (mappedCategory) {
                    "전체" -> {
                        query = query.whereIn("site", listOf("snowe", "snowboard"))
                        query = query.whereIn("board", listOf("notice", "internalJob", "externalJob", "jobcareer", "퀴즈", "강의", "과제"))
                    }
                    "snowe", "snowboard" -> {
                        query = query.whereEqualTo("site", mappedCategory)
                        if (mappedSubCategory != "전체") {
                            query = query.whereEqualTo("board", mappedSubCategory)
                        } else {
                            val boards = when (mappedCategory) {
                                "snowe" -> listOf("notice", "internalJob", "externalJob", "jobcareer")
                                "snowboard" -> listOf("퀴즈", "강의", "과제")
                                else -> emptyList()
                            }
                            query = query.whereIn("board", boards)
                        }
                    }
                    else -> {
                        return query.limit(0) // 빈 결과 반환
                    }
                }
                query = query.orderBy("deadline", Query.Direction.ASCENDING)
            }

            "최신순" -> {
                if (mappedCategory != "전체") {
                    query = query.whereEqualTo("site", mappedCategory)
                    if (mappedSubCategory != "전체") {
                        query = query.whereEqualTo("board", mappedSubCategory)
                    }
                }
                query = query.orderBy("uploadTime", Query.Direction.DESCENDING)
            }

            else -> {
                if (mappedCategory != "전체") {
                    query = query.whereEqualTo("site", mappedCategory)
                    if (mappedSubCategory != "전체") {
                        query = query.whereEqualTo("board", mappedSubCategory)
                    }
                }
            }
        }

        lastDocument?.let {
            query = query.startAfter(it)
        }

        return query.limit(pageSize.toLong())
    }

    private suspend fun createPostObject(document: DocumentSnapshot): Post {
        val isViewed = isDocumentViewed(document.id)
        val siteId = document.getString("site") ?: ""
        val boardId = document.getString("board") ?: ""

        val mappedSiteName = when (siteId) {
            "everytime" -> "에브리타임"
            "snoway" -> "스노웨이"
            "snowe" -> "스노위"
            "snowboard" -> "스노우보드"
            else -> ""
        }

        val mappedBoardName = when (boardId) {
            "notice" -> "공지사항"
            "internalJob" -> "교내채용공고"
            "externalJob" -> "외부기관공고"
            "cmtyService" -> "사회봉사공고"
            "jobcareer" -> "취업경력개발"
            "free" -> "자유게시판"
            "info" -> "정보게시판"
            "promo" -> "홍보게시판"
            "club" -> "동아리,학회"
            else -> ""
        }

        val rawUploadTime = document.get("uploadTime")
        val uploadTime = when (rawUploadTime) {
            is Timestamp -> rawUploadTime
            is String -> Timestamp(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(rawUploadTime)))
            else -> null
        }

        return Post(
            id = document.id,
            category = document.getString("category") ?: "",
            content = document.getString("content") ?: "",
            deadline = document.getTimestamp("deadline"),
            keywords = document.get("keywords") as? List<String> ?: listOf(),
            scraps = document.getLong("scraps")?.toInt() ?: 0,
            title = document.getString("title") ?: "",
            uploadTime = uploadTime,
            url = document.getString("url") ?: "",
            views = document.getLong("views")?.toInt() ?: 0,
            writer = document.getString("writer") ?: "",
            courseName = document.getString("courseName") ?: "",
            siteName = mappedSiteName,
            boardName = mappedBoardName,
            isViewed = isViewed
        )
    }

    fun updateSelectedCategory(category: String) {
        viewModelScope.launch {
            _selectedCategory.value = category
            _selectedSubCategory.value = "전체"
            _sortOrder.value = "최신순"
        }
    }

    fun updateSelectedSubCategory(subCategory: String) {
        viewModelScope.launch {
            _selectedSubCategory.value = subCategory
            _sortOrder.value = "최신순"
        }
    }

    fun updateSortOrder(sortOrder: String) {
        _sortOrder.value = sortOrder
    }

    fun markPostAsViewed(post: Post) {
        viewModelScope.launch {
            if (userId != null) {
                try {
                    // Firestore에 viewed 상태 업데이트
                    val userRef = firestore.collection("users").document(userId)
                    val viewedRef = userRef.collection("viewed").document(post.id)
                    viewedRef.set(emptyMap<String, Any>(), SetOptions.merge()).await()

                    //_posts에 반영
                    _posts.value = _posts.value.map { existingPost ->
                        if (existingPost.id == post.id) existingPost.copy(isViewed = true)
                        else existingPost
                    }

                    Log.d("MainViewModel", "Post marked as viewed successfully")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error marking post as viewed", e)
                }
            }
        }
    }

    private suspend fun isDocumentViewed(documentId: String): Boolean {
        return try {
            if (userId != null) {
                val userRef = firestore.collection("users").document(userId)
                val viewedRef = userRef.collection("viewed").document(documentId)
                val snapshot = viewedRef.get().await()
                snapshot.exists()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error checking document view status", e)
            false
        }
    }

    fun updateUserKeywords(postKeywords: List<String>) {
        if (userId != null) {
            val userRef = firestore.collection("users").document(userId)

            postKeywords.forEach { keyword ->
                userRef.update("favoriteKeywords.$keyword", FieldValue.increment(1))
            }

            getUserKeywords()
        }
    }

    fun fetchRecommendedPosts() {
        viewModelScope.launch {
            try {
                val userSimilar = userId?.let { fetchRecommendedPostsByType(it, "user_similar") }
                val contentSimilar = userId?.let { fetchRecommendedPostsByType(it, "content_similar") }

                if (userSimilar != null) {
                    _userSimilarPosts.value = userSimilar
                }
                if (contentSimilar != null) {
                    _contentSimilarPosts.value = contentSimilar
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching recommended posts", e)
            }
        }
    }

    private suspend fun fetchRecommendedPostsByType(userId: String, documentId: String): List<Post> {
        val postIds = firestore.collection("users")
            .document(userId)
            .collection("recommendPosts")
            .document(documentId)
            .get()
            .await()
            .get("items") as? List<String> ?: emptyList()

        return postIds.mapNotNull { postId ->
            getPostById(postId)
            _post.value
        }
    }

    private fun getUserKeywords() {
        val keywords = mutableListOf<String>()

        if (userId != null) {
            val userRef = firestore.collection("users").document(userId)

            userRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val favoriteKeywords = documentSnapshot.get("favoriteKeywords") as? Map<String, Long>
                    if (favoriteKeywords != null) {
                        // 빈도수가 높은 순으로 5개 키워드를 가져옴
                        keywords.addAll(
                            favoriteKeywords
                                .toList()
                                .sortedByDescending { (_, frequency) -> frequency }
                                .take(5)
                                .map { (keyword, _) -> keyword }
                        )
                    }

                    _recommendTags.value = keywords
                }
            }.addOnFailureListener { e ->
                Log.d("MainViewModel", "Error fetching user keywords", e)
            }
        }
    }

    // 스케줄 날짜별 필터링 함수
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        Log.d("get schddules for date","$date")
        return _allSchedules.value.filter {
            val startDate = LocalDate.parse(it.startDate)
            val endDate = LocalDate.parse(it.endDate)
            date in startDate..endDate
        }
    }

    // 선택된 날짜의 스케줄 업데이트
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSelectedDate(date: LocalDate) {
        _selectedDateSchedules.value = getSchedulesForDate(date)
    }

    suspend fun getAllSchedules() {
        if (userId != null) {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("schedules")
                    .get()
                    .await()

                val schedules = snapshot.toObjects(Schedule::class.java)
                _allSchedules.value = schedules
                _schedulePostIds.value = schedules.map { it.id }.toSet()
            } catch (e: Exception) {
                // 예외 처리
                _allSchedules.value = emptyList()
                _schedulePostIds.value = emptySet()
            }
        } else {
            // 사용자 ID가 없는 경우 빈 리스트 설정
            _allSchedules.value = emptyList()
            _schedulePostIds.value = emptySet()
        }
    }

    fun addSchedulePostId(postId: String) {
        _schedulePostIds.update { it + postId }
    }

    suspend fun getPostById(postId: String) {
        try {
            // 먼저 일반 posts 컬렉션에서 찾기
            var documentSnapshot = firestore.collection("posts").document(postId).get().await()

            // 일반 posts 컬렉션에 없다면 users 컬렉션 내의 posts에서 찾기
            if (!documentSnapshot.exists()) {
                documentSnapshot = userId?.let {
                    firestore.collection("users").document(it)
                        .collection("posts").document(postId).get().await()
                }
            }
            if (documentSnapshot.exists()) {
                val post = createPostObject(documentSnapshot)
                _post.value = post
            } else {
                Log.w("MainViewModel", "No document found with ID: $postId")
                _post.value = null
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error fetching post by $postId", e)
            _post.value = null
        }
    }

    fun getFormById(formId: String): Flow<Form?> {
        // 데이터베이스에서 Form 데이터를 찾고 Flow로 반환하는 로직
        return flow {
            val form = forms.value.find { it.id == formId }
            emit(form)
        }
    }

    // 폼 데이터를 로드하는 함수
    private fun loadForms(major: String) {
        database.child("forms").orderByChild("startDate")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val formsList = snapshot.children.mapNotNull { it.getValue(Form::class.java) }
                        .filter { it.major == major || it.major == "FA" }

                    // _forms.value를 메인 스레드에서 업데이트
                    viewModelScope.launch(Dispatchers.Main) {
                        _forms.value = formsList
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Failed to load forms", error.toException())
                }
            })
    }
}