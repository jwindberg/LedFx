package com.marsraver.LedFx.layout;

import com.marsraver.LedFx.AnimationSketchRunner;
import com.marsraver.LedFx.AnimationType;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Launcher for layout-based LED sketches with animation selection.
 * Uses XML layout files to configure window size and LED grid placements.
 */
public class LayoutLauncher {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }
        
        String layoutName = args[0];
        String animationTypeId = args.length > 1 ? args[1] : "bouncing-ball";
        
        try {
            System.out.println("Loading layout: " + layoutName);
            System.out.println("Animation: " + animationTypeId);
            
            // Parse animation type
            AnimationType animationType = AnimationType.fromId(animationTypeId);
            if (animationType == null) {
                System.err.println("Unknown animation type: " + animationTypeId);
                System.err.println(AnimationType.getAvailableAnimations());
                return;
            }
            
            // Create and run the layout sketch with animation selection
            AnimationSketchRunner runner = new AnimationSketchRunner(animationType, layoutName);
            
            // Add keyboard listener for ESC to exit
            runner.frame.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        runner.clearLeds();
                        System.exit(0);
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {}
            });
            
            // Make sure the frame can receive key events
            runner.frame.setFocusable(true);
            runner.frame.requestFocus();
            
            System.out.println("Layout sketch started!");
            System.out.println("Use the dropdown to switch animations. Press ESC to exit and turn off LEDs");
            
        } catch (Exception e) {
            System.err.println("Failed to start layout sketch: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java LayoutLauncher <layout-name> [animation-type]");
        System.out.println();
        System.out.println("Available layouts:");
        for (String layout : LayoutLoader.listAvailableLayouts()) {
            System.out.println("  - " + layout);
        }
        System.out.println();
        System.out.println("Available animations:");
        System.out.println("  - bouncing-ball");
        System.out.println("  - spinning-beachball");
        System.out.println("  - dj-light");
        System.out.println("  - clouds");
        System.out.println("  - fast-plasma");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java LayoutLauncher TwoGrids bouncing-ball");
    }
}

