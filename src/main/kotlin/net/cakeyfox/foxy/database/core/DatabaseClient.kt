package net.cakeyfox.foxy.database.core

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.cakeyfox.foxy.database.core.utils.*
import net.cakeyfox.foxy.database.data.bot.YouTubeWebhook
import net.cakeyfox.foxy.database.data.guild.FoxyverseGuild
import net.cakeyfox.foxy.database.data.guild.Guild
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.user.FoxyUser
import net.cakeyfox.foxy.database.utils.ThreadUtils
import java.util.concurrent.TimeUnit

class DatabaseClient() {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    lateinit var client: MongoClient
    lateinit var database: MongoDatabase

    lateinit var guilds: MongoCollection<Guild>
    lateinit var users: MongoCollection<FoxyUser>
    lateinit var foxyverseGuilds: MongoCollection<FoxyverseGuild>
    lateinit var youtubeWebhooks: MongoCollection<YouTubeWebhook>
    lateinit var premiumKeys: MongoCollection<Key>

    private var databaseUser: String = ""
    private var pwd: String = ""
    private var address: String = ""
    var databaseName: String = ""

    private val coroutineExecutor = ThreadUtils.createThreadPool("DatabaseExecutor [%d]")
    private val coroutineDispatcher = coroutineExecutor.asCoroutineDispatcher()

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val profile = ProfileUtils(this)
    val guild = GuildUtils(this)
    val user = UserUtils(this)
    val bot = BotUtils(this)
    val youtube = YouTubeUtils(this)
    val payment = PaymentUtils(this)

    fun connect() {
        require(databaseUser.isNotBlank() && pwd.isNotBlank() && address.isNotBlank() && databaseName.isNotBlank()) {
            "DatabaseClient: user, password, address e database are required."
        }

        val connectionString = "mongodb://$databaseUser:$pwd@$address/$databaseName"
        logger.info { "Connecting to MongoDB at $address" }

        client = MongoClient.create(connectionString)
        database = client.getDatabase(databaseName)

        users = database.getCollection("users")
        guilds = database.getCollection("guilds")
        youtubeWebhooks = database.getCollection("youtubeWebhooks")
        premiumKeys = database.getCollection("keys")
        foxyverseGuilds = database.getCollection("foxyverses")

        logger.info { "Connected to MongoDB database '$databaseName' successfully." }
    }

    private fun reconnect() {
        logger.warn { "Lost connection! Reconnecting..." }
        try {
            close()
            connect()
        } catch (e: Exception) {
            logger.error(e) { "Error while reconnecting to database" }
        }
    }

    fun close() {
        try {
            client.close()
            logger.info { "Database closed successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing database" }
        }
    }

    private suspend fun <T> withDatabaseRetry(
        retries: Int = 3,
        block: suspend MongoDatabase.() -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return database.block()
            } catch (e: Exception) {
                logger.warn(e) { "Database operation failed (attempt ${attempt + 1}/$retries)" }
                if (++attempt >= retries) throw e
                reconnect()
            }
        }
    }

    suspend fun <T> withRetry(block: suspend MongoDatabase.() -> T): T {
        return withContext(coroutineDispatcher) {
            this@DatabaseClient.withDatabaseRetry(block = block)
        }
    }

    fun setUser(user: String): DatabaseClient {
        databaseUser = user
        return this
    }

    fun setPassword(passwd: String): DatabaseClient {
        pwd = passwd
        return this
    }

    fun setAddress(address: String): DatabaseClient {
        this.address = address
        return this
    }

    fun setDatabase(databaseName: String): DatabaseClient {
        this.databaseName = databaseName
        return this
    }

    fun setTimeout(timeout: Long, unit: TimeUnit): DatabaseClient {
        try {
            client.withTimeout(timeout, unit)
            return this
        } catch (e: Exception) {
            logger.warn(e) { "Failed to set client timeout" }
            return this
        }
    }
}