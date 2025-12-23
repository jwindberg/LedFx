package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.math.*

/**
 * Abstract base class for FFT-based audio-reactive animations.
 * Provides FFT processing and spectrum smoothing that FFT-based animations share.
 */
abstract class FFTAnimation : AudioReactiveAnimation() {
    // FFT / spectrum configuration
    protected abstract val bandCount: Int
    protected abstract val fftSize: Int // must be power of two

    // Live spectrum (initialized in init after bandCount is known)
    protected lateinit var spectrum: FloatArray
    protected lateinit var spectrumSmoothed: FloatArray
    protected var arraysInitialized = false

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        // Initialize arrays now that bandCount is available
        spectrum = FloatArray(bandCount)
        spectrumSmoothed = FloatArray(bandCount)
        arraysInitialized = true
        super.init(width, height, ledGrid)
    }

    protected override fun getBufferSize(): Int = fftSize * 2

    override fun processAudioData(samples: FloatArray, sampleCount: Int) {
        // Safety check: ensure arrays are initialized
        if (!arraysInitialized) return

        // Copy samples into FFT arrays
        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)

        // Copy available samples (may be less than fftSize)
        val copyCount = min(sampleCount, fftSize)
        for (i in 0 until copyCount) {
            real[i] = samples[i]
            imag[i] = 0f
        }
        // Zero-pad if we have fewer samples than fftSize
        for (i in copyCount until fftSize) {
            real[i] = 0f
            imag[i] = 0f
        }

        // Apply Hann window
        for (i in 0 until fftSize) {
            val w = 0.5f * (1f - cos(2f * Math.PI.toFloat() * i / (fftSize - 1)))
            real[i] *= w
        }

        // Run FFT
        fftInPlace(real, imag)

        // Compute magnitude spectrum (only 0..N/2 bins are unique)
        val binCount = fftSize / 2
        val mags = FloatArray(binCount)
        for (i in 0 until binCount) {
            mags[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        // Normalize to 0..1
        var maxMag = mags.maxOrNull() ?: 1e-6f
        if (maxMag < 1e-6f) maxMag = 1e-6f

        // Map to bandCount bands
        for (b in 0 until bandCount) {
            val start = (b * binCount / bandCount)
            val end = ((b + 1) * binCount / bandCount).coerceAtMost(binCount)
            var sum = 0f
            var count = 0
            for (i in start until end) {
                sum += mags[i]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            spectrum[b] = (avg / maxMag).coerceIn(0f, 1f)
        }

        // Smooth spectrum
        smoothSpectrum()
    }

    /**
     * Smooths the spectrum to reduce jitter.
     * Subclasses can override to customize smoothing behavior.
     */
    protected open fun smoothSpectrum() {
        // Safety check: ensure arrays are initialized
        if (!arraysInitialized) return

        for (b in 0 until bandCount) {
            val target = spectrum[b]
            val current = spectrumSmoothed[b]
            spectrumSmoothed[b] = if (target > current) {
                // Attack: fast response to increases
                current + (target - current) * 0.8f
            } else {
                // Decay: slower response to decreases
                current + (target - current) * 0.3f
            }
        }
    }

    /**
     * In-place FFT implementation (Cooley-Tukey).
     */
    private fun fftInPlace(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tmpR = real[i]
                val tmpI = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tmpR
                imag[j] = tmpI
            }
        }

        // Cooley–Tukey FFT
        var len = 2
        while (len <= n) {
            val ang = (-2.0 * Math.PI / len)
            val wlenCos = cos(ang).toFloat()
            val wlenSin = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wCos = 1f
                var wSin = 0f
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + len / 2]
                    val vI = imag[i + k + len / 2]

                    val tR = vR * wCos - vI * wSin
                    val tI = vR * wSin + vI * wCos

                    real[i + k] = uR + tR
                    imag[i + k] = uI + tI
                    real[i + k + len / 2] = uR - tR
                    imag[i + k + len / 2] = uI - tI

                    val nextCos = wCos * wlenCos - wSin * wlenSin
                    val nextSin = wCos * wlenSin + wSin * wlenCos
                    wCos = nextCos
                    wSin = nextSin
                }
                i += len
            }
            len = len shl 1
        }
    }

    companion object {
        private val log: Logger = LogManager.getLogger(FFTAnimation::class.java)
    }
}

