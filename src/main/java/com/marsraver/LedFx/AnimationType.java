package com.marsraver.LedFx;

import com.marsraver.LedFx.animations.*;

/**
 * Enumeration of available animation types for the LED framework.
 * Each type corresponds to a different visual animation that can be displayed.
 */
public enum AnimationType {
    TEST("test", "Test Animation"),
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
    
    private final String id;
    private final String displayName;
    
    AnimationType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Gets the unique identifier for this animation type.
     * 
     * @return The animation ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the human-readable display name for this animation type.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Finds an animation type by its ID.
     * 
     * @param id The animation ID to search for
     * @return The matching AnimationType, or null if not found
     */
    public static AnimationType fromId(String id) {
        for (AnimationType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Gets all available animation types as a formatted string.
     * 
     * @return A string listing all available animations
     */
    public static String getAvailableAnimations() {
        StringBuilder sb = new StringBuilder("Available animations:\n");
        for (AnimationType type : values()) {
            sb.append("  ").append(type.id).append(" - ").append(type.displayName).append("\n");
        }
        return sb.toString();
    }


    public static LedAnimation createAnimation(String animationId) {
        AnimationType type = AnimationType.fromId(animationId);
        assert type != null;
        return createAnimation(type);
    }

    public static LedAnimation createAnimation(AnimationType animationType) {
            return switch (animationType) {
                case TEST -> new TestAnimation();
                case SPINNING_BEACHBALL -> new SpinningBeachballAnimation();
                case BOUNCING_BALL -> new BouncingBallAnimation();
                case MUSIC_BALL -> new MusicBallAnimation();
                case VIDEO_PLAYER -> new VideoPlayerAnimation();
                case FAST_PLASMA -> new FastPlasmaAnimation();
                case CLOUDS -> new CloudsAnimation();
                case PERLIN_OSCILLATOR -> new PerlinOscillatorAnimation();
                case STARFIELD -> new StarfieldAnimation();
                case BLACK_HOLE -> new BlackHoleAnimation();
                case BLURZ -> new BlurzAnimation();
            };
    }
}
