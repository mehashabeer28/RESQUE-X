package com.resquex.app.pulse

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

class CameraPulseDetector(
    private val context: Context,
    private val onFingerDetected: (Boolean) -> Unit,
    private val onResult: (EstimationResult) -> Unit,
    private val onError: (String) -> Unit
) {
    private val tag = "CameraPulseDetector"

    private val signalProcessor = PulseSignalProcessor()
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var isMeasuring = false

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("PulseCameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(tag, "Background thread join interrupted", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isMeasuring) return
        isMeasuring = true
        signalProcessor.reset()

        startBackgroundThread()

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            onError("Camera service unavailable on this device.")
            return
        }

        try {
            val backCameraId = cameraManager!!.cameraIdList.firstOrNull { id ->
                val chars = cameraManager!!.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }

            if (backCameraId == null) {
                onError("No rear camera found on device.")
                return
            }

            // Small 320x240 frame for lightweight edge-AI processing without memory strain
            imageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2)
            imageReader!!.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    processImageFrame(image)
                } finally {
                    image.close()
                }
            }, backgroundHandler)

            cameraManager!!.openCamera(backCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    onError("Camera device error: code $error")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(tag, "Failed to start camera", e)
            onError("Failed to initialize camera: ${e.message}")
        }
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: return
        val reader = imageReader ?: return

        try {
            val surface = reader.surface
            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session

                    try {
                        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                            // Enable Torch to illuminate finger capillaries
                            set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                        }

                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to set repeating request", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    onError("Failed to configure camera sensor capture session.")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(tag, "Error creating capture session", e)
            onError("Camera capture initialization failed.")
        }
    }

    private fun processImageFrame(image: android.media.Image) {
        val planes = image.planes
        if (planes.size < 3) return

        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val width = image.width
        val height = image.height

        // Sample center 80x80 box to minimize edge light interference
        val centerX = width / 2
        val centerY = height / 2
        val sampleRadius = 40

        var totalRed = 0.0
        var totalGreen = 0.0
        var totalBlue = 0.0
        var pixelCount = 0

        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride

        for (y in (centerY - sampleRadius)..(centerY + sampleRadius) step 2) {
            for (x in (centerX - sampleRadius)..(centerX + sampleRadius) step 2) {
                if (x in 0 until width && y in 0 until height) {
                    val yIndex = y * yRowStride + x
                    val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                    if (yIndex < yBuffer.capacity() && uvIndex < uBuffer.capacity() && uvIndex < vBuffer.capacity()) {
                        val yVal = (yBuffer.get(yIndex).toInt() and 0xFF)
                        val uVal = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                        val vVal = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                        // Standard YUV to RGB conversion
                        val r = (yVal + 1.402 * vVal).coerceIn(0.0, 255.0)
                        val g = (yVal - 0.34414 * uVal - 0.71414 * vVal).coerceIn(0.0, 255.0)
                        val b = (yVal + 1.772 * uVal).coerceIn(0.0, 255.0)

                        totalRed += r
                        totalGreen += g
                        totalBlue += b
                        pixelCount++
                    }
                }
            }
        }

        if (pixelCount == 0) return

        val avgRed = totalRed / pixelCount
        val avgGreen = totalGreen / pixelCount
        val avgBlue = totalBlue / pixelCount

        val isFingerCovering = signalProcessor.isFingerDetected(avgRed, avgGreen, avgBlue)

        if (!isFingerCovering) {
            signalProcessor.reset()
            onFingerDetected(false)
            onResult(
                EstimationResult(
                    bpm = 0,
                    isReliable = false,
                    progressPercent = 0,
                    status = "Place fingertip gently over the rear camera lens & flashlight",
                    normalizedSignal = 0f
                )
            )
        } else {
            onFingerDetected(true)
            val now = System.currentTimeMillis()
            val result = signalProcessor.addSample(now, avgRed)
            onResult(result)
        }
    }

    fun stop() {
        isMeasuring = false
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            signalProcessor.reset()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping camera detector", e)
        } finally {
            stopBackgroundThread()
        }
    }
}
