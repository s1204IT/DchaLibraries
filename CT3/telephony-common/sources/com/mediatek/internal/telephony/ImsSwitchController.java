package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.ims.internal.IImsService;
import com.android.internal.telephony.CommandsInterface;

public class ImsSwitchController extends Handler {
    static final int DISABLE_WIFI_FLIGHTMODE = 1;
    protected static final int EVENT_CONNECTIVITY_CHANGE = 6;
    protected static final int EVENT_DC_SWITCH_STATE_CHANGE = 5;
    protected static final int EVENT_IMS_DEREGISTER_TIMEOUT = 7;
    protected static final int EVENT_RADIO_AVAILABLE_PHONE1 = 2;
    protected static final int EVENT_RADIO_AVAILABLE_PHONE2 = 4;
    protected static final int EVENT_RADIO_NOT_AVAILABLE_PHONE1 = 1;
    protected static final int EVENT_RADIO_NOT_AVAILABLE_PHONE2 = 3;
    public static final String IMS_SERVICE = "ims";
    static final String LOG_TAG = "ImsSwitchController";
    private static final String MULTI_IMS_SUPPORT = "ro.mtk_multiple_ims_support";
    public static final String NW_SUB_TYPE_IMS = "IMS";
    public static final String NW_TYPE_WIFI = "MOBILE_IMS";
    private static final String PROPERTY_IMS_VIDEO_ENALBE = "persist.mtk.ims.video.enable";
    private static final String PROPERTY_VOLTE_ENALBE = "persist.mtk.volte.enable";
    private static final String PROPERTY_WFC_ENALBE = "persist.mtk.wfc.enable";
    private static IImsService mImsService = null;
    private CommandsInterface[] mCi;
    private Context mContext;
    private int mPhoneCount;
    private RadioPowerInterface mRadioPowerIf;
    private boolean mIsInVoLteCall = false;
    private ImsServiceDeathRecipient mDeathRecipient = new ImsServiceDeathRecipient(this, null);
    private boolean mNeedTurnOffWifi = false;
    private int REASON_INVALID = -1;
    private int mReason = this.REASON_INVALID;
    protected final Object mLock = new Object();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            ImsSwitchController.log("mIntentReceiver Receive action " + action);
        }
    };

    ImsSwitchController(Context context, int phoneCount, CommandsInterface[] ci) {
        log("Initialize ImsSwitchController");
        this.mContext = context;
        this.mCi = ci;
        this.mPhoneCount = phoneCount;
        if (!SystemProperties.get("persist.mtk_ims_support").equals("1") || SystemProperties.get("ro.mtk_tc1_feature").equals("1")) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter("com.android.ims.IMS_SERVICE_DOWN");
        if (SystemProperties.get("persist.mtk_wfc_support").equals("1")) {
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        }
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter);
        this.mRadioPowerIf = new RadioPowerInterface();
        RadioManager.registerForRadioPowerChange(LOG_TAG, this.mRadioPowerIf);
        this.mCi[0].registerForNotAvailable(this, 1, null);
        this.mCi[0].registerForAvailable(this, 2, null);
        if (this.mPhoneCount <= 1) {
            return;
        }
        this.mCi[1].registerForNotAvailable(this, 3, null);
        this.mCi[1].registerForAvailable(this, 4, null);
    }

    class RadioPowerInterface implements IRadioPower {
        RadioPowerInterface() {
        }

        @Override
        public void notifyRadioPowerChange(boolean power, int phoneId) {
            ImsSwitchController.log("notifyRadioPowerChange, power:" + power + " phoneId:" + phoneId);
            if (SystemProperties.getInt(ImsSwitchController.MULTI_IMS_SUPPORT, 1) == 1 && RadioCapabilitySwitchUtil.getMainCapabilityPhoneId() != phoneId) {
                ImsSwitchController.log("radio power change ignore due to phone id isn't LTE phone");
                return;
            }
            if (ImsSwitchController.mImsService == null) {
                ImsSwitchController.this.checkAndBindImsService(phoneId);
            }
            if (ImsSwitchController.mImsService != null) {
                try {
                    int radioState = power ? CommandsInterface.RadioState.RADIO_ON.ordinal() : CommandsInterface.RadioState.RADIO_OFF.ordinal();
                    ImsSwitchController.mImsService.updateRadioState(radioState, phoneId);
                } catch (RemoteException e) {
                    ImsSwitchController.log("RemoteException can't notify power state change");
                }
            } else {
                ImsSwitchController.log("notifyRadioPowerChange: ImsService not ready !!!");
            }
            ImsSwitchController.log("radio power change processed");
        }
    }

    private void checkAndBindImsService(int phoneId) {
        IBinder b = ServiceManager.getService(IMS_SERVICE);
        if (b != null) {
            try {
                b.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
        }
        mImsService = IImsService.Stub.asInterface(b);
        log("checkAndBindImsService: mImsService = " + mImsService);
    }

    private class ImsServiceDeathRecipient implements IBinder.DeathRecipient {
        ImsServiceDeathRecipient(ImsSwitchController this$0, ImsServiceDeathRecipient imsServiceDeathRecipient) {
            this();
        }

        private ImsServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            IImsService unused = ImsSwitchController.mImsService = null;
        }
    }

    private void registerEvent() {
        log("registerEvent, major phoneid:" + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId());
    }

    private void unregisterEvent() {
        log("unregisterEvent, major phoneid:" + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId());
    }

    private String eventIdtoString(int what) {
        switch (what) {
            case 1:
                return "RADIO_NOT_AVAILABLE_PHONE1";
            case 2:
                return "RADIO_AVAILABLE_PHONE1";
            case 3:
                return "RADIO_NOT_AVAILABLE_PHONE2";
            case 4:
                return "RADIO_AVAILABLE_PHONE2";
            default:
                return null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage msg.what: " + eventIdtoString(msg.what));
        switch (msg.what) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
