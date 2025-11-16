package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * Perlin Oscillator animation - creates oscillating circles with Perlin noise-driven motion.
 * Based on the Processing sketch by aa_debdeb.
 */
@Log4j2
public class PerlinOscillatorAnimation implements LedAnimation {
    
    private LedGrid ledGrid;
    @SuppressWarnings("unused")
    private int windowWidth, windowHeight;
    @SuppressWarnings("unused")
    private long startTime;
    
    // Oscillator parameters
    private float noiseX, noiseY;
    private ArrayList<Oscillator> oscillators;
    private Random random;
    
    // Spacing between oscillators
    private static final int SPACING = 25;
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        this.startTime = System.currentTimeMillis();
        
        // Initialize random generator
        random = new Random();
        
        // Initialize noise offsets
        noiseX = random.nextFloat() * 100;
        noiseY = random.nextFloat() * 100;
        
        // Create oscillators in a grid
        oscillators = new ArrayList<>();
        for (int x = 0; x <= width; x += SPACING) {
            for (int y = 0; y <= height; y += SPACING) {
                oscillators.add(new Oscillator(x, y));
            }
        }
        
        log.debug("Perlin Oscillator Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
        log.debug("Oscillators created: " + oscillators.size());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update noise parameters
        noiseX += 0.01f;
        noiseY += 0.01f;
        
        // Draw all oscillators
        for (Oscillator osc : oscillators) {
            osc.display(g, noiseX, noiseY);
        }
        
        // Update LED colors
        updateLedColors();
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Perlin Oscillator - Press ESC to exit", 10, 20);
        g.drawString("Oscillators: " + oscillators.size(), 10, 35);
    }
    
    /**
     * Updates LED colors from the current frame.
     */
    private void updateLedColors() {
        // The oscillators are drawn directly, and we'll sample them
        // to update LED colors via the mapToLeds method
        mapToLeds();
    }
    
    /**
     * Maps the current graphics to LED colors.
     * This samples the rendered oscillators to set LED colors.
     */
    private void mapToLeds() {
        int numGrids = ledGrid.getGridCount();
        
        for (int gridIndex = 0; gridIndex < numGrids; gridIndex++) {
            com.marsraver.LedFx.layout.GridConfig grid = ledGrid.getGridConfig(gridIndex);
            int gridSize = grid.getGridSize();
            int pixelSize = grid.getPixelSize();
            
            // For each LED in the grid
            for (int x = 0; x < gridSize; x++) {
                for (int y = 0; y < gridSize; y++) {
                    // Calculate window coordinates for this LED
                    int windowX = grid.getX() + x * pixelSize + pixelSize / 2;
                    int windowY = grid.getY() + y * pixelSize + pixelSize / 2;
                    
                    // Sample color from oscillators at this position
                    Color sampledColor = sampleColorAt(windowX, windowY);

                    // Use standard logical LED coordinates (x = left->right, y = top->bottom)
                    // so mapping is consistent with other animations and LedGrid packing.
                    ledGrid.setLedColor(gridIndex, x, y, sampledColor);
                }
            }
        }
    }
    
    /**
     * Samples the color at a specific window coordinate by checking nearby oscillators.
     */
    private Color sampleColorAt(int windowX, int windowY) {
        // Find the closest oscillator
        Oscillator closest = null;
        float minDist = Float.MAX_VALUE;
        
        for (Oscillator osc : oscillators) {
            float dist = (float) Math.sqrt(
                (osc.x - windowX) * (osc.x - windowX) + 
                (osc.y - windowY) * (osc.y - windowY)
            );
            if (dist < minDist) {
                minDist = dist;
                closest = osc;
            }
        }
        
        if (closest != null) {
            // Check if the point is within the oscillator's current size
            float currentSize = closest.getCurrentSize(noiseX, noiseY);
            if (minDist <= currentSize / 2) {
                return closest.getCurrentColor();
            }
        }
        
        return Color.BLACK;
    }
    
    @Override
    public String getName() {
        return "Perlin Oscillator";
    }
    
    @Override
    public String getDescription() {
        return "Oscillating circles with Perlin noise-driven motion";
    }
    
