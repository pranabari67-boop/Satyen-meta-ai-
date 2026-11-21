package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.money.MoneyUtils
import com.example.data.model.AccountItem
import com.example.data.model.DebtItem
import com.example.data.model.MemoryItem
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryEditorDialog(
    initial: MemoryItem? = null,
    defaultSourceType: String = "MEMORY",
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, tagsRaw: String, pinned: Boolean, sourceType: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "PERSONAL") }
    var tagsRaw by remember { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }

    val categories = listOf("PERSONAL", "WORK", "FINANCE", "HEALTH", "IDEA")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Memory / Note" else "Edit Memory") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("memory_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("memory_content_input")
                )
                OutlinedTextField(
                    value = tagsRaw,
                    onValueChange = { tagsRaw = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin to top", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, content, category, tagsRaw, pinned, initial?.sourceType ?: defaultSourceType)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("memory_save_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String?, triggerAtMillis: Long, recurrence: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf("ONCE") }
    var offsetChoice by remember { mutableStateOf(1) } // 0=15m, 1=1h, 2=Tomorrow 8AM, 3=Next Week

    val recurrences = listOf("ONCE", "DAILY", "WEEKLY", "MONTHLY")
    val timeLabels = listOf("In 15 min", "In 1 hour", "Tomorrow 8 AM", "Next Week")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Reminder") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Remind me to...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("When", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    timeLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = offsetChoice == idx,
                            onClick = { offsetChoice = idx },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recurrences.forEach { rec ->
                        FilterChip(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                            label = { Text(rec) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    when (offsetChoice) {
                        0 -> cal.add(Calendar.MINUTE, 15)
                        1 -> cal.add(Calendar.HOUR_OF_DAY, 1)
                        2 -> {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 8)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                        }
                        else -> cal.add(Calendar.DAY_OF_YEAR, 7)
                    }
                    onSave(title, notes.takeIf { it.isNotBlank() }, cal.timeInMillis, recurrence)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("reminder_save_button")
            ) { Text("Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String?, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New To-Do Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, notes.takeIf { it.isNotBlank() }, priority) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("task_save_button")
            ) { Text("Add Task") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorDialog(
    accounts: List<AccountItem>,
    initialType: String = "EXPENSE",
    defaultCurrency: String = "INR",
    onDismiss: () -> Unit,
    onSave: (accountId: String, type: String, amountText: String, currency: String, category: String, description: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (initialType == "INCOME") "Salary" else "Groceries") }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }

    val categories = if (type == "INCOME") {
        listOf("Salary", "Freelance", "Business", "Gift", "Investment")
    } else {
        listOf("Groceries", "Food", "Transport", "Bills", "Health", "Shopping", "Other")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Ledger Entry") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EXPENSE", "INCOME").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                category = if (t == "INCOME") "Salary" else "Groceries"
                            },
                            label = { Text(t) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($defaultCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_desc_input")
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                if (accounts.isNotEmpty()) {
                    Text("Account", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text("${acc.name} (${MoneyUtils.formatMinor(acc.balanceMinor, acc.currency)})") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val accId = selectedAccountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() }
                    if (accId.isNotEmpty()) {
                        onSave(accId, type, amountText, defaultCurrency, category, description.ifBlank { category })
                    }
                },
                enabled = MoneyUtils.parseToMinor(amountText) != null && accounts.isNotEmpty(),
                modifier = Modifier.testTag("tx_save_button")
            ) { Text("Post Entry") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DebtEditorDialog(
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (counterparty: String, direction: String, amountText: String, currency: String, notes: String?) -> Unit
) {
    var counterparty by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("RECEIVABLE") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Loan / Credit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == "RECEIVABLE",
                        onClick = { direction = "RECEIVABLE" },
                        label = { Text("Lent (Receivable)") }
                    )
                    FilterChip(
                        selected = direction == "PAYABLE",
                        onClick = { direction = "PAYABLE" },
                        label = { Text("Borrowed (Payable)") }
                    )
                }
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text("Person / Entity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_person_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($defaultCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_amount_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(counterparty, direction, amountText, defaultCurrency, notes.takeIf { it.isNotBlank() })
                },
                enabled = counterparty.isNotBlank() && MoneyUtils.parseToMinor(amountText) != null,
                modifier = Modifier.testTag("debt_save_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DebtPaymentDialog(
    debt: DebtItem,
    onDismiss: () -> Unit,
    onPay: (amountText: String, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(MoneyUtils.minorToDecimalString(debt.remainingMinor)) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment — ${debt.counterparty}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Remaining: ${MoneyUtils.formatMinor(debt.remainingMinor, debt.currency)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (${debt.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_pay_amount_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsed = MoneyUtils.parseToMinor(amountText)
            Button(
                onClick = { onPay(amountText, notes.takeIf { it.isNotBlank() }) },
                enabled = parsed != null && parsed <= debt.remainingMinor,
                modifier = Modifier.testTag("debt_pay_confirm_button")
            ) { Text("Record Payment") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
