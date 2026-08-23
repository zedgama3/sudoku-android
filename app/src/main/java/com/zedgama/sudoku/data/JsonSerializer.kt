package com.zedgama.sudoku.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** DataStore serializer that stores any @Serializable type as JSON. */
class JsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T
) : Serializer<T> {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): T = try {
        json.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (_: Exception) {
        defaultValue
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}
