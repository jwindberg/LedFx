package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

/**
 * Video player animation that displays a video file on LED grids.
 * Uses FFmpeg via Runtime to extract frames from the video.
 */
class VideoPlayerAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var videoPath: String? = null
    private var currentFrame: BufferedImage? = null
    private val isPlaying = AtomicBoolean(true)
    private val isExtracting = AtomicBoolean(false)
    private val lastFrameTime = AtomicLong(0)
    private var videoThread: Thread? = null
    private var audioThread: Thread? = null
    private var audioProcess: Process? = null
    private var frameFiles: Array<File>? = null
    private var frameIndex = 0
    private var frameCount = 0
    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.windowWidth = width
        this.windowHeight = height
        this.ledGrid = ledGrid


        // Initialize with black frame
        this.currentFrame = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = currentFrame!!.createGraphics()
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)
        g.dispose()


        // Default video path (can be overridden by calling setVideoPath)
        val defaultVideoPath = "/Users/jwindberg/Movies/Alice in Wonderland.m4v"
        if (File(defaultVideoPath).exists()) {
            setVideoPath(defaultVideoPath)
        } else {
            log.debug("Default video not found: {}", defaultVideoPath)
            log.debug("Please specify a video file using setVideoPath()")
        }

        log.debug("Video Player Animation initialized")
        log.debug("Animation: {}", getName())
        log.debug("Description: {}", getDescription())
        log.debug("WARNING: Video playback requires FFmpeg to be installed and in PATH")
    }

    /**
     * Sets the video file path.
     *
     * @param videoPath The path to the video file
     */
    fun setVideoPath(videoPath: String?) {
        this.videoPath = videoPath
        log.debug("Loading video: {}", videoPath)
        startVideoPlayback()
    }

    /**
     * Starts the video playback in a separate thread.
     */
    private fun startVideoPlayback() {
        if (videoPath == null) {
            log.error("No video path specified")
            return
        }

        val videoFile = File(videoPath)
        if (!videoFile.exists()) {
            log.error("Video file not found: {}", videoPath)
            return
        }

        isPlaying.set(true)


        // Start video playback thread
        videoThread = Thread(Runnable {
            try {
                playVideo()
                // Start audio playback after frames are ready
                startAudioPlayback()
            } catch (e: Exception) {
                log.error("Error playing video: {}", e.message, e)
                e.printStackTrace()
            }
        })
        videoThread!!.setDaemon(true)
        videoThread!!.start()
    }

    /**
     * Starts audio playback in a separate process at the current frame position.
     * Used initially and whenever the user seeks in the video.
     */
    private fun startAudioPlayback() {
        if (videoPath == null) {
            return
        }

        // Wait a moment for frames to be ready
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            return
        }

        restartAudioAtFrame(frameIndex)
    }

    /**
     * Stops any existing audio process and restarts it at the given frame index.
     */
    @Synchronized
    private fun restartAudioAtFrame(frameIndexForAudio: Int) {
        // Stop any existing audio playback
        if (audioProcess != null && audioProcess!!.isAlive()) {
            audioProcess!!.destroyForcibly()
            audioProcess = null
        }

        // Compute time offset from frame index
        val seconds: Double = frameIndexForAudio / VIDEO_FPS

        audioThread = Thread(Runnable {
            try {
                log.debug("Starting audio playback at ~{} seconds (frame {})", seconds, frameIndexForAudio)
                val pb = ProcessBuilder(
                    "ffplay",
                    "-nodisp",  // No video display
                    "-autoexit",  // Exit when done
                    "-loglevel", "quiet",  // Suppress output
                    "-ss", seconds.toString(),
                    videoPath
                )
                audioProcess = pb.start()
                audioProcess!!.waitFor()
            } catch (e: Exception) {
                log.error("Error playing audio: {}", e.message, e)
            }
        })
        audioThread!!.setDaemon(true)
        audioThread!!.start()
    }

    /**
     * Plays the video using FFmpeg to extract frames.
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun playVideo() {
        isExtracting.set(true)


        // Create a temporary directory for frames
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ledfx_video_frames")
        tempDir.mkdirs()


        // Check if frames already exist
        val existingFrames = tempDir.listFiles(FilenameFilter { dir: File?, name: String? -> name!!.endsWith(".png") })
        if (existingFrames != null && existingFrames.size > 0) {
            log.debug("Found {} cached frames. Using cached frames.", existingFrames.size)
            this.frameFiles = existingFrames
            Arrays.sort<File?>(
                this.frameFiles,
                Comparator { a: File?, b: File? -> a!!.getName().compareTo(b!!.getName()) })
            isExtracting.set(false)
            return
        }


        // Clean up any existing frames
        val oldFrames = tempDir.listFiles()
        if (oldFrames != null) {
            for (f in oldFrames) {
                f.delete()
            }
        }


        // Extract frames to temporary directory (scaled to 60% of window size)
        val scaledWidth = (windowWidth * VIDEO_SCALE).toInt()
        val scaledHeight = (windowHeight * VIDEO_SCALE).toInt()
        log.debug("Extracting frames from video (this may take a while)...")
        val extractPb = ProcessBuilder(
            "ffmpeg",
            "-i", videoPath,
            "-vf", "scale=" + scaledWidth + ":" + scaledHeight + ",fps=30",
            "-y",
            tempDir.getAbsolutePath() + "/frame_%05d.png"
        )

        extractPb.redirectErrorStream(true)
        val extractProcess = extractPb.start()


        // Read and print FFmpeg output
        val reader = BufferedReader(
            InputStreamReader(extractProcess.getInputStream())
        )
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            log.debug("FFmpeg: {}", line)
        }

        val exitCode = extractProcess.waitFor()
        if (exitCode != 0) {
            log.error("FFmpeg extraction failed with exit code: {}", exitCode)
            return
        }


        // Get list of frame files
        this.frameFiles = tempDir.listFiles(FilenameFilter { _: File?, name: String? -> name!!.endsWith(".png") })
        if (this.frameFiles == null || this.frameFiles!!.isEmpty()) {
            log.error("No frames extracted from video")
            return
        }


        // Sort frames by filename
        Arrays.sort(this.frameFiles, Comparator { a: File, b: File -> a.name.compareTo(b.name) })

        log.debug("Extracted {} frames. Ready to play!", this.frameFiles!!.size)
        isExtracting.set(false)
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Play next frame if frames are ready and not extracting
        if (!isExtracting.get() && frameFiles != null && frameFiles!!.size > 0) {
            val elapsed = System.currentTimeMillis() - lastFrameTime.get()
            if (elapsed >= FRAME_DELAY_MS) {
                try {
                    val frameFile = frameFiles!![frameIndex % frameFiles!!.size]
                    val frame = ImageIO.read(frameFile)
                    if (frame != null) {
                        this.currentFrame = frame
                        frameCount++
                        frameIndex++
                    }
                    lastFrameTime.set(System.currentTimeMillis())
                } catch (e: Exception) {
                    log.error("Error reading frame: {}", e.message, e)
                }
            }
        }


        // Draw current video frame (centered at 60% scale)
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)

        if (currentFrame != null) {
            val scaledWidth = (width * VIDEO_SCALE).toInt()
            val scaledHeight = (height * VIDEO_SCALE).toInt()
            val offsetX = (width - scaledWidth) / 2
            val offsetY = (height - scaledHeight) / 2
            g.drawImage(currentFrame, offsetX, offsetY, scaledWidth, scaledHeight, null)
        }


        // Draw scrollbar
        if (frameFiles != null && frameFiles!!.isNotEmpty()) {
            drawScrollbar(g, width, height)
        }


        // Draw info text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 12))
        g.drawString("Video Player - Press ESC to exit", 10, 20)
        g.drawString("Frame: " + frameCount, 10, 35)
        g.drawString("Video: " + (if (videoPath != null) File(videoPath!!).name else "None"), 10, 50)
        if (isExtracting.get()) {
            g.drawString("Extracting frames...", 10, 65)
        } else if (frameFiles != null) {
            g.drawString("Frames: " + frameFiles!!.size, 10, 65)
        }


        // Clear LEDs first
        ledGrid.clearAllLeds()


        // Map video frame to LED grids
        mapVideoToLedGrid()


        // Send to devices
        ledGrid.sendToDevices()
    }

    /**
     * Maps the current video frame to the LED grids.
     */
    private fun mapVideoToLedGrid() {
        val grid = ledGrid ?: return
        if (currentFrame == null) {
            return
        }

        val gridSize = grid.gridSize
        val pixelSize = grid.pixelSize
        val gridCount = grid.gridCount

        for (gridIndex in 0 until gridCount) {
            val gridConfig = grid.getGridConfig(gridIndex) ?: continue

            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    // Calculate window coordinates for this LED
                    val windowX = gridConfig.x + x * pixelSize + pixelSize / 2
                    val windowY = gridConfig.y + y * pixelSize + pixelSize / 2


                    // Adjust for centered video (60% scale with border)
                    val scaledWidth = (windowWidth * VIDEO_SCALE).toInt()
                    val scaledHeight = (windowHeight * VIDEO_SCALE).toInt()
                    val offsetX = (windowWidth - scaledWidth) / 2
                    val offsetY = (windowHeight - scaledHeight) / 2


                    // Map to video frame coordinates
                    val videoX = windowX - offsetX
                    val videoY = windowY - offsetY


                    // Check if inside video bounds and get color
                    if (videoX >= 0 && videoX < scaledWidth && videoY >= 0 && videoY < scaledHeight) {
                        // Clamp coordinates to frame bounds to prevent out-of-bounds errors
                        val actualFrameWidth = currentFrame!!.width
                        val actualFrameHeight = currentFrame!!.height
                        val clampedX = min(max(0, videoX), actualFrameWidth - 1)
                        val clampedY = min(max(0, videoY), actualFrameHeight - 1)

                        val ledColor = Color(currentFrame!!.getRGB(clampedX, clampedY))


                        // Use standard logical LED coordinates (x = left->right, y = top->bottom)
                        // so mapping is consistent with other animations and LedGrid packing.
                        grid.setLedColor(gridIndex, x, y, ledColor)
                    }
                }
            }
        }
    }

    override fun getName(): String {
        return "Video Player"
    }

    override fun getDescription(): String {
        return "Plays video files on LED grids (requires FFmpeg)"
    }

    /**
     * Draws the video scrollbar for seeking.
     */
    private fun drawScrollbar(g: Graphics2D, width: Int, height: Int) {
        if (frameFiles == null || frameFiles!!.isEmpty()) {
            return
        }

        val scrollbarWidth = width - 40
        val scrollbarX = 20
        val scrollbarY = height - 40
        val scrollbarHeight = 15


        // Draw scrollbar background
        g.setColor(Color.DARK_GRAY)
        g.fillRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)


        // Calculate thumb position based on current frame
        val thumbWidth = max(10, scrollbarWidth / frameFiles!!.size)
        val maxThumbPosition = scrollbarWidth - thumbWidth
        val thumbPosition = ((frameIndex % frameFiles!!.size) / frameFiles!!.size.toDouble() * maxThumbPosition).toInt()


        // Draw scrollbar thumb
        g.setColor(Color.WHITE)
        g.fillRect(scrollbarX + thumbPosition, scrollbarY, thumbWidth, scrollbarHeight)


        // Draw current position text
        g.setColor(Color.WHITE)
        g.setFont(Font("Arial", Font.PLAIN, 10))
        val positionText = String.format("%d / %d", frameIndex % frameFiles!!.size, frameFiles!!.size)
        g.drawString(positionText, scrollbarX + scrollbarWidth + 10, scrollbarY + 12)


        // Draw navigation instructions
        g.setFont(Font("Arial", Font.PLAIN, 10))
        g.setColor(Color.GRAY)
        g.drawString("Click on scrollbar to seek | ← → to rewind/fast-forward", 10, height - 10)
    }

    /**
     * Seeks to a specific frame position.
     *
     * @param position Position from 0.0 to 1.0
     */
    fun seekTo(position: Double) {
        if (frameFiles != null && frameFiles!!.isNotEmpty()) {
            frameIndex = (position * frameFiles!!.size).toInt()
            frameIndex = max(0, min(frameIndex, frameFiles!!.size - 1))

            // Restart audio roughly at the new position to keep A/V in sync
            restartAudioAtFrame(frameIndex)
        }
    }

    /**
     * Stops the video playback.
     */
    override fun stop() {
        isPlaying.set(false)
        if (videoThread != null && videoThread!!.isAlive()) {
            videoThread!!.interrupt()
        }
        // Stop audio playback
        if (audioProcess != null && audioProcess!!.isAlive()) {
            audioProcess!!.destroyForcibly()
        }
        if (audioThread != null && audioThread!!.isAlive()) {
            audioThread!!.interrupt()
        }
    }

    companion object {
        private const val FRAME_DELAY_MS: Long = 33 // ~30 FPS for smooth playback
        private const val VIDEO_SCALE = 0.9 // 90% scale - larger video display
        private const val VIDEO_FPS = 30.0 // Must match FFmpeg fps used for frame extraction
        private val log: Logger = LogManager.getLogger(VideoPlayerAnimation::class.java)
    }
}
