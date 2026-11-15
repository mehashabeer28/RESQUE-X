package com.resquex.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class HospitalDatasetTest {

    private val keralaDistricts = listOf(
        "Thiruvananthapuram",
        "Kollam",
        "Pathanamthitta",
        "Alappuzha",
        "Kottayam",
        "Idukki",
        "Ernakulam",
        "Thrissur",
        "Palakkad",
        "Malappuram",
        "Kozhikode",
        "Wayanad",
        "Kannur",
        "Kasaragod"
    )

    @Test
    fun testHospitalDatasetCoverage() {
        val file = listOf(
            File("src/main/assets/hospitals.csv"),
            File("app/src/main/assets/hospitals.csv"),
            File("assets/hospitals.csv")
        ).firstOrNull { it.exists() }

        assertNotNull("hospitals.csv asset file must exist", file)

        val lines = file!!.readLines().filter { it.isNotBlank() }
        assertTrue("Dataset must contain records", lines.size > 1)

        val header = lines.first()
        assertEquals("name,district,address,latitude,longitude", header)

        val records = lines.drop(1)
        val districtsFound = mutableSetOf<String>()

        records.forEachIndexed { index, line ->
            val parts = line.split(",")
            assertEquals(
                "Row ${index + 2} must have exactly 5 comma-separated values: $line",
                5,
                parts.size
            )

            val name = parts[0].trim()
            val district = parts[1].trim()
            val address = parts[2].trim()
            val lat = parts[3].trim().toDoubleOrNull()
            val lng = parts[4].trim().toDoubleOrNull()

            assertTrue("Hospital name must not be empty at row ${index + 2}", name.isNotEmpty())
            assertTrue("District must not be empty at row ${index + 2}", district.isNotEmpty())
            assertTrue("Address must not be empty at row ${index + 2}", address.isNotEmpty())
            assertNotNull("Latitude must be a valid number at row ${index + 2}", lat)
            assertNotNull("Longitude must be a valid number at row ${index + 2}", lng)

            // Kerala coordinates range check
            assertTrue("Latitude must be within Kerala bounds (8.0 to 13.0) for $name", lat!! in 8.0..13.0)
            assertTrue("Longitude must be within Kerala bounds (74.0 to 78.0) for $name", lng!! in 74.0..78.0)

            districtsFound.add(district)
        }

        // Verify all 14 districts are present
        for (requiredDistrict in keralaDistricts) {
            assertTrue(
                "District '$requiredDistrict' must be represented in hospital dataset",
                districtsFound.contains(requiredDistrict)
            )
        }

        assertEquals(
            "All 14 districts of Kerala must have hospitals represented",
            14,
            districtsFound.size
        )
    }
}
