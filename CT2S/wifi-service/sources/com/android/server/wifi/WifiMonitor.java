package com.android.server.wifi;

import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiSsid;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiMonitor {
    public static final int AP_DISCONNECTION_EVENT = 147519;
    public static final int AP_STA_CONNECTED_EVENT = 147498;
    private static final String AP_STA_CONNECTED_STR = "AP-STA-CONNECTED";
    public static final int AP_STA_DISCONNECTED_EVENT = 147497;
    private static final String AP_STA_DISCONNECTED_STR = "AP-STA-DISCONNECTED";
    private static final int AP_TERMINATING = 14;
    private static final String AP_TERMINATING_STR = "AP-TERMINATING";
    private static final String ASSOCIATED_WITH_STR = "Associated with ";
    public static final int ASSOCIATION_REJECTION_EVENT = 147499;
    private static final int ASSOC_REJECT = 9;
    private static final String ASSOC_REJECT_STR = "ASSOC-REJECT";
    public static final int AUTHENTICATION_FAILURE_EVENT = 147463;
    private static final int BASE = 147456;
    private static final int BSS_ADDED = 12;
    private static final String BSS_ADDED_STR = "BSS-ADDED";
    private static final int BSS_REMOVED = 13;
    private static final String BSS_REMOVED_STR = "BSS-REMOVED";
    private static final int CONFIG_AUTH_FAILURE = 18;
    private static final int CONFIG_MULTIPLE_PBC_DETECTED = 12;
    private static final int CONNECTED = 1;
    private static final String CONNECTED_STR = "CONNECTED";
    private static final int DISCONNECTED = 2;
    private static final String DISCONNECTED_STR = "DISCONNECTED";
    public static final int DRIVER_HUNG_EVENT = 147468;
    private static final int DRIVER_STATE = 7;
    private static final String DRIVER_STATE_STR = "DRIVER-STATE";
    private static final String EAP_AUTH_FAILURE_STR = "EAP authentication failed";
    private static final int EAP_FAILURE = 8;
    private static final String EAP_FAILURE_STR = "EAP-FAILURE";
    public static final int GAS_QUERY_DONE_EVENT = 147508;
    private static final String GAS_QUERY_DONE_STR = "GAS-QUERY-DONE";
    private static final String GAS_QUERY_PREFIX_STR = "GAS-QUERY-";
    public static final int GAS_QUERY_START_EVENT = 147507;
    private static final String GAS_QUERY_START_STR = "GAS-QUERY-START";
    private static final String HOST_AP_EVENT_PREFIX_STR = "AP";
    public static final int HS20_DEAUTH_EVENT = 147518;
    private static final String HS20_DEAUTH_STR = "HS20-DEAUTH-IMMINENT-NOTICE";
    private static final String HS20_PREFIX_STR = "HS20-";
    public static final int HS20_REMEDIATION_EVENT = 147517;
    private static final String HS20_SUB_REM_STR = "HS20-SUBSCRIPTION-REMEDIATION";
    private static final String IDENTITY_STR = "IDENTITY";
    private static final int LINK_SPEED = 5;
    private static final String LINK_SPEED_STR = "LINK-SPEED";
    private static final int MAX_RECV_ERRORS = 10;
    public static final int NETWORK_CONNECTION_EVENT = 147459;
    public static final int NETWORK_DISCONNECTION_EVENT = 147460;
    public static final int P2P_DEVICE_FOUND_EVENT = 147477;
    private static final String P2P_DEVICE_FOUND_STR = "P2P-DEVICE-FOUND";
    public static final int P2P_DEVICE_LOST_EVENT = 147478;
    private static final String P2P_DEVICE_LOST_STR = "P2P-DEVICE-LOST";
    private static final String P2P_EVENT_PREFIX_STR = "P2P";
    public static final int P2P_FIND_STOPPED_EVENT = 147493;
    private static final String P2P_FIND_STOPPED_STR = "P2P-FIND-STOPPED";
    public static final int P2P_GO_NEGOTIATION_FAILURE_EVENT = 147482;
    public static final int P2P_GO_NEGOTIATION_REQUEST_EVENT = 147479;
    public static final int P2P_GO_NEGOTIATION_SUCCESS_EVENT = 147481;
    private static final String P2P_GO_NEG_FAILURE_STR = "P2P-GO-NEG-FAILURE";
    private static final String P2P_GO_NEG_REQUEST_STR = "P2P-GO-NEG-REQUEST";
    private static final String P2P_GO_NEG_SUCCESS_STR = "P2P-GO-NEG-SUCCESS";
    public static final int P2P_GROUP_FORMATION_FAILURE_EVENT = 147484;
    private static final String P2P_GROUP_FORMATION_FAILURE_STR = "P2P-GROUP-FORMATION-FAILURE";
    public static final int P2P_GROUP_FORMATION_SUCCESS_EVENT = 147483;
    private static final String P2P_GROUP_FORMATION_SUCCESS_STR = "P2P-GROUP-FORMATION-SUCCESS";
    public static final int P2P_GROUP_REMOVED_EVENT = 147486;
    private static final String P2P_GROUP_REMOVED_STR = "P2P-GROUP-REMOVED";
    public static final int P2P_GROUP_STARTED_EVENT = 147485;
    private static final String P2P_GROUP_STARTED_STR = "P2P-GROUP-STARTED";
    public static final int P2P_INVITATION_RECEIVED_EVENT = 147487;
    private static final String P2P_INVITATION_RECEIVED_STR = "P2P-INVITATION-RECEIVED";
    public static final int P2P_INVITATION_RESULT_EVENT = 147488;
    private static final String P2P_INVITATION_RESULT_STR = "P2P-INVITATION-RESULT";
    public static final int P2P_PROV_DISC_ENTER_PIN_EVENT = 147491;
    private static final String P2P_PROV_DISC_ENTER_PIN_STR = "P2P-PROV-DISC-ENTER-PIN";
    public static final int P2P_PROV_DISC_FAILURE_EVENT = 147495;
    private static final String P2P_PROV_DISC_FAILURE_STR = "P2P-PROV-DISC-FAILURE";
    public static final int P2P_PROV_DISC_PBC_REQ_EVENT = 147489;
    private static final String P2P_PROV_DISC_PBC_REQ_STR = "P2P-PROV-DISC-PBC-REQ";
    public static final int P2P_PROV_DISC_PBC_RSP_EVENT = 147490;
    private static final String P2P_PROV_DISC_PBC_RSP_STR = "P2P-PROV-DISC-PBC-RESP";
    public static final int P2P_PROV_DISC_SHOW_PIN_EVENT = 147492;
    private static final String P2P_PROV_DISC_SHOW_PIN_STR = "P2P-PROV-DISC-SHOW-PIN";
    public static final int P2P_SERV_DISC_RESP_EVENT = 147494;
    private static final String P2P_SERV_DISC_RESP_STR = "P2P-SERV-DISC-RESP";
    private static final String PASSWORD_MAY_BE_INCORRECT_STR = "pre-shared key may be incorrect";
    private static final int REASON_TKIP_ONLY_PROHIBITED = 1;
    private static final int REASON_WEP_PROHIBITED = 2;
    private static final String REENABLED_STR = "SSID-REENABLED";
    public static final int RX_HS20_ANQP_ICON_EVENT = 147509;
    private static final int SCAN_RESULTS = 4;
    public static final int SCAN_RESULTS_EVENT = 147461;
    private static final String SCAN_RESULTS_STR = "SCAN-RESULTS";
    private static final String SIM_STR = "SIM";
    private static final int SSID_REENABLE = 11;
    public static final int SSID_REENABLED = 147470;
    private static final int SSID_TEMP_DISABLE = 10;
    public static final int SSID_TEMP_DISABLED = 147469;
    private static final int STATE_CHANGE = 3;
    private static final String STATE_CHANGE_STR = "STATE-CHANGE";
    public static final int SUPPLICANT_STATE_CHANGE_EVENT = 147462;
    public static final int SUP_CONNECTION_EVENT = 147457;
    public static final int SUP_DISCONNECTION_EVENT = 147458;
    public static final int SUP_REQUEST_IDENTITY = 147471;
    public static final int SUP_REQUEST_SIM_AUTH = 147472;
    private static final String TAG = "WifiMonitor";
    private static final String TARGET_BSSID_STR = "Trying to associate with ";
    private static final String TEMP_DISABLED_STR = "SSID-TEMP-DISABLED";
    private static final int TERMINATING = 6;
    private static final String TERMINATING_STR = "TERMINATING";
    private static final int UNKNOWN = 15;
    private static final String WPA_EVENT_PREFIX_STR = "WPA:";
    private static final String WPA_RECV_ERROR_STR = "recv error";
    public static final int WPS_FAIL_EVENT = 147465;
    private static final String WPS_FAIL_PATTERN = "WPS-FAIL msg=\\d+(?: config_error=(\\d+))?(?: reason=(\\d+))?";
    private static final String WPS_FAIL_STR = "WPS-FAIL";
    public static final int WPS_OVERLAP_EVENT = 147466;
    private static final String WPS_OVERLAP_STR = "WPS-OVERLAP-DETECTED";
    public static final int WPS_SUCCESS_EVENT = 147464;
    private static final String WPS_SUCCESS_STR = "WPS-SUCCESS";
    public static final int WPS_TIMEOUT_EVENT = 147467;
    private static final String WPS_TIMEOUT_STR = "WPS-TIMEOUT";
    private static int sRecvErrors;
    private final String mInterfaceName;
    private boolean mMonitoring;
    private final StateMachine mStateMachine;
    private StateMachine mStateMachine2;
    private final WifiNative mWifiNative;
    private static final boolean VDBG = false;
    private static boolean DBG = VDBG;
    private static final String EVENT_PREFIX_STR = "CTRL-EVENT-";
    private static final int EVENT_PREFIX_LEN_STR = EVENT_PREFIX_STR.length();
    private static final String REQUEST_PREFIX_STR = "CTRL-REQ-";
    private static final int REQUEST_PREFIX_LEN_STR = REQUEST_PREFIX_STR.length();
    private static final String RX_HS20_ANQP_ICON_STR = "RX-HS20-ANQP-ICON";
    private static final int RX_HS20_ANQP_ICON_STR_LEN = RX_HS20_ANQP_ICON_STR.length();
    private static int eventLogCounter = 0;
    private static Pattern mConnectedEventPattern = Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) .* \\[id=([0-9]+) ");
    private static Pattern mDisconnectedEventPattern = Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) +reason=([0-9]+) +locally_generated=([0-1])");
    private static Pattern mAssocRejectEventPattern = Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) +status_code=([0-9]+)");
    private static Pattern mTargetBSSIDPattern = Pattern.compile("Trying to associate with ((?:[0-9a-f]{2}:){5}[0-9a-f]{2}).*");
    private static Pattern mAssociatedPattern = Pattern.compile("Associated with ((?:[0-9a-f]{2}:){5}[0-9a-f]{2}).*");
    private static Pattern mRequestGsmAuthPattern = Pattern.compile("SIM-([0-9]*):GSM-AUTH((:[0-9a-f]+)+) needed for SSID (.+)");
    private static Pattern mRequestUmtsAuthPattern = Pattern.compile("SIM-([0-9]*):UMTS-AUTH:([0-9a-f]+):([0-9a-f]+) needed for SSID (.+)");
    private static Pattern mRequestIdentityPattern = Pattern.compile("IDENTITY-([0-9]+):Identity needed for SSID (.+)");

    public WifiMonitor(StateMachine stateMachine, WifiNative wifiNative) {
        if (DBG) {
            Log.d(TAG, "Creating WifiMonitor");
        }
        this.mWifiNative = wifiNative;
        this.mInterfaceName = wifiNative.mInterfaceName;
        this.mStateMachine = stateMachine;
        this.mStateMachine2 = null;
        this.mMonitoring = VDBG;
        WifiMonitorSingleton.sInstance.registerInterfaceMonitor(this.mInterfaceName, this);
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
        } else {
            DBG = VDBG;
        }
    }

    public void setStateMachine2(StateMachine stateMachine) {
        this.mStateMachine2 = stateMachine;
    }

    public void startMonitoring() {
        WifiMonitorSingleton.sInstance.startMonitoring(this.mInterfaceName);
    }

    public void stopMonitoring() {
        WifiMonitorSingleton.sInstance.stopMonitoring(this.mInterfaceName);
    }

    public void stopSupplicant() {
        WifiMonitorSingleton.sInstance.stopSupplicant();
    }

    public void killSupplicant(int wifiMode) {
        WifiMonitorSingleton.sInstance.killSupplicant(wifiMode);
    }

    private static class WifiMonitorSingleton {
        private static final WifiMonitorSingleton sInstance = new WifiMonitorSingleton();
        private WifiNative mWifiNative;
        private final HashMap<String, WifiMonitor> mIfaceMap = new HashMap<>();
        private boolean mConnected = WifiMonitor.VDBG;

        private WifiMonitorSingleton() {
        }

        public synchronized void startMonitoring(String iface) {
            WifiMonitor m = this.mIfaceMap.get(iface);
            if (m == null) {
                Log.e(WifiMonitor.TAG, "startMonitor called with unknown iface=" + iface);
            } else {
                Log.d(WifiMonitor.TAG, "startMonitoring(" + iface + ") with mConnected = " + this.mConnected);
                if (this.mConnected) {
                    m.mMonitoring = true;
                    m.mStateMachine.sendMessage(WifiMonitor.SUP_CONNECTION_EVENT);
                } else {
                    if (WifiMonitor.DBG) {
                        Log.d(WifiMonitor.TAG, "connecting to supplicant");
                    }
                    int connectTries = 0;
                    while (true) {
                        int connectTries2 = connectTries;
                        if (this.mWifiNative.connectToSupplicant()) {
                            m.mMonitoring = true;
                            m.mStateMachine.sendMessage(WifiMonitor.SUP_CONNECTION_EVENT);
                            new MonitorThread(this.mWifiNative, this).start();
                            this.mConnected = true;
                            break;
                        }
                        connectTries = connectTries2 + 1;
                        if (connectTries2 < 5) {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                            }
                        } else {
                            this.mIfaceMap.remove(iface);
                            m.mStateMachine.sendMessage(WifiMonitor.SUP_DISCONNECTION_EVENT);
                            Log.e(WifiMonitor.TAG, "startMonitoring(" + iface + ") failed!");
                            break;
                        }
                    }
                }
            }
        }

        public synchronized void stopMonitoring(String iface) {
            WifiMonitor m = this.mIfaceMap.get(iface);
            if (WifiMonitor.DBG) {
                Log.d(WifiMonitor.TAG, "stopMonitoring(" + iface + ") = " + m.mStateMachine);
            }
            m.mMonitoring = WifiMonitor.VDBG;
            m.mStateMachine.sendMessage(WifiMonitor.SUP_DISCONNECTION_EVENT);
        }

        public synchronized void registerInterfaceMonitor(String iface, WifiMonitor m) {
            if (WifiMonitor.DBG) {
                Log.d(WifiMonitor.TAG, "registerInterface(" + iface + "+" + m.mStateMachine + ")");
            }
            this.mIfaceMap.put(iface, m);
            if (this.mWifiNative == null) {
                this.mWifiNative = m.mWifiNative;
            }
        }

        public synchronized void unregisterInterfaceMonitor(String iface) {
            WifiMonitor m = this.mIfaceMap.remove(iface);
            if (WifiMonitor.DBG) {
                Log.d(WifiMonitor.TAG, "unregisterInterface(" + iface + "+" + m.mStateMachine + ")");
            }
        }

        public synchronized void stopSupplicant() {
            this.mWifiNative.stopSupplicant();
        }

        public synchronized void killSupplicant(int wifiMode) {
            WifiNative wifiNative = this.mWifiNative;
            WifiNative.killSupplicant(wifiMode);
            this.mConnected = WifiMonitor.VDBG;
            for (WifiMonitor m : this.mIfaceMap.values()) {
                m.mMonitoring = WifiMonitor.VDBG;
            }
        }

        private synchronized boolean dispatchEvent(String eventStr) {
            String iface;
            boolean z = WifiMonitor.VDBG;
            synchronized (this) {
                if (eventStr.startsWith("IFNAME=")) {
                    int space = eventStr.indexOf(32);
                    if (space != -1) {
                        iface = eventStr.substring(7, space);
                        if (!this.mIfaceMap.containsKey(iface) && iface.startsWith("p2p-")) {
                            iface = "p2p0";
                        }
                        eventStr = eventStr.substring(space + 1);
                    } else {
                        Log.e(WifiMonitor.TAG, "Dropping malformed event (unparsable iface): " + eventStr);
                    }
                } else {
                    iface = "p2p0";
                }
                WifiMonitor m = this.mIfaceMap.get(iface);
                if (m != null) {
                    if (m.mMonitoring) {
                        if (m.dispatchEvent(eventStr, iface)) {
                            this.mConnected = WifiMonitor.VDBG;
                            z = true;
                        }
                    } else if (WifiMonitor.DBG) {
                        Log.d(WifiMonitor.TAG, "Dropping event because (" + iface + ") is stopped");
                    }
                } else {
                    if (WifiMonitor.DBG) {
                        Log.d(WifiMonitor.TAG, "Sending to all monitors because there's no matching iface");
                    }
                    int countRunning = 0;
                    for (WifiMonitor monitor : this.mIfaceMap.values()) {
                        if (monitor.mMonitoring) {
                            countRunning++;
                            if (monitor.dispatchEvent(eventStr, iface)) {
                                countRunning--;
                            }
                        }
                    }
                    if (countRunning == 0) {
                        this.mConnected = WifiMonitor.VDBG;
                        z = true;
                    }
                }
            }
            return z;
        }
    }

    private static class MonitorThread extends Thread {
        private final WifiMonitorSingleton mWifiMonitorSingleton;
        private final WifiNative mWifiNative;

        public MonitorThread(WifiNative wifiNative, WifiMonitorSingleton wifiMonitorSingleton) {
            super(WifiMonitor.TAG);
            this.mWifiNative = wifiNative;
            this.mWifiMonitorSingleton = wifiMonitorSingleton;
        }

        @Override
        public void run() {
            String eventStr;
            do {
                eventStr = this.mWifiNative.waitForEvent();
                if (WifiMonitor.DBG && eventStr.indexOf(WifiMonitor.SCAN_RESULTS_STR) == -1) {
                    Log.d(WifiMonitor.TAG, "Event [" + eventStr + "]");
                }
            } while (!this.mWifiMonitorSingleton.dispatchEvent(eventStr));
            if (WifiMonitor.DBG) {
                Log.d(WifiMonitor.TAG, "Disconnecting from the supplicant, no more events");
            }
        }
    }

    private void logDbg(String debug) {
        Log.e(TAG, debug);
    }

    private boolean dispatchEvent(String eventStr, String iface) {
        int event;
        char c;
        if (DBG && eventStr != null && !eventStr.contains("CTRL-EVENT-BSS-ADDED")) {
            logDbg("WifiMonitor:" + iface + " cnt=" + Integer.toString(eventLogCounter) + " dispatchEvent: " + eventStr);
        }
        if (!eventStr.startsWith(EVENT_PREFIX_STR)) {
            if (eventStr.startsWith(WPA_EVENT_PREFIX_STR) && eventStr.indexOf(PASSWORD_MAY_BE_INCORRECT_STR) > 0) {
                this.mStateMachine.sendMessage(AUTHENTICATION_FAILURE_EVENT, eventLogCounter);
            } else if (eventStr.startsWith(WPS_SUCCESS_STR)) {
                this.mStateMachine.sendMessage(WPS_SUCCESS_EVENT);
            } else if (eventStr.startsWith(WPS_FAIL_STR)) {
                handleWpsFailEvent(eventStr);
            } else if (eventStr.startsWith(WPS_OVERLAP_STR)) {
                this.mStateMachine.sendMessage(WPS_OVERLAP_EVENT);
            } else if (eventStr.startsWith(WPS_TIMEOUT_STR)) {
                this.mStateMachine.sendMessage(WPS_TIMEOUT_EVENT);
            } else if (eventStr.startsWith(P2P_EVENT_PREFIX_STR)) {
                handleP2pEvents(eventStr);
            } else if (eventStr.startsWith(HOST_AP_EVENT_PREFIX_STR)) {
                handleHostApEvents(eventStr);
            } else if (eventStr.startsWith(GAS_QUERY_PREFIX_STR)) {
                handleGasQueryEvents(eventStr);
            } else if (eventStr.startsWith(RX_HS20_ANQP_ICON_STR)) {
                if (this.mStateMachine2 != null) {
                    this.mStateMachine2.sendMessage(RX_HS20_ANQP_ICON_EVENT, eventStr.substring(RX_HS20_ANQP_ICON_STR_LEN + 1));
                }
            } else if (eventStr.startsWith(HS20_PREFIX_STR)) {
                handleHs20Events(eventStr);
            } else if (eventStr.startsWith(REQUEST_PREFIX_STR)) {
                handleRequests(eventStr);
            } else if (eventStr.startsWith(TARGET_BSSID_STR)) {
                handleTargetBSSIDEvent(eventStr);
            } else if (eventStr.startsWith(ASSOCIATED_WITH_STR)) {
                handleAssociatedBSSIDEvent(eventStr);
            } else if (DBG) {
                Log.w(TAG, "couldn't identify event type - " + eventStr);
            }
            eventLogCounter++;
            return VDBG;
        }
        String eventName = eventStr.substring(EVENT_PREFIX_LEN_STR);
        int nameEnd = eventName.indexOf(32);
        if (nameEnd != -1) {
            eventName = eventName.substring(0, nameEnd);
        }
        if (eventName.length() == 0) {
            if (DBG) {
                Log.i(TAG, "Received wpa_supplicant event with empty event name");
            }
            eventLogCounter++;
            return VDBG;
        }
        if (eventName.equals(CONNECTED_STR)) {
            event = 1;
        } else if (eventName.equals(DISCONNECTED_STR)) {
            event = 2;
        } else if (eventName.equals(STATE_CHANGE_STR)) {
            event = 3;
        } else if (eventName.equals(SCAN_RESULTS_STR)) {
            event = 4;
        } else if (eventName.equals(LINK_SPEED_STR)) {
            event = 5;
        } else if (eventName.equals(TERMINATING_STR)) {
            event = 6;
        } else if (eventName.equals(AP_TERMINATING_STR)) {
            event = AP_TERMINATING;
        } else if (eventName.equals(DRIVER_STATE_STR)) {
            event = 7;
        } else if (eventName.equals(EAP_FAILURE_STR)) {
            event = 8;
        } else if (eventName.equals(ASSOC_REJECT_STR)) {
            event = 9;
        } else if (eventName.equals(TEMP_DISABLED_STR)) {
            event = 10;
        } else if (eventName.equals(REENABLED_STR)) {
            event = SSID_REENABLE;
        } else if (eventName.equals(BSS_ADDED_STR)) {
            event = 12;
        } else if (eventName.equals(BSS_REMOVED_STR)) {
            event = BSS_REMOVED;
        } else {
            event = UNKNOWN;
        }
        String eventData = eventStr;
        if (event == 7 || event == 5) {
            eventData = eventData.split(" ")[1];
        } else if (event == 3 || event == 8) {
            int ind = eventStr.indexOf(" ");
            if (ind != -1) {
                eventData = eventStr.substring(ind + 1);
            }
        } else {
            int ind2 = eventStr.indexOf(" - ");
            if (ind2 != -1) {
                eventData = eventStr.substring(ind2 + 3);
            }
        }
        if (event == 10 || event == SSID_REENABLE) {
            String substr = null;
            int netId = -1;
            int ind3 = eventStr.indexOf(" ");
            if (ind3 != -1) {
                substr = eventStr.substring(ind3 + 1);
            }
            if (substr != null) {
                String[] status = substr.split(" ");
                for (String key : status) {
                    if (key.regionMatches(0, "id=", 0, 3)) {
                        netId = 0;
                        for (int idx = 3; idx < key.length() && (c = key.charAt(idx)) >= '0' && c <= '9'; idx++) {
                            netId = (netId * 10) + (c - '0');
                        }
                    }
                }
            }
            this.mStateMachine.sendMessage(event == 10 ? SSID_TEMP_DISABLED : SSID_REENABLED, netId, 0, substr);
        } else if (event == 3) {
            handleSupplicantStateChange(eventData);
        } else if (event == 7) {
            handleDriverEvent(eventData);
        } else {
            if (event == 6) {
                if (eventData.startsWith(WPA_RECV_ERROR_STR)) {
                    int i = sRecvErrors + 1;
                    sRecvErrors = i;
                    if (i > 10) {
                        if (DBG) {
                            Log.d(TAG, "too many recv errors, closing connection");
                        }
                    } else {
                        eventLogCounter++;
                        return VDBG;
                    }
                }
                this.mStateMachine.sendMessage(SUP_DISCONNECTION_EVENT, eventLogCounter);
                return true;
            }
            if (event == AP_TERMINATING) {
                this.mStateMachine.sendMessage(AP_DISCONNECTION_EVENT);
                return true;
            }
            if (event == 8) {
                if (eventData.startsWith(EAP_AUTH_FAILURE_STR)) {
                    logDbg("WifiMonitor send auth failure (EAP_AUTH_FAILURE) ");
                    this.mStateMachine.sendMessage(AUTHENTICATION_FAILURE_EVENT, eventLogCounter);
                }
            } else if (event == 9) {
                Matcher match = mAssocRejectEventPattern.matcher(eventData);
                String BSSID = "";
                int status2 = -1;
                if (!match.find()) {
                    if (DBG) {
                        Log.d(TAG, "Assoc Reject: Could not parse assoc reject string");
                    }
                } else {
                    BSSID = match.group(1);
                    try {
                        status2 = Integer.parseInt(match.group(2));
                    } catch (NumberFormatException e) {
                        status2 = -1;
                    }
                }
                this.mStateMachine.sendMessage(ASSOCIATION_REJECTION_EVENT, eventLogCounter, status2, BSSID);
            } else if (event != 12 && event != BSS_REMOVED) {
                handleEvent(event, eventData);
            }
        }
        sRecvErrors = 0;
        eventLogCounter++;
        return VDBG;
    }

    private void handleDriverEvent(String state) {
        if (state != null && state.equals("HANGED")) {
            this.mStateMachine.sendMessage(DRIVER_HUNG_EVENT);
        }
    }

    void handleEvent(int event, String remainder) {
        if (DBG) {
            logDbg("handleEvent " + Integer.toString(event) + "  " + remainder);
        }
        switch (event) {
            case 1:
                handleNetworkStateChange(NetworkInfo.DetailedState.CONNECTED, remainder);
                break;
            case 2:
                handleNetworkStateChange(NetworkInfo.DetailedState.DISCONNECTED, remainder);
                break;
            case 4:
                this.mStateMachine.sendMessage(SCAN_RESULTS_EVENT);
                break;
            case UNKNOWN:
                if (DBG) {
                    logDbg("handleEvent unknown: " + Integer.toString(event) + "  " + remainder);
                }
                break;
        }
    }

    private void handleTargetBSSIDEvent(String eventStr) {
        String BSSID = null;
        Matcher match = mTargetBSSIDPattern.matcher(eventStr);
        if (match.find()) {
            BSSID = match.group(1);
        }
        this.mStateMachine.sendMessage(131213, eventLogCounter, 0, BSSID);
    }

    private void handleAssociatedBSSIDEvent(String eventStr) {
        String BSSID = null;
        Matcher match = mAssociatedPattern.matcher(eventStr);
        if (match.find()) {
            BSSID = match.group(1);
        }
        this.mStateMachine.sendMessage(131219, eventLogCounter, 0, BSSID);
    }

    private void handleWpsFailEvent(String dataString) {
        Pattern p = Pattern.compile(WPS_FAIL_PATTERN);
        Matcher match = p.matcher(dataString);
        int reason = 0;
        if (match.find()) {
            String cfgErrStr = match.group(1);
            String reasonStr = match.group(2);
            if (reasonStr != null) {
                int reasonInt = Integer.parseInt(reasonStr);
                switch (reasonInt) {
                    case 1:
                        this.mStateMachine.sendMessage(this.mStateMachine.obtainMessage(WPS_FAIL_EVENT, 5, 0));
                        return;
                    case 2:
                        this.mStateMachine.sendMessage(this.mStateMachine.obtainMessage(WPS_FAIL_EVENT, 4, 0));
                        return;
                    default:
                        reason = reasonInt;
                        break;
                }
            }
            if (cfgErrStr != null) {
                int cfgErrInt = Integer.parseInt(cfgErrStr);
                switch (cfgErrInt) {
                    case 12:
                        this.mStateMachine.sendMessage(this.mStateMachine.obtainMessage(WPS_FAIL_EVENT, 3, 0));
                        return;
                    case CONFIG_AUTH_FAILURE:
                        this.mStateMachine.sendMessage(this.mStateMachine.obtainMessage(WPS_FAIL_EVENT, 6, 0));
                        return;
                    default:
                        if (reason == 0) {
                            reason = cfgErrInt;
                        }
                        break;
                }
            }
        }
        this.mStateMachine.sendMessage(this.mStateMachine.obtainMessage(WPS_FAIL_EVENT, 0, reason));
    }

    private WifiP2pServiceImpl.P2pStatus p2pError(String dataString) {
        WifiP2pServiceImpl.P2pStatus err = WifiP2pServiceImpl.P2pStatus.UNKNOWN;
        String[] tokens = dataString.split(" ");
        if (tokens.length < 2) {
            return err;
        }
        String[] nameValue = tokens[1].split("=");
        if (nameValue.length != 2) {
            return err;
        }
        if (nameValue[1].equals("FREQ_CONFLICT")) {
            return WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL;
        }
        try {
            err = WifiP2pServiceImpl.P2pStatus.valueOf(Integer.parseInt(nameValue[1]));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return err;
    }

    private void handleP2pEvents(String dataString) {
        if (dataString.startsWith(P2P_DEVICE_FOUND_STR)) {
            this.mStateMachine.sendMessage(P2P_DEVICE_FOUND_EVENT, new WifiP2pDevice(dataString));
            return;
        }
        if (dataString.startsWith(P2P_DEVICE_LOST_STR)) {
            this.mStateMachine.sendMessage(P2P_DEVICE_LOST_EVENT, new WifiP2pDevice(dataString));
            return;
        }
        if (dataString.startsWith(P2P_FIND_STOPPED_STR)) {
            this.mStateMachine.sendMessage(P2P_FIND_STOPPED_EVENT);
            return;
        }
        if (dataString.startsWith(P2P_GO_NEG_REQUEST_STR)) {
            this.mStateMachine.sendMessage(P2P_GO_NEGOTIATION_REQUEST_EVENT, new WifiP2pConfig(dataString));
            return;
        }
        if (dataString.startsWith(P2P_GO_NEG_SUCCESS_STR)) {
            this.mStateMachine.sendMessage(P2P_GO_NEGOTIATION_SUCCESS_EVENT);
            return;
        }
        if (dataString.startsWith(P2P_GO_NEG_FAILURE_STR)) {
            this.mStateMachine.sendMessage(P2P_GO_NEGOTIATION_FAILURE_EVENT, p2pError(dataString));
            return;
        }
        if (dataString.startsWith(P2P_GROUP_FORMATION_SUCCESS_STR)) {
            this.mStateMachine.sendMessage(P2P_GROUP_FORMATION_SUCCESS_EVENT);
            return;
        }
        if (dataString.startsWith(P2P_GROUP_FORMATION_FAILURE_STR)) {
            this.mStateMachine.sendMessage(P2P_GROUP_FORMATION_FAILURE_EVENT, p2pError(dataString));
            return;
        }
        if (dataString.startsWith(P2P_GROUP_STARTED_STR)) {
            this.mStateMachine.sendMessage(P2P_GROUP_STARTED_EVENT, new WifiP2pGroup(dataString));
            return;
        }
        if (dataString.startsWith(P2P_GROUP_REMOVED_STR)) {
            this.mStateMachine.sendMessage(P2P_GROUP_REMOVED_EVENT, new WifiP2pGroup(dataString));
            return;
        }
        if (dataString.startsWith(P2P_INVITATION_RECEIVED_STR)) {
            this.mStateMachine.sendMessage(P2P_INVITATION_RECEIVED_EVENT, new WifiP2pGroup(dataString));
            return;
        }
        if (dataString.startsWith(P2P_INVITATION_RESULT_STR)) {
            this.mStateMachine.sendMessage(P2P_INVITATION_RESULT_EVENT, p2pError(dataString));
            return;
        }
        if (dataString.startsWith(P2P_PROV_DISC_PBC_REQ_STR)) {
            this.mStateMachine.sendMessage(P2P_PROV_DISC_PBC_REQ_EVENT, new WifiP2pProvDiscEvent(dataString));
            return;
        }
        if (dataString.startsWith(P2P_PROV_DISC_PBC_RSP_STR)) {
            this.mStateMachine.sendMessage(P2P_PROV_DISC_PBC_RSP_EVENT, new WifiP2pProvDiscEvent(dataString));
            return;
        }
        if (dataString.startsWith(P2P_PROV_DISC_ENTER_PIN_STR)) {
            this.mStateMachine.sendMessage(P2P_PROV_DISC_ENTER_PIN_EVENT, new WifiP2pProvDiscEvent(dataString));
            return;
        }
        if (dataString.startsWith(P2P_PROV_DISC_SHOW_PIN_STR)) {
            this.mStateMachine.sendMessage(P2P_PROV_DISC_SHOW_PIN_EVENT, new WifiP2pProvDiscEvent(dataString));
            return;
        }
        if (dataString.startsWith(P2P_PROV_DISC_FAILURE_STR)) {
            this.mStateMachine.sendMessage(P2P_PROV_DISC_FAILURE_EVENT);
            return;
        }
        if (dataString.startsWith(P2P_SERV_DISC_RESP_STR)) {
            List<WifiP2pServiceResponse> list = WifiP2pServiceResponse.newInstance(dataString);
            if (list != null) {
                this.mStateMachine.sendMessage(P2P_SERV_DISC_RESP_EVENT, list);
            } else {
                Log.e(TAG, "Null service resp " + dataString);
            }
        }
    }

    private void handleHostApEvents(String dataString) {
        String[] tokens = dataString.split(" ");
        if (tokens[0].equals(AP_STA_CONNECTED_STR)) {
            this.mStateMachine.sendMessage(AP_STA_CONNECTED_EVENT, new WifiP2pDevice(dataString));
        } else if (tokens[0].equals(AP_STA_DISCONNECTED_STR)) {
            this.mStateMachine.sendMessage(AP_STA_DISCONNECTED_EVENT, new WifiP2pDevice(dataString));
        }
    }

    private void handleGasQueryEvents(String dataString) {
        if (this.mStateMachine2 != null) {
            if (dataString.startsWith(GAS_QUERY_START_STR)) {
                this.mStateMachine2.sendMessage(GAS_QUERY_START_EVENT);
                return;
            }
            if (dataString.startsWith(GAS_QUERY_DONE_STR)) {
                String[] dataTokens = dataString.split(" ");
                String bssid = null;
                int success = 0;
                for (String token : dataTokens) {
                    String[] nameValue = token.split("=");
                    if (nameValue.length == 2) {
                        if (nameValue[0].equals("addr")) {
                            bssid = nameValue[1];
                        } else if (nameValue[0].equals("result")) {
                            success = nameValue[1].equals("SUCCESS") ? 1 : 0;
                        }
                    }
                }
                this.mStateMachine2.sendMessage(GAS_QUERY_DONE_EVENT, success, 0, bssid);
                return;
            }
            if (DBG) {
                Log.d(TAG, "Unknown GAS query event: " + dataString);
            }
        }
    }

    private void handleHs20Events(String dataString) {
        if (this.mStateMachine2 != null) {
            if (dataString.startsWith(HS20_SUB_REM_STR)) {
                String[] dataTokens = dataString.split(" ");
                int method = -1;
                String url = null;
                if (dataTokens.length >= 3) {
                    method = Integer.parseInt(dataTokens[1]);
                    url = dataTokens[2];
                }
                this.mStateMachine2.sendMessage(HS20_REMEDIATION_EVENT, method, 0, url);
                return;
            }
            if (dataString.startsWith(HS20_DEAUTH_STR)) {
                int code = -1;
                int delay = -1;
                String url2 = null;
                String[] dataTokens2 = dataString.split(" ");
                if (dataTokens2.length >= 4) {
                    code = Integer.parseInt(dataTokens2[1]);
                    delay = Integer.parseInt(dataTokens2[2]);
                    url2 = dataTokens2[3];
                }
                this.mStateMachine2.sendMessage(HS20_DEAUTH_EVENT, code, delay, url2);
                return;
            }
            if (DBG) {
                Log.d(TAG, "Unknown HS20 event: " + dataString);
            }
        }
    }

    private void handleRequests(String dataString) {
        String SSID = null;
        int reason = -2;
        String requestName = dataString.substring(REQUEST_PREFIX_LEN_STR);
        if (!TextUtils.isEmpty(requestName)) {
            if (requestName.startsWith(IDENTITY_STR)) {
                Matcher match = mRequestIdentityPattern.matcher(requestName);
                if (match.find()) {
                    SSID = match.group(2);
                    try {
                        reason = Integer.parseInt(match.group(1));
                    } catch (NumberFormatException e) {
                        reason = -1;
                    }
                } else {
                    Log.e(TAG, "didn't find SSID " + requestName);
                }
                this.mStateMachine.sendMessage(SUP_REQUEST_IDENTITY, eventLogCounter, reason, SSID);
            }
            if (requestName.startsWith(SIM_STR)) {
                Matcher match2 = mRequestGsmAuthPattern.matcher(requestName);
                if (match2.find()) {
                    WifiStateMachine.SimAuthRequestData data = new WifiStateMachine.SimAuthRequestData();
                    data.networkId = Integer.parseInt(match2.group(1));
                    data.protocol = 4;
                    data.ssid = match2.group(4);
                    data.challenges = match2.group(2).split(":");
                    this.mStateMachine.sendMessage(SUP_REQUEST_SIM_AUTH, data);
                    return;
                }
                Matcher match3 = mRequestUmtsAuthPattern.matcher(requestName);
                if (match3.find()) {
                    WifiStateMachine.SimAuthRequestData data2 = new WifiStateMachine.SimAuthRequestData();
                    data2.networkId = Integer.parseInt(match3.group(1));
                    data2.protocol = 5;
                    data2.ssid = match3.group(4);
                    String[] randAutn = {match3.group(2), match3.group(3)};
                    data2.challenges = randAutn;
                    Log.e(TAG, "receive the challenge rand: " + data2.challenges[0] + " AUTN: " + data2.challenges[1]);
                    this.mStateMachine.sendMessage(SUP_REQUEST_SIM_AUTH, data2);
                    return;
                }
                Log.e(TAG, "couldn't parse SIM auth request - " + requestName);
                return;
            }
            if (DBG) {
                Log.w(TAG, "couldn't identify request type - " + dataString);
            }
        }
    }

    private void handleSupplicantStateChange(String dataString) {
        WifiSsid wifiSsid = null;
        int index = dataString.lastIndexOf("SSID=");
        if (index != -1) {
            wifiSsid = WifiSsid.createFromAsciiEncoded(dataString.substring(index + 5));
        }
        String[] dataTokens = dataString.split(" ");
        String BSSID = null;
        int networkId = -1;
        int newState = -1;
        for (String token : dataTokens) {
            String[] nameValue = token.split("=");
            if (nameValue.length == 2) {
                if (nameValue[0].equals("BSSID")) {
                    BSSID = nameValue[1];
                } else {
                    try {
                        int value = Integer.parseInt(nameValue[1]);
                        if (nameValue[0].equals("id")) {
                            networkId = value;
                        } else if (nameValue[0].equals("state")) {
                            newState = value;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        if (newState != -1) {
            SupplicantState newSupplicantState = SupplicantState.INVALID;
            SupplicantState[] arr$ = SupplicantState.values();
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                SupplicantState state = arr$[i$];
                if (state.ordinal() != newState) {
                    i$++;
                } else {
                    newSupplicantState = state;
                    break;
                }
            }
            if (newSupplicantState == SupplicantState.INVALID) {
                Log.w(TAG, "Invalid supplicant state: " + newState);
            }
            notifySupplicantStateChange(networkId, wifiSsid, BSSID, newSupplicantState);
        }
    }

    private void handleNetworkStateChange(NetworkInfo.DetailedState newState, String data) {
        String BSSID = null;
        int networkId = -1;
        int reason = 0;
        int local = 0;
        if (newState == NetworkInfo.DetailedState.CONNECTED) {
            Matcher match = mConnectedEventPattern.matcher(data);
            if (!match.find()) {
                if (DBG) {
                    Log.d(TAG, "handleNetworkStateChange: Couldnt find BSSID in event string");
                }
            } else {
                BSSID = match.group(1);
                try {
                    networkId = Integer.parseInt(match.group(2));
                } catch (NumberFormatException e) {
                    networkId = -1;
                }
            }
            notifyNetworkStateChange(newState, BSSID, networkId, 0);
            return;
        }
        if (newState == NetworkInfo.DetailedState.DISCONNECTED) {
            Matcher match2 = mDisconnectedEventPattern.matcher(data);
            if (!match2.find()) {
                if (DBG) {
                    Log.d(TAG, "handleNetworkStateChange: Could not parse disconnect string");
                }
            } else {
                BSSID = match2.group(1);
                try {
                    reason = Integer.parseInt(match2.group(2));
                } catch (NumberFormatException e2) {
                    reason = -1;
                }
                try {
                    local = Integer.parseInt(match2.group(3));
                } catch (NumberFormatException e3) {
                    local = -1;
                }
            }
            notifyNetworkStateChange(newState, BSSID, local, reason);
        }
    }

    void notifyNetworkStateChange(NetworkInfo.DetailedState newState, String BSSID, int netId, int reason) {
        if (newState == NetworkInfo.DetailedState.CONNECTED) {
            Message m = this.mStateMachine.obtainMessage(NETWORK_CONNECTION_EVENT, netId, reason, BSSID);
            this.mStateMachine.sendMessage(m);
        } else {
            Message m2 = this.mStateMachine.obtainMessage(NETWORK_DISCONNECTION_EVENT, netId, reason, BSSID);
            if (DBG) {
                logDbg("WifiMonitor notify network disconnect: " + BSSID + " reason=" + Integer.toString(reason));
            }
            this.mStateMachine.sendMessage(m2);
        }
    }

    void notifySupplicantStateChange(int networkId, WifiSsid wifiSsid, String BSSID, SupplicantState newState) {
        this.mStateMachine.sendMessage(this.mStateMachine.obtainMessage(SUPPLICANT_STATE_CHANGE_EVENT, eventLogCounter, 0, new StateChangeResult(networkId, wifiSsid, BSSID, newState)));
    }
}
