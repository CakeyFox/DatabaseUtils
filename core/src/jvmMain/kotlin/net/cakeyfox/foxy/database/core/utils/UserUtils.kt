package net.cakeyfox.foxy.database.core.utils

import com.github.benmanes.caffeine.cache.Caffeine
import net.cakeyfox.foxy.database.utils.builders.FoxyUserBuilder
import com.mongodb.client.model.Filters.and
import org.bson.Document
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.Projections
import com.mongodb.client.model.ReturnDocument
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import net.cakeyfox.foxy.database.common.data.marry.CoupleStoreItem
import net.cakeyfox.foxy.database.common.data.marry.Marry
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.core.encodeToDocument
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.user.FoxyUser
import net.cakeyfox.foxy.database.data.user.MarryStatus
import net.cakeyfox.foxy.database.data.user.Reputation
import net.cakeyfox.foxy.database.data.user.Roulette
import net.cakeyfox.foxy.database.data.user.UserBirthday
import net.cakeyfox.foxy.database.data.user.UserCakes
import net.cakeyfox.foxy.database.data.user.UserPremium
import net.cakeyfox.foxy.database.data.user.UserProfile
import net.cakeyfox.foxy.database.data.user.UserSettings
import net.cakeyfox.foxy.database.utils.builders.MarryBuilder
import org.bson.conversions.Bson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

class UserUtils(val client: DatabaseClient) {
    @PublishedApi
    internal val userCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, FoxyUser>()

    /** Marriages are looked up by either partner's id, so a single
     *  Marry is cached under both `firstUser.id` and `secondUser.id`. */
    @PublishedApi
    internal val marriageCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Marry>()

    @PublishedApi
    internal fun updateUserCache(userId: String, user: FoxyUser) {
        userCache.put(userId, user)
    }

    @PublishedApi
    internal fun invalidateUserCache(userId: String) {
        userCache.invalidate(userId)
    }

    internal fun updateMarriageCache(marry: Marry) {
        marriageCache.put(marry.firstUser.id, marry)
        marriageCache.put(marry.secondUser.id, marry)
    }

    internal fun invalidateMarriageCache(marry: Marry?) {
        if (marry == null) return
        marriageCache.invalidate(marry.firstUser.id)
        marriageCache.invalidate(marry.secondUser.id)
    }

    suspend fun getUserByPremiumKey(key: String): FoxyUser? {
        return client.withRetry {
            val keyData = client.collections.premiumKeys.findOne(eq("key", key))
                ?: return@withRetry null

            userCache.getIfPresent(keyData.ownedBy!!)?.let { return@withRetry it }

            val decoded = client.collections.users.findById(keyData.ownedBy)
                ?: return@withRetry null

            updateUserCache(keyData.ownedBy, decoded)
            decoded
        }
    }

    /**
     * Fetches a user's profile, optionally projecting to specific [fields].
     *
     * Caching only applies to *full-profile* requests (no [fields] passed):
     * that's the only case where we have a complete document to safely
     * cache. Narrow field projections still hit Mongo directly with a
     * projection — caching those would mean either serving stale partial
     * data from unrelated reads, or discarding the DB-side projection and
     * always pulling the full document, which defeats its purpose.
     */
    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T> getFoxyProfile(
        userId: String,
        vararg fields: String
    ): T {
        if (fields.isEmpty()) {
            userCache.getIfPresent(userId)?.let { cached ->
                return client.json.decodeFromString(client.json.encodeToString(cached))
            }
        }

        return client.withRetry {
            val projection = if (fields.isNotEmpty())
                Projections.fields(fields.map { Projections.include(it) })
            else null

            val document = client.collections.users.findDocument(Document("_id", userId), projection)

            val json = document?.toJson() ?: client.json.encodeToString(createUser(userId))

            if (fields.isEmpty()) {
                val fullUser = client.json.decodeFromString<FoxyUser>(json)
                updateUserCache(userId, fullUser)
            }

            if (fields.size == 1) {
                val element = fields[0].split(".").fold(
                    client.json.parseToJsonElement(json) as JsonElement?
                ) { acc, key -> (acc as? JsonObject)?.get(key) }

                if (element == null || element is JsonNull) {
                    if (typeOf<T>().isMarkedNullable) return@withRetry null as T
                    throw NoSuchElementException("Field '${fields[0]}' is missing for user $userId")
                }

                return@withRetry client.json.decodeFromJsonElement(serializer<T>(), element)
            }

            client.json.decodeFromString<T>(json)
        }
    }

