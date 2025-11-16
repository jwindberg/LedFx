package com.marsraver.LedFx

import com.marsraver.LedFx.wled.WledNetworkScanner
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.SwingUtilities

/**
 * Launcher class for running LED sketches with unified LED integration.
 * Provides a simple way to start LED sketches with keyboard controls.
 */
object LedSketchLauncher {
    private val log: Logger = LogManager.getLogger(LedSketchLauncher::class.java)
    /**
     * Runs a custom LED sketch.
     *
     * @param sketch The LED sketch to run
     * @param layoutName The name of the layout to use
     */
    fun runLedSketch(sketch: LedSketch?, layoutName: String) {
        SwingUtilities.invokeLater(Runnable {
            val runner = LedSketchRunner(sketch, layoutName)
            // Add keyboard listener for ESC to exit
            runner.frame?.addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent?) {}

                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ESCAPE) {
                        runner.stop() // Turn off LEDs before exiting
                        System.exit(0)
                    }
                }

                override fun keyReleased(e: KeyEvent?) {}
            })


            // Make sure the frame can receive key events
            runner.frame?.isFocusable = true
            runner.frame?.requestFocus()
        })
    }

    /**
     * Discovers WLED devices on the network and runs a sketch with the first two found.
     *
     * @param sketch The LED sketch to run
     * @param layoutName The name of the layout to use
     */
    fun runWithAutoDiscovery(sketch: LedSketch?, layoutName: String) {
        try {
            log.debug("🔍 Discovering WLED devices...")
            val devices = WledNetworkScanner.discover()

            if (devices.size < 2) {
                log.error("❌ Need at least 2 WLED devices found on the network!")
                log.error("Found only {} devices.", devices.size)
                return
            }

            val leftDevice = devices.get(0)
            val rightDevice = devices.get(1)
            log.debug("✅ Using left device: {}", leftDevice)
            log.debug("✅ Using right device: {}", rightDevice)

            runLedSketch(sketch, layoutName)
        } catch (e: Exception) {
            log.error("❌ Error discovering WLED devices: {}", e.message, e)
            e.printStackTrace()
        }
    }
}
