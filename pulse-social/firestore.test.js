const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const EVE_UID = "eve_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowTimestamp() {
  return new Date();
}

test("Unauthenticated user: cannot read profiles, posts, or friendships", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("profiles").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("friendships").get());
});

test("Authenticated user: can create and update their own profile, but cannot spoof another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  const validProfile = {
    userId: ALICE_UID,
    displayName: "Alice Rivera",
    handle: "@alice_rivera",
    bio: "Building the next social graph",
    workplace: "Pulse Labs",
    hometown: "San Francisco, CA",
    avatarTheme: "ocean",
    coverTheme: "aurora",
    visibility: "public",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).set(validProfile));

  // Cannot create profile for Bob
  await assertFails(
    aliceDb.collection("profiles").doc(BOB_UID).set({
      ...validProfile,
      userId: BOB_UID,
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      bio: "Updated bio",
      isVerified: true,
      updatedAt: nowTimestamp(),
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      bio: "Updated bio!",
      updatedAt: nowTimestamp(),
    })
  );
});

test("Posts & Comments: create, react, comment parent gate, and cross-user edit protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTimestamp();

  const validPost = {
    authorId: ALICE_UID,
    authorName: "Alice Rivera",
    authorHandle: "@alice_rivera",
    authorAvatarTheme: "ocean",
    content: "Hello Pulse Social world!",
    feeling: "Feeling excited",
    mediaPreset: "banner_aurora",
    category: "status",
    visibility: "public",
    likesCount: 0,
    commentsCount: 0,
    likedByUids: [],
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("posts").doc("post_1").set(validPost));

  // Bob cannot edit Alice's post content
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      content: "Hacked by Bob",
      updatedAt: nowTimestamp(),
    })
  );

  // Bob CAN react/like Alice's post
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      likedByUids: [BOB_UID],
      updatedAt: nowTimestamp(),
    })
  );

  // Bob can comment on existing post_1
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("comment_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob Chen",
      authorAvatarTheme: "sunset",
      text: "Welcome to Pulse Social, Alice!",
      createdAt: nowTimestamp(),
    })
  );

  // Bob cannot comment on non-existent post (Parent Gate)
  await assertFails(
    bobDb.collection("posts").doc("missing_post").collection("comments").doc("comment_2").set({
      postId: "missing_post",
      authorId: BOB_UID,
      authorName: "Bob Chen",
      authorAvatarTheme: "sunset",
      text: "Orphan comment",
      createdAt: nowTimestamp(),
    })
  );
});

test("Friendships: isolation from third-party users and terminal state locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const ts = nowTimestamp();

  const friendshipDoc = {
    requesterId: ALICE_UID,
    requesterName: "Alice Rivera",
    requesterHandle: "@alice_rivera",
    requesterBio: "Product Designer",
    requesterAvatarTheme: "ocean",
    receiverId: BOB_UID,
    receiverName: "Bob Chen",
    receiverHandle: "@bob_chen",
    receiverBio: "Android Engineer",
    receiverAvatarTheme: "sunset",
    memberUids: [ALICE_UID, BOB_UID],
    status: "pending",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("friendships").doc("alice_bob").set(friendshipDoc));

  // Eve cannot read or list Alice-Bob friendship
  await assertFails(eveDb.collection("friendships").doc("alice_bob").get());
  await assertFails(eveDb.collection("friendships").where("memberUids", "array-contains", ALICE_UID).get());

  // Bob can list his friendships using array-contains
  await assertSucceeds(bobDb.collection("friendships").where("memberUids", "array-contains", BOB_UID).get());

  // Bob can accept the pending friendship
  await assertSucceeds(
    bobDb.collection("friendships").doc("alice_bob").update({
      status: "accepted",
      updatedAt: nowTimestamp(),
    })
  );

  // Terminal state lock: once accepted, status cannot be mutated again
  await assertFails(
    bobDb.collection("friendships").doc("alice_bob").update({
      status: "declined",
      updatedAt: nowTimestamp(),
    })
  );
});
