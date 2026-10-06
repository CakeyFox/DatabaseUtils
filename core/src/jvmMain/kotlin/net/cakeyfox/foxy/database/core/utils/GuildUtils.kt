package net.cakeyfox.foxy.database.core.utils

import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.model.Filters
import net.cakeyfox.foxy.database.utils.builders.GuildBuilder
import org.bson.Document
import mu.KotlinLogging
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.push
import com.mongodb.client.model.Updates.set
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import net.cakeyfox.foxy.database.common.data.guild.Case
import net.cakeyfox.foxy.database.common.data.guild.CaseType
import net.cakeyfox.foxy.database.common.data.guild.GuildErrors
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.core.encodeToDocument
import net.cakeyfox.foxy.database.data.guild.AntiRaidModule
import net.cakeyfox.foxy.database.data.guild.AutoRoleModule
import net.cakeyfox.foxy.database.data.guild.DashboardLog
import net.cakeyfox.foxy.database.data.guild.Guild
import net.cakeyfox.foxy.database.data.guild.GuildSettings
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.guild.Metrics
import net.cakeyfox.foxy.database.data.guild.ModerationUtils
import net.cakeyfox.foxy.database.data.guild.MusicSettings
import net.cakeyfox.foxy.database.data.guild.ServerLogModule
import net.cakeyfox.foxy.database.data.guild.StrictMode
import net.cakeyfox.foxy.database.data.guild.TempBan
import net.cakeyfox.foxy.database.data.guild.WelcomerModule
import net.cakeyfox.foxy.database.utils.builders.MetricBuilder
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

