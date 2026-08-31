package com.replaylead.app.domain

data class Scenario(
    val id: String,
    val title: String,
    val context: String,
    val goal: String,
    val counterpart: String,
    val difficulty: String,
)

enum class Speaker { USER, COUNTERPART }

data class ConversationTurn(
    val id: Long,
    val speaker: Speaker,
    val text: String,
    val branch: Int,
)

data class CoachingReport(
    val clarity: Int,
    val empathy: Int,
    val assertiveness: Int,
    val actionability: Int,
    val strength: String,
    val improvement: String,
    val suggestedResponse: String,
)

val scenarios = listOf(
    Scenario(
        id = "feedback",
        title = "Give difficult feedback",
        context = "Jordan is usually reliable, but two important deadlines slipped this month.",
        goal = "Name the impact, understand what happened, and agree on a concrete recovery plan.",
        counterpart = "Jordan",
        difficulty = "Balanced",
    ),
    Scenario(
        id = "boundary",
        title = "Set a boundary",
        context = "A teammate repeatedly messages late at night and expects an immediate reply.",
        goal = "Set a clear response-time boundary without weakening trust.",
        counterpart = "Casey",
        difficulty = "Direct",
    ),
    Scenario(
        id = "say_no",
        title = "Say no fairly",
        context = "A high performer asks for an exception you cannot fairly offer to the rest of the team.",
        goal = "Decline clearly, explain the principle, and offer a constructive path forward.",
        counterpart = "Morgan",
        difficulty = "Challenging",
    ),
)
