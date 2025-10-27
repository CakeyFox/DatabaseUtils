package net.cakeyfox.foxy.database.core.utils

import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import mu.KotlinLogging
import net.cakeyfox.foxy.database.data.profile.Background
import net.cakeyfox.foxy.database.data.profile.Badge
import net.cakeyfox.foxy.database.data.profile.Decoration
import net.cakeyfox.foxy.database.data.profile.Layout
import net.cakeyfox.foxy.database.core.DatabaseClient
import net.cakeyfox.foxy.database.data.store.DailyStore
import org.bson.Document
import java.util.Date
import kotlin.collections.mapOf
import kotlin.reflect.jvm.jvmName

class ProfileUtils(
    private val client: DatabaseClient
) {
    private val logger = KotlinLogging.logger(this::class.jvmName)

    suspend fun updateStore() {
        val backgrounds = getActiveBackgrounds()
        val layouts = getActiveLayouts()
        val decorations = getActiveDecorations()


        val randomBackgrounds = backgrounds.shuffled().take(6)
        val randomDecorations = decorations.shuffled().take(2)
        val randomLayouts = layouts.shuffled().take(3)

        val allItems = randomBackgrounds.map { DailyStore.Item(it.id, "background") } +
                randomDecorations.map { DailyStore.Item(it.id, "decoration") } +
                randomLayouts.map { DailyStore.Item(it.id, "layout") }

        val update = Document(
            mapOf(
                "itens" to allItems.map { Document(mapOf("id" to it.id, "type" to it.type)) },
                "lastUpdate" to Date.from(Clock.System.now().toJavaInstant()),
            )
        )

        client.withRetry {
            val collection = client.database.getCollection<Document>("dailystores")
            collection.updateOne(
                Document("id", "store"),
                Document("\$set", update),
                UpdateOptions().upsert(true)
            )
        }
    }

    suspend fun getActiveBackgrounds(): List<Background> {
        val collection = client.database.getCollection<Background>("backgrounds")

        val query = Document("inactive", false)
        return collection.find(query).toList()
    }

    suspend fun getActiveLayouts(): List<Layout> {
        val collection = client.database.getCollection<Layout>("layouts")

        val query = Document("inactive", false)
        return collection.find(query).toList()
    }

    suspend fun getActiveDecorations(): List<Decoration> {
        val collection = client.database.getCollection<Decoration>("decorations")

        val query = Document("inactive", false)
        return collection.find(query).toList()
    }

    suspend fun getBackground(backgroundId: String): Background {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("backgrounds")

            val query = Document("id", backgroundId)
            val existingDocument = collection.find(query).firstOrNull()

            if (existingDocument == null) {
                logger.error { "Background $backgroundId not found" }
                throw Exception("Background $backgroundId not found")
            }

            val documentToJSON = existingDocument.toJson()
            client.json.decodeFromString<Background>(documentToJSON!!)
        }
    }

    suspend fun getLayout(layoutId: String): Layout {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("layouts")

            val query = Document("id", layoutId)
            val existingDocument = collection.find(query).firstOrNull()

            if (existingDocument == null) {
                logger.error { "Layout $layoutId not found" }
                throw Exception("Layout $layoutId not found")
            }

            val documentToJSON = existingDocument.toJson()
            client.json.decodeFromString<Layout>(documentToJSON!!)
        }
    }

    suspend fun getDecoration(decorationId: String): Decoration {
        return client.withRetry {
            val collection = client.database.getCollection<Document>("decorations")

            val query = Document("id", decorationId)
            val existingDocument = collection.find(query).firstOrNull()

            if (existingDocument == null) {
                logger.error { "Decoration $decorationId not found" }
                throw Exception("Decoration $decorationId not found")
            }

            val documentToJSON = existingDocument.toJson()
            client.json.decodeFromString<Decoration>(documentToJSON!!)
        }
    }

    suspend fun getBadges(): List<Badge> {
        return client.withRetry {
            val collection= client.database.getCollection<Document>("badges")

            val badges = mutableListOf<Badge>()
            collection.find().collect {
                val documentToJSON = it.toJson()
                badges.add(client.json.decodeFromString<Badge>(documentToJSON!!))
            }

            badges
        }
    }
}