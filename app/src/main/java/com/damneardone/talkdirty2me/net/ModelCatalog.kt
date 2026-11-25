package com.damneardone.talkdirty2me.net

/** Curated model catalog + favorites logic (byte-identical ids, port of `curatedModels`/`toggleFav`). */
object ModelCatalog {

    val OPENROUTER_MODELS = listOf(
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
    )

    const val RECOMMENDED_MODEL = "~anthropic/claude-fable-latest"

    val OPENAI_MODELS = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1")

    /** Built-in favorites + user-starred, minus hidden ones, first-seen order preserved. */
    fun curated(favs: List<String>, hidden: List<String>): List<String> {
        val hiddenSet = hidden.toHashSet()
        val seen = HashSet<String>()
        val out = ArrayList<String>()
        for (m in OPENROUTER_MODELS + favs) {
            if (m in hiddenSet || !seen.add(m)) continue
            out.add(m)
        }
        return out
    }

    fun isFav(model: String, favs: List<String>, hidden: List<String>): Boolean =
        curated(favs, hidden).contains(model)

    /**
     * Star/unstar [current]; returns the new (favs, hidden) pair. Mirrors the web `toggleFav`:
     * unstar a built-in -> it moves to hidden; star a hidden/non-built-in -> unhidden + favorited.
     */
    fun toggleFav(current: String, favs: List<String>, hidden: List<String>): Pair<List<String>, List<String>> {
        val cur = current.trim()
        val starred = isFav(cur, favs, hidden)
        return if (starred) {
            val f = favs.filter { it != cur }
            val h = if (OPENROUTER_MODELS.contains(cur) && !hidden.contains(cur)) hidden + cur else hidden
            f to h
        } else {
            val h = hidden.filter { it != cur }
            val f = if (!OPENROUTER_MODELS.contains(cur) && !favs.contains(cur)) favs + cur else favs
            f to h
        }
    }
}