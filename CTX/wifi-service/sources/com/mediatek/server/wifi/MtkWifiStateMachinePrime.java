package com.mediatek.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.server.wifi.DefaultModeManager;
import com.android.server.wifi.SoftApModeConfiguration;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachinePrime;
import java.util.ArrayList;

public class MtkWifiStateMachinePrime extends WifiStateMachinePrime implements Handler.Callback {
    private static final int CMD_WFC_DEFER_OFF_TIMEOUT = 131474;
    private static final int CMD_WFC_DEFER_WIFI_ON = 131479;
    private static final int CMD_WFC_GO_SCAN_MODE = 131476;
    private static final int CMD_WFC_GO_SHUT_DOWN = 131478;
    private static final int CMD_WFC_GO_SOFT_AP = 131477;
    private static final int CMD_WFC_GO_WIFI_OFF = 131475;
    private static final int CMD_WFC_NOTIFY_GO = 131473;
    static final int MTK_BASE = 131472;
    private static final int NEED_DEFER = 1;
    private static final int NO_NEED_DEFER = 0;
    private static final String TAG = "WifiStateMachinePrime";
    private static final int WFC_NOTIFY_GO = 2;
    private static final int WFC_TIMEOUT = 3000;
    private static boolean mWaitForEvent = false;
    private final Context mContext;
    private ArrayList<Message> mDeferredMsgInQueue;
    private Handler mEventHandler;
    public boolean mShouldDeferDisableWifi;
    private SoftApModeConfiguration mWifiConfig;

