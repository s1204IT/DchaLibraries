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

/* loaded from: classes.dex */
public class BrowserActivity extends Activity {
    private static final boolean DEBUG = Browser.ENGONLY;
    private static final String[] DELETE_WHERE_ARGS = {"100", "0"};
    private boolean mAllGranted;
    private IntentFilter mIntentFilter;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private ActivityController mController = NullController.INSTANCE;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.browser.BrowserActivity.1
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BrowserActivity.DEBUG) {
                Log.v("@M_browser", "mReceiver action = " + action);
            }
            if (action.equals("com.mediatek.search.SEARCH_ENGINE_CHANGED")) {
                BrowserActivity.this.handleSearchEngineChanged();
            }
            if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
                Extensions.resetPlugins();
            }
        }
    };
    private PermissionHelper.PermissionCallback mPermissionCallback = new PermissionHelper.PermissionCallback() { // from class: com.android.browser.BrowserActivity.2
        AnonymousClass2() {
        }

        @Override // com.android.browser.PermissionHelper.PermissionCallback
        public void onPermissionsResult(int i, String[] strArr, int[] iArr) {
            if (iArr != null && iArr.length > 0) {
                BrowserActivity.this.mAllGranted = true;
                int i2 = 0;
                while (true) {
                    if (i2 >= iArr.length) {
                        break;
                    }
                    if (iArr[i2] != 0) {
                        BrowserActivity.this.mAllGranted = false;
                        if (BrowserActivity.DEBUG) {
                            Log.d("browser/BrowserActivity", strArr[i2] + " is not granted !");
                        }
                    } else {
                        i2++;
                    }
                }
                if (!BrowserActivity.this.mAllGranted) {
                    Toast.makeText(BrowserActivity.this.getApplicationContext(), BrowserActivity.this.getString(R.string.denied_required_permission), 1).show();
                    BrowserActivity.this.finish();
                }
                BrowserActivity.this.doResume();
            }
        }
    };

    /* renamed from: com.android.browser.BrowserActivity$1 */
    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BrowserActivity.DEBUG) {
                Log.v("@M_browser", "mReceiver action = " + action);
            }
            if (action.equals("com.mediatek.search.SEARCH_ENGINE_CHANGED")) {
                BrowserActivity.this.handleSearchEngineChanged();
            }
            if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
                Extensions.resetPlugins();
            }
        }
    }

    private void handleSearchEngineChanged() {
        String searchEngineName = BrowserSettings.getInstance().getSearchEngineName();
        if (DEBUG) {
            Log.v("@M_browser", "ChangeSearchEngineReceiver (search): search_engine---" + searchEngineName);
        }
    }

    private class DeleteFailedDownload implements Runnable {
        private DeleteFailedDownload() {
        }

        /* synthetic */ DeleteFailedDownload(BrowserActivity browserActivity, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // java.lang.Runnable
        public void run() {
            BrowserActivity.this.getContentResolver().delete(SnapshotProvider.Snapshots.CONTENT_URI, "progress < ? AND is_done = ?", BrowserActivity.DELETE_WHERE_ARGS);
        }
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.mController.onPause();
        super.onStop();
    }

    @Override // android.app.Activity
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

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    private Controller createController() {
        UI phoneUi;
        Controller controller = new Controller(this);
        if (isTablet(this)) {
            phoneUi = new XLargeUi(this, controller);
        } else {
            phoneUi = new PhoneUi(this, controller);
        }
        controller.setUi(phoneUi);
        return controller;
    }

    Controller getController() {
        return (Controller) this.mController;
    }

    @Override // android.app.Activity
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
                Bundle bundle = new Bundle();
                this.mController.onSaveInstanceState(bundle);
                finish();
                getApplicationContext().startActivity(new Intent(getApplicationContext(), (Class<?>) BrowserActivity.class).addFlags(268435456).putExtra("state", bundle));
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
        boolean z = !this.mPowerManager.isScreenOn();
        Log.v("browser", "ignore intents: " + z);
        return z;
    }

    @Override // android.app.Activity
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

    private void doResume() {
        if (DEBUG) {
            Log.v("browser", "BrowserActivity.onResume: this=" + this);
        }
        this.mController.onResume();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean onMenuOpened(int i, Menu menu) {
        if (i == 0) {
            this.mController.onMenuOpened(i, menu);
            return true;
        }
        return true;
    }

    @Override // android.app.Activity
    public void onOptionsMenuClosed(Menu menu) {
        this.mController.onOptionsMenuClosed(menu);
    }

    @Override // android.app.Activity
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        this.mController.onContextMenuClosed(menu);
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        this.mController.onSaveInstanceState(bundle);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (DEBUG) {
            Log.v("browser", "BrowserActivity.onDestroy: this=" + this);
        }
        unregisterReceiver(this.mReceiver);
        super.onDestroy();
        this.mController.onDestroy();
        this.mController = NullController.INSTANCE;
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mController.onConfgurationChanged(configuration);
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onLowMemory() {
        super.onLowMemory();
        this.mController.onLowMemory();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return this.mController.onCreateOptionsMenu(menu);
    }

    @Override // android.app.Activity
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return this.mController.onPrepareOptionsMenu(menu);
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (!this.mController.onOptionsItemSelected(menuItem)) {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    @Override // android.app.Activity, android.view.View.OnCreateContextMenuListener
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        this.mController.onCreateContextMenu(contextMenu, view, contextMenuInfo);
    }

    @Override // android.app.Activity
    public boolean onContextItemSelected(MenuItem menuItem) {
        return this.mController.onContextItemSelected(menuItem);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return this.mController.onKeyDown(i, keyEvent) || super.onKeyDown(i, keyEvent);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return this.mController.onKeyLongPress(i, keyEvent) || super.onKeyLongPress(i, keyEvent);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return this.mController.onKeyUp(i, keyEvent) || super.onKeyUp(i, keyEvent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onActionModeStarted(ActionMode actionMode) {
        super.onActionModeStarted(actionMode);
        this.mController.onActionModeStarted(actionMode);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onActionModeFinished(ActionMode actionMode) {
        super.onActionModeFinished(actionMode);
        this.mController.onActionModeFinished(actionMode);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        this.mController.onActivityResult(i, i2, intent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean onSearchRequested() {
        return this.mController.onSearchRequested();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return this.mController.dispatchKeyEvent(keyEvent) || super.dispatchKeyEvent(keyEvent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return this.mController.dispatchKeyShortcutEvent(keyEvent) || super.dispatchKeyShortcutEvent(keyEvent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return this.mController.dispatchTouchEvent(motionEvent) || super.dispatchTouchEvent(motionEvent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return this.mController.dispatchTrackballEvent(motionEvent) || super.dispatchTrackballEvent(motionEvent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return this.mController.dispatchGenericMotionEvent(motionEvent) || super.dispatchGenericMotionEvent(motionEvent);
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (DEBUG) {
            Log.d("browser/BrowserActivity", " onRequestPermissionsResult " + i);
        }
        PermissionHelper.getInstance().onPermissionsResult(i, strArr, iArr);
    }

    /* renamed from: com.android.browser.BrowserActivity$2 */
    class AnonymousClass2 implements PermissionHelper.PermissionCallback {
        AnonymousClass2() {
        }

        @Override // com.android.browser.PermissionHelper.PermissionCallback
        public void onPermissionsResult(int i, String[] strArr, int[] iArr) {
            if (iArr != null && iArr.length > 0) {
                BrowserActivity.this.mAllGranted = true;
                int i2 = 0;
                while (true) {
                    if (i2 >= iArr.length) {
                        break;
                    }
                    if (iArr[i2] != 0) {
                        BrowserActivity.this.mAllGranted = false;
                        if (BrowserActivity.DEBUG) {
                            Log.d("browser/BrowserActivity", strArr[i2] + " is not granted !");
                        }
                    } else {
                        i2++;
                    }
                }
                if (!BrowserActivity.this.mAllGranted) {
                    Toast.makeText(BrowserActivity.this.getApplicationContext(), BrowserActivity.this.getString(R.string.denied_required_permission), 1).show();
                    BrowserActivity.this.finish();
                }
                BrowserActivity.this.doResume();
            }
        }
    }

    @Override // android.app.Activity
    public void onMultiWindowModeChanged(boolean z) {
        WindowManager.LayoutParams attributes;
        if (!z && isTablet(this) && (attributes = getWindow().getAttributes()) != null) {
            attributes.flags |= 16777216;
            attributes.flags &= Integer.MIN_VALUE;
            getWindow().setAttributes(attributes);
            Log.d("browser", "BrowserActivity.onMultiWindowModeChanged");
        }
    }
}
