package net.cakeyfox.foxy.database.utils.builders

import kotlinx.datetime.Instant
import org.bson.Document
import toBsonDate

class MarryBuilder {

    var marriedDate: Instant? = null
    var lastFirstUserLetter: Instant? = null
    var lastSecondUserLetter: Instant? = null
    var firstUserId: String? = null
    var secondUserId: String? = null
    var marriageName: String? = null
    var canChangeMarriageName: Boolean? = null

    private var firstUserLettersInc: Int = 0
    private var secondUserLettersInc: Int = 0
    private var affinityPointsInc: Int = 0

    fun incFirstUserLetters(amount: Int = 1) {
        firstUserLettersInc += amount
    }

    fun incSecondUserLetters(amount: Int = 1) {
        secondUserLettersInc += amount
    }

    fun decAffinityPoints(amount: Int = 1) {
        affinityPointsInc -= amount
    }

    fun incAffinityPoints(amount: Int = 1) {
        affinityPointsInc += amount
    }

    fun toDocument(): Document {
        val setMap = mutableMapOf<String, Any?>()
        val incMap = mutableMapOf<String, Any?>()

        marriedDate?.let { setMap["marriedDate"] = it.toBsonDate() }
        firstUserId?.let { setMap["firstUser.id"] = it }
        secondUserId?.let { setMap["secondUser.id"] = it }
        lastFirstUserLetter?.let { setMap["firstUser.lastLetter"] = it.toBsonDate() }
        lastSecondUserLetter?.let { setMap["secondUser.lastLetter"] = it.toBsonDate() }
        marriageName?.let { setMap["marriageName"] = it }
        canChangeMarriageName?.let { setMap["canChangeMarriageName"] = it }

        if (firstUserLettersInc != 0)
            incMap["firstUser.letterCount"] = firstUserLettersInc

        if (secondUserLettersInc != 0)
            incMap["secondUser.letterCount"] = secondUserLettersInc

        if (affinityPointsInc != 0)
            incMap["affinityPoints"] = affinityPointsInc

        val doc = Document()
        if (setMap.isNotEmpty()) doc["\$set"] = setMap
        if (incMap.isNotEmpty()) doc["\$inc"] = incMap
        return doc
    }
}