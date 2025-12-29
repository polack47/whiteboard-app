package com.whiteboard.app.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.UUID

/**
 * Represents different shape types available in the whiteboard
 */
enum class ShapeType {
    RECTANGLE,
    ROUNDED_RECTANGLE,
    CIRCLE,
    DIAMOND,
    PARALLELOGRAM
}

/**
 * Represents a connection point on a shape
 */
enum class AnchorPoint {
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

/**
 * Represents arrow head styles for connectors
 */
enum class ArrowHead {
    NONE,
    SINGLE,
    DOUBLE
}

/**
 * Represents connector line styles
 */
enum class ConnectorStyle {
    STRAIGHT,
    ORTHOGONAL,
    BEZIER
}

/**
 * A shape on the whiteboard
 */
data class DiagramShape(
    val id: String = UUID.randomUUID().toString(),
    val type: ShapeType = ShapeType.ROUNDED_RECTANGLE,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 150f,
    val height: Float = 80f,
    val text: String = "",
    val fillColor: Int = Color(0xFF4FC3F7).toArgb(),
    val strokeColor: Int = Color(0xFF0288D1).toArgb(),
    val textColor: Int = Color.Black.toArgb(),
    val strokeWidth: Float = 2f,
    val cornerRadius: Float = 12f,
    val zIndex: Int = 0
) {
    val bounds: Rect
        get() = Rect(x, y, x + width, y + height)

    val center: Offset
        get() = Offset(x + width / 2, y + height / 2)

    fun getAnchorPosition(anchor: AnchorPoint): Offset {
        return when (anchor) {
            AnchorPoint.TOP -> Offset(x + width / 2, y)
            AnchorPoint.BOTTOM -> Offset(x + width / 2, y + height)
            AnchorPoint.LEFT -> Offset(x, y + height / 2)
            AnchorPoint.RIGHT -> Offset(x + width, y + height / 2)
            AnchorPoint.CENTER -> center
        }
    }

    fun containsPoint(point: Offset): Boolean {
        return bounds.contains(point)
    }

    fun getResizeHandle(point: Offset, handleSize: Float = 20f): ResizeHandle? {
        val handles = mapOf(
            ResizeHandle.TOP_LEFT to Offset(x, y),
            ResizeHandle.TOP_RIGHT to Offset(x + width, y),
            ResizeHandle.BOTTOM_LEFT to Offset(x, y + height),
            ResizeHandle.BOTTOM_RIGHT to Offset(x + width, y + height),
            ResizeHandle.TOP to Offset(x + width / 2, y),
            ResizeHandle.BOTTOM to Offset(x + width / 2, y + height),
            ResizeHandle.LEFT to Offset(x, y + height / 2),
            ResizeHandle.RIGHT to Offset(x + width, y + height / 2)
        )

        for ((handle, pos) in handles) {
            if ((point - pos).getDistance() <= handleSize) {
                return handle
            }
        }
        return null
    }

    fun getNearestAnchor(point: Offset): AnchorPoint {
        val anchors = AnchorPoint.entries.filter { it != AnchorPoint.CENTER }
        return anchors.minByOrNull { (getAnchorPosition(it) - point).getDistance() } ?: AnchorPoint.TOP
    }
}

/**
 * Resize handles for shapes
 */
enum class ResizeHandle {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
}

/**
 * A connector between two shapes or points
 */
data class Connector(
    val id: String = UUID.randomUUID().toString(),
    val startShapeId: String? = null,
    val startAnchor: AnchorPoint = AnchorPoint.RIGHT,
    val startPoint: Offset? = null, // Used if not connected to a shape
    val endShapeId: String? = null,
    val endAnchor: AnchorPoint = AnchorPoint.LEFT,
    val endPoint: Offset? = null, // Used if not connected to a shape
    val style: ConnectorStyle = ConnectorStyle.ORTHOGONAL,
    val arrowHead: ArrowHead = ArrowHead.SINGLE,
    val color: Int = Color(0xFF424242).toArgb(),
    val strokeWidth: Float = 2f,
    val zIndex: Int = -1
) {
    fun getStartPosition(shapes: Map<String, DiagramShape>): Offset? {
        return if (startShapeId != null) {
            shapes[startShapeId]?.getAnchorPosition(startAnchor)
        } else {
            startPoint
        }
    }

    fun getEndPosition(shapes: Map<String, DiagramShape>): Offset? {
        return if (endShapeId != null) {
            shapes[endShapeId]?.getAnchorPosition(endAnchor)
        } else {
            endPoint
        }
    }
}

/**
 * Represents the entire diagram
 */
data class Diagram(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val shapes: List<DiagramShape> = emptyList(),
    val connectors: List<Connector> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val gridSize: Float = 20f,
    val snapToGrid: Boolean = true
)

/**
 * Current editing mode
 */
sealed class EditMode {
    data object Select : EditMode()
    data class AddShape(val type: ShapeType) : EditMode()
    data object AddConnector : EditMode()
    data object Pan : EditMode()
}

/**
 * Represents an action for undo/redo
 */
sealed class DiagramAction {
    data class AddShape(val shape: DiagramShape) : DiagramAction()
    data class RemoveShape(val shape: DiagramShape) : DiagramAction()
    data class ModifyShape(val oldShape: DiagramShape, val newShape: DiagramShape) : DiagramAction()
    data class AddConnector(val connector: Connector) : DiagramAction()
    data class RemoveConnector(val connector: Connector) : DiagramAction()
    data class ModifyConnector(val oldConnector: Connector, val newConnector: Connector) : DiagramAction()
    data class BatchAction(val actions: List<DiagramAction>) : DiagramAction()
}
