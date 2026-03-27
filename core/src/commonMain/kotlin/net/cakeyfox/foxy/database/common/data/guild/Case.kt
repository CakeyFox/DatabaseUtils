package net.cakeyfox.foxy.database.common.data.guild

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.cakeyfox.foxy.database.common.data.MongoDateSerializer

@Serializable
data class Case(
    val guildId: String,
    val caseId: Long,
    val reason: String? = null,

    @Serializable(with = MongoDateSerializer::class)
    val duration: Instant? = null,

    @Serializable(with = MongoDateSerializer::class)
    val createdAt: Instant? = Clock.System.now(),
    val type: CaseType,
    val staff: String,
    val members: List<String>,
    val active: Boolean = true
)

enum class CaseType {
    WARN,
    MUTE,
    KICK,
    BAN
}
