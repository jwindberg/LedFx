package com.marsraver.LedFx.animations

import com.marsraver.LedFx.LedAnimation
import com.marsraver.LedFx.LedGrid
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*

/**
 * Tron Recognizer animation - a 3D recognizer swooping in and out of frame.
 * The recognizer is a distinctive geometric flying vehicle from the Tron universe.
 */
class TronRecognizerAnimation : LedAnimation {
    // Offscreen buffer for drawing and LED sampling
    private var offscreen: BufferedImage? = null
    private var offG: Graphics2D? = null
    
    private var windowWidth = 0
    private var windowHeight = 0
    private var startTime: Long = 0
    
    // Recognizer 3D model data
    private data class Vertex3D(val x: Float, val y: Float, val z: Float)
    private data class Edge(val v1: Int, val v2: Int)
    private data class Face(val vertexIndices: List<Int>)
    
    // Recognizer 3D model
    private val recognizerVertices = mutableListOf<Vertex3D>()
    private val recognizerEdges = mutableListOf<Edge>()
    private val recognizerFaces = mutableListOf<Face>()
    
    // Animation state
    private var recognizerX: Float = 0f
    private var recognizerY: Float = 0f
    private var recognizerZ: Float = 0f
    private var recognizerRotationX: Float = 0f
    private var recognizerRotationY: Float = 0f
    private var recognizerRotationZ: Float = 0f
    
    // Tron colors
    private val recognizerColor = Color(0, 255, 255)  // Cyan
    private val recognizerGlow = Color(0, 200, 255)  // Softer cyan
    private val bgColor = Color.BLACK  // Black background
    private val gridColor = Color(0, 150, 200)  // Grid lines (not used on black background)
    
    override fun init(width: Int, height: Int, ledGrid: LedGrid) {
        this.windowWidth = width
        this.windowHeight = height
        this.startTime = System.currentTimeMillis()
        
        // Initialize offscreen buffer
        offscreen = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        offG = offscreen!!.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        }
        
        // Load recognizer 3D model from file, or use programmatic fallback
        if (!loadRecognizerModelFromFile()) {
            log.warn("Could not load tronRecognizer.obj, using programmatic model")
            buildRecognizerModel()
        }
        
        // Initialize recognizer position (centered)
        recognizerX = width / 2f
        recognizerY = height / 2f
        recognizerZ = -250f
        
