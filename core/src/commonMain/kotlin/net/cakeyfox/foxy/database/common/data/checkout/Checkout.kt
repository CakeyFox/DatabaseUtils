package net.cakeyfox.foxy.database.data.checkout

data class Checkout(
    val checkoutId: String,
    val userId: String,
    val itemId: String,
    val isApproved: Boolean,
    val paymentId: String? = null,
)
