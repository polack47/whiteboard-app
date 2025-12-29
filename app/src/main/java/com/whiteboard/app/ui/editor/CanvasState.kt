package com.whiteboard.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.whiteboard.app.data.model.*

/**
 * Manages the state of the diagram canvas including selection,
 * zoom, pan, and interaction state
 */
class CanvasState {
    // View transformation
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    // Selection state
    var selectedShapeId by mutableStateOf<String?>(null)
        private set
    var selectedConnectorId by mutableStateOf<String?>(null)
        private set

    // Interaction state
    var editMode by mutableStateOf<EditMode>(EditMode.Select)
    var isDragging by mutableStateOf(false)
        private set
    var isResizing by mutableStateOf(false)
        private set
    var activeResizeHandle by mutableStateOf<ResizeHandle?>(null)
        private set

    // Connector creation state
    var pendingConnectorStart by mutableStateOf<Pair<String, AnchorPoint>?>(null)
        private set

    // Grid settings
    var showGrid by mutableStateOf(true)
    var snapToGrid by mutableStateOf(true)
    var gridSize by mutableFloatStateOf(20f)

    fun setScale(newScale: Float) {
        scale = newScale.coerceIn(0.25f, 3f)
    }

    fun setOffset(newOffset: Offset) {
        offset = newOffset
    }

    fun zoom(factor: Float, pivot: Offset) {
        val newScale = (scale * factor).coerceIn(0.25f, 3f)
        if (newScale != scale) {
            // Adjust offset to zoom around pivot point
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

    /**
     * Convert screen coordinates to canvas coordinates
     */
    fun screenToCanvas(screenPoint: Offset): Offset {
        return (screenPoint - offset) / scale
    }

    /**
     * Convert canvas coordinates to screen coordinates
     */
    fun canvasToScreen(canvasPoint: Offset): Offset {
        return canvasPoint * scale + offset
    }

    /**
     * Snap a point to the grid if snap is enabled
     */
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

    /**
     * Snap a size to the grid if snap is enabled
     */
    fun snapSizeToGrid(size: Float): Float {
        return if (snapToGrid) {
            ((size / gridSize).toInt() * gridSize).coerceAtLeast(gridSize)
        } else {
            size.coerceAtLeast(20f)
        }
    }
}
