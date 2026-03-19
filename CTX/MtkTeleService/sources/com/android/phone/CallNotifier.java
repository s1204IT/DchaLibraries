package com.android.phone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.SignalToneUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CallNotifier extends Handler {
    private static final boolean DBG;
    public static final int INTERNAL_SHOW_MESSAGE_NOTIFICATION_DONE = 22;
    private static final String LOG_TAG = "CallNotifier";
    public static final int PHONE_DISCONNECT = 3;
    public static final int PHONE_ENHANCED_VP_OFF = 10;
    public static final int PHONE_ENHANCED_VP_ON = 9;
    public static final int PHONE_STATE_DISPLAYINFO = 6;
    public static final int PHONE_STATE_SIGNALINFO = 7;
    public static final int PHONE_SUPP_SERVICE_FAILED = 14;
    public static final int PHONE_TTY_MODE_RECEIVED = 15;
    private static final int SHOW_MESSAGE_NOTIFICATION_TIME = 3000;
    private static final int TONE_RELATIVE_VOLUME_SIGNALINFO = 80;
    public static final int UPDATE_TYPE_CFI = 1;
    public static final int UPDATE_TYPE_MWI = 0;
    public static final int UPDATE_TYPE_MWI_CFI = 2;
    private static final boolean VDBG = false;
    private static CallNotifier sInstance;
    private PhoneGlobals mApplication;
    private AudioManager mAudioManager;
    private BluetoothHeadset mBluetoothHeadset;
    private CallManager mCM;
    private ToneGenerator mSignalInfoToneGenerator;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private Map<Integer, CallNotifierPhoneStateListener> mPhoneStateListeners = new ArrayMap();
    private Map<Integer, Boolean> mCFIStatus = new ArrayMap();
    private Map<Integer, Boolean> mMWIStatus = new ArrayMap();
    private boolean mVoicePrivacyState = false;
    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            CallNotifier.this.mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
        }

        @Override
        public void onServiceDisconnected(int i) {
            CallNotifier.this.mBluetoothHeadset = null;
        }
    };

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
    }

    static CallNotifier init(PhoneGlobals phoneGlobals) {
        CallNotifier callNotifier;
        synchronized (CallNotifier.class) {
            if (sInstance == null) {
                sInstance = new CallNotifier(phoneGlobals);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            callNotifier = sInstance;
        }
        return callNotifier;
    }

    private CallNotifier(PhoneGlobals phoneGlobals) {
        this.mApplication = phoneGlobals;
        this.mCM = phoneGlobals.mCM;
        this.mAudioManager = (AudioManager) this.mApplication.getSystemService("audio");
        this.mTelephonyManager = (TelephonyManager) this.mApplication.getSystemService("phone");
        this.mSubscriptionManager = (SubscriptionManager) this.mApplication.getSystemService("telephony_subscription_service");
        registerForNotifications();
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            defaultAdapter.getProfileProxy(this.mApplication.getApplicationContext(), this.mBluetoothProfileServiceListener, 1);
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                CallNotifier.this.updatePhoneStateListeners(true);
            }
        });
    }

    private void createSignalInfoToneGenerator() {
        if (this.mSignalInfoToneGenerator == null) {
            try {
                this.mSignalInfoToneGenerator = new ToneGenerator(0, 80);
                Log.d(LOG_TAG, "CallNotifier: mSignalInfoToneGenerator created when toneplay");
                return;
            } catch (RuntimeException e) {
                Log.w(LOG_TAG, "CallNotifier: Exception caught while creating mSignalInfoToneGenerator: " + e);
                this.mSignalInfoToneGenerator = null;
                return;
            }
        }
        Log.d(LOG_TAG, "mSignalInfoToneGenerator created already, hence skipping");
    }

    private void registerForNotifications() {
        this.mCM.registerForDisconnect(this, 3, (Object) null);
        this.mCM.registerForDisplayInfo(this, 6, (Object) null);
        this.mCM.registerForSignalInfo(this, 7, (Object) null);
        this.mCM.registerForInCallVoicePrivacyOn(this, 9, (Object) null);
        this.mCM.registerForInCallVoicePrivacyOff(this, 10, (Object) null);
        this.mCM.registerForSuppServiceFailed(this, 14, (Object) null);
        this.mCM.registerForTtyModeReceived(this, 15, (Object) null);
    }

    @Override
    public void handleMessage(Message message) {
        if (DBG) {
            Log.d(LOG_TAG, "handleMessage(" + message.what + ")");
        }
        switch (message.what) {
            case 3:
                if (DBG) {
                    log("DISCONNECT");
                }
                stopSignalInfoTone();
                break;
            case 6:
                if (DBG) {
                    log("Received PHONE_STATE_DISPLAYINFO event");
                }
                onDisplayInfo((AsyncResult) message.obj);
                break;
            case 7:
                if (DBG) {
                    log("Received PHONE_STATE_SIGNALINFO event");
                }
                onSignalInfo((AsyncResult) message.obj);
                break;
            case 9:
                if (DBG) {
                    log("PHONE_ENHANCED_VP_ON...");
                }
                if (!this.mVoicePrivacyState) {
                    new InCallTonePlayer(5).start();
                    this.mVoicePrivacyState = true;
                }
                break;
            case 10:
                if (DBG) {
                    log("PHONE_ENHANCED_VP_OFF...");
                }
                if (this.mVoicePrivacyState) {
                    new InCallTonePlayer(5).start();
                    this.mVoicePrivacyState = false;
                }
                break;
            case 14:
                if (DBG) {
                    log("PHONE_SUPP_SERVICE_FAILED...");
                }
                onSuppServiceFailed((AsyncResult) message.obj);
                break;
            case 15:
                if (DBG) {
                    log("Received PHONE_TTY_MODE_RECEIVED event");
                }
                onTtyModeReceived((AsyncResult) message.obj);
                break;
            case 22:
                if (DBG) {
                    log("Received Display Info notification done event ...");
                }
                PhoneDisplayMessage.dismissMessage();
                break;
        }
    }

    void updateCallNotifierRegistrationsAfterRadioTechnologyChange() {
        if (DBG) {
            Log.d(LOG_TAG, "updateCallNotifierRegistrationsAfterRadioTechnologyChange...");
        }
        createSignalInfoToneGenerator();
    }

    private void resetAudioStateAfterDisconnect() {
        if (this.mBluetoothHeadset != null) {
            this.mBluetoothHeadset.disconnectAudio();
        }
        PhoneUtils.turnOnSpeaker(this.mApplication, false, true);
        PhoneUtils.setAudioMode(this.mCM);
    }

    private class InCallTonePlayer extends Thread {
        public static final int TONE_BUSY = 2;
        public static final int TONE_CALL_ENDED = 4;
        public static final int TONE_CALL_WAITING = 1;
        public static final int TONE_CDMA_DROP = 8;
        public static final int TONE_CONGESTION = 3;
        public static final int TONE_INTERCEPT = 7;
        public static final int TONE_NONE = 0;
        static final int TONE_OFF = 0;
        static final int TONE_ON = 1;
        public static final int TONE_OTA_CALL_END = 11;
        public static final int TONE_OUT_OF_SERVICE = 9;
        public static final int TONE_REDIAL = 10;
        static final int TONE_RELATIVE_VOLUME_EMERGENCY = 100;
        static final int TONE_RELATIVE_VOLUME_HIPRI = 80;
        static final int TONE_RELATIVE_VOLUME_LOPRI = 50;
        public static final int TONE_REORDER = 6;
        static final int TONE_STOPPED = 2;
        static final int TONE_TIMEOUT_BUFFER = 20;
        public static final int TONE_UNOBTAINABLE_NUMBER = 13;
        public static final int TONE_VOICE_PRIVACY = 5;
        private int mState = 0;
        private int mToneId;

        InCallTonePlayer(int i) {
            this.mToneId = i;
        }

        @Override
        public void run() {
            int i;
            int i2;
            ToneGenerator toneGenerator;
            boolean z;
            boolean z2;
            int i3;
            CallNotifier.this.log("InCallTonePlayer.run(toneId = " + this.mToneId + ")...");
            int phoneType = CallNotifier.this.mCM.getFgPhone().getPhoneType();
            int i4 = this.mToneId;
            int i5 = 5000;
            int i6 = 80;
            if (i4 != 13) {
                switch (i4) {
                    case 1:
                        i3 = 22;
                        i5 = 2147483627;
                        i2 = i3;
                        toneGenerator = new ToneGenerator((CallNotifier.this.mBluetoothHeadset == null || !CallNotifier.this.mBluetoothHeadset.isAudioOn()) ? 0 : 6, i6);
                        if (toneGenerator != null) {
                            int ringerMode = CallNotifier.this.mAudioManager.getRingerMode();
                            if (phoneType != 2) {
                                z = true;
                                z2 = true;
                                synchronized (this) {
                                    if (z) {
                                        try {
                                            if (this.mState != 2) {
                                                this.mState = 1;
                                                toneGenerator.startTone(i2);
                                                try {
                                                    wait(i5 + 20);
                                                } catch (InterruptedException e) {
                                                    Log.w(CallNotifier.LOG_TAG, "InCallTonePlayer stopped: " + e);
                                                }
                                                if (z2) {
                                                    toneGenerator.stopTone();
                                                }
                                            }
                                        } finally {
                                        }
                                    }
                                    toneGenerator.release();
                                    this.mState = 0;
                                    break;
                                }
                            } else {
                                if (i2 == 93) {
                                    if (ringerMode != 0 && ringerMode != 1) {
                                        if (CallNotifier.DBG) {
                                            CallNotifier.this.log("- InCallTonePlayer: start playing call tone=" + i2);
                                        }
                                        z = true;
                                        z2 = false;
                                    }
                                    z2 = true;
                                    z = false;
                                } else if (i2 == 96 || i2 == 38 || i2 == 39 || i2 == 37 || i2 == 95) {
                                    if (ringerMode != 0) {
                                        if (CallNotifier.DBG) {
                                            CallNotifier.this.log("InCallTonePlayer:playing call fail tone:" + i2);
                                        }
                                        z = true;
                                        z2 = false;
                                    }
                                    z2 = true;
                                    z = false;
                                } else {
                                    if (i2 == 87 || i2 == 86) {
                                        if (ringerMode != 0 && ringerMode != 1) {
                                            if (CallNotifier.DBG) {
                                                CallNotifier.this.log("InCallTonePlayer:playing tone for toneType=" + i2);
                                            }
                                            z = true;
                                            z2 = false;
                                        }
                                        z2 = true;
                                        z = false;
                                    }
                                    z = true;
                                    z2 = true;
                                }
                                synchronized (this) {
                                }
                            }
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                            CallNotifier.this.resetAudioStateAfterDisconnect();
                            return;
                        }
                        return;
                    case 2:
                        if (phoneType != 2) {
                            if (phoneType != 1 && phoneType != 3 && phoneType != 5 && phoneType != 4) {
                                throw new IllegalStateException("Unexpected phone type: " + phoneType);
                            }
                            i = 17;
                            break;
                        } else {
                            i5 = TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR;
                            i2 = 96;
                            i6 = 50;
                            if (CallNotifier.this.mBluetoothHeadset == null) {
                                toneGenerator = new ToneGenerator((CallNotifier.this.mBluetoothHeadset == null || !CallNotifier.this.mBluetoothHeadset.isAudioOn()) ? 0 : 6, i6);
                                break;
                            }
                            if (toneGenerator != null) {
                            }
                            if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                            }
                        }
                        break;
                    case 3:
                        i = 18;
                        break;
                    case 4:
                        i3 = 27;
                        i5 = 200;
                        i2 = i3;
                        if (toneGenerator != null) {
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                        }
                        break;
                    case 5:
                        i2 = 86;
                        if (toneGenerator != null) {
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                        }
                        break;
                    case 6:
                        i2 = 38;
                        i5 = 4000;
                        if (toneGenerator != null) {
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                        }
                        break;
                    case 7:
                        i5 = TimeConsumingPreferenceActivity.RADIO_OFF_ERROR;
                        i2 = 37;
                        i6 = 50;
                        if (toneGenerator != null) {
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                        }
                        break;
                    case 8:
                    case 9:
                        i5 = 375;
                        i2 = 95;
                        i6 = 50;
                        if (toneGenerator != null) {
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                        }
                        break;
                    case 10:
                        i2 = 87;
                        i6 = 50;
                        if (toneGenerator != null) {
                        }
                        if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Bad toneId: " + this.mToneId);
                }
            } else {
                i = 21;
            }
            i2 = i;
            i5 = 4000;
            if (toneGenerator != null) {
            }
            if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
            }
        }
    }

    private void onDisplayInfo(AsyncResult asyncResult) {
        CdmaInformationRecords.CdmaDisplayInfoRec cdmaDisplayInfoRec = (CdmaInformationRecords.CdmaDisplayInfoRec) asyncResult.result;
        if (cdmaDisplayInfoRec != null) {
            String str = cdmaDisplayInfoRec.alpha;
            if (DBG) {
                log("onDisplayInfo: displayInfo=" + str);
            }
            PhoneDisplayMessage.displayNetworkMessage(this.mApplication, str);
            sendEmptyMessageDelayed(22, 3000L);
        }
    }

    private void onSuppServiceFailed(AsyncResult asyncResult) {
        String string;
        if (asyncResult.result == PhoneInternalInterface.SuppService.CONFERENCE) {
            if (DBG) {
                log("onSuppServiceFailed: displaying merge failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_conference);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.RESUME) {
            if (DBG) {
                log("onSuppServiceFailed: displaying merge failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_switch);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.HOLD) {
            if (DBG) {
                log("onSuppServiceFailed: displaying hold failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_hold);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.TRANSFER) {
            if (DBG) {
                log("onSuppServiceFailed: displaying transfer failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_transfer);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.SEPARATE) {
            if (DBG) {
                log("onSuppServiceFailed: displaying separate failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_separate);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.SWITCH) {
            if (DBG) {
                log("onSuppServiceFailed: displaying switch failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_switch);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.REJECT) {
            if (DBG) {
                log("onSuppServiceFailed: displaying reject failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_reject);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.HANGUP) {
            if (DBG) {
                log("onSuppServiceFailed: displaying hangup failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_hangup);
        } else if (asyncResult.result == PhoneInternalInterface.SuppService.UNKNOWN) {
            if (DBG) {
                log("onSuppServiceFailed: displaying unknown failure message");
            }
            string = this.mApplication.getResources().getString(R.string.incall_error_supp_service_unknown);
        } else {
            if (DBG) {
                log("onSuppServiceFailed: unknown failure");
                return;
            }
            return;
        }
        PhoneDisplayMessage.displayErrorMessage(this.mApplication, string);
        sendEmptyMessageDelayed(22, 3000L);
    }

    public void updatePhoneStateListeners(boolean z) {
        updatePhoneStateListeners(z, 2, -1);
    }

    public void updatePhoneStateListeners(boolean z, int i, int i2) {
        int i3;
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        ArrayList arrayList = new ArrayList(this.mPhoneStateListeners.keySet());
        Collections.sort(arrayList, new Comparator<Integer>() {
            @Override
            public int compare(Integer num, Integer num2) {
                return SubscriptionController.getInstance().getSlotIndex(num.intValue()) > SubscriptionController.getInstance().getSlotIndex(num2.intValue()) ? 0 : -1;
            }
        });
        int size = arrayList.size() - 1;
        while (true) {
            if (size < 0) {
                break;
            }
            int iIntValue = ((Integer) arrayList.get(size)).intValue();
            if (activeSubscriptionInfoList == null || !containsSubId(activeSubscriptionInfoList, iIntValue)) {
                Log.d(LOG_TAG, "updatePhoneStateListeners: Hide the outstanding notifications.");
                this.mApplication.notificationMgr.updateMwi(iIntValue, false);
                this.mApplication.notificationMgr.updateCfi(iIntValue, false);
                this.mTelephonyManager.listen(this.mPhoneStateListeners.get(Integer.valueOf(iIntValue)), 0);
                this.mPhoneStateListeners.remove(Integer.valueOf(iIntValue));
            } else {
                Log.d(LOG_TAG, "updatePhoneStateListeners: update CF notifications.");
                if (this.mCFIStatus.containsKey(Integer.valueOf(iIntValue))) {
                    if (i == 1 && iIntValue == i2) {
                        this.mApplication.notificationMgr.updateCfi(iIntValue, this.mCFIStatus.get(Integer.valueOf(iIntValue)).booleanValue(), z);
                    } else {
                        this.mApplication.notificationMgr.updateCfi(iIntValue, this.mCFIStatus.get(Integer.valueOf(iIntValue)).booleanValue(), true);
                    }
                }
                if (this.mMWIStatus.containsKey(Integer.valueOf(iIntValue))) {
                    if (i == 0 && iIntValue == i2) {
                        this.mApplication.notificationMgr.updateMwi(iIntValue, this.mMWIStatus.get(Integer.valueOf(iIntValue)).booleanValue(), z);
                    } else {
                        this.mApplication.notificationMgr.updateMwi(iIntValue, this.mMWIStatus.get(Integer.valueOf(iIntValue)).booleanValue(), true);
                    }
                }
            }
            size--;
        }
        if (activeSubscriptionInfoList == null) {
            return;
        }
        for (i3 = 0; i3 < activeSubscriptionInfoList.size(); i3++) {
            int subscriptionId = activeSubscriptionInfoList.get(i3).getSubscriptionId();
            if (!this.mPhoneStateListeners.containsKey(Integer.valueOf(subscriptionId))) {
                CallNotifierPhoneStateListener callNotifierPhoneStateListener = new CallNotifierPhoneStateListener(subscriptionId);
                this.mTelephonyManager.listen(callNotifierPhoneStateListener, 12);
                this.mPhoneStateListeners.put(Integer.valueOf(subscriptionId), callNotifierPhoneStateListener);
            }
        }
    }

    private boolean containsSubId(List<SubscriptionInfo> list, int i) {
        if (list == null) {
            return false;
        }
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (list.get(i2).getSubscriptionId() == i) {
                return true;
            }
        }
        return false;
    }

    private void onTtyModeReceived(AsyncResult asyncResult) {
        if (DBG) {
            log("TtyModeReceived: displaying notification message");
        }
        int i = 0;
        switch (((Integer) asyncResult.result).intValue()) {
            case 0:
                i = android.R.string.fcComplete;
                break;
            case 1:
                i = android.R.string.fast_scroll_alphabet;
                break;
            case 2:
                i = android.R.string.fast_scroll_numeric_alphabet;
                break;
            case 3:
                i = android.R.string.fcError;
                break;
            default:
                Log.e(LOG_TAG, "Unsupported TTY mode: " + asyncResult.result);
                break;
        }
        if (i != 0) {
            PhoneDisplayMessage.displayNetworkMessage(this.mApplication, this.mApplication.getResources().getString(i));
            sendEmptyMessageDelayed(22, 3000L);
        }
    }

    private class SignalInfoTonePlayer extends Thread {
        private int mToneId;

        SignalInfoTonePlayer(int i) {
            this.mToneId = i;
        }

        @Override
        public void run() {
            CallNotifier.this.log("SignalInfoTonePlayer.run(toneId = " + this.mToneId + ")...");
            CallNotifier.this.createSignalInfoToneGenerator();
            if (CallNotifier.this.mSignalInfoToneGenerator != null) {
                CallNotifier.this.mSignalInfoToneGenerator.stopTone();
                CallNotifier.this.mSignalInfoToneGenerator.startTone(this.mToneId);
            }
        }
    }

    private void onSignalInfo(AsyncResult asyncResult) {
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w(LOG_TAG, "Got onSignalInfo() on non-voice-capable device! Ignoring...");
            return;
        }
        if (PhoneUtils.isRealIncomingCall(this.mCM.getFirstActiveRingingCall().getState())) {
            stopSignalInfoTone();
            return;
        }
        CdmaInformationRecords.CdmaSignalInfoRec cdmaSignalInfoRec = (CdmaInformationRecords.CdmaSignalInfoRec) asyncResult.result;
        if (cdmaSignalInfoRec != null) {
            boolean z = cdmaSignalInfoRec.isPresent;
            if (DBG) {
                log("onSignalInfo: isPresent=" + z);
            }
            if (z) {
                int i = cdmaSignalInfoRec.signalType;
                int i2 = cdmaSignalInfoRec.alertPitch;
                int i3 = cdmaSignalInfoRec.signal;
                if (DBG) {
                    log("onSignalInfo: uSignalType=" + i + ", uAlertPitch=" + i2 + ", uSignal=" + i3);
                }
                new SignalInfoTonePlayer(SignalToneUtil.getAudioToneFromSignalInfo(i, i2, i3)).start();
            }
        }
    }

    void stopSignalInfoTone() {
        if (DBG) {
            log("stopSignalInfoTone: Stopping SignalInfo tone player");
        }
        new SignalInfoTonePlayer(98).start();
    }

    private class CallNotifierPhoneStateListener extends PhoneStateListener {
        public CallNotifierPhoneStateListener(int i) {
            super(Integer.valueOf(i));
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean z) {
            CallNotifier.this.mMWIStatus.put(this.mSubId, Boolean.valueOf(z));
            CallNotifier.this.updatePhoneStateListeners(false, 0, this.mSubId.intValue());
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean z) {
            StringBuilder sb = new StringBuilder();
            sb.append("onCallForwardingIndicatorChanged(): subId=");
            sb.append(this.mSubId);
            sb.append(", visible=");
            sb.append(z ? "Y" : "N");
            Log.i(CallNotifier.LOG_TAG, sb.toString());
            CallNotifier.this.mCFIStatus.put(this.mSubId, Boolean.valueOf(z));
            CallNotifier.this.updatePhoneStateListeners(false, 1, this.mSubId.intValue());
        }
    }

    private void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