    /**
     * Inner class representing a single oscillator.
     */
    private class Oscillator {
        float x, y;
        float rad;
        
        Oscillator(int x, int y) {
            this.x = x;
            this.y = y;
            this.rad = random.nextFloat() * (float) (2 * Math.PI);
        }
        
        void display(Graphics2D g, float noiseX, float noiseY) {
            // Calculate current diameter based on sine wave
            float diameter = map((float) Math.sin(rad), -1, 1, 10, 24);
            
            // Calculate color based on sine wave
            int r = (int) map((float) Math.sin(rad), -1, 1, 0, 255);
            int gVal = (int) map((float) Math.sin(rad), -1, 1, 139, 20);
            int b = (int) map((float) Math.sin(rad), -1, 1, 139, 147);
            
            // Clamp color values
            r = Math.max(0, Math.min(255, r));
            gVal = Math.max(0, Math.min(255, gVal));
            b = Math.max(0, Math.min(255, b));
            
            Color color = new Color(r, gVal, b);
            g.setColor(color);
            
            // Draw the ellipse
            Ellipse2D.Double ellipse = new Ellipse2D.Double(x - diameter / 2, y - diameter / 2, diameter, diameter);
            g.fill(ellipse);
            
            // Update radius based on Perlin noise
            float noiseValue = noise(noiseX + x * 0.05f, noiseY + y * 0.05f, 0.0f);
            rad += map(noiseValue, 0f, 1f, (float) (Math.PI / 128), (float) (Math.PI / 6));
            
            // Keep radius between 0 and 2*PI
            if (rad > 2 * Math.PI) {
                rad -= 2 * Math.PI;
            }
        }
        
        /**
         * Gets the current size of the oscillator.
         */
        float getCurrentSize(float noiseX, float noiseY) {
            return map((float) Math.sin(rad), -1, 1, 10, 24);
        }
        
        /**
         * Gets the current color of the oscillator.
         */
        Color getCurrentColor() {
            int r = (int) map((float) Math.sin(rad), -1, 1, 0, 255);
            int gVal = (int) map((float) Math.sin(rad), -1, 1, 139, 20);
            int b = (int) map((float) Math.sin(rad), -1, 1, 139, 147);
            
            r = Math.max(0, Math.min(255, r));
            gVal = Math.max(0, Math.min(255, gVal));
            b = Math.max(0, Math.min(255, b));
            
            return new Color(r, gVal, b);
        }
    }
    
    /**
     * Maps a value from one range to another (Processing map function).
     */
    private float map(float value, float start1, float stop1, float start2, float stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }
    
    private float noise(float x, float y, float z) {
        // Simple pseudo-Perlin noise using fractals
        float result = 0.0f;
        float amplitude = 1.0f;
        float frequency = 0.01f;
        
        for (int i = 0; i < 4; i++) {
            result += amplitude * smoothRandom(x * frequency, y * frequency, z * frequency);
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        
        return result * 0.5f + 0.5f; // Normalize to 0-1
    }
    
    private float smoothRandom(float x, float y, float z) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);
        
        float fx = (float) (x - ix);
        float fy = (float) (y - iy);
        float fz = (float) (z - iz);
        
        fx = fx * fx * (3 - 2 * fx); // Smoothstep
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        
        float v000 = hash(ix, iy, iz);
        float v100 = hash(ix + 1, iy, iz);
        float v010 = hash(ix, iy + 1, iz);
        float v110 = hash(ix + 1, iy + 1, iz);
        float v001 = hash(ix, iy, iz + 1);
        float v101 = hash(ix + 1, iy, iz + 1);
        float v011 = hash(ix, iy + 1, iz + 1);
        float v111 = hash(ix + 1, iy + 1, iz + 1);
        
        float v00 = lerp(v000, v100, fx);
        float v01 = lerp(v010, v110, fx);
        float v10 = lerp(v001, v101, fx);
        float v11 = lerp(v011, v111, fx);
        
        float v0 = lerp(v00, v01, fy);
        float v1 = lerp(v10, v11, fy);
        
        return lerp(v0, v1, fz);
    }
    
    private float hash(int x, int y, int z) {
        int n = x + y * 57 + z * 131;
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647.0f;
    }
    
    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
}
