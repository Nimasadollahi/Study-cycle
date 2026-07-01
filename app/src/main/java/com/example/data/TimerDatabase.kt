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
import com.example.model.TimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "timer_config")
data class TimerConfigEntity(
    @PrimaryKey val id: Int = 1,
    val studyDurationMinutes: Int = 90,
    val breakDurationMinutes: Int = 15,
    val cycle1Count: Int = 4,
    val bigBreakDurationMinutes: Int = 60,
    val cycle2Count: Int = 4,
    val startHour: Int = 7,
    val startMinute: Int = 0,
    val autoStartBreak: Boolean = false,
    val autoStartStudy: Boolean = false
) {
    fun toModel(): TimerConfig = TimerConfig(
        id = id,
        studyDurationMinutes = studyDurationMinutes,
        breakDurationMinutes = breakDurationMinutes,
        cycle1Count = cycle1Count,
        bigBreakDurationMinutes = bigBreakDurationMinutes,
        cycle2Count = cycle2Count,
        startHour = startHour,
        startMinute = startMinute,
        autoStartBreak = autoStartBreak,
        autoStartStudy = autoStartStudy
    )

    companion object {
        fun fromModel(model: TimerConfig) = TimerConfigEntity(
            id = model.id,
            studyDurationMinutes = model.studyDurationMinutes,
            breakDurationMinutes = model.breakDurationMinutes,
            cycle1Count = model.cycle1Count,
            bigBreakDurationMinutes = model.bigBreakDurationMinutes,
            cycle2Count = model.cycle2Count,
            startHour = model.startHour,
            startMinute = model.startMinute,
            autoStartBreak = model.autoStartBreak,
            autoStartStudy = model.autoStartStudy
        )
    }
}

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phase: String,
    val label: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val completed: Boolean
)

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_config WHERE id = :id LIMIT 1")
    fun getConfigFlow(id: Int): Flow<TimerConfigEntity?>

    @Query("SELECT * FROM timer_config WHERE id = :id LIMIT 1")
    suspend fun getConfigDirect(id: Int): TimerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(entity: TimerConfigEntity)

    @Query("SELECT * FROM session_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SessionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: SessionHistoryEntity)

    @Query("DELETE FROM session_history")
    suspend fun clearHistory()
}

@Database(entities = [TimerConfigEntity::class, SessionHistoryEntity::class], version = 2, exportSchema = false)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao

    companion object {
        @Volatile
        private var INSTANCE: TimerDatabase? = null

        fun getDatabase(context: Context): TimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimerDatabase::class.java,
                    "timer_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TimerRepository(private val timerDao: TimerDao) {
    val configFlow: Flow<TimerConfig> = timerDao.getConfigFlow(1).map { entity ->
        entity?.toModel() ?: TimerConfig()
    }

    suspend fun getDirectConfig(): TimerConfig {
        return timerDao.getConfigDirect(1)?.toModel() ?: TimerConfig()
    }

    suspend fun saveConfig(config: TimerConfig) {
        timerDao.saveConfig(TimerConfigEntity.fromModel(config))
    }

    val historyFlow: Flow<List<SessionHistoryEntity>> = timerDao.getAllHistory()

    suspend fun logSession(phase: String, label: String, durationMinutes: Int, completed: Boolean) {
        timerDao.insertHistory(
            SessionHistoryEntity(
                phase = phase,
                label = label,
                timestamp = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                completed = completed
            )
        )
    }

    suspend fun clearHistory() {
        timerDao.clearHistory()
    }
}
