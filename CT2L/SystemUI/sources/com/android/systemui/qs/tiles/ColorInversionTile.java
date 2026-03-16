package com.android.systemui.qs.tiles;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.UsageTracker;

public class ColorInversionTile extends QSTile<QSTile.BooleanState> {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private boolean mListening;
    private final SecureSetting mSetting;
    private final UsageTracker mUsageTracker;

    public ColorInversionTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_invert_colors_enable_animation);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_invert_colors_disable_animation);
        this.mSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                if (value != 0 || observedChange) {
                    ColorInversionTile.this.mUsageTracker.trackUsage();
                }
                if (ColorInversionTile.this.mListening) {
                    ColorInversionTile.this.handleRefreshState(Integer.valueOf(value));
                }
            }
        };
        this.mUsageTracker = new UsageTracker(host.getContext(), ColorInversionTile.class, R.integer.days_to_show_color_inversion_tile);
        if (this.mSetting.getValue() != 0 && !this.mUsageTracker.isRecentlyUsed()) {
            this.mUsageTracker.trackUsage();
        }
        this.mUsageTracker.setListening(true);
        this.mSetting.setListening(true);
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        this.mUsageTracker.setListening(false);
        this.mSetting.setListening(false);
    }

    @Override
    protected QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        this.mListening = listening;
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        this.mSetting.setUserId(newUserId);
        handleRefreshState(Integer.valueOf(this.mSetting.getValue()));
    }

    @Override
    protected void handleClick() {
        this.mSetting.setValue(((QSTile.BooleanState) this.mState).value ? 0 : 1);
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
                    ColorInversionTile.this.refreshState();
                }
            });
        }
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int value = arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue();
        boolean enabled = value != 0;
        state.visible = enabled || this.mUsageTracker.isRecentlyUsed();
        state.value = enabled;
        state.label = this.mContext.getString(R.string.quick_settings_inversion_label);
        state.icon = enabled ? this.mEnable : this.mDisable;
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.BooleanState) this.mState).value ? this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_off);
    }
}
