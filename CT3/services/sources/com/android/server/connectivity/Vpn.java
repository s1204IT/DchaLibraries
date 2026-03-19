package com.android.server.connectivity;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.Uri;
import android.net.dhcp.DhcpPacket;
import android.os.Binder;
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
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.usb.UsbAudioDevice;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;

public class Vpn {
    private static final boolean LOGD = true;
    private static final String NETWORKTYPE = "VPN";
    private static final String TAG = "Vpn";
    private VpnConfig mConfig;
    private Connection mConnection;
    private Context mContext;
    private String mInterface;
    private LegacyVpnRunner mLegacyVpnRunner;
    private final Looper mLooper;
    private final INetworkManagementService mNetd;
    private NetworkAgent mNetworkAgent;
    private final NetworkCapabilities mNetworkCapabilities;
    private NetworkInfo mNetworkInfo;
    private int mOwnerUID;
    private PendingIntent mStatusIntent;
    private final int mUserHandle;
    private volatile boolean mEnableTeardown = true;
    private boolean mAlwaysOn = false;
    private boolean mLockdown = false;

    @GuardedBy("this")
    private Set<UidRange> mVpnUsers = null;

    @GuardedBy("this")
    private Set<UidRange> mBlockedUsers = new ArraySet();
    private final BroadcastReceiver mPackageIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            String packageName = data != null ? data.getSchemeSpecificPart() : null;
            if (packageName == null) {
                return;
            }
            synchronized (Vpn.this) {
                if (!packageName.equals(Vpn.this.getAlwaysOnPackage())) {
                    return;
                }
                String action = intent.getAction();
                Log.i(Vpn.TAG, "Received broadcast " + action + " for always-on package " + packageName + " in user " + Vpn.this.mUserHandle);
                if (!action.equals("android.intent.action.PACKAGE_REPLACED")) {
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        boolean isPackageRemoved = intent.getBooleanExtra("android.intent.extra.REPLACING", false) ? false : true;
                        if (isPackageRemoved) {
                            Vpn.this.setAndSaveAlwaysOnPackage(null, false);
                        }
                    }
                } else {
                    Vpn.this.startAlwaysOnVpn();
                }
            }
        }
    };
    private boolean mIsPackageIntentReceiverRegistered = false;
    private INetworkManagementEventObserver mObserver = new BaseNetworkObserver() {
        public void interfaceStatusChanged(String interfaze, boolean up) {
            synchronized (Vpn.this) {
                if (!up) {
                    if (Vpn.this.mLegacyVpnRunner != null) {
                        Vpn.this.mLegacyVpnRunner.check(interfaze);
                    }
                }
            }
        }

        public void interfaceRemoved(String interfaze) {
            synchronized (Vpn.this) {
                if (interfaze.equals(Vpn.this.mInterface) && Vpn.this.jniCheck(interfaze) == 0) {
                    Vpn.this.mStatusIntent = null;
                    Vpn.this.mVpnUsers = null;
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
    private String mPackage = "[Legacy VPN]";

    private native boolean jniAddAddress(String str, String str2, int i);

    private native int jniCheck(String str);

    private native int jniCreate(int i);

    private native boolean jniDelAddress(String str, String str2, int i);

    private native String jniGetName(int i);

    private native void jniReset(String str);

    private native int jniSetAddresses(String str, String str2);

    public Vpn(Looper looper, Context context, INetworkManagementService netService, int userHandle) {
        this.mContext = context;
        this.mNetd = netService;
        this.mUserHandle = userHandle;
        this.mLooper = looper;
        this.mOwnerUID = getAppUid(this.mPackage, this.mUserHandle);
        try {
            netService.registerObserver(this.mObserver);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Problem registering observer", e);
        }
        this.mNetworkInfo = new NetworkInfo(17, 0, NETWORKTYPE, "");
        this.mNetworkCapabilities = new NetworkCapabilities();
        this.mNetworkCapabilities.addTransportType(4);
        this.mNetworkCapabilities.removeCapability(15);
    }

    public void setEnableTeardown(boolean enableTeardown) {
        this.mEnableTeardown = enableTeardown;
    }

    private void updateState(NetworkInfo.DetailedState detailedState, String reason) {
        Log.d(TAG, "setting state=" + detailedState + ", reason=" + reason);
        this.mNetworkInfo.setDetailedState(detailedState, reason, null);
        if (this.mNetworkAgent == null) {
            return;
        }
        this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
    }

    public synchronized boolean setAlwaysOnPackage(String packageName, boolean lockdown) {
        enforceControlPermissionOrInternalCaller();
        if ("[Legacy VPN]".equals(packageName)) {
            Log.w(TAG, "Not setting legacy VPN \"" + packageName + "\" as always-on.");
            return false;
        }
        if (packageName != null) {
            if (!setPackageAuthorization(packageName, true)) {
                return false;
            }
            this.mAlwaysOn = true;
        } else {
            packageName = "[Legacy VPN]";
            this.mAlwaysOn = false;
        }
        if (!this.mAlwaysOn) {
            lockdown = false;
        }
        this.mLockdown = lockdown;
        if (!isCurrentPreparedPackage(packageName)) {
            prepareInternal(packageName);
        }
        maybeRegisterPackageChangeReceiverLocked(packageName);
        setVpnForcedLocked(this.mLockdown);
        return true;
    }

    private static boolean isNullOrLegacyVpn(String packageName) {
        if (packageName != null) {
            return "[Legacy VPN]".equals(packageName);
        }
        return true;
    }

    private void unregisterPackageChangeReceiverLocked() {
        if (!this.mIsPackageIntentReceiverRegistered) {
            return;
        }
        this.mContext.unregisterReceiver(this.mPackageIntentReceiver);
        this.mIsPackageIntentReceiverRegistered = false;
    }

    private void maybeRegisterPackageChangeReceiverLocked(String packageName) {
        unregisterPackageChangeReceiverLocked();
        if (isNullOrLegacyVpn(packageName)) {
            return;
        }
        this.mIsPackageIntentReceiverRegistered = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart(packageName, 0);
        this.mContext.registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.of(this.mUserHandle), intentFilter, null, null);
    }

    public synchronized String getAlwaysOnPackage() {
        enforceControlPermissionOrInternalCaller();
        return this.mAlwaysOn ? this.mPackage : null;
    }

    public synchronized void saveAlwaysOnPackage() {
        long token = Binder.clearCallingIdentity();
        try {
            ContentResolver cr = this.mContext.getContentResolver();
            Settings.Secure.putStringForUser(cr, "always_on_vpn_app", getAlwaysOnPackage(), this.mUserHandle);
            Settings.Secure.putIntForUser(cr, "always_on_vpn_lockdown", this.mLockdown ? 1 : 0, this.mUserHandle);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private synchronized boolean setAndSaveAlwaysOnPackage(String packageName, boolean lockdown) {
        if (setAlwaysOnPackage(packageName, lockdown)) {
            saveAlwaysOnPackage();
            return true;
        }
        return false;
    }

    public boolean startAlwaysOnVpn() {
        synchronized (this) {
            String alwaysOnPackage = getAlwaysOnPackage();
            if (alwaysOnPackage == null) {
                return true;
            }
            if (getNetworkInfo().isConnected()) {
                return true;
            }
            Intent serviceIntent = new Intent("android.net.VpnService");
            serviceIntent.setPackage(alwaysOnPackage);
            try {
                return this.mContext.startServiceAsUser(serviceIntent, UserHandle.of(this.mUserHandle)) != null;
            } catch (RuntimeException e) {
                Log.e(TAG, "VpnService " + serviceIntent + " failed to start", e);
                return false;
            }
        }
    }

    public synchronized boolean prepare(String oldPackage, String newPackage) {
        Log.i(TAG, "prepare old:" + oldPackage + ",new:" + newPackage);
        if (oldPackage != null) {
            if (this.mAlwaysOn && !isCurrentPreparedPackage(oldPackage)) {
                return false;
            }
            if (!isCurrentPreparedPackage(oldPackage)) {
                if (oldPackage.equals("[Legacy VPN]") || !isVpnUserPreConsented(oldPackage)) {
                    return false;
                }
                prepareInternal(oldPackage);
                return true;
            }
            if (!oldPackage.equals("[Legacy VPN]") && !isVpnUserPreConsented(oldPackage)) {
                prepareInternal("[Legacy VPN]");
                return false;
            }
        }
        if (newPackage == null || (!newPackage.equals("[Legacy VPN]") && isCurrentPreparedPackage(newPackage))) {
            return true;
        }
        enforceControlPermission();
        if (this.mAlwaysOn && !isCurrentPreparedPackage(newPackage)) {
            return false;
        }
        prepareInternal(newPackage);
        return true;
    }

    private boolean isCurrentPreparedPackage(String packageName) {
        return getAppUid(packageName, this.mUserHandle) == this.mOwnerUID;
    }

    private void prepareInternal(String newPackage) {
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mInterface != null) {
                this.mStatusIntent = null;
                agentDisconnect();
                jniReset(this.mInterface);
                this.mInterface = null;
                this.mVpnUsers = null;
            }
            if (this.mConnection != null) {
                try {
                    this.mConnection.mService.transact(UsbAudioDevice.kAudioDeviceClassMask, Parcel.obtain(), null, 1);
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
            Log.i(TAG, "Switched from " + this.mPackage + " to " + newPackage);
            this.mPackage = newPackage;
            this.mOwnerUID = getAppUid(newPackage, this.mUserHandle);
            try {
                this.mNetd.allowProtect(this.mOwnerUID);
            } catch (Exception e3) {
                Log.wtf(TAG, "Failed to allow UID " + this.mOwnerUID + " to call protect() " + e3);
            }
            this.mConfig = null;
            updateState(NetworkInfo.DetailedState.IDLE, "prepare");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean setPackageAuthorization(String packageName, boolean authorized) {
        enforceControlPermissionOrInternalCaller();
        int uid = getAppUid(packageName, this.mUserHandle);
        if (uid == -1 || "[Legacy VPN]".equals(packageName)) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
            appOps.setMode(47, uid, packageName, authorized ? 0 : 1);
            return true;
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to set app ops for package " + packageName + ", uid " + uid, e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isVpnUserPreConsented(String packageName) {
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
        return appOps.noteOpNoThrow(47, Binder.getCallingUid(), packageName) == 0;
    }

    private int getAppUid(String app, int userHandle) {
        if ("[Legacy VPN]".equals(app)) {
            return Process.myUid();
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            int result = pm.getPackageUidAsUser(app, userHandle);
            return result;
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
        boolean allowIPv4 = this.mConfig.allowIPv4;
        boolean allowIPv6 = this.mConfig.allowIPv6;
        LinkProperties lp = new LinkProperties();
        lp.setInterfaceName(this.mInterface);
        if (this.mConfig.addresses != null) {
            for (LinkAddress address : this.mConfig.addresses) {
                lp.addLinkAddress(address);
                allowIPv4 |= address.getAddress() instanceof Inet4Address;
                allowIPv6 |= address.getAddress() instanceof Inet6Address;
            }
        }
        if (this.mConfig.routes != null) {
            for (RouteInfo route : this.mConfig.routes) {
                lp.addRoute(route);
                InetAddress address2 = route.getDestination().getAddress();
                allowIPv4 |= address2 instanceof Inet4Address;
                allowIPv6 |= address2 instanceof Inet6Address;
            }
        }
        if (this.mConfig.dnsServers != null) {
            for (String dnsServer : this.mConfig.dnsServers) {
                InetAddress address3 = InetAddress.parseNumericAddress(dnsServer);
                lp.addDnsServer(address3);
                allowIPv4 |= address3 instanceof Inet4Address;
                allowIPv6 |= address3 instanceof Inet6Address;
            }
        }
        if (!allowIPv4) {
            lp.addRoute(new RouteInfo(new IpPrefix(Inet4Address.ANY, 0), 7));
        }
        if (!allowIPv6) {
            lp.addRoute(new RouteInfo(new IpPrefix(Inet6Address.ANY, 0), 7));
        }
        StringBuilder buffer = new StringBuilder();
        if (this.mConfig.searchDomains != null) {
            for (String domain : this.mConfig.searchDomains) {
                buffer.append(domain).append(' ');
            }
        }
        lp.setDomains(buffer.toString().trim());
        return lp;
    }

    private void agentConnect() {
        boolean z = false;
        LinkProperties lp = makeLinkProperties();
        if (lp.hasIPv4DefaultRoute() || lp.hasIPv6DefaultRoute()) {
            this.mNetworkCapabilities.addCapability(12);
        } else {
            this.mNetworkCapabilities.removeCapability(12);
        }
        this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTING, null, null);
        NetworkMisc networkMisc = new NetworkMisc();
        if (this.mConfig.allowBypass && !this.mLockdown) {
            z = true;
        }
        networkMisc.allowBypass = z;
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkAgent = new NetworkAgent(this.mLooper, this.mContext, NETWORKTYPE, this.mNetworkInfo, this.mNetworkCapabilities, lp, 0, networkMisc) {
                public void unwanted() {
                }
            };
            Binder.restoreCallingIdentity(token);
            this.mVpnUsers = createUserAndRestrictedProfilesRanges(this.mUserHandle, this.mConfig.allowedApplications, this.mConfig.disallowedApplications);
            this.mNetworkAgent.addUidRanges((UidRange[]) this.mVpnUsers.toArray(new UidRange[this.mVpnUsers.size()]));
            this.mNetworkInfo.setIsAvailable(true);
            updateState(NetworkInfo.DetailedState.CONNECTED, "agentConnect");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean canHaveRestrictedProfile(int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            return UserManager.get(this.mContext).canHaveRestrictedProfile(userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void agentDisconnect(NetworkInfo networkInfo, NetworkAgent networkAgent) {
        networkInfo.setIsAvailable(false);
        networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
        if (networkAgent == null) {
            return;
        }
        networkAgent.sendNetworkInfo(networkInfo);
    }

    private void agentDisconnect(NetworkAgent networkAgent) {
        NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
        agentDisconnect(networkInfo, networkAgent);
    }

    private void agentDisconnect() {
        if (!this.mNetworkInfo.isConnected()) {
            return;
        }
        agentDisconnect(this.mNetworkInfo, this.mNetworkAgent);
        this.mNetworkAgent = null;
    }

    public synchronized ParcelFileDescriptor establish(VpnConfig config) {
        UserManager mgr = UserManager.get(this.mContext);
        if (Binder.getCallingUid() != this.mOwnerUID) {
            return null;
        }
        if (!isVpnUserPreConsented(this.mPackage)) {
            return null;
        }
        Intent intent = new Intent("android.net.VpnService");
        intent.setClassName(this.mPackage, config.user);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                UserInfo user = mgr.getUserInfo(this.mUserHandle);
                if (user.isRestricted()) {
                    throw new SecurityException("Restricted users cannot establish VPNs");
                }
                ResolveInfo info = AppGlobals.getPackageManager().resolveService(intent, (String) null, 0, this.mUserHandle);
                if (info == null) {
                    throw new SecurityException("Cannot find " + config.user);
                }
                if (!"android.permission.BIND_VPN_SERVICE".equals(info.serviceInfo.permission)) {
                    throw new SecurityException(config.user + " does not require android.permission.BIND_VPN_SERVICE");
                }
                Binder.restoreCallingIdentity(token);
                VpnConfig oldConfig = this.mConfig;
                String oldInterface = this.mInterface;
                Connection oldConnection = this.mConnection;
                NetworkAgent oldNetworkAgent = this.mNetworkAgent;
                this.mNetworkAgent = null;
                Set<UidRange> oldUsers = this.mVpnUsers;
                ParcelFileDescriptor tun = ParcelFileDescriptor.adoptFd(jniCreate(config.mtu));
                try {
                    updateState(NetworkInfo.DetailedState.CONNECTING, "establish");
                    String interfaze = jniGetName(tun.getFd());
                    StringBuilder builder = new StringBuilder();
                    for (LinkAddress address : config.addresses) {
                        builder.append(" ").append(address);
                    }
                    if (jniSetAddresses(interfaze, builder.toString()) < 1) {
                        throw new IllegalArgumentException("At least one address must be specified");
                    }
                    Connection connection = new Connection(this, null);
                    if (!this.mContext.bindServiceAsUser(intent, connection, 67108865, new UserHandle(this.mUserHandle))) {
                        throw new IllegalStateException("Cannot bind " + config.user);
                    }
                    this.mConnection = connection;
                    this.mInterface = interfaze;
                    config.user = this.mPackage;
                    config.interfaze = this.mInterface;
                    config.startTime = SystemClock.elapsedRealtime();
                    this.mConfig = config;
                    agentConnect();
                    if (oldConnection != null) {
                        this.mContext.unbindService(oldConnection);
                    }
                    agentDisconnect(oldNetworkAgent);
                    if (oldInterface != null && !oldInterface.equals(interfaze)) {
                        jniReset(oldInterface);
                    }
                    try {
                        IoUtils.setBlocking(tun.getFileDescriptor(), config.blocking);
                        Log.i(TAG, "Established by " + config.user + " on " + this.mInterface);
                        return tun;
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot set tunnel's fd as blocking=" + config.blocking, e);
                    }
                } catch (RuntimeException e2) {
                    IoUtils.closeQuietly(tun);
                    agentDisconnect();
                    this.mConfig = oldConfig;
                    this.mConnection = oldConnection;
                    this.mVpnUsers = oldUsers;
                    this.mNetworkAgent = oldNetworkAgent;
                    this.mInterface = oldInterface;
                    throw e2;
                }
            } catch (RemoteException e3) {
                throw new SecurityException("Cannot find " + config.user);
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean isRunningLocked() {
        return (this.mNetworkAgent == null || this.mInterface == null) ? false : true;
    }

    private boolean isCallerEstablishedOwnerLocked() {
        return isRunningLocked() && Binder.getCallingUid() == this.mOwnerUID;
    }

    private SortedSet<Integer> getAppsUids(List<String> packageNames, int userHandle) {
        SortedSet<Integer> uids = new TreeSet<>();
        for (String app : packageNames) {
            int uid = getAppUid(app, userHandle);
            if (uid != -1) {
                uids.add(Integer.valueOf(uid));
            }
        }
        return uids;
    }

    Set<UidRange> createUserAndRestrictedProfilesRanges(int userHandle, List<String> allowedApplications, List<String> disallowedApplications) {
        Set<UidRange> ranges = new ArraySet<>();
        addUserToRanges(ranges, userHandle, allowedApplications, disallowedApplications);
        if (canHaveRestrictedProfile(userHandle)) {
            long token = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = UserManager.get(this.mContext).getUsers(true);
                Binder.restoreCallingIdentity(token);
                for (UserInfo user : users) {
                    if (user.isRestricted() && user.restrictedProfileParentId == userHandle) {
                        addUserToRanges(ranges, user.id, allowedApplications, disallowedApplications);
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }
        return ranges;
    }

    void addUserToRanges(Set<UidRange> ranges, int userHandle, List<String> allowedApplications, List<String> disallowedApplications) {
        if (allowedApplications != null) {
            int start = -1;
            int stop = -1;
            Iterator uid$iterator = getAppsUids(allowedApplications, userHandle).iterator();
            while (uid$iterator.hasNext()) {
                int uid = ((Integer) uid$iterator.next()).intValue();
                if (start == -1) {
                    start = uid;
                } else if (uid != stop + 1) {
                    ranges.add(new UidRange(start, stop));
                    start = uid;
                }
                stop = uid;
            }
            if (start != -1) {
                ranges.add(new UidRange(start, stop));
                return;
            }
            return;
        }
        if (disallowedApplications != null) {
            UidRange userRange = UidRange.createForUser(userHandle);
            int start2 = userRange.start;
            Iterator uid$iterator2 = getAppsUids(disallowedApplications, userHandle).iterator();
            while (uid$iterator2.hasNext()) {
                int uid2 = ((Integer) uid$iterator2.next()).intValue();
                if (uid2 == start2) {
                    start2++;
                } else {
                    ranges.add(new UidRange(start2, uid2 - 1));
                    start2 = uid2 + 1;
                }
            }
            if (start2 <= userRange.stop) {
                ranges.add(new UidRange(start2, userRange.stop));
                return;
            }
            return;
        }
        ranges.add(UidRange.createForUser(userHandle));
    }

    private List<UidRange> uidRangesForUser(int userHandle) {
        UidRange userRange = UidRange.createForUser(userHandle);
        List<UidRange> ranges = new ArrayList<>();
        for (UidRange range : this.mVpnUsers) {
            if (userRange.containsRange(range)) {
                ranges.add(range);
            }
        }
        return ranges;
    }

    private void removeVpnUserLocked(int userHandle) {
        if (this.mVpnUsers == null) {
            throw new IllegalStateException("VPN is not active");
        }
        List<UidRange> ranges = uidRangesForUser(userHandle);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.removeUidRanges((UidRange[]) ranges.toArray(new UidRange[ranges.size()]));
        }
        this.mVpnUsers.removeAll(ranges);
    }

    public void onUserAdded(int userHandle) {
        UserInfo user = UserManager.get(this.mContext).getUserInfo(userHandle);
        if (!user.isRestricted() || user.restrictedProfileParentId != this.mUserHandle) {
            return;
        }
        synchronized (this) {
            if (this.mVpnUsers != null) {
                try {
                    addUserToRanges(this.mVpnUsers, userHandle, this.mConfig.allowedApplications, this.mConfig.disallowedApplications);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to add restricted user to owner", e);
                }
                if (this.mNetworkAgent != null) {
                    List<UidRange> ranges = uidRangesForUser(userHandle);
                    this.mNetworkAgent.addUidRanges((UidRange[]) ranges.toArray(new UidRange[ranges.size()]));
                    if (this.mAlwaysOn) {
                        setVpnForcedLocked(this.mLockdown);
                    }
                } else if (this.mAlwaysOn) {
                }
            }
        }
    }

    public void onUserRemoved(int userHandle) {
        UserInfo user = UserManager.get(this.mContext).getUserInfo(userHandle);
        if (!user.isRestricted() || user.restrictedProfileParentId != this.mUserHandle) {
            return;
        }
        synchronized (this) {
            if (this.mVpnUsers != null) {
                try {
                    removeVpnUserLocked(userHandle);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to remove restricted user to owner", e);
                }
                if (this.mAlwaysOn) {
                    setVpnForcedLocked(this.mLockdown);
                }
            } else if (this.mAlwaysOn) {
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
    private void setVpnForcedLocked(boolean enforce) {
        Set<UidRange> removedRanges = new ArraySet<>(this.mBlockedUsers);
        if (enforce) {
            Set<UidRange> addedRanges = createUserAndRestrictedProfilesRanges(this.mUserHandle, null, Collections.singletonList(this.mPackage));
            removedRanges.removeAll(addedRanges);
            addedRanges.removeAll(this.mBlockedUsers);
            setAllowOnlyVpnForUids(false, removedRanges);
            setAllowOnlyVpnForUids(true, addedRanges);
            return;
        }
        setAllowOnlyVpnForUids(false, removedRanges);
    }

    @GuardedBy("this")
    private boolean setAllowOnlyVpnForUids(boolean enforce, Collection<UidRange> ranges) {
        if (ranges.size() == 0) {
            return true;
        }
        UidRange[] rangesArray = (UidRange[]) ranges.toArray(new UidRange[ranges.size()]);
        try {
            this.mNetd.setAllowOnlyVpnForUids(enforce, rangesArray);
            if (enforce) {
                this.mBlockedUsers.addAll(ranges);
            } else {
                this.mBlockedUsers.removeAll(ranges);
            }
            return true;
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Updating blocked=" + enforce + " for UIDs " + Arrays.toString(ranges.toArray()) + " failed", e);
            return false;
        }
    }

    public VpnConfig getVpnConfig() {
        enforceControlPermission();
        return this.mConfig;
    }

    @Deprecated
    public synchronized void interfaceStatusChanged(String iface, boolean up) {
        try {
            this.mObserver.interfaceStatusChanged(iface, up);
        } catch (RemoteException e) {
        }
    }

    private void enforceControlPermission() {
        this.mContext.enforceCallingPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceControlPermissionOrInternalCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private class Connection implements ServiceConnection {
        private IBinder mService;

        Connection(Vpn this$0, Connection connection) {
            this();
        }

        private Connection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mService = service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.mService = null;
        }
    }

    private void prepareStatusIntent() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mStatusIntent = VpnConfig.getIntentForStatusPanel(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public synchronized boolean addAddress(String address, int prefixLength) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean success = jniAddAddress(this.mInterface, address, prefixLength);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return success;
    }

    public synchronized boolean removeAddress(String address, int prefixLength) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean success = jniDelAddress(this.mInterface, address, prefixLength);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return success;
    }

    public synchronized boolean setUnderlyingNetworks(Network[] networks) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        if (networks == null) {
            this.mConfig.underlyingNetworks = null;
        } else {
            this.mConfig.underlyingNetworks = new Network[networks.length];
            for (int i = 0; i < networks.length; i++) {
                if (networks[i] == null) {
                    this.mConfig.underlyingNetworks[i] = null;
                } else {
                    this.mConfig.underlyingNetworks[i] = new Network(networks[i].netId);
                }
            }
        }
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
        VpnInfo info = new VpnInfo();
        info.ownerUid = this.mOwnerUID;
        info.vpnIface = this.mInterface;
        return info;
    }

    public synchronized boolean appliesToUid(int uid) {
        if (!isRunningLocked()) {
            return false;
        }
        for (UidRange uidRange : this.mVpnUsers) {
            if (uidRange.contains(uid)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isBlockingUid(int uid) {
        synchronized (this) {
            if (!this.mLockdown) {
                return false;
            }
            if (this.mNetworkInfo.isConnected()) {
                return appliesToUid(uid) ? false : true;
            }
            for (UidRange uidRange : this.mBlockedUsers) {
                if (uidRange.contains(uid)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static RouteInfo findIPv4DefaultRoute(LinkProperties prop) {
        for (RouteInfo route : prop.getAllRoutes()) {
            if (route.isDefaultRoute() && (route.getGateway() instanceof Inet4Address)) {
                return route;
            }
        }
        throw new IllegalStateException("Unable to find IPv4 default gateway");
    }

    public void startLegacyVpn(VpnProfile profile, KeyStore keyStore, LinkProperties egress) {
        enforceControlPermission();
        long token = Binder.clearCallingIdentity();
        try {
            startLegacyVpnPrivileged(profile, keyStore, egress);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void startLegacyVpnPrivileged(VpnProfile profile, KeyStore keyStore, LinkProperties egress) {
        UserManager mgr = UserManager.get(this.mContext);
        UserInfo user = mgr.getUserInfo(this.mUserHandle);
        if (user.isRestricted() || mgr.hasUserRestriction("no_config_vpn", new UserHandle(this.mUserHandle))) {
            throw new SecurityException("Restricted users cannot establish VPNs");
        }
        RouteInfo ipv4DefaultRoute = findIPv4DefaultRoute(egress);
        String gateway = ipv4DefaultRoute.getGateway().getHostAddress();
        String iface = ipv4DefaultRoute.getInterface();
        String privateKey = "";
        String userCert = "";
        String caCert = "";
        String serverCert = "";
        if (!profile.ipsecUserCert.isEmpty()) {
            privateKey = "USRPKEY_" + profile.ipsecUserCert;
            byte[] value = keyStore.get("USRCERT_" + profile.ipsecUserCert);
            userCert = value == null ? null : new String(value, StandardCharsets.UTF_8);
        }
        if (!profile.ipsecCaCert.isEmpty()) {
            byte[] value2 = keyStore.get("CACERT_" + profile.ipsecCaCert);
            caCert = value2 == null ? null : new String(value2, StandardCharsets.UTF_8);
        }
        if (!profile.ipsecServerCert.isEmpty()) {
            byte[] value3 = keyStore.get("USRCERT_" + profile.ipsecServerCert);
            serverCert = value3 == null ? null : new String(value3, StandardCharsets.UTF_8);
        }
        if (privateKey == null || userCert == null || caCert == null || serverCert == null) {
            throw new IllegalStateException("Cannot load credentials");
        }
        String[] racoon = null;
        switch (profile.type) {
            case 1:
                racoon = new String[]{iface, profile.server, "udppsk", profile.ipsecIdentifier, profile.ipsecSecret, "1701"};
                break;
            case 2:
                racoon = new String[]{iface, profile.server, "udprsa", privateKey, userCert, caCert, serverCert, "1701"};
                break;
            case 3:
                racoon = new String[]{iface, profile.server, "xauthpsk", profile.ipsecIdentifier, profile.ipsecSecret, profile.username, profile.password, "", gateway};
                break;
            case 4:
                racoon = new String[]{iface, profile.server, "xauthrsa", privateKey, userCert, caCert, serverCert, profile.username, profile.password, "", gateway};
                break;
            case 5:
                racoon = new String[]{iface, profile.server, "hybridrsa", caCert, serverCert, profile.username, profile.password, "", gateway};
                break;
        }
        String[] mtpd = null;
        switch (profile.type) {
            case 0:
                mtpd = new String[20];
                mtpd[0] = iface;
                mtpd[1] = "pptp";
                mtpd[2] = profile.server;
                mtpd[3] = "1723";
                mtpd[4] = "name";
                mtpd[5] = profile.username;
                mtpd[6] = "password";
                mtpd[7] = profile.password;
                mtpd[8] = "linkname";
                mtpd[9] = "vpn";
                mtpd[10] = "refuse-eap";
                mtpd[11] = "nodefaultroute";
                mtpd[12] = "usepeerdns";
                mtpd[13] = "idle";
                mtpd[14] = "1800";
                mtpd[15] = "mtu";
                mtpd[16] = "1400";
                mtpd[17] = "mru";
                mtpd[18] = "1400";
                mtpd[19] = profile.mppe ? "+mppe" : "nomppe";
                break;
            case 1:
            case 2:
                mtpd = new String[]{iface, "l2tp", profile.server, "1701", profile.l2tpSecret, "name", profile.username, "password", profile.password, "linkname", "vpn", "refuse-eap", "nodefaultroute", "usepeerdns", "idle", "1800", "mtu", "1400", "mru", "1400"};
                break;
        }
        VpnConfig config = new VpnConfig();
        config.legacy = true;
        config.user = profile.key;
        config.interfaze = iface;
        config.session = profile.name;
        config.addLegacyRoutes(profile.routes);
        if (!profile.dnsServers.isEmpty()) {
            config.dnsServers = Arrays.asList(profile.dnsServers.split(" +"));
        }
        if (!profile.searchDomains.isEmpty()) {
            config.searchDomains = Arrays.asList(profile.searchDomains.split(" +"));
        }
        startLegacyVpn(config, racoon, mtpd);
    }

    private synchronized void startLegacyVpn(VpnConfig config, String[] racoon, String[] mtpd) {
        stopLegacyVpnPrivileged();
        prepareInternal("[Legacy VPN]");
        updateState(NetworkInfo.DetailedState.CONNECTING, "startLegacyVpn");
        this.mLegacyVpnRunner = new LegacyVpnRunner(this, config, racoon, mtpd);
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
        LegacyVpnInfo info = new LegacyVpnInfo();
        info.key = this.mConfig.user;
        info.state = LegacyVpnInfo.stateFromNetworkInfo(this.mNetworkInfo);
        if (this.mNetworkInfo.isConnected()) {
            info.intent = this.mStatusIntent;
        }
        return info;
    }

    public VpnConfig getLegacyVpnConfig() {
        if (this.mLegacyVpnRunner != null) {
            return this.mConfig;
        }
        return null;
    }

    public synchronized boolean forceDisconnect() {
        Log.i(TAG, "forceDisconnect");
        return prepare(this.mPackage, "[Legacy VPN]");
    }

    private class LegacyVpnRunner extends Thread {
        private static final String TAG = "LegacyVpnRunner";
        private final String[][] mArguments;
        private final BroadcastReceiver mBroadcastReceiver;
        private final String[] mDaemons;
        private final AtomicInteger mOuterConnection;
        private final String mOuterInterface;
        private final LocalSocket[] mSockets;
        private long mTimer;
        final Vpn this$0;

        public LegacyVpnRunner(Vpn this$0, VpnConfig config, String[] racoon, String[] mtpd) {
            NetworkInfo networkInfo;
            super(TAG);
            this.this$0 = this$0;
            this.mOuterConnection = new AtomicInteger(-1);
            this.mTimer = -1L;
            this.mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo info;
                    if (!LegacyVpnRunner.this.this$0.mEnableTeardown || !intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE") || intent.getIntExtra("networkType", -1) != LegacyVpnRunner.this.mOuterConnection.get() || (info = (NetworkInfo) intent.getExtra("networkInfo")) == null || info.isConnectedOrConnecting()) {
                        return;
                    }
                    try {
                        LegacyVpnRunner.this.this$0.mObserver.interfaceStatusChanged(LegacyVpnRunner.this.mOuterInterface, false);
                    } catch (RemoteException e) {
                    }
                }
            };
            this$0.mConfig = config;
            this.mDaemons = new String[]{"racoon", "mtpd"};
            this.mArguments = new String[][]{racoon, mtpd};
            this.mSockets = new LocalSocket[this.mDaemons.length];
            this.mOuterInterface = this$0.mConfig.interfaze;
            if (!TextUtils.isEmpty(this.mOuterInterface)) {
                ConnectivityManager cm = ConnectivityManager.from(this$0.mContext);
                for (Network network : cm.getAllNetworks()) {
                    LinkProperties lp = cm.getLinkProperties(network);
                    if (lp != null && lp.getAllInterfaceNames().contains(this.mOuterInterface) && (networkInfo = cm.getNetworkInfo(network)) != null) {
                        this.mOuterConnection.set(networkInfo.getType());
                    }
                }
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            this$0.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        }

        public void check(String interfaze) {
            if (!interfaze.equals(this.mOuterInterface)) {
                return;
            }
            Log.i(TAG, "Legacy VPN is going down with " + interfaze);
            exit();
        }

        public void exit() {
            interrupt();
            for (LocalSocket socket : this.mSockets) {
                IoUtils.closeQuietly(socket);
            }
            this.this$0.agentDisconnect();
            try {
                this.this$0.mContext.unregisterReceiver(this.mBroadcastReceiver);
            } catch (IllegalArgumentException e) {
            }
        }

        @Override
        public void run() {
            Log.v(TAG, "Waiting");
            synchronized (TAG) {
                Log.v(TAG, "Executing");
                execute();
                monitorDaemons();
            }
        }

        private void checkpoint(boolean yield) throws InterruptedException {
            long now = SystemClock.elapsedRealtime();
            if (this.mTimer == -1) {
                this.mTimer = now;
                Thread.sleep(1L);
            } else if (now - this.mTimer <= 60000) {
                Thread.sleep(yield ? 200 : 1);
            } else {
                this.this$0.updateState(NetworkInfo.DetailedState.FAILED, "checkpoint");
                throw new IllegalStateException("Time is up");
            }
        }

        private void execute() {
            try {
                try {
                    checkpoint(false);
                    for (String daemon : this.mDaemons) {
                        while (!SystemService.isStopped(daemon)) {
                            checkpoint(true);
                        }
                    }
                    File state = new File("/data/misc/vpn/state");
                    state.delete();
                    if (state.exists()) {
                        throw new IllegalStateException("Cannot delete the state");
                    }
                    new File("/data/misc/vpn/abort").delete();
                    boolean restart = false;
                    for (String[] strArr : this.mArguments) {
                        restart = restart || strArr != null;
                    }
                    if (!restart) {
                        this.this$0.agentDisconnect();
                        if (1 == 0) {
                            for (String daemon2 : this.mDaemons) {
                                SystemService.stop(daemon2);
                            }
                        }
                        if (1 == 0 || this.this$0.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                            this.this$0.agentDisconnect();
                            return;
                        }
                        return;
                    }
                    this.this$0.updateState(NetworkInfo.DetailedState.CONNECTING, "execute");
                    for (int i = 0; i < this.mDaemons.length; i++) {
                        String[] arguments = this.mArguments[i];
                        if (arguments != null) {
                            String daemon3 = this.mDaemons[i];
                            Log.d(TAG, "systemservice start " + daemon3);
                            SystemService.start(daemon3);
                            while (!SystemService.isRunning(daemon3)) {
                                checkpoint(true);
                            }
                            this.mSockets[i] = new LocalSocket();
                            LocalSocketAddress address = new LocalSocketAddress(daemon3, LocalSocketAddress.Namespace.RESERVED);
                            while (true) {
                                try {
                                    this.mSockets[i].connect(address);
                                    break;
                                } catch (Exception e) {
                                    checkpoint(true);
                                }
                            }
                            this.mSockets[i].setSoTimeout(com.android.server.SystemService.PHASE_SYSTEM_SERVICES_READY);
                            OutputStream out = this.mSockets[i].getOutputStream();
                            for (String argument : arguments) {
                                byte[] bytes = argument.getBytes(StandardCharsets.UTF_8);
                                if (bytes.length >= 65535) {
                                    throw new IllegalArgumentException("Argument is too large");
                                }
                                out.write(bytes.length >> 8);
                                out.write(bytes.length);
                                out.write(bytes);
                                checkpoint(false);
                            }
                            out.write(DhcpPacket.MAX_OPTION_LEN);
                            out.write(DhcpPacket.MAX_OPTION_LEN);
                            out.flush();
                            InputStream in = this.mSockets[i].getInputStream();
                            while (in.read() != -1) {
                                checkpoint(true);
                            }
                        }
                    }
                    while (!state.exists()) {
                        for (int i2 = 0; i2 < this.mDaemons.length; i2++) {
                            String daemon4 = this.mDaemons[i2];
                            if (this.mArguments[i2] != null && !SystemService.isRunning(daemon4)) {
                                throw new IllegalStateException(daemon4 + " is dead");
                            }
                        }
                        checkpoint(true);
                    }
                    String[] parameters = FileUtils.readTextFile(state, 0, null).split("\n", -1);
                    if (parameters.length != 7) {
                        throw new IllegalStateException("Cannot parse the state");
                    }
                    this.this$0.mConfig.interfaze = parameters[0].trim();
                    this.this$0.mConfig.addLegacyAddresses(parameters[1]);
                    if (this.this$0.mConfig.routes == null || this.this$0.mConfig.routes.isEmpty()) {
                        this.this$0.mConfig.addLegacyRoutes(parameters[2]);
                    }
                    if (this.this$0.mConfig.dnsServers == null || this.this$0.mConfig.dnsServers.size() == 0) {
                        String dnsServers = parameters[3].trim();
                        if (!dnsServers.isEmpty()) {
                            this.this$0.mConfig.dnsServers = Arrays.asList(dnsServers.split(" "));
                        }
                    }
                    if (this.this$0.mConfig.searchDomains == null || this.this$0.mConfig.searchDomains.size() == 0) {
                        String searchDomains = parameters[4].trim();
                        if (!searchDomains.isEmpty()) {
                            this.this$0.mConfig.searchDomains = Arrays.asList(searchDomains.split(" "));
                        }
                    }
                    String endpoint = parameters[5];
                    if (!endpoint.isEmpty()) {
                        try {
                            InetAddress addr = InetAddress.parseNumericAddress(endpoint);
                            if (addr instanceof Inet4Address) {
                                this.this$0.mConfig.routes.add(new RouteInfo(new IpPrefix(addr, 32), 9));
                            } else if (addr instanceof Inet6Address) {
                                this.this$0.mConfig.routes.add(new RouteInfo(new IpPrefix(addr, 128), 9));
                            } else {
                                Log.e(TAG, "Unknown IP address family for VPN endpoint: " + endpoint);
                            }
                        } catch (IllegalArgumentException e2) {
                            Log.e(TAG, "Exception constructing throw route to " + endpoint + ": " + e2);
                        }
                    }
                    synchronized (this.this$0) {
                        this.this$0.mConfig.startTime = SystemClock.elapsedRealtime();
                        checkpoint(false);
                        if (this.this$0.jniCheck(this.this$0.mConfig.interfaze) == 0) {
                            throw new IllegalStateException(this.this$0.mConfig.interfaze + " is gone");
                        }
                        this.this$0.mInterface = this.this$0.mConfig.interfaze;
                        this.this$0.prepareStatusIntent();
                        this.this$0.agentConnect();
                        Log.i(TAG, "Connected!");
                    }
                    if (1 == 0) {
                        for (String daemon5 : this.mDaemons) {
                            SystemService.stop(daemon5);
                        }
                    }
                    if (1 == 0 || this.this$0.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                        this.this$0.agentDisconnect();
                    }
                } catch (Exception e3) {
                    Log.i(TAG, "Aborting", e3);
                    this.this$0.updateState(NetworkInfo.DetailedState.FAILED, e3.getMessage());
                    exit();
                    if (0 == 0) {
                        for (String daemon6 : this.mDaemons) {
                            SystemService.stop(daemon6);
                        }
                    }
                    if (0 == 0 || this.this$0.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                        this.this$0.agentDisconnect();
                    }
                }
            } catch (Throwable th) {
                if (0 == 0) {
                    for (String daemon7 : this.mDaemons) {
                        SystemService.stop(daemon7);
                    }
                }
                if (0 == 0 || this.this$0.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                    this.this$0.agentDisconnect();
                }
                throw th;
            }
        }

        private void monitorDaemons() {
            int i = 0;
            if (this.this$0.mNetworkInfo.isConnected()) {
                loop0: while (true) {
                    try {
                        try {
                            Thread.sleep(2000L);
                            for (int i2 = 0; i2 < this.mDaemons.length; i2++) {
                                if (this.mArguments[i2] != null && SystemService.isStopped(this.mDaemons[i2])) {
                                    break loop0;
                                }
                            }
                        } catch (InterruptedException e) {
                            Log.d(TAG, "interrupted during monitorDaemons(); stopping services");
                            String[] strArr = this.mDaemons;
                            int length = strArr.length;
                            while (i < length) {
                                String daemon = strArr[i];
                                SystemService.stop(daemon);
                                i++;
                            }
                            this.this$0.agentDisconnect();
                            return;
                        }
                    } catch (Throwable th) {
                        String[] strArr2 = this.mDaemons;
                        int length2 = strArr2.length;
                        while (i < length2) {
                            String daemon2 = strArr2[i];
                            SystemService.stop(daemon2);
                            i++;
                        }
                        this.this$0.agentDisconnect();
                        throw th;
                    }
                }
                String[] strArr3 = this.mDaemons;
                int length3 = strArr3.length;
                while (i < length3) {
                    String daemon3 = strArr3[i];
                    SystemService.stop(daemon3);
                    i++;
                }
                this.this$0.agentDisconnect();
            }
        }
    }
}
