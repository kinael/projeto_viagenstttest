package com.example.projeto_viagens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration da versão 1 para 2: adiciona tabela de viagens
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trips (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                destination TEXT NOT NULL,
                type TEXT NOT NULL,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                budget REAL NOT NULL,
                userId INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

// Migration da versão 2 para 3: adiciona coluna totalSpent (total de gastos)
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE trips ADD COLUMN totalSpent REAL NOT NULL DEFAULT 0.0"
        )
    }
}

// Migration da versão 3 para 4: adiciona tabela de fotos vinculadas à viagem
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tripId INTEGER NOT NULL,
                uri TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_trip_photos_tripId ON trip_photos(tripId)"
        )
    }
}

// Migration da versão 4 para 5: adiciona tabela de roteiros gerados pela IA
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_roteiros (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tripId INTEGER NOT NULL,
                interesses TEXT NOT NULL,
                conteudo TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_trip_roteiros_tripId ON trip_roteiros(tripId)"
        )
    }
}

@Database(
    entities = [
        UserEntity::class,
        TripEntity::class,
        TripPhotoEntity::class,
        RoteiroEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao
    abstract fun tripPhotoDao(): TripPhotoDao
    abstract fun roteiroDao(): RoteiroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "viagens_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}