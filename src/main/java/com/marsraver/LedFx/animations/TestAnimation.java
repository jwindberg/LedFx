package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import com.marsraver.LedFx.layout.GridConfig;
import lombok.extern.log4j.Log4j2;

import java.awt.*;

/**
 * Test animation that lights corner LEDs with distinct colors on each grid.
 * Useful for debugging grid mapping, orientation, and serpentine layout.
 */
@Log4j2
public class TestAnimation implements LedAnimation {
    
    @SuppressWarnings("unused")
    private LedGrid ledGrid;
    @SuppressWarnings("unused")
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

        // Corner colors (visual and physical)
        Color topLeftColor = Color.RED;
        Color topRightColor = Color.GREEN;
        Color bottomLeftColor = Color.BLUE;
        Color bottomRightColor = Color.YELLOW;

        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            GridConfig cfg = ledGrid.getGridConfig(gridIndex);
            if (cfg == null) {
                continue;
            }

            int gridSize = cfg.getGridSize();
            int pixelSize = cfg.getPixelSize();

            // Clear this grid's LED buffer
            ledGrid.clearGrid(gridIndex);

            int maxIdx = gridSize - 1;

            // Logical LED coordinates:
            // (0,0)          ... (maxIdx,0)
            //    .                 .
            // (0,maxIdx) ... (maxIdx,maxIdx)
            //
            // We color each corner distinctly.
            ledGrid.setLedColor(gridIndex, 0, 0, topLeftColor);
            ledGrid.setLedColor(gridIndex, maxIdx, 0, topRightColor);
            ledGrid.setLedColor(gridIndex, 0, maxIdx, bottomLeftColor);
            ledGrid.setLedColor(gridIndex, maxIdx, maxIdx, bottomRightColor);

            // Draw matching circles on the canvas at LED centers so you can visually
            // compare screen vs. physical panel.
            drawCornerMarker(g, cfg, pixelSize, 0, 0, topLeftColor);
            drawCornerMarker(g, cfg, pixelSize, maxIdx, 0, topRightColor);
            drawCornerMarker(g, cfg, pixelSize, 0, maxIdx, bottomLeftColor);
            drawCornerMarker(g, cfg, pixelSize, maxIdx, maxIdx, bottomRightColor);
        }

        // Send LED buffer to devices
        ledGrid.sendToDevices();

        // Info legend
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Test Animation - Corners: TL=RED, TR=GREEN, BL=BLUE, BR=YELLOW", 10, 20);
    }

    private void drawCornerMarker(Graphics2D g, GridConfig cfg, int pixelSize, int ledX, int ledY, Color color) {
        int centerX = cfg.getX() + ledX * pixelSize + pixelSize / 2;
        int centerY = cfg.getY() + ledY * pixelSize + pixelSize / 2;
        int radius = Math.max(4, pixelSize / 3);

        g.setColor(color);
        g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
    
    @Override
    public String getName() {
        return "Test Animation";
    }
    
    @Override
    public String getDescription() {
        return "Lights corner LEDs with distinct colors for mapping/orientation testing";
    }
}
