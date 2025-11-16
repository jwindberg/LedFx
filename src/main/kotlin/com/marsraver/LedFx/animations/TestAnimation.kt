package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import com.marsraver.LedFx.layout.GridConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import kotlin.math.max

/**
 * Test animation that lights corner LEDs with distinct colors on each grid.
 * Useful for debugging grid mapping, orientation, and serpentine layout.
 */
class TestAnimation : LedAnimation {
    @Suppress("unused")
    private var ledGrid: LedGrid? = null

    @Suppress("unused")
    private var windowWidth = 0

    @Suppress("unused")
    private var windowHeight = 0

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height

        log.debug("Test Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Clear the background
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)

        val gridCount = ledGrid.gridCount

        // Corner colors (visual and physical)
        val topLeftColor = Color.RED
        val topRightColor = Color.GREEN
        val bottomLeftColor = Color.BLUE
        val bottomRightColor = Color.YELLOW

        for (gridIndex in 0 until gridCount) {
            val cfg = ledGrid.getGridConfig(gridIndex) ?: continue

            val gridSize = cfg.gridSize
            val pixelSize = cfg.pixelSize

            // Clear this grid's LED buffer
            ledGrid.clearGrid(gridIndex)

            val maxIdx = gridSize - 1

            // Logical LED coordinates:
            // (0,0)          ... (maxIdx,0)
            //    .                 .
            // (0,maxIdx) ... (maxIdx,maxIdx)
            //
            // We color each corner distinctly.
            ledGrid.setLedColor(gridIndex, 0, 0, topLeftColor)
            ledGrid.setLedColor(gridIndex, maxIdx, 0, topRightColor)
            ledGrid.setLedColor(gridIndex, 0, maxIdx, bottomLeftColor)
            ledGrid.setLedColor(gridIndex, maxIdx, maxIdx, bottomRightColor)

            // Draw matching circles on the canvas at LED centers so you can visually
            // compare screen vs. physical panel.
            drawCornerMarker(g, cfg, pixelSize, 0, 0, topLeftColor)
            drawCornerMarker(g, cfg, pixelSize, maxIdx, 0, topRightColor)
            drawCornerMarker(g, cfg, pixelSize, 0, maxIdx, bottomLeftColor)
            drawCornerMarker(g, cfg, pixelSize, maxIdx, maxIdx, bottomRightColor)
        }

        // Send LED buffer to devices
        ledGrid.sendToDevices()

        // Info legend
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 14))
        g.drawString("Test Animation - Corners: TL=RED, TR=GREEN, BL=BLUE, BR=YELLOW", 10, 20)
    }

    private fun drawCornerMarker(g: Graphics2D, cfg: GridConfig, pixelSize: Int, ledX: Int, ledY: Int, color: Color?) {
        val centerX = cfg.x + ledX * pixelSize + pixelSize / 2
        val centerY = cfg.y + ledY * pixelSize + pixelSize / 2
        val radius = max(4, pixelSize / 3)

        g.setColor(color)
        g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2)
    }

    override fun getName(): String {
        return "Test Animation"
    }

    override fun getDescription(): String {
        return "Lights corner LEDs with distinct colors for mapping/orientation testing"
    }

    companion object {
        private val log: Logger = LogManager.getLogger(TestAnimation::class.java)
    }
}
