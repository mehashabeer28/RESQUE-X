package com.resquex.app

import com.resquex.app.pulse.PulseSignalProcessor
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PulseSignalProcessorTest {

    @Test
    fun testFingerDetection() {
        val processor = PulseSignalProcessor()

        // Case 1: Real finger over torch (High red, low green and blue)
        val fingerDetected = processor.isFingerDetected(
            avgRed = 210.0,
            avgGreen = 45.0,
            avgBlue = 30.0
        )
        assertTrue("Finger with strong red capillary reflection must be detected", fingerDetected)

        // Case 2: Ambient white light / room light
        val ambientLight = processor.isFingerDetected(
            avgRed = 140.0,
            avgGreen = 140.0,
            avgBlue = 140.0
        )
        assertFalse("Neutral room light must not be classified as finger", ambientLight)

        // Case 3: Dark / uncovered camera
        val darkCamera = processor.isFingerDetected(
            avgRed = 20.0,
            avgGreen = 18.0,
            avgBlue = 15.0
        )
        assertFalse("Dark / uncovered camera must not be classified as finger", darkCamera)
    }

    @Test
    fun testPulseEstimationWithSyntheticHeartbeatSignal() {
        val processor = PulseSignalProcessor(windowDurationMs = 8000L)

        // Generate synthetic PPG signal at 72 BPM (72 / 60 = 1.2 Hz -> ~833 ms period)
        val targetBpm = 72
        val freqHz = targetBpm / 60.0
        val sampleIntervalMs = 33L // ~30 FPS (33.3 ms per frame)
        val totalDurationMs = 8000L

        var lastResult = processor.addSample(0L, 200.0)

        var time = 0L
        while (time <= totalDurationMs) {
            val tSec = time / 1000.0
            // Baseline 200.0 + 5.0 * sin(2 * pi * f * t)
            val signal = 200.0 + 5.0 * sin(2.0 * PI * freqHz * tSec)
            lastResult = processor.addSample(time, signal)
            time += sampleIntervalMs
        }

        assertTrue("Progress should reach 100%", lastResult.progressPercent >= 90)
        assertTrue("BPM must be calculated", lastResult.bpm > 0)
        // Check estimated BPM is close to target 72 BPM (within ± 5 BPM tolerance)
        val error = kotlin.math.abs(lastResult.bpm - targetBpm)
        assertTrue("Estimated BPM (${lastResult.bpm}) should be close to 72 BPM", error <= 5)
        assertTrue("Regular periodic signal should be marked reliable", lastResult.isReliable)
    }

    @Test
    fun testUnstableOrInsufficientSignal() {
        val processor = PulseSignalProcessor(windowDurationMs = 8000L)

        // Feed only 1 second of data
        var result = processor.addSample(0L, 200.0)
        result = processor.addSample(500L, 202.0)
        result = processor.addSample(1000L, 198.0)

        // Must not report a valid BPM with only 1 second of data
        assertEquals("BPM should be 0 during initial calibration", 0, result.bpm)
        assertFalse("Signal should not be marked reliable yet", result.isReliable)
        assertTrue("Status must indicate calibration / holding still", result.status.contains("Hold still", ignoreCase = true))
    }
}
