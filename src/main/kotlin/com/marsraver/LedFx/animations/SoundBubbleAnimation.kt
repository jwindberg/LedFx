package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * SoundBubble animation inspired by the original Processing/Minim sketch.
 *
 * - Uses live audio from the default input device.
 * - Central "bubble" pulses and leaves a trail based on audio level.
 * - Particles orbit around the center, with orbits and sizes modulated by the audio.
 * - The same visuals are rendered to the Swing window and mapped to the LED grid.
 */
class SoundBubbleAnimation : RMSAnimation() {
    // Visual configuration
    private val rippleSize = 800f
    private val bubbleCount = 40
    override val bandCount = 40 // Maps to particleCount

    private val gravity = 0.05f
    private val gravityRadius = 0.93f

    private val bubbles = Array(bubbleCount) { Bubble(0f, 0f, Color.BLACK, 0f) }
    private val particles = Array(bandCount) { i ->
        val orbit = random(30f, 70f)
        val speed = random(-0.02f, 0.02f)
        val size = random(30f, 80f)
        Particle(orbit, speed, i, size)
    }

    private var noiseSeed = 0.0f
    private var rotateAngle = 0.0f
    private val rotateSpeed = 0.12f

    // Colors (very dark background so colored bubbles pop)
    private val bgColorOuter = Color(0, 0, 5)    // almost black
    private val bgColorMid = Color(5, 5, 15)     // subtle inner disk
    private val bgColorInner = Color(10, 10, 25) // slightly brighter core
    private val particleOuterColor = Color(200, 200, 220, 130)
    private val particleInnerColor = Color(240, 240, 255, 220)

