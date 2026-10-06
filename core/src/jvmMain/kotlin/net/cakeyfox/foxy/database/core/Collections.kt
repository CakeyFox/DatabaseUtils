package net.cakeyfox.foxy.database.core

import net.cakeyfox.foxy.database.common.data.guild.Case
import net.cakeyfox.foxy.database.common.data.guild.GuildErrors
import net.cakeyfox.foxy.database.common.data.marry.CoupleStoreItem
import net.cakeyfox.foxy.database.common.data.marry.Marry
import net.cakeyfox.foxy.database.core.utils.BotUtils
import net.cakeyfox.foxy.database.data.bot.Command
import net.cakeyfox.foxy.database.data.bot.YouTubeWebhook
import net.cakeyfox.foxy.database.data.checkout.Checkout
import net.cakeyfox.foxy.database.data.guild.FoxyverseGuild
import net.cakeyfox.foxy.database.data.guild.Guild
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.profile.Background
import net.cakeyfox.foxy.database.data.profile.Badge
import net.cakeyfox.foxy.database.data.profile.Decoration
import net.cakeyfox.foxy.database.data.profile.Layout
import net.cakeyfox.foxy.database.data.store.DailyStore
import net.cakeyfox.foxy.database.data.store.StoreItem
import net.cakeyfox.foxy.database.data.user.FoxyUser

/**
 * The single source of truth for collection names. Every [TypedCollection] binds a model to one of
 * these, so a collection name is never a literal scattered through the CRUD code again.
 */
private object Names {
    const val GUILDS = "guilds"
    const val USERS = "users"
    const val CASES = "cases"
    const val MARRIAGES = "marriages"
    const val KEYS = "keys"
    const val GUILD_ERRORS = "guild_errors"
    const val YOUTUBE_WEBHOOKS = "youtubeWebhooks"
    const val CHECKOUTS = "checkoutlists"
    const val STORE_ITEMS = "storeitems"
    const val COMMANDS = "commands"
    const val BOT_SETTINGS = "botSettings"
    const val BACKGROUNDS = "backgrounds"
    const val LAYOUTS = "layouts"
    const val DECORATIONS = "decorations"
    const val BADGES = "badges"
    const val DAILY_STORES = "dailystores"
    const val COUPLE_SHOP = "couple_shop"
    const val FOXYVERSE = "foxyverses"
}

class Collections(private val client: DatabaseClient) {
    val guilds get() = TypedCollection(client, Names.GUILDS, Guild.serializer())
    val users get() = TypedCollection(client, Names.USERS, FoxyUser.serializer())
    val cases get() = TypedCollection(client, Names.CASES, Case.serializer())
    val marriages get() = TypedCollection(client, Names.MARRIAGES, Marry.serializer())
    val premiumKeys get() = TypedCollection(client, Names.KEYS, Key.serializer())
    val guildErrors get() = TypedCollection(client, Names.GUILD_ERRORS, GuildErrors.serializer())
    val youtubeWebhooks get() = TypedCollection(client, Names.YOUTUBE_WEBHOOKS, YouTubeWebhook.serializer())
    val checkouts get() = TypedCollection(client, Names.CHECKOUTS, Checkout.serializer())
    val storeItems get() = TypedCollection(client, Names.STORE_ITEMS, StoreItem.serializer())
    val commands get() = TypedCollection(client, Names.COMMANDS, Command.serializer())
    val botSettings get() = TypedCollection(client, Names.BOT_SETTINGS, BotUtils.Companion.BotSettings.serializer())
    val backgrounds get() = TypedCollection(client, Names.BACKGROUNDS, Background.serializer())
    val layouts get() = TypedCollection(client, Names.LAYOUTS, Layout.serializer())
    val decorations get() = TypedCollection(client, Names.DECORATIONS, Decoration.serializer())
    val badges get() = TypedCollection(client, Names.BADGES, Badge.serializer())
    val dailyStores get() = TypedCollection(client, Names.DAILY_STORES, DailyStore.serializer())
    val coupleShop get() = TypedCollection(client, Names.COUPLE_SHOP, CoupleStoreItem.serializer())
    val foxyverseGuilds get() = TypedCollection(client, Names.FOXYVERSE, FoxyverseGuild.serializer())
}
