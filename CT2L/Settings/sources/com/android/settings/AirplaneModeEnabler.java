package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.internal.telephony.PhoneStateIntentReceiver;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private final SwitchPreference mSwitchPref;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    AirplaneModeEnabler.this.onAirplaneModeChanged();
                    break;
            }
        }
    };
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            AirplaneModeEnabler.this.onAirplaneModeChanged();
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
        this.mSwitchPref.setChecked(isAirplaneModeOn(this.mContext));
        this.mPhoneStateReceiver.registerIntent();
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
    }

    public void pause() {
        this.mPhoneStateReceiver.unregisterIntent();
        this.mSwitchPref.setOnPreferenceChangeListener(null);
        this.mContext.getContentResolver().unregisterContentObserver(this.mAirplaneModeObserver);
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", enabling ? 1 : 0);
        this.mSwitchPref.setChecked(enabling);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", enabling);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void onAirplaneModeChanged() {
        this.mSwitchPref.setChecked(isAirplaneModeOn(this.mContext));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            setAirplaneModeOn(((Boolean) newValue).booleanValue());
            return true;
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            onAirplaneModeChanged();
        }
    }
}
