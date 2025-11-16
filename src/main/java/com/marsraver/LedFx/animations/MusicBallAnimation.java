package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Music Ball animation for the LED framework.
 * Creates audio-reactive particles that respond to music (simulated beat detection).
 */
@Log4j2
public class MusicBallAnimation implements LedAnimation {
    
    private class Ball {
        float x, y;
        float vx, vy;
        Color color;
        int age = 0;
        boolean alive = true;
        
        public Ball(float x, float y, float velocityX, float velocityY, Color color) {
            this.x = x;
            this.y = y;
            this.vx = velocityX;
            this.vy = velocityY;
            this.color = color;
        }
        
        public void update() {
            age += 3;
            vy += 0.1f; // gravity
            x += vx;
            y += vy;
            
            if (y > windowHeight || age >= 255) {
                alive = false;
            }
        }
    }
    
    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private long lastBeatTime;
    private Random random = new Random();
    private List<Ball> balls = new ArrayList<>();
    private float simulatedAudioLevel = 0f;
    private int beatCooldown = 0;
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        this.windowWidth = width;
        this.windowHeight = height;
        this.lastBeatTime = System.currentTimeMillis();
        
        log.debug("Music Ball Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
        log.debug("Note: Using simulated audio input (no microphone required)");
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Semi-transparent black for trail effect
        g.setColor(new Color(0, 0, 0, 45));
        g.fillRect(0, 0, width, height);
        
        // Simulate beat detection
        simulateBeat();
        
        // Spawn balls on beat
        spawnBallsOnBeat();
        
        // Update and draw all balls
        updateAndDrawBalls(g);
        
        // Update LED colors
        updateLedColors();
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Music Ball (Simulated Audio) - Press ESC to exit", 10, 20);
        g.drawString("Balls: " + balls.size(), 10, 35);
    }
    
    /**
     * Simulates beat detection with periodic beats
     */
    private void simulateBeat() {
        long currentTime = System.currentTimeMillis();
        
        // Simulate audio level (increase amplitude and speed a bit for more activity)
        simulatedAudioLevel = (float)(0.5f + 0.5f * Math.sin(currentTime / 180.0));
        
        // Reduce cooldown
        if (beatCooldown > 0) beatCooldown--;
        
    }
    
    /**
     * Spawns balls when a beat is detected (simulated)
     */
    private void spawnBallsOnBeat() {
        long currentTime = System.currentTimeMillis();
        
        // Simulate beat more often (~900ms) and with shorter cooldown for higher sensitivity
        if (currentTime - lastBeatTime > 900 && beatCooldown == 0) {
            lastBeatTime = currentTime;
            beatCooldown = 45; // Prevent too many simultaneous spawns but keep things lively
            
            // Random color
            Color ballColor = new Color(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
            );
            
            // Spawn more balls based on simulated audio level
            int ballCount = (int)(Math.abs(simulatedAudioLevel) * 18);
            for (int j = 0; j < ballCount; j++) {
                float x = random.nextFloat() * windowWidth;
                float y = random.nextFloat() * windowHeight;
                
                for (int i = 0; i < 3; i++) {
                    float vx = (random.nextFloat() - 0.5f) * 2f; // Slower velocity
                    float vy = (random.nextFloat() - 0.5f) * 2f; // Slower velocity
                    balls.add(new Ball(x, y, vx, vy, ballColor));
                }
            }
        }
    }
    
    /**
     * Updates and draws all balls
     */
    private void updateAndDrawBalls(Graphics2D g) {
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball ball = balls.get(i);
            ball.update();
            
            if (!ball.alive) {
                balls.remove(i);
                continue;
            }
            
            // Draw ball with fading transparency
            int alpha = 255 - ball.age;
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;
            
            Color drawColor = new Color(
                ball.color.getRed(),
                ball.color.getGreen(),
                ball.color.getBlue(),
                alpha
            );
            
            g.setColor(drawColor);
            g.fill(new Ellipse2D.Double(ball.x - 2.5, ball.y - 2.5, 5, 5));
        }
    }
    
    /**
     * Updates LED colors based on ball positions
     */
    private void updateLedColors() {
        ledGrid.clearAllLeds();
        
        int gridSize = ledGrid.getGridSize();
        int pixelSize = ledGrid.getPixelSize();
        int gridCount = ledGrid.getGridCount();
        
        for (Ball ball : balls) {
            if (!ball.alive) continue;
            
            // Map ball to each grid
            for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
                var gridConfig = ledGrid.getGridConfig(gridIndex);
                
                // Calculate ball position relative to this grid
                int ballX_relative = (int)ball.x - gridConfig.getX();
                int ballY_relative = (int)ball.y - gridConfig.getY();
                
                // Map to LED coordinates
                int ledX = ballX_relative / pixelSize;
                int ledY = ballY_relative / pixelSize;
                
                // Check if ball is within this grid
                if (ledX >= 0 && ledX < gridSize && ledY >= 0 && ledY < gridSize) {
                    // Use ball color with fade
                    int alpha = 255 - ball.age;
                    if (alpha > 0) {
                        float alphaRatio = alpha / 255.0f;
                        Color ledColor = new Color(
                            Math.min(255, Math.max(0, (int)(ball.color.getRed() * alphaRatio))),
                            Math.min(255, Math.max(0, (int)(ball.color.getGreen() * alphaRatio))),
                            Math.min(255, Math.max(0, (int)(ball.color.getBlue() * alphaRatio)))
                        );
                        // Standard logical coordinates: x = left->right, y = top->bottom
                        ledGrid.setLedColor(gridIndex, ledX, ledY, ledColor);
                    }
                }
            }
        }
    }
    
    public String getName() {
        return "Music Ball";
    }
    
    @Override
    public String getDescription() {
        return "Audio-reactive particles that respond to music beats (simulated audio)";
    }
}
