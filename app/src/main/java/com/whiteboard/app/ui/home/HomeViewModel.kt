package com.whiteboard.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiteboard.app.data.model.Diagram
import com.whiteboard.app.data.repository.DiagramRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: DiagramRepository
) : ViewModel() {

    val diagrams: StateFlow<List<Diagram>> = repository.allDiagrams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewDiagram(): String {
        val newDiagram = Diagram(name = "New Diagram")
        viewModelScope.launch {
            repository.saveDiagram(newDiagram)
        }
        return newDiagram.id
    }

    fun deleteDiagram(diagram: Diagram) {
        viewModelScope.launch {
            repository.deleteDiagram(diagram)
        }
    }
}
