package com.marsraver.LedFx

/**
 * Factory class for creating LED animations.
 * Provides a centralized way to instantiate different animation types.
 */
object AnimationFactory {
    val defaultAnimation: AnimationType
        /**
         * Gets the default animation type.
         *
         * @return The default animation type
         */
        get() = AnimationType.TEST
}
