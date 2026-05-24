package com.example.fitnesscoachai.ui.assistant

import java.util.UUID

/**
 * Single message in the FitBot chat UI.
 *
 * `id` is used by DiffUtil so the list adapter can do animated insertions /
 * smart updates instead of rebuilding the whole list. We also use it to
 * locate the streaming bubble while it's being filled in token-by-token.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timeMillis: Long = System.currentTimeMillis(),
    /** While Gemini is still streaming, the bubble shows a subtle cursor. */
    val isStreaming: Boolean = false,
)
