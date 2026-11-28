package com.damneardone.talkdirty2me.tts

import android.content.Context
import android.media.MediaPlayer
import com.damneardone.talkdirty2me.net.Td2mException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class TtsFormat(val ext: String) {
    MP3("mp3"),
    WAV("wav"),
    PCM_16LE("raw");

    companion object {
        fun of(s: String) = when (s.lowercase()) {
            "mp3" -> MP3
            "wav" -> WAV
            else -> PCM_16LE
        }
    }
}

/** Synthesized audio: raw bytes + detected playback format. */
data class TtsAudio(val bytes: ByteArray, val format: TtsFormat)

/**
 * Plays synthesized audio through MediaPlayer (mp3/wav) from a cacheDir temp file.
 * Cloud TTS only for now; the ONNX engine (v0.2b) will feed the same [TtsAudio].
 */
class TtsPlayer(private val context: Context) {

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private var active: MediaPlayer? = null
    private var activeFile: File? = null

    /** Stops and releases any current playback (also used for coroutine cancellation). */
    fun stop() {
        val p = active
        val f = activeFile
        active = null
        activeFile = null
        _playing.value = false
        if (p != null) {
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        f?.delete()
    }

    /** Plays [audio] through to completion. Throws [Td2mException] on unsupported format or playback failure. */
    suspend fun play(audio: TtsAudio): Unit = suspendCancellableCoroutine { cont ->
        if (audio.format == TtsFormat.PCM_16LE) {
            cont.resumeWithException(Td2mException("that voice model outputs raw PCM — pick a model with mp3 or wav output"))
            return@suspendCancellableCoroutine
        }
        stop()
        val file = File(context.cacheDir, "td2m_voice.${audio.format.ext}")
        try {
            file.writeBytes(audio.bytes)
        } catch (e: Exception) {
            cont.resumeWithException(Td2mException("could not cache audio for playback (${e.message})"))
            return@suspendCancellableCoroutine
        }
        val player = MediaPlayer()
        active = player
        activeFile = file
        cont.invokeOnCancellation { stop() }
        player.setOnCompletionListener {
            stop()
            if (cont.isActive) cont.resume(Unit)
        }
        player.setOnErrorListener { _, what, extra ->
            stop()
            if (cont.isActive) cont.resumeWithException(Td2mException("playback failed (what=$what extra=$extra)"))
            true
        }
        try {
            player.setDataSource(file.absolutePath)
            player.prepare()
            _playing.value = true
            player.start()
        } catch (e: Exception) {
            stop()
            if (cont.isActive) cont.resumeWithException(Td2mException("could not play the audio (${e.message})"))
        }
    }
}