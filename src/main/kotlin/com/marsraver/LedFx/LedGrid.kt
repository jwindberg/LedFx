package com.marsraver.LedFx

import com.marsraver.LedFx.layout.GridConfig
import com.marsraver.LedFx.layout.LayoutConfig
import com.marsraver.LedFx.wled.WledDdpClient
import com.marsraver.LedFx.wled.WledInfo
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Unified LED grid that manages multiple LED grids based on layout configuration.
 * This replaces the old SingleLedGrid and DualLedGrid classes with a flexible system
 * that can handle any number of grids positioned anywhere in the window.
 */
class LedGrid(
    /**
     * Gets the layout configuration.
     *
     * @return The layout configuration
     */
    val layout: LayoutConfig
) {
    private val controllers: MutableList<WledDdpClient> // DDP clients, one per grid
    private val ledColors: MutableList<Array<Array<Color?>>> // [gridIndex][x][y]
    private val grids: MutableList<GridConfig>

    init {
        this.grids = layout.grids
        this.controllers = ArrayList()
        this.ledColors = ArrayList()


        // Initialize DDP clients and LED color arrays for each grid
        for (i in grids.indices) {
            val grid = grids[i]
            val info = WledInfo(grid.deviceIp, grid.id)
            val client = WledDdpClient(info, WledDdpClient.defaultDdpPort)
            try {
                client.connect()
            } catch (e: Exception) {
                log.error(
                    "Failed to connect DDP client for grid {} at {}: {}",
                    grid.id,
                    grid.deviceIp,
                    e.message
                )
            }
            controllers.add(client)


            // Initialize LED color array for this grid
            val gridColors: Array<Array<Color?>> = Array(grid.gridSize) { arrayOfNulls<Color>(grid.gridSize) }
            ledColors.add(gridColors)


            // Initialize all LEDs to black
            clearGrid(i)
        }

        log.debug("Unified LED Grid initialized with DDP:")
        log.debug("  Layout: ${layout.name}")
        log.debug("  Window: ${layout.windowWidth}x${layout.windowHeight}")
        log.debug("  Grids: ${grids.size}")
        for (i in grids.indices) {
            val grid = grids[i]
            log.debug(
                "    Grid ${i + 1} (${grid.id}): ${grid.gridSize}x${grid.gridSize} at (${grid.x}, ${grid.y}) -> ${grid.deviceIp}"
            )
        }
    }

    /**
     * Sets the color of a specific LED using window coordinates.
     * The layout system will determine which physical LED device and position to use.
     *
     * @param windowX The X coordinate in the window
     * @param windowY The Y coordinate in the window
     * @param color The color to set
     */
    fun setLedColor(windowX: Int, windowY: Int, color: Color?) {
        // Find which grid this window coordinate maps to
        for (gridIndex in grids.indices) {
            val grid = grids[gridIndex]

            if (windowX >= grid.x && windowX < grid.x + grid.width &&
                windowY >= grid.y && windowY < grid.y + grid.height
            ) {
                // Convert window coordinates to grid coordinates

                var gridX = (windowX - grid.x) / grid.pixelSize
                var gridY = (windowY - grid.y) / grid.pixelSize


                // Clamp to grid bounds
                gridX = max(0, min(grid.gridSize - 1, gridX))
                gridY = max(0, min(grid.gridSize - 1, gridY))


                // Set the LED color
                ledColors[gridIndex][gridX][gridY] = color
                return
            }
        }
    }

    /**
     * Sets the color of a specific LED in a specific grid.
     *
     * @param gridIndex The index of the grid (0-based)
     * @param gridX The X position within the grid (0-based)
     * @param gridY The Y position within the grid (0-based)
     * @param color The color to set
     */
    fun setLedColor(gridIndex: Int, gridX: Int, gridY: Int, color: Color?) {
        if (gridIndex >= 0 && gridIndex < grids.size) {
            val grid = grids[gridIndex]
            if (gridX >= 0 && gridX < grid.gridSize && gridY >= 0 && gridY < grid.gridSize) {
                ledColors[gridIndex][gridX][gridY] = color
            }
        }
    }

    /**
     * Clears all LEDs (sets them to black/off).
     */
    fun clearAllLeds() {
        for (i in grids.indices) {
            clearGrid(i)
        }
    }

    /**
     * Clears all LEDs in a specific grid.
     *
     * @param gridIndex The index of the grid to clear
     */
    fun clearGrid(gridIndex: Int) {
        if (gridIndex >= 0 && gridIndex < grids.size) {
            val grid = grids[gridIndex]
            val gridColors = ledColors[gridIndex]
            for (x in 0 until grid.gridSize) {
                for (y in 0 until grid.gridSize) {
                    gridColors[x]!![y] = Color.BLACK
                }
            }
        }
    }

    /**
     * Sends the current LED data to all connected devices.
     *
     * @return true if all devices were successful, false otherwise
     */
    fun sendToDevices(): Boolean {
        var allSuccess = true
        for (i in grids.indices) {
            val grid = grids[i]
            val controller = controllers[i]
            val gridColors = ledColors[i]!!


            // Convert Color[][] to int[] in the order WLED appears to expect for DDP:
            // row-major, top-left first, left-to-right, top-to-bottom.
            //
            // Coordinate system in memory:
            //   gridColors[x][y], where y = 0 is top row on screen,
            //   y = gridSize - 1 is bottom row on screen.
            // Packing:
            //   index 0      -> (x=0,           y=0)         top-left
            //   index 15     -> (x=gridSize-1,  y=0)         top-right
            //   index 240    -> (x=0,           y=gridSize-1) bottom-left
            //   index 255    -> (x=gridSize-1,  y=gridSize-1) bottom-right
            val gridSize = grid.gridSize
            val ledCount = gridSize * gridSize
            val ledData = IntArray(ledCount * 3)
            Arrays.fill(ledData, 0)

            var index = 0
            // Some panels may be physically mirrored. For now we correct Grid01,
            // which is observed to be horizontally flipped compared to others.
            val flipHorizontal = "Grid01".equals(grid.id, ignoreCase = true)
            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    val sampleX = if (flipHorizontal) (gridSize - 1 - x) else x
                    val color = gridColors[sampleX][y] ?: Color.BLACK
                    ledData[index++] = min(255, max(0, color.red))
                    ledData[index++] = min(255, max(0, color.green))
                    ledData[index++] = min(255, max(0, color.blue))
                }
            }

            val success = controller.sendRgb(ledData, ledCount)
            if (!success) {
            log.error("Failed to send LED data to ${grid.deviceIp}")
                allSuccess = false
            }
        }
        return allSuccess
    }

    /**
     * Maps window coordinates to LED grid coordinates.
     *
     * @param windowX The X coordinate in the window
     * @param windowY The Y coordinate in the window
     * @return An array containing [gridIndex, ledX, ledY] or null if not within any grid
     */
    fun mapWindowToLed(windowX: Int, windowY: Int): IntArray? {
        for (gridIndex in grids.indices) {
            val grid = grids[gridIndex]

            if (windowX >= grid.x && windowX < grid.x + grid.width &&
                windowY >= grid.y && windowY < grid.y + grid.height
            ) {
                var gridX = (windowX - grid.x) / grid.pixelSize
                var gridY = (windowY - grid.y) / grid.pixelSize


                // Clamp to grid bounds
                gridX = max(0, min(grid.gridSize - 1, gridX))
                gridY = max(0, min(grid.gridSize - 1, gridY))

                return intArrayOf(gridIndex, gridX, gridY)
            }
        }
        return null
    }

    val gridCount: Int
        /**
         * Gets the number of LED grids in this layout.
         *
         * @return The number of grids
         */
        get() = grids.size

    val gridSize: Int
        /**
         * Gets the size of each LED grid (assumes all grids are the same size).
         *
         * @return The grid size (e.g., 16 for 16x16)
         */
        get() {
            if (grids.isEmpty()) return 0
            return grids[0].gridSize // Assume all grids are the same size
        }

    val pixelSize: Int
        /**
         * Gets the pixel size for each LED in the window.
         *
         * @return The pixel size
         */
        get() {
            if (grids.isEmpty()) return 0
            return grids[0].pixelSize // Assume all grids use the same pixel size
        }

    val windowWidth: Int
        /**
         * Gets the window width.
         *
         * @return The window width
         */
        get() = layout.windowWidth

    val windowHeight: Int
        /**
         * Gets the window height.
         *
         * @return The window height
         */
        get() = layout.windowHeight

    /**
     * Gets a specific grid configuration.
     *
     * @param gridIndex The index of the grid
     * @return The grid configuration
     */
    fun getGridConfig(gridIndex: Int): GridConfig? {
        if (gridIndex >= 0 && gridIndex < grids.size) {
            return grids[gridIndex]
        }
        return null
    }

    /**
     * Gets a grid configuration by ID.
     *
     * @param gridId The ID of the grid
     * @return The grid configuration
     */
    fun getGridConfig(gridId: String?): GridConfig? {
        val id = gridId ?: return null
        return layout.getGridById(id)
    }

    /**
     * Gets the controller for a specific grid.
     *
     * @param gridIndex The index of the grid
     * @return The WLED DDP client
     */
    fun getController(gridIndex: Int): WledDdpClient? {
        if (gridIndex >= 0 && gridIndex < controllers.size) {
            return controllers[gridIndex]
        }
        return null
    }

    /**
     * Gets the controller for a grid by ID.
     *
     * @param gridId The ID of the grid
     * @return The WLED DDP client
     */
    fun getController(gridId: String?): WledDdpClient? {
        val id = gridId ?: return null
        val grid = layout.getGridById(id)
        if (grid != null) {
            val gridIndex = grids.indexOf(grid)
            return getController(gridIndex)
        }
        return null
    }

    /**
     * Draws all LED grids with grid lines and LED indicators.
     */
    fun drawGrid(g: Graphics2D) {
        for (i in grids.indices) {
            drawSingleGrid(g, i)
        }
    }

    /**
     * Draws a single LED grid with grid lines and LED indicators.
     */
    private fun drawSingleGrid(g: Graphics2D, gridIndex: Int) {
        val grid = grids[gridIndex]


        // Draw grid lines
        g.color = Color(255, 255, 255, 100) // Semi-transparent white
        g.stroke = BasicStroke(1f)

        for (i in 0..grid.gridSize) {
            val x = grid.x + i * grid.pixelSize
            val y = grid.y + i * grid.pixelSize


            // Vertical lines
            g.drawLine(x, grid.y, x, grid.y + grid.gridSize * grid.pixelSize)
            // Horizontal lines
            g.drawLine(grid.x, y, grid.x + grid.gridSize * grid.pixelSize, y)
        }


        // Draw LED indicators (small circles at LED positions)
        g.color = Color(255, 255, 0, 150) // Semi-transparent yellow
        for (ledY in 0 until grid.gridSize) {
            for (ledX in 0 until grid.gridSize) {
                val centerX = grid.x + ledX * grid.pixelSize + grid.pixelSize / 2
                val centerY = grid.y + ledY * grid.pixelSize + grid.pixelSize / 2

                g.fillOval(centerX - 2, centerY - 2, 4, 4)
            }
        }


        // Draw grid label
        g.color = Color.WHITE
        g.font = Font("Arial", Font.BOLD, 14)
        g.drawString(grid.id ?: "", grid.x, grid.y - 10)
    }

    /**
     * Samples colors from the graphics context and updates LED colors.
     * This method samples colors from the current graphics context at each LED position.
     */
    fun sampleColors(g: Graphics2D) {
        for (gridIndex in grids.indices) {
            val grid = grids[gridIndex]
            for (ledY in 0 until grid.gridSize) {
                for (ledX in 0 until grid.gridSize) {
                    // Sample color from the current graphics color (placeholder behavior)
                    val color = Color(g.color.rgb)
                    ledColors[gridIndex]!![ledX]!![ledY] = color
                }
            }
        }
    }
}

private val log: Logger = LogManager.getLogger(LedGrid::class.java)