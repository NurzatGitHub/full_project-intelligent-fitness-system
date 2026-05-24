package com.example.fitnesscoachai.data.api

import android.content.Context
import com.example.fitnesscoachai.BuildConfig
import com.example.fitnesscoachai.data.auth.AuthInterceptor
import com.example.fitnesscoachai.data.auth.TokenAuthenticator
import com.example.fitnesscoachai.data.auth.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * Where the backend lives.
     *
     *  • Debug build:
     *      - LOCAL_LAN_URL — your PC's local IP. Use this when testing on a
     *        real phone connected to the same Wi-Fi as your dev machine.
     *      - LOCAL_EMULATOR_URL — 10.0.2.2 maps to host localhost from the
     *        Android Studio emulator.
     *
     *  • Release build: Render production.
     */
    private const val LOCAL_LAN_URL = "http://192.168.0.12:8000/"
    private const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8000/"
    private const val PROD_URL = "https://intelligent-fitness-system.onrender.com/"

    /** Flip to false when running inside the Android Studio emulator. */
    private const val USE_LAN = true

    val BASE_URL: String = when {
        !BuildConfig.DEBUG -> PROD_URL
        USE_LAN -> LOCAL_LAN_URL
        else -> LOCAL_EMULATOR_URL
    }

    private lateinit var appContext: Context
    private val tokenStore: TokenStore by lazy { TokenStore.get(appContext) }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(TokenAuthenticator(tokenStore, BASE_URL))
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Must be called once from Application.onCreate before any API call.
     * Wires the OkHttp client to a context-bound TokenStore.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        // Migrate any tokens from the legacy unencrypted prefs.
        tokenStore.migrateFromLegacyIfNeeded()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    /** Exposed so screens can save tokens after login/register/google sign-in. */
    fun tokenStore(): TokenStore = tokenStore

    /** Raw OkHttp client — used by the streaming chat SSE reader. */
    fun rawClient(): OkHttpClient = client
}
