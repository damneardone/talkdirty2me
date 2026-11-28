package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.prompt.TtsPrompts
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPromptsTest {

    @Test
    fun vibeSteersDelivery() {
        assertTrue(TtsPrompts.delivery("dominant and commanding", 5).contains("dominant"))
        assertTrue(TtsPrompts.delivery("filthy and unrestrained", 4).contains("filthy"))
        assertTrue(TtsPrompts.delivery("romantic and seductive", 3).contains("seductive"))
    }

    @Test
    fun unknownVibeFallsBackToSultry() {
        assertTrue(TtsPrompts.delivery("random", 2).contains("sultry"))
    }

    @Test
    fun lowSpiceStaysSuggestive() {
        assertTrue(TtsPrompts.delivery("sweet and flattering", 1).contains("Flirty"))
    }

    @Test
    fun highSpiceGoesExplicit() {
        assertTrue(TtsPrompts.delivery("cheeky and playful", 5).contains("explicit"))
    }
}