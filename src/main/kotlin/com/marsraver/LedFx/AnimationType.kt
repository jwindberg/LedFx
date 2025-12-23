package com.marsraver.LedFx

import com.marsraver.LedFx.animations.*

/**
 * Enumeration of available animation types for the LED framework.
 * Each type corresponds to a different visual animation that can be displayed.
 */
enum class AnimationType(
    /**
     * Gets the unique identifier for this animation type.
     *
     * @return The animation ID
     */
    val id: String,
    displayName: String
) {
    AKEMI("akemi", "Akemi Animation"),
    AUDIO_VISUALIZER("audio-visualizer", "Audio Visualizer"),
    BLACK_HOLE("black-hole", "Black Hole Animation"),
    BLURZ("blurz", "Blurz Animation"),
    BOUNCING_BALL("bouncing-ball", "Bouncing Ball Animation"),
    CLOUDS("clouds", "Clouds Animation"),
    FALLING_SNOW("falling-snow", "Falling Snow Animation"),
    FAST_PLASMA("fast-plasma", "Fast Plasma Animation"),
    MUSIC_BALL("music-ball", "Music Ball Animation"),
    PERLIN_OSCILLATOR("perlin-oscillator", "Perlin Oscillator Animation"),
    SOUND_BUBBLE("sound-bubble", "Sound Bubble Animation"),
    SPINNING_BEACHBALL("spinning-beachball", "Spinning Beachball Animation"),
    STARFIELD("starfield", "Starfield Animation"),
    TRON_RECOGNIZER("tron-recognizer", "Tron Recognizer Animation"),
    VIDEO_PLAYER("video-player", "Video Player Animation");

    /**
     * Gets the human-readable display name for this animation type.
     */
    val displayName: String = displayName

    override fun toString(): String = displayName

    companion object {
        /**
         * Finds an animation type by its ID.
         *
         * @param id The animation ID to search for
         * @return The matching AnimationType, or null if not found
         */
        fun fromId(id: String?): AnimationType? {
            for (type in AnimationType.entries) {
                if (type.id == id) {
                    return type
                }
            }
            return null
        }

        val availableAnimations: String
            /**
             * Gets all available animation types as a formatted string.
             *
             * @return A string listing all available animations
             */
            get() {
                val sb = StringBuilder("Available animations:\n")
                for (type in AnimationType.entries) {
                    sb.append("  ").append(type.id).append(" - ").append(type.displayName).append("\n")
                }
                return sb.toString()
            }

        fun createAnimation(animationType: AnimationType): LedAnimation =
            when (animationType) {
                AKEMI -> AkemiAnimation()
                AUDIO_VISUALIZER -> AudioVisualizerAnimation()
                BLACK_HOLE -> BlackHoleAnimation()
                BLURZ -> BlurzAnimation()
                BOUNCING_BALL -> BouncingBallAnimation()
                CLOUDS -> CloudsAnimation()
                FALLING_SNOW -> FallingSnowAnimation()
                FAST_PLASMA -> FastPlasmaAnimation()
                MUSIC_BALL -> MusicBallAnimation()
                PERLIN_OSCILLATOR -> PerlinOscillatorAnimation()
                SOUND_BUBBLE -> SoundBubbleAnimation()
                SPINNING_BEACHBALL -> SpinningBeachballAnimation()
                STARFIELD -> StarfieldAnimation()
                TRON_RECOGNIZER -> TronRecognizerAnimation()
                VIDEO_PLAYER -> VideoPlayerAnimation()
            }
    }
}
