package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Star Trek warp speed starfield animation.
 * Stars move from the center toward the edges at increasing speeds.
 */
public class StarfieldAnimation implements LedAnimation {

    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private List<Star> stars;
    private Random random;
    
    private class Star {
        float x, y;  // Normalized position (-1 to 1)
        float speed;
        Color color;
        int size;
        
        Star(float x, float y, float speed, Color color) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.color = color;
            this.size = random.nextFloat() < 0.2f ? 2 : 1; // 20% chance for larger star
        }
        
        void update(float centerX, float centerY, float maxSpeed) {
            // Move toward edges
            float dx = x - centerX;
            float dy = y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (dist > 0.01f) {
                x += dx / dist * speed * maxSpeed;
                y += dy / dist * speed * maxSpeed;
            }
            
            // Reset if outside bounds
            if (Math.abs(x) > 1.5f || Math.abs(y) > 1.5f) {
                // Start from center with random direction
                float angle = random.nextFloat() * 2 * (float) Math.PI;
                float radius = 0.1f;
                x = centerX + (float) Math.cos(angle) * radius;
                y = centerY + (float) Math.sin(angle) * radius;
                speed = 0.02f + random.nextFloat() * 0.03f;
            }
        }
    }

    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.ledGrid = ledGrid;
        this.random = new Random();
        this.stars = new ArrayList<>();
        
        // Create 200 stars starting from random positions
        float centerX = 0;
        float centerY = 0;
        for (int i = 0; i < 200; i++) {
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float radius = 0.05f + random.nextFloat() * 1.0f;
            float x = (float) (centerX + Math.cos(angle) * radius);
            float y = (float) (centerY + Math.sin(angle) * radius);
            float speed = 0.02f + random.nextFloat() * 0.03f;
            Color color = random.nextFloat() < 0.1f ? Color.CYAN : Color.WHITE; // 10% cyan stars
            stars.add(new Star(x, y, speed, color));
        }
    }

    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        float centerX = 0;
        float centerY = 0;
        float maxSpeed = 1.0f;
        
        // Update and draw stars
        for (Star star : stars) {
            star.update(centerX, centerY, maxSpeed);
            
            // Convert normalized coordinates to screen coordinates
            int screenX = (int) ((star.x + 1) * width / 2);
            int screenY = (int) ((star.y + 1) * height / 2);
            
            // Calculate brightness based on distance from center
            float dx = star.x - centerX;
            float dy = star.y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float brightness = Math.min(1.0f, 0.3f + dist * 0.7f);
            
            Color starColor = new Color(
                (int) (star.color.getRed() * brightness),
                (int) (star.color.getGreen() * brightness),
                (int) (star.color.getBlue() * brightness)
            );
            
            g.setColor(starColor);
            
            if (star.size > 1) {
                g.fillOval(screenX - 1, screenY - 1, 3, 3);
            } else {
                g.fillRect(screenX, screenY, 1, 1);
            }
        }
        
        // Map to LEDs
        mapToLeds(g, ledGrid);
    }
    
    private void mapToLeds(Graphics2D g, LedGrid ledGrid) {
        int gridCount = ledGrid.getGridCount();
        
        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            int gridSize = gridConfig.getGridSize();
            int gridX = gridConfig.getX();
            int gridY = gridConfig.getY();
            
            // Sample each LED position
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    // Get pixel color from rendered image
                    int pixelX = gridX + x;
                    int pixelY = gridY + y;
                    
                    if (pixelX >= 0 && pixelX < windowWidth && pixelY >= 0 && pixelY < windowHeight) {
                        // Sample from the graphics context
                        // For a simple approach, we'll recreate the star rendering logic here
                        Color ledColor = sampleStarColor(pixelX, pixelY);
                        
                        if (ledColor != null && ledColor.getRGB() != Color.BLACK.getRGB()) {
                            ledGrid.setLedColor(gridIndex, y, x, ledColor);
                        }
                    }
                }
            }
        }
    }
    
    private Color sampleStarColor(int screenX, int screenY) {
        // Convert screen coordinates to normalized coordinates
        float normalizedX = (screenX / (float) windowWidth) * 2 - 1;
        float normalizedY = (screenY / (float) windowHeight) * 2 - 1;
        
        // Check if any star is at this position
        for (Star star : stars) {
            float dx = normalizedX - star.x;
            float dy = normalizedY - star.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            
            // Check if this pixel is within the star's size
            float starRadius = star.size * 0.01f;
            if (dist < starRadius) {
                float centerX = 0;
                float centerY = 0;
                float starDx = star.x - centerX;
                float starDy = star.y - centerY;
                float starDist = (float) Math.sqrt(starDx * starDx + starDy * starDy);
                float brightness = Math.min(1.0f, 0.3f + starDist * 0.7f);
                
                return new Color(
                    (int) (star.color.getRed() * brightness),
                    (int) (star.color.getGreen() * brightness),
                    (int) (star.color.getBlue() * brightness)
                );
            }
        }
        
        return Color.BLACK;
    }

    @Override
    public String getName() {
        return "Starfield";
    }

    @Override
    public String getDescription() {
        return "Star Trek warp speed starfield effect";
    }

    @Override
    public void stop() {
        // No cleanup needed
    }
}
