package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Clouds animation that creates a cloud-like effect using Perlin noise.
 * Features color cycling and smooth, organic movement patterns.
 */
class CloudsAnimation : LedAnimation {
    private var windowWidth = 0
    private var windowHeight = 0
    private var startTime: Long = 0

    private var cloudImage: BufferedImage? = null

    // Noise parameters
    private val noiseScale = 0.02f // Increased for more detailed clouds
    private val timeScale = 0.00005f // Slower movement
    private val hueScale = 0.00005f // Slower color cycling

    // Color parameters (reserved for future tweaks)
    // Currently not used directly but kept for potential tuning
    @Suppress("unused")
    private val baseHue = 0.0f

    @Suppress("unused")
    private val saturation = 80.0f

    @Suppress("unused")
    private val brightnessScale = 500.0f

    @Suppress("unused")
    private val brightnessOffset = 0.4f

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.windowWidth = width
        this.windowHeight = height
        this.startTime = System.currentTimeMillis()


        // Create cloud image buffer
        this.cloudImage = BufferedImage(CLOUD_WIDTH, CLOUD_HEIGHT, BufferedImage.TYPE_INT_RGB)

        log.debug("Clouds Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)


        // Update cloud pattern
        updateCloudPattern()


        // Draw the cloud image scaled to window size
        g.drawImage(cloudImage, 0, 0, width, height, null)


        // Update LED colors
        updateLedColors(ledGrid)


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Clouds Animation - Press ESC to exit", 10, 20)
        g.drawString("Time: " + String.format("%.1f", (System.currentTimeMillis() - startTime) / 1000.0f) + "s", 10, 35)
    }

    /**
     * Updates the cloud pattern using Perlin noise.
     */
    private fun updateCloudPattern() {
        val currentTime = System.currentTimeMillis()
        val time = (currentTime - startTime) * timeScale


        // Calculate hue cycling
        val hue = (noise(time * hueScale, 0.0f, 0.0f) * 200.0f) % 100.0f
        val z = time
        val dx = time


        // Generate cloud pattern
        for (x in 0..<CLOUD_WIDTH) {
            for (y in 0..<CLOUD_HEIGHT) {
                // Calculate noise value
                val noiseValue = noise(dx + x * noiseScale, y * noiseScale, z)


                // Create cloud-like threshold effect
                val cloudThreshold = 0.3f // Adjust this to control cloud density
                val cloudValue = max(0f, (noiseValue - cloudThreshold) / (1.0f - cloudThreshold))


                // Convert to brightness with better cloud shape (darker base)
                var brightness = cloudValue * 50.0f + 5.0f // Range 5-55 (much darker)
                brightness = max(0f, min(100f, brightness)) // Clamp to 0-100


                // Calculate saturation (higher for brighter areas)
                var sat = 60.0f + (cloudValue * 40.0f) // Range 60-100
                sat = max(0f, min(100f, sat)) // Clamp to 0-100


                // Convert HSB to RGB
                val color = Color.getHSBColor(hue / 100.0f, sat / 100.0f, brightness / 100.0f)


                // Set pixel color
                cloudImage!!.setRGB(x, y, color.getRGB())
            }
        }
    }

    /**
     * Improved Perlin noise implementation for cloud effects.
     * Creates proper cloud-like patterns with multiple octaves.
     */
    private fun noise(x: Float, y: Float, z: Float): Float {
        // Multiple octaves of noise for realistic cloud patterns
        var noise = 0.0f
        var amplitude = 1.0f
        var frequency = 1.0f


        // Add multiple octaves
        for (i in 0..3) {
            noise += amplitude * smoothNoise(x * frequency, y * frequency, z * frequency)
            amplitude *= 0.5f
            frequency *= 2.0f
        }


        // Normalize to 0-1 range
        return max(0.0f, min(1.0f, noise * 0.5f + 0.5f))
    }

