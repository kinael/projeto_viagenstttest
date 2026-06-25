package com.example.projeto_viagens.data.location

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val city: String?,
    val state: String?,
    val country: String?
)