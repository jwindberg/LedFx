package com.marsraver.LedFx;

import com.marsraver.LedFx.wled.WledController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Enhanced sketch runner that supports dual LED output to two WLED devices.
 * Manages both the sketch window and dual LED grid communication.
 */
public class DualLedSketchRunner {
    
    private static final int TARGET_FPS = 60;
    private static final int FRAME_DELAY = 1000 / TARGET_FPS;
    
    public JFrame frame;
    private DualLedSketchCanvas canvas;
    private DualLedSketch sketch;
    private DualLedGrid dualLedGrid;
    private Timer animationTimer;
    private WledController leftController;
    private WledController rightController;
    
    /**
     * Creates a new DualLedSketchRunner for the given sketch and dual WLED devices.
     * 
     * @param sketch The dual LED sketch to run
     * @param width The width of the window
     * @param height The height of the window
     * @param title The title of the window
     * @param leftDeviceIp The IP address of the left WLED device
     * @param rightDeviceIp The IP address of the right WLED device
     * @param ledCount The number of LEDs on each device
     */
    public DualLedSketchRunner(DualLedSketch sketch, int width, int height, String title, 
                              String leftDeviceIp, String rightDeviceIp, int ledCount) {
        this.sketch = sketch;
        this.leftController = new WledController(leftDeviceIp, ledCount);
        this.rightController = new WledController(rightDeviceIp, ledCount);
        this.dualLedGrid = new DualLedGrid(width, height, leftController, rightController);
        this.canvas = new DualLedSketchCanvas(width, height);
        
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
     * Sets up the 60 FPS animation timer with dual LED output.
     */
    private void setupAnimation() {
        // Call init() once with dual LED grid
        sketch.init(canvas.getWidth(), canvas.getHeight(), dualLedGrid);
        
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
     * Stops the sketch animation and turns off LEDs on both devices.
     */
    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        // Clear all LEDs and turn off both devices
        clearLeds();
    }
    
    /**
     * Clears all LEDs on both devices by setting them to black and turning off the devices.
     */
    public void clearLeds() {
        // Clear the LED grid data
        dualLedGrid.clearAllLeds();
        
        // Send the cleared data to both devices
        dualLedGrid.sendToDevices();
        
        // Turn off both WLED devices
        leftController.turnOff();
        rightController.turnOff();
        
        System.out.println("LEDs cleared and devices turned off");
    }
    
    /**
     * Custom canvas that handles drawing and dual LED output.
     */
    private class DualLedSketchCanvas extends JPanel {
        
        public DualLedSketchCanvas(int width, int height) {
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
            
            // Call the sketch's draw method with dual LED grid
            sketch.draw(g2d, getWidth(), getHeight(), dualLedGrid);
            
            // Sample colors from the sketch and send to both LED devices
            dualLedGrid.sampleColors(g2d);
            dualLedGrid.sendToDevices();
            
            // Draw the dual LED grid overlay (optional, for visualization)
            dualLedGrid.drawGrids(g2d);
            
            g2d.dispose();
        }
    }
    
    /**
     * Gets the dual LED grid for external access.
     * 
     * @return The dual LED grid
     */
    public DualLedGrid getDualLedGrid() {
        return dualLedGrid;
    }
    
    /**
     * Gets the left WLED controller for external access.
     * 
     * @return The left WLED controller
     */
    public WledController getLeftController() {
        return leftController;
    }
    
    /**
     * Gets the right WLED controller for external access.
     * 
     * @return The right WLED controller
     */
    public WledController getRightController() {
        return rightController;
    }
}
