package com.example.data.repository

import android.util.Log
import com.example.core.money.MoneyUtils
import com.example.data.local.AppLanguage
import com.example.data.model.AccountItem
import com.example.data.model.DebtItem
import com.example.data.model.MemoryItem
import com.example.data.model.ReminderItem
import com.example.data.model.TaskItem
import com.example.data.model.TransactionItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AiToolPlan(
    val replyText: String,
    val intent: String,
    val requiresConfirmation: Boolean,
    val actionSummary: String?,
    val payloadJson: String?
)

class AiAssistantRepository {
    private val toolResponseSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(description = "Helpful natural response in Bengali or English matching the user's language."),
            "intent" to Schema.enumeration(
                values = listOf(
                    "chat",
                    "clarify",
                    "create_note",
                    "search_notes",
                    "create_reminder",
                    "list_reminders",
                    "create_task",
                    "record_income",
                    "record_expense",
                    "record_debt",
                    "get_financial_summary",
                    "search_personal_memory",
                    "get_upcoming_events"
                ),
                description = "Detected user intent / tool call."
            ),
            "requiresConfirmation" to Schema.boolean(description = "True when creating a financial record, debt, reminder, task, or memory that should be confirmed by the user before saving."),
            "actionSummary" to Schema.string(description = "Concise 1-line summary of the proposed action for the confirmation card.", nullable = true),
            "title" to Schema.string(description = "Title for note, memory, reminder, or task.", nullable = true),
            "content" to Schema.string(description = "Body content or notes.", nullable = true),
            "category" to Schema.string(description = "Category name (e.g. Groceries, Transport, Salary, PERSONAL, WORK, FINANCE).", nullable = true),
            "amount" to Schema.string(description = "Decimal amount string e.g. 500 or 10000.50 (digits only).", nullable = true),
            "currency" to Schema.string(description = "3-letter currency code e.g. INR, BDT, USD.", nullable = true),
            "counterparty" to Schema.string(description = "Person or entity name for debt/loan.", nullable = true),
            "debtDirection" to Schema.enumeration(values = listOf("RECEIVABLE", "PAYABLE"), description = "RECEIVABLE if user lent money, PAYABLE if user borrowed money.", nullable = true),
            "triggerEpochMillis" to Schema.integer(description = "Exact UTC epoch milliseconds for reminder or task due time if known.", nullable = true),
            "recurrence" to Schema.enumeration(values = listOf("ONCE", "DAILY", "WEEKLY", "MONTHLY"), description = "Recurrence schedule for reminders.", nullable = true),
            "priority" to Schema.enumeration(values = listOf("LOW", "MEDIUM", "HIGH"), description = "Priority for tasks.", nullable = true),
            "query" to Schema.string(description = "Search query string for memories or notes.", nullable = true)
        ),
        optionalProperties = listOf(
            "actionSummary",
            "title",
            "content",
            "category",
            "amount",
            "currency",
            "counterparty",
            "debtDirection",
            "triggerEpochMillis",
            "recurrence",
            "priority",
            "query"
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = toolResponseSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun processUserMessage(
        userMessage: String,
        language: AppLanguage,
        defaultCurrency: String,
        accounts: List<AccountItem>,
        transactions: List<TransactionItem>,
        debts: List<DebtItem>,
        reminders: List<ReminderItem>,
        tasks: List<TaskItem>,
        memories: List<MemoryItem>,
        monthlyIncomeMinor: Long,
        monthlyExpenseMinor: Long
    ): Result<AiToolPlan> {
        return try {
            val nowMillis = System.currentTimeMillis()
            val tz = TimeZone.getDefault()
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm Z", Locale.US).apply { timeZone = tz }
            val currentDateTimeStr = fmt.format(Date(nowMillis))

            val totalBalance = accounts.sumOf { it.balanceMinor }
            val receivableTotal = debts.filter { it.direction == "RECEIVABLE" && it.status == "OPEN" }.sumOf { it.remainingMinor }
            val payableTotal = debts.filter { it.direction == "PAYABLE" && it.status == "OPEN" }.sumOf { it.remainingMinor }

            val contextBuilder = StringBuilder()
            contextBuilder.appendLine("CURRENT_LOCAL_TIME: $currentDateTimeStr (epoch_ms=$nowMillis, timezone=${tz.id})")
            contextBuilder.appendLine("DEFAULT_CURRENCY: $defaultCurrency, PREFERRED_UI_LANG: ${language.code}")
            contextBuilder.appendLine("REAL_USER_FINANCIAL_SUMMARY:")
            contextBuilder.appendLine("- Total Balance: ${MoneyUtils.formatMinor(totalBalance, defaultCurrency)}")
            contextBuilder.appendLine("- This Month Income: ${MoneyUtils.formatMinor(monthlyIncomeMinor, defaultCurrency)}")
            contextBuilder.appendLine("- This Month Expense: ${MoneyUtils.formatMinor(monthlyExpenseMinor, defaultCurrency)}")
            contextBuilder.appendLine("- Open Receivables (Loans Given): ${MoneyUtils.formatMinor(receivableTotal, defaultCurrency)}")
            contextBuilder.appendLine("- Open Payables (Loans Received): ${MoneyUtils.formatMinor(payableTotal, defaultCurrency)}")
            contextBuilder.appendLine("RECENT_TRANSACTIONS (max 10):")
            transactions.take(10).forEach { tx ->
                contextBuilder.appendLine("  * ${fmt.format(Date(tx.occurredAtMillis))} | ${tx.type} | ${MoneyUtils.formatMinor(tx.amountMinor, tx.currency)} | ${tx.category} | ${tx.description}")
            }
            contextBuilder.appendLine("OPEN_DEBTS (max 10):")
            debts.filter { it.status == "OPEN" }.take(10).forEach { d ->
                contextBuilder.appendLine("  * ${d.counterparty} | ${d.direction} | Remaining: ${MoneyUtils.formatMinor(d.remainingMinor, d.currency)}")
            }
            contextBuilder.appendLine("UPCOMING_REMINDERS (max 10):")
            reminders.filter { it.status == "PENDING" || it.status == "SNOOZED" }.take(10).forEach { r ->
                contextBuilder.appendLine("  * ${r.title} at ${fmt.format(Date(r.triggerAtMillis))} (${r.recurrence})")
            }
            contextBuilder.appendLine("PENDING_TASKS (max 10):")
            tasks.filter { !it.completed }.take(10).forEach { t ->
                val dueStr = t.dueAtMillis?.let { fmt.format(Date(it)) } ?: "No due date"
                contextBuilder.appendLine("  * [${t.priority}] ${t.title} ($dueStr)")
            }
            contextBuilder.appendLine("RECENT_MEMORIES (max 12):")
            memories.take(12).forEach { m ->
                contextBuilder.appendLine("  * [${m.category}] ${m.title}: ${m.content.take(160)}")
            }

            val prompt = """
                You are SATYEN META AI ("Your Complete Life. One Intelligent Assistant." / "আপনার জীবনের সবকিছু মনে রাখবে এক অ্যাপ।").
                You understand both Bengali (বাংলা) and English fluently. Always reply in the language used by the user (or PREFERRED_UI_LANG if ambiguous).

                STRICT RULES:
                1. NEVER invent or hallucinate financial amounts, transactions, debts, reminders, tasks, or memories. Only cite numbers and items present in REAL_USER_FINANCIAL_SUMMARY / lists below.
                2. NEVER claim an expense, income, debt, reminder, task, or note has already been saved. When the user asks to save/record/remind/create something, set `requiresConfirmation = true` and explain what will be saved upon confirmation.
                3. If an essential parameter is missing (e.g., expense amount missing, or reminder time missing), set `intent = "clarify"`, `requiresConfirmation = false`, and politely ask a brief clarification question.
                4. For Bengali inputs like "টাকা" in INR/BDT context, preserve DEFAULT_CURRENCY ($defaultCurrency) unless the user explicitly names another currency (e.g. USD/BDT/INR). Note: when users say "আগামীকাল সকাল ৮টা" (tomorrow 8 AM), compute exact `triggerEpochMillis` relative to `epoch_ms=$nowMillis` in timezone `${tz.id}`.
                5. For "রহিমকে ১০,০০০ টাকা ধার দিলাম" (lent money to Rahim), set `intent = "record_debt"`, `debtDirection = "RECEIVABLE"`, `counterparty = "Rahim"`, `amount = "10000"`.
                6. For "এই মাসে আমার মোট খরচ কত?" or "আগামী সাত দিনে কী কী কাজ আছে?", use `get_financial_summary` or `get_upcoming_events`, `requiresConfirmation = false`, and answer strictly from the provided user data.

                AUTHENTICATED USER CONTEXT:
                $contextBuilder

                USER MESSAGE:
                $userMessage
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty response."))

            val json = JSONObject(rawJson)
            val replyText = json.optString("replyText").ifBlank { "I understood your request." }
            val intent = json.optString("intent", "chat")
            val requiresConfirmation = json.optBoolean("requiresConfirmation", false)
            val actionSummary = json.optString("actionSummary").takeIf { it.isNotBlank() && it != "null" }

            val payloadJson = if (requiresConfirmation) json.toString() else null
            Result.success(
                AiToolPlan(
                    replyText = replyText,
                    intent = intent,
                    requiresConfirmation = requiresConfirmation,
                    actionSummary = actionSummary,
                    payloadJson = payloadJson
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI generation timed out", e)
            Result.failure(Exception("AI request timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiAssistantRepo"
    }
}
