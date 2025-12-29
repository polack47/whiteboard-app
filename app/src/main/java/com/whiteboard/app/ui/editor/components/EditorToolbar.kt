package com.whiteboard.app.ui.editor.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
                Icon(Icons.Default.Undo, "Undo")
            }
            IconButton(
                onClick = { viewModel.redo() },
                enabled = viewModel.undoRedoManager.canRedo
            ) {
                Icon(Icons.Default.Redo, "Redo")
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
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Toggle button
        SmallFloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(44.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (isExpanded) "Close" else "Tools",
                modifier = Modifier.size(20.dp)
            )
        }

        // Expandable toolbar - kompakt
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(4.dp).width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Row 1: Select & Pan
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniToolButton(
                            icon = Icons.Default.NearMe,
                            isSelected = canvasState.editMode == EditMode.Select,
                            onClick = { 
                                canvasState.editMode = EditMode.Select
                                isExpanded = false
                            }
                        )
                        MiniToolButton(
                            icon = Icons.Default.PanTool,
                            isSelected = canvasState.editMode == EditMode.Pan,
                            onClick = { 
                                canvasState.editMode = EditMode.Pan
                                isExpanded = false
                            }
                        )
                    }
                    
                    Divider(Modifier.padding(vertical = 2.dp))

                    // Row 2: Shapes
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniToolButton(
                            icon = Icons.Default.CropSquare,
                            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.RECTANGLE),
                            onClick = { 
                                canvasState.editMode = EditMode.AddShape(ShapeType.RECTANGLE)
                                isExpanded = false
                            }
                        )
                        MiniToolButton(
                            icon = Icons.Default.RoundedCorner,
                            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.ROUNDED_RECTANGLE),
                            onClick = { 
                                canvasState.editMode = EditMode.AddShape(ShapeType.ROUNDED_RECTANGLE)
                                isExpanded = false
                            }
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniToolButton(
                            icon = Icons.Default.Circle,
                            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.CIRCLE),
                            onClick = { 
                                canvasState.editMode = EditMode.AddShape(ShapeType.CIRCLE)
                                isExpanded = false
                            }
                        )
                        MiniToolButton(
                            icon = Icons.Default.Diamond,
                            isSelected = canvasState.editMode == EditMode.AddShape(ShapeType.DIAMOND),
                            onClick = { 
                                canvasState.editMode = EditMode.AddShape(ShapeType.DIAMOND)
                                isExpanded = false
                            }
                        )
                    }

                    Divider(Modifier.padding(vertical = 2.dp))

                    // Row 3: Connector
                    MiniToolButton(
                        icon = Icons.Default.Timeline,
                        isSelected = canvasState.editMode == EditMode.AddConnector,
                        onClick = {
                            canvasState.cancelConnector()
                            canvasState.editMode = EditMode.AddConnector
                            isExpanded = false
                        }
                    )

                    Divider(Modifier.padding(vertical = 2.dp))

                    // Row 4: Grid & Snap
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniToolButton(
                            icon = Icons.Default.GridOn,
                            isSelected = canvasState.showGrid,
                            onClick = { canvasState.showGrid = !canvasState.showGrid }
                        )
                        MiniToolButton(
                            icon = Icons.Default.FilterCenterFocus,
                            isSelected = canvasState.snapToGrid,
                            onClick = { canvasState.snapToGrid = !canvasState.snapToGrid }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { canvasState.zoom(0.8f, androidx.compose.ui.geometry.Offset.Zero) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ZoomOut, "Zoom Out", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "${(canvasState.scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(36.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            IconButton(
                onClick = { canvasState.zoom(1.25f, androidx.compose.ui.geometry.Offset.Zero) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ZoomIn, "Zoom In", modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = {
                    canvasState.updateScale(1f)
                    canvasState.updateOffset(androidx.compose.ui.geometry.Offset.Zero)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.FitScreen, "Reset", modifier = Modifier.size(16.dp))
            }
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasShapeSelected) {
                IconButton(
                    onClick = { viewModel.showTextEditor = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.TextFields, "Edit Text", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { viewModel.showColorPicker = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Palette, "Color", modifier = Modifier.size(16.dp))
                }
            }

            IconButton(
                onClick = {
                    if (hasShapeSelected) viewModel.deleteSelectedShape()
                    else viewModel.deleteSelectedConnector()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}