    fun isPrimitive(klass: KClass<*>): Boolean {
        return klass in setOf(
            Int::class, Long::class, Double::class, Float::class,
            Boolean::class, String::class
        )
    }

    suspend fun getExpiredDailies(): List<FoxyUser> {
        return client.withRetry {
            val expirationTime = Instant.now().minus(24, ChronoUnit.HOURS)

            client.collections.users.findMany(
                and(
                    exists("userCakes.lastDaily", true),
                    lt("userCakes.lastDaily", expirationTime)
                )
            )
        }
    }

    suspend fun getExpiredVotes(): List<FoxyUser> {
        return client.withRetry {
            val expirationTime = Instant.now().minus(12, ChronoUnit.HOURS)

            client.collections.users.findMany(
                and(
                    lt("lastVote", expirationTime),
                    eq("notifiedForVote", false)
                )
            )
        }
    }

    suspend fun banUserById(userId: String, reason: String) {
        updateUser(userId) {
            isBanned = true
            banDate = Clock.System.now()
            banReason = reason
        }
    }

    suspend fun updateUser(userId: String, block: FoxyUserBuilder.() -> Unit) {
        val update = FoxyUserBuilder().apply(block).toDocument()
        if (update.isEmpty()) return

        client.withRetry {
            client.collections.users.updateOne(Document("_id", userId), update)
            invalidateUserCache(userId)
        }
    }

    /**
     * Rewritten to increment `voteCount` and `userCakes.balance` atomically
     * via `$inc` instead of the previous read-then-write pattern (read
     * voteCount/balance, add in application code, write back). That pattern
     * is a lost-update race condition even without a cache: two concurrent
     * votes from the same user (or a vote landing between two getFoxyProfile
     * reads) can silently drop one of the increments. `$inc` is atomic at
     * the database level regardless of caching.
     */
    suspend fun addVote(userId: String) {
        client.withRetry {
            if (client.collections.users.findById(userId) == null) {
                createUser(userId)
            }

            val update = Document(
                "\$set", Document(
                    mapOf(
                        "lastVote" to Date.from(Clock.System.now().toJavaInstant()),
                        "notifiedForVote" to false
                    )
                )
            ).apply {
                put(
                    "\$inc", Document(
                        mapOf(
                            "voteCount" to 1,
                            "userCakes.balance" to 1500.0
                        )
                    )
                )
            }

            client.collections.users.updateOne(Document("_id", userId), update)
            invalidateUserCache(userId)
        }
    }

    /**
     * @param sender the user giving the reputation. Defaults to [userId] to preserve the previous
     * behavior, but callers should pass the actual giver.
     */
    suspend fun addReputation(userId: String, reason: String, sender: String = userId) {
        client.withRetry {
            if (client.collections.users.findById(userId) == null) createUser(userId)

            val reputation = Reputation(
                sender = sender,
                reason = reason,
                date = Clock.System.now()
            )

            client.collections.users.updateOne(
                Document("_id", userId),
                Document("\$push", Document("userProfile.reputations", client.encodeToDocument(reputation))),
                upsert = true
            )
            invalidateUserCache(userId)
        }
    }

    suspend fun updateUsers(users: List<FoxyUser>, updates: Map<String, Any?>) {
        client.withRetry {
            val query = Document("_id", Document("\$in", users.map { it._id }))
            val update = Document("\$set", Document(updates))

            client.collections.users.updateMany(query, update)
            users.forEach { invalidateUserCache(it._id) }
        }
    }

    /**
     * Not cached: this needs a live count of users with a strictly higher
     * balance than the target user, which changes any time *any* user's
     * balance changes. Caching it per-user would require invalidating on
     * every other user's cake gain/loss, which isn't worth it for a value
     * that's cheap to compute on demand.
     */
    suspend fun getUserRankPosition(userId: String): Int {
        val userCakes = getFoxyProfile<Double?>(userId, "userCakes.balance") ?: return -1

        val countAbove = client.collections.users.count(
            Document("userCakes.balance", Document("\$gt", userCakes))
        )

        return (countAbove + 1).toInt()
    }

