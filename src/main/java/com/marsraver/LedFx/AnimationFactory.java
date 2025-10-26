package com.marsraver.LedFx;

import com.marsraver.LedFx.animations.BouncingBallAnimation;
import com.marsraver.LedFx.animations.CloudsAnimation;
import com.marsraver.LedFx.animations.DjLightAnimation;
import com.marsraver.LedFx.animations.FastPlasmaAnimation;
import com.marsraver.LedFx.animations.SpinningBeachballAnimation;

/**
 * Factory class for creating LED animations.
 * Provides a centralized way to instantiate different animation types.
 */
public class AnimationFactory {
    
    /**
     * Creates a new animation instance based on the specified animation type.
     * 
     * @param animationType The type of animation to create
     * @return A new animation instance, or null if the type is not supported
     */
    public static LedAnimation createAnimation(AnimationType animationType) {
        if (animationType == null) {
            System.err.println("AnimationFactory: animationType is null");
            return null;
        }
        
        System.out.println("AnimationFactory: Creating animation for type: " + animationType);
        
        try {
            switch (animationType) {
                case BOUNCING_BALL:
                    return new BouncingBallAnimation();
                case CLOUDS:
                    return new CloudsAnimation();
                case SPINNING_BEACHBALL:
                    return new SpinningBeachballAnimation();
                case DJ_LIGHT:
                    return new DjLightAnimation();
                case FAST_PLASMA:
                    return new FastPlasmaAnimation();
                default:
                    System.err.println("Unknown animation type: " + animationType);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error creating animation " + animationType + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a new animation instance by animation ID.
     * 
     * @param animationId The ID of the animation to create
     * @return A new animation instance, or null if the ID is not found
     */
    public static LedAnimation createAnimation(String animationId) {
        AnimationType type = AnimationType.fromId(animationId);
        return createAnimation(type);
    }
    
    /**
     * Gets the default animation type.
     * 
     * @return The default animation type
     */
    public static AnimationType getDefaultAnimation() {
        return AnimationType.BOUNCING_BALL;
    }
}
