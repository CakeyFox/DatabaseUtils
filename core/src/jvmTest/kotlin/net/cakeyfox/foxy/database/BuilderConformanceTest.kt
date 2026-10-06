package net.cakeyfox.foxy.database

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import net.cakeyfox.foxy.database.common.data.marry.Marry
import net.cakeyfox.foxy.database.data.checkout.Checkout
import net.cakeyfox.foxy.database.data.guild.Guild
import net.cakeyfox.foxy.database.data.store.StoreItem
import net.cakeyfox.foxy.database.data.user.FoxyUser
import net.cakeyfox.foxy.database.utils.builders.CheckoutBuilder
import net.cakeyfox.foxy.database.utils.builders.DashboardLogBuilder
import net.cakeyfox.foxy.database.utils.builders.FoxyUserBuilder
import net.cakeyfox.foxy.database.utils.builders.GuildBuilder
import net.cakeyfox.foxy.database.utils.builders.MarryBuilder
import net.cakeyfox.foxy.database.utils.builders.TempBanBuilder
import net.cakeyfox.foxy.database.utils.builders.YouTubeChannelBuilder
import org.bson.Document
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the frozen schema: every dotted key a builder emits must resolve to a real field in the
 * model's serializer descriptor. Typos (and the PascalCase `GuildJoinLeaveModule` / `AutoRoleModule`
 * keys) are therefore caught at build time instead of silently writing shadow fields.
 *
 * [allowlist] exists only for known-intentional orphans.
 */
class BuilderConformanceTest {
    private val updateOperators = listOf("\$set", "\$inc", "\$push", "\$addToSet")

    @Test
    fun `GuildBuilder keys resolve against Guild`() {
        val builder = GuildBuilder().apply {
            guildAddedAt = 1L
            leftAt = Instant.fromEpochMilliseconds(1)

            serverLogModule.sendVoiceChannelLogs = true
            serverLogModule.channelToSendExpiredBans = "c"
            guildJoinLeaveModule.isEnabled = true
            guildJoinLeaveModule.joinChannel = "c"
            autoRoleModule.isEnabled = true
            autoRoleModule.roles = mutableListOf("r")
            antiRaidModule.handleMultipleMessages = true
            antiRaidModule.actionForMaxWarns = "KICK"
            guildSettings.prefix = "!"
            guildSettings.language = "en-US"
            musicSettings.defaultVolume = 10
            moderationUtils.sendPunishmentsToAChannel = true
            inviteBlockerSettings.isEnabled = true
            reportSettings.isEnabled = true
            strictMode.isEnabled = true
            guildAnalytics.totalBlockedInvites = 1

            joinGateSettings.sendDmWhenPunished = true
            joinGateSettings.membersWithoutAvatarHandler { isEnabled = true }
            joinGateSettings.newAccountsHandler { isEnabled = true }
            joinGateSettings.unverifiedBotsAdditions { isEnabled = true }
            joinGateSettings.thirdPartyAuthSettings { isEnabled = true }
            joinGateSettings.blockMemberWithInviteLink { isEnabled = true }

            antiSelfbotModule.enableRoleHoneypot = true

            followedYouTubeChannels += YouTubeChannelBuilder().apply { channelId = "yt" }
            dashboardLogs += DashboardLogBuilder().apply { authorId = "a" }
            tempBans += TempBanBuilder().apply { userId = "u" }
        }

        assertKeysResolve(builder.toDocument(), Guild.serializer().descriptor)
    }

    @Test
    fun `FoxyUserBuilder keys resolve against FoxyUser`() {
        val builder = FoxyUserBuilder().apply {
            isBanned = true
            banReason = "spam"
            banDate = Instant.fromEpochMilliseconds(1)
            lastVote = Instant.fromEpochMilliseconds(2)
            notifiedForVote = true
            voteCount = 3
            steamUserId = "s"
            robloxUserId = "r"
            riotUserId = "i"
            lastBroadcastedMessageId = "m"

            userProfile.background = "bg"
            userProfile.backgroundList += "bg"
            userProfile.layoutList += "l"
            userProfile.decorationList += "d"
            userProfile.repCount = 1
            userProfile.lastRep = Instant.fromEpochMilliseconds(3)
            userPremium.premium = true
            userPremium.premiumDate = Instant.fromEpochMilliseconds(4)
            userCakes.addCakes(500)
            userCakes.lastDaily = Instant.fromEpochMilliseconds(5)
            marryStatus.cantMarry = true
            userSettings.language = "en-US"
            userBirthday.isEnabled = true
            userBirthday.birthday = Instant.fromEpochMilliseconds(6)
            roulette.availableSpins = 2
            notifications.disableUpvoteNotifications = true
        }

        // `lastRob` is a known orphan: the builder exposes it but FoxyUser has no such field.
        assertKeysResolve(builder.toDocument(), FoxyUser.serializer().descriptor, allowlist = setOf("lastRob"))
    }

    @Test
    fun `MarryBuilder keys resolve against Marry`() {
        val builder = MarryBuilder().apply {
            marriedDate = Instant.fromEpochMilliseconds(1)
            firstUserId = "a"
            secondUserId = "b"
            lastFirstUserLetter = Instant.fromEpochMilliseconds(2)
            lastSecondUserLetter = Instant.fromEpochMilliseconds(3)
            marriageName = "us"
            canChangeMarriageName = true
            incFirstUserLetters()
            incSecondUserLetters(2)
            incAffinityPoints(3)
            decAffinityPoints(1)
        }

        assertKeysResolve(builder.toDocument(), Marry.serializer().descriptor)
    }

    @Test
    fun `CheckoutBuilder keys resolve against Checkout`() {
        val builder = CheckoutBuilder().apply {
            userId = "u"
            itemId = "i"
            isApproved = true
            paymentId = "p"
        }

        assertKeysResolve(builder.toDocument(), Checkout.serializer().descriptor)
    }

    @Test
    fun `StoreItem is serializable`() {
        // Sanity check so the boundary can encode the payment models too.
        StoreItem.serializer()
    }

    private fun assertKeysResolve(
        update: Document,
        descriptor: SerialDescriptor,
        allowlist: Set<String> = emptySet(),
    ) {
        val validPaths = collectPaths(descriptor)
        val emitted = updateOperators.flatMap { operator ->
            (update[operator] as? Map<*, *>)?.keys?.map { it.toString() } ?: emptyList()
        }
        val unresolved = emitted.filterNot { it in validPaths || it in allowlist }

        assertTrue(
            unresolved.isEmpty(),
            "Unresolved builder keys for ${descriptor.serialName}: $unresolved"
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun collectPaths(descriptor: SerialDescriptor, prefix: String = ""): Set<String> {
        val paths = mutableSetOf<String>()

        for (index in 0 until descriptor.elementsCount) {
            val name = descriptor.getElementName(index)
            val child = descriptor.getElementDescriptor(index)
            val path = if (prefix.isEmpty()) name else "$prefix.$name"

            paths += path

            when (child.kind.toString()) {
                "CLASS", "OBJECT" -> paths += collectPaths(child, path)
                "LIST" -> {
                    val element = child.getElementDescriptor(0)
                    if (element.kind.toString() in setOf("CLASS", "OBJECT")) {
                        paths += collectPaths(element, path)
                    }
                }

                else -> Unit
            }
        }

        return paths
    }
}
