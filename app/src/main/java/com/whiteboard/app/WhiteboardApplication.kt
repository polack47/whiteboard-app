package com.whiteboard.app

import android.app.Application
import com.whiteboard.app.data.local.WhiteboardDatabase
import com.whiteboard.app.data.repository.DiagramRepository

class WhiteboardApplication : Application() {
    
    val database by lazy { WhiteboardDatabase.getInstance(this) }
    val repository by lazy { DiagramRepository(database.diagramDao()) }
}
