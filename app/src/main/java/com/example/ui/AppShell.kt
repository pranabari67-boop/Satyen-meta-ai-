package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppStringsProvider
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode
import com.example.data.model.DebtItem
import com.example.data.model.MemoryItem
import com.example.notifications.ReminderChannels
import com.example.ui.auth.signOutSafely
import com.example.ui.components.DebtEditorDialog
import com.example.ui.components.DebtPaymentDialog
import com.example.ui.components.MemoryEditorDialog
import com.example.ui.components.ReminderEditorDialog
import com.example.ui.components.TaskEditorDialog
import com.example.ui.components.TransactionEditorDialog
import com.example.ui.screens.AccountingHubScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MemoryHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SmartSearchScreen
import com.example.ui.viewmodel.MainLifeViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

private data class NavTabSpec(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AuthenticatedAppShell(
    viewModel: MainLifeViewModel,
    fallbackName: String,
    fallbackEmail: String?,
    localLanguage: AppLanguage,
    localThemeMode: AppThemeMode
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val memoriesState by viewModel.memoriesState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val accountsState by viewModel.accountsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val debtsState by viewModel.debtsState.collectAsStateWithLifecycle()
    val aiMessagesState by viewModel.aiMessagesState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val supportTicketsState by viewModel.supportTicketsState.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val activeLanguage = profile?.preferredLanguage?.let { AppLanguage.fromCode(it) } ?: localLanguage
    val activeTheme = profile?.themeMode?.let { AppThemeMode.fromName(it) } ?: localThemeMode
    val currency = profile?.currency ?: "INR"
    val strings = AppStringsProvider.forLanguage(activeLanguage)

    val memories = (memoriesState as? UiState.Success)?.data.orEmpty()
    val reminders = (remindersState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val accounts = (accountsState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val debts = (debtsState as? UiState.Success)?.data.orEmpty()
    val aiMessages = (aiMessagesState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()
    val supportTickets = (supportTicketsState as? UiState.Success)?.data.orEmpty()

    var currentTab by remember { mutableIntStateOf(0) }
    var memorySubTab by remember { mutableIntStateOf(0) }
    var accountingSubTab by remember { mutableIntStateOf(0) }
    var showSmartSearch by remember { mutableStateOf(false) }

    var editingMemory by remember { mutableStateOf<MemoryItem?>(null) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var txDialogType by remember { mutableStateOf<String?>(null) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<DebtItem?>(null) }

    var hasNotifPermission by remember {
        mutableStateOf(ReminderChannels.hasNotificationPermission(context))
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showSmartSearch) {
        BackHandler { showSmartSearch = false }
        SmartSearchScreen(
            strings = strings,
            memories = memories,
            reminders = reminders,
            tasks = tasks,
            transactions = transactions,
            debts = debts,
            onBack = { showSmartSearch = false }
        )
        return
    }

    if (currentTab != 0) {
        BackHandler { currentTab = 0 }
    }

    val navTabs = listOf(
        NavTabSpec(strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        NavTabSpec(strings.navAssistant, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
        NavTabSpec(strings.navMemory, Icons.Filled.Psychology, Icons.Outlined.Psychology, "nav_memory"),
        NavTabSpec(strings.navAccounting, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_accounting"),
        NavTabSpec(strings.navProfile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
    )

    val streamError = listOfNotNull(
        (profileState as? UiState.Error)?.message,
        (memoriesState as? UiState.Error)?.message,
        (transactionsState as? UiState.Error)?.message
    ).firstOrNull()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navTabs.forEachIndexed { idx, tab ->
                            val selected = currentTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navTabs.forEachIndexed { idx, tab ->
                            val selected = currentTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (streamError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = streamError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    val initialLoading = profileState is UiState.Loading && accountsState is UiState.Loading
                    if (initialLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (currentTab) {
                            0 -> HomeDashboardScreen(
                                displayName = profile?.displayName ?: fallbackName,
                                strings = strings,
                                language = activeLanguage,
                                currency = currency,
                                hasNotificationPermission = hasNotifPermission,
                                accounts = accounts,
                                transactions = transactions,
                                debts = debts,
                                reminders = reminders,
                                tasks = tasks,
                                memories = memories,
                                onToggleLanguage = {
                                    val next = if (activeLanguage == AppLanguage.EN) AppLanguage.BN else AppLanguage.EN
                                    viewModel.setLanguage(next)
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onOpenAiAssistant = { currentTab = 1 },
                                onOpenMemoryHub = { sub ->
                                    memorySubTab = sub
                                    currentTab = 2
                                },
                                onOpenAccountingHub = { sub ->
                                    accountingSubTab = sub
                                    currentTab = 3
                                },
                                onOpenSmartSearch = { showSmartSearch = true },
                                onQuickAddExpense = { txDialogType = "EXPENSE" },
                                onQuickAddIncome = { txDialogType = "INCOME" },
                                onQuickAddMemory = {
                                    editingMemory = null
                                    showMemoryDialog = true
                                },
                                onQuickAddReminder = { showReminderDialog = true },
                                onQuickAddTask = { showTaskDialog = true },
                                onToggleTask = viewModel::toggleTask,
                                onCompleteReminder = viewModel::markReminderCompleted
                            )

                            1 -> AiAssistantScreen(
                                strings = strings,
                                language = activeLanguage,
                                messages = aiMessages,
                                isBusy = aiBusy,
                                onSendMessage = { text -> viewModel.sendAiMessage(text, activeLanguage) },
                                onConfirmAction = viewModel::confirmAiAction,
                                onCancelAction = viewModel::cancelAiAction
                            )

                            2 -> MemoryHubScreen(
                                strings = strings,
                                language = activeLanguage,
                                selectedTab = memorySubTab,
                                onSelectTab = { memorySubTab = it },
                                memories = memories,
                                reminders = reminders,
                                tasks = tasks,
                                onAddMemory = {
                                    editingMemory = null
                                    showMemoryDialog = true
                                },
                                onEditMemory = {
                                    editingMemory = it
                                    showMemoryDialog = true
                                },
                                onTogglePin = viewModel::toggleMemoryPin,
                                onDeleteMemory = viewModel::deleteMemory,
                                onSaveVoiceOrDocMemory = { title, content, cat, src, uri ->
                                    viewModel.saveMemory(
                                        title = title,
                                        content = content,
                                        category = cat,
                                        tagsRaw = src.lowercase(),
                                        pinned = false,
                                        sourceType = src,
                                        attachmentUri = uri
                                    )
                                },
                                onAddReminder = { showReminderDialog = true },
                                onCompleteReminder = viewModel::markReminderCompleted,
                                onSnoozeReminder = viewModel::snoozeReminder,
                                onDeleteReminder = viewModel::deleteReminder,
                                onAddTask = { showTaskDialog = true },
                                onToggleTask = viewModel::toggleTask,
                                onDeleteTask = viewModel::deleteTask
                            )

                            3 -> AccountingHubScreen(
                                strings = strings,
                                selectedTab = accountingSubTab,
                                onSelectTab = { accountingSubTab = it },
                                currency = currency,
                                monthlyBudgetMinor = profile?.monthlyBudgetMinor ?: 2500000L,
                                accounts = accounts,
                                transactions = transactions,
                                debts = debts,
                                onAddTransaction = { txDialogType = "EXPENSE" },
                                onReverseTransaction = viewModel::reverseTransaction,
                                onCreateAccount = viewModel::createAccount,
                                onAddDebt = { showDebtDialog = true },
                                onPayDebt = { payingDebt = it },
                                onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )

                            else -> ProfileScreen(
                                strings = strings,
                                displayName = profile?.displayName ?: fallbackName,
                                email = profile?.email ?: fallbackEmail,
                                language = activeLanguage,
                                themeMode = activeTheme,
                                currency = currency,
                                monthlyBudgetMinor = profile?.monthlyBudgetMinor ?: 2500000L,
                                biometricLockEnabled = profile?.biometricLockEnabled ?: false,
                                subscriptionTier = profile?.subscriptionTier ?: "FREE",
                                memories = memories,
                                reminders = reminders,
                                tasks = tasks,
                                accounts = accounts,
                                transactions = transactions,
                                debts = debts,
                                auditLogs = auditLogs,
                                supportTickets = supportTickets,
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onSelectCurrency = viewModel::setCurrency,
                                onSaveBudget = viewModel::setMonthlyBudget,
                                onToggleBiometric = viewModel::setBiometricLock,
                                onSubmitSupportTicket = viewModel::submitSupportTicket,
                                onSignOut = { signOutSafely(context, scope) },
                                onDeleteAccount = {
                                    viewModel.deleteAccountAndSignOut {
                                        signOutSafely(context, scope)
                                    }
                                },
                                onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMemoryDialog) {
        MemoryEditorDialog(
            initial = editingMemory,
            onDismiss = { showMemoryDialog = false },
            onSave = { title, content, cat, tags, pinned, src ->
                showMemoryDialog = false
                viewModel.saveMemory(editingMemory?.id, title, content, cat, tags, pinned, src)
            }
        )
    }

    if (showReminderDialog) {
        ReminderEditorDialog(
            onDismiss = { showReminderDialog = false },
            onSave = { title, notes, trigger, rec ->
                showReminderDialog = false
                viewModel.createReminder(title, notes, trigger, rec)
            }
        )
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            onDismiss = { showTaskDialog = false },
            onSave = { title, notes, prio ->
                showTaskDialog = false
                viewModel.createTask(title, notes, prio)
            }
        )
    }

    txDialogType?.let { initialType ->
        TransactionEditorDialog(
            accounts = accounts,
            initialType = initialType,
            defaultCurrency = currency,
            onDismiss = { txDialogType = null },
            onSave = { accId, type, amt, curr, cat, desc ->
                txDialogType = null
                viewModel.recordTransaction(accId, type, amt, curr, cat, desc)
            }
        )
    }

    if (showDebtDialog) {
        DebtEditorDialog(
            defaultCurrency = currency,
            onDismiss = { showDebtDialog = false },
            onSave = { person, dir, amt, curr, notes ->
                showDebtDialog = false
                viewModel.createDebt(person, dir, amt, curr, notes)
            }
        )
    }

    payingDebt?.let { debt ->
        DebtPaymentDialog(
            debt = debt,
            onDismiss = { payingDebt = null },
            onPay = { amt, notes ->
                payingDebt = null
                viewModel.recordDebtPayment(debt, amt, notes)
            }
        )
    }
}
