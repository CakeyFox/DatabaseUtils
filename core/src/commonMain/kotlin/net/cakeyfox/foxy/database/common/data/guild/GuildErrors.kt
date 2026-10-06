package net.cakeyfox.foxy.database.common.data.guild

import kotlinx.serialization.Serializable

@Serializable
data class GuildErrors(
    val guildId: String,
    val errorCode: Int? = 0,
    val affectedMembers: List<String>? = emptyList(),
    val message: String? = null,
    val requiredPermission: String? = null,
)
