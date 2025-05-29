// RetrofitInstance.kt
package com.example.publictransport.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(AuthInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl("https://cdu-rest-api.tha.kz/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TransportApi::class.java)
    }
}
