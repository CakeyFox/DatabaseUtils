package net.cakeyfox.foxy.database.common.data.marry

import kotlinx.serialization.Serializable

@Serializable
data class CoupleStoreItem(
    val _id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
    val enabled: Boolean = true,
    val card: Card
) {
    @Serializable
    data class Card(
        val type: String,
        val affinityPoints: Int
    )
}
