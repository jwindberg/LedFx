package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Star Trek warp speed starfield animation.
 * Stars move from the center toward the edges at increasing speeds.
 */
public class StarfieldAnimation implements LedAnimation {

    @SuppressWarnings("unused")
    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private List<Star> stars;
    private Random random;
    
    private class Star {
        // Trail length for each star; longer history = longer visible tail
        private static final int TRAIL_LENGTH = 40;
        float x, y;  // Normalized position (-1 to 1)
        float speed;
        Color color;
        int size;
        float[] trailX = new float[TRAIL_LENGTH];
        float[] trailY = new float[TRAIL_LENGTH];
        
        Star(float x, float y, float speed, Color color) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.color = color;
            this.size = random.nextFloat() < 0.2f ? 2 : 1; // 20% chance for larger star

            // Initialize trail history at starting position
            for (int i = 0; i < TRAIL_LENGTH; i++) {
                trailX[i] = x;
                trailY[i] = y;
            }
        }
        
        void update(float centerX, float centerY, float maxSpeed) {
            // Move toward edges
            float dx = x - centerX;
            float dy = y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (dist > 0.01f) {
                x += dx / dist * speed * maxSpeed;
                y += dy / dist * speed * maxSpeed;
            }
            
            // Reset if outside bounds
            if (Math.abs(x) > 1.5f || Math.abs(y) > 1.5f) {
                // Start from center with random direction
                float angle = random.nextFloat() * 2 * (float) Math.PI;
                float radius = 0.1f;
                x = centerX + (float) Math.cos(angle) * radius;
                y = centerY + (float) Math.sin(angle) * radius;
                speed = 0.02f + random.nextFloat() * 0.03f;
            }

            // Update trail history (store most recent positions)
            for (int i = TRAIL_LENGTH - 1; i > 0; i--) {
                trailX[i] = trailX[i - 1];
                trailY[i] = trailY[i - 1];
            }
            trailX[0] = x;
            trailY[0] = y;
        }
    }

    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.ledGrid = ledGrid;
        this.random = new Random();
        this.stars = new ArrayList<>();
        
        // Create 100 stars starting from random positions (half the original)
        float centerX = 0;
        float centerY = 0;
        for (int i = 0; i < 100; i++) {
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float radius = 0.05f + random.nextFloat() * 1.0f;
            float x = (float) (centerX + Math.cos(angle) * radius);
            float y = (float) (centerY + Math.sin(angle) * radius);
            float speed = 0.02f + random.nextFloat() * 0.03f;
            // Richer star palette: mostly white, with some colored stars
            float roll = random.nextFloat();
            Color color;
            if (roll < 0.55f) {
                color = Color.WHITE;
            } else if (roll < 0.70f) {
                color = Color.CYAN;
            } else if (roll < 0.82f) {
                color = Color.MAGENTA;
            } else if (roll < 0.90f) {
                color = Color.YELLOW;
            } else {
                // Random warm/cool hue for variety
                float hue = random.nextFloat(); // 0‑1
                color = Color.getHSBColor(hue, 0.2f + 0.6f * random.nextFloat(), 0.7f + 0.3f * random.nextFloat());
            }
            stars.add(new Star(x, y, speed, color));
        }
    }

    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        float centerX = 0;
        float centerY = 0;
        // Reduce overall star speed again (quarter of original)
        float maxSpeed = 0.25f;
        
        // Update and draw stars
        for (Star star : stars) {
            star.update(centerX, centerY, maxSpeed);
            
            // Convert normalized coordinates to screen coordinates
            int screenX = (int) ((star.x + 1) * width / 2);
            int screenY = (int) ((star.y + 1) * height / 2);
            
            // Calculate brightness based on distance from center (for on-screen view)
            float dx = star.x - centerX;
            float dy = star.y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float brightness = Math.min(1.0f, Math.max(0.0f, 0.3f + dist * 0.7f));
            
            int rHead = (int) (star.color.getRed() * brightness);
            int gHead = (int) (star.color.getGreen() * brightness);
            int bHead = (int) (star.color.getBlue() * brightness);
            rHead = Math.max(0, Math.min(255, rHead));
            gHead = Math.max(0, Math.min(255, gHead));
            bHead = Math.max(0, Math.min(255, bHead));
            Color starColor = new Color(rHead, gHead, bHead);

            // Draw core
            g.setColor(starColor);
            
            if (star.size > 1) {
                g.fillOval(screenX - 1, screenY - 1, 3, 3);
            } else {
                g.fillRect(screenX, screenY, 1, 1);
            }

            // Draw a short fading trail behind the star using its recent positions
            for (int i = 1; i < Star.TRAIL_LENGTH; i++) {
                float txNorm = star.trailX[i];
                float tyNorm = star.trailY[i];
                int trailX = (int) ((txNorm + 1) * width / 2);
                int trailY = (int) ((tyNorm + 1) * height / 2);

                // Fade trail brightness over history; i=1 is brightest, last is dimmest
                float trailFactor = (Star.TRAIL_LENGTH - i) / (float) Star.TRAIL_LENGTH;
                float trailBrightness = brightness * trailFactor * 1.2f;
                trailBrightness = Math.min(1.0f, Math.max(0.0f, trailBrightness));

                if (trailBrightness > 0.01f) {
                    int rTrail = (int) (star.color.getRed() * trailBrightness);
                    int gTrail = (int) (star.color.getGreen() * trailBrightness);
                    int bTrail = (int) (star.color.getBlue() * trailBrightness);
                    rTrail = Math.max(0, Math.min(255, rTrail));
                    gTrail = Math.max(0, Math.min(255, gTrail));
                    bTrail = Math.max(0, Math.min(255, bTrail));
                    Color trailColor = new Color(rTrail, gTrail, bTrail);
                    g.setColor(trailColor);
                    if (star.size > 1) {
                        g.fillOval(trailX - 1, trailY - 1, 3, 3);
                    } else {
                        g.fillRect(trailX, trailY, 1, 1);
                    }
                }
            }
        }
        
        // Map to LEDs
        mapToLeds(g, ledGrid);
    }
    
    private void mapToLeds(Graphics2D g, LedGrid ledGrid) {
        int gridCount = ledGrid.getGridCount();

        // Clear all grids so only the current frame's stars remain lit
        ledGrid.clearAllLeds();

        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            int gridSize = gridConfig.getGridSize();
            int pixelSize = gridConfig.getPixelSize();
            
            // Sample each LED position
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    // Sample at the center of each LED cell in window coordinates
                    int windowX = gridConfig.getX() + x * pixelSize + pixelSize / 2;
                    int windowY = gridConfig.getY() + y * pixelSize + pixelSize / 2;

                    if (windowX >= 0 && windowX < windowWidth && windowY >= 0 && windowY < windowHeight) {
                        Color ledColor = sampleStarColor(windowX, windowY);

                        if (ledColor != null && ledColor.getRGB() != Color.BLACK.getRGB()) {
                            // Standard logical coordinates: x = left->right, y = top->bottom
                            ledGrid.setLedColor(gridIndex, x, y, ledColor);
                        }
                    }
                }
            }
        }
    }
    
    private Color sampleStarColor(int screenX, int screenY) {
        // Convert screen coordinates to normalized coordinates
        float normalizedX = (screenX / (float) windowWidth) * 2 - 1;
        float normalizedY = (screenY / (float) windowHeight) * 2 - 1;
        
        // Check if any star head or trail is at this position
        for (Star star : stars) {
            // First, check the star head
            {
                float dx = normalizedX - star.x;
                float dy = normalizedY - star.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float starRadius = star.size * 0.02f; // slightly larger for better LED hit
                if (dist < starRadius) {
                    float centerX = 0;
                    float centerY = 0;
                    float starDx = star.x - centerX;
                    float starDy = star.y - centerY;
                    float starDist = (float) Math.sqrt(starDx * starDx + starDy * starDy);
                    // LED-only brightness curve: capped lower and globally dimmed
                    float brightness = Math.min(0.7f, Math.max(0.0f, 0.2f + starDist * 0.4f));
                    float dim = 0.4f;
                    float finalBrightness = brightness * dim;

                    int r = (int) (star.color.getRed() * finalBrightness);
                    int g = (int) (star.color.getGreen() * finalBrightness);
                    int b = (int) (star.color.getBlue() * finalBrightness);
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));

                    return new Color(r, g, b);
                }
            }

            // Then, check the short fading trail positions
            for (int i = 1; i < Star.TRAIL_LENGTH; i++) {
                float tx = star.trailX[i];
                float ty = star.trailY[i];
                float dx = normalizedX - tx;
                float dy = normalizedY - ty;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float trailRadius = star.size * 0.02f;

                if (dist < trailRadius) {
                    float centerX = 0;
                    float centerY = 0;
                    float starDx = tx - centerX;
                    float starDy = ty - centerY;
                    float starDist = (float) Math.sqrt(starDx * starDx + starDy * starDy);
                    float baseBrightness = Math.min(0.7f, Math.max(0.0f, 0.2f + starDist * 0.4f));
                    float trailFactor = (Star.TRAIL_LENGTH - i) / (float) Star.TRAIL_LENGTH;
                    float brightness = baseBrightness * trailFactor * 0.9f;
                    float dim = 0.4f;
                    float finalBrightness = brightness * dim;

                    if (finalBrightness > 0.01f) {
                        int r = (int) (star.color.getRed() * finalBrightness);
                        int g = (int) (star.color.getGreen() * finalBrightness);
                        int b = (int) (star.color.getBlue() * finalBrightness);
                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));

                        return new Color(r, g, b);
                    }
                }
            }
        }
        
        return Color.BLACK;
    }

    @Override
    public String getName() {
        return "Starfield";
    }

    @Override
    public String getDescription() {
        return "Star Trek warp speed starfield effect";
    }

    @Override
    public void stop() {
        // No cleanup needed
    }
}
