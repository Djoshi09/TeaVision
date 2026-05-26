package com.example.teainfoapp.data

import android.content.Context
import android.util.Log
import androidx.room.*

// ==================== ENTITIES ====================
@Entity(tableName = "tea_types")
data class TeaEntity(
    @PrimaryKey val teaType: String,
    val calories: String,
    val protein: String,
    val carbohydrates: String,
    val fat: String,
    val calcium: String,
    val magnesium: String,
    val doctorRemark: String
)

@Entity(tableName = "tea_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teaType: String,
    val timestamp: Long,
    val userConfirmed: Boolean = false
)

// ==================== DAOs ====================
@Dao
interface TeaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTea(tea: TeaEntity)

    @Query("SELECT * FROM tea_types WHERE teaType = :teaType")
    suspend fun getTeaByType(teaType: String): TeaEntity?

    @Query("SELECT * FROM tea_types")
    suspend fun getAllTeas(): List<TeaEntity>

    @Query("DELETE FROM tea_types WHERE teaType NOT IN (:allowedTeaTypes)")
    suspend fun deleteTeasNotIn(allowedTeaTypes: List<String>)
}

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogEntity)

    @Delete
    suspend fun deleteLog(log: LogEntity)

    @Query("SELECT * FROM tea_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<LogEntity>
}

// ==================== DATABASE ====================
@Database(entities = [TeaEntity::class, LogEntity::class], version = 2, exportSchema = false)
abstract class TeaDatabase : RoomDatabase() {
    abstract fun teaDao(): TeaDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: TeaDatabase? = null

        fun getDatabase(context: Context): TeaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeaDatabase::class.java,
                    "tea_database"
                )
                .fallbackToDestructiveMigration() // Allow data refresh for new tea types
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==================== REPOSITORY ====================
class TeaRepository(
    private val teaDao: TeaDao,
    private val logDao: LogDao
) {
    suspend fun getTeaProfile(teaType: String): TeaEntity? = teaDao.getTeaByType(teaType)
    suspend fun getAllTeas(): List<TeaEntity> = teaDao.getAllTeas()
    suspend fun insertTea(tea: TeaEntity) = teaDao.insertTea(tea)
    suspend fun logTeaSelection(teaType: String, userConfirmed: Boolean = false) {
        logDao.insertLog(LogEntity(teaType = teaType, timestamp = System.currentTimeMillis(), userConfirmed = userConfirmed))
    }
    suspend fun getAllLogs(): List<LogEntity> = logDao.getAllLogs()
    suspend fun deleteLog(log: LogEntity) = logDao.deleteLog(log)

    suspend fun seedDefaultTeas() {
        val supportedTeas = listOf(
            TeaEntity("Green Tea", "2 kcal", "0.2 g", "0.6 g", "0 g", "2 mg", "2 mg",
                "Rich in antioxidants (EGCG). Supports heart, liver, and metabolic health. Contains moderate caffeine. Suitable for daily consumption."),
            TeaEntity("Black Tea", "2 kcal", "0.2 g", "0.7 g", "0 g", "2 mg", "2 mg",
                "High in caffeine. May improve alertness and focus. Contains theaflavins for heart health. Moderate intake advised for evening."),
            TeaEntity("Oolong Tea", "2 kcal", "0.15 g", "0.4 g", "0 g", "3 mg", "2 mg",
                "Partially fermented. Balanced caffeine and antioxidants. Good for weight management, bone health, and mental alertness."),
            TeaEntity("Chamomile Tea", "0 kcal", "0 g", "0 g", "0 g", "5 mg", "2 mg",
                "Caffeine-free. Promotes relaxation and better sleep. Anti-inflammatory properties. Excellent for anxiety and digestion."),
            TeaEntity("Peppermint Tea", "0 kcal", "0 g", "0.1 g", "0 g", "8 mg", "3 mg",
                "Caffeine-free. Soothes digestive issues and IBS. Helps with headaches. Refreshing and cooling. Safe for evening consumption."),
            TeaEntity("Ginger Tea", "2 kcal", "0.1 g", "0.5 g", "0 g", "1 mg", "2 mg",
                "Caffeine-free. Powerful anti-inflammatory. Eases nausea and aids digestion. Boosts immunity. Good for cold and flu symptoms."),
            TeaEntity("Hibiscus Tea", "1 kcal", "0 g", "0.3 g", "0 g", "8 mg", "3 mg",
                "Caffeine-free. May lower blood pressure. Rich in vitamin C. Supports liver health. Tart flavor, good iced or hot."),
            TeaEntity("Rooibos Tea", "0 kcal", "0 g", "0.1 g", "0 g", "12 mg", "4 mg",
                "Caffeine-free. High in antioxidants and minerals. Good for bone health. Naturally sweet, no sugar needed. Safe for children."),
            TeaEntity("Lavender Tea", "0 kcal", "0 g", "0 g", "0 g", "4 mg", "2 mg",
                "Caffeine-free. Reduces stress and anxiety. Promotes restful sleep. Anti-inflammatory. Good for headaches and mood."),
            TeaEntity("Matcha Tea", "6 kcal", "0.5 g", "1.2 g", "0.1 g", "4 mg", "3 mg",
                "Powdered green tea. High in caffeine and L-theanine. Provides sustained energy. Rich in antioxidants. Boosts metabolism."),
            TeaEntity("Chai Tea", "25 kcal", "0.8 g", "4.5 g", "0.5 g", "45 mg", "12 mg",
                "Black tea with spices and milk. Contains caffeine. Anti-inflammatory spices. Warming and energizing. Good for immunity."),
            TeaEntity("Turmeric Tea", "2 kcal", "0.1 g", "0.5 g", "0 g", "2 mg", "1 mg",
                "Caffeine-free. Powerful anti-inflammatory (curcumin). Supports joint health. Boosts immunity. Add black pepper for absorption."),
            TeaEntity("Rosehip Tea", "1 kcal", "0.1 g", "0.3 g", "0 g", "6 mg", "2 mg",
                "Caffeine-free. Very high in vitamin C. Boosts immunity. Anti-inflammatory. Good for skin health and joint pain."),
            TeaEntity("Blueberry Tea", "2 kcal", "0 g", "0.5 g", "0 g", "3 mg", "1 mg",
                "Caffeine-free. Rich in antioxidants. Supports brain health. May improve memory. Natural sweet flavor."),
            TeaEntity("Raspberry Tea", "1 kcal", "0.1 g", "0.2 g", "0 g", "8 mg", "3 mg",
                "Caffeine-free. Rich in vitamins and minerals. Traditionally used for digestive comfort and wellness support."),
            TeaEntity("Kukicha Tea", "1 kcal", "0.1 g", "0.2 g", "0 g", "4 mg", "2 mg",
                "Twig-based Japanese green tea with lower caffeine. Mild, nutty flavor. Good daily tea option."),
            TeaEntity("Genmaicha Tea", "2 kcal", "0.2 g", "0.4 g", "0 g", "3 mg", "2 mg",
                "Green tea blended with roasted rice. Light caffeine. Toasty flavor and gentle digestion profile."),
            TeaEntity("Lemon Tea", "2 kcal", "0 g", "0.4 g", "0 g", "2 mg", "1 mg",
                "Refreshing tea with lemon profile. Supports hydration and can be soothing during colds."
            )
        )

        teaDao.deleteTeasNotIn(supportedTeas.map { it.teaType })

        supportedTeas.forEach { tea ->
            try {
                teaDao.insertTea(tea)
            } catch (e: Exception) {
                Log.d("TeaDatabase", "Tea already exists: ${tea.teaType}")
            }
        }
    }
}
