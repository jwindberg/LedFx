package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Fast Plasma animation that creates liquid, organic movement patterns.
 * Uses lookup tables for fast performance with cycling colors.
 * Based on the FastPlasma Processing sketch by luis2048.
 */
public class FastPlasmaAnimation implements LedAnimation {

    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private long startTime;
    private int frameCount = 0;
    
    // Plasma rendering parameters
    private static final int PLASMA_WIDTH = 128;
    private static final int PLASMA_HEIGHT = 128;
    private BufferedImage plasmaImage;
    
    // Lookup tables for fast plasma generation
    private int[] palette = new int[128];
    private int[] plasmaLookup = new int[PLASMA_WIDTH * PLASMA_HEIGHT];
    
    // Plasma parameters
    private float scale = 32.0f;

    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.ledGrid = ledGrid;
        this.startTime = System.currentTimeMillis();
        
        // Create plasma image buffer
        this.plasmaImage = new BufferedImage(PLASMA_WIDTH, PLASMA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        
        // Initialize palette and lookup table
        initializePalette();
        initializePlasmaLookup();
    }
    
    /**
     * Initializes the color palette using sine functions.
     */
    private void initializePalette() {
        for (int i = 0; i < 128; i++) {
            float s1 = (float) Math.sin(i * Math.PI / 25.0);
            float s2 = (float) Math.sin(i * Math.PI / 50.0 + Math.PI / 4.0);
            
            // Convert to RGB color
            int r = (int) (128 + s1 * 128);
            int g = (int) (128 + s2 * 128);
            int b = (int) (s1 * 128);
            
            // Clamp values to 0-255
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));
            
            palette[i] = new Color(r, g, b).getRGB();
        }
    }
    
    /**
     * Initializes the plasma lookup table with pre-calculated values.
     */
    private void initializePlasmaLookup() {
        for (int x = 0; x < PLASMA_WIDTH; x++) {
            for (int y = 0; y < PLASMA_HEIGHT; y++) {
                int index = x + y * PLASMA_WIDTH;
                
                // Calculate plasma value using multiple sine waves
                float value1 = (float) Math.sin(x / scale);
                float value2 = (float) Math.cos(y / scale);
                float value3 = (float) Math.sin(Math.sqrt(x * x + y * y) / scale);
                
                // Combine the values and normalize to 0-127 range
                float combined = (127.5f + 127.5f * value1) + 
                               (127.5f + 127.5f * value2) + 
                               (127.5f + 127.5f * value3);
                
                plasmaLookup[index] = (int) (combined / 4.0f) & 127;
            }
        }
    }

    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update plasma pattern
        updatePlasmaPattern();
        
        // Draw the plasma image scaled to window size
        g.drawImage(plasmaImage, 0, 0, width, height, null);
        
        // Update LED colors
        updateLedColors();
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Fast Plasma Animation - Press ESC to exit", 10, 20);
        g.drawString("Frame: " + frameCount, 10, 35);
        g.drawString("Time: " + String.format("%.1f", (System.currentTimeMillis() - startTime) / 1000.0f) + "s", 10, 50);
    }
    
    /**
     * Updates the plasma pattern using lookup tables.
     */
    private void updatePlasmaPattern() {
        // Update frame count for animation
        frameCount++;
        
        // Generate plasma pattern using lookup table
        for (int pixelCount = 0; pixelCount < plasmaLookup.length; pixelCount++) {
            // Use frame count to animate the plasma
            int paletteIndex = (plasmaLookup[pixelCount] + frameCount) & 127;
            plasmaImage.setRGB(pixelCount % PLASMA_WIDTH, pixelCount / PLASMA_WIDTH, palette[paletteIndex]);
        }
    }
    
    /**
     * Updates LED colors based on the current plasma pattern.
     * The animation just draws to the window - the layout system handles LED mapping.
     */
    private void updateLedColors() {
        // Clear all LEDs first
        ledGrid.clearAllLeds();
        
        // Map plasma pattern to LED grids using the standard mapping
        mapPlasmaToLedGrid();
    }
    
    /**
     * Maps the plasma pattern to the LED grid using the standard DualLedGrid interface.
     * This lets the layout system handle the actual LED mapping.
     */
    private void mapPlasmaToLedGrid() {
        // Map plasma pattern to all grids in the layout
        for (int gridIndex = 0; gridIndex < ledGrid.getGridCount(); gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            for (int y = 0; y < ledGrid.getGridSize(); y++) {
                for (int x = 0; x < ledGrid.getGridSize(); x++) {
                    // Calculate window coordinates for this grid
                    int windowX = gridConfig.getX() + x * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    int windowY = gridConfig.getY() + y * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    
                    // Calculate position in plasma image
                    int plasmaX = (windowX * PLASMA_WIDTH) / windowWidth;
                    int plasmaY = (windowY * PLASMA_HEIGHT) / windowHeight;
                    
                    // Ensure coordinates are within bounds
                    plasmaX = Math.max(0, Math.min(plasmaX, PLASMA_WIDTH - 1));
                    plasmaY = Math.max(0, Math.min(plasmaY, PLASMA_HEIGHT - 1));
                    
                    // Get color from plasma image
                    Color ledColor = new Color(plasmaImage.getRGB(plasmaX, plasmaY));
                    
                    // Use the standard logical LED coordinates (x = left->right, y = top->bottom)
                    // so all animations share the same mapping and LedGrid handles packing.
                    ledGrid.setLedColor(gridIndex, x, y, ledColor);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Fast Plasma Animation";
    }

    @Override
    public String getDescription() {
        return "Fast plasma effect with liquid, organic movement using lookup tables for optimal performance";
    }
}
