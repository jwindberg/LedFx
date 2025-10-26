package com.marsraver.LedFx.layout;

import com.marsraver.LedFx.wled.WledController;
import java.awt.*;

/**
 * LED grid that works with layout configurations.
 * Manages multiple LED grids based on layout configuration.
 */
public class LayoutLedGrid {
    
    private final LayoutConfig layout;
    private final WledController[] controllers;
    private final Color[][][] ledColors; // [gridIndex][x][y]
    
    public LayoutLedGrid(LayoutConfig layout) {
        this.layout = layout;
        this.controllers = new WledController[layout.getGridCount()];
        this.ledColors = new Color[layout.getGridCount()][][];
        
        // Initialize controllers and LED color arrays
        for (int i = 0; i < layout.getGridCount(); i++) {
            GridConfig grid = layout.getGrids().get(i);
            controllers[i] = new WledController(grid.getDeviceIp(), grid.getLedCount());
            ledColors[i] = new Color[grid.getGridSize()][grid.getGridSize()];
            
            // Initialize all LEDs to black
            clearGrid(i);
        }
    }
    
    /**
     * Gets the layout configuration.
     */
    public LayoutConfig getLayout() {
        return layout;
    }
    
    /**
     * Gets a grid configuration by ID.
     */
    public GridConfig getGridConfig(String gridId) {
        return layout.getGridById(gridId);
    }
    
