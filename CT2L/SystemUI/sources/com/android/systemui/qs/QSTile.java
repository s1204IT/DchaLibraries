package com.android.systemui.qs;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.qs.QSTile.State;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.Listenable;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import java.util.Objects;

public abstract class QSTile<TState extends State> implements Listenable {
    protected static final boolean DEBUG = Log.isLoggable("QSTile", 3);
    private boolean mAnnounceNextStateChange;
    private Callback mCallback;
    protected final Context mContext;
    protected final QSTile<TState>.H mHandler;
    protected final Host mHost;
    protected final String TAG = "QSTile." + getClass().getSimpleName();
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());
    protected final TState mState = (TState) newTileState();
    private final TState mTmpState = (TState) newTileState();

    public interface Callback {
        void onAnnouncementRequested(CharSequence charSequence);

        void onScanStateChanged(boolean z);

        void onShowDetail(boolean z);

        void onStateChanged(State state);

        void onToggleStateChanged(boolean z);
    }

    public interface DetailAdapter {
        View createDetailView(Context context, View view, ViewGroup viewGroup);

        Intent getSettingsIntent();

        int getTitle();

        Boolean getToggleState();

        void setToggleState(boolean z);
    }

    public interface Host {

        public interface Callback {
            void onTilesChanged();
        }

        void collapsePanels();

        BluetoothController getBluetoothController();

        CastController getCastController();

        Context getContext();

        FlashlightController getFlashlightController();

        HotspotController getHotspotController();

        KeyguardMonitor getKeyguardMonitor();

        LocationController getLocationController();

        Looper getLooper();

        NetworkController getNetworkController();

        RotationLockController getRotationLockController();

        void startSettingsActivity(Intent intent);

        void warn(String str, Throwable th);
    }

    protected abstract void handleClick();

    protected abstract void handleUpdateState(TState tstate, Object obj);

    protected abstract TState newTileState();

    protected QSTile(Host host) {
        this.mHost = host;
        this.mContext = host.getContext();
        this.mHandler = new H(host.getLooper());
    }

    public boolean supportsDualTargets() {
        return false;
    }

    public QSTileView createTileView(Context context) {
        return new QSTileView(context);
    }

    public DetailAdapter getDetailAdapter() {
        return null;
    }

    public void setCallback(Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
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

    protected final void refreshState() {
        refreshState(null);
    }

    protected final void refreshState(Object arg) {
        this.mHandler.obtainMessage(5, arg).sendToTarget();
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

    private void handleSetCallback(Callback callback) {
        this.mCallback = callback;
        handleRefreshState(null);
    }

    protected void handleSecondaryClick() {
    }

    protected void handleLongClick() {
    }

    protected void handleRefreshState(Object arg) {
        handleUpdateState(this.mTmpState, arg);
        boolean changed = this.mTmpState.copyTo(this.mState);
        if (changed) {
            handleStateChanged();
        }
    }

    private void handleStateChanged() {
        String announcement;
        boolean delayAnnouncement = shouldAnnouncementBeDelayed();
        if (this.mCallback != null) {
            this.mCallback.onStateChanged(this.mState);
            if (this.mAnnounceNextStateChange && !delayAnnouncement && (announcement = composeChangeAnnouncement()) != null) {
                this.mCallback.onAnnouncementRequested(announcement);
            }
        }
        this.mAnnounceNextStateChange = this.mAnnounceNextStateChange && delayAnnouncement;
    }

    protected boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    protected String composeChangeAnnouncement() {
        return null;
    }

    private void handleShowDetail(boolean show) {
        if (this.mCallback != null) {
            this.mCallback.onShowDetail(show);
        }
    }

    private void handleToggleStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onToggleStateChanged(state);
        }
    }

    private void handleScanStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onScanStateChanged(state);
        }
    }

    protected void handleUserSwitch(int newUserId) {
        handleRefreshState(null);
    }

    protected void handleDestroy() {
        setListening(false);
        this.mCallback = null;
    }

    protected final class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == 1) {
                    QSTile.this.handleSetCallback((Callback) msg.obj);
                    return;
                }
                if (msg.what == 2) {
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
                } else if (msg.what == 9) {
                    QSTile.this.handleScanStateChanged(msg.arg1 != 0);
                } else {
                    if (msg.what == 10) {
                        QSTile.this.handleDestroy();
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

        public int hashCode() {
            return Icon.class.hashCode();
        }
    }

    public static class ResourceIcon extends Icon {
        private static final SparseArray<Icon> ICONS = new SparseArray<>();
        private final int mResId;

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

        public boolean equals(Object o) {
            return (o instanceof ResourceIcon) && ((ResourceIcon) o).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", Integer.valueOf(this.mResId));
        }
    }

    protected class AnimationIcon extends ResourceIcon {
        private boolean mAllowAnimation;

        public AnimationIcon(int resId) {
            super(resId);
        }

        public void setAllowAnimation(boolean allowAnimation) {
            this.mAllowAnimation = allowAnimation;
        }

        @Override
        public Drawable getDrawable(Context context) {
            AnimatedVectorDrawable d = (AnimatedVectorDrawable) super.getDrawable(context).getConstantState().newDrawable();
            d.start();
            if (this.mAllowAnimation) {
                this.mAllowAnimation = false;
            } else {
                d.stop();
            }
            return d;
        }
    }

    protected enum UserBoolean {
        USER_TRUE(true, true),
        USER_FALSE(true, false),
        BACKGROUND_TRUE(false, true),
        BACKGROUND_FALSE(false, false);

        public final boolean userInitiated;
        public final boolean value;

        UserBoolean(boolean userInitiated, boolean value) {
            this.value = value;
            this.userInitiated = userInitiated;
        }
    }

    public static class State {
        public boolean autoMirrorDrawable = true;
        public String contentDescription;
        public String dualLabelContentDescription;
        public Icon icon;
        public String label;
        public boolean visible;

        public boolean copyTo(State other) {
            if (other == null) {
                throw new IllegalArgumentException();
            }
            if (!other.getClass().equals(getClass())) {
                throw new IllegalArgumentException();
            }
            boolean changed = (other.visible == this.visible && Objects.equals(other.icon, this.icon) && Objects.equals(other.label, this.label) && Objects.equals(other.contentDescription, this.contentDescription) && Objects.equals(Boolean.valueOf(other.autoMirrorDrawable), Boolean.valueOf(this.autoMirrorDrawable)) && Objects.equals(other.dualLabelContentDescription, this.dualLabelContentDescription)) ? false : true;
            other.visible = this.visible;
            other.icon = this.icon;
            other.label = this.label;
            other.contentDescription = this.contentDescription;
            other.dualLabelContentDescription = this.dualLabelContentDescription;
            other.autoMirrorDrawable = this.autoMirrorDrawable;
            return changed;
        }

        public String toString() {
            return toStringBuilder().toString();
        }

        protected StringBuilder toStringBuilder() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
            sb.append("visible=").append(this.visible);
            sb.append(",icon=").append(this.icon);
            sb.append(",label=").append(this.label);
            sb.append(",contentDescription=").append(this.contentDescription);
            sb.append(",dualLabelContentDescription=").append(this.dualLabelContentDescription);
            sb.append(",autoMirrorDrawable=").append(this.autoMirrorDrawable);
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

    public static final class SignalState extends State {
        public boolean activityIn;
        public boolean activityOut;
        public boolean connected;
        public boolean enabled;
        public boolean filter;
        public boolean isOverlayIconWide;
        public int overlayIconId;

        @Override
        public boolean copyTo(State other) {
            SignalState o = (SignalState) other;
            boolean changed = (o.enabled == this.enabled && o.connected == this.connected && o.activityIn == this.activityIn && o.activityOut == this.activityOut && o.overlayIconId == this.overlayIconId && o.isOverlayIconWide == this.isOverlayIconWide) ? false : true;
            o.enabled = this.enabled;
            o.connected = this.connected;
            o.activityIn = this.activityIn;
            o.activityOut = this.activityOut;
            o.overlayIconId = this.overlayIconId;
            o.filter = this.filter;
            o.isOverlayIconWide = this.isOverlayIconWide;
            return super.copyTo(other) || changed;
        }

        @Override
        protected StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",enabled=" + this.enabled);
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
