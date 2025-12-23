package com.marsraver.LedFx.layout

import com.marsraver.LedFx.AnimationSketchRunner
import com.marsraver.LedFx.AnimationType
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

/**
 * Launcher for layout-based LED sketches with animation selection.
 * Uses XML layout files to configure window size and LED grid placements.
 */
object LayoutLauncher {
    private val log: Logger = LogManager.getLogger(LayoutLauncher::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }

        val layoutName: String = args[0]
        val animationTypeId: String = if (args.size > 1) args[1] else "test"

        try {
            log.debug("Loading layout: {}", layoutName)
            log.debug("Animation: {}", animationTypeId)


            // Parse animation type
            val animationType: AnimationType? = AnimationType.fromId(animationTypeId)
            if (animationType == null) {
                log.error("Unknown animation type: {}", animationTypeId)
                log.error(AnimationType.availableAnimations)
                return
            }


            // Create and run the layout sketch with animation selection
            val runner = AnimationSketchRunner(animationType, layoutName)


            // Add keyboard listener for ESC to exit
            runner.frame?.addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent?) {}

                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ESCAPE) {
                        System.exit(0)
                    }
                }

                override fun keyReleased(e: KeyEvent?) {}
            })


            // Make sure the frame can receive key events
            runner.frame?.isFocusable = true
            runner.frame?.requestFocus()

            log.debug("Layout sketch started!")
            log.debug("Use the dropdown to switch animations. Press ESC to exit")
            log.debug("")
            log.debug("Note: WLED devices may revert to their idle state when the app closes.")
            log.debug("This is normal device behavior and can be configured in WLED settings.")
        } catch (e: Exception) {
            log.error("Failed to start layout sketch: {}", e.message, e)
            e.printStackTrace()
            System.exit(1)
        }
    }

    private fun printUsage() {
        println("Usage: java LayoutLauncher <layout-name> [animation-type]")
        println("")
        println("Available layouts:")
        for (layout in LayoutLoader.listAvailableLayouts()) {
            println("  - " + layout)
        }
        println()
        println("Available animations:")
        println("  - test")
        println("  - spinning-beachball")
        println("  - bouncing-ball")
        println("  - music-ball")
        println("  - video-player")
        println("  - fast-plasma")
        println("  - clouds")
        println("")
        println("Example:")
        println("  java LayoutLauncher TwoGrids test")
    }
}

