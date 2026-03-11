package com.android.systemui.qs.external;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.UserHandle;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.systemui.qs.external.TileLifecycleManager;
import java.util.ArrayList;
import java.util.List;
import libcore.util.Objects;

public class TileServiceManager {

    @VisibleForTesting
    static final String PREFS_FILE = "CustomTileModes";
    private boolean mBindAllowed;
    private boolean mBindRequested;
    private boolean mBound;
    private final Handler mHandler;
    private boolean mJustBound;

    @VisibleForTesting
    final Runnable mJustBoundOver;
    private long mLastUpdate;
    private boolean mPendingBind;
    private int mPriority;
    private final TileServices mServices;
    private boolean mShowingDialog;
    private final TileLifecycleManager mStateManager;
    private final Runnable mUnbind;
    private final BroadcastReceiver mUninstallReceiver;

    TileServiceManager(TileServices tileServices, Handler handler, ComponentName component, Tile tile) {
        this(tileServices, handler, new TileLifecycleManager(handler, tileServices.getContext(), tileServices, tile, new Intent().setComponent(component), new UserHandle(ActivityManager.getCurrentUser())));
    }

    @VisibleForTesting
    TileServiceManager(TileServices tileServices, Handler handler, TileLifecycleManager tileLifecycleManager) {
        this.mPendingBind = true;
        this.mUnbind = new Runnable() {
            @Override
            public void run() {
                if (!TileServiceManager.this.mBound || TileServiceManager.this.mBindRequested) {
                    return;
                }
                TileServiceManager.this.unbindService();
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
                Uri data = intent.getData();
                String pkgName = data.getEncodedSchemeSpecificPart();
                ComponentName component = TileServiceManager.this.mStateManager.getComponent();
                if (!Objects.equal(pkgName, component.getPackageName())) {
                    return;
                }
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    Intent queryIntent = new Intent("android.service.quicksettings.action.QS_TILE");
                    queryIntent.setPackage(pkgName);
                    PackageManager pm = context.getPackageManager();
                    List<ResolveInfo> services = pm.queryIntentServicesAsUser(queryIntent, 0, ActivityManager.getCurrentUser());
                    if (BenesseExtension.getDchaState() != 0) {
                        services = new ArrayList<>();
                    }
                    for (ResolveInfo info : services) {
                        if (Objects.equal(info.serviceInfo.packageName, component.getPackageName()) && Objects.equal(info.serviceInfo.name, component.getClassName())) {
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
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mServices.getContext().registerReceiverAsUser(this.mUninstallReceiver, new UserHandle(ActivityManager.getCurrentUser()), filter, null, this.mHandler);
    }

    public void setTileChangeListener(TileLifecycleManager.TileChangeListener changeListener) {
        this.mStateManager.setTileChangeListener(changeListener);
    }

    public boolean isActiveTile() {
        return this.mStateManager.isActiveTile();
    }

    public void setShowingDialog(boolean dialog) {
        this.mShowingDialog = dialog;
    }

    public IQSTileService getTileService() {
        return this.mStateManager;
    }

    public void setBindRequested(boolean bindRequested) {
        if (this.mBindRequested == bindRequested) {
            return;
        }
        this.mBindRequested = bindRequested;
        if (this.mBindAllowed && this.mBindRequested && !this.mBound) {
            this.mHandler.removeCallbacks(this.mUnbind);
            bindService();
        } else {
            this.mServices.recalculateBindAllowance();
        }
        if (!this.mBound || this.mBindRequested) {
            return;
        }
        this.mHandler.postDelayed(this.mUnbind, 30000L);
    }

    public void setLastUpdate(long lastUpdate) {
        this.mLastUpdate = lastUpdate;
        if (this.mBound && isActiveTile()) {
            this.mStateManager.onStopListening();
            setBindRequested(false);
        }
        this.mServices.recalculateBindAllowance();
    }

    public void handleDestroy() {
        this.mServices.getContext().unregisterReceiver(this.mUninstallReceiver);
        this.mStateManager.handleDestroy();
    }

    public void setBindAllowed(boolean allowed) {
        if (this.mBindAllowed == allowed) {
            return;
        }
        this.mBindAllowed = allowed;
        if (!this.mBindAllowed && this.mBound) {
            unbindService();
        } else {
            if (!this.mBindAllowed || !this.mBindRequested || this.mBound) {
                return;
            }
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

    public void calculateBindPriority(long currentTime) {
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
        long timeSinceUpdate = currentTime - this.mLastUpdate;
        if (timeSinceUpdate > 2147483644) {
            this.mPriority = 2147483644;
        } else {
            this.mPriority = (int) timeSinceUpdate;
        }
    }

    public int getBindPriority() {
        return this.mPriority;
    }
}
