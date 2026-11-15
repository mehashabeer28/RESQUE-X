package com.resquex.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.resquex.app.pulse.CameraPulseDetector
import com.resquex.app.pulse.EstimationResult

private val EmergencyRed = Color(0xFFD62828)
private val DarkBlue = Color(0xFF102A43)
private val PulseGreen = Color(0xFF2E7D32)
private val CardBackground = Color(0xFFF8FAFC)

@Composable
fun PulseEstimationScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isMeasuring by remember { mutableStateOf(false) }
    var isFingerDetected by remember { mutableStateOf(false) }
    var estimationResult by remember {
        mutableStateOf(
            EstimationResult(
                bpm = 0,
                isReliable = false,
                progressPercent = 0,
                status = "Press 'Start Measurement' to begin",
                normalizedSignal = 0f
            )
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pulseDetector = remember {
        CameraPulseDetector(
            context = context,
            onFingerDetected = { detected ->
                isFingerDetected = detected
            },
            onResult = { result ->
                estimationResult = result
            },
            onError = { err ->
                errorMessage = err
                isMeasuring = false
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            pulseDetector.stop()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            errorMessage = null
            isMeasuring = true
            pulseDetector.start()
        } else {
            errorMessage = "Camera permission is required to analyze capillary pulse signals."
        }
    }

    // Heartbeat pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isFingerDetected) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (estimationResult.bpm in 45..180) (60000 / estimationResult.bpm) else 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA))
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header Bar ---
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
                    onClick = {
                        pulseDetector.stop()
                        onBack()
                    },
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
                        text = "Pulse Rate Estimation",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Camera-Based PPG Edge Prototype",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Experimental Medical Disclaimer Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3CD)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("⚠️", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                    Column {
                        Text(
                            text = "EXPERIMENTAL FIRST-AID PROTOTYPE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "This feature is an experimental tool using camera photoplethysmography (PPG). It is NOT a certified medical diagnostic device. Do not use for clinical emergencies.",
                            fontSize = 11.sp,
                            color = Color(0xFF856404),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Sensor Visualizer / Pulsing Ring ---
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFingerDetected) EmergencyRed.copy(alpha = 0.15f) else Color(0xFFE2E8F0)
                    )
                    .border(
                        width = 4.dp,
                        color = if (isFingerDetected) EmergencyRed else Color.LightGray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 42.sp,
                        modifier = Modifier.scale(if (isMeasuring) pulseScale else 1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!isMeasuring) "READY"
                        else if (isFingerDetected) "DETECTED"
                        else "NO FINGER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isFingerDetected) EmergencyRed else Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Status & Instructions ---
            Text(
                text = estimationResult.status,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFingerDetected) DarkBlue else EmergencyRed,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calibration Progress Bar
            if (isMeasuring) {
                LinearProgressIndicator(
                    progress = { estimationResult.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (estimationResult.isReliable) PulseGreen else EmergencyRed,
                    trackColor = Color(0xFFE0E0E0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calibration: ${estimationResult.progressPercent}%",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BPM Display Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ESTIMATED PULSE RATE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (estimationResult.bpm > 0) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${estimationResult.bpm}",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (estimationResult.isReliable) DarkBlue else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BPM",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmergencyRed,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        // Category
                        val zone = when {
                            estimationResult.bpm < 60 -> "Bradycardia / Below Normal (< 60)"
                            estimationResult.bpm in 60..100 -> "Normal Resting Pulse (60–100 BPM)"
                            else -> "Elevated / Tachycardia (> 100 BPM)"
                        }
                        Text(
                            text = zone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (estimationResult.bpm in 60..100) PulseGreen else EmergencyRed
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reliability Badge
                        Surface(
                            color = if (estimationResult.isReliable) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (estimationResult.isReliable) "✓ Reliable Signal" else "⏳ Stabilizing Reading...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estimationResult.isReliable) PulseGreen else Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "--",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Awaiting steady fingertip placement...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = EmergencyRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Action Buttons ---
            if (!isMeasuring) {
                Button(
                    onClick = {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            errorMessage = null
                            isMeasuring = true
                            pulseDetector.start()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "▶ Start Pulse Measurement",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = {
                        isMeasuring = false
                        pulseDetector.stop()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⏹ Stop Measurement",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- How It Works Step-by-Step Guide ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "How to use:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Place the fleshy part of your fingertip gently over the REAR CAMERA LENS and LED FLASHLIGHT.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. Do not press too hard (excessive pressure restricts blood microcirculation in capillaries).",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3. Hold your hand completely still for 8–10 seconds while the calibration bar fills.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
