package com.android.server.connectivity;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.os.Binder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.IoThread;
import com.android.server.net.BaseNetworkObserver;
import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Tethering extends BaseNetworkObserver {
    private static final boolean DBG = true;
    private static final String DNS_DEFAULT_SERVER1 = "8.8.8.8";
    private static final String DNS_DEFAULT_SERVER2 = "8.8.4.4";
    private static final String TAG = "Tethering";
    private static final String USB_NEAR_IFACE_ADDR = "192.168.42.129";
    private static final int USB_PREFIX_LENGTH = 24;
    private static final boolean VDBG = true;
    private Context mContext;
    private String[] mDefaultDnsServers;
    private String[] mDhcpRange;
    String mIPv6TetheredInterface;
    private Looper mLooper;
    private final INetworkManagementService mNMService;
    private boolean mRndisEnabled;
    private BroadcastReceiver mStateReceiver;
    private final INetworkStatsService mStatsService;
    private StateMachine mTetherMasterSM;
    private String[] mTetherableBluetoothRegexs;
    private String[] mTetherableUsbRegexs;
    private String[] mTetherableWifiRegexs;
    private Notification mTetheredNotification;
    private Collection<Integer> mUpstreamIfaceTypes;
    private boolean mUsbTetherRequested;
    private static final Integer MOBILE_TYPE = new Integer(0);
    private static final Integer HIPRI_TYPE = new Integer(5);
    private static final Integer DUN_TYPE = new Integer(4);
    private static final String[] DHCP_DEFAULT_RANGE = {"192.168.42.2", "192.168.42.254", "192.168.43.2", "192.168.43.254", "192.168.44.2", "192.168.44.254", "192.168.45.2", "192.168.45.254", "192.168.46.2", "192.168.46.254", "192.168.47.2", "192.168.47.254", "192.168.48.2", "192.168.48.254", "192.168.49.2", "192.168.49.254"};
    private int mPreferredUpstreamMobileApn = -1;
    private Object mPublicSync = new Object();
    private HashMap<String, TetherInterfaceSM> mIfaces = new HashMap<>();

    public Tethering(Context context, INetworkManagementService nmService, INetworkStatsService statsService, Looper looper) {
        this.mContext = context;
        this.mNMService = nmService;
        this.mStatsService = statsService;
        this.mLooper = looper;
        this.mLooper = IoThread.get().getLooper();
        this.mTetherMasterSM = new TetherMasterSM("TetherMaster", this.mLooper);
        this.mTetherMasterSM.start();
        this.mStateReceiver = new StateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.net.wifi.WIFI_AP_CLIENT_CHANGED");
        this.mContext.registerReceiver(this.mStateReceiver, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.MEDIA_SHARED");
        filter2.addAction("android.intent.action.MEDIA_UNSHARED");
        filter2.addDataScheme("file");
        this.mContext.registerReceiver(this.mStateReceiver, filter2);
        this.mDhcpRange = context.getResources().getStringArray(R.array.config_autoBrightnessLcdBacklightValues);
        if (this.mDhcpRange.length == 0 || this.mDhcpRange.length % 2 == 1) {
            this.mDhcpRange = DHCP_DEFAULT_RANGE;
        }
        updateConfiguration();
        this.mDefaultDnsServers = new String[2];
        this.mDefaultDnsServers[0] = DNS_DEFAULT_SERVER1;
        this.mDefaultDnsServers[1] = DNS_DEFAULT_SERVER2;
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    void updateConfiguration() {
        String[] tetherableUsbRegexs = this.mContext.getResources().getStringArray(R.array.config_angleAllowList);
        String[] tetherableWifiRegexs = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessButtonBacklightValues);
        String[] tetherableBluetoothRegexs = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessDisplayValuesNitsIdle);
        int[] ifaceTypes = this.mContext.getResources().getIntArray(R.array.config_autoBrightnessLevels);
        Collection<Integer> upstreamIfaceTypes = new ArrayList<>();
        for (int i : ifaceTypes) {
            upstreamIfaceTypes.add(new Integer(i));
        }
        synchronized (this.mPublicSync) {
            this.mTetherableUsbRegexs = tetherableUsbRegexs;
            this.mTetherableWifiRegexs = tetherableWifiRegexs;
            this.mTetherableBluetoothRegexs = tetherableBluetoothRegexs;
            this.mUpstreamIfaceTypes = upstreamIfaceTypes;
        }
        checkDunRequired();
    }

    public void interfaceStatusChanged(String iface, boolean up) {
        Log.d(TAG, "interfaceStatusChanged " + iface + ", " + up);
        boolean found = false;
        boolean usb = false;
        synchronized (this.mPublicSync) {
            if (isWifi(iface)) {
                found = true;
            } else if (isUsb(iface)) {
                found = true;
                usb = true;
            } else if (isBluetooth(iface)) {
                found = true;
            }
            if (found) {
                TetherInterfaceSM sm = this.mIfaces.get(iface);
                if (up) {
                    if (sm == null) {
                        TetherInterfaceSM sm2 = new TetherInterfaceSM(iface, this.mLooper, usb);
                        this.mIfaces.put(iface, sm2);
                        sm2.start();
                    }
                } else if (isUsb(iface)) {
                    Log.d(TAG, "ignore interface down for " + iface);
                } else if (sm != null) {
                    sm.sendMessage(4);
                    this.mIfaces.remove(iface);
                }
            }
        }
    }

    public void interfaceLinkStateChanged(String iface, boolean up) {
        Log.d(TAG, "interfaceLinkStateChanged " + iface + ", " + up);
        interfaceStatusChanged(iface, up);
    }

    private boolean isUsb(String iface) {
        boolean z;
        synchronized (this.mPublicSync) {
            String[] arr$ = this.mTetherableUsbRegexs;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ < len$) {
                    String regex = arr$[i$];
                    if (iface.matches(regex)) {
                        z = true;
                        break;
                    }
                    i$++;
                } else {
                    z = false;
                    break;
                }
            }
        }
        return z;
    }

    public boolean isWifi(String iface) {
        boolean z;
        synchronized (this.mPublicSync) {
            String[] arr$ = this.mTetherableWifiRegexs;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ < len$) {
                    String regex = arr$[i$];
                    if (iface.matches(regex)) {
                        z = true;
                        break;
                    }
                    i$++;
                } else {
                    z = false;
                    break;
                }
            }
        }
        return z;
    }

    public boolean isBluetooth(String iface) {
        boolean z;
        synchronized (this.mPublicSync) {
            String[] arr$ = this.mTetherableBluetoothRegexs;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ < len$) {
                    String regex = arr$[i$];
                    if (iface.matches(regex)) {
                        z = true;
                        break;
                    }
                    i$++;
                } else {
                    z = false;
                    break;
                }
            }
        }
        return z;
    }

    public void interfaceAdded(String iface) {
        Log.d(TAG, "interfaceAdded " + iface);
        boolean found = false;
        boolean usb = false;
        synchronized (this.mPublicSync) {
            if (!isWifi(iface)) {
                if (isUsb(iface)) {
                    found = true;
                    usb = true;
                }
                if (isBluetooth(iface)) {
                    found = true;
                }
                if (!found) {
                    Log.d(TAG, iface + " is not a tetherable iface, ignoring");
                } else {
                    if (this.mIfaces.get(iface) != null) {
                        Log.d(TAG, "active iface (" + iface + ") reported as added, ignoring");
                        return;
                    }
                    TetherInterfaceSM sm = new TetherInterfaceSM(iface, this.mLooper, usb);
                    this.mIfaces.put(iface, sm);
                    sm.start();
                }
            }
        }
    }

    public void interfaceRemoved(String iface) {
        Log.d(TAG, "interfaceRemoved " + iface);
        synchronized (this.mPublicSync) {
            TetherInterfaceSM sm = this.mIfaces.get(iface);
            if (sm == null) {
                Log.e(TAG, "attempting to remove unknown iface (" + iface + "), ignoring");
            } else {
                sm.sendMessage(4);
                this.mIfaces.remove(iface);
            }
        }
    }

    public int tether(String iface) {
        TetherInterfaceSM sm;
        Log.d(TAG, "Tethering " + iface);
        synchronized (this.mPublicSync) {
            sm = this.mIfaces.get(iface);
        }
        if (sm == null) {
            Log.e(TAG, "Tried to Tether an unknown iface :" + iface + ", ignoring");
            return 1;
        }
        if (!sm.isAvailable() && !sm.isErrored()) {
            Log.e(TAG, "Tried to Tether an unavailable iface :" + iface + ", ignoring");
            return 4;
        }
        sm.sendMessage(2);
        return 0;
    }

    public int untether(String iface) {
        TetherInterfaceSM sm;
        Log.d(TAG, "Untethering " + iface);
        synchronized (this.mPublicSync) {
            sm = this.mIfaces.get(iface);
        }
        if (sm == null) {
            Log.e(TAG, "Tried to Untether an unknown iface :" + iface + ", ignoring");
            return 1;
        }
        if (sm.isErrored()) {
            Log.e(TAG, "Tried to Untethered an errored iface :" + iface + ", ignoring");
            return 4;
        }
        sm.sendMessage(3);
        return 0;
    }

    public int getLastTetherError(String iface) {
        int lastError;
        synchronized (this.mPublicSync) {
            TetherInterfaceSM sm = this.mIfaces.get(iface);
            if (sm == null) {
                Log.e(TAG, "Tried to getLastTetherError on an unknown iface :" + iface + ", ignoring");
                lastError = 1;
            } else {
                lastError = sm.getLastError();
            }
        }
        return lastError;
    }

    private void sendTetherStateChangedBroadcast() {
        if (getConnectivityManager().isTetheringSupported()) {
            ArrayList<String> availableList = new ArrayList<>();
            ArrayList<String> activeList = new ArrayList<>();
            ArrayList<String> erroredList = new ArrayList<>();
            boolean wifiTethered = false;
            boolean usbTethered = false;
            boolean bluetoothTethered = false;
            synchronized (this.mPublicSync) {
                for (Object iface : this.mIfaces.keySet()) {
                    TetherInterfaceSM sm = this.mIfaces.get(iface);
                    if (sm != null) {
                        if (sm.isErrored()) {
                            erroredList.add((String) iface);
                        } else if (sm.isAvailable()) {
                            availableList.add((String) iface);
                        } else if (sm.isTethered()) {
                            if (isUsb((String) iface)) {
                                usbTethered = true;
                            } else if (isWifi((String) iface)) {
                                wifiTethered = true;
                            } else if (isBluetooth((String) iface)) {
                                bluetoothTethered = true;
                            }
                            activeList.add((String) iface);
                        }
                    }
                }
            }
            Intent broadcast = new Intent("android.net.conn.TETHER_STATE_CHANGED");
            broadcast.addFlags(603979776);
            broadcast.putStringArrayListExtra("availableArray", availableList);
            broadcast.putStringArrayListExtra("activeArray", activeList);
            broadcast.putStringArrayListExtra("erroredArray", erroredList);
            this.mContext.sendStickyBroadcastAsUser(broadcast, UserHandle.ALL);
            Log.d(TAG, "sendTetherStateChangedBroadcast " + availableList.size() + ", " + activeList.size() + ", " + erroredList.size());
            if (usbTethered) {
                if (wifiTethered || bluetoothTethered) {
                    showTetheredNotification(R.drawable.keyboard_key_feedback_background);
                    return;
                } else {
                    showTetheredNotification(R.drawable.keyboard_key_feedback_more_background);
                    return;
                }
            }
            if (wifiTethered) {
                if (bluetoothTethered) {
                    showTetheredNotification(R.drawable.keyboard_key_feedback_background);
                    return;
                } else {
                    clearTetheredNotification();
                    return;
                }
            }
            if (bluetoothTethered) {
                showTetheredNotification(R.drawable.keyboard_key_feedback);
            } else {
                clearTetheredNotification();
            }
        }
    }

    private void showTetheredNotification(int icon) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null) {
            if (this.mTetheredNotification != null) {
                if (this.mTetheredNotification.icon != icon) {
                    notificationManager.cancelAsUser(null, this.mTetheredNotification.icon, UserHandle.ALL);
                } else {
                    return;
                }
            }
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            intent.setFlags(1073741824);
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(R.string.kg_failed_attempts_now_wiping);
            CharSequence message = r.getText(R.string.kg_forgot_pattern_button_text);
            if (this.mTetheredNotification == null) {
                this.mTetheredNotification = new Notification();
                this.mTetheredNotification.when = 0L;
            }
            this.mTetheredNotification.icon = icon;
            this.mTetheredNotification.defaults &= -2;
            this.mTetheredNotification.flags = 2;
            this.mTetheredNotification.tickerText = title;
            this.mTetheredNotification.visibility = 1;
            this.mTetheredNotification.color = this.mContext.getResources().getColor(R.color.system_accent3_600);
            this.mTetheredNotification.setLatestEventInfo(this.mContext, title, message, pi);
            this.mTetheredNotification.category = "status";
            notificationManager.notifyAsUser(null, this.mTetheredNotification.icon, this.mTetheredNotification, UserHandle.ALL);
        }
    }

    public void updateNotification(int client_number) {
        CharSequence title;
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null) {
            if (this.mTetheredNotification != null) {
                notificationManager.cancelAsUser(null, this.mTetheredNotification.icon, UserHandle.ALL);
            }
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            intent.setFlags(1073741824);
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            Resources r = Resources.getSystem();
            if (client_number == 1) {
                title = "1 device connected";
            } else if (client_number > 1) {
                title = client_number + " devices connected";
            } else {
                title = r.getText(R.string.kg_failed_attempts_now_wiping);
            }
            CharSequence message = r.getText(R.string.kg_forgot_pattern_button_text);
            if (this.mTetheredNotification == null) {
                this.mTetheredNotification = new Notification();
                this.mTetheredNotification.when = 0L;
            }
            this.mTetheredNotification.icon = this.mTetheredNotification.icon;
            this.mTetheredNotification.defaults &= -2;
            this.mTetheredNotification.flags = 2;
            this.mTetheredNotification.tickerText = title;
            this.mTetheredNotification.setLatestEventInfo(this.mContext, title, message, pi);
            notificationManager.notifyAsUser(null, this.mTetheredNotification.icon, this.mTetheredNotification, UserHandle.ALL);
        }
    }

    private void clearTetheredNotification() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null && this.mTetheredNotification != null) {
            notificationManager.cancelAsUser(null, this.mTetheredNotification.icon, UserHandle.ALL);
            this.mTetheredNotification = null;
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        private StateReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.hardware.usb.action.USB_STATE")) {
                    synchronized (Tethering.this.mPublicSync) {
                        boolean usbConnected = intent.getBooleanExtra("connected", false);
                        Tethering.this.mRndisEnabled = intent.getBooleanExtra("rndis", false);
                        if (usbConnected && Tethering.this.mRndisEnabled && Tethering.this.mUsbTetherRequested) {
                            Tethering.this.tetherUsb(true);
                        }
                        Tethering.this.mUsbTetherRequested = false;
                    }
                    return;
                }
                if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null && networkInfo.getDetailedState() != NetworkInfo.DetailedState.FAILED) {
                        Log.d(Tethering.TAG, "Tethering got CONNECTIVITY_ACTION");
                        Tethering.this.mTetherMasterSM.sendMessage(3);
                        return;
                    }
                    return;
                }
                if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    Tethering.this.updateConfiguration();
                } else if (action.equals("android.net.wifi.WIFI_AP_CLIENT_CHANGED")) {
                    int client_number = intent.getIntExtra("wifi_ap_client", 0);
                    Tethering.this.updateNotification(client_number);
                }
            }
        }
    }

    private void tetherUsb(boolean enable) {
        Log.d(TAG, "tetherUsb " + enable);
        String[] strArr = new String[0];
        try {
            String[] ifaces = this.mNMService.listInterfaces();
            for (String iface : ifaces) {
                if (isUsb(iface)) {
                    int result = enable ? tether(iface) : untether(iface);
                    if (result == 0) {
                        return;
                    }
                }
            }
            Log.e(TAG, "unable start or stop USB tethering");
        } catch (Exception e) {
            Log.e(TAG, "Error listing Interfaces", e);
        }
    }

    private boolean configureUsbIface(boolean enabled) {
        Log.d(TAG, "configureUsbIface(" + enabled + ")");
        String[] strArr = new String[0];
        try {
            String[] ifaces = this.mNMService.listInterfaces();
            for (String iface : ifaces) {
                if (isUsb(iface)) {
                    try {
                        InterfaceConfiguration ifcg = this.mNMService.getInterfaceConfig(iface);
                        if (ifcg != null) {
                            InetAddress addr = NetworkUtils.numericToInetAddress(USB_NEAR_IFACE_ADDR);
                            ifcg.setLinkAddress(new LinkAddress(addr, 24));
                            if (enabled) {
                                ifcg.setInterfaceUp();
                            } else {
                                ifcg.setInterfaceDown();
                            }
                            ifcg.clearFlag("running");
                            this.mNMService.setInterfaceConfig(iface, ifcg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error configuring interface " + iface, e);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e2) {
            Log.e(TAG, "Error listing Interfaces", e2);
            return false;
        }
    }

    public String[] getTetherableUsbRegexs() {
        return this.mTetherableUsbRegexs;
    }

    public String[] getTetherableWifiRegexs() {
        return this.mTetherableWifiRegexs;
    }

    public String[] getTetherableBluetoothRegexs() {
        return this.mTetherableBluetoothRegexs;
    }

    public int setUsbTethering(boolean enable) {
        Log.d(TAG, "setUsbTethering(" + enable + ")");
        UsbManager usbManager = (UsbManager) this.mContext.getSystemService("usb");
        synchronized (this.mPublicSync) {
            if (enable) {
                this.mUsbTetherRequested = true;
                usbManager.setCurrentFunction("rndis", false);
            } else {
                tetherUsb(false);
                if (this.mRndisEnabled) {
                    usbManager.setCurrentFunction(null, false);
                }
                this.mUsbTetherRequested = false;
            }
        }
        return 0;
    }

    public int[] getUpstreamIfaceTypes() {
        int[] values;
        synchronized (this.mPublicSync) {
            updateConfiguration();
            values = new int[this.mUpstreamIfaceTypes.size()];
            Iterator<Integer> iterator = this.mUpstreamIfaceTypes.iterator();
            for (int i = 0; i < this.mUpstreamIfaceTypes.size(); i++) {
                values[i] = iterator.next().intValue();
            }
        }
        return values;
    }

    public void checkDunRequired() {
        int secureSetting = 2;
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm != null) {
            secureSetting = tm.getTetherApnRequired();
        }
        synchronized (this.mPublicSync) {
            if (secureSetting != 2) {
                int requiredApn = secureSetting == 1 ? 4 : 5;
                if (requiredApn == 4) {
                    while (this.mUpstreamIfaceTypes.contains(MOBILE_TYPE)) {
                        this.mUpstreamIfaceTypes.remove(MOBILE_TYPE);
                    }
                    while (this.mUpstreamIfaceTypes.contains(HIPRI_TYPE)) {
                        this.mUpstreamIfaceTypes.remove(HIPRI_TYPE);
                    }
                    if (!this.mUpstreamIfaceTypes.contains(DUN_TYPE)) {
                        this.mUpstreamIfaceTypes.add(DUN_TYPE);
                    }
                } else {
                    while (this.mUpstreamIfaceTypes.contains(DUN_TYPE)) {
                        this.mUpstreamIfaceTypes.remove(DUN_TYPE);
                    }
                    if (!this.mUpstreamIfaceTypes.contains(MOBILE_TYPE)) {
                        this.mUpstreamIfaceTypes.add(MOBILE_TYPE);
                    }
                    if (!this.mUpstreamIfaceTypes.contains(HIPRI_TYPE)) {
                        this.mUpstreamIfaceTypes.add(HIPRI_TYPE);
                    }
                }
            }
            if (this.mUpstreamIfaceTypes.contains(DUN_TYPE)) {
                this.mPreferredUpstreamMobileApn = 4;
            } else {
                this.mPreferredUpstreamMobileApn = 5;
            }
        }
    }

    public String[] getTetheredIfacePairs() {
        ArrayList<String> list = Lists.newArrayList();
        synchronized (this.mPublicSync) {
            for (TetherInterfaceSM sm : this.mIfaces.values()) {
                if (sm.isTethered()) {
                    list.add(sm.mMyUpstreamInterfaces == null ? null : sm.mMyUpstreamInterfaces.getUpstreamV4IfaceName());
                    list.add(sm.mIfaceName);
                }
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public String[] getTetheredIfaces() {
        ArrayList<String> list = new ArrayList<>();
        synchronized (this.mPublicSync) {
            for (Object key : this.mIfaces.keySet()) {
                TetherInterfaceSM sm = this.mIfaces.get(key);
                if (sm.isTethered()) {
                    list.add((String) key);
                }
            }
        }
        String[] retVal = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            retVal[i] = list.get(i);
        }
        return retVal;
    }

    public String[] getTetherableIfaces() {
        ArrayList<String> list = new ArrayList<>();
        synchronized (this.mPublicSync) {
            for (Object key : this.mIfaces.keySet()) {
                TetherInterfaceSM sm = this.mIfaces.get(key);
                if (sm.isAvailable()) {
                    list.add((String) key);
                }
            }
        }
        String[] retVal = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            retVal[i] = list.get(i);
        }
        return retVal;
    }

    public String[] getTetheredDhcpRanges() {
        return this.mDhcpRange;
    }

    public String[] getErroredIfaces() {
        ArrayList<String> list = new ArrayList<>();
        synchronized (this.mPublicSync) {
            for (Object key : this.mIfaces.keySet()) {
                TetherInterfaceSM sm = this.mIfaces.get(key);
                if (sm.isErrored()) {
                    list.add((String) key);
                }
            }
        }
        String[] retVal = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            retVal[i] = list.get(i);
        }
        return retVal;
    }

    class UpstreamInterfaces {
        private LinkProperties mLinkProperties;
        private String mUpstreamV4IfaceName;
        private String mUpstreamV6IfaceName;

        public UpstreamInterfaces(LinkProperties lp) {
            this.mUpstreamV4IfaceName = null;
            this.mUpstreamV6IfaceName = null;
            this.mLinkProperties = null;
            this.mLinkProperties = lp;
            if (lp != null) {
                this.mUpstreamV4IfaceName = lp.getInterfaceName();
                if (getUpstreamV6Address() != null) {
                    this.mUpstreamV6IfaceName = lp.getInterfaceName();
                }
            }
        }

        public String getUpstreamV4IfaceName() {
            return this.mUpstreamV4IfaceName;
        }

        public String getUpstreamV6IfaceName() {
            return this.mUpstreamV6IfaceName;
        }

        public String getUpstreamV6Address() {
            if (this.mLinkProperties == null) {
                return null;
            }
            Collection<InetAddress> addresses = this.mLinkProperties.getAddresses();
            for (InetAddress address : addresses) {
                if (address instanceof Inet6Address) {
                    String v6address = address.getHostAddress();
                    return v6address;
                }
            }
            return null;
        }

        public void setUpstreamV4IfaceName(String iface) {
            this.mUpstreamV4IfaceName = iface;
        }

        public void setUpstreamV6IfaceName(String iface) {
            this.mUpstreamV6IfaceName = iface;
        }

        public String toString() {
            return "v4=" + this.mUpstreamV4IfaceName + ", v6=" + this.mUpstreamV6IfaceName;
        }

        public boolean equals(UpstreamInterfaces other) {
            if (other == null) {
                return false;
            }
            String other_v6Iface = other.getUpstreamV6IfaceName();
            String other_v4Iface = other.getUpstreamV4IfaceName();
            boolean v6equal = this.mUpstreamV6IfaceName == null ? other_v6Iface == null : this.mUpstreamV6IfaceName.equals(other_v6Iface);
            boolean v4equal = this.mUpstreamV4IfaceName == null ? other_v4Iface == null : this.mUpstreamV4IfaceName.equals(other_v4Iface);
            return v6equal && v4equal;
        }

        public boolean isEmpty() {
            return this.mUpstreamV4IfaceName == null && this.mUpstreamV6IfaceName == null;
        }
    }

    class TetherInterfaceSM extends StateMachine {
        static final int CMD_CELL_DUN_ERROR = 6;
        static final int CMD_INTERFACE_DOWN = 4;
        static final int CMD_INTERFACE_UP = 5;
        static final int CMD_IP_FORWARDING_DISABLE_ERROR = 8;
        static final int CMD_IP_FORWARDING_ENABLE_ERROR = 7;
        static final int CMD_SET_DNS_FORWARDERS_ERROR = 11;
        static final int CMD_START_TETHERING_ERROR = 9;
        static final int CMD_STOP_TETHERING_ERROR = 10;
        static final int CMD_TETHER_CONNECTION_CHANGED = 12;
        static final int CMD_TETHER_MODE_DEAD = 1;
        static final int CMD_TETHER_REQUESTED = 2;
        static final int CMD_TETHER_UNREQUESTED = 3;
        private boolean mAvailable;
        private State mDefaultState;
        String mIfaceName;
        private State mInitialState;
        int mLastError;
        UpstreamInterfaces mMyUpstreamInterfaces;
        private State mStartingState;
        private boolean mTethered;
        private State mTetheredState;
        private State mUnavailableState;
        boolean mUsb;

        TetherInterfaceSM(String name, Looper looper, boolean usb) {
            super(name, looper);
            this.mIfaceName = name;
            this.mUsb = usb;
            setLastError(0);
            this.mInitialState = new InitialState();
            addState(this.mInitialState);
            this.mStartingState = new StartingState();
            addState(this.mStartingState);
            this.mTetheredState = new TetheredState();
            addState(this.mTetheredState);
            this.mUnavailableState = new UnavailableState();
            addState(this.mUnavailableState);
            setInitialState(this.mInitialState);
        }

        public String toString() {
            String res = new String() + this.mIfaceName + " - ";
            State currentState = getCurrentState();
            if (currentState == this.mInitialState) {
                res = res + "InitialState";
            }
            if (currentState == this.mStartingState) {
                res = res + "StartingState";
            }
            if (currentState == this.mTetheredState) {
                res = res + "TetheredState";
            }
            if (currentState == this.mUnavailableState) {
                res = res + "UnavailableState";
            }
            if (this.mAvailable) {
                res = res + " - Available";
            }
            if (this.mTethered) {
                res = res + " - Tethered";
            }
            return res + " - lastError =" + this.mLastError;
        }

        public int getLastError() {
            int i;
            synchronized (Tethering.this.mPublicSync) {
                i = this.mLastError;
            }
            return i;
        }

        private void setLastError(int error) {
            synchronized (Tethering.this.mPublicSync) {
                this.mLastError = error;
                if (isErrored() && this.mUsb) {
                    Tethering.this.configureUsbIface(false);
                }
            }
        }

        public boolean isAvailable() {
            boolean z;
            synchronized (Tethering.this.mPublicSync) {
                z = this.mAvailable;
            }
            return z;
        }

        private void setAvailable(boolean available) {
            synchronized (Tethering.this.mPublicSync) {
                this.mAvailable = available;
            }
        }

        public boolean isTethered() {
            boolean z;
            synchronized (Tethering.this.mPublicSync) {
                z = this.mTethered;
            }
            return z;
        }

        private void setTethered(boolean tethered) {
            synchronized (Tethering.this.mPublicSync) {
                this.mTethered = tethered;
            }
        }

        public boolean isErrored() {
            boolean z;
            synchronized (Tethering.this.mPublicSync) {
                z = this.mLastError != 0;
            }
            return z;
        }

        class InitialState extends State {
            InitialState() {
            }

            public void enter() {
                TetherInterfaceSM.this.setAvailable(true);
                TetherInterfaceSM.this.setTethered(false);
                Tethering.this.sendTetherStateChangedBroadcast();
            }

            public boolean processMessage(Message message) {
                Log.d(Tethering.TAG, "InitialState.processMessage what=" + message.what);
                switch (message.what) {
                    case 2:
                        TetherInterfaceSM.this.setLastError(0);
                        Tethering.this.mTetherMasterSM.sendMessage(1, TetherInterfaceSM.this);
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mStartingState);
                        break;
                    case 4:
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mUnavailableState);
                        break;
                }
                return true;
            }
        }

        class StartingState extends State {
            StartingState() {
            }

            public void enter() {
                TetherInterfaceSM.this.setAvailable(false);
                if (!TetherInterfaceSM.this.mUsb || Tethering.this.configureUsbIface(true)) {
                    Tethering.this.sendTetherStateChangedBroadcast();
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mTetheredState);
                } else {
                    Tethering.this.mTetherMasterSM.sendMessage(2, TetherInterfaceSM.this);
                    TetherInterfaceSM.this.setLastError(10);
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                }
            }

            public boolean processMessage(Message message) {
                Log.d(Tethering.TAG, "StartingState.processMessage what=" + message.what);
                switch (message.what) {
                    case 3:
                        Tethering.this.mTetherMasterSM.sendMessage(2, TetherInterfaceSM.this);
                        if (!TetherInterfaceSM.this.mUsb || Tethering.this.configureUsbIface(false)) {
                            TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                        } else {
                            TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(10);
                        }
                        break;
                    case 4:
                        Tethering.this.mTetherMasterSM.sendMessage(2, TetherInterfaceSM.this);
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mUnavailableState);
                        break;
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(5);
                        break;
                }
                return true;
            }
        }

        class TetheredState extends State {
            TetheredState() {
            }

            public void enter() {
                try {
                    Tethering.this.mNMService.tetherInterface(TetherInterfaceSM.this.mIfaceName);
                    Log.d(Tethering.TAG, "Tethered " + TetherInterfaceSM.this.mIfaceName);
                    TetherInterfaceSM.this.setAvailable(false);
                    TetherInterfaceSM.this.setTethered(true);
                    Tethering.this.sendTetherStateChangedBroadcast();
                } catch (Exception e) {
                    Log.e(Tethering.TAG, "Error Tethering: " + e.toString());
                    TetherInterfaceSM.this.setLastError(6);
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                }
            }

            private void cleanupUpstream() {
                if (TetherInterfaceSM.this.mMyUpstreamInterfaces != null && !TetherInterfaceSM.this.mMyUpstreamInterfaces.isEmpty()) {
                    try {
                        Tethering.this.mStatsService.forceUpdate();
                    } catch (Exception e) {
                        Log.e(Tethering.TAG, "Exception in forceUpdate: " + e.toString());
                    }
                    try {
                        String upstreamiface = TetherInterfaceSM.this.mMyUpstreamInterfaces.getUpstreamV4IfaceName();
                        if (upstreamiface != null) {
                            Tethering.this.mNMService.disableNat(TetherInterfaceSM.this.mIfaceName, upstreamiface);
                        }
                    } catch (Exception e2) {
                        Log.e(Tethering.TAG, "Exception in disableNat: " + e2.toString());
                    }
                    try {
                        if (TetherInterfaceSM.this.mIfaceName == Tethering.this.mIPv6TetheredInterface) {
                            String upstreamiface2 = TetherInterfaceSM.this.mMyUpstreamInterfaces.getUpstreamV6IfaceName();
                            String address = TetherInterfaceSM.this.mMyUpstreamInterfaces.getUpstreamV6Address();
                            Tethering.this.mNMService.stopIPv6Tethering(TetherInterfaceSM.this.mIfaceName, upstreamiface2, address);
                            Tethering.this.mIPv6TetheredInterface = null;
                        }
                    } catch (Exception e3) {
                        Log.e(Tethering.TAG, "Exception in stopIPv6Tethering: " + e3.toString());
                    }
                    TetherInterfaceSM.this.mMyUpstreamInterfaces = null;
                }
            }

            public boolean processMessage(Message message) {
                Log.d(Tethering.TAG, "TetheredState.processMessage what=" + message.what);
                boolean retValue = true;
                boolean error = false;
                switch (message.what) {
                    case 1:
                        break;
                    case 2:
                    case 5:
                    default:
                        retValue = false;
                        return retValue;
                    case 3:
                    case 4:
                        cleanupUpstream();
                        try {
                            Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                            Tethering.this.mTetherMasterSM.sendMessage(2, TetherInterfaceSM.this);
                            if (message.what == 3) {
                                if (TetherInterfaceSM.this.mUsb && !Tethering.this.configureUsbIface(false)) {
                                    TetherInterfaceSM.this.setLastError(10);
                                }
                                TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                            } else if (message.what == 4) {
                                TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mUnavailableState);
                            }
                            Log.d(Tethering.TAG, "Untethered " + TetherInterfaceSM.this.mIfaceName);
                        } catch (Exception e) {
                            TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(7);
                        }
                        return retValue;
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        error = true;
                        break;
                    case 12:
                        UpstreamInterfaces newUpstreamInterfaces = (UpstreamInterfaces) message.obj;
                        if ((TetherInterfaceSM.this.mMyUpstreamInterfaces == null && newUpstreamInterfaces == null) || (TetherInterfaceSM.this.mMyUpstreamInterfaces != null && TetherInterfaceSM.this.mMyUpstreamInterfaces.equals(newUpstreamInterfaces))) {
                            Log.d(Tethering.TAG, "Connection changed noop - dropping");
                        } else {
                            cleanupUpstream();
                            if (newUpstreamInterfaces != null) {
                                try {
                                    String upstreamV4IfaceName = newUpstreamInterfaces.getUpstreamV4IfaceName();
                                    if (upstreamV4IfaceName != null) {
                                        Tethering.this.mNMService.enableNat(TetherInterfaceSM.this.mIfaceName, upstreamV4IfaceName);
                                    }
                                    try {
                                        String address = newUpstreamInterfaces.getUpstreamV6Address();
                                        String upstreamV6IfaceName = newUpstreamInterfaces.getUpstreamV6IfaceName();
                                        if (address != null && upstreamV6IfaceName != null && Tethering.this.mIPv6TetheredInterface == null) {
                                            Tethering.this.mNMService.startIPv6Tethering(TetherInterfaceSM.this.mIfaceName, upstreamV6IfaceName, address);
                                            Tethering.this.mIPv6TetheredInterface = TetherInterfaceSM.this.mIfaceName;
                                        }
                                    } catch (Exception e2) {
                                        Log.e(Tethering.TAG, "Unable to start IPv6 tethering on " + TetherInterfaceSM.this.mIfaceName + ": " + e2.toString());
                                    }
                                } catch (Exception e3) {
                                    Log.e(Tethering.TAG, "Exception enabling Nat: " + e3.toString());
                                    try {
                                        Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                                        break;
                                    } catch (Exception e4) {
                                    }
                                    TetherInterfaceSM.this.setLastError(8);
                                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                                    return true;
                                }
                            }
                            TetherInterfaceSM.this.mMyUpstreamInterfaces = newUpstreamInterfaces;
                            break;
                        }
                        return retValue;
                }
                cleanupUpstream();
                Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                if (error) {
                    TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(5);
                } else {
                    Log.d(Tethering.TAG, "Tether lost upstream connection " + TetherInterfaceSM.this.mIfaceName);
                    Tethering.this.sendTetherStateChangedBroadcast();
                    if (TetherInterfaceSM.this.mUsb && !Tethering.this.configureUsbIface(false)) {
                        TetherInterfaceSM.this.setLastError(10);
                    }
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                }
                return retValue;
            }
        }

        class UnavailableState extends State {
            UnavailableState() {
            }

            public void enter() {
                TetherInterfaceSM.this.setAvailable(false);
                TetherInterfaceSM.this.setLastError(0);
                TetherInterfaceSM.this.setTethered(false);
                Tethering.this.sendTetherStateChangedBroadcast();
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 5:
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        void setLastErrorAndTransitionToInitialState(int error) {
            setLastError(error);
            transitionTo(this.mInitialState);
        }
    }

    class TetherMasterSM extends StateMachine {
        private static final int BLUETOOTH_TETHERING = 2;
        private static final int CELL_CONNECTION_RENEW_MS = 40000;
        static final int CMD_CELL_CONNECTION_RENEW = 4;
        static final int CMD_RETRY_UPSTREAM = 5;
        static final int CMD_TETHER_MODE_REQUESTED = 1;
        static final int CMD_TETHER_MODE_UNREQUESTED = 2;
        static final int CMD_UPSTREAM_CHANGED = 3;
        private static final String EXTRA_ADD_TETHER_TYPE = "extraAddTetherType";
        private static final String EXTRA_RUN_PROVISION = "extraRunProvision";
        private static final int UPSTREAM_SETTLE_TIME_MS = 10000;
        private static final int USB_TETHERING = 1;
        private static final int WIFI_TETHERING = 0;
        private SimChangeBroadcastReceiver mBroadcastReceiver;
        private int mCurrentConnectionSequence;
        private State mInitialState;
        private int mMobileApnReserved;
        private ArrayList<TetherInterfaceSM> mNotifyList;
        private int mSequenceNumber;
        private State mSetDnsForwardersErrorState;
        private State mSetIpForwardingDisabledErrorState;
        private State mSetIpForwardingEnabledErrorState;
        private final AtomicInteger mSimBcastGenerationNumber;
        private State mStartTetheringErrorState;
        private State mStopTetheringErrorState;
        private State mTetherModeAliveState;
        private UpstreamInterfaces mUpstreamInterfaces;

        static int access$3104(TetherMasterSM x0) {
            int i = x0.mCurrentConnectionSequence + 1;
            x0.mCurrentConnectionSequence = i;
            return i;
        }

        TetherMasterSM(String name, Looper looper) {
            super(name, looper);
            this.mMobileApnReserved = -1;
            this.mUpstreamInterfaces = null;
            this.mSimBcastGenerationNumber = new AtomicInteger(0);
            this.mBroadcastReceiver = null;
            this.mInitialState = new InitialState();
            addState(this.mInitialState);
            this.mTetherModeAliveState = new TetherModeAliveState();
            addState(this.mTetherModeAliveState);
            this.mSetIpForwardingEnabledErrorState = new SetIpForwardingEnabledErrorState();
            addState(this.mSetIpForwardingEnabledErrorState);
            this.mSetIpForwardingDisabledErrorState = new SetIpForwardingDisabledErrorState();
            addState(this.mSetIpForwardingDisabledErrorState);
            this.mStartTetheringErrorState = new StartTetheringErrorState();
            addState(this.mStartTetheringErrorState);
            this.mStopTetheringErrorState = new StopTetheringErrorState();
            addState(this.mStopTetheringErrorState);
            this.mSetDnsForwardersErrorState = new SetDnsForwardersErrorState();
            addState(this.mSetDnsForwardersErrorState);
            this.mNotifyList = new ArrayList<>();
            setInitialState(this.mInitialState);
        }

        class TetherMasterUtilState extends State {
            protected static final boolean TRY_TO_SETUP_MOBILE_CONNECTION = true;
            protected static final boolean WAIT_FOR_NETWORK_TO_SETTLE = false;

            TetherMasterUtilState() {
            }

            public boolean processMessage(Message m) {
                return false;
            }

            protected String enableString(int apnType) {
                switch (apnType) {
                    case 0:
                    case 5:
                        return "enableHIPRI";
                    case 1:
                    case 2:
                    case 3:
                    default:
                        return null;
                    case 4:
                        return "enableDUNAlways";
                }
            }

            protected boolean turnOnUpstreamMobileConnection(int apnType) {
                boolean retValue = TRY_TO_SETUP_MOBILE_CONNECTION;
                if (apnType == -1) {
                    return false;
                }
                if (apnType != TetherMasterSM.this.mMobileApnReserved) {
                    turnOffUpstreamMobileConnection();
                }
                String enableString = enableString(apnType);
                if (enableString == null) {
                    return false;
                }
                int result = Tethering.this.getConnectivityManager().startUsingNetworkFeature(0, enableString);
                switch (result) {
                    case 0:
                    case 1:
                        TetherMasterSM.this.mMobileApnReserved = apnType;
                        Message m = TetherMasterSM.this.obtainMessage(4);
                        m.arg1 = TetherMasterSM.access$3104(TetherMasterSM.this);
                        TetherMasterSM.this.sendMessageDelayed(m, 40000L);
                        break;
                    default:
                        retValue = false;
                        break;
                }
                return retValue;
            }

            protected boolean turnOffUpstreamMobileConnection() {
                TetherMasterSM.access$3104(TetherMasterSM.this);
                if (TetherMasterSM.this.mMobileApnReserved != -1) {
                    Tethering.this.getConnectivityManager().stopUsingNetworkFeature(0, enableString(TetherMasterSM.this.mMobileApnReserved));
                    TetherMasterSM.this.mMobileApnReserved = -1;
                    return TRY_TO_SETUP_MOBILE_CONNECTION;
                }
                return TRY_TO_SETUP_MOBILE_CONNECTION;
            }

            protected boolean turnOnMasterTetherSettings() {
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(TRY_TO_SETUP_MOBILE_CONNECTION);
                    Tethering.this.mNMService.setIPv6ForwardingEnabled(TRY_TO_SETUP_MOBILE_CONNECTION);
                    try {
                        Tethering.this.mNMService.startTethering(Tethering.this.mDhcpRange);
                        return TRY_TO_SETUP_MOBILE_CONNECTION;
                    } catch (Exception e) {
                        try {
                            Tethering.this.mNMService.stopTethering();
                            Tethering.this.mNMService.startTethering(Tethering.this.mDhcpRange);
                            return TRY_TO_SETUP_MOBILE_CONNECTION;
                        } catch (Exception e2) {
                            TetherMasterSM.this.transitionTo(TetherMasterSM.this.mStartTetheringErrorState);
                            return false;
                        }
                    }
                } catch (Exception e3) {
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mSetIpForwardingEnabledErrorState);
                    return false;
                }
            }

            protected boolean turnOffMasterTetherSettings() {
                try {
                    Tethering.this.mNMService.stopTethering();
                    try {
                        Tethering.this.mNMService.setIpForwardingEnabled(false);
                        Tethering.this.mNMService.setIPv6ForwardingEnabled(false);
                        TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
                        return TRY_TO_SETUP_MOBILE_CONNECTION;
                    } catch (Exception e) {
                        TetherMasterSM.this.transitionTo(TetherMasterSM.this.mSetIpForwardingDisabledErrorState);
                        return false;
                    }
                } catch (Exception e2) {
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mStopTetheringErrorState);
                    return false;
                }
            }

            protected void chooseUpstreamType(boolean tryCell) {
                int upType = -1;
                String iface = null;
                Tethering.this.updateConfiguration();
                synchronized (Tethering.this.mPublicSync) {
                    Log.d(Tethering.TAG, "chooseUpstreamType has upstream iface types:");
                    Iterator i$ = Tethering.this.mUpstreamIfaceTypes.iterator();
                    while (i$.hasNext()) {
                        Log.d(Tethering.TAG, " " + ((Integer) i$.next()));
                    }
                    Iterator i$2 = Tethering.this.mUpstreamIfaceTypes.iterator();
                    while (true) {
                        if (!i$2.hasNext()) {
                            break;
                        }
                        Integer netType = (Integer) i$2.next();
                        NetworkInfo info = Tethering.this.getConnectivityManager().getNetworkInfo(netType.intValue());
                        if (info != null && info.isConnected()) {
                            upType = netType.intValue();
                            break;
                        }
                    }
                }
                Log.d(Tethering.TAG, "chooseUpstreamType(" + tryCell + "), preferredApn =" + Tethering.this.mPreferredUpstreamMobileApn + ", got type=" + upType);
                if (upType == 4 || upType == 5) {
                    turnOnUpstreamMobileConnection(upType);
                } else if (upType != -1) {
                    turnOffUpstreamMobileConnection();
                }
                LinkProperties linkProperties = null;
                if (upType != -1) {
                    linkProperties = Tethering.this.getConnectivityManager().getLinkProperties(upType);
                    if (linkProperties != null) {
                        Log.i(Tethering.TAG, "Finding IPv4 upstream interface on: " + linkProperties);
                        RouteInfo ipv4Default = RouteInfo.selectBestRoute(linkProperties.getAllRoutes(), Inet4Address.ANY);
                        if (ipv4Default != null) {
                            iface = ipv4Default.getInterface();
                            Log.i(Tethering.TAG, "Found interface " + ipv4Default.getInterface());
                        } else {
                            Log.i(Tethering.TAG, "No IPv4 upstream interface, giving up.");
                        }
                        Collection<InetAddress> dnses = linkProperties.getDnsServers();
                        if (dnses != null) {
                            String[] dnsServers = Tethering.this.mDefaultDnsServers;
                            if (dnses.size() > 0) {
                                dnsServers = NetworkUtils.makeStrings(dnses);
                            }
                            try {
                                Network network = Tethering.this.getConnectivityManager().getNetworkForType(upType);
                                if (network == null) {
                                    Log.e(Tethering.TAG, "No Network for upstream type " + upType + "!");
                                }
                                Log.d(Tethering.TAG, "Setting DNS forwarders: Network=" + network + ", dnsServers=" + Arrays.toString(dnsServers));
                                Tethering.this.mNMService.setDnsForwarders(network, dnsServers);
                            } catch (Exception e) {
                                TetherMasterSM.this.transitionTo(TetherMasterSM.this.mSetDnsForwardersErrorState);
                            }
                        }
                    }
                } else {
                    boolean tryAgainLater = TRY_TO_SETUP_MOBILE_CONNECTION;
                    if (tryCell && turnOnUpstreamMobileConnection(Tethering.this.mPreferredUpstreamMobileApn)) {
                        tryAgainLater = false;
                    }
                    if (tryAgainLater) {
                        TetherMasterSM.this.sendMessageDelayed(5, 10000L);
                    }
                }
                UpstreamInterfaces uifs = Tethering.this.new UpstreamInterfaces(linkProperties);
                uifs.setUpstreamV4IfaceName(iface);
                notifyTetheredOfNewUpstreamIface(uifs);
            }

            protected void notifyTetheredOfNewUpstreamIface(UpstreamInterfaces newInterfaces) {
                Log.d(Tethering.TAG, "notifying tethered with iface =" + newInterfaces);
                TetherMasterSM.this.mUpstreamInterfaces = newInterfaces;
                for (TetherInterfaceSM sm : TetherMasterSM.this.mNotifyList) {
                    sm.sendMessage(12, newInterfaces);
                }
            }
        }

        private void startListeningForSimChanges() {
            Log.d(Tethering.TAG, "startListeningForSimChanges");
            if (this.mBroadcastReceiver == null) {
                this.mBroadcastReceiver = new SimChangeBroadcastReceiver(this.mSimBcastGenerationNumber.incrementAndGet());
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.SIM_STATE_CHANGED");
                Tethering.this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
            }
        }

        private void stopListeningForSimChanges() {
            Log.d(Tethering.TAG, "stopListeningForSimChanges");
            if (this.mBroadcastReceiver != null) {
                this.mSimBcastGenerationNumber.incrementAndGet();
                Tethering.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
                this.mBroadcastReceiver = null;
            }
        }

        class SimChangeBroadcastReceiver extends BroadcastReceiver {
            private final int mGenerationNumber;
            private boolean mSimNotLoadedSeen = false;

            public SimChangeBroadcastReceiver(int generationNumber) {
                this.mGenerationNumber = generationNumber;
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(Tethering.TAG, "simchange mGenerationNumber=" + this.mGenerationNumber + ", current generationNumber=" + TetherMasterSM.this.mSimBcastGenerationNumber.get());
                if (this.mGenerationNumber == TetherMasterSM.this.mSimBcastGenerationNumber.get()) {
                    String state = intent.getStringExtra("ss");
                    Log.d(Tethering.TAG, "got Sim changed to state " + state + ", mSimNotLoadedSeen=" + this.mSimNotLoadedSeen);
                    if (!this.mSimNotLoadedSeen && !"LOADED".equals(state)) {
                        this.mSimNotLoadedSeen = true;
                    }
                    if (this.mSimNotLoadedSeen && "LOADED".equals(state)) {
                        this.mSimNotLoadedSeen = false;
                        try {
                            if (!Tethering.this.mContext.getResources().getString(R.string.paste_as_plain_text).isEmpty()) {
                                String tetherService = Tethering.this.mContext.getResources().getString(R.string.config_helpPackageNameKey);
                                ArrayList<Integer> tethered = new ArrayList<>();
                                synchronized (Tethering.this.mPublicSync) {
                                    Set ifaces = Tethering.this.mIfaces.keySet();
                                    for (Object iface : ifaces) {
                                        TetherInterfaceSM sm = (TetherInterfaceSM) Tethering.this.mIfaces.get(iface);
                                        if (sm != null && sm.isTethered()) {
                                            if (Tethering.this.isUsb((String) iface)) {
                                                tethered.add(new Integer(1));
                                            } else if (Tethering.this.isWifi((String) iface)) {
                                                tethered.add(new Integer(0));
                                            } else if (Tethering.this.isBluetooth((String) iface)) {
                                                tethered.add(new Integer(2));
                                            }
                                        }
                                    }
                                }
                                Iterator<Integer> it = tethered.iterator();
                                while (it.hasNext()) {
                                    int tetherType = it.next().intValue();
                                    Intent startProvIntent = new Intent();
                                    startProvIntent.putExtra(TetherMasterSM.EXTRA_ADD_TETHER_TYPE, tetherType);
                                    startProvIntent.putExtra(TetherMasterSM.EXTRA_RUN_PROVISION, true);
                                    startProvIntent.setComponent(ComponentName.unflattenFromString(tetherService));
                                    Tethering.this.mContext.startServiceAsUser(startProvIntent, UserHandle.CURRENT);
                                }
                                Log.d(Tethering.TAG, "re-evaluate provisioning");
                                return;
                            }
                            Log.d(Tethering.TAG, "no prov-check needed for new SIM");
                        } catch (Resources.NotFoundException e) {
                            Log.d(Tethering.TAG, "no prov-check needed for new SIM");
                        }
                    }
                }
            }
        }

        class InitialState extends TetherMasterUtilState {
            InitialState() {
                super();
            }

            public void enter() {
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(Tethering.TAG, "MasterInitialState.processMessage what=" + message.what);
                switch (message.what) {
                    case 1:
                        TetherInterfaceSM who = (TetherInterfaceSM) message.obj;
                        Log.d(Tethering.TAG, "Tether Mode requested by " + who);
                        TetherMasterSM.this.mNotifyList.add(who);
                        TetherMasterSM.this.transitionTo(TetherMasterSM.this.mTetherModeAliveState);
                        break;
                    case 2:
                        TetherInterfaceSM who2 = (TetherInterfaceSM) message.obj;
                        Log.d(Tethering.TAG, "Tether Mode unrequested by " + who2);
                        int index = TetherMasterSM.this.mNotifyList.indexOf(who2);
                        if (index != -1) {
                            TetherMasterSM.this.mNotifyList.remove(who2);
                        }
                        break;
                }
                return true;
            }
        }

        class TetherModeAliveState extends TetherMasterUtilState {
            boolean mTryCell;

            TetherModeAliveState() {
                super();
                this.mTryCell = true;
            }

            public void enter() {
                turnOnMasterTetherSettings();
                TetherMasterSM.this.startListeningForSimChanges();
                this.mTryCell = true;
                chooseUpstreamType(this.mTryCell);
                this.mTryCell = this.mTryCell ? false : true;
            }

            public void exit() {
                turnOffUpstreamMobileConnection();
                TetherMasterSM.this.stopListeningForSimChanges();
                notifyTetheredOfNewUpstreamIface(null);
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(Tethering.TAG, "TetherModeAliveState.processMessage what=" + message.what);
                switch (message.what) {
                    case 1:
                        TetherInterfaceSM who = (TetherInterfaceSM) message.obj;
                        Log.d(Tethering.TAG, "Tether Mode requested by " + who);
                        TetherMasterSM.this.mNotifyList.add(who);
                        who.sendMessage(12, TetherMasterSM.this.mUpstreamInterfaces);
                        break;
                    case 2:
                        TetherInterfaceSM who2 = (TetherInterfaceSM) message.obj;
                        Log.d(Tethering.TAG, "Tether Mode unrequested by " + who2);
                        int index = TetherMasterSM.this.mNotifyList.indexOf(who2);
                        if (index != -1) {
                            Log.d(Tethering.TAG, "TetherModeAlive removing notifyee " + who2);
                            TetherMasterSM.this.mNotifyList.remove(index);
                            if (TetherMasterSM.this.mNotifyList.isEmpty()) {
                                turnOffMasterTetherSettings();
                            } else {
                                Log.d(Tethering.TAG, "TetherModeAlive still has " + TetherMasterSM.this.mNotifyList.size() + " live requests:");
                                for (Object o : TetherMasterSM.this.mNotifyList) {
                                    Log.d(Tethering.TAG, "  " + o);
                                }
                            }
                        } else {
                            Log.e(Tethering.TAG, "TetherModeAliveState UNREQUESTED has unknown who: " + who2);
                        }
                        break;
                    case 3:
                        this.mTryCell = true;
                        chooseUpstreamType(this.mTryCell);
                        this.mTryCell = this.mTryCell ? false : true;
                        break;
                    case 4:
                        if (TetherMasterSM.this.mCurrentConnectionSequence == message.arg1) {
                            Log.d(Tethering.TAG, "renewing mobile connection - requeuing for another 40000ms");
                            turnOnUpstreamMobileConnection(TetherMasterSM.this.mMobileApnReserved);
                        }
                        break;
                    case 5:
                        chooseUpstreamType(this.mTryCell);
                        this.mTryCell = this.mTryCell ? false : true;
                        break;
                }
                return true;
            }
        }

        class ErrorState extends State {
            int mErrorNotification;

            ErrorState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 1:
                        TetherInterfaceSM who = (TetherInterfaceSM) message.obj;
                        who.sendMessage(this.mErrorNotification);
                        return true;
                    default:
                        return false;
                }
            }

            void notify(int msgType) {
                this.mErrorNotification = msgType;
                for (Object o : TetherMasterSM.this.mNotifyList) {
                    TetherInterfaceSM sm = (TetherInterfaceSM) o;
                    sm.sendMessage(msgType);
                }
            }
        }

        class SetIpForwardingEnabledErrorState extends ErrorState {
            SetIpForwardingEnabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setIpForwardingEnabled");
                notify(7);
            }
        }

        class SetIpForwardingDisabledErrorState extends ErrorState {
            SetIpForwardingDisabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setIpForwardingDisabled");
                notify(8);
            }
        }

        class StartTetheringErrorState extends ErrorState {
            StartTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in startTethering");
                notify(9);
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                    Tethering.this.mNMService.setIPv6ForwardingEnabled(false);
                } catch (Exception e) {
                }
            }
        }

        class StopTetheringErrorState extends ErrorState {
            StopTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in stopTethering");
                notify(10);
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                    Tethering.this.mNMService.setIPv6ForwardingEnabled(false);
                } catch (Exception e) {
                }
            }
        }

        class SetDnsForwardersErrorState extends ErrorState {
            SetDnsForwardersErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setDnsForwarders");
                notify(11);
                try {
                    Tethering.this.mNMService.stopTethering();
                } catch (Exception e) {
                }
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                    Tethering.this.mNMService.setIPv6ForwardingEnabled(false);
                } catch (Exception e2) {
                }
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ConnectivityService.Tether from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mPublicSync) {
            pw.println("mUpstreamIfaceTypes: ");
            for (Integer netType : this.mUpstreamIfaceTypes) {
                pw.println(" " + netType);
            }
            pw.println();
            pw.println("Tether state:");
            for (Object o : this.mIfaces.values()) {
                pw.println(" " + o);
            }
        }
        pw.println();
    }
}
