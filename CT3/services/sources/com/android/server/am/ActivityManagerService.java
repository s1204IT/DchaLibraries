package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.app.ApplicationThreadNative;
import android.app.BroadcastOptions;
import android.app.Dialog;
import android.app.IActivityContainer;
import android.app.IActivityContainerCallback;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.IAppTask;
import android.app.IApplicationThread;
import android.app.IInstrumentationWatcher;
import android.app.INotificationManager;
import android.app.IProcessObserver;
import android.app.IServiceConnection;
import android.app.IStopUserCallback;
import android.app.ITaskStackListener;
import android.app.IUiAutomationConnection;
import android.app.IUidObserver;
import android.app.IUserSwitchObserver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProfilerInfo;
import android.app.admin.DevicePolicyManager;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.app.backup.IBackupManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.PathPermission;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.IPermissionController;
import android.os.IProcessInfoService;
import android.os.IProgressListener;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.TransactionTooLargeException;
import android.os.UpdateLock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.os.storage.IMountService;
import android.os.storage.MountServiceInternal;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionManagerInternal;
import android.telecom.TelecomManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DebugUtils;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.DumpHeapActivity;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.ProcessMap;
import com.android.internal.app.SystemUserHomeActivity;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.IResultReceiver;
import com.android.internal.os.InstallerConnection;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.MemInfoReader;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.AppOpsService;
import com.android.server.AttributeCache;
import com.android.server.DeviceIdleController;
import com.android.server.IntentResolver;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.am.ActiveServices;
import com.android.server.am.ActivityStack;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.am.PendingIntentRecord;
import com.android.server.am.UidRecord;
import com.android.server.am.UriPermission;
import com.android.server.firewall.IntentFirewall;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationFudger;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.vr.VrManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.mediatek.aal.AalUtils;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import com.mediatek.am.IAWSProcessRecord;
import com.mediatek.am.IAWSStoreRecord;
import com.mediatek.anrmanager.ANRManager;
import com.mediatek.anrmanager.ANRManager.DumpThread;
import com.mediatek.appworkingset.AWSManager;
import com.mediatek.common.MPlugin;
import com.mediatek.common.amsplus.ICustomizedOomExt;
import com.mediatek.cta.CtaUtils;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.multiwindow.MultiWindowManager;
import com.mediatek.server.am.AMEventHook;
import com.mediatek.server.am.AutoBootControl.ReceiverController;
import com.mediatek.server.am.BootEvent;
import com.mediatek.suppression.service.SuppressionInternal;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class ActivityManagerService extends ActivityManagerNative implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
    public static final String ACTION_TRIGGER_IDLE = "com.android.server.ACTION_TRIGGER_IDLE";
    static final int ALLOW_FULL_ONLY = 2;
    static final int ALLOW_NON_FULL = 0;
    static final int ALLOW_NON_FULL_IN_PROFILE = 1;
    static final boolean ANIMATE = true;
    static final int APP_BOOST_DEACTIVATE_MSG = 58;
    static final int APP_BOOST_MESSAGE_DELAY = 3000;
    static final int APP_BOOST_TIMEOUT = 2500;
    static final long APP_SWITCH_DELAY_TIME = 5000;
    private static final String ATTR_CREATED_TIME = "createdTime";
    private static final String ATTR_MODE_FLAGS = "modeFlags";
    private static final String ATTR_PREFIX = "prefix";
    private static final String ATTR_SOURCE_PKG = "sourcePkg";
    private static final String ATTR_SOURCE_USER_ID = "sourceUserId";
    private static final String ATTR_TARGET_PKG = "targetPkg";
    private static final String ATTR_TARGET_USER_ID = "targetUserId";
    private static final String ATTR_URI = "uri";
    private static final String ATTR_USER_HANDLE = "userHandle";
    static final int BACKGROUND_SETTLE_TIME = 60000;
    static final long BATTERY_STATS_TIME = 1800000;
    static final int BROADCAST_BG_TIMEOUT = 60000;
    static final int BROADCAST_FG_TIMEOUT = 10000;
    static final int CANCEL_HEAVY_NOTIFICATION_MSG = 25;
    static final int CHECK_EXCESSIVE_WAKE_LOCKS_MSG = 27;
    static final int CLEAR_DNS_CACHE_MSG = 28;
    static final int COLLECT_PSS_BG_MSG = 1;
    static final int COLLECT_PSS_FG_MSG = 2;
    static final int CONTENT_PROVIDER_PUBLISH_TIMEOUT = 10000;
    static final int CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG = 59;
    static final int CONTENT_PROVIDER_RETAIN_TIME = 20000;
    static final int CONTINUE_USER_SWITCH_MSG = 35;
    static final int CPU_MIN_CHECK_DURATION;
    static final int DELETE_DUMPHEAP_MSG = 52;
    static final int DISMISS_DIALOG_UI_MSG = 48;
    static final int DISPATCH_PROCESSES_CHANGED_UI_MSG = 31;
    static final int DISPATCH_PROCESS_DIED_UI_MSG = 32;
    static final int DISPATCH_UIDS_CHANGED_UI_MSG = 54;
    static final int DO_PENDING_ACTIVITY_LAUNCHES_MSG = 21;
    static final int DROPBOX_MAX_SIZE = 196608;
    static final long[] DUMP_MEM_BUCKETS;
    static final int[] DUMP_MEM_OOM_ADJ;
    static final String[] DUMP_MEM_OOM_COMPACT_LABEL;
    static final String[] DUMP_MEM_OOM_LABEL;
    static final String[] EMPTY_STRING_ARRAY;
    static final int ENTER_ANIMATION_COMPLETE_MSG = 44;
    static final int FINALIZE_PENDING_INTENT_MSG = 23;
    static final int FINISH_BOOTING_MSG = 45;
    static final int FIRST_ACTIVITY_STACK_MSG = 100;
    static final int FIRST_BROADCAST_QUEUE_MSG = 200;
    static final int FIRST_COMPAT_MODE_MSG = 300;
    static final int FIRST_SUPERVISOR_STACK_MSG = 100;
    static final int FOREGROUND_PROFILE_CHANGED_MSG = 53;
    public static final float FULLSCREEN_SCREENSHOT_SCALE = 0.6f;
    static final int FULL_PSS_LOWERED_INTERVAL = 120000;
    static final int FULL_PSS_MIN_INTERVAL = 600000;
    static final int GC_BACKGROUND_PROCESSES_MSG = 5;
    static final int GC_MIN_INTERVAL = 60000;
    static final int GC_TIMEOUT = 5000;
    static final int IDLE_UIDS_MSG = 60;
    static final int IMMERSIVE_MODE_LOCK_MSG = 37;
    static final int INSTRUMENTATION_KEY_DISPATCHING_TIMEOUT = 60000;
    private static final String INTENT_REMOTE_BUGREPORT_FINISHED = "android.intent.action.REMOTE_BUGREPORT_FINISHED";
    static final boolean IS_ENG_BUILD;
    static final boolean IS_USER_BUILD;
    static final int KEY_DISPATCHING_TIMEOUT = 30000;
    static final int KILL_APPLICATION_MSG = 22;
    private static final int KSM_SHARED = 0;
    private static final int KSM_SHARING = 1;
    private static final int KSM_UNSHARED = 2;
    private static final int KSM_VOLATILE = 3;
    static final int LOCK_SCREEN_HIDDEN = 0;
    static final int LOCK_SCREEN_LEAVING = 1;
    static final int LOCK_SCREEN_SHOWN = 2;
    static final int LOG_STACK_STATE = 62;
    private static final int MAX_DUP_SUPPRESSED_STACKS = 5000;
    static final int MAX_PERSISTED_URI_GRANTS = 128;
    private static final int MEMINFO_COMPACT_VERSION = 1;
    static final long MONITOR_CPU_MAX_TIME = 268435455;
    static final long MONITOR_CPU_MIN_TIME = 5000;
    static final boolean MONITOR_CPU_USAGE = true;
    static final boolean MONITOR_THREAD_CPU_USAGE = false;
    static final int MY_PID;
    static final int NOTIFY_ACTIVITY_DISMISSING_DOCKED_STACK_MSG = 68;
    static final int NOTIFY_ACTIVITY_PINNED_LISTENERS_MSG = 64;
    static final int NOTIFY_CLEARTEXT_NETWORK_MSG = 50;
    static final int NOTIFY_FORCED_RESIZABLE_MSG = 67;
    static final int NOTIFY_PINNED_ACTIVITY_RESTART_ATTEMPT_LISTENERS_MSG = 65;
    static final int NOTIFY_PINNED_STACK_ANIMATION_ENDED_LISTENERS_MSG = 66;
    static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_DELAY = 100;
    static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_MSG = 49;
    static final int PENDING_ASSIST_EXTRAS_LONG_TIMEOUT = 2000;
    static final int PENDING_ASSIST_EXTRAS_TIMEOUT = 500;
    private static final int PERSISTENT_MASK = 9;
    static final int PERSIST_URI_GRANTS_MSG = 38;
    static final int POST_DUMP_HEAP_NOTIFICATION_MSG = 51;
    static final int POST_HEAVY_NOTIFICATION_MSG = 24;
    static final int POWER_CHECK_DELAY;
    private static final int[] PROCESS_STATE_STATS_FORMAT;
    static final int PROC_START_TIMEOUT = 10000;
    static final int PROC_START_TIMEOUT_MSG = 20;
    static final int PROC_START_TIMEOUT_WITH_WRAPPER = 1200000;
    private static final boolean REMOVE_FROM_RECENTS = true;
    static final int REPORT_MEM_USAGE_MSG = 33;
    static final int REPORT_TIME_TRACKER_MSG = 55;
    static final int REPORT_USER_SWITCH_COMPLETE_MSG = 56;
    static final int REPORT_USER_SWITCH_MSG = 34;
    static final int REQUEST_ALL_PSS_MSG = 39;
    static final int RESERVED_BYTES_PER_LOGCAT_LINE = 100;
    static final int SEND_LOCALE_TO_MOUNT_DAEMON_MSG = 47;
    static final int SERVICE_TIMEOUT_MSG = 12;
    static final int SERVICE_USAGE_INTERACTION_TIME = 1800000;
    static final int SHOW_COMPAT_MODE_DIALOG_UI_MSG = 30;
    static final int SHOW_ERROR_UI_MSG = 1;
    static final int SHOW_FACTORY_ERROR_UI_MSG = 3;
    static final int SHOW_FINGERPRINT_ERROR_UI_MSG = 15;
    static final int SHOW_NOT_RESPONDING_UI_MSG = 2;
    static final int SHOW_STRICT_MODE_VIOLATION_UI_MSG = 26;
    static final int SHOW_UID_ERROR_UI_MSG = 14;
    static final int SHOW_UNSUPPORTED_DISPLAY_SIZE_DIALOG_MSG = 70;
    static final int SHUTDOWN_UI_AUTOMATION_CONNECTION_MSG = 57;
    static final int START_PROFILES_MSG = 40;
    static final int START_USER_SWITCH_UI_MSG = 46;
    static final int STOCK_PM_FLAGS = 1024;
    static final int SUPPRESS_ACTION_ALLOWED = 0;
    static final int SUPPRESS_ACTION_DELAYED = 1;
    static final int SUPPRESS_ACTION_SKIPPED = 2;
    static final int SUPPRESS_ACTION_STOP = 1;
    static final int SUPPRESS_ACTION_UNSTOP = 0;
    static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    static final int SYSTEM_USER_CURRENT_MSG = 43;
    static final int SYSTEM_USER_START_MSG = 42;
    static final int SYSTEM_USER_UNLOCK_MSG = 61;
    private static final String TAG_URI_GRANT = "uri-grant";
    private static final String TAG_URI_GRANTS = "uri-grants";
    static final boolean TAKE_FULLSCREEN_SCREENSHOTS = true;
    static final int UPDATE_CONFIGURATION_MSG = 4;
    static final int UPDATE_HTTP_PROXY_MSG = 29;
    static final int UPDATE_TIME = 41;
    static final int UPDATE_TIME_ZONE = 13;
    static final long USAGE_STATS_INTERACTION_INTERVAL = 86400000;
    static final int USER_SWITCH_TIMEOUT_MSG = 36;
    static final boolean VALIDATE_UID_STATES = true;
    static final int VR_MODE_APPLY_IF_NEEDED_MSG = 69;
    static final int VR_MODE_CHANGE_MSG = 63;
    static final int WAIT_FOR_DEBUGGER_UI_MSG = 6;
    static final int WAKE_LOCK_MIN_CHECK_DURATION;
    static ANRManager mANRManager;
    static final ArrayList<Integer> mInterestingPids;
    private static final ThreadLocal<Identity> sCallerIdentity;
    static ThreadLocal<Integer> sIsBoosted;
    static KillHandler sKillHandler;
    static ServiceThread sKillThread;
    final int GL_ES_VERSION;
    final ActivityStarter mActivityStarter;
    ANRManager.AnrDumpMgr mAnrDumpMgr;
    ANRManager.AnrMonitorHandler mAnrHandler;
    HashMap<String, IBinder> mAppBindArgs;
    final AppErrors mAppErrors;
    final AppOpsService mAppOpsService;
    long mAppSwitchesAllowedTime;
    final BatteryStatsService mBatteryStatsService;
    BroadcastQueue mBgBroadcastQueue;
    CompatModeDialog mCompatModeDialog;
    final CompatModePackages mCompatModePackages;
    int mConfigurationSeq;
    Context mContext;
    CoreSettingsObserver mCoreSettingsObserver;
    private AppTimeTracker mCurAppTimeTracker;
    BroadcastStats mCurBroadcastStats;
    Rect mDefaultPinnedStackBounds;
    String mDeviceOwnerName;
    boolean mDidAppSwitch;
    boolean mDidDexOpt;
    boolean mDoingSetFocusedActivity;
    BroadcastQueue mFgBroadcastQueue;
    FontScaleSettingObserver mFontScaleSettingObserver;
    boolean mForceResizableActivities;
    float mFullscreenThumbnailScale;
    private final AtomicFile mGrantFile;
    final MainHandler mHandler;
    final ServiceThread mHandlerThread;
    boolean mHasRecents;
    ProcessRecord mHomeProcess;
    private Installer mInstaller;
    public IntentFirewall mIntentFirewall;
    boolean mIsWallpaperFg;
    HashMap<String, IBinder> mIsolatedAppBindArgs;
    ActivityInfo mLastAddedTaskActivity;
    ComponentName mLastAddedTaskComponent;
    int mLastAddedTaskUid;
    BroadcastStats mLastBroadcastStats;
    private int mLastFocusedUserId;
    int mLastNumProcesses;
    long mLastPowerCheckRealtime;
    long mLastPowerCheckUptime;
    DeviceIdleController.LocalService mLocalDeviceIdleController;
    PowerManagerInternal mLocalPowerManager;
    String mMemWatchDumpFile;
    int mMemWatchDumpPid;
    String mMemWatchDumpProcName;
    int mMemWatchDumpUid;
    volatile boolean mOnBattery;
    PackageManagerInternal mPackageManagerInt;
    ProcessRecord mPreviousProcess;
    long mPreviousProcessVisibleTime;
    final Thread mProcessCpuThread;
    final ProcessStatsService mProcessStats;
    ParcelFileDescriptor mProfileFd;
    String mProfileFile;
    final ProviderMap mProviderMap;
    final RecentTasks mRecentTasks;
    private IVoiceInteractionSession mRunningVoice;
    boolean mSafeMode;
    final ActiveServices mServices;
    final ActivityStackSupervisor mStackSupervisor;
    boolean mSupportsFreeformWindowManagement;
    boolean mSupportsLeanbackOnly;
    boolean mSupportsMultiWindow;
    boolean mSupportsPictureInPicture;
    SystemServiceManager mSystemServiceManager;
    int mThumbnailHeight;
    int mThumbnailWidth;
    ComponentName mTopComponent;
    String mTopData;
    boolean mTrackingAssociations;
    final UiHandler mUiHandler;
    UnsupportedDisplaySizeDialog mUnsupportedDisplaySizeDialog;
    UsageStatsManagerInternal mUsageStatsService;
    final UserController mUserController;
    private boolean mUserIsMonkey;
    PowerManager.WakeLock mVoiceWakeLock;
    ComponentName mWallpaperClassName;
    ProcessRecord mWallpaperProcess;
    WindowManagerService mWindowManager;
    private volatile int mWtfClusterCount;
    private volatile long mWtfClusterStart;
    private static final String TAG = "ActivityManager";
    private static final String TAG_BACKUP = TAG + ActivityManagerDebugConfig.POSTFIX_BACKUP;
    private static final String TAG_BROADCAST = TAG + ActivityManagerDebugConfig.POSTFIX_BROADCAST;
    private static final String TAG_CLEANUP = TAG + ActivityManagerDebugConfig.POSTFIX_CLEANUP;
    private static final String TAG_CONFIGURATION = TAG + ActivityManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_FOCUS = TAG + ActivityManagerDebugConfig.POSTFIX_FOCUS;
    private static final String TAG_IMMERSIVE = TAG + ActivityManagerDebugConfig.POSTFIX_IMMERSIVE;
    private static final String TAG_LOCKSCREEN = TAG + ActivityManagerDebugConfig.POSTFIX_LOCKSCREEN;
    private static final String TAG_LOCKTASK = TAG + ActivityManagerDebugConfig.POSTFIX_LOCKTASK;
    private static final String TAG_LRU = TAG + ActivityManagerDebugConfig.POSTFIX_LRU;
    private static final String TAG_MU = TAG + "_MU";
    private static final String TAG_OOM_ADJ = TAG + ActivityManagerDebugConfig.POSTFIX_OOM_ADJ;
    private static final String TAG_POWER = TAG + ActivityManagerDebugConfig.POSTFIX_POWER;
    private static final String TAG_PROCESS_OBSERVERS = TAG + ActivityManagerDebugConfig.POSTFIX_PROCESS_OBSERVERS;
    private static final String TAG_PROCESSES = TAG + ActivityManagerDebugConfig.POSTFIX_PROCESSES;
    private static final String TAG_PROVIDER = TAG + ActivityManagerDebugConfig.POSTFIX_PROVIDER;
    private static final String TAG_PSS = TAG + ActivityManagerDebugConfig.POSTFIX_PSS;
    private static final String TAG_RECENTS = TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_SERVICE = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE;
    private static final String TAG_STACK = TAG + ActivityManagerDebugConfig.POSTFIX_STACK;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_UID_OBSERVERS = TAG + ActivityManagerDebugConfig.POSTFIX_UID_OBSERVERS;
    private static final String TAG_URI_PERMISSION = TAG + ActivityManagerDebugConfig.POSTFIX_URI_PERMISSION;
    private static final String TAG_VISIBILITY = TAG + ActivityManagerDebugConfig.POSTFIX_VISIBILITY;
    private static final String TAG_VISIBLE_BEHIND = TAG + ActivityManagerDebugConfig.POSTFIX_VISIBLE_BEHIND;
    static final boolean IS_USER_DEBUG_BUILD = "userdebug".equals(Build.TYPE);
    private boolean mIsBoosted = false;
    private long mBoostStartTime = 0;
    private final RemoteCallbackList<ITaskStackListener> mTaskStackListeners = new RemoteCallbackList<>();
    final InstrumentationReporter mInstrumentationReporter = new InstrumentationReporter();
    private boolean mShowDialogs = true;
    private boolean mInVrMode = false;
    final BroadcastQueue[] mBroadcastQueues = new BroadcastQueue[2];
    ActivityRecord mFocusedActivity = null;
    SparseArray<String[]> mLockTaskPackages = new SparseArray<>();
    final ArrayList<PendingAssistExtras> mPendingAssistExtras = new ArrayList<>();
    final ProcessList mProcessList = new ProcessList();
    final ProcessMap<ProcessRecord> mProcessNames = new ProcessMap<>();
    final SparseArray<ProcessRecord> mIsolatedProcesses = new SparseArray<>();
    int mNextIsolatedProcessUid = 0;
    ProcessRecord mHeavyWeightProcess = null;
    final SparseArray<ProcessRecord> mPidsSelfLocked = new SparseArray<>();
    final SparseArray<ForegroundToken> mForegroundProcesses = new SparseArray<>();
    final ArrayList<ProcessRecord> mProcessesOnHold = new ArrayList<>();
    final ArrayList<ProcessRecord> mPersistentStartingProcesses = new ArrayList<>();
    final ArrayList<ProcessRecord> mRemovedProcesses = new ArrayList<>();
    final ArrayList<ProcessRecord> mLruProcesses = new ArrayList<>();
    int mLruProcessActivityStart = 0;
    int mLruProcessServiceStart = 0;
    final ArrayList<ProcessRecord> mProcessesToGc = new ArrayList<>();
    final ArrayList<ProcessRecord> mPendingPssProcesses = new ArrayList<>();
    private boolean mBinderTransactionTrackingEnabled = false;
    long mLastFullPssTime = SystemClock.uptimeMillis();
    boolean mFullPssPending = false;
    final SparseArray<UidRecord> mActiveUids = new SparseArray<>();
    final SparseArray<UidRecord> mValidateUids = new SparseArray<>();
    final HashMap<PendingIntentRecord.Key, WeakReference<PendingIntentRecord>> mIntentSenderRecords = new HashMap<>();
    private final HashSet<Integer> mAlreadyLoggedViolatedStacks = new HashSet<>();
    private final StringBuilder mStrictModeBuffer = new StringBuilder();
    final HashMap<IBinder, ReceiverList> mRegisteredReceivers = new HashMap<>();
    final IntentResolver<BroadcastFilter, BroadcastFilter> mReceiverResolver = new IntentResolver<BroadcastFilter, BroadcastFilter>() {
        @Override
        protected boolean allowFilterResult(BroadcastFilter filter, List<BroadcastFilter> dest) {
            IBinder target = filter.receiverList.receiver.asBinder();
            for (int i = dest.size() - 1; i >= 0; i--) {
                if (dest.get(i).receiverList.receiver.asBinder() == target) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected BroadcastFilter newResult(BroadcastFilter filter, int match, int userId) {
            if (userId == -1 || filter.owningUserId == -1 || userId == filter.owningUserId) {
                return (BroadcastFilter) super.newResult(filter, match, userId);
            }
            return null;
        }

        @Override
        protected BroadcastFilter[] newArray(int size) {
            return new BroadcastFilter[size];
        }

        @Override
        protected boolean isPackageForFilter(String packageName, BroadcastFilter filter) {
            return packageName.equals(filter.packageName);
        }
    };
    final SparseArray<ArrayMap<String, ArrayList<Intent>>> mStickyBroadcasts = new SparseArray<>();
    final SparseArray<ArrayMap<ComponentName, SparseArray<ArrayMap<String, Association>>>> mAssociations = new SparseArray<>();
    String mBackupAppName = null;
    BackupRecord mBackupTarget = null;
    final ArrayList<ContentProviderRecord> mLaunchingProviders = new ArrayList<>();

    @GuardedBy("this")
    private final SparseArray<ArrayMap<GrantUri, UriPermission>> mGrantedUriPermissions = new SparseArray<>();
    Configuration mConfiguration = new Configuration();
    boolean mSuppressResizeConfigChanges = false;
    final StringBuilder mStringBuilder = new StringBuilder(256);
    String mTopAction = "android.intent.action.MAIN";
    volatile boolean mProcessesReady = false;
    volatile boolean mSystemReady = false;

    @GuardedBy("this")
    boolean mBooting = false;

    @GuardedBy("this")
    boolean mCallFinishBooting = false;

    @GuardedBy("this")
    boolean mBootAnimationComplete = false;

    @GuardedBy("this")
    boolean mLaunchWarningShown = false;

    @GuardedBy("this")
    boolean mCheckedForSetup = false;
    private boolean mSleeping = false;
    int mTopProcessState = 2;
    private int mWakefulness = 1;
    final ArrayList<ActivityManagerInternal.SleepToken> mSleepTokens = new ArrayList<>();
    int mLockScreenShown = 0;
    boolean mShuttingDown = false;
    int mAdjSeq = 0;
    int mLruSeq = 0;
    int mNumNonCachedProcs = 0;
    int mNumCachedHiddenProcs = 0;
    int mNumServiceProcs = 0;
    int mNewNumAServiceProcs = 0;
    int mNewNumServiceProcs = 0;
    boolean mAllowLowerMemLevel = false;
    int mLastMemoryLevel = 0;
    long mLastIdleTime = SystemClock.uptimeMillis();
    long mLowRamTimeSinceLastIdle = 0;
    long mLowRamStartTime = 0;
    private String mCurResumedPackage = null;
    private int mCurResumedUid = -1;
    final ProcessMap<ArrayList<ProcessRecord>> mForegroundPackages = new ProcessMap<>();
    boolean mTestPssMode = false;
    String mDebugApp = null;
    boolean mWaitForDebugger = false;
    boolean mDebugTransient = false;
    String mOrigDebugApp = null;
    boolean mOrigWaitForDebugger = false;
    boolean mAlwaysFinishActivities = false;
    boolean mLenientBackgroundCheck = false;
    IActivityController mController = null;
    boolean mControllerIsAMonkey = false;
    String mProfileApp = null;
    ProcessRecord mProfileProc = null;
    int mSamplingInterval = 0;
    boolean mAutoStopProfiler = false;
    int mProfileType = 0;
    final ProcessMap<Pair<Long, String>> mMemWatchProcesses = new ProcessMap<>();
    String mTrackAllocationApp = null;
    String mNativeDebuggingApp = null;
    final long[] mTmpLong = new long[2];
    final RemoteCallbackList<IProcessObserver> mProcessObservers = new RemoteCallbackList<>();
    ProcessChangeItem[] mActiveProcessChanges = new ProcessChangeItem[5];
    final ArrayList<ProcessChangeItem> mPendingProcessChanges = new ArrayList<>();
    final ArrayList<ProcessChangeItem> mAvailProcessChanges = new ArrayList<>();
    final RemoteCallbackList<IUidObserver> mUidObservers = new RemoteCallbackList<>();
    UidRecord.ChangeItem[] mActiveUidChanges = new UidRecord.ChangeItem[5];
    final ArrayList<UidRecord.ChangeItem> mPendingUidChanges = new ArrayList<>();
    final ArrayList<UidRecord.ChangeItem> mAvailUidChanges = new ArrayList<>();
    final ProcessCpuTracker mProcessCpuTracker = new ProcessCpuTracker(false);
    final AtomicLong mLastCpuTime = new AtomicLong(0);
    final AtomicBoolean mProcessCpuMutexFree = new AtomicBoolean(true);
    long mLastWriteTime = 0;
    final UpdateLock mUpdateLock = new UpdateLock("immersive");
    boolean mBooted = false;
    int mProcessLimit = 32;
    int mProcessLimitOverride = -1;
    SuppressManager mSuppressManager = null;
    long mLastMemUsageReportTime = 0;
    private int mViSessionId = 1000;
    final Handler mBgHandler = new Handler(BackgroundThread.getHandler().getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            ProcessRecord proc;
            int procState;
            long lastPssTime;
            int pid;
            switch (msg.what) {
                case 1:
                    long start = SystemClock.uptimeMillis();
                    MemInfoReader memInfo = null;
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (ActivityManagerService.this.mFullPssPending) {
                                ActivityManagerService.this.mFullPssPending = false;
                                memInfo = new MemInfoReader();
                            }
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (memInfo != null) {
                        ActivityManagerService.this.updateCpuStatsNow();
                        long nativeTotalPss = 0;
                        synchronized (ActivityManagerService.this.mProcessCpuTracker) {
                            int N = ActivityManagerService.this.mProcessCpuTracker.countStats();
                            for (int j = 0; j < N; j++) {
                                ProcessCpuTracker.Stats st = ActivityManagerService.this.mProcessCpuTracker.getStats(j);
                                if (st.vsize > 0 && st.uid < 10000) {
                                    synchronized (ActivityManagerService.this.mPidsSelfLocked) {
                                        if (ActivityManagerService.this.mPidsSelfLocked.indexOfKey(st.pid) < 0) {
                                            long nativePss = Debug.getPss(st.pid, ActivityManagerService.this.mTmpLong, null);
                                            nativeTotalPss += nativePss;
                                            if (SystemProperties.get("ro.mtk_aws_support").equals("1") && AWSManager.getInstance() != null) {
                                                AWSManager.getInstance().recordST(st.pid, st.uid, st.baseName);
                                                ProcessRecord prs = new ProcessRecord(null, new ApplicationInfo(), st.baseName, st.uid);
                                                prs.setPid(st.pid);
                                                AWSManager.getInstance().storeRecord(ActivityManagerService.this.convertStoreRecord(prs, ActivityManagerService.this.mTmpLong[1] + nativePss));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        memInfo.readMemInfo();
                        synchronized (ActivityManagerService.this) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityManagerDebugConfig.DEBUG_PSS) {
                                    Slog.d(ActivityManagerService.TAG_PSS, "Collected native and kernel memory in " + (SystemClock.uptimeMillis() - start) + "ms");
                                }
                                long cachedKb = memInfo.getCachedSizeKb();
                                long freeKb = memInfo.getFreeSizeKb();
                                long zramKb = memInfo.getZramTotalSizeKb();
                                long kernelKb = memInfo.getKernelUsedSizeKb();
                                EventLogTags.writeAmMeminfo(1024 * cachedKb, 1024 * freeKb, 1024 * zramKb, 1024 * kernelKb, 1024 * nativeTotalPss);
                                ActivityManagerService.this.mProcessStats.addSysMemUsageLocked(cachedKb, freeKb, zramKb, kernelKb, nativeTotalPss);
                            } catch (Throwable th2) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th2;
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                    int num = 0;
                    long[] tmp = new long[2];
                    while (true) {
                        synchronized (ActivityManagerService.this) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityManagerService.this.mPendingPssProcesses.size() <= 0) {
                                    if (ActivityManagerService.this.mTestPssMode || ActivityManagerDebugConfig.DEBUG_PSS) {
                                        Slog.d(ActivityManagerService.TAG_PSS, "Collected PSS of " + num + " processes in " + (SystemClock.uptimeMillis() - start) + "ms");
                                    }
                                    ActivityManagerService.this.mPendingPssProcesses.clear();
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                                proc = ActivityManagerService.this.mPendingPssProcesses.remove(0);
                                procState = proc.pssProcState;
                                lastPssTime = proc.lastPssTime;
                                if (proc.thread == null || procState != proc.setProcState || 1000 + lastPssTime >= SystemClock.uptimeMillis()) {
                                    proc = null;
                                    pid = 0;
                                } else {
                                    pid = proc.pid;
                                }
                            } catch (Throwable th3) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th3;
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        if (proc != null) {
                            long pss = Debug.getPss(pid, tmp, null);
                            synchronized (ActivityManagerService.this) {
                                try {
                                    ActivityManagerService.boostPriorityForLockedSection();
                                    if (pss != 0 && proc.thread != null && proc.setProcState == procState && proc.pid == pid && proc.lastPssTime == lastPssTime) {
                                        num++;
                                        ActivityManagerService.this.recordPssSampleLocked(proc, procState, pss, tmp[0], tmp[1], SystemClock.uptimeMillis());
                                    }
                                } catch (Throwable th4) {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    throw th4;
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    break;
                case 2:
                    if (AWSManager.getInstance() != null) {
                        AWSManager.getInstance().storeRecord(ActivityManagerService.this.convertStoreRecord(null, -1L));
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private final long[] mProcessStateStatsLongs = new long[1];
    private String[] mSupportedSystemLocales = null;
    private AMEventHook mAMEventHook = AMEventHook.createInstance();
    private ActivityStack mSystemReadyFocusedStack = null;
    ICustomizedOomExt mCustomizedOomExt = null;
    volatile int mFactoryTest = FactoryTest.getMode();
    final ActivityThread mSystemThread = ActivityThread.currentActivityThread();

    private static native int nativeMigrateFromBoost();

    private static native int nativeMigrateToBoost();

    static {
        IS_USER_BUILD = !"user".equals(Build.TYPE) ? IS_USER_DEBUG_BUILD : true;
        IS_ENG_BUILD = "eng".equals(Build.TYPE);
        POWER_CHECK_DELAY = (ActivityManagerDebugConfig.DEBUG_POWER_QUICK ? 2 : 15) * 60 * 1000;
        WAKE_LOCK_MIN_CHECK_DURATION = (ActivityManagerDebugConfig.DEBUG_POWER_QUICK ? 1 : 5) * 60 * 1000;
        CPU_MIN_CHECK_DURATION = (ActivityManagerDebugConfig.DEBUG_POWER_QUICK ? 1 : 5) * 60 * 1000;
        MY_PID = Process.myPid();
        EMPTY_STRING_ARRAY = new String[0];
        sIsBoosted = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
        sCallerIdentity = new ThreadLocal<>();
        mANRManager = null;
        mInterestingPids = new ArrayList<>();
        sKillThread = null;
        sKillHandler = null;
        PROCESS_STATE_STATS_FORMAT = new int[]{32, 544, 10272};
        DUMP_MEM_BUCKETS = new long[]{5120, 7168, 10240, 15360, 20480, 30720, 40960, 81920, 122880, 163840, 204800, 256000, 307200, 358400, 409600, 512000, 614400, 819200, 1048576, 2097152, 5242880, 10485760, 20971520};
        DUMP_MEM_OOM_ADJ = new int[]{JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, -900, -800, -700, 0, 100, FIRST_BROADCAST_QUEUE_MSG, FIRST_COMPAT_MODE_MSG, 400, 500, 600, 700, 800, 900};
        DUMP_MEM_OOM_LABEL = new String[]{"Native", "System", "Persistent", "Persistent Service", "Foreground", "Visible", "Perceptible", "Heavy Weight", "Backup", "A Services", "Home", "Previous", "B Services", "Cached"};
        DUMP_MEM_OOM_COMPACT_LABEL = new String[]{"native", "sys", "pers", "persvc", "fore", "vis", "percept", "heavy", "backup", "servicea", "home", "prev", "serviceb", "cached"};
        ProcessList.exportProcessADJ();
    }

    BroadcastQueue broadcastQueueForIntent(Intent intent) {
        boolean isFg = (intent.getFlags() & 268435456) != 0;
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND) {
            Slog.i(TAG_BROADCAST, "Broadcast intent " + intent + " on " + (isFg ? "foreground" : "background") + " queue");
        }
        return isFg ? this.mFgBroadcastQueue : this.mBgBroadcastQueue;
    }

    public boolean canShowErrorDialogs() {
        return (!this.mShowDialogs || this.mSleeping || this.mShuttingDown) ? false : true;
    }

    public static void boostPriorityForLockedSection() {
        if (sIsBoosted.get().intValue() == 0) {
            Process.setThreadPriority(Process.myTid(), -2);
        }
        int cur = sIsBoosted.get().intValue();
        sIsBoosted.set(Integer.valueOf(cur + 1));
    }

    public static void resetPriorityAfterLockedSection() {
        sIsBoosted.set(Integer.valueOf(sIsBoosted.get().intValue() - 1));
        if (sIsBoosted.get().intValue() != 0) {
            return;
        }
        Process.setThreadPriority(Process.myTid(), 0);
    }

    public class PendingAssistExtras extends Binder implements Runnable {
        public final ActivityRecord activity;
        public final Bundle extras;
        public final String hint;
        public final Intent intent;
        public final IResultReceiver receiver;
        public Bundle receiverExtras;
        public final int userHandle;
        public boolean haveResult = false;
        public Bundle result = null;
        public AssistStructure structure = null;
        public AssistContent content = null;

        public PendingAssistExtras(ActivityRecord _activity, Bundle _extras, Intent _intent, String _hint, IResultReceiver _receiver, Bundle _receiverExtras, int _userHandle) {
            this.activity = _activity;
            this.extras = _extras;
            this.intent = _intent;
            this.hint = _hint;
            this.receiver = _receiver;
            this.receiverExtras = _receiverExtras;
            this.userHandle = _userHandle;
        }

        @Override
        public void run() {
            Slog.w(ActivityManagerService.TAG, "getAssistContextExtras failed: timeout retrieving from " + this.activity);
            synchronized (this) {
                this.haveResult = true;
                notifyAll();
            }
            ActivityManagerService.this.pendingAssistExtrasTimedOut(this);
        }
    }

    abstract class ForegroundToken implements IBinder.DeathRecipient {
        int pid;
        IBinder token;

        ForegroundToken() {
        }
    }

    static final class Association {
        int mCount;
        long mLastStateUptime;
        int mNesting;
        final String mSourceProcess;
        final int mSourceUid;
        long mStartTime;
        final ComponentName mTargetComponent;
        final String mTargetProcess;
        final int mTargetUid;
        long mTime;
        int mLastState = 17;
        long[] mStateTimes = new long[18];

        Association(int sourceUid, String sourceProcess, int targetUid, ComponentName targetComponent, String targetProcess) {
            this.mSourceUid = sourceUid;
            this.mSourceProcess = sourceProcess;
            this.mTargetUid = targetUid;
            this.mTargetComponent = targetComponent;
            this.mTargetProcess = targetProcess;
        }
    }

    public static class GrantUri {
        public boolean prefix;
        public final int sourceUserId;
        public final Uri uri;

        public GrantUri(int sourceUserId, Uri uri, boolean prefix) {
            this.sourceUserId = sourceUserId;
            this.uri = uri;
            this.prefix = prefix;
        }

        public int hashCode() {
            int hashCode = this.sourceUserId + 31;
            return (((hashCode * 31) + this.uri.hashCode()) * 31) + (this.prefix ? 1231 : 1237);
        }

        public boolean equals(Object o) {
            if (!(o instanceof GrantUri)) {
                return false;
            }
            GrantUri other = (GrantUri) o;
            return this.uri.equals(other.uri) && this.sourceUserId == other.sourceUserId && this.prefix == other.prefix;
        }

        public String toString() {
            String result = Integer.toString(this.sourceUserId) + " @ " + this.uri.toString();
            return this.prefix ? result + " [prefix]" : result;
        }

        public String toSafeString() {
            String result = Integer.toString(this.sourceUserId) + " @ " + this.uri.toSafeString();
            return this.prefix ? result + " [prefix]" : result;
        }

        public static GrantUri resolve(int defaultSourceUserHandle, Uri uri) {
            return new GrantUri(ContentProvider.getUserIdFromUri(uri, defaultSourceUserHandle), ContentProvider.getUriWithoutUserId(uri), false);
        }
    }

    private final class FontScaleSettingObserver extends ContentObserver {
        private final Uri mFontScaleUri;

        public FontScaleSettingObserver() {
            super(ActivityManagerService.this.mHandler);
            this.mFontScaleUri = Settings.System.getUriFor("font_scale");
            ContentResolver resolver = ActivityManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mFontScaleUri, false, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!this.mFontScaleUri.equals(uri)) {
                return;
            }
            ActivityManagerService.this.updateFontScaleIfNeeded();
        }
    }

    private class Identity {
        public final int pid;
        public final IBinder token;
        public final int uid;

        Identity(IBinder _token, int _pid, int _uid) {
            this.token = _token;
            this.pid = _pid;
            this.uid = _uid;
        }
    }

    static final class ProcessChangeItem {
        static final int CHANGE_ACTIVITIES = 1;
        static final int CHANGE_PROCESS_STATE = 2;
        int changes;
        boolean foregroundActivities;
        int pid;
        int processState;
        int uid;

        ProcessChangeItem() {
        }
    }

    private final class AppDeathRecipient implements IBinder.DeathRecipient {
        final ProcessRecord mApp;
        final IApplicationThread mAppThread;
        final int mPid;

        AppDeathRecipient(ProcessRecord app, int pid, IApplicationThread thread) {
            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                Slog.v(ActivityManagerService.TAG, "New death recipient " + this + " for thread " + thread.asBinder());
            }
            this.mApp = app;
            this.mPid = pid;
            this.mAppThread = thread;
        }

        @Override
        public void binderDied() {
            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                Slog.v(ActivityManagerService.TAG, "Death received in " + this + " for thread " + this.mAppThread.asBinder());
            }
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.appDiedLocked(this.mApp, this.mPid, this.mAppThread, true);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    final class KillHandler extends Handler {
        static final int KILL_PROCESS_GROUP_MSG = 4000;

        public KillHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILL_PROCESS_GROUP_MSG:
                    Trace.traceBegin(64L, "killProcessGroup");
                    Process.killProcessGroup(msg.arg1, msg.arg2);
                    Trace.traceEnd(64L);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    final class UiHandler extends Handler {
        public UiHandler() {
            super(UiThread.get().getLooper(), null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ActivityManagerService.this.mAppErrors.handleShowAppErrorUi(msg);
                    ActivityManagerService.this.ensureBootCompleted();
                    return;
                case 2:
                    ActivityManagerService.this.mAppErrors.handleShowAnrUi(msg);
                    ActivityManagerService.this.ensureBootCompleted();
                    return;
                case 3:
                    new FactoryErrorDialog(ActivityManagerService.this.mContext, msg.getData().getCharSequence("msg")).show();
                    ActivityManagerService.this.ensureBootCompleted();
                    return;
                case 6:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ProcessRecord app = (ProcessRecord) msg.obj;
                            if (msg.arg1 != 0) {
                                if (!app.waitedForDebugger) {
                                    Dialog d = new AppWaitingForDebuggerDialog(ActivityManagerService.this, ActivityManagerService.this.mContext, app);
                                    app.waitDialog = d;
                                    app.waitedForDebugger = true;
                                    d.show();
                                }
                            } else if (app.waitDialog != null) {
                                app.waitDialog.dismiss();
                                app.waitDialog = null;
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                    return;
                case 14:
                    if (ActivityManagerService.this.mShowDialogs) {
                        AlertDialog d2 = new BaseErrorDialog(ActivityManagerService.this.mContext);
                        d2.getWindow().setType(2010);
                        d2.setCancelable(false);
                        d2.setTitle(ActivityManagerService.this.mContext.getText(R.string.aerr_mute));
                        d2.setMessage(ActivityManagerService.this.mContext.getText(R.string.lockscreen_pattern_wrong));
                        d2.setButton(-1, ActivityManagerService.this.mContext.getText(R.string.ok), obtainMessage(48, d2));
                        d2.show();
                        return;
                    }
                    return;
                case 15:
                    if (ActivityManagerService.this.mShowDialogs) {
                        AlertDialog d3 = new BaseErrorDialog(ActivityManagerService.this.mContext);
                        d3.getWindow().setType(2010);
                        d3.setCancelable(false);
                        d3.setTitle(ActivityManagerService.this.mContext.getText(R.string.aerr_mute));
                        d3.setMessage(ActivityManagerService.this.mContext.getText(R.string.lockscreen_permanent_disabled_sim_instructions));
                        d3.setButton(-1, ActivityManagerService.this.mContext.getText(R.string.ok), obtainMessage(48, d3));
                        d3.show();
                        return;
                    }
                    return;
                case 26:
                    HashMap<String, Object> data = (HashMap) msg.obj;
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ProcessRecord proc = (ProcessRecord) data.get("app");
                            if (proc == null) {
                                Slog.e(ActivityManagerService.TAG, "App not found when showing strict mode dialog.");
                                return;
                            }
                            if (proc.crashDialog != null) {
                                Slog.e(ActivityManagerService.TAG, "App already has strict mode dialog: " + proc);
                                return;
                            }
                            AppErrorResult res = (AppErrorResult) data.get("result");
                            if (!ActivityManagerService.this.mShowDialogs || ActivityManagerService.this.mSleeping || ActivityManagerService.this.mShuttingDown) {
                                res.set(0);
                            } else {
                                Dialog d4 = new StrictModeViolationDialog(ActivityManagerService.this.mContext, ActivityManagerService.this, res, proc);
                                d4.show();
                                proc.crashDialog = d4;
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            ActivityManagerService.this.ensureBootCompleted();
                            return;
                        } catch (Throwable th2) {
                            throw th2;
                        }
                    }
                case 30:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityRecord ar = (ActivityRecord) msg.obj;
                            if (ActivityManagerService.this.mCompatModeDialog != null) {
                                if (ActivityManagerService.this.mCompatModeDialog.mAppInfo.packageName.equals(ar.info.applicationInfo.packageName)) {
                                    return;
                                }
                                ActivityManagerService.this.mCompatModeDialog.dismiss();
                                ActivityManagerService.this.mCompatModeDialog = null;
                                break;
                            }
                            if (ar != null) {
                            }
                            return;
                        } catch (Throwable th3) {
                            throw th3;
                        }
                    }
                case 31:
                    ActivityManagerService.this.dispatchProcessesChanged();
                    return;
                case 32:
                    int pid = msg.arg1;
                    int uid = msg.arg2;
                    ActivityManagerService.this.dispatchProcessDied(pid, uid);
                    return;
                case 46:
                    ActivityManagerService.this.mUserController.showUserSwitchDialog((Pair) msg.obj);
                    return;
                case 48:
                    ((Dialog) msg.obj).dismiss();
                    return;
                case 54:
                    ActivityManagerService.this.dispatchUidsChanged();
                    return;
                case 70:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityRecord ar2 = (ActivityRecord) msg.obj;
                            if (ActivityManagerService.this.mUnsupportedDisplaySizeDialog != null) {
                                ActivityManagerService.this.mUnsupportedDisplaySizeDialog.dismiss();
                                ActivityManagerService.this.mUnsupportedDisplaySizeDialog = null;
                            }
                            if (ar2 != null && ActivityManagerService.this.mCompatModePackages.getPackageNotifyUnsupportedZoomLocked(ar2.packageName)) {
                                ActivityManagerService.this.mUnsupportedDisplaySizeDialog = new UnsupportedDisplaySizeDialog(ActivityManagerService.this, ActivityManagerService.this.mContext, ar2.info.applicationInfo);
                                ActivityManagerService.this.mUnsupportedDisplaySizeDialog.show();
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean vrMode;
            ComponentName requestedPackage;
            int userId;
            ComponentName callingPackage;
            String procName;
            int uid;
            long memLimit;
            String str;
            ActivityRecord root;
            ProcessRecord process;
            switch (msg.what) {
                case 4:
                    ContentResolver resolver = ActivityManagerService.this.mContext.getContentResolver();
                    Settings.System.putConfigurationForUser(resolver, (Configuration) msg.obj, msg.arg1);
                    return;
                case 5:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.performAppGcsIfAppropriateLocked();
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                    return;
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 26:
                case 30:
                case 31:
                case 32:
                case 46:
                case 48:
                case 54:
                default:
                    return;
                case 12:
                    if (ActivityManagerService.mANRManager.isAnrDeferrable()) {
                        Slog.d(ActivityManagerService.TAG, "Skip SERVICE_TIMEOUT ANR: " + msg.obj);
                        ActivityManagerService.this.mDidDexOpt = true;
                    }
                    if (!ActivityManagerService.this.mDidDexOpt) {
                        ActivityManagerService.this.mServices.serviceTimeout((ProcessRecord) msg.obj);
                        return;
                    }
                    ActivityManagerService.this.mDidDexOpt = false;
                    Message nmsg = ActivityManagerService.this.mHandler.obtainMessage(12);
                    nmsg.obj = msg.obj;
                    ActivityManagerService.this.mHandler.sendMessageDelayed(nmsg, 20000L);
                    return;
                case 13:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i = ActivityManagerService.this.mLruProcesses.size() - 1; i >= 0; i--) {
                                ProcessRecord r = ActivityManagerService.this.mLruProcesses.get(i);
                                if (r.thread != null) {
                                    try {
                                        r.thread.updateTimeZone();
                                    } catch (RemoteException e) {
                                        Slog.w(ActivityManagerService.TAG, "Failed to update time zone for: " + r.info.processName);
                                    }
                                }
                            }
                        } catch (Throwable th2) {
                            throw th2;
                        }
                    }
                    return;
                case 20:
                    if (ActivityManagerService.mANRManager.isAnrDeferrable()) {
                        Slog.d(ActivityManagerService.TAG, "Skip PROC_START_TIMEOUT: " + msg.obj);
                        ActivityManagerService.this.mDidDexOpt = true;
                    }
                    if (ActivityManagerService.this.mDidDexOpt) {
                        ActivityManagerService.this.mDidDexOpt = false;
                        Message nmsg2 = ActivityManagerService.this.mHandler.obtainMessage(20);
                        nmsg2.obj = msg.obj;
                        ActivityManagerService.this.mHandler.sendMessageDelayed(nmsg2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                        return;
                    }
                    ProcessRecord app = (ProcessRecord) msg.obj;
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.processStartTimedOutLocked(app);
                        } catch (Throwable th3) {
                            throw th3;
                        }
                    }
                    return;
                case 21:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.mActivityStarter.doPendingActivityLaunchesLocked(true);
                        } catch (Throwable th4) {
                            throw th4;
                        }
                    }
                    return;
                case 22:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            int appId = msg.arg1;
                            int userId2 = msg.arg2;
                            Bundle bundle = (Bundle) msg.obj;
                            String pkg = bundle.getString("pkg");
                            String reason = bundle.getString(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
                            ActivityManagerService.this.forceStopPackageLocked(pkg, appId, false, false, true, false, false, userId2, reason);
                        } catch (Throwable th5) {
                            throw th5;
                        }
                    }
                    return;
                case 23:
                    ((PendingIntentRecord) msg.obj).completeFinalize();
                    return;
                case 24:
                    INotificationManager inm = NotificationManager.getService();
                    if (inm == null || (process = (root = (ActivityRecord) msg.obj).app) == null) {
                        return;
                    }
                    try {
                        Context context = ActivityManagerService.this.mContext.createPackageContext(process.info.packageName, 0);
                        String text = ActivityManagerService.this.mContext.getString(R.string.eventTypeAnniversary, context.getApplicationInfo().loadLabel(context.getPackageManager()));
                        Notification notification = new Notification.Builder(context).setSmallIcon(R.drawable.list_selector_background_longpress_light).setWhen(0L).setOngoing(true).setTicker(text).setColor(ActivityManagerService.this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(text).setContentText(ActivityManagerService.this.mContext.getText(R.string.eventTypeBirthday)).setContentIntent(PendingIntent.getActivityAsUser(ActivityManagerService.this.mContext, 0, root.intent, 268435456, null, new UserHandle(root.userId))).build();
                        try {
                            int[] outId = new int[1];
                            inm.enqueueNotificationWithTag("android", "android", (String) null, R.string.eventTypeAnniversary, notification, outId, root.userId);
                            break;
                        } catch (RemoteException e2) {
                        } catch (RuntimeException e3) {
                            Slog.w(ActivityManagerService.TAG, "Error showing notification for heavy-weight app", e3);
                        }
                        return;
                    } catch (PackageManager.NameNotFoundException e4) {
                        Slog.w(ActivityManagerService.TAG, "Unable to create context for heavy notification", e4);
                        return;
                    }
                case 25:
                    INotificationManager inm2 = NotificationManager.getService();
                    if (inm2 == null) {
                        return;
                    }
                    try {
                        inm2.cancelNotificationWithTag("android", (String) null, R.string.eventTypeAnniversary, msg.arg1);
                        return;
                    } catch (RemoteException e5) {
                        return;
                    } catch (RuntimeException e6) {
                        Slog.w(ActivityManagerService.TAG, "Error canceling notification for service", e6);
                        return;
                    }
                case 27:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.checkExcessivePowerUsageLocked(true);
                            removeMessages(27);
                            sendMessageDelayed(obtainMessage(27), ActivityManagerService.POWER_CHECK_DELAY);
                        } catch (Throwable th6) {
                            throw th6;
                        }
                    }
                    return;
                case 28:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i2 = ActivityManagerService.this.mLruProcesses.size() - 1; i2 >= 0; i2--) {
                                ProcessRecord r2 = ActivityManagerService.this.mLruProcesses.get(i2);
                                if (r2.thread != null) {
                                    try {
                                        r2.thread.clearDnsCache();
                                    } catch (RemoteException e7) {
                                        Slog.w(ActivityManagerService.TAG, "Failed to clear dns cache for: " + r2.info.processName);
                                    }
                                }
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                case 29:
                    ProxyInfo proxy = (ProxyInfo) msg.obj;
                    String host = "";
                    String port = "";
                    String exclList = "";
                    Uri pacFileUrl = Uri.EMPTY;
                    if (proxy != null) {
                        host = proxy.getHost();
                        port = Integer.toString(proxy.getPort());
                        exclList = proxy.getExclusionListAsString();
                        pacFileUrl = proxy.getPacFileUrl();
                    }
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i3 = ActivityManagerService.this.mLruProcesses.size() - 1; i3 >= 0; i3--) {
                                ProcessRecord r3 = ActivityManagerService.this.mLruProcesses.get(i3);
                                if (r3.thread != null) {
                                    try {
                                        r3.thread.setHttpProxy(host, port, exclList, pacFileUrl);
                                    } catch (RemoteException e8) {
                                        Slog.w(ActivityManagerService.TAG, "Failed to update http proxy for: " + r3.info.processName);
                                    }
                                }
                            }
                        } catch (Throwable th7) {
                            throw th7;
                        }
                    }
                    return;
                case 33:
                    final ArrayList<ProcessMemInfo> memInfos = (ArrayList) msg.obj;
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            ActivityManagerService.this.reportMemUsage(memInfos);
                        }
                    };
                    thread.start();
                    return;
                case 34:
                    ActivityManagerService.this.mUserController.dispatchUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                    return;
                case 35:
                    ActivityManagerService.this.mUserController.continueUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                    return;
                case 36:
                    ActivityManagerService.this.mUserController.timeoutUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                    return;
                case 37:
                    boolean nextState = msg.arg1 != 0;
                    if (ActivityManagerService.this.mUpdateLock.isHeld() != nextState) {
                        if (ActivityManagerDebugConfig.DEBUG_IMMERSIVE) {
                            Slog.d(ActivityManagerService.TAG_IMMERSIVE, "Applying new update lock state '" + nextState + "' for " + ((ActivityRecord) msg.obj));
                        }
                        if (nextState) {
                            ActivityManagerService.this.mUpdateLock.acquire();
                            return;
                        } else {
                            ActivityManagerService.this.mUpdateLock.release();
                            return;
                        }
                    }
                    return;
                case 38:
                    ActivityManagerService.this.writeGrantedUriPermissions();
                    return;
                case 39:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
                        } catch (Throwable th8) {
                            throw th8;
                        }
                    }
                    return;
                case 40:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.mUserController.startProfilesLocked();
                        } catch (Throwable th9) {
                            throw th9;
                        }
                    }
                    return;
                case 41:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i4 = ActivityManagerService.this.mLruProcesses.size() - 1; i4 >= 0; i4--) {
                                ProcessRecord r4 = ActivityManagerService.this.mLruProcesses.get(i4);
                                if (r4.thread != null) {
                                    try {
                                        r4.thread.updateTimePrefs(msg.arg1 != 0);
                                    } catch (RemoteException e9) {
                                        Slog.w(ActivityManagerService.TAG, "Failed to update preferences for: " + r4.info.processName);
                                    }
                                }
                            }
                        } catch (Throwable th10) {
                            throw th10;
                        }
                    }
                    return;
                case 42:
                    ActivityManagerService.this.mBatteryStatsService.noteEvent(32775, Integer.toString(msg.arg1), msg.arg1);
                    ActivityManagerService.this.mSystemServiceManager.startUser(msg.arg1);
                    return;
                case 43:
                    ActivityManagerService.this.mBatteryStatsService.noteEvent(16392, Integer.toString(msg.arg2), msg.arg2);
                    ActivityManagerService.this.mBatteryStatsService.noteEvent(32776, Integer.toString(msg.arg1), msg.arg1);
                    ActivityManagerService.this.mSystemServiceManager.switchUser(msg.arg1);
                    return;
                case 44:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityRecord r5 = ActivityRecord.forTokenLocked((IBinder) msg.obj);
                            if (r5 != null && r5.app != null && r5.app.thread != null) {
                                try {
                                    r5.app.thread.scheduleEnterAnimationComplete(r5.appToken);
                                    break;
                                } catch (RemoteException e10) {
                                }
                            }
                        } catch (Throwable th11) {
                            throw th11;
                        }
                    }
                    return;
                case ActivityManagerService.FINISH_BOOTING_MSG:
                    if (msg.arg1 != 0) {
                        Trace.traceBegin(64L, "FinishBooting");
                        ActivityManagerService.this.finishBooting();
                        Trace.traceEnd(64L);
                    }
                    if (msg.arg2 != 0) {
                        ActivityManagerService.this.enableScreenAfterBoot();
                        return;
                    }
                    return;
                case 47:
                    try {
                        Locale l = (Locale) msg.obj;
                        IBinder service = ServiceManager.getService("mount");
                        IMountService mountService = IMountService.Stub.asInterface(service);
                        Log.d(ActivityManagerService.TAG, "Storing locale " + l.toLanguageTag() + " for decryption UI");
                        mountService.setField("SystemLocale", l.toLanguageTag());
                        return;
                    } catch (RemoteException e11) {
                        Log.e(ActivityManagerService.TAG, "Error storing locale for decryption UI", e11);
                        return;
                    }
                case 49:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i5 = ActivityManagerService.this.mTaskStackListeners.beginBroadcast() - 1; i5 >= 0; i5--) {
                                try {
                                    ActivityManagerService.this.mTaskStackListeners.getBroadcastItem(i5).onTaskStackChanged();
                                } catch (RemoteException e12) {
                                }
                            }
                            ActivityManagerService.this.mTaskStackListeners.finishBroadcast();
                        } catch (Throwable th12) {
                            throw th12;
                        }
                    }
                    return;
                case 50:
                    int uid2 = msg.arg1;
                    byte[] firstPacket = (byte[]) msg.obj;
                    synchronized (ActivityManagerService.this.mPidsSelfLocked) {
                        for (int i6 = 0; i6 < ActivityManagerService.this.mPidsSelfLocked.size(); i6++) {
                            ProcessRecord p = ActivityManagerService.this.mPidsSelfLocked.valueAt(i6);
                            if (p.uid == uid2) {
                                try {
                                    p.thread.notifyCleartextNetwork(firstPacket);
                                } catch (RemoteException e13) {
                                }
                            }
                            break;
                        }
                    }
                    return;
                case 51:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            procName = ActivityManagerService.this.mMemWatchDumpProcName;
                            uid = ActivityManagerService.this.mMemWatchDumpUid;
                            Pair<Long, String> val = (Pair) ActivityManagerService.this.mMemWatchProcesses.get(procName, uid);
                            if (val == null) {
                                val = (Pair) ActivityManagerService.this.mMemWatchProcesses.get(procName, 0);
                            }
                            if (val != null) {
                                memLimit = ((Long) val.first).longValue();
                                str = (String) val.second;
                            } else {
                                memLimit = 0;
                                str = null;
                            }
                        } catch (Throwable th13) {
                            throw th13;
                        }
                        break;
                    }
                    if (procName == null) {
                        return;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_PSS) {
                        Slog.d(ActivityManagerService.TAG_PSS, "Showing dump heap notification from " + procName + "/" + uid);
                    }
                    INotificationManager inm3 = NotificationManager.getService();
                    if (inm3 == null) {
                        return;
                    }
                    String text2 = ActivityManagerService.this.mContext.getString(R.string.ext_media_badremoval_notification_message, procName);
                    Intent deleteIntent = new Intent();
                    deleteIntent.setAction("com.android.server.am.DELETE_DUMPHEAP");
                    Intent intent = new Intent();
                    intent.setClassName("android", DumpHeapActivity.class.getName());
                    intent.putExtra("process", procName);
                    intent.putExtra("size", memLimit);
                    if (str != null) {
                        intent.putExtra("direct_launch", str);
                    }
                    int userId3 = UserHandle.getUserId(uid);
                    Notification notification2 = new Notification.Builder(ActivityManagerService.this.mContext).setSmallIcon(R.drawable.list_selector_background_longpress_light).setWhen(0L).setOngoing(true).setAutoCancel(true).setTicker(text2).setColor(ActivityManagerService.this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(text2).setContentText(ActivityManagerService.this.mContext.getText(R.string.ext_media_badremoval_notification_title)).setContentIntent(PendingIntent.getActivityAsUser(ActivityManagerService.this.mContext, 0, intent, 268435456, null, new UserHandle(userId3))).setDeleteIntent(PendingIntent.getBroadcastAsUser(ActivityManagerService.this.mContext, 0, deleteIntent, 0, UserHandle.SYSTEM)).build();
                    try {
                        int[] outId2 = new int[1];
                        inm3.enqueueNotificationWithTag("android", "android", (String) null, R.string.ext_media_badremoval_notification_message, notification2, outId2, userId3);
                        return;
                    } catch (RemoteException e14) {
                        return;
                    } catch (RuntimeException e15) {
                        Slog.w(ActivityManagerService.TAG, "Error showing notification for dump heap", e15);
                        return;
                    }
                case 52:
                    ActivityManagerService.this.revokeUriPermission(ActivityThread.currentActivityThread().getApplicationThread(), DumpHeapActivity.JAVA_URI, 3, UserHandle.myUserId());
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.mMemWatchDumpFile = null;
                            ActivityManagerService.this.mMemWatchDumpProcName = null;
                            ActivityManagerService.this.mMemWatchDumpPid = -1;
                            ActivityManagerService.this.mMemWatchDumpUid = -1;
                        } catch (Throwable th14) {
                            throw th14;
                        }
                    }
                    return;
                case 53:
                    ActivityManagerService.this.mUserController.dispatchForegroundProfileChanged(msg.arg1);
                    return;
                case 55:
                    AppTimeTracker tracker = (AppTimeTracker) msg.obj;
                    tracker.deliverResult(ActivityManagerService.this.mContext);
                    return;
                case 56:
                    ActivityManagerService.this.mUserController.dispatchUserSwitchComplete(msg.arg1);
                    return;
                case ActivityManagerService.SHUTDOWN_UI_AUTOMATION_CONNECTION_MSG:
                    IUiAutomationConnection connection = (IUiAutomationConnection) msg.obj;
                    try {
                        connection.shutdown();
                        break;
                    } catch (RemoteException e16) {
                        Slog.w(ActivityManagerService.TAG, "Error shutting down UiAutomationConnection");
                    }
                    ActivityManagerService.this.mUserIsMonkey = false;
                    return;
                case ActivityManagerService.APP_BOOST_DEACTIVATE_MSG:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (ActivityManagerService.this.mIsBoosted) {
                                if (ActivityManagerService.this.mBoostStartTime < SystemClock.uptimeMillis() - 2500) {
                                    ActivityManagerService.nativeMigrateFromBoost();
                                    ActivityManagerService.this.mIsBoosted = false;
                                    ActivityManagerService.this.mBoostStartTime = 0L;
                                } else {
                                    Message newmsg = ActivityManagerService.this.mHandler.obtainMessage(ActivityManagerService.APP_BOOST_DEACTIVATE_MSG);
                                    ActivityManagerService.this.mHandler.sendMessageDelayed(newmsg, 2500L);
                                }
                            }
                        } catch (Throwable th15) {
                            throw th15;
                        }
                    }
                    return;
                case ActivityManagerService.CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG:
                    ProcessRecord app2 = (ProcessRecord) msg.obj;
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.processContentProviderPublishTimedOutLocked(app2);
                        } catch (Throwable th16) {
                            throw th16;
                        }
                    }
                    return;
                case 60:
                    ActivityManagerService.this.idleUids();
                    return;
                case ActivityManagerService.SYSTEM_USER_UNLOCK_MSG:
                    int userId4 = msg.arg1;
                    ActivityManagerService.this.mSystemServiceManager.unlockUser(userId4);
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.mRecentTasks.loadUserRecentsLocked(userId4);
                        } catch (Throwable th17) {
                            throw th17;
                        }
                    }
                    if (userId4 == 0) {
                        ActivityManagerService.this.startPersistentApps(PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED);
                    }
                    ActivityManagerService.this.installEncryptionUnawareProviders(userId4);
                    if ("1".equals(SystemProperties.get("persist.runningbooster.support"))) {
                        AMEventHookData.SystemUserUnlock eventData = AMEventHookData.SystemUserUnlock.createInstance();
                        eventData.set(new Object[]{Integer.valueOf(userId4)});
                        ActivityManagerService.this.mAMEventHook.hook(AMEventHook.Event.AM_SystemUserUnlock, eventData);
                    }
                    ActivityManagerService.this.mUserController.finishUserUnlocked((UserState) msg.obj);
                    return;
                case ActivityManagerService.LOG_STACK_STATE:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.mStackSupervisor.logStackState();
                        } catch (Throwable th18) {
                            throw th18;
                        }
                    }
                    return;
                case ActivityManagerService.VR_MODE_CHANGE_MSG:
                    VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
                    ActivityRecord r6 = (ActivityRecord) msg.obj;
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            vrMode = r6.requestedVrComponent != null;
                            requestedPackage = r6.requestedVrComponent;
                            userId = r6.userId;
                            callingPackage = r6.info.getComponentName();
                            if (ActivityManagerService.this.mInVrMode != vrMode) {
                                ActivityManagerService.this.mInVrMode = vrMode;
                                ActivityManagerService.this.mShowDialogs = ActivityManagerService.shouldShowDialogs(ActivityManagerService.this.mConfiguration, ActivityManagerService.this.mInVrMode);
                            }
                        } catch (Throwable th19) {
                            throw th19;
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    vrService.setVrMode(vrMode, requestedPackage, userId, callingPackage);
                    return;
                case 64:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i7 = ActivityManagerService.this.mTaskStackListeners.beginBroadcast() - 1; i7 >= 0; i7--) {
                                try {
                                    ActivityManagerService.this.mTaskStackListeners.getBroadcastItem(i7).onActivityPinned();
                                } catch (RemoteException e17) {
                                }
                            }
                            ActivityManagerService.this.mTaskStackListeners.finishBroadcast();
                        } catch (Throwable th20) {
                            throw th20;
                        }
                    }
                    return;
                case 65:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i8 = ActivityManagerService.this.mTaskStackListeners.beginBroadcast() - 1; i8 >= 0; i8--) {
                                try {
                                    ActivityManagerService.this.mTaskStackListeners.getBroadcastItem(i8).onPinnedActivityRestartAttempt();
                                } catch (RemoteException e18) {
                                }
                            }
                            ActivityManagerService.this.mTaskStackListeners.finishBroadcast();
                        } catch (Throwable th21) {
                            throw th21;
                        }
                    }
                    return;
                case 66:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i9 = ActivityManagerService.this.mTaskStackListeners.beginBroadcast() - 1; i9 >= 0; i9--) {
                                try {
                                    ActivityManagerService.this.mTaskStackListeners.getBroadcastItem(i9).onPinnedStackAnimationEnded();
                                } catch (RemoteException e19) {
                                }
                            }
                            ActivityManagerService.this.mTaskStackListeners.finishBroadcast();
                        } catch (Throwable th22) {
                            throw th22;
                        }
                    }
                    return;
                case 67:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i10 = ActivityManagerService.this.mTaskStackListeners.beginBroadcast() - 1; i10 >= 0; i10--) {
                                try {
                                    ActivityManagerService.this.mTaskStackListeners.getBroadcastItem(i10).onActivityForcedResizable((String) msg.obj, msg.arg1);
                                } catch (RemoteException e20) {
                                }
                            }
                            ActivityManagerService.this.mTaskStackListeners.finishBroadcast();
                        } catch (Throwable th23) {
                            throw th23;
                        }
                    }
                    return;
                case 68:
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int i11 = ActivityManagerService.this.mTaskStackListeners.beginBroadcast() - 1; i11 >= 0; i11--) {
                                try {
                                    ActivityManagerService.this.mTaskStackListeners.getBroadcastItem(i11).onActivityDismissingDockedStack();
                                } catch (RemoteException e21) {
                                }
                            }
                            ActivityManagerService.this.mTaskStackListeners.finishBroadcast();
                        } catch (Throwable th24) {
                            throw th24;
                        }
                    }
                    return;
                case 69:
                    ActivityRecord r7 = (ActivityRecord) msg.obj;
                    boolean needsVrMode = (r7 == null || r7.requestedVrComponent == null) ? false : true;
                    if (needsVrMode) {
                        ActivityManagerService.this.applyVrMode(msg.arg1 == 1, r7.requestedVrComponent, r7.userId, r7.info.getComponentName(), false);
                        return;
                    }
                    return;
            }
        }
    }

    public void setSystemProcess() {
        try {
            ServiceManager.addService("activity", this, true);
            ServiceManager.addService("procstats", this.mProcessStats);
            ServiceManager.addService("meminfo", new MemBinder(this));
            ServiceManager.addService("gfxinfo", new GraphicsBinder(this));
            ServiceManager.addService("dbinfo", new DbBinder(this));
            ServiceManager.addService("cpuinfo", new CpuBinder(this));
            ServiceManager.addService("permission", new PermissionController(this));
            ServiceManager.addService("processinfo", new ProcessInfoService(this));
            ServiceManager.addService("anrmanager", mANRManager, true);
            ApplicationInfo info = this.mContext.getPackageManager().getApplicationInfo("android", 1049600);
            this.mSystemThread.installSystemApplicationInfo(info, getClass().getClassLoader());
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ProcessRecord app = newProcessRecordLocked(info, info.processName, false, 0);
                    app.persistent = true;
                    app.pid = MY_PID;
                    app.maxAdj = -900;
                    app.makeActive(this.mSystemThread.getApplicationThread(), this.mProcessStats);
                    synchronized (this.mPidsSelfLocked) {
                        this.mPidsSelfLocked.put(app.pid, app);
                    }
                    updateLruProcessLocked(app, false, null);
                    updateOomAdjLocked();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Unable to find android system package", e);
        }
    }

    public void setWindowManager(WindowManagerService wm) {
        this.mWindowManager = wm;
        this.mStackSupervisor.setWindowManager(wm);
        this.mActivityStarter.setWindowManager(wm);
    }

    public void setUsageStatsManager(UsageStatsManagerInternal usageStatsManager) {
        this.mUsageStatsService = usageStatsManager;
    }

    public void startObservingNativeCrashes() {
        NativeCrashListener ncl = new NativeCrashListener(this);
        ncl.start();
    }

    public IAppOpsService getAppOpsService() {
        return this.mAppOpsService;
    }

    static class MemBinder extends Binder {
        ActivityManagerService mActivityManagerService;

        MemBinder(ActivityManagerService activityManagerService) {
            this.mActivityManagerService = activityManagerService;
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) throws Throwable {
            if (this.mActivityManagerService.checkCallingPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump meminfo from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            } else {
                this.mActivityManagerService.dumpApplicationMemoryUsage(fd, pw, "  ", args, false, null);
            }
        }
    }

    static class GraphicsBinder extends Binder {
        ActivityManagerService mActivityManagerService;

        GraphicsBinder(ActivityManagerService activityManagerService) {
            this.mActivityManagerService = activityManagerService;
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (this.mActivityManagerService.checkCallingPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump gfxinfo from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            } else {
                this.mActivityManagerService.dumpGraphicsHardwareUsage(fd, pw, args);
            }
        }
    }

    static class DbBinder extends Binder {
        ActivityManagerService mActivityManagerService;

        DbBinder(ActivityManagerService activityManagerService) {
            this.mActivityManagerService = activityManagerService;
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (this.mActivityManagerService.checkCallingPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump dbinfo from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            } else {
                this.mActivityManagerService.dumpDbInfo(fd, pw, args);
            }
        }
    }

    static class CpuBinder extends Binder {
        ActivityManagerService mActivityManagerService;

        CpuBinder(ActivityManagerService activityManagerService) {
            this.mActivityManagerService = activityManagerService;
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (this.mActivityManagerService.checkCallingPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump cpuinfo from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
                return;
            }
            synchronized (this.mActivityManagerService.mProcessCpuTracker) {
                pw.print(this.mActivityManagerService.mProcessCpuTracker.printCurrentLoad());
                pw.print(this.mActivityManagerService.mProcessCpuTracker.printCurrentState(SystemClock.uptimeMillis()));
            }
        }
    }

    public static final class Lifecycle extends SystemService {
        private final ActivityManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            BootEvent.setEnabled(true);
            this.mService = new ActivityManagerService(context);
        }

        @Override
        public void onStart() {
            this.mService.start();
        }

        public ActivityManagerService getService() {
            return this.mService;
        }
    }

    public ActivityManagerService(Context systemContext) {
        this.mConfigurationSeq = 0;
        this.mOnBattery = false;
        this.mContext = systemContext;
        Slog.i(TAG, "Memory class: " + ActivityManager.staticGetMemoryClass());
        this.mHandlerThread = new ServiceThread(TAG, -2, false);
        this.mHandlerThread.start();
        this.mHandler = new MainHandler(this.mHandlerThread.getLooper());
        this.mUiHandler = new UiHandler();
        if (sKillHandler == null) {
            sKillThread = new ServiceThread(TAG + ":kill", 10, true);
            sKillThread.start();
            sKillHandler = new KillHandler(sKillThread.getLooper());
        }
        this.mFgBroadcastQueue = new BroadcastQueue(this, this.mHandler, "foreground", JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, false);
        this.mBgBroadcastQueue = new BroadcastQueue(this, this.mHandler, "background", 60000L, true);
        this.mBroadcastQueues[0] = this.mFgBroadcastQueue;
        this.mBroadcastQueues[1] = this.mBgBroadcastQueue;
        this.mServices = new ActiveServices(this);
        this.mProviderMap = new ProviderMap(this);
        this.mAppErrors = new AppErrors(this.mContext, this);
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        systemDir.mkdirs();
        this.mBatteryStatsService = new BatteryStatsService(systemDir, this.mHandler);
        this.mBatteryStatsService.getActiveStatistics().readLocked();
        this.mBatteryStatsService.scheduleWriteToDisk();
        this.mOnBattery = ActivityManagerDebugConfig.DEBUG_POWER ? true : this.mBatteryStatsService.getActiveStatistics().getIsOnBattery();
        this.mBatteryStatsService.getActiveStatistics().setCallback(this);
        this.mProcessStats = new ProcessStatsService(this, new File(systemDir, "procstats"));
        this.mAppOpsService = new AppOpsService(new File(systemDir, "appops.xml"), this.mHandler);
        this.mAppOpsService.startWatchingMode(VR_MODE_CHANGE_MSG, null, new IAppOpsCallback.Stub() {
            public void opChanged(int op, int uid, String packageName) {
                if (op != ActivityManagerService.VR_MODE_CHANGE_MSG || packageName == null || ActivityManagerService.this.mAppOpsService.checkOperation(op, uid, packageName) == 0) {
                    return;
                }
                ActivityManagerService.this.runInBackgroundDisabled(uid);
            }
        });
        this.mGrantFile = new AtomicFile(new File(systemDir, "urigrants.xml"));
        this.mUserController = new UserController(this);
        this.GL_ES_VERSION = SystemProperties.getInt("ro.opengles.version", 0);
        this.mTrackingAssociations = "1".equals(SystemProperties.get("debug.track-associations"));
        this.mConfiguration.setToDefaults();
        this.mConfiguration.setLocales(LocaleList.getDefault());
        this.mConfiguration.seq = 1;
        this.mConfigurationSeq = 1;
        this.mProcessCpuTracker.init();
        this.mCompatModePackages = new CompatModePackages(this, systemDir, this.mHandler);
        this.mIntentFirewall = new IntentFirewall(new IntentFirewallInterface(), this.mHandler);
        this.mStackSupervisor = new ActivityStackSupervisor(this);
        this.mActivityStarter = new ActivityStarter(this, this.mStackSupervisor);
        this.mRecentTasks = new RecentTasks(this, this.mStackSupervisor);
        this.mProcessCpuThread = new Thread("CpuTracker") {
            @Override
            public void run() {
                while (true) {
                    try {
                        try {
                            synchronized (this) {
                                long now = SystemClock.uptimeMillis();
                                long nextCpuDelay = (ActivityManagerService.this.mLastCpuTime.get() + ActivityManagerService.MONITOR_CPU_MAX_TIME) - now;
                                long nextWriteDelay = (ActivityManagerService.this.mLastWriteTime + ActivityManagerService.BATTERY_STATS_TIME) - now;
                                if (nextWriteDelay < nextCpuDelay) {
                                    nextCpuDelay = nextWriteDelay;
                                }
                                if (nextCpuDelay > 0) {
                                    ActivityManagerService.this.mProcessCpuMutexFree.set(true);
                                    wait(nextCpuDelay);
                                }
                            }
                        } catch (Exception e) {
                            Slog.e(ActivityManagerService.TAG, "Unexpected exception collecting process stats", e);
                        }
                    } catch (InterruptedException e2) {
                    }
                    ActivityManagerService.this.updateCpuStatsNow();
                }
            }
        };
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
        AMEventHookData.EndOfAMSCtor eventData = AMEventHookData.EndOfAMSCtor.createInstance();
        this.mAMEventHook.hook(AMEventHook.Event.AM_EndOfAMSCtor, eventData);
    }

    public void setSystemServiceManager(SystemServiceManager mgr) {
        this.mSystemServiceManager = mgr;
    }

    public void setInstaller(Installer installer) {
        this.mInstaller = installer;
    }

    private void start() {
        Process.removeAllProcessGroups();
        this.mProcessCpuThread.start();
        this.mBatteryStatsService.publish(this.mContext);
        this.mAppOpsService.publish(this.mContext);
        Slog.d("AppOps", "AppOpsService published");
        LocalServices.addService(ActivityManagerInternal.class, new LocalService(this, null));
        configLogTag();
        mANRManager = new ANRManager(new AnrActivityManagerService(), MY_PID, this.mContext);
        mANRManager.startANRManager();
        this.mAnrDumpMgr = mANRManager.mAnrDumpMgr;
        this.mAnrHandler = mANRManager.mAnrHandler;
        this.mSuppressManager = new SuppressManager();
    }

    void onUserStoppedLocked(int userId) {
        this.mRecentTasks.unloadUserDataFromMemoryLocked(userId);
    }

    public void initPowerManagement() {
        this.mStackSupervisor.initPowerManagement();
        this.mBatteryStatsService.initPowerManagement();
        this.mLocalPowerManager = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mVoiceWakeLock = pm.newWakeLock(1, "*voice*");
        this.mVoiceWakeLock.setReferenceCounted(false);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 1599295570) {
            ArrayList<IBinder> procs = new ArrayList<>();
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    int NP = this.mProcessNames.getMap().size();
                    for (int ip = 0; ip < NP; ip++) {
                        SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                        int NA = apps.size();
                        for (int ia = 0; ia < NA; ia++) {
                            ProcessRecord app = apps.valueAt(ia);
                            if (app.thread != null) {
                                procs.add(app.thread.asBinder());
                            }
                        }
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                Parcel data2 = Parcel.obtain();
                try {
                    procs.get(i).transact(1599295570, data2, null, 0);
                } catch (RemoteException e) {
                }
                data2.recycle();
            }
        }
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e2) {
            if (!(e2 instanceof SecurityException)) {
                Slog.e(TAG, "Activity Manager Crash", e2);
            }
            throw e2;
        }
    }

    void updateCpuStats() {
        long now = SystemClock.uptimeMillis();
        if (this.mLastCpuTime.get() >= now - DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC || !this.mProcessCpuMutexFree.compareAndSet(true, false)) {
            return;
        }
        synchronized (this.mProcessCpuThread) {
            this.mProcessCpuThread.notify();
        }
    }

    void updateCpuStatsNow() {
        synchronized (this.mProcessCpuTracker) {
            this.mProcessCpuMutexFree.set(false);
            long now = SystemClock.uptimeMillis();
            boolean haveNewCpuStats = false;
            if (this.mLastCpuTime.get() < now - DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC) {
                this.mLastCpuTime.set(now);
                this.mProcessCpuTracker.update();
                if (this.mProcessCpuTracker.hasGoodLastStats()) {
                    haveNewCpuStats = true;
                    if ("true".equals(SystemProperties.get("events.cpu"))) {
                        int user = this.mProcessCpuTracker.getLastUserTime();
                        int system = this.mProcessCpuTracker.getLastSystemTime();
                        int iowait = this.mProcessCpuTracker.getLastIoWaitTime();
                        int irq = this.mProcessCpuTracker.getLastIrqTime();
                        int softIrq = this.mProcessCpuTracker.getLastSoftIrqTime();
                        int idle = this.mProcessCpuTracker.getLastIdleTime();
                        int total = user + system + iowait + irq + softIrq + idle;
                        if (total == 0) {
                            total = 1;
                        }
                        EventLog.writeEvent(EventLogTags.CPU, Integer.valueOf((((((user + system) + iowait) + irq) + softIrq) * 100) / total), Integer.valueOf((user * 100) / total), Integer.valueOf((system * 100) / total), Integer.valueOf((iowait * 100) / total), Integer.valueOf((irq * 100) / total), Integer.valueOf((softIrq * 100) / total));
                    }
                }
            }
            BatteryStatsImpl bstats = this.mBatteryStatsService.getActiveStatistics();
            synchronized (bstats) {
                synchronized (this.mPidsSelfLocked) {
                    if (haveNewCpuStats) {
                        if (bstats.startAddingCpuLocked()) {
                            int totalUTime = 0;
                            int totalSTime = 0;
                            int N = this.mProcessCpuTracker.countStats();
                            for (int i = 0; i < N; i++) {
                                ProcessCpuTracker.Stats st = this.mProcessCpuTracker.getStats(i);
                                if (st.working) {
                                    ProcessRecord pr = this.mPidsSelfLocked.get(st.pid);
                                    totalUTime += st.rel_utime;
                                    totalSTime += st.rel_stime;
                                    if (pr != null) {
                                        BatteryStatsImpl.Uid.Proc ps = pr.curProcBatteryStats;
                                        if (ps == null || !ps.isActive()) {
                                            ps = bstats.getProcessStatsLocked(pr.info.uid, pr.processName);
                                            pr.curProcBatteryStats = ps;
                                        }
                                        ps.addCpuTimeLocked(st.rel_utime, st.rel_stime);
                                        pr.curCpuTime += (long) (st.rel_utime + st.rel_stime);
                                    } else {
                                        BatteryStatsImpl.Uid.Proc ps2 = st.batteryStats;
                                        if (ps2 == null || !ps2.isActive()) {
                                            ps2 = bstats.getProcessStatsLocked(bstats.mapUid(st.uid), st.name);
                                            st.batteryStats = ps2;
                                        }
                                        ps2.addCpuTimeLocked(st.rel_utime, st.rel_stime);
                                    }
                                }
                            }
                            int userTime = this.mProcessCpuTracker.getLastUserTime();
                            int systemTime = this.mProcessCpuTracker.getLastSystemTime();
                            int iowaitTime = this.mProcessCpuTracker.getLastIoWaitTime();
                            int irqTime = this.mProcessCpuTracker.getLastIrqTime();
                            int softIrqTime = this.mProcessCpuTracker.getLastSoftIrqTime();
                            int idleTime = this.mProcessCpuTracker.getLastIdleTime();
                            bstats.finishAddingCpuLocked(totalUTime, totalSTime, userTime, systemTime, iowaitTime, irqTime, softIrqTime, idleTime);
                        }
                    }
                }
                if (this.mLastWriteTime < now - BATTERY_STATS_TIME) {
                    this.mLastWriteTime = now;
                    this.mBatteryStatsService.scheduleWriteToDisk();
                }
            }
        }
    }

    public void batteryNeedsCpuUpdate() {
        updateCpuStatsNow();
    }

    public void batteryPowerChanged(boolean onBattery) {
        updateCpuStatsNow();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                synchronized (this.mPidsSelfLocked) {
                    if (ActivityManagerDebugConfig.DEBUG_POWER) {
                        onBattery = true;
                    }
                    this.mOnBattery = onBattery;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void batterySendBroadcast(Intent intent) {
        broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, -1, 1000, -1);
    }

    private HashMap<String, IBinder> getCommonServicesLocked(boolean isolated) {
        if (isolated) {
            if (this.mIsolatedAppBindArgs == null) {
                this.mIsolatedAppBindArgs = new HashMap<>();
                this.mIsolatedAppBindArgs.put("package", ServiceManager.getService("package"));
            }
            return this.mIsolatedAppBindArgs;
        }
        if (this.mAppBindArgs == null) {
            this.mAppBindArgs = new HashMap<>();
            this.mAppBindArgs.put("package", ServiceManager.getService("package"));
            this.mAppBindArgs.put("window", ServiceManager.getService("window"));
            this.mAppBindArgs.put("alarm", ServiceManager.getService("alarm"));
        }
        return this.mAppBindArgs;
    }

    boolean setFocusedActivityLocked(ActivityRecord r, String reason) {
        IVoiceInteractionSession session;
        if (MultiWindowManager.isSupported()) {
            if (MultiWindowManager.DEBUG) {
                Slog.v(TAG_FOCUS, "setFocusedActivityLocked: r =  " + r + ", mFocusedActivity = " + this.mFocusedActivity);
            }
            if (r == null || (this.mFocusedActivity == r && r.task.getLaunchStackId() != 2)) {
                return false;
            }
        } else if (r == null || this.mFocusedActivity == r) {
            return false;
        }
        if (!r.isFocusable()) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                Slog.d(TAG_FOCUS, "setFocusedActivityLocked: unfocusable r=" + r);
            }
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d(TAG_FOCUS, "setFocusedActivityLocked: r=" + r);
        }
        boolean wasDoingSetFocusedActivity = this.mDoingSetFocusedActivity;
        if (wasDoingSetFocusedActivity) {
            Slog.w(TAG, "setFocusedActivityLocked: called recursively, r=" + r + ", reason=" + reason);
        }
        this.mDoingSetFocusedActivity = true;
        ActivityRecord last = this.mFocusedActivity;
        this.mFocusedActivity = r;
        if (r.task.isApplicationTask()) {
            if (this.mCurAppTimeTracker != r.appTimeTracker) {
                if (this.mCurAppTimeTracker != null) {
                    this.mCurAppTimeTracker.stop();
                    this.mHandler.obtainMessage(55, this.mCurAppTimeTracker).sendToTarget();
                    this.mStackSupervisor.clearOtherAppTimeTrackers(r.appTimeTracker);
                    this.mCurAppTimeTracker = null;
                }
                if (r.appTimeTracker != null) {
                    this.mCurAppTimeTracker = r.appTimeTracker;
                    startTimeTrackingFocusedActivityLocked();
                }
            } else {
                startTimeTrackingFocusedActivityLocked();
            }
        } else {
            r.appTimeTracker = null;
        }
        if (r.task.voiceInteractor != null) {
            startRunningVoiceLocked(r.task.voiceSession, r.info.applicationInfo.uid);
        } else {
            finishRunningVoiceLocked();
            if (last != null && ((session = last.task.voiceSession) != null || (session = last.voiceSession) != null)) {
                finishVoiceTask(session);
            }
        }
        if (this.mStackSupervisor.moveActivityStackToFront(r, reason + " setFocusedActivity")) {
            this.mWindowManager.setFocusedApp(r.appToken, true);
        }
        applyUpdateLockStateLocked(r);
        applyUpdateVrModeLocked(r);
        if (this.mFocusedActivity.userId != this.mLastFocusedUserId) {
            this.mHandler.removeMessages(53);
            this.mHandler.obtainMessage(53, this.mFocusedActivity.userId, 0).sendToTarget();
            this.mLastFocusedUserId = this.mFocusedActivity.userId;
        }
        if (this.mFocusedActivity != r) {
            Slog.w(TAG, "setFocusedActivityLocked: r=" + r + " but focused to " + this.mFocusedActivity);
        }
        this.mDoingSetFocusedActivity = wasDoingSetFocusedActivity;
        EventLogTags.writeAmFocusedActivity(this.mFocusedActivity == null ? -1 : this.mFocusedActivity.userId, this.mFocusedActivity == null ? "NULL" : this.mFocusedActivity.shortComponentName, reason);
        return true;
    }

    final void resetFocusedActivityIfNeededLocked(ActivityRecord goingAway) {
        ActivityRecord top;
        if (this.mFocusedActivity != goingAway) {
            return;
        }
        ActivityStack focusedStack = this.mStackSupervisor.getFocusedStack();
        if (focusedStack != null && (top = focusedStack.topActivity()) != null && top.userId != this.mLastFocusedUserId) {
            this.mHandler.removeMessages(53);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(53, top.userId, 0));
            this.mLastFocusedUserId = top.userId;
        }
        if (setFocusedActivityLocked(focusedStack.topRunningActivityLocked(), "resetFocusedActivityIfNeeded")) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d(TAG_FOCUS, "resetFocusedActivityIfNeeded: Setting focus to NULL prev mFocusedActivity=" + this.mFocusedActivity + " goingAway=" + goingAway);
        }
        this.mFocusedActivity = null;
        EventLogTags.writeAmFocusedActivity(-1, "NULL", "resetFocusedActivityIfNeeded");
    }

    public void setFocusedStack(int stackId) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "setFocusedStack()");
        if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d(TAG_FOCUS, "setFocusedStack: stackId=" + stackId);
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityStack stack = this.mStackSupervisor.getStack(stackId);
                    if (stack == null) {
                        return;
                    }
                    ActivityRecord r = stack.topRunningActivityLocked();
                    if (setFocusedActivityLocked(r, "setFocusedStack")) {
                        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                    }
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void setFocusedTask(int taskId) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "setFocusedTask()");
        if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d(TAG_FOCUS, "setFocusedTask: taskId=" + taskId);
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                    if (task == null) {
                        return;
                    }
                    if (!this.mUserController.shouldConfirmCredentials(task.userId)) {
                        ActivityRecord r = task.topRunningActivityLocked();
                        if (setFocusedActivityLocked(r, "setFocusedTask")) {
                            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                        }
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mActivityStarter.showConfirmDeviceCredential(task.userId);
                    if (task.stack != null && task.stack.mStackId == 2) {
                        this.mStackSupervisor.moveTaskToStackLocked(task.taskId, 1, false, false, "setFocusedTask", true);
                    }
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void registerTaskStackListener(ITaskStackListener listener) throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "registerTaskStackListener()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (listener != null) {
                    this.mTaskStackListeners.register(listener);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyActivityDrawn(IBinder token) {
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.d(TAG_VISIBILITY, "notifyActivityDrawn: token=" + token);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = this.mStackSupervisor.isInAnyStackLocked(token);
                if (r != null) {
                    r.task.stack.notifyActivityDrawnLocked(r);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    final void applyUpdateLockStateLocked(ActivityRecord r) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(37, r != null ? r.immersive : false ? 1 : 0, 0, r));
    }

    final void applyUpdateVrModeLocked(ActivityRecord r) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(VR_MODE_CHANGE_MSG, 0, 0, r));
    }

    private void applyVrModeIfNeededLocked(ActivityRecord r, boolean enable) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(69, enable ? 1 : 0, 0, r));
    }

    private void applyVrMode(boolean enabled, ComponentName packageName, int userId, ComponentName callingPackage, boolean immediate) {
        VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (immediate) {
            vrService.setVrModeImmediate(enabled, packageName, userId, callingPackage);
        } else {
            vrService.setVrMode(enabled, packageName, userId, callingPackage);
        }
    }

    final void showAskCompatModeDialogLocked(ActivityRecord r) {
        Message msg = Message.obtain();
        msg.what = 30;
        if (r.task.askedCompatMode) {
            r = null;
        }
        msg.obj = r;
        this.mUiHandler.sendMessage(msg);
    }

    final void showUnsupportedZoomDialogIfNeededLocked(ActivityRecord r) {
        if (this.mConfiguration.densityDpi == DisplayMetrics.DENSITY_DEVICE_STABLE || r.appInfo.requiresSmallestWidthDp <= this.mConfiguration.smallestScreenWidthDp) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = 70;
        msg.obj = r;
        this.mUiHandler.sendMessage(msg);
    }

    private int updateLruProcessInternalLocked(ProcessRecord app, long now, int index, String what, Object obj, ProcessRecord srcApp) {
        app.lastActivityTime = now;
        if (app.activities.size() > 0) {
            return index;
        }
        int lrui = this.mLruProcesses.lastIndexOf(app);
        if (lrui < 0) {
            Slog.wtf(TAG, "Adding dependent process " + app + " not on LRU list: " + what + " " + obj + " from " + srcApp);
            return index;
        }
        if (lrui >= index || lrui >= this.mLruProcessActivityStart) {
            return index;
        }
        this.mLruProcesses.remove(lrui);
        if (index > 0) {
            index--;
        }
        if (ActivityManagerDebugConfig.DEBUG_LRU) {
            Slog.d(TAG_LRU, "Moving dep from " + lrui + " to " + index + " in LRU list: " + app);
        }
        this.mLruProcesses.add(index, app);
        return index;
    }

    static void killProcessGroup(int uid, int pid) {
        if (sKillHandler != null) {
            sKillHandler.sendMessage(sKillHandler.obtainMessage(4000, uid, pid));
        } else {
            Slog.w(TAG, "Asked to kill process group before system bringup!");
            Process.killProcessGroup(uid, pid);
        }
    }

    final void removeLruProcessLocked(ProcessRecord app) {
        int lrui = this.mLruProcesses.lastIndexOf(app);
        if (lrui < 0) {
            return;
        }
        if (!app.killed) {
            Slog.e(TAG, "Removing process that hasn't been killed: " + app, new RuntimeException("here").fillInStackTrace());
            Process.killProcessQuiet(app.pid);
            killProcessGroup(app.uid, app.pid);
        }
        if (lrui <= this.mLruProcessActivityStart) {
            this.mLruProcessActivityStart--;
        }
        if (lrui <= this.mLruProcessServiceStart) {
            this.mLruProcessServiceStart--;
        }
        this.mLruProcesses.remove(lrui);
    }

    final void updateLruProcessLocked(ProcessRecord app, boolean activityChange, ProcessRecord client) {
        int nextIndex;
        boolean z = (app.activities.size() > 0 || app.hasClientActivities) ? true : app.treatLikeActivity;
        if (activityChange || !z) {
            this.mLruSeq++;
            long now = SystemClock.uptimeMillis();
            app.lastActivityTime = now;
            if (z) {
                int N = this.mLruProcesses.size();
                if (N > 0 && this.mLruProcesses.get(N - 1) == app) {
                    if (ActivityManagerDebugConfig.DEBUG_LRU) {
                        Slog.d(TAG_LRU, "Not moving, already top activity: " + app);
                        return;
                    }
                    return;
                }
            } else if (this.mLruProcessServiceStart > 0 && this.mLruProcesses.get(this.mLruProcessServiceStart - 1) == app) {
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(TAG_LRU, "Not moving, already top other: " + app);
                    return;
                }
                return;
            }
            int lrui = this.mLruProcesses.lastIndexOf(app);
            if (app.persistent && lrui >= 0) {
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(TAG_LRU, "Not moving, persistent: " + app);
                    return;
                }
                return;
            }
            if (lrui >= 0) {
                if (lrui < this.mLruProcessActivityStart) {
                    this.mLruProcessActivityStart--;
                }
                if (lrui < this.mLruProcessServiceStart) {
                    this.mLruProcessServiceStart--;
                }
                this.mLruProcesses.remove(lrui);
            }
            if (z) {
                int N2 = this.mLruProcesses.size();
                if (app.activities.size() == 0 && this.mLruProcessActivityStart < N2 - 1) {
                    if (ActivityManagerDebugConfig.DEBUG_LRU) {
                        Slog.d(TAG_LRU, "Adding to second-top of LRU activity list: " + app);
                    }
                    this.mLruProcesses.add(N2 - 1, app);
                    int uid = app.info.uid;
                    int i = N2 - 2;
                    while (i > this.mLruProcessActivityStart) {
                        ProcessRecord subProc = this.mLruProcesses.get(i);
                        if (subProc.info.uid != uid) {
                            break;
                        }
                        if (this.mLruProcesses.get(i - 1).info.uid != uid) {
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                Slog.d(TAG_LRU, "Pushing uid " + uid + " swapping at " + i + ": " + this.mLruProcesses.get(i) + " : " + this.mLruProcesses.get(i - 1));
                            }
                            ProcessRecord tmp = this.mLruProcesses.get(i);
                            this.mLruProcesses.set(i, this.mLruProcesses.get(i - 1));
                            this.mLruProcesses.set(i - 1, tmp);
                            i--;
                        }
                        i--;
                    }
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_LRU) {
                        Slog.d(TAG_LRU, "Adding to top of LRU activity list: " + app);
                    }
                    this.mLruProcesses.add(app);
                }
                nextIndex = this.mLruProcessServiceStart;
            } else {
                int index = this.mLruProcessServiceStart;
                if (client != null) {
                    int clientIndex = this.mLruProcesses.lastIndexOf(client);
                    if (ActivityManagerDebugConfig.DEBUG_LRU && clientIndex < 0) {
                        Slog.d(TAG_LRU, "Unknown client " + client + " when updating " + app);
                    }
                    if (clientIndex <= lrui) {
                        clientIndex = lrui;
                    }
                    if (clientIndex >= 0 && index > clientIndex) {
                        index = clientIndex;
                    }
                }
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(TAG_LRU, "Adding at " + index + " of LRU list: " + app);
                }
                this.mLruProcesses.add(index, app);
                nextIndex = index - 1;
                this.mLruProcessActivityStart++;
                this.mLruProcessServiceStart++;
            }
            for (int j = app.connections.size() - 1; j >= 0; j--) {
                ConnectionRecord cr = app.connections.valueAt(j);
                if (cr.binding != null && !cr.serviceDead && cr.binding.service != null && cr.binding.service.app != null && cr.binding.service.app.lruSeq != this.mLruSeq && !cr.binding.service.app.persistent) {
                    nextIndex = updateLruProcessInternalLocked(cr.binding.service.app, now, nextIndex, "service connection", cr, app);
                }
            }
            for (int j2 = app.conProviders.size() - 1; j2 >= 0; j2--) {
                ContentProviderRecord cpr = app.conProviders.get(j2).provider;
                if (cpr.proc != null && cpr.proc.lruSeq != this.mLruSeq && !cpr.proc.persistent) {
                    nextIndex = updateLruProcessInternalLocked(cpr.proc, now, nextIndex, "provider reference", cpr, app);
                }
            }
        }
    }

    final ProcessRecord getProcessRecordLocked(String processName, int uid, boolean keepIfLarge) {
        if (uid == 1000) {
            SparseArray<ProcessRecord> procs = (SparseArray) this.mProcessNames.getMap().get(processName);
            if (procs == null) {
                return null;
            }
            int procCount = procs.size();
            for (int i = 0; i < procCount; i++) {
                int procUid = procs.keyAt(i);
                if (!UserHandle.isApp(procUid) && UserHandle.isSameUser(procUid, uid)) {
                    return procs.valueAt(i);
                }
            }
        }
        ProcessRecord proc = (ProcessRecord) this.mProcessNames.get(processName, uid);
        if (proc != null && !keepIfLarge && this.mLastMemoryLevel > 0 && proc.setProcState >= 16) {
            if (ActivityManagerDebugConfig.DEBUG_PSS) {
                Slog.d(TAG_PSS, "May not keep " + proc + ": pss=" + proc.lastCachedPss);
            }
            if (proc.lastCachedPss >= this.mProcessList.getCachedRestoreThresholdKb()) {
                if (proc.baseProcessTracker != null) {
                    proc.baseProcessTracker.reportCachedKill(proc.pkgList, proc.lastCachedPss);
                }
                proc.kill(Long.toString(proc.lastCachedPss) + "k from cached", true);
            }
        }
        return proc;
    }

    void notifyPackageUse(String packageName, int reason) {
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            pm.notifyPackageUse(packageName, reason);
        } catch (RemoteException e) {
        }
    }

    boolean isNextTransitionForward() {
        int transit = this.mWindowManager.getPendingAppTransition();
        return transit == 6 || transit == 8 || transit == 10;
    }

    int startIsolatedProcess(String entryPoint, String[] entryPointArgs, String processName, String abiOverride, int uid, Runnable crashHandler) {
        int i;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ApplicationInfo info = new ApplicationInfo();
                info.uid = 1000;
                info.processName = processName;
                info.className = entryPoint;
                info.packageName = "android";
                ProcessRecord proc = startProcessLocked(processName, info, false, 0, "", null, true, true, uid, true, abiOverride, entryPoint, entryPointArgs, crashHandler);
                i = proc != null ? proc.pid : 0;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return i;
    }

    final ProcessRecord startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting, boolean isolated, boolean keepIfLarge) {
        return startProcessLocked(processName, info, knownToBeDead, intentFlags, hostingType, hostingName, allowWhileBooting, isolated, 0, keepIfLarge, null, null, null, null);
    }

    final ProcessRecord startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting, boolean isolated, int isolatedUid, boolean keepIfLarge, String abiOverride, String entryPoint, String[] entryPointArgs, Runnable crashHandler) {
        ProcessRecord app;
        long startTime = SystemClock.elapsedRealtime();
        if (isolated) {
            app = null;
        } else {
            app = getProcessRecordLocked(processName, info.uid, keepIfLarge);
            checkTime(startTime, "startProcess: after getProcessRecord");
            if ((intentFlags & 4) == 0) {
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.v(TAG, "Clearing bad process: " + info.uid + "/" + info.processName);
                }
                this.mAppErrors.resetProcessCrashTimeLocked(info);
                if (this.mAppErrors.isBadProcessLocked(info)) {
                    EventLog.writeEvent(EventLogTags.AM_PROC_GOOD, Integer.valueOf(UserHandle.getUserId(info.uid)), Integer.valueOf(info.uid), info.processName);
                    this.mAppErrors.clearBadProcessLocked(info);
                    if (app != null) {
                        app.bad = false;
                    }
                }
            } else if (this.mAppErrors.isBadProcessLocked(info)) {
                if (!ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    return null;
                }
                Slog.v(TAG, "Bad process: " + info.uid + "/" + info.processName);
                return null;
            }
        }
        nativeMigrateToBoost();
        this.mIsBoosted = true;
        this.mBoostStartTime = SystemClock.uptimeMillis();
        Message msg = this.mHandler.obtainMessage(APP_BOOST_DEACTIVATE_MSG);
        this.mHandler.sendMessageDelayed(msg, 3000L);
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.v(TAG_PROCESSES, "startProcess: name=" + processName + " app=" + app + " knownToBeDead=" + knownToBeDead + " thread=" + (app != null ? app.thread : null) + " pid=" + (app != null ? app.pid : -1));
        }
        if (app != null && app.pid > 0) {
            if ((!knownToBeDead && !app.killed) || app.thread == null) {
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.v(TAG_PROCESSES, "App already running: " + app);
                }
                app.addPackage(info.packageName, info.versionCode, this.mProcessStats);
                checkTime(startTime, "startProcess: done, added package to proc");
                return app;
            }
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_PROCESSES, "App died: " + app);
            }
            checkTime(startTime, "startProcess: bad proc running, killing");
            killProcessGroup(app.uid, app.pid);
            handleAppDiedLocked(app, true, true);
            checkTime(startTime, "startProcess: done killing old proc");
        }
        String strFlattenToShortString = hostingName != null ? hostingName.flattenToShortString() : null;
        if (app == null) {
            checkTime(startTime, "startProcess: creating new process record");
            app = newProcessRecordLocked(info, processName, isolated, isolatedUid);
            if (app == null) {
                Slog.w(TAG, "Failed making new process record for " + processName + "/" + info.uid + " isolated=" + isolated);
                return null;
            }
            app.crashHandler = crashHandler;
            checkTime(startTime, "startProcess: done creating new process record");
        } else {
            app.addPackage(info.packageName, info.versionCode, this.mProcessStats);
            checkTime(startTime, "startProcess: added package to existing proc");
        }
        if (this.mProcessesReady || isAllowedWhileBooting(info) || allowWhileBooting) {
            checkTime(startTime, "startProcess: stepping in to startProcess");
            startProcessLocked(app, hostingType, strFlattenToShortString, abiOverride, entryPoint, entryPointArgs);
            checkTime(startTime, "startProcess: done starting proc!");
            if (app.pid != 0) {
                return app;
            }
            return null;
        }
        if (!this.mProcessesOnHold.contains(app)) {
            this.mProcessesOnHold.add(app);
        }
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.v(TAG_PROCESSES, "System not ready, putting on hold: " + app);
        }
        checkTime(startTime, "startProcess: returning with proc on hold");
        return app;
    }

    boolean isAllowedWhileBooting(ApplicationInfo ai) {
        return (ai.flags & 8) != 0;
    }

    private final void startProcessLocked(ProcessRecord app, String hostingType, String hostingNameStr) {
        startProcessLocked(app, hostingType, hostingNameStr, null, null, null);
    }

    private final void startProcessLocked(ProcessRecord app, String hostingType, String hostingNameStr, String abiOverride, String entryPoint, String[] entryPointArgs) {
        ProcessRecord oldApp;
        long startTime = SystemClock.elapsedRealtime();
        if (app.pid > 0 && app.pid != MY_PID) {
            checkTime(startTime, "startProcess: removing from pids map");
            synchronized (this.mPidsSelfLocked) {
                this.mPidsSelfLocked.remove(app.pid);
                this.mHandler.removeMessages(20, app);
            }
            checkTime(startTime, "startProcess: done removing from pids map");
            app.setPid(0);
        }
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES && this.mProcessesOnHold.contains(app)) {
            Slog.v(TAG_PROCESSES, "startProcessLocked removing on hold: " + app);
        }
        this.mProcessesOnHold.remove(app);
        checkTime(startTime, "startProcess: starting to update cpu stats");
        updateCpuStats();
        checkTime(startTime, "startProcess: done updating cpu stats");
        try {
            try {
                int userId = UserHandle.getUserId(app.uid);
                AppGlobals.getPackageManager().checkPackageStartable(app.info.packageName, userId);
                AMEventHookData.StartProcessForActivity eventData = AMEventHookData.StartProcessForActivity.createInstance();
                eventData.set(new Object[]{hostingType, app.info.packageName});
                this.mAMEventHook.hook(AMEventHook.Event.AM_StartProcessForActivity, eventData);
                int uid = app.uid;
                int[] gids = null;
                int mountExternal = 0;
                if (!app.isolated) {
                    try {
                        checkTime(startTime, "startProcess: getting gids from package manager");
                        IPackageManager pm = AppGlobals.getPackageManager();
                        int[] permGids = pm.getPackageGids(app.info.packageName, 268435456, app.userId);
                        MountServiceInternal mountServiceInternal = (MountServiceInternal) LocalServices.getService(MountServiceInternal.class);
                        mountExternal = mountServiceInternal.getExternalStorageMountMode(uid, app.info.packageName);
                        if (ArrayUtils.isEmpty(permGids)) {
                            gids = new int[2];
                        } else {
                            gids = new int[permGids.length + 2];
                            System.arraycopy(permGids, 0, gids, 2, permGids.length);
                        }
                        gids[0] = UserHandle.getSharedAppGid(UserHandle.getAppId(uid));
                        gids[1] = UserHandle.getUserGid(UserHandle.getUserId(uid));
                    } catch (RemoteException e) {
                        throw e.rethrowAsRuntimeException();
                    }
                }
                checkTime(startTime, "startProcess: building args");
                if (this.mFactoryTest != 0) {
                    if (this.mFactoryTest == 1 && this.mTopComponent != null && app.processName.equals(this.mTopComponent.getPackageName())) {
                        uid = 0;
                    }
                    if (this.mFactoryTest == 2 && (app.info.flags & 16) != 0) {
                        uid = 0;
                    }
                }
                int debugFlags = 0;
                if ((app.info.flags & 2) != 0) {
                    debugFlags = 1 | 2;
                }
                if ((app.info.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0 || this.mSafeMode) {
                    debugFlags |= 8;
                }
                if ("1".equals(SystemProperties.get("debug.checkjni"))) {
                    debugFlags |= 2;
                }
                String genDebugInfoProperty = SystemProperties.get("debug.generate-debug-info");
                if ("true".equals(genDebugInfoProperty)) {
                    debugFlags |= 32;
                }
                if ("1".equals(SystemProperties.get("debug.jni.logging"))) {
                    debugFlags |= 16;
                }
                if ("1".equals(SystemProperties.get("debug.assert"))) {
                    debugFlags |= 4;
                }
                if (this.mNativeDebuggingApp != null && this.mNativeDebuggingApp.equals(app.processName)) {
                    debugFlags = debugFlags | 64 | 32 | 128;
                    this.mNativeDebuggingApp = null;
                }
                String requiredAbi = abiOverride != null ? abiOverride : app.info.primaryCpuAbi;
                if (requiredAbi == null) {
                    requiredAbi = Build.SUPPORTED_ABIS[0];
                }
                String instructionSet = null;
                if (app.info.primaryCpuAbi != null) {
                    instructionSet = VMRuntime.getInstructionSet(app.info.primaryCpuAbi);
                }
                app.gids = gids;
                app.requiredAbi = requiredAbi;
                app.instructionSet = instructionSet;
                boolean isActivityProcess = entryPoint == null;
                if (entryPoint == null) {
                    entryPoint = "android.app.ActivityThread";
                }
                Trace.traceBegin(64L, "Start proc: " + app.processName);
                checkTime(startTime, "startProcess: asking zygote to start proc");
                Process.ProcessStartResult startResult = Process.start(entryPoint, app.processName, uid, uid, gids, debugFlags, mountExternal, app.info.targetSdkVersion, app.info.seinfo, requiredAbi, instructionSet, app.info.dataDir, entryPointArgs);
                checkTime(startTime, "startProcess: returned from zygote!");
                Trace.traceEnd(64L);
                if (app.isolated) {
                    this.mBatteryStatsService.addIsolatedUid(app.uid, app.info.uid);
                }
                this.mBatteryStatsService.noteProcessStart(app.processName, app.info.uid);
                checkTime(startTime, "startProcess: done updating battery stats");
                Object[] objArr = new Object[6];
                objArr[0] = Integer.valueOf(UserHandle.getUserId(uid));
                objArr[1] = Integer.valueOf(startResult.pid);
                objArr[2] = Integer.valueOf(uid);
                objArr[3] = app.processName;
                objArr[4] = hostingType;
                objArr[5] = hostingNameStr != null ? hostingNameStr : "";
                EventLog.writeEvent(EventLogTags.AM_PROC_START, objArr);
                try {
                    AppGlobals.getPackageManager().logAppProcessStartIfNeeded(app.processName, app.uid, app.info.seinfo, app.info.sourceDir, startResult.pid);
                } catch (RemoteException e2) {
                }
                StringBuilder bootbuf = new StringBuilder();
                bootbuf.setLength(0);
                bootbuf.append("AP_Init:[");
                bootbuf.append(hostingType);
                bootbuf.append("]:[");
                bootbuf.append(app.processName);
                if (hostingNameStr != null) {
                    bootbuf.append("]:[");
                    bootbuf.append(hostingNameStr);
                }
                bootbuf.append("]:pid:");
                bootbuf.append(startResult.pid);
                if (app.persistent) {
                    bootbuf.append(":(PersistAP)");
                }
                BootEvent.addBootEvent(bootbuf.toString());
                if (app.persistent) {
                    Watchdog.getInstance().processStarted(app.processName, startResult.pid);
                }
                checkTime(startTime, "startProcess: building log message");
                StringBuilder buf = this.mStringBuilder;
                buf.setLength(0);
                buf.append("Start proc ");
                buf.append(startResult.pid);
                buf.append(':');
                buf.append(app.processName);
                buf.append('/');
                UserHandle.formatUid(buf, uid);
                if (!isActivityProcess) {
                    buf.append(" [");
                    buf.append(entryPoint);
                    buf.append("]");
                }
                buf.append(" for ");
                buf.append(hostingType);
                if (hostingNameStr != null) {
                    buf.append(" ");
                    buf.append(hostingNameStr);
                }
                Slog.i(TAG, buf.toString());
                app.setPid(startResult.pid);
                app.usingWrapper = startResult.usingWrapper;
                app.removed = false;
                app.killed = false;
                app.killedByAm = false;
                checkTime(startTime, "startProcess: starting to update pids map");
                synchronized (this.mPidsSelfLocked) {
                    oldApp = this.mPidsSelfLocked.get(startResult.pid);
                }
                if (oldApp != null && !app.isolated) {
                    Slog.w(TAG, "Reusing pid " + startResult.pid + " while app is still mapped to it");
                    cleanUpApplicationRecordLocked(oldApp, false, false, -1, true);
                }
                synchronized (this.mPidsSelfLocked) {
                    this.mPidsSelfLocked.put(startResult.pid, app);
                    if (isActivityProcess) {
                        Message msg = this.mHandler.obtainMessage(20);
                        msg.obj = app;
                        this.mHandler.sendMessageDelayed(msg, startResult.usingWrapper ? PROC_START_TIMEOUT_WITH_WRAPPER : 10000);
                    }
                }
                checkTime(startTime, "startProcess: done updating pids map");
            } catch (RemoteException e3) {
                throw e3.rethrowAsRuntimeException();
            }
        } catch (RuntimeException e4) {
            Slog.e(TAG, "Failure starting process " + app.processName, e4);
            forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false, false, true, false, false, UserHandle.getUserId(app.userId), "start failure");
        }
    }

    void updateUsageStats(ActivityRecord component, boolean resumed) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "updateUsageStats: comp=" + component + "res=" + resumed);
        }
        BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
        if (resumed) {
            if (this.mUsageStatsService != null) {
                this.mUsageStatsService.reportEvent(component.realActivity, component.userId, 1);
            }
            synchronized (stats) {
                stats.noteActivityResumedLocked(component.app.uid);
            }
        }
        if (this.mUsageStatsService != null) {
            this.mUsageStatsService.reportEvent(component.realActivity, component.userId, 2);
        }
        synchronized (stats) {
            stats.noteActivityPausedLocked(component.app.uid);
        }
    }

    Intent getHomeIntent() {
        Intent intent = new Intent(this.mTopAction, this.mTopData != null ? Uri.parse(this.mTopData) : null);
        intent.setComponent(this.mTopComponent);
        intent.addFlags(256);
        if (this.mFactoryTest != 1) {
            intent.addCategory("android.intent.category.HOME");
        }
        return intent;
    }

    boolean startHomeActivityLocked(int userId, String reason) {
        if (this.mFactoryTest == 1 && this.mTopAction == null) {
            return false;
        }
        Intent intent = getHomeIntent();
        ActivityInfo aInfo = resolveActivityInfo(intent, 1024, userId);
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            ActivityInfo aInfo2 = new ActivityInfo(aInfo);
            aInfo2.applicationInfo = getAppInfoForUser(aInfo2.applicationInfo, userId);
            ProcessRecord app = getProcessRecordLocked(aInfo2.processName, aInfo2.applicationInfo.uid, true);
            if (app == null || app.instrumentationClass == null) {
                intent.setFlags(intent.getFlags() | 268435456);
                this.mActivityStarter.startHomeActivityLocked(intent, aInfo2, reason);
            }
            if (this.mStackSupervisor.mLastResumedActivity.packageName == null || this.mStackSupervisor.isUpdatedLastActivityWhenStartHome(aInfo2.packageName, aInfo2.name)) {
                this.mStackSupervisor.mLastResumedActivity.packageName = aInfo2.packageName;
                this.mStackSupervisor.mLastResumedActivity.activityName = aInfo2.name;
                this.mStackSupervisor.mLastResumedActivity.activityType = 1;
            }
        } else {
            Slog.wtf(TAG, "No home screen found for " + intent, new Throwable());
        }
        return true;
    }

    private ActivityInfo resolveActivityInfo(Intent intent, int flags, int userId) {
        ActivityInfo ai = null;
        ComponentName comp = intent.getComponent();
        try {
            if (comp != null) {
                ai = AppGlobals.getPackageManager().getActivityInfo(comp, flags, userId);
            } else {
                ResolveInfo info = AppGlobals.getPackageManager().resolveIntent(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), flags, userId);
                if (info != null) {
                    ai = info.activityInfo;
                }
            }
        } catch (RemoteException e) {
        }
        return ai;
    }

    void startSetupActivityLocked() {
        String vers;
        if (this.mCheckedForSetup) {
            return;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        if (this.mFactoryTest == 1 || Settings.Global.getInt(resolver, "device_provisioned", 0) == 0) {
            return;
        }
        this.mCheckedForSetup = true;
        Intent intent = new Intent("android.intent.action.UPGRADE_SETUP");
        List<ResolveInfo> ris = this.mContext.getPackageManager().queryIntentActivities(intent, 1048704);
        if (ris.isEmpty()) {
            return;
        }
        ResolveInfo ri = ris.get(0);
        if (ri.activityInfo.metaData != null) {
            vers = ri.activityInfo.metaData.getString("android.SETUP_VERSION");
        } else {
            vers = null;
        }
        if (vers == null && ri.activityInfo.applicationInfo.metaData != null) {
            vers = ri.activityInfo.applicationInfo.metaData.getString("android.SETUP_VERSION");
        }
        String lastVers = Settings.Secure.getString(resolver, "last_setup_shown");
        if (vers == null || vers.equals(lastVers)) {
            return;
        }
        intent.setFlags(268435456);
        intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
        this.mActivityStarter.startActivityLocked(null, intent, null, null, ri.activityInfo, null, null, null, null, null, 0, 0, 0, null, 0, 0, 0, null, false, false, null, null, null);
    }

    CompatibilityInfo compatibilityInfoForPackageLocked(ApplicationInfo ai) {
        return this.mCompatModePackages.compatibilityInfoForPackageLocked(ai);
    }

    void enforceNotIsolatedCaller(String caller) {
        if (!UserHandle.isIsolated(Binder.getCallingUid())) {
        } else {
            throw new SecurityException("Isolated process not allowed to call " + caller);
        }
    }

    void enforceShellRestriction(String restriction, int userHandle) {
        if (Binder.getCallingUid() != PENDING_ASSIST_EXTRAS_LONG_TIMEOUT) {
            return;
        }
        if (userHandle >= 0 && !this.mUserController.hasUserRestriction(restriction, userHandle)) {
        } else {
            throw new SecurityException("Shell does not have permission to access user " + userHandle);
        }
    }

    public int getFrontActivityScreenCompatMode() {
        int frontActivityScreenCompatModeLocked;
        enforceNotIsolatedCaller("getFrontActivityScreenCompatMode");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                frontActivityScreenCompatModeLocked = this.mCompatModePackages.getFrontActivityScreenCompatModeLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return frontActivityScreenCompatModeLocked;
    }

    public void setFrontActivityScreenCompatMode(int mode) {
        enforceCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY", "setFrontActivityScreenCompatMode");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mCompatModePackages.setFrontActivityScreenCompatModeLocked(mode);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int getPackageScreenCompatMode(String packageName) {
        int packageScreenCompatModeLocked;
        enforceNotIsolatedCaller("getPackageScreenCompatMode");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                packageScreenCompatModeLocked = this.mCompatModePackages.getPackageScreenCompatModeLocked(packageName);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return packageScreenCompatModeLocked;
    }

    public void setPackageScreenCompatMode(String packageName, int mode) {
        enforceCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY", "setPackageScreenCompatMode");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mCompatModePackages.setPackageScreenCompatModeLocked(packageName, mode);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean getPackageAskScreenCompat(String packageName) {
        boolean packageAskCompatModeLocked;
        enforceNotIsolatedCaller("getPackageAskScreenCompat");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                packageAskCompatModeLocked = this.mCompatModePackages.getPackageAskCompatModeLocked(packageName);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return packageAskCompatModeLocked;
    }

    public void setPackageAskScreenCompat(String packageName, boolean ask) {
        enforceCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY", "setPackageAskScreenCompat");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mCompatModePackages.setPackageAskCompatModeLocked(packageName, ask);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private boolean hasUsageStatsPermission(String callingPackage) {
        int mode = this.mAppOpsService.checkOperation(43, Binder.getCallingUid(), callingPackage);
        return mode == 3 ? checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0 : mode == 0;
    }

    public int getPackageProcessState(String packageName, String callingPackage) {
        if (!hasUsageStatsPermission(callingPackage)) {
            enforceCallingPermission("android.permission.GET_PACKAGE_IMPORTANCE", "getPackageProcessState");
        }
        int procState = -1;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord proc = this.mLruProcesses.get(i);
                    if (procState == -1 || procState > proc.setProcState) {
                        boolean found = false;
                        for (int j = proc.pkgList.size() - 1; j >= 0 && !found; j--) {
                            if (proc.pkgList.keyAt(j).equals(packageName)) {
                                procState = proc.setProcState;
                                found = true;
                            }
                        }
                        if (proc.pkgDeps != null && !found) {
                            int j2 = proc.pkgDeps.size() - 1;
                            while (true) {
                                if (j2 < 0) {
                                    break;
                                }
                                if (proc.pkgDeps.valueAt(j2).equals(packageName)) {
                                    procState = proc.setProcState;
                                    break;
                                }
                                j2--;
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return procState;
    }

    public boolean setProcessMemoryTrimLevel(String process, int userId, int level) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord app = findProcessLocked(process, userId, "setProcessMemoryTrimLevel");
                if (app == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                if (app.trimMemoryLevel < level && app.thread != null && (level < 20 || app.curProcState >= 7)) {
                    try {
                        app.thread.scheduleTrimMemory(level);
                        app.trimMemoryLevel = level;
                        resetPriorityAfterLockedSection();
                        return true;
                    } catch (RemoteException e) {
                    }
                }
                resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void dispatchProcessesChanged() {
        int N;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                N = this.mPendingProcessChanges.size();
                if (this.mActiveProcessChanges.length < N) {
                    this.mActiveProcessChanges = new ProcessChangeItem[N];
                }
                this.mPendingProcessChanges.toArray(this.mActiveProcessChanges);
                this.mPendingProcessChanges.clear();
                if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                    Slog.i(TAG_PROCESS_OBSERVERS, "*** Delivering " + N + " process changes");
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        int i = this.mProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IProcessObserver observer = this.mProcessObservers.getBroadcastItem(i);
            if (observer != null) {
                for (int j = 0; j < N; j++) {
                    try {
                        ProcessChangeItem item = this.mActiveProcessChanges[j];
                        if ((item.changes & 1) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                                Slog.i(TAG_PROCESS_OBSERVERS, "ACTIVITIES CHANGED pid=" + item.pid + " uid=" + item.uid + ": " + item.foregroundActivities);
                            }
                            observer.onForegroundActivitiesChanged(item.pid, item.uid, item.foregroundActivities);
                        }
                        if ((item.changes & 2) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                                Slog.i(TAG_PROCESS_OBSERVERS, "PROCSTATE CHANGED pid=" + item.pid + " uid=" + item.uid + ": " + item.processState);
                            }
                            observer.onProcessStateChanged(item.pid, item.uid, item.processState);
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
        }
        this.mProcessObservers.finishBroadcast();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                for (int j2 = 0; j2 < N; j2++) {
                    this.mAvailProcessChanges.add(this.mActiveProcessChanges[j2]);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private void dispatchProcessDied(int pid, int uid) {
        int i = this.mProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IProcessObserver observer = this.mProcessObservers.getBroadcastItem(i);
            if (observer != null) {
                try {
                    observer.onProcessDied(pid, uid);
                } catch (RemoteException e) {
                }
            }
        }
        this.mProcessObservers.finishBroadcast();
    }

    private void dispatchUidsChanged() {
        int N;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                N = this.mPendingUidChanges.size();
                if (this.mActiveUidChanges.length < N) {
                    this.mActiveUidChanges = new UidRecord.ChangeItem[N];
                }
                for (int i = 0; i < N; i++) {
                    UidRecord.ChangeItem change = this.mPendingUidChanges.get(i);
                    this.mActiveUidChanges[i] = change;
                    if (change.uidRecord != null) {
                        change.uidRecord.pendingChange = null;
                        change.uidRecord = null;
                    }
                }
                this.mPendingUidChanges.clear();
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(TAG_UID_OBSERVERS, "*** Delivering " + N + " uid changes");
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (this.mLocalPowerManager != null) {
            for (int j = 0; j < N; j++) {
                UidRecord.ChangeItem item = this.mActiveUidChanges[j];
                if (item.change == 1 || item.change == 2) {
                    this.mLocalPowerManager.uidGone(item.uid);
                } else {
                    this.mLocalPowerManager.updateUidProcState(item.uid, item.processState);
                }
            }
        }
        int i2 = this.mUidObservers.beginBroadcast();
        while (i2 > 0) {
            i2--;
            IUidObserver observer = this.mUidObservers.getBroadcastItem(i2);
            int which = ((Integer) this.mUidObservers.getBroadcastCookie(i2)).intValue();
            if (observer != null) {
                for (int j2 = 0; j2 < N; j2++) {
                    try {
                        UidRecord.ChangeItem item2 = this.mActiveUidChanges[j2];
                        int change2 = item2.change;
                        UidRecord validateUid = null;
                        if (i2 == 0) {
                            UidRecord validateUid2 = this.mValidateUids.get(item2.uid);
                            validateUid = validateUid2;
                            if (validateUid == null && change2 != 1 && change2 != 2) {
                                validateUid = new UidRecord(item2.uid);
                                this.mValidateUids.put(item2.uid, validateUid);
                            }
                        }
                        if (change2 == 3 || change2 == 2) {
                            if ((which & 4) != 0) {
                                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                    Slog.i(TAG_UID_OBSERVERS, "UID idle uid=" + item2.uid);
                                }
                                observer.onUidIdle(item2.uid);
                            }
                            if (i2 == 0 && validateUid != null) {
                                validateUid.idle = true;
                            }
                        } else if (change2 == 4) {
                            if ((which & 8) != 0) {
                                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                    Slog.i(TAG_UID_OBSERVERS, "UID active uid=" + item2.uid);
                                }
                                observer.onUidActive(item2.uid);
                            }
                            if (i2 == 0) {
                                validateUid.idle = false;
                            }
                        }
                        if (change2 == 1 || change2 == 2) {
                            if ((which & 2) != 0) {
                                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                    Slog.i(TAG_UID_OBSERVERS, "UID gone uid=" + item2.uid);
                                }
                                observer.onUidGone(item2.uid);
                            }
                            if (i2 == 0 && validateUid != null) {
                                this.mValidateUids.remove(item2.uid);
                            }
                        } else {
                            if ((which & 1) != 0) {
                                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                    Slog.i(TAG_UID_OBSERVERS, "UID CHANGED uid=" + item2.uid + ": " + item2.processState);
                                }
                                observer.onUidStateChanged(item2.uid, item2.processState);
                            }
                            if (i2 == 0) {
                                int i3 = item2.processState;
                                validateUid.setProcState = i3;
                                validateUid.curProcState = i3;
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
        }
        this.mUidObservers.finishBroadcast();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                for (int j3 = 0; j3 < N; j3++) {
                    this.mAvailUidChanges.add(this.mActiveUidChanges[j3]);
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public final int startActivity(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
        return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profilerInfo, bOptions, UserHandle.getCallingUserId());
    }

    final int startActivity(Intent intent, ActivityStackSupervisor.ActivityContainer container) {
        enforceNotIsolatedCaller("ActivityContainer.startActivity");
        int userId = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), this.mStackSupervisor.mCurrentUser, false, 2, "ActivityContainer", null);
        String mimeType = intent.getType();
        Uri data = intent.getData();
        if (mimeType == null && data != null && "content".equals(data.getScheme())) {
            mimeType = getProviderMimeType(data, userId);
        }
        container.checkEmbeddedAllowedInner(userId, intent, mimeType);
        intent.addFlags(402718720);
        return this.mActivityStarter.startActivityMayWait(null, -1, null, intent, mimeType, null, null, null, null, 0, 0, null, null, null, null, false, userId, container, null);
    }

    public final int startActivityAsUser(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
        enforceNotIsolatedCaller("startActivity");
        return this.mActivityStarter.startActivityMayWait(caller, -1, callingPackage, intent, resolvedType, null, null, resultTo, resultWho, requestCode, startFlags, profilerInfo, null, null, bOptions, false, this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startActivity", null), null, null);
    }

    public final int startActivityAsCaller(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, boolean ignoreTargetSecurity, int userId) {
        ActivityRecord sourceRecord;
        int targetUid;
        String targetPackage;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (resultTo == null) {
                    throw new SecurityException("Must be called from an activity");
                }
                sourceRecord = this.mStackSupervisor.isInAnyStackLocked(resultTo);
                if (sourceRecord == null) {
                    throw new SecurityException("Called with bad activity token: " + resultTo);
                }
                if (!sourceRecord.info.packageName.equals("android")) {
                    throw new SecurityException("Must be called from an activity that is declared in the android package");
                }
                if (sourceRecord.app == null) {
                    throw new SecurityException("Called without a process attached to activity");
                }
                if (UserHandle.getAppId(sourceRecord.app.uid) != 1000 && sourceRecord.app.uid != sourceRecord.launchedFromUid) {
                    throw new SecurityException("Calling activity in uid " + sourceRecord.app.uid + " must be system uid or original calling uid " + sourceRecord.launchedFromUid);
                }
                if (ignoreTargetSecurity) {
                    if (intent.getComponent() == null) {
                        throw new SecurityException("Component must be specified with ignoreTargetSecurity");
                    }
                    if (intent.getSelector() != null) {
                        throw new SecurityException("Selector not allowed with ignoreTargetSecurity");
                    }
                }
                targetUid = sourceRecord.launchedFromUid;
                targetPackage = sourceRecord.launchedFromPackage;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (userId == -10000) {
            userId = UserHandle.getUserId(sourceRecord.app.uid);
        }
        try {
            int ret = this.mActivityStarter.startActivityMayWait(null, targetUid, targetPackage, intent, resolvedType, null, null, resultTo, resultWho, requestCode, startFlags, null, null, null, bOptions, ignoreTargetSecurity, userId, null, null);
            return ret;
        } catch (SecurityException e) {
            throw e;
        }
    }

    public final IActivityManager.WaitResult startActivityAndWait(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
        enforceNotIsolatedCaller("startActivityAndWait");
        int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startActivityAndWait", null);
        IActivityManager.WaitResult res = new IActivityManager.WaitResult();
        this.mActivityStarter.startActivityMayWait(caller, -1, callingPackage, intent, resolvedType, null, null, resultTo, resultWho, requestCode, startFlags, profilerInfo, res, null, bOptions, false, userId2, null, null);
        return res;
    }

    public final int startActivityWithConfig(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, Configuration config, Bundle bOptions, int userId) {
        enforceNotIsolatedCaller("startActivityWithConfig");
        int ret = this.mActivityStarter.startActivityMayWait(caller, -1, callingPackage, intent, resolvedType, null, null, resultTo, resultWho, requestCode, startFlags, null, null, config, bOptions, false, this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startActivityWithConfig", null), null, null);
        return ret;
    }

    public int startActivityIntentSender(IApplicationThread caller, IntentSender intent, Intent fillInIntent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle bOptions) throws TransactionTooLargeException {
        enforceNotIsolatedCaller("startActivityIntentSender");
        if (fillInIntent != null && fillInIntent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        PendingIntentRecord target = intent.getTarget();
        if (!(target instanceof PendingIntentRecord)) {
            throw new IllegalArgumentException("Bad PendingIntent object");
        }
        PendingIntentRecord pir = target;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = getFocusedStack();
                if (stack.mResumedActivity != null && stack.mResumedActivity.info.applicationInfo.uid == Binder.getCallingUid()) {
                    this.mAppSwitchesAllowedTime = 0L;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        int ret = pir.sendInner(0, fillInIntent, resolvedType, null, null, resultTo, resultWho, requestCode, flagsMask, flagsValues, bOptions, null);
        return ret;
    }

    public int startVoiceActivity(String callingPackage, int callingPid, int callingUid, Intent intent, String resolvedType, IVoiceInteractionSession session, IVoiceInteractor interactor, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
        if (checkCallingPermission("android.permission.BIND_VOICE_INTERACTION") != 0) {
            String msg = "Permission Denial: startVoiceActivity() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.BIND_VOICE_INTERACTION";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        if (session == null || interactor == null) {
            throw new NullPointerException("null session or interactor");
        }
        return this.mActivityStarter.startActivityMayWait(null, callingUid, callingPackage, intent, resolvedType, session, interactor, null, null, 0, startFlags, profilerInfo, null, null, bOptions, false, this.mUserController.handleIncomingUser(callingPid, callingUid, userId, false, 2, "startVoiceActivity", null), null, null);
    }

    public void startLocalVoiceInteraction(IBinder callingActivity, Bundle options) throws RemoteException {
        Slog.i(TAG, "Activity tried to startVoiceInteraction");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord activity = getFocusedStack().topActivity();
                if (ActivityRecord.forTokenLocked(callingActivity) != activity) {
                    throw new SecurityException("Only focused activity can call startVoiceInteraction");
                }
                if (this.mRunningVoice != null || activity.task.voiceSession != null || activity.voiceSession != null) {
                    Slog.w(TAG, "Already in a voice interaction, cannot start new voice interaction");
                } else {
                    if (activity.pendingVoiceInteractionStart) {
                        Slog.w(TAG, "Pending start of voice interaction already.");
                        return;
                    }
                    activity.pendingVoiceInteractionStart = true;
                    resetPriorityAfterLockedSection();
                    ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).startLocalVoiceInteraction(callingActivity, options);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void stopLocalVoiceInteraction(IBinder callingActivity) throws RemoteException {
        ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).stopLocalVoiceInteraction(callingActivity);
    }

    public boolean supportsLocalVoiceInteraction() throws RemoteException {
        return ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).supportsLocalVoiceInteraction();
    }

    void onLocalVoiceInteractionStartedLocked(IBinder activity, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        ActivityRecord activityToCallback = ActivityRecord.forTokenLocked(activity);
        if (activityToCallback == null) {
            return;
        }
        activityToCallback.setVoiceSessionLocked(voiceSession);
        try {
            activityToCallback.app.thread.scheduleLocalVoiceInteractionStarted(activity, voiceInteractor);
            long token = Binder.clearCallingIdentity();
            try {
                startRunningVoiceLocked(voiceSession, activityToCallback.appInfo.uid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } catch (RemoteException e) {
            activityToCallback.clearVoiceSessionLocked();
        }
    }

    public void setVoiceKeepAwake(IVoiceInteractionSession session, boolean keepAwake) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (this.mRunningVoice != null && this.mRunningVoice.asBinder() == session.asBinder()) {
                    if (keepAwake) {
                        this.mVoiceWakeLock.acquire();
                    } else {
                        this.mVoiceWakeLock.release();
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean startNextMatchingActivity(IBinder callingActivity, Intent intent, Bundle bOptions) throws Throwable {
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        ActivityOptions options = ActivityOptions.fromBundle(bOptions);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(callingActivity);
                if (r == null) {
                    ActivityOptions.abort(options);
                    resetPriorityAfterLockedSection();
                    return false;
                }
                if (r.app == null || r.app.thread == null) {
                    ActivityOptions.abort(options);
                    resetPriorityAfterLockedSection();
                    return false;
                }
                Intent intent2 = new Intent(intent);
                try {
                    intent2.setDataAndType(r.intent.getData(), r.intent.getType());
                    intent2.setComponent(null);
                    boolean debug = (intent2.getFlags() & 8) != 0;
                    ActivityInfo aInfo = null;
                    try {
                        List<ResolveInfo> resolves = AppGlobals.getPackageManager().queryIntentActivities(intent2, r.resolvedType, 66560, UserHandle.getCallingUserId()).getList();
                        int N = resolves != null ? resolves.size() : 0;
                        int i = 0;
                        while (true) {
                            if (i >= N) {
                                break;
                            }
                            ResolveInfo rInfo = resolves.get(i);
                            if (rInfo.activityInfo.packageName.equals(r.packageName) && rInfo.activityInfo.name.equals(r.info.name)) {
                                break;
                            }
                            i++;
                        }
                    } catch (RemoteException e) {
                    }
                    if (aInfo == null) {
                        ActivityOptions.abort(options);
                        if (debug) {
                            Slog.d(TAG, "Next matching activity: nothing found");
                        }
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    intent2.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
                    intent2.setFlags(intent2.getFlags() & (-503316481));
                    boolean wasFinishing = r.finishing;
                    r.finishing = true;
                    ActivityRecord resultTo = r.resultTo;
                    String resultWho = r.resultWho;
                    int requestCode = r.requestCode;
                    r.resultTo = null;
                    if (resultTo != null) {
                        resultTo.removeResultsLocked(r, resultWho, requestCode);
                    }
                    long origId = Binder.clearCallingIdentity();
                    int res = this.mActivityStarter.startActivityLocked(r.app.thread, intent2, null, r.resolvedType, aInfo, null, null, null, resultTo != null ? resultTo.appToken : null, resultWho, requestCode, -1, r.launchedFromUid, r.launchedFromPackage, -1, r.launchedFromUid, 0, options, false, false, null, null, null);
                    Binder.restoreCallingIdentity(origId);
                    r.finishing = wasFinishing;
                    if (res != 0) {
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    resetPriorityAfterLockedSection();
                    return true;
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public final int startActivityFromRecents(int taskId, Bundle bOptions) {
        int iStartActivityFromRecentsInner;
        if (checkCallingPermission("android.permission.START_TASKS_FROM_RECENTS") != 0) {
            Slog.w(TAG, "Permission Denial: startActivityFromRecents called without android.permission.START_TASKS_FROM_RECENTS");
            throw new SecurityException("Permission Denial: startActivityFromRecents called without android.permission.START_TASKS_FROM_RECENTS");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    iStartActivityFromRecentsInner = this.mStackSupervisor.startActivityFromRecentsInner(taskId, bOptions);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return iStartActivityFromRecentsInner;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    final int startActivityInPackage(int uid, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, Bundle bOptions, int userId, IActivityContainer container, TaskRecord inTask) {
        int ret = this.mActivityStarter.startActivityMayWait(null, uid, callingPackage, intent, resolvedType, null, null, resultTo, resultWho, requestCode, startFlags, null, null, null, bOptions, false, this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startActivityInPackage", null), container, inTask);
        return ret;
    }

    public final int startActivities(IApplicationThread caller, String callingPackage, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle bOptions, int userId) {
        enforceNotIsolatedCaller("startActivities");
        int ret = this.mActivityStarter.startActivities(caller, -1, callingPackage, intents, resolvedTypes, resultTo, bOptions, this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startActivity", null));
        return ret;
    }

    final int startActivitiesInPackage(int uid, String callingPackage, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle bOptions, int userId) {
        int ret = this.mActivityStarter.startActivities(null, uid, callingPackage, intents, resolvedTypes, resultTo, bOptions, this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startActivityInPackage", null));
        return ret;
    }

    public void reportActivityFullyDrawn(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                } else {
                    r.reportFullyDrawnLocked();
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setRequestedOrientation(IBinder token, int requestedOrientation) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                TaskRecord task = r.task;
                if (task != null && (!task.mFullscreen || !task.stack.mFullscreen)) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                long origId = Binder.clearCallingIdentity();
                this.mWindowManager.setAppOrientation(r.appToken, requestedOrientation);
                Configuration config = this.mWindowManager.updateOrientationFromAppTokens(this.mConfiguration, r.mayFreezeScreenLocked(r.app) ? r.appToken : null);
                if (config != null) {
                    r.frozenBeforeDestroy = true;
                    if (!updateConfigurationLocked(config, r, false)) {
                        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                    }
                }
                Binder.restoreCallingIdentity(origId);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getRequestedOrientation(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                int appOrientation = this.mWindowManager.getAppOrientation(r.appToken);
                resetPriorityAfterLockedSection();
                return appOrientation;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public final boolean finishActivity(IBinder token, int resultCode, Intent resultData, int finishTask) {
        boolean res;
        ActivityRecord next;
        if (resultData != null && resultData.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.d(TAG, "ACT-Finishing activity token=" + token + ", result=" + resultCode + ", data=" + resultData + ", fTask=" + finishTask);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return true;
                }
                TaskRecord tr = r.task;
                ActivityRecord rootR = tr.getRootActivity();
                if (rootR == null) {
                    Slog.w(TAG, "Finishing task with all activities already finished");
                }
                if (tr.mLockTaskAuth != 4 && rootR == r && this.mStackSupervisor.isLastLockedTask(tr)) {
                    Slog.i(TAG, "Not finishing task in lock task mode");
                    this.mStackSupervisor.showLockTaskToast();
                    resetPriorityAfterLockedSection();
                    return false;
                }
                if (this.mController != null && (next = r.task.stack.topRunningActivityLocked(token, 0)) != null) {
                    boolean resumeOK = true;
                    try {
                        resumeOK = this.mController.activityResuming(next.packageName);
                    } catch (RemoteException e) {
                        this.mController = null;
                        Watchdog.getInstance().setActivityController(null);
                    }
                    if (!resumeOK) {
                        Slog.i(TAG, "Not finishing activity because controller resumed");
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                }
                long origId = Binder.clearCallingIdentity();
                boolean finishWithRootActivity = finishTask == 1;
                try {
                    if (finishTask == 2 || (finishWithRootActivity && r == rootR)) {
                        res = removeTaskByIdLocked(tr.taskId, false, finishWithRootActivity);
                        if (!res) {
                            Slog.i(TAG, "Removing task failed to finish activity");
                        }
                    } else {
                        res = tr.stack.requestFinishActivityLocked(token, resultCode, resultData, "app-request", true);
                        if (!res) {
                            Slog.i(TAG, "Failed to finish by app-request");
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return res;
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public final void finishHeavyWeightApp() {
        if (checkCallingPermission("android.permission.FORCE_STOP_PACKAGES") != 0) {
            String msg = "Permission Denial: finishHeavyWeightApp() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.FORCE_STOP_PACKAGES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (this.mHeavyWeightProcess == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                ArrayList<ActivityRecord> activities = new ArrayList<>(this.mHeavyWeightProcess.activities);
                for (int i = 0; i < activities.size(); i++) {
                    ActivityRecord r = activities.get(i);
                    if (!r.finishing && r.isInStackLocked()) {
                        r.task.stack.finishActivityLocked(r, 0, null, "finish-heavy", true);
                    }
                }
                this.mHandler.sendMessage(this.mHandler.obtainMessage(25, this.mHeavyWeightProcess.userId, 0));
                this.mHeavyWeightProcess = null;
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void crashApplication(int uid, int initialPid, String packageName, String message) {
        if (checkCallingPermission("android.permission.FORCE_STOP_PACKAGES") != 0) {
            String msg = "Permission Denial: crashApplication() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.FORCE_STOP_PACKAGES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mAppErrors.scheduleAppCrashLocked(uid, initialPid, packageName, message);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public final void finishSubActivity(IBinder token, String resultWho, int requestCode) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r != null) {
                    r.task.stack.finishSubActivityLocked(r, resultWho, requestCode);
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean finishActivityAffinity(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r == null) {
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    TaskRecord task = r.task;
                    if (task.mLockTaskAuth != 4 && this.mStackSupervisor.isLastLockedTask(task) && task.getRootActivity() == r) {
                        this.mStackSupervisor.showLockTaskToast();
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean zFinishActivityAffinityLocked = task.stack.finishActivityAffinityLocked(r);
                    resetPriorityAfterLockedSection();
                    return zFinishActivityAffinityLocked;
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void finishVoiceTask(IVoiceInteractionSession session) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    this.mStackSupervisor.finishVoiceTask(session);
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean releaseActivityInstance(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r == null) {
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean zSafelyDestroyActivityLocked = r.task.stack.safelyDestroyActivityLocked(r, "app-req");
                    resetPriorityAfterLockedSection();
                    return zSafelyDestroyActivityLocked;
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void releaseSomeActivities(IApplicationThread appInt) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    ProcessRecord app = getRecordForAppLocked(appInt);
                    this.mStackSupervisor.releaseSomeActivitiesLocked(app, "low-mem");
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean willActivityBeVisible(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean zWillActivityBeVisibleLocked = stack.willActivityBeVisibleLocked(token);
                resetPriorityAfterLockedSection();
                return zWillActivityBeVisibleLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void overridePendingTransition(IBinder token, String packageName, int enterAnim, int exitAnim) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord self = ActivityRecord.isInStackLocked(token);
                if (self == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                long origId = Binder.clearCallingIdentity();
                if (self.state == ActivityStack.ActivityState.RESUMED || self.state == ActivityStack.ActivityState.PAUSING) {
                    this.mWindowManager.overridePendingAppTransition(packageName, enterAnim, exitAnim, null);
                }
                Binder.restoreCallingIdentity(origId);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private final void handleAppDiedLocked(ProcessRecord app, boolean restarting, boolean allowRestart) {
        Slog.d(TAG, "handleAppDiedLocked: app = " + app + ", app.pid = " + app.pid);
        int pid = app.pid;
        boolean kept = cleanUpApplicationRecordLocked(app, restarting, allowRestart, -1, false);
        if (!kept && !restarting) {
            removeLruProcessLocked(app);
            if (pid > 0) {
                ProcessList.remove(pid);
            }
        }
        if (this.mProfileProc == app) {
            clearProfilerLocked();
        }
        boolean hasVisibleActivities = this.mStackSupervisor.handleAppDiedLocked(app);
        app.activities.clear();
        if (app.instrumentationClass != null) {
            Slog.w(TAG, "Crash of app " + app.processName + " running instrumentation " + app.instrumentationClass);
            Bundle info = new Bundle();
            info.putString("shortMsg", "Process crashed.");
            finishInstrumentationLocked(app, 0, info);
        }
        if (restarting || !hasVisibleActivities || this.mStackSupervisor.resumeFocusedStackTopActivityLocked()) {
            return;
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    private final int getLRURecordIndexForAppLocked(IApplicationThread thread) {
        IBinder threadBinder = thread.asBinder();
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord rec = this.mLruProcesses.get(i);
            if (rec.thread != null && rec.thread.asBinder() == threadBinder) {
                return i;
            }
        }
        return -1;
    }

    final ProcessRecord getRecordForAppLocked(IApplicationThread thread) {
        int appIndex;
        if (thread != null && (appIndex = getLRURecordIndexForAppLocked(thread)) >= 0) {
            return this.mLruProcesses.get(appIndex);
        }
        return null;
    }

    final void doLowMemReportIfNeededLocked(ProcessRecord dyingProc) {
        boolean haveBg = false;
        int i = this.mLruProcesses.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            ProcessRecord rec = this.mLruProcesses.get(i);
            if (rec.thread != null && rec.setProcState >= 14) {
                haveBg = true;
                break;
            }
            i--;
        }
        if (haveBg) {
            return;
        }
        boolean doReport = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
        if (doReport) {
            long now = SystemClock.uptimeMillis();
            if (now < this.mLastMemUsageReportTime + 300000) {
                doReport = false;
            } else {
                this.mLastMemUsageReportTime = now;
            }
        }
        ArrayList arrayList = doReport ? new ArrayList(this.mLruProcesses.size()) : null;
        EventLog.writeEvent(EventLogTags.AM_LOW_MEMORY, this.mLruProcesses.size());
        long now2 = SystemClock.uptimeMillis();
        for (int i2 = this.mLruProcesses.size() - 1; i2 >= 0; i2--) {
            ProcessRecord rec2 = this.mLruProcesses.get(i2);
            if (rec2 != dyingProc && rec2.thread != null) {
                if (doReport) {
                    arrayList.add(new ProcessMemInfo(rec2.processName, rec2.pid, rec2.setAdj, rec2.setProcState, rec2.adjType, rec2.makeAdjReason()));
                }
                if (rec2.lastLowMemory + 60000 <= now2) {
                    if (rec2.setAdj <= 400) {
                        rec2.lastRequestedGc = 0L;
                    } else {
                        rec2.lastRequestedGc = rec2.lastLowMemory;
                    }
                    rec2.reportLowMemory = true;
                    rec2.lastLowMemory = now2;
                    this.mProcessesToGc.remove(rec2);
                    addProcessToGcListLocked(rec2);
                }
            }
        }
        if (doReport) {
            Message msg = this.mHandler.obtainMessage(33, arrayList);
            this.mHandler.sendMessage(msg);
        }
        scheduleAppGcsLocked();
    }

    final void appDiedLocked(ProcessRecord app) {
        appDiedLocked(app, app.pid, app.thread, false);
    }

    final void appDiedLocked(ProcessRecord app, int pid, IApplicationThread thread, boolean fromBinderDied) {
        synchronized (this.mPidsSelfLocked) {
            ProcessRecord curProc = this.mPidsSelfLocked.get(pid);
            if (curProc != app) {
                Slog.w(TAG, "Spurious death for " + app + ", curProc for " + pid + ": " + curProc);
                return;
            }
            BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
            synchronized (stats) {
                stats.noteProcessDiedLocked(app.info.uid, pid);
            }
            if (!app.killed) {
                if (!fromBinderDied) {
                    Process.killProcessQuiet(pid);
                }
                killProcessGroup(app.uid, pid);
                app.killed = true;
            }
            if (app.pid != pid || app.thread == null || app.thread.asBinder() != thread.asBinder()) {
                if (app.pid != pid) {
                    Slog.i(TAG, "Process " + app.processName + " (pid " + pid + ") has died and restarted (pid " + app.pid + ").");
                    EventLog.writeEvent(EventLogTags.AM_PROC_DIED, Integer.valueOf(app.userId), Integer.valueOf(app.pid), app.processName);
                    return;
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                        Slog.d(TAG_PROCESSES, "Received spurious death notification for thread " + thread.asBinder());
                        return;
                    }
                    return;
                }
            }
            boolean doLowMem = app.instrumentationClass == null;
            boolean doOomAdj = doLowMem;
            if (app.killedByAm) {
                this.mAllowLowerMemLevel = false;
                doLowMem = false;
            } else {
                Slog.i(TAG, "Process " + app.processName + " (pid " + pid + ") has died");
                this.mAllowLowerMemLevel = true;
            }
            EventLog.writeEvent(EventLogTags.AM_PROC_DIED, Integer.valueOf(app.userId), Integer.valueOf(app.pid), app.processName);
            if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_CLEANUP, "Dying app: " + app + ", pid: " + pid + ", thread: " + thread.asBinder());
            }
            handleAppDiedLocked(app, false, true);
            if (doOomAdj) {
                updateOomAdjLocked();
            }
            if (doLowMem) {
                doLowMemReportIfNeededLocked(app);
            }
        }
    }

    public static File dumpStackTraces(boolean clearTraces, ArrayList<Integer> firstPids, ProcessCpuTracker processCpuTracker, SparseArray<Boolean> lastPids, String[] nativeProcs) {
        String tracesPath = SystemProperties.get("dalvik.vm.stack-trace-file", (String) null);
        if (tracesPath == null || tracesPath.length() == 0) {
            return null;
        }
        Slog.d(TAG, "dumpStackTraces Full Begin:");
        File dumptracesFile = null;
        ANRManager aNRManager = mANRManager;
        if (ANRManager.enableANRDebuggingMechanism() == 0) {
            File tracesFile = new File(tracesPath);
            dumptracesFile = tracesFile;
            try {
                File tracesDir = tracesFile.getParentFile();
                if (!tracesDir.exists()) {
                    tracesDir.mkdirs();
                    if (!SELinux.restorecon(tracesDir)) {
                        return null;
                    }
                }
                FileUtils.setPermissions(tracesDir.getPath(), 509, -1, -1);
                if (clearTraces && tracesFile.exists()) {
                    tracesFile.delete();
                }
                tracesFile.createNewFile();
                FileUtils.setPermissions(tracesFile.getPath(), 438, -1, -1);
            } catch (IOException e) {
                Slog.w(TAG, "Unable to prepare ANR traces file: " + tracesPath, e);
                return null;
            }
        }
        dumpStackTraces(tracesPath, firstPids, processCpuTracker, lastPids, nativeProcs);
        ANRManager aNRManager2 = mANRManager;
        if (ANRManager.enableANRDebuggingMechanism() != 0) {
            dumptracesFile = new File(tracesPath);
            mANRManager.delayRenameTraceFiles(300000);
        }
        Slog.d(TAG, "dumpStackTraces Full End:");
        Slog.d(TAG, "dumptracesFile = " + dumptracesFile);
        return dumptracesFile;
    }

    private static void dumpStackTraces(String tracesPath, ArrayList<Integer> firstPids, ProcessCpuTracker processCpuTracker, SparseArray<Boolean> lastPids, String[] nativeProcs) {
        int num;
        int i;
        int[] pids;
        int[] pids2;
        int i2;
        int timeout = 0;
        FileObserver observer = new FileObserver(tracesPath, 8) {
            @Override
            public synchronized void onEvent(int event, String path) {
                notify();
            }
        };
        try {
            observer.startWatching();
            if (firstPids != null) {
                try {
                    num = firstPids.size();
                    i = 0;
                } catch (InterruptedException e) {
                    Slog.wtf(TAG, e);
                }
                while (i < num) {
                    synchronized (observer) {
                        if (ActivityManagerDebugConfig.DEBUG_ANR) {
                            Slog.d(TAG, "Collecting stacks for pid " + firstPids.get(i));
                        }
                        long sime = SystemClock.elapsedRealtime();
                        ANRManager aNRManager = mANRManager;
                        if (ANRManager.enableANRDebuggingMechanism() != 0) {
                            int pid = firstPids.get(i).intValue();
                            if (mANRManager.isJavaProcess(pid)) {
                                Slog.i(TAG, "dumpStackTraces process: " + pid + " parent: " + Process.getParentPid(pid) + " zygote: " + Arrays.toString(ANRManager.mZygotePids));
                                Process.sendSignal(pid, 3);
                            } else if (!mInterestingPids.contains(Integer.valueOf(pid))) {
                                mInterestingPids.add(Integer.valueOf(pid));
                            }
                        } else {
                            Process.sendSignal(firstPids.get(i).intValue(), 3);
                        }
                        observer.wait(1000L);
                        if (ActivityManagerDebugConfig.DEBUG_ANR) {
                            Slog.d(TAG, "Done with pid " + firstPids.get(i) + " in " + (SystemClock.elapsedRealtime() - sime) + "ms");
                        }
                    }
                }
            }
            if (processCpuTracker != null) {
                processCpuTracker.init();
                System.gc();
                processCpuTracker.update();
                try {
                    synchronized (processCpuTracker) {
                        processCpuTracker.wait(500L);
                    }
                } catch (InterruptedException e2) {
                }
                processCpuTracker.update();
                int N = processCpuTracker.countWorkingStats();
                int numProcs = 0;
                for (i2 = 0; i2 < N && numProcs < 5; i2++) {
                    ProcessCpuTracker.Stats stats = processCpuTracker.getWorkingStats(i2);
                    ANRManager aNRManager2 = mANRManager;
                    if (ANRManager.enableANRDebuggingMechanism() != 0 && i2 < 3 && !mInterestingPids.contains(Integer.valueOf(stats.pid))) {
                        mInterestingPids.add(Integer.valueOf(stats.pid));
                    }
                    if (lastPids.indexOfKey(stats.pid) >= 0) {
                        numProcs++;
                        try {
                            synchronized (observer) {
                                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                    Slog.d(TAG, "Collecting stacks for extra pid " + stats.pid);
                                }
                                long stime = SystemClock.elapsedRealtime();
                                ANRManager aNRManager3 = mANRManager;
                                if (ANRManager.enableANRDebuggingMechanism() == 0) {
                                    Process.sendSignal(stats.pid, 3);
                                    observer.wait(1000L);
                                } else if (mANRManager.isJavaProcess(stats.pid)) {
                                    Slog.i(TAG, "dumpStackTraces stats process: " + stats.pid + " parent: " + Process.getParentPid(stats.pid) + " zygote: " + Arrays.toString(ANRManager.mZygotePids));
                                    Process.sendSignal(stats.pid, 3);
                                    observer.wait(1000L);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                    Slog.d(TAG, "Done with extra pid " + stats.pid + " in " + (SystemClock.elapsedRealtime() - stime) + "ms");
                                }
                            }
                        } catch (InterruptedException e3) {
                            Slog.wtf(TAG, e3);
                        }
                    } else if (ActivityManagerDebugConfig.DEBUG_ANR) {
                        Slog.d(TAG, "Skipping next CPU consuming process, not a java proc: " + stats.pid);
                    }
                }
            }
            observer.stopWatching();
            ANRManager aNRManager4 = mANRManager;
            if (2 != ANRManager.enableANRDebuggingMechanism()) {
                if (nativeProcs == null || (pids = Process.getPidsForCommands(nativeProcs)) == null) {
                    return;
                }
                for (int pid2 : pids) {
                    if (ActivityManagerDebugConfig.DEBUG_ANR) {
                        Slog.d(TAG, "Collecting stacks for native pid " + pid2);
                    }
                    long sime2 = SystemClock.elapsedRealtime();
                    Debug.dumpNativeBacktraceToFile(pid2, tracesPath);
                    if (ActivityManagerDebugConfig.DEBUG_ANR) {
                        Slog.d(TAG, "Done with native pid " + pid2 + " in " + (SystemClock.elapsedRealtime() - sime2) + "ms");
                    }
                }
                return;
            }
            Slog.d(TAG, "[DumpNative] dumpNativeBacktraceToFile begin:");
            if (nativeProcs != null && (pids2 = Process.getPidsForCommands(nativeProcs)) != null) {
                for (int pid3 : pids2) {
                    if (!mInterestingPids.contains(Integer.valueOf(pid3))) {
                        mInterestingPids.add(Integer.valueOf(pid3));
                    }
                }
            }
            int systemServerIndex = mInterestingPids.indexOf(Integer.valueOf(MY_PID));
            if (systemServerIndex != -1) {
                try {
                    mInterestingPids.remove(systemServerIndex);
                } catch (Exception e4) {
                    Slog.i(TAG, "[DumpNative] DumpThread Exception: " + e4);
                }
            }
            int pidListSize = mInterestingPids.size();
            int[] nativePidList1 = new int[pidListSize / 2];
            int[] nativePidList2 = new int[pidListSize - (pidListSize / 2)];
            for (int i3 = 0; i3 < pidListSize; i3++) {
                if (i3 < pidListSize / 2) {
                    nativePidList1[i3] = mInterestingPids.get(i3).intValue();
                } else {
                    nativePidList2[i3 - (pidListSize / 2)] = mInterestingPids.get(i3).intValue();
                }
            }
            mInterestingPids.clear();
            ANRManager aNRManager5 = mANRManager;
            aNRManager5.getClass();
            ANRManager.DumpThread dumpNativeThread1 = aNRManager5.new DumpThread(nativePidList1, "/data/anr/native1.txt");
            dumpNativeThread1.setName("dumpnativethread1");
            dumpNativeThread1.start();
            ANRManager aNRManager6 = mANRManager;
            aNRManager6.getClass();
            ANRManager.DumpThread dumpNativeThread2 = aNRManager6.new DumpThread(nativePidList2, "/data/anr/native2.txt");
            dumpNativeThread2.setName("dumpnativethread2");
            dumpNativeThread2.start();
            while (true) {
                if (dumpNativeThread1.mResult && dumpNativeThread2.mResult) {
                    break;
                }
                try {
                    Thread.sleep(200L);
                    timeout += FIRST_BROADCAST_QUEUE_MSG;
                    if (timeout > 15000) {
                        Slog.d(TAG, "[DumpNative] dumpNativeBacktraceToFile is over 15s");
                        break;
                    }
                    continue;
                } catch (InterruptedException e5) {
                }
            }
            Slog.d(TAG, "[DumpNative] dumpNativeBacktraceToFile end:");
        } catch (Throwable th) {
            observer.stopWatching();
            throw th;
        }
    }

    final void logAppTooSlow(ProcessRecord app, long startTime, String msg) {
    }

    final void showLaunchWarningLocked(final ActivityRecord cur, final ActivityRecord next) {
        if (this.mLaunchWarningShown) {
            return;
        }
        this.mLaunchWarningShown = true;
        this.mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (ActivityManagerService.this) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        final Dialog d = new LaunchWarningWindow(ActivityManagerService.this.mContext, cur, next);
                        d.show();
                        ActivityManagerService.this.mUiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (ActivityManagerService.this) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        d.dismiss();
                                        ActivityManagerService.this.mLaunchWarningShown = false;
                                    } catch (Throwable th) {
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                        }, 4000L);
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        });
    }

    public boolean clearApplicationUserData(String packageName, final IPackageDataObserver observer, int userId) {
        enforceNotIsolatedCaller("clearApplicationUserData");
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        final int userId2 = this.mUserController.handleIncomingUser(pid, uid, userId, false, 2, "clearApplicationUserData", null);
        long callingId = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            int pkgUid = -1;
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (getPackageManagerInternalLocked().canPackageBeWiped(userId2, packageName)) {
                        throw new SecurityException("Cannot clear data for a device owner or a profile owner");
                    }
                    try {
                        pkgUid = pm.getPackageUid(packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, userId2);
                    } catch (RemoteException e) {
                    }
                    if (pkgUid == -1) {
                        Slog.w(TAG, "Invalid packageName: " + packageName);
                        if (observer != null) {
                            try {
                                observer.onRemoveCompleted(packageName, false);
                            } catch (RemoteException e2) {
                                Slog.i(TAG, "Observer no longer exists.");
                            }
                        }
                        return false;
                    }
                    if (uid != pkgUid && checkComponentPermission("android.permission.CLEAR_APP_USER_DATA", pid, uid, -1, true) != 0) {
                        throw new SecurityException("PID " + pid + " does not have permission android.permission.CLEAR_APP_USER_DATA to clear data of package " + packageName);
                    }
                    forceStopPackageLocked(packageName, pkgUid, "clear data");
                    for (int i = this.mRecentTasks.size() - 1; i >= 0; i--) {
                        TaskRecord tr = this.mRecentTasks.get(i);
                        String taskPackageName = tr.getBaseIntent().getComponent().getPackageName();
                        if (tr.userId == userId2 && taskPackageName.equals(packageName)) {
                            removeTaskByIdLocked(tr.taskId, false, true);
                        }
                    }
                    resetPriorityAfterLockedSection();
                    final int pkgUidF = pkgUid;
                    try {
                        pm.clearApplicationUserData(packageName, new IPackageDataObserver.Stub() {
                            public void onRemoveCompleted(String packageName2, boolean succeeded) throws RemoteException {
                                synchronized (ActivityManagerService.this) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        ActivityManagerService.this.finishForceStopPackageLocked(packageName2, pkgUidF);
                                    } catch (Throwable th) {
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                Intent intent = new Intent("android.intent.action.PACKAGE_DATA_CLEARED", Uri.fromParts("package", packageName2, null));
                                intent.putExtra("android.intent.extra.UID", pkgUidF);
                                intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(pkgUidF));
                                ActivityManagerService.this.broadcastIntentInPackage("android", 1000, intent, null, null, 0, null, null, null, null, false, false, userId2);
                                if (observer == null) {
                                    return;
                                }
                                observer.onRemoveCompleted(packageName2, succeeded);
                            }
                        }, userId2);
                        synchronized (this) {
                            try {
                                boostPriorityForLockedSection();
                                removeUriPermissionsForPackageLocked(packageName, userId2, true);
                            } finally {
                                resetPriorityAfterLockedSection();
                            }
                        }
                        resetPriorityAfterLockedSection();
                        INotificationManager inm = NotificationManager.getService();
                        inm.removeAutomaticZenRules(packageName);
                        inm.setNotificationPolicyAccessGranted(packageName, false);
                    } catch (RemoteException e3) {
                    }
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                } finally {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void killBackgroundProcesses(String packageName, int userId) {
        if (checkCallingPermission("android.permission.KILL_BACKGROUND_PROCESSES") != 0 && checkCallingPermission("android.permission.RESTART_PACKAGES") != 0) {
            String msg = "Permission Denial: killBackgroundProcesses() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.KILL_BACKGROUND_PROCESSES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, 2, "killBackgroundProcesses", null);
        long callingId = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    int appId = -1;
                    try {
                        appId = UserHandle.getAppId(pm.getPackageUid(packageName, 268435456, userId2));
                    } catch (RemoteException e) {
                    }
                    if (appId == -1) {
                        Slog.w(TAG, "Invalid packageName: " + packageName);
                    } else {
                        killPackageProcessesLocked(packageName, appId, userId2, 500, false, true, true, false, "kill background");
                        resetPriorityAfterLockedSection();
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void killAllBackgroundProcesses() {
        if (checkCallingPermission("android.permission.KILL_BACKGROUND_PROCESSES") != 0) {
            String msg = "Permission Denial: killAllBackgroundProcesses() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.KILL_BACKGROUND_PROCESSES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ArrayList<ProcessRecord> procs = new ArrayList<>();
                    int NP = this.mProcessNames.getMap().size();
                    for (int ip = 0; ip < NP; ip++) {
                        SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                        int NA = apps.size();
                        for (int ia = 0; ia < NA; ia++) {
                            ProcessRecord app = apps.valueAt(ia);
                            if (!app.persistent) {
                                if (app.removed) {
                                    procs.add(app);
                                } else if (app.setAdj >= 900) {
                                    app.removed = true;
                                    procs.add(app);
                                }
                            }
                        }
                    }
                    int N = procs.size();
                    for (int i = 0; i < N; i++) {
                        removeProcessLocked(procs.get(i), false, true, "kill all background");
                    }
                    this.mAllowLowerMemLevel = true;
                    updateOomAdjLocked();
                    doLowMemReportIfNeededLocked(null);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void killAllBackgroundProcessesExcept(int minTargetSdk, int maxProcState) {
        if (checkCallingPermission("android.permission.KILL_BACKGROUND_PROCESSES") != 0) {
            String msg = "Permission Denial: killAllBackgroundProcessesExcept() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.KILL_BACKGROUND_PROCESSES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ArrayList<ProcessRecord> procs = new ArrayList<>();
                    int NP = this.mProcessNames.getMap().size();
                    for (int ip = 0; ip < NP; ip++) {
                        SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                        int NA = apps.size();
                        for (int ia = 0; ia < NA; ia++) {
                            ProcessRecord app = apps.valueAt(ia);
                            if (app.removed) {
                                procs.add(app);
                            } else if ((minTargetSdk < 0 || app.info.targetSdkVersion < minTargetSdk) && (maxProcState < 0 || app.setProcState > maxProcState)) {
                                app.removed = true;
                                procs.add(app);
                            }
                        }
                    }
                    int N = procs.size();
                    for (int i = 0; i < N; i++) {
                        removeProcessLocked(procs.get(i), false, true, "kill all background except");
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void forceStopPackage(String packageName, int userId) {
        if (checkCallingPermission("android.permission.FORCE_STOP_PACKAGES") != 0) {
            String msg = "Permission Denial: forceStopPackage() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.FORCE_STOP_PACKAGES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        int callingPid = Binder.getCallingPid();
        int userId2 = this.mUserController.handleIncomingUser(callingPid, Binder.getCallingUid(), userId, true, 2, "forceStopPackage", null);
        long callingId = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    int[] users = userId2 == -1 ? this.mUserController.getUsers() : new int[]{userId2};
                    for (int user : users) {
                        int pkgUid = -1;
                        try {
                            pkgUid = pm.getPackageUid(packageName, 268435456, user);
                        } catch (RemoteException e) {
                        }
                        if (pkgUid == -1) {
                            Slog.w(TAG, "Invalid packageName: " + packageName);
                        } else {
                            try {
                                pm.setPackageStoppedState(packageName, true, user);
                            } catch (RemoteException e2) {
                            } catch (IllegalArgumentException e3) {
                                Slog.w(TAG, "Failed trying to unstop package " + packageName + ": " + e3);
                            }
                            if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                                AMEventHookData.PackageStoppedStatusChanged eventData = AMEventHookData.PackageStoppedStatusChanged.createInstance();
                                eventData.set(new Object[]{packageName, 1, "forceStopPackage"});
                                this.mAMEventHook.hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData);
                            }
                            if (this.mUserController.isUserRunningLocked(user, 0)) {
                                forceStopPackageLocked(packageName, pkgUid, "from pid " + callingPid);
                                finishForceStopPackageLocked(packageName, pkgUid);
                            }
                        }
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void addPackageDependency(String packageName) {
        ProcessRecord proc;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int callingPid = Binder.getCallingPid();
                if (callingPid == Process.myPid()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                synchronized (this.mPidsSelfLocked) {
                    proc = this.mPidsSelfLocked.get(Binder.getCallingPid());
                }
                if (proc != null) {
                    if (proc.pkgDeps == null) {
                        proc.pkgDeps = new ArraySet<>(1);
                    }
                    proc.pkgDeps.add(packageName);
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void killApplication(String pkg, int appId, int userId, String reason) {
        if (pkg == null) {
            return;
        }
        if (appId < 0) {
            Slog.w(TAG, "Invalid appid specified for pkg : " + pkg);
            return;
        }
        int callerUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callerUid) == 1000) {
            Message msg = this.mHandler.obtainMessage(22);
            msg.arg1 = appId;
            msg.arg2 = userId;
            Bundle bundle = new Bundle();
            bundle.putString("pkg", pkg);
            bundle.putString(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, reason);
            msg.obj = bundle;
            this.mHandler.sendMessage(msg);
            return;
        }
        throw new SecurityException(callerUid + " cannot kill pkg: " + pkg);
    }

    public void closeSystemDialogs(String reason) {
        ProcessRecord proc;
        enforceNotIsolatedCaller("closeSystemDialogs");
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (uid >= 10000) {
                        synchronized (this.mPidsSelfLocked) {
                            proc = this.mPidsSelfLocked.get(pid);
                        }
                        if (proc.curRawAdj > FIRST_BROADCAST_QUEUE_MSG) {
                            Slog.w(TAG, "Ignoring closeSystemDialogs " + reason + " from background process " + proc);
                            return;
                        }
                    }
                    closeSystemDialogsLocked(reason);
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void closeSystemDialogsLocked(String reason) {
        Intent intent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intent.addFlags(1342177280);
        if (reason != null) {
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, reason);
        }
        this.mWindowManager.closeSystemDialogs(reason);
        this.mStackSupervisor.closeSystemDialogsLocked();
        broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, -1, 1000, -1);
    }

    public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids) {
        ProcessRecord proc;
        int oomAdj;
        enforceNotIsolatedCaller("getProcessMemoryInfo");
        Debug.MemoryInfo[] infos = new Debug.MemoryInfo[pids.length];
        for (int i = pids.length - 1; i >= 0; i--) {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    synchronized (this.mPidsSelfLocked) {
                        proc = this.mPidsSelfLocked.get(pids[i]);
                        oomAdj = proc != null ? proc.setAdj : 0;
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            infos[i] = new Debug.MemoryInfo();
            Debug.getMemoryInfo(pids[i], infos[i]);
            if (proc != null) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (proc.thread != null && proc.setAdj == oomAdj) {
                            proc.baseProcessTracker.addPss(infos[i].getTotalPss(), infos[i].getTotalUss(), false, proc.pkgList);
                        }
                    } catch (Throwable th2) {
                        resetPriorityAfterLockedSection();
                        throw th2;
                    }
                }
                resetPriorityAfterLockedSection();
            }
        }
        return infos;
    }

    public long[] getProcessPss(int[] pids) {
        ProcessRecord proc;
        int oomAdj;
        enforceNotIsolatedCaller("getProcessPss");
        long[] pss = new long[pids.length];
        for (int i = pids.length - 1; i >= 0; i--) {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    synchronized (this.mPidsSelfLocked) {
                        proc = this.mPidsSelfLocked.get(pids[i]);
                        oomAdj = proc != null ? proc.setAdj : 0;
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            long[] tmpUss = new long[1];
            pss[i] = Debug.getPss(pids[i], tmpUss, null);
            if (proc != null) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (proc.thread != null && proc.setAdj == oomAdj) {
                            proc.baseProcessTracker.addPss(pss[i], tmpUss[0], false, proc.pkgList);
                        }
                    } catch (Throwable th2) {
                        resetPriorityAfterLockedSection();
                        throw th2;
                    }
                }
                resetPriorityAfterLockedSection();
            }
        }
        return pss;
    }

    public long[] getProcessPswap(int[] pids) throws RemoteException {
        enforceNotIsolatedCaller("getProcessPswap");
        long[] pss = new long[pids.length];
        for (int i = pids.length - 1; i >= 0; i--) {
            pss[i] = Debug.getPswap(pids[i]);
        }
        return pss;
    }

    public void killApplicationProcess(String processName, int uid) {
        if (processName == null) {
            return;
        }
        int callerUid = Binder.getCallingUid();
        if (callerUid == 1000) {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ProcessRecord app = getProcessRecordLocked(processName, uid, true);
                    if (app != null && app.thread != null) {
                        try {
                            app.thread.scheduleSuicide();
                        } catch (RemoteException e) {
                        }
                    } else {
                        Slog.w(TAG, "Process/uid not found attempting kill of " + processName + " / " + uid);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            return;
        }
        throw new SecurityException(callerUid + " cannot kill app process: " + processName);
    }

    private void forceStopPackageLocked(String packageName, int uid, String reason) {
        forceStopPackageLocked(packageName, UserHandle.getAppId(uid), false, false, true, false, false, UserHandle.getUserId(uid), reason);
    }

    private void finishForceStopPackageLocked(String packageName, int uid) {
        Intent intent = new Intent("android.intent.action.PACKAGE_RESTARTED", Uri.fromParts("package", packageName, null));
        if (!this.mProcessesReady) {
            intent.addFlags(1342177280);
        }
        intent.putExtra("android.intent.extra.UID", uid);
        intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(uid));
        broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, MY_PID, 1000, UserHandle.getUserId(uid));
    }

    private final boolean killPackageProcessesLocked(String packageName, int appId, int userId, int minOomAdj, boolean callerWillRestart, boolean allowRestart, boolean doit, boolean evenPersistent, String reason) {
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        int NP = this.mProcessNames.getMap().size();
        if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.d(TAG, "ACT-killPackageProcessesLocked NP=" + NP + " name=" + packageName);
        }
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
            int NA = apps.size();
            if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.d(TAG, "ACT-killPackageProcessesLocked NA=" + NA);
            }
            for (int ia = 0; ia < NA; ia++) {
                ProcessRecord app = apps.valueAt(ia);
                if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.d(TAG, "ACT-killPackageProcessesLocked check process=" + app.toString());
                }
                if (!app.persistent || evenPersistent) {
                    if (app.removed) {
                        if (doit) {
                            procs.add(app);
                        }
                        if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                            Slog.d(TAG, "ACT-killPackageProcessesLocked ignore removed process " + app.processName);
                        }
                    } else if (app.setAdj < minOomAdj) {
                        if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                            Slog.d(TAG, "ACT-killPackageProcessesLocked ignore process " + app.processName + " setAdj=" + app.setAdj + " minOomAdj=" + minOomAdj);
                        }
                    } else if (packageName != null) {
                        boolean isDep = app.pkgDeps != null ? app.pkgDeps.contains(packageName) : false;
                        if (isDep || UserHandle.getAppId(app.uid) == appId) {
                            if (userId == -1 || app.userId == userId) {
                                if (!app.pkgList.containsKey(packageName) && !isDep) {
                                    if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                                        Slog.d(TAG, "ACT-killPackageProcessesLocked ignore, pkgList didn't contain " + packageName + " isDep=" + isDep + "pkgDeps=" + app.pkgDeps);
                                    }
                                }
                            } else if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                                Slog.d(TAG, "ACT-killPackageProcessesLocked ignore app.userId=" + app.userId + " userId=" + userId);
                            }
                        } else if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                            Slog.d(TAG, "ACT-killPackageProcessesLocked ignore getAppId=" + UserHandle.getAppId(app.uid) + " appId=" + appId + " isDep=" + isDep + "pkgDeps=" + app.pkgDeps);
                        }
                    } else if (userId == -1 || app.userId == userId) {
                        if (appId >= 0 && UserHandle.getAppId(app.uid) != appId) {
                            boolean isChooserProcess = isChooserProcessFromUid(app, UserHandle.getUid(userId, appId));
                            if (isChooserProcess) {
                                Slog.d(TAG, "ACT-killPackageProcessesLocked isChooserProcessFromUid userId=" + userId + " appId=" + appId + " process=" + app.toString());
                                if (doit) {
                                }
                            } else if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                                Slog.d(TAG, "ACT-killPackageProcessesLocked ignore getAppId=" + UserHandle.getAppId(app.uid) + " appId=" + appId);
                            }
                        } else {
                            if (doit) {
                                return true;
                            }
                            app.removed = true;
                            procs.add(app);
                            if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                                Slog.d(TAG, "ACT-killPackageProcessesLocked procs add " + app.processName + " to kill list");
                            }
                        }
                    } else if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                        Slog.d(TAG, "ACT-killPackageProcessesLocked ignore app.userId=" + app.userId + " userId=" + userId);
                    }
                } else if (IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.d(TAG, "ACT-killPackageProcessesLocked ignore persistent process " + app.persistent + " " + evenPersistent);
                }
            }
        }
        int N = procs.size();
        for (int i = 0; i < N; i++) {
            removeProcessLocked(procs.get(i), callerWillRestart, allowRestart, reason);
        }
        updateOomAdjLocked();
        return N > 0;
    }

    private void cleanupDisabledPackageComponentsLocked(String packageName, int userId, boolean killProcess, String[] changedClasses) {
        Set<String> disabledClasses = null;
        boolean packageDisabled = false;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (changedClasses == null) {
            return;
        }
        int i = changedClasses.length - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            String changedClass = changedClasses[i];
            if (changedClass.equals(packageName)) {
                try {
                    int enabled = pm.getApplicationEnabledSetting(packageName, userId != -1 ? userId : 0);
                    packageDisabled = (enabled == 1 || enabled == 0) ? false : true;
                    if (packageDisabled) {
                        disabledClasses = null;
                        break;
                    }
                } catch (Exception e) {
                    return;
                }
            } else {
                try {
                    int enabled2 = pm.getComponentEnabledSetting(new ComponentName(packageName, changedClass), userId != -1 ? userId : 0);
                    if (enabled2 != 1 && enabled2 != 0) {
                        if (disabledClasses == null) {
                            disabledClasses = new ArraySet<>(changedClasses.length);
                        }
                        disabledClasses.add(changedClass);
                    }
                } catch (Exception e2) {
                    return;
                }
            }
            i--;
        }
        if (!packageDisabled && disabledClasses == null) {
            return;
        }
        if (this.mStackSupervisor.finishDisabledPackageActivitiesLocked(packageName, disabledClasses, true, false, userId) && this.mBooted) {
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            this.mStackSupervisor.scheduleIdleLocked();
        }
        cleanupDisabledPackageTasksLocked(packageName, disabledClasses, userId);
        this.mServices.bringDownDisabledPackageServicesLocked(packageName, disabledClasses, userId, false, killProcess, true);
        ArrayList<ContentProviderRecord> providers = new ArrayList<>();
        this.mProviderMap.collectPackageProvidersLocked(packageName, disabledClasses, true, false, userId, providers);
        for (int i2 = providers.size() - 1; i2 >= 0; i2--) {
            removeDyingProviderLocked(null, providers.get(i2), true);
        }
        for (int i3 = this.mBroadcastQueues.length - 1; i3 >= 0; i3--) {
            this.mBroadcastQueues[i3].cleanupDisabledPackageReceiversLocked(packageName, disabledClasses, userId, true);
        }
    }

    final boolean clearBroadcastQueueForUserLocked(int userId) {
        boolean didSomething = false;
        for (int i = this.mBroadcastQueues.length - 1; i >= 0; i--) {
            didSomething |= this.mBroadcastQueues[i].cleanupDisabledPackageReceiversLocked(null, null, userId, true);
        }
        return didSomething;
    }

    final boolean forceStopPackageLocked(String packageName, int appId, boolean callerWillRestart, boolean purgeCache, boolean doit, boolean evenPersistent, boolean uninstalling, int userId, String reason) {
        AttributeCache ac;
        if (userId == -1 && packageName == null) {
            Slog.w(TAG, "Can't force stop all processes of all users, that is insane!");
        }
        if (appId < 0 && packageName != null) {
            try {
                appId = UserHandle.getAppId(AppGlobals.getPackageManager().getPackageUid(packageName, 268435456, 0));
            } catch (RemoteException e) {
            }
        }
        if (doit) {
            if (packageName != null) {
                Slog.i(TAG, "Force stopping " + packageName + " appid=" + appId + " user=" + userId + ": " + reason);
            } else {
                Slog.i(TAG, "Force stopping u" + userId + ": " + reason);
            }
            this.mAppErrors.resetProcessCrashTimeLocked(packageName == null, appId, userId);
        }
        boolean didSomething = killPackageProcessesLocked(packageName, appId, userId, -10000, callerWillRestart, true, doit, evenPersistent, packageName == null ? "stop user " + userId : "stop " + packageName);
        if (this.mStackSupervisor.finishDisabledPackageActivitiesLocked(packageName, null, doit, evenPersistent, userId)) {
            if (!doit) {
                return true;
            }
            didSomething = true;
        }
        if (this.mServices.bringDownDisabledPackageServicesLocked(packageName, null, userId, evenPersistent, true, doit)) {
            if (!doit) {
                return true;
            }
            didSomething = true;
        }
        if (packageName == null) {
            this.mStickyBroadcasts.remove(userId);
        }
        ArrayList<ContentProviderRecord> providers = new ArrayList<>();
        if (this.mProviderMap.collectPackageProvidersLocked(packageName, (Set<String>) null, doit, evenPersistent, userId, providers)) {
            if (!doit) {
                return true;
            }
            didSomething = true;
        }
        for (int i = providers.size() - 1; i >= 0; i--) {
            removeDyingProviderLocked(null, providers.get(i), true);
        }
        removeUriPermissionsForPackageLocked(packageName, userId, false);
        if (doit) {
            for (int i2 = this.mBroadcastQueues.length - 1; i2 >= 0; i2--) {
                didSomething |= this.mBroadcastQueues[i2].cleanupDisabledPackageReceiversLocked(packageName, null, userId, doit);
            }
        }
        if ((packageName == null || uninstalling) && this.mIntentSenderRecords.size() > 0) {
            Iterator<WeakReference<PendingIntentRecord>> it = this.mIntentSenderRecords.values().iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> wpir = it.next();
                if (wpir == null) {
                    it.remove();
                } else {
                    PendingIntentRecord pir = wpir.get();
                    if (pir == null) {
                        it.remove();
                    } else if (packageName == null) {
                        if (pir.key.userId != userId) {
                            continue;
                        } else {
                            if (doit) {
                                return true;
                            }
                            didSomething = true;
                            it.remove();
                            pir.canceled = true;
                            if (pir.key.activity != null && pir.key.activity.pendingResults != null) {
                                pir.key.activity.pendingResults.remove(pir.ref);
                            }
                        }
                    } else if (UserHandle.getAppId(pir.uid) == appId && (userId == -1 || pir.key.userId == userId)) {
                        if (!pir.key.packageName.equals(packageName)) {
                            continue;
                        } else if (doit) {
                        }
                    }
                }
            }
        }
        if (doit) {
            if (purgeCache && packageName != null && (ac = AttributeCache.instance()) != null) {
                ac.removePackage(packageName);
            }
            if (this.mBooted) {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                this.mStackSupervisor.scheduleIdleLocked();
            }
        }
        return didSomething;
    }

    private final ProcessRecord removeProcessNameLocked(String name, int uid) {
        ProcessRecord old = (ProcessRecord) this.mProcessNames.remove(name, uid);
        if (old != null) {
            UidRecord uidRecord = old.uidRecord;
            uidRecord.numProcs--;
            if (old.uidRecord.numProcs == 0) {
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(TAG_UID_OBSERVERS, "No more processes in " + old.uidRecord);
                }
                enqueueUidChangeLocked(old.uidRecord, -1, 1);
                this.mActiveUids.remove(uid);
                noteUidProcessState(uid, -1);
            }
            old.uidRecord = null;
        }
        this.mIsolatedProcesses.remove(uid);
        return old;
    }

    private final void addProcessNameLocked(ProcessRecord proc) {
        ProcessRecord old = removeProcessNameLocked(proc.processName, proc.uid);
        if (old == proc && proc.persistent) {
            Slog.w(TAG, "Re-adding persistent process " + proc);
        } else if (old != null) {
            Slog.w(TAG, "Already have existing proc " + old + " when adding " + proc);
        }
        UidRecord uidRec = this.mActiveUids.get(proc.uid);
        if (uidRec == null) {
            uidRec = new UidRecord(proc.uid);
            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                Slog.i(TAG_UID_OBSERVERS, "Creating new process uid: " + uidRec);
            }
            this.mActiveUids.put(proc.uid, uidRec);
            noteUidProcessState(uidRec.uid, uidRec.curProcState);
            enqueueUidChangeLocked(uidRec, -1, 4);
        }
        proc.uidRecord = uidRec;
        uidRec.numProcs++;
        this.mProcessNames.put(proc.processName, proc.uid, proc);
        if (!proc.isolated) {
            return;
        }
        this.mIsolatedProcesses.put(proc.uid, proc);
    }

    boolean removeProcessLocked(ProcessRecord app, boolean callerWillRestart, boolean allowRestart, String reason) {
        String name = app.processName;
        int uid = app.uid;
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.d(TAG_PROCESSES, "Force removing proc " + app.toShortString() + " (" + name + "/" + uid + ")");
        }
        ProcessRecord old = (ProcessRecord) this.mProcessNames.get(name, uid);
        if (old != app) {
            Slog.w(TAG, "Ignoring remove of inactive process: " + app);
            return false;
        }
        removeProcessNameLocked(name, uid);
        if (this.mHeavyWeightProcess == app) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(25, this.mHeavyWeightProcess.userId, 0));
            this.mHeavyWeightProcess = null;
        }
        boolean needRestart = false;
        if (app.pid > 0 && app.pid != MY_PID) {
            int pid = app.pid;
            synchronized (this.mPidsSelfLocked) {
                this.mPidsSelfLocked.remove(pid);
                this.mHandler.removeMessages(20, app);
            }
            this.mBatteryStatsService.noteProcessFinish(app.processName, app.info.uid);
            if (app.isolated) {
                this.mBatteryStatsService.removeIsolatedUid(app.uid, app.info.uid);
            }
            boolean willRestart = false;
            if (app.persistent && !app.isolated) {
                if (!callerWillRestart) {
                    willRestart = true;
                } else {
                    needRestart = true;
                }
            }
            app.kill(reason, true);
            handleAppDiedLocked(app, willRestart, allowRestart);
            if (willRestart) {
                removeLruProcessLocked(app);
                addAppLocked(app.info, false, null);
            }
        } else {
            this.mRemovedProcesses.add(app);
        }
        return needRestart;
    }

    private final void processContentProviderPublishTimedOutLocked(ProcessRecord app) {
        cleanupAppInLaunchingProvidersLocked(app, true);
        removeProcessLocked(app, false, true, "timeout publishing content providers");
    }

    private final void processStartTimedOutLocked(ProcessRecord app) {
        int pid = app.pid;
        boolean gone = false;
        synchronized (this.mPidsSelfLocked) {
            ProcessRecord knownApp = this.mPidsSelfLocked.get(pid);
            if (knownApp != null && knownApp.thread == null) {
                this.mPidsSelfLocked.remove(pid);
                gone = true;
            }
        }
        if (gone) {
            Slog.w(TAG, "Process " + app + " failed to attach");
            EventLog.writeEvent(EventLogTags.AM_PROCESS_START_TIMEOUT, Integer.valueOf(app.userId), Integer.valueOf(pid), Integer.valueOf(app.uid), app.processName);
            removeProcessNameLocked(app.processName, app.uid);
            if (this.mHeavyWeightProcess == app) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(25, this.mHeavyWeightProcess.userId, 0));
                this.mHeavyWeightProcess = null;
            }
            this.mBatteryStatsService.noteProcessFinish(app.processName, app.info.uid);
            if (app.isolated) {
                this.mBatteryStatsService.removeIsolatedUid(app.uid, app.info.uid);
            }
            cleanupAppInLaunchingProvidersLocked(app, true);
            this.mServices.processStartTimedOutLocked(app);
            app.kill("start timeout", true);
            removeLruProcessLocked(app);
            if (this.mBackupTarget != null && this.mBackupTarget.app.pid == pid) {
                Slog.w(TAG, "Unattached app died before backup, skipping");
                try {
                    IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    bm.agentDisconnected(app.info.packageName);
                } catch (RemoteException e) {
                }
            }
            if (!isPendingBroadcastProcessLocked(pid)) {
                return;
            }
            Slog.w(TAG, "Unattached app died before broadcast acknowledged, skipping");
            skipPendingBroadcastLocked(pid);
            return;
        }
        Slog.w(TAG, "Spurious process start timeout - pid not known for " + app);
    }

    private final boolean attachApplicationLocked(IApplicationThread thread, int pid) {
        ProcessRecord app;
        if (pid == MY_PID || pid < 0) {
            app = null;
        } else {
            synchronized (this.mPidsSelfLocked) {
                app = this.mPidsSelfLocked.get(pid);
            }
        }
        if (app == null) {
            Slog.w(TAG, "No pending application record for pid " + pid + " (IApplicationThread " + thread + "); dropping process");
            EventLog.writeEvent(EventLogTags.AM_DROP_PROCESS, pid);
            if (pid > 0 && pid != MY_PID) {
                Process.killProcessQuiet(pid);
                return false;
            }
            try {
                thread.scheduleExit();
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        if (app.thread != null) {
            handleAppDiedLocked(app, true, true);
        }
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Binding process pid " + pid + " to record " + app);
        }
        String processName = app.processName;
        try {
            AppDeathRecipient adr = new AppDeathRecipient(app, pid, thread);
            thread.asBinder().linkToDeath(adr, 0);
            app.deathRecipient = adr;
            EventLog.writeEvent(EventLogTags.AM_PROC_BOUND, Integer.valueOf(app.userId), Integer.valueOf(app.pid), app.processName);
            app.makeActive(thread, this.mProcessStats);
            app.verifiedAdj = -10000;
            app.setAdj = -10000;
            app.curAdj = -10000;
            app.setSchedGroup = 1;
            app.curSchedGroup = 1;
            app.forcingToForeground = null;
            updateProcessForegroundLocked(app, false, false);
            app.hasShownUi = false;
            app.debugging = false;
            app.cached = false;
            app.killedByAm = false;
            app.unlocked = StorageManager.isUserKeyUnlocked(app.userId);
            this.mHandler.removeMessages(20, app);
            boolean normalMode = !this.mProcessesReady ? isAllowedWhileBooting(app.info) : true;
            List<ProviderInfo> providers = normalMode ? generateApplicationProvidersLocked(app) : null;
            if (providers != null && checkAppInLaunchingProvidersLocked(app)) {
                Message msg = this.mHandler.obtainMessage(CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG);
                msg.obj = app;
                this.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            }
            if (!normalMode) {
                Slog.i(TAG, "Launching preboot mode app: " + app);
            }
            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                Slog.v(TAG, "New app record " + app + " thread=" + thread.asBinder() + " pid=" + pid);
            }
            int testMode = 0;
            try {
                if (this.mDebugApp != null && this.mDebugApp.equals(processName)) {
                    testMode = this.mWaitForDebugger ? 2 : 1;
                    app.debugging = true;
                    if (this.mDebugTransient) {
                        this.mDebugApp = this.mOrigDebugApp;
                        this.mWaitForDebugger = this.mOrigWaitForDebugger;
                    }
                }
                String profileFile = app.instrumentationProfileFile;
                ParcelFileDescriptor profileFd = null;
                int samplingInterval = 0;
                boolean profileAutoStop = false;
                if (this.mProfileApp != null && this.mProfileApp.equals(processName)) {
                    this.mProfileProc = app;
                    profileFile = this.mProfileFile;
                    profileFd = this.mProfileFd;
                    samplingInterval = this.mSamplingInterval;
                    profileAutoStop = this.mAutoStopProfiler;
                }
                boolean enableTrackAllocation = false;
                if (this.mTrackAllocationApp != null && this.mTrackAllocationApp.equals(processName)) {
                    enableTrackAllocation = true;
                    this.mTrackAllocationApp = null;
                }
                boolean isRestrictedBackupMode = false;
                if (this.mBackupTarget != null && this.mBackupAppName.equals(processName)) {
                    isRestrictedBackupMode = this.mBackupTarget.appInfo.uid >= 10000 ? this.mBackupTarget.backupMode == 2 || this.mBackupTarget.backupMode == 3 || this.mBackupTarget.backupMode == 1 : false;
                }
                if (app.instrumentationClass != null) {
                    notifyPackageUse(app.instrumentationClass.getPackageName(), 7);
                }
                if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Binding proc " + processName + " with config " + this.mConfiguration);
                }
                ApplicationInfo appInfo = app.instrumentationInfo != null ? app.instrumentationInfo : app.info;
                app.compat = compatibilityInfoForPackageLocked(appInfo);
                if (profileFd != null) {
                    profileFd = profileFd.dup();
                }
                thread.bindApplication(processName, appInfo, providers, app.instrumentationClass, profileFile == null ? null : new ProfilerInfo(profileFile, profileFd, samplingInterval, profileAutoStop), app.instrumentationArguments, app.instrumentationWatcher, app.instrumentationUiAutomationConnection, testMode, this.mBinderTransactionTrackingEnabled, enableTrackAllocation, isRestrictedBackupMode || !normalMode, app.persistent, new Configuration(this.mConfiguration), app.compat, getCommonServicesLocked(app.isolated), this.mCoreSettingsObserver.getCoreSettingsLocked());
                updateLruProcessLocked(app, false, null);
                long jUptimeMillis = SystemClock.uptimeMillis();
                app.lastLowMemory = jUptimeMillis;
                app.lastRequestedGc = jUptimeMillis;
                this.mPersistentStartingProcesses.remove(app);
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES && this.mProcessesOnHold.contains(app)) {
                    Slog.v(TAG_PROCESSES, "Attach application locked removing on hold: " + app);
                }
                this.mProcessesOnHold.remove(app);
                boolean badApp = false;
                boolean didSomething = false;
                if (normalMode) {
                    try {
                        if (this.mStackSupervisor.attachApplicationLocked(app)) {
                            didSomething = true;
                        }
                    } catch (Exception e2) {
                        Slog.wtf(TAG, "Exception thrown launching activities in " + app, e2);
                        badApp = true;
                    }
                }
                if (!badApp) {
                    try {
                        didSomething |= this.mServices.attachApplicationLocked(app, processName);
                    } catch (Exception e3) {
                        Slog.wtf(TAG, "Exception thrown starting services in " + app, e3);
                        badApp = true;
                    }
                }
                if (!badApp && isPendingBroadcastProcessLocked(pid)) {
                    try {
                        didSomething |= sendPendingBroadcastsLocked(app);
                    } catch (Exception e4) {
                        Slog.wtf(TAG, "Exception thrown dispatching broadcasts in " + app, e4);
                        badApp = true;
                    }
                }
                if (!badApp && this.mBackupTarget != null && this.mBackupTarget.appInfo.uid == app.uid) {
                    if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
                        Slog.v(TAG_BACKUP, "New app is backup target, launching agent for " + app);
                    }
                    notifyPackageUse(this.mBackupTarget.appInfo.packageName, 5);
                    try {
                        thread.scheduleCreateBackupAgent(this.mBackupTarget.appInfo, compatibilityInfoForPackageLocked(this.mBackupTarget.appInfo), this.mBackupTarget.backupMode);
                    } catch (Exception e5) {
                        Slog.wtf(TAG, "Exception thrown creating backup agent in " + app, e5);
                        badApp = true;
                    }
                }
                if (badApp) {
                    app.kill("error during init", true);
                    handleAppDiedLocked(app, false, true);
                    return false;
                }
                if (didSomething) {
                    return true;
                }
                updateOomAdjLocked();
                return true;
            } catch (Exception e6) {
                Slog.wtf(TAG, "Exception thrown during bind of " + app, e6);
                app.resetPackageList(this.mProcessStats);
                app.unlinkDeathRecipient();
                startProcessLocked(app, "bind fail", processName);
                return false;
            }
        } catch (RemoteException e7) {
            app.resetPackageList(this.mProcessStats);
            startProcessLocked(app, "link fail", processName);
            return false;
        }
    }

    public final void attachApplication(IApplicationThread thread) {
        if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.d(TAG, "ACT-attachApplication pid " + Binder.getCallingPid() + " to thread " + thread);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int callingPid = Binder.getCallingPid();
                long origId = Binder.clearCallingIdentity();
                attachApplicationLocked(thread, callingPid);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public final void activityIdle(IBinder token, Configuration config, boolean stopProfiling) {
        long origId = Binder.clearCallingIdentity();
        Intent idleIntent = null;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack != null) {
                    ActivityRecord r = this.mStackSupervisor.activityIdleInternalLocked(token, false, config);
                    if (stopProfiling && this.mProfileProc == r.app && this.mProfileFd != null) {
                        try {
                            this.mProfileFd.close();
                        } catch (IOException e) {
                        }
                        clearProfilerLocked();
                    }
                    ActivityRecord rFromStack = this.mStackSupervisor.isInAnyStackLocked(token);
                    if (rFromStack != null) {
                        idleIntent = r.intent;
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
        AMEventHookData.EndOfActivityIdle eventData = AMEventHookData.EndOfActivityIdle.createInstance();
        eventData.set(new Object[]{this.mContext, idleIntent});
        this.mAMEventHook.hook(AMEventHook.Event.AM_EndOfActivityIdle, eventData);
    }

    void postFinishBooting(boolean finishBooting, boolean enableScreen) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(FINISH_BOOTING_MSG, finishBooting ? 1 : 0, enableScreen ? 1 : 0));
    }

    void enableScreenAfterBoot() {
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_ENABLE_SCREEN, SystemClock.uptimeMillis());
        BootEvent.addBootEvent("AMS:ENABLE_SCREEN");
        this.mWindowManager.enableScreenAfterBoot();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                updateEventDispatchingLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void showBootMessage(CharSequence msg, boolean always) {
        if (Binder.getCallingUid() != Process.myUid()) {
            throw new SecurityException();
        }
        this.mWindowManager.showBootMessage(msg, always);
    }

    public void keyguardWaitingForActivityDrawn() {
        enforceNotIsolatedCaller("keyguardWaitingForActivityDrawn");
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (ActivityManagerDebugConfig.DEBUG_LOCKSCREEN) {
                        logLockScreen("");
                    }
                    this.mWindowManager.keyguardWaitingForActivityDrawn();
                    if (this.mLockScreenShown == 2) {
                        this.mLockScreenShown = 1;
                        updateSleepIfNeededLocked();
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void keyguardGoingAway(int flags) {
        enforceNotIsolatedCaller("keyguardGoingAway");
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (ActivityManagerDebugConfig.DEBUG_LOCKSCREEN) {
                        logLockScreen("");
                    }
                    this.mWindowManager.keyguardGoingAway(flags);
                    if (this.mLockScreenShown == 2) {
                        this.mLockScreenShown = 0;
                        updateSleepIfNeededLocked();
                        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                        applyVrModeIfNeededLocked(this.mFocusedActivity, true);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    final void finishBooting() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!this.mBootAnimationComplete) {
                    this.mCallFinishBooting = true;
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mCallFinishBooting = false;
                resetPriorityAfterLockedSection();
                ArraySet<String> completedIsas = new ArraySet<>();
                for (String abi : Build.SUPPORTED_ABIS) {
                    Process.establishZygoteConnectionForAbi(abi);
                    String instructionSet = VMRuntime.getInstructionSet(abi);
                    if (!completedIsas.contains(instructionSet)) {
                        try {
                            this.mInstaller.markBootComplete(VMRuntime.getInstructionSet(abi));
                        } catch (InstallerConnection.InstallerException e) {
                            Slog.w(TAG, "Unable to mark boot complete for abi: " + abi + " (" + e.getMessage() + ")");
                        }
                        completedIsas.add(instructionSet);
                    }
                }
                IntentFilter pkgFilter = new IntentFilter();
                pkgFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
                pkgFilter.addDataScheme("package");
                this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String[] pkgs = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                        if (pkgs != null) {
                            for (String pkg : pkgs) {
                                synchronized (ActivityManagerService.this) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        if (ActivityManagerService.this.forceStopPackageLocked(pkg, -1, false, false, false, false, false, 0, "query restart")) {
                                            setResultCode(-1);
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            return;
                                        }
                                    } catch (Throwable th) {
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                    }
                }, pkgFilter);
                IntentFilter dumpheapFilter = new IntentFilter();
                dumpheapFilter.addAction("com.android.server.am.DELETE_DUMPHEAP");
                this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getBooleanExtra("delay_delete", false)) {
                            ActivityManagerService.this.mHandler.sendEmptyMessageDelayed(51, 300000L);
                        } else {
                            ActivityManagerService.this.mHandler.sendEmptyMessage(51);
                        }
                    }
                }, dumpheapFilter);
                this.mSystemServiceManager.startBootPhase(1000);
                BootEvent.setEnabled(false);
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        int NP = this.mProcessesOnHold.size();
                        if (NP > 0) {
                            ArrayList<ProcessRecord> procs = new ArrayList<>(this.mProcessesOnHold);
                            for (int ip = 0; ip < NP; ip++) {
                                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                                    Slog.v(TAG_PROCESSES, "Starting process on hold: " + procs.get(ip));
                                }
                                startProcessLocked(procs.get(ip), "on-hold", null);
                            }
                        }
                        if (this.mFactoryTest != 1) {
                            Message nmsg = this.mHandler.obtainMessage(27);
                            this.mHandler.sendMessageDelayed(nmsg, POWER_CHECK_DELAY);
                            AMEventHookData.BeforeSendBootCompleted eventData = AMEventHookData.BeforeSendBootCompleted.createInstance();
                            AMEventHookResult eventResult = this.mAMEventHook.hook(AMEventHook.Event.AM_BeforeSendBootCompleted, eventData);
                            if (AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_Interrupt)) {
                                resetPriorityAfterLockedSection();
                                return;
                            }
                            Slog.v(TAG, "broadcast BOOT_COMPLETED intent");
                            SystemProperties.set("sys.boot_completed", "1");
                            if (!"trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt")) || "".equals(SystemProperties.get("vold.encrypt_progress"))) {
                                SystemProperties.set("dev.bootcomplete", "1");
                            }
                            this.mUserController.sendBootCompletedLocked(new IIntentReceiver.Stub() {
                                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                                    synchronized (ActivityManagerService.this) {
                                        try {
                                            ActivityManagerService.boostPriorityForLockedSection();
                                            ActivityManagerService.this.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
                                        } catch (Throwable th) {
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    }
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                }
                            });
                            scheduleStartProfilesLocked();
                        }
                        resetPriorityAfterLockedSection();
                        mANRManager.writeEvent(ANRManager.EVENT_BOOT_COMPLETED);
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
    }

    public void bootAnimationComplete() {
        boolean callFinishBooting;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.d(TAG, "bootAnimationComplete() mCallFinishBooting = " + this.mCallFinishBooting);
                }
                callFinishBooting = this.mCallFinishBooting;
                this.mBootAnimationComplete = true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (!callFinishBooting) {
            return;
        }
        Trace.traceBegin(64L, "FinishBooting");
        finishBooting();
        Trace.traceEnd(64L);
    }

    final void ensureBootCompleted() {
        boolean booting;
        boolean enableScreen;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                booting = this.mBooting;
                this.mBooting = false;
                enableScreen = !this.mBooted;
                this.mBooted = true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (booting) {
            Trace.traceBegin(64L, "FinishBooting");
            finishBooting();
            Trace.traceEnd(64L);
        }
        if (!enableScreen) {
            return;
        }
        enableScreenAfterBoot();
    }

    public final void activityResumed(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack != null) {
                    stack.activityResumedLocked(token);
                    if (SystemProperties.get("ro.mtk_aws_support").equals("1") && AWSManager.getInstance() != null) {
                        this.mBgHandler.sendEmptyMessage(2);
                        this.mBgHandler.sendEmptyMessageDelayed(2, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                    }
                    if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                        ActivityRecord r = ActivityRecord.forTokenLocked(token);
                        AMEventHookData.ActivityThreadResumedDone eventData = AMEventHookData.ActivityThreadResumedDone.createInstance();
                        eventData.set(new Object[]{r.info.packageName});
                        this.mAMEventHook.hook(AMEventHook.Event.AM_ActivityThreadResumedDone, eventData);
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public final void activityPaused(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack != null) {
                    stack.activityPausedLocked(token, false);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public final void activityStopped(IBinder token, Bundle icicle, PersistableBundle persistentState, CharSequence description) {
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Activity stopped: token=" + token);
        }
        if (icicle != null && icicle.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }
        long origId = Binder.clearCallingIdentity();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r != null) {
                    r.task.stack.activityStoppedLocked(r, icicle, persistentState, description);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        trimApplications();
        Binder.restoreCallingIdentity(origId);
    }

    public final void activityDestroyed(IBinder token) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "ACTIVITY DESTROYED: " + token);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack != null) {
                    stack.activityDestroyedLocked(token, "activityDestroyed");
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public final void activityRelaunched(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mStackSupervisor.activityRelaunchedLocked(token);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void reportSizeConfigurations(IBinder token, int[] horizontalSizeConfiguration, int[] verticalSizeConfigurations, int[] smallestSizeConfigurations) {
        if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG, "Report configuration: " + token + " " + horizontalSizeConfiguration + " " + verticalSizeConfigurations);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord record = ActivityRecord.isInStackLocked(token);
                if (record == null) {
                    throw new IllegalArgumentException("reportSizeConfigurations: ActivityRecord not found for: " + token);
                }
                record.setSizeConfigurations(horizontalSizeConfiguration, verticalSizeConfigurations, smallestSizeConfigurations);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public final void backgroundResourcesReleased(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityStack stack = ActivityRecord.getStackLocked(token);
                    if (stack != null) {
                        stack.backgroundResourcesReleased();
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public final void notifyLaunchTaskBehindComplete(IBinder token) {
        this.mStackSupervisor.scheduleLaunchTaskBehindComplete(token);
    }

    public final void notifyEnterAnimationComplete(IBinder token) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(44, token));
    }

    public String getCallingPackage(IBinder token) {
        String str;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = getCallingRecordLocked(token);
                str = r != null ? r.info.packageName : null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return str;
    }

    public ComponentName getCallingActivity(IBinder token) {
        ComponentName component;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = getCallingRecordLocked(token);
                component = r != null ? r.intent.getComponent() : null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return component;
    }

    private ActivityRecord getCallingRecordLocked(IBinder token) {
        ActivityRecord r = ActivityRecord.isInStackLocked(token);
        if (r == null) {
            return null;
        }
        return r.resultTo;
    }

    public ComponentName getActivityClassForToken(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                ComponentName component = r.intent.getComponent();
                resetPriorityAfterLockedSection();
                return component;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public String getPackageForToken(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                String str = r.packageName;
                resetPriorityAfterLockedSection();
                return str;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean isRootVoiceInteraction(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean z = r.rootVoiceInteraction;
                resetPriorityAfterLockedSection();
                return z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String resultWho, int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle bOptions, int userId) {
        IIntentSender intentSenderLocked;
        enforceNotIsolatedCaller("getIntentSender");
        if (intents != null) {
            if (intents.length < 1) {
                throw new IllegalArgumentException("Intents array length must be >= 1");
            }
            for (int i = 0; i < intents.length; i++) {
                Intent intent = intents[i];
                if (intent != null) {
                    if (intent.hasFileDescriptors()) {
                        throw new IllegalArgumentException("File descriptors passed in Intent");
                    }
                    if (type == 1 && (intent.getFlags() & 33554432) != 0) {
                        throw new IllegalArgumentException("Can't use FLAG_RECEIVER_BOOT_UPGRADE here");
                    }
                    intents[i] = new Intent(intent);
                }
            }
            if (resolvedTypes != null && resolvedTypes.length != intents.length) {
                throw new IllegalArgumentException("Intent array length does not match resolvedTypes length");
            }
        }
        if (bOptions != null && bOptions.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in options");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int callingUid = Binder.getCallingUid();
                int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, type == 1, 0, "getIntentSender", null);
                if (userId == -2) {
                    userId2 = -2;
                }
                if (callingUid != 0 && callingUid != 1000) {
                    try {
                        int uid = AppGlobals.getPackageManager().getPackageUid(packageName, 268435456, UserHandle.getUserId(callingUid));
                        if (!UserHandle.isSameApp(callingUid, uid)) {
                            String msg = "Permission Denial: getIntentSender() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", (need uid=" + uid + ") is not allowed to send as package " + packageName;
                            Slog.w(TAG, msg);
                            throw new SecurityException(msg);
                        }
                    } catch (RemoteException e) {
                        throw new SecurityException(e);
                    }
                }
                intentSenderLocked = getIntentSenderLocked(type, packageName, callingUid, userId2, token, resultWho, requestCode, intents, resolvedTypes, flags, bOptions);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return intentSenderLocked;
    }

    IIntentSender getIntentSenderLocked(int type, String packageName, int callingUid, int userId, IBinder token, String resultWho, int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle bOptions) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "getIntentSenderLocked(): uid=" + callingUid);
        }
        ActivityRecord activity = null;
        if (type == 3) {
            activity = ActivityRecord.isInStackLocked(token);
            if (activity == null) {
                Slog.w(TAG, "Failed createPendingResult: activity " + token + " not in any stack");
                return null;
            }
            if (activity.finishing) {
                Slog.w(TAG, "Failed createPendingResult: activity " + activity + " is finishing");
                return null;
            }
        }
        if (intents != null) {
            for (Intent intent : intents) {
                intent.setDefusable(true);
            }
        }
        Bundle.setDefusable(bOptions, true);
        boolean noCreate = (536870912 & flags) != 0;
        boolean cancelCurrent = (268435456 & flags) != 0;
        boolean updateCurrent = (134217728 & flags) != 0;
        PendingIntentRecord.Key key = new PendingIntentRecord.Key(type, packageName, activity, resultWho, requestCode, intents, resolvedTypes, flags & (-939524097), bOptions, userId);
        WeakReference<PendingIntentRecord> ref = this.mIntentSenderRecords.get(key);
        IIntentSender iIntentSender = ref != null ? (PendingIntentRecord) ref.get() : null;
        if (iIntentSender != null) {
            if (!cancelCurrent) {
                if (updateCurrent) {
                    if (iIntentSender.key.requestIntent != null) {
                        iIntentSender.key.requestIntent.replaceExtras(intents != null ? intents[intents.length - 1] : null);
                    }
                    if (intents != null) {
                        intents[intents.length - 1] = iIntentSender.key.requestIntent;
                        iIntentSender.key.allIntents = intents;
                        iIntentSender.key.allResolvedTypes = resolvedTypes;
                    } else {
                        iIntentSender.key.allIntents = null;
                        iIntentSender.key.allResolvedTypes = null;
                    }
                }
                return iIntentSender;
            }
            iIntentSender.canceled = true;
            this.mIntentSenderRecords.remove(key);
        }
        if (noCreate) {
            return iIntentSender;
        }
        PendingIntentRecord rec = new PendingIntentRecord(this, key, callingUid);
        this.mIntentSenderRecords.put(key, rec.ref);
        if (type == 3) {
            if (activity.pendingResults == null) {
                activity.pendingResults = new HashSet<>();
            }
            activity.pendingResults.add(rec.ref);
        }
        return rec;
    }

    public int sendIntentSender(IIntentSender target, int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        if (target instanceof PendingIntentRecord) {
            return ((PendingIntentRecord) target).sendWithResult(code, intent, resolvedType, finishedReceiver, requiredPermission, options);
        }
        if (intent == null) {
            Slog.wtf(TAG, "Can't use null intent with direct IIntentSender call");
            intent = new Intent("android.intent.action.MAIN");
        }
        try {
            target.send(code, intent, resolvedType, (IIntentReceiver) null, requiredPermission, options);
        } catch (RemoteException e) {
        }
        if (finishedReceiver != null) {
            try {
                finishedReceiver.performReceive(intent, 0, (String) null, (Bundle) null, false, false, UserHandle.getCallingUserId());
                return 0;
            } catch (RemoteException e2) {
                return 0;
            }
        }
        return 0;
    }

    void tempWhitelistAppForPowerSave(int callerPid, int callerUid, int targetUid, long duration) {
        if (ActivityManagerDebugConfig.DEBUG_WHITELISTS) {
            Slog.d(TAG, "tempWhitelistAppForPowerSave(" + callerPid + ", " + callerUid + ", " + targetUid + ", " + duration + ")");
        }
        synchronized (this.mPidsSelfLocked) {
            ProcessRecord pr = this.mPidsSelfLocked.get(callerPid);
            if (pr == null) {
                Slog.w(TAG, "tempWhitelistAppForPowerSave() no ProcessRecord for pid " + callerPid);
                return;
            }
            if (!pr.whitelistManager) {
                if (ActivityManagerDebugConfig.DEBUG_WHITELISTS) {
                    Slog.d(TAG, "tempWhitelistAppForPowerSave() for target " + targetUid + ": pid " + callerPid + " is not allowed");
                }
            } else {
                long token = Binder.clearCallingIdentity();
                try {
                    this.mLocalDeviceIdleController.addPowerSaveTempWhitelistAppDirect(targetUid, duration, true, "pe from uid:" + callerUid);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
    }

    public void cancelIntentSender(IIntentSender sender) {
        if (sender instanceof PendingIntentRecord) {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    PendingIntentRecord rec = (PendingIntentRecord) sender;
                    try {
                        int uid = AppGlobals.getPackageManager().getPackageUid(rec.key.packageName, 268435456, UserHandle.getCallingUserId());
                        if (!UserHandle.isSameApp(uid, Binder.getCallingUid())) {
                            String msg = "Permission Denial: cancelIntentSender() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " is not allowed to cancel packges " + rec.key.packageName;
                            Slog.w(TAG, msg);
                            throw new SecurityException(msg);
                        }
                        cancelIntentSenderLocked(rec, true);
                    } catch (RemoteException e) {
                        throw new SecurityException(e);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        }
    }

    void cancelIntentSenderLocked(PendingIntentRecord rec, boolean cleanActivity) {
        rec.canceled = true;
        this.mIntentSenderRecords.remove(rec.key);
        if (!cleanActivity || rec.key.activity == null) {
            return;
        }
        rec.key.activity.pendingResults.remove(rec.ref);
    }

    public String getPackageForIntentSender(IIntentSender pendingResult) {
        if (!(pendingResult instanceof PendingIntentRecord)) {
            return null;
        }
        try {
            PendingIntentRecord res = (PendingIntentRecord) pendingResult;
            return res.key.packageName;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public int getUidForIntentSender(IIntentSender sender) {
        if (sender instanceof PendingIntentRecord) {
            try {
                PendingIntentRecord res = (PendingIntentRecord) sender;
                return res.uid;
            } catch (ClassCastException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean isIntentSenderTargetedToPackage(IIntentSender pendingResult) {
        if (!(pendingResult instanceof PendingIntentRecord)) {
            return false;
        }
        try {
            PendingIntentRecord res = (PendingIntentRecord) pendingResult;
            if (res.key.allIntents == null) {
                return false;
            }
            for (int i = 0; i < res.key.allIntents.length; i++) {
                Intent intent = res.key.allIntents[i];
                if (intent.getPackage() != null && intent.getComponent() != null) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public boolean isIntentSenderAnActivity(IIntentSender pendingResult) {
        if (!(pendingResult instanceof PendingIntentRecord)) {
            return false;
        }
        try {
            PendingIntentRecord res = (PendingIntentRecord) pendingResult;
            return res.key.type == 2;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public Intent getIntentForIntentSender(IIntentSender pendingResult) {
        enforceCallingPermission("android.permission.GET_INTENT_SENDER_INTENT", "getIntentForIntentSender()");
        if (!(pendingResult instanceof PendingIntentRecord)) {
            return null;
        }
        try {
            PendingIntentRecord res = (PendingIntentRecord) pendingResult;
            if (res.key.requestIntent != null) {
                return new Intent(res.key.requestIntent);
            }
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public String getTagForIntentSender(IIntentSender pendingResult, String prefix) {
        String tagForIntentSenderLocked;
        if (!(pendingResult instanceof PendingIntentRecord)) {
            return null;
        }
        try {
            PendingIntentRecord res = (PendingIntentRecord) pendingResult;
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    tagForIntentSenderLocked = getTagForIntentSenderLocked(res, prefix);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return tagForIntentSenderLocked;
        } catch (ClassCastException e) {
            return null;
        }
    }

    String getTagForIntentSenderLocked(PendingIntentRecord res, String prefix) {
        Intent intent = res.key.requestIntent;
        if (intent == null) {
            return null;
        }
        if (res.lastTag != null && res.lastTagPrefix == prefix && (res.lastTagPrefix == null || res.lastTagPrefix.equals(prefix))) {
            return res.lastTag;
        }
        res.lastTagPrefix = prefix;
        StringBuilder sb = new StringBuilder(128);
        if (prefix != null) {
            sb.append(prefix);
        }
        if (intent.getAction() != null) {
            sb.append(intent.getAction());
        } else if (intent.getComponent() != null) {
            intent.getComponent().appendShortString(sb);
        } else {
            sb.append("?");
        }
        String string = sb.toString();
        res.lastTag = string;
        return string;
    }

    public void setProcessLimit(int max) {
        enforceCallingPermission("android.permission.SET_PROCESS_LIMIT", "setProcessLimit()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mProcessLimit = max < 0 ? 32 : max;
                this.mProcessLimitOverride = max;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        trimApplications();
    }

    public int getProcessLimit() {
        int i;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                i = this.mProcessLimitOverride;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return i;
    }

    void foregroundTokenDied(ForegroundToken token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                synchronized (this.mPidsSelfLocked) {
                    ForegroundToken cur = this.mForegroundProcesses.get(token.pid);
                    if (cur != token) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mForegroundProcesses.remove(token.pid);
                    ProcessRecord pr = this.mPidsSelfLocked.get(token.pid);
                    if (pr == null) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    pr.forcingToForeground = null;
                    updateProcessForegroundLocked(pr, false, false);
                    updateOomAdjLocked();
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setProcessForeground(IBinder token, int pid, boolean isForeground) {
        enforceCallingPermission("android.permission.SET_PROCESS_LIMIT", "setProcessForeground()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                boolean changed = false;
                synchronized (this.mPidsSelfLocked) {
                    ProcessRecord pr = this.mPidsSelfLocked.get(pid);
                    if (pr == null && isForeground) {
                        Slog.w(TAG, "setProcessForeground called on unknown pid: " + pid);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    ForegroundToken oldToken = this.mForegroundProcesses.get(pid);
                    if (oldToken != null) {
                        oldToken.token.unlinkToDeath(oldToken, 0);
                        this.mForegroundProcesses.remove(pid);
                        if (pr != null) {
                            pr.forcingToForeground = null;
                        }
                        changed = true;
                    }
                    if (isForeground && token != null) {
                        ForegroundToken newToken = new ForegroundToken(this) {
                            {
                                super();
                            }

                            @Override
                            public void binderDied() {
                                this.foregroundTokenDied(this);
                            }
                        };
                        newToken.pid = pid;
                        newToken.token = token;
                        try {
                            token.linkToDeath(newToken, 0);
                            this.mForegroundProcesses.put(pid, newToken);
                            pr.forcingToForeground = token;
                            changed = true;
                        } catch (RemoteException e) {
                        }
                    }
                    if (changed) {
                        updateOomAdjLocked();
                    }
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean isAppForeground(int uid) throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                UidRecord uidRec = this.mActiveUids.get(uid);
                if (uidRec == null || uidRec.idle) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean z = uidRec.curProcState <= 6;
                resetPriorityAfterLockedSection();
                return z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    int getUidState(int uid) {
        int i;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                UidRecord uidRec = this.mActiveUids.get(uid);
                i = uidRec == null ? -1 : uidRec.curProcState;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return i;
    }

    public boolean isInMultiWindowMode(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r == null) {
                        return false;
                    }
                    boolean z = r.task.mFullscreen ? false : true;
                    resetPriorityAfterLockedSection();
                    return z;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isInPictureInPictureMode(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityStack stack = ActivityRecord.getStackLocked(token);
                    if (stack == null) {
                        return false;
                    }
                    boolean z = stack.mStackId == 4;
                    resetPriorityAfterLockedSection();
                    return z;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void enterPictureInPictureMode(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (!this.mSupportsPictureInPicture) {
                        throw new IllegalStateException("enterPictureInPictureMode: Device doesn't support picture-in-picture mode.");
                    }
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    if (r == null) {
                        throw new IllegalStateException("enterPictureInPictureMode: Can't find activity for token=" + token);
                    }
                    if (!r.supportsPictureInPicture()) {
                        throw new IllegalArgumentException("enterPictureInPictureMode: Picture-In-Picture not supported for r=" + r);
                    }
                    ActivityStack pinnedStack = this.mStackSupervisor.getStack(4);
                    Rect bounds = pinnedStack != null ? pinnedStack.mBounds : this.mDefaultPinnedStackBounds;
                    this.mStackSupervisor.moveActivityToPinnedStackLocked(r, "enterPictureInPictureMode", bounds);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    static class ProcessInfoService extends IProcessInfoService.Stub {
        final ActivityManagerService mActivityManagerService;

        ProcessInfoService(ActivityManagerService activityManagerService) {
            this.mActivityManagerService = activityManagerService;
        }

        public void getProcessStatesFromPids(int[] pids, int[] states) {
            this.mActivityManagerService.getProcessStatesAndOomScoresForPIDs(pids, states, null);
        }

        public void getProcessStatesAndOomScoresFromPids(int[] pids, int[] states, int[] scores) {
            this.mActivityManagerService.getProcessStatesAndOomScoresForPIDs(pids, states, scores);
        }
    }

    public void getProcessStatesAndOomScoresForPIDs(int[] pids, int[] states, int[] scores) {
        if (scores != null) {
            enforceCallingPermission("android.permission.GET_PROCESS_STATE_AND_OOM_SCORE", "getProcessStatesAndOomScoresForPIDs()");
        }
        if (pids == null) {
            throw new NullPointerException("pids");
        }
        if (states == null) {
            throw new NullPointerException("states");
        }
        if (pids.length != states.length) {
            throw new IllegalArgumentException("pids and states arrays have different lengths!");
        }
        if (scores != null && pids.length != scores.length) {
            throw new IllegalArgumentException("pids and scores arrays have different lengths!");
        }
        synchronized (this.mPidsSelfLocked) {
            for (int i = 0; i < pids.length; i++) {
                ProcessRecord pr = this.mPidsSelfLocked.get(pids[i]);
                states[i] = pr == null ? -1 : pr.curProcState;
                if (scores != null) {
                    scores[i] = pr == null ? -10000 : pr.curAdj;
                }
            }
        }
    }

    static class PermissionController extends IPermissionController.Stub {
        ActivityManagerService mActivityManagerService;

        PermissionController(ActivityManagerService activityManagerService) {
            this.mActivityManagerService = activityManagerService;
        }

        public boolean checkPermission(String permission, int pid, int uid) {
            return this.mActivityManagerService.checkPermission(permission, pid, uid) == 0;
        }

        public String[] getPackagesForUid(int uid) {
            return this.mActivityManagerService.mContext.getPackageManager().getPackagesForUid(uid);
        }

        public boolean isRuntimePermission(String permission) {
            try {
                PermissionInfo info = this.mActivityManagerService.mContext.getPackageManager().getPermissionInfo(permission, 0);
                return info.protectionLevel == 1;
            } catch (PackageManager.NameNotFoundException nnfe) {
                Slog.e(ActivityManagerService.TAG, "No such permission: " + permission, nnfe);
                return false;
            }
        }
    }

    class IntentFirewallInterface implements IntentFirewall.AMSInterface {
        IntentFirewallInterface() {
        }

        @Override
        public int checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
            return ActivityManagerService.this.checkComponentPermission(permission, pid, uid, owningUid, exported);
        }

        @Override
        public Object getAMSLock() {
            return ActivityManagerService.this;
        }
    }

    int checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
        if (pid != MY_PID) {
            if (ActivityManagerDebugConfig.DEBUG_PERMISSION) {
                Slog.d(TAG, "checkComponentPermission: " + permission + ", " + pid + ", " + uid + ", " + owningUid + ", " + exported + ", " + UserHandle.getAppId(uid) + ", " + UserHandle.isIsolated(uid) + ", " + UserHandle.isSameApp(uid, owningUid));
            }
            return ActivityManager.checkComponentPermission(permission, uid, owningUid, exported);
        }
        if (!ActivityManagerDebugConfig.DEBUG_PERMISSION) {
            return 0;
        }
        Slog.d(TAG, "checkComponentPermission: " + permission + ", " + pid + ", " + uid + ", " + owningUid + ", " + exported);
        return 0;
    }

    public int checkPermission(String permission, int pid, int uid) {
        if (permission == null) {
            if (ActivityManagerDebugConfig.DEBUG_PERMISSION) {
                Slog.d(TAG, "checkPermission: permission == null, " + pid + ", " + uid);
            }
            return -1;
        }
        return checkComponentPermission(permission, pid, uid, -1, true);
    }

    public int checkPermissionWithToken(String permission, int pid, int uid, IBinder callerToken) {
        if (permission == null) {
            if (ActivityManagerDebugConfig.DEBUG_PERMISSION) {
                Slog.d(TAG, "checkPermissionWithToken: permission == null, " + pid + ", " + uid);
            }
            return -1;
        }
        Identity tlsIdentity = sCallerIdentity.get();
        if (tlsIdentity != null && tlsIdentity.token == callerToken) {
            Slog.d(TAG, "checkComponentPermission() adjusting {pid,uid} to {" + tlsIdentity.pid + "," + tlsIdentity.uid + "}");
            uid = tlsIdentity.uid;
            pid = tlsIdentity.pid;
        }
        return checkComponentPermission(permission, pid, uid, -1, true);
    }

    int checkCallingPermission(String permission) {
        return checkPermission(permission, Binder.getCallingPid(), UserHandle.getAppId(Binder.getCallingUid()));
    }

    void enforceCallingPermission(String permission, String func) {
        if (checkCallingPermission(permission) == 0) {
            return;
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission;
        Slog.w(TAG, msg);
        throw new SecurityException(msg);
    }

    private final boolean checkHoldingPermissionsLocked(IPackageManager pm, ProviderInfo pi, GrantUri grantUri, int uid, int modeFlags) {
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "checkHoldingPermissionsLocked: uri=" + grantUri + " uid=" + uid);
        }
        if (UserHandle.getUserId(uid) == grantUri.sourceUserId || ActivityManager.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", uid, -1, true) == 0) {
            return checkHoldingPermissionsInternalLocked(pm, pi, grantUri, uid, modeFlags, true);
        }
        return false;
    }

    private final boolean checkHoldingPermissionsInternalLocked(IPackageManager pm, ProviderInfo pi, GrantUri grantUri, int uid, int modeFlags, boolean considerUidPermissions) {
        if (pi.applicationInfo.uid == uid) {
            return true;
        }
        if (!pi.exported) {
            return false;
        }
        boolean readMet = (modeFlags & 1) == 0;
        boolean writeMet = (modeFlags & 2) == 0;
        if (!readMet) {
            try {
                if (pi.readPermission != null && considerUidPermissions && pm.checkUidPermission(pi.readPermission, uid) == 0) {
                    readMet = true;
                }
            } catch (RemoteException e) {
                return false;
            }
        }
        if (!writeMet && pi.writePermission != null && considerUidPermissions && pm.checkUidPermission(pi.writePermission, uid) == 0) {
            writeMet = true;
        }
        boolean allowDefaultRead = pi.readPermission == null;
        boolean allowDefaultWrite = pi.writePermission == null;
        PathPermission[] pps = pi.pathPermissions;
        if (pps != null) {
            String path = grantUri.uri.getPath();
            int i = pps.length;
            while (i > 0 && (!readMet || !writeMet)) {
                i--;
                PathPermission pp = pps[i];
                if (pp.match(path)) {
                    if (!readMet) {
                        String pprperm = pp.getReadPermission();
                        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                            Slog.v(TAG_URI_PERMISSION, "Checking read perm for " + pprperm + " for " + pp.getPath() + ": match=" + pp.match(path) + " check=" + pm.checkUidPermission(pprperm, uid));
                        }
                        if (pprperm != null) {
                            if (considerUidPermissions && pm.checkUidPermission(pprperm, uid) == 0) {
                                readMet = true;
                            } else {
                                allowDefaultRead = false;
                            }
                        }
                    }
                    if (!writeMet) {
                        String ppwperm = pp.getWritePermission();
                        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                            Slog.v(TAG_URI_PERMISSION, "Checking write perm " + ppwperm + " for " + pp.getPath() + ": match=" + pp.match(path) + " check=" + pm.checkUidPermission(ppwperm, uid));
                        }
                        if (ppwperm != null) {
                            if (considerUidPermissions && pm.checkUidPermission(ppwperm, uid) == 0) {
                                writeMet = true;
                            } else {
                                allowDefaultWrite = false;
                            }
                        }
                    }
                }
            }
        }
        if (allowDefaultRead) {
            readMet = true;
        }
        if (allowDefaultWrite) {
            writeMet = true;
        }
        if (readMet) {
            return writeMet;
        }
        return false;
    }

    public int getAppStartMode(int uid, String packageName) {
        int iCheckAllowBackgroundLocked;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                iCheckAllowBackgroundLocked = checkAllowBackgroundLocked(uid, packageName, -1, true);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return iCheckAllowBackgroundLocked;
    }

    int checkAllowBackgroundLocked(int uid, String packageName, int callingPid, boolean allowWhenForeground) {
        ProcessRecord proc;
        UidRecord uidRec = this.mActiveUids.get(uid);
        if (!this.mLenientBackgroundCheck) {
            if ((!allowWhenForeground || uidRec == null || uidRec.curProcState >= 7) && this.mAppOpsService.noteOperation(VR_MODE_CHANGE_MSG, uid, packageName) != 0) {
                return 1;
            }
        } else if (uidRec == null || uidRec.idle) {
            if (callingPid >= 0) {
                synchronized (this.mPidsSelfLocked) {
                    proc = this.mPidsSelfLocked.get(callingPid);
                }
                if (proc != null && proc.curProcState < 11) {
                    return 0;
                }
            }
            if (this.mAppOpsService.noteOperation(VR_MODE_CHANGE_MSG, uid, packageName) != 0) {
                return 1;
            }
        }
        return 0;
    }

    private ProviderInfo getProviderInfoLocked(String authority, int userHandle, int pmFlags) {
        ContentProviderRecord cpr = this.mProviderMap.getProviderByName(authority, userHandle);
        if (cpr != null) {
            ProviderInfo pi = cpr.info;
            return pi;
        }
        try {
            ProviderInfo pi2 = AppGlobals.getPackageManager().resolveContentProvider(authority, pmFlags | PackageManagerService.DumpState.DUMP_VERIFIERS, userHandle);
            return pi2;
        } catch (RemoteException e) {
            return null;
        }
    }

    private UriPermission findUriPermissionLocked(int targetUid, GrantUri grantUri) {
        ArrayMap<GrantUri, UriPermission> targetUris = this.mGrantedUriPermissions.get(targetUid);
        if (targetUris != null) {
            return targetUris.get(grantUri);
        }
        return null;
    }

    private UriPermission findOrCreateUriPermissionLocked(String sourcePkg, String targetPkg, int targetUid, GrantUri grantUri) {
        ArrayMap<GrantUri, UriPermission> targetUris = this.mGrantedUriPermissions.get(targetUid);
        if (targetUris == null) {
            targetUris = Maps.newArrayMap();
            this.mGrantedUriPermissions.put(targetUid, targetUris);
        }
        UriPermission perm = targetUris.get(grantUri);
        if (perm == null) {
            UriPermission perm2 = new UriPermission(sourcePkg, targetPkg, targetUid, grantUri);
            targetUris.put(grantUri, perm2);
            return perm2;
        }
        return perm;
    }

    private final boolean checkUriPermissionLocked(GrantUri grantUri, int uid, int modeFlags) {
        boolean persistable = (modeFlags & 64) != 0;
        int minStrength = persistable ? 3 : 1;
        if (uid == 0) {
            return true;
        }
        ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.get(uid);
        if (perms == null) {
            return false;
        }
        UriPermission exactPerm = perms.get(grantUri);
        if (exactPerm != null && exactPerm.getStrength(modeFlags) >= minStrength) {
            return true;
        }
        int N = perms.size();
        for (int i = 0; i < N; i++) {
            UriPermission perm = perms.valueAt(i);
            if (perm.uri.prefix && grantUri.uri.isPathPrefixMatch(perm.uri.uri) && perm.getStrength(modeFlags) >= minStrength) {
                return true;
            }
        }
        return false;
    }

    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags, int userId, IBinder callerToken) {
        int i;
        enforceNotIsolatedCaller("checkUriPermission");
        Identity tlsIdentity = sCallerIdentity.get();
        if (tlsIdentity != null && tlsIdentity.token == callerToken) {
            uid = tlsIdentity.uid;
            pid = tlsIdentity.pid;
        }
        if (pid == MY_PID) {
            return 0;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                i = checkUriPermissionLocked(new GrantUri(userId, uri, false), uid, modeFlags) ? 0 : -1;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return i;
    }

    int checkGrantUriPermissionLocked(int callingUid, String targetPkg, GrantUri grantUri, int modeFlags, int lastTargetUid) {
        if (!Intent.isAccessUriMode(modeFlags)) {
            return -1;
        }
        if (targetPkg != null && ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "Checking grant " + targetPkg + " permission to " + grantUri);
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        if (!"content".equals(grantUri.uri.getScheme())) {
            if (!ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                return -1;
            }
            Slog.v(TAG_URI_PERMISSION, "Can't grant URI permission for non-content URI: " + grantUri);
            return -1;
        }
        String authority = grantUri.uri.getAuthority();
        ProviderInfo pi = getProviderInfoLocked(authority, grantUri.sourceUserId, 268435456);
        if (pi == null) {
            Slog.w(TAG, "No content provider found for permission check: " + grantUri.uri.toSafeString());
            return -1;
        }
        int targetUid = lastTargetUid;
        if (lastTargetUid < 0 && targetPkg != null) {
            try {
                targetUid = pm.getPackageUid(targetPkg, 268435456, UserHandle.getUserId(callingUid));
                if (targetUid < 0) {
                    if (!ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                        return -1;
                    }
                    Slog.v(TAG_URI_PERMISSION, "Can't grant URI permission no uid for: " + targetPkg);
                    return -1;
                }
            } catch (RemoteException e) {
                return -1;
            }
        }
        if (targetUid < 0) {
            boolean allowed = pi.exported;
            if ((modeFlags & 1) != 0 && pi.readPermission != null) {
                allowed = false;
            }
            if ((modeFlags & 2) != 0 && pi.writePermission != null) {
                allowed = false;
            }
            if (allowed) {
                return -1;
            }
        } else if (checkHoldingPermissionsLocked(pm, pi, grantUri, targetUid, modeFlags)) {
            if (!ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                return -1;
            }
            Slog.v(TAG_URI_PERMISSION, "Target " + targetPkg + " already has full permission to " + grantUri);
            return -1;
        }
        boolean specialCrossUserGrant = UserHandle.getUserId(targetUid) != grantUri.sourceUserId ? checkHoldingPermissionsInternalLocked(pm, pi, grantUri, callingUid, modeFlags, false) : false;
        if (!specialCrossUserGrant) {
            if (!pi.grantUriPermissions) {
                throw new SecurityException("Provider " + pi.packageName + "/" + pi.name + " does not allow granting of Uri permissions (uri " + grantUri + ")");
            }
            if (pi.uriPermissionPatterns != null) {
                int N = pi.uriPermissionPatterns.length;
                boolean allowed2 = false;
                int i = 0;
                while (true) {
                    if (i < N) {
                        if (pi.uriPermissionPatterns[i] != null && pi.uriPermissionPatterns[i].match(grantUri.uri.getPath())) {
                            allowed2 = true;
                            break;
                        }
                        i++;
                    } else {
                        break;
                    }
                }
                if (!allowed2) {
                    throw new SecurityException("Provider " + pi.packageName + "/" + pi.name + " does not allow granting of permission to path of Uri " + grantUri);
                }
            }
        }
        if (UserHandle.getAppId(callingUid) == 1000 || checkHoldingPermissionsLocked(pm, pi, grantUri, callingUid, modeFlags) || checkUriPermissionLocked(grantUri, callingUid, modeFlags)) {
            return targetUid;
        }
        throw new SecurityException("Uid " + callingUid + " does not have permission to uri " + grantUri);
    }

    public int checkGrantUriPermission(int callingUid, String targetPkg, Uri uri, int modeFlags, int userId) {
        int iCheckGrantUriPermissionLocked;
        enforceNotIsolatedCaller("checkGrantUriPermission");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                iCheckGrantUriPermissionLocked = checkGrantUriPermissionLocked(callingUid, targetPkg, new GrantUri(userId, uri, false), modeFlags, -1);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return iCheckGrantUriPermissionLocked;
    }

    void grantUriPermissionUncheckedLocked(int targetUid, String targetPkg, GrantUri grantUri, int modeFlags, UriPermissionOwner owner) {
        if (!Intent.isAccessUriMode(modeFlags)) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "Granting " + targetPkg + "/" + targetUid + " permission to " + grantUri);
        }
        String authority = grantUri.uri.getAuthority();
        ProviderInfo pi = getProviderInfoLocked(authority, grantUri.sourceUserId, 268435456);
        if (pi == null) {
            Slog.w(TAG, "No content provider found for grant: " + grantUri.toSafeString());
            return;
        }
        if ((modeFlags & 128) != 0) {
            grantUri.prefix = true;
        }
        UriPermission perm = findOrCreateUriPermissionLocked(pi.packageName, targetPkg, targetUid, grantUri);
        perm.grantModes(modeFlags, owner);
    }

    void grantUriPermissionLocked(int callingUid, String targetPkg, GrantUri grantUri, int modeFlags, UriPermissionOwner owner, int targetUserId) {
        if (targetPkg == null) {
            throw new NullPointerException(ATTR_TARGET_PKG);
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            int targetUid = checkGrantUriPermissionLocked(callingUid, targetPkg, grantUri, modeFlags, pm.getPackageUid(targetPkg, 268435456, targetUserId));
            if (targetUid < 0) {
                return;
            }
            grantUriPermissionUncheckedLocked(targetUid, targetPkg, grantUri, modeFlags, owner);
        } catch (RemoteException e) {
        }
    }

    static class NeededUriGrants extends ArrayList<GrantUri> {
        final int flags;
        final String targetPkg;
        final int targetUid;

        NeededUriGrants(String targetPkg, int targetUid, int flags) {
            this.targetPkg = targetPkg;
            this.targetUid = targetUid;
            this.flags = flags;
        }
    }

    NeededUriGrants checkGrantUriPermissionFromIntentLocked(int callingUid, String targetPkg, Intent intent, int mode, NeededUriGrants needed, int targetUserId) {
        int targetUid;
        NeededUriGrants newNeeded;
        GrantUri grantUri;
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "Checking URI perm to data=" + (intent != null ? intent.getData() : null) + " clip=" + (intent != null ? intent.getClipData() : null) + " from " + intent + "; flags=0x" + Integer.toHexString(intent != null ? intent.getFlags() : 0));
        }
        if (targetPkg == null) {
            throw new NullPointerException(ATTR_TARGET_PKG);
        }
        if (intent == null) {
            return null;
        }
        Uri data = intent.getData();
        ClipData clip = intent.getClipData();
        if (data == null && clip == null) {
            return null;
        }
        int contentUserHint = intent.getContentUserHint();
        if (contentUserHint == -2) {
            contentUserHint = UserHandle.getUserId(callingUid);
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        if (needed != null) {
            targetUid = needed.targetUid;
        } else {
            try {
                targetUid = pm.getPackageUid(targetPkg, 268435456, targetUserId);
                if (targetUid < 0) {
                    if (!ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                        return null;
                    }
                    Slog.v(TAG_URI_PERMISSION, "Can't grant URI permission no uid for: " + targetPkg + " on user " + targetUserId);
                    return null;
                }
            } catch (RemoteException e) {
                return null;
            }
        }
        if (data != null && (targetUid = checkGrantUriPermissionLocked(callingUid, targetPkg, (grantUri = GrantUri.resolve(contentUserHint, data)), mode, targetUid)) > 0) {
            if (needed == null) {
                needed = new NeededUriGrants(targetPkg, targetUid, mode);
            }
            needed.add(grantUri);
        }
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) {
                    GrantUri grantUri2 = GrantUri.resolve(contentUserHint, uri);
                    targetUid = checkGrantUriPermissionLocked(callingUid, targetPkg, grantUri2, mode, targetUid);
                    if (targetUid > 0) {
                        if (needed == null) {
                            needed = new NeededUriGrants(targetPkg, targetUid, mode);
                        }
                        needed.add(grantUri2);
                    }
                } else {
                    Intent clipIntent = clip.getItemAt(i).getIntent();
                    if (clipIntent != null && (newNeeded = checkGrantUriPermissionFromIntentLocked(callingUid, targetPkg, clipIntent, mode, needed, targetUserId)) != null) {
                        needed = newNeeded;
                    }
                }
            }
        }
        return needed;
    }

    void grantUriPermissionUncheckedFromIntentLocked(NeededUriGrants needed, UriPermissionOwner owner) {
        if (needed == null) {
            return;
        }
        for (int i = 0; i < needed.size(); i++) {
            GrantUri grantUri = needed.get(i);
            grantUriPermissionUncheckedLocked(needed.targetUid, needed.targetPkg, grantUri, needed.flags, owner);
        }
    }

    void grantUriPermissionFromIntentLocked(int callingUid, String targetPkg, Intent intent, UriPermissionOwner owner, int targetUserId) {
        NeededUriGrants needed = checkGrantUriPermissionFromIntentLocked(callingUid, targetPkg, intent, intent != null ? intent.getFlags() : 0, null, targetUserId);
        if (needed == null) {
            return;
        }
        grantUriPermissionUncheckedFromIntentLocked(needed, owner);
    }

    public void grantUriPermission(IApplicationThread caller, String targetPkg, Uri uri, int modeFlags, int userId) {
        enforceNotIsolatedCaller("grantUriPermission");
        GrantUri grantUri = new GrantUri(userId, uri, false);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord r = getRecordForAppLocked(caller);
                if (r == null) {
                    throw new SecurityException("Unable to find app for caller " + caller + " when granting permission to uri " + grantUri);
                }
                if (targetPkg == null) {
                    throw new IllegalArgumentException("null target");
                }
                if (grantUri == null) {
                    throw new IllegalArgumentException("null uri");
                }
                Preconditions.checkFlagsArgument(modeFlags, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS);
                grantUriPermissionLocked(r.uid, targetPkg, grantUri, modeFlags, null, UserHandle.getUserId(r.uid));
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    void removeUriPermissionIfNeededLocked(UriPermission perm) {
        ArrayMap<GrantUri, UriPermission> perms;
        if (perm.modeFlags != 0 || (perms = this.mGrantedUriPermissions.get(perm.targetUid)) == null) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "Removing " + perm.targetUid + " permission to " + perm.uri);
        }
        perms.remove(perm.uri);
        if (!perms.isEmpty()) {
            return;
        }
        this.mGrantedUriPermissions.remove(perm.targetUid);
    }

    private void revokeUriPermissionLocked(int callingUid, GrantUri grantUri, int modeFlags) {
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "Revoking all granted permissions to " + grantUri);
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        String authority = grantUri.uri.getAuthority();
        ProviderInfo pi = getProviderInfoLocked(authority, grantUri.sourceUserId, 786432);
        if (pi == null) {
            Slog.w(TAG, "No content provider found for permission revoke: " + grantUri.toSafeString());
            return;
        }
        if (!checkHoldingPermissionsLocked(pm, pi, grantUri, callingUid, modeFlags)) {
            ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.get(callingUid);
            if (perms != null) {
                boolean persistChanged = false;
                Iterator<UriPermission> it = perms.values().iterator();
                while (it.hasNext()) {
                    UriPermission perm = it.next();
                    if (perm.uri.sourceUserId == grantUri.sourceUserId && perm.uri.uri.isPathPrefixMatch(grantUri.uri)) {
                        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                            Slog.v(TAG_URI_PERMISSION, "Revoking non-owned " + perm.targetUid + " permission to " + perm.uri);
                        }
                        persistChanged |= perm.revokeModes(modeFlags | 64, false);
                        if (perm.modeFlags == 0) {
                            it.remove();
                        }
                    }
                }
                if (perms.isEmpty()) {
                    this.mGrantedUriPermissions.remove(callingUid);
                }
                if (persistChanged) {
                    schedulePersistUriGrants();
                    return;
                }
                return;
            }
            return;
        }
        boolean persistChanged2 = false;
        int N = this.mGrantedUriPermissions.size();
        int i = 0;
        while (i < N) {
            int targetUid = this.mGrantedUriPermissions.keyAt(i);
            ArrayMap<GrantUri, UriPermission> perms2 = this.mGrantedUriPermissions.valueAt(i);
            Iterator<UriPermission> it2 = perms2.values().iterator();
            while (it2.hasNext()) {
                UriPermission perm2 = it2.next();
                if (perm2.uri.sourceUserId == grantUri.sourceUserId && perm2.uri.uri.isPathPrefixMatch(grantUri.uri)) {
                    if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                        Slog.v(TAG_URI_PERMISSION, "Revoking " + perm2.targetUid + " permission to " + perm2.uri);
                    }
                    persistChanged2 |= perm2.revokeModes(modeFlags | 64, true);
                    if (perm2.modeFlags == 0) {
                        it2.remove();
                    }
                }
            }
            if (perms2.isEmpty()) {
                this.mGrantedUriPermissions.remove(targetUid);
                N--;
                i--;
            }
            i++;
        }
        if (!persistChanged2) {
            return;
        }
        schedulePersistUriGrants();
    }

    public void revokeUriPermission(IApplicationThread caller, Uri uri, int modeFlags, int userId) {
        enforceNotIsolatedCaller("revokeUriPermission");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord r = getRecordForAppLocked(caller);
                if (r == null) {
                    throw new SecurityException("Unable to find app for caller " + caller + " when revoking permission to uri " + uri);
                }
                if (uri == null) {
                    Slog.w(TAG, "revokeUriPermission: null uri");
                    return;
                }
                if (Intent.isAccessUriMode(modeFlags)) {
                    String authority = uri.getAuthority();
                    ProviderInfo pi = getProviderInfoLocked(authority, userId, 786432);
                    if (pi == null) {
                        Slog.w(TAG, "No content provider found for permission revoke: " + uri.toSafeString());
                    } else {
                        revokeUriPermissionLocked(r.uid, new GrantUri(userId, uri, false), modeFlags);
                    }
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private void removeUriPermissionsForPackageLocked(String packageName, int userHandle, boolean persistable) {
        if (userHandle == -1 && packageName == null) {
            throw new IllegalArgumentException("Must narrow by either package or user");
        }
        boolean persistChanged = false;
        int N = this.mGrantedUriPermissions.size();
        int i = 0;
        while (i < N) {
            int targetUid = this.mGrantedUriPermissions.keyAt(i);
            ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.valueAt(i);
            if (userHandle == -1 || userHandle == UserHandle.getUserId(targetUid)) {
                Iterator<UriPermission> it = perms.values().iterator();
                while (it.hasNext()) {
                    UriPermission perm = it.next();
                    if (packageName == null || perm.sourcePkg.equals(packageName) || perm.targetPkg.equals(packageName)) {
                        if (!"downloads".equals(perm.uri.uri.getAuthority()) || persistable) {
                            persistChanged |= perm.revokeModes(persistable ? -1 : -65, true);
                            if (perm.modeFlags == 0) {
                                it.remove();
                            }
                        }
                    }
                }
                if (perms.isEmpty()) {
                    this.mGrantedUriPermissions.remove(targetUid);
                    N--;
                    i--;
                }
            }
            i++;
        }
        if (!persistChanged) {
            return;
        }
        schedulePersistUriGrants();
    }

    public IBinder newUriPermissionOwner(String name) {
        Binder externalTokenLocked;
        enforceNotIsolatedCaller("newUriPermissionOwner");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                UriPermissionOwner owner = new UriPermissionOwner(this, name);
                externalTokenLocked = owner.getExternalTokenLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return externalTokenLocked;
    }

    public IBinder getUriPermissionOwnerForActivity(IBinder activityToken) {
        Binder externalTokenLocked;
        enforceNotIsolatedCaller("getUriPermissionOwnerForActivity");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(activityToken);
                if (r == null) {
                    throw new IllegalArgumentException("Activity does not exist; token=" + activityToken);
                }
                externalTokenLocked = r.getUriPermissionsLocked().getExternalTokenLocked();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return externalTokenLocked;
    }

    public void grantUriPermissionFromOwner(IBinder token, int fromUid, String targetPkg, Uri uri, int modeFlags, int sourceUserId, int targetUserId) {
        int targetUserId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), targetUserId, false, 2, "grantUriPermissionFromOwner", null);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                UriPermissionOwner owner = UriPermissionOwner.fromExternalToken(token);
                if (owner == null) {
                    throw new IllegalArgumentException("Unknown owner: " + token);
                }
                if (fromUid != Binder.getCallingUid() && Binder.getCallingUid() != Process.myUid()) {
                    throw new SecurityException("nice try");
                }
                if (targetPkg == null) {
                    throw new IllegalArgumentException("null target");
                }
                if (uri == null) {
                    throw new IllegalArgumentException("null uri");
                }
                grantUriPermissionLocked(fromUid, targetPkg, new GrantUri(sourceUserId, uri, false), modeFlags, owner, targetUserId2);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void revokeUriPermissionFromOwner(IBinder token, Uri uri, int mode, int userId) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                UriPermissionOwner owner = UriPermissionOwner.fromExternalToken(token);
                if (owner == null) {
                    throw new IllegalArgumentException("Unknown owner: " + token);
                }
                if (uri == null) {
                    owner.removeUriPermissionsLocked(mode);
                } else {
                    owner.removeUriPermissionLocked(new GrantUri(userId, uri, false), mode);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private void schedulePersistUriGrants() {
        if (this.mHandler.hasMessages(38)) {
            return;
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(38), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void writeGrantedUriPermissions() {
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "writeGrantedUriPermissions()");
        }
        ArrayList<UriPermission.Snapshot> persist = Lists.newArrayList();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int size = this.mGrantedUriPermissions.size();
                for (int i = 0; i < size; i++) {
                    ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.valueAt(i);
                    for (UriPermission perm : perms.values()) {
                        if (perm.persistedModeFlags != 0) {
                            persist.add(perm.snapshot());
                        }
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        FileOutputStream fos = null;
        try {
            fos = this.mGrantFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_URI_GRANTS);
            for (UriPermission.Snapshot perm2 : persist) {
                fastXmlSerializer.startTag(null, TAG_URI_GRANT);
                XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_SOURCE_USER_ID, perm2.uri.sourceUserId);
                XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_TARGET_USER_ID, perm2.targetUserId);
                fastXmlSerializer.attribute(null, ATTR_SOURCE_PKG, perm2.sourcePkg);
                fastXmlSerializer.attribute(null, ATTR_TARGET_PKG, perm2.targetPkg);
                fastXmlSerializer.attribute(null, ATTR_URI, String.valueOf(perm2.uri.uri));
                XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_PREFIX, perm2.uri.prefix);
                XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_MODE_FLAGS, perm2.persistedModeFlags);
                XmlUtils.writeLongAttribute(fastXmlSerializer, ATTR_CREATED_TIME, perm2.persistedCreateTime);
                fastXmlSerializer.endTag(null, TAG_URI_GRANT);
            }
            fastXmlSerializer.endTag(null, TAG_URI_GRANTS);
            fastXmlSerializer.endDocument();
            this.mGrantFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos == null) {
                return;
            }
            this.mGrantFile.failWrite(fos);
        }
    }

    private void readGrantedUriPermissionsLocked() {
        int sourceUserId;
        int targetUserId;
        if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
            Slog.v(TAG_URI_PERMISSION, "readGrantedUriPermissions()");
        }
        long now = System.currentTimeMillis();
        FileInputStream fis = null;
        try {
            fis = this.mGrantFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int type = in.next();
                if (type == 1) {
                    return;
                }
                String tag = in.getName();
                if (type == 2 && TAG_URI_GRANT.equals(tag)) {
                    int userHandle = XmlUtils.readIntAttribute(in, ATTR_USER_HANDLE, -10000);
                    if (userHandle != -10000) {
                        sourceUserId = userHandle;
                        targetUserId = userHandle;
                    } else {
                        sourceUserId = XmlUtils.readIntAttribute(in, ATTR_SOURCE_USER_ID);
                        targetUserId = XmlUtils.readIntAttribute(in, ATTR_TARGET_USER_ID);
                    }
                    String sourcePkg = in.getAttributeValue(null, ATTR_SOURCE_PKG);
                    String targetPkg = in.getAttributeValue(null, ATTR_TARGET_PKG);
                    Uri uri = Uri.parse(in.getAttributeValue(null, ATTR_URI));
                    boolean prefix = XmlUtils.readBooleanAttribute(in, ATTR_PREFIX);
                    int modeFlags = XmlUtils.readIntAttribute(in, ATTR_MODE_FLAGS);
                    long createdTime = XmlUtils.readLongAttribute(in, ATTR_CREATED_TIME, now);
                    ProviderInfo pi = getProviderInfoLocked(uri.getAuthority(), sourceUserId, 786432);
                    if (pi == null || !sourcePkg.equals(pi.packageName)) {
                        Slog.w(TAG, "Persisted grant for " + uri + " had source " + sourcePkg + " but instead found " + pi);
                    } else {
                        int targetUid = -1;
                        try {
                            targetUid = AppGlobals.getPackageManager().getPackageUid(targetPkg, PackageManagerService.DumpState.DUMP_PREFERRED_XML, targetUserId);
                        } catch (RemoteException e) {
                        }
                        if (targetUid != -1) {
                            UriPermission perm = findOrCreateUriPermissionLocked(sourcePkg, targetPkg, targetUid, new GrantUri(sourceUserId, uri, prefix));
                            perm.initPersistedModes(modeFlags, createdTime);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e2) {
        } catch (IOException e3) {
            Slog.wtf(TAG, "Failed reading Uri grants", e3);
        } catch (XmlPullParserException e4) {
            Slog.wtf(TAG, "Failed reading Uri grants", e4);
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    public void takePersistableUriPermission(Uri uri, int modeFlags, int userId) {
        enforceNotIsolatedCaller("takePersistableUriPermission");
        Preconditions.checkFlagsArgument(modeFlags, 3);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int callingUid = Binder.getCallingUid();
                boolean persistChanged = false;
                GrantUri grantUri = new GrantUri(userId, uri, false);
                UriPermission exactPerm = findUriPermissionLocked(callingUid, new GrantUri(userId, uri, false));
                UriPermission prefixPerm = findUriPermissionLocked(callingUid, new GrantUri(userId, uri, true));
                boolean exactValid = exactPerm != null && (exactPerm.persistableModeFlags & modeFlags) == modeFlags;
                boolean prefixValid = prefixPerm != null && (prefixPerm.persistableModeFlags & modeFlags) == modeFlags;
                if (!(exactValid ? true : prefixValid)) {
                    throw new SecurityException("No persistable permission grants found for UID " + callingUid + " and Uri " + grantUri.toSafeString());
                }
                if (exactValid) {
                    persistChanged = exactPerm.takePersistableModes(modeFlags);
                }
                if (prefixValid) {
                    persistChanged |= prefixPerm.takePersistableModes(modeFlags);
                }
                if (persistChanged | maybePrunePersistedUriGrantsLocked(callingUid)) {
                    schedulePersistUriGrants();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void releasePersistableUriPermission(Uri uri, int modeFlags, int userId) {
        enforceNotIsolatedCaller("releasePersistableUriPermission");
        Preconditions.checkFlagsArgument(modeFlags, 3);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int callingUid = Binder.getCallingUid();
                boolean persistChanged = false;
                UriPermission exactPerm = findUriPermissionLocked(callingUid, new GrantUri(userId, uri, false));
                UriPermission prefixPerm = findUriPermissionLocked(callingUid, new GrantUri(userId, uri, true));
                if (exactPerm == null && prefixPerm == null) {
                    throw new SecurityException("No permission grants found for UID " + callingUid + " and Uri " + uri.toSafeString());
                }
                if (exactPerm != null) {
                    persistChanged = exactPerm.releasePersistableModes(modeFlags);
                    removeUriPermissionIfNeededLocked(exactPerm);
                }
                if (prefixPerm != null) {
                    persistChanged |= prefixPerm.releasePersistableModes(modeFlags);
                    removeUriPermissionIfNeededLocked(prefixPerm);
                }
                if (persistChanged) {
                    schedulePersistUriGrants();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private boolean maybePrunePersistedUriGrantsLocked(int uid) {
        ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.get(uid);
        if (perms == null || perms.size() < 128) {
            return false;
        }
        ArrayList<UriPermission> persisted = Lists.newArrayList();
        for (UriPermission perm : perms.values()) {
            if (perm.persistedModeFlags != 0) {
                persisted.add(perm);
            }
        }
        int trimCount = persisted.size() - 128;
        if (trimCount <= 0) {
            return false;
        }
        Collections.sort(persisted, new UriPermission.PersistedTimeComparator());
        for (int i = 0; i < trimCount; i++) {
            UriPermission perm2 = persisted.get(i);
            if (ActivityManagerDebugConfig.DEBUG_URI_PERMISSION) {
                Slog.v(TAG_URI_PERMISSION, "Trimming grant created at " + perm2.persistedCreateTime);
            }
            perm2.releasePersistableModes(-1);
            removeUriPermissionIfNeededLocked(perm2);
        }
        return true;
    }

    public ParceledListSlice<android.content.UriPermission> getPersistedUriPermissions(String packageName, boolean incoming) {
        enforceNotIsolatedCaller("getPersistedUriPermissions");
        Preconditions.checkNotNull(packageName, "packageName");
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            int packageUid = pm.getPackageUid(packageName, 786432, callingUserId);
            if (packageUid != callingUid) {
                throw new SecurityException("Package " + packageName + " does not belong to calling UID " + callingUid);
            }
            ArrayList<android.content.UriPermission> result = Lists.newArrayList();
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (incoming) {
                        ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.get(callingUid);
                        if (perms == null) {
                            Slog.w(TAG, "No permission grants found for " + packageName);
                        } else {
                            for (UriPermission perm : perms.values()) {
                                if (packageName.equals(perm.targetPkg) && perm.persistedModeFlags != 0) {
                                    result.add(perm.buildPersistedPublicApiObject());
                                }
                            }
                        }
                    } else {
                        int size = this.mGrantedUriPermissions.size();
                        for (int i = 0; i < size; i++) {
                            for (UriPermission perm2 : this.mGrantedUriPermissions.valueAt(i).values()) {
                                if (packageName.equals(perm2.sourcePkg) && perm2.persistedModeFlags != 0) {
                                    result.add(perm2.buildPersistedPublicApiObject());
                                }
                            }
                        }
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            return new ParceledListSlice<>(result);
        } catch (RemoteException e) {
            throw new SecurityException("Failed to verify package name ownership");
        }
    }

    public ParceledListSlice<android.content.UriPermission> getGrantedUriPermissions(String packageName, int userId) {
        enforceCallingPermission("android.permission.GET_APP_GRANTED_URI_PERMISSIONS", "getGrantedUriPermissions");
        ArrayList<android.content.UriPermission> result = Lists.newArrayList();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int size = this.mGrantedUriPermissions.size();
                for (int i = 0; i < size; i++) {
                    ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.valueAt(i);
                    for (UriPermission perm : perms.values()) {
                        if (packageName.equals(perm.targetPkg) && perm.targetUserId == userId && perm.persistedModeFlags != 0) {
                            result.add(perm.buildPersistedPublicApiObject());
                        }
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return new ParceledListSlice<>(result);
    }

    public void clearGrantedUriPermissions(String packageName, int userId) {
        enforceCallingPermission("android.permission.CLEAR_APP_GRANTED_URI_PERMISSIONS", "clearGrantedUriPermissions");
        removeUriPermissionsForPackageLocked(packageName, userId, true);
    }

    public void showWaitingForDebugger(IApplicationThread who, boolean waiting) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord recordForAppLocked = who != null ? getRecordForAppLocked(who) : null;
                if (recordForAppLocked == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                Message msg = Message.obtain();
                msg.what = 6;
                msg.obj = recordForAppLocked;
                msg.arg1 = waiting ? 1 : 0;
                this.mUiHandler.sendMessage(msg);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void getMemoryInfo(ActivityManager.MemoryInfo outInfo) {
        long homeAppMem = this.mProcessList.getMemLevel(600);
        long cachedAppMem = this.mProcessList.getMemLevel(900);
        outInfo.availMem = Process.getFreeMemory();
        outInfo.totalMem = Process.getTotalMemory();
        outInfo.threshold = homeAppMem;
        outInfo.lowMemory = outInfo.availMem < ((cachedAppMem - homeAppMem) / 2) + homeAppMem;
        outInfo.hiddenAppThreshold = cachedAppMem;
        outInfo.secondaryServerThreshold = this.mProcessList.getMemLevel(500);
        outInfo.visibleAppThreshold = this.mProcessList.getMemLevel(100);
        outInfo.foregroundAppThreshold = this.mProcessList.getMemLevel(0);
    }

    public List<IAppTask> getAppTasks(String callingPackage) {
        ArrayList<IAppTask> list;
        Intent intent;
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                list = new ArrayList<>();
                try {
                    if (ActivityManagerDebugConfig.DEBUG_ALL) {
                        Slog.v(TAG, "getAppTasks");
                    }
                    int N = this.mRecentTasks.size();
                    for (int i = 0; i < N; i++) {
                        TaskRecord tr = this.mRecentTasks.get(i);
                        if (tr.effectiveUid == callingUid && (intent = tr.getBaseIntent()) != null && callingPackage.equals(intent.getComponent().getPackageName())) {
                            ActivityManager.RecentTaskInfo taskInfo = createRecentTaskInfoFromTaskRecord(tr);
                            AppTaskImpl taskImpl = new AppTaskImpl(taskInfo.persistentId, callingUid);
                            list.add(taskImpl);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return list;
    }

    public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, int flags) {
        int callingUid = Binder.getCallingUid();
        ArrayList<ActivityManager.RunningTaskInfo> list = new ArrayList<>();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (ActivityManagerDebugConfig.DEBUG_ALL) {
                    Slog.v(TAG, "getTasks: max=" + maxNum + ", flags=" + flags);
                }
                boolean allowed = isGetTasksAllowed("getTasks", Binder.getCallingPid(), callingUid);
                this.mStackSupervisor.getTasksLocked(maxNum, list, callingUid, allowed);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return list;
    }

    private ActivityManager.RecentTaskInfo createRecentTaskInfoFromTaskRecord(TaskRecord tr) {
        tr.updateTaskDescription();
        ActivityManager.RecentTaskInfo rti = new ActivityManager.RecentTaskInfo();
        rti.id = tr.getTopActivity() == null ? -1 : tr.taskId;
        rti.persistentId = tr.taskId;
        rti.baseIntent = new Intent(tr.getBaseIntent());
        rti.origActivity = tr.origActivity;
        rti.realActivity = tr.realActivity;
        rti.description = tr.lastDescription;
        rti.stackId = tr.stack != null ? tr.stack.mStackId : -1;
        rti.userId = tr.userId;
        rti.taskDescription = new ActivityManager.TaskDescription(tr.lastTaskDescription);
        rti.firstActiveTime = tr.firstActiveTime;
        rti.lastActiveTime = tr.lastActiveTime;
        rti.affiliatedTaskId = tr.mAffiliatedTaskId;
        rti.affiliatedTaskColor = tr.mAffiliatedTaskColor;
        rti.numActivities = 0;
        if (tr.mBounds != null) {
            rti.bounds = new Rect(tr.mBounds);
        }
        rti.isDockable = tr.canGoInDockedStack();
        rti.resizeMode = tr.mResizeMode;
        ActivityRecord base = null;
        ActivityRecord top = null;
        for (int i = tr.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord tmp = tr.mActivities.get(i);
            if (!tmp.finishing) {
                base = tmp;
                if (top == null || top.state == ActivityStack.ActivityState.INITIALIZING) {
                    top = tmp;
                }
                rti.numActivities++;
            }
        }
        rti.baseActivity = base != null ? base.intent.getComponent() : null;
        rti.topActivity = top != null ? top.intent.getComponent() : null;
        return rti;
    }

    private boolean isGetTasksAllowed(String caller, int callingPid, int callingUid) {
        boolean allowed = checkPermission("android.permission.REAL_GET_TASKS", callingPid, callingUid) == 0;
        if (!allowed && checkPermission("android.permission.GET_TASKS", callingPid, callingUid) == 0) {
            try {
                if (AppGlobals.getPackageManager().isUidPrivileged(callingUid)) {
                    allowed = true;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.w(TAG, caller + ": caller " + callingUid + " is using old GET_TASKS but privileged; allowing");
                    }
                }
            } catch (RemoteException e) {
            }
        }
        if (!allowed && ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.w(TAG, caller + ": caller " + callingUid + " does not hold REAL_GET_TASKS; limiting output");
        }
        return allowed;
    }

    public ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId) {
        ActivityStack stack;
        int callingUid = Binder.getCallingUid();
        int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, 2, "getRecentTasks", null);
        boolean includeProfiles = (flags & 4) != 0;
        boolean withExcluded = (flags & 1) != 0;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                boolean allowed = isGetTasksAllowed("getRecentTasks", Binder.getCallingPid(), callingUid);
                boolean detailed = checkCallingPermission("android.permission.GET_DETAILED_TASKS") == 0;
                if (!isUserRunning(userId2, 4)) {
                    Slog.i(TAG, "user " + userId2 + " is still locked. Cannot load recents");
                    ParceledListSlice<ActivityManager.RecentTaskInfo> parceledListSliceEmptyList = ParceledListSlice.emptyList();
                    resetPriorityAfterLockedSection();
                    return parceledListSliceEmptyList;
                }
                this.mRecentTasks.loadUserRecentsLocked(userId2);
                int recentsCount = this.mRecentTasks.size();
                ArrayList<ActivityManager.RecentTaskInfo> res = new ArrayList<>(maxNum < recentsCount ? maxNum : recentsCount);
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "getRecentTasks recentsCount = " + recentsCount + " maxNum = " + maxNum + " flags = " + flags + " userId = " + userId2);
                }
                Set<Integer> includedUsers = includeProfiles ? this.mUserController.getProfileIds(userId2) : new HashSet<>();
                includedUsers.add(Integer.valueOf(userId2));
                for (int i = 0; i < recentsCount && maxNum > 0; i++) {
                    TaskRecord tr = this.mRecentTasks.get(i);
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "getRecentTasks tr = " + tr);
                    }
                    if (includedUsers.contains(Integer.valueOf(tr.userId))) {
                        if (tr.realActivitySuspended) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "Skipping, activity suspended: " + tr);
                            }
                        } else if (i == 0 || withExcluded || tr.intent == null || (tr.intent.getFlags() & 8388608) == 0) {
                            if (allowed || tr.isHomeTask() || tr.effectiveUid == callingUid) {
                                if ((flags & 8) == 0 || tr.stack == null || !tr.stack.isHomeStack()) {
                                    if ((flags & 16) == 0 || (stack = tr.stack) == null || !stack.isDockedStack() || stack.topTask() != tr) {
                                        if ((flags & 32) == 0 || tr.stack == null || !tr.stack.isPinnedStack()) {
                                            if (tr.autoRemoveRecents && tr.getTopActivity() == null) {
                                                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                                    Slog.d(TAG_RECENTS, "Skipping, auto-remove without activity: " + tr);
                                                }
                                            } else if ((flags & 2) == 0 || tr.isAvailable) {
                                                if (tr.mUserSetupComplete) {
                                                    ActivityManager.RecentTaskInfo rti = createRecentTaskInfoFromTaskRecord(tr);
                                                    if (!detailed) {
                                                        rti.baseIntent.replaceExtras((Bundle) null);
                                                    }
                                                    res.add(rti);
                                                    maxNum--;
                                                } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                                    Slog.d(TAG_RECENTS, "Skipping, user setup not complete: " + tr);
                                                }
                                            } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                                Slog.d(TAG_RECENTS, "Skipping, unavail real act: " + tr);
                                            }
                                        } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                            Slog.d(TAG_RECENTS, "Skipping, pinned stack task: " + tr);
                                        }
                                    } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                        Slog.d(TAG_RECENTS, "Skipping, top task in docked stack: " + tr);
                                    }
                                } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                    Slog.d(TAG_RECENTS, "Skipping, home stack task: " + tr);
                                }
                            } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "Skipping, not allowed: " + tr);
                            }
                        }
                    } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "Skipping, not user: " + tr);
                    }
                }
                ParceledListSlice<ActivityManager.RecentTaskInfo> parceledListSlice = new ParceledListSlice<>(res);
                resetPriorityAfterLockedSection();
                return parceledListSlice;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public ActivityManager.TaskThumbnail getTaskThumbnail(int id) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                enforceCallingPermission("android.permission.READ_FRAME_BUFFER", "getTaskThumbnail()");
                TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(id, false, -1);
                if (tr == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                ActivityManager.TaskThumbnail taskThumbnailLocked = tr.getTaskThumbnailLocked();
                resetPriorityAfterLockedSection();
                return taskThumbnailLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int addAppTask(IBinder activityToken, Intent intent, ActivityManager.TaskDescription description, Bitmap thumbnail) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        long callingIdent = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInStackLocked(activityToken);
                    if (r == null) {
                        throw new IllegalArgumentException("Activity does not exist; token=" + activityToken);
                    }
                    ComponentName comp = intent.getComponent();
                    if (comp == null) {
                        throw new IllegalArgumentException("Intent " + intent + " must specify explicit component");
                    }
                    if (thumbnail.getWidth() != this.mThumbnailWidth || thumbnail.getHeight() != this.mThumbnailHeight) {
                        throw new IllegalArgumentException("Bad thumbnail size: got " + thumbnail.getWidth() + "x" + thumbnail.getHeight() + ", require " + this.mThumbnailWidth + "x" + this.mThumbnailHeight);
                    }
                    if (intent.getSelector() != null) {
                        intent.setSelector(null);
                    }
                    if (intent.getSourceBounds() != null) {
                        intent.setSourceBounds(null);
                    }
                    if ((intent.getFlags() & PackageManagerService.DumpState.DUMP_FROZEN) != 0) {
                        if ((intent.getFlags() & PackageManagerService.DumpState.DUMP_PREFERRED_XML) == 0) {
                            intent.addFlags(PackageManagerService.DumpState.DUMP_PREFERRED_XML);
                        }
                    } else if ((intent.getFlags() & 268435456) != 0) {
                        intent.addFlags(268435456);
                    }
                    if (!comp.equals(this.mLastAddedTaskComponent) || callingUid != this.mLastAddedTaskUid) {
                        this.mLastAddedTaskActivity = null;
                    }
                    ActivityInfo ainfo = this.mLastAddedTaskActivity;
                    if (ainfo == null) {
                        ainfo = AppGlobals.getPackageManager().getActivityInfo(comp, 0, UserHandle.getUserId(callingUid));
                        this.mLastAddedTaskActivity = ainfo;
                        if (ainfo.applicationInfo.uid != callingUid) {
                            throw new SecurityException("Can't add task for another application: target uid=" + ainfo.applicationInfo.uid + ", calling uid=" + callingUid);
                        }
                    }
                    Point displaySize = new Point();
                    ActivityManager.TaskThumbnailInfo thumbnailInfo = new ActivityManager.TaskThumbnailInfo();
                    r.task.stack.getDisplaySize(displaySize);
                    thumbnailInfo.taskWidth = displaySize.x;
                    thumbnailInfo.taskHeight = displaySize.y;
                    thumbnailInfo.screenOrientation = this.mConfiguration.orientation;
                    TaskRecord task = new TaskRecord(this, this.mStackSupervisor.getNextTaskIdForUserLocked(r.userId), ainfo, intent, description, thumbnailInfo);
                    int trimIdx = this.mRecentTasks.trimForTaskLocked(task, false);
                    if (trimIdx >= 0) {
                        return -1;
                    }
                    int N = this.mRecentTasks.size();
                    if (N >= ActivityManager.getMaxRecentTasksStatic() - 1) {
                        TaskRecord tr = this.mRecentTasks.remove(N - 1);
                        tr.removedFromRecents();
                    }
                    task.inRecents = true;
                    this.mRecentTasks.add(task);
                    r.task.stack.addTask(task, false, "addAppTask");
                    task.setLastThumbnailLocked(thumbnail);
                    task.freeLastThumbnail();
                    int i = task.taskId;
                    resetPriorityAfterLockedSection();
                    return i;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingIdent);
        }
    }

    public Point getAppTaskThumbnailSize() {
        Point point;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                point = new Point(this.mThumbnailWidth, this.mThumbnailHeight);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return point;
    }

    public void setTaskDescription(IBinder token, ActivityManager.TaskDescription td) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r != null) {
                    r.setTaskDescription(td);
                    r.task.updateTaskDescription();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setTaskResizeable(int taskId, int resizeableMode) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId, false, -1);
                if (task == null) {
                    Slog.w(TAG, "setTaskResizeable: taskId=" + taskId + " not found");
                    return;
                }
                if (task.mResizeMode != resizeableMode) {
                    task.mResizeMode = resizeableMode;
                    this.mWindowManager.setTaskResizeable(taskId, resizeableMode);
                    this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void resizeTask(int taskId, Rect bounds, int resizeMode) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "resizeTask()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                    if (task == null) {
                        Slog.w(TAG, "resizeTask: taskId=" + taskId + " not found");
                        return;
                    }
                    int stackId = task.stack.mStackId;
                    if (bounds != null && task.inCropWindowsResizeMode() && this.mStackSupervisor.isStackDockedInEffect(stackId)) {
                        this.mWindowManager.scrollTask(task.taskId, bounds);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (!ActivityManager.StackId.isTaskResizeAllowed(stackId)) {
                        throw new IllegalArgumentException("resizeTask not allowed on task=" + task);
                    }
                    if (bounds == null && stackId == 2) {
                        stackId = 1;
                    } else if (bounds != null && stackId != 2) {
                        stackId = 2;
                    }
                    boolean preserveWindow = (resizeMode & 1) != 0;
                    if (stackId != task.stack.mStackId) {
                        this.mStackSupervisor.moveTaskToStackUncheckedLocked(task, stackId, true, false, "resizeTask");
                        preserveWindow = false;
                    }
                    this.mStackSupervisor.resizeTaskLocked(task, bounds, resizeMode, preserveWindow, false);
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public Rect getTaskBounds(int taskId) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "getTaskBounds()");
        long ident = Binder.clearCallingIdentity();
        Rect rect = new Rect();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId, false, -1);
                    if (task == null) {
                        Slog.w(TAG, "getTaskBounds: taskId=" + taskId + " not found");
                        return rect;
                    }
                    if (task.stack != null) {
                        this.mWindowManager.getTaskBounds(task.taskId, rect);
                    } else if (task.mBounds != null) {
                        rect.set(task.mBounds);
                    } else if (task.mLastNonFullscreenBounds != null) {
                        rect.set(task.mLastNonFullscreenBounds);
                    }
                    resetPriorityAfterLockedSection();
                    return rect;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public Bitmap getTaskDescriptionIcon(String filePath, int userId) {
        if (userId != UserHandle.getCallingUserId()) {
            enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "getTaskDescriptionIcon");
        }
        File passedIconFile = new File(filePath);
        File legitIconFile = new File(TaskPersister.getUserImagesDir(userId), passedIconFile.getName());
        if (!legitIconFile.getPath().equals(filePath) || !filePath.contains("_activity_icon_")) {
            throw new IllegalArgumentException("Bad file path: " + filePath + " passed for userId " + userId);
        }
        return this.mRecentTasks.getTaskDescriptionIcon(filePath);
    }

    public void startInPlaceAnimationOnFrontMostApplication(ActivityOptions opts) throws RemoteException {
        if (opts.getAnimationType() != 10 || opts.getCustomInPlaceResId() == 0) {
            throw new IllegalArgumentException("Expected in-place ActivityOption with valid animation");
        }
        this.mWindowManager.prepareAppTransition(17, false);
        this.mWindowManager.overridePendingAppTransitionInPlace(opts.getPackageName(), opts.getCustomInPlaceResId());
        this.mWindowManager.executeAppTransition();
    }

    private void cleanUpRemovedTaskLocked(TaskRecord tr, boolean killProcess, boolean removeFromRecents) {
        if (removeFromRecents) {
            this.mRecentTasks.remove(tr);
            tr.removedFromRecents();
        }
        ComponentName component = tr.getBaseIntent().getComponent();
        if (component == null) {
            Slog.w(TAG, "No component for base intent of task: " + tr);
            return;
        }
        this.mServices.cleanUpRemovedTaskLocked(tr, component, new Intent(tr.getBaseIntent()));
        if (!killProcess) {
            return;
        }
        String pkg = component.getPackageName();
        ArrayList<ProcessRecord> procsToKill = new ArrayList<>();
        ArrayMap<String, SparseArray<ProcessRecord>> pmap = this.mProcessNames.getMap();
        for (int i = 0; i < pmap.size(); i++) {
            SparseArray<ProcessRecord> uids = pmap.valueAt(i);
            for (int j = 0; j < uids.size(); j++) {
                ProcessRecord proc = uids.valueAt(j);
                if (proc.userId == tr.userId && proc != this.mHomeProcess && proc.pkgList.containsKey(pkg)) {
                    for (int k = 0; k < proc.activities.size(); k++) {
                        TaskRecord otherTask = proc.activities.get(k).task;
                        if (tr.taskId != otherTask.taskId && otherTask.inRecents) {
                            return;
                        }
                    }
                    if (proc.foregroundServices) {
                        return;
                    } else {
                        procsToKill.add(proc);
                    }
                }
            }
        }
        for (int i2 = 0; i2 < procsToKill.size(); i2++) {
            ProcessRecord pr = procsToKill.get(i2);
            if (pr.setSchedGroup == 0 && pr.curReceiver == null) {
                pr.kill("remove task", true);
            } else {
                pr.waitingToKill = "remove task";
            }
        }
    }

    private void removeTasksByPackageNameLocked(String packageName, int userId) {
        ComponentName cn;
        for (int i = this.mRecentTasks.size() - 1; i >= 0; i--) {
            TaskRecord tr = this.mRecentTasks.get(i);
            if (tr.userId == userId && (cn = tr.intent.getComponent()) != null && cn.getPackageName().equals(packageName)) {
                removeTaskByIdLocked(tr.taskId, true, true);
            }
        }
    }

    private void cleanupDisabledPackageTasksLocked(String packageName, Set<String> filterByClasses, int userId) {
        boolean sameComponent;
        for (int i = this.mRecentTasks.size() - 1; i >= 0; i--) {
            TaskRecord tr = this.mRecentTasks.get(i);
            if (userId == -1 || tr.userId == userId) {
                ComponentName cn = tr.intent.getComponent();
                if (cn == null || !cn.getPackageName().equals(packageName)) {
                    sameComponent = false;
                } else {
                    sameComponent = filterByClasses != null ? filterByClasses.contains(cn.getClassName()) : true;
                }
                if (sameComponent) {
                    removeTaskByIdLocked(tr.taskId, false, true);
                }
            }
        }
    }

    private boolean removeTaskByIdLocked(int taskId, boolean killProcess, boolean removeFromRecents) {
        TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(taskId, false, -1);
        if (tr != null) {
            tr.removeTaskActivitiesLocked();
            cleanUpRemovedTaskLocked(tr, killProcess, removeFromRecents);
            if (tr.isPersistable) {
                notifyTaskPersisterLocked(null, true);
            }
            return true;
        }
        Slog.w(TAG, "Request to remove task ignored for non-existent task " + taskId);
        return false;
    }

    public void removeStack(int stackId) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "removeStack()");
        if (stackId == 0) {
            throw new IllegalArgumentException("Removing home stack is not allowed.");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityStack stack = this.mStackSupervisor.getStack(stackId);
                    if (stack == null) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    ArrayList<TaskRecord> tasks = stack.getAllTasks();
                    for (int i = tasks.size() - 1; i >= 0; i--) {
                        removeTaskByIdLocked(tasks.get(i).taskId, true, true);
                    }
                    resetPriorityAfterLockedSection();
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean removeTask(int taskId) {
        boolean zRemoveTaskByIdLocked;
        enforceCallingPermission("android.permission.REMOVE_TASKS", "removeTask()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    zRemoveTaskByIdLocked = removeTaskByIdLocked(taskId, true, true);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zRemoveTaskByIdLocked;
    }

    public void moveTaskToFront(int taskId, int flags, Bundle bOptions) {
        enforceCallingPermission("android.permission.REORDER_TASKS", "moveTaskToFront()");
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "moveTaskToFront: moving taskId=" + taskId);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                moveTaskToFrontLocked(taskId, flags, bOptions);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void moveTaskToFrontLocked(int taskId, int flags, Bundle bOptions) {
        ActivityOptions options = ActivityOptions.fromBundle(bOptions);
        if (!checkAppSwitchAllowedLocked(Binder.getCallingPid(), Binder.getCallingUid(), -1, -1, "Task to front")) {
            ActivityOptions.abort(options);
            return;
        }
        long origId = Binder.clearCallingIdentity();
        try {
            TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
            if (task == null) {
                Slog.d(TAG, "Could not find task for id: " + taskId);
                return;
            }
            if (this.mStackSupervisor.isLockTaskModeViolation(task)) {
                this.mStackSupervisor.showLockTaskToast();
                Slog.e(TAG, "moveTaskToFront: Attempt to violate Lock Task Mode");
                return;
            }
            ActivityRecord prev = this.mStackSupervisor.topRunningActivityLocked();
            if (prev != null && prev.isRecentsActivity()) {
                task.setTaskToReturnTo(2);
            }
            this.mStackSupervisor.findTaskToMoveToFrontLocked(task, flags, options, "moveTaskToFront", false);
            Binder.restoreCallingIdentity(origId);
            ActivityOptions.abort(options);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot) {
        enforceNotIsolatedCaller("moveActivityTaskToBack");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    int taskId = ActivityRecord.getTaskForActivityLocked(token, !nonRoot);
                    TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                    if (task == null) {
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    if (this.mStackSupervisor.isLockedTask(task)) {
                        this.mStackSupervisor.showLockTaskToast();
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean zMoveTaskToBackLocked = ActivityRecord.getStackLocked(token).moveTaskToBackLocked(taskId);
                    resetPriorityAfterLockedSection();
                    return zMoveTaskToBackLocked;
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void moveTaskBackwards(int task) {
        enforceCallingPermission("android.permission.REORDER_TASKS", "moveTaskBackwards()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!checkAppSwitchAllowedLocked(Binder.getCallingPid(), Binder.getCallingUid(), -1, -1, "Task backwards")) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                long origId = Binder.clearCallingIdentity();
                moveTaskBackwardsLocked(task);
                Binder.restoreCallingIdentity(origId);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private final void moveTaskBackwardsLocked(int task) {
        Slog.e(TAG, "moveTaskBackwards not yet implemented!");
    }

    public IActivityContainer createVirtualActivityContainer(IBinder parentActivityToken, IActivityContainerCallback callback) throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "createActivityContainer()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (parentActivityToken == null) {
                    throw new IllegalArgumentException("parent token must not be null");
                }
                ActivityRecord r = ActivityRecord.forTokenLocked(parentActivityToken);
                if (r == null) {
                    return null;
                }
                if (callback == null) {
                    throw new IllegalArgumentException("callback must not be null");
                }
                return this.mStackSupervisor.createVirtualActivityContainer(r, callback);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void deleteActivityContainer(IActivityContainer container) throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "deleteActivityContainer()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mStackSupervisor.deleteActivityContainer(container);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public IActivityContainer createStackOnDisplay(int displayId) throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "createStackOnDisplay()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int stackId = this.mStackSupervisor.getNextStackId();
                ActivityStack stack = this.mStackSupervisor.createStackOnDisplay(stackId, displayId, true);
                if (stack == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                ActivityStackSupervisor.ActivityContainer activityContainer = stack.mActivityContainer;
                resetPriorityAfterLockedSection();
                return activityContainer;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getActivityDisplayId(IBinder activityToken) throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(activityToken);
                if (stack == null || !stack.mActivityContainer.isAttachedLocked()) {
                    resetPriorityAfterLockedSection();
                    return 0;
                }
                int displayId = stack.mActivityContainer.getDisplayId();
                resetPriorityAfterLockedSection();
                return displayId;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getActivityStackId(IBinder token) throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack == null) {
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                int i = stack.mStackId;
                resetPriorityAfterLockedSection();
                return i;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void exitFreeformMode(IBinder token) throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    if (r == null) {
                        throw new IllegalArgumentException("exitFreeformMode: No activity record matching token=" + token);
                    }
                    ActivityStack stack = ActivityRecord.getStackLocked(token);
                    if (stack == null || stack.mStackId != 2) {
                        throw new IllegalStateException("exitFreeformMode: You can only go fullscreen from freeform.");
                    }
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_STACK, "exitFreeformMode: " + r);
                    }
                    this.mStackSupervisor.moveTaskToStackLocked(r.task.taskId, 1, true, false, "exitFreeformMode", true);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void moveTaskToStack(int taskId, int stackId, boolean toTop) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "moveTaskToStack()");
        if (stackId == 0) {
            throw new IllegalArgumentException("moveTaskToStack: Attempt to move task " + taskId + " to home stack");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_STACK, "moveTaskToStack: moving task=" + taskId + " to stackId=" + stackId + " toTop=" + toTop);
                    }
                    if (stackId == 3) {
                        this.mWindowManager.setDockedStackCreateState(0, null);
                    }
                    boolean result = this.mStackSupervisor.moveTaskToStackLocked(taskId, stackId, toTop, false, "moveTaskToStack", true);
                    if (result && stackId == 3) {
                        this.mStackSupervisor.moveHomeStackTaskToTop(2, "moveTaskToDockedStack");
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void swapDockedAndFullscreenStack() throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "swapDockedAndFullscreenStack()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityStack fullscreenStack = this.mStackSupervisor.getStack(1);
                    TaskRecord taskRecord = fullscreenStack != null ? fullscreenStack.topTask() : null;
                    ActivityStack dockedStack = this.mStackSupervisor.getStack(3);
                    ArrayList<TaskRecord> tasks = dockedStack != null ? dockedStack.getAllTasks() : null;
                    if (taskRecord == null || tasks == null || tasks.size() == 0) {
                        Slog.w(TAG, "Unable to swap tasks, either docked or fullscreen stack is empty.");
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mWindowManager.prepareAppTransition(18, false);
                    this.mStackSupervisor.moveTaskToStackLocked(taskRecord.taskId, 3, false, false, "swapDockedAndFullscreenStack", true, true);
                    int size = tasks.size();
                    for (int i = 0; i < size; i++) {
                        int id = tasks.get(i).taskId;
                        if (id != taskRecord.taskId) {
                            this.mStackSupervisor.moveTaskToStackLocked(id, 1, true, false, "swapDockedAndFullscreenStack", true, true);
                        }
                    }
                    this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                    this.mWindowManager.executeAppTransition();
                    resetPriorityAfterLockedSection();
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean moveTaskToDockedStack(int taskId, int createMode, boolean toTop, boolean animate, Rect initialBounds, boolean moveHomeStackFront) {
        boolean moved;
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "moveTaskToDockedStack()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_STACK, "moveTaskToDockedStack: moving task=" + taskId + " to createMode=" + createMode + " toTop=" + toTop);
                    }
                    this.mWindowManager.setDockedStackCreateState(createMode, initialBounds);
                    moved = this.mStackSupervisor.moveTaskToStackLocked(taskId, 3, toTop, false, "moveTaskToDockedStack", animate, true);
                    if (moved) {
                        if (moveHomeStackFront) {
                            this.mStackSupervisor.moveHomeStackToFront("moveTaskToDockedStack");
                        }
                        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return moved;
    }

    public boolean moveTopActivityToPinnedStack(int stackId, Rect bounds) {
        boolean zMoveTopStackActivityToPinnedStackLocked;
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "moveTopActivityToPinnedStack()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!this.mSupportsPictureInPicture) {
                    throw new IllegalStateException("moveTopActivityToPinnedStack:Device doesn't support picture-in-pciture mode");
                }
                long ident = Binder.clearCallingIdentity();
                try {
                    zMoveTopStackActivityToPinnedStackLocked = this.mStackSupervisor.moveTopStackActivityToPinnedStackLocked(stackId, bounds);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zMoveTopStackActivityToPinnedStackLocked;
    }

    public void resizeStack(int stackId, Rect bounds, boolean allowResizeInDockedMode, boolean preserveWindows, boolean animate, int animationDuration) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "resizeStack()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (animate) {
                        if (stackId == 4) {
                            this.mWindowManager.animateResizePinnedStack(bounds, animationDuration);
                        } else {
                            throw new IllegalArgumentException("Stack: " + stackId + " doesn't support animated resize.");
                        }
                    } else {
                        this.mStackSupervisor.resizeStackLocked(stackId, bounds, null, null, preserveWindows, allowResizeInDockedMode, false);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void resizeDockedStack(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "resizeDockedStack()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    this.mStackSupervisor.resizeDockedStackLocked(dockedBounds, tempDockedTaskBounds, tempDockedTaskInsetBounds, tempOtherTaskBounds, tempOtherTaskInsetBounds, true);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void resizePinnedStack(Rect pinnedBounds, Rect tempPinnedTaskBounds) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "resizePinnedStack()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    this.mStackSupervisor.resizePinnedStackLocked(pinnedBounds, tempPinnedTaskBounds);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void positionTaskInStack(int taskId, int stackId, int position) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "positionTaskInStack()");
        if (stackId == 0) {
            throw new IllegalArgumentException("positionTaskInStack: Attempt to change the position of task " + taskId + " in/to home stack");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_STACK, "positionTaskInStack: positioning task=" + taskId + " in stackId=" + stackId + " at position=" + position);
                    }
                    this.mStackSupervisor.positionTaskInStackLocked(taskId, stackId, position);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public List<ActivityManager.StackInfo> getAllStackInfos() {
        ArrayList<ActivityManager.StackInfo> allStackInfosLocked;
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "getAllStackInfos()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    allStackInfosLocked = this.mStackSupervisor.getAllStackInfosLocked();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return allStackInfosLocked;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ActivityManager.StackInfo getStackInfo(int stackId) {
        ActivityManager.StackInfo stackInfoLocked;
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "getStackInfo()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    stackInfoLocked = this.mStackSupervisor.getStackInfoLocked(stackId);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return stackInfoLocked;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isInHomeStack(int taskId) {
        boolean zIsHomeStack = false;
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "getStackInfo()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(taskId, false, -1);
                    if (tr != null && tr.stack != null) {
                        zIsHomeStack = tr.stack.isHomeStack();
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return zIsHomeStack;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getTaskForActivity(IBinder token, boolean onlyRoot) {
        int taskForActivityLocked;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                taskForActivityLocked = ActivityRecord.getTaskForActivityLocked(token, onlyRoot);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return taskForActivityLocked;
    }

    public void updateDeviceOwner(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("updateDeviceOwner called from non-system process");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mDeviceOwnerName = packageName;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void updateLockTaskPackages(int userId, String[] packages) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            enforceCallingPermission("android.permission.UPDATE_LOCK_TASK_PACKAGES", "updateLockTaskPackages()");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.w(TAG_LOCKTASK, "Whitelisting " + userId + ":" + Arrays.toString(packages));
                }
                this.mLockTaskPackages.put(userId, packages);
                this.mStackSupervisor.onLockTaskPackagesUpdatedLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void startLockTaskModeLocked(TaskRecord task) {
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(TAG_LOCKTASK, "startLockTaskModeLocked: " + task);
        }
        if (task.mLockTaskAuth == 0) {
            return;
        }
        int callingUid = Binder.getCallingUid();
        boolean isSystemInitiated = callingUid == 1000;
        long ident = Binder.clearCallingIdentity();
        if (!isSystemInitiated) {
            try {
                task.mLockTaskUid = callingUid;
                if (task.mLockTaskAuth == 1) {
                    if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                        Slog.w(TAG_LOCKTASK, "Mode default, asking user");
                    }
                    StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                    if (statusBarManager != null) {
                        statusBarManager.showScreenPinningRequest(task.taskId);
                    }
                    return;
                }
                ActivityStack stack = this.mStackSupervisor.getFocusedStack();
                if (stack == null || task != stack.topTask()) {
                    throw new IllegalArgumentException("Invalid task, not in foreground");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(TAG_LOCKTASK, isSystemInitiated ? "Locking pinned" : "Locking fully");
        }
        this.mStackSupervisor.setLockTaskModeLocked(task, isSystemInitiated ? 2 : 1, "startLockTask", true);
    }

    public void startLockTaskMode(int taskId) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                TaskRecord task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                if (task != null) {
                    startLockTaskModeLocked(task);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void startLockTaskMode(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                TaskRecord task = r.task;
                if (task != null) {
                    startLockTaskModeLocked(task);
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void startSystemLockTaskMode(int taskId) throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "startSystemLockTaskMode");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    startLockTaskMode(taskId);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void stopLockTaskMode() {
        TaskRecord lockTask = this.mStackSupervisor.getLockedTaskLocked();
        if (lockTask == null) {
            return;
        }
        int callingUid = Binder.getCallingUid();
        int lockTaskUid = lockTask.mLockTaskUid;
        int lockTaskModeState = this.mStackSupervisor.getLockTaskModeState();
        if (lockTaskModeState == 0) {
            return;
        }
        if (checkCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS") != 0 && callingUid != lockTaskUid && (lockTaskUid != 0 || callingUid != lockTask.effectiveUid)) {
            throw new SecurityException("Invalid uid, expected " + lockTaskUid + " callingUid=" + callingUid + " effectiveUid=" + lockTask.effectiveUid);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            Log.d(TAG, "stopLockTaskMode");
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    this.mStackSupervisor.setLockTaskModeLocked(null, 0, "stopLockTask", true);
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            TelecomManager tm = (TelecomManager) this.mContext.getSystemService("telecom");
            if (tm != null) {
                tm.showInCallScreen(false);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void stopSystemLockTaskMode() throws RemoteException {
        if (this.mStackSupervisor.getLockTaskModeState() == 2) {
            stopLockTaskMode();
        } else {
            this.mStackSupervisor.showLockTaskToast();
        }
    }

    public boolean isInLockTaskMode() {
        return getLockTaskModeState() != 0;
    }

    public int getLockTaskModeState() {
        int lockTaskModeState;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                lockTaskModeState = this.mStackSupervisor.getLockTaskModeState();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return lockTaskModeState;
    }

    public void showLockTaskEscapeMessage(IBinder token) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                } else {
                    this.mStackSupervisor.showLockTaskEscapeMessageLocked(r.task);
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private final List<ProviderInfo> generateApplicationProvidersLocked(ProcessRecord app) {
        List<ProviderInfo> providers = null;
        try {
            providers = AppGlobals.getPackageManager().queryContentProviders(app.processName, app.uid, 268438528).getList();
        } catch (RemoteException e) {
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "generateApplicationProvidersLocked, app.info.uid = " + app.uid);
        }
        int userId = app.userId;
        if (providers != null) {
            int N = providers.size();
            app.pubProviders.ensureCapacity(app.pubProviders.size() + N);
            int i = 0;
            while (i < N) {
                ProviderInfo cpi = providers.get(i);
                boolean singleton = isSingleton(cpi.processName, cpi.applicationInfo, cpi.name, cpi.flags);
                if (singleton && UserHandle.getUserId(app.uid) != 0) {
                    providers.remove(i);
                    N--;
                    i--;
                } else {
                    ComponentName comp = new ComponentName(cpi.packageName, cpi.name);
                    ContentProviderRecord cpr = this.mProviderMap.getProviderByClass(comp, userId);
                    if (cpr == null) {
                        cpr = new ContentProviderRecord(this, cpi, app.info, comp, singleton);
                        this.mProviderMap.putProviderByClass(comp, cpr);
                    }
                    if (ActivityManagerDebugConfig.DEBUG_MU) {
                        Slog.v(TAG_MU, "generateApplicationProvidersLocked, cpi.uid = " + cpr.uid);
                    }
                    app.pubProviders.put(cpi.name, cpr);
                    if (!cpi.multiprocess || !"android".equals(cpi.packageName)) {
                        app.addPackage(cpi.applicationInfo.packageName, cpi.applicationInfo.versionCode, this.mProcessStats);
                    }
                    notifyPackageUse(cpi.applicationInfo.packageName, 4);
                }
                i++;
            }
        }
        return providers;
    }

    public String checkContentProviderAccess(String authority, int userId) {
        ProcessRecord r;
        String strCheckContentProviderPermissionLocked;
        if (userId == -1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", TAG);
            userId = UserHandle.getCallingUserId();
        }
        ProviderInfo cpi = null;
        try {
            cpi = AppGlobals.getPackageManager().resolveContentProvider(authority, 789504, userId);
        } catch (RemoteException e) {
        }
        if (cpi == null) {
            return null;
        }
        synchronized (this.mPidsSelfLocked) {
            r = this.mPidsSelfLocked.get(Binder.getCallingPid());
        }
        if (r == null) {
            return "Failed to find PID " + Binder.getCallingPid();
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                strCheckContentProviderPermissionLocked = checkContentProviderPermissionLocked(cpi, r, userId, true);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return strCheckContentProviderPermissionLocked;
    }

    private final String checkContentProviderPermissionLocked(ProviderInfo providerInfo, ProcessRecord processRecord, int i, boolean z) {
        String string;
        int callingPid = processRecord != null ? processRecord.pid : Binder.getCallingPid();
        int callingUid = processRecord != null ? processRecord.uid : Binder.getCallingUid();
        boolean z2 = false;
        if (z) {
            int iUnsafeConvertIncomingUserLocked = this.mUserController.unsafeConvertIncomingUserLocked(i);
            if (iUnsafeConvertIncomingUserLocked != UserHandle.getUserId(callingUid)) {
                if (checkAuthorityGrants(callingUid, providerInfo, iUnsafeConvertIncomingUserLocked, z)) {
                    return null;
                }
                z2 = true;
            }
            i = this.mUserController.handleIncomingUser(callingPid, callingUid, i, false, 0, "checkContentProviderPermissionLocked " + providerInfo.authority, null);
            if (i != iUnsafeConvertIncomingUserLocked) {
                z2 = false;
            }
        }
        if (checkComponentPermission(providerInfo.readPermission, callingPid, callingUid, providerInfo.applicationInfo.uid, providerInfo.exported) == 0) {
            return null;
        }
        if (checkComponentPermission(providerInfo.writePermission, callingPid, callingUid, providerInfo.applicationInfo.uid, providerInfo.exported) == 0) {
            return null;
        }
        PathPermission[] pathPermissionArr = providerInfo.pathPermissions;
        if (pathPermissionArr != null) {
            int length = pathPermissionArr.length;
            while (length > 0) {
                length--;
                PathPermission pathPermission = pathPermissionArr[length];
                String readPermission = pathPermission.getReadPermission();
                if (readPermission != null) {
                    if (checkComponentPermission(readPermission, callingPid, callingUid, providerInfo.applicationInfo.uid, providerInfo.exported) == 0) {
                        return null;
                    }
                }
                String writePermission = pathPermission.getWritePermission();
                if (writePermission != null) {
                    if (checkComponentPermission(writePermission, callingPid, callingUid, providerInfo.applicationInfo.uid, providerInfo.exported) == 0) {
                        return null;
                    }
                }
            }
        }
        if (!z2 && checkAuthorityGrants(callingUid, providerInfo, i, z)) {
            return null;
        }
        if (providerInfo.exported) {
            StringBuilder sbAppend = new StringBuilder().append("Permission Denial: opening provider ").append(providerInfo.name).append(" from ");
            Object obj = processRecord;
            if (processRecord == null) {
                obj = "(null)";
            }
            string = sbAppend.append(obj).append(" (pid=").append(callingPid).append(", uid=").append(callingUid).append(") requires ").append(providerInfo.readPermission).append(" or ").append(providerInfo.writePermission).toString();
        } else {
            StringBuilder sbAppend2 = new StringBuilder().append("Permission Denial: opening provider ").append(providerInfo.name).append(" from ");
            Object obj2 = processRecord;
            if (processRecord == null) {
                obj2 = "(null)";
            }
            string = sbAppend2.append(obj2).append(" (pid=").append(callingPid).append(", uid=").append(callingUid).append(") that is not exported from uid ").append(providerInfo.applicationInfo.uid).toString();
        }
        Slog.w(TAG, string);
        return string;
    }

    boolean checkAuthorityGrants(int callingUid, ProviderInfo cpi, int userId, boolean checkUser) {
        ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.get(callingUid);
        if (perms != null) {
            for (int i = perms.size() - 1; i >= 0; i--) {
                GrantUri grantUri = perms.keyAt(i);
                if ((grantUri.sourceUserId == userId || !checkUser) && matchesProvider(grantUri.uri, cpi)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean matchesProvider(Uri uri, ProviderInfo cpi) {
        String uriAuth = uri.getAuthority();
        String cpiAuth = cpi.authority;
        if (cpiAuth.indexOf(CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG) == -1) {
            return cpiAuth.equals(uriAuth);
        }
        String[] cpiAuths = cpiAuth.split(";");
        for (String str : cpiAuths) {
            if (str.equals(uriAuth)) {
                return true;
            }
        }
        return false;
    }

    ContentProviderConnection incProviderCountLocked(ProcessRecord r, ContentProviderRecord cpr, IBinder externalProcessToken, boolean stable) {
        if (r == null) {
            cpr.addExternalProcessHandleLocked(externalProcessToken);
            return null;
        }
        for (int i = 0; i < r.conProviders.size(); i++) {
            ContentProviderConnection conn = r.conProviders.get(i);
            if (conn.provider == cpr) {
                if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                    Slog.v(TAG_PROVIDER, "Adding provider requested by " + r.processName + " from process " + cpr.info.processName + ": " + cpr.name.flattenToShortString() + " scnt=" + conn.stableCount + " uscnt=" + conn.unstableCount);
                }
                if (stable) {
                    conn.stableCount++;
                    conn.numStableIncs++;
                } else {
                    conn.unstableCount++;
                    conn.numUnstableIncs++;
                }
                return conn;
            }
        }
        ContentProviderConnection conn2 = new ContentProviderConnection(cpr, r);
        if (stable) {
            conn2.stableCount = 1;
            conn2.numStableIncs = 1;
        } else {
            conn2.unstableCount = 1;
            conn2.numUnstableIncs = 1;
        }
        cpr.connections.add(conn2);
        r.conProviders.add(conn2);
        startAssociationLocked(r.uid, r.processName, r.curProcState, cpr.uid, cpr.name, cpr.info.processName);
        return conn2;
    }

    boolean decProviderCountLocked(ContentProviderConnection conn, ContentProviderRecord cpr, IBinder externalProcessToken, boolean stable) {
        if (conn == null) {
            cpr.removeExternalProcessHandleLocked(externalProcessToken);
            return false;
        }
        ContentProviderRecord cpr2 = conn.provider;
        if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
            Slog.v(TAG_PROVIDER, "Removing provider requested by " + conn.client.processName + " from process " + cpr2.info.processName + ": " + cpr2.name.flattenToShortString() + " scnt=" + conn.stableCount + " uscnt=" + conn.unstableCount);
        }
        if (stable) {
            conn.stableCount--;
        } else {
            conn.unstableCount--;
        }
        if (conn.stableCount != 0 || conn.unstableCount != 0) {
            return false;
        }
        cpr2.connections.remove(conn);
        conn.client.conProviders.remove(conn);
        if (conn.client.setProcState < 13 && cpr2.proc != null) {
            cpr2.proc.lastProviderTime = SystemClock.uptimeMillis();
        }
        stopAssociationLocked(conn.client.uid, conn.client.processName, cpr2.uid, cpr2.name);
        return true;
    }

    private void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime <= 50) {
            return;
        }
        Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
    }

    boolean isProcessAliveLocked(ProcessRecord proc) {
        if (proc.procStatFile == null) {
            proc.procStatFile = "/proc/" + proc.pid + "/stat";
        }
        this.mProcessStateStatsLongs[0] = 0;
        if (!Process.readProcFile(proc.procStatFile, PROCESS_STATE_STATS_FORMAT, null, this.mProcessStateStatsLongs, null)) {
            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.d(TAG, "UNABLE TO RETRIEVE STATE FOR " + proc.procStatFile);
            }
            return false;
        }
        long state = this.mProcessStateStatsLongs[0];
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            Slog.d(TAG, "RETRIEVED STATE FOR " + proc.procStatFile + ": " + ((char) state));
        }
        return (state == 90 || state == 88 || state == 120 || state == 75) ? false : true;
    }

    private IActivityManager.ContentProviderHolder getContentProviderImpl(IApplicationThread caller, String name, IBinder token, boolean stable, int userId) {
        int N;
        int i;
        long origId;
        ApplicationInfo ai;
        ContentProviderConnection conn = null;
        ProviderInfo cpi = null;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long startTime = SystemClock.uptimeMillis();
                ProcessRecord r = null;
                if (caller != null && (r = getRecordForAppLocked(caller)) == null) {
                    throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when getting content provider " + name);
                }
                boolean checkCrossUser = true;
                checkTime(startTime, "getContentProviderImpl: getProviderByName");
                ContentProviderRecord cpr = this.mProviderMap.getProviderByName(name, userId);
                if (cpr == null && userId != 0 && (cpr = this.mProviderMap.getProviderByName(name, 0)) != null) {
                    cpi = cpr.info;
                    if (isSingleton(cpi.processName, cpi.applicationInfo, cpi.name, cpi.flags) && isValidSingletonCall(r.uid, cpi.applicationInfo.uid)) {
                        userId = 0;
                        checkCrossUser = false;
                    } else {
                        cpr = null;
                        cpi = null;
                    }
                }
                boolean providerRunning = (cpr == null || cpr.proc == null) ? false : !cpr.proc.killed;
                if (providerRunning) {
                    cpi = cpr.info;
                    checkTime(startTime, "getContentProviderImpl: before checkContentProviderPermission");
                    String msg = checkContentProviderPermissionLocked(cpi, r, userId, checkCrossUser);
                    if (msg != null) {
                        throw new SecurityException(msg);
                    }
                    checkTime(startTime, "getContentProviderImpl: after checkContentProviderPermission");
                    if (r != null && cpr.canRunHere(r)) {
                        IActivityManager.ContentProviderHolder holder = cpr.newHolder(null);
                        holder.provider = null;
                        if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                            AMEventHookData.ReadyToGetProvider eventData = AMEventHookData.ReadyToGetProvider.createInstance();
                            ArrayList<String> callerPkgList = new ArrayList<>(r.pkgList.keySet());
                            eventData.set(new Object[]{cpi.applicationInfo.packageName, callerPkgList, Integer.valueOf(r.uid)});
                            this.mAMEventHook.hook(AMEventHook.Event.AM_ReadyToGetProvider, eventData);
                        }
                        if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                            Slog.i(TAG_PROVIDER, " cpr.canRunHere  " + holder);
                        }
                        resetPriorityAfterLockedSection();
                        return holder;
                    }
                    origId = Binder.clearCallingIdentity();
                    checkTime(startTime, "getContentProviderImpl: incProviderCountLocked");
                    conn = incProviderCountLocked(r, cpr, token, stable);
                    if (conn != null && conn.stableCount + conn.unstableCount == 1 && cpr.proc != null && r.setAdj <= FIRST_BROADCAST_QUEUE_MSG) {
                        checkTime(startTime, "getContentProviderImpl: before updateLruProcess");
                        updateLruProcessLocked(cpr.proc, false, null);
                        checkTime(startTime, "getContentProviderImpl: after updateLruProcess");
                    }
                    checkTime(startTime, "getContentProviderImpl: before updateOomAdj");
                    int verifiedAdj = cpr.proc.verifiedAdj;
                    boolean success = updateOomAdjLocked(cpr.proc);
                    if (success && verifiedAdj != cpr.proc.setAdj && !isProcessAliveLocked(cpr.proc)) {
                        success = false;
                    }
                    maybeUpdateProviderUsageStatsLocked(r, cpr.info.packageName, name);
                    checkTime(startTime, "getContentProviderImpl: after updateOomAdj");
                    if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                        Slog.i(TAG_PROVIDER, "Adjust success: " + success);
                    }
                    if (success) {
                        cpr.proc.verifiedAdj = cpr.proc.setAdj;
                    } else {
                        Slog.i(TAG, "Existing provider " + cpr.name.flattenToShortString() + " is crashing; detaching " + r);
                        boolean lastRef = decProviderCountLocked(conn, cpr, token, stable);
                        checkTime(startTime, "getContentProviderImpl: before appDied");
                        appDiedLocked(cpr.proc);
                        checkTime(startTime, "getContentProviderImpl: after appDied");
                        if (!lastRef) {
                            if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                                Slog.i(TAG_PROVIDER, " !lastRef return null");
                            }
                            resetPriorityAfterLockedSection();
                            return null;
                        }
                        providerRunning = false;
                        conn = null;
                    }
                    if (success) {
                        for (int provi = cpr.proc.conProviders.size() - 1; provi >= 0; provi--) {
                            ContentProviderConnection proviConn = cpr.proc.conProviders.get(provi);
                            if (proviConn.stableCount > 0 && proviConn.provider.proc != null && !proviConn.provider.proc.processName.equals("system")) {
                                Slog.e(TAG, "getContentProviderImpl: Update provider " + cpr.proc + " conProviers's adj. conProviders.provider.proc = " + proviConn.provider.proc + " stableCount = " + proviConn.stableCount);
                                updateOomAdjLocked(proviConn.provider.proc);
                            }
                        }
                    }
                }
                if (!providerRunning) {
                    try {
                        checkTime(startTime, "getContentProviderImpl: before resolveContentProvider");
                        cpi = AppGlobals.getPackageManager().resolveContentProvider(name, 3072, userId);
                        checkTime(startTime, "getContentProviderImpl: after resolveContentProvider");
                    } catch (RemoteException e) {
                    }
                    if (cpi == null) {
                        if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                            Slog.i(TAG_PROVIDER, " resolveContentProvider return null ");
                        }
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                    boolean zIsValidSingletonCall = isSingleton(cpi.processName, cpi.applicationInfo, cpi.name, cpi.flags) ? isValidSingletonCall(r.uid, cpi.applicationInfo.uid) : false;
                    if (zIsValidSingletonCall) {
                        userId = 0;
                    }
                    cpi.applicationInfo = getAppInfoForUser(cpi.applicationInfo, userId);
                    checkTime(startTime, "getContentProviderImpl: got app info for user");
                    checkTime(startTime, "getContentProviderImpl: before checkContentProviderPermission");
                    String msg2 = checkContentProviderPermissionLocked(cpi, r, userId, !zIsValidSingletonCall);
                    if (msg2 != null) {
                        throw new SecurityException(msg2);
                    }
                    checkTime(startTime, "getContentProviderImpl: after checkContentProviderPermission");
                    if (!this.mProcessesReady && !cpi.processName.equals("system")) {
                        throw new IllegalArgumentException("Attempt to launch content provider before system ready");
                    }
                    if (!this.mUserController.isUserRunningLocked(userId, 0)) {
                        Slog.w(TAG, "Unable to launch app " + cpi.applicationInfo.packageName + "/" + cpi.applicationInfo.uid + " for provider " + name + ": user " + userId + " is stopped");
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                    ComponentName comp = new ComponentName(cpi.packageName, cpi.name);
                    checkTime(startTime, "getContentProviderImpl: before getProviderByClass");
                    ContentProviderRecord cpr2 = this.mProviderMap.getProviderByClass(comp, userId);
                    checkTime(startTime, "getContentProviderImpl: after getProviderByClass");
                    boolean firstClass = cpr2 == null;
                    if (firstClass) {
                        origId = Binder.clearCallingIdentity();
                        if (Build.isPermissionReviewRequired() && !requestTargetProviderPermissionsReviewIfNeededLocked(cpi, r, userId)) {
                            if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                                Slog.i(TAG_PROVIDER, "!requestTargetProviderPermissionsReviewIfNeededLocked null");
                            }
                            resetPriorityAfterLockedSection();
                            return null;
                        }
                        try {
                            checkTime(startTime, "getContentProviderImpl: before getApplicationInfo");
                            ai = AppGlobals.getPackageManager().getApplicationInfo(cpi.applicationInfo.packageName, 1024, userId);
                            checkTime(startTime, "getContentProviderImpl: after getApplicationInfo");
                        } catch (RemoteException e2) {
                            cpr = cpr2;
                        } finally {
                        }
                        if (ai == null) {
                            Slog.w(TAG, "No package info for content provider " + cpi.name);
                            resetPriorityAfterLockedSection();
                            return null;
                        }
                        cpr = new ContentProviderRecord(this, cpi, getAppInfoForUser(ai, userId), comp, zIsValidSingletonCall);
                        checkTime(startTime, "getContentProviderImpl: now have ContentProviderRecord");
                        if (r == null && cpr.canRunHere(r)) {
                            if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                                AMEventHookData.ReadyToGetProvider eventData2 = AMEventHookData.ReadyToGetProvider.createInstance();
                                ArrayList<String> callerPkgList2 = new ArrayList<>(r.pkgList.keySet());
                                eventData2.set(new Object[]{cpi.applicationInfo.packageName, callerPkgList2, Integer.valueOf(r.uid)});
                                this.mAMEventHook.hook(AMEventHook.Event.AM_ReadyToGetProvider, eventData2);
                            }
                            if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                                Slog.i(TAG_PROVIDER, " this is a multiprocess provider " + cpr);
                            }
                            IActivityManager.ContentProviderHolder contentProviderHolderNewHolder = cpr.newHolder(null);
                            resetPriorityAfterLockedSection();
                            return contentProviderHolderNewHolder;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                            Slog.w(TAG_PROVIDER, "LAUNCHING REMOTE PROVIDER (myuid " + (r != null ? Integer.valueOf(r.uid) : null) + " pruid " + cpr.appInfo.uid + "): " + cpr.info.name + " callers=" + Debug.getCallers(6));
                        }
                        N = this.mLaunchingProviders.size();
                        i = 0;
                        while (i < N && this.mLaunchingProviders.get(i) != cpr) {
                            i++;
                        }
                        if (i >= N) {
                            int callerUid = Binder.getCallingUid();
                            origId = Binder.clearCallingIdentity();
                            try {
                                try {
                                    checkTime(startTime, "getContentProviderImpl: before set stopped state");
                                    AppGlobals.getPackageManager().setPackageStoppedState(cpr.appInfo.packageName, false, userId);
                                    checkTime(startTime, "getContentProviderImpl: after set stopped state");
                                } catch (RemoteException e3) {
                                } catch (IllegalArgumentException e4) {
                                    Slog.w(TAG, "Failed trying to unstop package " + cpr.appInfo.packageName + ": " + e4);
                                }
                                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                                    AMEventHookData.PackageStoppedStatusChanged eventData1 = AMEventHookData.PackageStoppedStatusChanged.createInstance();
                                    eventData1.set(new Object[]{cpr.appInfo.packageName, 0, "getContentProviderImpl"});
                                    this.mAMEventHook.hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData1);
                                }
                                checkTime(startTime, "getContentProviderImpl: looking for process record");
                                ProcessRecord proc = getProcessRecordLocked(cpi.processName, cpr.appInfo.uid, false);
                                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                                    AMEventHookData.ReadyToGetProvider eventData3 = AMEventHookData.ReadyToGetProvider.createInstance();
                                    if (r != null) {
                                        ArrayList<String> callerPkgList3 = new ArrayList<>(r.pkgList.keySet());
                                        eventData3.set(new Object[]{cpi.applicationInfo.packageName, callerPkgList3, Integer.valueOf(r.uid)});
                                    } else {
                                        eventData3.set(new Object[]{cpi.applicationInfo.packageName, null, Integer.valueOf(callerUid)});
                                    }
                                    this.mAMEventHook.hook(AMEventHook.Event.AM_ReadyToGetProvider, eventData3);
                                }
                                if (proc == null || proc.thread == null || proc.killed) {
                                    checkTime(startTime, "getContentProviderImpl: before start process");
                                    proc = startProcessLocked(cpi.processName, cpr.appInfo, false, 0, "content provider", new ComponentName(cpi.applicationInfo.packageName, cpi.name), false, false, false);
                                    checkTime(startTime, "getContentProviderImpl: after start process");
                                    if (proc == null) {
                                        Slog.w(TAG, "Unable to launch app " + cpi.applicationInfo.packageName + "/" + cpi.applicationInfo.uid + " for provider " + name + ": process is bad");
                                        resetPriorityAfterLockedSection();
                                        return null;
                                    }
                                } else {
                                    if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                                        Slog.d(TAG_PROVIDER, "Installing in existing process " + proc);
                                    }
                                    if (!proc.pubProviders.containsKey(cpi.name)) {
                                        checkTime(startTime, "getContentProviderImpl: scheduling install");
                                        proc.pubProviders.put(cpi.name, cpr);
                                        try {
                                            proc.thread.scheduleInstallProvider(cpi);
                                        } catch (RemoteException e5) {
                                        }
                                    }
                                }
                                cpr.launchingApp = proc;
                                this.mLaunchingProviders.add(cpr);
                            } finally {
                            }
                        }
                        checkTime(startTime, "getContentProviderImpl: updating data structures");
                        if (firstClass) {
                            this.mProviderMap.putProviderByClass(comp, cpr);
                        }
                        this.mProviderMap.putProviderByName(name, cpr);
                        conn = incProviderCountLocked(r, cpr, token, stable);
                        if (conn != null) {
                            conn.waiting = true;
                        }
                    } else {
                        cpr = cpr2;
                        checkTime(startTime, "getContentProviderImpl: now have ContentProviderRecord");
                        if (r == null) {
                        }
                        if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                        }
                        N = this.mLaunchingProviders.size();
                        i = 0;
                        while (i < N) {
                            i++;
                        }
                        if (i >= N) {
                        }
                        checkTime(startTime, "getContentProviderImpl: updating data structures");
                        if (firstClass) {
                        }
                        this.mProviderMap.putProviderByName(name, cpr);
                        conn = incProviderCountLocked(r, cpr, token, stable);
                        if (conn != null) {
                        }
                    }
                }
                checkTime(startTime, "getContentProviderImpl: done!");
                resetPriorityAfterLockedSection();
                synchronized (cpr) {
                    while (cpr.provider == null) {
                        if (cpr.launchingApp == null) {
                            Slog.w(TAG, "Unable to launch app " + cpi.applicationInfo.packageName + "/" + cpi.applicationInfo.uid + " for provider " + name + ": launching app became null");
                            EventLog.writeEvent(EventLogTags.AM_PROVIDER_LOST_PROCESS, Integer.valueOf(UserHandle.getUserId(cpi.applicationInfo.uid)), cpi.applicationInfo.packageName, Integer.valueOf(cpi.applicationInfo.uid), name);
                            return null;
                        }
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_MU) {
                                Slog.v(TAG_MU, "Waiting to start provider " + cpr + " launchingApp=" + cpr.launchingApp);
                            }
                            if (conn != null) {
                                conn.waiting = true;
                            }
                            cpr.wait();
                            if (conn != null) {
                                conn.waiting = false;
                            }
                        } catch (InterruptedException e6) {
                            if (conn != null) {
                                conn.waiting = false;
                            }
                        } catch (Throwable th) {
                            if (conn != null) {
                                conn.waiting = false;
                            }
                            throw th;
                        }
                    }
                    if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                        Slog.i(TAG_PROVIDER, " end return " + cpr);
                    }
                    if (cpr != null) {
                        return cpr.newHolder(conn);
                    }
                    return null;
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
    }

    private boolean requestTargetProviderPermissionsReviewIfNeededLocked(ProviderInfo cpi, ProcessRecord r, int userId) {
        boolean callerForeground = true;
        if (!getPackageManagerInternalLocked().isPermissionsReviewRequired(cpi.packageName, userId)) {
            return true;
        }
        if (r != null && r.setSchedGroup == 0) {
            callerForeground = false;
        }
        if (!callerForeground) {
            Slog.w(TAG, "u" + userId + " Instantiating a provider in package" + cpi.packageName + " requires a permissions review");
            return false;
        }
        final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
        intent.addFlags(276824064);
        intent.putExtra("android.intent.extra.PACKAGE_NAME", cpi.packageName);
        if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW || !IS_USER_BUILD) {
            Slog.i(TAG, "u" + userId + " Launching permission review for package " + cpi.packageName);
        }
        final UserHandle userHandle = new UserHandle(userId);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ActivityManagerService.this.mContext.startActivityAsUser(intent, userHandle);
            }
        });
        return false;
    }

    PackageManagerInternal getPackageManagerInternalLocked() {
        if (this.mPackageManagerInt == null) {
            this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPackageManagerInt;
    }

    public final IActivityManager.ContentProviderHolder getContentProvider(IApplicationThread caller, String name, int userId, boolean stable) {
        enforceNotIsolatedCaller("getContentProvider");
        if (caller == null) {
            String msg = "null IApplicationThread when getting content provider " + name;
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        return getContentProviderImpl(caller, name, null, stable, userId);
    }

    public IActivityManager.ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token) {
        enforceCallingPermission("android.permission.ACCESS_CONTENT_PROVIDERS_EXTERNALLY", "Do not have permission in call getContentProviderExternal()");
        return getContentProviderExternalUnchecked(name, token, this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "getContentProvider", null));
    }

    private IActivityManager.ContentProviderHolder getContentProviderExternalUnchecked(String name, IBinder token, int userId) {
        return getContentProviderImpl(null, name, token, true, userId);
    }

    public void removeContentProvider(IBinder connection, boolean stable) {
        enforceNotIsolatedCaller("removeContentProvider");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    try {
                        ContentProviderConnection conn = (ContentProviderConnection) connection;
                        if (conn == null) {
                            throw new NullPointerException("connection is null");
                        }
                        if (decProviderCountLocked(conn, null, null, stable)) {
                            updateOomAdjLocked();
                        }
                    } catch (ClassCastException e) {
                        String msg = "removeContentProvider: " + connection + " not a ContentProviderConnection";
                        Slog.w(TAG, msg);
                        throw new IllegalArgumentException(msg);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void removeContentProviderExternal(String name, IBinder token) {
        enforceCallingPermission("android.permission.ACCESS_CONTENT_PROVIDERS_EXTERNALLY", "Do not have permission in call removeContentProviderExternal()");
        int userId = UserHandle.getCallingUserId();
        long ident = Binder.clearCallingIdentity();
        try {
            removeContentProviderExternalUnchecked(name, token, userId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void removeContentProviderExternalUnchecked(String name, IBinder token, int userId) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ContentProviderRecord cpr = this.mProviderMap.getProviderByName(name, userId);
                if (cpr == null) {
                    if (ActivityManagerDebugConfig.DEBUG_ALL) {
                        Slog.v(TAG, name + " content provider not found in providers list");
                    }
                    return;
                }
                ComponentName comp = new ComponentName(cpr.info.packageName, cpr.info.name);
                ContentProviderRecord localCpr = this.mProviderMap.getProviderByClass(comp, userId);
                if (!localCpr.hasExternalProcessHandles()) {
                    Slog.e(TAG, "Attmpt to remove content provider: " + localCpr + " with no external references.");
                } else if (localCpr.removeExternalProcessHandleLocked(token)) {
                    updateOomAdjLocked();
                } else {
                    Slog.e(TAG, "Attmpt to remove content provider " + localCpr + " with no external reference for token: " + token + ".");
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public final void publishContentProviders(IApplicationThread caller, List<IActivityManager.ContentProviderHolder> providers) {
        if (providers == null) {
            return;
        }
        enforceNotIsolatedCaller("publishContentProviders");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord r = getRecordForAppLocked(caller);
                if (ActivityManagerDebugConfig.DEBUG_MU && r != null) {
                    Slog.v(TAG_MU, "ProcessRecord uid = " + r.uid);
                }
                if (r == null) {
                    throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when publishing content providers");
                }
                long origId = Binder.clearCallingIdentity();
                int N = providers.size();
                for (int i = 0; i < N; i++) {
                    IActivityManager.ContentProviderHolder src = providers.get(i);
                    if (src != null && src.info != null && src.provider != null) {
                        ContentProviderRecord dst = r.pubProviders.get(src.info.name);
                        if (ActivityManagerDebugConfig.DEBUG_MU) {
                            if (dst != null) {
                                Slog.v(TAG_MU, "ContentProviderRecord uid = " + dst.uid);
                            } else {
                                Slog.v(TAG_MU, "ContentProviderRecord dst == null");
                            }
                        }
                        if (dst != null) {
                            ComponentName comp = new ComponentName(dst.info.packageName, dst.info.name);
                            this.mProviderMap.putProviderByClass(comp, dst);
                            String[] names = dst.info.authority.split(";");
                            for (String str : names) {
                                this.mProviderMap.putProviderByName(str, dst);
                            }
                            int launchingCount = this.mLaunchingProviders.size();
                            boolean wasInLaunchingProviders = false;
                            int j = 0;
                            while (j < launchingCount) {
                                if (this.mLaunchingProviders.get(j) == dst) {
                                    this.mLaunchingProviders.remove(j);
                                    wasInLaunchingProviders = true;
                                    j--;
                                    launchingCount--;
                                }
                                j++;
                            }
                            if (wasInLaunchingProviders) {
                                this.mHandler.removeMessages(CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG, r);
                            }
                            synchronized (dst) {
                                dst.provider = src.provider;
                                dst.proc = r;
                                dst.notifyAll();
                            }
                            updateOomAdjLocked(r);
                            maybeUpdateProviderUsageStatsLocked(r, src.info.packageName, src.info.authority);
                        } else {
                            continue;
                        }
                    }
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean refContentProvider(IBinder connection, int stable, int unstable) {
        boolean z;
        try {
            ContentProviderConnection conn = (ContentProviderConnection) connection;
            if (conn == null) {
                throw new NullPointerException("connection is null");
            }
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (stable > 0) {
                        conn.numStableIncs += stable;
                    }
                    int stable2 = stable + conn.stableCount;
                    if (stable2 < 0) {
                        throw new IllegalStateException("stableCount < 0: " + stable2);
                    }
                    if (unstable > 0) {
                        conn.numUnstableIncs += unstable;
                    }
                    int unstable2 = unstable + conn.unstableCount;
                    if (unstable2 < 0) {
                        throw new IllegalStateException("unstableCount < 0: " + unstable2);
                    }
                    if (stable2 + unstable2 <= 0) {
                        throw new IllegalStateException("ref counts can't go to zero here: stable=" + stable2 + " unstable=" + unstable2);
                    }
                    conn.stableCount = stable2;
                    conn.unstableCount = unstable2;
                    z = conn.dead ? false : true;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            return z;
        } catch (ClassCastException e) {
            String msg = "refContentProvider: " + connection + " not a ContentProviderConnection";
            Slog.w(TAG, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    public void unstableProviderDied(IBinder connection) {
        IContentProvider provider;
        try {
            ContentProviderConnection conn = (ContentProviderConnection) connection;
            if (conn == null) {
                throw new NullPointerException("connection is null");
            }
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    provider = conn.provider.provider;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            if (provider == null) {
                return;
            }
            if (provider.asBinder().pingBinder()) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        Slog.w(TAG, "unstableProviderDied: caller " + Binder.getCallingUid() + " says " + conn + " died, but we don't agree");
                    } catch (Throwable th2) {
                        resetPriorityAfterLockedSection();
                        throw th2;
                    }
                }
                resetPriorityAfterLockedSection();
                return;
            }
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (conn.provider.provider != provider) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    ProcessRecord proc = conn.provider.proc;
                    if (proc == null || proc.thread == null) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    Slog.i(TAG, "Process " + proc.processName + " (pid " + proc.pid + ") early provider death");
                    long ident = Binder.clearCallingIdentity();
                    try {
                        appDiedLocked(proc);
                        resetPriorityAfterLockedSection();
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                } catch (Throwable th3) {
                    resetPriorityAfterLockedSection();
                    throw th3;
                }
            }
        } catch (ClassCastException e) {
            String msg = "refContentProvider: " + connection + " not a ContentProviderConnection";
            Slog.w(TAG, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    public void appNotRespondingViaProvider(IBinder connection) {
        enforceCallingPermission("android.permission.REMOVE_TASKS", "appNotRespondingViaProvider()");
        ContentProviderConnection conn = (ContentProviderConnection) connection;
        if (conn == null) {
            Slog.w(TAG, "ContentProviderConnection is null");
            return;
        }
        final ProcessRecord host = conn.provider.proc;
        if (host == null) {
            Slog.w(TAG, "Failed to find hosting ProcessRecord");
        } else {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ActivityManagerService.this.mAppErrors.appNotResponding(host, null, null, false, "ContentProvider not responding");
                }
            });
        }
    }

    public final void installSystemProviders() {
        List<ProviderInfo> providers;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord app = (ProcessRecord) this.mProcessNames.get("system", 1000);
                providers = generateApplicationProvidersLocked(app);
                if (providers != null) {
                    for (int i = providers.size() - 1; i >= 0; i--) {
                        ProviderInfo pi = providers.get(i);
                        if ((pi.applicationInfo.flags & 1) == 0) {
                            Slog.w(TAG, "Not installing system proc provider " + pi.name + ": not system .apk");
                            providers.remove(i);
                        }
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (providers != null) {
            this.mSystemThread.installSystemProviders(providers);
        }
        this.mCoreSettingsObserver = new CoreSettingsObserver(this);
        this.mFontScaleSettingObserver = new FontScaleSettingObserver();
    }

    private void startPersistentApps(int matchFlags) {
        if (this.mFactoryTest == 1) {
            return;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                try {
                    List<ApplicationInfo> apps = AppGlobals.getPackageManager().getPersistentApplications(matchFlags | 1024).getList();
                    for (ApplicationInfo app : apps) {
                        if (!"android".equals(app.packageName)) {
                            addAppLocked(app, false, null);
                        }
                    }
                } catch (RemoteException e) {
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void installEncryptionUnawareProviders(int userId) {
        boolean z;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int NP = this.mProcessNames.getMap().size();
                for (int ip = 0; ip < NP; ip++) {
                    SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                    int NA = apps.size();
                    for (int ia = 0; ia < NA; ia++) {
                        ProcessRecord app = apps.valueAt(ia);
                        if (app.userId == userId && app.thread != null && !app.unlocked) {
                            int NG = app.pkgList.size();
                            for (int ig = 0; ig < NG; ig++) {
                                try {
                                    String pkgName = app.pkgList.keyAt(ig);
                                    PackageInfo pkgInfo = AppGlobals.getPackageManager().getPackageInfo(pkgName, 262152, userId);
                                    if (pkgInfo != null && !ArrayUtils.isEmpty(pkgInfo.providers)) {
                                        for (ProviderInfo pi : pkgInfo.providers) {
                                            if (Objects.equals(pi.processName, app.processName)) {
                                                z = true;
                                            } else {
                                                z = pi.multiprocess;
                                            }
                                            boolean userMatch = !isSingleton(pi.processName, pi.applicationInfo, pi.name, pi.flags) || app.userId == 0;
                                            if (z && userMatch) {
                                                Log.v(TAG, "Installing " + pi);
                                                app.thread.scheduleInstallProvider(pi);
                                            } else {
                                                Log.v(TAG, "Skipping " + pi);
                                            }
                                        }
                                    }
                                } catch (RemoteException e) {
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public String getProviderMimeType(Uri uri, int userId) {
        int userId2;
        enforceNotIsolatedCaller("getProviderMimeType");
        String name = uri.getAuthority();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long ident = 0;
        boolean clearedIdentity = false;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                userId2 = this.mUserController.unsafeConvertIncomingUserLocked(userId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (canClearIdentity(callingPid, callingUid, userId2)) {
            clearedIdentity = true;
            ident = Binder.clearCallingIdentity();
        }
        IActivityManager.ContentProviderHolder holder = null;
        try {
            try {
                holder = getContentProviderExternalUnchecked(name, null, userId2);
                if (holder == null) {
                    if (!clearedIdentity) {
                        ident = Binder.clearCallingIdentity();
                    }
                    if (holder != null) {
                        try {
                            removeContentProviderExternalUnchecked(name, null, userId2);
                        } finally {
                        }
                    }
                    return null;
                }
                String type = holder.provider.getType(uri);
                if (!clearedIdentity) {
                    ident = Binder.clearCallingIdentity();
                }
                if (holder != null) {
                    try {
                        removeContentProviderExternalUnchecked(name, null, userId2);
                    } finally {
                    }
                }
                return type;
            } catch (Throwable th2) {
                if (!clearedIdentity) {
                    ident = Binder.clearCallingIdentity();
                }
                if (holder != null) {
                    try {
                        removeContentProviderExternalUnchecked(name, null, userId2);
                    } finally {
                    }
                }
                throw th2;
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Content provider dead retrieving " + uri, e);
            if (!clearedIdentity) {
                ident = Binder.clearCallingIdentity();
            }
            if (holder != null) {
                try {
                    removeContentProviderExternalUnchecked(name, null, userId2);
                } finally {
                }
            }
            return null;
        } catch (Exception e2) {
            Log.w(TAG, "Exception while determining type of " + uri, e2);
            if (!clearedIdentity) {
                ident = Binder.clearCallingIdentity();
            }
            if (holder != null) {
                try {
                    removeContentProviderExternalUnchecked(name, null, userId2);
                } finally {
                }
            }
            return null;
        }
    }

    private boolean canClearIdentity(int callingPid, int callingUid, int userId) {
        return UserHandle.getUserId(callingUid) == userId || checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingPid, callingUid, -1, true) == 0 || checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, callingUid, -1, true) == 0;
    }

    final ProcessRecord newProcessRecordLocked(ApplicationInfo info, String customProcess, boolean isolated, int isolatedUid) {
        String proc = customProcess != null ? customProcess : info.processName;
        BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
        int userId = UserHandle.getUserId(info.uid);
        int uid = info.uid;
        if (isolated) {
            if (isolatedUid == 0) {
                int stepsLeft = 1000;
                do {
                    if (this.mNextIsolatedProcessUid < 99000 || this.mNextIsolatedProcessUid > 99999) {
                        this.mNextIsolatedProcessUid = 99000;
                    }
                    uid = UserHandle.getUid(userId, this.mNextIsolatedProcessUid);
                    this.mNextIsolatedProcessUid++;
                    if (this.mIsolatedProcesses.indexOfKey(uid) >= 0) {
                        stepsLeft--;
                    }
                } while (stepsLeft > 0);
                return null;
            }
            uid = isolatedUid;
        }
        ProcessRecord r = new ProcessRecord(stats, info, proc, uid);
        if (!this.mBooted && !this.mBooting && userId == 0 && (info.flags & 9) == 9) {
            r.persistent = true;
        }
        addProcessNameLocked(r);
        return r;
    }

    final ProcessRecord addAppLocked(ApplicationInfo info, boolean isolated, String abiOverride) {
        ProcessRecord app;
        if (!isolated) {
            app = getProcessRecordLocked(info.processName, info.uid, true);
        } else {
            app = null;
        }
        if (app == null) {
            app = newProcessRecordLocked(info, null, isolated, 0);
            updateLruProcessLocked(app, false, null);
            updateOomAdjLocked();
        }
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(info.packageName, false, UserHandle.getUserId(app.uid));
        } catch (RemoteException e) {
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Failed trying to unstop package " + info.packageName + ": " + e2);
        }
        if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
            AMEventHookData.PackageStoppedStatusChanged eventData = AMEventHookData.PackageStoppedStatusChanged.createInstance();
            eventData.set(new Object[]{info.packageName, 0, "addAppLocked"});
            this.mAMEventHook.hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData);
        }
        if ((info.flags & 9) == 9) {
            app.persistent = true;
            app.maxAdj = -800;
        }
        if (app.thread == null && this.mPersistentStartingProcesses.indexOf(app) < 0) {
            this.mPersistentStartingProcesses.add(app);
            startProcessLocked(app, "added application", app.processName, abiOverride, null, null);
        }
        return app;
    }

    public void unhandledBack() {
        enforceCallingPermission("android.permission.FORCE_BACK", "unhandledBack()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    getFocusedStack().unhandledBackLocked();
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public ParcelFileDescriptor openContentUri(Uri uri) throws RemoteException {
        enforceNotIsolatedCaller("openContentUri");
        int userId = UserHandle.getCallingUserId();
        String name = uri.getAuthority();
        IActivityManager.ContentProviderHolder cph = getContentProviderExternalUnchecked(name, null, userId);
        ParcelFileDescriptor pfd = null;
        if (cph != null) {
            Binder token = new Binder();
            sCallerIdentity.set(new Identity(token, Binder.getCallingPid(), Binder.getCallingUid()));
            try {
                pfd = cph.provider.openFile((String) null, uri, "r", (ICancellationSignal) null, token);
            } catch (FileNotFoundException e) {
            } finally {
                sCallerIdentity.remove();
                removeContentProviderExternalUnchecked(name, null, userId);
            }
        } else {
            Slog.d(TAG, "Failed to get provider for authority '" + name + "'");
        }
        return pfd;
    }

    boolean isSleepingOrShuttingDownLocked() {
        if (isSleepingLocked()) {
            return true;
        }
        return this.mShuttingDown;
    }

    boolean isShuttingDownLocked() {
        return this.mShuttingDown;
    }

    boolean isSleepingLocked() {
        return this.mSleeping;
    }

    void onWakefulnessChanged(int wakefulness) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                    AMEventHookData.WakefulnessChanged eventData = AMEventHookData.WakefulnessChanged.createInstance();
                    eventData.set(new Object[]{Integer.valueOf(wakefulness)});
                    this.mAMEventHook.hook(AMEventHook.Event.AM_WakefulnessChanged, eventData);
                }
                this.mWakefulness = wakefulness;
                updateSleepIfNeededLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void finishRunningVoiceLocked() {
        if (this.mRunningVoice == null) {
            return;
        }
        this.mRunningVoice = null;
        this.mVoiceWakeLock.release();
        updateSleepIfNeededLocked();
    }

    void startTimeTrackingFocusedActivityLocked() {
        if (this.mSleeping || this.mCurAppTimeTracker == null || this.mFocusedActivity == null) {
            return;
        }
        this.mCurAppTimeTracker.start(this.mFocusedActivity.packageName);
    }

    void updateSleepIfNeededLocked() {
        boolean sleeping = this.mSleeping;
        if (this.mSleeping && !shouldSleepLocked()) {
            this.mSleeping = false;
            startTimeTrackingFocusedActivityLocked();
            this.mTopProcessState = 2;
            this.mStackSupervisor.comeOutOfSleepIfNeededLocked();
            updateOomAdjLocked();
        } else if (!this.mSleeping && shouldSleepLocked()) {
            this.mSleeping = true;
            if (this.mCurAppTimeTracker != null) {
                this.mCurAppTimeTracker.stop();
            }
            this.mTopProcessState = 5;
            this.mStackSupervisor.goingToSleepLocked();
            updateOomAdjLocked();
            checkExcessivePowerUsageLocked(false);
            this.mHandler.removeMessages(27);
            Message nmsg = this.mHandler.obtainMessage(27);
            this.mHandler.sendMessageDelayed(nmsg, POWER_CHECK_DELAY);
        }
        AMEventHookData.UpdateSleep eventData = AMEventHookData.UpdateSleep.createInstance();
        eventData.set(new Object[]{Boolean.valueOf(sleeping), Boolean.valueOf(this.mSleeping)});
        this.mAMEventHook.hook(AMEventHook.Event.AM_UpdateSleep, eventData);
    }

    private boolean shouldSleepLocked() {
        if (this.mRunningVoice != null) {
            return false;
        }
        switch (this.mWakefulness) {
            case 1:
            case 2:
            case 3:
                return (this.mLockScreenShown == 0 && this.mSleepTokens.isEmpty()) ? false : true;
            default:
                return true;
        }
    }

    void notifyTaskPersisterLocked(TaskRecord task, boolean flush) {
        this.mRecentTasks.notifyTaskPersisterLocked(task, flush);
    }

    void notifyTaskStackChangedLocked() {
        this.mHandler.sendEmptyMessage(LOG_STACK_STATE);
        this.mHandler.removeMessages(49);
        Message nmsg = this.mHandler.obtainMessage(49);
        this.mHandler.sendMessageDelayed(nmsg, 100L);
    }

    void notifyActivityPinnedLocked() {
        this.mHandler.removeMessages(64);
        this.mHandler.obtainMessage(64).sendToTarget();
    }

    void notifyPinnedActivityRestartAttemptLocked() {
        this.mHandler.removeMessages(65);
        this.mHandler.obtainMessage(65).sendToTarget();
    }

    public void notifyPinnedStackAnimationEnded() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mHandler.removeMessages(66);
                this.mHandler.obtainMessage(66).sendToTarget();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyCleartextNetwork(int uid, byte[] firstPacket) {
        this.mHandler.obtainMessage(50, uid, 0, firstPacket).sendToTarget();
    }

    public boolean shutdown(int timeout) throws Throwable {
        boolean timedout;
        if (checkCallingPermission("android.permission.SHUTDOWN") != 0) {
            throw new SecurityException("Requires permission android.permission.SHUTDOWN");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mShuttingDown = true;
                updateEventDispatchingLocked();
                timedout = this.mStackSupervisor.shutdownLocked(timeout);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        this.mAppOpsService.shutdown();
        if (this.mUsageStatsService != null) {
            this.mUsageStatsService.prepareShutdown();
        }
        this.mBatteryStatsService.shutdown();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mProcessStats.shutdownLocked();
                notifyTaskPersisterLocked(null, true);
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
        return timedout;
    }

    public final void activitySlept(IBinder token) {
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Activity slept: token=" + token);
        }
        long origId = Binder.clearCallingIdentity();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r != null) {
                    this.mStackSupervisor.activitySleptLocked(r);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    private String lockScreenShownToString() {
        switch (this.mLockScreenShown) {
            case 0:
                return "LOCK_SCREEN_HIDDEN";
            case 1:
                return "LOCK_SCREEN_LEAVING";
            case 2:
                return "LOCK_SCREEN_SHOWN";
            default:
                return "Unknown=" + this.mLockScreenShown;
        }
    }

    void logLockScreen(String msg) {
        if (ActivityManagerDebugConfig.DEBUG_LOCKSCREEN) {
            Slog.d(TAG_LOCKSCREEN, Debug.getCallers(2) + ":" + msg + " mLockScreenShown=" + lockScreenShownToString() + " mWakefulness=" + PowerManagerInternal.wakefulnessToString(this.mWakefulness) + " mSleeping=" + this.mSleeping);
        }
    }

    void startRunningVoiceLocked(IVoiceInteractionSession session, int targetUid) {
        Slog.d(TAG, "<<<  startRunningVoiceLocked()");
        this.mVoiceWakeLock.setWorkSource(new WorkSource(targetUid));
        if (this.mRunningVoice != null && this.mRunningVoice.asBinder() == session.asBinder()) {
            return;
        }
        boolean wasRunningVoice = this.mRunningVoice != null;
        this.mRunningVoice = session;
        if (wasRunningVoice) {
            return;
        }
        this.mVoiceWakeLock.acquire();
        updateSleepIfNeededLocked();
    }

    private void updateEventDispatchingLocked() {
        boolean z = false;
        WindowManagerService windowManagerService = this.mWindowManager;
        if (this.mBooted && !this.mShuttingDown) {
            z = true;
        }
        windowManagerService.setEventDispatching(z);
    }

    public void setLockScreenShown(boolean showing, boolean occluded) {
        if (checkCallingPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("Requires permission android.permission.DEVICE_POWER");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    if (ActivityManagerDebugConfig.DEBUG_LOCKSCREEN) {
                        logLockScreen(" showing=" + showing + " occluded=" + occluded);
                    }
                    this.mLockScreenShown = (!showing || occluded) ? 0 : 2;
                    if (showing && occluded) {
                        this.mStackSupervisor.moveTasksToFullscreenStackLocked(3, this.mStackSupervisor.mFocusedStack.getStackId() == 3);
                    }
                    updateSleepIfNeededLocked();
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyLockedProfile(int userId) {
        try {
            if (!AppGlobals.getPackageManager().isUidPrivileged(Binder.getCallingUid())) {
                throw new SecurityException("Only privileged app can call notifyLockedProfile");
            }
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (this.mStackSupervisor.isUserLockedProfile(userId)) {
                        long ident = Binder.clearCallingIdentity();
                        try {
                            int currentUserId = this.mUserController.getCurrentUserIdLocked();
                            this.mStackSupervisor.moveProfileTasksFromFreeformToFullscreenStackLocked(userId);
                            if (this.mUserController.isLockScreenDisabled(currentUserId)) {
                                this.mActivityStarter.showConfirmDeviceCredential(userId);
                            } else {
                                startHomeActivityLocked(currentUserId, "notifyLockedProfile");
                            }
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } catch (RemoteException ex) {
            throw new SecurityException("Fail to check is caller a privileged app", ex);
        }
    }

    public void startConfirmDeviceCredentialIntent(Intent intent) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "startConfirmDeviceCredentialIntent");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    this.mActivityStarter.startConfirmCredentialIntent(intent);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void stopAppSwitches() {
        if (checkCallingPermission("android.permission.STOP_APP_SWITCHES") != 0) {
            throw new SecurityException("viewquires permission android.permission.STOP_APP_SWITCHES");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mAppSwitchesAllowedTime = SystemClock.uptimeMillis() + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
                this.mDidAppSwitch = false;
                this.mHandler.removeMessages(21);
                Message msg = this.mHandler.obtainMessage(21);
                this.mHandler.sendMessageDelayed(msg, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void resumeAppSwitches() {
        if (checkCallingPermission("android.permission.STOP_APP_SWITCHES") != 0) {
            throw new SecurityException("Requires permission android.permission.STOP_APP_SWITCHES");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mAppSwitchesAllowedTime = 0L;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    boolean checkAppSwitchAllowedLocked(int sourcePid, int sourceUid, int callingPid, int callingUid, String name) {
        if (this.mAppSwitchesAllowedTime < SystemClock.uptimeMillis()) {
            return true;
        }
        int perm = checkComponentPermission("android.permission.STOP_APP_SWITCHES", sourcePid, sourceUid, -1, true);
        if (perm == 0) {
            return true;
        }
        if (callingUid != -1 && callingUid != sourceUid) {
            int perm2 = checkComponentPermission("android.permission.STOP_APP_SWITCHES", callingPid, callingUid, -1, true);
            if (perm2 == 0) {
                return true;
            }
        }
        Slog.w(TAG, name + " request from " + sourceUid + " stopped");
        return false;
    }

    public void setDebugApp(String packageName, boolean waitForDebugger, boolean persistent) {
        enforceCallingPermission("android.permission.SET_DEBUG_APP", "setDebugApp()");
        long ident = Binder.clearCallingIdentity();
        if (persistent) {
            try {
                ContentResolver resolver = this.mContext.getContentResolver();
                Settings.Global.putString(resolver, "debug_app", packageName);
                Settings.Global.putInt(resolver, "wait_for_debugger", waitForDebugger ? 1 : 0);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!persistent) {
                    this.mOrigDebugApp = this.mDebugApp;
                    this.mOrigWaitForDebugger = this.mWaitForDebugger;
                }
                this.mDebugApp = packageName;
                this.mWaitForDebugger = waitForDebugger;
                this.mDebugTransient = !persistent;
                if (packageName != null) {
                    forceStopPackageLocked(packageName, -1, false, false, true, true, false, -1, "set debug app");
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    void setTrackAllocationApp(ApplicationInfo app, String processName) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                boolean isDebuggable = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
                if (!isDebuggable && (app.flags & 2) == 0) {
                    throw new SecurityException("Process not debuggable: " + app.packageName);
                }
                this.mTrackAllocationApp = processName;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    void setProfileApp(ApplicationInfo app, String processName, ProfilerInfo profilerInfo) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                boolean isDebuggable = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
                if (!isDebuggable && (app.flags & 2) == 0) {
                    throw new SecurityException("Process not debuggable: " + app.packageName);
                }
                this.mProfileApp = processName;
                this.mProfileFile = profilerInfo.profileFile;
                if (this.mProfileFd != null) {
                    try {
                        this.mProfileFd.close();
                    } catch (IOException e) {
                    }
                    this.mProfileFd = null;
                }
                this.mProfileFd = profilerInfo.profileFd;
                this.mSamplingInterval = profilerInfo.samplingInterval;
                this.mAutoStopProfiler = profilerInfo.autoStopProfiler;
                this.mProfileType = 0;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void setNativeDebuggingAppLocked(ApplicationInfo app, String processName) {
        boolean isDebuggable = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
        if (!isDebuggable && (app.flags & 2) == 0) {
            throw new SecurityException("Process not debuggable: " + app.packageName);
        }
        this.mNativeDebuggingApp = processName;
    }

    public void setAlwaysFinish(boolean enabled) {
        enforceCallingPermission("android.permission.SET_ALWAYS_FINISH", "setAlwaysFinish()");
        long ident = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(this.mContext.getContentResolver(), "always_finish_activities", enabled ? 1 : 0);
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    this.mAlwaysFinishActivities = enabled;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setLenientBackgroundCheck(boolean enabled) {
        enforceCallingPermission("android.permission.SET_PROCESS_LIMIT", "setLenientBackgroundCheck()");
        long ident = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(this.mContext.getContentResolver(), "lenient_background_check", enabled ? 1 : 0);
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    this.mLenientBackgroundCheck = enabled;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setActivityController(IActivityController controller, boolean imAMonkey) {
        enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "setActivityController()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mController = controller;
                this.mControllerIsAMonkey = imAMonkey;
                Watchdog.getInstance().setActivityController(controller);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setUserIsMonkey(boolean userIsMonkey) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                synchronized (this.mPidsSelfLocked) {
                    int callingPid = Binder.getCallingPid();
                    ProcessRecord precessRecord = this.mPidsSelfLocked.get(callingPid);
                    if (precessRecord == null) {
                        throw new SecurityException("Unknown process: " + callingPid);
                    }
                    if (precessRecord.instrumentationUiAutomationConnection == null) {
                        throw new SecurityException("Only an instrumentation process with a UiAutomation can call setUserIsMonkey");
                    }
                }
                this.mUserIsMonkey = userIsMonkey;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean isUserAMonkey() {
        boolean z;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                z = !this.mUserIsMonkey ? this.mController != null ? this.mControllerIsAMonkey : false : true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    public void requestBugReport(int bugreportType) {
        String service = null;
        switch (bugreportType) {
            case 0:
                service = "bugreport";
                break;
            case 1:
                service = "bugreportplus";
                break;
            case 2:
                service = "bugreportremote";
                break;
        }
        if (service == null) {
            throw new IllegalArgumentException("Provided bugreport type is not correct, value: " + bugreportType);
        }
        enforceCallingPermission("android.permission.DUMP", "requestBugReport");
        SystemProperties.set("ctl.start", service);
    }

    public static long getInputDispatchingTimeoutLocked(ActivityRecord r) {
        if (r != null) {
            return getInputDispatchingTimeoutLocked(r.app);
        }
        return 30000L;
    }

    public static long getInputDispatchingTimeoutLocked(ProcessRecord r) {
        if (r == null) {
            return 30000L;
        }
        if (r.instrumentationClass != null || r.usingWrapper) {
            return 60000L;
        }
        return 30000L;
    }

    public long inputDispatchingTimedOut(int pid, boolean aboveSystem, String reason) {
        ProcessRecord proc;
        long timeout;
        if (checkCallingPermission("android.permission.FILTER_EVENTS") != 0) {
            throw new SecurityException("Requires permission android.permission.FILTER_EVENTS");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                synchronized (this.mPidsSelfLocked) {
                    proc = this.mPidsSelfLocked.get(pid);
                }
                timeout = getInputDispatchingTimeoutLocked(proc);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (!inputDispatchingTimedOut(proc, null, null, aboveSystem, reason)) {
            return -1L;
        }
        return timeout;
    }

    public boolean inputDispatchingTimedOut(final ProcessRecord proc, final ActivityRecord activity, final ActivityRecord parent, final boolean aboveSystem, String reason) {
        if (checkCallingPermission("android.permission.FILTER_EVENTS") != 0) {
            throw new SecurityException("Requires permission android.permission.FILTER_EVENTS");
        }
        final String annotation = reason == null ? "Input dispatching timed out" : "Input dispatching timed out (" + reason + ")";
        if (proc != null) {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (proc.debugging) {
                        return false;
                    }
                    if (mANRManager.isAnrDeferrable()) {
                        Slog.i(TAG, "Skipping keyDispatchingTimedOut ANR: " + proc);
                        this.mDidDexOpt = true;
                    }
                    if (this.mDidDexOpt) {
                        this.mDidDexOpt = false;
                        return false;
                    }
                    if (proc.instrumentationClass != null) {
                        Bundle info = new Bundle();
                        info.putString("shortMsg", "keyDispatchingTimedOut");
                        info.putString("longMsg", annotation);
                        finishInstrumentationLocked(proc, 0, info);
                        return true;
                    }
                    resetPriorityAfterLockedSection();
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ActivityManagerService.this.mAppErrors.appNotResponding(proc, activity, parent, aboveSystem, annotation);
                        }
                    });
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        }
        return true;
    }

    public Bundle getAssistContextExtras(int requestType) {
        PendingAssistExtras pae = enqueueAssistContext(requestType, null, null, null, null, null, true, true, UserHandle.getCallingUserId(), null, 500L);
        if (pae == null) {
            return null;
        }
        synchronized (pae) {
            while (!pae.haveResult) {
                try {
                    pae.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                buildAssistBundleLocked(pae, pae.result);
                this.mPendingAssistExtras.remove(pae);
                this.mUiHandler.removeCallbacks(pae);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return pae.extras;
    }

    public boolean isAssistDataAllowedOnCurrentActivity() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mUserController.getCurrentUserIdLocked();
                ActivityRecord activity = getFocusedStack().topActivity();
                if (activity == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                int userId = activity.userId;
                resetPriorityAfterLockedSection();
                DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
                return dpm == null || !dpm.getScreenCaptureDisabled(null, userId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean showAssistFromActivity(IBinder token, Bundle args) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord caller = ActivityRecord.forTokenLocked(token);
                    ActivityRecord top = getFocusedStack().topActivity();
                    if (top != caller) {
                        Slog.w(TAG, "showAssistFromActivity failed: caller " + caller + " is not current top " + top);
                        return false;
                    }
                    if (top.nowVisible) {
                        resetPriorityAfterLockedSection();
                        AssistUtils utils = new AssistUtils(this.mContext);
                        return utils.showSessionForActiveService(args, 8, (IVoiceInteractionSessionShowCallback) null, token);
                    }
                    Slog.w(TAG, "showAssistFromActivity failed: caller " + caller + " is not visible");
                    resetPriorityAfterLockedSection();
                    return false;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean requestAssistContextExtras(int requestType, IResultReceiver receiver, Bundle receiverExtras, IBinder activityToken, boolean focused, boolean newSessionId) {
        return enqueueAssistContext(requestType, null, null, receiver, receiverExtras, activityToken, focused, newSessionId, UserHandle.getCallingUserId(), null, 2000L) != null;
    }

    private PendingAssistExtras enqueueAssistContext(int requestType, Intent intent, String hint, IResultReceiver receiver, Bundle receiverExtras, IBinder activityToken, boolean focused, boolean newSessionId, int userHandle, Bundle args, long timeout) {
        ActivityRecord caller;
        enforceCallingPermission("android.permission.GET_TOP_ACTIVITY_INFO", "enqueueAssistContext()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord activity = getFocusedStack().topActivity();
                if (activity == null) {
                    Slog.w(TAG, "getAssistContextExtras failed: no top activity");
                    resetPriorityAfterLockedSection();
                    return null;
                }
                if (activity.app == null || activity.app.thread == null) {
                    Slog.w(TAG, "getAssistContextExtras failed: no process for " + activity);
                    resetPriorityAfterLockedSection();
                    return null;
                }
                if (!focused) {
                    activity = ActivityRecord.forTokenLocked(activityToken);
                    if (activity == null) {
                        Slog.w(TAG, "enqueueAssistContext failed: activity for token=" + activityToken + " couldn't be found");
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                } else if (activityToken != null && activity != (caller = ActivityRecord.forTokenLocked(activityToken))) {
                    Slog.w(TAG, "enqueueAssistContext failed: caller " + caller + " is not current top " + activity);
                    resetPriorityAfterLockedSection();
                    return null;
                }
                Bundle extras = new Bundle();
                if (args != null) {
                    extras.putAll(args);
                }
                extras.putString("android.intent.extra.ASSIST_PACKAGE", activity.packageName);
                extras.putInt("android.intent.extra.ASSIST_UID", activity.app.uid);
                PendingAssistExtras pae = new PendingAssistExtras(activity, extras, intent, hint, receiver, receiverExtras, userHandle);
                if (newSessionId) {
                    this.mViSessionId++;
                }
                try {
                    activity.app.thread.requestAssistContextExtras(activity.appToken, pae, requestType, this.mViSessionId);
                    this.mPendingAssistExtras.add(pae);
                    this.mUiHandler.postDelayed(pae, timeout);
                    resetPriorityAfterLockedSection();
                    return pae;
                } catch (RemoteException e) {
                    Slog.w(TAG, "getAssistContextExtras failed: crash calling " + activity);
                    resetPriorityAfterLockedSection();
                    return null;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void pendingAssistExtrasTimedOut(PendingAssistExtras pae) {
        IResultReceiver receiver;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mPendingAssistExtras.remove(pae);
                receiver = pae.receiver;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (receiver == null) {
            return;
        }
        Bundle sendBundle = new Bundle();
        sendBundle.putBundle("receiverExtras", pae.receiverExtras);
        try {
            pae.receiver.send(0, sendBundle);
        } catch (RemoteException e) {
        }
    }

    private void buildAssistBundleLocked(PendingAssistExtras pae, Bundle result) {
        if (result != null) {
            pae.extras.putBundle("android.intent.extra.ASSIST_CONTEXT", result);
        }
        if (pae.hint == null) {
            return;
        }
        pae.extras.putBoolean(pae.hint, true);
    }

    public void reportAssistContextExtras(IBinder token, Bundle extras, AssistStructure structure, AssistContent content, Uri referrer) throws Throwable {
        PendingAssistExtras pae = (PendingAssistExtras) token;
        synchronized (pae) {
            pae.result = extras;
            pae.structure = structure;
            pae.content = content;
            if (referrer != null) {
                pae.extras.putParcelable("android.intent.extra.REFERRER", referrer);
            }
            pae.haveResult = true;
            pae.notifyAll();
            if (pae.intent == null) {
                if (pae.receiver == null) {
                    return;
                }
            }
            Bundle bundle = null;
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    buildAssistBundleLocked(pae, extras);
                    boolean exists = this.mPendingAssistExtras.remove(pae);
                    this.mUiHandler.removeCallbacks(pae);
                    if (!exists) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    IResultReceiver sendReceiver = pae.receiver;
                    if (sendReceiver != null) {
                        Bundle sendBundle = new Bundle();
                        try {
                            sendBundle.putBundle("data", pae.extras);
                            sendBundle.putParcelable("structure", pae.structure);
                            sendBundle.putParcelable("content", pae.content);
                            sendBundle.putBundle("receiverExtras", pae.receiverExtras);
                            bundle = sendBundle;
                        } catch (Throwable th) {
                            th = th;
                        }
                    }
                    resetPriorityAfterLockedSection();
                    if (sendReceiver != null) {
                        try {
                            sendReceiver.send(0, bundle);
                            return;
                        } catch (RemoteException e) {
                            return;
                        }
                    }
                    long ident = Binder.clearCallingIdentity();
                    try {
                        pae.intent.replaceExtras(pae.extras);
                        pae.intent.setFlags(872415232);
                        closeSystemDialogs(PhoneWindowManager.SYSTEM_DIALOG_REASON_ASSIST);
                        try {
                            this.mContext.startActivityAsUser(pae.intent, new UserHandle(pae.userHandle));
                        } catch (ActivityNotFoundException e2) {
                            Slog.w(TAG, "No activity to handle assist action.", e2);
                        }
                        return;
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean launchAssistIntent(Intent intent, int requestType, String hint, int userHandle, Bundle args) {
        return enqueueAssistContext(requestType, intent, hint, null, null, null, true, true, userHandle, args, 500L) != null;
    }

    public void registerProcessObserver(IProcessObserver observer) {
        enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerProcessObserver()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mProcessObservers.register(observer);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void unregisterProcessObserver(IProcessObserver observer) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mProcessObservers.unregister(observer);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void registerUidObserver(IUidObserver observer, int which) {
        enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "registerUidObserver()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mUidObservers.register(observer, Integer.valueOf(which));
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void unregisterUidObserver(IUidObserver observer) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mUidObservers.unregister(observer);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean convertFromTranslucent(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r == null) {
                        return false;
                    }
                    boolean translucentChanged = r.changeWindowTranslucency(true);
                    if (translucentChanged) {
                        r.task.stack.releaseBackgroundResources(r);
                        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                    }
                    this.mWindowManager.setAppFullscreen(token, true);
                    resetPriorityAfterLockedSection();
                    return translucentChanged;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean convertToTranslucent(IBinder token, ActivityOptions options) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r == null) {
                        return false;
                    }
                    int index = r.task.mActivities.lastIndexOf(r);
                    if (index > 0) {
                        ActivityRecord under = r.task.mActivities.get(index - 1);
                        under.returningOptions = options;
                    }
                    boolean translucentChanged = r.changeWindowTranslucency(false);
                    if (translucentChanged) {
                        r.task.stack.convertActivityToTranslucent(r);
                    }
                    this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                    this.mWindowManager.setAppFullscreen(token, false);
                    resetPriorityAfterLockedSection();
                    return translucentChanged;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean requestVisibleBehind(IBinder token, boolean visible) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r != null) {
                        return this.mStackSupervisor.requestVisibleBehindLocked(r, visible);
                    }
                    resetPriorityAfterLockedSection();
                    return false;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isBackgroundVisibleBehind(IBinder token) {
        boolean zHasVisibleBehindActivity;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityStack stack = ActivityRecord.getStackLocked(token);
                    zHasVisibleBehindActivity = stack == null ? false : stack.hasVisibleBehindActivity();
                    if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
                        Slog.d(TAG_VISIBLE_BEHIND, "isBackgroundVisibleBehind: stack=" + stack + " visible=" + zHasVisibleBehindActivity);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return zHasVisibleBehindActivity;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public ActivityOptions getActivityOptions(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInStackLocked(token);
                    if (r == null) {
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                    ActivityOptions activityOptions = r.pendingOptions;
                    r.pendingOptions = null;
                    return activityOptions;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setImmersive(IBinder token, boolean immersive) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException();
                }
                r.immersive = immersive;
                if (r == this.mFocusedActivity) {
                    if (ActivityManagerDebugConfig.DEBUG_IMMERSIVE) {
                        Slog.d(TAG_IMMERSIVE, "Frontmost changed immersion: " + r);
                    }
                    applyUpdateLockStateLocked(r);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isImmersive(IBinder token) {
        boolean z;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException();
                }
                z = r.immersive;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    public int setVrMode(IBinder token, boolean enabled, ComponentName packageName) {
        ActivityRecord r;
        if (!this.mContext.getPackageManager().hasSystemFeature("android.software.vr.mode")) {
            throw new UnsupportedOperationException("VR mode not supported on this device!");
        }
        VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                r = ActivityRecord.isInStackLocked(token);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (r == null) {
            throw new IllegalArgumentException();
        }
        int err = vrService.hasVrPackage(packageName, r.userId);
        if (err != 0) {
            return err;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!enabled) {
                    packageName = null;
                }
                r.requestedVrComponent = packageName;
                if (r == this.mFocusedActivity) {
                    applyUpdateVrModeLocked(r);
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
        return 0;
    }

    public boolean isVrModePackageEnabled(ComponentName packageName) {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.software.vr.mode")) {
            throw new UnsupportedOperationException("VR mode not supported on this device!");
        }
        VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        return vrService.hasVrPackage(packageName, UserHandle.getCallingUserId()) == 0;
    }

    public boolean isTopActivityImmersive() {
        boolean z;
        enforceNotIsolatedCaller("startActivity");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = getFocusedStack().topRunningActivityLocked();
                z = r != null ? r.immersive : false;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    public boolean isTopOfTask(IBinder token) {
        boolean z;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException();
                }
                z = r.task.getTopActivity() == r;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    public final void enterSafeMode() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!this.mSystemReady) {
                    try {
                        AppGlobals.getPackageManager().enterSafeMode();
                    } catch (RemoteException e) {
                    }
                }
                this.mSafeMode = true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public final void showSafeModeOverlay() {
        View v = LayoutInflater.from(this.mContext).inflate(R.layout.notification_2025_template_expanded_messaging, (ViewGroup) null);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = 2015;
        lp.width = -2;
        lp.height = -2;
        lp.gravity = 8388691;
        lp.format = v.getBackground().getOpacity();
        lp.flags = 24;
        lp.privateFlags |= 16;
        ((WindowManager) this.mContext.getSystemService("window")).addView(v, lp);
    }

    public void noteWakeupAlarm(IIntentSender sender, int sourceUid, String sourcePkg, String tag) {
        int uid;
        if (sender != null && !(sender instanceof PendingIntentRecord)) {
            return;
        }
        PendingIntentRecord rec = (PendingIntentRecord) sender;
        BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
        synchronized (stats) {
            if (this.mBatteryStatsService.isOnBattery()) {
                this.mBatteryStatsService.enforceCallingPermission();
                int MY_UID = Binder.getCallingUid();
                if (sender == null) {
                    uid = sourceUid;
                } else {
                    uid = rec.uid == MY_UID ? 1000 : rec.uid;
                }
                if (sourceUid < 0) {
                    sourceUid = uid;
                }
                if (sourcePkg == null) {
                    sourcePkg = rec.key.packageName;
                }
                BatteryStatsImpl.Uid.Pkg pkg = stats.getPackageStatsLocked(sourceUid, sourcePkg);
                pkg.noteWakeupAlarmLocked(tag);
            }
        }
    }

    public void noteAlarmStart(IIntentSender sender, int sourceUid, String tag) {
        int uid;
        if (sender != null && !(sender instanceof PendingIntentRecord)) {
            return;
        }
        PendingIntentRecord rec = (PendingIntentRecord) sender;
        BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
        synchronized (stats) {
            this.mBatteryStatsService.enforceCallingPermission();
            int MY_UID = Binder.getCallingUid();
            if (sender == null) {
                uid = sourceUid;
            } else {
                uid = rec.uid == MY_UID ? 1000 : rec.uid;
            }
            BatteryStatsService batteryStatsService = this.mBatteryStatsService;
            if (sourceUid < 0) {
                sourceUid = uid;
            }
            batteryStatsService.noteAlarmStart(tag, sourceUid);
        }
    }

    public void noteAlarmFinish(IIntentSender sender, int sourceUid, String tag) {
        int uid;
        if (sender != null && !(sender instanceof PendingIntentRecord)) {
            return;
        }
        PendingIntentRecord rec = (PendingIntentRecord) sender;
        BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
        synchronized (stats) {
            this.mBatteryStatsService.enforceCallingPermission();
            int MY_UID = Binder.getCallingUid();
            if (sender == null) {
                uid = sourceUid;
            } else {
                uid = rec.uid == MY_UID ? 1000 : rec.uid;
            }
            BatteryStatsService batteryStatsService = this.mBatteryStatsService;
            if (sourceUid < 0) {
                sourceUid = uid;
            }
            batteryStatsService.noteAlarmFinish(tag, sourceUid);
        }
    }

    public boolean killPids(int[] pids, String pReason, boolean secure) {
        int type;
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("killPids only available to the system");
        }
        String reason = pReason == null ? "Unknown" : pReason;
        boolean killed = false;
        synchronized (this.mPidsSelfLocked) {
            int worstType = 0;
            for (int i : pids) {
                ProcessRecord proc = this.mPidsSelfLocked.get(i);
                if (proc != null && (type = proc.setAdj) > worstType) {
                    worstType = type;
                }
            }
            if (worstType < 906 && worstType > 900) {
                worstType = 900;
            }
            if (!secure && worstType < 500) {
                worstType = 500;
            }
            Slog.w(TAG, "Killing processes " + reason + " at adjustment " + worstType);
            for (int i2 : pids) {
                ProcessRecord proc2 = this.mPidsSelfLocked.get(i2);
                if (proc2 != null) {
                    int adj = proc2.setAdj;
                    if (adj >= worstType && !proc2.killedByAm) {
                        proc2.kill(reason, true);
                        killed = true;
                    }
                }
            }
        }
        return killed;
    }

    public void killUid(int appId, int userId, String reason) {
        enforceCallingPermission("android.permission.KILL_UID", "killUid");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long identity = Binder.clearCallingIdentity();
                try {
                    killPackageProcessesLocked(null, appId, userId, -800, false, true, true, true, reason != null ? reason : "kill uid");
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean killProcessesBelowForeground(String reason) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("killProcessesBelowForeground() only available to system");
        }
        return killProcessesBelowAdj(0, reason);
    }

    private boolean killProcessesBelowAdj(int belowAdj, String reason) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("killProcessesBelowAdj() only available to system");
        }
        boolean killed = false;
        synchronized (this.mPidsSelfLocked) {
            int size = this.mPidsSelfLocked.size();
            for (int i = 0; i < size; i++) {
                this.mPidsSelfLocked.keyAt(i);
                ProcessRecord proc = this.mPidsSelfLocked.valueAt(i);
                if (proc != null) {
                    int adj = proc.setAdj;
                    if (adj > belowAdj && !proc.killedByAm) {
                        proc.kill(reason, true);
                        killed = true;
                    }
                }
            }
        }
        return killed;
    }

    public void hang(IBinder who, boolean allowRestart) {
        if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
            throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
        }
        IBinder.DeathRecipient death = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                synchronized (this) {
                    notifyAll();
                }
            }
        };
        try {
            who.linkToDeath(death, 0);
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    Watchdog.getInstance().setAllowRestart(allowRestart);
                    Slog.i(TAG, "Hanging system process at request of pid " + Binder.getCallingPid());
                    synchronized (death) {
                        while (who.isBinderAlive()) {
                            try {
                                death.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    Watchdog.getInstance().setAllowRestart(true);
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } catch (RemoteException e2) {
            Slog.w(TAG, "hang: given caller IBinder is already dead.");
        }
    }

    public void restart() {
        if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
            throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
        }
        Log.i(TAG, "Sending shutdown broadcast...");
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) throws Throwable {
                Log.i(ActivityManagerService.TAG, "Shutting down activity manager...");
                ActivityManagerService.this.shutdown(10000);
                Log.i(ActivityManagerService.TAG, "Shutdown complete, restarting!");
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        };
        Intent intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        intent.addFlags(268435456);
        intent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
        br.onReceive(this.mContext, intent);
    }

    private long getLowRamTimeSinceIdle(long now) {
        return (this.mLowRamStartTime > 0 ? now - this.mLowRamStartTime : 0L) + this.mLowRamTimeSinceLastIdle;
    }

    public void performIdleMaintenance() {
        if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
            throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long now = SystemClock.uptimeMillis();
                long timeSinceLastIdle = now - this.mLastIdleTime;
                long lowRamSinceLastIdle = getLowRamTimeSinceIdle(now);
                this.mLastIdleTime = now;
                this.mLowRamTimeSinceLastIdle = 0L;
                if (this.mLowRamStartTime != 0) {
                    this.mLowRamStartTime = now;
                }
                StringBuilder sb = new StringBuilder(128);
                sb.append("Idle maintenance over ");
                TimeUtils.formatDuration(timeSinceLastIdle, sb);
                sb.append(" low RAM for ");
                TimeUtils.formatDuration(lowRamSinceLastIdle, sb);
                Slog.i(TAG, sb.toString());
                boolean doKilling = lowRamSinceLastIdle > timeSinceLastIdle / 3;
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord proc = this.mLruProcesses.get(i);
                    if (proc.notCachedSinceIdle) {
                        if (proc.setProcState != 5 && proc.setProcState >= 4 && proc.setProcState <= 10 && doKilling && proc.initialIdlePss != 0 && proc.lastPss > (proc.initialIdlePss * 3) / 2) {
                            StringBuilder sb2 = new StringBuilder(128);
                            sb2.append("Kill");
                            sb2.append(proc.processName);
                            sb2.append(" in idle maint: pss=");
                            sb2.append(proc.lastPss);
                            sb2.append(", swapPss=");
                            sb2.append(proc.lastSwapPss);
                            sb2.append(", initialPss=");
                            sb2.append(proc.initialIdlePss);
                            sb2.append(", period=");
                            TimeUtils.formatDuration(timeSinceLastIdle, sb2);
                            sb2.append(", lowRamPeriod=");
                            TimeUtils.formatDuration(lowRamSinceLastIdle, sb2);
                            Slog.wtfQuiet(TAG, sb2.toString());
                            proc.kill("idle maint (pss " + proc.lastPss + " from " + proc.initialIdlePss + ")", true);
                        }
                    } else if (proc.setProcState < 12 && proc.setProcState > -1) {
                        proc.notCachedSinceIdle = true;
                        proc.initialIdlePss = 0L;
                        proc.nextPssTime = ProcessList.computeNextPssTime(proc.setProcState, true, this.mTestPssMode, isSleepingLocked(), now);
                    }
                }
                this.mHandler.removeMessages(39);
                this.mHandler.sendEmptyMessageDelayed(39, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void sendIdleJobTrigger() {
        if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
            throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent(ACTION_TRIGGER_IDLE).setPackage("android").addFlags(1073741824);
            broadcastIntent(null, intent, null, null, 0, null, null, null, -1, null, true, false, -1);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void retrieveSettings() {
        boolean freeformWindowManagement;
        ContentResolver resolver = this.mContext.getContentResolver();
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.freeform_window_management")) {
            if (this.mContext.getPackageManager().hasSystemFeature("android.software.freeform_window_management") || Settings.Global.getInt(resolver, "enable_freeform_support", 0) != 0) {
                freeformWindowManagement = true;
            } else {
                freeformWindowManagement = MultiWindowManager.isSupported();
            }
        } else {
            freeformWindowManagement = this.mContext.getPackageManager().hasSystemFeature("android.software.freeform_window_management") || Settings.Global.getInt(resolver, "enable_freeform_support", 0) != 0;
        }
        boolean supportsPictureInPicture = this.mContext.getPackageManager().hasSystemFeature("android.software.picture_in_picture");
        boolean supportsMultiWindow = ActivityManager.supportsMultiWindow();
        String debugApp = Settings.Global.getString(resolver, "debug_app");
        boolean waitForDebugger = Settings.Global.getInt(resolver, "wait_for_debugger", 0) != 0;
        boolean alwaysFinishActivities = Settings.Global.getInt(resolver, "always_finish_activities", 0) != 0;
        boolean lenientBackgroundCheck = Settings.Global.getInt(resolver, "lenient_background_check", 0) != 0;
        boolean forceRtl = Settings.Global.getInt(resolver, "debug.force_rtl", 0) != 0;
        boolean forceResizable = Settings.Global.getInt(resolver, "force_resizable_activities", 0) != 0;
        boolean supportsLeanbackOnly = this.mContext.getPackageManager().hasSystemFeature("android.software.leanback_only");
        SystemProperties.set("debug.force_rtl", forceRtl ? "1" : "0");
        Configuration configuration = new Configuration();
        Settings.System.getConfiguration(resolver, configuration);
        if (forceRtl) {
            configuration.setLayoutDirection(configuration.locale);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mOrigDebugApp = debugApp;
                this.mDebugApp = debugApp;
                this.mOrigWaitForDebugger = waitForDebugger;
                this.mWaitForDebugger = waitForDebugger;
                this.mAlwaysFinishActivities = alwaysFinishActivities;
                this.mLenientBackgroundCheck = lenientBackgroundCheck;
                this.mSupportsLeanbackOnly = supportsLeanbackOnly;
                this.mForceResizableActivities = forceResizable;
                this.mWindowManager.setForceResizableTasks(this.mForceResizableActivities);
                if (supportsMultiWindow || forceResizable) {
                    this.mSupportsMultiWindow = true;
                    this.mSupportsFreeformWindowManagement = !freeformWindowManagement ? forceResizable : true;
                    if (supportsPictureInPicture) {
                        forceResizable = true;
                    }
                    this.mSupportsPictureInPicture = forceResizable;
                } else {
                    this.mSupportsMultiWindow = false;
                    this.mSupportsFreeformWindowManagement = false;
                    this.mSupportsPictureInPicture = false;
                }
                updateConfigurationLocked(configuration, null, true);
                if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Initial config: " + this.mConfiguration);
                }
                Resources res = this.mContext.getResources();
                this.mHasRecents = res.getBoolean(R.^attr-private.listItemLayout);
                this.mThumbnailWidth = res.getDimensionPixelSize(R.dimen.thumbnail_width);
                this.mThumbnailHeight = res.getDimensionPixelSize(R.dimen.thumbnail_height);
                this.mDefaultPinnedStackBounds = Rect.unflattenFromString(res.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_ENTRY));
                this.mAppErrors.loadAppsNotReportingCrashesFromConfigLocked(res.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_ERROR));
                if ((this.mConfiguration.uiMode & 4) == 4) {
                    this.mFullscreenThumbnailScale = res.getInteger(R.integer.config_globalActionsKeyTimeout) / this.mConfiguration.screenWidthDp;
                } else {
                    this.mFullscreenThumbnailScale = res.getFraction(R.fraction.config_prescaleAbsoluteVolume_index1, 1, 1);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean testIsSystemReady() {
        return this.mSystemReady;
    }

    public void systemReady(Runnable goingCallback) throws Throwable {
        int currentUserId;
        ArrayList<ProcessRecord> procsToKill;
        BootEvent.addBootEvent("AMS:systemReady");
        initOnSystemReady();
        AMEventHookData.SystemReady eventData = AMEventHookData.SystemReady.createInstance();
        eventData.set(new Object[]{0, this.mContext, this});
        this.mAMEventHook.hook(AMEventHook.Event.AM_SystemReady, eventData);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (this.mSystemReady) {
                    if (goingCallback != null) {
                        goingCallback.run();
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
                this.mUserController.onSystemReady();
                this.mRecentTasks.onSystemReadyLocked();
                this.mAppOpsService.systemReady();
                this.mSystemReady = true;
                resetPriorityAfterLockedSection();
                synchronized (this.mPidsSelfLocked) {
                    try {
                        int i = this.mPidsSelfLocked.size() - 1;
                        ArrayList<ProcessRecord> procsToKill2 = null;
                        while (i >= 0) {
                            try {
                                ProcessRecord proc = this.mPidsSelfLocked.valueAt(i);
                                if (isAllowedWhileBooting(proc.info)) {
                                    procsToKill = procsToKill2;
                                } else {
                                    procsToKill = procsToKill2 == null ? new ArrayList<>() : procsToKill2;
                                    procsToKill.add(proc);
                                }
                                i--;
                                procsToKill2 = procsToKill;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        synchronized (this) {
                            try {
                                boostPriorityForLockedSection();
                                if (procsToKill2 != null) {
                                    for (int i2 = procsToKill2.size() - 1; i2 >= 0; i2--) {
                                        ProcessRecord proc2 = procsToKill2.get(i2);
                                        Slog.i(TAG, "Removing system update proc: " + proc2);
                                        removeProcessLocked(proc2, true, false, "system update done");
                                    }
                                }
                                this.mProcessesReady = true;
                            } catch (Throwable th2) {
                                resetPriorityAfterLockedSection();
                                throw th2;
                            }
                        }
                        resetPriorityAfterLockedSection();
                        Slog.i(TAG, "System now ready");
                        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_AMS_READY, SystemClock.uptimeMillis());
                        BootEvent.addBootEvent("AMS:AMS_READY");
                        AMEventHookData.SystemReady eventData2 = AMEventHookData.SystemReady.createInstance();
                        eventData2.set(new Object[]{Integer.valueOf(FIRST_BROADCAST_QUEUE_MSG), this.mContext, this});
                        this.mAMEventHook.hook(AMEventHook.Event.AM_SystemReady, eventData2);
                        synchronized (this) {
                            try {
                                boostPriorityForLockedSection();
                                if (this.mFactoryTest == 1) {
                                    ResolveInfo ri = this.mContext.getPackageManager().resolveActivity(new Intent("android.intent.action.FACTORY_TEST"), 1024);
                                    CharSequence errorMsg = null;
                                    if (ri != null) {
                                        ActivityInfo ai = ri.activityInfo;
                                        ApplicationInfo app = ai.applicationInfo;
                                        if ((app.flags & 1) != 0) {
                                            this.mTopAction = "android.intent.action.FACTORY_TEST";
                                            this.mTopData = null;
                                            this.mTopComponent = new ComponentName(app.packageName, ai.name);
                                        } else {
                                            errorMsg = this.mContext.getResources().getText(R.string.config_systemGameService);
                                        }
                                    } else {
                                        errorMsg = this.mContext.getResources().getText(R.string.config_systemImageEditor);
                                    }
                                    if (errorMsg != null) {
                                        this.mTopAction = null;
                                        this.mTopData = null;
                                        this.mTopComponent = null;
                                        Message msg = Message.obtain();
                                        msg.what = 3;
                                        msg.getData().putCharSequence("msg", errorMsg);
                                        this.mUiHandler.sendMessage(msg);
                                    }
                                }
                            } catch (Throwable th3) {
                                resetPriorityAfterLockedSection();
                                throw th3;
                            }
                        }
                        resetPriorityAfterLockedSection();
                        retrieveSettings();
                        synchronized (this) {
                            try {
                                boostPriorityForLockedSection();
                                currentUserId = this.mUserController.getCurrentUserIdLocked();
                                readGrantedUriPermissionsLocked();
                            } catch (Throwable th4) {
                                resetPriorityAfterLockedSection();
                                throw th4;
                            }
                        }
                        resetPriorityAfterLockedSection();
                        if (goingCallback != null) {
                            goingCallback.run();
                        }
                        this.mBatteryStatsService.noteEvent(32775, Integer.toString(currentUserId), currentUserId);
                        this.mBatteryStatsService.noteEvent(32776, Integer.toString(currentUserId), currentUserId);
                        this.mSystemServiceManager.startUser(currentUserId);
                        synchronized (this) {
                            try {
                                boostPriorityForLockedSection();
                                startPersistentApps(PackageManagerService.DumpState.DUMP_FROZEN);
                                this.mBooting = true;
                                if (UserManager.isSplitSystemUser()) {
                                    ComponentName cName = new ComponentName(this.mContext, (Class<?>) SystemUserHomeActivity.class);
                                    try {
                                        AppGlobals.getPackageManager().setComponentEnabledSetting(cName, 1, 0, 0);
                                    } catch (RemoteException e) {
                                        throw e.rethrowAsRuntimeException();
                                    }
                                }
                                AMEventHookData.SystemReady eventData3 = AMEventHookData.SystemReady.createInstance();
                                eventData3.set(new Object[]{Integer.valueOf(FIRST_COMPAT_MODE_MSG), this.mContext, this});
                                AMEventHookResult eventResult = this.mAMEventHook.hook(AMEventHook.Event.AM_SystemReady, eventData3);
                                boolean skipHome = AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_SkipHomeActivityLaunching);
                                if (!skipHome) {
                                    startHomeActivityLocked(currentUserId, "systemReady");
                                }
                                try {
                                    if (AppGlobals.getPackageManager().hasSystemUidErrors()) {
                                        Slog.e(TAG, "UIDs on the system are inconsistent, you need to wipe your data partition or your device will be unstable.");
                                        this.mUiHandler.obtainMessage(14).sendToTarget();
                                    }
                                } catch (RemoteException e2) {
                                }
                                if (!Build.isBuildConsistent()) {
                                    Slog.e(TAG, "Build fingerprint is not consistent, warning user");
                                    this.mUiHandler.obtainMessage(15).sendToTarget();
                                }
                                long ident = Binder.clearCallingIdentity();
                                try {
                                    try {
                                        Intent intent = new Intent("android.intent.action.USER_STARTED");
                                        intent.addFlags(1342177280);
                                        intent.putExtra("android.intent.extra.user_handle", currentUserId);
                                        broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, MY_PID, 1000, currentUserId);
                                        Intent intent2 = new Intent("android.intent.action.USER_STARTING");
                                        intent2.addFlags(1073741824);
                                        intent2.putExtra("android.intent.extra.user_handle", currentUserId);
                                        broadcastIntentLocked(null, null, intent2, null, new IIntentReceiver.Stub() {
                                            public void performReceive(Intent intent3, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                                            }
                                        }, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, MY_PID, 1000, -1);
                                    } catch (Throwable t) {
                                        Slog.wtf(TAG, "Failed sending first user broadcasts", t);
                                        Binder.restoreCallingIdentity(ident);
                                    }
                                    AMEventHookData.SystemReady eventData4 = AMEventHookData.SystemReady.createInstance();
                                    eventData4.set(new Object[]{400, this.mContext, this});
                                    AMEventHookResult eventResult2 = this.mAMEventHook.hook(AMEventHook.Event.AM_SystemReady, eventData4);
                                    if (AMEventHookResult.hasAction(eventResult2, AMEventHookAction.AM_PostEnableScreenAfterBoot)) {
                                        postFinishBooting(false, true);
                                        AMEventHookData.AfterPostEnableScreenAfterBoot afterEventData = AMEventHookData.AfterPostEnableScreenAfterBoot.createInstance();
                                        afterEventData.set(new Object[]{this.mInstaller});
                                        AMEventHookResult eventResult3 = this.mAMEventHook.hook(AMEventHook.Event.AM_AfterPostEnableScreenAfterBoot, afterEventData);
                                        if (AMEventHookResult.hasAction(eventResult3, AMEventHookAction.AM_Interrupt)) {
                                            resetPriorityAfterLockedSection();
                                            return;
                                        }
                                    }
                                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                                    this.mUserController.sendUserSwitchBroadcastsLocked(-1, currentUserId);
                                    resetPriorityAfterLockedSection();
                                } finally {
                                    Binder.restoreCallingIdentity(ident);
                                }
                            } catch (Throwable th5) {
                                resetPriorityAfterLockedSection();
                                throw th5;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                }
            } catch (Throwable th7) {
                resetPriorityAfterLockedSection();
                throw th7;
            }
        }
    }

    void killAppAtUsersRequest(ProcessRecord app, Dialog fromDialog) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mAppErrors.killAppAtUserRequestLocked(app, fromDialog);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void skipCurrentReceiverLocked(ProcessRecord app) {
        for (BroadcastQueue queue : this.mBroadcastQueues) {
            if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.d(TAG_BROADCAST, "mBroadcastQueues: " + queue + " app = " + app);
            }
            queue.skipCurrentReceiverLocked(app);
        }
    }

    public void handleApplicationCrash(IBinder app, ApplicationErrorReport.CrashInfo crashInfo) {
        String processName;
        ProcessRecord r = findAppProcess(app, "Crash");
        if (app == null) {
            processName = "system_server";
        } else {
            processName = r == null ? "unknown" : r.processName;
        }
        handleApplicationCrashInner("crash", r, processName, crashInfo);
    }

    void handleApplicationCrashInner(String eventType, ProcessRecord r, String processName, ApplicationErrorReport.CrashInfo crashInfo) {
        Object[] objArr = new Object[8];
        objArr[0] = Integer.valueOf(Binder.getCallingPid());
        objArr[1] = Integer.valueOf(UserHandle.getUserId(Binder.getCallingUid()));
        objArr[2] = processName;
        objArr[3] = Integer.valueOf(r == null ? -1 : r.info.flags);
        objArr[4] = crashInfo.exceptionClassName;
        objArr[5] = crashInfo.exceptionMessage;
        objArr[6] = crashInfo.throwFileName;
        objArr[7] = Integer.valueOf(crashInfo.throwLineNumber);
        EventLog.writeEvent(EventLogTags.AM_CRASH, objArr);
        addErrorToDropBox(eventType, r, processName, null, null, null, null, null, crashInfo);
        this.mAppErrors.crashApplication(r, crashInfo);
    }

    public void handleApplicationStrictModeViolation(IBinder app, int violationMask, StrictMode.ViolationInfo info) {
        ProcessRecord r = findAppProcess(app, "StrictMode");
        if (r == null) {
            return;
        }
        if ((2097152 & violationMask) != 0) {
            Integer stackFingerprint = Integer.valueOf(info.hashCode());
            boolean logIt = true;
            synchronized (this.mAlreadyLoggedViolatedStacks) {
                if (this.mAlreadyLoggedViolatedStacks.contains(stackFingerprint)) {
                    logIt = false;
                } else {
                    if (this.mAlreadyLoggedViolatedStacks.size() >= 5000) {
                        this.mAlreadyLoggedViolatedStacks.clear();
                    }
                    this.mAlreadyLoggedViolatedStacks.add(stackFingerprint);
                }
            }
            if (logIt) {
                logStrictModeViolationToDropBox(r, info);
            }
        }
        if ((131072 & violationMask) == 0) {
            return;
        }
        AppErrorResult result = new AppErrorResult();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                Message msg = Message.obtain();
                msg.what = 26;
                HashMap<String, Object> data = new HashMap<>();
                data.put("result", result);
                data.put("app", r);
                data.put("violationMask", Integer.valueOf(violationMask));
                data.put("info", info);
                msg.obj = data;
                this.mUiHandler.sendMessage(msg);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        int res = result.get();
        Slog.w(TAG, "handleApplicationStrictModeViolation; res=" + res);
    }

    private void logStrictModeViolationToDropBox(ProcessRecord process, StrictMode.ViolationInfo info) {
        boolean bufferWasEmpty;
        boolean needsFlush;
        if (info == null) {
            return;
        }
        boolean isSystemApp = process == null || (process.info.flags & 129) != 0;
        String processName = process == null ? "unknown" : process.processName;
        final String dropboxTag = isSystemApp ? "system_app_strictmode" : "data_app_strictmode";
        final DropBoxManager dbox = (DropBoxManager) this.mContext.getSystemService("dropbox");
        if (dbox == null || !dbox.isTagEnabled(dropboxTag)) {
            return;
        }
        final StringBuilder sb = isSystemApp ? this.mStrictModeBuffer : new StringBuilder(1024);
        synchronized (sb) {
            bufferWasEmpty = sb.length() == 0;
            appendDropBoxProcessHeaders(process, processName, sb);
            sb.append("Build: ").append(Build.FINGERPRINT).append("\n");
            sb.append("System-App: ").append(isSystemApp).append("\n");
            sb.append("Uptime-Millis: ").append(info.violationUptimeMillis).append("\n");
            if (info.violationNumThisLoop != 0) {
                sb.append("Loop-Violation-Number: ").append(info.violationNumThisLoop).append("\n");
            }
            if (info.numAnimationsRunning != 0) {
                sb.append("Animations-Running: ").append(info.numAnimationsRunning).append("\n");
            }
            if (info.broadcastIntentAction != null) {
                sb.append("Broadcast-Intent-Action: ").append(info.broadcastIntentAction).append("\n");
            }
            if (info.durationMillis != -1) {
                sb.append("Duration-Millis: ").append(info.durationMillis).append("\n");
            }
            if (info.numInstances != -1) {
                sb.append("Instance-Count: ").append(info.numInstances).append("\n");
            }
            if (info.tags != null) {
                for (String tag : info.tags) {
                    sb.append("Span-Tag: ").append(tag).append("\n");
                }
            }
            sb.append("\n");
            if (info.crashInfo != null && info.crashInfo.stackTrace != null) {
                sb.append(info.crashInfo.stackTrace);
                sb.append("\n");
            }
            if (info.message != null) {
                sb.append(info.message);
                sb.append("\n");
            }
            needsFlush = sb.length() > 65536;
        }
        if (!isSystemApp || needsFlush) {
            new Thread("Error dump: " + dropboxTag) {
                @Override
                public void run() {
                    String report;
                    synchronized (sb) {
                        report = sb.toString();
                        sb.delete(0, sb.length());
                        sb.trimToSize();
                    }
                    if (report.length() == 0) {
                        return;
                    }
                    dbox.addText(dropboxTag, report);
                }
            }.start();
        } else {
            if (!bufferWasEmpty) {
                return;
            }
            new Thread("Error dump: " + dropboxTag) {
                @Override
                public void run() {
                    try {
                        Thread.sleep(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                    } catch (InterruptedException e) {
                    }
                    synchronized (ActivityManagerService.this.mStrictModeBuffer) {
                        String errorReport = ActivityManagerService.this.mStrictModeBuffer.toString();
                        if (errorReport.length() == 0) {
                            return;
                        }
                        ActivityManagerService.this.mStrictModeBuffer.delete(0, ActivityManagerService.this.mStrictModeBuffer.length());
                        ActivityManagerService.this.mStrictModeBuffer.trimToSize();
                        dbox.addText(dropboxTag, errorReport);
                    }
                }
            }.start();
        }
    }

    public boolean handleApplicationWtf(final IBinder app, final String tag, boolean system, final ApplicationErrorReport.CrashInfo crashInfo) {
        final int callingUid = Binder.getCallingUid();
        final int callingPid = Binder.getCallingPid();
        if (system) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ActivityManagerService.this.handleApplicationWtfInner(callingUid, callingPid, app, tag, crashInfo);
                }
            });
            return false;
        }
        ProcessRecord r = handleApplicationWtfInner(callingUid, callingPid, app, tag, crashInfo);
        if (r == null || r.pid == Process.myPid() || Settings.Global.getInt(this.mContext.getContentResolver(), "wtf_is_fatal", 0) == 0) {
            return false;
        }
        this.mAppErrors.crashApplication(r, crashInfo);
        return true;
    }

    ProcessRecord handleApplicationWtfInner(int callingUid, int callingPid, IBinder app, String tag, ApplicationErrorReport.CrashInfo crashInfo) {
        String processName;
        ProcessRecord r = findAppProcess(app, "WTF");
        if (app == null) {
            processName = "system_server";
        } else {
            processName = r == null ? "unknown" : r.processName;
        }
        Object[] objArr = new Object[6];
        objArr[0] = Integer.valueOf(UserHandle.getUserId(callingUid));
        objArr[1] = Integer.valueOf(callingPid);
        objArr[2] = processName;
        objArr[3] = Integer.valueOf(r == null ? -1 : r.info.flags);
        objArr[4] = tag;
        objArr[5] = crashInfo.exceptionMessage;
        EventLog.writeEvent(EventLogTags.AM_WTF, objArr);
        addErrorToDropBox("wtf", r, processName, null, null, tag, null, null, crashInfo);
        return r;
    }

    private ProcessRecord findAppProcess(IBinder app, String reason) {
        if (app == null) {
            return null;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int NP = this.mProcessNames.getMap().size();
                for (int ip = 0; ip < NP; ip++) {
                    SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                    int NA = apps.size();
                    for (int ia = 0; ia < NA; ia++) {
                        ProcessRecord p = apps.valueAt(ia);
                        if (p.thread != null && p.thread.asBinder() == app) {
                            resetPriorityAfterLockedSection();
                            return p;
                        }
                    }
                }
                Slog.w(TAG, "Can't find mystery application for " + reason + " from pid=" + Binder.getCallingPid() + " uid=" + Binder.getCallingUid() + ": " + app);
                resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void appendDropBoxProcessHeaders(ProcessRecord process, String processName, StringBuilder sb) {
        if (process == null) {
            sb.append("Process: ").append(processName).append("\n");
            return;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                sb.append("Process: ").append(processName).append("\n");
                int flags = process.info.flags;
                IPackageManager pm = AppGlobals.getPackageManager();
                sb.append("Flags: 0x").append(Integer.toString(flags, 16)).append("\n");
                for (int ip = 0; ip < process.pkgList.size(); ip++) {
                    String pkg = process.pkgList.keyAt(ip);
                    sb.append("Package: ").append(pkg);
                    try {
                        PackageInfo pi = pm.getPackageInfo(pkg, 0, UserHandle.getCallingUserId());
                        if (pi != null) {
                            sb.append(" v").append(pi.versionCode);
                            if (pi.versionName != null) {
                                sb.append(" (").append(pi.versionName).append(")");
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Error getting package info: " + pkg, e);
                    }
                    sb.append("\n");
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private static String processClass(ProcessRecord process) {
        if (process == null || process.pid == MY_PID) {
            return "system_server";
        }
        if ((process.info.flags & 1) != 0) {
            return "system_app";
        }
        return "data_app";
    }

    public void addErrorToDropBox(String eventType, ProcessRecord process, String processName, ActivityRecord activity, ActivityRecord parent, String subject, final String report, final File dataFile, final ApplicationErrorReport.CrashInfo crashInfo) {
        final String dropboxTag = processClass(process) + "_" + eventType;
        final DropBoxManager dbox = (DropBoxManager) this.mContext.getSystemService("dropbox");
        if (dbox == null || !dbox.isTagEnabled(dropboxTag)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - this.mWtfClusterStart > JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
            this.mWtfClusterStart = now;
            this.mWtfClusterCount = 1;
        } else {
            int i = this.mWtfClusterCount;
            this.mWtfClusterCount = i + 1;
            if (i >= 5) {
                return;
            }
        }
        final StringBuilder sb = new StringBuilder(1024);
        appendDropBoxProcessHeaders(process, processName, sb);
        if (process != null) {
            sb.append("Foreground: ").append(process.isInterestingToUserLocked() ? "Yes" : "No").append("\n");
        }
        final String spid = getProcessPid(process);
        if (activity != null) {
            sb.append("Activity: ").append(activity.shortComponentName).append("\n");
        }
        if (parent != null && parent.app != null && parent.app.pid != process.pid) {
            sb.append("Parent-Process: ").append(parent.app.processName).append("\n");
        }
        if (parent != null && parent != activity) {
            sb.append("Parent-Activity: ").append(parent.shortComponentName).append("\n");
        }
        if (subject != null) {
            sb.append("Subject: ").append(subject).append("\n");
        }
        sb.append("Build: ").append(Build.FINGERPRINT).append("\n");
        if (Debug.isDebuggerConnected()) {
            sb.append("Debugger: Connected\n");
        }
        sb.append("\n");
        Thread worker = new Thread("Error dump: " + dropboxTag) {
            @Override
            public void run() throws Throwable {
                InputStreamReader input;
                if (report != null) {
                    sb.append(report);
                }
                String setting = "logcat_for_" + dropboxTag;
                int lines = Settings.Global.getInt(ActivityManagerService.this.mContext.getContentResolver(), setting, 0);
                int maxDataFileSize = (ActivityManagerService.DROPBOX_MAX_SIZE - sb.length()) - (lines * 100);
                if (dataFile != null && maxDataFileSize > 0) {
                    try {
                        sb.append(FileUtils.readTextFile(dataFile, maxDataFileSize, "\n\n[[TRUNCATED]]"));
                    } catch (IOException e) {
                        Slog.e(ActivityManagerService.TAG, "Error reading " + dataFile, e);
                    }
                }
                if (crashInfo != null && crashInfo.stackTrace != null) {
                    sb.append(crashInfo.stackTrace);
                }
                if (lines > 0) {
                    sb.append("\n");
                    InputStreamReader inputStreamReader = null;
                    try {
                        try {
                            Process logcat = new ProcessBuilder("/system/bin/timeout", "-k", "15s", "10s", "/system/bin/logcat", "-v", "time", "-b", "events", "-b", "system", "-b", "main", "-b", "crash", "-t", String.valueOf(lines)).redirectErrorStream(true).start();
                            try {
                                logcat.getOutputStream().close();
                            } catch (IOException e2) {
                            }
                            try {
                                logcat.getErrorStream().close();
                            } catch (IOException e3) {
                            }
                            input = new InputStreamReader(logcat.getInputStream());
                        } catch (IOException e4) {
                            e = e4;
                        }
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        char[] buf = new char[PackageManagerService.DumpState.DUMP_PREFERRED_XML];
                        while (true) {
                            int num = input.read(buf);
                            if (num <= 0) {
                                break;
                            } else {
                                sb.append(buf, 0, num);
                            }
                        }
                        if (input != null) {
                            try {
                                input.close();
                            } catch (IOException e5) {
                            }
                        }
                    } catch (IOException e6) {
                        e = e6;
                        inputStreamReader = input;
                        Slog.e(ActivityManagerService.TAG, "Error running logcat", e);
                        if (inputStreamReader != null) {
                            try {
                                inputStreamReader.close();
                            } catch (IOException e7) {
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        inputStreamReader = input;
                        if (inputStreamReader != null) {
                            try {
                                inputStreamReader.close();
                            } catch (IOException e8) {
                            }
                        }
                        throw th;
                    }
                }
                dbox.addText(dropboxTag, sb.toString());
                AMEventHookData.EndOfErrorDumpThread eventData = AMEventHookData.EndOfErrorDumpThread.createInstance();
                eventData.set(new Object[]{dropboxTag, sb.toString(), spid});
                ActivityManagerService.this.mAMEventHook.hook(AMEventHook.Event.AM_EndOfErrorDumpThread, eventData);
            }
        };
        if (process == null) {
            worker.run();
        } else {
            worker.start();
        }
    }

    public List<ActivityManager.ProcessErrorStateInfo> getProcessesInErrorState() throws Throwable {
        List<ActivityManager.ProcessErrorStateInfo> errList;
        enforceNotIsolatedCaller("getProcessesInErrorState");
        boolean allUsers = ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", Binder.getCallingUid()) == 0;
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int i = this.mLruProcesses.size() - 1;
                List<ActivityManager.ProcessErrorStateInfo> errList2 = null;
                while (i >= 0) {
                    try {
                        ProcessRecord app = this.mLruProcesses.get(i);
                        if (!allUsers && app.userId != userId) {
                            errList = errList2;
                        } else if (app.thread == null) {
                            errList = errList2;
                        } else if (app.crashing || app.notResponding) {
                            ActivityManager.ProcessErrorStateInfo report = null;
                            if (app.crashing) {
                                report = app.crashingReport;
                            } else if (app.notResponding) {
                                report = app.notRespondingReport;
                            }
                            if (report != null) {
                                errList = errList2 == null ? new ArrayList<>(1) : errList2;
                                errList.add(report);
                            } else {
                                Slog.w(TAG, "Missing app error report, app = " + app.processName + " crashing = " + app.crashing + " notResponding = " + app.notResponding);
                                errList = errList2;
                            }
                        } else {
                            errList = errList2;
                        }
                        i--;
                        errList2 = errList;
                    } catch (Throwable th) {
                        th = th;
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
                return errList2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    static int procStateToImportance(int procState, int memAdj, ActivityManager.RunningAppProcessInfo currApp) {
        int imp = ActivityManager.RunningAppProcessInfo.procStateToImportance(procState);
        if (imp == 400) {
            currApp.lru = memAdj;
        } else {
            currApp.lru = 0;
        }
        return imp;
    }

    private void fillInProcMemInfo(ProcessRecord app, ActivityManager.RunningAppProcessInfo outInfo) {
        outInfo.pid = app.pid;
        outInfo.uid = app.info.uid;
        if (this.mHeavyWeightProcess == app) {
            outInfo.flags |= 1;
        }
        if (app.persistent) {
            outInfo.flags |= 2;
        }
        if (app.activities.size() > 0) {
            outInfo.flags |= 4;
        }
        outInfo.lastTrimLevel = app.trimMemoryLevel;
        int adj = app.curAdj;
        int procState = app.curProcState;
        outInfo.importance = procStateToImportance(procState, adj, outInfo);
        outInfo.importanceReasonCode = app.adjTypeCode;
        outInfo.processState = app.curProcState;
    }

    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() throws Throwable {
        List<ActivityManager.RunningAppProcessInfo> runList;
        enforceNotIsolatedCaller("getRunningAppProcesses");
        int callingUid = Binder.getCallingUid();
        boolean allUsers = ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingUid) == 0;
        int userId = UserHandle.getUserId(callingUid);
        boolean allUids = isGetTasksAllowed("getRunningAppProcesses", Binder.getCallingPid(), callingUid);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int i = this.mLruProcesses.size() - 1;
                List<ActivityManager.RunningAppProcessInfo> runList2 = null;
                while (i >= 0) {
                    try {
                        ProcessRecord app = this.mLruProcesses.get(i);
                        if ((!allUsers && app.userId != userId) || (!allUids && app.uid != callingUid)) {
                            runList = runList2;
                        } else if (app.thread == null || app.crashing || app.notResponding) {
                            runList = runList2;
                        } else {
                            ActivityManager.RunningAppProcessInfo currApp = new ActivityManager.RunningAppProcessInfo(app.processName, app.pid, app.getPackageList());
                            fillInProcMemInfo(app, currApp);
                            if (app.adjSource instanceof ProcessRecord) {
                                currApp.importanceReasonPid = ((ProcessRecord) app.adjSource).pid;
                                currApp.importanceReasonImportance = ActivityManager.RunningAppProcessInfo.procStateToImportance(app.adjSourceProcState);
                            } else if (app.adjSource instanceof ActivityRecord) {
                                ActivityRecord r = (ActivityRecord) app.adjSource;
                                if (r.app != null) {
                                    currApp.importanceReasonPid = r.app.pid;
                                }
                            }
                            if (app.adjTarget instanceof ComponentName) {
                                currApp.importanceReasonComponent = (ComponentName) app.adjTarget;
                            }
                            runList = runList2 == null ? new ArrayList<>() : runList2;
                            runList.add(currApp);
                        }
                        i--;
                        runList2 = runList;
                    } catch (Throwable th) {
                        th = th;
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
                return runList2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public List<ApplicationInfo> getRunningExternalApplications() throws Throwable {
        enforceNotIsolatedCaller("getRunningExternalApplications");
        List<ActivityManager.RunningAppProcessInfo> runningApps = getRunningAppProcesses();
        List<ApplicationInfo> retList = new ArrayList<>();
        if (runningApps != null && runningApps.size() > 0) {
            Set<String> extList = new HashSet<>();
            for (ActivityManager.RunningAppProcessInfo app : runningApps) {
                if (app.pkgList != null) {
                    for (String pkg : app.pkgList) {
                        extList.add(pkg);
                    }
                }
            }
            IPackageManager pm = AppGlobals.getPackageManager();
            for (String pkg2 : extList) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg2, 0, UserHandle.getCallingUserId());
                    if ((info.flags & PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED) != 0) {
                        retList.add(info);
                    }
                } catch (RemoteException e) {
                }
            }
        }
        return retList;
    }

    public void getMyMemoryState(ActivityManager.RunningAppProcessInfo outInfo) {
        ProcessRecord proc;
        enforceNotIsolatedCaller("getMyMemoryState");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                synchronized (this.mPidsSelfLocked) {
                    proc = this.mPidsSelfLocked.get(Binder.getCallingPid());
                }
                fillInProcMemInfo(proc, outInfo);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int getMemoryTrimLevel() {
        int i;
        enforceNotIsolatedCaller("getMyMemoryState");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                i = this.mLastMemoryLevel;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return i;
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
        new ActivityManagerShellCommand(this, false).exec(this, in, out, err, args, resultReceiver);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ActiveServices.ServiceDumper sdumper;
        ActiveServices.ServiceDumper dumper;
        String name;
        String[] newArgs;
        String name2;
        String[] newArgs2;
        String opt;
        if (checkCallingPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ActivityManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        boolean dumpAll = false;
        boolean dumpClient = false;
        boolean dumpCheckin = false;
        boolean dumpCheckinFormat = false;
        String dumpPackage = null;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == FINISH_BOOTING_MSG) {
            opti++;
            if ("-a".equals(opt)) {
                dumpAll = true;
            } else if ("-c".equals(opt)) {
                dumpClient = true;
            } else if ("-p".equals(opt)) {
                if (opti >= args.length) {
                    pw.println("Error: -p option requires package argument");
                    return;
                } else {
                    dumpPackage = args[opti];
                    opti++;
                    dumpClient = true;
                }
            } else if ("--checkin".equals(opt)) {
                dumpCheckinFormat = true;
                dumpCheckin = true;
            } else if ("-C".equals(opt)) {
                dumpCheckinFormat = true;
            } else {
                if ("-h".equals(opt)) {
                    ActivityManagerShellCommand.dumpHelp(pw, true);
                    return;
                }
                pw.println("Unknown argument: " + opt + "; use -h for help");
            }
        }
        long origId = Binder.clearCallingIdentity();
        boolean more = false;
        if (opti < args.length) {
            String cmd = args[opti];
            opti++;
            if ("activities".equals(cmd) || "a".equals(cmd)) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpActivitiesLocked(fd, pw, args, opti, true, dumpClient, dumpPackage);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            } else if ("recents".equals(cmd) || "r".equals(cmd)) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpRecentsLocked(fd, pw, args, opti, true, dumpPackage);
                    } catch (Throwable th2) {
                        throw th2;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("broadcasts".equals(cmd) || "b".equals(cmd)) {
                if (opti >= args.length) {
                    String[] strArr = EMPTY_STRING_ARRAY;
                } else {
                    dumpPackage = args[opti];
                    opti++;
                    String[] newArgs3 = new String[args.length - opti];
                    if (args.length > 2) {
                        System.arraycopy(args, opti, newArgs3, 0, args.length - opti);
                    }
                }
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpBroadcastsLocked(fd, pw, args, opti, true, dumpPackage);
                    } catch (Throwable th3) {
                        throw th3;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("broadcast-stats".equals(cmd)) {
                if (opti >= args.length) {
                    String[] strArr2 = EMPTY_STRING_ARRAY;
                } else {
                    dumpPackage = args[opti];
                    opti++;
                    String[] newArgs4 = new String[args.length - opti];
                    if (args.length > 2) {
                        System.arraycopy(args, opti, newArgs4, 0, args.length - opti);
                    }
                }
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (dumpCheckinFormat) {
                            dumpBroadcastStatsCheckinLocked(fd, pw, args, opti, dumpCheckin, dumpPackage);
                        } else {
                            dumpBroadcastStatsLocked(fd, pw, args, opti, true, dumpPackage);
                        }
                    } finally {
                        resetPriorityAfterLockedSection();
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("intents".equals(cmd) || "i".equals(cmd)) {
                if (opti >= args.length) {
                    String[] strArr3 = EMPTY_STRING_ARRAY;
                } else {
                    dumpPackage = args[opti];
                    opti++;
                    String[] newArgs5 = new String[args.length - opti];
                    if (args.length > 2) {
                        System.arraycopy(args, opti, newArgs5, 0, args.length - opti);
                    }
                }
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpPendingIntentsLocked(fd, pw, args, opti, true, dumpPackage);
                    } catch (Throwable th4) {
                        throw th4;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("processes".equals(cmd) || "p".equals(cmd)) {
                if (opti >= args.length) {
                    String[] strArr4 = EMPTY_STRING_ARRAY;
                } else {
                    dumpPackage = args[opti];
                    opti++;
                    String[] newArgs6 = new String[args.length - opti];
                    if (args.length > 2) {
                        System.arraycopy(args, opti, newArgs6, 0, args.length - opti);
                    }
                }
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpProcessesLocked(fd, pw, args, opti, true, dumpPackage);
                    } catch (Throwable th5) {
                        throw th5;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("oom".equals(cmd) || "o".equals(cmd)) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpOomLocked(fd, pw, args, opti, true);
                    } catch (Throwable th6) {
                        throw th6;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("permissions".equals(cmd) || "perm".equals(cmd)) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpPermissionsLocked(fd, pw, args, opti, true, null);
                    } catch (Throwable th7) {
                        throw th7;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("provider".equals(cmd)) {
                if (opti >= args.length) {
                    name2 = null;
                    newArgs2 = EMPTY_STRING_ARRAY;
                } else {
                    name2 = args[opti];
                    opti++;
                    newArgs2 = new String[args.length - opti];
                    if (args.length > 2) {
                        System.arraycopy(args, opti, newArgs2, 0, args.length - opti);
                    }
                }
                if (!dumpProvider(fd, pw, name2, newArgs2, 0, dumpAll)) {
                    pw.println("No providers match: " + name2);
                    pw.println("Use -h for help.");
                }
            } else if ("providers".equals(cmd) || "prov".equals(cmd)) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpProvidersLocked(fd, pw, args, opti, true, null);
                    } catch (Throwable th8) {
                        throw th8;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("service".equals(cmd)) {
                if (opti >= args.length) {
                    name = null;
                    newArgs = EMPTY_STRING_ARRAY;
                } else {
                    name = args[opti];
                    opti++;
                    newArgs = new String[args.length - opti];
                    if (args.length > 2) {
                        System.arraycopy(args, opti, newArgs, 0, args.length - opti);
                    }
                }
                if (!this.mServices.dumpService(fd, pw, name, newArgs, 0, dumpAll)) {
                    pw.println("No services match: " + name);
                    pw.println("Use -h for help.");
                }
            } else if ("package".equals(cmd)) {
                if (opti >= args.length) {
                    pw.println("package: no package name specified");
                    pw.println("Use -h for help.");
                } else {
                    dumpPackage = args[opti];
                    int opti2 = opti + 1;
                    String[] newArgs7 = new String[args.length - opti2];
                    if (args.length > 2) {
                        System.arraycopy(args, opti2, newArgs7, 0, args.length - opti2);
                    }
                    args = newArgs7;
                    opti = 0;
                    more = true;
                }
            } else if ("associations".equals(cmd) || "as".equals(cmd)) {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        dumpAssociationsLocked(fd, pw, args, opti, true, dumpClient, dumpPackage);
                    } catch (Throwable th9) {
                        throw th9;
                    }
                }
                resetPriorityAfterLockedSection();
            } else if ("services".equals(cmd) || "s".equals(cmd)) {
                if (dumpClient) {
                    synchronized (this) {
                        try {
                            boostPriorityForLockedSection();
                            dumper = this.mServices.newServiceDumperLocked(fd, pw, args, opti, true, dumpPackage);
                        } catch (Throwable th10) {
                            throw th10;
                        }
                    }
                    resetPriorityAfterLockedSection();
                    dumper.dumpWithClient();
                } else {
                    synchronized (this) {
                        try {
                            boostPriorityForLockedSection();
                            this.mServices.newServiceDumperLocked(fd, pw, args, opti, true, dumpPackage).dumpLocked();
                        } catch (Throwable th11) {
                            throw th11;
                        }
                    }
                    resetPriorityAfterLockedSection();
                }
            } else if ("locks".equals(cmd)) {
                LockGuard.dump(fd, pw, args);
            } else if ("log".equals(cmd)) {
                configLogTag(pw, args, opti);
            } else if ("alog".equals(cmd)) {
                configActivityLogTag(pw, args, opti);
            } else if ("aal".equals(cmd)) {
                opti = AalUtils.getInstance(true).dump(pw, args, opti);
            } else if (!dumpActivity(fd, pw, cmd, args, opti, dumpAll)) {
                ActivityManagerShellCommand shell = new ActivityManagerShellCommand(this, true);
                int res = shell.exec(this, null, fd, null, args, new ResultReceiver(null));
                if (res < 0) {
                    pw.println("Bad activity command, or no activities match: " + cmd);
                    pw.println("Use -h for help.");
                }
            }
            if (!more) {
                Binder.restoreCallingIdentity(origId);
                return;
            }
        }
        if (dumpCheckinFormat) {
            dumpBroadcastStatsCheckinLocked(fd, pw, args, opti, dumpCheckin, dumpPackage);
        } else if (dumpClient) {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    dumpPendingIntentsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpBroadcastsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    if (dumpAll || dumpPackage != null) {
                        dumpBroadcastStatsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                    }
                    dumpProvidersLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpPermissionsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    sdumper = this.mServices.newServiceDumperLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                } catch (Throwable th12) {
                    throw th12;
                }
            }
            resetPriorityAfterLockedSection();
            sdumper.dumpWithClient();
            pw.println();
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpRecentsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpActivitiesLocked(fd, pw, args, opti, dumpAll, dumpClient, dumpPackage);
                    if (this.mAssociations.size() > 0) {
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpAssociationsLocked(fd, pw, args, opti, dumpAll, dumpClient, dumpPackage);
                    }
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpProcessesLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                } catch (Throwable th13) {
                    throw th13;
                }
            }
        } else {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    dumpPendingIntentsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpBroadcastsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    if (dumpAll || dumpPackage != null) {
                        dumpBroadcastStatsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                    }
                    dumpProvidersLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpPermissionsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    this.mServices.newServiceDumperLocked(fd, pw, args, opti, dumpAll, dumpPackage).dumpLocked();
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpRecentsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpActivitiesLocked(fd, pw, args, opti, dumpAll, dumpClient, dumpPackage);
                    if (this.mAssociations.size() > 0) {
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpAssociationsLocked(fd, pw, args, opti, dumpAll, dumpClient, dumpPackage);
                    }
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpProcessesLocked(fd, pw, args, opti, dumpAll, dumpPackage);
                } catch (Throwable th14) {
                    throw th14;
                }
            }
            resetPriorityAfterLockedSection();
        }
        Binder.restoreCallingIdentity(origId);
    }

    void dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        pw.println("ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)");
        boolean printedAnything = this.mStackSupervisor.dumpActivitiesLocked(fd, pw, dumpAll, dumpClient, dumpPackage);
        boolean needSep = printedAnything;
        boolean printed = ActivityStackSupervisor.printThisActivity(pw, this.mFocusedActivity, dumpPackage, printedAnything, "  mFocusedActivity: ");
        if (printed) {
            printedAnything = true;
            needSep = false;
        }
        if (dumpPackage == null) {
            if (needSep) {
                pw.println();
            }
            printedAnything = true;
            this.mStackSupervisor.dump(pw, "  ");
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    void dumpRecentsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        pw.println("ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)");
        boolean printedAnything = false;
        if (this.mRecentTasks != null && this.mRecentTasks.size() > 0) {
            boolean printedHeader = false;
            int N = this.mRecentTasks.size();
            for (int i = 0; i < N; i++) {
                TaskRecord tr = this.mRecentTasks.get(i);
                if (dumpPackage == null || (tr.realActivity != null && dumpPackage.equals(tr.realActivity))) {
                    if (!printedHeader) {
                        pw.println("  Recent tasks:");
                        printedHeader = true;
                        printedAnything = true;
                    }
                    pw.print("  * Recent #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(tr);
                    if (dumpAll) {
                        this.mRecentTasks.get(i).dump(pw, "    ");
                    }
                }
            }
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    void dumpAssociationsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        pw.println("ACTIVITY MANAGER ASSOCIATIONS (dumpsys activity associations)");
        int dumpUid = 0;
        if (dumpPackage != null) {
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                dumpUid = pm.getPackageUid(dumpPackage, PackageManagerService.DumpState.DUMP_PREFERRED_XML, 0);
            } catch (RemoteException e) {
            }
        }
        boolean printedAnything = false;
        long now = SystemClock.uptimeMillis();
        int N1 = this.mAssociations.size();
        for (int i1 = 0; i1 < N1; i1++) {
            ArrayMap<ComponentName, SparseArray<ArrayMap<String, Association>>> targetComponents = this.mAssociations.valueAt(i1);
            int N2 = targetComponents.size();
            for (int i2 = 0; i2 < N2; i2++) {
                SparseArray<ArrayMap<String, Association>> sourceUids = targetComponents.valueAt(i2);
                int N3 = sourceUids.size();
                for (int i3 = 0; i3 < N3; i3++) {
                    ArrayMap<String, Association> sourceProcesses = sourceUids.valueAt(i3);
                    int N4 = sourceProcesses.size();
                    for (int i4 = 0; i4 < N4; i4++) {
                        Association ass = sourceProcesses.valueAt(i4);
                        if (dumpPackage == null || ass.mTargetComponent.getPackageName().equals(dumpPackage) || UserHandle.getAppId(ass.mSourceUid) == dumpUid) {
                            printedAnything = true;
                            pw.print("  ");
                            pw.print(ass.mTargetProcess);
                            pw.print("/");
                            UserHandle.formatUid(pw, ass.mTargetUid);
                            pw.print(" <- ");
                            pw.print(ass.mSourceProcess);
                            pw.print("/");
                            UserHandle.formatUid(pw, ass.mSourceUid);
                            pw.println();
                            pw.print("    via ");
                            pw.print(ass.mTargetComponent.flattenToShortString());
                            pw.println();
                            pw.print("    ");
                            long dur = ass.mTime;
                            if (ass.mNesting > 0) {
                                dur += now - ass.mStartTime;
                            }
                            TimeUtils.formatDuration(dur, pw);
                            pw.print(" (");
                            pw.print(ass.mCount);
                            pw.print(" times)");
                            pw.print("  ");
                            for (int i = 0; i < ass.mStateTimes.length; i++) {
                                long amt = ass.mStateTimes[i];
                                if (ass.mLastState + 1 == i) {
                                    amt += now - ass.mLastStateUptime;
                                }
                                if (amt != 0) {
                                    pw.print(" ");
                                    pw.print(ProcessList.makeProcStateString(i - 1));
                                    pw.print("=");
                                    TimeUtils.formatDuration(amt, pw);
                                    if (ass.mLastState + 1 == i) {
                                        pw.print("*");
                                    }
                                }
                            }
                            pw.println();
                            if (ass.mNesting > 0) {
                                pw.print("    Currently active: ");
                                TimeUtils.formatDuration(now - ass.mStartTime, pw);
                                pw.println();
                            }
                        }
                    }
                }
            }
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    boolean dumpUids(PrintWriter pw, String dumpPackage, SparseArray<UidRecord> uids, String header, boolean needSep) {
        boolean printed = false;
        int whichAppId = -1;
        if (dumpPackage != null) {
            try {
                ApplicationInfo info = this.mContext.getPackageManager().getApplicationInfo(dumpPackage, 0);
                whichAppId = UserHandle.getAppId(info.uid);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < uids.size(); i++) {
            UidRecord uidRec = uids.valueAt(i);
            if (dumpPackage == null || UserHandle.getAppId(uidRec.uid) == whichAppId) {
                if (!printed) {
                    printed = true;
                    if (needSep) {
                        pw.println();
                    }
                    pw.print("  ");
                    pw.println(header);
                    needSep = true;
                }
                pw.print("    UID ");
                UserHandle.formatUid(pw, uidRec.uid);
                pw.print(": ");
                pw.println(uidRec);
            }
        }
        return printed;
    }

    void dumpProcessesLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        boolean needSep = false;
        boolean printedAnything = false;
        int numPers = 0;
        pw.println("ACTIVITY MANAGER RUNNING PROCESSES (dumpsys activity processes)");
        if (dumpAll) {
            int NP = this.mProcessNames.getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> procs = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                int NA = procs.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord r = procs.valueAt(ia);
                    if (dumpPackage == null || r.pkgList.containsKey(dumpPackage)) {
                        if (!needSep) {
                            pw.println("  All known processes:");
                            needSep = true;
                            printedAnything = true;
                        }
                        pw.print(r.persistent ? "  *PERS*" : "  *APP*");
                        pw.print(" UID ");
                        pw.print(procs.keyAt(ia));
                        pw.print(" ");
                        pw.println(r);
                        r.dump(pw, "    ");
                        if (r.persistent) {
                            numPers++;
                        }
                    }
                }
            }
        }
        if (this.mIsolatedProcesses.size() > 0) {
            boolean printed = false;
            for (int i = 0; i < this.mIsolatedProcesses.size(); i++) {
                ProcessRecord r2 = this.mIsolatedProcesses.valueAt(i);
                if (dumpPackage == null || r2.pkgList.containsKey(dumpPackage)) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        pw.println("  Isolated process list (sorted by uid):");
                        printedAnything = true;
                        printed = true;
                        needSep = true;
                    }
                    pw.println(String.format("%sIsolated #%2d: %s", "    ", Integer.valueOf(i), r2.toString()));
                }
            }
        }
        if (this.mActiveUids.size() > 0 && dumpUids(pw, dumpPackage, this.mActiveUids, "UID states:", needSep)) {
            needSep = true;
            printedAnything = true;
        }
        if (this.mValidateUids.size() > 0 && dumpUids(pw, dumpPackage, this.mValidateUids, "UID validation:", needSep)) {
            needSep = true;
            printedAnything = true;
        }
        if (this.mLruProcesses.size() > 0) {
            if (needSep) {
                pw.println();
            }
            pw.print("  Process LRU list (sorted by oom_adj, ");
            pw.print(this.mLruProcesses.size());
            pw.print(" total, non-act at ");
            pw.print(this.mLruProcesses.size() - this.mLruProcessActivityStart);
            pw.print(", non-svc at ");
            pw.print(this.mLruProcesses.size() - this.mLruProcessServiceStart);
            pw.println("):");
            dumpProcessOomList(pw, this, this.mLruProcesses, "    ", "Proc", "PERS", false, dumpPackage);
            needSep = true;
            printedAnything = true;
        }
        if (dumpAll || dumpPackage != null) {
            synchronized (this.mPidsSelfLocked) {
                boolean printed2 = false;
                for (int i2 = 0; i2 < this.mPidsSelfLocked.size(); i2++) {
                    ProcessRecord r3 = this.mPidsSelfLocked.valueAt(i2);
                    if (dumpPackage == null || r3.pkgList.containsKey(dumpPackage)) {
                        if (!printed2) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  PID mappings:");
                            printed2 = true;
                            printedAnything = true;
                        }
                        pw.print("    PID #");
                        pw.print(this.mPidsSelfLocked.keyAt(i2));
                        pw.print(": ");
                        pw.println(this.mPidsSelfLocked.valueAt(i2));
                    }
                }
            }
        }
        if (this.mForegroundProcesses.size() > 0) {
            synchronized (this.mPidsSelfLocked) {
                boolean printed3 = false;
                for (int i3 = 0; i3 < this.mForegroundProcesses.size(); i3++) {
                    ProcessRecord r4 = this.mPidsSelfLocked.get(this.mForegroundProcesses.valueAt(i3).pid);
                    if (dumpPackage == null || (r4 != null && r4.pkgList.containsKey(dumpPackage))) {
                        if (!printed3) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  Foreground Processes:");
                            printed3 = true;
                            printedAnything = true;
                        }
                        pw.print("    PID #");
                        pw.print(this.mForegroundProcesses.keyAt(i3));
                        pw.print(": ");
                        pw.println(this.mForegroundProcesses.valueAt(i3));
                    }
                }
            }
        }
        if (this.mPersistentStartingProcesses.size() > 0) {
            if (needSep) {
                pw.println();
            }
            needSep = true;
            printedAnything = true;
            pw.println("  Persisent processes that are starting:");
            dumpProcessList(pw, this, this.mPersistentStartingProcesses, "    ", "Starting Norm", "Restarting PERS", dumpPackage);
        }
        if (this.mRemovedProcesses.size() > 0) {
            if (needSep) {
                pw.println();
            }
            needSep = true;
            printedAnything = true;
            pw.println("  Processes that are being removed:");
            dumpProcessList(pw, this, this.mRemovedProcesses, "    ", "Removed Norm", "Removed PERS", dumpPackage);
        }
        if (this.mProcessesOnHold.size() > 0) {
            if (needSep) {
                pw.println();
            }
            needSep = true;
            printedAnything = true;
            pw.println("  Processes that are on old until the system is ready:");
            dumpProcessList(pw, this, this.mProcessesOnHold, "    ", "OnHold Norm", "OnHold PERS", dumpPackage);
        }
        boolean needSep2 = this.mAppErrors.dumpLocked(fd, pw, dumpProcessesToGc(fd, pw, args, opti, needSep, dumpAll, dumpPackage), dumpPackage);
        if (needSep2) {
            printedAnything = true;
        }
        if (dumpPackage == null) {
            pw.println();
            needSep2 = false;
            this.mUserController.dump(pw, dumpAll);
        }
        if (this.mHomeProcess != null && (dumpPackage == null || this.mHomeProcess.pkgList.containsKey(dumpPackage))) {
            if (needSep2) {
                pw.println();
                needSep2 = false;
            }
            pw.println("  mHomeProcess: " + this.mHomeProcess);
        }
        if (this.mPreviousProcess != null && (dumpPackage == null || this.mPreviousProcess.pkgList.containsKey(dumpPackage))) {
            if (needSep2) {
                pw.println();
                needSep2 = false;
            }
            pw.println("  mPreviousProcess: " + this.mPreviousProcess);
        }
        if (dumpAll) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("  mPreviousProcessVisibleTime: ");
            TimeUtils.formatDuration(this.mPreviousProcessVisibleTime, sb);
            pw.println(sb);
        }
        if (this.mHeavyWeightProcess != null && (dumpPackage == null || this.mHeavyWeightProcess.pkgList.containsKey(dumpPackage))) {
            if (needSep2) {
                pw.println();
                needSep2 = false;
            }
            pw.println("  mHeavyWeightProcess: " + this.mHeavyWeightProcess);
        }
        if (SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1") && this.mWallpaperProcess != null) {
            pw.println("  mWallpaperProcess: " + this.mWallpaperProcess + " fg: " + this.mIsWallpaperFg);
        }
        if (dumpPackage == null) {
            pw.println("  mConfiguration: " + this.mConfiguration);
        }
        if (dumpAll) {
            pw.println("  mConfigWillChange: " + getFocusedStack().mConfigWillChange);
            if (this.mCompatModePackages.getPackages().size() > 0) {
                boolean printed4 = false;
                for (Map.Entry<String, Integer> entry : this.mCompatModePackages.getPackages().entrySet()) {
                    String pkg = entry.getKey();
                    int mode = entry.getValue().intValue();
                    if (dumpPackage == null || dumpPackage.equals(pkg)) {
                        if (!printed4) {
                            pw.println("  mScreenCompatPackages:");
                            printed4 = true;
                        }
                        pw.print("    ");
                        pw.print(pkg);
                        pw.print(": ");
                        pw.print(mode);
                        pw.println();
                    }
                }
            }
        }
        if (dumpPackage == null) {
            pw.println("  mWakefulness=" + PowerManagerInternal.wakefulnessToString(this.mWakefulness));
            pw.println("  mSleepTokens=" + this.mSleepTokens);
            pw.println("  mSleeping=" + this.mSleeping + " mLockScreenShown=" + lockScreenShownToString());
            pw.println("  mShuttingDown=" + this.mShuttingDown + " mTestPssMode=" + this.mTestPssMode);
            if (this.mRunningVoice != null) {
                pw.println("  mRunningVoice=" + this.mRunningVoice);
                pw.println("  mVoiceWakeLock" + this.mVoiceWakeLock);
            }
        }
        if ((this.mDebugApp != null || this.mOrigDebugApp != null || this.mDebugTransient || this.mOrigWaitForDebugger) && (dumpPackage == null || dumpPackage.equals(this.mDebugApp) || dumpPackage.equals(this.mOrigDebugApp))) {
            if (needSep2) {
                pw.println();
                needSep2 = false;
            }
            pw.println("  mDebugApp=" + this.mDebugApp + "/orig=" + this.mOrigDebugApp + " mDebugTransient=" + this.mDebugTransient + " mOrigWaitForDebugger=" + this.mOrigWaitForDebugger);
        }
        if (this.mCurAppTimeTracker != null) {
            this.mCurAppTimeTracker.dumpWithHeader(pw, "  ", true);
        }
        if (this.mMemWatchProcesses.getMap().size() > 0) {
            pw.println("  Mem watch processes:");
            ArrayMap<String, SparseArray<Pair<Long, String>>> procs2 = this.mMemWatchProcesses.getMap();
            for (int i4 = 0; i4 < procs2.size(); i4++) {
                String proc = procs2.keyAt(i4);
                SparseArray<Pair<Long, String>> uids = procs2.valueAt(i4);
                for (int j = 0; j < uids.size(); j++) {
                    if (needSep2) {
                        pw.println();
                        needSep2 = false;
                    }
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("    ").append(proc).append('/');
                    UserHandle.formatUid(sb2, uids.keyAt(j));
                    Pair<Long, String> val = uids.valueAt(j);
                    sb2.append(": ");
                    DebugUtils.sizeValueToString(((Long) val.first).longValue(), sb2);
                    if (val.second != null) {
                        sb2.append(", report to ").append((String) val.second);
                    }
                    pw.println(sb2.toString());
                }
            }
            pw.print("  mMemWatchDumpProcName=");
            pw.println(this.mMemWatchDumpProcName);
            pw.print("  mMemWatchDumpFile=");
            pw.println(this.mMemWatchDumpFile);
            pw.print("  mMemWatchDumpPid=");
            pw.print(this.mMemWatchDumpPid);
            pw.print(" mMemWatchDumpUid=");
            pw.println(this.mMemWatchDumpUid);
        }
        if (this.mTrackAllocationApp != null && (dumpPackage == null || dumpPackage.equals(this.mTrackAllocationApp))) {
            if (needSep2) {
                pw.println();
                needSep2 = false;
            }
            pw.println("  mTrackAllocationApp=" + this.mTrackAllocationApp);
        }
        if ((this.mProfileApp != null || this.mProfileProc != null || this.mProfileFile != null || this.mProfileFd != null) && (dumpPackage == null || dumpPackage.equals(this.mProfileApp))) {
            if (needSep2) {
                pw.println();
                needSep2 = false;
            }
            pw.println("  mProfileApp=" + this.mProfileApp + " mProfileProc=" + this.mProfileProc);
            pw.println("  mProfileFile=" + this.mProfileFile + " mProfileFd=" + this.mProfileFd);
            pw.println("  mSamplingInterval=" + this.mSamplingInterval + " mAutoStopProfiler=" + this.mAutoStopProfiler);
            pw.println("  mProfileType=" + this.mProfileType);
        }
        if (this.mNativeDebuggingApp != null && (dumpPackage == null || dumpPackage.equals(this.mNativeDebuggingApp))) {
            if (needSep2) {
                pw.println();
            }
            pw.println("  mNativeDebuggingApp=" + this.mNativeDebuggingApp);
        }
        if (dumpPackage == null) {
            if (this.mAlwaysFinishActivities || this.mLenientBackgroundCheck) {
                pw.println("  mAlwaysFinishActivities=" + this.mAlwaysFinishActivities + " mLenientBackgroundCheck=" + this.mLenientBackgroundCheck);
            }
            if (this.mController != null) {
                pw.println("  mController=" + this.mController + " mControllerIsAMonkey=" + this.mControllerIsAMonkey);
            }
            if (dumpAll) {
                pw.println("  Total persistent processes: " + numPers);
                pw.println("  mProcessesReady=" + this.mProcessesReady + " mSystemReady=" + this.mSystemReady + " mBooted=" + this.mBooted + " mFactoryTest=" + this.mFactoryTest);
                pw.println("  mBooting=" + this.mBooting + " mCallFinishBooting=" + this.mCallFinishBooting + " mBootAnimationComplete=" + this.mBootAnimationComplete);
                pw.print("  mLastPowerCheckRealtime=");
                TimeUtils.formatDuration(this.mLastPowerCheckRealtime, pw);
                pw.println("");
                pw.print("  mLastPowerCheckUptime=");
                TimeUtils.formatDuration(this.mLastPowerCheckUptime, pw);
                pw.println("");
                pw.println("  mGoingToSleep=" + this.mStackSupervisor.mGoingToSleep);
                pw.println("  mLaunchingActivity=" + this.mStackSupervisor.mLaunchingActivity);
                pw.println("  mAdjSeq=" + this.mAdjSeq + " mLruSeq=" + this.mLruSeq);
                pw.println("  mNumNonCachedProcs=" + this.mNumNonCachedProcs + " (" + this.mLruProcesses.size() + " total) mNumCachedHiddenProcs=" + this.mNumCachedHiddenProcs + " mNumServiceProcs=" + this.mNumServiceProcs + " mNewNumServiceProcs=" + this.mNewNumServiceProcs);
                pw.println("  mAllowLowerMemLevel=" + this.mAllowLowerMemLevel + " mLastMemoryLevel=" + this.mLastMemoryLevel + " mLastNumProcesses=" + this.mLastNumProcesses);
                long now = SystemClock.uptimeMillis();
                pw.print("  mLastIdleTime=");
                TimeUtils.formatDuration(now, this.mLastIdleTime, pw);
                pw.print(" mLowRamSinceLastIdle=");
                TimeUtils.formatDuration(getLowRamTimeSinceIdle(now), pw);
                pw.println();
            }
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    boolean dumpProcessesToGc(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean needSep, boolean dumpAll, String dumpPackage) {
        if (this.mProcessesToGc.size() > 0) {
            boolean printed = false;
            long now = SystemClock.uptimeMillis();
            for (int i = 0; i < this.mProcessesToGc.size(); i++) {
                ProcessRecord proc = this.mProcessesToGc.get(i);
                if (dumpPackage == null || dumpPackage.equals(proc.info.packageName)) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Processes that are waiting to GC:");
                        printed = true;
                    }
                    pw.print("    Process ");
                    pw.println(proc);
                    pw.print("      lowMem=");
                    pw.print(proc.reportLowMemory);
                    pw.print(", last gced=");
                    pw.print(now - proc.lastRequestedGc);
                    pw.print(" ms ago, last lowMem=");
                    pw.print(now - proc.lastLowMemory);
                    pw.println(" ms ago");
                }
            }
        }
        return needSep;
    }

    void printOomLevel(PrintWriter pw, String name, int adj) {
        pw.print("    ");
        if (adj >= 0) {
            pw.print(' ');
            if (adj < 10) {
                pw.print(' ');
            }
        } else if (adj > -10) {
            pw.print(' ');
        }
        pw.print(adj);
        pw.print(": ");
        pw.print(name);
        pw.print(" (");
        pw.print(stringifySize(this.mProcessList.getMemLevel(adj), 1024));
        pw.println(")");
    }

    boolean dumpOomLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll) {
        boolean needSep = false;
        if (this.mLruProcesses.size() > 0) {
            if (0 != 0) {
                pw.println();
            }
            pw.println("  OOM levels:");
            printOomLevel(pw, "SYSTEM_ADJ", -900);
            printOomLevel(pw, "PERSISTENT_PROC_ADJ", -800);
            printOomLevel(pw, "PERSISTENT_SERVICE_ADJ", -700);
            printOomLevel(pw, "FOREGROUND_APP_ADJ", 0);
            printOomLevel(pw, "VISIBLE_APP_ADJ", 100);
            printOomLevel(pw, "PERCEPTIBLE_APP_ADJ", FIRST_BROADCAST_QUEUE_MSG);
            printOomLevel(pw, "BACKUP_APP_ADJ", FIRST_COMPAT_MODE_MSG);
            printOomLevel(pw, "HEAVY_WEIGHT_APP_ADJ", 400);
            printOomLevel(pw, "SERVICE_ADJ", 500);
            printOomLevel(pw, "HOME_APP_ADJ", 600);
            printOomLevel(pw, "PREVIOUS_APP_ADJ", 700);
            printOomLevel(pw, "SERVICE_B_ADJ", 800);
            printOomLevel(pw, "CACHED_APP_MIN_ADJ", 900);
            printOomLevel(pw, "CACHED_APP_MAX_ADJ", 906);
            if (1 != 0) {
                pw.println();
            }
            pw.print("  Process OOM control (");
            pw.print(this.mLruProcesses.size());
            pw.print(" total, non-act at ");
            pw.print(this.mLruProcesses.size() - this.mLruProcessActivityStart);
            pw.print(", non-svc at ");
            pw.print(this.mLruProcesses.size() - this.mLruProcessServiceStart);
            pw.println("):");
            dumpProcessOomList(pw, this, this.mLruProcesses, "    ", "Proc", "PERS", true, null);
            needSep = true;
        }
        dumpProcessesToGc(fd, pw, args, opti, needSep, dumpAll, null);
        pw.println();
        pw.println("  mHomeProcess: " + this.mHomeProcess);
        pw.println("  mPreviousProcess: " + this.mPreviousProcess);
        if (this.mHeavyWeightProcess != null) {
            pw.println("  mHeavyWeightProcess: " + this.mHeavyWeightProcess);
        }
        if (SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1") && this.mWallpaperProcess != null) {
            pw.println("  mWallpaperProcess: " + this.mWallpaperProcess + " fg: " + this.mIsWallpaperFg);
            return true;
        }
        return true;
    }

    protected boolean dumpProvider(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        return this.mProviderMap.dumpProvider(fd, pw, name, args, opti, dumpAll);
    }

    static class ItemMatcher {
        boolean all = true;
        ArrayList<ComponentName> components;
        ArrayList<Integer> objects;
        ArrayList<String> strings;

        ItemMatcher() {
        }

        void build(String name) {
            ComponentName componentName = ComponentName.unflattenFromString(name);
            if (componentName != null) {
                if (this.components == null) {
                    this.components = new ArrayList<>();
                }
                this.components.add(componentName);
                this.all = false;
                return;
            }
            try {
                int objectId = Integer.parseInt(name, 16);
                if (this.objects == null) {
                    this.objects = new ArrayList<>();
                }
                this.objects.add(Integer.valueOf(objectId));
                this.all = false;
            } catch (RuntimeException e) {
                if (this.strings == null) {
                    this.strings = new ArrayList<>();
                }
                this.strings.add(name);
                this.all = false;
            }
        }

        int build(String[] args, int opti) {
            while (opti < args.length) {
                String name = args[opti];
                if ("--".equals(name)) {
                    return opti + 1;
                }
                build(name);
                opti++;
            }
            return opti;
        }

        boolean match(Object object, ComponentName comp) {
            if (this.all) {
                return true;
            }
            if (this.components != null) {
                for (int i = 0; i < this.components.size(); i++) {
                    if (this.components.get(i).equals(comp)) {
                        return true;
                    }
                }
            }
            if (this.objects != null) {
                for (int i2 = 0; i2 < this.objects.size(); i2++) {
                    if (System.identityHashCode(object) == this.objects.get(i2).intValue()) {
                        return true;
                    }
                }
            }
            if (this.strings != null) {
                String flat = comp.flattenToString();
                for (int i3 = 0; i3 < this.strings.size(); i3++) {
                    if (flat.contains(this.strings.get(i3))) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }

    protected boolean dumpActivity(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<ActivityRecord> activities;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                activities = this.mStackSupervisor.getDumpActivitiesLocked(name);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (activities.size() <= 0) {
            return false;
        }
        String[] newArgs = new String[args.length - opti];
        System.arraycopy(args, opti, newArgs, 0, args.length - opti);
        TaskRecord lastTask = null;
        boolean needSep = false;
        for (int i = activities.size() - 1; i >= 0; i--) {
            ActivityRecord r = activities.get(i);
            if (needSep) {
                pw.println();
            }
            needSep = true;
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    if (lastTask != r.task) {
                        lastTask = r.task;
                        pw.print("TASK ");
                        pw.print(lastTask.affinity);
                        pw.print(" id=");
                        pw.println(lastTask.taskId);
                        if (dumpAll) {
                            lastTask.dump(pw, "  ");
                        }
                    }
                } catch (Throwable th2) {
                    resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
            resetPriorityAfterLockedSection();
            dumpActivity("  ", fd, pw, activities.get(i), newArgs, dumpAll);
        }
        return true;
    }

    private void dumpActivity(String prefix, FileDescriptor fd, PrintWriter pw, ActivityRecord r, String[] args, boolean dumpAll) {
        String innerPrefix = prefix + "  ";
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("ACTIVITY ");
                pw.print(r.shortComponentName);
                pw.print(" ");
                pw.print(Integer.toHexString(System.identityHashCode(r)));
                pw.print(" pid=");
                if (r.app != null) {
                    pw.println(r.app.pid);
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (r.app == null || r.app.thread == null) {
            return;
        }
        pw.flush();
        try {
            TransferPipe tp = new TransferPipe();
            try {
                r.app.thread.dumpActivity(tp.getWriteFd().getFileDescriptor(), r.appToken, innerPrefix, args);
                tp.go(fd);
            } finally {
                tp.kill();
            }
        } catch (RemoteException e) {
            pw.println(innerPrefix + "Got a RemoteException while dumping the activity");
        } catch (IOException e2) {
            pw.println(innerPrefix + "Failure while dumping the activity: " + e2);
        }
    }

    void dumpBroadcastsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        boolean needSep = false;
        boolean onlyHistory = false;
        boolean printedAnything = false;
        if ("history".equals(dumpPackage)) {
            if (opti < args.length && "-s".equals(args[opti])) {
                dumpAll = false;
            }
            onlyHistory = true;
            dumpPackage = null;
        }
        pw.println("ACTIVITY MANAGER BROADCAST STATE (dumpsys activity broadcasts)");
        if (!onlyHistory && dumpAll) {
            if (this.mRegisteredReceivers.size() > 0) {
                boolean printed = false;
                for (ReceiverList r : this.mRegisteredReceivers.values()) {
                    if (dumpPackage != null) {
                        if (r.app != null) {
                            if (dumpPackage.equals(r.app.info.packageName)) {
                            }
                        }
                    }
                    if (!printed) {
                        pw.println("  Registered Receivers:");
                        needSep = true;
                        printed = true;
                        printedAnything = true;
                    }
                    pw.print("  * ");
                    pw.println(r);
                    r.dump(pw, "    ");
                }
            }
            if (this.mReceiverResolver.dump(pw, needSep ? "\n  Receiver Resolver Table:" : "  Receiver Resolver Table:", "    ", dumpPackage, false, false)) {
                needSep = true;
                printedAnything = true;
            }
        }
        BroadcastQueue[] broadcastQueueArr = this.mBroadcastQueues;
        int i = 0;
        int length = broadcastQueueArr.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                break;
            }
            BroadcastQueue q = broadcastQueueArr[i2];
            needSep = q.dumpLocked(fd, pw, args, opti, dumpAll, dumpPackage, needSep);
            printedAnything |= needSep;
            i = i2 + 1;
        }
        boolean needSep2 = true;
        if (!onlyHistory && this.mStickyBroadcasts != null && dumpPackage == null) {
            for (int user = 0; user < this.mStickyBroadcasts.size(); user++) {
                if (needSep2) {
                    pw.println();
                }
                needSep2 = true;
                printedAnything = true;
                pw.print("  Sticky broadcasts for user ");
                pw.print(this.mStickyBroadcasts.keyAt(user));
                pw.println(":");
                StringBuilder sb = new StringBuilder(128);
                for (Map.Entry<String, ArrayList<Intent>> ent : this.mStickyBroadcasts.valueAt(user).entrySet()) {
                    pw.print("  * Sticky action ");
                    pw.print(ent.getKey());
                    if (dumpAll) {
                        pw.println(":");
                        ArrayList<Intent> intents = ent.getValue();
                        int N = intents.size();
                        for (int i3 = 0; i3 < N; i3++) {
                            sb.setLength(0);
                            sb.append("    Intent: ");
                            intents.get(i3).toShortString(sb, false, true, false, false);
                            pw.println(sb.toString());
                            Bundle bundle = intents.get(i3).getExtras();
                            if (bundle != null) {
                                pw.print("      ");
                                pw.println(bundle.toString());
                            }
                        }
                    } else {
                        pw.println("");
                    }
                }
            }
        }
        if (!onlyHistory && dumpAll) {
            pw.println();
            for (BroadcastQueue queue : this.mBroadcastQueues) {
                pw.println("  mBroadcastsScheduled [" + queue.mQueueName + "]=" + queue.mBroadcastsScheduled);
            }
            pw.println("  mHandler:");
            this.mHandler.dump(new PrintWriterPrinter(pw), "    ");
            printedAnything = true;
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    void dumpBroadcastStatsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        if (this.mCurBroadcastStats == null) {
            return;
        }
        pw.println("ACTIVITY MANAGER BROADCAST STATS STATE (dumpsys activity broadcast-stats)");
        long now = SystemClock.elapsedRealtime();
        if (this.mLastBroadcastStats != null) {
            pw.print("  Last stats (from ");
            TimeUtils.formatDuration(this.mLastBroadcastStats.mStartRealtime, now, pw);
            pw.print(" to ");
            TimeUtils.formatDuration(this.mLastBroadcastStats.mEndRealtime, now, pw);
            pw.print(", ");
            TimeUtils.formatDuration(this.mLastBroadcastStats.mEndUptime - this.mLastBroadcastStats.mStartUptime, pw);
            pw.println(" uptime):");
            if (!this.mLastBroadcastStats.dumpStats(pw, "    ", dumpPackage)) {
                pw.println("    (nothing)");
            }
            pw.println();
        }
        pw.print("  Current stats (from ");
        TimeUtils.formatDuration(this.mCurBroadcastStats.mStartRealtime, now, pw);
        pw.print(" to now, ");
        TimeUtils.formatDuration(SystemClock.uptimeMillis() - this.mCurBroadcastStats.mStartUptime, pw);
        pw.println(" uptime):");
        if (this.mCurBroadcastStats.dumpStats(pw, "    ", dumpPackage)) {
            return;
        }
        pw.println("    (nothing)");
    }

    void dumpBroadcastStatsCheckinLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean fullCheckin, String dumpPackage) {
        if (this.mCurBroadcastStats == null) {
            return;
        }
        if (this.mLastBroadcastStats != null) {
            this.mLastBroadcastStats.dumpCheckinStats(pw, dumpPackage);
            if (fullCheckin) {
                this.mLastBroadcastStats = null;
                return;
            }
        }
        this.mCurBroadcastStats.dumpCheckinStats(pw, dumpPackage);
        if (!fullCheckin) {
            return;
        }
        this.mCurBroadcastStats = null;
    }

    void dumpProvidersLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        ItemMatcher matcher = new ItemMatcher();
        matcher.build(args, opti);
        pw.println("ACTIVITY MANAGER CONTENT PROVIDERS (dumpsys activity providers)");
        boolean needSep = this.mProviderMap.dumpProvidersLocked(pw, dumpAll, dumpPackage);
        boolean printedAnything = needSep;
        if (this.mLaunchingProviders.size() > 0) {
            boolean printed = false;
            for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
                ContentProviderRecord r = this.mLaunchingProviders.get(i);
                if (dumpPackage == null || dumpPackage.equals(r.name.getPackageName())) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Launching content providers:");
                        printed = true;
                        printedAnything = true;
                    }
                    pw.print("  Launching #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(r);
                }
            }
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    void dumpPermissionsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        boolean needSep = false;
        boolean printedAnything = false;
        pw.println("ACTIVITY MANAGER URI PERMISSIONS (dumpsys activity permissions)");
        if (this.mGrantedUriPermissions.size() > 0) {
            boolean printed = false;
            int dumpUid = -2;
            if (dumpPackage != null) {
                try {
                    dumpUid = this.mContext.getPackageManager().getPackageUidAsUser(dumpPackage, PackageManagerService.DumpState.DUMP_PREFERRED_XML, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    dumpUid = -1;
                }
            }
            for (int i = 0; i < this.mGrantedUriPermissions.size(); i++) {
                int uid = this.mGrantedUriPermissions.keyAt(i);
                if (dumpUid < -1 || UserHandle.getAppId(uid) == dumpUid) {
                    ArrayMap<GrantUri, UriPermission> perms = this.mGrantedUriPermissions.valueAt(i);
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Granted Uri Permissions:");
                        printed = true;
                        printedAnything = true;
                    }
                    pw.print("  * UID ");
                    pw.print(uid);
                    pw.println(" holds:");
                    for (UriPermission perm : perms.values()) {
                        pw.print("    ");
                        pw.println(perm);
                        if (dumpAll) {
                            perm.dump(pw, "      ");
                        }
                    }
                }
            }
        }
        if (printedAnything) {
            return;
        }
        pw.println("  (nothing)");
    }

    void dumpPendingIntentsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        boolean printed = false;
        pw.println("ACTIVITY MANAGER PENDING INTENTS (dumpsys activity intents)");
        if (this.mIntentSenderRecords.size() > 0) {
            Iterator<WeakReference<PendingIntentRecord>> it = this.mIntentSenderRecords.values().iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> ref = it.next();
                PendingIntentRecord pendingIntentRecord = ref != null ? ref.get() : null;
                if (dumpPackage == null || (pendingIntentRecord != null && dumpPackage.equals(pendingIntentRecord.key.packageName))) {
                    printed = true;
                    if (pendingIntentRecord != null) {
                        pw.print("  * ");
                        pw.println(pendingIntentRecord);
                        if (dumpAll) {
                            pendingIntentRecord.dump(pw, "    ");
                        }
                    } else {
                        pw.print("  * ");
                        pw.println(ref);
                    }
                }
            }
        }
        if (printed) {
            return;
        }
        pw.println("  (nothing)");
    }

    private static final int dumpProcessList(PrintWriter pw, ActivityManagerService service, List list, String prefix, String normalLabel, String persistentLabel, String dumpPackage) {
        int numPers = 0;
        int N = list.size() - 1;
        for (int i = N; i >= 0; i--) {
            ProcessRecord r = (ProcessRecord) list.get(i);
            if (dumpPackage == null || dumpPackage.equals(r.info.packageName)) {
                Object[] objArr = new Object[4];
                objArr[0] = prefix;
                objArr[1] = r.persistent ? persistentLabel : normalLabel;
                objArr[2] = Integer.valueOf(i);
                objArr[3] = r.toString();
                pw.println(String.format("%s%s #%2d: %s", objArr));
                if (r.persistent) {
                    numPers++;
                }
            }
        }
        return numPers;
    }

    private static final boolean dumpProcessOomList(PrintWriter pw, ActivityManagerService service, List<ProcessRecord> origList, String prefix, String normalLabel, String persistentLabel, boolean inclDetails, String dumpPackage) {
        char schedGroup;
        char foreground;
        long wtime;
        ArrayList<Pair<ProcessRecord, Integer>> list = new ArrayList<>(origList.size());
        for (int i = 0; i < origList.size(); i++) {
            ProcessRecord r = origList.get(i);
            if (dumpPackage == null || r.pkgList.containsKey(dumpPackage)) {
                list.add(new Pair<>(origList.get(i), Integer.valueOf(i)));
            }
        }
        if (list.size() <= 0) {
            return false;
        }
        Comparator<Pair<ProcessRecord, Integer>> comparator = new Comparator<Pair<ProcessRecord, Integer>>() {
            @Override
            public int compare(Pair<ProcessRecord, Integer> object1, Pair<ProcessRecord, Integer> object2) {
                if (((ProcessRecord) object1.first).setAdj != ((ProcessRecord) object2.first).setAdj) {
                    return ((ProcessRecord) object1.first).setAdj > ((ProcessRecord) object2.first).setAdj ? -1 : 1;
                }
                if (((ProcessRecord) object1.first).setProcState != ((ProcessRecord) object2.first).setProcState) {
                    return ((ProcessRecord) object1.first).setProcState > ((ProcessRecord) object2.first).setProcState ? -1 : 1;
                }
                if (((Integer) object1.second).intValue() != ((Integer) object2.second).intValue()) {
                    return ((Integer) object1.second).intValue() > ((Integer) object2.second).intValue() ? -1 : 1;
                }
                return 0;
            }
        };
        Collections.sort(list, comparator);
        long curRealtime = SystemClock.elapsedRealtime();
        long realtimeSince = curRealtime - service.mLastPowerCheckRealtime;
        long curUptime = SystemClock.uptimeMillis();
        long uptimeSince = curUptime - service.mLastPowerCheckUptime;
        for (int i2 = list.size() - 1; i2 >= 0; i2--) {
            ProcessRecord r2 = (ProcessRecord) list.get(i2).first;
            String oomAdj = ProcessList.makeOomAdjString(r2.setAdj);
            switch (r2.setSchedGroup) {
                case 0:
                    schedGroup = 'B';
                    break;
                case 1:
                    schedGroup = 'F';
                    break;
                case 2:
                    schedGroup = 'T';
                    break;
                default:
                    schedGroup = '?';
                    break;
            }
            if (r2.foregroundActivities) {
                foreground = 'A';
            } else if (r2.foregroundServices) {
                foreground = 'S';
            } else {
                foreground = ' ';
            }
            String procState = ProcessList.makeProcStateString(r2.curProcState);
            pw.print(prefix);
            pw.print(r2.persistent ? persistentLabel : normalLabel);
            pw.print(" #");
            int num = (origList.size() - 1) - ((Integer) list.get(i2).second).intValue();
            if (num < 10) {
                pw.print(' ');
            }
            pw.print(num);
            pw.print(": ");
            pw.print(oomAdj);
            pw.print(' ');
            pw.print(schedGroup);
            pw.print('/');
            pw.print(foreground);
            pw.print('/');
            pw.print(procState);
            pw.print(" trm:");
            if (r2.trimMemoryLevel < 10) {
                pw.print(' ');
            }
            pw.print(r2.trimMemoryLevel);
            pw.print(' ');
            pw.print(r2.toShortString());
            pw.print(" (");
            pw.print(r2.adjType);
            pw.println(')');
            if (r2.adjSource != null || r2.adjTarget != null) {
                pw.print(prefix);
                pw.print("    ");
                if (r2.adjTarget instanceof ComponentName) {
                    pw.print(((ComponentName) r2.adjTarget).flattenToShortString());
                } else if (r2.adjTarget != null) {
                    pw.print(r2.adjTarget.toString());
                } else {
                    pw.print("{null}");
                }
                pw.print("<=");
                if (r2.adjSource instanceof ProcessRecord) {
                    pw.print("Proc{");
                    pw.print(((ProcessRecord) r2.adjSource).toShortString());
                    pw.println("}");
                } else if (r2.adjSource != null) {
                    pw.println(r2.adjSource.toString());
                } else {
                    pw.println("{null}");
                }
            }
            if (inclDetails) {
                pw.print(prefix);
                pw.print("    ");
                pw.print("oom: max=");
                pw.print(r2.maxAdj);
                pw.print(" curRaw=");
                pw.print(r2.curRawAdj);
                pw.print(" setRaw=");
                pw.print(r2.setRawAdj);
                pw.print(" cur=");
                pw.print(r2.curAdj);
                pw.print(" set=");
                pw.println(r2.setAdj);
                pw.print(prefix);
                pw.print("    ");
                pw.print("state: cur=");
                pw.print(ProcessList.makeProcStateString(r2.curProcState));
                pw.print(" set=");
                pw.print(ProcessList.makeProcStateString(r2.setProcState));
                pw.print(" lastPss=");
                DebugUtils.printSizeValue(pw, r2.lastPss * 1024);
                pw.print(" lastSwapPss=");
                DebugUtils.printSizeValue(pw, r2.lastSwapPss * 1024);
                pw.print(" lastCachedPss=");
                DebugUtils.printSizeValue(pw, r2.lastCachedPss * 1024);
                pw.println();
                pw.print(prefix);
                pw.print("    ");
                pw.print("cached=");
                pw.print(r2.cached);
                pw.print(" empty=");
                pw.print(r2.empty);
                pw.print(" hasAboveClient=");
                pw.println(r2.hasAboveClient);
                if (r2.setProcState < 10) {
                    continue;
                } else {
                    if (r2.lastWakeTime != 0) {
                        BatteryStatsImpl stats = service.mBatteryStatsService.getActiveStatistics();
                        synchronized (stats) {
                            wtime = stats.getProcessWakeTime(r2.info.uid, r2.pid, curRealtime);
                        }
                        long timeUsed = wtime - r2.lastWakeTime;
                        pw.print(prefix);
                        pw.print("    ");
                        pw.print("keep awake over ");
                        TimeUtils.formatDuration(realtimeSince, pw);
                        pw.print(" used ");
                        TimeUtils.formatDuration(timeUsed, pw);
                        pw.print(" (");
                        pw.print((100 * timeUsed) / realtimeSince);
                        pw.println("%)");
                    }
                    if (r2.lastCpuTime != 0) {
                        long timeUsed2 = r2.curCpuTime - r2.lastCpuTime;
                        pw.print(prefix);
                        pw.print("    ");
                        pw.print("run cpu over ");
                        TimeUtils.formatDuration(uptimeSince, pw);
                        pw.print(" used ");
                        TimeUtils.formatDuration(timeUsed2, pw);
                        pw.print(" (");
                        pw.print((100 * timeUsed2) / uptimeSince);
                        pw.println("%)");
                    }
                }
            }
        }
        return true;
    }

    ArrayList<ProcessRecord> collectProcesses(PrintWriter pw, int start, boolean allPkgs, String[] args) {
        ArrayList<ProcessRecord> procs;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (args == null || args.length <= start || args[start].charAt(0) == FINISH_BOOTING_MSG) {
                    procs = new ArrayList<>(this.mLruProcesses);
                } else {
                    procs = new ArrayList<>();
                    int pid = -1;
                    try {
                        pid = Integer.parseInt(args[start]);
                    } catch (NumberFormatException e) {
                    }
                    for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                        ProcessRecord proc = this.mLruProcesses.get(i);
                        if (proc.pid == pid) {
                            procs.add(proc);
                        } else if (allPkgs && proc.pkgList != null && proc.pkgList.containsKey(args[start])) {
                            procs.add(proc);
                        } else if (proc.processName.equals(args[start])) {
                            procs.add(proc);
                        }
                    }
                    if (procs.size() <= 0) {
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                }
                resetPriorityAfterLockedSection();
                return procs;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    final void dumpGraphicsHardwareUsage(FileDescriptor fd, PrintWriter pw, String[] args) {
        ArrayList<ProcessRecord> procs = collectProcesses(pw, 0, false, args);
        if (procs == null) {
            pw.println("No process found for: " + args[0]);
            return;
        }
        long uptime = SystemClock.uptimeMillis();
        long realtime = SystemClock.elapsedRealtime();
        pw.println("Applications Graphics Acceleration Info:");
        pw.println("Uptime: " + uptime + " Realtime: " + realtime);
        for (int i = procs.size() - 1; i >= 0; i--) {
            ProcessRecord r = procs.get(i);
            if (r.thread != null) {
                pw.println("\n** Graphics info for pid " + r.pid + " [" + r.processName + "] **");
                pw.flush();
                try {
                    TransferPipe tp = new TransferPipe();
                    try {
                        r.thread.dumpGfxInfo(tp.getWriteFd().getFileDescriptor(), args);
                        tp.go(fd);
                        tp.kill();
                    } catch (Throwable th) {
                        tp.kill();
                        throw th;
                    }
                } catch (RemoteException e) {
                    pw.println("Got a RemoteException while dumping the app " + r);
                    pw.flush();
                } catch (IOException e2) {
                    pw.println("Failure while dumping the app: " + r);
                    pw.flush();
                }
            }
        }
    }

    final void dumpDbInfo(FileDescriptor fd, PrintWriter pw, String[] args) {
        ArrayList<ProcessRecord> procs = collectProcesses(pw, 0, false, args);
        if (procs == null) {
            pw.println("No process found for: " + args[0]);
            return;
        }
        pw.println("Applications Database Info:");
        for (int i = procs.size() - 1; i >= 0; i--) {
            ProcessRecord r = procs.get(i);
            if (r.thread != null) {
                pw.println("\n** Database info for pid " + r.pid + " [" + r.processName + "] **");
                pw.flush();
                try {
                    TransferPipe tp = new TransferPipe();
                    try {
                        r.thread.dumpDbInfo(tp.getWriteFd().getFileDescriptor(), args);
                        tp.go(fd);
                        tp.kill();
                    } catch (Throwable th) {
                        tp.kill();
                        throw th;
                    }
                } catch (RemoteException e) {
                    pw.println("Got a RemoteException while dumping the app " + r);
                    pw.flush();
                } catch (IOException e2) {
                    pw.println("Failure while dumping the app: " + r);
                    pw.flush();
                }
            }
        }
    }

    static final class MemItem {
        final boolean hasActivities;
        final int id;
        final boolean isProc;
        final String label;
        final long pss;
        final String shortLabel;
        ArrayList<MemItem> subitems;
        final long swapPss;

        public MemItem(String _label, String _shortLabel, long _pss, long _swapPss, int _id, boolean _hasActivities) {
            this.isProc = true;
            this.label = _label;
            this.shortLabel = _shortLabel;
            this.pss = _pss;
            this.swapPss = _swapPss;
            this.id = _id;
            this.hasActivities = _hasActivities;
        }

        public MemItem(String _label, String _shortLabel, long _pss, long _swapPss, int _id) {
            this.isProc = false;
            this.label = _label;
            this.shortLabel = _shortLabel;
            this.pss = _pss;
            this.swapPss = _swapPss;
            this.id = _id;
            this.hasActivities = false;
        }
    }

    static final void dumpMemItems(PrintWriter pw, String prefix, String tag, ArrayList<MemItem> items, boolean sort, boolean isCompact, boolean dumpSwapPss) {
        if (sort && !isCompact) {
            Collections.sort(items, new Comparator<MemItem>() {
                @Override
                public int compare(MemItem lhs, MemItem rhs) {
                    if (lhs.pss < rhs.pss) {
                        return 1;
                    }
                    if (lhs.pss > rhs.pss) {
                        return -1;
                    }
                    return 0;
                }
            });
        }
        for (int i = 0; i < items.size(); i++) {
            MemItem mi = items.get(i);
            if (!isCompact) {
                if (dumpSwapPss) {
                    pw.printf("%s%s: %-60s (%s in swap)\n", prefix, stringifyKBSize(mi.pss), mi.label, stringifyKBSize(mi.swapPss));
                } else {
                    pw.printf("%s%s: %s\n", prefix, stringifyKBSize(mi.pss), mi.label);
                }
            } else if (mi.isProc) {
                pw.print("proc,");
                pw.print(tag);
                pw.print(",");
                pw.print(mi.shortLabel);
                pw.print(",");
                pw.print(mi.id);
                pw.print(",");
                pw.print(mi.pss);
                pw.print(",");
                pw.print(dumpSwapPss ? Long.valueOf(mi.swapPss) : "N/A");
                pw.println(mi.hasActivities ? ",a" : ",e");
            } else {
                pw.print(tag);
                pw.print(",");
                pw.print(mi.shortLabel);
                pw.print(",");
                pw.print(mi.pss);
                pw.print(",");
                pw.println(dumpSwapPss ? Long.valueOf(mi.swapPss) : "N/A");
            }
            if (mi.subitems != null) {
                dumpMemItems(pw, prefix + "    ", mi.shortLabel, mi.subitems, true, isCompact, dumpSwapPss);
            }
        }
    }

    static final void appendMemBucket(StringBuilder out, long memKB, String label, boolean stackLike) {
        int start = label.lastIndexOf(46);
        int start2 = start >= 0 ? start + 1 : 0;
        int end = label.length();
        for (int i = 0; i < DUMP_MEM_BUCKETS.length; i++) {
            if (DUMP_MEM_BUCKETS[i] >= memKB) {
                long bucket = DUMP_MEM_BUCKETS[i] / 1024;
                out.append(bucket);
                out.append(stackLike ? "MB." : "MB ");
                out.append((CharSequence) label, start2, end);
                return;
            }
        }
        out.append(memKB / 1024);
        out.append(stackLike ? "MB." : "MB ");
        out.append((CharSequence) label, start2, end);
    }

    private final void dumpApplicationMemoryUsageHeader(PrintWriter pw, long uptime, long realtime, boolean isCheckinRequest, boolean isCompact) {
        if (isCompact) {
            pw.print("version,");
            pw.println(1);
        }
        if (isCheckinRequest || isCompact) {
            pw.print("time,");
            pw.print(uptime);
            pw.print(",");
            pw.println(realtime);
            return;
        }
        pw.println("Applications Memory Usage (in Kilobytes):");
        pw.println("Uptime: " + uptime + " Realtime: " + realtime);
    }

    private final long[] getKsmInfo() {
        int[] SINGLE_LONG_FORMAT = {8224};
        long[] longTmp = {0};
        Process.readProcFile("/sys/kernel/mm/ksm/pages_shared", SINGLE_LONG_FORMAT, null, longTmp, null);
        Process.readProcFile("/sys/kernel/mm/ksm/pages_sharing", SINGLE_LONG_FORMAT, null, longTmp, null);
        longTmp[0] = 0;
        Process.readProcFile("/sys/kernel/mm/ksm/pages_unshared", SINGLE_LONG_FORMAT, null, longTmp, null);
        longTmp[0] = 0;
        Process.readProcFile("/sys/kernel/mm/ksm/pages_volatile", SINGLE_LONG_FORMAT, null, longTmp, null);
        long[] longOut = {(longTmp[0] * 4096) / 1024, (longTmp[0] * 4096) / 1024, (longTmp[0] * 4096) / 1024, (longTmp[0] * 4096) / 1024};
        return longOut;
    }

    private static String stringifySize(long size, int order) {
        Locale locale = Locale.US;
        switch (order) {
            case 1:
                return String.format(locale, "%,13d", Long.valueOf(size));
            case 1024:
                return String.format(locale, "%,9dK", Long.valueOf(size / 1024));
            case PackageManagerService.DumpState.DUMP_DEXOPT:
                return String.format(locale, "%,5dM", Long.valueOf((size / 1024) / 1024));
            case 1073741824:
                return String.format(locale, "%,1dG", Long.valueOf(((size / 1024) / 1024) / 1024));
            default:
                throw new IllegalArgumentException("Invalid size order");
        }
    }

    private static String stringifyKBSize(long size) {
        return stringifySize(1024 * size, 1024);
    }

    final void dumpApplicationMemoryUsage(FileDescriptor fd, PrintWriter pw, String prefix, String[] args, boolean brief, PrintWriter categoryPw) throws Throwable {
        Debug.MemoryInfo mi;
        IApplicationThread thread;
        int pid;
        int oomAdj;
        boolean hasActivities;
        String opt;
        boolean dumpDetails = false;
        boolean dumpFullDetails = false;
        boolean dumpDalvik = false;
        boolean dumpSummaryOnly = false;
        boolean dumpUnreachable = false;
        boolean oomOnly = false;
        boolean isCompact = false;
        boolean localOnly = false;
        boolean packages = false;
        boolean isCheckinRequest = false;
        boolean dumpSwapPss = false;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == FINISH_BOOTING_MSG) {
            opti++;
            if ("-a".equals(opt)) {
                dumpDetails = true;
                dumpFullDetails = true;
                dumpDalvik = true;
                dumpSwapPss = true;
            } else if ("-d".equals(opt)) {
                dumpDalvik = true;
            } else if ("-c".equals(opt)) {
                isCompact = true;
            } else if ("-s".equals(opt)) {
                dumpDetails = true;
                dumpSummaryOnly = true;
            } else if ("-S".equals(opt)) {
                dumpSwapPss = true;
            } else if ("--unreachable".equals(opt)) {
                dumpUnreachable = true;
            } else if ("--oom".equals(opt)) {
                oomOnly = true;
            } else if ("--local".equals(opt)) {
                localOnly = true;
            } else if ("--package".equals(opt)) {
                packages = true;
            } else if ("--checkin".equals(opt)) {
                isCheckinRequest = true;
            } else {
                if ("-h".equals(opt)) {
                    pw.println("meminfo dump options: [-a] [-d] [-c] [-s] [--oom] [process]");
                    pw.println("  -a: include all available information for each process.");
                    pw.println("  -d: include dalvik details.");
                    pw.println("  -c: dump in a compact machine-parseable representation.");
                    pw.println("  -s: dump only summary of application memory usage.");
                    pw.println("  -S: dump also SwapPss.");
                    pw.println("  --oom: only show processes organized by oom adj.");
                    pw.println("  --local: only collect details locally, don't call process.");
                    pw.println("  --package: interpret process arg as package, dumping all");
                    pw.println("             processes that have loaded that package.");
                    pw.println("  --checkin: dump data for a checkin");
                    pw.println("If [process] is specified it can be the name or ");
                    pw.println("pid of a specific process to dump.");
                    return;
                }
                pw.println("Unknown argument: " + opt + "; use -h for help");
            }
        }
        long uptime = SystemClock.uptimeMillis();
        long realtime = SystemClock.elapsedRealtime();
        long[] tmpLong = new long[1];
        ArrayList<ProcessRecord> procs = collectProcesses(pw, opti, packages, args);
        if (procs == null) {
            if (args != null && args.length > opti && args[opti].charAt(0) != FINISH_BOOTING_MSG) {
                ArrayList<ProcessCpuTracker.Stats> nativeProcs = new ArrayList<>();
                updateCpuStatsNow();
                int findPid = -1;
                try {
                    findPid = Integer.parseInt(args[opti]);
                } catch (NumberFormatException e) {
                }
                synchronized (this.mProcessCpuTracker) {
                    int N = this.mProcessCpuTracker.countStats();
                    for (int i = 0; i < N; i++) {
                        ProcessCpuTracker.Stats st = this.mProcessCpuTracker.getStats(i);
                        if (st.pid == findPid || (st.baseName != null && st.baseName.equals(args[opti]))) {
                            nativeProcs.add(st);
                        }
                    }
                }
                if (nativeProcs.size() > 0) {
                    dumpApplicationMemoryUsageHeader(pw, uptime, realtime, isCheckinRequest, isCompact);
                    Debug.MemoryInfo mi2 = null;
                    for (int i2 = nativeProcs.size() - 1; i2 >= 0; i2--) {
                        ProcessCpuTracker.Stats r = nativeProcs.get(i2);
                        int pid2 = r.pid;
                        if (!isCheckinRequest && dumpDetails) {
                            pw.println("\n** MEMINFO in pid " + pid2 + " [" + r.baseName + "] **");
                        }
                        if (mi2 == null) {
                            mi2 = new Debug.MemoryInfo();
                        }
                        if (dumpDetails || !(brief || oomOnly)) {
                            Debug.getMemoryInfo(pid2, mi2);
                        } else {
                            mi2.dalvikPss = (int) Debug.getPss(pid2, tmpLong, null);
                            mi2.dalvikPrivateDirty = (int) tmpLong[0];
                        }
                        ActivityThread.dumpMemInfoTable(pw, mi2, isCheckinRequest, dumpFullDetails, dumpDalvik, dumpSummaryOnly, pid2, r.baseName, 0L, 0L, 0L, 0L, 0L, 0L);
                        if (isCheckinRequest) {
                            pw.println();
                        }
                    }
                    return;
                }
            }
            pw.println("No process found for: " + args[opti]);
            return;
        }
        if (!brief && !oomOnly && (procs.size() == 1 || isCheckinRequest || packages)) {
            dumpDetails = true;
        }
        dumpApplicationMemoryUsageHeader(pw, uptime, realtime, isCheckinRequest, isCompact);
        String[] innerArgs = new String[args.length - opti];
        System.arraycopy(args, opti, innerArgs, 0, args.length - opti);
        ArrayList<MemItem> procMems = new ArrayList<>();
        SparseArray<MemItem> procMemsMap = new SparseArray<>();
        long nativePss = 0;
        long nativeSwapPss = 0;
        long dalvikPss = 0;
        long dalvikSwapPss = 0;
        long[] dalvikSubitemPss = dumpDalvik ? new long[8] : EmptyArray.LONG;
        long[] dalvikSubitemSwapPss = dumpDalvik ? new long[8] : EmptyArray.LONG;
        long otherPss = 0;
        long otherSwapPss = 0;
        long[] miscPss = new long[17];
        long[] miscSwapPss = new long[17];
        long[] oomPss = new long[DUMP_MEM_OOM_LABEL.length];
        long[] oomSwapPss = new long[DUMP_MEM_OOM_LABEL.length];
        ArrayList<MemItem>[] oomProcs = new ArrayList[DUMP_MEM_OOM_LABEL.length];
        long totalPss = 0;
        long totalSwapPss = 0;
        long cachedPss = 0;
        long cachedSwapPss = 0;
        boolean hasSwapPss = false;
        Debug.MemoryInfo mi3 = null;
        for (int i3 = procs.size() - 1; i3 >= 0; i3--) {
            ProcessRecord r2 = procs.get(i3);
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    thread = r2.thread;
                    pid = r2.pid;
                    oomAdj = r2.getSetAdjWithServices();
                    hasActivities = r2.activities.size() > 0;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            if (thread != null) {
                if (!isCheckinRequest && dumpDetails) {
                    pw.println("\n** MEMINFO in pid " + pid + " [" + r2.processName + "] **");
                }
                if (mi3 == null) {
                    mi3 = new Debug.MemoryInfo();
                }
                if (dumpDetails || !(brief || oomOnly)) {
                    Debug.getMemoryInfo(pid, mi3);
                    hasSwapPss = mi3.hasSwappedOutPss;
                } else {
                    mi3.dalvikPss = (int) Debug.getPss(pid, tmpLong, null);
                    mi3.dalvikPrivateDirty = (int) tmpLong[0];
                }
                if (dumpDetails) {
                    if (localOnly) {
                        ActivityThread.dumpMemInfoTable(pw, mi3, isCheckinRequest, dumpFullDetails, dumpDalvik, dumpSummaryOnly, pid, r2.processName, 0L, 0L, 0L, 0L, 0L, 0L);
                        if (isCheckinRequest) {
                            pw.println();
                        }
                    } else {
                        try {
                            pw.flush();
                            thread.dumpMemInfo(fd, mi3, isCheckinRequest, dumpFullDetails, dumpDalvik, dumpSummaryOnly, dumpUnreachable, innerArgs);
                        } catch (RemoteException e2) {
                            if (!isCheckinRequest) {
                                pw.println("Got RemoteException!");
                                pw.flush();
                            }
                        }
                    }
                }
                long myTotalPss = mi3.getTotalPss();
                long myTotalUss = mi3.getTotalUss();
                long myTotalSwapPss = mi3.getTotalSwappedOutPss();
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (r2.thread != null && oomAdj == r2.getSetAdjWithServices()) {
                            r2.baseProcessTracker.addPss(myTotalPss, myTotalUss, true, r2.pkgList);
                        }
                    } catch (Throwable th2) {
                        resetPriorityAfterLockedSection();
                        throw th2;
                    }
                }
                resetPriorityAfterLockedSection();
                if (!isCheckinRequest && mi3 != null) {
                    totalPss += myTotalPss;
                    totalSwapPss += myTotalSwapPss;
                    MemItem pssItem = new MemItem(r2.processName + " (pid " + pid + (hasActivities ? " / activities)" : ")"), r2.processName, myTotalPss, myTotalSwapPss, pid, hasActivities);
                    procMems.add(pssItem);
                    procMemsMap.put(pid, pssItem);
                    nativePss += (long) mi3.nativePss;
                    nativeSwapPss += (long) mi3.nativeSwappedOutPss;
                    dalvikPss += (long) mi3.dalvikPss;
                    dalvikSwapPss += (long) mi3.dalvikSwappedOutPss;
                    for (int j = 0; j < dalvikSubitemPss.length; j++) {
                        dalvikSubitemPss[j] = dalvikSubitemPss[j] + ((long) mi3.getOtherPss(j + 17));
                        dalvikSubitemSwapPss[j] = dalvikSubitemSwapPss[j] + ((long) mi3.getOtherSwappedOutPss(j + 17));
                    }
                    otherPss += (long) mi3.otherPss;
                    otherSwapPss += (long) mi3.otherSwappedOutPss;
                    for (int j2 = 0; j2 < 17; j2++) {
                        long mem = mi3.getOtherPss(j2);
                        miscPss[j2] = miscPss[j2] + mem;
                        otherPss -= mem;
                        long mem2 = mi3.getOtherSwappedOutPss(j2);
                        miscSwapPss[j2] = miscSwapPss[j2] + mem2;
                        otherSwapPss -= mem2;
                    }
                    if (oomAdj >= 900) {
                        cachedPss += myTotalPss;
                        cachedSwapPss += myTotalSwapPss;
                    }
                    for (int oomIndex = 0; oomIndex < oomPss.length; oomIndex++) {
                        if (oomIndex == oomPss.length - 1 || (oomAdj >= DUMP_MEM_OOM_ADJ[oomIndex] && oomAdj < DUMP_MEM_OOM_ADJ[oomIndex + 1])) {
                            oomPss[oomIndex] = oomPss[oomIndex] + myTotalPss;
                            oomSwapPss[oomIndex] = oomSwapPss[oomIndex] + myTotalSwapPss;
                            if (oomProcs[oomIndex] == null) {
                                oomProcs[oomIndex] = new ArrayList<>();
                            }
                            oomProcs[oomIndex].add(pssItem);
                        }
                    }
                }
            }
        }
        long nativeProcTotalPss = 0;
        if (isCheckinRequest || procs.size() <= 1 || packages) {
            return;
        }
        updateCpuStatsNow();
        synchronized (this.mProcessCpuTracker) {
            try {
                int N2 = this.mProcessCpuTracker.countStats();
                int i4 = 0;
                Debug.MemoryInfo mi4 = null;
                while (i4 < N2) {
                    try {
                        ProcessCpuTracker.Stats st2 = this.mProcessCpuTracker.getStats(i4);
                        if (st2.vsize <= 0 || procMemsMap.indexOfKey(st2.pid) >= 0) {
                            mi = mi4;
                        } else {
                            mi = mi4 == null ? new Debug.MemoryInfo() : mi4;
                            if (brief || oomOnly) {
                                mi.nativePss = (int) Debug.getPss(st2.pid, tmpLong, null);
                                mi.nativePrivateDirty = (int) tmpLong[0];
                            } else {
                                Debug.getMemoryInfo(st2.pid, mi);
                            }
                            long myTotalPss2 = mi.getTotalPss();
                            long myTotalSwapPss2 = mi.getTotalSwappedOutPss();
                            totalPss += myTotalPss2;
                            nativeProcTotalPss += myTotalPss2;
                            MemItem pssItem2 = new MemItem(st2.name + " (pid " + st2.pid + ")", st2.name, myTotalPss2, mi.getSummaryTotalSwapPss(), st2.pid, false);
                            procMems.add(pssItem2);
                            nativePss += (long) mi.nativePss;
                            nativeSwapPss += (long) mi.nativeSwappedOutPss;
                            dalvikPss += (long) mi.dalvikPss;
                            dalvikSwapPss += (long) mi.dalvikSwappedOutPss;
                            for (int j3 = 0; j3 < dalvikSubitemPss.length; j3++) {
                                dalvikSubitemPss[j3] = dalvikSubitemPss[j3] + ((long) mi.getOtherPss(j3 + 17));
                                dalvikSubitemSwapPss[j3] = dalvikSubitemSwapPss[j3] + ((long) mi.getOtherSwappedOutPss(j3 + 17));
                            }
                            otherPss += (long) mi.otherPss;
                            otherSwapPss += (long) mi.otherSwappedOutPss;
                            for (int j4 = 0; j4 < 17; j4++) {
                                long mem3 = mi.getOtherPss(j4);
                                miscPss[j4] = miscPss[j4] + mem3;
                                otherPss -= mem3;
                                long mem4 = mi.getOtherSwappedOutPss(j4);
                                miscSwapPss[j4] = miscSwapPss[j4] + mem4;
                                otherSwapPss -= mem4;
                            }
                            oomPss[0] = oomPss[0] + myTotalPss2;
                            oomSwapPss[0] = oomSwapPss[0] + myTotalSwapPss2;
                            if (oomProcs[0] == null) {
                                oomProcs[0] = new ArrayList<>();
                            }
                            oomProcs[0].add(pssItem2);
                        }
                        i4++;
                        mi4 = mi;
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
                ArrayList<MemItem> catMems = new ArrayList<>();
                catMems.add(new MemItem("Native", "Native", nativePss, nativeSwapPss, -1));
                MemItem dalvikItem = new MemItem("Dalvik", "Dalvik", dalvikPss, dalvikSwapPss, -2);
                if (dalvikSubitemPss.length > 0) {
                    dalvikItem.subitems = new ArrayList<>();
                    for (int j5 = 0; j5 < dalvikSubitemPss.length; j5++) {
                        String name = Debug.MemoryInfo.getOtherLabel(j5 + 17);
                        dalvikItem.subitems.add(new MemItem(name, name, dalvikSubitemPss[j5], dalvikSubitemSwapPss[j5], j5));
                    }
                }
                catMems.add(dalvikItem);
                catMems.add(new MemItem("Unknown", "Unknown", otherPss, otherSwapPss, -3));
                for (int j6 = 0; j6 < 17; j6++) {
                    String label = Debug.MemoryInfo.getOtherLabel(j6);
                    catMems.add(new MemItem(label, label, miscPss[j6], miscSwapPss[j6], j6));
                }
                ArrayList<MemItem> oomMems = new ArrayList<>();
                for (int j7 = 0; j7 < oomPss.length; j7++) {
                    if (oomPss[j7] != 0) {
                        String label2 = isCompact ? DUMP_MEM_OOM_COMPACT_LABEL[j7] : DUMP_MEM_OOM_LABEL[j7];
                        MemItem item = new MemItem(label2, label2, oomPss[j7], oomSwapPss[j7], DUMP_MEM_OOM_ADJ[j7]);
                        item.subitems = oomProcs[j7];
                        oomMems.add(item);
                    }
                }
                boolean dumpSwapPss2 = dumpSwapPss && hasSwapPss && totalSwapPss != 0;
                if (!brief && !oomOnly && !isCompact) {
                    pw.println();
                    pw.println("Total PSS by process:");
                    dumpMemItems(pw, "  ", "proc", procMems, true, isCompact, dumpSwapPss2);
                    pw.println();
                }
                if (!isCompact) {
                    pw.println("Total PSS by OOM adjustment:");
                }
                dumpMemItems(pw, "  ", "oom", oomMems, false, isCompact, dumpSwapPss2);
                if (!brief && !oomOnly) {
                    PrintWriter out = categoryPw != null ? categoryPw : pw;
                    if (!isCompact) {
                        out.println();
                        out.println("Total PSS by category:");
                    }
                    dumpMemItems(out, "  ", "cat", catMems, true, isCompact, dumpSwapPss2);
                }
                if (!isCompact) {
                    pw.println();
                }
                MemInfoReader memInfo = new MemInfoReader();
                memInfo.readMemInfo();
                memInfo.readExtraMemInfo();
                if (nativeProcTotalPss > 0) {
                    synchronized (this) {
                        try {
                            boostPriorityForLockedSection();
                            long cachedKb = memInfo.getCachedSizeKb();
                            long freeKb = memInfo.getFreeSizeKb();
                            long zramKb = memInfo.getZramTotalSizeKb();
                            long kernelKb = memInfo.getKernelUsedSizeKb();
                            EventLogTags.writeAmMeminfo(cachedKb * 1024, freeKb * 1024, zramKb * 1024, kernelKb * 1024, nativeProcTotalPss * 1024);
                            this.mProcessStats.addSysMemUsageLocked(cachedKb, freeKb, zramKb, kernelKb, nativeProcTotalPss);
                        } catch (Throwable th4) {
                            resetPriorityAfterLockedSection();
                            throw th4;
                        }
                    }
                    resetPriorityAfterLockedSection();
                }
                if (!brief) {
                    if (isCompact) {
                        pw.print("ram,");
                        pw.print(memInfo.getTotalSizeKb());
                        pw.print(",");
                        pw.print(memInfo.getCachedSizeKb() + cachedPss + memInfo.getFreeSizeKb());
                        pw.print(",");
                        pw.println(totalPss - cachedPss);
                    } else {
                        pw.print("Total RAM: ");
                        pw.print(stringifyKBSize(memInfo.getTotalSizeKb()));
                        pw.print(" (status ");
                        switch (this.mLastMemoryLevel) {
                            case 0:
                                pw.println("normal)");
                                break;
                            case 1:
                                pw.println("moderate)");
                                break;
                            case 2:
                                pw.println("low)");
                                break;
                            case 3:
                                pw.println("critical)");
                                break;
                            default:
                                pw.print(this.mLastMemoryLevel);
                                pw.println(")");
                                break;
                        }
                        pw.print(" Free RAM: ");
                        pw.print(stringifyKBSize(memInfo.getCachedSizeKb() + cachedPss + (memInfo.getRawInfo()[13] * 4) + (memInfo.getRawInfo()[14] * 4) + memInfo.getFreeSizeKb()));
                        pw.print(" (");
                        pw.print(stringifyKBSize(cachedPss));
                        pw.print(" cached pss + ");
                        pw.print(stringifyKBSize(memInfo.getCachedSizeKb()));
                        pw.print(" cached kernel + ");
                        pw.print(stringifyKBSize(memInfo.getFreeSizeKb()));
                        pw.print(" free + ");
                        pw.print(stringifyKBSize(memInfo.getRawInfo()[13] * 4));
                        pw.print(" ion cached + ");
                        pw.print(stringifyKBSize(memInfo.getRawInfo()[14] * 4));
                        pw.println(" gpu cached)");
                    }
                }
                long lostRAM = (((((((((memInfo.getTotalSizeKb() - (totalPss - totalSwapPss)) - memInfo.getFreeSizeKb()) - memInfo.getCachedSizeKb()) - (memInfo.getRawInfo()[13] * 4)) - (memInfo.getRawInfo()[14] * 4)) - memInfo.getRawInfo()[16]) - memInfo.getRawInfo()[15]) - memInfo.getRawInfo()[17]) - memInfo.getKernelUsedSizeKb()) - memInfo.getZramTotalSizeKb();
                if (isCompact) {
                    pw.print("lostram,");
                    pw.println(lostRAM);
                } else {
                    pw.print(" Used RAM: ");
                    pw.print(stringifyKBSize((totalPss - cachedPss) + memInfo.getRawInfo()[16] + memInfo.getRawInfo()[15] + memInfo.getRawInfo()[17] + memInfo.getKernelUsedSizeKb()));
                    pw.print(" (");
                    pw.print(stringifyKBSize(totalPss - cachedPss));
                    pw.print(" used pss + ");
                    pw.print(stringifyKBSize(memInfo.getKernelUsedSizeKb()));
                    pw.print(" kernel + ");
                    pw.print(stringifyKBSize(memInfo.getRawInfo()[16]));
                    pw.print(" trace buffer + ");
                    pw.print(stringifyKBSize(memInfo.getRawInfo()[15]));
                    pw.print(" ion disp + ");
                    pw.print(stringifyKBSize(memInfo.getRawInfo()[17]));
                    pw.print(" cma usage)\n");
                    pw.print(" Lost RAM: ");
                    pw.println(stringifyKBSize(lostRAM));
                }
                if (brief) {
                    return;
                }
                if (memInfo.getZramTotalSizeKb() != 0) {
                    if (isCompact) {
                        pw.print("zram,");
                        pw.print(memInfo.getZramTotalSizeKb());
                        pw.print(",");
                        pw.print(memInfo.getSwapTotalSizeKb());
                        pw.print(",");
                        pw.println(memInfo.getSwapFreeSizeKb());
                    } else {
                        pw.print("     ZRAM: ");
                        pw.print(stringifyKBSize(memInfo.getZramTotalSizeKb()));
                        pw.print(" physical used for ");
                        pw.print(stringifyKBSize(memInfo.getSwapTotalSizeKb() - memInfo.getSwapFreeSizeKb()));
                        pw.print(" in swap (");
                        pw.print(stringifyKBSize(memInfo.getSwapTotalSizeKb()));
                        pw.println(" total swap)");
                    }
                }
                long[] ksm = getKsmInfo();
                if (isCompact) {
                    pw.print("ksm,");
                    pw.print(ksm[1]);
                    pw.print(",");
                    pw.print(ksm[0]);
                    pw.print(",");
                    pw.print(ksm[2]);
                    pw.print(",");
                    pw.println(ksm[3]);
                    pw.print("tuning,");
                    pw.print(ActivityManager.staticGetMemoryClass());
                    pw.print(',');
                    pw.print(ActivityManager.staticGetLargeMemoryClass());
                    pw.print(',');
                    pw.print(this.mProcessList.getMemLevel(906) / 1024);
                    if (ActivityManager.isLowRamDeviceStatic()) {
                        pw.print(",low-ram");
                    }
                    if (ActivityManager.isHighEndGfx()) {
                        pw.print(",high-end-gfx");
                    }
                    pw.println();
                    return;
                }
                if (ksm[1] != 0 || ksm[0] != 0 || ksm[2] != 0 || ksm[3] != 0) {
                    pw.print("      KSM: ");
                    pw.print(stringifyKBSize(ksm[1]));
                    pw.print(" saved from shared ");
                    pw.print(stringifyKBSize(ksm[0]));
                    pw.print("           ");
                    pw.print(stringifyKBSize(ksm[2]));
                    pw.print(" unshared; ");
                    pw.print(stringifyKBSize(ksm[3]));
                    pw.println(" volatile");
                }
                pw.print("   Tuning: ");
                pw.print(ActivityManager.staticGetMemoryClass());
                pw.print(" (large ");
                pw.print(ActivityManager.staticGetLargeMemoryClass());
                pw.print("), oom ");
                pw.print(stringifySize(this.mProcessList.getMemLevel(906), 1024));
                pw.print(", restore limit ");
                pw.print(stringifyKBSize(this.mProcessList.getCachedRestoreThresholdKb()));
                if (ActivityManager.isLowRamDeviceStatic()) {
                    pw.print(" (low-ram)");
                }
                if (ActivityManager.isHighEndGfx()) {
                    pw.print(" (high-end-gfx)");
                }
                pw.println();
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    private void appendBasicMemEntry(StringBuilder sb, int oomAdj, int procState, long pss, long memtrack, String name) {
        sb.append("  ");
        sb.append(ProcessList.makeOomAdjString(oomAdj));
        sb.append(' ');
        sb.append(ProcessList.makeProcStateString(procState));
        sb.append(' ');
        ProcessList.appendRamKb(sb, pss);
        sb.append(": ");
        sb.append(name);
        if (memtrack <= 0) {
            return;
        }
        sb.append(" (");
        sb.append(stringifyKBSize(memtrack));
        sb.append(" memtrack)");
    }

    private void appendMemInfo(StringBuilder sb, ProcessMemInfo mi) {
        appendBasicMemEntry(sb, mi.oomAdj, mi.procState, mi.pss, mi.memtrack, mi.name);
        sb.append(" (pid ");
        sb.append(mi.pid);
        sb.append(") ");
        sb.append(mi.adjType);
        sb.append('\n');
        if (mi.adjReason == null) {
            return;
        }
        sb.append("                      ");
        sb.append(mi.adjReason);
        sb.append('\n');
    }

    void reportMemUsage(ArrayList<ProcessMemInfo> memInfos) {
        SparseArray<ProcessMemInfo> infoMap = new SparseArray<>(memInfos.size());
        int N = memInfos.size();
        for (int i = 0; i < N; i++) {
            ProcessMemInfo mi = memInfos.get(i);
            infoMap.put(mi.pid, mi);
        }
        updateCpuStatsNow();
        long[] memtrackTmp = new long[1];
        synchronized (this.mProcessCpuTracker) {
            int N2 = this.mProcessCpuTracker.countStats();
            for (int i2 = 0; i2 < N2; i2++) {
                ProcessCpuTracker.Stats st = this.mProcessCpuTracker.getStats(i2);
                if (st.vsize > 0) {
                    long pss = Debug.getPss(st.pid, null, memtrackTmp);
                    if (pss > 0 && infoMap.indexOfKey(st.pid) < 0) {
                        ProcessMemInfo mi2 = new ProcessMemInfo(st.name, st.pid, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, -1, "native", null);
                        mi2.pss = pss;
                        mi2.memtrack = memtrackTmp[0];
                        memInfos.add(mi2);
                    }
                }
            }
        }
        long totalPss = 0;
        long totalMemtrack = 0;
        int N3 = memInfos.size();
        for (int i3 = 0; i3 < N3; i3++) {
            ProcessMemInfo mi3 = memInfos.get(i3);
            if (mi3.pss == 0) {
                mi3.pss = Debug.getPss(mi3.pid, null, memtrackTmp);
                mi3.memtrack = memtrackTmp[0];
            }
            totalPss += mi3.pss;
            totalMemtrack += mi3.memtrack;
        }
        Collections.sort(memInfos, new Comparator<ProcessMemInfo>() {
            @Override
            public int compare(ProcessMemInfo lhs, ProcessMemInfo rhs) {
                if (lhs.oomAdj != rhs.oomAdj) {
                    return lhs.oomAdj < rhs.oomAdj ? -1 : 1;
                }
                if (lhs.pss != rhs.pss) {
                    return lhs.pss < rhs.pss ? 1 : -1;
                }
                return 0;
            }
        });
        StringBuilder tag = new StringBuilder(128);
        StringBuilder stack = new StringBuilder(128);
        tag.append("Low on memory -- ");
        appendMemBucket(tag, totalPss, "total", false);
        appendMemBucket(stack, totalPss, "total", true);
        StringBuilder fullNativeBuilder = new StringBuilder(1024);
        StringBuilder shortNativeBuilder = new StringBuilder(1024);
        StringBuilder fullJavaBuilder = new StringBuilder(1024);
        boolean firstLine = true;
        int lastOomAdj = Integer.MIN_VALUE;
        long extraNativeRam = 0;
        long extraNativeMemtrack = 0;
        long cachedPss = 0;
        int N4 = memInfos.size();
        for (int i4 = 0; i4 < N4; i4++) {
            ProcessMemInfo mi4 = memInfos.get(i4);
            if (mi4.oomAdj >= 900) {
                cachedPss += mi4.pss;
            }
            if (mi4.oomAdj != -1000 && (mi4.oomAdj < 500 || mi4.oomAdj == 600 || mi4.oomAdj == 700)) {
                if (lastOomAdj != mi4.oomAdj) {
                    lastOomAdj = mi4.oomAdj;
                    if (mi4.oomAdj <= 0) {
                        tag.append(" / ");
                    }
                    if (mi4.oomAdj >= 0) {
                        if (firstLine) {
                            stack.append(":");
                            firstLine = false;
                        }
                        stack.append("\n\t at ");
                    } else {
                        stack.append("$");
                    }
                } else {
                    tag.append(" ");
                    stack.append("$");
                }
                if (mi4.oomAdj <= 0) {
                    appendMemBucket(tag, mi4.pss, mi4.name, false);
                }
                appendMemBucket(stack, mi4.pss, mi4.name, true);
                if (mi4.oomAdj >= 0 && (i4 + 1 >= N4 || memInfos.get(i4 + 1).oomAdj != lastOomAdj)) {
                    stack.append("(");
                    for (int k = 0; k < DUMP_MEM_OOM_ADJ.length; k++) {
                        if (DUMP_MEM_OOM_ADJ[k] == mi4.oomAdj) {
                            stack.append(DUMP_MEM_OOM_LABEL[k]);
                            stack.append(":");
                            stack.append(DUMP_MEM_OOM_ADJ[k]);
                        }
                    }
                    stack.append(")");
                }
            }
            appendMemInfo(fullNativeBuilder, mi4);
            if (mi4.oomAdj != -1000) {
                if (extraNativeRam > 0) {
                    appendBasicMemEntry(shortNativeBuilder, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, -1, extraNativeRam, extraNativeMemtrack, "(Other native)");
                    shortNativeBuilder.append('\n');
                    extraNativeRam = 0;
                }
                appendMemInfo(fullJavaBuilder, mi4);
            } else if (mi4.pss >= 512) {
                appendMemInfo(shortNativeBuilder, mi4);
            } else {
                extraNativeRam += mi4.pss;
                extraNativeMemtrack += mi4.memtrack;
            }
        }
        fullJavaBuilder.append("           ");
        ProcessList.appendRamKb(fullJavaBuilder, totalPss);
        fullJavaBuilder.append(": TOTAL");
        if (totalMemtrack > 0) {
            fullJavaBuilder.append(" (");
            fullJavaBuilder.append(stringifyKBSize(totalMemtrack));
            fullJavaBuilder.append(" memtrack)");
        }
        fullJavaBuilder.append("\n");
        MemInfoReader memInfo = new MemInfoReader();
        memInfo.readMemInfo();
        long[] infos = memInfo.getRawInfo();
        StringBuilder memInfoBuilder = new StringBuilder(1024);
        Debug.getMemInfo(infos);
        memInfoBuilder.append("  MemInfo: ");
        memInfoBuilder.append(stringifyKBSize(infos[5])).append(" slab, ");
        memInfoBuilder.append(stringifyKBSize(infos[4])).append(" shmem, ");
        memInfoBuilder.append(stringifyKBSize(infos[10])).append(" vm alloc, ");
        memInfoBuilder.append(stringifyKBSize(infos[11])).append(" page tables ");
        memInfoBuilder.append(stringifyKBSize(infos[12])).append(" kernel stack\n");
        memInfoBuilder.append("           ");
        memInfoBuilder.append(stringifyKBSize(infos[2])).append(" buffers, ");
        memInfoBuilder.append(stringifyKBSize(infos[3])).append(" cached, ");
        memInfoBuilder.append(stringifyKBSize(infos[9])).append(" mapped, ");
        memInfoBuilder.append(stringifyKBSize(infos[1])).append(" free\n");
        if (infos[8] != 0) {
            memInfoBuilder.append("  ZRAM: ");
            memInfoBuilder.append(stringifyKBSize(infos[8]));
            memInfoBuilder.append(" RAM, ");
            memInfoBuilder.append(stringifyKBSize(infos[6]));
            memInfoBuilder.append(" swap total, ");
            memInfoBuilder.append(stringifyKBSize(infos[7]));
            memInfoBuilder.append(" swap free\n");
        }
        long[] ksm = getKsmInfo();
        if (ksm[1] != 0 || ksm[0] != 0 || ksm[2] != 0 || ksm[3] != 0) {
            memInfoBuilder.append("  KSM: ");
            memInfoBuilder.append(stringifyKBSize(ksm[1]));
            memInfoBuilder.append(" saved from shared ");
            memInfoBuilder.append(stringifyKBSize(ksm[0]));
            memInfoBuilder.append("\n       ");
            memInfoBuilder.append(stringifyKBSize(ksm[2]));
            memInfoBuilder.append(" unshared; ");
            memInfoBuilder.append(stringifyKBSize(ksm[3]));
            memInfoBuilder.append(" volatile\n");
        }
        memInfoBuilder.append("  Free RAM: ");
        memInfoBuilder.append(stringifyKBSize(memInfo.getCachedSizeKb() + cachedPss + memInfo.getFreeSizeKb()));
        memInfoBuilder.append("\n");
        memInfoBuilder.append("  Used RAM: ");
        memInfoBuilder.append(stringifyKBSize((totalPss - cachedPss) + memInfo.getKernelUsedSizeKb()));
        memInfoBuilder.append("\n");
        memInfoBuilder.append("  Lost RAM: ");
        memInfoBuilder.append(stringifyKBSize(((((memInfo.getTotalSizeKb() - totalPss) - memInfo.getFreeSizeKb()) - memInfo.getCachedSizeKb()) - memInfo.getKernelUsedSizeKb()) - memInfo.getZramTotalSizeKb()));
        memInfoBuilder.append("\n");
        Slog.i(TAG, "Low on memory:");
        Slog.i(TAG, shortNativeBuilder.toString());
        Slog.i(TAG, fullJavaBuilder.toString());
        Slog.i(TAG, memInfoBuilder.toString());
        StringBuilder dropBuilder = new StringBuilder(1024);
        dropBuilder.append("Low on memory:");
        dropBuilder.append((CharSequence) stack);
        dropBuilder.append('\n');
        dropBuilder.append((CharSequence) fullNativeBuilder);
        dropBuilder.append((CharSequence) fullJavaBuilder);
        dropBuilder.append('\n');
        dropBuilder.append((CharSequence) memInfoBuilder);
        dropBuilder.append('\n');
        StringWriter catSw = new StringWriter();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                PrintWriter catPw = new FastPrintWriter(catSw, false, 256);
                String[] emptyArgs = new String[0];
                catPw.println();
                dumpProcessesLocked(null, catPw, emptyArgs, 0, false, null);
                catPw.println();
                this.mServices.newServiceDumperLocked(null, catPw, emptyArgs, 0, false, null).dumpLocked();
                catPw.println();
                dumpActivitiesLocked(null, catPw, emptyArgs, 0, false, false, null);
                catPw.flush();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        resetPriorityAfterLockedSection();
        dropBuilder.append(catSw.toString());
        addErrorToDropBox("lowmem", null, "system_server", null, null, tag.toString(), dropBuilder.toString(), null, null);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long now = SystemClock.uptimeMillis();
                if (this.mLastMemUsageReportTime < now) {
                    this.mLastMemUsageReportTime = now;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static boolean scanArgs(String[] args, String value) {
        if (args != null) {
            for (String arg : args) {
                if (value.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final boolean removeDyingProviderLocked(ProcessRecord proc, ContentProviderRecord cpr, boolean always) {
        boolean inLaunching = this.mLaunchingProviders.contains(cpr);
        if (!inLaunching || always) {
            synchronized (cpr) {
                cpr.launchingApp = null;
                cpr.notifyAll();
            }
            this.mProviderMap.removeProviderByClass(cpr.name, UserHandle.getUserId(cpr.uid));
            String[] names = cpr.info.authority.split(";");
            for (String str : names) {
                this.mProviderMap.removeProviderByName(str, UserHandle.getUserId(cpr.uid));
            }
        }
        for (int i = cpr.connections.size() - 1; i >= 0; i--) {
            ContentProviderConnection conn = cpr.connections.get(i);
            if (!conn.waiting || !inLaunching || always) {
                ProcessRecord capp = conn.client;
                conn.dead = true;
                if (conn.stableCount > 0) {
                    if (!capp.persistent && capp.thread != null && capp.pid != 0 && capp.pid != MY_PID) {
                        capp.kill("depends on provider " + cpr.name.flattenToShortString() + " in dying proc " + (proc != null ? proc.processName : "??") + " (adj " + (proc != null ? Integer.valueOf(proc.setAdj) : "??") + ")", true);
                    }
                } else if (capp.thread != null && conn.provider.provider != null) {
                    try {
                        capp.thread.unstableProviderDied(conn.provider.provider.asBinder());
                    } catch (RemoteException e) {
                    }
                    cpr.connections.remove(i);
                    if (conn.client.conProviders.remove(conn)) {
                        stopAssociationLocked(capp.uid, capp.processName, cpr.uid, cpr.name);
                    }
                }
            }
        }
        if (inLaunching && always) {
            this.mLaunchingProviders.remove(cpr);
        }
        return inLaunching;
    }

    private final boolean cleanUpApplicationRecordLocked(ProcessRecord app, boolean restarting, boolean allowRestart, int index, boolean replacingPid) {
        Slog.d(TAG, "cleanUpApplicationRecord -- " + app.pid);
        if (index >= 0) {
            removeLruProcessLocked(app);
            ProcessList.remove(app.pid);
        }
        this.mProcessesToGc.remove(app);
        this.mPendingPssProcesses.remove(app);
        if (app.crashDialog != null && !app.forceCrashReport) {
            app.crashDialog.dismiss();
            app.crashDialog = null;
        }
        if (app.anrDialog != null) {
            app.anrDialog.dismiss();
            app.anrDialog = null;
        }
        if (app.waitDialog != null) {
            app.waitDialog.dismiss();
            app.waitDialog = null;
        }
        app.crashing = false;
        app.notResponding = false;
        app.resetPackageList(this.mProcessStats);
        app.unlinkDeathRecipient();
        app.makeInactive(this.mProcessStats);
        app.waitingToKill = null;
        app.forcingToForeground = null;
        updateProcessForegroundLocked(app, false, false);
        app.foregroundActivities = false;
        app.hasShownUi = false;
        app.treatLikeActivity = false;
        app.hasAboveClient = false;
        app.hasClientActivities = false;
        this.mServices.killServicesLocked(app, allowRestart);
        boolean restart = false;
        for (int i = app.pubProviders.size() - 1; i >= 0; i--) {
            ContentProviderRecord cpr = app.pubProviders.valueAt(i);
            boolean always = app.bad || !allowRestart;
            boolean inLaunching = removeDyingProviderLocked(app, cpr, always);
            if ((inLaunching || always) && cpr.hasConnectionOrHandle()) {
                restart = true;
            }
            cpr.provider = null;
            cpr.proc = null;
        }
        app.pubProviders.clear();
        if (cleanupAppInLaunchingProvidersLocked(app, false)) {
            restart = true;
        }
        if (!app.conProviders.isEmpty()) {
            for (int i2 = app.conProviders.size() - 1; i2 >= 0; i2--) {
                ContentProviderConnection conn = app.conProviders.get(i2);
                conn.provider.connections.remove(conn);
                stopAssociationLocked(app.uid, app.processName, conn.provider.uid, conn.provider.name);
            }
            app.conProviders.clear();
        }
        skipCurrentReceiverLocked(app);
        for (int i3 = app.receivers.size() - 1; i3 >= 0; i3--) {
            removeReceiverLocked(app.receivers.valueAt(i3));
        }
        app.receivers.clear();
        if (this.mBackupTarget != null && app.pid == this.mBackupTarget.app.pid) {
            if (ActivityManagerDebugConfig.DEBUG_BACKUP || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.d(TAG_CLEANUP, "App " + this.mBackupTarget.appInfo + " died during backup");
            }
            try {
                IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                bm.agentDisconnected(app.info.packageName);
            } catch (RemoteException e) {
            }
        }
        for (int i4 = this.mPendingProcessChanges.size() - 1; i4 >= 0; i4--) {
            ProcessChangeItem item = this.mPendingProcessChanges.get(i4);
            if (item.pid == app.pid) {
                this.mPendingProcessChanges.remove(i4);
                this.mAvailProcessChanges.add(item);
            }
        }
        this.mUiHandler.obtainMessage(32, app.pid, app.info.uid, null).sendToTarget();
        if (restarting) {
            return false;
        }
        if (!app.persistent || app.isolated) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_CLEANUP, "Removing non-persistent process during cleanup: " + app);
            }
            if (!replacingPid) {
                removeProcessNameLocked(app.processName, app.uid);
            }
            if (this.mHeavyWeightProcess == app) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(25, this.mHeavyWeightProcess.userId, 0));
                this.mHeavyWeightProcess = null;
            }
        } else if (!app.removed && this.mPersistentStartingProcesses.indexOf(app) < 0) {
            this.mPersistentStartingProcesses.add(app);
            restart = true;
        }
        if ((ActivityManagerDebugConfig.DEBUG_PROCESSES || ActivityManagerDebugConfig.DEBUG_CLEANUP) && this.mProcessesOnHold.contains(app)) {
            Slog.v(TAG_CLEANUP, "Clean-up removing on hold: " + app);
        }
        this.mProcessesOnHold.remove(app);
        if (app == this.mHomeProcess) {
            this.mHomeProcess = null;
        }
        if (app == this.mPreviousProcess) {
            this.mPreviousProcess = null;
        }
        if (restart && !app.isolated) {
            if (index < 0) {
                ProcessList.remove(app.pid);
            }
            addProcessNameLocked(app);
            startProcessLocked(app, "restart", app.processName);
            return true;
        }
        if (app.pid > 0 && app.pid != MY_PID) {
            synchronized (this.mPidsSelfLocked) {
                this.mPidsSelfLocked.remove(app.pid);
                this.mHandler.removeMessages(20, app);
            }
            this.mBatteryStatsService.noteProcessFinish(app.processName, app.info.uid);
            if (app.isolated) {
                this.mBatteryStatsService.removeIsolatedUid(app.uid, app.info.uid);
            }
            app.setPid(0);
            return false;
        }
        return false;
    }

    boolean checkAppInLaunchingProvidersLocked(ProcessRecord app) {
        for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
            ContentProviderRecord cpr = this.mLaunchingProviders.get(i);
            if (cpr.launchingApp == app) {
                return true;
            }
        }
        return false;
    }

    boolean cleanupAppInLaunchingProvidersLocked(ProcessRecord app, boolean alwaysBad) {
        boolean restart = false;
        for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
            ContentProviderRecord cpr = this.mLaunchingProviders.get(i);
            if (cpr.launchingApp == app) {
                if (!alwaysBad && !app.bad && cpr.hasConnectionOrHandle()) {
                    restart = true;
                } else {
                    removeDyingProviderLocked(app, cpr, true);
                }
            }
        }
        return restart;
    }

    public List<ActivityManager.RunningServiceInfo> getServices(int maxNum, int flags) {
        List<ActivityManager.RunningServiceInfo> runningServiceInfoLocked;
        enforceNotIsolatedCaller("getServices");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                runningServiceInfoLocked = this.mServices.getRunningServiceInfoLocked(maxNum, flags);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return runningServiceInfoLocked;
    }

    public PendingIntent getRunningServiceControlPanel(ComponentName name) {
        PendingIntent runningServiceControlPanelLocked;
        enforceNotIsolatedCaller("getRunningServiceControlPanel");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                runningServiceControlPanelLocked = this.mServices.getRunningServiceControlPanelLocked(name);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return runningServiceControlPanelLocked;
    }

    public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType, String callingPackage, int userId) throws TransactionTooLargeException {
        ComponentName res;
        enforceNotIsolatedCaller("startService");
        if (service != null && service.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        if (callingPackage == null) {
            throw new IllegalArgumentException("callingPackage cannot be null");
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "startService: " + service + " type=" + resolvedType);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long origId = Binder.clearCallingIdentity();
                res = this.mServices.startServiceLocked(caller, service, resolvedType, callingPid, callingUid, callingPackage, userId);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return res;
    }

    ComponentName startServiceInPackage(int uid, Intent service, String resolvedType, String callingPackage, int userId) throws TransactionTooLargeException {
        ComponentName res;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "startServiceInPackage: " + service + " type=" + resolvedType);
                }
                long origId = Binder.clearCallingIdentity();
                res = this.mServices.startServiceLocked(null, service, resolvedType, -1, uid, callingPackage, userId);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return res;
    }

    public int stopService(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        int iStopServiceLocked;
        enforceNotIsolatedCaller("stopService");
        if (service != null && service.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                iStopServiceLocked = this.mServices.stopServiceLocked(caller, service, resolvedType, userId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return iStopServiceLocked;
    }

    public IBinder peekService(Intent service, String resolvedType, String callingPackage) {
        IBinder iBinderPeekServiceLocked;
        enforceNotIsolatedCaller("peekService");
        if (service != null && service.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        if (callingPackage == null) {
            throw new IllegalArgumentException("callingPackage cannot be null");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                iBinderPeekServiceLocked = this.mServices.peekServiceLocked(service, resolvedType, callingPackage);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return iBinderPeekServiceLocked;
    }

    public boolean stopServiceToken(ComponentName className, IBinder token, int startId) {
        boolean zStopServiceTokenLocked;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                zStopServiceTokenLocked = this.mServices.stopServiceTokenLocked(className, token, startId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zStopServiceTokenLocked;
    }

    public void setServiceForeground(ComponentName className, IBinder token, int id, Notification notification, int flags) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mServices.setServiceForegroundLocked(className, token, id, notification, flags);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int handleIncomingUser(int callingPid, int callingUid, int userId, boolean allowAll, boolean requireFull, String name, String callerPackage) {
        return this.mUserController.handleIncomingUser(callingPid, callingUid, userId, allowAll, requireFull ? 2 : 0, name, callerPackage);
    }

    boolean isSingleton(String componentProcessName, ApplicationInfo aInfo, String className, int flags) {
        boolean result = false;
        if (UserHandle.getAppId(aInfo.uid) >= 10000) {
            if ((flags & 1073741824) != 0) {
                if (ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS", aInfo.uid) != 0) {
                    ComponentName comp = new ComponentName(aInfo.packageName, className);
                    String msg = "Permission Denial: Component " + comp.flattenToShortString() + " requests FLAG_SINGLE_USER, but app does not hold android.permission.INTERACT_ACROSS_USERS";
                    Slog.w(TAG, msg);
                    throw new SecurityException(msg);
                }
                result = true;
            }
        } else if ("system".equals(componentProcessName)) {
            result = true;
        } else if ((flags & 1073741824) != 0) {
            result = UserHandle.isSameApp(aInfo.uid, ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG) || (aInfo.flags & 8) != 0;
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "isSingleton(" + componentProcessName + ", " + aInfo + ", " + className + ", 0x" + Integer.toHexString(flags) + ") = " + result);
        }
        return result;
    }

    boolean isValidSingletonCall(int callingUid, int componentUid) {
        int componentAppId = UserHandle.getAppId(componentUid);
        return UserHandle.isSameApp(callingUid, componentUid) || componentAppId == 1000 || componentAppId == 1001 || ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", componentUid) == 0;
    }

    public int bindService(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String callingPackage, int userId) throws TransactionTooLargeException {
        int iBindServiceLocked;
        enforceNotIsolatedCaller("bindService");
        if (service != null && service.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        if (callingPackage == null) {
            throw new IllegalArgumentException("callingPackage cannot be null");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                iBindServiceLocked = this.mServices.bindServiceLocked(caller, token, service, resolvedType, connection, flags, callingPackage, userId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return iBindServiceLocked;
    }

    public boolean unbindService(IServiceConnection connection) {
        boolean zUnbindServiceLocked;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                zUnbindServiceLocked = this.mServices.unbindServiceLocked(connection);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zUnbindServiceLocked;
    }

    public void publishService(IBinder token, Intent intent, IBinder service) {
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!(token instanceof ServiceRecord)) {
                    throw new IllegalArgumentException("Invalid service token");
                }
                this.mServices.publishServiceLocked((ServiceRecord) token, intent, service);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void unbindFinished(IBinder token, Intent intent, boolean doRebind) {
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mServices.unbindFinishedLocked((ServiceRecord) token, intent, doRebind);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void serviceDoneExecuting(IBinder token, int type, int startId, int res) {
        if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG, "SVC-Executing service done: " + token + ", type=" + type + ", startId=" + startId + ", res=" + res);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!(token instanceof ServiceRecord)) {
                    Slog.e(TAG, "serviceDoneExecuting: Invalid service token=" + token);
                    throw new IllegalArgumentException("Invalid service token");
                }
                this.mServices.serviceDoneExecutingLocked((ServiceRecord) token, type, startId, res);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean bindBackupAgent(String packageName, int backupMode, int userId) {
        BatteryStatsImpl.Uid.Pkg.Serv ss;
        if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
            Slog.v(TAG, "bindBackupAgent: app=" + packageName + " mode=" + backupMode);
        }
        enforceCallingPermission("android.permission.CONFIRM_FULL_BACKUP", "bindBackupAgent");
        IPackageManager pm = AppGlobals.getPackageManager();
        ApplicationInfo app = null;
        try {
            app = pm.getApplicationInfo(packageName, 0, userId);
        } catch (RemoteException e) {
        }
        if (app == null) {
            Slog.w(TAG, "Unable to bind backup agent for " + packageName);
            return false;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
                synchronized (stats) {
                    ss = stats.getServiceStatsLocked(app.uid, app.packageName, app.name);
                }
                try {
                    AppGlobals.getPackageManager().setPackageStoppedState(app.packageName, false, UserHandle.getUserId(app.uid));
                } catch (RemoteException e2) {
                } catch (IllegalArgumentException e3) {
                    Slog.w(TAG, "Failed trying to unstop package " + app.packageName + ": " + e3);
                }
                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                    AMEventHookData.PackageStoppedStatusChanged eventData = AMEventHookData.PackageStoppedStatusChanged.createInstance();
                    eventData.set(new Object[]{app.packageName, 0, "bindBackupAgent"});
                    this.mAMEventHook.hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData);
                }
                BackupRecord r = new BackupRecord(ss, app, backupMode);
                ComponentName hostingName = backupMode == 0 ? new ComponentName(app.packageName, app.backupAgentName) : new ComponentName("android", "FullBackupAgent");
                ProcessRecord proc = startProcessLocked(app.processName, app, false, 0, "backup", hostingName, false, false, false);
                if (proc == null) {
                    Slog.e(TAG, "Unable to start backup agent process " + r);
                    resetPriorityAfterLockedSection();
                    return false;
                }
                if (UserHandle.isApp(app.uid) && backupMode == 1) {
                    proc.inFullBackup = true;
                }
                r.app = proc;
                this.mBackupTarget = r;
                this.mBackupAppName = app.packageName;
                updateOomAdjLocked(proc);
                if (proc.thread != null) {
                    if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
                        Slog.v(TAG_BACKUP, "Agent proc already running: " + proc);
                    }
                    try {
                        proc.thread.scheduleCreateBackupAgent(app, compatibilityInfoForPackageLocked(app), backupMode);
                    } catch (RemoteException e4) {
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
                    Slog.v(TAG_BACKUP, "Agent proc not running, waiting for attach");
                }
                resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void clearPendingBackup() {
        if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
            Slog.v(TAG_BACKUP, "clearPendingBackup");
        }
        enforceCallingPermission("android.permission.BACKUP", "clearPendingBackup");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mBackupTarget = null;
                this.mBackupAppName = null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void backupAgentCreated(String agentPackageName, IBinder agent) {
        if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
            Slog.v(TAG_BACKUP, "backupAgentCreated: " + agentPackageName + " = " + agent);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (!agentPackageName.equals(this.mBackupAppName)) {
                    Slog.e(TAG, "Backup agent created for " + agentPackageName + " but not requested!");
                    resetPriorityAfterLockedSection();
                    return;
                }
                resetPriorityAfterLockedSection();
                long oldIdent = Binder.clearCallingIdentity();
                try {
                    IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    bm.agentConnected(agentPackageName, agent);
                } catch (RemoteException e) {
                } catch (Exception e2) {
                    Slog.w(TAG, "Exception trying to deliver BackupAgent binding: ");
                    e2.printStackTrace();
                } finally {
                    Binder.restoreCallingIdentity(oldIdent);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void unbindBackupAgent(ApplicationInfo appInfo) {
        if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
            Slog.v(TAG_BACKUP, "unbindBackupAgent: " + appInfo);
        }
        if (appInfo == null) {
            Slog.w(TAG, "unbind backup agent for null app");
            return;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                try {
                    if (this.mBackupAppName == null) {
                        Slog.w(TAG, "Unbinding backup agent with no active backup");
                        this.mBackupTarget = null;
                        this.mBackupAppName = null;
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (!this.mBackupAppName.equals(appInfo.packageName)) {
                        Slog.e(TAG, "Unbind of " + appInfo + " but is not the current backup target");
                        this.mBackupTarget = null;
                        this.mBackupAppName = null;
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    ProcessRecord proc = this.mBackupTarget.app;
                    updateOomAdjLocked(proc);
                    if (proc.thread != null) {
                        try {
                            proc.thread.scheduleDestroyBackupAgent(appInfo, compatibilityInfoForPackageLocked(appInfo));
                        } catch (Exception e) {
                            Slog.e(TAG, "Exception when unbinding backup agent:");
                            e.printStackTrace();
                        }
                    }
                    this.mBackupTarget = null;
                    this.mBackupAppName = null;
                    resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    this.mBackupTarget = null;
                    this.mBackupAppName = null;
                    throw th;
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
    }

    boolean isPendingBroadcastProcessLocked(int pid) {
        if (this.mFgBroadcastQueue.isPendingBroadcastProcessLocked(pid)) {
            return true;
        }
        return this.mBgBroadcastQueue.isPendingBroadcastProcessLocked(pid);
    }

    void skipPendingBroadcastLocked(int pid) {
        Slog.w(TAG, "Unattached app died before broadcast acknowledged, skipping");
        for (BroadcastQueue queue : this.mBroadcastQueues) {
            queue.skipPendingBroadcastLocked(pid);
        }
    }

    boolean sendPendingBroadcastsLocked(ProcessRecord app) {
        boolean didSomething = false;
        for (BroadcastQueue queue : this.mBroadcastQueues) {
            didSomething |= queue.sendPendingBroadcastsLocked(app);
        }
        return didSomething;
    }

    public Intent registerReceiver(IApplicationThread caller, String callerPackage, IIntentReceiver receiver, IntentFilter filter, String permission, int userId) throws Throwable {
        int callingUid;
        int callingPid;
        ArrayList<Intent> stickyIntents;
        ArrayList<Intent> intents;
        enforceNotIsolatedCaller("registerReceiver");
        ArrayList<Intent> stickyIntents2 = null;
        ProcessRecord callerApp = null;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (caller != null) {
                    callerApp = getRecordForAppLocked(caller);
                    if (callerApp == null) {
                        throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when registering receiver " + receiver);
                    }
                    if (callerApp.info.uid != 1000 && !callerApp.pkgList.containsKey(callerPackage) && !"android".equals(callerPackage)) {
                        throw new SecurityException("Given caller package " + callerPackage + " is not running in process " + callerApp);
                    }
                    callingUid = callerApp.info.uid;
                    callingPid = callerApp.pid;
                } else {
                    callerPackage = null;
                    callingUid = Binder.getCallingUid();
                    callingPid = Binder.getCallingPid();
                }
                int userId2 = this.mUserController.handleIncomingUser(callingPid, callingUid, userId, true, 2, "registerReceiver", callerPackage);
                Iterator<String> actions = filter.actionsIterator();
                if (actions == null) {
                    ArrayList<String> noAction = new ArrayList<>(1);
                    noAction.add(null);
                    actions = noAction.iterator();
                }
                int[] userIds = {-1, UserHandle.getUserId(callingUid)};
                while (actions.hasNext()) {
                    String action = actions.next();
                    int i = 0;
                    int length = userIds.length;
                    ArrayList<Intent> stickyIntents3 = stickyIntents2;
                    while (i < length) {
                        try {
                            int id = userIds[i];
                            ArrayMap<String, ArrayList<Intent>> stickies = this.mStickyBroadcasts.get(id);
                            if (stickies == null || (intents = stickies.get(action)) == null) {
                                stickyIntents = stickyIntents3;
                            } else {
                                stickyIntents = stickyIntents3 == null ? new ArrayList<>() : stickyIntents3;
                                stickyIntents.addAll(intents);
                            }
                            i++;
                            stickyIntents3 = stickyIntents;
                        } catch (Throwable th) {
                            th = th;
                            resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    stickyIntents2 = stickyIntents3;
                }
                resetPriorityAfterLockedSection();
                ArrayList<Intent> allSticky = null;
                if (stickyIntents2 != null) {
                    ContentResolver resolver = this.mContext.getContentResolver();
                    int N = stickyIntents2.size();
                    for (int i2 = 0; i2 < N; i2++) {
                        Intent intent = stickyIntents2.get(i2);
                        if (filter.match(resolver, intent, true, TAG) >= 0) {
                            if (allSticky == null) {
                                allSticky = new ArrayList<>();
                            }
                            allSticky.add(intent);
                        }
                    }
                }
                Intent intent2 = allSticky != null ? allSticky.get(0) : null;
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Register receiver " + filter + ": " + intent2);
                }
                if (receiver == null) {
                    return intent2;
                }
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (callerApp != null && (callerApp.thread == null || callerApp.thread.asBinder() != caller.asBinder())) {
                            resetPriorityAfterLockedSection();
                            return null;
                        }
                        ReceiverList rl = this.mRegisteredReceivers.get(receiver.asBinder());
                        if (rl == null) {
                            rl = new ReceiverList(this, callerApp, callingPid, callingUid, userId2, receiver);
                            if (rl.app != null) {
                                rl.app.receivers.add(rl);
                            } else {
                                try {
                                    receiver.asBinder().linkToDeath(rl, 0);
                                    rl.linkedToDeath = true;
                                } catch (RemoteException e) {
                                    resetPriorityAfterLockedSection();
                                    return intent2;
                                }
                            }
                            this.mRegisteredReceivers.put(receiver.asBinder(), rl);
                        } else {
                            if (rl.uid != callingUid) {
                                throw new IllegalArgumentException("Receiver requested to register for uid " + callingUid + " was previously registered for uid " + rl.uid);
                            }
                            if (rl.pid != callingPid) {
                                throw new IllegalArgumentException("Receiver requested to register for pid " + callingPid + " was previously registered for pid " + rl.pid);
                            }
                            if (rl.userId != userId2) {
                                throw new IllegalArgumentException("Receiver requested to register for user " + userId2 + " was previously registered for user " + rl.userId);
                            }
                        }
                        BroadcastFilter bf = new BroadcastFilter(filter, rl, callerPackage, permission, callingUid, userId2);
                        rl.add(bf);
                        if (!bf.debugCheck()) {
                            Slog.w(TAG, "==> For Dynamic broadcast");
                        }
                        this.mReceiverResolver.addFilter(bf);
                        if (allSticky != null) {
                            ArrayList receivers = new ArrayList();
                            receivers.add(bf);
                            int stickyCount = allSticky.size();
                            for (int i3 = 0; i3 < stickyCount; i3++) {
                                Intent intent3 = allSticky.get(i3);
                                BroadcastQueue queue = broadcastQueueForIntent(intent3);
                                BroadcastRecord r = new BroadcastRecord(queue, intent3, null, null, -1, -1, null, null, -1, null, receivers, null, 0, null, null, false, true, true, -1);
                                queue.enqueueParallelBroadcastLocked(r);
                                queue.scheduleBroadcastsLocked();
                            }
                        }
                        resetPriorityAfterLockedSection();
                        return intent2;
                    } catch (Throwable th2) {
                        resetPriorityAfterLockedSection();
                        throw th2;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void unregisterReceiver(IIntentReceiver receiver) {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "Unregister receiver: " + receiver);
        }
        long origId = Binder.clearCallingIdentity();
        boolean doTrim = false;
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    ReceiverList rl = this.mRegisteredReceivers.get(receiver.asBinder());
                    if (rl != null) {
                        BroadcastRecord r = rl.curBroadcast;
                        if (r != null && r == r.queue.getMatchingOrderedReceiver(r)) {
                            boolean doNext = r.queue.finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
                            if (doNext) {
                                doTrim = true;
                                r.queue.processNextBroadcast(false);
                            }
                        }
                        if (rl.app != null) {
                            rl.app.receivers.remove(rl);
                        }
                        removeReceiverLocked(rl);
                        if (rl.linkedToDeath) {
                            rl.linkedToDeath = false;
                            rl.receiver.asBinder().unlinkToDeath(rl, 0);
                        }
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            if (!doTrim) {
                return;
            }
            trimApplications();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void removeReceiverLocked(ReceiverList rl) {
        this.mRegisteredReceivers.remove(rl.receiver.asBinder());
        for (int i = rl.size() - 1; i >= 0; i--) {
            this.mReceiverResolver.removeFilter(rl.get(i));
        }
    }

    private final void sendPackageBroadcastLocked(int cmd, String[] packages, int userId) {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord r = this.mLruProcesses.get(i);
            if (r.thread != null && (userId == -1 || r.userId == userId)) {
                try {
                    r.thread.dispatchPackageBroadcast(cmd, packages);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private List<ResolveInfo> collectReceiverComponents(Intent intent, String resolvedType, int callingUid, int[] users) {
        HashSet<ComponentName> singleUserReceivers;
        List<ResolveInfo> receivers = null;
        HashSet<ComponentName> singleUserReceivers2 = null;
        boolean scannedFirstReceivers = false;
        try {
            for (int user : users) {
                if (callingUid != PENDING_ASSIST_EXTRAS_LONG_TIMEOUT || !this.mUserController.hasUserRestriction("no_debugging_features", user) || isPermittedShellBroadcast(intent)) {
                    List<ResolveInfo> newReceivers = AppGlobals.getPackageManager().queryIntentReceivers(intent, resolvedType, 268436480, user).getList();
                    if (user != 0 && newReceivers != null) {
                        int i = 0;
                        while (i < newReceivers.size()) {
                            if ((newReceivers.get(i).activityInfo.flags & 536870912) != 0) {
                                newReceivers.remove(i);
                                i--;
                            }
                            i++;
                        }
                    }
                    if (newReceivers != null && newReceivers.size() == 0) {
                        newReceivers = null;
                    }
                    if (receivers == null) {
                        receivers = newReceivers;
                    } else if (newReceivers != null) {
                        if (!scannedFirstReceivers) {
                            scannedFirstReceivers = true;
                            int i2 = 0;
                            while (true) {
                                try {
                                    singleUserReceivers = singleUserReceivers2;
                                    if (i2 >= receivers.size()) {
                                        break;
                                    }
                                    ResolveInfo ri = receivers.get(i2);
                                    if ((ri.activityInfo.flags & 1073741824) != 0) {
                                        ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
                                        singleUserReceivers2 = singleUserReceivers == null ? new HashSet<>() : singleUserReceivers;
                                        singleUserReceivers2.add(cn);
                                    } else {
                                        singleUserReceivers2 = singleUserReceivers;
                                    }
                                    i2++;
                                } catch (RemoteException e) {
                                }
                            }
                            singleUserReceivers2 = singleUserReceivers;
                        }
                        int i3 = 0;
                        while (true) {
                            singleUserReceivers = singleUserReceivers2;
                            if (i3 >= newReceivers.size()) {
                                break;
                            }
                            ResolveInfo ri2 = newReceivers.get(i3);
                            if ((ri2.activityInfo.flags & 1073741824) != 0) {
                                ComponentName cn2 = new ComponentName(ri2.activityInfo.packageName, ri2.activityInfo.name);
                                singleUserReceivers2 = singleUserReceivers == null ? new HashSet<>() : singleUserReceivers;
                                if (!singleUserReceivers2.contains(cn2)) {
                                    singleUserReceivers2.add(cn2);
                                    receivers.add(ri2);
                                }
                            } else {
                                receivers.add(ri2);
                                singleUserReceivers2 = singleUserReceivers;
                            }
                            i3++;
                        }
                        singleUserReceivers2 = singleUserReceivers;
                    }
                    if (CtaUtils.isCtaSupported()) {
                        ReceiverController autoBootController = ReceiverController.getInstance(this.mContext);
                        autoBootController.filterReceiver(intent, receivers, user);
                    }
                }
            }
        } catch (RemoteException e2) {
        }
        return receivers;
    }

    private boolean isPermittedShellBroadcast(Intent intent) {
        return INTENT_REMOTE_BUGREPORT_FINISHED.equals(intent.getAction());
    }

    final int broadcastIntentLocked(ProcessRecord callerApp, String callerPackage, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean ordered, boolean sticky, int callingPid, int callingUid, int userId) {
        boolean isCallerSystem;
        String pkgName;
        ArrayMap<String, ArrayList<Intent>> stickies;
        ArrayList<Intent> list;
        String ssp;
        String ssp2;
        String ssp3;
        String ssp4;
        Intent intent2 = new Intent(intent);
        intent2.addFlags(16);
        if (!this.mProcessesReady && (intent2.getFlags() & 33554432) == 0) {
            intent2.addFlags(1073741824);
        }
        Slog.v(TAG_BROADCAST, (sticky ? "Broadcast sticky: " : "Broadcast: ") + intent2 + " ordered=" + ordered + " userid=" + userId + " callerApp=" + callerApp);
        if (resultTo != null && !ordered) {
            Slog.w(TAG, "Broadcast " + intent2 + " not ordered but result callback requested!");
        }
        int userId2 = this.mUserController.handleIncomingUser(callingPid, callingUid, userId, true, 0, "broadcast", callerPackage);
        if (userId2 != -1 && !this.mUserController.isUserRunningLocked(userId2, 0) && ((callingUid != 1000 || (intent2.getFlags() & 33554432) == 0) && !"android.intent.action.ACTION_SHUTDOWN".equals(intent2.getAction()))) {
            Slog.w(TAG, "Skipping broadcast of " + intent2 + ": user " + userId2 + " is stopped");
            return -2;
        }
        BroadcastOptions brOptions = null;
        if (bOptions != null) {
            brOptions = new BroadcastOptions(bOptions);
            if (brOptions.getTemporaryAppWhitelistDuration() > 0 && checkComponentPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", Binder.getCallingPid(), Binder.getCallingUid(), -1, true) != 0) {
                String msg = "Permission Denial: " + intent2.getAction() + " broadcast from " + callerPackage + " (pid=" + callingPid + ", uid=" + callingUid + ") requires android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST";
                Slog.w(TAG, msg);
                throw new SecurityException(msg);
            }
        }
        String action = intent2.getAction();
        try {
            boolean isProtectedBroadcast = AppGlobals.getPackageManager().isProtectedBroadcast(action);
            switch (UserHandle.getAppId(callingUid)) {
                case 0:
                case 1000:
                case ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                case ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG:
                case 1027:
                    isCallerSystem = true;
                    break;
                default:
                    isCallerSystem = callerApp != null ? callerApp.persistent : false;
                    break;
            }
            if (!isCallerSystem) {
                if (isProtectedBroadcast) {
                    String msg2 = "Permission Denial: not allowed to send broadcast " + action + " from pid=" + callingPid + ", uid=" + callingUid;
                    Slog.w(TAG, msg2);
                    throw new SecurityException(msg2);
                }
                if ("android.appwidget.action.APPWIDGET_CONFIGURE".equals(action) || "android.appwidget.action.APPWIDGET_UPDATE".equals(action)) {
                    if (callerPackage == null) {
                        String msg3 = "Permission Denial: not allowed to send broadcast " + action + " from unknown caller.";
                        Slog.w(TAG, msg3);
                        throw new SecurityException(msg3);
                    }
                    if (intent2.getComponent() == null) {
                        intent2.setPackage(callerPackage);
                    } else if (!intent2.getComponent().getPackageName().equals(callerPackage)) {
                        String msg4 = "Permission Denial: not allowed to send broadcast " + action + " to " + intent2.getComponent().getPackageName() + " from " + callerPackage;
                        Slog.w(TAG, msg4);
                        throw new SecurityException(msg4);
                    }
                }
            } else if (!isProtectedBroadcast && !"android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) && !"android.intent.action.DISMISS_KEYBOARD_SHORTCUTS".equals(action) && !"android.intent.action.MEDIA_BUTTON".equals(action) && !"android.intent.action.MEDIA_SCANNER_SCAN_FILE".equals(action) && !"android.intent.action.SHOW_KEYBOARD_SHORTCUTS".equals(action) && !"android.appwidget.action.APPWIDGET_CONFIGURE".equals(action) && !"android.appwidget.action.APPWIDGET_UPDATE".equals(action) && !"android.location.HIGH_POWER_REQUEST_CHANGE".equals(action) && !"com.android.omadm.service.CONFIGURATION_UPDATE".equals(action) && !"android.text.style.SUGGESTION_PICKED".equals(action)) {
                if (callerApp != null) {
                    Log.wtf(TAG, "Sending non-protected broadcast " + action + " from system " + callerApp.toShortString() + " pkg " + callerPackage, new Throwable());
                } else {
                    Log.wtf(TAG, "Sending non-protected broadcast " + action + " from system uid " + UserHandle.formatUid(callingUid) + " pkg " + callerPackage, new Throwable());
                }
            }
            if (action != null) {
                if (action.equals("android.intent.action.UID_REMOVED") || action.equals("android.intent.action.PACKAGE_REMOVED") || action.equals("android.intent.action.PACKAGE_CHANGED") || action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE") || action.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE") || action.equals("android.intent.action.PACKAGES_SUSPENDED") || action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                    if (checkComponentPermission("android.permission.BROADCAST_PACKAGE_REMOVED", callingPid, callingUid, -1, true) != 0) {
                        String msg5 = "Permission Denial: " + intent2.getAction() + " broadcast from " + callerPackage + " (pid=" + callingPid + ", uid=" + callingUid + ") requires android.permission.BROADCAST_PACKAGE_REMOVED";
                        Slog.w(TAG, msg5);
                        throw new SecurityException(msg5);
                    }
                    if (action.equals("android.intent.action.UID_REMOVED")) {
                        Bundle intentExtras = intent2.getExtras();
                        int uid = intentExtras != null ? intentExtras.getInt("android.intent.extra.UID") : -1;
                        if (uid >= 0) {
                            this.mBatteryStatsService.removeUid(uid);
                            this.mAppOpsService.uidRemoved(uid);
                        }
                    } else if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                        String[] list2 = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
                        if (list2 != null && list2.length > 0) {
                            for (String str : list2) {
                                forceStopPackageLocked(str, -1, false, true, true, false, false, userId2, "storage unmount");
                            }
                            this.mRecentTasks.cleanupLocked(-1);
                            sendPackageBroadcastLocked(1, list2, userId2);
                        }
                    } else if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE")) {
                        this.mRecentTasks.cleanupLocked(-1);
                    } else if (action.equals("android.intent.action.PACKAGE_REMOVED") || action.equals("android.intent.action.PACKAGE_CHANGED")) {
                        Uri data = intent2.getData();
                        if (data != null && (ssp4 = data.getSchemeSpecificPart()) != null) {
                            boolean removed = "android.intent.action.PACKAGE_REMOVED".equals(action);
                            boolean replacing = intent2.getBooleanExtra("android.intent.extra.REPLACING", false);
                            boolean killProcess = !intent2.getBooleanExtra("android.intent.extra.DONT_KILL_APP", false);
                            boolean fullUninstall = removed && !replacing;
                            if (removed) {
                                if (killProcess) {
                                    forceStopPackageLocked(ssp4, UserHandle.getAppId(intent2.getIntExtra("android.intent.extra.UID", -1)), false, true, true, false, fullUninstall, userId2, removed ? "pkg removed" : "pkg changed");
                                }
                                int cmd = killProcess ? 0 : 2;
                                sendPackageBroadcastLocked(cmd, new String[]{ssp4}, userId2);
                                if (fullUninstall) {
                                    this.mAppOpsService.packageRemoved(intent2.getIntExtra("android.intent.extra.UID", -1), ssp4);
                                    removeUriPermissionsForPackageLocked(ssp4, userId2, true);
                                    removeTasksByPackageNameLocked(ssp4, userId2);
                                    if (this.mUnsupportedDisplaySizeDialog != null && ssp4.equals(this.mUnsupportedDisplaySizeDialog.getPackageName())) {
                                        this.mUnsupportedDisplaySizeDialog.dismiss();
                                        this.mUnsupportedDisplaySizeDialog = null;
                                    }
                                    this.mCompatModePackages.handlePackageUninstalledLocked(ssp4);
                                    this.mBatteryStatsService.notePackageUninstalled(ssp4);
                                }
                            } else {
                                if (killProcess) {
                                    killPackageProcessesLocked(ssp4, UserHandle.getAppId(intent2.getIntExtra("android.intent.extra.UID", -1)), userId2, -10000, false, true, true, false, "change " + ssp4);
                                }
                                cleanupDisabledPackageComponentsLocked(ssp4, userId2, killProcess, intent2.getStringArrayExtra("android.intent.extra.changed_component_name_list"));
                            }
                        }
                    } else if (action.equals("android.intent.action.PACKAGES_SUSPENDED") || action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                        boolean suspended = "android.intent.action.PACKAGES_SUSPENDED".equals(intent2.getAction());
                        String[] packageNames = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
                        int userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -10000);
                        synchronized (this) {
                            try {
                                boostPriorityForLockedSection();
                                this.mRecentTasks.onPackagesSuspendedChanged(packageNames, suspended, userHandle);
                            } catch (Throwable th) {
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        resetPriorityAfterLockedSection();
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                    Uri data2 = intent2.getData();
                    if (data2 != null && (ssp3 = data2.getSchemeSpecificPart()) != null) {
                        ApplicationInfo aInfo = getPackageManagerInternalLocked().getApplicationInfo(ssp3, userId2);
                        if (aInfo == null) {
                            Slog.w(TAG, "Dropping ACTION_PACKAGE_REPLACED for non-existent pkg: ssp=" + ssp3 + " data=" + data2);
                            return 0;
                        }
                        this.mStackSupervisor.updateActivityApplicationInfoLocked(aInfo);
                        sendPackageBroadcastLocked(3, new String[]{ssp3}, userId2);
                    }
                } else if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                    Uri data3 = intent2.getData();
                    if (data3 != null && (ssp2 = data3.getSchemeSpecificPart()) != null) {
                        boolean replacing2 = intent2.getBooleanExtra("android.intent.extra.REPLACING", false);
                        this.mCompatModePackages.handlePackageAddedLocked(ssp2, replacing2);
                        try {
                            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(ssp2, 0, 0);
                            this.mBatteryStatsService.notePackageInstalled(ssp2, ai != null ? ai.versionCode : 0);
                        } catch (RemoteException e) {
                        }
                    }
                } else if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                    Uri data4 = intent2.getData();
                    if (data4 != null && (ssp = data4.getSchemeSpecificPart()) != null) {
                        if (this.mUnsupportedDisplaySizeDialog != null && ssp.equals(this.mUnsupportedDisplaySizeDialog.getPackageName())) {
                            this.mUnsupportedDisplaySizeDialog.dismiss();
                            this.mUnsupportedDisplaySizeDialog = null;
                        }
                        this.mCompatModePackages.handlePackageDataClearedLocked(ssp);
                    }
                } else if (action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                    this.mHandler.sendEmptyMessage(13);
                } else if (action.equals("android.intent.action.TIME_SET")) {
                    int is24Hour = intent2.getBooleanExtra("android.intent.extra.TIME_PREF_24_HOUR_FORMAT", false) ? 1 : 0;
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(41, is24Hour, 0));
                    BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
                    synchronized (stats) {
                        stats.noteCurrentTimeChangedLocked();
                    }
                } else if (action.equals("android.intent.action.CLEAR_DNS_CACHE")) {
                    this.mHandler.sendEmptyMessage(28);
                } else if (action.equals("android.intent.action.PROXY_CHANGE")) {
                    ProxyInfo proxy = (ProxyInfo) intent2.getParcelableExtra("android.intent.extra.PROXY_INFO");
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(29, proxy));
                } else if (action.equals("android.hardware.action.NEW_PICTURE") || action.equals("android.hardware.action.NEW_VIDEO")) {
                    Slog.w(TAG, action + " no longer allowed; dropping from " + UserHandle.formatUid(callingUid));
                    if (resultTo == null) {
                        return 0;
                    }
                    BroadcastQueue queue = broadcastQueueForIntent(intent2);
                    try {
                        queue.performReceiveLocked(callerApp, resultTo, intent2, 0, null, null, false, false, userId2);
                        return 0;
                    } catch (RemoteException e2) {
                        Slog.w(TAG, "Failure [" + queue.mQueueName + "] sending broadcast result of " + intent2, e2);
                        return 0;
                    }
                }
            }
            if (sticky) {
                if (checkPermission("android.permission.BROADCAST_STICKY", callingPid, callingUid) != 0) {
                    String msg6 = "Permission Denial: broadcastIntent() requesting a sticky broadcast from pid=" + callingPid + ", uid=" + callingUid + " requires android.permission.BROADCAST_STICKY";
                    Slog.w(TAG, msg6);
                    throw new SecurityException(msg6);
                }
                if (requiredPermissions != null && requiredPermissions.length > 0) {
                    Slog.w(TAG, "Can't broadcast sticky intent " + intent2 + " and enforce permissions " + Arrays.toString(requiredPermissions));
                    return -1;
                }
                if (intent2.getComponent() != null) {
                    throw new SecurityException("Sticky broadcasts can't target a specific component");
                }
                if (userId2 != -1 && (stickies = this.mStickyBroadcasts.get(-1)) != null && (list = stickies.get(intent2.getAction())) != null) {
                    int N = list.size();
                    for (int i = 0; i < N; i++) {
                        if (intent2.filterEquals(list.get(i))) {
                            throw new IllegalArgumentException("Sticky broadcast " + intent2 + " for user " + userId2 + " conflicts with existing global broadcast");
                        }
                    }
                }
                ArrayMap<String, ArrayList<Intent>> stickies2 = this.mStickyBroadcasts.get(userId2);
                if (stickies2 == null) {
                    stickies2 = new ArrayMap<>();
                    this.mStickyBroadcasts.put(userId2, stickies2);
                }
                ArrayList<Intent> list3 = stickies2.get(intent2.getAction());
                if (list3 == null) {
                    list3 = new ArrayList<>();
                    stickies2.put(intent2.getAction(), list3);
                }
                int stickiesCount = list3.size();
                int i2 = 0;
                while (true) {
                    if (i2 < stickiesCount) {
                        if (intent2.filterEquals(list3.get(i2))) {
                            list3.set(i2, new Intent(intent2));
                        } else {
                            i2++;
                        }
                    }
                }
                if (i2 >= stickiesCount) {
                    list3.add(new Intent(intent2));
                }
            }
            int[] users = userId2 == -1 ? this.mUserController.getStartedUserArrayLocked() : new int[]{userId2};
            List<BroadcastFilter> registeredReceivers = null;
            List<ResolveInfo> listCollectReceiverComponents = (intent2.getFlags() & 1073741824) == 0 ? collectReceiverComponents(intent2, resolvedType, callingUid, users) : null;
            if (intent2.getComponent() == null) {
                if (userId2 == -1 && callingUid == PENDING_ASSIST_EXTRAS_LONG_TIMEOUT) {
                    for (int i3 = 0; i3 < users.length; i3++) {
                        if (!this.mUserController.hasUserRestriction("no_debugging_features", users[i3])) {
                            List<BroadcastFilter> registeredReceiversForUser = this.mReceiverResolver.queryIntent(intent2, resolvedType, false, users[i3]);
                            if (registeredReceivers == null) {
                                registeredReceivers = registeredReceiversForUser;
                            } else if (registeredReceiversForUser != null) {
                                registeredReceivers.addAll(registeredReceiversForUser);
                            }
                        }
                    }
                } else {
                    registeredReceivers = this.mReceiverResolver.queryIntent(intent2, resolvedType, false, userId2);
                }
            }
            AMEventHookData.BeforeSendBroadcast eventData = AMEventHookData.BeforeSendBroadcast.createInstance();
            List<String> filterStaticList = new ArrayList<>();
            List<String> filterDynamicList = new ArrayList<>();
            eventData.set(new Object[]{intent2, filterStaticList, filterDynamicList});
            AMEventHookResult eventResult = this.mAMEventHook.hook(AMEventHook.Event.AM_BeforeSendBroadcast, eventData);
            if (AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_FilterRegisteredReceiver)) {
                filterRegisteredReceivers(registeredReceivers, filterDynamicList);
            }
            if (AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_FilterStaticReceiver)) {
                filterStaticReceivers(listCollectReceiverComponents, filterStaticList);
            }
            boolean replacePending = (intent2.getFlags() & 536870912) != 0;
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "Enqueing broadcast: " + intent2.getAction() + " replacePending=" + replacePending);
            }
            int NR = registeredReceivers != null ? registeredReceivers.size() : 0;
            if (!ordered && NR > 0) {
                BroadcastQueue queue2 = broadcastQueueForIntent(intent2);
                BroadcastRecord r = new BroadcastRecord(queue2, intent2, callerApp, callerPackage, callingPid, callingUid, resolvedType, requiredPermissions, appOp, brOptions, registeredReceivers, resultTo, resultCode, resultData, resultExtras, ordered, sticky, false, userId2);
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Enqueueing parallel broadcast " + r);
                }
                r.enqueueTime = SystemClock.uptimeMillis();
                boolean replaced = replacePending ? queue2.replaceParallelBroadcastLocked(r) : false;
                if (!replaced) {
                    queue2.enqueueParallelBroadcastLocked(r);
                    queue2.scheduleBroadcastsLocked();
                }
                registeredReceivers = null;
                NR = 0;
            }
            int ir = 0;
            if (listCollectReceiverComponents != null) {
                String[] skipPackages = null;
                if ("android.intent.action.PACKAGE_ADDED".equals(intent2.getAction()) || "android.intent.action.PACKAGE_RESTARTED".equals(intent2.getAction()) || "android.intent.action.PACKAGE_DATA_CLEARED".equals(intent2.getAction())) {
                    Uri data5 = intent2.getData();
                    if (data5 != null && (pkgName = data5.getSchemeSpecificPart()) != null) {
                        skipPackages = new String[]{pkgName};
                    }
                } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(intent2.getAction())) {
                    skipPackages = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
                }
                if (skipPackages != null && skipPackages.length > 0) {
                    for (String skipPackage : skipPackages) {
                        if (skipPackage != null) {
                            int NT = listCollectReceiverComponents.size();
                            int it = 0;
                            while (it < NT) {
                                ResolveInfo curt = listCollectReceiverComponents.get(it);
                                if (curt.activityInfo.packageName.equals(skipPackage)) {
                                    listCollectReceiverComponents.remove(it);
                                    it--;
                                    NT--;
                                }
                                it++;
                            }
                        }
                    }
                }
                int NT2 = listCollectReceiverComponents != null ? listCollectReceiverComponents.size() : 0;
                int it2 = 0;
                ResolveInfo curt2 = null;
                BroadcastFilter broadcastFilter = null;
                while (it2 < NT2 && ir < NR) {
                    if (curt2 == null) {
                        curt2 = listCollectReceiverComponents.get(it2);
                    }
                    if (broadcastFilter == null) {
                        broadcastFilter = registeredReceivers.get(ir);
                    }
                    if (broadcastFilter.getPriority() >= curt2.priority) {
                        listCollectReceiverComponents.add(it2, broadcastFilter);
                        ir++;
                        broadcastFilter = null;
                        it2++;
                        NT2++;
                    } else {
                        it2++;
                        curt2 = null;
                    }
                }
            }
            while (ir < NR) {
                if (listCollectReceiverComponents == null) {
                    listCollectReceiverComponents = new ArrayList<>();
                }
                listCollectReceiverComponents.add(registeredReceivers.get(ir));
                ir++;
            }
            if ((listCollectReceiverComponents == null || listCollectReceiverComponents.size() <= 0) && resultTo == null) {
                if (intent2.getComponent() != null || intent2.getPackage() != null || (intent2.getFlags() & 1073741824) != 0) {
                    return 0;
                }
                addBroadcastStatLocked(intent2.getAction(), callerPackage, 0, 0, 0L);
                return 0;
            }
            BroadcastQueue queue3 = broadcastQueueForIntent(intent2);
            BroadcastRecord r2 = new BroadcastRecord(queue3, intent2, callerApp, callerPackage, callingPid, callingUid, resolvedType, requiredPermissions, appOp, brOptions, listCollectReceiverComponents, resultTo, resultCode, resultData, resultExtras, ordered, sticky, false, userId2);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "Enqueueing ordered broadcast " + r2 + ": prev had " + queue3.mOrderedBroadcasts.size());
            }
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.i(TAG_BROADCAST, "Enqueueing broadcast " + r2.intent.getAction());
            }
            r2.enqueueTime = SystemClock.uptimeMillis();
            boolean replaced2 = replacePending ? queue3.replaceOrderedBroadcastLocked(r2) : false;
            if (replaced2) {
                return 0;
            }
            queue3.enqueueOrderedBroadcastLocked(r2);
            queue3.scheduleBroadcastsLocked();
            return 0;
        } catch (RemoteException e3) {
            Slog.w(TAG, "Remote exception", e3);
            return 0;
        }
    }

    final void addBroadcastStatLocked(String action, String srcPackage, int receiveCount, int skipCount, long dispatchTime) {
        long now = SystemClock.elapsedRealtime();
        if (this.mCurBroadcastStats == null || this.mCurBroadcastStats.mStartRealtime + 86400000 < now) {
            this.mLastBroadcastStats = this.mCurBroadcastStats;
            if (this.mLastBroadcastStats != null) {
                this.mLastBroadcastStats.mEndRealtime = SystemClock.elapsedRealtime();
                this.mLastBroadcastStats.mEndUptime = SystemClock.uptimeMillis();
            }
            this.mCurBroadcastStats = new BroadcastStats();
        }
        this.mCurBroadcastStats.addBroadcast(action, srcPackage, receiveCount, skipCount, dispatchTime);
    }

    final Intent verifyBroadcastLocked(Intent intent) {
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        int flags = intent.getFlags();
        if (!this.mProcessesReady && (67108864 & flags) == 0 && (1073741824 & flags) == 0) {
            Slog.e(TAG, "Attempt to launch receivers of broadcast intent " + intent + " before boot completion");
            throw new IllegalStateException("Cannot broadcast before boot completed");
        }
        if ((33554432 & flags) != 0) {
            throw new IllegalArgumentException("Can't use FLAG_RECEIVER_BOOT_UPGRADE here");
        }
        return intent;
    }

    public final int broadcastIntent(IApplicationThread caller, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean serialized, boolean sticky, int userId) {
        int res;
        enforceNotIsolatedCaller("broadcastIntent");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                Intent intent2 = verifyBroadcastLocked(intent);
                ProcessRecord callerApp = getRecordForAppLocked(caller);
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long origId = Binder.clearCallingIdentity();
                String suppressAction = "allowed";
                String callerAppPackageName = null;
                if (callerApp != null && callerApp.info != null) {
                    callerAppPackageName = callerApp.info.packageName;
                }
                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                    AMEventHookData.ReadyToStartComponent eventData = AMEventHookData.ReadyToStartComponent.createInstance();
                    eventData.set(new Object[]{callerAppPackageName, Integer.valueOf(callingUid), null, null, null, null, null, null, "broadcast", "allowed"});
                    this.mAMEventHook.hook(AMEventHook.Event.AM_ReadyToStartComponent, eventData);
                    suppressAction = eventData.getString(AMEventHookData.ReadyToStartComponent.Index.suppressAction);
                    if (!IS_USER_BUILD) {
                        Slog.i(TAG, "[process suppression] broadcastIntent: suppressAction = " + suppressAction);
                    }
                }
                if (suppressAction != null && suppressAction.equals("skipped")) {
                    res = 0;
                } else {
                    res = broadcastIntentLocked(callerApp, callerAppPackageName, intent2, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, appOp, bOptions, serialized, sticky, callingPid, callingUid, userId);
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return res;
    }

    int broadcastIntentInPackage(String packageName, int uid, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String requiredPermission, Bundle bOptions, boolean serialized, boolean sticky, int userId) {
        int res;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                Intent intent2 = verifyBroadcastLocked(intent);
                long origId = Binder.clearCallingIdentity();
                String[] strArr = requiredPermission == null ? null : new String[]{requiredPermission};
                res = 0;
                String suppressAction = "allowed";
                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                    AMEventHookData.ReadyToStartComponent eventData = AMEventHookData.ReadyToStartComponent.createInstance();
                    eventData.set(new Object[]{packageName, Integer.valueOf(uid), null, null, null, null, null, null, "broadcast_p", "allowed"});
                    this.mAMEventHook.hook(AMEventHook.Event.AM_ReadyToStartComponent, eventData);
                    suppressAction = eventData.getString(AMEventHookData.ReadyToStartComponent.Index.suppressAction);
                    Slog.d(TAG, "[process suppression] suppressAction = " + suppressAction);
                }
                if (suppressAction != null && suppressAction.equals("skipped")) {
                    Slog.d(TAG, "[process suppression] broadcastIntentInPackage : skip sending broadcast by pendingintent");
                } else {
                    Slog.d(TAG, "[process suppression] broadcastIntentInPackage : allow sending broadcast by pendingintent");
                    res = broadcastIntentLocked(null, packageName, intent2, resolvedType, resultTo, resultCode, resultData, resultExtras, strArr, -1, bOptions, serialized, sticky, -1, uid, userId);
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return res;
    }

    public final void unbroadcastIntent(IApplicationThread caller, Intent intent, int userId) {
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, 0, "removeStickyBroadcast", null);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (checkCallingPermission("android.permission.BROADCAST_STICKY") != 0) {
                    String msg = "Permission Denial: unbroadcastIntent() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.BROADCAST_STICKY";
                    Slog.w(TAG, msg);
                    throw new SecurityException(msg);
                }
                ArrayMap<String, ArrayList<Intent>> stickies = this.mStickyBroadcasts.get(userId2);
                if (stickies != null) {
                    ArrayList<Intent> list = stickies.get(intent.getAction());
                    if (list != null) {
                        int N = list.size();
                        int i = 0;
                        while (true) {
                            if (i >= N) {
                                break;
                            }
                            if (intent.filterEquals(list.get(i))) {
                                list.remove(i);
                                break;
                            }
                            i++;
                        }
                        if (list.size() <= 0) {
                            stickies.remove(intent.getAction());
                        }
                    }
                    if (stickies.size() <= 0) {
                        this.mStickyBroadcasts.remove(userId2);
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void backgroundServicesFinishedLocked(int userId) {
        for (BroadcastQueue queue : this.mBroadcastQueues) {
            queue.backgroundServicesFinishedLocked(userId);
        }
    }

    public void finishReceiver(IBinder who, int resultCode, String resultData, Bundle resultExtras, boolean resultAbort, int flags) {
        BroadcastRecord r;
        if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "BDC-Finish receiver: " + who);
        }
        if (resultExtras != null && resultExtras.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }
        long origId = Binder.clearCallingIdentity();
        boolean doNext = false;
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    BroadcastQueue queue = (268435456 & flags) != 0 ? this.mFgBroadcastQueue : this.mBgBroadcastQueue;
                    r = queue.getMatchingOrderedReceiver(who);
                    if (r != null) {
                        if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.d(TAG_BROADCAST, r + ", spend: " + (SystemClock.uptimeMillis() - r.receiverTime) + (r.receiver != null ? ", " + r.receiver : "") + (r.curFilter != null ? ", " + r.curFilter : "") + (r.curReceiver != null ? ", " + r.curReceiver : ""));
                        }
                        doNext = r.queue.finishReceiverLocked(r, resultCode, resultData, resultExtras, resultAbort, true);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            if (doNext) {
                r.queue.processNextBroadcast(false);
            }
            trimApplications();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean startInstrumentation(ComponentName className, String profileFile, int flags, Bundle arguments, IInstrumentationWatcher watcher, IUiAutomationConnection uiAutomationConnection, int userId, String abiOverride) {
        enforceNotIsolatedCaller("startInstrumentation");
        int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "startInstrumentation", null);
        if (arguments != null && arguments.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                InstrumentationInfo ii = null;
                ApplicationInfo ai = null;
                try {
                    ii = this.mContext.getPackageManager().getInstrumentationInfo(className, 1024);
                    ai = AppGlobals.getPackageManager().getApplicationInfo(ii.targetPackage, 1024, userId2);
                } catch (PackageManager.NameNotFoundException e) {
                } catch (RemoteException e2) {
                }
                if (ii == null) {
                    reportStartInstrumentationFailureLocked(watcher, className, "Unable to find instrumentation info for: " + className);
                    resetPriorityAfterLockedSection();
                    return false;
                }
                if (ai == null) {
                    reportStartInstrumentationFailureLocked(watcher, className, "Unable to find instrumentation target package: " + ii.targetPackage);
                    resetPriorityAfterLockedSection();
                    return false;
                }
                if (!ai.hasCode()) {
                    reportStartInstrumentationFailureLocked(watcher, className, "Instrumentation target has no code: " + ii.targetPackage);
                    resetPriorityAfterLockedSection();
                    return false;
                }
                int match = this.mContext.getPackageManager().checkSignatures(ii.targetPackage, ii.packageName);
                if (match < 0 && match != -1) {
                    String msg = "Permission Denial: starting instrumentation " + className + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingPid() + " not allowed because package " + ii.packageName + " does not have a signature matching the target " + ii.targetPackage;
                    reportStartInstrumentationFailureLocked(watcher, className, msg);
                    throw new SecurityException(msg);
                }
                long origId = Binder.clearCallingIdentity();
                forceStopPackageLocked(ii.targetPackage, -1, true, false, true, true, false, userId2, "start instr");
                ProcessRecord app = addAppLocked(ai, false, abiOverride);
                app.instrumentationClass = className;
                app.instrumentationInfo = ai;
                app.instrumentationProfileFile = profileFile;
                app.instrumentationArguments = arguments;
                app.instrumentationWatcher = watcher;
                app.instrumentationUiAutomationConnection = uiAutomationConnection;
                app.instrumentationResultClass = className;
                Binder.restoreCallingIdentity(origId);
                resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void reportStartInstrumentationFailureLocked(IInstrumentationWatcher watcher, ComponentName cn, String report) {
        Slog.w(TAG, report);
        if (watcher == null) {
            return;
        }
        Bundle results = new Bundle();
        results.putString("id", "ActivityManagerService");
        results.putString("Error", report);
        this.mInstrumentationReporter.reportStatus(watcher, cn, -1, results);
    }

    void finishInstrumentationLocked(ProcessRecord app, int resultCode, Bundle results) {
        if (app.instrumentationWatcher != null) {
            this.mInstrumentationReporter.reportFinished(app.instrumentationWatcher, app.instrumentationClass, resultCode, results);
        }
        if (app.instrumentationUiAutomationConnection != null) {
            this.mHandler.obtainMessage(SHUTDOWN_UI_AUTOMATION_CONNECTION_MSG, app.instrumentationUiAutomationConnection).sendToTarget();
        }
        app.instrumentationWatcher = null;
        app.instrumentationUiAutomationConnection = null;
        app.instrumentationClass = null;
        app.instrumentationInfo = null;
        app.instrumentationProfileFile = null;
        app.instrumentationArguments = null;
        forceStopPackageLocked(app.info.packageName, -1, false, false, true, true, false, app.userId, "finished inst");
    }

    public void finishInstrumentation(IApplicationThread target, int resultCode, Bundle results) {
        UserHandle.getCallingUserId();
        if (results != null && results.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ProcessRecord app = getRecordForAppLocked(target);
                if (app == null) {
                    Slog.w(TAG, "finishInstrumentation: no app for " + target);
                    return;
                }
                long origId = Binder.clearCallingIdentity();
                finishInstrumentationLocked(app, resultCode, results);
                Binder.restoreCallingIdentity(origId);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public ConfigurationInfo getDeviceConfigurationInfo() {
        ConfigurationInfo config = new ConfigurationInfo();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                config.reqTouchScreen = this.mConfiguration.touchscreen;
                config.reqKeyboardType = this.mConfiguration.keyboard;
                config.reqNavigation = this.mConfiguration.navigation;
                if (this.mConfiguration.navigation == 2 || this.mConfiguration.navigation == 3) {
                    config.reqInputFeatures |= 2;
                }
                if (this.mConfiguration.keyboard != 0 && this.mConfiguration.keyboard != 1) {
                    config.reqInputFeatures |= 1;
                }
                config.reqGlEsVersion = this.GL_ES_VERSION;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return config;
    }

    ActivityStack getFocusedStack() {
        return this.mStackSupervisor.getFocusedStack();
    }

    public int getFocusedStackId() throws RemoteException {
        ActivityStack focusedStack = getFocusedStack();
        if (focusedStack != null) {
            return focusedStack.getStackId();
        }
        return -1;
    }

    public Configuration getConfiguration() {
        Configuration ci;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ci = new Configuration(this.mConfiguration);
                ci.userSetLocale = false;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return ci;
    }

    public void suppressResizeConfigChanges(boolean suppress) throws RemoteException {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "suppressResizeConfigChanges()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mSuppressResizeConfigChanges = suppress;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void moveTasksToFullscreenStack(int fromStackId, boolean onTop) {
        enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "moveTasksToFullscreenStack()");
        if (fromStackId == 0) {
            throw new IllegalArgumentException("You can't move tasks from the home stack.");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                try {
                    this.mStackSupervisor.moveTasksToFullscreenStackLocked(fromStackId, onTop);
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void updatePersistentConfiguration(Configuration values) {
        enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
        enforceWriteSettingsPermission("updateConfiguration()");
        if (values == null) {
            throw new NullPointerException("Configuration must not be null");
        }
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                updateConfigurationLocked(values, null, false, true, userId);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void updateFontScaleIfNeeded() {
        int currentUserId;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                currentUserId = this.mUserController.getCurrentUserIdLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        float scaleFactor = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "font_scale", 1.0f, currentUserId);
        if (this.mConfiguration.fontScale == scaleFactor) {
            return;
        }
        Configuration configuration = this.mWindowManager.computeNewConfiguration();
        configuration.fontScale = scaleFactor;
        enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
        enforceWriteSettingsPermission("updateConfiguration()");
        if (configuration == null) {
            throw new NullPointerException("Configuration must not be null");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                updateConfigurationLocked(configuration, null, false, false, currentUserId);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void enforceWriteSettingsPermission(String func) {
        int uid = Binder.getCallingUid();
        if (uid == 0 || Settings.checkAndNoteWriteSettingsOperation(this.mContext, uid, Settings.getPackageNameForUid(this.mContext, uid), false)) {
            return;
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + uid + " requires android.permission.WRITE_SETTINGS";
        Slog.w(TAG, msg);
        throw new SecurityException(msg);
    }

    public void updateConfiguration(Configuration values) {
        enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (values == null && this.mWindowManager != null) {
                    values = this.mWindowManager.computeNewConfiguration();
                }
                if (this.mWindowManager != null) {
                    this.mProcessList.applyDisplaySize(this.mWindowManager);
                }
                long origId = Binder.clearCallingIdentity();
                if (values != null) {
                    Settings.System.clearConfiguration(values);
                }
                updateConfigurationLocked(values, null, false);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void updateUserConfigurationLocked() {
        Configuration configuration = new Configuration(this.mConfiguration);
        Settings.System.adjustConfigurationForUser(this.mContext.getContentResolver(), configuration, this.mUserController.getCurrentUserIdLocked(), Settings.System.canWrite(this.mContext));
        updateConfigurationLocked(configuration, null, false);
    }

    boolean updateConfigurationLocked(Configuration values, ActivityRecord starting, boolean initLocale) {
        return updateConfigurationLocked(values, starting, initLocale, false, -10000);
    }

    private boolean updateConfigurationLocked(Configuration values, ActivityRecord starting, boolean initLocale, boolean persistent, int userId) {
        int[] resizedStacks;
        int changes = 0;
        if (this.mWindowManager != null) {
            this.mWindowManager.deferSurfaceLayout();
        }
        if (values != null) {
            Configuration newConfig = new Configuration(this.mConfiguration);
            changes = newConfig.updateFrom(values);
            if (changes != 0) {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.i(TAG_CONFIGURATION, "Updating configuration to: " + values);
                }
                EventLog.writeEvent(EventLogTags.CONFIGURATION_CHANGED, changes);
                if (!initLocale && !values.getLocales().isEmpty() && values.userSetLocale) {
                    LocaleList locales = values.getLocales();
                    int bestLocaleIndex = 0;
                    if (locales.size() > 1) {
                        if (this.mSupportedSystemLocales == null) {
                            this.mSupportedSystemLocales = Resources.getSystem().getAssets().getLocales();
                        }
                        bestLocaleIndex = Math.max(0, locales.getFirstMatchIndex(this.mSupportedSystemLocales));
                    }
                    SystemProperties.set("persist.sys.locale", locales.get(bestLocaleIndex).toLanguageTag());
                    LocaleList.setDefault(locales, bestLocaleIndex);
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(47, locales.get(bestLocaleIndex)));
                }
                this.mConfigurationSeq++;
                if (this.mConfigurationSeq <= 0) {
                    this.mConfigurationSeq = 1;
                }
                newConfig.seq = this.mConfigurationSeq;
                this.mConfiguration = newConfig;
                Slog.i(TAG, "Config changes=" + Integer.toHexString(changes) + " " + newConfig);
                this.mUsageStatsService.reportConfigurationChange(newConfig, this.mUserController.getCurrentUserIdLocked());
                Configuration configCopy = new Configuration(this.mConfiguration);
                this.mShowDialogs = shouldShowDialogs(newConfig, this.mInVrMode);
                AttributeCache ac = AttributeCache.instance();
                if (ac != null) {
                    ac.updateConfiguration(configCopy);
                }
                this.mSystemThread.applyConfigurationToResources(configCopy);
                if (persistent && Settings.System.hasInterestingConfigurationChanges(changes)) {
                    Message msg = this.mHandler.obtainMessage(4);
                    msg.obj = new Configuration(configCopy);
                    msg.arg1 = userId;
                    this.mHandler.sendMessage(msg);
                }
                boolean isDensityChange = (changes & 4096) != 0;
                if (isDensityChange) {
                    this.mUiHandler.sendEmptyMessage(70);
                    killAllBackgroundProcessesExcept(24, 4);
                }
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord app = this.mLruProcesses.get(i);
                    try {
                        if (app.thread != null) {
                            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                Slog.v(TAG_CONFIGURATION, "Sending to proc " + app.processName + " new config " + this.mConfiguration);
                            }
                            app.thread.scheduleConfigurationChanged(configCopy);
                        }
                    } catch (Exception e) {
                    }
                }
                Intent intent = new Intent("android.intent.action.CONFIGURATION_CHANGED");
                intent.addFlags(1879048192);
                broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, MY_PID, 1000, -1);
                if ((changes & 4) != 0) {
                    ShortcutServiceInternal shortcutService = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
                    if (shortcutService != null) {
                        shortcutService.onSystemLocaleChangedNoLock();
                    }
                    Intent intent2 = new Intent("android.intent.action.LOCALE_CHANGED");
                    intent2.addFlags(268435456);
                    if (!this.mProcessesReady) {
                        intent2.addFlags(1073741824);
                    }
                    broadcastIntentLocked(null, null, intent2, null, null, 0, null, null, null, -1, null, false, false, MY_PID, 1000, -1);
                    Settings.System.putString(this.mContext.getContentResolver(), "bc_locale_language", values.locale == null ? null : values.locale.getLanguage());
                }
            }
            if (this.mWindowManager != null && (resizedStacks = this.mWindowManager.setNewConfiguration(this.mConfiguration)) != null) {
                for (int stackId : resizedStacks) {
                    Rect newBounds = this.mWindowManager.getBoundsForNewConfiguration(stackId);
                    this.mStackSupervisor.resizeStackLocked(stackId, newBounds, null, null, false, false, false);
                }
            }
        }
        boolean kept = true;
        ActivityStack mainStack = this.mStackSupervisor.getFocusedStack();
        if (mainStack != null) {
            if (changes != 0 && starting == null) {
                starting = mainStack.topRunningActivityLocked();
            }
            if (starting != null) {
                kept = mainStack.ensureActivityConfigurationLocked(starting, changes, false);
                this.mStackSupervisor.ensureActivitiesVisibleLocked(starting, changes, false);
            }
        }
        if (this.mWindowManager != null) {
            this.mWindowManager.continueSurfaceLayout();
        }
        return kept;
    }

    private static final boolean shouldShowDialogs(Configuration config, boolean inVrMode) {
        boolean inputMethodExists = (config.keyboard == 1 && config.touchscreen == 1 && config.navigation == 1) ? false : true;
        boolean uiIsNotCarType = (config.uiMode & 15) != 3;
        return inputMethodExists && uiIsNotCarType && !inVrMode;
    }

    public boolean shouldUpRecreateTask(IBinder token, String destAffinity) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord srec = ActivityRecord.forTokenLocked(token);
                if (srec == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean zShouldUpRecreateTaskLocked = srec.task.stack.shouldUpRecreateTaskLocked(srec, destAffinity);
                resetPriorityAfterLockedSection();
                return zShouldUpRecreateTaskLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean navigateUpTo(IBinder token, Intent destIntent, int resultCode, Intent resultData) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean zNavigateUpToLocked = r.task.stack.navigateUpToLocked(r, destIntent, resultCode, resultData);
                resetPriorityAfterLockedSection();
                return zNavigateUpToLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getLaunchedFromUid(IBinder activityToken) {
        ActivityRecord srec;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                srec = ActivityRecord.forTokenLocked(activityToken);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        if (srec == null) {
            return -1;
        }
        return srec.launchedFromUid;
    }

    public String getLaunchedFromPackage(IBinder activityToken) {
        ActivityRecord srec;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                srec = ActivityRecord.forTokenLocked(activityToken);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (srec == null) {
            return null;
        }
        return srec.launchedFromPackage;
    }

    private BroadcastQueue isReceivingBroadcast(ProcessRecord app) {
        BroadcastRecord r = app.curReceiver;
        if (r != null) {
            return r.queue;
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                for (BroadcastQueue queue : this.mBroadcastQueues) {
                    BroadcastRecord r2 = queue.mPendingBroadcast;
                    if (r2 != null && r2.curApp == app) {
                        resetPriorityAfterLockedSection();
                        return queue;
                    }
                }
                resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    Association startAssociationLocked(int sourceUid, String sourceProcess, int sourceState, int targetUid, ComponentName targetComponent, String targetProcess) {
        if (!this.mTrackingAssociations) {
            return null;
        }
        ArrayMap<ComponentName, SparseArray<ArrayMap<String, Association>>> components = this.mAssociations.get(targetUid);
        if (components == null) {
            components = new ArrayMap<>();
            this.mAssociations.put(targetUid, components);
        }
        SparseArray<ArrayMap<String, Association>> sourceUids = components.get(targetComponent);
        if (sourceUids == null) {
            sourceUids = new SparseArray<>();
            components.put(targetComponent, sourceUids);
        }
        ArrayMap<String, Association> sourceProcesses = sourceUids.get(sourceUid);
        if (sourceProcesses == null) {
            sourceProcesses = new ArrayMap<>();
            sourceUids.put(sourceUid, sourceProcesses);
        }
        Association ass = sourceProcesses.get(sourceProcess);
        if (ass == null) {
            ass = new Association(sourceUid, sourceProcess, targetUid, targetComponent, targetProcess);
            sourceProcesses.put(sourceProcess, ass);
        }
        ass.mCount++;
        ass.mNesting++;
        if (ass.mNesting == 1) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            ass.mLastStateUptime = jUptimeMillis;
            ass.mStartTime = jUptimeMillis;
            ass.mLastState = sourceState;
        }
        return ass;
    }

    void stopAssociationLocked(int sourceUid, String sourceProcess, int targetUid, ComponentName targetComponent) {
        ArrayMap<ComponentName, SparseArray<ArrayMap<String, Association>>> components;
        SparseArray<ArrayMap<String, Association>> sourceUids;
        ArrayMap<String, Association> sourceProcesses;
        Association ass;
        if (!this.mTrackingAssociations || (components = this.mAssociations.get(targetUid)) == null || (sourceUids = components.get(targetComponent)) == null || (sourceProcesses = sourceUids.get(sourceUid)) == null || (ass = sourceProcesses.get(sourceProcess)) == null || ass.mNesting <= 0) {
            return;
        }
        ass.mNesting--;
        if (ass.mNesting != 0) {
            return;
        }
        long uptime = SystemClock.uptimeMillis();
        ass.mTime += uptime - ass.mStartTime;
        long[] jArr = ass.mStateTimes;
        int i = ass.mLastState + 1;
        jArr[i] = jArr[i] + (uptime - ass.mLastStateUptime);
        ass.mLastState = 18;
    }

    private void noteUidProcessState(int uid, int state) {
        this.mBatteryStatsService.noteUidProcessState(uid, state);
        if (!this.mTrackingAssociations) {
            return;
        }
        int N1 = this.mAssociations.size();
        for (int i1 = 0; i1 < N1; i1++) {
            ArrayMap<ComponentName, SparseArray<ArrayMap<String, Association>>> targetComponents = this.mAssociations.valueAt(i1);
            int N2 = targetComponents.size();
            for (int i2 = 0; i2 < N2; i2++) {
                SparseArray<ArrayMap<String, Association>> sourceUids = targetComponents.valueAt(i2);
                ArrayMap<String, Association> sourceProcesses = sourceUids.get(uid);
                if (sourceProcesses != null) {
                    int N4 = sourceProcesses.size();
                    for (int i4 = 0; i4 < N4; i4++) {
                        Association ass = sourceProcesses.valueAt(i4);
                        if (ass.mNesting >= 1) {
                            long uptime = SystemClock.uptimeMillis();
                            long[] jArr = ass.mStateTimes;
                            int i = ass.mLastState + 1;
                            jArr[i] = jArr[i] + (uptime - ass.mLastStateUptime);
                            ass.mLastState = state;
                            ass.mLastStateUptime = uptime;
                        }
                    }
                }
            }
        }
    }

    private final int computeOomAdjLocked(ProcessRecord app, int cachedAdj, ProcessRecord TOP_APP, boolean doingAll, long now) {
        int schedGroup;
        int adj;
        int procState;
        int layer;
        if (this.mAdjSeq == app.adjSeq) {
            return app.curRawAdj;
        }
        if (app.thread == null) {
            app.adjSeq = this.mAdjSeq;
            app.curSchedGroup = 0;
            app.curProcState = 16;
            app.curRawAdj = 906;
            app.curAdj = 906;
            return 906;
        }
        app.adjTypeCode = 0;
        app.adjSource = null;
        app.adjTarget = null;
        app.empty = false;
        app.cached = false;
        int activitiesSize = app.activities.size();
        if (app.maxAdj <= 0) {
            app.adjType = "fixed";
            app.adjSeq = this.mAdjSeq;
            app.curRawAdj = app.maxAdj;
            app.foregroundActivities = false;
            app.curSchedGroup = 1;
            app.curProcState = 0;
            app.systemNoUi = true;
            if (app == TOP_APP) {
                app.systemNoUi = false;
                app.curSchedGroup = 2;
                app.adjType = "pers-top-activity";
            } else if (activitiesSize > 0) {
                for (int j = 0; j < activitiesSize; j++) {
                    if (app.activities.get(j).visible) {
                        app.systemNoUi = false;
                    }
                }
            }
            if (!app.systemNoUi) {
                app.curProcState = 1;
            }
            int i = app.maxAdj;
            app.curAdj = i;
            return i;
        }
        app.systemNoUi = false;
        int PROCESS_STATE_CUR_TOP = this.mTopProcessState;
        boolean foregroundActivities = false;
        if (app == TOP_APP) {
            adj = 0;
            schedGroup = 2;
            app.adjType = "top-activity";
            foregroundActivities = true;
            procState = PROCESS_STATE_CUR_TOP;
        } else if (app.instrumentationClass != null) {
            adj = 0;
            schedGroup = 1;
            app.adjType = "instrumentation";
            procState = 4;
        } else {
            BroadcastQueue queue = isReceivingBroadcast(app);
            if (queue != null) {
                adj = 0;
                schedGroup = queue == this.mFgBroadcastQueue ? 1 : 0;
                app.adjType = "broadcast";
                procState = 11;
            } else if (app.executingServices.size() > 0) {
                adj = 0;
                schedGroup = app.execServicesFg ? 1 : 0;
                app.adjType = "exec-service";
                procState = 10;
            } else {
                schedGroup = 0;
                adj = cachedAdj;
                procState = 16;
                app.cached = true;
                app.empty = true;
                app.adjType = "cch-empty";
            }
        }
        if (!foregroundActivities && activitiesSize > 0) {
            int minLayer = 99;
            int j2 = 0;
            while (true) {
                if (j2 >= activitiesSize) {
                    break;
                }
                ActivityRecord r = app.activities.get(j2);
                if (r.app != app) {
                    Log.e(TAG, "Found activity " + r + " in proc activity list using " + r.app + " instead of expected " + app);
                    if (r.app != null && r.app.uid != app.uid) {
                        j2++;
                    } else {
                        r.app = app;
                        if (!r.visible) {
                            if (adj > 100) {
                                adj = 100;
                                app.adjType = "visible";
                            }
                            if (procState > PROCESS_STATE_CUR_TOP) {
                                procState = PROCESS_STATE_CUR_TOP;
                            }
                            schedGroup = 1;
                            app.cached = false;
                            app.empty = false;
                            foregroundActivities = true;
                            if (r.task != null && (layer = r.task.mLayerRank) >= 0 && 99 > layer) {
                                minLayer = layer;
                            }
                        } else {
                            if (r.state == ActivityStack.ActivityState.PAUSING || r.state == ActivityStack.ActivityState.PAUSED) {
                                if (adj > FIRST_BROADCAST_QUEUE_MSG) {
                                    adj = FIRST_BROADCAST_QUEUE_MSG;
                                    app.adjType = "pausing";
                                }
                                if (procState > PROCESS_STATE_CUR_TOP) {
                                    procState = PROCESS_STATE_CUR_TOP;
                                }
                                schedGroup = 1;
                                app.cached = false;
                                app.empty = false;
                                foregroundActivities = true;
                            } else if (r.state == ActivityStack.ActivityState.STOPPING) {
                                if (adj > FIRST_BROADCAST_QUEUE_MSG) {
                                    adj = FIRST_BROADCAST_QUEUE_MSG;
                                    app.adjType = "stopping";
                                }
                                if (!r.finishing && procState > 13) {
                                    procState = 13;
                                }
                                app.cached = false;
                                app.empty = false;
                                foregroundActivities = true;
                            } else if (procState > 14) {
                                procState = 14;
                                app.adjType = "cch-act";
                            }
                            j2++;
                        }
                    }
                } else if (!r.visible) {
                }
            }
            if (adj == 100) {
                adj += minLayer;
            }
        }
        if (adj > FIRST_BROADCAST_QUEUE_MSG || procState > 4) {
            if (app.foregroundServices) {
                adj = FIRST_BROADCAST_QUEUE_MSG;
                procState = 4;
                app.cached = false;
                app.adjType = "fg-service";
                schedGroup = 1;
            } else if (app.forcingToForeground != null) {
                adj = FIRST_BROADCAST_QUEUE_MSG;
                procState = 6;
                app.cached = false;
                app.adjType = "force-fg";
                app.adjSource = app.forcingToForeground;
                schedGroup = 1;
            }
        }
        if (app == this.mHeavyWeightProcess) {
            if (adj > 400) {
                adj = 400;
                schedGroup = 0;
                app.cached = false;
                app.adjType = "heavy";
            }
            if (procState > 9) {
                procState = 9;
            }
        }
        if (app == this.mHomeProcess) {
            if (adj > 600) {
                adj = 600;
                schedGroup = 0;
                app.cached = false;
                app.adjType = "home";
            }
            if (procState > 12) {
                procState = 12;
            }
        }
        if (app == this.mPreviousProcess && app.activities.size() > 0) {
            if (adj > 700) {
                adj = 700;
                schedGroup = 0;
                app.cached = false;
                app.adjType = "previous";
            }
            if (procState > 13) {
                procState = 13;
            }
        }
        app.adjSeq = this.mAdjSeq;
        app.curRawAdj = adj;
        app.hasStartedServices = false;
        if (this.mBackupTarget != null && app == this.mBackupTarget.app) {
            if (adj > FIRST_COMPAT_MODE_MSG) {
                if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
                    Slog.v(TAG_BACKUP, "oom BACKUP_APP_ADJ for " + app);
                }
                adj = FIRST_COMPAT_MODE_MSG;
                if (procState > 7) {
                    procState = 7;
                }
                app.adjType = "backup";
                app.cached = false;
            }
            if (procState > 8) {
                procState = 8;
            }
        }
        boolean mayBeTop = false;
        for (int is = app.services.size() - 1; is >= 0 && (adj > 0 || schedGroup == 0 || procState > 2); is--) {
            ServiceRecord s = app.services.valueAt(is);
            if (s.startRequested) {
                app.hasStartedServices = true;
                if (procState > 10) {
                    procState = 10;
                }
                if (app.hasShownUi && app != this.mHomeProcess) {
                    if (adj > 500) {
                        app.adjType = "cch-started-ui-services";
                    }
                } else {
                    if (now < s.lastActivity + BATTERY_STATS_TIME && adj > 500) {
                        adj = 500;
                        app.adjType = "started-services";
                        app.cached = false;
                    }
                    if (adj > 500) {
                        app.adjType = "cch-started-services";
                    }
                }
            }
            for (int conni = s.connections.size() - 1; conni >= 0 && (adj > 0 || schedGroup == 0 || procState > 2); conni--) {
                ArrayList<ConnectionRecord> clist = s.connections.valueAt(conni);
                for (int i2 = 0; i2 < clist.size() && (adj > 0 || schedGroup == 0 || procState > 2); i2++) {
                    ConnectionRecord cr = clist.get(i2);
                    if (cr.binding.client != app) {
                        if ((cr.flags & 32) == 0) {
                            ProcessRecord client = cr.binding.client;
                            int clientAdj = computeOomAdjLocked(client, cachedAdj, TOP_APP, doingAll, now);
                            int clientProcState = client.curProcState;
                            if (clientProcState >= 14) {
                                clientProcState = 16;
                            }
                            String adjType = null;
                            if ((cr.flags & 16) != 0) {
                                if (app.hasShownUi && app != this.mHomeProcess) {
                                    if (adj > clientAdj) {
                                        adjType = "cch-bound-ui-services";
                                    }
                                    app.cached = false;
                                    clientAdj = adj;
                                    clientProcState = procState;
                                } else if (now >= s.lastActivity + BATTERY_STATS_TIME) {
                                    if (adj > clientAdj) {
                                        adjType = "cch-bound-services";
                                    }
                                    clientAdj = adj;
                                }
                            }
                            if (adj > clientAdj) {
                                if (app.hasShownUi && app != this.mHomeProcess && clientAdj > FIRST_BROADCAST_QUEUE_MSG) {
                                    adjType = "cch-bound-ui-services";
                                } else {
                                    if ((cr.flags & 72) != 0) {
                                        adj = clientAdj >= -700 ? clientAdj : -700;
                                    } else if ((cr.flags & 1073741824) != 0 && clientAdj < FIRST_BROADCAST_QUEUE_MSG && adj > FIRST_BROADCAST_QUEUE_MSG) {
                                        adj = FIRST_BROADCAST_QUEUE_MSG;
                                    } else if (clientAdj >= FIRST_BROADCAST_QUEUE_MSG) {
                                        adj = clientAdj;
                                    } else if (adj > 100) {
                                        adj = Math.max(clientAdj, 100);
                                    }
                                    if (!client.cached) {
                                        app.cached = false;
                                    }
                                    adjType = "service";
                                }
                            }
                            if ((cr.flags & 4) == 0) {
                                if (client.curSchedGroup > schedGroup) {
                                    if ((cr.flags & 64) != 0) {
                                        schedGroup = client.curSchedGroup;
                                    } else {
                                        schedGroup = 1;
                                    }
                                }
                                if (clientProcState <= 2) {
                                    if (clientProcState == 2) {
                                        mayBeTop = true;
                                        clientProcState = 16;
                                    } else if ((cr.flags & 67108864) != 0) {
                                        clientProcState = 3;
                                    } else if (this.mWakefulness == 1 && (cr.flags & 33554432) != 0) {
                                        clientProcState = 3;
                                    } else {
                                        clientProcState = 6;
                                    }
                                }
                            } else if (clientProcState < 7) {
                                clientProcState = 7;
                            }
                            if (procState > clientProcState) {
                                procState = clientProcState;
                            }
                            if (procState < 7 && (cr.flags & 536870912) != 0) {
                                app.pendingUiClean = true;
                            }
                            if (adjType != null) {
                                app.adjType = adjType;
                                app.adjTypeCode = 2;
                                app.adjSource = cr.binding.client;
                                app.adjSourceProcState = clientProcState;
                                app.adjTarget = s.name;
                            }
                        }
                        if ((cr.flags & 134217728) != 0) {
                            app.treatLikeActivity = true;
                        }
                        ActivityRecord a = cr.activity;
                        if ((cr.flags & 128) != 0 && a != null && adj > 0 && (a.visible || a.state == ActivityStack.ActivityState.RESUMED || a.state == ActivityStack.ActivityState.PAUSING)) {
                            adj = 0;
                            if ((cr.flags & 4) == 0) {
                                if ((cr.flags & 64) != 0) {
                                    schedGroup = 2;
                                } else {
                                    schedGroup = 1;
                                }
                            }
                            app.cached = false;
                            app.adjType = "service";
                            app.adjTypeCode = 2;
                            app.adjSource = a;
                            app.adjSourceProcState = procState;
                            app.adjTarget = s.name;
                        }
                    }
                }
            }
        }
        for (int provi = app.pubProviders.size() - 1; provi >= 0 && (adj > 0 || schedGroup == 0 || procState > 2); provi--) {
            ContentProviderRecord cpr = app.pubProviders.valueAt(provi);
            for (int i3 = cpr.connections.size() - 1; i3 >= 0 && (adj > 0 || schedGroup == 0 || procState > 2); i3--) {
                ContentProviderConnection conn = cpr.connections.get(i3);
                ProcessRecord client2 = conn.client;
                if (client2 != app) {
                    int clientAdj2 = computeOomAdjLocked(client2, cachedAdj, TOP_APP, doingAll, now);
                    int clientProcState2 = client2.curProcState;
                    if (clientProcState2 >= 14) {
                        clientProcState2 = 16;
                    }
                    if (adj > clientAdj2) {
                        if (app.hasShownUi && app != this.mHomeProcess && clientAdj2 > FIRST_BROADCAST_QUEUE_MSG) {
                            app.adjType = "cch-ui-provider";
                        } else {
                            adj = clientAdj2 > 0 ? clientAdj2 : 0;
                            app.adjType = "provider";
                        }
                        app.cached &= client2.cached;
                        app.adjTypeCode = 1;
                        app.adjSource = client2;
                        app.adjSourceProcState = clientProcState2;
                        app.adjTarget = cpr.name;
                    }
                    if (clientProcState2 <= 2) {
                        if (clientProcState2 == 2) {
                            mayBeTop = true;
                            clientProcState2 = 16;
                        } else {
                            clientProcState2 = 3;
                        }
                    }
                    if (procState > clientProcState2) {
                        procState = clientProcState2;
                    }
                    if (client2.curSchedGroup > schedGroup) {
                        schedGroup = 1;
                    }
                }
            }
            if (cpr.hasExternalProcessHandles()) {
                if (adj > 0) {
                    adj = 0;
                    schedGroup = 1;
                    app.cached = false;
                    app.adjType = "provider";
                    app.adjTarget = cpr.name;
                }
                if (procState > 6) {
                    procState = 6;
                }
            }
        }
        if (app.lastProviderTime > 0 && app.lastProviderTime + 20000 > now) {
            if (adj > 700) {
                adj = 700;
                schedGroup = 0;
                app.cached = false;
                app.adjType = "provider";
            }
            if (procState > 13) {
                procState = 13;
            }
        }
        if (mayBeTop && procState > 2) {
            switch (procState) {
                case 6:
                case 7:
                case 10:
                    procState = 3;
                    break;
                case 8:
                case 9:
                default:
                    procState = 2;
                    break;
            }
        }
        if (procState >= 16) {
            if (app.hasClientActivities) {
                procState = 15;
                app.adjType = "cch-client-act";
            } else if (app.treatLikeActivity) {
                procState = 14;
                app.adjType = "cch-as-act";
            }
        }
        if (adj == 500) {
            if (doingAll) {
                app.serviceb = this.mNewNumAServiceProcs > this.mNumServiceProcs / 3;
                this.mNewNumServiceProcs++;
                if (!app.serviceb) {
                    if (this.mLastMemoryLevel > 0 && app.lastPss >= this.mProcessList.getCachedRestoreThresholdKb()) {
                        app.serviceHighRam = true;
                        app.serviceb = true;
                    } else {
                        this.mNewNumAServiceProcs++;
                    }
                } else {
                    app.serviceHighRam = false;
                }
            }
            if (app.serviceb) {
                adj = 800;
            }
        }
        if (SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1") && app == this.mWallpaperProcess && adj == 100 && !this.mIsWallpaperFg) {
            adj = 600;
        }
        int adj2 = getCustomizedAdj(app.processName, adj);
        app.curRawAdj = adj2;
        if (adj2 > app.maxAdj) {
            adj2 = app.maxAdj;
            if (app.maxAdj <= FIRST_BROADCAST_QUEUE_MSG) {
                schedGroup = 1;
            }
        }
        app.curAdj = app.modifyRawOomAdj(adj2);
        app.curSchedGroup = schedGroup;
        app.curProcState = procState;
        app.foregroundActivities = foregroundActivities;
        return app.curRawAdj;
    }

    void recordPssSampleLocked(final ProcessRecord proc, int procState, long pss, long uss, long swapPss, long now) {
        EventLogTags.writeAmPss(proc.pid, proc.uid, proc.processName, 1024 * pss, 1024 * uss, 1024 * swapPss);
        proc.lastPssTime = now;
        proc.baseProcessTracker.addPss(pss, uss, true, proc.pkgList);
        if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "PSS of " + proc.toShortString() + ": " + pss + " lastPss=" + proc.lastPss + " state=" + ProcessList.makeProcStateString(procState));
        }
        if (proc.initialIdlePss == 0) {
            proc.initialIdlePss = pss;
        }
        proc.lastPss = pss;
        proc.lastSwapPss = swapPss;
        if (procState >= 12) {
            proc.lastCachedPss = pss;
            proc.lastCachedSwapPss = swapPss;
        }
        if (SystemProperties.get("ro.mtk_aws_support").equals("1") && AWSManager.getInstance() != null) {
            AWSManager.getInstance().storeRecord(convertStoreRecord(proc, pss + swapPss));
        }
        SparseArray<Pair<Long, String>> watchUids = (SparseArray) this.mMemWatchProcesses.getMap().get(proc.processName);
        Long check = null;
        if (watchUids != null) {
            Pair<Long, String> val = watchUids.get(proc.uid);
            if (val == null) {
                val = watchUids.get(0);
            }
            if (val != null) {
                check = (Long) val.first;
            }
        }
        if (check == null || 1024 * pss < check.longValue() || proc.thread == null || this.mMemWatchDumpProcName != null) {
            return;
        }
        boolean isDebuggable = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
        if (!isDebuggable && (proc.info.flags & 2) != 0) {
            isDebuggable = true;
        }
        if (isDebuggable) {
            Slog.w(TAG, "Process " + proc + " exceeded pss limit " + check + "; reporting");
            final File heapdumpFile = DumpHeapProvider.getJavaFile();
            this.mMemWatchDumpProcName = proc.processName;
            this.mMemWatchDumpFile = heapdumpFile.toString();
            this.mMemWatchDumpPid = proc.pid;
            this.mMemWatchDumpUid = proc.uid;
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    ActivityManagerService.this.revokeUriPermission(ActivityThread.currentActivityThread().getApplicationThread(), DumpHeapActivity.JAVA_URI, 3, UserHandle.myUserId());
                    ParcelFileDescriptor fd = null;
                    try {
                        try {
                            heapdumpFile.delete();
                            fd = ParcelFileDescriptor.open(heapdumpFile, 771751936);
                            IApplicationThread thread = proc.thread;
                            if (thread != null) {
                                try {
                                    if (ActivityManagerDebugConfig.DEBUG_PSS) {
                                        Slog.d(ActivityManagerService.TAG_PSS, "Requesting dump heap from " + proc + " to " + heapdumpFile);
                                    }
                                    thread.dumpHeap(true, heapdumpFile.toString(), fd);
                                } catch (RemoteException e) {
                                }
                            }
                            if (fd != null) {
                                try {
                                    fd.close();
                                } catch (IOException e2) {
                                }
                            }
                        } catch (Throwable th) {
                            if (fd != null) {
                                try {
                                    fd.close();
                                } catch (IOException e3) {
                                }
                            }
                            throw th;
                        }
                    } catch (FileNotFoundException e4) {
                        e4.printStackTrace();
                        if (fd != null) {
                            try {
                                fd.close();
                            } catch (IOException e5) {
                            }
                        }
                    }
                }
            });
            return;
        }
        Slog.w(TAG, "Process " + proc + " exceeded pss limit " + check + ", but debugging not enabled");
    }

    void requestPssLocked(ProcessRecord proc, int procState) {
        if (this.mPendingPssProcesses.contains(proc)) {
            return;
        }
        if (this.mPendingPssProcesses.size() == 0) {
            this.mBgHandler.sendEmptyMessage(1);
        }
        if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "Requesting PSS of: " + proc);
        }
        proc.pssProcState = procState;
        this.mPendingPssProcesses.add(proc);
    }

    void requestPssAllProcsLocked(long now, boolean always, boolean memLowered) {
        if (!always) {
            if (now < this.mLastFullPssTime + ((long) (memLowered ? FULL_PSS_LOWERED_INTERVAL : 600000))) {
                return;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "Requesting PSS of all procs!  memLowered=" + memLowered);
        }
        this.mLastFullPssTime = now;
        this.mFullPssPending = true;
        this.mPendingPssProcesses.ensureCapacity(this.mLruProcesses.size());
        this.mPendingPssProcesses.clear();
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord app = this.mLruProcesses.get(i);
            if (app.thread != null && app.curProcState != -1 && (memLowered || now > app.lastStateTime + LocationFudger.FASTEST_INTERVAL_MS)) {
                app.pssProcState = app.setProcState;
                app.nextPssTime = ProcessList.computeNextPssTime(app.curProcState, true, this.mTestPssMode, isSleepingLocked(), now);
                this.mPendingPssProcesses.add(app);
            }
        }
        this.mBgHandler.sendEmptyMessage(1);
    }

    public void setTestPssMode(boolean enabled) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mTestPssMode = enabled;
                if (enabled) {
                    requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, true);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    final void performAppGcLocked(ProcessRecord app) {
        try {
            app.lastRequestedGc = SystemClock.uptimeMillis();
            if (app.thread != null) {
                if (app.reportLowMemory) {
                    app.reportLowMemory = false;
                    app.thread.scheduleLowMemory();
                } else {
                    app.thread.processInBackground();
                }
            }
        } catch (Exception e) {
        }
    }

    private final boolean canGcNowLocked() {
        boolean processingBroadcasts = false;
        for (BroadcastQueue q : this.mBroadcastQueues) {
            if (q.mParallelBroadcasts.size() != 0 || q.mOrderedBroadcasts.size() != 0) {
                processingBroadcasts = true;
            }
        }
        if (processingBroadcasts) {
            return false;
        }
        if (isSleepingLocked()) {
            return true;
        }
        return this.mStackSupervisor.allResumedActivitiesIdle();
    }

    final void performAppGcsLocked() {
        int N = this.mProcessesToGc.size();
        if (N <= 0 || !canGcNowLocked()) {
            return;
        }
        while (this.mProcessesToGc.size() > 0) {
            ProcessRecord proc = this.mProcessesToGc.remove(0);
            if (proc.curRawAdj > FIRST_BROADCAST_QUEUE_MSG || proc.reportLowMemory) {
                if (proc.lastRequestedGc + 60000 <= SystemClock.uptimeMillis()) {
                    performAppGcLocked(proc);
                    scheduleAppGcsLocked();
                    return;
                } else {
                    addProcessToGcListLocked(proc);
                    scheduleAppGcsLocked();
                }
            }
        }
        scheduleAppGcsLocked();
    }

    final void performAppGcsIfAppropriateLocked() {
        if (canGcNowLocked()) {
            performAppGcsLocked();
        } else {
            scheduleAppGcsLocked();
        }
    }

    final void scheduleAppGcsLocked() {
        this.mHandler.removeMessages(5);
        if (this.mProcessesToGc.size() <= 0) {
            return;
        }
        ProcessRecord proc = this.mProcessesToGc.get(0);
        Message msg = this.mHandler.obtainMessage(5);
        long when = proc.lastRequestedGc + 60000;
        long now = SystemClock.uptimeMillis();
        if (when < now + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC) {
            when = now + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
        }
        this.mHandler.sendMessageAtTime(msg, when);
    }

    final void addProcessToGcListLocked(ProcessRecord proc) {
        boolean added = false;
        int i = this.mProcessesToGc.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            if (this.mProcessesToGc.get(i).lastRequestedGc >= proc.lastRequestedGc) {
                i--;
            } else {
                added = true;
                this.mProcessesToGc.add(i + 1, proc);
                break;
            }
        }
        if (added) {
            return;
        }
        this.mProcessesToGc.add(0, proc);
    }

    final void scheduleAppGcLocked(ProcessRecord app) {
        long now = SystemClock.uptimeMillis();
        if (app.lastRequestedGc + 60000 > now || this.mProcessesToGc.contains(app)) {
            return;
        }
        addProcessToGcListLocked(app);
        scheduleAppGcsLocked();
    }

    final void checkExcessivePowerUsageLocked(boolean doKills) {
        long wtime;
        updateCpuStatsNow();
        BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
        boolean doWakeKills = doKills;
        boolean doCpuKills = doKills;
        if (this.mLastPowerCheckRealtime == 0) {
            doWakeKills = false;
        }
        if (this.mLastPowerCheckUptime == 0) {
            doCpuKills = false;
        }
        if (stats.isScreenOn()) {
            doWakeKills = false;
        }
        long curRealtime = SystemClock.elapsedRealtime();
        long realtimeSince = curRealtime - this.mLastPowerCheckRealtime;
        long curUptime = SystemClock.uptimeMillis();
        long uptimeSince = curUptime - this.mLastPowerCheckUptime;
        this.mLastPowerCheckRealtime = curRealtime;
        this.mLastPowerCheckUptime = curUptime;
        if (realtimeSince < WAKE_LOCK_MIN_CHECK_DURATION) {
            doWakeKills = false;
        }
        if (uptimeSince < CPU_MIN_CHECK_DURATION) {
            doCpuKills = false;
        }
        int i = this.mLruProcesses.size();
        while (i > 0) {
            i--;
            ProcessRecord app = this.mLruProcesses.get(i);
            if (app.setProcState >= 12) {
                synchronized (stats) {
                    wtime = stats.getProcessWakeTime(app.info.uid, app.pid, curRealtime);
                }
                long wtimeUsed = wtime - app.lastWakeTime;
                long cputimeUsed = app.curCpuTime - app.lastCpuTime;
                if (ActivityManagerDebugConfig.DEBUG_POWER) {
                    StringBuilder sb = new StringBuilder(128);
                    sb.append("Wake for ");
                    app.toShortString(sb);
                    sb.append(": over ");
                    TimeUtils.formatDuration(realtimeSince, sb);
                    sb.append(" used ");
                    TimeUtils.formatDuration(wtimeUsed, sb);
                    sb.append(" (");
                    sb.append((100 * wtimeUsed) / realtimeSince);
                    sb.append("%)");
                    Slog.i(TAG_POWER, sb.toString());
                    sb.setLength(0);
                    sb.append("CPU for ");
                    app.toShortString(sb);
                    sb.append(": over ");
                    TimeUtils.formatDuration(uptimeSince, sb);
                    sb.append(" used ");
                    TimeUtils.formatDuration(cputimeUsed, sb);
                    sb.append(" (");
                    sb.append((100 * cputimeUsed) / uptimeSince);
                    sb.append("%)");
                    Slog.i(TAG_POWER, sb.toString());
                }
                if (doWakeKills && realtimeSince > 0 && (100 * wtimeUsed) / realtimeSince >= 50) {
                    synchronized (stats) {
                        stats.reportExcessiveWakeLocked(app.info.uid, app.processName, realtimeSince, wtimeUsed);
                    }
                    app.kill("excessive wake held " + wtimeUsed + " during " + realtimeSince, true);
                    app.baseProcessTracker.reportExcessiveWake(app.pkgList);
                } else if (doCpuKills && uptimeSince > 0 && (100 * cputimeUsed) / uptimeSince >= 25) {
                    synchronized (stats) {
                        stats.reportExcessiveCpuLocked(app.info.uid, app.processName, uptimeSince, cputimeUsed);
                    }
                    app.kill("excessive cpu " + cputimeUsed + " during " + uptimeSince, true);
                    app.baseProcessTracker.reportExcessiveCpu(app.pkgList);
                } else {
                    app.lastWakeTime = wtime;
                    app.lastCpuTime = app.curCpuTime;
                }
            }
        }
    }

    private final boolean applyOomAdjLocked(ProcessRecord app, boolean doingAll, long now, long nowElapsed) {
        int processGroup;
        boolean success = true;
        if (app.curRawAdj != app.setRawAdj) {
            app.setRawAdj = app.curRawAdj;
        }
        int changes = 0;
        if (app.curAdj != app.setAdj) {
            ProcessList.setOomAdj(app.pid, app.info.uid, app.curAdj);
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.v(TAG_OOM_ADJ, "Set " + app.pid + " " + app.processName + " adj " + app.curAdj + ": " + app.adjType);
            }
            app.setAdj = app.curAdj;
            app.verifiedAdj = -10000;
        }
        if (app.setSchedGroup != app.curSchedGroup) {
            app.setSchedGroup = app.curSchedGroup;
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.v(TAG_OOM_ADJ, "Setting sched group of " + app.processName + " to " + app.curSchedGroup);
            }
            if (app.waitingToKill != null && app.curReceiver == null && app.setSchedGroup == 0) {
                app.kill(app.waitingToKill, true);
                success = false;
            } else {
                switch (app.curSchedGroup) {
                    case 0:
                        processGroup = 0;
                        break;
                    case 1:
                    default:
                        processGroup = -1;
                        break;
                    case 2:
                        processGroup = 5;
                        break;
                }
                long oldId = Binder.clearCallingIdentity();
                try {
                    Process.setProcessGroup(app.pid, processGroup);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed setting process group of " + app.pid + " to " + app.curSchedGroup);
                    e.printStackTrace();
                } finally {
                    Binder.restoreCallingIdentity(oldId);
                }
            }
        }
        if (app.repForegroundActivities != app.foregroundActivities) {
            app.repForegroundActivities = app.foregroundActivities;
            changes = 1;
        }
        if (app.repProcState != app.curProcState) {
            app.repProcState = app.curProcState;
            changes |= 2;
            if (app.thread != null) {
                try {
                    app.thread.setProcessState(app.repProcState);
                } catch (RemoteException e2) {
                }
            }
        }
        if (app.setProcState == -1 || ProcessList.procStatesDifferForMem(app.curProcState, app.setProcState)) {
            app.lastStateTime = now;
            app.nextPssTime = ProcessList.computeNextPssTime(app.curProcState, true, this.mTestPssMode, isSleepingLocked(), now);
            if (ActivityManagerDebugConfig.DEBUG_PSS) {
                Slog.d(TAG_PSS, "Process state change from " + ProcessList.makeProcStateString(app.setProcState) + " to " + ProcessList.makeProcStateString(app.curProcState) + " next pss in " + (app.nextPssTime - now) + ": " + app);
            }
        } else if (now > app.nextPssTime || (now > app.lastPssTime + BATTERY_STATS_TIME && now > app.lastStateTime + ProcessList.minTimeFromStateChange(this.mTestPssMode))) {
            requestPssLocked(app, app.setProcState);
            app.nextPssTime = ProcessList.computeNextPssTime(app.curProcState, false, this.mTestPssMode, isSleepingLocked(), now);
        }
        if (app.setProcState != app.curProcState) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.v(TAG_OOM_ADJ, "Proc state change of " + app.processName + " to " + app.curProcState);
            }
            boolean setImportant = app.setProcState < 10;
            boolean curImportant = app.curProcState < 10;
            if (setImportant && !curImportant) {
                BatteryStatsImpl stats = this.mBatteryStatsService.getActiveStatistics();
                synchronized (stats) {
                    app.lastWakeTime = stats.getProcessWakeTime(app.info.uid, app.pid, nowElapsed);
                }
                app.lastCpuTime = app.curCpuTime;
            }
            maybeUpdateUsageStatsLocked(app, nowElapsed);
            app.setProcState = app.curProcState;
            if (app.setProcState >= 12) {
                app.notCachedSinceIdle = false;
            }
            if (doingAll) {
                app.procStateChanged = true;
            } else {
                setProcessTrackerStateLocked(app, this.mProcessStats.getMemFactorLocked(), now);
            }
        } else if (app.reportedInteraction && nowElapsed - app.interactionEventTime > 86400000) {
            maybeUpdateUsageStatsLocked(app, nowElapsed);
        }
        if (changes != 0) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                Slog.i(TAG_PROCESS_OBSERVERS, "Changes in " + app + ": " + changes);
            }
            int i = this.mPendingProcessChanges.size() - 1;
            ProcessChangeItem item = null;
            while (true) {
                if (i >= 0) {
                    item = this.mPendingProcessChanges.get(i);
                    if (item.pid != app.pid) {
                        i--;
                    } else if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "Re-using existing item: " + item);
                    }
                }
            }
            if (i < 0) {
                int NA = this.mAvailProcessChanges.size();
                if (NA > 0) {
                    item = this.mAvailProcessChanges.remove(NA - 1);
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "Retrieving available item: " + item);
                    }
                } else {
                    item = new ProcessChangeItem();
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "Allocating new item: " + item);
                    }
                }
                item.changes = 0;
                item.pid = app.pid;
                item.uid = app.info.uid;
                if (this.mPendingProcessChanges.size() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "*** Enqueueing dispatch processes changed!");
                    }
                    this.mUiHandler.obtainMessage(31).sendToTarget();
                }
                this.mPendingProcessChanges.add(item);
            }
            item.changes |= changes;
            item.processState = app.repProcState;
            item.foregroundActivities = app.repForegroundActivities;
            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                Slog.i(TAG_PROCESS_OBSERVERS, "Item " + Integer.toHexString(System.identityHashCode(item)) + " " + app.toShortString() + ": changes=" + item.changes + " procState=" + item.processState + " foreground=" + item.foregroundActivities + " type=" + app.adjType + " source=" + app.adjSource + " target=" + app.adjTarget);
            }
        }
        return success;
    }

    private final void enqueueUidChangeLocked(UidRecord uidRec, int uid, int change) {
        UidRecord.ChangeItem pendingChange;
        if (uidRec == null || uidRec.pendingChange == null) {
            if (this.mPendingUidChanges.size() == 0) {
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(TAG_UID_OBSERVERS, "*** Enqueueing dispatch uid changed!");
                }
                this.mUiHandler.obtainMessage(54).sendToTarget();
            }
            int NA = this.mAvailUidChanges.size();
            if (NA > 0) {
                pendingChange = this.mAvailUidChanges.remove(NA - 1);
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(TAG_UID_OBSERVERS, "Retrieving available item: " + pendingChange);
                }
            } else {
                pendingChange = new UidRecord.ChangeItem();
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(TAG_UID_OBSERVERS, "Allocating new item: " + pendingChange);
                }
            }
            if (uidRec != null) {
                uidRec.pendingChange = pendingChange;
                if (change == 1 && !uidRec.idle) {
                    change = 2;
                }
            } else if (uid < 0) {
                throw new IllegalArgumentException("No UidRecord or uid");
            }
            pendingChange.uidRecord = uidRec;
            if (uidRec != null) {
                uid = uidRec.uid;
            }
            pendingChange.uid = uid;
            this.mPendingUidChanges.add(pendingChange);
        } else {
            pendingChange = uidRec.pendingChange;
            if (change == 1 && pendingChange.change == 3) {
                change = 2;
            }
        }
        pendingChange.change = change;
        pendingChange.processState = uidRec != null ? uidRec.setProcState : -1;
    }

    private void maybeUpdateProviderUsageStatsLocked(ProcessRecord app, String providerPkgName, String authority) {
        UserState userState;
        if (app == null || app.curProcState > 6 || (userState = this.mUserController.getStartedUserStateLocked(app.userId)) == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        Long lastReported = userState.mProviderLastReportedFg.get(authority);
        if (lastReported != null && lastReported.longValue() >= now - 60000) {
            return;
        }
        this.mUsageStatsService.reportContentProviderUsage(authority, providerPkgName, app.userId);
        userState.mProviderLastReportedFg.put(authority, Long.valueOf(now));
    }

    private void maybeUpdateUsageStatsLocked(ProcessRecord app, long nowElapsed) {
        boolean isInteraction;
        if (ActivityManagerDebugConfig.DEBUG_USAGE_STATS) {
            Slog.d(TAG, "Checking proc [" + Arrays.toString(app.getPackageList()) + "] state changes: old = " + app.setProcState + ", new = " + app.curProcState);
        }
        if (this.mUsageStatsService == null) {
            return;
        }
        if (app.curProcState <= 3) {
            isInteraction = true;
            app.fgInteractionTime = 0L;
        } else if (app.curProcState > 5) {
            isInteraction = app.curProcState <= 6;
            app.fgInteractionTime = 0L;
        } else if (app.fgInteractionTime == 0) {
            app.fgInteractionTime = nowElapsed;
            isInteraction = false;
        } else {
            isInteraction = nowElapsed > app.fgInteractionTime + BATTERY_STATS_TIME;
        }
        if (isInteraction && (!app.reportedInteraction || nowElapsed - app.interactionEventTime > 86400000)) {
            app.interactionEventTime = nowElapsed;
            String[] packages = app.getPackageList();
            if (packages != null) {
                for (String str : packages) {
                    this.mUsageStatsService.reportEvent(str, app.userId, 6);
                }
            }
        }
        app.reportedInteraction = isInteraction;
        if (isInteraction) {
            return;
        }
        app.interactionEventTime = 0L;
    }

    private final void setProcessTrackerStateLocked(ProcessRecord proc, int memFactor, long now) {
        if (proc.thread == null || proc.baseProcessTracker == null) {
            return;
        }
        proc.baseProcessTracker.setState(proc.repProcState, memFactor, now, proc.pkgList);
    }

    private final boolean updateOomAdjLocked(ProcessRecord app, int cachedAdj, ProcessRecord TOP_APP, boolean doingAll, long now) {
        if (app.thread == null) {
            return false;
        }
        computeOomAdjLocked(app, cachedAdj, TOP_APP, doingAll, now);
        return applyOomAdjLocked(app, doingAll, now, SystemClock.elapsedRealtime());
    }

    final void updateProcessForegroundLocked(ProcessRecord proc, boolean isForeground, boolean oomAdj) {
        if (isForeground == proc.foregroundServices) {
            return;
        }
        proc.foregroundServices = isForeground;
        ArrayList<ProcessRecord> curProcs = (ArrayList) this.mForegroundPackages.get(proc.info.packageName, proc.info.uid);
        if (isForeground) {
            if (curProcs == null) {
                curProcs = new ArrayList<>();
                this.mForegroundPackages.put(proc.info.packageName, proc.info.uid, curProcs);
            }
            if (!curProcs.contains(proc)) {
                curProcs.add(proc);
                this.mBatteryStatsService.noteEvent(32770, proc.info.packageName, proc.info.uid);
            }
        } else if (curProcs != null && curProcs.remove(proc)) {
            this.mBatteryStatsService.noteEvent(16386, proc.info.packageName, proc.info.uid);
            if (curProcs.size() <= 0) {
                this.mForegroundPackages.remove(proc.info.packageName, proc.info.uid);
            }
        }
        if (!oomAdj) {
            return;
        }
        updateOomAdjLocked();
    }

    private final ActivityRecord resumedAppLocked() {
        String pkg;
        int uid;
        ActivityRecord act = this.mStackSupervisor.resumedAppLocked();
        if (act != null) {
            pkg = act.packageName;
            uid = act.info.applicationInfo.uid;
        } else {
            pkg = null;
            uid = -1;
        }
        if (uid != this.mCurResumedUid || (pkg != this.mCurResumedPackage && (pkg == null || !pkg.equals(this.mCurResumedPackage)))) {
            if (this.mCurResumedPackage != null) {
                this.mBatteryStatsService.noteEvent(16387, this.mCurResumedPackage, this.mCurResumedUid);
            }
            this.mCurResumedPackage = pkg;
            this.mCurResumedUid = uid;
            if (this.mCurResumedPackage != null) {
                this.mBatteryStatsService.noteEvent(32771, this.mCurResumedPackage, this.mCurResumedUid);
            }
        }
        return act;
    }

    final boolean updateOomAdjLocked(ProcessRecord app) {
        ActivityRecord TOP_ACT = resumedAppLocked();
        ProcessRecord processRecord = TOP_ACT != null ? TOP_ACT.app : null;
        boolean wasCached = app.cached;
        this.mAdjSeq++;
        int cachedAdj = app.curRawAdj >= 900 ? app.curRawAdj : ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG;
        boolean success = updateOomAdjLocked(app, cachedAdj, processRecord, false, SystemClock.uptimeMillis());
        if (wasCached != app.cached || app.curRawAdj == 1001) {
            updateOomAdjLocked();
        }
        return success;
    }

    final void updateOomAdjLocked() {
        int emptyProcessLimit;
        int cachedProcessLimit;
        int fgTrimLevel;
        ActivityRecord TOP_ACT = resumedAppLocked();
        ProcessRecord processRecord = TOP_ACT != null ? TOP_ACT.app : null;
        long now = SystemClock.uptimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();
        long oldTime = now - BATTERY_STATS_TIME;
        int N = this.mLruProcesses.size();
        for (int i = this.mActiveUids.size() - 1; i >= 0; i--) {
            this.mActiveUids.valueAt(i).reset();
        }
        this.mStackSupervisor.rankTaskLayersIfNeeded();
        this.mAdjSeq++;
        this.mNewNumServiceProcs = 0;
        this.mNewNumAServiceProcs = 0;
        if (this.mProcessLimit <= 0) {
            cachedProcessLimit = 0;
            emptyProcessLimit = 0;
        } else if (this.mProcessLimit == 1) {
            emptyProcessLimit = 1;
            cachedProcessLimit = 0;
        } else {
            emptyProcessLimit = ProcessList.computeEmptyProcessLimit(this.mProcessLimit);
            cachedProcessLimit = this.mProcessLimit - emptyProcessLimit;
        }
        int numEmptyProcs = (N - this.mNumNonCachedProcs) - this.mNumCachedHiddenProcs;
        if (numEmptyProcs > cachedProcessLimit) {
            numEmptyProcs = cachedProcessLimit;
        }
        int emptyFactor = numEmptyProcs / 3;
        if (emptyFactor < 1) {
            emptyFactor = 1;
        }
        int cachedFactor = (this.mNumCachedHiddenProcs > 0 ? this.mNumCachedHiddenProcs : 1) / 3;
        if (cachedFactor < 1) {
            cachedFactor = 1;
        }
        int stepCached = 0;
        int stepEmpty = 0;
        int numCached = 0;
        int numEmpty = 0;
        int numTrimming = 0;
        this.mNumNonCachedProcs = 0;
        this.mNumCachedHiddenProcs = 0;
        int curCachedAdj = 900;
        int nextCachedAdj = 901;
        int curEmptyAdj = 900;
        int nextEmptyAdj = 902;
        for (int i2 = N - 1; i2 >= 0; i2--) {
            ProcessRecord app = this.mLruProcesses.get(i2);
            if (!app.killedByAm && app.thread != null) {
                app.procStateChanged = false;
                computeOomAdjLocked(app, ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG, processRecord, true, now);
                if (app.curAdj >= 1001) {
                    switch (app.curProcState) {
                        case 14:
                        case 15:
                            app.curRawAdj = curCachedAdj;
                            app.curAdj = app.modifyRawOomAdj(curCachedAdj);
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                            }
                            if (curCachedAdj != nextCachedAdj) {
                                stepCached++;
                                if (stepCached >= cachedFactor) {
                                    stepCached = 0;
                                    curCachedAdj = nextCachedAdj;
                                    nextCachedAdj += 2;
                                    if (nextCachedAdj > 906) {
                                        nextCachedAdj = 906;
                                    }
                                }
                            }
                            break;
                        default:
                            app.curRawAdj = curEmptyAdj;
                            app.curAdj = app.modifyRawOomAdj(curEmptyAdj);
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                            }
                            if (curEmptyAdj != nextEmptyAdj) {
                                stepEmpty++;
                                if (stepEmpty >= emptyFactor) {
                                    stepEmpty = 0;
                                    curEmptyAdj = nextEmptyAdj;
                                    nextEmptyAdj += 2;
                                    if (nextEmptyAdj > 906) {
                                        nextEmptyAdj = 906;
                                    }
                                }
                            }
                            break;
                    }
                }
                applyOomAdjLocked(app, true, now, nowElapsed);
                switch (app.curProcState) {
                    case 14:
                    case 15:
                        this.mNumCachedHiddenProcs++;
                        numCached++;
                        if (numCached > cachedProcessLimit) {
                            app.kill("cached #" + numCached, true);
                        }
                        break;
                    case 16:
                        if (numEmpty <= ProcessList.TRIM_EMPTY_APPS || app.lastActivityTime >= oldTime) {
                            numEmpty++;
                            if (numEmpty > emptyProcessLimit) {
                                app.kill("empty #" + numEmpty, true);
                            }
                        } else {
                            app.kill("empty for " + (((BATTERY_STATS_TIME + oldTime) - app.lastActivityTime) / 1000) + "s", true);
                        }
                        break;
                    default:
                        this.mNumNonCachedProcs++;
                        break;
                }
                if (!app.isolated || app.services.size() > 0) {
                    UidRecord uidRec = app.uidRecord;
                    if (uidRec != null && uidRec.curProcState > app.curProcState) {
                        uidRec.curProcState = app.curProcState;
                    }
                } else {
                    app.kill("isolated not needed", true);
                }
                if (app.curProcState >= 12 && !app.killedByAm) {
                    numTrimming++;
                }
            }
        }
        if (SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1")) {
            int numTotalCached = 0;
            for (int i3 = N - 1; i3 >= 0; i3--) {
                if (this.mLruProcesses.get(i3).curAdj >= 900) {
                    numTotalCached++;
                }
            }
            if (numTotalCached <= 3 && this.mLruProcesses.size() < this.mLastNumProcesses && this.mAllowLowerMemLevel) {
                MemInfoReader memInfo = new MemInfoReader();
                memInfo.readMemInfo();
                long[] memInfos = memInfo.getRawInfo();
                if (memInfos[3] <= this.mProcessList.getMemLevel(900) / 1024) {
                    int expiredApp = 0;
                    int expiredService = 0;
                    long lastActivity = 0;
                    boolean lastActivityFound = false;
                    if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                        Slog.d(TAG, "Memory Critical! Cached Size = " + memInfos[3] + " <= minfree [adj=9] = " + this.mProcessList.getMemLevel(900));
                    }
                    for (int i4 = N - 1; i4 >= 0; i4--) {
                        ProcessRecord app2 = this.mLruProcesses.get(i4);
                        if (app2.serviceb && !isSystemOrProtectedPackageName(app2.processName)) {
                            for (int is = app2.services.size() - 1; is >= 0; is--) {
                                ServiceRecord s = app2.services.valueAt(is);
                                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                    Slog.d(TAG, "Try to find expiredApp(" + i4 + ")=" + this.mLruProcesses.get(i4) + ",expiredService(" + is + ")=" + this.mLruProcesses.get(i4).services.valueAt(is) + ",lastActivity=" + (((now - s.lastActivity) / 1000.0d) / 60.0d) + " minute(s) ago. App lastPss size = " + this.mLruProcesses.get(i4).lastPss);
                                }
                                if (now - s.lastActivity < BATTERY_STATS_TIME && now - s.lastActivity > lastActivity) {
                                    expiredApp = i4;
                                    expiredService = is;
                                    lastActivity = now - s.lastActivity;
                                    lastActivityFound = true;
                                }
                            }
                        }
                    }
                    if (lastActivityFound) {
                        Slog.d(TAG, "Set the expiredService! app = " + this.mLruProcesses.get(expiredApp) + ",service = " + this.mLruProcesses.get(expiredApp).services.valueAt(expiredService) + ",lastActivity=" + ((lastActivity / 1000.0d) / 60.0d) + " minute(s) ago.");
                        this.mLruProcesses.get(expiredApp).services.valueAt(expiredService).lastActivity = now - BATTERY_STATS_TIME;
                    }
                }
            }
        }
        this.mNumServiceProcs = this.mNewNumServiceProcs;
        int numCachedAndEmpty = numCached + numEmpty;
        int memFactor = (numCached > ProcessList.TRIM_CACHED_APPS || numEmpty > ProcessList.TRIM_EMPTY_APPS) ? 0 : numCachedAndEmpty <= 3 ? 3 : numCachedAndEmpty <= 5 ? 2 : 1;
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            Slog.d(TAG_OOM_ADJ, "oom: memFactor=" + memFactor + " last=" + this.mLastMemoryLevel + " allowLow=" + this.mAllowLowerMemLevel + " numProcs=" + this.mLruProcesses.size() + " last=" + this.mLastNumProcesses);
        }
        if (memFactor > this.mLastMemoryLevel && (!this.mAllowLowerMemLevel || this.mLruProcesses.size() >= this.mLastNumProcesses)) {
            memFactor = this.mLastMemoryLevel;
            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.d(TAG_OOM_ADJ, "Keeping last mem factor!");
            }
        }
        if (memFactor != this.mLastMemoryLevel) {
            EventLogTags.writeAmMemFactor(memFactor, this.mLastMemoryLevel);
        }
        this.mLastMemoryLevel = memFactor;
        this.mLastNumProcesses = this.mLruProcesses.size();
        boolean allChanged = this.mProcessStats.setMemFactorLocked(memFactor, !isSleepingLocked(), now);
        int trackerMemFactor = this.mProcessStats.getMemFactorLocked();
        if (memFactor != 0) {
            if (this.mLowRamStartTime == 0) {
                this.mLowRamStartTime = now;
            }
            int step = 0;
            switch (memFactor) {
                case 2:
                    fgTrimLevel = 10;
                    break;
                case 3:
                    fgTrimLevel = 15;
                    break;
                default:
                    fgTrimLevel = 5;
                    break;
            }
            int factor = numTrimming / 3;
            int minFactor = this.mHomeProcess != null ? 3 : 2;
            if (this.mPreviousProcess != null) {
                minFactor++;
            }
            if (factor < minFactor) {
                factor = minFactor;
            }
            int curLevel = 80;
            for (int i5 = N - 1; i5 >= 0; i5--) {
                ProcessRecord app3 = this.mLruProcesses.get(i5);
                if (allChanged || app3.procStateChanged) {
                    setProcessTrackerStateLocked(app3, trackerMemFactor, now);
                    app3.procStateChanged = false;
                }
                if (app3.curProcState >= 12 && !app3.killedByAm) {
                    if (app3.trimMemoryLevel < curLevel && app3.thread != null) {
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                Slog.v(TAG_OOM_ADJ, "Trimming memory of " + app3.processName + " to " + curLevel);
                            }
                            app3.thread.scheduleTrimMemory(curLevel);
                        } catch (RemoteException e) {
                        }
                    }
                    app3.trimMemoryLevel = curLevel;
                    step++;
                    if (step >= factor) {
                        step = 0;
                        switch (curLevel) {
                            case 60:
                                curLevel = 40;
                                break;
                            case 80:
                                curLevel = 60;
                                break;
                        }
                    }
                } else if (app3.curProcState == 9) {
                    if (app3.trimMemoryLevel < 40 && app3.thread != null) {
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                Slog.v(TAG_OOM_ADJ, "Trimming memory of heavy-weight " + app3.processName + " to 40");
                            }
                            app3.thread.scheduleTrimMemory(40);
                        } catch (RemoteException e2) {
                        }
                    }
                    app3.trimMemoryLevel = 40;
                } else {
                    if ((app3.curProcState >= 7 || app3.systemNoUi) && app3.pendingUiClean) {
                        if (app3.trimMemoryLevel < 20 && app3.thread != null) {
                            try {
                                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                    Slog.v(TAG_OOM_ADJ, "Trimming memory of bg-ui " + app3.processName + " to 20");
                                }
                                app3.thread.scheduleTrimMemory(20);
                            } catch (RemoteException e3) {
                            }
                        }
                        app3.pendingUiClean = false;
                    }
                    if (app3.trimMemoryLevel < fgTrimLevel && app3.thread != null) {
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                Slog.v(TAG_OOM_ADJ, "Trimming memory of fg " + app3.processName + " to " + fgTrimLevel);
                            }
                            app3.thread.scheduleTrimMemory(fgTrimLevel);
                        } catch (RemoteException e4) {
                        }
                    }
                    app3.trimMemoryLevel = fgTrimLevel;
                }
            }
        } else {
            if (this.mLowRamStartTime != 0) {
                this.mLowRamTimeSinceLastIdle += now - this.mLowRamStartTime;
                this.mLowRamStartTime = 0L;
            }
            for (int i6 = N - 1; i6 >= 0; i6--) {
                ProcessRecord app4 = this.mLruProcesses.get(i6);
                if (allChanged || app4.procStateChanged) {
                    setProcessTrackerStateLocked(app4, trackerMemFactor, now);
                    app4.procStateChanged = false;
                }
                if ((app4.curProcState >= 7 || app4.systemNoUi) && app4.pendingUiClean) {
                    if (app4.trimMemoryLevel < 20 && app4.thread != null) {
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                Slog.v(TAG_OOM_ADJ, "Trimming memory of ui hidden " + app4.processName + " to 20");
                            }
                            app4.thread.scheduleTrimMemory(20);
                        } catch (RemoteException e5) {
                        }
                    }
                    app4.pendingUiClean = false;
                }
                app4.trimMemoryLevel = 0;
            }
        }
        if (this.mAlwaysFinishActivities) {
            this.mStackSupervisor.scheduleDestroyAllActivities(null, "always-finish");
        }
        if (allChanged) {
            requestPssAllProcsLocked(now, false, this.mProcessStats.isMemFactorLowered());
        }
        for (int i7 = this.mActiveUids.size() - 1; i7 >= 0; i7--) {
            UidRecord uidRec2 = this.mActiveUids.valueAt(i7);
            int uidChange = 0;
            if (uidRec2.setProcState != uidRec2.curProcState) {
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(TAG_UID_OBSERVERS, "Changes in " + uidRec2 + ": proc state from " + uidRec2.setProcState + " to " + uidRec2.curProcState);
                }
                if (!ActivityManager.isProcStateBackground(uidRec2.curProcState)) {
                    if (uidRec2.idle) {
                        uidChange = 4;
                        uidRec2.idle = false;
                    }
                    uidRec2.lastBackgroundTime = 0L;
                } else if (!ActivityManager.isProcStateBackground(uidRec2.setProcState)) {
                    uidRec2.lastBackgroundTime = nowElapsed;
                    if (!this.mHandler.hasMessages(60)) {
                        this.mHandler.sendEmptyMessageDelayed(60, 60000L);
                    }
                }
                uidRec2.setProcState = uidRec2.curProcState;
                enqueueUidChangeLocked(uidRec2, -1, uidChange);
                noteUidProcessState(uidRec2.uid, uidRec2.curProcState);
            }
        }
        if (this.mProcessStats.shouldWriteNowLocked(now)) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (ActivityManagerService.this) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityManagerService.this.mProcessStats.writeStateAsyncLocked();
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            });
        }
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            long duration = SystemClock.uptimeMillis() - now;
            Slog.d(TAG_OOM_ADJ, "Did OOM ADJ in " + duration + "ms");
        }
    }

    final void idleUids() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long nowElapsed = SystemClock.elapsedRealtime();
                long maxBgTime = nowElapsed - 60000;
                long nextTime = 0;
                for (int i = this.mActiveUids.size() - 1; i >= 0; i--) {
                    UidRecord uidRec = this.mActiveUids.valueAt(i);
                    long bgTime = uidRec.lastBackgroundTime;
                    if (bgTime > 0 && !uidRec.idle) {
                        if (bgTime <= maxBgTime) {
                            uidRec.idle = true;
                            doStopUidLocked(uidRec.uid, uidRec);
                        } else if (nextTime == 0 || nextTime > bgTime) {
                            nextTime = bgTime;
                        }
                    }
                }
                if (nextTime > 0) {
                    this.mHandler.removeMessages(60);
                    this.mHandler.sendEmptyMessageDelayed(60, (60000 + nextTime) - nowElapsed);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    final void runInBackgroundDisabled(int uid) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                UidRecord uidRec = this.mActiveUids.get(uid);
                if (uidRec != null) {
                    if (uidRec.idle) {
                        doStopUidLocked(uidRec.uid, uidRec);
                    }
                } else {
                    doStopUidLocked(uid, null);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    final void doStopUidLocked(int uid, UidRecord uidRec) {
        this.mServices.stopInBackgroundLocked(uid);
        enqueueUidChangeLocked(uidRec, uid, 3);
    }

    final void trimApplications() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                for (int i = this.mRemovedProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord app = this.mRemovedProcesses.get(i);
                    if (app.activities.size() == 0 && app.curReceiver == null && app.services.size() == 0) {
                        Slog.i(TAG, "Exiting empty application process " + app.toShortString() + " (" + (app.thread != null ? app.thread.asBinder() : null) + ")\n");
                        if (app.pid <= 0 || app.pid == MY_PID) {
                            try {
                                app.thread.scheduleExit();
                            } catch (Exception e) {
                            }
                        } else {
                            app.kill("empty", false);
                        }
                        cleanUpApplicationRecordLocked(app, false, true, -1, false);
                        this.mRemovedProcesses.remove(i);
                        if (app.persistent) {
                            addAppLocked(app.info, false, null);
                        }
                    }
                }
                updateOomAdjLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void signalPersistentProcesses(int sig) throws RemoteException {
        if (sig != 10) {
            throw new SecurityException("Only SIGNAL_USR1 is allowed");
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (checkCallingPermission("android.permission.SIGNAL_PERSISTENT_PROCESSES") != 0) {
                    throw new SecurityException("Requires permission android.permission.SIGNAL_PERSISTENT_PROCESSES");
                }
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord r = this.mLruProcesses.get(i);
                    if (r.thread != null && r.persistent) {
                        Process.sendSignal(r.pid, sig);
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void stopProfilerLocked(ProcessRecord proc, int profileType) {
        if (proc == null || proc == this.mProfileProc) {
            proc = this.mProfileProc;
            profileType = this.mProfileType;
            clearProfilerLocked();
        }
        if (proc == null) {
            return;
        }
        try {
            proc.thread.profilerControl(false, (ProfilerInfo) null, profileType);
        } catch (RemoteException e) {
            throw new IllegalStateException("Process disappeared");
        }
    }

    private void clearProfilerLocked() {
        if (this.mProfileFd != null) {
            try {
                this.mProfileFd.close();
            } catch (IOException e) {
            }
        }
        this.mProfileApp = null;
        this.mProfileProc = null;
        this.mProfileFile = null;
        this.mProfileType = 0;
        this.mAutoStopProfiler = false;
        this.mSamplingInterval = 0;
    }

    public boolean profileControl(String process, int userId, boolean start, ProfilerInfo profilerInfo, int profileType) throws RemoteException {
        ParcelFileDescriptor fd;
        try {
            try {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
                            throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
                        }
                        if (start && (profilerInfo == null || profilerInfo.profileFd == null)) {
                            throw new IllegalArgumentException("null profile info or fd");
                        }
                        ProcessRecord proc = null;
                        if (process != null) {
                            proc = findProcessLocked(process, userId, "profileControl");
                        }
                        if (start && (proc == null || proc.thread == null)) {
                            throw new IllegalArgumentException("Unknown process: " + process);
                        }
                        if (start) {
                            stopProfilerLocked(null, 0);
                            setProfileApp(proc.info, proc.processName, profilerInfo);
                            this.mProfileProc = proc;
                            this.mProfileType = profileType;
                            ParcelFileDescriptor fd2 = profilerInfo.profileFd;
                            try {
                                fd = fd2.dup();
                            } catch (IOException e) {
                                fd = null;
                            }
                            profilerInfo.profileFd = fd;
                            proc.thread.profilerControl(start, profilerInfo, profileType);
                            this.mProfileFd = null;
                        } else {
                            stopProfilerLocked(proc, profileType);
                            if (profilerInfo != null && profilerInfo.profileFd != null) {
                                try {
                                    profilerInfo.profileFd.close();
                                } catch (IOException e2) {
                                }
                            }
                        }
                    } finally {
                        resetPriorityAfterLockedSection();
                    }
                }
                return true;
            } catch (RemoteException e3) {
                throw new IllegalStateException("Process disappeared");
            }
        } finally {
            if (profilerInfo != null && profilerInfo.profileFd != null) {
                try {
                    profilerInfo.profileFd.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    private ProcessRecord findProcessLocked(String process, int userId, String callName) {
        int userId2 = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, 2, callName, null);
        ProcessRecord proc = null;
        try {
            int pid = Integer.parseInt(process);
            synchronized (this.mPidsSelfLocked) {
                proc = this.mPidsSelfLocked.get(pid);
            }
        } catch (NumberFormatException e) {
        }
        if (proc == null) {
            ArrayMap<String, SparseArray<ProcessRecord>> all = this.mProcessNames.getMap();
            SparseArray<ProcessRecord> procs = all.get(process);
            if (procs != null && procs.size() > 0) {
                ProcessRecord proc2 = procs.valueAt(0);
                if (userId2 != -1 && proc2.userId != userId2) {
                    for (int i = 1; i < procs.size(); i++) {
                        ProcessRecord thisProc = procs.valueAt(i);
                        if (thisProc.userId == userId2) {
                            return thisProc;
                        }
                    }
                    return proc2;
                }
                return proc2;
            }
            return proc;
        }
        return proc;
    }

    public boolean dumpHeap(String process, int userId, boolean managed, String path, ParcelFileDescriptor fd) throws RemoteException {
        try {
            try {
                synchronized (this) {
                    try {
                        boostPriorityForLockedSection();
                        if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
                            throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
                        }
                        if (fd == null) {
                            throw new IllegalArgumentException("null fd");
                        }
                        ProcessRecord proc = findProcessLocked(process, userId, "dumpHeap");
                        if (proc == null || proc.thread == null) {
                            throw new IllegalArgumentException("Unknown process: " + process);
                        }
                        boolean isDebuggable = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
                        if (!isDebuggable && (proc.info.flags & 2) == 0) {
                            throw new SecurityException("Process not debuggable: " + proc);
                        }
                        proc.thread.dumpHeap(managed, path, fd);
                    } finally {
                        resetPriorityAfterLockedSection();
                    }
                }
                return true;
            } catch (RemoteException e) {
                throw new IllegalStateException("Process disappeared");
            }
        } catch (Throwable th) {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e2) {
                }
            }
            throw th;
        }
    }

    public void setDumpHeapDebugLimit(String processName, int uid, long maxMemSize, String reportPackage) {
        if (processName != null) {
            enforceCallingPermission("android.permission.SET_DEBUG_APP", "setDumpHeapDebugLimit()");
        } else {
            synchronized (this.mPidsSelfLocked) {
                ProcessRecord proc = this.mPidsSelfLocked.get(Binder.getCallingPid());
                if (proc == null) {
                    throw new SecurityException("No process found for calling pid " + Binder.getCallingPid());
                }
                if (!Build.IS_DEBUGGABLE && (proc.info.flags & 2) == 0) {
                    throw new SecurityException("Not running a debuggable build");
                }
                processName = proc.processName;
                uid = proc.uid;
                if (reportPackage != null && !proc.pkgList.containsKey(reportPackage)) {
                    throw new SecurityException("Package " + reportPackage + " is not running in " + proc);
                }
            }
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (maxMemSize > 0) {
                    this.mMemWatchProcesses.put(processName, uid, new Pair(Long.valueOf(maxMemSize), reportPackage));
                } else if (uid != 0) {
                    this.mMemWatchProcesses.remove(processName, uid);
                } else {
                    this.mMemWatchProcesses.getMap().remove(processName);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void dumpHeapFinished(String path) {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                if (Binder.getCallingPid() != this.mMemWatchDumpPid) {
                    Slog.w(TAG, "dumpHeapFinished: Calling pid " + Binder.getCallingPid() + " does not match last pid " + this.mMemWatchDumpPid);
                    return;
                }
                if (this.mMemWatchDumpFile == null || !this.mMemWatchDumpFile.equals(path)) {
                    Slog.w(TAG, "dumpHeapFinished: Calling path " + path + " does not match last path " + this.mMemWatchDumpFile);
                    return;
                }
                if (ActivityManagerDebugConfig.DEBUG_PSS) {
                    Slog.d(TAG_PSS, "Dump heap finished for " + path);
                }
                this.mHandler.sendEmptyMessage(51);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    @Override
    public void monitor() {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void onCoreSettingsChange(Bundle settings) {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord processRecord = this.mLruProcesses.get(i);
            try {
                if (processRecord.thread != null) {
                    processRecord.thread.setCoreSettings(settings);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public boolean startUserInBackground(int userId) {
        return this.mUserController.startUser(userId, false);
    }

    public boolean unlockUser(int userId, byte[] token, byte[] secret, IProgressListener listener) {
        return this.mUserController.unlockUser(userId, token, secret, listener);
    }

    public boolean switchUser(int targetUserId) {
        enforceShellRestriction("no_debugging_features", targetUserId);
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                int currentUserId = this.mUserController.getCurrentUserIdLocked();
                UserInfo currentUserInfo = this.mUserController.getUserInfo(currentUserId);
                UserInfo targetUserInfo = this.mUserController.getUserInfo(targetUserId);
                if (targetUserInfo == null) {
                    Slog.w(TAG, "No user info for user #" + targetUserId);
                    return false;
                }
                if (!targetUserInfo.supportsSwitchTo()) {
                    Slog.w(TAG, "Cannot switch to User #" + targetUserId + ": not supported");
                    return false;
                }
                if (targetUserInfo.isManagedProfile()) {
                    Slog.w(TAG, "Cannot switch to User #" + targetUserId + ": not a full user");
                    return false;
                }
                this.mUserController.setTargetUserIdLocked(targetUserId);
                resetPriorityAfterLockedSection();
                Pair<UserInfo, UserInfo> userNames = new Pair<>(currentUserInfo, targetUserInfo);
                this.mUiHandler.removeMessages(46);
                this.mUiHandler.sendMessage(this.mUiHandler.obtainMessage(46, userNames));
                return true;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    void scheduleStartProfilesLocked() {
        if (this.mHandler.hasMessages(40)) {
            return;
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(40), 1000L);
    }

    public int stopUser(int userId, boolean force, IStopUserCallback callback) {
        return this.mUserController.stopUser(userId, force, callback);
    }

    public UserInfo getCurrentUser() {
        return this.mUserController.getCurrentUser();
    }

    public boolean isUserRunning(int userId, int flags) {
        boolean zIsUserRunningLocked;
        if (userId != UserHandle.getCallingUserId() && checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0) {
            String msg = "Permission Denial: isUserRunning() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                zIsUserRunningLocked = this.mUserController.isUserRunningLocked(userId, flags);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zIsUserRunningLocked;
    }

    public int[] getRunningUserIds() {
        int[] startedUserArrayLocked;
        if (checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0) {
            String msg = "Permission Denial: isUserRunning() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                startedUserArrayLocked = this.mUserController.getStartedUserArrayLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return startedUserArrayLocked;
    }

    public void registerUserSwitchObserver(IUserSwitchObserver observer) {
        this.mUserController.registerUserSwitchObserver(observer);
    }

    public void unregisterUserSwitchObserver(IUserSwitchObserver observer) {
        this.mUserController.unregisterUserSwitchObserver(observer);
    }

    ApplicationInfo getAppInfoForUser(ApplicationInfo info, int userId) {
        if (info == null) {
            return null;
        }
        ApplicationInfo newInfo = new ApplicationInfo(info);
        newInfo.initForUser(userId);
        return newInfo;
    }

    public boolean isUserStopped(int userId) {
        boolean z;
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                z = this.mUserController.getStartedUserStateLocked(userId) == null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    ActivityInfo getActivityInfoForUser(ActivityInfo aInfo, int userId) {
        if (aInfo == null || (userId < 1 && aInfo.applicationInfo.uid < 100000)) {
            return aInfo;
        }
        ActivityInfo info = new ActivityInfo(aInfo);
        info.applicationInfo = getAppInfoForUser(info.applicationInfo, userId);
        return info;
    }

    private boolean processSanityChecksLocked(ProcessRecord process) {
        if (process == null || process.thread == null) {
            return false;
        }
        boolean isDebuggable = "1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
        return isDebuggable || (process.info.flags & 2) != 0;
    }

    public boolean startBinderTracking() throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                this.mBinderTransactionTrackingEnabled = true;
                if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
                    throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
                }
                for (int i = 0; i < this.mLruProcesses.size(); i++) {
                    ProcessRecord process = this.mLruProcesses.get(i);
                    if (processSanityChecksLocked(process)) {
                        try {
                            process.thread.startBinderTracking();
                        } catch (RemoteException e) {
                            Log.v(TAG, "Process disappared");
                        }
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return true;
    }

    public boolean stopBinderTrackingAndDump(ParcelFileDescriptor fd) throws RemoteException {
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    this.mBinderTransactionTrackingEnabled = false;
                    if (checkCallingPermission("android.permission.SET_ACTIVITY_WATCHER") != 0) {
                        throw new SecurityException("Requires permission android.permission.SET_ACTIVITY_WATCHER");
                    }
                    if (fd == null) {
                        throw new IllegalArgumentException("null fd");
                    }
                    FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fd.getFileDescriptor()));
                    fastPrintWriter.println("Binder transaction traces for all processes.\n");
                    for (ProcessRecord process : this.mLruProcesses) {
                        if (processSanityChecksLocked(process)) {
                            fastPrintWriter.println("Traces for process: " + process.processName);
                            fastPrintWriter.flush();
                            try {
                                TransferPipe tp = new TransferPipe();
                                try {
                                    process.thread.stopBinderTrackingAndDump(tp.getWriteFd().getFileDescriptor());
                                    tp.go(fd.getFileDescriptor());
                                    tp.kill();
                                } catch (Throwable th) {
                                    tp.kill();
                                    throw th;
                                }
                            } catch (RemoteException e) {
                                fastPrintWriter.println("Got a RemoteException while dumping IPC traces from " + process + ".  Exception: " + e);
                                fastPrintWriter.flush();
                            } catch (IOException e2) {
                                fastPrintWriter.println("Failure while dumping IPC traces from " + process + ".  Exception: " + e2);
                                fastPrintWriter.flush();
                            }
                        }
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return true;
        } catch (Throwable th2) {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e3) {
                }
            }
            throw th2;
        }
    }

    private final class LocalService extends ActivityManagerInternal {
        LocalService(ActivityManagerService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        public String checkContentProviderAccess(String authority, int userId) {
            return ActivityManagerService.this.checkContentProviderAccess(authority, userId);
        }

        public void onWakefulnessChanged(int wakefulness) {
            ActivityManagerService.this.onWakefulnessChanged(wakefulness);
        }

        public int startIsolatedProcess(String entryPoint, String[] entryPointArgs, String processName, String abiOverride, int uid, Runnable crashHandler) {
            return ActivityManagerService.this.startIsolatedProcess(entryPoint, entryPointArgs, processName, abiOverride, uid, crashHandler);
        }

        public ActivityManagerInternal.SleepToken acquireSleepToken(String tag) {
            SleepTokenImpl token;
            Preconditions.checkNotNull(tag);
            ComponentName requestedVrService = null;
            ComponentName callingVrActivity = null;
            int userId = -1;
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (ActivityManagerService.this.mFocusedActivity != null) {
                        requestedVrService = ActivityManagerService.this.mFocusedActivity.requestedVrComponent;
                        callingVrActivity = ActivityManagerService.this.mFocusedActivity.info.getComponentName();
                        userId = ActivityManagerService.this.mFocusedActivity.userId;
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            if (requestedVrService != null) {
                ActivityManagerService.this.applyVrMode(false, requestedVrService, userId, callingVrActivity, true);
            }
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    token = ActivityManagerService.this.new SleepTokenImpl(tag);
                    ActivityManagerService.this.mSleepTokens.add(token);
                    ActivityManagerService.this.updateSleepIfNeededLocked();
                } catch (Throwable th2) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return token;
        }

        public ComponentName getHomeActivityForUser(int userId) {
            ComponentName componentName;
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord homeActivity = ActivityManagerService.this.mStackSupervisor.getHomeActivityForUser(userId);
                    componentName = homeActivity != null ? homeActivity.realActivity : null;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return componentName;
        }

        public void onUserRemoved(int userId) {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.onUserStoppedLocked(userId);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void onLocalVoiceInteractionStarted(IBinder activity, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.onLocalVoiceInteractionStartedLocked(activity, voiceSession, voiceInteractor);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void notifyStartingWindowDrawn() {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.mStackSupervisor.mActivityMetricsLogger.notifyStartingWindowDrawn();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void notifyAppTransitionStarting(int reason) {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.mStackSupervisor.mActivityMetricsLogger.notifyTransitionStarting(reason);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void notifyAppTransitionFinished() {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.mStackSupervisor.notifyAppTransitionDone();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void notifyAppTransitionCancelled() {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.mStackSupervisor.notifyAppTransitionDone();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public List<IBinder> getTopVisibleActivities() {
            List<IBinder> topVisibleActivities;
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    topVisibleActivities = ActivityManagerService.this.mStackSupervisor.getTopVisibleActivities();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return topVisibleActivities;
        }

        public void notifyDockedStackMinimizedChanged(boolean minimized) {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityManagerService.this.mStackSupervisor.setDockedStackMinimized(minimized);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void killForegroundAppsForUser(int userHandle) {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ArrayList<ProcessRecord> procs = new ArrayList<>();
                    int NP = ActivityManagerService.this.mProcessNames.getMap().size();
                    for (int ip = 0; ip < NP; ip++) {
                        SparseArray<ProcessRecord> apps = (SparseArray) ActivityManagerService.this.mProcessNames.getMap().valueAt(ip);
                        int NA = apps.size();
                        for (int ia = 0; ia < NA; ia++) {
                            ProcessRecord app = apps.valueAt(ia);
                            if (!app.persistent) {
                                if (app.removed) {
                                    procs.add(app);
                                } else if (app.userId == userHandle && app.foregroundActivities) {
                                    app.removed = true;
                                    procs.add(app);
                                }
                            }
                        }
                    }
                    int N = procs.size();
                    for (int i = 0; i < N; i++) {
                        ActivityManagerService.this.removeProcessLocked(procs.get(i), false, true, "kill all fg");
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void setPendingIntentWhitelistDuration(IIntentSender target, long duration) {
            if (!(target instanceof PendingIntentRecord)) {
                Slog.w(ActivityManagerService.TAG, "markAsSentFromNotification(): not a PendingIntentRecord: " + target);
            } else {
                ((PendingIntentRecord) target).setWhitelistDuration(duration);
            }
        }
    }

    private final class SleepTokenImpl extends ActivityManagerInternal.SleepToken {
        private final long mAcquireTime = SystemClock.uptimeMillis();
        private final String mTag;

        public SleepTokenImpl(String tag) {
            this.mTag = tag;
        }

        public void release() {
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (ActivityManagerService.this.mSleepTokens.remove(this)) {
                        ActivityManagerService.this.updateSleepIfNeededLocked();
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public String toString() {
            return "{\"" + this.mTag + "\", acquire at " + TimeUtils.formatUptime(this.mAcquireTime) + "}";
        }
    }

    class AppTaskImpl extends IAppTask.Stub {
        private int mCallingUid;
        private int mTaskId;

        public AppTaskImpl(int taskId, int callingUid) {
            this.mTaskId = taskId;
            this.mCallingUid = callingUid;
        }

        private void checkCaller() {
            if (this.mCallingUid == Binder.getCallingUid()) {
            } else {
                throw new SecurityException("Caller " + this.mCallingUid + " does not match caller of getAppTasks(): " + Binder.getCallingUid());
            }
        }

        public void finishAndRemoveTask() {
            checkCaller();
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    long origId = Binder.clearCallingIdentity();
                    try {
                        if (!ActivityManagerService.this.removeTaskByIdLocked(this.mTaskId, false, true)) {
                            throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public ActivityManager.RecentTaskInfo getTaskInfo() {
            ActivityManager.RecentTaskInfo recentTaskInfoCreateRecentTaskInfoFromTaskRecord;
            checkCaller();
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    long origId = Binder.clearCallingIdentity();
                    try {
                        TaskRecord tr = ActivityManagerService.this.mStackSupervisor.anyTaskForIdLocked(this.mTaskId);
                        if (tr == null) {
                            throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                        }
                        recentTaskInfoCreateRecentTaskInfoFromTaskRecord = ActivityManagerService.this.createRecentTaskInfoFromTaskRecord(tr);
                    } finally {
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return recentTaskInfoCreateRecentTaskInfoFromTaskRecord;
        }

        public void moveToFront() {
            checkCaller();
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this) {
                    ActivityManagerService.this.mStackSupervisor.startActivityFromRecentsInner(this.mTaskId, null);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public int startActivity(IBinder whoThread, String callingPackage, Intent intent, String resolvedType, Bundle bOptions) {
            TaskRecord tr;
            IApplicationThread appThread;
            checkCaller();
            int callingUser = UserHandle.getCallingUserId();
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    tr = ActivityManagerService.this.mStackSupervisor.anyTaskForIdLocked(this.mTaskId);
                    if (tr == null) {
                        throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                    }
                    appThread = ApplicationThreadNative.asInterface(whoThread);
                    if (appThread == null) {
                        throw new IllegalArgumentException("Bad app thread " + appThread);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return ActivityManagerService.this.mActivityStarter.startActivityMayWait(appThread, -1, callingPackage, intent, resolvedType, null, null, null, null, 0, 0, null, null, null, bOptions, false, callingUser, null, tr);
        }

        public void setExcludeFromRecents(boolean exclude) {
            checkCaller();
            synchronized (ActivityManagerService.this) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    long origId = Binder.clearCallingIdentity();
                    try {
                        TaskRecord tr = ActivityManagerService.this.mStackSupervisor.anyTaskForIdLocked(this.mTaskId);
                        if (tr == null) {
                            throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                        }
                        Intent intent = tr.getBaseIntent();
                        if (exclude) {
                            intent.addFlags(8388608);
                        } else {
                            intent.setFlags(intent.getFlags() & (-8388609));
                        }
                    } finally {
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    public void killPackageDependents(String packageName, int userId) {
        enforceCallingPermission("android.permission.KILL_UID", "killPackageDependents()");
        if (packageName == null) {
            throw new NullPointerException("Cannot kill the dependents of a package without its name.");
        }
        long callingId = Binder.clearCallingIdentity();
        IPackageManager pm = AppGlobals.getPackageManager();
        int pkgUid = -1;
        try {
            pkgUid = pm.getPackageUid(packageName, 268435456, userId);
        } catch (RemoteException e) {
        }
        if (userId != -1 && pkgUid == -1) {
            throw new IllegalArgumentException("Cannot kill dependents of non-existing package " + packageName);
        }
        try {
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    killPackageProcessesLocked(packageName, UserHandle.getAppId(pkgUid), userId, 0, false, true, true, false, "dep: " + packageName);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void getRunningProcessPids(ArrayList<Integer> pids) {
        int size = this.mLruProcesses.size();
        for (int i = size - 1; i >= 0; i--) {
            try {
                pids.add(Integer.valueOf(this.mLruProcesses.get(i).pid));
            } catch (IndexOutOfBoundsException e) {
                Slog.w(TAG, "Failed to get mLruProcesses element i = " + i);
            }
        }
    }

    protected void configLogTag() {
        String activitylog = SystemProperties.get("persist.sys.activitylog", (String) null);
        if (activitylog == null || activitylog.equals("")) {
            return;
        }
        if (activitylog.indexOf(" ") + 1 <= activitylog.length() && activitylog.indexOf(" ") != -1) {
            String[] args = {activitylog.substring(0, activitylog.indexOf(" ")), activitylog.substring(activitylog.indexOf(" ") + 1, activitylog.length())};
            configLogTag(null, args, 0);
        } else {
            SystemProperties.set("persist.sys.activitylog", "");
        }
    }

    protected void configLogTag(PrintWriter pw, String[] args, int opti) {
        if (opti + 1 >= args.length) {
            if (pw != null) {
                pw.println("  Invalid argument!");
            }
            SystemProperties.set("persist.sys.activitylog", "");
            return;
        }
        String tag = args[opti];
        boolean on = "on".equals(args[opti + 1]);
        SystemProperties.set("persist.sys.activitylog", args[opti] + " " + args[opti + 1]);
        if (tag.equals("a")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_PROCESSES = on;
            ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = on;
            ActivityManagerDebugConfig.DEBUG_CLEANUP = on;
            ActivityManagerDebugConfig.DEBUG_MU = on;
            ActivityManagerDebugConfig.DEBUG_SWITCH = on;
            ActivityManagerDebugConfig.DEBUG_TASKS = on;
            ActivityManagerDebugConfig.DEBUG_PAUSE = on;
            ActivityManagerDebugConfig.DEBUG_TRANSITION = on;
            ActivityManagerDebugConfig.DEBUG_CONFIGURATION = on;
            ActivityManagerDebugConfig.DEBUG_STATES = on;
            ActivityManagerDebugConfig.DEBUG_ADD_REMOVE = on;
            ActivityManagerDebugConfig.DEBUG_SAVED_STATE = on;
            ActivityManagerDebugConfig.DEBUG_APP = on;
            ActivityManagerDebugConfig.DEBUG_IDLE = on;
            return;
        }
        if (tag.equals("da")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_OOM_ADJ = on;
            ActivityManagerDebugConfig.DEBUG_VISIBILITY = on;
            ActivityManagerDebugConfig.DEBUG_USER_LEAVING = on;
            ActivityManagerDebugConfig.DEBUG_RESULTS = on;
            return;
        }
        if (tag.equals("br")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_PROCESSES = on;
            ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = on;
            ActivityManagerDebugConfig.DEBUG_CLEANUP = on;
            ActivityManagerDebugConfig.DEBUG_MU = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = on;
            return;
        }
        if (tag.equals("s")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_PROCESSES = on;
            ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = on;
            ActivityManagerDebugConfig.DEBUG_CLEANUP = on;
            ActivityManagerDebugConfig.DEBUG_MU = on;
            ActivityManagerDebugConfig.DEBUG_SERVICE = on;
            ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING = on;
            return;
        }
        if (tag.equals("cp")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_PROCESSES = on;
            ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = on;
            ActivityManagerDebugConfig.DEBUG_CLEANUP = on;
            ActivityManagerDebugConfig.DEBUG_MU = on;
            ActivityManagerDebugConfig.DEBUG_PROVIDER = on;
            return;
        }
        if (tag.equals("p")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_URI_PERMISSION = on;
            return;
        }
        if (tag.equals("m")) {
            ActivityManagerDebugConfig.DEBUG_ALL = false;
            ActivityManagerDebugConfig.DEBUG_BACKUP = on;
            ActivityManagerDebugConfig.DEBUG_POWER = on;
            ActivityManagerDebugConfig.DEBUG_POWER_QUICK = on;
            return;
        }
        if (tag.equals("x")) {
            ActivityManagerDebugConfig.DEBUG_ALL = on;
            ActivityManagerDebugConfig.DEBUG_ALL_ACTIVITIES = on;
            ActivityManagerDebugConfig.DEBUG_ADD_REMOVE = on;
            ActivityManagerDebugConfig.DEBUG_ANR = on;
            ActivityManagerDebugConfig.DEBUG_APP = on;
            ActivityManagerDebugConfig.DEBUG_BACKUP = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = on;
            ActivityManagerDebugConfig.DEBUG_CLEANUP = on;
            ActivityManagerDebugConfig.DEBUG_CONFIGURATION = on;
            ActivityManagerDebugConfig.DEBUG_CONTAINERS = on;
            ActivityManagerDebugConfig.DEBUG_FOCUS = on;
            ActivityManagerDebugConfig.DEBUG_IDLE = on;
            ActivityManagerDebugConfig.DEBUG_IMMERSIVE = on;
            ActivityManagerDebugConfig.DEBUG_LOCKSCREEN = on;
            ActivityManagerDebugConfig.DEBUG_LOCKTASK = on;
            ActivityManagerDebugConfig.DEBUG_LRU = on;
            ActivityManagerDebugConfig.DEBUG_MU = on;
            ActivityManagerDebugConfig.DEBUG_OOM_ADJ = on;
            ActivityManagerDebugConfig.DEBUG_PAUSE = on;
            ActivityManagerDebugConfig.DEBUG_POWER = on;
            ActivityManagerDebugConfig.DEBUG_POWER_QUICK = on;
            ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = on;
            ActivityManagerDebugConfig.DEBUG_PROCESSES = on;
            ActivityManagerDebugConfig.DEBUG_PROVIDER = on;
            ActivityManagerDebugConfig.DEBUG_PSS = on;
            ActivityManagerDebugConfig.DEBUG_RECENTS = on;
            ActivityManagerDebugConfig.DEBUG_RELEASE = on;
            ActivityManagerDebugConfig.DEBUG_RESULTS = on;
            ActivityManagerDebugConfig.DEBUG_SAVED_STATE = on;
            ActivityManagerDebugConfig.DEBUG_SCREENSHOTS = on;
            ActivityManagerDebugConfig.DEBUG_SERVICE = on;
            ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING = on;
            ActivityManagerDebugConfig.DEBUG_STACK = on;
            ActivityManagerDebugConfig.DEBUG_STATES = on;
            ActivityManagerDebugConfig.DEBUG_SWITCH = on;
            ActivityManagerDebugConfig.DEBUG_TASKS = on;
            ActivityManagerDebugConfig.DEBUG_THUMBNAILS = on;
            ActivityManagerDebugConfig.DEBUG_TRANSITION = on;
            ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS = on;
            ActivityManagerDebugConfig.DEBUG_URI_PERMISSION = on;
            ActivityManagerDebugConfig.DEBUG_USER_LEAVING = on;
            ActivityManagerDebugConfig.DEBUG_VISIBILITY = on;
            ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND = on;
            ActivityManagerDebugConfig.DEBUG_USAGE_STATS = on;
            ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = on;
            ActivityManagerDebugConfig.DEBUG_WHITELISTS = on;
            ActivityManagerDebugConfig.DEBUG_PERMISSION = on;
            ActivityManagerDebugConfig.DEBUG_TASK_RETURNTO = on;
            ActivityManagerDebugConfig.DEBUG_MULTIWINDOW = on;
            AMEventHook.setDebug(on);
            configActivityLogTag(tag, on);
            return;
        }
        if (tag.equals("lp")) {
            String processName = args[opti + 1];
            Slog.i(TAG, "Enalbe Looper Log: " + processName);
            for (int i = 0; i < this.mLruProcesses.size(); i++) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app != null && app.processName.toLowerCase().indexOf(processName.toLowerCase()) >= 0) {
                    if (app.thread == null) {
                        return;
                    }
                    try {
                        app.thread.enableLooperLog();
                        return;
                    } catch (Exception e) {
                        Slog.e(TAG, "Error happens when enable looper log", e);
                        return;
                    }
                }
            }
            return;
        }
        if (tag.equals("anr")) {
            String tmpAnrOption = args[opti + 1];
            int anrOption = Integer.valueOf(tmpAnrOption).intValue();
            Settings.System.putInt(this.mContext.getContentResolver(), "anr_debugging_mechanism", anrOption);
            ANRManager.AnrOption = anrOption;
            return;
        }
        if (tag.equals("event")) {
            AMEventHook.setEventDetailDebug(on);
            return;
        }
        if (pw != null) {
            pw.println("  Invalid argument!");
        }
        SystemProperties.set("persist.sys.activitylog", "");
    }

    public AMEventHook getAMEventHook() {
        return this.mAMEventHook;
    }

    private static String getProcessPid(ProcessRecord process) {
        if (process == null) {
            return Integer.toString(MY_PID);
        }
        return Integer.toString(process.pid);
    }

    public void setWallpaperProcess(ComponentName className) {
        if (!SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1")) {
            return;
        }
        final ComponentName cmpName = className.clone();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ActivityManagerService.this.mWallpaperClassName = cmpName;
                    ActivityManagerService.this.mIsWallpaperFg = true;
                }
            }
        });
    }

    public void updateWallpaperState(final boolean isForeground) {
        if (!SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1")) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ActivityManagerService.this.mIsWallpaperFg = isForeground;
                    if (ActivityManagerService.this.mWallpaperProcess != null && ActivityManagerService.this.mIsWallpaperFg) {
                        ActivityManagerService.this.updateOomAdjLocked(ActivityManagerService.this.mWallpaperProcess);
                    }
                }
            }
        });
    }

    class AnrActivityManagerService implements ANRManager.IAnrActivityManagerService {
        AnrActivityManagerService() {
        }

        @Override
        public boolean getShuttingDown() {
            return ActivityManagerService.this.mShuttingDown;
        }

        @Override
        public void getPidFromLruProcesses(int appPid, int parentPid, ArrayList<Integer> firstPids, SparseArray<Boolean> lastPids) {
            int pid;
            for (int i = ActivityManagerService.this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord r = ActivityManagerService.this.mLruProcesses.get(i);
                if (r != null && r.thread != null && (pid = r.pid) > 0 && pid != appPid && pid != parentPid && pid != ActivityManagerService.MY_PID) {
                    if (r.persistent) {
                        firstPids.add(Integer.valueOf(pid));
                    } else {
                        lastPids.put(pid, Boolean.TRUE);
                    }
                }
            }
        }

        @Override
        public ArrayList<Integer> getInterestingPids() {
            return ActivityManagerService.mInterestingPids;
        }

        @Override
        public File dumpStackTraces(boolean clearTraces, ArrayList<Integer> firstPids, ProcessCpuTracker processCpuTracker, SparseArray<Boolean> lastPids, String[] nativeProcs) {
            return ActivityManagerService.dumpStackTraces(clearTraces, firstPids, processCpuTracker, lastPids, nativeProcs);
        }

        @Override
        public int getProcessRecordPid(Object obj) {
            if (obj != null) {
                return ((ProcessRecord) obj).pid;
            }
            return -1;
        }

        @Override
        public boolean getMonitorCpuUsage() {
            return true;
        }

        @Override
        public void updateCpuStatsNow() {
            ActivityManagerService.this.updateCpuStatsNow();
        }

        @Override
        public ProcessCpuTracker getProcessCpuTracker() {
            return ActivityManagerService.this.mProcessCpuTracker;
        }
    }

    public boolean resumeTopActivityOnSystemReadyFocusedStackLocked() {
        if (this.mSystemReadyFocusedStack == null) {
            Slog.d(TAG, "SystemReadyFocusedStack not ready");
            return false;
        }
        return this.mSystemReadyFocusedStack.resumeTopActivityUncheckedLocked(null, null);
    }

    private void initOnSystemReady() {
        this.mSystemReadyFocusedStack = getFocusedStack();
        mANRManager.registerDumpNBTReceiver();
        int anrStatus = Settings.System.getInt(this.mContext.getContentResolver(), "anr_debugging_mechanism_status", 0);
        ANRManager.AnrOption = Settings.System.getInt(this.mContext.getContentResolver(), "anr_debugging_mechanism", 0);
        if (!IS_USER_BUILD && anrStatus == 0) {
            Settings.System.putInt(this.mContext.getContentResolver(), "anr_debugging_mechanism", 2);
            ANRManager.AnrOption = 2;
            Settings.System.putInt(this.mContext.getContentResolver(), "anr_debugging_mechanism_status", 1);
        }
        Settings.System.putInt(this.mContext.getContentResolver(), "anr_debugging_mechanism", 0);
        ANRManager.AnrOption = 0;
        if (SystemProperties.get("ro.mtk_emulator_support").equals("1")) {
            ANRManager.AnrOption = 0;
        }
        this.mCustomizedOomExt = (ICustomizedOomExt) MPlugin.createInstance(ICustomizedOomExt.class.getName(), this.mContext);
        if (!ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            return;
        }
        Slog.d(TAG, "CustomizedOomExt initialized: " + this.mCustomizedOomExt);
    }

    private void filterStaticReceivers(List<ResolveInfo> receivers, List<String> filterList) {
        int size = receivers != null ? receivers.size() : 0;
        int i = 0;
        while (i < size) {
            ResolveInfo curr = receivers.get(i);
            if (filterList.contains(curr.activityInfo.packageName)) {
                receivers.remove(i);
                size--;
                i--;
            }
            i++;
        }
    }

    private void filterRegisteredReceivers(List<BroadcastFilter> receivers, List<String> filterList) {
        int size = receivers != null ? receivers.size() : 0;
        int i = 0;
        while (i < size) {
            BroadcastFilter curr = receivers.get(i);
            if (filterList.contains(curr.receiverList.app.processName)) {
                receivers.remove(i);
                size--;
                i--;
            }
            i++;
        }
    }

    public void forceKillPackage(String packageName, int userId, String reason) {
        if (checkCallingPermission("android.permission.FORCE_STOP_PACKAGES") != 0) {
            String msg = "Permission Denial: forceKillPackage() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.FORCE_STOP_PACKAGES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        int callingPid = Binder.getCallingPid();
        int userId2 = this.mUserController.handleIncomingUser(callingPid, Binder.getCallingUid(), userId, true, 2, "forceKillPackage", null);
        long callingId = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            synchronized (this) {
                try {
                    boostPriorityForLockedSection();
                    int[] users = userId2 == -1 ? this.mUserController.getUsers() : new int[]{userId2};
                    for (int user : users) {
                        int pkgUid = -1;
                        try {
                            pkgUid = pm.getPackageUid(packageName, 268435456, user);
                        } catch (RemoteException e) {
                        }
                        if (pkgUid == -1) {
                            Slog.w(TAG, "Invalid packageName: " + packageName);
                        } else if (this.mUserController.isUserRunningLocked(user, 0)) {
                            forceStopPackageLocked(packageName, pkgUid, reason + " from pid " + callingPid);
                            finishForceStopPackageLocked(packageName, pkgUid);
                        }
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private boolean isSystemOrProtectedPackageName(String name) {
        if (name.matches("com\\.android\\..*")) {
            return true;
        }
        boolean isSystemOrProtected = name.matches("com\\.mediatek\\..*");
        return isSystemOrProtected;
    }

    private int getCustomizedAdj(String processName, int adj) {
        if (this.mCustomizedOomExt != null) {
            int customizedAdj = this.mCustomizedOomExt.getCustomizedAdj(processName);
            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.d(TAG, "getCustomizedAdj(" + processName + ") with adj = " + customizedAdj + " cur = " + adj);
            }
            if (customizedAdj != 1001 && adj > customizedAdj) {
                return customizedAdj;
            }
            return adj;
        }
        return adj;
    }

    public void stickWindow(IBinder token, boolean isSticky) throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    if (r == null) {
                        throw new IllegalArgumentException("stickWindow: No activity record matching token=" + token);
                    }
                    ActivityStack stack = ActivityRecord.getStackLocked(token);
                    if (stack == null || stack.mStackId != 2) {
                        throw new IllegalStateException("stickWindow: You can only stick window from freeform.");
                    }
                    stickWindow(r.task, isSticky);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void stickWindow(TaskRecord task, boolean isSticky) {
        Slog.d(TAG_STACK, "stickWindow, task = " + task + ", isSticky = " + isSticky);
        task.mSticky = isSticky;
        this.mWindowManager.stickWindow(task.stack.mStackId, task.taskId, isSticky);
    }

    public boolean isStickyByMtk(IBinder token) throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException("isSticky: No activity record matching token=" + token);
                }
                ActivityStack stack = ActivityRecord.getStackLocked(token);
                if (stack == null || stack.mStackId != 2) {
                    Slog.e(TAG, "isSticky: You can only stick window from freeform.");
                    return false;
                }
                return r.task.mSticky;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void restoreWindow() throws RemoteException {
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityRecord r = this.mFocusedActivity;
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_STACK, "[BMW] restoreWindow: " + r);
                    }
                    if (r == null) {
                        throw new IllegalArgumentException("[BMW] restoreWindow: mFocusedActivity is null.");
                    }
                    if (!r.task.isResizeable() || r.task.isRecentsTask()) {
                        Slog.e(TAG_STACK, "[BMW] restoreWindow: You can only restore window from resizeable task.");
                        resetPriorityAfterLockedSection();
                    } else {
                        r.forceNewConfig = true;
                        this.mStackSupervisor.moveTaskToStackLocked(r.task.taskId, 2, true, false, "restoreWindow", false);
                        resetPriorityAfterLockedSection();
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public String[] getPackageListFromPid(int pid) {
        ProcessRecord pr;
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("getPackageListFromPid called from non-system process");
        }
        synchronized (this.mPidsSelfLocked) {
            pr = this.mPidsSelfLocked.get(pid);
        }
        if (pr != null) {
            return pr.getPackageList();
        }
        return null;
    }

    public ArrayMap<Integer, ArrayList<Integer>> getProcessesWithAdj() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("getProcessesWithAdj called from non-system process");
        }
        ArrayMap<Integer, ArrayList<Integer>> processMap = new ArrayMap<>();
        synchronized (this) {
            try {
                boostPriorityForLockedSection();
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord r = this.mLruProcesses.get(i);
                    int adj = r.setAdj;
                    int pid = r.pid;
                    ArrayList<Integer> indicateList = processMap.get(Integer.valueOf(adj));
                    if (indicateList == null) {
                        indicateList = new ArrayList<>();
                        processMap.put(Integer.valueOf(adj), indicateList);
                    }
                    indicateList.add(Integer.valueOf(pid));
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return processMap;
    }

    public int readyToGetContentProvider(IApplicationThread caller, String name, int userId) {
        if (caller == null) {
            return 0;
        }
        ContentProviderRecord cpr = this.mProviderMap.getProviderByName(name, userId);
        ProviderInfo cpi = null;
        String packageName = null;
        if (cpr != null) {
            cpi = cpr.info;
        }
        if (cpi != null && cpi.applicationInfo != null) {
            packageName = cpi.applicationInfo.packageName;
        }
        ProcessRecord callerApp = getRecordForAppLocked(caller);
        List<String> callerList = new ArrayList<>();
        List<Integer> callerUidList = new ArrayList<>();
        if (callerApp != null && callerApp.pkgList != null) {
            for (int i = 0; i < callerApp.pkgList.size(); i++) {
                callerList.add(callerApp.pkgList.keyAt(i));
                callerUidList.add(Integer.valueOf(callerApp.userId));
            }
        }
        AMEventHookData.ReadyToStartComponent eventData = AMEventHookData.ReadyToStartComponent.createInstance();
        eventData.set(new Object[]{packageName, Integer.valueOf(userId), callerList, callerUidList, null, null, null, null, "provider", "allowed"});
        this.mAMEventHook.hook(AMEventHook.Event.AM_ReadyToStartComponent, eventData);
        String suppressAction = eventData.getString(AMEventHookData.ReadyToStartComponent.Index.suppressAction);
        Slog.d(TAG, "[process suppression] suppressAction = " + suppressAction);
        if (suppressAction != null && suppressAction.equals("delayed")) {
            return 1;
        }
        if (suppressAction != null && suppressAction.equals("skipped")) {
            return 2;
        }
        return 0;
    }

    static final IAWSProcessRecord convertProcessRecord(final ProcessRecord pr) {
        if (pr == null) {
            return null;
        }
        return new IAWSProcessRecord() {
            public String getPkgName() {
                return pr.info.packageName;
            }

            public int getPkgVer() {
                return pr.info.versionCode;
            }

            public String getProcName() {
                return pr.processName;
            }

            public int getPid() {
                return pr.pid;
            }

            public int getAdj() {
                return pr.curAdj;
            }

            public int getUid() {
                return pr.uid;
            }

            public int getprocState() {
                return pr.curProcState;
            }

            public ArrayMap<String, ProcessStats.ProcessStateHolder> getpkgList() {
                return pr.pkgList;
            }

            public boolean isKilledByAm() {
                return pr.killedByAm;
            }

            public boolean isKilled() {
                return pr.killed;
            }

            public String getWaitingToKill() {
                return pr.waitingToKill;
            }
        };
    }

    final IAWSStoreRecord convertStoreRecord(final ProcessRecord proc, final long extraVal) {
        return new IAWSStoreRecord() {
            public ArrayList<IAWSProcessRecord> getRecords() {
                ArrayList<IAWSProcessRecord> procList = new ArrayList<>();
                synchronized (ActivityManagerService.this.mPidsSelfLocked) {
                    int size = ActivityManagerService.this.mPidsSelfLocked.size();
                    for (int i = 0; i < size; i++) {
                        ProcessRecord proc2 = ActivityManagerService.this.mPidsSelfLocked.valueAt(i);
                        IAWSProcessRecord pr = ActivityManagerService.convertProcessRecord(proc2);
                        procList.add(pr);
                    }
                }
                return procList;
            }

            public IAWSProcessRecord getRecord() {
                if (proc == null) {
                    return null;
                }
                IAWSProcessRecord pr = ActivityManagerService.convertProcessRecord(proc);
                return pr;
            }

            public String getTopPkgName() {
                ActivityRecord top;
                synchronized (ActivityManagerService.this) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        ActivityStack stack = ActivityManagerService.this.mStackSupervisor.getFocusedStack();
                        if (stack != null) {
                            TaskRecord task = stack.topTask();
                            if (task != null && (top = stack.topActivity()) != null) {
                                String str = top.packageName;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return str;
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }

            public long getExtraVal() {
                return extraVal;
            }
        };
    }

    protected void configActivityLogTag(PrintWriter pw, String[] args, int opti) {
        if (opti + 1 >= args.length) {
            if (pw != null) {
                pw.println("  Invalid argument!");
            }
            SystemProperties.set("persist.sys.actthreadlog", "");
            return;
        }
        String tag = args[opti];
        boolean on = "on".equals(args[opti + 1]);
        if (tag != null) {
            configActivityLogTag(tag, on);
            return;
        }
        if (pw != null) {
            pw.println("  Invalid argument!");
        }
        SystemProperties.set("persist.sys.actthreadlog", "");
    }

    protected void configActivityLogTag(String tag, boolean on) {
        if (tag == null) {
            Slog.d(TAG, "Invalid argument: " + tag + " " + on);
            SystemProperties.set("persist.sys.actthreadlog", "");
            return;
        }
        if (on) {
            SystemProperties.set("persist.sys.actthreadlog", tag + " on");
        } else {
            SystemProperties.set("persist.sys.actthreadlog", tag + " off");
        }
        for (int i = 0; i < this.mLruProcesses.size(); i++) {
            ProcessRecord app = this.mLruProcesses.get(i);
            if (app != null && app.thread != null) {
                try {
                    app.thread.configActivityLogTag(tag, on);
                } catch (Exception e) {
                    Slog.e(TAG, "Error happens when configActivityLogTag", e);
                }
            }
        }
    }

    private boolean isChooserProcessFromUid(ProcessRecord app, int uid) {
        for (int i = app.activities.size() - 1; i >= 0; i--) {
            ActivityRecord r = app.activities.get(i);
            if (r.launchedFromUid == uid) {
                if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.d(TAG, "isChooserProcessFromUid: r=" + r + " launchedFromUid=" + r.launchedFromUid + " realActivity=" + r.realActivity + " intent=" + r.intent + " stack=" + r.task.stack + " task=" + r.task);
                }
                if (("android.intent.action.CHOOSER".equals(r.intent.getAction()) || r.isResolverActivity()) && r.task.stack != null) {
                    ActivityRecord top = r.task.stack.topRunningActivityLocked();
                    if (top == r) {
                        return true;
                    }
                    if (!IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                        Slog.d(TAG, "top=" + top);
                    }
                }
            }
        }
        return false;
    }

    public void setAalMode(int mode) {
        AalUtils.getInstance(true).setAalModeInternal(mode);
    }

    public void setAalEnabled(boolean enabled) {
        AalUtils.getInstance(true).setEnabledInternal(enabled);
    }

    public SuppressManager getSuppressManager() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("getSuppressManager called from non-system process");
        }
        return this.mSuppressManager;
    }

    public class SuppressManager {
        public static final int DOING_SUPPRESS_ADD = 0;
        public static final int DOING_SUPPRESS_FINISH = 2;
        public static final int DOING_SUPPRESS_SKIP = 1;

        public SuppressManager() {
        }

        public int checkCallingPermission(String permission) {
            return ActivityManagerService.this.checkCallingPermission(permission);
        }

        public int handleIncomingUser(int callingPid, int callingUid, int userId, boolean allowAll, int allowMode, String name, String callerPackage) {
            return ActivityManagerService.this.mUserController.handleIncomingUser(callingPid, callingUid, userId, allowAll, allowMode, "doSuppressPackage", callerPackage);
        }

        public void resumeFocusedStackTopActivityLocked() {
            ActivityManagerService.this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }

        public void scheduleIdleLocked() {
            ActivityManagerService.this.mStackSupervisor.scheduleIdleLocked();
        }

        public void broadcastIntentLocked(Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle options, boolean ordered, boolean sticky, int callingPid, int callingUid, int userId) {
            ActivityManagerService.this.broadcastIntentLocked(null, null, intent, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, appOp, options, ordered, sticky, callingPid, callingUid, userId);
        }

        public void triggerEventHook(AMEventHook.Event event, Object data) {
            ActivityManagerService.this.mAMEventHook.hook(event, data);
        }

        public int[] getUsers() {
            return ActivityManagerService.this.mUserController.getUsers();
        }

        public BroadcastQueue[] getBroadcastQueues() {
            return ActivityManagerService.this.mBroadcastQueues;
        }

        public void resetProcessCrashTimeLocked(boolean resetEntireUser, int appId, int userId) {
            ActivityManagerService.this.mAppErrors.resetProcessCrashTimeLocked(resetEntireUser, appId, userId);
        }

        public boolean getBooted() {
            return ActivityManagerService.this.mBooted;
        }

        public boolean getProcessesReady() {
            return ActivityManagerService.this.mProcessesReady;
        }

        public ServiceThread getKillThread() {
            ActivityManagerService activityManagerService = ActivityManagerService.this;
            return ActivityManagerService.sKillThread;
        }

        public boolean isUserRunningLocked(int userId, int flags) {
            return ActivityManagerService.this.mUserController.isUserRunningLocked(userId, flags);
        }

        public boolean isSuppressedProcessesLocked(String packageName, ArrayList<Object> procs, int appId, int userId) {
            int NP = ActivityManagerService.this.mProcessNames.getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) ActivityManagerService.this.mProcessNames.getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    SuppressionInternal mSuppressionInternal = (SuppressionInternal) LocalServices.getService(SuppressionInternal.class);
                    if (mSuppressionInternal != null) {
                        int doingSuppress = mSuppressionInternal.doingSuppress(packageName, userId, appId, app.userId, app.uid, app.setAdj, app.pkgDeps, app.pkgList);
                        if (doingSuppress != 1) {
                            if (doingSuppress == 2) {
                                return false;
                            }
                            procs.add(app);
                        }
                    } else {
                        return false;
                    }
                }
            }
            for (int i = procs.size() - 1; i >= 0; i--) {
                ProcessRecord proc = (ProcessRecord) procs.get(i);
                proc.removed = true;
            }
            return true;
        }

        public void killSuppressedProcessesLocked(String packageName, ArrayList<Object> procs, List<String> packageList, int userId) {
            SuppressionInternal mSuppressionInternal = (SuppressionInternal) LocalServices.getService(SuppressionInternal.class);
            if (mSuppressionInternal == null) {
                return;
            }
            for (int i = 0; i < procs.size(); i++) {
                ProcessRecord proc = (ProcessRecord) procs.get(i);
                if (mSuppressionInternal.isAllPackagesInList(proc.pkgList, packageList)) {
                    ActivityManagerService.this.removeProcessLocked(proc, false, true, packageName == null ? "Suppress " + userId : "Suppress " + packageName);
                }
            }
            ActivityManagerService.this.updateOomAdjLocked();
        }

        public boolean bringDownDisabledPackageServicesLocked(String packageName, Set<String> filterByClasses, int userId, boolean evenPersistent, boolean killProcess, boolean doit) {
            return ActivityManagerService.this.mServices.bringDownDisabledPackageServicesLocked(packageName, filterByClasses, userId, evenPersistent, killProcess, doit);
        }

        public void removeDyingProviderLocked(String packageName, int userId) {
            ArrayList<ContentProviderRecord> providers = new ArrayList<>();
            ActivityManagerService.this.mProviderMap.collectPackageProvidersLocked(packageName, (Set<String>) null, true, false, userId, providers);
            for (int i = providers.size() - 1; i >= 0; i--) {
                ActivityManagerService.this.removeDyingProviderLocked(null, providers.get(i), true);
            }
        }

        public void removeUriPermissionsForPackageLocked(String packageName, int userHandle, boolean persistable) {
            ActivityManagerService.this.removeUriPermissionsForPackageLocked(packageName, userHandle, persistable);
        }

        public boolean cleanupDisabledPackageReceiversLocked(BroadcastQueue broadcastQueue, String packageName, Set<String> filterByClasses, int userId, boolean doit) {
            return broadcastQueue.cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
        }
    }
}