    // Not cached, for the same reason as getUserRankPosition: it's a live
    // aggregate/sorted view over the whole collection, not a per-user read.
    suspend fun getCakesLeaderboardPage(page: Int, pageSize: Int? = 10): List<FoxyUser> {
        val skip = (page - 1) * pageSize!!

        return client.collections.users.findMany(
            filter = Document("userCakes.balance", Document("\$gt", 0)),
            sort = Document("userCakes.balance", -1),
            skip = skip,
            limit = pageSize
        )
    }

    suspend fun addCakesToUser(userId: String, amount: Long) {
        client.withRetry {
            client.collections.users.updateOne(
                Document("_id", userId),
                Document("\$inc", Document("userCakes.balance", amount.toDouble()))
            )
            invalidateUserCache(userId)
        }
    }

    suspend fun removeCakesFromUser(userId: String, amount: Long) {
        client.withRetry {
            client.collections.users.updateOne(
                Document("_id", userId),
                Document("\$inc", Document("userCakes.balance", -amount.toDouble()))
            )
            invalidateUserCache(userId)
        }
    }

    suspend fun getItemFromCoupleShop(itemId: String): CoupleStoreItem? {
        return client.withRetry {
            client.collections.coupleShop.findById(itemId)
        }
    }

    suspend fun updateMarriage(
        userId: String,
        block: MarryBuilder.() -> Unit
    ): Marry? {
        val update = MarryBuilder().apply(block).toDocument()
        if (update.isEmpty()) return null

        return client.withRetry {
            val filter = or(
                eq("firstUser.id", userId),
                eq("secondUser.id", userId)
            )

            val result = client.collections.marriages.findOneAndUpdate(
                filter,
                update,
                returnDocument = ReturnDocument.AFTER
            )

            result?.let { updateMarriageCache(it) }
            result
        }
    }

    suspend fun createMarriage(requesterId: String, userId: String, marriageName: String): Marry {
        return client.withRetry {
            val uuidv4 = UUID.randomUUID()
            val date = ZonedDateTime.now(ZoneId.systemDefault()).toInstant()

            val newMarriage = Marry(
                marryId = uuidv4.toString(),
                marriedDate = date.toKotlinInstant(),
                firstUser = Marry.User(
                    id = requesterId,
                    letterCount = 0,
                    lastLetter = null
                ),
                secondUser = Marry.User(
                    id = userId,
                    letterCount = 0,
                    lastLetter = null
                ),
                affinityPoints = 0
            )

            client.collections.marriages.insertOne(newMarriage)

            updateMarriageCache(newMarriage)
            newMarriage
        }
    }

    suspend fun deleteUser(userId: String) {
        client.withRetry {
            client.collections.users.deleteOne(eq("_id", userId))
            client.collections.premiumKeys.deleteOne(eq("ownedBy", userId))
            client.collections.marriages.deleteMany(or(eq("firstUser.id", userId), eq("secondUser.id", userId)))
            client.collections.checkouts.deleteMany(eq("userId", userId))
            invalidateUserCache(userId)
        }
    }

    suspend fun deleteMarriage(userId: String) {
        client.withRetry {
            val filter = or(
                eq("firstUser.id", userId),
                eq("secondUser.id", userId)
            )

            val deleted = client.collections.marriages.findOneAndDelete(filter)
            invalidateMarriageCache(deleted)
        }
    }

    suspend fun getMarriage(userId: String): Marry? {
        marriageCache.getIfPresent(userId)?.let { return it }

        return client.withRetry {
            val marry = client.collections.marriages.findOne(
                or(
                    eq("firstUser.id", userId),
                    eq("secondUser.id", userId)
                )
            )

            marry?.let { updateMarriageCache(it) }
            marry
        }
    }

    suspend fun createUser(userId: String): FoxyUser {
        return client.withRetry {
            val newUser = FoxyUser(
                _id = userId,
                userCakes = UserCakes(balance = 0.0),
                marryStatus = MarryStatus(),
                userProfile = UserProfile(),
                userBirthday = UserBirthday(),
                userPremium = UserPremium(),
                userSettings = UserSettings(language = "pt-br"),
                userTransactions = emptyList(),
                roulette = Roulette(),
            )

            val document = client.collections.users.encode(newUser)
            document["userCreationTimestamp"] = Date.from(newUser.userCreationTimestamp!!.toJavaInstant())

            client.collections.users.insertOne(document)

            updateUserCache(userId, newUser)
            newUser
        }
    }
}
