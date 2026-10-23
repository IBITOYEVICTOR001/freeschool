package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Friendship
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.PostCardItem
import com.example.ui.components.StoriesHighlightsBar
import com.example.ui.components.UserAvatar

@Composable
fun FeedScreen(
    myProfile: UserProfile,
    postsState: UiState<List<Post>>,
    friendshipsState: UiState<List<Friendship>>,
    selectedCategory: String,
    searchQuery: String,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenCreatePost: () -> Unit,
    onLikePost: (Post) -> Unit,
    onCommentPost: (Post) -> Unit,
    onAuthorClick: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onPublishQuickStarterPost: (content: String, feeling: String, mediaPreset: String, category: String) -> Unit
) {
    val friendUids = remember(friendshipsState, myProfile.userId) {
        val list = (friendshipsState as? UiState.Success)?.data ?: emptyList()
        list.filter { it.status == "accepted" }
            .flatMap { it.memberUids }
            .toSet() + myProfile.userId
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("feed_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Composer Bar ("What's on your mind?")
        item {
            ComposerPromptCard(
                myProfile = myProfile,
                onOpenCreatePost = onOpenCreatePost
            )
        }

        // 2. Stories & Highlights Bar
        item {
            val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
            StoriesHighlightsBar(
                myProfile = myProfile,
                recentPosts = allPosts,
                onCreateStoryClick = onOpenCreatePost,
                onStoryClick = { post -> onCommentPost(post) }
            )
        }

        // 3. Search & Category Filter Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search posts, people, or topics...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "all" to "All Feed",
                        "friends" to "Friends & Me",
                        "milestone" to "Milestones",
                        "question" to "Questions",
                        "photo" to "Visuals",
                        "status" to "Updates"
                    )
                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { onSelectCategory(key) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }
        }

        // 4. Live News Feed Content
        when (postsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Unable to load live feed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = postsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            is UiState.Success -> {
                val filteredPosts = postsState.data.filter { post ->
                    val matchesCategory = when (selectedCategory) {
                        "all" -> true
                        "friends" -> friendUids.contains(post.authorId)
                        else -> post.category == selectedCategory
                    }
                    val matchesQuery = searchQuery.isBlank() ||
                        post.content.contains(searchQuery, ignoreCase = true) ||
                        post.authorName.contains(searchQuery, ignoreCase = true) ||
                        post.authorHandle.contains(searchQuery, ignoreCase = true) ||
                        post.feeling.contains(searchQuery, ignoreCase = true)
                    matchesCategory && matchesQuery
                }

                if (filteredPosts.isEmpty()) {
                    item {
                        EmptyFeedCard(
                            onOpenCreatePost = onOpenCreatePost,
                            onPublishQuickStarterPost = onPublishQuickStarterPost
                        )
                    }
                } else {
                    items(filteredPosts, key = { it.id }) { post ->
                        PostCardItem(
                            post = post,
                            currentUserId = myProfile.userId,
                            onLikeClick = onLikePost,
                            onCommentClick = onCommentPost,
                            onAuthorClick = onAuthorClick,
                            onEditClick = onEditPost,
                            onDeleteClick = onDeletePost
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerPromptCard(
    myProfile: UserProfile,
    onOpenCreatePost: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(
                    name = myProfile.displayName,
                    themeKey = myProfile.avatarTheme,
                    size = 44.dp,
                    showOnlineBadge = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onOpenCreatePost() }
                        .testTag("open_composer_bar"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "What's on your mind, ${myProfile.displayName.split(" ").firstOrNull() ?: "friend"}?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onOpenCreatePost) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Photo / Card", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onOpenCreatePost) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Feeling", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onOpenCreatePost) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = Color(0xFFFF4D6D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Milestone", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(
    onOpenCreatePost: () -> Unit,
    onPublishQuickStarterPost: (content: String, feeling: String, mediaPreset: String, category: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.empty_feed_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.empty_feed_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onOpenCreatePost,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_feed_create_post_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Custom Post")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    onPublishQuickStarterPost(
                        "Just joined Pulse Social! Excited to connect with friends and share real-time updates here.",
                        "Feeling excited",
                        "banner_aurora",
                        "milestone"
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quick_starter_post_button")
            ) {
                Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish 1-Tap Welcome Milestone")
            }
        }
    }
}
