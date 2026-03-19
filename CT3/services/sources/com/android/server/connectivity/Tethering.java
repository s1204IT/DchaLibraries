package com.android.server.connectivity;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.INetworkStatsService;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.IoThread;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.BaseNetworkObserver;
import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Tethering extends BaseNetworkObserver {
    public static final String ACTION_ENABLE_NSIOT_TESTING = "android.intent.action.ACTION_ENABLE_NSIOT_TESTING";
    private static final int BR_PREFIX_LENGTH = 24;
    private static final String BR_SUB_IFACE_ADDR = "0.0.0.0";
    private static final boolean DBG;
    private static final String[] DHCP_DEFAULT_RANGE;
    private static final String DNS_DEFAULT_SERVER1 = "8.8.8.8";
    private static final String DNS_DEFAULT_SERVER2 = "8.8.4.4";
    private static final Integer DUN_TYPE;
    public static final String EXTRA_NSIOT_ENABLED = "nsiot_enabled";
    public static final String EXTRA_NSIOT_IP_ADDR = "nsiot_ip_addr";
    private static final Integer HIPRI_TYPE;
    private static final boolean IS_USER_BUILD;
    private static final String MASTERSM_IPV4 = "TetherMaster";
    private static final String MASTERSM_IPV6 = "Ipv6TetherMaster";
    private static final String MD_DIRECT_TETHERING_IFACE_BR_SUB1 = "rndis0";
    private static final String MD_DIRECT_TETHERING_IFACE_BR_SUB2 = "ccmni-lan";
    private static final Integer MOBILE_TYPE;
    public static final String SYSTEM_PROPERTY_MD_DRT_BRIDGE_NAME = "ro.tethering.bridge.interface";
    public static final String SYSTEM_PROPERTY_MD_DRT_TETHER_ENABLE = "sys.mtk_md_direct_tether_enable";
    public static final String SYSTEM_PROPERTY_MD_DRT_TETHER_SUPPORT = "ro.mtk_md_direct_tethering";
    public static final String SYSTEM_PROPERTY_MD_DRT_USB_MODE_CHANG = "sys.usb.rndis.direct";
    public static final String SYSTEM_PROPERTY_NSIOT_PENDING = "net.nsiot_pending";
    private static final String TAG = "Tethering";
    private static final ComponentName TETHER_SERVICE;
    private static final String USB_NEAR_IFACE_ADDR = "192.168.42.129";
    private static final int USB_PREFIX_LENGTH = 24;
    private static final boolean VDBG;
    private static final Class[] messageClasses;
    private static final SparseArray<String> sMagicDecoderRing;
    private boolean mBspPackage;
    private final Context mContext;
    private String mCurrentUpstreamIface;
    private String[] mDefaultDnsServers;
    private String[] mDhcpRange;
    private boolean mIpv6FeatureEnable;
    private StateMachine mIpv6TetherMasterSM;
    private boolean mIpv6TetherPdModeSupport;
    private int mLastNotificationId;
    private boolean mMdDirectTetheringFeatureEnable;
    private boolean mMdDirectTetheringSupport;
    private boolean mMtkTetheringEemSupport;
    private final INetworkManagementService mNMService;
    private Object mNotificationSync;
    private boolean mRndisEnabled;
    private BroadcastReceiver mStateReceiver;
    private final INetworkStatsService mStatsService;
    private String[] mTetherableBluetoothRegexs;
    private String[] mTetherableMdDirectUsbRegexs;
    private String[] mTetherableUsbRegexs;
    private String[] mTetherableWifiRegexs;
    private Notification.Builder mTetheredNotificationBuilder;
    private boolean mTetheringIpv6Support;
    private Collection<Integer> mUpstreamIfaceTypes;
    private final UpstreamNetworkMonitor mUpstreamNetworkMonitor;
    private boolean mUsbTetherRequested;
    private String mWifiIface;
    private int mPreferredUpstreamMobileApn = -1;
    private boolean mUnTetherDone = true;
    private boolean mTetherDone = true;
    private boolean mTetheredFail = false;
    private boolean mUsbTetherEnabled = false;
    private boolean mIsTetheringChangeDone = true;
    private int mUpstreamIpv4UpType = -1;
    private int mUpstreamIpv6UpType = -1;
    private final Object mPublicSync = new Object();
    private HashMap<String, TetherInterfaceSM> mIfaces = new HashMap<>();
    private final Looper mLooper = IoThread.get().getLooper();
    private final StateMachine mTetherMasterSM = new TetherMasterSM(MASTERSM_IPV4, this.mLooper);

    static {
        IS_USER_BUILD = !"user".equals(Build.TYPE) ? "userdebug".equals(Build.TYPE) : true;
        DBG = IS_USER_BUILD ? Log.isLoggable(TAG, 3) : true;
        VDBG = IS_USER_BUILD ? Log.isLoggable(TAG, 3) : true;
        messageClasses = new Class[]{Tethering.class, TetherMasterSM.class, TetherInterfaceSM.class};
        sMagicDecoderRing = MessageUtils.findMessageNames(messageClasses);
        MOBILE_TYPE = new Integer(0);
        HIPRI_TYPE = new Integer(5);
        DUN_TYPE = new Integer(4);
        TETHER_SERVICE = ComponentName.unflattenFromString(Resources.getSystem().getString(R.string.config_systemTextIntelligence));
        DHCP_DEFAULT_RANGE = new String[]{"192.168.42.2", "192.168.42.254", "192.168.43.2", "192.168.43.254", "192.168.44.2", "192.168.44.254", "192.168.45.2", "192.168.45.254", "192.168.46.2", "192.168.46.254", "192.168.47.2", "192.168.47.254", "192.168.48.2", "192.168.48.254", "192.168.49.2", "192.168.49.254"};
    }

    public Tethering(Context context, INetworkManagementService nmService, INetworkStatsService statsService) {
        this.mContext = context;
        this.mNMService = nmService;
        this.mStatsService = statsService;
        this.mTetherMasterSM.start();
        this.mUpstreamNetworkMonitor = new UpstreamNetworkMonitor();
        this.mBspPackage = SystemProperties.getBoolean("ro.mtk_bsp_package", false);
        Log.d(TAG, "mBspPackage: " + this.mBspPackage);
        this.mMtkTetheringEemSupport = SystemProperties.getBoolean("ro.mtk_tethering_eem_support", false);
        Log.d(TAG, "mMtkTetheringEemSupport: " + this.mMtkTetheringEemSupport);
        this.mTetheringIpv6Support = SystemProperties.getBoolean("ro.mtk_tetheringipv6_support", false);
        Log.d(TAG, "mTetheringIpv6Support: " + this.mTetheringIpv6Support);
        this.mIpv6TetherPdModeSupport = SystemProperties.getBoolean("ro.mtk_ipv6_tether_pd_mode", false);
        Log.d(TAG, "mIpv6TetherPdModeSupport: " + this.mIpv6TetherPdModeSupport);
        this.mMdDirectTetheringSupport = SystemProperties.getBoolean(SYSTEM_PROPERTY_MD_DRT_TETHER_SUPPORT, false);
        Log.d(TAG, "mMdDirectTetheringSupport: " + this.mMdDirectTetheringSupport);
        this.mTetherableMdDirectUsbRegexs = new String[1];
        this.mTetherableMdDirectUsbRegexs[0] = SystemProperties.get(SYSTEM_PROPERTY_MD_DRT_BRIDGE_NAME, "mdbr0");
        Log.d(TAG, "mTetherableMdDirectUsbRegexs:" + Arrays.toString(this.mTetherableMdDirectUsbRegexs));
        if (isTetheringIpv6Support()) {
            this.mIpv6TetherMasterSM = new TetherMasterSM(MASTERSM_IPV6, this.mLooper);
            this.mIpv6TetherMasterSM.start();
        }
        this.mStateReceiver = new StateReceiver(this, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction(ACTION_ENABLE_NSIOT_TESTING);
        this.mContext.registerReceiver(this.mStateReceiver, filter);
        this.mNotificationSync = new Object();
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.MEDIA_SHARED");
        filter2.addAction("android.intent.action.MEDIA_UNSHARED");
        filter2.addDataScheme("file");
        this.mContext.registerReceiver(this.mStateReceiver, filter2);
        this.mDhcpRange = context.getResources().getStringArray(R.array.config_autoBrightnessLevels);
        if (this.mDhcpRange.length == 0 || this.mDhcpRange.length % 2 == 1) {
            this.mDhcpRange = DHCP_DEFAULT_RANGE;
        }
        updateConfiguration();
        this.mDefaultDnsServers = new String[2];
        this.mDefaultDnsServers[0] = DNS_DEFAULT_SERVER1;
        this.mDefaultDnsServers[1] = DNS_DEFAULT_SERVER2;
        this.mWifiIface = SystemProperties.get("wifi.interface", "wlan0");
        SystemProperties.set(SYSTEM_PROPERTY_NSIOT_PENDING, "false");
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    void updateConfiguration() {
        String[] tetherableUsbRegexs = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessDisplayValuesNits);
        String[] tetherableWifiRegexs = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessDisplayValuesNitsIdle);
        String[] tetherableBluetoothRegexs = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessLcdBacklightValues_doze);
        int[] ifaceTypes = null;
        try {
            TelephonyManager mTelephonyManager = TelephonyManager.getDefault();
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            String sMccMnc = null;
            if (DBG) {
                Log.d(TAG, "updateConfiguration() : mTetheringIpv6Support:" + this.mTetheringIpv6Support);
            }
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                sMccMnc = mTelephonyManager.getSimOperator(subId);
            }
            if (sMccMnc == null || sMccMnc.length() < 5) {
                Log.e(TAG, "updateConfiguration: wrong MCCMNC =" + sMccMnc);
            } else {
                if (DBG) {
                    Log.i(TAG, "updateConfiguration: MCCMNC =" + sMccMnc);
                }
                String sMcc = sMccMnc.substring(0, 3);
                String sMnc = sMccMnc.substring(3, sMccMnc.length());
                int mcc = Integer.parseInt(sMcc);
                int mnc = Integer.parseInt(sMnc);
                Resources res = getResourcesUsingMccMnc(this.mContext, mcc, mnc);
                if (res != null) {
                    ifaceTypes = res.getIntArray(R.array.config_autoKeyboardBacklightBrightnessValues);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ifaceTypes == null) {
            Log.i(TAG, "ifaceTypes = null, use default");
            ifaceTypes = this.mContext.getResources().getIntArray(R.array.config_autoKeyboardBacklightBrightnessValues);
        }
        Collection<Integer> upstreamIfaceTypes = new ArrayList<>();
        for (int i : ifaceTypes) {
            upstreamIfaceTypes.add(new Integer(i));
        }
        if (VDBG) {
            synchronized (this.mPublicSync) {
                String result = "";
                Iterator<Integer> iterator = upstreamIfaceTypes.iterator();
                for (int i2 = 0; i2 < upstreamIfaceTypes.size(); i2++) {
                    result = result + " " + iterator.next();
                }
                Log.i(TAG, "upstreamIfaceTypes.add: " + result);
            }
        }
        synchronized (this.mPublicSync) {
            if (isMdDirectTetheringEnable()) {
                this.mTetherableUsbRegexs = this.mTetherableMdDirectUsbRegexs;
            } else {
                this.mTetherableUsbRegexs = tetherableUsbRegexs;
            }
            if (VDBG) {
                Log.i(TAG, "mTetherableUsbRegexs:" + Arrays.toString(this.mTetherableUsbRegexs));
            }
            this.mTetherableWifiRegexs = tetherableWifiRegexs;
            this.mTetherableBluetoothRegexs = tetherableBluetoothRegexs;
            this.mUpstreamIfaceTypes = upstreamIfaceTypes;
        }
        checkDunRequired();
        if (isTetheringIpv6Support()) {
            this.mIpv6FeatureEnable = readIpv6FeatureEnable();
        }
        if (!isMdDirectTetheringSupport()) {
            return;
        }
        this.mMdDirectTetheringFeatureEnable = readMdDirectFeatureEnable();
    }

    public void interfaceStatusChanged(String iface, boolean up) {
        if (VDBG) {
            Log.d(TAG, "interfaceStatusChanged " + iface + ", " + up);
        }
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
                } else if (isUsb(iface) || isBluetooth(iface)) {
                    Log.d(TAG, "ignore interface down for " + iface);
                } else if (sm != null) {
                    Log.d(TAG, "interfaceLinkStatusChanged, sm!=null, sendMessage:CMD_INTERFACE_DOWN");
                    sm.sendMessage(327784);
                    this.mIfaces.remove(iface);
                }
            }
        }
    }

    public void interfaceLinkStateChanged(String iface, boolean up) {
        if (VDBG) {
            Log.d(TAG, "interfaceLinkStateChanged " + iface + ", " + up);
        }
        interfaceStatusChanged(iface, up);
    }

    private boolean isUsb(String iface) {
        synchronized (this.mPublicSync) {
            if (iface == null) {
                Log.e(TAG, "iface == null, isUsb() return false;");
                return false;
            }
            for (String regex : this.mTetherableUsbRegexs) {
                if (iface.matches(regex)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isWifi(String iface) {
        synchronized (this.mPublicSync) {
            if (iface == null) {
                Log.e(TAG, "iface == null, isWifi() return false;");
                return false;
            }
            for (String regex : this.mTetherableWifiRegexs) {
                if (iface.matches(regex)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isBluetooth(String iface) {
        synchronized (this.mPublicSync) {
            if (iface == null) {
                Log.e(TAG, "iface == null, isBluetooth() return false;");
                return false;
            }
            for (String regex : this.mTetherableBluetoothRegexs) {
                if (iface.matches(regex)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void interfaceAdded(String iface) {
        if (VDBG) {
            Log.d(TAG, "interfaceAdded " + iface);
        }
        boolean found = false;
        boolean usb = false;
        synchronized (this.mPublicSync) {
            if (isWifi(iface)) {
                found = true;
            }
            if (isUsb(iface)) {
                found = true;
                usb = true;
            }
            if (isBluetooth(iface)) {
                found = true;
            }
            if (!found) {
                if (VDBG) {
                    Log.d(TAG, iface + " is not a tetherable iface, ignoring");
                }
            } else if (this.mIfaces.get(iface) != null) {
                if (VDBG) {
                    Log.d(TAG, "active iface (" + iface + ") reported as added, ignoring");
                }
            } else {
                TetherInterfaceSM sm = new TetherInterfaceSM(iface, this.mLooper, usb);
                this.mIfaces.put(iface, sm);
                sm.start();
                Log.d(TAG, "interfaceAdded :" + iface);
            }
        }
    }

    public void interfaceRemoved(String iface) {
        if (VDBG) {
            Log.d(TAG, "interfaceRemoved " + iface);
        }
        synchronized (this.mPublicSync) {
            TetherInterfaceSM sm = this.mIfaces.get(iface);
            if (sm == null) {
                if (VDBG) {
                    Log.e(TAG, "attempting to remove unknown iface (" + iface + "), ignoring");
                }
            } else {
                Log.d(TAG, "interfaceRemoved, iface=" + iface + ", sendMessage:CMD_INTERFACE_DOWN");
                sm.sendMessage(327784);
                this.mIfaces.remove(iface);
            }
        }
    }

    public void startTethering(int type, ResultReceiver receiver, boolean showProvisioningUi) {
        Log.d(TAG, "startTethering:" + type);
        if (!isTetherProvisioningRequired()) {
            Log.d(TAG, "Not TetherProvisioningRequired");
            enableTetheringInternal(type, true, receiver);
            return;
        }
        Log.d(TAG, "TetherProvisioningRequired");
        if (showProvisioningUi) {
            runUiTetherProvisioningAndEnable(type, receiver);
        } else {
            runSilentTetherProvisioningAndEnable(type, receiver);
        }
    }

    public void stopTethering(int type) {
        Log.d(TAG, "stopTethering:" + type);
        enableTetheringInternal(type, false, null);
        if (!isTetherProvisioningRequired()) {
            return;
        }
        cancelTetherProvisioningRechecks(type);
    }

    public void addressUpdated(String iface, LinkAddress address) {
        if (VDBG) {
            Log.i(TAG, "addressUpdated " + iface + ", " + address);
        }
        if (!(address.getAddress() instanceof Inet6Address) || !address.isGlobalPreferred() || !isIpv6MasterSmOn()) {
            return;
        }
        this.mIpv6TetherMasterSM.sendMessage(327683);
    }

    public void addressRemoved(String iface, LinkAddress address) {
        if (VDBG) {
            Log.i(TAG, "addressRemoved " + iface + ", " + address);
        }
        if (!(address.getAddress() instanceof Inet6Address) || !address.isGlobalPreferred() || !isIpv6MasterSmOn()) {
            return;
        }
        this.mIpv6TetherMasterSM.sendMessage(327683);
    }

    private boolean isTetherProvisioningRequired() {
        CarrierConfigManager configManager;
        String[] provisionApp = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessLevelsIdle);
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false) || provisionApp == null || (configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config")) == null || configManager.getConfig() == null) {
            return false;
        }
        boolean isEntitlementCheckRequired = configManager.getConfig().getBoolean("require_entitlement_checks_bool");
        return isEntitlementCheckRequired && provisionApp.length == 2;
    }

    private void enableTetheringInternal(int type, boolean enable, ResultReceiver receiver) {
        boolean isProvisioningRequired = isTetherProvisioningRequired();
        Log.d(TAG, "enableTetheringInternal type:" + type + ", enable:" + enable);
        switch (type) {
            case 0:
                WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                if (wifiManager.setWifiApEnabled(null, enable)) {
                    sendTetherResult(receiver, 0);
                    if (enable && isProvisioningRequired) {
                        scheduleProvisioningRechecks(type);
                        break;
                    }
                } else {
                    sendTetherResult(receiver, 5);
                    break;
                }
                break;
            case 1:
                int result = setUsbTethering(enable);
                if (enable && isProvisioningRequired && result == 0) {
                    scheduleProvisioningRechecks(type);
                }
                sendTetherResult(receiver, result);
                break;
            case 2:
                setBluetoothTethering(enable, receiver);
                break;
            default:
                Log.w(TAG, "Invalid tether type.");
                sendTetherResult(receiver, 1);
                break;
        }
    }

    private void sendTetherResult(ResultReceiver receiver, int result) {
        if (receiver == null) {
            return;
        }
        receiver.send(result, null);
    }

    private void setBluetoothTethering(final boolean enable, final ResultReceiver receiver) {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.w(TAG, "Tried to enable bluetooth tethering with null or disabled adapter. null: " + (adapter == null));
            sendTetherResult(receiver, 2);
        } else {
            adapter.getProfileProxy(this.mContext, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceDisconnected(int profile) {
                }

                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    int result;
                    ((BluetoothPan) proxy).setBluetoothTethering(enable);
                    if (((BluetoothPan) proxy).isTetheringOn() == enable) {
                        result = 0;
                    } else {
                        result = 5;
                    }
                    Tethering.this.sendTetherResult(receiver, result);
                    if (enable && Tethering.this.isTetherProvisioningRequired()) {
                        Tethering.this.scheduleProvisioningRechecks(2);
                    }
                    adapter.closeProfileProxy(5, proxy);
                }
            }, 5);
        }
    }

    private void runUiTetherProvisioningAndEnable(int type, ResultReceiver receiver) {
        ResultReceiver proxyReceiver = getProxyReceiver(type, receiver);
        sendUiTetherProvisionIntent(type, proxyReceiver);
    }

    private void sendUiTetherProvisionIntent(int type, ResultReceiver receiver) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.TETHER_PROVISIONING_UI");
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraProvisionCallback", receiver);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private ResultReceiver getProxyReceiver(final int type, final ResultReceiver receiver) {
        ResultReceiver rr = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) {
                    Tethering.this.enableTetheringInternal(type, true, receiver);
                } else {
                    Tethering.this.sendTetherResult(receiver, resultCode);
                }
            }
        };
        Parcel parcel = Parcel.obtain();
        rr.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }

    private void scheduleProvisioningRechecks(int type) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraSetAlarm", true);
        intent.setComponent(TETHER_SERVICE);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void runSilentTetherProvisioningAndEnable(int type, ResultReceiver receiver) {
        ResultReceiver proxyReceiver = getProxyReceiver(type, receiver);
        sendSilentTetherProvisionIntent(type, proxyReceiver);
    }

    private void sendSilentTetherProvisionIntent(int type, ResultReceiver receiver) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraRunProvision", true);
        intent.putExtra("extraProvisionCallback", receiver);
        intent.setComponent(TETHER_SERVICE);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void cancelTetherProvisioningRechecks(int type) {
        if (!getConnectivityManager().isTetheringSupported()) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra("extraRemTetherType", type);
        intent.setComponent(TETHER_SERVICE);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int tether(String iface) {
        TetherInterfaceSM sm;
        if (DBG) {
            Log.d(TAG, "Tethering " + iface);
        }
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
        sm.sendMessage(327782);
        return 0;
    }

    public int untether(String iface) {
        TetherInterfaceSM sm;
        if (DBG) {
            Log.d(TAG, "Untethering " + iface);
        }
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
        sm.sendMessage(327783);
        return 0;
    }

    public void untetherAll() {
        if (DBG) {
            Log.d(TAG, "Untethering " + this.mIfaces);
        }
        for (String iface : this.mIfaces.keySet()) {
            untether(iface);
        }
    }

    public int getLastTetherError(String iface) {
        synchronized (this.mPublicSync) {
            TetherInterfaceSM sm = this.mIfaces.get(iface);
            if (sm == null) {
                Log.e(TAG, "Tried to getLastTetherError on an unknown iface :" + iface + ", ignoring");
                return 1;
            }
            int error = sm.getLastError();
            if (isBspPackage() && isTetheringIpv6Support() && (error & 240) == 16) {
                return error & 15;
            }
            return error;
        }
    }

    private void sendTetherStateChangedBroadcast() {
        if (getConnectivityManager().isTetheringSupported()) {
            if (DBG) {
                Log.d(TAG, "sendTetherStateChangedBroadcast");
            }
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
                            Log.d(TAG, "add err");
                            erroredList.add((String) iface);
                        } else if (sm.isAvailable()) {
                            if (DBG) {
                                Log.d(TAG, "add avai");
                            }
                            availableList.add((String) iface);
                        } else if (sm.isTethered()) {
                            if (isUsb((String) iface)) {
                                Log.d(TAG, "usb isTethered");
                                usbTethered = true;
                            } else if (isWifi((String) iface)) {
                                Log.d(TAG, "wifi isTethered");
                                wifiTethered = true;
                            } else if (isBluetooth((String) iface)) {
                                Log.d(TAG, "bt isTethered");
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
            broadcast.putExtra("UnTetherDone", this.mUnTetherDone);
            broadcast.putExtra("TetherDone", this.mTetherDone);
            broadcast.putExtra("TetherFail", this.mTetheredFail);
            this.mContext.sendStickyBroadcastAsUser(broadcast, UserHandle.ALL);
            if (DBG) {
                Log.d(TAG, String.format("sendTetherStateChangedBroadcast avail=[%s] active=[%s] error=[%s]", TextUtils.join(",", availableList), TextUtils.join(",", activeList), TextUtils.join(",", erroredList)));
            }
            if (usbTethered) {
                if (wifiTethered || bluetoothTethered) {
                    showTetheredNotification(R.drawable.notification_icon_circle);
                    return;
                } else {
                    showTetheredNotification(R.drawable.notification_large_icon_outline);
                    return;
                }
            }
            if (wifiTethered) {
                if (bluetoothTethered) {
                    showTetheredNotification(R.drawable.notification_icon_circle);
                    return;
                } else {
                    clearTetheredNotification();
                    return;
                }
            }
            if (bluetoothTethered) {
                showTetheredNotification(R.drawable.notification_close_button_icon);
            } else {
                clearTetheredNotification();
            }
        }
    }

    private void showTetheredNotification(int icon) {
        Log.i(TAG, "showTetheredNotification icon:" + icon);
        synchronized (this.mNotificationSync) {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            if (notificationManager == null) {
                return;
            }
            if (this.mLastNotificationId != 0) {
                if (this.mLastNotificationId == icon) {
                    return;
                }
                notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
                this.mLastNotificationId = 0;
            }
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            intent.setFlags(1073741824);
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            if (BenesseExtension.getDchaState() != 0) {
                pi = null;
            }
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(R.string.global_action_restart);
            CharSequence message = r.getText(R.string.global_action_screenshot);
            if (this.mTetheredNotificationBuilder == null) {
                this.mTetheredNotificationBuilder = new Notification.Builder(this.mContext);
                this.mTetheredNotificationBuilder.setWhen(0L).setOngoing(true).setColor(this.mContext.getColor(R.color.system_accent3_600)).setVisibility(1).setCategory("status");
            }
            this.mTetheredNotificationBuilder.setSmallIcon(icon).setContentTitle(title).setContentText(message).setContentIntent(pi);
            this.mLastNotificationId = icon;
            notificationManager.notifyAsUser(null, this.mLastNotificationId, this.mTetheredNotificationBuilder.build(), UserHandle.ALL);
        }
    }

    private void clearTetheredNotification() {
        Log.i(TAG, "clearTetheredNotification");
        synchronized (this.mNotificationSync) {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            if (notificationManager != null && this.mLastNotificationId != 0) {
                notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
                this.mLastNotificationId = 0;
            }
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        StateReceiver(Tethering this$0, StateReceiver stateReceiver) {
            this();
        }

        private StateReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.i(Tethering.TAG, "StateReceiver onReceive action:" + action);
            if (action == null) {
                return;
            }
            if (action.equals("android.hardware.usb.action.USB_STATE")) {
                synchronized (Tethering.this.mPublicSync) {
                    boolean usbConfigured = intent.getBooleanExtra("configured", false);
                    boolean usbConnected = intent.getBooleanExtra("connected", false);
                    boolean oriRndisEnabled = Tethering.this.mRndisEnabled;
                    Tethering.this.mRndisEnabled = !intent.getBooleanExtra("rndis", false) ? intent.getBooleanExtra("eem", false) : true;
                    Log.i(Tethering.TAG, "StateReceiver onReceive action synchronized: usbConnected = " + usbConnected + " usbConfigured = " + usbConfigured + ", mRndisEnabled = " + Tethering.this.mRndisEnabled + ", mUsbTetherRequested = " + Tethering.this.mUsbTetherRequested);
                    Log.i(Tethering.TAG, "StateReceiver onReceive action synchronized: mUsbTetherEnabled = " + Tethering.this.mUsbTetherEnabled);
                    if (!Tethering.this.mUsbTetherEnabled && Tethering.this.mRndisEnabled && Tethering.this.mRndisEnabled != oriRndisEnabled) {
                        Log.i(Tethering.TAG, "StateReceiver onReceive action synchronized: mUsbTetherEnabled = " + Tethering.this.mUsbTetherEnabled + ", mRndisEnabled = " + Tethering.this.mRndisEnabled + ", oriRndisEnabled = " + oriRndisEnabled);
                        Tethering.this.tetherUsb(false);
                        UsbManager usbManager = (UsbManager) Tethering.this.mContext.getSystemService("usb");
                        usbManager.setCurrentFunction(null);
                        Tethering.this.mUsbTetherRequested = false;
                    }
                    if (usbConnected && Tethering.this.mRndisEnabled && Tethering.this.mUsbTetherRequested && usbConfigured) {
                        Log.i(Tethering.TAG, "StateReceiver onReceive action synchronized: usbConnected && mRndisEnabled && mUsbTetherRequested, tetherUsb!! ");
                        Tethering.this.tetherUsb(true);
                        Tethering.this.mUsbTetherRequested = false;
                    }
                }
                return;
            }
            if (!action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                if (action.equals(Tethering.ACTION_ENABLE_NSIOT_TESTING)) {
                    boolean enabled = intent.getBooleanExtra(Tethering.EXTRA_NSIOT_ENABLED, false);
                    String ipAddr = intent.getStringExtra(Tethering.EXTRA_NSIOT_IP_ADDR);
                    Log.e(Tethering.TAG, "[NS-IOT]Receieve ACTION_ENABLE_NSIOT_TESTING:nsiot_enabled = " + enabled + "," + Tethering.EXTRA_NSIOT_IP_ADDR + " = " + ipAddr);
                    SystemProperties.set(Tethering.SYSTEM_PROPERTY_NSIOT_PENDING, "true");
                    Tethering.this.enableUdpForwardingForUsb(enabled, ipAddr);
                    return;
                }
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            if (Tethering.VDBG) {
                Log.i(Tethering.TAG, "Tethering got CONNECTIVITY_ACTION, networkInfo:" + networkInfo);
            }
            if (networkInfo == null || networkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
                if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    Tethering.this.updateConfiguration();
                    return;
                }
                return;
            }
            if (Tethering.VDBG) {
                Log.d(Tethering.TAG, "Tethering got CONNECTIVITY_ACTION");
            }
            Tethering.this.mTetherMasterSM.sendMessage(327683);
            if (Tethering.this.isIpv6MasterSmOn()) {
                if (Tethering.VDBG) {
                    Log.d(Tethering.TAG, "mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED)");
                }
                Tethering.this.mIpv6TetherMasterSM.sendMessage(327683);
            }
        }
    }

    private void tetherUsb(boolean enable) {
        if (VDBG) {
            Log.d(TAG, "tetherUsb " + enable);
        }
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
            this.mTetheredFail = true;
            SystemClock.sleep(500L);
            sendTetherStateChangedBroadcast();
            Log.e(TAG, "unable start or stop USB tethering");
        } catch (Exception e) {
            Log.e(TAG, "Error listing Interfaces", e);
        }
    }

    private boolean configureUsbIface(boolean enabled) {
        if (VDBG) {
            Log.d(TAG, "configureUsbIface(" + enabled + ")");
        }
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
                            if (isMdDirectTetheringEnable()) {
                                InterfaceConfiguration ifcg2 = new InterfaceConfiguration();
                                try {
                                    InetAddress addr2 = NetworkUtils.numericToInetAddress(BR_SUB_IFACE_ADDR);
                                    ifcg2.setLinkAddress(new LinkAddress(addr2, 24));
                                    if (enabled) {
                                        ifcg2.setInterfaceUp();
                                        this.mNMService.addBridgeInterface(iface, MD_DIRECT_TETHERING_IFACE_BR_SUB1);
                                        this.mNMService.addBridgeInterface(iface, MD_DIRECT_TETHERING_IFACE_BR_SUB2);
                                    } else {
                                        ifcg2.setInterfaceDown();
                                    }
                                    this.mNMService.setInterfaceConfig(MD_DIRECT_TETHERING_IFACE_BR_SUB2, ifcg2);
                                    this.mNMService.setInterfaceConfig(MD_DIRECT_TETHERING_IFACE_BR_SUB1, ifcg2);
                                } catch (Exception e) {
                                    e = e;
                                    Log.e(TAG, "Error configuring interface " + iface, e);
                                    return false;
                                }
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } catch (Exception e2) {
                        e = e2;
                    }
                }
            }
            return true;
        } catch (Exception e3) {
            Log.e(TAG, "Error listing Interfaces", e3);
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
        long ident;
        if (VDBG) {
            Log.d(TAG, "setUsbTethering(" + enable + ")");
        }
        UsbManager usbManager = (UsbManager) this.mContext.getSystemService("usb");
        synchronized (this.mPublicSync) {
            this.mUsbTetherEnabled = enable;
            this.mTetheredFail = false;
            if (enable) {
                this.mTetherDone = false;
                if (this.mRndisEnabled) {
                    ident = Binder.clearCallingIdentity();
                    try {
                        tetherUsb(true);
                    } finally {
                    }
                } else {
                    this.mUsbTetherRequested = true;
                    int value = Settings.System.getInt(this.mContext.getContentResolver(), "usb_tethering_type", 0);
                    if (value == 1 && isMtkTetheringEemSupport()) {
                        Log.d(TAG, "The MTK_TETHERING_EEM_SUPPORT is True");
                        usbManager.setCurrentFunction("eem");
                    } else {
                        Log.d(TAG, "The MTK_TETHERING_RNDIS only");
                        usbManager.setCurrentFunction("rndis");
                    }
                }
            } else {
                ident = Binder.clearCallingIdentity();
                try {
                    this.mUnTetherDone = false;
                    tetherUsb(false);
                    Binder.restoreCallingIdentity(ident);
                    if (this.mRndisEnabled) {
                        usbManager.setCurrentFunction(null);
                    }
                    this.mUsbTetherRequested = false;
                } finally {
                }
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

    private void checkDunRequired() {
        int requiredApn;
        int secureSetting = 2;
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm != null) {
            secureSetting = tm.getTetherApnRequired();
        }
        synchronized (this.mPublicSync) {
            if (VDBG) {
                Log.i(TAG, "checkDunRequired:" + secureSetting);
            }
            if (secureSetting != 2) {
                if (secureSetting == 1) {
                    requiredApn = 4;
                } else {
                    requiredApn = 5;
                }
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
            if (DBG) {
                Log.d(TAG, "mPreferredUpstreamMobileApn = " + this.mPreferredUpstreamMobileApn);
            }
        }
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

    public String[] getTetheredIfacePairs() {
        ArrayList<String> list = Lists.newArrayList();
        synchronized (this.mPublicSync) {
            for (TetherInterfaceSM sm : this.mIfaces.values()) {
                if (sm.isTethered()) {
                    list.add(sm.mMyUpstreamIfaceName);
                    list.add(sm.mIfaceName);
                    Log.i(TAG, "getTetheredIfacePairs:" + sm.mMyUpstreamIfaceName + ", " + sm.mIfaceName);
                }
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
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

    private void maybeLogMessage(State state, int what) {
        if (!DBG) {
            return;
        }
        Log.d(TAG, state.getName() + " got " + sMagicDecoderRing.get(what, Integer.toString(what)));
    }

    public void handleTetherIfaceChange() {
        this.mTetherMasterSM.sendMessage(327683);
        if (!isIpv6MasterSmOn()) {
            return;
        }
        this.mIpv6TetherMasterSM.sendMessage(327683);
    }

    private boolean isIpv6MasterSmOn() {
        if (isTetheringIpv6Support()) {
            return this.mIpv6FeatureEnable;
        }
        return false;
    }

    private boolean readIpv6FeatureEnable() {
        int value = Settings.System.getInt(this.mContext.getContentResolver(), "tether_ipv6_feature", 0);
        Log.d(TAG, "getIpv6FeatureEnable:" + value);
        return value == 1;
    }

    public boolean getIpv6FeatureEnable() {
        return this.mIpv6FeatureEnable;
    }

    public void setIpv6FeatureEnable(boolean enable) {
        Log.d(TAG, "setIpv6FeatureEnable:" + enable + " old:" + this.mIpv6FeatureEnable);
        int value = enable ? 1 : 0;
        if (this.mIpv6FeatureEnable == enable) {
            return;
        }
        this.mIpv6FeatureEnable = enable;
        Settings.System.putInt(this.mContext.getContentResolver(), "tether_ipv6_feature", value);
    }

    private boolean hasIpv6Address(int networkType) {
        LinkProperties netProperties;
        if (-1 == networkType || (netProperties = getConnectivityManager().getLinkProperties(networkType)) == null) {
            return false;
        }
        String iface = netProperties.getInterfaceName();
        return hasIpv6Address(iface);
    }

    private boolean hasIpv6Address(String iface) {
        if (iface == null || iface.isEmpty()) {
            return false;
        }
        String propertyName = "net.ipv6." + iface + ".prefix";
        String value = SystemProperties.get(propertyName);
        if (value == null || value.length() == 0) {
            Log.d(TAG, "This is No IPv6 prefix!");
            return false;
        }
        Log.d(TAG, "This is IPv6 prefix: " + value);
        return true;
    }

    private boolean hasIpv4Address(int networkType) {
        LinkProperties netProperties;
        if (-1 == networkType || (netProperties = getConnectivityManager().getLinkProperties(networkType)) == null) {
            return false;
        }
        for (LinkAddress l : netProperties.getLinkAddresses()) {
            if (l.getAddress() instanceof Inet4Address) {
                Log.i(TAG, "This is v4 address:" + l.getAddress());
                return true;
            }
            Log.i(TAG, "address:" + l.getAddress());
        }
        return false;
    }

    private boolean hasDhcpv6PD(int networkType) {
        LinkProperties netProperties;
        if (isIpv6TetherPdModeSupport()) {
            if (-1 == networkType || (netProperties = getConnectivityManager().getLinkProperties(networkType)) == null) {
                return false;
            }
            String iface = netProperties.getInterfaceName();
            return hasDhcpv6PD(iface);
        }
        Log.e(TAG, "[MSM_TetherModeAlive] bypass hasDhcpv6PD");
        return true;
    }

    private boolean hasDhcpv6PD(String iface) {
        if (isIpv6TetherPdModeSupport()) {
            if (iface == null || iface.isEmpty()) {
                return false;
            }
            String propertyName = "net.pd." + iface + ".prefix";
            String value = SystemProperties.get(propertyName);
            if (value == null || value.length() == 0) {
                Log.i(TAG, "This is No Dhcpv6PD prefix!");
                return false;
            }
            Log.i(TAG, "This is Dhcpv6PD prefix: " + value);
            return true;
        }
        Log.e(TAG, "[MSM_TetherModeAlive] bypass hasDhcpv6PD");
        return true;
    }

    class TetherInterfaceSM extends StateMachine {
        private static final int BASE_IFACE = 327780;
        static final int CMD_CELL_DUN_ERROR = 327786;
        static final int CMD_INTERFACE_DOWN = 327784;
        static final int CMD_INTERFACE_UP = 327785;
        static final int CMD_IP_FORWARDING_DISABLE_ERROR = 327788;
        static final int CMD_IP_FORWARDING_ENABLE_ERROR = 327787;
        static final int CMD_SET_DNS_FORWARDERS_ERROR = 327791;
        static final int CMD_START_TETHERING_ERROR = 327789;
        static final int CMD_STOP_TETHERING_ERROR = 327790;
        static final int CMD_TETHER_CONNECTION_CHANGED = 327792;
        static final int CMD_TETHER_MODE_DEAD = 327781;
        static final int CMD_TETHER_REQUESTED = 327782;
        static final int CMD_TETHER_UNREQUESTED = 327783;
        private boolean mAvailable;
        private State mDefaultState;
        private boolean mDhcpv6Enabled;
        String mIfaceName;
        private State mInitialState;
        int mLastError;
        String mMyUpstreamIfaceName;
        String mMyUpstreamIfaceNameIpv6;
        List<InetAddress> mMyUpstreamLP;
        List<InetAddress> mMyUpstreamLPIpv6;
        private State mStartingState;
        private boolean mTethered;
        private State mTetheredState;
        int mTetheredStateMessage;
        private State mUnavailableState;
        boolean mUsb;

        TetherInterfaceSM(String name, Looper looper, boolean usb) {
            super(name, looper);
            this.mMyUpstreamLP = new ArrayList();
            this.mMyUpstreamLPIpv6 = new ArrayList();
            this.mIfaceName = name;
            this.mUsb = usb;
            setLastError(0);
            if (Tethering.this.isTetheringIpv6Support()) {
                setLastError(16);
            }
            this.mDhcpv6Enabled = false;
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
                Log.i(Tethering.TAG, "getLastError:" + this.mLastError);
                i = this.mLastError;
            }
            return i;
        }

        private void setLastError(int error) {
            synchronized (Tethering.this.mPublicSync) {
                if (Tethering.this.isTetheringIpv6Support()) {
                    if (error >= 16) {
                        this.mLastError &= 15;
                        this.mLastError |= error;
                    } else {
                        this.mLastError &= 240;
                        this.mLastError |= error;
                    }
                } else {
                    this.mLastError = error;
                }
                Log.i(Tethering.TAG, "setLastError: " + this.mLastError);
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
            synchronized (Tethering.this.mPublicSync) {
                if (Tethering.this.isTetheringIpv6Support()) {
                    if ((this.mLastError & 15) == 0 || (this.mLastError & 240) == 16) {
                        ret = false;
                    }
                    return ret;
                }
                ret = this.mLastError != 0;
                return ret;
            }
        }

        class InitialState extends State {
            InitialState() {
            }

            public void enter() {
                Log.i(Tethering.TAG, "[ISM_Initial] enter, sendTetherStateChangedBroadcast");
                TetherInterfaceSM.this.setAvailable(true);
                TetherInterfaceSM.this.setTethered(false);
                Tethering.this.sendTetherStateChangedBroadcast();
            }

            public boolean processMessage(Message message) {
                Tethering.this.maybeLogMessage(this, message.what);
                if (Tethering.DBG) {
                    Log.i(Tethering.TAG, "[ISM_Initial] " + TetherInterfaceSM.this.mIfaceName + " processMessage what=" + message.what);
                }
                switch (message.what) {
                    case TetherInterfaceSM.CMD_TETHER_REQUESTED:
                        TetherInterfaceSM.this.setLastError(0);
                        Tethering.this.mTetherMasterSM.sendMessage(327681, TetherInterfaceSM.this);
                        if (Tethering.this.isTetheringIpv6Support()) {
                            TetherInterfaceSM.this.setLastError(16);
                            if (Tethering.this.mIpv6FeatureEnable) {
                                Tethering.this.mIpv6TetherMasterSM.sendMessage(327681, TetherInterfaceSM.this);
                            }
                        }
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mStartingState);
                        break;
                    case TetherInterfaceSM.CMD_INTERFACE_DOWN:
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
                Log.i(Tethering.TAG, "[ISM_Starting] enter");
                TetherInterfaceSM.this.setAvailable(false);
                if (TetherInterfaceSM.this.mUsb && !Tethering.this.configureUsbIface(true)) {
                    Tethering.this.mTetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                    TetherInterfaceSM.this.setLastError(10);
                    if (Tethering.this.isIpv6MasterSmOn()) {
                        Tethering.this.mIpv6TetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                        TetherInterfaceSM.this.setLastError(48);
                    }
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                    Tethering.this.mTetherDone = true;
                    Tethering.this.sendTetherStateChangedBroadcast();
                    return;
                }
                Log.i(Tethering.TAG, "[ISM_Starting] sendTetherStateChangedBroadcast");
                Tethering.this.sendTetherStateChangedBroadcast();
                TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mTetheredState);
            }

            public boolean processMessage(Message message) {
                Tethering.this.maybeLogMessage(this, message.what);
                if (Tethering.DBG) {
                    Log.i(Tethering.TAG, "[ISM_Starting] " + TetherInterfaceSM.this.mIfaceName + " processMessage what=" + message.what);
                }
                switch (message.what) {
                    case TetherInterfaceSM.CMD_TETHER_UNREQUESTED:
                        Tethering.this.mTetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            Tethering.this.mIpv6TetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                        }
                        if (TetherInterfaceSM.this.mUsb) {
                            if (Tethering.this.isMdDirectTetheringEnable()) {
                                Tethering.this.handleUsbDisconnect();
                            }
                            if (!Tethering.this.configureUsbIface(false)) {
                                TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(10);
                                if (Tethering.this.isIpv6MasterSmOn()) {
                                    TetherInterfaceSM.this.setLastError(48);
                                }
                            }
                        }
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                        break;
                    case TetherInterfaceSM.CMD_INTERFACE_DOWN:
                        Tethering.this.mTetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            Tethering.this.mIpv6TetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                        }
                        if (TetherInterfaceSM.this.mUsb && Tethering.this.isMdDirectTetheringEnable()) {
                            Tethering.this.handleUsbDisconnect();
                        }
                        TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mUnavailableState);
                        break;
                    case TetherInterfaceSM.CMD_CELL_DUN_ERROR:
                    case TetherInterfaceSM.CMD_IP_FORWARDING_ENABLE_ERROR:
                    case TetherInterfaceSM.CMD_IP_FORWARDING_DISABLE_ERROR:
                    case TetherInterfaceSM.CMD_START_TETHERING_ERROR:
                    case TetherInterfaceSM.CMD_STOP_TETHERING_ERROR:
                    case TetherInterfaceSM.CMD_SET_DNS_FORWARDERS_ERROR:
                        TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(5);
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            TetherInterfaceSM.this.setLastError(48);
                        }
                        break;
                }
                return true;
            }
        }

        class TetheredState extends State {
            TetheredState() {
            }

            public void enter() {
                Log.i(Tethering.TAG, "[ISM_Tethered] enter");
                try {
                    Tethering.this.mNMService.tetherInterface(TetherInterfaceSM.this.mIfaceName);
                    if (Tethering.this.isIpv6MasterSmOn()) {
                        try {
                            Tethering.this.mNMService.setDhcpv6Enabled(true, TetherInterfaceSM.this.mIfaceName);
                            TetherInterfaceSM.this.mDhcpv6Enabled = true;
                        } catch (Exception e) {
                            Log.e(Tethering.TAG, "[ISM_Tethered] Error setDhcpv6Enabled: " + e.toString());
                            TetherInterfaceSM.this.setLastError(48);
                            try {
                                TetherInterfaceSM.this.mDhcpv6Enabled = false;
                                Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                                Tethering.this.mNMService.setDhcpv6Enabled(false, TetherInterfaceSM.this.mIfaceName);
                            } catch (Exception ee) {
                                Log.e(Tethering.TAG, "[ISM_Tethered] untetherInterface failed, exception: " + ee);
                            }
                            TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                            return;
                        }
                    }
                    if (Tethering.DBG) {
                        Log.i(Tethering.TAG, "[ISM_Tethered] Tethered " + TetherInterfaceSM.this.mIfaceName);
                    }
                    TetherInterfaceSM.this.setAvailable(false);
                    TetherInterfaceSM.this.setTethered(true);
                    Tethering.this.mTetherDone = true;
                    Log.d(Tethering.TAG, "[ISM_Tethered] sendTetherStateChangedBroadcast");
                    Tethering.this.sendTetherStateChangedBroadcast();
                } catch (Exception e2) {
                    Log.e(Tethering.TAG, "[ISM_Tethered] Error Tethering: " + e2.toString());
                    TetherInterfaceSM.this.setLastError(6);
                    try {
                        Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                    } catch (Exception ee2) {
                        Log.e(Tethering.TAG, "Error untethering after failure!" + ee2.toString());
                    }
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                }
            }

            public void exit() {
                Log.i(Tethering.TAG, "[ISM_Tethered] exit ,sendMessage CMD_TETHER_MODE_UNREQUESTED to TetherMasterSM");
                Tethering.this.mTetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                if (!Tethering.this.isIpv6MasterSmOn()) {
                    return;
                }
                Log.i(Tethering.TAG, "[ISM_Tethered] exit ,sendMessage CMD_TETHER_MODE_UNREQUESTED to Ipv6TetherMaster");
                Tethering.this.mIpv6TetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
            }

            private void cleanupUpstream() {
                if (TetherInterfaceSM.this.mMyUpstreamIfaceName != null) {
                    try {
                        Tethering.this.mStatsService.forceUpdate();
                    } catch (Exception e) {
                        if (Tethering.VDBG) {
                            Log.e(Tethering.TAG, "[ISM_Tethered] Exception in forceUpdate: " + e.toString());
                        }
                    }
                    try {
                        Tethering.this.mNMService.stopInterfaceForwarding(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceName);
                    } catch (Exception e2) {
                        if (Tethering.VDBG) {
                            Log.e(Tethering.TAG, "Exception in removeInterfaceForward: " + e2.toString());
                        }
                    }
                    try {
                        Tethering.this.mNMService.disableNat(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceName);
                        Log.d(Tethering.TAG, "[ISM_Tethered] cleanupUpstream disableNat(" + TetherInterfaceSM.this.mIfaceName + ", " + TetherInterfaceSM.this.mMyUpstreamIfaceName + ")");
                        if (Tethering.this.isUsb(TetherInterfaceSM.this.mIfaceName)) {
                            Tethering.this.mNMService.enableUdpForwarding(false, TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceName, "");
                        }
                    } catch (Exception e3) {
                        if (Tethering.VDBG) {
                            Log.e(Tethering.TAG, "[ISM_Tethered] Exception in disableNat: " + e3.toString());
                        }
                    }
                    TetherInterfaceSM.this.mMyUpstreamIfaceName = null;
                    TetherInterfaceSM.this.mMyUpstreamLP.clear();
                }
                if (TetherInterfaceSM.this.mUsb && Tethering.this.isMdDirectTetheringEnable()) {
                    Log.d(Tethering.TAG, "[ISM_Tethered] mTetheredStateMessage:" + TetherInterfaceSM.this.mTetheredStateMessage);
                    if (TetherInterfaceSM.this.mTetheredStateMessage == TetherInterfaceSM.CMD_TETHER_UNREQUESTED || TetherInterfaceSM.this.mTetheredStateMessage == TetherInterfaceSM.CMD_INTERFACE_DOWN) {
                        return;
                    }
                    try {
                        Tethering.this.mNMService.clearBridgeMac(TetherInterfaceSM.this.mIfaceName);
                    } catch (Exception e4) {
                        Log.i(Tethering.TAG, "clearBridgeMac Error: " + e4);
                    }
                }
            }

            private void cleanupUpstreamIpv6() {
                if (TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 != null) {
                    try {
                        Tethering.this.mNMService.clearRouteIpv6(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6);
                        Log.i(Tethering.TAG, "[ISM_Tethered] cleanupUpstream clearRouteIpv6(" + TetherInterfaceSM.this.mIfaceName + ", " + TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 + ")");
                        Tethering.this.mNMService.clearSourceRouteIpv6(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6);
                        Log.i(Tethering.TAG, "[ISM_Tethered] clearSourceRouteIpv6(" + TetherInterfaceSM.this.mIfaceName + ", " + TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 + ")");
                    } catch (Exception e) {
                        if (Tethering.VDBG) {
                            Log.e(Tethering.TAG, "[ISM_Tethered] Exception in clearRouteIpv6: " + e.toString());
                        }
                    }
                    TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 = null;
                    TetherInterfaceSM.this.mMyUpstreamLPIpv6.clear();
                }
            }

            public boolean processMessage(Message message) {
                boolean isSameLinkproperty;
                Tethering.this.maybeLogMessage(this, message.what);
                if (Tethering.DBG) {
                    Log.i(Tethering.TAG, "[ISM_Tethered] " + TetherInterfaceSM.this.mIfaceName + " processMessage what=" + message.what);
                }
                boolean error = false;
                TetherInterfaceSM.this.mTetheredStateMessage = message.what;
                switch (message.what) {
                    case TetherInterfaceSM.CMD_TETHER_MODE_DEAD:
                        break;
                    case TetherInterfaceSM.CMD_TETHER_REQUESTED:
                    case TetherInterfaceSM.CMD_INTERFACE_UP:
                    default:
                        return false;
                    case TetherInterfaceSM.CMD_TETHER_UNREQUESTED:
                    case TetherInterfaceSM.CMD_INTERFACE_DOWN:
                        Log.i(Tethering.TAG, "[ISM_Tethered] mMyUpstreamIfaceName: " + TetherInterfaceSM.this.mMyUpstreamIfaceName);
                        cleanupUpstream();
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            cleanupUpstreamIpv6();
                        }
                        try {
                            Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                            if (Tethering.this.isIpv6MasterSmOn() || TetherInterfaceSM.this.mDhcpv6Enabled) {
                                TetherInterfaceSM.this.mDhcpv6Enabled = false;
                                Tethering.this.mNMService.setDhcpv6Enabled(false, TetherInterfaceSM.this.mIfaceName);
                                Tethering.this.mNMService.disableNatIpv6(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6);
                                break;
                            }
                            Tethering.this.mTetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                            if (Tethering.this.isIpv6MasterSmOn()) {
                                Tethering.this.mIpv6TetherMasterSM.sendMessage(327682, TetherInterfaceSM.this);
                            }
                            if (TetherInterfaceSM.this.mUsb && Tethering.this.isMdDirectTetheringEnable()) {
                                Tethering.this.handleUsbDisconnect();
                            }
                            if (message.what == TetherInterfaceSM.CMD_TETHER_UNREQUESTED) {
                                if (TetherInterfaceSM.this.mUsb && !Tethering.this.configureUsbIface(false)) {
                                    TetherInterfaceSM.this.setLastError(10);
                                    if (Tethering.this.isTetheringIpv6Support()) {
                                        TetherInterfaceSM.this.setLastError(48);
                                    }
                                }
                                TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                            } else if (message.what == TetherInterfaceSM.CMD_INTERFACE_DOWN) {
                                TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mUnavailableState);
                            }
                            if (!Tethering.DBG) {
                                return true;
                            }
                            Log.i(Tethering.TAG, "[ISM_Tethered] Untethered " + TetherInterfaceSM.this.mIfaceName);
                            return true;
                        } catch (Exception e) {
                            TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(7);
                            if (!Tethering.this.isIpv6MasterSmOn()) {
                                return true;
                            }
                            TetherInterfaceSM.this.setLastError(48);
                            return true;
                        }
                    case TetherInterfaceSM.CMD_CELL_DUN_ERROR:
                    case TetherInterfaceSM.CMD_IP_FORWARDING_ENABLE_ERROR:
                    case TetherInterfaceSM.CMD_IP_FORWARDING_DISABLE_ERROR:
                    case TetherInterfaceSM.CMD_START_TETHERING_ERROR:
                    case TetherInterfaceSM.CMD_STOP_TETHERING_ERROR:
                    case TetherInterfaceSM.CMD_SET_DNS_FORWARDERS_ERROR:
                        error = true;
                        break;
                    case TetherInterfaceSM.CMD_TETHER_CONNECTION_CHANGED:
                        String s = (String) message.obj;
                        String newUpstreamIfaceName = null;
                        List<InetAddress> newUpstreamLP = new ArrayList<>();
                        String smName = null;
                        if (!Tethering.this.isIpv6MasterSmOn()) {
                            newUpstreamIfaceName = s;
                        } else if (s != null) {
                            Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED s:" + s);
                            String[] IfaceNameSmNames = s.split(",");
                            if (IfaceNameSmNames.length > 1) {
                                Log.i(Tethering.TAG, "[ISM_Tethered] IfaceNameSmNames[0]:" + IfaceNameSmNames[0] + " IfaceNameSmNames[1]:" + IfaceNameSmNames[1]);
                                newUpstreamIfaceName = IfaceNameSmNames[0];
                                smName = IfaceNameSmNames[1];
                                if ("empty".equals(newUpstreamIfaceName)) {
                                    newUpstreamIfaceName = null;
                                }
                            }
                        }
                        Log.i(Tethering.TAG, "[ISM_Tethered:" + smName + "] CMD_TETHER_CONNECTION_CHANGED mMyUpstreamIfaceName: " + TetherInterfaceSM.this.mMyUpstreamIfaceName + ", mMyUpstreamIfaceNameIpv6:" + TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 + ", newUpstreamIfaceName: " + newUpstreamIfaceName);
                        if (newUpstreamIfaceName == null && Tethering.this.isIpv6MasterSmOn() && Tethering.MASTERSM_IPV6.equals(smName)) {
                            TetherInterfaceSM.this.setLastError(48);
                        }
                        if (newUpstreamIfaceName != null) {
                            NetworkInterface ni = null;
                            try {
                                ni = NetworkInterface.getByName(newUpstreamIfaceName);
                            } catch (NullPointerException e2) {
                                Log.e(Tethering.TAG, "Error NetworkInterface.getByName:", e2);
                            } catch (SocketException e3) {
                                Log.e(Tethering.TAG, "Error NetworkInterface.getByName:", e3);
                            }
                            if (ni != null) {
                                Enumeration<InetAddress> inet_enum = ni.getInetAddresses();
                                List<InetAddress> list = Collections.list(inet_enum);
                                Log.i(Tethering.TAG, "getInetAddresses newUpstreamLP list: " + list);
                                if (!Tethering.this.isIpMatchSM(smName, list)) {
                                    Log.i(Tethering.TAG, "[ISM_Tethered] Connection changed IP not match SM - dropping");
                                    return true;
                                }
                                newUpstreamLP = list;
                                Log.i(Tethering.TAG, "[ISM_Tethered:" + smName + "] mMyUpstreamLP: " + TetherInterfaceSM.this.mMyUpstreamLP);
                                Log.i(Tethering.TAG, "[ISM_Tethered:" + smName + "] mMyUpstreamLPIpv6: " + TetherInterfaceSM.this.mMyUpstreamLPIpv6);
                                Log.i(Tethering.TAG, "[ISM_Tethered:" + smName + "] newUpstreamLP: " + list);
                            }
                            break;
                        }
                        if (smName == null || Tethering.MASTERSM_IPV4.equals(smName)) {
                            isSameLinkproperty = Tethering.this.mPreferredUpstreamMobileApn == 4 ? TetherInterfaceSM.this.mMyUpstreamLP.size() == newUpstreamLP.size() ? TetherInterfaceSM.this.mMyUpstreamLP.containsAll(newUpstreamLP) : false : true;
                            if ((TetherInterfaceSM.this.mMyUpstreamIfaceName == null && newUpstreamIfaceName == null) || (TetherInterfaceSM.this.mMyUpstreamIfaceName != null && TetherInterfaceSM.this.mMyUpstreamIfaceName.equals(newUpstreamIfaceName) && isSameLinkproperty)) {
                                if (!Tethering.VDBG) {
                                    return true;
                                }
                                Log.i(Tethering.TAG, "[ISM_Tethered] Connection changed noop - dropping");
                                return true;
                            }
                        } else if (Tethering.MASTERSM_IPV6.equals(smName)) {
                            isSameLinkproperty = Tethering.this.mPreferredUpstreamMobileApn == 4 ? TetherInterfaceSM.this.mMyUpstreamLPIpv6.size() == newUpstreamLP.size() ? TetherInterfaceSM.this.mMyUpstreamLPIpv6.containsAll(newUpstreamLP) : false : true;
                            if ((TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 == null && newUpstreamIfaceName == null) || (TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 != null && TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6.equals(newUpstreamIfaceName) && isSameLinkproperty)) {
                                if (!Tethering.VDBG) {
                                    return true;
                                }
                                Log.i(Tethering.TAG, "[ISM_Tethered] Connection changed noop - dropping ipv6");
                                return true;
                            }
                        }
                        Tethering.this.mIsTetheringChangeDone = false;
                        if (!Tethering.this.isTetheringIpv6Support() || smName == null || Tethering.MASTERSM_IPV4.equals(smName)) {
                            cleanupUpstream();
                        } else if (Tethering.MASTERSM_IPV6.equals(smName)) {
                            cleanupUpstreamIpv6();
                        }
                        if (Tethering.this.mPreferredUpstreamMobileApn == 4) {
                            try {
                                InterfaceConfiguration ifcg = Tethering.this.mNMService.getInterfaceConfig(newUpstreamIfaceName);
                                if (ifcg == null || !(ifcg.isActive() || (ifcg.hasFlag("up") && Tethering.this.hasIpv6Address(newUpstreamIfaceName)))) {
                                    Log.i(Tethering.TAG, "[ISM_Tethered] " + newUpstreamIfaceName + " is down!");
                                    newUpstreamIfaceName = null;
                                    newUpstreamLP.clear();
                                } else {
                                    Log.i(Tethering.TAG, "[ISM_Tethered] " + newUpstreamIfaceName + " is up!");
                                }
                            } catch (Exception e4) {
                                Log.e(Tethering.TAG, "[ISM_Tethered] Exception getInterfaceConfig: " + e4.toString());
                                newUpstreamIfaceName = null;
                                newUpstreamLP.clear();
                            }
                        }
                        if (newUpstreamIfaceName != null) {
                            try {
                                if (!Tethering.this.isTetheringIpv6Support()) {
                                    Tethering.this.mNMService.enableNat(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    Tethering.this.mNMService.startInterfaceForwarding(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    TetherInterfaceSM.this.mMyUpstreamIfaceName = newUpstreamIfaceName;
                                    TetherInterfaceSM.this.mMyUpstreamLP = newUpstreamLP;
                                    Tethering.this.mNMService.setIpForwardingEnabled(true);
                                    Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED enableNat(" + TetherInterfaceSM.this.mIfaceName + ", " + newUpstreamIfaceName + ")");
                                } else if (smName == null || Tethering.MASTERSM_IPV4.equals(smName)) {
                                    Tethering.this.mNMService.enableNat(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    Tethering.this.mNMService.startInterfaceForwarding(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    TetherInterfaceSM.this.mMyUpstreamIfaceName = newUpstreamIfaceName;
                                    TetherInterfaceSM.this.mMyUpstreamLP = newUpstreamLP;
                                    Tethering.this.mNMService.setIpForwardingEnabled(true);
                                    Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED enableNat for:" + smName + "(" + TetherInterfaceSM.this.mIfaceName + ", " + newUpstreamIfaceName + ")");
                                    if (SystemProperties.getBoolean(Tethering.SYSTEM_PROPERTY_NSIOT_PENDING, false) && Tethering.this.isUsb(TetherInterfaceSM.this.mIfaceName)) {
                                        Tethering.this.enableUdpForwardingForUsb(true, null);
                                    }
                                } else if (Tethering.this.mIpv6FeatureEnable && Tethering.MASTERSM_IPV6.equals(smName)) {
                                    Tethering.this.mNMService.setRouteIpv6(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    Tethering.this.mNMService.enableNatIpv6(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 = newUpstreamIfaceName;
                                    TetherInterfaceSM.this.mMyUpstreamLPIpv6 = newUpstreamLP;
                                    Tethering.this.mNMService.setSourceRouteIpv6(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    Tethering.this.mNMService.setIpv6ForwardingEnabled(true);
                                    Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED enableNat for:" + smName + "(" + TetherInterfaceSM.this.mIfaceName + ", " + newUpstreamIfaceName + ")");
                                    if (SystemProperties.getBoolean(Tethering.SYSTEM_PROPERTY_NSIOT_PENDING, false) && Tethering.this.isUsb(TetherInterfaceSM.this.mIfaceName)) {
                                        Tethering.this.enableUdpForwardingForUsb(true, null);
                                    }
                                }
                            } catch (Exception e5) {
                                Log.e(Tethering.TAG, "[ISM_Tethered] Exception enabling Nat: " + e5.toString());
                                try {
                                    Tethering.this.mNMService.disableNat(TetherInterfaceSM.this.mIfaceName, newUpstreamIfaceName);
                                    break;
                                } catch (Exception e6) {
                                }
                                try {
                                    Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                                    if (Tethering.this.isIpv6MasterSmOn() || TetherInterfaceSM.this.mDhcpv6Enabled) {
                                        TetherInterfaceSM.this.mDhcpv6Enabled = false;
                                        Tethering.this.mNMService.setDhcpv6Enabled(false, TetherInterfaceSM.this.mIfaceName);
                                        Tethering.this.mNMService.disableNatIpv6(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6);
                                    }
                                    break;
                                } catch (Exception ee) {
                                    Log.e(Tethering.TAG, "[ISM_Tethered] untetherInterface failed, exception: " + ee);
                                }
                                if (Tethering.this.isIpv6MasterSmOn()) {
                                    TetherInterfaceSM.this.setLastError(48);
                                }
                                cleanupUpstream();
                                cleanupUpstreamIpv6();
                                TetherInterfaceSM.this.setLastError(8);
                                TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                                if (Tethering.this.mPreferredUpstreamMobileApn == 4) {
                                    Tethering.this.mContext.sendBroadcastAsUser(new Intent("android.net.conn.TETHER_CHANGED_DONE"), UserHandle.ALL);
                                }
                                Tethering.this.mIsTetheringChangeDone = true;
                                return true;
                            }
                            break;
                        } else {
                            try {
                                if (Tethering.this.isIpv6MasterSmOn()) {
                                    if (Tethering.MASTERSM_IPV4.equals(smName)) {
                                        Tethering.this.mNMService.setIpForwardingEnabled(false);
                                    }
                                    if (Tethering.MASTERSM_IPV6.equals(smName)) {
                                        Tethering.this.mNMService.setIpv6ForwardingEnabled(false);
                                    }
                                } else {
                                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                                }
                            } catch (Exception eee) {
                                Log.e(Tethering.TAG, "[ISM_Tethered] untetherInterface failed, exception: " + eee);
                            }
                        }
                        if (!Tethering.this.isIpv6MasterSmOn()) {
                            TetherInterfaceSM.this.mMyUpstreamIfaceName = newUpstreamIfaceName;
                            TetherInterfaceSM.this.mMyUpstreamLP = newUpstreamLP;
                        } else if (smName == null || Tethering.MASTERSM_IPV4.equals(smName)) {
                            if (newUpstreamIfaceName == null) {
                                Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED mMyUpstreamIfaceName = null");
                                TetherInterfaceSM.this.mMyUpstreamIfaceName = null;
                                TetherInterfaceSM.this.mMyUpstreamLP.clear();
                            } else {
                                TetherInterfaceSM.this.mMyUpstreamIfaceName = newUpstreamIfaceName;
                                TetherInterfaceSM.this.mMyUpstreamLP = newUpstreamLP;
                            }
                        } else if (Tethering.MASTERSM_IPV6.equals(smName)) {
                            if (newUpstreamIfaceName == null) {
                                Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED mMyUpstreamIfaceNameIpv6 = null");
                                TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 = null;
                                TetherInterfaceSM.this.mMyUpstreamLPIpv6.clear();
                            } else {
                                TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 = newUpstreamIfaceName;
                                TetherInterfaceSM.this.mMyUpstreamLPIpv6 = newUpstreamLP;
                            }
                        }
                        Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED finished! [" + smName + "] mCurrentUpstreamIface to " + newUpstreamIfaceName);
                        Tethering.this.mCurrentUpstreamIface = newUpstreamIfaceName;
                        if (Tethering.this.mPreferredUpstreamMobileApn == 4) {
                            Tethering.this.mContext.sendBroadcastAsUser(new Intent("android.net.conn.TETHER_CHANGED_DONE"), UserHandle.ALL);
                        }
                        Tethering.this.mIsTetheringChangeDone = true;
                        if (!Tethering.this.isIpv6MasterSmOn() || !Tethering.MASTERSM_IPV6.equals(smName)) {
                            return true;
                        }
                        if (TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6 != null) {
                            TetherInterfaceSM.this.setLastError(32);
                        } else {
                            TetherInterfaceSM.this.setLastError(48);
                        }
                        Tethering.this.sendTetherStateChangedBroadcast();
                        return true;
                }
                Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_MODE_DEAD, mMyUpstreamIfaceName: " + TetherInterfaceSM.this.mMyUpstreamIfaceName);
                Log.i(Tethering.TAG, "[ISM_Tethered] CMD_TETHER_MODE_DEAD, mMyUpstreamIfaceNameIpv6: " + TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6);
                cleanupUpstream();
                if (Tethering.this.isIpv6MasterSmOn()) {
                    cleanupUpstreamIpv6();
                }
                try {
                    Tethering.this.mNMService.untetherInterface(TetherInterfaceSM.this.mIfaceName);
                    if (Tethering.this.isIpv6MasterSmOn() || TetherInterfaceSM.this.mDhcpv6Enabled) {
                        TetherInterfaceSM.this.mDhcpv6Enabled = false;
                        Tethering.this.mNMService.setDhcpv6Enabled(false, TetherInterfaceSM.this.mIfaceName);
                        Tethering.this.mNMService.disableNatIpv6(TetherInterfaceSM.this.mIfaceName, TetherInterfaceSM.this.mMyUpstreamIfaceNameIpv6);
                    }
                    if (error) {
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            TetherInterfaceSM.this.setLastError(48);
                        }
                        TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(5);
                        return true;
                    }
                    if (Tethering.DBG) {
                        Log.i(Tethering.TAG, "[ISM_Tethered] Tether lost upstream connection " + TetherInterfaceSM.this.mIfaceName);
                    }
                    Log.i(Tethering.TAG, "[ISM_Tethered] sendTetherStateChangedBroadcast in CMD_TETHER_MODE_DEAD of TetheredState");
                    Tethering.this.sendTetherStateChangedBroadcast();
                    if (TetherInterfaceSM.this.mUsb && !Tethering.this.configureUsbIface(false)) {
                        TetherInterfaceSM.this.setLastError(10);
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            TetherInterfaceSM.this.setLastError(48);
                        }
                    }
                    TetherInterfaceSM.this.transitionTo(TetherInterfaceSM.this.mInitialState);
                    return true;
                } catch (Exception e7) {
                    if (Tethering.this.isIpv6MasterSmOn()) {
                        TetherInterfaceSM.this.setLastError(48);
                    }
                    TetherInterfaceSM.this.setLastErrorAndTransitionToInitialState(7);
                    return true;
                }
            }
        }

        class UnavailableState extends State {
            UnavailableState() {
            }

            public void enter() {
                Log.i(Tethering.TAG, "[ISM_Unavailable] enter, sendTetherStateChangedBroadcast");
                TetherInterfaceSM.this.setAvailable(false);
                TetherInterfaceSM.this.setLastError(0);
                if (Tethering.this.isIpv6MasterSmOn()) {
                    TetherInterfaceSM.this.setLastError(16);
                }
                TetherInterfaceSM.this.setTethered(false);
                Tethering.this.mTetherDone = true;
                Tethering.this.mTetheredFail = true;
                Tethering.this.sendTetherStateChangedBroadcast();
            }

            public boolean processMessage(Message message) {
                Log.i(Tethering.TAG, "[ISM_Unavailable] " + TetherInterfaceSM.this.mIfaceName + " processMessage what=" + message.what);
                switch (message.what) {
                    case TetherInterfaceSM.CMD_INTERFACE_UP:
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

    class UpstreamNetworkCallback extends ConnectivityManager.NetworkCallback {
        UpstreamNetworkCallback() {
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties newLp) {
            Tethering.this.mTetherMasterSM.sendMessage(327685, new NetworkState((NetworkInfo) null, newLp, (NetworkCapabilities) null, network, (String) null, (String) null));
        }

        @Override
        public void onLost(Network network) {
            Tethering.this.mTetherMasterSM.sendMessage(327686, network);
        }
    }

    class UpstreamNetworkMonitor {
        ConnectivityManager.NetworkCallback mDefaultNetworkCallback;
        ConnectivityManager.NetworkCallback mDunTetheringCallback;
        final HashMap<Network, NetworkState> mNetworkMap = new HashMap<>();

        UpstreamNetworkMonitor() {
        }

        void start() {
            stop();
            this.mDefaultNetworkCallback = Tethering.this.new UpstreamNetworkCallback();
            Tethering.this.getConnectivityManager().registerDefaultNetworkCallback(this.mDefaultNetworkCallback);
            NetworkRequest dunTetheringRequest = new NetworkRequest.Builder().addTransportType(0).removeCapability(13).addCapability(2).build();
            this.mDunTetheringCallback = Tethering.this.new UpstreamNetworkCallback();
            Tethering.this.getConnectivityManager().registerNetworkCallback(dunTetheringRequest, this.mDunTetheringCallback);
        }

        void stop() {
            if (this.mDefaultNetworkCallback != null) {
                Tethering.this.getConnectivityManager().unregisterNetworkCallback(this.mDefaultNetworkCallback);
                this.mDefaultNetworkCallback = null;
            }
            if (this.mDunTetheringCallback != null) {
                Tethering.this.getConnectivityManager().unregisterNetworkCallback(this.mDunTetheringCallback);
                this.mDunTetheringCallback = null;
            }
            this.mNetworkMap.clear();
        }

        boolean processLinkPropertiesChanged(NetworkState networkState) {
            if (networkState == null || networkState.network == null || networkState.linkProperties == null) {
                return false;
            }
            this.mNetworkMap.put(networkState.network, networkState);
            if (Tethering.this.mCurrentUpstreamIface != null) {
                for (String ifname : networkState.linkProperties.getAllInterfaceNames()) {
                    if (Tethering.this.mCurrentUpstreamIface.equals(ifname)) {
                        return true;
                    }
                }
            }
            return false;
        }

        void processNetworkLost(Network network) {
            if (network == null) {
                return;
            }
            this.mNetworkMap.remove(network);
        }
    }

    class TetherMasterSM extends StateMachine {
        private static final int BASE_MASTER = 327680;
        static final int CMD_RETRY_UPSTREAM = 327684;
        static final int CMD_TETHER_MODE_REQUESTED = 327681;
        static final int CMD_TETHER_MODE_UNREQUESTED = 327682;
        static final int CMD_UPSTREAM_CHANGED = 327683;
        static final int EVENT_UPSTREAM_LINKPROPERTIES_CHANGED = 327685;
        static final int EVENT_UPSTREAM_LOST = 327686;
        private static final int UPSTREAM_SETTLE_TIME_MS = 10000;
        private SimChangeBroadcastReceiver mBroadcastReceiver;
        private Thread mDhcpv6PDThread;
        private ConnectivityManager.NetworkCallback mIPv6MobileUpstreamCallback;
        private State mInitialState;
        private int mMobileApnReserved;
        private ConnectivityManager.NetworkCallback mMobileUpstreamCallback;
        private String mName;
        private ArrayList<TetherInterfaceSM> mNotifyList;
        private String mPreviousDhcpv6PDIface;
        private int mSequenceNumber;
        private State mSetDnsForwardersErrorState;
        private State mSetIpForwardingDisabledErrorState;
        private State mSetIpForwardingEnabledErrorState;
        private final AtomicInteger mSimBcastGenerationNumber;
        private State mStartTetheringErrorState;
        private State mStopTetheringErrorState;
        private State mTetherModeAliveState;

        TetherMasterSM(String name, Looper looper) {
            super(name, looper);
            this.mMobileApnReserved = -1;
            this.mSimBcastGenerationNumber = new AtomicInteger(0);
            this.mBroadcastReceiver = null;
            this.mName = name;
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
            this.mDhcpv6PDThread = null;
            this.mPreviousDhcpv6PDIface = null;
        }

        class TetherMasterUtilState extends State {
            TetherMasterUtilState() {
            }

            public boolean processMessage(Message m) {
                return false;
            }

            protected boolean turnOnUpstreamMobileConnection(int apnType) {
                if (apnType == -1) {
                    return false;
                }
                if (apnType != TetherMasterSM.this.mMobileApnReserved) {
                    turnOffUpstreamMobileConnection();
                }
                if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV4) && TetherMasterSM.this.mMobileUpstreamCallback != null) {
                    return true;
                }
                if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6) && TetherMasterSM.this.mIPv6MobileUpstreamCallback != null) {
                    return true;
                }
                switch (apnType) {
                    case 0:
                    case 4:
                    case 5:
                        boolean isCcpMode = SystemProperties.getBoolean("persist.op12.ccp.mode", false);
                        if (isCcpMode) {
                            Log.i(Tethering.TAG, "isCcpMode enabled, don't enable mobile");
                            return false;
                        }
                        TetherMasterSM.this.mMobileApnReserved = apnType;
                        Log.i(Tethering.TAG, "[MSM_TetherModeAlive][" + TetherMasterSM.this.mName + "] mMobileApnReserved:" + TetherMasterSM.this.mMobileApnReserved);
                        NetworkRequest.Builder builder = new NetworkRequest.Builder().addTransportType(0);
                        if (apnType == 4) {
                            builder.removeCapability(13).addCapability(2);
                        } else {
                            builder.addCapability(12);
                        }
                        NetworkRequest mobileUpstreamRequest = builder.build();
                        if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV4)) {
                            TetherMasterSM.this.mMobileUpstreamCallback = new ConnectivityManager.NetworkCallback();
                            if (Tethering.DBG) {
                                Log.d(Tethering.TAG, "requesting mobile upstream network: " + mobileUpstreamRequest);
                            }
                            Tethering.this.getConnectivityManager().requestNetwork(mobileUpstreamRequest, TetherMasterSM.this.mMobileUpstreamCallback, 0, apnType);
                        } else {
                            TetherMasterSM.this.mIPv6MobileUpstreamCallback = new ConnectivityManager.NetworkCallback();
                            Log.d(Tethering.TAG, "" + TetherMasterSM.this.mName + " requesting mobile upstream network: " + mobileUpstreamRequest);
                            Tethering.this.getConnectivityManager().requestNetwork(mobileUpstreamRequest, TetherMasterSM.this.mIPv6MobileUpstreamCallback, 0, apnType);
                        }
                        return true;
                    case 1:
                    case 2:
                    case 3:
                    default:
                        return false;
                }
            }

            protected void turnOffUpstreamMobileConnection() {
                if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV4)) {
                    if (TetherMasterSM.this.mMobileUpstreamCallback != null) {
                        if (Tethering.DBG) {
                            Log.d(Tethering.TAG, "" + TetherMasterSM.this.mName + " unregister mobile upstream network ");
                        }
                        Tethering.this.getConnectivityManager().unregisterNetworkCallback(TetherMasterSM.this.mMobileUpstreamCallback);
                        TetherMasterSM.this.mMobileUpstreamCallback = null;
                    }
                } else if (TetherMasterSM.this.mIPv6MobileUpstreamCallback != null) {
                    if (Tethering.DBG) {
                        Log.d(Tethering.TAG, "" + TetherMasterSM.this.mName + " unregister mobile upstream network ");
                    }
                    Tethering.this.getConnectivityManager().unregisterNetworkCallback(TetherMasterSM.this.mIPv6MobileUpstreamCallback);
                    TetherMasterSM.this.mIPv6MobileUpstreamCallback = null;
                }
                TetherMasterSM.this.mMobileApnReserved = -1;
            }

            protected boolean turnOnMasterTetherSettings() {
                try {
                    if (Tethering.this.isIpv6MasterSmOn() && !Tethering.MASTERSM_IPV4.equals(TetherMasterSM.this.mName) && Tethering.MASTERSM_IPV6.equals(TetherMasterSM.this.mName)) {
                        Tethering.this.mNMService.setIpv6ForwardingEnabled(true);
                    }
                    try {
                        Tethering.this.mNMService.startTethering(Tethering.this.mDhcpRange);
                    } catch (Exception e) {
                        try {
                            Tethering.this.mNMService.stopTethering();
                            Tethering.this.mNMService.startTethering(Tethering.this.mDhcpRange);
                        } catch (Exception e2) {
                            TetherMasterSM.this.transitionTo(TetherMasterSM.this.mStartTetheringErrorState);
                            return false;
                        }
                    }
                    return true;
                } catch (Exception e3) {
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mSetIpForwardingEnabledErrorState);
                    return false;
                }
            }

            protected boolean turnOffMasterTetherSettings() {
                try {
                    Tethering.this.mNMService.stopTethering();
                    try {
                        if (!Tethering.this.isIpv6MasterSmOn() || Tethering.MASTERSM_IPV4.equals(TetherMasterSM.this.mName)) {
                            Tethering.this.mNMService.setIpForwardingEnabled(false);
                        } else if (Tethering.MASTERSM_IPV6.equals(TetherMasterSM.this.mName)) {
                            Tethering.this.mNMService.setIpv6ForwardingEnabled(false);
                        }
                        TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
                        return true;
                    } catch (Exception e) {
                        TetherMasterSM.this.transitionTo(TetherMasterSM.this.mSetIpForwardingDisabledErrorState);
                        return false;
                    }
                } catch (Exception e2) {
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mStopTetheringErrorState);
                    return false;
                }
            }

            private boolean checkDataEnabled(int networkType) {
                TelephonyManager tm = TelephonyManager.getDefault();
                boolean dataEnabled = tm.getDataEnabled();
                Log.i(Tethering.TAG, "checkDataEnabled:" + dataEnabled);
                return dataEnabled;
            }

            protected void chooseUpstreamType(boolean tryCell) {
                int upType = -1;
                String iface = null;
                int radioNetworkType = 0;
                Tethering.this.updateConfiguration();
                synchronized (Tethering.this.mPublicSync) {
                    if (Tethering.VDBG) {
                        String result = "";
                        Iterator<Integer> iterator = Tethering.this.mUpstreamIfaceTypes.iterator();
                        for (int i = 0; i < Tethering.this.mUpstreamIfaceTypes.size(); i++) {
                            result = result + " " + iterator.next();
                        }
                        Log.d(Tethering.TAG, "[" + TetherMasterSM.this.mName + "]chooseUpstreamType has upstream iface types: " + result);
                    }
                    Iterator netType$iterator = Tethering.this.mUpstreamIfaceTypes.iterator();
                    while (true) {
                        if (!netType$iterator.hasNext()) {
                            break;
                        }
                        Integer netType = (Integer) netType$iterator.next();
                        NetworkInfo info = Tethering.this.getConnectivityManager().getNetworkInfo(netType.intValue());
                        if (info != null && info.isConnected()) {
                            break;
                        }
                    }
                }
                if (Tethering.DBG) {
                    Log.d(Tethering.TAG, "[" + TetherMasterSM.this.mName + "]chooseUpstreamType(" + tryCell + "), preferredApn=" + ConnectivityManager.getNetworkTypeName(Tethering.this.mPreferredUpstreamMobileApn) + ", got type=" + ConnectivityManager.getNetworkTypeName(upType));
                }
                Log.d(Tethering.TAG, "pre-checkDataEnabled + " + checkDataEnabled(upType));
                switch (upType) {
                    case -1:
                        if (!tryCell || !turnOnUpstreamMobileConnection(Tethering.this.mPreferredUpstreamMobileApn)) {
                            TetherMasterSM.this.sendMessageDelayed(TetherMasterSM.CMD_RETRY_UPSTREAM, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                        }
                        break;
                    case 4:
                    case 5:
                        if (checkDataEnabled(upType)) {
                            turnOnUpstreamMobileConnection(upType);
                        }
                        break;
                    default:
                        turnOffUpstreamMobileConnection();
                        break;
                }
                if (Tethering.this.isTetheringIpv6Support() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6) && Tethering.this.mIpv6FeatureEnable) {
                    if (!Tethering.this.hasIpv6Address(upType)) {
                        Log.i(Tethering.TAG, "we have no ipv6 address, upType:" + upType);
                        upType = -1;
                    } else if (Tethering.this.isIpv6TetherPdModeSupport()) {
                        LinkProperties linkProperties = Tethering.this.getConnectivityManager().getLinkProperties(upType);
                        ifacePD = linkProperties != null ? linkProperties.getInterfaceName() : null;
                        if (ifacePD == null || Tethering.this.hasDhcpv6PD(ifacePD)) {
                            ifacePD = null;
                        } else {
                            Log.i(Tethering.TAG, "we have no dhcp ipv6 PD address, iface:" + ifacePD);
                            upType = -1;
                        }
                    }
                }
                if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6) && Tethering.this.isIpv6TetherPdModeSupport() && Tethering.this.mIpv6FeatureEnable) {
                    Log.i(Tethering.TAG, "mPreviousDhcpv6PDIface:" + TetherMasterSM.this.mPreviousDhcpv6PDIface + ",ifacePD:" + ifacePD + ",upType:" + upType);
                    if (TetherMasterSM.this.mPreviousDhcpv6PDIface != null && ((ifacePD != null && ifacePD != TetherMasterSM.this.mPreviousDhcpv6PDIface) || (ifacePD == null && upType == -1))) {
                        TetherMasterSM.this.stopDhcpv6PDSequence();
                    }
                    if (ifacePD != null) {
                        runDhcpv6PDSequence(ifacePD);
                    }
                }
                if (upType != -1) {
                    LinkProperties linkProperties2 = Tethering.this.getConnectivityManager().getLinkProperties(upType);
                    if (linkProperties2 != null) {
                        if (Tethering.this.isTetheringIpv6Support() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6)) {
                            iface = linkProperties2.getInterfaceName();
                        } else {
                            Log.i(Tethering.TAG, "Finding IPv4 upstream interface on: " + linkProperties2);
                            RouteInfo ipv4Default = RouteInfo.selectBestRoute(linkProperties2.getAllRoutes(), Inet4Address.ANY);
                            if (ipv4Default != null) {
                                iface = ipv4Default.getInterface();
                                Log.i(Tethering.TAG, "Found interface " + ipv4Default.getInterface());
                            } else {
                                Log.i(Tethering.TAG, "No IPv4 upstream interface, giving up.");
                            }
                        }
                    }
                    if (iface != null) {
                        Network network = Tethering.this.getConnectivityManager().getNetworkForType(upType);
                        if (network == null) {
                            Log.e(Tethering.TAG, "No Network for upstream type " + upType + "!");
                        }
                        setDnsForwarders(network, linkProperties2);
                    }
                }
                if (Tethering.this.isTetheringIpv6Support() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6)) {
                    Tethering.this.mUpstreamIpv6UpType = upType;
                } else {
                    Tethering.this.mUpstreamIpv4UpType = upType;
                }
                String downIface = Tethering.this.getDownIface();
                if (Tethering.this.isMdDirectTetheringEnable() && Tethering.this.isUsb(downIface)) {
                    boolean useMdTethering = Tethering.this.shouldUseMdTethering(radioNetworkType);
                    Log.i(Tethering.TAG, "SystemProperties.set[sys.usb.rndis.direct]:" + String.valueOf(useMdTethering));
                    SystemProperties.set(Tethering.SYSTEM_PROPERTY_MD_DRT_USB_MODE_CHANG, String.valueOf(useMdTethering));
                    if (useMdTethering) {
                        try {
                            Log.d(Tethering.TAG, "mNM.addBridgeInterface(" + downIface + "," + Tethering.MD_DIRECT_TETHERING_IFACE_BR_SUB2);
                            Tethering.this.mNMService.addBridgeInterface(downIface, Tethering.MD_DIRECT_TETHERING_IFACE_BR_SUB2);
                            Log.d(Tethering.TAG, "mNM.setInterfaceConfig");
                            InterfaceConfiguration ifcg = new InterfaceConfiguration();
                            InetAddress addr = NetworkUtils.numericToInetAddress(Tethering.BR_SUB_IFACE_ADDR);
                            ifcg.setLinkAddress(new LinkAddress(addr, 24));
                            ifcg.setInterfaceUp();
                            Tethering.this.mNMService.setInterfaceConfig(Tethering.MD_DIRECT_TETHERING_IFACE_BR_SUB2, ifcg);
                        } catch (Exception e) {
                            Log.e(Tethering.TAG, "ignore add/up Exception: " + e);
                        }
                    } else {
                        try {
                            Log.d(Tethering.TAG, "mNM.delBridgeInterface(" + downIface + "," + Tethering.MD_DIRECT_TETHERING_IFACE_BR_SUB2);
                            Tethering.this.mNMService.deleteBridgeInterface(downIface, Tethering.MD_DIRECT_TETHERING_IFACE_BR_SUB2);
                        } catch (Exception e2) {
                            Log.e(Tethering.TAG, "ignore del Exception:: " + e2);
                        }
                    }
                }
                notifyTetheredOfNewUpstreamIface(iface);
            }

            protected void setDnsForwarders(Network network, LinkProperties lp) {
                String[] dnsServers = Tethering.this.mDefaultDnsServers;
                Collection<InetAddress> dnses = lp.getDnsServers();
                if (dnses != null && !dnses.isEmpty()) {
                    Collection<InetAddress> sortedDnses = new ArrayList<>();
                    for (InetAddress ia : dnses) {
                        if (ia instanceof Inet6Address) {
                            sortedDnses.add(ia);
                        }
                    }
                    for (InetAddress ia2 : dnses) {
                        if (ia2 instanceof Inet4Address) {
                            sortedDnses.add(ia2);
                        }
                    }
                    dnsServers = NetworkUtils.makeStrings(sortedDnses);
                }
                if (Tethering.VDBG) {
                    Log.d(Tethering.TAG, "Setting DNS forwarders: Network=" + network + ", dnsServers=" + Arrays.toString(dnsServers));
                }
                try {
                    Tethering.this.mNMService.setDnsForwarders(network, dnsServers);
                } catch (Exception e) {
                    Log.e(Tethering.TAG, "Setting DNS forwarders failed!");
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mSetDnsForwardersErrorState);
                }
            }

            protected void notifyTetheredOfNewUpstreamIface(String ifaceName) {
                if (Tethering.DBG) {
                    Log.i(Tethering.TAG, "[MSM_TetherModeAlive][" + TetherMasterSM.this.mName + "] Notifying tethered with upstream =" + ifaceName);
                }
                Tethering.this.mCurrentUpstreamIface = ifaceName;
                if (Tethering.this.isIpv6MasterSmOn()) {
                    if (ifaceName != null) {
                        ifaceName = ifaceName + "," + TetherMasterSM.this.mName;
                    } else {
                        ifaceName = "empty," + TetherMasterSM.this.mName;
                    }
                    Log.i(Tethering.TAG, "notifying tethered with change iface =" + ifaceName);
                }
                for (TetherInterfaceSM sm : TetherMasterSM.this.mNotifyList) {
                    sm.sendMessage(327792, ifaceName);
                }
            }

            protected void runDhcpv6PDSequence(String iface) {
                Log.i(Tethering.TAG, "runDhcpv6PDSequence:" + iface);
                if (TetherMasterSM.this.mDhcpv6PDThread == null) {
                    Log.i(Tethering.TAG, "mDhcpv6PDThread is null, creating thread");
                    TetherMasterSM.this.mPreviousDhcpv6PDIface = iface;
                    TetherMasterSM.this.mDhcpv6PDThread = new Thread(new MyRunDhcpv6PDSequence(iface));
                    TetherMasterSM.this.mDhcpv6PDThread.start();
                    return;
                }
                Log.i(Tethering.TAG, "mDhcpv6PDThread is not null");
            }

            private class MyRunDhcpv6PDSequence implements Runnable {
                private String mIface;

                public MyRunDhcpv6PDSequence(String iface) {
                    this.mIface = "";
                    this.mIface = iface;
                }

                @Override
                public void run() {
                    DhcpResults dhcpResults = new DhcpResults();
                    Log.i(Tethering.TAG, "runDhcpv6PD:" + this.mIface);
                    if (!NetworkUtils.runDhcpv6PD(this.mIface, dhcpResults)) {
                        Log.e(Tethering.TAG, "Finish runDhcpv6PD request error:" + NetworkUtils.getDhcpv6PDError());
                        TetherMasterSM.this.stopDhcpv6PDSequence();
                        TetherMasterSM.this.mDhcpv6PDThread = null;
                        TetherMasterSM.this.mPreviousDhcpv6PDIface = null;
                        return;
                    }
                    if (Tethering.this.isIpv6MasterSmOn()) {
                        Tethering.this.mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED);
                    }
                    TetherMasterSM.this.mDhcpv6PDThread = null;
                    Log.i(Tethering.TAG, "Finish runDhcpv6PD:" + this.mIface);
                }
            }
        }

        private void startListeningForSimChanges() {
            if (Tethering.DBG) {
                Log.d(Tethering.TAG, "startListeningForSimChanges");
            }
            if (this.mBroadcastReceiver != null) {
                return;
            }
            this.mBroadcastReceiver = new SimChangeBroadcastReceiver(this.mSimBcastGenerationNumber.incrementAndGet());
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SIM_STATE_CHANGED");
            Tethering.this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        }

        private void stopListeningForSimChanges() {
            if (Tethering.DBG) {
                Log.d(Tethering.TAG, "stopListeningForSimChanges");
            }
            if (this.mBroadcastReceiver == null) {
                return;
            }
            this.mSimBcastGenerationNumber.incrementAndGet();
            Tethering.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            this.mBroadcastReceiver = null;
        }

        class SimChangeBroadcastReceiver extends BroadcastReceiver {
            private final int mGenerationNumber;
            private boolean mSimNotLoadedSeen = false;

            public SimChangeBroadcastReceiver(int generationNumber) {
                this.mGenerationNumber = generationNumber;
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                if (Tethering.DBG) {
                    Log.d(Tethering.TAG, "simchange mGenerationNumber=" + this.mGenerationNumber + ", current generationNumber=" + TetherMasterSM.this.mSimBcastGenerationNumber.get());
                }
                if (this.mGenerationNumber != TetherMasterSM.this.mSimBcastGenerationNumber.get()) {
                    return;
                }
                String state = intent.getStringExtra("ss");
                Log.d(Tethering.TAG, "got Sim changed to state " + state + ", mSimNotLoadedSeen=" + this.mSimNotLoadedSeen);
                if (!this.mSimNotLoadedSeen && !"LOADED".equals(state)) {
                    this.mSimNotLoadedSeen = true;
                }
                if (!this.mSimNotLoadedSeen || !"LOADED".equals(state)) {
                    return;
                }
                this.mSimNotLoadedSeen = false;
                try {
                    if (!Tethering.this.mContext.getResources().getString(R.string.config_systemAudioIntelligence).isEmpty()) {
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
                        Iterator tetherType$iterator = tethered.iterator();
                        while (tetherType$iterator.hasNext()) {
                            int tetherType = ((Integer) tetherType$iterator.next()).intValue();
                            Intent startProvIntent = new Intent();
                            startProvIntent.putExtra("extraAddTetherType", tetherType);
                            startProvIntent.putExtra("extraRunProvision", true);
                            startProvIntent.setComponent(Tethering.TETHER_SERVICE);
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

        class InitialState extends TetherMasterUtilState {
            InitialState() {
                super();
            }

            public void enter() {
                Log.i(Tethering.TAG, "[MSM_Initial][" + TetherMasterSM.this.mName + "] enter");
            }

            @Override
            public boolean processMessage(Message message) {
                Tethering.this.maybeLogMessage(this, message.what);
                if (Tethering.DBG) {
                    Log.d(Tethering.TAG, "[MSM_Initial][" + TetherMasterSM.this.mName + "] processMessage what=" + message.what);
                }
                switch (message.what) {
                    case TetherMasterSM.CMD_TETHER_MODE_REQUESTED:
                        TetherInterfaceSM who = (TetherInterfaceSM) message.obj;
                        if (Tethering.VDBG) {
                            Log.d(Tethering.TAG, "[MSM_Initial][" + TetherMasterSM.this.mName + "] Tether Mode requested by " + who);
                        }
                        TetherMasterSM.this.mNotifyList.add(who);
                        TetherMasterSM.this.transitionTo(TetherMasterSM.this.mTetherModeAliveState);
                        break;
                    case TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED:
                        TetherInterfaceSM who2 = (TetherInterfaceSM) message.obj;
                        Log.d(Tethering.TAG, "[MSM_Initial] CMD_TETHER_MODE_UNREQUESTED ===========>");
                        if (Tethering.VDBG) {
                            Log.d(Tethering.TAG, "[MSM_Initial][" + TetherMasterSM.this.mName + "] Tether Mode unrequested by " + who2);
                        }
                        int index = TetherMasterSM.this.mNotifyList.indexOf(who2);
                        while (index != -1) {
                            TetherMasterSM.this.mNotifyList.remove(who2);
                            index = TetherMasterSM.this.mNotifyList.indexOf(who2);
                        }
                        if (who2.mUsb) {
                            Tethering.this.mUnTetherDone = true;
                            Log.i(Tethering.TAG, "[MSM_Initial] sendTetherStateChangedBroadcast");
                            Tethering.this.sendTetherStateChangedBroadcast();
                        }
                        Log.i(Tethering.TAG, "[MSM_Initial] CMD_TETHER_MODE_UNREQUESTED <===========");
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
                Log.i(Tethering.TAG, "[MSM_TetherModeAlive][" + TetherMasterSM.this.mName + "] enter");
                turnOnMasterTetherSettings();
                TetherMasterSM.this.startListeningForSimChanges();
                if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV4)) {
                    Log.d(Tethering.TAG, "mUpstreamNetworkMonitor.start()");
                    Tethering.this.mUpstreamNetworkMonitor.start();
                }
                this.mTryCell = true;
                chooseUpstreamType(this.mTryCell);
                this.mTryCell = this.mTryCell ? false : true;
            }

            public void exit() {
                Log.i(Tethering.TAG, "[MSM_TetherModeAlive][" + TetherMasterSM.this.mName + "] exit");
                if (Tethering.this.isIpv6MasterSmOn()) {
                    if (TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV4)) {
                        turnOffUpstreamMobileConnection();
                        Tethering.this.mUpstreamNetworkMonitor.stop();
                        TetherMasterSM.this.stopListeningForSimChanges();
                        Tethering.this.mUpstreamIpv4UpType = -1;
                        if (Tethering.this.isMdDirectTetheringEnable()) {
                            SystemProperties.set(Tethering.SYSTEM_PROPERTY_MD_DRT_USB_MODE_CHANG, String.valueOf(false));
                        }
                        notifyTetheredOfNewUpstreamIface(null);
                        return;
                    }
                    Tethering.this.mUpstreamIpv6UpType = -1;
                    turnOffUpstreamMobileConnection();
                    Log.i(Tethering.TAG, "[MSM_TetherModeAlive][" + TetherMasterSM.this.mName + "] do turnOffUpstreamMobileConnection only");
                    return;
                }
                turnOffUpstreamMobileConnection();
                Tethering.this.mUpstreamNetworkMonitor.stop();
                TetherMasterSM.this.stopListeningForSimChanges();
                Tethering.this.mUpstreamIpv4UpType = -1;
                if (Tethering.this.isMdDirectTetheringEnable()) {
                    SystemProperties.set(Tethering.SYSTEM_PROPERTY_MD_DRT_USB_MODE_CHANG, String.valueOf(String.valueOf(false)));
                }
                notifyTetheredOfNewUpstreamIface(null);
            }

            @Override
            public boolean processMessage(Message message) {
                Tethering.this.maybeLogMessage(this, message.what);
                if (Tethering.DBG) {
                    Log.d(Tethering.TAG, "[MSM_TetherModeAlive][" + TetherMasterSM.this.mName + "] processMessage what=" + message.what);
                }
                switch (message.what) {
                    case TetherMasterSM.CMD_TETHER_MODE_REQUESTED:
                        TetherInterfaceSM who = (TetherInterfaceSM) message.obj;
                        if (Tethering.VDBG) {
                            Log.d(Tethering.TAG, "Tether Mode requested by " + who);
                        }
                        TetherMasterSM.this.mNotifyList.add(who);
                        String ifaceName = Tethering.this.mCurrentUpstreamIface;
                        if (Tethering.this.isIpv6MasterSmOn()) {
                            if (ifaceName != null) {
                                ifaceName = ifaceName + "," + TetherMasterSM.this.mName;
                            } else {
                                ifaceName = "empty," + TetherMasterSM.this.mName;
                            }
                            Log.i(Tethering.TAG, "CMD_TETHER_MODE_REQUESTED with change iface =" + ifaceName);
                        }
                        who.sendMessage(327792, ifaceName);
                        break;
                    case TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED:
                        TetherInterfaceSM who2 = (TetherInterfaceSM) message.obj;
                        if (Tethering.VDBG) {
                            Log.d(Tethering.TAG, "Tether Mode unrequested by " + who2);
                        }
                        int index = TetherMasterSM.this.mNotifyList.indexOf(who2);
                        if (index != -1) {
                            while (index != -1) {
                                TetherMasterSM.this.mNotifyList.remove(who2);
                                index = TetherMasterSM.this.mNotifyList.indexOf(who2);
                            }
                            if (Tethering.DBG) {
                                Log.d(Tethering.TAG, "TetherModeAlive removing notifyee " + who2);
                            }
                            if (TetherMasterSM.this.mNotifyList.isEmpty()) {
                                if (Tethering.this.isIpv6TetherPdModeSupport() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6)) {
                                    TetherMasterSM.this.stopDhcpv6PDSequence();
                                }
                                Log.i(Tethering.TAG, "[MSM_TetherModeAlive] CMD_TETHER_MODE_UNREQUESTED is empty");
                                turnOffMasterTetherSettings();
                            } else if (Tethering.DBG) {
                                Log.d(Tethering.TAG, "TetherModeAlive still has " + TetherMasterSM.this.mNotifyList.size() + " live requests:");
                                for (Object o : TetherMasterSM.this.mNotifyList) {
                                    Log.d(Tethering.TAG, "  " + o);
                                }
                            }
                        } else {
                            Log.e(Tethering.TAG, "TetherModeAliveState UNREQUESTED has unknown who: " + who2);
                        }
                        if (who2.mUsb) {
                            Tethering.this.mUnTetherDone = true;
                            Log.i(Tethering.TAG, "[MSM_TetherModeAliveState] sendTetherStateChangedBroadcast");
                            Tethering.this.sendTetherStateChangedBroadcast();
                        }
                        Log.i(Tethering.TAG, "[MSM_TetherModeAlive] CMD_TETHER_MODE_UNREQUESTED <==========");
                        break;
                    case TetherMasterSM.CMD_UPSTREAM_CHANGED:
                        this.mTryCell = true;
                        chooseUpstreamType(this.mTryCell);
                        this.mTryCell = this.mTryCell ? false : true;
                        break;
                    case TetherMasterSM.CMD_RETRY_UPSTREAM:
                        chooseUpstreamType(this.mTryCell);
                        this.mTryCell = this.mTryCell ? false : true;
                        break;
                    case TetherMasterSM.EVENT_UPSTREAM_LINKPROPERTIES_CHANGED:
                        NetworkState state = (NetworkState) message.obj;
                        if (Tethering.this.mUpstreamNetworkMonitor.processLinkPropertiesChanged(state)) {
                            setDnsForwarders(state.network, state.linkProperties);
                        } else if (Tethering.this.mCurrentUpstreamIface == null) {
                            chooseUpstreamType(false);
                        }
                        break;
                    case TetherMasterSM.EVENT_UPSTREAM_LOST:
                        Tethering.this.mUpstreamNetworkMonitor.processNetworkLost((Network) message.obj);
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
                Log.i(Tethering.TAG, "[MSM_Error][" + TetherMasterSM.this.mName + "] processMessage what=" + message.what);
                switch (message.what) {
                    case TetherMasterSM.CMD_TETHER_MODE_REQUESTED:
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
                Log.e(Tethering.TAG, "[MSM_Error][" + TetherMasterSM.this.mName + "] setIpForwardingEnabled");
                notify(327787);
                TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
            }
        }

        class SetIpForwardingDisabledErrorState extends ErrorState {
            SetIpForwardingDisabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "[MSM_Error][" + TetherMasterSM.this.mName + "] setIpForwardingDisabled");
                notify(327788);
                TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
            }
        }

        class StartTetheringErrorState extends ErrorState {
            StartTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "[MSM_Error][" + TetherMasterSM.this.mName + "] startTethering");
                notify(327789);
                try {
                    if (!Tethering.this.isIpv6MasterSmOn() || Tethering.MASTERSM_IPV4.equals(TetherMasterSM.this.mName)) {
                        Tethering.this.mNMService.setIpForwardingEnabled(false);
                    } else if (Tethering.MASTERSM_IPV6.equals(TetherMasterSM.this.mName)) {
                        Tethering.this.mNMService.setIpv6ForwardingEnabled(false);
                        if (Tethering.this.isIpv6TetherPdModeSupport() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6)) {
                            TetherMasterSM.this.stopDhcpv6PDSequence();
                        }
                    }
                } catch (Exception e) {
                }
                TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
            }
        }

        class StopTetheringErrorState extends ErrorState {
            StopTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "[MSM_Error][" + TetherMasterSM.this.mName + "] stopTethering");
                notify(327790);
                try {
                    if (!Tethering.this.isIpv6MasterSmOn() || Tethering.MASTERSM_IPV4.equals(TetherMasterSM.this.mName)) {
                        Tethering.this.mNMService.setIpForwardingEnabled(false);
                    } else if (Tethering.MASTERSM_IPV6.equals(TetherMasterSM.this.mName)) {
                        Tethering.this.mNMService.setIpv6ForwardingEnabled(false);
                        if (Tethering.this.isIpv6TetherPdModeSupport() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6)) {
                            TetherMasterSM.this.stopDhcpv6PDSequence();
                        }
                    }
                } catch (Exception e) {
                }
                TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
            }
        }

        class SetDnsForwardersErrorState extends ErrorState {
            SetDnsForwardersErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "[MSM_Error][" + TetherMasterSM.this.mName + "] setDnsForwarders");
                notify(327791);
                try {
                    Tethering.this.mNMService.stopTethering();
                } catch (Exception e) {
                }
                try {
                    if (Tethering.this.isIpv6MasterSmOn()) {
                        if (Tethering.MASTERSM_IPV4.equals(TetherMasterSM.this.mName)) {
                            Tethering.this.mNMService.setIpForwardingEnabled(false);
                        }
                        if (Tethering.MASTERSM_IPV6.equals(TetherMasterSM.this.mName)) {
                            Tethering.this.mNMService.setIpv6ForwardingEnabled(false);
                            if (Tethering.this.isIpv6TetherPdModeSupport() && TetherMasterSM.this.mName.equals(Tethering.MASTERSM_IPV6)) {
                                TetherMasterSM.this.stopDhcpv6PDSequence();
                            }
                        }
                    } else {
                        Tethering.this.mNMService.setIpForwardingEnabled(false);
                    }
                } catch (Exception e2) {
                }
                TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
            }
        }

        protected void stopDhcpv6PDSequence() {
            Log.i(Tethering.TAG, "stopDhcpv6PD:" + this.mPreviousDhcpv6PDIface);
            if (this.mPreviousDhcpv6PDIface != null) {
                NetworkUtils.stopDhcpv6PD(this.mPreviousDhcpv6PDIface);
            }
            this.mPreviousDhcpv6PDIface = null;
            this.mDhcpv6PDThread = null;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ConnectivityService.Tether from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Tethering:");
        pw.increaseIndent();
        pw.print("mUpstreamIfaceTypes:");
        synchronized (this.mPublicSync) {
            for (Integer netType : this.mUpstreamIfaceTypes) {
                pw.print(" " + ConnectivityManager.getNetworkTypeName(netType.intValue()));
            }
            pw.println();
            pw.println("Tether state:");
            pw.increaseIndent();
            for (TetherInterfaceSM o : this.mIfaces.values()) {
                pw.println(" " + o);
                pw.println("  mMyUpstreamIfaceName: " + o.mMyUpstreamIfaceName);
                pw.println("  mMyUpstreamIfaceNameIpv6: " + o.mMyUpstreamIfaceNameIpv6);
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    public boolean isTetheringChangeDone() {
        return this.mIsTetheringChangeDone;
    }

    private boolean enableUdpForwardingForUsb(boolean enabled, String ipAddr) {
        Toast mToast = Toast.makeText(this.mContext, (CharSequence) null, 0);
        String[] tetherInterfaces = getTetheredIfacePairs();
        if (tetherInterfaces.length != 2) {
            Log.e(TAG, "[NS-IOT]Wrong tethering state:" + tetherInterfaces.length);
            mToast.setText("Please only enable one tethering, now:" + (tetherInterfaces.length / 2));
            mToast.show();
            return false;
        }
        if (tetherInterfaces[0] == null) {
            Log.e(TAG, "[NS-IOT]Upstream is null");
            mToast.setText("[NS-IOT]Upstream is null" + (tetherInterfaces.length / 2));
            mToast.show();
            return false;
        }
        String extInterface = tetherInterfaces[0];
        String inInterface = tetherInterfaces[1];
        if (ipAddr == null || ipAddr.length() == 0 || "unknown".equals(ipAddr)) {
            try {
                Log.e(TAG, "[NS-IOT]getUsbClient(" + inInterface);
                this.mNMService.getUsbClient(inInterface);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "[NS-IOT]getUsbClient failed!");
            }
            ipAddr = SystemProperties.get("net.rndis.client");
            if (enabled && (ipAddr == null || ipAddr.length() == 0)) {
                Log.d(TAG, "[NS-IOT]There is no HostPC address!");
                mToast.setText("There is no HostPC address");
                mToast.show();
                return false;
            }
            Log.d(TAG, "[NS-IOT]Disable or There is HostPC prefix: " + ipAddr);
        }
        mToast.setText("enableUdpForwarding(" + enabled + "," + inInterface + "," + extInterface + "," + ipAddr);
        mToast.show();
        try {
            Log.e(TAG, "[NS-IOT]enableUdpForwarding(" + enabled + "," + inInterface + "," + extInterface + "," + ipAddr);
            this.mNMService.enableUdpForwarding(enabled, inInterface, extInterface, ipAddr);
            this.mNMService.setMtu(extInterface, 1500);
            return true;
        } catch (Exception e2) {
            e2.printStackTrace();
            Log.e(TAG, "[NS-IOT]enableUdpForwarding failed!");
            mToast.setText("enableUdpForwarding failed!");
            mToast.show();
            return false;
        }
    }

    private Resources getResourcesUsingMccMnc(Context context, int mcc, int mnc) {
        try {
            if (DBG) {
                Log.i(TAG, "getResourcesUsingMccMnc: mcc = " + mcc + ", mnc = " + mnc);
            }
            Configuration configuration = new Configuration();
            configuration.mcc = mcc;
            configuration.mnc = mnc;
            Context resc = context.createConfigurationContext(configuration);
            return resc.getResources();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "getResourcesUsingMccMnc fail, return null");
            return null;
        }
    }

    private boolean isMtkTetheringEemSupport() {
        Log.d(TAG, "isMtkTetheringEemSupport: " + this.mMtkTetheringEemSupport);
        return this.mMtkTetheringEemSupport;
    }

    private boolean isBspPackage() {
        Log.d(TAG, "isBspPackage: " + this.mBspPackage);
        return this.mBspPackage;
    }

    private boolean isTetheringIpv6Support() {
        return this.mTetheringIpv6Support;
    }

    private boolean isIpv6TetherPdModeSupport() {
        Log.d(TAG, "isIpv6TetherPdModeSupport: " + this.mIpv6TetherPdModeSupport);
        if (isTetheringIpv6Support()) {
            return this.mIpv6TetherPdModeSupport;
        }
        return false;
    }

    private boolean isMdDirectTetheringSupport() {
        Log.d(TAG, "isMdDirectTetheringSupport: " + this.mMdDirectTetheringSupport);
        return this.mMdDirectTetheringSupport;
    }

    public boolean isMdDirectTetheringEnable() {
        if (isMdDirectTetheringSupport()) {
            return getMdDirectFeatureEmModeEnable();
        }
        return false;
    }

    private boolean readMdDirectFeatureEnable() {
        boolean result = SystemProperties.getBoolean(SYSTEM_PROPERTY_MD_DRT_TETHER_ENABLE, false);
        Log.d(TAG, "readMdDirectFeatureEnable:" + result);
        return result;
    }

    public boolean getMdDirectFeatureEmModeEnable() {
        Log.d(TAG, "getMdDirectFeatureEmModeEnable: " + this.mMdDirectTetheringFeatureEnable);
        return this.mMdDirectTetheringFeatureEnable;
    }

    public void setMdDirectFeatureEmModeEnable(boolean enable) {
        Log.d(TAG, "setMdDirectFeatureEnable:" + enable + " old:" + this.mMdDirectTetheringFeatureEnable);
        String value = enable ? "true" : "false";
        this.mMdDirectTetheringFeatureEnable = enable;
        SystemProperties.set(SYSTEM_PROPERTY_MD_DRT_TETHER_ENABLE, value);
    }

    private boolean handleUsbDisconnect() {
        Log.d(TAG, "handleUsbDisconnect");
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        InetAddress addr = NetworkUtils.numericToInetAddress(BR_SUB_IFACE_ADDR);
        ifcg.setLinkAddress(new LinkAddress(addr, 24));
        ifcg.setInterfaceDown();
        try {
            this.mNMService.setInterfaceConfig(MD_DIRECT_TETHERING_IFACE_BR_SUB2, ifcg);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handleUsbDisconnect: " + e);
            return false;
        }
    }

    private boolean shouldUseMdTethering(int radioNetworkType) {
        if ((isMobileUpstream(this.mUpstreamIpv4UpType) || isMobileUpstream(this.mUpstreamIpv6UpType)) && isGsm(radioNetworkType)) {
            return true;
        }
        return false;
    }

    public static boolean isGsm(int radioNetworkType) {
        Log.d(TAG, "isGsm :" + radioNetworkType);
        return radioNetworkType == 1 || radioNetworkType == 2 || radioNetworkType == 3 || radioNetworkType == 8 || radioNetworkType == 9 || radioNetworkType == 10 || radioNetworkType == 13 || radioNetworkType == 15 || radioNetworkType == 16 || radioNetworkType == 17 || radioNetworkType == 18;
    }

    public boolean isMobileUpstream(int upType) {
        if (upType == 0 || 4 == upType || 5 == upType) {
            Log.d(TAG, "isMobileUpstream: true");
            return true;
        }
        Log.d(TAG, "isMobileUpstream: false");
        return false;
    }

    private String getDownIface() {
        Log.d(TAG, "getDownIface()");
        synchronized (this.mPublicSync) {
            for (Object iface : this.mIfaces.keySet()) {
                TetherInterfaceSM sm = this.mIfaces.get(iface);
                if (sm != null && sm.isTethered()) {
                    Log.d(TAG, "sm.mIfaceName:" + sm.mIfaceName + " sm.isTethered():" + sm.isTethered());
                    return sm.mIfaceName;
                }
            }
            return null;
        }
    }

    private boolean isIpMatchSM(String smName, List<InetAddress> list) {
        if (smName == null || MASTERSM_IPV4.equals(smName)) {
            for (InetAddress ia : list) {
                if (ia instanceof Inet4Address) {
                    Log.d(TAG, "ipAddress " + ia + " match " + smName);
                    return true;
                }
            }
            Log.d(TAG, "ipAddress " + list + " not match " + smName);
            return false;
        }
        if (!MASTERSM_IPV6.equals(smName)) {
            return false;
        }
        for (InetAddress ia2 : list) {
            if (ia2 instanceof Inet6Address) {
                Log.d(TAG, "ipAddress " + ia2 + " match " + smName);
                return true;
            }
        }
        Log.d(TAG, "ipAddress " + list + " not match " + smName);
        return false;
    }
}
