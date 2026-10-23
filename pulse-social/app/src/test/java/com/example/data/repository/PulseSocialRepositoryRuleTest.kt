package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseSocialRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProfileAndPost_authenticatedOwner_emitsInRealtimeFeed() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = PulseSocialRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveProfile(
                displayName = "Alice Rivera",
                handle = "@alice_rivera",
                bio = "Building social graphs",
                workplace = "Pulse Labs",
                hometown = "San Francisco",
                avatarTheme = "ocean",
                coverTheme = "aurora"
            )
        }
        assertTrue("saveProfile failed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertEquals(aliceUid, profile.userId)

        val postResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPost(
                authorProfile = profile,
                content = "Launching our real-time news feed!",
                feeling = "Feeling excited",
                mediaPreset = "banner_aurora",
                category = "milestone",
                visibility = "public"
            )
        }
        assertTrue("createPost failed: ${postResult.exceptionOrNull()}", postResult.isSuccess)
        val postId = postResult.getOrThrow()

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicPosts().first { list -> list.any { it.id == postId } }
        }
        val createdPost = posts.first { it.id == postId }
        assertEquals("Launching our real-time news feed!", createdPost.content)

        // Add comment and verify realtime comments stream
        val commentResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addComment(createdPost, profile, "First comment on this milestone!")
        }
        assertTrue("addComment failed: ${commentResult.exceptionOrNull()}", commentResult.isSuccess)
        val commentId = commentResult.getOrThrow()

        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeComments(postId).first { list -> list.any { it.id == commentId } }
        }
        assertTrue(comments.any { it.id == commentId && it.text == "First comment on this milestone!" })
    }

    @Test
    fun friendshipLifecycle_crossUserAccessBlocked_andAcceptWorks() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PulseSocialRepository(firestore, auth)
        val aliceProfile = UserProfile(
            id = aliceUid,
            userId = aliceUid,
            displayName = "Alice Rivera",
            handle = "@alice_rivera",
            bio = "Product Lead",
            avatarTheme = "ocean"
        )

        val bobUid = signInTestUser(BOB_EMAIL)
        // Switch back to Alice to send request to Bob
        signInTestUser(ALICE_EMAIL)
        val frResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendFriendRequest(
                myProfile = aliceProfile,
                targetId = bobUid,
                targetName = "Bob Chen",
                targetHandle = "@bob_chen",
                targetBio = "Android Dev",
                targetAvatarTheme = "sunset"
            )
        }
        assertTrue(frResult.isSuccess)
        val friendshipId = frResult.getOrThrow()

        // Eve (third party) cannot read Alice-Bob friendship
        signInTestUser(EVE_EMAIL)
        val eveRepo = PulseSocialRepository(firestore, auth)
        val eveReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            eveRepo.getFriendshipById(friendshipId)
        }
        assertTrue(eveReadResult.isFailure)
        val eveEx = eveReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(eveEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, eveEx?.code)

        // Bob can accept the friend request
        signInTestUser(BOB_EMAIL)
        val bobRepo = PulseSocialRepository(firestore, auth)
        val acceptResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.respondToFriendRequest(friendshipId, "accepted")
        }
        assertTrue(acceptResult.isSuccess)
    }

    @Test
    fun observePublicPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = PulseSocialRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublicPosts().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected exception for unauthenticated query", caught)
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_social@test.com"
        const val BOB_EMAIL = "bob_social@test.com"
        const val EVE_EMAIL = "eve_social@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
