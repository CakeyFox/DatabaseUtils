package net.cakeyfox.foxy.database.core.utils

import com.github.benmanes.caffeine.cache.Caffeine
import net.cakeyfox.foxy.database.utils.builders.FoxyUserBuilder
import com.mongodb.client.model.Filters.and
import org.bson.Document
import kotlinx.coroutines.flow.firstOrNull
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.Projections
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import net.cakeyfox.foxy.database.common.data.marry.CoupleStoreItem
import net.cakeyfox.foxy.database.common.data.marry.Marry
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.user.FoxyUser
import net.cakeyfox.foxy.database.data.user.MarryStatus
import net.cakeyfox.foxy.database.data.user.PetInfo
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

class UserUtils(val client: DatabaseClient) {
    @PublishedApi
    internal val userCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, FoxyUser>()

    /** Marriages are looked up by either partner's id, so a single
     *  Marry is cached under both `firstUser.id` and `secondUser.id`. */
    @PublishedApi
    internal val marriageCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, Marry>()

    @PublishedApi
    internal fun updateUserCache(userId: String, user: FoxyUser) {
        userCache.put(userId, user)
    }

    @PublishedApi
    internal fun invalidateUserCache(userId: String) {
        userCache.invalidate(userId)
    }

    private fun updateMarriageCache(marry: Marry) {
        marriageCache.put(marry.firstUser.id, marry)
        marriageCache.put(marry.secondUser.id, marry)
    }

    private fun invalidateMarriageCache(marry: Marry?) {
        if (marry == null) return
        marriageCache.invalidate(marry.firstUser.id)
        marriageCache.invalidate(marry.secondUser.id)
    }

    suspend fun getUserByPremiumKey(key: String): FoxyUser? {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("keys")
            val userCollection = client.database.getCollection<Document>("users")
            val keyInfo = collection.find(eq("key", key)).firstOrNull()
                ?: return@withRetry null
            val keyToJSON = keyInfo.toJson()
            val keyData = client.json.decodeFromString<Key>(keyToJSON)

            userCache.getIfPresent(keyData.ownedBy!!)?.let { return@withRetry it }

            val user = userCollection.find(eq("_id", keyData.ownedBy)).firstOrNull()
                ?: return@withRetry null
            val documentToJSON = user.toJson()

            val decoded = client.json.decodeFromString<FoxyUser>(documentToJSON)
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
            val collection = client.database.getCollection<Document>("users")

            val projection = if (fields.isNotEmpty())
                Projections.fields(fields.map { Projections.include(it) })
            else null

            val document = collection
                .find(eq("_id", userId))
                .apply { projection?.let { projection(it) } }
                .firstOrNull()

            val json = document?.toJson() ?: client.json.encodeToString(createUser(userId))

            if (fields.isEmpty()) {
                val fullUser = client.json.decodeFromString<FoxyUser>(json)
                updateUserCache(userId, fullUser)
            }

            if (fields.size == 1 && isPrimitive(T::class)) {
                val element = fields[0].split(".").fold(
                    client.json.parseToJsonElement(json) as JsonElement?
                ) { acc, key ->
                    (acc as? JsonObject)?.get(key)
                } ?: return@withRetry null as T

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
            val now = Instant.now()
            val expirationTime = now.minus(24, ChronoUnit.HOURS)

            val expiredDailies = client.users.find(
                and(
                    exists("userCakes.lastDaily", true),
                    lt("userCakes.lastDaily", expirationTime)
                )
            ).toList()

            expiredDailies
        }
    }

