package com.marsraver.LedFx.layout

/**
 * Complete layout configuration loaded from XML.
 * Contains window settings and all LED grid configurations.
 */
class LayoutConfig {
    // Public properties
    var name: String? = null
    var title: String? = null
    var windowWidth: Int = 0
    var windowHeight: Int = 0
    var grids: MutableList<GridConfig>

    constructor() {
        this.grids = ArrayList()
    }

    constructor(name: String?, title: String?, windowWidth: Int, windowHeight: Int) {
        this.name = name
        this.title = title
        this.windowWidth = windowWidth
        this.windowHeight = windowHeight
        this.grids = ArrayList()
    }

    fun addGrid(grid: GridConfig) {
        this.grids.add(grid)
    }

    fun getGridById(id: String): GridConfig? {
        return grids.firstOrNull { grid -> id == grid.id }
    }

    val gridCount: Int
        get() = grids.size

    override fun toString(): String {
        return String.format(
            "LayoutConfig{name='%s', title='%s', window=%dx%d, grids=%d}",
            name, title, windowWidth, windowHeight, grids.size
        )
    }
}

