package com.example.fitnesscoachai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.legal.TermsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpStep1Activity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var cbTerms: MaterialCheckBox
    private lateinit var tvTermsLabel: TextView
    private lateinit var btnNext: MaterialButton

    private var attempted = false
    private var emailTouched = false
    private var passTouched = false
    private var confirmTouched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step1)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        cbTerms = findViewById(R.id.cbTerms)
        tvTermsLabel = findViewById(R.id.tvTermsLabel)
        btnNext = findViewById(R.id.btnNext)

        setupTermsLabel()
        setupUi()
        updateButtonEnabled()
    }

    /**
     * Build a "I agree to the Terms and Privacy Policy" string where only
     * "Terms and Privacy Policy" is clickable and underlined. Tap opens
     * TermsActivity in a new screen (with system back to return).
     */
    private fun setupTermsLabel() {
        val prefix = getString(R.string.signup_terms_full)         // "I agree to the "
        val linkText = getString(R.string.signup_terms_clickable)  // "Terms and Privacy Policy"

        val builder = SpannableStringBuilder(prefix)
        val linkSpannable = SpannableString(linkText)

        val clickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@SignUpStep1Activity, TermsActivity::class.java))
            }
        }
        linkSpannable.setSpan(clickable, 0, linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        linkSpannable.setSpan(UnderlineSpan(), 0, linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        builder.append(linkSpannable)

        tvTermsLabel.text = builder
        tvTermsLabel.movementMethod = LinkMovementMethod.getInstance()
        // Tap on the label area (outside the link) toggles the checkbox.
        tvTermsLabel.setOnClickListener { cbTerms.isChecked = !cbTerms.isChecked }
    }

    private fun setupUi() {
        btnNext.isEnabled = false

        val watcher = SimpleTextWatcher {
            updateButtonEnabled()
            validate(showErrors = attempted)
        }

        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        etConfirmPassword.addTextChangedListener(watcher)

        cbTerms.setOnCheckedChangeListener { _, _ ->
            updateButtonEnabled()
            if (attempted) validate(showErrors = true)
        }

        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                emailTouched = true
                validate(showErrors = true)
            }
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                passTouched = true
                validate(showErrors = true)
            }
        }

        etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                confirmTouched = true
                validate(showErrors = true)
            }
        }

        etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnNext.performClick()
                true
            } else false
        }

        btnNext.setOnClickListener {
            attempted = true
            emailTouched = true
            passTouched = true
            confirmTouched = true

            val ok = validate(showErrors = true)
            if (!ok) return@setOnClickListener

            val email = etEmail.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString()?.trim().orEmpty()

            val intent = Intent(this, com.example.fitnesscoachai.ui.auth.onboarding.SignUpAgeActivity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("password", password)
            startActivity(intent)


        }
    }

    private fun updateButtonEnabled() {
        btnNext.isEnabled = isFormValid()
    }

    private fun isFormValid(): Boolean {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()
        val confirm = etConfirmPassword.text?.toString()?.trim().orEmpty()

        val emailOk = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val passOk = pass.length >= 8
        val confirmOk = confirm.isNotBlank() && confirm == pass
        val termsOk = cbTerms.isChecked

        return emailOk && passOk && confirmOk && termsOk
    }

    private fun validate(showErrors: Boolean): Boolean {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()
        val confirm = etConfirmPassword.text?.toString()?.trim().orEmpty()

        // сброс
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        var ok = true

        val showEmailErr = showErrors && (emailTouched || attempted)
        val showPassErr = showErrors && (passTouched || attempted)
        val showConfirmErr = showErrors && (confirmTouched || attempted)

        if (showEmailErr) {
            tilEmail.error = when {
                email.isBlank() -> "Email is required"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email"
                else -> null
            }
            if (tilEmail.error != null) ok = false
        } else {
            // даже если не показываем, всё равно учитываем валидность
            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) ok = false
        }

        if (showPassErr) {
            tilPassword.error = when {
                pass.isBlank() -> "Password is required"
                pass.length < 8 -> "Password must be at least 8 characters"
                else -> null
            }
            if (tilPassword.error != null) ok = false
        } else {
            if (pass.length < 8) ok = false
        }

        if (showConfirmErr) {
            tilConfirmPassword.error = when {
                confirm.isBlank() -> "Please confirm your password"
                confirm != pass -> "Passwords do not match"
                else -> null
            }
            if (tilConfirmPassword.error != null) ok = false
        } else {
            if (confirm.isBlank() || confirm != pass) ok = false
        }

        if (!cbTerms.isChecked) {
            ok = false
            if (showErrors && attempted) {
                Toast.makeText(this, "Please agree to the Terms and Privacy Policy", Toast.LENGTH_SHORT).show()
            }
        }

        updateButtonEnabled()
        return ok
    }
}
