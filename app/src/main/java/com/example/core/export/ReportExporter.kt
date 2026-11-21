package com.example.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.core.money.MoneyUtils
import com.example.data.model.AccountItem
import com.example.data.model.DebtItem
import com.example.data.model.MemoryItem
import com.example.data.model.ReminderItem
import com.example.data.model.TaskItem
import com.example.data.model.TransactionItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun exportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun shareCsvReport(
        context: Context,
        transactions: List<TransactionItem>,
        debts: List<DebtItem>,
        currency: String
    ): Result<Unit> = runCatching {
        val file = File(exportDir(context), "satyen_accounting_report.csv")
        val sb = StringBuilder()
        sb.appendLine("RecordType,Date,Type,Category,CounterpartyOrDescription,Amount,Currency,Status")
        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.occurredAtMillis))
            val desc = tx.description.replace("\"", "\"\"")
            val amt = MoneyUtils.minorToDecimalString(tx.amountMinor)
            sb.appendLine("TRANSACTION,\"$dateStr\",${tx.type},\"${tx.category}\",\"$desc\",$amt,${tx.currency},POSTED")
        }
        for (debt in debts) {
            val notes = (debt.notes ?: "").replace("\"", "\"\"")
            val rem = MoneyUtils.minorToDecimalString(debt.remainingMinor)
            sb.appendLine("DEBT,,${debt.direction},Loan,\"${debt.counterparty} ($notes)\",$rem,${debt.currency},${debt.status}")
        }
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Satyen Meta AI Accounting CSV")
    }

    fun sharePdfReport(
        context: Context,
        accounts: List<AccountItem>,
        transactions: List<TransactionItem>,
        debts: List<DebtItem>,
        currency: String,
        incomeMinor: Long,
        expenseMinor: Long
    ): Result<Unit> = runCatching {
        val file = File(exportDir(context), "satyen_financial_report.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(11, 23, 59)
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(29, 78, 216)
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
        }

        var y = 48f
        canvas.drawText("SATYEN META AI — Financial Statement", 40f, y, titlePaint)
        y += 22f
        canvas.drawText("Generated: ${dateFormat.format(Date())}", 40f, y, bodyPaint)
        y += 28f

        canvas.drawText("Profit & Loss Summary", 40f, y, headerPaint)
        y += 18f
        canvas.drawText("Total Income: ${MoneyUtils.formatMinor(incomeMinor, currency)}", 40f, y, bodyPaint)
        y += 16f
        canvas.drawText("Total Expense: ${MoneyUtils.formatMinor(expenseMinor, currency)}", 40f, y, bodyPaint)
        y += 16f
        canvas.drawText("Net Savings: ${MoneyUtils.formatMinor(incomeMinor - expenseMinor, currency)}", 40f, y, bodyPaint)
        y += 26f

        canvas.drawText("Accounts", 40f, y, headerPaint)
        y += 18f
        accounts.take(8).forEach { acc ->
            canvas.drawText("${acc.name} (${acc.accountType}): ${MoneyUtils.formatMinor(acc.balanceMinor, acc.currency)}", 40f, y, bodyPaint)
            y += 15f
        }
        y += 16f

        canvas.drawText("Recent Ledger Entries (Top 18)", 40f, y, headerPaint)
        y += 18f
        transactions.take(18).forEach { tx ->
            if (y < 760f) {
                val line = "${dateFormat.format(Date(tx.occurredAtMillis))} | ${tx.type} | ${tx.category} | ${MoneyUtils.formatMinor(tx.amountMinor, tx.currency)} | ${tx.description.take(28)}"
                canvas.drawText(line, 40f, y, bodyPaint)
                y += 15f
            }
        }
        y += 16f

        if (y < 740f && debts.isNotEmpty()) {
            canvas.drawText("Active Debts & Credits", 40f, y, headerPaint)
            y += 18f
            debts.take(6).forEach { debt ->
                if (y < 800f) {
                    canvas.drawText("${debt.counterparty} (${debt.direction}): Remaining ${MoneyUtils.formatMinor(debt.remainingMinor, debt.currency)} [${debt.status}]", 40f, y, bodyPaint)
                    y += 15f
                }
            }
        }

        doc.finishPage(page)
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()
        shareFile(context, file, "application/pdf", "Satyen Meta AI Financial PDF")
    }

    fun shareFullJsonExport(
        context: Context,
        memories: List<MemoryItem>,
        reminders: List<ReminderItem>,
        tasks: List<TaskItem>,
        accounts: List<AccountItem>,
        transactions: List<TransactionItem>,
        debts: List<DebtItem>
    ): Result<Unit> = runCatching {
        val file = File(exportDir(context), "satyen_meta_ai_backup.json")
        val root = JSONObject().apply {
            put("exportedAt", dateFormat.format(Date()))
            put("memories", JSONArray().apply {
                memories.forEach { m ->
                    put(JSONObject().apply {
                        put("title", m.title)
                        put("content", m.content)
                        put("category", m.category)
                        put("sourceType", m.sourceType)
                        put("pinned", m.pinned)
                    })
                }
            })
            put("reminders", JSONArray().apply {
                reminders.forEach { r ->
                    put(JSONObject().apply {
                        put("title", r.title)
                        put("triggerAtMillis", r.triggerAtMillis)
                        put("timezoneId", r.timezoneId)
                        put("recurrence", r.recurrence)
                        put("status", r.status)
                    })
                }
            })
            put("tasks", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("title", t.title)
                        put("priority", t.priority)
                        put("completed", t.completed)
                    })
                }
            })
            put("accounts", JSONArray().apply {
                accounts.forEach { a ->
                    put(JSONObject().apply {
                        put("name", a.name)
                        put("type", a.accountType)
                        put("currency", a.currency)
                        put("balanceMinor", a.balanceMinor)
                    })
                }
            })
            put("transactions", JSONArray().apply {
                transactions.forEach { tx ->
                    put(JSONObject().apply {
                        put("type", tx.type)
                        put("category", tx.category)
                        put("description", tx.description)
                        put("amountMinor", tx.amountMinor)
                        put("currency", tx.currency)
                    })
                }
            })
            put("debts", JSONArray().apply {
                debts.forEach { d ->
                    put(JSONObject().apply {
                        put("counterparty", d.counterparty)
                        put("direction", d.direction)
                        put("principalMinor", d.principalMinor)
                        put("remainingMinor", d.remainingMinor)
                        put("currency", d.currency)
                        put("status", d.status)
                    })
                }
            })
        }
        file.writeText(root.toString(2))
        shareFile(context, file, "application/json", "Satyen Meta AI Full Backup")
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
