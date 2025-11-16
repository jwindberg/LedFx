package com.marsraver.LedFx

import java.awt.Graphics2D

/**
 * Enhanced sketch interface that supports LED output.
 * Extends the basic Sketch interface with LED grid functionality.
 */
interface LedSketch : Sketch {
    /**
     * Called once when the sketch starts, with LED grid support.
     * Use this to set up the window size, LED grid, and initial configuration.
     *
     * @param width The width of the drawing canvas
     * @param height The height of the drawing canvas
     * @param ledGrid The LED grid for output to WLED devices
     */
    fun init(width: Int, height: Int, ledGrid: LedGrid?)

    /**
     * Called continuously at 60 FPS for animations, with LED grid support.
     * Use this to draw your graphics and update LED colors.
     *
     * @param g The Graphics2D object for drawing
     * @param width The current width of the canvas
     * @param height The current height of the canvas
     * @param ledGrid The LED grid for output to WLED devices
     */
    fun draw(g: Graphics2D?, width: Int, height: Int, ledGrid: LedGrid?)
}

