package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.i18n.AppStrings
import com.example.core.money.MoneyUtils
import com.example.data.model.AccountItem
import com.example.data.model.DebtItem
import com.example.data.model.TransactionItem
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VioletGlow
import com.example.ui.viewmodel.MainLifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountingHubScreen(
    strings: AppStrings,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    currency: String,
    monthlyBudgetMinor: Long,
    accounts: List<AccountItem>,
    transactions: List<TransactionItem>,
    debts: List<DebtItem>,
    onAddTransaction: () -> Unit,
    onReverseTransaction: (TransactionItem) -> Unit,
    onCreateAccount: (name: String, type: String, currency: String, initialBalance: String) -> Unit,
    onAddDebt: () -> Unit,
    onPayDebt: (DebtItem) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    var showAddAccount by remember { mutableStateOf(false) }

    val tabs = listOf(
        strings.accountingOverview,
        strings.accountingLedger,
        strings.accountingDebts,
        strings.accountingReports
    )

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1 || selectedTab == 2) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0, 1 -> onAddTransaction()
                            2 -> onAddDebt()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("accounting_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add financial entry")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { onSelectTab(idx) },
                        text = { Text(label, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AccountingOverviewTab(
                    strings = strings,
                    currency = currency,
                    monthlyBudgetMinor = monthlyBudgetMinor,
                    accounts = accounts,
                    transactions = transactions,
                    onOpenAddAccount = { showAddAccount = true }
                )
                1 -> LedgerTab(
                    strings = strings,
                    transactions = transactions,
                    onReverse = onReverseTransaction
                )
                2 -> DebtsTab(
                    strings = strings,
                    debts = debts,
                    onPayDebt = onPayDebt
                )
                else -> ReportsTab(
                    strings = strings,
                    currency = currency,
                    transactions = transactions,
                    onExportCsv = {
                        ReportExporter.shareCsvReport(context, transactions, debts, currency)
                            .onFailure { onNotify(it.localizedMessage ?: "CSV export failed") }
                    },
                    onExportPdf = {
                        val (inc, exp) = MainLifeViewModel.computeMonthlyTotals(transactions)
                        ReportExporter.sharePdfReport(context, accounts, transactions, debts, currency, inc, exp)
                            .onFailure { onNotify(it.localizedMessage ?: "PDF export failed") }
                    }
                )
            }
        }
    }

    if (showAddAccount) {
        AddAccountDialog(
            defaultCurrency = currency,
            onDismiss = { showAddAccount = false },
            onSave = { name, type, bal ->
                showAddAccount = false
                onCreateAccount(name, type, currency, bal)
            }
        )
    }
}

