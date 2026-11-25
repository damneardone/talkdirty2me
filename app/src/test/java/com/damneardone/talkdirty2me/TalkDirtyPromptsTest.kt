package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.prompt.TalkDirtyPrompts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TalkDirtyPromptsTest {

    private fun level5() = TalkDirtyPrompts.buildInstruction(
        userGender = "woman",
        voice = "a sultry woman",
        vibe = "cheeky and playful",
        spice = 5,
        sentences = 2,
        extra = "mention my tattoos",
        twist = "Echo one charged word or phrase for rhythm (never repeat a whole sentence).",
    )

    private fun level2() = TalkDirtyPrompts.buildInstruction(
        userGender = "man",
        voice = "a smooth-talking man",
        vibe = "bold and confident",
        spice = 2,
        sentences = 3,
        extra = "",
        twist = "Open mid-thought, as if you couldn't hold it in any longer.",
    )

    @Test
    fun level5ErpPromptMarkers() {
        val p = level5()
        assertTrue(p.startsWith("You are a sultry woman, roleplaying LIVE erotic roleplay (ERP) with the reader, right in the middle of it. Consensual adult fun between adults."))
        assertTrue(p.contains("Mode: IN-THE-MOMENT ERP. Stay inside the scene the whole time."))
        assertTrue(p.contains("The reader: the person you are with is a woman."))
        assertTrue(p.contains("Style: cheeky and playful. Hungry, breathless, reactive."))
        assertTrue(p.contains("- EXACTLY 2 sentences. Count your sentences. Never write more or fewer."))
        assertTrue(p.contains("Extra flavor from the reader (weave it in naturally): \"mention my tattoos\"\n"))
        assertTrue(
            p.trimEnd().endsWith(
                "TASK: Write one cheeky and playful in-the-moment ERP message at spice level 5/5, EXACTLY 2 sentences.",
            ),
        )
    }

    @Test
    fun level2SpiceBriefAndTask() {
        val p = level2()
        assertFalse(p.contains("IN-THE-MOMENT ERP"))
        assertTrue(
            p.contains(
                "Spice level: FLIRTY INNUENDO: cheeky, suggestive, clearly turned on. Heavy innuendo and erotic flattery, but no graphic anatomy or sex acts.",
            ),
        )
        assertTrue(p.contains("The reader: the person being flattered and teased is a man."))
        assertTrue(p.contains("- EXACTLY 3 sentences. Count your sentences. Never write more or fewer."))
        assertTrue(!p.contains("Extra flavor from the reader"))
        assertTrue(
            p.trimEnd().endsWith("TASK: Write one bold and confident message at spice level 2/5, EXACTLY 3 sentences."),
        )
    }

    @Test
    fun sentenceWordCounting() {
        val one = TalkDirtyPrompts.buildInstruction("woman", "a sultry woman", "sweet and flattering", 1, 1, "", "Whisper it like a secret — conspiratorial, close, meant only for them.")
        assertTrue(one.contains("- EXACTLY 1 sentence. Count your sentences."))
        assertTrue(one.trimEnd().endsWith("EXACTLY 1 sentence."))
    }

    @Test
    fun extraCleanedAndCappedAt140() {
        val longExtra = ("word " * 60).trim()
        val p = TalkDirtyPrompts.buildInstruction("woman", "a sultry woman", "cheeky and playful", 2, 2, longExtra, "End on a lingering promise of what happens next.")
        val line = p.lineSequence().first { it.startsWith("Extra flavor from the reader") }
        assertTrue(line.endsWith("\""))
        assertTrue(line.length <= "Extra flavor from the reader (weave it in naturally): \"".length + 140 + 1)
        // 140 cap keeps whole words in this fixture: no mid-word cut
        assertFalse(line.dropLast(2).endsWith("wo"))
    }

    private operator fun String.times(n: Int): String = buildString { repeat(n) { append(this@times) } }

    @Test
    fun cleanExtraCollapsesWhitespaceAndTrims() {
        assertEquals("hello world", TalkDirtyPrompts.cleanExtra("   hello    world   "))
        assertEquals(140, TalkDirtyPrompts.cleanExtra("x".repeat(300)).length)
    }

    @Test
    fun twistNeverRepeatsTwiceInARow() {
        val rng = Random(42)
        var last = -1
        repeat(200) {
            val (idx, _) = TalkDirtyPrompts.pickTwist(rng, last)
            assertTrue(idx in TalkDirtyPrompts.VARIETY_TWISTS.indices)
            if (it > 0) assertFalse("twist repeated at call $it", idx == last)
            last = idx
        }
    }
}