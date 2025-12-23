package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.util.*
import kotlin.math.*

/**
 * Sound-reactive falling snow animation.
 * Snowflakes fall from top to bottom, with audio-reactive size, speed, and sparkle effects.
 */
class FallingSnowAnimation : RMSAnimation() {
    // RMS configuration - only need 1 band for overall volume
    override val bandCount = 1

    private data class Snowflake(
        var x: Float,
        var y: Float,
        val baseSpeed: Float,  // Base falling speed
        val size: Float,       // Size in pixels
        val sparkle: Float,    // Sparkle factor (0-1)
        var angle: Float,      // Rotation angle
        val rotationSpeed: Float // Rotation speed
    )

    private val snowflakes = mutableListOf<Snowflake>()
    private var random: Random? = null
    private val maxSnowflakes = 150
    
    // Snowflake patterns (simple star shapes)
    private val snowflakePatterns = arrayOf(
        arrayOf(0, -3, 0, 3, -2, -1, 2, 1, -2, 1, 2, -1),  // 6-pointed
        arrayOf(0, -2, 0, 2, -2, 0, 2, 0, -1, -1, 1, 1, -1, 1, 1, -1),  // 8-pointed
        arrayOf(0, -3, 0, 3, -2, -2, 2, 2, -2, 2, 2, -2, -3, 0, 3, 0)   // Star
    )

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        super.init(width, height, ledGrid)
        random = Random()
        
        // Get LED grid bounds to initialize snowflakes within area
        val bounds = ledGrid.getGridBounds(width, height)
        
        // Initialize snowflakes across the LED grid area
        snowflakes.clear()
        for (i in 0 until maxSnowflakes) {
            val x = bounds.minX + random!!.nextFloat() * bounds.width
            val y = bounds.minY + random!!.nextFloat() * bounds.height
            val baseSpeed = 0.5f + random!!.nextFloat() * 1.5f
            val size = 2f + random!!.nextFloat() * 4f
            val sparkle = 0.3f + random!!.nextFloat() * 0.7f
            val angle = random!!.nextFloat() * 2f * PI.toFloat()
            val rotationSpeed = -0.05f + random!!.nextFloat() * 0.1f
            
            snowflakes.add(Snowflake(x, y, baseSpeed, size, sparkle, angle, rotationSpeed))
        }
        
        log.debug("Falling Snow Animation initialized with ${snowflakes.size} snowflakes")
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Safety check: ensure arrays are initialized
        if (!arraysInitialized) return
        
        // Clear background - dark blue/navy for night sky effect
        g.color = Color(5, 10, 30)
        g.fillRect(0, 0, width, height)

        // Get LED grid bounds to constrain snow to LED area
        val bounds = ledGrid.getGridBounds(width, height)
        
        // Get current volume level (audio-reactive)
        val volumeLevel = masterLevel.coerceIn(0f, 1f)
        
        // Audio affects: speed boost, size boost, sparkle intensity
        val speedBoost = 1f + volumeLevel * 1.5f  // Up to 2.5x speed
        val sizeBoost = 1f + volumeLevel * 0.5f   // Up to 1.5x size
        val sparkleIntensity = 0.5f + volumeLevel * 0.8f  // More sparkle when loud

