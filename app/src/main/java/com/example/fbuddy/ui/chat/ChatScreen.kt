package com.example.fbuddy.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fbuddy.ui.theme.*

private val SUGGESTIONS = listOf(
    "How much did I spend today?",
    "Show my top merchants",
    "Give me saving tips",
    "Category breakdown",
    "This month's total",
    "How much yesterday?",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 18.sp)
                        }
                        Column {
                            Text("Ask F-Buddy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Teal))
                                Text("Powered by Gemini AI", fontSize = 10.sp, color = Teal, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Ink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White
                )
            )
        },
        containerColor = BgSand
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Divider(color = Sand2, thickness = 1.dp)

            // ── Messages ──────────────────────────────────────────────────────
            LazyColumn(
                state        = listState,
                modifier     = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(state.messages) { msg ->
                    ChatBubble(msg)
                }
                if (state.isThinking) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                    .background(CardWhite)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text("F-Buddy is thinking…", fontSize = 12.sp, color = Ink3, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
                // Suggestion chips (show at bottom if few messages)
                if (state.messages.size <= 1) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try asking:", fontSize = 11.sp, color = Ink3, fontWeight = FontWeight.SemiBold)
                            SUGGESTIONS.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { suggestion ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(CardWhite)
                                                .clickable {
                                                    input = suggestion
                                                    viewModel.send(suggestion)
                                                    input = ""
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(suggestion, fontSize = 11.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Divider(color = Sand2, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value       = input,
                    onValueChange = { input = it },
                    modifier    = Modifier.weight(1f),
                    placeholder = { Text("Ask about your spending…", fontSize = 12.sp, color = Ink4) },
                    singleLine  = true,
                    shape       = RoundedCornerShape(20.dp),
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Teal,
                        unfocusedBorderColor = Sand2,
                        focusedContainerColor   = Sand,
                        unfocusedContainerColor = Sand,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Ink)
                )
                IconButton(
                    onClick  = {
                        val msg = input.trim()
                        if (msg.isNotBlank()) {
                            viewModel.send(msg)
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Teal),
                    enabled  = input.isNotBlank() && !state.isThinking
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send",
                        tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                    else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .background(if (isUser) Teal else CardWhite)
                .then(
                    if (!isUser) Modifier.then(
                        Modifier // border for bot bubble
                    ) else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = if (isUser) Color.White else Ink
            )
        }
    }
}