package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Black hole animation with accretion disk and gravitational lensing effects.
 * Particles spiral toward the event horizon, creating a mesmerizing cosmic effect.
 * Based on the WLED black hole effect pattern.
 */
@Log4j2
public class BlackHoleAnimation implements LedAnimation {
    
    private LedGrid ledGrid;
    private long lastTime;
    private float time = 0;
    private Random random;
    
    // Black hole properties
    private int centerX, centerY;
    private float eventHorizonRadius = 30.0f;
    private float outerRadius = 200.0f;
    
    // Particle system for accretion disk
    private List<Particle> particles;
    private static final int PARTICLE_COUNT = 150;
    
    private class Particle {
        float x, y;           // Position relative to center
        float angle;          // Angular position (0-2π)
        float radius;         // Distance from center
        float angularVel;     // Angular velocity
        float radialVel;      // Radial velocity (inward)
        Color color;
        float brightness;
        
        Particle(float angle, float radius) {
            this.angle = angle;
            this.radius = radius;
            this.angularVel = (0.5f + random.nextFloat() * 2.0f) / (radius + 30); // Faster closer to center
            this.radialVel = -0.3f - random.nextFloat() * 0.5f; // Inward motion
            this.brightness = 0.3f + random.nextFloat() * 0.7f;
            updateColor();
        }
        
        void update() {
            // Angular motion - faster near the black hole
            angle += angularVel;
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
            
            // Radial motion - spiral inward
            radius += radialVel;
            
            // Increase speed as we get closer to event horizon
            if (radius > eventHorizonRadius) {
                radialVel *= 1.01f; // Accelerate inward
                angularVel *= 1.005f; // Spin faster
            }
            
            // Calculate position
            x = (float) (Math.cos(angle) * radius);
            y = (float) (Math.sin(angle) * radius);
            
            // Update color based on distance (redshift effect)
            updateColor();
            
            // Reset particle if it crosses event horizon
            if (radius < eventHorizonRadius) {
                resetParticle();
            }
        }
        
        void updateColor() {
            // Color gradient: blue/white (hot) far away -> red/orange (cooler) near event horizon
            float normalizedDist = (radius - eventHorizonRadius) / (outerRadius - eventHorizonRadius);
            normalizedDist = Math.max(0, Math.min(1, normalizedDist));
            
            // HSV: Hue shifts from blue (240) to red (0) as we approach
            float hue = 240.0f * normalizedDist / 360.0f;
            float saturation = 0.7f + 0.3f * normalizedDist;
            float value = brightness * (0.6f + 0.4f * normalizedDist);
            
            // Convert to RGB
            color = Color.getHSBColor(hue, saturation, value);
        }
        
        void resetParticle() {
            angle = random.nextFloat() * 2 * (float) Math.PI;
            radius = outerRadius - random.nextFloat() * 50;
            angularVel = (0.5f + random.nextFloat() * 2.0f) / (radius + 30);
            radialVel = -0.3f - random.nextFloat() * 0.5f;
            brightness = 0.3f + random.nextFloat() * 0.7f;
            updateColor();
        }
    }
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.centerX = width / 2;
        this.centerY = height / 2;
        this.lastTime = System.currentTimeMillis();
        this.random = new Random();
        this.particles = new ArrayList<>();
        