    suspend fun getExpiredVotes(): List<FoxyUser> {
        return client.withRetry {
            val now = Instant.now()
            val expirationTime = now.minus(12, ChronoUnit.HOURS)

            val expiredVotes = client.users.find(
                and(
                    lt("lastVote", expirationTime),
                    eq("notifiedForVote", false)
                )
            ).toList()

            expiredVotes
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
        val builder = FoxyUserBuilder().apply(block)

        client.withRetry {
            val query = Document("_id", userId)
            val update = builder.toDocument()
            client.users.updateOne(query, update)
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
            if (client.users.find(eq("_id", userId)).firstOrNull() == null) {
                createUser(userId)
            }

            val query = Document("_id", userId)
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

            client.users.updateOne(query, update)
            invalidateUserCache(userId)
        }
    }

    suspend fun addReputation(userId: String, reason: String) {
        client.withRetry {
            val collection = client.database.getCollection<Document>("users")

            collection.find(eq("_id", userId)).firstOrNull() ?: createUser(userId)

            val query = Document("_id", userId)
            val update = Reputation(
                sender = userId,
                reason = reason,
                date = Clock.System.now()
            )

            client.users.updateOne(
                query,
                Document(
                    "\$push",
                    Document("userProfile.reputations", update)
                ),
                UpdateOptions().upsert(true)
            )
            invalidateUserCache(userId)
        }
    }

    suspend fun updateUsers(users: List<FoxyUser>, updates: Map<String, Any?>) {
        client.withRetry {
            val query = Document("_id", Document("\$in", users.map { it._id }))
            val update = Document("\$set", Document(updates))

            client.users.updateMany(query, update)
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
        val collection = client.database.getCollection<FoxyUser>("users")

        val userCakes = getFoxyProfile<Double?>(userId, "userCakes.balance") ?: return -1

        val countAbove = collection.countDocuments(
            Document("userCakes.balance", Document("\$gt", userCakes))
        )

        return (countAbove + 1).toInt()
    }

    // Not cached, for the same reason as getUserRankPosition: it's a live
    // aggregate/sorted view over the whole collection, not a per-user read.
    suspend fun getCakesLeaderboardPage(page: Int, pageSize: Int? = 10): List<FoxyUser> {
        val skip = (page - 1) * pageSize!!

        val collection = client.database.getCollection<FoxyUser>("users")

        return collection
            .find(Document("userCakes.balance", Document("\$gt", 0)))
            .sort(Document("userCakes.balance", -1))
            .skip(skip)
            .limit(pageSize)
            .toList()
    }

    suspend fun addCakesToUser(userId: String, amount: Long) {
        client.withRetry {
            val query = Document("_id", userId)
            val update = Document("\$inc", Document("userCakes.balance", amount.toDouble()))

            client.users.updateOne(query, update)
            invalidateUserCache(userId)
        }
    }

    suspend fun removeCakesFromUser(userId: String, amount: Long) {
        client.withRetry {
            val query = Document("_id", userId)
            val update = Document("\$inc", Document("userCakes.balance", -amount.toDouble()))

            client.users.updateOne(query, update)
            invalidateUserCache(userId)
        }
    }

    suspend fun getItemFromCoupleShop(itemId: String): CoupleStoreItem? {
        return client.withRetry {
            val store = client.database.getCollection<CoupleStoreItem>("couple_shop")
            val filter = eq("_id", itemId)

            store.find(filter).firstOrNull()
        }
    }

    suspend fun updateMarriage(
        userId: String,
        block: MarryBuilder.() -> Unit
    ): Marry? {
        val builder = MarryBuilder().apply(block)

        return client.withRetry {
            val marriages = client.database.getCollection<Marry>("marriages")

            val filter = or(
                eq("firstUser.id", userId),
                eq("secondUser.id", userId)
            )

            val update = builder.toDocument()

            val result = marriages.findOneAndUpdate(
                filter,
                update,
                FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER)
            )

            result?.let { updateMarriageCache(it) }
            result
        }
    }

    suspend fun createMarriage(requesterId: String, userId: String, marriageName: String): Marry {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("marriages")
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

            val documentToJSON = client.json.encodeToString(newMarriage)
            val document = Document.parse(documentToJSON)
            collection.insertOne(document)

            updateMarriageCache(newMarriage)
            newMarriage
        }
    }

    suspend fun deleteMarriage(userId: String) {
        client.withRetry {
            val collection = client.database.getCollection<Marry>("marriages")

            val filter = or(
                eq("firstUser.id", userId),
                eq("secondUser.id", userId)
            )

            val deleted = collection.findOneAndDelete(filter)
            invalidateMarriageCache(deleted)
        }
    }

    suspend fun getMarriage(userId: String): Marry? {
        marriageCache.getIfPresent(userId)?.let { return it }

        return client.withRetry {
            val marriages = client.database.getCollection<Marry>("marriages")

            val marry = marriages.find(
                or(
                    eq("firstUser.id", userId),
                    eq("secondUser.id", userId)
                )
            ).firstOrNull()

            marry?.let { updateMarriageCache(it) }
            marry
        }
    }

    suspend fun createUser(userId: String): FoxyUser {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("users")

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

            val documentToJSON = client.json.encodeToString(newUser)
            val document = Document.parse(documentToJSON)
            document["userCreationTimestamp"] = Date.from(newUser.userCreationTimestamp!!.toJavaInstant())

            collection.insertOne(document)

            updateUserCache(userId, newUser)
            newUser
        }
    }
}