package com.example.fbuddy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.ui.theme.*
import com.example.fbuddy.utils.DateUtils

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onChatClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSand)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good morning,", fontSize = 11.sp, color = Ink3)
                Text("F-Buddy 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat icon — opens AI chat
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealLight)
                        .clickable { onChatClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = "Chat",
                        tint = Teal,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TealLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Hero card ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Teal)
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TODAY'S SPENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Tue, 24 Mar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹%.2f".format(state.todayTotal),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    "${state.todayCount} transaction${if (state.todayCount != 1) "s" else ""} today",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        "Yesterday: ₹%.0f".format(state.yesterdayTotal),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        "7-day avg: ₹%.0f/day".format(state.sevenDayAvg),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Budget bar ────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Monthly budget used", fontSize = 11.sp, color = Ink3)
                Text(
                    "₹%.0f / ₹25,000".format(state.monthTotal),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val pct = (state.monthTotal / 25000.0).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color      = if (pct > 0.8f) Rose else Teal,
                trackColor = Sand2,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 7-day mini chart ──────────────────────────────────────────────
        Card(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = CardWhite),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Last 7 days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("Daily spending overview", fontSize = 10.sp, color = Ink3)
                Spacer(modifier = Modifier.height(14.dp))

                val bars   = state.sevenDayBars
                val maxVal = bars.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEachIndexed { i, v ->
                        val fraction = ((v / maxVal) * 0.9 + 0.05).toFloat().coerceAtLeast(0.05f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (i == 6) Teal else TealMid)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("D-6","D-5","D-4","D-3","D-2","D-1","Now").forEach { lbl ->
                        Text(
                            lbl,
                            fontSize   = 8.sp,
                            color      = if (lbl == "Now") Teal else Ink4,
                            fontWeight = if (lbl == "Now") FontWeight.Bold else FontWeight.Normal,
                            modifier   = Modifier.weight(1f),
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Recent transactions ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent transactions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("See all", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Teal)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.recentTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions yet.\nGo to Settings → Re-scan SMS Inbox",
                    fontSize  = 12.sp,
                    color     = Ink3,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = CardWhite),
                border   = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
            ) {
                state.recentTransactions.forEachIndexed { index, txn ->
                    TransactionRow(txn)
                    if (index < state.recentTransactions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color    = Sand,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun TransactionRow(txn: TransactionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(categoryBg(txn.category)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoryEmoji(txn.category), fontSize = 17.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = txn.merchant ?: txn.category.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color    = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text     = "${txn.category.displayName} · ${DateUtils.friendlyTimeLabel(txn.timestamp)}",
                fontSize = 10.sp,
                color    = Ink3
            )
        }
        Text(
            text       = "${if (txn.type == TransactionType.DEBIT) "−" else "+"}₹%.0f".format(txn.amount),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = if (txn.type == TransactionType.DEBIT) Rose else Green
        )
    }
}

private fun categoryEmoji(c: Category): String = when (c) {
    Category.FOOD_DINING      -> "🍕"
    Category.TRAVEL_TRANSPORT -> "🚕"
    Category.SHOPPING         -> "🛍"
    Category.GROCERIES        -> "🛒"
    Category.UTILITIES_BILLS  -> "💡"
    Category.ENTERTAINMENT    -> "🎬"
    Category.HEALTH_MEDICAL   -> "💊"
    Category.EDUCATION        -> "📚"
    Category.FUEL             -> "⛽"
    Category.OTHER            -> "🧾"
}

private fun categoryBg(c: Category): Color = when (c) {
    Category.FOOD_DINING      -> Color(0xFFFDF0E8)
    Category.TRAVEL_TRANSPORT -> Color(0xFFE8F0FD)
    Category.SHOPPING         -> Color(0xFFF5E8FD)
    Category.GROCERIES        -> Color(0xFFE8F5EC)
    Category.HEALTH_MEDICAL   -> Color(0xFFFDE8E8)
    else                      -> Color(0xFFF0EDE8)
}
