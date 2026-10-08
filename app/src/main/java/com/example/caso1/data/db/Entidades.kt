package com.example.caso1.data.db

import androidx.room.*
import com.example.caso1.data.model.EstadoGalpon
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "galpones")
data class GalponEntity(
    @PrimaryKey val id: Int,
    val granja: String,
    val nombre: String,
    val estado: EstadoGalpon
)

@Entity(tableName = "mediciones")
data class MedicionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val galponId: Int,
    val temperatura: Double,
    val humedad: Double,
    val fechaHora: Long
)

@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val galponId: Int,
    val tipo: String,
    val nivel: String,
    val activa: Boolean,
    val fechaHora: Long
)

@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val galponId: Int,
    val descripcion: String,
    val accionRegistrada: String?,
    val fechaHora: Long
)

@Dao
interface GalponDao {
    @Query("SELECT * FROM galpones")
    fun observarTodos(): Flow<List<GalponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(galpones: List<GalponEntity>)
}

@Dao
interface MedicionDao {
    @Query("SELECT * FROM mediciones WHERE galponId = :galponId ORDER BY fechaHora DESC")
    fun observarPorGalpon(galponId: Int): Flow<List<MedicionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(medicion: MedicionEntity)
}

@Dao
interface AlertaDao {
    @Query("SELECT * FROM alertas WHERE activa = 1 ORDER BY fechaHora DESC")
    fun observarActivas(): Flow<List<AlertaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alerta: AlertaEntity)
}

@Dao
interface EventoDao {
    @Query("SELECT * FROM eventos ORDER BY fechaHora DESC")
    fun observarTodos(): Flow<List<EventoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(evento: EventoEntity)
}

class EstadoConverter {
    @TypeConverter fun aTexto(e: EstadoGalpon): String = e.name
    @TypeConverter fun aEnum(v: String): EstadoGalpon = EstadoGalpon.valueOf(v)
}

@TypeConverters(EstadoConverter::class)
@Database(
    entities = [GalponEntity::class, MedicionEntity::class, AlertaEntity::class, EventoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun galponDao(): GalponDao
    abstract fun medicionDao(): MedicionDao
    abstract fun alertaDao(): AlertaDao
    abstract fun eventoDao(): EventoDao
}
