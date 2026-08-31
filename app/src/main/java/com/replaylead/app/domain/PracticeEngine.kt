package com.replaylead.app.domain

class PracticeEngine {
    fun opening(scenario: Scenario): String = when (scenario.id) {
        "feedback" -> "You said you wanted to talk about the project. Is something wrong?"
        "boundary" -> "I know I message late, but the work is urgent. What else am I supposed to do?"
        else -> "I have delivered a lot this quarter. Why can't you make one exception for me?"
    }

    fun reply(scenario: Scenario, userText: String, turnNumber: Int): String {
        val lower = userText.lowercase()
        val acknowledges = listOf("understand", "hear", "appreciate", "help me", "what happened")
            .any(lower::contains)
        val concrete = listOf("by ", "next", "tomorrow", "plan", "agree", "deadline")
            .any(lower::contains)

        return when {
            turnNumber >= 3 && concrete -> "That feels specific. I can commit to that, and I'll flag it earlier if the plan starts slipping."
            scenario.id == "feedback" && acknowledges -> "I've been covering part of another project and I was embarrassed to say I was behind."
            scenario.id == "boundary" && acknowledges -> "I don't need an instant answer every time. I do need to know what counts as a real emergency."
            scenario.id == "say_no" && acknowledges -> "I still don't love the answer, but I want to understand what I can work toward instead."
            lower.contains("but") && !acknowledges -> "It sounds like the decision is already made. Are you actually asking for my perspective?"
            else -> "I'm not sure what you want me to do differently. Can you be more specific?"
        }
    }

    fun coach(turns: List<ConversationTurn>): CoachingReport {
        val userText = turns.filter { it.speaker == Speaker.USER }.joinToString(" ") { it.text.lowercase() }
        fun hasAny(vararg words: String) = words.any(userText::contains)
        val clarity = 58 + if (hasAny("impact", "because", "specific")) 18 else 4
        val empathy = 56 + if (hasAny("understand", "help me", "hear", "perspective")) 24 else 5
        val assertiveness = 55 + if (hasAny("need", "expect", "cannot", "boundary")) 22 else 6
        val actionability = 52 + if (hasAny("next", "by ", "plan", "agree", "tomorrow")) 27 else 5
        return CoachingReport(
            clarity = clarity.coerceAtMost(94),
            empathy = empathy.coerceAtMost(94),
            assertiveness = assertiveness.coerceAtMost(94),
            actionability = actionability.coerceAtMost(94),
            strength = if (empathy >= 75) {
                "You made space for the other person's perspective before moving to the ask."
            } else {
                "You stayed focused on the situation instead of attacking the person."
            },
            improvement = if (actionability < 72) {
                "End with one observable next step, an owner, and a time to check back."
            } else {
                "Make the impact statement one sentence shorter so the request lands faster."
            },
            suggestedResponse = "I want to understand what got in the way. Then let's agree on one concrete next step and when we'll check progress.",
        )
    }
}
