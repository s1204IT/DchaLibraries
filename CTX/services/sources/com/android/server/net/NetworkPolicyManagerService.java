package com.android.server.net;

import android.R;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
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
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkQuotaInfo;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.StringNetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BestClock;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IDeviceIdleController;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DataUnit;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.RecurrenceRule;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.internal.util.XmlUtils;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.NetworkManagementService;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;

public class NetworkPolicyManagerService extends INetworkPolicyManager.Stub {
    private static final String ACTION_ALLOW_BACKGROUND = "com.android.server.net.action.ALLOW_BACKGROUND";
    private static final String ACTION_SNOOZE_RAPID = "com.android.server.net.action.SNOOZE_RAPID";
    private static final String ACTION_SNOOZE_WARNING = "com.android.server.net.action.SNOOZE_WARNING";
    private static final String ATTR_APP_ID = "appId";

    @Deprecated
    private static final String ATTR_CYCLE_DAY = "cycleDay";
    private static final String ATTR_CYCLE_END = "cycleEnd";
    private static final String ATTR_CYCLE_PERIOD = "cyclePeriod";
    private static final String ATTR_CYCLE_START = "cycleStart";

    @Deprecated
    private static final String ATTR_CYCLE_TIMEZONE = "cycleTimezone";
    private static final String ATTR_INFERRED = "inferred";
    private static final String ATTR_LAST_LIMIT_SNOOZE = "lastLimitSnooze";
    private static final String ATTR_LAST_SNOOZE = "lastSnooze";
    private static final String ATTR_LAST_WARNING_SNOOZE = "lastWarningSnooze";
    private static final String ATTR_LIMIT_BEHAVIOR = "limitBehavior";
    private static final String ATTR_LIMIT_BYTES = "limitBytes";
    private static final String ATTR_METERED = "metered";
    private static final String ATTR_NETWORK_ID = "networkId";
    private static final String ATTR_NETWORK_TEMPLATE = "networkTemplate";
    private static final String ATTR_OWNER_PACKAGE = "ownerPackage";
    private static final String ATTR_POLICY = "policy";
    private static final String ATTR_RESTRICT_BACKGROUND = "restrictBackground";
    private static final String ATTR_SUBSCRIBER_ID = "subscriberId";
    private static final String ATTR_SUB_ID = "subId";
    private static final String ATTR_SUMMARY = "summary";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_USAGE_BYTES = "usageBytes";
    private static final String ATTR_USAGE_TIME = "usageTime";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_WARNING_BYTES = "warningBytes";
    private static final int CHAIN_TOGGLE_DISABLE = 2;
    private static final int CHAIN_TOGGLE_ENABLE = 1;
    private static final int CHAIN_TOGGLE_NONE = 0;
    private static final int MSG_ADVISE_PERSIST_THRESHOLD = 7;
    private static final int MSG_LIMIT_REACHED = 5;
    private static final int MSG_METERED_IFACES_CHANGED = 2;
    private static final int MSG_METERED_RESTRICTED_PACKAGES_CHANGED = 17;
    private static final int MSG_POLICIES_CHANGED = 13;
    private static final int MSG_REMOVE_INTERFACE_QUOTA = 11;
    private static final int MSG_RESET_FIREWALL_RULES_BY_UID = 15;
    private static final int MSG_RESTRICT_BACKGROUND_CHANGED = 6;
    private static final int MSG_RULES_CHANGED = 1;
    private static final int MSG_SET_NETWORK_TEMPLATE_ENABLED = 18;
    private static final int MSG_SUBSCRIPTION_OVERRIDE = 16;
    private static final int MSG_UPDATE_INTERFACE_QUOTA = 10;
    public static final int OPPORTUNISTIC_QUOTA_UNKNOWN = -1;
    private static final String PROP_SUB_PLAN_OWNER = "persist.sys.sub_plan_owner";
    private static final float QUOTA_FRAC_JOBS_DEFAULT = 0.5f;
    private static final float QUOTA_FRAC_MULTIPATH_DEFAULT = 0.5f;
    private static final float QUOTA_LIMITED_DEFAULT = 0.1f;
    static final String TAG = "NetworkPolicy";
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_RESTRICT_BACKGROUND = "restrict-background";
    private static final String TAG_REVOKED_RESTRICT_BACKGROUND = "revoked-restrict-background";
    private static final String TAG_SUBSCRIPTION_PLAN = "subscription-plan";
    private static final String TAG_UID_POLICY = "uid-policy";
    private static final String TAG_WHITELIST = "whitelist";

    @VisibleForTesting
    public static final int TYPE_LIMIT = 35;

    @VisibleForTesting
    public static final int TYPE_LIMIT_SNOOZED = 36;

    @VisibleForTesting
    public static final int TYPE_RAPID = 45;
    private static final int TYPE_RESTRICT_BACKGROUND = 1;
    private static final int TYPE_RESTRICT_POWER = 2;

    @VisibleForTesting
    public static final int TYPE_WARNING = 34;
    private static final int UID_MSG_GONE = 101;
    private static final int UID_MSG_STATE_CHANGED = 100;
    private static final int VERSION_ADDED_CYCLE = 11;
    private static final int VERSION_ADDED_INFERRED = 7;
    private static final int VERSION_ADDED_METERED = 4;
    private static final int VERSION_ADDED_NETWORK_ID = 9;
    private static final int VERSION_ADDED_RESTRICT_BACKGROUND = 3;
    private static final int VERSION_ADDED_SNOOZE = 2;
    private static final int VERSION_ADDED_TIMEZONE = 6;
    private static final int VERSION_INIT = 1;
    private static final int VERSION_LATEST = 11;
    private static final int VERSION_SPLIT_SNOOZE = 5;
    private static final int VERSION_SWITCH_APP_ID = 8;
    private static final int VERSION_SWITCH_UID = 10;
    private static final long WAIT_FOR_ADMIN_DATA_TIMEOUT_MS = 10000;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private final ArraySet<NotificationId> mActiveNotifs;
    private final IActivityManager mActivityManager;
    private ActivityManagerInternal mActivityManagerInternal;
    private final CountDownLatch mAdminDataAvailableLatch;
    private final INetworkManagementEventObserver mAlertObserver;
    private final BroadcastReceiver mAllowReceiver;
    private final AppOpsManager mAppOps;
    private final CarrierConfigManager mCarrierConfigManager;
    private BroadcastReceiver mCarrierConfigReceiver;
    private final Clock mClock;
    private IConnectivityManager mConnManager;
    private BroadcastReceiver mConnReceiver;
    private final Context mContext;

    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mDefaultRestrictBackgroundWhitelistUids;
    private IDeviceIdleController mDeviceIdleController;

    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mDeviceIdleMode;

    @GuardedBy("mUidRulesFirstLock")
    final SparseBooleanArray mFirewallChainStates;
    final Handler mHandler;
    private final Handler.Callback mHandlerCallback;
    private final IPackageManager mIPm;
    private final RemoteCallbackList<INetworkPolicyListener> mListeners;
    private boolean mLoadedRestrictBackground;
    private final NetworkPolicyLogger mLogger;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private String[] mMergedSubscriberIds;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private ArraySet<String> mMeteredIfaces;

    @GuardedBy("mUidRulesFirstLock")
    private final SparseArray<Set<Integer>> mMeteredRestrictedUids;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseIntArray mNetIdToSubId;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private final INetworkManagementService mNetworkManager;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseBooleanArray mNetworkMetered;
    final Object mNetworkPoliciesSecondLock;

    @GuardedBy("mNetworkPoliciesSecondLock")
    final ArrayMap<NetworkTemplate, NetworkPolicy> mNetworkPolicy;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseBooleanArray mNetworkRoaming;
    private NetworkStatsManagerInternal mNetworkStats;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private final ArraySet<NetworkTemplate> mOverLimitNotified;
    private final BroadcastReceiver mPackageReceiver;

    @GuardedBy("allLocks")
    private final AtomicFile mPolicyFile;
    private PowerManagerInternal mPowerManagerInternal;

    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mPowerSaveTempWhitelistAppIds;

    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mPowerSaveWhitelistAppIds;

    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds;
    private final BroadcastReceiver mPowerSaveWhitelistReceiver;

    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mRestrictBackground;
    private boolean mRestrictBackgroundBeforeBsm;

    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mRestrictBackgroundChangedInBsm;

    @GuardedBy("mUidRulesFirstLock")
    private PowerSaveState mRestrictBackgroundPowerState;

    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mRestrictBackgroundWhitelistRevokedUids;

    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mRestrictPower;
    private final BroadcastReceiver mSnoozeReceiver;
    public final StatLogger mStatLogger;
    private final BroadcastReceiver mStatsReceiver;

    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseArray<String> mSubIdToSubscriberId;

    @GuardedBy("mNetworkPoliciesSecondLock")
    final SparseLongArray mSubscriptionOpportunisticQuota;

    @GuardedBy("mNetworkPoliciesSecondLock")
    final SparseArray<SubscriptionPlan[]> mSubscriptionPlans;

    @GuardedBy("mNetworkPoliciesSecondLock")
    final SparseArray<String> mSubscriptionPlansOwner;
    private final boolean mSuppressDefaultPolicy;

    @GuardedBy("allLocks")
    volatile boolean mSystemReady;

    @VisibleForTesting
    public final Handler mUidEventHandler;
    private final Handler.Callback mUidEventHandlerCallback;
    private final ServiceThread mUidEventThread;

    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidFirewallDozableRules;

    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidFirewallPowerSaveRules;

    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidFirewallStandbyRules;
    private final IUidObserver mUidObserver;

    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidPolicy;
    private final BroadcastReceiver mUidRemovedReceiver;

    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidRules;
    final Object mUidRulesFirstLock;

    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidState;
    private UsageStatsManagerInternal mUsageStats;
    private final UserManager mUserManager;
    private final BroadcastReceiver mUserReceiver;
    private final BroadcastReceiver mWifiReceiver;
    private static final boolean LOGD = NetworkPolicyLogger.LOGD;
    private static final boolean LOGV = NetworkPolicyLogger.LOGV;
    private static final long QUOTA_UNLIMITED_DEFAULT = DataUnit.MEBIBYTES.toBytes(20);

    @Retention(RetentionPolicy.SOURCE)
    public @interface ChainToggleType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RestrictType {
    }

    interface Stats {
        public static final int COUNT = 2;
        public static final int IS_UID_NETWORKING_BLOCKED = 1;
        public static final int UPDATE_NETWORK_ENABLED = 0;
    }

    public NetworkPolicyManagerService(Context context, IActivityManager iActivityManager, INetworkManagementService iNetworkManagementService) {
        this(context, iActivityManager, iNetworkManagementService, AppGlobals.getPackageManager(), getDefaultClock(), getDefaultSystemDir(), false);
    }

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    private static Clock getDefaultClock() {
        return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
    }

    public NetworkPolicyManagerService(Context context, IActivityManager iActivityManager, INetworkManagementService iNetworkManagementService, IPackageManager iPackageManager, Clock clock, File file, boolean z) {
        this.mUidRulesFirstLock = new Object();
        this.mNetworkPoliciesSecondLock = new Object();
        this.mAdminDataAvailableLatch = new CountDownLatch(1);
        this.mNetworkPolicy = new ArrayMap<>();
        this.mSubscriptionPlans = new SparseArray<>();
        this.mSubscriptionPlansOwner = new SparseArray<>();
        this.mSubscriptionOpportunisticQuota = new SparseLongArray();
        this.mUidPolicy = new SparseIntArray();
        this.mUidRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistAppIds = new SparseBooleanArray();
        this.mPowerSaveTempWhitelistAppIds = new SparseBooleanArray();
        this.mDefaultRestrictBackgroundWhitelistUids = new SparseBooleanArray();
        this.mRestrictBackgroundWhitelistRevokedUids = new SparseBooleanArray();
        this.mMeteredIfaces = new ArraySet<>();
        this.mOverLimitNotified = new ArraySet<>();
        this.mActiveNotifs = new ArraySet<>();
        this.mUidState = new SparseIntArray();
        this.mNetworkMetered = new SparseBooleanArray();
        this.mNetworkRoaming = new SparseBooleanArray();
        this.mNetIdToSubId = new SparseIntArray();
        this.mSubIdToSubscriberId = new SparseArray<>();
        this.mMergedSubscriberIds = EmptyArray.STRING;
        this.mMeteredRestrictedUids = new SparseArray<>();
        this.mListeners = new RemoteCallbackList<>();
        this.mLogger = new NetworkPolicyLogger();
        this.mStatLogger = new StatLogger(new String[]{"updateNetworkEnabledNL()", "isUidNetworkingBlocked()"});
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int i, int i2, long j) {
                NetworkPolicyManagerService.this.mUidEventHandler.obtainMessage(100, i, i2, Long.valueOf(j)).sendToTarget();
            }

            public void onUidGone(int i, boolean z2) {
                NetworkPolicyManagerService.this.mUidEventHandler.obtainMessage(101, i, 0).sendToTarget();
            }

            public void onUidActive(int i) {
            }

            public void onUidIdle(int i, boolean z2) {
            }

