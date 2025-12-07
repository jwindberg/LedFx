package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import kotlin.math.*

/**
 * AudioVisualizer-style animation, inspired by the original Processing/Minim sketch.
 *
 * This implementation uses live audio from the default input device (microphone / line‑in),
 * performs a simple FFT in a background thread, and renders two concentric "atomic sprocket"
 * rings of boxes that swirl and react to the real spectrum. The same data is mapped to the LED grid.
 */
class AudioVisualizerAnimation : FFTAnimation() {
    // FFT / spectrum configuration
    override val bandCount = 64
    override val fftSize = 1024  // must be power of two

    // Position / phase per band (driven by spectrum)
    private val x = FloatArray(bandCount)
    private val y = FloatArray(bandCount)
    private val angle = FloatArray(bandCount)

    private var startTimeMs: Long = 0

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        super.init(width, height, ledGrid)
        this.startTimeMs = System.currentTimeMillis()

        // Initialize positions and angles
        for (i in 0 until bandCount) {
            x[i] = 0f
            y[i] = 0f
            angle[i] = (i / bandCount.toFloat()) * (2f * Math.PI.toFloat())
        }
    }

    override fun smoothSpectrum() {
        // Custom smoother smoothing for more visible motion
        for (b in 0 until bandCount) {
            val target = spectrum[b]
            val current = spectrumSmoothed[b]
            spectrumSmoothed[b] = if (target > current) {
                // Attack very fast, decay moderately fast for more visible motion
                current + (target - current) * 0.9f
            } else {
                current + (target - current) * 0.4f
            }
        }
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Safety check: ensure arrays are initialized
        if (!arraysInitialized) return

        // Smooth background with light trails
        g.color = Color(0, 0, 0, 40)
        g.fillRect(0, 0, width, height)

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Get LED grid bounds to constrain animation to LED area
        val bounds = ledGrid.getGridBounds(width, height)
        val centerX = bounds.centerX.toFloat()
        val centerY = bounds.centerY.toFloat()

        val now = System.currentTimeMillis()
        val t = (now - startTimeMs) / 1000.0f

        // First ring: bright colored boxes
        g.translate(centerX.toDouble(), centerY.toDouble())
        doubleAtomicRing(g, t, inner = false, bounds)
        g.translate(-centerX.toDouble(), -centerY.toDouble())

        // Second ring: inverse colors further out
        g.translate(centerX.toDouble(), centerY.toDouble())
        doubleAtomicRing(g, t, inner = true, bounds)
        g.translate(-centerX.toDouble(), -centerY.toDouble())

        // Map to LEDs from the spectrum‑driven positions and colors
        mapToLeds(ledGrid, t, bounds)

        // Info text
        g.color = Color.WHITE
        g.drawString("AudioVisualizer (live audio) - Press ESC to exit", 10, 20)
    }

    /**
     * Draws a single "atomic sprocket" style ring of boxes.
     */
    private fun doubleAtomicRing(g: Graphics2D, t: Float, inner: Boolean, bounds: LedGrid.GridBounds) {
        // Stronger movement and radius for higher activity
        val radiusBase = if (inner) 0.45f else 0.25f
        val radiusMod = if (inner) 1.20f else 0.80f
        val spreadFactor = if (inner) 3.0f else 2.0f
        val gridSize = min(bounds.width, bounds.height).toFloat() / 2.0f

        for (i in 0 until bandCount) {
            // Emphasize magnitudes heavily for strong visual response
            val rawMag = spectrumSmoothed[i].coerceIn(0f, 1f)
            val bandMag = (rawMag * 3.0f).coerceAtMost(1.0f)
            val bandFreqNorm = (i.toFloat() / (bandCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)

            // Integrate motion
            x[i] += bandFreqNorm * spreadFactor
            y[i] += bandMag * spreadFactor
            angle[i] += bandFreqNorm * if (inner) 0.0025f else 0.01f

            val ringRadius = radiusBase + radiusMod * bandMag
            val px = (ringRadius * cos(angle[i].toDouble())).toFloat()
            val py = (ringRadius * sin(angle[i].toDouble())).toFloat()

            // Color scheme inspired by the original code
            val color = if (!inner) {
                Color(
                    (bandFreqNorm * 510f).toInt().coerceIn(0, 255),
                    (bandMag * 180f).toInt().coerceIn(0, 255),
                    (bandMag * 720f).toInt().coerceIn(0, 255)
                )
            } else {
                Color(
                    (bandFreqNorm * 120f).toInt().coerceIn(0, 255),
                    (255f - bandFreqNorm * 510f).toInt().coerceIn(0, 255),
                    (255f - bandMag * 720f).toInt().coerceIn(0, 255)
                )
            }

            g.color = color

            val boxSize = 4.0 + bandMag * 28.0 + bandFreqNorm * 12.0
            val drawX = (px * gridSize).toInt()
            val drawY = (py * gridSize).toInt()

            g.fillRect(drawX - boxSize.toInt() / 2, drawY - boxSize.toInt() / 2, boxSize.toInt(), boxSize.toInt())
        }
    }

    /**
     * Maps the spectrum‑driven ring positions to the LED grid.
     */
    private fun mapToLeds(ledGrid: LedGrid, t: Float, bounds: LedGrid.GridBounds) {
        ledGrid.clearAllLeds()

        val gridCount = ledGrid.gridCount
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize

        val centerX = bounds.centerX.toFloat()
        val centerY = bounds.centerY.toFloat()
        val gridSizeFloat = min(bounds.width, bounds.height).toFloat() / 2.0f

        for (i in 0 until bandCount) {
            val rawMag = spectrumSmoothed[i].coerceIn(0f, 1f)
            val bandMag = (rawMag * 3.0f).coerceAtMost(1.0f)
            val bandFreqNorm = (i.toFloat() / (bandCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)

            // Use the outer ring positions for LED mapping
            val radiusBase = 0.18f
            val radiusMod = 0.30f
            val ringRadius = radiusBase + radiusMod * bandMag
            val px = (ringRadius * cos(angle[i].toDouble())).toFloat()
            val py = (ringRadius * sin(angle[i].toDouble())).toFloat()

            val worldX = centerX + px * gridSizeFloat
            val worldY = centerY + py * gridSizeFloat

            val color = Color(
                (bandFreqNorm * 510f).toInt().coerceIn(0, 255),
                (bandMag * 255f).toInt().coerceIn(0, 255),
                (255f - bandMag * 255f).toInt().coerceIn(0, 255)
            )

            for (gridIndex in 0 until gridCount) {
                val cfg = ledGrid.getGridConfig(gridIndex) ?: continue

                val relX = (worldX - cfg.x).toInt()
                val relY = (worldY - cfg.y).toInt()

                val ledX = relX / pixelSize
                val ledY = relY / pixelSize

                if (ledX in 0 until gridSize && ledY in 0 until gridSize) {
                    ledGrid.setLedColor(gridIndex, ledX, ledY, color)
                }
            }
        }

        ledGrid.sendToDevices()
    }


    override fun getName(): String = "Audio Visualizer"

    override fun getDescription(): String =
        "Live audio-visualizer rings driven by FFT of the default audio input"

    companion object {
        private val log: Logger = LogManager.getLogger(AudioVisualizerAnimation::class.java)
    }
}



