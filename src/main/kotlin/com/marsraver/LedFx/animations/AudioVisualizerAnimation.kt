package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlin.math.*

/**
 * AudioVisualizer-style animation, inspired by the original Processing/Minim sketch.
 *
 * This implementation uses live audio from the default input device (microphone / line‑in),
 * performs a simple FFT in a background thread, and renders two concentric "atomic sprocket"
 * rings of boxes that swirl and react to the real spectrum. The same data is mapped to the LED grid.
 */
class AudioVisualizerAnimation : LedAnimation {
    private var ledGrid: LedGrid? = null
    private var windowWidth: Int = 0
    private var windowHeight: Int = 0

    // FFT / spectrum configuration
    private val bandCount = 64
    private val fftSize = 1024  // must be power of two

    // Position / phase per band (driven by spectrum)
    private val x = FloatArray(bandCount)
    private val y = FloatArray(bandCount)
    private val angle = FloatArray(bandCount)

    // Live spectrum (smoothed magnitudes 0..1)
    private val spectrum = FloatArray(bandCount)
    private val spectrumSmoothed = FloatArray(bandCount)

    // Audio capture state
    @Volatile
    private var running = false
    private var audioThread: Thread? = null
    private var line: TargetDataLine? = null

    private var startTimeMs: Long = 0

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.ledGrid = ledGrid
        this.windowWidth = width
        this.windowHeight = height
        this.startTimeMs = System.currentTimeMillis()

        // Initialize positions and angles
        for (i in 0 until bandCount) {
            x[i] = 0f
            y[i] = 0f
            angle[i] = (i / bandCount.toFloat()) * (2f * Math.PI.toFloat())
        }

        startAudioCapture()
        log.debug("AudioVisualizer Animation initialized with real audio input")
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        // Smooth background with light trails
        g.color = Color(0, 0, 0, 40)
        g.fillRect(0, 0, width, height)

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val now = System.currentTimeMillis()
        val t = (now - startTimeMs) / 1000.0f

        val centerX = width / 2.0f
        val centerY = height / 2.0f

        // First ring: bright colored boxes
        g.translate(centerX.toDouble(), centerY.toDouble())
        doubleAtomicRing(g, t, inner = false)
        g.translate(-centerX.toDouble(), -centerY.toDouble())

        // Second ring: inverse colors further out
        g.translate(centerX.toDouble(), centerY.toDouble())
        doubleAtomicRing(g, t, inner = true)
        g.translate(-centerX.toDouble(), -centerY.toDouble())

        // Map to LEDs from the spectrum‑driven positions and colors
        mapToLeds(ledGrid, t)

