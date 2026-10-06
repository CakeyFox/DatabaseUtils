package net.cakeyfox.foxy.database.core.utils

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.push
import com.mongodb.client.model.Updates.set
import mu.KotlinLogging
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.data.bot.YouTubeWebhook
import org.bson.Document
import java.time.Instant

class YouTubeUtils(
    private val client: DatabaseClient
) {
    private val logger = KotlinLogging.logger {}

    suspend fun removeChannelFromGuild(guildId: String, channelId: String) {
        client.withRetry {
            client.collections.guilds.updateOne(
                eq("_id", guildId),
                pull("followedYouTubeChannels", Document("channelId", channelId))
            )
            client.guild.invalidateCache(guildId)
        }
    }

    suspend fun getAllFollowedYouTubeChannelIds(): List<String> {
        val guilds = client.collections.guilds.findMany()

        return guilds
            .flatMap { guild -> guild.followedYouTubeChannels.map { it.channelId } }
            .distinct()
    }

    suspend fun addChannelToAGuild(guildId: String, channelId: String, textChannelId: String, message: String?) {
        client.withRetry {
            val channelDoc = mapOf(
                "channelId" to channelId,
                "notificationMessage" to message,
                "channelToSend" to textChannelId,
                "notifiedVideos" to emptyList<String>()
            )

            client.collections.guilds.updateOne(
                eq("_id", guildId),
                push("followedYouTubeChannels", Document(channelDoc))
            )
            client.guild.invalidateCache(guildId)
        }
    }

    suspend fun updateChannelCustomMessage(
        guildId: String,
        youtubeChannelId: String,
        discordChannelId: String?,
        message: String?
    ) {
        client.withRetry {
            client.collections.guilds.findOneAndUpdate(
                and(
                    eq("_id", guildId),
                    eq("followedYouTubeChannels.channelId", youtubeChannelId)
                ),
                combine(
                    buildList {
                        add(set("followedYouTubeChannels.$.notificationMessage", message))

                        if (discordChannelId != null) {
                            add(set("followedYouTubeChannels.$.channelToSend", discordChannelId))
                        }
                    }
                )
            )
            client.guild.invalidateCache(guildId)
        }
    }

    suspend fun addVideoToList(guildId: String, channelId: String, videoId: String) {
        return client.withRetry {
            val query = Document("_id", guildId)

            val update = Document(
                "\$push", Document(
                    "followedYouTubeChannels.$[elem].notifiedVideos",
                    Document("id", videoId).append("notifiedAt", Instant.now())
                )
            )

            client.collections.guilds.updateOne(
                query,
                update,
                arrayFilters = listOf(Document("elem.channelId", channelId))
            )
            client.guild.invalidateCache(guildId)
        }
    }

    suspend fun getYouTubeWebhooks(): List<YouTubeWebhook> {
        return client.withRetry {
            client.collections.youtubeWebhooks.findMany()
        }
    }

    suspend fun getOrRegisterYouTubeWebhook(channelId: String): YouTubeWebhook {
        return client.withRetry {
            client.collections.youtubeWebhooks.findOne(eq("channelId", channelId))
                ?: return@withRetry registerOrUpdateYouTubeWebhook(channelId)
        }
    }

    suspend fun registerOrUpdateYouTubeWebhook(channelId: String): YouTubeWebhook {
        return client.withRetry {
            val newWebhook = YouTubeWebhook(
                channelId = channelId,
                createdAt = System.currentTimeMillis(),
                leaseSeconds = 432_000
            )

            val result = client.collections.youtubeWebhooks.replaceOne(
                eq("channelId", channelId),
                newWebhook,
                upsert = true
            )

            if (result.matchedCount == 0L) {
                logger.info { "Created new webhook for $channelId" }
            } else {
                logger.info { "Updated webhook for $channelId" }
            }

            newWebhook
        }
    }
}
