package net.cakeyfox.foxy.database.data.guild

import kotlinx.serialization.Serializable

@Serializable
data class Key(
    val key: String,
    val usedBy: String? = null,
    val ownedBy: String? = null,
)
