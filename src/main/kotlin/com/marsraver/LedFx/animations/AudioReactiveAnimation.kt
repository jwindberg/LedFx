package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread

/**
 * Abstract base class for audio-reactive animations.
 * Provides common audio capture infrastructure that all audio-reactive animations share.
 */
abstract class AudioReactiveAnimation : LedAnimation {
    protected var ledGrid: LedGrid? = null
    protected var windowWidth: Int = 0
    protected var windowHeight: Int = 0

    // Audio capture state
    @Volatile
    protected var running = false
    protected var line: TargetDataLine? = null
    protected var audioThread: Thread? = null

    // Audio format configuration
    protected val sampleRate = 44100f
    protected val sampleSizeInBits = 16
    protected val channels = 1
    protected val signed = true
    protected val bigEndian = false

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height

        startAudioCapture()
        log.debug("${getName()} initialized with live audio")
    }

    override fun stop() {
        running = false
        try {
            line?.stop()
            line?.close()
        } catch (_: Exception) {
        }
    }

    /**
     * Starts the audio capture thread.
     * Subclasses should call this and implement processAudioData() to handle the captured audio.
     */
    protected fun startAudioCapture() {
        try {
            val format = AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
            val info = javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, format)
            val targetLine = AudioSystem.getLine(info) as TargetDataLine
            val bufferSize = getBufferSize()
            targetLine.open(format, bufferSize)
            targetLine.start()

            line = targetLine
            running = true

            audioThread = thread(start = true, isDaemon = true, name = "${getName()}-Audio") {
                val byteBuffer = ByteArray(bufferSize)
                val samples = FloatArray(bufferSize / 2) // 16-bit mono = 2 bytes per sample

                while (running) {
                    val read = targetLine.read(byteBuffer, 0, byteBuffer.size)
                    if (read <= 0) continue

                    // Convert bytes to normalized PCM samples (-1..1)
                    var idx = 0
                    for (i in 0 until samples.size) {
                        if (idx + 1 >= read) break
                        val lo = byteBuffer[idx].toInt() and 0xFF
                        val hi = byteBuffer[idx + 1].toInt()
                        val sample = ((hi shl 8) or lo) / 32768.0f
                        samples[i] = sample
                        idx += 2
                    }

                    // Process the audio data (implemented by subclasses)
                    processAudioData(samples, read / 2)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to start audio capture for ${getName()}", e)
        }
    }

    /**
     * Returns the buffer size for audio capture.
     * Subclasses can override to use different buffer sizes.
     */
    protected open fun getBufferSize(): Int = 4096

    /**
     * Processes the captured audio data.
     * Subclasses must implement this to handle FFT, RMS, or other audio processing.
     *
     * @param samples The audio samples in normalized PCM format (-1..1)
     * @param sampleCount The number of valid samples in the array
     */
    protected abstract fun processAudioData(samples: FloatArray, sampleCount: Int)

    companion object {
        protected val log: Logger = LogManager.getLogger(AudioReactiveAnimation::class.java)
    }
}

