package com.example.publictransport.yandex

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Singleton для Retrofit
object YandexRetrofitInstance {
    private const val BASE_URL = "https://api.routing.yandex.net/"

    val api: YandexRoutesApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YandexRoutesApi::class.java)
    }
}