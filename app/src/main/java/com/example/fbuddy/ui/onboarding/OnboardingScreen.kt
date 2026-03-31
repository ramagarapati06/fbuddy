package com.example.fbuddy.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fbuddy.ui.theme.*

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
    onComplete: () -> Unit = {}
) {
    val data by viewModel.data.collectAsState()
    val step by viewModel.step.collectAsState()
    val isDone by viewModel.isDone.collectAsState()

    LaunchedEffect(isDone) {
        if (isDone) onComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSand)
    ) {
        // ── Progress bar ─────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(52.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(viewModel.totalSteps) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                i < step  -> Teal
                                i == step -> Teal.copy(alpha = 0.5f)
                                else      -> Sand2
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Step content ─────────────────────────────────────────────────
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn(tween(200))).togetherWith(
                        slideOutHorizontally { -it } + fadeOut(tween(200))
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn(tween(200))).togetherWith(
                        slideOutHorizontally { it } + fadeOut(tween(200))
                    )
                }
            },
            modifier = Modifier.weight(1f),
            label    = "step"
        ) { currentStep ->
            when (currentStep) {
                0 -> Step1Identity(data,
                    onNext = { viewModel.nextStep(it) })
                1 -> Step2Income(data,
                    onNext = { viewModel.nextStep(it) },
                    onBack = { viewModel.prevStep() })
                2 -> Step3FixedExpenses(data,
                    onNext = { viewModel.nextStep(it) },
                    onBack = { viewModel.prevStep() })
                3 -> Step4Savings(data,
                    onNext = { viewModel.nextStep(it) },
                    onBack = { viewModel.prevStep() })
                4 -> Step5Budget(data,
                    onNext = { viewModel.nextStep(it) },
                    onBack = { viewModel.prevStep() })
                5 -> Step6Summary(data,
                    onComplete = { viewModel.complete(it) },
                    onBack     = { viewModel.prevStep() })
            }
        }
    }
}

// ── STEP 1: Identity ──────────────────────────────────────────────────────────
@Composable
private fun Step1Identity(data: OnboardingData, onNext: (OnboardingData) -> Unit) {
    var name by remember { mutableStateOf(data.fullName) }
    var city by remember { mutableStateOf(data.city) }

    StepScaffold(
        emoji   = "👋",
        title   = "Let's get to know you",
        subtitle = "This helps us personalise your experience.",
        onNext  = { onNext(data.copy(fullName = name, city = city)) },
        nextEnabled = name.isNotBlank()
    ) {
        ObLabel("FULL NAME")
        ObTextField(value = name, onValueChange = { name = it },
            placeholder = "e.g. Arjun Sharma")
        Spacer(modifier = Modifier.height(14.dp))
        ObLabel("CITY / TOWN")
        ObTextField(value = city, onValueChange = { city = it },
            placeholder = "e.g. Hyderabad")
    }
}

// ── STEP 2: Income ────────────────────────────────────────────────────────────
@Composable
private fun Step2Income(
    data: OnboardingData,
    onNext: (OnboardingData) -> Unit,
    onBack: () -> Unit
) {
    var salary    by remember { mutableStateOf(data.monthlySalary) }
    var frequency by remember { mutableStateOf(data.salaryFrequency) }

    StepScaffold(
        emoji    = "💰",
        title    = "What's your monthly income?",
        subtitle = "This helps us understand your spending capacity.",
        onNext   = { onNext(data.copy(monthlySalary = salary, salaryFrequency = frequency)) },
        onBack   = onBack,
        nextEnabled = salary.isNotBlank()
    ) {
        ObLabel("TAKE-HOME SALARY")
        ObTextField(value = salary, onValueChange = { salary = it },
            placeholder = "e.g. 50000", prefix = "₹",
            keyboardType = KeyboardType.Number)

        Spacer(modifier = Modifier.height(16.dp))
        ObLabel("SALARY FREQUENCY")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SalaryFrequency.entries.take(2).forEach { freq ->
                ChoiceChip(
                    label    = freq.label,
                    selected = frequency == freq,
                    onClick  = { frequency = freq },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SalaryFrequency.entries.drop(2).forEach { freq ->
                ChoiceChip(
                    label    = freq.label,
                    selected = frequency == freq,
                    onClick  = { frequency = freq },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick salary chips
        Spacer(modifier = Modifier.height(14.dp))
        ObLabel("QUICK SELECT")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("20000","40000","80000","150000").forEach { amt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (salary == amt) Teal else CardWhite)
                        .border(1.dp, if (salary == amt) Teal else Sand2, RoundedCornerShape(10.dp))
                        .clickable { salary = amt }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "₹${amt.toLong() / 1000}K",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (salary == amt) Color.White else Ink2
                    )
                }
            }
        }
    }
}

