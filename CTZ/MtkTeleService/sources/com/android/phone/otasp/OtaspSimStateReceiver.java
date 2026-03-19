package com.android.phone.otasp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.phone.PhoneGlobals;

public class OtaspSimStateReceiver extends BroadcastReceiver {
    private static final String TAG = OtaspSimStateReceiver.class.getSimpleName();
    private Context mContext;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onOtaspChanged(int i) {
            OtaspSimStateReceiver.logd("onOtaspChanged: otaspMode=" + i);
            if (i == 2) {
                OtaspSimStateReceiver.logd("otasp activation required, start otaspActivationService");
                OtaspSimStateReceiver.this.mContext.startService(new Intent(OtaspSimStateReceiver.this.mContext, (Class<?>) OtaspActivationService.class));
            } else if (i == 3) {
                OtaspActivationService.updateActivationState(OtaspSimStateReceiver.this.mContext, true);
            }
        }
    };

    private static boolean isCarrierSupported() {
        Context context = PhoneGlobals.getPhone().getContext();
        if (context != null) {
            PersistableBundle config = null;
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
            if (carrierConfigManager != null) {
                config = carrierConfigManager.getConfig();
            }
            if (config != null && config.getBoolean("use_otasp_for_provisioning_bool")) {
                return true;
            }
        }
        logd("otasp activation not needed: no supported carrier");
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(intent.getAction())) {
            logd("Received intent: " + intent.getAction());
            if (PhoneGlobals.getPhone().getIccRecordsLoaded() && isCarrierSupported()) {
                TelephonyManager.from(context).listen(this.mPhoneStateListener, 512);
            }
        }
    }

    private static void logd(String str) {
        Log.d(TAG, str);
    }
}
