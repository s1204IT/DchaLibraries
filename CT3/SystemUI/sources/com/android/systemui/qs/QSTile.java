package com.android.systemui.qs;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.qs.QSTile.State;
import com.android.systemui.qs.external.TileServices;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NightModeController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.mediatek.systemui.statusbar.policy.HotKnotController;
import java.util.ArrayList;
import java.util.Objects;

public abstract class QSTile<TState extends State> {
    protected static final boolean DEBUG = Log.isLoggable("Tile", 3);
    private boolean mAnnounceNextStateChange;
    protected final Context mContext;
    protected final QSTile<TState>.H mHandler;
    protected final Host mHost;
    private String mTileSpec;
    protected final String TAG = "Tile." + getClass().getSimpleName();
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final ArraySet<Object> mListeners = new ArraySet<>();
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    protected TState mState = (TState) newTileState();
    private TState mTmpState = (TState) newTileState();

    public interface Callback {
        void onAnnouncementRequested(CharSequence charSequence);

        void onScanStateChanged(boolean z);

        void onShowDetail(boolean z);

        void onStateChanged(State state);

        void onToggleStateChanged(boolean z);
    }

    public interface DetailAdapter {
        View createDetailView(Context context, View view, ViewGroup viewGroup);

        int getMetricsCategory();

        Intent getSettingsIntent();

        CharSequence getTitle();

        Boolean getToggleState();

        void setToggleState(boolean z);
    }

    public interface Host {

        public interface Callback {
            void onTilesChanged();
        }

        void collapsePanels();

        BatteryController getBatteryController();

        BluetoothController getBluetoothController();

        CastController getCastController();

        Context getContext();

        FlashlightController getFlashlightController();

        HotKnotController getHotKnotController();

        HotspotController getHotspotController();

        KeyguardMonitor getKeyguardMonitor();

        LocationController getLocationController();

        Looper getLooper();

        ManagedProfileController getManagedProfileController();

        NetworkController getNetworkController();

        NightModeController getNightModeController();

        RotationLockController getRotationLockController();

        TileServices getTileServices();

        UserInfoController getUserInfoController();

        UserSwitcherController getUserSwitcherController();

        ZenModeController getZenModeController();

        void openPanels();

        void removeTile(String str);

        void startActivityDismissingKeyguard(PendingIntent pendingIntent);

        void startActivityDismissingKeyguard(Intent intent);

        void startRunnableDismissingKeyguard(Runnable runnable);

        void warn(String str, Throwable th);
    }

    public abstract Intent getLongClickIntent();

    public abstract int getMetricsCategory();

    public abstract CharSequence getTileLabel();

    protected abstract void handleClick();

    protected abstract void handleUpdateState(TState tstate, Object obj);

    public abstract TState newTileState();

    protected abstract void setListening(boolean z);

    protected QSTile(Host host) {
        this.mHost = host;
        this.mContext = host.getContext();
        this.mHandler = new H(this, host.getLooper(), null);
    }

    public void setListening(Object listener, boolean listening) {
        if (listening) {
            if (!this.mListeners.add(listener) || this.mListeners.size() != 1) {
                return;
            }
            if (DEBUG) {
                Log.d(this.TAG, "setListening true");
            }
            this.mHandler.obtainMessage(14, 1, 0).sendToTarget();
            return;
        }
        if (!this.mListeners.remove(listener) || this.mListeners.size() != 0) {
            return;
        }
        if (DEBUG) {
            Log.d(this.TAG, "setListening false");
        }
        this.mHandler.obtainMessage(14, 0, 0).sendToTarget();
    }

    public String getTileSpec() {
        return this.mTileSpec;
    }

    public void setTileSpec(String tileSpec) {
        this.mTileSpec = tileSpec;
    }

    public Host getHost() {
        return this.mHost;
    }

    public QSIconView createTileView(Context context) {
        return new QSIconView(context);
    }

    public DetailAdapter getDetailAdapter() {
        return null;
    }

    public boolean isAvailable() {
        return true;
    }

    public void addCallback(Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
    }

    public void removeCallback(Callback callback) {
        this.mHandler.obtainMessage(13, callback).sendToTarget();
    }

    public void removeCallbacks() {
        this.mHandler.sendEmptyMessage(12);
    }

    public void click() {
        this.mHandler.sendEmptyMessage(2);
    }

    public void secondaryClick() {
        this.mHandler.sendEmptyMessage(3);
    }

