package com.android.server.wifi;

import android.net.wifi.SupplicantState;
import android.net.wifi.WifiSsid;
import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.WnmData;
import com.android.server.wifi.util.TelephonyUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WifiMonitor {
    public static final int ANQP_DONE_EVENT = 147500;
    public static final int ASSOCIATION_REJECTION_EVENT = 147499;
    public static final int AUTHENTICATION_FAILURE_EVENT = 147463;
    private static final int BASE = 147456;
    private static final int CONFIG_AUTH_FAILURE = 18;
    private static final int CONFIG_MULTIPLE_PBC_DETECTED = 12;
    public static final int GAS_QUERY_DONE_EVENT = 147508;
    public static final int GAS_QUERY_START_EVENT = 147507;
    public static final int HS20_REMEDIATION_EVENT = 147517;
    public static final int NETWORK_CONNECTION_EVENT = 147459;
    public static final int NETWORK_DISCONNECTION_EVENT = 147460;
    public static final int PNO_SCAN_RESULTS_EVENT = 147474;
    private static final int REASON_TKIP_ONLY_PROHIBITED = 1;
    private static final int REASON_WEP_PROHIBITED = 2;
    public static final int RX_HS20_ANQP_ICON_EVENT = 147509;
    public static final int SCAN_FAILED_EVENT = 147473;
    public static final int SCAN_RESULTS_EVENT = 147461;
    public static final int SUPPLICANT_STATE_CHANGE_EVENT = 147462;
    public static final int SUP_CONNECTION_EVENT = 147457;
    public static final int SUP_DISCONNECTION_EVENT = 147458;
    public static final int SUP_REQUEST_IDENTITY = 147471;
    public static final int SUP_REQUEST_SIM_AUTH = 147472;
    private static final String TAG = "WifiMonitor";
    public static final int WPS_FAIL_EVENT = 147465;
    public static final int WPS_OVERLAP_EVENT = 147466;
    public static final int WPS_SUCCESS_EVENT = 147464;
    public static final int WPS_TIMEOUT_EVENT = 147467;
    private final WifiInjector mWifiInjector;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mConnected = false;
    private final Map<String, SparseArray<Set<Handler>>> mHandlerMap = new HashMap();
    private final Map<String, Boolean> mMonitoringMap = new HashMap();

    public WifiMonitor(WifiInjector wifiInjector) {
        this.mWifiInjector = wifiInjector;
    }

    void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public synchronized void registerHandler(String str, int i, Handler handler) {
        SparseArray<Set<Handler>> sparseArray = this.mHandlerMap.get(str);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>();
            this.mHandlerMap.put(str, sparseArray);
        }
        Set<Handler> arraySet = sparseArray.get(i);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            sparseArray.put(i, arraySet);
        }
        arraySet.add(handler);
    }

    public synchronized void deregisterHandler(String str, int i, Handler handler) {
        SparseArray<Set<Handler>> sparseArray = this.mHandlerMap.get(str);
        if (sparseArray == null) {
            return;
        }
        Set<Handler> set = sparseArray.get(i);
        if (set == null) {
            return;
        }
        set.remove(handler);
    }

    private boolean isMonitoring(String str) {
        Boolean bool = this.mMonitoringMap.get(str);
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    @VisibleForTesting
    public void setMonitoring(String str, boolean z) {
        this.mMonitoringMap.put(str, Boolean.valueOf(z));
    }

    private void setMonitoringNone() {
        Iterator<String> it = this.mMonitoringMap.keySet().iterator();
        while (it.hasNext()) {
            setMonitoring(it.next(), false);
        }
    }

    public synchronized void startMonitoring(String str) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "startMonitoring(" + str + ")");
        }
        setMonitoring(str, true);
        broadcastSupplicantConnectionEvent(str);
    }

    public synchronized void stopMonitoring(String str) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "stopMonitoring(" + str + ")");
        }
        setMonitoring(str, true);
        broadcastSupplicantDisconnectionEvent(str);
        setMonitoring(str, false);
    }

    public synchronized void stopAllMonitoring() {
        this.mConnected = false;
        setMonitoringNone();
    }

    private void sendMessage(String str, int i) {
        sendMessage(str, Message.obtain((Handler) null, i));
    }

    private void sendMessage(String str, int i, Object obj) {
        sendMessage(str, Message.obtain(null, i, obj));
    }

    private void sendMessage(String str, int i, int i2) {
        sendMessage(str, Message.obtain(null, i, i2, 0));
    }

    private void sendMessage(String str, int i, int i2, int i3) {
        sendMessage(str, Message.obtain(null, i, i2, i3));
    }

    private void sendMessage(String str, int i, int i2, int i3, Object obj) {
        sendMessage(str, Message.obtain(null, i, i2, i3, obj));
    }

    private void sendMessage(String str, Message message) {
        SparseArray<Set<Handler>> sparseArray = this.mHandlerMap.get(str);
        if (str != null && sparseArray != null) {
            if (isMonitoring(str)) {
                Set<Handler> set = sparseArray.get(message.what);
                if (set != null) {
                    for (Handler handler : set) {
                        if (handler != null) {
                            sendMessage(handler, Message.obtain(message));
                        }
                    }
                }
            } else if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Dropping event because (" + str + ") is stopped");
            }
        } else {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Sending to all monitors because there's no matching iface");
            }
            for (Map.Entry<String, SparseArray<Set<Handler>>> entry : this.mHandlerMap.entrySet()) {
                if (isMonitoring(entry.getKey())) {
                    for (Handler handler2 : entry.getValue().get(message.what)) {
                        if (handler2 != null) {
                            sendMessage(handler2, Message.obtain(message));
                        }
                    }
                }
            }
        }
        message.recycle();
    }

    private void sendMessage(Handler handler, Message message) {
        message.setTarget(handler);
        message.sendToTarget();
    }

    public void broadcastWpsFailEvent(String str, int i, int i2) {
        switch (i2) {
            case 1:
                sendMessage(str, WPS_FAIL_EVENT, 5);
                break;
            case 2:
                sendMessage(str, WPS_FAIL_EVENT, 4);
                break;
            default:
                if (i == 12) {
                    sendMessage(str, WPS_FAIL_EVENT, 3);
                } else if (i == 18) {
                    sendMessage(str, WPS_FAIL_EVENT, 6);
                } else {
                    if (i2 != 0) {
                        i = i2;
                    }
                    sendMessage(str, WPS_FAIL_EVENT, 0, i);
                }
                break;
        }
    }

    public void broadcastWpsSuccessEvent(String str) {
        sendMessage(str, WPS_SUCCESS_EVENT);
    }

    public void broadcastWpsOverlapEvent(String str) {
        sendMessage(str, WPS_OVERLAP_EVENT);
    }

    public void broadcastWpsTimeoutEvent(String str) {
        sendMessage(str, WPS_TIMEOUT_EVENT);
    }

    public void broadcastAnqpDoneEvent(String str, AnqpEvent anqpEvent) {
        sendMessage(str, ANQP_DONE_EVENT, anqpEvent);
    }

    public void broadcastIconDoneEvent(String str, IconEvent iconEvent) {
        sendMessage(str, RX_HS20_ANQP_ICON_EVENT, iconEvent);
    }

    public void broadcastWnmEvent(String str, WnmData wnmData) {
        sendMessage(str, HS20_REMEDIATION_EVENT, wnmData);
    }

    public void broadcastNetworkIdentityRequestEvent(String str, int i, String str2) {
        sendMessage(str, SUP_REQUEST_IDENTITY, 0, i, str2);
    }

    public void broadcastNetworkGsmAuthRequestEvent(String str, int i, String str2, String[] strArr) {
        sendMessage(str, SUP_REQUEST_SIM_AUTH, new TelephonyUtil.SimAuthRequestData(i, 4, str2, strArr));
    }

    public void broadcastNetworkUmtsAuthRequestEvent(String str, int i, String str2, String[] strArr) {
        sendMessage(str, SUP_REQUEST_SIM_AUTH, new TelephonyUtil.SimAuthRequestData(i, 5, str2, strArr));
    }

    public void broadcastScanResultEvent(String str) {
        sendMessage(str, SCAN_RESULTS_EVENT);
    }

    public void broadcastPnoScanResultEvent(String str) {
        sendMessage(str, PNO_SCAN_RESULTS_EVENT);
    }

    public void broadcastScanFailedEvent(String str) {
        sendMessage(str, SCAN_FAILED_EVENT);
    }

    public void broadcastAuthenticationFailureEvent(String str, int i, int i2) {
        sendMessage(str, AUTHENTICATION_FAILURE_EVENT, i, i2);
    }

    public void broadcastAssociationRejectionEvent(String str, int i, boolean z, String str2) {
        sendMessage(str, ASSOCIATION_REJECTION_EVENT, z ? 1 : 0, i, str2);
    }

    public void broadcastAssociatedBssidEvent(String str, String str2) {
        sendMessage(str, 131219, 0, 0, str2);
    }

    public void broadcastTargetBssidEvent(String str, String str2) {
        sendMessage(str, 131213, 0, 0, str2);
    }

    public void broadcastNetworkConnectionEvent(String str, int i, String str2) {
        sendMessage(str, NETWORK_CONNECTION_EVENT, i, 0, str2);
    }

    public void broadcastNetworkDisconnectionEvent(String str, int i, int i2, String str2) {
        sendMessage(str, NETWORK_DISCONNECTION_EVENT, i, i2, str2);
    }

    public void broadcastSupplicantStateChangeEvent(String str, int i, WifiSsid wifiSsid, String str2, SupplicantState supplicantState) {
        sendMessage(str, SUPPLICANT_STATE_CHANGE_EVENT, 0, 0, new StateChangeResult(i, wifiSsid, str2, supplicantState));
    }

    public void broadcastSupplicantConnectionEvent(String str) {
        sendMessage(str, 147457);
    }

    public void broadcastSupplicantDisconnectionEvent(String str) {
        sendMessage(str, 147458);
    }
}
