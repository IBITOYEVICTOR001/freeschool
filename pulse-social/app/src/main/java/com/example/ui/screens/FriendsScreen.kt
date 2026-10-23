package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarThemes
import com.example.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    currentUserId: String,
    friendshipsState: UiState<List<Friendship>>,
    publicProfilesState: UiState<List<UserProfile>>,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onRemoveFriendship: (String) -> Unit,
    onSendRequestToProfile: (UserProfile) -> Unit,
    onSendCustomConnection: (name: String, handle: String, bio: String, avatarTheme: String, status: String) -> Unit,
    onInspectProfile: (UserProfile) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showConnectByHandleDialog by remember { mutableStateOf(false) }

    val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()
    val pendingRequests = remember(friendships) {
        friendships.filter { it.status == "pending" }
    }
    val acceptedFriends = remember(friendships) {
        friendships.filter { it.status == "accepted" }
    }
    val connectedOrPendingUserIds = remember(friendships, currentUserId) {
        friendships.filter { it.status == "pending" || it.status == "accepted" }
            .flatMap { it.memberUids }
            .toSet() + currentUserId
    }

    val discoverProfiles = remember(publicProfilesState, connectedOrPendingUserIds, searchQuery) {
        val all = (publicProfilesState as? UiState.Success)?.data ?: emptyList()
        all.filter { profile ->
            profile.userId != currentUserId &&
                (searchQuery.isBlank() ||
                    profile.displayName.contains(searchQuery, ignoreCase = true) ||
                    profile.handle.contains(searchQuery, ignoreCase = true) ||
                    profile.bio.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + Connect by Handle action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Friends & Connections",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${acceptedFriends.size} connected • ${pendingRequests.size} pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showConnectByHandleDialog = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("connect_by_handle_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Friend")
            }
        }

        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Requests")
                        if (pendingRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge { Text("${pendingRequests.size}") }
                        }
                    }
                },
                modifier = Modifier.testTag("friends_tab_requests")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Friends (${acceptedFriends.size})") },
                modifier = Modifier.testTag("friends_tab_my_friends")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Discover") },
                modifier = Modifier.testTag("friends_tab_discover")
            )
        }

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search friends or community members...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("friends_search_input")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    val filteredPending = pendingRequests.filter { fr ->
                        searchQuery.isBlank() ||
                            fr.requesterName.contains(searchQuery, ignoreCase = true) ||
                            fr.receiverName.contains(searchQuery, ignoreCase = true) ||
                            fr.requesterHandle.contains(searchQuery, ignoreCase = true) ||
                            fr.receiverHandle.contains(searchQuery, ignoreCase = true)
                    }
                    if (filteredPending.isEmpty()) {
                        item {
                            EmptyFriendsStateCard(
                                title = "No Pending Friend Requests",
                                subtitle = "Send a friend request or connect with community members to grow your network.",
                                actionLabel = "Add Friend or Send Request",
                                onActionClick = { showConnectByHandleDialog = true }
                            )
                        }
                    } else {
                        items(filteredPending, key = { it.id }) { fr ->
                            val isIncoming = fr.receiverId == currentUserId
                            val otherName = if (isIncoming) fr.requesterName else fr.receiverName
                            val otherHandle = if (isIncoming) fr.requesterHandle else fr.receiverHandle
                            val otherBio = if (isIncoming) fr.requesterBio else fr.receiverBio
                            val otherAvatar = if (isIncoming) fr.requesterAvatarTheme else fr.receiverAvatarTheme

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("friend_request_card_${fr.id}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        UserAvatar(
                                            name = otherName,
                                            themeKey = otherAvatar,
                                            size = 52.dp,
                                            showOnlineBadge = true
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = otherName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "$otherHandle • ${if (isIncoming) "Incoming request" else "Pending request"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (otherBio.isNotBlank()) {
                                                Text(
                                                    text = otherBio,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { onAcceptRequest(fr.id) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("accept_request_${fr.id}"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Confirm")
                                        }
                                        OutlinedButton(
                                            onClick = { onDeclineRequest(fr.id) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("decline_request_${fr.id}"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Decline")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val filteredAccepted = acceptedFriends.filter { fr ->
                        searchQuery.isBlank() ||
                            fr.requesterName.contains(searchQuery, ignoreCase = true) ||
                            fr.receiverName.contains(searchQuery, ignoreCase = true) ||
                            fr.requesterHandle.contains(searchQuery, ignoreCase = true) ||
                            fr.receiverHandle.contains(searchQuery, ignoreCase = true)
                    }
                    if (filteredAccepted.isEmpty()) {
                        item {
                            EmptyFriendsStateCard(
                                title = "Build Your Friends Circle",
                                subtitle = "Connect with friends to filter their updates in your Friends feed.",
                                actionLabel = "Connect with a Friend",
                                onActionClick = { showConnectByHandleDialog = true }
                            )
                        }
                    } else {
                        items(filteredAccepted, key = { it.id }) { fr ->
                            val isRequesterMe = fr.requesterId == currentUserId
                            val friendId = if (isRequesterMe) fr.receiverId else fr.requesterId
                            val friendName = if (isRequesterMe) fr.receiverName else fr.requesterName
                            val friendHandle = if (isRequesterMe) fr.receiverHandle else fr.requesterHandle
                            val friendBio = if (isRequesterMe) fr.receiverBio else fr.requesterBio
                            val friendAvatar = if (isRequesterMe) fr.receiverAvatarTheme else fr.requesterAvatarTheme

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("friend_card_${fr.id}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        name = friendName,
                                        themeKey = friendAvatar,
                                        size = 50.dp,
                                        showOnlineBadge = true,
                                        onClick = {
                                            onInspectProfile(
                                                UserProfile(
                                                    id = friendId,
                                                    userId = friendId,
                                                    displayName = friendName,
                                                    handle = friendHandle,
                                                    bio = friendBio,
                                                    avatarTheme = friendAvatar
                                                )
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friendName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = friendHandle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (friendBio.isNotBlank()) {
                                            Text(
                                                text = friendBio,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onRemoveFriendship(fr.id) },
                                        modifier = Modifier.testTag("remove_friend_${fr.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonRemove,
                                            contentDescription = "Remove friend",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (discoverProfiles.isEmpty()) {
                        item {
                            EmptyFriendsStateCard(
                                title = "Community Directory",
                                subtitle = "Other users who sign into Pulse Social appear here automatically, or you can connect directly by handle.",
                                actionLabel = "Connect by Handle",
                                onActionClick = { showConnectByHandleDialog = true }
                            )
                        }
                    } else {
                        items(discoverProfiles, key = { it.userId }) { profile ->
                            val isConnected = connectedOrPendingUserIds.contains(profile.userId)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("discover_profile_${profile.userId}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        name = profile.displayName,
                                        themeKey = profile.avatarTheme,
                                        size = 50.dp,
                                        showOnlineBadge = true,
                                        onClick = { onInspectProfile(profile) }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = profile.handle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (profile.bio.isNotBlank()) {
                                            Text(
                                                text = profile.bio,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isConnected) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Connected",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { onSendRequestToProfile(profile) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("add_friend_${profile.userId}")
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConnectByHandleDialog) {
        AddFriendByHandleDialog(
            onDismiss = { showConnectByHandleDialog = false },
            onSubmit = { name, handle, bio, avatarTheme, status ->
                onSendCustomConnection(name, handle, bio, avatarTheme, status)
                showConnectByHandleDialog = false
            }
        )
    }
}

@Composable
private fun EmptyFriendsStateCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun AddFriendByHandleDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, handle: String, bio: String, avatarTheme: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarTheme by remember { mutableStateOf("sunset") }
    var status by remember { mutableStateOf("pending") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect with a Friend") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Friend's Name") },
                    placeholder = { Text("e.g. Jordan Taylor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_name_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Handle") },
                    placeholder = { Text("e.g. @jordan_taylor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_handle_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Headline / Note") },
                    placeholder = { Text("e.g. Product Designer & Photographer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = status == "pending",
                        onClick = { status = "pending" },
                        label = { Text("Send Request") }
                    )
                    FilterChip(
                        selected = status == "accepted",
                        onClick = { status = "accepted" },
                        label = { Text("Instant Connect") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSubmit(name, handle, bio, avatarTheme, status)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_friend_button")
            ) {
                Text(if (status == "accepted") "Connect" else "Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
