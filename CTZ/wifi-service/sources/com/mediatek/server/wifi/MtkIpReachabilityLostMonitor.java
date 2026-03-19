package com.mediatek.server.wifi;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiStateMachine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MtkIpReachabilityLostMonitor implements Handler.Callback {
    private static final int CMD_IP_REACHABILITY_TIMEOUT = 1;
    private static final int IP_REACHABILITY_TIMEOUT = 10000;
    private static final String TAG = "WifiStateMachine";
    private Handler mEventHandler;
    private boolean mIsMonitoring = false;
    private String mLastBssid = null;
    private WifiMonitor mWifiMonitor;
    private WifiStateMachine mWifiStateMachine;

    public MtkIpReachabilityLostMonitor(WifiStateMachine wifiStateMachine, WifiMonitor wifiMonitor, Looper looper) {
        this.mWifiStateMachine = wifiStateMachine;
        this.mWifiMonitor = wifiMonitor;
        this.mEventHandler = new Handler(looper, this);
        this.mWifiStateMachine.setIpReachabilityDisconnectEnabled(false);
    }

    public void registerForWifiMonitorEvents() {
        this.mWifiMonitor.registerHandler(getInterfaceName(), WifiMonitor.NETWORK_CONNECTION_EVENT, this.mEventHandler);
        this.mWifiMonitor.registerHandler(getInterfaceName(), WifiMonitor.NETWORK_DISCONNECTION_EVENT, this.mEventHandler);
    }

    private String getInterfaceName() {
        try {
            Field declaredField = this.mWifiStateMachine.getClass().getDeclaredField("mInterfaceName");
            declaredField.setAccessible(true);
            return (String) declaredField.get(this.mWifiStateMachine);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getLogRecString(Message message) {
        try {
            Method declaredMethod = this.mWifiStateMachine.getClass().getDeclaredMethod("getLogRecString", Message.class);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(this.mWifiStateMachine, message);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private IState getCurrentState() {
        try {
            Method declaredMethod = StateMachine.class.getDeclaredMethod("getCurrentState", new Class[0]);
            declaredMethod.setAccessible(true);
            return (IState) declaredMethod.invoke(this.mWifiStateMachine, new Object[0]);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.d(TAG, " MtkIpReachabilityLostMonitor " + getLogRecString(message) + " IRM enable = " + this.mWifiStateMachine.getIpReachabilityDisconnectEnabled());
        int i = message.what;
        if (i != 1) {
            switch (i) {
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (getCurrentState().getName().equals("ConnectedState") && this.mLastBssid != null && !this.mLastBssid.equals(message.obj)) {
                        if (this.mIsMonitoring) {
                            this.mEventHandler.removeMessages(1);
                        }
                        this.mWifiStateMachine.setIpReachabilityDisconnectEnabled(true);
                        this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(1), 10000L);
                        this.mIsMonitoring = true;
                        Log.d(TAG, "MtkIpReachabilityLostMonitor: enable IRM for driver roaming, mIsMonitoring = " + this.mIsMonitoring + ", mLastBssid = " + this.mLastBssid + ", Current Bssid = " + ((String) message.obj));
                    }
                    this.mLastBssid = (String) message.obj;
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    this.mLastBssid = null;
                    this.mWifiStateMachine.setIpReachabilityDisconnectEnabled(false);
                    this.mIsMonitoring = false;
                    Log.d(TAG, "MtkIpReachabilityLostMonitor: disable IRM, mIsMonitoring = " + this.mIsMonitoring);
                    break;
            }
        } else {
            this.mWifiStateMachine.setIpReachabilityDisconnectEnabled(false);
            this.mIsMonitoring = false;
            Log.d(TAG, "MtkIpReachabilityLostMonitor: disable IRM, mIsMonitoring = " + this.mIsMonitoring);
        }
        return true;
    }
}
