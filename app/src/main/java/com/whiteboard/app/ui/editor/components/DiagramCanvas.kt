package com.whiteboard.app.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import android.graphics.Paint
import com.whiteboard.app.data.model.*
import com.whiteboard.app.ui.editor.EditorViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun DiagramCanvas(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val diagram = viewModel.diagram
    val canvasState = viewModel.canvasState
    
    var dragStartShape by remember { mutableStateOf<DiagramShape?>(null) }
    var pendingConnectorEndPoint by remember { mutableStateOf<Offset?>(null) }
    var currentResizeHandle by remember { mutableStateOf<ResizeHandle?>(null) }
    var isDraggingShape by remember { mutableStateOf(false) }
    var isResizingShape by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .pointerInput(canvasState.editMode, canvasState.selectedShapeId, diagram.shapes) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val firstDownPos = firstDown.position
                    val canvasPos = canvasState.screenToCanvas(firstDownPos)
                    
                    // Reset states
                    isDraggingShape = false
                    isResizingShape = false
                    currentResizeHandle = null
                    dragStartShape = null
                    
                    var hasMoved = false
                    var totalDrag = Offset.Zero
                    var lastPosition = firstDownPos
                    var isMultiTouch = false
                    
                    // Get current shapes map
                    val shapesMap = viewModel.getShapesMap()
                    
                    // Find what we're touching
                    val touchedShape = viewModel.findShapeAt(canvasPos)
                    val selectedShape = canvasState.selectedShapeId?.let { shapesMap[it] }
                    
                    // Check if touching resize handle of selected shape
                    if (selectedShape != null && canvasState.editMode == EditMode.Select) {
                        val handle = selectedShape.getResizeHandle(canvasPos, 60f / canvasState.scale)
                        if (handle != null) {
                            currentResizeHandle = handle
                            dragStartShape = selectedShape
                            isResizingShape = true
                        }
                    }
                    
                    // If not resizing, check if we should drag a shape
                    if (!isResizingShape && touchedShape != null && canvasState.editMode == EditMode.Select) {
                        canvasState.selectShape(touchedShape.id)
                        dragStartShape = touchedShape
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.filter { it.pressed }
                        
                        if (pointers.isEmpty()) break
                        
                        // Multi-touch: zoom and pan
                        if (pointers.size >= 2) {
                            isMultiTouch = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            
                            if (zoom != 1f) {
                                canvasState.zoom(zoom, canvasState.screenToCanvas(centroid))
                            }
                            canvasState.pan(pan)
                            
                            pointers.forEach { it.consume() }
                        } 
                        // Single touch
                        else if (pointers.size == 1 && !isMultiTouch) {
                            val change = pointers.first()
                            val currentPos = change.position
                            val currentCanvasPos = canvasState.screenToCanvas(currentPos)
                            val delta = currentPos - lastPosition
                            totalDrag += delta
                            
                            // Start dragging after 8 pixels movement
                            if (!hasMoved && totalDrag.getDistance() > 8f) {
                                hasMoved = true
                                
                                if (!isResizingShape && dragStartShape != null) {
                                    isDraggingShape = true
                                    canvasState.startDragging()
                                } else if (canvasState.editMode == EditMode.Pan) {
                                    canvasState.startDragging()
                                }
                            }
                            
                            // Handle ongoing movement
                            if (hasMoved && change.positionChanged()) {
                                when {
                                    // Resizing shape - use absolute finger position
                                    isResizingShape && currentResizeHandle != null -> {
                                        canvasState.selectedShapeId?.let { shapeId ->
                                            viewModel.resizeShapeToPosition(shapeId, currentResizeHandle!!, currentCanvasPos)
                                        }
                                        change.consume()
                                    }
                                    // Pan mode - move canvas
                                    canvasState.editMode == EditMode.Pan -> {
                                        canvasState.pan(delta)
                                        change.consume()
                                    }
                                    // Select mode - move shape
                                    isDraggingShape && canvasState.selectedShapeId != null -> {
                                        val currentShapesMap = viewModel.getShapesMap()
                                        val currentShape = currentShapesMap[canvasState.selectedShapeId]
                                        if (currentShape != null) {
                                            val scaledDelta = delta / canvasState.scale
                                            val newPos = Offset(
                                                currentShape.x + scaledDelta.x,
                                                currentShape.y + scaledDelta.y
                                            )
                                            viewModel.moveShape(canvasState.selectedShapeId!!, newPos)
                                        }
                                        change.consume()
                                    }
                                    // Connector mode
                                    canvasState.editMode == EditMode.AddConnector && canvasState.pendingConnectorStart != null -> {
                                        pendingConnectorEndPoint = currentCanvasPos
                                        change.consume()
                                    }
                                }
                            }
                            
                            lastPosition = currentPos
                        }
                    } while (event.changes.any { it.pressed })
                    
                    // Gesture ended
                    val endCanvasPos = canvasState.screenToCanvas(lastPosition)
                    
                    // Handle tap (no significant movement and no multi-touch)
                    if (!hasMoved && !isMultiTouch) {
                        when (val mode = canvasState.editMode) {
                            is EditMode.AddShape -> {
                                viewModel.addShape(mode.type, endCanvasPos)
                            }
                            is EditMode.Select -> {
                                val shape = viewModel.findShapeAt(endCanvasPos)
                                if (shape != null) {
                                    canvasState.selectShape(shape.id)
                                } else {
                                    val connector = viewModel.findConnectorNear(endCanvasPos)
                                    if (connector != null) {
                                        canvasState.selectConnector(connector.id)
                                    } else {
                                        canvasState.clearSelection()
                                    }
                                }
                            }
                            is EditMode.Pan -> {
                                val shape = viewModel.findShapeAt(endCanvasPos)
                                if (shape != null) {
                                    canvasState.selectShape(shape.id)
                                } else {
                                    canvasState.clearSelection()
                                }
                            }
                            is EditMode.AddConnector -> {
                                val shape = viewModel.findShapeAt(endCanvasPos)
                                if (shape != null) {
                                    val anchor = shape.getNearestAnchor(endCanvasPos)
                                    if (canvasState.pendingConnectorStart == null) {
                                        viewModel.startConnector(shape.id, anchor)
                                    } else {
                                        viewModel.completeConnector(shape.id, anchor)
                                    }
                                }
                            }
                        }
                    }
                    
                    // End resize
                    if (isResizingShape) {
                        dragStartShape?.let { original ->
                            canvasState.selectedShapeId?.let { shapeId ->
                                viewModel.finishResizeShape(shapeId, original)
                            }
                        }
                    }
                    
                    // End shape drag
                    if (isDraggingShape) {
                        dragStartShape?.let { original ->
                            canvasState.selectedShapeId?.let { shapeId ->
                                viewModel.finishMoveShape(shapeId, original)
                            }
                        }
                    }
                    
                    // Cleanup
                    canvasState.stopDragging()
                    canvasState.stopResizing()
                    dragStartShape = null
                    pendingConnectorEndPoint = null
                    isDraggingShape = false
                    isResizingShape = false
                    currentResizeHandle = null
                }
            }
    ) {
        val scale = canvasState.scale
        val offset = canvasState.offset
        val shapesMap = viewModel.getShapesMap()

        // Draw grid
        if (canvasState.showGrid) {
            drawGrid(canvasState.gridSize, scale, offset)
        }

        // Draw connectors
        diagram.connectors.sortedBy { it.zIndex }.forEach { connector ->
            drawConnector(
                connector = connector,
                shapes = shapesMap,
                scale = scale,
                offset = offset,
                isSelected = connector.id == canvasState.selectedConnectorId
            )
        }

        // Draw pending connector
        canvasState.pendingConnectorStart?.let { (startId, startAnchor) ->
            pendingConnectorEndPoint?.let { endPoint ->
                val pendingConnector = Connector(
                    startShapeId = startId,
                    startAnchor = startAnchor,
                    endPoint = endPoint
                )
                drawConnector(
                    connector = pendingConnector,
                    shapes = shapesMap,
                    scale = scale,
                    offset = offset,
                    isPending = true,
                    pendingEndPoint = endPoint
                )
            }
        }

        // Draw shapes
        diagram.shapes.sortedBy { it.zIndex }.forEach { shape ->
            val isSelected = shape.id == canvasState.selectedShapeId
            
            drawDiagramShape(
                shape = shape,
                scale = scale,
                offset = offset,
                isSelected = isSelected
            )

            // Draw text - auto-sizing to fit shape
            if (shape.text.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.apply {
                    val shapeScreenWidth = shape.width * scale * 0.85f
                    val shapeScreenHeight = shape.height * scale * 0.7f
                    
                    val centerX = shape.x * scale + offset.x + shape.width * scale / 2
                    val centerY = shape.y * scale + offset.y + shape.height * scale / 2
                    
                    // Calculate optimal font size
                    val baseFontSize = 16f * scale
                    val minFontSize = 6f * scale
                    var fontSize = baseFontSize
                    
                    val textPaint = Paint().apply {
                        color = shape.textColor
                        textSize = fontSize
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    
                    // Function to wrap text and calculate dimensions
                    fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
                        val words = text.split(" ")
                        val lines = mutableListOf<String>()
                        var currentLine = ""
                        
                        words.forEach { word ->
                            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                            if (paint.measureText(testLine) <= maxWidth) {
                                currentLine = testLine
                            } else {
                                if (currentLine.isNotEmpty()) lines.add(currentLine)
                                currentLine = word
                                // If single word is too long, add it anyway
                                if (paint.measureText(word) > maxWidth) {
                                    lines.add(currentLine)
                                    currentLine = ""
                                }
                            }
                        }
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        return lines
                    }
                    
                    // Reduce font size until text fits
                    var lines: List<String>
                    do {
                        textPaint.textSize = fontSize
                        lines = wrapText(shape.text, shapeScreenWidth, textPaint)
                        val totalTextHeight = lines.size * fontSize * 1.2f
                        
                        if (totalTextHeight <= shapeScreenHeight && 
                            lines.all { textPaint.measureText(it) <= shapeScreenWidth }) {
                            break
                        }
                        fontSize -= 1f
                    } while (fontSize > minFontSize)
                    
                    // Draw the text
                    val lineHeight = fontSize * 1.2f
                    val totalHeight = lines.size * lineHeight
                    val startY = centerY - totalHeight / 2 + lineHeight * 0.7f
                    
                    lines.forEachIndexed { index, line ->
                        drawText(line, centerX, startY + index * lineHeight, textPaint)
                    }
                }
            }

            // Draw resize handles for selected shape
            if (isSelected) {
                drawResizeHandles(shape, scale, offset, handleSize = 24f)
            }

            // Draw anchor points in connector mode
            if (canvasState.editMode == EditMode.AddConnector) {
                drawAnchorPoints(shape, scale, offset, anchorSize = 18f)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    gridSize: Float,
    scale: Float,
    offset: Offset
) {
    val scaledGridSize = gridSize * scale
    if (scaledGridSize < 5f) return

    val startX = (offset.x % scaledGridSize) - scaledGridSize
    val startY = (offset.y % scaledGridSize) - scaledGridSize

    val gridColor = Color(0xFFE0E0E0)
    val majorGridColor = Color(0xFFBDBDBD)
    val majorGridInterval = 5

    var gridIndex = 0
    var x = startX
    while (x < size.width + scaledGridSize) {
        val isMajor = gridIndex % majorGridInterval == 0
        drawLine(
            color = if (isMajor) majorGridColor else gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = if (isMajor) 1f else 0.5f
        )
        x += scaledGridSize
        gridIndex++
    }

    gridIndex = 0
    var y = startY
    while (y < size.height + scaledGridSize) {
        val isMajor = gridIndex % majorGridInterval == 0
        drawLine(
            color = if (isMajor) majorGridColor else gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = if (isMajor) 1f else 0.5f
        )
        y += scaledGridSize
        gridIndex++
    }
}
