package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Spinning beachball animation that fills 90% of the LED grid height.
 * Creates a colorful spinning ball with alternating colored segments.
 */
public class SpinningBeachballAnimation implements LedAnimation {
    
    private LedGrid ledGrid;
    private long lastTime;
    private float rotation = 0;
    private int windowWidth, windowHeight;
    
    // Beachball colors (bright, vibrant colors)
    private static final Color[] BEACHBALL_COLORS = {
        new Color(255, 0, 0),     // Red
        new Color(255, 165, 0),   // Orange
        new Color(255, 255, 0),   // Yellow
        new Color(0, 255, 0),     // Green
        new Color(0, 0, 255),     // Blue
        new Color(128, 0, 128)    // Purple
    };
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        this.lastTime = System.currentTimeMillis();
        
        System.out.println("Spinning Beachball Animation initialized");
        System.out.println("Animation: " + getName());
        System.out.println("Description: " + getDescription());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        System.out.println("DEBUG: SpinningBeachballAnimation.draw() called - size: " + width + "x" + height);
        
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update rotation for spinning effect
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

            rotation += timeDelta * 90; // 90 degrees per second (correct direction)
        if (rotation > 360) rotation -= 360;
        
        // Calculate beachball size - as tall as the LED grid
        int beachballSize = 240; // Match LED grid height - never change this again

        // Center the beachball in the LED grid area (not the window)
        // The grids are at (10,80) and (250,80), each 240x240
        // So the total LED area is from (10,80) to (490,320)
        int ledAreaCenterX = (10 + 490) / 2; // 250 (middle of LED area)
        int ledAreaCenterY = (80 + 320) / 2; // 200 (middle of LED area)
        
        int centerX = ledAreaCenterX;
        int centerY = ledAreaCenterY; // Back to original position
        
        // Draw the spinning beachball FIRST
        drawSpinningBeachball(g, centerX, centerY, beachballSize, rotation);
        
        // NOW sample the colors from what was actually drawn and map to LEDs
        sampleAndMapToLeds(g, ledGrid, centerX, centerY, beachballSize / 2);
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Spinning Beachball - Press ESC to exit", 10, 20);
    }
    
    /**
     * Draws the spinning beachball with colored segments.
     */
    private void drawSpinningBeachball(Graphics2D g, int centerX, int centerY, int size, float rotation) {
        int radius = size / 2;
        System.out.println("DEBUG: drawSpinningBeachball - center=(" + centerX + "," + centerY + ") radius=" + radius + " rotation=" + rotation);
        
        // Enable anti-aliasing for smooth edges
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw each colored segment
        for (int i = 0; i < BEACHBALL_COLORS.length; i++) {
            g.setColor(BEACHBALL_COLORS[i]);
            
            // Calculate segment angles (60 degrees each for 6 segments)
            float startAngle = (i * 60) + rotation;
            float arcAngle = 60;
            
            // Draw the arc segment
            Arc2D arc = new Arc2D.Double(
                centerX - radius, centerY - radius, size, size,
                startAngle, arcAngle, Arc2D.PIE
            );
            g.fill(arc);
        }
        
        // Draw a subtle outline
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(2));
        g.drawOval(centerX - radius, centerY - radius, size, size);
    }
    
    /**
     * Samples colors from the rendered graphics and maps them to LEDs.
     * This ensures perfect synchronization between what's drawn and what's sent to LEDs.
     */
    private void sampleAndMapToLeds(Graphics2D g, LedGrid ledGrid, int centerX, int centerY, int radius) {
        // Directly calculate beachball colors for each LED position
        for (int gridIndex = 0; gridIndex < ledGrid.getGridCount(); gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            for (int y = 0; y < ledGrid.getGridSize(); y++) {
                for (int x = 0; x < ledGrid.getGridSize(); x++) {
                    // Transform LED coordinates to match physical LED arrangement
                    // For 90-degree clockwise rotation only: (x,y) -> (y, x)
                    int transformedX = y;
                    int transformedY = x;
                    
                    // Map transformed LED coordinates to window coordinates for this grid
                    int windowX = gridConfig.getX() + transformedX * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    int windowY = gridConfig.getY() + transformedY * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    
                    // Calculate distance from beachball center
                    int dx = windowX - centerX;
                    int dy = windowY - centerY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    // Check if LED is within the beachball
                    if (distance <= radius) {
                        // Calculate which color segment this LED belongs to
                        // Try without any angle adjustment first
                        double angle = Math.toDegrees(Math.atan2(dy, dx)) + rotation;
                        while (angle < 0) angle += 360;
                        while (angle >= 360) angle -= 360;
                        
                        int segmentIndex = (int)(angle / 60) % BEACHBALL_COLORS.length;
                        Color color = BEACHBALL_COLORS[segmentIndex];
                        
                        // Set LED with the calculated color
                        ledGrid.setLedColor(gridIndex, x, y, color);
                    }
                }
            }
        }
    }
    
    /**
     * Samples the color from the rendered graphics at the given window coordinates.
     * This ensures the LED colors exactly match what's actually drawn on screen.
     */
    private Color sampleColorFromGraphics(Graphics2D g, int windowX, int windowY, int centerX, int centerY, int radius) {
        // Calculate distance from center
        int dx = windowX - centerX;
        int dy = windowY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Debug: Show what we're checking
        if (windowX == centerX && windowY == centerY) {
            System.out.println("Beachball center: (" + centerX + "," + centerY + ") radius: " + radius);
        }
        
        // Check if point is within the beachball
        if (distance <= radius) {
            // Calculate which color segment this point belongs to
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + rotation;
            while (angle < 0) angle += 360;
            while (angle >= 360) angle -= 360;
            
            int segmentIndex = (int)(angle / 60) % BEACHBALL_COLORS.length;
            Color color = BEACHBALL_COLORS[segmentIndex];
            
            // Debug: Show first few colors found
            if (windowX < centerX + 50 && windowY < centerY + 50) {
                System.out.println("Beachball color at (" + windowX + "," + windowY + "): R=" + color.getRed() + " G=" + color.getGreen() + " B=" + color.getBlue());
            }
            
            return color;
        }
        
        return null; // Outside the beachball
    }
    
    /**
     * Adds a glow effect around a position.
     */
    private void addGlowEffect(int ledX, int ledY, Color color, boolean isLeftGrid, int glowRadius) {
        for (int dy = -glowRadius; dy <= glowRadius; dy++) {
            for (int dx = -glowRadius; dx <= glowRadius; dx++) {
                int glowX = ledX + dx;
                int glowY = ledY + dy;
                
                if (glowX >= 0 && glowX < ledGrid.getGridSize() && 
                    glowY >= 0 && glowY < ledGrid.getGridSize()) {
                    
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= glowRadius && distance > 0) {
                        float intensity = 1.0f - (float)(distance / glowRadius);
                        Color glowColor = new Color(
                            (int)(color.getRed() * intensity),
                            (int)(color.getGreen() * intensity),
                            (int)(color.getBlue() * intensity)
                        );
                        
                        // Simplified glow effect - just set the main LED color
                        // TODO: Implement unified glow effect for all grids
                    }
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "Spinning Beachball";
    }
    
    @Override
    public String getDescription() {
        return "A colorful spinning beachball that fills 90% of the LED grid height with 6 colored segments";
    }
}
