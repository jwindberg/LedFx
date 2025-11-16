package com.marsraver.LedFx.layout

import com.marsraver.LedFx.wled.ColorMapping
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Loads layout configurations from XML files.
 * Parses XML files in the resources/layouts directory.
 */
object LayoutLoader {
    private const val LAYOUTS_PATH = "/layouts/"
    private val log: Logger = LogManager.getLogger(LayoutLoader::class.java)

    /**
     * Loads a layout configuration from an XML file.
     *
     * @param layoutName the name of the layout file (without .xml extension)
     * @return the parsed layout configuration
     * @throws Exception if the layout file cannot be loaded or parsed
     */
    @Throws(Exception::class)
    fun loadLayout(layoutName: String): LayoutConfig {
        val resourcePath = "$LAYOUTS_PATH$layoutName.xml"
        val inputStream = LayoutLoader::class.java.getResourceAsStream(resourcePath)

        requireNotNull(inputStream) { "Layout file not found: $resourcePath" }

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputStream)

            return parseLayout(document)
        } finally {
            inputStream.close()
        }
    }

    /**
     * Parses a layout configuration from a DOM document.
     */
    private fun parseLayout(document: Document): LayoutConfig {
        val root = document.documentElement

        require(root.tagName == "layout") { "Root element must be 'layout'" }

        val layout = LayoutConfig()

        // Parse layout attributes
        layout.name = getAttributeValue(root, "name", "Unnamed")
        layout.title = getAttributeValue(root, "title", "LedFx")
        layout.windowWidth = getIntAttribute(root, "windowWidth", 500)
        layout.windowHeight = getIntAttribute(root, "windowHeight", 400)

        // Parse grids
        val gridNodes = root.getElementsByTagName("grid")
        for (i in 0 until gridNodes.length) {
            val gridElement = gridNodes.item(i) as Element
            val grid = parseGrid(gridElement)
            layout.addGrid(grid)
        }

        return layout
    }

    /**
     * Parses a single grid configuration from an XML element.
     */
    private fun parseGrid(gridElement: Element): GridConfig {
        val grid = GridConfig()

        grid.id = getAttributeValue(gridElement, "id", "grid")
        grid.deviceIp = getAttributeValue(gridElement, "deviceIp", "192.168.1.100")
        grid.ledCount = getIntAttribute(gridElement, "ledCount", 256)
        grid.x = getIntAttribute(gridElement, "x", 0)
        grid.y = getIntAttribute(gridElement, "y", 0)
        grid.width = getIntAttribute(gridElement, "width", 240)
        grid.height = getIntAttribute(gridElement, "height", 240)
        grid.gridSize = getIntAttribute(gridElement, "gridSize", 16)
        grid.pixelSize = getIntAttribute(gridElement, "pixelSize", 15)

        // Parse colorMapping attribute
        val colorMappingStr = getAttributeValue(gridElement, "colorMapping", "")
        if (colorMappingStr.isNotEmpty()) {
            try {
                val colorMapping = ColorMapping.valueOf(colorMappingStr.uppercase(Locale.getDefault()))
                grid.colorMapping = colorMapping
            } catch (e: IllegalArgumentException) {
                log.error(
                    "Invalid colorMapping '$colorMappingStr' for grid '${grid.id}', using default GBR"
                )
            }
        }

        return grid
    }

    /**
     * Gets a string attribute value with a default.
     */
    private fun getAttributeValue(element: Element, attributeName: String, defaultValue: String): String {
        val value = element.getAttribute(attributeName)
        return if (value.isEmpty()) defaultValue else value
    }

    /**
     * Gets an integer attribute value with a default.
     */
    private fun getIntAttribute(element: Element, attributeName: String, defaultValue: Int): Int {
        val value = element.getAttribute(attributeName)
        return try {
            if (value.isEmpty()) defaultValue else value.toInt()
        } catch (e: NumberFormatException) {
            log.error("Invalid integer value for attribute '$attributeName': $value")
            defaultValue
        }
    }

    /**
     * Lists all available layout files.
     *
     * @return list of layout names (without .xml extension)
     */
    fun listAvailableLayouts(): MutableList<String> {
        val layouts: MutableList<String> = ArrayList()

        // For now, we'll hardcode the known layouts
        // In a more sophisticated implementation, we could scan the resources directory
        layouts.add("OneGrid")
        layouts.add("TwoGrids")
        layouts.add("FourGrids")

        return layouts
    }
}

