package com.whiteboard.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.app.data.model.*
import com.whiteboard.app.data.repository.DiagramRepository
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class EditorViewModel(
    private val repository: DiagramRepository,
    diagramId: String?
) : ViewModel() {

    var diagram by mutableStateOf(Diagram())
        private set

    val canvasState = CanvasState()
    val undoRedoManager = UndoRedoManager()

    var showShapePicker by mutableStateOf(false)
    var showColorPicker by mutableStateOf(false)
    var showTextEditor by mutableStateOf(false)
    var editingText by mutableStateOf("")

    companion object {
        const val MIN_SHAPE_SIZE = 30f
    }

    init {
        if (diagramId != null) {
            loadDiagram(diagramId)
        }
    }

    private fun loadDiagram(id: String) {
        viewModelScope.launch {
            repository.getDiagramById(id)?.let {
                diagram = it
                canvasState.gridSize = it.gridSize
                canvasState.snapToGrid = it.snapToGrid
            }
        }
    }

    fun saveDiagram() {
        viewModelScope.launch {
            repository.saveDiagram(
                diagram.copy(
                    gridSize = canvasState.gridSize,
                    snapToGrid = canvasState.snapToGrid
                )
            )
        }
    }

    fun updateDiagramName(name: String) {
        diagram = diagram.copy(name = name)
    }

    fun addShape(type: ShapeType, position: Offset) {
        val snappedPos = canvasState.snapToGridIfEnabled(position)
        val newShape = DiagramShape(
            type = type,
            x = snappedPos.x,
            y = snappedPos.y,
            zIndex = diagram.shapes.maxOfOrNull { it.zIndex }?.plus(1) ?: 0
        )
        
        val action = DiagramAction.AddShape(newShape)
        diagram = diagram.copy(shapes = diagram.shapes + newShape)
        undoRedoManager.recordAction(action)
        
        canvasState.selectShape(newShape.id)
        canvasState.editMode = EditMode.Select
    }

    fun moveShape(shapeId: String, newPosition: Offset) {
        val shapeIndex = diagram.shapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex == -1) return
        
        val shape = diagram.shapes[shapeIndex]
        val snappedPos = canvasState.snapToGridIfEnabled(newPosition)
        val updatedShape = shape.copy(x = snappedPos.x, y = snappedPos.y)
        
        val newShapes = diagram.shapes.toMutableList()
        newShapes[shapeIndex] = updatedShape
        diagram = diagram.copy(shapes = newShapes)
    }

    fun finishMoveShape(shapeId: String, originalShape: DiagramShape) {
        val newShape = diagram.shapes.find { it.id == shapeId } ?: return
        if (originalShape.x != newShape.x || originalShape.y != newShape.y) {
            undoRedoManager.recordAction(DiagramAction.ModifyShape(originalShape, newShape))
        }
    }

    // Resize using absolute finger position on canvas
    fun resizeShapeToPosition(shapeId: String, handle: ResizeHandle, fingerPos: Offset) {
        val shapeIndex = diagram.shapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex == -1) return
        
        val shape = diagram.shapes[shapeIndex]
        
        var newX = shape.x
        var newY = shape.y
        var newWidth = shape.width
        var newHeight = shape.height
        
        // The finger position directly determines where the handle should be
        when (handle) {
            ResizeHandle.TOP_LEFT -> {
                // Finger is at top-left corner
                // Right and bottom edges stay fixed
                val right = shape.x + shape.width
                val bottom = shape.y + shape.height
                
                newX = min(fingerPos.x, right - MIN_SHAPE_SIZE)
                newY = min(fingerPos.y, bottom - MIN_SHAPE_SIZE)
                newWidth = right - newX
                newHeight = bottom - newY
            }
            ResizeHandle.TOP -> {
                // Finger controls top edge, others stay fixed
                val bottom = shape.y + shape.height
                newY = min(fingerPos.y, bottom - MIN_SHAPE_SIZE)
                newHeight = bottom - newY
            }
            ResizeHandle.TOP_RIGHT -> {
                // Left and bottom edges stay fixed
                val left = shape.x
                val bottom = shape.y + shape.height
                
                newY = min(fingerPos.y, bottom - MIN_SHAPE_SIZE)
                newWidth = max(fingerPos.x - left, MIN_SHAPE_SIZE)
                newHeight = bottom - newY
            }
            ResizeHandle.LEFT -> {
                // Right edge stays fixed
                val right = shape.x + shape.width
                newX = min(fingerPos.x, right - MIN_SHAPE_SIZE)
                newWidth = right - newX
            }
            ResizeHandle.RIGHT -> {
                // Left edge stays fixed
                newWidth = max(fingerPos.x - shape.x, MIN_SHAPE_SIZE)
            }
            ResizeHandle.BOTTOM_LEFT -> {
                // Right and top edges stay fixed
                val right = shape.x + shape.width
                val top = shape.y
                
                newX = min(fingerPos.x, right - MIN_SHAPE_SIZE)
                newWidth = right - newX
                newHeight = max(fingerPos.y - top, MIN_SHAPE_SIZE)
            }
            ResizeHandle.BOTTOM -> {
                // Top edge stays fixed
                newHeight = max(fingerPos.y - shape.y, MIN_SHAPE_SIZE)
            }
            ResizeHandle.BOTTOM_RIGHT -> {
                // Left and top edges stay fixed
                newWidth = max(fingerPos.x - shape.x, MIN_SHAPE_SIZE)
                newHeight = max(fingerPos.y - shape.y, MIN_SHAPE_SIZE)
            }
        }

        val updatedShape = shape.copy(x = newX, y = newY, width = newWidth, height = newHeight)
        val newShapes = diagram.shapes.toMutableList()
        newShapes[shapeIndex] = updatedShape
        diagram = diagram.copy(shapes = newShapes)
    }

    fun finishResizeShape(shapeId: String, originalShape: DiagramShape) {
        val newShape = diagram.shapes.find { it.id == shapeId } ?: return
        if (originalShape != newShape) {
            // Snap final position/size to grid
            val snappedShape = newShape.copy(
                x = canvasState.snapToGridIfEnabled(Offset(newShape.x, 0f)).x,
                y = canvasState.snapToGridIfEnabled(Offset(0f, newShape.y)).y,
                width = canvasState.snapSizeToGrid(newShape.width),
                height = canvasState.snapSizeToGrid(newShape.height)
            )
            
            val shapeIndex = diagram.shapes.indexOfFirst { it.id == shapeId }
            if (shapeIndex != -1) {
                val newShapes = diagram.shapes.toMutableList()
                newShapes[shapeIndex] = snappedShape
                diagram = diagram.copy(shapes = newShapes)
            }
            
            undoRedoManager.recordAction(DiagramAction.ModifyShape(originalShape, snappedShape))
        }
    }

    fun updateShapeText(shapeId: String, text: String) {
        val shapeIndex = diagram.shapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex == -1) return
        
        val shape = diagram.shapes[shapeIndex]
        val newShape = shape.copy(text = text)
        
        val newShapes = diagram.shapes.toMutableList()
        newShapes[shapeIndex] = newShape
        diagram = diagram.copy(shapes = newShapes)
        undoRedoManager.recordAction(DiagramAction.ModifyShape(shape, newShape))
    }

    fun updateShapeColor(shapeId: String, fillColor: Color, strokeColor: Color) {
        val shapeIndex = diagram.shapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex == -1) return
        
        val shape = diagram.shapes[shapeIndex]
        val newShape = shape.copy(
            fillColor = fillColor.toArgb(),
            strokeColor = strokeColor.toArgb()
        )
        
        val newShapes = diagram.shapes.toMutableList()
        newShapes[shapeIndex] = newShape
        diagram = diagram.copy(shapes = newShapes)
        undoRedoManager.recordAction(DiagramAction.ModifyShape(shape, newShape))
    }

    fun deleteSelectedShape() {
        val shapeId = canvasState.selectedShapeId ?: return
        val shape = diagram.shapes.find { it.id == shapeId } ?: return
        
        val connectedConnectors = diagram.connectors.filter { 
            it.startShapeId == shapeId || it.endShapeId == shapeId 
        }
        
        val actions = mutableListOf<DiagramAction>()
        connectedConnectors.forEach { actions.add(DiagramAction.RemoveConnector(it)) }
        actions.add(DiagramAction.RemoveShape(shape))
        
        diagram = diagram.copy(
            shapes = diagram.shapes.filter { it.id != shapeId },
            connectors = diagram.connectors.filter { 
                it.startShapeId != shapeId && it.endShapeId != shapeId 
            }
        )
        
        undoRedoManager.recordAction(DiagramAction.BatchAction(actions))
        canvasState.clearSelection()
    }

    fun startConnector(shapeId: String, anchor: AnchorPoint) {
        canvasState.startConnector(shapeId, anchor)
    }

    fun completeConnector(endShapeId: String, endAnchor: AnchorPoint) {
        val start = canvasState.completePendingConnector() ?: return
        
        if (start.first == endShapeId) return
        
        val newConnector = Connector(
            startShapeId = start.first,
            startAnchor = start.second,
            endShapeId = endShapeId,
            endAnchor = endAnchor
        )
        
        diagram = diagram.copy(connectors = diagram.connectors + newConnector)
        undoRedoManager.recordAction(DiagramAction.AddConnector(newConnector))
    }

    fun cancelConnector() {
        canvasState.cancelConnector()
    }

    fun updateConnectorStyle(connectorId: String, style: ConnectorStyle) {
        val connectorIndex = diagram.connectors.indexOfFirst { it.id == connectorId }
        if (connectorIndex == -1) return
        
        val connector = diagram.connectors[connectorIndex]
        val newConnector = connector.copy(style = style)
        
        val newConnectors = diagram.connectors.toMutableList()
        newConnectors[connectorIndex] = newConnector
        diagram = diagram.copy(connectors = newConnectors)
        undoRedoManager.recordAction(DiagramAction.ModifyConnector(connector, newConnector))
    }

    fun updateConnectorArrowHead(connectorId: String, arrowHead: ArrowHead) {
        val connectorIndex = diagram.connectors.indexOfFirst { it.id == connectorId }
        if (connectorIndex == -1) return
        
        val connector = diagram.connectors[connectorIndex]
        val newConnector = connector.copy(arrowHead = arrowHead)
        
        val newConnectors = diagram.connectors.toMutableList()
        newConnectors[connectorIndex] = newConnector
        diagram = diagram.copy(connectors = newConnectors)
        undoRedoManager.recordAction(DiagramAction.ModifyConnector(connector, newConnector))
    }

    fun deleteSelectedConnector() {
        val connectorId = canvasState.selectedConnectorId ?: return
        val connector = diagram.connectors.find { it.id == connectorId } ?: return
        
        diagram = diagram.copy(
            connectors = diagram.connectors.filter { it.id != connectorId }
        )
        undoRedoManager.recordAction(DiagramAction.RemoveConnector(connector))
        canvasState.clearSelection()
    }

    fun undo() {
        diagram = undoRedoManager.undo(diagram)
    }

    fun redo() {
        diagram = undoRedoManager.redo(diagram)
    }

    fun findShapeAt(canvasPoint: Offset): DiagramShape? {
        return diagram.shapes
            .sortedByDescending { it.zIndex }
            .firstOrNull { it.containsPoint(canvasPoint) }
    }

    fun findConnectorNear(canvasPoint: Offset, threshold: Float = 20f): Connector? {
        val shapesMap = diagram.shapes.associateBy { it.id }
        return diagram.connectors.firstOrNull { connector ->
            val start = connector.getStartPosition(shapesMap)
            val end = connector.getEndPosition(shapesMap)
            if (start != null && end != null) {
                pointToLineDistance(canvasPoint, start, end) < threshold
            } else {
                false
            }
        }
    }

    private fun pointToLineDistance(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val lineLength = (lineEnd - lineStart).getDistance()
        if (lineLength == 0f) return (point - lineStart).getDistance()

        val t = (((point.x - lineStart.x) * (lineEnd.x - lineStart.x) +
                (point.y - lineStart.y) * (lineEnd.y - lineStart.y)) / (lineLength * lineLength))
            .coerceIn(0f, 1f)

        val projection = Offset(
            lineStart.x + t * (lineEnd.x - lineStart.x),
            lineStart.y + t * (lineEnd.y - lineStart.y)
        )
        return (point - projection).getDistance()
    }

    fun getShapesMap(): Map<String, DiagramShape> = diagram.shapes.associateBy { it.id }
}
