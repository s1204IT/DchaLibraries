package com.android.server.connectivity;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.IConnectivityManager;
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
import android.security.KeyStore;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.server.net.BaseNetworkObserver;
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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;

public class Vpn {
    private static final boolean LOGD = true;
    private static final String NETWORKTYPE = "VPN";
    private static final String TAG = "Vpn";
    private VpnConfig mConfig;
    private final IConnectivityManager mConnService;
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
    private BroadcastReceiver mUserIntentReceiver;
    private volatile boolean mEnableTeardown = LOGD;

    @GuardedBy("this")
    private List<UidRange> mVpnUsers = null;
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

    public Vpn(Looper looper, Context context, INetworkManagementService netService, IConnectivityManager connService, int userHandle) {
        this.mUserIntentReceiver = null;
        this.mContext = context;
        this.mNetd = netService;
        this.mConnService = connService;
        this.mUserHandle = userHandle;
        this.mLooper = looper;
        this.mOwnerUID = getAppUid(this.mPackage, this.mUserHandle);
        try {
            netService.registerObserver(this.mObserver);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Problem registering observer", e);
        }
        if (userHandle == 0) {
            this.mUserIntentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    int userHandle2 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    if (userHandle2 != -10000) {
                        if ("android.intent.action.USER_ADDED".equals(action)) {
                            Vpn.this.onUserAdded(userHandle2);
                        } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                            Vpn.this.onUserRemoved(userHandle2);
                        }
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_ADDED");
            intentFilter.addAction("android.intent.action.USER_REMOVED");
            this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter, null, null);
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
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
    }

