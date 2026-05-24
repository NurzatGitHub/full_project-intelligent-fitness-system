package com.example.fitnesscoachai.ui.legal

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.fitnesscoachai.R

/**
 * Displays the static Terms of Use & Privacy Policy text. Reachable from the
 * clickable "Terms and Privacy Policy" link on the signup screen.
 *
 * The body is stored in strings.xml as a CDATA block with simple HTML tags
 * (<b>, <br>) so we can keep formatting without bundling a WebView.
 */
class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        findViewById<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener { finish() }

        val body = findViewById<TextView>(R.id.tvTermsBody)
        val raw = getString(R.string.terms_body)
        body.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(raw)
        }
    }
}
