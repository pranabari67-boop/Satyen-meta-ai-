package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val preferredLanguage: String = "en",
    val currency: String = "INR",
    val themeMode: String = "SYSTEM",
    val monthlyBudgetMinor: Long = 2000000L,
    val biometricLockEnabled: Boolean = false,
    val subscriptionTier: String = "FREE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MemoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "PERSONAL",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val sourceType: String = "MEMORY",
    val attachmentUri: String? = null,
    val eventDateMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReminderItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val triggerAtMillis: Long = 0L,
    val timezoneId: String = "UTC",
    val recurrence: String = "ONCE",
    val status: String = "PENDING",
    val snoozedUntilMillis: Long? = null,
    val deliveryStatus: String = "SCHEDULED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val priority: String = "MEDIUM",
    val completed: Boolean = false,
    val dueAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AccountItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val accountType: String = "CASH",
    val currency: String = "INR",
    val balanceMinor: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransactionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val type: String = "EXPENSE",
    val amountMinor: Long = 0L,
    val currency: String = "INR",
    val category: String = "General",
    val description: String = "",
    val occurredAtMillis: Long = 0L,
    val reversedTransactionId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DebtItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val counterparty: String = "",
    val direction: String = "RECEIVABLE",
    val principalMinor: Long = 0L,
    val remainingMinor: Long = 0L,
    val currency: String = "INR",
    val notes: String? = null,
    val dueAtMillis: Long? = null,
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DebtPaymentItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val debtId: String = "",
    val amountMinor: Long = 0L,
    val currency: String = "INR",
    val notes: String? = null,
    val paidAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiMessageItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "USER",
    val text: String = "",
    val intent: String? = null,
    val actionStatus: String = "NONE",
    val actionSummary: String? = null,
    val actionPayloadJson: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLogItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicketItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
