package com.whiteboard.app.data.repository

import com.whiteboard.app.data.local.DiagramDao
import com.whiteboard.app.data.local.DiagramEntity
import com.whiteboard.app.data.model.Diagram
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DiagramRepository(private val diagramDao: DiagramDao) {

    val allDiagrams: Flow<List<Diagram>> = diagramDao.getAllDiagrams()
        .map { entities -> entities.map { it.toDiagram() } }

    suspend fun getDiagramById(id: String): Diagram? {
        return diagramDao.getDiagramById(id)?.toDiagram()
    }

    suspend fun saveDiagram(diagram: Diagram) {
        val entity = DiagramEntity.fromDiagram(
            diagram.copy(updatedAt = System.currentTimeMillis())
        )
        diagramDao.insertDiagram(entity)
    }

    suspend fun deleteDiagram(diagram: Diagram) {
        diagramDao.deleteDiagramById(diagram.id)
    }

    suspend fun getDiagramCount(): Int {
        return diagramDao.getDiagramCount()
    }
}
