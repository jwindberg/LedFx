package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.math.sqrt

/**
 * Abstract base class for RMS-based audio-reactive animations.
 * Provides RMS (Root Mean Square) processing for overall loudness and per-band levels.
 */
abstract class RMSAnimation : AudioReactiveAnimation() {
    // RMS configuration
    protected abstract val bandCount: Int

    // Audio levels (written from audio thread, read from render thread)
    @Volatile
    protected var masterLevel = 0f // Overall RMS level (0..1)

    // Per-band RMS levels (initialized in init after bandCount is known)
    protected lateinit var bandLevels: FloatArray
    protected var arraysInitialized = false

    // Synchronization for thread-safe access
    protected val levelLock = Any()

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        // Initialize arrays now that bandCount is available
        bandLevels = FloatArray(bandCount)
        arraysInitialized = true
        super.init(width, height, ledGrid)
    }

    override fun processAudioData(samples: FloatArray, sampleCount: Int) {
        // Safety check: ensure arrays are initialized
        if (!arraysInitialized) return
        // Compute overall RMS (master level)
        var sumSquares = 0.0
        val validCount = minOf(sampleCount, samples.size)
        for (i in 0 until validCount) {
            val sample = samples[i]
            sumSquares += (sample * sample).toDouble()
        }
        val rms = if (validCount > 0) sqrt(sumSquares / validCount.toDouble()).toFloat() else 0f
        val normalizedRms = rms.coerceIn(0f, 1f)

        // Compute per-band RMS levels
        val samplesPerBand = (validCount / bandCount).coerceAtLeast(1)
        val bandRms = FloatArray(bandCount)

        for (b in 0 until bandCount) {
            val start = b * samplesPerBand
            val end = if (b == bandCount - 1) {
                validCount // Last band gets remaining samples
            } else {
                (b + 1) * samplesPerBand
            }

            var bandSumSquares = 0.0
            var bandSampleCount = 0
            for (i in start until end) {
                val sample = samples[i]
                bandSumSquares += (sample * sample).toDouble()
                bandSampleCount++
            }

            val bandRmsValue = if (bandSampleCount > 0) {
                sqrt(bandSumSquares / bandSampleCount.toDouble()).toFloat()
            } else {
                0f
            }
            bandRms[b] = bandRmsValue.coerceIn(0f, 1f)
        }

        // Smooth and update levels (thread-safe)
        synchronized(levelLock) {
            // Smooth master level
            masterLevel = if (normalizedRms > masterLevel) {
                // Fast attack
                masterLevel + (normalizedRms - masterLevel) * 0.8f
            } else {
                // Slow decay
                masterLevel + (normalizedRms - masterLevel) * 0.3f
            }

            // Smooth band levels
            for (b in 0 until bandCount) {
                val target = bandRms[b]
                val current = bandLevels[b]
                bandLevels[b] = if (target > current) {
                    // Fast attack
                    current + (target - current) * 0.8f
                } else {
                    // Slow decay
                    current + (target - current) * 0.3f
                }
            }
        }
    }

    companion object {
        private val log: Logger = LogManager.getLogger(RMSAnimation::class.java)
    }
}

