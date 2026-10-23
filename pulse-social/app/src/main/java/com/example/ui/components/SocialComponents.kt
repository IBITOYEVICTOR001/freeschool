package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp

val AvatarThemes = mapOf(
    "ocean" to listOf(Color(0xFF1877F2), Color(0xFF00B4D8)),
    "sunset" to listOf(Color(0xFFFF4D6D), Color(0xFFFF9E00)),
    "emerald" to listOf(Color(0xFF10B981), Color(0xFF059669)),
    "violet" to listOf(Color(0xFF7C3AED), Color(0xFFEC4899)),
    "amber" to listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    "midnight" to listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
)

val CoverThemes = mapOf(
    "aurora" to listOf(Color(0xFF1877F2), Color(0xFF6366F1), Color(0xFFEC4899)),
    "pacific" to listOf(Color(0xFF0F172A), Color(0xFF1E40AF), Color(0xFF0EA5E9)),
    "coral_glow" to listOf(Color(0xFF881337), Color(0xFFE11D48), Color(0xFFF97316)),
    "forest_mist" to listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF34D399))
)

val PostMediaPresets = listOf(
    "" to "None",
    "banner_aurora" to "Aurora Wave",
    "banner_celebration" to "Milestone Gold",
    "banner_cyber" to "Electric Grid",
    "banner_sunset" to "Warm Horizon"
)

val FeelingOptions = listOf(
    "",
    "Feeling excited",
    "Celebrating a milestone",
    "Feeling grateful",
    "Building something new",
    "Looking for recommendations",
    "Feeling inspired"
)

