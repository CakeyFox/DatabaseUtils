package net.cakeyfox.foxy.database.utils.builders

/**
 * Marker for the update DSL. Keeps nested builder scopes from reaching into their outer builder, so
 * e.g. configuring `guildSettings` can't silently assign to a `moderationUtils` field.
 */
@DslMarker
annotation class FoxyDsl
