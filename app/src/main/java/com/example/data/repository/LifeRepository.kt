package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AccountItem
import com.example.data.model.AiMessageItem
import com.example.data.model.AuditLogItem
import com.example.data.model.DebtItem
import com.example.data.model.DebtPaymentItem
import com.example.data.model.MemoryItem
import com.example.data.model.ReminderItem
import com.example.data.model.SupportTicketItem
import com.example.data.model.TaskItem
import com.example.data.model.TransactionItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LifeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeSubcollection(
        uid: String,
        collection: String,
        orderField: String = "createdAt",
        descending: Boolean = true
    ): Flow<List<T>> {
        val path = "users/$uid/$collection"
        return userDoc(uid).collection(collection)
            .orderBy(orderField, if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(toLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeProfile(uid: String): Flow<UserProfile?> =
        userDoc(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(toLog, OperationType.GET, "users/$uid")
                }
                throw error
            }

    fun observeMemories(uid: String): Flow<List<MemoryItem>> = observeSubcollection(uid, "memories")
    fun observeReminders(uid: String): Flow<List<ReminderItem>> = observeSubcollection(uid, "reminders", "triggerAtMillis", false)
    fun observeTasks(uid: String): Flow<List<TaskItem>> = observeSubcollection(uid, "tasks")
    fun observeAccounts(uid: String): Flow<List<AccountItem>> = observeSubcollection(uid, "accounts")
    fun observeTransactions(uid: String): Flow<List<TransactionItem>> = observeSubcollection(uid, "transactions", "occurredAtMillis", true)
    fun observeDebts(uid: String): Flow<List<DebtItem>> = observeSubcollection(uid, "debts")
    fun observeDebtPayments(uid: String): Flow<List<DebtPaymentItem>> = observeSubcollection(uid, "debt_payments", "paidAtMillis", true)
    fun observeAiMessages(uid: String): Flow<List<AiMessageItem>> = observeSubcollection(uid, "ai_messages", "createdAt", false)
    fun observeAuditLogs(uid: String): Flow<List<AuditLogItem>> = observeSubcollection(uid, "audit_logs")
    fun observeSupportTickets(uid: String): Flow<List<SupportTicketItem>> = observeSubcollection(uid, "support_tickets")

    suspend fun ensureUserInitialized(
        uid: String,
        displayName: String,
        email: String?,
        preferredLanguage: String,
        themeMode: String
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "User ID mismatch" }
            val profileSnap = userDoc(uid).get().await()
            if (!profileSnap.exists()) {
                val cleanName = displayName.trim().ifBlank { "Satyen User" }.take(100)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "email" to email?.take(160),
                    "preferredLanguage" to if (preferredLanguage in listOf("en", "bn")) preferredLanguage else "en",
                    "currency" to "INR",
                    "themeMode" to if (themeMode in listOf("SYSTEM", "LIGHT", "DARK")) themeMode else "SYSTEM",
                    "monthlyBudgetMinor" to 2500000L,
                    "biometricLockEnabled" to false,
                    "subscriptionTier" to "FREE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userDoc(uid).set(payload).await()
            }

            // Ensure default Cash & Bank accounts exist if user has zero accounts yet
            val accountsSnap = userDoc(uid).collection("accounts").limit(1).get().await()
            if (accountsSnap.isEmpty) {
                val batch = db.batch()
                val cashRef = userDoc(uid).collection("accounts").document()
                batch.set(
                    cashRef,
                    mapOf(
                        "userId" to uid,
                        "name" to "Cash Wallet",
                        "accountType" to "CASH",
                        "currency" to "INR",
                        "balanceMinor" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val bankRef = userDoc(uid).collection("accounts").document()
                batch.set(
                    bankRef,
                    mapOf(
                        "userId" to uid,
                        "name" to "Primary Bank",
                        "accountType" to "BANK",
                        "currency" to "INR",
                        "balanceMinor" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        uid: String,
        preferredLanguage: String? = null,
        currency: String? = null,
        themeMode: String? = null,
        monthlyBudgetMinor: Long? = null,
        biometricLockEnabled: Boolean? = null
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            preferredLanguage?.let { updates["preferredLanguage"] = it }
            currency?.let { updates["currency"] = it }
            themeMode?.let { updates["themeMode"] = it }
            monthlyBudgetMinor?.let { updates["monthlyBudgetMinor"] = it.coerceAtLeast(0L) }
            biometricLockEnabled?.let { updates["biometricLockEnabled"] = it }
            userDoc(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMemory(
        uid: String,
        existingId: String? = null,
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        pinned: Boolean,
        sourceType: String,
        attachmentUri: String? = null,
        eventDateMillis: Long? = null
    ): Result<String> {
        val path = "users/$uid/memories"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            val cleanTitle = title.trim().take(160)
            require(cleanTitle.isNotEmpty()) { "Title cannot be empty" }
            val cleanContent = content.trim().take(8000)
            val cleanTags = tags.mapNotNull { it.trim().take(30).takeIf { t -> t.isNotEmpty() } }.take(10)
            val validCategory = if (category in listOf("PERSONAL", "WORK", "FINANCE", "HEALTH", "IDEA", "VOICE", "DOCUMENT")) category else "PERSONAL"
            val validSource = if (sourceType in listOf("NOTE", "MEMORY", "VOICE", "DOCUMENT")) sourceType else "MEMORY"

            if (existingId.isNullOrBlank()) {
                val docRef = userDoc(uid).collection("memories").document()
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "category" to validCategory,
                    "tags" to cleanTags,
                    "pinned" to pinned,
                    "sourceType" to validSource,
                    "attachmentUri" to attachmentUri?.take(500),
                    "eventDateMillis" to eventDateMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                appendAuditLog(uid, "CREATE_MEMORY", "MEMORY", docRef.id, "Saved memory: $cleanTitle")
                Result.success(docRef.id)
            } else {
                val docRef = userDoc(uid).collection("memories").document(existingId)
                val updates = mapOf(
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "category" to validCategory,
                    "tags" to cleanTags,
                    "pinned" to pinned,
                    "sourceType" to validSource,
                    "attachmentUri" to attachmentUri?.take(500),
                    "eventDateMillis" to eventDateMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updates).await()
                Result.success(existingId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMemoryPin(uid: String, memoryId: String, pinned: Boolean): Result<Unit> {
        val path = "users/$uid/memories/$memoryId"
        return try {
            requireUserId()
            userDoc(uid).collection("memories").document(memoryId)
                .update(mapOf("pinned" to pinned, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(uid: String, memoryId: String): Result<Unit> {
        val path = "users/$uid/memories/$memoryId"
        return try {
            requireUserId()
            userDoc(uid).collection("memories").document(memoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createReminder(
        uid: String,
        title: String,
        notes: String?,
        triggerAtMillis: Long,
        timezoneId: String,
        recurrence: String,
        deliveryStatus: String
    ): Result<String> {
        val path = "users/$uid/reminders"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            val cleanTitle = title.trim().take(160)
            require(cleanTitle.isNotEmpty()) { "Reminder title required" }
            require(triggerAtMillis > 0L) { "Valid reminder time required" }
            val validRecurrence = if (recurrence in listOf("ONCE", "DAILY", "WEEKLY", "MONTHLY")) recurrence else "ONCE"
            val validDelivery = if (deliveryStatus in listOf("SCHEDULED", "DELIVERED", "PERMISSION_DENIED", "SKIPPED")) deliveryStatus else "SCHEDULED"

            val docRef = userDoc(uid).collection("reminders").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "triggerAtMillis" to triggerAtMillis,
                "timezoneId" to timezoneId.take(64).ifBlank { "UTC" },
                "recurrence" to validRecurrence,
                "status" to "PENDING",
                "deliveryStatus" to validDelivery,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            appendAuditLog(uid, "CREATE_REMINDER", "REMINDER", docRef.id, "Scheduled reminder: $cleanTitle")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateReminderStatus(
        uid: String,
        reminderId: String,
        status: String,
        newTriggerAtMillis: Long? = null,
        snoozedUntilMillis: Long? = null,
        deliveryStatus: String? = null
    ): Result<Unit> {
        val path = "users/$uid/reminders/$reminderId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            newTriggerAtMillis?.let { updates["triggerAtMillis"] = it }
            snoozedUntilMillis?.let { updates["snoozedUntilMillis"] = it }
            deliveryStatus?.let { updates["deliveryStatus"] = it }
            userDoc(uid).collection("reminders").document(reminderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(uid: String, reminderId: String): Result<Unit> {
        val path = "users/$uid/reminders/$reminderId"
        return try {
            requireUserId()
            userDoc(uid).collection("reminders").document(reminderId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createTask(
        uid: String,
        title: String,
        notes: String?,
        priority: String,
        dueAtMillis: Long?
    ): Result<String> {
        val path = "users/$uid/tasks"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            val cleanTitle = title.trim().take(160)
            require(cleanTitle.isNotEmpty()) { "Task title required" }
            val validPriority = if (priority in listOf("LOW", "MEDIUM", "HIGH")) priority else "MEDIUM"
            val docRef = userDoc(uid).collection("tasks").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "priority" to validPriority,
                "completed" to false,
                "dueAtMillis" to dueAtMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(uid: String, taskId: String, completed: Boolean): Result<Unit> {
        val path = "users/$uid/tasks/$taskId"
        return try {
            requireUserId()
            userDoc(uid).collection("tasks").document(taskId)
                .update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(uid: String, taskId: String): Result<Unit> {
        val path = "users/$uid/tasks/$taskId"
        return try {
            requireUserId()
            userDoc(uid).collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createAccount(
        uid: String,
        name: String,
        accountType: String,
        currency: String,
        initialBalanceMinor: Long
    ): Result<String> {
        val path = "users/$uid/accounts"
        return try {
            requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Account name required" }
            val validType = if (accountType in listOf("CASH", "BANK", "WALLET", "CARD")) accountType else "CASH"
            val docRef = userDoc(uid).collection("accounts").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "accountType" to validType,
                    "currency" to currency.take(5).ifBlank { "INR" },
                    "balanceMinor" to initialBalanceMinor,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(uid, "CREATE_ACCOUNT", "ACCOUNT", docRef.id, "Created account: $cleanName")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(
        uid: String,
        accountId: String,
        type: String,
        amountMinor: Long,
        currency: String,
        category: String,
        description: String,
        occurredAtMillis: Long = System.currentTimeMillis()
    ): Result<String> {
        val path = "users/$uid/transactions"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            require(amountMinor in 1L..100_000_000_000L) { "Amount must be positive" }
            require(type in listOf("INCOME", "EXPENSE")) { "Invalid transaction type" }
            val cleanCat = category.trim().ifBlank { "General" }.take(60)
            val cleanDesc = description.trim().ifBlank { cleanCat }.take(300)

            val txRef = userDoc(uid).collection("transactions").document()
            val accRef = userDoc(uid).collection("accounts").document(accountId)
            val auditRef = userDoc(uid).collection("audit_logs").document()
            val delta = if (type == "INCOME") amountMinor else -amountMinor

            db.runBatch { batch ->
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "accountId" to accountId,
                        "type" to type,
                        "amountMinor" to amountMinor,
                        "currency" to currency.take(5).ifBlank { "INR" },
                        "category" to cleanCat,
                        "description" to cleanDesc,
                        "occurredAtMillis" to occurredAtMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    accRef,
                    mapOf(
                        "balanceMinor" to FieldValue.increment(delta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    auditRef,
                    mapOf(
                        "userId" to uid,
                        "action" to "RECORD_$type",
                        "entityType" to "TRANSACTION",
                        "entityId" to txRef.id,
                        "summary" to "$type $currency ${amountMinor / 100.0}: $cleanDesc",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(txRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun reverseTransaction(uid: String, original: TransactionItem): Result<String> {
        val path = "users/$uid/transactions"
        return try {
            requireUserId()
            require(original.type != "REVERSAL") { "Cannot reverse a reversal entry" }
            val txRef = userDoc(uid).collection("transactions").document()
            val accRef = userDoc(uid).collection("accounts").document(original.accountId)
            val auditRef = userDoc(uid).collection("audit_logs").document()

            val delta = if (original.type == "INCOME") -original.amountMinor else original.amountMinor

            db.runBatch { batch ->
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "accountId" to original.accountId,
                        "type" to "REVERSAL",
                        "amountMinor" to original.amountMinor,
                        "currency" to original.currency,
                        "category" to "Reversal (${original.category})",
                        "description" to "Reversal of: ${original.description}".take(300),
                        "occurredAtMillis" to System.currentTimeMillis(),
                        "reversedTransactionId" to original.id,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    accRef,
                    mapOf(
                        "balanceMinor" to FieldValue.increment(delta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    auditRef,
                    mapOf(
                        "userId" to uid,
                        "action" to "REVERSE_TRANSACTION",
                        "entityType" to "TRANSACTION",
                        "entityId" to original.id,
                        "summary" to "Reversed ${original.type} (${original.description.take(80)})",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(txRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createDebt(
        uid: String,
        counterparty: String,
        direction: String,
        principalMinor: Long,
        currency: String,
        notes: String?,
        dueAtMillis: Long?
    ): Result<String> {
        val path = "users/$uid/debts"
        return try {
            requireUserId()
            val cleanPerson = counterparty.trim().take(100)
            require(cleanPerson.isNotEmpty()) { "Counterparty name required" }
            require(principalMinor in 1L..100_000_000_000L) { "Amount must be positive" }
            val validDirection = if (direction in listOf("RECEIVABLE", "PAYABLE")) direction else "RECEIVABLE"
            val docRef = userDoc(uid).collection("debts").document()
            val payload = mapOf(
                "userId" to uid,
                "counterparty" to cleanPerson,
                "direction" to validDirection,
                "principalMinor" to principalMinor,
                "remainingMinor" to principalMinor,
                "currency" to currency.take(5).ifBlank { "INR" },
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300),
                "dueAtMillis" to dueAtMillis,
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            appendAuditLog(uid, "CREATE_DEBT", "DEBT", docRef.id, "$validDirection with $cleanPerson")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordDebtPayment(
        uid: String,
        debt: DebtItem,
        paymentMinor: Long,
        notes: String?
    ): Result<Unit> {
        val path = "users/$uid/debt_payments"
        return try {
            requireUserId()
            require(debt.status != "SETTLED") { "Debt is already settled" }
            require(paymentMinor in 1L..debt.remainingMinor) { "Payment cannot exceed remaining balance" }

            val newRemaining = debt.remainingMinor - paymentMinor
            val newStatus = if (newRemaining == 0L) "SETTLED" else "OPEN"
            val debtRef = userDoc(uid).collection("debts").document(debt.id)
            val payRef = userDoc(uid).collection("debt_payments").document()
            val auditRef = userDoc(uid).collection("audit_logs").document()

            db.runBatch { batch ->
                batch.set(
                    payRef,
                    mapOf(
                        "userId" to uid,
                        "debtId" to debt.id,
                        "amountMinor" to paymentMinor,
                        "currency" to debt.currency,
                        "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(200),
                        "paidAtMillis" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
                batch.update(
                    debtRef,
                    mapOf(
                        "remainingMinor" to newRemaining,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    auditRef,
                    mapOf(
                        "userId" to uid,
                        "action" to "DEBT_PAYMENT",
                        "entityType" to "DEBT",
                        "entityId" to debt.id,
                        "summary" to "Payment recorded for ${debt.counterparty} ($newStatus)",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addAiMessage(
        uid: String,
        sender: String,
        text: String,
        intent: String? = null,
        actionStatus: String = "NONE",
        actionSummary: String? = null,
        actionPayloadJson: String? = null
    ): Result<String> {
        val path = "users/$uid/ai_messages"
        return try {
            requireUserId()
            val docRef = userDoc(uid).collection("ai_messages").document()
            val payload = mapOf(
                "userId" to uid,
                "sender" to sender,
                "text" to text.trim().take(8000).ifEmpty { "..." },
                "intent" to intent?.take(64),
                "actionStatus" to actionStatus,
                "actionSummary" to actionSummary?.take(500),
                "actionPayloadJson" to actionPayloadJson?.take(4000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAiMessageStatus(
        uid: String,
        messageId: String,
        actionStatus: String,
        actionSummary: String? = null
    ): Result<Unit> {
        val path = "users/$uid/ai_messages/$messageId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "actionStatus" to actionStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            actionSummary?.let { updates["actionSummary"] = it.take(500) }
            userDoc(uid).collection("ai_messages").document(messageId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(uid: String, subject: String, message: String): Result<String> {
        val path = "users/$uid/support_tickets"
        return try {
            requireUserId()
            val cleanSubj = subject.trim().take(120)
            val cleanMsg = message.trim().take(2000)
            require(cleanSubj.isNotEmpty() && cleanMsg.isNotEmpty()) { "Subject and message required" }
            val docRef = userDoc(uid).collection("support_tickets").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "subject" to cleanSubj,
                    "message" to cleanMsg,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(uid: String): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            val subcollections = listOf(
                "memories",
                "reminders",
                "tasks",
                "transactions",
                "accounts",
                "debt_payments",
                "debts",
                "ai_messages",
                "audit_logs",
                "support_tickets"
            )
            for (sub in subcollections) {
                val snap = userDoc(uid).collection(sub).get().await()
                if (!snap.isEmpty) {
                    val chunks = snap.documents.chunked(400)
                    for (chunk in chunks) {
                        val batch = db.batch()
                        chunk.forEach { batch.delete(it.reference) }
                        batch.commit().await()
                    }
                }
            }
            userDoc(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun appendAuditLog(
        uid: String,
        action: String,
        entityType: String,
        entityId: String,
        summary: String
    ) {
        val logRef = userDoc(uid).collection("audit_logs").document()
        logRef.set(
            mapOf(
                "userId" to uid,
                "action" to action.take(64),
                "entityType" to entityType.take(64),
                "entityId" to entityId.take(128),
                "summary" to summary.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
