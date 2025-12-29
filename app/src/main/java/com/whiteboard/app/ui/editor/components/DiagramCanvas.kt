package com.whiteboard.app.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import android.graphics.Paint
import com.whiteboard.app.data.model.*
import com.whiteboard.app.ui.editor.CanvasState
import com.whiteboard.app.ui.editor.EditorViewModel

@Composable
fun DiagramCanvas(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val diagram = viewModel.diagram
    val canvasState = viewModel.canvasState
    val shapesMap = viewModel.getShapesMap()

    var dragStartShape by remember { mutableStateOf<DiagramShape?>(null) }
    var dragStartPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingConnectorEndPoint by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .pointerInput(canvasState.editMode) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (canvasState.editMode == EditMode.Pan || canvasState.editMode == EditMode.Select) {
                        if (zoom != 1f) {
                            canvasState.zoom(zoom, canvasState.screenToCanvas(centroid))
                        }
                        if (canvasState.editMode == EditMode.Pan) {
                            canvasState.pan(pan)
                        }
                    }
                }
            }
            .pointerInput(canvasState.editMode) {
                detectTapGestures(
                    onTap = { screenPos ->
                        val canvasPos = canvasState.screenToCanvas(screenPos)
                        
                        when (val mode = canvasState.editMode) {
                            is EditMode.AddShape -> {
                                viewModel.addShape(mode.type, canvasPos)
                            }
                            is EditMode.Select -> {
                                val shape = viewModel.findShapeAt(canvasPos)
                                if (shape != null) {
                                    canvasState.selectShape(shape.id)
                                } else {
                                    val connector = viewModel.findConnectorNear(canvasPos)
                                    if (connector != null) {
                                        canvasState.selectConnector(connector.id)
                                    } else {
                                        canvasState.clearSelection()
                                    }
                                }
                            }
                            is EditMode.AddConnector -> {
                                val shape = viewModel.findShapeAt(canvasPos)
                                if (shape != null) {
                                    val anchor = shape.getNearestAnchor(canvasPos)
                                    if (canvasState.pendingConnectorStart == null) {
                                        viewModel.startConnector(shape.id, anchor)
                                    } else {
                                        viewModel.completeConnector(shape.id, anchor)
                                    }
                                }
                            }
                            else -> {}
                        }
                    },
                    onDoubleTap = { screenPos ->
                        val canvasPos = canvasState.screenToCanvas(screenPos)
                        val shape = viewModel.findShapeAt(canvasPos)
                        if (shape != null) {
                            canvasState.selectShape(shape.id)
                            viewModel.editingText = shape.text
                            viewModel.showTextEditor = true
                        }
                    }
                )
            }
            .pointerInput(canvasState.editMode, canvasState.selectedShapeId) {
                detectDragGestures(
                    onDragStart = { screenPos ->
                        val canvasPos = canvasState.screenToCanvas(screenPos)
                        
                        when (canvasState.editMode) {
                            is EditMode.Select -> {
                                val selectedId = canvasState.selectedShapeId
                                if (selectedId != null) {
                                    val shape = shapesMap[selectedId]
                                    if (shape != null) {
                                        val handle = shape.getResizeHandle(canvasPos, 20f / canvasState.scale)
                                        if (handle != null) {
                                            canvasState.startResizing(handle)
                                            dragStartShape = shape
                                        } else if (shape.containsPoint(canvasPos)) {
                                            canvasState.startDragging()
                                            dragStartShape = shape
                                            dragStartPosition = canvasPos
                                        }
                                    }
                                } else {
                                    val shape = viewModel.findShapeAt(canvasPos)
                                    if (shape != null) {
                                        canvasState.selectShape(shape.id)
                                        canvasState.startDragging()
                                        dragStartShape = shape
                                        dragStartPosition = canvasPos
                                    }
                                }
                            }
                            is EditMode.Pan -> {
                                canvasState.startDragging()
                            }
                            is EditMode.AddConnector -> {
                                val shape = viewModel.findShapeAt(canvasPos)
                                if (shape != null && canvasState.pendingConnectorStart != null) {
                                    pendingConnectorEndPoint = canvasPos
                                }
                            }
                            else -> {}
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val scaledDelta = dragAmount / canvasState.scale
                        
                        when {
                            canvasState.editMode == EditMode.Pan && canvasState.isDragging -> {
                                canvasState.pan(dragAmount)
                            }
                            canvasState.isResizing -> {
                                canvasState.selectedShapeId?.let { shapeId ->
                                    canvasState.activeResizeHandle?.let { handle ->
                                        viewModel.resizeShape(shapeId, handle, scaledDelta)
                                    }
                                }
                            }
                            canvasState.isDragging -> {
                                canvasState.selectedShapeId?.let { shapeId ->
                                    val currentShape = shapesMap[shapeId]
                                    if (currentShape != null) {
                                        val newPos = Offset(
                                            currentShape.x + scaledDelta.x,
                                            currentShape.y + scaledDelta.y
                                        )
                                        viewModel.moveShape(shapeId, newPos)
                                    }
                                }
                            }
                            canvasState.editMode == EditMode.AddConnector -> {
                                val canvasPos = canvasState.screenToCanvas(change.position)
                                pendingConnectorEndPoint = canvasPos
                            }
                        }
                    },
                    onDragEnd = {
                        if (canvasState.isResizing) {
                            dragStartShape?.let { original ->
                                canvasState.selectedShapeId?.let { shapeId ->
                                    viewModel.finishResizeShape(shapeId, original)
                                }
                            }
                            canvasState.stopResizing()
                        } else if (canvasState.isDragging && canvasState.editMode == EditMode.Select) {
                            dragStartShape?.let { original ->
                                canvasState.selectedShapeId?.let { shapeId ->
                                    viewModel.finishMoveShape(shapeId, original)
                                }
                            }
                        }
                        canvasState.stopDragging()
                        dragStartShape = null
                        pendingConnectorEndPoint = null
                    }
                )
            }
    ) {
        val scale = canvasState.scale
        val offset = canvasState.offset

        // Draw grid
        if (canvasState.showGrid) {
            drawGrid(canvasState.gridSize, scale, offset)
        }

        // Draw connectors (below shapes)
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
                    
                    // Simple text wrapping
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

            // Draw handles for selected shape
            if (isSelected) {
                drawResizeHandles(shape, scale, offset)
            }

            // Draw anchor points in connector mode
            if (canvasState.editMode == EditMode.AddConnector) {
                drawAnchorPoints(shape, scale, offset)
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
    if (scaledGridSize < 5f) return // Don't draw grid if too zoomed out

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
