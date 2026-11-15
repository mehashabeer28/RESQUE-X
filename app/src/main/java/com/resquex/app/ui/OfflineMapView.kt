package com.resquex.app.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.resquex.app.data.Hospital
import com.resquex.app.routing.EmergencyRoute
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@Composable
fun OfflineMapView(
    modifier: Modifier = Modifier,
    userLocation: Location?,
    hospitals: List<Hospital>,
    activeRoute: EmergencyRoute?,
    selectedHospital: Hospital?,
    onSelectHospital: (Hospital) -> Unit
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration for caching
    remember {
        val basePath = File(context.cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = tileCache
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(11.0)
            // Center around central Kerala (Thrissur/Ernakulam) by default
            controller.setCenter(GeoPoint(10.5276, 76.2144))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            // 1. Draw Active Route Polyline if present
            if (activeRoute != null && activeRoute.points.isNotEmpty()) {
                val polyline = Polyline(map).apply {
                    setPoints(activeRoute.points)
                    outlinePaint.color = AndroidColor.argb(220, 214, 40, 40) // Emergency Red
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                map.overlays.add(polyline)
            }

            // 2. Add User Location Marker
            if (userLocation != null) {
                val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                val userMarker = Marker(map).apply {
                    position = userPoint
                    title = "Your Emergency Location"
                    snippet = "Lat: ${"%.4f".format(userLocation.latitude)}, Lng: ${"%.4f".format(userLocation.longitude)}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(userMarker)
            }

            // 3. Add Hospital Markers
            for (hospital in hospitals) {
                val hospPoint = GeoPoint(hospital.latitude, hospital.longitude)
                val isSelected = selectedHospital?.name == hospital.name
                val marker = Marker(map).apply {
                    position = hospPoint
                    title = hospital.name
                    snippet = "${hospital.district} • ${hospital.address}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        showInfoWindow()
                        onSelectHospital(hospital)
                        true
                    }
                }
                map.overlays.add(marker)
            }

            // Center view priority:
            if (activeRoute != null && activeRoute.points.isNotEmpty()) {
                // Center on start of route or destination
                map.controller.animateTo(activeRoute.points.first())
            } else if (selectedHospital != null) {
                map.controller.animateTo(GeoPoint(selectedHospital.latitude, selectedHospital.longitude))
            } else if (userLocation != null) {
                map.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude))
            }

            map.invalidate()
        }
    )
}
