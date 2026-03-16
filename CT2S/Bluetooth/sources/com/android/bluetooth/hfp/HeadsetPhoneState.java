package com.android.bluetooth.hfp;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

class HeadsetPhoneState {
    private static final String TAG = "HeadsetPhoneState";
    private Context mContext;
    private ServiceState mServiceState;
    private HeadsetStateMachine mStateMachine;
    private SubscriptionManager mSubMgr;
    private TelephonyManager mTelephonyManager;
    private int mService = 0;
    private int mNumActive = 0;
    private int mCallState = 6;
    private int mNumHeld = 0;
    private int mSignal = 0;
    private int mRoam = 0;
    private int mBatteryCharge = 0;
    private int mSpeakerVolume = 0;
    private int mMicVolume = 0;
    private boolean mListening = false;
    private boolean mSlcReady = false;
    private PhoneStateListener mPhoneStateListener = null;
    private SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            HeadsetPhoneState.this.listenForPhoneState(false);
            HeadsetPhoneState.this.listenForPhoneState(true);
        }
    };

    static int access$208(HeadsetPhoneState x0) {
        int i = x0.mSignal;
        x0.mSignal = i + 1;
        return i;
    }

    HeadsetPhoneState(Context context, HeadsetStateMachine stateMachine) {
        this.mContext = null;
        this.mStateMachine = stateMachine;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mContext = context;
        this.mSubMgr = SubscriptionManager.from(this.mContext);
        this.mSubMgr.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    public void cleanup() {
        listenForPhoneState(false);
        this.mSubMgr.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mTelephonyManager = null;
        this.mStateMachine = null;
    }

    void listenForPhoneState(boolean start) {
        this.mSlcReady = start;
        if (start) {
            startListenForPhoneState();
        } else {
            stopListenForPhoneState();
        }
    }

    private void startListenForPhoneState() {
        if (!this.mListening && this.mSlcReady) {
            int subId = SubscriptionManager.getDefaultSubId();
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                this.mPhoneStateListener = getPhoneStateListener(subId);
                if (this.mTelephonyManager != null) {
                    this.mTelephonyManager.listen(this.mPhoneStateListener, 257);
                }
                this.mListening = true;
            }
        }
    }

    private void stopListenForPhoneState() {
        if (this.mListening) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mListening = false;
        }
    }

    int getService() {
        return this.mService;
    }

    int getNumActiveCall() {
        return this.mNumActive;
    }

    void setNumActiveCall(int numActive) {
        this.mNumActive = numActive;
    }

    int getCallState() {
        return this.mCallState;
    }

    void setCallState(int callState) {
        this.mCallState = callState;
    }

    int getNumHeldCall() {
        return this.mNumHeld;
    }

    void setNumHeldCall(int numHeldCall) {
        this.mNumHeld = numHeldCall;
    }

    int getSignal() {
        return this.mSignal;
    }

    int getRoam() {
        return this.mRoam;
    }

    void setRoam(int roam) {
        this.mRoam = roam;
    }

    void setBatteryCharge(int batteryLevel) {
        if (this.mBatteryCharge != batteryLevel) {
            this.mBatteryCharge = batteryLevel;
            sendDeviceStateChanged();
        }
    }

    int getBatteryCharge() {
        return this.mBatteryCharge;
    }

    void setSpeakerVolume(int volume) {
        this.mSpeakerVolume = volume;
    }

    int getSpeakerVolume() {
        return this.mSpeakerVolume;
    }

    void setMicVolume(int volume) {
        this.mMicVolume = volume;
    }

    int getMicVolume() {
        return this.mMicVolume;
    }

    boolean isInCall() {
        return this.mNumActive >= 1;
    }

    void sendDeviceStateChanged() {
        int signal = this.mService == 1 ? this.mSignal : 0;
        Log.d(TAG, "sendDeviceStateChanged. mService=" + this.mService + " mSignal=" + signal + " mRoam=" + this.mRoam + " mBatteryCharge=" + this.mBatteryCharge);
        HeadsetStateMachine sm = this.mStateMachine;
        if (sm != null) {
            sm.sendMessage(11, new HeadsetDeviceState(this.mService, this.mRoam, signal, this.mBatteryCharge));
        }
    }

    private PhoneStateListener getPhoneStateListener(int subId) {
        PhoneStateListener mPhoneStateListener = new PhoneStateListener(subId) {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                HeadsetPhoneState.this.mServiceState = serviceState;
                HeadsetPhoneState.this.mService = serviceState.getState() == 0 ? 1 : 0;
                HeadsetPhoneState.this.setRoam(serviceState.getRoaming() ? 1 : 0);
                HeadsetPhoneState.this.sendDeviceStateChanged();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                int prevSignal = HeadsetPhoneState.this.mSignal;
                if (HeadsetPhoneState.this.mService == 0) {
                    HeadsetPhoneState.this.mSignal = 0;
                } else if (signalStrength.isGsm()) {
                    HeadsetPhoneState.this.mSignal = signalStrength.getLteLevel();
                    if (HeadsetPhoneState.this.mSignal == 0) {
                        HeadsetPhoneState.this.mSignal = gsmAsuToSignal(signalStrength);
                    } else {
                        HeadsetPhoneState.access$208(HeadsetPhoneState.this);
                    }
                } else {
                    HeadsetPhoneState.this.mSignal = cdmaDbmEcioToSignal(signalStrength);
                }
                if (prevSignal != HeadsetPhoneState.this.mSignal) {
                    HeadsetPhoneState.this.sendDeviceStateChanged();
                }
            }

            private int gsmAsuToSignal(SignalStrength signalStrength) {
                int asu = signalStrength.getGsmSignalStrength();
                if (asu >= 16) {
                    return 5;
                }
                if (asu >= 8) {
                    return 4;
                }
                if (asu >= 4) {
                    return 3;
                }
                if (asu >= 2) {
                    return 2;
                }
                return asu >= 1 ? 1 : 0;
            }

            private int cdmaDbmEcioToSignal(SignalStrength signalStrength) {
                int levelDbm;
                int levelEcio;
                int levelEvdoEcio;
                int levelEvdoSnr;
                int evdoIconLevel = 0;
                int cdmaDbm = signalStrength.getCdmaDbm();
                int cdmaEcio = signalStrength.getCdmaEcio();
                if (cdmaDbm >= -75) {
                    levelDbm = 4;
                } else if (cdmaDbm >= -85) {
                    levelDbm = 3;
                } else if (cdmaDbm >= -95) {
                    levelDbm = 2;
                } else {
                    levelDbm = cdmaDbm >= -100 ? 1 : 0;
                }
                if (cdmaEcio >= -90) {
                    levelEcio = 4;
                } else if (cdmaEcio >= -110) {
                    levelEcio = 3;
                } else if (cdmaEcio >= -130) {
                    levelEcio = 2;
                } else {
                    levelEcio = cdmaEcio >= -150 ? 1 : 0;
                }
                int cdmaIconLevel = levelDbm < levelEcio ? levelDbm : levelEcio;
                if (HeadsetPhoneState.this.mServiceState != null && (HeadsetPhoneState.this.mServiceState.getRadioTechnology() == 7 || HeadsetPhoneState.this.mServiceState.getRadioTechnology() == 8)) {
                    int evdoEcio = signalStrength.getEvdoEcio();
                    int evdoSnr = signalStrength.getEvdoSnr();
                    if (evdoEcio >= -650) {
                        levelEvdoEcio = 4;
                    } else if (evdoEcio >= -750) {
                        levelEvdoEcio = 3;
                    } else if (evdoEcio >= -900) {
                        levelEvdoEcio = 2;
                    } else {
                        levelEvdoEcio = evdoEcio >= -1050 ? 1 : 0;
                    }
                    if (evdoSnr > 7) {
                        levelEvdoSnr = 4;
                    } else if (evdoSnr > 5) {
                        levelEvdoSnr = 3;
                    } else if (evdoSnr > 3) {
                        levelEvdoSnr = 2;
                    } else {
                        levelEvdoSnr = evdoSnr > 1 ? 1 : 0;
                    }
                    evdoIconLevel = levelEvdoEcio < levelEvdoSnr ? levelEvdoEcio : levelEvdoSnr;
                }
                if (cdmaIconLevel > evdoIconLevel) {
                    return cdmaIconLevel;
                }
                return evdoIconLevel;
            }
        };
        return mPhoneStateListener;
    }
}
