package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.i18n.AppStrings
import com.example.core.money.MoneyUtils
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode
import com.example.data.model.AccountItem
import com.example.data.model.AuditLogItem
import com.example.data.model.DebtItem
import com.example.data.model.MemoryItem
import com.example.data.model.ReminderItem
import com.example.data.model.SupportTicketItem
import com.example.data.model.TaskItem
import com.example.data.model.TransactionItem
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VioletGlow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    strings: AppStrings,
    displayName: String,
    email: String?,
    language: AppLanguage,
    themeMode: AppThemeMode,
    currency: String,
    monthlyBudgetMinor: Long,
    biometricLockEnabled: Boolean,
    subscriptionTier: String,
    memories: List<MemoryItem>,
    reminders: List<ReminderItem>,
    tasks: List<TaskItem>,
    accounts: List<AccountItem>,
    transactions: List<TransactionItem>,
    debts: List<DebtItem>,
    auditLogs: List<AuditLogItem>,
    supportTickets: List<SupportTicketItem>,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onSaveBudget: (String) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSubmitSupportTicket: (subject: String, message: String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var budgetInput by remember(monthlyBudgetMinor) {
        mutableStateOf(MoneyUtils.minorToDecimalString(monthlyBudgetMinor))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!email.isNullOrBlank()) {
                        Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Plan: $subscriptionTier", style = MaterialTheme.typography.labelLarge, color = VioletGlow)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.profilePreferences, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = { Text(lang.displayName) }
                            )
                        }
                    }

                    Text(strings.themeLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(mode.name) }
                            )
                        }
                    }

                    Text(strings.currencyLabel, style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoneyUtils.supportedCurrencies.forEach { curr ->
                            FilterChip(
                                selected = currency == curr,
                                onClick = { onSelectCurrency(curr) },
                                label = { Text("$curr (${MoneyUtils.currencySymbol(curr)})") }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it },
                            label = { Text("${strings.budgetLabel} ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onSaveBudget(budgetInput) }) {
                            Text(strings.save)
                        }
                    }
                }
            }
        }

        item {
            ProfileNavRow(
                icon = Icons.Default.WorkspacePremium,
                title = strings.subscriptionPlans,
                subtitle = "Free & Premium capabilities",
                onClick = { showSubscriptionDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            ProfileNavRow(
                icon = Icons.Default.Security,
                title = strings.privacySecurity,
                subtitle = "Audit history (${auditLogs.size}), JSON backup & lock",
                onClick = { showPrivacyDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            ProfileNavRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = strings.helpSupport,
                subtitle = "Submit support ticket (${supportTickets.size} open/sent)",
                onClick = { showHelpDialog = true }
            )
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("delete_account_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.deleteAccountData)
            }
        }
    }

    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            title = { Text(strings.subscriptionPlans) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FREE PLAN (Active)", fontWeight = FontWeight.Bold, color = RoyalBlue)
                    Text("• Personal memory, notes, reminders & to-do\n• Exact multi-currency accounting & debt ledger\n• Bilingual Bengali & English Gemini AI Assistant")
                    Spacer(Modifier.height(6.dp))
                    Text("PREMIUM PLAN (Google Play Billing)", fontWeight = FontWeight.Bold, color = VioletGlow)
                    Text("• Extended monthly AI quota & priority reasoning\n• Advanced analytics & expanded cloud vault\n• Verified directly via Google Play Billing Console when published.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubscriptionDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacySecurity) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require app lock on launch", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = biometricLockEnabled, onCheckedChange = onToggleBiometric)
                    }
                    OutlinedButton(
                        onClick = {
                            ReportExporter.shareFullJsonExport(
                                context = context,
                                memories = memories,
                                reminders = reminders,
                                tasks = tasks,
                                accounts = accounts,
                                transactions = transactions,
                                debts = debts
                            ).onFailure { onNotify(it.localizedMessage ?: "Export failed") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportAllJson)
                    }
                    Text("Recent Security & Ledger Audit Logs", fontWeight = FontWeight.Bold)
                    auditLogs.take(5).forEach { log ->
                        Text("• ${log.action}: ${log.summary}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showHelpDialog) {
        var subject by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(strings.helpSupport) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Describe your issue or feedback") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (supportTickets.isNotEmpty()) {
                        Text("Recent Tickets:", style = MaterialTheme.typography.labelLarge)
                        supportTickets.take(3).forEach { t ->
                            Text("• [${t.status}] ${t.subject}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitSupportTicket(subject, body)
                        showHelpDialog = false
                    },
                    enabled = subject.isNotBlank() && body.isNotBlank()
                ) { Text(strings.submitTicket) }
            },
            dismissButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense)
                ) { Text(strings.confirmDelete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ProfileNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchScreen(
    strings: AppStrings,
    memories: List<MemoryItem>,
    reminders: List<ReminderItem>,
    tasks: List<TaskItem>,
    transactions: List<TransactionItem>,
    debts: List<DebtItem>,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    val matchedMemories = remember(memories, q) {
        if (q.isEmpty()) emptyList() else memories.filter {
            it.title.lowercase().contains(q) || it.content.lowercase().contains(q) || it.tags.any { t -> t.lowercase().contains(q) }
        }
    }
    val matchedReminders = remember(reminders, q) {
        if (q.isEmpty()) emptyList() else reminders.filter { it.title.lowercase().contains(q) }
    }
    val matchedTasks = remember(tasks, q) {
        if (q.isEmpty()) emptyList() else tasks.filter { it.title.lowercase().contains(q) }
    }
    val matchedTx = remember(transactions, q) {
        if (q.isEmpty()) emptyList() else transactions.filter {
            it.description.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }
    val matchedDebts = remember(debts, q) {
        if (q.isEmpty()) emptyList() else debts.filter { it.counterparty.lowercase().contains(q) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.smartSearch) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search across memories, tasks, reminders, ledger, debts...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("smart_search_input")
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(matchedMemories, key = { "m_${it.id}" }) { m ->
                    SearchHitCard(badge = "MEMORY", title = m.title, subtitle = m.content.take(120))
                }
                items(matchedReminders, key = { "r_${it.id}" }) { r ->
                    SearchHitCard(badge = "REMINDER", title = r.title, subtitle = "${r.status} • ${r.recurrence}")
                }
                items(matchedTasks, key = { "t_${it.id}" }) { t ->
                    SearchHitCard(badge = "TASK", title = t.title, subtitle = t.priority)
                }
                items(matchedTx, key = { "tx_${it.id}" }) { tx ->
                    SearchHitCard(
                        badge = tx.type,
                        title = tx.description,
                        subtitle = "${tx.category} • ${MoneyUtils.formatMinor(tx.amountMinor, tx.currency)}"
                    )
                }
                items(matchedDebts, key = { "d_${it.id}" }) { d ->
                    SearchHitCard(
                        badge = d.direction,
                        title = d.counterparty,
                        subtitle = MoneyUtils.formatMinor(d.remainingMinor, d.currency)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHitCard(badge: String, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(badge, style = MaterialTheme.typography.labelSmall, color = EmeraldIncome, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
