package net.cakeyfox.foxy.database.utils.builders

import kotlinx.datetime.Instant
import net.cakeyfox.foxy.database.data.guild.YouTubeChannel
import org.bson.Document
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.let

@FoxyDsl
class GuildBuilder {
    val guildJoinLeaveModule = WelcomerModuleBuilder()
    val autoRoleModule = AutoRoleModuleBuilder()
    val antiRaidModule = AntiRaidModuleBuilder()
    val guildSettings = GuildSettingsBuilder()
    val musicSettings = MusicSettingsBuilder()
    val serverLogModule = ServerLogModuleBuilder()
    val moderationUtils = ModerationUtilsBuilder()
    val inviteBlockerSettings = InviteBlockerSettingsBuilder()
    val joinGateSettings = JoinGateSettingsBuilder()
    var guildAddedAt: Long? = null
    var leftAt: Instant? = null
    val followedYouTubeChannels = mutableListOf<YouTubeChannelBuilder>()
    val dashboardLogs = mutableListOf<DashboardLogBuilder>()
    val tempBans = mutableListOf<TempBanBuilder>()
    val reportSettings = ReportSettingsBuilder()
    val guildAnalytics = MetricBuilder()
    val strictMode = StrictModeBuilder()
    val antiSelfbotModule = AntiSelfbotModuleBuilder()

    fun toDocument(): Document {
        val setOps = mutableMapOf<String, Any?>()
        val pushOps = mutableMapOf<String, Any?>()

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
        setOps.putAll(joinGateSettings.toDocument("joinGateSettings"))
        setOps.putAll(guildAnalytics.toDocument("guildAnalytics"))
        setOps.putAll(strictMode.toDocument("strictMode"))
        setOps.putAll(antiSelfbotModule.toDocument("antiSelfbotModule"))

        if (followedYouTubeChannels.isNotEmpty()) {
            pushOps["followedYouTubeChannels"] = Document("\$each", followedYouTubeChannels.map { it.toMap() })
        }

        if (dashboardLogs.isNotEmpty()) {
            pushOps["dashboardLogs"] = Document("\$each", dashboardLogs.map { it.toMap() })
        }

        if (tempBans.isNotEmpty()) {
            pushOps["tempBans"] = Document("\$each", tempBans.map { it.toMap() })
        }

        val document = Document()
        if (setOps.isNotEmpty()) document["\$set"] = setOps
        if (pushOps.isNotEmpty()) document["\$push"] = pushOps
        return document
    }
}

@FoxyDsl
class StrictModeBuilder {
    var isEnabled: Boolean? = null
    var allowedRoles: MutableList<String>? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        allowedRoles?.let { map["$prefix.allowedRoles"] = it }
        return map
    }
}

@FoxyDsl
class AntiSelfbotModuleBuilder {
    var enableRoleHoneypot: Boolean? = null
    var enableChannelHoneypot: Boolean? = null
    var enableMessageHoneypot: Boolean? = null
    var roleHoneypotPunishment: String? = null
    var channelHoneypotPunishment: String? = null
    var messageHoneypotPunishment: String? = null
    var roles: List<String>? = null
    var channels: List<String>? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        enableRoleHoneypot?.let { map["$prefix.enableRoleHoneypot"] = it }
        enableChannelHoneypot?.let { map["$prefix.enableChannelHoneypot"] = it }
        enableMessageHoneypot?.let { map["$prefix.enableMessageHoneypot"] = it }
        roleHoneypotPunishment?.let { map["$prefix.roleHoneypotPunishment"] = it }
        channelHoneypotPunishment?.let { map["$prefix.channelHoneypotPunishment"] = it }
        messageHoneypotPunishment?.let { map["$prefix.messageHoneypotPunishment"] = it }
        roles?.let { map["$prefix.roles"] = it }
        channels?.let { map["$prefix.channels"] = it }

        return map
    }
}

@FoxyDsl
class JoinGateSettingsBuilder {
    var sendDmWhenPunished: Boolean? = null
    var membersWithoutAvatarHandler: MembersWithoutAvatarHandler? = null
    var newAccountsHandler: NewAccountsHandler? = null
    var unverifiedBotsAdditions: UnverifiedBotsAdditions? = null
    var thirdPartyAuthSettings: ThirdPartyAuthSettings? = null
    var blockMemberWithInviteLink: BlockMemberWithInviteLink? = null

    fun membersWithoutAvatarHandler(block: MembersWithoutAvatarHandler.() -> Unit) {
        val handler =
            membersWithoutAvatarHandler ?: MembersWithoutAvatarHandler().also { membersWithoutAvatarHandler = it }
        handler.block()
    }

    fun blockMemberWithInviteLink(block: BlockMemberWithInviteLink.() -> Unit) {
        val handler = blockMemberWithInviteLink ?: BlockMemberWithInviteLink().also { blockMemberWithInviteLink = it }
        handler.block()
    }

