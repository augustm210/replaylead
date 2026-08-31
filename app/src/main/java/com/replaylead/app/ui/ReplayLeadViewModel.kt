package com.replaylead.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replaylead.app.data.RemoteConversationService
import com.replaylead.app.domain.CoachingReport
import com.replaylead.app.domain.ConversationTurn
import com.replaylead.app.domain.PracticeEngine
import com.replaylead.app.domain.Scenario
import com.replaylead.app.domain.Speaker
import com.replaylead.app.domain.scenarios
import kotlinx.coroutines.launch

enum class AppStage { HOME, SCENARIOS, PRACTICE, REPORT, PAYWALL }

data class ReplayLeadUiState(
    val stage: AppStage = AppStage.HOME,
    val selectedScenario: Scenario = scenarios.first(),
    val confidenceBefore: Int = 2,
    val confidenceAfter: Int = 4,
    val turns: List<ConversationTurn> = emptyList(),
    val branch: Int = 0,
    val rewindNotice: String? = null,
    val report: CoachingReport? = null,
    val engineLabel: String = "Local rehearsal engine",
    val isSending: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val serviceNotice: String? = null,
)

class ReplayLeadViewModel : ViewModel() {
    private val engine = PracticeEngine()
    private val remote = RemoteConversationService()
    private var nextId = 1L
    var state by mutableStateOf(ReplayLeadUiState())
        private set

    fun go(stage: AppStage) {
        state = state.copy(stage = stage)
    }

    fun chooseScenario(scenario: Scenario) {
        state = state.copy(selectedScenario = scenario)
    }

    fun setConfidenceBefore(value: Int) {
        state = state.copy(confidenceBefore = value)
    }

    fun startPractice() {
        val scenario = state.selectedScenario
        state = state.copy(
            stage = AppStage.PRACTICE,
            turns = listOf(
                ConversationTurn(nextId++, Speaker.COUNTERPART, engine.opening(scenario), 0),
            ),
            branch = 0,
            rewindNotice = null,
            report = null,
            engineLabel = if (remote.isConfigured) "AI rehearsal · secure cloud service" else "Local rehearsal engine",
            isSending = false,
            isGeneratingReport = false,
            serviceNotice = null,
        )
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || state.isSending || state.isGeneratingReport) return
        val branch = state.branch
        val userTurn = ConversationTurn(nextId++, Speaker.USER, clean, branch)
        val userTurnCount = state.turns.count { it.speaker == Speaker.USER } + 1
        val turnsWithUser = state.turns + userTurn
        val scenario = state.selectedScenario
        state = state.copy(turns = turnsWithUser, rewindNotice = null, isSending = true, serviceNotice = null)
        viewModelScope.launch {
            val remoteReply = if (remote.isConfigured) runCatching { remote.reply(scenario, turnsWithUser) } else null
            val replyText = remoteReply?.getOrNull() ?: engine.reply(scenario, clean, userTurnCount)
            if (state.turns.none { it.id == userTurn.id }) return@launch
            val reply = ConversationTurn(nextId++, Speaker.COUNTERPART, replyText, branch)
            state = state.copy(
                turns = state.turns + reply,
                isSending = false,
                engineLabel = if (remoteReply?.isSuccess == true) "AI rehearsal · secure cloud service" else "Local rehearsal engine",
                serviceNotice = if (remoteReply?.isFailure == true) "Cloud coaching was unavailable, so this turn used the on-device fallback." else null,
            )
        }
    }

    fun rewind(userTurnId: Long) {
        if (state.isSending || state.isGeneratingReport) return
        val index = state.turns.indexOfFirst { it.id == userTurnId && it.speaker == Speaker.USER }
        if (index < 0) return
        val newBranch = state.branch + 1
        state = state.copy(
            turns = state.turns.take(index),
            branch = newBranch,
            rewindNotice = "Branch ${newBranch + 1} started. Try a different response.",
        )
    }

    fun finishPractice() {
        if (state.isSending || state.isGeneratingReport) return
        val turns = state.turns
        val scenario = state.selectedScenario
        if (!remote.isConfigured) {
            state = state.copy(stage = AppStage.REPORT, report = engine.coach(turns))
            return
        }
        state = state.copy(isGeneratingReport = true, serviceNotice = null)
        viewModelScope.launch {
            val remoteReport = runCatching { remote.coach(scenario, turns) }
            if (state.turns != turns) return@launch
            state = state.copy(
                stage = AppStage.REPORT,
                report = remoteReport.getOrElse { engine.coach(turns) },
                isGeneratingReport = false,
                engineLabel = if (remoteReport.isSuccess) "AI rehearsal · secure cloud service" else "Local rehearsal engine",
                serviceNotice = if (remoteReport.isFailure) "Cloud coaching was unavailable, so this report used the on-device fallback." else null,
            )
        }
    }

    fun setConfidenceAfter(value: Int) {
        state = state.copy(confidenceAfter = value)
    }

    fun reset() {
        state = ReplayLeadUiState()
    }
}