        log.debug("Tron Recognizer Animation initialized")
    }
    
    /**
     * Attempts to load the recognizer 3D model from an OBJ file.
     * The OBJ file should be exported from the tronRecognizer.blend file.
     * 
     * @return true if the model was loaded successfully, false otherwise
     */
    private fun loadRecognizerModelFromFile(): Boolean {
        recognizerVertices.clear()
        recognizerEdges.clear()
        recognizerFaces.clear()
        
        return try {
            // Try to load tronRecognizer.obj from resources
            val resourceStream = javaClass.classLoader.getResourceAsStream("tronRecognizer.obj")
                ?: return false
            
            val reader = BufferedReader(InputStreamReader(resourceStream))
            val vertices = mutableListOf<Vertex3D>()
            val faceIndices = mutableListOf<List<Int>>()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("v ") -> {
                            // Vertex: v x y z
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 4) {
                                val x = parts[1].toFloatOrNull() ?: 0f
                                val y = parts[2].toFloatOrNull() ?: 0f
                                val z = parts[3].toFloatOrNull() ?: 0f
                                vertices.add(Vertex3D(x, y, z))
                            }
                        }
                        trimmed.startsWith("f ") -> {
                            // Face: f v1 v2 v3 ... (may have texture/normal indices, use first number only)
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 4) {
                                val faceVertices = mutableListOf<Int>()
                                for (i in 1 until parts.size) {
                                    // OBJ indices are 1-based, and may have format v/vt/vn
                                    val vertexPart = parts[i].split("/")[0]
                                    val index = vertexPart.toIntOrNull()?.minus(1)  // Convert to 0-based
                                    if (index != null && index >= 0 && index < vertices.size) {
                                        faceVertices.add(index)
                                    }
                                }
                                if (faceVertices.size >= 2) {
                                    faceIndices.add(faceVertices)
                                }
                            }
                        }
                    }
                }
            }
            
            if (vertices.isEmpty()) {
                log.warn("No vertices found in OBJ file")
                return false
            }
            
            // Calculate bounding box to auto-scale model to appropriate size
            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            var minZ = Float.MAX_VALUE
            var maxZ = Float.MIN_VALUE
            
            for (v in vertices) {
                minX = minOf(minX, v.x)
                maxX = maxOf(maxX, v.x)
                minY = minOf(minY, v.y)
                maxY = maxOf(maxY, v.y)
                minZ = minOf(minZ, v.z)
                maxZ = maxOf(maxZ, v.z)
            }
            
            val width = maxX - minX
            val height = maxY - minY
            val depth = maxZ - minZ
            val maxDim = maxOf(width, height, depth)
            
            // Scale to make the model much larger - 150% of previous size
            val targetSize = 675f  // 450 * 1.5 = 675
            val scale = if (maxDim > 0.001f) targetSize / maxDim else 1.0f
            
            // Center the model at origin
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val centerZ = (minZ + maxZ) / 2f
            
            recognizerVertices.addAll(vertices.map { 
                Vertex3D(
                    (it.x - centerX) * scale, 
                    (it.y - centerY) * scale, 
                    (it.z - centerZ) * scale
                ) 
            })
            
            log.info("Model bounds: ${width}x${height}x${depth}, scale: $scale")
            
            // Store faces for solid rendering
            for (face in faceIndices) {
                recognizerFaces.add(Face(face))
            }
            
            // Build edges from faces (connect adjacent vertices in each face) for outline if needed
            for (face in faceIndices) {
                for (i in face.indices) {
                    val v1 = face[i]
                    val v2 = face[(i + 1) % face.size]
                    // Add edge if it doesn't already exist
                    val edgeExists = recognizerEdges.any { 
                        (it.v1 == v1 && it.v2 == v2) || (it.v1 == v2 && it.v2 == v1) 
                    }
                    if (!edgeExists) {
                        recognizerEdges.add(Edge(v1, v2))
                    }
                }
            }
            
            log.info("Loaded recognizer model: ${recognizerVertices.size} vertices, ${recognizerEdges.size} edges, ${recognizerFaces.size} faces")
            true
        } catch (e: Exception) {
            log.error("Error loading OBJ file: ${e.message}", e)
            false
        }
    }
    
    /**
     * Builds a simplified 3D model of the Tron recognizer (fallback).
     * The recognizer is a geometric disc-like craft with angular edges.
     */
    private fun buildRecognizerModel() {
        recognizerVertices.clear()
        recognizerEdges.clear()
        recognizerFaces.clear()
        
        val radius = 40f
        val thickness = 15f
        
        // Top disc vertices (8-sided polygon)
        for (i in 0 until 8) {
            val angle = (i * PI / 4.0).toFloat()
            val x = cos(angle.toDouble()).toFloat() * radius
            val y = sin(angle.toDouble()).toFloat() * radius
            recognizerVertices.add(Vertex3D(x, y, thickness / 2f))
        }
        
        // Bottom disc vertices
        for (i in 0 until 8) {
            val angle = (i * PI / 4.0).toFloat()
            val x = cos(angle.toDouble()).toFloat() * radius
            val y = sin(angle.toDouble()).toFloat() * radius
            recognizerVertices.add(Vertex3D(x, y, -thickness / 2f))
        }
        
        // Add center vertices for detail
        recognizerVertices.add(Vertex3D(0f, 0f, thickness / 2f))
        recognizerVertices.add(Vertex3D(0f, 0f, -thickness / 2f))
        
        // Top disc edges
        for (i in 0 until 8) {
            recognizerEdges.add(Edge(i, (i + 1) % 8))
            recognizerEdges.add(Edge(i, 16))  // Connect to center
        }
        
        // Bottom disc edges
        for (i in 0 until 8) {
            recognizerEdges.add(Edge(i + 8, ((i + 1) % 8) + 8))
            recognizerEdges.add(Edge(i + 8, 17))  // Connect to center
        }
        
        // Vertical edges connecting top and bottom
        for (i in 0 until 8) {
            recognizerEdges.add(Edge(i, i + 8))
        }
        
        // Add some diagonal edges for more detail
        for (i in 0 until 4) {
            recognizerEdges.add(Edge(i * 2, i * 2 + 8))
            recognizerEdges.add(Edge(i * 2 + 1, i * 2 + 1 + 8))
        }
        
        // Create faces for solid rendering (top disc, bottom disc, and side faces)
        // Top disc face
        recognizerFaces.add(Face((0 until 8).toList()))
        // Bottom disc face
        recognizerFaces.add(Face((8 until 16).toList()))
        // Side faces (quadrilaterals connecting top and bottom)
        for (i in 0 until 8) {
            val top = i
            val topNext = (i + 1) % 8
            val bottom = i + 8
            val bottomNext = ((i + 1) % 8) + 8
            recognizerFaces.add(Face(listOf(top, topNext, bottomNext, bottom)))
        }
    }
    
    override fun draw(g: Graphics2D, width: Int, height: Int, ledGrid: LedGrid) {
        val canvas = offscreen ?: return
        val cg = offG ?: return
        
        // Clear background
        cg.color = bgColor
        cg.fillRect(0, 0, width, height)
        
        // Get LED grid bounds
        val bounds = ledGrid.getGridBounds(width, height)
        
        // Update animation
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - startTime) / 1000.0f
        
        // Center the recognizer in the window (no swooping)
        recognizerX = bounds.centerX.toFloat()
        recognizerY = bounds.centerY.toFloat()
        recognizerZ = -250f  // Fixed depth - closer for larger size
        
        // Slow tumbling rotation
        recognizerRotationY = elapsed * 0.2f  // Slower Y rotation
        recognizerRotationX = elapsed * 0.2f  // Slow X rotation
        recognizerRotationZ = elapsed * 0.2f   // Slow Z rotation
        
        // No grid lines on black background (removed per user request)
        
        // Draw recognizer
        drawRecognizer(cg, recognizerX, recognizerY, recognizerZ, 
                      recognizerRotationX, recognizerRotationY, recognizerRotationZ)
        
        // Blit to screen
        g.drawImage(canvas, 0, 0, null)
        
        // Map to LEDs by sampling from the offscreen image
        mapToLedsFromImage(canvas, ledGrid)
        
        // Info text
        g.color = gridColor
        g.drawString("Tron Recognizer - Press ESC to exit", 10, 20)
    }
    
    /**
     * Draws the recognizer in 3D space.
     */
    private fun drawRecognizer(
        g: Graphics2D,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        rotX: Float,
        rotY: Float,
        rotZ: Float
    ) {
        // Project 3D vertices to 2D screen space
        val projectedVertices = recognizerVertices.map { vertex ->
            // Apply rotations
            var x = vertex.x
            var y = vertex.y
            var z = vertex.z
            
            // Rotate around Y axis
            val cosY = cos(rotY.toDouble()).toFloat()
            val sinY = sin(rotY.toDouble()).toFloat()
            val x1 = x * cosY - z * sinY
            val z1 = x * sinY + z * cosY
            
            // Rotate around X axis
            val cosX = cos(rotX.toDouble()).toFloat()
            val sinX = sin(rotX.toDouble()).toFloat()
            val y1 = y * cosX - z1 * sinX
            val z2 = y * sinX + z1 * cosX
            
            // Rotate around Z axis
            val cosZ = cos(rotZ.toDouble()).toFloat()
            val sinZ = sin(rotZ.toDouble()).toFloat()
            val x2 = x1 * cosZ - y1 * sinZ
            val y2 = x1 * sinZ + y1 * cosZ
            
            // Translate to position
            val worldX = x2 + centerX
            val worldY = y2 + centerY
            val worldZ = z2 + centerZ
            
            // Perspective projection
            // Camera is at Z=0, looking down negative Z (negative Z = closer to camera)
            // Use simple perspective: scale = distance / (distance + depth)
            val cameraDistance = 400f
            val depth = -worldZ  // Convert to positive depth (closer = larger depth value)
            val perspective = cameraDistance / (cameraDistance + depth)
            
            // Project to screen coordinates
            val screenX = (worldX - centerX) * perspective + centerX
            val screenY = (worldY - centerY) * perspective + centerY
            
            // Store depth for sorting
            Triple(screenX, screenY, worldZ)
        }
        
        // Sort faces by depth (back to front) for proper rendering
        val sortedFaces = recognizerFaces.map { face ->
            // Calculate average depth of face vertices
            var totalDepth = 0f
            for (vertexIndex in face.vertexIndices) {
                if (vertexIndex < projectedVertices.size) {
                    totalDepth += projectedVertices[vertexIndex].third
                }
            }
            val avgDepth = totalDepth / face.vertexIndices.size
            Pair(face, avgDepth)
        }.sortedByDescending { it.second }  // Draw back to front
        
        // Draw solid faces (back to front for proper depth)
        for ((face, depth) in sortedFaces) {
            // Skip if face is behind camera
            if (depth > 50f) continue
            
            // Project all vertices of this face
            val facePoints = mutableListOf<Pair<Int, Int>>()
            var allOnScreen = false
            
            for (vertexIndex in face.vertexIndices) {
                if (vertexIndex < projectedVertices.size) {
                    val proj = projectedVertices[vertexIndex]
                    val x = proj.first.toInt()
                    val y = proj.second.toInt()
                    facePoints.add(Pair(x, y))
                    
                    // Check if at least one vertex is on screen
                    if (x >= 0 && x < windowWidth && y >= 0 && y < windowHeight) {
                        allOnScreen = true
                    }
                }
            }
            
            // Skip if face has too few points or is completely off screen
            if (facePoints.size < 3 || !allOnScreen) continue
            
            // Calculate brightness based on depth (closer = brighter)
            val depthFactor = if (depth < 0) {
                // Closer to camera - brighter
                ((-depth) / 400f).coerceIn(0.5f, 1.5f)
            } else {
                // Farther from camera - dimmer
                (1f - depth / 400f).coerceIn(0.3f, 1.0f)
            }
            
            // Calculate brightness based on depth
            val brightness = depthFactor * 0.8f + 0.2f  // Base brightness
            val r = (recognizerColor.red * brightness).toInt().coerceIn(0, 255)
            val grn = (recognizerColor.green * brightness).toInt().coerceIn(0, 255)
            val b = (recognizerColor.blue * brightness).toInt().coerceIn(0, 255)
            val faceColor = Color(r, grn, b)
            
            // Draw filled polygon
            val xPoints = IntArray(facePoints.size) { facePoints[it].first }
            val yPoints = IntArray(facePoints.size) { facePoints[it].second }
            
            g.color = faceColor
            g.fillPolygon(xPoints, yPoints, facePoints.size)
            
            // Draw outline with contrasting color (white/bright yellow for contrast)
            // Use white for high contrast against cyan faces
            val outlineColor = Color(255, 255, 255)  // White for maximum contrast
            
            g.color = outlineColor
            g.stroke = java.awt.BasicStroke(3.0f,  // Thicker edges for better visibility
                                          java.awt.BasicStroke.CAP_ROUND,
                                          java.awt.BasicStroke.JOIN_ROUND)
            g.drawPolygon(xPoints, yPoints, facePoints.size)
        }
    }
    
    /**
     * Draws Tron-style grid lines.
     */
    private fun drawGridLines(g: Graphics2D, bounds: LedGrid.GridBounds) {
        g.color = Color(gridColor.red / 4, gridColor.green / 4, gridColor.blue / 4, 100)
        
        val gridSpacing = 50
        
        // Vertical lines
        var x = bounds.minX
        while (x < bounds.maxX) {
            g.drawLine(x, bounds.minY, x, bounds.maxY)
            x += gridSpacing
        }
        
        // Horizontal lines
        var y = bounds.minY
        while (y < bounds.maxY) {
            g.drawLine(bounds.minX, y, bounds.maxX, y)
            y += gridSpacing
        }
    }
    
    /**
     * Maps the offscreen image to the LED grid by sampling at LED centers.
     */
    private fun mapToLedsFromImage(canvas: BufferedImage, ledGrid: LedGrid) {
        ledGrid.clearAllLeds()
        
        val gridCount = ledGrid.gridCount
        val gridSize = ledGrid.gridSize
        val pixelSize = ledGrid.pixelSize
        
        for (gridIndex in 0 until gridCount) {
            val cfg = ledGrid.getGridConfig(gridIndex) ?: continue
            
            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    val windowX = cfg.x + x * pixelSize + pixelSize / 2
                    val windowY = cfg.y + y * pixelSize + pixelSize / 2
                    
                    if (windowX < 0 || windowX >= windowWidth || windowY < 0 || windowY >= windowHeight) {
                        continue
                    }
                    
                    val rgb = canvas.getRGB(windowX, windowY)
                    val color = Color(rgb)
                    
                    // Ignore near-black pixels to keep LEDs mostly on active areas
                    if (color.red > 5 || color.green > 5 || color.blue > 5) {
                        ledGrid.setLedColor(gridIndex, x, y, color)
                    }
                }
            }
        }
        
        ledGrid.sendToDevices()
    }
    
    override fun getName(): String = "Tron Recognizer"
    
    override fun getDescription(): String = 
        "3D Tron recognizer swooping in and out of frame"
    
    companion object {
        private val log: Logger = LogManager.getLogger(TronRecognizerAnimation::class.java)
    }
}

