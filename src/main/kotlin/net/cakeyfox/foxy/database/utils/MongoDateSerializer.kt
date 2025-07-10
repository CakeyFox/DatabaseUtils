package net.cakeyfox.foxy.database.utils

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/*
    * This serializer is used to serialize and deserialize MongoDB date objects.
    * For example: {"$date": "2021-08-01T00:00:00Z"}
 */

object MongoDateSerializer : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MongoDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("This serializer can only be used with Json format")
        val jsonElement = jsonDecoder.decodeJsonElement()

        if (jsonElement is JsonNull) return null

        if (jsonElement is JsonObject && jsonElement.containsKey("\$date")) {
            val dateString = (jsonElement["\$date"] as JsonPrimitive).content
            return Instant.parse(dateString)
        } else {
            throw IllegalArgumentException("Expected a MongoDB date object with '\$date' key")
        }
    }

    override fun serialize(encoder: Encoder, value: Instant?) {
        val jsonObject = JsonObject(mapOf("\$date" to JsonPrimitive(value.toString())))
        encoder.encodeString(jsonObject.toString())
    }
}