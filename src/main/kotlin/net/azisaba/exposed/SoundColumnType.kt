package net.azisaba.exposed

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.io.InputStream
import java.nio.ByteBuffer
import java.sql.Blob
import java.util.OptionalLong

internal class SoundColumnType(private val length: Int) : ColumnType<Sound>() {
    override fun sqlType(): String = when (currentDialect) {
        is PostgreSQLDialect -> "BYTEA"
        is MysqlDialect -> "VARBINARY(${length})"
        is H2Dialect -> "VARBINARY(${length})"
        else -> "BLOB"
    }

    override fun valueFromDB(value: Any): Sound? {
        val bytes = when (value) {
            is ByteArray -> value
            is Blob -> value.binaryStream.use(InputStream::readAllBytes)
            else -> error("Unexpected value for Sound: $value (${value::class})")
        }.takeIf(ByteArray::isNotEmpty) ?: return null

        val buffer = ByteBuffer.wrap(bytes)

        val name = readName(buffer)
        val source = readSource(buffer)
        val volume = readVolume(buffer)
        val pitch = readPitch(buffer)
        val seed = readSeed(buffer)

        return Sound.sound()
            .type(name)
            .source(source)
            .volume(volume)
            .pitch(pitch)
            .seed(seed?.let(OptionalLong::of) ?: OptionalLong.empty())
            .build()
    }

    override fun notNullValueToDB(value: Sound): Any {
        val nameBytes = value.name().asString().toByteArray(Charsets.UTF_8)
        val sourceBytes = value.source().name.toByteArray(Charsets.UTF_8)

        require(nameBytes.size <= 255) { "Name too long: ${nameBytes.size}" }
        require(sourceBytes.size <= 255) { "Source too long: ${sourceBytes.size}" }

        val hasSeed = value.seed().isPresent

        val size = 1 + nameBytes.size +
                1 + sourceBytes.size +
                4 + 4 +
                sizeOfSeed(hasSeed)

        val buffer = ByteBuffer.allocate(size)

        writeName(buffer, nameBytes)
        writeSource(buffer, sourceBytes)
        writeVolume(buffer, value.volume())
        writePitch(buffer, value.pitch())
        writeSeed(buffer, value.seed())

        val result = buffer.array()
        require(result.size <= length) {
            "Encoded Sound too large: ${result.size} > $length"
        }

        return result
    }

    private fun readName(buffer: ByteBuffer): Key {
        val len = buffer.get().toInt() and 0xFF

        require(buffer.remaining() >= len) {
            "Invalid Sound encoding: not enough bytes (needed=$len, remaining=${buffer.remaining()})"
        }

        val bytes = ByteArray(len)
        buffer.get(bytes)

        val str = runCatching { bytes.decodeToString() }
            .getOrElse { error("Invalid UTF-8 for Sound name") }

        return Key.key(str)
    }

    private fun writeName(buffer: ByteBuffer, bytes: ByteArray) {
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
    }

    private fun readSource(buffer: ByteBuffer): Sound.Source {
        val len = buffer.get().toInt() and 0xFF

        require(buffer.remaining() >= len) {
            "Invalid Sound encoding: not enough bytes (needed=$len, remaining=${buffer.remaining()})"
        }

        val bytes = ByteArray(len)
        buffer.get(bytes)

        val name = runCatching { bytes.decodeToString() }
            .getOrElse { error("Invalid UTF-8 for Sound.Source") }

        return runCatching { Sound.Source.valueOf(name) }
            .getOrElse { error("Unknown Sound.Source: $name") }
    }

    private fun writeSource(buffer: ByteBuffer, bytes: ByteArray) {
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
    }

    private fun readVolume(buffer: ByteBuffer): Float = buffer.getFloat()

    private fun writeVolume(buffer: ByteBuffer, volume: Float) {
        buffer.putFloat(volume)
    }

    private fun readPitch(buffer: ByteBuffer): Float = buffer.getFloat()

    private fun writePitch(buffer: ByteBuffer, pitch: Float) {
        buffer.putFloat(pitch)
    }

    private fun readSeed(buffer: ByteBuffer): Long? {
        val hasSeed = buffer.get().toInt() != 0
        return if (hasSeed) buffer.getLong() else null
    }

    private fun writeSeed(buffer: ByteBuffer, seed: OptionalLong) {
        if (seed.isPresent) {
            buffer.put(1)
            buffer.putLong(seed.asLong)
        } else {
            buffer.put(0)
        }
    }

    private fun sizeOfSeed(hasSeed: Boolean): Int = 1 + if (hasSeed) 8 else 0
}
