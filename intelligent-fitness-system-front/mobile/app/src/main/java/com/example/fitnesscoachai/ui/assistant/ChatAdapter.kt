package com.example.fitnesscoachai.ui.assistant

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat list adapter built on ListAdapter + DiffUtil:
 *   - inserting a new message only animates that row (no full rebuild),
 *   - updating the streaming bubble's text only invalidates that row,
 *   - scroll position and view state survive between updates.
 *
 * Markwon renders proper Markdown (bold / italic / lists / links / code)
 * into a real Spanned, replacing the old regex hack that only knew **bold**
 * and `* bullet`.
 */
class ChatAdapter(context: Context) :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(o: ChatMessage, n: ChatMessage) = o.id == n.id
            override fun areContentsTheSame(o: ChatMessage, n: ChatMessage) = o == n
        }
    }

    private val markwon: Markwon = Markwon.builder(context.applicationContext)
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .build()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Legacy API used by AssistantViewModel — accepts a plain list and forwards
     * to ListAdapter.submitList. Keeps existing call sites working.
     */
    fun submitListCompat(items: List<ChatMessage>) {
        // ListAdapter wants a new list reference each time.
        submitList(items.toList())
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isUser) TYPE_USER else TYPE_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserVH(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            AiVH(inflater.inflate(R.layout.item_message_ai, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        val timeStr = timeFormat.format(Date(msg.timeMillis))

        when (holder) {
            is UserVH -> {
                holder.tvText.text = msg.text
                holder.tvTime.text = timeStr
            }
            is AiVH -> {
                val display = when {
                    msg.isStreaming && msg.text.isBlank() -> "…"
                    msg.isStreaming -> "${msg.text} ▍"  // tiny cursor while streaming
                    else -> msg.text
                }
                markwon.setMarkdown(holder.tvText, display)
                holder.tvTime.text = timeStr
            }
        }
    }

    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    class AiVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }
}
