package com.resquex.app

import com.resquex.app.data.Hospital
import com.resquex.app.routing.EmergencyRoadGraph
import org.junit.Assert.*
import org.junit.Test

class EmergencyRoutingTest {

    @Test
    fun testOfflineRouteCalculationToHospital() {
        val graph = EmergencyRoadGraph()

        // Test hospital in Thrissur
        val hospital = Hospital(
            name = "Government Medical College Thrissur",
            district = "Thrissur",
            address = "Mulamkunnathukavu Thrissur 680596",
            latitude = 10.6558,
            longitude = 76.2251
        )

        // Starting from Kochi (Edappally)
        val startLat = 10.0260
        val startLng = 76.3125

        val route = graph.findEmergencyRoute(startLat, startLng, hospital)

        assertNotNull("Emergency route must be generated", route)
        assertEquals("Government Medical College Thrissur", route.destinationName)
        assertEquals("Thrissur", route.destinationDistrict)
        assertTrue("Route must contain coordinate points", route.points.size >= 3)
        assertTrue("Total distance must be positive (> 20 km)", route.totalDistanceKm > 20.0)
        assertTrue("Estimated minutes must be positive", route.estimatedMinutes > 10)
        assertTrue("Guidance steps must be generated", route.steps.isNotEmpty())

        // Verify start and destination step instructions
        val firstStep = route.steps.first()
        val lastStep = route.steps.last()
        assertTrue(firstStep.instruction.contains("Depart", ignoreCase = true))
        assertTrue(lastStep.instruction.contains(hospital.name, ignoreCase = true))
    }

    @Test
    fun testLongDistanceCorridorRoute() {
        val graph = EmergencyRoadGraph()

        // Starting from Thiruvananthapuram to Kozhikode Medical College
        val kozhikodeGMC = Hospital(
            name = "Government Medical College Kozhikode",
            district = "Kozhikode",
            address = "Medical College Road Kozhikode 673008",
            latitude = 11.2677,
            longitude = 75.8361
        )

        val startLat = 8.5204
        val startLng = 76.9157

        val route = graph.findEmergencyRoute(startLat, startLng, kozhikodeGMC)

        assertNotNull(route)
        assertTrue("Distance from TVM to Calicut should be > 300 km", route.totalDistanceKm > 300.0)
        assertTrue("Multiple route steps along NH corridors must exist", route.steps.size >= 5)
    }
}
