package com.android.browser.stub;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import com.android.browser.ActivityController;

/* loaded from: classes.dex */
public class NullController implements ActivityController {
    public static NullController INSTANCE = new NullController();

    private NullController() {
    }

    @Override // com.android.browser.ActivityController
    public void start(Intent intent) {
    }

    @Override // com.android.browser.ActivityController
    public void onSaveInstanceState(Bundle bundle) {
    }

    @Override // com.android.browser.ActivityController, com.android.browser.UiController
    public void handleNewIntent(Intent intent) {
    }

    @Override // com.android.browser.ActivityController
    public void onResume() {
    }

    @Override // com.android.browser.ActivityController
    public boolean onMenuOpened(int i, Menu menu) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public void onOptionsMenuClosed(Menu menu) {
    }

    @Override // com.android.browser.ActivityController
    public void onContextMenuClosed(Menu menu) {
    }

    @Override // com.android.browser.ActivityController
    public void onPause() {
    }

    @Override // com.android.browser.ActivityController
    public void onDestroy() {
    }

    @Override // com.android.browser.ActivityController
    public void onConfgurationChanged(Configuration configuration) {
    }

    @Override // com.android.browser.ActivityController
    public void onLowMemory() {
    }

    @Override // com.android.browser.ActivityController
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override // com.android.browser.ActivityController, com.android.browser.UiController
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
    }

    @Override // com.android.browser.ActivityController
    public boolean onContextItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public void onActionModeStarted(ActionMode actionMode) {
    }

    @Override // com.android.browser.ActivityController
    public void onActionModeFinished(ActionMode actionMode) {
    }

    @Override // com.android.browser.ActivityController
    public void onActivityResult(int i, int i2, Intent intent) {
    }

    @Override // com.android.browser.ActivityController
    public boolean onSearchRequested() {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override // com.android.browser.ActivityController
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }
}
