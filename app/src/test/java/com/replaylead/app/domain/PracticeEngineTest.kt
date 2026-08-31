package com.replaylead.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeEngineTest {
    private val engine = PracticeEngine()

    @Test
    fun empathy_language_improves_empathy_score() {
        val turns = listOf(
            ConversationTurn(1, Speaker.USER, "I want to understand your perspective. Help me understand what happened.", 0),
        )

        assertTrue(engine.coach(turns).empathy >= 75)
    }

    @Test
    fun concrete_plan_improves_actionability_score() {
        val turns = listOf(
            ConversationTurn(1, Speaker.USER, "Let's agree on a plan by tomorrow and check progress next week.", 0),
        )

        assertTrue(engine.coach(turns).actionability >= 75)
    }
}
