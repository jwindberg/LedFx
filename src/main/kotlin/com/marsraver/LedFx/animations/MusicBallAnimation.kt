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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Music Ball animation for the LED framework.
 * Creates audio-reactive particles that respond to music (simulated beat detection).
 */
class MusicBallAnimation : LedAnimation {
    private inner class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float, var color: Color) {
        var age: Int = 0
        var alive: Boolean = true

        fun update() {
            age += 3
            vy += 0.1f // gravity
            x += vx
            y += vy

            if (y > windowHeight || age >= 255) {
                alive = false
            }
        }
    }

    private var ledGrid: LedGrid? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var lastBeatTime: Long = 0
    private val random = Random()
    private val balls: MutableList<Ball> = ArrayList<Ball>()
    private var simulatedAudioLevel = 0f
    private var beatCooldown = 0

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height
        this.lastBeatTime = System.currentTimeMillis()

        log.debug("Music Ball Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
        log.debug("Note: Using simulated audio input (no microphone required)")
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Semi-transparent black for trail effect
        g.setColor(Color(0, 0, 0, 45))
        g.fillRect(0, 0, width, height)


        // Simulate beat detection
        simulateBeat()


        // Spawn balls on beat
        spawnBallsOnBeat()


        // Update and draw all balls
        updateAndDrawBalls(g)


        // Update LED colors
        updateLedColors()


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Music Ball (Simulated Audio) - Press ESC to exit", 10, 20)
        g.drawString("Balls: " + balls.size, 10, 35)
    }

    /**
     * Simulates beat detection with periodic beats
     */
    private fun simulateBeat() {
        val currentTime = System.currentTimeMillis()


        // Simulate audio level (increase amplitude and speed a bit for more activity)
        simulatedAudioLevel = (0.5f + 0.5f * sin(currentTime / 180.0)).toFloat()


        // Reduce cooldown
        if (beatCooldown > 0) beatCooldown--
    }

    /**
     * Spawns balls when a beat is detected (simulated)
     */
    private fun spawnBallsOnBeat() {
        val currentTime = System.currentTimeMillis()


        // Simulate beat more often (~900ms) and with shorter cooldown for higher sensitivity
        if (currentTime - lastBeatTime > 900 && beatCooldown == 0) {
            lastBeatTime = currentTime
            beatCooldown = 45 // Prevent too many simultaneous spawns but keep things lively


            // Random color
            val ballColor = Color(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
            )


            // Spawn more balls based on simulated audio level
            val ballCount = (abs(simulatedAudioLevel) * 18).toInt()
            for (j in 0..<ballCount) {
                val x = random.nextFloat() * windowWidth
                val y = random.nextFloat() * windowHeight

                for (i in 0..2) {
                    val vx = (random.nextFloat() - 0.5f) * 2f // Slower velocity
                    val vy = (random.nextFloat() - 0.5f) * 2f // Slower velocity
                    balls.add(Ball(x, y, vx, vy, ballColor))
                }
            }
        }
    }

    /**
     * Updates and draws all balls
     */
    private fun updateAndDrawBalls(g: Graphics2D) {
        for (i in balls.indices.reversed()) {
            val ball = balls.get(i)
            ball.update()

            if (!ball.alive) {
                balls.removeAt(i)
                continue
            }


            // Draw ball with fading transparency
            var alpha = 255 - ball.age
            if (alpha < 0) alpha = 0
            if (alpha > 255) alpha = 255

            val drawColor = Color(
                ball.color.getRed(),
                ball.color.getGreen(),
                ball.color.getBlue(),
                alpha
            )

            g.setColor(drawColor)
            g.fill(Ellipse2D.Double(ball.x - 2.5, ball.y - 2.5, 5.0, 5.0))
        }
    }

    /**
     * Updates LED colors based on ball positions
     */
    private fun updateLedColors() {
        val grid = ledGrid ?: return
        grid.clearAllLeds()

        val gridSize = grid.gridSize
        val pixelSize = grid.pixelSize
        val gridCount = grid.gridCount

        for (ball in balls) {
            if (!ball.alive) continue


            // Map ball to each grid
            for (gridIndex in 0 until gridCount) {
                val gridConfig = grid.getGridConfig(gridIndex) ?: continue


                // Calculate ball position relative to this grid
                val ballX_relative = ball.x.toInt() - gridConfig.x
                val ballY_relative = ball.y.toInt() - gridConfig.y


                // Map to LED coordinates
                val ledX = ballX_relative / pixelSize
                val ledY = ballY_relative / pixelSize


                // Check if ball is within this grid
                if (ledX >= 0 && ledX < gridSize && ledY >= 0 && ledY < gridSize) {
                    // Use ball color with fade
                    val alpha = 255 - ball.age
                    if (alpha > 0) {
                        val alphaRatio = alpha / 255.0f
                        val ledColor = Color(
                            min(255, max(0, (ball.color.getRed() * alphaRatio).toInt())),
                            min(255, max(0, (ball.color.getGreen() * alphaRatio).toInt())),
                            min(255, max(0, (ball.color.getBlue() * alphaRatio).toInt()))
                        )
                        // Standard logical coordinates: x = left->right, y = top->bottom
                        grid.setLedColor(gridIndex, ledX, ledY, ledColor)
                    }
                }
            }
        }
    }

    override fun getName(): String {
        return "Music Ball"
    }

    override fun getDescription(): String {
        return "Audio-reactive particles that respond to music beats (simulated audio)"
    }

    companion object {
        private val log: Logger = LogManager.getLogger(MusicBallAnimation::class.java)
    }
}
