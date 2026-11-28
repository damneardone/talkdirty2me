package com.damneardone.talkdirty2me.prompt

/**
 * Delivery directions for TTS models that accept free-text `instructions`
 * (OpenRouter /audio/speech — e.g. Seed-TTS, Gemini TTS). The vibe + spice
 * picks steer *how* the message is read, so the voice matches what the
 * chat model just wrote — no manual tagging.
 */
object TtsPrompts {

    fun delivery(vibe: String, spice: Int): String {
        val vibeLine = when (vibe) {
            "cheeky and playful" -> "Deliver it cheeky and playful — a grin in your voice, teasing little pauses."
            "sweet and flattering" -> "Deliver it warm and sweet — soft, adoring, unhurried."
            "bold and confident" -> "Deliver it bold and confident — smirking, in command, zero hesitation."
            "dominant and commanding" -> "Deliver it dominant and commanding — low, controlled, an order impossible to refuse."
            "submissive and worshipful" -> "Deliver it soft and worshipful — breathy, eager to please."
            "romantic and seductive" -> "Deliver it slow and seductive — velvet, romantic, close to the ear."
            "filthy and unrestrained" -> "Deliver it filthy and unrestrained — hungry, shameless, ragged with want."
            else -> "Deliver it sultry and intimate."
        }
        val heatLine = when {
            spice >= 4 -> "Whisper-close and explicit — bedroom talk, urgent and physical."
            spice == 3 -> "Spicy and heated — clearly turned on, but still smooth."
            else -> "Flirty and suggestive — warm with heat under it, never clinical."
        }
        return "$vibeLine $heatLine Read it like a voice message meant for one person's ears only."
    }
}