        // Initialize particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float radius = eventHorizonRadius + random.nextFloat() * (outerRadius - eventHorizonRadius);
            particles.add(new Particle(angle, radius));
        }
        
        log.debug("Black Hole Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Update time
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;
        time += deltaTime;
        
        // Update and draw particles
        for (Particle p : particles) {
            p.update();
            
            // Draw particle with glow effect
            int screenX = centerX + (int) p.x;
            int screenY = centerY + (int) p.y;
            
            // Calculate size based on distance (gravitational lensing)
            float lensingFactor = 1.0f + (1.0f / (p.radius / eventHorizonRadius + 0.1f)) * 0.1f;
            int size = (int) (9 * lensingFactor); // 3x bigger particles
            
            // Draw glow
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 50));
            g.fill(new Ellipse2D.Double(screenX - size, screenY - size, size * 2, size * 2));
            
            // Draw core
            g.setColor(p.color);
            g.fill(new Ellipse2D.Double(screenX - size/2, screenY - size/2, size, size));
        }
        
        // Draw event horizon (black hole core)
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(
            centerX - eventHorizonRadius, 
            centerY - eventHorizonRadius, 
            eventHorizonRadius * 2, 
            eventHorizonRadius * 2
        ));
        
        // Draw accretion disk rings
        drawAccretionRings(g);
        
        // Map to LEDs
        mapToLeds(g);
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Black Hole - Press ESC to exit", 10, 20);
    }
    
    /**
     * Draws glowing accretion disk rings.
     */
    private void drawAccretionRings(Graphics2D g) {
        // Draw multiple rings at different radii
        for (int ring = 0; ring < 3; ring++) {
            float ringRadius = eventHorizonRadius + 20 + ring * 25;
            float intensity = 0.2f / (ring + 1);
            
            // Rotating ring color
            float hue = (time * 30.0f + ring * 60.0f) % 360.0f / 360.0f;
            Color ringColor = Color.getHSBColor(hue, 0.8f, intensity);
            
            // Draw ring segments
            for (int i = 0; i < 12; i++) {
                float angle = (i * 360.0f / 12.0f + time * 10.0f) * (float) Math.PI / 180.0f;
                float x1 = centerX + (float) Math.cos(angle) * (ringRadius - 2);
                float y1 = centerY + (float) Math.sin(angle) * (ringRadius - 2);
                float x2 = centerX + (float) Math.cos(angle) * (ringRadius + 2);
                float y2 = centerY + (float) Math.sin(angle) * (ringRadius + 2);
                
                g.setColor(new Color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), 100));
                g.setStroke(new BasicStroke(2.0f));
                g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }
        }
    }
    
    /**
     * Maps the black hole effect to LEDs by sampling colors at LED positions.
     */
    private void mapToLeds(Graphics2D g) {
        int gridSize = ledGrid.getGridSize();
        int pixelSize = ledGrid.getPixelSize();
        int gridCount = ledGrid.getGridCount();
        
        // Clear all grids
        for (int i = 0; i < gridCount; i++) {
            ledGrid.clearGrid(i);
        }
        
        // Sample colors at each LED position
        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    // Calculate window coordinates for this LED
                    int windowX = gridConfig.getX() + x * pixelSize + pixelSize / 2;
                    int windowY = gridConfig.getY() + y * pixelSize + pixelSize / 2;
                    
                    // Sample color at this position by checking distance from particles and center
                    Color ledColor = sampleColorAt(windowX, windowY);
                    
                    if (ledColor != null) {
                        // Standard logical coordinates: x = left->right, y = top->bottom
                        ledGrid.setLedColor(gridIndex, x, y, ledColor);
                    }
                }
            }
        }
        
        // Send to devices
        ledGrid.sendToDevices();
    }
    
    /**
     * Samples the color at a specific window coordinate.
     */
    private Color sampleColorAt(int x, int y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Check if inside event horizon (black)
        if (dist < eventHorizonRadius) {
            return null; // Black - don't set LED
        }
        
        int finalR = 0;
        int finalG = 0;
        int finalB = 0;
        
        // Check particles
        for (Particle p : particles) {
            float particleX = centerX + p.x;
            float particleY = centerY + p.y;
            float pdx = x - particleX;
            float pdy = y - particleY;
            float pdist = (float) Math.sqrt(pdx * pdx + pdy * pdy);
            
            if (pdist < 24) { // Particle influence radius (3x bigger to match particle size)
                float weight = 1.0f / (pdist + 1.0f) * p.brightness;
                finalR += (int) (p.color.getRed() * weight);
                finalG += (int) (p.color.getGreen() * weight);
                finalB += (int) (p.color.getBlue() * weight);
            }
        }
        
        Color finalColor = new Color(
            Math.min(255, finalR),
            Math.min(255, finalG),
            Math.min(255, finalB)
        );
        
        // Check accretion rings
        for (int ring = 0; ring < 3; ring++) {
            float ringRadius = eventHorizonRadius + 20 + ring * 25;
            float ringDist = Math.abs(dist - ringRadius);
            
            if (ringDist < 5) {
                float intensity = (1.0f - ringDist / 5.0f) * 0.3f / (ring + 1);
                float hue = (time * 30.0f + ring * 60.0f) % 360.0f / 360.0f;
                Color ringColor = Color.getHSBColor(hue, 0.8f, intensity);
                
                finalR += (int) (ringColor.getRed() * intensity);
                finalG += (int) (ringColor.getGreen() * intensity);
                finalB += (int) (ringColor.getBlue() * intensity);
                
                finalColor = new Color(
                    Math.min(255, finalR),
                    Math.min(255, finalG),
                    Math.min(255, finalB)
                );
            }
        }
        
        // Return color only if there's visible light
        if (finalColor.getRed() > 5 || finalColor.getGreen() > 5 || finalColor.getBlue() > 5) {
            return finalColor;
        }
        
        return null;
    }
    
    @Override
    public String getName() {
        return "Black Hole";
    }
    
    @Override
    public String getDescription() {
        return "Cosmic black hole with accretion disk and gravitational effects";
    }
}

