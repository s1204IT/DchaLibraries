package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public class AirplaneModeEnabler {
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private OnAirplaneModeChangedListener mOnAirplaneModeChangedListener;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private Handler mHandler = new Handler(Looper.getMainLooper()) { // from class: com.android.settings.AirplaneModeEnabler.1
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 3) {
                AirplaneModeEnabler.this.onAirplaneModeChanged();
            }
        }
    };
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) { // from class: com.android.settings.AirplaneModeEnabler.2
        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            AirplaneModeEnabler.this.onAirplaneModeChanged();
        }
    };

    public interface OnAirplaneModeChangedListener {
        void onAirplaneModeChanged(boolean z);
    }

    public AirplaneModeEnabler(Context context, MetricsFeatureProvider metricsFeatureProvider, OnAirplaneModeChangedListener onAirplaneModeChangedListener) {
        this.mContext = context;
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        this.mOnAirplaneModeChangedListener = onAirplaneModeChangedListener;
        this.mPhoneStateReceiver = new PhoneStateIntentReceiver(this.mContext, this.mHandler);
        this.mPhoneStateReceiver.notifyServiceState(3);
    }

    public void resume() {
        this.mPhoneStateReceiver.registerIntent();
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
    }

    public void pause() {
        this.mPhoneStateReceiver.unregisterIntent();
        this.mContext.getContentResolver().unregisterContentObserver(this.mAirplaneModeObserver);
    }

    private void setAirplaneModeOn(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", z ? 1 : 0);
        if (this.mOnAirplaneModeChangedListener != null) {
            this.mOnAirplaneModeChangedListener.onAirplaneModeChanged(z);
        }
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void onAirplaneModeChanged() {
        if (this.mOnAirplaneModeChangedListener != null) {
            this.mOnAirplaneModeChangedListener.onAirplaneModeChanged(isAirplaneModeOn());
        }
    }

    public void setAirplaneMode(boolean z) {
        Log.d("AirplaneModeEnabler", "setAirplaneMode, isAirplaneModeOn=" + z);
        String str = SystemProperties.get("ril.cdma.inecmmode", "false");
        boolean zIsAdminUser = UserManager.get(this.mContext).isAdminUser();
        if (str != null && str.contains("true") && zIsAdminUser) {
            Log.d("AirplaneModeEnabler", "ignore as ecbMode=" + str);
            return;
        }
        this.mMetricsFeatureProvider.action(this.mContext, 177, z);
        setAirplaneModeOn(z);
    }

    public void setAirplaneModeInECM(boolean z, boolean z2) {
        if (z) {
            setAirplaneModeOn(z2);
        } else {
            onAirplaneModeChanged();
        }
    }

    public boolean isAirplaneModeOn() {
        return WirelessUtils.isAirplaneModeOn(this.mContext);
    }
}
