package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlin.math.*

/**
 * SoundBubble animation inspired by the original Processing/Minim sketch.
 *
 * - Uses live audio from the default input device.
 * - Central "bubble" pulses and leaves a trail based on audio level.
 * - Particles orbit around the center, with orbits and sizes modulated by the audio.
 * - The same visuals are rendered to the Swing window and mapped to the LED grid.
 */
class SoundBubbleAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null
    private var windowWidth: Int = 0
    private var windowHeight: Int = 0

    // Visual configuration
    private val rippleSize = 800f
    private val bubbleCount = 40
    private val particleCount = 40
    private val gravity = 0.05f
    private val gravityRadius = 0.93f

    private val bubbles = Array(bubbleCount) { Bubble(0f, 0f, Color.BLACK, 0f) }
    private val particles = Array(particleCount) { i ->
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

    // Audio capture state
    @Volatile
    private var running = false
    private var audioThread: Thread? = null
    private var line: TargetDataLine? = null

    @Volatile
    private var masterLevel = 0f

    // Per-particle band levels (written from audio thread, read from render thread)
    private val bandLevels = FloatArray(particleCount)

    // Offscreen buffer for drawing and LED sampling
    private var offscreen: BufferedImage? = null
    private var offG: Graphics2D? = null

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height

        offscreen = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        offG = offscreen!!.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        }

        startAudioCapture()
        log.debug("SoundBubble Animation initialized with live audio")
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        val canvas = offscreen ?: return
        val cg = offG ?: return

        noiseSeed += 0.01f

        // Background
        scrRefresh(cg, width, height)

        // Draw in centered coordinate system
        val centerX = width / 2.0
        val centerY = height / 2.0
        val oldTx: AffineTransform = cg.transform
        cg.translate(centerX, centerY)

        // Particles
        for (i in 0 until particleCount) {
            particles[i].update(cg, bandLevels[i])
        }

        // Bubbles (central ripple trail) with slow rotation
        rotateAngle += (pseudoNoise(noiseSeed) - 0.5f) * rotateSpeed
        cg.rotate(rotateAngle.toDouble())
        drawRipples(cg)
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
     * Clears and redraws the background with concentric disks.
     */
    private fun scrRefresh(g: Graphics2D, width: Int, height: Int) {
        g.color = bgColorOuter
        g.fillRect(0, 0, width, height)

        val centerX = width / 2
        val centerY = height / 2

        g.color = bgColorMid
        val r1 = (width * 0.4).toInt()
        g.fillOval(centerX - r1 / 2, centerY - r1 / 2, r1, r1)

        g.color = bgColorInner
        val r2 = (width * 0.2).toInt()
        g.fillOval(centerX - r2 / 2, centerY - r2 / 2, r2, r2)
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

        fun update(g: Graphics2D, level: Float) {
            val amp = ln(1f + level * 40f).coerceAtLeast(0f)

            // Angular position modulated by audio level
            crtPosAngle += (0.5f + amp) * speed

            // Pseudo noise for radial variation
            val n = pseudoNoise(noiseSeed + id * 0.31f)
            val radial = (orbit + 300f * n) * (0.6f + amp * 1.4f)

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
    private fun drawRipples(g: Graphics2D) {
        val mainLevel = masterLevel.coerceIn(0f, 1f)
        val movRadius = windowWidth * 0.40f

        // Head bubble driven by audio
        val angle = Math.PI.toFloat() * mainLevel * 2f - (Math.PI.toFloat() / 2f)
        bubbles[0].x = movRadius * cos(angle.toDouble()).toFloat()
        bubbles[0].y = movRadius * sin(angle.toDouble()).toFloat()

        // Softer, warm head bubble color (similar to original)
        val hue = 10f + 150f * pseudoNoise(noiseSeed + 10f)
        val c = Color.getHSBColor((hue / 360f).coerceIn(0f, 1f), 0.4f, 1.0f)
        bubbles[0].color = c
        bubbles[0].radius = mainLevel * (rippleSize + 0.02f * windowWidth)

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

    /**
     * Starts the audio capture thread and computes simple per-band levels.
     */
    private fun startAudioCapture() {
        try {
            val format = AudioFormat(44100f, 16, 1, true, false)
            val info = javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, format)
            val targetLine = AudioSystem.getLine(info) as TargetDataLine
            val bufferSize = 2048
            targetLine.open(format, bufferSize * 2)
            targetLine.start()

            line = targetLine
            running = true

            audioThread = thread(start = true, isDaemon = true, name = "SoundBubble-Audio") {
                val byteBuffer = ByteArray(bufferSize * 2)
                val perBandSq = FloatArray(particleCount)
                val perBandCount = IntArray(particleCount)

                while (running) {
                    val read = targetLine.read(byteBuffer, 0, byteBuffer.size)
                    if (read <= 0) continue

                    var idx = 0
                    var sumSq = 0f
                    var sampleIndex = 0
                    perBandSq.fill(0f)
                    perBandCount.fill(0)

                    while (idx + 1 < read) {
                        val lo = byteBuffer[idx].toInt() and 0xFF
                        val hi = byteBuffer[idx + 1].toInt()
                        val sample = ((hi shl 8) or lo) / 32768.0f
                        idx += 2

                        sumSq += sample * sample
                        val band = (sampleIndex * particleCount / (bufferSize)).coerceIn(0, particleCount - 1)
                        perBandSq[band] += sample * sample
                        perBandCount[band]++
                        sampleIndex++
                    }

                    val totalSamples = max(1, sampleIndex)
                    val rms = sqrt(sumSq / totalSamples)
                    val newMaster = (ln(1f + rms * 50f)).coerceIn(0f, 1f)
                    masterLevel = masterLevel * 0.7f + newMaster * 0.3f

                    var maxBand = 1e-6f
                    for (b in 0 until particleCount) {
                        if (perBandCount[b] > 0) {
                            val rmsBand = sqrt(perBandSq[b] / perBandCount[b])
                            bandLevels[b] = rmsBand
                            if (rmsBand > maxBand) maxBand = rmsBand
                        } else {
                            bandLevels[b] = 0f
                        }
                    }

                    if (maxBand < 1e-6f) maxBand = 1e-6f
                    for (b in 0 until particleCount) {
                        bandLevels[b] = (bandLevels[b] / maxBand).coerceIn(0f, 1f)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Failed to start audio capture for SoundBubble", e)
        }
    }

    override fun stop() {
        running = false
        try {
            line?.stop()
            line?.close()
        } catch (_: Exception) {
        }
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


