package com.marsraver.LedFx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Runs a Processing-like sketch with a 60 FPS animation loop.
 * Creates a window and calls the sketch's init() and draw() methods.
 */
public class SketchRunner {
    
    private static final int TARGET_FPS = 60;
    private static final int FRAME_DELAY = 1000 / TARGET_FPS;
    
    public JFrame frame;
    private SketchCanvas canvas;
    private Sketch sketch;
    private Timer animationTimer;
    
    /**
     * Creates a new SketchRunner for the given sketch.
     * 
     * @param sketch The sketch to run
     * @param width The width of the window
     * @param height The height of the window
     * @param title The title of the window
     */
    public SketchRunner(Sketch sketch, int width, int height, String title) {
        this.sketch = sketch;
        this.canvas = new SketchCanvas(width, height);
        
        setupWindow(title);
        setupAnimation();
    }
    
    /**
     * Sets up the window and canvas.
     */
    private void setupWindow(String title) {
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    /**
     * Sets up the 60 FPS animation timer.
     */
    private void setupAnimation() {
        // Call init() once
        sketch.init(canvas.getWidth(), canvas.getHeight());
        
        // Set up the animation timer
        animationTimer = new Timer(FRAME_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.repaint();
            }
        });
        animationTimer.start();
    }
    
    /**
     * Starts the sketch animation.
     */
    public void start() {
        if (animationTimer != null && !animationTimer.isRunning()) {
            animationTimer.start();
        }
    }
    
    /**
     * Stops the sketch animation.
     */
    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
    
    /**
     * Custom canvas that handles the drawing.
     */
    private class SketchCanvas extends JPanel {
        
        public SketchCanvas(int width, int height) {
            setPreferredSize(new Dimension(width, height));
            setBackground(Color.BLACK);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Enable anti-aliasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Call the sketch's draw method
            sketch.draw(g2d, getWidth(), getHeight());
            
            g2d.dispose();
        }
        
    }
}
