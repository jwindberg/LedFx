package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Fast Plasma animation that creates liquid, organic movement patterns.
 * Uses lookup tables for fast performance with cycling colors.
 * Based on the FastPlasma Processing sketch by luis2048.
 */
class FastPlasmaAnimation : LedAnimation {
    private var windowWidth = 0
    private var windowHeight = 0
    private var startTime: Long = 0
    private var frameCount = 0

    private var plasmaImage: BufferedImage? = null

    // Lookup tables for fast plasma generation
    private val palette = IntArray(128)
    private val plasmaLookup = IntArray(PLASMA_WIDTH * PLASMA_HEIGHT)

    // Plasma parameters
    private val scale = 32.0f

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.windowWidth = width
        this.windowHeight = height
        this.startTime = System.currentTimeMillis()


        // Create plasma image buffer
        this.plasmaImage = BufferedImage(PLASMA_WIDTH, PLASMA_HEIGHT, BufferedImage.TYPE_INT_RGB)


        // Initialize palette and lookup table
        initializePalette()
        initializePlasmaLookup()
    }

    /**
     * Initializes the color palette using sine functions.
     */
    private fun initializePalette() {
        for (i in 0..127) {
            val s1 = sin(i * Math.PI / 25.0).toFloat()
            val s2 = sin(i * Math.PI / 50.0 + Math.PI / 4.0).toFloat()


            // Convert to RGB color
            var r = (128 + s1 * 128).toInt()
            var g = (128 + s2 * 128).toInt()
            var b = (s1 * 128).toInt()


            // Clamp values to 0-255
            r = max(0, min(255, r))
            g = max(0, min(255, g))
            b = max(0, min(255, b))

            palette[i] = Color(r, g, b).getRGB()
        }
    }

    /**
     * Initializes the plasma lookup table with pre-calculated values.
     */
    private fun initializePlasmaLookup() {
        for (x in 0..<PLASMA_WIDTH) {
            for (y in 0..<PLASMA_HEIGHT) {
                val index: Int = x + y * PLASMA_WIDTH


                // Calculate plasma value using multiple sine waves
                val value1 = sin((x / scale).toDouble()).toFloat()
                val value2 = cos((y / scale).toDouble()).toFloat()
                val value3 = sin(sqrt((x * x + y * y).toDouble()) / scale).toFloat()


                // Combine the values and normalize to 0-127 range
                val combined = (127.5f + 127.5f * value1) +
                        (127.5f + 127.5f * value2) +
                        (127.5f + 127.5f * value3)

                plasmaLookup[index] = (combined / 4.0f).toInt() and 127
            }
        }
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)


        // Update plasma pattern
        updatePlasmaPattern()


        // Draw the plasma image scaled to window size
        g.drawImage(plasmaImage, 0, 0, width, height, null)


        // Update LED colors
        updateLedColors(ledGrid)


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Fast Plasma Animation - Press ESC to exit", 10, 20)
        g.drawString("Frame: " + frameCount, 10, 35)
        g.drawString("Time: " + String.format("%.1f", (System.currentTimeMillis() - startTime) / 1000.0f) + "s", 10, 50)
    }

    /**
     * Updates the plasma pattern using lookup tables.
     */
    private fun updatePlasmaPattern() {
        // Update frame count for animation
        frameCount++


        // Generate plasma pattern using lookup table
        for (pixelCount in plasmaLookup.indices) {
            // Use frame count to animate the plasma
            val paletteIndex = (plasmaLookup[pixelCount] + frameCount) and 127
            plasmaImage!!.setRGB(pixelCount % PLASMA_WIDTH, pixelCount / PLASMA_WIDTH, palette[paletteIndex])
        }
    }

    /**
     * Updates LED colors based on the current plasma pattern.
     * The animation just draws to the window - the layout system handles LED mapping.
     */
    private fun updateLedColors(ledGrid: LedGrid) {
        // Clear all LEDs first
        ledGrid.clearAllLeds()


        // Map plasma pattern to LED grids using the standard mapping
        mapPlasmaToLedGrid(ledGrid)
    }

    /**
     * Maps the plasma pattern to the LED grid using the standard DualLedGrid interface.
     * This lets the layout system handle the actual LED mapping.
     */
    private fun mapPlasmaToLedGrid(ledGrid: LedGrid) {
        // Map plasma pattern to all grids in the layout
        for (gridIndex in 0 until ledGrid.gridCount) {
            val gridConfig = ledGrid.getGridConfig(gridIndex) ?: continue
            for (y in 0 until ledGrid.gridSize) {
                for (x in 0 until ledGrid.gridSize) {
                    // Calculate window coordinates for this grid
                    val windowX = gridConfig.x + x * ledGrid.pixelSize + ledGrid.pixelSize / 2
                    val windowY = gridConfig.y + y * ledGrid.pixelSize + ledGrid.pixelSize / 2


                    // Calculate position in plasma image
                    var plasmaX: Int = (windowX * PLASMA_WIDTH) / windowWidth
                    var plasmaY: Int = (windowY * PLASMA_HEIGHT) / windowHeight


                    // Ensure coordinates are within bounds
                    plasmaX = max(0, min(plasmaX, PLASMA_WIDTH - 1))
                    plasmaY = max(0, min(plasmaY, PLASMA_HEIGHT - 1))


                    // Get color from plasma image
                    val ledColor = Color(plasmaImage!!.getRGB(plasmaX, plasmaY))


                    // Use the standard logical LED coordinates (x = left->right, y = top->bottom)
                    // so all animations share the same mapping and LedGrid handles packing.
                    ledGrid.setLedColor(gridIndex, x, y, ledColor)
                }
            }
        }
    }

    override fun getName(): String {
        return "Fast Plasma Animation"
    }

    override fun getDescription(): String {
        return "Fast plasma effect with liquid, organic movement using lookup tables for optimal performance"
    }

    companion object {
        // Plasma rendering parameters
        private const val PLASMA_WIDTH = 128
        private const val PLASMA_HEIGHT = 128
        private val log: Logger = LogManager.getLogger(FastPlasmaAnimation::class.java)
    }
}
