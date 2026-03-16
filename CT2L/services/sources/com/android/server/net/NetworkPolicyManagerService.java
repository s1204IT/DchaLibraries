package com.android.server.net;

import android.R;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.IProcessObserver;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
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
import android.net.NetworkIdentity;
import android.net.NetworkInfo;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkQuotaInfo;
import android.net.NetworkState;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.IPowerManager;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.os.PowerManagerInternal;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
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
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TrustedTime;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.NetworkManagementService;
import com.android.server.SystemConfig;
import com.android.server.job.controllers.JobStatus;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
    private static final boolean LOGD = false;
    private static final boolean LOGV = false;
    private static final int MSG_ADVISE_PERSIST_THRESHOLD = 7;
    private static final int MSG_LIMIT_REACHED = 5;
    private static final int MSG_METERED_IFACES_CHANGED = 2;
    private static final int MSG_RESTRICT_BACKGROUND_CHANGED = 6;
    private static final int MSG_RULES_CHANGED = 1;
    private static final int MSG_SCREEN_ON_CHANGED = 8;
    private static final String TAG = "NetworkPolicy";
    private static final String TAG_ALLOW_BACKGROUND = "NetworkPolicy:allowBackground";
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_UID_POLICY = "uid-policy";
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
    private INetworkManagementEventObserver mAlertObserver;
    private BroadcastReceiver mAllowReceiver;
    private IConnectivityManager mConnManager;
    private BroadcastReceiver mConnReceiver;
    private final Context mContext;
    private int mCurForegroundState;
    final Handler mHandler;
    private Handler.Callback mHandlerCallback;
    private final RemoteCallbackList<INetworkPolicyListener> mListeners;
    private ArraySet<String> mMeteredIfaces;
    private final INetworkManagementService mNetworkManager;
    final ArrayMap<NetworkTemplate, NetworkPolicy> mNetworkPolicy;
    final ArrayMap<NetworkPolicy, String[]> mNetworkRules;
    private final INetworkStatsService mNetworkStats;
    private INotificationManager mNotifManager;
    private final ArraySet<NetworkTemplate> mOverLimitNotified;
    private BroadcastReceiver mPackageReceiver;
    private final AtomicFile mPolicyFile;
    private final IPowerManager mPowerManager;
    private PowerManagerInternal mPowerManagerInternal;
    private final SparseBooleanArray mPowerSaveWhitelistAppIds;
    private IProcessObserver mProcessObserver;
    volatile boolean mRestrictBackground;
    volatile boolean mRestrictPower;
    final Object mRulesLock;
    volatile boolean mScreenOn;
    private BroadcastReceiver mScreenReceiver;
    private BroadcastReceiver mSnoozeWarningReceiver;
    private BroadcastReceiver mStatsReceiver;
    private final boolean mSuppressDefaultPolicy;
    private final TrustedTime mTime;
    final SparseArray<SparseIntArray> mUidPidState;
    final SparseIntArray mUidPolicy;
    private BroadcastReceiver mUidRemovedReceiver;
    final SparseIntArray mUidRules;
    final SparseIntArray mUidState;
    private BroadcastReceiver mUserReceiver;
    private BroadcastReceiver mWifiConfigReceiver;
    private BroadcastReceiver mWifiStateReceiver;

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
        this.mPowerSaveWhitelistAppIds = new SparseBooleanArray();
        this.mMeteredIfaces = new ArraySet<>();
        this.mOverLimitNotified = new ArraySet<>();
        this.mActiveNotifs = new ArraySet<>();
        this.mUidState = new SparseIntArray();
        this.mUidPidState = new SparseArray<>();
        this.mCurForegroundState = 2;
        this.mListeners = new RemoteCallbackList<>();
        this.mProcessObserver = new IProcessObserver.Stub() {
            public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            }

            public void onProcessStateChanged(int pid, int uid, int procState) {
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    SparseIntArray pidState = NetworkPolicyManagerService.this.mUidPidState.get(uid);
                    if (pidState == null) {
                        pidState = new SparseIntArray(2);
                        NetworkPolicyManagerService.this.mUidPidState.put(uid, pidState);
                    }
                    pidState.put(pid, procState);
                    NetworkPolicyManagerService.this.computeUidStateLocked(uid);
                }
            }

            public void onProcessDied(int pid, int uid) {
                synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                    SparseIntArray pidState = NetworkPolicyManagerService.this.mUidPidState.get(uid);
                    if (pidState != null) {
                        pidState.delete(pid);
                        if (pidState.size() <= 0) {
                            NetworkPolicyManagerService.this.mUidPidState.remove(uid);
                        }
                        NetworkPolicyManagerService.this.computeUidStateLocked(uid);
                    }
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
                if (uid != -1 && "android.intent.action.PACKAGE_ADDED".equals(action)) {
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        NetworkPolicyManagerService.this.updateRulesForUidLocked(uid);
                    }
                }
            }
        };
        this.mUidRemovedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid != -1) {
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        NetworkPolicyManagerService.this.mUidPolicy.delete(uid);
                        NetworkPolicyManagerService.this.updateRulesForUidLocked(uid);
                        NetworkPolicyManagerService.this.writePolicyLocked();
                    }
                }
            }
        };
        this.mUserReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                intent.getAction();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId != -1) {
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        NetworkPolicyManagerService.this.removePoliciesForUserLocked(userId);
                        NetworkPolicyManagerService.this.updateRulesForGlobalChangeLocked(true);
                    }
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
                if (reason == 1) {
                    WifiConfiguration config = (WifiConfiguration) intent.getParcelableExtra("wifiConfiguration");
                    if (config.SSID != null) {
                        NetworkTemplate template = NetworkTemplate.buildTemplateWifi(config.SSID);
                        synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                            if (NetworkPolicyManagerService.this.mNetworkPolicy.containsKey(template)) {
                                NetworkPolicyManagerService.this.mNetworkPolicy.remove(template);
                                NetworkPolicyManagerService.this.writePolicyLocked();
                            }
                        }
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
                            NetworkPolicyManagerService.this.addNetworkPolicyLocked(new NetworkPolicy(template, -1, "UTC", -1L, -1L, -1L, -1L, meteredHint, true));
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
                if (!NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(limitName)) {
                    NetworkPolicyManagerService.this.mHandler.obtainMessage(5, iface).sendToTarget();
                }
            }
        };
        this.mConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
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
                switch (msg.what) {
                    case 1:
                        int uid = msg.arg1;
                        int uidRules = msg.arg2;
                        int length = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i = 0; i < length; i++) {
                            INetworkPolicyListener listener = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i);
                            if (listener != null) {
                                try {
                                    listener.onUidRulesChanged(uid, uidRules);
                                } catch (RemoteException e) {
                                }
                            }
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 2:
                        String[] meteredIfaces = (String[]) msg.obj;
                        int length2 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i2 = 0; i2 < length2; i2++) {
                            INetworkPolicyListener listener2 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i2);
                            if (listener2 != null) {
                                try {
                                    listener2.onMeteredIfacesChanged(meteredIfaces);
                                } catch (RemoteException e2) {
                                }
                            }
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
                                } catch (RemoteException e3) {
                                }
                                NetworkPolicyManagerService.this.updateNetworkEnabledLocked();
                                NetworkPolicyManagerService.this.updateNotificationsLocked();
                            }
                            break;
                        }
                        return true;
                    case 6:
                        boolean restrictBackground = msg.arg1 != 0;
                        int length3 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i3 = 0; i3 < length3; i3++) {
                            INetworkPolicyListener listener3 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i3);
                            if (listener3 != null) {
                                try {
                                    listener3.onRestrictBackgroundChanged(restrictBackground);
                                } catch (RemoteException e4) {
                                }
                            }
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 7:
                        long lowestRule = ((Long) msg.obj).longValue();
                        try {
                            long persistThreshold = lowestRule / 1000;
                            NetworkPolicyManagerService.this.mNetworkStats.advisePersistThreshold(persistThreshold);
                            break;
                        } catch (RemoteException e5) {
                        }
                        return true;
                    case 8:
                        NetworkPolicyManagerService.this.updateScreenOn();
                        return true;
                }
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing context");
        this.mActivityManager = (IActivityManager) Preconditions.checkNotNull(activityManager, "missing activityManager");
        this.mPowerManager = (IPowerManager) Preconditions.checkNotNull(powerManager, "missing powerManager");
        this.mNetworkStats = (INetworkStatsService) Preconditions.checkNotNull(networkStats, "missing networkStats");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(networkManagement, "missing networkManagement");
        this.mTime = (TrustedTime) Preconditions.checkNotNull(time, "missing TrustedTime");
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this.mHandlerCallback);
        this.mSuppressDefaultPolicy = suppressDefaultPolicy;
        this.mPolicyFile = new AtomicFile(new File(systemDir, "netpolicy.xml"));
    }

    public void bindConnectivityManager(IConnectivityManager connManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(connManager, "missing IConnectivityManager");
    }

    public void bindNotificationManager(INotificationManager notifManager) {
        this.mNotifManager = (INotificationManager) Preconditions.checkNotNull(notifManager, "missing INotificationManager");
    }

    public void systemReady() {
        if (!isBandwidthControlEnabled()) {
            Slog.w(TAG, "bandwidth controls disabled, unable to enforce policy");
            return;
        }
        PackageManager pm = this.mContext.getPackageManager();
        synchronized (this.mRulesLock) {
            SystemConfig sysConfig = SystemConfig.getInstance();
            ArraySet<String> allowPower = sysConfig.getAllowInPowerSave();
            for (int i = 0; i < allowPower.size(); i++) {
                String pkg = allowPower.valueAt(i);
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    if ((ai.flags & 1) != 0) {
                        this.mPowerSaveWhitelistAppIds.put(UserHandle.getAppId(ai.uid), true);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
            this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                public void onLowPowerModeChanged(boolean enabled) {
                    synchronized (NetworkPolicyManagerService.this.mRulesLock) {
                        if (NetworkPolicyManagerService.this.mRestrictPower != enabled) {
                            NetworkPolicyManagerService.this.mRestrictPower = enabled;
                            NetworkPolicyManagerService.this.updateRulesForGlobalChangeLocked(true);
                        }
                    }
                }
            });
            this.mRestrictPower = this.mPowerManagerInternal.getLowPowerModeEnabled();
            readPolicyLocked();
            if (this.mRestrictBackground || this.mRestrictPower) {
                updateRulesForGlobalChangeLocked(true);
                updateNotificationsLocked();
            }
        }
        updateScreenOn();
        try {
            this.mActivityManager.registerProcessObserver(this.mProcessObserver);
            this.mNetworkManager.registerObserver(this.mAlertObserver);
        } catch (RemoteException e2) {
        }
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction("android.intent.action.SCREEN_ON");
        screenFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mScreenReceiver, screenFilter);
        IntentFilter connFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE");
        this.mContext.registerReceiver(this.mConnReceiver, connFilter, "android.permission.CONNECTIVITY_INTERNAL", this.mHandler);
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
    }

    void updateNotificationsLocked() {
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
        if (this.mRestrictBackground) {
            enqueueRestrictedNotification(TAG_ALLOW_BACKGROUND);
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
            NetworkIdentity probeIdent = new NetworkIdentity(0, 0, subscriberId, (String) null, false);
            if (template.matches(probeIdent)) {
                return true;
            }
        }
        return false;
    }

    private void notifyOverLimitLocked(NetworkTemplate template) {
        if (!this.mOverLimitNotified.contains(template)) {
            this.mContext.startActivity(buildNetworkOverLimitIntent(template));
            this.mOverLimitNotified.add(template);
        }
    }

    private void notifyUnderLimitLocked(NetworkTemplate template) {
        this.mOverLimitNotified.remove(template);
    }

    private String buildNotificationTag(NetworkPolicy policy, int type) {
        return "NetworkPolicy:" + policy.template.hashCode() + ":" + type;
    }

    private void enqueueNotification(NetworkPolicy policy, int type, long totalBytes) {
        CharSequence title;
        CharSequence title2;
        String tag = buildNotificationTag(policy, type);
        Notification.Builder builder = new Notification.Builder(this.mContext);
        builder.setOnlyAlertOnce(true);
        builder.setWhen(0L);
        builder.setColor(this.mContext.getResources().getColor(R.color.system_accent3_600));
        Resources res = this.mContext.getResources();
        switch (type) {
            case 1:
                CharSequence title3 = res.getText(R.string.lockscreen_instructions_when_pattern_enabled);
                CharSequence body = res.getString(R.string.lockscreen_missing_sim_instructions);
                builder.setSmallIcon(R.drawable.stat_notify_error);
                builder.setTicker(title3);
                builder.setContentTitle(title3);
                builder.setContentText(body);
                Intent snoozeIntent = buildSnoozeWarningIntent(policy.template);
                builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, snoozeIntent, 134217728));
                Intent viewIntent = buildViewDataUsageIntent(policy.template);
                builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, viewIntent, 134217728));
                break;
            case 2:
                CharSequence body2 = res.getText(R.string.lockscreen_password_wrong);
                int icon = R.drawable.input_method_item_background;
                switch (policy.template.getMatchRule()) {
                    case 1:
                        title2 = res.getText(R.string.lockscreen_missing_sim_message_short);
                        break;
                    case 2:
                        title2 = res.getText(R.string.lockscreen_missing_sim_instructions_long);
                        break;
                    case 3:
                        title2 = res.getText(R.string.lockscreen_missing_sim_message);
                        break;
                    case 4:
                        title2 = res.getText(R.string.lockscreen_network_locked_message);
                        icon = R.drawable.stat_notify_error;
                        break;
                    default:
                        title2 = null;
                        break;
                }
                builder.setOngoing(true);
                builder.setSmallIcon(icon);
                builder.setTicker(title2);
                builder.setContentTitle(title2);
                builder.setContentText(body2);
                Intent intent = buildNetworkOverLimitIntent(policy.template);
                builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent, 134217728));
                break;
            case 3:
                long overBytes = totalBytes - policy.limitBytes;
                CharSequence body3 = res.getString(R.string.lockscreen_permanent_disabled_sim_message_short, Formatter.formatFileSize(this.mContext, overBytes));
                switch (policy.template.getMatchRule()) {
                    case 1:
                        title = res.getText(R.string.lockscreen_pattern_wrong);
                        break;
                    case 2:
                        title = res.getText(R.string.lockscreen_pattern_correct);
                        break;
                    case 3:
                        title = res.getText(R.string.lockscreen_pattern_instructions);
                        break;
                    case 4:
                        title = res.getText(R.string.lockscreen_permanent_disabled_sim_instructions);
                        break;
                    default:
                        title = null;
                        break;
                }
                builder.setOngoing(true);
                builder.setSmallIcon(R.drawable.stat_notify_error);
                builder.setTicker(title);
                builder.setContentTitle(title);
                builder.setContentText(body3);
                Intent intent2 = buildViewDataUsageIntent(policy.template);
                builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent2, 134217728));
                break;
        }
        try {
            String packageName = this.mContext.getPackageName();
            int[] idReceived = new int[1];
            this.mNotifManager.enqueueNotificationWithTag(packageName, packageName, tag, 0, builder.getNotification(), idReceived, 0);
            this.mActiveNotifs.add(tag);
        } catch (RemoteException e) {
        }
    }

    private void enqueueRestrictedNotification(String tag) {
        Resources res = this.mContext.getResources();
        Notification.Builder builder = new Notification.Builder(this.mContext);
        CharSequence title = res.getText(R.string.lockscreen_return_to_call);
        CharSequence body = res.getString(R.string.lockscreen_screen_locked);
        builder.setOnlyAlertOnce(true);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.stat_notify_error);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setColor(this.mContext.getResources().getColor(R.color.system_accent3_600));
        Intent intent = buildAllowBackgroundDataIntent();
        builder.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728));
        try {
            String packageName = this.mContext.getPackageName();
            int[] idReceived = new int[1];
            this.mNotifManager.enqueueNotificationWithTag(packageName, packageName, tag, 0, builder.getNotification(), idReceived, 0);
            this.mActiveNotifs.add(tag);
        } catch (RemoteException e) {
        }
    }

    private void cancelNotification(String tag) {
        try {
            String packageName = this.mContext.getPackageName();
            this.mNotifManager.cancelNotificationWithTag(packageName, tag, 0, 0);
        } catch (RemoteException e) {
        }
    }

    void updateNetworkEnabledLocked() {
        long currentTime = currentTimeMillis();
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
            if (policy.limitBytes == -1 || !policy.hasCycle()) {
                setNetworkTemplateEnabled(policy.template, true);
            } else {
                long start = NetworkPolicyManager.computeLastCycleBoundary(currentTime, policy);
                long totalBytes = getTotalBytes(policy.template, start, currentTime);
                boolean overLimitWithoutSnooze = policy.isOverLimit(totalBytes) && policy.lastLimitSnooze < start;
                boolean networkEnabled = !overLimitWithoutSnooze;
                setNetworkTemplateEnabled(policy.template, networkEnabled);
            }
        }
    }

    private void setNetworkTemplateEnabled(NetworkTemplate template, boolean enabled) {
    }

    void updateNetworkRulesLocked() {
        long start;
        long totalBytes;
        long quotaBytes;
        try {
            NetworkState[] states = this.mConnManager.getAllNetworkState();
            boolean powerSave = this.mRestrictPower && !this.mRestrictBackground;
            ArrayList<Pair<String, NetworkIdentity>> connIdents = new ArrayList<>(states.length);
            ArraySet<String> connIfaces = new ArraySet<>(states.length);
            int len$ = states.length;
            int i$ = 0;
            while (true) {
                int i$2 = i$;
                if (i$2 >= len$) {
                    break;
                }
                NetworkState state = states[i$2];
                if (state.networkInfo.isConnected()) {
                    NetworkIdentity ident = NetworkIdentity.buildNetworkIdentity(this.mContext, state);
                    String baseIface = state.linkProperties.getInterfaceName();
                    if (baseIface != null) {
                        connIdents.add(Pair.create(baseIface, ident));
                        if (powerSave) {
                            connIfaces.add(baseIface);
                        }
                    }
                    List<LinkProperties> stackedLinks = state.linkProperties.getStackedLinks();
                    for (LinkProperties stackedLink : stackedLinks) {
                        String stackedIface = stackedLink.getInterfaceName();
                        if (stackedIface != null) {
                            connIdents.add(Pair.create(stackedIface, ident));
                            if (powerSave) {
                                connIfaces.add(stackedIface);
                            }
                        }
                    }
                }
                i$ = i$2 + 1;
            }
            this.mNetworkRules.clear();
            ArrayList arrayListNewArrayList = Lists.newArrayList();
            for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
                arrayListNewArrayList.clear();
                for (int j = connIdents.size() - 1; j >= 0; j--) {
                    Pair<String, NetworkIdentity> ident2 = connIdents.get(j);
                    if (policy.template.matches((NetworkIdentity) ident2.second)) {
                        arrayListNewArrayList.add(ident2.first);
                    }
                }
                if (arrayListNewArrayList.size() > 0) {
                    this.mNetworkRules.put(policy, (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]));
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
                        removeInterfaceQuota(iface);
                        setInterfaceQuota(iface, quotaBytes);
                        newMeteredIfaces.add(iface);
                        if (powerSave) {
                            connIfaces.remove(iface);
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
                removeInterfaceQuota(iface2);
                setInterfaceQuota(iface2, JobStatus.NO_LATEST_RUNTIME);
                newMeteredIfaces.add(iface2);
            }
            this.mHandler.obtainMessage(7, Long.valueOf(lowestRule)).sendToTarget();
            for (int i4 = this.mMeteredIfaces.size() - 1; i4 >= 0; i4--) {
                String iface3 = this.mMeteredIfaces.valueAt(i4);
                if (!newMeteredIfaces.contains(iface3)) {
                    removeInterfaceQuota(iface3);
                }
            }
            this.mMeteredIfaces = newMeteredIfaces;
            String[] meteredIfaces = (String[]) this.mMeteredIfaces.toArray(new String[this.mMeteredIfaces.size()]);
            this.mHandler.obtainMessage(2, meteredIfaces).sendToTarget();
        } catch (RemoteException e) {
        }
    }

    private void ensureActiveMobilePolicyLocked() {
        if (!this.mSuppressDefaultPolicy) {
            TelephonyManager tele = TelephonyManager.from(this.mContext);
            SubscriptionManager sub = SubscriptionManager.from(this.mContext);
            int[] subIds = sub.getActiveSubscriptionIdList();
            for (int subId : subIds) {
                String subscriberId = tele.getSubscriberId(subId);
                ensureActiveMobilePolicyLocked(subscriberId);
            }
        }
    }

    private void ensureActiveMobilePolicyLocked(String subscriberId) {
        NetworkIdentity probeIdent = new NetworkIdentity(0, 0, subscriberId, (String) null, false);
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkTemplate template = this.mNetworkPolicy.keyAt(i);
            if (template.matches(probeIdent)) {
                return;
            }
        }
        Slog.i(TAG, "No policy for subscriber " + NetworkIdentity.scrubSubscriberId(subscriberId) + "; generating default policy");
        long warningBytes = ((long) this.mContext.getResources().getInteger(R.integer.config_defaultUndimsRequired)) * 1048576;
        Time time = new Time();
        time.setToNow();
        int cycleDay = time.monthDay;
        String cycleTimezone = time.timezone;
        NetworkTemplate template2 = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        NetworkPolicy policy = new NetworkPolicy(template2, cycleDay, cycleTimezone, warningBytes, -1L, -1L, -1L, true, true);
        addNetworkPolicyLocked(policy);
    }

    private void readPolicyLocked() {
        boolean metered;
        this.mNetworkPolicy.clear();
        this.mUidPolicy.clear();
        FileInputStream fis = null;
        try {
            fis = this.mPolicyFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, null);
            int version = 1;
            while (true) {
                int type = in.next();
                if (type == 1) {
                    return;
                }
                String tag = in.getName();
                if (type == 2) {
                    if (TAG_POLICY_LIST.equals(tag)) {
                        version = XmlUtils.readIntAttribute(in, ATTR_VERSION);
                        if (version >= 3) {
                            this.mRestrictBackground = XmlUtils.readBooleanAttribute(in, ATTR_RESTRICT_BACKGROUND);
                        } else {
                            this.mRestrictBackground = false;
                        }
                    } else if (TAG_NETWORK_POLICY.equals(tag)) {
                        int networkTemplate = XmlUtils.readIntAttribute(in, ATTR_NETWORK_TEMPLATE);
                        String subscriberId = in.getAttributeValue(null, ATTR_SUBSCRIBER_ID);
                        String networkId = version >= 9 ? in.getAttributeValue(null, ATTR_NETWORK_ID) : null;
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
                        boolean inferred = version >= 7 ? XmlUtils.readBooleanAttribute(in, ATTR_INFERRED) : false;
                        NetworkTemplate template = new NetworkTemplate(networkTemplate, subscriberId, networkId);
                        this.mNetworkPolicy.put(template, new NetworkPolicy(template, cycleDay, cycleTimezone, warningBytes, limitBytes, lastWarningSnooze, lastLimitSnooze, metered, inferred));
                    } else if (TAG_UID_POLICY.equals(tag)) {
                        int uid = XmlUtils.readIntAttribute(in, ATTR_UID);
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
                    }
                }
            }
        } catch (FileNotFoundException e) {
            upgradeLegacyBackgroundData();
        } catch (IOException e2) {
            Log.wtf(TAG, "problem reading network policy", e2);
        } catch (XmlPullParserException e3) {
            Log.wtf(TAG, "problem reading network policy", e3);
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    private void upgradeLegacyBackgroundData() {
        this.mRestrictBackground = Settings.Secure.getInt(this.mContext.getContentResolver(), "background_data", 1) != 1;
        if (this.mRestrictBackground) {
            Intent broadcast = new Intent("android.net.conn.BACKGROUND_DATA_SETTING_CHANGED");
            this.mContext.sendBroadcastAsUser(broadcast, UserHandle.ALL);
        }
    }

    void writePolicyLocked() {
        FileOutputStream fos = null;
        try {
            fos = this.mPolicyFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_POLICY_LIST);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_VERSION, 10);
            XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_RESTRICT_BACKGROUND, this.mRestrictBackground);
            for (int i = 0; i < this.mNetworkPolicy.size(); i++) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
                NetworkTemplate template = policy.template;
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
            for (int i2 = 0; i2 < this.mUidPolicy.size(); i2++) {
                int uid = this.mUidPolicy.keyAt(i2);
                int policy2 = this.mUidPolicy.valueAt(i2);
                if (policy2 != 0) {
                    fastXmlSerializer.startTag(null, TAG_UID_POLICY);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_UID, uid);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_POLICY, policy2);
                    fastXmlSerializer.endTag(null, TAG_UID_POLICY);
                }
            }
            fastXmlSerializer.endTag(null, TAG_POLICY_LIST);
            fastXmlSerializer.endDocument();
            this.mPolicyFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mPolicyFile.failWrite(fos);
            }
        }
    }

    public void setUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mRulesLock) {
            int oldPolicy = this.mUidPolicy.get(uid, 0);
            if (oldPolicy != policy) {
                setUidPolicyUncheckedLocked(uid, policy, true);
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
                setUidPolicyUncheckedLocked(uid, policy2, true);
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
            int policy2 = oldPolicy & (policy ^ (-1));
            if (oldPolicy != policy2) {
                setUidPolicyUncheckedLocked(uid, policy2, true);
            }
        }
    }

    private void setUidPolicyUncheckedLocked(int uid, int policy, boolean persist) {
        this.mUidPolicy.put(uid, policy);
        updateRulesForUidLocked(uid);
        if (persist) {
            writePolicyLocked();
        }
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

    public int[] getPowerSaveAppIdWhitelist() {
        int[] appids;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            int size = this.mPowerSaveWhitelistAppIds.size();
            appids = new int[size];
            for (int i = 0; i < size; i++) {
                appids[i] = this.mPowerSaveWhitelistAppIds.keyAt(i);
            }
        }
        return appids;
    }

    void removePoliciesForUserLocked(int userId) {
        int[] uids = new int[0];
        for (int i = 0; i < this.mUidPolicy.size(); i++) {
            int uid = this.mUidPolicy.keyAt(i);
            if (UserHandle.getUserId(uid) == userId) {
                uids = ArrayUtils.appendInt(uids, uid);
            }
        }
        if (uids.length > 0) {
            int[] arr$ = uids;
            for (int uid2 : arr$) {
                this.mUidPolicy.delete(uid2);
                updateRulesForUidLocked(uid2);
            }
            writePolicyLocked();
        }
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
        maybeRefreshTrustedTime();
        synchronized (this.mRulesLock) {
            normalizePoliciesLocked(policies);
            updateNetworkEnabledLocked();
            updateNetworkRulesLocked();
            updateNotificationsLocked();
            writePolicyLocked();
        }
    }

    void addNetworkPolicyLocked(NetworkPolicy policy) {
        NetworkPolicy[] policies = getNetworkPolicies();
        setNetworkPolicies((NetworkPolicy[]) ArrayUtils.appendElement(NetworkPolicy.class, policies, policy));
    }

    public NetworkPolicy[] getNetworkPolicies() {
        NetworkPolicy[] policies;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", TAG);
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
        normalizePoliciesLocked(getNetworkPolicies());
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

    public void setRestrictBackground(boolean restrictBackground) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        maybeRefreshTrustedTime();
        synchronized (this.mRulesLock) {
            this.mRestrictBackground = restrictBackground;
            updateRulesForGlobalChangeLocked(false);
            updateNotificationsLocked();
            writePolicyLocked();
        }
        this.mHandler.obtainMessage(6, restrictBackground ? 1 : 0, 0).sendToTarget();
    }

    public boolean getRestrictBackground() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            z = this.mRestrictBackground;
        }
        return z;
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
            fout.print("Restrict background: ");
            fout.println(this.mRestrictBackground);
            fout.print("Restrict power: ");
            fout.println(this.mRestrictPower);
            fout.print("Current foreground state: ");
            fout.println(this.mCurForegroundState);
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
                NetworkPolicyManager.dumpPolicy(fout, policy);
                fout.println();
            }
            fout.decreaseIndent();
            int size2 = this.mPowerSaveWhitelistAppIds.size();
            if (size2 > 0) {
                fout.println("Power save whitelist app ids:");
                fout.increaseIndent();
                for (int i4 = 0; i4 < size2; i4++) {
                    fout.print("UID=");
                    fout.print(this.mPowerSaveWhitelistAppIds.keyAt(i4));
                    fout.print(": ");
                    fout.print(this.mPowerSaveWhitelistAppIds.valueAt(i4));
                    fout.println();
                }
                fout.decreaseIndent();
            }
            SparseBooleanArray knownUids = new SparseBooleanArray();
            collectKeys(this.mUidState, knownUids);
            collectKeys(this.mUidRules, knownUids);
            fout.println("Status for known UIDs:");
            fout.increaseIndent();
            int size3 = knownUids.size();
            for (int i5 = 0; i5 < size3; i5++) {
                int uid2 = knownUids.keyAt(i5);
                fout.print("UID=");
                fout.print(uid2);
                int state = this.mUidState.get(uid2, 13);
                fout.print(" state=");
                fout.print(state);
                fout.print(state <= this.mCurForegroundState ? " (fg)" : " (bg)");
                fout.print(" pids=");
                int foregroundIndex = this.mUidPidState.indexOfKey(uid2);
                if (foregroundIndex < 0) {
                    fout.print("UNKNOWN");
                } else {
                    dumpSparseIntArray(fout, this.mUidPidState.valueAt(foregroundIndex));
                }
                fout.print(" rules=");
                int rulesIndex = this.mUidRules.indexOfKey(uid2);
                if (rulesIndex < 0) {
                    fout.print("UNKNOWN");
                } else {
                    NetworkPolicyManager.dumpRules(fout, this.mUidRules.valueAt(rulesIndex));
                }
                fout.println();
            }
            fout.decreaseIndent();
        }
    }

    public boolean isUidForeground(int uid) {
        boolean zIsUidForegroundLocked;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mRulesLock) {
            zIsUidForegroundLocked = isUidForegroundLocked(uid);
        }
        return zIsUidForegroundLocked;
    }

    boolean isUidForegroundLocked(int uid) {
        return this.mScreenOn && this.mUidState.get(uid, 13) <= this.mCurForegroundState;
    }

    void computeUidStateLocked(int uid) {
        SparseIntArray pidState = this.mUidPidState.get(uid);
        int uidState = 13;
        if (pidState != null) {
            int size = pidState.size();
            for (int i = 0; i < size; i++) {
                int state = pidState.valueAt(i);
                if (state < uidState) {
                    uidState = state;
                }
            }
        }
        int oldUidState = this.mUidState.get(uid, 13);
        if (oldUidState != uidState) {
            this.mUidState.put(uid, uidState);
            boolean oldForeground = oldUidState <= this.mCurForegroundState;
            boolean newForeground = uidState <= this.mCurForegroundState;
            if (oldForeground != newForeground) {
                updateRulesForUidLocked(uid);
            }
        }
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
            if (this.mUidState.valueAt(i) <= this.mCurForegroundState) {
                int uid = this.mUidState.keyAt(i);
                updateRulesForUidLocked(uid);
            }
        }
    }

    void updateRulesForGlobalChangeLocked(boolean restrictedNetworksChanged) {
        PackageManager pm = this.mContext.getPackageManager();
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        this.mCurForegroundState = (this.mRestrictBackground || !this.mRestrictPower) ? 2 : 3;
        List<UserInfo> users = um.getUsers();
        List<ApplicationInfo> apps = pm.getInstalledApplications(8704);
        for (UserInfo user : users) {
            for (ApplicationInfo app : apps) {
                int uid = UserHandle.getUid(user.id, app.uid);
                updateRulesForUidLocked(uid);
            }
        }
        updateRulesForUidLocked(1013);
        updateRulesForUidLocked(1019);
        if (restrictedNetworksChanged) {
            normalizePoliciesLocked();
            updateNetworkRulesLocked();
        }
    }

    private static boolean isUidValidForRules(int uid) {
        return uid == 1013 || uid == 1019 || UserHandle.isApp(uid);
    }

    void updateRulesForUidLocked(int uid) {
        if (isUidValidForRules(uid)) {
            int uidPolicy = this.mUidPolicy.get(uid, 0);
            boolean uidForeground = isUidForegroundLocked(uid);
            int uidRules = 0;
            if (!uidForeground && (uidPolicy & 1) != 0) {
                uidRules = 1;
            } else if (this.mRestrictBackground) {
                if (!uidForeground) {
                    uidRules = 1;
                }
            } else if (this.mRestrictPower) {
                boolean whitelisted = this.mPowerSaveWhitelistAppIds.get(UserHandle.getAppId(uid));
                if (!whitelisted && !uidForeground && (uidPolicy & 2) == 0) {
                    uidRules = 1;
                }
            }
            if (uidRules == 0) {
                this.mUidRules.delete(uid);
            } else {
                this.mUidRules.put(uid, uidRules);
            }
            boolean rejectMetered = (uidRules & 1) != 0;
            setUidNetworkRules(uid, rejectMetered);
            this.mHandler.obtainMessage(1, uid, uidRules).sendToTarget();
            try {
                this.mNetworkStats.setUidForeground(uid, uidForeground);
            } catch (RemoteException e) {
            }
        }
    }

    private void setInterfaceQuota(String iface, long quotaBytes) {
        try {
            this.mNetworkManager.setInterfaceQuota(iface, quotaBytes);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting interface quota", e2);
        }
    }

    private void removeInterfaceQuota(String iface) {
        try {
            this.mNetworkManager.removeInterfaceQuota(iface);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem removing interface quota", e2);
        }
    }

    private void setUidNetworkRules(int uid, boolean rejectOnQuotaInterfaces) {
        try {
            this.mNetworkManager.setUidNetworkRules(uid, rejectOnQuotaInterfaces);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting uid rules", e2);
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
        if (this.mTime.getCacheAge() > TIME_CACHE_MAX_AGE) {
            this.mTime.forceRefresh();
        }
    }

    private long currentTimeMillis() {
        return this.mTime.hasCache() ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
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

    private static void dumpSparseIntArray(PrintWriter fout, SparseIntArray value) {
        fout.print("[");
        int size = value.size();
        for (int i = 0; i < size; i++) {
            fout.print(value.keyAt(i) + "=" + value.valueAt(i));
            if (i < size - 1) {
                fout.print(",");
            }
        }
        fout.print("]");
    }
}
