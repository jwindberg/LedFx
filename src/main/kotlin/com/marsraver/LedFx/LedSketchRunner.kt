package com.marsraver.LedFx

import com.marsraver.LedFx.layout.GridConfig
import com.marsraver.LedFx.layout.LayoutConfig
import com.marsraver.LedFx.layout.LayoutLoader
import com.marsraver.LedFx.wled.WledDdpClient
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.min

/**
 * Enhanced sketch runner that supports LED output to WLED devices.
 * Manages both the sketch window and LED grid communication.
 */
class LedSketchRunner {
    var frame: JFrame? = null
    private val canvas: LedSketchCanvas
    private val sketch: Any?

    /**
     * Gets the LED grid for external access.
     *
     * @return The LED grid
     */
    val ledGrid: LedGrid
    private var animationTimer: Timer? = null

    /**
     * Creates a new LedSketchRunner using a layout configuration.
     *
     * @param sketch The LED sketch to run (can be LedSketch or DualLedSketch)
     * @param layoutName The name of the layout to load
     */
    constructor(sketch: Any?, layoutName: String) {
        this.sketch = sketch


        // Load the layout configuration
        val layout: LayoutConfig = try {
            LayoutLoader.loadLayout(layoutName)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to load layout: $layoutName", e)
        }


        // Create the unified LED grid
        this.ledGrid = LedGrid(layout)
        this.canvas = LedSketchCanvas(layout.windowWidth, layout.windowHeight)

        setupWindow(layout.title)
        setupAnimation()
    }

    /**
     * Creates a new LedSketchRunner for the given sketch and WLED device (legacy method).
     * This creates a simple single-grid layout programmatically.
     *
     * @param sketch The LED sketch to run
     * @param width The width of the window
     * @param height The height of the window
     * @param title The title of the window
     * @param wledDeviceIp The IP address of the WLED device
     * @param ledCount The number of LEDs on the device
     */
    constructor(
        sketch: LedSketch?, width: Int, height: Int, title: String?,
        wledDeviceIp: String?, ledCount: Int
    ) {
        this.sketch = sketch


        // Create a simple single-grid layout programmatically
        val layout = LayoutConfig("SingleGrid", title, width, height)


        // Calculate grid dimensions - grid takes up 80% of the window, centered
        val maxGridSize = min(width, height) * 80 / 100
        val pixelSize = maxGridSize / 16 // Assume 16x16 grid
        val totalGridSize = 16 * pixelSize
        val gridX = (width - totalGridSize) / 2
        val gridY = (height - totalGridSize) / 2


        // Add a single grid to the layout
        layout.addGrid(
            GridConfig(
                "Grid01", wledDeviceIp, ledCount, gridX, gridY,
                totalGridSize, totalGridSize, 16, pixelSize
            )
        )


        // Create the unified LED grid
        this.ledGrid = LedGrid(layout)
        this.canvas = LedSketchCanvas(width, height)

        setupWindow(title)
        setupAnimation()
    }

    /**
     * Sets up the window and canvas.
     */
    private fun setupWindow(title: String?) {
        frame = JFrame(title)
        frame!!.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        frame!!.setResizable(false)
        frame!!.add(canvas)
        frame!!.pack()
        frame!!.setLocationRelativeTo(null)
        frame!!.setVisible(true)
    }

    /**
     * Sets up the 60 FPS animation timer with LED output.
     */
    private fun setupAnimation() {
        // Call init() once with LED grid
        if (sketch is LedSketch) {
            sketch.init(canvas.getWidth(), canvas.getHeight(), ledGrid)
        }


        // Set up the animation timer
        animationTimer = Timer(FRAME_DELAY, object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                canvas.repaint()
            }
        })
        animationTimer!!.start()
    }

    /**
     * Starts the sketch animation.
     */
    fun start() {
        if (animationTimer != null && !animationTimer!!.isRunning()) {
            animationTimer!!.start()
        }
    }

    /**
     * Stops the sketch animation and turns off LEDs.
     */
    fun stop() {
        if (animationTimer != null && animationTimer!!.isRunning()) {
            animationTimer!!.stop()
        }


        // Turn off all LED devices when stopping
        for (i in 0 until ledGrid.gridCount) {
            val gridConfig = ledGrid.getGridConfig(i)
            val controller = ledGrid.getController(i)
            if (controller != null && gridConfig != null) {
                controller.turnOff(gridConfig.gridSize * gridConfig.gridSize)
            }
        }
    }

    /**
     * Custom canvas that handles drawing and LED output.
     */
    private inner class LedSketchCanvas(width: Int, height: Int) : JPanel() {
        init {
            setPreferredSize(Dimension(width, height))
            setBackground(Color.BLACK)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g.create() as Graphics2D


            // Enable anti-aliasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)


            // Call the sketch's draw method with LED grid
            if (sketch is LedSketch) {
                sketch.draw(g2d, getWidth(), getHeight(), ledGrid)
            }


            // Sample colors from the sketch and send to LED device
            ledGrid.sampleColors(g2d)
            ledGrid.sendToDevices()


            // Draw the LED grid overlay (optional, for visualization)
            ledGrid.drawGrid(g2d)

            g2d.dispose()
        }
    }

    /**
     * Gets the WLED controller for a specific grid.
     *
     * @param gridIndex The index of the grid
     * @return The WLED DDP client
     */
    fun getWledController(gridIndex: Int): WledDdpClient? {
        return ledGrid.getController(gridIndex)
    }

    /**
     * Gets the WLED controller for a grid by ID.
     *
     * @param gridId The ID of the grid
     * @return The WLED DDP client
     */
    fun getWledController(gridId: String?): WledDdpClient? {
        return ledGrid.getController(gridId)
    }

    companion object {
        private const val TARGET_FPS = 60
        private val FRAME_DELAY: Int = 1000 / TARGET_FPS
    }
}
