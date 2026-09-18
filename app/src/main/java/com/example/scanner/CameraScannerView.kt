package com.example.scanner

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraScannerView(
    onQrScanned: (String) -> Unit,
    onPickImageClicked: () -> Unit,
    onManualInputClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraError by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Deduplication cooldown state
    var lastScannedValue by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCameraError) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            bindCamera(
                                cameraProvider = cameraProvider,
                                previewView = previewView,
                                lifecycleOwner = lifecycleOwner,
                                lensFacing = lensFacing,
                                cameraExecutor = cameraExecutor,
                                onScanned = { raw ->
                                    val now = System.currentTimeMillis()
                                    if (raw != lastScannedValue || (now - lastScannedTime) > 2000) {
                                        lastScannedValue = raw
                                        lastScannedTime = now
                                        onQrScanned(raw)
                                    }
                                },
                                onCameraBound = { cam ->
                                    camera = cam
                                }
                            )
                        } catch (e: Exception) {
                            hasCameraError = true
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = { previewView ->
                    // Rebind on lensFacing change
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            bindCamera(
                                cameraProvider = cameraProvider,
                                previewView = previewView,
                                lifecycleOwner = lifecycleOwner,
                                lensFacing = lensFacing,
                                cameraExecutor = cameraExecutor,
                                onScanned = { raw ->
                                    val now = System.currentTimeMillis()
                                    if (raw != lastScannedValue || (now - lastScannedTime) > 2000) {
                                        lastScannedValue = raw
                                        lastScannedTime = now
                                        onQrScanned(raw)
                                    }
                                },
                                onCameraBound = { cam ->
                                    camera = cam
                                    camera?.cameraControl?.enableTorch(isTorchOn)
                                }
                            )
                        } catch (e: Exception) {
                            hasCameraError = true
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback screen if camera hardware is unavailable (e.g. some emulators)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera Unavailable",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You can still scan QR codes from screenshots, photos, or enter data manually.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Overlay with laser animation & viewfinder
        ViewfinderOverlay(modifier = Modifier.fillMaxSize())

        // Top guidance chip
        Surface(
            color = Color(0xCC0F172A),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Text(
                text = "Point camera at any QR Code",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Bottom Controls Bar (Flash, Camera switch, Gallery, Manual input)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight toggle
            FilledIconButton(
                onClick = {
                    val nextTorch = !isTorchOn
                    isTorchOn = nextTorch
                    camera?.cameraControl?.enableTorch(nextTorch)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isTorchOn) Color(0xFFFBBF24) else Color(0x991E293B),
                    contentColor = if (isTorchOn) Color.Black else Color.White
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("flash_toggle_button")
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flashlight"
                )
            }

            // Flip camera (Back / Front)
            FilledIconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0x991E293B),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera"
                )
            }

            // Gallery / Photo picker button
            FilledIconButton(
                onClick = onPickImageClicked,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0x991E293B),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("gallery_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Scan from Image or Gallery"
                )
            }

            // Manual or Test input button
            FilledIconButton(
                onClick = onManualInputClicked,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("manual_entry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Manual or Test Entry"
                )
            }
        }
    }
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    lensFacing: Int,
    cameraExecutor: java.util.concurrent.ExecutorService,
    onScanned: (String) -> Unit,
    onCameraBound: (Camera) -> Unit
) {
    cameraProvider.unbindAll()

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, QrCodeAnalyzer(onScanned))
        }

    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageAnalysis
    )
    onCameraBound(camera)
}

@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight

        val viewFinderSize = (boxWidth * 0.72f).coerceAtMost(300.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height
            val vfSizePx = viewFinderSize.toPx()

            val left = (widthPx - vfSizePx) / 2f
            val top = (heightPx - vfSizePx) / 2f - 40.dp.toPx()
            val right = left + vfSizePx
            val bottom = top + vfSizePx

            // Dimmed mask with cut-out
            val path = Path().apply {
                addRect(Rect(0f, 0f, widthPx, heightPx))
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                )
            }
            drawPath(path, color = Color(0x99000000))

            // Corner Brackets
            val cornerLength = 32.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val cornerRadiusPx = 16.dp.toPx()
            val bracketColor = Color(0xFF14B8A6) // Emerald Teal

            // Top-Left corner
            drawLine(bracketColor, Offset(left, top + cornerLength), Offset(left, top + cornerRadiusPx), strokeWidth)
            drawLine(bracketColor, Offset(left + cornerRadiusPx, top), Offset(left + cornerLength, top), strokeWidth)
            drawArc(bracketColor, 180f, 90f, false, Offset(left, top), Size(cornerRadiusPx * 2, cornerRadiusPx * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))

            // Top-Right corner
            drawLine(bracketColor, Offset(right - cornerLength, top), Offset(right - cornerRadiusPx, top), strokeWidth)
            drawLine(bracketColor, Offset(right, top + cornerRadiusPx), Offset(right, top + cornerLength), strokeWidth)
            drawArc(bracketColor, 270f, 90f, false, Offset(right - cornerRadiusPx * 2, top), Size(cornerRadiusPx * 2, cornerRadiusPx * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))

            // Bottom-Left corner
            drawLine(bracketColor, Offset(left, bottom - cornerLength), Offset(left, bottom - cornerRadiusPx), strokeWidth)
            drawLine(bracketColor, Offset(left + cornerRadiusPx, bottom), Offset(left + cornerLength, bottom), strokeWidth)
            drawArc(bracketColor, 90f, 90f, false, Offset(left, bottom - cornerRadiusPx * 2), Size(cornerRadiusPx * 2, cornerRadiusPx * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))

            // Bottom-Right corner
            drawLine(bracketColor, Offset(right - cornerLength, bottom), Offset(right - cornerRadiusPx, bottom), strokeWidth)
            drawLine(bracketColor, Offset(right, bottom - cornerLength), Offset(right, bottom - cornerRadiusPx), strokeWidth)
            drawArc(bracketColor, 0f, 90f, false, Offset(right - cornerRadiusPx * 2, bottom - cornerRadiusPx * 2), Size(cornerRadiusPx * 2, cornerRadiusPx * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))

            // Animated Laser Beam
            val laserY = top + 12.dp.toPx() + (vfSizePx - 24.dp.toPx()) * laserProgress
            val laserBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x9938BDF8),
                    Color(0xFF22D3EE),
                    Color(0xFFFFFFFF),
                    Color(0xFF22D3EE),
                    Color(0x9938BDF8),
                    Color.Transparent
                ),
                startX = left,
                endX = right
            )
            drawLine(
                brush = laserBrush,
                start = Offset(left + 8.dp.toPx(), laserY),
                end = Offset(right - 8.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
