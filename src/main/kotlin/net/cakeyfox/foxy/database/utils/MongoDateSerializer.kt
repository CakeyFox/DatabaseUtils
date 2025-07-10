package net.cakeyfox.foxy.database.utils

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

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

        if (jsonElement is JsonNull) {
            return null
        }

        if (jsonElement is JsonObject && jsonElement.containsKey("\$date")) {
            val dateString = (jsonElement["\$date"] as? JsonPrimitive)?.content
                ?: return null
            return Instant.parse(dateString)
        }

        return null
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value == null) {
            encoder.encodeNull()
            return
        }

        val jsonObject = JsonObject(mapOf("\$date" to JsonPrimitive(value.toString())))
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("This serializer can only be used with Json format")
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}