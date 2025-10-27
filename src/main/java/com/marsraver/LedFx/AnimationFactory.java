package com.marsraver.LedFx;

import com.marsraver.LedFx.animations.TestAnimation;
import com.marsraver.LedFx.animations.SpinningBeachballAnimation;
import com.marsraver.LedFx.animations.BouncingBallAnimation;
import com.marsraver.LedFx.animations.MusicBallAnimation;
import com.marsraver.LedFx.animations.VideoPlayerAnimation;
import com.marsraver.LedFx.animations.FastPlasmaAnimation;
import com.marsraver.LedFx.animations.CloudsAnimation;
import com.marsraver.LedFx.animations.PerlinOscillatorAnimation;
import com.marsraver.LedFx.animations.StarfieldAnimation;

/**
 * Factory class for creating LED animations.
 * Provides a centralized way to instantiate different animation types.
 */
public class AnimationFactory {

    /**
     * Gets the default animation type.
     * 
     * @return The default animation type
     */
    public static AnimationType getDefaultAnimation() {
        return AnimationType.TEST;
    }
}
