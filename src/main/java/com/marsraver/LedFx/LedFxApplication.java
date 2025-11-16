package com.marsraver.LedFx;

import com.marsraver.LedFx.layout.LayoutLoader;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@Log4j2
public class LedFxApplication {

	public static void main(String[] args) {
		// Launch the LED layout application on the Swing EDT
		SwingUtilities.invokeLater(() -> {
			try {
				// Get layout name from args or use default
				String layoutName = args.length > 0 ? args[0] : "FourGrids";
				
				// Get animation type from args or use default
				String animationTypeId = args.length > 1 ? args[1] : "test";
				
				log.info("Starting LED Layout application with layout: {} and animation: {}", layoutName, animationTypeId);
				
				// Validate layout exists
				if (!LayoutLoader.listAvailableLayouts().contains(layoutName)) {
					log.error("Layout '{}' not found. Available layouts: {}", layoutName, LayoutLoader.listAvailableLayouts());
					log.error("Using default layout: FourGrids");
					layoutName = "FourGrids";
				}
				
				// Parse animation type
				AnimationType animationType = AnimationType.fromId(animationTypeId);
				if (animationType == null) {
					log.error("Unknown animation type: {}", animationTypeId);
					log.error(AnimationType.getAvailableAnimations());
					log.error("Using default animation: test");
					animationType = AnimationType.TEST;
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
							log.info("Exiting application...");
							runner.stop();
							System.exit(0);
						}
					}
					
					@Override
					public void keyReleased(KeyEvent e) {}
				});
				
				// Make sure the frame can receive key events
				runner.frame.setFocusable(true);
				runner.frame.requestFocus();
				
				log.info("LED Layout application started successfully!");
				log.info("Use the dropdown to switch animations. Press ESC to exit");
				
			} catch (HeadlessException he) {
				log.error("HeadlessException - GUI not available. Cannot launch application.");
				System.exit(1);
			} catch (Exception e) {
				log.error("Failed to start layout application: {}", e.getMessage(), e);
				System.exit(1);
			}
		});
		
		// Keep the main thread alive so the GUI can run
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			log.info("Application interrupted, shutting down...");
		}
	}
}
