package com.floracare.app.data.remote.perenual

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class LenientStringListAdapterFactoryTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(LenientStringListAdapterFactory())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(Wrapper::class.java)

    @Test
    fun `parses array of strings as list`() {
        val parsed = adapter.fromJson("""{"values":["full_sun","part_shade"]}""")!!
        assertEquals(listOf("full_sun", "part_shade"), parsed.values)
    }

    @Test
    fun `parses single string as singleton list`() {
        val parsed = adapter.fromJson("""{"values":"full_sun"}""")!!
        assertEquals(listOf("full_sun"), parsed.values)
    }

    @Test
    fun `parses null as empty list`() {
        val parsed = adapter.fromJson("""{"values":null}""")!!
        assertEquals(emptyList<String>(), parsed.values)
    }

    @Test
    fun `parses missing field as empty list`() {
        val parsed = adapter.fromJson("""{}""")!!
        assertEquals(emptyList<String>(), parsed.values)
    }

    @Test
    fun `parses blank string as empty list`() {
        val parsed = adapter.fromJson("""{"values":""}""")!!
        assertEquals(emptyList<String>(), parsed.values)
    }

    @Test
    fun `parses unexpected json shape as empty list`() {
        val parsed = adapter.fromJson("""{"values":42}""")!!
        assertEquals(emptyList<String>(), parsed.values)
    }

    @Test
    fun `serializes list back to json array`() {
        val json = adapter.toJson(Wrapper(values = listOf("a", "b")))
        assertEquals("""{"values":["a","b"]}""", json)
    }

    data class Wrapper(
        @LenientStringList val values: List<String> = emptyList(),
    )
}
