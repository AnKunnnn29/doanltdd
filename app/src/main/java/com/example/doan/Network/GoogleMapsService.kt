package com.example.doan.Network

import com.example.doan.Models.DistanceMatrixResponse
import com.example.doan.Models.GeocodingResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsService {
    @GET("distancematrix/json")
    fun getDistance(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("key") key: String
    ): Call<DistanceMatrixResponse>
    
    @GET("geocode/json")
    fun geocodeAddress(
        @Query("address") address: String,
        @Query("key") key: String,
        @Query("region") region: String = "vn",
        @Query("language") language: String = "vi"
    ): Call<GeocodingResponse>
}
