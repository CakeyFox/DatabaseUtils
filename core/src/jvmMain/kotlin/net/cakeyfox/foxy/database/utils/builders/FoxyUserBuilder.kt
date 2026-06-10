package net.cakeyfox.foxy.database.utils.builders

import org.bson.Document
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date

class FoxyUserBuilder {
    val userProfile = UserProfileBuilder()
    val userPremium = UserPremiumBuilder()
    val userCakes = UserCakesBuilder()
    val marryStatus = MarryStatusBuilder()
    val userSettings = UserSettingsBuilder()
    val userBirthday = UserBirthdayBuilder()
    val roulette = RouletteBuilder()
    val notifications = NotificationsBuilder()

    var isBanned: Boolean? = null
    var banReason: String? = null
    var banDate: Instant? = null
    var lastRob: Long? = null
    var lastVote: Instant? = null
    var notifiedForVote: Boolean? = null
    var voteCount: Int? = null

    fun toDocument(): Document {
        val setMap = mutableMapOf<String, Any?>()
        val incMap = mutableMapOf<String, Any?>()
        val pushMap = mutableMapOf<String, Any?>()

        isBanned?.let { setMap["isBanned"] = it }
        banReason?.let { setMap["banReason"] = it }
        banDate?.let { setMap["banDate"] = it.toBsonDate() }
        lastRob?.let { setMap["lastRob"] = it }
        lastVote?.let { setMap["lastVote"] = it.toBsonDate() }
        notifiedForVote?.let { setMap["notifiedForVote"] = it }
        voteCount?.let { setMap["voteCount"] = it }

        mergeBuilderMaps(userProfile.toDocument("userProfile"), setMap, incMap, pushMap)
        mergeBuilderMaps(userPremium.toDocument("userPremium"), setMap, incMap, pushMap)
        mergeBuilderMaps(userCakes.toDocument("userCakes"), setMap, incMap, pushMap)
        mergeBuilderMaps(marryStatus.toDocument("marryStatus"), setMap, incMap, pushMap)
        mergeBuilderMaps(userSettings.toDocument("userSettings"), setMap, incMap, pushMap)
        mergeBuilderMaps(userBirthday.toDocument("userBirthday"), setMap, incMap, pushMap)
        mergeBuilderMaps(roulette.toDocument("roulette"), setMap, incMap, pushMap)
        mergeBuilderMaps(notifications.toDocument("notifications"), setMap, incMap, pushMap)

        val doc = Document()
        if (setMap.isNotEmpty()) doc["\$set"] = setMap
        if (incMap.isNotEmpty()) doc["\$inc"] = incMap
        if (pushMap.isNotEmpty()) doc["\$push"] = pushMap
        return doc
    }

    private fun mergeBuilderMaps(
        builderDoc: Document,
        setMap: MutableMap<String, Any?>,
        incMap: MutableMap<String, Any?>,
        pushMap: MutableMap<String, Any?>
    ) {
        builderDoc.forEach { (key, value) ->
            when (key) {
                "\$set" -> setMap.putAll(value as Map<String, Any?>)
                "\$inc" -> incMap.putAll(value as Map<String, Any?>)
                "\$push" -> pushMap.putAll(value as Map<String, Any?>)
                else -> setMap[key] = value
            }
        }
    }
}

class NotificationsBuilder {
    var disableTempBanNotifications: Boolean? = false
    var disableDailyReminderNotifications: Boolean? = false
    var disableInactivityTaxNotifications: Boolean? = false
    var disableUpvoteNotifications: Boolean? = false

    fun toDocument(prefix: String): Document {
        val map = mutableMapOf<String, Any?>()
        disableTempBanNotifications?.let { map["$prefix.disableTempBanNotifications"] = it }
        disableDailyReminderNotifications?.let { map["$prefix.disableDailyReminderNotifications"] = it }
        disableInactivityTaxNotifications?.let { map["$prefix.disableInactivityTaxNotifications"] = it }
        disableUpvoteNotifications?.let { map["$prefix.disableUpvoteNotifications"] = it }

        return Document(map)
    }
}

class UserProfileBuilder {
    var background: String? = null
    var layout: String? = null
    var aboutme: String? = null
    var disabledBadges: List<String>? = null
    var backgroundList = mutableListOf<String>()
    var layoutList = mutableListOf<String>()
    var decorationList = mutableListOf<String>()
    var decoration: String? = null
    var lastRep: Instant? = null
    var repCount: Int? = null

