package com.damneardone.talkdirty2me.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.damneardone.talkdirty2me.GenViewModel
import com.damneardone.talkdirty2me.net.ModelCatalog
import com.damneardone.talkdirty2me.prompt.Choice

/** 🔌 AI provider panel (OpenRouter default / OpenAI-compatible server), ported from the web details box. */
@Composable
fun ProviderSheet(vm: GenViewModel) {
    val settings by vm.settings.collectAsState()
    val connStatus by vm.connStatus.collectAsState()
    val modelsStatus by vm.modelsStatus.collectAsState()

    var open by remember { mutableStateOf(false) }
    var provider by remember(settings.provider) { mutableStateOf(settings.provider) }
    var base by remember(settings.base) { mutableStateOf(settings.base) }
    var key by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var model by remember(settings.model) { mutableStateOf(settings.model) }
    var keyVisible by remember { mutableStateOf(false) }

    fun saveAll() = vm.saveProvider(provider, base, key, model)

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        TextButton(onClick = { open = !open }) {
            Text("🔌 AI provider: ${vm.providerName(provider)} — use your own key for spicier models")
        }
        if (open) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
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
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                        )
                        TextButton(onClick = { keyVisible = !keyVisible }) { Text("👁") }
                    }
                    Text(
                        "Your key is stored only in this app and sent only to the server URL above — never committed anywhere. " +
                            "OpenRouter keys: openrouter.ai/keys · models: openrouter.ai/models",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = {
                                model = it
                                saveAll()
                            },
                            label = { Text("Model") },
                            placeholder = {
                                Text(
                                    if (provider == "openrouter") "e.g. anthropic/claude-opus-5.5" else "e.g. gpt-4o-mini",
                                )
                            },
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                        )
                        TextButton(onClick = vm::refreshModels) { Text("↻") }
                    }

                    val suggestions = vm.modelSuggestions(settings)
                    DropdownField(
                        "Pick a model (${suggestions.size})",
                        model,
                        suggestions.map { Choice(it, if (it == ModelCatalog.RECOMMENDED_MODEL) "$it (recommended)" else it) },
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        model = it
                        saveAll()
                    }

                    if (provider == "openrouter") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = settings.favOnly,
                                onCheckedChange = { vm.setFavOnly(it) },
                            )
                            Text("★ favorites only")
                            TextButton(onClick = { vm.toggleFavorite(model) }) {
                                Text(if (vm.isFavorite(model, settings)) "★ favorited" else "☆ favorite")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
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
        }
    }
}