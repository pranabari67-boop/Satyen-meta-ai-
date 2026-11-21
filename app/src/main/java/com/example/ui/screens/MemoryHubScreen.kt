package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppStrings
import com.example.data.local.AppLanguage
import com.example.data.model.MemoryItem
import com.example.data.model.ReminderItem
import com.example.data.model.TaskItem
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VioletGlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryHubScreen(
    strings: AppStrings,
    language: AppLanguage,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    memories: List<MemoryItem>,
    reminders: List<ReminderItem>,
    tasks: List<TaskItem>,
    onAddMemory: () -> Unit,
    onEditMemory: (MemoryItem) -> Unit,
    onTogglePin: (MemoryItem) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onSaveVoiceOrDocMemory: (title: String, content: String, category: String, sourceType: String, attachmentUri: String?) -> Unit,
    onAddReminder: () -> Unit,
    onCompleteReminder: (ReminderItem) -> Unit,
    onSnoozeReminder: (ReminderItem, Int) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val tabs = listOf(
        strings.memoryTabNotes,
        strings.memoryTabReminders,
        strings.memoryTabTasks,
        strings.memoryTabVault
    )

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onSaveVoiceOrDocMemory(
                    spoken.take(48),
                    spoken,
                    "VOICE",
                    "VOICE",
                    null
                )
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val label = uri.lastPathSegment?.take(48) ?: "Document Attachment"
            onSaveVoiceOrDocMemory(
                "Document: $label",
                "Attached via Android Photo Picker: $uri",
                "DOCUMENT",
                "DOCUMENT",
                uri.toString()
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab < 3) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> onAddMemory()
                            1 -> onAddReminder()
                            2 -> onAddTask()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("memory_hub_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
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
                0 -> MemoriesTabContent(
                    strings = strings,
                    memories = memories,
                    onEdit = onEditMemory,
                    onTogglePin = onTogglePin,
                    onDelete = onDeleteMemory
                )
                1 -> RemindersTabContent(
                    strings = strings,
                    reminders = reminders,
                    onComplete = onCompleteReminder,
                    onSnooze = onSnoozeReminder,
                    onDelete = onDeleteReminder
                )
                2 -> TasksTabContent(
                    strings = strings,
                    tasks = tasks,
                    onToggle = onToggleTask,
                    onDelete = onDeleteTask
                )
                else -> VaultTabContent(
                    memories = memories.filter { it.sourceType == "VOICE" || it.sourceType == "DOCUMENT" },
                    onRecordVoice = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == AppLanguage.BN) "bn-BD" else "en-US")
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (_: Exception) {
                        }
                    },
                    onPickDocumentImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDelete = onDeleteMemory
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoriesTabContent(
    strings: AppStrings,
    memories: List<MemoryItem>,
    onEdit: (MemoryItem) -> Unit,
    onTogglePin: (MemoryItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filtered = remember(memories, query, selectedCategory) {
        memories
            .filter { item ->
                val catOk = selectedCategory == "ALL" || item.category == selectedCategory
                val q = query.trim().lowercase()
                val qOk = q.isEmpty() ||
                    item.title.lowercase().contains(q) ||
                    item.content.lowercase().contains(q) ||
                    item.tags.any { it.lowercase().contains(q) }
                catOk && qOk
            }
            .sortedWith(compareByDescending<MemoryItem> { it.pinned }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search memories, notes, tags...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("memories_search_input")
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "PERSONAL", "WORK", "FINANCE", "HEALTH", "IDEA").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    strings.emptyMemories,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(item) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = RoyalBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { onTogglePin(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (item.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = strings.pinnedLabel,
                                    tint = if (item.pinned) VioletGlow else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDelete(item.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonExpense)
                            }
                        }
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (item.content.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(item.content, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (item.tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item.tags.forEach { tag ->
                                    AssistChip(onClick = { query = tag }, label = { Text("#$tag") })
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
private fun RemindersTabContent(
    strings: AppStrings,
    reminders: List<ReminderItem>,
    onComplete: (ReminderItem) -> Unit,
    onSnooze: (ReminderItem, Int) -> Unit,
    onDelete: (String) -> Unit
) {
    val fmt = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reminders.isEmpty()) {
            item {
                Text(
                    strings.emptyReminders,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(reminders, key = { it.id }) { rem ->
                val isMissed = rem.status != "COMPLETED" && rem.triggerAtMillis < now
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = when {
                                    rem.status == "COMPLETED" -> EmeraldIncome
                                    isMissed -> CrimsonExpense
                                    else -> VioletGlow
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rem.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (rem.status == "COMPLETED") TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = "${fmt.format(Date(rem.triggerAtMillis))} (${rem.timezoneId})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isMissed) CrimsonExpense else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(rem.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonExpense)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Repeat: ${rem.recurrence} • Status: ${if (isMissed) "MISSED" else rem.status} • Delivery: ${rem.deliveryStatus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (rem.status != "COMPLETED") {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onSnooze(rem, 15) }) {
                                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.snooze15m)
                                }
                                OutlinedButton(onClick = { onSnooze(rem, 60) }) {
                                    Text(strings.snooze1h)
                                }
                                Button(onClick = { onComplete(rem) }) {
                                    Text(strings.markDone)
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
private fun TasksTabContent(
    strings: AppStrings,
    tasks: List<TaskItem>,
    onToggle: (TaskItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var showCompleted by remember { mutableStateOf(true) }
    val filtered = remember(tasks, showCompleted) {
        if (showCompleted) tasks else tasks.filter { !it.completed }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showCompleted,
                    onClick = { showCompleted = false },
                    label = { Text("Active (${tasks.count { !it.completed }})") }
                )
                FilterChip(
                    selected = showCompleted,
                    onClick = { showCompleted = true },
                    label = { Text("All (${tasks.size})") }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    strings.emptyTasks,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id }) { task ->
                val priorityColor = when (task.priority) {
                    "HIGH" -> CrimsonExpense
                    "MEDIUM" -> AmberWarn
                    else -> CyanSpark
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(task) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = task.title,
                            tint = if (task.completed) EmeraldIncome else priorityColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else null
                            )
                            Text(
                                text = "Priority: ${task.priority}",
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor
                            )
                        }
                        IconButton(onClick = { onDelete(task.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonExpense)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultTabContent(
    memories: List<MemoryItem>,
    onRecordVoice: () -> Unit,
    onPickDocumentImage: () -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRecordVoice,
                    modifier = Modifier.weight(1f).testTag("vault_voice_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Voice Note")
                }
                OutlinedButton(
                    onClick = onPickDocumentImage,
                    modifier = Modifier.weight(1f).testTag("vault_doc_picker_button")
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Attach Photo")
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                Text(
                    "Record a voice memo or attach a receipt/document photo using Android Photo Picker.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        } else {
            items(memories, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.sourceType == "VOICE") Icons.Default.Mic else Icons.Default.Description,
                            contentDescription = item.sourceType,
                            tint = if (item.sourceType == "VOICE") VioletGlow else CyanSpark
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(item.content, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonExpense)
                        }
                    }
                }
            }
        }
    }
}
