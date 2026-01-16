package com.mohamedrejeb.richeditor.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.ActionMode
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
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

    // On Android, Compose text selection uses ActionMode (cut/copy/paste).
    // Overriding LocalTextToolbar hides it, but can still briefly "flash" because the ActionMode
    // is started before the toolbar implementation is consulted.
    //
    // We prevent the ActionMode from being created by wrapping the Window.Callback.
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val originalCallback = window?.callback

        if (window != null && originalCallback != null && originalCallback !is NoSelectionActionModeWindowCallback) {
            window.callback = NoSelectionActionModeWindowCallback(
                original = originalCallback,
                context = window.context
            )
        }

        onDispose {
            // Restore the original callback if we replaced it.
            val current = window?.callback
            if (window != null && current is NoSelectionActionModeWindowCallback && current.original === originalCallback) {
                window.callback = originalCallback
            }
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides noToolbar) {
        content()
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private class NoSelectionActionModeWindowCallback(
    val original: Window.Callback,
    private val context: Context,
) : Window.Callback {

    // --- Critical hooks: stop ActionMode before it becomes visible ---
    override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode {
        return NoOpActionMode(context)
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
        return NoOpActionMode(context)
    }

    // --- Delegate everything else ---
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = original.dispatchKeyEvent(event)
    override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean = original.dispatchKeyShortcutEvent(event)
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = original.dispatchTouchEvent(event)
    override fun dispatchTrackballEvent(event: MotionEvent): Boolean = original.dispatchTrackballEvent(event)
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean = original.dispatchGenericMotionEvent(event)
    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean =
        original.dispatchPopulateAccessibilityEvent(event)

    override fun onCreatePanelView(featureId: Int): View? = original.onCreatePanelView(featureId)
    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean = original.onCreatePanelMenu(featureId, menu)
    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean =
        original.onPreparePanel(featureId, view, menu)

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean = original.onMenuOpened(featureId, menu)
    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = original.onMenuItemSelected(featureId, item)
    override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams) = original.onWindowAttributesChanged(attrs)
    override fun onContentChanged() = original.onContentChanged()
    override fun onWindowFocusChanged(hasFocus: Boolean) = original.onWindowFocusChanged(hasFocus)
    override fun onAttachedToWindow() = original.onAttachedToWindow()
    override fun onDetachedFromWindow() = original.onDetachedFromWindow()
    override fun onPanelClosed(featureId: Int, menu: Menu) = original.onPanelClosed(featureId, menu)
    override fun onSearchRequested(): Boolean = original.onSearchRequested()
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent): Boolean = original.onSearchRequested(searchEvent)
    override fun onActionModeStarted(mode: ActionMode) = original.onActionModeStarted(mode)
    override fun onActionModeFinished(mode: ActionMode) = original.onActionModeFinished(mode)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup>?,
        menu: Menu?,
        deviceId: Int,
    ) = original.onProvideKeyboardShortcuts(data, menu, deviceId)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPointerCaptureChanged(hasCapture: Boolean) = original.onPointerCaptureChanged(hasCapture)
}

/**
 * A minimal no-op ActionMode implementation.
 * Returning an ActionMode instance prevents callers from treating it as a failure,
 * while effectively disabling any UI.
 */
private class NoOpActionMode(private val ctx: Context) : ActionMode() {
    override fun setTitle(title: CharSequence?) = Unit
    override fun setTitle(resId: Int) = Unit
    override fun setSubtitle(subtitle: CharSequence?) = Unit
    override fun setSubtitle(resId: Int) = Unit
    override fun setCustomView(view: View?) = Unit
    override fun invalidate() = Unit
    override fun finish() = Unit

    override fun getMenu(): Menu = EmptyMenu()
    override fun getTitle(): CharSequence = ""
    override fun getSubtitle(): CharSequence = ""
    override fun getCustomView(): View? = null
    override fun getMenuInflater(): android.view.MenuInflater = android.view.MenuInflater(ctx)
    override fun getTag(): Any? = null
    override fun setTag(tag: Any?) = Unit
    override fun getTitleOptionalHint(): Boolean = false
    override fun setTitleOptionalHint(titleOptional: Boolean) = Unit
    override fun isTitleOptional(): Boolean = true
}

