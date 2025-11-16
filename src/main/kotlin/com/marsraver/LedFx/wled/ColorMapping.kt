package com.marsraver.LedFx.wled

import kotlin.math.max
import kotlin.math.min

/**
 * Defines the possible color channel orders for LED data mapping.
 * Each order specifies how R, G, B channels are arranged in the output data.
 */
enum class ColorMapping(
    private val redIndex: Int,
    private val greenIndex: Int,
    private val blueIndex: Int,
    description: String
) {
    RGB(0, 1, 2, "Red, Green, Blue"),
    BGR(2, 1, 0, "Blue, Green, Red"),
    GRB(1, 0, 2, "Green, Red, Blue"),
    RBG(0, 2, 1, "Red, Blue, Green"),
    BRG(2, 0, 1, "Blue, Red, Green"),
    GBR(1, 2, 0, "Green, Blue, Red");

    val description: String?

    init {
        this.description = description
    }

    /**
     * Maps a color value from the input array to the specified channel order.
     *
     * @param colorIndex The base index in the color array (i * 3)
     * @param channel The channel to retrieve: 0=Red, 1=Green, 2=Blue
     * @param ledColors The input color array
     * @return The color value for the specified channel in the target order
     */
    fun mapChannel(colorIndex: Int, channel: Int, ledColors: IntArray): Byte {
        val sourceIndex: Int
        when (channel) {
            0 -> sourceIndex = redIndex
            1 -> sourceIndex = greenIndex
            2 -> sourceIndex = blueIndex
            else -> sourceIndex = 0
        }

        val arrayIndex = colorIndex + sourceIndex
        if (arrayIndex >= 0 && arrayIndex < ledColors.size) {
            return max(0, min(255, ledColors[arrayIndex])).toByte()
        }
        return 0
    }
}
