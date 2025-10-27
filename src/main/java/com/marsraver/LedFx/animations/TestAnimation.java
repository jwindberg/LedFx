package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import com.marsraver.LedFx.layout.GridConfig;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Test animation that displays the grid name in large centered text.
 * Useful for debugging grid mapping and layout issues.
 */
@Log4j2
public class TestAnimation implements LedAnimation {
    
    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        
        log.debug("Test Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        int gridCount = ledGrid.getGridCount();
        
        // Define colors for each grid
        Color[] gridColors = {
            Color.CYAN,      // Grid01 - Cyan
            Color.MAGENTA,   // Grid02 - Magenta
            Color.YELLOW,    // Grid03 - Yellow
            Color.GREEN      // Grid04 - Green
        };
        
        // Draw grid name for each grid
        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            GridConfig gridConfig = ledGrid.getGridConfig(gridIndex);
            // Extract just the number from "Grid01", "Grid02", etc., and remove the leading "0"
            String gridName = gridConfig.getId();
            String number = gridName.replaceAll("Grid", "").replaceFirst("^0", "");
            String displayText = number + " ↑";
            
            // Get the grid's center position
            int gridCenterX = gridConfig.getX() + gridConfig.getWidth() / 2;
            int gridCenterY = gridConfig.getY() + gridConfig.getHeight() / 2;
            
            // Set font to large and bold (double size: 86 * 2 = 172)
            Font font = new Font("Arial", Font.BOLD, 172);
            g.setFont(font);
            
            // Set color for this grid
            Color gridColor = gridIndex < gridColors.length ? gridColors[gridIndex] : Color.WHITE;
            g.setColor(gridColor);
            
            // Draw the text centered
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(displayText);
            int textHeight = fm.getAscent();
            int x = gridCenterX - textWidth / 2;
            int y = gridCenterY + textHeight / 2;
            
            g.drawString(displayText, x, y);
            
            // Map text to LEDs
            mapTextToLeds(gridIndex, displayText, gridCenterX, gridCenterY, g, fm, gridColor);
        }
        
        // Send to devices
        ledGrid.sendToDevices();
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Test Animation - Grid Names - Press ESC to exit", 10, 20);
    }
    
    private void mapTextToLeds(int gridIndex, String text, int centerX, int centerY, Graphics2D g, FontMetrics fm, Color gridColor) {
        // Set the same font for LED mapping (172pt)
        Font font = new Font("Arial", Font.BOLD, 172);
        g.setFont(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int textX = centerX - textWidth / 2;
        int textY = centerY + textHeight / 2;
        
        int gridSize = ledGrid.getGridSize();
        int pixelSize = ledGrid.getPixelSize();
        GridConfig gridConfig = ledGrid.getGridConfig(gridIndex);
        
        // Clear this grid first
        ledGrid.clearGrid(gridIndex);
        
        // Create a BufferedImage with the text rendered
        BufferedImage textImage = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D imgG = textImage.createGraphics();
        imgG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        imgG.setColor(Color.BLACK);
        imgG.fillRect(0, 0, textWidth, textHeight);
        imgG.setFont(font);
        imgG.setColor(gridColor);
        imgG.drawString(text, 0, textHeight);
        imgG.dispose();
        
        // Map each LED position
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                // Calculate window coordinates for this LED
                int windowX = gridConfig.getX() + x * pixelSize + pixelSize / 2;
                int windowY = gridConfig.getY() + y * pixelSize + pixelSize / 2;
                
                // Check if this LED is within the text bounds
                if (windowX >= textX && windowX < textX + textWidth &&
                    windowY >= textY - textHeight && windowY < textY) {
                    
                    // Sample from the text image
                    int imageX = windowX - textX;
                    int imageY = windowY - (textY - textHeight);
                    
                    // Clamp coordinates to image bounds
                    imageX = Math.max(0, Math.min(textWidth - 1, imageX));
                    imageY = Math.max(0, Math.min(textHeight - 1, imageY));
                    
                    int rgb = textImage.getRGB(imageX, imageY);
                    Color ledColor = new Color(rgb);
                    
                    // Only set the LED if the color is not black (i.e., part of the text)
                    // This filters out antialiasing artifacts
                    if (ledColor.getRed() > 10 || ledColor.getGreen() > 10 || ledColor.getBlue() > 10) {
                        // The LED layout is column-by-column, so we need to reverse x and y
                        ledGrid.setLedColor(gridIndex, y, x, ledColor);
                    }
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "Test Animation";
    }
    
    @Override
    public String getDescription() {
        return "Displays grid names in large centered text for testing";
    }
}
