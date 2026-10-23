package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.PulseSocialRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CreateOrEditPostSheet
import com.example.ui.components.ProfileInspectorDialog
import com.example.ui.components.UserAvatar
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.PulseSocialTheme
import com.example.ui.viewmodel.PulseSocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseSocialTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val defaultName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Pulse Member"
        val defaultHandle = "@" + (user.email?.substringBefore("@")
            ?.replace(Regex("[^a-zA-Z0-9_]"), "_")
            ?: "member_${user.uid.take(5)}")

        PulseSocialMainApp(
            currentUserId = user.uid,
            defaultDisplayName = defaultName,
            defaultHandle = defaultHandle
        )
    }
}

private enum class MainTab {
    FEED,
    FRIENDS,
    NOTIFICATIONS,
    PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseSocialMainApp(
    currentUserId: String,
    defaultDisplayName: String,
    defaultHandle: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: PulseSocialViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PulseSocialViewModel(
                    repository = PulseSocialRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName,
                    defaultHandle = defaultHandle
                )
            }
        }
    )

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val publicPostsState by viewModel.publicPostsState.collectAsStateWithLifecycle()
    val myPostsState by viewModel.myPostsState.collectAsStateWithLifecycle()
    val publicProfilesState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendshipsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeCommentPost by viewModel.activeCommentPost.collectAsStateWithLifecycle()
    val activePostCommentsState by viewModel.activePostCommentsState.collectAsStateWithLifecycle()
    val inspectingProfile by viewModel.inspectingProfile.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.FEED) }
    var showCreatePostSheet by rememberSaveable { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }

    if (currentTab != MainTab.FEED) {
        BackHandler {
            currentTab = MainTab.FEED
        }
    }

    val myProfile = (myProfileState as? UiState.Success)?.data ?: viewModel.currentOrFallbackProfile()
    val pendingFriendsCount = remember(friendshipsState) {
        val list = (friendshipsState as? UiState.Success)?.data ?: emptyList()
        list.count { it.status == "pending" }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            UserAvatar(
                                name = myProfile.displayName,
                                themeKey = myProfile.avatarTheme,
                                size = 36.dp,
                                showOnlineBadge = true,
                                onClick = { currentTab = MainTab.PROFILE }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (currentTab == MainTab.FEED) {
                    ExtendedFloatingActionButton(
                        onClick = { showCreatePostSheet = true },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("New Post") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_create_post")
                    )
                }
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == MainTab.FEED,
                            onClick = { currentTab = MainTab.FEED },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.FEED) Icons.Default.Home else Icons.Outlined.Home,
                                    contentDescription = stringResource(R.string.nav_feed)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_feed)) },
                            modifier = Modifier.testTag("nav_feed")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.FRIENDS,
                            onClick = { currentTab = MainTab.FRIENDS },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (pendingFriendsCount > 0) {
                                            Badge { Text("$pendingFriendsCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.FRIENDS) Icons.Default.Groups else Icons.Outlined.Groups,
                                        contentDescription = stringResource(R.string.nav_friends)
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.nav_friends)) },
                            modifier = Modifier.testTag("nav_friends")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.NOTIFICATIONS,
                            onClick = { currentTab = MainTab.NOTIFICATIONS },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.NOTIFICATIONS) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = stringResource(R.string.nav_activity)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_activity)) },
                            modifier = Modifier.testTag("nav_activity")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.PROFILE,
                            onClick = { currentTab = MainTab.PROFILE },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                                    contentDescription = stringResource(R.string.nav_profile)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_profile)) },
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = currentTab == MainTab.FEED,
                            onClick = { currentTab = MainTab.FEED },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
                            label = { Text("Feed") }
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.FRIENDS,
                            onClick = { currentTab = MainTab.FRIENDS },
                            icon = { Icon(Icons.Default.Groups, contentDescription = "Friends") },
                            label = { Text("Friends") }
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.NOTIFICATIONS,
                            onClick = { currentTab = MainTab.NOTIFICATIONS },
                            icon = { Icon(Icons.Default.Notifications, contentDescription = "Activity") },
                            label = { Text("Activity") }
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.PROFILE,
                            onClick = { currentTab = MainTab.PROFILE },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") }
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    ) {
                        when (currentTab) {
                            MainTab.FEED -> {
                                FeedScreen(
                                    myProfile = myProfile,
                                    postsState = publicPostsState,
                                    friendshipsState = friendshipsState,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onOpenCreatePost = { showCreatePostSheet = true },
                                    onLikePost = viewModel::toggleLike,
                                    onCommentPost = viewModel::openComments,
                                    onAuthorClick = { post ->
                                        if (post.authorId == currentUserId) {
                                            currentTab = MainTab.PROFILE
                                        } else {
                                            viewModel.inspectProfile(
                                                UserProfile(
                                                    id = post.authorId,
                                                    userId = post.authorId,
                                                    displayName = post.authorName,
                                                    handle = post.authorHandle,
                                                    avatarTheme = post.authorAvatarTheme
                                                )
                                            )
                                        }
                                    },
                                    onEditPost = { post -> editingPost = post },
                                    onDeletePost = { post -> viewModel.deletePost(post.id) },
                                    onPublishQuickStarterPost = { content, feeling, mediaPreset, category ->
                                        viewModel.createPost(content, feeling, mediaPreset, category, "public")
                                    }
                                )
                            }

                            MainTab.FRIENDS -> {
                                FriendsScreen(
                                    currentUserId = currentUserId,
                                    friendshipsState = friendshipsState,
                                    publicProfilesState = publicProfilesState,
                                    onAcceptRequest = viewModel::acceptFriendRequest,
                                    onDeclineRequest = viewModel::declineFriendRequest,
                                    onRemoveFriendship = viewModel::removeFriendship,
                                    onSendRequestToProfile = viewModel::sendFriendRequestToUser,
                                    onSendCustomConnection = viewModel::sendCustomFriendConnection,
                                    onInspectProfile = viewModel::inspectProfile
                                )
                            }

                            MainTab.NOTIFICATIONS -> {
                                NotificationsScreen(
                                    currentUserId = currentUserId,
                                    postsState = publicPostsState,
                                    friendshipsState = friendshipsState,
                                    onOpenPostComments = viewModel::openComments
                                )
                            }

                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    myProfile = myProfile,
                                    myPostsState = myPostsState,
                                    friendshipsState = friendshipsState,
                                    onSaveProfile = viewModel::saveMyProfile,
                                    onLikePost = viewModel::toggleLike,
                                    onCommentPost = viewModel::openComments,
                                    onEditPost = { post -> editingPost = post },
                                    onDeletePost = { post -> viewModel.deletePost(post.id) },
                                    onSignOut = {
                                        signOutUser(
                                            context = context,
                                            onSignOutComplete = {},
                                            scope = scope
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePostSheet) {
        CreateOrEditPostSheet(
            initialPost = null,
            onDismiss = { showCreatePostSheet = false },
            onSubmit = { content, feeling, mediaPreset, category, visibility ->
                viewModel.createPost(content, feeling, mediaPreset, category, visibility)
            }
        )
    }

    val postBeingEdited = editingPost
    if (postBeingEdited != null) {
        CreateOrEditPostSheet(
            initialPost = postBeingEdited,
            onDismiss = { editingPost = null },
            onSubmit = { content, feeling, mediaPreset, category, visibility ->
                viewModel.updatePost(
                    postId = postBeingEdited.id,
                    content = content,
                    feeling = feeling,
                    mediaPreset = mediaPreset,
                    category = category,
                    visibility = visibility
                )
            }
        )
    }

    val commentPost = activeCommentPost
    if (commentPost != null) {
        CommentsBottomSheet(
            post = commentPost,
            commentsState = activePostCommentsState,
            currentUserId = currentUserId,
            onDismiss = viewModel::closeComments,
            onAddComment = viewModel::addComment,
            onDeleteComment = viewModel::deleteComment
        )
    }

    val profileToInspect = inspectingProfile
    if (profileToInspect != null) {
        val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()
        val isConnected = friendships.any {
            (it.status == "accepted" || it.status == "pending") &&
                it.memberUids.contains(profileToInspect.userId)
        }
        ProfileInspectorDialog(
            profile = profileToInspect,
            currentUserId = currentUserId,
            isAlreadyConnectedOrPending = isConnected,
            onDismiss = { viewModel.inspectProfile(null) },
            onAddFriend = viewModel::sendFriendRequestToUser
        )
    }
}
