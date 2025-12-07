package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Akemi animation – renders a stylised character with audio‑reactive elements and side volume bar.
 *
 * Ported from the original WLED/Processing AkemiAnimation to the LedFx framework.
 * Uses live audio from the default input device with RMS-based volume detection.
 */
class AkemiAnimation : RMSAnimation() {
    // RMS configuration - only need 1 band for overall volume
    override val bandCount = 1

    // Offscreen buffer for drawing and LED sampling
    private var offscreen: BufferedImage? = null

    // Animation parameters
    private var colorSpeed: Int = 128
    private var intensity: Int = 128

    // Adaptive volume normalization - keep history for auto-ranging
    private data class VolumeSample(val timestamp: Long, val level: Float)
    private val volumeHistory = mutableListOf<VolumeSample>()
    private val historyWindowMs = 3000L // Keep last 3 seconds of samples
    private val volumeLock = Any()

    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        super.init(width, height, ledGrid)
        offscreen = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        synchronized(volumeLock) {
            volumeHistory.clear()
        }
    }

    /**
     * Updates the volume history and returns the normalized volume level
     * based on the min/max range from the recent history.
     */
    private fun getAdaptiveVolumeLevel(currentLevel: Float): Float {
        val nowMs = System.currentTimeMillis()
        
        synchronized(volumeLock) {
            // Add current sample
            volumeHistory.add(VolumeSample(nowMs, currentLevel))
            
            // Remove old samples outside the window
            val cutoffTime = nowMs - historyWindowMs
            volumeHistory.removeAll { it.timestamp < cutoffTime }
            
            // Calculate min/max from recent history
            if (volumeHistory.isEmpty()) {
                return currentLevel // Fallback if no history yet
            }
            
            val levels = volumeHistory.map { it.level }
            val minLevel = levels.minOrNull() ?: 0f
            val maxLevel = levels.maxOrNull() ?: 1f
            val range = maxLevel - minLevel
            
            // Map current volume to the adaptive range
            return if (range > 0.001f) {
                // Normalize to 0..1 based on recent min/max
                ((currentLevel - minLevel) / range).coerceIn(0f, 1f)
            } else {
                // If range is too small, just use the level as-is
                currentLevel
            }
        }
    }

    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        val canvas = offscreen ?: return
        val cg = canvas.createGraphics()

        // Clear background
        cg.color = Color(0, 0, 0)
        cg.fillRect(0, 0, width, height)

        // Get LED grid bounds to constrain entire animation to LED area
        val bounds = ledGrid.getGridBounds(width, height)
        val minGridX = bounds.minX
        val minGridY = bounds.minY
        val maxGridX = bounds.maxX
        val maxGridY = bounds.maxY
        val ledGridWidth = bounds.width
        val ledGridHeight = bounds.height
        val ledGridCenterX = bounds.centerX
        val ledGridCenterY = bounds.centerY

        // Get current volume level with adaptive normalization
        val rawVolume = masterLevel.coerceIn(0f, 1f)
        val volumeLevel = getAdaptiveVolumeLevel(rawVolume)

        val nowNs = System.nanoTime()
        val timeMs = nowNs / 1_000_000L
        val speedFactor = (colorSpeed shr 2) + 2
        var counter = ((timeMs * speedFactor) and 0xFFFF).toInt()
        counter = counter shr 8

        val lightFactor = 0.15f
        val normalFactor = 0.4f

        val soundColor = intArrayOf(255, 165, 0)
        val armsAndLegsDefault = intArrayOf(0xFF, 0xE0, 0xA0)
        val eyeColor = intArrayOf(255, 255, 255)

        val faceColor = colorWheel(counter and 0xFF)
        val armsAndLegsColor = armsAndLegsDefault.clone()

        // Use adaptive volume level for character reactivity
        val base = volumeLevel
        val isDancing = intensity > 128 && volumeLevel > 0.5f

        // Draw Akemi character scaled to 70% of LED grid size, centered within LED grid area
        val scale = 0.7
        val akemiWidth = (ledGridWidth * scale).toInt().coerceAtLeast(1)
        val akemiHeight = (ledGridHeight * scale).toInt().coerceAtLeast(1)
        val offsetX = ledGridCenterX - akemiWidth / 2
        val offsetY = ledGridCenterY - akemiHeight / 2

        // Draw character, constrained to LED grid bounds
        for (y in 0 until akemiHeight) {
            val akY = min(BASE_HEIGHT - 1, y * BASE_HEIGHT / akemiHeight)
            val targetY = offsetY + y
            // Only draw within LED grid bounds
            if (targetY < minGridY || targetY >= maxGridY) continue

            for (x in 0 until akemiWidth) {
                val akX = min(BASE_WIDTH - 1, x * BASE_WIDTH / akemiWidth)
                val ak = AKEMI_MAP[akY * BASE_WIDTH + akX]

                val color = when (ak) {
                    3 -> multiplyColor(armsAndLegsColor, lightFactor)
                    2 -> multiplyColor(armsAndLegsColor, normalFactor)
                    1 -> armsAndLegsColor.clone()
                    6 -> multiplyColor(faceColor, lightFactor)
                    5 -> multiplyColor(faceColor, normalFactor)
                    4 -> faceColor.clone()
                    7 -> eyeColor.clone()
                    8 -> if (base > 0.4f) {
                        val boost = clamp01(base)
                        intArrayOf(
                            min(255, (soundColor[0] * boost).roundToInt()),
                            min(255, (soundColor[1] * boost).roundToInt()),
                            min(255, (soundColor[2] * boost).roundToInt())
                        )
                    } else {
                        armsAndLegsColor.clone()
                    }
                    else -> intArrayOf(0, 0, 0)
                }

                val screenX = offsetX + x
                // Only draw within LED grid bounds
                if (screenX < minGridX || screenX >= maxGridX) continue

                if (isDancing) {
                    val danceY = (targetY + 1).coerceAtMost(maxGridY - 1)
                    if (danceY >= minGridY) {
                        setPixel(canvas, screenX, danceY, color)
                    }
                } else {
                    setPixel(canvas, screenX, targetY, color)
                }
            }
        }

        // Volume bars constrained to LED grid area
        val maxBarHeight = ledGridHeight / 2 // Half the grid height (from middle to top)

        // Single volume bar – positioned just outside the scaled Akemi character
        val barWidth = max(6, akemiWidth / 12) // Thick bar (at least 6 pixels)

        // Left bar immediately to the left of Akemi
        val leftStartX = (offsetX - barWidth - 2).coerceAtLeast(0) // Add small gap from character
        // Right bar immediately to the right of Akemi
        val rightStartX = (offsetX + akemiWidth + 2).coerceAtMost(width - barWidth) // Add small gap from character

        // Calculate bar height based on volume level, constrained to LED grid area
        var barHeight = (volumeLevel * maxBarHeight).toInt()
        barHeight = barHeight.coerceIn(0, maxBarHeight)

        // Color based on volume (blue to orange to red as volume increases)
        val hue = 240f - (volumeLevel * 180f) // Blue (240) to red (60)
        val barColor = hsvToRgb(hue.coerceIn(0f, 360f), 1.0f, 1.0f)

        // Draw the volume bar on both sides, extending upward from LED grid middle
        for (xOffset in 0 until barWidth) {
            for (y in 0 until barHeight) {
                val topY = ledGridCenterY - y // Extend upward from LED grid middle
                if (topY in minGridY until maxGridY) {
                    // Left bar
                    val leftX = leftStartX + xOffset
                    if (leftX in 0 until width) {
                        setPixel(canvas, leftX, topY, barColor)
                    }
                    // Right bar
                    val rightX = rightStartX + xOffset
                    if (rightX in 0 until width) {
                        setPixel(canvas, rightX, topY, barColor)
                    }
                }
            }
        }

        cg.dispose()

        // Blit to screen
        g.drawImage(canvas, 0, 0, null)

        // Map to LEDs
        mapToLedsFromImage(canvas, ledGrid)
    }

    override fun getName(): String = "Akemi"

    override fun getDescription(): String =
        "Stylised character with audio-reactive face and side volume bar"

    // --- Helpers for drawing / color math ---

    private fun setPixel(canvas: BufferedImage, x: Int, y: Int, rgb: IntArray) {
        if (x !in 0 until windowWidth || y !in 0 until windowHeight) return
        val r = rgb[0].coerceIn(0, 255)
        val g = rgb[1].coerceIn(0, 255)
        val b = rgb[2].coerceIn(0, 255)
        canvas.setRGB(x, y, Color(r, g, b).rgb)
    }

    private fun mapToLedsFromImage(canvas: BufferedImage, ledGrid: LedGrid) {
        ledGrid.clearAllLeds()

        val gridCount = ledGrid.gridCount
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize

        for (gridIndex in 0 until gridCount) {
            val cfg = ledGrid.getGridConfig(gridIndex) ?: continue

            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    val windowX = cfg.x + x * pixelSize + pixelSize / 2
                    val windowY = cfg.y + y * pixelSize + pixelSize / 2

                    if (windowX < 0 || windowX >= windowWidth || windowY < 0 || windowY >= windowHeight) {
                        continue
                    }

                    val rgb = canvas.getRGB(windowX, windowY)
                    val color = Color(rgb)
                    if (color.red > 5 || color.green > 5 || color.blue > 5) {
                        ledGrid.setLedColor(gridIndex, x, y, color)
                    }
                }
            }
        }

        ledGrid.sendToDevices()
    }

    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun constrain(value: Int, minVal: Int, maxVal: Int): Int = value.coerceIn(minVal, maxVal)

    private fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        var hue = h % 360f
        if (hue < 0) hue += 360f
        val hi = ((hue / 60f) % 6).toInt()
        val f = hue / 60f - hi
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        val (r, g, b) = when (hi) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return intArrayOf(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    private fun colorWheel(posValue: Int): IntArray {
        var pos = ((posValue % 256) + 256) % 256
        return when {
            pos < 85 -> intArrayOf(pos * 3, 255 - pos * 3, 0)
            pos < 170 -> {
                pos -= 85
                intArrayOf(255 - pos * 3, 0, pos * 3)
            }
            else -> {
                pos -= 170
                intArrayOf(0, pos * 3, 255 - pos * 3)
            }
        }
    }

    private fun multiplyColor(rgb: IntArray, factor: Float): IntArray {
        val clampedFactor = clamp01(factor)
        return intArrayOf(
            min(255, (rgb[0] * clampedFactor).roundToInt()),
            min(255, (rgb[1] * clampedFactor).roundToInt()),
            min(255, (rgb[2] * clampedFactor).roundToInt())
        )
    }

    companion object {
        private val log: Logger = LogManager.getLogger(AkemiAnimation::class.java)

        private const val BASE_WIDTH = 32
        private const val BASE_HEIGHT = 32

        // Direct port of the original 32x32 Akemi map
        private val AKEMI_MAP = intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,2,2,3,3,3,3,3,3,2,2,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,2,3,3,0,0,0,0,0,0,3,3,2,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,2,3,0,0,0,6,5,5,4,0,0,0,3,2,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,0,6,6,5,5,5,5,4,4,0,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,3,2,0,6,5,5,5,5,5,5,5,5,5,5,4,0,2,3,0,0,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,7,7,5,5,5,5,7,7,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,5,1,1,5,5,5,5,1,1,5,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,7,7,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            1,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,2,
            0,2,2,2,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,2,2,2,0,
            0,0,0,3,2,0,0,0,6,5,4,4,4,4,4,4,4,4,4,4,4,4,4,4,0,0,0,2,2,0,0,0,
            0,0,0,3,2,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,2,3,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,3,0,3,3,0,0,3,3,0,3,3,0,0,0,0,2,2,0,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,2,0,3,2,0,0,3,2,0,3,2,0,0,0,0,2,3,0,0,0,0,
            0,0,0,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,2,3,0,0,0,0,0,
            0,0,0,0,0,3,2,2,2,2,0,0,0,3,2,0,0,3,2,0,0,0,3,2,2,2,3,0,0,0,0,0,
            0,0,0,0,0,0,3,3,3,0,0,0,0,3,2,0,0,3,2,0,0,0,0,3,3,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        )
    }
}

