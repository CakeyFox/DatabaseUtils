package net.cakeyfox.foxy.database

import kotlinx.datetime.Instant
import net.cakeyfox.foxy.database.common.data.guild.Case
import net.cakeyfox.foxy.database.common.data.guild.CaseType
import net.cakeyfox.foxy.database.common.data.marry.Marry
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.core.TypedCollection
import net.cakeyfox.foxy.database.data.bot.Command
import net.cakeyfox.foxy.database.data.bot.YouTubeWebhook
import net.cakeyfox.foxy.database.data.checkout.Checkout
import net.cakeyfox.foxy.database.data.guild.Guild
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.store.StoreItem
import net.cakeyfox.foxy.database.data.user.FoxyUser
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Validates the `Document` <-> model boundary (and [net.cakeyfox.foxy.database.common.data.MongoDateSerializer])
 * without a live Mongo server: `decode(encode(value)) == value` for every representative model.
 */
class BoundaryRoundTripTest {
    private val client = DatabaseClient()

    @Test
    fun `FoxyUser round trips`() {
        assertRoundTrip(
            client.collections.users,
            FoxyUser(
                _id = "u",
                userCreationTimestamp = Instant.fromEpochMilliseconds(7),
                banDate = Instant.fromEpochMilliseconds(8),
                isBanned = true,
            )
        )
    }

    @Test
    fun `Guild round trips with dates and lists`() {
        assertRoundTrip(
            client.collections.guilds,
            Guild(
                _id = "g",
                guildAddedAt = 1L,
                leftAt = Instant.fromEpochMilliseconds(1_640_630_250_393),
            )
        )
    }

    @Test
    fun `Marry round trips`() {
        assertRoundTrip(
            client.collections.marriages,
            Marry(
                marryId = "m",
                marriedDate = Instant.fromEpochMilliseconds(9),
                firstUser = Marry.User(id = "a", letterCount = 1, lastLetter = Instant.fromEpochMilliseconds(10)),
                secondUser = Marry.User(id = "b", letterCount = 2),
                marriageName = "us",
                canChangeMarriageName = true,
                affinityPoints = 3,
            )
        )
    }

    @Test
    fun `Case round trips`() {
        assertRoundTrip(
            client.collections.cases,
            Case(
                guildId = "g",
                caseId = 1,
                reason = "spam",
                duration = Instant.fromEpochMilliseconds(11),
                createdAt = Instant.fromEpochMilliseconds(12),
                type = CaseType.BAN,
                staff = "s",
                members = listOf("m1", "m2"),
                active = false,
            )
        )
    }

    @Test
    fun `Checkout round trips`() {
        assertRoundTrip(
            client.collections.checkouts,
            Checkout(
                checkoutId = "c",
                userId = "u",
                itemId = "i",
                valueToPay = 9.99,
                isApproved = true,
                paymentId = "p",
                isAnnual = true,
            )
        )
    }

    @Test
    fun `StoreItem round trips`() {
        assertRoundTrip(
            client.collections.storeItems,
            StoreItem(
                itemId = "i",
                itemName = "Premium",
                price = 9.99,
                description = "desc",
                isSubscription = true,
                quantity = 2,
            )
        )
    }

    @Test
    fun `Key round trips`() {
        assertRoundTrip(client.collections.premiumKeys, Key(key = "k", usedBy = "u", ownedBy = "o"))
    }

    @Test
    fun `YouTubeWebhook round trips`() {
        assertRoundTrip(client.collections.youtubeWebhooks, YouTubeWebhook("c", 1L, 2L))
    }

    @Test
    fun `Command round trips`() {
        assertRoundTrip(
            client.collections.commands,
            Command(
                uniqueId = "id",
                name = "name",
                description = "desc",
                subCommands = emptyList(),
                usageCount = 5,
            )
        )
    }

    private fun <T : Any> assertRoundTrip(collection: TypedCollection<T>, value: T) {
        assertEquals(value, collection.decode(collection.encode(value)))
    }
}