    fun thirdPartyAuthSettings(block: ThirdPartyAuthSettings.() -> Unit) {
        val handler = thirdPartyAuthSettings ?: ThirdPartyAuthSettings().also { thirdPartyAuthSettings = it }
        handler.block()
    }

    fun newAccountsHandler(block: NewAccountsHandler.() -> Unit) {
        val handler = newAccountsHandler ?: NewAccountsHandler().also { newAccountsHandler = it }
        handler.block()
    }

    fun unverifiedBotsAdditions(block: UnverifiedBotsAdditions.() -> Unit) {
        val handler = unverifiedBotsAdditions ?: UnverifiedBotsAdditions().also { unverifiedBotsAdditions = it }
        handler.block()
    }

    @FoxyDsl
    inner class BlockMemberWithInviteLink {
        var isEnabled: Boolean? = null
        var channelToSendLogs: String? = null
        var action: String? = null

        fun toDocument(prefix: String): Document {
            val map = mutableMapOf<String, Any?>()
            isEnabled?.let { map["$prefix.isEnabled"] = it }
            channelToSendLogs?.let { map["$prefix.channelToSendLogs"] = it }
            action?.let { map["$prefix.action"] = it }
            return Document(map)
        }
    }

    @FoxyDsl
    inner class ThirdPartyAuthSettings {
        var isEnabled: Boolean? = null
        var channelToSendVerification: String? = null
        var useRobloxAuthentication: Boolean? = null
        var useSteamAuthentication: Boolean? = null
        var useRiotGamesAuthentication: Boolean? = null
        var verifiedRole: String? = null
        var enableSeparatedRoles: Boolean? = null
        var roleForSteam: String? = null
        var roleForRiot: String? = null
        var roleForRoblox: String? = null

        fun toDocument(prefix: String): Document {
            val map = mutableMapOf<String, Any?>()
            isEnabled?.let { map["$prefix.isEnabled"] = it }
            channelToSendVerification?.let { map["$prefix.channelToSendVerification"] = it }
            useRobloxAuthentication?.let { map["$prefix.useRobloxAuthentication"] = it }
            useSteamAuthentication?.let { map["$prefix.useSteamAuthentication"] = it }
            useRiotGamesAuthentication?.let { map["$prefix.useRiotGamesAuthentication"] = it }
            verifiedRole?.let { map["$prefix.verifiedRole"] = it }
            enableSeparatedRoles?.let { map["$prefix.enableSeparatedRoles"] = it }
            roleForSteam?.let { map["$prefix.roleForSteam"] = it }
            roleForRiot?.let { map["$prefix.roleForRiot"] = it }
            roleForRoblox?.let { map["$prefix.roleForRoblox"] = it }

            return Document(map)
        }
    }

    @FoxyDsl
    inner class MembersWithoutAvatarHandler {
        var isEnabled: Boolean? = null
        var channelToSendLogs: String? = null
        var action: String? = null

        fun toDocument(prefix: String): Document {
            val map = mutableMapOf<String, Any?>()
            isEnabled?.let { map["$prefix.isEnabled"] = it }
            channelToSendLogs?.let { map["$prefix.channelToSendLogs"] = it }
            action?.let { map["$prefix.action"] = it }
            return Document(map)
        }
    }

    @FoxyDsl
    inner class NewAccountsHandler {
        var isEnabled: Boolean? = null
        var action: String? = null
        var channelToSendLogs: String? = null
        var minimumAccountAge: Long? = null
        var manualVerificationChannel: String? = null
        var allowedRolesForVerification: MutableList<String>? = null

        fun toDocument(prefix: String): Document {
            val map = mutableMapOf<String, Any?>()
            isEnabled?.let { map["$prefix.isEnabled"] = it }
            action?.let { map["$prefix.action"] = it }
            channelToSendLogs?.let { map["$prefix.channelToSendLogs"] = it }
            minimumAccountAge?.let { map["$prefix.minimumAccountAge"] = it }
            manualVerificationChannel?.let { map["$prefix.manualVerificationChannel"] = it }
            allowedRolesForVerification?.let { map["$prefix.allowedRolesForVerification"] = it }
            return Document(map)
        }
    }

    @FoxyDsl
    inner class UnverifiedBotsAdditions {
        var isEnabled: Boolean? = null
        var action: String? = null
        var channelToSendLogs: String? = null

        fun toDocument(prefix: String): Document {
            val map = mutableMapOf<String, Any?>()
            isEnabled?.let { map["$prefix.isEnabled"] = it }
            action?.let { map["$prefix.action"] = it }
            channelToSendLogs?.let { map["$prefix.channelToSendLogs"] = it }
            return Document(map)
        }
    }

