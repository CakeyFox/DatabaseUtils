package net.cakeyfox.foxy.database.common.data.marry

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.cakeyfox.foxy.database.common.data.MongoDateSerializer

@Serializable
data class Marry(
    val marryId: String,
    @Serializable(with = MongoDateSerializer::class)
    val marriedDate: Instant? = null,
    val firstUser: User,
    val secondUser: User,
    val marriageName: String? = null,
    val canChangeMarriageName: Boolean? = false,
    val affinityPoints: Int,
) {
    @Serializable
    data class User(
        val id: String,
        val letterCount: Int,
        @Serializable(with = MongoDateSerializer::class)
        val lastLetter: Instant? = null,
    )
}