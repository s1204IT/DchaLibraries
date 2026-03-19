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

public class NullController implements ActivityController {
    public static NullController INSTANCE = new NullController();

    private NullController() {
    }

    @Override
    public void start(Intent intent) {
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
    }

    @Override
    public void handleNewIntent(Intent intent) {
    }

    @Override
    public void onResume() {
    }

    @Override
    public boolean onMenuOpened(int i, Menu menu) {
        return false;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onConfgurationChanged(Configuration configuration) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }
}
