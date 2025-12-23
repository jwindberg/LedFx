package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.*
import java.awt.geom.Ellipse2D
import java.util.*
import kotlin.math.*

/**
 * Black hole animation with accretion disk and gravitational lensing effects.
 * Particles spiral toward the event horizon, creating a mesmerizing cosmic effect.
 * Based on the WLED black hole effect pattern.
 */
class BlackHoleAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null
    private var lastTime: Long = 0
    private var time = 0f
    private var random: Random? = null

    // Black hole properties
    private var centerX = 0
    private var centerY = 0
    private val eventHorizonRadius = 30.0f
    private val outerRadius = 200.0f

    // Particle system for accretion disk
    private var particles: MutableList<Particle>? = null

    private inner class Particle(// Angular position (0-2π)
        var angle: Float, // Distance from center
        var radius: Float
    ) {
        var x: Float = 0f
        var y: Float = 0f // Position relative to center
        var angularVel: Float // Angular velocity
        var radialVel: Float // Radial velocity (inward)
        var color: Color? = null
        var brightness: Float

        init {
            this.angularVel = (0.5f + random!!.nextFloat() * 2.0f) / (radius + 30) // Faster closer to center
            this.radialVel = -0.3f - random!!.nextFloat() * 0.5f // Inward motion
            this.brightness = 0.3f + random!!.nextFloat() * 0.7f
            updateColor()
        }

        fun update() {
            // Angular motion - faster near the black hole
            angle += angularVel
            if (angle > 2 * Math.PI) angle -= (2 * Math.PI).toFloat()


            // Radial motion - spiral inward
            radius += radialVel


            // Increase speed as we get closer to event horizon
            if (radius > eventHorizonRadius) {
                radialVel *= 1.01f // Accelerate inward
                angularVel *= 1.005f // Spin faster
            }


            // Calculate position
            x = (cos(angle.toDouble()) * radius).toFloat()
            y = (sin(angle.toDouble()) * radius).toFloat()


            // Update color based on distance (redshift effect)
            updateColor()


            // Reset particle if it crosses event horizon
            if (radius < eventHorizonRadius) {
                resetParticle()
            }
        }

        fun updateColor() {
            // Color gradient: blue/white (hot) far away -> red/orange (cooler) near event horizon
            var normalizedDist = (radius - eventHorizonRadius) / (outerRadius - eventHorizonRadius)
            normalizedDist = max(0f, min(1f, normalizedDist))


            // HSV: Hue shifts from blue (240) to red (0) as we approach
            val hue = 240.0f * normalizedDist / 360.0f
            val saturation = 0.7f + 0.3f * normalizedDist
            val value = brightness * (0.6f + 0.4f * normalizedDist)


            // Convert to RGB
            color = Color.getHSBColor(hue, saturation, value)
        }

        fun resetParticle() {
            angle = random!!.nextFloat() * 2 * Math.PI.toFloat()
            radius = outerRadius - random!!.nextFloat() * 50
            angularVel = (0.5f + random!!.nextFloat() * 2.0f) / (radius + 30)
            radialVel = -0.3f - random!!.nextFloat() * 0.5f
            brightness = 0.3f + random!!.nextFloat() * 0.7f
            updateColor()
        }
    }

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.centerX = width / 2
        this.centerY = height / 2
        this.lastTime = System.currentTimeMillis()
        this.random = Random()
        this.particles = ArrayList<Particle>()


        // Initialize particles
        for (i in 0..<PARTICLE_COUNT) {
            val angle = random!!.nextFloat() * 2 * Math.PI.toFloat()
            val radius = eventHorizonRadius + random!!.nextFloat() * (outerRadius - eventHorizonRadius)
            particles!!.add(Particle(angle, radius))
        }

        log.debug("Black Hole Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)


        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)


        // Update time
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime) / 1000.0f
        lastTime = currentTime
        time += deltaTime


        // Update and draw particles
        for (p in particles!!) {
            p.update()


            // Draw particle with glow effect
            val screenX = centerX + p.x.toInt()
            val screenY = centerY + p.y.toInt()


            // Calculate size based on distance (gravitational lensing)
            val lensingFactor = 1.0f + (1.0f / (p.radius / eventHorizonRadius + 0.1f)) * 0.1f
            val size = (9 * lensingFactor).toInt() // 3x bigger particles


            // Draw glow
            g.setColor(Color(p.color!!.getRed(), p.color!!.getGreen(), p.color!!.getBlue(), 50))
            g.fill(
                Ellipse2D.Double(
                    (screenX - size).toDouble(),
                    (screenY - size).toDouble(),
                    (size * 2).toDouble(),
                    (size * 2).toDouble()
                )
            )


            // Draw core
            g.setColor(p.color)
            g.fill(
                Ellipse2D.Double(
                    (screenX - size / 2).toDouble(),
                    (screenY - size / 2).toDouble(),
                    size.toDouble(),
                    size.toDouble()
                )
            )
        }


        // Draw event horizon (black hole core)
        g.setColor(Color.BLACK)
        g.fill(
            Ellipse2D.Double(
                (centerX - eventHorizonRadius).toDouble(),
                (centerY - eventHorizonRadius).toDouble(),
                (eventHorizonRadius * 2).toDouble(),
                (eventHorizonRadius * 2).toDouble()
            )
        )


        // Draw accretion disk rings
        drawAccretionRings(g)


        // Map to LEDs
        mapToLeds()


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Black Hole - Press ESC to exit", 10, 20)
    }

    /**
     * Draws glowing accretion disk rings.
     */
    private fun drawAccretionRings(g: Graphics2D) {
        // Draw multiple rings at different radii
        for (ring in 0..2) {
            val ringRadius = eventHorizonRadius + 20 + ring * 25
            val intensity = 0.2f / (ring + 1)


            // Rotating ring color
            val hue = (time * 30.0f + ring * 60.0f) % 360.0f / 360.0f
            val ringColor = Color.getHSBColor(hue, 0.8f, intensity)


            // Draw ring segments
            for (i in 0..11) {
                val angle = (i * 360.0f / 12.0f + time * 10.0f) * Math.PI.toFloat() / 180.0f
                val x1 = centerX + cos(angle.toDouble()).toFloat() * (ringRadius - 2)
                val y1 = centerY + sin(angle.toDouble()).toFloat() * (ringRadius - 2)
                val x2 = centerX + cos(angle.toDouble()).toFloat() * (ringRadius + 2)
                val y2 = centerY + sin(angle.toDouble()).toFloat() * (ringRadius + 2)

                g.setColor(Color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), 100))
                g.setStroke(BasicStroke(2.0f))
                g.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
            }
        }
    }

    /**
     * Maps the black hole effect to LEDs by sampling colors at LED positions.
     */
    private fun mapToLeds() {
        val gridSize = ledGrid!!.gridSize
        val pixelSize = ledGrid!!.pixelSize
        val gridCount = ledGrid!!.gridCount


        // Clear all grids
        for (i in 0 until gridCount) {
            ledGrid!!.clearGrid(i)
        }


        // Sample colors at each LED position
        for (gridIndex in 0 until gridCount) {
            val gridConfig = ledGrid!!.getGridConfig(gridIndex) ?: continue

            for (y in 0..<gridSize) {
                for (x in 0..<gridSize) {
                    // Calculate window coordinates for this LED
                    val windowX = gridConfig.x + x * pixelSize + pixelSize / 2
                    val windowY = gridConfig.y + y * pixelSize + pixelSize / 2


                    // Sample color at this position by checking distance from particles and center
                    val ledColor = sampleColorAt(windowX, windowY)

                    if (ledColor != null) {
                        // Standard logical coordinates: x = left->right, y = top->bottom
                        ledGrid!!.setLedColor(gridIndex, x, y, ledColor)
                    }
                }
            }
        }


        // Send to devices
        ledGrid!!.sendToDevices()
    }

    /**
     * Samples the color at a specific window coordinate.
     */
    private fun sampleColorAt(x: Int, y: Int): Color? {
        val dx = (x - centerX).toFloat()
        val dy = (y - centerY).toFloat()
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()


        // Check if inside event horizon (black)
        if (dist < eventHorizonRadius) {
            return null // Black - don't set LED
        }

        var finalR = 0
        var finalG = 0
        var finalB = 0


        // Check particles
        for (p in particles!!) {
            val particleX = centerX + p.x
            val particleY = centerY + p.y
            val pdx = x - particleX
            val pdy = y - particleY
            val pdist = sqrt((pdx * pdx + pdy * pdy).toDouble()).toFloat()

            if (pdist < 24) { // Particle influence radius (3x bigger to match particle size)
                val weight = 1.0f / (pdist + 1.0f) * p.brightness
                finalR += (p.color!!.getRed() * weight).toInt()
                finalG += (p.color!!.getGreen() * weight).toInt()
                finalB += (p.color!!.getBlue() * weight).toInt()
            }
        }

        var finalColor = Color(
            min(255, finalR),
            min(255, finalG),
            min(255, finalB)
        )


        // Check accretion rings
        for (ring in 0..2) {
            val ringRadius = eventHorizonRadius + 20 + ring * 25
            val ringDist = abs(dist - ringRadius)

            if (ringDist < 5) {
                val intensity = (1.0f - ringDist / 5.0f) * 0.3f / (ring + 1)
                val hue = (time * 30.0f + ring * 60.0f) % 360.0f / 360.0f
                val ringColor = Color.getHSBColor(hue, 0.8f, intensity)

                finalR += (ringColor.getRed() * intensity).toInt()
                finalG += (ringColor.getGreen() * intensity).toInt()
                finalB += (ringColor.getBlue() * intensity).toInt()

                finalColor = Color(
                    min(255, finalR),
                    min(255, finalG),
                    min(255, finalB)
                )
            }
        }


        // Return color only if there's visible light
        if (finalColor.getRed() > 5 || finalColor.getGreen() > 5 || finalColor.getBlue() > 5) {
            return finalColor
        }

        return null
    }

    override fun getName(): String {
        return "Black Hole"
    }

    override fun getDescription(): String {
        return "Cosmic black hole with accretion disk and gravitational effects"
    }

    companion object {
        private const val PARTICLE_COUNT = 150
        private val log: Logger = LogManager.getLogger(BlackHoleAnimation::class.java)
    }
}

