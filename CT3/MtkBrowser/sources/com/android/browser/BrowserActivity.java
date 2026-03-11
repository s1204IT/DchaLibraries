package com.android.browser;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.browser.PermissionHelper;
import com.android.browser.provider.SnapshotProvider;
import com.android.browser.stub.NullController;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;

public class BrowserActivity extends Activity {
    private static final String[] DELETE_WHERE_ARGS = {"100", "0"};
    private boolean mAllGranted;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private ActivityController mController = NullController.INSTANCE;
    private PermissionHelper.PermissionCallback mPermissionCallback = new PermissionHelper.PermissionCallback() {
        @Override
        public void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            if (grantResults == null || grantResults.length <= 0) {
                return;
            }
            BrowserActivity.this.mAllGranted = true;
            int i = 0;
            while (true) {
                if (i >= grantResults.length) {
                    break;
                }
                if (grantResults[i] == 0) {
                    i++;
                } else {
                    BrowserActivity.this.mAllGranted = false;
                    Log.d("browser/BrowserActivity", permissions[i] + " is not granted !");
                    break;
                }
            }
            if (!BrowserActivity.this.mAllGranted) {
                String toastStr = BrowserActivity.this.getString(134545700);
                Toast.makeText(BrowserActivity.this.getApplicationContext(), toastStr, 1).show();
                BrowserActivity.this.finish();
            }
            BrowserActivity.this.doResume();
        }
    };

    private class DeleteFailedDownload implements Runnable {
        DeleteFailedDownload(BrowserActivity this$0, DeleteFailedDownload deleteFailedDownload) {
            this();
        }

        private DeleteFailedDownload() {
        }

        @Override
        public void run() {
            ContentResolver cr = BrowserActivity.this.getContentResolver();
            cr.delete(SnapshotProvider.Snapshots.CONTENT_URI, "progress < ? AND is_done = ?", BrowserActivity.DELETE_WHERE_ARGS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        this.mController.onPause();
        super.onStop();
    }

    @Override
    public void onCreate(Bundle icicle) {
        DeleteFailedDownload deleteFailedDownload = null;
        super.onCreate(icicle);
        this.mAllGranted = false;
        PermissionHelper.init(this);
        if (isTablet(this)) {
            getWindow().setSoftInputMode(16);
        }
        if (shouldIgnoreIntents()) {
            finish();
            return;
        }
        if (IntentHandler.handleWebSearchIntent(this, null, getIntent())) {
            finish();
            return;
        }
        this.mController = createController();
        this.mController.start(icicle == null ? getIntent() : null);
        Thread deleteFailDownload = new Thread(new DeleteFailedDownload(this, deleteFailedDownload));
        deleteFailDownload.start();
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

    @VisibleForTesting
    Controller getController() {
        return (Controller) this.mController;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (shouldIgnoreIntents()) {
            return;
        }
        if (this.mController == NullController.INSTANCE) {
            Log.w("browser/BrowserActivity", "onNewIntent for Action_Search Intent reached before finish(), so enter onNewIntent instead of on create");
            startActivity(intent);
            finish();
        } else {
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
        Log.v("browser", "ignore intents: " + ignore);
        return ignore;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mAllGranted) {
            doResume();
            return;
        }
        List<String> permissionsRequestList = PermissionHelper.getInstance().getAllUngrantedPermissions();
        if (permissionsRequestList.size() > 0) {
            PermissionHelper.getInstance().requestPermissions(permissionsRequestList, this.mPermissionCallback);
        } else {
            doResume();
        }
    }

    public void doResume() {
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
        if (!this.mController.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
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
        if (this.mController.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (this.mController.onKeyLongPress(keyCode, event)) {
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (this.mController.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
        if (this.mController.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        if (this.mController.dispatchKeyShortcutEvent(event)) {
            return true;
        }
        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mController.dispatchTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if (this.mController.dispatchTrackballEvent(ev)) {
            return true;
        }
        return super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (this.mController.dispatchGenericMotionEvent(ev)) {
            return true;
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("browser/BrowserActivity", " onRequestPermissionsResult " + requestCode);
        PermissionHelper.getInstance().onPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        WindowManager.LayoutParams lp;
        if (isInMultiWindowMode || !isTablet(this) || (lp = getWindow().getAttributes()) == null) {
            return;
        }
        lp.flags |= 16777216;
        lp.flags &= Integer.MIN_VALUE;
        getWindow().setAttributes(lp);
        Log.d("browser", "BrowserActivity.onMultiWindowModeChanged");
    }
}
