package com.ujwal.colai.core.database

import androidx.room.TypeConverter
import org.json.JSONObject

/**
 * Room TypeConverters for serializing complex object types into SQLite primitives.
 */
class Converters {

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        if (map == null) return null
        val json = JSONObject()
        for ((key, value) in map) {
            json.put(key, value)
        }
        return json.toString()
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        if (value.isNullOrBlank()) return null
        return try {
            val json = JSONObject(value)
            val result = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = json.optString(key)
            }
            result
        } catch (e: Exception) {
            null
        }
    }
}
