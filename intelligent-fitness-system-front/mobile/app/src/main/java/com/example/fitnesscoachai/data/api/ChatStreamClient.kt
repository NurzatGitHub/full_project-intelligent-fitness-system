package com.example.fitnesscoachai.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Reads a Server-Sent Events stream from /api/assistant/chat/stream/ and emits
 * one ChunkEvent per Gemini token batch.
 *
 * Why a manual SSE reader instead of Retrofit?  Retrofit's converters buffer
 * the whole body; for streaming we need to consume the response line-by-line
 * as bytes arrive over the wire. OkHttp gives us exactly that via BufferedSource.
 *
 * Frames produced by the server (see assistant.views.assistant_chat_stream):
 *   data: {"type": "chunk", "text": "..."}
 *   data: {"type": "done",  "text": "<full>"}
 *   data: {"type": "error", "detail": "..."}
 */
class ChatStreamClient {

    sealed class ChunkEvent {
        data class Chunk(val text: String) : ChunkEvent()
        data class Done(val fullText: String) : ChunkEvent()
        data class Error(val message: String) : ChunkEvent()
    }

    fun stream(message: String): Flow<ChunkEvent> = flow {
        val url = RetrofitClient.BASE_URL.trimEnd('/') + "/api/assistant/chat/stream/"
        val body = JSONObject().put("message", message).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "text/event-stream")
            .build()

        val call = RetrofitClient.rawClient().newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    emit(ChunkEvent.Error("Server error: ${response.code}"))
                    return@flow
                }
                val source = response.body?.source()
                    ?: run { emit(ChunkEvent.Error("Empty response")); return@flow }

                // SSE frame parser: collect lines, dispatch on a blank line.
                val dataLines = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isEmpty()) {
                        if (dataLines.isNotEmpty()) {
                            val frame = dataLines.toString()
                            dataLines.clear()
                            parseFrame(frame)?.let { emit(it) }
                            // No early break on Done — server closes the stream.
                        }
                        continue
                    }
                    if (line.startsWith("data:")) {
                        if (dataLines.isNotEmpty()) dataLines.append("\n")
                        dataLines.append(line.removePrefix("data:").trimStart())
                    }
                    // ignore other SSE fields (event:, id:, retry:)
                }
                // Trailing frame without blank line.
                if (dataLines.isNotEmpty()) {
                    parseFrame(dataLines.toString())?.let { emit(it) }
                }
            }
        } catch (e: Exception) {
            emit(ChunkEvent.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseFrame(raw: String): ChunkEvent? {
        if (raw.isBlank()) return null
        return try {
            val obj = JSONObject(raw)
            when (obj.optString("type")) {
                "chunk" -> ChunkEvent.Chunk(obj.optString("text", ""))
                "done" -> ChunkEvent.Done(obj.optString("text", ""))
                "error" -> ChunkEvent.Error(obj.optString("detail", "Coach error"))
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
