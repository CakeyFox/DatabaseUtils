package net.cakeyfox.foxy.database.utils.builders

import org.bson.Document

class CheckoutBuilder {
    var userId: String? = null
    var itemId: String? = null
    var isApproved: Boolean? = null
    var paymentId: String? = null

    fun toDocument(): Document {
        val map = mutableMapOf<String, Any?>()
        userId?.let { map["userId"] = it }
        itemId?.let { map["itemId"] = it }
        isApproved?.let { map["isApproved"] = it }
        paymentId?.let { map["paymentId"] = it }
        return Document(map)
    }
}
