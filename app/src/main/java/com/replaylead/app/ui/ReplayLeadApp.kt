package com.replaylead.app.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replaylead.app.BuildConfig
import com.replaylead.app.domain.CoachingReport
import com.replaylead.app.domain.ConversationTurn
import com.replaylead.app.domain.Scenario
import com.replaylead.app.domain.Speaker
import com.replaylead.app.domain.scenarios
import com.replaylead.app.ui.theme.Coral
import com.replaylead.app.ui.theme.DeepInk
import com.replaylead.app.ui.theme.Mint
import com.replaylead.app.ui.theme.Paper
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.launch

@Composable
fun ReplayLeadApp(viewModel: ReplayLeadViewModel = viewModel()) {
    val state = viewModel.state
    Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
        AnimatedContent(targetState = state.stage, label = "screen") { stage ->
            when (stage) {
                AppStage.HOME -> HomeScreen(
                    state = state,
                    onPractice = { viewModel.go(AppStage.SCENARIOS) },
                    onPaywall = { viewModel.go(AppStage.PAYWALL) },
                )
                AppStage.SCENARIOS -> ScenarioScreen(
                    selected = state.selectedScenario,
                    confidence = state.confidenceBefore,
                    onBack = { viewModel.go(AppStage.HOME) },
                    onSelect = viewModel::chooseScenario,
                    onConfidence = viewModel::setConfidenceBefore,
                    onStart = viewModel::startPractice,
                )
                AppStage.PRACTICE -> PracticeScreen(
                    scenario = state.selectedScenario,
                    turns = state.turns,
                    branch = state.branch,
                    rewindNotice = state.rewindNotice,
                    engineLabel = state.engineLabel,
                    isSending = state.isSending,
                    isGeneratingReport = state.isGeneratingReport,
                    serviceNotice = state.serviceNotice,
                    onBack = { viewModel.go(AppStage.SCENARIOS) },
                    onSend = viewModel::send,
                    onRewind = viewModel::rewind,
                    onFinish = viewModel::finishPractice,
                )
                AppStage.REPORT -> ReportScreen(
                    report = requireNotNull(state.report),
                    before = state.confidenceBefore,
                    after = state.confidenceAfter,
                    engineLabel = state.engineLabel,
                    serviceNotice = state.serviceNotice,
                    onAfter = viewModel::setConfidenceAfter,
                    onAgain = viewModel::startPractice,
                    onHome = viewModel::reset,
                )
                AppStage.PAYWALL -> PaywallScreen(onBack = { viewModel.go(AppStage.HOME) })
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: ReplayLeadUiState,
    onPractice: () -> Unit,
    onPaywall: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(DeepInk),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Coral)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ReplayLead", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Practice before it matters.", color = DeepInk.copy(alpha = .62f), fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Mint.copy(alpha = .17f),
                ) {
                    Text("DEMO", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = DeepInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepInk),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("The real conversation shouldn't be your first practice.", color = Color.White, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Text("Rehearse the hard moment, rewind your response, and leave with language you can actually use.", color = Color.White.copy(alpha = .74f), lineHeight = 21.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onPractice,
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Text("Practice a conversation", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
        item {
            Text("YOUR PRACTICE", fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 12.sp, color = DeepInk.copy(alpha = .55f))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("3", "scenarios", Modifier.weight(1f))
                MetricCard("4", "coaching skills", Modifier.weight(1f), accent = Mint)
            }
        }
        item {
            Card(
                onClick = onPaywall,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Coral)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Unlock deeper coaching", fontWeight = FontWeight.Bold)
                        Text("Unlimited practice, history, and branch comparison", color = DeepInk.copy(alpha = .58f), fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
        item {
            Text("Selected next: ${state.selectedScenario.title}", color = DeepInk.copy(alpha = .48f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier, accent: Color = Coral) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 27.sp)
            Text(label, color = DeepInk.copy(alpha = .58f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ScenarioScreen(
    selected: Scenario,
    confidence: Int,
    onBack: () -> Unit,
    onSelect: (Scenario) -> Unit,
    onConfidence: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Scaffold(containerColor = Paper, topBar = { SimpleTopBar("Choose a scenario", onBack) }) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("Pick the conversation you want to feel ready for.", color = DeepInk.copy(alpha = .62f)) }
            items(scenarios) { scenario ->
                ScenarioCard(scenario, selected.id == scenario.id) { onSelect(scenario) }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("How prepared do you feel now?", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                ConfidencePicker(confidence, onConfidence)
            }
            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                    contentPadding = PaddingValues(16.dp),
                ) { Text("Start rehearsal", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ScenarioCard(scenario: Scenario, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) Coral else Color.Transparent
    Card(
        modifier = Modifier.fillMaxWidth().border(2.dp, border, RoundedCornerShape(20.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scenario.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Surface(shape = RoundedCornerShape(99.dp), color = if (selected) Coral.copy(alpha = .14f) else Paper) {
                    Text(scenario.difficulty, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepInk)
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(scenario.context, color = DeepInk.copy(alpha = .65f), lineHeight = 20.sp)
            if (selected) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DeepInk.copy(alpha = .08f))
                Spacer(Modifier.height(12.dp))
                Text("YOUR GOAL", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Coral, letterSpacing = 1.sp)
                Text(scenario.goal, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ConfidencePicker(value: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { number ->
            Surface(
                modifier = Modifier.weight(1f).clickable { onValue(number) },
                shape = RoundedCornerShape(14.dp),
                color = if (number == value) DeepInk else Color.White,
            ) {
                Text(
                    number.toString(),
                    modifier = Modifier.padding(vertical = 13.dp),
                    textAlign = TextAlign.Center,
                    color = if (number == value) Color.White else DeepInk,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeScreen(
    scenario: Scenario,
    turns: List<ConversationTurn>,
    branch: Int,
    rewindNotice: String?,
    engineLabel: String,
    isSending: Boolean,
    isGeneratingReport: Boolean,
    serviceNotice: String?,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onRewind: (Long) -> Unit,
    onFinish: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    LaunchedEffect(turns.size, isSending, rewindNotice, serviceNotice) {
        val lastItem = listState.layoutInfo.totalItemsCount - 1
        if (lastItem >= 0) listState.animateScrollToItem(lastItem)
    }
    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                SimpleTopBar(scenario.counterpart, onBack, trailing = "Branch ${branch + 1}")
                Text(engineLabel, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center, color = DeepInk.copy(alpha = .45f), fontSize = 11.sp)
            }
        },
        bottomBar = {
            Column(Modifier.background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        enabled = !isSending && !isGeneratingReport,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Say what you would really say…") },
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Paper,
                            unfocusedContainerColor = Paper,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            onSend(draft); draft = ""; keyboard?.hide()
                        }),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { onSend(draft); draft = ""; keyboard?.hide() },
                        enabled = !isSending && !isGeneratingReport && draft.isNotBlank(),
                        modifier = Modifier.size(48.dp).background(Coral, CircleShape),
                    ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White) }
                }
                if (turns.count { it.speaker == Speaker.USER } >= 2) {
                    if (isGeneratingReport) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Coral)
                            Spacer(Modifier.width(8.dp))
                            Text("Generating coaching…", color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else if (!isSending) {
                        TextButtonLike("Finish and get coaching", onFinish)
                    }
                }
            }
        },
    ) { insets ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(color = DeepInk.copy(alpha = .06f), shape = RoundedCornerShape(16.dp)) {
                    Text(scenario.goal, modifier = Modifier.padding(14.dp), fontSize = 13.sp, color = DeepInk.copy(alpha = .72f))
                }
            }
            rewindNotice?.let { notice ->
                item {
                    Surface(color = Mint.copy(alpha = .16f), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = Mint)
                            Spacer(Modifier.width(8.dp))
                            Text(notice, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
            serviceNotice?.let { notice ->
                item {
                    Surface(color = Coral.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) {
                        Text(notice, modifier = Modifier.padding(12.dp), color = DeepInk.copy(alpha = .76f), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
            items(turns, key = { it.id }) { turn ->
                MessageBubble(turn = turn, canRewind = turn.speaker == Speaker.USER && !isSending && !isGeneratingReport, onRewind = { onRewind(turn.id) })
            }
            if (isSending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Coral)
                        Spacer(Modifier.width(9.dp))
                        Text("Counterpart is responding…", color = DeepInk.copy(alpha = .55f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(turn: ConversationTurn, canRewind: Boolean, onRewind: () -> Unit) {
    val user = turn.speaker == Speaker.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (user) Alignment.End else Alignment.Start, modifier = Modifier.fillMaxWidth(.86f)) {
            Surface(
                color = if (user) DeepInk else Color.White,
                contentColor = if (user) Color.White else DeepInk,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (user) 20.dp else 5.dp,
                    bottomEnd = if (user) 5.dp else 20.dp,
                ),
                shadowElevation = if (user) 0.dp else 1.dp,
            ) { Text(turn.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp), lineHeight = 21.sp) }
            if (canRewind) {
                Row(
                    modifier = Modifier.clickable(onClick = onRewind).padding(top = 5.dp, start = 6.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Coral)
                    Spacer(Modifier.width(4.dp))
                    Text("Rewind here", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportScreen(
    report: CoachingReport,
    before: Int,
    after: Int,
    engineLabel: String,
    serviceNotice: String?,
    onAfter: (Int) -> Unit,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(22.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("COACHING REPORT", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text("You kept the conversation moving.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
            Text(engineLabel, color = DeepInk.copy(alpha = .48f), fontSize = 11.sp)
        }
        serviceNotice?.let { notice ->
            item {
                Surface(color = Coral.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) {
                    Text(notice, modifier = Modifier.padding(12.dp), color = DeepInk.copy(alpha = .76f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = DeepInk), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    ScoreRow("Clarity", report.clarity, Coral)
                    ScoreRow("Empathy", report.empathy, Mint)
                    ScoreRow("Assertiveness", report.assertiveness, Color(0xFFFFC857))
                    ScoreRow("Actionability", report.actionability, Color(0xFF9AA8FF))
                }
            }
        }
        item { InsightCard("STRONGEST MOMENT", report.strength, Mint) }
        item { InsightCard("TRY NEXT", report.improvement, Coral) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("A stronger way to say it", fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("“${report.suggestedResponse}”", color = DeepInk.copy(alpha = .72f), lineHeight = 21.sp)
                }
            }
        }
        item {
            Text("How prepared do you feel now?", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            ConfidencePicker(after, onAfter)
            Spacer(Modifier.height(10.dp))
            Text("Confidence $before → $after", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Mint, fontWeight = FontWeight.Black)
        }
        item {
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Coral), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Practice this again", fontWeight = FontWeight.Bold)
            }
            TextButtonLike("Back to home", onHome)
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(110.dp), color = Color.White.copy(alpha = .72f), fontSize = 13.sp)
        Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f))) {
            val progress by animateFloatAsState(value / 100f, label = label)
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(color))
        }
        Spacer(Modifier.width(10.dp))
        Text(value.toString(), color = Color.White, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InsightCard(label: String, text: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Text(text, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaywallScreen(onBack: () -> Unit) {
    val configured = BuildConfig.REVENUECAT_API_KEY.isNotBlank()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var selectedPackage by remember { mutableStateOf<Package?>(null) }
    var loading by remember { mutableStateOf(configured) }
    var purchaseInProgress by remember { mutableStateOf(false) }
    var proIsActive by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun hasPro(customerInfo: com.revenuecat.purchases.CustomerInfo): Boolean =
        customerInfo.entitlements.active.containsKey(BuildConfig.REVENUECAT_ENTITLEMENT_ID)

    LaunchedEffect(configured) {
        if (!configured) return@LaunchedEffect
        runCatching {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            selectedPackage = offerings.current?.annual
                ?: offerings.current?.availablePackages?.firstOrNull()
            if (selectedPackage == null) {
                statusMessage = "No purchasable package is available yet. Check the RevenueCat offering."
            }
        }.onFailure {
            statusMessage = "Pricing could not be loaded. Check your connection and try again."
        }
        loading = false
    }

    val price = selectedPackage?.product?.price?.formatted
    val purchaseEnabled = configured && selectedPackage != null && activity != null && !loading && !purchaseInProgress && !proIsActive
    Scaffold(containerColor = DeepInk, topBar = { SimpleTopBar("ReplayLead Pro", onBack, dark = true) }) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Coral, modifier = Modifier.size(40.dp))
            Text("Turn practice into a leadership habit.", color = Color.White, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
            listOf("Unlimited rehearsals", "Deep coaching and branch comparison", "Practice history and confidence trends").forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = DeepInk, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(it, color = Color.White.copy(alpha = .84f))
                }
            }
            Spacer(Modifier.weight(1f))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (proIsActive) "ReplayLead Pro" else "Annual plan", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(
                                if (proIsActive) "Active on this account" else "Cancel anytime",
                                color = Mint,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(price ?: "—", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val packageToBuy = selectedPackage ?: return@Button
                            val purchaseActivity = activity ?: return@Button
                            scope.launch {
                                purchaseInProgress = true
                                statusMessage = null
                                try {
                                    val result = Purchases.sharedInstance.awaitPurchase(
                                        PurchaseParams.Builder(purchaseActivity, packageToBuy).build(),
                                    )
                                    proIsActive = hasPro(result.customerInfo)
                                    statusMessage = if (proIsActive) {
                                        "ReplayLead Pro is unlocked."
                                    } else {
                                        "Purchase completed, but the Pro entitlement is not active. Check the RevenueCat entitlement mapping."
                                    }
                                } catch (error: PurchasesTransactionException) {
                                    if (!error.userCancelled) {
                                        statusMessage = "Purchase could not be completed. Please try again."
                                    }
                                } catch (_: Exception) {
                                    statusMessage = "Purchase could not be completed. Please try again."
                                } finally {
                                    purchaseInProgress = false
                                }
                            }
                        },
                        enabled = purchaseEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        if (loading || purchaseInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(if (purchaseEnabled) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Lock, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                proIsActive -> "ReplayLead Pro unlocked"
                                loading -> "Loading live price…"
                                purchaseInProgress -> "Completing purchase…"
                                !configured -> "Connect RevenueCat to purchase"
                                selectedPackage == null -> "Offering unavailable"
                                else -> "Continue with RevenueCat"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (!configured) {
                        Spacer(Modifier.height(8.dp))
                        Text("Pricing preview only. Purchases stay disabled until a public SDK key is configured.", color = DeepInk.copy(alpha = .52f), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    statusMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = if (proIsActive) Mint else Coral, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    if (configured) {
                        TextButtonLike("Restore purchases") {
                            scope.launch {
                                purchaseInProgress = true
                                statusMessage = null
                                try {
                                    proIsActive = hasPro(Purchases.sharedInstance.awaitRestore())
                                    statusMessage = if (proIsActive) {
                                        "ReplayLead Pro was restored."
                                    } else {
                                        "No active ReplayLead Pro purchase was found."
                                    }
                                } catch (_: Exception) {
                                    statusMessage = "Purchases could not be restored. Please try again."
                                } finally {
                                    purchaseInProgress = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit, trailing: String? = null, dark: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (dark) DeepInk else Paper).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = if (dark) Color.White else DeepInk) }
        Text(title, modifier = Modifier.weight(1f), color = if (dark) Color.White else DeepInk, fontWeight = FontWeight.Black, fontSize = 18.sp)
        trailing?.let { Text(it, color = if (dark) Coral else DeepInk.copy(alpha = .5f), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        if (trailing == null) Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        textAlign = TextAlign.Center,
        color = Coral,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}
