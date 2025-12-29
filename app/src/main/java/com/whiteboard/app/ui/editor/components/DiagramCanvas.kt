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
import kotlin.math.abs

@Composable
fun DiagramCanvas(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val diagram = viewModel.diagram
    val canvasState = viewModel.canvasState
    val shapesMap = viewModel.getShapesMap()

    var dragStartShape by remember { mutableStateOf<DiagramShape?>(null) }
    var pendingConnectorEndPoint by remember { mutableStateOf<Offset?>(null) }
    var activePointerId by remember { mutableStateOf<Long?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .pointerInput(canvasState.editMode, canvasState.selectedShapeId) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val firstDownPos = firstDown.position
                    val canvasPos = canvasState.screenToCanvas(firstDownPos)
                    activePointerId = firstDown.id.value
                    
                    var isDragging = false
                    var isZooming = false
                    var hasMoved = false
                    var totalDrag = Offset.Zero
                    var lastPosition = firstDownPos
                    var currentResizeHandle: ResizeHandle? = null
                    
                    // Check what we're touching
                    val touchedShape = viewModel.findShapeAt(canvasPos)
                    val selectedShape = canvasState.selectedShapeId?.let { shapesMap[it] }
                    
                    // Check resize handle if shape is selected
                    if (selectedShape != null && canvasState.editMode == EditMode.Select) {
                        currentResizeHandle = selectedShape.getResizeHandle(canvasPos, 50f / canvasState.scale)
                        if (currentResizeHandle != null) {
                            dragStartShape = selectedShape
                            canvasState.startResizing(currentResizeHandle)
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.filter { it.pressed }
                        
                        if (pointers.isEmpty()) break
                        
                        // Multi-touch: zoom and pan
                        if (pointers.size >= 2) {
                            isZooming = true
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
                        else if (pointers.size == 1) {
                            val change = pointers.first()
                            val currentPos = change.position
                            val delta = currentPos - lastPosition
                            totalDrag += delta
                            
                            // Consider it a drag after moving 10 pixels
                            if (!hasMoved && totalDrag.getDistance() > 10f) {
                                hasMoved = true
                                isDragging = true
                                
                                // Start appropriate drag action
                                when (canvasState.editMode) {
                                    is EditMode.Select -> {
                                        if (currentResizeHandle == null) {
                                            if (touchedShape != null) {
                                                canvasState.selectShape(touchedShape.id)
                                                dragStartShape = touchedShape
                                                canvasState.startDragging()
                                            }
                                        }
                                    }
                                    is EditMode.Pan -> {
                                        canvasState.startDragging()
                                    }
                                    else -> {}
                                }
                            }
                            
                            // Handle ongoing drag
                            if (hasMoved && change.positionChanged()) {
                                when {
                                    // Resizing shape
                                    canvasState.isResizing && currentResizeHandle != null -> {
                                        val scaledDelta = delta / canvasState.scale
                                        canvasState.selectedShapeId?.let { shapeId ->
                                            viewModel.resizeShape(shapeId, currentResizeHandle, scaledDelta)
                                        }
                                    }
                                    // Pan mode - move canvas
                                    canvasState.editMode == EditMode.Pan -> {
                                        canvasState.pan(delta)
                                    }
                                    // Select mode - move shape
                                    canvasState.editMode == EditMode.Select && canvasState.isDragging -> {
                                        canvasState.selectedShapeId?.let { shapeId ->
                                            val currentShape = shapesMap[shapeId]
                                            if (currentShape != null) {
                                                val scaledDelta = delta / canvasState.scale
                                                val newPos = Offset(
                                                    currentShape.x + scaledDelta.x,
                                                    currentShape.y + scaledDelta.y
                                                )
                                                viewModel.moveShape(shapeId, newPos)
                                            }
                                        }
                                    }
                                    // Connector mode
                                    canvasState.editMode == EditMode.AddConnector -> {
                                        pendingConnectorEndPoint = canvasState.screenToCanvas(currentPos)
                                    }
                                }
                                change.consume()
                            }
                            
                            lastPosition = currentPos
                        }
                    } while (event.changes.any { it.pressed })
                    
                    // Gesture ended
                    val endCanvasPos = canvasState.screenToCanvas(lastPosition)
                    
                    // Handle tap (no significant movement)
                    if (!hasMoved && !isZooming) {
                        when (val mode = canvasState.editMode) {
                            is EditMode.AddShape -> {
                                viewModel.addShape(mode.type, endCanvasPos)
                            }
                            is EditMode.Select, is EditMode.Pan -> {
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
                    if (canvasState.isResizing) {
                        dragStartShape?.let { original ->
                            canvasState.selectedShapeId?.let { shapeId ->
                                viewModel.finishResizeShape(shapeId, original)
                            }
                        }
                        canvasState.stopResizing()
                    }
                    
                    // End drag
                    if (canvasState.isDragging && canvasState.editMode == EditMode.Select) {
                        dragStartShape?.let { original ->
                            canvasState.selectedShapeId?.let { shapeId ->
                                viewModel.finishMoveShape(shapeId, original)
                            }
                        }
                    }
                    
                    canvasState.stopDragging()
                    dragStartShape = null
                    pendingConnectorEndPoint = null
                    activePointerId = null
                }
            }
    ) {
        val scale = canvasState.scale
        val offset = canvasState.offset

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

            // Draw text
            if (shape.text.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        color = shape.textColor
                        textSize = 14f * scale
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    
                    val centerX = shape.x * scale + offset.x + shape.width * scale / 2
                    val centerY = shape.y * scale + offset.y + shape.height * scale / 2
                    
                    val maxWidth = shape.width * scale * 0.9f
                    val words = shape.text.split(" ")
                    val lines = mutableListOf<String>()
                    var currentLine = ""
                    
                    words.forEach { word ->
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (textPaint.measureText(testLine) <= maxWidth) {
                            currentLine = testLine
                        } else {
                            if (currentLine.isNotEmpty()) lines.add(currentLine)
                            currentLine = word
                        }
                    }
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    
                    val lineHeight = textPaint.textSize * 1.2f
                    val totalHeight = lines.size * lineHeight
                    val startY = centerY - totalHeight / 2 + lineHeight * 0.7f
                    
                    lines.forEachIndexed { index, line ->
                        drawText(line, centerX, startY + index * lineHeight, textPaint)
                    }
                }
            }

            // Draw handles for selected shape (larger for touch)
            if (isSelected) {
                drawResizeHandles(shape, scale, offset, handleSize = 20f)
            }

            // Draw anchor points in connector mode
            if (canvasState.editMode == EditMode.AddConnector) {
                drawAnchorPoints(shape, scale, offset, anchorSize = 16f)
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
