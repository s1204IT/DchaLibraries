package com.android.systemui.qs.external;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.TileLifecycleManager;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.util.Objects;
import java.util.function.Supplier;

public class CustomTile extends QSTileImpl<QSTile.State> implements TileLifecycleManager.TileChangeListener {
    private final ComponentName mComponent;
    private Icon mDefaultIcon;
    private boolean mIsShowingDialog;
    private boolean mIsTokenGranted;
    private boolean mListening;
    private final IQSTileService mService;
    private final TileServiceManager mServiceManager;
    private final Tile mTile;
    private final IBinder mToken;
    private final int mUser;
    private final IWindowManager mWindowManager;

    private CustomTile(QSTileHost qSTileHost, String str) {
        super(qSTileHost);
        this.mToken = new Binder();
        this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        this.mComponent = ComponentName.unflattenFromString(str);
        this.mTile = new Tile();
        setTileIcon();
        this.mServiceManager = qSTileHost.getTileServices().getTileWrapper(this);
        this.mService = this.mServiceManager.getTileService();
        this.mServiceManager.setTileChangeListener(this);
        this.mUser = ActivityManager.getCurrentUser();
    }

    @Override
    protected long getStaleTimeout() {
        return 3600000 + (60000 * ((long) this.mHost.indexOf(getTileSpec())));
    }

