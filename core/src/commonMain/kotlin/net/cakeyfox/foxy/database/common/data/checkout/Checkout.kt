package net.cakeyfox.foxy.database.data.checkout

import kotlinx.serialization.Serializable

@Serializable
data class Checkout(
    val checkoutId: String,
    val userId: String,
    val itemId: String,
    val valueToPay: Double? = null,
    val isApproved: Boolean? = false,
    val paymentId: String? = null,
    val isAnnual: Boolean? = false
)
