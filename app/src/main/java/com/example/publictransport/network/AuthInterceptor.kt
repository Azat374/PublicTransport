// AuthInterceptor.kt
package com.example.publictransport.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import com.example.publictransport.R

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenProvider.getTokenOrThrow()
        val newReq = chain.request()
            .newBuilder()
            .header("X-Auth-Token", token)
            // Если нужен тот же заголовок X-Visitor-Id и Referer, можно тоже их тут прописать:
            .header("X-Visitor-Id", "1333796405bf72754589e09938b54f64")
            .header("Referer", "https://citybus.tha.kz/")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(newReq)
    }
}
