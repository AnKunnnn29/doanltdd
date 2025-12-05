package com.example.doan.Network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClientMaps {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"

    val instance: GoogleMapsService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(GoogleMapsService::class.java)
    }
}
