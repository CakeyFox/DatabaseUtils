import net.cakeyfox.foxy.database.data.guild.YouTubeChannel
import org.bson.Document
import kotlin.collections.map

class GuildBuilder {
    val guildJoinLeaveModule = WelcomerModuleBuilder()
    val autoRoleModule = AutoRoleModuleBuilder()
    val antiRaidModule = AntiRaidModuleBuilder()
    val guildSettings = GuildSettingsBuilder()
    val musicSettings = MusicSettingsBuilder()

    var guildAddedAt: Long? = null
    val followedYouTubeChannels = mutableListOf<YouTubeChannelBuilder>()
    val dashboardLogs = mutableListOf<DashboardLogBuilder>()

    fun toDocument(): Document {
        val setOps = mutableMapOf<String, Any?>()

        guildAddedAt?.let { setOps["guildAddedAt"] = it }

        setOps.putAll(guildJoinLeaveModule.toDocument("guildJoinLeaveModule"))
        setOps.putAll(autoRoleModule.toDocument("autoRoleModule"))
        setOps.putAll(antiRaidModule.toDocument("antiRaidModule"))
        setOps.putAll(guildSettings.toDocument("guildSettings"))
        setOps.putAll(musicSettings.toDocument("musicSettings"))


        if (followedYouTubeChannels.isNotEmpty()) {
            setOps["followedYouTubeChannels"] = followedYouTubeChannels.map { it.toMap() }
        }

        if (dashboardLogs.isNotEmpty()) {
            setOps["dashboardLogs"] = dashboardLogs.map { it.toMap() }
        }

        return Document("\$set", setOps)
    }
}

class WelcomerModuleBuilder {
    var isEnabled: Boolean? = null
    var joinMessage: String? = null
    var leaveMessage: String? = null
    var alertWhenUserLeaves: Boolean? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        joinMessage?.let { map["$prefix.joinMessage"] = it }
        leaveMessage?.let { map["$prefix.leaveMessage"] = it }
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

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        this.prefix?.let { map["$prefix.prefix"] = it }
        language?.let { map["$prefix.language"] = it }
        if (disabledCommands.isNotEmpty()) map["$prefix.disabledCommands"] = disabledCommands
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
