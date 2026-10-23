package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.Friendship
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.PulseSocialRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PulseSocialViewModel(
    private val repository: PulseSocialRepository,
    val currentUserId: String,
    private val defaultDisplayName: String = "Pulse Member",
    private val defaultHandle: String = "@pulse_member"
) : ViewModel() {

    private var profileAutoCreated = false

    val myProfileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { profile ->
            if (profile == null && !profileAutoCreated) {
                profileAutoCreated = true
                autoInitializeProfile()
            }
            UiState.Success(profile)
        }
        .catch { error ->
            Log.w(TAG, "Error observing user profile", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val publicPostsState: StateFlow<UiState<List<Post>>> = repository.observePublicPosts()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing public posts", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load news feed"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val myPostsState: StateFlow<UiState<List<Post>>> = repository.observeUserPosts(currentUserId)
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing user posts", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load your posts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val publicProfilesState: StateFlow<UiState<List<UserProfile>>> = repository.observePublicProfiles()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing public profiles", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load people directory"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val friendshipsState: StateFlow<UiState<List<Friendship>>> = repository.observeFriendships(currentUserId)
        .map<List<Friendship>, UiState<List<Friendship>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing friendships", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load friendships"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    val activeCommentPost: StateFlow<Post?> = _activeCommentPost.asStateFlow()

    val activePostCommentsState: StateFlow<UiState<List<Comment>>> = _activeCommentPost
        .flatMapLatest { post ->
            if (post == null) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(post.id)
                    .map<List<Comment>, UiState<List<Comment>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing comments for post ${post.id}", error)
                        emit(UiState.Error(error.localizedMessage ?: "Failed to load comments"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    private val _inspectingProfile = MutableStateFlow<UserProfile?>(null)
    val inspectingProfile: StateFlow<UserProfile?> = _inspectingProfile.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private fun autoInitializeProfile() {
        viewModelScope.launch {
            repository.saveProfile(
                displayName = defaultDisplayName,
                handle = defaultHandle,
                bio = "Exploring real-time connections and updates on Pulse Social",
                workplace = "Creator Community",
                hometown = "Global Network",
                avatarTheme = "ocean",
                coverTheme = "aurora"
            )
        }
    }

    fun currentOrFallbackProfile(): UserProfile {
        val state = myProfileState.value
        val existing = (state as? UiState.Success)?.data
        return existing ?: UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = defaultDisplayName,
            handle = defaultHandle,
            bio = "Exploring real-time connections and updates on Pulse Social",
            workplace = "Creator Community",
            hometown = "Global Network",
            avatarTheme = "ocean",
            coverTheme = "aurora",
            visibility = "public"
        )
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openComments(post: Post) {
        _activeCommentPost.value = post
    }

    fun closeComments() {
        _activeCommentPost.value = null
    }

    fun inspectProfile(profile: UserProfile?) {
        _inspectingProfile.value = profile
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun createPost(
        content: String,
        feeling: String,
        mediaPreset: String,
        category: String,
        visibility: String = "public"
    ) {
        if (content.isBlank()) return
        val profile = currentOrFallbackProfile()
        viewModelScope.launch {
            val result = repository.createPost(
                authorProfile = profile,
                content = content,
                feeling = feeling,
                mediaPreset = mediaPreset,
                category = category,
                visibility = visibility
            )
            result.onSuccess {
                _snackbarMessage.value = "Post shared to the live feed!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not publish post"
            }
        }
    }

    fun updatePost(
        postId: String,
        content: String,
        feeling: String,
        mediaPreset: String,
        category: String,
        visibility: String
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val result = repository.updatePostContent(
                postId = postId,
                content = content,
                feeling = feeling,
                mediaPreset = mediaPreset,
                category = category,
                visibility = visibility
            )
            result.onSuccess {
                _snackbarMessage.value = "Post updated!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update post"
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            repository.togglePostLike(post).onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not update reaction"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onSuccess {
                _snackbarMessage.value = "Post deleted"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not delete post"
            }
        }
    }

    fun addComment(text: String) {
        val post = _activeCommentPost.value ?: return
        if (text.isBlank()) return
        val profile = currentOrFallbackProfile()
        viewModelScope.launch {
            repository.addComment(post, profile, text).onSuccess {
                // Also update local activeCommentPost count so consecutive comments increment accurately
                _activeCommentPost.value = post.copy(commentsCount = post.commentsCount + 1L)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to post comment"
            }
        }
    }

    fun deleteComment(commentId: String) {
        val post = _activeCommentPost.value ?: return
        viewModelScope.launch {
            repository.deleteComment(post.id, commentId, post.commentsCount).onSuccess {
                _activeCommentPost.value = post.copy(
                    commentsCount = (post.commentsCount - 1L).coerceAtLeast(0L)
                )
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to delete comment"
            }
        }
    }

    fun sendFriendRequestToUser(target: UserProfile) {
        if (target.userId == currentUserId) return
        val myProfile = currentOrFallbackProfile()
        viewModelScope.launch {
            repository.sendFriendRequest(
                myProfile = myProfile,
                targetId = target.userId,
                targetName = target.displayName,
                targetHandle = target.handle,
                targetBio = target.bio,
                targetAvatarTheme = target.avatarTheme,
                initialStatus = "pending"
            ).onSuccess {
                _snackbarMessage.value = "Friend request sent to ${target.displayName}!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not send friend request"
            }
        }
    }

    fun sendCustomFriendConnection(
        name: String,
        handle: String,
        bio: String,
        avatarTheme: String,
        status: String = "pending"
    ) {
        if (name.isBlank()) return
        val myProfile = currentOrFallbackProfile()
        val cleanHandle = if (handle.startsWith("@")) handle else "@${handle.ifBlank { name.lowercase().replace(" ", "_") }}"
        val syntheticTargetId = "member_${cleanHandle.removePrefix("@").replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
        if (syntheticTargetId == currentUserId) return

        viewModelScope.launch {
            repository.sendFriendRequest(
                myProfile = myProfile,
                targetId = syntheticTargetId,
                targetName = name.trim(),
                targetHandle = cleanHandle.trim(),
                targetBio = bio.trim().ifBlank { "Pulse Social Member" },
                targetAvatarTheme = avatarTheme,
                initialStatus = status
            ).onSuccess {
                _snackbarMessage.value = if (status == "accepted") {
                    "Connected with ${name.trim()}!"
                } else {
                    "Friend request sent to ${name.trim()}!"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not create connection"
            }
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.respondToFriendRequest(friendshipId, "accepted").onSuccess {
                _snackbarMessage.value = "Friend connection confirmed!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not confirm request"
            }
        }
    }

    fun declineFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.respondToFriendRequest(friendshipId, "declined").onSuccess {
                _snackbarMessage.value = "Friend request declined"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not decline request"
            }
        }
    }

    fun removeFriendship(friendshipId: String) {
        viewModelScope.launch {
            repository.removeFriendship(friendshipId).onSuccess {
                _snackbarMessage.value = "Connection removed"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not remove connection"
            }
        }
    }

    fun saveMyProfile(
        displayName: String,
        handle: String,
        bio: String,
        workplace: String,
        hometown: String,
        avatarTheme: String,
        coverTheme: String
    ) {
        viewModelScope.launch {
            repository.saveProfile(
                displayName = displayName,
                handle = handle,
                bio = bio,
                workplace = workplace,
                hometown = hometown,
                avatarTheme = avatarTheme,
                coverTheme = coverTheme
            ).onSuccess {
                _snackbarMessage.value = "Profile updated!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    companion object {
        private const val TAG = "PulseSocialVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
