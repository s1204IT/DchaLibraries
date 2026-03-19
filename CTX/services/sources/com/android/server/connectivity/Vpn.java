package com.android.server.connectivity;

import android.R;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemService;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.server.DeviceIdleController;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;

public class Vpn {
    private static final boolean LOGD = true;
    private static final int MAX_ROUTES_TO_EVALUATE = 150;
    private static final long MOST_IPV4_ADDRESSES_COUNT = 3650722201L;
    private static final BigInteger MOST_IPV6_ADDRESSES_COUNT = BigInteger.ONE.shiftLeft(128).multiply(BigInteger.valueOf(85)).divide(BigInteger.valueOf(100));
    private static final String NETWORKTYPE = "VPN";
    private static final String TAG = "Vpn";
    private static final long VPN_LAUNCH_IDLE_WHITELIST_DURATION_MS = 60000;
    private boolean mAlwaysOn;

    @GuardedBy("this")
    private Set<UidRange> mBlockedUsers;

    @VisibleForTesting
    protected VpnConfig mConfig;
    private Connection mConnection;
    private Context mContext;
    private volatile boolean mEnableTeardown;
    private String mInterface;
    private boolean mIsPackageIntentReceiverRegistered;
    private LegacyVpnRunner mLegacyVpnRunner;
    private boolean mLockdown;
    private final Looper mLooper;
    private final INetworkManagementService mNetd;

    @VisibleForTesting
    protected NetworkAgent mNetworkAgent;

    @VisibleForTesting
    protected final NetworkCapabilities mNetworkCapabilities;
    private NetworkInfo mNetworkInfo;
    private INetworkManagementEventObserver mObserver;
    private int mOwnerUID;
    private String mPackage;
    private final BroadcastReceiver mPackageIntentReceiver;
    private PendingIntent mStatusIntent;
    private final SystemServices mSystemServices;
    private final int mUserHandle;

    private native boolean jniAddAddress(String str, String str2, int i);

    private native int jniCheck(String str);

    private native int jniCreate(int i);

    private native boolean jniDelAddress(String str, String str2, int i);

    private native String jniGetName(int i);

    private native void jniReset(String str);

    private native int jniSetAddresses(String str, String str2);

    public Vpn(Looper looper, Context context, INetworkManagementService iNetworkManagementService, int i) {
        this(looper, context, iNetworkManagementService, i, new SystemServices(context));
    }