            public void onUidCachedChanged(int i, boolean z2) {
            }
        };
        this.mPowerSaveWhitelistReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.updatePowerSaveWhitelistUL();
                    NetworkPolicyManagerService.this.updateRulesForRestrictPowerUL();
                    NetworkPolicyManagerService.this.updateRulesForAppIdleUL();
                }
            }
        };
        this.mPackageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
                if (intExtra != -1 && "android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if (NetworkPolicyManagerService.LOGV) {
                        Slog.v(NetworkPolicyManagerService.TAG, "ACTION_PACKAGE_ADDED for uid=" + intExtra);
                    }
                    synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                        NetworkPolicyManagerService.this.updateRestrictionRulesForUidUL(intExtra);
                    }
                }
            }
        };
        this.mUidRemovedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
                if (intExtra == -1) {
                    return;
                }
                if (NetworkPolicyManagerService.LOGV) {
                    Slog.v(NetworkPolicyManagerService.TAG, "ACTION_UID_REMOVED for uid=" + intExtra);
                }
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.onUidDeletedUL(intExtra);
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.writePolicyAL();
                    }
                }
            }
        };
        this.mUserReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                byte b = -1;
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (intExtra == -1) {
                    return;
                }
                int iHashCode = action.hashCode();
                if (iHashCode != -2061058799) {
                    if (iHashCode == 1121780209 && action.equals("android.intent.action.USER_ADDED")) {
                        b = 1;
                    }
                } else if (action.equals("android.intent.action.USER_REMOVED")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                    case 1:
                        synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                            NetworkPolicyManagerService.this.removeUserStateUL(intExtra, true);
                            NetworkPolicyManagerService.this.mMeteredRestrictedUids.remove(intExtra);
                            if (action == "android.intent.action.USER_ADDED") {
                                NetworkPolicyManagerService.this.addDefaultRestrictBackgroundWhitelistUidsUL(intExtra);
                            }
                            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                                NetworkPolicyManagerService.this.updateRulesForGlobalChangeAL(true);
                                break;
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mStatsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                    NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                    NetworkPolicyManagerService.this.updateNotificationsNL();
                }
            }
        };
        this.mAllowReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.setRestrictBackground(false);
            }
        };
        this.mSnoozeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkTemplate parcelableExtra = intent.getParcelableExtra("android.net.NETWORK_TEMPLATE");
                if (NetworkPolicyManagerService.ACTION_SNOOZE_WARNING.equals(intent.getAction())) {
                    NetworkPolicyManagerService.this.performSnooze(parcelableExtra, 34);
                } else if (NetworkPolicyManagerService.ACTION_SNOOZE_RAPID.equals(intent.getAction())) {
                    NetworkPolicyManagerService.this.performSnooze(parcelableExtra, 45);
                }
            }
        };
        this.mWifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.upgradeWifiMeteredOverrideAL();
                    }
                }
                NetworkPolicyManagerService.this.mContext.unregisterReceiver(this);
            }
        };
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (network == null || networkCapabilities == null) {
                    return;
                }
                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                    boolean z2 = !networkCapabilities.hasCapability(11);
                    boolean zUpdateCapabilityChange = NetworkPolicyManagerService.updateCapabilityChange(NetworkPolicyManagerService.this.mNetworkMetered, z2, network);
                    boolean zUpdateCapabilityChange2 = NetworkPolicyManagerService.updateCapabilityChange(NetworkPolicyManagerService.this.mNetworkRoaming, !networkCapabilities.hasCapability(18), network);
                    if (zUpdateCapabilityChange || zUpdateCapabilityChange2) {
                        NetworkPolicyManagerService.this.mLogger.meterednessChanged(network.netId, z2);
                        NetworkPolicyManagerService.this.updateNetworkRulesNL();
                    }
                }
            }
        };
        this.mAlertObserver = new BaseNetworkObserver() {
            public void limitReached(String str, String str2) {
                NetworkPolicyManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkPolicyManagerService.TAG);
                if (!NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(str)) {
                    NetworkPolicyManagerService.this.mHandler.obtainMessage(5, str2).sendToTarget();
                }
            }
        };
        this.mConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.updateNetworksInternal();
            }
        };
        this.mCarrierConfigReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!intent.hasExtra("subscription")) {
                    return;
                }
                int intExtra = intent.getIntExtra("subscription", -1);
                NetworkPolicyManagerService.this.updateSubscriptions();
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        String str = (String) NetworkPolicyManagerService.this.mSubIdToSubscriberId.get(intExtra, null);
                        if (str != null) {
                            NetworkPolicyManagerService.this.ensureActiveMobilePolicyAL(intExtra, str);
                            NetworkPolicyManagerService.this.maybeUpdateMobilePolicyCycleAL(intExtra, str);
                        } else {
                            Slog.e(NetworkPolicyManagerService.TAG, "Missing subscriberId for subId " + intExtra);
                        }
                        NetworkPolicyManagerService.this.handleNetworkPoliciesUpdateAL(true);
                    }
                }
            }
        };
        this.mHandlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                if (NetworkPolicyManagerService.LOGV) {
                    Log.v(NetworkPolicyManagerService.TAG, "handleMessage(): msg=" + message.what);
                }
                switch (message.what) {
                    case 1:
                        int i = message.arg1;
                        int i2 = message.arg2;
                        int iBeginBroadcast = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i3 = 0; i3 < iBeginBroadcast; i3++) {
                            NetworkPolicyManagerService.this.dispatchUidRulesChanged(NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i3), i, i2);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 2:
                        String[] strArr = (String[]) message.obj;
                        int iBeginBroadcast2 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i4 = 0; i4 < iBeginBroadcast2; i4++) {
                            NetworkPolicyManagerService.this.dispatchMeteredIfacesChanged(NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i4), strArr);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 3:
                    case 4:
                    case 8:
                    case 9:
                    case 12:
                    case 14:
                    default:
                        return false;
                    case 5:
                        String str = (String) message.obj;
                        synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                            if (NetworkPolicyManagerService.this.mMeteredIfaces.contains(str)) {
                                NetworkPolicyManagerService.this.mNetworkStats.forceUpdate();
                                NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                                NetworkPolicyManagerService.this.updateNotificationsNL();
                            }
                            break;
                        }
                        return true;
                    case 6:
                        boolean z2 = message.arg1 != 0;
                        int iBeginBroadcast3 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i5 = 0; i5 < iBeginBroadcast3; i5++) {
                            NetworkPolicyManagerService.this.dispatchRestrictBackgroundChanged(NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i5), z2);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                        intent.setFlags(1073741824);
                        NetworkPolicyManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                        return true;
                    case 7:
                        NetworkPolicyManagerService.this.mNetworkStats.advisePersistThreshold(((Long) message.obj).longValue() / 1000);
                        return true;
                    case 10:
                        NetworkPolicyManagerService.this.setInterfaceQuota((String) message.obj, (((long) message.arg1) << 32) | (((long) message.arg2) & 4294967295L));
                        return true;
                    case 11:
                        NetworkPolicyManagerService.this.removeInterfaceQuota((String) message.obj);
                        return true;
                    case 13:
                        int i6 = message.arg1;
                        int i7 = message.arg2;
                        Boolean bool = (Boolean) message.obj;
                        int iBeginBroadcast4 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i8 = 0; i8 < iBeginBroadcast4; i8++) {
                            NetworkPolicyManagerService.this.dispatchUidPoliciesChanged(NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i8), i6, i7);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        if (bool.booleanValue()) {
                            NetworkPolicyManagerService.this.broadcastRestrictBackgroundChanged(i6, bool);
                        }
                        return true;
                    case 15:
                        NetworkPolicyManagerService.this.resetUidFirewallRules(message.arg1);
                        return true;
                    case 16:
                        int i9 = message.arg1;
                        int i10 = message.arg2;
                        int iIntValue = ((Integer) message.obj).intValue();
                        int iBeginBroadcast5 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i11 = 0; i11 < iBeginBroadcast5; i11++) {
                            NetworkPolicyManagerService.this.dispatchSubscriptionOverride(NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i11), iIntValue, i9, i10);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 17:
                        NetworkPolicyManagerService.this.setMeteredRestrictedPackagesInternal((Set) message.obj, message.arg1);
                        return true;
                    case 18:
                        NetworkPolicyManagerService.this.setNetworkTemplateEnabledInner((NetworkTemplate) message.obj, message.arg1 != 0);
                        return true;
                }
            }
        };
        this.mUidEventHandlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case 100:
                        NetworkPolicyManagerService.this.handleUidChanged(message.arg1, message.arg2, ((Long) message.obj).longValue());
                        break;
                    case 101:
                        NetworkPolicyManagerService.this.handleUidGone(message.arg1);
                        break;
                }
                return true;
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing context");
        this.mActivityManager = (IActivityManager) Preconditions.checkNotNull(iActivityManager, "missing activityManager");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(iNetworkManagementService, "missing networkManagement");
        this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        this.mClock = (Clock) Preconditions.checkNotNull(clock, "missing Clock");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mCarrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService(CarrierConfigManager.class);
        this.mIPm = iPackageManager;
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper(), this.mHandlerCallback);
        this.mUidEventThread = new ServiceThread("NetworkPolicy.uid", -2, false);
        this.mUidEventThread.start();
        this.mUidEventHandler = new Handler(this.mUidEventThread.getLooper(), this.mUidEventHandlerCallback);
        this.mSuppressDefaultPolicy = z;
        this.mPolicyFile = new AtomicFile(new File(file, "netpolicy.xml"), "net-policy");
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        LocalServices.addService(NetworkPolicyManagerInternal.class, new NetworkPolicyManagerInternalImpl());
    }

    public void bindConnectivityManager(IConnectivityManager iConnectivityManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(iConnectivityManager, "missing IConnectivityManager");
    }

    void updatePowerSaveWhitelistUL() {
        try {
            int[] appIdWhitelistExceptIdle = this.mDeviceIdleController.getAppIdWhitelistExceptIdle();
            this.mPowerSaveWhitelistExceptIdleAppIds.clear();
            if (appIdWhitelistExceptIdle != null) {
                for (int i : appIdWhitelistExceptIdle) {
                    this.mPowerSaveWhitelistExceptIdleAppIds.put(i, true);
                }
            }
            int[] appIdWhitelist = this.mDeviceIdleController.getAppIdWhitelist();
            this.mPowerSaveWhitelistAppIds.clear();
            if (appIdWhitelist != null) {
                for (int i2 : appIdWhitelist) {
                    this.mPowerSaveWhitelistAppIds.put(i2, true);
                }
            }
        } catch (RemoteException e) {
        }
    }

    boolean addDefaultRestrictBackgroundWhitelistUidsUL() {
        List users = this.mUserManager.getUsers();
        int size = users.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            z = addDefaultRestrictBackgroundWhitelistUidsUL(((UserInfo) users.get(i)).id) || z;
        }
        return z;
    }

    private boolean addDefaultRestrictBackgroundWhitelistUidsUL(int i) {
        SystemConfig systemConfig = SystemConfig.getInstance();
        PackageManager packageManager = this.mContext.getPackageManager();
        ArraySet allowInDataUsageSave = systemConfig.getAllowInDataUsageSave();
        boolean z = false;
        for (int i2 = 0; i2 < allowInDataUsageSave.size(); i2++) {
            String str = (String) allowInDataUsageSave.valueAt(i2);
            if (LOGD) {
                Slog.d(TAG, "checking restricted background whitelisting for package " + str + " and user " + i);
            }
            try {
                ApplicationInfo applicationInfoAsUser = packageManager.getApplicationInfoAsUser(str, DumpState.DUMP_DEXOPT, i);
                if (!applicationInfoAsUser.isPrivilegedApp()) {
                    Slog.e(TAG, "addDefaultRestrictBackgroundWhitelistUidsUL(): skipping non-privileged app  " + str);
                } else {
                    int uid = UserHandle.getUid(i, applicationInfoAsUser.uid);
                    this.mDefaultRestrictBackgroundWhitelistUids.append(uid, true);
                    if (LOGD) {
                        Slog.d(TAG, "Adding uid " + uid + " (user " + i + ") to default restricted background whitelist. Revoked status: " + this.mRestrictBackgroundWhitelistRevokedUids.get(uid));
                    }
                    if (!this.mRestrictBackgroundWhitelistRevokedUids.get(uid)) {
                        if (LOGD) {
                            Slog.d(TAG, "adding default package " + str + " (uid " + uid + " for user " + i + ") to restrict background whitelist");
                        }
                        setUidPolicyUncheckedUL(uid, 4, false);
                        z = true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                if (LOGD) {
                    Slog.d(TAG, "No ApplicationInfo for package " + str);
                }
            }
        }
        return z;
    }

    private void initService(CountDownLatch countDownLatch) {
        Trace.traceBegin(2097152L, "systemReady");
        int threadPriority = Process.getThreadPriority(Process.myTid());
        try {
            Process.setThreadPriority(-2);
            if (!isBandwidthControlEnabled()) {
                Slog.w(TAG, "bandwidth controls disabled, unable to enforce policy");
                return;
            }
            this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
            this.mNetworkStats = (NetworkStatsManagerInternal) LocalServices.getService(NetworkStatsManagerInternal.class);
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    updatePowerSaveWhitelistUL();
                    this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                    this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                        public int getServiceType() {
                            return 6;
                        }

                        public void onLowPowerModeChanged(PowerSaveState powerSaveState) {
                            boolean z = powerSaveState.batterySaverEnabled;
                            if (NetworkPolicyManagerService.LOGD) {
                                Slog.d(NetworkPolicyManagerService.TAG, "onLowPowerModeChanged(" + z + ")");
                            }
                            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                if (NetworkPolicyManagerService.this.mRestrictPower != z) {
                                    NetworkPolicyManagerService.this.mRestrictPower = z;
                                    NetworkPolicyManagerService.this.updateRulesForRestrictPowerUL();
                                }
                            }
                        }
                    });
                    this.mRestrictPower = this.mPowerManagerInternal.getLowPowerState(6).batterySaverEnabled;
                    this.mSystemReady = true;
                    waitForAdminData();
                    readPolicyAL();
                    this.mRestrictBackgroundBeforeBsm = this.mLoadedRestrictBackground;
                    this.mRestrictBackgroundPowerState = this.mPowerManagerInternal.getLowPowerState(10);
                    if (this.mRestrictBackgroundPowerState.batterySaverEnabled && !this.mLoadedRestrictBackground) {
                        this.mLoadedRestrictBackground = true;
                    }
                    this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                        public int getServiceType() {
                            return 10;
                        }

                        public void onLowPowerModeChanged(PowerSaveState powerSaveState) {
                            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                NetworkPolicyManagerService.this.updateRestrictBackgroundByLowPowerModeUL(powerSaveState);
                            }
                        }
                    });
                    if (addDefaultRestrictBackgroundWhitelistUidsUL()) {
                        writePolicyAL();
                    }
                    setRestrictBackgroundUL(this.mLoadedRestrictBackground);
                    updateRulesForGlobalChangeAL(false);
                    updateNotificationsNL();
                }
            }
            this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            try {
                this.mActivityManager.registerUidObserver(this.mUidObserver, 3, -1, (String) null);
                this.mNetworkManager.registerObserver(this.mAlertObserver);
            } catch (RemoteException e) {
            }
            this.mContext.registerReceiver(this.mPowerSaveWhitelistReceiver, new IntentFilter("android.os.action.POWER_SAVE_WHITELIST_CHANGED"), null, this.mHandler);
            this.mContext.registerReceiver(this.mConnReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"), "android.permission.CONNECTIVITY_INTERNAL", this.mHandler);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
            this.mContext.registerReceiver(this.mPackageReceiver, intentFilter, null, this.mHandler);
            this.mContext.registerReceiver(this.mUidRemovedReceiver, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.USER_ADDED");
            intentFilter2.addAction("android.intent.action.USER_REMOVED");
            this.mContext.registerReceiver(this.mUserReceiver, intentFilter2, null, this.mHandler);
            this.mContext.registerReceiver(this.mStatsReceiver, new IntentFilter(NetworkStatsService.ACTION_NETWORK_STATS_UPDATED), "android.permission.READ_NETWORK_USAGE_HISTORY", this.mHandler);
            this.mContext.registerReceiver(this.mAllowReceiver, new IntentFilter(ACTION_ALLOW_BACKGROUND), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
            this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter(ACTION_SNOOZE_WARNING), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
            this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter(ACTION_SNOOZE_RAPID), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
            this.mContext.registerReceiver(this.mWifiReceiver, new IntentFilter("android.net.wifi.CONFIGURED_NETWORKS_CHANGE"), null, this.mHandler);
            this.mContext.registerReceiver(this.mCarrierConfigReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"), null, this.mHandler);
            ((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class)).registerNetworkCallback(new NetworkRequest.Builder().build(), this.mNetworkCallback);
            this.mUsageStats.addAppIdleStateChangeListener(new AppIdleStateChangeListener());
            ((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener(this.mHandler.getLooper()) {
                @Override
                public void onSubscriptionsChanged() {
                    NetworkPolicyManagerService.this.updateNetworksInternal();
                }
            });
            countDownLatch.countDown();
        } finally {
            Process.setThreadPriority(threadPriority);
            Trace.traceEnd(2097152L);
        }
    }

    public CountDownLatch networkScoreAndNetworkManagementServiceReady() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.initService(countDownLatch);
            }
        });
        return countDownLatch;
    }

    public void systemReady(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(30L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Service NetworkPolicy init timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Service NetworkPolicy init interrupted", e);
        }
    }

    private static boolean updateCapabilityChange(SparseBooleanArray sparseBooleanArray, boolean z, Network network) {
        boolean z2 = sparseBooleanArray.get(network.netId, false) != z || sparseBooleanArray.indexOfKey(network.netId) < 0;
        if (z2) {
            sparseBooleanArray.put(network.netId, z);
        }
        return z2;
    }

    void updateNotificationsNL() {
        long j;
        if (LOGV) {
            Slog.v(TAG, "updateNotificationsNL()");
        }
        Trace.traceBegin(2097152L, "updateNotificationsNL");
        ArraySet arraySet = new ArraySet((ArraySet) this.mActiveNotifs);
        this.mActiveNotifs.clear();
        long jMillis = this.mClock.millis();
        for (int size = this.mNetworkPolicy.size() - 1; size >= 0; size--) {
            NetworkPolicy networkPolicyValueAt = this.mNetworkPolicy.valueAt(size);
            int iFindRelevantSubIdNL = findRelevantSubIdNL(networkPolicyValueAt.template);
            if (iFindRelevantSubIdNL != -1 && networkPolicyValueAt.hasCycle()) {
                Pair pair = (Pair) NetworkPolicyManager.cycleIterator(networkPolicyValueAt).next();
                long epochMilli = ((ZonedDateTime) pair.first).toInstant().toEpochMilli();
                long epochMilli2 = ((ZonedDateTime) pair.second).toInstant().toEpochMilli();
                long totalBytes = getTotalBytes(networkPolicyValueAt.template, epochMilli, epochMilli2);
                PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(iFindRelevantSubIdNL);
                boolean booleanDefeatingNullable = getBooleanDefeatingNullable(configForSubId, "data_warning_notification_bool", true);
                boolean booleanDefeatingNullable2 = getBooleanDefeatingNullable(configForSubId, "data_limit_notification_bool", true);
                boolean booleanDefeatingNullable3 = getBooleanDefeatingNullable(configForSubId, "data_rapid_notification_bool", true);
                boolean z = false;
                if (booleanDefeatingNullable && networkPolicyValueAt.isOverWarning(totalBytes) && !networkPolicyValueAt.isOverLimit(totalBytes)) {
                    if (!(networkPolicyValueAt.lastWarningSnooze >= epochMilli)) {
                        j = totalBytes;
                        enqueueNotification(networkPolicyValueAt, 34, totalBytes, null);
                    }
                    if (booleanDefeatingNullable2) {
                    }
                    if (!booleanDefeatingNullable3) {
                    }
                } else {
                    j = totalBytes;
                    if (booleanDefeatingNullable2) {
                        long j2 = j;
                        if (!networkPolicyValueAt.isOverLimit(j2)) {
                            notifyUnderLimitNL(networkPolicyValueAt.template);
                        } else if (networkPolicyValueAt.lastLimitSnooze >= epochMilli) {
                            enqueueNotification(networkPolicyValueAt, 36, j2, null);
                        } else {
                            enqueueNotification(networkPolicyValueAt, 35, j2, null);
                            notifyOverLimitNL(networkPolicyValueAt.template);
                        }
                    }
                    if (!booleanDefeatingNullable3 && networkPolicyValueAt.limitBytes != -1) {
                        long millis = TimeUnit.DAYS.toMillis(4L);
                        long j3 = jMillis - millis;
                        long totalBytes2 = getTotalBytes(networkPolicyValueAt.template, j3, jMillis);
                        long j4 = ((epochMilli2 - epochMilli) * totalBytes2) / millis;
                        long j5 = (networkPolicyValueAt.limitBytes * 3) / 2;
                        if (LOGD) {
                            Slog.d(TAG, "Rapid usage considering recent " + totalBytes2 + " projected " + j4 + " alert " + j5);
                        }
                        if (networkPolicyValueAt.lastRapidSnooze >= jMillis - 86400000) {
                            z = true;
                        }
                        if (j4 > j5 && !z) {
                            enqueueNotification(networkPolicyValueAt, 45, 0L, findRapidBlame(networkPolicyValueAt.template, j3, jMillis));
                        }
                    }
                }
            }
        }
        for (int size2 = arraySet.size() - 1; size2 >= 0; size2--) {
            NotificationId notificationId = (NotificationId) arraySet.valueAt(size2);
            if (!this.mActiveNotifs.contains(notificationId)) {
                cancelNotification(notificationId);
            }
        }
        Trace.traceEnd(2097152L);
    }

    private ApplicationInfo findRapidBlame(NetworkTemplate networkTemplate, long j, long j2) {
        String[] packagesForUid;
        NetworkStats networkUidBytes = getNetworkUidBytes(networkTemplate, j, j2);
        long j3 = 0;
        long j4 = 0;
        NetworkStats.Entry values = null;
        int i = 0;
        for (int i2 = 0; i2 < networkUidBytes.size(); i2++) {
            values = networkUidBytes.getValues(i2, values);
            long j5 = values.rxBytes + values.txBytes;
            j3 += j5;
            if (j5 > j4) {
                i = values.uid;
                j4 = j5;
            }
        }
        if (j4 > 0 && j4 > j3 / 2 && (packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i)) != null && packagesForUid.length == 1) {
            try {
                return this.mContext.getPackageManager().getApplicationInfo(packagesForUid[0], 4989440);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private int findRelevantSubIdNL(NetworkTemplate networkTemplate) {
        for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
            int iKeyAt = this.mSubIdToSubscriberId.keyAt(i);
            if (networkTemplate.matches(new NetworkIdentity(0, 0, this.mSubIdToSubscriberId.valueAt(i), (String) null, false, true, true))) {
                return iKeyAt;
            }
        }
        return -1;
    }

    private void notifyOverLimitNL(NetworkTemplate networkTemplate) {
        if (!this.mOverLimitNotified.contains(networkTemplate)) {
            this.mContext.startActivityAsUser(buildNetworkOverLimitIntent(this.mContext.getResources(), networkTemplate), UserHandle.CURRENT);
            this.mOverLimitNotified.add(networkTemplate);
        }
    }

    private void notifyUnderLimitNL(NetworkTemplate networkTemplate) {
        this.mOverLimitNotified.remove(networkTemplate);
    }

    private void enqueueNotification(NetworkPolicy networkPolicy, int i, long j, ApplicationInfo applicationInfo) {
        CharSequence text;
        CharSequence string;
        NotificationId notificationId = new NotificationId(networkPolicy, i);
        Notification.Builder builder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_ALERTS);
        builder.setOnlyAlertOnce(true);
        builder.setWhen(0L);
        builder.setColor(this.mContext.getColor(R.color.car_colorPrimary));
        Resources resources = this.mContext.getResources();
        if (i != 45) {
            switch (i) {
                case 34:
                    text = resources.getText(R.string.autofill_save_accessibility_title);
                    string = resources.getString(R.string.autofill_picker_some_suggestions, Formatter.formatFileSize(this.mContext, j));
                    builder.setSmallIcon(R.drawable.stat_notify_error);
                    builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, buildSnoozeWarningIntent(networkPolicy.template), 134217728));
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, buildViewDataUsageIntent(resources, networkPolicy.template), 134217728, null, UserHandle.CURRENT));
                    break;
                case 35:
                    int matchRule = networkPolicy.template.getMatchRule();
                    if (matchRule == 1) {
                        text = resources.getText(R.string.auto_data_switch_title);
                    } else if (matchRule == 4) {
                        text = resources.getText(R.string.autofill_save_no);
                    } else {
                        return;
                    }
                    string = resources.getText(R.string.app_upgrading_toast);
                    builder.setOngoing(true);
                    builder.setSmallIcon(R.drawable.pointer_grab_vector_icon);
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, buildNetworkOverLimitIntent(resources, networkPolicy.template), 134217728, null, UserHandle.CURRENT));
                    break;
                case 36:
                    int matchRule2 = networkPolicy.template.getMatchRule();
                    if (matchRule2 == 1) {
                        text = resources.getText(R.string.auto_data_switch_content);
                    } else if (matchRule2 == 4) {
                        text = resources.getText(R.string.autofill_save_never);
                    } else {
                        return;
                    }
                    string = resources.getString(R.string.as_app_forced_to_restricted_bucket, Formatter.formatFileSize(this.mContext, j - networkPolicy.limitBytes));
                    builder.setOngoing(true);
                    builder.setSmallIcon(R.drawable.stat_notify_error);
                    builder.setChannelId(SystemNotificationChannels.NETWORK_STATUS);
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, buildViewDataUsageIntent(resources, networkPolicy.template), 134217728, null, UserHandle.CURRENT));
                    break;
                default:
                    return;
            }
        } else {
            text = resources.getText(R.string.autofill_error_cannot_autofill);
            if (applicationInfo != null) {
                string = resources.getString(R.string.autoclick_feature_name, applicationInfo.loadLabel(this.mContext.getPackageManager()));
            } else {
                string = resources.getString(R.string.autofill_continue_yes);
            }
            builder.setSmallIcon(R.drawable.stat_notify_error);
            builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, buildSnoozeRapidIntent(networkPolicy.template), 134217728));
            builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, buildViewDataUsageIntent(resources, networkPolicy.template), 134217728));
        }
        builder.setTicker(text);
        builder.setContentTitle(text);
        builder.setContentText(string);
        builder.setStyle(new Notification.BigTextStyle().bigText(string));
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(notificationId.getTag(), notificationId.getId(), builder.build(), UserHandle.ALL);
        this.mActiveNotifs.add(notificationId);
    }

    private void cancelNotification(NotificationId notificationId) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(notificationId.getTag(), notificationId.getId());
    }

    private void updateNetworksInternal() {
        updateSubscriptions();
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                ensureActiveMobilePolicyAL();
                normalizePoliciesNL();
                updateNetworkEnabledNL();
                updateNetworkRulesNL();
                updateNotificationsNL();
            }
        }
    }

    @VisibleForTesting
    public void updateNetworks() throws InterruptedException {
        updateNetworksInternal();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await(5L, TimeUnit.SECONDS);
    }

    private boolean maybeUpdateMobilePolicyCycleAL(int i, String str) {
        if (LOGV) {
            Slog.v(TAG, "maybeUpdateMobilePolicyCycleAL()");
        }
        boolean zUpdateDefaultMobilePolicyAL = false;
        NetworkIdentity networkIdentity = new NetworkIdentity(0, 0, str, (String) null, false, true, true);
        for (int size = this.mNetworkPolicy.size() - 1; size >= 0; size--) {
            if (this.mNetworkPolicy.keyAt(size).matches(networkIdentity)) {
                zUpdateDefaultMobilePolicyAL |= updateDefaultMobilePolicyAL(i, this.mNetworkPolicy.valueAt(size));
            }
        }
        return zUpdateDefaultMobilePolicyAL;
    }

    @VisibleForTesting
    public int getCycleDayFromCarrierConfig(PersistableBundle persistableBundle, int i) {
        int i2;
        if (persistableBundle == null || (i2 = persistableBundle.getInt("monthly_data_cycle_day_int")) == -1) {
            return i;
        }
        Calendar calendar = Calendar.getInstance();
        if (i2 < calendar.getMinimum(5) || i2 > calendar.getMaximum(5)) {
            Slog.e(TAG, "Invalid date in CarrierConfigManager.KEY_MONTHLY_DATA_CYCLE_DAY_INT: " + i2);
            return i;
        }
        return i2;
    }

    @VisibleForTesting
    public long getWarningBytesFromCarrierConfig(PersistableBundle persistableBundle, long j) {
        if (persistableBundle == null) {
            return j;
        }
        long j2 = persistableBundle.getLong("data_warning_threshold_bytes_long");
        if (j2 == -2) {
            return -1L;
        }
        if (j2 == -1) {
            return getPlatformDefaultWarningBytes();
        }
        if (j2 < 0) {
            Slog.e(TAG, "Invalid value in CarrierConfigManager.KEY_DATA_WARNING_THRESHOLD_BYTES_LONG; expected a non-negative value but got: " + j2);
            return j;
        }
        return j2;
    }

    @VisibleForTesting
    public long getLimitBytesFromCarrierConfig(PersistableBundle persistableBundle, long j) {
        if (persistableBundle == null) {
            return j;
        }
        long j2 = persistableBundle.getLong("data_limit_threshold_bytes_long");
        if (j2 == -2) {
            return -1L;
        }
        if (j2 == -1) {
            return getPlatformDefaultLimitBytes();
        }
        if (j2 < 0) {
            Slog.e(TAG, "Invalid value in CarrierConfigManager.KEY_DATA_LIMIT_THRESHOLD_BYTES_LONG; expected a non-negative value but got: " + j2);
            return j;
        }
        return j2;
    }

    void handleNetworkPoliciesUpdateAL(boolean z) {
        if (z) {
            normalizePoliciesNL();
        }
        updateNetworkEnabledNL();
        updateNetworkRulesNL();
        updateNotificationsNL();
        writePolicyAL();
    }

    void updateNetworkEnabledNL() {
        if (LOGV) {
            Slog.v(TAG, "updateNetworkEnabledNL()");
        }
        Trace.traceBegin(2097152L, "updateNetworkEnabledNL");
        long time = this.mStatLogger.getTime();
        int size = this.mNetworkPolicy.size() - 1;
        while (true) {
            boolean z = false;
            if (size >= 0) {
                NetworkPolicy networkPolicyValueAt = this.mNetworkPolicy.valueAt(size);
                if (networkPolicyValueAt.limitBytes == -1 || !networkPolicyValueAt.hasCycle()) {
                    setNetworkTemplateEnabled(networkPolicyValueAt.template, true);
                } else {
                    Pair pair = (Pair) NetworkPolicyManager.cycleIterator(networkPolicyValueAt).next();
                    long epochMilli = ((ZonedDateTime) pair.first).toInstant().toEpochMilli();
                    if (networkPolicyValueAt.isOverLimit(getTotalBytes(networkPolicyValueAt.template, epochMilli, ((ZonedDateTime) pair.second).toInstant().toEpochMilli())) && networkPolicyValueAt.lastLimitSnooze < epochMilli) {
                        z = true;
                    }
                    setNetworkTemplateEnabled(networkPolicyValueAt.template, !z);
                }
                size--;
            } else {
                this.mStatLogger.logDurationStat(0, time);
                Trace.traceEnd(2097152L);
                return;
            }
        }
    }

    private void setNetworkTemplateEnabled(NetworkTemplate networkTemplate, boolean z) {
        this.mHandler.obtainMessage(18, z ? 1 : 0, 0, networkTemplate).sendToTarget();
    }

    private void setNetworkTemplateEnabledInner(NetworkTemplate networkTemplate, boolean z) {
        int i;
        if (networkTemplate.getMatchRule() == 1) {
            IntArray intArray = new IntArray();
            synchronized (this.mNetworkPoliciesSecondLock) {
                for (int i2 = 0; i2 < this.mSubIdToSubscriberId.size(); i2++) {
                    int iKeyAt = this.mSubIdToSubscriberId.keyAt(i2);
                    if (networkTemplate.matches(new NetworkIdentity(0, 0, this.mSubIdToSubscriberId.valueAt(i2), (String) null, false, true, true))) {
                        intArray.add(iKeyAt);
                    }
                }
            }
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            for (i = 0; i < intArray.size(); i++) {
                telephonyManager.setPolicyDataEnabled(z, intArray.get(i));
            }
        }
    }

    private static void collectIfaces(ArraySet<String> arraySet, NetworkState networkState) {
        String interfaceName = networkState.linkProperties.getInterfaceName();
        if (interfaceName != null) {
            arraySet.add(interfaceName);
        }
        Iterator it = networkState.linkProperties.getStackedLinks().iterator();
        while (it.hasNext()) {
            String interfaceName2 = ((LinkProperties) it.next()).getInterfaceName();
            if (interfaceName2 != null) {
                arraySet.add(interfaceName2);
            }
        }
    }

    void updateSubscriptions() {
        if (LOGV) {
            Slog.v(TAG, "updateSubscriptions()");
        }
        Trace.traceBegin(2097152L, "updateSubscriptions");
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        int[] iArrDefeatNullable = ArrayUtils.defeatNullable(((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).getActiveSubscriptionIdList());
        String[] strArrDefeatNullable = ArrayUtils.defeatNullable(telephonyManager.getMergedSubscriberIds());
        SparseArray sparseArray = new SparseArray(iArrDefeatNullable.length);
        for (int i : iArrDefeatNullable) {
            String subscriberId = telephonyManager.getSubscriberId(i);
            if (!TextUtils.isEmpty(subscriberId)) {
                sparseArray.put(i, subscriberId);
            } else {
                Slog.e(TAG, "Missing subscriberId for subId " + i);
            }
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            this.mSubIdToSubscriberId.clear();
            for (int i2 = 0; i2 < sparseArray.size(); i2++) {
                this.mSubIdToSubscriberId.put(sparseArray.keyAt(i2), (String) sparseArray.valueAt(i2));
            }
            this.mMergedSubscriberIds = strArrDefeatNullable;
        }
        Trace.traceEnd(2097152L);
    }

    void updateNetworkRulesNL() {
        long j;
        NetworkState[] networkStateArr;
        int i;
        int i2;
        int i3;
        boolean z;
        long j2;
        long j3;
        long j4;
        long jMax;
        NetworkPolicy networkPolicy;
        long j5;
        long jMax2;
        long j6;
        if (LOGV) {
            Slog.v(TAG, "updateNetworkRulesNL()");
        }
        Trace.traceBegin(2097152L, "updateNetworkRulesNL");
        try {
            NetworkState[] networkStateArrDefeatNullable = defeatNullable(this.mConnManager.getAllNetworkState());
            this.mNetIdToSubId.clear();
            ArrayMap arrayMap = new ArrayMap();
            int length = networkStateArrDefeatNullable.length;
            int i4 = 0;
            while (true) {
                if (i4 >= length) {
                    break;
                }
                NetworkState networkState = networkStateArrDefeatNullable[i4];
                if (networkState.network != null) {
                    this.mNetIdToSubId.put(networkState.network.netId, parseSubId(networkState));
                }
                if (networkState.networkInfo != null && networkState.networkInfo.isConnected()) {
                    arrayMap.put(networkState, NetworkIdentity.buildNetworkIdentity(this.mContext, networkState, true));
                }
                i4++;
            }
            ArraySet<String> arraySet = new ArraySet<>();
            ArraySet arraySet2 = new ArraySet();
            long j7 = JobStatus.NO_LATEST_RUNTIME;
            for (int size = this.mNetworkPolicy.size() - 1; size >= 0; size--) {
                NetworkPolicy networkPolicyValueAt = this.mNetworkPolicy.valueAt(size);
                arraySet2.clear();
                for (int size2 = arrayMap.size() - 1; size2 >= 0; size2--) {
                    if (networkPolicyValueAt.template.matches((NetworkIdentity) arrayMap.valueAt(size2))) {
                        collectIfaces(arraySet2, (NetworkState) arrayMap.keyAt(size2));
                    }
                }
                if (LOGD) {
                    Slog.d(TAG, "Applying " + networkPolicyValueAt + " to ifaces " + arraySet2);
                }
                boolean z2 = networkPolicyValueAt.warningBytes != -1;
                boolean z3 = networkPolicyValueAt.limitBytes != -1;
                if (z3 || networkPolicyValueAt.metered) {
                    if (z3 && networkPolicyValueAt.hasCycle()) {
                        Pair pair = (Pair) NetworkPolicyManager.cycleIterator(networkPolicyValueAt).next();
                        long epochMilli = ((ZonedDateTime) pair.first).toInstant().toEpochMilli();
                        networkPolicy = networkPolicyValueAt;
                        j5 = j7;
                        jMax2 = networkPolicy.lastLimitSnooze >= epochMilli ? JobStatus.NO_LATEST_RUNTIME : Math.max(1L, networkPolicy.limitBytes - getTotalBytes(networkPolicyValueAt.template, epochMilli, ((ZonedDateTime) pair.second).toInstant().toEpochMilli()));
                    } else {
                        networkPolicy = networkPolicyValueAt;
                        j5 = j7;
                        jMax2 = JobStatus.NO_LATEST_RUNTIME;
                    }
                    if (arraySet2.size() > 1) {
                        Slog.w(TAG, "shared quota unsupported; generating rule for each iface");
                    }
                    for (int size3 = arraySet2.size() - 1; size3 >= 0; size3--) {
                        String str = (String) arraySet2.valueAt(size3);
                        setInterfaceQuotaAsync(str, jMax2);
                        arraySet.add(str);
                    }
                } else {
                    networkPolicy = networkPolicyValueAt;
                    j5 = j7;
                }
                if (z2) {
                    j6 = j5;
                    if (networkPolicy.warningBytes < j6) {
                        j7 = networkPolicy.warningBytes;
                    }
                    if (!z3 && networkPolicy.limitBytes < j7) {
                        j7 = networkPolicy.limitBytes;
                    }
                } else {
                    j6 = j5;
                }
                j7 = j6;
                if (!z3) {
                }
            }
            long j8 = j7;
            long j9 = 1;
            for (NetworkState networkState2 : networkStateArrDefeatNullable) {
                if (networkState2.networkInfo != null && networkState2.networkInfo.isConnected() && !networkState2.networkCapabilities.hasCapability(11)) {
                    arraySet2.clear();
                    collectIfaces(arraySet2, networkState2);
                    for (int size4 = arraySet2.size() - 1; size4 >= 0; size4--) {
                        String str2 = (String) arraySet2.valueAt(size4);
                        if (!arraySet.contains(str2)) {
                            setInterfaceQuotaAsync(str2, JobStatus.NO_LATEST_RUNTIME);
                            arraySet.add(str2);
                        }
                    }
                }
            }
            long j10 = JobStatus.NO_LATEST_RUNTIME;
            for (int size5 = this.mMeteredIfaces.size() - 1; size5 >= 0; size5--) {
                String strValueAt = this.mMeteredIfaces.valueAt(size5);
                if (!arraySet.contains(strValueAt)) {
                    removeInterfaceQuotaAsync(strValueAt);
                }
            }
            this.mMeteredIfaces = arraySet;
            ContentResolver contentResolver = this.mContext.getContentResolver();
            boolean z4 = Settings.Global.getInt(contentResolver, "netpolicy_quota_enabled", 1) != 0;
            long j11 = Settings.Global.getLong(contentResolver, "netpolicy_quota_unlimited", QUOTA_UNLIMITED_DEFAULT);
            float f = Settings.Global.getFloat(contentResolver, "netpolicy_quota_limited", QUOTA_LIMITED_DEFAULT);
            this.mSubscriptionOpportunisticQuota.clear();
            int length2 = networkStateArrDefeatNullable.length;
            int i5 = 0;
            while (i5 < length2) {
                NetworkState networkState3 = networkStateArrDefeatNullable[i5];
                if (z4 && networkState3.network != null) {
                    int subIdLocked = getSubIdLocked(networkState3.network);
                    SubscriptionPlan primarySubscriptionPlanLocked = getPrimarySubscriptionPlanLocked(subIdLocked);
                    if (primarySubscriptionPlanLocked == null) {
                        i2 = length2;
                        i3 = i5;
                        networkStateArr = networkStateArrDefeatNullable;
                        j = j10;
                        z = z4;
                        j2 = j11;
                        j4 = 1;
                    } else {
                        long dataLimitBytes = primarySubscriptionPlanLocked.getDataLimitBytes();
                        if (!networkState3.networkCapabilities.hasCapability(18)) {
                            i2 = length2;
                            i3 = i5;
                            networkStateArr = networkStateArrDefeatNullable;
                            z = z4;
                            j2 = j11;
                            jMax = 0;
                        } else if (dataLimitBytes == -1) {
                            i2 = length2;
                            i3 = i5;
                            networkStateArr = networkStateArrDefeatNullable;
                            z = z4;
                            j2 = j11;
                            jMax = -1;
                        } else {
                            j = JobStatus.NO_LATEST_RUNTIME;
                            if (dataLimitBytes == JobStatus.NO_LATEST_RUNTIME) {
                                i2 = length2;
                                i3 = i5;
                                networkStateArr = networkStateArrDefeatNullable;
                                z = z4;
                                jMax = j11;
                                j2 = jMax;
                                i = subIdLocked;
                                j3 = j8;
                                j4 = 1;
                                this.mSubscriptionOpportunisticQuota.put(i, jMax);
                                i5 = i3 + 1;
                                length2 = i2;
                                j8 = j3;
                                j10 = j;
                                networkStateArrDefeatNullable = networkStateArr;
                                z4 = z;
                                j11 = j2;
                                j9 = j4;
                            } else {
                                Range<ZonedDateTime> next = primarySubscriptionPlanLocked.cycleIterator().next();
                                long epochMilli2 = ((ZonedDateTime) next.getLower()).toInstant().toEpochMilli();
                                long epochMilli3 = ((ZonedDateTime) next.getUpper()).toInstant().toEpochMilli();
                                long epochMilli4 = ZonedDateTime.ofInstant(this.mClock.instant(), ((ZonedDateTime) next.getLower()).getZone()).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
                                networkStateArr = networkStateArrDefeatNullable;
                                i = subIdLocked;
                                i2 = length2;
                                i3 = i5;
                                z = z4;
                                j2 = j11;
                                j3 = j8;
                                j4 = 1;
                                jMax = Math.max(0L, (long) (((dataLimitBytes - getTotalBytes(NetworkTemplate.buildTemplateMobileAll(networkState3.subscriberId), epochMilli2, epochMilli4)) / (1 + (((epochMilli3 - r8.toEpochMilli()) - 1) / TimeUnit.DAYS.toMillis(1L)))) * f));
                                this.mSubscriptionOpportunisticQuota.put(i, jMax);
                                i5 = i3 + 1;
                                length2 = i2;
                                j8 = j3;
                                j10 = j;
                                networkStateArrDefeatNullable = networkStateArr;
                                z4 = z;
                                j11 = j2;
                                j9 = j4;
                            }
                        }
                        j = JobStatus.NO_LATEST_RUNTIME;
                        i = subIdLocked;
                        j3 = j8;
                        j4 = 1;
                        this.mSubscriptionOpportunisticQuota.put(i, jMax);
                        i5 = i3 + 1;
                        length2 = i2;
                        j8 = j3;
                        j10 = j;
                        networkStateArrDefeatNullable = networkStateArr;
                        z4 = z;
                        j11 = j2;
                        j9 = j4;
                    }
                } else {
                    i3 = i5;
                    j4 = j9;
                    networkStateArr = networkStateArrDefeatNullable;
                    j = j10;
                    z = z4;
                    j2 = j11;
                    i2 = length2;
                }
                j3 = j8;
                i5 = i3 + 1;
                length2 = i2;
                j8 = j3;
                j10 = j;
                networkStateArrDefeatNullable = networkStateArr;
                z4 = z;
                j11 = j2;
                j9 = j4;
            }
            this.mHandler.obtainMessage(2, (String[]) this.mMeteredIfaces.toArray(new String[this.mMeteredIfaces.size()])).sendToTarget();
            this.mHandler.obtainMessage(7, Long.valueOf(j8)).sendToTarget();
            Trace.traceEnd(2097152L);
        } catch (RemoteException e) {
        }
    }

    private void ensureActiveMobilePolicyAL() {
        if (LOGV) {
            Slog.v(TAG, "ensureActiveMobilePolicyAL()");
        }
        if (this.mSuppressDefaultPolicy) {
            return;
        }
        for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
            ensureActiveMobilePolicyAL(this.mSubIdToSubscriberId.keyAt(i), this.mSubIdToSubscriberId.valueAt(i));
        }
    }

    private boolean ensureActiveMobilePolicyAL(int i, String str) {
        if (str == null) {
            Slog.v(TAG, "skip ensureActiveMobilePolicyAL due to subscriberId = null");
            return false;
        }
        NetworkIdentity networkIdentity = new NetworkIdentity(0, 0, str, (String) null, false, true, true);
        for (int size = this.mNetworkPolicy.size() - 1; size >= 0; size--) {
            NetworkTemplate networkTemplateKeyAt = this.mNetworkPolicy.keyAt(size);
            if (networkTemplateKeyAt.matches(networkIdentity)) {
                if (LOGD) {
                    Slog.d(TAG, "Found template " + networkTemplateKeyAt + " which matches subscriber " + NetworkIdentity.scrubSubscriberId(str));
                }
                return false;
            }
        }
        Slog.i(TAG, "No policy for subscriber " + NetworkIdentity.scrubSubscriberId(str) + "; generating default policy");
        addNetworkPolicyAL(buildDefaultMobilePolicy(i, str));
        return true;
    }

    private long getPlatformDefaultWarningBytes() {
        long integer = this.mContext.getResources().getInteger(R.integer.config_defaultVibrationAmplitude);
        if (integer == -1) {
            return -1L;
        }
        return integer * 1048576;
    }

    private long getPlatformDefaultLimitBytes() {
        return -1L;
    }

    @VisibleForTesting
    public NetworkPolicy buildDefaultMobilePolicy(int i, String str) {
        NetworkPolicy networkPolicy = new NetworkPolicy(NetworkTemplate.buildTemplateMobileAll(str), NetworkPolicy.buildRule(ZonedDateTime.now().getDayOfMonth(), ZoneId.systemDefault()), getPlatformDefaultWarningBytes(), getPlatformDefaultLimitBytes(), -1L, -1L, true, true);
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                updateDefaultMobilePolicyAL(i, networkPolicy);
            }
        }
        return networkPolicy;
    }

    private boolean updateDefaultMobilePolicyAL(int i, NetworkPolicy networkPolicy) {
        int dayOfMonth;
        if (!networkPolicy.inferred) {
            if (LOGD) {
                Slog.d(TAG, "Ignoring user-defined policy " + networkPolicy);
            }
            return false;
        }
        NetworkPolicy networkPolicy2 = new NetworkPolicy(networkPolicy.template, networkPolicy.cycleRule, networkPolicy.warningBytes, networkPolicy.limitBytes, networkPolicy.lastWarningSnooze, networkPolicy.lastLimitSnooze, networkPolicy.metered, networkPolicy.inferred);
        SubscriptionPlan[] subscriptionPlanArr = this.mSubscriptionPlans.get(i);
        if (!ArrayUtils.isEmpty(subscriptionPlanArr)) {
            SubscriptionPlan subscriptionPlan = subscriptionPlanArr[0];
            networkPolicy.cycleRule = subscriptionPlan.getCycleRule();
            long dataLimitBytes = subscriptionPlan.getDataLimitBytes();
            if (dataLimitBytes != -1) {
                if (dataLimitBytes != JobStatus.NO_LATEST_RUNTIME) {
                    networkPolicy.warningBytes = (9 * dataLimitBytes) / 10;
                    switch (subscriptionPlan.getDataLimitBehavior()) {
                        case 0:
                        case 1:
                            networkPolicy.limitBytes = dataLimitBytes;
                            break;
                        default:
                            networkPolicy.limitBytes = -1L;
                            break;
                    }
                } else {
                    networkPolicy.warningBytes = -1L;
                    networkPolicy.limitBytes = -1L;
                }
            } else {
                networkPolicy.warningBytes = getPlatformDefaultWarningBytes();
                networkPolicy.limitBytes = getPlatformDefaultLimitBytes();
            }
        } else {
            PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(i);
            if (networkPolicy.cycleRule.isMonthly()) {
                dayOfMonth = networkPolicy.cycleRule.start.getDayOfMonth();
            } else {
                dayOfMonth = -1;
            }
            networkPolicy.cycleRule = NetworkPolicy.buildRule(getCycleDayFromCarrierConfig(configForSubId, dayOfMonth), ZoneId.systemDefault());
            networkPolicy.warningBytes = getWarningBytesFromCarrierConfig(configForSubId, networkPolicy.warningBytes);
            networkPolicy.limitBytes = getLimitBytesFromCarrierConfig(configForSubId, networkPolicy.limitBytes);
        }
        if (!networkPolicy.equals(networkPolicy2)) {
            Slog.d(TAG, "Updated " + networkPolicy2 + " to " + networkPolicy);
            return true;
        }
        return false;
    }

    private void readPolicyAL() throws Throwable {
        FileInputStream fileInputStreamOpenRead;
        long j;
        long longAttribute;
        NetworkTemplate networkTemplate;
        if (LOGV) {
            Slog.v(TAG, "readPolicyAL()");
        }
        this.mNetworkPolicy.clear();
        this.mSubscriptionPlans.clear();
        this.mSubscriptionPlansOwner.clear();
        this.mUidPolicy.clear();
        FileInputStream fileInputStream = null;
        try {
            try {
                fileInputStreamOpenRead = this.mPolicyFile.openRead();
            } catch (Throwable th) {
                th = th;
                fileInputStreamOpenRead = null;
            }
        } catch (FileNotFoundException e) {
        } catch (Exception e2) {
            e = e2;
        }
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
            SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
            int intAttribute = 1;
            boolean z = false;
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next == 1) {
                    break;
                }
                String name = xmlPullParserNewPullParser.getName();
                if (next == 2) {
                    if (TAG_POLICY_LIST.equals(name)) {
                        boolean z2 = this.mRestrictBackground;
                        intAttribute = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_VERSION);
                        this.mLoadedRestrictBackground = intAttribute >= 3 && XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_RESTRICT_BACKGROUND);
                    } else if (TAG_NETWORK_POLICY.equals(name)) {
                        int intAttribute2 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_NETWORK_TEMPLATE);
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_SUBSCRIBER_ID);
                        String attributeValue2 = intAttribute >= 9 ? xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NETWORK_ID) : null;
                        RecurrenceRule recurrenceRule = intAttribute >= 11 ? new RecurrenceRule(RecurrenceRule.convertZonedDateTime(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_START)), RecurrenceRule.convertZonedDateTime(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_END)), RecurrenceRule.convertPeriod(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_PERIOD))) : NetworkPolicy.buildRule(XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_DAY), ZoneId.of(intAttribute >= 6 ? xmlPullParserNewPullParser.getAttributeValue(null, ATTR_CYCLE_TIMEZONE) : "UTC"));
                        long longAttribute2 = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_WARNING_BYTES);
                        long longAttribute3 = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_LIMIT_BYTES);
                        if (intAttribute >= 5) {
                            longAttribute = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_LAST_LIMIT_SNOOZE);
                        } else if (intAttribute >= 2) {
                            longAttribute = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_LAST_SNOOZE);
                        } else {
                            j = -1;
                            boolean booleanAttribute = intAttribute < 4 ? XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_METERED) : intAttribute2 == 1;
                            long longAttribute4 = intAttribute < 5 ? XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_LAST_WARNING_SNOOZE) : -1L;
                            boolean booleanAttribute2 = intAttribute < 7 ? XmlUtils.readBooleanAttribute(xmlPullParserNewPullParser, ATTR_INFERRED) : false;
                            networkTemplate = new NetworkTemplate(intAttribute2, attributeValue, attributeValue2);
                            if (networkTemplate.isPersistable()) {
                                this.mNetworkPolicy.put(networkTemplate, new NetworkPolicy(networkTemplate, recurrenceRule, longAttribute2, longAttribute3, longAttribute4, j, booleanAttribute, booleanAttribute2));
                            }
                        }
                        j = longAttribute;
                        if (intAttribute < 4) {
                        }
                        if (intAttribute < 5) {
                        }
                        if (intAttribute < 7) {
                        }
                        networkTemplate = new NetworkTemplate(intAttribute2, attributeValue, attributeValue2);
                        if (networkTemplate.isPersistable()) {
                        }
                    } else if (TAG_SUBSCRIPTION_PLAN.equals(name)) {
                        SubscriptionPlan.Builder builder = new SubscriptionPlan.Builder(RecurrenceRule.convertZonedDateTime(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_START)), RecurrenceRule.convertZonedDateTime(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_END)), RecurrenceRule.convertPeriod(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_CYCLE_PERIOD)));
                        builder.setTitle(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_TITLE));
                        builder.setSummary(XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_SUMMARY));
                        long longAttribute5 = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_LIMIT_BYTES, -1L);
                        int intAttribute3 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_LIMIT_BEHAVIOR, -1);
                        if (longAttribute5 != -1 && intAttribute3 != -1) {
                            builder.setDataLimit(longAttribute5, intAttribute3);
                        }
                        long longAttribute6 = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_USAGE_BYTES, -1L);
                        long longAttribute7 = XmlUtils.readLongAttribute(xmlPullParserNewPullParser, ATTR_USAGE_TIME, -1L);
                        if (longAttribute6 != -1 && longAttribute7 != -1) {
                            builder.setDataUsage(longAttribute6, longAttribute7);
                        }
                        int intAttribute4 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_SUB_ID);
                        this.mSubscriptionPlans.put(intAttribute4, (SubscriptionPlan[]) ArrayUtils.appendElement(SubscriptionPlan.class, this.mSubscriptionPlans.get(intAttribute4), builder.build()));
                        this.mSubscriptionPlansOwner.put(intAttribute4, XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_OWNER_PACKAGE));
                    } else if (TAG_UID_POLICY.equals(name)) {
                        int intAttribute5 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "uid");
                        int intAttribute6 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_POLICY);
                        if (UserHandle.isApp(intAttribute5)) {
                            setUidPolicyUncheckedUL(intAttribute5, intAttribute6, false);
                        } else {
                            Slog.w(TAG, "unable to apply policy to UID " + intAttribute5 + "; ignoring");
                        }
                    } else if (TAG_APP_POLICY.equals(name)) {
                        int intAttribute7 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_APP_ID);
                        int intAttribute8 = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_POLICY);
                        int uid = UserHandle.getUid(0, intAttribute7);
                        if (UserHandle.isApp(uid)) {
                            setUidPolicyUncheckedUL(uid, intAttribute8, false);
                        } else {
                            Slog.w(TAG, "unable to apply policy to UID " + uid + "; ignoring");
                        }
                    } else if (TAG_WHITELIST.equals(name)) {
                        z = true;
                    } else if (TAG_RESTRICT_BACKGROUND.equals(name) && z) {
                        sparseBooleanArray.append(XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "uid"), true);
                    } else if (TAG_REVOKED_RESTRICT_BACKGROUND.equals(name) && z) {
                        this.mRestrictBackgroundWhitelistRevokedUids.put(XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "uid"), true);
                    }
                } else if (next == 3 && TAG_WHITELIST.equals(name)) {
                    z = false;
                }
            }
            int size = sparseBooleanArray.size();
            for (int i = 0; i < size; i++) {
                int iKeyAt = sparseBooleanArray.keyAt(i);
                int i2 = this.mUidPolicy.get(iKeyAt, 0);
                if ((i2 & 1) != 0) {
                    Slog.w(TAG, "ignoring restrict-background-whitelist for " + iKeyAt + " because its policy is " + NetworkPolicyManager.uidPoliciesToString(i2));
                } else if (UserHandle.isApp(iKeyAt)) {
                    int i3 = i2 | 4;
                    if (LOGV) {
                        Log.v(TAG, "new policy for " + iKeyAt + ": " + NetworkPolicyManager.uidPoliciesToString(i3));
                    }
                    setUidPolicyUncheckedUL(iKeyAt, i3, false);
                } else {
                    Slog.w(TAG, "unable to update policy on UID " + iKeyAt);
                }
            }
            IoUtils.closeQuietly(fileInputStreamOpenRead);
        } catch (FileNotFoundException e3) {
            fileInputStream = fileInputStreamOpenRead;
            upgradeDefaultBackgroundDataUL();
            IoUtils.closeQuietly(fileInputStream);
        } catch (Exception e4) {
            e = e4;
            fileInputStream = fileInputStreamOpenRead;
            Log.wtf(TAG, "problem reading network policy", e);
            IoUtils.closeQuietly(fileInputStream);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(fileInputStreamOpenRead);
            throw th;
        }
    }

    private void upgradeDefaultBackgroundDataUL() {
        this.mLoadedRestrictBackground = Settings.Global.getInt(this.mContext.getContentResolver(), "default_restrict_background_data", 0) == 1;
    }

    private void upgradeWifiMeteredOverrideAL() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(WifiManager.class);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        int i = 0;
        boolean z = false;
        while (i < this.mNetworkPolicy.size()) {
            NetworkPolicy networkPolicyValueAt = this.mNetworkPolicy.valueAt(i);
            if (networkPolicyValueAt.template.getMatchRule() == 4 && !networkPolicyValueAt.inferred) {
                this.mNetworkPolicy.removeAt(i);
                String strResolveNetworkId = NetworkPolicyManager.resolveNetworkId(networkPolicyValueAt.template.getNetworkId());
                for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                    if (Objects.equals(NetworkPolicyManager.resolveNetworkId(wifiConfiguration), strResolveNetworkId)) {
                        Slog.d(TAG, "Found network " + strResolveNetworkId + "; upgrading metered hint");
                        wifiConfiguration.meteredOverride = networkPolicyValueAt.metered ? 1 : 2;
                        wifiManager.updateNetwork(wifiConfiguration);
                    }
                }
                z = true;
            } else {
                i++;
            }
        }
        if (z) {
            writePolicyAL();
        }
    }

    void writePolicyAL() {
        FileOutputStream fileOutputStreamStartWrite;
        if (LOGV) {
            Slog.v(TAG, "writePolicyAL()");
        }
        try {
            fileOutputStreamStartWrite = this.mPolicyFile.startWrite();
        } catch (IOException e) {
            fileOutputStreamStartWrite = null;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_POLICY_LIST);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_VERSION, 11);
            XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_RESTRICT_BACKGROUND, this.mRestrictBackground);
            for (int i = 0; i < this.mNetworkPolicy.size(); i++) {
                NetworkPolicy networkPolicyValueAt = this.mNetworkPolicy.valueAt(i);
                NetworkTemplate networkTemplate = networkPolicyValueAt.template;
                if (networkTemplate.isPersistable()) {
                    fastXmlSerializer.startTag(null, TAG_NETWORK_POLICY);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_NETWORK_TEMPLATE, networkTemplate.getMatchRule());
                    String subscriberId = networkTemplate.getSubscriberId();
                    if (subscriberId != null) {
                        fastXmlSerializer.attribute(null, ATTR_SUBSCRIBER_ID, subscriberId);
                    }
                    String networkId = networkTemplate.getNetworkId();
                    if (networkId != null) {
                        fastXmlSerializer.attribute(null, ATTR_NETWORK_ID, networkId);
                    }
                    XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CYCLE_START, RecurrenceRule.convertZonedDateTime(networkPolicyValueAt.cycleRule.start));
                    XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CYCLE_END, RecurrenceRule.convertZonedDateTime(networkPolicyValueAt.cycleRule.end));
                    XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CYCLE_PERIOD, RecurrenceRule.convertPeriod(networkPolicyValueAt.cycleRule.period));
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_WARNING_BYTES, networkPolicyValueAt.warningBytes);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LIMIT_BYTES, networkPolicyValueAt.limitBytes);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LAST_WARNING_SNOOZE, networkPolicyValueAt.lastWarningSnooze);
                    XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LAST_LIMIT_SNOOZE, networkPolicyValueAt.lastLimitSnooze);
                    XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_METERED, networkPolicyValueAt.metered);
                    XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_INFERRED, networkPolicyValueAt.inferred);
                    fastXmlSerializer.endTag(null, TAG_NETWORK_POLICY);
                }
            }
            for (int i2 = 0; i2 < this.mSubscriptionPlans.size(); i2++) {
                int iKeyAt = this.mSubscriptionPlans.keyAt(i2);
                String str = this.mSubscriptionPlansOwner.get(iKeyAt);
                SubscriptionPlan[] subscriptionPlanArrValueAt = this.mSubscriptionPlans.valueAt(i2);
                if (!ArrayUtils.isEmpty(subscriptionPlanArrValueAt)) {
                    for (SubscriptionPlan subscriptionPlan : subscriptionPlanArrValueAt) {
                        fastXmlSerializer.startTag(null, TAG_SUBSCRIPTION_PLAN);
                        XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_SUB_ID, iKeyAt);
                        XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_OWNER_PACKAGE, str);
                        RecurrenceRule cycleRule = subscriptionPlan.getCycleRule();
                        XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CYCLE_START, RecurrenceRule.convertZonedDateTime(cycleRule.start));
                        XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CYCLE_END, RecurrenceRule.convertZonedDateTime(cycleRule.end));
                        XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CYCLE_PERIOD, RecurrenceRule.convertPeriod(cycleRule.period));
                        XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_TITLE, subscriptionPlan.getTitle());
                        XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_SUMMARY, subscriptionPlan.getSummary());
                        XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_LIMIT_BYTES, subscriptionPlan.getDataLimitBytes());
                        XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_LIMIT_BEHAVIOR, subscriptionPlan.getDataLimitBehavior());
                        XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_USAGE_BYTES, subscriptionPlan.getDataUsageBytes());
                        XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_USAGE_TIME, subscriptionPlan.getDataUsageTime());
                        fastXmlSerializer.endTag(null, TAG_SUBSCRIPTION_PLAN);
                    }
                }
            }
            for (int i3 = 0; i3 < this.mUidPolicy.size(); i3++) {
                int iKeyAt2 = this.mUidPolicy.keyAt(i3);
                int iValueAt = this.mUidPolicy.valueAt(i3);
                if (iValueAt != 0) {
                    fastXmlSerializer.startTag(null, TAG_UID_POLICY);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, "uid", iKeyAt2);
                    XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_POLICY, iValueAt);
                    fastXmlSerializer.endTag(null, TAG_UID_POLICY);
                }
            }
            fastXmlSerializer.endTag(null, TAG_POLICY_LIST);
            fastXmlSerializer.startTag(null, TAG_WHITELIST);
            int size = this.mRestrictBackgroundWhitelistRevokedUids.size();
            for (int i4 = 0; i4 < size; i4++) {
                int iKeyAt3 = this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i4);
                fastXmlSerializer.startTag(null, TAG_REVOKED_RESTRICT_BACKGROUND);
                XmlUtils.writeIntAttribute(fastXmlSerializer, "uid", iKeyAt3);
                fastXmlSerializer.endTag(null, TAG_REVOKED_RESTRICT_BACKGROUND);
            }
            fastXmlSerializer.endTag(null, TAG_WHITELIST);
            fastXmlSerializer.endDocument();
            this.mPolicyFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            if (fileOutputStreamStartWrite != null) {
                this.mPolicyFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    public void setUidPolicy(int i, int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(i)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + i);
        }
        synchronized (this.mUidRulesFirstLock) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int i3 = this.mUidPolicy.get(i, 0);
                if (i3 != i2) {
                    setUidPolicyUncheckedUL(i, i3, i2, true);
                    this.mLogger.uidPolicyChanged(i, i3, i2);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void addUidPolicy(int i, int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(i)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + i);
        }
        synchronized (this.mUidRulesFirstLock) {
            int i3 = this.mUidPolicy.get(i, 0);
            int i4 = i2 | i3;
            if (i3 != i4) {
                setUidPolicyUncheckedUL(i, i3, i4, true);
                this.mLogger.uidPolicyChanged(i, i3, i4);
            }
        }
    }

    public void removeUidPolicy(int i, int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(i)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + i);
        }
        synchronized (this.mUidRulesFirstLock) {
            int i3 = this.mUidPolicy.get(i, 0);
            int i4 = (~i2) & i3;
            if (i3 != i4) {
                setUidPolicyUncheckedUL(i, i3, i4, true);
                this.mLogger.uidPolicyChanged(i, i3, i4);
            }
        }
    }

    private void setUidPolicyUncheckedUL(int i, int i2, int i3, boolean z) {
        boolean z2 = false;
        setUidPolicyUncheckedUL(i, i3, false);
        if (isUidValidForWhitelistRules(i)) {
            boolean z3 = i2 == 1;
            boolean z4 = i3 == 1;
            boolean z5 = i2 == 4;
            boolean z6 = i3 == 4;
            boolean z7 = z3 || (this.mRestrictBackground && !z5);
            boolean z8 = z4 || (this.mRestrictBackground && !z6);
            if (z5 && ((!z6 || z4) && this.mDefaultRestrictBackgroundWhitelistUids.get(i) && !this.mRestrictBackgroundWhitelistRevokedUids.get(i))) {
                if (LOGD) {
                    Slog.d(TAG, "Adding uid " + i + " to revoked restrict background whitelist");
                }
                this.mRestrictBackgroundWhitelistRevokedUids.append(i, true);
            }
            if (z7 != z8) {
                z2 = true;
            }
        }
        this.mHandler.obtainMessage(13, i, i3, Boolean.valueOf(z2)).sendToTarget();
        if (z) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    private void setUidPolicyUncheckedUL(int i, int i2, boolean z) {
        if (i2 == 0) {
            this.mUidPolicy.delete(i);
        } else {
            this.mUidPolicy.put(i, i2);
        }
        updateRulesForDataUsageRestrictionsUL(i);
        if (z) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    public int getUidPolicy(int i) {
        int i2;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            i2 = this.mUidPolicy.get(i, 0);
        }
        return i2;
    }

    public int[] getUidsWithPolicy(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        int[] iArrAppendInt = new int[0];
        synchronized (this.mUidRulesFirstLock) {
            for (int i2 = 0; i2 < this.mUidPolicy.size(); i2++) {
                int iKeyAt = this.mUidPolicy.keyAt(i2);
                int iValueAt = this.mUidPolicy.valueAt(i2);
                if ((i == 0 && iValueAt == 0) || (iValueAt & i) != 0) {
                    iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, iKeyAt);
                }
            }
        }
        return iArrAppendInt;
    }

    boolean removeUserStateUL(int i, boolean z) {
        this.mLogger.removingUserState(i);
        boolean z2 = false;
        for (int size = this.mRestrictBackgroundWhitelistRevokedUids.size() - 1; size >= 0; size--) {
            if (UserHandle.getUserId(this.mRestrictBackgroundWhitelistRevokedUids.keyAt(size)) == i) {
                this.mRestrictBackgroundWhitelistRevokedUids.removeAt(size);
                z2 = true;
            }
        }
        int[] iArrAppendInt = new int[0];
        for (int i2 = 0; i2 < this.mUidPolicy.size(); i2++) {
            int iKeyAt = this.mUidPolicy.keyAt(i2);
            if (UserHandle.getUserId(iKeyAt) == i) {
                iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, iKeyAt);
            }
        }
        if (iArrAppendInt.length > 0) {
            for (int i3 : iArrAppendInt) {
                this.mUidPolicy.delete(i3);
            }
            z2 = true;
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            updateRulesForGlobalChangeAL(true);
            if (z && z2) {
                writePolicyAL();
            }
        }
        return z2;
    }

    public void registerListener(INetworkPolicyListener iNetworkPolicyListener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mListeners.register(iNetworkPolicyListener);
    }

    public void unregisterListener(INetworkPolicyListener iNetworkPolicyListener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mListeners.unregister(iNetworkPolicyListener);
    }

    public void setNetworkPolicies(NetworkPolicy[] networkPolicyArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    normalizePoliciesNL(networkPolicyArr);
                    handleNetworkPoliciesUpdateAL(false);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void addNetworkPolicyAL(NetworkPolicy networkPolicy) {
        setNetworkPolicies((NetworkPolicy[]) ArrayUtils.appendElement(NetworkPolicy.class, getNetworkPolicies(this.mContext.getOpPackageName()), networkPolicy));
    }

    public NetworkPolicy[] getNetworkPolicies(String str) {
        NetworkPolicy[] networkPolicyArr;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", TAG);
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", TAG);
            if (this.mAppOps.noteOp(51, Binder.getCallingUid(), str) != 0) {
                return new NetworkPolicy[0];
            }
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            int size = this.mNetworkPolicy.size();
            networkPolicyArr = new NetworkPolicy[size];
            for (int i = 0; i < size; i++) {
                networkPolicyArr[i] = this.mNetworkPolicy.valueAt(i);
            }
        }
        return networkPolicyArr;
    }

    private void normalizePoliciesNL() {
        normalizePoliciesNL(getNetworkPolicies(this.mContext.getOpPackageName()));
    }

    private void normalizePoliciesNL(NetworkPolicy[] networkPolicyArr) {
        this.mNetworkPolicy.clear();
        for (NetworkPolicy networkPolicy : networkPolicyArr) {
            if (networkPolicy != null) {
                networkPolicy.template = NetworkTemplate.normalize(networkPolicy.template, this.mMergedSubscriberIds);
                NetworkPolicy networkPolicy2 = this.mNetworkPolicy.get(networkPolicy.template);
                if (networkPolicy2 == null || networkPolicy2.compareTo(networkPolicy) > 0) {
                    if (networkPolicy2 != null) {
                        Slog.d(TAG, "Normalization replaced " + networkPolicy2 + " with " + networkPolicy);
                    }
                    this.mNetworkPolicy.put(networkPolicy.template, networkPolicy);
                }
            }
        }
    }

    public void snoozeLimit(NetworkTemplate networkTemplate) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            performSnooze(networkTemplate, 35);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void performSnooze(NetworkTemplate networkTemplate, int i) {
        if (LOGD) {
            Log.d(TAG, "performSnooze on:" + networkTemplate);
        }
        long jMillis = this.mClock.millis();
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                NetworkPolicy networkPolicy = this.mNetworkPolicy.get(networkTemplate);
                if (networkPolicy == null) {
                    throw new IllegalArgumentException("unable to find policy for " + networkTemplate);
                }
                if (i != 45) {
                    switch (i) {
                        case 34:
                            networkPolicy.lastWarningSnooze = jMillis;
                            break;
                        case 35:
                            networkPolicy.lastLimitSnooze = jMillis;
                            break;
                        default:
                            throw new IllegalArgumentException("unexpected type");
                    }
                } else {
                    networkPolicy.lastRapidSnooze = jMillis;
                }
                handleNetworkPoliciesUpdateAL(true);
            }
        }
    }

    public void onTetheringChanged(String str, boolean z) {
        synchronized (this.mUidRulesFirstLock) {
            if (this.mRestrictBackground && z) {
                Log.d(TAG, "Tethering on (" + str + "); disable Data Saver");
                setRestrictBackground(false);
            }
        }
    }

    public void setRestrictBackground(boolean z) {
        Trace.traceBegin(2097152L, "setRestrictBackground");
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            Slog.d(TAG, "setRestrictBackground(" + z + ")");
            try {
                synchronized (this.mUidRulesFirstLock) {
                    setRestrictBackgroundUL(z);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void setRestrictBackgroundUL(boolean z) {
        Trace.traceBegin(2097152L, "setRestrictBackgroundUL");
        try {
            if (z == this.mRestrictBackground) {
                Slog.w(TAG, "setRestrictBackgroundUL: already " + z);
                return;
            }
            Slog.d(TAG, "setRestrictBackgroundUL(): " + z);
            boolean z2 = this.mRestrictBackground;
            this.mRestrictBackground = z;
            updateRulesForRestrictBackgroundUL();
            try {
                if (!this.mNetworkManager.setDataSaverModeEnabled(this.mRestrictBackground)) {
                    Slog.e(TAG, "Could not change Data Saver Mode on NMS to " + this.mRestrictBackground);
                    this.mRestrictBackground = z2;
                    return;
                }
            } catch (RemoteException e) {
            }
            sendRestrictBackgroundChangedMsg();
            this.mLogger.restrictBackgroundChanged(z2, this.mRestrictBackground);
            if (this.mRestrictBackgroundPowerState.globalBatterySaverEnabled) {
                this.mRestrictBackgroundChangedInBsm = true;
            }
            synchronized (this.mNetworkPoliciesSecondLock) {
                updateNotificationsNL();
                writePolicyAL();
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void sendRestrictBackgroundChangedMsg() {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, this.mRestrictBackground ? 1 : 0, 0).sendToTarget();
    }

    public int getRestrictBackgroundByCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        int callingUid = Binder.getCallingUid();
        synchronized (this.mUidRulesFirstLock) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int uidPolicy = getUidPolicy(callingUid);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                int i = 3;
                if (uidPolicy == 1) {
                    return 3;
                }
                if (!this.mRestrictBackground) {
                    return 1;
                }
                if ((this.mUidPolicy.get(callingUid) & 4) != 0) {
                    i = 2;
                }
                return i;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
    }

    public boolean getRestrictBackground() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            z = this.mRestrictBackground;
        }
        return z;
    }

    public void setDeviceIdleMode(boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        Trace.traceBegin(2097152L, "setDeviceIdleMode");
        try {
            synchronized (this.mUidRulesFirstLock) {
                if (this.mDeviceIdleMode == z) {
                    return;
                }
                this.mDeviceIdleMode = z;
                this.mLogger.deviceIdleModeEnabled(z);
                if (this.mSystemReady) {
                    updateRulesForRestrictPowerUL();
                }
                if (z) {
                    EventLogTags.writeDeviceIdleOnPhase("net");
                } else {
                    EventLogTags.writeDeviceIdleOffPhase("net");
                }
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    public void setWifiMeteredOverride(String str, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(WifiManager.class);
            for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks()) {
                if (Objects.equals(NetworkPolicyManager.resolveNetworkId(wifiConfiguration), str)) {
                    wifiConfiguration.meteredOverride = i;
                    wifiManager.updateNetwork(wifiConfiguration);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Deprecated
    public NetworkQuotaInfo getNetworkQuotaInfo(NetworkState networkState) {
        Log.w(TAG, "Shame on UID " + Binder.getCallingUid() + " for calling the hidden API getNetworkQuotaInfo(). Shame!");
        return new NetworkQuotaInfo();
    }

    private void enforceSubscriptionPlanAccess(int i, int i2, String str) {
        this.mAppOps.checkPackage(i2, str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo activeSubscriptionInfo = ((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).getActiveSubscriptionInfo(i);
            PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(i);
            if (activeSubscriptionInfo != null && activeSubscriptionInfo.isEmbedded() && activeSubscriptionInfo.canManageSubscription(this.mContext, str)) {
                return;
            }
            if (configForSubId != null) {
                String string = configForSubId.getString("config_plans_package_override_string", null);
                if (!TextUtils.isEmpty(string) && Objects.equals(string, str)) {
                    return;
                }
            }
            String defaultCarrierServicePackageName = this.mCarrierConfigManager.getDefaultCarrierServicePackageName();
            if (!TextUtils.isEmpty(defaultCarrierServicePackageName) && Objects.equals(defaultCarrierServicePackageName, str)) {
                return;
            }
            String str2 = SystemProperties.get("persist.sys.sub_plan_owner." + i, (String) null);
            if (!TextUtils.isEmpty(str2) && Objects.equals(str2, str)) {
                return;
            }
            String str3 = SystemProperties.get("fw.sub_plan_owner." + i, (String) null);
            if (!TextUtils.isEmpty(str3) && Objects.equals(str3, str)) {
                return;
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_SUBSCRIPTION_PLANS", TAG);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public SubscriptionPlan[] getSubscriptionPlans(int i, String str) {
        enforceSubscriptionPlanAccess(i, Binder.getCallingUid(), str);
        String str2 = SystemProperties.get("fw.fake_plan");
        if (!TextUtils.isEmpty(str2)) {
            ArrayList arrayList = new ArrayList();
            if ("month_hard".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile").setDataLimit(5368709120L, 1).setDataUsage(1073741824L, ZonedDateTime.now().minusHours(36L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile Happy").setDataLimit(JobStatus.NO_LATEST_RUNTIME, 1).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(36L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Charged after limit").setDataLimit(5368709120L, 1).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(36L).toInstant().toEpochMilli()).build());
            } else if ("month_soft".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile is the carriers name who this plan belongs to").setSummary("Crazy unlimited bandwidth plan with incredibly long title that should be cut off to prevent UI from looking terrible").setDataLimit(5368709120L, 2).setDataUsage(1073741824L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Throttled after limit").setDataLimit(5368709120L, 2).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, No data connection after limit").setDataLimit(5368709120L, 0).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
            } else if ("month_over".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile is the carriers name who this plan belongs to").setDataLimit(5368709120L, 2).setDataUsage(6442450944L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Throttled after limit").setDataLimit(5368709120L, 2).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, No data connection after limit").setDataLimit(5368709120L, 0).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
            } else if ("month_none".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile").build());
            } else if ("prepaid".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20L), ZonedDateTime.now().plusDays(10L)).setTitle("G-Mobile").setDataLimit(536870912L, 0).setDataUsage(104857600L, ZonedDateTime.now().minusHours(3L).toInstant().toEpochMilli()).build());
            } else if ("prepaid_crazy".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20L), ZonedDateTime.now().plusDays(10L)).setTitle("G-Mobile Anytime").setDataLimit(536870912L, 0).setDataUsage(104857600L, ZonedDateTime.now().minusHours(3L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(10L), ZonedDateTime.now().plusDays(20L)).setTitle("G-Mobile Nickel Nights").setSummary("5¢/GB between 1-5AM").setDataLimit(5368709120L, 2).setDataUsage(15728640L, ZonedDateTime.now().minusHours(30L).toInstant().toEpochMilli()).build());
                arrayList.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(10L), ZonedDateTime.now().plusDays(20L)).setTitle("G-Mobile Bonus 3G").setSummary("Unlimited 3G data").setDataLimit(1073741824L, 2).setDataUsage(314572800L, ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
            } else if ("unlimited".equals(str2)) {
                arrayList.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20L), ZonedDateTime.now().plusDays(10L)).setTitle("G-Mobile Awesome").setDataLimit(JobStatus.NO_LATEST_RUNTIME, 2).setDataUsage(52428800L, ZonedDateTime.now().minusHours(3L).toInstant().toEpochMilli()).build());
            }
            return (SubscriptionPlan[]) arrayList.toArray(new SubscriptionPlan[arrayList.size()]);
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            String str3 = this.mSubscriptionPlansOwner.get(i);
            if (!Objects.equals(str3, str) && UserHandle.getCallingAppId() != 1000) {
                Log.w(TAG, "Not returning plans because caller " + str + " doesn't match owner " + str3);
                return null;
            }
            return this.mSubscriptionPlans.get(i);
        }
    }

    public void setSubscriptionPlans(int i, SubscriptionPlan[] subscriptionPlanArr, String str) {
        enforceSubscriptionPlanAccess(i, Binder.getCallingUid(), str);
        for (SubscriptionPlan subscriptionPlan : subscriptionPlanArr) {
            Preconditions.checkNotNull(subscriptionPlan);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    this.mSubscriptionPlans.put(i, subscriptionPlanArr);
                    this.mSubscriptionPlansOwner.put(i, str);
                    String str2 = this.mSubIdToSubscriberId.get(i, null);
                    if (str2 != null) {
                        ensureActiveMobilePolicyAL(i, str2);
                        maybeUpdateMobilePolicyCycleAL(i, str2);
                    } else {
                        Slog.e(TAG, "Missing subscriberId for subId " + i);
                    }
                    handleNetworkPoliciesUpdateAL(true);
                }
            }
            Intent intent = new Intent("android.telephony.action.SUBSCRIPTION_PLANS_CHANGED");
            intent.addFlags(1073741824);
            intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", i);
            this.mContext.sendBroadcast(intent, "android.permission.MANAGE_SUBSCRIPTION_PLANS");
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void setSubscriptionPlansOwner(int i, String str) {
        SystemProperties.set("persist.sys.sub_plan_owner." + i, str);
    }

    public String getSubscriptionPlansOwner(int i) {
        String str;
        if (UserHandle.getCallingAppId() != 1000) {
            throw new SecurityException();
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            str = this.mSubscriptionPlansOwner.get(i);
        }
        return str;
    }

    public void setSubscriptionOverride(int i, int i2, int i3, long j, String str) {
        enforceSubscriptionPlanAccess(i, Binder.getCallingUid(), str);
        synchronized (this.mNetworkPoliciesSecondLock) {
            SubscriptionPlan primarySubscriptionPlanLocked = getPrimarySubscriptionPlanLocked(i);
            if (primarySubscriptionPlanLocked == null || primarySubscriptionPlanLocked.getDataLimitBehavior() == -1) {
                throw new IllegalStateException("Must provide valid SubscriptionPlan to enable overriding");
            }
        }
        if ((Settings.Global.getInt(this.mContext.getContentResolver(), "netpolicy_override_enabled", 1) != 0) || i3 == 0) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(16, i2, i3, Integer.valueOf(i)));
            if (j > 0) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(16, i2, 0, Integer.valueOf(i)), j);
            }
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            ArraySet arraySet = new ArraySet(strArr.length);
            for (String str : strArr) {
                arraySet.add(str);
            }
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    if (arraySet.contains("--unsnooze")) {
                        for (int size = this.mNetworkPolicy.size() - 1; size >= 0; size--) {
                            this.mNetworkPolicy.valueAt(size).clearSnooze();
                        }
                        handleNetworkPoliciesUpdateAL(true);
                        indentingPrintWriter.println("Cleared snooze timestamps");
                        return;
                    }
                    indentingPrintWriter.print("System ready: ");
                    indentingPrintWriter.println(this.mSystemReady);
                    indentingPrintWriter.print("Restrict background: ");
                    indentingPrintWriter.println(this.mRestrictBackground);
                    indentingPrintWriter.print("Restrict power: ");
                    indentingPrintWriter.println(this.mRestrictPower);
                    indentingPrintWriter.print("Device idle: ");
                    indentingPrintWriter.println(this.mDeviceIdleMode);
                    indentingPrintWriter.print("Metered ifaces: ");
                    indentingPrintWriter.println(String.valueOf(this.mMeteredIfaces));
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Network policies:");
                    indentingPrintWriter.increaseIndent();
                    for (int i = 0; i < this.mNetworkPolicy.size(); i++) {
                        indentingPrintWriter.println(this.mNetworkPolicy.valueAt(i).toString());
                    }
                    indentingPrintWriter.decreaseIndent();
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Subscription plans:");
                    indentingPrintWriter.increaseIndent();
                    for (int i2 = 0; i2 < this.mSubscriptionPlans.size(); i2++) {
                        indentingPrintWriter.println("Subscriber ID " + this.mSubscriptionPlans.keyAt(i2) + ":");
                        indentingPrintWriter.increaseIndent();
                        SubscriptionPlan[] subscriptionPlanArrValueAt = this.mSubscriptionPlans.valueAt(i2);
                        if (!ArrayUtils.isEmpty(subscriptionPlanArrValueAt)) {
                            for (SubscriptionPlan subscriptionPlan : subscriptionPlanArrValueAt) {
                                indentingPrintWriter.println(subscriptionPlan);
                            }
                        }
                        indentingPrintWriter.decreaseIndent();
                    }
                    indentingPrintWriter.decreaseIndent();
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Active subscriptions:");
                    indentingPrintWriter.increaseIndent();
                    for (int i3 = 0; i3 < this.mSubIdToSubscriberId.size(); i3++) {
                        indentingPrintWriter.println(this.mSubIdToSubscriberId.keyAt(i3) + "=" + NetworkIdentity.scrubSubscriberId(this.mSubIdToSubscriberId.valueAt(i3)));
                    }
                    indentingPrintWriter.decreaseIndent();
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Merged subscriptions: " + Arrays.toString(NetworkIdentity.scrubSubscriberId(this.mMergedSubscriberIds)));
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Policy for UIDs:");
                    indentingPrintWriter.increaseIndent();
                    int size2 = this.mUidPolicy.size();
                    for (int i4 = 0; i4 < size2; i4++) {
                        int iKeyAt = this.mUidPolicy.keyAt(i4);
                        int iValueAt = this.mUidPolicy.valueAt(i4);
                        indentingPrintWriter.print("UID=");
                        indentingPrintWriter.print(iKeyAt);
                        indentingPrintWriter.print(" policy=");
                        indentingPrintWriter.print(NetworkPolicyManager.uidPoliciesToString(iValueAt));
                        indentingPrintWriter.println();
                    }
                    indentingPrintWriter.decreaseIndent();
                    int size3 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
                    if (size3 > 0) {
                        indentingPrintWriter.println("Power save whitelist (except idle) app ids:");
                        indentingPrintWriter.increaseIndent();
                        for (int i5 = 0; i5 < size3; i5++) {
                            indentingPrintWriter.print("UID=");
                            indentingPrintWriter.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(i5));
                            indentingPrintWriter.print(": ");
                            indentingPrintWriter.print(this.mPowerSaveWhitelistExceptIdleAppIds.valueAt(i5));
                            indentingPrintWriter.println();
                        }
                        indentingPrintWriter.decreaseIndent();
                    }
                    int size4 = this.mPowerSaveWhitelistAppIds.size();
                    if (size4 > 0) {
                        indentingPrintWriter.println("Power save whitelist app ids:");
                        indentingPrintWriter.increaseIndent();
                        for (int i6 = 0; i6 < size4; i6++) {
                            indentingPrintWriter.print("UID=");
                            indentingPrintWriter.print(this.mPowerSaveWhitelistAppIds.keyAt(i6));
                            indentingPrintWriter.print(": ");
                            indentingPrintWriter.print(this.mPowerSaveWhitelistAppIds.valueAt(i6));
                            indentingPrintWriter.println();
                        }
                        indentingPrintWriter.decreaseIndent();
                    }
                    int size5 = this.mDefaultRestrictBackgroundWhitelistUids.size();
                    if (size5 > 0) {
                        indentingPrintWriter.println("Default restrict background whitelist uids:");
                        indentingPrintWriter.increaseIndent();
                        for (int i7 = 0; i7 < size5; i7++) {
                            indentingPrintWriter.print("UID=");
                            indentingPrintWriter.print(this.mDefaultRestrictBackgroundWhitelistUids.keyAt(i7));
                            indentingPrintWriter.println();
                        }
                        indentingPrintWriter.decreaseIndent();
                    }
                    int size6 = this.mRestrictBackgroundWhitelistRevokedUids.size();
                    if (size6 > 0) {
                        indentingPrintWriter.println("Default restrict background whitelist uids revoked by users:");
                        indentingPrintWriter.increaseIndent();
                        for (int i8 = 0; i8 < size6; i8++) {
                            indentingPrintWriter.print("UID=");
                            indentingPrintWriter.print(this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i8));
                            indentingPrintWriter.println();
                        }
                        indentingPrintWriter.decreaseIndent();
                    }
                    SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
                    collectKeys(this.mUidState, sparseBooleanArray);
                    collectKeys(this.mUidRules, sparseBooleanArray);
                    indentingPrintWriter.println("Status for all known UIDs:");
                    indentingPrintWriter.increaseIndent();
                    int size7 = sparseBooleanArray.size();
                    for (int i9 = 0; i9 < size7; i9++) {
                        int iKeyAt2 = sparseBooleanArray.keyAt(i9);
                        indentingPrintWriter.print("UID=");
                        indentingPrintWriter.print(iKeyAt2);
                        int i10 = this.mUidState.get(iKeyAt2, 18);
                        indentingPrintWriter.print(" state=");
                        indentingPrintWriter.print(i10);
                        if (i10 <= 2) {
                            indentingPrintWriter.print(" (fg)");
                        } else {
                            indentingPrintWriter.print(i10 <= 4 ? " (fg svc)" : " (bg)");
                        }
                        int i11 = this.mUidRules.get(iKeyAt2, 0);
                        indentingPrintWriter.print(" rules=");
                        indentingPrintWriter.print(NetworkPolicyManager.uidRulesToString(i11));
                        indentingPrintWriter.println();
                    }
                    indentingPrintWriter.decreaseIndent();
                    indentingPrintWriter.println("Status for just UIDs with rules:");
                    indentingPrintWriter.increaseIndent();
                    int size8 = this.mUidRules.size();
                    for (int i12 = 0; i12 < size8; i12++) {
                        int iKeyAt3 = this.mUidRules.keyAt(i12);
                        indentingPrintWriter.print("UID=");
                        indentingPrintWriter.print(iKeyAt3);
                        int i13 = this.mUidRules.get(iKeyAt3, 0);
                        indentingPrintWriter.print(" rules=");
                        indentingPrintWriter.print(NetworkPolicyManager.uidRulesToString(i13));
                        indentingPrintWriter.println();
                    }
                    indentingPrintWriter.decreaseIndent();
                    indentingPrintWriter.println("Admin restricted uids for metered data:");
                    indentingPrintWriter.increaseIndent();
                    int size9 = this.mMeteredRestrictedUids.size();
                    for (int i14 = 0; i14 < size9; i14++) {
                        indentingPrintWriter.print("u" + this.mMeteredRestrictedUids.keyAt(i14) + ": ");
                        indentingPrintWriter.println(this.mMeteredRestrictedUids.valueAt(i14));
                    }
                    indentingPrintWriter.decreaseIndent();
                    indentingPrintWriter.println();
                    this.mStatLogger.dump(indentingPrintWriter);
                    this.mLogger.dumpLogs(indentingPrintWriter);
                }
            }
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new NetworkPolicyManagerShellCommand(this.mContext, this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    @VisibleForTesting
    public boolean isUidForeground(int i) {
        boolean zIsUidStateForeground;
        synchronized (this.mUidRulesFirstLock) {
            zIsUidStateForeground = isUidStateForeground(this.mUidState.get(i, 18));
        }
        return zIsUidStateForeground;
    }

    private boolean isUidForegroundOnRestrictBackgroundUL(int i) {
        return NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(this.mUidState.get(i, 18));
    }

    private boolean isUidForegroundOnRestrictPowerUL(int i) {
        return NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.get(i, 18));
    }

    private boolean isUidStateForeground(int i) {
        return i <= 4;
    }

    private void updateUidStateUL(int i, int i2) {
        Trace.traceBegin(2097152L, "updateUidStateUL");
        try {
            int i3 = this.mUidState.get(i, 18);
            if (i3 != i2) {
                this.mUidState.put(i, i2);
                updateRestrictBackgroundRulesOnUidStatusChangedUL(i, i3, i2);
                if (NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(i3) != NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(i2)) {
                    updateRuleForAppIdleUL(i);
                    if (this.mDeviceIdleMode) {
                        updateRuleForDeviceIdleUL(i);
                    }
                    if (this.mRestrictPower) {
                        updateRuleForRestrictPowerUL(i);
                    }
                    updateRulesForPowerRestrictionsUL(i);
                }
                updateNetworkStats(i, isUidStateForeground(i2));
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void removeUidStateUL(int i) {
        int iIndexOfKey = this.mUidState.indexOfKey(i);
        if (iIndexOfKey >= 0) {
            int iValueAt = this.mUidState.valueAt(iIndexOfKey);
            this.mUidState.removeAt(iIndexOfKey);
            if (iValueAt != 18) {
                updateRestrictBackgroundRulesOnUidStatusChangedUL(i, iValueAt, 18);
                if (this.mDeviceIdleMode) {
                    updateRuleForDeviceIdleUL(i);
                }
                if (this.mRestrictPower) {
                    updateRuleForRestrictPowerUL(i);
                }
                updateRulesForPowerRestrictionsUL(i);
                updateNetworkStats(i, false);
            }
        }
    }

    private void updateNetworkStats(int i, boolean z) {
        if (Trace.isTagEnabled(2097152L)) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateNetworkStats: ");
            sb.append(i);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(z ? "F" : "B");
            Trace.traceBegin(2097152L, sb.toString());
        }
        try {
            this.mNetworkStats.setUidForeground(i, z);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRestrictBackgroundRulesOnUidStatusChangedUL(int i, int i2, int i3) {
        if (NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(i2) != NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(i3)) {
            updateRulesForDataUsageRestrictionsUL(i);
        }
    }

    void updateRulesForPowerSaveUL() {
        Trace.traceBegin(2097152L, "updateRulesForPowerSaveUL");
        try {
            updateRulesForWhitelistedPowerSaveUL(this.mRestrictPower, 3, this.mUidFirewallPowerSaveRules);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForRestrictPowerUL(int i) {
        updateRulesForWhitelistedPowerSaveUL(i, this.mRestrictPower, 3);
    }

    void updateRulesForDeviceIdleUL() {
        Trace.traceBegin(2097152L, "updateRulesForDeviceIdleUL");
        try {
            updateRulesForWhitelistedPowerSaveUL(this.mDeviceIdleMode, 1, this.mUidFirewallDozableRules);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForDeviceIdleUL(int i) {
        updateRulesForWhitelistedPowerSaveUL(i, this.mDeviceIdleMode, 1);
    }

    private void updateRulesForWhitelistedPowerSaveUL(boolean z, int i, SparseIntArray sparseIntArray) {
        if (z) {
            sparseIntArray.clear();
            List users = this.mUserManager.getUsers();
            for (int size = users.size() - 1; size >= 0; size--) {
                UserInfo userInfo = (UserInfo) users.get(size);
                updateRulesForWhitelistedAppIds(sparseIntArray, this.mPowerSaveTempWhitelistAppIds, userInfo.id);
                updateRulesForWhitelistedAppIds(sparseIntArray, this.mPowerSaveWhitelistAppIds, userInfo.id);
                if (i == 3) {
                    updateRulesForWhitelistedAppIds(sparseIntArray, this.mPowerSaveWhitelistExceptIdleAppIds, userInfo.id);
                }
            }
            for (int size2 = this.mUidState.size() - 1; size2 >= 0; size2--) {
                if (NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.valueAt(size2))) {
                    sparseIntArray.put(this.mUidState.keyAt(size2), 1);
                }
            }
            setUidFirewallRulesUL(i, sparseIntArray, 1);
            return;
        }
        setUidFirewallRulesUL(i, null, 2);
    }

    private void updateRulesForWhitelistedAppIds(SparseIntArray sparseIntArray, SparseBooleanArray sparseBooleanArray, int i) {
        for (int size = sparseBooleanArray.size() - 1; size >= 0; size--) {
            if (sparseBooleanArray.valueAt(size)) {
                sparseIntArray.put(UserHandle.getUid(i, sparseBooleanArray.keyAt(size)), 1);
            }
        }
    }

    private boolean isWhitelistedBatterySaverUL(int i, boolean z) {
        int appId = UserHandle.getAppId(i);
        boolean z2 = this.mPowerSaveTempWhitelistAppIds.get(appId) || this.mPowerSaveWhitelistAppIds.get(appId);
        return !z ? z2 || this.mPowerSaveWhitelistExceptIdleAppIds.get(appId) : z2;
    }

    private void updateRulesForWhitelistedPowerSaveUL(int i, boolean z, int i2) {
        if (z) {
            if (isWhitelistedBatterySaverUL(i, i2 == 1) || isUidForegroundOnRestrictPowerUL(i)) {
                setUidFirewallRule(i2, i, 1);
            } else {
                setUidFirewallRule(i2, i, 0);
            }
        }
    }

    void updateRulesForAppIdleUL() {
        Trace.traceBegin(2097152L, "updateRulesForAppIdleUL");
        try {
            SparseIntArray sparseIntArray = this.mUidFirewallStandbyRules;
            sparseIntArray.clear();
            List users = this.mUserManager.getUsers();
            for (int size = users.size() - 1; size >= 0; size--) {
                for (int i : this.mUsageStats.getIdleUidsForUser(((UserInfo) users.get(size)).id)) {
                    if (!this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(i), false) && hasInternetPermissions(i)) {
                        sparseIntArray.put(i, 2);
                    }
                }
            }
            setUidFirewallRulesUL(2, sparseIntArray, 0);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForAppIdleUL(int i) {
        if (isUidValidForBlacklistRules(i)) {
            if (Trace.isTagEnabled(2097152L)) {
                Trace.traceBegin(2097152L, "updateRuleForAppIdleUL: " + i);
            }
            try {
                if (!this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(i)) && isUidIdle(i) && !isUidForegroundOnRestrictPowerUL(i)) {
                    setUidFirewallRule(2, i, 2);
                } else {
                    setUidFirewallRule(2, i, 0);
                }
            } finally {
                Trace.traceEnd(2097152L);
            }
        }
    }

    void updateRulesForAppIdleParoleUL() {
        boolean zIsAppIdleParoleOn = this.mUsageStats.isAppIdleParoleOn();
        boolean z = !zIsAppIdleParoleOn;
        enableFirewallChainUL(2, z);
        int size = this.mUidFirewallStandbyRules.size();
        for (int i = 0; i < size; i++) {
            int iKeyAt = this.mUidFirewallStandbyRules.keyAt(i);
            int i2 = this.mUidRules.get(iKeyAt);
            if (z) {
                i2 &= 15;
            } else {
                if ((i2 & 240) == 0) {
                }
            }
            int iUpdateRulesForPowerRestrictionsUL = updateRulesForPowerRestrictionsUL(iKeyAt, i2, zIsAppIdleParoleOn);
            if (iUpdateRulesForPowerRestrictionsUL == 0) {
                this.mUidRules.delete(iKeyAt);
            } else {
                this.mUidRules.put(iKeyAt, iUpdateRulesForPowerRestrictionsUL);
            }
        }
    }

    private void updateRulesForGlobalChangeAL(boolean z) {
        if (Trace.isTagEnabled(2097152L)) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateRulesForGlobalChangeAL: ");
            sb.append(z ? "R" : "-");
            Trace.traceBegin(2097152L, sb.toString());
        }
        try {
            updateRulesForAppIdleUL();
            updateRulesForRestrictPowerUL();
            updateRulesForRestrictBackgroundUL();
            if (z) {
                normalizePoliciesNL();
                updateNetworkRulesNL();
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForRestrictPowerUL() {
        Trace.traceBegin(2097152L, "updateRulesForRestrictPowerUL");
        try {
            updateRulesForDeviceIdleUL();
            updateRulesForPowerSaveUL();
            updateRulesForAllAppsUL(2);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForRestrictBackgroundUL() {
        Trace.traceBegin(2097152L, "updateRulesForRestrictBackgroundUL");
        try {
            updateRulesForAllAppsUL(1);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForAllAppsUL(int i) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "updateRulesForRestrictPowerUL-" + i);
        }
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            Trace.traceBegin(2097152L, "list-users");
            List users = this.mUserManager.getUsers();
            Trace.traceEnd(2097152L);
            Trace.traceBegin(2097152L, "list-uids");
            List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(4981248);
            Trace.traceEnd(2097152L);
            int size = users.size();
            int size2 = installedApplications.size();
            for (int i2 = 0; i2 < size; i2++) {
                UserInfo userInfo = (UserInfo) users.get(i2);
                for (int i3 = 0; i3 < size2; i3++) {
                    int uid = UserHandle.getUid(userInfo.id, installedApplications.get(i3).uid);
                    switch (i) {
                        case 1:
                            updateRulesForDataUsageRestrictionsUL(uid);
                            break;
                        case 2:
                            updateRulesForPowerRestrictionsUL(uid);
                            break;
                        default:
                            Slog.w(TAG, "Invalid type for updateRulesForAllApps: " + i);
                            break;
                    }
                }
            }
        } finally {
        }
    }

    private void updateRulesForTempWhitelistChangeUL(int i) {
        List users = this.mUserManager.getUsers();
        int size = users.size();
        for (int i2 = 0; i2 < size; i2++) {
            int uid = UserHandle.getUid(((UserInfo) users.get(i2)).id, i);
            updateRuleForAppIdleUL(uid);
            updateRuleForDeviceIdleUL(uid);
            updateRuleForRestrictPowerUL(uid);
            updateRulesForPowerRestrictionsUL(uid);
        }
    }

    private boolean isUidValidForBlacklistRules(int i) {
        if (i != 1013 && i != 1019) {
            if (UserHandle.isApp(i) && hasInternetPermissions(i)) {
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean isUidValidForWhitelistRules(int i) {
        return UserHandle.isApp(i) && hasInternetPermissions(i);
    }

    private boolean isUidIdle(int i) {
        String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i);
        int userId = UserHandle.getUserId(i);
        if (packagesForUid != null) {
            for (String str : packagesForUid) {
                if (!this.mUsageStats.isAppIdle(str, i, userId)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean hasInternetPermissions(int i) {
        try {
            if (this.mIPm.checkUidPermission("android.permission.INTERNET", i) != 0) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            return true;
        }
    }

    private void onUidDeletedUL(int i) {
        this.mUidRules.delete(i);
        this.mUidPolicy.delete(i);
        this.mUidFirewallStandbyRules.delete(i);
        this.mUidFirewallDozableRules.delete(i);
        this.mUidFirewallPowerSaveRules.delete(i);
        this.mPowerSaveWhitelistExceptIdleAppIds.delete(i);
        this.mPowerSaveWhitelistAppIds.delete(i);
        this.mPowerSaveTempWhitelistAppIds.delete(i);
        this.mHandler.obtainMessage(15, i, 0).sendToTarget();
    }

    private void updateRestrictionRulesForUidUL(int i) {
        updateRuleForDeviceIdleUL(i);
        updateRuleForAppIdleUL(i);
        updateRuleForRestrictPowerUL(i);
        updateRulesForPowerRestrictionsUL(i);
        updateRulesForDataUsageRestrictionsUL(i);
    }

    private void updateRulesForDataUsageRestrictionsUL(int i) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "updateRulesForDataUsageRestrictionsUL: " + i);
        }
        try {
            updateRulesForDataUsageRestrictionsULInner(i);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForDataUsageRestrictionsULInner(int i) {
        int i2;
        if (!isUidValidForWhitelistRules(i)) {
            if (LOGD) {
                Slog.d(TAG, "no need to update restrict data rules for uid " + i);
                return;
            }
            return;
        }
        int i3 = this.mUidPolicy.get(i, 0);
        int i4 = this.mUidRules.get(i, 0);
        boolean zIsUidForegroundOnRestrictBackgroundUL = isUidForegroundOnRestrictBackgroundUL(i);
        boolean zIsRestrictedByAdminUL = isRestrictedByAdminUL(i);
        boolean z = (i3 & 1) != 0;
        boolean z2 = (i3 & 4) != 0;
        int i5 = i4 & 15;
        if (zIsRestrictedByAdminUL) {
            i2 = 4;
        } else if (zIsUidForegroundOnRestrictBackgroundUL) {
            i2 = (z || (this.mRestrictBackground && !z2)) ? 2 : z2 ? 1 : 0;
        } else {
            if (!z) {
                if (!this.mRestrictBackground || !z2) {
                }
            }
            i2 = 4;
        }
        int i6 = (i4 & 240) | i2;
        if (LOGV) {
            Log.v(TAG, "updateRuleForRestrictBackgroundUL(" + i + "): isForeground=" + zIsUidForegroundOnRestrictBackgroundUL + ", isBlacklisted=" + z + ", isWhitelisted=" + z2 + ", isRestrictedByAdmin=" + zIsRestrictedByAdminUL + ", oldRule=" + NetworkPolicyManager.uidRulesToString(i5) + ", newRule=" + NetworkPolicyManager.uidRulesToString(i2) + ", newUidRules=" + NetworkPolicyManager.uidRulesToString(i6) + ", oldUidRules=" + NetworkPolicyManager.uidRulesToString(i4));
        }
        if (i6 == 0) {
            this.mUidRules.delete(i);
        } else {
            this.mUidRules.put(i, i6);
        }
        if (i2 != i5) {
            if (hasRule(i2, 2)) {
                setMeteredNetworkWhitelist(i, true);
                if (z) {
                    setMeteredNetworkBlacklist(i, false);
                }
            } else if (hasRule(i5, 2)) {
                if (!z2) {
                    setMeteredNetworkWhitelist(i, false);
                }
                if (z || zIsRestrictedByAdminUL) {
                    setMeteredNetworkBlacklist(i, true);
                }
            } else if (hasRule(i2, 4) || hasRule(i5, 4)) {
                setMeteredNetworkBlacklist(i, z || zIsRestrictedByAdminUL);
                if (hasRule(i5, 4) && z2) {
                    setMeteredNetworkWhitelist(i, z2);
                }
            } else if (hasRule(i2, 1) || hasRule(i5, 1)) {
                setMeteredNetworkWhitelist(i, z2);
            } else {
                Log.wtf(TAG, "Unexpected change of metered UID state for " + i + ": foreground=" + zIsUidForegroundOnRestrictBackgroundUL + ", whitelisted=" + z2 + ", blacklisted=" + z + ", isRestrictedByAdmin=" + zIsRestrictedByAdminUL + ", newRule=" + NetworkPolicyManager.uidRulesToString(i6) + ", oldRule=" + NetworkPolicyManager.uidRulesToString(i4));
            }
            this.mHandler.obtainMessage(1, i, i6).sendToTarget();
        }
    }

    private void updateRulesForPowerRestrictionsUL(int i) {
        int iUpdateRulesForPowerRestrictionsUL = updateRulesForPowerRestrictionsUL(i, this.mUidRules.get(i, 0), false);
        if (iUpdateRulesForPowerRestrictionsUL == 0) {
            this.mUidRules.delete(i);
        } else {
            this.mUidRules.put(i, iUpdateRulesForPowerRestrictionsUL);
        }
    }

    private int updateRulesForPowerRestrictionsUL(int i, int i2, boolean z) {
        if (Trace.isTagEnabled(2097152L)) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateRulesForPowerRestrictionsUL: ");
            sb.append(i);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(i2);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(z ? "P" : "-");
            Trace.traceBegin(2097152L, sb.toString());
        }
        try {
            return updateRulesForPowerRestrictionsULInner(i, i2, z);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private int updateRulesForPowerRestrictionsULInner(int i, int i2, boolean z) {
        int i3 = 0;
        if (!isUidValidForBlacklistRules(i)) {
            if (LOGD) {
                Slog.d(TAG, "no need to update restrict power rules for uid " + i);
            }
            return 0;
        }
        boolean z2 = !z && isUidIdle(i);
        boolean z3 = z2 || this.mRestrictPower || this.mDeviceIdleMode;
        boolean zIsUidForegroundOnRestrictPowerUL = isUidForegroundOnRestrictPowerUL(i);
        boolean zIsWhitelistedBatterySaverUL = isWhitelistedBatterySaverUL(i, this.mDeviceIdleMode);
        int i4 = i2 & 240;
        if (zIsUidForegroundOnRestrictPowerUL) {
            if (z3) {
                i3 = 32;
            }
        } else if (z3) {
            if (!zIsWhitelistedBatterySaverUL) {
                i3 = 64;
            }
        }
        int i5 = (i2 & 15) | i3;
        if (LOGV) {
            Log.v(TAG, "updateRulesForPowerRestrictionsUL(" + i + "), isIdle: " + z2 + ", mRestrictPower: " + this.mRestrictPower + ", mDeviceIdleMode: " + this.mDeviceIdleMode + ", isForeground=" + zIsUidForegroundOnRestrictPowerUL + ", isWhitelisted=" + zIsWhitelistedBatterySaverUL + ", oldRule=" + NetworkPolicyManager.uidRulesToString(i4) + ", newRule=" + NetworkPolicyManager.uidRulesToString(i3) + ", newUidRules=" + NetworkPolicyManager.uidRulesToString(i5) + ", oldUidRules=" + NetworkPolicyManager.uidRulesToString(i2));
        }
        if (i3 != i4) {
            if (i3 == 0 || hasRule(i3, 32)) {
                if (LOGV) {
                    Log.v(TAG, "Allowing non-metered access for UID " + i);
                }
            } else if (hasRule(i3, 64)) {
                if (LOGV) {
                    Log.v(TAG, "Rejecting non-metered access for UID " + i);
                }
            } else {
                Log.wtf(TAG, "Unexpected change of non-metered UID state for " + i + ": foreground=" + zIsUidForegroundOnRestrictPowerUL + ", whitelisted=" + zIsWhitelistedBatterySaverUL + ", newRule=" + NetworkPolicyManager.uidRulesToString(i5) + ", oldRule=" + NetworkPolicyManager.uidRulesToString(i2));
            }
            this.mHandler.obtainMessage(1, i, i5).sendToTarget();
        }
        return i5;
    }

    private class AppIdleStateChangeListener extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        private AppIdleStateChangeListener() {
        }

        public void onAppIdleStateChanged(String str, int i, boolean z, int i2, int i3) {
            try {
                int packageUidAsUser = NetworkPolicyManagerService.this.mContext.getPackageManager().getPackageUidAsUser(str, 8192, i);
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.mLogger.appIdleStateChanged(packageUidAsUser, z);
                    NetworkPolicyManagerService.this.updateRuleForAppIdleUL(packageUidAsUser);
                    NetworkPolicyManagerService.this.updateRulesForPowerRestrictionsUL(packageUidAsUser);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        public void onParoleStateChanged(boolean z) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                NetworkPolicyManagerService.this.mLogger.paroleStateChanged(z);
                NetworkPolicyManagerService.this.updateRulesForAppIdleParoleUL();
            }
        }
    }

    private void dispatchUidRulesChanged(INetworkPolicyListener iNetworkPolicyListener, int i, int i2) {
        if (iNetworkPolicyListener != null) {
            try {
                iNetworkPolicyListener.onUidRulesChanged(i, i2);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchMeteredIfacesChanged(INetworkPolicyListener iNetworkPolicyListener, String[] strArr) {
        if (iNetworkPolicyListener != null) {
            try {
                iNetworkPolicyListener.onMeteredIfacesChanged(strArr);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchRestrictBackgroundChanged(INetworkPolicyListener iNetworkPolicyListener, boolean z) {
        if (iNetworkPolicyListener != null) {
            try {
                iNetworkPolicyListener.onRestrictBackgroundChanged(z);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchUidPoliciesChanged(INetworkPolicyListener iNetworkPolicyListener, int i, int i2) {
        if (iNetworkPolicyListener != null) {
            try {
                iNetworkPolicyListener.onUidPoliciesChanged(i, i2);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchSubscriptionOverride(INetworkPolicyListener iNetworkPolicyListener, int i, int i2, int i3) {
        if (iNetworkPolicyListener != null) {
            try {
                iNetworkPolicyListener.onSubscriptionOverride(i, i2, i3);
            } catch (RemoteException e) {
            }
        }
    }

    void handleUidChanged(int i, int i2, long j) {
        Trace.traceBegin(2097152L, "onUidStateChanged");
        try {
            synchronized (this.mUidRulesFirstLock) {
                this.mLogger.uidStateChanged(i, i2, j);
                updateUidStateUL(i, i2);
                this.mActivityManagerInternal.notifyNetworkPolicyRulesUpdated(i, j);
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void handleUidGone(int i) {
        Trace.traceBegin(2097152L, "onUidGone");
        try {
            synchronized (this.mUidRulesFirstLock) {
                removeUidStateUL(i);
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void broadcastRestrictBackgroundChanged(int i, Boolean bool) {
        String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i);
        if (packagesForUid != null) {
            int userId = UserHandle.getUserId(i);
            for (String str : packagesForUid) {
                Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                intent.setPackage(str);
                intent.setFlags(1073741824);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
            }
        }
    }

    private void setInterfaceQuotaAsync(String str, long j) {
        this.mHandler.obtainMessage(10, (int) (j >> 32), (int) (j & (-1)), str).sendToTarget();
    }

    private void setInterfaceQuota(String str, long j) {
        try {
            this.mNetworkManager.setInterfaceQuota(str, j);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.e(TAG, "problem setting interface quota", e2);
        }
    }

    private void removeInterfaceQuotaAsync(String str) {
        this.mHandler.obtainMessage(11, str).sendToTarget();
    }

    private void removeInterfaceQuota(String str) {
        try {
            this.mNetworkManager.removeInterfaceQuota(str);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.e(TAG, "problem removing interface quota", e2);
        }
    }

    private void setMeteredNetworkBlacklist(int i, boolean z) {
        if (LOGV) {
            Slog.v(TAG, "setMeteredNetworkBlacklist " + i + ": " + z);
        }
        try {
            this.mNetworkManager.setUidMeteredNetworkBlacklist(i, z);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.e(TAG, "problem setting blacklist (" + z + ") rules for " + i, e2);
        }
    }

    private void setMeteredNetworkWhitelist(int i, boolean z) {
        if (LOGV) {
            Slog.v(TAG, "setMeteredNetworkWhitelist " + i + ": " + z);
        }
        try {
            this.mNetworkManager.setUidMeteredNetworkWhitelist(i, z);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting whitelist (" + z + ") rules for " + i, e2);
        }
    }

    private void setUidFirewallRulesUL(int i, SparseIntArray sparseIntArray, int i2) {
        if (sparseIntArray != null) {
            setUidFirewallRulesUL(i, sparseIntArray);
        }
        if (i2 != 0) {
            enableFirewallChainUL(i, i2 == 1);
        }
    }

    private void setUidFirewallRulesUL(int i, SparseIntArray sparseIntArray) {
        try {
            int size = sparseIntArray.size();
            int[] iArr = new int[size];
            int[] iArr2 = new int[size];
            for (int i2 = size - 1; i2 >= 0; i2--) {
                iArr[i2] = sparseIntArray.keyAt(i2);
                iArr2[i2] = sparseIntArray.valueAt(i2);
            }
            this.mNetworkManager.setFirewallUidRules(i, iArr, iArr2);
            this.mLogger.firewallRulesChanged(i, iArr, iArr2);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting firewall uid rules", e2);
        }
    }

    private void setUidFirewallRule(int i, int i2, int i3) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "setUidFirewallRule: " + i + SliceClientPermissions.SliceAuthority.DELIMITER + i2 + SliceClientPermissions.SliceAuthority.DELIMITER + i3);
        }
        try {
            if (i == 1) {
                this.mUidFirewallDozableRules.put(i2, i3);
            } else if (i == 2) {
                this.mUidFirewallStandbyRules.put(i2, i3);
            } else if (i == 3) {
                this.mUidFirewallPowerSaveRules.put(i2, i3);
            }
            try {
                this.mNetworkManager.setFirewallUidRule(i, i2, i3);
                this.mLogger.uidFirewallRuleChanged(i, i2, i3);
            } catch (RemoteException e) {
            } catch (IllegalStateException e2) {
                Log.wtf(TAG, "problem setting firewall uid rules", e2);
            }
            Trace.traceEnd(2097152L);
        } catch (Throwable th) {
            Trace.traceEnd(2097152L);
            throw th;
        }
    }

    private void enableFirewallChainUL(int i, boolean z) {
        if (this.mFirewallChainStates.indexOfKey(i) >= 0 && this.mFirewallChainStates.get(i) == z) {
            return;
        }
        this.mFirewallChainStates.put(i, z);
        try {
            this.mNetworkManager.setFirewallChainEnabled(i, z);
            this.mLogger.firewallChainEnabled(i, z);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem enable firewall chain", e2);
        }
    }

    private void resetUidFirewallRules(int i) {
        try {
            this.mNetworkManager.setFirewallUidRule(1, i, 0);
            this.mNetworkManager.setFirewallUidRule(2, i, 0);
            this.mNetworkManager.setFirewallUidRule(3, i, 0);
            this.mNetworkManager.setUidMeteredNetworkWhitelist(i, false);
            this.mNetworkManager.setUidMeteredNetworkBlacklist(i, false);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem resetting firewall uid rules for " + i, e2);
        }
    }

    @Deprecated
    private long getTotalBytes(NetworkTemplate networkTemplate, long j, long j2) {
        return getNetworkTotalBytes(networkTemplate, j, j2);
    }

    private long getNetworkTotalBytes(NetworkTemplate networkTemplate, long j, long j2) {
        try {
            return this.mNetworkStats.getNetworkTotalBytes(networkTemplate, j, j2);
        } catch (RuntimeException e) {
            Slog.w(TAG, "Failed to read network stats: " + e);
            return 0L;
        }
    }

    private NetworkStats getNetworkUidBytes(NetworkTemplate networkTemplate, long j, long j2) {
        try {
            return this.mNetworkStats.getNetworkUidBytes(networkTemplate, j, j2);
        } catch (RuntimeException e) {
            Slog.w(TAG, "Failed to read network stats: " + e);
            return new NetworkStats(SystemClock.elapsedRealtime(), 0);
        }
    }

    private boolean isBandwidthControlEnabled() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mNetworkManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static Intent buildAllowBackgroundDataIntent() {
        return new Intent(ACTION_ALLOW_BACKGROUND);
    }

    private static Intent buildSnoozeWarningIntent(NetworkTemplate networkTemplate) {
        Intent intent = new Intent(ACTION_SNOOZE_WARNING);
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) networkTemplate);
        return intent;
    }

    private static Intent buildSnoozeRapidIntent(NetworkTemplate networkTemplate) {
        Intent intent = new Intent(ACTION_SNOOZE_RAPID);
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) networkTemplate);
        return intent;
    }

    private static Intent buildNetworkOverLimitIntent(Resources resources, NetworkTemplate networkTemplate) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(resources.getString(R.string.android_upgrading_title)));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) networkTemplate);
        return intent;
    }

    private static Intent buildViewDataUsageIntent(Resources resources, NetworkTemplate networkTemplate) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(resources.getString(R.string.accessibility_system_action_power_dialog_label)));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) networkTemplate);
        return intent;
    }

    @VisibleForTesting
    public void addIdleHandler(MessageQueue.IdleHandler idleHandler) {
        this.mHandler.getLooper().getQueue().addIdleHandler(idleHandler);
    }

    @VisibleForTesting
    public void updateRestrictBackgroundByLowPowerModeUL(PowerSaveState powerSaveState) {
        boolean z;
        this.mRestrictBackgroundPowerState = powerSaveState;
        boolean z2 = powerSaveState.batterySaverEnabled;
        boolean z3 = this.mRestrictBackgroundChangedInBsm;
        if (powerSaveState.globalBatterySaverEnabled) {
            z = !this.mRestrictBackground && powerSaveState.batterySaverEnabled;
            this.mRestrictBackgroundBeforeBsm = this.mRestrictBackground;
            z3 = false;
        } else {
            z = true ^ this.mRestrictBackgroundChangedInBsm;
            z2 = this.mRestrictBackgroundBeforeBsm;
        }
        if (z) {
            setRestrictBackgroundUL(z2);
        }
        this.mRestrictBackgroundChangedInBsm = z3;
    }

    private static void collectKeys(SparseIntArray sparseIntArray, SparseBooleanArray sparseBooleanArray) {
        int size = sparseIntArray.size();
        for (int i = 0; i < size; i++) {
            sparseBooleanArray.put(sparseIntArray.keyAt(i), true);
        }
    }

    public void factoryReset(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        NetworkPolicy[] networkPolicies = getNetworkPolicies(this.mContext.getOpPackageName());
        NetworkTemplate networkTemplateBuildTemplateMobileAll = NetworkTemplate.buildTemplateMobileAll(str);
        for (NetworkPolicy networkPolicy : networkPolicies) {
            if (networkPolicy.template.equals(networkTemplateBuildTemplateMobileAll)) {
                networkPolicy.limitBytes = -1L;
                networkPolicy.inferred = false;
                networkPolicy.clearSnooze();
            }
        }
        setNetworkPolicies(networkPolicies);
        setRestrictBackground(false);
        if (!this.mUserManager.hasUserRestriction("no_control_apps")) {
            for (int i : getUidsWithPolicy(1)) {
                setUidPolicy(i, 0);
            }
        }
    }

    public boolean isUidNetworkingBlocked(int i, boolean z) {
        long time = this.mStatLogger.getTime();
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        boolean zIsUidNetworkingBlockedInternal = isUidNetworkingBlockedInternal(i, z);
        this.mStatLogger.logDurationStat(1, time);
        return zIsUidNetworkingBlockedInternal;
    }

    private boolean isUidNetworkingBlockedInternal(int i, boolean z) {
        int i2;
        boolean z2;
        synchronized (this.mUidRulesFirstLock) {
            i2 = this.mUidRules.get(i, 0);
            z2 = this.mRestrictBackground;
        }
        if (hasRule(i2, 64)) {
            this.mLogger.networkBlocked(i, 0);
            return true;
        }
        if (!z) {
            this.mLogger.networkBlocked(i, 1);
            return false;
        }
        if (hasRule(i2, 4)) {
            this.mLogger.networkBlocked(i, 2);
            return true;
        }
        if (hasRule(i2, 1)) {
            this.mLogger.networkBlocked(i, 3);
            return false;
        }
        if (hasRule(i2, 2)) {
            this.mLogger.networkBlocked(i, 4);
            return false;
        }
        if (z2) {
            this.mLogger.networkBlocked(i, 5);
            return true;
        }
        this.mLogger.networkBlocked(i, 6);
        return false;
    }

    private class NetworkPolicyManagerInternalImpl extends NetworkPolicyManagerInternal {
        private NetworkPolicyManagerInternalImpl() {
        }

        @Override
        public void resetUserState(int i) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                boolean z = false;
                boolean zRemoveUserStateUL = NetworkPolicyManagerService.this.removeUserStateUL(i, false);
                if (NetworkPolicyManagerService.this.addDefaultRestrictBackgroundWhitelistUidsUL(i) || zRemoveUserStateUL) {
                    z = true;
                }
                if (z) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.writePolicyAL();
                    }
                }
            }
        }

        @Override
        public boolean isUidRestrictedOnMeteredNetworks(int i) {
            int i2;
            boolean z;
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                i2 = NetworkPolicyManagerService.this.mUidRules.get(i, 32);
                z = NetworkPolicyManagerService.this.mRestrictBackground;
            }
            return (!z || NetworkPolicyManagerService.hasRule(i2, 1) || NetworkPolicyManagerService.hasRule(i2, 2)) ? false : true;
        }

        @Override
        public boolean isUidNetworkingBlocked(int i, String str) {
            boolean zContains;
            long time = NetworkPolicyManagerService.this.mStatLogger.getTime();
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                zContains = NetworkPolicyManagerService.this.mMeteredIfaces.contains(str);
            }
            boolean zIsUidNetworkingBlockedInternal = NetworkPolicyManagerService.this.isUidNetworkingBlockedInternal(i, zContains);
            NetworkPolicyManagerService.this.mStatLogger.logDurationStat(1, time);
            return zIsUidNetworkingBlockedInternal;
        }

        @Override
        public void onTempPowerSaveWhitelistChange(int i, boolean z) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                NetworkPolicyManagerService.this.mLogger.tempPowerSaveWlChanged(i, z);
                if (z) {
                    NetworkPolicyManagerService.this.mPowerSaveTempWhitelistAppIds.put(i, true);
                } else {
                    NetworkPolicyManagerService.this.mPowerSaveTempWhitelistAppIds.delete(i);
                }
                NetworkPolicyManagerService.this.updateRulesForTempWhitelistChangeUL(i);
            }
        }

        @Override
        public SubscriptionPlan getSubscriptionPlan(Network network) {
            SubscriptionPlan primarySubscriptionPlanLocked;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                primarySubscriptionPlanLocked = NetworkPolicyManagerService.this.getPrimarySubscriptionPlanLocked(NetworkPolicyManagerService.this.getSubIdLocked(network));
            }
            return primarySubscriptionPlanLocked;
        }

        @Override
        public SubscriptionPlan getSubscriptionPlan(NetworkTemplate networkTemplate) {
            SubscriptionPlan primarySubscriptionPlanLocked;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                primarySubscriptionPlanLocked = NetworkPolicyManagerService.this.getPrimarySubscriptionPlanLocked(NetworkPolicyManagerService.this.findRelevantSubIdNL(networkTemplate));
            }
            return primarySubscriptionPlanLocked;
        }

        @Override
        public long getSubscriptionOpportunisticQuota(Network network, int i) {
            long j;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                j = NetworkPolicyManagerService.this.mSubscriptionOpportunisticQuota.get(NetworkPolicyManagerService.this.getSubIdLocked(network), -1L);
            }
            if (j == -1) {
                return -1L;
            }
            if (i == 1) {
                return (long) (j * Settings.Global.getFloat(NetworkPolicyManagerService.this.mContext.getContentResolver(), "netpolicy_quota_frac_jobs", 0.5f));
            }
            if (i == 2) {
                return (long) (j * Settings.Global.getFloat(NetworkPolicyManagerService.this.mContext.getContentResolver(), "netpolicy_quota_frac_multipath", 0.5f));
            }
            return -1L;
        }

        @Override
        public void onAdminDataAvailable() {
            NetworkPolicyManagerService.this.mAdminDataAvailableLatch.countDown();
        }

        @Override
        public void setMeteredRestrictedPackages(Set<String> set, int i) {
            NetworkPolicyManagerService.this.setMeteredRestrictedPackagesInternal(set, i);
        }

        @Override
        public void setMeteredRestrictedPackagesAsync(Set<String> set, int i) {
            NetworkPolicyManagerService.this.mHandler.obtainMessage(17, i, 0, set).sendToTarget();
        }
    }

    private void setMeteredRestrictedPackagesInternal(Set<String> set, int i) {
        synchronized (this.mUidRulesFirstLock) {
            ArraySet arraySet = new ArraySet();
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                int uidForPackage = getUidForPackage(it.next(), i);
                if (uidForPackage >= 0) {
                    arraySet.add(Integer.valueOf(uidForPackage));
                }
            }
            Set<Integer> set2 = this.mMeteredRestrictedUids.get(i);
            this.mMeteredRestrictedUids.put(i, arraySet);
            handleRestrictedPackagesChangeUL(set2, arraySet);
            this.mLogger.meteredRestrictedPkgsChanged(arraySet);
        }
    }

    private int getUidForPackage(String str, int i) {
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(str, 4202496, i);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private int parseSubId(NetworkState networkState) {
        if (networkState != null && networkState.networkCapabilities != null && networkState.networkCapabilities.hasTransport(0)) {
            StringNetworkSpecifier networkSpecifier = networkState.networkCapabilities.getNetworkSpecifier();
            if (networkSpecifier instanceof StringNetworkSpecifier) {
                try {
                    return Integer.parseInt(networkSpecifier.specifier);
                } catch (NumberFormatException e) {
                }
            }
        }
        return -1;
    }

    @GuardedBy("mNetworkPoliciesSecondLock")
    private int getSubIdLocked(Network network) {
        return this.mNetIdToSubId.get(network.netId, -1);
    }

    @GuardedBy("mNetworkPoliciesSecondLock")
    private SubscriptionPlan getPrimarySubscriptionPlanLocked(int i) {
        SubscriptionPlan[] subscriptionPlanArr = this.mSubscriptionPlans.get(i);
        if (!ArrayUtils.isEmpty(subscriptionPlanArr)) {
            for (SubscriptionPlan subscriptionPlan : subscriptionPlanArr) {
                if (subscriptionPlan.getCycleRule().isRecurring()) {
                    return subscriptionPlan;
                }
                if (subscriptionPlan.cycleIterator().next().contains(ZonedDateTime.now(this.mClock))) {
                    return subscriptionPlan;
                }
            }
            return null;
        }
        return null;
    }

    private void waitForAdminData() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin")) {
            ConcurrentUtils.waitForCountDownNoInterrupt(this.mAdminDataAvailableLatch, 10000L, "Wait for admin data");
        }
    }

    private void handleRestrictedPackagesChangeUL(Set<Integer> set, Set<Integer> set2) {
        if (set == null) {
            Iterator<Integer> it = set2.iterator();
            while (it.hasNext()) {
                updateRulesForDataUsageRestrictionsUL(it.next().intValue());
            }
            return;
        }
        Iterator<Integer> it2 = set.iterator();
        while (it2.hasNext()) {
            int iIntValue = it2.next().intValue();
            if (!set2.contains(Integer.valueOf(iIntValue))) {
                updateRulesForDataUsageRestrictionsUL(iIntValue);
            }
        }
        Iterator<Integer> it3 = set2.iterator();
        while (it3.hasNext()) {
            int iIntValue2 = it3.next().intValue();
            if (!set.contains(Integer.valueOf(iIntValue2))) {
                updateRulesForDataUsageRestrictionsUL(iIntValue2);
            }
        }
    }

    private boolean isRestrictedByAdminUL(int i) {
        Set<Integer> set = this.mMeteredRestrictedUids.get(UserHandle.getUserId(i));
        return set != null && set.contains(Integer.valueOf(i));
    }

    private static boolean hasRule(int i, int i2) {
        return (i & i2) != 0;
    }

    private static NetworkState[] defeatNullable(NetworkState[] networkStateArr) {
        return networkStateArr != null ? networkStateArr : new NetworkState[0];
    }

    private static boolean getBooleanDefeatingNullable(PersistableBundle persistableBundle, String str, boolean z) {
        return persistableBundle != null ? persistableBundle.getBoolean(str, z) : z;
    }

    private class NotificationId {
        private final int mId;
        private final String mTag;

        NotificationId(NetworkPolicy networkPolicy, int i) {
            this.mTag = buildNotificationTag(networkPolicy, i);
            this.mId = i;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NotificationId) {
                return Objects.equals(this.mTag, ((NotificationId) obj).mTag);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mTag);
        }

        private String buildNotificationTag(NetworkPolicy networkPolicy, int i) {
            return "NetworkPolicy:" + networkPolicy.template.hashCode() + ":" + i;
        }

        public String getTag() {
            return this.mTag;
        }

        public int getId() {
            return this.mId;
        }
    }
}
