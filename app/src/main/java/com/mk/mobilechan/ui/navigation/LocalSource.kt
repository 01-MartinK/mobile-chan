package com.mk.mobilechan.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import com.mk.mobilechan.data.Source
import com.mk.mobilechan.data.Sources
import com.mk.mobilechan.data.SourceId

val LocalSource = compositionLocalOf<Source> { Sources.of(SourceId.FOUR_CHAN) }
