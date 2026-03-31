package com.example.fbuddy.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fbuddy.ui.theme.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onRescan: () -> Unit = {}
) {
    val context = LocalContext.current
    val hasSms  = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    var scanTriggered by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSand)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text("Profile", fontSize = 22.sp,
            fontWeight = FontWeight.Bold, color = Ink,
            modifier = Modifier.padding(horizontal = 20.dp))

        Spacer(modifier = Modifier.height(14.dp))

        // ── Profile card ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(TealLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
                Column {
                    Text("Arjun", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("arjun@email.com", fontSize = 11.sp, color = Ink3)
                    Text("Edit profile", fontSize = 11.sp, color = Teal,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── SMS section ───────────────────────────────────────────────────
        SettingsGroupLabel("SMS TRANSACTIONS")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TealLight),
            border = androidx.compose.foundation.BorderStroke(1.dp, TealMid)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SMS Transactions", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    if (hasSms) "SMS permission granted. Tap below to re-scan your inbox."
                    else        "Grant SMS permission to import transactions automatically.",
                    fontSize = 12.sp, color = Ink2,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                Button(
                    onClick = {
                        scanTriggered = true
                        onRescan()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (scanTriggered) "Scanning…" else "Re-scan SMS Inbox",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
                if (scanTriggered) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ Scan started! Check Transactions tab in a few seconds.",
                        fontSize = 11.sp, color = Teal)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Privacy ───────────────────────────────────────────────────────
        SettingsGroupLabel("PRIVACY & SECURITY")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔒 Your data stays on this device", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    "No bank credentials are ever stored or shared. F-Buddy only reads SMS — it cannot send messages.",
                    fontSize = 12.sp, color = Ink2,
                    modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── About ─────────────────────────────────────────────────────────
        SettingsGroupLabel("ABOUT")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("F-Buddy v1.0", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = Ink)
                Text("Smart Expense Intelligence\nBuilt with ML Kit · Room · Jetpack Compose · Gemini AI",
                    fontSize = 12.sp, color = Ink3,
                    modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.7.sp,
        color = Ink3,
        modifier = Modifier.padding(start = 22.dp, bottom = 8.dp)
    )
}
