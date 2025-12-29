package com.whiteboard.app.ui.editor.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.whiteboard.app.data.model.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draw a connector between shapes
 */
fun DrawScope.drawConnector(
    connector: Connector,
    shapes: Map<String, DiagramShape>,
    scale: Float,
    offset: Offset,
    isSelected: Boolean = false,
    isPending: Boolean = false,
    pendingEndPoint: Offset? = null
) {
    val startPos = connector.getStartPosition(shapes)
    val endPos = if (isPending && pendingEndPoint != null) {
        pendingEndPoint
    } else {
        connector.getEndPosition(shapes)
    }

    if (startPos == null || endPos == null) return

    val screenStart = Offset(startPos.x * scale + offset.x, startPos.y * scale + offset.y)
    val screenEnd = Offset(endPos.x * scale + offset.x, endPos.y * scale + offset.y)
    val color = if (isPending) Color(0xFF9E9E9E) else Color(connector.color)
    val strokeWidth = connector.strokeWidth * scale

    when (connector.style) {
        ConnectorStyle.STRAIGHT -> {
            drawStraightConnector(screenStart, screenEnd, color, strokeWidth, connector.arrowHead, isSelected)
        }
        ConnectorStyle.ORTHOGONAL -> {
            drawOrthogonalConnector(
                screenStart, screenEnd, 
                connector.startAnchor, connector.endAnchor,
                color, strokeWidth, connector.arrowHead, isSelected
            )
        }
        ConnectorStyle.BEZIER -> {
            drawBezierConnector(
                screenStart, screenEnd,
                connector.startAnchor, connector.endAnchor,
                color, strokeWidth, connector.arrowHead, isSelected
            )
        }
    }
}

private fun DrawScope.drawStraightConnector(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    arrowHead: ArrowHead,
    isSelected: Boolean
) {
    val strokeStyle = Stroke(
        width = strokeWidth,
        pathEffect = if (isSelected) PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null
    )

    drawLine(
        color = if (isSelected) Color(0xFF2196F3) else color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )

    drawArrowHead(end, start, color, strokeWidth, arrowHead)
}

private fun DrawScope.drawOrthogonalConnector(
    start: Offset,
    end: Offset,
    startAnchor: AnchorPoint,
    endAnchor: AnchorPoint,
    color: Color,
    strokeWidth: Float,
    arrowHead: ArrowHead,
    isSelected: Boolean
) {
    val path = Path()
    val points = calculateOrthogonalPath(start, end, startAnchor, endAnchor)
    
    path.moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { point ->
        path.lineTo(point.x, point.y)
    }

    drawPath(
        path = path,
        color = if (isSelected) Color(0xFF2196F3) else color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = if (isSelected) PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null
        )
    )

    // Draw arrow at the last segment
    if (points.size >= 2) {
        val lastPoint = points.last()
        val secondLastPoint = points[points.size - 2]
        drawArrowHead(lastPoint, secondLastPoint, color, strokeWidth, arrowHead)
    }
}

private fun calculateOrthogonalPath(
    start: Offset,
    end: Offset,
    startAnchor: AnchorPoint,
    endAnchor: AnchorPoint
): List<Offset> {
    val minDistance = 30f
    val points = mutableListOf<Offset>()
    points.add(start)

    when {
        // Horizontal to Horizontal
        startAnchor in listOf(AnchorPoint.LEFT, AnchorPoint.RIGHT) &&
        endAnchor in listOf(AnchorPoint.LEFT, AnchorPoint.RIGHT) -> {
            val midX = (start.x + end.x) / 2
            points.add(Offset(midX, start.y))
            points.add(Offset(midX, end.y))
        }
        // Vertical to Vertical
        startAnchor in listOf(AnchorPoint.TOP, AnchorPoint.BOTTOM) &&
        endAnchor in listOf(AnchorPoint.TOP, AnchorPoint.BOTTOM) -> {
            val midY = (start.y + end.y) / 2
            points.add(Offset(start.x, midY))
            points.add(Offset(end.x, midY))
        }
        // Horizontal start
        startAnchor in listOf(AnchorPoint.LEFT, AnchorPoint.RIGHT) -> {
            val extendX = if (startAnchor == AnchorPoint.RIGHT) {
                maxOf(start.x + minDistance, end.x)
            } else {
                minOf(start.x - minDistance, end.x)
            }
            points.add(Offset(extendX, start.y))
            points.add(Offset(extendX, end.y))
        }
        // Vertical start
        else -> {
            val extendY = if (startAnchor == AnchorPoint.BOTTOM) {
                maxOf(start.y + minDistance, end.y)
            } else {
                minOf(start.y - minDistance, end.y)
            }
            points.add(Offset(start.x, extendY))
            points.add(Offset(end.x, extendY))
        }
    }

    points.add(end)
    return points
}

