package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Comment
import com.example.data.model.Friendship
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PulseSocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Real-time Observation Flows ---

    fun observeProfile(userId: String): Flow<UserProfile?> {
        val path = "profiles/$userId"
        return db.collection("profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observePublicProfiles(): Flow<List<UserProfile>> {
        val path = "profiles"
        return db.collection("profiles")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePublicPosts(): Flow<List<Post>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeUserPosts(authorId: String): Flow<List<Post>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("authorId", authorId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeComments(postId: String): Flow<List<Comment>> {
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("postId", postId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeFriendships(userId: String): Flow<List<Friendship>> {
        val path = "friendships"
        return db.collection("friendships")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // --- Profile CRUD ---

    suspend fun saveProfile(
        displayName: String,
        handle: String,
        bio: String,
        workplace: String,
        hometown: String,
        avatarTheme: String,
        coverTheme: String
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("profiles").document(uid)
        val cleanName = displayName.trim().ifBlank { "Pulse Member" }.take(60)
        val cleanHandle = handle.trim().let {
            val base = if (it.startsWith("@")) it else "@$it"
            if (base.length < 2) "@member_${uid.take(5)}" else base
        }.take(40)
        val cleanBio = bio.trim().take(300)
        val cleanWork = workplace.trim().take(80)
        val cleanHome = hometown.trim().take(80)
        val cleanAvatar = avatarTheme.ifBlank { "ocean" }.take(30)
        val cleanCover = coverTheme.ifBlank { "aurora" }.take(30)

        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "bio" to cleanBio,
                    "workplace" to cleanWork,
                    "hometown" to cleanHome,
                    "avatarTheme" to cleanAvatar,
                    "coverTheme" to cleanCover,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "bio" to cleanBio,
                    "workplace" to cleanWork,
                    "hometown" to cleanHome,
                    "avatarTheme" to cleanAvatar,
                    "coverTheme" to cleanCover,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            val updated = docRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            updated ?: UserProfile(
                id = uid,
                userId = uid,
                displayName = cleanName,
                handle = cleanHandle,
                bio = cleanBio,
                workplace = cleanWork,
                hometown = cleanHome,
                avatarTheme = cleanAvatar,
                coverTheme = cleanCover,
                visibility = "public"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getProfileById(userId: String): Result<UserProfile?> = runCatching {
        val docRef = db.collection("profiles").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    // --- Posts CRUD & Reactions ---

    suspend fun createPost(
        authorProfile: UserProfile,
        content: String,
        feeling: String = "",
        mediaPreset: String = "",
        category: String = "status",
        visibility: String = "public"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("posts").document(postId)
        val payload = mapOf(
            "authorId" to uid,
            "authorName" to authorProfile.displayName.ifBlank { "Pulse User" }.take(60),
            "authorHandle" to authorProfile.handle.ifBlank { "@user_${uid.take(5)}" }.take(40),
            "authorAvatarTheme" to authorProfile.avatarTheme.ifBlank { "ocean" }.take(30),
            "content" to content.trim().take(2000),
            "feeling" to feeling.trim().take(60),
            "mediaPreset" to mediaPreset.trim().take(500),
            "category" to category.ifBlank { "status" },
            "visibility" to visibility.ifBlank { "public" },
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "likedByUids" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            postId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePostContent(
        postId: String,
        content: String,
        feeling: String,
        mediaPreset: String,
        category: String,
        visibility: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("posts").document(postId)
        val updates = mapOf(
            "content" to content.trim().take(2000),
            "feeling" to feeling.trim().take(60),
            "mediaPreset" to mediaPreset.trim().take(500),
            "category" to category,
            "visibility" to visibility,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun togglePostLike(post: Post): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("posts").document(post.id)
        val alreadyLiked = post.likedByUids.contains(uid)
        val updatedUids = if (alreadyLiked) {
            post.likedByUids.filter { it != uid }
        } else {
            (post.likedByUids + uid).distinct().take(50)
        }
        val updatedCount = if (alreadyLiked) {
            (post.likesCount - 1L).coerceAtLeast(0L)
        } else {
            post.likesCount + 1L
        }
        val updates = mapOf(
            "likesCount" to updatedCount,
            "likedByUids" to updatedUids,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("posts").document(postId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Comments CRUD ---

    suspend fun addComment(
        post: Post,
        authorProfile: UserProfile,
        text: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val commentRef = db.collection("posts").document(post.id)
            .collection("comments").document(commentId)
        val postRef = db.collection("posts").document(post.id)

        val commentPayload = mapOf(
            "postId" to post.id,
            "authorId" to uid,
            "authorName" to authorProfile.displayName.ifBlank { "Pulse User" }.take(60),
            "authorAvatarTheme" to authorProfile.avatarTheme.ifBlank { "ocean" }.take(30),
            "text" to text.trim().take(1000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            commentRef.set(commentPayload).await()
            postRef.update(
                mapOf(
                    "commentsCount" to (post.commentsCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            commentId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
    }

    suspend fun deleteComment(
        postId: String,
        commentId: String,
        currentCommentsCount: Long
    ): Result<Unit> = runCatching {
        requireUserId()
        val commentRef = db.collection("posts").document(postId)
            .collection("comments").document(commentId)
        val postRef = db.collection("posts").document(postId)
        try {
            commentRef.delete().await()
            postRef.update(
                mapOf(
                    "commentsCount" to (currentCommentsCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            throw e
        }
    }

    // --- Friendships CRUD ---

    suspend fun sendFriendRequest(
        myProfile: UserProfile,
        targetId: String,
        targetName: String,
        targetHandle: String,
        targetBio: String,
        targetAvatarTheme: String,
        initialStatus: String = "pending"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanTargetId = targetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(60)
        val friendshipId = "fr_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("friendships").document(friendshipId)

        val payload = mapOf(
            "requesterId" to uid,
            "requesterName" to myProfile.displayName.ifBlank { "Pulse User" }.take(60),
            "requesterHandle" to myProfile.handle.ifBlank { "@user_${uid.take(5)}" }.take(40),
            "requesterBio" to myProfile.bio.take(200),
            "requesterAvatarTheme" to myProfile.avatarTheme.ifBlank { "ocean" }.take(30),
            "receiverId" to cleanTargetId,
            "receiverName" to targetName.ifBlank { "Community Friend" }.take(60),
            "receiverHandle" to targetHandle.ifBlank { "@friend" }.take(40),
            "receiverBio" to targetBio.take(200),
            "receiverAvatarTheme" to targetAvatarTheme.ifBlank { "sunset" }.take(30),
            "memberUids" to listOf(uid, cleanTargetId),
            "status" to initialStatus,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            friendshipId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun respondToFriendRequest(
        friendshipId: String,
        newStatus: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("friendships").document(friendshipId)
        val updates = mapOf(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("friendships").document(friendshipId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getFriendshipById(friendshipId: String): Result<Friendship?> = runCatching {
        val docRef = db.collection("friendships").document(friendshipId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }
}
