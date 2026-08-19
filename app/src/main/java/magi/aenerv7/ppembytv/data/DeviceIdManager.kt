package magi.aenerv7.ppembytv.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.UUID

object DeviceIdManager {

    private const val PREF_NAME = "emby_device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    @Volatile
    private var cachedDeviceId: String? = null

    private fun getPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun clearDeviceId(context: Context) {
        getPreferences(context).edit().remove(KEY_DEVICE_ID).apply()
        cachedDeviceId = null
        Log.w("DeviceIdManager", "DeviceId已清除")
    }

    @Synchronized
    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }
        val preferences = getPreferences(context)
        var id = preferences.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            preferences.edit().putString(KEY_DEVICE_ID, id).apply()
            Log.i("DeviceIdManager", "生成新的DeviceId: $id")
        } else {
            Log.d("DeviceIdManager", "读取已存在的DeviceId: $id")
        }
        cachedDeviceId = id
        return id
    }

    fun hasDeviceId(context: Context): Boolean =
        getPreferences(context).contains(KEY_DEVICE_ID)
}
