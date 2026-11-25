package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.net.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun catalogMatchesPortedIds() {
        assertEquals(
            listOf(
                "z-ai/glm-4.7-flash",
                "~z-ai/glm-flash-latest",
                "z-ai/glm-5.3-flash",
                "x-ai/grok-4.20",
                "~anthropic/claude-fable-latest",
                "~anthropic/claude-sonnet-latest",
                "~anthropic/claude-opus-latest",
                "anthropic/claude-sonnet-5.5",
                "anthropic/claude-opus-5.5",
                "deepseek/deepseek-v4.1-flash-1017",
                "thedrummer/cydonia-24b-v4.1",
                "thedrummer/valkyrie-49b-v1",
                "thedrummer/skyfall-36b-v2",
                "thedrummer/unslopnemo-12b",
                "thedrummer/rocinante-12b",
                "cognitivecomputations/dolphin-mistral-24b-venice-edition",
            ),
            ModelCatalog.OPENROUTER_MODELS,
        )
        assertEquals("~anthropic/claude-fable-latest", ModelCatalog.RECOMMENDED_MODEL)
        assertEquals(
            listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1"),
            ModelCatalog.OPENAI_MODELS,
        )
    }

    @Test
    fun curatedHidesAndAppendsUserFavorites() {
        val curated = ModelCatalog.curated(
            favs = listOf("custom/starred", "z-ai/glm-4.7-flash"),
            hidden = listOf("x-ai/grok-4.20"),
        )
        // built-ins minus hidden, then user favorites appended (first-seen order)
        assertEquals(
            listOf(
                "z-ai/glm-4.7-flash",
                "~z-ai/glm-flash-latest",
                "z-ai/glm-5.3-flash",
                "~anthropic/claude-fable-latest",
                "~anthropic/claude-sonnet-latest",
                "~anthropic/claude-opus-latest",
                "anthropic/claude-sonnet-5.5",
                "anthropic/claude-opus-5.5",
                "deepseek/deepseek-v4.1-flash-1017",
                "thedrummer/cydonia-24b-v4.1",
                "thedrummer/valkyrie-49b-v1",
                "thedrummer/skyfall-36b-v2",
                "thedrummer/unslopnemo-12b",
                "thedrummer/rocinante-12b",
                "cognitivecomputations/dolphin-mistral-24b-venice-edition",
                "custom/starred",
            ),
            curated,
        )
    }

    @Test
    fun toggleFavBuiltInMovesToHidden() {
        val (favs, hidden) = ModelCatalog.toggleFav(
            "z-ai/glm-4.7-flash",
            favs = listOf("custom/starred"),
            hidden = emptyList(),
        )
        assertEquals(listOf("custom/starred"), favs)
        assertEquals(listOf("z-ai/glm-4.7-flash"), hidden)
        assertFalse(ModelCatalog.isFav("z-ai/glm-4.7-flash", favs, hidden))
    }

    @Test
    fun toggleFavUnlistedAddsToFavs() {
        val (favs, hidden) = ModelCatalog.toggleFav(
            "custom/starred",
            favs = emptyList(),
            hidden = listOf("z-ai/glm-4.7-flash"),
        )
        assertEquals(listOf("custom/starred"), favs)
        assertEquals(listOf("z-ai/glm-4.7-flash"), hidden)
        assertTrue(ModelCatalog.isFav("custom/starred", favs, hidden))
    }

    @Test
    fun toggleFavUnstarsHiddenBuiltInThenRestars() {
        val once = ModelCatalog.toggleFav("z-ai/glm-4.7-flash", emptyList(), emptyList())
        assertFalse(ModelCatalog.isFav("z-ai/glm-4.7-flash", once.first, once.second))
        val twice = ModelCatalog.toggleFav("z-ai/glm-4.7-flash", once.first, once.second)
        assertTrue(ModelCatalog.isFav("z-ai/glm-4.7-flash", twice.first, twice.second))
        // starred built-in returns to the visible built-in list
        assertTrue(ModelCatalog.curated(twice.first, twice.second).contains("z-ai/glm-4.7-flash"))
    }
}