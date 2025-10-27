package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Clouds animation that creates a cloud-like effect using Perlin noise.
 * Features color cycling and smooth, organic movement patterns.
 */
@Log4j2
public class CloudsAnimation implements LedAnimation {

    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private long startTime;
    
    // Cloud rendering parameters
    private static final int CLOUD_WIDTH = 128;
    private static final int CLOUD_HEIGHT = 128;
    private BufferedImage cloudImage;
    
    // Noise parameters
    private float noiseScale = 0.02f;  // Increased for more detailed clouds
    private float timeScale = 0.00005f; // Slower movement
    private float hueScale = 0.00005f;  // Slower color cycling
    
    // Color parameters
    private float baseHue = 0.0f;
    private float saturation = 80.0f;
    private float brightnessScale = 500.0f;
    private float brightnessOffset = 0.4f;

    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.ledGrid = ledGrid;
        this.startTime = System.currentTimeMillis();
        
        // Create cloud image buffer
        this.cloudImage = new BufferedImage(CLOUD_WIDTH, CLOUD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        
        log.debug("Clouds Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }

    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update cloud pattern
        updateCloudPattern();
        
        // Draw the cloud image scaled to window size
        g.drawImage(cloudImage, 0, 0, width, height, null);
        
        // Update LED colors
        updateLedColors();
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Clouds Animation - Press ESC to exit", 10, 20);
        g.drawString("Time: " + String.format("%.1f", (System.currentTimeMillis() - startTime) / 1000.0f) + "s", 10, 35);
    }
    
    /**
     * Updates the cloud pattern using Perlin noise.
     */
    private void updateCloudPattern() {
        long currentTime = System.currentTimeMillis();
        float time = (currentTime - startTime) * timeScale;
        
        // Calculate hue cycling
        float hue = (noise(time * hueScale, 0.0f, 0.0f) * 200.0f) % 100.0f;
        float z = time;
        float dx = time;
        
        // Generate cloud pattern
        for (int x = 0; x < CLOUD_WIDTH; x++) {
            for (int y = 0; y < CLOUD_HEIGHT; y++) {
                // Calculate noise value
                float noiseValue = noise(dx + x * noiseScale, y * noiseScale, z);
                
                // Create cloud-like threshold effect
                float cloudThreshold = 0.3f; // Adjust this to control cloud density
                float cloudValue = Math.max(0, (noiseValue - cloudThreshold) / (1.0f - cloudThreshold));
                
                // Convert to brightness with better cloud shape (darker base)
                float brightness = cloudValue * 50.0f + 5.0f; // Range 5-55 (much darker)
                brightness = Math.max(0, Math.min(100, brightness)); // Clamp to 0-100
                
                // Calculate saturation (higher for brighter areas)
                float sat = 60.0f + (cloudValue * 40.0f); // Range 60-100
                sat = Math.max(0, Math.min(100, sat)); // Clamp to 0-100
                
                // Convert HSB to RGB
                Color color = Color.getHSBColor(hue / 100.0f, sat / 100.0f, brightness / 100.0f);
                
                // Set pixel color
                cloudImage.setRGB(x, y, color.getRGB());
            }
        }
    }
    
    /**
     * Improved Perlin noise implementation for cloud effects.
     * Creates proper cloud-like patterns with multiple octaves.
     */
    private float noise(float x, float y, float z) {
        // Multiple octaves of noise for realistic cloud patterns
        float noise = 0.0f;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        
        // Add multiple octaves
        for (int i = 0; i < 4; i++) {
            noise += amplitude * smoothNoise(x * frequency, y * frequency, z * frequency);
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        
        // Normalize to 0-1 range
        return Math.max(0.0f, Math.min(1.0f, noise * 0.5f + 0.5f));
    }
    
    /**
     * Smooth noise function using interpolation.
     */
    private float smoothNoise(float x, float y, float z) {
        // Get integer and fractional parts
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        
        float fx = x - xi;
        float fy = y - yi;
        float fz = z - zi;
        
        // Smooth interpolation weights
        float u = fx * fx * (3.0f - 2.0f * fx);
        float v = fy * fy * (3.0f - 2.0f * fy);
        float w = fz * fz * (3.0f - 2.0f * fz);
        
        // Get noise values at 8 corners of cube
        float n000 = randomNoise(xi, yi, zi);
        float n001 = randomNoise(xi, yi, zi + 1);
        float n010 = randomNoise(xi, yi + 1, zi);
        float n011 = randomNoise(xi, yi + 1, zi + 1);
        float n100 = randomNoise(xi + 1, yi, zi);
        float n101 = randomNoise(xi + 1, yi, zi + 1);
        float n110 = randomNoise(xi + 1, yi + 1, zi);
        float n111 = randomNoise(xi + 1, yi + 1, zi + 1);
        
        // Trilinear interpolation
        float nx00 = lerp(n000, n100, u);
        float nx01 = lerp(n001, n101, u);
        float nx10 = lerp(n010, n110, u);
        float nx11 = lerp(n011, n111, u);
        
        float nxy0 = lerp(nx00, nx10, v);
        float nxy1 = lerp(nx01, nx11, v);
        
        return lerp(nxy0, nxy1, w);
    }
    
    /**
     * Linear interpolation helper.
     */
    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
    
    /**
     * Pseudo-random noise function.
     */
    private float randomNoise(int x, int y, int z) {
        // Simple hash function for pseudo-random values
        int n = x + y * 57 + z * 131;
        n = (n << 13) ^ n;
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }
    
    /**
     * Updates LED colors based on the current cloud pattern.
     */
    private void updateLedColors() {
        // Clear all LEDs first
        ledGrid.clearAllLeds();
        
        // Map cloud pattern to LED grids
        mapCloudsToLedGrid();
    }
    
    /**
     * Maps the cloud pattern to the LED grid using window coordinates.
     */
    private void mapCloudsToLedGrid() {
        // Map cloud pattern to all grids in the layout
        for (int gridIndex = 0; gridIndex < ledGrid.getGridCount(); gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            for (int y = 0; y < ledGrid.getGridSize(); y++) {
                for (int x = 0; x < ledGrid.getGridSize(); x++) {
                    // Calculate window coordinates for this grid
                    int windowX = gridConfig.getX() + x * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    int windowY = gridConfig.getY() + y * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    
                    // Calculate position in cloud image
                    int cloudX = (windowX * CLOUD_WIDTH) / windowWidth;
                    int cloudY = (windowY * CLOUD_HEIGHT) / windowHeight;
                    
                    // Ensure coordinates are within bounds
                    cloudX = Math.max(0, Math.min(cloudX, CLOUD_WIDTH - 1));
                    cloudY = Math.max(0, Math.min(cloudY, CLOUD_HEIGHT - 1));
                    
                    // Get color from cloud image
                    Color ledColor = new Color(cloudImage.getRGB(cloudX, cloudY));
                    
                    // Transform LED coordinates to match physical LED arrangement
                    // For 90-degree clockwise rotation: (x,y) -> (y, x)
                    int transformedX = y;
                    int transformedY = x;
                    
                    ledGrid.setLedColor(gridIndex, transformedX, transformedY, ledColor);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Clouds Animation";
    }

    @Override
    public String getDescription() {
        return "Cloud-like patterns using Perlin noise with color cycling and organic movement";
    }
}