    public MtkWifiStateMachinePrime(WifiInjector wifiInjector, Context context, Looper looper, WifiNative wifiNative, DefaultModeManager defaultModeManager, IBatteryStats iBatteryStats) {
        super(wifiInjector, context, looper, wifiNative, defaultModeManager, iBatteryStats);
        this.mShouldDeferDisableWifi = false;
        this.mDeferredMsgInQueue = new ArrayList<>();
        this.mEventHandler = new Handler(looper, this);
        this.mContext = context;
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                switch (intent.getIntExtra("wfc_status", 0)) {
                    case 0:
                        Log.d(MtkWifiStateMachinePrime.TAG, "Received WFC_STATUS_CHANGED, status: NO_NEED_DEFER");
                        MtkWifiStateMachinePrime.this.mShouldDeferDisableWifi = false;
                        break;
                    case 1:
                        Log.d(MtkWifiStateMachinePrime.TAG, "Received WFC_STATUS_CHANGED, status: NEED_DEFER");
                        MtkWifiStateMachinePrime.this.mShouldDeferDisableWifi = true;
                        break;
                    case 2:
                        Log.d(MtkWifiStateMachinePrime.TAG, "Received WFC_STATUS_CHANGED, status: WFC_NOTIFY_GO");
                        if (MtkWifiStateMachinePrime.this.mShouldDeferDisableWifi) {
                            MtkWifiStateMachinePrime.this.mEventHandler.sendMessage(MtkWifiStateMachinePrime.this.mEventHandler.obtainMessage(MtkWifiStateMachinePrime.CMD_WFC_NOTIFY_GO));
                        }
                        break;
                }
            }
        }, new IntentFilter("com.mediatek.intent.action.WFC_STATUS_CHANGED"));
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case CMD_WFC_NOTIFY_GO:
                break;
            case CMD_WFC_DEFER_OFF_TIMEOUT:
                Log.d(TAG, "WFC Defer Wi-Fi off timeout");
                break;
            case CMD_WFC_GO_WIFI_OFF:
                super.disableWifi();
                return true;
            case CMD_WFC_GO_SCAN_MODE:
                super.enterScanOnlyMode();
                return true;
            case CMD_WFC_GO_SOFT_AP:
                super.enterSoftAPMode(this.mWifiConfig);
                return true;
            case CMD_WFC_GO_SHUT_DOWN:
                super.shutdownWifi();
                return true;
            case CMD_WFC_DEFER_WIFI_ON:
                super.enterClientMode();
                return true;
            default:
                Log.e(TAG, "Unhandle message");
                return true;
        }
        for (int i = 0; i < this.mDeferredMsgInQueue.size(); i++) {
            Log.d(TAG, "mDeferredMsgInQueue: " + this.mDeferredMsgInQueue.get(i));
            Message messageObtainMessage = this.mEventHandler.obtainMessage();
            messageObtainMessage.copyFrom(this.mDeferredMsgInQueue.get(i));
            this.mEventHandler.sendMessage(messageObtainMessage);
        }
        this.mDeferredMsgInQueue.clear();
        mWaitForEvent = false;
        return true;
    }

    @Override
    public void enterClientMode() {
        if (!mWaitForEvent) {
            super.enterClientMode();
            return;
        }
        Log.d(TAG, "enterClientMode, mWaitForEvent " + mWaitForEvent);
        this.mDeferredMsgInQueue.add(this.mEventHandler.obtainMessage(CMD_WFC_DEFER_WIFI_ON));
    }

    @Override
    public void disableWifi() {
        if (!this.mShouldDeferDisableWifi) {
            super.disableWifi();
            return;
        }
        Log.d(TAG, "disableWifi, mShouldDeferDisableWifi: " + this.mShouldDeferDisableWifi);
        mWaitForEvent = true;
        updateWifiState(0, 3);
        this.mDeferredMsgInQueue.add(this.mEventHandler.obtainMessage(CMD_WFC_GO_WIFI_OFF));
        this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(CMD_WFC_DEFER_OFF_TIMEOUT), WifiMetrics.TIMEOUT_RSSI_DELTA_MILLIS);
    }

    @Override
    public void enterScanOnlyMode() {
        if (!this.mShouldDeferDisableWifi) {
            super.enterScanOnlyMode();
            return;
        }
        Log.d(TAG, "enterScanOnlyMode, mShouldDeferDisableWifi: " + this.mShouldDeferDisableWifi);
        mWaitForEvent = true;
        updateWifiState(0, 3);
        this.mDeferredMsgInQueue.add(this.mEventHandler.obtainMessage(CMD_WFC_GO_SCAN_MODE));
        this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(CMD_WFC_DEFER_OFF_TIMEOUT), WifiMetrics.TIMEOUT_RSSI_DELTA_MILLIS);
    }

    @Override
    public void enterSoftAPMode(SoftApModeConfiguration softApModeConfiguration) {
        if (!this.mShouldDeferDisableWifi) {
            super.enterSoftAPMode(softApModeConfiguration);
            return;
        }
        Log.d(TAG, "enterSoftAPMode, mShouldDeferDisableWifi: " + this.mShouldDeferDisableWifi);
        mWaitForEvent = true;
        this.mWifiConfig = softApModeConfiguration;
        updateWifiState(0, 3);
        this.mDeferredMsgInQueue.add(this.mEventHandler.obtainMessage(CMD_WFC_GO_SOFT_AP));
        this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(CMD_WFC_DEFER_OFF_TIMEOUT), WifiMetrics.TIMEOUT_RSSI_DELTA_MILLIS);
    }

    @Override
    public void shutdownWifi() {
        if (!this.mShouldDeferDisableWifi) {
            super.shutdownWifi();
            return;
        }
        Log.d(TAG, "shutdownWifi, mShouldDeferDisableWifi: " + this.mShouldDeferDisableWifi);
        mWaitForEvent = true;
        updateWifiState(0, 3);
        this.mDeferredMsgInQueue.add(this.mEventHandler.obtainMessage(CMD_WFC_GO_SHUT_DOWN));
        this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(CMD_WFC_DEFER_OFF_TIMEOUT), WifiMetrics.TIMEOUT_RSSI_DELTA_MILLIS);
    }

    private void updateWifiState(int i, int i2) {
        Intent intent = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_state", i);
        intent.putExtra("previous_wifi_state", i2);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }
}