    private void setTileIcon() {
        Icon iconCreateWithResource;
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            int i = 786432;
            if (isSystemApp(packageManager)) {
                i = 786944;
            }
            ServiceInfo serviceInfo = packageManager.getServiceInfo(this.mComponent, i);
            int i2 = serviceInfo.icon != 0 ? serviceInfo.icon : serviceInfo.applicationInfo.icon;
            boolean z = this.mTile.getIcon() == null || iconEquals(this.mTile.getIcon(), this.mDefaultIcon);
            if (i2 == 0) {
                iconCreateWithResource = null;
            } else {
                iconCreateWithResource = Icon.createWithResource(this.mComponent.getPackageName(), i2);
            }
            this.mDefaultIcon = iconCreateWithResource;
            if (z) {
                this.mTile.setIcon(this.mDefaultIcon);
            }
            if (this.mTile.getLabel() == null) {
                this.mTile.setLabel(serviceInfo.loadLabel(packageManager));
            }
        } catch (Exception e) {
            this.mDefaultIcon = null;
        }
    }

    private boolean isSystemApp(PackageManager packageManager) throws PackageManager.NameNotFoundException {
        return packageManager.getApplicationInfo(this.mComponent.getPackageName(), 0).isSystemApp();
    }

    private boolean iconEquals(Icon icon, Icon icon2) {
        if (icon == icon2) {
            return true;
        }
        if (icon != null && icon2 != null && icon.getType() == 2 && icon2.getType() == 2 && icon.getResId() == icon2.getResId() && Objects.equals(icon.getResPackage(), icon2.getResPackage())) {
            return true;
        }
        return false;
    }

    @Override
    public void onTileChanged(ComponentName componentName) {
        setTileIcon();
    }

    @Override
    public boolean isAvailable() {
        return this.mDefaultIcon != null;
    }

    public int getUser() {
        return this.mUser;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    @Override
    public LogMaker populate(LogMaker logMaker) {
        return super.populate(logMaker).setComponentName(this.mComponent);
    }

    public Tile getQsTile() {
        return this.mTile;
    }

    public void updateState(Tile tile) {
        this.mTile.setIcon(tile.getIcon());
        this.mTile.setLabel(tile.getLabel());
        this.mTile.setContentDescription(tile.getContentDescription());
        this.mTile.setState(tile.getState());
    }

    public void onDialogShown() {
        this.mIsShowingDialog = true;
    }

    public void onDialogHidden() {
        this.mIsShowingDialog = false;
        try {
            this.mWindowManager.removeWindowToken(this.mToken, 0);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void handleSetListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        try {
            if (z) {
                setTileIcon();
                refreshState();
                if (!this.mServiceManager.isActiveTile()) {
                    this.mServiceManager.setBindRequested(true);
                    this.mService.onStartListening();
                    return;
                }
                return;
            }
            this.mService.onStopListening();
            if (this.mIsTokenGranted && !this.mIsShowingDialog) {
                try {
                    this.mWindowManager.removeWindowToken(this.mToken, 0);
                } catch (RemoteException e) {
                }
                this.mIsTokenGranted = false;
            }
            this.mIsShowingDialog = false;
            this.mServiceManager.setBindRequested(false);
        } catch (RemoteException e2) {
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        if (this.mIsTokenGranted) {
            try {
                this.mWindowManager.removeWindowToken(this.mToken, 0);
            } catch (RemoteException e) {
            }
        }
        this.mHost.getTileServices().freeService(this, this.mServiceManager);
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    public Intent getLongClickIntent() {
        Intent intent = new Intent("android.service.quicksettings.action.QS_TILE_PREFERENCES");
        intent.setPackage(this.mComponent.getPackageName());
        Intent intentResolveIntent = resolveIntent(intent);
        if (intentResolveIntent != null) {
            intentResolveIntent.putExtra("android.intent.extra.COMPONENT_NAME", this.mComponent);
            intentResolveIntent.putExtra("state", this.mTile.getState());
            return intentResolveIntent;
        }
        return new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", this.mComponent.getPackageName(), null));
    }

    private Intent resolveIntent(Intent intent) {
        ResolveInfo resolveInfoResolveActivityAsUser = this.mContext.getPackageManager().resolveActivityAsUser(intent, 0, ActivityManager.getCurrentUser());
        if (resolveInfoResolveActivityAsUser != null) {
            return new Intent("android.service.quicksettings.action.QS_TILE_PREFERENCES").setClassName(resolveInfoResolveActivityAsUser.activityInfo.packageName, resolveInfoResolveActivityAsUser.activityInfo.name);
        }
        return null;
    }

    @Override
    protected void handleClick() {
        if (this.mTile.getState() == 0) {
            return;
        }
        try {
            this.mWindowManager.addWindowToken(this.mToken, 2035, 0);
            this.mIsTokenGranted = true;
        } catch (RemoteException e) {
        }
        try {
            if (this.mServiceManager.isActiveTile()) {
                this.mServiceManager.setBindRequested(true);
                this.mService.onStartListening();
            }
            this.mService.onClick(this.mToken);
        } catch (RemoteException e2) {
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object obj) {
        final Drawable drawableLoadDrawable;
        int state2 = this.mTile.getState();
        if (this.mServiceManager.hasPendingBind()) {
            state2 = 0;
        }
        state.state = state2;
        try {
            drawableLoadDrawable = this.mTile.getIcon().loadDrawable(this.mContext);
        } catch (Exception e) {
            Log.w(this.TAG, "Invalid icon, forcing into unavailable state");
            state.state = 0;
            drawableLoadDrawable = this.mDefaultIcon.loadDrawable(this.mContext);
        }
        state.iconSupplier = new Supplier() {
            @Override
            public final Object get() {
                return CustomTile.lambda$handleUpdateState$0(drawableLoadDrawable);
            }
        };
        state.label = this.mTile.getLabel();
        if (this.mTile.getContentDescription() != null) {
            state.contentDescription = this.mTile.getContentDescription();
        } else {
            state.contentDescription = state.label;
        }
    }

    static QSTile.Icon lambda$handleUpdateState$0(Drawable drawable) {
        Drawable.ConstantState constantState = drawable.getConstantState();
        if (constantState != null) {
            return new QSTileImpl.DrawableIcon(constantState.newDrawable());
        }
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return 268;
    }

    public void startUnlockAndRun() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mService.onUnlockComplete();
            }
        });
    }

    public static String toSpec(ComponentName componentName) {
        return "custom(" + componentName.flattenToShortString() + ")";
    }

    public static ComponentName getComponentFromSpec(String str) {
        String strSubstring = str.substring("custom(".length(), str.length() - 1);
        if (strSubstring.isEmpty()) {
            throw new IllegalArgumentException("Empty custom tile spec action");
        }
        return ComponentName.unflattenFromString(strSubstring);
    }

    public static CustomTile create(QSTileHost qSTileHost, String str) {
        if (str == null || !str.startsWith("custom(") || !str.endsWith(")")) {
            throw new IllegalArgumentException("Bad custom tile spec: " + str);
        }
        String strSubstring = str.substring("custom(".length(), str.length() - 1);
        if (strSubstring.isEmpty()) {
            throw new IllegalArgumentException("Empty custom tile spec action");
        }
        return new CustomTile(qSTileHost, strSubstring);
    }
}