// ── STEP 3: Fixed Expenses ────────────────────────────────────────────────────
@Composable
private fun Step3FixedExpenses(
    data: OnboardingData,
    onNext: (OnboardingData) -> Unit,
    onBack: () -> Unit
) {
    var rent  by remember { mutableStateOf(data.rent) }
    var emi   by remember { mutableStateOf(data.emiLoans) }
    var subs  by remember { mutableStateOf(data.subscriptions) }
    var utils by remember { mutableStateOf(data.utilities) }

    val total = listOf(rent, emi, subs, utils)
        .sumOf { it.toIntOrNull() ?: 0 }

    StepScaffold(
        emoji    = "🏠",
        title    = "Fixed monthly expenses",
        subtitle = "Things that don't change month to month.",
        onNext   = { onNext(data.copy(rent = rent, emiLoans = emi, subscriptions = subs, utilities = utils)) },
        onBack   = onBack,
        nextEnabled = true
    ) {
        ObLabel("RENT / PG")
        ObTextField(rent, { rent = it }, "e.g. 12000", "₹", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        ObLabel("LOAN EMIs")
        ObTextField(emi, { emi = it }, "e.g. 5000", "₹", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        ObLabel("SUBSCRIPTIONS (Netflix, Spotify…)")
        ObTextField(subs, { subs = it }, "e.g. 1000", "₹", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        ObLabel("UTILITIES (electricity, water, internet)")
        ObTextField(utils, { utils = it }, "e.g. 2000", "₹", keyboardType = KeyboardType.Number)

        if (total > 0) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealLight)
                    .padding(12.dp)
            ) {
                Text(
                    "Total fixed: ₹%,d/mo".format(total),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Teal
                )
            }
        }
    }
}

