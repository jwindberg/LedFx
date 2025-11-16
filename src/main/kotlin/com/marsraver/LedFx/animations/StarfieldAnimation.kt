package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import java.awt.Color
import java.awt.Graphics2D
import java.util.*
import kotlin.math.*

/**
 * Star Trek warp speed starfield animation.
 * Stars move from the center toward the edges at increasing speeds.
 */
class StarfieldAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var stars: MutableList<Star>? = null
    private var random: Random? = null

    private inner class Star(
        var x: Float, // Normalized position (-1 to 1)
        var y: Float,
        var speed: Float,
        var color: Color
    ) {
        var size: Int
        var trailX: FloatArray = FloatArray(TRAIL_LENGTH)
        var trailY: FloatArray = FloatArray(TRAIL_LENGTH)

        init {
            this.size = if (random!!.nextFloat() < 0.2f) 2 else 1 // 20% chance for larger star

            // Initialize trail history at starting position
            for (i in 0 until TRAIL_LENGTH) {
                trailX[i] = x
                trailY[i] = y
            }
        }

        fun update(centerX: Float, centerY: Float, maxSpeed: Float) {
            // Move toward edges
            val dx = x - centerX
            val dy = y - centerY
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (dist > 0.01f) {
                x += dx / dist * speed * maxSpeed
                y += dy / dist * speed * maxSpeed
            }


            // Reset if outside bounds
            if (abs(x) > 1.5f || abs(y) > 1.5f) {
                // Start from center with random direction
                val angle = random!!.nextFloat() * 2 * Math.PI.toFloat()
                val radius = 0.1f
                x = centerX + cos(angle.toDouble()).toFloat() * radius
                y = centerY + sin(angle.toDouble()).toFloat() * radius
                speed = 0.02f + random!!.nextFloat() * 0.03f
            }

            // Update trail history (store most recent positions)
            for (i in TRAIL_LENGTH - 1 downTo 1) {
                trailX[i] = trailX[i - 1]
                trailY[i] = trailY[i - 1]
            }
            trailX[0] = x
            trailY[0] = y
        }
    }

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.windowWidth = width
        this.windowHeight = height
        this.ledGrid = ledGrid
        this.random = Random()
        this.stars = ArrayList<Star>()


        // Create 100 stars starting from random positions (half the original)
        val centerX = 0f
        val centerY = 0f
        for (i in 0..99) {
            val angle = random!!.nextFloat() * 2 * Math.PI.toFloat()
            val radius = 0.05f + random!!.nextFloat() * 1.0f
            val x = (centerX + cos(angle.toDouble()) * radius).toFloat()
            val y = (centerY + sin(angle.toDouble()) * radius).toFloat()
            val speed = 0.02f + random!!.nextFloat() * 0.03f
            // Richer star palette: mostly white, with some colored stars
            val roll = random!!.nextFloat()
            val color: Color
            if (roll < 0.55f) {
                color = Color.WHITE
            } else if (roll < 0.70f) {
                color = Color.CYAN
            } else if (roll < 0.82f) {
                color = Color.MAGENTA
            } else if (roll < 0.90f) {
                color = Color.YELLOW
            } else {
                // Random warm/cool hue for variety
                val hue = random!!.nextFloat() // 0‑1
                color = Color.getHSBColor(hue, 0.2f + 0.6f * random!!.nextFloat(), 0.7f + 0.3f * random!!.nextFloat())
            }
            stars!!.add(Star(x, y, speed, color))
        }
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)

        val centerX = 0f
        val centerY = 0f
        // Reduce overall star speed again (quarter of original)
        val maxSpeed = 0.25f


        // Update and draw stars
        for (star in stars!!) {
            star.update(centerX, centerY, maxSpeed)


            // Convert normalized coordinates to screen coordinates
            val screenX = ((star.x + 1) * width / 2).toInt()
            val screenY = ((star.y + 1) * height / 2).toInt()


            // Calculate brightness based on distance from center (for on-screen view)
            val dx = star.x - centerX
            val dy = star.y - centerY
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val brightness = min(1.0f, max(0.0f, 0.3f + dist * 0.7f))

            var rHead = (star.color.getRed() * brightness).toInt()
            var gHead = (star.color.getGreen() * brightness).toInt()
            var bHead = (star.color.getBlue() * brightness).toInt()
            rHead = max(0, min(255, rHead))
            gHead = max(0, min(255, gHead))
            bHead = max(0, min(255, bHead))
            val starColor = Color(rHead, gHead, bHead)

            // Draw core
            g.setColor(starColor)

            if (star.size > 1) {
                g.fillOval(screenX - 1, screenY - 1, 3, 3)
            } else {
                g.fillRect(screenX, screenY, 1, 1)
            }

            // Draw a short fading trail behind the star using its recent positions
            for (i in 1 until TRAIL_LENGTH) {
                val txNorm = star.trailX[i]
                val tyNorm = star.trailY[i]
                val trailX = ((txNorm + 1) * width / 2).toInt()
                val trailY = ((tyNorm + 1) * height / 2).toInt()

                // Fade trail brightness over history; i=1 is brightest, last is dimmest
                val trailFactor = (TRAIL_LENGTH - i) / TRAIL_LENGTH.toFloat()
                var trailBrightness = brightness * trailFactor * 1.2f
                trailBrightness = min(1.0f, max(0.0f, trailBrightness))

                if (trailBrightness > 0.01f) {
                    var rTrail = (star.color.getRed() * trailBrightness).toInt()
                    var gTrail = (star.color.getGreen() * trailBrightness).toInt()
                    var bTrail = (star.color.getBlue() * trailBrightness).toInt()
                    rTrail = max(0, min(255, rTrail))
                    gTrail = max(0, min(255, gTrail))
                    bTrail = max(0, min(255, bTrail))
                    val trailColor = Color(rTrail, gTrail, bTrail)
                    g.setColor(trailColor)
                    if (star.size > 1) {
                        g.fillOval(trailX - 1, trailY - 1, 3, 3)
                    } else {
                        g.fillRect(trailX, trailY, 1, 1)
                    }
                }
            }
        }


        // Map to LEDs
        mapToLeds(g, ledGrid)
    }

    private fun mapToLeds(g: Graphics2D?, ledGrid: LedGrid) {
        val gridCount = ledGrid.gridCount

        // Clear all grids so only the current frame's stars remain lit
        ledGrid.clearAllLeds()

        for (gridIndex in 0 until gridCount) {
            val gridConfig = ledGrid.getGridConfig(gridIndex) ?: continue
            val gridSize = gridConfig.gridSize
            val pixelSize = gridConfig.pixelSize


            // Sample each LED position
            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    // Sample at the center of each LED cell in window coordinates
                    val windowX = gridConfig.x + x * pixelSize + pixelSize / 2
                    val windowY = gridConfig.y + y * pixelSize + pixelSize / 2

                    if (windowX >= 0 && windowX < windowWidth && windowY >= 0 && windowY < windowHeight) {
                        val ledColor = sampleStarColor(windowX, windowY)

                        if (ledColor != null && ledColor.getRGB() != Color.BLACK.getRGB()) {
                            // Standard logical coordinates: x = left->right, y = top->bottom
                            ledGrid.setLedColor(gridIndex, x, y, ledColor)
                        }
                    }
                }
            }
        }
    }

    private fun sampleStarColor(screenX: Int, screenY: Int): Color? {
        // Convert screen coordinates to normalized coordinates
        val normalizedX = (screenX / windowWidth.toFloat()) * 2 - 1
        val normalizedY = (screenY / windowHeight.toFloat()) * 2 - 1


        // Check if any star head or trail is at this position
        for (star in stars!!) {
            // First, check the star head
            run {
                val dx = normalizedX - star.x
                val dy = normalizedY - star.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val starRadius = star.size * 0.02f // slightly larger for better LED hit
                if (dist < starRadius) {
                    val centerX = 0f
                    val centerY = 0f
                    val starDx = star.x - centerX
                    val starDy = star.y - centerY
                    val starDist = sqrt((starDx * starDx + starDy * starDy).toDouble()).toFloat()
                    // LED-only brightness curve: capped lower and globally dimmed
                    val brightness = min(0.7f, max(0.0f, 0.2f + starDist * 0.4f))
                    val dim = 0.4f
                    val finalBrightness = brightness * dim

                    var r = (star.color.getRed() * finalBrightness).toInt()
                    var g = (star.color.getGreen() * finalBrightness).toInt()
                    var b = (star.color.getBlue() * finalBrightness).toInt()
                    r = max(0, min(255, r))
                    g = max(0, min(255, g))
                    b = max(0, min(255, b))

                    return Color(r, g, b)
                }
            }

            // Then, check the short fading trail positions
                for (i in 1 until TRAIL_LENGTH) {
                val tx = star.trailX[i]
                val ty = star.trailY[i]
                val dx = normalizedX - tx
                val dy = normalizedY - ty
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val trailRadius = star.size * 0.02f

                if (dist < trailRadius) {
                    val centerX = 0f
                    val centerY = 0f
                    val starDx = tx - centerX
                    val starDy = ty - centerY
                    val starDist = sqrt((starDx * starDx + starDy * starDy).toDouble()).toFloat()
                    val baseBrightness = min(0.7f, max(0.0f, 0.2f + starDist * 0.4f))
                    val trailFactor = (TRAIL_LENGTH - i) / TRAIL_LENGTH.toFloat()
                    val brightness = baseBrightness * trailFactor * 0.9f
                    val dim = 0.4f
                    val finalBrightness = brightness * dim

                    if (finalBrightness > 0.01f) {
                        var r = (star.color.getRed() * finalBrightness).toInt()
                        var g = (star.color.getGreen() * finalBrightness).toInt()
                        var b = (star.color.getBlue() * finalBrightness).toInt()
                        r = max(0, min(255, r))
                        g = max(0, min(255, g))
                        b = max(0, min(255, b))

                        return Color(r, g, b)
                    }
                }
            }
        }

        return Color.BLACK
    }

    override fun getName(): String {
        return "Starfield"
    }

    override fun getDescription(): String {
        return "Star Trek warp speed starfield effect"
    }

    override fun stop() {
        // No cleanup needed
    }

    companion object {
        // Trail length for each star; longer history = longer visible tail
        private const val TRAIL_LENGTH = 40
    }
}
