package com.marsraver.LedFx.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete layout configuration loaded from XML.
 * Contains window settings and all LED grid configurations.
 */
public class LayoutConfig {
    
    private String name;
    private String title;
    private int windowWidth;
    private int windowHeight;
    private List<GridConfig> grids;
    
    public LayoutConfig() {
        this.grids = new ArrayList<>();
    }
    
    public LayoutConfig(String name, String title, int windowWidth, int windowHeight) {
        this.name = name;
        this.title = title;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.grids = new ArrayList<>();
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public int getWindowWidth() {
        return windowWidth;
    }
    
    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }
    
    public int getWindowHeight() {
        return windowHeight;
    }
    
    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }
    
    public List<GridConfig> getGrids() {
        return grids;
    }
    
    public void setGrids(List<GridConfig> grids) {
        this.grids = grids;
    }
    
    public void addGrid(GridConfig grid) {
        this.grids.add(grid);
    }
    
    public GridConfig getGridById(String id) {
        return grids.stream()
                .filter(grid -> id.equals(grid.getId()))
                .findFirst()
                .orElse(null);
    }
    
    public int getGridCount() {
        return grids.size();
    }
    
    @Override
    public String toString() {
        return String.format("LayoutConfig{name='%s', title='%s', window=%dx%d, grids=%d}",
                name, title, windowWidth, windowHeight, grids.size());
    }
}

