package com.whiteboard.app.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.whiteboard.app.data.model.EditMode
import com.whiteboard.app.data.model.ShapeType
import com.whiteboard.app.ui.editor.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNameDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = viewModel.diagram.name,
                modifier = Modifier.clickable { showNameDialog = true }
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                viewModel.saveDiagram()
                onBackClick()
            }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(
                onClick = { viewModel.undo() },
                enabled = viewModel.undoRedoManager.canUndo
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
            }
            IconButton(
                onClick = { viewModel.redo() },
                enabled = viewModel.undoRedoManager.canRedo
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
            }
            IconButton(onClick = { viewModel.saveDiagram() }) {
                Icon(Icons.Default.Save, "Save")
            }
        },
        modifier = modifier
    )

    if (showNameDialog) {
        var newName by remember { mutableStateOf(viewModel.diagram.name) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Rename Diagram") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDiagramName(newName)
                    showNameDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditorToolbar(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val canvasState = viewModel.canvasState

    Column(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Selection tool
        ToolbarButton(
            icon = Icons.Default.NearMe,
            label = "Select",
            isSelected = canvasState.editMode == EditMode.Select,
            onClick = { canvasState.editMode = EditMode.Select }
        )

        // Pan tool
        ToolbarButton(
            icon = Icons.Default.PanTool,
            label = "Pan",
            isSelected = canvasState.editMode == EditMode.Pan,
            onClick = { canvasState.editMode = EditMode.Pan }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Shape tools
        ToolbarButton(
            icon = Icons.Default.CropSquare,
            label = "Rectangle",
            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.RECTANGLE),
            onClick = { canvasState.editMode = EditMode.AddShape(ShapeType.RECTANGLE) }
        )

        ToolbarButton(
            icon = Icons.Default.RoundedCorner,
            label = "Rounded",
            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.ROUNDED_RECTANGLE),
            onClick = { canvasState.editMode = EditMode.AddShape(ShapeType.ROUNDED_RECTANGLE) }
        )

        ToolbarButton(
            icon = Icons.Default.Circle,
            label = "Circle",
            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.CIRCLE),
            onClick = { canvasState.editMode = EditMode.AddShape(ShapeType.CIRCLE) }
        )

        ToolbarButton(
            icon = Icons.Default.Diamond,
            label = "Diamond",
            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.DIAMOND),
            onClick = { canvasState.editMode = EditMode.AddShape(ShapeType.DIAMOND) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Connector tool
        ToolbarButton(
            icon = Icons.Default.Timeline,
            label = "Connect",
            isSelected = canvasState.editMode == EditMode.AddConnector,
            onClick = {
                canvasState.cancelConnector()
                canvasState.editMode = EditMode.AddConnector
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Grid toggle
        ToolbarButton(
            icon = Icons.Default.GridOn,
            label = "Grid",
            isSelected = canvasState.showGrid,
            onClick = { canvasState.showGrid = !canvasState.showGrid }
        )

        // Snap toggle
        ToolbarButton(
            icon = Icons.Default.FilterCenterFocus,
            label = "Snap",
            isSelected = canvasState.snapToGrid,
            onClick = { canvasState.snapToGrid = !canvasState.snapToGrid }
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ZoomControls(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val canvasState = viewModel.canvasState

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { canvasState.zoom(0.8f, androidx.compose.ui.geometry.Offset.Zero) }) {
            Icon(Icons.Default.ZoomOut, "Zoom Out")
        }

        Text(
            text = "${(canvasState.scale * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        IconButton(onClick = { canvasState.zoom(1.25f, androidx.compose.ui.geometry.Offset.Zero) }) {
            Icon(Icons.Default.ZoomIn, "Zoom In")
        }

        IconButton(onClick = {
            canvasState.setScale(1f)
            canvasState.setOffset(androidx.compose.ui.geometry.Offset.Zero)
        }) {
            Icon(Icons.Default.FitScreen, "Reset View")
        }
    }
}

@Composable
fun SelectionActions(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val canvasState = viewModel.canvasState
    val hasShapeSelected = canvasState.selectedShapeId != null
    val hasConnectorSelected = canvasState.selectedConnectorId != null

    if (!hasShapeSelected && !hasConnectorSelected) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (hasShapeSelected) {
            IconButton(onClick = { viewModel.showTextEditor = true }) {
                Icon(Icons.Default.TextFields, "Edit Text")
            }
            IconButton(onClick = { viewModel.showColorPicker = true }) {
                Icon(Icons.Default.Palette, "Color")
            }
        }

        IconButton(
            onClick = {
                if (hasShapeSelected) viewModel.deleteSelectedShape()
                else viewModel.deleteSelectedConnector()
            }
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
