package com.resquex.app.routing

import org.osmdroid.util.GeoPoint

data class RouteStep(
    val instruction: String,
    val distanceKm: Double
)

data class EmergencyRoute(
    val destinationName: String,
    val destinationDistrict: String,
    val points: List<GeoPoint>,
    val totalDistanceKm: Double,
    val estimatedMinutes: Int,
    val steps: List<RouteStep>
)
