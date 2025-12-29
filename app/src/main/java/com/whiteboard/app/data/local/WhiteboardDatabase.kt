package com.whiteboard.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DiagramEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DiagramConverters::class)
abstract class WhiteboardDatabase : RoomDatabase() {
    abstract fun diagramDao(): DiagramDao

    companion object {
        @Volatile
        private var INSTANCE: WhiteboardDatabase? = null

        fun getInstance(context: Context): WhiteboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhiteboardDatabase::class.java,
                    "whiteboard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
