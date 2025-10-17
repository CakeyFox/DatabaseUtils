package net.cakeyfox.foxy.database.data.bot

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeWebhook(
    val channelId: String,
    val createdAt: Long,
    val leaseSeconds: Long
)
