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

class SignUpWeightActivity : AppCompatActivity() {

    private lateinit var tilWeight: TextInputLayout
    private lateinit var etWeight: TextInputEditText
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var attempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_weight)

        tilWeight = findViewById(R.id.tilWeight)
        etWeight = findViewById(R.id.etWeight)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(33, true)

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()
        val age = intent.getIntExtra("age", -1)
        val height = intent.getIntExtra("height_cm", -1)
        val fromGoogle = intent.getBooleanExtra("from_google", false)

        btnNext.isEnabled = false

        etWeight.addTextChangedListener(SimpleTextWatcher {
            btnNext.isEnabled = isValidWeight(safeInt(etWeight.text?.toString()))
            if (attempted) validate(showErrors = true)
        })

        etWeight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnNext.performClick()
                true
            } else false
        }

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            attempted = true
            val w = safeInt(etWeight.text?.toString())
            if (!validate(showErrors = true)) return@setOnClickListener

            val intent = Intent(this, SignUpGenderActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)
                putExtra("age", age)
                putExtra("height_cm", height)
                putExtra("weight_kg", w.toFloat())
                putExtra("from_google", fromGoogle)
            }
            startActivity(intent)
        }
    }

    private fun validate(showErrors: Boolean): Boolean {
        val w = safeInt(etWeight.text?.toString())

        tilWeight.error = null
        val ok = isValidWeight(w)

        if (!ok && showErrors) {
            tilWeight.error = "Enter weight in kg (30–250)"
            Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show()
        }

        btnNext.isEnabled = ok
        return ok
    }

    private fun isValidWeight(w: Int): Boolean = w in 30..250
    private fun safeInt(s: String?): Int = s?.trim()?.toIntOrNull() ?: -1
}