    /**
     * Gets a grid configuration by index.
     */
    public GridConfig getGridConfig(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= layout.getGridCount()) {
            return null;
        }
        return layout.getGrids().get(gridIndex);
    }
    
    /**
     * Gets the controller for a specific grid.
     */
    public WledController getController(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= controllers.length) {
            return null;
        }
        return controllers[gridIndex];
    }
    
    /**
     * Gets the controller for a grid by ID.
     */
    public WledController getController(String gridId) {
        GridConfig grid = layout.getGridById(gridId);
        if (grid == null) {
            return null;
        }
        
        // Find the grid index
        for (int i = 0; i < layout.getGridCount(); i++) {
            if (grid.getId().equals(layout.getGrids().get(i).getId())) {
                return controllers[i];
            }
        }
        return null;
    }
    
    /**
     * Gets the number of grids in this layout.
     */
    public int getGridCount() {
        return layout.getGridCount();
    }
    
    /**
     * Draws a visual representation of all LED grids on the sketch.
     * This helps visualize which pixels correspond to which LEDs.
     * 
     * @param g The Graphics2D object from the sketch
     */
    public void drawGrids(Graphics2D g) {
        for (int i = 0; i < layout.getGridCount(); i++) {
            GridConfig grid = layout.getGrids().get(i);
            drawSingleGrid(g, grid, i);
        }
    }
    
    /**
     * Draws a single LED grid with grid lines and LED indicators.
     */
    private void drawSingleGrid(Graphics2D g, GridConfig grid, int gridIndex) {
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
     * Sets the color of a specific LED in a specific grid.
     */
    public void setLedColor(int gridIndex, int x, int y, Color color) {
        if (gridIndex < 0 || gridIndex >= ledColors.length) {
            return;
        }
        
        GridConfig grid = getGridConfig(gridIndex);
        if (grid == null || x < 0 || x >= grid.getGridSize() || y < 0 || y >= grid.getGridSize()) {
            return;
        }
        
        ledColors[gridIndex][x][y] = color;
    }
    
    /**
     * Sets the color of a specific LED in a grid by ID.
     */
    public void setLedColor(String gridId, int x, int y, Color color) {
        GridConfig grid = layout.getGridById(gridId);
        if (grid == null) {
            return;
        }
        
        // Find the grid index
        for (int i = 0; i < layout.getGridCount(); i++) {
            if (grid.getId().equals(layout.getGrids().get(i).getId())) {
                setLedColor(i, x, y, color);
                return;
            }
        }
    }
    
    /**
     * Gets the color of a specific LED.
     */
    public Color getLedColor(int gridIndex, int x, int y) {
        if (gridIndex < 0 || gridIndex >= ledColors.length) {
            return null;
        }
        
        GridConfig grid = getGridConfig(gridIndex);
        if (grid == null || x < 0 || x >= grid.getGridSize() || y < 0 || y >= grid.getGridSize()) {
            return null;
        }
        
        return ledColors[gridIndex][x][y];
    }
    
    /**
     * Clears all LEDs in a specific grid to black.
     */
    public void clearGrid(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= ledColors.length) {
            return;
        }
        
        GridConfig grid = getGridConfig(gridIndex);
        if (grid == null) {
            return;
        }
        
        for (int x = 0; x < grid.getGridSize(); x++) {
            for (int y = 0; y < grid.getGridSize(); y++) {
                ledColors[gridIndex][x][y] = Color.BLACK;
            }
        }
    }
    
    /**
     * Clears all LEDs in all grids to black.
     */
    public void clearAllLeds() {
        for (int i = 0; i < ledColors.length; i++) {
            clearGrid(i);
        }
    }
    
    /**
     * Maps window coordinates to a specific grid and LED position.
     * 
     * @param windowX window X coordinate
     * @param windowY window Y coordinate
     * @return array with [gridIndex, ledX, ledY] or null if not within any grid
     */
    public int[] mapWindowToLed(int windowX, int windowY) {
        for (int i = 0; i < layout.getGridCount(); i++) {
            GridConfig grid = layout.getGrids().get(i);
            
            if (windowX >= grid.getX() && windowX < grid.getX() + grid.getWidth() &&
                windowY >= grid.getY() && windowY < grid.getY() + grid.getHeight()) {
                
                // Calculate LED coordinates within the grid
                int ledX = (windowX - grid.getX()) / grid.getPixelSize();
                int ledY = (windowY - grid.getY()) / grid.getPixelSize();
                
                // Ensure coordinates are within grid bounds
                ledX = Math.max(0, Math.min(ledX, grid.getGridSize() - 1));
                ledY = Math.max(0, Math.min(ledY, grid.getGridSize() - 1));
                
                return new int[]{i, ledX, ledY};
            }
        }
        
        return null; // Not within any grid
    }
    
    /**
     * Sends LED data to all devices.
     */
    public void sendToDevices() {
        for (int i = 0; i < controllers.length; i++) {
            GridConfig grid = getGridConfig(i);
            if (grid == null) {
                continue;
            }
            
            // Convert LED colors to RGB array
            int[] rgbData = new int[grid.getLedCount() * 3];
            int ledIndex = 0;
            
            for (int y = 0; y < grid.getGridSize(); y++) {
                for (int x = 0; x < grid.getGridSize(); x++) {
                    Color color = ledColors[i][x][y];
                    if (color == null) {
                        color = Color.BLACK;
                    }
                    
                    rgbData[ledIndex * 3] = color.getRed();
                    rgbData[ledIndex * 3 + 1] = color.getGreen();
                    rgbData[ledIndex * 3 + 2] = color.getBlue();
                    ledIndex++;
                }
            }
            
            // Send to device
            controllers[i].sendLedDataSimple(rgbData);
        }
    }
    
    /**
     * Turns off all devices.
     */
    public void turnOffAllDevices() {
        for (WledController controller : controllers) {
            if (controller != null) {
                controller.turnOff();
            }
        }
    }
    
    /**
     * Gets the grid size for a specific grid.
     */
    public int getGridSize(int gridIndex) {
        GridConfig grid = getGridConfig(gridIndex);
        return grid != null ? grid.getGridSize() : 0;
    }
    
    /**
     * Gets the pixel size for a specific grid.
     */
    public int getPixelSize(int gridIndex) {
        GridConfig grid = getGridConfig(gridIndex);
        return grid != null ? grid.getPixelSize() : 0;
    }
}