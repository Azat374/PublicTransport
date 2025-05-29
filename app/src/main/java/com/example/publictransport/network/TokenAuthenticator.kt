// TokenAuthenticator.kt
package com.example.publictransport.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        return try {
            val current = TokenProvider.getTokenOrThrow()
            // если это тот же токен, значит обновляться бесполезно
            if (response.request.header("X-Auth-Token") == current) return null

            val newToken = TokenProvider.updateToken(current)
            response.request.newBuilder()
                .header("X-Auth-Token", newToken.toString())
                .build()
        } catch (e: Exception) {
            // при любых ошибках с токеном – не крашить, просто не перепылять запрос
            null
        }
    }
}