// ── STEP 4: Savings Goal ──────────────────────────────────────────────────────
@Composable
private fun Step4Savings(
    data: OnboardingData,
    onNext: (OnboardingData) -> Unit,
    onBack: () -> Unit
) {
    var goal    by remember { mutableStateOf(data.savingsGoal) }
    var purpose by remember { mutableStateOf(data.savingsPurpose) }

    val feasibility = checkFeasibility(data.copy(savingsGoal = goal))

    StepScaffold(
        emoji    = "🎯",
        title    = "How much do you want to save?",
        subtitle = "We'll check if this is realistic based on your income.",
        onNext   = { onNext(data.copy(savingsGoal = goal, savingsPurpose = purpose)) },
        onBack   = onBack,
        nextEnabled = goal.isNotBlank()
    ) {
        ObLabel("MONTHLY SAVINGS GOAL")
        ObTextField(goal, { goal = it }, "e.g. 10000", "₹", keyboardType = KeyboardType.Number)

        // Feasibility banner
        feasibility?.let { result ->
            Spacer(Modifier.height(10.dp))
            val (bg, textColor, icon, msg) = when (result) {
                is FeasibilityResult.OK   -> listOf(GreenLight, Green,  "✅", result.message)
                is FeasibilityResult.Warn -> listOf(AmberLight, Amber,  "🟡", result.message)
                is FeasibilityResult.Bad  -> listOf(RoseLight,  Rose,   "⚠️", result.message)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg as Color)
                    .padding(12.dp)
            ) {
                Column {
                    Text("$icon  ${msg as String}", fontSize = 12.sp, color = textColor as Color,
                        fontWeight = FontWeight.SemiBold)
                    if (result is FeasibilityResult.Warn || result is FeasibilityResult.Bad) {
                        val suggestion = if (result is FeasibilityResult.Warn) result.suggestion
                                         else (result as FeasibilityResult.Bad).suggestion
                        Text(suggestion, fontSize = 11.sp, color = textColor,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ObLabel("SAVING FOR")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SavingsPurpose.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        ChoiceChip(
                            label    = "${p.emoji} ${p.label}",
                            selected = purpose == p,
                            onClick  = { purpose = p },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── STEP 5: Budget ────────────────────────────────────────────────────────────
@Composable
private fun Step5Budget(
    data: OnboardingData,
    onNext: (OnboardingData) -> Unit,
    onBack: () -> Unit
) {
    val salary  = data.monthlySalary.toIntOrNull() ?: 0
    val fixed   = listOf(data.rent, data.emiLoans, data.subscriptions, data.utilities)
        .sumOf { it.toIntOrNull() ?: 0 }
    val savings = data.savingsGoal.toIntOrNull() ?: 0
    val suggested = (salary - fixed - savings).coerceAtLeast(0)

    var budget by remember { mutableStateOf(data.monthlyBudget.ifBlank { suggested.toString() }) }
    var catBudgets by remember { mutableStateOf(data.categoryBudgets) }

    StepScaffold(
        emoji    = "📊",
        title    = "Set your spending budget",
        subtitle = "Your daily limit will be calculated automatically.",
        onNext   = { onNext(data.copy(monthlyBudget = budget, categoryBudgets = catBudgets)) },
        onBack   = onBack,
        nextEnabled = budget.isNotBlank()
    ) {
        if (suggested > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealLight)
                    .padding(12.dp)
            ) {
                Column {
                    Text("💡 Smart suggestion", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = Teal)
                    Text(
                        "Based on your salary minus fixed costs and savings target: ₹%,d/mo (₹%,d/day)"
                            .format(suggested, suggested / 30),
                        fontSize = 12.sp, color = Ink2,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        ObLabel("MONTHLY SPENDING BUDGET")
        ObTextField(budget, { budget = it }, "e.g. $suggested", "₹",
            keyboardType = KeyboardType.Number)

        Spacer(Modifier.height(16.dp))
        ObLabel("CATEGORY BUDGETS (optional)")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpendCategory.entries.forEach { cat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${cat.emoji} ${cat.label}", fontSize = 12.sp,
                        color = Ink, modifier = Modifier.weight(1f))
                    ObTextField(
                        value         = catBudgets[cat] ?: "",
                        onValueChange = { v -> catBudgets = catBudgets + (cat to v) },
                        placeholder   = "Optional",
                        prefix        = "₹",
                        keyboardType  = KeyboardType.Number,
                        modifier      = Modifier.width(120.dp)
                    )
                }
            }
        }
    }
}

// ── STEP 6: Summary & Preferences ────────────────────────────────────────────
@Composable
private fun Step6Summary(
    data: OnboardingData,
    onComplete: (OnboardingData) -> Unit,
    onBack: () -> Unit
) {
    var sms    by remember { mutableStateOf(data.enableSmsTracking) }
    var ocr    by remember { mutableStateOf(data.enableOcrScanning) }
    var notify by remember { mutableStateOf(data.notifyOnOverspend) }

    val salary  = data.monthlySalary.toIntOrNull() ?: 0
    val fixed   = listOf(data.rent, data.emiLoans, data.subscriptions, data.utilities)
        .sumOf { it.toIntOrNull() ?: 0 }
    val savings = data.savingsGoal.toIntOrNull() ?: 0
    val budget  = data.monthlyBudget.toIntOrNull() ?: 0
    val buffer  = salary - fixed - savings - budget

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSand)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(TealLight)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) { Text("✅", fontSize = 22.sp) }

        Spacer(Modifier.height(12.dp))
        Text("Your financial profile", fontSize = 22.sp,
            fontWeight = FontWeight.Bold, color = Ink,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Here's what we've set up. You can always edit this in Profile.",
            fontSize = 12.sp, color = Ink3, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                .align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(16.dp))

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, Sand2)
        ) {
            Column(modifier = Modifier.padding(0.dp)) {
                Box(modifier = Modifier.fillMaxWidth().background(TealLight).padding(12.dp, 10.dp)) {
                    Text("YOUR PLAN", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp, color = Teal)
                }
                listOf(
                    Triple("Monthly salary",  "₹%,d".format(salary),  Teal),
                    Triple("Fixed costs",     "₹%,d".format(fixed),   Amber),
                    Triple("Savings goal",    "₹%,d".format(savings), Teal),
                    Triple("Spending budget", "₹%,d".format(budget),  Ink),
                    Triple("Monthly buffer",  "₹%,d".format(buffer),  if (buffer >= 0) Green else Rose),
                ).forEach { (label, value, color) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 12.sp, color = Ink3)
                        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = color as Color)
                    }
                    HorizontalDivider(color = Sand, thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Preferences
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, Sand2)
        ) {
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Preferences", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = Ink)
                PrefToggle("Auto-import bank SMS", sms,  { sms = it })
                PrefToggle("Receipt scanning (OCR)", ocr, { ocr = it })
                PrefToggle("Overspend notifications", notify, { notify = it })
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                onComplete(data.copy(
                    enableSmsTracking = sms,
                    enableOcrScanning = ocr,
                    notifyOnOverspend = notify
                ))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal)
        ) {
            Text("Start tracking →", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(
            onClick  = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("← Back", color = Ink3)
        }
    }
}

// ── SHARED COMPONENTS ─────────────────────────────────────────────────────────

@Composable
private fun StepScaffold(
    emoji: String,
    title: String,
    subtitle: String,
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(TealLight),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }

        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = Ink, lineHeight = 28.sp)
        Text(subtitle, fontSize = 12.sp, color = Ink3,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

        content()

        Spacer(Modifier.height(24.dp))
        Button(
            onClick  = onNext,
            enabled  = nextEnabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Teal)
        ) {
            Text("Continue →", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("← Back", color = Ink3)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ObLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp, color = Ink3,
        modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun ObTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    prefix: String = "",
    suffix: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier,
        placeholder   = { Text(placeholder, fontSize = 13.sp, color = Ink4) },
        prefix        = if (prefix.isNotEmpty()) ({ Text(prefix, color = Ink2, fontWeight = FontWeight.SemiBold) }) else null,
        suffix        = if (suffix.isNotEmpty()) ({ Text(suffix, color = Ink3) }) else null,
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = Teal,
            unfocusedBorderColor  = Sand2,
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite,
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Medium
        )
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Teal else CardWhite)
            .border(1.dp, if (selected) Teal else Sand2, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = if (selected) Color.White else Ink2,
            textAlign  = TextAlign.Center,
            maxLines   = 2
        )
    }
}

@Composable
private fun PrefToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(checkedThumbColor = Color.White,
                checkedTrackColor = Teal)
        )
    }
}
