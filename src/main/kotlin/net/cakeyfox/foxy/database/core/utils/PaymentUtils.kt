package net.cakeyfox.foxy.database.core.utils

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
        val builder = CheckoutBuilder().apply(block)
        val collection = client.database.getCollection<Checkout>("checkoutlists")
        collection.updateOne(
            Document("checkoutId", checkoutId),
            Document("\$set", builder.toDocument())
        )
    }

    suspend fun getCheckout(checkoutId: String): Checkout? {
        return client.withRetry {
            val checkouts = client.database.getCollection<Checkout>("checkoutlists")
            val existingDocument = checkouts.find(
                and(
                    eq("checkoutId", checkoutId),
                    eq("isApproved", false)
                )
            ).firstOrNull()

            existingDocument
        }
    }

    suspend fun getProductFromStore(productId: String): StoreItem? {
        return client.withRetry {
            val storeItems = client.database.getCollection<StoreItem>("storeitems")
            val existingDocument = storeItems.find(eq("itemId", productId))
                .firstOrNull()

            existingDocument
        }
    }

    suspend fun getCheckoutByUserId(userId: String): Checkout? {
        return client.withRetry {
            val checkouts = client.database.getCollection<Checkout>("checkoutlists")
            val existingDocument = checkouts.find(
                and(
                    eq("userId", userId),
                    eq("isApproved", false)
                )
            )
                .firstOrNull()

            existingDocument
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
            val keys = client.database.getCollection<Key>("keys")
            keys.find(eq("ownedBy", userId)).firstOrNull() ?: run {
                keys.insertOne(key)
                key
            }
        }
    }

    suspend fun deleteCheckout(checkoutId: String): Boolean {
        return client.withRetry {
            val checkouts = client.database.getCollection<Checkout>("checkoutlists")
            checkouts.deleteOne(eq("checkoutId", checkoutId))

            true
        }
    }
}