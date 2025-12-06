package net.cakeyfox.foxy.database.data.guild

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.cakeyfox.foxy.database.utils.MongoDateSerializer

@Serializable
data class Guild(
    val _id: String,
    val guildAddedAt: Long,
    val GuildJoinLeaveModule: WelcomerModule,
    val AutoRoleModule: AutoRoleModule? = null,
    val antiRaidModule: AntiRaidModule? = null,
    val guildSettings: GuildSettings,
    val moderationUtils: ModerationUtils? = null,
    val followedYouTubeChannels: List<YouTubeChannel> = emptyList(),
    val musicSettings: MusicSettings? = null,
    val dashboardLogs: List<DashboardLog> = emptyList(),
    val tempBans: List<TempBan>? = emptyList(),
    val serverLogModule: ServerLogModule? = null
)

@Serializable
data class ModerationUtils(
    val channelToSendPunishments: String? = null,
    val sendPunishmentsToAChannel: Boolean = false,
    val customPunishmentMessage: String? = null,
    val customPunishmentDmMessage: String? = null,
    val sendPunishmentsToDm: Boolean? = false
)

@Serializable
data class TempBan(
    val userId: String,
    val reason: String,
    val bannedBy: String,
    @Serializable(with = MongoDateSerializer::class)
    val duration: Instant? = null,
)

@Serializable
data class ServerLogModule(
    val channelToSendLogs: String? = null,
    val channelToSendExpiredBans: String? = null,
    val sendExpiredBansLogs: Boolean? = false,
    val sendVoiceChannelLogs: Boolean? = false,
    val sendDeletedMessagesLogs: Boolean? = false,
    val sendUpdatedMessagesLogs: Boolean? = false
)

@Serializable
data class MusicSettings(
    val defaultVolume: Int = 35,
    val is247ModeEnabled: Boolean = false,
    val requestMusicChannel: String? = null,
    val announceNowPlaying: Boolean = true,
    val updateStageChannelTopic: Boolean = false,
)

@Serializable
data class YouTubeChannel(
    val channelId: String,
    val notificationMessage: String? = null,
    val channelToSend: String? = null,
    val notifiedVideos: List<Video>? = emptyList(),
) {
    @Serializable
    data class Video(
        val id: String,
        @Serializable(with = MongoDateSerializer::class)
        val notifiedAt: Instant? = null
    )
}

@Serializable
data class WelcomerModule(
    val isEnabled: Boolean = false,
    val joinMessage: String? = null,
    val alertWhenUserLeaves: Boolean = false,
    val sendDmWelcomeMessage: Boolean = false,
    val dmWelcomeMessage: String? = null,
    val leaveMessage: String? = null,
    val joinChannel: String? = null,
    val leaveChannel: String? = null,
)

@Serializable
data class AntiRaidModule(
    val handleMultipleMessages: Boolean = false,
    val handleMultipleJoins: Boolean = false,
    val handleMultipleChars: Boolean = false,
    val messagesThreshold: Int = 8,
    val newUsersThreshold: Int = 5,
    val repeatedCharsThreshold: Int = 10,
    val warnsThreshold: Int = 3,
    val alertChannel: String? = null,
    val actionForMassJoin: String = "NOTHING",
    val actionForMassMessage: String = "TIMEOUT",
    val actionForMassChars: String = "WARN",
    val timeoutDuration: Long = 10000,
    val whitelistedChannels: List<String> = emptyList(),
    val whitelistedRoles: List<String> = emptyList(),
)

@Serializable
data class AutoRoleModule(
    val isEnabled: Boolean = false,
    val roles: List<String> = emptyList(),
)

@Serializable
data class GuildSettings(
    val prefix: String = ".",
    val language: String = "pt-BR",
    val disabledCommands: List<String> = emptyList(),
    val blockedChannels: List<String> = emptyList(),
    val sendMessageIfChannelIsBlocked: Boolean = false,
    val deleteMessageIfCommandIsExecuted: Boolean = false,
    val usersWhoCanAccessDashboard: List<String> = emptyList(),
)

@Serializable
data class DashboardLog(
    val authorId: String?,
    val actionType: String?,
    val date: Long
)

// Foxyverse guilds
@Serializable
data class FoxyverseGuild(
    val serverBenefits: ServerBenefits? = null,
    val guildAdmins: List<String>? = emptyList(),
    val serverInvite: String? = null,
) {
    @Serializable
    data class ServerBenefits(
        val givePremiumIfBoosted: GivePremiumIfBoosted
    ) {
        @Serializable
        data class GivePremiumIfBoosted(
            val isEnabled: Boolean? = false,
            val notifyUser: Boolean? = false,
            val textChannelToRedeem: String? = null
        )
    }
}