    /**
     * Smooth noise function using interpolation.
     */
    private fun smoothNoise(x: Float, y: Float, z: Float): Float {
        // Get integer and fractional parts
        val xi = floor(x.toDouble()).toInt()
        val yi = floor(y.toDouble()).toInt()
        val zi = floor(z.toDouble()).toInt()

        val fx = x - xi
        val fy = y - yi
        val fz = z - zi


        // Smooth interpolation weights
        val u = fx * fx * (3.0f - 2.0f * fx)
        val v = fy * fy * (3.0f - 2.0f * fy)
        val w = fz * fz * (3.0f - 2.0f * fz)


        // Get noise values at 8 corners of cube
        val n000 = randomNoise(xi, yi, zi)
        val n001 = randomNoise(xi, yi, zi + 1)
        val n010 = randomNoise(xi, yi + 1, zi)
        val n011 = randomNoise(xi, yi + 1, zi + 1)
        val n100 = randomNoise(xi + 1, yi, zi)
        val n101 = randomNoise(xi + 1, yi, zi + 1)
        val n110 = randomNoise(xi + 1, yi + 1, zi)
        val n111 = randomNoise(xi + 1, yi + 1, zi + 1)


        // Trilinear interpolation
        val nx00 = lerp(n000, n100, u)
        val nx01 = lerp(n001, n101, u)
        val nx10 = lerp(n010, n110, u)
        val nx11 = lerp(n011, n111, u)

        val nxy0 = lerp(nx00, nx10, v)
        val nxy1 = lerp(nx01, nx11, v)

        return lerp(nxy0, nxy1, w)
    }

    /**
     * Linear interpolation helper.
     */
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + t * (b - a)
    }

    /**
     * Pseudo-random noise function.
     */
    private fun randomNoise(x: Int, y: Int, z: Int): Float {
        // Simple hash function for pseudo-random values
        var n = x + y * 57 + z * 131
        n = (n shl 13) xor n
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) and 0x7fffffff) / 1073741824.0f)
    }

    /**
     * Updates LED colors based on the current cloud pattern.
     */
    private fun updateLedColors(ledGrid: LedGrid) {
        // Clear all LEDs first
        ledGrid.clearAllLeds()


        // Map cloud pattern to LED grids
        mapCloudsToLedGrid(ledGrid)
    }

    /**
     * Maps the cloud pattern to the LED grid using window coordinates.
     */
    private fun mapCloudsToLedGrid(ledGrid: LedGrid) {
        // Map cloud pattern to all grids in the layout
        for (gridIndex in 0 until ledGrid.gridCount) {
            val gridConfig = ledGrid.getGridConfig(gridIndex) ?: continue
            for (y in 0 until ledGrid.gridSize) {
                for (x in 0 until ledGrid.gridSize) {
                    // Calculate window coordinates for this grid
                    val windowX = gridConfig.x + x * ledGrid.pixelSize + ledGrid.pixelSize / 2
                    val windowY = gridConfig.y + y * ledGrid.pixelSize + ledGrid.pixelSize / 2


                    // Calculate position in cloud image
                    var cloudX: Int = (windowX * CLOUD_WIDTH) / windowWidth
                    var cloudY: Int = (windowY * CLOUD_HEIGHT) / windowHeight


                    // Ensure coordinates are within bounds
                    cloudX = max(0, min(cloudX, CLOUD_WIDTH - 1))
                    cloudY = max(0, min(cloudY, CLOUD_HEIGHT - 1))


                    // Get color from cloud image
                    val ledColor = Color(cloudImage!!.getRGB(cloudX, cloudY))

                    // Use standard logical LED coordinates (x = left->right, y = top->bottom)
                    // so mapping is consistent with other animations and LedGrid packing.
                    ledGrid.setLedColor(gridIndex, x, y, ledColor)
                }
            }
        }
    }

    override fun getName(): String {
        return "Clouds Animation"
    }

    override fun getDescription(): String {
        return "Cloud-like patterns using Perlin noise with color cycling and organic movement"
    }

    companion object {
        // Cloud rendering parameters
        private const val CLOUD_WIDTH = 128
        private const val CLOUD_HEIGHT = 128
        private val log: Logger = LogManager.getLogger(CloudsAnimation::class.java)
    }
}
