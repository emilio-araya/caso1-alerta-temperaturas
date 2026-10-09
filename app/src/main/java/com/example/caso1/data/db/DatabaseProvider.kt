package com.example.caso1.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "caso1_db"
            ).addMigrations(MIGRACION_2_3)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }

    /** v3: columnas de confirmación de alertas, sin perder los datos existentes. */
    private val MIGRACION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE alertas ADD COLUMN confirmadaPor TEXT")
            db.execSQL("ALTER TABLE alertas ADD COLUMN confirmadaEn INTEGER")
        }
    }
}
