package com.android.systemui.qs.external;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.external.TileLifecycleManager;
import com.android.systemui.statusbar.phone.QSTileHost;
import libcore.util.Objects;

public class CustomTile extends QSTile<QSTile.State> implements TileLifecycleManager.TileChangeListener {
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

    private CustomTile(QSTileHost host, String action) {
        super(host);
        this.mToken = new Binder();
        this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        this.mComponent = ComponentName.unflattenFromString(action);
        this.mTile = new Tile(this.mComponent);
        setTileIcon();
        this.mServiceManager = host.getTileServices().getTileWrapper(this);
        this.mService = this.mServiceManager.getTileService();
        this.mServiceManager.setTileChangeListener(this);
        this.mUser = ActivityManager.getCurrentUser();
    }

    private void setTileIcon() {
        boolean zIconEquals;
        try {
            PackageManager pm = this.mContext.getPackageManager();
            ServiceInfo info = pm.getServiceInfo(this.mComponent, 786432);
            int icon = info.icon != 0 ? info.icon : info.applicationInfo.icon;
            if (this.mTile.getIcon() == null) {
                zIconEquals = true;
            } else {
                zIconEquals = iconEquals(this.mTile.getIcon(), this.mDefaultIcon);
            }
            this.mDefaultIcon = icon != 0 ? Icon.createWithResource(this.mComponent.getPackageName(), icon) : null;
            if (zIconEquals) {
                this.mTile.setIcon(this.mDefaultIcon);
            }
            if (this.mTile.getLabel() != null) {
                return;
            }
            this.mTile.setLabel(info.loadLabel(pm));
        } catch (Exception e) {
            this.mDefaultIcon = null;
        }
    }

    private boolean iconEquals(Icon icon1, Icon icon2) {
        if (icon1 == icon2) {
            return true;
        }
        return icon1 != null && icon2 != null && icon1.getType() == 2 && icon2.getType() == 2 && icon1.getResId() == icon2.getResId() && Objects.equal(icon1.getResPackage(), icon2.getResPackage());
    }

    @Override
    public void onTileChanged(ComponentName tile) {
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
            this.mWindowManager.removeWindowToken(this.mToken);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        try {
            if (listening) {
                setTileIcon();
                refreshState();
                if (this.mServiceManager.isActiveTile()) {
                    return;
                }
                this.mServiceManager.setBindRequested(true);
                this.mService.onStartListening();
                return;
            }
            this.mService.onStopListening();
            if (this.mIsTokenGranted && !this.mIsShowingDialog) {
                try {
                    this.mWindowManager.removeWindowToken(this.mToken);
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
                this.mWindowManager.removeWindowToken(this.mToken);
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
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent i = new Intent("android.service.quicksettings.action.QS_TILE_PREFERENCES");
        i.setPackage(this.mComponent.getPackageName());
        Intent i2 = resolveIntent(i);
        if (i2 != null) {
            return i2;
        }
        return new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", this.mComponent.getPackageName(), null));
    }

    private Intent resolveIntent(Intent i) {
        ResolveInfo result = this.mContext.getPackageManager().resolveActivityAsUser(i, 0, ActivityManager.getCurrentUser());
        if (result != null) {
            return new Intent("android.service.quicksettings.action.QS_TILE_PREFERENCES").setClassName(result.activityInfo.packageName, result.activityInfo.name);
        }
        return null;
    }

    @Override
    protected void handleClick() {
        if (this.mTile.getState() == 0) {
            return;
        }
        try {
            this.mWindowManager.addWindowToken(this.mToken, 2035);
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
        MetricsLogger.action(this.mContext, getMetricsCategory(), this.mComponent.getPackageName());
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object arg) {
        Drawable drawable;
        int tileState = this.mTile.getState();
        if (this.mServiceManager.hasPendingBind()) {
            tileState = 0;
        }
        try {
            drawable = this.mTile.getIcon().loadDrawable(this.mContext);
        } catch (Exception e) {
            Log.w(this.TAG, "Invalid icon, forcing into unavailable state");
            tileState = 0;
            drawable = this.mDefaultIcon.loadDrawable(this.mContext);
        }
        int color = this.mContext.getColor(getColor(tileState));
        drawable.setTint(color);
        state.icon = new QSTile.DrawableIcon(drawable);
        state.label = this.mTile.getLabel();
        if (tileState == 0) {
            state.label = new SpannableStringBuilder().append(state.label, new ForegroundColorSpan(color), 18);
        }
        if (this.mTile.getContentDescription() != null) {
            state.contentDescription = this.mTile.getContentDescription();
        } else {
            state.contentDescription = state.label;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 268;
    }

    public void startUnlockAndRun() {
        this.mHost.startRunnableDismissingKeyguard(new Runnable() {
            @Override
            public void run() {
                try {
                    CustomTile.this.mService.onUnlockComplete();
                } catch (RemoteException e) {
                }
            }
        });
    }

    private static int getColor(int state) {
        switch (state) {
            case 0:
                return R.color.qs_tile_tint_unavailable;
            case 1:
                return R.color.qs_tile_tint_inactive;
            case 2:
                return R.color.qs_tile_tint_active;
            default:
                return 0;
        }
    }

    public static String toSpec(ComponentName name) {
        return "custom(" + name.flattenToShortString() + ")";
    }

    public static ComponentName getComponentFromSpec(String spec) {
        String action = spec.substring("custom(".length(), spec.length() - 1);
        if (action.isEmpty()) {
            throw new IllegalArgumentException("Empty custom tile spec action");
        }
        return ComponentName.unflattenFromString(action);
    }

    public static QSTile<?> create(QSTileHost host, String spec) {
        if (spec == null || !spec.startsWith("custom(") || !spec.endsWith(")")) {
            throw new IllegalArgumentException("Bad custom tile spec: " + spec);
        }
        String action = spec.substring("custom(".length(), spec.length() - 1);
        if (action.isEmpty()) {
            throw new IllegalArgumentException("Empty custom tile spec action");
        }
        return new CustomTile(host, action);
    }
}
