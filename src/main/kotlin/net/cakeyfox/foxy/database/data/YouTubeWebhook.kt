package net.cakeyfox.foxy.database.data

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeWebhook(
    val channelId: String,
    val createdAt: Long,
    val leaseSeconds: Long
)
