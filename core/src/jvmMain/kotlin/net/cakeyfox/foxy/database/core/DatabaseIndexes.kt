package net.cakeyfox.foxy.database.core

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import mu.KotlinLogging
import org.bson.Document
import org.bson.conversions.Bson

/**
 * Indexes backing the queries issued by this library.
 *
 * Every spec mirrors a real filter/sort used by a repository. Without them MongoDB falls back to
 * collection scans, which is what made the leaderboard rank `count(...)` and the paginated
 * `find(...).sort(...)` grow linearly with the users collection.
 *
 * Creation is idempotent: re-creating an index with the same keys and options is a no-op, so this
 * can run on every boot. It is also best-effort — a deployment whose database user lacks the
 * `createIndexes` action still works, just without the speedup.
 */
internal object DatabaseIndexes {
    private val logger = KotlinLogging.logger {}

    private data class IndexSpec(
        val collection: String,
        val name: String,
        val keys: Bson,
        val unique: Boolean = false
    )

    private val specs = listOf(
        // users: rank count + sorted leaderboard pagination. A descending index serves both the
        // `{balance: {$gt: x}}` count and the `sort({balance: -1})` page.
        IndexSpec("users", "users_balance_desc", Indexes.descending("userCakes.balance")),
        IndexSpec("users", "users_vote_count_desc", Indexes.descending("voteCount")),
        // Daily/vote reminder scans.
        IndexSpec("users", "users_last_daily", Indexes.ascending("userCakes.lastDaily")),
        IndexSpec("users", "users_vote_reminder", Indexes.compoundIndex(
            Indexes.ascending("notifiedForVote"),
            Indexes.ascending("lastVote")
        )),

        // marriages: every read filters by either partner through an $or, which becomes an
        // index union when both fields are indexed.
        IndexSpec("marriages", "marriages_first_user", Indexes.ascending("firstUser.id")),
        IndexSpec("marriages", "marriages_second_user", Indexes.ascending("secondUser.id")),

        // keys
        IndexSpec("keys", "keys_key", Indexes.ascending("key")),
        IndexSpec("keys", "keys_owned_by", Indexes.ascending("ownedBy")),
        IndexSpec("keys", "keys_used_by", Indexes.ascending("usedBy")),

        // commands: registered once per name at startup, then incremented on every execution.
        IndexSpec("commands", "commands_name", Indexes.ascending("name")),

        // cases
        IndexSpec("cases", "cases_guild_case", Indexes.compoundIndex(
            Indexes.ascending("guildId"),
            Indexes.ascending("caseId")
        )),
        IndexSpec("cases", "cases_guild_members", Indexes.compoundIndex(
            Indexes.ascending("guildId"),
            Indexes.ascending("members")
        )),

        // guilds
        IndexSpec("guilds", "guilds_left_at", Indexes.ascending("leftAt")),
        IndexSpec("guilds", "guilds_youtube_channel", Indexes.ascending("followedYouTubeChannels.channelId")),
        IndexSpec("guild_errors", "guild_errors_guild_id", Indexes.ascending("guildId")),

        // checkout / store
        IndexSpec("checkoutlists", "checkouts_user_approved", Indexes.compoundIndex(
            Indexes.ascending("userId"),
            Indexes.ascending("isApproved")
        )),
        IndexSpec("checkoutlists", "checkouts_id_approved", Indexes.compoundIndex(
            Indexes.ascending("checkoutId"),
            Indexes.ascending("isApproved")
        )),
        IndexSpec("storeitems", "store_items_item_id", Indexes.ascending("itemId")),

        // Profile catalogues: fetched by id on every profile render, and filtered by `inactive`
        // when the shop lists them.
        IndexSpec("backgrounds", "backgrounds_id", Indexes.ascending("id")),
        IndexSpec("backgrounds", "backgrounds_inactive", Indexes.ascending("inactive")),
        IndexSpec("layouts", "layouts_id", Indexes.ascending("id")),
        IndexSpec("layouts", "layouts_inactive", Indexes.ascending("inactive")),
        IndexSpec("decorations", "decorations_id", Indexes.ascending("id")),
        IndexSpec("decorations", "decorations_inactive", Indexes.ascending("inactive")),
        IndexSpec("dailystores", "daily_stores_id", Indexes.ascending("id")),

        // youtube webhooks
        IndexSpec("youtubeWebhooks", "youtube_webhooks_channel_id", Indexes.ascending("channelId"))
    )

    suspend fun ensure(database: MongoDatabase) {
        var created = 0
        var failed = 0

        for (spec in specs) {
            try {
                database.getCollection<Document>(spec.collection).createIndex(
                    spec.keys,
                    IndexOptions().name(spec.name).unique(spec.unique)
                )
                created++
            } catch (e: Exception) {
                failed++
                logger.warn(e) { "Could not create index ${spec.name} on ${spec.collection}" }
            }
        }

        if (failed > 0) {
            logger.warn { "Indexes ensured with $failed failure(s) out of ${specs.size}" }
        } else {
            logger.info { "Ensured $created database indexes" }
        }
    }
}
