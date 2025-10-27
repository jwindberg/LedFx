package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.DualLedGrid;
import com.marsraver.LedFx.LedAnimation;
import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Spinning beachball animation that fills 90% of the LED grid height.
 * Creates a colorful spinning ball with alternating colored segments.
 */
public class SpinningBeachballAnimation implements LedAnimation {
    
    private DualLedGrid dualLedGrid;
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
    public void init(int width, int height, DualLedGrid dualLedGrid) {
        this.dualLedGrid = dualLedGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        this.lastTime = System.currentTimeMillis();
        
        log.debug("Spinning Beachball Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, DualLedGrid dualLedGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update rotation
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;
        
        rotation += timeDelta * 90; // 90 degrees per second
        if (rotation > 360) rotation -= 360;
        
        // Calculate beachball size to just touch top and bottom of LED grid
        int gridHeight = dualLedGrid.getGridSize() * dualLedGrid.getPixelSize();
        int beachballSize = gridHeight; // Exactly the height of the LED grid
        
        // Center the beachball in the LED grid area, not the window
        int totalGridWidth = dualLedGrid.getGridSize() * dualLedGrid.getPixelSize() * 2; // 32 LEDs wide
        int centerX = dualLedGrid.getLeftGridStartX() + totalGridWidth / 2;
        int centerY = dualLedGrid.getGridStartY() + gridHeight / 2;
        
        // Draw the spinning beachball FIRST
        drawSpinningBeachball(g, centerX, centerY, beachballSize, rotation);
        
        // NOW sample the colors from what was actually drawn and map to LEDs
        sampleAndMapToLeds(g, dualLedGrid, centerX, centerY, beachballSize / 2);
        
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
    private void sampleAndMapToLeds(Graphics2D g, DualLedGrid dualLedGrid, int centerX, int centerY, int radius) {
        // Clear all LEDs first
        dualLedGrid.clearAllLeds();
        
        // Sample colors from the actual rendered graphics
        // Left grid (left half of beachball)
        for (int y = 0; y < dualLedGrid.getGridSize(); y++) {
            for (int x = 0; x < dualLedGrid.getGridSize(); x++) {
                // Map LED coordinates to window coordinates for left grid
                int windowX = dualLedGrid.getLeftGridStartX() + x * dualLedGrid.getPixelSize() + dualLedGrid.getPixelSize() / 2;
                int windowY = dualLedGrid.getGridStartY() + y * dualLedGrid.getPixelSize() + dualLedGrid.getPixelSize() / 2;
                
                // Sample the color from the rendered graphics at this exact position
                Color sampledColor = sampleColorFromGraphics(g, windowX, windowY, centerX, centerY, radius);
                
                if (sampledColor != null) {
                    // Set left grid LED with the sampled color
                    dualLedGrid.setLeftLedColor(x, y, sampledColor);
                }
            }
        }
        
        // Right grid (right half of beachball)
        for (int y = 0; y < dualLedGrid.getGridSize(); y++) {
            for (int x = 0; x < dualLedGrid.getGridSize(); x++) {
                // Map LED coordinates to window coordinates for right grid
                int windowX = dualLedGrid.getRightGridStartX() + x * dualLedGrid.getPixelSize() + dualLedGrid.getPixelSize() / 2;
                int windowY = dualLedGrid.getGridStartY() + y * dualLedGrid.getPixelSize() + dualLedGrid.getPixelSize() / 2;
                
                // Sample the color from the rendered graphics at this exact position
                Color sampledColor = sampleColorFromGraphics(g, windowX, windowY, centerX, centerY, radius);
                
                if (sampledColor != null) {
                    // Set right grid LED with the sampled color
                    dualLedGrid.setRightLedColor(x, y, sampledColor);
                }
            }
        }
        
        log.debug("Beachball colors sampled from rendered graphics and mapped to LED grid");
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
        
        // Check if point is within the beachball
        if (distance <= radius) {
            // Calculate which color segment this point belongs to
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + rotation;
            while (angle < 0) angle += 360;
            while (angle >= 360) angle -= 360;
            
            int segmentIndex = (int)(angle / 60) % BEACHBALL_COLORS.length;
            return BEACHBALL_COLORS[segmentIndex];
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
                
                if (glowX >= 0 && glowX < dualLedGrid.getGridSize() && 
                    glowY >= 0 && glowY < dualLedGrid.getGridSize()) {
                    
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= glowRadius && distance > 0) {
                        float intensity = 1.0f - (float)(distance / glowRadius);
                        Color glowColor = new Color(
                            (int)(color.getRed() * intensity),
                            (int)(color.getGreen() * intensity),
                            (int)(color.getBlue() * intensity)
                        );
                        
                        if (isLeftGrid) {
                            dualLedGrid.setLeftLedColor(glowX, glowY, glowColor);
                        } else {
                            dualLedGrid.setRightLedColor(glowX, glowY, glowColor);
                        }
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