        // Update and draw snowflakes
        for (snowflake in snowflakes) {
            // Update position (falling down)
            snowflake.y += snowflake.baseSpeed * speedBoost
            snowflake.x += sin(snowflake.angle.toDouble()).toFloat() * 0.3f  // Slight horizontal drift
            snowflake.angle += snowflake.rotationSpeed
            
            // Wrap around - reset to top when it reaches bottom
            if (snowflake.y > bounds.maxY) {
                snowflake.y = bounds.minY.toFloat()
                snowflake.x = bounds.minX + random!!.nextFloat() * bounds.width
            }
            
            // Keep x within bounds with wrapping
            if (snowflake.x < bounds.minX) {
                snowflake.x = bounds.maxX.toFloat()
            } else if (snowflake.x >= bounds.maxX) {
                snowflake.x = bounds.minX.toFloat()
            }
            
            // Only draw if within LED grid bounds
            if (snowflake.x < bounds.minX || snowflake.x >= bounds.maxX || 
                snowflake.y < bounds.minY || snowflake.y >= bounds.maxY) {
                continue
            }
            
            // Calculate audio-reactive size
            val currentSize = snowflake.size * sizeBoost
            val sparkleFactor = snowflake.sparkle * sparkleIntensity
            
            // Color - white with sparkle effect (brighter when audio is loud)
            val brightness = (200f + sparkleFactor * 55f).coerceAtMost(255f).toInt()
            val color = Color(brightness, brightness, (brightness * 1.1f).coerceAtMost(255f).toInt())
            
            g.color = color
            
            // Draw snowflake as a simple pattern (small cross or star)
            val pattern = snowflakePatterns[(abs(snowflake.x + snowflake.y).toInt() % snowflakePatterns.size)]
            val x = snowflake.x.toInt()
            val y = snowflake.y.toInt()
            
            // Draw pattern lines
            for (i in pattern.indices step 2) {
                if (i + 1 < pattern.size) {
                    val dx = (pattern[i] * currentSize / 4f).toInt()
                    val dy = (pattern[i + 1] * currentSize / 4f).toInt()
                    g.fillRect(x + dx - 1, y + dy - 1, 2, 2)
                }
            }
            
            // Center dot
            g.fillOval(x - 1, y - 1, 3, 3)
        }

        // Update LEDs
        mapToLeds(ledGrid, bounds)
        
        // Info text
        g.color = Color.WHITE
        g.drawString("Falling Snow (live audio) - Press ESC to exit", 10, 20)
    }
    
    /**
     * Maps snowflakes to LED grid.
     */
    private fun mapToLeds(ledGrid: LedGrid, bounds: LedGrid.GridBounds) {
        ledGrid.clearAllLeds()
        
        val gridCount = ledGrid.gridCount
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize
        
        for (snowflake in snowflakes) {
            // Skip if outside bounds
            if (snowflake.x < bounds.minX || snowflake.x >= bounds.maxX || 
                snowflake.y < bounds.minY || snowflake.y >= bounds.maxY) {
                continue
            }
            
            // Find which grid this snowflake is in
            for (gridIndex in 0 until gridCount) {
                val cfg = ledGrid.getGridConfig(gridIndex) ?: continue
                
                // Check if snowflake is within this grid
                if (snowflake.x >= cfg.x && snowflake.x < cfg.x + cfg.gridSize * cfg.pixelSize &&
                    snowflake.y >= cfg.y && snowflake.y < cfg.y + cfg.gridSize * cfg.pixelSize) {
                    
                    val relX = (snowflake.x - cfg.x).toInt()
                    val relY = (snowflake.y - cfg.y).toInt()
                    val ledX = relX / pixelSize
                    val ledY = relY / pixelSize
                    
                    if (ledX in 0 until gridSize && ledY in 0 until gridSize) {
                        val volumeLevel = masterLevel.coerceIn(0f, 1f)
                        val sparkleFactor = snowflake.sparkle * (0.5f + volumeLevel * 0.8f)
                        val brightness = (200f + sparkleFactor * 55f).coerceAtMost(255f).toInt()
                        val color = Color(brightness, brightness, (brightness * 1.1f).coerceAtMost(255f).toInt())
                        
                        ledGrid.setLedColor(gridIndex, ledX, ledY, color)
                    }
                }
            }
        }
        
        ledGrid.sendToDevices()
    }

    override fun getName(): String = "Falling Snow"

    override fun getDescription(): String =
        "Sound-reactive falling snow animation with audio-controlled size, speed, and sparkle effects"

    companion object {
        private val log: Logger = LogManager.getLogger(FallingSnowAnimation::class.java)
    }
}

