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
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.util.InterfaceSet;
import android.net.util.PrefixUtils;
import android.net.util.SharedLog;
import android.net.util.VersionedBroadcastListener;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.ConnectivityService;
import com.android.server.LocalServices;
import com.android.server.connectivity.tethering.IControlsTethering;
import com.android.server.connectivity.tethering.IPv6TetheringCoordinator;
import com.android.server.connectivity.tethering.OffloadController;
import com.android.server.connectivity.tethering.SimChangeListener;
import com.android.server.connectivity.tethering.TetherInterfaceStateMachine;
import com.android.server.connectivity.tethering.TetheringConfiguration;
import com.android.server.connectivity.tethering.TetheringDependencies;
import com.android.server.connectivity.tethering.TetheringInterfaceUtils;
import com.android.server.connectivity.tethering.UpstreamNetworkMonitor;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.BaseNetworkObserver;
import dalvik.system.PathClassLoader;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class Tethering extends BaseNetworkObserver {
    private static final boolean DBG = true;
    protected static final String DISABLE_PROVISIONING_SYSPROP_KEY = "net.tethering.noprovisioning";
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tag.tel_dbg";
    private static final String TAG = Tethering.class.getSimpleName();
    private static final ComponentName TETHER_SERVICE;
    private static final boolean VDBG;
    private static Object mMtkTethering;
    private static final Class[] messageClasses;
    private static final SparseArray<String> sMagicDecoderRing;
    private static PathClassLoader sPcLoader;
    private final VersionedBroadcastListener mCarrierConfigChange;
    private volatile TetheringConfiguration mConfig;
    private final Context mContext;
    private InterfaceSet mCurrentUpstreamIfaceSet;
    private final TetheringDependencies mDeps;
    private final HashSet<TetherInterfaceStateMachine> mForwardedDownstreams;
    private int mLastNotificationId;
    private final SharedLog mLog = new SharedLog(TAG);
    private final Looper mLooper;
    private final INetworkManagementService mNMService;
    private final OffloadController mOffloadController;
    private final INetworkPolicyManager mPolicyManager;
    private final Object mPublicSync;
    private boolean mRndisEnabled;
    private final SimChangeListener mSimChange;
    private final BroadcastReceiver mStateReceiver;
    private final INetworkStatsService mStatsService;
    private final MockableSystemProperties mSystemProperties;
    private final StateMachine mTetherMasterSM;
    private final ArrayMap<String, TetherState> mTetherStates;
    private Notification.Builder mTetheredNotificationBuilder;
    private final UpstreamNetworkMonitor mUpstreamNetworkMonitor;
    private boolean mWifiTetherRequested;

    static {
        VDBG = Build.IS_ENG || SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
        messageClasses = new Class[]{Tethering.class, TetherMasterSM.class, TetherInterfaceStateMachine.class};
        sMagicDecoderRing = MessageUtils.findMessageNames(messageClasses);
        TETHER_SERVICE = ComponentName.unflattenFromString(Resources.getSystem().getString(R.string.app_info));
    }

    private static class TetherState {
        public final TetherInterfaceStateMachine stateMachine;
        public int lastState = 1;
        public int lastError = 0;

        public TetherState(TetherInterfaceStateMachine tetherInterfaceStateMachine) {
            this.stateMachine = tetherInterfaceStateMachine;
        }

        public boolean isCurrentlyServing() {
            switch (this.lastState) {
                case 2:
                case 3:
                    return true;
                default:
                    return false;
            }
        }
    }

    public Tethering(Context context, INetworkManagementService iNetworkManagementService, INetworkStatsService iNetworkStatsService, INetworkPolicyManager iNetworkPolicyManager, Looper looper, MockableSystemProperties mockableSystemProperties, TetheringDependencies tetheringDependencies) {
        this.mLog.mark("constructed");
        this.mContext = context;
        this.mNMService = iNetworkManagementService;
        this.mStatsService = iNetworkStatsService;
        this.mPolicyManager = iNetworkPolicyManager;
        this.mLooper = looper;
        this.mSystemProperties = mockableSystemProperties;
        this.mDeps = tetheringDependencies;
        this.mPublicSync = new Object();
        this.mTetherStates = new ArrayMap<>();
        this.mTetherMasterSM = new TetherMasterSM("TetherMaster", this.mLooper, tetheringDependencies);
        this.mTetherMasterSM.start();
        Handler handler = this.mTetherMasterSM.getHandler();
        this.mOffloadController = new OffloadController(handler, this.mDeps.getOffloadHardwareInterface(handler, this.mLog), this.mContext.getContentResolver(), this.mNMService, this.mLog);
        this.mUpstreamNetworkMonitor = tetheringDependencies.getUpstreamNetworkMonitor(this.mContext, this.mTetherMasterSM, this.mLog, 327685);
        this.mForwardedDownstreams = new HashSet<>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        this.mCarrierConfigChange = new VersionedBroadcastListener("CarrierConfigChangeListener", this.mContext, handler, intentFilter, new Consumer() {
            @Override
            public final void accept(Object obj) {
                Tethering.lambda$new$0(this.f$0, (Intent) obj);
            }
        });
        this.mSimChange = new SimChangeListener(this.mContext, handler, new Runnable() {
            @Override
            public final void run() {
                this.f$0.mLog.log("OBSERVED SIM card change");
            }
        });
        this.mStateReceiver = new StateReceiver();
        updateConfiguration();
        startStateMachineUpdaters();
    }

    public static void lambda$new$0(Tethering tethering, Intent intent) {
        tethering.mLog.log("OBSERVED carrier config change");
        tethering.updateConfiguration();
        tethering.reevaluateSimCardProvisioning();
    }

    private void startStateMachineUpdaters() {
        this.mCarrierConfigChange.startListening();
        Handler handler = this.mTetherMasterSM.getHandler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiver(this.mStateReceiver, intentFilter, null, handler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.MEDIA_SHARED");
        intentFilter2.addAction("android.intent.action.MEDIA_UNSHARED");
        intentFilter2.addDataScheme("file");
        this.mContext.registerReceiver(this.mStateReceiver, intentFilter2, null, handler);
        UserManagerInternal userManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        if (userManagerInternal != null) {
            userManagerInternal.addUserRestrictionsListener(new TetheringUserRestrictionListener(this));
        }
        mMtkTethering = getMtkTethering(this.mContext);
    }

    private WifiManager getWifiManager() {
        return (WifiManager) this.mContext.getSystemService("wifi");
    }

    private void updateConfiguration() {
        this.mConfig = new TetheringConfiguration(this.mContext, this.mLog);
        this.mUpstreamNetworkMonitor.updateMobileRequiresDun(this.mConfig.isDunRequired);
    }

    private void maybeUpdateConfiguration() {
        if (TetheringConfiguration.checkDunRequired(this.mContext) == this.mConfig.dunCheck) {
            return;
        }
        updateConfiguration();
    }

    public void interfaceStatusChanged(String str, boolean z) {
        if (VDBG) {
            Log.d(TAG, "interfaceStatusChanged " + str + ", " + z);
        }
        synchronized (this.mPublicSync) {
            try {
                if (z) {
                    maybeTrackNewInterfaceLocked(str);
                } else if (ifaceNameToType(str) == 2) {
                    stopTrackingInterfaceLocked(str);
                } else if (VDBG) {
                    Log.d(TAG, "ignore interface down for " + str);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void interfaceLinkStateChanged(String str, boolean z) {
        interfaceStatusChanged(str, z);
    }

    private int ifaceNameToType(String str) {
        TetheringConfiguration tetheringConfiguration = this.mConfig;
        if (tetheringConfiguration.isWifi(str)) {
            return 0;
        }
        if (tetheringConfiguration.isUsb(str)) {
            return 1;
        }
        if (tetheringConfiguration.isBluetooth(str)) {
            return 2;
        }
        return -1;
    }

    public void interfaceAdded(String str) {
        if (VDBG) {
            Log.d(TAG, "interfaceAdded " + str);
        }
        synchronized (this.mPublicSync) {
            maybeTrackNewInterfaceLocked(str);
        }
    }

    public void interfaceRemoved(String str) {
        if (VDBG) {
            Log.d(TAG, "interfaceRemoved " + str);
        }
        synchronized (this.mPublicSync) {
            stopTrackingInterfaceLocked(str);
        }
    }

    public void startTethering(int i, ResultReceiver resultReceiver, boolean z) {
        if (!isTetherProvisioningRequired()) {
            enableTetheringInternal(i, true, resultReceiver);
        } else if (z) {
            runUiTetherProvisioningAndEnable(i, resultReceiver);
        } else {
            runSilentTetherProvisioningAndEnable(i, resultReceiver);
        }
    }

    public void stopTethering(int i) {
        enableTetheringInternal(i, false, null);
        if (isTetherProvisioningRequired()) {
            cancelTetherProvisioningRechecks(i);
        }
    }

    @VisibleForTesting
    protected boolean isTetherProvisioningRequired() {
        TetheringConfiguration tetheringConfiguration = this.mConfig;
        return (this.mSystemProperties.getBoolean(DISABLE_PROVISIONING_SYSPROP_KEY, false) || tetheringConfiguration.provisioningApp.length == 0 || carrierConfigAffirmsEntitlementCheckNotRequired() || tetheringConfiguration.provisioningApp.length != 2) ? false : true;
    }

    private boolean carrierConfigAffirmsEntitlementCheckNotRequired() {
        PersistableBundle config;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null || (config = carrierConfigManager.getConfig()) == null) {
            return false;
        }
        return !config.getBoolean("require_entitlement_checks_bool");
    }

    private void enableTetheringInternal(int i, boolean z, ResultReceiver resultReceiver) {
        boolean z2;
        if (!z || !isTetherProvisioningRequired()) {
            z2 = false;
        } else {
            z2 = true;
        }
        switch (i) {
            case 0:
                int wifiTethering = setWifiTethering(z);
                if (z2 && wifiTethering == 0) {
                    scheduleProvisioningRechecks(i);
                }
                sendTetherResult(resultReceiver, wifiTethering);
                break;
            case 1:
                int usbTethering = setUsbTethering(z);
                if (z2 && usbTethering == 0) {
                    scheduleProvisioningRechecks(i);
                }
                sendTetherResult(resultReceiver, usbTethering);
                break;
            case 2:
                setBluetoothTethering(z, resultReceiver);
                break;
            default:
                Log.w(TAG, "Invalid tether type.");
                sendTetherResult(resultReceiver, 1);
                break;
        }
    }

    private void sendTetherResult(ResultReceiver resultReceiver, int i) {
        if (resultReceiver != null) {
            resultReceiver.send(i, null);
        }
    }

    private int setWifiTethering(boolean z) {
        int i;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPublicSync) {
                this.mWifiTetherRequested = z;
                WifiManager wifiManager = getWifiManager();
                if ((z && wifiManager.startSoftAp(null)) || (!z && wifiManager.stopSoftAp())) {
                    i = 0;
                } else {
                    i = 5;
                }
            }
            return i;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void setBluetoothTethering(final boolean z, final ResultReceiver resultReceiver) {
        final BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null || !defaultAdapter.isEnabled()) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Tried to enable bluetooth tethering with null or disabled adapter. null: ");
            sb.append(defaultAdapter == null);
            Log.w(str, sb.toString());
            sendTetherResult(resultReceiver, 2);
            return;
        }
        defaultAdapter.getProfileProxy(this.mContext, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceDisconnected(int i) {
            }

            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                int i2;
                BluetoothPan bluetoothPan = (BluetoothPan) bluetoothProfile;
                bluetoothPan.setBluetoothTethering(z);
                if (bluetoothPan.isTetheringOn() == z) {
                    i2 = 0;
                } else {
                    i2 = 5;
                }
                Tethering.this.sendTetherResult(resultReceiver, i2);
                if (z && Tethering.this.isTetherProvisioningRequired()) {
                    Tethering.this.scheduleProvisioningRechecks(2);
                }
                defaultAdapter.closeProfileProxy(5, bluetoothProfile);
            }
        }, 5);
    }

    private void runUiTetherProvisioningAndEnable(int i, ResultReceiver resultReceiver) {
        sendUiTetherProvisionIntent(i, getProxyReceiver(i, resultReceiver));
    }

    private void sendUiTetherProvisionIntent(int i, ResultReceiver resultReceiver) {
        if (BenesseExtension.getDchaState() == 0) {
            return;
        }
        Intent intent = new Intent("android.settings.TETHER_PROVISIONING_UI");
        intent.putExtra("extraAddTetherType", i);
        intent.putExtra("extraProvisionCallback", resultReceiver);
        intent.addFlags(268435456);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private ResultReceiver getProxyReceiver(final int i, final ResultReceiver resultReceiver) {
        ResultReceiver resultReceiver2 = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int i2, Bundle bundle) {
                if (i2 == 0) {
                    Tethering.this.enableTetheringInternal(i, true, resultReceiver);
                } else {
                    Tethering.this.sendTetherResult(resultReceiver, i2);
                }
            }
        };
        Parcel parcelObtain = Parcel.obtain();
        resultReceiver2.writeToParcel(parcelObtain, 0);
        parcelObtain.setDataPosition(0);
        ResultReceiver resultReceiver3 = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcelObtain);
        parcelObtain.recycle();
        return resultReceiver3;
    }

    private void scheduleProvisioningRechecks(int i) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", i);
        intent.putExtra("extraSetAlarm", true);
        intent.setComponent(TETHER_SERVICE);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void runSilentTetherProvisioningAndEnable(int i, ResultReceiver resultReceiver) {
        sendSilentTetherProvisionIntent(i, getProxyReceiver(i, resultReceiver));
    }

    private void sendSilentTetherProvisionIntent(int i, ResultReceiver resultReceiver) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", i);
        intent.putExtra("extraRunProvision", true);
        intent.putExtra("extraProvisionCallback", resultReceiver);
        intent.setComponent(TETHER_SERVICE);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void cancelTetherProvisioningRechecks(int i) {
        if (this.mDeps.isTetheringSupported()) {
            Intent intent = new Intent();
            intent.putExtra("extraRemTetherType", i);
            intent.setComponent(TETHER_SERVICE);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void startProvisionIntent(int i) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", i);
        intent.putExtra("extraRunProvision", true);
        intent.setComponent(TETHER_SERVICE);
        this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    public int tether(String str) {
        return tether(str, 2);
    }

    private int tether(String str, int i) {
        Log.d(TAG, "Tethering " + str);
        synchronized (this.mPublicSync) {
            TetherState tetherState = this.mTetherStates.get(str);
            if (tetherState != null) {
                if (tetherState.lastState != 1) {
                    Log.e(TAG, "Tried to Tether an unavailable iface: " + str + ", ignoring");
                    return 4;
                }
                tetherState.stateMachine.sendMessage(TetherInterfaceStateMachine.CMD_TETHER_REQUESTED, i);
                return 0;
            }
            Log.e(TAG, "Tried to Tether an unknown iface: " + str + ", ignoring");
            return 1;
        }
    }

    public int untether(String str) {
        Log.d(TAG, "Untethering " + str);
        synchronized (this.mPublicSync) {
            TetherState tetherState = this.mTetherStates.get(str);
            if (tetherState == null) {
                Log.e(TAG, "Tried to Untether an unknown iface :" + str + ", ignoring");
                return 1;
            }
            if (!tetherState.isCurrentlyServing()) {
                Log.e(TAG, "Tried to untether an inactive iface :" + str + ", ignoring");
                return 4;
            }
            tetherState.stateMachine.sendMessage(TetherInterfaceStateMachine.CMD_TETHER_UNREQUESTED);
            return 0;
        }
    }

    public void untetherAll() {
        stopTethering(0);
        stopTethering(1);
        stopTethering(2);
    }

    public int getLastTetherError(String str) {
        synchronized (this.mPublicSync) {
            TetherState tetherState = this.mTetherStates.get(str);
            if (tetherState == null) {
                Log.e(TAG, "Tried to getLastTetherError on an unknown iface :" + str + ", ignoring");
                return 1;
            }
            return tetherState.lastError;
        }
    }

    private void sendTetherStateChangedBroadcast() {
        boolean z;
        boolean z2;
        boolean z3;
        if (this.mDeps.isTetheringSupported()) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<String> arrayList2 = new ArrayList<>();
            ArrayList<String> arrayList3 = new ArrayList<>();
            ArrayList<String> arrayList4 = new ArrayList<>();
            TetheringConfiguration tetheringConfiguration = this.mConfig;
            synchronized (this.mPublicSync) {
                z = false;
                z2 = false;
                z3 = false;
                for (int i = 0; i < this.mTetherStates.size(); i++) {
                    TetherState tetherStateValueAt = this.mTetherStates.valueAt(i);
                    String strKeyAt = this.mTetherStates.keyAt(i);
                    if (tetherStateValueAt.lastError != 0) {
                        arrayList4.add(strKeyAt);
                    } else if (tetherStateValueAt.lastState == 1) {
                        arrayList.add(strKeyAt);
                    } else if (tetherStateValueAt.lastState == 3) {
                        arrayList3.add(strKeyAt);
                    } else if (tetherStateValueAt.lastState == 2) {
                        if (!tetheringConfiguration.isUsb(strKeyAt)) {
                            if (!tetheringConfiguration.isWifi(strKeyAt)) {
                                if (tetheringConfiguration.isBluetooth(strKeyAt)) {
                                    z3 = true;
                                }
                            } else {
                                z2 = true;
                            }
                        } else {
                            z = true;
                        }
                        arrayList2.add(strKeyAt);
                    }
                }
            }
            Intent intent = new Intent("android.net.conn.TETHER_STATE_CHANGED");
            intent.addFlags(603979776);
            intent.putStringArrayListExtra("availableArray", arrayList);
            intent.putStringArrayListExtra("localOnlyArray", arrayList3);
            intent.putStringArrayListExtra("tetherArray", arrayList2);
            intent.putStringArrayListExtra("erroredArray", arrayList4);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            Log.d(TAG, String.format("sendTetherStateChangedBroadcast %s=[%s] %s=[%s] %s=[%s] %s=[%s]", "avail", TextUtils.join(",", arrayList), "local_only", TextUtils.join(",", arrayList3), "tether", TextUtils.join(",", arrayList2), "error", TextUtils.join(",", arrayList4)));
            if (z) {
                if (!z2 && !z3) {
                    showTetheredNotification(15);
                    return;
                } else {
                    showTetheredNotification(14);
                    return;
                }
            }
            if (z2) {
                if (z3) {
                    showTetheredNotification(14);
                    return;
                } else {
                    clearTetheredNotification();
                    return;
                }
            }
            if (z3) {
                showTetheredNotification(16);
            } else {
                clearTetheredNotification();
            }
        }
    }

    private void showTetheredNotification(int i) {
        showTetheredNotification(i, true);
    }

    @VisibleForTesting
    protected void showTetheredNotification(int i, boolean z) {
        int i2;
        CharSequence text;
        CharSequence text2;
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager == null) {
            return;
        }
        switch (i) {
            case 15:
                i2 = R.drawable.pointer_vertical_text_large_icon;
                break;
            case 16:
                i2 = R.drawable.pointer_vertical_text_icon;
                break;
            default:
                i2 = R.drawable.pointer_vertical_text_large;
                break;
        }
        if (this.mLastNotificationId != 0) {
            if (this.mLastNotificationId == i2) {
                return;
            }
            notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
            this.mLastNotificationId = 0;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setFlags(1073741824);
        PendingIntent activityAsUser = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
        if (BenesseExtension.getDchaState() != 0) {
            activityAsUser = null;
        }
        Resources system = Resources.getSystem();
        if (z) {
            text = system.getText(R.string.mediasize_na_quarto);
            text2 = system.getText(R.string.mediasize_na_monarch);
        } else {
            text = system.getText(R.string.bluetooth_airplane_mode_toast);
            text2 = system.getText(R.string.bluetooth_a2dp_audio_route_name);
        }
        if (this.mTetheredNotificationBuilder == null) {
            this.mTetheredNotificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS);
            this.mTetheredNotificationBuilder.setWhen(0L).setOngoing(true).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setVisibility(1).setCategory("status");
        }
        this.mTetheredNotificationBuilder.setSmallIcon(i2).setContentTitle(text).setContentText(text2).setContentIntent(activityAsUser);
        this.mLastNotificationId = i;
        notificationManager.notifyAsUser(null, this.mLastNotificationId, this.mTetheredNotificationBuilder.buildInto(new Notification()), UserHandle.ALL);
    }

    @VisibleForTesting
    protected void clearTetheredNotification() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null && this.mLastNotificationId != 0) {
            notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
            this.mLastNotificationId = 0;
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        private StateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals("android.hardware.usb.action.USB_STATE")) {
                handleUsbAction(intent);
                return;
            }
            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                handleConnectivityAction(intent);
                return;
            }
            if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                handleWifiApAction(intent);
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                Tethering.this.mLog.log("OBSERVED configuration changed");
                Tethering.this.updateConfiguration();
            }
        }

        private void handleConnectivityAction(Intent intent) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            if (networkInfo != null && networkInfo.getDetailedState() != NetworkInfo.DetailedState.FAILED) {
                if (Tethering.VDBG) {
                    Log.d(Tethering.TAG, "Tethering got CONNECTIVITY_ACTION: " + networkInfo.toString());
                }
                Tethering.this.mTetherMasterSM.sendMessage(327683);
            }
        }

        private void handleUsbAction(Intent intent) {
            boolean z = false;
            boolean booleanExtra = intent.getBooleanExtra("connected", false);
            boolean booleanExtra2 = intent.getBooleanExtra("configured", false);
            boolean booleanExtra3 = intent.getBooleanExtra("rndis", false);
            Tethering.this.mLog.log(String.format("USB bcast connected:%s configured:%s rndis:%s", Boolean.valueOf(booleanExtra), Boolean.valueOf(booleanExtra2), Boolean.valueOf(booleanExtra3)));
            synchronized (Tethering.this.mPublicSync) {
                if (!booleanExtra) {
                    try {
                        if (Tethering.this.mRndisEnabled) {
                            Tethering.this.tetherMatchingInterfaces(1, 1);
                        } else if (booleanExtra2 && booleanExtra3) {
                            Tethering.this.tetherMatchingInterfaces(2, 1);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                Tethering tethering = Tethering.this;
                if (booleanExtra2 && booleanExtra3) {
                    z = true;
                }
                tethering.mRndisEnabled = z;
            }
        }

        private void handleWifiApAction(Intent intent) {
            int intExtra = intent.getIntExtra("wifi_state", 11);
            String stringExtra = intent.getStringExtra("wifi_ap_interface_name");
            int intExtra2 = intent.getIntExtra("wifi_ap_mode", -1);
            synchronized (Tethering.this.mPublicSync) {
                switch (intExtra) {
                    case 12:
                        break;
                    case 13:
                        Tethering.this.enableWifiIpServingLocked(stringExtra, intExtra2);
                        break;
                    default:
                        Tethering.this.disableWifiIpServingLocked(stringExtra, intExtra);
                        break;
                }
            }
        }
    }

    @VisibleForTesting
    protected static class TetheringUserRestrictionListener implements UserManagerInternal.UserRestrictionsListener {
        private final Tethering mWrapper;

        public TetheringUserRestrictionListener(Tethering tethering) {
            this.mWrapper = tethering;
        }

        public void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
            boolean z = bundle.getBoolean("no_config_tethering");
            boolean z2 = bundle2.getBoolean("no_config_tethering");
            boolean z3 = true;
            if (!(z != z2)) {
                return;
            }
            this.mWrapper.clearTetheredNotification();
            if (this.mWrapper.getTetheredIfaces().length == 0) {
                z3 = false;
            }
            if (z && z3) {
                this.mWrapper.showTetheredNotification(R.drawable.pointer_vertical_text_large, false);
                this.mWrapper.untetherAll();
            }
        }
    }

    private void disableWifiIpServingLocked(String str, int i) {
        TetherState tetherState;
        this.mLog.log("Canceling WiFi tethering request - AP_STATE=" + i);
        this.mWifiTetherRequested = false;
        if (!TextUtils.isEmpty(str) && (tetherState = this.mTetherStates.get(str)) != null) {
            tetherState.stateMachine.unwanted();
            return;
        }
        for (int i2 = 0; i2 < this.mTetherStates.size(); i2++) {
            TetherInterfaceStateMachine tetherInterfaceStateMachine = this.mTetherStates.valueAt(i2).stateMachine;
            if (tetherInterfaceStateMachine.interfaceType() == 0) {
                tetherInterfaceStateMachine.unwanted();
                return;
            }
        }
        SharedLog sharedLog = this.mLog;
        StringBuilder sb = new StringBuilder();
        sb.append("Error disabling Wi-Fi IP serving; ");
        sb.append(TextUtils.isEmpty(str) ? "no interface name specified" : "specified interface: " + str);
        sharedLog.log(sb.toString());
    }

    private void enableWifiIpServingLocked(String str, int i) {
        int i2;
        switch (i) {
            case 1:
                i2 = 2;
                break;
            case 2:
                i2 = 3;
                break;
            default:
                this.mLog.e("Cannot enable IP serving in unknown WiFi mode: " + i);
                return;
        }
        if (!TextUtils.isEmpty(str)) {
            maybeTrackNewInterfaceLocked(str, 0);
            changeInterfaceState(str, i2);
        } else {
            this.mLog.e(String.format("Cannot enable IP serving in mode %s on missing interface name", Integer.valueOf(i2)));
        }
    }

    private void tetherMatchingInterfaces(int i, int i2) {
        if (VDBG) {
            Log.d(TAG, "tetherMatchingInterfaces(" + i + ", " + i2 + ")");
        }
        try {
            String[] strArrListInterfaces = this.mNMService.listInterfaces();
            String str = null;
            if (strArrListInterfaces != null) {
                int length = strArrListInterfaces.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length) {
                        break;
                    }
                    String str2 = strArrListInterfaces[i3];
                    if (ifaceNameToType(str2) != i2) {
                        i3++;
                    } else {
                        str = str2;
                        break;
                    }
                }
            }
            if (str == null) {
                Log.e(TAG, "could not find iface of type " + i2);
                return;
            }
            changeInterfaceState(str, i);
        } catch (Exception e) {
            Log.e(TAG, "Error listing Interfaces", e);
        }
    }

    private void changeInterfaceState(String str, int i) {
        int iUntether;
        switch (i) {
            case 0:
            case 1:
                iUntether = untether(str);
                break;
            case 2:
            case 3:
                iUntether = tether(str, i);
                break;
            default:
                Log.wtf(TAG, "Unknown interface state: " + i);
                return;
        }
        if (iUntether != 0) {
            Log.e(TAG, "unable start or stop tethering on iface " + str);
        }
    }

    public TetheringConfiguration getTetheringConfiguration() {
        return this.mConfig;
    }

    public boolean hasTetherableConfiguration() {
        TetheringConfiguration tetheringConfiguration = this.mConfig;
        return (tetheringConfiguration.tetherableUsbRegexs.length != 0 || tetheringConfiguration.tetherableWifiRegexs.length != 0 || tetheringConfiguration.tetherableBluetoothRegexs.length != 0) && (tetheringConfiguration.preferredUpstreamIfaceTypes.isEmpty() ^ true);
    }

    public String[] getTetherableUsbRegexs() {
        return copy(this.mConfig.tetherableUsbRegexs);
    }

    public String[] getTetherableWifiRegexs() {
        return copy(this.mConfig.tetherableWifiRegexs);
    }

    public String[] getTetherableBluetoothRegexs() {
        return copy(this.mConfig.tetherableBluetoothRegexs);
    }

    public int setUsbTethering(boolean z) {
        if (VDBG) {
            Log.d(TAG, "setUsbTethering(" + z + ")");
        }
        UsbManager usbManager = (UsbManager) this.mContext.getSystemService("usb");
        synchronized (this.mPublicSync) {
            usbManager.setCurrentFunctions(z ? 32L : 0L);
        }
        return 0;
    }

    public String[] getTetheredIfaces() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mPublicSync) {
            for (int i = 0; i < this.mTetherStates.size(); i++) {
                if (this.mTetherStates.valueAt(i).lastState == 2) {
                    arrayList.add(this.mTetherStates.keyAt(i));
                }
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public String[] getTetherableIfaces() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mPublicSync) {
            for (int i = 0; i < this.mTetherStates.size(); i++) {
                if (this.mTetherStates.valueAt(i).lastState == 1) {
                    arrayList.add(this.mTetherStates.keyAt(i));
                }
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public String[] getTetheredDhcpRanges() {
        return this.mConfig.dhcpRanges;
    }

    public String[] getErroredIfaces() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mPublicSync) {
            for (int i = 0; i < this.mTetherStates.size(); i++) {
                if (this.mTetherStates.valueAt(i).lastError != 0) {
                    arrayList.add(this.mTetherStates.keyAt(i));
                }
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    private void logMessage(State state, int i) {
        this.mLog.log(state.getName() + " got " + sMagicDecoderRing.get(i, Integer.toString(i)));
    }

    private boolean upstreamWanted() {
        boolean z;
        if (!this.mForwardedDownstreams.isEmpty()) {
            return true;
        }
        synchronized (this.mPublicSync) {
            z = this.mWifiTetherRequested;
        }
        return z;
    }

    private boolean pertainsToCurrentUpstream(NetworkState networkState) {
        if (networkState != null && networkState.linkProperties != null && this.mCurrentUpstreamIfaceSet != null) {
            Iterator it = networkState.linkProperties.getAllInterfaceNames().iterator();
            while (it.hasNext()) {
                if (this.mCurrentUpstreamIfaceSet.ifnames.contains((String) it.next())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void reevaluateSimCardProvisioning() {
        int iIfaceNameToType;
        if (this.mConfig.hasMobileHotspotProvisionApp() && !carrierConfigAffirmsEntitlementCheckNotRequired()) {
            ArrayList arrayList = new ArrayList();
            synchronized (this.mPublicSync) {
                for (int i = 0; i < this.mTetherStates.size(); i++) {
                    if (this.mTetherStates.valueAt(i).lastState == 2 && (iIfaceNameToType = ifaceNameToType(this.mTetherStates.keyAt(i))) != -1) {
                        arrayList.add(Integer.valueOf(iIfaceNameToType));
                    }
                }
            }
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                startProvisionIntent(((Integer) it.next()).intValue());
            }
        }
    }

    class TetherMasterSM extends StateMachine {
        private static final int BASE_MASTER = 327680;
        static final int CMD_CLEAR_ERROR = 327686;
        static final int CMD_RETRY_UPSTREAM = 327684;
        static final int CMD_UPSTREAM_CHANGED = 327683;
        static final int EVENT_IFACE_SERVING_STATE_ACTIVE = 327681;
        static final int EVENT_IFACE_SERVING_STATE_INACTIVE = 327682;
        static final int EVENT_IFACE_UPDATE_LINKPROPERTIES = 327687;
        static final int EVENT_UPSTREAM_CALLBACK = 327685;
        private static final int UPSTREAM_SETTLE_TIME_MS = 10000;
        private IPv6TetheringCoordinator mIPv6TetheringCoordinator;
        private final State mInitialState;
        private final ArrayList<TetherInterfaceStateMachine> mNotifyList;
        private final OffloadWrapper mOffload;
        private final State mSetDnsForwardersErrorState;
        private final State mSetIpForwardingDisabledErrorState;
        private final State mSetIpForwardingEnabledErrorState;
        private final State mStartTetheringErrorState;
        private final State mStopTetheringErrorState;
        private final State mTetherModeAliveState;

        TetherMasterSM(String str, Looper looper, TetheringDependencies tetheringDependencies) {
            super(str, looper);
            this.mInitialState = new InitialState();
            this.mTetherModeAliveState = new TetherModeAliveState();
            this.mSetIpForwardingEnabledErrorState = new SetIpForwardingEnabledErrorState();
            this.mSetIpForwardingDisabledErrorState = new SetIpForwardingDisabledErrorState();
            this.mStartTetheringErrorState = new StartTetheringErrorState();
            this.mStopTetheringErrorState = new StopTetheringErrorState();
            this.mSetDnsForwardersErrorState = new SetDnsForwardersErrorState();
            addState(this.mInitialState);
            addState(this.mTetherModeAliveState);
            addState(this.mSetIpForwardingEnabledErrorState);
            addState(this.mSetIpForwardingDisabledErrorState);
            addState(this.mStartTetheringErrorState);
            addState(this.mStopTetheringErrorState);
            addState(this.mSetDnsForwardersErrorState);
            this.mNotifyList = new ArrayList<>();
            this.mIPv6TetheringCoordinator = Tethering.this.getMtkIPv6TetheringCoordinator(this.mNotifyList, Tethering.this.mLog);
            if (this.mIPv6TetheringCoordinator == null) {
                this.mIPv6TetheringCoordinator = tetheringDependencies.getIPv6TetheringCoordinator(this.mNotifyList, Tethering.this.mLog);
            }
            this.mOffload = new OffloadWrapper();
            setInitialState(this.mInitialState);
        }

        class InitialState extends State {
            InitialState() {
            }

            public boolean processMessage(Message message) {
                Tethering.this.logMessage(this, message.what);
                int i = message.what;
                if (i != TetherMasterSM.EVENT_IFACE_UPDATE_LINKPROPERTIES) {
                    switch (i) {
                        case TetherMasterSM.EVENT_IFACE_SERVING_STATE_ACTIVE:
                            TetherInterfaceStateMachine tetherInterfaceStateMachine = (TetherInterfaceStateMachine) message.obj;
                            if (Tethering.VDBG) {
                                Log.d(Tethering.TAG, "Tether Mode requested by " + tetherInterfaceStateMachine);
                            }
                            TetherMasterSM.this.handleInterfaceServingStateActive(message.arg1, tetherInterfaceStateMachine);
                            TetherMasterSM.this.transitionTo(TetherMasterSM.this.mTetherModeAliveState);
                            return true;
                        case TetherMasterSM.EVENT_IFACE_SERVING_STATE_INACTIVE:
                            TetherInterfaceStateMachine tetherInterfaceStateMachine2 = (TetherInterfaceStateMachine) message.obj;
                            if (Tethering.VDBG) {
                                Log.d(Tethering.TAG, "Tether Mode unrequested by " + tetherInterfaceStateMachine2);
                            }
                            TetherMasterSM.this.handleInterfaceServingStateInactive(tetherInterfaceStateMachine2);
                            return true;
                        default:
                            return false;
                    }
                }
                return true;
            }
        }

        protected boolean turnOnMasterTetherSettings() {
            TetheringConfiguration tetheringConfiguration = Tethering.this.mConfig;
            try {
                Tethering.this.mNMService.setIpForwardingEnabled(true);
                try {
                    Tethering.this.mNMService.startTethering(tetheringConfiguration.dhcpRanges);
                } catch (Exception e) {
                    try {
                        Tethering.this.mNMService.stopTethering();
                        Tethering.this.mNMService.startTethering(tetheringConfiguration.dhcpRanges);
                    } catch (Exception e2) {
                        Tethering.this.mLog.e(e2);
                        transitionTo(this.mStartTetheringErrorState);
                        return false;
                    }
                }
                Tethering.this.mLog.log("SET master tether settings: ON");
                return true;
            } catch (Exception e3) {
                Tethering.this.mLog.e(e3);
                transitionTo(this.mSetIpForwardingEnabledErrorState);
                return false;
            }
        }

        protected boolean turnOffMasterTetherSettings() {
            try {
                Tethering.this.mNMService.stopTethering();
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                    transitionTo(this.mInitialState);
                    Tethering.this.mLog.log("SET master tether settings: OFF");
                    return true;
                } catch (Exception e) {
                    Tethering.this.mLog.e(e);
                    transitionTo(this.mSetIpForwardingDisabledErrorState);
                    return false;
                }
            } catch (Exception e2) {
                Tethering.this.mLog.e(e2);
                transitionTo(this.mStopTetheringErrorState);
                return false;
            }
        }

        protected void chooseUpstreamType(boolean z) {
            Tethering.this.maybeUpdateConfiguration();
            NetworkState networkStateSelectPreferredUpstreamType = Tethering.this.mUpstreamNetworkMonitor.selectPreferredUpstreamType(Tethering.this.mConfig.preferredUpstreamIfaceTypes);
            if (networkStateSelectPreferredUpstreamType == null) {
                if (z) {
                    Tethering.this.mUpstreamNetworkMonitor.registerMobileNetworkRequest();
                } else {
                    sendMessageDelayed(CMD_RETRY_UPSTREAM, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                }
            }
            Tethering.this.mUpstreamNetworkMonitor.setCurrentUpstream(networkStateSelectPreferredUpstreamType != null ? networkStateSelectPreferredUpstreamType.network : null);
            setUpstreamNetwork(networkStateSelectPreferredUpstreamType);
        }

        protected void setUpstreamNetwork(NetworkState networkState) {
            InterfaceSet tetheringInterfaces;
            if (networkState != null) {
                Tethering.this.mLog.i("Looking for default routes on: " + networkState.linkProperties);
                tetheringInterfaces = Tethering.this.getTetheringInterfaces(networkState);
                Tethering.this.mLog.i("Found upstream interface(s): " + tetheringInterfaces);
            } else {
                tetheringInterfaces = null;
            }
            if (tetheringInterfaces != null) {
                setDnsForwarders(networkState.network, networkState.linkProperties);
            }
            notifyDownstreamsOfNewUpstreamIface(tetheringInterfaces);
            if (networkState == null || !Tethering.this.pertainsToCurrentUpstream(networkState)) {
                if (Tethering.this.mCurrentUpstreamIfaceSet == null) {
                    handleNewUpstreamNetworkState(null);
                    return;
                }
                return;
            }
            handleNewUpstreamNetworkState(networkState);
        }

        protected void setDnsForwarders(Network network, LinkProperties linkProperties) {
            String[] strArrMakeStrings = Tethering.this.mConfig.defaultIPv4DNS;
            List<InetAddress> dnsServers = linkProperties.getDnsServers();
            if (dnsServers != null && !dnsServers.isEmpty()) {
                strArrMakeStrings = NetworkUtils.makeStrings(dnsServers);
            }
            try {
                Tethering.this.mNMService.setDnsForwarders(network, strArrMakeStrings);
                Tethering.this.mLog.log(String.format("SET DNS forwarders: network=%s dnsServers=%s", network, Arrays.toString(strArrMakeStrings)));
            } catch (Exception e) {
                Tethering.this.mLog.e("setting DNS forwarders failed, " + e);
                transitionTo(this.mSetDnsForwardersErrorState);
            }
        }

        protected void notifyDownstreamsOfNewUpstreamIface(InterfaceSet interfaceSet) {
            Tethering.this.mCurrentUpstreamIfaceSet = interfaceSet;
            Iterator<TetherInterfaceStateMachine> it = this.mNotifyList.iterator();
            while (it.hasNext()) {
                it.next().sendMessage(TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED, interfaceSet);
            }
        }

        protected void handleNewUpstreamNetworkState(NetworkState networkState) {
            this.mIPv6TetheringCoordinator.updateUpstreamNetworkState(networkState);
            this.mOffload.updateUpstreamNetworkState(networkState);
        }

        private void handleInterfaceServingStateActive(int i, TetherInterfaceStateMachine tetherInterfaceStateMachine) {
            if (this.mNotifyList.indexOf(tetherInterfaceStateMachine) < 0) {
                this.mNotifyList.add(tetherInterfaceStateMachine);
                this.mIPv6TetheringCoordinator.addActiveDownstream(tetherInterfaceStateMachine, i);
            }
            if (i == 2) {
                Tethering.this.mForwardedDownstreams.add(tetherInterfaceStateMachine);
            } else {
                this.mOffload.excludeDownstreamInterface(tetherInterfaceStateMachine.interfaceName());
                Tethering.this.mForwardedDownstreams.remove(tetherInterfaceStateMachine);
            }
            if (tetherInterfaceStateMachine.interfaceType() == 0) {
                WifiManager wifiManager = Tethering.this.getWifiManager();
                String strInterfaceName = tetherInterfaceStateMachine.interfaceName();
                switch (i) {
                    case 2:
                        wifiManager.updateInterfaceIpState(strInterfaceName, 1);
                        break;
                    case 3:
                        wifiManager.updateInterfaceIpState(strInterfaceName, 2);
                        break;
                    default:
                        Log.wtf(Tethering.TAG, "Unknown active serving mode: " + i);
                        break;
                }
            }
        }

        private void handleInterfaceServingStateInactive(TetherInterfaceStateMachine tetherInterfaceStateMachine) {
            this.mNotifyList.remove(tetherInterfaceStateMachine);
            this.mIPv6TetheringCoordinator.removeActiveDownstream(tetherInterfaceStateMachine);
            this.mOffload.excludeDownstreamInterface(tetherInterfaceStateMachine.interfaceName());
            Tethering.this.mForwardedDownstreams.remove(tetherInterfaceStateMachine);
            if (tetherInterfaceStateMachine.interfaceType() == 0 && tetherInterfaceStateMachine.lastError() != 0) {
                Tethering.this.getWifiManager().updateInterfaceIpState(tetherInterfaceStateMachine.interfaceName(), 0);
            }
        }

        private void handleUpstreamNetworkMonitorCallback(int i, Object obj) {
            if (i == 10) {
                this.mOffload.sendOffloadExemptPrefixes((Set) obj);
            }
            NetworkState networkState = (NetworkState) obj;
            if (networkState == null || !Tethering.this.pertainsToCurrentUpstream(networkState)) {
                if (Tethering.this.mCurrentUpstreamIfaceSet == null) {
                    chooseUpstreamType(false);
                    return;
                }
                return;
            }
            switch (i) {
                case 1:
                    break;
                case 2:
                    handleNewUpstreamNetworkState(networkState);
                    break;
                case 3:
                    chooseUpstreamType(false);
                    break;
                case 4:
                    handleNewUpstreamNetworkState(null);
                    break;
                default:
                    Tethering.this.mLog.e("Unknown arg1 value: " + i);
                    break;
            }
        }

        class TetherModeAliveState extends State {
            boolean mUpstreamWanted = false;
            boolean mTryCell = true;

            TetherModeAliveState() {
            }

            public void enter() {
                if (TetherMasterSM.this.turnOnMasterTetherSettings()) {
                    Tethering.this.mSimChange.startListening();
                    Tethering.this.mUpstreamNetworkMonitor.start();
                    if (Tethering.this.upstreamWanted()) {
                        this.mUpstreamWanted = true;
                        TetherMasterSM.this.mOffload.start();
                        TetherMasterSM.this.chooseUpstreamType(true);
                        this.mTryCell = false;
                    }
                }
            }

            public void exit() {
                TetherMasterSM.this.mOffload.stop();
                Tethering.this.mUpstreamNetworkMonitor.stop();
                Tethering.this.mSimChange.stopListening();
                TetherMasterSM.this.notifyDownstreamsOfNewUpstreamIface(null);
                TetherMasterSM.this.handleNewUpstreamNetworkState(null);
            }

            private boolean updateUpstreamWanted() {
                boolean z = this.mUpstreamWanted;
                this.mUpstreamWanted = Tethering.this.upstreamWanted();
                if (this.mUpstreamWanted != z) {
                    if (this.mUpstreamWanted) {
                        TetherMasterSM.this.mOffload.start();
                    } else {
                        TetherMasterSM.this.mOffload.stop();
                    }
                }
                return z;
            }

            public boolean processMessage(Message message) {
                Tethering.this.logMessage(this, message.what);
                switch (message.what) {
                    case TetherMasterSM.EVENT_IFACE_SERVING_STATE_ACTIVE:
                        TetherInterfaceStateMachine tetherInterfaceStateMachine = (TetherInterfaceStateMachine) message.obj;
                        if (Tethering.VDBG) {
                            Log.d(Tethering.TAG, "Tether Mode requested by " + tetherInterfaceStateMachine);
                        }
                        TetherMasterSM.this.handleInterfaceServingStateActive(message.arg1, tetherInterfaceStateMachine);
                        tetherInterfaceStateMachine.sendMessage(TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED, Tethering.this.mCurrentUpstreamIfaceSet);
                        if (!updateUpstreamWanted() && this.mUpstreamWanted) {
                            TetherMasterSM.this.chooseUpstreamType(true);
                        }
                        break;
                    case TetherMasterSM.EVENT_IFACE_SERVING_STATE_INACTIVE:
                        TetherInterfaceStateMachine tetherInterfaceStateMachine2 = (TetherInterfaceStateMachine) message.obj;
                        if (Tethering.VDBG) {
                            Log.d(Tethering.TAG, "Tether Mode unrequested by " + tetherInterfaceStateMachine2);
                        }
                        TetherMasterSM.this.handleInterfaceServingStateInactive(tetherInterfaceStateMachine2);
                        if (!TetherMasterSM.this.mNotifyList.isEmpty()) {
                            Log.d(Tethering.TAG, "TetherModeAlive still has " + TetherMasterSM.this.mNotifyList.size() + " live requests:");
                            for (TetherInterfaceStateMachine tetherInterfaceStateMachine3 : TetherMasterSM.this.mNotifyList) {
                                Log.d(Tethering.TAG, "  " + tetherInterfaceStateMachine3);
                            }
                            if (updateUpstreamWanted() && !this.mUpstreamWanted) {
                                Tethering.this.mUpstreamNetworkMonitor.releaseMobileNetworkRequest();
                            }
                        } else {
                            TetherMasterSM.this.turnOffMasterTetherSettings();
                        }
                        break;
                    case TetherMasterSM.CMD_UPSTREAM_CHANGED:
                        updateUpstreamWanted();
                        if (this.mUpstreamWanted) {
                            TetherMasterSM.this.chooseUpstreamType(true);
                            this.mTryCell = false;
                        }
                        break;
                    case TetherMasterSM.CMD_RETRY_UPSTREAM:
                        updateUpstreamWanted();
                        if (this.mUpstreamWanted) {
                            TetherMasterSM.this.chooseUpstreamType(this.mTryCell);
                            this.mTryCell = !this.mTryCell;
                        }
                        break;
                    case TetherMasterSM.EVENT_UPSTREAM_CALLBACK:
                        updateUpstreamWanted();
                        if (this.mUpstreamWanted) {
                            TetherMasterSM.this.handleUpstreamNetworkMonitorCallback(message.arg1, message.obj);
                        }
                        break;
                    case TetherMasterSM.CMD_CLEAR_ERROR:
                    default:
                        return false;
                    case TetherMasterSM.EVENT_IFACE_UPDATE_LINKPROPERTIES:
                        LinkProperties linkProperties = (LinkProperties) message.obj;
                        if (message.arg1 == 2) {
                            TetherMasterSM.this.mOffload.updateDownstreamLinkProperties(linkProperties);
                        } else {
                            TetherMasterSM.this.mOffload.excludeDownstreamInterface(linkProperties.getInterfaceName());
                        }
                        break;
                }
                return true;
            }
        }

        class ErrorState extends State {
            private int mErrorNotification;

            ErrorState() {
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i == TetherMasterSM.EVENT_IFACE_SERVING_STATE_ACTIVE) {
                    ((TetherInterfaceStateMachine) message.obj).sendMessage(this.mErrorNotification);
                } else {
                    if (i != TetherMasterSM.CMD_CLEAR_ERROR) {
                        return false;
                    }
                    this.mErrorNotification = 0;
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
                }
                return true;
            }

            void notify(int i) {
                this.mErrorNotification = i;
                Iterator it = TetherMasterSM.this.mNotifyList.iterator();
                while (it.hasNext()) {
                    ((TetherInterfaceStateMachine) it.next()).sendMessage(i);
                }
            }
        }

        class SetIpForwardingEnabledErrorState extends ErrorState {
            SetIpForwardingEnabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setIpForwardingEnabled");
                notify(TetherInterfaceStateMachine.CMD_IP_FORWARDING_ENABLE_ERROR);
            }
        }

        class SetIpForwardingDisabledErrorState extends ErrorState {
            SetIpForwardingDisabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setIpForwardingDisabled");
                notify(TetherInterfaceStateMachine.CMD_IP_FORWARDING_DISABLE_ERROR);
            }
        }

        class StartTetheringErrorState extends ErrorState {
            StartTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in startTethering");
                notify(TetherInterfaceStateMachine.CMD_START_TETHERING_ERROR);
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
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
                notify(TetherInterfaceStateMachine.CMD_STOP_TETHERING_ERROR);
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
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
                notify(TetherInterfaceStateMachine.CMD_SET_DNS_FORWARDERS_ERROR);
                try {
                    Tethering.this.mNMService.stopTethering();
                } catch (Exception e) {
                }
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                } catch (Exception e2) {
                }
            }
        }

        class OffloadWrapper {
            OffloadWrapper() {
            }

            public void start() {
                Tethering.this.mOffloadController.start();
                sendOffloadExemptPrefixes();
            }

            public void stop() {
                Tethering.this.mOffloadController.stop();
            }

            public void updateUpstreamNetworkState(NetworkState networkState) {
                Tethering.this.mOffloadController.setUpstreamLinkProperties(networkState != null ? networkState.linkProperties : null);
            }

            public void updateDownstreamLinkProperties(LinkProperties linkProperties) {
                sendOffloadExemptPrefixes();
                Tethering.this.mOffloadController.notifyDownstreamLinkProperties(linkProperties);
            }

            public void excludeDownstreamInterface(String str) {
                sendOffloadExemptPrefixes();
                Tethering.this.mOffloadController.removeDownstreamInterface(str);
            }

            public void sendOffloadExemptPrefixes() {
                sendOffloadExemptPrefixes(Tethering.this.mUpstreamNetworkMonitor.getLocalPrefixes());
            }

            public void sendOffloadExemptPrefixes(Set<IpPrefix> set) {
                PrefixUtils.addNonForwardablePrefixes(set);
                set.add(PrefixUtils.DEFAULT_WIFI_P2P_PREFIX);
                for (TetherInterfaceStateMachine tetherInterfaceStateMachine : TetherMasterSM.this.mNotifyList) {
                    LinkProperties linkProperties = tetherInterfaceStateMachine.linkProperties();
                    switch (tetherInterfaceStateMachine.servingMode()) {
                        case 2:
                            Iterator it = linkProperties.getAllLinkAddresses().iterator();
                            while (it.hasNext()) {
                                InetAddress address = ((LinkAddress) it.next()).getAddress();
                                if (!address.isLinkLocalAddress()) {
                                    set.add(PrefixUtils.ipAddressAsPrefix(address));
                                }
                            }
                            break;
                        case 3:
                            set.addAll(PrefixUtils.localPrefixesFrom(linkProperties));
                            break;
                    }
                }
                Tethering.this.mOffloadController.setLocalPrefixes(set);
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, indentingPrintWriter)) {
            indentingPrintWriter.println("Tethering:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println("Configuration:");
            indentingPrintWriter.increaseIndent();
            this.mConfig.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
            synchronized (this.mPublicSync) {
                indentingPrintWriter.println("Tether state:");
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < this.mTetherStates.size(); i++) {
                    String strKeyAt = this.mTetherStates.keyAt(i);
                    TetherState tetherStateValueAt = this.mTetherStates.valueAt(i);
                    indentingPrintWriter.print(strKeyAt + " - ");
                    switch (tetherStateValueAt.lastState) {
                        case 0:
                            indentingPrintWriter.print("UnavailableState");
                            break;
                        case 1:
                            indentingPrintWriter.print("AvailableState");
                            break;
                        case 2:
                            indentingPrintWriter.print("TetheredState");
                            break;
                        case 3:
                            indentingPrintWriter.print("LocalHotspotState");
                            break;
                        default:
                            indentingPrintWriter.print("UnknownState");
                            break;
                    }
                    indentingPrintWriter.println(" - lastError = " + tetherStateValueAt.lastError);
                }
                indentingPrintWriter.println("Upstream wanted: " + upstreamWanted());
                indentingPrintWriter.println("Current upstream interface(s): " + this.mCurrentUpstreamIfaceSet);
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.println("Hardware offload:");
            indentingPrintWriter.increaseIndent();
            this.mOffloadController.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("Log:");
            indentingPrintWriter.increaseIndent();
            if (argsContain(strArr, ConnectivityService.SHORT_ARG)) {
                indentingPrintWriter.println("<log removed for brevity>");
            } else {
                this.mLog.dump(fileDescriptor, indentingPrintWriter, strArr);
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.decreaseIndent();
        }
    }

    private static boolean argsContain(String[] strArr, String str) {
        for (String str2 : strArr) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    private IControlsTethering makeControlCallback(final String str) {
        return new IControlsTethering() {
            @Override
            public void updateInterfaceState(TetherInterfaceStateMachine tetherInterfaceStateMachine, int i, int i2) {
                Tethering.this.notifyInterfaceStateChange(str, tetherInterfaceStateMachine, i, i2);
            }

            @Override
            public void updateLinkProperties(TetherInterfaceStateMachine tetherInterfaceStateMachine, LinkProperties linkProperties) {
                Tethering.this.notifyLinkPropertiesChanged(str, tetherInterfaceStateMachine, linkProperties);
            }
        };
    }

    private void notifyInterfaceStateChange(String str, TetherInterfaceStateMachine tetherInterfaceStateMachine, int i, int i2) {
        int i3;
        synchronized (this.mPublicSync) {
            TetherState tetherState = this.mTetherStates.get(str);
            if (tetherState != null && tetherState.stateMachine.equals(tetherInterfaceStateMachine)) {
                tetherState.lastState = i;
                tetherState.lastError = i2;
            } else {
                Log.d(TAG, "got notification from stale iface " + str);
            }
        }
        boolean z = true;
        this.mLog.log(String.format("OBSERVED iface=%s state=%s error=%s", str, Integer.valueOf(i), Integer.valueOf(i2)));
        try {
            INetworkPolicyManager iNetworkPolicyManager = this.mPolicyManager;
            if (i != 2) {
                z = false;
            }
            iNetworkPolicyManager.onTetheringChanged(str, z);
        } catch (RemoteException e) {
        }
        if (i2 == 5) {
            this.mTetherMasterSM.sendMessage(327686, tetherInterfaceStateMachine);
        }
        switch (i) {
            case 0:
            case 1:
                i3 = 327682;
                break;
            case 2:
            case 3:
                i3 = 327681;
                break;
            default:
                Log.wtf(TAG, "Unknown interface state: " + i);
                return;
        }
        this.mTetherMasterSM.sendMessage(i3, i, 0, tetherInterfaceStateMachine);
        sendTetherStateChangedBroadcast();
    }

    private void notifyLinkPropertiesChanged(String str, TetherInterfaceStateMachine tetherInterfaceStateMachine, LinkProperties linkProperties) {
        synchronized (this.mPublicSync) {
            TetherState tetherState = this.mTetherStates.get(str);
            if (tetherState != null && tetherState.stateMachine.equals(tetherInterfaceStateMachine)) {
                int i = tetherState.lastState;
                this.mLog.log(String.format("OBSERVED LinkProperties update iface=%s state=%s lp=%s", str, IControlsTethering.getStateString(i), linkProperties));
                this.mTetherMasterSM.sendMessage(327687, i, 0, linkProperties);
            } else {
                this.mLog.log("got notification from stale iface " + str);
            }
        }
    }

    private void maybeTrackNewInterfaceLocked(String str) {
        int iIfaceNameToType = ifaceNameToType(str);
        if (iIfaceNameToType == -1) {
            this.mLog.log(str + " is not a tetherable iface, ignoring");
            return;
        }
        maybeTrackNewInterfaceLocked(str, iIfaceNameToType);
    }

    private void maybeTrackNewInterfaceLocked(String str, int i) {
        if (this.mTetherStates.containsKey(str)) {
            this.mLog.log("active iface (" + str + ") reported as added, ignoring");
            return;
        }
        this.mLog.log("adding TetheringInterfaceStateMachine for: " + str);
        TetherState tetherState = new TetherState(new TetherInterfaceStateMachine(str, this.mLooper, i, this.mLog, this.mNMService, this.mStatsService, makeControlCallback(str), this.mDeps));
        this.mTetherStates.put(str, tetherState);
        tetherState.stateMachine.start();
    }

    private void stopTrackingInterfaceLocked(String str) {
        TetherState tetherState = this.mTetherStates.get(str);
        if (tetherState == null) {
            this.mLog.log("attempting to remove unknown iface (" + str + "), ignoring");
            return;
        }
        tetherState.stateMachine.stop();
        this.mLog.log("removing TetheringInterfaceStateMachine for: " + str);
        this.mTetherStates.remove(str);
    }

    private static String getIPv4DefaultRouteInterface(NetworkState networkState) {
        if (networkState == null) {
            return null;
        }
        return getInterfaceForDestination(networkState.linkProperties, Inet4Address.ANY);
    }

    private static String getIPv6DefaultRouteInterface(NetworkState networkState) {
        if (networkState == null || networkState.networkCapabilities == null || !networkState.networkCapabilities.hasTransport(0)) {
            return null;
        }
        return getInterfaceForDestination(networkState.linkProperties, Inet6Address.ANY);
    }

    private static String getInterfaceForDestination(LinkProperties linkProperties, InetAddress inetAddress) {
        RouteInfo routeInfoSelectBestRoute;
        if (linkProperties != null) {
            routeInfoSelectBestRoute = RouteInfo.selectBestRoute(linkProperties.getAllRoutes(), inetAddress);
        } else {
            routeInfoSelectBestRoute = null;
        }
        if (routeInfoSelectBestRoute != null) {
            return routeInfoSelectBestRoute.getInterface();
        }
        return null;
    }

    private Object getMtkTethering(Context context) {
        try {
            if (sPcLoader == null) {
                sPcLoader = new PathClassLoader("/system/framework/mediatek-framework-net.jar", this.mContext.getClassLoader());
            }
            if (sPcLoader != null) {
                Constructor constructor = sPcLoader.loadClass("com.mediatek.net.tethering.MtkTethering").getConstructor(Context.class, Tethering.class);
                constructor.setAccessible(true);
                Object objNewInstance = constructor.newInstance(context, this);
                Log.d(TAG, "getMtkTethering is loaded");
                return objNewInstance;
            }
            Log.e(TAG, "sPcLoader is null for getMtkTethering");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "No mtkTethering! Used AOSP for instead! %s", e);
            return null;
        }
    }

    private IPv6TetheringCoordinator getMtkIPv6TetheringCoordinator(ArrayList<TetherInterfaceStateMachine> arrayList, SharedLog sharedLog) {
        try {
            if (sPcLoader == null) {
                sPcLoader = new PathClassLoader("/system/framework/mediatek-framework-net.jar", this.mContext.getClassLoader());
            }
            if (sPcLoader != null) {
                Constructor constructor = sPcLoader.loadClass("com.mediatek.net.tethering.MtkIPv6TetheringCoordinator").getConstructor(ArrayList.class, SharedLog.class);
                constructor.setAccessible(true);
                IPv6TetheringCoordinator iPv6TetheringCoordinator = (IPv6TetheringCoordinator) constructor.newInstance(arrayList, sharedLog);
                Log.d(TAG, "MtkIPv6TetheringCoordinator is loaded");
                return iPv6TetheringCoordinator;
            }
            Log.e(TAG, "sPcLoader is null for getMtkIPv6TetheringCoordinator");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get MtkIPv6TetheringCoordinator: %s", e);
            return null;
        }
    }

    private InterfaceSet getTetheringInterfaces(NetworkState networkState) {
        if (mMtkTethering != null) {
            try {
                Method method = mMtkTethering.getClass().getMethod("getTetheringInterfaces", NetworkState.class);
                method.setAccessible(true);
                return (InterfaceSet) method.invoke(mMtkTethering, networkState);
            } catch (Exception e) {
                Log.e(TAG, "getTetheringInterfaces method error:" + e);
                return null;
            }
        }
        Log.w(TAG, "getTetheringInterfaces fallback to aosp");
        return TetheringInterfaceUtils.getTetheringInterfaces(networkState);
    }

    private static String[] copy(String[] strArr) {
        return (String[]) Arrays.copyOf(strArr, strArr.length);
    }
}
