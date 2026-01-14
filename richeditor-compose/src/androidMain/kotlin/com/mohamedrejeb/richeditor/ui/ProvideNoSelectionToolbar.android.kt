package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

@Composable
internal actual fun ProvideNoSelectionToolbar(
    disableSelectionToolbar: Boolean,
    content: @Composable () -> Unit,
) {
    if (!disableSelectionToolbar) {
        content()
        return
    }

    val noToolbar: TextToolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus = TextToolbarStatus.Hidden
            override fun hide() = Unit
            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) = Unit
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides noToolbar) {
        content()
    }
}
