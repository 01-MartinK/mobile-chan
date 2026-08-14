package com.mk.mobilechan.ui.navigation

import com.mk.mobilechan.R

enum class AppModules(
    val labelRes: Int,
    val url: String,
    val mirrors: List<String>?
) {
    FOUR_CHAN(R.string.four_chan, "https://a.4cdn.org", null),
    END_CHAN(R.string.end_chan, "endchan.net", listOf("endchan.org"))
}