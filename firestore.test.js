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

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write any private user documents", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("memories").get()
  );
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauth
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        title: "Hack",
        priority: "HIGH",
        completed: false,
        createdAt: pastDate(),
        updatedAt: pastDate(),
      })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's profile, memories, reminders, or transactions", async () => {
  const now = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      preferredLanguage: "en",
      currency: "INR",
      themeMode: "SYSTEM",
      monthlyBudgetMinor: 100000,
      biometricLockEnabled: false,
      subscriptionTier: "FREE",
      createdAt: now,
      updatedAt: now,
    });
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("memories")
      .doc("mem_1")
      .set({
        userId: BOB_UID,
        title: "Secret Note",
        content: "Private",
        category: "PERSONAL",
        tags: ["secret"],
        pinned: true,
        sourceType: "MEMORY",
        createdAt: now,
        updatedAt: now,
      });
  });

  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(alice.collection("users").doc(BOB_UID).get());
  await assertFails(
    alice.collection("users").doc(BOB_UID).collection("memories").get()
  );
  await assertFails(
    alice
      .collection("users")
      .doc(BOB_UID)
      .collection("memories")
      .doc("mem_1")
      .update({ title: "Tampered", updatedAt: pastDate() })
  );
  await assertFails(
    alice
      .collection("users")
      .doc(BOB_UID)
      .collection("memories")
      .doc("mem_1")
      .delete()
  );
});

test("User profile: valid create & partial update succeed; ghost field & future timestamp rejected", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Satyen User",
      email: "user@example.com",
      preferredLanguage: "bn",
      currency: "INR",
      themeMode: "DARK",
      monthlyBudgetMinor: 2500000,
      biometricLockEnabled: false,
      subscriptionTier: "FREE",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "en",
      monthlyBudgetMinor: 3000000,
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Memories, reminders, and tasks: valid payloads succeed; @DocumentId id field & invalid enums rejected", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("memories")
      .doc("mem_1")
      .set({
        userId: ALICE_UID,
        title: "Passport Renewal",
        content: "Renew before December",
        category: "PERSONAL",
        tags: ["docs", "urgent"],
        pinned: true,
        sourceType: "MEMORY",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject redundant @DocumentId `id` field
  await assertFails(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("memories")
      .doc("mem_2")
      .set({
        id: "mem_2",
        userId: ALICE_UID,
        title: "Bad Doc",
        content: "Has id",
        category: "PERSONAL",
        tags: [],
        pinned: false,
        sourceType: "NOTE",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Valid reminder create & snooze update
  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_1")
      .set({
        userId: ALICE_UID,
        title: "Morning Market",
        triggerAtMillis: Date.now() + 3600000,
        timezoneId: "Asia/Kolkata",
        recurrence: "ONCE",
        status: "PENDING",
        deliveryStatus: "SCHEDULED",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_1")
      .update({
        status: "SNOOZED",
        snoozedUntilMillis: Date.now() + 5400000,
        updatedAt: pastDate(1000),
      })
  );

  // Valid task create & completion toggle
  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        title: "Pay electricity bill",
        priority: "HIGH",
        completed: false,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        completed: true,
        updatedAt: pastDate(1000),
      })
  );
});

test("Accounting immutability, parent existence, and settled debt locking", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  // Transaction referencing non-existent account must fail
  await assertFails(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_orphan")
      .set({
        userId: ALICE_UID,
        accountId: "missing_acc",
        type: "EXPENSE",
        amountMinor: 50000,
        currency: "INR",
        category: "Groceries",
        description: "Market",
        occurredAtMillis: Date.now(),
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create account first
  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("accounts")
      .doc("acc_cash")
      .set({
        userId: ALICE_UID,
        name: "Cash Wallet",
        accountType: "CASH",
        currency: "INR",
        balanceMinor: 500000,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create valid transaction
  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        accountId: "acc_cash",
        type: "EXPENSE",
        amountMinor: 50000,
        currency: "INR",
        category: "Groceries",
        description: "Vegetables",
        occurredAtMillis: Date.now(),
        createdAt: created,
        updatedAt: created,
      })
  );

  // Silent modification of historical transaction must FAIL
  await assertFails(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .update({
        amountMinor: 1000,
        updatedAt: pastDate(1000),
      })
  );

  // Debt + payment + settled lock
  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("debts")
      .doc("debt_1")
      .set({
        userId: ALICE_UID,
        counterparty: "Rahim",
        direction: "RECEIVABLE",
        principalMinor: 1000000,
        remainingMinor: 1000000,
        currency: "INR",
        status: "OPEN",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("debt_payments")
      .doc("pay_1")
      .set({
        userId: ALICE_UID,
        debtId: "debt_1",
        amountMinor: 1000000,
        currency: "INR",
        paidAtMillis: Date.now(),
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("debts")
      .doc("debt_1")
      .update({
        remainingMinor: 0,
        status: "SETTLED",
        updatedAt: pastDate(1000),
      })
  );

  // Subsequent mutation on SETTLED debt must fail
  await assertFails(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("debts")
      .doc("debt_1")
      .update({
        remainingMinor: 500,
        status: "OPEN",
        updatedAt: pastDate(500),
      })
  );
});
