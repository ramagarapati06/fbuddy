package com.example.fbuddy.ui.settings
//final push test
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fbuddy.data.db.UserProfileEntity
import com.example.fbuddy.data.repository.UserProfileRepository
import com.example.fbuddy.ui.onboarding.SalaryFrequency
import com.example.fbuddy.ui.onboarding.SavingsPurpose
import com.example.fbuddy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { UserProfileRepository(context) }

    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(SalaryFrequency.MONTHLY) }
    var rent by remember { mutableStateOf("") }
    var emi by remember { mutableStateOf("") }
    var subs by remember { mutableStateOf("") }
    var utils by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf(SavingsPurpose.EMERGENCY_FUND) }
    var budget by remember { mutableStateOf("") }
    var smsTracking by remember { mutableStateOf(true) }
    var ocrScanning by remember { mutableStateOf(true) }
    var notifyOverspend by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repo.getProfile()?.let { p ->
            name = p.fullName; city = p.city
            salary = p.monthlySalary.toString()
            frequency = try { SalaryFrequency.valueOf(p.salaryFrequency) } catch (e: Exception) { SalaryFrequency.MONTHLY }
            rent = p.rent.toString(); emi = p.emiLoans.toString()
            subs = p.subscriptions.toString(); utils = p.utilities.toString()
            goal = p.savingsGoal.toString()
            purpose = try { SavingsPurpose.valueOf(p.savingsPurpose) } catch (e: Exception) { SavingsPurpose.EMERGENCY_FUND }
            budget = p.monthlyBudget.toString()
            smsTracking = p.enableSmsTracking; ocrScanning = p.enableOcrScanning; notifyOverspend = p.notifyOnOverspend
        }
        isLoading = false
    }

    val totalFixed = listOf(rent, emi, subs, utils).sumOf { it.toIntOrNull() ?: 0 }
    val salaryInt = salary.toIntOrNull() ?: 0
    val savingsInt = goal.toIntOrNull() ?: 0
    val budgetInt = budget.toIntOrNull() ?: 0
    val buffer = salaryInt - totalFixed - savingsInt - budgetInt
    val suggested = (salaryInt - totalFixed - savingsInt).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxSize().background(BgSand)) {
        // ── Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            TextButton(onClick = {
                scope.launch {
                    repo.saveProfile(UserProfileEntity(
                        id = 1, fullName = name, city = city,
                        monthlySalary = salaryInt, salaryFrequency = frequency.name,
                        rent = rent.toIntOrNull() ?: 0, emiLoans = emi.toIntOrNull() ?: 0,
                        subscriptions = subs.toIntOrNull() ?: 0, utilities = utils.toIntOrNull() ?: 0,
                        savingsGoal = savingsInt, savingsPurpose = purpose.name,
                        monthlyBudget = budgetInt, categoryBudgetsJson = "{}",
                        enableSmsTracking = smsTracking, enableOcrScanning = ocrScanning,
                        notifyOnOverspend = notifyOverspend, onboardingComplete = true
                    ))
                    showSuccess = true
                    kotlinx.coroutines.delay(1200)
                    onBackClick()
                }
            }) { Text("Save", color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal)
            }
            return
        }

        // ── Scrollable content
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // IDENTITY
            SectionLabel("IDENTITY")
            ProfileTextField(name, { name = it }, "Full Name", "e.g. Arjun Sharma")
            ProfileTextField(city, { city = it }, "City", "e.g. Hyderabad")

            // INCOME
            SectionLabel("INCOME")
            ProfileTextField(salary, { salary = it.filter { c -> c.isDigit() } }, "Monthly Salary (₹)", "e.g. 50000", KeyboardType.Number)

            Text("Salary Frequency", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Ink3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SalaryFrequency.entries.take(2).forEach { freq ->
                    FreqChip(freq.label, frequency == freq, { frequency = freq }, Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SalaryFrequency.entries.drop(2).forEach { freq ->
                    FreqChip(freq.label, frequency == freq, { frequency = freq }, Modifier.weight(1f))
                }
            }

            // FIXED EXPENSES
            SectionLabel("FIXED MONTHLY EXPENSES")
            ProfileTextField(rent, { rent = it.filter { c -> c.isDigit() } }, "Rent / PG (₹)", "e.g. 12000", KeyboardType.Number)
            ProfileTextField(emi, { emi = it.filter { c -> c.isDigit() } }, "Loan EMIs (₹)", "e.g. 5000", KeyboardType.Number)
            ProfileTextField(subs, { subs = it.filter { c -> c.isDigit() } }, "Subscriptions (₹)", "e.g. 1000", KeyboardType.Number)
            ProfileTextField(utils, { utils = it.filter { c -> c.isDigit() } }, "Utilities (₹)", "e.g. 2000", KeyboardType.Number)

            if (totalFixed > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = if (totalFixed < salaryInt) TealLight else RoseLight)) {
                    Text(
                        if (totalFixed < salaryInt) "Total fixed: ₹%,d/mo".format(totalFixed)
                        else "⚠️ Fixed expenses exceed income!",
                        modifier = Modifier.padding(12.dp),
                        color = if (totalFixed < salaryInt) Teal else Rose,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // SAVINGS
            SectionLabel("SAVINGS")
            ProfileTextField(goal, { goal = it.filter { c -> c.isDigit() } }, "Monthly Savings Goal (₹)", "e.g. 10000", KeyboardType.Number)

            Text("Saving For", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Ink3)
            SavingsPurpose.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p -> FreqChip("${p.emoji} ${p.label}", purpose == p, { purpose = p }, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // BUDGET
            SectionLabel("SPENDING BUDGET")
            if (suggested > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = TealLight)) {
                    Text("💡 Suggested: ₹%,d/mo (₹%,d/day)".format(suggested, suggested / 30),
                        modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }
            ProfileTextField(budget, { budget = it.filter { c -> c.isDigit() } }, "Monthly Budget (₹)", "e.g. $suggested", KeyboardType.Number)

            // SUMMARY
            SectionLabel("SUMMARY")
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        Triple("Monthly Salary", "₹%,d".format(salaryInt), Teal),
                        Triple("Fixed Costs", "₹%,d".format(totalFixed), Amber),
                        Triple("Savings Goal", "₹%,d".format(savingsInt), Teal),
                        Triple("Spending Budget", "₹%,d".format(budgetInt), Ink),
                    ).forEach { (label, value, color) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontSize = 12.sp, color = Ink3)
                            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Sand)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monthly Buffer", fontSize = 12.sp, color = Ink3)
                        Text("₹%,d".format(buffer), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (buffer >= 0) Green else Rose)
                    }
                }
            }

            // PREFERENCES
            SectionLabel("PREFERENCES")
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sand2)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PrefToggle("Auto-import bank SMS", smsTracking) { smsTracking = it }
                    PrefToggle("Receipt scanning (OCR)", ocrScanning) { ocrScanning = it }
                    PrefToggle("Overspend notifications", notifyOverspend) { notifyOverspend = it }
                }
            }

            if (showSuccess) {
                Card(colors = CardDefaults.cardColors(containerColor = GreenLight)) {
                    Text("✅ Profile updated successfully!", modifier = Modifier.padding(16.dp),
                        color = Green, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        repo.saveProfile(UserProfileEntity(
                            id = 1, fullName = name, city = city,
                            monthlySalary = salaryInt, salaryFrequency = frequency.name,
                            rent = rent.toIntOrNull() ?: 0, emiLoans = emi.toIntOrNull() ?: 0,
                            subscriptions = subs.toIntOrNull() ?: 0, utilities = utils.toIntOrNull() ?: 0,
                            savingsGoal = savingsInt, savingsPurpose = purpose.name,
                            monthlyBudget = budgetInt, categoryBudgetsJson = "{}",
                            enableSmsTracking = smsTracking, enableOcrScanning = ocrScanning,
                            notifyOnOverspend = notifyOverspend, onboardingComplete = true
                        ))
                        showSuccess = true
                        kotlinx.coroutines.delay(1200)
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text("Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Ink3)
}

@Composable
private fun ProfileTextField(value: String, onValueChange: (String) -> Unit, label: String,
    placeholder: String = "", keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = Ink4) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Sand2,
            focusedLabelColor = Teal, focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite),
        shape = RoundedCornerShape(12.dp))
}

@Composable
private fun FreqChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(if (selected) Teal else CardWhite)
        .border(1.dp, if (selected) Teal else Sand2, RoundedCornerShape(10.dp))
        .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else Ink2, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun PrefToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Teal))
    }
}