    fun toDocument(prefix: String): Document {
        val setMap = mutableMapOf<String, Any?>()
        val pushMap = mutableMapOf<String, Any?>()

        background?.let { setMap["$prefix.background"] = it }
        layout?.let { setMap["$prefix.layout"] = it }
        aboutme?.let { setMap["$prefix.aboutme"] = it }
        disabledBadges?.let { setMap["$prefix.disabledBadges"] = it }
        lastRep?.let { setMap["$prefix.lastRep"] = it.toBsonDate() }
        decoration?.let { setMap["$prefix.decoration"] = it }
        repCount?.let { setMap["$prefix.repCount"] = it }

        if (backgroundList.isNotEmpty()) {
            pushMap["$prefix.backgroundList"] = Document("\$each", backgroundList)
        }
        if (layoutList.isNotEmpty()) {
            pushMap["$prefix.layoutList"] = Document("\$each", layoutList)
        }
        if (decorationList.isNotEmpty()) {
            pushMap["$prefix.decorationList"] = Document("\$each", decorationList)
        }

        val update = Document()
        if (setMap.isNotEmpty()) update["\$set"] = Document(setMap)
        if (pushMap.isNotEmpty()) update["\$push"] = Document(pushMap)

        return update
    }
}

class UserPremiumBuilder {
    var premium: Boolean? = null
    var premiumDate: Instant? = null
    var premiumType: String? = null

    fun toDocument(prefix: String): Document {
        val map = mutableMapOf<String, Any?>()
        premium?.let { map["$prefix.premium"] = it }
        premiumType?.let { map["$prefix.premiumType"] = it }
        premiumDate?.let { map["$prefix.premiumDate"] = it.toBsonDate() }
        return Document(map)
    }
}

class UserCakesBuilder {
    private var balanceIncrement: Double = 0.0
    var balance: Double? = null
    var lastDaily: Instant? = null
    var lastInactivityTax: Instant? = null
    var notifiedForDaily: Boolean? = null
    var warnedAboutInactivityTax: Boolean? = null

    fun addCakes(value: Long) {
        balanceIncrement += value
    }

    fun removeCakes(value: Long) {
        balanceIncrement -= value
    }

    fun toDocument(prefix: String): Document {
        val setMap = mutableMapOf<String, Any?>()
        val incMap = mutableMapOf<String, Any?>()
        val pushMap = mutableMapOf<String, Any?>()

        if (balanceIncrement != 0.0) incMap["$prefix.balance"] = balanceIncrement
        lastInactivityTax?.let { setMap["$prefix.lastInactivityTax"] = it.toBsonDate() }
        lastDaily?.let { setMap["$prefix.lastDaily"] = it.toBsonDate() }
        notifiedForDaily?.let { setMap["$prefix.notifiedForDaily"] = it }
        warnedAboutInactivityTax?.let { setMap["$prefix.warnedAboutInactivityTax"] = it }

        val doc = Document()
        if (setMap.isNotEmpty()) doc["\$set"] = setMap
        if (incMap.isNotEmpty()) doc["\$inc"] = incMap
        if (pushMap.isNotEmpty()) doc["\$push"] = pushMap
        return doc
    }
}

class MarryStatusBuilder {
    var cantMarry: Boolean? = null

    fun toDocument(prefix: String): Document {
        val map = mutableMapOf<String, Any?>()
        cantMarry?.let { map["$prefix.cantMarry"] = it }
        return Document(map)
    }
}

class UserSettingsBuilder {
    var language: String? = null
    fun toDocument(prefix: String) = Document().apply { language?.let { put("$prefix.language", it) } }
}

class UserBirthdayBuilder {
    var isEnabled: Boolean? = null
    var lastMessage: Instant? = null
    var birthday: Instant? = null

    fun toDocument(prefix: String): Document {
        val map = mutableMapOf<String, Any?>()
        isEnabled?.let { map["$prefix.isEnabled"] = it }
        lastMessage?.let { map["$prefix.lastMessage"] = it.toBsonDate() }
        birthday?.let { map["$prefix.birthday"] = it.toBsonDate() }
        return Document(map)
    }
}

class RouletteBuilder {
    var availableSpins: Int? = null
    fun toDocument(prefix: String) = Document().apply { availableSpins?.let { put("$prefix.availableSpins", it) } }
}

fun Instant?.toBsonDate(): Date? = this?.let { Date.from(it.toJavaInstant()) }