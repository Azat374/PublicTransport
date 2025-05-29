package com.example.publictransport.model

data class Stop(
    val id: Long,
    val name: LocalizedName,
    val point: List<Double>,
    val routes: List<Long>,
    val updatedAt: String
)
