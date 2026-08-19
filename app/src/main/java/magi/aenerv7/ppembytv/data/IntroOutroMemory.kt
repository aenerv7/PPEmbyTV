package magi.aenerv7.ppembytv.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Remembers the manually tuned intro/outro skip times per season id, stored as a
 * Gson JSON array (capped at [MAX_CACHE] entries, least-recently-touched dropped).
 */
class IntroOutroMemory(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getSeasonIntroOutro(seasonId: String): SeasonIntroOutroMemory? =
        getSeasonMemories().firstOrNull { it.seasonId == seasonId }

    fun saveSeasonIntroOutro(seasonId: String, manualIntroTime: Long?, manualOutroRemainingMs: Long?) {
        val memories = getSeasonMemories().toMutableList()
        memories.removeAll { it.seasonId == seasonId }
        memories.add(SeasonIntroOutroMemory(seasonId, manualIntroTime, manualOutroRemainingMs))
        val capped = if (memories.size > MAX_CACHE) {
            memories.sortedBy { it.timestamp }.takeLast(MAX_CACHE)
        } else {
            memories
        }
        saveSeasonMemories(capped)
        Log.d(
            TAG,
            "保存季片头片尾记忆: seasonId=" + seasonId + ", intro=" + manualIntroTime + ", outroRemaining=" + manualOutroRemainingMs,
        )
    }

    private fun getSeasonMemories(): List<SeasonIntroOutroMemory> {
        val json = prefs.getString(KEY_SEASON_MEMORY, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SeasonIntroOutroMemory>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析季片头片尾记忆失败", e)
            emptyList()
        }
    }

    private fun saveSeasonMemories(list: List<SeasonIntroOutroMemory>) {
        prefs.edit().putString(KEY_SEASON_MEMORY, gson.toJson(list)).apply()
    }

    data class SeasonIntroOutroMemory(
        val seasonId: String,
        val manualIntroTime: Long? = null,
        val manualOutroRemainingMs: Long? = null,
        val timestamp: Long = System.currentTimeMillis(),
    )

    companion object {
        private const val KEY_SEASON_MEMORY = "season_intro_outro_memory"
        private const val MAX_CACHE = 100
        private const val PREFS_NAME = "intro_outro_memory"
        private const val TAG = "IntroOutroMemory"
    }
}
