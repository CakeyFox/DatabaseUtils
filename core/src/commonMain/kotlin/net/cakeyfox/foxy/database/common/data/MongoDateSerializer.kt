package net.cakeyfox.foxy.database.common.data

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Serializer for MongoDB date fields.
 * Accepts:
 *   - {"$date": "2021-08-01T00:00:00Z"}
 *   - {"$date": {"$numberLong": "1640630250393"}}
 *   - 1640630250393 (raw)
 */

object MongoDateSerializer : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MongoDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("MongoDateSerializer only supports JSON")

        val element = jsonDecoder.decodeJsonElement()

        return try {
            when (element) {
                JsonNull -> null


                is JsonPrimitive -> {
                    // Raw epoch millis
                    element.longOrNull?.let {
                        Instant.fromEpochMilliseconds(it)
                    }
                }

                is JsonObject -> {
                    val dateField = element["\$date"] ?: return null

                    when (dateField) {
                        is JsonPrimitive -> {
                            val iso = dateField.contentOrNull
                            iso?.let { Instant.parse(it) }
                        }

                        is JsonObject -> {
                            val longStr = (dateField["\$numberLong"] as? JsonPrimitive)?.contentOrNull
                            longStr?.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) }
                        }

                        else -> null
                    }
                }

                else -> null

            }
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value == null) {
            encoder.encodeNull()
            return
        }

        val millis = value.toEpochMilliseconds()
        val jsonObject = JsonObject(
            mapOf(
                "\$date" to JsonObject(
                    mapOf("\$numberLong" to JsonPrimitive(millis.toString()))
                )
            )
        )

        (encoder as? JsonEncoder)?.encodeJsonElement(jsonObject)
            ?: encoder.encodeString(jsonObject.toString())
    }
}