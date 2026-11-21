package com.example

import android.app.Activity
import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.firebase.configureAppCheck
import com.example.core.i18n.AppStringsProvider
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode
import com.example.data.local.LocalAppPreferences
import com.example.data.local.SettingsDataStore
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.LifeRepository
import com.example.notifications.ReminderChannels
import com.example.notifications.ReminderScheduler
import com.example.ui.AuthenticatedAppShell
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.BrandSplashScreen
import com.example.ui.auth.OnboardingScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SatyenMetaAiTheme
import com.example.ui.viewmodel.MainLifeViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        ReminderChannels.ensureChannel(this)
        enableEdgeToEdge()

        setContent {
            SatyenMetaAiRoot()
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun SatyenMetaAiRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember(context) { SettingsDataStore(context.applicationContext) }
    val localPrefs by settingsStore.preferences.collectAsStateWithLifecycle(initialValue = LocalAppPreferences())
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var splashDone by remember { mutableStateOf(false) }
    var unlockedSession by remember { mutableStateOf(false) }

    val strings = AppStringsProvider.forLanguage(localPrefs.language)

    LaunchedEffect(Unit) {
        val cm = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = cm,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    SatyenMetaAiTheme(
        themeMode = localPrefs.themeMode,
        language = localPrefs.language
    ) {
        if (!splashDone) {
            BrandSplashScreen(
                strings = strings,
                onFinished = { splashDone = true }
            )
            return@SatyenMetaAiTheme
        }

        val user = currentUser
        if (user == null) {
            if (!localPrefs.onboardingSeen) {
                OnboardingScreen(
                    strings = strings,
                    currentLanguage = localPrefs.language,
                    onSelectLanguage = { lang -> scope.launch { settingsStore.setLanguage(lang) } },
                    onFinishOnboarding = { scope.launch { settingsStore.setOnboardingSeen(true) } }
                )
            } else {
                AuthScreen(
                    strings = strings,
                    currentLanguage = localPrefs.language,
                    onSelectLanguage = { lang -> scope.launch { settingsStore.setLanguage(lang) } },
                    onAuthSuccess = {}
                )
            }
        } else {
            val uid = user.uid
            key(uid) {
                val viewModel: MainLifeViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val dbId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(dbId)
                            MainLifeViewModel(
                                currentUserId = uid,
                                displayName = user.displayName ?: "Satyen User",
                                email = user.email,
                                repository = LifeRepository(db),
                                aiRepository = AiAssistantRepository(),
                                settingsStore = SettingsDataStore(app),
                                reminderScheduler = ReminderScheduler(app)
                            )
                        }
                    }
                )

                val profileState by viewModel.profileState.collectAsStateWithLifecycle()
                val profile = (profileState as? UiState.Success)?.data
                val effectiveLang = profile?.preferredLanguage?.let { AppLanguage.fromCode(it) } ?: localPrefs.language
                val effectiveTheme = profile?.themeMode?.let { AppThemeMode.fromName(it) } ?: localPrefs.themeMode
                val lockRequired = (profile?.biometricLockEnabled ?: localPrefs.biometricLockEnabled) && !unlockedSession

                SatyenMetaAiTheme(
                    themeMode = effectiveTheme,
                    language = effectiveLang
                ) {
                    if (lockRequired) {
                        AppLockGate(
                            onUnlocked = { unlockedSession = true }
                        )
                    } else {
                        AuthenticatedAppShell(
                            viewModel = viewModel,
                            fallbackName = user.displayName ?: "Satyen User",
                            fallbackEmail = user.email,
                            localLanguage = effectiveLang,
                            localThemeMode = effectiveTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val keyguard = remember(context) { context.getSystemService(KeyguardManager::class.java) }
    val unlockLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            onUnlocked()
        }
    }

    LaunchedEffect(Unit) {
        if (keyguard?.isDeviceSecure != true) {
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Satyen Meta AI Locked",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Button(onClick = {
                @Suppress("DEPRECATION")
                val intent = keyguard?.createConfirmDeviceCredentialIntent(
                    "Unlock Satyen Meta AI",
                    "Verify your identity to access your personal assistant"
                )
                if (intent != null) {
                    unlockLauncher.launch(intent)
                } else {
                    onUnlocked()
                }
            }) {
                Text("Unlock")
            }
        }
    }
}
