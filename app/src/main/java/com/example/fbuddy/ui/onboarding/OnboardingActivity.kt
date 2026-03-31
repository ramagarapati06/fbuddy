package com.example.fbuddy.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fbuddy.MainActivity
import com.example.fbuddy.sms.SmsParserService
import com.example.fbuddy.ui.theme.FBuddyTheme
import com.example.fbuddy.utils.OnboardingPreferences

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (OnboardingPreferences.isOnboardingComplete(this)) {
            navigateToMain()
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            FBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OnboardingContent(
                        modifier = Modifier.padding(innerPadding),
                        onFinished = { smsPermissionGranted ->
                            if (smsPermissionGranted) {
                                SmsParserService.startFullScan(this)
                            }
                            OnboardingPreferences.setOnboardingComplete(this, true)
                            navigateToMain()
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}

@Composable
private fun OnboardingContent(
    modifier: Modifier = Modifier,
    onFinished: (smsPermissionGranted: Boolean) -> Unit
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }

    var smsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted = granted
    }

    LaunchedEffect(Unit) {
        // If permission is already granted, reflect it in state.
        smsPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (page) {
                0 -> OnboardingPageIntro()
                1 -> OnboardingPageSmsPermission(
                    smsPermissionGranted = smsPermissionGranted,
                    onRequestPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                )

                2 -> OnboardingPageReceipt()
            }
        }

        OnboardingNavigationBar(
            page = page,
            isLastPage = page == 2,
            onNext = {
                if (page < 2) page++ else onFinished(smsPermissionGranted)
            },
            onBack = {
                if (page > 0) page--
            }
        )
    }
}

@Composable
private fun OnboardingPageIntro() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FBuddy",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your money, understood.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "We turn your bank SMS alerts into a private, on-device spending dashboard. No account linking, no passwords.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingPageSmsPermission(
    smsPermissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Read SMS for automatic tracking",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "FBuddy reads only your bank and payment SMS alerts on this device to detect transactions. All parsing happens locally on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "We never upload SMS content or transaction data to any server. You can continue without SMS access, but automatic tracking will be disabled.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center
        )

        if (smsPermissionGranted) {
            Text(
                text = "SMS access is enabled.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
            ) {
                Text("Enable SMS Parsing")
            }
        }
    }
}

@Composable
private fun OnboardingPageReceipt() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scan receipts (optional)",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "You can also snap photos of paper receipts to add them to your timeline. This works entirely offline using on-device text recognition.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "You can start scanning later from the home screen. For now, let’s finish setup and see today’s spending.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingNavigationBar(
    page: Int,
    isLastPage: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        if (page > 0) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(bottom = 8.dp))
            ) {
                Text("Back")
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLastPage) "Finish" else "Next")
        }
    }
}

