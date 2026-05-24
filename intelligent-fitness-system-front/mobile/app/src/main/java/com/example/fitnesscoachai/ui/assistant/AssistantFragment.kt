package com.example.fitnesscoachai.ui.assistant

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView

class AssistantFragment : Fragment(R.layout.fragment_assistant) {

    private val vm: AssistantViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    private var bottomNavigation: BottomNavigationView? = null
    private var historyLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val root = view
        val rv = view.findViewById<RecyclerView>(R.id.rvChat)
        val et = view.findViewById<EditText>(R.id.etMessage)
        val btn = view.findViewById<ImageButton>(R.id.btnSend)
        val progress = view.findViewById<View>(R.id.progressTyping)
        val inputContainer = view.findViewById<LinearLayout>(R.id.inputContainer)

        bottomNavigation = requireActivity().findViewById(R.id.bottomNavigation)

        adapter = ChatAdapter(requireContext())
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        val rvBaseTop = rv.paddingTop
        val rvBaseBottom = rv.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeExtraBottom = (imeInsets.bottom - navInsets.bottom).coerceAtLeast(0)

            bottomNavigation?.isGone = imeVisible
            inputContainer.translationY = -imeExtraBottom.toFloat()

            rv.updatePadding(
                left = rv.paddingLeft,
                top = rvBaseTop,
                right = rv.paddingRight,
                bottom = rvBaseBottom + if (imeVisible) imeExtraBottom else 0,
            )

            insets
        }

        vm.messages.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList()) {
                if (list.isNotEmpty()) {
                    rv.scrollToPosition(list.size - 1)
                }
            }
        }

        vm.typing.observe(viewLifecycleOwner) { isTyping ->
            progress.visibility = if (isTyping) View.VISIBLE else View.GONE
            // Keep input enabled during streaming — answer renders live.
            btn.isEnabled = !isTyping
            // EditText stays enabled so users can compose the next prompt while
            // the previous answer is still streaming.
            rv.post {
                val last = adapter.itemCount - 1
                if (last >= 0) rv.scrollToPosition(last)
            }
        }

        fun sendNow() {
            val text = et.text.toString().trim()
            if (text.isEmpty()) return

            if (RetrofitClient.tokenStore().accessToken.isNullOrBlank()) {
                // Polite signed-out hint.
                val msgs = vm.messages.value.orEmpty().toMutableList()
                msgs.add(ChatMessage(text = "You need to sign in to use FitBot.", isUser = false))
                adapter.submitList(msgs)
                return
            }

            et.setText("")
            // bearer arg ignored — AuthInterceptor injects it. Keep API for now.
            vm.sendUserMessage(text, "")
        }

        btn.setOnClickListener { sendNow() }

        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendNow(); true
            } else false
        }

        et.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                rv.post {
                    val last = adapter.itemCount - 1
                    if (last >= 0) rv.scrollToPosition(last)
                }
            }
        }

        root.post { ViewCompat.requestApplyInsets(root) }
    }

    override fun onResume() {
        super.onResume()
        // Lazy-load history once per fragment lifecycle. Reset on logout
        // happens elsewhere by recreating the fragment.
        if (!historyLoaded && !RetrofitClient.tokenStore().accessToken.isNullOrBlank()) {
            historyLoaded = true
            vm.loadHistory()
        }
    }

    override fun onDestroyView() {
        bottomNavigation?.isGone = false
        bottomNavigation = null
        super.onDestroyView()
    }
}
