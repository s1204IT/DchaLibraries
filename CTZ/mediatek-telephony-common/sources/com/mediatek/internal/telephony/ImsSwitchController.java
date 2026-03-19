package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandsInterface;
import com.mediatek.ims.internal.IMtkImsService;
import com.mediatek.ims.internal.MtkImsManager;

public class ImsSwitchController extends Handler {
    private static final int BIND_IMS_SERVICE_DELAY_IN_MILLIS = 2000;
    static final String LOG_TAG = "ImsSwitchController";
    private static IMtkImsService mMtkImsService = null;
    private CommandsInterface[] mCi;
    private Context mContext;
    private int mPhoneCount;
    private RadioPowerInterface mRadioPowerIf;
    private ImsServiceDeathRecipient mDeathRecipient = new ImsServiceDeathRecipient();
    private Runnable mBindImsServiceRunnable = new Runnable() {
        @Override
        public void run() {
            Rlog.w(ImsSwitchController.LOG_TAG, "try to bind ImsService again");
            if (ImsSwitchController.this.checkAndBindImsService(0)) {
                ImsSwitchController.log("manually updateImsServiceConfig");
                if (!MtkImsManager.isSupportMims()) {
                    ImsManager.updateImsServiceConfig(ImsSwitchController.this.mContext, RadioCapabilitySwitchUtil.getMainCapabilityPhoneId(), true);
                    return;
                } else {
                    for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                        ImsManager.updateImsServiceConfig(ImsSwitchController.this.mContext, i, true);
                    }
                    return;
                }
            }
            ImsSwitchController.this.postDelayed(this, 2000L);
        }
    };

    ImsSwitchController(Context context, int i, CommandsInterface[] commandsInterfaceArr) {
        log("Initialize ImsSwitchController");
        this.mContext = context;
        this.mCi = commandsInterfaceArr;
        this.mPhoneCount = i;
        if (SystemProperties.get("persist.vendor.ims_support").equals("1") && !SystemProperties.get("ro.vendor.mtk_tc1_feature").equals("1")) {
            this.mRadioPowerIf = new RadioPowerInterface();
            RadioManager.registerForRadioPowerChange(LOG_TAG, this.mRadioPowerIf);
            if (mMtkImsService == null) {
                checkAndBindImsService(0);
            }
        }
    }

    class RadioPowerInterface implements IRadioPower {
        RadioPowerInterface() {
        }

        @Override
        public void notifyRadioPowerChange(boolean z, int i) {
            ImsSwitchController.log("notifyRadioPowerChange, power:" + z + " phoneId:" + i);
            if (!MtkImsManager.isSupportMims() && RadioCapabilitySwitchUtil.getMainCapabilityPhoneId() != i) {
                ImsSwitchController.log("radio power change ignore due to phone id isn't LTE phone");
                return;
            }
            if (!SystemProperties.get("ro.vendor.md_auto_setup_ims").equals("1")) {
                if (ImsSwitchController.mMtkImsService == null) {
                    ImsSwitchController.this.checkAndBindImsService(i);
                }
                if (ImsSwitchController.mMtkImsService != null) {
                    try {
                        ImsSwitchController.mMtkImsService.updateRadioState((z ? CommandsInterface.RadioState.RADIO_ON : CommandsInterface.RadioState.RADIO_OFF).ordinal(), i);
                    } catch (RemoteException e) {
                        Rlog.e(ImsSwitchController.LOG_TAG, "RemoteException can't notify power state change");
                    }
                } else {
                    Rlog.w(ImsSwitchController.LOG_TAG, "notifyRadioPowerChange: ImsService not ready !!!");
                }
                ImsSwitchController.log("radio power change processed");
                return;
            }
            ImsSwitchController.log("[" + i + "] Modem auto registration so that we don't triggerImsService updateRadioState");
        }
    }

    private boolean checkAndBindImsService(int i) {
        IBinder service = ServiceManager.getService("mtkIms");
        if (service != null) {
            try {
                service.linkToDeath(this.mDeathRecipient, 0);
                mMtkImsService = IMtkImsService.Stub.asInterface(service);
                log("checkAndBindImsService: mMtkImsService = " + mMtkImsService);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    private class ImsServiceDeathRecipient implements IBinder.DeathRecipient {
        private ImsServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            Rlog.w(ImsSwitchController.LOG_TAG, "ImsService died detected");
            IMtkImsService unused = ImsSwitchController.mMtkImsService = null;
            ImsSwitchController.this.postDelayed(ImsSwitchController.this.mBindImsServiceRunnable, 2000L);
        }
    }

    private static void log(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
