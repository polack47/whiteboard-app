package com.whiteboard.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whiteboard.app.ui.editor.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    var showConnectorStyleDialog by remember { mutableStateOf(false) }

    // Show connector style dialog when connector is selected
    LaunchedEffect(viewModel.canvasState.selectedConnectorId) {
        if (viewModel.canvasState.selectedConnectorId != null) {
            showConnectorStyleDialog = true
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                viewModel = viewModel,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main canvas
            DiagramCanvas(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Left toolbar
            EditorToolbar(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp)
            )

            // Bottom controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoomControls(viewModel = viewModel)
                SelectionActions(viewModel = viewModel)
            }

            // Help text for connector mode
            if (viewModel.canvasState.editMode == com.whiteboard.app.data.model.EditMode.AddConnector) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    val text = if (viewModel.canvasState.pendingConnectorStart == null) {
                        "Tap a shape to start connection"
                    } else {
                        "Tap another shape to complete connection"
                    }
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Dialogs
    if (viewModel.showTextEditor) {
        TextEditorDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showTextEditor = false }
        )
    }

    if (viewModel.showColorPicker) {
        ColorPickerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showColorPicker = false }
        )
    }

    if (showConnectorStyleDialog && viewModel.canvasState.selectedConnectorId != null) {
        ConnectorStyleDialog(
            viewModel = viewModel,
            onDismiss = { showConnectorStyleDialog = false }
        )
    }
}
