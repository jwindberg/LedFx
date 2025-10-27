package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * Bouncing ball animation for the LED framework.
 * Creates a single ball that bounces around the window with color cycling.
 */
@Log4j2
public class BouncingBallAnimation implements LedAnimation {
    
    // Ball properties
    private int ballX, ballY;
    private int ballSize = 25; // Increased size for better visibility
    private int velocityX = 5, velocityY = 5; // Slower movement for better visualization
    private Color ballColor;
    
    private LedGrid ledGrid;
    private long lastTime;
    private float hue = 0;
    private int windowWidth, windowHeight;
    private Random random;
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Start the ball in the center of the window
        ballX = width / 2;
        ballY = height / 2;
        
        // Set initial color
        ballColor = new Color(100, 150, 255);
        lastTime = System.currentTimeMillis();
        
        // Initialize random number generator for path variation
        random = new Random();
        
        log.debug("Bouncing Ball Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
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
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Bouncing Ball - Press ESC to exit", 10, 20);
    }
    
    /**
     * Updates and draws the single ball.
     */
    private void updateAndDrawBall(Graphics2D g) {
        // Update ball position
        ballX += velocityX;
        ballY += velocityY;
        
        boolean hitX = ballX <= ballSize/2 || ballX >= windowWidth - ballSize/2;
        boolean hitY = ballY <= ballSize/2 || ballY >= windowHeight - ballSize/2;
        
        // Handle corner bounces properly
        if (hitX && hitY) {
            // Hit corner - reverse both directions
            velocityX = -velocityX;
            velocityY = -velocityY;
        } else if (hitX) {
            // Hit vertical wall
            velocityX = -velocityX;
        } else if (hitY) {
            // Hit horizontal wall
            velocityY = -velocityY;
        }
        
        // Add random noise to velocity if we hit a wall
        if (hitX || hitY) {
            // Add random noise to velocity (-2 to +2)
            velocityX += random.nextInt(5) - 2;
            velocityY += random.nextInt(5) - 2;
            // Ensure minimum speed to prevent getting stuck
            if (velocityX == 0) velocityX = random.nextBoolean() ? 3 : -3;
            if (velocityY == 0) velocityY = random.nextBoolean() ? 3 : -3;
            // Keep velocity within reasonable bounds
            velocityX = Math.max(-10, Math.min(10, velocityX));
            velocityY = Math.max(-10, Math.min(10, velocityY));
        }
        
        // Keep ball within window bounds
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
    
    // Cache commonly used values
    private int gridSize;
    private int pixelSize;
    
    /**
     * Updates the LED grid colors based on the current ball position.
     */
    private void updateLedColors() {
        // Cache values to avoid repeated method calls
        gridSize = ledGrid.getGridSize();
        pixelSize = ledGrid.getPixelSize();
        int gridCount = ledGrid.getGridCount();
        
        // Clear all LEDs first
        ledGrid.clearAllLeds();
        
        // Debug: Check if ball is in window bounds
        if (ballX < 0 || ballX >= windowWidth || ballY < 0 || ballY >= windowHeight) {
            // Ball is out of bounds, don't update LEDs
            return;
        }
        
        // Pre-compute dimmed color for glow effect
        int glowR = ballColor.getRed() / 2;
        int glowG = ballColor.getGreen() / 2;
        int glowB = ballColor.getBlue() / 2;
        Color glowColor = new Color(glowR, glowG, glowB);
        
        // Find which grid contains the ball
        int targetGridIndex = -1;
        int ledX = -1;
        int ledY = -1;
        
        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            
            // Calculate ball position relative to this grid
            int ballX_relative = ballX - gridConfig.getX();
            int ballY_relative = ballY - gridConfig.getY();
            
            // Map to LED coordinates
            int gridLedX = ballX_relative / pixelSize;
            int gridLedY = ballY_relative / pixelSize;
            
            // Check if ball is within this grid
            if (gridLedX >= 0 && gridLedX < gridSize && gridLedY >= 0 && gridLedY < gridSize) {
                targetGridIndex = gridIndex;
                ledX = gridLedX;
                ledY = gridLedY;
                break; // Found the grid, stop searching
            }
        }
        
        // Only draw if we found a valid grid
        if (targetGridIndex >= 0) {
            // Set the LED at the ball position (no transformation needed)
            ledGrid.setLedColor(targetGridIndex, ledX, ledY, ballColor);
            
            // Add a small glow effect (optimized to use pre-computed color)
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    // Skip the center (already set to ball color)
                    if (dx == 0 && dy == 0) continue;
                    
                    int glowX = ledX + dx;
                    int glowY = ledY + dy;
                    
                    // Check bounds for glow effect
                    if (glowX >= 0 && glowX < gridSize && glowY >= 0 && glowY < gridSize) {
                        ledGrid.setLedColor(targetGridIndex, glowX, glowY, glowColor);
                    }
                }
            }
        }
    }
    
    /**
     * Adds a glow effect around a ball position.
     */
    private void addGlowEffect(int gridIndex, int ledX, int ledY, Color ballColor) {
        int glowRadius = 2; // Increased glow radius for better visibility
        for (int dy = -glowRadius; dy <= glowRadius; dy++) {
            for (int dx = -glowRadius; dx <= glowRadius; dx++) {
                int glowX = ledX + dx;
                int glowY = ledY + dy;
                
                if (glowX >= 0 && glowX < ledGrid.getGridSize() && 
                    glowY >= 0 && glowY < ledGrid.getGridSize()) {
                    
                    // Calculate distance for glow intensity
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= glowRadius) {
                        float intensity = 1.0f - (float)(distance / glowRadius) * 0.5f; // Reduced intensity falloff
                        Color glowColor = new Color(
                            Math.min(255, Math.max(0, (int)(ballColor.getRed() * intensity))),
                            Math.min(255, Math.max(0, (int)(ballColor.getGreen() * intensity))),
                            Math.min(255, Math.max(0, (int)(ballColor.getBlue() * intensity)))
                        );
                        
                        ledGrid.setLedColor(gridIndex, glowX, glowY, glowColor);
                    }
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "Bouncing Ball";
    }
    
    @Override
    public String getDescription() {
        return "A single ball that bounces around the window with dynamic color cycling";
    }
}
