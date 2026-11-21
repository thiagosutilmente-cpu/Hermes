const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_rider_01";
const BOB_UID = "bob_rider_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write profiles or deliveries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profiles").doc("cockpit").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("deliveries").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profiles").doc("cockpit").set({
      userId: ALICE_UID,
      dailyGoalBrl: 300,
      minGainPerKm: 4.5,
      minOfferValue: 15,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can create, update, and read RiderGoalProfile matching Kotlin repository", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  const profileRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("profiles")
    .doc("cockpit");

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      dailyGoalBrl: 350.0,
      minGainPerKm: 4.8,
      minOfferValue: 16.0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserves createdAt and userId
  await assertSucceeds(
    profileRef.update({
      dailyGoalBrl: 400.0,
      minGainPerKm: 5.0,
      minOfferValue: 18.0,
      updatedAt: updated,
    })
  );

  await assertSucceeds(profileRef.get());

  // Reject mutating createdAt
  await assertFails(
    profileRef.update({
      createdAt: updated,
      updatedAt: updated,
    })
  );

  // Reject ghost/unexpected field (Shadow Update)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("cockpit_ghost")
      .set({
        userId: ALICE_UID,
        dailyGoalBrl: 350.0,
        minGainPerKm: 4.8,
        minOfferValue: 16.0,
        isAdmin: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("cockpit_future")
      .set({
        userId: ALICE_UID,
        dailyGoalBrl: 350.0,
        minGainPerKm: 4.8,
        minOfferValue: 16.0,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});

test("Owner can create and query DeliveryLog, and @DocumentId id injection fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("deliveries")
      .doc("deliv_1")
      .set({
        userId: ALICE_UID,
        appSource: "iFood",
        restaurant: "Madero Container",
        payoutBrl: 33.5,
        distanceKm: 5.2,
        merged: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Matches Kotlin query: orderBy("createdAt", DESCENDING).limit(30)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("deliveries")
      .orderBy("createdAt", "desc")
      .limit(30)
      .get()
  );

  // Reject explicit id field inside document body when @DocumentId is used
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("deliveries")
      .doc("deliv_2")
      .set({
        id: "deliv_2",
        userId: ALICE_UID,
        appSource: "99 Moto",
        restaurant: "Burger King",
        payoutBrl: 21.0,
        distanceKm: 3.1,
        merged: false,
        createdAt: created,
        updatedAt: created,
      })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's profile or deliveries", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const bobDb = context.firestore();
    const created = new Date(Date.now() - 5000);
    await bobDb.collection("users").doc(BOB_UID).collection("profiles").doc("cockpit").set({
      userId: BOB_UID,
      dailyGoalBrl: 300.0,
      minGainPerKm: 4.5,
      minOfferValue: 15.0,
      createdAt: created,
      updatedAt: created,
    });
    await bobDb.collection("users").doc(BOB_UID).collection("deliveries").doc("bob_d1").set({
      userId: BOB_UID,
      appSource: "Rappi",
      restaurant: "Outback",
      payoutBrl: 28.0,
      distanceKm: 4.0,
      merged: false,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("profiles").doc("cockpit").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("deliveries").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("deliveries").doc("bob_d1").update({
      payoutBrl: 999.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
