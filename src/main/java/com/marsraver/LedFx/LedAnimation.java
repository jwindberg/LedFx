package com.marsraver.LedFx;

import java.awt.*;

/**
 * Interface for LED animations that work with the unified LedGrid system.
 * Animations can now work with any layout configuration without knowing
 * specific details about grid arrangements.
 */
public interface LedAnimation {
    
    /**
     * Initializes the animation with the given parameters.
     * Called once when the animation starts.
     * 
     * @param width The width of the sketch window
     * @param height The height of the sketch window
     * @param ledGrid The unified LED grid system
     */
    void init(int width, int height, LedGrid ledGrid);
    
    /**
     * Updates and draws the animation for the current frame.
     * Called 60 times per second.
     * 
     * @param g The Graphics2D object for drawing
     * @param width The width of the sketch window
     * @param height The height of the sketch window
     * @param ledGrid The unified LED grid system
     */
    void draw(Graphics2D g, int width, int height, LedGrid ledGrid);
    
    /**
     * Gets the name of this animation.
     * 
     * @return The animation name
     */
    String getName();
    
    /**
     * Gets a description of this animation.
     * 
     * @return The animation description
     */
    String getDescription();
}

