const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validBackupPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    label: "MengiTex Cloud Backup",
    deviceModel: "Pixel 9",
    encryptedPayload: "MTX1:c2FsdA==:aXY=:Y2lwaGVydGV4dA==",
    messageCount: 12,
    templateCount: 4,
    contactCount: 8,
    passwordProtected: true,
    formatVersion: 1,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user cannot read, list, create, or delete backups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ref = unauthDb.collection("users").doc(ALICE_UID).collection("cloud_backups");
  await assertFails(ref.get());
  await assertFails(ref.doc("bk_1").get());
  await assertFails(ref.doc("bk_1").set(validBackupPayload(ALICE_UID)));
  await assertFails(ref.doc("bk_1").delete());
});

test("Authenticated owner: valid create, list, get, rename update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("cloud_backups");

  await assertSucceeds(col.doc("bk_2026_01").set(validBackupPayload(ALICE_UID)));
  await assertSucceeds(col.doc("bk_2026_01").get());
  await assertSucceeds(col.orderBy("createdAt", "desc").get());

  // Rename action (allowed affectedKeys: label, updatedAt)
  await assertSucceeds(
    col.doc("bk_2026_01").update({
      label: "Backup Muhimu",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete own backup
  await assertSucceeds(col.doc("bk_2026_01").delete());
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's backups", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("cloud_backups")
      .doc("alice_bk_1")
      .set(validBackupPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceCol = bobDb.collection("users").doc(ALICE_UID).collection("cloud_backups");

  await assertFails(aliceCol.get());
  await assertFails(aliceCol.doc("alice_bk_1").get());
  await assertFails(aliceCol.doc("bob_inject").set(validBackupPayload(BOB_UID)));
  await assertFails(
    aliceCol.doc("alice_bk_1").update({
      label: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(aliceCol.doc("alice_bk_1").delete());
});

test("Red team guards: reject ghost field, @DocumentId id field, userId spoofing, future timestamp, and immutable field tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("cloud_backups");

  // Shadow/ghost field injection
  await assertFails(
    col.doc("bk_ghost").set(validBackupPayload(ALICE_UID, { isAdmin: true }))
  );

  // @DocumentId `id` field in payload
  await assertFails(
    col.doc("bk_with_id").set(validBackupPayload(ALICE_UID, { id: "bk_with_id" }))
  );

  // Identity spoofing in userId field
  await assertFails(
    col.doc("bk_spoof").set(validBackupPayload(BOB_UID))
  );

  // Tampered future timestamp
  await assertFails(
    col.doc("bk_future").set(
      validBackupPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );

  // Oversized label (> 100 chars)
  await assertFails(
    col.doc("bk_long").set(validBackupPayload(ALICE_UID, { label: "A".repeat(105) }))
  );

  // Valid create, then attempt immutable createdAt & encryptedPayload tampering on update
  await assertSucceeds(col.doc("bk_immut").set(validBackupPayload(ALICE_UID)));
  await assertFails(
    col.doc("bk_immut").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    col.doc("bk_immut").update({
      encryptedPayload: "TAMPERED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
