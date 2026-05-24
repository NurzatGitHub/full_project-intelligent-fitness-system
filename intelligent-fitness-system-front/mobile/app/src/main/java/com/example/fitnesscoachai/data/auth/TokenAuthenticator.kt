package com.example.fitnesscoachai.data.auth

import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.io.IOException

/**
 * OkHttp Authenticator that kicks in **after** a 401.
 *
 * Flow:
 *   1. Original request fails with 401 (access token expired).
 *   2. We POST refresh_token to /api/users/token/refresh/.
 *   3. On success we save the new access token and retry the original request
 *      once with the new Authorization header.
 *   4. On failure we clear tokens (forces the user to log in again).
 *
 * Thread safety: OkHttp guarantees that Authenticator.authenticate is called
 * on a single thread for a given Route, but multiple Routes can hit it in
 * parallel. We synchronize on TokenStore to avoid two parallel refreshes.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshBaseUrl: String,
) : Authenticator {

    companion object {
        private const val MAX_RETRIES = 1
    }

    private val refreshLock = Any()

    // Bare-bones client just for the refresh call — must NOT itself install
    // this authenticator (would recurse forever).
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRIES + 1) {
            // Already retried, still 401 — give up.
            return null
        }

        val tokenBefore = tokenStore.accessToken

        synchronized(refreshLock) {
            // Another thread may have refreshed while we waited on the lock.
            val tokenNow = tokenStore.accessToken
            if (tokenNow != null && tokenNow != tokenBefore) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $tokenNow")
                    .build()
            }

            val refresh = tokenStore.refreshToken ?: return null
            val newAccess = tryRefresh(refresh) ?: run {
                tokenStore.clear()
                return null
            }
            tokenStore.saveAccessToken(newAccess)

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        }
    }

    private fun tryRefresh(refreshToken: String): String? {
        return try {
            val body = JSONObject().put("refresh", refreshToken).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(refreshBaseUrl.trimEnd('/') + "/api/users/token/refresh/")
                .post(body)
                .build()
            refreshClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return null
                JSONObject(raw).optString("access").takeIf { it.isNotBlank() }
            }
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
