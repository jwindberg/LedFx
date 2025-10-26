package com.marsraver.LedFx;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Example LED sketch demonstrating the Processing-like framework with unified LED integration.
 * Creates a bouncing ball animation that works with any number of LED grids.
 */
public class LedExampleSketch implements LedSketch {
    
    // Single ball that moves across all grids
    private int ballX, ballY;
    private int ballSize = 15; // Reduced from 30 to half size for less drawing work
    private int velocityX = 15, velocityY = 15; // Increased from 9 for faster movement
    private Color ballColor;
    
    private LedGrid ledGrid;
    private long lastTime;
    private float hue = 0;
    private int windowWidth, windowHeight;
    
    @Override
    public void init(int width, int height) {
        // This method is required by the Sketch interface but not used in LED sketches
        // The LED-specific init method will be called instead
    }
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Start the ball in the center
        ballX = width / 2;
        ballY = height / 2;
        
        // Set initial color
        ballColor = new Color(100, 150, 255);
        lastTime = System.currentTimeMillis();
        
        System.out.println("LED Sketch initialized with size: " + width + "x" + height);
        System.out.println("LED Grids: " + ledGrid.getGridCount());
        for (int i = 0; i < ledGrid.getGridCount(); i++) {
            System.out.println("  Grid " + (i + 1) + ": " + ledGrid.getGridSize() + "x" + ledGrid.getGridSize() + 
                             " -> " + ledGrid.getController(i).getDeviceIp());
        }
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height) {
        // This method is required by the Sketch interface but not used in LED sketches
        // The LED-specific draw method will be called instead
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update and draw the single ball
        updateAndDrawBall(g);
        
        // Update LED colors based on ball position
        updateLedColors();
        
        // Draw info text (smaller font for less drawing work)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12)); // Reduced from 16 to 12
        g.drawString("LED Grids: " + ledGrid.getGridCount() + " - One Ball - Press ESC to exit", 10, 20);
    }
    
    /**
     * Updates and draws the single ball.
     */
    private void updateAndDrawBall(Graphics2D g) {
        // Update ball position
        ballX += velocityX;
        ballY += velocityY;
        
        // Bounce off window edges
        if (ballX <= ballSize/2 || ballX >= windowWidth - ballSize/2) {
            velocityX = -velocityX;
        }
        if (ballY <= ballSize/2 || ballY >= windowHeight - ballSize/2) {
            velocityY = -velocityY;
        }
        
        // Keep ball within bounds
        ballX = Math.max(ballSize/2, Math.min(windowWidth - ballSize/2, ballX));
        ballY = Math.max(ballSize/2, Math.min(windowHeight - ballSize/2, ballY));
        
        // Animate color
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;
        
        hue += timeDelta * 120; // Rotate hue over time (doubled speed for more dynamic colors)
        if (hue > 360) hue -= 360;
        ballColor = Color.getHSBColor(hue / 360.0f, 0.8f, 1.0f);
        
        // Draw the ball
        g.setColor(ballColor);
        g.fill(new Ellipse2D.Double(ballX - ballSize/2, ballY - ballSize/2, ballSize, ballSize));
        
        // Add a subtle glow effect (reduced size)
        g.setColor(new Color(ballColor.getRed(), ballColor.getGreen(), ballColor.getBlue(), 50));
        g.fill(new Ellipse2D.Double(ballX - ballSize/2 - 2, ballY - ballSize/2 - 2, 
                                   ballSize + 4, ballSize + 4));
    }
    
    /**
     * Updates the LED grid colors based on the current ball position.
     */
    private void updateLedColors() {
        // Clear all LEDs first
        ledGrid.clearAllLeds();
        
        // Map the single ball to the appropriate LED grid
        int[] mapping = ledGrid.mapWindowToLed(ballX, ballY);
        if (mapping != null) {
            int gridIndex = mapping[0];
            int ledX = mapping[1];
            int ledY = mapping[2];
            
            // Set the LED at the ball position
            ledGrid.setLedColor(gridIndex, ledX, ledY, ballColor);
            
            // Add glow effect
            addGlowEffect(gridIndex, ledX, ledY, ballColor);
        }
    }
    
    /**
     * Adds a glow effect around a ball position.
     */
    private void addGlowEffect(int gridIndex, int ledX, int ledY, Color ballColor) {
        int glowRadius = 1; // Reduced from 2 to minimize processing overhead
        for (int dy = -glowRadius; dy <= glowRadius; dy++) {
            for (int dx = -glowRadius; dx <= glowRadius; dx++) {
                int glowX = ledX + dx;
                int glowY = ledY + dy;
                
                if (glowX >= 0 && glowX < ledGrid.getGridSize() && 
                    glowY >= 0 && glowY < ledGrid.getGridSize()) {
                    
                    // Calculate distance for glow intensity
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= glowRadius && distance > 0) {
                        float intensity = 1.0f - (float)(distance / glowRadius);
                        Color glowColor = new Color(
                            (int)(ballColor.getRed() * intensity),
                            (int)(ballColor.getGreen() * intensity),
                            (int)(ballColor.getBlue() * intensity)
                        );
                        
                        ledGrid.setLedColor(gridIndex, glowX, glowY, glowColor);
                    }
                }
            }
        }
    }
}
