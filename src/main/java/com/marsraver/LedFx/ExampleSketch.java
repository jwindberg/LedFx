package com.marsraver.LedFx;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Example sketch demonstrating the Processing-like framework.
 * Creates a bouncing ball animation.
 */
public class ExampleSketch implements Sketch {
    
    private int ballX, ballY;
    private int ballSize = 50;
    private int velocityX = 3, velocityY = 3;
    private Color ballColor;
    
    @Override
    public void init(int width, int height) {
        // Start the ball in the center
        ballX = width / 2;
        ballY = height / 2;
        
        // Set initial color
        ballColor = new Color(100, 150, 255);
        
        System.out.println("Sketch initialized with size: " + width + "x" + height);
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update ball position
        ballX += velocityX;
        ballY += velocityY;
        
        // Bounce off walls
        if (ballX <= 0 || ballX >= width - ballSize) {
            velocityX = -velocityX;
        }
        if (ballY <= 0 || ballY >= height - ballSize) {
            velocityY = -velocityY;
        }
        
        // Keep ball within bounds
        ballX = Math.max(0, Math.min(width - ballSize, ballX));
        ballY = Math.max(0, Math.min(height - ballSize, ballY));
        
        // Draw the ball
        g.setColor(ballColor);
        g.fill(new Ellipse2D.Double(ballX, ballY, ballSize, ballSize));
        
        // Add a subtle glow effect
        g.setColor(new Color(ballColor.getRed(), ballColor.getGreen(), ballColor.getBlue(), 50));
        g.fill(new Ellipse2D.Double(ballX - 5, ballY - 5, ballSize + 10, ballSize + 10));
        
        // Draw some info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Bouncing Ball - Press ESC to exit", 10, 25);
        g.drawString("Position: (" + ballX + ", " + ballY + ")", 10, 45);
    }
}
