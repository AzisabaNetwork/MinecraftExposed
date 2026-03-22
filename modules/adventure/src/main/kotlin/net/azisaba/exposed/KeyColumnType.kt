package net.azisaba.exposed

import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.ColumnType

internal class KeyColumnType(private val length: Int) : ColumnType<Key>() {
    override fun sqlType(): String = "VARCHAR($length)"

    override fun valueFromDB(value: Any): Key = when (value) {
        is String -> Key.key(value)
        else -> error("Unexpected value for Key: $value (${value::class})")
    }

    override fun notNullValueToDB(value: Key): Any = value.asString()
}
