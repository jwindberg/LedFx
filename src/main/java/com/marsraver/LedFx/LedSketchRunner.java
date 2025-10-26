package com.marsraver.LedFx;

import com.marsraver.LedFx.layout.LayoutConfig;
import com.marsraver.LedFx.layout.LayoutLoader;
import com.marsraver.LedFx.wled.WledController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Enhanced sketch runner that supports LED output to WLED devices.
 * Manages both the sketch window and LED grid communication.
 */
public class LedSketchRunner {
    
    private static final int TARGET_FPS = 60;
    private static final int FRAME_DELAY = 1000 / TARGET_FPS;
    
    public JFrame frame;
    private LedSketchCanvas canvas;
    private Object sketch;
    private LedGrid ledGrid;
    private Timer animationTimer;
    
    /**
     * Creates a new LedSketchRunner using a layout configuration.
     * 
     * @param sketch The LED sketch to run (can be LedSketch or DualLedSketch)
     * @param layoutName The name of the layout to load
     */
    public LedSketchRunner(Object sketch, String layoutName) {
        this.sketch = sketch;
        
        // Load the layout configuration
        LayoutConfig layout;
        try {
            layout = LayoutLoader.loadLayout(layoutName);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load layout: " + layoutName, e);
        }
        if (layout == null) {
            throw new IllegalArgumentException("Layout not found: " + layoutName);
        }
        
        // Create the unified LED grid
        this.ledGrid = new LedGrid(layout);
        this.canvas = new LedSketchCanvas(layout.getWindowWidth(), layout.getWindowHeight());
        
        setupWindow(layout.getTitle());
        setupAnimation();
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
    public LedSketchRunner(LedSketch sketch, int width, int height, String title, 
                          String wledDeviceIp, int ledCount) {
        this.sketch = sketch;
        
        // Create a simple single-grid layout programmatically
        LayoutConfig layout = new LayoutConfig("SingleGrid", title, width, height);
        
        // Calculate grid dimensions - grid takes up 80% of the window, centered
        int maxGridSize = Math.min(width, height) * 80 / 100;
        int pixelSize = maxGridSize / 16; // Assume 16x16 grid
        int totalGridSize = 16 * pixelSize;
        int gridX = (width - totalGridSize) / 2;
        int gridY = (height - totalGridSize) / 2;
        
        // Add a single grid to the layout
        layout.addGrid(new com.marsraver.LedFx.layout.GridConfig(
            "Grid01", wledDeviceIp, ledCount, gridX, gridY, 
            totalGridSize, totalGridSize, 16, pixelSize));
        
        // Create the unified LED grid
        this.ledGrid = new LedGrid(layout);
        this.canvas = new LedSketchCanvas(width, height);
        
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
     * Sets up the 60 FPS animation timer with LED output.
     */
    private void setupAnimation() {
        // Call init() once with LED grid
        if (sketch instanceof LedSketch) {
            ((LedSketch) sketch).init(canvas.getWidth(), canvas.getHeight(), ledGrid);
        }
        
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
     * Stops the sketch animation and turns off LEDs.
     */
    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        // Turn off all LED devices when stopping
        for (int i = 0; i < ledGrid.getGridCount(); i++) {
            WledController controller = ledGrid.getController(i);
            if (controller != null) {
                controller.turnOff();
            }
        }
    }
    
    /**
     * Custom canvas that handles drawing and LED output.
     */
    private class LedSketchCanvas extends JPanel {
        
        public LedSketchCanvas(int width, int height) {
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
            
            // Call the sketch's draw method with LED grid
            if (sketch instanceof LedSketch) {
                ((LedSketch) sketch).draw(g2d, getWidth(), getHeight(), ledGrid);
            }
            
            // Sample colors from the sketch and send to LED device
            ledGrid.sampleColors(g2d);
            ledGrid.sendToDevices();
            
            // Draw the LED grid overlay (optional, for visualization)
            ledGrid.drawGrid(g2d);
            
            g2d.dispose();
        }
    }
    
    /**
     * Gets the LED grid for external access.
     * 
     * @return The LED grid
     */
    public LedGrid getLedGrid() {
        return ledGrid;
    }
    
    /**
     * Gets the WLED controller for a specific grid.
     * 
     * @param gridIndex The index of the grid
     * @return The WLED controller
     */
    public WledController getWledController(int gridIndex) {
        return ledGrid.getController(gridIndex);
    }
    
    /**
     * Gets the WLED controller for a grid by ID.
     * 
     * @param gridId The ID of the grid
     * @return The WLED controller
     */
    public WledController getWledController(String gridId) {
        return ledGrid.getController(gridId);
    }
}