    public void longClick() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void showDetail(boolean show) {
        this.mHandler.obtainMessage(6, show ? 1 : 0, 0).sendToTarget();
    }

    public final void refreshState() {
        refreshState(null);
    }

    public final void refreshState(Object arg) {
        this.mHandler.obtainMessage(5, arg).sendToTarget();
    }

    public final void clearState() {
        this.mHandler.sendEmptyMessage(11);
    }

    public void userSwitch(int newUserId) {
        this.mHandler.obtainMessage(7, newUserId, 0).sendToTarget();
    }

    public void fireToggleStateChanged(boolean state) {
        this.mHandler.obtainMessage(8, state ? 1 : 0, 0).sendToTarget();
    }

    public void fireScanStateChanged(boolean state) {
        this.mHandler.obtainMessage(9, state ? 1 : 0, 0).sendToTarget();
    }

    public void destroy() {
        this.mHandler.sendEmptyMessage(10);
    }

    public TState getState() {
        return this.mState;
    }

    public void setDetailListening(boolean listening) {
    }

    public void handleAddCallback(Callback callback) {
        this.mCallbacks.add(callback);
        callback.onStateChanged(this.mState);
    }

    public void handleRemoveCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public void handleRemoveCallbacks() {
        this.mCallbacks.clear();
    }

    protected void handleSecondaryClick() {
        handleClick();
    }

    protected void handleLongClick() {
        MetricsLogger.action(this.mContext, 366, getTileSpec());
        this.mHost.startActivityDismissingKeyguard(getLongClickIntent());
    }

    protected void handleClearState() {
        this.mTmpState = (TState) newTileState();
        this.mState = (TState) newTileState();
    }

    protected void handleRefreshState(Object arg) {
        handleUpdateState(this.mTmpState, arg);
        boolean changed = this.mTmpState.copyTo(this.mState);
        if (!changed) {
            return;
        }
        handleStateChanged();
    }

