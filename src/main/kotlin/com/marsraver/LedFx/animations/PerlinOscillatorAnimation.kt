package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D
import java.util.*
import kotlin.math.*

/**
 * Perlin Oscillator animation - creates oscillating circles with Perlin noise-driven motion.
 * Based on the Processing sketch by aa_debdeb.
 */
class PerlinOscillatorAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null

    @Suppress("unused")
    private var windowWidth = 0

    @Suppress("unused")
    private var windowHeight = 0

    @Suppress("unused")
    private var startTime: Long = 0

    // Oscillator parameters
    private var noiseX = 0f
    private var noiseY = 0f
    private var oscillators: ArrayList<Oscillator>? = null
    private var random: Random? = null

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height
        this.startTime = System.currentTimeMillis()


        // Initialize random generator
        random = Random()


        // Initialize noise offsets
        noiseX = random!!.nextFloat() * 100
        noiseY = random!!.nextFloat() * 100


        // Create oscillators in a grid
        oscillators = ArrayList<Oscillator>()
        var x = 0
        while (x <= width) {
            var y = 0
            while (y <= height) {
                oscillators!!.add(Oscillator(x, y))
                y += SPACING
            }
            x += SPACING
        }

        log.debug("Perlin Oscillator Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
        log.debug("Oscillators created: {}", oscillators!!.size)
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)


        // Update noise parameters
        noiseX += 0.01f
        noiseY += 0.01f


        // Draw all oscillators
        for (osc in oscillators!!) {
            osc.display(g, noiseX, noiseY)
        }


        // Update LED colors
        updateLedColors(ledGrid)


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Perlin Oscillator - Press ESC to exit", 10, 20)
        g.drawString("Oscillators: " + oscillators!!.size, 10, 35)
    }

    /**
     * Updates LED colors from the current frame.
     */
    private fun updateLedColors(ledGrid: LedGrid) {
        // The oscillators are drawn directly, and we'll sample them
        // to update LED colors via the mapToLeds method
        mapToLeds(ledGrid)
    }

    /**
     * Maps the current graphics to LED colors.
     * This samples the rendered oscillators to set LED colors.
     */
    private fun mapToLeds(ledGrid: LedGrid) {
        val numGrids = ledGrid.gridCount

        for (gridIndex in 0 until numGrids) {
            val grid = ledGrid.getGridConfig(gridIndex) ?: continue
            val gridSize = grid.gridSize
            val pixelSize = grid.pixelSize


            // For each LED in the grid
            for (x in 0 until gridSize) {
                for (y in 0 until gridSize) {
                    // Calculate window coordinates for this LED
                    val windowX = grid.x + x * pixelSize + pixelSize / 2
                    val windowY = grid.y + y * pixelSize + pixelSize / 2


                    // Sample color from oscillators at this position
                    val sampledColor = sampleColorAt(windowX, windowY)

                    // Use standard logical LED coordinates (x = left->right, y = top->bottom)
                    // so mapping is consistent with other animations and LedGrid packing.
                    ledGrid.setLedColor(gridIndex, x, y, sampledColor)
                }
            }
        }
    }

    /**
     * Samples the color at a specific window coordinate by checking nearby oscillators.
     */
    private fun sampleColorAt(windowX: Int, windowY: Int): Color? {
        // Find the closest oscillator
        var closest: Oscillator? = null
        var minDist = Float.Companion.MAX_VALUE

        for (osc in oscillators!!) {
            val dist = sqrt(
                ((osc.x - windowX) * (osc.x - windowX) +
                        (osc.y - windowY) * (osc.y - windowY)).toDouble()
            ).toFloat()
            if (dist < minDist) {
                minDist = dist
                closest = osc
            }
        }

        if (closest != null) {
            // Check if the point is within the oscillator's current size
            val currentSize = closest.getCurrentSize()
            if (minDist <= currentSize / 2) {
                return closest.currentColor
            }
        }

        return Color.BLACK
    }

    override fun getName(): String {
        return "Perlin Oscillator"
    }

    override fun getDescription(): String {
        return "Oscillating circles with Perlin noise-driven motion"
    }

    /**
     * Inner class representing a single oscillator.
     */
    private inner class Oscillator(x: Int, y: Int) {
        var x: Float
        var y: Float
        var rad: Float

        init {
            this.x = x.toFloat()
            this.y = y.toFloat()
            this.rad = random!!.nextFloat() * (2 * Math.PI).toFloat()
        }

        fun display(g: Graphics2D, noiseX: Float, noiseY: Float) {
            // Calculate current diameter based on sine wave
            val diameter = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 10f, 24f)


            // Calculate color based on sine wave
            var r = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 0f, 255f).toInt()
            var gVal = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 139f, 20f).toInt()
            var b = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 139f, 147f).toInt()


            // Clamp color values
            r = max(0, min(255, r))
            gVal = max(0, min(255, gVal))
            b = max(0, min(255, b))

            val color = Color(r, gVal, b)
            g.setColor(color)


            // Draw the ellipse
            val ellipse = Ellipse2D.Double(
                (x - diameter / 2).toDouble(),
                (y - diameter / 2).toDouble(),
                diameter.toDouble(),
                diameter.toDouble()
            )
            g.fill(ellipse)


            // Update radius based on Perlin noise
            val noiseValue = noise(noiseX + x * 0.05f, noiseY + y * 0.05f, 0.0f)
            rad += map(noiseValue, 0f, 1f, (Math.PI / 128).toFloat(), (Math.PI / 6).toFloat())


            // Keep radius between 0 and 2*PI
            if (rad > 2 * Math.PI) {
                rad -= (2 * Math.PI).toFloat()
            }
        }

        /**
         * Gets the current size of the oscillator.
         */
        fun getCurrentSize(): Float {
            return map(sin(rad.toDouble()).toFloat(), -1f, 1f, 10f, 24f)
        }

        val currentColor: Color
            /**
             * Gets the current color of the oscillator.
             */
            get() {
                var r = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 0f, 255f).toInt()
                var gVal = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 139f, 20f).toInt()
                var b = map(sin(rad.toDouble()).toFloat(), -1f, 1f, 139f, 147f).toInt()

                r = max(0, min(255, r))
                gVal = max(0, min(255, gVal))
                b = max(0, min(255, b))

                return Color(r, gVal, b)
            }
    }

    /**
     * Maps a value from one range to another (Processing map function).
     */
    private fun map(value: Float, start1: Float, stop1: Float, start2: Float, stop2: Float): Float {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1))
    }

    private fun noise(x: Float, y: Float, z: Float): Float {
        // Simple pseudo-Perlin noise using fractals
        var result = 0.0f
        var amplitude = 1.0f
        var frequency = 0.01f

        for (i in 0..3) {
            result += amplitude * smoothRandom(x * frequency, y * frequency, z * frequency)
            amplitude *= 0.5f
            frequency *= 2.0f
        }

        return result * 0.5f + 0.5f // Normalize to 0-1
    }

    private fun smoothRandom(x: Float, y: Float, z: Float): Float {
        val ix = floor(x.toDouble()).toInt()
        val iy = floor(y.toDouble()).toInt()
        val iz = floor(z.toDouble()).toInt()

        var fx = (x - ix)
        var fy = (y - iy)
        var fz = (z - iz)

        fx = fx * fx * (3 - 2 * fx) // Smoothstep
        fy = fy * fy * (3 - 2 * fy)
        fz = fz * fz * (3 - 2 * fz)

        val v000 = hash(ix, iy, iz)
        val v100 = hash(ix + 1, iy, iz)
        val v010 = hash(ix, iy + 1, iz)
        val v110 = hash(ix + 1, iy + 1, iz)
        val v001 = hash(ix, iy, iz + 1)
        val v101 = hash(ix + 1, iy, iz + 1)
        val v011 = hash(ix, iy + 1, iz + 1)
        val v111 = hash(ix + 1, iy + 1, iz + 1)

        val v00 = lerp(v000, v100, fx)
        val v01 = lerp(v010, v110, fx)
        val v10 = lerp(v001, v101, fx)
        val v11 = lerp(v011, v111, fx)

        val v0 = lerp(v00, v01, fy)
        val v1 = lerp(v10, v11, fy)

        return lerp(v0, v1, fz)
    }

    private fun hash(x: Int, y: Int, z: Int): Float {
        var n = x + y * 57 + z * 131
        n = (n shl 13) xor n
        return ((n * (n * n * 15731 + 789221) + 1376312589) and 0x7fffffff) / 2147483647.0f
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + t * (b - a)
    }

    companion object {
        // Spacing between oscillators
        private const val SPACING = 25
        private val log: Logger = LogManager.getLogger(PerlinOscillatorAnimation::class.java)
    }
}
