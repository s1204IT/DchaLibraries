package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;

public class AirplaneModeTile extends QSTile<QSTile.BooleanState> {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private boolean mListening;
    private final BroadcastReceiver mReceiver;
    private final GlobalSetting mSetting;

    public AirplaneModeTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_airplane_enable_animation);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_airplane_disable_animation);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                    AirplaneModeTile.this.refreshState();
                }
            }
        };
        this.mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
            @Override
            protected void handleValueChanged(int value) {
                AirplaneModeTile.this.handleRefreshState(Integer.valueOf(value));
            }
        };
    }

    @Override
    protected QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void handleClick() {
        setEnabled(!((QSTile.BooleanState) this.mState).value);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
    }

    private void setEnabled(boolean enabled) {
        ConnectivityManager mgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        mgr.setAirplaneMode(enabled);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int value = arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue();
        boolean airplaneMode = value != 0;
        state.value = airplaneMode;
        state.visible = true;
        state.label = this.mContext.getString(R.string.quick_settings_airplane_mode_label);
        if (airplaneMode) {
            state.icon = this.mEnable;
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_airplane_on);
        } else {
            state.icon = this.mDisable;
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_airplane_off);
        }
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.BooleanState) this.mState).value ? this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_off);
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.AIRPLANE_MODE");
                this.mContext.registerReceiver(this.mReceiver, filter);
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
            }
            this.mSetting.setListening(listening);
        }
    }
}
