package com.marsraver.LedFx;

import com.marsraver.LedFx.layout.LayoutConfig;
import com.marsraver.LedFx.layout.GridConfig;
import com.marsraver.LedFx.wled.WledController;
import com.marsraver.LedFx.wled.WledArtNetController;
import com.marsraver.LedFx.wled.ColorMapping;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified LED grid that manages multiple LED grids based on layout configuration.
 * This replaces the old SingleLedGrid and DualLedGrid classes with a flexible system
 * that can handle any number of grids positioned anywhere in the window.
 * 
 * Now uses Art-Net UDP protocol for much faster LED updates (120+ FPS).
 */
@Log4j2
public class LedGrid {
    
    private final LayoutConfig layout;
    private final List<WledArtNetController> controllers; // Changed to Art-Net controllers
    private final List<Color[][]> ledColors; // [gridIndex][x][y]
    private final List<GridConfig> grids;
    
    public LedGrid(LayoutConfig layout) {
        this.layout = layout;
        this.grids = layout.getGrids();
        this.controllers = new ArrayList<>();
        this.ledColors = new ArrayList<>();
        
        // Initialize Art-Net controllers and LED color arrays for each grid
        for (int i = 0; i < grids.size(); i++) {
            GridConfig grid = grids.get(i);
            // Use Art-Net controller with universe number based on grid index
            // Use the color mapping from GridConfig, or default to GBR if not specified
            ColorMapping colorMapping = grid.getColorMapping();
            if (colorMapping == null) {
                colorMapping = ColorMapping.GBR;
            }
            controllers.add(new WledArtNetController(grid.getDeviceIp(), grid.getLedCount(), i, colorMapping));
            
            // Initialize LED color array for this grid
            Color[][] gridColors = new Color[grid.getGridSize()][grid.getGridSize()];
            ledColors.add(gridColors);
            
            // Initialize all LEDs to black
            clearGrid(i);
        }
        
        log.debug("Unified LED Grid initialized with Art-Net:");
        log.debug("  Layout: " + layout.getName());
        log.debug("  Window: " + layout.getWindowWidth() + "x" + layout.getWindowHeight());
        log.debug("  Grids: " + grids.size());
        for (int i = 0; i < grids.size(); i++) {
            GridConfig grid = grids.get(i);
            log.debug("    Grid " + (i + 1) + " (" + grid.getId() + "): " + 
                             grid.getGridSize() + "x" + grid.getGridSize() + 
                             " at (" + grid.getX() + ", " + grid.getY() + ") -> " + grid.getDeviceIp() +
                             " (Universe " + i + ")");
        }
    }
    
    /**
     * Sets the color of a specific LED using window coordinates.
     * The layout system will determine which physical LED device and position to use.
     * 
     * @param windowX The X coordinate in the window
     * @param windowY The Y coordinate in the window
     * @param color The color to set
     */
    public void setLedColor(int windowX, int windowY, Color color) {
        // Find which grid this window coordinate maps to
        for (int gridIndex = 0; gridIndex < grids.size(); gridIndex++) {
            GridConfig grid = grids.get(gridIndex);
            
            if (windowX >= grid.getX() && windowX < grid.getX() + grid.getWidth() &&
                windowY >= grid.getY() && windowY < grid.getY() + grid.getHeight()) {
                
                // Convert window coordinates to grid coordinates
                int gridX = (windowX - grid.getX()) / grid.getPixelSize();
                int gridY = (windowY - grid.getY()) / grid.getPixelSize();
                
                // Clamp to grid bounds
                gridX = Math.max(0, Math.min(grid.getGridSize() - 1, gridX));
                gridY = Math.max(0, Math.min(grid.getGridSize() - 1, gridY));
                
                // Set the LED color
                ledColors.get(gridIndex)[gridX][gridY] = color;
                return;
            }
        }
    }
    
    /**
     * Sets the color of a specific LED in a specific grid.
     * 
     * @param gridIndex The index of the grid (0-based)
     * @param gridX The X position within the grid (0-based)
     * @param gridY The Y position within the grid (0-based)
     * @param color The color to set
     */
    public void setLedColor(int gridIndex, int gridX, int gridY, Color color) {
        if (gridIndex >= 0 && gridIndex < grids.size()) {
            GridConfig grid = grids.get(gridIndex);
            if (gridX >= 0 && gridX < grid.getGridSize() && gridY >= 0 && gridY < grid.getGridSize()) {
                ledColors.get(gridIndex)[gridX][gridY] = color;
            }
        }
    }
    
