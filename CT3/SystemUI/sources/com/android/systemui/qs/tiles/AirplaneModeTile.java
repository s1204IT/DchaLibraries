package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.mediatek.internal.telephony.ITelephonyEx;

public class AirplaneModeTile extends QSTile<QSTile.BooleanState> {
    private AnimationHandler mAnimHandler;
    private QSTile.Icon[] mAnimMembers;
    private int mCount;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private boolean mListening;
    private final BroadcastReceiver mReceiver;
    private final GlobalSetting mSetting;
    private boolean mSwitching;

    public AirplaneModeTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_airplane_enable_animation, R.drawable.ic_signal_airplane_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_airplane_disable_animation, R.drawable.ic_signal_airplane_enable);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction()) || !"com.mediatek.intent.action.AIRPLANE_CHANGE_DONE".equals(intent.getAction())) {
                    return;
                }
                boolean airplaneModeOn = intent.getBooleanExtra("airplaneMode", false);
                Log.d(AirplaneModeTile.this.TAG, "onReceive() AIRPLANE_CHANGE_DONE,  airplaneModeOn= " + airplaneModeOn);
                AirplaneModeTile.this.stopAnimation();
                AirplaneModeTile.this.refreshState();
            }
        };
        this.mCount = -1;
        this.mAnimMembers = new QSTile.Icon[]{QSTile.ResourceIcon.get(R.drawable.ic_signal_airplane_swiching_2), QSTile.ResourceIcon.get(R.drawable.ic_signal_airplane_swiching_3)};
        this.mAnimHandler = new AnimationHandler(this, null);
        this.mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
            @Override
            protected void handleValueChanged(int value) {
                AirplaneModeTile.this.handleRefreshState(Integer.valueOf(value));
            }
        };
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void handleClick() {
        if (this.mSwitching) {
            return;
        }
        startAnimation();
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.BooleanState) this.mState).value);
        setEnabled(((QSTile.BooleanState) this.mState).value ? false : true);
    }

    private void setEnabled(boolean enabled) {
        Log.d(this.TAG, "setEnabled = " + enabled);
        ConnectivityManager mgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        mgr.setAirplaneMode(enabled);
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.AIRPLANE_MODE_SETTINGS");
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.airplane_mode);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int value = arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue();
        boolean airplaneMode = value != 0;
        state.value = airplaneMode;
        state.label = this.mContext.getString(R.string.airplane_mode);
        if (airplaneMode) {
            state.icon = this.mEnable;
        } else {
            state.icon = this.mDisable;
        }
        state.contentDescription = state.label;
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
        handleAnimationState(state, arg);
    }

    @Override
    public int getMetricsCategory() {
        return 112;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_off);
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        Log.d(this.TAG, "setListening(): " + this.mListening);
        if (listening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.AIRPLANE_MODE");
            filter.addAction("com.mediatek.intent.action.AIRPLANE_CHANGE_DONE");
            this.mContext.registerReceiver(this.mReceiver, filter);
            if (!isAirplanemodeAvailableNow()) {
                Log.d(this.TAG, "setListening() Airplanemode not Available, start anim.");
                startAnimation();
            }
        } else {
            this.mContext.unregisterReceiver(this.mReceiver);
            stopAnimation();
        }
        this.mSetting.setListening(listening);
    }

    private class AnimationHandler extends Handler {
        AnimationHandler(AirplaneModeTile this$0, AnimationHandler animationHandler) {
            this();
        }

        private AnimationHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            AirplaneModeTile.this.mAnimHandler.sendEmptyMessageDelayed(0, 400L);
            AirplaneModeTile airplaneModeTile = AirplaneModeTile.this;
            int i = airplaneModeTile.mCount;
            airplaneModeTile.mCount = i + 1;
            if (i >= 60) {
                AirplaneModeTile.this.mCount = -1;
                if (AirplaneModeTile.this.isAirplanemodeAvailableNow()) {
                    Log.w(AirplaneModeTile.this.TAG, "No need show anim now...");
                    AirplaneModeTile.this.stopAnimation();
                }
            }
            AirplaneModeTile.this.refreshState();
        }
    }

    private void startAnimation() {
        stopAnimation();
        this.mSwitching = true;
        this.mAnimHandler.sendEmptyMessage(0);
        Log.d(this.TAG, "startAnimation()");
    }

    public void stopAnimation() {
        this.mSwitching = false;
        this.mCount = -1;
        if (this.mAnimHandler.hasMessages(0)) {
            this.mAnimHandler.removeMessages(0);
        }
        Log.d(this.TAG, "stopAnimation()");
    }

    private void handleAnimationState(QSTile.BooleanState state, Object arg) {
        if (!this.mSwitching || this.mCount == -1) {
            return;
        }
        state.icon = this.mAnimMembers[this.mCount % 2];
    }

    public boolean isAirplanemodeAvailableNow() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean isAvailable = true;
        try {
            if (telephonyEx != null) {
                isAvailable = telephonyEx.isAirplanemodeAvailableNow();
            } else {
                Log.w(this.TAG, "telephonyEx == null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(this.TAG, "isAirplaneModeAvailable = " + isAvailable);
        return isAvailable;
    }
}
