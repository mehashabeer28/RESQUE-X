package com.resquex.app.data

import android.content.Context
import android.location.Location


class HospitalRepository(private val context: Context) {

    fun getHospitals(): List<Hospital> {

        val hospitalList = mutableListOf<Hospital>()

        try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            val fileName = if ("hospitals.csv" in assetFiles) "hospitals.csv" else "hospital.csv"
            val inputStream = context.assets.open(fileName)

            val reader = inputStream.bufferedReader()

            reader.readLine() // Skip column headings

            reader.forEachLine { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    val parts = trimmedLine.split(",")
                    if (parts.size == 5) {
                        try {
                            val hospital = Hospital(
                                name = parts[0].trim(),
                                district = parts[1].trim(),
                                address = parts[2].trim(),
                                latitude = parts[3].trim().toDouble(),
                                longitude = parts[4].trim().toDouble()
                            )
                            hospitalList.add(hospital)
                        } catch (nfe: NumberFormatException) {
                            nfe.printStackTrace()
                        }
                    }
                }
            }

            reader.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return hospitalList
    }
    fun getNearbyHospitals(
        userLatitude: Double,
        userLongitude: Double
    ): List<Hospital> {

        return getHospitals().sortedBy { hospital ->

            val results = FloatArray(1)

            Location.distanceBetween(
                userLatitude,
                userLongitude,
                hospital.latitude,
                hospital.longitude,
                results
            )

            results[0]
        }
    }
}