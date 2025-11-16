package com.marsraver.LedFx.layout

import com.marsraver.LedFx.wled.ColorMapping

/**
 * Configuration for a single LED grid within a layout.
 * Defines the grid's position, size, and device connection.
 */
class GridConfig {
    // Getters and setters
    var id: String? = null
    var deviceIp: String? = null
    var ledCount: Int = 0
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0
    var gridSize: Int = 0 // 16x16, 32x16, etc.
    var pixelSize: Int = 0 // Size of each LED pixel in the window
    var colorMapping: ColorMapping? = null // Color channel order for this device

    constructor()

    @JvmOverloads
    constructor(
        id: String?,
        deviceIp: String?,
        ledCount: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        gridSize: Int,
        pixelSize: Int,
        colorMapping: ColorMapping? = null
    ) {
        this.id = id
        this.deviceIp = deviceIp
        this.ledCount = ledCount
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        this.gridSize = gridSize
        this.pixelSize = pixelSize
        this.colorMapping = colorMapping
    }

    override fun toString(): String {
        return String.format(
            "GridConfig{id='%s', deviceIp='%s', ledCount=%d, pos=(%d,%d), size=%dx%d, gridSize=%d, pixelSize=%d, colorMapping=%s}",
            id, deviceIp, ledCount, x, y, width, height, gridSize, pixelSize, colorMapping
        )
    }
}

