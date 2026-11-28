package com.damneardone.talkdirty2me.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.damneardone.talkdirty2me.GenViewModel
import com.damneardone.talkdirty2me.net.ModelCatalog
import com.damneardone.talkdirty2me.prompt.Choice

/** Bordered + lightly shaded, so it's clear where a settings panel starts and ends. */
@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun StepText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 14.dp),
    )
}

/** 🔌 AI provider + 🔊 Voice panels, arranged as numbered steps for non-tech users. */
@Composable
fun ProviderSheet(vm: GenViewModel) {
    val settings by vm.settings.collectAsState()
    val connStatus by vm.connStatus.collectAsState()
    val modelsStatus by vm.modelsStatus.collectAsState()
    val ttsModels by vm.ttsModels.collectAsState()
    val ttsStatus by vm.ttsStatus.collectAsState()
    val context = LocalContext.current

    var open by remember { mutableStateOf(false) }
    var ttsOpen by remember { mutableStateOf(false) }

    // The live model lists load themselves the first time a panel opens in a session.
    LaunchedEffect(open) { if (open) vm.ensureModels() }
    LaunchedEffect(ttsOpen) { if (ttsOpen) vm.ensureTtsModels() }

    var provider by remember(settings.provider) { mutableStateOf(settings.provider) }
    var base by remember(settings.base) { mutableStateOf(settings.base) }
    var key by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    fun saveAll(model: String = settings.model) = vm.saveProvider(provider, base, key, model)

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        OutlinedButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
            Text("🔌 AI provider — use your own key for spicier models")
        }
        if (open) {
            PanelCard {
                val suggestions = vm.modelSuggestions(settings)

                StepText("Step 1 · Pick your provider")
                DropdownField(
                    "Provider",
                    provider,
                    listOf(
                        Choice("openrouter", "OpenRouter (default)"),
                        Choice("openai", "OpenAI-compatible server"),
                    ),
                ) {
                    provider = it
                    saveAll()
                }
                if (provider == "openai") {
                    OutlinedTextField(
                        value = base,
                        onValueChange = {
                            base = it
                            saveAll()
                        },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }

                StepText("Step 2 · Enter your API key")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = {
                            key = it
                            saveAll()
                        },
                        label = { Text("API key") },
                        placeholder = { Text("sk-…") },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { keyVisible = !keyVisible }) { Text("👁") }
                }
                if (provider == "openrouter") {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys")))
                    }) { Text("🔑 get an OpenRouter key") }
                }
                Text(
                    "Your key is stored only in this app and sent only to the server above — never committed anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                )

                StepText("Step 3 · Pick a model")
                if (settings.model.isNotEmpty()) {
                    Text("Current model: ${settings.model}", style = MaterialTheme.typography.bodySmall)
                }
                DropdownField(
                    "Pick a model (${suggestions.size})",
                    settings.model,
                    suggestions.map { Choice(it, if (it == ModelCatalog.RECOMMENDED_MODEL) "$it (recommended)" else it) },
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    saveAll(it)
                }
                if (provider == "openrouter") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = settings.favOnly,
                            onCheckedChange = { vm.setFavOnly(it) },
                        )
                        Text("★ favorites only")
                        TextButton(onClick = { vm.toggleFavorite(settings.model) }) {
                            Text(if (vm.isFavorite(settings.model, settings)) "★ favorited" else "☆ favorite")
                        }
                    }
                }

                StepText("Step 4 · Test your connection")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::testConnection) { Text("test connection") }
                    OutlinedButton(onClick = {
                        key = ""
                        vm.forgetKey()
                    }) { Text("forget key") }
                }
                if (connStatus.isNotEmpty()) Text(connStatus, style = MaterialTheme.typography.bodySmall)
                if (modelsStatus.isNotEmpty()) Text(modelsStatus, style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedButton(
            onClick = { ttsOpen = !ttsOpen },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("🔊 Voice (TTS) — hear it out loud")
        }
        if (ttsOpen) {
            PanelCard {
                val info = vm.ttsModelInfo(settings)

                Text(
                    "Cloud voices use your OpenRouter key and a little credit per message.",
                    style = MaterialTheme.typography.bodySmall,
                )

                StepText("Step 1 · Pick a voice model")
                if (ttsModels.isEmpty()) {
                    Text("Loading the voice-model list…", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (info != null) {
                        Text("Current: ${info.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    DropdownField(
                        "Pick a voice model (${ttsModels.size})",
                        settings.ttsModel,
                        ttsModels.map { Choice(it.id, it.name) },
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        vm.saveTts(it, settings.ttsVoice, settings.ttsSpeed)
                    }
                }

                if (info != null && info.voices.isNotEmpty()) {
                    StepText("Step 2 · Pick a voice")
                    var voiceQuery by remember(settings.ttsModel) { mutableStateOf("") }
                    OutlinedTextField(
                        value = voiceQuery,
                        onValueChange = { voiceQuery = it },
                        label = { Text("Filter the ${info.voices.size} voices") },
                        placeholder = { Text("type a word — e.g. female, male, or a name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val filtered = info.voices.filter {
                        voiceQuery.isBlank() || it.contains(voiceQuery, ignoreCase = true)
                    }
                    val shown = filtered.take(80)
                    DropdownField(
                        "Pick a voice (${filtered.size} of ${info.voices.size})",
                        settings.ttsVoice,
                        shown.map { Choice(it, it) },
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        vm.saveTts(settings.ttsModel, it, settings.ttsSpeed)
                    }
                    if (filtered.size > shown.size) {
                        Text(
                            "showing the first ${shown.size} — narrow the list with the filter above",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (ttsModels.isNotEmpty()) {
                    StepText("Step 2 · Voice id")
                    Text(
                        "This model doesn't publish a voice list. Paste a voice id from the provider's docs " +
                            "if you have one — or leave it blank to use the model's default voice.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = settings.ttsVoice,
                        onValueChange = { vm.saveTts(settings.ttsModel, it, settings.ttsSpeed) },
                        label = { Text("Voice id (optional)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }

                StepText("Step 3 · Delivery & speed")
                if (info?.supportsInstructions == true) {
                    Text(
                        "🎭 delivery is auto-tagged: your vibe + spice picks on the main screen steer how it's read.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val smin = info?.speedMin
                val smax = info?.speedMax
                if (smin != null && smax != null) {
                    Text(
                        "Speed — ${settings.ttsSpeed}×",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Slider(
                        value = settings.ttsSpeed.toDouble().coerceIn(smin, smax).toFloat(),
                        onValueChange = { vm.saveTts(settings.ttsModel, settings.ttsVoice, it) },
                        valueRange = smin.toFloat()..smax.toFloat(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = settings.readAloud, onCheckedChange = { vm.setReadAloud(it) })
                    Text("🔊 read every new message aloud automatically")
                }
                if (settings.apiKey.isEmpty()) {
                    Text(
                        "⚠ you haven't entered an API key yet — see Step 2 of the AI provider panel above.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (ttsStatus.isNotEmpty()) Text(ttsStatus, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}