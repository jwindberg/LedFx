package com.marsraver.LedFx.layout;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads layout configurations from XML files.
 * Parses XML files in the resources/layouts directory.
 */
public class LayoutLoader {
    
    private static final String LAYOUTS_PATH = "/layouts/";
    
    /**
     * Loads a layout configuration from an XML file.
     * 
     * @param layoutName the name of the layout file (without .xml extension)
     * @return the parsed layout configuration
     * @throws Exception if the layout file cannot be loaded or parsed
     */
    public static LayoutConfig loadLayout(String layoutName) throws Exception {
        String resourcePath = LAYOUTS_PATH + layoutName + ".xml";
        InputStream inputStream = LayoutLoader.class.getResourceAsStream(resourcePath);
        
        if (inputStream == null) {
            throw new IllegalArgumentException("Layout file not found: " + resourcePath);
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            return parseLayout(document);
        } finally {
            inputStream.close();
        }
    }
    
    /**
     * Parses a layout configuration from a DOM document.
     */
    private static LayoutConfig parseLayout(Document document) {
        Element root = document.getDocumentElement();
        
        if (!"layout".equals(root.getTagName())) {
            throw new IllegalArgumentException("Root element must be 'layout'");
        }
        
        LayoutConfig layout = new LayoutConfig();
        
        // Parse layout attributes
        layout.setName(getAttributeValue(root, "name", "Unnamed"));
        layout.setTitle(getAttributeValue(root, "title", "LedFx"));
        layout.setWindowWidth(getIntAttribute(root, "windowWidth", 500));
        layout.setWindowHeight(getIntAttribute(root, "windowHeight", 400));
        
        // Parse grids
        NodeList gridNodes = root.getElementsByTagName("grid");
        for (int i = 0; i < gridNodes.getLength(); i++) {
            Element gridElement = (Element) gridNodes.item(i);
            GridConfig grid = parseGrid(gridElement);
            layout.addGrid(grid);
        }
        
        return layout;
    }
    
    /**
     * Parses a single grid configuration from an XML element.
     */
    private static GridConfig parseGrid(Element gridElement) {
        GridConfig grid = new GridConfig();
        
        grid.setId(getAttributeValue(gridElement, "id", "grid"));
        grid.setDeviceIp(getAttributeValue(gridElement, "deviceIp", "192.168.1.100"));
        grid.setLedCount(getIntAttribute(gridElement, "ledCount", 256));
        grid.setX(getIntAttribute(gridElement, "x", 0));
        grid.setY(getIntAttribute(gridElement, "y", 0));
        grid.setWidth(getIntAttribute(gridElement, "width", 240));
        grid.setHeight(getIntAttribute(gridElement, "height", 240));
        grid.setGridSize(getIntAttribute(gridElement, "gridSize", 16));
        grid.setPixelSize(getIntAttribute(gridElement, "pixelSize", 15));
        
        return grid;
    }
    
    /**
     * Gets a string attribute value with a default.
     */
    private static String getAttributeValue(Element element, String attributeName, String defaultValue) {
        String value = element.getAttribute(attributeName);
        return value.isEmpty() ? defaultValue : value;
    }
    
    /**
     * Gets an integer attribute value with a default.
     */
    private static int getIntAttribute(Element element, String attributeName, int defaultValue) {
        String value = element.getAttribute(attributeName);
        try {
            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for attribute '" + attributeName + "': " + value);
            return defaultValue;
        }
    }
    
    /**
     * Lists all available layout files.
     * 
     * @return list of layout names (without .xml extension)
     */
    public static List<String> listAvailableLayouts() {
        List<String> layouts = new ArrayList<>();
        
        // For now, we'll hardcode the known layouts
        // In a more sophisticated implementation, we could scan the resources directory
        layouts.add("TwoGrids");
        
        return layouts;
    }
}

