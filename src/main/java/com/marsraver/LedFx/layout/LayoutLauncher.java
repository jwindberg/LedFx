package com.marsraver.LedFx.layout;

import com.marsraver.LedFx.AnimationSketchRunner;
import com.marsraver.LedFx.AnimationType;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Launcher for layout-based LED sketches with animation selection.
 * Uses XML layout files to configure window size and LED grid placements.
 */
@Log4j2
public class LayoutLauncher {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }
        
        String layoutName = args[0];
        String animationTypeId = args.length > 1 ? args[1] : "test";
        
        try {
            log.debug("Loading layout: " + layoutName);
            log.debug("Animation: " + animationTypeId);
            
            // Parse animation type
            AnimationType animationType = AnimationType.fromId(animationTypeId);
            if (animationType == null) {
                log.error("Unknown animation type: " + animationTypeId);
                log.error(AnimationType.getAvailableAnimations());
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
                        System.exit(0);
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {}
            });
            
            // Make sure the frame can receive key events
            runner.frame.setFocusable(true);
            runner.frame.requestFocus();
            
            log.debug("Layout sketch started!");
            log.debug("Use the dropdown to switch animations. Press ESC to exit");
            log.debug("");
            log.debug("Note: WLED devices may revert to their idle state when the app closes.");
            log.debug("This is normal device behavior and can be configured in WLED settings.");
            
        } catch (Exception e) {
            log.error("Failed to start layout sketch: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java LayoutLauncher <layout-name> [animation-type]");
        System.out.println("");
        System.out.println("Available layouts:");
        for (String layout : LayoutLoader.listAvailableLayouts()) {
            System.out.println("  - " + layout);
        }
        System.out.println();
        System.out.println("Available animations:");
        System.out.println("  - test");
        System.out.println("  - spinning-beachball");
        System.out.println("  - bouncing-ball");
        System.out.println("  - music-ball");
        System.out.println("  - video-player");
        System.out.println("  - fast-plasma");
        System.out.println("  - clouds");
        System.out.println("");
        System.out.println("Example:");
        System.out.println("  java LayoutLauncher TwoGrids test");
    }
}