class GuildUtils(
    private val client: DatabaseClient
) {
    private val logger = KotlinLogging.logger { }
    private val guildCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Guild>()
    private val guildErrorCache = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .build<String, List<GuildErrors>>()

    internal fun updateCache(guildId: String, guild: Guild) {
        guildCache.put(guildId, guild)
    }

    internal fun invalidateCache(guildId: String) {
        guildCache.invalidate(guildId)
    }

    suspend fun getAllExpiredBans(): Map<String, List<TempBan>> {
        return client.withRetry {
            val now = Clock.System.now()

            val allGuilds = client.collections.guilds.findMany()

            allGuilds.mapNotNull { guild ->
                val expiredBans = guild.tempBans.orEmpty().filter { it.duration != null && it.duration <= now }

                if (expiredBans.isNotEmpty()) guild._id to expiredBans else null
            }.toMap()
        }
    }

    suspend fun addErrorLogToGuild(
        guildId: String,
        errorCode: Int? = 0,
        errorMessage: String? = null,
        affectedMembers: List<String>? = emptyList(),
        requiredPermission: String? = null,
    ) {
        return client.withRetry {
            val errorLength = client.collections.guildErrors.count(eq("guildId", guildId))

            if (errorLength >= 10) {
                client.collections.guildErrors.deleteOne(eq("guildId", guildId))
            }

            client.collections.guildErrors.insertOne(
                GuildErrors(
                    guildId,
                    errorCode,
                    affectedMembers,
                    errorMessage,
                    requiredPermission
                )
            )

            guildErrorCache.invalidate(guildId)
            guildErrorCache.put(guildId, client.collections.guildErrors.findMany(eq("guildId", guildId)))
        }
    }

    suspend fun getErrorLogFromGuild(guildId: String): List<GuildErrors> {
        return client.withRetry {
            val logs = client.collections.guildErrors.findMany(eq("guildId", guildId))

            guildErrorCache.put(guildId, logs)
            return@withRetry logs
        }
    }

    suspend fun clearErrorLogFromGuild(guildId: String) {
        return client.withRetry {
            client.collections.guildErrors.deleteMany(eq("guildId", guildId))

            guildErrorCache.invalidate(guildId)
        }
    }

    suspend fun addLogToGuild(guildId: String, authorId: String, actionType: String) {
        return client.withRetry {
            client.collections.guilds.updateOne(
                eq("_id", guildId),
                push(
                    "dashboardLogs",
                    client.encodeToDocument(
                        DashboardLog(
                            authorId,
                            actionType,
                            date = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                )
            )
            invalidateCache(guildId)
        }
    }

    suspend fun clearCache() {
        guildCache.invalidateAll()
    }

    suspend fun removeGuildKey(guildId: String) {
        return client.withRetry {
            val currentGuildKey = getKeyByGuildId(guildId) ?: return@withRetry

            client.collections.premiumKeys.updateOne(
                Document("key", currentGuildKey.key),
                set("usedBy", null)
            )
        }
    }

    suspend fun addGuildKey(userId: String, guildId: String) {
        return client.withRetry {
            val userKey = client.payment.getKeyByUserId(userId) ?: return@withRetry

            client.collections.premiumKeys.updateOne(
                Document("key", userKey.key),
                set("usedBy", guildId)
            )
        }
    }

    suspend fun addTempBanToGuild(guildId: String, tempBan: TempBan) {
        return client.withRetry {
            client.collections.guilds.updateOne(
                eq("_id", guildId),
                push("tempBans", client.encodeToDocument(tempBan))
            )
            invalidateCache(guildId)
        }
    }

    suspend fun removeTempBanFromGuild(guildId: String, userId: String) {
        return client.withRetry {
            client.collections.guilds.updateOne(
                eq("_id", guildId),
                pull("tempBans", eq("userId", userId))
            )
            invalidateCache(guildId)
        }
    }

    suspend fun getKeyByGuildId(guildId: String): Key? {
        return client.withRetry {
            client.collections.premiumKeys.findOne(Document("usedBy", guildId))
        }
    }

    /**
     * Fetches a guild, creating it if it doesn't exist yet.
     * Always goes through the cache.
     */
    suspend fun getGuild(guildId: String): Guild {
        return client.withRetry {
            guildCache.getIfPresent(guildId)?.let { return@withRetry it }

            val existingGuild = client.collections.guilds.findOne(Document("_id", guildId))
                ?: return@withRetry createGuild(guildId)

            updateCache(guildId, existingGuild)
            existingGuild
        }
    }

    suspend fun getGuildsLeftMoreThan14Days(): List<Guild> {
        return client.withRetry {
            val fourteenDaysAgo = Date.from(
                (Clock.System.now() - 14.days).toJavaInstant()
            )

            val query = Document("leftAt", Document("\$lt", fourteenDaysAgo))

            client.collections.guilds.findMany(query)
        }
    }

    /**
     * Same semantics as [getGuild] but returns null instead of creating
     * a new guild when none exists. Shares the same cache so the two
     * methods never disagree about a guild's state.
     */
    suspend fun getGuildOrNull(guildId: String): Guild? {
        return client.withRetry {
            guildCache.getIfPresent(guildId)?.let { return@withRetry it }

            val guild = client.collections.guilds.findOne(Document("_id", guildId)) ?: return@withRetry null

            updateCache(guildId, guild)
            guild
        }
    }

    suspend fun getGuildsByFollowedYouTubeChannel(channelId: String): List<Guild> {
        return client.withRetry {
            client.collections.guilds.findMany(Document("followedYouTubeChannels.channelId", channelId))
        }
    }

    /**
     * Applies [block] as an atomic findOneAndUpdate, returning the
     * post-update document directly from Mongo instead of doing a
     * separate updateOne + find round trip. The cache is always
     * refreshed with the fresh value.
     */
    suspend fun updateGuild(guildId: String, block: GuildBuilder.() -> Unit) {
        return client.withRetry {
            val builder = GuildBuilder().apply(block)
            val update = builder.toDocument()
            if (update.isEmpty()) return@withRetry

            val updatedGuild = client.collections.guilds.findOneAndUpdate(
                Document("_id", guildId),
                update,
                returnDocument = ReturnDocument.AFTER
            ) ?: return@withRetry

            updateCache(guildId, updatedGuild)
        }
    }

    suspend fun deleteGuild(guildId: String) {
        client.withRetry {
            client.collections.guilds.deleteOne(eq("_id", guildId))
            invalidateCache(guildId)
        }
    }

    suspend fun getCaseById(guildId: String, caseId: Long): Case? {
        return client.withRetry {
            client.collections.cases.findOne(
                Document()
                    .append("guildId", guildId)
                    .append("caseId", caseId)
            )
        }
    }

    /**
     * Register a punishment as a [Case] and stores the id
     * @param guildId
     * @param punishedMembers
     * @param staff
     * @param type The punishment [CaseType]
     * @param reason
     * @param duration The punishment duration using [Instant]
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

            client.collections.cases.insertOne(case)

            case
        }
    }

    suspend fun getCaseByUserId(guildId: String, userId: String): List<Case> {
        return client.withRetry {
            client.collections.cases.findMany(
                Document()
                    .append("guildId", guildId)
                    .append("members", userId)
            )
        }
    }

    /**
     * NOTE: this increments `registeredCases` directly on Mongo, bypassing
     * the Guild cache. It also invalidates the cache so a subsequent
     * getGuild() doesn't return a stale registeredCases count.
     */
    private suspend fun getNextCaseId(guildId: String): Long {
        return client.withRetry {
            val result = client.collections.guilds.findOneAndUpdate(
                Document("_id", guildId),
                Document("\$inc", Document("registeredCases", 1)),
                returnDocument = ReturnDocument.AFTER
            )

            invalidateCache(guildId)
            result?.registeredCases ?: 0
        }
    }

    suspend fun addMetricToGuild(guildId: String, block: MetricBuilder.() -> Unit) {
        val incFields = MetricBuilder().apply(block).toDocument("guildAnalytics")

        if (incFields.isEmpty()) return

        client.withRetry {
            client.collections.guilds.updateOne(
                Filters.eq("_id", guildId),
                Document("\$inc", Document(incFields))
            )
            invalidateCache(guildId)
        }
    }

    private suspend fun createGuild(guildId: String): Guild {
        return client.withRetry {
            val newGuild = Guild(
                _id = guildId,
                guildAddedAt = System.currentTimeMillis(),
                GuildJoinLeaveModule = WelcomerModule(),
                antiRaidModule = AntiRaidModule(),
                AutoRoleModule = AutoRoleModule(),
                guildSettings = GuildSettings(),
                musicSettings = MusicSettings(),
                serverLogModule = ServerLogModule(),
                moderationUtils = ModerationUtils(),
                guildAnalytics = Metrics(),
                strictMode = StrictMode()
            )

            client.collections.guilds.insertOne(newGuild)

            updateCache(guildId, newGuild)
            newGuild
        }
    }
}
