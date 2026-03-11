package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.BenesseExtension;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.SecureSetting;

public class ColorInversionTile extends QSTile<QSTile.BooleanState> {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private final SecureSetting mSetting;

    public ColorInversionTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_invert_colors_enable_animation, R.drawable.ic_invert_colors_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_invert_colors_disable_animation, R.drawable.ic_invert_colors_enable);
        this.mSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                ColorInversionTile.this.handleRefreshState(Integer.valueOf(value));
            }
        };
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        this.mSetting.setListening(false);
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        this.mSetting.setListening(listening);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        this.mSetting.setUserId(newUserId);
        handleRefreshState(Integer.valueOf(this.mSetting.getValue()));
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.ACCESSIBILITY_SETTINGS");
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.BooleanState) this.mState).value);
        this.mSetting.setValue(((QSTile.BooleanState) this.mState).value ? 0 : 1);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_inversion_label);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int value = arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue();
        boolean enabled = value != 0;
        state.value = enabled;
        state.label = this.mContext.getString(R.string.quick_settings_inversion_label);
        state.icon = enabled ? this.mEnable : this.mDisable;
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
        state.contentDescription = state.label;
    }

    @Override
    public int getMetricsCategory() {
        return 116;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_off);
    }
}
