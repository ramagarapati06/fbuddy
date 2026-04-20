package com.example.fbuddy.ui.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.ui.theme.*
import com.example.fbuddy.utils.DateUtils

@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: TransactionsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(BgSand)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSand)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Text(
                    "Transactions", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = Ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Filter ↓", fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = Teal,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // ── Search bar ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardWhite)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Ink3, modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (state.searchQuery.isEmpty()) {
                        Text("Search merchants, amounts…", fontSize = 13.sp, color = Ink4)
                    }
                    BasicTextField(
                        value         = state.searchQuery,
                        onValueChange = viewModel::onSearchQuery,
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        textStyle     = TextStyle(fontSize = 13.sp, color = Ink)
                    )
                }
            }

            // ── Filter chips (horizontally scrollable) ────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("All","SMS","Receipt","Food","Travel","Shopping",
                       "Health","Groceries","Utilities","Entertainment").forEach { chip ->
                    val selected = state.selectedFilter == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Teal else CardWhite)
                            .clickable { viewModel.onFilter(chip) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            chip, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else Ink2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Transaction list ──────────────────────────────────────
            if (state.groupedTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No transactions found.\nGo to Settings → Re-scan SMS Inbox",
                        fontSize = 13.sp, color = Ink3, textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    state.groupedTransactions.forEach { (dateLabel, txns) ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    dateLabel.uppercase(),
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp, color = Amber
                                )
                                Text(
                                    "₹%.0f".format(
                                        txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                                    ),
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink2
                                )
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                shape  = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
                            ) {
                                txns.forEachIndexed { index, txn ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Icon
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(categoryBg(txn.category)),
                                            contentAlignment = Alignment.Center
                                        ) { Text(categoryEmoji(txn.category), fontSize = 17.sp) }

                                        // Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    txn.merchant ?: txn.category.displayName,
                                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                                    color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                val isSms = txn.source == TransactionSource.SMS
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isSms) Color(0xFFE8F0F5) else Color(0xFFF0EBF5))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        if (isSms) "SMS" else "OCR",
                                                        fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                                        color = if (isSms) Color(0xFF4A7A9B) else Color(0xFF7A4A9B)
                                                    )
                                                }
                                            }
                                            Text(
                                                "${txn.category.displayName} · ${DateUtils.friendlyTimeLabel(txn.timestamp)}",
                                                fontSize = 10.sp, color = Ink3
                                            )
                                        }

                                        // Amount + delete
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${if (txn.type == TransactionType.DEBIT) "−" else "+"}₹%.0f".format(txn.amount),
                                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                                color = if (txn.type == TransactionType.DEBIT) Rose else Green
                                            )
                                            IconButton(
                                                onClick  = { viewModel.delete(txn.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete",
                                                    tint = Ink4, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    if (index < txns.lastIndex) {
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 14.dp),
                                            color     = Sand, thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                }
            }
        }

        // ── Category picker banner — floats at the bottom ─────────────
        AnimatedVisibility(
            visible  = state.pendingCategoryPick != null,
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.pendingCategoryPick?.let { pick ->
                CategoryPickerBanner(
                    transactionId    = pick.transactionId,
                    merchantName     = pick.merchantName,
                    amount           = pick.amount,
                    onCategoryPicked = { id, category ->
                        viewModel.updateCategory(id, category)
                    },
                    onDismiss = {
                        viewModel.dismissBanner(pick.transactionId)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

private fun categoryEmoji(c: Category): String = when (c) {
    Category.FOOD_DINING      -> "🍕"; Category.TRAVEL_TRANSPORT -> "🚕"
    Category.SHOPPING         -> "🛍"; Category.GROCERIES        -> "🛒"
    Category.UTILITIES_BILLS  -> "💡"; Category.ENTERTAINMENT    -> "🎬"
    Category.HEALTH_MEDICAL   -> "💊"; Category.EDUCATION        -> "📚"
    Category.FUEL             -> "⛽"; Category.OTHER            -> "🧾"
}

private fun categoryBg(c: Category): Color = when (c) {
    Category.FOOD_DINING      -> Color(0xFFFDF0E8); Category.TRAVEL_TRANSPORT -> Color(0xFFE8F0FD)
    Category.SHOPPING         -> Color(0xFFF5E8FD); Category.GROCERIES        -> Color(0xFFE8F5EC)
    Category.HEALTH_MEDICAL   -> Color(0xFFFDE8E8); else                      -> Color(0xFFF0EDE8)
}
