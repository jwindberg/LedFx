package com.marsraver.LedFx

import com.marsraver.LedFx.animations.VideoPlayerAnimation
import com.marsraver.LedFx.layout.LayoutLoader
import com.marsraver.LedFx.wled.WledDdpClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

/**
 * Generic sketch runner that supports any LED animation.
 * Manages both the sketch window and dual LED grid communication.
 */
class AnimationSketchRunner(initialAnimationType: AnimationType, layoutName: String) {
    var frame: JFrame? = null
        private set
    private var canvas: AnimationSketchCanvas? = null
    private var ledGrid: LedGrid? = null

    /**
     * Gets the current animation.
     *
     * @return The current animation
     */
    var animation: LedAnimation? = null
        private set
    private var animationTimer: Timer? = null
    private var animationSelector: JComboBox<AnimationType?>? = null

    /**
     * Creates a new AnimationSketchRunner for the given animation and layout.
     *
     * @param initialAnimationType The initial animation type to run
     * @param layoutName The name of the layout to load
     */
    init {
        try {
            // Load the layout configuration
            val layout = LayoutLoader.loadLayout(layoutName)


            // Create the unified LED grid
            this.ledGrid = LedGrid(layout)
            this.canvas = AnimationSketchCanvas(layout.windowWidth, layout.windowHeight)


            // Create the initial animation immediately to prevent null reference
            this.animation = AnimationType.createAnimation(initialAnimationType)
            this.animation!!.init(layout.windowWidth, layout.windowHeight, this.ledGrid!!)

            setupWindow(layout.title)
            setupAnimationSelector(initialAnimationType)
            setupAnimation()
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize AnimationSketchRunner", e)
        }
    }

    /**
     * Sets up the window and canvas.
     */
    private fun setupWindow(title: String?) {
        frame = JFrame(title)
        frame!!.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE) // Handle close manually
        frame!!.setResizable(false)


