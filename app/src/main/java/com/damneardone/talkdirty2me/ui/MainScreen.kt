package com.damneardone.talkdirty2me.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.damneardone.talkdirty2me.GenViewModel
import com.damneardone.talkdirty2me.prompt.Choice
import com.damneardone.talkdirty2me.prompt.Choices
import com.damneardone.talkdirty2me.prompt.Spice
import kotlinx.coroutines.delay

/** Simple label + outlined-button dropdown (port of the web <select>s). */
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<Choice>,
    modifier: Modifier = Modifier,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(options.find { it.value == value }?.label ?: value)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(o.label) },
                        onClick = {
                            onPick(o.value)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val alpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Text("▌", color = AccentPink.copy(alpha = alpha))
}

@Composable
fun MainScreen(vm: GenViewModel) {
    val message by vm.message.collectAsState()
    val status by vm.status.collectAsState()
    val settings by vm.settings.collectAsState()
    val generating by vm.generating.collectAsState()
    val userGender by vm.userGender.collectAsState()
    val voice by vm.voice.collectAsState()
    val vibe by vm.vibeChoice.collectAsState()
    val spiceLevel by vm.spice.collectAsState()
    val lengthChoice by vm.lengthChoice.collectAsState()
    val extra by vm.extra.collectAsState()
    val speaking by vm.speaking.collectAsState()
    val ttsStatus by vm.ttsStatus.collectAsState()

    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔥 TalkDirty2Me", style = MaterialTheme.typography.headlineMedium)
        Text(
            "AI-powered dirty-mouthed messages — double entendre, innuendo & erotic flattery, written to you. " +
                "Pick a spice level, pick a voice, get 1–4 sentences. Level 5 plays it in the moment, like live ERP.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(
            modifier = Modifier.padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(
                        "Your gender", userGender, Choices.USER_GENDERS,
                        Modifier.weight(1f),
                    ) { vm.userGender.value = it }
                    DropdownField(
                        "Dirty-talker voice", voice, Choices.VOICES,
                        Modifier.weight(1f),
                    ) { vm.voice.value = it }
                }

                Text(
                    "Decency level",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    Spice.name(spiceLevel) ?: "",
                    color = AccentPink,
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = spiceLevel.toFloat(),
                    onValueChange = { vm.spice.value = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 4,
                )
                Text(
                    Spice.desc(spiceLevel) ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    DropdownField(
                        "Length", lengthChoice, Choices.LENGTHS,
                        Modifier.weight(1f),
                    ) { vm.lengthChoice.value = it }
                    DropdownField(
                        "Vibe", vibe, Choices.VIBES,
                        Modifier.weight(1f),
                    ) { vm.vibeChoice.value = it }
                }

                OutlinedTextField(
                    value = extra,
                    onValueChange = { vm.extra.value = it.take(140) },
                    label = { Text("Extra flavor (optional)") },
                    placeholder = { Text("e.g. good-morning text, after a long day, mention my tattoos…") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )

                OutlinedTextField(
                    value = settings.bans,
                    onValueChange = { vm.saveBans(it) },
                    label = { Text("🚫 Banned phrases (optional, comma-separated)") },
                    placeholder = { Text("e.g. every inch of, porcelain, isn't shy") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
        val ready = settings.apiKey.isNotEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = vm::generate, enabled = ready && !generating) { Text("🔥 Talk dirty to me") }
            if (generating) {
                OutlinedButton(onClick = vm::stopGen) { Text("stop") }
            }
            OutlinedButton(onClick = vm::surpriseMe, enabled = ready && !generating) { Text("🎲 surprise me") }
        }
        if (!ready) {
            Text(
                "🔌 set up your API key in the AI provider panel below first — it takes about a minute.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (status.isNotEmpty()) {
            Text(status, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (ttsStatus.isNotEmpty()) {
            Text(ttsStatus, style = MaterialTheme.typography.bodySmall)
        }

        if (message.isNotEmpty() || generating) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AccentPink.copy(alpha = 0.10f),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Row(Modifier.padding(12.dp)) {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    if (generating) BlinkingCursor()
                }
            }
            if (message.isNotEmpty() && !generating) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(message))
                        copied = true
                    }) { Text(if (copied) "copied! 😘" else "copy message") }
                    if (!speaking) {
                        Button(onClick = { vm.speak(message) }, enabled = ready) { Text("🔊 read it to me") }
                    } else {
                        OutlinedButton(onClick = vm::stopSpeaking) { Text("⏹ hush") }
                    }
                    OutlinedButton(onClick = vm::generate, enabled = ready) { Text("↻ more like this") }
                }
            }
        }

        ProviderSheet(vm)

        Text(
            "🔞 Adults only. Everything here is consensual fantasy between adults — be kind, get consent, and keep it private.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        TextButton(onClick = vm::lockSite) { Text("leave / lock") }
    }
}