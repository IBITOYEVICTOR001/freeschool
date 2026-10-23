package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.Friendship
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarThemes
import com.example.ui.components.CoverBannerCanvas
import com.example.ui.components.CoverThemes
import com.example.ui.components.PostCardItem
import com.example.ui.components.UserAvatar

@Composable
fun ProfileScreen(
    myProfile: UserProfile,
    myPostsState: UiState<List<Post>>,
    friendshipsState: UiState<List<Friendship>>,
    onSaveProfile: (displayName: String, handle: String, bio: String, workplace: String, hometown: String, avatarTheme: String, coverTheme: String) -> Unit,
    onLikePost: (Post) -> Unit,
    onCommentPost: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onSignOut: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val myPosts = (myPostsState as? UiState.Success)?.data ?: emptyList()
    val acceptedFriendsCount = remember(friendshipsState) {
        val list = (friendshipsState as? UiState.Success)?.data ?: emptyList()
        list.count { it.status == "accepted" }
    }
    val totalLikesReceived = remember(myPosts) {
        myPosts.sumOf { it.likesCount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Cover Banner & Overlapping Profile Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        CoverBannerCanvas(
                            coverTheme = myProfile.coverTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Overlapping Avatar + Edit/SignOut Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .offset(y = (-36).dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            UserAvatar(
                                name = myProfile.displayName,
                                themeKey = myProfile.avatarTheme,
                                size = 84.dp,
                                showOnlineBadge = true
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                            .offset(y = (-16).dp)
                    ) {
                        Text(
                            text = myProfile.displayName,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = myProfile.handle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (myProfile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = myProfile.bio,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (myProfile.workplace.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Work,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = myProfile.workplace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (myProfile.hometown.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = myProfile.hometown,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Social Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatItem(count = "${myPosts.size}", label = "Posts")
                            ProfileStatItem(count = "$acceptedFriendsCount", label = "Friends")
                            ProfileStatItem(count = "$totalLikesReceived", label = "Reactions")
                        }
                    }
                }
            }
        }

        // 2. Personal Timeline Header
        item {
            Text(
                text = "Your Timeline (${myPosts.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (myPosts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No timeline posts yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Posts you share on the News Feed will appear here on your personal wall.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(myPosts, key = { it.id }) { post ->
                PostCardItem(
                    post = post,
                    currentUserId = myProfile.userId,
                    onLikeClick = onLikePost,
                    onCommentClick = onCommentPost,
                    onAuthorClick = {},
                    onEditClick = onEditPost,
                    onDeleteClick = onDeletePost
                )
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = myProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { displayName, handle, bio, workplace, hometown, avatarTheme, coverTheme ->
                onSaveProfile(displayName, handle, bio, workplace, hometown, avatarTheme, coverTheme)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
private fun ProfileStatItem(count: String, label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (displayName: String, handle: String, bio: String, workplace: String, hometown: String, avatarTheme: String, coverTheme: String) -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var bio by remember { mutableStateOf(profile.bio) }
    var workplace by remember { mutableStateOf(profile.workplace) }
    var hometown by remember { mutableStateOf(profile.hometown) }
    var avatarTheme by remember { mutableStateOf(profile.avatarTheme) }
    var coverTheme by remember { mutableStateOf(profile.coverTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Your Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Handle") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_handle_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_bio_input")
                )
                OutlinedTextField(
                    value = workplace,
                    onValueChange = { workplace = it },
                    label = { Text("Workplace / Role") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hometown,
                    onValueChange = { hometown = it },
                    label = { Text("Location / City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Avatar Theme",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarThemes.keys.forEach { key ->
                        FilterChip(
                            selected = avatarTheme == key,
                            onClick = { avatarTheme = key },
                            label = { Text(key.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Text(
                    text = "Cover Banner Theme",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoverThemes.keys.forEach { key ->
                        FilterChip(
                            selected = coverTheme == key,
                            onClick = { coverTheme = key },
                            label = { Text(key.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.isNotBlank()) {
                        onSave(displayName, handle, bio, workplace, hometown, avatarTheme, coverTheme)
                    }
                },
                enabled = displayName.isNotBlank(),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
