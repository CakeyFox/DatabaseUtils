package net.cakeyfox.foxy.database.core.utils

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
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
        val allCommands = client.database.getCollection<Document>("commands")
        val commandQuery = Document("name", command.name)
        val existingDocument = allCommands.find(commandQuery).firstOrNull()

        if (existingDocument == null) {
            val documentToJSON = client.json.encodeToString(command)
            val document = Document.parse(documentToJSON)

            allCommands.insertOne(document)

            return command
        }

        val documentToJSON = existingDocument.toJson()
        return client.json.decodeFromString<Command>(documentToJSON)
    }

    suspend fun updateCommandUsage(commandName: String): Boolean? {
        val allCommands = client.database.getCollection<Document>("commands")
        val commandQuery = Document("name", commandName)
        val existingDocument = allCommands.find(commandQuery).firstOrNull()
        val update = Document("\$inc", Document("usageCount", 1))

        if (existingDocument == null) return null
        allCommands.updateOne(commandQuery, update)

        return true
    }

    suspend fun getBotSettings(): BotSettings {
        val botSettings = client.database.getCollection<Document>("botSettings")
        val botSettingsDocument = botSettings.find().firstOrNull()
            ?: run {
                createBotSettings(botSettings)
                return BotSettings(
                    activity = "foxybot.xyz · /help",
                    status = "online",
                    avatarUrl = null,
                    lastBroadcastMessageId = null,
                    lastBroadcastMessageContent = null
                )
            }

        val documentToJSON = botSettingsDocument.toJson()
        return client.json.decodeFromString(documentToJSON)
    }

    suspend fun setBroadcastMessage(messageContent: String) {
        val botSettings = client.database.getCollection<Document>("botSettings")
        val botSettingsDocument = botSettings.find().firstOrNull()
            ?: run {
                createBotSettings(botSettings)
                return
            }

        val documentToJSON = botSettingsDocument.toJson()
        val botSettingsData = client.json.decodeFromString<BotSettings>(documentToJSON)
        val broadcastMessageId = UUID.randomUUID().toString()

        val newBotSettings = botSettingsData.copy(
            lastBroadcastMessageId = broadcastMessageId,
            lastBroadcastMessageContent = messageContent
        )

        val documentToJSON2 = client.json.encodeToString(newBotSettings)
        val document2 = Document.parse(documentToJSON2)

        botSettings.updateOne(botSettingsDocument, document2)
    }

    suspend fun getActivity(): String {
        val botSettings = client.database.getCollection<Document>("botSettings")
        val botSettingsDocument = botSettings.find().firstOrNull()
            ?: run {
                createBotSettings(botSettings)
                return "foxybot.xyz · /help"
            }

        val documentToJSON = botSettingsDocument.toJson()
        val botSettingsData = client.json.decodeFromString<BotSettings>(documentToJSON)

        return botSettingsData.activity
    }

    private suspend fun createBotSettings(botSettingsCollection: MongoCollection<Document>) {
        val botSettings = BotSettings(
            activity = "foxybot.xyz · /help",
            status = "online",
            avatarUrl = null,
            lastBroadcastMessageId = null,
            lastBroadcastMessageContent = null
        )

        logger.info { "Generating Foxy settings..." }

        val documentToJSON = client.json.encodeToString(botSettings)
        val document = Document.parse(documentToJSON)

        botSettingsCollection.insertOne(document)
    }
}