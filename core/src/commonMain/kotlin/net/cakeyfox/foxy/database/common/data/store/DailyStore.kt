package net.cakeyfox.foxy.database.data.store

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.cakeyfox.foxy.database.common.data.MongoDateSerializer

@Serializable
data class DailyStore(
    val id: String,
    val itens: List<Item>,
    @Serializable(with = MongoDateSerializer::class)
    val lastUpdate: Instant?,
) {
    @Serializable
    data class Item(
        val id: String,
        val type: String,
    )
}
