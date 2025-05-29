// TokenProvider.kt
package com.example.publictransport.network

import java.util.concurrent.atomic.AtomicReference

object TokenProvider {
    private val tokenRef = AtomicReference<String?>(null)
    private val expiresAtRef = AtomicReference(0L)

    /** Внутренний setter, будем вызывать из HiddenTokenFetcher */
    fun updateToken(newToken: String, ttlMs: Long = 5 * 60 * 1000L) {
        tokenRef.set(newToken)
        expiresAtRef.set(System.currentTimeMillis() + ttlMs)
    }

    /** Возвращает последний известный токен; если нет или истёк — бросает */
    fun getTokenOrThrow(): String {
        val tok = tokenRef.get()
        if (tok == null || System.currentTimeMillis() >= expiresAtRef.get()) {
            throw IllegalStateException("Token is missing or expired")
        }
        return tok
    }
}
