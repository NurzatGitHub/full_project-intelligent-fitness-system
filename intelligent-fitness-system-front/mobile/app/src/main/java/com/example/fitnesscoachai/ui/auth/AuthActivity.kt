package com.example.fitnesscoachai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.models.AuthResponse
import com.example.fitnesscoachai.ui.auth.onboarding.SignUpAgeActivity
import com.example.fitnesscoachai.ui.home.HomeFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var btnLogin: MaterialButton
    private lateinit var btnContinueAsGuest: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvForgotPassword: TextView

    private val viewModel: AuthViewModel by viewModels()

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.result
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, "Google token not found", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            viewModel.loginWithGoogle(idToken)
        }.onFailure { e ->
            val msg = if (e is ApiException) {
                "Google sign-in failed: code=${e.statusCode}, message=${e.message}"
            } else {
                "Google sign-in failed: ${e.message}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            btnGoogle.isEnabled = true
            btnLogin.isEnabled = isFormValid()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        btnLogin = findViewById(R.id.btnLogin)
        btnContinueAsGuest = findViewById(R.id.btnContinueAsGuest)
        btnGoogle = findViewById(R.id.btnGoogle)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.isEnabled = false

        setupValidation()
        observeState()

        btnLogin.setOnClickListener { submitLogin() }
        btnGoogle.setOnClickListener { startGoogleSignIn() }

        btnContinueAsGuest.setOnClickListener {
            HomeFragment.clearCache()

            // CRITICAL: also wipe the encrypted TokenStore. Otherwise the
            // previous logged-in user's JWT survives, and ProfileFragment
            // fetches THEIR profile from the server while the UI thinks
            // we're a guest. That's how "Azamat" leaked into guest mode.
            com.example.fitnesscoachai.data.api.RetrofitClient
                .tokenStore()
                .clear()

            // NOTE: we no longer wipe `user_profile` or `workout_history`
            // prefs here, because those are already user-scoped (keys like
            // age_<userId>, history_count_<userId>, avatar_uri_<userId>).
            // A guest reads guest-scoped keys, a logged-in user reads their
            // own. Wiping them used to delete avatars on re-login, which is
            // the actual bug we hit on the device.

            getSharedPreferences("auth", MODE_PRIVATE).edit()
                .putBoolean("isLoggedIn", true)
                .putBoolean("isGuest", true)
                .remove("user_id")
                .remove("user_name")
                .remove("user_email")
                .remove("access_token")
                .remove("refresh_token")
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignUpStep1Activity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot password: coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        val client = GoogleSignIn.getClient(this, gso)

        client.signOut().addOnCompleteListener {
            googleLauncher.launch(client.signInIntent)
        }
    }

    private fun setupValidation() {
        val watcher = SimpleTextWatcher {
            tilEmail.error = null
            tilPassword.error = null
            btnLogin.isEnabled = isFormValid()
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
    }

    private fun isFormValid(): Boolean {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && pass.isNotBlank()
    }

    private fun submitLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()

        var ok = true
        tilEmail.error = null
        tilPassword.error = null

        if (email.isBlank()) {
            tilEmail.error = "Email is required"
            ok = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            ok = false
        }

        if (pass.isBlank()) {
            tilPassword.error = "Password is required"
            ok = false
        }

        if (!ok) return
        viewModel.login(email, pass)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is AuthViewModel.LoginState.Idle -> {
                            btnLogin.isEnabled = isFormValid()
                            btnGoogle.isEnabled = true
                            btnLogin.text = "Log In"
                        }

                        is AuthViewModel.LoginState.Loading -> {
                            btnLogin.isEnabled = false
                            btnGoogle.isEnabled = false
                            btnLogin.text = "Loading..."
                        }

                        is AuthViewModel.LoginState.Success -> {
                            btnGoogle.isEnabled = true
                            btnLogin.text = "Log In"

                            val auth = state.authResponse
                            saveAuthData(auth)
                            HomeFragment.clearCache()

                            if (needsOnboarding(auth)) {
                                startActivity(
                                    Intent(this@AuthActivity, SignUpAgeActivity::class.java).apply {
                                        putExtra("email", auth.user.email)
                                        putExtra("from_google", true)
                                    }
                                )
                            } else {
                                startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                finish()
                            }
                        }

                        is AuthViewModel.LoginState.Error -> {
                            btnGoogle.isEnabled = true
                            btnLogin.text = "Log In"
                            btnLogin.isEnabled = isFormValid()
                            Toast.makeText(this@AuthActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun needsOnboarding(auth: AuthResponse): Boolean {
        val u = auth.user

        return auth.is_new_user ||
                u.age == null ||
                u.height == null ||
                u.weight == null ||
                u.fitness_level.isBlank() ||
                u.goal.isBlank() ||
                u.frequency.isBlank() ||
                u.workout_duration.isBlank() ||
                u.workout_place.isBlank() ||
                u.endurance_level.isBlank() ||
                u.gender.isBlank()
    }

    private fun saveAuthData(authResponse: AuthResponse) {
        // Secret tokens go to EncryptedSharedPreferences via TokenStore.
        com.example.fitnesscoachai.data.api.RetrofitClient
            .tokenStore()
            .saveTokens(access = authResponse.access, refresh = authResponse.refresh)

        // Non-secret session flags / profile crumbs stay in the regular prefs
        // because lots of existing screens already read them.
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putBoolean("isLoggedIn", true)
            .putBoolean("isGuest", false)
            .putInt("user_id", authResponse.user.id)
            .putString("user_name", authResponse.user.username)
            .putString("user_email", authResponse.user.email)
            // Wipe any stale plaintext tokens left from older builds.
            .remove("access_token")
            .remove("refresh_token")
            .apply()
    }
}