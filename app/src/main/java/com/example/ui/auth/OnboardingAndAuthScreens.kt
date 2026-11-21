package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.AppStrings
import com.example.data.local.AppLanguage
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.VioletGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BrandSplashScreen(
    strings: AppStrings,
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1100L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavySurface, Color(0xFF14184E))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.88f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ai_orb),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(listOf(CyanSpark, VioletGlow)), CircleShape)
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "SATYEN META AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.bengaliSlogan,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA78BFA),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class OnboardingPageData(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val description: String,
    val examplePrompt: String
)

@Composable
fun OnboardingScreen(
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onFinishOnboarding: () -> Unit
) {
    val pages = remember(strings) {
        listOf(
            OnboardingPageData(
                icon = Icons.Default.AutoAwesome,
                accent = RoyalBlueBright,
                title = strings.onboardingTitle1,
                description = strings.onboardingDesc1,
                examplePrompt = "\"আজ ৫০০ টাকা বাজার খরচ হয়েছে\""
            ),
            OnboardingPageData(
                icon = Icons.Default.NotificationsActive,
                accent = VioletGlow,
                title = strings.onboardingTitle2,
                description = strings.onboardingDesc2,
                examplePrompt = "\"আগামীকাল সকাল ৮টায় বাজারে যাওয়ার কথা মনে করিয়ে দিও\""
            ),
            OnboardingPageData(
                icon = Icons.Default.AccountBalanceWallet,
                accent = CyanSpark,
                title = strings.onboardingTitle3,
                description = strings.onboardingDesc3,
                examplePrompt = "\"রহিমকে ১০,০০০ টাকা ধার দিলাম\""
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavySurface, NavyCard)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageToggleRow(currentLanguage = currentLanguage, onSelectLanguage = onSelectLanguage)
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(strings.skipToLogin, color = Color(0xFFCBD5E1))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(page.accent.copy(alpha = 0.16f))
                            .border(1.5.dp, page.accent.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = page.accent,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(16.dp)
                        )
                    ) {
                        Text(
                            text = page.examplePrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE0E7FF),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { idx ->
                    val active = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) RoyalBlueBright else Color.White.copy(alpha = 0.25f))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val isLast = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = {
                    if (isLast) {
                        onFinishOnboarding()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (isLast) strings.getStarted else strings.next,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavySurface, Color(0xFF17184A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageToggleRow(currentLanguage = currentLanguage, onSelectLanguage = onSelectLanguage)
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = "SATYEN META AI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = strings.bengaliSlogan,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFC4B5FD),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.Psychology,
                        tint = CyanSpark,
                        text = if (currentLanguage == AppLanguage.BN) {
                            "ব্যক্তিগত মেমোরি, নোট, কাজ ও রিমাইন্ডার"
                        } else {
                            "Private memory, notes, tasks & smart reminders"
                        }
                    )
                    FeatureRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = RoyalBlueBright,
                        text = if (currentLanguage == AppLanguage.BN) {
                            "সঠিক ব্যক্তিগত হিসাব, ধার-দেনা ও PDF/CSV রিপোর্ট"
                        } else {
                            "Exact accounting ledger, debts & PDF/CSV exports"
                        }
                    )
                    FeatureRow(
                        icon = Icons.Default.Lock,
                        tint = Color(0xFF10B981),
                        text = if (currentLanguage == AppLanguage.BN) {
                            "Google সাইন-ইন ও ব্যবহারকারী-ভিত্তিক ডেটা সুরক্ষা"
                        } else {
                            "Isolated per-user cloud security & zero data sharing"
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            GoogleSignInButton(
                label = strings.continueGoogle,
                loadingLabel = strings.signingIn,
                onAuthSuccess = {
                    errorMessage = null
                    onAuthSuccess()
                },
                onAuthError = { msg -> errorMessage = msg }
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = if (currentLanguage == AppLanguage.BN) {
                    "আপনার ব্যক্তিগত ডেটা শুধুমাত্র আপনার অ্যাকাউন্টেই সুরক্ষিত থাকে।"
                } else {
                    "Your memories and finances remain strictly private to your Google account."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun LanguageToggleRow(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = "Language",
            tint = Color(0xFFA78BFA),
            modifier = Modifier.size(18.dp)
        )
        AppLanguage.entries.forEach { lang ->
            val selected = lang == currentLanguage
            FilterChip(
                selected = selected,
                onClick = { onSelectLanguage(lang) },
                label = { Text(lang.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoyalBlue,
                    selectedLabelColor = Color.White,
                    labelColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.testTag("lang_chip_${lang.code}")
            )
        }
    }
}
