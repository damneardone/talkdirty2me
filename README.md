# TalkDirty2Me 🔥

18+ adults-only Android app: AI-powered dirty-talk message generator —
double entendre, innuendo & erotic flattery written *to you*, up to live
in-the-moment ERP. Consensual adult fantasy between adults; nothing here is
for minors.

An original creation, first built as a private generator on perchance.org
(`talkdirty2me` — that generator stays live there, frozen). The author's own
generator source (list code + output template) is preserved under
`reference/perchance/` as the porting reference. **No Perchance engine or
plugin code is included in this repo** — `ai-text-plugin` is not vendored
(its author states it is coupled to perchance.org's servers and must not be
copied).

## AI provider

The app talks to **OpenRouter by default** (BYOK — bring your own key):

1. Open the 🔌 *AI provider* box in the app.
2. Paste your OpenRouter key (create one at <https://openrouter.ai/keys>).
3. Pick a model (the live list loads automatically when you open the panel; ★ favorites are curated).
Your key is stored only in the app's private on-device storage and is sent
only to the provider you configure — never committed anywhere.

A generic **OpenAI-compatible server** option (base URL + key + model) is
also built in — that's the path for local backends (e.g. LM Studio) later.

## 🚫 Banned phrases

Tired of seeing the same words in every generation ("every inch of", anyone?)?
Type them into the *Banned phrases* box on the main screen — they're injected into
every prompt as a hard rule ("never write these, not even close variants") and,
if a model still sneaks one past, the status line flags it so you can reroll.

## Voice (TTS)

The 🔊 *Voice* box reads messages out loud:

1. **OpenRouter cloud voices** — same key as chat: pick a voice model, then a voice
   and a speed (the voice list loads automatically). Each message costs a little OpenRouter credit.
2. **🎭 Auto-tagged delivery** — on voice models that accept `instructions`
   (e.g. Seed-TTS, Gemini TTS), your vibe + spice picks steer *how* the line is read.
3. Offline on-device voices are on the roadmap below.

## Roadmap

- [x] Cloud TTS — OpenRouter `/audio/speech` (same key as chat, per-model voices + speed + delivery `instructions`)
- [ ] Built-in TTS — on-device, NekoSpeak-style ONNX engines (Piper / Kokoro / KittenTTS / Pocket-tts)
- [ ] Image generation via AI Horde

## Development

Native Android app, Kotlin + Jetpack Compose.

```
./gradlew :app:test        # unit tests
./gradlew assembleDebug    # -> app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17+ (Android Studio's JBR works); `local.properties` points at
your Android SDK. Note: if `java` on your PATH is an old Java 8, don't use
it for Gradle — the wrapper + a JDK 17+ `JAVA_HOME` is the safe route.