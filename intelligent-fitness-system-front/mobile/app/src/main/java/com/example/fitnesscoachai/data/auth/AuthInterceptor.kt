package com.example.fitnesscoachai.data.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <access>` to every outgoing request that doesn't
 * already carry one and that targets our own API.
 *
 * Public endpoints (login / register / token refresh) deliberately don't have
 * a token yet — we skip them by path.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    companion object {
        // Paths that must NOT receive the bearer header (would 401 otherwise).
        private val NO_AUTH_PATHS = listOf(
            "/api/users/login/",
            "/api/users/register/",
            "/api/users/google/",
            "/api/users/token/refresh/",
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Caller already set a header (legacy code path): don't override.
        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }

        val path = original.url.encodedPath
        if (NO_AUTH_PATHS.any { path.endsWith(it) }) {
            return chain.proceed(original)
        }

        val bearer = tokenStore.bearerHeader() ?: return chain.proceed(original)
        val authed = original.newBuilder()
            .header("Authorization", bearer)
            .build()
        return chain.proceed(authed)
    }
}
