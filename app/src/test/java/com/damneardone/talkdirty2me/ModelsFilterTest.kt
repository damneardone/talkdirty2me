package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.net.ModelsClient
import com.damneardone.talkdirty2me.net.Td2mException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsFilterTest {

    private val openrouterFixture = """
        {"data":[
          {"id":"a/b:batch","architecture":{"output_modalities":["image"]}},
          {"id":"z/ai","architecture":{"output_modalities":["text"]}},
          {"id":"z/ai","architecture":{"output_modalities":["text"]}},
          {"id":"named/model","name":"named","architecture":{"output_modalities":["text","image"]}},
          {"id":"e/point","endpoints":["/chat/completions"]},
          {"id":"keep/embed","endpoints":["/embeddings"]},
          {"id":"plain/model"},
          {"id":null,"name":"only-name"}
        ]}
    """.trimIndent()

    @Test
    fun openRouterFiltersTextModalityDedupeSortDropBatch() {
        val models = ModelsClient.parseModels(openrouterFixture, isOpenRouter = true)
        assertEquals(
            listOf("e/point", "named/model", "only-name", "plain/model", "z/ai"),
            models,
        )
    }

    @Test
    fun plainServerKeepsAllButBatch() {
        val models = ModelsClient.parseModels(openrouterFixture, isOpenRouter = false)
        assertEquals(
            listOf("e/point", "keep/embed", "named/model", "only-name", "plain/model", "z/ai"),
            models,
        )
    }

    @Test
    fun nonArrayDataThrowsFriendly() {
        try {
            ModelsClient.parseModels("""{"data":{"unexpected":true}}""", isOpenRouter = false)
            error("expected Td2mException")
        } catch (e: Td2mException) {
            assertEquals("unexpected /models response — type the model id manually", e.message)
        }
    }
}