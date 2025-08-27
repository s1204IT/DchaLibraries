package com.android.settings.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class MobileNetworkPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private BroadcastReceiver mAirplanModeChangedReceiver;
    private final boolean mIsSecondaryUser;
    PhoneStateListener mPhoneStateListener;
    private Preference mPreference;
    private final BroadcastReceiver mReceiver;
    private final TelephonyManager mTelephonyManager;
    private final UserManager mUserManager;

    public MobileNetworkPreferenceController(Context context) {
        super(context);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.settings.network.MobileNetworkPreferenceController.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(intent.getAction())) {
                    Log.d("MobileNetworkPreferenceController", "ACTION_SIM_INFO_UPDATE received");
                    MobileNetworkPreferenceController.this.updateMobileNetworkEnabled();
                }
            }
        };
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mIsSecondaryUser = !this.mUserManager.isAdminUser();
        this.mAirplanModeChangedReceiver = new BroadcastReceiver() { // from class: com.android.settings.network.MobileNetworkPreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                MobileNetworkPreferenceController.this.updateState(MobileNetworkPreferenceController.this.mPreference);
            }
        };
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (isUserRestricted() || Utils.isWifiOnly(this.mContext)) ? false : true;
    }

    public boolean isUserRestricted() {
        return this.mIsSecondaryUser || RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_mobile_networks", UserHandle.myUserId());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "mobile_network_settings";
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
        if (isAvailable()) {
            if (Looper.myLooper() == null) {
                Log.d("MobileNetworkPreferenceController", "onResume Looper is null.");
                return;
            } else {
                if (this.mPhoneStateListener == null) {
                    this.mPhoneStateListener = new PhoneStateListener() { // from class: com.android.settings.network.MobileNetworkPreferenceController.2
                        @Override // android.telephony.PhoneStateListener
                        public void onCallStateChanged(int i, String str) {
                            super.onCallStateChanged(i, str);
                            Log.d("MobileNetworkPreferenceController", "PhoneStateListener, new state=" + i);
                            if (i == 0) {
                                MobileNetworkPreferenceController.this.updateMobileNetworkEnabled();
                            }
                        }

                        @Override // android.telephony.PhoneStateListener
                        public void onServiceStateChanged(ServiceState serviceState) {
                        }
                    };
                }
                this.mTelephonyManager.listen(this.mPhoneStateListener, 33);
            }
        }
        if (this.mAirplanModeChangedReceiver != null) {
            this.mContext.registerReceiver(this.mAirplanModeChangedReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        if (this.mPhoneStateListener != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        if (this.mAirplanModeChangedReceiver != null) {
            this.mContext.unregisterReceiver(this.mAirplanModeChangedReceiver);
        }
        if (this.mReceiver != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        if ((preference instanceof RestrictedPreference) && ((RestrictedPreference) preference).isDisabledByAdmin()) {
            Log.d("MobileNetworkPreferenceController", "updateState,Mobile Network preference disabled by Admin");
            return;
        }
        boolean z = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0;
        Log.d("MobileNetworkPreferenceController", "updateState,isAirplaneModeOff = " + z);
        if (!z) {
            preference.setEnabled(false);
            return;
        }
        preference.setEnabled(z);
        try {
            if (SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoList() == null) {
                Log.d("MobileNetworkPreferenceController", "updateState,si == null");
                preference.setEnabled(false);
            } else {
                int callState = ((TelephonyManager) this.mContext.getSystemService("phone")).getCallState();
                Log.d("MobileNetworkPreferenceController", "updateState,callState = " + callState);
                if (callState == 0) {
                    preference.setEnabled(true);
                } else {
                    preference.setEnabled(false);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("MobileNetworkPreferenceController", "IndexOutOfBoundsException");
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return "";
    }

    private void updateMobileNetworkEnabled() {
        if (this.mPreference == null) {
            return;
        }
        if ((this.mPreference instanceof RestrictedPreference) && ((RestrictedPreference) this.mPreference).isDisabledByAdmin()) {
            Log.d("MobileNetworkPreferenceController", "updateMobileNetworkEnabled,Mobile Network disabled by Admin");
            return;
        }
        boolean z = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0;
        Log.d("MobileNetworkPreferenceController", "updateMobileNetworkEnabled,isAirplaneModeOff = " + z);
        if (!z) {
            this.mPreference.setEnabled(false);
            return;
        }
        int activeSubscriptionInfoCount = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoCount();
        int callState = ((TelephonyManager) this.mContext.getSystemService("phone")).getCallState();
        Log.d("MobileNetworkPreferenceController", "callstate = " + callState + " simNum = " + activeSubscriptionInfoCount);
        if (activeSubscriptionInfoCount <= 0 || callState != 0) {
            this.mPreference.setEnabled(false);
        } else {
            this.mPreference.setEnabled(true);
        }
    }
}
