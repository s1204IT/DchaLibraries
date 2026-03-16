package com.android.phone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.SignalToneUtil;
import com.android.phone.OtaUtils;
import com.android.phone.PhoneGlobals;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CallNotifier extends Handler {
    private static final boolean DBG;
    private static final AudioAttributes VIBRATION_ATTRIBUTES;
    private static CallNotifier sInstance;
    private PhoneGlobals mApplication;
    private AudioManager mAudioManager;
    private BluetoothHeadset mBluetoothHeadset;
    private final BluetoothManager mBluetoothManager;
    private CallManager mCM;
    private CallLogger mCallLogger;
    private Call.State mPreviousCdmaCallState;
    private ToneGenerator mSignalInfoToneGenerator;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private Object mCallerInfoQueryStateGuard = new Object();
    private Map<Integer, CallNotifierPhoneStateListener> mPhoneStateListeners = new ArrayMap();
    private boolean mVoicePrivacyState = false;
    private boolean mIsCdmaRedialCall = false;
    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            CallNotifier.this.mBluetoothHeadset = (BluetoothHeadset) proxy;
            CallNotifier.this.log("- Got BluetoothHeadset: " + CallNotifier.this.mBluetoothHeadset);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            CallNotifier.this.mBluetoothHeadset = null;
        }
    };

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(1).setUsage(2).build();
    }

    static CallNotifier init(PhoneGlobals app, CallLogger callLogger, CallStateMonitor callStateMonitor, BluetoothManager bluetoothManager) {
        CallNotifier callNotifier;
        synchronized (CallNotifier.class) {
            if (sInstance == null) {
                sInstance = new CallNotifier(app, callLogger, callStateMonitor, bluetoothManager);
            } else {
                Log.wtf("CallNotifier", "init() called multiple times!  sInstance = " + sInstance);
            }
            callNotifier = sInstance;
        }
        return callNotifier;
    }

    private CallNotifier(PhoneGlobals app, CallLogger callLogger, CallStateMonitor callStateMonitor, BluetoothManager bluetoothManager) {
        this.mApplication = app;
        this.mCM = app.mCM;
        this.mCallLogger = callLogger;
        this.mBluetoothManager = bluetoothManager;
        this.mAudioManager = (AudioManager) this.mApplication.getSystemService("audio");
        this.mTelephonyManager = (TelephonyManager) this.mApplication.getSystemService("phone");
        this.mSubscriptionManager = (SubscriptionManager) this.mApplication.getSystemService("telephony_subscription_service");
        callStateMonitor.addListener(this);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this.mApplication.getApplicationContext(), this.mBluetoothProfileServiceListener, 1);
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                CallNotifier.this.updatePhoneStateListeners();
            }
        });
    }

    private void createSignalInfoToneGenerator() {
        if (this.mSignalInfoToneGenerator == null) {
            try {
                this.mSignalInfoToneGenerator = new ToneGenerator(0, 80);
                Log.d("CallNotifier", "CallNotifier: mSignalInfoToneGenerator created when toneplay");
                return;
            } catch (RuntimeException e) {
                Log.w("CallNotifier", "CallNotifier: Exception caught while creating mSignalInfoToneGenerator: " + e);
                this.mSignalInfoToneGenerator = null;
                return;
            }
        }
        Log.d("CallNotifier", "mSignalInfoToneGenerator created already, hence skipping");
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                onPhoneStateChanged((AsyncResult) msg.obj);
                break;
            case 2:
                log("RINGING... (new)");
                onNewRingingConnection((AsyncResult) msg.obj);
                break;
            case 3:
                if (DBG) {
                    log("DISCONNECT");
                }
                onDisconnect((AsyncResult) msg.obj);
                break;
            case 4:
                onUnknownConnectionAppeared((AsyncResult) msg.obj);
                break;
            case 6:
                if (DBG) {
                    log("Received PHONE_STATE_DISPLAYINFO event");
                }
                onDisplayInfo((AsyncResult) msg.obj);
                break;
            case 7:
                if (DBG) {
                    log("Received PHONE_STATE_SIGNALINFO event");
                }
                onSignalInfo((AsyncResult) msg.obj);
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
                onSuppServiceFailed((AsyncResult) msg.obj);
                break;
            case 15:
                if (DBG) {
                    log("Received PHONE_TTY_MODE_RECEIVED event");
                }
                onTtyModeReceived((AsyncResult) msg.obj);
                break;
            case 20:
                if (DBG) {
                    log("EVENT_OTA_PROVISION_CHANGE...");
                }
                this.mApplication.handleOtaspEvent(msg);
                break;
            case 22:
                if (DBG) {
                    log("Received Display Info notification done event ...");
                }
                PhoneDisplayMessage.dismissMessage();
                break;
        }
    }

    private void onNewRingingConnection(AsyncResult r) {
        Connection c = (Connection) r.result;
        log("onNewRingingConnection(): state = " + this.mCM.getState() + ", conn = { " + c + " }");
        Call ringing = c.getCall();
        Phone phone = ringing.getPhone();
        if (ignoreAllIncomingCalls(phone)) {
            PhoneUtils.hangupRingingCall(ringing);
            return;
        }
        if (!c.isRinging()) {
            Log.i("CallNotifier", "CallNotifier.onNewRingingConnection(): connection not ringing!");
            return;
        }
        stopSignalInfoTone();
        Call.State state = c.getState();
        log("- connection is ringing!  state = " + state);
        log("Holding wake lock on new incoming connection.");
        this.mApplication.requestWakeState(PhoneGlobals.WakeState.PARTIAL);
        log("- onNewRingingConnection() done.");
    }

    private boolean ignoreAllIncomingCalls(Phone phone) {
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w("CallNotifier", "Got onNewRingingConnection() on non-voice-capable device! Ignoring...");
            return true;
        }
        if (PhoneUtils.isPhoneInEcm(phone)) {
            if (DBG) {
                log("Incoming call while in ECM: always allow...");
            }
            return false;
        }
        boolean provisioned = Settings.Global.getInt(this.mApplication.getContentResolver(), "device_provisioned", 0) != 0;
        if (!provisioned) {
            Log.i("CallNotifier", "Ignoring incoming call: not provisioned");
            return true;
        }
        if (TelephonyCapabilities.supportsOtasp(phone)) {
            boolean activateState = this.mApplication.cdmaOtaScreenState.otaScreenState == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION;
            boolean dialogState = this.mApplication.cdmaOtaScreenState.otaScreenState == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG;
            boolean spcState = this.mApplication.cdmaOtaProvisionData.inOtaSpcState;
            if (spcState) {
                Log.i("CallNotifier", "Ignoring incoming call: OTA call is active");
                return true;
            }
            if (activateState || dialogState) {
                if (dialogState) {
                    this.mApplication.dismissOtaDialogs();
                }
                this.mApplication.clearOtaState();
                return false;
            }
        }
        return false;
    }

    private void onUnknownConnectionAppeared(AsyncResult r) {
        PhoneConstants.State state = this.mCM.getState();
        if (state == PhoneConstants.State.OFFHOOK) {
            if (DBG) {
                log("unknown connection appeared...");
            }
            onPhoneStateChanged(r);
        }
    }

    private void onPhoneStateChanged(AsyncResult r) {
        PhoneConstants.State state = this.mCM.getState();
        log("onPhoneStateChanged: state = " + state);
        this.mApplication.notificationMgr.statusBarHelper.enableNotificationAlerts(state == PhoneConstants.State.IDLE);
        Phone fgPhone = this.mCM.getFgPhone();
        if (fgPhone.getPhoneType() == 2) {
            if (fgPhone.getForegroundCall().getState() == Call.State.ACTIVE && (this.mPreviousCdmaCallState == Call.State.DIALING || this.mPreviousCdmaCallState == Call.State.ALERTING)) {
                if (this.mIsCdmaRedialCall) {
                    new InCallTonePlayer(10).start();
                }
                stopSignalInfoTone();
            }
            this.mPreviousCdmaCallState = fgPhone.getForegroundCall().getState();
        }
        this.mBluetoothManager.updateBluetoothIndication();
        this.mApplication.updatePhoneState(state);
        if (state == PhoneConstants.State.OFFHOOK) {
            log("onPhoneStateChanged: OFF HOOK");
            PhoneUtils.setAudioMode(this.mCM);
        }
    }

    void updateCallNotifierRegistrationsAfterRadioTechnologyChange() {
        if (DBG) {
            Log.d("CallNotifier", "updateCallNotifierRegistrationsAfterRadioTechnologyChange...");
        }
        createSignalInfoToneGenerator();
    }

    private void onDisconnect(AsyncResult r) {
        int cause;
        log("onDisconnect()...  CallManager state: " + this.mCM.getState());
        this.mVoicePrivacyState = false;
        Connection c = (Connection) r.result;
        if (c != null) {
            log("onDisconnect: cause = " + DisconnectCause.toString(c.getDisconnectCause()) + ", incoming = " + c.isIncoming() + ", date = " + c.getCreateTime());
        } else {
            Log.w("CallNotifier", "onDisconnect: null connection");
        }
        int autoretrySetting = 0;
        if (c != null && c.getCall().getPhone().getPhoneType() == 2) {
            autoretrySetting = Settings.Global.getInt(this.mApplication.getContentResolver(), "call_auto_retry", 0);
        }
        stopSignalInfoTone();
        if (c != null && c.getCall().getPhone().getPhoneType() == 2) {
            this.mApplication.cdmaPhoneCallState.resetCdmaPhoneCallState();
        }
        if (c != null && TelephonyCapabilities.supportsOtasp(c.getCall().getPhone())) {
            if (c.getCall().getPhone().isOtaSpNumber(c.getAddress())) {
                if (DBG) {
                    log("onDisconnect: this was an OTASP call!");
                }
                this.mApplication.handleOtaspDisconnect();
            }
        }
        int toneToPlay = 0;
        if (0 == 0 && this.mCM.getState() == PhoneConstants.State.IDLE && c != null && ((cause = c.getDisconnectCause()) == 2 || cause == 3)) {
            log("- need to play CALL_ENDED tone!");
            toneToPlay = 4;
            this.mIsCdmaRedialCall = false;
        }
        if (this.mCM.getState() == PhoneConstants.State.IDLE && toneToPlay == 0) {
            resetAudioStateAfterDisconnect();
        }
        if (c != null) {
            this.mCallLogger.logCall(c);
            String number = c.getAddress();
            Phone phone = c.getCall().getPhone();
            boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mApplication, number);
            if (toneToPlay != 0) {
                log("- starting post-disconnect tone (" + toneToPlay + ")...");
                new InCallTonePlayer(toneToPlay).start();
            }
            int cause2 = c.getDisconnectCause();
            if ((this.mPreviousCdmaCallState == Call.State.DIALING || this.mPreviousCdmaCallState == Call.State.ALERTING) && !isEmergencyNumber && cause2 != 1 && cause2 != 2 && cause2 != 3 && cause2 != 16) {
                if (!this.mIsCdmaRedialCall) {
                    if (autoretrySetting == 1) {
                        int status = PhoneUtils.placeCall(this.mApplication, phone, number, null, false);
                        if (status != 2) {
                            this.mIsCdmaRedialCall = true;
                            return;
                        }
                        return;
                    }
                    this.mIsCdmaRedialCall = false;
                    return;
                }
                this.mIsCdmaRedialCall = false;
            }
        }
    }

    private void resetAudioStateAfterDisconnect() {
        log("resetAudioStateAfterDisconnect()...");
        if (this.mBluetoothHeadset != null) {
            this.mBluetoothHeadset.disconnectAudio();
        }
        PhoneUtils.turnOnSpeaker(this.mApplication, false, true);
        PhoneUtils.setAudioMode(this.mCM);
    }

    private class InCallTonePlayer extends Thread {
        private int mState = 0;
        private int mToneId;

        InCallTonePlayer(int toneId) {
            this.mToneId = toneId;
        }

        @Override
        public void run() {
            int toneType;
            int toneVolume;
            int toneLengthMillis;
            ToneGenerator toneGenerator;
            int stream = 0;
            CallNotifier.this.log("InCallTonePlayer.run(toneId = " + this.mToneId + ")...");
            int phoneType = CallNotifier.this.mCM.getFgPhone().getPhoneType();
            switch (this.mToneId) {
                case 1:
                    toneType = 22;
                    toneVolume = 80;
                    toneLengthMillis = 2147483627;
                    break;
                case 2:
                    if (phoneType == 2) {
                        toneType = 96;
                        toneVolume = 50;
                        toneLengthMillis = 1000;
                    } else if (phoneType == 1 || phoneType == 3 || phoneType == 5 || phoneType == 4) {
                        toneType = 17;
                        toneVolume = 80;
                        toneLengthMillis = 4000;
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    break;
                case 3:
                    toneType = 18;
                    toneVolume = 80;
                    toneLengthMillis = 4000;
                    break;
                case 4:
                    toneType = 27;
                    toneVolume = 80;
                    toneLengthMillis = 200;
                    break;
                case 5:
                    toneType = 86;
                    toneVolume = 80;
                    toneLengthMillis = 5000;
                    break;
                case 6:
                    toneType = 38;
                    toneVolume = 80;
                    toneLengthMillis = 4000;
                    break;
                case 7:
                    toneType = 37;
                    toneVolume = 50;
                    toneLengthMillis = 500;
                    break;
                case 8:
                case 9:
                    toneType = 95;
                    toneVolume = 50;
                    toneLengthMillis = 375;
                    break;
                case 10:
                    toneType = 87;
                    toneVolume = 50;
                    toneLengthMillis = 5000;
                    break;
                case 11:
                    if (CallNotifier.this.mApplication.cdmaOtaConfigData.otaPlaySuccessFailureTone == 1) {
                        toneType = 93;
                        toneVolume = 80;
                        toneLengthMillis = 750;
                    } else {
                        toneType = 27;
                        toneVolume = 80;
                        toneLengthMillis = 200;
                    }
                    break;
                case 12:
                default:
                    throw new IllegalArgumentException("Bad toneId: " + this.mToneId);
                case 13:
                    toneType = 21;
                    toneVolume = 80;
                    toneLengthMillis = 4000;
                    break;
            }
            try {
                if (CallNotifier.this.mBluetoothHeadset != null) {
                    if (CallNotifier.this.mBluetoothHeadset.isAudioOn()) {
                        stream = 6;
                    }
                } else {
                    stream = 0;
                }
                toneGenerator = new ToneGenerator(stream, toneVolume);
            } catch (RuntimeException e) {
                Log.w("CallNotifier", "InCallTonePlayer: Exception caught while creating ToneGenerator: " + e);
                toneGenerator = null;
            }
            boolean needToStopTone = true;
            boolean okToPlayTone = false;
            if (toneGenerator != null) {
                int ringerMode = CallNotifier.this.mAudioManager.getRingerMode();
                if (phoneType == 2) {
                    if (toneType == 93) {
                        if (ringerMode != 0 && ringerMode != 1) {
                            if (CallNotifier.DBG) {
                                CallNotifier.this.log("- InCallTonePlayer: start playing call tone=" + toneType);
                            }
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else if (toneType == 96 || toneType == 38 || toneType == 39 || toneType == 37 || toneType == 95) {
                        if (ringerMode != 0) {
                            if (CallNotifier.DBG) {
                                CallNotifier.this.log("InCallTonePlayer:playing call fail tone:" + toneType);
                            }
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else if (toneType == 87 || toneType == 86) {
                        if (ringerMode != 0 && ringerMode != 1) {
                            if (CallNotifier.DBG) {
                                CallNotifier.this.log("InCallTonePlayer:playing tone for toneType=" + toneType);
                            }
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else {
                        okToPlayTone = true;
                    }
                } else {
                    okToPlayTone = true;
                }
                synchronized (this) {
                    if (okToPlayTone) {
                        if (this.mState != 2) {
                            this.mState = 1;
                            toneGenerator.startTone(toneType);
                            try {
                                wait(toneLengthMillis + 20);
                            } catch (InterruptedException e2) {
                                Log.w("CallNotifier", "InCallTonePlayer stopped: " + e2);
                            }
                            if (needToStopTone) {
                                toneGenerator.stopTone();
                            }
                        }
                    }
                    toneGenerator.release();
                    this.mState = 0;
                }
            }
            if (CallNotifier.this.mCM.getState() == PhoneConstants.State.IDLE) {
                CallNotifier.this.resetAudioStateAfterDisconnect();
            }
        }
    }

    private void onDisplayInfo(AsyncResult r) {
        CdmaInformationRecords.CdmaDisplayInfoRec displayInfoRec = (CdmaInformationRecords.CdmaDisplayInfoRec) r.result;
        if (displayInfoRec != null) {
            String displayInfo = displayInfoRec.alpha;
            if (DBG) {
                log("onDisplayInfo: displayInfo=" + displayInfo);
            }
            PhoneDisplayMessage.displayNetworkMessage(this.mApplication, displayInfo);
            sendEmptyMessageDelayed(22, 3000L);
        }
    }

    private void onSuppServiceFailed(AsyncResult r) {
        if (r.result != Phone.SuppService.CONFERENCE && r.result != Phone.SuppService.RESUME) {
            if (DBG) {
                log("onSuppServiceFailed: not a merge or resume failure event");
                return;
            }
            return;
        }
        String mergeFailedString = "";
        if (r.result == Phone.SuppService.CONFERENCE) {
            if (DBG) {
                log("onSuppServiceFailed: displaying merge failure message");
            }
            mergeFailedString = this.mApplication.getResources().getString(R.string.incall_error_supp_service_conference);
        } else if (r.result == Phone.SuppService.RESUME) {
            if (DBG) {
                log("onSuppServiceFailed: displaying merge failure message");
            }
            mergeFailedString = this.mApplication.getResources().getString(R.string.incall_error_supp_service_switch);
        }
        PhoneDisplayMessage.displayErrorMessage(this.mApplication, mergeFailedString);
        sendEmptyMessageDelayed(22, 3000L);
    }

    public void updatePhoneStateListeners() {
        List<SubscriptionInfo> subInfos = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        Iterator<Integer> itr = this.mPhoneStateListeners.keySet().iterator();
        while (itr.hasNext()) {
            int subId = itr.next().intValue();
            if (subInfos == null || !containsSubId(subInfos, subId)) {
                this.mApplication.notificationMgr.updateMwi(subId, false);
                this.mApplication.notificationMgr.updateCfi(subId, false);
                this.mTelephonyManager.listen(this.mPhoneStateListeners.get(Integer.valueOf(subId)), 0);
                itr.remove();
            }
        }
        if (subInfos != null) {
            for (int i = 0; i < subInfos.size(); i++) {
                int subId2 = subInfos.get(i).getSubscriptionId();
                if (!this.mPhoneStateListeners.containsKey(Integer.valueOf(subId2))) {
                    CallNotifierPhoneStateListener listener = new CallNotifierPhoneStateListener(subId2);
                    this.mTelephonyManager.listen(listener, 12);
                    this.mPhoneStateListeners.put(Integer.valueOf(subId2), listener);
                }
            }
        }
    }

    private boolean containsSubId(List<SubscriptionInfo> subInfos, int subId) {
        if (subInfos == null) {
            return false;
        }
        for (int i = 0; i < subInfos.size(); i++) {
            if (subInfos.get(i).getSubscriptionId() == subId) {
                return true;
            }
        }
        return false;
    }

    private void onTtyModeReceived(AsyncResult r) {
        if (DBG) {
            log("TtyModeReceived: displaying notification message");
        }
        int resId = 0;
        switch (((Integer) r.result).intValue()) {
            case 0:
                resId = android.R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_ERROR;
                break;
            case 1:
                resId = android.R.string.PERSOSUBSTATE_SIM_NS_SP_IN_PROGRESS;
                break;
            case 2:
                resId = android.R.string.PERSOSUBSTATE_SIM_NS_SP_SUCCESS;
                break;
            case 3:
                resId = android.R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_ENTRY;
                break;
            default:
                Log.e("CallNotifier", "Unsupported TTY mode: " + r.result);
                break;
        }
        if (resId != 0) {
            PhoneDisplayMessage.displayNetworkMessage(this.mApplication, this.mApplication.getResources().getString(resId));
            sendEmptyMessageDelayed(22, 3000L);
        }
    }

    private class SignalInfoTonePlayer extends Thread {
        private int mToneId;

        SignalInfoTonePlayer(int toneId) {
            this.mToneId = toneId;
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

    private void onSignalInfo(AsyncResult r) {
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w("CallNotifier", "Got onSignalInfo() on non-voice-capable device! Ignoring...");
            return;
        }
        if (PhoneUtils.isRealIncomingCall(this.mCM.getFirstActiveRingingCall().getState())) {
            stopSignalInfoTone();
            return;
        }
        CdmaInformationRecords.CdmaSignalInfoRec signalInfoRec = (CdmaInformationRecords.CdmaSignalInfoRec) r.result;
        if (signalInfoRec != null) {
            boolean isPresent = signalInfoRec.isPresent;
            if (DBG) {
                log("onSignalInfo: isPresent=" + isPresent);
            }
            if (isPresent) {
                int uSignalType = signalInfoRec.signalType;
                int uAlertPitch = signalInfoRec.alertPitch;
                int uSignal = signalInfoRec.signal;
                if (DBG) {
                    log("onSignalInfo: uSignalType=" + uSignalType + ", uAlertPitch=" + uAlertPitch + ", uSignal=" + uSignal);
                }
                int toneID = SignalToneUtil.getAudioToneFromSignalInfo(uSignalType, uAlertPitch, uSignal);
                new SignalInfoTonePlayer(toneID).start();
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
        public CallNotifierPhoneStateListener(int subId) {
            super(subId);
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean visible) {
            CallNotifier.this.log("onMessageWaitingIndicatorChanged(): " + this.mSubId + " " + visible);
            CallNotifier.this.mApplication.notificationMgr.updateMwi(this.mSubId, visible);
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean visible) {
            CallNotifier.this.log("onCallForwardingIndicatorChanged(): " + this.mSubId + " " + visible);
            CallNotifier.this.mApplication.notificationMgr.updateCfi(this.mSubId, visible);
        }
    }

    private void log(String msg) {
        Log.d("CallNotifier", msg);
    }
}
