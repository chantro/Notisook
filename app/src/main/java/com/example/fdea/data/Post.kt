package com.example.fdea.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

//공지사항 데이터
data class Post(
    val id: String = "",
    val category: String = "",
    val content: String = "",
    val deadline: Timestamp? = null,
    val keywords: List<String> = emptyList(),
    val scraps: Int = 0,
    val title: String = "",
    val uploadTime: Timestamp? = null,
    val url: String = "",
    val views: Int = 0,
    val writer: String = "",
    val courseName:String = "",
    val siteName: String = "",
    val boardName: String = "",
    var isViewed: Boolean = false
)

//게시물의 댓글
data class Comment(
    val id : String = "",
    val userId: String = "",
    val content: String = "",
    val date: String = ""
)

//스크랩한 게시물 관리하는 클라스
class ScrapRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun addScrap(post: Post) {
        userId?.let { uid ->
            val scrapData = mapOf(
                "id" to post.id,
                "siteName" to post.siteName,
                "boardName" to post.boardName,
                "category" to post.category,
                "title" to post.title,
                "deadline" to post.deadline,
            )

            firestore.collection("users")
                .document(uid)
                .collection("scrappedPosts")
                .document(post.id)
                .set(scrapData)
                .await()
        }
    }

    suspend fun removeScrap(postId: String) {
        if (userId != null) {
            // scrappedPosts 삭제
            firestore.collection("users")
                .document(userId)
                .collection("scrappedPosts")
                .document(postId)
                .delete()
                .await()

            // schedules 삭제
            val schedulesRef = firestore.collection("users")
                .document(userId)
                .collection("schedules")
                .document(postId)

            val scheduleSnapshot = schedulesRef.get().await()
            if (scheduleSnapshot.exists()) {
                schedulesRef.delete().await()
            }
        }
    }

    suspend fun getScrappedPosts(): List<Post> {
        if (userId != null) {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("scrappedPosts")
                .get()
                .await()
            return snapshot.toObjects(Post::class.java)
        }
        return emptyList()
    }

    suspend fun isScrapped(post: Post): Boolean {
        if (userId != null) {
            val document = firestore.collection("users")
                .document(userId)
                .collection("scrappedPosts")
                .document(post.id)
                .get()
                .await()
            return document.exists()
        }
        return false
    }
}