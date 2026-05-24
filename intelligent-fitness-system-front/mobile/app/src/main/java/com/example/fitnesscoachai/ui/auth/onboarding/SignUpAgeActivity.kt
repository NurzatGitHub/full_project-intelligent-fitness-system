package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.auth.SimpleTextWatcher
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpAgeActivity : AppCompatActivity() {

    private lateinit var tilAge: TextInputLayout
    private lateinit var etAge: TextInputEditText
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_age)

        // progress (2/8 = 25)
        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(17, true)
        tilAge = findViewById(R.id.tilAge)
        etAge = findViewById(R.id.etAge)
        btnNext = findViewById(R.id.btnNext)
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()

        fun validate(): Boolean {
            tilAge.error = null
            val ageStr = etAge.text?.toString()?.trim().orEmpty()
            val age = ageStr.toIntOrNull()

            val ok = age != null && age in 10..80
            if (!ok && ageStr.isNotEmpty()) tilAge.error = "Enter age 10-80"
            btnNext.isEnabled = ok
            return ok
        }

        etAge.addTextChangedListener(SimpleTextWatcher { validate() })

        etAge.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validate()) btnNext.performClick()
                true
            } else false
        }

        btnNext.setOnClickListener {
            if (!validate()) return@setOnClickListener

            val age = etAge.text!!.toString().trim().toInt()
            val fromGoogle = intent.getBooleanExtra("from_google", false)

            // next screen: Height
            val i = Intent(this, SignUpHeightActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)
                putExtra("age", age)
                putExtra("from_google", fromGoogle)
            }
            startActivity(i)
        }
    }
}
