package com.tarot.card.meaning.tarot

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Entity(tableName = "readings", indices = [Index("spread"), Index("createdAt")])
data class ReadingEntity(
    @PrimaryKey val id: String,
    val spread: String,
    val createdAt: Long,
    val snapshot: String,
    val favorite: Boolean = false
)

@Dao
interface TarotDao {
    @Query("SELECT * FROM readings ORDER BY createdAt DESC")
    fun observe(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE spread = :spread ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(spread: String): ReadingEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reading: ReadingEntity)

    @Query("UPDATE readings SET favorite = :favorite WHERE id = :id")
    suspend fun favorite(id: String, favorite: Boolean)
}

@Database(entities = [ReadingEntity::class], version = 1, exportSchema = false)
abstract class TarotDatabase : RoomDatabase() {
    abstract fun readings(): TarotDao

    companion object {
        @Volatile private var instance: TarotDatabase? = null
        fun get(context: Context): TarotDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TarotDatabase::class.java, "tarot_readings.db")
                .build().also { instance = it }
        }
    }
}

class TarotPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("tarot_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()
    val profile: Profile?
        get() {
            val raw = runCatching { preferences.getString("profile", null) }.getOrNull() ?: return null
            val parsed = runCatching { gson.fromJson(raw, Profile::class.java) }.getOrNull() ?: return null
            val error = ProfileValidator.validate(parsed)
            if (error != null && error != ProfileValidationError.BIRTHDAY_ZODIAC_MISMATCH) return null
            // Older versions allowed inconsistent pairs. Keep the actual birthday
            // and derive its sign on read, without erasing or rewriting the raw record.
            return parsed.copy(name = ProfileValidator.normalizeName(parsed.name),
                zodiac = requireNotNull(ZodiacDates.signFor(parsed.birthday)))
        }

    /** Invalid input never overwrites an existing valid profile. Null means success. */
    fun saveProfile(value: Profile): ProfileValidationError? {
        ProfileValidator.validate(value)?.let { return it }
        val normalized = value.copy(name = ProfileValidator.normalizeName(value.name))
        preferences.edit().putString("profile", gson.toJson(normalized)).apply()
        return null
    }
    var dark: Boolean
        get() = preferences.getBoolean("dark", false)
        set(value) { preferences.edit().putBoolean("dark", value).apply() }
    var showGuidance: Boolean
        get() = preferences.getBoolean("show_guidance", true)
        set(value) { preferences.edit().putBoolean("show_guidance", value).apply() }
    var reverseChance: Int
        get() = preferences.getInt("reverse_chance", 0).coerceIn(0, 100)
        set(value) { preferences.edit().putInt("reverse_chance", value.coerceIn(0, 100)).apply() }
}

class TarotRepository(context: Context) {
    private val appContext = context.applicationContext
    val dao = TarotDatabase.get(context).readings()
    private val gson = Gson()
    suspend fun loadCards(): List<TarotCard> = withContext(Dispatchers.IO) {
        val type = object : TypeToken<List<TarotCard>>() {}.type
        val cards = appContext.assets.open("tarot/tarot_data.json").bufferedReader().use { gson.fromJson<List<TarotCard>>(it, type) }
        require(cards.size == 78 && cards.map { it.slug }.distinct().size == 78) { "Invalid 78-card deck" }
        val originalJson = appContext.assets.open(TarotInterpretations.ASSET_PATH).bufferedReader().use { it.readText() }
        TarotInterpretations.attach(cards, originalJson)
    }
    fun snapshot(entity: ReadingEntity): ReadingSnapshot = gson.fromJson(entity.snapshot, ReadingSnapshot::class.java)
    suspend fun save(reading: ReadingSnapshot) = dao.insert(ReadingEntity(reading.id, reading.spread, reading.createdAt, gson.toJson(reading)))
}
