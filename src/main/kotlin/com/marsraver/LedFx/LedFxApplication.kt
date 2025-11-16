package com.marsraver.LedFx

import com.marsraver.LedFx.layout.LayoutLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.HeadlessException
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.SwingUtilities

object LedFxApplication {
    private val log: Logger = LogManager.getLogger(LedFxApplication::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
        // Launch the LED layout application on the Swing EDT
        SwingUtilities.invokeLater(Runnable {
            try {
                // Get layout name from args or use default
                var layoutName: String = if (args.isNotEmpty()) args[0] else "FourGrids"


                // Get animation type from args or use default (use spinning beachball as default)
                val animationTypeId: String = if (args.size > 1) args[1] else "spinning-beachball"

                log.info(
                    "Starting LED Layout application with layout: {} and animation: {}",
                    layoutName,
                    animationTypeId
                )


                // Validate layout exists
                if (!LayoutLoader.listAvailableLayouts().contains(layoutName)) {
                    log.error(
                        "Layout '{}' not found. Available layouts: {}",
                        layoutName,
                        LayoutLoader.listAvailableLayouts()
                    )
                    log.error("Using default layout: FourGrids")
                    layoutName = "FourGrids"
                }


                // Parse animation type
                var animationType: AnimationType? = AnimationType.fromId(animationTypeId)
                if (animationType == null) {
                    log.error("Unknown animation type: {}", animationTypeId)
                    log.error(AnimationType.availableAnimations)
                    log.error("Using default animation: spinning-beachball")
                    animationType = AnimationType.SPINNING_BEACHBALL
                }


                // Create and run the layout sketch with animation selection
                val runner = AnimationSketchRunner(animationType, layoutName)


                // Add keyboard listener for ESC to exit
                runner.frame?.addKeyListener(object : KeyListener {
                    override fun keyTyped(e: KeyEvent?) {}

                    override fun keyPressed(e: KeyEvent) {
                        if (e.keyCode == KeyEvent.VK_ESCAPE) {
                            log.info("Exiting application...")
                            runner.stop()
                            System.exit(0)
                        }
                    }

                    override fun keyReleased(e: KeyEvent?) {}
                })


                // Make sure the frame can receive key events
                runner.frame?.isFocusable = true
                runner.frame?.requestFocus()

                log.info("LED Layout application started successfully!")
                log.info("Use the dropdown to switch animations. Press ESC to exit")
            } catch (he: HeadlessException) {
                log.error("HeadlessException - GUI not available. Cannot launch application.")
                System.exit(1)
            } catch (e: Exception) {
                log.error("Failed to start layout application: {}", e.message, e)
                System.exit(1)
            }
        })


        // Keep the main thread alive so the GUI can run
        try {
            Thread.sleep(Long.Companion.MAX_VALUE)
        } catch (e: InterruptedException) {
            log.info("Application interrupted, shutting down...")
        }
    }
}
