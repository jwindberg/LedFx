package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;

/**
 * DJ Light animation that creates classic DJ lighting effects.
 * Features strobe, color cycling, and dynamic patterns.
 */
public class DjLightAnimation implements LedAnimation {

    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private long lastTime;
    
    // Audio capture
    private TargetDataLine microphone;
    private AudioFormat audioFormat;
    private boolean audioInitialized = false;
    
    // Audio analysis
    private byte[] audioBuffer;
    private float[] audioSamples;
    private float currentVolume = 0.0f;
    private float bassLevel = 0.0f;
    private float midLevel = 0.0f;
    private float trebleLevel = 0.0f;
    
    // DJ Light effects
    private float hue = 0.0f;
    private float strobeTimer = 0.0f;
    private boolean strobeOn = false;
    private float strobeSpeed = 1.0f;
    private float colorSpeed = 1.0f;
    private float brightness = 1.0f;
    
    // Animation parameters
    private float centerX, centerY;
    private float maxRadius;
    private float[] frequencyBands = new float[8];
    private int numBands = 8;
    
    // DJ Light patterns
    private float patternTimer = 0.0f;
    private int currentPattern = 0;
    private float[] patternIntensities = new float[4];
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.ledGrid = ledGrid;
        this.lastTime = System.currentTimeMillis();
        
        // Calculate center and max radius
        int totalGridWidth = ledGrid.getGridSize() * ledGrid.getPixelSize() * 2;
        this.centerX = ledGrid.getGridConfig(0).getX() + totalGridWidth / 2.0f;
        this.centerY = ledGrid.getGridConfig(0).getY() + (ledGrid.getGridSize() * ledGrid.getPixelSize()) / 2.0f;
        this.maxRadius = Math.min(totalGridWidth, ledGrid.getGridSize() * ledGrid.getPixelSize()) / 2.0f;
        
        // Initialize frequency bands
        Arrays.fill(frequencyBands, 0.0f);
        Arrays.fill(patternIntensities, 0.0f);
        
        // Initialize audio buffers
        this.audioBuffer = new byte[1024];
        this.audioSamples = new float[1024];
        
        // Initialize audio capture
        initializeAudio();
        
        log.debug("DJ Light Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
    }
    
    /**
     * Initializes audio capture from the default microphone.
     */
    private void initializeAudio() {
        try {
            // Set up audio format
            audioFormat = new AudioFormat(44100, 16, 1, true, true);
            
            // Get the default microphone
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            microphone = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            microphone.open(audioFormat);
            microphone.start();
            
            audioInitialized = true;
            log.debug("Microphone initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize microphone: " + e.getMessage());
            log.error("DJ Light effects will be audio-independent");
            audioInitialized = false;
        }
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Clear the background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        // Update audio analysis
        updateAudioAnalysis();
        
        // Update animation parameters
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;
        
        // Update DJ Light effects
        updateDjLightEffects(timeDelta);
        
        // Draw DJ Light patterns
        drawDjLightPatterns(g);
        
        // Update LED colors
        updateLedColors();
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("DJ Light Animation - Press ESC to exit", 10, 20);
        g.drawString("Volume: " + String.format("%.1f", currentVolume * 100) + "%", 10, 35);
        g.drawString("Strobe: " + (strobeOn ? "ON" : "OFF"), 10, 50);
        g.drawString("Audio: " + (audioInitialized ? "Connected" : "Not Available"), 10, 65);
    }
    
    /**
     * Updates audio analysis by reading from the microphone.
     */
    private void updateAudioAnalysis() {
        if (!audioInitialized || microphone == null) {
            return;
        }
        
        try {
            // Read audio data
            int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
            if (bytesRead > 0) {
                // Convert bytes to float samples
                convertBytesToSamples(bytesRead);
                
                // Calculate volume (RMS)
                float sum = 0.0f;
                for (int i = 0; i < audioSamples.length; i++) {
                    sum += audioSamples[i] * audioSamples[i];
                }
                currentVolume = (float) Math.sqrt(sum / audioSamples.length);
                
                // Calculate frequency bands
                calculateFrequencyBands();
            }
        } catch (Exception e) {
            // Audio read failed, continue with last known values
        }
    }
    
