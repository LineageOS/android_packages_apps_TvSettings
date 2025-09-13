/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tv.twopanelsettings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap
import kotlin.coroutines.CoroutineContext

class QrCodeView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    FrameLayout(context, attrs, defStyle) {

    private val qrCanvas: AppCompatImageView
    private val spinner: CircularProgressIndicator
    private val backgroundContainer: View
    private val boundaryMarkers: View
    private val qrLogo: AppCompatImageView

    private var viewScope: CoroutineScope? = null
    private var generationJob: Job? = null
    private var delayJob: Job? = null
    private var isQrBitmapGenerationCoroutineContextSet = false
    private var qrBitmapGenerationCoroutineContext: CoroutineContext = Dispatchers.Default

    private val defaultSize: Int =
        context.resources.getDimensionPixelSize(R.dimen.qr_frame_default_size)
    private var scaledLogoRadius: Float = 0f
    private var data: String? = null
    private var isDeferredData: Boolean = false

    private val dotPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.qr_dot_color)
            style = Paint.Style.FILL
        }
    }

    private val finderRingPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = context.getColor(R.color.qr_finder_outer_ring_color)
        }
    }

    private val finderClearPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private val finderDotPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = context.getColor(R.color.qr_finder_inner_dot_color)
        }
    }

    @VisibleForTesting var onQrGenerationComplete: (() -> Unit)? = null

    var logo: Drawable? = null
        set(value) {
            field = value
            qrLogo.setImageDrawable(value)
            qrLogo.visibility = if (value != null) VISIBLE else GONE
        }

    init {
        val themedContext =
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        LayoutInflater.from(themedContext).inflate(R.layout.qr_code_layout, this, true)
        qrCanvas = findViewById(R.id.qr_code_canvas)
        spinner = findViewById(R.id.qr_loading_spinner)
        backgroundContainer = findViewById(R.id.background_container)
        boundaryMarkers = findViewById(R.id.boundary_markers)
        qrLogo = findViewById(R.id.qr_code_logo)

        qrCanvas.visibility = GONE
        spinner.visibility = GONE

        backgroundContainer.setRoundRectOutline(
            resources.getDimension(R.dimen.qr_frame_border_radius)
        )

        val logoDrawable = context.getDrawable(R.drawable.qr_code_logo)
        if (logoDrawable !is ColorDrawable) { // Logo supplied in resource overlay.
            logo = logoDrawable
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        if (isReadyToRender()) {
            // If data was set before the view is attached, QR code generation was deferred. Now since
            // the view is attached and `CoroutineScope` is available, we can kick off the QR code
            // generation.
            setData(data)
        }
    }

    private fun isReadyToRender() = data != null && isAttachedToWindow && width > 0 && height > 0

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
        viewScope = null
        isDeferredData = data != null
    }

    override fun onSizeChanged(layoutWidth: Int, layoutHeight: Int, oldWidth: Int, oldHeight: Int) {
        val scale = layoutWidth / defaultSize.toFloat()

        val frameSize = (resources.getDimension(R.dimen.qr_frame_default_size) * scale).toInt()
        backgroundContainer.updateViewSize(frameSize, frameSize)

        val boundaryMarkersSize =
            (resources.getDimension(R.dimen.qr_boundary_markers_default_size) * scale).toInt()
        boundaryMarkers.updateViewSize(boundaryMarkersSize, boundaryMarkersSize)

        val canvasSize = (resources.getDimension(R.dimen.qr_code_default_size) * scale).toInt()
        qrCanvas.updateViewSize(canvasSize, canvasSize)

        val defaultRadius = resources.getDimension(R.dimen.qr_frame_border_radius)
        backgroundContainer.setRoundRectOutline(defaultRadius * scale)

        val logoSize = resources.getDimension(R.dimen.qr_logo_default_size)
        qrLogo.updateViewSize((logoSize * scale).toInt(), (logoSize * scale).toInt())

        scaledLogoRadius = logoSize * scale

        if (isReadyToRender()) {
            setData(data)
        }
    }

    /**
     * Sets the [CoroutineContext] to be used for the background QR code bitmap generation. This
     * function can only be called once and will throw an [IllegalStateException] if called again.
     *
     * @param coroutineContext The [CoroutineContext] to use for bitmap generation, which should be
     *   analogous to `Dispatchers.Default`.
     * @throws IllegalStateException if the coroutineContext is set to different value more than once.
     */
    fun setQrBitmapGenerationCoroutineContext(coroutineContext: CoroutineContext) {
        if (qrBitmapGenerationCoroutineContext == coroutineContext) {
            return
        }

        if (isQrBitmapGenerationCoroutineContextSet) {
            throw IllegalStateException("qrBitmapGenerationCoroutineContext can only be set once")
        } else {
            isQrBitmapGenerationCoroutineContextSet = true
        }
        qrBitmapGenerationCoroutineContext = coroutineContext
    }

    /**
     * Sets the data to be encoded in the QR code. Starts the asynchronous generation process.
     *
     * @param data The String data to encode. If null or empty, the view will be cleared.
     */
    fun setData(data: String?) {
        if (this.data == data && !isDeferredData) {
            return
        }

        this.data = data

        generationJob?.cancel()
        delayJob?.cancel()

        if (data.isNullOrBlank()) {
            qrCanvas.setImageBitmap(null)
            qrCanvas.visibility = GONE
            spinner.visibility = GONE
            return
        }

        if (!isReadyToRender()) {
            // Delay setting the data until the view is attached to the window and laid out.
            isDeferredData = true
            return
        }
        isDeferredData = false
        val scope = viewScope!!

        delayJob =
            scope.launch {
                delay(SPINNER_DELAY_MS)
                spinner.visibility = VISIBLE
            }

        qrCanvas.visibility = GONE

        generationJob =
            scope.launch {
                var qrBitmap: Bitmap? = null
                try {
                    qrBitmap = generateQrCodeBitmap(data, width)
                } catch (e: WriterException) {
                    Log.e("QrCodeView", "Error generating QR code", e)
                } catch (e: CancellationException) {
                    Log.i("QrCodeView", "QR generation cancelled")
                } catch (e: Exception) {
                    Log.e("QrCodeView", "Unexpected error during QR generation", e)
                }

                delayJob?.cancel()
                spinner.visibility = GONE

                // Update UI back on the main thread (already on Main.immediate)
                spinner.visibility = GONE
                if (qrBitmap != null && isActive) {
                    qrCanvas.setImageBitmap(qrBitmap)
                    qrCanvas.visibility = VISIBLE
                    onQrGenerationComplete?.invoke()
                } else {
                    qrCanvas.setImageBitmap(null)
                    qrCanvas.visibility = GONE
                    onQrGenerationComplete?.invoke()
                }
            }
    }

    /** Generates the QR Code Bitmap on a background thread. This is a suspending function. */
    private suspend fun generateQrCodeBitmap(data: String, size: Int): Bitmap? =
        withContext(qrBitmapGenerationCoroutineContext) {
            val hints =
                EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                    put(EncodeHintType.CHARACTER_SET, "UTF-8")
                    put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                    put(EncodeHintType.MARGIN, 0)
                }

            try {
                val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints)
                val matrixWidth = bitMatrix.width
                val moduleSize = size.toFloat() / matrixWidth

                val bitmap = createBitmap(size, size)
                val canvas = Canvas(bitmap)
                val radius = moduleSize * DOT_RADIUS_FACTOR

                // Draw circular dots
                for (y in 0 until matrixWidth) {
                    for (x in 0 until matrixWidth) {
                        if (bitMatrix[x, y] && canDrawModuleAt(bitMatrix, x, y)) {
                            val cx = x * moduleSize + moduleSize / 2
                            val cy = y * moduleSize + moduleSize / 2
                            canvas.drawCircle(cx, cy, radius, dotPaint)
                        }
                    }
                }

                // Draw stylized finder patterns
                drawFinderPattern(canvas, moduleSize, 0f, 0f)
                drawFinderPattern(canvas, moduleSize, (matrixWidth - FINDER_SIZE) * moduleSize, 0f)
                drawFinderPattern(canvas, moduleSize, 0f, (matrixWidth - FINDER_SIZE) * moduleSize)

                bitmap
            } catch (e: Exception) {
                Log.e("QrCodeView", "QR generation error", e)
                null
            }
        }

    private fun canDrawModuleAt(matrix: BitMatrix, x: Int, y: Int): Boolean {

        // Skip finder patterns
        if (x <= FINDER_SIZE && y <= FINDER_SIZE) return false
        if (x >= matrix.width - FINDER_SIZE - 1 && y <= FINDER_SIZE) return false
        if (x <= FINDER_SIZE && y >= matrix.height - FINDER_SIZE - 1) return false

        // Skip center circular logo area if there is logo
        if (logo != null) {
            val moduleSize = width.toFloat() / matrix.width
            val cx = x * moduleSize + moduleSize / 2
            val cy = y * moduleSize + moduleSize / 2
            val qrCenter = width / 2f

            val dx = cx - qrCenter
            val dy = cy - qrCenter
            val distanceSquared = dx * dx + dy * dy

            return distanceSquared > scaledLogoRadius * scaledLogoRadius
        }

        return true
    }

    private fun drawFinderPattern(canvas: Canvas, moduleSize: Float, offsetX: Float, offsetY: Float) {
        val centerX = offsetX + moduleSize * FINDER_SIZE / 2
        val centerY = offsetY + moduleSize * FINDER_SIZE / 2

        val outerRadius = moduleSize * FINDER_SIZE / 2
        val innerRadius = outerRadius * FINDER_INNER_RING_FACTOR

        canvas.drawCircle(centerX, centerY, outerRadius, finderRingPaint)
        canvas.drawCircle(centerX, centerY, innerRadius, finderClearPaint)

        val centerDotRadius = outerRadius * FINDER_CENTER_DOT_FACTOR
        canvas.drawCircle(centerX, centerY, centerDotRadius, finderDotPaint)
    }

    companion object {
        private const val FINDER_SIZE = 7f
        private const val SPINNER_DELAY_MS = 150L

        private const val DOT_RADIUS_FACTOR = 0.36f
        private const val FINDER_INNER_RING_FACTOR = 0.7f
        private const val FINDER_CENTER_DOT_FACTOR = 0.4f
    }
}

/**
 * Sets a rounded rectangle outline for the View.
 *
 * This extension function simplifies applying a rounded rectangle outline to a `View`. It creates
 * an [ViewOutlineProvider] that defines the outline as a rounded rectangle and sets `clipToOutline`
 * to true to ensure the view is clipped to the defined outline.
 *
 * @param cornerRadius The corner radius in pixels for the rounded rectangle. Defaults to 20f.
 */
private fun View.setRoundRectOutline(cornerRadius: Float = 20f) {
    if (cornerRadius <= 0) {
        outlineProvider = null
        clipToOutline = false
        return
    }

    outlineProvider =
        object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
    clipToOutline = true
}

/** Update View Size to provided [width] and [height] */
private fun View.updateViewSize(width: Int, height: Int = width) {
    val layoutParam = layoutParams
    layoutParam.width = width
    layoutParam.height = height
    requestLayout()
}
