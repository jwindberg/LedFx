package com.marsraver.LedFx.wled;

/**
 * Defines the possible color channel orders for LED data mapping.
 * Each order specifies how R, G, B channels are arranged in the output data.
 */
public enum ColorMapping {
    RGB(0, 1, 2, "Red, Green, Blue"),
    BGR(2, 1, 0, "Blue, Green, Red"),
    GRB(1, 0, 2, "Green, Red, Blue"),
    RBG(0, 2, 1, "Red, Blue, Green"),
    BRG(2, 0, 1, "Blue, Red, Green"),
    GBR(1, 2, 0, "Green, Blue, Red");

    private final int redIndex;
    private final int greenIndex;
    private final int blueIndex;
    private final String description;

    ColorMapping(int redIndex, int greenIndex, int blueIndex, String description) {
        this.redIndex = redIndex;
        this.greenIndex = greenIndex;
        this.blueIndex = blueIndex;
        this.description = description;
    }

    /**
     * Maps a color value from the input array to the specified channel order.
     * 
     * @param colorIndex The base index in the color array (i * 3)
     * @param channel The channel to retrieve: 0=Red, 1=Green, 2=Blue
     * @param ledColors The input color array
     * @return The color value for the specified channel in the target order
     */
    public byte mapChannel(int colorIndex, int channel, int[] ledColors) {
        int sourceIndex;
        switch (channel) {
            case 0: // Red channel
                sourceIndex = redIndex;
                break;
            case 1: // Green channel
                sourceIndex = greenIndex;
                break;
            case 2: // Blue channel
                sourceIndex = blueIndex;
                break;
            default:
                sourceIndex = 0;
        }
        
        int arrayIndex = colorIndex + sourceIndex;
        if (arrayIndex >= 0 && arrayIndex < ledColors.length) {
            return (byte) Math.max(0, Math.min(255, ledColors[arrayIndex]));
        }
        return 0;
    }

    public String getDescription() {
        return description;
    }
}
