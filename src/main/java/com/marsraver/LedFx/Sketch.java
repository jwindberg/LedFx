package com.marsraver.LedFx;

import java.awt.Graphics2D;

/**
 * Interface for creating Processing-like sketches.
 * Users must implement init() and draw() methods.
 */
public interface Sketch {
    
    /**
     * Called once when the sketch starts.
     * Use this to set up the window size and initial configuration.
     * 
     * @param width The width of the drawing canvas
     * @param height The height of the drawing canvas
     */
    void init(int width, int height);
    
    /**
     * Called continuously at 60 FPS for animations.
     * Use this to draw your graphics and create animations.
     * 
     * @param g The Graphics2D object for drawing
     * @param width The current width of the canvas
     * @param height The current height of the canvas
     */
    void draw(Graphics2D g, int width, int height);
}