    /**
     * Clears all LEDs (sets them to black/off).
     */
    public void clearAllLeds() {
        for (int i = 0; i < grids.size(); i++) {
            clearGrid(i);
        }
    }
    
    /**
     * Clears all LEDs in a specific grid.
     * 
     * @param gridIndex The index of the grid to clear
     */
    public void clearGrid(int gridIndex) {
        if (gridIndex >= 0 && gridIndex < grids.size()) {
            GridConfig grid = grids.get(gridIndex);
            Color[][] gridColors = ledColors.get(gridIndex);
            for (int x = 0; x < grid.getGridSize(); x++) {
                for (int y = 0; y < grid.getGridSize(); y++) {
                    gridColors[x][y] = Color.BLACK;
                }
            }
        }
    }
    
    /**
     * Sends the current LED data to all connected devices.
     * 
     * @return true if all devices were successful, false otherwise
     */
    public boolean sendToDevices() {
        boolean allSuccess = true;
        for (int i = 0; i < grids.size(); i++) {
            GridConfig grid = grids.get(i);
            WledArtNetController controller = controllers.get(i);
            Color[][] gridColors = ledColors.get(i);
            
            // Convert Color[][] to int[] for WLED controller
            // Use column-by-column layout (top to bottom, left to right)
            int[] ledData = new int[grid.getGridSize() * grid.getGridSize() * 3];
            // Initialize all values to 0 to prevent artifacts from uninitialized data
            java.util.Arrays.fill(ledData, 0);
            int index = 0;
            for (int x = 0; x < grid.getGridSize(); x++) {
                for (int y = 0; y < grid.getGridSize(); y++) {
                    Color color = gridColors[x][y];
                    if (color == null) color = Color.BLACK;
                    // Clamp color values to valid 0-255 range to prevent edge artifacts
                    ledData[index++] = Math.min(255, Math.max(0, color.getRed()));
                    ledData[index++] = Math.min(255, Math.max(0, color.getGreen()));
                    ledData[index++] = Math.min(255, Math.max(0, color.getBlue()));
                }
            }
            
            boolean success = controller.sendLedData(ledData);
            if (!success) {
                log.error("Failed to send LED data to " + grid.getDeviceIp() + " (Universe " + i + ")");
                allSuccess = false;
            }
        }
        return allSuccess;
    }
    
    /**
     * Maps window coordinates to LED grid coordinates.
     * 
     * @param windowX The X coordinate in the window
     * @param windowY The Y coordinate in the window
     * @return An array containing [gridIndex, ledX, ledY] or null if not within any grid
     */
    public int[] mapWindowToLed(int windowX, int windowY) {
        for (int gridIndex = 0; gridIndex < grids.size(); gridIndex++) {
            GridConfig grid = grids.get(gridIndex);
            
            if (windowX >= grid.getX() && windowX < grid.getX() + grid.getWidth() &&
                windowY >= grid.getY() && windowY < grid.getY() + grid.getHeight()) {
                
                int gridX = (windowX - grid.getX()) / grid.getPixelSize();
                int gridY = (windowY - grid.getY()) / grid.getPixelSize();
                
                // Clamp to grid bounds
                gridX = Math.max(0, Math.min(grid.getGridSize() - 1, gridX));
                gridY = Math.max(0, Math.min(grid.getGridSize() - 1, gridY));
                
                return new int[]{gridIndex, gridX, gridY};
            }
        }
        return null;
    }
    
    /**
     * Gets the number of LED grids in this layout.
     * 
     * @return The number of grids
     */
    public int getGridCount() {
        return grids.size();
    }
    
    /**
     * Gets the size of each LED grid (assumes all grids are the same size).
     * 
     * @return The grid size (e.g., 16 for 16x16)
     */
    public int getGridSize() {
        if (grids.isEmpty()) return 0;
        return grids.get(0).getGridSize(); // Assume all grids are the same size
    }
    
    /**
     * Gets the pixel size for each LED in the window.
     * 
     * @return The pixel size
     */
    public int getPixelSize() {
        if (grids.isEmpty()) return 0;
        return grids.get(0).getPixelSize(); // Assume all grids use the same pixel size
    }
    
    /**
     * Gets the window width.
     * 
     * @return The window width
     */
    public int getWindowWidth() {
        return layout.getWindowWidth();
    }
    
