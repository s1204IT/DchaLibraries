package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BinaryOperator;

public class HeadsetPhoneState {
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final String TAG = "HeadsetPhoneState";
    private int mCindBatteryCharge;
    private int mCindSignal;
    private final HeadsetService mHeadsetService;
    private boolean mIsSimStateLoaded;
    private int mNumActive;
    private int mNumHeld;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    private PhoneStateListener mPhoneStateListener;
    private ServiceState mServiceState;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    private int mCindService = 0;
    private int mCallState = 6;
    private int mCindRoam = 0;
    private final HashMap<BluetoothDevice, Integer> mDeviceEventMap = new HashMap<>();

    static int access$908(HeadsetPhoneState headsetPhoneState) {
        int i = headsetPhoneState.mCindSignal;
        headsetPhoneState.mCindSignal = i + 1;
        return i;
    }

    HeadsetPhoneState(HeadsetService headsetService) {
        Objects.requireNonNull(headsetService, "headsetService is null");
        this.mHeadsetService = headsetService;
        this.mTelephonyManager = (TelephonyManager) this.mHeadsetService.getSystemService("phone");
        Objects.requireNonNull(this.mTelephonyManager, "TELEPHONY_SERVICE is null");
        this.mSubscriptionManager = SubscriptionManager.from(this.mHeadsetService);
        Objects.requireNonNull(this.mSubscriptionManager, "TELEPHONY_SUBSCRIPTION_SERVICE is null");
        this.mOnSubscriptionsChangedListener = new HeadsetPhoneStateOnSubscriptionChangedListener(headsetService.getStateMachinesThreadLooper());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    public void cleanup() {
        synchronized (this.mDeviceEventMap) {
            this.mDeviceEventMap.clear();
            stopListenForPhoneState();
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    public String toString() {
        return "HeadsetPhoneState [mTelephonyServiceAvailability=" + this.mCindService + ", mNumActive=" + this.mNumActive + ", mCallState=" + this.mCallState + ", mNumHeld=" + this.mNumHeld + ", mSignal=" + this.mCindSignal + ", mRoam=" + this.mCindRoam + ", mBatteryCharge=" + this.mCindBatteryCharge + ", TelephonyEvents=" + getTelephonyEventsToListen() + "]";
    }

    private int getTelephonyEventsToListen() {
        int iIntValue;
        synchronized (this.mDeviceEventMap) {
            iIntValue = this.mDeviceEventMap.values().stream().reduce(0, new BinaryOperator() {
                @Override
                public final Object apply(Object obj, Object obj2) {
                    return Integer.valueOf(((Integer) obj).intValue() | ((Integer) obj2).intValue());
                }
            }).intValue();
        }
        return iIntValue;
    }

    @VisibleForTesting
    public void listenForPhoneState(BluetoothDevice bluetoothDevice, int i) {
        synchronized (this.mDeviceEventMap) {
            int telephonyEventsToListen = getTelephonyEventsToListen();
            if (i == 0) {
                this.mDeviceEventMap.remove(bluetoothDevice);
            } else {
                this.mDeviceEventMap.put(bluetoothDevice, Integer.valueOf(i));
            }
            if (telephonyEventsToListen != getTelephonyEventsToListen()) {
                stopListenForPhoneState();
                startListenForPhoneState();
            }
        }
    }

    private void startListenForPhoneState() {
        if (this.mPhoneStateListener != null) {
            Log.w(TAG, "startListenForPhoneState, already listening");
            return;
        }
        int telephonyEventsToListen = getTelephonyEventsToListen();
        if (telephonyEventsToListen == 0) {
            Log.w(TAG, "startListenForPhoneState, no event to listen");
            return;
        }
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        if (!SubscriptionManager.isValidSubscriptionId(defaultSubscriptionId)) {
            Log.w(TAG, "startListenForPhoneState, invalid subscription ID " + defaultSubscriptionId);
            return;
        }
        Log.i(TAG, "startListenForPhoneState(), subId=" + defaultSubscriptionId + ", enabled_events=" + telephonyEventsToListen);
        this.mPhoneStateListener = new HeadsetPhoneStateListener(Integer.valueOf(defaultSubscriptionId), this.mHeadsetService.getStateMachinesThreadLooper());
        this.mTelephonyManager.listen(this.mPhoneStateListener, telephonyEventsToListen);
        if ((telephonyEventsToListen & 256) != 0) {
            this.mTelephonyManager.setRadioIndicationUpdateMode(1, 2);
        }
    }

    private void stopListenForPhoneState() {
        if (this.mPhoneStateListener == null) {
            Log.i(TAG, "stopListenForPhoneState(), no listener indicates nothing is listening");
            return;
        }
        Log.i(TAG, "stopListenForPhoneState(), stopping listener, enabled_events=" + getTelephonyEventsToListen());
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        this.mTelephonyManager.setRadioIndicationUpdateMode(1, 1);
        this.mPhoneStateListener = null;
    }

    int getCindService() {
        return this.mCindService;
    }

    int getNumActiveCall() {
        return this.mNumActive;
    }

    @VisibleForTesting(otherwise = 3)
    public void setNumActiveCall(int i) {
        this.mNumActive = i;
    }

    int getCallState() {
        return this.mCallState;
    }

    @VisibleForTesting(otherwise = 3)
    public void setCallState(int i) {
        this.mCallState = i;
    }

    int getNumHeldCall() {
        return this.mNumHeld;
    }

    @VisibleForTesting(otherwise = 3)
    public void setNumHeldCall(int i) {
        this.mNumHeld = i;
    }

    int getCindSignal() {
        return this.mCindSignal;
    }

    int getCindRoam() {
        return this.mCindRoam;
    }

    @VisibleForTesting(otherwise = 3)
    public void setCindBatteryCharge(int i) {
        if (this.mCindBatteryCharge != i) {
            this.mCindBatteryCharge = i;
            sendDeviceStateChanged();
        }
    }

    int getCindBatteryCharge() {
        return this.mCindBatteryCharge;
    }

    boolean isInCall() {
        return this.mNumActive >= 1;
    }

    private void sendDeviceStateChanged() {
        int i = this.mCindService;
        int i2 = i == 1 ? this.mCindSignal : 0;
        Log.d(TAG, "sendDeviceStateChanged. mService=" + this.mCindService + " mIsSimStateLoaded=" + this.mIsSimStateLoaded + " mSignal=" + i2 + " mRoam=" + this.mCindRoam + " mBatteryCharge=" + this.mCindBatteryCharge);
        this.mHeadsetService.onDeviceStateChanged(new HeadsetDeviceState(i, this.mCindRoam, i2, this.mCindBatteryCharge));
    }

    private class HeadsetPhoneStateOnSubscriptionChangedListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        HeadsetPhoneStateOnSubscriptionChangedListener(Looper looper) {
            super(looper);
        }

        @Override
        public void onSubscriptionsChanged() {
            synchronized (HeadsetPhoneState.this.mDeviceEventMap) {
                HeadsetPhoneState.this.stopListenForPhoneState();
                HeadsetPhoneState.this.startListenForPhoneState();
            }
        }
    }

    private class HeadsetPhoneStateListener extends PhoneStateListener {
        HeadsetPhoneStateListener(Integer num, Looper looper) {
            super(num, looper);
        }

        @Override
        public synchronized void onServiceStateChanged(ServiceState serviceState) {
            HeadsetPhoneState.this.mServiceState = serviceState;
            int i = 1;
            int i2 = serviceState.getState() == 0 ? 1 : 0;
            if (!serviceState.getRoaming()) {
                i = 0;
            }
            if (i2 == HeadsetPhoneState.this.mCindService && i == HeadsetPhoneState.this.mCindRoam) {
                return;
            }
            HeadsetPhoneState.this.mCindService = i2;
            HeadsetPhoneState.this.mCindRoam = i;
            if (i2 == 0) {
                HeadsetPhoneState.this.mIsSimStateLoaded = false;
                HeadsetPhoneState.this.sendDeviceStateChanged();
            } else {
                HeadsetPhoneState.this.mHeadsetService.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && "LOADED".equals(intent.getStringExtra("ss"))) {
                            HeadsetPhoneState.this.mIsSimStateLoaded = true;
                            HeadsetPhoneState.this.sendDeviceStateChanged();
                            try {
                                HeadsetPhoneState.this.mHeadsetService.unregisterReceiver(this);
                            } catch (Exception e) {
                                Log.w(HeadsetPhoneState.TAG, "Unable to unregister SIM state receiver", e);
                            }
                        }
                    }
                }, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            int i = HeadsetPhoneState.this.mCindSignal;
            if (HeadsetPhoneState.this.mCindService == 0) {
                HeadsetPhoneState.this.mCindSignal = 0;
            } else if (signalStrength.isGsm()) {
                HeadsetPhoneState.this.mCindSignal = signalStrength.getLevel();
                if (HeadsetPhoneState.DBG) {
                    Log.d(HeadsetPhoneState.TAG, "GSM signal: " + HeadsetPhoneState.this.mCindSignal);
                }
                if (HeadsetPhoneState.this.mCindSignal == 0) {
                    HeadsetPhoneState.this.mCindSignal = gsmAsuToSignal(signalStrength);
                } else {
                    HeadsetPhoneState.access$908(HeadsetPhoneState.this);
                }
            } else {
                HeadsetPhoneState.this.mCindSignal = cdmaDbmEcioToSignal(signalStrength);
            }
            if (i != HeadsetPhoneState.this.mCindSignal) {
                HeadsetPhoneState.this.sendDeviceStateChanged();
            }
        }

        private int gsmAsuToSignal(SignalStrength signalStrength) {
            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
            if (gsmSignalStrength == 99) {
                return 0;
            }
            if (gsmSignalStrength >= 16) {
                return 5;
            }
            if (gsmSignalStrength >= 8) {
                return 4;
            }
            if (gsmSignalStrength >= 4) {
                return 3;
            }
            if (gsmSignalStrength >= 2) {
                return 2;
            }
            if (gsmSignalStrength < 1) {
                return 0;
            }
            return 1;
        }

        private int cdmaDbmEcioToSignal(SignalStrength signalStrength) {
            int cdmaDbm = signalStrength.getCdmaDbm();
            int cdmaEcio = signalStrength.getCdmaEcio();
            int i = 2;
            int i2 = 0;
            int i3 = cdmaDbm >= -75 ? 4 : cdmaDbm >= -85 ? 3 : cdmaDbm >= -95 ? 2 : cdmaDbm >= -100 ? 1 : 0;
            int i4 = cdmaEcio >= -90 ? 4 : cdmaEcio >= -110 ? 3 : cdmaEcio >= -130 ? 2 : cdmaEcio >= -150 ? 1 : 0;
            if (i3 >= i4) {
                i3 = i4;
            }
            if (HeadsetPhoneState.this.mServiceState != null && (HeadsetPhoneState.this.mServiceState.getRadioTechnology() == 7 || HeadsetPhoneState.this.mServiceState.getRadioTechnology() == 8)) {
                int evdoEcio = signalStrength.getEvdoEcio();
                int evdoSnr = signalStrength.getEvdoSnr();
                int i5 = evdoEcio >= -650 ? 4 : evdoEcio >= -750 ? 3 : evdoEcio >= -900 ? 2 : evdoEcio >= -1050 ? 1 : 0;
                if (evdoSnr > 7) {
                    i = 4;
                } else if (evdoSnr > 5) {
                    i = 3;
                } else if (evdoSnr <= 3) {
                    i = evdoSnr > 1 ? 1 : 0;
                }
                i2 = i5 < i ? i5 : i;
            }
            return i3 > i2 ? i3 : i2;
        }
    }
}
