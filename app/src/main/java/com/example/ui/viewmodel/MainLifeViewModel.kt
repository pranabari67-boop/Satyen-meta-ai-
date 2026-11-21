package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.money.MoneyUtils
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode
import com.example.data.local.SettingsDataStore
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
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.LifeRepository
import com.example.notifications.ReminderScheduler
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainLifeViewModel(
    private val currentUserId: String,
    private val displayName: String,
    private val email: String?,
    private val repository: LifeRepository,
    private val aiRepository: AiAssistantRepository,
    private val settingsStore: SettingsDataStore,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId)
            .toUiStateFlow("Failed to load profile")

    val memoriesState: StateFlow<UiState<List<MemoryItem>>> =
        repository.observeMemories(currentUserId)
            .toUiStateFlow("Failed to load memories")

    val remindersState: StateFlow<UiState<List<ReminderItem>>> =
        repository.observeReminders(currentUserId)
            .toUiStateFlow("Failed to load reminders")

    val tasksState: StateFlow<UiState<List<TaskItem>>> =
        repository.observeTasks(currentUserId)
            .toUiStateFlow("Failed to load tasks")

    val accountsState: StateFlow<UiState<List<AccountItem>>> =
        repository.observeAccounts(currentUserId)
            .toUiStateFlow("Failed to load accounts")

    val transactionsState: StateFlow<UiState<List<TransactionItem>>> =
        repository.observeTransactions(currentUserId)
            .toUiStateFlow("Failed to load transactions")

    val debtsState: StateFlow<UiState<List<DebtItem>>> =
        repository.observeDebts(currentUserId)
            .toUiStateFlow("Failed to load debts")

    val debtPaymentsState: StateFlow<UiState<List<DebtPaymentItem>>> =
        repository.observeDebtPayments(currentUserId)
            .toUiStateFlow("Failed to load debt payments")

    val aiMessagesState: StateFlow<UiState<List<AiMessageItem>>> =
        repository.observeAiMessages(currentUserId)
            .toUiStateFlow("Failed to load AI chat")

    val auditLogsState: StateFlow<UiState<List<AuditLogItem>>> =
        repository.observeAuditLogs(currentUserId)
            .toUiStateFlow("Failed to load audit history")

    val supportTicketsState: StateFlow<UiState<List<SupportTicketItem>>> =
        repository.observeSupportTickets(currentUserId)
            .toUiStateFlow("Failed to load support tickets")

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized(
                uid = currentUserId,
                displayName = displayName,
                email = email,
                preferredLanguage = "en",
                themeMode = "SYSTEM"
            )
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, fallbackError, root)
                emit(UiState.Error(root.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsStore.setLanguage(language)
            repository.updateProfilePreferences(currentUserId, preferredLanguage = language.code)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
            repository.updateProfilePreferences(currentUserId, themeMode = mode.name)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            repository.updateProfilePreferences(currentUserId, currency = currency)
                .onFailure { _events.emit(it.message ?: "Failed to update currency") }
        }
    }

    fun setMonthlyBudget(amountText: String) {
        val minor = MoneyUtils.parseToMinor(amountText)
        if (minor == null) {
            emitMessage("Enter a valid positive budget amount.")
            return
        }
        viewModelScope.launch {
            repository.updateProfilePreferences(currentUserId, monthlyBudgetMinor = minor)
                .onSuccess { _events.emit("Monthly budget updated.") }
                .onFailure { _events.emit(it.message ?: "Failed to update budget") }
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setBiometricLockEnabled(enabled)
            repository.updateProfilePreferences(currentUserId, biometricLockEnabled = enabled)
        }
    }

    fun saveMemory(
        existingId: String? = null,
        title: String,
        content: String,
        category: String,
        tagsRaw: String,
        pinned: Boolean,
        sourceType: String,
        attachmentUri: String? = null
    ) {
        val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            repository.saveMemory(
                uid = currentUserId,
                existingId = existingId,
                title = title,
                content = content,
                category = category,
                tags = tags,
                pinned = pinned,
                sourceType = sourceType,
                attachmentUri = attachmentUri
            ).onSuccess {
                _events.emit("Saved to Memory")
            }.onFailure {
                _events.emit(it.message ?: "Failed to save memory")
            }
        }
    }

    fun toggleMemoryPin(item: MemoryItem) {
        viewModelScope.launch {
            repository.toggleMemoryPin(currentUserId, item.id, !item.pinned)
                .onFailure { _events.emit(it.message ?: "Failed to update pin") }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            repository.deleteMemory(currentUserId, memoryId)
                .onSuccess { _events.emit("Memory removed") }
                .onFailure { _events.emit(it.message ?: "Failed to delete memory") }
        }
    }

    fun createReminder(
        title: String,
        notes: String?,
        triggerAtMillis: Long,
        recurrence: String
    ) {
        viewModelScope.launch {
            val tzId = TimeZone.getDefault().id
            val tempId = "rem_${System.currentTimeMillis()}"
            val deliveryStatus = reminderScheduler.scheduleReminder(tempId, title, notes, triggerAtMillis)
            repository.createReminder(
                uid = currentUserId,
                title = title,
                notes = notes,
                triggerAtMillis = triggerAtMillis,
                timezoneId = tzId,
                recurrence = recurrence,
                deliveryStatus = deliveryStatus
            ).onSuccess { docId ->
                reminderScheduler.scheduleReminder(docId, title, notes, triggerAtMillis)
                if (deliveryStatus == "PERMISSION_DENIED") {
                    _events.emit("Reminder saved, but notifications are disabled in OS permissions.")
                } else {
                    _events.emit("Reminder scheduled.")
                }
            }.onFailure {
                _events.emit(it.message ?: "Failed to schedule reminder")
            }
        }
    }

    fun markReminderCompleted(item: ReminderItem) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(item.id)
            if (item.recurrence == "ONCE") {
                repository.updateReminderStatus(currentUserId, item.id, "COMPLETED")
            } else {
                val nextTrigger = computeNextOccurrence(item.triggerAtMillis, item.recurrence)
                val deliveryStatus = reminderScheduler.scheduleReminder(item.id, item.title, item.notes, nextTrigger)
                repository.updateReminderStatus(
                    uid = currentUserId,
                    reminderId = item.id,
                    status = "PENDING",
                    newTriggerAtMillis = nextTrigger,
                    deliveryStatus = deliveryStatus
                )
            }
        }
    }

    fun snoozeReminder(item: ReminderItem, minutes: Int) {
        viewModelScope.launch {
            val next = System.currentTimeMillis() + minutes * 60_000L
            val deliveryStatus = reminderScheduler.scheduleReminder(item.id, item.title, item.notes, next)
            repository.updateReminderStatus(
                uid = currentUserId,
                reminderId = item.id,
                status = "SNOOZED",
                newTriggerAtMillis = next,
                snoozedUntilMillis = next,
                deliveryStatus = deliveryStatus
            ).onSuccess {
                _events.emit("Snoozed for $minutes minutes")
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(reminderId)
            repository.deleteReminder(currentUserId, reminderId)
        }
    }

    fun createTask(title: String, notes: String?, priority: String, dueAtMillis: Long? = null) {
        viewModelScope.launch {
            repository.createTask(currentUserId, title, notes, priority, dueAtMillis)
                .onSuccess { _events.emit("Task added") }
                .onFailure { _events.emit(it.message ?: "Failed to add task") }
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(currentUserId, task.id, !task.completed)
                .onFailure { _events.emit(it.message ?: "Failed to update task") }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(currentUserId, taskId)
        }
    }

    fun createAccount(name: String, accountType: String, currency: String, initialBalanceText: String) {
        val minor = if (initialBalanceText.isBlank()) 0L else {
            val parsed = MoneyUtils.parseToMinor(initialBalanceText)
            if (parsed == null) {
                emitMessage("Invalid initial balance.")
                return
            }
            parsed
        }
        viewModelScope.launch {
            repository.createAccount(currentUserId, name, accountType, currency, minor)
                .onSuccess { _events.emit("Account created") }
                .onFailure { _events.emit(it.message ?: "Failed to create account") }
        }
    }

    fun recordTransaction(
        accountId: String,
        type: String,
        amountText: String,
        currency: String,
        category: String,
        description: String
    ) {
        val minor = MoneyUtils.parseToMinor(amountText)
        if (minor == null) {
            emitMessage("Enter a valid positive amount.")
            return
        }
        viewModelScope.launch {
            repository.recordTransaction(
                uid = currentUserId,
                accountId = accountId,
                type = type,
                amountMinor = minor,
                currency = currency,
                category = category,
                description = description
            ).onSuccess {
                _events.emit("Transaction posted to immutable ledger")
            }.onFailure {
                _events.emit(it.message ?: "Transaction failed")
            }
        }
    }

    fun reverseTransaction(tx: TransactionItem) {
        viewModelScope.launch {
            repository.reverseTransaction(currentUserId, tx)
                .onSuccess { _events.emit("Reversal entry recorded with audit log") }
                .onFailure { _events.emit(it.message ?: "Reversal failed") }
        }
    }

    fun createDebt(
        counterparty: String,
        direction: String,
        amountText: String,
        currency: String,
        notes: String?
    ) {
        val minor = MoneyUtils.parseToMinor(amountText)
        if (minor == null) {
            emitMessage("Enter a valid positive amount.")
            return
        }
        viewModelScope.launch {
            repository.createDebt(
                uid = currentUserId,
                counterparty = counterparty,
                direction = direction,
                principalMinor = minor,
                currency = currency,
                notes = notes,
                dueAtMillis = null
            ).onSuccess {
                _events.emit("Loan record saved")
            }.onFailure {
                _events.emit(it.message ?: "Failed to save loan")
            }
        }
    }

    fun recordDebtPayment(debt: DebtItem, amountText: String, notes: String?) {
        val minor = MoneyUtils.parseToMinor(amountText)
        if (minor == null || minor > debt.remainingMinor) {
            emitMessage("Enter a valid payment up to the remaining balance.")
            return
        }
        viewModelScope.launch {
            repository.recordDebtPayment(currentUserId, debt, minor, notes)
                .onSuccess { _events.emit("Installment payment recorded") }
                .onFailure { _events.emit(it.message ?: "Payment failed") }
        }
    }

    fun sendAiMessage(userText: String, language: AppLanguage) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _aiBusy.value) return

        viewModelScope.launch {
            _aiBusy.value = true
            try {
                repository.addAiMessage(currentUserId, sender = "USER", text = trimmed)

                val profile = (profileState.value as? UiState.Success)?.data
                val currency = profile?.currency ?: "INR"
                val accounts = (accountsState.value as? UiState.Success)?.data.orEmpty()
                val transactions = (transactionsState.value as? UiState.Success)?.data.orEmpty()
                val debts = (debtsState.value as? UiState.Success)?.data.orEmpty()
                val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
                val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
                val memories = (memoriesState.value as? UiState.Success)?.data.orEmpty()

                val (monthInc, monthExp) = computeMonthlyTotals(transactions)

                val planResult = aiRepository.processUserMessage(
                    userMessage = trimmed,
                    language = language,
                    defaultCurrency = currency,
                    accounts = accounts,
                    transactions = transactions,
                    debts = debts,
                    reminders = reminders,
                    tasks = tasks,
                    memories = memories,
                    monthlyIncomeMinor = monthInc,
                    monthlyExpenseMinor = monthExp
                )

                planResult.onSuccess { plan ->
                    val status = if (plan.requiresConfirmation && plan.payloadJson != null) {
                        "PENDING_CONFIRMATION"
                    } else {
                        "NONE"
                    }
                    repository.addAiMessage(
                        uid = currentUserId,
                        sender = "ASSISTANT",
                        text = plan.replyText,
                        intent = plan.intent,
                        actionStatus = status,
                        actionSummary = plan.actionSummary,
                        actionPayloadJson = plan.payloadJson
                    )
                }.onFailure { err ->
                    val errText = err.localizedMessage ?: "AI Assistant unavailable right now."
                    repository.addAiMessage(
                        uid = currentUserId,
                        sender = "ASSISTANT",
                        text = "Error: $errText",
                        intent = "error",
                        actionStatus = "NONE"
                    )
                }
            } finally {
                _aiBusy.value = false
            }
        }
    }

    fun confirmAiAction(message: AiMessageItem) {
        val payloadStr = message.actionPayloadJson ?: return
        viewModelScope.launch {
            try {
                val json = JSONObject(payloadStr)
                val intent = json.optString("intent")
                val profile = (profileState.value as? UiState.Success)?.data
                val defaultCurrency = json.optString("currency").takeIf { it.length in 3..5 && it != "null" }
                    ?: profile?.currency ?: "INR"
                val accounts = (accountsState.value as? UiState.Success)?.data.orEmpty()
                val primaryAccountId = accounts.firstOrNull()?.id

                val result: Result<Any> = when (intent) {
                    "record_expense", "record_income" -> {
                        if (primaryAccountId == null) {
                            Result.failure(IllegalStateException("No financial account found."))
                        } else {
                            val amountMinor = MoneyUtils.parseToMinor(json.optString("amount"))
                                ?: return@launch emitMessage("Could not parse valid amount.")
                            val txType = if (intent == "record_income") "INCOME" else "EXPENSE"
                            val cat = json.optString("category").takeIf { it.isNotBlank() && it != "null" } ?: "General"
                            val desc = json.optString("title").takeIf { it.isNotBlank() && it != "null" }
                                ?: json.optString("content").takeIf { it.isNotBlank() && it != "null" }
                                ?: cat
                            repository.recordTransaction(
                                uid = currentUserId,
                                accountId = primaryAccountId,
                                type = txType,
                                amountMinor = amountMinor,
                                currency = defaultCurrency,
                                category = cat,
                                description = desc
                            )
                        }
                    }
                    "record_debt" -> {
                        val amountMinor = MoneyUtils.parseToMinor(json.optString("amount"))
                            ?: return@launch emitMessage("Could not parse valid loan amount.")
                        val counterparty = json.optString("counterparty").takeIf { it.isNotBlank() && it != "null" }
                            ?: "Contact"
                        val direction = json.optString("debtDirection").takeIf { it in listOf("RECEIVABLE", "PAYABLE") }
                            ?: "RECEIVABLE"
                        val notes = json.optString("content").takeIf { it.isNotBlank() && it != "null" }
                        repository.createDebt(
                            uid = currentUserId,
                            counterparty = counterparty,
                            direction = direction,
                            principalMinor = amountMinor,
                            currency = defaultCurrency,
                            notes = notes,
                            dueAtMillis = null
                        )
                    }
                    "create_reminder" -> {
                        val title = json.optString("title").takeIf { it.isNotBlank() && it != "null" }
                            ?: "Reminder"
                        val notes = json.optString("content").takeIf { it.isNotBlank() && it != "null" }
                        val trigger = json.optLong("triggerEpochMillis", 0L).let {
                            if (it > System.currentTimeMillis()) it else System.currentTimeMillis() + 3600_000L
                        }
                        val recurrence = json.optString("recurrence").takeIf { it in listOf("ONCE", "DAILY", "WEEKLY", "MONTHLY") }
                            ?: "ONCE"
                        val deliveryStatus = reminderScheduler.scheduleReminder("ai_rem", title, notes, trigger)
                        repository.createReminder(
                            uid = currentUserId,
                            title = title,
                            notes = notes,
                            triggerAtMillis = trigger,
                            timezoneId = TimeZone.getDefault().id,
                            recurrence = recurrence,
                            deliveryStatus = deliveryStatus
                        ).onSuccess { docId ->
                            reminderScheduler.scheduleReminder(docId, title, notes, trigger)
                        }
                    }
                    "create_task" -> {
                        val title = json.optString("title").takeIf { it.isNotBlank() && it != "null" }
                            ?: "New Task"
                        val notes = json.optString("content").takeIf { it.isNotBlank() && it != "null" }
                        val priority = json.optString("priority").takeIf { it in listOf("LOW", "MEDIUM", "HIGH") }
                            ?: "MEDIUM"
                        val due = json.optLong("triggerEpochMillis", 0L).takeIf { it > 0L }
                        repository.createTask(currentUserId, title, notes, priority, due)
                    }
                    "create_note" -> {
                        val title = json.optString("title").takeIf { it.isNotBlank() && it != "null" }
                            ?: "Saved Note"
                        val content = json.optString("content").takeIf { it.isNotBlank() && it != "null" }
                            ?: title
                        val cat = json.optString("category").uppercase().let {
                            if (it in listOf("PERSONAL", "WORK", "FINANCE", "HEALTH", "IDEA", "VOICE", "DOCUMENT")) it else "PERSONAL"
                        }
                        repository.saveMemory(
                            uid = currentUserId,
                            title = title,
                            content = content,
                            category = cat,
                            tags = listOf("ai"),
                            pinned = false,
                            sourceType = "NOTE"
                        )
                    }
                    else -> Result.failure(IllegalStateException("Unsupported confirmation action"))
                }

                result.onSuccess {
                    repository.updateAiMessageStatus(currentUserId, message.id, "CONFIRMED")
                    _events.emit("Action confirmed and saved.")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Failed to save action.")
                }
            } catch (e: Exception) {
                _events.emit(e.localizedMessage ?: "Failed to execute AI action")
            }
        }
    }

    fun cancelAiAction(message: AiMessageItem) {
        viewModelScope.launch {
            repository.updateAiMessageStatus(currentUserId, message.id, "CANCELLED")
        }
    }

    fun submitSupportTicket(subject: String, message: String) {
        viewModelScope.launch {
            repository.submitSupportTicket(currentUserId, subject, message)
                .onSuccess { _events.emit("Support ticket submitted.") }
                .onFailure { _events.emit(it.message ?: "Failed to submit ticket") }
        }
    }

    fun deleteAccountAndSignOut(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData(currentUserId)
                .onSuccess {
                    onDeleted()
                }
                .onFailure {
                    _events.emit(it.message ?: "Failed to delete user data")
                }
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch { _events.emit(msg) }
    }

    companion object {
        private const val TAG = "MainLifeVM"

        fun computeMonthlyTotals(transactions: List<TransactionItem>): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            val curMonth = cal.get(Calendar.MONTH)
            val curYear = cal.get(Calendar.YEAR)
            val reversedIds = transactions.mapNotNull { it.reversedTransactionId }.toSet()

            var income = 0L
            var expense = 0L
            for (tx in transactions) {
                if (tx.type == "REVERSAL" || tx.id in reversedIds) continue
                cal.timeInMillis = tx.occurredAtMillis
                if (cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear) {
                    if (tx.type == "INCOME") income += tx.amountMinor
                    else if (tx.type == "EXPENSE") expense += tx.amountMinor
                }
            }
            return income to expense
        }

        private fun computeNextOccurrence(currentMillis: Long, recurrence: String): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
            when (recurrence) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            }
            return cal.timeInMillis
        }
    }
}
