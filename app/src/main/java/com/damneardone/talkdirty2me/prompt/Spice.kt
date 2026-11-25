package com.damneardone.talkdirty2me.prompt

data class Choice(val value: String, val label: String)

data class SpiceInfo(val name: String, val desc: String)

/** Decency levels + generator choices, byte-identical to the Perchance source. */
object Spice {
    val LEVELS: Map<Int, SpiceInfo> = mapOf(
        1 to SpiceInfo(
            "😇 Slightly Suggestive (1/5)",
            "Playful wink only — teasing double meanings, nothing graphic. Flirty but deniable.",
        ),
        2 to SpiceInfo(
            "💋 Flirty Innuendo (2/5)",
            "Cheeky and clearly flirting — heavy innuendo, light on explicit detail.",
        ),
        3 to SpiceInfo(
            "🥵 Spicy & Lewd (3/5)",
            "Bold erotic flattery — hungry, hands-on language and explicit suggestion.",
        ),
        4 to SpiceInfo(
            "⛔ X-Rated Bedroom (4/5)",
            "Raw consensual bedroom dirty-talk — graphic, filthy, no euphemisms.",
        ),
        5 to SpiceInfo(
            "🔥 ERP In-The-Moment (5/5)",
            "Live roleplay, first person + present tense — what I'm touching, seeing, tasting right now, and what you're doing to me.",
        ),
    )

    fun name(level: Int): String? = LEVELS[level]?.name
    fun desc(level: Int): String? = LEVELS[level]?.desc
}

object Choices {
    val USER_GENDERS = listOf(
        Choice("woman", "woman"),
        Choice("man", "man"),
        Choice("nonbinary person", "nonbinary"),
        Choice("trans woman", "trans woman"),
        Choice("trans man", "trans man"),
        Choice("genderfluid person", "genderfluid"),
        Choice("agender person", "agender"),
    )

    val VOICES = listOf(
        Choice("a sultry woman", "💋 sultry woman"),
        Choice("a smooth-talking man", "😏 smooth-talking man"),
        Choice("a dominant woman (femdomme)", "⛓️ dominant woman"),
        Choice("a dominant man", "⛓️ dominant man"),
        Choice("a sweet submissive woman", "🌹 sweet submissive woman"),
        Choice("an eager submissive man", "🌹 eager submissive man"),
        Choice("a nonbinary tease", "✨ nonbinary tease"),
    )

    val VIBES = listOf(
        Choice("cheeky and playful", "cheeky & playful"),
        Choice("sweet and flattering", "sweet & flattering"),
        Choice("bold and confident", "bold & confident"),
        Choice("dominant and commanding", "dominant & commanding"),
        Choice("submissive and worshipful", "submissive & worshipful"),
        Choice("romantic and seductive", "romantic & seductive"),
        Choice("filthy and unrestrained", "filthy & unrestrained"),
        Choice("random", "🎲 random"),
    )

    val LENGTHS = listOf(
        Choice("1", "1 sentence"),
        Choice("2", "2 sentences"),
        Choice("3", "3 sentences"),
        Choice("4", "4 sentences"),
        Choice("random", "🎲 random"),
    )
}