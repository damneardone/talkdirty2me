package com.damneardone.talkdirty2me.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Full-screen 18+ gate; strings ported from the Perchance source (Perchance links dropped). */
@Composable
fun AgeGate(onConfirm: (rememberMe: Boolean) -> Unit) {
    var confirmed by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var declined by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().background(GateBackground).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(GateCard, RoundedCornerShape(16.dp))
                .border(2.dp, AccentPink, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (declined) {
                Text("🔞", fontSize = 40.sp)
                Text(
                    "No worries — come back later",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "This app is only for adults aged 18+. The content stays hidden until you confirm your age.",
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("🔞", fontSize = 40.sp)
                Text(
                    "Adults only — explicit content ahead",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "This app, TalkDirty2Me, writes suggestive to sexually explicit bedroom roleplay — " +
                        "double entendre, innuendo, lewd comments and erotic flattery, up to live in-the-moment ERP.",
                    textAlign = TextAlign.Center,
                )
                Text(
                    "It is intended only for consenting adults aged 18 or older. Everything here is consensual " +
                        "adult fantasy. Nothing in this app is suitable for minors.",
                    textAlign = TextAlign.Center,
                )
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                    Text("I am 18 years or older and I want to enter and see explicit content.")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text("Remember me on this device")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = confirmed, onClick = { onConfirm(rememberMe) }) { Text("Enter 🔥") }
                    OutlinedButton(onClick = { declined = true }) { Text("Leave") }
                }
            }
        }
    }
}