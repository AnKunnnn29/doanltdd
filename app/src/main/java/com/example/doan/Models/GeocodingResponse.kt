package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String,
    @SerializedName("error_message") val errorMessage: String? = null
)

data class GeocodingResult(
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)
