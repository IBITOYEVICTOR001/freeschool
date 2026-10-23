package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.ui.components.formatRelativeTimestamp
import com.google.firebase.Timestamp

private data class ActivityNotificationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: Timestamp?,
    val icon: ImageVector,
    val badgeColor: Color,
    val relatedPost: Post? = null
)

@Composable
fun NotificationsScreen(
    currentUserId: String,
    postsState: UiState<List<Post>>,
    friendshipsState: UiState<List<Friendship>>,
    onOpenPostComments: (Post) -> Unit
) {
    val activityItems = remember(postsState, friendshipsState, currentUserId) {
        val list = mutableListOf<ActivityNotificationItem>()
        val posts = (postsState as? UiState.Success)?.data ?: emptyList()
        val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()

        // 1. Reactions and comments on user's posts
        posts.filter { it.authorId == currentUserId }.forEach { post ->
            if (post.likesCount > 0) {
                list.add(
                    ActivityNotificationItem(
                        id = "like_${post.id}_${post.likesCount}",
                        title = "${post.likesCount} ${if (post.likesCount == 1L) "person reacted" else "people reacted"} to your post",
                        subtitle = "\"${post.content}\"",
                        timestamp = post.updatedAt ?: post.createdAt,
                        icon = Icons.Default.Favorite,
                        badgeColor = Color(0xFFFF4D6D),
                        relatedPost = post
                    )
                )
            }
            if (post.commentsCount > 0) {
                list.add(
                    ActivityNotificationItem(
                        id = "cmt_${post.id}_${post.commentsCount}",
                        title = "${post.commentsCount} ${if (post.commentsCount == 1L) "comment" else "comments"} on your post",
                        subtitle = "\"${post.content}\"",
                        timestamp = post.updatedAt ?: post.createdAt,
                        icon = Icons.Default.ChatBubble,
                        badgeColor = Color(0xFF1877F2),
                        relatedPost = post
                    )
                )
            }
        }

        // 2. Friendship events
        friendships.forEach { fr ->
            val otherName = if (fr.requesterId == currentUserId) fr.receiverName else fr.requesterName
            val statusTitle = when (fr.status) {
                "accepted" -> "You and $otherName are now connected on Pulse Social"
                "pending" -> "Pending friend request with $otherName"
                else -> "Friend request update with $otherName"
            }
            list.add(
                ActivityNotificationItem(
                    id = "fr_${fr.id}_${fr.status}",
                    title = statusTitle,
                    subtitle = if (fr.requesterId == currentUserId) fr.receiverHandle else fr.requesterHandle,
                    timestamp = fr.updatedAt ?: fr.createdAt,
                    icon = Icons.Default.People,
                    badgeColor = Color(0xFF10B981)
                )
            )
        }

        list.sortedByDescending { it.timestamp ?: Timestamp(0, 0) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Activity & Notifications",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Real-time updates on your posts, reactions, and friend connections",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activityItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "All Caught Up!",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When friends react to your posts, leave comments, or connect with you, live alerts will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activityItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (item.relatedPost != null) {
                                    Modifier.clickable { onOpenPostComments(item.relatedPost) }
                                } else {
                                    Modifier
                                }
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = item.badgeColor.copy(alpha = 0.14f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.badgeColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.subtitle.isNotBlank()) {
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRelativeTimestamp(item.timestamp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
