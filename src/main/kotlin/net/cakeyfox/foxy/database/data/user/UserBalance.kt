package net.cakeyfox.foxy.database.data.user

import kotlinx.serialization.Serializable

@Serializable
data class UserBalance(
    val userId: String,
    val balance: Long
)