    fun toDocument(prefix: String): Document {
        val map = mutableMapOf<String, Any?>()
        sendDmWhenPunished?.let { map["$prefix.sendDmWhenPunished"] = it }
        membersWithoutAvatarHandler?.toDocument("$prefix.membersWithoutAvatarHandler")?.let { map.putAll(it) }
        newAccountsHandler?.toDocument("$prefix.newAccountsHandler")?.let { map.putAll(it) }
        unverifiedBotsAdditions?.toDocument("$prefix.unverifiedBotsAdditions")?.let { map.putAll(it) }
        thirdPartyAuthSettings?.toDocument("$prefix.thirdPartyAuthSettings")?.let { map.putAll(it) }
        blockMemberWithInviteLink?.toDocument("$prefix.blockMemberWithInviteLink")?.let { map.putAll(it) }

        return Document(map)
    }
}

@FoxyDsl
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

@FoxyDsl
class ServerLogModuleBuilder {
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

@FoxyDsl
class ModerationUtilsBuilder {
    var sendPunishmentsToAChannel: Boolean? = null
    var customPunishmentMessage: String? = null
    var channelToSendPunishments: String? = null
    var sendPunishmentsToDm: Boolean? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        channelToSendPunishments?.let { map["$prefix.channelToSendPunishments"] = it }
        sendPunishmentsToDm?.let { map["$prefix.sendPunishmentsToDm"] = it }
        sendPunishmentsToAChannel?.let { map["$prefix.sendPunishmentsToAChannel"] = it }
        customPunishmentMessage?.let { map["$prefix.customPunishmentMessage"] = it }
        return map
    }
}

@FoxyDsl
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

@FoxyDsl
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

@FoxyDsl
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

@FoxyDsl
class InviteBlockerSettingsBuilder {
    var isEnabled: Boolean? = null
    var channelsThatCanSendInvites: MutableList<String>? = null
    var rolesThatCanSendInvites: MutableList<String>? = null
    var message: String? = null
    var blockProfileInvitesAutomodRuleId: String? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        message?.let { map["$prefix.message"] = it }
        channelsThatCanSendInvites?.let { map["$prefix.channelsThatCanSendInvites"] = it }
        rolesThatCanSendInvites?.let { map["$prefix.rolesThatCanSendInvites"] = it }
        blockProfileInvitesAutomodRuleId?.let { map["$prefix.blockProfileInvitesAutomodRuleId"] = it }
        return map
    }
}

@FoxyDsl
class GuildSettingsBuilder {
    var prefix: String? = null
    var language: String? = null
    var disabledCommands: MutableList<String>? = null
    var blockedChannels: MutableList<String>? = null
    var sendMessageIfChannelIsBlocked: Boolean? = null
    var deleteMessageIfCommandIsExecuted: Boolean? = null
    var usersWhoCanAccessDashboard: MutableList<String>? = null
    var useLegacyCommands: Boolean? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        this.prefix?.let { map["$prefix.prefix"] = it }
        language?.let { map["$prefix.language"] = it }
        sendMessageIfChannelIsBlocked?.let { map["$prefix.sendMessageIfChannelIsBlocked"] = it }
        deleteMessageIfCommandIsExecuted?.let { map["$prefix.deleteMessageIfCommandIsExecuted"] = it }
        blockedChannels?.let { map["$prefix.blockedChannels"] = it }
        usersWhoCanAccessDashboard?.let { map["$prefix.usersWhoCanAccessDashboard"] = it }
        disabledCommands?.let { map["$prefix.disabledCommands"] = it }
        useLegacyCommands?.let { map["$prefix.useLegacyCommands"] = it }
        return map
    }
}

@FoxyDsl
class MetricBuilder {
    var totalBlockedInvites: Long = 0
    var totalBlockedSuspectedAccounts: Long = 0
    var totalReportedMessages: Long = 0
    var totalVerifiedMembers: Long = 0
    var totalAddedRoles: Long = 0
    var totalNotifiedVideos: Long = 0

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        if (totalBlockedInvites != 0L) map["$prefix.totalBlockedInvites"] = totalBlockedInvites
        if (totalBlockedSuspectedAccounts != 0L) map["$prefix.totalBlockedSuspectedAccounts"] = totalBlockedSuspectedAccounts
        if (totalReportedMessages != 0L) map["$prefix.totalReportedMessages"] = totalReportedMessages
        if (totalVerifiedMembers != 0L) map["$prefix.totalVerifiedMembers"] = totalVerifiedMembers
        if (totalAddedRoles != 0L) map["$prefix.totalAddedRoles"] = totalAddedRoles
        if (totalNotifiedVideos != 0L) map["$prefix.totalNotifiedVideos"] = totalNotifiedVideos

        return map
    }
}

@FoxyDsl
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

@FoxyDsl
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

@FoxyDsl
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

@FoxyDsl
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
