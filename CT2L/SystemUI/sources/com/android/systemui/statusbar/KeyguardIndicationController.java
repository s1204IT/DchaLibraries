package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;

public class KeyguardIndicationController {
    private final Context mContext;
    private boolean mPowerCharged;
    private boolean mPowerPluggedIn;
    private String mRestingIndication;
    private final KeyguardIndicationTextView mTextView;
    private String mTransientIndication;
    private boolean mVisible;
    KeyguardUpdateMonitorCallback mUpdateMonitor = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
            boolean isChargingOrFull = status.status == 2 || status.status == 5;
            KeyguardIndicationController.this.mPowerPluggedIn = status.isPluggedIn() && isChargingOrFull;
            KeyguardIndicationController.this.mPowerCharged = status.isCharged();
            KeyguardIndicationController.this.updateIndication();
        }
    };
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication();
            }
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1 && KeyguardIndicationController.this.mTransientIndication != null) {
                KeyguardIndicationController.this.mTransientIndication = null;
                KeyguardIndicationController.this.updateIndication();
            }
        }
    };
    private final IBatteryStats mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));

    public KeyguardIndicationController(Context context, KeyguardIndicationTextView textView) {
        this.mContext = context;
        this.mTextView = textView;
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateMonitor);
        context.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, new IntentFilter("android.intent.action.TIME_TICK"), null, null);
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
        this.mTextView.setVisibility(visible ? 0 : 8);
        if (visible) {
            hideTransientIndication();
            updateIndication();
        }
    }

    public void hideTransientIndicationDelayed(long delayMs) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), delayMs);
    }

    public void showTransientIndication(int transientIndication) {
        showTransientIndication(this.mContext.getResources().getString(transientIndication));
    }

    public void showTransientIndication(String transientIndication) {
        this.mTransientIndication = transientIndication;
        this.mHandler.removeMessages(1);
        updateIndication();
    }

    public void hideTransientIndication() {
        if (this.mTransientIndication != null) {
            this.mTransientIndication = null;
            this.mHandler.removeMessages(1);
            updateIndication();
        }
    }

    private void updateIndication() {
        if (this.mVisible) {
            this.mTextView.switchIndication(computeIndication());
        }
    }

    private String computeIndication() {
        if (!TextUtils.isEmpty(this.mTransientIndication)) {
            return this.mTransientIndication;
        }
        if (this.mPowerPluggedIn) {
            return computePowerIndication();
        }
        return this.mRestingIndication;
    }

    private String computePowerIndication() {
        if (this.mPowerCharged) {
            return this.mContext.getResources().getString(R.string.keyguard_charged);
        }
        try {
            long chargingTimeRemaining = this.mBatteryInfo.computeChargeTimeRemaining();
            if (chargingTimeRemaining > 0) {
                String chargingTimeFormatted = Formatter.formatShortElapsedTimeRoundingUpToMinutes(this.mContext, chargingTimeRemaining);
                return this.mContext.getResources().getString(R.string.keyguard_indication_charging_time, chargingTimeFormatted);
            }
        } catch (RemoteException e) {
            Log.e("KeyguardIndicationController", "Error calling IBatteryStats: ", e);
        }
        return this.mContext.getResources().getString(R.string.keyguard_plugged_in);
    }
}
