package com.marsraver.LedFx;

import com.marsraver.LedFx.wled.WledDevice;
import com.marsraver.LedFx.wled.WledNetworkScanner;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Launcher class for running LED sketches with unified LED integration.
 * Provides a simple way to start LED sketches with keyboard controls.
 */
public class LedSketchLauncher {
    
    public static void main(String[] args) {
        // Default layout name
        String layoutName = "TwoGrids";
        
        // Parse command line arguments
        if (args.length > 0) {
            layoutName = args[0];
        }
        
        // Run the LED example sketch with the specified layout
        runLedExampleSketch(layoutName);
    }
    
    
    /**
     * Runs the LED example bouncing ball sketch.
     * 
     * @param layoutName The name of the layout to use
     */
    public static void runLedExampleSketch(String layoutName) {
        SwingUtilities.invokeLater(() -> {
            LedExampleSketch sketch = new LedExampleSketch();
            LedSketchRunner runner = new LedSketchRunner(sketch, layoutName);
            
            // Add keyboard listener for ESC to exit
            runner.frame.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        runner.stop(); // Turn off LEDs before exiting
                        System.exit(0);
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {}
            });
            
            // Make sure the frame can receive key events
            runner.frame.setFocusable(true);
            runner.frame.requestFocus();
            
            System.out.println("LED Sketch started with layout: " + layoutName);
            System.out.println("Press ESC to exit and turn off LEDs");
        });
    }
    
    /**
     * Runs a custom LED sketch.
     * 
     * @param sketch The LED sketch to run
     * @param layoutName The name of the layout to use
     */
    public static void runLedSketch(LedSketch sketch, String layoutName) {
        SwingUtilities.invokeLater(() -> {
            LedSketchRunner runner = new LedSketchRunner(sketch, layoutName);
            
            // Add keyboard listener for ESC to exit
            runner.frame.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        runner.stop(); // Turn off LEDs before exiting
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
     * Discovers WLED devices on the network and runs a sketch with the first two found.
     * 
     * @param sketch The LED sketch to run
     * @param layoutName The name of the layout to use
     */
    public static void runWithAutoDiscovery(LedSketch sketch, String layoutName) {
        try {
            System.out.println("🔍 Discovering WLED devices...");
            var devices = WledNetworkScanner.discover();
            
            if (devices.size() < 2) {
                System.err.println("❌ Need at least 2 WLED devices found on the network!");
                System.err.println("Found only " + devices.size() + " devices.");
                return;
            }
            
            WledDevice leftDevice = devices.get(0);
            WledDevice rightDevice = devices.get(1);
            System.out.println("✅ Using left device: " + leftDevice);
            System.out.println("✅ Using right device: " + rightDevice);
            
            runLedSketch(sketch, layoutName);
            
        } catch (Exception e) {
            System.err.println("❌ Error discovering WLED devices: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
