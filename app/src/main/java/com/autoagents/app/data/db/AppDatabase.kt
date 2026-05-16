package com.autoagents.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemoEntity::class,
        ArticleEntity::class,
        InsightEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun articleDao(): ArticleDao
    abstract fun insightDao(): InsightDao
    abstract fun chatDao(): ChatDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "autoagents.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
