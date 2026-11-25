package net.cakeyfox.foxy.database.core

import com.mongodb.MongoCommandException
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.bson.Document

/**
 * Indexes backing the queries issued by this library.
 *
 * Every spec mirrors a real filter/sort used by a repository. Without them MongoDB falls back to
 * collection scans, which is what made the leaderboard rank `count(...)` and the paginated
 * `find(...).sort(...)` grow linearly with the users collection.
 *
 * Creation is idempotent and key-based: before creating anything the existing indexes are listed
 * and a spec is skipped when its key pattern is already present, whatever name that index has.
 * MongoDB treats "same keys, different name" as a conflict (error 85), and databases created
 * before this class existed commonly already have default-named indexes such as
 * `secondUser.id_1`, so matching by name alone would fail on every boot.
 *
 * Best-effort by design — a deployment whose database user lacks `createIndexes` still works,
 * just without the speedup.
 */
internal object DatabaseIndexes {
    private val logger = KotlinLogging.logger {}

    private data class IndexSpec(
        val collection: String,
        val name: String,
        val keys: Document,
        val unique: Boolean = false
    )

    private const val INDEX_OPTIONS_CONFLICT = 85
    private const val INDEX_KEY_SPECS_CONFLICT = 86

    private val specs = listOf(
        // users: rank count + sorted leaderboard pagination. A descending index serves both the
        // `{balance: {$gt: x}}` count and the `sort({balance: -1})` page.
        IndexSpec("users", "users_balance_desc", Document("userCakes.balance", -1)),
        IndexSpec("users", "users_vote_count_desc", Document("voteCount", -1)),
        // Daily/vote reminder scans.
        IndexSpec("users", "users_last_daily", Document("userCakes.lastDaily", 1)),
        IndexSpec("users", "users_vote_reminder", Document("notifiedForVote", 1).append("lastVote", 1)),

        // marriages: every read filters by either partner through an $or, which becomes an
        // index union when both fields are indexed. Existing databases may already carry these
        // under the default `firstUser.id_1` / `secondUser.id_1` names.
        IndexSpec("marriages", "marriages_first_user", Document("firstUser.id", 1)),
        IndexSpec("marriages", "marriages_second_user", Document("secondUser.id", 1)),

        // keys
        IndexSpec("keys", "keys_key", Document("key", 1)),
        IndexSpec("keys", "keys_owned_by", Document("ownedBy", 1)),
        IndexSpec("keys", "keys_used_by", Document("usedBy", 1)),

        // commands: registered once per name at startup, then incremented on every execution.
        IndexSpec("commands", "commands_name", Document("name", 1)),

        // cases
        IndexSpec("cases", "cases_guild_case", Document("guildId", 1).append("caseId", 1)),
        IndexSpec("cases", "cases_guild_members", Document("guildId", 1).append("members", 1)),

        // guilds
        IndexSpec("guilds", "guilds_left_at", Document("leftAt", 1)),
        IndexSpec("guilds", "guilds_youtube_channel", Document("followedYouTubeChannels.channelId", 1)),
        IndexSpec("guild_errors", "guild_errors_guild_id", Document("guildId", 1)),

        // checkout / store
        IndexSpec("checkoutlists", "checkouts_user_approved", Document("userId", 1).append("isApproved", 1)),
        IndexSpec("checkoutlists", "checkouts_id_approved", Document("checkoutId", 1).append("isApproved", 1)),
        IndexSpec("storeitems", "store_items_item_id", Document("itemId", 1)),

        // Profile catalogues: fetched by id on every profile render, and filtered by `inactive`
        // when the shop lists them.
        IndexSpec("backgrounds", "backgrounds_id", Document("id", 1)),
        IndexSpec("backgrounds", "backgrounds_inactive", Document("inactive", 1)),
        IndexSpec("layouts", "layouts_id", Document("id", 1)),
        IndexSpec("layouts", "layouts_inactive", Document("inactive", 1)),
        IndexSpec("decorations", "decorations_id", Document("id", 1)),
        IndexSpec("decorations", "decorations_inactive", Document("inactive", 1)),
        IndexSpec("dailystores", "daily_stores_id", Document("id", 1)),

        // youtube webhooks
        IndexSpec("youtubeWebhooks", "youtube_webhooks_channel_id", Document("channelId", 1))
    )

    suspend fun ensure(database: MongoDatabase) {
        val existingKeys = mutableMapOf<String, MutableSet<String>>()

        specs.map { it.collection }.distinct().forEach { collectionName ->
            existingKeys[collectionName] = readExistingKeyPatterns(database, collectionName)
        }

        var created = 0
        var skipped = 0
        var failed = 0

        for (spec in specs) {
            val signature = spec.keys.toJson()
            val collectionKeys = existingKeys.getOrPut(spec.collection) { mutableSetOf() }

            if (signature in collectionKeys) {
                skipped++
                continue
            }

            try {
                database.getCollection<Document>(spec.collection).createIndex(
                    spec.keys,
                    IndexOptions().name(spec.name).unique(spec.unique)
                )

                collectionKeys.add(signature)
                created++
            } catch (e: MongoCommandException) {
                if (e.errorCode == INDEX_OPTIONS_CONFLICT || e.errorCode == INDEX_KEY_SPECS_CONFLICT) {
                    // Race with another cluster creating the same index, or a pre-existing index
                    // with the same keys and different options. Either way the index is there.
                    collectionKeys.add(signature)
                    skipped++
                } else {
                    failed++
                    logger.warn(e) { "Could not create index ${spec.name} on ${spec.collection}" }
                }
            } catch (e: Exception) {
                failed++
                logger.warn(e) { "Could not create index ${spec.name} on ${spec.collection}" }
            }
        }

        logger.info { "Database indexes: $created created, $skipped already present, $failed failed" }
    }

    private suspend fun readExistingKeyPatterns(
        database: MongoDatabase,
        collectionName: String
    ): MutableSet<String> {
        return try {
            database.getCollection<Document>(collectionName)
                .listIndexes()
                .toList()
                .mapNotNull { (it["key"] as? Document)?.toJson() }
                .toMutableSet()
        } catch (e: Exception) {
            // A missing collection has no indexes yet; that is not an error worth reporting.
            logger.debug(e) { "Could not list indexes for $collectionName" }
            mutableSetOf()
        }
    }
}
