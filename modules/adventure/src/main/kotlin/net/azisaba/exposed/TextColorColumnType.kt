package net.azisaba.exposed

import net.kyori.adventure.text.format.TextColor
import org.jetbrains.exposed.v1.core.ColumnType

internal object TextColorColumnType : ColumnType<TextColor>() {
    override fun sqlType(): String = "INT"

    override fun valueFromDB(value: Any): TextColor {
        val intValue = when (value) {
            is Int -> value
            is Number -> value.toInt()
            else -> error("Unexpected value for TextColor: $value (${value::class})")
        }
        return TextColor.color(intValue)
    }

    override fun notNullValueToDB(value: TextColor): Any = value.value()
}
