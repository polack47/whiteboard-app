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

    private val shapesMapInternalInternal: Map<String, DiagramShape>
    get() = diagram.shapes.associateBy { it.id }

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

    // Shape operations
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
        val shape = shapesMapInternal[shapeId] ?: return
        val snappedPos = canvasState.snapToGridIfEnabled(newPosition)
        
        val updatedShape = shape.copy(x = snappedPos.x, y = snappedPos.y)
        diagram = diagram.copy(
            shapes = diagram.shapes.map { if (it.id == shapeId) updatedShape else it }
        )
    }

    fun finishMoveShape(shapeId: String, originalShape: DiagramShape) {
        val newShape = shapesMapInternal[shapeId] ?: return
        if (originalShape.x != newShape.x || originalShape.y != newShape.y) {
            undoRedoManager.recordAction(DiagramAction.ModifyShape(originalShape, newShape))
        }
    }

    fun resizeShape(shapeId: String, handle: ResizeHandle, delta: Offset) {
        val shape = shapesMapInternal[shapeId] ?: return
        
        var newX = shape.x
        var newY = shape.y
        var newWidth = shape.width
        var newHeight = shape.height

        when (handle) {
            ResizeHandle.TOP_LEFT -> {
                newX += delta.x
                newY += delta.y
                newWidth -= delta.x
                newHeight -= delta.y
            }
            ResizeHandle.TOP -> {
                newY += delta.y
                newHeight -= delta.y
            }
            ResizeHandle.TOP_RIGHT -> {
                newY += delta.y
                newWidth += delta.x
                newHeight -= delta.y
            }
            ResizeHandle.LEFT -> {
                newX += delta.x
                newWidth -= delta.x
            }
            ResizeHandle.RIGHT -> {
                newWidth += delta.x
            }
            ResizeHandle.BOTTOM_LEFT -> {
                newX += delta.x
                newWidth -= delta.x
                newHeight += delta.y
            }
            ResizeHandle.BOTTOM -> {
                newHeight += delta.y
            }
            ResizeHandle.BOTTOM_RIGHT -> {
                newWidth += delta.x
                newHeight += delta.y
            }
        }

        // Ensure minimum size
        val minSize = canvasState.gridSize
        if (newWidth < minSize) {
            if (handle in listOf(ResizeHandle.LEFT, ResizeHandle.TOP_LEFT, ResizeHandle.BOTTOM_LEFT)) {
                newX = shape.x + shape.width - minSize
            }
            newWidth = minSize
        }
        if (newHeight < minSize) {
            if (handle in listOf(ResizeHandle.TOP, ResizeHandle.TOP_LEFT, ResizeHandle.TOP_RIGHT)) {
                newY = shape.y + shape.height - minSize
            }
            newHeight = minSize
        }

        // Snap to grid
        newX = canvasState.snapToGridIfEnabled(Offset(newX, 0f)).x
        newY = canvasState.snapToGridIfEnabled(Offset(0f, newY)).y
        newWidth = canvasState.snapSizeToGrid(newWidth)
        newHeight = canvasState.snapSizeToGrid(newHeight)

        val updatedShape = shape.copy(x = newX, y = newY, width = newWidth, height = newHeight)
        diagram = diagram.copy(
            shapes = diagram.shapes.map { if (it.id == shapeId) updatedShape else it }
        )
    }

    fun finishResizeShape(shapeId: String, originalShape: DiagramShape) {
        val newShape = shapesMapInternal[shapeId] ?: return
        if (originalShape != newShape) {
            undoRedoManager.recordAction(DiagramAction.ModifyShape(originalShape, newShape))
        }
    }

    fun updateShapeText(shapeId: String, text: String) {
        val shape = shapesMapInternal[shapeId] ?: return
        val oldShape = shape
        val newShape = shape.copy(text = text)
        
        diagram = diagram.copy(
            shapes = diagram.shapes.map { if (it.id == shapeId) newShape else it }
        )
        undoRedoManager.recordAction(DiagramAction.ModifyShape(oldShape, newShape))
    }

    fun updateShapeColor(shapeId: String, fillColor: Color, strokeColor: Color) {
        val shape = shapesMapInternal[shapeId] ?: return
        val oldShape = shape
        val newShape = shape.copy(
            fillColor = fillColor.toArgb(),
            strokeColor = strokeColor.toArgb()
        )
        
        diagram = diagram.copy(
            shapes = diagram.shapes.map { if (it.id == shapeId) newShape else it }
        )
        undoRedoManager.recordAction(DiagramAction.ModifyShape(oldShape, newShape))
    }

    fun deleteSelectedShape() {
        val shapeId = canvasState.selectedShapeId ?: return
        val shape = shapesMapInternal[shapeId] ?: return
        
        // Also remove connected connectors
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

    // Connector operations
    fun startConnector(shapeId: String, anchor: AnchorPoint) {
        canvasState.startConnector(shapeId, anchor)
    }

    fun completeConnector(endShapeId: String, endAnchor: AnchorPoint) {
        val start = canvasState.completePendingConnector() ?: return
        
        // Don't connect to same shape
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
        val connector = diagram.connectors.find { it.id == connectorId } ?: return
        val oldConnector = connector
        val newConnector = connector.copy(style = style)
        
        diagram = diagram.copy(
            connectors = diagram.connectors.map { if (it.id == connectorId) newConnector else it }
        )
        undoRedoManager.recordAction(DiagramAction.ModifyConnector(oldConnector, newConnector))
    }

    fun updateConnectorArrowHead(connectorId: String, arrowHead: ArrowHead) {
        val connector = diagram.connectors.find { it.id == connectorId } ?: return
        val oldConnector = connector
        val newConnector = connector.copy(arrowHead = arrowHead)
        
        diagram = diagram.copy(
            connectors = diagram.connectors.map { if (it.id == connectorId) newConnector else it }
        )
        undoRedoManager.recordAction(DiagramAction.ModifyConnector(oldConnector, newConnector))
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

    // Undo/Redo
    fun undo() {
        diagram = undoRedoManager.undo(diagram)
    }

    fun redo() {
        diagram = undoRedoManager.redo(diagram)
    }

    // Hit testing
    fun findShapeAt(canvasPoint: Offset): DiagramShape? {
        return diagram.shapes
            .sortedByDescending { it.zIndex }
            .firstOrNull { it.containsPoint(canvasPoint) }
    }

    fun findConnectorNear(canvasPoint: Offset, threshold: Float = 15f): Connector? {
        return diagram.connectors.firstOrNull { connector ->
            val start = connector.getStartPosition(shapesMapInternal)
            val end = connector.getEndPosition(shapesMapInternal)
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

    fun getShapesMap(): Map<String, DiagramShape> = shapesMapInternal
}
