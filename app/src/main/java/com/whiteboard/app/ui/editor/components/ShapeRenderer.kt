package com.whiteboard.app.ui.editor.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.whiteboard.app.data.model.DiagramShape
import com.whiteboard.app.data.model.ShapeType

/**
 * Draw a shape on the canvas
 */
fun DrawScope.drawDiagramShape(
    shape: DiagramShape,
    scale: Float,
    offset: Offset,
    isSelected: Boolean = false
) {
    val screenX = shape.x * scale + offset.x
    val screenY = shape.y * scale + offset.y
    val screenWidth = shape.width * scale
    val screenHeight = shape.height * scale
    val screenCornerRadius = shape.cornerRadius * scale
    val screenStrokeWidth = shape.strokeWidth * scale

    val fillColor = Color(shape.fillColor)
    val strokeColor = Color(shape.strokeColor)

    when (shape.type) {
        ShapeType.RECTANGLE -> {
            drawRect(
                color = fillColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenWidth, screenHeight),
                style = Fill
            )
            drawRect(
                color = strokeColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenWidth, screenHeight),
                style = Stroke(width = screenStrokeWidth)
            )
        }

        ShapeType.ROUNDED_RECTANGLE -> {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenWidth, screenHeight),
                cornerRadius = CornerRadius(screenCornerRadius),
                style = Fill
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenWidth, screenHeight),
                cornerRadius = CornerRadius(screenCornerRadius),
                style = Stroke(width = screenStrokeWidth)
            )
        }

        ShapeType.CIRCLE -> {
            val centerX = screenX + screenWidth / 2
            val centerY = screenY + screenHeight / 2
            val radius = minOf(screenWidth, screenHeight) / 2

            drawCircle(
                color = fillColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Fill
            )
            drawCircle(
                color = strokeColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = screenStrokeWidth)
            )
        }

        ShapeType.DIAMOND -> {
            val path = Path().apply {
                moveTo(screenX + screenWidth / 2, screenY)
                lineTo(screenX + screenWidth, screenY + screenHeight / 2)
                lineTo(screenX + screenWidth / 2, screenY + screenHeight)
                lineTo(screenX, screenY + screenHeight / 2)
                close()
            }
            drawPath(path, fillColor, style = Fill)
            drawPath(path, strokeColor, style = Stroke(width = screenStrokeWidth))
        }

        ShapeType.PARALLELOGRAM -> {
            val skew = screenWidth * 0.2f
            val path = Path().apply {
                moveTo(screenX + skew, screenY)
                lineTo(screenX + screenWidth, screenY)
                lineTo(screenX + screenWidth - skew, screenY + screenHeight)
                lineTo(screenX, screenY + screenHeight)
                close()
            }
            drawPath(path, fillColor, style = Fill)
            drawPath(path, strokeColor, style = Stroke(width = screenStrokeWidth))
        }
    }

    // Draw selection outline
    if (isSelected) {
        val selectionPadding = 4f * scale
        drawRoundRect(
            color = Color(0xFF2196F3),
            topLeft = Offset(screenX - selectionPadding, screenY - selectionPadding),
            size = Size(screenWidth + selectionPadding * 2, screenHeight + selectionPadding * 2),
            cornerRadius = CornerRadius(screenCornerRadius + selectionPadding),
            style = Stroke(width = 2f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))
        )
    }
}

/**
 * Draw resize handles for a selected shape
 */
fun DrawScope.drawResizeHandles(
    shape: DiagramShape,
    scale: Float,
    offset: Offset,
    handleSize: Float = 10f
) {
    val screenX = shape.x * scale + offset.x
    val screenY = shape.y * scale + offset.y
    val screenWidth = shape.width * scale
    val screenHeight = shape.height * scale
    val scaledHandleSize = handleSize * scale

    val handlePositions = listOf(
        Offset(screenX, screenY), // TOP_LEFT
        Offset(screenX + screenWidth / 2, screenY), // TOP
        Offset(screenX + screenWidth, screenY), // TOP_RIGHT
        Offset(screenX, screenY + screenHeight / 2), // LEFT
        Offset(screenX + screenWidth, screenY + screenHeight / 2), // RIGHT
        Offset(screenX, screenY + screenHeight), // BOTTOM_LEFT
        Offset(screenX + screenWidth / 2, screenY + screenHeight), // BOTTOM
        Offset(screenX + screenWidth, screenY + screenHeight) // BOTTOM_RIGHT
    )

    handlePositions.forEach { pos ->
        // White fill
        drawCircle(
            color = Color.White,
            radius = scaledHandleSize / 2,
            center = pos,
            style = Fill
        )
        // Blue border
        drawCircle(
            color = Color(0xFF2196F3),
            radius = scaledHandleSize / 2,
            center = pos,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Draw anchor points for connector attachment
 */
fun DrawScope.drawAnchorPoints(
    shape: DiagramShape,
    scale: Float,
    offset: Offset,
    anchorSize: Float = 8f
) {
    val anchors = listOf(
        shape.getAnchorPosition(com.whiteboard.app.data.model.AnchorPoint.TOP),
        shape.getAnchorPosition(com.whiteboard.app.data.model.AnchorPoint.BOTTOM),
        shape.getAnchorPosition(com.whiteboard.app.data.model.AnchorPoint.LEFT),
        shape.getAnchorPosition(com.whiteboard.app.data.model.AnchorPoint.RIGHT)
    )

    anchors.forEach { anchor ->
        val screenPos = Offset(
            anchor.x * scale + offset.x,
            anchor.y * scale + offset.y
        )
        
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = anchorSize * scale / 2,
            center = screenPos,
            style = Fill
        )
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = anchorSize * scale / 2,
            center = screenPos,
            style = Stroke(width = 2f)
        )
    }
}
