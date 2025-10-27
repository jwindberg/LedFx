package com.marsraver.LedFx.animations;

import com.marsraver.LedFx.LedGrid;
import com.marsraver.LedFx.LedAnimation;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Video player animation that displays a video file on LED grids.
 * Uses FFmpeg via Runtime to extract frames from the video.
 */
@Log4j2
public class VideoPlayerAnimation implements LedAnimation {

    private LedGrid ledGrid;
    private int windowWidth, windowHeight;
    private String videoPath;
    private BufferedImage currentFrame;
    private AtomicBoolean isPlaying = new AtomicBoolean(true);
    private AtomicBoolean isExtracting = new AtomicBoolean(false);
    private AtomicLong lastFrameTime = new AtomicLong(0);
    private Thread videoThread;
    private Thread audioThread;
    private Process audioProcess;
    private File[] frameFiles;
    private int frameIndex = 0;
    private static final long FRAME_DELAY_MS = 33; // ~30 FPS for smooth playback
    private int frameCount = 0;
    private static final double VIDEO_SCALE = 0.9; // 90% scale - larger video display
    private int videoScrollPosition = 0; // Scroll position for navigation

    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.ledGrid = ledGrid;
        
        // Initialize with black frame
        this.currentFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = currentFrame.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
        
        // Default video path (can be overridden by calling setVideoPath)
        String defaultVideoPath = "/Users/jwindberg/Movies/Alice in Wonderland.m4v";
        if (new File(defaultVideoPath).exists()) {
            setVideoPath(defaultVideoPath);
        } else {
            log.debug("Default video not found: " + defaultVideoPath);
            log.debug("Please specify a video file using setVideoPath()");
        }
        
