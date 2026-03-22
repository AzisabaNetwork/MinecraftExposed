package net.azisaba.exposed

import net.kyori.adventure.util.TriState
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.sql.Blob

internal object TriStateColumnType : ColumnType<TriState>() {
    override fun sqlType(): String = when (currentDialect) {
        is PostgreSQLDialect -> "SMALLINT"
        is MysqlDialect -> "TINYINT"
        is H2Dialect -> "TINYINT"
        else -> "SMALLINT"
    }

    override fun valueFromDB(value: Any): TriState {
        val intValue = when (value) {
            is Number -> value.toInt()
            is ByteArray -> value.firstOrNull()?.toInt()?.and(0xFF) ?: error("Empty ByteArray for TriState")
            is Blob -> value.binaryStream.use {
                val v = it.read()
                require(v != -1) { "Empty Blob for TriState" }
                v
            }
            else -> error("Unexpected value for TriState: $value (${value::class})")
        }

        return when (intValue) {
            0 -> TriState.NOT_SET
            1 -> TriState.FALSE
            2 -> TriState.TRUE
            else -> error("Invalid TriState value: $intValue")
        }
    }

    override fun notNullValueToDB(value: TriState): Any = when (value) {
        TriState.NOT_SET -> 0
        TriState.FALSE -> 1
        TriState.TRUE -> 2
    }
}
