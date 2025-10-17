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

    var isBanned: Boolean? = null
    var banReason: String? = null
    var banDate: Instant? = null
    var lastRob: Long? = null
    var lastVote: Instant? = null
    var notifiedForVote: Boolean? = null
    var voteCount: Int? = null

    fun toDocument(): Document {
        val map = mutableMapOf<String, Any?>()

        isBanned?.let { map["isBanned"] = it }
        banReason?.let { map["banReason"] = it }
        banDate?.let { map["banDate"] = it }
        lastRob?.let { map["lastRob"] = it }
        lastVote?.let { map["lastVote"] = it }
        notifiedForVote?.let { map["notifiedForVote"] = it }
        voteCount?.let { map["voteCount"] = it }

        map.putAll(userProfile.toDocument("userProfile"))
        map.putAll(userPremium.toDocument("userPremium"))
        map.putAll(userCakes.toDocument("userCakes"))
        map.putAll(marryStatus.toDocument("marryStatus"))
        map.putAll(userSettings.toDocument("userSettings"))
        map.putAll(userBirthday.toDocument("userBirthday"))
        map.putAll(roulette.toDocument("roulette"))

        return Document(map)
    }
}

class UserProfileBuilder {
    var background: String? = null
    var layout: String? = null
    var aboutme: String? = null
    var disabledBadges: List<String>? = null

    val backgroundList = mutableListOf<String>()
    val layoutList = mutableListOf<String>()

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        background?.let { map["$prefix.background"] = it }
        layout?.let { map["$prefix.layout"] = it }
        aboutme?.let { map["$prefix.aboutme"] = it }
        if (backgroundList.isNotEmpty()) map["$prefix.backgroundList"] = backgroundList
        if (layoutList.isNotEmpty()) map["$prefix.layoutList"] = layoutList
        if (disabledBadges.isNullOrEmpty()) map["$prefix.disabledBadges"] = disabledBadges
        return map
    }
}

class UserPremiumBuilder {
    var premium: Boolean? = null
    var premiumDate: Instant? = null
    var premiumType: String? = null
    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        premium?.let { map["$prefix.premium"] = it }
        premiumType?.let { map["$prefix.premiumType"] = it }
        premiumDate?.let { map["$prefix.premiumDate"] = it.toBsonDate() }
        return map
    }
}

class UserCakesBuilder {
    var balance: Double? = null
    var lastInactivityTax: Instant? = null
    var notifiedForDaily: Boolean? = null
    var warnedAboutInactivityTax: Boolean? = null

    fun toDocument(prefix: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        balance?.let { map["$prefix.balance"] = it }
        lastInactivityTax?.let { map["$prefix.lastInactivityTax"] = it.toBsonDate() }
        notifiedForDaily?.let { map["$prefix.notifiedForDaily"] = it }
        warnedAboutInactivityTax?.let { map["$prefix.warnedAboutInactivityTax"] = it }
        return map
    }
}

class MarryStatusBuilder {
    var cantMarry: Boolean? = null
    var marriedWith: String? = null
    var marriedDate: Instant? = null
    fun toDocument(prefix: String) = mutableMapOf<String, Any?>().apply {
        cantMarry?.let { this["$prefix.cantMarry"] = it }
        marriedWith?.let { this["$prefix.marriedWith"] = it }
        marriedDate?.let { this["$prefix.marriedDate"] = it.toBsonDate() }
    }
}

class UserSettingsBuilder {
    var language: String? = null
    fun toDocument(prefix: String) = mutableMapOf<String, Any?>().apply {
        language?.let { this["$prefix.language"] = it }
    }
}

class UserBirthdayBuilder {
    var isEnabled: Boolean? = null
    var lastMessage: Instant? = null
    var birthday: Instant? = null
    fun toDocument(prefix: String) = mutableMapOf<String, Any?>().apply {
        isEnabled?.let { this["$prefix.isEnabled"] = it }
        lastMessage?.let { this["$prefix.lastMessage"] = it.toBsonDate() }
        birthday?.let { this["$prefix.birthday"] = it.toBsonDate() }
    }
}

class RouletteBuilder {
    var availableSpins: Int? = null
    fun toDocument(prefix: String) = mutableMapOf<String, Any?>().apply {
        availableSpins?.let { this["$prefix.availableSpins"] = it }
    }
}

fun Instant?.toBsonDate(): Any? = this?.let { Date.from(it.toJavaInstant()) }