    private void handleStateChanged() {
        String announcement;
        boolean delayAnnouncement = shouldAnnouncementBeDelayed();
        if (this.mCallbacks.size() != 0) {
            State stateNewTileState = newTileState();
            this.mState.copyTo(stateNewTileState);
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                this.mCallbacks.get(i).onStateChanged(stateNewTileState);
            }
            if (this.mAnnounceNextStateChange && !delayAnnouncement && (announcement = composeChangeAnnouncement()) != null) {
                this.mCallbacks.get(0).onAnnouncementRequested(announcement);
            }
        }
        if (!this.mAnnounceNextStateChange) {
            delayAnnouncement = false;
        }
        this.mAnnounceNextStateChange = delayAnnouncement;
    }

    protected boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    protected String composeChangeAnnouncement() {
        return null;
    }

    public void handleShowDetail(boolean show) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onShowDetail(show);
        }
    }

    public void handleToggleStateChanged(boolean state) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onToggleStateChanged(state);
        }
    }

    public void handleScanStateChanged(boolean state) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onScanStateChanged(state);
        }
    }

    protected void handleUserSwitch(int newUserId) {
        handleRefreshState(null);
    }

    protected void handleDestroy() {
        setListening(false);
        this.mCallbacks.clear();
    }

    protected void checkIfRestrictionEnforcedByAdminOnly(State state, String userRestriction) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, userRestriction, ActivityManager.getCurrentUser());
        if (admin != null && !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, userRestriction, ActivityManager.getCurrentUser())) {
            state.disabledByPolicy = true;
            state.enforcedAdmin = admin;
        } else {
            state.disabledByPolicy = false;
            state.enforcedAdmin = null;
        }
    }

    protected final class H extends Handler {
        H(QSTile this$0, Looper looper, H h) {
            this(looper);
        }

        private H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == 1) {
                    QSTile.this.handleAddCallback((Callback) msg.obj);
                    return;
                }
                if (msg.what == 12) {
                    QSTile.this.handleRemoveCallbacks();
                    return;
                }
                if (msg.what == 13) {
                    QSTile.this.handleRemoveCallback((Callback) msg.obj);
                    return;
                }
                if (msg.what == 2) {
                    if (QSTile.this.mState.disabledByPolicy) {
                        Intent intent = RestrictedLockUtils.getShowAdminSupportDetailsIntent(QSTile.this.mContext, QSTile.this.mState.enforcedAdmin);
                        if (intent != null) {
                            QSTile.this.mHost.startActivityDismissingKeyguard(intent);
                            return;
                        }
                        return;
                    }
                    QSTile.this.mAnnounceNextStateChange = true;
                    QSTile.this.handleClick();
                    return;
                }
                if (msg.what == 3) {
                    QSTile.this.handleSecondaryClick();
                    return;
                }
                if (msg.what == 4) {
                    QSTile.this.handleLongClick();
                    return;
                }
                if (msg.what == 5) {
                    QSTile.this.handleRefreshState(msg.obj);
                    return;
                }
                if (msg.what == 6) {
                    QSTile.this.handleShowDetail(msg.arg1 != 0);
                    return;
                }
                if (msg.what == 7) {
                    QSTile.this.handleUserSwitch(msg.arg1);
                    return;
                }
                if (msg.what == 8) {
                    QSTile.this.handleToggleStateChanged(msg.arg1 != 0);
                    return;
                }
                if (msg.what == 9) {
                    QSTile.this.handleScanStateChanged(msg.arg1 != 0);
                    return;
                }
                if (msg.what == 10) {
                    QSTile.this.handleDestroy();
                } else if (msg.what == 11) {
                    QSTile.this.handleClearState();
                } else {
                    if (msg.what == 14) {
                        QSTile.this.setListening(msg.arg1 != 0);
                        return;
                    }
                    throw new IllegalArgumentException("Unknown msg: " + msg.what);
                }
            } catch (Throwable t) {
                String error = "Error in " + ((String) null);
                Log.w(QSTile.this.TAG, error, t);
                QSTile.this.mHost.warn(error, t);
            }
        }
    }

    public static abstract class Icon {
        public abstract Drawable getDrawable(Context context);

        public Drawable getInvisibleDrawable(Context context) {
            return getDrawable(context);
        }

        public int hashCode() {
            return Icon.class.hashCode();
        }

        public int getPadding() {
            return 0;
        }
    }

    public static class DrawableIcon extends Icon {
        protected final Drawable mDrawable;

        public DrawableIcon(Drawable drawable) {
            this.mDrawable = drawable;
        }

        @Override
        public Drawable getDrawable(Context context) {
            return this.mDrawable;
        }

        @Override
        public Drawable getInvisibleDrawable(Context context) {
            return this.mDrawable;
        }
    }

    public static class ResourceIcon extends Icon {
        private static final SparseArray<Icon> ICONS = new SparseArray<>();
        protected final int mResId;

        ResourceIcon(int resId, ResourceIcon resourceIcon) {
            this(resId);
        }

        private ResourceIcon(int resId) {
            this.mResId = resId;
        }

        public static Icon get(int resId) {
            Icon icon = ICONS.get(resId);
            if (icon == null) {
                Icon icon2 = new ResourceIcon(resId);
                ICONS.put(resId, icon2);
                return icon2;
            }
            return icon;
        }

        @Override
        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        @Override
        public Drawable getInvisibleDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public boolean equals(Object o) {
            return (o instanceof ResourceIcon) && ((ResourceIcon) o).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", Integer.valueOf(this.mResId));
        }
    }

    protected class AnimationIcon extends ResourceIcon {
        private final int mAnimatedResId;

        public AnimationIcon(int resId, int staticResId) {
            super(staticResId, null);
            this.mAnimatedResId = resId;
        }

        @Override
        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mAnimatedResId).getConstantState().newDrawable();
        }
    }

    public static class State {
        public boolean autoMirrorDrawable = true;
        public CharSequence contentDescription;
        public boolean disabledByPolicy;
        public CharSequence dualLabelContentDescription;
        public RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public String expandedAccessibilityClassName;
        public Icon icon;
        public CharSequence label;
        public String minimalAccessibilityClassName;
        public CharSequence minimalContentDescription;

        public boolean copyTo(State other) {
            if (other == null) {
                throw new IllegalArgumentException();
            }
            if (!other.getClass().equals(getClass())) {
                throw new IllegalArgumentException();
            }
            boolean changed = (Objects.equals(other.icon, this.icon) && Objects.equals(other.label, this.label) && Objects.equals(other.contentDescription, this.contentDescription) && Objects.equals(Boolean.valueOf(other.autoMirrorDrawable), Boolean.valueOf(this.autoMirrorDrawable)) && Objects.equals(other.dualLabelContentDescription, this.dualLabelContentDescription) && Objects.equals(other.minimalContentDescription, this.minimalContentDescription) && Objects.equals(other.minimalAccessibilityClassName, this.minimalAccessibilityClassName) && Objects.equals(other.expandedAccessibilityClassName, this.expandedAccessibilityClassName) && Objects.equals(Boolean.valueOf(other.disabledByPolicy), Boolean.valueOf(this.disabledByPolicy)) && Objects.equals(other.enforcedAdmin, this.enforcedAdmin)) ? false : true;
            other.icon = this.icon;
            other.label = this.label;
            other.contentDescription = this.contentDescription;
            other.dualLabelContentDescription = this.dualLabelContentDescription;
            other.minimalContentDescription = this.minimalContentDescription;
            other.minimalAccessibilityClassName = this.minimalAccessibilityClassName;
            other.expandedAccessibilityClassName = this.expandedAccessibilityClassName;
            other.autoMirrorDrawable = this.autoMirrorDrawable;
            other.disabledByPolicy = this.disabledByPolicy;
            if (this.enforcedAdmin == null) {
                other.enforcedAdmin = null;
            } else if (other.enforcedAdmin == null) {
                other.enforcedAdmin = new RestrictedLockUtils.EnforcedAdmin(this.enforcedAdmin);
            } else {
                this.enforcedAdmin.copyTo(other.enforcedAdmin);
            }
            return changed;
        }

        public String toString() {
            return toStringBuilder().toString();
        }

        protected StringBuilder toStringBuilder() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
            sb.append(",icon=").append(this.icon);
            sb.append(",label=").append(this.label);
            sb.append(",contentDescription=").append(this.contentDescription);
            sb.append(",dualLabelContentDescription=").append(this.dualLabelContentDescription);
            sb.append(",minimalContentDescription=").append(this.minimalContentDescription);
            sb.append(",minimalAccessibilityClassName=").append(this.minimalAccessibilityClassName);
            sb.append(",expandedAccessibilityClassName=").append(this.expandedAccessibilityClassName);
            sb.append(",autoMirrorDrawable=").append(this.autoMirrorDrawable);
            sb.append(",disabledByPolicy=").append(this.disabledByPolicy);
            sb.append(",enforcedAdmin=").append(this.enforcedAdmin);
            return sb.append(']');
        }
    }

    public static class BooleanState extends State {
        public boolean value;

        @Override
        public boolean copyTo(State other) {
            BooleanState o = (BooleanState) other;
            boolean changed = super.copyTo(other) || o.value != this.value;
            o.value = this.value;
            return changed;
        }

        @Override
        protected StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",value=" + this.value);
            return rt;
        }
    }

    public static class AirplaneBooleanState extends BooleanState {
        public boolean isAirplaneMode;

        @Override
        public boolean copyTo(State other) {
            AirplaneBooleanState o = (AirplaneBooleanState) other;
            boolean changed = super.copyTo(other) || o.isAirplaneMode != this.isAirplaneMode;
            o.isAirplaneMode = this.isAirplaneMode;
            return changed;
        }
    }

    public static final class SignalState extends BooleanState {
        public boolean activityIn;
        public boolean activityOut;
        public boolean connected;
        public boolean filter;
        public boolean isOverlayIconWide;
        public int overlayIconId;

        @Override
        public boolean copyTo(State other) {
            SignalState o = (SignalState) other;
            boolean changed = (o.connected == this.connected && o.activityIn == this.activityIn && o.activityOut == this.activityOut && o.overlayIconId == this.overlayIconId && o.isOverlayIconWide == this.isOverlayIconWide) ? false : true;
            o.connected = this.connected;
            o.activityIn = this.activityIn;
            o.activityOut = this.activityOut;
            o.overlayIconId = this.overlayIconId;
            o.filter = this.filter;
            o.isOverlayIconWide = this.isOverlayIconWide;
            if (super.copyTo(other)) {
                return true;
            }
            return changed;
        }

        @Override
        protected StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",connected=" + this.connected);
            rt.insert(rt.length() - 1, ",activityIn=" + this.activityIn);
            rt.insert(rt.length() - 1, ",activityOut=" + this.activityOut);
            rt.insert(rt.length() - 1, ",overlayIconId=" + this.overlayIconId);
            rt.insert(rt.length() - 1, ",filter=" + this.filter);
            rt.insert(rt.length() - 1, ",wideOverlayIcon=" + this.isOverlayIconWide);
            return rt;
        }
    }
}
