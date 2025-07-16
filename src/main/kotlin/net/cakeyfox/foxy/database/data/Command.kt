package net.cakeyfox.foxy.database.data

import kotlinx.serialization.Serializable

@Serializable
data class Command(
    val uniqueId: String,
    val name: String,
    val usageCount: Long = 0,
    val description: String,
    val subCommands: List<SubCommand>,
    val usage: String? = null,
    val category: String? = "utils",
    val supportsLegacy: Boolean? = false
) {
    @Serializable
    data class SubCommand(
        val uniqueId: String,
        val name: String,
        val description: String,
        val usage: String? = null,
        val supportsLegacy: Boolean? = false
    )
}
