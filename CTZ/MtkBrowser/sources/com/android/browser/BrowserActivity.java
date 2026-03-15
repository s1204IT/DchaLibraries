package com.android.browser;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.List;

public class BrowserActivity extends Activity {
    private static final boolean DEBUG = Browser.ENGONLY;
    private static final String[] DELETE_WHERE_ARGS = {"100", "0"};
    private boolean mAllGranted;
    private IntentFilter mIntentFilter;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private ActivityController mController = NullController.INSTANCE;
    private BroadcastReceiver mReceiver = new BroadcastReceiver(this) {
        final BrowserActivity this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BrowserActivity.DEBUG) {
                Log.v("@M_browser", "mReceiver action = " + action);
            }
            if (action.equals("com.mediatek.search.SEARCH_ENGINE_CHANGED")) {
                this.this$0.handleSearchEngineChanged();
            }
            if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
                Extensions.resetPlugins();
            }
        }
    };
    private PermissionHelper.PermissionCallback mPermissionCallback = new PermissionHelper.PermissionCallback(this) {
        final BrowserActivity this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onPermissionsResult(int i, String[] strArr, int[] iArr) {
            if (iArr == null || iArr.length <= 0) {
                return;
            }
            this.this$0.mAllGranted = true;
            int i2 = 0;
            while (true) {
                if (i2 >= iArr.length) {
                    break;
                }
                if (iArr[i2] != 0) {
                    this.this$0.mAllGranted = false;
                    if (BrowserActivity.DEBUG) {
                        Log.d("browser/BrowserActivity", strArr[i2] + " is not granted !");
                    }
                } else {
                    i2++;
                }
            }
            if (!this.this$0.mAllGranted) {
                Toast.makeText(this.this$0.getApplicationContext(), this.this$0.getString(2131492926), 1).show();
                this.this$0.finish();
            }
            this.this$0.doResume();
        }
    };

    private class DeleteFailedDownload implements Runnable {
        final BrowserActivity this$0;

        private DeleteFailedDownload(BrowserActivity browserActivity) {
            this.this$0 = browserActivity;
        }

        @Override
        public void run() {
            this.this$0.getContentResolver().delete(SnapshotProvider.Snapshots.CONTENT_URI, "progress < ? AND is_done = ?", BrowserActivity.DELETE_WHERE_ARGS);
        }
    }

    private Controller createController() {
        Controller controller = new Controller(this);
        controller.setUi(isTablet(this) ? new XLargeUi(this, controller) : new PhoneUi(this, controller));
        return controller;
    }

    private void doResume() {
        if (DEBUG) {
            Log.v("browser", "BrowserActivity.onResume: this=" + this);
        }
        this.mController.onResume();
    }

    private void handleSearchEngineChanged() {
        String searchEngineName = BrowserSettings.getInstance().getSearchEngineName();
        if (DEBUG) {
            Log.v("@M_browser", "ChangeSearchEngineReceiver (search): search_engine---" + searchEngineName);
        }
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(2131296256);
    }

    private boolean shouldIgnoreIntents() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
        }
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) getSystemService("power");
        }
        boolean z = !this.mPowerManager.isScreenOn();
        Log.v("browser", "ignore intents: " + z);
        return z;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return this.mController.dispatchGenericMotionEvent(motionEvent) || super.dispatchGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return this.mController.dispatchKeyEvent(keyEvent) || super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return this.mController.dispatchKeyShortcutEvent(keyEvent) || super.dispatchKeyShortcutEvent(keyEvent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return this.mController.dispatchTouchEvent(motionEvent) || super.dispatchTouchEvent(motionEvent);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return this.mController.dispatchTrackballEvent(motionEvent) || super.dispatchTrackballEvent(motionEvent);
    }

    Controller getController() {
        return (Controller) this.mController;
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
        super.onActionModeFinished(actionMode);
        this.mController.onActionModeFinished(actionMode);
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        super.onActionModeStarted(actionMode);
        this.mController.onActionModeStarted(actionMode);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        this.mController.onActivityResult(i, i2, intent);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mController.onConfgurationChanged(configuration);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        return this.mController.onContextItemSelected(menuItem);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        this.mController.onContextMenuClosed(menu);
    }

    @Override
    public void onCreate(Bundle bundle) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append(this);
            sb.append(" onCreate, has state: ");
            sb.append(bundle == null ? "false" : "true");
            Log.v("browser", sb.toString());
        }
        super.onCreate(bundle);
        this.mAllGranted = false;
        PermissionHelper.init(this);
        if (isTablet(this)) {
            getWindow().setSoftInputMode(16);
        }
        handleSearchEngineChanged();
        this.mIntentFilter = new IntentFilter("com.mediatek.search.SEARCH_ENGINE_CHANGED");
        this.mIntentFilter.addAction("com.mediatek.common.carrierexpress.operator_config_changed");
        registerReceiver(this.mReceiver, this.mIntentFilter);
        if (shouldIgnoreIntents()) {
            finish();
        } else {
            if (IntentHandler.handleWebSearchIntent(this, null, getIntent())) {
                finish();
                return;
            }
            this.mController = createController();
            this.mController.start(bundle == null ? getIntent() : null);
            new Thread(new DeleteFailedDownload()).start();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        this.mController.onCreateContextMenu(contextMenu, view, contextMenuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return this.mController.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.v("browser", "BrowserActivity.onDestroy: this=" + this);
        }
        unregisterReceiver(this.mReceiver);
        super.onDestroy();
        this.mController.onDestroy();
        this.mController = NullController.INSTANCE;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return this.mController.onKeyDown(i, keyEvent) || super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return this.mController.onKeyLongPress(i, keyEvent) || super.onKeyLongPress(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return this.mController.onKeyUp(i, keyEvent) || super.onKeyUp(i, keyEvent);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        this.mController.onLowMemory();
    }

    @Override
    public boolean onMenuOpened(int i, Menu menu) {
        if (i != 0) {
            return true;
        }
        this.mController.onMenuOpened(i, menu);
        return true;
    }

    @Override
    public void onMultiWindowModeChanged(boolean z) {
        WindowManager.LayoutParams attributes;
        if (z || !isTablet(this) || (attributes = getWindow().getAttributes()) == null) {
            return;
        }
        attributes.flags |= 16777216;
        attributes.flags &= Integer.MIN_VALUE;
        getWindow().setAttributes(attributes);
        Log.d("browser", "BrowserActivity.onMultiWindowModeChanged");
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
            if (!"--restart--".equals(intent.getAction())) {
                this.mController.handleNewIntent(intent);
                return;
            }
            Bundle bundle = new Bundle();
            this.mController.onSaveInstanceState(bundle);
            finish();
            getApplicationContext().startActivity(new Intent(getApplicationContext(), (Class<?>) BrowserActivity.class).addFlags(268435456).putExtra("state", bundle));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mController.onOptionsItemSelected(menuItem)) {
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        this.mController.onOptionsMenuClosed(menu);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return this.mController.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (DEBUG) {
            Log.d("browser/BrowserActivity", " onRequestPermissionsResult " + i);
        }
        PermissionHelper.getInstance().onPermissionsResult(i, strArr, iArr);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mAllGranted) {
            doResume();
            return;
        }
        List<String> allUngrantedPermissions = PermissionHelper.getInstance().getAllUngrantedPermissions();
        if (allUngrantedPermissions.size() > 0) {
            PermissionHelper.getInstance().requestPermissions(allUngrantedPermissions, this.mPermissionCallback);
        } else {
            doResume();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        this.mController.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onSearchRequested() {
        return this.mController.onSearchRequested();
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
}
