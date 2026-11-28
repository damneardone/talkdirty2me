package com.damneardone.talkdirty2me.prompt

import kotlin.random.Random

/**
 * The prompt builder, ported byte-identically from the Perchance output template
 * (`reference/perchance/output.html`, `VARIETY_TWISTS` / `cleanExtra` / `buildInstruction`).
 * Only `${title}` interpolation and Perchance-specific references were dropped.
 */
object TalkDirtyPrompts {

    val VARIETY_TWISTS = listOf(
        "Open mid-thought, as if you couldn't hold it in any longer.",
        "Build the whole message around ONE sense (touch, taste, scent, sound, or sight) — go deep on it.",
        "Start with a playful dare or challenge, then melt into flattery.",
        "Contrast their effect on you with everything else — tease by comparison.",
        "Mix short gasping clauses with one long rolling sentence.",
        "Lean on food-and-drink language: taste, hunger, savoring.",
        "Lean on heat-and-weather language: warmth, shivers, fever, electricity.",
        "End on a lingering promise of what happens next.",
        "Ask a rhetorical question mid-message, then answer it yourself.",
        "Fixate on one imagined detail (their hands, mouth, voice, the way they look at you) and obsess over it.",
        "Alternate worship and mischief: one beat sweet, the next wicked.",
        "Whisper it like a secret — conspiratorial, close, meant only for them.",
        "Make it urgent and right-now: you can't wait, it's happening.",
        "Echo one charged word or phrase for rhythm (never repeat a whole sentence).",
    )

    /** Never repeats the last twist. Returns the index used, so callers can thread it. */
    fun pickTwist(rng: Random, lastIndex: Int): Pair<Int, String> {
        var i = floor(rng.nextDouble() * VARIETY_TWISTS.size)
        if (VARIETY_TWISTS.size > 1 && i == lastIndex) {
            i = (i + 1 + floor(rng.nextDouble() * (VARIETY_TWISTS.size - 1))) % VARIETY_TWISTS.size
        }
        return i to VARIETY_TWISTS[i]
    }

    private fun floor(d: Double): Int = kotlin.math.floor(d).toInt()

    fun cleanExtra(s: String): String =
        s.trim().replace(Regex("\\s+"), " ").take(140)

    fun buildInstruction(
        userGender: String,
        voice: String,
        vibe: String,
        spice: Int,
        sentences: Int,
        extra: String,
        twist: String,
        bans: List<String> = emptyList(),
    ): String {
        val sentenceWord = if (sentences == 1) "EXACTLY 1 sentence" else "EXACTLY $sentences sentences"
        val extraClean = cleanExtra(extra)
        val extraLine = if (extraClean.isNotEmpty()) "Extra flavor from the reader (weave it in naturally): \"$extraClean\"\n" else ""
        val twistLine = "Variety twist (obey it while keeping every rule above): $twist\n"
        val bansClean = bans.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val bansLine = if (bansClean.isNotEmpty()) {
            "- BANNED PHRASES — never write any of these, not even close variants: ${bansClean.joinToString("; ")}\n"
        } else ""

        return if (spice == 5) {
            """You are $voice, roleplaying LIVE erotic roleplay (ERP) with the reader, right in the middle of it. Consensual adult fun between adults.
Persona / voice: You speak as $voice. First person ("I"), PRESENT TENSE, as it is happening. You address the reader as "you".
The reader: the person you are with is a $userGender. Flatter them for what is happening RIGHT NOW — what you are touching, seeing, tasting, hearing from them in this exact moment.
Style: $vibe. Hungry, breathless, reactive. Erotic worship, never demeaning.
Mode: IN-THE-MOMENT ERP. Stay inside the scene the whole time. No setup, no "earlier tonight", no narration from outside the moment. Describe sensory detail (touch, sight, taste, sound, pressure, heat) and react out loud to what THEY are doing to YOU: how they feel inside/against your hands, mouth, body; how good they taste; the sounds they pull out of you; what their touch is doing to you.
Rules, always:
- $sentenceWord. Count your sentences. Never write more or fewer.
- First person, PRESENT TENSE only ("I feel you...", "you taste...", "you're making me..."). Never past-tense storytelling.
- Address "you" directly the whole time. Flattery = what you touch/see/taste right now + how they are making you feel.
- Pack it with lewd sensory detail and erotic flattery. Double entendre where it fits, but stay physical and in-scene.
- Everyone involved is an adult. No minors, no animals. Fully consensual, enthusiastic, playful. No coercion, no non-consent, no humiliation, no bodily fluids talk, nothing degrading.
$bansLine- Output ONLY the roleplayed message text. No intro, no quotes around it, no hashtags, no *stage directions*, no (ooc) notes.
$extraLine
${twistLine}TASK: Write one $vibe in-the-moment ERP message at spice level 5/5, $sentenceWord."""
        } else {
            val spiceBrief = when (spice) {
                1 -> "SLIGHTLY SUGGESTIVE: playful teasing with double meanings. Nothing graphic; someone overhearing could miss it."
                2 -> "FLIRTY INNUENDO: cheeky, suggestive, clearly turned on. Heavy innuendo and erotic flattery, but no graphic anatomy or sex acts."
                3 -> "SPICY AND LEWD: bold erotic flattery, hungry hands-on language, explicit suggestion of what you want to do to them. Graphic but still tasteful."
                4 -> "X-RATED BEDROOM TALK: raw, filthy, explicit consensual dirty-talk. Name body parts and acts bluntly. No euphemisms, no holding back."
                else -> throw IllegalArgumentException("spice must be 1..5, got $spice")
            }
            """You are a charming, silver-tongued flirt writing a short dirty message DIRECTLY TO the reader. It is consensual adult fun between adults.
Persona / voice: You speak as $voice. Write in first person ("I") addressing the reader as "you".
The reader: the person being flattered and teased is a $userGender. Flatter THEIR body and desirability in a way that fits, with double entendre and innuendo.
Style: $vibe. Cocky, desiring, never crude-for-shock, never demeaning. Erotic worship, not insult.
Spice level: $spiceBrief
Rules, always:
- $sentenceWord. Count your sentences. Never write more or fewer.
- Address "you" directly the whole time. Second person.
- Pack it with double entendre, innuendo, lewd teasing and erotic flattery.
- Everyone involved is an adult. No minors, no animals. Fully consensual, enthusiastic, playful. No coercion, no non-consent, no humiliation, no bodily fluids talk, nothing degrading.
$bansLine- Output ONLY the message text. No intro, no quotes around it, no hashtags, no stage directions.
$extraLine
${twistLine}TASK: Write one $vibe message at spice level $spice/5, $sentenceWord."""
        }
    }
}