        // Info text
        g.color = Color.WHITE
        g.drawString("AudioVisualizer (live audio) - Press ESC to exit", 10, 20)
    }

    /**
     * Draws a single "atomic sprocket" style ring of boxes.
     */
    private fun doubleAtomicRing(g: Graphics2D, t: Float, inner: Boolean) {
        // Stronger movement and radius for higher activity
        val radiusBase = if (inner) 0.45f else 0.25f
        val radiusMod = if (inner) 1.20f else 0.80f
        val spreadFactor = if (inner) 3.0f else 2.0f

        for (i in 0 until bandCount) {
            // Emphasize magnitudes heavily for strong visual response
            val rawMag = spectrumSmoothed[i].coerceIn(0f, 1f)
            val bandMag = (rawMag * 3.0f).coerceAtMost(1.0f)
            val bandFreqNorm = (i.toFloat() / (bandCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)

            // Integrate motion
            x[i] += bandFreqNorm * spreadFactor
            y[i] += bandMag * spreadFactor
            angle[i] += bandFreqNorm * if (inner) 0.0025f else 0.01f

            val ringRadius = radiusBase + radiusMod * bandMag
            val px = (ringRadius * cos(angle[i].toDouble())).toFloat()
            val py = (ringRadius * sin(angle[i].toDouble())).toFloat()

            // Color scheme inspired by the original code
            val color = if (!inner) {
                Color(
                    (bandFreqNorm * 510f).toInt().coerceIn(0, 255),
                    (bandMag * 180f).toInt().coerceIn(0, 255),
                    (bandMag * 720f).toInt().coerceIn(0, 255)
                )
            } else {
                Color(
                    (bandFreqNorm * 120f).toInt().coerceIn(0, 255),
                    (255f - bandFreqNorm * 510f).toInt().coerceIn(0, 255),
                    (255f - bandMag * 720f).toInt().coerceIn(0, 255)
                )
            }

            g.color = color

            val boxSize = 4.0 + bandMag * 28.0 + bandFreqNorm * 12.0
            val drawX = (px * windowWidth / 2.0).toInt()
            val drawY = (py * windowHeight / 2.0).toInt()

            g.fillRect(drawX - boxSize.toInt() / 2, drawY - boxSize.toInt() / 2, boxSize.toInt(), boxSize.toInt())
        }
    }

    /**
     * Maps the spectrum‑driven ring positions to the LED grid.
     */
    private fun mapToLeds(ledGrid: LedGrid, t: Float) {
        ledGrid.clearAllLeds()

        val gridCount = ledGrid.gridCount
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize

        val centerX = windowWidth / 2.0f
        val centerY = windowHeight / 2.0f

        for (i in 0 until bandCount) {
            val rawMag = spectrumSmoothed[i].coerceIn(0f, 1f)
            val bandMag = (rawMag * 3.0f).coerceAtMost(1.0f)
            val bandFreqNorm = (i.toFloat() / (bandCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)

            // Use the outer ring positions for LED mapping
            val radiusBase = 0.18f
            val radiusMod = 0.30f
            val ringRadius = radiusBase + radiusMod * bandMag
            val px = (ringRadius * cos(angle[i].toDouble())).toFloat()
            val py = (ringRadius * sin(angle[i].toDouble())).toFloat()

            val worldX = centerX + px * windowWidth / 2.0f
            val worldY = centerY + py * windowHeight / 2.0f

            val color = Color(
                (bandFreqNorm * 510f).toInt().coerceIn(0, 255),
                (bandMag * 255f).toInt().coerceIn(0, 255),
                (255f - bandMag * 255f).toInt().coerceIn(0, 255)
            )

            for (gridIndex in 0 until gridCount) {
                val cfg = ledGrid.getGridConfig(gridIndex) ?: continue

                val relX = (worldX - cfg.x).toInt()
                val relY = (worldY - cfg.y).toInt()

                val ledX = relX / pixelSize
                val ledY = relY / pixelSize

                if (ledX in 0 until gridSize && ledY in 0 until gridSize) {
                    ledGrid.setLedColor(gridIndex, ledX, ledY, color)
                }
            }
        }

        ledGrid.sendToDevices()
    }

    /**
     * Starts the audio capture thread and FFT processing.
     */
    private fun startAudioCapture() {
        try {
            val format = AudioFormat(44100f, 16, 1, true, false)
            val info = javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, format)
            val targetLine = AudioSystem.getLine(info) as TargetDataLine
            targetLine.open(format, fftSize * 2)
            targetLine.start()

            line = targetLine
            running = true

            audioThread = thread(start = true, isDaemon = true, name = "AudioVisualizer-FFT") {
                val byteBuffer = ByteArray(fftSize * 2) // 16‑bit mono -> 2 bytes per sample
                val samples = FloatArray(fftSize)
                val real = FloatArray(fftSize)
                val imag = FloatArray(fftSize)

                while (running) {
                    val read = targetLine.read(byteBuffer, 0, byteBuffer.size)
                    if (read <= 0) continue

                    // Convert bytes to normalized PCM samples (-1..1)
                    var idx = 0
                    for (i in 0 until fftSize) {
                        if (idx + 1 >= read) break
                        val lo = byteBuffer[idx].toInt() and 0xFF
                        val hi = byteBuffer[idx + 1].toInt()
                        val sample = ((hi shl 8) or lo) / 32768.0f
                        samples[i] = sample
                        idx += 2
                    }

                    // Copy samples into real part and clear imag
                    for (i in 0 until fftSize) {
                        real[i] = samples[i]
                        imag[i] = 0f
                    }

                    // Apply a simple Hann window
                    for (i in 0 until fftSize) {
                        val w = 0.5f * (1f - cos(2f * Math.PI.toFloat() * i / (fftSize - 1)))
                        real[i] *= w
                    }

                    // Run in‑place FFT
                    fftInPlace(real, imag)

                    // Compute magnitude spectrum (only 0..N/2 bins are unique)
                    val binCount = fftSize / 2
                    val mags = FloatArray(binCount)
                    for (i in 0 until binCount) {
                        mags[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
                    }

                    // Normalize and map into bandCount bands (log-ish grouping would be nicer,
                    // but linear works fine for now).
                    var maxMag = mags.maxOrNull() ?: 1e-6f
                    if (maxMag < 1e-6f) maxMag = 1e-6f

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

                    // Smooth into spectrumSmoothed for stable but reactive visuals
                    for (b in 0 until bandCount) {
                        val target = spectrum[b]
                        // Attack very fast, decay moderately fast for more visible motion
                        val current = spectrumSmoothed[b]
                        spectrumSmoothed[b] = if (target > current) {
                            current + (target - current) * 0.9f
                        } else {
                            current + (target - current) * 0.4f
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Failed to start audio capture for AudioVisualizer", e)
        }
    }

    /**
     * Simple in-place radix-2 Cooley–Tukey FFT on separate real/imag arrays.
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

        // Cooley–Tukey
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

    override fun stop() {
        running = false
        try {
            line?.stop()
            line?.close()
        } catch (_: Exception) {
        }
    }

    override fun getName(): String = "Audio Visualizer"

    override fun getDescription(): String =
        "Live audio-visualizer rings driven by FFT of the default audio input"

    companion object {
        private val log: Logger = LogManager.getLogger(AudioVisualizerAnimation::class.java)
    }
}



