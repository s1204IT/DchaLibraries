package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.settingslib.WirelessUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private final SwitchPreference mSwitchPref;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.DESTROY:
                    AirplaneModeEnabler.this.mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(AirplaneModeEnabler.this.mContext));
                    break;
            }
        }
    };
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (AirplaneModeEnabler.this.mSwitchPref.isChecked() == WirelessUtils.isAirplaneModeOn(AirplaneModeEnabler.this.mContext)) {
                return;
            }
            Log.d("AirplaneModeEnabler", "airplanemode changed by others, update UI...");
            AirplaneModeEnabler.this.onAirplaneModeChanged();
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"com.mediatek.intent.action.AIRPLANE_CHANGE_DONE".equals(action)) {
                return;
            }
            boolean airplaneMode = intent.getBooleanExtra("airplaneMode", false);
            Log.d("AirplaneModeEnabler", "onReceive, ACTION_AIRPLANE_CHANGE_DONE, " + airplaneMode);
            AirplaneModeEnabler.this.mSwitchPref.setEnabled(AirplaneModeEnabler.this.isAirplaneModeAvailable());
        }
    };

    public AirplaneModeEnabler(Context context, SwitchPreference airplaneModeSwitchPreference) {
        this.mContext = context;
        this.mSwitchPref = airplaneModeSwitchPreference;
        airplaneModeSwitchPreference.setPersistent(false);
        this.mPhoneStateReceiver = new PhoneStateIntentReceiver(this.mContext, this.mHandler);
        this.mPhoneStateReceiver.notifyServiceState(3);
    }

    public void resume() {
        this.mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(this.mContext));
        this.mPhoneStateReceiver.registerIntent();
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        this.mSwitchPref.setEnabled(isAirplaneModeAvailable());
        IntentFilter intentFilter = new IntentFilter("com.mediatek.intent.action.AIRPLANE_CHANGE_DONE");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    public void pause() {
        this.mPhoneStateReceiver.unregisterIntent();
        this.mSwitchPref.setOnPreferenceChangeListener(null);
        this.mContext.getContentResolver().unregisterContentObserver(this.mAirplaneModeObserver);
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void setAirplaneModeOn(boolean enabling) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", enabling ? 1 : 0);
        this.mSwitchPref.setChecked(enabling);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", enabling);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        this.mSwitchPref.setEnabled(false);
    }

    public void onAirplaneModeChanged() {
        this.mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(this.mContext));
        this.mSwitchPref.setEnabled(isAirplaneModeAvailable());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("AirplaneModeEnabler", "onPreferenceChange, newValue = " + newValue);
        String ecbMode = SystemProperties.get("ril.cdma.inecmmode", "false");
        if (ecbMode != null && ecbMode.contains("true") && UserHandle.myUserId() == 0) {
            Log.d("AirplaneModeEnabler", "ignore as ecbMode = " + newValue);
            return true;
        }
        Boolean value = (Boolean) newValue;
        MetricsLogger.action(this.mContext, 177, value.booleanValue());
        setAirplaneModeOn(value.booleanValue());
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            onAirplaneModeChanged();
        }
    }

    public boolean isAirplaneModeAvailable() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean isAvailable = false;
        if (telephonyEx != null) {
            try {
                isAvailable = telephonyEx.isAirplanemodeAvailableNow();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Log.d("AirplaneModeEnabler", "isAirplaneModeAvailable = " + isAvailable);
        return isAvailable;
    }
}
