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
    SPINNING_BEACHBALL("spinning-beachball", "Spinning Beachball Animation"),
    BOUNCING_BALL("bouncing-ball", "Bouncing Ball Animation"),
    MUSIC_BALL("music-ball", "Music Ball Animation"),
    VIDEO_PLAYER("video-player", "Video Player Animation"),
    FAST_PLASMA("fast-plasma", "Fast Plasma Animation"),
    CLOUDS("clouds", "Clouds Animation"),
    PERLIN_OSCILLATOR("perlin-oscillator", "Perlin Oscillator Animation"),
    STARFIELD("starfield", "Starfield Animation"),
    BLACK_HOLE("black-hole", "Black Hole Animation"),
    BLURZ("blurz", "Blurz Animation");

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


        fun createAnimation(animationId: String?): LedAnimation {
            val animationType = fromId(animationId)
                ?: error("Unknown animation id '$animationId'")
            return createAnimation(animationType)
        }

        fun createAnimation(animationType: AnimationType): LedAnimation =
            when (animationType) {
                AnimationType.SPINNING_BEACHBALL -> SpinningBeachballAnimation()
                AnimationType.BOUNCING_BALL -> BouncingBallAnimation()
                AnimationType.MUSIC_BALL -> MusicBallAnimation()
                AnimationType.VIDEO_PLAYER -> VideoPlayerAnimation()
                AnimationType.FAST_PLASMA -> FastPlasmaAnimation()
                AnimationType.CLOUDS -> CloudsAnimation()
                AnimationType.PERLIN_OSCILLATOR -> PerlinOscillatorAnimation()
                AnimationType.STARFIELD -> StarfieldAnimation()
                AnimationType.BLACK_HOLE -> BlackHoleAnimation()
                AnimationType.BLURZ -> BlurzAnimation()
            }
    }
}
