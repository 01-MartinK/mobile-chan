package com.mk.mobilechan.ui.navigation

import com.mk.mobilechan.R
import com.mk.mobilechan.data.SourceId

enum class AppModules(
    val labelRes: Int,
    val sourceId: SourceId,
) {
    FOUR_CHAN(R.string.four_chan, SourceId.FOUR_CHAN),
    END_CHAN(R.string.end_chan, SourceId.END_CHAN),
}