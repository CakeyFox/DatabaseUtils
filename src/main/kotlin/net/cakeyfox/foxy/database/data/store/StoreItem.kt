package net.cakeyfox.foxy.database.data.store

data class StoreItem(
    val itemId: String,
    val itemName: String,
    val price: Double,
    val description: String,
    val isSubscription: Boolean,
    val quantity: Int? = 1,
)
