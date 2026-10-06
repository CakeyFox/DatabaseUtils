package net.cakeyfox.foxy.database.core.utils

import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import mu.KotlinLogging
import net.cakeyfox.foxy.database.common.data.utils.DailyStoreContent
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

    suspend fun getStoreContent(): DailyStoreContent {
        val backgrounds = getActiveBackgrounds()
        val layouts = getActiveLayouts()
        val decorations = getActiveDecorations()

        val store = client.collections.dailyStores.find(eq("id", "store")).first()

        val backgroundIds = store.itens
            .filter { it.type == "background" }
            .map { it.id }
            .toSet()

        val layoutIds = store.itens
            .filter { it.type == "layout" }
            .map { it.id }
            .toSet()

        val decorationIds = store.itens
            .filter { it.type == "decoration" }
            .map { it.id }
            .toSet()

        return DailyStoreContent(
            backgrounds = backgrounds.filter { it.id in backgroundIds },
            layouts = layouts.filter { it.id in layoutIds },
            decorations = decorations.filter { it.id in decorationIds }
        )
    }

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
            client.collections.dailyStores.updateOne(
                Document("id", "store"),
                Document("\$set", update),
                upsert = true
            )
        }
    }

    suspend fun getActiveBackgrounds(): List<Background> {
        return client.collections.backgrounds.findMany(Document("inactive", false))
    }

    suspend fun getActiveLayouts(): List<Layout> {
        return client.collections.layouts.findMany(Document("inactive", false))
    }

    suspend fun getActiveDecorations(): List<Decoration> {
        return client.collections.decorations.findMany(Document("inactive", false))
    }

    suspend fun getBackground(backgroundId: String): Background? {
        return client.withRetry {
            client.collections.backgrounds.findOne(Document("id", backgroundId))
        }
    }

    suspend fun getLayout(layoutId: String): Layout? {
        return client.withRetry {
            client.collections.layouts.findOne(Document("id", layoutId))
        }
    }

    suspend fun getDecoration(decorationId: String): Decoration? {
        return client.withRetry {
            client.collections.decorations.findOne(Document("id", decorationId))
        }
    }

    suspend fun getBadges(): List<Badge> {
        return client.withRetry {
            client.collections.badges.findMany()
        }
    }
}
