package com.marsraver.LedFx.layout;

import com.marsraver.LedFx.LedAnimation;
import com.marsraver.LedFx.AnimationFactory;
import com.marsraver.LedFx.AnimationType;
import com.marsraver.LedFx.DualLedGrid;
import com.marsraver.LedFx.DualLedGridAdapter;
import com.marsraver.LedFx.wled.WledController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Sketch runner that works with layout configurations.
 * Manages the window and animation loop based on layout settings.
 */
public class LayoutSketchRunner {
    
    private static final int TARGET_FPS = 60;
    private static final int FRAME_DELAY = 1000 / TARGET_FPS;
    
    public JFrame frame;
    private LayoutSketchCanvas canvas;
    private LedAnimation animation;
    private LayoutLedGrid layoutLedGrid;
    private DualLedGrid dualLedGrid;
    private Timer animationTimer;
    private JComboBox<AnimationType> animationSelector;
    
    public LayoutSketchRunner(String layoutName, String initialAnimation) throws Exception {
        // Load layout configuration
        LayoutConfig layout = LayoutLoader.loadLayout(layoutName);
        this.layoutLedGrid = new LayoutLedGrid(layout);
        
        // Create a DualLedGrid from the layout for backward compatibility
        this.dualLedGrid = createDualLedGridFromLayout(layout);
        
        // Parse initial animation type
        AnimationType animationType = AnimationType.fromId(initialAnimation);
        if (animationType == null) {
            animationType = AnimationType.BOUNCING_BALL;
        }
        
        // Create animation
        this.animation = AnimationFactory.createAnimation(animationType);
        if (animation == null) {
            throw new IllegalArgumentException("Failed to create animation: " + initialAnimation);
        }
        
        // Initialize animation with the dual LED grid
        animation.init(layout.getWindowWidth(), layout.getWindowHeight(), dualLedGrid);
        
        // Setup window
        setupWindow(layout);
        
        // Setup animation selector
        setupAnimationSelector(animationType);
        
        // Start animation
        startAnimation();
    }
    
    private void setupWindow(LayoutConfig layout) {
        frame = new JFrame(layout.getTitle());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        
        // Create canvas
        canvas = new LayoutSketchCanvas(layout, layoutLedGrid);
        canvas.setPreferredSize(new Dimension(layout.getWindowWidth(), layout.getWindowHeight()));
        
        // Add canvas to frame
        frame.add(canvas);
        
        // Handle window close
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                clearLeds();
                System.exit(0);
            }
        });
        
        // Pack and center
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private void setupAnimationSelector(AnimationType initialAnimationType) {
        // Create animation selector panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(Color.DARK_GRAY);
        
        JLabel label = new JLabel("Animation:");
        label.setForeground(Color.WHITE);
        controlPanel.add(label);
        
        animationSelector = new JComboBox<>(AnimationType.values());
        animationSelector.setSelectedItem(initialAnimationType);
        animationSelector.addActionListener(this::onAnimationChanged);
        controlPanel.add(animationSelector);
        
        // Add control panel to frame
        frame.add(controlPanel, BorderLayout.NORTH);
        
        // Adjust canvas size to account for control panel
        int controlHeight = controlPanel.getPreferredSize().height;
        canvas.setPreferredSize(new Dimension(layoutLedGrid.getLayout().getWindowWidth(), 
                                            layoutLedGrid.getLayout().getWindowHeight() - controlHeight));
        frame.pack();
    }
    
    private void onAnimationChanged(ActionEvent e) {
        AnimationType selectedType = (AnimationType) animationSelector.getSelectedItem();
        if (selectedType != null) {
            switchToAnimation(selectedType);
        }
    }
    
    private void switchToAnimation(AnimationType animationType) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animation = AnimationFactory.createAnimation(animationType);
        if (animation == null) {
            System.err.println("Failed to create animation: " + animationType);
            return;
        }
        
        // Initialize animation with layout dimensions
        LayoutConfig layout = layoutLedGrid.getLayout();
        animation.init(layout.getWindowWidth(), layout.getWindowHeight(), dualLedGrid);
        
        if (animationTimer == null) {
            animationTimer = new Timer(FRAME_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.repaint();
                }
            });
        }
        
        animationTimer.start();
        System.out.println("Switched to animation: " + animation.getName());
    }
    
    private void startAnimation() {
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
    
    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        clearLeds();
    }
    
    public void clearLeds() {
        layoutLedGrid.clearAllLeds();
        layoutLedGrid.sendToDevices();
        layoutLedGrid.turnOffAllDevices();
        System.out.println("LEDs cleared and devices turned off");
    }
    
    /**
     * Canvas that renders the animation and handles LED mapping.
     */
    private class LayoutSketchCanvas extends JPanel {
        
        private final LayoutConfig layout;
        private final LayoutLedGrid layoutLedGrid;
        
        public LayoutSketchCanvas(LayoutConfig layout, LayoutLedGrid layoutLedGrid) {
            this.layout = layout;
            this.layoutLedGrid = layoutLedGrid;
            setBackground(Color.BLACK);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw animation
            if (animation != null) {
                drawAnimation(g2d);
            }
            
            // Draw LED grid overlays
            drawGridOverlays(g2d);
            
            g2d.dispose();
        }
        
        private void drawAnimation(Graphics2D g) {
            // Clear all LEDs first
            layoutLedGrid.clearAllLeds();
            
            // Draw the animation with the dual LED grid
            animation.draw(g, getWidth(), getHeight(), dualLedGrid);
            
            // Send LED data to devices
            layoutLedGrid.sendToDevices();
        }
        
        
        private void drawGridOverlays(Graphics2D g) {
            // Draw the visual LED grid structure (grid lines and LED indicators)
            layoutLedGrid.drawGrids(g);
        }
    }
    
    /**
     * Creates a DualLedGrid from a layout configuration for backward compatibility.
     * This allows existing animations to work with the layout system.
     */
    private DualLedGrid createDualLedGridFromLayout(LayoutConfig layout) {
        // Get the first two grids from the layout
        if (layout.getGridCount() < 2) {
            throw new IllegalArgumentException("Layout must contain at least two grids for DualLedGrid compatibility");
        }
        
        GridConfig leftGrid = layout.getGrids().get(0);
        GridConfig rightGrid = layout.getGrids().get(1);
        
        // Create WLED controllers
        WledController leftController = new WledController(leftGrid.getDeviceIp(), leftGrid.getLedCount());
        WledController rightController = new WledController(rightGrid.getDeviceIp(), rightGrid.getLedCount());
        
        // Create DualLedGrid with the layout's window dimensions
        return new DualLedGrid(layout.getWindowWidth(), layout.getWindowHeight(), leftController, rightController);
    }
}
