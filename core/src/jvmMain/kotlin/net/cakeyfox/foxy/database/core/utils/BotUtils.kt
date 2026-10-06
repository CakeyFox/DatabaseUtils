package net.cakeyfox.foxy.database.core.utils

import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.data.bot.Command
import org.bson.Document
import java.util.UUID


class BotUtils(
    val client: DatabaseClient
) {
    companion object {
        @Serializable
        data class BotSettings(
            val activity: String,
            val status: String,
            val avatarUrl: String?,
            val lastBroadcastMessageId: String?,
            val lastBroadcastMessageContent: String?
        )

        private val logger = KotlinLogging.logger { }
    }

    suspend fun getOrRegisterCommand(command: Command): Command {
        return client.collections.commands.findOne(Document("name", command.name)) ?: run {
            client.collections.commands.insertOne(command)
            command
        }
    }

    suspend fun updateCommandUsage(commandName: String): Boolean? {
        val query = Document("name", commandName)

        if (!client.collections.commands.exists(query)) return null

        client.collections.commands.updateOne(
            query,
            Document("\$inc", Document("usageCount", 1))
        )

        return true
    }

    suspend fun getBotSettings(): BotSettings {
        return client.collections.botSettings.findOne(Document()) ?: run {
            createBotSettings()
            defaultSettings()
        }
    }

    suspend fun setBroadcastMessage(messageContent: String) {
        val botSettingsData = client.collections.botSettings.findOne(Document()) ?: run {
            createBotSettings()
            return
        }

        val newBotSettings = botSettingsData.copy(
            lastBroadcastMessageId = UUID.randomUUID().toString(),
            lastBroadcastMessageContent = messageContent
        )

        client.collections.botSettings.replaceOne(Document(), newBotSettings)
    }

    suspend fun getActivity(): String {
        return (client.collections.botSettings.findOne(Document()) ?: run {
            createBotSettings()
            defaultSettings()
        }).activity
    }

    private suspend fun createBotSettings() {
        logger.info { "Generating Foxy settings..." }

        client.collections.botSettings.insertOne(defaultSettings())
    }

    private fun defaultSettings() = BotSettings(
        activity = "foxybot.xyz · /help",
        status = "online",
        avatarUrl = null,
        lastBroadcastMessageId = null,
        lastBroadcastMessageContent = null
    )
}
