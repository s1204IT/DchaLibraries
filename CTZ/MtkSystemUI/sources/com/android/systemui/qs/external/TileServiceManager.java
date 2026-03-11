package com.android.systemui.qs.external;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.util.Log;
import com.android.systemui.qs.external.TileLifecycleManager;
import java.util.Objects;

public class TileServiceManager {
    static final String PREFS_FILE = "CustomTileModes";
    private boolean mBindAllowed;
    private boolean mBindRequested;
    private boolean mBound;
    private final Handler mHandler;
    private boolean mJustBound;
    final Runnable mJustBoundOver;
    private long mLastUpdate;
    private boolean mPendingBind;
    private int mPriority;
    private final TileServices mServices;
    private boolean mShowingDialog;
    private final TileLifecycleManager mStateManager;
    private final Runnable mUnbind;
    private final BroadcastReceiver mUninstallReceiver;

    TileServiceManager(TileServices tileServices, Handler handler, ComponentName componentName, Tile tile) {
        this(tileServices, handler, new TileLifecycleManager(handler, tileServices.getContext(), tileServices, tile, new Intent().setComponent(componentName), new UserHandle(ActivityManager.getCurrentUser())));
    }

    TileServiceManager(TileServices tileServices, Handler handler, TileLifecycleManager tileLifecycleManager) {
        this.mPendingBind = true;
        this.mUnbind = new Runnable() {
            @Override
            public void run() {
                if (TileServiceManager.this.mBound && !TileServiceManager.this.mBindRequested) {
                    TileServiceManager.this.unbindService();
                }
            }
        };
        this.mJustBoundOver = new Runnable() {
            @Override
            public void run() {
                TileServiceManager.this.mJustBound = false;
                TileServiceManager.this.mServices.recalculateBindAllowance();
            }
        };
        this.mUninstallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!"android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                    return;
                }
                String encodedSchemeSpecificPart = intent.getData().getEncodedSchemeSpecificPart();
                ComponentName component = TileServiceManager.this.mStateManager.getComponent();
                if (!Objects.equals(encodedSchemeSpecificPart, component.getPackageName())) {
                    return;
                }
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    Intent intent2 = new Intent("android.service.quicksettings.action.QS_TILE");
                    intent2.setPackage(encodedSchemeSpecificPart);
                    for (ResolveInfo resolveInfo : context.getPackageManager().queryIntentServicesAsUser(intent2, 0, ActivityManager.getCurrentUser())) {
                        if (Objects.equals(resolveInfo.serviceInfo.packageName, component.getPackageName()) && Objects.equals(resolveInfo.serviceInfo.name, component.getClassName())) {
                            return;
                        }
                    }
                }
                TileServiceManager.this.mServices.getHost().removeTile(component);
            }
        };
        this.mServices = tileServices;
        this.mHandler = handler;
        this.mStateManager = tileLifecycleManager;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        Context context = this.mServices.getContext();
        context.registerReceiverAsUser(this.mUninstallReceiver, new UserHandle(ActivityManager.getCurrentUser()), intentFilter, null, this.mHandler);
        ComponentName component = tileLifecycleManager.getComponent();
        if (!TileLifecycleManager.isTileAdded(context, component)) {
            TileLifecycleManager.setTileAdded(context, component, true);
            this.mStateManager.onTileAdded();
            this.mStateManager.flushMessagesAndUnbind();
        }
    }

    public void setTileChangeListener(TileLifecycleManager.TileChangeListener tileChangeListener) {
        this.mStateManager.setTileChangeListener(tileChangeListener);
    }

    public boolean isActiveTile() {
        return this.mStateManager.isActiveTile();
    }

    public void setShowingDialog(boolean z) {
        this.mShowingDialog = z;
    }

    public IQSTileService getTileService() {
        return this.mStateManager;
    }

    public IBinder getToken() {
        return this.mStateManager.getToken();
    }

    public void setBindRequested(boolean z) {
        if (this.mBindRequested == z) {
            return;
        }
        this.mBindRequested = z;
        if (this.mBindAllowed && this.mBindRequested && !this.mBound) {
            this.mHandler.removeCallbacks(this.mUnbind);
            bindService();
        } else {
            this.mServices.recalculateBindAllowance();
        }
        if (this.mBound && !this.mBindRequested) {
            this.mHandler.postDelayed(this.mUnbind, 30000L);
        }
    }

    public void setLastUpdate(long j) {
        this.mLastUpdate = j;
        if (this.mBound && isActiveTile()) {
            this.mStateManager.onStopListening();
            setBindRequested(false);
        }
        this.mServices.recalculateBindAllowance();
    }

    public void handleDestroy() {
        setBindAllowed(false);
        this.mServices.getContext().unregisterReceiver(this.mUninstallReceiver);
        this.mStateManager.handleDestroy();
    }

    public void setBindAllowed(boolean z) {
        if (this.mBindAllowed == z) {
            return;
        }
        this.mBindAllowed = z;
        if (!this.mBindAllowed && this.mBound) {
            unbindService();
        } else if (this.mBindAllowed && this.mBindRequested && !this.mBound) {
            bindService();
        }
    }

    public boolean hasPendingBind() {
        return this.mPendingBind;
    }

    public void clearPendingBind() {
        this.mPendingBind = false;
    }

    private void bindService() {
        if (this.mBound) {
            Log.e("TileServiceManager", "Service already bound");
            return;
        }
        this.mPendingBind = true;
        this.mBound = true;
        this.mJustBound = true;
        this.mHandler.postDelayed(this.mJustBoundOver, 5000L);
        this.mStateManager.setBindService(true);
    }

    public void unbindService() {
        if (!this.mBound) {
            Log.e("TileServiceManager", "Service not bound");
            return;
        }
        this.mBound = false;
        this.mJustBound = false;
        this.mStateManager.setBindService(false);
    }

    public void calculateBindPriority(long j) {
        if (this.mStateManager.hasPendingClick()) {
            this.mPriority = Integer.MAX_VALUE;
            return;
        }
        if (this.mShowingDialog) {
            this.mPriority = 2147483646;
            return;
        }
        if (this.mJustBound) {
            this.mPriority = 2147483645;
            return;
        }
        if (!this.mBindRequested) {
            this.mPriority = Integer.MIN_VALUE;
            return;
        }
        long j2 = j - this.mLastUpdate;
        if (j2 > 2147483644) {
            this.mPriority = 2147483644;
        } else {
            this.mPriority = (int) j2;
        }
    }

    public int getBindPriority() {
        return this.mPriority;
    }
}