private fun DrawScope.drawBezierConnector(
    start: Offset,
    end: Offset,
    startAnchor: AnchorPoint,
    endAnchor: AnchorPoint,
    color: Color,
    strokeWidth: Float,
    arrowHead: ArrowHead,
    isSelected: Boolean
) {
    val controlPointDistance = (end - start).getDistance() * 0.4f

    val control1 = when (startAnchor) {
        AnchorPoint.TOP -> Offset(start.x, start.y - controlPointDistance)
        AnchorPoint.BOTTOM -> Offset(start.x, start.y + controlPointDistance)
        AnchorPoint.LEFT -> Offset(start.x - controlPointDistance, start.y)
        AnchorPoint.RIGHT -> Offset(start.x + controlPointDistance, start.y)
        AnchorPoint.CENTER -> start
    }

    val control2 = when (endAnchor) {
        AnchorPoint.TOP -> Offset(end.x, end.y - controlPointDistance)
        AnchorPoint.BOTTOM -> Offset(end.x, end.y + controlPointDistance)
        AnchorPoint.LEFT -> Offset(end.x - controlPointDistance, end.y)
        AnchorPoint.RIGHT -> Offset(end.x + controlPointDistance, end.y)
        AnchorPoint.CENTER -> end
    }

    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    }

    drawPath(
        path = path,
        color = if (isSelected) Color(0xFF2196F3) else color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = if (isSelected) PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null
        )
    )

    // Calculate tangent at end for arrow
    val t = 0.95f
    val tangentX = 3 * (1 - t) * (1 - t) * (control1.x - start.x) +
            6 * (1 - t) * t * (control2.x - control1.x) +
            3 * t * t * (end.x - control2.x)
    val tangentY = 3 * (1 - t) * (1 - t) * (control1.y - start.y) +
            6 * (1 - t) * t * (control2.y - control1.y) +
            3 * t * t * (end.y - control2.y)
    
    val fakeStart = Offset(end.x - tangentX, end.y - tangentY)
    drawArrowHead(end, fakeStart, color, strokeWidth, arrowHead)
}

private fun DrawScope.drawArrowHead(
    tip: Offset,
    from: Offset,
    color: Color,
    strokeWidth: Float,
    arrowHead: ArrowHead
) {
    if (arrowHead == ArrowHead.NONE) return

    val arrowSize = strokeWidth * 5
    val angle = atan2((tip.y - from.y).toDouble(), (tip.x - from.x).toDouble())
    val arrowAngle = PI / 6 // 30 degrees

    val point1 = Offset(
        (tip.x - arrowSize * cos(angle - arrowAngle)).toFloat(),
        (tip.y - arrowSize * sin(angle - arrowAngle)).toFloat()
    )
    val point2 = Offset(
        (tip.x - arrowSize * cos(angle + arrowAngle)).toFloat(),
        (tip.y - arrowSize * sin(angle + arrowAngle)).toFloat()
    )

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(point1.x, point1.y)
        lineTo(point2.x, point2.y)
        close()
    }

    drawPath(path, color, style = Fill)

    // Draw double arrow at start if needed
    if (arrowHead == ArrowHead.DOUBLE) {
        val reverseAngle = angle + PI
        val startTip = from
        val rPoint1 = Offset(
            (startTip.x - arrowSize * cos(reverseAngle - arrowAngle)).toFloat(),
            (startTip.y - arrowSize * sin(reverseAngle - arrowAngle)).toFloat()
        )
        val rPoint2 = Offset(
            (startTip.x - arrowSize * cos(reverseAngle + arrowAngle)).toFloat(),
            (startTip.y - arrowSize * sin(reverseAngle + arrowAngle)).toFloat()
        )

        val reversePath = Path().apply {
            moveTo(startTip.x, startTip.y)
            lineTo(rPoint1.x, rPoint1.y)
            lineTo(rPoint2.x, rPoint2.y)
            close()
        }
        drawPath(reversePath, color, style = Fill)
    }
}
