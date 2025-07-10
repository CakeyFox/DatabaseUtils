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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long

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

        return when {
            jsonElement is JsonObject && jsonElement.containsKey("\$date") -> {
                val dateString = (jsonElement["\$date"] as? JsonPrimitive)?.contentOrNull ?: return null
                try {
                    Instant.parse(dateString)
                } catch (e: Exception) {
                    null
                }
            }
            jsonElement is JsonPrimitive && jsonElement.isString -> {
                try {
                    Instant.parse(jsonElement.content)
                } catch (e: Exception) {
                    null
                }
            }
            jsonElement is JsonPrimitive && jsonElement.intOrNull != null -> {
                try {
                    Instant.fromEpochMilliseconds(jsonElement.long)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Instant?) {
        val jsonObject = JsonObject(mapOf("\$date" to JsonPrimitive(value.toString())))
        encoder.encodeString(jsonObject.toString())
    }
}