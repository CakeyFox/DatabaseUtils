package net.cakeyfox.foxy.database

import net.cakeyfox.foxy.database.utils.builders.FoxyUserBuilder
import net.cakeyfox.foxy.database.utils.builders.GuildBuilder
import net.cakeyfox.foxy.database.utils.builders.JoinGateSettingsBuilder
import net.cakeyfox.foxy.database.utils.builders.MarryBuilder
import net.cakeyfox.foxy.database.utils.builders.NotificationsBuilder
import net.cakeyfox.foxy.database.utils.builders.TempBanBuilder
import net.cakeyfox.foxy.database.utils.builders.UserCakesBuilder
import net.cakeyfox.foxy.database.utils.builders.YouTubeChannelBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateOperatorTest {
    @Test
    fun `empty builders produce empty updates`() {
        assertTrue(GuildBuilder().toDocument().isEmpty())
        assertTrue(FoxyUserBuilder().toDocument().isEmpty())
        assertTrue(NotificationsBuilder().toDocument("notifications").isEmpty())
        assertTrue(MarryBuilder().toDocument().isEmpty())
    }

    @Test
    fun `notifications only writes set fields`() {
        val doc = NotificationsBuilder()
            .apply { disableUpvoteNotifications = true }
            .toDocument("notifications")

        assertEquals(setOf("notifications.disableUpvoteNotifications"), doc.keys)
    }

    @Test
    fun `join gate invite link only writes set fields`() {
        val doc = JoinGateSettingsBuilder()
            .apply { blockMemberWithInviteLink { isEnabled = true } }
            .toDocument("joinGateSettings")

        assertEquals(setOf("joinGateSettings.blockMemberWithInviteLink.isEnabled"), doc.keys)
    }

    @Test
    fun `cakes delta lands in inc`() {
        val doc = UserCakesBuilder().apply { addCakes(500) }.toDocument("userCakes")
        val inc = doc["\$inc"] as Map<*, *>

        assertEquals(500.0, inc["userCakes.balance"])
    }

    @Test
    fun `marry deltas land in inc`() {
        val doc = MarryBuilder().apply { incAffinityPoints(3) }.toDocument()
        val inc = doc["\$inc"] as Map<*, *>

        assertEquals(3, inc["affinityPoints"])
    }

    @Test
    fun `guild arrays use push not set`() {
        val doc = GuildBuilder().apply {
            tempBans += TempBanBuilder().apply { userId = "u" }
            followedYouTubeChannels += YouTubeChannelBuilder().apply { channelId = "c" }
        }.toDocument()

        val push = doc["\$push"] as Map<*, *>

        assertTrue(push.containsKey("tempBans"))
        assertTrue(push.containsKey("followedYouTubeChannels"))
        assertFalse(doc.containsKey("\$set"))
    }
}
