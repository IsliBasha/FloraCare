package com.floracare.app.data.remote.perenual

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class LenientStringList

/**
 * Perenual's list endpoint returns fields like `sunlight` and `scientific_name`
 * as either a JSON array, a single string, or null. This adapter normalizes any
 * of those shapes into a `List<String>`.
 */
internal class LenientStringListAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? {
        val qualifier = annotations.firstOrNull { it is LenientStringList } ?: return null
        if (Types.getRawType(type) != List::class.java) return null
        val elementType = Types.collectionElementType(type, List::class.java)
        if (elementType != String::class.java) return null
        val delegate = moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java),
            annotations - qualifier,
        )
        return Adapter(delegate)
    }

    private class Adapter(private val delegate: JsonAdapter<List<String>>) : JsonAdapter<List<String>>() {
        override fun fromJson(reader: JsonReader): List<String> = when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                emptyList()
            }
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                if (value.isBlank()) emptyList() else listOf(value)
            }
            JsonReader.Token.BEGIN_ARRAY -> delegate.fromJson(reader) ?: emptyList()
            else -> {
                reader.skipValue()
                emptyList()
            }
        }

        override fun toJson(writer: JsonWriter, value: List<String>?) {
            delegate.toJson(writer, value)
        }
    }
}