    @VisibleForTesting
    protected Vpn(Looper looper, Context context, INetworkManagementService iNetworkManagementService, int i, SystemServices systemServices) {
        this.mEnableTeardown = true;
        this.mAlwaysOn = false;
        this.mLockdown = false;
        this.mBlockedUsers = new ArraySet();
        this.mPackageIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String schemeSpecificPart;
                Uri data = intent.getData();
                if (data != null) {
                    schemeSpecificPart = data.getSchemeSpecificPart();
                } else {
                    schemeSpecificPart = null;
                }
                if (schemeSpecificPart == null) {
                    return;
                }
                synchronized (Vpn.this) {
                    if (schemeSpecificPart.equals(Vpn.this.getAlwaysOnPackage())) {
                        String action = intent.getAction();
                        Log.i(Vpn.TAG, "Received broadcast " + action + " for always-on VPN package " + schemeSpecificPart + " in user " + Vpn.this.mUserHandle);
                        byte b = -1;
                        int iHashCode = action.hashCode();
                        if (iHashCode != -810471698) {
                            if (iHashCode == 525384130 && action.equals("android.intent.action.PACKAGE_REMOVED")) {
                                b = 1;
                            }
                        } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                            b = 0;
                        }
                        switch (b) {
                            case 0:
                                Vpn.this.startAlwaysOnVpn();
                                break;
                            case 1:
                                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                                    Vpn.this.setAlwaysOnPackage(null, false);
                                }
                                break;
                        }
                    }
                }
            }
        };
        this.mIsPackageIntentReceiverRegistered = false;
        this.mObserver = new BaseNetworkObserver() {
            public void interfaceStatusChanged(String str, boolean z) {
                synchronized (Vpn.this) {
                    if (!z) {
                        try {
                            if (Vpn.this.mLegacyVpnRunner != null) {
                                Vpn.this.mLegacyVpnRunner.check(str);
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                }
            }

            public void interfaceRemoved(String str) {
                synchronized (Vpn.this) {
                    if (str.equals(Vpn.this.mInterface) && Vpn.this.jniCheck(str) == 0) {
                        Vpn.this.mStatusIntent = null;
                        Vpn.this.mNetworkCapabilities.setUids(null);
                        Vpn.this.mConfig = null;
                        Vpn.this.mInterface = null;
                        if (Vpn.this.mConnection != null) {
                            Vpn.this.mContext.unbindService(Vpn.this.mConnection);
                            Vpn.this.mConnection = null;
                            Vpn.this.agentDisconnect();
                        } else if (Vpn.this.mLegacyVpnRunner != null) {
                            Vpn.this.mLegacyVpnRunner.exit();
                            Vpn.this.mLegacyVpnRunner = null;
                        }
                    }
                }
            }
        };
        this.mContext = context;
        this.mNetd = iNetworkManagementService;
        this.mUserHandle = i;
        this.mLooper = looper;
        this.mSystemServices = systemServices;
        this.mPackage = "[Legacy VPN]";
        this.mOwnerUID = getAppUid(this.mPackage, this.mUserHandle);
        try {
            iNetworkManagementService.registerObserver(this.mObserver);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Problem registering observer", e);
        }
        this.mNetworkInfo = new NetworkInfo(17, 0, NETWORKTYPE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mNetworkCapabilities = new NetworkCapabilities();
        this.mNetworkCapabilities.addTransportType(4);
        this.mNetworkCapabilities.removeCapability(15);
        updateCapabilities();
        loadAlwaysOnPackage();
    }

    public void setEnableTeardown(boolean z) {
        this.mEnableTeardown = z;
    }

    @VisibleForTesting
    protected void updateState(NetworkInfo.DetailedState detailedState, String str) {
        Log.d(TAG, "setting state=" + detailedState + ", reason=" + str);
        this.mNetworkInfo.setDetailedState(detailedState, str, null);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        updateAlwaysOnNotification(detailedState);
    }

    public void updateCapabilities() {
        updateCapabilities((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class), this.mConfig != null ? this.mConfig.underlyingNetworks : null, this.mNetworkCapabilities);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkCapabilities(this.mNetworkCapabilities);
        }
    }

    @VisibleForTesting
    public static void updateCapabilities(ConnectivityManager connectivityManager, Network[] networkArr, NetworkCapabilities networkCapabilities) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        int iMinBandwidth;
        int iMinBandwidth2;
        boolean z5;
        boolean z6;
        boolean z7 = true;
        int[] iArr = {4};
        if (networkArr != null) {
            int length = networkArr.length;
            int[] iArr2 = iArr;
            int i = 0;
            z4 = false;
            iMinBandwidth = 0;
            iMinBandwidth2 = 0;
            boolean z8 = false;
            boolean z9 = false;
            z5 = false;
            while (i < length) {
                NetworkCapabilities networkCapabilities2 = connectivityManager.getNetworkCapabilities(networkArr[i]);
                if (networkCapabilities2 == null) {
                    z6 = z7;
                } else {
                    int[] iArrAppendInt = iArr2;
                    for (int i2 : networkCapabilities2.getTransportTypes()) {
                        iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i2);
                    }
                    iMinBandwidth = NetworkCapabilities.minBandwidth(iMinBandwidth, networkCapabilities2.getLinkDownstreamBandwidthKbps());
                    iMinBandwidth2 = NetworkCapabilities.minBandwidth(iMinBandwidth2, networkCapabilities2.getLinkUpstreamBandwidthKbps());
                    z6 = true;
                    boolean z10 = (!networkCapabilities2.hasCapability(11)) | z8;
                    z9 |= !networkCapabilities2.hasCapability(18);
                    z5 |= !networkCapabilities2.hasCapability(20);
                    iArr2 = iArrAppendInt;
                    z4 = true;
                    z8 = z10;
                }
                i++;
                z7 = z6;
            }
            z = z7;
            iArr = iArr2;
            z2 = z8;
            z3 = z9;
        } else {
            z = true;
            z2 = false;
            z3 = false;
            z4 = false;
            iMinBandwidth = 0;
            iMinBandwidth2 = 0;
            z5 = false;
        }
        if (!z4) {
            z2 = z;
            z3 = false;
            z5 = false;
        }
        networkCapabilities.setTransportTypes(iArr);
        networkCapabilities.setLinkDownstreamBandwidthKbps(iMinBandwidth);
        networkCapabilities.setLinkUpstreamBandwidthKbps(iMinBandwidth2);
        networkCapabilities.setCapability(11, !z2 ? z : false);
        networkCapabilities.setCapability(18, !z3 ? z : false);
        if (z5) {
            z = false;
        }
        networkCapabilities.setCapability(20, z);
    }

    public synchronized void setLockdown(boolean z) {
        enforceControlPermissionOrInternalCaller();
        setVpnForcedLocked(z);
        this.mLockdown = z;
        if (this.mAlwaysOn) {
            saveAlwaysOnPackage();
        }
    }

    public boolean isAlwaysOnPackageSupported(String str) {
        enforceSettingsPermission();
        if (str == null) {
            return false;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        ApplicationInfo applicationInfoAsUser = null;
        try {
            applicationInfoAsUser = packageManager.getApplicationInfoAsUser(str, 0, this.mUserHandle);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Can't find \"" + str + "\" when checking always-on support");
        }
        if (applicationInfoAsUser == null || applicationInfoAsUser.targetSdkVersion < 24) {
            return false;
        }
        Intent intent = new Intent("android.net.VpnService");
        intent.setPackage(str);
        List listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(intent, 128, this.mUserHandle);
        if (listQueryIntentServicesAsUser == null || listQueryIntentServicesAsUser.size() == 0) {
            return false;
        }
        Iterator it = listQueryIntentServicesAsUser.iterator();
        while (it.hasNext()) {
            Bundle bundle = ((ResolveInfo) it.next()).serviceInfo.metaData;
            if (bundle != null && !bundle.getBoolean("android.net.VpnService.SUPPORTS_ALWAYS_ON", true)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean setAlwaysOnPackage(String str, boolean z) {
        enforceControlPermissionOrInternalCaller();
        if (setAlwaysOnPackageInternal(str, z)) {
            saveAlwaysOnPackage();
            return true;
        }
        return false;
    }

    @GuardedBy("this")
    private boolean setAlwaysOnPackageInternal(String str, boolean z) {
        boolean z2 = false;
        if ("[Legacy VPN]".equals(str)) {
            Log.w(TAG, "Not setting legacy VPN \"" + str + "\" as always-on.");
            return false;
        }
        if (str != null) {
            if (!setPackageAuthorization(str, true)) {
                return false;
            }
            this.mAlwaysOn = true;
        } else {
            str = "[Legacy VPN]";
            this.mAlwaysOn = false;
        }
        if (this.mAlwaysOn && z) {
            z2 = true;
        }
        this.mLockdown = z2;
        if (isCurrentPreparedPackage(str)) {
            updateAlwaysOnNotification(this.mNetworkInfo.getDetailedState());
        } else {
            prepareInternal(str);
        }
        maybeRegisterPackageChangeReceiverLocked(str);
        setVpnForcedLocked(this.mLockdown);
        return true;
    }

    private static boolean isNullOrLegacyVpn(String str) {
        return str == null || "[Legacy VPN]".equals(str);
    }

    private void unregisterPackageChangeReceiverLocked() {
        if (this.mIsPackageIntentReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mPackageIntentReceiver);
            this.mIsPackageIntentReceiverRegistered = false;
        }
    }

    private void maybeRegisterPackageChangeReceiverLocked(String str) {
        unregisterPackageChangeReceiverLocked();
        if (!isNullOrLegacyVpn(str)) {
            this.mIsPackageIntentReceiverRegistered = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
            intentFilter.addDataSchemeSpecificPart(str, 0);
            this.mContext.registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.of(this.mUserHandle), intentFilter, null, null);
        }
    }

    public synchronized String getAlwaysOnPackage() {
        enforceControlPermissionOrInternalCaller();
        return this.mAlwaysOn ? this.mPackage : null;
    }

    @GuardedBy("this")
    private void saveAlwaysOnPackage() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mSystemServices.settingsSecurePutStringForUser("always_on_vpn_app", getAlwaysOnPackage(), this.mUserHandle);
            this.mSystemServices.settingsSecurePutIntForUser("always_on_vpn_lockdown", (this.mAlwaysOn && this.mLockdown) ? 1 : 0, this.mUserHandle);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("this")
    private void loadAlwaysOnPackage() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            setAlwaysOnPackageInternal(this.mSystemServices.settingsSecureGetStringForUser("always_on_vpn_app", this.mUserHandle), this.mSystemServices.settingsSecureGetIntForUser("always_on_vpn_lockdown", 0, this.mUserHandle) != 0);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean startAlwaysOnVpn() {
        Intent intent;
        synchronized (this) {
            String alwaysOnPackage = getAlwaysOnPackage();
            boolean z = true;
            if (alwaysOnPackage == null) {
                return true;
            }
            if (!isAlwaysOnPackageSupported(alwaysOnPackage)) {
                setAlwaysOnPackage(null, false);
                return false;
            }
            if (getNetworkInfo().isConnected()) {
                return true;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ((DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class)).addPowerSaveTempWhitelistApp(Process.myUid(), alwaysOnPackage, 60000L, this.mUserHandle, false, "vpn");
                intent = new Intent("android.net.VpnService");
                intent.setPackage(alwaysOnPackage);
                if (this.mContext.startServiceAsUser(intent, UserHandle.of(this.mUserHandle)) == null) {
                    z = false;
                }
                return z;
            } catch (RuntimeException e) {
                Log.e(TAG, "VpnService " + intent + " failed to start", e);
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public synchronized boolean prepare(String str, String str2) {
        Log.i(TAG, "prepare old:" + str + ",new:" + str2);
        if (str != null) {
            if (this.mAlwaysOn && !isCurrentPreparedPackage(str)) {
                return false;
            }
            if (!isCurrentPreparedPackage(str)) {
                if (str.equals("[Legacy VPN]") || !isVpnUserPreConsented(str)) {
                    return false;
                }
                prepareInternal(str);
                return true;
            }
            if (!str.equals("[Legacy VPN]") && !isVpnUserPreConsented(str)) {
                prepareInternal("[Legacy VPN]");
                return false;
            }
        }
        if (str2 != null && (str2.equals("[Legacy VPN]") || !isCurrentPreparedPackage(str2))) {
            enforceControlPermission();
            if (this.mAlwaysOn && !isCurrentPreparedPackage(str2)) {
                return false;
            }
            prepareInternal(str2);
            return true;
        }
        return true;
    }

    private boolean isCurrentPreparedPackage(String str) {
        return getAppUid(str, this.mUserHandle) == this.mOwnerUID;
    }

    private void prepareInternal(String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mInterface != null) {
                this.mStatusIntent = null;
                agentDisconnect();
                jniReset(this.mInterface);
                this.mInterface = null;
                this.mNetworkCapabilities.setUids(null);
            }
            if (this.mConnection != null) {
                try {
                    this.mConnection.mService.transact(16777215, Parcel.obtain(), null, 1);
                } catch (Exception e) {
                }
                this.mContext.unbindService(this.mConnection);
                this.mConnection = null;
            } else if (this.mLegacyVpnRunner != null) {
                this.mLegacyVpnRunner.exit();
                this.mLegacyVpnRunner = null;
            }
            try {
                this.mNetd.denyProtect(this.mOwnerUID);
            } catch (Exception e2) {
                Log.wtf(TAG, "Failed to disallow UID " + this.mOwnerUID + " to call protect() " + e2);
            }
            Log.i(TAG, "Switched from " + this.mPackage + " to " + str);
            this.mPackage = str;
            this.mOwnerUID = getAppUid(str, this.mUserHandle);
            try {
                this.mNetd.allowProtect(this.mOwnerUID);
            } catch (Exception e3) {
                Log.wtf(TAG, "Failed to allow UID " + this.mOwnerUID + " to call protect() " + e3);
            }
            this.mConfig = null;
            updateState(NetworkInfo.DetailedState.IDLE, "prepare");
            setVpnForcedLocked(this.mLockdown);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean setPackageAuthorization(String str, boolean z) {
        enforceControlPermissionOrInternalCaller();
        int appUid = getAppUid(str, this.mUserHandle);
        if (appUid == -1 || "[Legacy VPN]".equals(str)) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ((AppOpsManager) this.mContext.getSystemService("appops")).setMode(47, appUid, str, !z ? 1 : 0);
            return true;
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to set app ops for package " + str + ", uid " + appUid, e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isVpnUserPreConsented(String str) {
        return ((AppOpsManager) this.mContext.getSystemService("appops")).noteOpNoThrow(47, Binder.getCallingUid(), str) == 0;
    }

    private int getAppUid(String str, int i) {
        if ("[Legacy VPN]".equals(str)) {
            return Process.myUid();
        }
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(str, i);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public NetworkInfo getNetworkInfo() {
        return this.mNetworkInfo;
    }

    public int getNetId() {
        if (this.mNetworkAgent != null) {
            return this.mNetworkAgent.netId;
        }
        return 0;
    }

    private LinkProperties makeLinkProperties() {
        boolean z = this.mConfig.allowIPv4;
        boolean z2 = this.mConfig.allowIPv6;
        LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(this.mInterface);
        if (this.mConfig.addresses != null) {
            for (LinkAddress linkAddress : this.mConfig.addresses) {
                linkProperties.addLinkAddress(linkAddress);
                z |= linkAddress.getAddress() instanceof Inet4Address;
                z2 |= linkAddress.getAddress() instanceof Inet6Address;
            }
        }
        if (this.mConfig.routes != null) {
            for (RouteInfo routeInfo : this.mConfig.routes) {
                linkProperties.addRoute(routeInfo);
                InetAddress address = routeInfo.getDestination().getAddress();
                z |= address instanceof Inet4Address;
                z2 |= address instanceof Inet6Address;
            }
        }
        if (this.mConfig.dnsServers != null) {
            Iterator it = this.mConfig.dnsServers.iterator();
            while (it.hasNext()) {
                InetAddress numericAddress = InetAddress.parseNumericAddress((String) it.next());
                linkProperties.addDnsServer(numericAddress);
                z |= numericAddress instanceof Inet4Address;
                z2 |= numericAddress instanceof Inet6Address;
            }
        }
        if (!z) {
            linkProperties.addRoute(new RouteInfo(new IpPrefix(Inet4Address.ANY, 0), 7));
        }
        if (!z2) {
            linkProperties.addRoute(new RouteInfo(new IpPrefix(Inet6Address.ANY, 0), 7));
        }
        StringBuilder sb = new StringBuilder();
        if (this.mConfig.searchDomains != null) {
            Iterator it2 = this.mConfig.searchDomains.iterator();
            while (it2.hasNext()) {
                sb.append((String) it2.next());
                sb.append(' ');
            }
        }
        linkProperties.setDomains(sb.toString().trim());
        return linkProperties;
    }

    @VisibleForTesting
    static boolean providesRoutesToMostDestinations(LinkProperties linkProperties) {
        List allRoutes = linkProperties.getAllRoutes();
        if (allRoutes.size() > 150) {
            return true;
        }
        Comparator comparatorLengthComparator = IpPrefix.lengthComparator();
        TreeSet treeSet = new TreeSet(comparatorLengthComparator);
        TreeSet treeSet2 = new TreeSet(comparatorLengthComparator);
        Iterator it = allRoutes.iterator();
        while (it.hasNext()) {
            IpPrefix destination = ((RouteInfo) it.next()).getDestination();
            if (destination.isIPv4()) {
                treeSet.add(destination);
            } else {
                treeSet2.add(destination);
            }
        }
        return NetworkUtils.routedIPv4AddressCount(treeSet) > MOST_IPV4_ADDRESSES_COUNT || NetworkUtils.routedIPv6AddressCount(treeSet2).compareTo(MOST_IPV6_ADDRESSES_COUNT) >= 0;
    }

    private boolean updateLinkPropertiesInPlaceIfPossible(NetworkAgent networkAgent, VpnConfig vpnConfig) {
        if (vpnConfig.allowBypass != this.mConfig.allowBypass) {
            Log.i(TAG, "Handover not possible due to changes to allowBypass");
            return false;
        }
        if (!Objects.equals(vpnConfig.allowedApplications, this.mConfig.allowedApplications) || !Objects.equals(vpnConfig.disallowedApplications, this.mConfig.disallowedApplications)) {
            Log.i(TAG, "Handover not possible due to changes to whitelisted/blacklisted apps");
            return false;
        }
        LinkProperties linkPropertiesMakeLinkProperties = makeLinkProperties();
        if (this.mNetworkCapabilities.hasCapability(12) != providesRoutesToMostDestinations(linkPropertiesMakeLinkProperties)) {
            Log.i(TAG, "Handover not possible due to changes to INTERNET capability");
            return false;
        }
        networkAgent.sendLinkProperties(linkPropertiesMakeLinkProperties);
        return true;
    }

    private void agentConnect() {
        LinkProperties linkPropertiesMakeLinkProperties = makeLinkProperties();
        if (providesRoutesToMostDestinations(linkPropertiesMakeLinkProperties)) {
            this.mNetworkCapabilities.addCapability(12);
        } else {
            this.mNetworkCapabilities.removeCapability(12);
        }
        this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTING, null, null);
        NetworkMisc networkMisc = new NetworkMisc();
        networkMisc.allowBypass = this.mConfig.allowBypass && !this.mLockdown;
        this.mNetworkCapabilities.setEstablishingVpnAppUid(Binder.getCallingUid());
        this.mNetworkCapabilities.setUids(createUserAndRestrictedProfilesRanges(this.mUserHandle, this.mConfig.allowedApplications, this.mConfig.disallowedApplications));
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNetworkAgent = new NetworkAgent(this.mLooper, this.mContext, NETWORKTYPE, this.mNetworkInfo, this.mNetworkCapabilities, linkPropertiesMakeLinkProperties, 101, networkMisc) {
                public void unwanted() {
                }
            };
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            this.mNetworkInfo.setIsAvailable(true);
            updateState(NetworkInfo.DetailedState.CONNECTED, "agentConnect");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private boolean canHaveRestrictedProfile(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return UserManager.get(this.mContext).canHaveRestrictedProfile(i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void agentDisconnect(NetworkAgent networkAgent) {
        if (networkAgent != null) {
            NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
            networkInfo.setIsAvailable(false);
            networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
            networkAgent.sendNetworkInfo(networkInfo);
        }
    }

    private void agentDisconnect() {
        if (this.mNetworkInfo.isConnected()) {
            this.mNetworkInfo.setIsAvailable(false);
            updateState(NetworkInfo.DetailedState.DISCONNECTED, "agentDisconnect");
            this.mNetworkAgent = null;
        }
    }

    public synchronized ParcelFileDescriptor establish(VpnConfig vpnConfig) {
        UserManager userManager = UserManager.get(this.mContext);
        if (Binder.getCallingUid() != this.mOwnerUID) {
            return null;
        }
        if (!isVpnUserPreConsented(this.mPackage)) {
            return null;
        }
        Intent intent = new Intent("android.net.VpnService");
        intent.setClassName(this.mPackage, vpnConfig.user);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                if (userManager.getUserInfo(this.mUserHandle).isRestricted()) {
                    throw new SecurityException("Restricted users cannot establish VPNs");
                }
                ResolveInfo resolveInfoResolveService = AppGlobals.getPackageManager().resolveService(intent, (String) null, 0, this.mUserHandle);
                if (resolveInfoResolveService == null) {
                    throw new SecurityException("Cannot find " + vpnConfig.user);
                }
                if (!"android.permission.BIND_VPN_SERVICE".equals(resolveInfoResolveService.serviceInfo.permission)) {
                    throw new SecurityException(vpnConfig.user + " does not require android.permission.BIND_VPN_SERVICE");
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                VpnConfig vpnConfig2 = this.mConfig;
                String str = this.mInterface;
                Connection connection = this.mConnection;
                NetworkAgent networkAgent = this.mNetworkAgent;
                Set uids = this.mNetworkCapabilities.getUids();
                ParcelFileDescriptor parcelFileDescriptorAdoptFd = ParcelFileDescriptor.adoptFd(jniCreate(vpnConfig.mtu));
                try {
                    String strJniGetName = jniGetName(parcelFileDescriptorAdoptFd.getFd());
                    StringBuilder sb = new StringBuilder();
                    Iterator it = vpnConfig.addresses.iterator();
                    while (it.hasNext()) {
                        sb.append(" " + ((LinkAddress) it.next()));
                    }
                    if (jniSetAddresses(strJniGetName, sb.toString()) < 1) {
                        throw new IllegalArgumentException("At least one address must be specified");
                    }
                    Connection connection2 = new Connection();
                    if (!this.mContext.bindServiceAsUser(intent, connection2, 67108865, new UserHandle(this.mUserHandle))) {
                        throw new IllegalStateException("Cannot bind " + vpnConfig.user);
                    }
                    this.mConnection = connection2;
                    this.mInterface = strJniGetName;
                    vpnConfig.user = this.mPackage;
                    vpnConfig.interfaze = this.mInterface;
                    vpnConfig.startTime = SystemClock.elapsedRealtime();
                    this.mConfig = vpnConfig;
                    if (vpnConfig2 == null || !updateLinkPropertiesInPlaceIfPossible(this.mNetworkAgent, vpnConfig2)) {
                        this.mNetworkAgent = null;
                        updateState(NetworkInfo.DetailedState.CONNECTING, "establish");
                        agentConnect();
                        agentDisconnect(networkAgent);
                    }
                    if (connection != null) {
                        this.mContext.unbindService(connection);
                    }
                    if (str != null && !str.equals(strJniGetName)) {
                        jniReset(str);
                    }
                    try {
                        IoUtils.setBlocking(parcelFileDescriptorAdoptFd.getFileDescriptor(), vpnConfig.blocking);
                        Log.i(TAG, "Established by " + vpnConfig.user + " on " + this.mInterface);
                        return parcelFileDescriptorAdoptFd;
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot set tunnel's fd as blocking=" + vpnConfig.blocking, e);
                    }
                } catch (RuntimeException e2) {
                    IoUtils.closeQuietly(parcelFileDescriptorAdoptFd);
                    agentDisconnect();
                    this.mConfig = vpnConfig2;
                    this.mConnection = connection;
                    this.mNetworkCapabilities.setUids(uids);
                    this.mNetworkAgent = networkAgent;
                    this.mInterface = str;
                    throw e2;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (RemoteException e3) {
            throw new SecurityException("Cannot find " + vpnConfig.user);
        }
    }

    private boolean isRunningLocked() {
        return (this.mNetworkAgent == null || this.mInterface == null) ? false : true;
    }

    @VisibleForTesting
    protected boolean isCallerEstablishedOwnerLocked() {
        return isRunningLocked() && Binder.getCallingUid() == this.mOwnerUID;
    }

    private SortedSet<Integer> getAppsUids(List<String> list, int i) {
        TreeSet treeSet = new TreeSet();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            int appUid = getAppUid(it.next(), i);
            if (appUid != -1) {
                treeSet.add(Integer.valueOf(appUid));
            }
        }
        return treeSet;
    }

    @VisibleForTesting
    Set<UidRange> createUserAndRestrictedProfilesRanges(int i, List<String> list, List<String> list2) {
        ArraySet arraySet = new ArraySet();
        addUserToRanges(arraySet, i, list, list2);
        if (canHaveRestrictedProfile(i)) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = UserManager.get(this.mContext).getUsers(true);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                for (UserInfo userInfo : users) {
                    if (userInfo.isRestricted() && userInfo.restrictedProfileParentId == i) {
                        addUserToRanges(arraySet, userInfo.id, list, list2);
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        return arraySet;
    }

    @VisibleForTesting
    void addUserToRanges(Set<UidRange> set, int i, List<String> list, List<String> list2) {
        if (list != null) {
            Iterator<Integer> it = getAppsUids(list, i).iterator();
            int i2 = -1;
            int i3 = -1;
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                if (i2 != -1) {
                    if (iIntValue != i3 + 1) {
                        set.add(new UidRange(i2, i3));
                        i2 = iIntValue;
                    }
                } else {
                    i2 = iIntValue;
                }
                i3 = iIntValue;
            }
            if (i2 != -1) {
                set.add(new UidRange(i2, i3));
                return;
            }
            return;
        }
        if (list2 != null) {
            UidRange uidRangeCreateForUser = UidRange.createForUser(i);
            int i4 = uidRangeCreateForUser.start;
            Iterator<Integer> it2 = getAppsUids(list2, i).iterator();
            while (it2.hasNext()) {
                int iIntValue2 = it2.next().intValue();
                if (iIntValue2 == i4) {
                    i4++;
                } else {
                    set.add(new UidRange(i4, iIntValue2 - 1));
                    i4 = iIntValue2 + 1;
                }
            }
            if (i4 <= uidRangeCreateForUser.stop) {
                set.add(new UidRange(i4, uidRangeCreateForUser.stop));
                return;
            }
            return;
        }
        set.add(UidRange.createForUser(i));
    }

    private static List<UidRange> uidRangesForUser(int i, Set<UidRange> set) {
        UidRange uidRangeCreateForUser = UidRange.createForUser(i);
        ArrayList arrayList = new ArrayList();
        for (UidRange uidRange : set) {
            if (uidRangeCreateForUser.containsRange(uidRange)) {
                arrayList.add(uidRange);
            }
        }
        return arrayList;
    }

    public void onUserAdded(int i) {
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(i);
        if (userInfo.isRestricted() && userInfo.restrictedProfileParentId == this.mUserHandle) {
            synchronized (this) {
                Set uids = this.mNetworkCapabilities.getUids();
                if (uids != null) {
                    try {
                        addUserToRanges(uids, i, this.mConfig.allowedApplications, this.mConfig.disallowedApplications);
                        this.mNetworkCapabilities.setUids(uids);
                        updateCapabilities();
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed to add restricted user to owner", e);
                    }
                    setVpnForcedLocked(this.mLockdown);
                } else {
                    setVpnForcedLocked(this.mLockdown);
                }
            }
        }
    }

    public void onUserRemoved(int i) {
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(i);
        if (userInfo.isRestricted() && userInfo.restrictedProfileParentId == this.mUserHandle) {
            synchronized (this) {
                Set uids = this.mNetworkCapabilities.getUids();
                if (uids != null) {
                    try {
                        uids.removeAll(uidRangesForUser(i, uids));
                        this.mNetworkCapabilities.setUids(uids);
                        updateCapabilities();
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed to remove restricted user to owner", e);
                    }
                    setVpnForcedLocked(this.mLockdown);
                } else {
                    setVpnForcedLocked(this.mLockdown);
                }
            }
        }
    }

    public synchronized void onUserStopped() {
        setVpnForcedLocked(false);
        this.mAlwaysOn = false;
        unregisterPackageChangeReceiverLocked();
        agentDisconnect();
    }

    @GuardedBy("this")
    private void setVpnForcedLocked(boolean z) {
        List<String> listSingletonList;
        if (!isNullOrLegacyVpn(this.mPackage)) {
            listSingletonList = Collections.singletonList(this.mPackage);
        } else {
            listSingletonList = null;
        }
        ArraySet arraySet = new ArraySet(this.mBlockedUsers);
        Set<UidRange> setEmptySet = Collections.emptySet();
        if (z) {
            setEmptySet = createUserAndRestrictedProfilesRanges(this.mUserHandle, null, listSingletonList);
            for (UidRange uidRange : setEmptySet) {
                if (uidRange.start == 0) {
                    setEmptySet.remove(uidRange);
                    if (uidRange.stop != 0) {
                        setEmptySet.add(new UidRange(1, uidRange.stop));
                    }
                }
            }
            arraySet.removeAll(setEmptySet);
            setEmptySet.removeAll(this.mBlockedUsers);
        }
        setAllowOnlyVpnForUids(false, arraySet);
        setAllowOnlyVpnForUids(true, setEmptySet);
    }

    @GuardedBy("this")
    private boolean setAllowOnlyVpnForUids(boolean z, Collection<UidRange> collection) {
        if (collection.size() == 0) {
            return true;
        }
        try {
            this.mNetd.setAllowOnlyVpnForUids(z, (UidRange[]) collection.toArray(new UidRange[collection.size()]));
            if (z) {
                this.mBlockedUsers.addAll(collection);
            } else {
                this.mBlockedUsers.removeAll(collection);
            }
            return true;
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Updating blocked=" + z + " for UIDs " + Arrays.toString(collection.toArray()) + " failed", e);
            return false;
        }
    }

    public VpnConfig getVpnConfig() {
        enforceControlPermission();
        return this.mConfig;
    }

    @Deprecated
    public synchronized void interfaceStatusChanged(String str, boolean z) {
        try {
            this.mObserver.interfaceStatusChanged(str, z);
        } catch (RemoteException e) {
        }
    }

    private void enforceControlPermission() {
        this.mContext.enforceCallingPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceControlPermissionOrInternalCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", "Unauthorized Caller");
    }

    private class Connection implements ServiceConnection {
        private IBinder mService;

        private Connection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            this.mService = iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            this.mService = null;
        }
    }

    private void prepareStatusIntent() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mStatusIntent = VpnConfig.getIntentForStatusPanel(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public synchronized boolean addAddress(String str, int i) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean zJniAddAddress = jniAddAddress(this.mInterface, str, i);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return zJniAddAddress;
    }

    public synchronized boolean removeAddress(String str, int i) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean zJniDelAddress = jniDelAddress(this.mInterface, str, i);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return zJniDelAddress;
    }

    public synchronized boolean setUnderlyingNetworks(Network[] networkArr) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        if (networkArr == null) {
            this.mConfig.underlyingNetworks = null;
        } else {
            this.mConfig.underlyingNetworks = new Network[networkArr.length];
            for (int i = 0; i < networkArr.length; i++) {
                if (networkArr[i] == null) {
                    this.mConfig.underlyingNetworks[i] = null;
                } else {
                    this.mConfig.underlyingNetworks[i] = new Network(networkArr[i].netId);
                }
            }
        }
        updateCapabilities();
        return true;
    }

    public synchronized Network[] getUnderlyingNetworks() {
        if (!isRunningLocked()) {
            return null;
        }
        return this.mConfig.underlyingNetworks;
    }

    public synchronized VpnInfo getVpnInfo() {
        if (!isRunningLocked()) {
            return null;
        }
        VpnInfo vpnInfo = new VpnInfo();
        vpnInfo.ownerUid = this.mOwnerUID;
        vpnInfo.vpnIface = this.mInterface;
        return vpnInfo;
    }

    public synchronized boolean appliesToUid(int i) {
        if (!isRunningLocked()) {
            return false;
        }
        return this.mNetworkCapabilities.appliesToUid(i);
    }

    public synchronized boolean isBlockingUid(int i) {
        if (!this.mLockdown) {
            return false;
        }
        if (this.mNetworkInfo.isConnected()) {
            return !appliesToUid(i);
        }
        Iterator<UidRange> it = this.mBlockedUsers.iterator();
        while (it.hasNext()) {
            if (it.next().contains(i)) {
                return true;
            }
        }
        return false;
    }

    private void updateAlwaysOnNotification(NetworkInfo.DetailedState detailedState) {
        boolean z;
        if (!this.mAlwaysOn || detailedState == NetworkInfo.DetailedState.CONNECTED) {
            z = false;
        } else {
            z = true;
        }
        UserHandle userHandleOf = UserHandle.of(this.mUserHandle);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            NotificationManager notificationManagerFrom = NotificationManager.from(this.mContext);
            if (!z) {
                notificationManagerFrom.cancelAsUser(TAG, 17, userHandleOf);
                return;
            }
            Intent intent = new Intent();
            intent.setComponent(ComponentName.unflattenFromString(this.mContext.getString(R.string.accessibility_system_action_on_screen_a11y_shortcut_chooser_label)));
            intent.putExtra("lockdown", this.mLockdown);
            intent.addFlags(268435456);
            notificationManagerFrom.notifyAsUser(TAG, 17, new Notification.Builder(this.mContext, SystemNotificationChannels.VPN).setSmallIcon(R.drawable.popup_background_mtrl_mult).setContentTitle(this.mContext.getString(R.string.needPuk2)).setContentText(this.mContext.getString(R.string.nas_upgrade_notification_learn_more_content)).setContentIntent(this.mSystemServices.pendingIntentGetActivityAsUser(intent, 201326592, userHandleOf)).setCategory("sys").setVisibility(1).setOngoing(true).setColor(this.mContext.getColor(R.color.car_colorPrimary)).build(), userHandleOf);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting
    public static class SystemServices {
        private final Context mContext;

        public SystemServices(Context context) {
            this.mContext = context;
        }

        public PendingIntent pendingIntentGetActivityAsUser(Intent intent, int i, UserHandle userHandle) {
            return PendingIntent.getActivityAsUser(this.mContext, 0, intent, i, null, userHandle);
        }

        public void settingsSecurePutStringForUser(String str, String str2, int i) {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), str, str2, i);
        }

        public void settingsSecurePutIntForUser(String str, int i, int i2) {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), str, i, i2);
        }

        public String settingsSecureGetStringForUser(String str, int i) {
            return Settings.Secure.getStringForUser(this.mContext.getContentResolver(), str, i);
        }

        public int settingsSecureGetIntForUser(String str, int i, int i2) {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), str, i, i2);
        }
    }

    private static RouteInfo findIPv4DefaultRoute(LinkProperties linkProperties) {
        for (RouteInfo routeInfo : linkProperties.getAllRoutes()) {
            if (routeInfo.isDefaultRoute() && (routeInfo.getGateway() instanceof Inet4Address)) {
                return routeInfo;
            }
        }
        throw new IllegalStateException("Unable to find IPv4 default gateway");
    }

    public void startLegacyVpn(VpnProfile vpnProfile, KeyStore keyStore, LinkProperties linkProperties) {
        enforceControlPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            startLegacyVpnPrivileged(vpnProfile, keyStore, linkProperties);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void startLegacyVpnPrivileged(VpnProfile vpnProfile, KeyStore keyStore, LinkProperties linkProperties) {
        String[] strArr;
        String[] strArr2;
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager.getUserInfo(this.mUserHandle).isRestricted() || userManager.hasUserRestriction("no_config_vpn", new UserHandle(this.mUserHandle))) {
            throw new SecurityException("Restricted users cannot establish VPNs");
        }
        RouteInfo routeInfoFindIPv4DefaultRoute = findIPv4DefaultRoute(linkProperties);
        String hostAddress = routeInfoFindIPv4DefaultRoute.getGateway().getHostAddress();
        String str = routeInfoFindIPv4DefaultRoute.getInterface();
        String str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String str3 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String str4 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String str5 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (!vpnProfile.ipsecUserCert.isEmpty()) {
            str2 = "USRPKEY_" + vpnProfile.ipsecUserCert;
            byte[] bArr = keyStore.get("USRCERT_" + vpnProfile.ipsecUserCert);
            if (bArr != null) {
                str3 = new String(bArr, StandardCharsets.UTF_8);
            } else {
                str3 = null;
            }
        }
        if (!vpnProfile.ipsecCaCert.isEmpty()) {
            byte[] bArr2 = keyStore.get("CACERT_" + vpnProfile.ipsecCaCert);
            if (bArr2 != null) {
                str4 = new String(bArr2, StandardCharsets.UTF_8);
            } else {
                str4 = null;
            }
        }
        if (!vpnProfile.ipsecServerCert.isEmpty()) {
            byte[] bArr3 = keyStore.get("USRCERT_" + vpnProfile.ipsecServerCert);
            if (bArr3 != null) {
                str5 = new String(bArr3, StandardCharsets.UTF_8);
            } else {
                str5 = null;
            }
        }
        if (str2 == null || str3 == null || str4 == null || str5 == null) {
            throw new IllegalStateException("Cannot load credentials");
        }
        switch (vpnProfile.type) {
            case 1:
                strArr = new String[]{str, vpnProfile.server, "udppsk", vpnProfile.ipsecIdentifier, vpnProfile.ipsecSecret, "1701"};
                break;
            case 2:
                strArr = new String[]{str, vpnProfile.server, "udprsa", str2, str3, str4, str5, "1701"};
                break;
            case 3:
                strArr = new String[]{str, vpnProfile.server, "xauthpsk", vpnProfile.ipsecIdentifier, vpnProfile.ipsecSecret, vpnProfile.username, vpnProfile.password, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, hostAddress};
                break;
            case 4:
                strArr = new String[]{str, vpnProfile.server, "xauthrsa", str2, str3, str4, str5, vpnProfile.username, vpnProfile.password, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, hostAddress};
                break;
            case 5:
                strArr = new String[]{str, vpnProfile.server, "hybridrsa", str4, str5, vpnProfile.username, vpnProfile.password, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, hostAddress};
                break;
            default:
                strArr = null;
                break;
        }
        switch (vpnProfile.type) {
            case 0:
                strArr2 = new String[20];
                strArr2[0] = str;
                strArr2[1] = "pptp";
                strArr2[2] = vpnProfile.server;
                strArr2[3] = "1723";
                strArr2[4] = com.android.server.pm.Settings.ATTR_NAME;
                strArr2[5] = vpnProfile.username;
                strArr2[6] = "password";
                strArr2[7] = vpnProfile.password;
                strArr2[8] = "linkname";
                strArr2[9] = "vpn";
                strArr2[10] = "refuse-eap";
                strArr2[11] = "nodefaultroute";
                strArr2[12] = "usepeerdns";
                strArr2[13] = "idle";
                strArr2[14] = "1800";
                strArr2[15] = "mtu";
                strArr2[16] = "1400";
                strArr2[17] = "mru";
                strArr2[18] = "1400";
                strArr2[19] = vpnProfile.mppe ? "+mppe" : "nomppe";
                break;
            case 1:
            case 2:
                strArr2 = new String[]{str, "l2tp", vpnProfile.server, "1701", vpnProfile.l2tpSecret, com.android.server.pm.Settings.ATTR_NAME, vpnProfile.username, "password", vpnProfile.password, "linkname", "vpn", "refuse-eap", "nodefaultroute", "usepeerdns", "idle", "1800", "mtu", "1400", "mru", "1400"};
                break;
            default:
                strArr2 = null;
                break;
        }
        VpnConfig vpnConfig = new VpnConfig();
        vpnConfig.legacy = true;
        vpnConfig.user = vpnProfile.key;
        vpnConfig.interfaze = str;
        vpnConfig.session = vpnProfile.name;
        vpnConfig.addLegacyRoutes(vpnProfile.routes);
        if (!vpnProfile.dnsServers.isEmpty()) {
            vpnConfig.dnsServers = Arrays.asList(vpnProfile.dnsServers.split(" +"));
        }
        if (!vpnProfile.searchDomains.isEmpty()) {
            vpnConfig.searchDomains = Arrays.asList(vpnProfile.searchDomains.split(" +"));
        }
        startLegacyVpn(vpnConfig, strArr, strArr2);
    }

    private synchronized void startLegacyVpn(VpnConfig vpnConfig, String[] strArr, String[] strArr2) {
        stopLegacyVpnPrivileged();
        prepareInternal("[Legacy VPN]");
        updateState(NetworkInfo.DetailedState.CONNECTING, "startLegacyVpn");
        this.mLegacyVpnRunner = new LegacyVpnRunner(vpnConfig, strArr, strArr2);
        this.mLegacyVpnRunner.start();
    }

    public synchronized void stopLegacyVpnPrivileged() {
        if (this.mLegacyVpnRunner != null) {
            this.mLegacyVpnRunner.exit();
            this.mLegacyVpnRunner = null;
            synchronized ("LegacyVpnRunner") {
            }
        }
    }

    public synchronized LegacyVpnInfo getLegacyVpnInfo() {
        enforceControlPermission();
        return getLegacyVpnInfoPrivileged();
    }

    public synchronized LegacyVpnInfo getLegacyVpnInfoPrivileged() {
        if (this.mLegacyVpnRunner == null) {
            return null;
        }
        LegacyVpnInfo legacyVpnInfo = new LegacyVpnInfo();
        legacyVpnInfo.key = this.mConfig.user;
        legacyVpnInfo.state = LegacyVpnInfo.stateFromNetworkInfo(this.mNetworkInfo);
        if (this.mNetworkInfo.isConnected()) {
            legacyVpnInfo.intent = this.mStatusIntent;
        }
        return legacyVpnInfo;
    }

    public VpnConfig getLegacyVpnConfig() {
        if (this.mLegacyVpnRunner != null) {
            return this.mConfig;
        }
        return null;
    }

    private class LegacyVpnRunner extends Thread {
        private static final String TAG = "LegacyVpnRunner";
        private final String[][] mArguments;
        private long mBringupStartTime;
        private final BroadcastReceiver mBroadcastReceiver;
        private final String[] mDaemons;
        private final AtomicInteger mOuterConnection;
        private final String mOuterInterface;
        private final LocalSocket[] mSockets;

        public LegacyVpnRunner(VpnConfig vpnConfig, String[] strArr, String[] strArr2) {
            NetworkInfo networkInfo;
            super(TAG);
            this.mOuterConnection = new AtomicInteger(-1);
            this.mBringupStartTime = -1L;
            this.mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo networkInfo2;
                    if (Vpn.this.mEnableTeardown && intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE") && intent.getIntExtra("networkType", -1) == LegacyVpnRunner.this.mOuterConnection.get() && (networkInfo2 = (NetworkInfo) intent.getExtra("networkInfo")) != null && !networkInfo2.isConnectedOrConnecting()) {
                        try {
                            Vpn.this.mObserver.interfaceStatusChanged(LegacyVpnRunner.this.mOuterInterface, false);
                        } catch (RemoteException e) {
                        }
                    }
                }
            };
            Vpn.this.mConfig = vpnConfig;
            this.mDaemons = new String[]{"racoon", "mtpd"};
            this.mArguments = new String[][]{strArr, strArr2};
            this.mSockets = new LocalSocket[this.mDaemons.length];
            this.mOuterInterface = Vpn.this.mConfig.interfaze;
            if (!TextUtils.isEmpty(this.mOuterInterface)) {
                ConnectivityManager connectivityManagerFrom = ConnectivityManager.from(Vpn.this.mContext);
                for (Network network : connectivityManagerFrom.getAllNetworks()) {
                    LinkProperties linkProperties = connectivityManagerFrom.getLinkProperties(network);
                    if (linkProperties != null && linkProperties.getAllInterfaceNames().contains(this.mOuterInterface) && (networkInfo = connectivityManagerFrom.getNetworkInfo(network)) != null) {
                        this.mOuterConnection.set(networkInfo.getType());
                    }
                }
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            Vpn.this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        }

        public void check(String str) {
            if (str.equals(this.mOuterInterface)) {
                Log.i(TAG, "Legacy VPN is going down with " + str);
                exit();
            }
        }

        public void exit() {
            interrupt();
            Vpn.this.agentDisconnect();
            try {
                Vpn.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            } catch (IllegalArgumentException e) {
            }
        }

        @Override
        public void run() {
            Log.v(TAG, "Waiting");
            synchronized (TAG) {
                Log.v(TAG, "Executing");
                int i = 0;
                try {
                    bringup();
                    waitForDaemonsToStop();
                    interrupted();
                    for (LocalSocket localSocket : this.mSockets) {
                        IoUtils.closeQuietly(localSocket);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                    }
                    String[] strArr = this.mDaemons;
                    int length = strArr.length;
                    while (i < length) {
                        SystemService.stop(strArr[i]);
                        i++;
                    }
                } catch (InterruptedException e2) {
                    for (LocalSocket localSocket2 : this.mSockets) {
                        IoUtils.closeQuietly(localSocket2);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e3) {
                    }
                    String[] strArr2 = this.mDaemons;
                    int length2 = strArr2.length;
                    while (i < length2) {
                        SystemService.stop(strArr2[i]);
                        i++;
                    }
                } catch (Throwable th) {
                    for (LocalSocket localSocket3 : this.mSockets) {
                        IoUtils.closeQuietly(localSocket3);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e4) {
                    }
                    String[] strArr3 = this.mDaemons;
                    int length3 = strArr3.length;
                    while (i < length3) {
                        SystemService.stop(strArr3[i]);
                        i++;
                    }
                    throw th;
                }
                Vpn.this.agentDisconnect();
            }
        }

        private void checkInterruptAndDelay(boolean z) throws InterruptedException {
            if (SystemClock.elapsedRealtime() - this.mBringupStartTime <= 60000) {
                Thread.sleep(z ? 200L : 1L);
            } else {
                Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, "checkpoint");
                throw new IllegalStateException("VPN bringup took too long");
            }
        }

        private void bringup() {
            try {
                this.mBringupStartTime = SystemClock.elapsedRealtime();
                for (String str : this.mDaemons) {
                    while (!SystemService.isStopped(str)) {
                        checkInterruptAndDelay(true);
                    }
                }
                File file = new File("/data/misc/vpn/state");
                file.delete();
                if (file.exists()) {
                    throw new IllegalStateException("Cannot delete the state");
                }
                new File("/data/misc/vpn/abort").delete();
                boolean z = false;
                for (String[] strArr : this.mArguments) {
                    z = z || strArr != null;
                }
                if (!z) {
                    Vpn.this.agentDisconnect();
                    return;
                }
                Vpn.this.updateState(NetworkInfo.DetailedState.CONNECTING, "execute");
                for (int i = 0; i < this.mDaemons.length; i++) {
                    String[] strArr2 = this.mArguments[i];
                    if (strArr2 != null) {
                        String str2 = this.mDaemons[i];
                        SystemService.start(str2);
                        while (!SystemService.isRunning(str2)) {
                            checkInterruptAndDelay(true);
                        }
                        this.mSockets[i] = new LocalSocket();
                        LocalSocketAddress localSocketAddress = new LocalSocketAddress(str2, LocalSocketAddress.Namespace.RESERVED);
                        while (true) {
                            try {
                                this.mSockets[i].connect(localSocketAddress);
                                break;
                            } catch (Exception e) {
                                checkInterruptAndDelay(true);
                            }
                        }
                        this.mSockets[i].setSoTimeout(com.android.server.SystemService.PHASE_SYSTEM_SERVICES_READY);
                        OutputStream outputStream = this.mSockets[i].getOutputStream();
                        for (String str3 : strArr2) {
                            byte[] bytes = str3.getBytes(StandardCharsets.UTF_8);
                            if (bytes.length >= 65535) {
                                throw new IllegalArgumentException("Argument is too large");
                            }
                            outputStream.write(bytes.length >> 8);
                            outputStream.write(bytes.length);
                            outputStream.write(bytes);
                            checkInterruptAndDelay(false);
                        }
                        outputStream.write(255);
                        outputStream.write(255);
                        outputStream.flush();
                        InputStream inputStream = this.mSockets[i].getInputStream();
                        while (inputStream.read() != -1) {
                            checkInterruptAndDelay(true);
                        }
                    }
                }
                while (!file.exists()) {
                    for (int i2 = 0; i2 < this.mDaemons.length; i2++) {
                        String str4 = this.mDaemons[i2];
                        if (this.mArguments[i2] != null && !SystemService.isRunning(str4)) {
                            throw new IllegalStateException(str4 + " is dead");
                        }
                    }
                    checkInterruptAndDelay(true);
                }
                String[] strArrSplit = FileUtils.readTextFile(file, 0, null).split("\n", -1);
                if (strArrSplit.length != 7) {
                    throw new IllegalStateException("Cannot parse the state");
                }
                Vpn.this.mConfig.interfaze = strArrSplit[0].trim();
                Vpn.this.mConfig.addLegacyAddresses(strArrSplit[1]);
                if (Vpn.this.mConfig.routes == null || Vpn.this.mConfig.routes.isEmpty()) {
                    Vpn.this.mConfig.addLegacyRoutes(strArrSplit[2]);
                }
                if (Vpn.this.mConfig.dnsServers == null || Vpn.this.mConfig.dnsServers.size() == 0) {
                    String strTrim = strArrSplit[3].trim();
                    if (!strTrim.isEmpty()) {
                        Vpn.this.mConfig.dnsServers = Arrays.asList(strTrim.split(" "));
                    }
                }
                if (Vpn.this.mConfig.searchDomains == null || Vpn.this.mConfig.searchDomains.size() == 0) {
                    String strTrim2 = strArrSplit[4].trim();
                    if (!strTrim2.isEmpty()) {
                        Vpn.this.mConfig.searchDomains = Arrays.asList(strTrim2.split(" "));
                    }
                }
                String str5 = strArrSplit[5];
                if (!str5.isEmpty()) {
                    try {
                        InetAddress numericAddress = InetAddress.parseNumericAddress(str5);
                        if (numericAddress instanceof Inet4Address) {
                            Vpn.this.mConfig.routes.add(new RouteInfo(new IpPrefix(numericAddress, 32), 9));
                        } else if (numericAddress instanceof Inet6Address) {
                            Vpn.this.mConfig.routes.add(new RouteInfo(new IpPrefix(numericAddress, 128), 9));
                        } else {
                            Log.e(TAG, "Unknown IP address family for VPN endpoint: " + str5);
                        }
                    } catch (IllegalArgumentException e2) {
                        Log.e(TAG, "Exception constructing throw route to " + str5 + ": " + e2);
                    }
                }
                synchronized (Vpn.this) {
                    Vpn.this.mConfig.startTime = SystemClock.elapsedRealtime();
                    checkInterruptAndDelay(false);
                    if (Vpn.this.jniCheck(Vpn.this.mConfig.interfaze) != 0) {
                        Vpn.this.mInterface = Vpn.this.mConfig.interfaze;
                        Vpn.this.prepareStatusIntent();
                        Vpn.this.agentConnect();
                        Log.i(TAG, "Connected!");
                    } else {
                        throw new IllegalStateException(Vpn.this.mConfig.interfaze + " is gone");
                    }
                }
            } catch (Exception e3) {
                Log.i(TAG, "Aborting", e3);
                Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, e3.getMessage());
                exit();
            }
        }

        private void waitForDaemonsToStop() throws InterruptedException {
            if (!Vpn.this.mNetworkInfo.isConnected()) {
                return;
            }
            while (true) {
                Thread.sleep(2000L);
                for (int i = 0; i < this.mDaemons.length; i++) {
                    if (this.mArguments[i] != null && SystemService.isStopped(this.mDaemons[i])) {
                        return;
                    }
                }
            }
        }
    }
}
