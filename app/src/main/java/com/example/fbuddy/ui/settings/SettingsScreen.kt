package com.example.fbuddy.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fbuddy.data.repository.UserProfileRepository
import com.example.fbuddy.ui.theme.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onRescan: () -> Unit = {}
) {
    val context = LocalContext.current
    val hasSms  = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    var scanTriggered by remember { mutableStateOf(false) }

    // Load real name and email from profile
    val repo = remember { UserProfileRepository(context) }
    val profile by repo.observeProfile().collectAsState(initial = null)
    val displayName  = profile?.fullName?.ifBlank { "User" } ?: "User"
    val displayCity  = profile?.city?.ifBlank { "" } ?: ""
    val avatarLetter = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Column(modifier = modifier.fillMaxSize().background(BgSand)) {

        // ── Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Profile card — shows real name
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onEditProfile() },
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(TealLight),
                            contentAlignment = Alignment.Center
                        ) { Text(avatarLetter, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Teal) }
                        Column {
                            Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                            if (displayCity.isNotBlank()) {
                                Text(displayCity, fontSize = 11.sp, color = Ink3)
                            }
                            Text("Edit profile", fontSize = 11.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Teal, modifier = Modifier.size(20.dp))
                }
            }

            // ── Financial summary from profile
            if (profile != null && profile!!.monthlySalary > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsGroupLabel("MY FINANCES")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinanceRow("Monthly Salary",  "₹%,d".format(profile!!.monthlySalary),  Teal)
                        FinanceRow("Fixed Costs",     "₹%,d".format(profile!!.rent + profile!!.emiLoans + profile!!.subscriptions + profile!!.utilities), Amber)
                        FinanceRow("Savings Goal",    "₹%,d".format(profile!!.savingsGoal),    Teal)
                        FinanceRow("Spending Budget", "₹%,d".format(profile!!.monthlyBudget),  Ink)
                        HorizontalDivider(color = Sand)
                        val buffer = profile!!.monthlySalary - (profile!!.rent + profile!!.emiLoans + profile!!.subscriptions + profile!!.utilities) - profile!!.savingsGoal - profile!!.monthlyBudget
                        FinanceRow("Monthly Buffer", "₹%,d".format(buffer), if (buffer >= 0) Green else Rose)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── SMS section
            SettingsGroupLabel("SMS TRANSACTIONS")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealMid)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SMS Transactions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        if (hasSms) "SMS permission granted. Tap below to re-scan your inbox."
                        else "Grant SMS permission to import transactions automatically.",
                        fontSize = 12.sp, color = Ink2,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = { scanTriggered = true; onRescan() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (scanTriggered) "Scanning…" else "Re-scan SMS Inbox",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (scanTriggered) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✓ Scan started! Check Transactions tab in a few seconds.", fontSize = 11.sp, color = Teal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Privacy
            SettingsGroupLabel("PRIVACY & SECURITY")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔒 Your data stays on this device", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("No bank credentials are ever stored or shared. F-Buddy only reads SMS — it cannot send messages.",
                        fontSize = 12.sp, color = Ink2, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── About
            SettingsGroupLabel("ABOUT")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("F-Buddy v1.0", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Smart Expense Intelligence\nBuilt with ML Kit · Room · Jetpack Compose · Gemini AI",
                        fontSize = 12.sp, color = Ink3, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinanceRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Ink3)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp,
        color = Ink3, modifier = Modifier.padding(start = 22.dp, bottom = 8.dp))
}
