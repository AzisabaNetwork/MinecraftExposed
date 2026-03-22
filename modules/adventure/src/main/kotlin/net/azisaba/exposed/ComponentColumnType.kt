package net.azisaba.exposed

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import org.jetbrains.exposed.v1.core.ColumnType

internal class ComponentColumnType(private val serializer: JSONComponentSerializer) : ColumnType<Component>() {
    override fun sqlType(): String = "TEXT"

    override fun valueFromDB(value: Any): Component = when (value) {
        is String -> serializer.deserialize(value)
        else -> error("Unexpected value for Component: $value (${value::class})")
    }

    override fun notNullValueToDB(value: Component): Any = serializer.serialize(value)
}
