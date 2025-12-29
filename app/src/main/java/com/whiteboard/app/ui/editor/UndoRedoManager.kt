package com.whiteboard.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.whiteboard.app.data.model.*

/**
 * Manages undo/redo functionality for diagram editing
 */
class UndoRedoManager(private val maxHistorySize: Int = 50) {
    private val undoStack = mutableListOf<DiagramAction>()
    private val redoStack = mutableListOf<DiagramAction>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun recordAction(action: DiagramAction) {
        undoStack.add(action)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateState()
    }

    fun undo(diagram: Diagram): Diagram {
        if (undoStack.isEmpty()) return diagram

        val action = undoStack.removeLast()
        redoStack.add(action)
        updateState()
        return reverseAction(diagram, action)
    }

    fun redo(diagram: Diagram): Diagram {
        if (redoStack.isEmpty()) return diagram

        val action = redoStack.removeLast()
        undoStack.add(action)
        updateState()
        return applyAction(diagram, action)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }

    private fun updateState() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private fun applyAction(diagram: Diagram, action: DiagramAction): Diagram {
        return when (action) {
            is DiagramAction.AddShape -> diagram.copy(
                shapes = diagram.shapes + action.shape
            )
            is DiagramAction.RemoveShape -> diagram.copy(
                shapes = diagram.shapes.filter { it.id != action.shape.id },
                connectors = diagram.connectors.filter { 
                    it.startShapeId != action.shape.id && it.endShapeId != action.shape.id 
                }
            )
            is DiagramAction.ModifyShape -> diagram.copy(
                shapes = diagram.shapes.map { 
                    if (it.id == action.newShape.id) action.newShape else it 
                }
            )
            is DiagramAction.AddConnector -> diagram.copy(
                connectors = diagram.connectors + action.connector
            )
            is DiagramAction.RemoveConnector -> diagram.copy(
                connectors = diagram.connectors.filter { it.id != action.connector.id }
            )
            is DiagramAction.ModifyConnector -> diagram.copy(
                connectors = diagram.connectors.map { 
                    if (it.id == action.newConnector.id) action.newConnector else it 
                }
            )
            is DiagramAction.BatchAction -> {
                var result = diagram
                for (a in action.actions) {
                    result = applyAction(result, a)
                }
                result
            }
        }
    }

    private fun reverseAction(diagram: Diagram, action: DiagramAction): Diagram {
        return when (action) {
            is DiagramAction.AddShape -> diagram.copy(
                shapes = diagram.shapes.filter { it.id != action.shape.id },
                connectors = diagram.connectors.filter { 
                    it.startShapeId != action.shape.id && it.endShapeId != action.shape.id 
                }
            )
            is DiagramAction.RemoveShape -> diagram.copy(
                shapes = diagram.shapes + action.shape
            )
            is DiagramAction.ModifyShape -> diagram.copy(
                shapes = diagram.shapes.map { 
                    if (it.id == action.oldShape.id) action.oldShape else it 
                }
            )
            is DiagramAction.AddConnector -> diagram.copy(
                connectors = diagram.connectors.filter { it.id != action.connector.id }
            )
            is DiagramAction.RemoveConnector -> diagram.copy(
                connectors = diagram.connectors + action.connector
            )
            is DiagramAction.ModifyConnector -> diagram.copy(
                connectors = diagram.connectors.map { 
                    if (it.id == action.oldConnector.id) action.oldConnector else it 
                }
            )
            is DiagramAction.BatchAction -> {
                var result = diagram
                for (a in action.actions.reversed()) {
                    result = reverseAction(result, a)
                }
                result
            }
        }
    }
}