@Composable
private fun AccountingOverviewTab(
    strings: AppStrings,
    currency: String,
    monthlyBudgetMinor: Long,
    accounts: List<AccountItem>,
    transactions: List<TransactionItem>,
    onOpenAddAccount: () -> Unit
) {
    val (monthIncome, monthExpense) = MainLifeViewModel.computeMonthlyTotals(transactions)
    val budgetRatio = if (monthlyBudgetMinor > 0L) {
        (monthExpense.toFloat() / monthlyBudgetMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val categoryTotals = remember(transactions) {
        val reversedIds = transactions.mapNotNull { it.reversedTransactionId }.toSet()
        transactions
            .filter { it.type == "EXPENSE" && it.id !in reversedIds }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.monthlyBudget, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Spent: ${MoneyUtils.formatMinor(monthExpense, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (monthExpense > monthlyBudgetMinor) CrimsonExpense else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Target: ${MoneyUtils.formatMinor(monthlyBudgetMinor, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { budgetRatio },
                        color = if (monthExpense > monthlyBudgetMinor) CrimsonExpense else RoyalBlue,
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${strings.monthlyIncome}: ${MoneyUtils.formatMinor(monthIncome, currency)}", color = EmeraldIncome, style = MaterialTheme.typography.labelLarge)
                        Text("Net: ${MoneyUtils.formatMinor(monthIncome - monthExpense, currency)}", color = RoyalBlue, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenAddAccount) {
                    Text("+ Add Account")
                }
            }
        }

        items(accounts, key = { it.id }) { acc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(acc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(acc.accountType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = MoneyUtils.formatMinor(acc.balanceMinor, acc.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (categoryTotals.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Expense Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        val maxVal = categoryTotals.maxOf { it.value }.coerceAtLeast(1L)
                        val palette = listOf(RoyalBlue, VioletGlow, CyanSpark, AmberWarn, CrimsonExpense)
                        categoryTotals.forEachIndexed { idx, entry ->
                            val ratio = entry.value.toFloat() / maxVal.toFloat()
                            val barColor = palette[idx % palette.size]
                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(MoneyUtils.formatMinor(entry.value, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                                    drawRoundRect(
                                        color = barColor.copy(alpha = 0.16f),
                                        size = size,
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset.Zero,
                                        size = Size(size.width * ratio, size.height),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTab(
    strings: AppStrings,
    transactions: List<TransactionItem>,
    onReverse: (TransactionItem) -> Unit
) {
    var filter by remember { mutableStateOf("ALL") }
    val reversedIds = remember(transactions) { transactions.mapNotNull { it.reversedTransactionId }.toSet() }
    val filtered = remember(transactions, filter) {
        if (filter == "ALL") transactions else transactions.filter { it.type == filter }
    }
    val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "EXPENSE", "INCOME", "REVERSAL").forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    strings.emptyTransactions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                val isReversed = tx.id in reversedIds
                val amountColor = when (tx.type) {
                    "INCOME" -> EmeraldIncome
                    "REVERSAL" -> AmberWarn
                    else -> CrimsonExpense
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${tx.category} • ${fmt.format(Date(tx.occurredAtMillis))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = MoneyUtils.formatMinor(tx.amountMinor, tx.currency, signed = tx.type == "INCOME"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                        }
                        if (tx.type != "REVERSAL" && !isReversed) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onReverse(tx) }) {
                                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.reverseTransaction)
                                }
                            }
                        } else if (isReversed || tx.type == "REVERSAL") {
                            Spacer(Modifier.height(4.dp))
                            Surface(color = AmberWarn.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = if (tx.type == "REVERSAL") strings.reversedBadge else "REVERSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarn,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtsTab(
    strings: AppStrings,
    debts: List<DebtItem>,
    onPayDebt: (DebtItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (debts.isEmpty()) {
            item {
                Text(
                    strings.emptyDebts,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(debts, key = { it.id }) { debt ->
                val isReceivable = debt.direction == "RECEIVABLE"
                val progress = if (debt.principalMinor > 0L) {
                    ((debt.principalMinor - debt.remainingMinor).toFloat() / debt.principalMinor.toFloat()).coerceIn(0f, 1f)
                } else 1f
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(debt.counterparty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isReceivable) strings.receivables else strings.payables,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isReceivable) EmeraldIncome else CrimsonExpense
                                )
                            }
                            Text(
                                MoneyUtils.formatMinor(debt.remainingMinor, debt.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = if (isReceivable) EmeraldIncome else RoyalBlue,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Principal: ${MoneyUtils.formatMinor(debt.principalMinor, debt.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (debt.status == "OPEN") {
                                Button(onClick = { onPayDebt(debt) }) {
                                    Text(strings.recordPayment)
                                }
                            } else {
                                Surface(color = EmeraldIncome.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        strings.settleDebt,
                                        color = EmeraldIncome,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    strings: AppStrings,
    currency: String,
    transactions: List<TransactionItem>,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    val (monthInc, monthExp) = MainLifeViewModel.computeMonthlyTotals(transactions)
    val reversedIds = remember(transactions) { transactions.mapNotNull { it.reversedTransactionId }.toSet() }
    val allIncome = transactions.filter { it.type == "INCOME" && it.id !in reversedIds }.sumOf { it.amountMinor }
    val allExpense = transactions.filter { it.type == "EXPENSE" && it.id !in reversedIds }.sumOf { it.amountMinor }

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
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profit & Loss Statement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("This Month Income: ${MoneyUtils.formatMinor(monthInc, currency)}", color = EmeraldIncome)
                    Text("This Month Expense: ${MoneyUtils.formatMinor(monthExp, currency)}", color = CrimsonExpense)
                    Text("This Month Net: ${MoneyUtils.formatMinor(monthInc - monthExp, currency)}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("All-Time Income: ${MoneyUtils.formatMinor(allIncome, currency)}")
                    Text("All-Time Expense: ${MoneyUtils.formatMinor(allExpense, currency)}")
                    Text("All-Time Net Savings: ${MoneyUtils.formatMinor(allIncome - allExpense, currency)}", fontWeight = FontWeight.Bold, color = RoyalBlue)
                }
            }
        }

        item {
            Button(
                onClick = onExportPdf,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.exportPdf)
            }
        }

        item {
            OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_csv_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.exportCsv)
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, balance: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("WALLET") }
    var balance by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Financial Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. bKash / HDFC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH", "BANK", "WALLET", "CARD").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Opening Balance ($defaultCurrency)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, type, balance) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
