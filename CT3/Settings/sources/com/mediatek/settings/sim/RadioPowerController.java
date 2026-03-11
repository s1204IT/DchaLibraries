package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;

public class RadioPowerController {
    private static RadioPowerController sInstance = null;
    private Context mContext;
    private ISimManagementExt mExt;

    private RadioPowerController(Context context) {
        this.mContext = context;
        this.mExt = UtilsExt.getSimManagmentExtPlugin(this.mContext);
    }

    private static synchronized void createInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RadioPowerController(context);
        }
    }

    public static RadioPowerController getInstance(Context context) {
        if (sInstance == null) {
            createInstance(context);
        }
        return sInstance;
    }

    public boolean setRadionOn(int subId, boolean turnOn) {
        Log.d("RadioPowerController", "setRadioOn, turnOn: " + turnOn + ", subId = " + subId);
        boolean isSuccessful = false;
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        try {
            if (telephony != null) {
                isSuccessful = telephony.setRadioForSubscriber(subId, turnOn);
                if (isSuccessful) {
                    updateRadioMsimDb(subId, turnOn);
                    this.mExt.setRadioPowerState(subId, turnOn);
                }
            } else {
                Log.d("RadioPowerController", "telephony is null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d("RadioPowerController", "setRadionOn, isSuccessful: " + isSuccessful);
        return isSuccessful;
    }

    private void updateRadioMsimDb(int subId, boolean turnOn) {
        int currentSimMode;
        boolean isPriviousRadioOn;
        int priviousSimMode = Settings.Global.getInt(this.mContext.getContentResolver(), "msim_mode_setting", -1);
        Log.i("RadioPowerController", "updateRadioMsimDb, The current dual sim mode is " + priviousSimMode + ", with subId = " + subId);
        int slot = SubscriptionManager.getSlotId(subId);
        int modeSlot = 1 << slot;
        if ((priviousSimMode & modeSlot) > 0) {
            currentSimMode = priviousSimMode & (~modeSlot);
            isPriviousRadioOn = true;
        } else {
            currentSimMode = priviousSimMode | modeSlot;
            isPriviousRadioOn = false;
        }
        Log.d("RadioPowerController", "currentSimMode=" + currentSimMode + " isPriviousRadioOn =" + isPriviousRadioOn + ", turnOn: " + turnOn);
        if (turnOn != isPriviousRadioOn) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "msim_mode_setting", currentSimMode);
        } else {
            Log.w("RadioPowerController", "quickly click don't allow.");
        }
    }

    public boolean isRadioSwitchComplete(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean isRadioOn = TelephonyUtils.isRadioOn(subId, this.mContext);
        Log.d("RadioPowerController", "isRadioSwitchComplete: slot: " + slotId + ", isRadioOn: " + isRadioOn);
        if (isRadioOn) {
            return isExpectedRadioStateOn(slotId) && isRadioOn;
        }
        return true;
    }

    public boolean isExpectedRadioStateOn(int slot) {
        int currentSimMode = Settings.Global.getInt(this.mContext.getContentResolver(), "msim_mode_setting", -1);
        boolean expectedRadioOn = ((1 << slot) & currentSimMode) != 0;
        Log.d("RadioPowerController", "isExpectedRadioStateOn: slot: " + slot + ", expectedRadioOn: " + expectedRadioOn);
        return expectedRadioOn;
    }
}
