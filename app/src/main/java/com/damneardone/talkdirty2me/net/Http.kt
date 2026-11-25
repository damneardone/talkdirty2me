package com.damneardone.talkdirty2me.net

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Small shared HTTP helpers for the OpenAI-compatible clients. */
object Http {

    /** OpenRouter attribution headers (replaces the perchance.org referer of the web version). */
    fun addOpenRouterAttribution(builder: okhttp3.Request.Builder) {
        builder.header("HTTP-Referer", "https://github.com/damneardone/talkdirty2me")
        builder.header("X-Title", "TalkDirty2Me")
    }

    suspend fun executeAwait(client: OkHttpClient, request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (call.isCanceled()) {
                        cont.cancel(e)
                    } else {
                        cont.resumeWithException(e)
                    }
                }
            })
        }
}