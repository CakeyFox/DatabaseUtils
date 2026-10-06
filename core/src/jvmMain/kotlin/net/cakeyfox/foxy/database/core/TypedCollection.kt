package net.cakeyfox.foxy.database.core

import com.mongodb.client.model.CountOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.bson.Document
import org.bson.conversions.Bson

/**
 * Encodes any `@Serializable` value to a [Document] using [DatabaseClient.json]. Use this for values
 * pushed into nested arrays (`$push`/`$addToSet`): the default codec registry has no POJO codec, so
 * passing a data class directly to `Updates.push` fails at runtime.
 */
inline fun <reified T> DatabaseClient.encodeToDocument(value: T): Document =
    Document.parse(json.encodeToString(serializer(), value))

/**
 * The single serialization boundary for a Mongo collection.
 *
 * Every document goes through `MongoCollection<Document>` + [DatabaseClient.json] instead of a
 * typed `MongoCollection<T>`: [net.cakeyfox.foxy.database.common.data.MongoDateSerializer] requires a
 * JSON encoder, so no BSON codec can ever decode these models. Encoding/decoding here therefore
 * replicates the proven `Document.parse(json)` / `document.toJson()` round trip used across the
 * codebase.
 *
 * These methods execute directly. Retrying is the caller's responsibility via
 * [DatabaseClient.withRetry], which keeps the retry/connect boundary in one place.
 */
class TypedCollection<T : Any>(
    private val client: DatabaseClient,
    val collectionName: String,
    private val serializer: KSerializer<T>,
) {
    private val raw: MongoCollection<Document>
        get() = client.database.getCollection(collectionName)

    fun encode(value: T): Document =
        Document.parse(client.json.encodeToString(serializer, value))

    fun decode(document: Document): T =
        client.json.decodeFromString(serializer, document.toJson())

    fun find(filter: Bson = Document(), projection: Bson? = null): Flow<T> =
        findFlow(filter, projection).map { decode(it) }

    suspend fun findOne(filter: Bson, projection: Bson? = null): T? =
        findFlow(filter, projection).firstOrNull()?.let(::decode)

    suspend fun findById(id: Any): T? = findOne(Document("_id", id))

    suspend fun findMany(
        filter: Bson = Document(),
        projection: Bson? = null,
        sort: Bson? = null,
        skip: Int? = null,
        limit: Int? = null,
    ): List<T> {
        var flow = findFlow(filter, projection)
        sort?.let { flow = flow.sort(it) }
        skip?.let { flow = flow.skip(it) }
        limit?.let { flow = flow.limit(it) }
        return flow.toList().map(::decode)
    }

    suspend fun findDocument(filter: Bson, projection: Bson? = null): Document? =
        findFlow(filter, projection).firstOrNull()

    suspend fun insertOne(value: T) = raw.insertOne(encode(value))

    /** Escape hatch for documents that need a post-serialization tweak (see `createUser`). */
    suspend fun insertOne(document: Document) = raw.insertOne(document)

    suspend fun insertMany(values: List<T>) = raw.insertMany(values.map(::encode))

    suspend fun replaceOne(filter: Bson, value: T, upsert: Boolean = false) =
        raw.replaceOne(filter, encode(value), ReplaceOptions().upsert(upsert))

    suspend fun upsertById(id: Any, value: T) = replaceOne(Document("_id", id), value, upsert = true)

    suspend fun updateOne(
        filter: Bson,
        update: Bson,
        upsert: Boolean = false,
        arrayFilters: List<Bson>? = null,
    ) {
        val options = UpdateOptions().upsert(upsert)
        arrayFilters?.let { options.arrayFilters(it) }
        raw.updateOne(filter, update, options)
    }

    suspend fun updateMany(
        filter: Bson,
        update: Bson,
        upsert: Boolean = false,
        arrayFilters: List<Bson>? = null,
    ) {
        val options = UpdateOptions().upsert(upsert)
        arrayFilters?.let { options.arrayFilters(it) }
        raw.updateMany(filter, update, options)
    }

    suspend fun findOneAndUpdate(
        filter: Bson,
        update: Bson,
        upsert: Boolean = false,
        arrayFilters: List<Bson>? = null,
        returnDocument: ReturnDocument = ReturnDocument.AFTER,
    ): T? {
        val options = FindOneAndUpdateOptions().upsert(upsert).returnDocument(returnDocument)
        arrayFilters?.let { options.arrayFilters(it) }
        return raw.findOneAndUpdate(filter, update, options)?.let(::decode)
    }

    suspend fun findOneAndDelete(filter: Bson): T? = raw.findOneAndDelete(filter)?.let(::decode)

    suspend fun deleteOne(filter: Bson) = raw.deleteOne(filter)

    suspend fun deleteMany(filter: Bson) = raw.deleteMany(filter)

    suspend fun deleteById(id: Any) = raw.deleteOne(Document("_id", id))

    suspend fun count(filter: Bson = Document()): Long = raw.countDocuments(filter)

    suspend fun exists(filter: Bson): Boolean = raw.countDocuments(filter, CountOptions().limit(1)) > 0

    private fun findFlow(filter: Bson, projection: Bson?): FindFlow<Document> {
        val flow = raw.find(filter)
        return if (projection == null) flow else flow.projection(projection)
    }
}
