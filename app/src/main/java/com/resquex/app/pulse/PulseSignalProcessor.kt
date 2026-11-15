package com.resquex.app.pulse

import kotlin.math.*

/**
 * Photoplethysmography (PPG) Signal Processor
 * Analyzes frame intensity variations over time to detect arterial pulse peaks.
 */
class PulseSignalProcessor(
    private val windowDurationMs: Long = 8000L, // 8-second analysis window
    private val minBpm: Int = 45,
    private val maxBpm: Int = 180
) {
    data class Sample(val timestampMs: Long, val redIntensity: Double)

    private val samples = mutableListOf<Sample>()

    // Physiological peak-to-peak interval limits in milliseconds
    private val minPeakIntervalMs = (60_000.0 / maxBpm).toLong() // ~333 ms
    private val maxPeakIntervalMs = (60_000.0 / minBpm).toLong() // ~1333 ms

    fun reset() {
        samples.clear()
    }

    /**
     * Verifies if a finger is securely covering the camera lens and torch.
     * When illuminated by torch, blood tissue reflects overwhelmingly Red light with low Green/Blue.
     */
    fun isFingerDetected(avgRed: Double, avgGreen: Double, avgBlue: Double): Boolean {
        // Red must be high (> 160) and significantly exceed green and blue
        return avgRed > 150.0 && avgRed > (avgGreen * 1.3) && avgRed > (avgBlue * 1.5)
    }

    /**
     * Adds an intensity sample. Returns estimation result.
     */
    fun addSample(timestampMs: Long, intensity: Double): EstimationResult {
        samples.add(Sample(timestampMs, intensity))

        // Prune samples older than the sliding window
        val cutoff = timestampMs - windowDurationMs
        while (samples.isNotEmpty() && samples.first().timestampMs < cutoff) {
            samples.removeAt(0)
        }

        val totalWindowSpan = if (samples.size > 1) {
            samples.last().timestampMs - samples.first().timestampMs
        } else 0L

        val progressPercent = ((totalWindowSpan.toDouble() / windowDurationMs) * 100).toInt().coerceIn(0, 100)

        // Need at least 4 seconds of samples (~120 frames at 30fps) for initial estimate
        if (totalWindowSpan < 4000L || samples.size < 60) {
            return EstimationResult(
                bpm = 0,
                isReliable = false,
                progressPercent = progressPercent,
                status = "Calibrating pulse signal... Hold still ($progressPercent%)",
                normalizedSignal = 0f
            )
        }

        return processSignal(progressPercent)
    }

    private fun processSignal(progressPercent: Int): EstimationResult {
        // 1. Moving average smoothing (kernel size 5)
        val smoothed = mutableListOf<Double>()
        val kernelSize = 5
        for (i in samples.indices) {
            val start = (i - kernelSize / 2).coerceAtLeast(0)
            val end = (i + kernelSize / 2).coerceAtMost(samples.size - 1)
            var sum = 0.0
            for (j in start..end) {
                sum += samples[j].redIntensity
            }
            smoothed.add(sum / (end - start + 1))
        }

        // 2. Detrend (subtract rolling baseline mean to isolate pulse AC component)
        val baselineWindow = 15
        val detrended = mutableListOf<Double>()
        for (i in smoothed.indices) {
            val start = (i - baselineWindow / 2).coerceAtLeast(0)
            val end = (i + baselineWindow / 2).coerceAtMost(smoothed.size - 1)
            var sum = 0.0
            for (j in start..end) {
                sum += smoothed[j]
            }
            val localMean = sum / (end - start + 1)
            detrended.add(smoothed[i] - localMean)
        }

        // 3. Peak detection
        val peakTimes = mutableListOf<Long>()
        // A peak occurs when value is greater than its immediate neighbors and exceeds positive threshold
        var maxAmp = 0.0
        for (v in detrended) {
            if (abs(v) > maxAmp) maxAmp = abs(v)
        }
        val threshold = maxAmp * 0.25

        for (i in 1 until detrended.size - 1) {
            if (detrended[i] > threshold &&
                detrended[i] > detrended[i - 1] &&
                detrended[i] > detrended[i + 1]
            ) {
                val peakTime = samples[i].timestampMs
                if (peakTimes.isEmpty() || (peakTime - peakTimes.last()) >= minPeakIntervalMs) {
                    peakTimes.add(peakTime)
                }
            }
        }

        val lastNormalized = if (maxAmp > 0.001) {
            (detrended.last() / maxAmp).toFloat().coerceIn(-1f, 1f)
        } else 0f

        // 4. Calculate intervals and BPM
        if (peakTimes.size < 3) {
            return EstimationResult(
                bpm = 0,
                isReliable = false,
                progressPercent = progressPercent,
                status = "Detecting heartbeat peaks... Hold still ($progressPercent%)",
                normalizedSignal = lastNormalized
            )
        }

        val intervals = mutableListOf<Long>()
        for (i in 0 until peakTimes.size - 1) {
            val interval = peakTimes[i + 1] - peakTimes[i]
            if (interval in minPeakIntervalMs..maxPeakIntervalMs) {
                intervals.add(interval)
            }
        }

        if (intervals.size < 2) {
            return EstimationResult(
                bpm = 0,
                isReliable = false,
                progressPercent = progressPercent,
                status = "Weak pulse rhythm detected. Keep finger steady.",
                normalizedSignal = lastNormalized
            )
        }

        val avgInterval = intervals.average()
        val calculatedBpm = (60_000.0 / avgInterval).roundToInt()

        // Check rhythm consistency (standard deviation of intervals)
        var varianceSum = 0.0
        for (interval in intervals) {
            varianceSum += (interval - avgInterval).pow(2)
        }
        val stdDev = sqrt(varianceSum / intervals.size)
        val cv = stdDev / avgInterval // Coefficient of variation

        // If rhythm consistency is high (CV < 0.22) and in physiological range
        val isReliable = cv < 0.22 && calculatedBpm in minBpm..maxBpm

        val statusText = if (isReliable) {
            "Pulse rhythm stabilized"
        } else {
            "Analyzing pulse consistency... ($progressPercent%)"
        }

        return EstimationResult(
            bpm = calculatedBpm,
            isReliable = isReliable,
            progressPercent = progressPercent,
            status = statusText,
            normalizedSignal = lastNormalized
        )
    }
}

data class EstimationResult(
    val bpm: Int,
    val isReliable: Boolean,
    val progressPercent: Int,
    val status: String,
    val normalizedSignal: Float
)
