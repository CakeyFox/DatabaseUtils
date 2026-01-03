package net.cakeyfox.foxy.database.core.utils


import FoxyUserBuilder
import com.mongodb.client.model.Aggregates.skip
import com.mongodb.client.model.Filters.and
import org.bson.Document
import kotlinx.coroutines.flow.firstOrNull
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.Indexes.descending
import com.mongodb.client.model.Projections.include
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class UserUtils(private val client: DatabaseClient) {
    suspend fun getUserByPremiumKey(key: String): FoxyUser? {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("keys")
            val userCollection = client.database.getCollection<Document>("users")
            val keyInfo = collection.find(eq("key", key)).firstOrNull()
                ?: return@withRetry null
            val keyToJSON = keyInfo.toJson()
            val keyData = client.json.decodeFromString<Key>(keyToJSON)
            val user = userCollection.find(eq("_id", keyData.ownedBy)).firstOrNull()
                ?: return@withRetry null
            val documentToJSON = user.toJson()

            client.json.decodeFromString<FoxyUser>(documentToJSON)
        }
    }

    suspend fun getFoxyProfile(userId: String): FoxyUser {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("users")

            val existingUserDocument = collection.find(eq("_id", userId)).firstOrNull()
                ?: return@withRetry createUser(userId)

            val documentToJSON = existingUserDocument.toJson()
            client.json.decodeFromString<FoxyUser>(documentToJSON)
        }
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
        }
    }


    suspend fun addVote(userId: String) {
        val userData = getFoxyProfile(userId)
        client.withRetry {
            updateUser(userId) {
                lastVote = Clock.System.now()
                voteCount = (userData.voteCount ?: 0) + 1
                notifiedForVote = false
                userCakes.balance = userData.userCakes.balance + 1500
            }
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
        }
    }

    suspend fun updateUsers(users: List<FoxyUser>, updates: Map<String, Any?>) {
        client.withRetry {
            val query = Document("_id", Document("\$in", users.map { it._id }))
            val update = Document("\$set", Document(updates))

            client.users.updateMany(query, update)
        }
    }

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
        }
    }

    suspend fun removeCakesFromUser(userId: String, amount: Long) {
        client.withRetry {
            val query = Document("_id", userId)
            val update = Document("\$inc", Document("userCakes.balance", -amount.toDouble()))

            client.users.updateOne(query, update)
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
                eq("firstUserId", userId),
                eq("secondUserId", userId)
            )

            val update = builder.toDocument()

            marriages.findOneAndUpdate(
                filter,
                update,
                FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER)
            )
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
                firstUserId = requesterId,
                marriageName = marriageName,
                secondUserId = userId,
                firstUserLetters = 0,
                secondUserLetters = 0,
                affinityPoints = 0
            )

            val documentToJSON = client.json.encodeToString(newMarriage)
            val document = Document.parse(documentToJSON)
            collection.insertOne(document)

            newMarriage
        }
    }

    suspend fun deleteMarriage(userId: String) {
        client.withRetry {
            val collection = client.database.getCollection<Document>("marriages")

            val filter = or(
                eq("firstUserId", userId),
                eq("secondUserId", userId)
            )

            collection.findOneAndDelete(filter)

        }
    }

    suspend fun getMarriage(userId: String): Marry? =
        client.withRetry {
            val marriages = client.database.getCollection<Marry>("marriages")

            marriages.find(
                or(
                    eq("firstUserId", userId),
                    eq("secondUserId", userId)
                )
            ).firstOrNull()
        }

    private suspend fun createUser(userId: String): FoxyUser {
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
                petInfo = PetInfo(),
                userTransactions = emptyList(),
                roulette = Roulette(),
            )

            val documentToJSON = client.json.encodeToString(newUser)
            val document = Document.parse(documentToJSON)
            document["userCreationTimestamp"] = Date.from(newUser.userCreationTimestamp!!.toJavaInstant())

            collection.insertOne(document)

            newUser
        }
    }
}