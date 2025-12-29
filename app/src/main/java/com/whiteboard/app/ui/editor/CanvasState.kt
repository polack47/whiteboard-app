package com.whiteboard.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.whiteboard.app.data.model.*

class CanvasState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    var selectedShapeId by mutableStateOf<String?>(null)
    var selectedConnectorId by mutableStateOf<String?>(null)

    var editMode by mutableStateOf<EditMode>(EditMode.Select)
    var isDragging by mutableStateOf(false)
    var isResizing by mutableStateOf(false)
    var activeResizeHandle by mutableStateOf<ResizeHandle?>(null)

    var pendingConnectorStart by mutableStateOf<Pair<String, AnchorPoint>?>(null)

    var showGrid by mutableStateOf(true)
    var snapToGrid by mutableStateOf(true)
    var gridSize by mutableFloatStateOf(20f)

    fun updateScale(newScale: Float) {
        scale = newScale.coerceIn(0.25f, 3f)
    }

    fun updateOffset(newOffset: Offset) {
        offset = newOffset
    }

    fun zoom(factor: Float, pivot: Offset) {
        val newScale = (scale * factor).coerceIn(0.25f, 3f)
        if (newScale != scale) {
            val scaleDiff = newScale - scale
            offset = Offset(
                offset.x - pivot.x * scaleDiff,
                offset.y - pivot.y * scaleDiff
            )
            scale = newScale
        }
    }

    fun pan(delta: Offset) {
        offset += delta
    }

    fun selectShape(shapeId: String?) {
        selectedShapeId = shapeId
        selectedConnectorId = null
    }

    fun selectConnector(connectorId: String?) {
        selectedConnectorId = connectorId
        selectedShapeId = null
    }

    fun clearSelection() {
        selectedShapeId = null
        selectedConnectorId = null
    }

    fun startDragging() {
        isDragging = true
    }

    fun stopDragging() {
        isDragging = false
    }

    fun startResizing(handle: ResizeHandle) {
        isResizing = true
        activeResizeHandle = handle
    }

    fun stopResizing() {
        isResizing = false
        activeResizeHandle = null
    }

    fun startConnector(shapeId: String, anchor: AnchorPoint) {
        pendingConnectorStart = shapeId to anchor
    }

    fun cancelConnector() {
        pendingConnectorStart = null
    }

    fun completePendingConnector(): Pair<String, AnchorPoint>? {
        val result = pendingConnectorStart
        pendingConnectorStart = null
        return result
    }

    fun screenToCanvas(screenPoint: Offset): Offset {
        return (screenPoint - offset) / scale
    }

    fun canvasToScreen(canvasPoint: Offset): Offset {
        return canvasPoint * scale + offset
    }

    fun snapToGridIfEnabled(point: Offset): Offset {
        return if (snapToGrid) {
            Offset(
                (point.x / gridSize).toInt() * gridSize,
                (point.y / gridSize).toInt() * gridSize
            )
        } else {
            point
        }
    }

    fun snapSizeToGrid(size: Float): Float {
        return if (snapToGrid) {
            ((size / gridSize).toInt() * gridSize).coerceAtLeast(gridSize)
        } else {
            size.coerceAtLeast(20f)
        }
    }
}
