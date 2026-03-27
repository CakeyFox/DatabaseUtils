package net.cakeyfox.foxy.database.core.utils

import net.cakeyfox.foxy.database.utils.builders.GuildBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.Document
import kotlin.reflect.KClass
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.flow.firstOrNull
import mu.KotlinLogging
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.push
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import net.cakeyfox.foxy.database.common.data.guild.Case
import net.cakeyfox.foxy.database.common.data.guild.CaseType
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.data.guild.AntiRaidModule
import net.cakeyfox.foxy.database.data.guild.AutoRoleModule
import net.cakeyfox.foxy.database.data.guild.DashboardLog
import net.cakeyfox.foxy.database.data.guild.FoxyverseGuild
import net.cakeyfox.foxy.database.data.guild.Guild
import net.cakeyfox.foxy.database.data.guild.GuildSettings
import net.cakeyfox.foxy.database.data.guild.InviteBlockerSettings
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.guild.ModerationUtils
import net.cakeyfox.foxy.database.data.guild.MusicSettings
import net.cakeyfox.foxy.database.data.guild.ServerLogModule
import net.cakeyfox.foxy.database.data.guild.TempBan
import net.cakeyfox.foxy.database.data.guild.WelcomerModule
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration.Companion.days

class GuildUtils(
    private val client: DatabaseClient
) {
    private val logger = KotlinLogging.logger { }

    suspend fun getFoxyverseGuildOrNull(guildId: String): FoxyverseGuild? {
        return client.withRetry {
            val query = Document("_id", guildId)
            client.foxyverseGuilds.find(query).firstOrNull()
        }
    }

    suspend fun getAllExpiredBans(): Map<String, List<TempBan>> {
        return client.withRetry {
            val guildsCollection = client.database.getCollection<Document>("guilds")
            val now = Clock.System.now()

            val allGuilds = guildsCollection.find().toList()

            allGuilds.mapNotNull { guildDoc ->
                val guildJson = guildDoc.toJson()
                val guild = client.json.decodeFromString<Guild>(guildJson)
                val expiredBans = guild.tempBans.orEmpty().filter { it.duration != null && it.duration <= now }

                if (expiredBans.isNotEmpty()) guild._id to expiredBans else null
            }.toMap()
        }
    }

    suspend fun addLogToGuild(guildId: String, authorId: String, actionType: String) {
        return client.withRetry {
            client.guilds.updateOne(
                eq("_id", guildId),
                push(
                    "dashboardLogs", DashboardLog(
                        authorId,
                        actionType,
                        date = Clock.System.now().toEpochMilliseconds()
                    )
                )
            )
        }
    }

    suspend fun addTempBanToGuild(guildId: String, tempBan: TempBan) {
        return client.withRetry {
            client.guilds.updateOne(
                eq("_id", guildId),
                push("tempBans", tempBan)
            )
        }
    }

    suspend fun removeTempBanFromGuild(guildId: String, userId: String) {
        return client.withRetry {
            client.guilds.updateOne(
                eq("_id", guildId),
                pull("tempBans", eq("userId", userId))
            )
        }
    }

    suspend fun getKeyByGuildId(guildId: String): Key? {
        return client.withRetry {
            val query = Document("usedBy", guildId)
            client.premiumKeys.find(query).firstOrNull()
        }
    }

    suspend fun getGuild(guildId: String): Guild {
        return updateGuildWithNewFields(guildId)
    }

    suspend fun getGuildsLeftMoreThan90Days(): List<Guild> {
        return client.withRetry {
            val ninetyDaysAgo = Date.from(
                (Clock.System.now() - 90.days).toJavaInstant()
            )

            val query = Document("leftAt", Document("\$lt", ninetyDaysAgo))

            client.guilds.find(query).toList()
        }
    }

    suspend fun getGuildOrNull(guildId: String): Guild? {
        return client.withRetry {
            val query = Document("_id", guildId)
            client.guilds.find(query).firstOrNull()
        }
    }

    suspend fun getGuildsByFollowedYouTubeChannel(channelId: String): List<Guild> {
        return client.withRetry {
            val query = Document("followedYouTubeChannels.channelId", channelId)
            client.guilds.find(query).toList()
        }
    }

    suspend fun updateGuild(guildId: String, block: GuildBuilder.() -> Unit) {
        val builder = GuildBuilder().apply(block)
        val collection = client.database.getCollection<Guild>("guilds")
        val update = builder.toDocument()
        val query = Document("_id", guildId)

        collection.updateOne(query, update)
    }

    suspend fun deleteGuild(guildId: String) {
        client.withRetry {
            val guilds = client.database.getCollection<Document>("guilds")
            guilds.deleteOne(eq("_id", guildId))
        }
    }

    suspend fun getCaseById(guildId: String, caseId: Long): Case? {
        return client.withRetry {
            val cases = client.database.getCollection<Case>("cases")

            val filter = Document()
                .append("guildId", guildId)
                .append("caseId", caseId)

            cases.find(filter).firstOrNull()
        }
    }

    /**
     * Register a punishment as a [Case] and stores the id
     * @param guildId
     * @param punishedMembers
     * @param punishedStaff
     * @param type The punishment [CaseType]
     * @param reason
     * @param punishmentDuration The punishment duration using [Instant]
     */
    suspend fun registerPunishmentAsCase(
        guildId: String,
        punishedMembers: List<String>,
        staff: String,
        type: CaseType,
        reason: String? = null,
        duration: Instant? = null,
    ): Case {
        return client.withRetry {
            val caseId = getNextCaseId(guildId)

            val case = Case(
                guildId,
                caseId,
                reason,
                duration,
                Clock.System.now(),
                type,
                staff,
                punishedMembers,
                true,
            )

            val documentToJSON = client.json.encodeToString(case)
            val document = Document.parse(documentToJSON)
            client.database
                .getCollection<Document>("cases")
                .insertOne(document)

            case
        }
    }

    suspend fun getCaseByUserId(guildId: String, userId: String): List<Case> {
        return client.withRetry {
            val cases = client.database.getCollection<Case>("cases")

            val filter = Document()
                .append("guildId", guildId)
                .append("members", userId)

            return@withRetry cases.find(filter).toList()
        }
    }

    private suspend fun getNextCaseId(guildId: String): Long {
        return client.withRetry {
            val result = client.guilds.findOneAndUpdate(
                Document("_id", guildId),
                Document("\$inc", Document("registeredCases", 1)),
                FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER)
            )

            result?.registeredCases ?: 0
        }
    }

    private suspend fun createGuild(guildId: String): Guild {
        return client.withRetry {
            val guilds = client.database.getCollection<Document>("guilds")

            val newGuild = Guild(
                _id = guildId,
                guildAddedAt = System.currentTimeMillis(),
                GuildJoinLeaveModule = WelcomerModule(),
                antiRaidModule = AntiRaidModule(),
                AutoRoleModule = AutoRoleModule(),
                guildSettings = GuildSettings(),
                musicSettings = MusicSettings(),
                serverLogModule = ServerLogModule(),
                moderationUtils = ModerationUtils()
            )

            val documentToJSON = client.json.encodeToString(newGuild)
            val document = Document.parse(documentToJSON)
            guilds.insertOne(document)

            newGuild
        }
    }

    // Adding missing fields if necessary

    private suspend fun updateGuildWithNewFields(guildId: String): Guild {
        return client.withRetry {
            val guilds = client.database.getCollection<Document>("guilds")

            val existingDocument =
                guilds.find(eq("_id", guildId))
                    .firstOrNull() ?: return@withRetry createGuild(guildId)

            val documentToJSON = existingDocument.toJson()

            client.json.decodeFromString<Guild>(documentToJSON.toString())
        }
    }
}