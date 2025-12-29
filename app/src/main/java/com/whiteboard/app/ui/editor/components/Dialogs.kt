package com.whiteboard.app.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.whiteboard.app.ui.editor.EditorViewModel

@Composable
fun TextEditorDialog(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val shapeId = viewModel.canvasState.selectedShapeId
    if (shapeId == null) {
        onDismiss()
        return
    }

    var text by remember { mutableStateOf(viewModel.editingText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Shape Text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.updateShapeText(shapeId, text)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ColorPickerDialog(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val shapeId = viewModel.canvasState.selectedShapeId
    if (shapeId == null) {
        onDismiss()
        return
    }

    val shape = viewModel.getShapesMap()[shapeId]
    if (shape == null) {
        onDismiss()
        return
    }

    var selectedFillColor by remember { mutableStateOf(Color(shape.fillColor)) }
    var selectedStrokeColor by remember { mutableStateOf(Color(shape.strokeColor)) }
    var editingFill by remember { mutableStateOf(true) }

    val colors = listOf(
        // Blues
        Color(0xFF4FC3F7), Color(0xFF29B6F6), Color(0xFF03A9F4),
        Color(0xFF039BE5), Color(0xFF0288D1), Color(0xFF0277BD),
        // Greens
        Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFF4CAF50),
        Color(0xFF43A047), Color(0xFF388E3C), Color(0xFF2E7D32),
        // Yellows/Oranges
        Color(0xFFFFD54F), Color(0xFFFFCA28), Color(0xFFFFC107),
        Color(0xFFFFB300), Color(0xFFFFA000), Color(0xFFFF8F00),
        // Reds/Pinks
        Color(0xFFE57373), Color(0xFFEF5350), Color(0xFFF44336),
        Color(0xFFE53935), Color(0xFFD32F2F), Color(0xFFC62828),
        // Purples
        Color(0xFFBA68C8), Color(0xFFAB47BC), Color(0xFF9C27B0),
        Color(0xFF8E24AA), Color(0xFF7B1FA2), Color(0xFF6A1B9A),
        // Grays
        Color(0xFFF5F5F5), Color(0xFFEEEEEE), Color(0xFFE0E0E0),
        Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575),
        Color(0xFF616161), Color(0xFF424242), Color(0xFF212121),
        // Other
        Color.White, Color.Black
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Color") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ColorTypeButton(
                        label = "Fill",
                        color = selectedFillColor,
                        isSelected = editingFill,
                        onClick = { editingFill = true }
                    )
                    ColorTypeButton(
                        label = "Stroke",
                        color = selectedStrokeColor,
                        isSelected = !editingFill,
                        onClick = { editingFill = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        val isSelected = if (editingFill) color == selectedFillColor else color == selectedStrokeColor
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (editingFill) {
                                        selectedFillColor = color
                                    } else {
                                        selectedStrokeColor = color
                                    }
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.updateShapeColor(shapeId, selectedFillColor, selectedStrokeColor)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorTypeButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ConnectorStyleDialog(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val connectorId = viewModel.canvasState.selectedConnectorId
    if (connectorId == null) {
        onDismiss()
        return
    }

    val connector = viewModel.diagram.connectors.find { it.id == connectorId }
    if (connector == null) {
        onDismiss()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connector Style") },
        text = {
            Column {
                Text("Line Style", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    com.whiteboard.app.data.model.ConnectorStyle.entries.forEach { style ->
                        FilterChip(
                            selected = connector.style == style,
                            onClick = { viewModel.updateConnectorStyle(connectorId, style) },
                            label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Arrow Head", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    com.whiteboard.app.data.model.ArrowHead.entries.forEach { arrow ->
                        FilterChip(
                            selected = connector.arrowHead == arrow,
                            onClick = { viewModel.updateConnectorArrowHead(connectorId, arrow) },
                            label = { 
                                Text(
                                    when (arrow) {
                                        com.whiteboard.app.data.model.ArrowHead.NONE -> "None"
                                        com.whiteboard.app.data.model.ArrowHead.SINGLE -> "→"
                                        com.whiteboard.app.data.model.ArrowHead.DOUBLE -> "↔"
                                    }
                                ) 
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
