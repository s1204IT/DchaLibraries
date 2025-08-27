package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.ITelephony;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.telephony.MtkTelephonyManagerEx;

/* loaded from: classes.dex */
public class RadioPowerController {
    private static final boolean ENG_LOAD;
    private static RadioPowerController sInstance = null;
    private Context mContext;
    private ISimManagementExt mExt;
    private MtkTelephonyManagerEx mTelEx = MtkTelephonyManagerEx.getDefault();

    static {
        ENG_LOAD = SystemProperties.get("ro.build.type").equals("eng") ? true : Log.isLoggable("RadioPowerController", 3);
    }

    private RadioPowerController(Context context) {
        this.mContext = context;
        this.mExt = UtilsExt.getSimManagementExt(this.mContext);
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

    public boolean setRadionOn(int i, boolean z) {
        logInEng("setRadioOn, turnOn=" + z + ", subId=" + i);
        boolean z2 = false;
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return false;
        }
        boolean zIsEccInProgress = this.mTelEx.isEccInProgress();
        if (!z && zIsEccInProgress) {
            Log.d("RadioPowerController", "Not allow to operate radio power during emergency call");
            Toast.makeText(this.mContext.getApplicationContext(), R.string.radio_off_during_emergency_call, 1).show();
            return false;
        }
        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if (iTelephonyAsInterface != null) {
            try {
                if (iTelephonyAsInterface.isRadioOnForSubscriber(i, this.mContext.getPackageName()) != z) {
                    boolean radioForSubscriber = iTelephonyAsInterface.setRadioForSubscriber(i, z);
                    if (radioForSubscriber) {
                        try {
                            updateRadioMsimDb(i, z);
                            this.mExt.setRadioPowerState(i, z);
                        } catch (RemoteException e) {
                            e = e;
                            z2 = radioForSubscriber;
                            Log.e("RadioPowerController", "setRadionOn, RemoteException=" + e);
                            logInEng("setRadionOn, isSuccessful=" + z2);
                            return z2;
                        }
                    }
                    z2 = radioForSubscriber;
                }
            } catch (RemoteException e2) {
                e = e2;
            }
        } else {
            logInEng("telephony is null.");
        }
        logInEng("setRadionOn, isSuccessful=" + z2);
        return z2;
    }

    private void updateRadioMsimDb(int i, boolean z) {
        int i2;
        int i3 = Settings.Global.getInt(this.mContext.getContentResolver(), "msim_mode_setting", -1);
        logInEng("updateRadioMsimDb, the current msim_mode=" + i3 + ", subId=" + i);
        boolean z2 = true;
        int slotIndex = 1 << SubscriptionManager.getSlotIndex(i);
        if ((i3 & slotIndex) > 0) {
            i2 = (~slotIndex) & i3;
        } else {
            i2 = slotIndex | i3;
            z2 = false;
        }
        logInEng("updateRadioMsimDb, currentSimMode=" + i2 + ", isPriviousRadioOn=" + z2 + ", turnOn=" + z);
        if (z != z2) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "msim_mode_setting", i2);
        } else {
            logInEng("updateRadioMsimDb, quickly click don't allow.");
        }
    }

    public boolean isRadioSwitchComplete(int i) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return false;
        }
        return isRadioSwitchComplete(i, TelephonyUtils.isRadioOn(i, this.mContext));
    }

    public boolean isRadioSwitchComplete(int i, boolean z) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return false;
        }
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        logInEng("isRadioSwitchComplete, slot=" + slotIndex + ", radioOn=" + z);
        if (z && (!isExpectedRadioStateOn(slotIndex) || !z)) {
            return false;
        }
        logInEng("isRadioSwitchComplete, done.");
        return true;
    }

    public boolean isExpectedRadioStateOn(int i) {
        boolean z = (Settings.Global.getInt(this.mContext.getContentResolver(), "msim_mode_setting", -1) & (1 << i)) != 0;
        logInEng("isExpectedRadioStateOn, slot=" + i + ", expectedRadioOn=" + z);
        return z;
    }

    private void logInEng(String str) {
        if (ENG_LOAD) {
            Log.d("RadioPowerController", str);
        }
    }
}
