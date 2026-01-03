import com.mongodb.client.model.Updates
import kotlinx.datetime.Instant
import net.cakeyfox.foxy.database.data.guild.ModerationUtils
import net.cakeyfox.foxy.database.data.guild.TempBan
import net.cakeyfox.foxy.database.data.guild.YouTubeChannel
import org.bson.Document
import kotlin.collections.map

class GuildBuilder {
    val guildJoinLeaveModule = WelcomerModuleBuilder()
    val autoRoleModule = AutoRoleModuleBuilder()
    val antiRaidModule = AntiRaidModuleBuilder()
    val guildSettings = GuildSettingsBuilder()
    val musicSettings = MusicSettingsBuilder()
    val serverLogModule = ServerLogModule()
    val moderationUtils = ModerationUtilsBuilder()
    var guildAddedAt: Long? = null
    val followedYouTubeChannels = mutableListOf<YouTubeChannelBuilder>()
    val dashboardLogs = mutableListOf<DashboardLogBuilder>()
    val tempBans = mutableListOf<TempBanBuilder>()

    fun toDocument(): Document {
        val setOps = mutableMapOf<String, Any?>()

        guildAddedAt?.let { setOps["guildAddedAt"] = it }

        setOps.putAll(serverLogModule.toDocument("serverLogModule"))
        setOps.putAll(guildJoinLeaveModule.toDocument("GuildJoinLeaveModule"))
        setOps.putAll(autoRoleModule.toDocument("AutoRoleModule"))
        setOps.putAll(antiRaidModule.toDocument("antiRaidModule"))
        setOps.putAll(guildSettings.toDocument("guildSettings"))
        setOps.putAll(musicSettings.toDocument("musicSettings"))
        setOps.putAll(moderationUtils.toDocument("moderationUtils"))


        if (followedYouTubeChannels.isNotEmpty()) {
            setOps["followedYouTubeChannels"] = followedYouTubeChannels.map { it.toMap() }
        }

        if (dashboardLogs.isNotEmpty()) {
            setOps["dashboardLogs"] = dashboardLogs.map { it.toMap() }
        }

        if (tempBans.isNotEmpty()) {
            setOps["tempBans"] = tempBans.map { it.toMap() }
        }

        return Document("\$set", setOps)
    }
}

class ServerLogModule {
    var sendVoiceChannelLogs: Boolean? = null
    var sendDeletedMessagesLogs: Boolean? = null
    var sendUpdatedMessagesLogs: Boolean? = null
    var channelToSendExpiredBans: String? = null
    var sendExpiredBansLogs: Boolean? = false

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        channelToSendExpiredBans?.let { map["$prefix.channelToSendExpiredBans"] = it }
        sendExpiredBansLogs?.let { map["$prefix.sendExpiredBansLogs"] = it }
        sendVoiceChannelLogs?.let { map["$prefix.sendVoiceChannelLogs"] = it }
        sendDeletedMessagesLogs?.let { map["$prefix.sendDeletedMessagesLogs"] = it }
        sendUpdatedMessagesLogs?.let { map["$prefix.sendUpdatedMessagesLogs"] = it }

        return map
    }
}

class ModerationUtilsBuilder {
    var customPunishmentMessage: String? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        customPunishmentMessage?.let { map["$prefix.customPunishmentMessage"] = it }

        return map
    }
}

class WelcomerModuleBuilder {
    var isEnabled: Boolean? = null
    var joinMessage: String? = null
    var leaveMessage: String? = null
    var alertWhenUserLeaves: Boolean? = null
    var sendDmWelcomeMessage: Boolean? = false
    var dmWelcomeMessage: String? = null
    var leaveChannel: String? = null
    var joinChannel: String? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        sendDmWelcomeMessage?.let { map["$prefix.sendDmWelcomeMessage"] = it }
        dmWelcomeMessage?.let { map["$prefix.dmWelcomeMessage"] = it }
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        joinMessage?.let { map["$prefix.joinMessage"] = it }
        leaveMessage?.let { map["$prefix.leaveMessage"] = it }
        leaveChannel?.let { map["$prefix.leaveChannel"] = it }
        joinChannel?.let { map["$prefix.joinChannel"] = it }
        alertWhenUserLeaves?.let { map["$prefix.alertWhenUserLeaves"] = it }
        return map
    }
}

class AutoRoleModuleBuilder {
    var isEnabled: Boolean? = null
    val roles = mutableListOf<String>()

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        if (roles.isNotEmpty()) map["$prefix.roles"] = roles
        return map
    }
}

class AntiRaidModuleBuilder {
    var handleMultipleMessages: Boolean? = null
    var handleMultipleJoins: Boolean? = null
    var messagesThreshold: Int? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        handleMultipleMessages?.let { map["$prefix.handleMultipleMessages"] = it }
        handleMultipleJoins?.let { map["$prefix.handleMultipleJoins"] = it }
        messagesThreshold?.let { map["$prefix.messagesThreshold"] = it }
        return map
    }
}

class GuildSettingsBuilder {
    var prefix: String? = null
    var language: String? = null
    val disabledCommands = mutableListOf<String>()
    var blockedChannels = mutableListOf<String>()
    var sendMessageIfChannelIsBlocked: Boolean? = false
    var deleteMessageIfCommandIsExecuted: Boolean? = false
    var usersWhoCanAccessDashboard = mutableListOf<String>()

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        this.prefix?.let { map["$prefix.prefix"] = it }
        language?.let { map["$prefix.language"] = it }
        sendMessageIfChannelIsBlocked?.let { map["$prefix.sendMessageIfChannelIsBlocked"] = it }
        deleteMessageIfCommandIsExecuted?.let { map["$prefix.deleteMessageIfCommandIsExecuted"] = it }
        map["$prefix.blockedChannels"] = blockedChannels
        map["$prefix.usersWhoCanAccessDashboard"] = usersWhoCanAccessDashboard
        map["$prefix.disabledCommands"] = disabledCommands
        return map
    }
}

class MusicSettingsBuilder {
    var defaultVolume: Int? = null
    var is247ModeEnabled: Boolean? = null
    var requestMusicChannel: String? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        defaultVolume?.let { map["$prefix.defaultVolume"] = it }
        is247ModeEnabled?.let { map["$prefix.is247ModeEnabled"] = it }
        requestMusicChannel?.let { map["$prefix.requestMusicChannel"] = it }
        return map
    }
}

class TempBanBuilder {
    var userId: String? = null
    var reason: String? = null
    var duration: Instant? = null

    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        userId?.let { map["userId"] = it }
        reason?.let { map["reason"] = it }
        duration?.let { map["duration"] = it.toBsonDate() }

        return map
    }
}

class YouTubeChannelBuilder {
    var channelId: String? = null
    var notificationMessage: String? = null

    val notifiedVideos = mutableListOf<YouTubeChannel.Video>()

    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        channelId?.let { map["channelId"] = it }
        notificationMessage?.let { map["notificationMessage"] = it }
        if (notifiedVideos.isNotEmpty()) {
            map["notifiedVideos"] = notifiedVideos.map {
                mapOf("id" to it.id, "notifiedAt" to it.notifiedAt)
            }
        }
        return map
    }
}

class DashboardLogBuilder {
    var authorId: String? = null
    var actionType: String? = null
    var date: Long? = null

    fun toMap(): Map<String, Any?> {
        return mutableMapOf<String, Any?>().apply {
            authorId?.let { this["authorId"] = it }
            actionType?.let { this["actionType"] = it }
            date?.let { this["date"] = it }
        }
    }
}