    // Offscreen buffer for drawing and LED sampling
    private var offscreen: BufferedImage? = null
    private var offG: Graphics2D? = null

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        super.init(width, height, ledGrid)
        offscreen = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        offG = offscreen!!.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        }
    }

    override fun processAudioData(samples: FloatArray, sampleCount: Int) {
        // Custom RMS processing with log transform for master level
        var sumSq = 0.0
        val perBandSq = FloatArray(bandCount)
        val perBandCount = IntArray(bandCount)
        
        val validCount = minOf(sampleCount, samples.size)
        for (i in 0 until validCount) {
            val sample = samples[i]
            val sampleSq = sample * sample
            sumSq += sampleSq.toDouble()
            
            val band = (i * bandCount / validCount).coerceIn(0, bandCount - 1)
            perBandSq[band] += sampleSq
            perBandCount[band]++
        }

        val totalSamples = maxOf(1, validCount).toDouble()
        val rms = sqrt(sumSq / totalSamples).toFloat()
        
        // Custom log transform for master level (like original code)
        val newMaster = (ln(1f + rms * 50f)).coerceIn(0f, 1f)
        
        // Smooth master level with custom coefficients
        masterLevel = masterLevel * 0.7f + newMaster * 0.3f

        // Compute per-band RMS levels
        var maxBand = 1e-6f
        val bandRms = FloatArray(bandCount)
        
        for (b in 0 until bandCount) {
            if (perBandCount[b] > 0) {
                val rmsBand = sqrt(perBandSq[b] / perBandCount[b].toDouble()).toFloat()
                bandRms[b] = rmsBand
                if (rmsBand > maxBand) maxBand = rmsBand
            } else {
                bandRms[b] = 0f
            }
        }

        // Normalize and update band levels (thread-safe via base class)
        if (maxBand < 1e-6f) maxBand = 1e-6f
        for (b in 0 until bandCount) {
            val normalized = (bandRms[b] / maxBand).coerceIn(0f, 1f)
            // Custom smoothing like original code (override base class smoothing)
            bandLevels[b] = if (normalized > bandLevels[b]) {
                bandLevels[b] + (normalized - bandLevels[b]) * 0.8f
            } else {
                bandLevels[b] + (normalized - bandLevels[b]) * 0.3f
            }
        }
    }

    override fun getBufferSize(): Int = 2048

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        val canvas = offscreen ?: return
        val cg = offG ?: return

        noiseSeed += 0.01f

        // Get LED grid bounds to constrain animation to LED area
        val bounds = ledGrid.getGridBounds(width, height)

        // Background
        scrRefresh(cg, width, height)

        // Draw in centered coordinate system, using LED grid center
        val centerX = bounds.centerX.toDouble()
        val centerY = bounds.centerY.toDouble()
        val oldTx: AffineTransform = cg.transform
        cg.translate(centerX, centerY)
        
        // Set clip region to LED grid bounds to prevent drawing outside
        val maxRadius = min(bounds.width, bounds.height) / 2.0
        cg.clip = java.awt.geom.Ellipse2D.Double(-maxRadius, -maxRadius, maxRadius * 2, maxRadius * 2)
        
        // Draw concentric background disks at LED grid center
        val gridSize = min(bounds.width, bounds.height).toDouble()
        cg.color = bgColorMid
        val r1 = (gridSize * 0.4).toInt()
        cg.fillOval(-r1 / 2, -r1 / 2, r1, r1)
        cg.color = bgColorInner
        val r2 = (gridSize * 0.2).toInt()
        cg.fillOval(-r2 / 2, -r2 / 2, r2, r2)

        // Particles
        for (i in 0 until bandCount) {
            particles[i].update(cg, bandLevels[i], bounds)
        }

        // Bubbles (central ripple trail) with slow rotation
        rotateAngle += (pseudoNoise(noiseSeed) - 0.5f) * rotateSpeed
        cg.rotate(rotateAngle.toDouble())
        drawRipples(cg, bounds)
        for (i in 0 until bubbleCount) {
            bubbles[i].draw(cg)
        }

        // Restore transform
        cg.transform = oldTx

        // Paint to screen
        g.drawImage(canvas, 0, 0, null)

        // Map to LEDs
        mapToLedsFromImage(canvas, ledGrid)

        // Info text
        g.color = Color.WHITE
        g.font = Font("Arial", Font.PLAIN, 12)
        g.drawString("SoundBubble (live audio) - Press ESC to exit", 10, 20)
    }

    /**
     * Clears and redraws the background.
     */
    private fun scrRefresh(g: Graphics2D, width: Int, height: Int) {
        g.color = bgColorOuter
        g.fillRect(0, 0, width, height)
    }

    private inner class Particle(
        private val orbit: Float,
        private val speed: Float,
        private val id: Int,
        private val baseSize: Float
    ) {
        private var crtPosAngle: Float = (-Math.PI * 0.5).toFloat()
        // Assign each particle a base hue so they appear in different colors
        private val hue: Float = random(0f, 360f)

        fun update(g: Graphics2D, level: Float, bounds: LedGrid.GridBounds) {
            val amp = ln(1f + level * 40f).coerceAtLeast(0f)

            // Angular position modulated by audio level
            crtPosAngle += (0.5f + amp) * speed

            // Pseudo noise for radial variation
            val n = pseudoNoise(noiseSeed + id * 0.31f)
            val maxRadius = min(bounds.width, bounds.height) / 2.0f
            val radial = ((orbit + 300f * n) * (0.6f + amp * 1.4f)).coerceAtMost(maxRadius * 0.95f)

            val x = radial * cos(crtPosAngle.toDouble()).toFloat()
            val y = radial * sin(crtPosAngle.toDouble()).toFloat()

            // Roughly half the previous size range for a subtler look
            val diameter = (baseSize * (0.2f + amp * 0.9f))
                .coerceIn(2f, 60f)

            // Outer and inner colors derived from the particle's hue
            val outer = Color.getHSBColor((hue / 360f).coerceIn(0f, 1f), 0.7f, 1.0f)
            val innerColor = Color.getHSBColor((hue / 360f).coerceIn(0f, 1f), 0.2f, 1.0f)

            g.color = outer
            g.fillOval(
                (x - diameter / 2).toInt(),
                (y - diameter / 2).toInt(),
                diameter.toInt(),
                diameter.toInt()
            )

            g.color = innerColor
            val inner = max(0f, diameter - 6f)
            g.fillOval(
                (x - inner / 2).toInt(),
                (y - inner / 2).toInt(),
                inner.toInt(),
                inner.toInt()
            )
        }
    }

    private inner class Bubble(
        var x: Float,
        var y: Float,
        var color: Color,
        var radius: Float
    ) {
        fun draw(g: Graphics2D) {
            if (radius <= 0f) return

            g.color = color
            g.fillOval(
                (x - radius / 2).toInt(),
                (y - radius / 2).toInt(),
                radius.toInt(),
                radius.toInt()
            )

            // Simple highlight using background color (like original sketch)
            g.color = bgColorOuter
            val highlightRadius = radius * 0.6f
            g.fillOval(
                (x + radius * 0.08f - highlightRadius / 2).toInt(),
                (y + radius * 0.08f - highlightRadius / 2).toInt(),
                highlightRadius.toInt(),
                highlightRadius.toInt()
            )
        }
    }

    /**
     * Updates the ripple bubbles based on current audio level.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun drawRipples(g: Graphics2D, bounds: LedGrid.GridBounds) {
        val mainLevel = masterLevel.coerceIn(0f, 1f)
        val gridSize = min(bounds.width, bounds.height).toFloat()
        val maxRadius = gridSize / 2.0f
        val movRadius = (gridSize * 0.40f).coerceAtMost(maxRadius * 0.95f)

        // Head bubble driven by audio
        val angle = Math.PI.toFloat() * mainLevel * 2f - (Math.PI.toFloat() / 2f)
        bubbles[0].x = movRadius * cos(angle.toDouble()).toFloat()
        bubbles[0].y = movRadius * sin(angle.toDouble()).toFloat()

        // Softer, warm head bubble color (similar to original)
        val hue = 10f + 150f * pseudoNoise(noiseSeed + 10f)
        val c = Color.getHSBColor((hue / 360f).coerceIn(0f, 1f), 0.4f, 1.0f)
        bubbles[0].color = c
        bubbles[0].radius = mainLevel * (rippleSize + 0.02f * gridSize)

        // Trail bubbles follow with gravity-like decay
        for (i in bubbleCount - 1 downTo 1) {
            val prev = bubbles[i - 1]
            val b = bubbles[i]
            b.x = prev.x - prev.x * gravity
            b.y = prev.y - prev.y * gravity
            b.color = prev.color
            b.radius = prev.radius * gravityRadius
        }
    }

    /**
     * Maps the offscreen image to the LED grid by sampling at LED centers.
     */
    private fun mapToLedsFromImage(canvas: BufferedImage, ledGrid: LedGrid) {
        ledGrid.clearAllLeds()

        val gridCount = ledGrid.gridCount
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize

        for (gridIndex in 0 until gridCount) {
            val cfg = ledGrid.getGridConfig(gridIndex) ?: continue

            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    val windowX = cfg.x + x * pixelSize + pixelSize / 2
                    val windowY = cfg.y + y * pixelSize + pixelSize / 2

                    if (windowX < 0 || windowX >= windowWidth || windowY < 0 || windowY >= windowHeight) {
                        continue
                    }

                    val rgb = canvas.getRGB(windowX, windowY)
                    val color = Color(rgb)

                    // Ignore near-black pixels to keep LEDs mostly on active areas
                    if (color.red > 5 || color.green > 5 || color.blue > 5) {
                        ledGrid.setLedColor(gridIndex, x, y, color)
                    }
                }
            }
        }

        ledGrid.sendToDevices()
    }


    override fun getName(): String = "Sound Bubble"

    override fun getDescription(): String =
        "Live audio-driven bubbles and orbiting particles inspired by the original SoundBubble sketch"

    // Simple deterministic noise based on sine
    private fun pseudoNoise(x: Float): Float =
        (sin(x.toDouble()).toFloat() + 1f) * 0.5f

    private fun random(min: Float, max: Float): Float =
        min + (max - min) * Math.random().toFloat()

    companion object {
        private val log: Logger = LogManager.getLogger(SoundBubbleAnimation::class.java)
    }
}


