package com.marsraver.LedFx

import java.awt.Graphics2D

/**
 * Interface for LED animations that work with the unified LedGrid system.
 * Animations can now work with any layout configuration without knowing
 * specific details about grid arrangements.
 */
interface LedAnimation {
    /**
     * Initializes the animation with the given parameters.
     * Called once when the animation starts.
     *
     * @param width The width of the sketch window
     * @param height The height of the sketch window
     * @param ledGrid The unified LED grid system
     */
    fun init(width: Int, height: Int, ledGrid: LedGrid)

    /**
     * Updates and draws the animation for the current frame.
     * Called 60 times per second.
     *
     * @param g The Graphics2D object for drawing
     * @param width The width of the sketch window
     * @param height The height of the sketch window
     * @param ledGrid The unified LED grid system
     */
    fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid)

    /**
     * Gets the name of this animation.
     */
    fun getName(): String

    /**
     * Gets a description of this animation.
     */
    fun getDescription(): String

    /**
     * Stops the animation and performs any necessary cleanup.
     * Called when switching animations or closing the application.
     */
    fun stop() {
        // Default implementation does nothing
        // Animations can override this to perform cleanup
    }
}

