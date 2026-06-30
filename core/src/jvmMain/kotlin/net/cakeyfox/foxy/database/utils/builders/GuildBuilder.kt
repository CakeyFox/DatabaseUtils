package net.cakeyfox.foxy.database.utils.builders

import kotlinx.datetime.Instant
import net.cakeyfox.foxy.database.data.guild.YouTubeChannel
import org.bson.Document
import kotlin.collections.map
import kotlin.collections.mutableListOf

class GuildBuilder {
    val guildJoinLeaveModule = WelcomerModuleBuilder()
    val autoRoleModule = AutoRoleModuleBuilder()
    val antiRaidModule = AntiRaidModuleBuilder()
    val guildSettings = GuildSettingsBuilder()
    val musicSettings = MusicSettingsBuilder()
    val serverLogModule = ServerLogModule()
    val moderationUtils = ModerationUtilsBuilder()
    val inviteBlockerSettings = InviteBlockerSettingsBuilder()
    var guildAddedAt: Long? = null
    var leftAt: Instant? = null
    val followedYouTubeChannels = mutableListOf<YouTubeChannelBuilder>()
    val dashboardLogs = mutableListOf<DashboardLogBuilder>()
    val tempBans = mutableListOf<TempBanBuilder>()
    val reportSettings = ReportSettingsBuilder()

    fun toDocument(): Document {
        val setOps = mutableMapOf<String, Any?>()

        guildAddedAt?.let { setOps["guildAddedAt"] = it }
        leftAt?.let { setOps["leftAt"] = it.toBsonDate() }

        setOps.putAll(serverLogModule.toDocument("serverLogModule"))
        setOps.putAll(guildJoinLeaveModule.toDocument("GuildJoinLeaveModule"))
        setOps.putAll(autoRoleModule.toDocument("AutoRoleModule"))
        setOps.putAll(antiRaidModule.toDocument("antiRaidModule"))
        setOps.putAll(guildSettings.toDocument("guildSettings"))
        setOps.putAll(musicSettings.toDocument("musicSettings"))
        setOps.putAll(moderationUtils.toDocument("moderationUtils"))
        setOps.putAll(inviteBlockerSettings.toDocument("inviteBlockerSettings"))
        setOps.putAll(reportSettings.toDocument("reportSettings"))

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

class ReportSettingsBuilder {
    var isEnabled: Boolean? = null
    var channelToSendReports: String? = null

    fun toDocument(prefix: String): Document {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        channelToSendReports?.let { map["$prefix.channelToSendReports"] = it }
        return Document(map)
    }
}

class ServerLogModule {
    var sendVoiceChannelLogs: Boolean? = null
    var sendDeletedMessagesLogs: Boolean? = null
    var sendUpdatedMessagesLogs: Boolean? = null
    var channelToSendExpiredBans: String? = null
    var sendMessageUpdateLogsToChannel: String? = null
    var sendMessageDeleteLogsToChannel: String? = null
    var sendVoiceLogsToChannel: String? = null
    var sendExpiredBansLogs: Boolean? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        channelToSendExpiredBans?.let { map["$prefix.channelToSendExpiredBans"] = it }
        sendExpiredBansLogs?.let { map["$prefix.sendExpiredBansLogs"] = it }
        sendVoiceChannelLogs?.let { map["$prefix.sendVoiceChannelLogs"] = it }
        sendDeletedMessagesLogs?.let { map["$prefix.sendDeletedMessagesLogs"] = it }
        sendUpdatedMessagesLogs?.let { map["$prefix.sendUpdatedMessagesLogs"] = it }
        sendMessageUpdateLogsToChannel?.let { map["$prefix.sendMessageUpdateLogsToChannel"] = it }
        sendMessageDeleteLogsToChannel?.let { map["$prefix.sendMessageDeleteLogsToChannel"] = it }
        sendVoiceLogsToChannel?.let { map["$prefix.sendVoiceLogsToChannel"] = it }
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
    var sendDmWelcomeMessage: Boolean? = null
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
    var roles: MutableList<String>? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        roles?.let { map["$prefix.roles"] = it }
        return map
    }
}

class AntiRaidModuleBuilder {
    var handleMultipleMessages: Boolean? = null
    var handleMultipleJoins: Boolean? = null
    var handleMultipleChars: Boolean? = null
    var actionForMassJoin: String? = null
    var actionForMassMessage: String? = null
    var actionForMassChars: String? = null
    var actionForMaxWarns: String? = null
    var timeoutDuration: Long? = null
    var repeatedCharsThreshold: Long? = null
    var warnsThreshold: Long? = null
    var newUsersThreshold: Long? = null
    var messagesThreshold: Long? = null
    var alertChannel: String? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        handleMultipleMessages?.let { map["$prefix.handleMultipleMessages"] = it }
        handleMultipleChars?.let { map["$prefix.handleMultipleChars"] = it }
        handleMultipleJoins?.let { map["$prefix.handleMultipleJoins"] = it }
        actionForMassJoin?.let { map["$prefix.actionForMassJoin"] = it }
        actionForMassMessage?.let { map["$prefix.actionForMassMessage"] = it }
        actionForMassChars?.let { map["$prefix.actionForMassChars"] = it }
        messagesThreshold?.let { map["$prefix.messagesThreshold"] = it }
        repeatedCharsThreshold?.let { map["$prefix.repeatedCharsThreshold"] = it }
        warnsThreshold?.let { map["$prefix.warnsThreshold"] = it }
        newUsersThreshold?.let { map["$prefix.newUsersThreshold"] = it }
        alertChannel?.let { map["$prefix.alertChannel"] = it }
        actionForMaxWarns?.let { map["$prefix.actionForMaxWarns"] = it }
        timeoutDuration?.let { map["$prefix.timeoutDuration"] = it }
        return map
    }
}

class InviteBlockerSettingsBuilder {
    var isEnabled: Boolean? = null
    var channelsThatCanSendInvites: MutableList<String>? = null
    var rolesThatCanSendInvites: MutableList<String>? = null
    var message: String? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        message?.let { map["$prefix.message"] = it }
        channelsThatCanSendInvites?.let { map["$prefix.channelsThatCanSendInvites"] = it }
        rolesThatCanSendInvites?.let { map["$prefix.rolesThatCanSendInvites"] = it }
        return map
    }
}

class GuildSettingsBuilder {
    var prefix: String? = null
    var language: String? = null
    var disabledCommands: MutableList<String>? = null
    var blockedChannels: MutableList<String>? = null
    var sendMessageIfChannelIsBlocked: Boolean? = null
    var deleteMessageIfCommandIsExecuted: Boolean? = null
    var usersWhoCanAccessDashboard: MutableList<String>? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        this.prefix?.let { map["$prefix.prefix"] = it }
        language?.let { map["$prefix.language"] = it }
        sendMessageIfChannelIsBlocked?.let { map["$prefix.sendMessageIfChannelIsBlocked"] = it }
        deleteMessageIfCommandIsExecuted?.let { map["$prefix.deleteMessageIfCommandIsExecuted"] = it }
        blockedChannels?.let { map["$prefix.blockedChannels"] = it }
        usersWhoCanAccessDashboard?.let { map["$prefix.usersWhoCanAccessDashboard"] = it }
        disabledCommands?.let { map["$prefix.disabledCommands"] = it }
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