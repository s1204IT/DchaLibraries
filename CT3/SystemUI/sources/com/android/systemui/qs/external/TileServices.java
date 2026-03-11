package com.android.systemui.qs.external;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.quicksettings.IQSService;
import android.service.quicksettings.Tile;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TileServices extends IQSService.Stub {
    private static final Comparator<TileServiceManager> SERVICE_SORT = new Comparator<TileServiceManager>() {
        @Override
        public int compare(TileServiceManager left, TileServiceManager right) {
            return -Integer.compare(left.getBindPriority(), right.getBindPriority());
        }
    };
    private final Context mContext;
    private final Handler mHandler;
    private final QSTileHost mHost;
    private final Handler mMainHandler;
    private final ArrayMap<CustomTile, TileServiceManager> mServices = new ArrayMap<>();
    private final ArrayMap<ComponentName, CustomTile> mTiles = new ArrayMap<>();
    private int mMaxBound = 3;
    private final BroadcastReceiver mRequestListeningReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.service.quicksettings.action.REQUEST_LISTENING".equals(intent.getAction())) {
                return;
            }
            TileServices.this.requestListening((ComponentName) intent.getParcelableExtra("android.service.quicksettings.extra.COMPONENT"));
        }
    };

    public TileServices(QSTileHost host, Looper looper) {
        this.mHost = host;
        this.mContext = this.mHost.getContext();
        this.mContext.registerReceiver(this.mRequestListeningReceiver, new IntentFilter("android.service.quicksettings.action.REQUEST_LISTENING"));
        this.mHandler = new Handler(looper);
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    public Context getContext() {
        return this.mContext;
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public TileServiceManager getTileWrapper(CustomTile tile) {
        ComponentName component = tile.getComponent();
        TileServiceManager service = onCreateTileService(component, tile.getQsTile());
        synchronized (this.mServices) {
            this.mServices.put(tile, service);
            this.mTiles.put(component, tile);
        }
        return service;
    }

    protected TileServiceManager onCreateTileService(ComponentName component, Tile tile) {
        return new TileServiceManager(this, this.mHandler, component, tile);
    }

    public void freeService(CustomTile tile, TileServiceManager service) {
        synchronized (this.mServices) {
            service.setBindAllowed(false);
            service.handleDestroy();
            this.mServices.remove(tile);
            this.mTiles.remove(tile.getComponent());
            final String slot = tile.getComponent().getClassName();
            this.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    TileServices.this.mHost.getIconController().removeIcon(slot);
                }
            });
        }
    }

    public void recalculateBindAllowance() {
        ArrayList<TileServiceManager> services;
        synchronized (this.mServices) {
            services = new ArrayList<>(this.mServices.values());
        }
        int N = services.size();
        if (N > this.mMaxBound) {
            long currentTime = System.currentTimeMillis();
            for (int i = 0; i < N; i++) {
                services.get(i).calculateBindPriority(currentTime);
            }
            Collections.sort(services, SERVICE_SORT);
        }
        int i2 = 0;
        while (i2 < this.mMaxBound && i2 < N) {
            services.get(i2).setBindAllowed(true);
            i2++;
        }
        while (i2 < N) {
            services.get(i2).setBindAllowed(false);
            i2++;
        }
    }

    private void verifyCaller(String packageName) {
        try {
            int uid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, Binder.getCallingUserHandle().getIdentifier());
            if (Binder.getCallingUid() == uid) {
            } else {
                throw new SecurityException("Component outside caller's uid");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException(e);
        }
    }

    public void requestListening(ComponentName component) {
        synchronized (this.mServices) {
            CustomTile customTile = getTileForComponent(component);
            if (customTile == null) {
                Log.d("TileServices", "Couldn't find tile for " + component);
                return;
            }
            TileServiceManager service = this.mServices.get(customTile);
            if (!service.isActiveTile()) {
                return;
            }
            service.setBindRequested(true);
            try {
                service.getTileService().onStartListening();
            } catch (RemoteException e) {
            }
        }
    }

    public void updateQsTile(Tile tile) {
        ComponentName componentName = tile.getComponentName();
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        synchronized (this.mServices) {
            TileServiceManager tileServiceManager = this.mServices.get(customTile);
            tileServiceManager.clearPendingBind();
            tileServiceManager.setLastUpdate(System.currentTimeMillis());
        }
        customTile.updateState(tile);
        customTile.refreshState();
    }

    public void onStartSuccessful(Tile tile) {
        ComponentName componentName = tile.getComponentName();
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        synchronized (this.mServices) {
            TileServiceManager tileServiceManager = this.mServices.get(customTile);
            tileServiceManager.clearPendingBind();
        }
        customTile.refreshState();
    }

    public void onShowDialog(Tile tile) {
        ComponentName componentName = tile.getComponentName();
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        customTile.onDialogShown();
        this.mHost.collapsePanels();
        this.mServices.get(customTile).setShowingDialog(true);
    }

    public void onDialogHidden(Tile tile) {
        ComponentName componentName = tile.getComponentName();
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        this.mServices.get(customTile).setShowingDialog(false);
        customTile.onDialogHidden();
    }

    public void onStartActivity(Tile tile) {
        ComponentName componentName = tile.getComponentName();
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        this.mHost.collapsePanels();
    }

    public void updateStatusIcon(Tile tile, Icon icon, String contentDescription) {
        final StatusBarIcon statusBarIcon;
        final ComponentName componentName = tile.getComponentName();
        String packageName = componentName.getPackageName();
        verifyCaller(packageName);
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        try {
            UserHandle userHandle = getCallingUserHandle();
            PackageInfo info = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 0, userHandle.getIdentifier());
            if (!info.applicationInfo.isSystemApp()) {
                return;
            }
            if (icon != null) {
                statusBarIcon = new StatusBarIcon(userHandle, packageName, icon, 0, 0, contentDescription);
            } else {
                statusBarIcon = null;
            }
            this.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    StatusBarIconController iconController = TileServices.this.mHost.getIconController();
                    iconController.setIcon(componentName.getClassName(), statusBarIcon);
                    iconController.setExternalIcon(componentName.getClassName());
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    public Tile getTile(ComponentName componentName) {
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile != null) {
            return customTile.getQsTile();
        }
        return null;
    }

    public void startUnlockAndRun(Tile tile) {
        ComponentName componentName = tile.getComponentName();
        verifyCaller(componentName.getPackageName());
        CustomTile customTile = getTileForComponent(componentName);
        if (customTile == null) {
            return;
        }
        customTile.startUnlockAndRun();
    }

    public boolean isLocked() {
        KeyguardMonitor keyguardMonitor = this.mHost.getKeyguardMonitor();
        return keyguardMonitor.isShowing();
    }

    public boolean isSecure() {
        KeyguardMonitor keyguardMonitor = this.mHost.getKeyguardMonitor();
        if (keyguardMonitor.isSecure()) {
            return keyguardMonitor.isShowing();
        }
        return false;
    }

    private CustomTile getTileForComponent(ComponentName component) {
        CustomTile customTile;
        synchronized (this.mServices) {
            customTile = this.mTiles.get(component);
        }
        return customTile;
    }
}