    public synchronized boolean prepare(String oldPackage, String newPackage) {
        boolean z = LOGD;
        synchronized (this) {
            if (oldPackage != null) {
                if (getAppUid(oldPackage, this.mUserHandle) != this.mOwnerUID) {
                    if (!oldPackage.equals("[Legacy VPN]") && isVpnUserPreConsented(oldPackage)) {
                        prepareInternal(oldPackage);
                    } else {
                        z = false;
                    }
                } else if (newPackage != null && (newPackage.equals("[Legacy VPN]") || getAppUid(newPackage, this.mUserHandle) != this.mOwnerUID)) {
                    enforceControlPermission();
                    prepareInternal(newPackage);
                }
            }
        }
        return z;
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
            if (this.mConnection == null) {
                if (this.mLegacyVpnRunner != null) {
                    this.mLegacyVpnRunner.exit();
                    this.mLegacyVpnRunner = null;
                }
            } else {
                try {
                    this.mConnection.mService.transact(16777215, Parcel.obtain(), null, 1);
                } catch (Exception e) {
                }
                this.mContext.unbindService(this.mConnection);
                this.mConnection = null;
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

    public void setPackageAuthorization(boolean authorized) {
        enforceControlPermission();
        if (this.mPackage != null && !"[Legacy VPN]".equals(this.mPackage)) {
            long token = Binder.clearCallingIdentity();
            try {
                AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
                appOps.setMode(47, this.mOwnerUID, this.mPackage, authorized ? 0 : 1);
            } catch (Exception e) {
                Log.wtf(TAG, "Failed to set app ops for package " + this.mPackage, e);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private boolean isVpnUserPreConsented(String packageName) {
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
        if (appOps.noteOpNoThrow(47, Binder.getCallingUid(), packageName) == 0) {
            return LOGD;
        }
        return false;
    }

    private int getAppUid(String app, int userHandle) {
        if ("[Legacy VPN]".equals(app)) {
            return Process.myUid();
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            return pm.getPackageUid(app, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public NetworkInfo getNetworkInfo() {
        return this.mNetworkInfo;
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
        LinkProperties lp = makeLinkProperties();
        if (lp.hasIPv4DefaultRoute() || lp.hasIPv6DefaultRoute()) {
            this.mNetworkCapabilities.addCapability(12);
        } else {
            this.mNetworkCapabilities.removeCapability(12);
        }
        this.mNetworkInfo.setIsAvailable(LOGD);
        this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
        NetworkMisc networkMisc = new NetworkMisc();
        networkMisc.allowBypass = this.mConfig.allowBypass;
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkAgent = new NetworkAgent(this.mLooper, this.mContext, NETWORKTYPE, this.mNetworkInfo, this.mNetworkCapabilities, lp, 0, networkMisc) {
                public void unwanted() {
                }
            };
            Binder.restoreCallingIdentity(token);
            addVpnUserLocked(this.mUserHandle);
            if (this.mUserHandle == 0) {
                token = Binder.clearCallingIdentity();
                try {
                    List<UserInfo> users = UserManager.get(this.mContext).getUsers();
                    Binder.restoreCallingIdentity(token);
                    for (UserInfo user : users) {
                        if (user.isRestricted()) {
                            addVpnUserLocked(user.id);
                        }
                    }
                } finally {
                }
            }
            this.mNetworkAgent.addUidRanges((UidRange[]) this.mVpnUsers.toArray(new UidRange[this.mVpnUsers.size()]));
        } finally {
        }
    }

    private void agentDisconnect(NetworkInfo networkInfo, NetworkAgent networkAgent) {
        networkInfo.setIsAvailable(false);
        networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
        if (networkAgent != null) {
            networkAgent.sendNetworkInfo(networkInfo);
        }
    }

    private void agentDisconnect(NetworkAgent networkAgent) {
        NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
        agentDisconnect(networkInfo, networkAgent);
    }

    private void agentDisconnect() {
        if (this.mNetworkInfo.isConnected()) {
            agentDisconnect(this.mNetworkInfo, this.mNetworkAgent);
            this.mNetworkAgent = null;
        }
    }

    public synchronized ParcelFileDescriptor establish(VpnConfig config) {
        long token;
        ParcelFileDescriptor tun;
        UserManager mgr = UserManager.get(this.mContext);
        if (Binder.getCallingUid() != this.mOwnerUID) {
            tun = null;
        } else {
            try {
                Intent intent = new Intent("android.net.VpnService");
                intent.setClassName(this.mPackage, config.user);
                token = Binder.clearCallingIdentity();
                try {
                    UserInfo user = mgr.getUserInfo(this.mUserHandle);
                    if (user.isRestricted() || mgr.hasUserRestriction("no_config_vpn")) {
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
                    List<UidRange> oldUsers = this.mVpnUsers;
                    tun = ParcelFileDescriptor.adoptFd(jniCreate(config.mtu));
                    try {
                        updateState(NetworkInfo.DetailedState.CONNECTING, "establish");
                        String interfaze = jniGetName(tun.getFd());
                        StringBuilder builder = new StringBuilder();
                        for (LinkAddress address : config.addresses) {
                            builder.append(" " + address);
                        }
                        if (jniSetAddresses(interfaze, builder.toString()) < 1) {
                            throw new IllegalArgumentException("At least one address must be specified");
                        }
                        Connection connection = new Connection();
                        if (!this.mContext.bindServiceAsUser(intent, connection, 1, new UserHandle(this.mUserHandle))) {
                            throw new IllegalStateException("Cannot bind " + config.user);
                        }
                        this.mConnection = connection;
                        this.mInterface = interfaze;
                        config.user = this.mPackage;
                        config.interfaze = this.mInterface;
                        config.startTime = SystemClock.elapsedRealtime();
                        this.mConfig = config;
                        this.mVpnUsers = new ArrayList();
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
        return tun;
    }

    private boolean isRunningLocked() {
        if (this.mNetworkAgent == null || this.mInterface == null) {
            return false;
        }
        return LOGD;
    }

    private boolean isCallerEstablishedOwnerLocked() {
        if (isRunningLocked() && Binder.getCallingUid() == this.mOwnerUID) {
            return LOGD;
        }
        return false;
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

    private void addVpnUserLocked(int userHandle) {
        if (this.mVpnUsers == null) {
            throw new IllegalStateException("VPN is not active");
        }
        if (this.mConfig.allowedApplications != null) {
            int start = -1;
            int stop = -1;
            Iterator<Integer> it = getAppsUids(this.mConfig.allowedApplications, userHandle).iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                if (start == -1) {
                    start = uid;
                } else if (uid != stop + 1) {
                    this.mVpnUsers.add(new UidRange(start, stop));
                    start = uid;
                }
                stop = uid;
            }
            if (start != -1) {
                this.mVpnUsers.add(new UidRange(start, stop));
            }
        } else if (this.mConfig.disallowedApplications != null) {
            UidRange userRange = UidRange.createForUser(userHandle);
            int start2 = userRange.start;
            Iterator<Integer> it2 = getAppsUids(this.mConfig.disallowedApplications, userHandle).iterator();
            while (it2.hasNext()) {
                int uid2 = it2.next().intValue();
                if (uid2 == start2) {
                    start2++;
                } else {
                    this.mVpnUsers.add(new UidRange(start2, uid2 - 1));
                    start2 = uid2 + 1;
                }
            }
            if (start2 <= userRange.stop) {
                this.mVpnUsers.add(new UidRange(start2, userRange.stop));
            }
        } else {
            this.mVpnUsers.add(UidRange.createForUser(userHandle));
        }
        prepareStatusIntent();
    }

    private List<UidRange> uidRangesForUser(int userHandle) {
        UidRange userRange = UidRange.createForUser(userHandle);
        List<UidRange> ranges = new ArrayList<>();
        for (UidRange range : this.mVpnUsers) {
            if (range.start >= userRange.start && range.stop <= userRange.stop) {
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
        this.mStatusIntent = null;
    }

    private void onUserAdded(int userHandle) {
        synchronized (this) {
            UserManager mgr = UserManager.get(this.mContext);
            UserInfo user = mgr.getUserInfo(userHandle);
            if (user.isRestricted()) {
                try {
                    addVpnUserLocked(userHandle);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to add restricted user to owner", e);
                }
                if (this.mNetworkAgent != null) {
                    List<UidRange> ranges = uidRangesForUser(userHandle);
                    this.mNetworkAgent.addUidRanges((UidRange[]) ranges.toArray(new UidRange[ranges.size()]));
                }
            }
        }
    }

    private void onUserRemoved(int userHandle) {
        synchronized (this) {
            UserManager mgr = UserManager.get(this.mContext);
            UserInfo user = mgr.getUserInfo(userHandle);
            if (user.isRestricted()) {
                try {
                    removeVpnUserLocked(userHandle);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to remove restricted user to owner", e);
                }
            }
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

    private class Connection implements ServiceConnection {
        private IBinder mService;

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
        boolean zJniAddAddress;
        if (!isCallerEstablishedOwnerLocked()) {
            zJniAddAddress = false;
        } else {
            zJniAddAddress = jniAddAddress(this.mInterface, address, prefixLength);
            this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        }
        return zJniAddAddress;
    }

    public synchronized boolean removeAddress(String address, int prefixLength) {
        boolean zJniDelAddress;
        if (!isCallerEstablishedOwnerLocked()) {
            zJniDelAddress = false;
        } else {
            zJniDelAddress = jniDelAddress(this.mInterface, address, prefixLength);
            this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        }
        return zJniDelAddress;
    }

    public synchronized boolean setUnderlyingNetworks(Network[] networks) {
        boolean z;
        if (!isCallerEstablishedOwnerLocked()) {
            z = false;
        } else {
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
            z = LOGD;
        }
        return z;
    }

    public synchronized Network[] getUnderlyingNetworks() {
        return !isRunningLocked() ? null : this.mConfig.underlyingNetworks;
    }

    public synchronized boolean appliesToUid(int uid) {
        boolean z = false;
        synchronized (this) {
            if (isRunningLocked()) {
                Iterator<UidRange> it = this.mVpnUsers.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    UidRange uidRange = it.next();
                    if (uidRange.start <= uid && uid <= uidRange.stop) {
                        z = LOGD;
                        break;
                    }
                }
            }
        }
        return z;
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
        if (!keyStore.isUnlocked()) {
            throw new IllegalStateException("KeyStore isn't unlocked");
        }
        UserManager mgr = UserManager.get(this.mContext);
        UserInfo user = mgr.getUserInfo(this.mUserHandle);
        if (user.isRestricted() || mgr.hasUserRestriction("no_config_vpn")) {
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
        config.legacy = LOGD;
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
        this.mLegacyVpnRunner = new LegacyVpnRunner(config, racoon, mtpd);
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
        LegacyVpnInfo info;
        enforceControlPermission();
        if (this.mLegacyVpnRunner == null) {
            info = null;
        } else {
            info = new LegacyVpnInfo();
            info.key = this.mConfig.user;
            info.state = LegacyVpnInfo.stateFromNetworkInfo(this.mNetworkInfo);
            if (this.mNetworkInfo.isConnected()) {
                info.intent = this.mStatusIntent;
            }
        }
        return info;
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
        private final BroadcastReceiver mBroadcastReceiver;
        private final String[] mDaemons;
        private final AtomicInteger mOuterConnection;
        private final String mOuterInterface;
        private final LocalSocket[] mSockets;
        private long mTimer;

        public LegacyVpnRunner(VpnConfig config, String[] racoon, String[] mtpd) {
            super(TAG);
            this.mOuterConnection = new AtomicInteger(-1);
            this.mTimer = -1L;
            this.mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo info;
                    if (Vpn.this.mEnableTeardown && intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE") && intent.getIntExtra("networkType", -1) == LegacyVpnRunner.this.mOuterConnection.get() && (info = (NetworkInfo) intent.getExtra("networkInfo")) != null && !info.isConnectedOrConnecting()) {
                        try {
                            Vpn.this.mObserver.interfaceStatusChanged(LegacyVpnRunner.this.mOuterInterface, false);
                        } catch (RemoteException e) {
                        }
                    }
                }
            };
            Vpn.this.mConfig = config;
            this.mDaemons = new String[]{"racoon", "mtpd"};
            this.mArguments = new String[][]{racoon, mtpd};
            this.mSockets = new LocalSocket[this.mDaemons.length];
            this.mOuterInterface = Vpn.this.mConfig.interfaze;
            try {
                this.mOuterConnection.set(Vpn.this.mConnService.findConnectionTypeForIface(this.mOuterInterface));
            } catch (Exception e) {
                this.mOuterConnection.set(-1);
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            Vpn.this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        }

        public void check(String interfaze) {
            if (interfaze.equals(this.mOuterInterface)) {
                Log.i(TAG, "Legacy VPN is going down with " + interfaze);
                exit();
            }
        }

        public void exit() {
            interrupt();
            LocalSocket[] arr$ = this.mSockets;
            for (LocalSocket socket : arr$) {
                IoUtils.closeQuietly(socket);
            }
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
                execute();
                monitorDaemons();
            }
        }

        private void checkpoint(boolean yield) throws InterruptedException {
            long now = SystemClock.elapsedRealtime();
            if (this.mTimer == -1) {
                this.mTimer = now;
                Thread.sleep(1L);
            } else {
                if (now - this.mTimer > 60000) {
                    Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, "checkpoint");
                    throw new IllegalStateException("Time is up");
                }
                Thread.sleep(yield ? 200L : 1L);
            }
        }

        private void execute() {
            try {
                try {
                    checkpoint(false);
                    String[] arr$ = this.mDaemons;
                    for (String daemon : arr$) {
                        while (!SystemService.isStopped(daemon)) {
                            checkpoint(Vpn.LOGD);
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
                        restart = (restart || strArr != null) ? Vpn.LOGD : false;
                    }
                    if (!restart) {
                        Vpn.this.agentDisconnect();
                        if (1 == 0) {
                            String[] arr$2 = this.mDaemons;
                            for (String daemon2 : arr$2) {
                                SystemService.stop(daemon2);
                            }
                        }
                        if (1 == 0 || Vpn.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                            Vpn.this.agentDisconnect();
                            return;
                        }
                        return;
                    }
                    Vpn.this.updateState(NetworkInfo.DetailedState.CONNECTING, "execute");
                    for (int i = 0; i < this.mDaemons.length; i++) {
                        String[] arguments = this.mArguments[i];
                        if (arguments != null) {
                            String daemon3 = this.mDaemons[i];
                            SystemService.start(daemon3);
                            while (!SystemService.isRunning(daemon3)) {
                                checkpoint(Vpn.LOGD);
                            }
                            this.mSockets[i] = new LocalSocket();
                            LocalSocketAddress address = new LocalSocketAddress(daemon3, LocalSocketAddress.Namespace.RESERVED);
                            while (true) {
                                try {
                                    this.mSockets[i].connect(address);
                                    break;
                                } catch (Exception e) {
                                    checkpoint(Vpn.LOGD);
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
                            out.write(255);
                            out.write(255);
                            out.flush();
                            InputStream in = this.mSockets[i].getInputStream();
                            while (in.read() != -1) {
                                checkpoint(Vpn.LOGD);
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
                        checkpoint(Vpn.LOGD);
                    }
                    String[] parameters = FileUtils.readTextFile(state, 0, null).split("\n", -1);
                    if (parameters.length != 7) {
                        throw new IllegalStateException("Cannot parse the state");
                    }
                    Vpn.this.mConfig.interfaze = parameters[0].trim();
                    Vpn.this.mConfig.addLegacyAddresses(parameters[1]);
                    if (Vpn.this.mConfig.routes == null || Vpn.this.mConfig.routes.isEmpty()) {
                        Vpn.this.mConfig.addLegacyRoutes(parameters[2]);
                    }
                    if (Vpn.this.mConfig.dnsServers == null || Vpn.this.mConfig.dnsServers.size() == 0) {
                        String dnsServers = parameters[3].trim();
                        if (!dnsServers.isEmpty()) {
                            Vpn.this.mConfig.dnsServers = Arrays.asList(dnsServers.split(" "));
                        }
                    }
                    if (Vpn.this.mConfig.searchDomains == null || Vpn.this.mConfig.searchDomains.size() == 0) {
                        String searchDomains = parameters[4].trim();
                        if (!searchDomains.isEmpty()) {
                            Vpn.this.mConfig.searchDomains = Arrays.asList(searchDomains.split(" "));
                        }
                    }
                    String endpoint = parameters[5];
                    if (!endpoint.isEmpty()) {
                        try {
                            InetAddress addr = InetAddress.parseNumericAddress(endpoint);
                            if (addr instanceof Inet4Address) {
                                Vpn.this.mConfig.routes.add(new RouteInfo(new IpPrefix(addr, 32), 9));
                            } else if (addr instanceof Inet6Address) {
                                Vpn.this.mConfig.routes.add(new RouteInfo(new IpPrefix(addr, 128), 9));
                            } else {
                                Log.e(TAG, "Unknown IP address family for VPN endpoint: " + endpoint);
                            }
                        } catch (IllegalArgumentException e2) {
                            Log.e(TAG, "Exception constructing throw route to " + endpoint + ": " + e2);
                        }
                    }
                    synchronized (Vpn.this) {
                        Vpn.this.mConfig.startTime = SystemClock.elapsedRealtime();
                        checkpoint(false);
                        if (Vpn.this.jniCheck(Vpn.this.mConfig.interfaze) == 0) {
                            throw new IllegalStateException(Vpn.this.mConfig.interfaze + " is gone");
                        }
                        Vpn.this.mInterface = Vpn.this.mConfig.interfaze;
                        Vpn.this.mVpnUsers = new ArrayList();
                        Vpn.this.agentConnect();
                        Log.i(TAG, "Connected!");
                    }
                    if (1 == 0) {
                        String[] arr$3 = this.mDaemons;
                        for (String daemon5 : arr$3) {
                            SystemService.stop(daemon5);
                        }
                    }
                    if (1 == 0 || Vpn.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                        Vpn.this.agentDisconnect();
                    }
                } catch (Exception e3) {
                    Log.i(TAG, "Aborting", e3);
                    Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, e3.getMessage());
                    exit();
                    if (0 == 0) {
                        String[] arr$4 = this.mDaemons;
                        for (String daemon6 : arr$4) {
                            SystemService.stop(daemon6);
                        }
                    }
                    if (0 == 0 || Vpn.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                        Vpn.this.agentDisconnect();
                    }
                }
            } catch (Throwable th) {
                if (0 == 0) {
                    String[] arr$5 = this.mDaemons;
                    for (String daemon7 : arr$5) {
                        SystemService.stop(daemon7);
                    }
                }
                if (0 == 0 || Vpn.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                    Vpn.this.agentDisconnect();
                }
                throw th;
            }
        }

        private void monitorDaemons() {
            if (Vpn.this.mNetworkInfo.isConnected()) {
                loop0: while (true) {
                    try {
                        try {
                            Thread.sleep(2000L);
                            for (int i = 0; i < this.mDaemons.length; i++) {
                                if (this.mArguments[i] != null && SystemService.isStopped(this.mDaemons[i])) {
                                    break loop0;
                                }
                            }
                        } catch (InterruptedException e) {
                            Log.d(TAG, "interrupted during monitorDaemons(); stopping services");
                            String[] arr$ = this.mDaemons;
                            for (String daemon : arr$) {
                                SystemService.stop(daemon);
                            }
                            Vpn.this.agentDisconnect();
                            return;
                        }
                    } catch (Throwable th) {
                        String[] arr$2 = this.mDaemons;
                        for (String daemon2 : arr$2) {
                            SystemService.stop(daemon2);
                        }
                        Vpn.this.agentDisconnect();
                        throw th;
                    }
                }
                String[] arr$3 = this.mDaemons;
                for (String daemon3 : arr$3) {
                    SystemService.stop(daemon3);
                }
                Vpn.this.agentDisconnect();
            }
        }
    }
}
