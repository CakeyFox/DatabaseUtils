package net.cakeyfox.foxy.database.core.utils

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.data.checkout.Checkout
import net.cakeyfox.foxy.database.data.guild.Key
import net.cakeyfox.foxy.database.data.store.StoreItem
import net.cakeyfox.foxy.database.utils.builders.CheckoutBuilder
import org.bson.Document
import java.util.UUID

class PaymentUtils(
    private val client: DatabaseClient
) {
    suspend fun updateCheckout(checkoutId: String, block: CheckoutBuilder.() -> Unit) {
        val update = CheckoutBuilder().apply(block).toDocument()
        if (update.isEmpty()) return

        client.collections.checkouts.updateOne(
            Document("checkoutId", checkoutId),
            Document("\$set", update)
        )
    }

    suspend fun getOrCreateCheckout(userId: String, itemId: String, isAnnual: Boolean = false, valueToPay: Double? = null) : Checkout {
        return client.withRetry {
            val existingDocument = client.collections.checkouts.findOne(
                and(
                    eq("userId", userId),
                    eq("isApproved", false)
                )
            )

            if (existingDocument != null) {
                existingDocument
            } else {
                val newCheckout = Checkout(
                    userId = userId,
                    isApproved = false,
                    valueToPay = valueToPay,
                    checkoutId = UUID.randomUUID().toString(),
                    itemId = itemId,
                    isAnnual = isAnnual
                )

                client.collections.checkouts.insertOne(newCheckout)
                newCheckout
            }
        }
    }

    suspend fun getCheckout(checkoutId: String): Checkout? {
        return client.withRetry {
            client.collections.checkouts.findOne(
                and(
                    eq("checkoutId", checkoutId),
                    eq("isApproved", false)
                )
            )
        }
    }

    suspend fun getProductFromStore(productId: String): StoreItem? {
        return client.withRetry {
            client.collections.storeItems.findOne(eq("itemId", productId))
        }
    }

    suspend fun getCheckoutByUserId(userId: String): Checkout? {
        return client.withRetry {
            client.collections.checkouts.findOne(
                and(
                    eq("userId", userId),
                    eq("isApproved", false)
                )
            )
        }
    }

    suspend fun getKeyByUserId(userId: String): Key? {
        return client.withRetry {
            client.collections.premiumKeys.findOne(eq("ownedBy", userId))
        }
    }

    suspend fun registerKey(userId: String): Key {
        val key = Key(
            key = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .uppercase(),
            ownedBy = userId,
            usedBy = null
        )

        return client.withRetry {
            client.collections.premiumKeys.findOne(eq("ownedBy", userId)) ?: run {
                client.collections.premiumKeys.insertOne(key)
                key
            }
        }
    }

    suspend fun deleteCheckout(checkoutId: String): Boolean {
        return client.withRetry {
            client.collections.checkouts.deleteOne(eq("checkoutId", checkoutId))

            true
        }
    }
}
