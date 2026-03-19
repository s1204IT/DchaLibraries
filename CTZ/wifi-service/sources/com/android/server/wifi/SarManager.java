package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SarManager {
    private static final String TAG = "WifiSarManager";
    private final Context mContext;
    private boolean mEnableSarTxPowerLimit;
    private final Looper mLooper;
    private final WifiPhoneStateListener mPhoneStateListener;
    private final TelephonyManager mTelephonyManager;
    private final WifiNative mWifiNative;
    private boolean mVerboseLoggingEnabled = true;
    private int mCurrentSarScenario = 0;
    private boolean mCellOn = false;
    private boolean mWifiStaEnabled = false;

    SarManager(Context context, TelephonyManager telephonyManager, Looper looper, WifiNative wifiNative) {
        this.mContext = context;
        this.mTelephonyManager = telephonyManager;
        this.mWifiNative = wifiNative;
        this.mLooper = looper;
        this.mPhoneStateListener = new WifiPhoneStateListener(looper);
        registerListeners();
    }

    private void registerListeners() {
        this.mEnableSarTxPowerLimit = this.mContext.getResources().getBoolean(R.^attr-private.progressBarCornerRadius);
        if (this.mEnableSarTxPowerLimit) {
            Log.d(TAG, "Registering Listeners for the SAR Manager");
            registerPhoneListener();
        }
    }

    private void onCellStateChangeEvent(int i) {
        boolean z = this.mCellOn;
        switch (i) {
            case 0:
                this.mCellOn = false;
                break;
            case 1:
            case 2:
                this.mCellOn = true;
                break;
            default:
                Log.e(TAG, "Invalid Cell State: " + i);
                break;
        }
        if (this.mCellOn != z) {
            updateSarScenario();
        }
    }

    public void setClientWifiState(int i) {
        if (this.mEnableSarTxPowerLimit) {
            if (i == 1 && this.mWifiStaEnabled) {
                this.mWifiStaEnabled = false;
            } else if (i == 3 && !this.mWifiStaEnabled) {
                this.mWifiStaEnabled = true;
                sendTxPowerScenario(this.mCurrentSarScenario);
            }
        }
    }

    public void enableVerboseLogging(int i) {
        Log.d(TAG, "Inside enableVerboseLogging: " + i);
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("*** WiFi SAR Manager Dump ***");
        printWriter.println("Current SAR Scenario is " + scenarioToString(this.mCurrentSarScenario));
    }

    private void registerPhoneListener() {
        Log.i(TAG, "Registering for telephony call state changes");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
    }

    private class WifiPhoneStateListener extends PhoneStateListener {
        WifiPhoneStateListener(Looper looper) {
            super(looper);
        }

        @Override
        public void onCallStateChanged(int i, String str) {
            Log.d(SarManager.TAG, "Received Phone State Change: " + i);
            if (SarManager.this.mEnableSarTxPowerLimit) {
                SarManager.this.onCellStateChangeEvent(i);
            }
        }
    }

    private void updateSarScenario() {
        int i;
        if (this.mCellOn) {
            i = 1;
        } else {
            i = 0;
        }
        if (i != this.mCurrentSarScenario) {
            if (this.mWifiStaEnabled) {
                Log.d(TAG, "Sending SAR Scenario #" + scenarioToString(i));
                sendTxPowerScenario(i);
            }
            this.mCurrentSarScenario = i;
        }
    }

    private void sendTxPowerScenario(int i) {
        if (!this.mWifiNative.selectTxPowerScenario(i)) {
            Log.e(TAG, "Failed to set TX power scenario");
        }
    }

    private String scenarioToString(int i) {
        switch (i) {
            case 0:
                return "TX_POWER_SCENARIO_NORMAL";
            case 1:
                return "TX_POWER_SCENARIO_VOICE_CALL";
            default:
                return "Invalid Scenario";
        }
    }
}