/** Minimal no-op Menu implementation (required by ActionMode). */
private class EmptyMenu : Menu {
    override fun add(title: CharSequence?): MenuItem = EmptyMenuItem()
    override fun add(titleRes: Int): MenuItem = EmptyMenuItem()
    override fun add(groupId: Int, itemId: Int, order: Int, title: CharSequence?): MenuItem = EmptyMenuItem()
    override fun add(groupId: Int, itemId: Int, order: Int, titleRes: Int): MenuItem = EmptyMenuItem()
    override fun addSubMenu(title: CharSequence?) = null
    override fun addSubMenu(titleRes: Int) = null
    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, title: CharSequence?) = null
    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, titleRes: Int) = null
    override fun addIntentOptions(
        groupId: Int,
        itemId: Int,
        order: Int,
        caller: android.content.ComponentName?,
        specifics: Array<android.content.Intent>?,
        intent: android.content.Intent?,
        flags: Int,
        outSpecificItems: Array<MenuItem>?,
    ): Int = 0

    override fun removeItem(id: Int) = Unit
    override fun removeGroup(groupId: Int) = Unit
    override fun clear() = Unit
    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) = Unit
    override fun setGroupVisible(group: Int, visible: Boolean) = Unit
    override fun setGroupEnabled(group: Int, enabled: Boolean) = Unit
    override fun hasVisibleItems(): Boolean = false
    override fun findItem(id: Int): MenuItem? = null
    override fun size(): Int = 0
    override fun getItem(index: Int): MenuItem = EmptyMenuItem()
    override fun close() = Unit
    override fun performShortcut(keyCode: Int, event: KeyEvent?, flags: Int): Boolean = false
    override fun isShortcutKey(keyCode: Int, event: KeyEvent?): Boolean = false
    override fun performIdentifierAction(id: Int, flags: Int): Boolean = false
    override fun setQwertyMode(isQwerty: Boolean) = Unit
}

/** Minimal no-op MenuItem implementation. */
private class EmptyMenuItem : MenuItem {
    override fun getItemId(): Int = 0
    override fun getGroupId(): Int = 0
    override fun getOrder(): Int = 0
    override fun setTitle(title: CharSequence?): MenuItem = this
    override fun setTitle(title: Int): MenuItem = this
    override fun getTitle(): CharSequence = ""
    override fun setTitleCondensed(title: CharSequence?): MenuItem = this
    override fun getTitleCondensed(): CharSequence? = ""
    override fun setIcon(icon: android.graphics.drawable.Drawable?): MenuItem = this
    override fun setIcon(iconRes: Int): MenuItem = this
    override fun getIcon(): android.graphics.drawable.Drawable? = null
    override fun setIntent(intent: android.content.Intent?): MenuItem = this
    override fun getIntent(): android.content.Intent? = null
    override fun setShortcut(numericChar: Char, alphaChar: Char): MenuItem = this
    override fun setNumericShortcut(numericChar: Char): MenuItem = this
    override fun getNumericShortcut(): Char = 0.toChar()
    override fun setAlphabeticShortcut(alphaChar: Char): MenuItem = this
    override fun getAlphabeticShortcut(): Char = 0.toChar()
    override fun setCheckable(checkable: Boolean): MenuItem = this
    override fun isCheckable(): Boolean = false
    override fun setChecked(checked: Boolean): MenuItem = this
    override fun isChecked(): Boolean = false
    override fun setVisible(visible: Boolean): MenuItem = this
    override fun isVisible(): Boolean = false
    override fun setEnabled(enabled: Boolean): MenuItem = this
    override fun isEnabled(): Boolean = false
    override fun hasSubMenu(): Boolean = false
    override fun getSubMenu(): android.view.SubMenu? = null
    override fun setOnMenuItemClickListener(menuItemClickListener: MenuItem.OnMenuItemClickListener?): MenuItem = this
    override fun getMenuInfo(): android.view.ContextMenu.ContextMenuInfo? = null
    override fun setShowAsAction(actionEnum: Int) = Unit
    override fun setShowAsActionFlags(actionEnum: Int): MenuItem = this
    override fun setActionView(view: View?): MenuItem = this
    override fun setActionView(resId: Int): MenuItem = this
    override fun getActionView(): View? = null
    override fun setActionProvider(actionProvider: android.view.ActionProvider?): MenuItem = this
    override fun getActionProvider(): android.view.ActionProvider? = null
    override fun expandActionView(): Boolean = false
    override fun collapseActionView(): Boolean = false
    override fun isActionViewExpanded(): Boolean = false
    override fun setOnActionExpandListener(listener: MenuItem.OnActionExpandListener?): MenuItem = this

    // Newer API methods
    override fun setContentDescription(contentDescription: CharSequence?): MenuItem = this
    override fun getContentDescription(): CharSequence? = null
    override fun setTooltipText(tooltipText: CharSequence?): MenuItem = this
    override fun getTooltipText(): CharSequence? = null
    override fun setIconTintList(tint: android.content.res.ColorStateList?): MenuItem = this
    override fun getIconTintList(): android.content.res.ColorStateList? = null
    override fun setIconTintMode(tintMode: android.graphics.PorterDuff.Mode?): MenuItem = this
    override fun getIconTintMode(): android.graphics.PorterDuff.Mode? = null
    override fun setAlphabeticShortcut(alphaChar: Char, alphaModifiers: Int): MenuItem = this
    override fun getAlphabeticModifiers(): Int = 0
    override fun setNumericShortcut(numericChar: Char, numericModifiers: Int): MenuItem = this
    override fun getNumericModifiers(): Int = 0
    override fun setShortcut(
        numericChar: Char,
        alphaChar: Char,
        numericModifiers: Int,
        alphaModifiers: Int,
    ): MenuItem = this

}

