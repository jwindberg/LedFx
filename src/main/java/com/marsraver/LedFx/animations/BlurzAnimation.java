package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Random;

/**
 * Blurz animation based on WLED's Blurz effect.
 * Creates a blurry, color-mixed effect with organic movement.
 * Features color mixing and soft blur effects.
 */
@Log4j2
public class BlurzAnimation implements LedAnimation {
    
    private LedGrid ledGrid;
    private long lastTime;
    private float time = 0;
    private Random random;
    
    // Blur properties
    private BufferedImage backBuffer;
    private BufferedImage blurBuffer;
    private Graphics2D backGraphics;
    
    // Color mixing
    private float hue = 0.0f;
    private float hueSpeed = 20.0f;
    private int pulseCount = 0;
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.lastTime = System.currentTimeMillis();
        this.random = new Random();
        
        // Create buffers for blur effect
        this.backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.blurBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.backGraphics = backBuffer.createGraphics();
        
        // Enable anti-aliasing
        backGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        backGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        log.debug("Blurz Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Update time
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;
        time += deltaTime;
        
        // Update color hue
        hue += deltaTime * hueSpeed;
        if (hue > 360) hue -= 360;
        
        // Draw to back buffer first
        drawToBackBuffer(width, height);
        
        // Apply blur effect
        applyBlur(width, height);
        
        // Draw blurred result to main graphics
        g.drawImage(blurBuffer, 0, 0, null);
        
        // Add fresh colored pulses
        addColorPulses(g, width, height);
        
        // Map to LEDs
        mapToLeds(g, width, height);
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Blurz - Press ESC to exit", 10, 20);
    }
    
    /**
     * Draws the base pattern to the back buffer.
     */
    private void drawToBackBuffer(int width, int height) {
        // Semi-transparent fade for trail effect
        backGraphics.setColor(new Color(0, 0, 0, 200));
        backGraphics.fillRect(0, 0, width, height);
        
        // Draw blurred motion trails
        for (int i = 0; i < 5; i++) {
            float phase = (time * (0.5f + i * 0.2f) + i * 60.0f) % (float) (Math.PI * 2);
            float x = width / 2.0f + (float) (Math.cos(phase) * (width / 3.0f));
            float y = height / 2.0f + (float) (Math.sin(phase) * (height / 3.0f));
            
            float hueValue = ((hue + i * 72.0f) % 360.0f) / 360.0f;
            Color trailColor = Color.getHSBColor(hueValue, 0.8f, 0.7f);
            
            backGraphics.setColor(trailColor);
            int size = 40 + i * 10;
            backGraphics.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }
    }
    
    /**
     * Applies blur effect using convolution.
     */
    private void applyBlur(int width, int height) {
        // Create blur kernel (9x9 Gaussian-like blur)
        float[] matrix = {
            0.01f, 0.02f, 0.03f, 0.02f, 0.01f,
            0.02f, 0.04f, 0.06f, 0.04f, 0.02f,
            0.03f, 0.06f, 0.10f, 0.06f, 0.03f,
            0.02f, 0.04f, 0.06f, 0.04f, 0.02f,
            0.01f, 0.02f, 0.03f, 0.02f, 0.01f
        };
        
        Kernel kernel = new Kernel(5, 5, matrix);
        ConvolveOp blurOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        blurOp.filter(backBuffer, blurBuffer);
    }
    
    /**
     * Adds fresh color pulses for brightness.
     */
    private void addColorPulses(Graphics2D g, int width, int height) {
        pulseCount++;
        
        // Add pulses periodically
        if (pulseCount % 60 == 0) {
            float hueValue = (hue % 360.0f) / 360.0f;
            Color pulseColor = Color.getHSBColor(hueValue, 1.0f, 1.0f);
            
            // Random position
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            
            // Draw bright pulse
            g.setColor(new Color(pulseColor.getRed(), pulseColor.getGreen(), 
                                  pulseColor.getBlue(), 200));
            g.fillOval(x - 20, y - 20, 40, 40);
            
            // Outer glow
            g.setColor(new Color(pulseColor.getRed(), pulseColor.getGreen(), 
                                pulseColor.getBlue(), 100));
            g.fillOval(x - 30, y - 30, 60, 60);
        }
    }
    
    /**
     * Maps the blurz effect to LEDs.
     */
    private void mapToLeds(Graphics2D g, int width, int height) {
        int gridSize = ledGrid.getGridSize();
        int pixelSize = ledGrid.getPixelSize();
        int gridCount = ledGrid.getGridCount();
        
        // Clear all grids
        for (int i = 0; i < gridCount; i++) {
            ledGrid.clearGrid(i);
        }
        
        // Sample colors from the blurred image
        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    // Calculate window coordinates for this LED
                    int windowX = gridConfig.getX() + x * pixelSize + pixelSize / 2;
                    int windowY = gridConfig.getY() + y * pixelSize + pixelSize / 2;
                    
                    // Sample color from blurred buffer
                    if (windowX >= 0 && windowX < width && windowY >= 0 && windowY < height) {
                        int rgb = blurBuffer.getRGB(windowX, windowY);
                        Color ledColor = new Color(rgb);
                        
                        // Only set if not black
                        if (ledColor.getRed() > 5 || ledColor.getGreen() > 5 || ledColor.getBlue() > 5) {
                            // Standard logical coordinates: x = left->right, y = top->bottom
                            ledGrid.setLedColor(gridIndex, x, y, ledColor);
                        }
                    }
                }
            }
        }
        
        // Send to devices
        ledGrid.sendToDevices();
    }
    
    @Override
    public String getName() {
        return "Blurz";
    }
    
    @Override
    public String getDescription() {
        return "Blurry color-mixed effect with organic movement (WLED-style)";
    }
    
    @Override
    public void stop() {
        if (backGraphics != null) {
            backGraphics.dispose();
        }
    }
}



