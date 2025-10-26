package com.marsraver.LedFx;

import com.marsraver.LedFx.layout.LayoutConfig;
import com.marsraver.LedFx.layout.LayoutLoader;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

/**
 * Generic sketch runner that supports any LED animation.
 * Manages both the sketch window and dual LED grid communication.
 */
public class AnimationSketchRunner {
    
    private static final int TARGET_FPS = 120; // Increased from 60 to 120 FPS
    private static final int FRAME_DELAY = 1000 / TARGET_FPS;
    
    public JFrame frame;
    private AnimationSketchCanvas canvas;
    private LedGrid ledGrid;
    private LedAnimation animation;
    private Timer animationTimer;
    private JComboBox<AnimationType> animationSelector;
    
    /**
     * Creates a new AnimationSketchRunner for the given animation and layout.
     * 
     * @param initialAnimationType The initial animation type to run
     * @param layoutName The name of the layout to load
     */
    public AnimationSketchRunner(AnimationType initialAnimationType, String layoutName) {
        try {
            // Load the layout configuration
            LayoutConfig layout = LayoutLoader.loadLayout(layoutName);
            if (layout == null) {
                throw new IllegalArgumentException("Layout not found: " + layoutName);
            }
            
            // Create the unified LED grid
            this.ledGrid = new LedGrid(layout);
            this.canvas = new AnimationSketchCanvas(layout.getWindowWidth(), layout.getWindowHeight());
            
            // Create the initial animation immediately to prevent null reference
            this.animation = AnimationFactory.createAnimation(initialAnimationType);
            if (this.animation != null) {
                this.animation.init(layout.getWindowWidth(), layout.getWindowHeight(), this.ledGrid);
            }
            
            setupWindow(layout.getTitle());
            setupAnimationSelector(initialAnimationType);
            setupAnimation();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AnimationSketchRunner", e);
        }
    }
    
    /**
     * Sets up the window and canvas.
     */
    private void setupWindow(String title) {
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close manually
        frame.setResizable(false);
        
        // Add window close listener to clear LEDs before exiting
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                clearLeds();
                System.exit(0);
            }
        });
        
        // Create main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(canvas, BorderLayout.CENTER);
        
        // Create top panel for animation selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(Color.DARK_GRAY);
        
        // Add animation selector to top right
        JLabel animationLabel = new JLabel("Animation:");
        animationLabel.setForeground(Color.WHITE);
        topPanel.add(animationLabel);
        
        animationSelector = new JComboBox<>(AnimationType.values());
        animationSelector.setBackground(Color.WHITE);
        animationSelector.addActionListener(this::onAnimationChanged);
        topPanel.add(animationSelector);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    /**
     * Sets up the animation selector with the initial animation type.
     */
    private void setupAnimationSelector(AnimationType initialAnimationType) {
        animationSelector.setSelectedItem(initialAnimationType);
        switchToAnimation(initialAnimationType);
    }
    
    /**
     * Handles animation selection changes.
     */
    private void onAnimationChanged(ActionEvent e) {
        AnimationType selectedType = (AnimationType) animationSelector.getSelectedItem();
        if (selectedType != null) {
            switchToAnimation(selectedType);
        }
    }
    
    /**
     * Switches to the specified animation type.
     */
    private void switchToAnimation(AnimationType animationType) {
        // Stop current animation timer
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        // Create new animation
        animation = AnimationFactory.createAnimation(animationType);
        System.out.println("AnimationFactory returned: " + (animation != null ? animation.getClass().getSimpleName() : "null"));
        if (animation == null) {
            System.err.println("Failed to create animation: " + animationType);
            // Clear LEDs when animation fails
            clearLeds();
            // Still need to restart timer to keep the UI responsive
            restartAnimationTimer();
            return;
        }
        
        // Initialize the new animation
        try {
            animation.init(canvas.getWidth(), canvas.getHeight(), ledGrid);
            System.out.println("Animation initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing animation: " + e.getMessage());
            e.printStackTrace();
            animation = null;
        }
        
        // Restart animation timer
        restartAnimationTimer();
        
        System.out.println("Switched to animation: " + animation.getName());
    }
    
    /**
     * Restarts the animation timer to keep the UI responsive
     */
    private void restartAnimationTimer() {
        if (animationTimer == null) {
            animationTimer = new Timer(FRAME_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.repaint();
                }
            });
        }
        animationTimer.start();
    }

    /**
     * Sets up the 60 FPS animation timer with dual LED output.
     */
    private void setupAnimation() {
        // Animation is now set up in switchToAnimation method
        // This method is kept for compatibility but does nothing
    }
    
    /**
     * Starts the animation.
     */
    public void start() {
        if (animationTimer != null && !animationTimer.isRunning()) {
            animationTimer.start();
        }
    }
    
    /**
     * Stops the animation and turns off LEDs on both devices.
     */
    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // Clear all LEDs and turn off both devices
        clearLeds();
    }
    
    /**
     * Clears all LEDs on all devices by setting them to black and turning off the devices.
     */
    public void clearLeds() {
        // Clear the LED grid data
        ledGrid.clearAllLeds();
        
        // Send the cleared data to all devices
        ledGrid.sendToDevices();
        
        // Turn off all WLED devices
        for (int i = 0; i < ledGrid.getGridCount(); i++) {
            var controller = ledGrid.getController(i);
            if (controller != null) {
                controller.turnOff();
            }
        }
        
        System.out.println("LEDs cleared and devices turned off");
    }
    
    /**
     * Custom canvas that handles drawing and dual LED output.
     */
    private class AnimationSketchCanvas extends JPanel {
        
        public AnimationSketchCanvas(int width, int height) {
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
            
            // Call the animation's draw method with LED grid (if animation exists)
            if (animation != null) {
                animation.draw(g2d, getWidth(), getHeight(), ledGrid);
                
                // Send colors to LED devices (animations set colors directly)
                ledGrid.sendToDevices();
            } else {
                // Show error message when animation failed to load
                System.out.println("DEBUG: animation is null in paintComponent - showing error message");
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                String errorMsg = "Animation failed to load - not yet updated for unified LedGrid system";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(errorMsg)) / 2;
                int y = getHeight() / 2;
                g2d.drawString(errorMsg, x, y);
            }
            
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
     * @return The WLED controller for that grid
     */
    public com.marsraver.LedFx.wled.WledController getController(int gridIndex) {
        return ledGrid.getController(gridIndex);
    }
    
    /**
     * Gets the current animation.
     * 
     * @return The current animation
     */
    public LedAnimation getAnimation() {
        return animation;
    }
}
