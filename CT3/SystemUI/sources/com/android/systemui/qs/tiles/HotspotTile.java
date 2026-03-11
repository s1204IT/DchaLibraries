package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BenesseExtension;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.HotspotController;

public class HotspotTile extends QSTile<QSTile.AirplaneBooleanState> {
    private final GlobalSetting mAirplaneMode;
    private final Callback mCallback;
    private final HotspotController mController;
    private final QSTile<QSTile.AirplaneBooleanState>.AnimationIcon mDisable;
    private final QSTile.Icon mDisableNoAnimation;
    private final QSTile<QSTile.AirplaneBooleanState>.AnimationIcon mEnable;
    private boolean mListening;
    private final QSTile.Icon mUnavailable;

    public HotspotTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_hotspot_enable_animation, R.drawable.ic_hotspot_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_hotspot_disable_animation, R.drawable.ic_hotspot_enable);
        this.mDisableNoAnimation = QSTile.ResourceIcon.get(R.drawable.ic_hotspot_enable);
        this.mUnavailable = QSTile.ResourceIcon.get(R.drawable.ic_hotspot_unavailable);
        this.mCallback = new Callback(this, null);
        this.mController = host.getHotspotController();
        this.mAirplaneMode = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
            @Override
            protected void handleValueChanged(int value) {
                HotspotTile.this.refreshState();
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return this.mController.isHotspotSupported();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    public QSTile.AirplaneBooleanState newTileState() {
        return new QSTile.AirplaneBooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (listening) {
            this.mController.addCallback(this.mCallback);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.AIRPLANE_MODE");
            refreshState();
        } else {
            this.mController.removeCallback(this.mCallback);
        }
        this.mAirplaneMode.setListening(listening);
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.WIRELESS_SETTINGS");
    }

    @Override
    protected void handleClick() {
        boolean isEnabled = Boolean.valueOf(((QSTile.AirplaneBooleanState) this.mState).value).booleanValue();
        if (!isEnabled && this.mAirplaneMode.getValue() != 0) {
            return;
        }
        MetricsLogger.action(this.mContext, getMetricsCategory(), !isEnabled);
        this.mController.setHotspotEnabled(isEnabled ? false : true);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_hotspot_label);
    }

    @Override
    public void handleUpdateState(QSTile.AirplaneBooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_hotspot_label);
        checkIfRestrictionEnforcedByAdminOnly(state, "no_config_tethering");
        if (arg instanceof Boolean) {
            state.value = ((Boolean) arg).booleanValue();
        } else {
            state.value = this.mController.isHotspotEnabled();
        }
        state.icon = state.value ? this.mEnable : this.mDisable;
        boolean wasAirplane = state.isAirplaneMode;
        state.isAirplaneMode = this.mAirplaneMode.getValue() != 0;
        if (state.isAirplaneMode) {
            int disabledColor = this.mHost.getContext().getColor(R.color.qs_tile_tint_unavailable);
            state.label = new SpannableStringBuilder().append(state.label, new ForegroundColorSpan(disabledColor), 18);
            state.icon = this.mUnavailable;
        } else if (wasAirplane) {
            state.icon = this.mDisableNoAnimation;
        }
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
        state.contentDescription = state.label;
    }

    @Override
    public int getMetricsCategory() {
        return 120;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.AirplaneBooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_off);
    }

    private final class Callback implements HotspotController.Callback {
        Callback(HotspotTile this$0, Callback callback) {
            this();
        }

        private Callback() {
        }

        @Override
        public void onHotspotChanged(boolean enabled) {
            HotspotTile.this.refreshState(Boolean.valueOf(enabled));
        }
    }
}
