package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

/**
 * Spinning beachball animation with rainbow colored segments.
 */
@Log4j2
public class SpinningBeachballAnimation implements LedAnimation {
    
    private LedGrid ledGrid;
    private long lastTime;
    private float rotation = 0;
    @SuppressWarnings("unused")
    private int windowWidth, windowHeight;
    
    // Beachball colors (six bright, vibrant colors)
    private static final Color[] BEACHBALL_COLORS = {
        new Color(255, 0, 0),     // Red
        new Color(255, 128, 0),   // Orange
        new Color(255, 255, 0),   // Yellow
        new Color(0, 255, 0),     // Green
        new Color(0, 0, 255),     // Blue
        new Color(128, 0, 255)    // Purple
    };
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        this.lastTime = System.currentTimeMillis();
        
        log.debug("Spinning Beachball Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update rotation for spinning effect
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;
        rotation -= timeDelta * 90; // 90 degrees per second
        if (rotation < 0) rotation += 360;
        
        // Calculate beachball size
        int beachballSize = 480;

        // Center the beachball in the LED grid area
        // Grid01 and Grid02 start at y=45, Grid03 and Grid04 end at y=525 (285+240)
        // So the visual center is at (45 + 525) / 2 = 285
        int centerX = width / 2;
        int centerY = 285;
        
        // Draw the spinning beachball
        drawBeachball(g, centerX, centerY, beachballSize, rotation);
        
        // Map to LEDs by sampling the rendered canvas
        mapToLeds(g, centerX, centerY, beachballSize / 2, width, height);
    }
    
    /**
     * Draws the spinning beachball with colored segments.
     */
    private void drawBeachball(Graphics2D g, int centerX, int centerY, int size, float rotation) {
        int radius = size / 2;
        
        // Enable anti-aliasing for smooth edges
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw each colored segment (60 degrees each for 6 segments)
        int segmentAngle = 360 / BEACHBALL_COLORS.length;
        for (int i = 0; i < BEACHBALL_COLORS.length; i++) {
            g.setColor(BEACHBALL_COLORS[i]);
            
            // Calculate segment angles (60 degrees each for 6 segments)
            float startAngle = (i * segmentAngle) + rotation;
            float arcAngle = segmentAngle;
            
            // Draw the arc segment
            Arc2D arc = new Arc2D.Double(
                centerX - radius, centerY - radius, size, size,
                startAngle, arcAngle, Arc2D.PIE
            );
            g.fill(arc);
        }
    }
    
    /**
     * Maps the beachball to LEDs by sampling the rendered Graphics2D output.
     */
    private void mapToLeds(Graphics2D g, int centerX, int centerY, int radius, int width, int height) {
        int gridSize = ledGrid.getGridSize();
        int pixelSize = ledGrid.getPixelSize();
        int gridCount = ledGrid.getGridCount();
        
        // Clear all grids
        for (int i = 0; i < gridCount; i++) {
            ledGrid.clearGrid(i);
        }
        
        // Create a BufferedImage to sample from the current Graphics2D output
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D canvasG = canvas.createGraphics();
        
        // Draw the beachball to the BufferedImage (same as what's shown on screen)
        canvasG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvasG.setColor(Color.BLACK);
        canvasG.fillRect(0, 0, width, height);
        drawBeachball(canvasG, centerX, centerY, radius * 2, rotation);
        canvasG.dispose();
        
        // For each grid, check each LED position.
        // We now treat (x,y) in the same way as TestAnimation and packing:
        // x = 0 is left, y = 0 is top.
        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    // Map LED coordinates to window coordinates
                    int windowX = gridConfig.getX() + x * pixelSize + pixelSize / 2;
                    int windowY = gridConfig.getY() + y * pixelSize + pixelSize / 2;
                    
                    // Calculate distance from beachball center
                    int dx = windowX - centerX;
                    int dy = windowY - centerY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    // Check if LED is within the beachball
                    if (distance <= radius) {
                        // Sample the color from the rendered canvas at this position
                        int rgb = canvas.getRGB(windowX, windowY);
                        Color color = new Color(rgb);
                        
                        // Display all colors without filter
                        ledGrid.setLedColor(gridIndex, x, y, color);
                    }
                }
            }
        }
        
        // Send to devices
        ledGrid.sendToDevices();
    }
    
    @Override
    public String getName() {
        return "Spinning Beachball";
    }
    
    @Override
    public String getDescription() {
        return "A colorful spinning beachball with 6 colored segments";
    }
}



