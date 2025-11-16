package com.marsraver.LedFx

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer

/**
 * Runs a Processing-like sketch with a 60 FPS animation loop.
 * Creates a window and calls the sketch's init() and draw() methods.
 */
class SketchRunner(private val sketch: Sketch, width: Int, height: Int, title: String?) {
    var frame: JFrame? = null
    private val canvas: SketchCanvas
    private var animationTimer: Timer? = null

    /**
     * Creates a new SketchRunner for the given sketch.
     *
     * @param sketch The sketch to run
     * @param width The width of the window
     * @param height The height of the window
     * @param title The title of the window
     */
    init {
        this.canvas = SketchCanvas(width, height)

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
     * Sets up the 60 FPS animation timer.
     */
    private fun setupAnimation() {
        // Call init() once
        sketch.init(canvas.getWidth(), canvas.getHeight())


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
     * Stops the sketch animation.
     */
    fun stop() {
        if (animationTimer != null && animationTimer!!.isRunning()) {
            animationTimer!!.stop()
        }
    }

    /**
     * Custom canvas that handles the drawing.
     */
    private inner class SketchCanvas(width: Int, height: Int) : JPanel() {
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


            // Call the sketch's draw method
            sketch.draw(g2d, getWidth(), getHeight())

            g2d.dispose()
        }
    }

    companion object {
        private const val TARGET_FPS = 60
        private val FRAME_DELAY: Int = 1000 / TARGET_FPS
    }
}
