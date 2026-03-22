package net.azisaba.exposed

import net.kyori.adventure.text.format.ShadowColor
import org.jetbrains.exposed.v1.core.ColumnType

object ShadowColorColumnType : ColumnType<ShadowColor>() {
    override fun sqlType(): String = "INT"

    override fun valueFromDB(value: Any): ShadowColor {
        val intValue = when (value) {
            is Int -> value
            is Number -> value.toInt()
            else -> error("Unexpected value for ShadowColor: $value (${value::class})")
        }
        return ShadowColor.shadowColor(intValue)
    }

    override fun notNullValueToDB(value: ShadowColor): Any = value.value()
}
