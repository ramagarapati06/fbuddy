package com.example.fbuddy.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.fbuddy.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel()
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
        if (bitmap != null) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result -> viewModel.setExtractedText(result.text) }
                .addOnFailureListener { viewModel.setExtractedText("") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val image = InputImage.fromFilePath(context, it)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result -> viewModel.setExtractedText(result.text) }
                .addOnFailureListener { viewModel.setExtractedText("") }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSand)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scan Receipt", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Viewfinder ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Ink),
            contentAlignment = Alignment.Center
        ) {
            // Corner brackets
            val cornerColor = Teal
            val cornerSize  = 20.dp
            val cornerStroke = 2.dp
            Box(modifier = Modifier.size(160.dp, 120.dp)) {
                // Top-left
                Box(modifier = Modifier.size(cornerSize).align(Alignment.TopStart)
                    .background(Color.Transparent))
                // We use borders via a custom draw — simplified with a central icon instead
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null,
                    tint = Teal, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Point camera at a receipt",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            // Bottom hint
            Text(
                "Keep receipt flat and well-lit for best results",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Action buttons ────────────────────────────────────────────────
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
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink2),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Sand2)
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("📷 Capture", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).height(48.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null,
                    modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Parsed result ─────────────────────────────────────────────────
        state.parsed?.let { parsed ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)
            ) {
                // Header
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

                // Fields
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReceiptField("Merchant", parsed.merchant ?: "Unknown")
                    ReceiptField("Amount",
                        parsed.totalAmount?.let { "₹%.2f".format(it) } ?: "Not detected")
                    ReceiptField("Category",
                        com.example.fbuddy.utils.Categorizer.categorize(
                            parsed.merchant, parsed.rawText).displayName)
                }

                // Save button
                if (parsed.totalAmount != null) {
                    Button(
                        onClick  = { viewModel.saveAsTransaction() },
                        enabled  = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Text(
                            if (state.isSaving) "Saving…" else "Save to Transactions",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }
            }

            state.lastSavedId?.let {
                Spacer(modifier = Modifier.height(10.dp))
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

        // ── Empty state ───────────────────────────────────────────────────
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
                    Spacer(modifier = Modifier.height(6.dp))
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
            Spacer(modifier = Modifier.height(10.dp))
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

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ReceiptField(label: String, value: String) {
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
        Text("Edit", fontSize = 10.sp, color = Teal, fontWeight = FontWeight.Bold)
    }
    Divider(color = Sand, thickness = 1.dp)
}