        // Add window close listener
        frame!!.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                // Ensure we stop timers, animations, and external resources (e.g., video/audio)
                stop()
                System.exit(0)
            }
        })


        // Create main panel with border layout
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(canvas, BorderLayout.CENTER)


        // Create top panel for animation selector
        val topPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        topPanel.setBackground(Color.DARK_GRAY)


        // Add animation selector to top right
        val animationLabel = JLabel("Animation:")
        animationLabel.setForeground(Color.WHITE)
        topPanel.add(animationLabel)

        val sortedAnimations = AnimationType.entries
            .sortedBy { it.displayName.lowercase() }
            .toTypedArray()

        animationSelector = JComboBox(sortedAnimations)
        animationSelector!!.setBackground(Color.WHITE)
        animationSelector!!.addActionListener { this.onAnimationChanged(null) }
        topPanel.add(animationSelector)

        mainPanel.add(topPanel, BorderLayout.NORTH)
        frame!!.add(mainPanel)
        frame!!.pack()
        frame!!.setLocationRelativeTo(null)
        frame!!.setVisible(true)
    }

    /**
     * Sets up the animation selector with the initial animation type.
     */
    private fun setupAnimationSelector(initialAnimationType: AnimationType) {
        animationSelector!!.selectedItem = initialAnimationType
        switchToAnimation(initialAnimationType)
    }

    /**
     * Handles animation selection changes.
     */
    private fun onAnimationChanged(@Suppress("UNUSED_PARAMETER") e: ActionEvent?) {
        val selectedType = animationSelector!!.selectedItem as? AnimationType ?: return
        switchToAnimation(selectedType)
    }

    /**
     * Switches to the specified animation type.
     */
    private fun switchToAnimation(animationType: AnimationType) {
        // Stop current animation timer
        animationTimer?.stop()


        // Stop the current animation if it exists
        animation?.stop()


        // Create new animation
        animation = AnimationType.createAnimation(animationType)
        log.debug("AnimationFactory returned: ${animation!!.javaClass.simpleName}")

        // Initialize the new animation
        try {
            animation!!.init(canvas!!.width, canvas!!.height, ledGrid!!)
            log.debug("Animation initialized successfully")
        } catch (e: Exception) {
            log.error("Error initializing animation: ${e.message}", e)
            e.printStackTrace()
            animation = null
        }


        // Restart animation timer
        restartAnimationTimer()

        log.debug("Switched to animation: ${animation?.getName()}")
    }

    /**
     * Restarts the animation timer to keep the UI responsive
     */
    private fun restartAnimationTimer() {
        if (animationTimer == null) {
            animationTimer = Timer(FRAME_DELAY) { canvas!!.repaint() }
        }
        animationTimer!!.start()
    }

    /**
     * Sets up the 60 FPS animation timer with dual LED output.
     */
    private fun setupAnimation() {
        // Animation is now set up in switchToAnimation method
        // This method is kept for compatibility but does nothing
    }

    /**
     * Starts the animation.
     */
    fun start() {
        if (animationTimer?.isRunning == false) {
            animationTimer!!.start()
        }
    }

    /**
     * Stops the animation.
     */
    fun stop() {
        animationTimer?.stop()


        // Stop the current animation if it exists
        animation?.stop()
    }


    /**
     * Custom canvas that handles drawing and dual LED output.
     */
    private inner class AnimationSketchCanvas(width: Int, height: Int) : JPanel() {
        init {
            setPreferredSize(Dimension(width, height))
            setBackground(Color.BLACK)

            // Enable mouse interaction for animations that support seeking (e.g., VideoPlayerAnimation)
            val mouseHandler: MouseAdapter = object : MouseAdapter() {
                private var dragging = false

                override fun mousePressed(e: MouseEvent) {
                    if (handleSeekEvent(e)) {
                        dragging = true
                    }
                }

                override fun mouseReleased(e: MouseEvent?) {
                    dragging = false
                }

                override fun mouseDragged(e: MouseEvent) {
                    if (dragging) {
                        handleSeekEvent(e)
                    }
                }

                fun handleSeekEvent(e: MouseEvent): Boolean {
                    val vp = animation as? VideoPlayerAnimation ?: return false

                    val canvasWidth = this@AnimationSketchCanvas.width
                    val canvasHeight = this@AnimationSketchCanvas.height

                    // Must match scrollbar geometry used in VideoPlayerAnimation.drawScrollbar
                    val scrollbarWidth = canvasWidth - 40
                    val scrollbarX = 20
                    val scrollbarY = canvasHeight - 40
                    val scrollbarHeight = 15

                    val mx = e.x
                    val my = e.y

                    if (mx < scrollbarX || mx > scrollbarX + scrollbarWidth) return false
                    if (my < scrollbarY || my > scrollbarY + scrollbarHeight) return false

                    var position = (mx - scrollbarX) / scrollbarWidth.toDouble()
                    position = max(0.0, min(1.0, position))

                    vp.seekTo(position)
                    return true
                }
            }

            addMouseListener(mouseHandler)
            addMouseMotionListener(mouseHandler)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g.create() as Graphics2D


            // Enable anti-aliasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)


            // Call the animation's draw method with LED grid (if animation exists)
            if (animation != null) {
                animation!!.draw(g2d, width, height, ledGrid!!)


                // Send colors to LED devices (animations set colors directly)
                ledGrid!!.sendToDevices()
            } else {
                // Show error message when animation failed to load
                log.debug("DEBUG: animation is null in paintComponent - showing error message")
                g2d.setColor(Color.RED)
                g2d.setFont(Font("Arial", Font.BOLD, 16))
                val errorMsg = "Animation failed to load - not yet updated for unified LedGrid system"
                val fm = g2d.getFontMetrics()
                val x = (getWidth() - fm.stringWidth(errorMsg)) / 2
                val y = getHeight() / 2
                g2d.drawString(errorMsg, x, y)
            }


            // Draw the LED grid overlay (optional, for visualization)
            ledGrid!!.drawGrid(g2d)

            g2d.dispose()
        }
    }

    /**
     * Gets the LED grid for external access.
     *
     * @return The LED grid
     */
    fun getLedGrid(): LedGrid {
        return ledGrid!!
    }

    /**
     * Gets the WLED controller for a specific grid.
     *
     * @param gridIndex The index of the grid
     * @return The WLED DDP client for that grid
     */
    fun getController(gridIndex: Int): WledDdpClient? {
        return ledGrid!!.getController(gridIndex)
    }

    companion object {
        private const val TARGET_FPS = 120 // Increased from 60 to 120 FPS
        private val FRAME_DELAY: Int = 1000 / TARGET_FPS
        private val log: Logger = LogManager.getLogger(AnimationSketchRunner::class.java)
    }
}
