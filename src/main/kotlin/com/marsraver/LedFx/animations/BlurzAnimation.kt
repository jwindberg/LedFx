package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Blurz animation based on WLED's Blurz effect.
 * Creates a blurry, color-mixed effect with organic movement.
 * Features color mixing and soft blur effects.
 */
class BlurzAnimation : LedAnimation {
    private var lastTime: Long = 0
    private var time = 0f
    private var random: Random? = null

    // Blur properties
    private var backBuffer: BufferedImage? = null
    private var blurBuffer: BufferedImage? = null
    private var backGraphics: Graphics2D? = null

    // Color mixing
    private var hue = 0.0f
    private val hueSpeed = 20.0f
    private var pulseCount = 0

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.lastTime = System.currentTimeMillis()
        this.random = Random()


        // Create buffers for blur effect
        this.backBuffer = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        this.blurBuffer = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        this.backGraphics = backBuffer!!.createGraphics()


        // Enable anti-aliasing
        backGraphics!!.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        backGraphics!!.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        log.debug("Blurz Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Update time
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime) / 1000.0f
        lastTime = currentTime
        time += deltaTime


        // Update color hue
        hue += deltaTime * hueSpeed
        if (hue > 360) hue -= 360f


        // Draw to back buffer first
        drawToBackBuffer(width, height)


        // Apply blur effect
        applyBlur(width, height)


        // Draw blurred result to main graphics
        g.drawImage(blurBuffer, 0, 0, null)


        // Add fresh colored pulses
        addColorPulses(g, width, height)


        // Map to LEDs
        mapToLeds(g, width, height, ledGrid)


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Blurz - Press ESC to exit", 10, 20)
    }

    /**
     * Draws the base pattern to the back buffer.
     */
    private fun drawToBackBuffer(width: Int, height: Int) {
        // Semi-transparent fade for trail effect
        backGraphics!!.setColor(Color(0, 0, 0, 200))
        backGraphics!!.fillRect(0, 0, width, height)


        // Draw blurred motion trails
        for (i in 0..4) {
            val phase = (time * (0.5f + i * 0.2f) + i * 60.0f) % (Math.PI * 2).toFloat()
            val x = width / 2.0f + (cos(phase.toDouble()) * (width / 3.0f)).toFloat()
            val y = height / 2.0f + (sin(phase.toDouble()) * (height / 3.0f)).toFloat()

            val hueValue = ((hue + i * 72.0f) % 360.0f) / 360.0f
            val trailColor = Color.getHSBColor(hueValue, 0.8f, 0.7f)

            backGraphics!!.setColor(trailColor)
            val size = 40 + i * 10
            backGraphics!!.fillOval(x.toInt() - size / 2, y.toInt() - size / 2, size, size)
        }
    }

    /**
     * Applies blur effect using convolution.
     */
    private fun applyBlur(width: Int, height: Int) {
        // Create blur kernel (9x9 Gaussian-like blur)
        val matrix = floatArrayOf(
            0.01f, 0.02f, 0.03f, 0.02f, 0.01f,
            0.02f, 0.04f, 0.06f, 0.04f, 0.02f,
            0.03f, 0.06f, 0.10f, 0.06f, 0.03f,
            0.02f, 0.04f, 0.06f, 0.04f, 0.02f,
            0.01f, 0.02f, 0.03f, 0.02f, 0.01f
        )

        val kernel = Kernel(5, 5, matrix)
        val blurOp = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
        blurOp.filter(backBuffer, blurBuffer)
    }

    /**
     * Adds fresh color pulses for brightness.
     */
    private fun addColorPulses(g: Graphics2D, width: Int, height: Int) {
        pulseCount++


        // Add pulses periodically
        if (pulseCount % 60 == 0) {
            val hueValue = (hue % 360.0f) / 360.0f
            val pulseColor = Color.getHSBColor(hueValue, 1.0f, 1.0f)


            // Random position
            val x = random!!.nextInt(width)
            val y = random!!.nextInt(height)


            // Draw bright pulse
            g.setColor(
                Color(
                    pulseColor.getRed(), pulseColor.getGreen(),
                    pulseColor.getBlue(), 200
                )
            )
            g.fillOval(x - 20, y - 20, 40, 40)


            // Outer glow
            g.setColor(
                Color(
                    pulseColor.getRed(), pulseColor.getGreen(),
                    pulseColor.getBlue(), 100
                )
            )
            g.fillOval(x - 30, y - 30, 60, 60)
        }
    }

    /**
     * Maps the blurz effect to LEDs.
     */
    private fun mapToLeds(g: Graphics2D?, width: Int, height: Int, ledGrid: LedGrid) {
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize
        val gridCount = ledGrid.gridCount


        // Clear all grids
        for (i in 0 until gridCount) {
            ledGrid.clearGrid(i)
        }


        // Sample colors from the blurred image
        for (gridIndex in 0 until gridCount) {
            val gridConfig = ledGrid.getGridConfig(gridIndex) ?: continue

            for (y in 0..<gridSize) {
                for (x in 0..<gridSize) {
                    // Calculate window coordinates for this LED
                    val windowX = gridConfig.x + x * pixelSize + pixelSize / 2
                    val windowY = gridConfig.y + y * pixelSize + pixelSize / 2


                    // Sample color from blurred buffer
                    if (windowX >= 0 && windowX < width && windowY >= 0 && windowY < height) {
                        val rgb = blurBuffer!!.getRGB(windowX, windowY)
                        val ledColor = Color(rgb)


                        // Only set if not black
                        if (ledColor.getRed() > 5 || ledColor.getGreen() > 5 || ledColor.getBlue() > 5) {
                            // Standard logical coordinates: x = left->right, y = top->bottom
                            ledGrid.setLedColor(gridIndex, x, y, ledColor)
                        }
                    }
                }
            }
        }


        // Send to devices
        ledGrid.sendToDevices()
    }

    override fun getName(): String {
        return "Blurz"
    }

    override fun getDescription(): String {
        return "Blurry color-mixed effect with organic movement (WLED-style)"
    }

    override fun stop() {
        if (backGraphics != null) {
            backGraphics!!.dispose()
        }
    }

    companion object {
        private val log: Logger = LogManager.getLogger(BlurzAnimation::class.java)
    }
}



