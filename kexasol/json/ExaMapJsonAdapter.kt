package com.badoo.kexasol.json

import com.squareup.moshi.*

internal class ExaMapJsonAdapter {
    @FromJson
    @JsonExaMap
    fun fromJson(reader: JsonReader): Map<String, Any>? {
        val map = mutableMapOf<String, Any>()

        reader.beginObject()

        while (reader.hasNext()) {
            map[reader.nextName()] = when (reader.peek()) {
                JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                JsonReader.Token.NUMBER -> reader.nextLong()
                else -> reader.nextString()
            }
        }

        reader.endObject()

        return map
    }

    @ToJson
    fun toJson(writer: JsonWriter, @JsonExaMap map: Map<@JvmSuppressWildcards String, @JvmSuppressWildcards Any>?) {
        if (map == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()

        map.forEach { (k, v) ->
            writer.name(k)

            when (v) {
                is Int -> writer.value(v)
                is Long -> writer.value(v)
                is Boolean -> writer.value(v)
                is String -> writer.value(v)
                else -> throw IllegalArgumentException("Unexpected type of map element: $k")
            }
        }

        writer.endObject()
    }
}

@JsonQualifier
internal annotation class JsonExaMap