fun formatRelativeTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val timeMs = timestamp.toDate().time
    val nowMs = System.currentTimeMillis()
    if (nowMs - timeMs < 45_000L) return "Just now"
    return DateUtils.getRelativeTimeSpanString(
        timeMs,
        nowMs,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

@Composable
fun UserAvatar(
    name: String,
    themeKey: String,
    size: Dp = 44.dp,
    showOnlineBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = AvatarThemes[themeKey] ?: AvatarThemes["ocean"]!!
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "P" }
    }

    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(CircleShape)
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = if (size >= 64.dp) {
                    MaterialTheme.typography.headlineMedium
                } else if (size >= 40.dp) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.28f).dp.coerceAtLeast(10.dp))
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun CoverBannerCanvas(
    coverTheme: String,
    modifier: Modifier = Modifier
) {
    val colors = CoverThemes[coverTheme] ?: CoverThemes["aurora"]!!
    Box(
        modifier = modifier
            .background(Brush.linearGradient(colors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = h * 0.8f,
                center = Offset(w * 0.15f, h * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = h * 0.65f,
                center = Offset(w * 0.85f, h * 0.75f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = h * 0.45f,
                center = Offset(w * 0.52f, h * 0.15f)
            )
        }
    }
}

@Composable
fun StoriesHighlightsBar(
    myProfile: UserProfile,
    recentPosts: List<Post>,
    onCreateStoryClick: () -> Unit,
    onStoryClick: (Post) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Create Story / Status Card
            Card(
                modifier = Modifier
                    .width(106.dp)
                    .height(164.dp)
                    .clickable { onCreateStoryClick() }
                    .testTag("create_story_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        CoverBannerCanvas(
                            coverTheme = myProfile.coverTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatar(
                                name = myProfile.displayName,
                                themeKey = myProfile.avatarTheme,
                                size = 46.dp
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.BottomCenter)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Share Moment",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        val storyPosts = recentPosts.take(8)
        items(storyPosts, key = { "story_${it.id}" }) { post ->
            val gradient = when (post.category) {
                "milestone" -> listOf(Color(0xFFF59E0B), Color(0xFFEC4899))
                "question" -> listOf(Color(0xFF7C3AED), Color(0xFF2563EB))
                "photo" -> listOf(Color(0xFF059669), Color(0xFF0EA5E9))
                else -> listOf(Color(0xFF1877F2), Color(0xFF4F46E5))
            }
            Card(
                modifier = Modifier
                    .width(106.dp)
                    .height(164.dp)
                    .clickable { onStoryClick(post) },
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(gradient))
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .padding(2.dp)
                    ) {
                        UserAvatar(
                            name = post.authorName,
                            themeKey = post.authorAvatarTheme,
                            size = 32.dp
                        )
                    }

                    Column(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = post.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = post.authorName.split(" ").firstOrNull() ?: post.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCardItem(
    post: Post,
    currentUserId: String,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onAuthorClick: (Post) -> Unit,
    onEditClick: (Post) -> Unit,
    onDeleteClick: (Post) -> Unit
) {
    val context = LocalContext.current
    val isLiked = post.likedByUids.contains(currentUserId)
    val isOwnPost = post.authorId == currentUserId
    var showMenu by remember { mutableStateOf(false) }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "likeScale"
    )
    val likeColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF4D6D) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "likeColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = post.authorName,
                    themeKey = post.authorAvatarTheme,
                    size = 44.dp,
                    showOnlineBadge = true,
                    onClick = { onAuthorClick(post) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick(post) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.feeling.isNotBlank()) {
                            Text(
                                text = " • ${post.feeling}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${post.authorHandle} • ${formatRelativeTimestamp(post.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (post.visibility == "friends") Icons.Default.People else Icons.Default.Public,
                            contentDescription = post.visibility,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Category badge
                CategoryPill(category = post.category)

                if (isOwnPost) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("post_menu_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Post") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditClick(post)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick(post)
                                }
                            )
                        }
                    }
                }
            }

            // Post Body Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Optional Visual Banner or Attached Photo
            if (post.mediaPreset.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                PostMediaBanner(
                    mediaPreset = post.mediaPreset,
                    postContent = post.content,
                    feeling = post.feeling
                )
            }

            // Reactions & Comments Counter Summary
            if (post.likesCount > 0 || post.commentsCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (post.likesCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF4D6D),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (post.likesCount == 1L) "1 reaction" else "${post.likesCount} reactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (post.commentsCount > 0) {
                        Text(
                            text = if (post.commentsCount == 1L) "1 comment" else "${post.commentsCount} comments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onCommentClick(post) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Action Bar: Like, Comment, Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = { onLikeClick(post) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(likeScale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLiked) "Liked" else "Like",
                        color = likeColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                TextButton(
                    onClick = { onCommentClick(post) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comment",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                TextButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${post.authorName} (${post.authorHandle}) on Pulse Social:\n\"${post.content}\""
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Post"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(category: String) {
    val (label, color) = when (category) {
        "milestone" -> "Milestone" to Color(0xFFD97706)
        "question" -> "Question" to Color(0xFF7C3AED)
        "photo" -> "Visual" to Color(0xFF059669)
        else -> "Update" to Color(0xFF1877F2)
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PostMediaBanner(
    mediaPreset: String,
    postContent: String,
    feeling: String
) {
    if (mediaPreset.startsWith("content://") || mediaPreset.startsWith("file://") || mediaPreset.startsWith("http")) {
        AsyncImage(
            model = Uri.parse(mediaPreset),
            contentDescription = "Attached post photo",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 280.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        return
    }

    val gradientColors = when (mediaPreset) {
        "banner_celebration" -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEC4899))
        "banner_cyber" -> listOf(Color(0xFF0F172A), Color(0xFF1D4ED8), Color(0xFF06B6D4))
        "banner_sunset" -> listOf(Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFFACC15))
        else -> listOf(Color(0xFF1877F2), Color(0xFF4F46E5), Color(0xFF9333EA))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = size.height * 0.65f,
                center = Offset(size.width * 0.15f, size.height * 0.25f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.height * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.8f)
            )
        }
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (feeling.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = feeling,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = postContent,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditPostSheet(
    initialPost: Post? = null,
    onDismiss: () -> Unit,
    onSubmit: (content: String, feeling: String, mediaPreset: String, category: String, visibility: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var content by remember { mutableStateOf(initialPost?.content ?: "") }
    var feeling by remember { mutableStateOf(initialPost?.feeling ?: "") }
    var mediaPreset by remember { mutableStateOf(initialPost?.mediaPreset ?: "") }
    var category by remember { mutableStateOf(initialPost?.category ?: "status") }
    var visibility by remember { mutableStateOf(initialPost?.visibility ?: "public") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            mediaPreset = uri.toString()
            category = "photo"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialPost == null) "Create Post" else "Edit Post",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Selection
            Text(
                text = "Post Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "status" to "Status",
                    "milestone" to "Milestone",
                    "question" to "Question",
                    "photo" to "Visual"
                )
                categories.forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's on your mind? Share an update, milestone, or question...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("post_content_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Photo Picker + Visual Card Banner Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Visual Card Style or Photo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    label = { Text("Pick Photo") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostMediaPresets.forEach { (presetKey, label) ->
                    FilterChip(
                        selected = mediaPreset == presetKey,
                        onClick = { mediaPreset = presetKey },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Feeling / Activity Chips
            Text(
                text = "Feeling / Activity",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FeelingOptions.forEach { option ->
                    val label = if (option.isEmpty()) "None" else option
                    FilterChip(
                        selected = feeling == option,
                        onClick = { feeling = option },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audience Visibility
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Audience:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = visibility == "public",
                    onClick = { visibility = "public" },
                    label = { Text("Public Feed") },
                    leadingIcon = {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSubmit(content.trim(), feeling, mediaPreset, category, visibility)
                        onDismiss()
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_post_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (initialPost == null) "Publish to News Feed" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    commentsState: UiState<List<Comment>>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments (${post.commentsCount})",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close comments")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (commentsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Loading comments...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = commentsState.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is UiState.Success -> {
                        val comments = commentsState.data
                        if (comments.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No comments yet. Start the conversation!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(comments, key = { it.id }) { comment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        UserAvatar(
                                            name = comment.authorName,
                                            themeKey = comment.authorAvatarTheme,
                                            size = 36.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = comment.authorName,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = formatRelativeTimestamp(comment.createdAt),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = comment.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        if (comment.authorId == currentUserId) {
                                            IconButton(
                                                onClick = { onDeleteComment(comment.id) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete comment",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Comment Composer Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText.trim())
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (commentText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInspectorDialog(
    profile: UserProfile,
    currentUserId: String,
    isAlreadyConnectedOrPending: Boolean,
    onDismiss: () -> Unit,
    onAddFriend: (UserProfile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = profile.displayName,
                    themeKey = profile.avatarTheme,
                    size = 52.dp,
                    showOnlineBadge = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = profile.handle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (profile.workplace.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.workplace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (profile.hometown.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.hometown,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (profile.userId != currentUserId && !isAlreadyConnectedOrPending) {
                Button(
                    onClick = {
                        onAddFriend(profile)
                        onDismiss()
                    }
                ) {
                    Text("Add Friend")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (profile.userId != currentUserId && !isAlreadyConnectedOrPending) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
