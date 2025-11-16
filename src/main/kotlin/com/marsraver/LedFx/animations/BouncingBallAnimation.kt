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
import kotlin.math.max
import kotlin.math.min

/**
 * Bouncing ball animation for the LED framework.
 * Creates a single ball that bounces around the window with color cycling.
 */
class BouncingBallAnimation : LedAnimation {
    // Ball properties
    private var ballX = 0
    private var ballY = 0
    private val ballSize = 25 // Increased size for better visibility
    private var velocityX = 5
    private var velocityY = 5 // Slower movement for better visualization
    private var ballColor: Color? = null

    private var lastTime: Long = 0
    private var hue = 0f
    private var windowWidth = 0
    private var windowHeight = 0
    private var random: Random? = null

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.windowWidth = width
        this.windowHeight = height


        // Start the ball in the center of the window
        ballX = width / 2
        ballY = height / 2


        // Set initial color
        ballColor = Color(100, 150, 255)
        lastTime = System.currentTimeMillis()


        // Initialize random number generator for path variation
        random = Random()

        log.debug("Bouncing Ball Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)


        // Update and draw the single ball
        updateAndDrawBall(g)


        // Update LED colors based on ball position
        updateLedColors(ledGrid)


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Bouncing Ball - Press ESC to exit", 10, 20)
    }

    /**
     * Updates and draws the single ball.
     */
    private fun updateAndDrawBall(g: Graphics2D) {
        // Update ball position
        ballX += velocityX
        ballY += velocityY

        val hitX = ballX <= ballSize / 2 || ballX >= windowWidth - ballSize / 2
        val hitY = ballY <= ballSize / 2 || ballY >= windowHeight - ballSize / 2


        // Handle corner bounces properly
        if (hitX && hitY) {
            // Hit corner - reverse both directions
            velocityX = -velocityX
            velocityY = -velocityY
        } else if (hitX) {
            // Hit vertical wall
            velocityX = -velocityX
        } else if (hitY) {
            // Hit horizontal wall
            velocityY = -velocityY
        }


        // Add random noise to velocity if we hit a wall
        if (hitX || hitY) {
            // Add random noise to velocity (-2 to +2)
            velocityX += random!!.nextInt(5) - 2
            velocityY += random!!.nextInt(5) - 2
            // Ensure minimum speed to prevent getting stuck
            if (velocityX == 0) velocityX = if (random!!.nextBoolean()) 3 else -3
            if (velocityY == 0) velocityY = if (random!!.nextBoolean()) 3 else -3
            // Keep velocity within reasonable bounds
            velocityX = max(-10, min(10, velocityX))
            velocityY = max(-10, min(10, velocityY))
        }


        // Keep ball within window bounds
        ballX = max(ballSize / 2, min(windowWidth - ballSize / 2, ballX))
        ballY = max(ballSize / 2, min(windowHeight - ballSize / 2, ballY))


        // Animate color
        val currentTime = System.currentTimeMillis()
        val timeDelta = (currentTime - lastTime) / 1000.0f
        lastTime = currentTime

        hue += timeDelta * 120 // Rotate hue over time (doubled speed for more dynamic colors)
        if (hue > 360) hue -= 360f
        ballColor = Color.getHSBColor(hue / 360.0f, 0.8f, 1.0f)


        // Draw the ball
        g.setColor(ballColor)
        g.fill(
            Ellipse2D.Double(
                (ballX - ballSize / 2).toDouble(),
                (ballY - ballSize / 2).toDouble(),
                ballSize.toDouble(),
                ballSize.toDouble()
            )
        )


        // Add a subtle glow effect (reduced size)
        g.setColor(Color(ballColor!!.getRed(), ballColor!!.getGreen(), ballColor!!.getBlue(), 50))
        g.fill(
            Ellipse2D.Double(
                (ballX - ballSize / 2 - 2).toDouble(), (ballY - ballSize / 2 - 2).toDouble(),
                (ballSize + 4).toDouble(), (ballSize + 4).toDouble()
            )
        )
    }

    // Cache commonly used values
    private var gridSize = 0
    private var pixelSize = 0

    /**
     * Updates the LED grid colors based on the current ball position.
     */
    private fun updateLedColors(ledGrid: LedGrid) {
        // Cache values to avoid repeated method calls
        gridSize = ledGrid.gridSize
        pixelSize = ledGrid.pixelSize
        val gridCount = ledGrid.gridCount


        // Clear all LEDs first
        ledGrid.clearAllLeds()


        // Debug: Check if ball is in window bounds
        if (ballX < 0 || ballX >= windowWidth || ballY < 0 || ballY >= windowHeight) {
            // Ball is out of bounds, don't update LEDs
            return
        }


        // Pre-compute dimmed color for glow effect
        val glowR = ballColor!!.getRed() / 2
        val glowG = ballColor!!.getGreen() / 2
        val glowB = ballColor!!.getBlue() / 2
        val glowColor = Color(glowR, glowG, glowB)


        // Find which grid contains the ball
        var targetGridIndex = -1
        var ledX = -1
        var ledY = -1

        for (gridIndex in 0 until gridCount) {
            val gridConfig = ledGrid.getGridConfig(gridIndex) ?: continue


            // Calculate ball position relative to this grid
            val ballX_relative = ballX - gridConfig.x
            val ballY_relative = ballY - gridConfig.y


            // Map to LED coordinates
            val gridLedX = ballX_relative / pixelSize
            val gridLedY = ballY_relative / pixelSize


            // Check if ball is within this grid
            if (gridLedX >= 0 && gridLedX < gridSize && gridLedY >= 0 && gridLedY < gridSize) {
                targetGridIndex = gridIndex
                ledX = gridLedX
                ledY = gridLedY
                break // Found the grid, stop searching
            }
        }


        // Only draw if we found a valid grid
        if (targetGridIndex >= 0) {
            // Set the LED at the ball position (no transformation needed)
            ledGrid.setLedColor(targetGridIndex, ledX, ledY, ballColor)


            // Add a small glow effect (optimized to use pre-computed color)
            for (dy in -1..1) {
                for (dx in -1..1) {
                    // Skip the center (already set to ball color)
                    if (dx == 0 && dy == 0) continue

                    val glowX = ledX + dx
                    val glowY = ledY + dy


                    // Check bounds for glow effect
                    if (glowX >= 0 && glowX < gridSize && glowY >= 0 && glowY < gridSize) {
                        ledGrid.setLedColor(targetGridIndex, glowX, glowY, glowColor)
                    }
                }
            }
        }
    }

    override fun getName(): String {
        return "Bouncing Ball"
    }

    override fun getDescription(): String {
        return "A single ball that bounces around the window with dynamic color cycling"
    }

    companion object {
        private val log: Logger = LogManager.getLogger(BouncingBallAnimation::class.java)
    }
}
