package com.example.fbuddy.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val state   by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val image = InputImage.fromBitmap(it, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { result -> viewModel.setExtractedText(result.text) }
                .addOnFailureListener { viewModel.setExtractedText("") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val image = InputImage.fromFilePath(context, it)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { result -> viewModel.setExtractedText(result.text) }
                .addOnFailureListener { viewModel.setExtractedText("") }
        }
    }

    // ── Edit dialogs state ────────────────────────────────────────────────────
    var showMerchantDialog by remember { mutableStateOf(false) }
    var showAmountDialog   by remember { mutableStateOf(false) }
    var merchantInput      by remember { mutableStateOf("") }
    var amountInput        by remember { mutableStateOf("") }

    // Sync local inputs when parsed receipt changes
    LaunchedEffect(state.editedMerchant, state.editedAmount) {
        merchantInput = state.editedMerchant
        amountInput   = state.editedAmount
    }

    // ── Category picker dialog ────────────────────────────────────────────────
    if (state.showCategoryPicker) {
        CategoryPickerDialog(
            currentCategory = state.editedCategory,
            onCategorySelected = viewModel::onCategorySelected,
            onDismiss = viewModel::dismissCategoryPicker
        )
    }

    // ── Merchant edit dialog ──────────────────────────────────────────────────
    if (showMerchantDialog) {
        AlertDialog(
            onDismissRequest = { showMerchantDialog = false },
            title = { Text("Edit Merchant", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = it },
                    label = { Text("Merchant name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onMerchantEdited(merchantInput)
                    showMerchantDialog = false
                }) { Text("Save", color = Teal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showMerchantDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Amount edit dialog ────────────────────────────────────────────────────
    if (showAmountDialog) {
        AlertDialog(
            onDismissRequest = { showAmountDialog = false },
            title = { Text("Edit Amount", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAmountEdited(amountInput)
                    showAmountDialog = false
                }) { Text("Save", color = Teal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAmountDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Main UI ───────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt", color = Ink) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSand)
            )
        },
        containerColor = BgSand
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))

            // Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Ink),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = Teal, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Point camera at a receipt", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Text(
                    "Keep receipt flat and well-lit for best results",
                    fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (hasCameraPermission) cameraLauncher.launch(null)
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink2),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Sand2)
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("📷 Capture", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Parsed result card
            state.parsed?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TealLight)
                            .padding(12.dp, 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✅", fontSize = 14.sp)
                        Text("Receipt Parsed", fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = Teal)
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Merchant
                        EditableReceiptField(
                            label = "Merchant",
                            value = state.editedMerchant.ifBlank { "Unknown" },
                            onEditClick = {
                                merchantInput = state.editedMerchant
                                showMerchantDialog = true
                            }
                        )
                        // Amount
                        EditableReceiptField(
                            label = "Amount",
                            value = state.editedAmount.toDoubleOrNull()
                                ?.let { "₹%.2f".format(it) } ?: "Not detected",
                            onEditClick = {
                                amountInput = state.editedAmount
                                showAmountDialog = true
                            }
                        )
                        // Category
                        EditableReceiptField(
                            label = "Category",
                            value = state.editedCategory.displayName,
                            onEditClick = { viewModel.openCategoryPicker() }
                        )
                    }

                    Button(
                        onClick = { viewModel.saveAsTransaction() },
                        enabled = !state.isSaving &&
                                (state.editedAmount.toDoubleOrNull() != null ||
                                        state.parsed?.totalAmount != null),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Text(
                            if (state.isSaving) "Saving…" else "Save to Transactions",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }

                state.lastSavedId?.let {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenLight)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✅ Saved to your transactions!", fontSize = 12.sp,
                            color = Green, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Empty state
            if (state.parsed == null && state.extractedText.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealMid)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap Capture to photograph a receipt, or pick one from Gallery. " +
                                    "F-Buddy will extract the amount and merchant automatically.",
                            fontSize = 12.sp, color = Ink2, lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            state.error?.let { err ->
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseLight)
                        .padding(12.dp)
                ) {
                    Text("⚠️ $err", fontSize = 12.sp, color = Rose)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Reusable field row with working Edit tap ──────────────────────────────────

@Composable
private fun EditableReceiptField(label: String, value: String, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Ink3, modifier = Modifier.width(72.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink,
            modifier = Modifier.weight(1f))
        Text(
            "Edit",
            fontSize = 10.sp, color = Teal, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onEditClick)   // ← FIXED: clickable
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
    HorizontalDivider(color = Sand, thickness = 1.dp)
}

// ── Category picker dialog ────────────────────────────────────────────────────

@Composable
fun CategoryPickerDialog(
    currentCategory: Category,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Category", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Unknown merchant — choose a category",
                    fontSize = 11.sp, color = Ink3)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Category.entries.forEach { cat ->
                    val selected = cat == currentCategory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) TealLight else Color.Transparent)
                            .clickable { onCategorySelected(cat) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(categoryEmoji(cat), fontSize = 16.sp)
                        Text(cat.displayName, fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Teal else Ink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Teal, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun categoryEmoji(c: Category): String = when (c) {
    Category.FOOD_DINING        -> "🍕"
    Category.TRAVEL_TRANSPORT   -> "🚕"
    Category.SHOPPING           -> "🛍"
    Category.GROCERIES          -> "🛒"
    Category.UTILITIES_BILLS    -> "💡"
    Category.ENTERTAINMENT      -> "🎬"
    Category.HEALTH_MEDICAL     -> "💊"
    Category.EDUCATION          -> "📚"
    Category.FUEL               -> "⛽"
    Category.OTHER              -> "🧾"
}