    /**
     * Converts audio bytes to float samples.
     */
    private void convertBytesToSamples(int bytesRead) {
        int samplesRead = bytesRead / 2; // 16-bit audio = 2 bytes per sample
        
        for (int i = 0; i < samplesRead && i < audioSamples.length; i++) {
            // Convert 16-bit signed little-endian bytes to float
            int sample = (audioBuffer[i * 2] & 0xFF) | ((audioBuffer[i * 2 + 1] & 0xFF) << 8);
            if (sample > 32767) sample -= 65536; // Convert to signed
            audioSamples[i] = sample / 32768.0f; // Normalize to [-1, 1]
        }
    }
    
    /**
     * Calculates frequency bands from audio data.
     */
    private void calculateFrequencyBands() {
        // Simplified frequency analysis
        // Bass (low frequencies) - first 1/4 of samples
        float bassSum = 0.0f;
        for (int i = 0; i < audioSamples.length / 4; i++) {
            bassSum += Math.abs(audioSamples[i]);
        }
        bassLevel = bassSum / (audioSamples.length / 4);
        
        // Mid frequencies - middle half of samples
        float midSum = 0.0f;
        for (int i = audioSamples.length / 4; i < 3 * audioSamples.length / 4; i++) {
            midSum += Math.abs(audioSamples[i]);
        }
        midLevel = midSum / (audioSamples.length / 2);
        
        // Treble (high frequencies) - last 1/4 of samples
        float trebleSum = 0.0f;
        for (int i = 3 * audioSamples.length / 4; i < audioSamples.length; i++) {
            trebleSum += Math.abs(audioSamples[i]);
        }
        trebleLevel = trebleSum / (audioSamples.length / 4);
    }
    
    /**
     * Updates DJ Light effects based on audio and time.
     */
    private void updateDjLightEffects(float timeDelta) {
        // Update hue for color cycling
        hue += timeDelta * colorSpeed * 30.0f;
        if (hue > 360) hue -= 360;
        
        // Update strobe effect
        strobeTimer += timeDelta * strobeSpeed * 10.0f;
        strobeOn = (strobeTimer % 2.0f) < 1.0f;
        
        // Update pattern timer
        patternTimer += timeDelta;
        
        // Change pattern every 8 seconds
        currentPattern = ((int)(patternTimer / 8.0f)) % 4;
        
        // Update pattern intensities based on audio
        patternIntensities[0] = bassLevel;
        patternIntensities[1] = midLevel;
        patternIntensities[2] = trebleLevel;
        patternIntensities[3] = currentVolume;
    }
    
    /**
     * Draws DJ Light patterns.
     */
    private void drawDjLightPatterns(Graphics2D g) {
        // Draw the classic DJ Light quarter-circle arc with rainbow stripes
        drawDjLightArc(g);
    }
    
