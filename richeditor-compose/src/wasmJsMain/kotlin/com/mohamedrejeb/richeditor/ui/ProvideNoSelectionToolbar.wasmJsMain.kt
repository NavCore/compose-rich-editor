package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvideNoSelectionToolbar(
    disableSelectionToolbar: Boolean,
    content: @Composable () -> Unit,
) {
    // No platform selection toolbar (ActionMode) available on this target.
    content()
}
