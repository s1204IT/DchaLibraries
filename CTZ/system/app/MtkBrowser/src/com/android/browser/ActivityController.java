package com.android.browser;

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

/* loaded from: classes.dex */
public interface ActivityController {
    boolean dispatchGenericMotionEvent(MotionEvent motionEvent);

    boolean dispatchKeyEvent(KeyEvent keyEvent);

    boolean dispatchKeyShortcutEvent(KeyEvent keyEvent);

    boolean dispatchTouchEvent(MotionEvent motionEvent);

    boolean dispatchTrackballEvent(MotionEvent motionEvent);

    void handleNewIntent(Intent intent);

    void onActionModeFinished(ActionMode actionMode);

    void onActionModeStarted(ActionMode actionMode);

    void onActivityResult(int i, int i2, Intent intent);

    void onConfgurationChanged(Configuration configuration);

    boolean onContextItemSelected(MenuItem menuItem);

    void onContextMenuClosed(Menu menu);

    void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo);

    boolean onCreateOptionsMenu(Menu menu);

    void onDestroy();

    boolean onKeyDown(int i, KeyEvent keyEvent);

    boolean onKeyLongPress(int i, KeyEvent keyEvent);

    boolean onKeyUp(int i, KeyEvent keyEvent);

    void onLowMemory();

    boolean onMenuOpened(int i, Menu menu);

    boolean onOptionsItemSelected(MenuItem menuItem);

    void onOptionsMenuClosed(Menu menu);

    void onPause();

    boolean onPrepareOptionsMenu(Menu menu);

    void onResume();

    void onSaveInstanceState(Bundle bundle);

    boolean onSearchRequested();

    void start(Intent intent);
}
