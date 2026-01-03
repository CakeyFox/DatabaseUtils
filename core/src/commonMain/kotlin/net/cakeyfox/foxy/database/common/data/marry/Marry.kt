package net.cakeyfox.foxy.database.common.data.marry

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.cakeyfox.foxy.database.common.data.MongoDateSerializer

@Serializable
data class Marry(
    val marryId: String,
    @Serializable(with = MongoDateSerializer::class)
    val marriedDate: Instant? = null,
    val firstUserId: String,
    val secondUserId: String,
    val firstUserLetters: Int,
    val secondUserLetters: Int,
    val marriageName: String? = null,
    val canChangeMarriageName: Boolean? = false,
    val affinityPoints: Int,
)