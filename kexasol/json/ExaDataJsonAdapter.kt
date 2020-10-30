package com.badoo.kexasol.json

import com.squareup.moshi.*

internal class ExaDataJsonAdapter {
    @FromJson
    @JsonExaData
    fun fromJson(reader: JsonReader): List<List<Any?>>? {
        val outerList = mutableListOf<List<Any?>>()

        reader.beginArray()

        while (reader.hasNext()) {
            reader.beginArray()

            val innerList = mutableListOf<Any?>()

            while (reader.hasNext()) {
                innerList.add(
                    when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                        JsonReader.Token.NULL -> reader.nextNull()
                        else -> reader.nextString()
                    }
                )
            }

            reader.endArray()
            outerList.add(innerList)
        }

        reader.endArray()

        return outerList
    }

    @ToJson
    fun toJson(
        writer: JsonWriter,
        @JsonExaData outerList: List<@JvmSuppressWildcards List<@JvmSuppressWildcards Any?>>?
    ) {
        if (outerList == null) {
            writer.nullValue()
            return
        }

        writer.beginArray()

        for (innerList in outerList) {
            writer.beginArray()

            for (item in innerList) {
                when (item) {
                    is Boolean? -> writer.value(item)
                    is String? -> writer.value(item)
                    else -> throw IllegalStateException("Unsupported data type for @JsonExaData writer")
                }
            }

            writer.endArray()
        }

        writer.endArray()
    }
}

@JsonQualifier
internal annotation class JsonExaData
