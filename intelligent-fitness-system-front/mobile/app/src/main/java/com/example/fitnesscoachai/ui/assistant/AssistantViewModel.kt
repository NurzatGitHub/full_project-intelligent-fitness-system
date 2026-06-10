package com.example.fitnesscoachai.ui.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesscoachai.data.api.ChatStreamClient
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.ChatRequest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class AssistantViewModel : ViewModel() {

    private val api = RetrofitClient.apiService
    private val streamClient = ChatStreamClient()

    companion object {
        // Initial greeting shown before we load history from the server.
        private val GREETING = ChatMessage(
            id = "greeting",
            text = "Hi! I'm FitBot, your AI training coach. " +
                "Ask me anything about form, rest days, or your plan.",
            isUser = false,
        )
    }

    private val _messages = MutableLiveData<List<ChatMessage>>(listOf(GREETING))
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _typing = MutableLiveData(false)
    val typing: LiveData<Boolean> = _typing

    /**
     * Pull the user's saved history from /api/assistant/chat/messages/ and
     * replace the current list. The greeting only stays if the user has no
     * history at all.
     */
    fun loadHistory() {
        viewModelScope.launch {
            try {
                val resp = api.assistantChatHistory(limit = 200)
                if (!resp.isSuccessful) return@launch
                val items = resp.body()?.messages.orEmpty()
                if (items.isEmpty()) {
                    _messages.value = listOf(GREETING)
                } else {
                    _messages.value = items.map {
                        ChatMessage(
                            id = it.id.toString(),
                            text = it.content,
                            isUser = it.role == "user",
                        )
                    }
                }
            } catch (_: Exception) {
                // keep whatever we have locally
            }
        }
    }

    /**
     * Streaming chat. Adds the user's bubble immediately, then opens an SSE
     * stream and grows the assistant bubble token-by-token. Falls back to the
     * non-streaming endpoint if the stream errors out.
     */
    fun sendUserMessage(text: String, @Suppress("UNUSED_PARAMETER") accessToken: String) {
        if (text.isBlank()) return

        val current = _messages.value.orEmpty().toMutableList()
        current.add(ChatMessage(text = text, isUser = true))

        // The assistant bubble is added empty and grown live.
        val streamingId = UUID.randomUUID().toString()
        current.add(
            ChatMessage(
                id = streamingId,
                text = "",
                isUser = false,
                isStreaming = true,
            )
        )
        _messages.value = current
        _typing.value = true

        viewModelScope.launch {
            val buffer = StringBuilder()
            var sawAnyChunk = false
            var streamFailed = false
            try {
                streamClient.stream(text).collect { event ->
                    when (event) {
                        is ChatStreamClient.ChunkEvent.Chunk -> {
                            sawAnyChunk = true
                            buffer.append(event.text)
                            replaceStreamingMessage(streamingId, buffer.toString(), done = false)
                        }
                        is ChatStreamClient.ChunkEvent.Done -> {
                            val finalText = event.fullText.ifBlank { buffer.toString() }
                            replaceStreamingMessage(streamingId, finalText, done = true)
                            _typing.value = false
                        }
                        is ChatStreamClient.ChunkEvent.Error -> {
                            streamFailed = !sawAnyChunk
                            if (!sawAnyChunk) {
                                // No tokens at all — try the non-streaming endpoint
                                // as a fallback (handled below).
                            } else {
                                replaceStreamingMessage(
                                    streamingId,
                                    buffer.toString() + "\n\n_(stream interrupted: ${event.message})_",
                                    done = true,
                                )
                                _typing.value = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                streamFailed = !sawAnyChunk
            }

            if (streamFailed) {
                fallbackToBlockingChat(text, streamingId)
            } else if (_typing.value == true) {
                // Stream ended without an explicit Done frame; flush whatever we have.
                replaceStreamingMessage(
                    streamingId,
                    buffer.toString().ifBlank { "I couldn't generate a response. Please try again." },
                    done = true,
                )
                _typing.value = false
            }
        }
    }

    private suspend fun fallbackToBlockingChat(message: String, bubbleId: String) {
        try {
            val resp = api.assistantChat(ChatRequest(message = message))
            if (resp.isSuccessful) {
                val reply = resp.body()?.reply?.trim().orEmpty()
                replaceStreamingMessage(
                    bubbleId,
                    reply.ifBlank { "Empty response from coach." },
                    done = true,
                )
            } else {
                val raw = resp.errorBody()?.string().orEmpty()
                val detail = runCatching { JSONObject(raw).optString("detail") }.getOrNull()
                replaceStreamingMessage(
                    bubbleId,
                    detail?.takeIf { it.isNotBlank() } ?: "Server error: ${resp.code()}",
                    done = true,
                )
            }
        } catch (e: Exception) {
            replaceStreamingMessage(
                bubbleId,
                "Could not reach the coach. Check your connection and try again.",
                done = true,
            )
        } finally {
            _typing.value = false
        }
    }

    private fun replaceStreamingMessage(id: String, newText: String, done: Boolean) {
        val list = _messages.value.orEmpty()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val updated = list.toMutableList()
        updated[idx] = updated[idx].copy(text = newText, isStreaming = !done)
        _messages.value = updated
    }

    /**
     * Wipe local + server history.
     */
    fun resetConversation() {
        viewModelScope.launch {
            runCatching { api.assistantChatClear() }
            _messages.value = listOf(GREETING)
            _typing.value = false
        }
    }

    /**
     * Clear in-memory chat WITHOUT touching the server. Used when we detect
     * that the active user changed (logout/login) since the last time history
     * was loaded — we don't want the new user to briefly see the old user's
     * messages before the network reload finishes.
     */
    fun clearInMemory() {
        _messages.value = listOf(GREETING)
        _typing.value = false
    }
}
