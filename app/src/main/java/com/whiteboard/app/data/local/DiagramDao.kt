package com.whiteboard.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagramDao {
    @Query("SELECT * FROM diagrams ORDER BY updatedAt DESC")
    fun getAllDiagrams(): Flow<List<DiagramEntity>>

    @Query("SELECT * FROM diagrams WHERE id = :id")
    suspend fun getDiagramById(id: String): DiagramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagram(diagram: DiagramEntity)

    @Update
    suspend fun updateDiagram(diagram: DiagramEntity)

    @Delete
    suspend fun deleteDiagram(diagram: DiagramEntity)

    @Query("DELETE FROM diagrams WHERE id = :id")
    suspend fun deleteDiagramById(id: String)

    @Query("SELECT COUNT(*) FROM diagrams")
    suspend fun getDiagramCount(): Int
}
