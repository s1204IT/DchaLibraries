package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.UsageTracker;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class HotspotTile extends QSTile<QSTile.BooleanState> {
    private final Callback mCallback;
    private final HotspotController mController;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private final KeyguardMonitor mKeyguard;
    private final UsageTracker mUsageTracker;

    public HotspotTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_hotspot_enable_animation);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_hotspot_disable_animation);
        this.mCallback = new Callback();
        this.mController = host.getHotspotController();
        this.mUsageTracker = newUsageTracker(host.getContext());
        this.mUsageTracker.setListening(true);
        this.mKeyguard = host.getKeyguardMonitor();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        this.mUsageTracker.setListening(false);
    }

    @Override
    protected QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addCallback(this.mCallback);
        } else {
            this.mController.removeCallback(this.mCallback);
        }
    }

    @Override
    protected void handleClick() {
        boolean isEnabled = Boolean.valueOf(((QSTile.BooleanState) this.mState).value).booleanValue();
        this.mController.setHotspotEnabled(!isEnabled);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
    }

    @Override
    protected void handleLongClick() {
        if (!((QSTile.BooleanState) this.mState).value) {
            String title = this.mContext.getString(R.string.quick_settings_reset_confirmation_title, ((QSTile.BooleanState) this.mState).label);
            this.mUsageTracker.showResetConfirmation(title, new Runnable() {
                @Override
                public void run() {
                    HotspotTile.this.refreshState();
                }
            });
        }
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.visible = this.mController.isHotspotSupported() && this.mUsageTracker.isRecentlyUsed();
        state.label = this.mContext.getString(R.string.quick_settings_hotspot_label);
        state.value = this.mController.isHotspotEnabled();
        state.icon = (state.visible && state.value) ? this.mEnable : this.mDisable;
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.BooleanState) this.mState).value ? this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_off);
    }

    private static UsageTracker newUsageTracker(Context context) {
        return new UsageTracker(context, HotspotTile.class, R.integer.days_to_show_hotspot_tile);
    }

    private final class Callback implements HotspotController.Callback {
        private Callback() {
        }

        @Override
        public void onHotspotChanged(boolean enabled) {
            HotspotTile.this.refreshState();
        }
    }

    public static class APChangedReceiver extends BroadcastReceiver {
        private UsageTracker mUsageTracker;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (this.mUsageTracker == null) {
                this.mUsageTracker = HotspotTile.newUsageTracker(context);
            }
            this.mUsageTracker.trackUsage();
        }
    }
}