    /**
     * Gets the window height.
     * 
     * @return The window height
     */
    public int getWindowHeight() {
        return layout.getWindowHeight();
    }
    
    /**
     * Gets a specific grid configuration.
     * 
     * @param gridIndex The index of the grid
     * @return The grid configuration
     */
    public GridConfig getGridConfig(int gridIndex) {
        if (gridIndex >= 0 && gridIndex < grids.size()) {
            return grids.get(gridIndex);
        }
        return null;
    }
    
    /**
     * Gets a grid configuration by ID.
     * 
     * @param gridId The ID of the grid
     * @return The grid configuration
     */
    public GridConfig getGridConfig(String gridId) {
        return layout.getGridById(gridId);
    }
    
    /**
     * Gets the controller for a specific grid.
     * 
     * @param gridIndex The index of the grid
     * @return The WLED Art-Net controller
     */
    public WledArtNetController getController(int gridIndex) {
        if (gridIndex >= 0 && gridIndex < controllers.size()) {
            return controllers.get(gridIndex);
        }
        return null;
    }
    
    /**
     * Gets the controller for a grid by ID.
     * 
     * @param gridId The ID of the grid
     * @return The WLED Art-Net controller
     */
    public WledArtNetController getController(String gridId) {
        GridConfig grid = layout.getGridById(gridId);
        if (grid != null) {
            int gridIndex = grids.indexOf(grid);
            return getController(gridIndex);
        }
        return null;
    }
    
    /**
     * Draws all LED grids with grid lines and LED indicators.
     */
    public void drawGrid(Graphics2D g) {
        for (int i = 0; i < grids.size(); i++) {
            drawSingleGrid(g, i);
        }
    }
    
    /**
     * Draws a single LED grid with grid lines and LED indicators.
     */
    private void drawSingleGrid(Graphics2D g, int gridIndex) {
        GridConfig grid = grids.get(gridIndex);
        Color[][] gridColors = ledColors.get(gridIndex);
        
        // Draw grid lines
        g.setColor(new Color(255, 255, 255, 100)); // Semi-transparent white
        g.setStroke(new BasicStroke(1));
        
        for (int i = 0; i <= grid.getGridSize(); i++) {
            int x = grid.getX() + (i * grid.getPixelSize());
            int y = grid.getY() + (i * grid.getPixelSize());
            
            // Vertical lines
            g.drawLine(x, grid.getY(), x, grid.getY() + grid.getGridSize() * grid.getPixelSize());
            // Horizontal lines
            g.drawLine(grid.getX(), y, grid.getX() + grid.getGridSize() * grid.getPixelSize(), y);
        }
        
        // Draw LED indicators (small circles at LED positions)
        g.setColor(new Color(255, 255, 0, 150)); // Semi-transparent yellow
        for (int ledY = 0; ledY < grid.getGridSize(); ledY++) {
            for (int ledX = 0; ledX < grid.getGridSize(); ledX++) {
                int centerX = grid.getX() + (ledX * grid.getPixelSize()) + (grid.getPixelSize() / 2);
                int centerY = grid.getY() + (ledY * grid.getPixelSize()) + (grid.getPixelSize() / 2);
                
                g.fillOval(centerX - 2, centerY - 2, 4, 4);
            }
        }
        
        // Draw grid label
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(grid.getId(), grid.getX(), grid.getY() - 10);
    }
    
    /**
     * Samples colors from the graphics context and updates LED colors.
     * This method samples colors from the current graphics context at each LED position.
     */
    public void sampleColors(Graphics2D g) {
        for (int gridIndex = 0; gridIndex < grids.size(); gridIndex++) {
            GridConfig grid = grids.get(gridIndex);
            Color[][] gridColors = ledColors.get(gridIndex);
            
            for (int ledY = 0; ledY < grid.getGridSize(); ledY++) {
                for (int ledX = 0; ledX < grid.getGridSize(); ledX++) {
                    int centerX = grid.getX() + (ledX * grid.getPixelSize()) + (grid.getPixelSize() / 2);
                    int centerY = grid.getY() + (ledY * grid.getPixelSize()) + (grid.getPixelSize() / 2);
                    
                    // Sample color from the center of the LED area
                    Color color = new Color(g.getColor().getRGB());
                    gridColors[ledX][ledY] = color;
                }
            }
        }
    }
    
    /**
     * Gets the layout configuration.
     * 
     * @return The layout configuration
     */
    public LayoutConfig getLayout() {
        return layout;
    }
}