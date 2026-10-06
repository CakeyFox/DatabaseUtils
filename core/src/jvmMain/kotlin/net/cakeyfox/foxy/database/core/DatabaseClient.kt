package net.cakeyfox.foxy.database.core

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.MongoSocketException
import com.mongodb.MongoTimeoutException
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.cakeyfox.foxy.database.common.data.guild.GuildErrors
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

    @Deprecated(
        "Typed collections cannot decode MongoDateSerializer fields. Use collections.guilds instead.",
        ReplaceWith("collections.guilds"),
    )
    lateinit var guilds: MongoCollection<Guild>

    @Deprecated(
        "Typed collections cannot decode MongoDateSerializer fields. Use collections.users instead.",
        ReplaceWith("collections.users"),
    )
    lateinit var users: MongoCollection<FoxyUser>

    @Deprecated(
        "Typed collections cannot decode MongoDateSerializer fields. Use collections.foxyverseGuilds instead.",
        ReplaceWith("collections.foxyverseGuilds"),
    )
    lateinit var foxyverseGuilds: MongoCollection<FoxyverseGuild>

    @Deprecated(
        "Typed collections cannot decode MongoDateSerializer fields. Use collections.youtubeWebhooks instead.",
        ReplaceWith("collections.youtubeWebhooks"),
    )
    lateinit var youtubeWebhooks: MongoCollection<YouTubeWebhook>

    @Deprecated(
        "Typed collections cannot decode MongoDateSerializer fields. Use collections.premiumKeys instead.",
        ReplaceWith("collections.premiumKeys"),
    )
    lateinit var premiumKeys: MongoCollection<Key>

    @Deprecated(
        "Typed collections cannot decode MongoDateSerializer fields. Use collections.guildErrors instead.",
        ReplaceWith("collections.guildErrors"),
    )
    lateinit var guildErrors: MongoCollection<GuildErrors>

    private var databaseUser: String = ""
    private var pwd: String = ""
    private var address: String = ""
    private var protocol: String? = "mongodb://"
    private var authSource: String? = null
    private var readTimeoutMs: Long? = null
    var databaseName: String = ""

    private val coroutineExecutor = ThreadUtils.createThreadPool("DatabaseExecutor [%d]")
    private val coroutineDispatcher = coroutineExecutor.asCoroutineDispatcher()

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val collections: Collections get() = Collections(this)

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

        val connectionString = "$protocol$databaseUser:$pwd@$address/$databaseName"
        logger.info { "Connecting to MongoDB at $address" }

        client = MongoClient.create(connectionString)
        database = client.getDatabase(databaseName)

        @Suppress("DEPRECATION")
        run {
            guildErrors = database.getCollection("guild_errors")
            users = database.getCollection("users")
            guilds = database.getCollection("guilds")
            youtubeWebhooks = database.getCollection("youtubeWebhooks")
            premiumKeys = database.getCollection("keys")
            foxyverseGuilds = database.getCollection("foxyverses")
        }

        logger.info { "Connected to MongoDB database '$databaseName' successfully." }
    }

    private fun reconnect() {
        logger.warn { "Lost connection! Reconnecting..." }
        try {
            if (::client.isInitialized) close()
            connect()
        } catch (e: Exception) {
            logger.error(e) { "Error while reconnecting to database" }
        }
    }

    fun close() {
        if (!::client.isInitialized) return
        try {
            client.close()
            logger.info { "Database closed successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing database" }
        }
    }

    /**
     * Only transient driver failures are retried. Retrying on every [Exception] meant validation
     * errors, serialization failures and even `require(...)` calls triggered a close+reconnect and
     * were replayed up to 3 times, masking the real cause.
     */
    private suspend fun <T> withDatabaseRetry(
        retries: Int = 3,
        block: suspend MongoDatabase.() -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return database.block()
            } catch (e: Exception) {
                if (!isTransient(e) || ++attempt >= retries) throw e
                logger.warn(e) { "Transient database failure (attempt $attempt/$retries), reconnecting..." }
                delay(50L * attempt)
                reconnect()
            }
        }
    }

    private fun isTransient(error: Throwable): Boolean =
        error is MongoSocketException || error is MongoTimeoutException

    suspend fun <T> withRetry(block: suspend MongoDatabase.() -> T): T {
        return withContext(coroutineDispatcher) {
            this@DatabaseClient.withDatabaseRetry(block = block)
        }
    }

    fun setProtocol(connectionProtocol: String? = "mongodb://"): DatabaseClient {
        protocol = connectionProtocol
        return this
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

    fun setAuthSource(source: String): DatabaseClient {
        authSource = source
        return this
    }

    fun setTimeout(timeout: Long, unit: TimeUnit): DatabaseClient {
        readTimeoutMs = unit.toMillis(timeout)
        return this
    }
}
