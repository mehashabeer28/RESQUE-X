package com.resquex.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.resquex.app.data.Hospital
import com.resquex.app.data.HospitalRepository
import com.resquex.app.routing.EmergencyRoadGraph
import com.resquex.app.routing.EmergencyRoute
import com.resquex.app.util.LocationHelper

private val EmergencyRed = Color(0xFFD62828)
private val DarkBlue = Color(0xFF102A43)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val hospitalRepository = remember { HospitalRepository(context) }
    val roadGraph = remember { EmergencyRoadGraph() }

    val allHospitals = remember { mutableStateOf<List<Hospital>>(emptyList()) }
    var displayedHospitals by remember { mutableStateOf<List<Hospital>>(emptyList()) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationStatusText by remember { mutableStateOf("Location not acquired yet") }

    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = List, 1 = Map & Route
    var selectedHospital by remember { mutableStateOf<Hospital?>(null) }
    var activeRoute by remember { mutableStateOf<EmergencyRoute?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Load initial hospitals from repository
    LaunchedEffect(Unit) {
        val list = hospitalRepository.getHospitals()
        allHospitals.value = list
        displayedHospitals = list
    }

    // Function to acquire GPS location and sort hospitals by proximity
    fun refreshLocationAndSort() {
        val location = LocationHelper(context).getCurrentLocation()
        currentLocation = location
        if (location != null) {
            val sorted = hospitalRepository.getNearbyHospitals(location.latitude, location.longitude)
            allHospitals.value = sorted
            displayedHospitals = if (searchQuery.isBlank()) {
                sorted
            } else {
                sorted.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.district.contains(searchQuery, ignoreCase = true)
                }
            }
            locationStatusText = "Lat: ${"%.4f".format(location.latitude)}, Lng: ${"%.4f".format(location.longitude)}"
        } else {
            locationStatusText = "GPS fix unavailable. Ensure device GPS is ON."
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                refreshLocationAndSort()
            } else {
                locationStatusText = "Location permission denied."
            }
        }

    fun requestGpsPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            refreshLocationAndSort()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun calculateRouteTo(hospital: Hospital) {
        selectedHospital = hospital
        val loc = currentLocation ?: Location("fallback").apply {
            // If GPS is not yet acquired, default to nearest node or central Kerala for offline routing preview
            latitude = 10.0163
            longitude = 76.3516
        }
        val route = roadGraph.findEmergencyRoute(loc.latitude, loc.longitude, hospital)
        activeRoute = route
        selectedTabIndex = 1 // Switch to map tab to show route
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA))
    ) {
        // --- Top Bar ---
        Surface(
            color = DarkBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("← Back", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Emergency Hospital Navigation",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Offline Maps & Corridor Guidance",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- Tabs: List vs Map ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = DarkBlue
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Hospital Directory (${allHospitals.value.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Offline Map & Route", fontWeight = FontWeight.SemiBold) }
            )
        }

        // --- GPS Info Bar ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE4EEFA)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPS Status: $locationStatusText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkBlue
                    )
                }
                Button(
                    onClick = { requestGpsPermission() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Acquire GPS", fontSize = 11.sp)
                }
            }
        }

        // --- Main Content Switcher ---
        if (selectedTabIndex == 0) {
            // === TAB 0: HOSPITAL DIRECTORY LIST ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // Search bar for 14 districts / hospital names
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        displayedHospitals = if (query.isBlank()) {
                            allHospitals.value
                        } else {
                            allHospitals.value.filter {
                                it.name.contains(query, ignoreCase = true) ||
                                        it.district.contains(query, ignoreCase = true)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    placeholder = { Text("Search by district or hospital name...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayedHospitals) { hospital ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hospital.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = DarkBlue
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${hospital.district} District",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = EmergencyRed
                                        )
                                        Text(
                                            text = hospital.address,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray
                                        )
                                    }

                                    if (currentLocation != null) {
                                        val dist = FloatArray(1)
                                        Location.distanceBetween(
                                            currentLocation!!.latitude,
                                            currentLocation!!.longitude,
                                            hospital.latitude,
                                            hospital.longitude,
                                            dist
                                        )
                                        Text(
                                            text = "${"%.1f".format(dist[0] / 1000)} km",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = EmergencyRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { calculateRouteTo(hospital) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmergencyRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("🗺 Offline Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // === TAB 1: OFFLINE MAP & ROUTE GUIDANCE ===
            Box(modifier = Modifier.fillMaxSize()) {
                // Interactive OSMDroid Map
                OfflineMapView(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = currentLocation,
                    hospitals = allHospitals.value,
                    activeRoute = activeRoute,
                    selectedHospital = selectedHospital,
                    onSelectHospital = { hosp ->
                        selectedHospital = hosp
                    }
                )

                // Bottom Overlay Card: Route Guidance & Selected Hospital info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (activeRoute != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "EMERGENCY ROUTE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmergencyRed
                                        )
                                        Text(
                                            text = activeRoute!!.destinationName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBlue
                                        )
                                    }
                                    Button(
                                        onClick = { activeRoute = null },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEEEEEE),
                                            contentColor = Color.DarkGray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Clear", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "📍 Total: ${"%.1f".format(activeRoute!!.totalDistanceKm)} km",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkBlue
                                    )
                                    Text(
                                        text = "⏱ ETA: ~${activeRoute!!.estimatedMinutes} mins",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmergencyRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Turn-by-Turn Corridor Guidance (Offline):",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Scrollable route steps
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp)
                                ) {
                                    items(activeRoute!!.steps) { step ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "•",
                                                color = EmergencyRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = "${step.instruction} (${"%.1f".format(step.distanceKm)} km)",
                                                fontSize = 12.sp,
                                                color = DarkBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedHospital != null) {
                        // Card showing hospital selected on map
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = selectedHospital!!.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkBlue
                                )
                                Text(
                                    text = "${selectedHospital!!.district} • ${selectedHospital!!.address}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { calculateRouteTo(selectedHospital!!) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmergencyRed,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Find Offline Emergency Route", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Hint card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.92f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 Tap any hospital pin on the map or switch to Hospital Directory to generate an offline route.",
                                    fontSize = 12.sp,
                                    color = DarkBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
