# Pulse Social Security Specification

## 1. Data Invariants
1. **Default-Deny Catch-All**: All paths not explicitly matched (`/{document=**}`) deny all reads and writes.
2. **User Profiles (`/profiles/{userId}`)**:
   - Strictly non-PII public profile metadata (`displayName`, `handle`, `bio`, `workplace`, `hometown`, `avatarTheme`, `coverTheme`, `visibility`).
   - Only the authenticated owner (`request.auth.uid == userId`) can create, update, or delete their profile.
   - `userId` and `createdAt` are immutable after creation.
   - `allow list` enforces `existing().userId == request.auth.uid || existing().visibility == 'public'`.
3. **Posts (`/posts/{postId}`)**:
   - Only authenticated users can create a post where `authorId == request.auth.uid`, with initial `likesCount == 0`, `commentsCount == 0`, and empty `likedByUids`.
   - Action-based updates:
     - **Author Edit Action**: Only `authorId == request.auth.uid` can update `content`, `feeling`, `mediaPreset`, `category`, `visibility`, `updatedAt`.
     - **Reaction/Comment Count Action**: Authenticated users can update `likesCount`, `likedByUids`, `commentsCount`, `updatedAt`.
   - `authorId` and `createdAt` are immutable on all updates.
   - Only the post author can delete their post.
4. **Comments (`/posts/{postId}/comments/{commentId}`)**:
   - Parent Gate: A comment can only be created if `exists(/databases/$(database)/documents/posts/$(postId))` is true and `incoming().postId == postId` and `incoming().authorId == request.auth.uid`.
   - Comments are append-only (`allow update: if false;`) and can only be deleted by their author (`existing().authorId == request.auth.uid`).
   - `allow list` enforces `existing().postId == postId` without expensive `get()` calls.
5. **Friendships (`/friendships/{friendshipId}`)**:
   - Scoped strictly to the two participants (`request.auth.uid in existing().memberUids`). Non-members cannot read, list, update, or delete another user's friendships.
   - Creation requires `incoming().requesterId == request.auth.uid` and `incoming().receiverId != incoming().requesterId`.
   - Terminal State Locking: Updates are only permitted while `existing().status == 'pending'`, and can only modify `['status', 'updatedAt']`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: Unauthenticated client attempts to list `/posts` or `/profiles` -> Rejected (`isSignedIn()`).
2. **Identity Spoofing on Profile**: User Alice (`alice_123`) attempts to create `/profiles/bob_456` -> Rejected (`request.auth.uid == userId`).
3. **Ghost Field / Shadow Update**: User Alice attempts to update `/profiles/alice_123` with `{"isVerified": true}` -> Rejected (`hasOnly` on `keys()` and `affectedKeys()`).
4. **Author Impersonation on Post**: User Alice creates a post with `authorId: "bob_456"` -> Rejected (`incoming().authorId == request.auth.uid`).
5. **Unauthorized Post Content Edit**: User Bob attempts to edit `content` of Alice's post -> Rejected (Action 1 requires `request.auth.uid == existing().authorId`).
6. **Immutable Field Tampering**: User Alice attempts to modify `createdAt` or `authorId` on post update -> Rejected (`incoming().createdAt == existing().createdAt`).
7. **Orphaned Comment Creation**: User Alice attempts to create `/posts/non_existent_post/comments/c1` -> Rejected (`exists(...)` Parent Gate).
8. **Cross-User Friendship Spying**: User Eve (`eve_789`) attempts to `get` or `list` friendship between Alice and Bob -> Rejected (`request.auth.uid in existing().memberUids`).
9. **Terminal State Bypass on Friendship**: User Bob attempts to update a friendship whose status is already `'declined'` or `'accepted'` -> Rejected (`existing().status == 'pending'`).
10. **Value / Size Poisoning**: Client submits a 5,000-character post `content` or 100-item `likedByUids` list -> Rejected (`isValidPost` size bounds).
11. **Unfiltered List Query on Friendships**: Authenticated user queries `/friendships` without `.where("memberUids", "array-contains", uid)` -> Rejected by rule `request.auth.uid in existing().memberUids`.
12. **Future Timestamp Injection**: Client submits `createdAt` in the future (`> request.time`) -> Rejected (`data.createdAt <= request.time`).
