package com.android.browser;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import com.android.browser.stub.NullController;

public class BrowserActivity extends Activity {
    private ActivityController mController = NullController.INSTANCE;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (shouldIgnoreIntents()) {
            finish();
        } else {
            if (IntentHandler.handleWebSearchIntent(this, null, getIntent())) {
                finish();
                return;
            }
            this.mController = createController();
            Intent intent = icicle == null ? getIntent() : null;
            this.mController.start(intent);
        }
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    private Controller createController() {
        UI ui;
        Controller controller = new Controller(this);
        boolean xlarge = isTablet(this);
        if (xlarge) {
            ui = new XLargeUi(this, controller);
        } else {
            ui = new PhoneUi(this, controller);
        }
        controller.setUi(ui);
        return controller;
    }

    Controller getController() {
        return (Controller) this.mController;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!shouldIgnoreIntents()) {
            if ("--restart--".equals(intent.getAction())) {
                Bundle outState = new Bundle();
                this.mController.onSaveInstanceState(outState);
                finish();
                getApplicationContext().startActivity(new Intent(getApplicationContext(), (Class<?>) BrowserActivity.class).addFlags(268435456).putExtra("state", outState));
                return;
            }
            this.mController.handleNewIntent(intent);
        }
    }

    private boolean shouldIgnoreIntents() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
        }
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) getSystemService("power");
        }
        boolean ignore = !this.mPowerManager.isScreenOn();
        return ignore | this.mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mController.onResume();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == 0) {
            this.mController.onMenuOpened(featureId, menu);
            return true;
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        this.mController.onOptionsMenuClosed(menu);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        this.mController.onContextMenuClosed(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        this.mController.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        this.mController.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mController.onDestroy();
        this.mController = NullController.INSTANCE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mController.onConfgurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        this.mController.onLowMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return this.mController.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return this.mController.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mController.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        this.mController.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return this.mController.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mController.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return this.mController.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.mController.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        this.mController.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        this.mController.onActionModeFinished(mode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        this.mController.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onSearchRequested() {
        return this.mController.onSearchRequested();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == 126 || event.getKeyCode() == 127 || event.getKeyCode() == 128) {
            return false;
        }
        return this.mController.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return this.mController.dispatchKeyShortcutEvent(event) || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return this.mController.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return this.mController.dispatchTrackballEvent(ev) || super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return this.mController.dispatchGenericMotionEvent(ev) || super.dispatchGenericMotionEvent(ev);
    }
}
