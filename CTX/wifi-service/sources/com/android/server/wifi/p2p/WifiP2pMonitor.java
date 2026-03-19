package com.android.server.wifi.p2p;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WifiP2pMonitor {
    public static final int AP_STA_CONNECTED_EVENT = 147498;
    public static final int AP_STA_DISCONNECTED_EVENT = 147497;
    private static final int BASE = 147456;
    public static final int P2P_DEVICE_FOUND_EVENT = 147477;
    public static final int P2P_DEVICE_LOST_EVENT = 147478;
    public static final int P2P_FIND_STOPPED_EVENT = 147493;
    public static final int P2P_GO_NEGOTIATION_FAILURE_EVENT = 147482;
    public static final int P2P_GO_NEGOTIATION_REQUEST_EVENT = 147479;
    public static final int P2P_GO_NEGOTIATION_SUCCESS_EVENT = 147481;
    public static final int P2P_GROUP_FORMATION_FAILURE_EVENT = 147484;
    public static final int P2P_GROUP_FORMATION_SUCCESS_EVENT = 147483;
    public static final int P2P_GROUP_REMOVED_EVENT = 147486;
    public static final int P2P_GROUP_STARTED_EVENT = 147485;
    public static final int P2P_INVITATION_RECEIVED_EVENT = 147487;
    public static final int P2P_INVITATION_RESULT_EVENT = 147488;
    public static final int P2P_PROV_DISC_ENTER_PIN_EVENT = 147491;
    public static final int P2P_PROV_DISC_FAILURE_EVENT = 147495;
    public static final int P2P_PROV_DISC_PBC_REQ_EVENT = 147489;
    public static final int P2P_PROV_DISC_PBC_RSP_EVENT = 147490;
    public static final int P2P_PROV_DISC_SHOW_PIN_EVENT = 147492;
    public static final int P2P_SERV_DISC_RESP_EVENT = 147494;
    public static final int SUP_CONNECTION_EVENT = 147457;
    public static final int SUP_DISCONNECTION_EVENT = 147458;
    private static final String TAG = "WifiP2pMonitor";
    private final WifiInjector mWifiInjector;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mConnected = false;
    private final Map<String, SparseArray<Set<Handler>>> mHandlerMap = new HashMap();
    private final Map<String, Boolean> mMonitoringMap = new HashMap();

    public WifiP2pMonitor(WifiInjector wifiInjector) {
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

    public void broadcastSupplicantConnectionEvent(String str) {
        sendMessage(str, 147457);
    }

    public void broadcastSupplicantDisconnectionEvent(String str) {
        sendMessage(str, 147458);
    }

    public void broadcastP2pDeviceFound(String str, WifiP2pDevice wifiP2pDevice) {
        if (wifiP2pDevice != null) {
            sendMessage(str, P2P_DEVICE_FOUND_EVENT, wifiP2pDevice);
        }
    }

    public void broadcastP2pDeviceLost(String str, WifiP2pDevice wifiP2pDevice) {
        if (wifiP2pDevice != null) {
            sendMessage(str, P2P_DEVICE_LOST_EVENT, wifiP2pDevice);
        }
    }

    public void broadcastP2pFindStopped(String str) {
        sendMessage(str, P2P_FIND_STOPPED_EVENT);
    }

    public void broadcastP2pGoNegotiationRequest(String str, WifiP2pConfig wifiP2pConfig) {
        if (wifiP2pConfig != null) {
            sendMessage(str, P2P_GO_NEGOTIATION_REQUEST_EVENT, wifiP2pConfig);
        }
    }

    public void broadcastP2pGoNegotiationSuccess(String str) {
        sendMessage(str, P2P_GO_NEGOTIATION_SUCCESS_EVENT);
    }

    public void broadcastP2pGoNegotiationFailure(String str, WifiP2pServiceImpl.P2pStatus p2pStatus) {
        sendMessage(str, P2P_GO_NEGOTIATION_FAILURE_EVENT, p2pStatus);
    }

    public void broadcastP2pGroupFormationSuccess(String str) {
        sendMessage(str, P2P_GROUP_FORMATION_SUCCESS_EVENT);
    }

    public void broadcastP2pGroupFormationFailure(String str, String str2) {
        WifiP2pServiceImpl.P2pStatus p2pStatus = WifiP2pServiceImpl.P2pStatus.UNKNOWN;
        if (str2.equals("FREQ_CONFLICT")) {
            p2pStatus = WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL;
        }
        sendMessage(str, P2P_GROUP_FORMATION_FAILURE_EVENT, p2pStatus);
    }

    public void broadcastP2pGroupStarted(String str, WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup != null) {
            sendMessage(str, P2P_GROUP_STARTED_EVENT, wifiP2pGroup);
        }
    }

    public void broadcastP2pGroupRemoved(String str, WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup != null) {
            sendMessage(str, P2P_GROUP_REMOVED_EVENT, wifiP2pGroup);
        }
    }

    public void broadcastP2pInvitationReceived(String str, WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup != null) {
            sendMessage(str, P2P_INVITATION_RECEIVED_EVENT, wifiP2pGroup);
        }
    }

    public void broadcastP2pInvitationResult(String str, WifiP2pServiceImpl.P2pStatus p2pStatus) {
        sendMessage(str, P2P_INVITATION_RESULT_EVENT, p2pStatus);
    }

    public void broadcastP2pProvisionDiscoveryPbcRequest(String str, WifiP2pProvDiscEvent wifiP2pProvDiscEvent) {
        if (wifiP2pProvDiscEvent != null) {
            sendMessage(str, P2P_PROV_DISC_PBC_REQ_EVENT, wifiP2pProvDiscEvent);
        }
    }

    public void broadcastP2pProvisionDiscoveryPbcResponse(String str, WifiP2pProvDiscEvent wifiP2pProvDiscEvent) {
        if (wifiP2pProvDiscEvent != null) {
            sendMessage(str, P2P_PROV_DISC_PBC_RSP_EVENT, wifiP2pProvDiscEvent);
        }
    }

    public void broadcastP2pProvisionDiscoveryEnterPin(String str, WifiP2pProvDiscEvent wifiP2pProvDiscEvent) {
        if (wifiP2pProvDiscEvent != null) {
            sendMessage(str, P2P_PROV_DISC_ENTER_PIN_EVENT, wifiP2pProvDiscEvent);
        }
    }

    public void broadcastP2pProvisionDiscoveryShowPin(String str, WifiP2pProvDiscEvent wifiP2pProvDiscEvent) {
        if (wifiP2pProvDiscEvent != null) {
            sendMessage(str, P2P_PROV_DISC_SHOW_PIN_EVENT, wifiP2pProvDiscEvent);
        }
    }

    public void broadcastP2pProvisionDiscoveryFailure(String str) {
        sendMessage(str, P2P_PROV_DISC_FAILURE_EVENT);
    }

    public void broadcastP2pServiceDiscoveryResponse(String str, List<WifiP2pServiceResponse> list) {
        sendMessage(str, P2P_SERV_DISC_RESP_EVENT, list);
    }

    public void broadcastP2pApStaConnected(String str, WifiP2pDevice wifiP2pDevice) {
        sendMessage(str, 147498, wifiP2pDevice);
    }

    public void broadcastP2pApStaDisconnected(String str, WifiP2pDevice wifiP2pDevice) {
        sendMessage(str, 147497, wifiP2pDevice);
    }
}
