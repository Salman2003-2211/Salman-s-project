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

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sharma",
      phoneNumber: "+919818452198",
      email: "alice@example.com",
      language: "en",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read Alice's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sharma",
      phoneNumber: "+919818452198",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Properties: authenticated user can query properties", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("properties").doc("prop_1").set({
      userId: ALICE_UID,
      title: "2 BHK Flat",
      listingType: "RENT",
      locality: "Sector 62, Noida",
      price: 22000,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("properties").doc("prop_1").get());
  await assertSucceeds(bobDb.collection("properties").get());
});

test("Loan leads: user can create their own loan lead", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("loan_leads").doc("lead_1").set({
      userId: ALICE_UID,
      clientName: "Alice Sharma",
      phoneNumber: "+919818452198",
      loanTypeSelected: "HOME_LOAN",
      requiredAmountInLakhs: 45,
      createdAt: new Date(),
    })
  );
});

test("Loan leads: Bob cannot read Alice's loan lead", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("loan_leads").doc("lead_alice").set({
      userId: ALICE_UID,
      clientName: "Alice Sharma",
      phoneNumber: "+919818452198",
      loanTypeSelected: "HOME_LOAN",
      requiredAmountInLakhs: 45,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("loan_leads").doc("lead_alice").get());
});
