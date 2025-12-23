package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.image.BufferedImage
import kotlin.math.sqrt

/**
 * Spinning beachball animation with rainbow colored segments.
 */
class SpinningBeachballAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null
    private var lastTime: Long = 0
    private var rotation = 0f

    @Suppress("unused")
    private var windowWidth = 0

    @Suppress("unused")
    private var windowHeight = 0

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height
        this.lastTime = System.currentTimeMillis()

        log.debug("Spinning Beachball Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)


        // Update rotation for spinning effect
        val currentTime = System.currentTimeMillis()
        val timeDelta = (currentTime - lastTime) / 1000.0f
        lastTime = currentTime
        rotation -= timeDelta * 90 // 90 degrees per second
        if (rotation < 0) rotation += 360f


        // Calculate beachball size
        val beachballSize = 480

        // Center the beachball in the LED grid area
        // Grid01 and Grid02 start at y=45, Grid03 and Grid04 end at y=525 (285+240)
        // So the visual center is at (45 + 525) / 2 = 285
        val centerX = width / 2
        val centerY = 285


        // Draw the spinning beachball
        drawBeachball(g, centerX, centerY, beachballSize, rotation)


        // Map to LEDs by sampling the rendered canvas
        mapToLeds(centerX, centerY, beachballSize / 2, width, height)
    }

    /**
     * Draws the spinning beachball with colored segments.
     */
    private fun drawBeachball(g: Graphics2D, centerX: Int, centerY: Int, size: Int, rotation: Float) {
        val radius = size / 2


        // Enable anti-aliasing for smooth edges
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)


        // Draw each colored segment (60 degrees each for 6 segments)
        val segmentAngle: Int = 360 / BEACHBALL_COLORS.size
        for (i in BEACHBALL_COLORS.indices) {
            g.setColor(BEACHBALL_COLORS[i])


            // Calculate segment angles (60 degrees each for 6 segments)
            val startAngle = (i * segmentAngle) + rotation
            val arcAngle = segmentAngle.toFloat()


            // Draw the arc segment
            val arc: Arc2D = Arc2D.Double(
                (centerX - radius).toDouble(), (centerY - radius).toDouble(), size.toDouble(), size.toDouble(),
                startAngle.toDouble(), arcAngle.toDouble(), Arc2D.PIE
            )
            g.fill(arc)
        }
    }

    /**
     * Maps the beachball to LEDs by sampling the rendered Graphics2D output.
     */
    private fun mapToLeds(centerX: Int, centerY: Int, radius: Int, width: Int, height: Int) {
        val gridSize = ledGrid!!.gridSize
        val pixelSize = ledGrid!!.pixelSize
        val gridCount = ledGrid!!.gridCount


        // Clear all grids
        for (i in 0 until gridCount) {
            ledGrid!!.clearGrid(i)
        }


        // Create a BufferedImage to sample from the current Graphics2D output
        val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val canvasG = canvas.createGraphics()


        // Draw the beachball to the BufferedImage (same as what's shown on screen)
        canvasG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        canvasG.setColor(Color.BLACK)
        canvasG.fillRect(0, 0, width, height)
        drawBeachball(canvasG, centerX, centerY, radius * 2, rotation)
        canvasG.dispose()


        // For each grid, check each LED position.
        // We now treat (x,y) in the same way as TestAnimation and packing:
        // x = 0 is left, y = 0 is top.
        for (gridIndex in 0 until gridCount) {
            val gridConfig = ledGrid!!.getGridConfig(gridIndex) ?: continue

            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    // Map LED coordinates to window coordinates
                    val windowX = gridConfig.x + x * pixelSize + pixelSize / 2
                    val windowY = gridConfig.y + y * pixelSize + pixelSize / 2


                    // Calculate distance from beachball center
                    val dx = windowX - centerX
                    val dy = windowY - centerY
                    val distance = sqrt((dx * dx + dy * dy).toDouble())


                    // Check if LED is within the beachball
                    if (distance <= radius) {
                        // Sample the color from the rendered canvas at this position
                        val rgb = canvas.getRGB(windowX, windowY)
                        val color = Color(rgb)


                        // Display all colors without filter
                        ledGrid!!.setLedColor(gridIndex, x, y, color)
                    }
                }
            }
        }


        // Send to devices
        ledGrid!!.sendToDevices()
    }

    override fun getName(): String {
        return "Spinning Beachball"
    }

    override fun getDescription(): String {
        return "A colorful spinning beachball with 6 colored segments"
    }

    companion object {
        // Beachball colors (six bright, vibrant colors)
        private val BEACHBALL_COLORS = arrayOf<Color?>(
            Color(255, 0, 0),  // Red
            Color(255, 128, 0),  // Orange
            Color(255, 255, 0),  // Yellow
            Color(0, 255, 0),  // Green
            Color(0, 0, 255),  // Blue
            Color(128, 0, 255) // Purple
        )
        private val log: Logger = LogManager.getLogger(SpinningBeachballAnimation::class.java)
    }
}



