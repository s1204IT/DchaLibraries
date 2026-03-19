package com.mediatek.server.wifi;

import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MtkWifiApMonitor {
    public static final int AP_STA_CONNECTED_EVENT = 147498;
    public static final int AP_STA_DISCONNECTED_EVENT = 147497;
    private static final int BASE = 147456;
    public static final int SUP_CONNECTION_EVENT = 147457;
    public static final int SUP_DISCONNECTION_EVENT = 147458;
    private static final String TAG = "MtkWifiApMonitor";
    private static final Map<String, SparseArray<Set<Handler>>> sHandlerMap = new HashMap();
    private static final Map<String, Boolean> sMonitoringMap = new HashMap();

    public static synchronized void registerHandler(String str, int i, Handler handler) {
        SparseArray<Set<Handler>> sparseArray = sHandlerMap.get(str);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>();
            sHandlerMap.put(str, sparseArray);
        }
        Set<Handler> arraySet = sparseArray.get(i);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            sparseArray.put(i, arraySet);
        }
        arraySet.add(handler);
    }

    public static synchronized void deregisterAllHandler() {
        sHandlerMap.clear();
    }

    private static boolean isMonitoring(String str) {
        Boolean bool = sMonitoringMap.get(str);
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    @VisibleForTesting
    public static void setMonitoring(String str, boolean z) {
        sMonitoringMap.put(str, Boolean.valueOf(z));
    }

    private static void setMonitoringNone() {
        Iterator<String> it = sMonitoringMap.keySet().iterator();
        while (it.hasNext()) {
            setMonitoring(it.next(), false);
        }
    }

    public static synchronized void startMonitoring(String str) {
        Log.d(TAG, "startMonitoring(" + str + ")");
        setMonitoring(str, true);
        broadcastSupplicantConnectionEvent(str);
    }

    public static synchronized void stopMonitoring(String str) {
        Log.d(TAG, "stopMonitoring(" + str + ")");
        setMonitoring(str, true);
        broadcastSupplicantDisconnectionEvent(str);
        setMonitoring(str, false);
    }

    public static synchronized void stopAllMonitoring() {
        setMonitoringNone();
    }

    private static void sendMessage(String str, int i) {
        sendMessage(str, Message.obtain((Handler) null, i));
    }

    private static void sendMessage(String str, int i, Object obj) {
        sendMessage(str, Message.obtain(null, i, obj));
    }

    private static void sendMessage(String str, int i, int i2) {
        sendMessage(str, Message.obtain(null, i, i2, 0));
    }

    private static void sendMessage(String str, int i, int i2, int i3) {
        sendMessage(str, Message.obtain(null, i, i2, i3));
    }

    private static void sendMessage(String str, int i, int i2, int i3, Object obj) {
        sendMessage(str, Message.obtain(null, i, i2, i3, obj));
    }

    private static void sendMessage(String str, Message message) {
        SparseArray<Set<Handler>> sparseArray = sHandlerMap.get(str);
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
            } else {
                Log.d(TAG, "Dropping event because (" + str + ") is stopped");
            }
        } else {
            Log.d(TAG, "Sending to all monitors because there's no matching iface");
            for (Map.Entry<String, SparseArray<Set<Handler>>> entry : sHandlerMap.entrySet()) {
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

    private static void sendMessage(Handler handler, Message message) {
        message.setTarget(handler);
        message.sendToTarget();
    }

    public static void broadcastSupplicantConnectionEvent(String str) {
        sendMessage(str, 147457);
    }

    public static void broadcastSupplicantDisconnectionEvent(String str) {
        sendMessage(str, 147458);
    }

    public static void broadcastApStaConnected(String str, String str2) {
        sendMessage(str, 147498, str2);
    }

    public static void broadcastApStaDisconnected(String str, String str2) {
        sendMessage(str, 147497, str2);
    }
}
