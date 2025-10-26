package com.marsraver.LedFx.layout;

/**
 * Configuration for a single LED grid within a layout.
 * Defines the grid's position, size, and device connection.
 */
public class GridConfig {
    
    private String id;
    private String deviceIp;
    private int ledCount;
    private int x;
    private int y;
    private int width;
    private int height;
    private int gridSize; // 16x16, 32x16, etc.
    private int pixelSize; // Size of each LED pixel in the window
    
    public GridConfig() {
        // Default constructor for XML binding
    }
    
    public GridConfig(String id, String deviceIp, int ledCount, int x, int y, int width, int height, int gridSize, int pixelSize) {
        this.id = id;
        this.deviceIp = deviceIp;
        this.ledCount = ledCount;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.gridSize = gridSize;
        this.pixelSize = pixelSize;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDeviceIp() {
        return deviceIp;
    }
    
    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }
    
    public int getLedCount() {
        return ledCount;
    }
    
    public void setLedCount(int ledCount) {
        this.ledCount = ledCount;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getGridSize() {
        return gridSize;
    }
    
    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }
    
    public int getPixelSize() {
        return pixelSize;
    }
    
    public void setPixelSize(int pixelSize) {
        this.pixelSize = pixelSize;
    }
    
    @Override
    public String toString() {
        return String.format("GridConfig{id='%s', deviceIp='%s', ledCount=%d, pos=(%d,%d), size=%dx%d, gridSize=%d, pixelSize=%d}",
                id, deviceIp, ledCount, x, y, width, height, gridSize, pixelSize);
    }
}

