package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.AppStrings
import com.example.core.money.MoneyUtils
import com.example.data.local.AppLanguage
import com.example.data.model.AccountItem
import com.example.data.model.DebtItem
import com.example.data.model.MemoryItem
import com.example.data.model.ReminderItem
import com.example.data.model.TaskItem
import com.example.data.model.TransactionItem
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.VioletGlow
import com.example.ui.viewmodel.MainLifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun HomeDashboardScreen(
    displayName: String,
    strings: AppStrings,
    language: AppLanguage,
    currency: String,
    hasNotificationPermission: Boolean,
    accounts: List<AccountItem>,
    transactions: List<TransactionItem>,
    debts: List<DebtItem>,
    reminders: List<ReminderItem>,
    tasks: List<TaskItem>,
    memories: List<MemoryItem>,
    onToggleLanguage: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenMemoryHub: (Int) -> Unit,
    onOpenAccountingHub: (Int) -> Unit,
    onOpenSmartSearch: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onQuickAddIncome: () -> Unit,
    onQuickAddMemory: () -> Unit,
    onQuickAddReminder: () -> Unit,
    onQuickAddTask: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onCompleteReminder: (ReminderItem) -> Unit
) {
    val totalBalanceMinor = accounts.sumOf { it.balanceMinor }
    val (monthIncome, monthExpense) = MainLifeViewModel.computeMonthlyTotals(transactions)
    val openReceivable = debts.filter { it.direction == "RECEIVABLE" && it.status == "OPEN" }.sumOf { it.remainingMinor }
    val openPayable = debts.filter { it.direction == "PAYABLE" && it.status == "OPEN" }.sumOf { it.remainingMinor }
    val activeReminders = reminders.filter { it.status == "PENDING" || it.status == "SNOOZED" }
    val pendingTasks = tasks.filter { !it.completed }

    val quickActions = listOf(
        QuickActionSpec(strings.askAi, Icons.Default.AutoAwesome, VioletGlow, "qa_ask_ai", onOpenAiAssistant),
        QuickActionSpec(strings.addExpense, Icons.Default.TrendingDown, CrimsonExpense, "qa_expense", onQuickAddExpense),
        QuickActionSpec(strings.addIncome, Icons.Default.TrendingUp, EmeraldIncome, "qa_income", onQuickAddIncome),
        QuickActionSpec(strings.addMemory, Icons.Default.Psychology, CyanSpark, "qa_memory", onQuickAddMemory),
        QuickActionSpec(strings.addReminder, Icons.Default.NotificationsActive, AmberWarn, "qa_reminder", onQuickAddReminder),
        QuickActionSpec(strings.addTask, Icons.Default.TaskAlt, RoyalBlueBright, "qa_task", onQuickAddTask),
        QuickActionSpec(strings.voiceNotes, Icons.Default.Mic, VioletGlow, "qa_voice") { onOpenMemoryHub(3) },
        QuickActionSpec(strings.documentVault, Icons.Default.Description, CyanSpark, "qa_vault") { onOpenMemoryHub(3) },
        QuickActionSpec(strings.reportsExport, Icons.Default.Assessment, EmeraldIncome, "qa_reports") { onOpenAccountingHub(3) }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Identity Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    NavyDeep.copy(alpha = 0.88f),
                                    RoyalBlue.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.welcomeBack}, ${displayName.substringBefore(" ")}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (language == AppLanguage.BN) strings.bengaliSlogan else strings.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDBEAFE)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = onToggleLanguage,
                                label = { Text(if (language == AppLanguage.EN) "বাংলা" else "EN", color = Color.White) },
                                modifier = Modifier.testTag("home_lang_toggle")
                            )
                            IconButton(
                                onClick = onOpenSmartSearch,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .testTag("home_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = strings.smartSearch, tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = strings.totalCashBalance,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = MoneyUtils.formatMinor(totalBalanceMinor, currency),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.monthlyIncome, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                                Text(
                                    MoneyUtils.formatMinor(monthIncome, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.monthlyExpense, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFECACA))
                                Text(
                                    MoneyUtils.formatMinor(monthExpense, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!hasNotificationPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberWarn.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = AmberWarn)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = strings.notificationPermissionBanner,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.testTag("grant_notifications_button")
                        ) {
                            Text(strings.grantPermission)
                        }
                    }
                }
            }
        }

        item {
            // Snapshot Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SnapshotStatCard(
                    title = strings.receivables,
                    value = MoneyUtils.formatMinor(openReceivable, currency),
                    accent = EmeraldIncome,
                    onClick = { onOpenAccountingHub(2) },
                    modifier = Modifier.weight(1f)
                )
                SnapshotStatCard(
                    title = strings.payables,
                    value = MoneyUtils.formatMinor(openPayable, currency),
                    accent = CrimsonExpense,
                    onClick = { onOpenAccountingHub(2) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quickActions, key = { it.tag }) { action ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .width(96.dp)
                            .clickable { action.onClick() }
                            .testTag(action.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(action.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(action.icon, contentDescription = action.label, tint = action.accent)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "${strings.upcomingReminders} (${activeReminders.size})",
                actionLabel = strings.memoryTabReminders,
                onAction = { onOpenMemoryHub(1) }
            )
        }

        if (activeReminders.isEmpty()) {
            item {
                EmptyTipCard(text = strings.emptyReminders, onClick = onQuickAddReminder)
            }
        } else {
            items(activeReminders.take(3), key = { it.id }) { rem ->
                ReminderPreviewCard(
                    reminder = rem,
                    onDone = { onCompleteReminder(rem) }
                )
            }
        }

        item {
            SectionHeader(
                title = "${strings.pendingTasks} (${pendingTasks.size})",
                actionLabel = strings.memoryTabTasks,
                onAction = { onOpenMemoryHub(2) }
            )
        }

        if (pendingTasks.isEmpty()) {
            item {
                EmptyTipCard(text = strings.emptyTasks, onClick = onQuickAddTask)
            }
        } else {
            items(pendingTasks.take(4), key = { it.id }) { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTask(task) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = task.title,
                            tint = RoyalBlue
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(task.priority, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "${strings.memoryTabNotes} (${memories.size})",
                actionLabel = strings.smartSearch,
                onAction = { onOpenMemoryHub(0) }
            )
            if (memories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                memories.take(2).forEach { mem ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onOpenMemoryHub(0) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(mem.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (mem.content.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(mem.content, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotStatCard(
    title: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable { onAction() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyTipCard(text: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderPreviewCard(reminder: ReminderItem, onDone: () -> Unit) {
    val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val overdue = reminder.triggerAtMillis < System.currentTimeMillis()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (overdue) CrimsonExpense else VioletGlow
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${fmt.format(Date(reminder.triggerAtMillis))} • ${reminder.recurrence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) CrimsonExpense else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDone, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Mark completed", tint = EmeraldIncome)
            }
        }
    }
}
