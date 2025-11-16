package com.marsraver.LedFx.layout

import com.marsraver.LedFx.wled.WledController
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import kotlin.math.max
import kotlin.math.min

/**
 * LED grid that works with layout configurations.
 * Manages multiple LED grids based on layout configuration.
 */
class LayoutLedGrid(
    /**
     * Gets the layout configuration.
     */
    val layout: LayoutConfig
) {
    private val controllers: Array<WledController?>
    private val ledColors: Array<Array<Array<Color?>?>?> // [gridIndex][x][y]

    init {
        this.controllers = arrayOfNulls<WledController>(layout.gridCount)
        this.ledColors = arrayOfNulls<Array<Array<Color?>?>>(layout.gridCount)


        // Initialize controllers and LED color arrays
        for (i in 0 until layout.gridCount) {
            val grid = layout.grids[i]!!
            controllers[i] = WledController(grid.deviceIp!!, grid.ledCount)
            ledColors[i] = Array<Array<Color?>?>(grid.gridSize) { arrayOfNulls<Color>(grid.gridSize) }


            // Initialize all LEDs to black
            clearGrid(i)
        }
    }

    /**
     * Gets a grid configuration by ID.
     */
    fun getGridConfig(gridId: String?): GridConfig? {
        val id = gridId ?: return null
        return layout.getGridById(id)
    }

    /**
     * Gets a grid configuration by index.
     */
    fun getGridConfig(gridIndex: Int): GridConfig? {
        if (gridIndex < 0 || gridIndex >= layout.gridCount) {
            return null
        }
        return layout.grids[gridIndex]
    }

    /**
     * Gets the controller for a specific grid.
     */
    fun getController(gridIndex: Int): WledController? {
        if (gridIndex < 0 || gridIndex >= controllers.size) {
            return null
        }
        return controllers[gridIndex]
    }

    /**
     * Gets the controller for a grid by ID.
     */
    fun getController(gridId: String?): WledController? {
        val id = gridId ?: return null
        val grid = layout.getGridById(id)
        if (grid == null) {
            return null
        }


        // Find the grid index
        for (i in 0 until layout.gridCount) {
            if (grid.id == layout.grids[i]!!.id) {
                return controllers[i]
            }
        }
        return null
    }

    val gridCount: Int
        /**
         * Gets the number of grids in this layout.
         */
        get() = layout.gridCount

    /**
     * Draws a visual representation of all LED grids on the sketch.
     * This helps visualize which pixels correspond to which LEDs.
     *
     * @param g The Graphics2D object from the sketch
     */
    fun drawGrids(g: Graphics2D) {
        for (i in 0 until layout.gridCount) {
            val grid = layout.grids[i]!!
            drawSingleGrid(g, grid, i)
        }
    }

    /**
     * Draws a single LED grid with grid lines and LED indicators.
     */
    private fun drawSingleGrid(g: Graphics2D, grid: GridConfig, gridIndex: Int) {
        // Draw grid lines
        g.setColor(Color(255, 255, 255, 100)) // Semi-transparent white
        g.setStroke(BasicStroke(1f))

        for (i in 0..grid.gridSize) {
            val x = grid.x + (i * grid.pixelSize)
            val y = grid.y + (i * grid.pixelSize)


            // Vertical lines
            g.drawLine(x, grid.y, x, grid.y + grid.gridSize * grid.pixelSize)
            // Horizontal lines
            g.drawLine(grid.x, y, grid.x + grid.gridSize * grid.pixelSize, y)
        }


        // Draw LED indicators (small circles at LED positions)
        g.setColor(Color(255, 255, 0, 150)) // Semi-transparent yellow
        for (ledY in 0 until grid.gridSize) {
            for (ledX in 0 until grid.gridSize) {
                val centerX = grid.x + (ledX * grid.pixelSize) + (grid.pixelSize / 2)
                val centerY = grid.y + (ledY * grid.pixelSize) + (grid.pixelSize / 2)

                g.fillOval(centerX - 2, centerY - 2, 4, 4)
            }
        }


        // Draw grid label
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.BOLD, 14))
        g.drawString(grid.id, grid.x, grid.y - 10)
    }

    /**
     * Sets the color of a specific LED in a specific grid.
     */
    fun setLedColor(gridIndex: Int, x: Int, y: Int, color: Color?) {
        if (gridIndex < 0 || gridIndex >= ledColors.size) {
            return
        }

        val grid = getGridConfig(gridIndex)
        if (grid == null || x < 0 || x >= grid.gridSize || y < 0 || y >= grid.gridSize) {
            return
        }

        ledColors[gridIndex]!![x]!![y] = color
    }

    /**
     * Sets the color of a specific LED in a grid by ID.
     */
    fun setLedColor(gridId: String?, x: Int, y: Int, color: Color?) {
        val id = gridId ?: return
        val grid = layout.getGridById(id) ?: return


        // Find the grid index
        for (i in 0 until layout.gridCount) {
            if (grid.id == layout.grids[i]!!.id) {
                setLedColor(i, x, y, color)
                return
            }
        }
    }

    /**
     * Gets the color of a specific LED.
     */
    fun getLedColor(gridIndex: Int, x: Int, y: Int): Color? {
        if (gridIndex < 0 || gridIndex >= ledColors.size) {
            return null
        }

        val grid = getGridConfig(gridIndex)
        if (grid == null || x < 0 || x >= grid.gridSize || y < 0 || y >= grid.gridSize) {
            return null
        }

        return ledColors[gridIndex]!![x]!![y]
    }

    /**
     * Clears all LEDs in a specific grid to black.
     */
    fun clearGrid(gridIndex: Int) {
        if (gridIndex < 0 || gridIndex >= ledColors.size) {
            return
        }

        val grid = getGridConfig(gridIndex)
        if (grid == null) {
            return
        }

        for (x in 0 until grid.gridSize) {
            for (y in 0 until grid.gridSize) {
                ledColors[gridIndex]!![x]!![y] = Color.BLACK
            }
        }
    }

    /**
     * Clears all LEDs in all grids to black.
     */
    fun clearAllLeds() {
        for (i in ledColors.indices) {
            clearGrid(i)
        }
    }

    /**
     * Maps window coordinates to a specific grid and LED position.
     *
     * @param windowX window X coordinate
     * @param windowY window Y coordinate
     * @return array with [gridIndex, ledX, ledY] or null if not within any grid
     */
    fun mapWindowToLed(windowX: Int, windowY: Int): IntArray? {
        for (i in 0 until layout.gridCount) {
            val grid = layout.grids[i]!!

            if (windowX >= grid.x && windowX < grid.x + grid.width && windowY >= grid.y && windowY < grid.y + grid.height) {
                // Calculate LED coordinates within the grid

                var ledX = (windowX - grid.x) / grid.pixelSize
                var ledY = (windowY - grid.y) / grid.pixelSize


                // Ensure coordinates are within grid bounds
                ledX = max(0, min(ledX, grid.gridSize - 1))
                ledY = max(0, min(ledY, grid.gridSize - 1))

                return intArrayOf(i, ledX, ledY)
            }
        }

        return null // Not within any grid
    }

    /**
     * Sends LED data to all devices.
     */
    fun sendToDevices() {
        for (i in controllers.indices) {
            val grid = getGridConfig(i)
            if (grid == null) {
                continue
            }


            // Convert LED colors to RGB array
            val rgbData = IntArray(grid.ledCount * 3)
            var ledIndex = 0

            for (y in 0 until grid.gridSize) {
                for (x in 0 until grid.gridSize) {
                    val color = ledColors[i]!![x]!![y] ?: Color.BLACK

                    rgbData[ledIndex * 3] = color.red
                    rgbData[ledIndex * 3 + 1] = color.green
                    rgbData[ledIndex * 3 + 2] = color.blue
                    ledIndex++
                }
            }


            // Send to device
            controllers[i]!!.sendLedDataSimple(rgbData)
        }
    }

    /**
     * Turns off all devices.
     */
    fun turnOffAllDevices() {
        for (controller in controllers) {
            if (controller != null) {
                controller.turnOff()
            }
        }
    }

    /**
     * Gets the grid size for a specific grid.
     */
    fun getGridSize(gridIndex: Int): Int {
        val grid = getGridConfig(gridIndex)
        return grid?.gridSize ?: 0
    }

    /**
     * Gets the pixel size for a specific grid.
     */
    fun getPixelSize(gridIndex: Int): Int {
        val grid = getGridConfig(gridIndex)
        return grid?.pixelSize ?: 0
    }
}