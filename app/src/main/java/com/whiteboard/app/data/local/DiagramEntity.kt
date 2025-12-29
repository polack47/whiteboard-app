package com.whiteboard.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.whiteboard.app.data.model.Connector
import com.whiteboard.app.data.model.Diagram
import com.whiteboard.app.data.model.DiagramShape

@Entity(tableName = "diagrams")
@TypeConverters(DiagramConverters::class)
data class DiagramEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val shapes: List<DiagramShape>,
    val connectors: List<Connector>,
    val createdAt: Long,
    val updatedAt: Long,
    val gridSize: Float,
    val snapToGrid: Boolean
) {
    fun toDiagram(): Diagram = Diagram(
        id = id,
        name = name,
        shapes = shapes,
        connectors = connectors,
        createdAt = createdAt,
        updatedAt = updatedAt,
        gridSize = gridSize,
        snapToGrid = snapToGrid
    )

    companion object {
        fun fromDiagram(diagram: Diagram): DiagramEntity = DiagramEntity(
            id = diagram.id,
            name = diagram.name,
            shapes = diagram.shapes,
            connectors = diagram.connectors,
            createdAt = diagram.createdAt,
            updatedAt = diagram.updatedAt,
            gridSize = diagram.gridSize,
            snapToGrid = diagram.snapToGrid
        )
    }
}

class DiagramConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromShapeList(shapes: List<DiagramShape>): String {
        return gson.toJson(shapes)
    }

    @TypeConverter
    fun toShapeList(json: String): List<DiagramShape> {
        val type = object : TypeToken<List<DiagramShape>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromConnectorList(connectors: List<Connector>): String {
        return gson.toJson(connectors)
    }

    @TypeConverter
    fun toConnectorList(json: String): List<Connector> {
        val type = object : TypeToken<List<Connector>>() {}.type
        return gson.fromJson(json, type)
    }
}