        log.debug("Video Player Animation initialized");
        log.debug("Animation: " + getName());
        log.debug("Description: " + getDescription());
        log.debug("WARNING: Video playback requires FFmpeg to be installed and in PATH");
    }

    /**
     * Sets the video file path.
     * 
     * @param videoPath The path to the video file
     */
    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
        log.debug("Loading video: " + videoPath);
        startVideoPlayback();
    }

    /**
     * Starts the video playback in a separate thread.
     */
    private void startVideoPlayback() {
        if (videoPath == null) {
            log.error("No video path specified");
            return;
        }

        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            log.error("Video file not found: " + videoPath);
            return;
        }

        isPlaying.set(true);
        
        // Start video playback thread
        videoThread = new Thread(() -> {
            try {
                playVideo();
                // Start audio playback after frames are ready
                startAudioPlayback();
            } catch (Exception e) {
                log.error("Error playing video: " + e.getMessage());
                e.printStackTrace();
            }
        });
        videoThread.setDaemon(true);
        videoThread.start();
    }
    
    /**
     * Starts audio playback in a separate process.
     */
    private void startAudioPlayback() {
        if (videoPath == null) {
            return;
        }
        
        // Wait a moment for frames to be ready
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return;
        }
        
        audioThread = new Thread(() -> {
            try {
                log.debug("Starting audio playback...");
                ProcessBuilder pb = new ProcessBuilder(
                    "ffplay",
                    "-nodisp",  // No video display
                    "-autoexit", // Exit when done
                    "-loglevel", "quiet", // Suppress output
                    videoPath
                );
                audioProcess = pb.start();
                audioProcess.waitFor();
            } catch (Exception e) {
                log.error("Error playing audio: " + e.getMessage());
            }
        });
        audioThread.setDaemon(true);
        audioThread.start();
    }

    /**
     * Plays the video using FFmpeg to extract frames.
     */
    private void playVideo() throws IOException, InterruptedException {
        isExtracting.set(true);
        
        // Create a temporary directory for frames
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "ledfx_video_frames");
        tempDir.mkdirs();
        
        // Check if frames already exist
        File[] existingFrames = tempDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (existingFrames != null && existingFrames.length > 0) {
            log.debug("Found " + existingFrames.length + " cached frames. Using cached frames.");
            this.frameFiles = existingFrames;
            java.util.Arrays.sort(this.frameFiles, (a, b) -> a.getName().compareTo(b.getName()));
            isExtracting.set(false);
            return;
        }
        
        // Clean up any existing frames
        File[] oldFrames = tempDir.listFiles();
        if (oldFrames != null) {
            for (File f : oldFrames) {
                f.delete();
            }
        }
        
        // Extract frames to temporary directory (scaled to 60% of window size)
        int scaledWidth = (int) (windowWidth * VIDEO_SCALE);
        int scaledHeight = (int) (windowHeight * VIDEO_SCALE);
        log.debug("Extracting frames from video (this may take a while)...");
        ProcessBuilder extractPb = new ProcessBuilder(
            "ffmpeg",
            "-i", videoPath,
            "-vf", "scale=" + scaledWidth + ":" + scaledHeight + ",fps=30",
            "-y",
            tempDir.getAbsolutePath() + "/frame_%05d.png"
        );
        
        extractPb.redirectErrorStream(true);
        Process extractProcess = extractPb.start();
        
        // Read and print FFmpeg output
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(extractProcess.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("FFmpeg: " + line);
        }
        
        int exitCode = extractProcess.waitFor();
        if (exitCode != 0) {
            log.error("FFmpeg extraction failed with exit code: " + exitCode);
            return;
        }
        
        // Get list of frame files
        this.frameFiles = tempDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (this.frameFiles == null || this.frameFiles.length == 0) {
            log.error("No frames extracted from video");
            return;
        }
        
        // Sort frames by filename
        java.util.Arrays.sort(this.frameFiles, (a, b) -> a.getName().compareTo(b.getName()));
        
        log.debug("Extracted " + this.frameFiles.length + " frames. Ready to play!");
        isExtracting.set(false);
    }

    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Play next frame if frames are ready and not extracting
        if (!isExtracting.get() && frameFiles != null && frameFiles.length > 0) {
            long elapsed = System.currentTimeMillis() - lastFrameTime.get();
            if (elapsed >= FRAME_DELAY_MS) {
                try {
                    File frameFile = frameFiles[frameIndex % frameFiles.length];
                    BufferedImage frame = ImageIO.read(frameFile);
                    if (frame != null) {
                        this.currentFrame = frame;
                        frameCount++;
                        frameIndex++;
                    }
                    lastFrameTime.set(System.currentTimeMillis());
                } catch (Exception e) {
                    log.error("Error reading frame: " + e.getMessage());
                }
            }
        }
        
        // Draw current video frame (centered at 60% scale)
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        if (currentFrame != null) {
            int scaledWidth = (int) (width * VIDEO_SCALE);
            int scaledHeight = (int) (height * VIDEO_SCALE);
            int offsetX = (width - scaledWidth) / 2;
            int offsetY = (height - scaledHeight) / 2;
            g.drawImage(currentFrame, offsetX, offsetY, scaledWidth, scaledHeight, null);
        }
        
        // Draw scrollbar
        if (frameFiles != null && frameFiles.length > 0) {
            drawScrollbar(g, width, height);
        }
        
        // Draw info text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Video Player - Press ESC to exit", 10, 20);
        g.drawString("Frame: " + frameCount, 10, 35);
        g.drawString("Video: " + (videoPath != null ? new File(videoPath).getName() : "None"), 10, 50);
        if (isExtracting.get()) {
            g.drawString("Extracting frames...", 10, 65);
        } else if (frameFiles != null) {
            g.drawString("Frames: " + frameFiles.length, 10, 65);
        }
        
        // Clear LEDs first
        ledGrid.clearAllLeds();
        
        // Map video frame to LED grids
        mapVideoToLedGrid();
        
        // Send to devices
        ledGrid.sendToDevices();
    }

    /**
     * Maps the current video frame to the LED grids.
     */
    private void mapVideoToLedGrid() {
        if (currentFrame == null) {
            return;
        }

        int gridSize = ledGrid.getGridSize();
        int pixelSize = ledGrid.getPixelSize();
        int gridCount = ledGrid.getGridCount();

        for (int gridIndex = 0; gridIndex < gridCount; gridIndex++) {
            var gridConfig = ledGrid.getGridConfig(gridIndex);
            
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    // Calculate window coordinates for this LED
                    int windowX = gridConfig.getX() + x * pixelSize + pixelSize / 2;
                    int windowY = gridConfig.getY() + y * pixelSize + pixelSize / 2;
                    
                    // Adjust for centered video (60% scale with border)
                    int scaledWidth = (int) (windowWidth * VIDEO_SCALE);
                    int scaledHeight = (int) (windowHeight * VIDEO_SCALE);
                    int offsetX = (windowWidth - scaledWidth) / 2;
                    int offsetY = (windowHeight - scaledHeight) / 2;
                    
                    // Map to video frame coordinates
                    int videoX = windowX - offsetX;
                    int videoY = windowY - offsetY;
                    
                    // Check if inside video bounds and get color
                    if (videoX >= 0 && videoX < scaledWidth && videoY >= 0 && videoY < scaledHeight) {
                        // Clamp coordinates to frame bounds to prevent out-of-bounds errors
                        int actualFrameWidth = currentFrame.getWidth();
                        int actualFrameHeight = currentFrame.getHeight();
                        int clampedX = Math.min(Math.max(0, videoX), actualFrameWidth - 1);
                        int clampedY = Math.min(Math.max(0, videoY), actualFrameHeight - 1);
                        
                        Color ledColor = new Color(currentFrame.getRGB(clampedX, clampedY));
                        
                        // Transform LED coordinates (90-degree rotation)
                        int transformedX = y;
                        int transformedY = x;
                        
                        ledGrid.setLedColor(gridIndex, transformedX, transformedY, ledColor);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Video Player";
    }

    @Override
    public String getDescription() {
        return "Plays video files on LED grids (requires FFmpeg)";
    }

    /**
     * Draws the video scrollbar for seeking.
     */
    private void drawScrollbar(Graphics2D g, int width, int height) {
        if (frameFiles == null || frameFiles.length == 0) {
            return;
        }
        
        int scrollbarWidth = width - 40;
        int scrollbarX = 20;
        int scrollbarY = height - 40;
        int scrollbarHeight = 15;
        
        // Draw scrollbar background
        g.setColor(Color.DARK_GRAY);
        g.fillRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight);
        
        // Calculate thumb position based on current frame
        int thumbWidth = Math.max(10, scrollbarWidth / frameFiles.length);
        int maxThumbPosition = scrollbarWidth - thumbWidth;
        int thumbPosition = (int) ((frameIndex % frameFiles.length) / (double) frameFiles.length * maxThumbPosition);
        
        // Draw scrollbar thumb
        g.setColor(Color.WHITE);
        g.fillRect(scrollbarX + thumbPosition, scrollbarY, thumbWidth, scrollbarHeight);
        
        // Draw current position text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        String positionText = String.format("%d / %d", frameIndex % frameFiles.length, frameFiles.length);
        g.drawString(positionText, scrollbarX + scrollbarWidth + 10, scrollbarY + 12);
        
        // Draw navigation instructions
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.setColor(Color.GRAY);
        g.drawString("Click on scrollbar to seek | ← → to rewind/fast-forward", 10, height - 10);
    }

    /**
     * Seeks to a specific frame position.
     * 
     * @param position Position from 0.0 to 1.0
     */
    public void seekTo(double position) {
        if (frameFiles != null && frameFiles.length > 0) {
            frameIndex = (int) (position * frameFiles.length);
            frameIndex = Math.max(0, Math.min(frameIndex, frameFiles.length - 1));
        }
    }

    /**
     * Stops the video playback.
     */
    public void stop() {
        isPlaying.set(false);
        if (videoThread != null && videoThread.isAlive()) {
            videoThread.interrupt();
        }
        // Stop audio playback
        if (audioProcess != null && audioProcess.isAlive()) {
            audioProcess.destroyForcibly();
        }
        if (audioThread != null && audioThread.isAlive()) {
            audioThread.interrupt();
        }
    }
}
