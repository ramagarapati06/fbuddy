package com.example.fbuddy.ui.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.ui.theme.*

/**
 * A slide-down banner shown when a new "Other" category transaction arrives.
 * The user can tap a category chip to reassign it immediately.
 */
@Composable
fun CategoryPickerBanner(
    transactionId: Long,
    merchantName: String?,
    amount: Double,
    onCategoryPicked: (transactionId: Long, category: Category) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryItems = listOf(
        Category.FOOD_DINING      to ("🍕" to "Food"),
        Category.GROCERIES        to ("🛒" to "Groceries"),
        Category.TRAVEL_TRANSPORT to ("🚕" to "Travel"),
        Category.SHOPPING         to ("🛍" to "Shopping"),
        Category.UTILITIES_BILLS  to ("💡" to "Utilities"),
        Category.ENTERTAINMENT    to ("🎬" to "Entertainment"),
        Category.HEALTH_MEDICAL   to ("💊" to "Health"),
        Category.EDUCATION        to ("📚" to "Education"),
        Category.FUEL             to ("⛽" to "Fuel"),
        Category.OTHER            to ("🧾" to "Other"),
    )

    AnimatedVisibility(
        visible = true,
        enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Ink)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "What was this for?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${merchantName ?: "Unknown"} · −₹%.0f".format(amount),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryItems.forEach { (category, emojiLabel) ->
                        val (emoji, label) = emojiLabel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    onCategoryPicked(transactionId, category)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(emoji, fontSize = 13.sp)
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
