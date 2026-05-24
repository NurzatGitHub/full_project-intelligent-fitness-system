package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.auth.SimpleTextWatcher
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpHeightActivity : AppCompatActivity() {

    private lateinit var tilHeight: TextInputLayout
    private lateinit var etHeight: TextInputEditText
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var progressSteps: LinearProgressIndicator

    private var attempted = false

    // Настрой под свой flow
    private val step = 3
    private val totalSteps = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_height)

        tilHeight = findViewById(R.id.tilHeight)
        etHeight = findViewById(R.id.etHeight)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        progressSteps = findViewById(R.id.progressSteps)

        // Прогресс выставляем тут (вместо app:progress в XML)
        progressSteps.max = 100
        progressSteps.setProgressCompat(calcPercent(step, totalSteps), true)

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()
        val age = intent.getIntExtra("age", -1)

        btnNext.isEnabled = false

        etHeight.addTextChangedListener(SimpleTextWatcher {
            btnNext.isEnabled = isValidHeight(safeInt(etHeight.text?.toString()))
            if (attempted) validate(showErrors = true)
        })

        etHeight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnNext.performClick()
                true
            } else false
        }

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            attempted = true
            val h = safeInt(etHeight.text?.toString())
            val fromGoogle = intent.getBooleanExtra("from_google", false)
            if (!validate(showErrors = true)) return@setOnClickListener

            val i = Intent(this, SignUpWeightActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)
                putExtra("age", age)
                putExtra("height_cm", h)
                putExtra("from_google", fromGoogle)
            }
            startActivity(i)
        }
    }

    private fun validate(showErrors: Boolean): Boolean {
        val h = safeInt(etHeight.text?.toString())

        tilHeight.error = null

        val ok = isValidHeight(h)
        if (!ok && showErrors) {
            tilHeight.error = "Enter height in cm (120–230)"
            Toast.makeText(this, "Please enter a valid height", Toast.LENGTH_SHORT).show()
        }
        btnNext.isEnabled = ok
        return ok
    }

    private fun isValidHeight(h: Int): Boolean = h in 120..230

    private fun safeInt(s: String?): Int = s?.trim()?.toIntOrNull() ?: -1

    private fun calcPercent(step: Int, total: Int): Int {
        if (total <= 1) return 0
        val s = step.coerceIn(1, total)
        return ((s - 1) * 100) / (total - 1)
    }
}
