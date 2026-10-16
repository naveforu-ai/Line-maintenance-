package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Task Entity representing Grid patrolling assignments ---
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val locationFrom: String,
    val locationTo: String,
    val voltage: String,
    val lineName: String,
    val type: String,
    val reportTime: String,
    val numMembersOtherWorks: Int,
    val otherWorksDetails: String,
    val assignedBy: String,
    val status: String, // "AWAITING_PTW", "IN_PROGRESS", "COMPLETED"
    val priority: String // "CRITICAL", "HIGH", "NORMAL"
)

// --- Defect Entity representing captured grid/insulator failures ---
@Entity(tableName = "defects")
data class DefectEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: String, // custom e.g., "DEF-4389"
    val category: String,
    val notes: String,
    val imageUrl: String,
    val timestamp: String,
    val latitude: Double = 16.5301,
    val longitude: Double = 80.6111,
    val status: String // "RECORDED_CACHED", "SYNCED"
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priority DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int
}

@Dao
interface DefectDao {
    @Query("SELECT * FROM defects ORDER BY localId DESC")
    fun getAllDefects(): Flow<List<DefectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefect(defect: DefectEntity)

    @Query("DELETE FROM defects WHERE localId = :localId")
    suspend fun deleteDefect(localId: Int)
}

@Database(entities = [TaskEntity::class, DefectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val taskDao: TaskDao
    abstract val defectDao: DefectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gridguard_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
