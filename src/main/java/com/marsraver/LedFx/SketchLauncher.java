package com.marsraver.LedFx;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Launcher class for running Processing-like sketches.
 * Provides a simple way to start sketches with keyboard controls.
 */
public class SketchLauncher {
    
    public static void main(String[] args) {
        // Run the example sketch
        runExampleSketch();
    }
    
    /**
     * Runs the example bouncing ball sketch.
     */
    public static void runExampleSketch() {
        SwingUtilities.invokeLater(() -> {
            ExampleSketch sketch = new ExampleSketch();
            SketchRunner runner = new SketchRunner(sketch, 800, 600, "LedFx - Example Sketch");
            
            // Add keyboard listener for ESC to exit
            runner.frame.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        System.exit(0);
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {}
            });
            
            // Make sure the frame can receive key events
            runner.frame.setFocusable(true);
            runner.frame.requestFocus();
        });
    }
    
    /**
     * Runs a custom sketch.
     * 
     * @param sketch The sketch to run
     * @param width Window width
     * @param height Window height
     * @param title Window title
     */
    public static void runSketch(Sketch sketch, int width, int height, String title) {
        SwingUtilities.invokeLater(() -> {
            SketchRunner runner = new SketchRunner(sketch, width, height, title);
            
            // Add keyboard listener for ESC to exit
            runner.frame.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        System.exit(0);
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {}
            });
            
            // Make sure the frame can receive key events
            runner.frame.setFocusable(true);
            runner.frame.requestFocus();
        });
    }
}

