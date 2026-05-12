package net.cakeyfox.foxy.database.common.data.utils

import net.cakeyfox.foxy.database.data.profile.Background
import net.cakeyfox.foxy.database.data.profile.Decoration
import net.cakeyfox.foxy.database.data.profile.Layout

data class DailyStoreContent(
    val backgrounds: List<Background>,
    val layouts: List<Layout>,
    val decorations: List<Decoration>
)