    /**
     * Draws the classic DJ Light quarter-circle arc with rainbow stripes.
     */
    private void drawDjLightArc(Graphics2D g) {
        // Calculate arc parameters
        float arcRadius = maxRadius * 0.8f;
        float arcThickness = arcRadius * 0.3f; // Thickness of the arc
        int numStripes = 12; // Number of rainbow stripes
        
        // Calculate rotation based on audio
        float rotation = hue + (currentVolume * 180.0f); // Rotate based on volume
        
        // Draw quarter-circle arc with rainbow stripes
        for (int i = 0; i < numStripes; i++) {
            float stripeAngle = (360.0f / numStripes) * i;
            float startAngle = stripeAngle + rotation;
            float arcAngle = 360.0f / numStripes;
            
            // Calculate stripe color based on position and audio
            float hueValue = (stripeAngle + rotation) / 360.0f;
            float brightness = 0.3f + (currentVolume * 0.7f); // Brightness based on volume
            Color stripeColor = Color.getHSBColor(hueValue, 1.0f, brightness);
            
            g.setColor(stripeColor);
            
            // Draw the arc segment
            g.fillArc((int)(centerX - arcRadius), (int)(centerY - arcRadius), 
                     (int)(arcRadius * 2), (int)(arcRadius * 2), 
                     (int)startAngle, (int)arcAngle);
        }
        
        // Draw inner circle to create the arc effect
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(centerX - (arcRadius - arcThickness), 
                                   centerY - (arcRadius - arcThickness), 
                                   (arcRadius - arcThickness) * 2, 
                                   (arcRadius - arcThickness) * 2));
    }
    
    /**
     * Gets the name of the current pattern.
     */
    private String getPatternName(int pattern) {
        return "DJ Light Arc";
    }
    
    /**
     * Updates LED colors based on the current visual effects.
     */
    private void updateLedColors() {
        // Clear all LEDs first
        ledGrid.clearAllLeds();
        
        // Map visual effects to LED grid
        mapEffectsToLedGrid();
    }
    
    /**
     * Maps the DJ Light effects to the LED grid.
     */
    private void mapEffectsToLedGrid() {
        // Map the classic DJ Light quarter-circle arc
        mapDjLightArc();
    }
    
    /**
     * Maps the classic DJ Light quarter-circle arc to LEDs.
     */
    private void mapDjLightArc() {
        // Calculate arc parameters
        float arcRadius = maxRadius * 0.8f;
        float arcThickness = arcRadius * 0.3f; // Thickness of the arc
        int numStripes = 12; // Number of rainbow stripes
        
        // Calculate rotation based on audio
        float rotation = hue + (currentVolume * 180.0f); // Rotate based on volume
        
        // Map to all grids in the layout
        for (int gridIndex = 0; gridIndex < ledGrid.getGridCount(); gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            for (int y = 0; y < ledGrid.getGridSize(); y++) {
                for (int x = 0; x < ledGrid.getGridSize(); x++) {
                    // Calculate window coordinates for this grid
                    int windowX = gridConfig.getX() + x * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    int windowY = gridConfig.getY() + y * ledGrid.getPixelSize() + ledGrid.getPixelSize() / 2;
                    
                    Color ledColor = calculateDjLightColor(windowX, windowY, arcRadius, arcThickness, rotation, numStripes);
                    if (ledColor != null) {
                        ledGrid.setLedColor(gridIndex, x, y, ledColor);
                    }
                }
            }
        }
    }
    
    /**
     * Calculates the DJ Light color for a given pixel position.
     */
    private Color calculateDjLightColor(int windowX, int windowY, float arcRadius, float arcThickness, float rotation, int numStripes) {
        // Calculate distance from center
        float dx = windowX - centerX;
        float dy = windowY - centerY;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        
        // Check if pixel is within the arc
        if (distance < (arcRadius - arcThickness) || distance > arcRadius) {
            return null; // Outside the arc
        }
        
        // Calculate angle
        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;
        
        // Calculate which stripe this pixel belongs to
        float adjustedAngle = (angle + rotation) % 360;
        if (adjustedAngle < 0) adjustedAngle += 360;
        
        int stripeIndex = (int)((adjustedAngle / 360.0f) * numStripes) % numStripes;
        
        // Calculate stripe color
        float hueValue = (stripeIndex * 360.0f / numStripes) / 360.0f;
        float brightness = 0.3f + (currentVolume * 0.7f); // Brightness based on volume
        
        return Color.getHSBColor(hueValue, 1.0f, brightness);
    }
    
    
    /**
     * Cleanup method to close audio resources.
     */
    public void cleanup() {
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }
    
    @Override
    public String getName() {
        return "DJ Light Animation";
    }
    
    @Override
    public String getDescription() {
        return "Classic DJ lighting effects with strobe, color wash, frequency bars, pulse, and rainbow patterns";
    }
}
