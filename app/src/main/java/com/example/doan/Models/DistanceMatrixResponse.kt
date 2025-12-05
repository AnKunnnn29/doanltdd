package com.example.doan.Models

data class DistanceMatrixResponse(
    val rows: List<Row>,
    val status: String
)

data class Row(
    val elements: List<Element>
)

data class Element(
    val distance: DistanceInfo,
    val duration: DurationInfo,
    val status: String
)

data class DistanceInfo(
    val text: String,
    val value: Int // Distance in meters
)

data class DurationInfo(
    val text: String,
    val value: Int // Duration in seconds
)
