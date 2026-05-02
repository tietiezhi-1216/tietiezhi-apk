package com.tietiezhi.apk.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tietiezhi.apk.data.local.db.dao.ChatDao
import com.tietiezhi.apk.data.local.db.dao.MessageDao
import com.tietiezhi.apk.data.local.db.entity.ChatEntity
import com.tietiezhi.apk.data.local.db.entity.MessageEntity

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
