package com.android.server.net;

import android.R;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.IUidObserver;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkPolicyListener;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkIdentity;
import android.net.NetworkInfo;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkQuotaInfo;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IDeviceIdleController;
import android.os.INetworkManagementService;
import android.os.IPowerManager;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.os.PowerManagerInternal;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DebugUtils;
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TrustedTime;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.DeviceIdleController;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.NetworkManagementService;
import com.android.server.SystemConfig;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NetworkPolicyManagerService extends INetworkPolicyManager.Stub {
    private static final String ACTION_ALLOW_BACKGROUND = "com.android.server.net.action.ALLOW_BACKGROUND";
    private static final String ACTION_SNOOZE_WARNING = "com.android.server.net.action.SNOOZE_WARNING";
    private static final String ATTR_APP_ID = "appId";
    private static final String ATTR_CYCLE_DAY = "cycleDay";
    private static final String ATTR_CYCLE_TIMEZONE = "cycleTimezone";
    private static final String ATTR_INFERRED = "inferred";
    private static final String ATTR_LAST_LIMIT_SNOOZE = "lastLimitSnooze";
    private static final String ATTR_LAST_SNOOZE = "lastSnooze";
    private static final String ATTR_LAST_WARNING_SNOOZE = "lastWarningSnooze";
    private static final String ATTR_LIMIT_BYTES = "limitBytes";
    private static final String ATTR_METERED = "metered";
    private static final String ATTR_NETWORK_ID = "networkId";
    private static final String ATTR_NETWORK_TEMPLATE = "networkTemplate";
    private static final String ATTR_POLICY = "policy";
    private static final String ATTR_RESTRICT_BACKGROUND = "restrictBackground";
    private static final String ATTR_SUBSCRIBER_ID = "subscriberId";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_WARNING_BYTES = "warningBytes";
    private static final String INVALID_SUBSCRIBER_ID = "FFFFFFFFFFFFFFF";
    private static final boolean LOGD = true;
    private static final boolean LOGV = true;
    private static final int MSG_ADVISE_PERSIST_THRESHOLD = 7;
    private static final int MSG_INTERFACE_DOWN = 13;
    private static final int MSG_LIMIT_REACHED = 5;
    private static final int MSG_METERED_IFACES_CHANGED = 2;
    private static final int MSG_REMOVE_INTERFACE_QUOTA = 11;
    private static final int MSG_RESTRICT_BACKGROUND_BLACKLIST_CHANGED = 12;
    private static final int MSG_RESTRICT_BACKGROUND_CHANGED = 6;
    private static final int MSG_RESTRICT_BACKGROUND_WHITELIST_CHANGED = 9;
    private static final int MSG_RULES_CHANGED = 1;
    private static final int MSG_SCREEN_ON_CHANGED = 8;
    private static final int MSG_UPDATE_INTERFACE_QUOTA = 10;
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_RESTRICT_BACKGROUND = "restrict-background";
    private static final String TAG_REVOKED_RESTRICT_BACKGROUND = "revoked-restrict-background";
    private static final String TAG_UID_POLICY = "uid-policy";
    private static final String TAG_WHITELIST = "whitelist";
    private static final long TIME_CACHE_MAX_AGE = 86400000;
    public static final int TYPE_LIMIT = 2;
    public static final int TYPE_LIMIT_SNOOZED = 3;
    public static final int TYPE_WARNING = 1;
    private static final int VERSION_ADDED_INFERRED = 7;
    private static final int VERSION_ADDED_METERED = 4;
    private static final int VERSION_ADDED_NETWORK_ID = 9;
    private static final int VERSION_ADDED_RESTRICT_BACKGROUND = 3;
    private static final int VERSION_ADDED_SNOOZE = 2;
    private static final int VERSION_ADDED_TIMEZONE = 6;
    private static final int VERSION_INIT = 1;
    private static final int VERSION_LATEST = 10;
    private static final int VERSION_SPLIT_SNOOZE = 5;
    private static final int VERSION_SWITCH_APP_ID = 8;
    private static final int VERSION_SWITCH_UID = 10;
    private final ArraySet<String> mActiveNotifs;
    private final IActivityManager mActivityManager;
    private final INetworkManagementEventObserver mAlertObserver;
    private final BroadcastReceiver mAllowReceiver;
    private final AppOpsManager mAppOps;
    private IConnectivityManager mConnManager;
    private BroadcastReceiver mConnReceiver;
    private INetworkPolicyListener mConnectivityListener;
    private final Context mContext;
    private final SparseBooleanArray mDefaultRestrictBackgroundWhitelistUids;
    private IDeviceIdleController mDeviceIdleController;
    volatile boolean mDeviceIdleMode;
    final SparseBooleanArray mFirewallChainStates;
    final Handler mHandler;
    private Handler.Callback mHandlerCallback;
    private final IPackageManager mIPm;
    private final RemoteCallbackList<INetworkPolicyListener> mListeners;
    private ArraySet<String> mMeteredIfaces;
    private final INetworkManagementService mNetworkManager;
    final ArrayMap<NetworkTemplate, NetworkPolicy> mNetworkPolicy;
    final ArrayMap<NetworkPolicy, String[]> mNetworkRules;
    private final INetworkStatsService mNetworkStats;
    private INotificationManager mNotifManager;
    private final ArraySet<NetworkTemplate> mOverLimitNotified;
    private final BroadcastReceiver mPackageReceiver;
    private final AtomicFile mPolicyFile;
    private final IPowerManager mPowerManager;
    private PowerManagerInternal mPowerManagerInternal;
    private final SparseBooleanArray mPowerSaveTempWhitelistAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds;
    private final BroadcastReceiver mPowerSaveWhitelistReceiver;
    volatile boolean mRestrictBackground;
    private final SparseBooleanArray mRestrictBackgroundWhitelistRevokedUids;
    private final SparseBooleanArray mRestrictBackgroundWhitelistUids;
    volatile boolean mRestrictPower;
    final Object mRulesLock;
    volatile boolean mScreenOn;
    private final BroadcastReceiver mScreenReceiver;
    private final BroadcastReceiver mSimStateReceiver;
    private final BroadcastReceiver mSnoozeWarningReceiver;
    private final BroadcastReceiver mStatsReceiver;
    private final boolean mSuppressDefaultPolicy;
    volatile boolean mSystemReady;
    private final Runnable mTempPowerSaveChangedCallback;
    private final TrustedTime mTime;
    final SparseIntArray mUidFirewallDozableRules;
    final SparseIntArray mUidFirewallPowerSaveRules;
    final SparseIntArray mUidFirewallStandbyRules;
    private final IUidObserver mUidObserver;
    final SparseIntArray mUidPolicy;
    private final BroadcastReceiver mUidRemovedReceiver;
    final SparseIntArray mUidRules;
    final SparseIntArray mUidState;
    private UsageStatsManagerInternal mUsageStats;
    private final UserManager mUserManager;
    private final BroadcastReceiver mUserReceiver;
    private final BroadcastReceiver mWifiConfigReceiver;
    private final BroadcastReceiver mWifiStateReceiver;
    static final String TAG = "NetworkPolicy";
    private static final boolean ENG_DBG = Log.isLoggable(TAG, 3);

    public NetworkPolicyManagerService(Context context, IActivityManager activityManager, IPowerManager powerManager, INetworkStatsService networkStats, INetworkManagementService networkManagement) {
        this(context, activityManager, powerManager, networkStats, networkManagement, NtpTrustedTime.getInstance(context), getSystemDir(), false);
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public NetworkPolicyManagerService(Context context, IActivityManager activityManager, IPowerManager powerManager, INetworkStatsService networkStats, INetworkManagementService networkManagement, TrustedTime time, File systemDir, boolean suppressDefaultPolicy) {
        this.mRulesLock = new Object();
        this.mNetworkPolicy = new ArrayMap<>();
        this.mNetworkRules = new ArrayMap<>();
        this.mUidPolicy = new SparseIntArray();
        this.mUidRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistAppIds = new SparseBooleanArray();
        this.mPowerSaveTempWhitelistAppIds = new SparseBooleanArray();
        this.mRestrictBackgroundWhitelistUids = new SparseBooleanArray();
        this.mDefaultRestrictBackgroundWhitelistUids = new SparseBooleanArray();
        this.mRestrictBackgroundWhitelistRevokedUids = new SparseBooleanArray();
        this.mMeteredIfaces = new ArraySet<>();
        this.mOverLimitNotified = new ArraySet<>();
        this.mActiveNotifs = new ArraySet<>();
        this.mUidState = new SparseIntArray();
        this.mListeners = new RemoteCallbackList<>();
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int uid, int procState) throws RemoteException {
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.updateUidStateLocked(uid, procState);
                }
                NetworkPolicyManagerService.this.updateNetworkStats(uid, NetworkPolicyManagerService.this.isUidStateForegroundLocked(procState));
            }

            public void onUidGone(int uid) throws RemoteException {
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.removeUidStateLocked(uid);
                }
                NetworkPolicyManagerService.this.updateNetworkStats(uid, false);
            }

            public void onUidActive(int uid) throws RemoteException {
            }

            public void onUidIdle(int uid) throws RemoteException {
            }
        };
        this.mPowerSaveWhitelistReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.updatePowerSaveWhitelistLocked();
                    NetworkPolicyManagerService.this.updateRulesForGlobalChangeLocked(false);
                }
            }
        };
        this.mTempPowerSaveChangedCallback = new Runnable() {
            @Override
            public void run() {
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.updatePowerSaveTempWhitelistLocked();
                    NetworkPolicyManagerService.this.updateRulesForTempWhitelistChangeLocked();
                    NetworkPolicyManagerService.this.purgePowerSaveTempWhitelistLocked();
                }
            }
        };
        this.mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.mHandler.obtainMessage(8).sendToTarget();
            }
        };
        this.mPackageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid == -1 || !"android.intent.action.PACKAGE_ADDED".equals(action)) {
                    return;
                }
                Slog.v(NetworkPolicyManagerService.TAG, "ACTION_PACKAGE_ADDED for uid=" + uid);
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    if (NetworkPolicyManagerService.this.mDefaultRestrictBackgroundWhitelistUids.get(uid) && !NetworkPolicyManagerService.this.mRestrictBackgroundWhitelistRevokedUids.get(uid)) {
                        Slog.v(NetworkPolicyManagerService.TAG, "add default white list back, uid=" + uid);
                        NetworkPolicyManagerService.this.addRestrictBackgroundWhitelistedUid(uid);
                    }
                    NetworkPolicyManagerService.this.updateRestrictionRulesForUidLocked(uid);
                }
            }
        };
        this.mUidRemovedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid == -1) {
                    return;
                }
                Slog.v(NetworkPolicyManagerService.TAG, "ACTION_UID_REMOVED for uid=" + uid);
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.mUidPolicy.delete(uid);
                    NetworkPolicyManagerService.this.removeRestrictBackgroundWhitelistedUidLocked(uid, true, true);
                    NetworkPolicyManagerService.this.updateRestrictionRulesForUidLocked(uid);
                    NetworkPolicyManagerService.this.writePolicyLocked();
                }
            }
        };
        this.mUserReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId == -1) {
                    return;
                }
                if (!action.equals("android.intent.action.USER_REMOVED") && !action.equals("android.intent.action.USER_ADDED")) {
                    return;
                }
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.removeUserStateLocked(userId, true);
                    if (action == "android.intent.action.USER_ADDED") {
                        NetworkPolicyManagerService.this.addDefaultRestrictBackgroundWhitelistUidsLocked(userId);
                    }
                    NetworkPolicyManagerService.this.updateRulesForGlobalChangeLocked(true);
                }
            }
        };
        this.mStatsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.maybeRefreshTrustedTime();
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.updateNetworkEnabledLocked();
                    NetworkPolicyManagerService.this.updateNotificationsLocked();
                }
            }
        };
        this.mAllowReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.setRestrictBackground(false);
            }
        };
        this.mSnoozeWarningReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkTemplate template = intent.getParcelableExtra("android.net.NETWORK_TEMPLATE");
                NetworkPolicyManagerService.this.performSnooze(template, 1);
            }
        };
        this.mWifiConfigReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int reason = intent.getIntExtra("changeReason", 0);
                if (reason != 1) {
                    return;
                }
                WifiConfiguration config = (WifiConfiguration) intent.getParcelableExtra("wifiConfiguration");
                if (config.SSID == null) {
                    return;
                }
                NetworkTemplate template = NetworkTemplate.buildTemplateWifi(config.SSID);
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    if (NetworkPolicyManagerService.this.mNetworkPolicy.containsKey(template)) {
                        NetworkPolicyManagerService.this.mNetworkPolicy.remove(template);
                        NetworkPolicyManagerService.this.writePolicyLocked();
                    }
                }
            }
        };
        this.mWifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netInfo.isConnected()) {
                    WifiInfo info = (WifiInfo) intent.getParcelableExtra("wifiInfo");
                    boolean meteredHint = info.getMeteredHint();
                    NetworkTemplate template = NetworkTemplate.buildTemplateWifi(info.getSSID());
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        NetworkPolicy policy = NetworkPolicyManagerService.this.mNetworkPolicy.get(template);
                        if (policy == null && meteredHint) {
                            NetworkPolicyManagerService.this.addNetworkPolicyLocked(NetworkPolicyManagerService.newWifiPolicy(template, meteredHint));
                        } else if (policy != null && policy.inferred) {
                            policy.metered = meteredHint;
                            NetworkPolicyManagerService.this.updateNetworkRulesLocked();
                        }
                    }
                }
            }
        };
        this.mAlertObserver = new BaseNetworkObserver() {
            public void limitReached(String limitName, String iface) {
                NetworkPolicyManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkPolicyManagerService.TAG);
                if (NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(limitName)) {
                    return;
                }
                NetworkPolicyManagerService.this.mHandler.obtainMessage(5, iface).sendToTarget();
            }

            public void interfaceRemoved(String iface) {
                Slog.d(NetworkPolicyManagerService.TAG, "interfaceRemoved: " + iface);
                if (iface.contains("ccmni") || iface.contains("ppp") || iface.contains("cc2mni") || iface.contains("ccemni")) {
                    NetworkPolicyManagerService.this.mHandler.obtainMessage(13, iface).sendToTarget();
                } else {
                    Slog.d(NetworkPolicyManagerService.TAG, "interfaceRemoved: ignore " + iface);
                }
            }
        };
        this.mConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("com.mediatek.conn.MMS_CONNECTIVITY".equals(intent.getAction())) {
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        NetworkPolicyManagerService.this.normalizePoliciesLocked();
                        NetworkPolicyManagerService.this.updateNetworkRulesLocked();
                    }
                    return;
                }
                NetworkPolicyManagerService.this.maybeRefreshTrustedTime();
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.ensureActiveMobilePolicyLocked();
                    NetworkPolicyManagerService.this.normalizePoliciesLocked();
                    NetworkPolicyManagerService.this.updateNetworkEnabledLocked();
                    NetworkPolicyManagerService.this.updateNetworkRulesLocked();
                    NetworkPolicyManagerService.this.updateNotificationsLocked();
                }
            }
        };
        this.mSimStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!"android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                    return;
                }
                String simState = intent.getStringExtra("ss");
                if (!"LOADED".equals(simState) && !"LOCKED".equals(simState) && !"ABSENT".equals(simState)) {
                    return;
                }
                Slog.d(NetworkPolicyManagerService.TAG, "receive ACTION_SIM_STATE_CHANGED");
                NetworkPolicyManagerService.this.maybeRefreshTrustedTime();
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.ensureActiveMobilePolicyLocked();
                    NetworkPolicyManagerService.this.normalizePoliciesLocked();
                    NetworkPolicyManagerService.this.updateNetworkEnabledLocked();
                    NetworkPolicyManagerService.this.updateNetworkRulesLocked();
                    NetworkPolicyManagerService.this.updateNotificationsLocked();
                }
            }
        };
        this.mHandlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.v(NetworkPolicyManagerService.TAG, "handleMessage(): msg=" + msg.what);
                switch (msg.what) {
                    case 1:
                        int uid = msg.arg1;
                        int uidRules = msg.arg2;
                        NetworkPolicyManagerService.this.dispatchUidRulesChanged(NetworkPolicyManagerService.this.mConnectivityListener, uid, uidRules);
                        int length = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i = 0; i < length; i++) {
                            INetworkPolicyListener listener = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i);
                            NetworkPolicyManagerService.this.dispatchUidRulesChanged(listener, uid, uidRules);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 2:
                        String[] meteredIfaces = (String[]) msg.obj;
                        NetworkPolicyManagerService.this.dispatchMeteredIfacesChanged(NetworkPolicyManagerService.this.mConnectivityListener, meteredIfaces);
                        int length2 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i2 = 0; i2 < length2; i2++) {
                            INetworkPolicyListener listener2 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i2);
                            NetworkPolicyManagerService.this.dispatchMeteredIfacesChanged(listener2, meteredIfaces);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 3:
                    case 4:
                    default:
                        return false;
                    case 5:
                        String iface = (String) msg.obj;
                        NetworkPolicyManagerService.this.maybeRefreshTrustedTime();
                        synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                            if (NetworkPolicyManagerService.this.mMeteredIfaces.contains(iface)) {
                                try {
                                    NetworkPolicyManagerService.this.mNetworkStats.forceUpdate();
                                    break;
                                } catch (RemoteException e) {
                                }
                                NetworkPolicyManagerService.this.updateNetworkEnabledLocked();
                                NetworkPolicyManagerService.this.updateNotificationsLocked();
                            }
                            break;
                        }
                        return true;
                    case 6:
                        boolean restrictBackground = msg.arg1 != 0;
                        NetworkPolicyManagerService.this.dispatchRestrictBackgroundChanged(NetworkPolicyManagerService.this.mConnectivityListener, restrictBackground);
                        int length3 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i3 = 0; i3 < length3; i3++) {
                            INetworkPolicyListener listener3 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i3);
                            NetworkPolicyManagerService.this.dispatchRestrictBackgroundChanged(listener3, restrictBackground);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                        intent.setFlags(1073741824);
                        NetworkPolicyManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                        return true;
                    case 7:
                        long lowestRule = ((Long) msg.obj).longValue();
                        try {
                            long persistThreshold = lowestRule / 1000;
                            NetworkPolicyManagerService.this.mNetworkStats.advisePersistThreshold(persistThreshold);
                            return true;
                        } catch (RemoteException e2) {
                            return true;
                        }
                    case 8:
                        NetworkPolicyManagerService.this.updateScreenOn();
                        return true;
                    case 9:
                        int uid2 = msg.arg1;
                        boolean changed = msg.arg2 == 1;
                        Boolean whitelisted = (Boolean) msg.obj;
                        if (whitelisted != null) {
                            boolean whitelistedBool = whitelisted.booleanValue();
                            NetworkPolicyManagerService.this.dispatchRestrictBackgroundWhitelistChanged(NetworkPolicyManagerService.this.mConnectivityListener, uid2, whitelistedBool);
                            int length4 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                            for (int i4 = 0; i4 < length4; i4++) {
                                INetworkPolicyListener listener4 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i4);
                                NetworkPolicyManagerService.this.dispatchRestrictBackgroundWhitelistChanged(listener4, uid2, whitelistedBool);
                            }
                            NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        }
                        PackageManager pm = NetworkPolicyManagerService.this.mContext.getPackageManager();
                        String[] packages = pm.getPackagesForUid(uid2);
                        if (changed && packages != null) {
                            int userId = UserHandle.getUserId(uid2);
                            for (String packageName : packages) {
                                Intent intent2 = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                                intent2.setPackage(packageName);
                                intent2.setFlags(1073741824);
                                NetworkPolicyManagerService.this.mContext.sendBroadcastAsUser(intent2, UserHandle.of(userId));
                            }
                            return true;
                        }
                        return true;
                    case 10:
                        NetworkPolicyManagerService.this.removeInterfaceQuota((String) msg.obj);
                        NetworkPolicyManagerService.this.setInterfaceQuota((String) msg.obj, (((long) msg.arg1) << 32) | (((long) msg.arg2) & 4294967295L));
                        return true;
                    case 11:
                        NetworkPolicyManagerService.this.removeInterfaceQuota((String) msg.obj);
                        return true;
                    case 12:
                        int uid3 = msg.arg1;
                        boolean blacklisted = msg.arg2 == 1;
                        NetworkPolicyManagerService.this.dispatchRestrictBackgroundBlacklistChanged(NetworkPolicyManagerService.this.mConnectivityListener, uid3, blacklisted);
                        int length5 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i5 = 0; i5 < length5; i5++) {
                            INetworkPolicyListener listener5 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i5);
                            NetworkPolicyManagerService.this.dispatchRestrictBackgroundBlacklistChanged(listener5, uid3, blacklisted);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 13:
                        NetworkPolicyManagerService.this.maybeRefreshTrustedTime();
                        synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                            Slog.d(NetworkPolicyManagerService.TAG, " MSG_INTERFACE_DOWN call updateNetworkRulesLocked");
                            NetworkPolicyManagerService.this.updateNetworkRulesLocked();
                        }
                        return true;
                }
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing context");
        this.mActivityManager = (IActivityManager) Preconditions.checkNotNull(activityManager, "missing activityManager");
        this.mPowerManager = (IPowerManager) Preconditions.checkNotNull(powerManager, "missing powerManager");
        this.mNetworkStats = (INetworkStatsService) Preconditions.checkNotNull(networkStats, "missing networkStats");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(networkManagement, "missing networkManagement");
        this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        this.mTime = (TrustedTime) Preconditions.checkNotNull(time, "missing TrustedTime");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mIPm = AppGlobals.getPackageManager();
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this.mHandlerCallback);
        this.mSuppressDefaultPolicy = suppressDefaultPolicy;
        this.mPolicyFile = new AtomicFile(new File(systemDir, "netpolicy.xml"));
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        LocalServices.addService(NetworkPolicyManagerInternal.class, new NetworkPolicyManagerInternalImpl(this, null));
    }

    public void bindConnectivityManager(IConnectivityManager connManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(connManager, "missing IConnectivityManager");
    }

    public void bindNotificationManager(INotificationManager notifManager) {
        this.mNotifManager = (INotificationManager) Preconditions.checkNotNull(notifManager, "missing INotificationManager");
    }

    void updatePowerSaveWhitelistLocked() {
        try {
            int[] whitelist = this.mDeviceIdleController.getAppIdWhitelistExceptIdle();
            this.mPowerSaveWhitelistExceptIdleAppIds.clear();
            if (whitelist != null) {
                for (int uid : whitelist) {
                    this.mPowerSaveWhitelistExceptIdleAppIds.put(uid, true);
                }
            }
            int[] whitelist2 = this.mDeviceIdleController.getAppIdWhitelist();
            this.mPowerSaveWhitelistAppIds.clear();
            if (whitelist2 == null) {
                return;
            }
            for (int uid2 : whitelist2) {
                this.mPowerSaveWhitelistAppIds.put(uid2, true);
            }
        } catch (RemoteException e) {
        }
    }

    public static boolean isUnderCryptKeeper() {
        String status = SystemProperties.get("vold.decrypt");
        return "trigger_restart_min_framework".equals(status);
    }

    boolean addDefaultRestrictBackgroundWhitelistUidsLocked() {
        List<UserInfo> users = this.mUserManager.getUsers();
        int numberUsers = users.size();
        boolean changed = false;
        for (int i = 0; i < numberUsers; i++) {
            UserInfo user = users.get(i);
            if (addDefaultRestrictBackgroundWhitelistUidsLocked(user.id)) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean addDefaultRestrictBackgroundWhitelistUidsLocked(int userId) {
        SystemConfig sysConfig = SystemConfig.getInstance();
        PackageManager pm = this.mContext.getPackageManager();
        ArraySet<String> allowDataUsage = sysConfig.getAllowInDataUsageSave();
        boolean changed = false;
        for (int i = 0; i < allowDataUsage.size(); i++) {
            String pkg = allowDataUsage.valueAt(i);
            Slog.d(TAG, "checking restricted background whitelisting for package " + pkg + " and user " + userId);
            try {
                ApplicationInfo app = pm.getApplicationInfoAsUser(pkg, PackageManagerService.DumpState.DUMP_DEXOPT, userId);
                if (!app.isPrivilegedApp()) {
                    Slog.wtf(TAG, "pm.getApplicationInfoAsUser() returned non-privileged app: " + pkg);
                } else {
                    int uid = UserHandle.getUid(userId, app.uid);
                    this.mDefaultRestrictBackgroundWhitelistUids.append(uid, true);
                    Slog.d(TAG, "Adding uid " + uid + " (user " + userId + ") to default restricted background whitelist. Revoked status: " + this.mRestrictBackgroundWhitelistRevokedUids.get(uid));
                    if (!this.mRestrictBackgroundWhitelistRevokedUids.get(uid)) {
                        Slog.i(TAG, "adding default package " + pkg + " (uid " + uid + " for user " + userId + ") to restrict background whitelist");
                        this.mRestrictBackgroundWhitelistUids.append(uid, true);
                        changed = true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                if (!isUnderCryptKeeper()) {
                    Slog.wtf(TAG, "No ApplicationInfo for package " + pkg);
                }
            }
        }
        return changed;
    }

    void updatePowerSaveTempWhitelistLocked() {
        try {
            int N = this.mPowerSaveTempWhitelistAppIds.size();
            for (int i = 0; i < N; i++) {
                this.mPowerSaveTempWhitelistAppIds.setValueAt(i, false);
            }
            int[] whitelist = this.mDeviceIdleController.getAppIdTempWhitelist();
            if (whitelist == null) {
                return;
            }
            for (int uid : whitelist) {
                this.mPowerSaveTempWhitelistAppIds.put(uid, true);
            }
        } catch (RemoteException e) {
        }
    }

    void purgePowerSaveTempWhitelistLocked() {
        int N = this.mPowerSaveTempWhitelistAppIds.size();
        for (int i = N - 1; i >= 0; i--) {
            if (!this.mPowerSaveTempWhitelistAppIds.valueAt(i)) {
                this.mPowerSaveTempWhitelistAppIds.removeAt(i);
            }
        }
    }

    public void systemReady() {
        if (!isBandwidthControlEnabled()) {
            Slog.w(TAG, "bandwidth controls disabled, unable to enforce policy");
            return;
        }
        this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        synchronized (this.mRulesLock) {
            updatePowerSaveWhitelistLocked();
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
            this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                public void onLowPowerModeChanged(boolean enabled) {
                    Slog.d(NetworkPolicyManagerService.TAG, "onLowPowerModeChanged(" + enabled + ")");
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        if (NetworkPolicyManagerService.this.mRestrictPower != enabled) {
                            NetworkPolicyManagerService.this.mRestrictPower = enabled;
                            NetworkPolicyManagerService.this.updateRulesForRestrictPowerLocked();
                            NetworkPolicyManagerService.this.updateRulesForGlobalChangeLocked(true);
                        }
                    }
                }
            });
            this.mRestrictPower = this.mPowerManagerInternal.getLowPowerModeEnabled();
            this.mSystemReady = true;
            readPolicyLocked();
            if (addDefaultRestrictBackgroundWhitelistUidsLocked()) {
                writePolicyLocked();
            }
            updateRulesForGlobalChangeLocked(false);
            updateNotificationsLocked();
        }
        updateScreenOn();
        try {
            this.mActivityManager.registerUidObserver(this.mUidObserver, 3);
            this.mNetworkManager.registerObserver(this.mAlertObserver);
        } catch (RemoteException e) {
        }
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction("android.intent.action.SCREEN_ON");
        screenFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mScreenReceiver, screenFilter);
        IntentFilter whitelistFilter = new IntentFilter("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        this.mContext.registerReceiver(this.mPowerSaveWhitelistReceiver, whitelistFilter, null, this.mHandler);
        DeviceIdleController.LocalService deviceIdleService = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
        deviceIdleService.setNetworkPolicyTempWhitelistCallback(this.mTempPowerSaveChangedCallback);
        IntentFilter connFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        connFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        connFilter.addAction("com.mediatek.conn.MMS_CONNECTIVITY");
        this.mContext.registerReceiver(this.mConnReceiver, connFilter, "android.permission.CONNECTIVITY_INTERNAL", this.mHandler);
        IntentFilter mSimFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mSimStateReceiver, mSimFilter);
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageReceiver, packageFilter, null, this.mHandler);
        this.mContext.registerReceiver(this.mUidRemovedReceiver, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
        IntentFilter statsFilter = new IntentFilter(NetworkStatsService.ACTION_NETWORK_STATS_UPDATED);
        this.mContext.registerReceiver(this.mStatsReceiver, statsFilter, "android.permission.READ_NETWORK_USAGE_HISTORY", this.mHandler);
        IntentFilter allowFilter = new IntentFilter(ACTION_ALLOW_BACKGROUND);
        this.mContext.registerReceiver(this.mAllowReceiver, allowFilter, "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
        IntentFilter snoozeWarningFilter = new IntentFilter(ACTION_SNOOZE_WARNING);
        this.mContext.registerReceiver(this.mSnoozeWarningReceiver, snoozeWarningFilter, "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
        IntentFilter wifiConfigFilter = new IntentFilter("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mContext.registerReceiver(this.mWifiConfigReceiver, wifiConfigFilter, null, this.mHandler);
        IntentFilter wifiStateFilter = new IntentFilter("android.net.wifi.STATE_CHANGE");
        this.mContext.registerReceiver(this.mWifiStateReceiver, wifiStateFilter, null, this.mHandler);
        this.mUsageStats.addAppIdleStateChangeListener(new AppIdleStateChangeListener(this, null));
    }

    static NetworkPolicy newWifiPolicy(NetworkTemplate template, boolean metered) {
        return new NetworkPolicy(template, -1, "UTC", -1L, -1L, -1L, -1L, metered, true);
    }

    void updateNotificationsLocked() {
        Slog.v(TAG, "updateNotificationsLocked()");
        ArraySet<String> beforeNotifs = new ArraySet<>(this.mActiveNotifs);
        this.mActiveNotifs.clear();
        long currentTime = currentTimeMillis();
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
            if (isTemplateRelevant(policy.template) && policy.hasCycle()) {
                long start = NetworkPolicyManager.computeLastCycleBoundary(currentTime, policy);
                long totalBytes = getTotalBytes(policy.template, start, currentTime);
                if (!policy.isOverLimit(totalBytes)) {
                    notifyUnderLimitLocked(policy.template);
                    if (policy.isOverWarning(totalBytes) && policy.lastWarningSnooze < start) {
                        enqueueNotification(policy, 1, totalBytes);
                    }
                } else if (policy.lastLimitSnooze >= start) {
                    enqueueNotification(policy, 3, totalBytes);
                } else {
                    enqueueNotification(policy, 2, totalBytes);
                    notifyOverLimitLocked(policy.template);
                }
            }
        }
        for (int i2 = beforeNotifs.size() - 1; i2 >= 0; i2--) {
            String tag = beforeNotifs.valueAt(i2);
            if (!this.mActiveNotifs.contains(tag)) {
                cancelNotification(tag);
            }
        }
    }

    private boolean isTemplateRelevant(NetworkTemplate template) {
        if (!template.isMatchRuleMobile()) {
            return true;
        }
        TelephonyManager tele = TelephonyManager.from(this.mContext);
        SubscriptionManager sub = SubscriptionManager.from(this.mContext);
        int[] subIds = sub.getActiveSubscriptionIdList();
        for (int subId : subIds) {
            String subscriberId = tele.getSubscriberId(subId);
            NetworkIdentity probeIdent = new NetworkIdentity(0, 0, subscriberId, (String) null, false, true);
            if (template.matches(probeIdent)) {
                return true;
            }
        }
        return false;
    }

    private void notifyOverLimitLocked(NetworkTemplate template) {
        if (this.mOverLimitNotified.contains(template)) {
            return;
        }
        this.mContext.startActivityAsUser(buildNetworkOverLimitIntent(template), UserHandle.CURRENT);
        this.mOverLimitNotified.add(template);
    }

    private void notifyUnderLimitLocked(NetworkTemplate template) {
        this.mOverLimitNotified.remove(template);
    }

    private String buildNotificationTag(NetworkPolicy policy, int type) {
        return "NetworkPolicy:" + policy.template.hashCode() + ":" + type;
    }

    private void enqueueNotification(NetworkPolicy policy, int type, long totalBytes) {
        CharSequence text;
        CharSequence text2;
        String tag = buildNotificationTag(policy, type);
        Notification.Builder builder = new Notification.Builder(this.mContext);
        builder.setOnlyAlertOnce(true);
        builder.setWhen(0L);
        builder.setColor(this.mContext.getColor(R.color.system_accent3_600));
        Resources res = this.mContext.getResources();
        switch (type) {
            case 1:
                CharSequence title = res.getText(R.string.imProtocolMsn);
                CharSequence body = res.getString(R.string.imProtocolNetMeeting);
                builder.setSmallIcon(R.drawable.stat_notify_error);
                builder.setTicker(title);
                builder.setContentTitle(title);
                builder.setContentText(body);
                Intent snoozeIntent = buildSnoozeWarningIntent(policy.template);
                builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, snoozeIntent, 134217728));
                Intent viewIntent = buildViewDataUsageIntent(policy.template);
                builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, viewIntent, 134217728, null, UserHandle.CURRENT));
                break;
            case 2:
                CharSequence body2 = res.getText(R.string.imTypeHome);
                int icon = R.drawable.list_selector_background_default_light;
                switch (policy.template.getMatchRule()) {
                    case 1:
                        text2 = res.getText(R.string.imProtocolYahoo);
                        break;
                    case 2:
                        text2 = res.getText(R.string.imProtocolQq);
                        break;
                    case 3:
                        text2 = res.getText(R.string.imProtocolSkype);
                        break;
                    case 4:
                        text2 = res.getText(R.string.imTypeCustom);
                        icon = R.drawable.stat_notify_error;
                        break;
                    default:
                        text2 = null;
                        break;
                }
                builder.setOngoing(true);
                builder.setSmallIcon(icon);
                builder.setTicker(text2);
                builder.setContentTitle(text2);
                builder.setContentText(body2);
                Intent intent = buildNetworkOverLimitIntent(policy.template);
                builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 134217728, null, UserHandle.CURRENT));
                break;
            case 3:
                long overBytes = totalBytes - policy.limitBytes;
                CharSequence body3 = res.getString(R.string.ime_action_done, Formatter.formatFileSize(this.mContext, overBytes));
                switch (policy.template.getMatchRule()) {
                    case 1:
                        text = res.getText(R.string.image_wallpaper_component);
                        break;
                    case 2:
                        text = res.getText(R.string.imTypeOther);
                        break;
                    case 3:
                        text = res.getText(R.string.imTypeWork);
                        break;
                    case 4:
                        text = res.getText(R.string.ime_action_default);
                        break;
                    default:
                        text = null;
                        break;
                }
                builder.setOngoing(true);
                builder.setSmallIcon(R.drawable.stat_notify_error);
                builder.setTicker(text);
                builder.setContentTitle(text);
                builder.setContentText(body3);
                Intent intent2 = buildViewDataUsageIntent(policy.template);
                builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent2, 134217728, null, UserHandle.CURRENT));
                break;
        }
        try {
            String packageName = this.mContext.getPackageName();
            int[] idReceived = new int[1];
            this.mNotifManager.enqueueNotificationWithTag(packageName, packageName, tag, 0, builder.getNotification(), idReceived, -1);
            this.mActiveNotifs.add(tag);
        } catch (RemoteException e) {
        }
    }

    private void cancelNotification(String tag) {
        try {
            String packageName = this.mContext.getPackageName();
            this.mNotifManager.cancelNotificationWithTag(packageName, tag, 0, -1);
        } catch (RemoteException e) {
        }
    }

    void updateNetworkEnabledLocked() {
        Slog.v(TAG, "updateNetworkEnabledLocked()");
        long currentTime = currentTimeMillis();
        int defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
            Slog.v(TAG, " checking policy:" + policy);
            if (policy.limitBytes == -1 || !policy.hasCycle()) {
                setNetworkTemplateEnabled(defaultSubId, policy.template, true);
            } else {
                long start = NetworkPolicyManager.computeLastCycleBoundary(currentTime, policy);
                long totalBytes = getTotalBytes(policy.template, start, currentTime);
                boolean overLimitWithoutSnooze = policy.isOverLimit(totalBytes) && policy.lastLimitSnooze < start;
                boolean networkEnabled = !overLimitWithoutSnooze;
                setNetworkTemplateEnabled(defaultSubId, policy.template, networkEnabled);
            }
        }
    }

    private void setNetworkTemplateEnabled(int subId, NetworkTemplate template, boolean enabled) {
        TelephonyManager tele = TelephonyManager.from(this.mContext);
        switch (template.getMatchRule()) {
            case 1:
            case 2:
            case 3:
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    int slotId = SubscriptionManager.getSlotId(subId);
                    if (tele.getSimState(slotId) == 5 && Objects.equals(tele.getSubscriberId(subId), template.getSubscriberId())) {
                        Slog.v(TAG, "setPolicyDataEnableForSubscriber:subId(" + subId + ")," + enabled);
                        tele.setPolicyDataEnableForSubscriber(subId, enabled);
                        break;
                    }
                }
                break;
        }
    }

    void updateNetworkRulesLocked() {
        long start;
        long totalBytes;
        long quotaBytes;
        Slog.v(TAG, "updateNetworkRulesLocked()");
        String mmsifacename = null;
        try {
            NetworkState[] states = this.mConnManager.getAllNetworkState();
            NetworkRequest nr = new NetworkRequest.Builder().addTransportType(0).addCapability(0).build();
            Network mmsNetwork = this.mConnManager.getNetworkIfCreated(nr);
            if (ENG_DBG) {
                Slog.d(TAG, "getNetworkIfCreated: mmsNetwork =" + mmsNetwork);
            }
            if (mmsNetwork != null) {
                LinkProperties netProperties = this.mConnManager.getLinkProperties(mmsNetwork);
                if (netProperties != null) {
                    mmsifacename = netProperties.getInterfaceName();
                }
                Slog.v(TAG, "updateNetworkRulesLocked() mmsifacename=" + mmsifacename);
            }
            ArrayList<Pair<String, NetworkIdentity>> connIdents = new ArrayList<>(states.length);
            ArraySet<String> connIfaces = new ArraySet<>(states.length);
            for (NetworkState state : states) {
                if (state.networkInfo != null && state.networkInfo.isConnected()) {
                    NetworkIdentity ident = NetworkIdentity.buildNetworkIdentity(this.mContext, state);
                    String baseIface = state.linkProperties.getInterfaceName();
                    if (baseIface != null) {
                        connIdents.add(Pair.create(baseIface, ident));
                    }
                    List<LinkProperties> stackedLinks = state.linkProperties.getStackedLinks();
                    for (LinkProperties stackedLink : stackedLinks) {
                        String stackedIface = stackedLink.getInterfaceName();
                        if (stackedIface != null) {
                            connIdents.add(Pair.create(stackedIface, ident));
                        }
                    }
                }
            }
            this.mNetworkRules.clear();
            ArrayList<String> ifaceList = Lists.newArrayList();
            for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
                ifaceList.clear();
                for (int j = connIdents.size() - 1; j >= 0; j--) {
                    Pair<String, NetworkIdentity> ident2 = connIdents.get(j);
                    if (policy.template.matches((NetworkIdentity) ident2.second)) {
                        Slog.d(TAG, "add iface for policy:" + policy);
                        ifaceList.add((String) ident2.first);
                    }
                }
                if (ifaceList.size() > 0) {
                    this.mNetworkRules.put(policy, (String[]) ifaceList.toArray(new String[ifaceList.size()]));
                }
            }
            long lowestRule = JobStatus.NO_LATEST_RUNTIME;
            ArraySet<String> newMeteredIfaces = new ArraySet<>(states.length);
            long currentTime = currentTimeMillis();
            for (int i2 = this.mNetworkRules.size() - 1; i2 >= 0; i2--) {
                NetworkPolicy policy2 = this.mNetworkRules.keyAt(i2);
                String[] ifaces = this.mNetworkRules.valueAt(i2);
                if (policy2.hasCycle()) {
                    start = NetworkPolicyManager.computeLastCycleBoundary(currentTime, policy2);
                    totalBytes = getTotalBytes(policy2.template, start, currentTime);
                } else {
                    start = JobStatus.NO_LATEST_RUNTIME;
                    totalBytes = 0;
                }
                Slog.d(TAG, "applying policy " + policy2 + " to ifaces " + Arrays.toString(ifaces));
                boolean hasWarning = policy2.warningBytes != -1;
                boolean hasLimit = policy2.limitBytes != -1;
                if (hasLimit || policy2.metered) {
                    if (!hasLimit || policy2.lastLimitSnooze >= start) {
                        quotaBytes = JobStatus.NO_LATEST_RUNTIME;
                    } else {
                        quotaBytes = Math.max(1L, policy2.limitBytes - totalBytes);
                    }
                    if (ifaces.length > 1) {
                        Slog.w(TAG, "shared quota unsupported; generating rule for each iface");
                    }
                    for (String iface : ifaces) {
                        if (iface != null && iface.length() != 0) {
                            if (iface.equals(mmsifacename)) {
                                Slog.d(TAG, "mmsifacename set quota mms ifacename=" + mmsifacename);
                                quotaBytes = JobStatus.NO_LATEST_RUNTIME;
                            }
                            this.mHandler.obtainMessage(10, (int) (quotaBytes >> 32), (int) ((-1) & quotaBytes), iface).sendToTarget();
                            newMeteredIfaces.add(iface);
                        }
                    }
                }
                if (hasWarning && policy2.warningBytes < lowestRule) {
                    lowestRule = policy2.warningBytes;
                }
                if (hasLimit && policy2.limitBytes < lowestRule) {
                    lowestRule = policy2.limitBytes;
                }
            }
            for (int i3 = connIfaces.size() - 1; i3 >= 0; i3--) {
                String iface2 = connIfaces.valueAt(i3);
                this.mHandler.obtainMessage(10, Integer.MAX_VALUE, -1, iface2).sendToTarget();
                newMeteredIfaces.add(iface2);
            }
            this.mHandler.obtainMessage(7, Long.valueOf(lowestRule)).sendToTarget();
            for (int i4 = this.mMeteredIfaces.size() - 1; i4 >= 0; i4--) {
                String iface3 = this.mMeteredIfaces.valueAt(i4);
                if (!newMeteredIfaces.contains(iface3)) {
                    this.mHandler.obtainMessage(11, iface3).sendToTarget();
                }
            }
            this.mMeteredIfaces = newMeteredIfaces;
            String[] meteredIfaces = (String[]) this.mMeteredIfaces.toArray(new String[this.mMeteredIfaces.size()]);
            this.mHandler.obtainMessage(2, meteredIfaces).sendToTarget();
        } catch (RemoteException e) {
        }
    }

    private void ensureActiveMobilePolicyLocked() {
        Slog.v(TAG, "ensureActiveMobilePolicyLocked()");
        if (this.mSuppressDefaultPolicy) {
            return;
        }
        TelephonyManager tele = TelephonyManager.from(this.mContext);
        SubscriptionManager sub = SubscriptionManager.from(this.mContext);
        int[] subIds = sub.getActiveSubscriptionIdList();
        for (int subId : subIds) {
            int slotId = SubscriptionManager.getSlotId(subId);
            if (SubscriptionManager.isValidSubscriptionId(subId) && tele.getSimState(slotId) == 5) {
                String subscriberId = tele.getSubscriberId(subId);
                ensureActiveMobilePolicyLocked(subscriberId);
            }
        }
    }

    private void ensureActiveMobilePolicyLocked(String subscriberId) {
        if (ENG_DBG) {
            Slog.v(TAG, "ensureActiveMobilePolicyLocked subscriberId(" + subscriberId + ")");
        }
        NetworkIdentity probeIdent = new NetworkIdentity(0, 0, subscriberId, (String) null, false, true);
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkTemplate template = this.mNetworkPolicy.keyAt(i);
            if (template.matches(probeIdent)) {
                Slog.d(TAG, "Found template " + template + " which matches subscriber " + NetworkIdentity.scrubSubscriberId(subscriberId));
                return;
            }
        }
        Slog.i(TAG, "No policy for subscriber " + NetworkIdentity.scrubSubscriberId(subscriberId) + "; generating default policy");
        long warningBytes = ((long) this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceIncreaseDebounce)) * 1048576;
        Time time = new Time();
        time.setToNow();
        int cycleDay = time.monthDay;
        String cycleTimezone = time.timezone;
        NetworkPolicy policy = new NetworkPolicy(NetworkTemplate.buildTemplateMobileAll(subscriberId), cycleDay, cycleTimezone, warningBytes, -1L, -1L, -1L, true, true);
        addNetworkPolicyLocked(policy);
        sendPolicyCreatedBroadcast();
    }

    private void readPolicyLocked() {
        boolean metered;
        Slog.v(TAG, "readPolicyLocked()");
        this.mNetworkPolicy.clear();
        this.mUidPolicy.clear();
        FileInputStream fis = null;
        try {
            fis = this.mPolicyFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            int version = 1;
            boolean insideWhitelist = false;
            while (true) {
                int type = in.next();
                if (type == 1) {
                    return;
                }
                String tag = in.getName();
                if (type == 2) {
                    if (TAG_POLICY_LIST.equals(tag)) {
                        boolean oldValue = this.mRestrictBackground;
                        version = XmlUtils.readIntAttribute(in, ATTR_VERSION);
                        if (version >= 3) {
                            this.mRestrictBackground = XmlUtils.readBooleanAttribute(in, ATTR_RESTRICT_BACKGROUND);
                        } else {
                            this.mRestrictBackground = false;
                        }
                        if (this.mRestrictBackground != oldValue) {
                            this.mHandler.obtainMessage(6, this.mRestrictBackground ? 1 : 0, 0).sendToTarget();
                        }
                    } else if (TAG_NETWORK_POLICY.equals(tag)) {
                        int networkTemplate = XmlUtils.readIntAttribute(in, ATTR_NETWORK_TEMPLATE);
                        String subscriberId = in.getAttributeValue(null, ATTR_SUBSCRIBER_ID);
                        String attributeValue = version >= 9 ? in.getAttributeValue(null, ATTR_NETWORK_ID) : null;
                        int cycleDay = XmlUtils.readIntAttribute(in, ATTR_CYCLE_DAY);
                        String cycleTimezone = version >= 6 ? in.getAttributeValue(null, ATTR_CYCLE_TIMEZONE) : "UTC";
                        long warningBytes = XmlUtils.readLongAttribute(in, ATTR_WARNING_BYTES);
                        long limitBytes = XmlUtils.readLongAttribute(in, ATTR_LIMIT_BYTES);
                        long lastLimitSnooze = version >= 5 ? XmlUtils.readLongAttribute(in, ATTR_LAST_LIMIT_SNOOZE) : version >= 2 ? XmlUtils.readLongAttribute(in, ATTR_LAST_SNOOZE) : -1L;
                        if (version < 4) {
                            switch (networkTemplate) {
                                case 1:
                                case 2:
                                case 3:
                                    metered = true;
                                    break;
                                default:
                                    metered = false;
                                    break;
                            }
                        } else {
                            metered = XmlUtils.readBooleanAttribute(in, ATTR_METERED);
                        }
                        long lastWarningSnooze = version >= 5 ? XmlUtils.readLongAttribute(in, ATTR_LAST_WARNING_SNOOZE) : -1L;
                        boolean booleanAttribute = version >= 7 ? XmlUtils.readBooleanAttribute(in, ATTR_INFERRED) : false;
                        NetworkTemplate template = new NetworkTemplate(networkTemplate, subscriberId, attributeValue);
                        if (template.isPersistable()) {
                            this.mNetworkPolicy.put(template, new NetworkPolicy(template, cycleDay, cycleTimezone, warningBytes, limitBytes, lastWarningSnooze, lastLimitSnooze, metered, booleanAttribute));
                        }
                    } else if (TAG_UID_POLICY.equals(tag)) {
                        int uid = XmlUtils.readIntAttribute(in, "uid");
                        int policy = XmlUtils.readIntAttribute(in, ATTR_POLICY);
                        if (UserHandle.isApp(uid)) {
                            setUidPolicyUncheckedLocked(uid, policy, false);
                        } else {
                            Slog.w(TAG, "unable to apply policy to UID " + uid + "; ignoring");
                        }
                    } else if (TAG_APP_POLICY.equals(tag)) {
                        int appId = XmlUtils.readIntAttribute(in, ATTR_APP_ID);
                        int policy2 = XmlUtils.readIntAttribute(in, ATTR_POLICY);
                        int uid2 = UserHandle.getUid(0, appId);
                        if (UserHandle.isApp(uid2)) {
                            setUidPolicyUncheckedLocked(uid2, policy2, false);
                        } else {
                            Slog.w(TAG, "unable to apply policy to UID " + uid2 + "; ignoring");
                        }
                    } else if (TAG_WHITELIST.equals(tag)) {
                        insideWhitelist = true;
                    } else if (TAG_RESTRICT_BACKGROUND.equals(tag) && insideWhitelist) {
                        this.mRestrictBackgroundWhitelistUids.put(XmlUtils.readIntAttribute(in, "uid"), true);
                    } else if (TAG_REVOKED_RESTRICT_BACKGROUND.equals(tag) && insideWhitelist) {
                        this.mRestrictBackgroundWhitelistRevokedUids.put(XmlUtils.readIntAttribute(in, "uid"), true);
                    }
                } else if (type == 3 && TAG_WHITELIST.equals(tag)) {
                    insideWhitelist = false;
                }
            }
        } catch (IOException e) {
            Log.wtf(TAG, "problem reading network policy", e);
        } catch (FileNotFoundException e2) {
            upgradeLegacyBackgroundData();
        } catch (XmlPullParserException e3) {
            Log.wtf(TAG, "problem reading network policy", e3);
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    private void upgradeLegacyBackgroundData() {
        this.mRestrictBackground = Settings.Secure.getInt(this.mContext.getContentResolver(), "background_data", 1) != 1;
        if (!this.mRestrictBackground) {
            return;
        }
        Intent broadcast = new Intent("android.net.conn.BACKGROUND_DATA_SETTING_CHANGED");
        this.mContext.sendBroadcastAsUser(broadcast, UserHandle.ALL);
    }

    void writePolicyLocked() {
        Slog.v(TAG, "writePolicyLocked()");
        FileOutputStream fos = null;
        try {
            fos = this.mPolicyFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_POLICY_LIST);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_VERSION, 10);
            XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_RESTRICT_BACKGROUND, this.mRestrictBackground);
            for (int i = 0; i < this.mNetworkPolicy.size(); i++) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
                NetworkTemplate template = policy.template;
                if (template.isPersistable()) {
                    fastXmlSerializer.startTag(null, TAG_NETWORK_POLICY);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_NETWORK_TEMPLATE, template.getMatchRule());
                    String subscriberId = template.getSubscriberId();
                    if (subscriberId != null) {
                        fastXmlSerializer.attribute(null, ATTR_SUBSCRIBER_ID, subscriberId);
                    }
                    String networkId = template.getNetworkId();
                    if (networkId != null) {
                        fastXmlSerializer.attribute(null, ATTR_NETWORK_ID, networkId);
                    }
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_CYCLE_DAY, policy.cycleDay);
                    fastXmlSerializer.attribute(null, ATTR_CYCLE_TIMEZONE, policy.cycleTimezone);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_WARNING_BYTES, policy.warningBytes);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LIMIT_BYTES, policy.limitBytes);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LAST_WARNING_SNOOZE, policy.lastWarningSnooze);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LAST_LIMIT_SNOOZE, policy.lastLimitSnooze);
                    XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_METERED, policy.metered);
                    XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_INFERRED, policy.inferred);
                    fastXmlSerializer.endTag(null, TAG_NETWORK_POLICY);
                }
            }
            for (int i2 = 0; i2 < this.mUidPolicy.size(); i2++) {
                int uid = this.mUidPolicy.keyAt(i2);
                int policy2 = this.mUidPolicy.valueAt(i2);
                if (policy2 != 0) {
                    fastXmlSerializer.startTag(null, TAG_UID_POLICY);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, "uid", uid);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_POLICY, policy2);
                    fastXmlSerializer.endTag(null, TAG_UID_POLICY);
                }
            }
            fastXmlSerializer.endTag(null, TAG_POLICY_LIST);
            fastXmlSerializer.startTag(null, TAG_WHITELIST);
            int size = this.mRestrictBackgroundWhitelistUids.size();
            for (int i3 = 0; i3 < size; i3++) {
                int uid2 = this.mRestrictBackgroundWhitelistUids.keyAt(i3);
                fastXmlSerializer.startTag(null, TAG_RESTRICT_BACKGROUND);
                XmlUtils.writeIntAttribute(fastXmlSerializer, "uid", uid2);
                fastXmlSerializer.endTag(null, TAG_RESTRICT_BACKGROUND);
            }
            int size2 = this.mRestrictBackgroundWhitelistRevokedUids.size();
            for (int i4 = 0; i4 < size2; i4++) {
                int uid3 = this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i4);
                fastXmlSerializer.startTag(null, TAG_REVOKED_RESTRICT_BACKGROUND);
                XmlUtils.writeIntAttribute(fastXmlSerializer, "uid", uid3);
                fastXmlSerializer.endTag(null, TAG_REVOKED_RESTRICT_BACKGROUND);
            }
            fastXmlSerializer.endTag(null, TAG_WHITELIST);
            fastXmlSerializer.endDocument();
            this.mPolicyFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos == null) {
                return;
            }
            this.mPolicyFile.failWrite(fos);
        }
    }

    public void setUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mRulesLock) {
            long token = Binder.clearCallingIdentity();
            try {
                int oldPolicy = this.mUidPolicy.get(uid, 0);
                if (oldPolicy != policy) {
                    setUidPolicyUncheckedLocked(uid, oldPolicy, policy, true);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void addUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mRulesLock) {
            int oldPolicy = this.mUidPolicy.get(uid, 0);
            int policy2 = policy | oldPolicy;
            if (oldPolicy != policy2) {
                setUidPolicyUncheckedLocked(uid, oldPolicy, policy2, true);
            }
        }
    }

    public void removeUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mRulesLock) {
            int oldPolicy = this.mUidPolicy.get(uid, 0);
            int policy2 = oldPolicy & (~policy);
            if (oldPolicy != policy2) {
                setUidPolicyUncheckedLocked(uid, oldPolicy, policy2, true);
            }
        }
    }

    private void setUidPolicyUncheckedLocked(int uid, int oldPolicy, int policy, boolean persist) {
        setUidPolicyUncheckedLocked(uid, policy, persist);
        boolean isBlacklisted = policy == 1;
        this.mHandler.obtainMessage(12, uid, isBlacklisted ? 1 : 0).sendToTarget();
        boolean wasBlacklisted = oldPolicy == 1;
        if ((oldPolicy != 0 || !isBlacklisted) && (!wasBlacklisted || policy != 0)) {
            return;
        }
        this.mHandler.obtainMessage(9, uid, 1, null).sendToTarget();
    }

    private void setUidPolicyUncheckedLocked(int uid, int policy, boolean persist) {
        this.mUidPolicy.put(uid, policy);
        updateRulesForDataUsageRestrictionsLocked(uid);
        if (!persist) {
            return;
        }
        writePolicyLocked();
    }

    public int getUidPolicy(int uid) {
        int i;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            i = this.mUidPolicy.get(uid, 0);
        }
        return i;
    }

    public int[] getUidsWithPolicy(int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        int[] uids = new int[0];
        synchronized (this.mRulesLock) {
            for (int i = 0; i < this.mUidPolicy.size(); i++) {
                int uid = this.mUidPolicy.keyAt(i);
                int uidPolicy = this.mUidPolicy.valueAt(i);
                if (uidPolicy == policy) {
                    uids = ArrayUtils.appendInt(uids, uid);
                }
            }
        }
        return uids;
    }

    boolean removeUserStateLocked(int userId, boolean writePolicy) {
        Slog.v(TAG, "removeUserStateLocked()");
        boolean changed = false;
        int[] wlUids = new int[0];
        for (int i = 0; i < this.mRestrictBackgroundWhitelistUids.size(); i++) {
            int uid = this.mRestrictBackgroundWhitelistUids.keyAt(i);
            if (UserHandle.getUserId(uid) == userId) {
                wlUids = ArrayUtils.appendInt(wlUids, uid);
            }
        }
        if (wlUids.length > 0) {
            for (int i2 : wlUids) {
                removeRestrictBackgroundWhitelistedUidLocked(i2, false, false);
            }
            changed = true;
        }
        for (int i3 = this.mRestrictBackgroundWhitelistRevokedUids.size() - 1; i3 >= 0; i3--) {
            if (UserHandle.getUserId(this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i3)) == userId) {
                this.mRestrictBackgroundWhitelistRevokedUids.removeAt(i3);
                changed = true;
            }
        }
        int[] uids = new int[0];
        for (int i4 = 0; i4 < this.mUidPolicy.size(); i4++) {
            int uid2 = this.mUidPolicy.keyAt(i4);
            if (UserHandle.getUserId(uid2) == userId) {
                uids = ArrayUtils.appendInt(uids, uid2);
            }
        }
        if (uids.length > 0) {
            for (int i5 : uids) {
                this.mUidPolicy.delete(i5);
            }
            changed = true;
        }
        updateRulesForGlobalChangeLocked(true);
        if (writePolicy && changed) {
            writePolicyLocked();
        }
        return changed;
    }

    public void setConnectivityListener(INetworkPolicyListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mConnectivityListener != null) {
            throw new IllegalStateException("Connectivity listener already registered");
        }
        this.mConnectivityListener = listener;
    }

    public void registerListener(INetworkPolicyListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mListeners.register(listener);
    }

    public void unregisterListener(INetworkPolicyListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mListeners.unregister(listener);
    }

    public void setNetworkPolicies(NetworkPolicy[] policies) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            maybeRefreshTrustedTime();
            synchronized (this.mRulesLock) {
                for (NetworkPolicy policy : policies) {
                    if (7 == policy.template.getMatchRule()) {
                        throw new IllegalArgumentException("unexpected template in setNetworkPolicies");
                    }
                }
                normalizePoliciesLocked(policies);
                updateNetworkEnabledLocked();
                updateNetworkRulesLocked();
                updateNotificationsLocked();
                writePolicyLocked();
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void addNetworkPolicyLocked(NetworkPolicy policy) {
        Slog.v(TAG, "addNetworkPolicyLocked(" + policy + ")");
        NetworkPolicy[] policies = getNetworkPolicies(this.mContext.getOpPackageName());
        if (7 == policy.template.getMatchRule()) {
            Slog.e(TAG, " Error!! addNetworkPolicyLocked( MATCH_WIFI_WILDCARD )");
        }
        setNetworkPolicies((NetworkPolicy[]) ArrayUtils.appendElement(NetworkPolicy.class, policies, policy));
    }

    public NetworkPolicy[] getNetworkPolicies(String callingPackage) {
        NetworkPolicy[] policies;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", TAG);
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", TAG);
            if (this.mAppOps.noteOp(51, Binder.getCallingUid(), callingPackage) != 0) {
                return new NetworkPolicy[0];
            }
        }
        synchronized (this.mRulesLock) {
            int size = this.mNetworkPolicy.size();
            policies = new NetworkPolicy[size];
            for (int i = 0; i < size; i++) {
                policies[i] = this.mNetworkPolicy.valueAt(i);
            }
        }
        return policies;
    }

    private void normalizePoliciesLocked() {
        normalizePoliciesLocked(getNetworkPolicies(this.mContext.getOpPackageName()));
    }

    private void normalizePoliciesLocked(NetworkPolicy[] policies) {
        TelephonyManager tele = TelephonyManager.from(this.mContext);
        String[] merged = tele.getMergedSubscriberIds();
        this.mNetworkPolicy.clear();
        for (NetworkPolicy policy : policies) {
            policy.template = NetworkTemplate.normalize(policy.template, merged);
            NetworkPolicy existing = this.mNetworkPolicy.get(policy.template);
            if (existing == null || existing.compareTo(policy) > 0) {
                if (existing != null) {
                    Slog.d(TAG, "Normalization replaced " + existing + " with " + policy);
                }
                this.mNetworkPolicy.put(policy.template, policy);
            }
        }
    }

    public void snoozeLimit(NetworkTemplate template) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            performSnooze(template, 2);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void performSnooze(NetworkTemplate template, int type) {
        maybeRefreshTrustedTime();
        long currentTime = currentTimeMillis();
        synchronized (this.mRulesLock) {
            NetworkPolicy policy = this.mNetworkPolicy.get(template);
            if (policy == null) {
                throw new IllegalArgumentException("unable to find policy for " + template);
            }
            switch (type) {
                case 1:
                    policy.lastWarningSnooze = currentTime;
                    break;
                case 2:
                    policy.lastLimitSnooze = currentTime;
                    break;
                default:
                    throw new IllegalArgumentException("unexpected type");
            }
            normalizePoliciesLocked();
            updateNetworkEnabledLocked();
            updateNetworkRulesLocked();
            updateNotificationsLocked();
            writePolicyLocked();
        }
    }

    public void onTetheringChanged(String iface, boolean tethering) {
        Log.d(TAG, "onTetherStateChanged(" + iface + ", " + tethering + ")");
        synchronized (this.mRulesLock) {
            if (this.mRestrictBackground && tethering) {
                Log.d(TAG, "Tethering on (" + iface + "); disable Data Saver");
                setRestrictBackground(false);
            }
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        Slog.d(TAG, "setRestrictBackground(" + restrictBackground + ")");
        try {
            maybeRefreshTrustedTime();
            synchronized (this.mRulesLock) {
                if (restrictBackground == this.mRestrictBackground) {
                    Slog.w(TAG, "setRestrictBackground: already " + restrictBackground);
                    return;
                }
                setRestrictBackgroundLocked(restrictBackground);
                Binder.restoreCallingIdentity(token);
                this.mHandler.obtainMessage(6, restrictBackground ? 1 : 0, 0).sendToTarget();
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void setRestrictBackgroundLocked(boolean restrictBackground) {
        Slog.d(TAG, "setRestrictBackgroundLocked(): " + restrictBackground);
        boolean oldRestrictBackground = this.mRestrictBackground;
        this.mRestrictBackground = restrictBackground;
        updateRulesForRestrictBackgroundLocked();
        try {
            if (!this.mNetworkManager.setDataSaverModeEnabled(this.mRestrictBackground)) {
                Slog.e(TAG, "Could not change Data Saver Mode on NMS to " + this.mRestrictBackground);
                this.mRestrictBackground = oldRestrictBackground;
                return;
            }
        } catch (RemoteException e) {
        }
        updateNotificationsLocked();
        writePolicyLocked();
    }

    public void addRestrictBackgroundWhitelistedUid(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            boolean oldStatus = this.mRestrictBackgroundWhitelistUids.get(uid);
            if (oldStatus) {
                Slog.d(TAG, "uid " + uid + " is already whitelisted");
                return;
            }
            boolean needFirewallRules = isUidValidForWhitelistRules(uid);
            Slog.i(TAG, "adding uid " + uid + " to restrict background whitelist");
            this.mRestrictBackgroundWhitelistUids.append(uid, true);
            if (this.mDefaultRestrictBackgroundWhitelistUids.get(uid) && this.mRestrictBackgroundWhitelistRevokedUids.get(uid)) {
                Slog.d(TAG, "Removing uid " + uid + " from revoked restrict background whitelist");
                this.mRestrictBackgroundWhitelistRevokedUids.delete(uid);
            }
            if (needFirewallRules) {
                updateRulesForDataUsageRestrictionsLocked(uid);
            }
            writePolicyLocked();
            int changed = (this.mRestrictBackground && !oldStatus && needFirewallRules) ? 1 : 0;
            this.mHandler.obtainMessage(9, uid, changed, Boolean.TRUE).sendToTarget();
        }
    }

    public void removeRestrictBackgroundWhitelistedUid(int uid) {
        boolean changed;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            changed = removeRestrictBackgroundWhitelistedUidLocked(uid, false, true);
        }
        this.mHandler.obtainMessage(9, uid, changed ? 1 : 0, Boolean.FALSE).sendToTarget();
    }

    private boolean removeRestrictBackgroundWhitelistedUidLocked(int uid, boolean uidDeleted, boolean updateNow) {
        boolean oldStatus = this.mRestrictBackgroundWhitelistUids.get(uid);
        if (!oldStatus && !uidDeleted) {
            Slog.d(TAG, "uid " + uid + " was not whitelisted before");
            return false;
        }
        boolean zIsUidValidForWhitelistRules = !uidDeleted ? isUidValidForWhitelistRules(uid) : true;
        if (oldStatus) {
            Slog.i(TAG, "removing uid " + uid + " from restrict background whitelist");
            this.mRestrictBackgroundWhitelistUids.delete(uid);
        }
        if (!uidDeleted && this.mDefaultRestrictBackgroundWhitelistUids.get(uid) && !this.mRestrictBackgroundWhitelistRevokedUids.get(uid)) {
            Slog.d(TAG, "Adding uid " + uid + " to revoked restrict background whitelist");
            this.mRestrictBackgroundWhitelistRevokedUids.append(uid, true);
        } else {
            Slog.d(TAG, "Skip revoking " + uid + " from restrict background whitelist");
        }
        if (zIsUidValidForWhitelistRules) {
            updateRulesForDataUsageRestrictionsLocked(uid, uidDeleted);
        }
        if (updateNow) {
            writePolicyLocked();
        }
        if (this.mRestrictBackground) {
            return zIsUidValidForWhitelistRules;
        }
        return false;
    }

    public int[] getRestrictBackgroundWhitelistedUids() {
        int[] whitelist;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            int size = this.mRestrictBackgroundWhitelistUids.size();
            whitelist = new int[size];
            for (int i = 0; i < size; i++) {
                whitelist[i] = this.mRestrictBackgroundWhitelistUids.keyAt(i);
            }
            Slog.v(TAG, "getRestrictBackgroundWhitelistedUids(): " + this.mRestrictBackgroundWhitelistUids);
        }
        return whitelist;
    }

    public int getRestrictBackgroundByCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        int uid = Binder.getCallingUid();
        synchronized (this.mRulesLock) {
            long token = Binder.clearCallingIdentity();
            try {
                int policy = getUidPolicy(uid);
                if (policy == 1) {
                    return 3;
                }
                if (this.mRestrictBackground) {
                    return this.mRestrictBackgroundWhitelistUids.get(uid) ? 2 : 3;
                }
                return 1;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public boolean getRestrictBackground() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            z = this.mRestrictBackground;
        }
        return z;
    }

    public void setDeviceIdleMode(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            if (this.mDeviceIdleMode != enabled) {
                this.mDeviceIdleMode = enabled;
                if (this.mSystemReady) {
                    updateRulesForGlobalChangeLocked(false);
                }
                if (enabled) {
                    EventLogTags.writeDeviceIdleOnPhase("net");
                } else {
                    EventLogTags.writeDeviceIdleOffPhase("net");
                }
            }
        }
    }

    private NetworkPolicy findPolicyForNetworkLocked(NetworkIdentity ident) {
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
            if (policy.template.matches(ident)) {
                return policy;
            }
        }
        return null;
    }

    public NetworkQuotaInfo getNetworkQuotaInfo(NetworkState state) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            return getNetworkQuotaInfoUnchecked(state);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private NetworkQuotaInfo getNetworkQuotaInfoUnchecked(NetworkState state) {
        NetworkPolicy policy;
        NetworkIdentity ident = NetworkIdentity.buildNetworkIdentity(this.mContext, state);
        synchronized (this.mRulesLock) {
            policy = findPolicyForNetworkLocked(ident);
        }
        if (policy == null || !policy.hasCycle()) {
            return null;
        }
        long currentTime = currentTimeMillis();
        long start = NetworkPolicyManager.computeLastCycleBoundary(currentTime, policy);
        long totalBytes = getTotalBytes(policy.template, start, currentTime);
        long softLimitBytes = policy.warningBytes != -1 ? policy.warningBytes : -1L;
        long hardLimitBytes = policy.limitBytes != -1 ? policy.limitBytes : -1L;
        return new NetworkQuotaInfo(totalBytes, softLimitBytes, hardLimitBytes);
    }

    public boolean isNetworkMetered(NetworkState state) {
        NetworkPolicy policy;
        if (state.networkInfo == null) {
            return false;
        }
        NetworkIdentity ident = NetworkIdentity.buildNetworkIdentity(this.mContext, state);
        if (ident.getRoaming()) {
            return true;
        }
        synchronized (this.mRulesLock) {
            policy = findPolicyForNetworkLocked(ident);
        }
        if (policy != null) {
            return policy.metered;
        }
        int type = state.networkInfo.getType();
        return ConnectivityManager.isNetworkTypeMobile(type) || type == 6;
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        IndentingPrintWriter fout = new IndentingPrintWriter(writer, "  ");
        ArraySet<String> argSet = new ArraySet<>(args.length);
        for (String arg : args) {
            argSet.add(arg);
        }
        synchronized (this.mRulesLock) {
            if (argSet.contains("--unsnooze")) {
                for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
                    this.mNetworkPolicy.valueAt(i).clearSnooze();
                }
                normalizePoliciesLocked();
                updateNetworkEnabledLocked();
                updateNetworkRulesLocked();
                updateNotificationsLocked();
                writePolicyLocked();
                fout.println("Cleared snooze timestamps");
                return;
            }
            fout.print("System ready: ");
            fout.println(this.mSystemReady);
            fout.print("Restrict background: ");
            fout.println(this.mRestrictBackground);
            fout.print("Restrict power: ");
            fout.println(this.mRestrictPower);
            fout.print("Device idle: ");
            fout.println(this.mDeviceIdleMode);
            fout.println("Network policies:");
            fout.increaseIndent();
            for (int i2 = 0; i2 < this.mNetworkPolicy.size(); i2++) {
                fout.println(this.mNetworkPolicy.valueAt(i2).toString());
            }
            fout.decreaseIndent();
            fout.print("Metered ifaces: ");
            fout.println(String.valueOf(this.mMeteredIfaces));
            fout.println("Policy for UIDs:");
            fout.increaseIndent();
            int size = this.mUidPolicy.size();
            for (int i3 = 0; i3 < size; i3++) {
                int uid = this.mUidPolicy.keyAt(i3);
                int policy = this.mUidPolicy.valueAt(i3);
                fout.print("UID=");
                fout.print(uid);
                fout.print(" policy=");
                fout.print(DebugUtils.flagsToString(NetworkPolicyManager.class, "POLICY_", policy));
                fout.println();
            }
            fout.decreaseIndent();
            int size2 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
            if (size2 > 0) {
                fout.println("Power save whitelist (except idle) app ids:");
                fout.increaseIndent();
                for (int i4 = 0; i4 < size2; i4++) {
                    fout.print("UID=");
                    fout.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(i4));
                    fout.print(": ");
                    fout.print(this.mPowerSaveWhitelistExceptIdleAppIds.valueAt(i4));
                    fout.println();
                }
                fout.decreaseIndent();
            }
            int size3 = this.mPowerSaveWhitelistAppIds.size();
            if (size3 > 0) {
                fout.println("Power save whitelist app ids:");
                fout.increaseIndent();
                for (int i5 = 0; i5 < size3; i5++) {
                    fout.print("UID=");
                    fout.print(this.mPowerSaveWhitelistAppIds.keyAt(i5));
                    fout.print(": ");
                    fout.print(this.mPowerSaveWhitelistAppIds.valueAt(i5));
                    fout.println();
                }
                fout.decreaseIndent();
            }
            int size4 = this.mRestrictBackgroundWhitelistUids.size();
            if (size4 > 0) {
                fout.println("Restrict background whitelist uids:");
                fout.increaseIndent();
                for (int i6 = 0; i6 < size4; i6++) {
                    fout.print("UID=");
                    fout.print(this.mRestrictBackgroundWhitelistUids.keyAt(i6));
                    fout.println();
                }
                fout.decreaseIndent();
            }
            int size5 = this.mDefaultRestrictBackgroundWhitelistUids.size();
            if (size5 > 0) {
                fout.println("Default restrict background whitelist uids:");
                fout.increaseIndent();
                for (int i7 = 0; i7 < size5; i7++) {
                    fout.print("UID=");
                    fout.print(this.mDefaultRestrictBackgroundWhitelistUids.keyAt(i7));
                    fout.println();
                }
                fout.decreaseIndent();
            }
            int size6 = this.mRestrictBackgroundWhitelistRevokedUids.size();
            if (size6 > 0) {
                fout.println("Default restrict background whitelist uids revoked by users:");
                fout.increaseIndent();
                for (int i8 = 0; i8 < size6; i8++) {
                    fout.print("UID=");
                    fout.print(this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i8));
                    fout.println();
                }
                fout.decreaseIndent();
            }
            SparseBooleanArray knownUids = new SparseBooleanArray();
            collectKeys(this.mUidState, knownUids);
            collectKeys(this.mUidRules, knownUids);
            fout.println("Status for all known UIDs:");
            fout.increaseIndent();
            int size7 = knownUids.size();
            for (int i9 = 0; i9 < size7; i9++) {
                int uid2 = knownUids.keyAt(i9);
                fout.print("UID=");
                fout.print(uid2);
                int state = this.mUidState.get(uid2, 16);
                fout.print(" state=");
                fout.print(state);
                if (state <= 2) {
                    fout.print(" (fg)");
                } else {
                    fout.print(state <= 4 ? " (fg svc)" : " (bg)");
                }
                int uidRules = this.mUidRules.get(uid2, 0);
                fout.print(" rules=");
                fout.print(NetworkPolicyManager.uidRulesToString(uidRules));
                fout.println();
            }
            fout.decreaseIndent();
            fout.println("Status for just UIDs with rules:");
            fout.increaseIndent();
            int size8 = this.mUidRules.size();
            for (int i10 = 0; i10 < size8; i10++) {
                int uid3 = this.mUidRules.keyAt(i10);
                fout.print("UID=");
                fout.print(uid3);
                int uidRules2 = this.mUidRules.get(uid3, 0);
                fout.print(" rules=");
                fout.print(NetworkPolicyManager.uidRulesToString(uidRules2));
                fout.println();
            }
            fout.decreaseIndent();
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) throws RemoteException {
        new NetworkPolicyManagerShellCommand(this.mContext, this).exec(this, in, out, err, args, resultReceiver);
    }

    public boolean isUidForeground(int uid) {
        boolean zIsUidForegroundLocked;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            zIsUidForegroundLocked = isUidForegroundLocked(uid);
        }
        return zIsUidForegroundLocked;
    }

    private boolean isUidForegroundLocked(int uid) {
        return isUidStateForegroundLocked(this.mUidState.get(uid, 16));
    }

    private boolean isUidForegroundOnRestrictBackgroundLocked(int uid) {
        int procState = this.mUidState.get(uid, 16);
        return isProcStateAllowedWhileOnRestrictBackgroundLocked(procState);
    }

    private boolean isUidForegroundOnRestrictPowerLocked(int uid) {
        int procState = this.mUidState.get(uid, 16);
        return isProcStateAllowedWhileIdleOrPowerSaveMode(procState);
    }

    private boolean isUidStateForegroundLocked(int state) {
        return this.mScreenOn && state <= 2;
    }

    private void updateUidStateLocked(int uid, int uidState) {
        int oldUidState = this.mUidState.get(uid, 16);
        if (oldUidState == uidState) {
            return;
        }
        this.mUidState.put(uid, uidState);
        updateRestrictBackgroundRulesOnUidStatusChangedLocked(uid, oldUidState, uidState);
        if (isProcStateAllowedWhileIdleOrPowerSaveMode(oldUidState) == isProcStateAllowedWhileIdleOrPowerSaveMode(uidState)) {
            return;
        }
        if (isUidIdle(uid)) {
            updateRuleForAppIdleLocked(uid);
        }
        if (this.mDeviceIdleMode) {
            updateRuleForDeviceIdleLocked(uid);
        }
        if (this.mRestrictPower) {
            updateRuleForRestrictPowerLocked(uid);
        }
        updateRulesForPowerRestrictionsLocked(uid);
    }

    private void removeUidStateLocked(int uid) {
        int index = this.mUidState.indexOfKey(uid);
        if (index < 0) {
            return;
        }
        int oldUidState = this.mUidState.valueAt(index);
        this.mUidState.removeAt(index);
        if (oldUidState == 16) {
            return;
        }
        updateRestrictBackgroundRulesOnUidStatusChangedLocked(uid, oldUidState, 16);
        if (this.mDeviceIdleMode) {
            updateRuleForDeviceIdleLocked(uid);
        }
        if (this.mRestrictPower) {
            updateRuleForRestrictPowerLocked(uid);
        }
        updateRulesForPowerRestrictionsLocked(uid);
    }

    private void updateNetworkStats(int uid, boolean uidForeground) {
        try {
            this.mNetworkStats.setUidForeground(uid, uidForeground);
        } catch (RemoteException e) {
        }
    }

    private void updateRestrictBackgroundRulesOnUidStatusChangedLocked(int uid, int oldUidState, int newUidState) {
        boolean oldForeground = isProcStateAllowedWhileOnRestrictBackgroundLocked(oldUidState);
        boolean newForeground = isProcStateAllowedWhileOnRestrictBackgroundLocked(newUidState);
        if (oldForeground == newForeground) {
            return;
        }
        updateRulesForDataUsageRestrictionsLocked(uid);
    }

    private void updateScreenOn() {
        synchronized (this.mRulesLock) {
            try {
                this.mScreenOn = this.mPowerManager.isInteractive();
            } catch (RemoteException e) {
            }
            updateRulesForScreenLocked();
        }
    }

    private void updateRulesForScreenLocked() {
        int size = this.mUidState.size();
        for (int i = 0; i < size; i++) {
            if (this.mUidState.valueAt(i) <= 4) {
                int uid = this.mUidState.keyAt(i);
                updateRestrictionRulesForUidLocked(uid);
            }
        }
    }

    static boolean isProcStateAllowedWhileIdleOrPowerSaveMode(int procState) {
        return procState <= 4;
    }

    static boolean isProcStateAllowedWhileOnRestrictBackgroundLocked(int procState) {
        return procState <= 4;
    }

    void updateRulesForRestrictPowerLocked() {
        updateRulesForWhitelistedPowerSaveLocked(this.mRestrictPower, 3, this.mUidFirewallPowerSaveRules);
    }

    void updateRuleForRestrictPowerLocked(int uid) {
        updateRulesForWhitelistedPowerSaveLocked(uid, this.mRestrictPower, 3);
    }

    void updateRulesForDeviceIdleLocked() {
        updateRulesForWhitelistedPowerSaveLocked(this.mDeviceIdleMode, 1, this.mUidFirewallDozableRules);
    }

    void updateRuleForDeviceIdleLocked(int uid) {
        updateRulesForWhitelistedPowerSaveLocked(uid, this.mDeviceIdleMode, 1);
    }

    private void updateRulesForWhitelistedPowerSaveLocked(boolean enabled, int chain, SparseIntArray rules) {
        if (enabled) {
            rules.clear();
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int ui = users.size() - 1; ui >= 0; ui--) {
                UserInfo user = users.get(ui);
                for (int i = this.mPowerSaveTempWhitelistAppIds.size() - 1; i >= 0; i--) {
                    if (this.mPowerSaveTempWhitelistAppIds.valueAt(i)) {
                        int appId = this.mPowerSaveTempWhitelistAppIds.keyAt(i);
                        int uid = UserHandle.getUid(user.id, appId);
                        rules.put(uid, 1);
                    }
                }
                for (int i2 = this.mPowerSaveWhitelistAppIds.size() - 1; i2 >= 0; i2--) {
                    int appId2 = this.mPowerSaveWhitelistAppIds.keyAt(i2);
                    int uid2 = UserHandle.getUid(user.id, appId2);
                    rules.put(uid2, 1);
                }
            }
            for (int i3 = this.mUidState.size() - 1; i3 >= 0; i3--) {
                if (isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.valueAt(i3))) {
                    rules.put(this.mUidState.keyAt(i3), 1);
                }
            }
            setUidFirewallRules(chain, rules);
        }
        enableFirewallChainLocked(chain, enabled);
    }

    private void updateRulesForNonMeteredNetworksLocked() {
    }

    private boolean isWhitelistedBatterySaverLocked(int uid) {
        int appId = UserHandle.getAppId(uid);
        if (this.mPowerSaveTempWhitelistAppIds.get(appId)) {
            return true;
        }
        return this.mPowerSaveWhitelistAppIds.get(appId);
    }

    private void updateRulesForWhitelistedPowerSaveLocked(int uid, boolean enabled, int chain) {
        if (!enabled) {
            return;
        }
        if (isWhitelistedBatterySaverLocked(uid) || isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.get(uid))) {
            setUidFirewallRule(chain, uid, 1);
        } else {
            setUidFirewallRule(chain, uid, 0);
        }
    }

    void updateRulesForAppIdleLocked() {
        SparseIntArray uidRules = this.mUidFirewallStandbyRules;
        uidRules.clear();
        List<UserInfo> users = this.mUserManager.getUsers();
        for (int ui = users.size() - 1; ui >= 0; ui--) {
            UserInfo user = users.get(ui);
            int[] idleUids = this.mUsageStats.getIdleUidsForUser(user.id);
            for (int uid : idleUids) {
                if (!this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(uid), false) && hasInternetPermissions(uid)) {
                    uidRules.put(uid, 2);
                }
            }
        }
        setUidFirewallRules(2, uidRules);
    }

    void updateRuleForAppIdleLocked(int uid) {
        if (isUidValidForBlacklistRules(uid)) {
            int appId = UserHandle.getAppId(uid);
            if (!this.mPowerSaveTempWhitelistAppIds.get(appId) && isUidIdle(uid) && !isUidForegroundOnRestrictPowerLocked(uid)) {
                setUidFirewallRule(2, uid, 2);
            } else {
                setUidFirewallRule(2, uid, 0);
            }
        }
    }

    void updateRulesForAppIdleParoleLocked() {
        boolean enableChain = !this.mUsageStats.isAppIdleParoleOn();
        enableFirewallChainLocked(2, enableChain);
    }

    private void updateRulesForGlobalChangeLocked(boolean restrictedNetworksChanged) {
        long start = System.currentTimeMillis();
        updateRulesForDeviceIdleLocked();
        updateRulesForAppIdleLocked();
        updateRulesForRestrictPowerLocked();
        updateRulesForRestrictBackgroundLocked();
        setRestrictBackgroundLocked(this.mRestrictBackground);
        if (restrictedNetworksChanged) {
            normalizePoliciesLocked();
            updateNetworkRulesLocked();
        }
        long delta = System.currentTimeMillis() - start;
        Slog.d(TAG, "updateRulesForGlobalChangeLocked(" + restrictedNetworksChanged + ") took " + delta + "ms");
    }

    private void updateRulesForRestrictBackgroundLocked() {
        PackageManager pm = this.mContext.getPackageManager();
        List<UserInfo> users = this.mUserManager.getUsers();
        List<ApplicationInfo> apps = pm.getInstalledApplications(795136);
        int usersSize = users.size();
        int appsSize = apps.size();
        for (int i = 0; i < usersSize; i++) {
            UserInfo user = users.get(i);
            for (int j = 0; j < appsSize; j++) {
                ApplicationInfo app = apps.get(j);
                int uid = UserHandle.getUid(user.id, app.uid);
                updateRulesForDataUsageRestrictionsLocked(uid);
                updateRulesForPowerRestrictionsLocked(uid);
            }
        }
    }

    private void updateRulesForTempWhitelistChangeLocked() {
        List<UserInfo> users = this.mUserManager.getUsers();
        for (int i = 0; i < users.size(); i++) {
            UserInfo user = users.get(i);
            for (int j = this.mPowerSaveTempWhitelistAppIds.size() - 1; j >= 0; j--) {
                int appId = this.mPowerSaveTempWhitelistAppIds.keyAt(j);
                int uid = UserHandle.getUid(user.id, appId);
                updateRuleForAppIdleLocked(uid);
                updateRuleForDeviceIdleLocked(uid);
                updateRuleForRestrictPowerLocked(uid);
                updateRulesForPowerRestrictionsLocked(uid);
            }
        }
    }

    private boolean isUidValidForBlacklistRules(int uid) {
        if (uid != 1013 && uid != 1019) {
            if (UserHandle.isApp(uid) && hasInternetPermissions(uid)) {
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean isUidValidForWhitelistRules(int uid) {
        if (UserHandle.isApp(uid)) {
            return hasInternetPermissions(uid);
        }
        return false;
    }

    private boolean isUidIdle(int uid) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        int userId = UserHandle.getUserId(uid);
        if (!ArrayUtils.isEmpty(packages)) {
            for (String packageName : packages) {
                if (!this.mUsageStats.isAppIdle(packageName, uid, userId)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean hasInternetPermissions(int uid) {
        try {
            return this.mIPm.checkUidPermission("android.permission.INTERNET", uid) == 0;
        } catch (RemoteException e) {
            return true;
        }
    }

    private void updateRestrictionRulesForUidLocked(int uid) {
        updateRuleForDeviceIdleLocked(uid);
        updateRuleForAppIdleLocked(uid);
        updateRuleForRestrictPowerLocked(uid);
        updateRulesForPowerRestrictionsLocked(uid);
        updateRulesForDataUsageRestrictionsLocked(uid);
    }

    private void updateRulesForDataUsageRestrictionsLocked(int uid) {
        updateRulesForDataUsageRestrictionsLocked(uid, false);
    }

    private void updateRulesForDataUsageRestrictionsLocked(int uid, boolean uidDeleted) {
        if (!uidDeleted && !isUidValidForWhitelistRules(uid)) {
            if (ENG_DBG) {
                Slog.d(TAG, "no need to update restrict data rules for uid " + uid);
                return;
            }
            return;
        }
        int uidPolicy = this.mUidPolicy.get(uid, 0);
        int oldUidRules = this.mUidRules.get(uid, 0);
        boolean isForeground = isUidForegroundOnRestrictBackgroundLocked(uid);
        boolean isBlacklisted = (uidPolicy & 1) != 0;
        boolean isWhitelisted = this.mRestrictBackgroundWhitelistUids.get(uid);
        int oldRule = oldUidRules & 15;
        int newRule = 0;
        if (isForeground) {
            if (isBlacklisted || (this.mRestrictBackground && !isWhitelisted)) {
                newRule = 2;
            } else if (isWhitelisted) {
                newRule = 1;
            }
        } else if (isBlacklisted) {
            newRule = 4;
        } else if (this.mRestrictBackground && isWhitelisted) {
            newRule = 1;
        }
        int newUidRules = newRule | (oldUidRules & 240);
        Log.v(TAG, "updateRuleForRestrictBackgroundLocked(" + uid + "): isForeground=" + isForeground + ", isBlacklisted=" + isBlacklisted + ", isWhitelisted=" + isWhitelisted + ", oldRule=" + NetworkPolicyManager.uidRulesToString(oldRule) + ", newRule=" + NetworkPolicyManager.uidRulesToString(newRule) + ", newUidRules=" + NetworkPolicyManager.uidRulesToString(newUidRules) + ", oldUidRules=" + NetworkPolicyManager.uidRulesToString(oldUidRules));
        if (newUidRules == 0) {
            this.mUidRules.delete(uid);
        } else {
            this.mUidRules.put(uid, newUidRules);
        }
        if (newRule != oldRule) {
            if ((newRule & 2) != 0) {
                setMeteredNetworkWhitelist(uid, true);
                if (isBlacklisted) {
                    setMeteredNetworkBlacklist(uid, false);
                }
            } else if ((oldRule & 2) != 0) {
                if (!isWhitelisted) {
                    setMeteredNetworkWhitelist(uid, false);
                }
                if (isBlacklisted) {
                    setMeteredNetworkBlacklist(uid, true);
                }
            } else if ((newRule & 4) != 0 || (oldRule & 4) != 0) {
                setMeteredNetworkBlacklist(uid, isBlacklisted);
                if ((oldRule & 4) != 0 && isWhitelisted) {
                    setMeteredNetworkWhitelist(uid, isWhitelisted);
                }
            } else if ((newRule & 1) == 0 && (oldRule & 1) == 0) {
                Log.wtf(TAG, "Unexpected change of metered UID state for " + uid + ": foreground=" + isForeground + ", whitelisted=" + isWhitelisted + ", blacklisted=" + isBlacklisted + ", newRule=" + NetworkPolicyManager.uidRulesToString(newUidRules) + ", oldRule=" + NetworkPolicyManager.uidRulesToString(oldUidRules));
            } else {
                setMeteredNetworkWhitelist(uid, isWhitelisted);
            }
            this.mHandler.obtainMessage(1, uid, newUidRules).sendToTarget();
        }
    }

    private void updateRulesForPowerRestrictionsLocked(int uid) {
        if (!isUidValidForBlacklistRules(uid)) {
            if (ENG_DBG) {
                Slog.d(TAG, "no need to update restrict power rules for uid " + uid);
                return;
            }
            return;
        }
        boolean isIdle = isUidIdle(uid);
        boolean restrictMode = (isIdle || this.mRestrictPower) ? true : this.mDeviceIdleMode;
        this.mUidPolicy.get(uid, 0);
        int oldUidRules = this.mUidRules.get(uid, 0);
        boolean isForeground = isUidForegroundOnRestrictPowerLocked(uid);
        boolean isWhitelisted = isWhitelistedBatterySaverLocked(uid);
        int oldRule = oldUidRules & 240;
        int newRule = 0;
        if (isForeground) {
            if (restrictMode) {
                newRule = 32;
            }
        } else if (restrictMode) {
            newRule = isWhitelisted ? 32 : 64;
        }
        int newUidRules = (oldUidRules & 15) | newRule;
        Log.v(TAG, "updateRulesForNonMeteredNetworksLocked(" + uid + "), isIdle: " + isIdle + ", mRestrictPower: " + this.mRestrictPower + ", mDeviceIdleMode: " + this.mDeviceIdleMode + ", isForeground=" + isForeground + ", isWhitelisted=" + isWhitelisted + ", oldRule=" + NetworkPolicyManager.uidRulesToString(oldRule) + ", newRule=" + NetworkPolicyManager.uidRulesToString(newRule) + ", newUidRules=" + NetworkPolicyManager.uidRulesToString(newUidRules) + ", oldUidRules=" + NetworkPolicyManager.uidRulesToString(oldUidRules));
        if (newUidRules == 0) {
            this.mUidRules.delete(uid);
        } else {
            this.mUidRules.put(uid, newUidRules);
        }
        if (newRule != oldRule) {
            if (newRule == 0 || (newRule & 32) != 0) {
                Log.v(TAG, "Allowing non-metered access for UID " + uid);
            } else if ((newRule & 64) != 0) {
                Log.v(TAG, "Rejecting non-metered access for UID " + uid);
            } else {
                Log.wtf(TAG, "Unexpected change of non-metered UID state for " + uid + ": foreground=" + isForeground + ", whitelisted=" + isWhitelisted + ", newRule=" + NetworkPolicyManager.uidRulesToString(newUidRules) + ", oldRule=" + NetworkPolicyManager.uidRulesToString(oldUidRules));
            }
            this.mHandler.obtainMessage(1, uid, newUidRules).sendToTarget();
        }
    }

    private class AppIdleStateChangeListener extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        AppIdleStateChangeListener(NetworkPolicyManagerService this$0, AppIdleStateChangeListener appIdleStateChangeListener) {
            this();
        }

        private AppIdleStateChangeListener() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle) {
            try {
                int uid = NetworkPolicyManagerService.this.mContext.getPackageManager().getPackageUidAsUser(packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, userId);
                if (NetworkPolicyManagerService.ENG_DBG) {
                    Log.v(NetworkPolicyManagerService.TAG, "onAppIdleStateChanged(): uid=" + uid + ", idle=" + idle);
                }
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    NetworkPolicyManagerService.this.updateRuleForAppIdleLocked(uid);
                    NetworkPolicyManagerService.this.updateRulesForPowerRestrictionsLocked(uid);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                NetworkPolicyManagerService.this.updateRulesForAppIdleParoleLocked();
            }
        }
    }

    private void dispatchUidRulesChanged(INetworkPolicyListener listener, int uid, int uidRules) {
        if (listener == null) {
            return;
        }
        try {
            listener.onUidRulesChanged(uid, uidRules);
        } catch (RemoteException e) {
        }
    }

    private void dispatchMeteredIfacesChanged(INetworkPolicyListener listener, String[] meteredIfaces) {
        if (listener == null) {
            return;
        }
        try {
            listener.onMeteredIfacesChanged(meteredIfaces);
        } catch (RemoteException e) {
        }
    }

    private void dispatchRestrictBackgroundChanged(INetworkPolicyListener listener, boolean restrictBackground) {
        if (listener == null) {
            return;
        }
        try {
            listener.onRestrictBackgroundChanged(restrictBackground);
        } catch (RemoteException e) {
        }
    }

    private void dispatchRestrictBackgroundWhitelistChanged(INetworkPolicyListener listener, int uid, boolean whitelisted) {
        if (listener == null) {
            return;
        }
        try {
            listener.onRestrictBackgroundWhitelistChanged(uid, whitelisted);
        } catch (RemoteException e) {
        }
    }

    private void dispatchRestrictBackgroundBlacklistChanged(INetworkPolicyListener listener, int uid, boolean blacklisted) {
        if (listener == null) {
            return;
        }
        try {
            listener.onRestrictBackgroundBlacklistChanged(uid, blacklisted);
        } catch (RemoteException e) {
        }
    }

    private void setInterfaceQuota(String iface, long quotaBytes) {
        try {
            this.mNetworkManager.setInterfaceQuota(iface, quotaBytes);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.e(TAG, "problem setting interface quota:" + e2);
        }
    }

    private void removeInterfaceQuota(String iface) {
        try {
            this.mNetworkManager.removeInterfaceQuota(iface);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.e(TAG, "problem removing interface quota", e2);
        }
    }

    private void setMeteredNetworkBlacklist(int uid, boolean enable) {
        Slog.v(TAG, "setMeteredNetworkBlacklist " + uid + ": " + enable);
        try {
            this.mNetworkManager.setUidMeteredNetworkBlacklist(uid, enable);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.e(TAG, "problem setting blacklist (" + enable + ") rules for " + uid, e2);
        }
    }

    private void setMeteredNetworkWhitelist(int uid, boolean enable) {
        Slog.v(TAG, "setMeteredNetworkWhitelist " + uid + ": " + enable);
        try {
            this.mNetworkManager.setUidMeteredNetworkWhitelist(uid, enable);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting whitelist (" + enable + ") rules for " + uid, e2);
        }
    }

    private void setUidFirewallRules(int chain, SparseIntArray uidRules) {
        try {
            int size = uidRules.size();
            int[] uids = new int[size];
            int[] rules = new int[size];
            for (int index = size - 1; index >= 0; index--) {
                uids[index] = uidRules.keyAt(index);
                rules[index] = uidRules.valueAt(index);
            }
            this.mNetworkManager.setFirewallUidRules(chain, uids, rules);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting firewall uid rules", e2);
        }
    }

    private void setUidFirewallRule(int chain, int uid, int rule) {
        if (chain == 1) {
            this.mUidFirewallDozableRules.put(uid, rule);
        } else if (chain == 2) {
            this.mUidFirewallStandbyRules.put(uid, rule);
        } else if (chain == 3) {
            this.mUidFirewallPowerSaveRules.put(uid, rule);
        }
        try {
            this.mNetworkManager.setFirewallUidRule(chain, uid, rule);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting firewall uid rules", e2);
        }
    }

    private void enableFirewallChainLocked(int chain, boolean enable) {
        if (this.mFirewallChainStates.indexOfKey(chain) >= 0 && this.mFirewallChainStates.get(chain) == enable) {
            return;
        }
        this.mFirewallChainStates.put(chain, enable);
        try {
            this.mNetworkManager.setFirewallChainEnabled(chain, enable);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem enable firewall chain", e2);
        }
    }

    private long getTotalBytes(NetworkTemplate template, long start, long end) {
        try {
            return this.mNetworkStats.getNetworkTotalBytes(template, start, end);
        } catch (RemoteException e) {
            return 0L;
        } catch (RuntimeException e2) {
            Slog.w(TAG, "problem reading network stats: " + e2);
            return 0L;
        }
    }

    private boolean isBandwidthControlEnabled() {
        long token = Binder.clearCallingIdentity();
        try {
            return this.mNetworkManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void maybeRefreshTrustedTime() {
        if (this.mTime.getCacheAge() <= 86400000 || !NetworkStatsService.USE_TRUESTED_TIME) {
            return;
        }
        this.mTime.forceRefresh();
    }

    private long currentTimeMillis() {
        return (this.mTime.hasCache() && NetworkStatsService.USE_TRUESTED_TIME) ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
    }

    private static Intent buildAllowBackgroundDataIntent() {
        return new Intent(ACTION_ALLOW_BACKGROUND);
    }

    private static Intent buildSnoozeWarningIntent(NetworkTemplate template) {
        Intent intent = new Intent(ACTION_SNOOZE_WARNING);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) template);
        return intent;
    }

    private static Intent buildNetworkOverLimitIntent(NetworkTemplate template) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.net.NetworkOverLimitActivity"));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) template);
        return intent;
    }

    private static Intent buildViewDataUsageIntent(NetworkTemplate template) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) template);
        return intent;
    }

    public void addIdleHandler(MessageQueue.IdleHandler handler) {
        this.mHandler.getLooper().getQueue().addIdleHandler(handler);
    }

    private static void collectKeys(SparseIntArray source, SparseBooleanArray target) {
        int size = source.size();
        for (int i = 0; i < size; i++) {
            target.put(source.keyAt(i), true);
        }
    }

    public void factoryReset(String subscriber) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        NetworkPolicy[] policies = getNetworkPolicies(this.mContext.getOpPackageName());
        NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriber);
        for (NetworkPolicy policy : policies) {
            if (policy.template.equals(template)) {
                policy.limitBytes = -1L;
                policy.inferred = false;
                policy.clearSnooze();
            }
        }
        setNetworkPolicies(policies);
        setRestrictBackground(false);
        if (this.mUserManager.hasUserRestriction("no_control_apps")) {
            return;
        }
        for (int uid : getUidsWithPolicy(1)) {
            setUidPolicy(uid, 0);
        }
    }

    private class NetworkPolicyManagerInternalImpl extends NetworkPolicyManagerInternal {
        NetworkPolicyManagerInternalImpl(NetworkPolicyManagerService this$0, NetworkPolicyManagerInternalImpl networkPolicyManagerInternalImpl) {
            this();
        }

        private NetworkPolicyManagerInternalImpl() {
        }

        @Override
        public void resetUserState(int userId) {
            synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                boolean changed = NetworkPolicyManagerService.this.removeUserStateLocked(userId, false);
                if (NetworkPolicyManagerService.this.addDefaultRestrictBackgroundWhitelistUidsLocked(userId)) {
                    changed = true;
                }
                if (changed) {
                    NetworkPolicyManagerService.this.writePolicyLocked();
                }
            }
        }
    }

    private void sendPolicyCreatedBroadcast() {
        Slog.v(TAG, "sendPolicyCreatedBroadcast ACTION_POLICY_CREATED");
        Intent intent = new Intent("com.mediatek.server.action.ACTION_POLICY_CREATED");
        intent.addFlags(536870912);
        intent.addFlags(67108864);
        this.mContext.sendBroadcast(intent);
    }
}
