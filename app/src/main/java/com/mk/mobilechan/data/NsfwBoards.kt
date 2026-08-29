package com.mk.mobilechan.data

/**
 * Official 4chan non-worksafe boards (`ws_board = 0`), plus any board the API
 * marks as not worksafe.
 */
object NsfwBoards {
    val tags: Set<String> = setOf(
        "aco",
        "b",
        "bant",
        "d",
        "e",
        "gif",
        "h",
        "hc",
        "hm",
        "hr",
        "pol",
        "r9k",
        "s",
        "s4s",
        "soc",
        "t",
        "trash",
        "u",
        "y",
    )

    fun contains(tag: String): Boolean = tag.trim().trim('/').lowercase() in tags

    fun contains(board: Board): Boolean = board.isNsfw || contains(board.board)

    fun hiddenTags(excluded: Set<String>, allowNsfw: Boolean): Set<String> =
        if (allowNsfw) excluded else excluded + tags

    fun visibleExcluded(excluded: Set<String>, allowNsfw: Boolean): Set<String> =
        if (allowNsfw) excluded else excluded.filterNot(::contains).toSet()
}
