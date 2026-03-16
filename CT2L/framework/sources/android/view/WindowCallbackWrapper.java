package android.view;

import android.view.ActionMode;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class WindowCallbackWrapper implements Window.Callback {
    private Window.Callback mWrapped;

    public WindowCallbackWrapper(Window.Callback wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("Window callback may not be null");
        }
        this.mWrapped = wrapped;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return this.mWrapped.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return this.mWrapped.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return this.mWrapped.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        return this.mWrapped.dispatchTrackballEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return this.mWrapped.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return this.mWrapped.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        return this.mWrapped.onCreatePanelView(featureId);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return this.mWrapped.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return this.mWrapped.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return this.mWrapped.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return this.mWrapped.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
        this.mWrapped.onWindowAttributesChanged(attrs);
    }

    @Override
    public void onContentChanged() {
        this.mWrapped.onContentChanged();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        this.mWrapped.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onAttachedToWindow() {
        this.mWrapped.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        this.mWrapped.onDetachedFromWindow();
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        this.mWrapped.onPanelClosed(featureId, menu);
    }

    @Override
    public boolean onSearchRequested() {
        return this.mWrapped.onSearchRequested();
    }

    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return this.mWrapped.onWindowStartingActionMode(callback);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        this.mWrapped.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        this.mWrapped.onActionModeFinished(mode);
    }
}
