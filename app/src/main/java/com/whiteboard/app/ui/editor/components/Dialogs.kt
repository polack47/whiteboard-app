@Composable
fun ColorPickerDialog(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val selectedShape = viewModel.canvasState.selectedShapeId?.let { id ->
        viewModel.diagram.shapes.find { it.id == id }
    } ?: return
    
    var selectedFillColor by remember { mutableStateOf(Color(selectedShape.fillColor)) }
    var selectedStrokeColor by remember { mutableStateOf(Color(selectedShape.strokeColor)) }
    var selectedTextColor by remember { mutableStateOf(Color(selectedShape.textColor)) }
    var activeTab by remember { mutableStateOf(0) }

    val colors = listOf(
        Color(0xFFE3F2FD), Color(0xFF90CAF9), Color(0xFF2196F3), Color(0xFF1565C0),
        Color(0xFFE8F5E9), Color(0xFFA5D6A7), Color(0xFF4CAF50), Color(0xFF2E7D32),
        Color(0xFFFFF3E0), Color(0xFFFFCC80), Color(0xFFFF9800), Color(0xFFEF6C00),
        Color(0xFFFFEBEE), Color(0xFFEF9A9A), Color(0xFFF44336), Color(0xFFC62828),
        Color(0xFFF3E5F5), Color(0xFFCE93D8), Color(0xFF9C27B0), Color(0xFF6A1B9A),
        Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFF00BCD4), Color(0xFF00838F),
        Color(0xFFFFFDE7), Color(0xFFFFF59D), Color(0xFFFFEB3B), Color(0xFFF9A825),
        Color(0xFFEFEBE9), Color(0xFFBCAAA4), Color(0xFF795548), Color(0xFF4E342E),
        Color.White, Color(0xFFEEEEEE), Color(0xFF9E9E9E), Color(0xFF424242),
    )
    
    val textColors = listOf(
        Color.Black,
        Color.White
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Colors", style = MaterialTheme.typography.titleLarge)

                // Tabs
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Fill") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Stroke") }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Text") }
                    )
                }

                when (activeTab) {
                    0, 1 -> {
                        // Fill or Stroke color grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(8),
                            modifier = Modifier.height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(colors) { color ->
                                val isSelected = if (activeTab == 0) color == selectedFillColor else color == selectedStrokeColor
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (activeTab == 0) selectedFillColor = color
                                            else selectedStrokeColor = color
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Text color - only black and white
                        Text("Text Color", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            textColors.forEach { color ->
                                val isSelected = color == selectedTextColor
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 4.dp else 2.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedTextColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (color == Color.White) Color.Black else Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedFillColor)
                        .border(3.dp, selectedStrokeColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Preview",
                        color = selectedTextColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        viewModel.updateShapeColor(selectedShape.id, selectedFillColor, selectedStrokeColor, selectedTextColor)
                        onDismiss()
                    }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
