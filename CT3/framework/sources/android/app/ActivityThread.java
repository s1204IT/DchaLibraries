package android.app;

import android.R;
import android.app.Activity;
import android.app.IActivityManager;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.app.backup.BackupAgent;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDebug;
import android.ddm.DdmHandleAppName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.display.DisplayManagerGlobal;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.Proxy;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.provider.Settings;
import android.renderscript.RenderScriptCacheDir;
import android.security.NetworkSecurityPolicy;
import android.security.net.config.NetworkSecurityConfigProvider;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.SuperNotCalledException;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewManager;
import android.view.ViewRootImpl;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.RuntimeInit;
import com.android.internal.os.SamplingProfilerIntegration;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.org.conscrypt.OpenSSLSocketImpl;
import com.android.org.conscrypt.TrustedCertificateStore;
import com.google.android.collect.Lists;
import com.mediatek.anrappframeworks.ANRAppFrameworks;
import com.mediatek.anrappmanager.ANRAppManager;
import com.mediatek.anrappmanager.MessageLogger;
import dalvik.system.CloseGuard;
import dalvik.system.VMDebug;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import libcore.io.DropBox;
import libcore.io.EventLogger;
import libcore.io.IoUtils;
import libcore.net.event.NetworkEventDispatcher;
import org.apache.harmony.dalvik.ddmc.DdmVmInternal;

public final class ActivityThread {
    private static final int ACTIVITY_THREAD_CHECKIN_VERSION = 4;
    private static final int DONT_REPORT = 2;
    private static final String HEAP_COLUMN = "%13s %8s %8s %8s %8s %8s %8s %8s";
    private static final String HEAP_FULL_COLUMN = "%13s %8s %8s %8s %8s %8s %8s %8s %8s %8s %8s";
    static final boolean IS_USER_BUILD;
    private static final int LOG_AM_ON_PAUSE_CALLED = 30021;
    private static final int LOG_AM_ON_RESUME_CALLED = 30022;
    private static final int LOG_AM_ON_STOP_CALLED = 30049;
    private static final long MIN_TIME_BETWEEN_GCS = 5000;
    private static final String ONE_COUNT_COLUMN = "%21s %8d";
    private static final String ONE_COUNT_COLUMN_HEADER = "%21s %8s";
    private static final boolean REPORT_TO_ACTIVITY = true;
    public static final int SERVICE_DONE_EXECUTING_ANON = 0;
    public static final int SERVICE_DONE_EXECUTING_START = 1;
    public static final int SERVICE_DONE_EXECUTING_STOP = 2;
    private static final int SQLITE_MEM_RELEASED_EVENT_LOG_TAG = 75003;
    private static final int SUPPRESS_ACTION_ALLOWED = 0;
    private static final int SUPPRESS_ACTION_DELAYED = 1;
    private static final int SUPPRESS_TIME = 5000;
    public static final String TAG = "ActivityThread";
    private static final String TWO_COUNT_COLUMNS = "%21s %8d %21s %8d";
    private static final int USER_LEAVING = 1;
    private static volatile ActivityThread sCurrentActivityThread;
    private static final ThreadLocal<Intent> sCurrentBroadcastIntent;
    static volatile Handler sMainThreadHandler;
    static volatile IPackageManager sPackageManager;
    AppBindData mBoundApplication;
    Configuration mCompatConfiguration;
    Configuration mConfiguration;
    int mCurDefaultDisplayDpi;
    boolean mDensityCompatMode;
    Application mInitialApplication;
    Instrumentation mInstrumentation;
    private int mLastSessionId;
    Profiler mProfiler;
    private final ResourcesManager mResourcesManager;
    private ContextImpl mSystemContext;
    private static final Bitmap.Config THUMBNAIL_FORMAT = Bitmap.Config.RGB_565;
    static boolean localLOGV = false;
    static boolean DEBUG_MESSAGES = false;
    public static boolean DEBUG_BROADCAST = false;
    private static boolean DEBUG_RESULTS = false;
    private static boolean DEBUG_BACKUP = false;
    public static boolean DEBUG_CONFIGURATION = false;
    private static boolean DEBUG_SERVICE = false;
    private static boolean DEBUG_MEMORY_TRIM = false;
    private static boolean DEBUG_PROVIDER = false;
    private static boolean DEBUG_ORDER = false;
    private static boolean DEBUG_LIFECYCLE = false;
    private static final boolean mIsEngBuild = SystemProperties.get("ro.build.type").equals("eng");
    static final boolean IS_USER_DEBUG_BUILD = "userdebug".equals(Build.TYPE);
    final ApplicationThread mAppThread = new ApplicationThread(this, null);
    final Looper mLooper = Looper.myLooper();
    final H mH = new H(this, 0 == true ? 1 : 0);
    final ArrayMap<IBinder, ActivityClientRecord> mActivities = new ArrayMap<>();
    ActivityClientRecord mNewActivities = null;
    int mNumVisibleActivities = 0;
    ArrayList<WeakReference<AssistStructure>> mLastAssistStructures = new ArrayList<>();
    final ArrayMap<IBinder, Service> mServices = new ArrayMap<>();
    final ArrayList<Application> mAllApplications = new ArrayList<>();
    final ArrayMap<String, BackupAgent> mBackupAgents = new ArrayMap<>();
    String mInstrumentationPackageName = null;
    String mInstrumentationAppDir = null;
    String[] mInstrumentationSplitAppDirs = null;
    String mInstrumentationLibDir = null;
    String mInstrumentedAppDir = null;
    String[] mInstrumentedSplitAppDirs = null;
    String mInstrumentedLibDir = null;
    boolean mSystemThread = false;
    boolean mJitEnabled = false;
    boolean mSomeActivitiesChanged = false;
    final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<>();
    final ArrayMap<String, WeakReference<LoadedApk>> mResourcePackages = new ArrayMap<>();
    final ArrayList<ActivityClientRecord> mRelaunchingActivities = new ArrayList<>();
    Configuration mPendingConfiguration = null;

    @GuardedBy("mResourcesManager")
    int mLifecycleSeq = 0;
    final ArrayMap<ProviderKey, ProviderClientRecord> mProviderMap = new ArrayMap<>();
    final ArrayMap<IBinder, ProviderRefCount> mProviderRefCountMap = new ArrayMap<>();
    final ArrayMap<IBinder, ProviderClientRecord> mLocalProviders = new ArrayMap<>();
    final ArrayMap<ComponentName, ProviderClientRecord> mLocalProvidersByName = new ArrayMap<>();
    final ArrayMap<Activity, ArrayList<OnActivityPausedListener>> mOnPauseListeners = new ArrayMap<>();
    final GcIdler mGcIdler = new GcIdler();
    boolean mGcIdlerScheduled = false;
    Bundle mCoreSettings = null;
    private Configuration mMainThreadConfig = new Configuration();
    private int mThumbnailWidth = -1;
    private int mThumbnailHeight = -1;
    private Bitmap mAvailThumbnailBitmap = null;
    private Canvas mThumbnailCanvas = null;

    private native void dumpGraphicsInfo(FileDescriptor fileDescriptor);

    static {
        IS_USER_BUILD = !Context.USER_SERVICE.equals(Build.TYPE) ? IS_USER_DEBUG_BUILD : true;
        sCurrentBroadcastIntent = new ThreadLocal<>();
    }

    private static final class ProviderKey {
        final String authority;
        final int userId;

        public ProviderKey(String authority, int userId) {
            this.authority = authority;
            this.userId = userId;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ProviderKey)) {
                return false;
            }
            ProviderKey other = (ProviderKey) o;
            return Objects.equals(this.authority, other.authority) && this.userId == other.userId;
        }

        public int hashCode() {
            return (this.authority != null ? this.authority.hashCode() : 0) ^ this.userId;
        }
    }

    static final class ActivityClientRecord {
        Activity activity;
        ActivityInfo activityInfo;
        CompatibilityInfo compatInfo;
        Configuration createdConfig;
        int ident;
        Intent intent;
        boolean isForward;
        Activity.NonConfigurationInstances lastNonConfigurationInstances;
        Window mPendingRemoveWindow;
        WindowManager mPendingRemoveWindowManager;
        boolean mPreserveWindow;
        Configuration newConfig;
        boolean onlyLocalRequest;
        Configuration overrideConfig;
        LoadedApk packageInfo;
        int pendingConfigChanges;
        List<ReferrerIntent> pendingIntents;
        List<ResultInfo> pendingResults;
        PersistableBundle persistentState;
        ProfilerInfo profilerInfo;
        String referrer;
        boolean startsNotResumed;
        Bundle state;
        IBinder token;
        IVoiceInteractor voiceInteractor;
        Window window;
        private Configuration tmpConfig = new Configuration();
        int relaunchSeq = 0;
        int lastProcessedSeq = 0;
        Activity parent = null;
        String embeddedID = null;
        boolean paused = false;
        boolean stopped = false;
        boolean hideForNow = false;
        ActivityClientRecord nextIdle = null;

        ActivityClientRecord() {
        }

        public boolean isPreHoneycomb() {
            return this.activity != null && this.activity.getApplicationInfo().targetSdkVersion < 11;
        }

        public boolean isPersistable() {
            return this.activityInfo.persistableMode == 2;
        }

        public String toString() {
            ComponentName componentName = this.intent != null ? this.intent.getComponent() : null;
            return "ActivityRecord{" + Integer.toHexString(System.identityHashCode(this)) + " token=" + this.token + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + (componentName == null ? "no component name" : componentName.toShortString()) + "}";
        }

        public String getStateString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ActivityClientRecord{");
            sb.append("paused=").append(this.paused);
            sb.append(", stopped=").append(this.stopped);
            sb.append(", hideForNow=").append(this.hideForNow);
            sb.append(", startsNotResumed=").append(this.startsNotResumed);
            sb.append(", isForward=").append(this.isForward);
            sb.append(", pendingConfigChanges=").append(this.pendingConfigChanges);
            sb.append(", onlyLocalRequest=").append(this.onlyLocalRequest);
            sb.append(", preserveWindow=").append(this.mPreserveWindow);
            if (this.activity != null) {
                sb.append(", Activity{");
                sb.append("resumed=").append(this.activity.mResumed);
                sb.append(", stopped=").append(this.activity.mStopped);
                sb.append(", finished=").append(this.activity.isFinishing());
                sb.append(", destroyed=").append(this.activity.isDestroyed());
                sb.append(", startedActivity=").append(this.activity.mStartedActivity);
                sb.append(", temporaryPause=").append(this.activity.mTemporaryPause);
                sb.append(", changingConfigurations=").append(this.activity.mChangingConfigurations);
                sb.append(", visibleBehind=").append(this.activity.mVisibleBehind);
                sb.append("}");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    final class ProviderClientRecord {
        final IActivityManager.ContentProviderHolder mHolder;
        final ContentProvider mLocalProvider;
        final String[] mNames;
        final IContentProvider mProvider;

        ProviderClientRecord(String[] names, IContentProvider provider, ContentProvider localProvider, IActivityManager.ContentProviderHolder holder) {
            this.mNames = names;
            this.mProvider = provider;
            this.mLocalProvider = localProvider;
            this.mHolder = holder;
        }
    }

    static final class NewIntentData {
        List<ReferrerIntent> intents;
        IBinder token;

        NewIntentData() {
        }

        public String toString() {
            return "NewIntentData{intents=" + this.intents + " token=" + this.token + "}";
        }
    }

    static final class ReceiverData extends BroadcastReceiver.PendingResult {
        CompatibilityInfo compatInfo;
        ActivityInfo info;
        Intent intent;

        public ReceiverData(Intent intent, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, IBinder token, int sendingUser) {
            super(resultCode, resultData, resultExtras, 0, ordered, sticky, token, sendingUser, intent.getFlags());
            this.intent = intent;
        }

        public String toString() {
            return "ReceiverData{intent=" + this.intent + " packageName=" + this.info.packageName + " resultCode=" + getResultCode() + " resultData=" + getResultData() + " resultExtras=" + getResultExtras(false) + "}";
        }
    }

    static final class CreateBackupAgentData {
        ApplicationInfo appInfo;
        int backupMode;
        CompatibilityInfo compatInfo;

        CreateBackupAgentData() {
        }

        public String toString() {
            return "CreateBackupAgentData{appInfo=" + this.appInfo + " backupAgent=" + this.appInfo.backupAgentName + " mode=" + this.backupMode + "}";
        }
    }

    static final class CreateServiceData {
        CompatibilityInfo compatInfo;
        ServiceInfo info;
        Intent intent;
        IBinder token;

        CreateServiceData() {
        }

        public String toString() {
            return "CreateServiceData{token=" + this.token + " className=" + this.info.name + " packageName=" + this.info.packageName + " intent=" + this.intent + "}";
        }
    }

    static final class BindServiceData {
        Intent intent;
        boolean rebind;
        IBinder token;

        BindServiceData() {
        }

        public String toString() {
            return "BindServiceData{token=" + this.token + " intent=" + this.intent + "}";
        }
    }

    static final class ServiceArgsData {
        Intent args;
        int flags;
        int startId;
        boolean taskRemoved;
        IBinder token;

        ServiceArgsData() {
        }

        public String toString() {
            return "ServiceArgsData{token=" + this.token + " startId=" + this.startId + " args=" + this.args + "}";
        }
    }

    static final class AppBindData {
        ApplicationInfo appInfo;
        CompatibilityInfo compatInfo;
        Configuration config;
        int debugMode;
        boolean enableBinderTracking;
        LoadedApk info;
        ProfilerInfo initProfilerInfo;
        Bundle instrumentationArgs;
        ComponentName instrumentationName;
        IUiAutomationConnection instrumentationUiAutomationConnection;
        IInstrumentationWatcher instrumentationWatcher;
        boolean persistent;
        String processName;
        List<ProviderInfo> providers;
        boolean restrictedBackupMode;
        boolean trackAllocation;

        AppBindData() {
        }

        public String toString() {
            return "AppBindData{appInfo=" + this.appInfo + "}";
        }
    }

    static final class Profiler {
        boolean autoStopProfiler;
        boolean handlingProfiling;
        ParcelFileDescriptor profileFd;
        String profileFile;
        boolean profiling;
        int samplingInterval;

        Profiler() {
        }

        public void setProfiler(ProfilerInfo profilerInfo) {
            ParcelFileDescriptor fd = profilerInfo.profileFd;
            if (this.profiling) {
                if (fd != null) {
                    try {
                        fd.close();
                        return;
                    } catch (IOException e) {
                        return;
                    }
                }
                return;
            }
            if (this.profileFd != null) {
                try {
                    this.profileFd.close();
                } catch (IOException e2) {
                }
            }
            this.profileFile = profilerInfo.profileFile;
            this.profileFd = fd;
            this.samplingInterval = profilerInfo.samplingInterval;
            this.autoStopProfiler = profilerInfo.autoStopProfiler;
        }

        public void startProfiling() {
            if (this.profileFd == null || this.profiling) {
                return;
            }
            try {
                int bufferSize = SystemProperties.getInt("debug.traceview-buffer-size-mb", 8);
                VMDebug.startMethodTracing(this.profileFile, this.profileFd.getFileDescriptor(), bufferSize * 1024 * 1024, 0, this.samplingInterval != 0, this.samplingInterval);
                this.profiling = true;
            } catch (RuntimeException e) {
                Slog.w(ActivityThread.TAG, "Profiling failed on path " + this.profileFile);
                try {
                    this.profileFd.close();
                    this.profileFd = null;
                } catch (IOException e2) {
                    Slog.w(ActivityThread.TAG, "Failure closing profile fd", e2);
                }
            }
        }

        public void stopProfiling() {
            if (!this.profiling) {
                return;
            }
            this.profiling = false;
            Debug.stopMethodTracing();
            if (this.profileFd != null) {
                try {
                    this.profileFd.close();
                } catch (IOException e) {
                }
            }
            this.profileFd = null;
            this.profileFile = null;
        }
    }

    static final class DumpComponentInfo {
        String[] args;
        ParcelFileDescriptor fd;
        String prefix;
        IBinder token;

        DumpComponentInfo() {
        }
    }

    static final class ResultData {
        List<ResultInfo> results;
        IBinder token;

        ResultData() {
        }

        public String toString() {
            return "ResultData{token=" + this.token + " results" + this.results + "}";
        }
    }

    static final class ContextCleanupInfo {
        ContextImpl context;
        String what;
        String who;

        ContextCleanupInfo() {
        }
    }

    static final class DumpHeapData {
        ParcelFileDescriptor fd;
        String path;

        DumpHeapData() {
        }
    }

    static final class UpdateCompatibilityData {
        CompatibilityInfo info;
        String pkg;

        UpdateCompatibilityData() {
        }
    }

    static final class RequestAssistContextExtras {
        IBinder activityToken;
        IBinder requestToken;
        int requestType;
        int sessionId;

        RequestAssistContextExtras() {
        }
    }

    static final class ActivityConfigChangeData {
        final IBinder activityToken;
        final Configuration overrideConfig;

        public ActivityConfigChangeData(IBinder token, Configuration config) {
            this.activityToken = token;
            this.overrideConfig = config;
        }
    }

    private class ApplicationThread extends ApplicationThreadNative {
        private static final String DB_INFO_FORMAT = "  %8s %8s %14s %14s  %s";
        private int mLastProcessState;

        ApplicationThread(ActivityThread this$0, ApplicationThread applicationThread) {
            this();
        }

        private ApplicationThread() {
            this.mLastProcessState = -1;
        }

        private void updatePendingConfiguration(Configuration config) {
            synchronized (ActivityThread.this.mResourcesManager) {
                if (ActivityThread.this.mPendingConfiguration == null || ActivityThread.this.mPendingConfiguration.isOtherSeqNewer(config)) {
                    ActivityThread.this.mPendingConfiguration = config;
                }
            }
        }

        @Override
        public final void schedulePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges, boolean dontReport) {
            int seq = ActivityThread.this.getLifecycleSeq();
            if (ActivityThread.DEBUG_ORDER) {
                Slog.d(ActivityThread.TAG, "pauseActivity " + ActivityThread.this + " operation received seq: " + seq);
            }
            ActivityThread.this.sendMessage(finished ? 102 : 101, token, (userLeaving ? 1 : 0) | (dontReport ? 2 : 0), configChanges, seq);
        }

        @Override
        public final void scheduleStopActivity(IBinder token, boolean showWindow, int configChanges) {
            int seq = ActivityThread.this.getLifecycleSeq();
            if (ActivityThread.DEBUG_ORDER) {
                Slog.d(ActivityThread.TAG, "stopActivity " + ActivityThread.this + " operation received seq: " + seq);
            }
            ActivityThread.this.sendMessage(showWindow ? 103 : 104, token, 0, configChanges, seq);
        }

        @Override
        public final void scheduleWindowVisibility(IBinder token, boolean showWindow) {
            ActivityThread.this.sendMessage(showWindow ? 105 : 106, token);
        }

        @Override
        public final void scheduleSleeping(IBinder token, boolean sleeping) {
            ActivityThread.this.sendMessage(137, token, sleeping ? 1 : 0);
        }

        @Override
        public final void scheduleResumeActivity(IBinder token, int processState, boolean isForward, Bundle resumeArgs) {
            int seq = ActivityThread.this.getLifecycleSeq();
            if (ActivityThread.DEBUG_ORDER) {
                Slog.d(ActivityThread.TAG, "resumeActivity " + ActivityThread.this + " operation received seq: " + seq);
            }
            updateProcessState(processState, false);
            ActivityThread.this.sendMessage(107, token, isForward ? 1 : 0, 0, seq);
        }

        @Override
        public final void scheduleSendResult(IBinder token, List<ResultInfo> results) {
            ResultData res = new ResultData();
            res.token = token;
            res.results = results;
            ActivityThread.this.sendMessage(108, res);
        }

        @Override
        public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident, ActivityInfo info, Configuration curConfig, Configuration overrideConfig, CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor, int procState, Bundle state, PersistableBundle persistentState, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) {
            updateProcessState(procState, false);
            ActivityClientRecord r = new ActivityClientRecord();
            r.token = token;
            r.ident = ident;
            r.intent = intent;
            r.referrer = referrer;
            r.voiceInteractor = voiceInteractor;
            r.activityInfo = info;
            r.compatInfo = compatInfo;
            r.state = state;
            r.persistentState = persistentState;
            r.pendingResults = pendingResults;
            r.pendingIntents = pendingNewIntents;
            r.startsNotResumed = notResumed;
            r.isForward = isForward;
            r.profilerInfo = profilerInfo;
            r.overrideConfig = overrideConfig;
            updatePendingConfiguration(curConfig);
            ActivityThread.this.sendMessage(100, r);
        }

        @Override
        public final void scheduleRelaunchActivity(IBinder token, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, int configChanges, boolean notResumed, Configuration config, Configuration overrideConfig, boolean preserveWindow) throws Throwable {
            ActivityThread.this.requestRelaunchActivity(token, pendingResults, pendingNewIntents, configChanges, notResumed, config, overrideConfig, true, preserveWindow);
        }

        @Override
        public final void scheduleNewIntent(List<ReferrerIntent> intents, IBinder token) {
            NewIntentData data = new NewIntentData();
            data.intents = intents;
            data.token = token;
            ActivityThread.this.sendMessage(112, data);
        }

        @Override
        public final void scheduleDestroyActivity(IBinder token, boolean finishing, int configChanges) {
            ActivityThread.this.sendMessage(109, token, finishing ? 1 : 0, configChanges);
        }

        @Override
        public final void scheduleReceiver(Intent intent, ActivityInfo info, CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras, boolean sync, int sendingUser, int processState) {
            updateProcessState(processState, false);
            ReceiverData r = new ReceiverData(intent, resultCode, data, extras, sync, false, ActivityThread.this.mAppThread.asBinder(), sendingUser);
            r.info = info;
            r.compatInfo = compatInfo;
            ActivityThread.this.sendMessage(113, r);
        }

        @Override
        public final void scheduleCreateBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int backupMode) {
            CreateBackupAgentData d = new CreateBackupAgentData();
            d.appInfo = app;
            d.compatInfo = compatInfo;
            d.backupMode = backupMode;
            ActivityThread.this.sendMessage(128, d);
        }

        @Override
        public final void scheduleDestroyBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo) {
            CreateBackupAgentData d = new CreateBackupAgentData();
            d.appInfo = app;
            d.compatInfo = compatInfo;
            ActivityThread.this.sendMessage(129, d);
        }

        @Override
        public final void scheduleCreateService(IBinder token, ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
            updateProcessState(processState, false);
            CreateServiceData s = new CreateServiceData();
            s.token = token;
            s.info = info;
            s.compatInfo = compatInfo;
            ActivityThread.this.sendMessage(114, s);
        }

        @Override
        public final void scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState) {
            updateProcessState(processState, false);
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            s.rebind = rebind;
            if (ActivityThread.DEBUG_SERVICE) {
                Slog.v(ActivityThread.TAG, "scheduleBindService token=" + token + " intent=" + intent + " uid=" + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
            }
            ActivityThread.this.sendMessage(121, s);
        }

        @Override
        public final void scheduleUnbindService(IBinder token, Intent intent) {
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            ActivityThread.this.sendMessage(122, s);
        }

        @Override
        public final void scheduleServiceArgs(IBinder token, boolean taskRemoved, int startId, int flags, Intent args) {
            ServiceArgsData s = new ServiceArgsData();
            s.token = token;
            s.taskRemoved = taskRemoved;
            s.startId = startId;
            s.flags = flags;
            s.args = args;
            ActivityThread.this.sendMessage(115, s);
        }

        @Override
        public final void scheduleStopService(IBinder token) {
            ActivityThread.this.sendMessage(116, token);
        }

        @Override
        public final void bindApplication(String processName, ApplicationInfo appInfo, List<ProviderInfo> providers, ComponentName instrumentationName, ProfilerInfo profilerInfo, Bundle instrumentationArgs, IInstrumentationWatcher instrumentationWatcher, IUiAutomationConnection instrumentationUiConnection, int debugMode, boolean enableBinderTracking, boolean trackAllocation, boolean isRestrictedBackupMode, boolean persistent, Configuration config, CompatibilityInfo compatInfo, Map<String, IBinder> services, Bundle coreSettings) {
            if (services != null) {
                ServiceManager.initServiceCache(services);
            }
            setCoreSettings(coreSettings);
            AppBindData data = new AppBindData();
            data.processName = processName;
            data.appInfo = appInfo;
            data.providers = providers;
            data.instrumentationName = instrumentationName;
            data.instrumentationArgs = instrumentationArgs;
            data.instrumentationWatcher = instrumentationWatcher;
            data.instrumentationUiAutomationConnection = instrumentationUiConnection;
            data.debugMode = debugMode;
            data.enableBinderTracking = enableBinderTracking;
            data.trackAllocation = trackAllocation;
            data.restrictedBackupMode = isRestrictedBackupMode;
            data.persistent = persistent;
            data.config = config;
            data.compatInfo = compatInfo;
            data.initProfilerInfo = profilerInfo;
            ActivityThread.this.sendMessage(110, data);
        }

        @Override
        public final void scheduleExit() {
            ActivityThread.this.sendMessage(111, null);
        }

        @Override
        public final void scheduleSuicide() {
            ActivityThread.this.sendMessage(130, null);
        }

        @Override
        public void scheduleConfigurationChanged(Configuration config) {
            updatePendingConfiguration(config);
            ActivityThread.this.sendMessage(118, config);
        }

        @Override
        public void updateTimeZone() {
            TimeZone.setDefault(null);
        }

        @Override
        public void clearDnsCache() {
            InetAddress.clearDnsCache();
            NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
        }

        @Override
        public void setHttpProxy(String host, String port, String exclList, Uri pacFileUrl) {
            ConnectivityManager cm = ConnectivityManager.from(ActivityThread.this.getSystemContext());
            Network network = cm.getBoundNetworkForProcess();
            if (network != null) {
                Proxy.setHttpProxySystemProperty(cm.getDefaultProxy());
            } else {
                Proxy.setHttpProxySystemProperty(host, port, exclList, pacFileUrl);
            }
        }

        @Override
        public void processInBackground() {
            ActivityThread.this.mH.removeMessages(120);
            ActivityThread.this.mH.sendMessage(ActivityThread.this.mH.obtainMessage(120));
        }

        @Override
        public void dumpService(FileDescriptor fd, IBinder servicetoken, String[] args) {
            DumpComponentInfo data = new DumpComponentInfo();
            try {
                data.fd = ParcelFileDescriptor.dup(fd);
                data.token = servicetoken;
                data.args = args;
                ActivityThread.this.sendMessage(123, (Object) data, 0, 0, true);
            } catch (IOException e) {
                Slog.w(ActivityThread.TAG, "dumpService failed", e);
            }
        }

        @Override
        public void scheduleRegisteredReceiver(IIntentReceiver receiver, Intent intent, int resultCode, String dataStr, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int processState) throws RemoteException {
            updateProcessState(processState, false);
            receiver.performReceive(intent, resultCode, dataStr, extras, ordered, sticky, sendingUser);
        }

        @Override
        public void scheduleLowMemory() {
            ActivityThread.this.sendMessage(124, null);
        }

        @Override
        public void scheduleActivityConfigurationChanged(IBinder token, Configuration overrideConfig, boolean reportToActivity) {
            ActivityThread.this.sendMessage(125, new ActivityConfigChangeData(token, overrideConfig), reportToActivity ? 1 : 0);
        }

        @Override
        public void profilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) {
            ActivityThread.this.sendMessage(127, profilerInfo, start ? 1 : 0, profileType);
        }

        @Override
        public void dumpHeap(boolean managed, String path, ParcelFileDescriptor fd) {
            DumpHeapData dhd = new DumpHeapData();
            dhd.path = path;
            dhd.fd = fd;
            ActivityThread.this.sendMessage(135, (Object) dhd, managed ? 1 : 0, 0, true);
        }

        @Override
        public void setSchedulingGroup(int group) {
            try {
                Process.setProcessGroup(Process.myPid(), group);
            } catch (Exception e) {
                Slog.w(ActivityThread.TAG, "Failed setting process group to " + group, e);
            }
        }

        @Override
        public void dispatchPackageBroadcast(int cmd, String[] packages) {
            ActivityThread.this.sendMessage(133, packages, cmd);
        }

        @Override
        public void scheduleCrash(String msg) {
            ActivityThread.this.sendMessage(134, msg);
        }

        @Override
        public void dumpActivity(FileDescriptor fd, IBinder activitytoken, String prefix, String[] args) {
            DumpComponentInfo data = new DumpComponentInfo();
            try {
                data.fd = ParcelFileDescriptor.dup(fd);
                data.token = activitytoken;
                data.prefix = prefix;
                data.args = args;
                ActivityThread.this.sendMessage(136, (Object) data, 0, 0, true);
            } catch (IOException e) {
                Slog.w(ActivityThread.TAG, "dumpActivity failed", e);
            }
        }

        @Override
        public void dumpProvider(FileDescriptor fd, IBinder providertoken, String[] args) {
            DumpComponentInfo data = new DumpComponentInfo();
            try {
                data.fd = ParcelFileDescriptor.dup(fd);
                data.token = providertoken;
                data.args = args;
                ActivityThread.this.sendMessage(141, (Object) data, 0, 0, true);
            } catch (IOException e) {
                Slog.w(ActivityThread.TAG, "dumpProvider failed", e);
            }
        }

        @Override
        public void dumpMemInfo(FileDescriptor fd, Debug.MemoryInfo mem, boolean checkin, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) {
            FileOutputStream fout = new FileOutputStream(fd);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(fout);
            try {
                dumpMemInfo(fastPrintWriter, mem, checkin, dumpFullInfo, dumpDalvik, dumpSummaryOnly, dumpUnreachable);
            } finally {
                fastPrintWriter.flush();
            }
        }

        private void dumpMemInfo(PrintWriter pw, Debug.MemoryInfo memInfo, boolean checkin, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable) {
            boolean z;
            long nativeMax = Debug.getNativeHeapSize() / 1024;
            long nativeAllocated = Debug.getNativeHeapAllocatedSize() / 1024;
            long nativeFree = Debug.getNativeHeapFreeSize() / 1024;
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long dalvikMax = runtime.totalMemory() / 1024;
            long dalvikFree = runtime.freeMemory() / 1024;
            long dalvikAllocated = dalvikMax - dalvikFree;
            long viewInstanceCount = ViewDebug.getViewInstanceCount();
            long viewRootInstanceCount = ViewDebug.getViewRootImplCount();
            long appContextInstanceCount = Debug.countInstancesOfClass(ContextImpl.class);
            long activityInstanceCount = Debug.countInstancesOfClass(Activity.class);
            int globalAssetCount = AssetManager.getGlobalAssetCount();
            int globalAssetManagerCount = AssetManager.getGlobalAssetManagerCount();
            int binderLocalObjectCount = Debug.getBinderLocalObjectCount();
            int binderProxyObjectCount = Debug.getBinderProxyObjectCount();
            int binderDeathObjectCount = Debug.getBinderDeathObjectCount();
            long parcelSize = Parcel.getGlobalAllocSize();
            long parcelCount = Parcel.getGlobalAllocCount();
            long openSslSocketCount = Debug.countInstancesOfClass(OpenSSLSocketImpl.class);
            SQLiteDebug.PagerStats stats = SQLiteDebug.getDatabaseInfo();
            ActivityThread.dumpMemInfoTable(pw, memInfo, checkin, dumpFullInfo, dumpDalvik, dumpSummaryOnly, Process.myPid(), ActivityThread.this.mBoundApplication != null ? ActivityThread.this.mBoundApplication.processName : "unknown", nativeMax, nativeAllocated, nativeFree, dalvikMax, dalvikAllocated, dalvikFree);
            if (checkin) {
                pw.print(viewInstanceCount);
                pw.print(',');
                pw.print(viewRootInstanceCount);
                pw.print(',');
                pw.print(appContextInstanceCount);
                pw.print(',');
                pw.print(activityInstanceCount);
                pw.print(',');
                pw.print(globalAssetCount);
                pw.print(',');
                pw.print(globalAssetManagerCount);
                pw.print(',');
                pw.print(binderLocalObjectCount);
                pw.print(',');
                pw.print(binderProxyObjectCount);
                pw.print(',');
                pw.print(binderDeathObjectCount);
                pw.print(',');
                pw.print(openSslSocketCount);
                pw.print(',');
                pw.print(stats.memoryUsed / 1024);
                pw.print(',');
                pw.print(stats.memoryUsed / 1024);
                pw.print(',');
                pw.print(stats.pageCacheOverflow / 1024);
                pw.print(',');
                pw.print(stats.largestMemAlloc / 1024);
                for (int i = 0; i < stats.dbStats.size(); i++) {
                    SQLiteDebug.DbStats dbStats = stats.dbStats.get(i);
                    pw.print(',');
                    pw.print(dbStats.dbName);
                    pw.print(',');
                    pw.print(dbStats.pageSize);
                    pw.print(',');
                    pw.print(dbStats.dbSize);
                    pw.print(',');
                    pw.print(dbStats.lookaside);
                    pw.print(',');
                    pw.print(dbStats.cache);
                    pw.print(',');
                    pw.print(dbStats.cache);
                }
                pw.println();
                return;
            }
            pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            pw.println(" Objects");
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "Views:", Long.valueOf(viewInstanceCount), "ViewRootImpl:", Long.valueOf(viewRootInstanceCount));
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "AppContexts:", Long.valueOf(appContextInstanceCount), "Activities:", Long.valueOf(activityInstanceCount));
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "Assets:", Integer.valueOf(globalAssetCount), "AssetManagers:", Integer.valueOf(globalAssetManagerCount));
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "Local Binders:", Integer.valueOf(binderLocalObjectCount), "Proxy Binders:", Integer.valueOf(binderProxyObjectCount));
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "Parcel memory:", Long.valueOf(parcelSize / 1024), "Parcel count:", Long.valueOf(parcelCount));
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "Death Recipients:", Integer.valueOf(binderDeathObjectCount), "OpenSSL Sockets:", Long.valueOf(openSslSocketCount));
            pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            pw.println(" SQL");
            ActivityThread.printRow(pw, ActivityThread.ONE_COUNT_COLUMN, "MEMORY_USED:", Integer.valueOf(stats.memoryUsed / 1024));
            ActivityThread.printRow(pw, ActivityThread.TWO_COUNT_COLUMNS, "PAGECACHE_OVERFLOW:", Integer.valueOf(stats.pageCacheOverflow / 1024), "MALLOC_SIZE:", Integer.valueOf(stats.largestMemAlloc / 1024));
            pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            int N = stats.dbStats.size();
            if (N > 0) {
                pw.println(" DATABASES");
                ActivityThread.printRow(pw, DB_INFO_FORMAT, "pgsz", "dbsz", "Lookaside(b)", "cache", "Dbname");
                for (int i2 = 0; i2 < N; i2++) {
                    SQLiteDebug.DbStats dbStats2 = stats.dbStats.get(i2);
                    Object[] objArr = new Object[5];
                    objArr[0] = dbStats2.pageSize > 0 ? String.valueOf(dbStats2.pageSize) : WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
                    objArr[1] = dbStats2.dbSize > 0 ? String.valueOf(dbStats2.dbSize) : WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
                    objArr[2] = dbStats2.lookaside > 0 ? String.valueOf(dbStats2.lookaside) : WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
                    objArr[3] = dbStats2.cache;
                    objArr[4] = dbStats2.dbName;
                    ActivityThread.printRow(pw, DB_INFO_FORMAT, objArr);
                }
            }
            String assetAlloc = AssetManager.getAssetAllocations();
            if (assetAlloc != null) {
                pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                pw.println(" Asset Allocations");
                pw.print(assetAlloc);
            }
            if (!dumpUnreachable) {
                return;
            }
            if (ActivityThread.this.mBoundApplication != null && (ActivityThread.this.mBoundApplication.appInfo.flags & 2) != 0) {
                z = true;
            } else {
                z = Build.IS_DEBUGGABLE;
            }
            pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            pw.println(" Unreachable memory");
            pw.print(Debug.getUnreachableMemory(100, z));
        }

        @Override
        public void dumpGfxInfo(FileDescriptor fd, String[] args) {
            ActivityThread.this.dumpGraphicsInfo(fd);
            WindowManagerGlobal.getInstance().dumpGfxInfo(fd, args);
        }

        private void dumpDatabaseInfo(FileDescriptor fd, String[] args) {
            FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fd));
            PrintWriterPrinter printer = new PrintWriterPrinter(fastPrintWriter);
            SQLiteDebug.dump(printer, args);
            fastPrintWriter.flush();
        }

        @Override
        public void dumpDbInfo(FileDescriptor fd, final String[] args) {
            if (ActivityThread.this.mSystemThread) {
                try {
                    final ParcelFileDescriptor dup = ParcelFileDescriptor.dup(fd);
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ApplicationThread.this.dumpDatabaseInfo(dup.getFileDescriptor(), args);
                            } finally {
                                IoUtils.closeQuietly(dup);
                            }
                        }
                    });
                    return;
                } catch (IOException e) {
                    Log.w(ActivityThread.TAG, "Could not dup FD " + fd.getInt$());
                    return;
                }
            }
            dumpDatabaseInfo(fd, args);
        }

        @Override
        public void unstableProviderDied(IBinder provider) {
            ActivityThread.this.sendMessage(142, provider);
        }

        @Override
        public void requestAssistContextExtras(IBinder activityToken, IBinder requestToken, int requestType, int sessionId) {
            RequestAssistContextExtras cmd = new RequestAssistContextExtras();
            cmd.activityToken = activityToken;
            cmd.requestToken = requestToken;
            cmd.requestType = requestType;
            cmd.sessionId = sessionId;
            ActivityThread.this.sendMessage(143, cmd);
        }

        @Override
        public void setCoreSettings(Bundle coreSettings) {
            ActivityThread.this.sendMessage(138, coreSettings);
        }

        @Override
        public void updatePackageCompatibilityInfo(String pkg, CompatibilityInfo info) {
            UpdateCompatibilityData ucd = new UpdateCompatibilityData();
            ucd.pkg = pkg;
            ucd.info = info;
            ActivityThread.this.sendMessage(139, ucd);
        }

        @Override
        public void scheduleTrimMemory(int level) {
            ActivityThread.this.sendMessage(140, null, level);
        }

        @Override
        public void scheduleTranslucentConversionComplete(IBinder token, boolean drawComplete) {
            ActivityThread.this.sendMessage(144, token, drawComplete ? 1 : 0);
        }

        @Override
        public void scheduleOnNewActivityOptions(IBinder token, ActivityOptions options) {
            ActivityThread.this.sendMessage(146, new Pair(token, options));
        }

        @Override
        public void setProcessState(int state) {
            updateProcessState(state, true);
        }

        public void updateProcessState(int processState, boolean fromIpc) {
            synchronized (this) {
                if (this.mLastProcessState != processState) {
                    this.mLastProcessState = processState;
                    int dalvikProcessState = 1;
                    if (processState <= 6) {
                        dalvikProcessState = 0;
                    }
                    VMRuntime.getRuntime().updateProcessState(dalvikProcessState);
                }
            }
        }

        @Override
        public void scheduleInstallProvider(ProviderInfo provider) {
            ActivityThread.this.sendMessage(145, provider);
        }

        @Override
        public final void updateTimePrefs(boolean is24Hour) {
            DateFormat.set24HourTimePref(is24Hour);
        }

        @Override
        public void scheduleCancelVisibleBehind(IBinder token) {
            ActivityThread.this.sendMessage(147, token);
        }

        @Override
        public void scheduleBackgroundVisibleBehindChanged(IBinder token, boolean visible) {
            ActivityThread.this.sendMessage(148, token, visible ? 1 : 0);
        }

        @Override
        public void scheduleEnterAnimationComplete(IBinder token) {
            ActivityThread.this.sendMessage(149, token);
        }

        @Override
        public void notifyCleartextNetwork(byte[] firstPacket) {
            if (!StrictMode.vmCleartextNetworkEnabled()) {
                return;
            }
            StrictMode.onCleartextNetworkDetected(firstPacket);
        }

        @Override
        public void startBinderTracking() {
            ActivityThread.this.sendMessage(150, null);
        }

        @Override
        public void stopBinderTrackingAndDump(FileDescriptor fd) {
            try {
                ActivityThread.this.sendMessage(151, ParcelFileDescriptor.dup(fd));
            } catch (IOException e) {
            }
        }

        @Override
        public void scheduleMultiWindowModeChanged(IBinder token, boolean isInMultiWindowMode) throws RemoteException {
            ActivityThread.this.sendMessage(152, token, isInMultiWindowMode ? 1 : 0);
        }

        @Override
        public void schedulePictureInPictureModeChanged(IBinder token, boolean isInPipMode) throws RemoteException {
            ActivityThread.this.sendMessage(153, token, isInPipMode ? 1 : 0);
        }

        @Override
        public void scheduleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor voiceInteractor) throws RemoteException {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = token;
            args.arg2 = voiceInteractor;
            ActivityThread.this.sendMessage(154, args);
        }

        @Override
        public void enableLooperLog() {
            ActivityThread.enableLooperLog();
        }

        @Override
        public void dumpMessageHistory() {
            ANRAppManager.dumpMessageHistory();
        }

        @Override
        public void dumpAllMessageHistory() {
            ANRAppManager.dumpAllMessageHistory();
        }

        @Override
        public void configActivityLogTag(String tag, boolean on) {
            ActivityThread.this.configActivityLogTag(tag, on);
        }
    }

    private int getLifecycleSeq() {
        int i;
        synchronized (this.mResourcesManager) {
            i = this.mLifecycleSeq;
            this.mLifecycleSeq = i + 1;
        }
        return i;
    }

    private class H extends Handler {
        public static final int ACTIVITY_CONFIGURATION_CHANGED = 125;
        public static final int BACKGROUND_VISIBLE_BEHIND_CHANGED = 148;
        public static final int BIND_APPLICATION = 110;
        public static final int BIND_SERVICE = 121;
        public static final int CANCEL_VISIBLE_BEHIND = 147;
        public static final int CLEAN_UP_CONTEXT = 119;
        public static final int CONFIGURATION_CHANGED = 118;
        public static final int CREATE_BACKUP_AGENT = 128;
        public static final int CREATE_SERVICE = 114;
        public static final int DESTROY_ACTIVITY = 109;
        public static final int DESTROY_BACKUP_AGENT = 129;
        public static final int DISPATCH_PACKAGE_BROADCAST = 133;
        public static final int DUMP_ACTIVITY = 136;
        public static final int DUMP_HEAP = 135;
        public static final int DUMP_PROVIDER = 141;
        public static final int DUMP_SERVICE = 123;
        public static final int ENABLE_JIT = 132;
        public static final int ENTER_ANIMATION_COMPLETE = 149;
        public static final int EXIT_APPLICATION = 111;
        public static final int GC_WHEN_IDLE = 120;
        public static final int HIDE_WINDOW = 106;
        public static final int INSTALL_PROVIDER = 145;
        public static final int LAUNCH_ACTIVITY = 100;
        public static final int LOCAL_VOICE_INTERACTION_STARTED = 154;
        public static final int LOW_MEMORY = 124;
        public static final int MULTI_WINDOW_MODE_CHANGED = 152;
        public static final int NEW_INTENT = 112;
        public static final int ON_NEW_ACTIVITY_OPTIONS = 146;
        public static final int PAUSE_ACTIVITY = 101;
        public static final int PAUSE_ACTIVITY_FINISHING = 102;
        public static final int PICTURE_IN_PICTURE_MODE_CHANGED = 153;
        public static final int PROFILER_CONTROL = 127;
        public static final int RECEIVER = 113;
        public static final int RELAUNCH_ACTIVITY = 126;
        public static final int REMOVE_PROVIDER = 131;
        public static final int REQUEST_ASSIST_CONTEXT_EXTRAS = 143;
        public static final int RESUME_ACTIVITY = 107;
        public static final int SCHEDULE_CRASH = 134;
        public static final int SEND_RESULT = 108;
        public static final int SERVICE_ARGS = 115;
        public static final int SET_CORE_SETTINGS = 138;
        public static final int SHOW_WINDOW = 105;
        public static final int SLEEPING = 137;
        public static final int START_BINDER_TRACKING = 150;
        public static final int STOP_ACTIVITY_HIDE = 104;
        public static final int STOP_ACTIVITY_SHOW = 103;
        public static final int STOP_BINDER_TRACKING_AND_DUMP = 151;
        public static final int STOP_SERVICE = 116;
        public static final int SUICIDE = 130;
        public static final int TRANSLUCENT_CONVERSION_COMPLETE = 144;
        public static final int TRIM_MEMORY = 140;
        public static final int UNBIND_SERVICE = 122;
        public static final int UNSTABLE_PROVIDER_DIED = 142;
        public static final int UPDATE_PACKAGE_COMPATIBILITY_INFO = 139;

        H(ActivityThread this$0, H h) {
            this();
        }

        private H() {
        }

        String codeToString(int code) {
            if (ActivityThread.DEBUG_MESSAGES || isDebuggableMessage(code)) {
                switch (code) {
                    case 100:
                        return "LAUNCH_ACTIVITY";
                    case 101:
                        return "PAUSE_ACTIVITY";
                    case 102:
                        return "PAUSE_ACTIVITY_FINISHING";
                    case 103:
                        return "STOP_ACTIVITY_SHOW";
                    case 104:
                        return "STOP_ACTIVITY_HIDE";
                    case 105:
                        return "SHOW_WINDOW";
                    case 106:
                        return "HIDE_WINDOW";
                    case 107:
                        return "RESUME_ACTIVITY";
                    case 108:
                        return "SEND_RESULT";
                    case 109:
                        return "DESTROY_ACTIVITY";
                    case 110:
                        return "BIND_APPLICATION";
                    case 111:
                        return "EXIT_APPLICATION";
                    case 112:
                        return "NEW_INTENT";
                    case 113:
                        return "RECEIVER";
                    case 114:
                        return "CREATE_SERVICE";
                    case 115:
                        return "SERVICE_ARGS";
                    case 116:
                        return "STOP_SERVICE";
                    case 118:
                        return "CONFIGURATION_CHANGED";
                    case 119:
                        return "CLEAN_UP_CONTEXT";
                    case 120:
                        return "GC_WHEN_IDLE";
                    case 121:
                        return "BIND_SERVICE";
                    case 122:
                        return "UNBIND_SERVICE";
                    case 123:
                        return "DUMP_SERVICE";
                    case 124:
                        return "LOW_MEMORY";
                    case 125:
                        return "ACTIVITY_CONFIGURATION_CHANGED";
                    case 126:
                        return "RELAUNCH_ACTIVITY";
                    case 127:
                        return "PROFILER_CONTROL";
                    case 128:
                        return "CREATE_BACKUP_AGENT";
                    case 129:
                        return "DESTROY_BACKUP_AGENT";
                    case 130:
                        return "SUICIDE";
                    case 131:
                        return "REMOVE_PROVIDER";
                    case 132:
                        return "ENABLE_JIT";
                    case 133:
                        return "DISPATCH_PACKAGE_BROADCAST";
                    case 134:
                        return "SCHEDULE_CRASH";
                    case 135:
                        return "DUMP_HEAP";
                    case 136:
                        return "DUMP_ACTIVITY";
                    case 137:
                        return "SLEEPING";
                    case 138:
                        return "SET_CORE_SETTINGS";
                    case 139:
                        return "UPDATE_PACKAGE_COMPATIBILITY_INFO";
                    case 140:
                        return "TRIM_MEMORY";
                    case 141:
                        return "DUMP_PROVIDER";
                    case 142:
                        return "UNSTABLE_PROVIDER_DIED";
                    case 143:
                        return "REQUEST_ASSIST_CONTEXT_EXTRAS";
                    case 144:
                        return "TRANSLUCENT_CONVERSION_COMPLETE";
                    case 145:
                        return "INSTALL_PROVIDER";
                    case 146:
                        return "ON_NEW_ACTIVITY_OPTIONS";
                    case 147:
                        return "CANCEL_VISIBLE_BEHIND";
                    case 148:
                        return "BACKGROUND_VISIBLE_BEHIND_CHANGED";
                    case 149:
                        return "ENTER_ANIMATION_COMPLETE";
                    case 152:
                        return "MULTI_WINDOW_MODE_CHANGED";
                    case 153:
                        return "PICTURE_IN_PICTURE_MODE_CHANGED";
                    case 154:
                        return "LOCAL_VOICE_INTERACTION_STARTED";
                }
            }
            return Integer.toString(code);
        }

        boolean isDebuggableMessage(int code) {
            if (ActivityThread.IS_USER_BUILD) {
                return false;
            }
            switch (code) {
            }
            return false;
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            if (ActivityThread.DEBUG_MESSAGES) {
                Slog.v(ActivityThread.TAG, ">>> handling: " + codeToString(msg.what));
            }
            switch (msg.what) {
                case 100:
                    Trace.traceBegin(64L, "activityStart");
                    ActivityClientRecord r = (ActivityClientRecord) msg.obj;
                    r.packageInfo = ActivityThread.this.getPackageInfoNoCheck(r.activityInfo.applicationInfo, r.compatInfo);
                    ActivityThread.this.handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
                    Trace.traceEnd(64L);
                    break;
                case 101:
                    Trace.traceBegin(64L, "activityPause");
                    SomeArgs args = (SomeArgs) msg.obj;
                    ActivityThread.this.handlePauseActivity((IBinder) args.arg1, false, (args.argi1 & 1) != 0, args.argi2, (args.argi1 & 2) != 0, args.argi3);
                    maybeSnapshot();
                    Trace.traceEnd(64L);
                    break;
                case 102:
                    Trace.traceBegin(64L, "activityPause");
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    ActivityThread.this.handlePauseActivity((IBinder) args2.arg1, true, (args2.argi1 & 1) != 0, args2.argi2, (args2.argi1 & 2) != 0, args2.argi3);
                    Trace.traceEnd(64L);
                    break;
                case 103:
                    Trace.traceBegin(64L, "activityStop");
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    ActivityThread.this.handleStopActivity((IBinder) args3.arg1, true, args3.argi2, args3.argi3);
                    Trace.traceEnd(64L);
                    break;
                case 104:
                    Trace.traceBegin(64L, "activityStop");
                    SomeArgs args4 = (SomeArgs) msg.obj;
                    ActivityThread.this.handleStopActivity((IBinder) args4.arg1, false, args4.argi2, args4.argi3);
                    Trace.traceEnd(64L);
                    break;
                case 105:
                    Trace.traceBegin(64L, "activityShowWindow");
                    ActivityThread.this.handleWindowVisibility((IBinder) msg.obj, true);
                    Trace.traceEnd(64L);
                    break;
                case 106:
                    Trace.traceBegin(64L, "activityHideWindow");
                    ActivityThread.this.handleWindowVisibility((IBinder) msg.obj, false);
                    Trace.traceEnd(64L);
                    break;
                case 107:
                    Trace.traceBegin(64L, "activityResume");
                    SomeArgs args5 = (SomeArgs) msg.obj;
                    ActivityThread.this.handleResumeActivity((IBinder) args5.arg1, true, args5.argi1 != 0, true, args5.argi3, "RESUME_ACTIVITY");
                    Trace.traceEnd(64L);
                    break;
                case 108:
                    Trace.traceBegin(64L, "activityDeliverResult");
                    ActivityThread.this.handleSendResult((ResultData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 109:
                    Trace.traceBegin(64L, "activityDestroy");
                    ActivityThread.this.handleDestroyActivity((IBinder) msg.obj, msg.arg1 != 0, msg.arg2, false);
                    Trace.traceEnd(64L);
                    break;
                case 110:
                    Trace.traceBegin(64L, "bindApplication");
                    AppBindData data = (AppBindData) msg.obj;
                    ActivityThread.this.handleBindApplication(data);
                    Trace.traceEnd(64L);
                    break;
                case 111:
                    if (ActivityThread.this.mInitialApplication != null) {
                        ActivityThread.this.mInitialApplication.onTerminate();
                    }
                    Looper.myLooper().quit();
                    break;
                case 112:
                    Trace.traceBegin(64L, "activityNewIntent");
                    ActivityThread.this.handleNewIntent((NewIntentData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 113:
                    Trace.traceBegin(64L, "broadcastReceiveComp");
                    ActivityThread.this.handleReceiver((ReceiverData) msg.obj);
                    maybeSnapshot();
                    Trace.traceEnd(64L);
                    break;
                case 114:
                    Trace.traceBegin(64L, "serviceCreate: " + String.valueOf(msg.obj));
                    ActivityThread.this.handleCreateService((CreateServiceData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 115:
                    Trace.traceBegin(64L, "serviceStart: " + String.valueOf(msg.obj));
                    ActivityThread.this.handleServiceArgs((ServiceArgsData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 116:
                    Trace.traceBegin(64L, "serviceStop");
                    ActivityThread.this.handleStopService((IBinder) msg.obj);
                    maybeSnapshot();
                    Trace.traceEnd(64L);
                    break;
                case 118:
                    Trace.traceBegin(64L, "configChanged");
                    ActivityThread.this.mCurDefaultDisplayDpi = ((Configuration) msg.obj).densityDpi;
                    ActivityThread.this.handleConfigurationChanged((Configuration) msg.obj, null);
                    Trace.traceEnd(64L);
                    break;
                case 119:
                    ContextCleanupInfo cci = (ContextCleanupInfo) msg.obj;
                    cci.context.performFinalCleanup(cci.who, cci.what);
                    break;
                case 120:
                    ActivityThread.this.scheduleGcIdler();
                    break;
                case 121:
                    Trace.traceBegin(64L, "serviceBind");
                    ActivityThread.this.handleBindService((BindServiceData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 122:
                    Trace.traceBegin(64L, "serviceUnbind");
                    ActivityThread.this.handleUnbindService((BindServiceData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 123:
                    ActivityThread.this.handleDumpService((DumpComponentInfo) msg.obj);
                    break;
                case 124:
                    Trace.traceBegin(64L, "lowMemory");
                    ActivityThread.this.handleLowMemory();
                    Trace.traceEnd(64L);
                    break;
                case 125:
                    Trace.traceBegin(64L, "activityConfigChanged");
                    ActivityThread.this.handleActivityConfigurationChanged((ActivityConfigChangeData) msg.obj, msg.arg1 == 1);
                    Trace.traceEnd(64L);
                    break;
                case 126:
                    Trace.traceBegin(64L, "activityRestart");
                    ActivityThread.this.handleRelaunchActivity((ActivityClientRecord) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 127:
                    ActivityThread.this.handleProfilerControl(msg.arg1 != 0, (ProfilerInfo) msg.obj, msg.arg2);
                    break;
                case 128:
                    Trace.traceBegin(64L, "backupCreateAgent");
                    ActivityThread.this.handleCreateBackupAgent((CreateBackupAgentData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 129:
                    Trace.traceBegin(64L, "backupDestroyAgent");
                    ActivityThread.this.handleDestroyBackupAgent((CreateBackupAgentData) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 130:
                    Process.killProcess(Process.myPid());
                    break;
                case 131:
                    Trace.traceBegin(64L, "providerRemove");
                    ActivityThread.this.completeRemoveProvider((ProviderRefCount) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 132:
                    ActivityThread.this.ensureJitEnabled();
                    break;
                case 133:
                    Trace.traceBegin(64L, "broadcastPackage");
                    ActivityThread.this.handleDispatchPackageBroadcast(msg.arg1, (String[]) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 134:
                    throw new RemoteServiceException((String) msg.obj);
                case 135:
                    ActivityThread.handleDumpHeap(msg.arg1 != 0, (DumpHeapData) msg.obj);
                    break;
                case 136:
                    ActivityThread.this.handleDumpActivity((DumpComponentInfo) msg.obj);
                    break;
                case 137:
                    Trace.traceBegin(64L, "sleeping");
                    ActivityThread.this.handleSleeping((IBinder) msg.obj, msg.arg1 != 0);
                    Trace.traceEnd(64L);
                    break;
                case 138:
                    Trace.traceBegin(64L, "setCoreSettings");
                    ActivityThread.this.handleSetCoreSettings((Bundle) msg.obj);
                    Trace.traceEnd(64L);
                    break;
                case 139:
                    ActivityThread.this.handleUpdatePackageCompatibilityInfo((UpdateCompatibilityData) msg.obj);
                    break;
                case 140:
                    Trace.traceBegin(64L, "trimMemory");
                    ActivityThread.this.handleTrimMemory(msg.arg1);
                    Trace.traceEnd(64L);
                    break;
                case 141:
                    ActivityThread.this.handleDumpProvider((DumpComponentInfo) msg.obj);
                    break;
                case 142:
                    ActivityThread.this.handleUnstableProviderDied((IBinder) msg.obj, false);
                    break;
                case 143:
                    ActivityThread.this.handleRequestAssistContextExtras((RequestAssistContextExtras) msg.obj);
                    break;
                case 144:
                    ActivityThread.this.handleTranslucentConversionComplete((IBinder) msg.obj, msg.arg1 == 1);
                    break;
                case 145:
                    ActivityThread.this.handleInstallProvider((ProviderInfo) msg.obj);
                    break;
                case 146:
                    Pair<IBinder, ActivityOptions> pair = (Pair) msg.obj;
                    ActivityThread.this.onNewActivityOptions((IBinder) pair.first, (ActivityOptions) pair.second);
                    break;
                case 147:
                    ActivityThread.this.handleCancelVisibleBehind((IBinder) msg.obj);
                    break;
                case 148:
                    ActivityThread.this.handleOnBackgroundVisibleBehindChanged((IBinder) msg.obj, msg.arg1 > 0);
                    break;
                case 149:
                    ActivityThread.this.handleEnterAnimationComplete((IBinder) msg.obj);
                    break;
                case 150:
                    ActivityThread.this.handleStartBinderTracking();
                    break;
                case 151:
                    ActivityThread.this.handleStopBinderTrackingAndDump((ParcelFileDescriptor) msg.obj);
                    break;
                case 152:
                    ActivityThread.this.handleMultiWindowModeChanged((IBinder) msg.obj, msg.arg1 == 1);
                    break;
                case 153:
                    ActivityThread.this.handlePictureInPictureModeChanged((IBinder) msg.obj, msg.arg1 == 1);
                    break;
                case 154:
                    ActivityThread.this.handleLocalVoiceInteractionStarted((IBinder) ((SomeArgs) msg.obj).arg1, (IVoiceInteractor) ((SomeArgs) msg.obj).arg2);
                    break;
            }
            Object obj = msg.obj;
            if (obj instanceof SomeArgs) {
                ((SomeArgs) obj).recycle();
            }
            if (ActivityThread.DEBUG_MESSAGES) {
                Slog.v(ActivityThread.TAG, "<<< done: " + codeToString(msg.what));
            }
            if (ActivityThread.DEBUG_MESSAGES || isDebuggableMessage(msg.what)) {
                Slog.d(ActivityThread.TAG, codeToString(msg.what) + " handled : " + msg.arg1 + " / " + msg.obj);
            }
        }

        private void maybeSnapshot() {
            Context context;
            if (ActivityThread.this.mBoundApplication == null || !SamplingProfilerIntegration.isEnabled()) {
                return;
            }
            String packageName = ActivityThread.this.mBoundApplication.info.mPackageName;
            PackageInfo packageInfo = null;
            try {
                context = ActivityThread.this.getSystemContext();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(ActivityThread.TAG, "cannot get package info for " + packageName, e);
            }
            if (context == null) {
                Log.e(ActivityThread.TAG, "cannot get a valid context");
                return;
            }
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                Log.e(ActivityThread.TAG, "cannot get a valid PackageManager");
            } else {
                packageInfo = pm.getPackageInfo(packageName, 1);
                SamplingProfilerIntegration.writeSnapshot(ActivityThread.this.mBoundApplication.processName, packageInfo);
            }
        }
    }

    private class Idler implements MessageQueue.IdleHandler {
        Idler(ActivityThread this$0, Idler idler) {
            this();
        }

        private Idler() {
        }

        @Override
        public final boolean queueIdle() {
            ActivityClientRecord a = ActivityThread.this.mNewActivities;
            boolean stopProfiling = false;
            if (ActivityThread.this.mBoundApplication != null && ActivityThread.this.mProfiler.profileFd != null && ActivityThread.this.mProfiler.autoStopProfiler) {
                stopProfiling = true;
            }
            if (a != null) {
                ActivityThread.this.mNewActivities = null;
                IActivityManager am = ActivityManagerNative.getDefault();
                do {
                    if (ActivityThread.localLOGV) {
                        Slog.v(ActivityThread.TAG, "Reporting idle of " + a + " finished=" + (a.activity != null ? a.activity.mFinished : false));
                    }
                    if (a.activity != null && !a.activity.mFinished) {
                        try {
                            am.activityIdle(a.token, a.createdConfig, stopProfiling);
                            a.createdConfig = null;
                        } catch (RemoteException ex) {
                            throw ex.rethrowFromSystemServer();
                        }
                    }
                    ActivityClientRecord prev = a;
                    a = a.nextIdle;
                    prev.nextIdle = null;
                } while (a != null);
            }
            if (stopProfiling) {
                ActivityThread.this.mProfiler.stopProfiling();
            }
            ActivityThread.this.ensureJitEnabled();
            return false;
        }
    }

    final class GcIdler implements MessageQueue.IdleHandler {
        GcIdler() {
        }

        @Override
        public final boolean queueIdle() {
            ActivityThread.this.doGcIfNeeded();
            return false;
        }
    }

    public static ActivityThread currentActivityThread() {
        return sCurrentActivityThread;
    }

    public static boolean isSystem() {
        if (sCurrentActivityThread != null) {
            return sCurrentActivityThread.mSystemThread;
        }
        return false;
    }

    public static String currentOpPackageName() {
        ActivityThread am = currentActivityThread();
        if (am == null || am.getApplication() == null) {
            return null;
        }
        return am.getApplication().getOpPackageName();
    }

    public static String currentPackageName() {
        ActivityThread am = currentActivityThread();
        if (am == null || am.mBoundApplication == null) {
            return null;
        }
        return am.mBoundApplication.appInfo.packageName;
    }

    public static String currentProcessName() {
        ActivityThread am = currentActivityThread();
        if (am == null || am.mBoundApplication == null) {
            return null;
        }
        return am.mBoundApplication.processName;
    }

    public static Application currentApplication() {
        ActivityThread am = currentActivityThread();
        if (am != null) {
            return am.mInitialApplication;
        }
        return null;
    }

    public static IPackageManager getPackageManager() {
        if (sPackageManager != null) {
            return sPackageManager;
        }
        IBinder b = ServiceManager.getService("package");
        sPackageManager = IPackageManager.Stub.asInterface(b);
        return sPackageManager;
    }

    Configuration applyConfigCompatMainThread(int displayDensity, Configuration config, CompatibilityInfo compat) {
        if (config == null) {
            return null;
        }
        if (!compat.supportsScreen()) {
            this.mMainThreadConfig.setTo(config);
            Configuration config2 = this.mMainThreadConfig;
            compat.applyToConfiguration(displayDensity, config2);
            return config2;
        }
        return config;
    }

    Resources getTopLevelResources(String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, LoadedApk pkgInfo) {
        return this.mResourcesManager.getResources(null, resDir, splitResDirs, overlayDirs, libDirs, displayId, null, pkgInfo.getCompatibilityInfo(), pkgInfo.getClassLoader());
    }

    final Handler getHandler() {
        return this.mH;
    }

    public final LoadedApk getPackageInfo(String packageName, CompatibilityInfo compatInfo, int flags) {
        return getPackageInfo(packageName, compatInfo, flags, UserHandle.myUserId());
    }

    public final LoadedApk getPackageInfo(String packageName, CompatibilityInfo compatInfo, int flags, int userId) {
        boolean differentUser = UserHandle.myUserId() != userId;
        synchronized (this.mResourcesManager) {
            WeakReference<LoadedApk> weakReference = differentUser ? null : (flags & 1) != 0 ? this.mPackages.get(packageName) : this.mResourcePackages.get(packageName);
            LoadedApk packageInfo = weakReference != null ? weakReference.get() : null;
            if (packageInfo != null && (packageInfo.mResources == null || packageInfo.mResources.getAssets().isUpToDate())) {
                if (packageInfo.isSecurityViolation() && (flags & 2) == 0) {
                    throw new SecurityException("Requesting code from " + packageName + " to be run in process " + this.mBoundApplication.processName + "/" + this.mBoundApplication.appInfo.uid);
                }
                return packageInfo;
            }
            try {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 268436480, userId);
                if (ai != null) {
                    return getPackageInfo(ai, compatInfo, flags);
                }
                return null;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public final LoadedApk getPackageInfo(ApplicationInfo ai, CompatibilityInfo compatInfo, int flags) {
        boolean includeCode = (flags & 1) != 0;
        boolean securityViolation = (!includeCode || ai.uid == 0 || ai.uid == 1000) ? false : this.mBoundApplication == null || !UserHandle.isSameApp(ai.uid, this.mBoundApplication.appInfo.uid);
        boolean registerPackage = includeCode && (1073741824 & flags) != 0;
        if ((flags & 3) != 1 || !securityViolation) {
            return getPackageInfo(ai, compatInfo, null, securityViolation, includeCode, registerPackage);
        }
        String msg = "Requesting code from " + ai.packageName + " (with uid " + ai.uid + ")";
        if (this.mBoundApplication != null) {
            msg = msg + " to be run in process " + this.mBoundApplication.processName + " (with uid " + this.mBoundApplication.appInfo.uid + ")";
        }
        throw new SecurityException(msg);
    }

    public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai, CompatibilityInfo compatInfo) {
        return getPackageInfo(ai, compatInfo, null, false, true, false);
    }

    public final LoadedApk peekPackageInfo(String packageName, boolean includeCode) {
        WeakReference<LoadedApk> ref;
        LoadedApk loadedApk;
        synchronized (this.mResourcesManager) {
            if (includeCode) {
                ref = this.mPackages.get(packageName);
            } else {
                ref = this.mResourcePackages.get(packageName);
            }
            loadedApk = ref != null ? ref.get() : null;
        }
        return loadedApk;
    }

    private LoadedApk getPackageInfo(ApplicationInfo aInfo, CompatibilityInfo compatInfo, ClassLoader baseLoader, boolean securityViolation, boolean includeCode, boolean registerPackage) {
        LoadedApk packageInfo;
        boolean differentUser = UserHandle.myUserId() != UserHandle.getUserId(aInfo.uid);
        synchronized (this.mResourcesManager) {
            WeakReference<LoadedApk> weakReference = differentUser ? null : includeCode ? this.mPackages.get(aInfo.packageName) : this.mResourcePackages.get(aInfo.packageName);
            packageInfo = weakReference != null ? weakReference.get() : null;
            if (packageInfo == null || (packageInfo.mResources != null && !packageInfo.mResources.getAssets().isUpToDate())) {
                if (localLOGV) {
                    Slog.v(TAG, (includeCode ? "Loading code package " : "Loading resource-only package ") + aInfo.packageName + " (in " + (this.mBoundApplication != null ? this.mBoundApplication.processName : null) + ")");
                }
                boolean z = includeCode && (aInfo.flags & 4) != 0;
                packageInfo = new LoadedApk(this, aInfo, compatInfo, baseLoader, securityViolation, z, registerPackage);
                if (this.mSystemThread && ZenModeConfig.SYSTEM_AUTHORITY.equals(aInfo.packageName)) {
                    packageInfo.installSystemApplicationInfo(aInfo, getSystemContext().mPackageInfo.getClassLoader());
                }
                if (!differentUser) {
                    if (includeCode) {
                        this.mPackages.put(aInfo.packageName, new WeakReference<>(packageInfo));
                    } else {
                        this.mResourcePackages.put(aInfo.packageName, new WeakReference<>(packageInfo));
                    }
                }
            }
        }
        return packageInfo;
    }

    ActivityThread() {
        configActivityLogTag();
        this.mResourcesManager = ResourcesManager.getInstance();
    }

    public ApplicationThread getApplicationThread() {
        return this.mAppThread;
    }

    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
    }

    public boolean isProfiling() {
        return (this.mProfiler == null || this.mProfiler.profileFile == null || this.mProfiler.profileFd != null) ? false : true;
    }

    public String getProfileFilePath() {
        return this.mProfiler.profileFile;
    }

    public Looper getLooper() {
        return this.mLooper;
    }

    public Application getApplication() {
        return this.mInitialApplication;
    }

    public String getProcessName() {
        return this.mBoundApplication.processName;
    }

    public ContextImpl getSystemContext() {
        ContextImpl contextImpl;
        synchronized (this) {
            if (this.mSystemContext == null) {
                this.mSystemContext = ContextImpl.createSystemContext(this);
            }
            contextImpl = this.mSystemContext;
        }
        return contextImpl;
    }

    public void installSystemApplicationInfo(ApplicationInfo info, ClassLoader classLoader) {
        synchronized (this) {
            getSystemContext().installSystemApplicationInfo(info, classLoader);
            this.mProfiler = new Profiler();
        }
    }

    void ensureJitEnabled() {
        if (this.mJitEnabled) {
            return;
        }
        this.mJitEnabled = true;
        VMRuntime.getRuntime().startJitCompilation();
    }

    void scheduleGcIdler() {
        if (!this.mGcIdlerScheduled) {
            this.mGcIdlerScheduled = true;
            Looper.myQueue().addIdleHandler(this.mGcIdler);
        }
        this.mH.removeMessages(120);
    }

    void unscheduleGcIdler() {
        if (this.mGcIdlerScheduled) {
            this.mGcIdlerScheduled = false;
            Looper.myQueue().removeIdleHandler(this.mGcIdler);
        }
        this.mH.removeMessages(120);
    }

    void doGcIfNeeded() {
        this.mGcIdlerScheduled = false;
        long now = SystemClock.uptimeMillis();
        if (BinderInternal.getLastGcTime() + MIN_TIME_BETWEEN_GCS >= now) {
            return;
        }
        BinderInternal.forceGc("bg");
    }

    static void printRow(PrintWriter pw, String format, Object... objs) {
        pw.println(String.format(format, objs));
    }

    public static void dumpMemInfoTable(PrintWriter pw, Debug.MemoryInfo memInfo, boolean checkin, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, int pid, String processName, long nativeMax, long nativeAllocated, long nativeFree, long dalvikMax, long dalvikAllocated, long dalvikFree) {
        if (checkin) {
            pw.print(4);
            pw.print(',');
            pw.print(pid);
            pw.print(',');
            pw.print(processName);
            pw.print(',');
            pw.print(nativeMax);
            pw.print(',');
            pw.print(dalvikMax);
            pw.print(',');
            pw.print("N/A,");
            pw.print(nativeMax + dalvikMax);
            pw.print(',');
            pw.print(nativeAllocated);
            pw.print(',');
            pw.print(dalvikAllocated);
            pw.print(',');
            pw.print("N/A,");
            pw.print(nativeAllocated + dalvikAllocated);
            pw.print(',');
            pw.print(nativeFree);
            pw.print(',');
            pw.print(dalvikFree);
            pw.print(',');
            pw.print("N/A,");
            pw.print(nativeFree + dalvikFree);
            pw.print(',');
            pw.print(memInfo.nativePss);
            pw.print(',');
            pw.print(memInfo.dalvikPss);
            pw.print(',');
            pw.print(memInfo.otherPss);
            pw.print(',');
            pw.print(memInfo.getTotalPss());
            pw.print(',');
            pw.print(memInfo.nativeSwappablePss);
            pw.print(',');
            pw.print(memInfo.dalvikSwappablePss);
            pw.print(',');
            pw.print(memInfo.otherSwappablePss);
            pw.print(',');
            pw.print(memInfo.getTotalSwappablePss());
            pw.print(',');
            pw.print(memInfo.nativeSharedDirty);
            pw.print(',');
            pw.print(memInfo.dalvikSharedDirty);
            pw.print(',');
            pw.print(memInfo.otherSharedDirty);
            pw.print(',');
            pw.print(memInfo.getTotalSharedDirty());
            pw.print(',');
            pw.print(memInfo.nativeSharedClean);
            pw.print(',');
            pw.print(memInfo.dalvikSharedClean);
            pw.print(',');
            pw.print(memInfo.otherSharedClean);
            pw.print(',');
            pw.print(memInfo.getTotalSharedClean());
            pw.print(',');
            pw.print(memInfo.nativePrivateDirty);
            pw.print(',');
            pw.print(memInfo.dalvikPrivateDirty);
            pw.print(',');
            pw.print(memInfo.otherPrivateDirty);
            pw.print(',');
            pw.print(memInfo.getTotalPrivateDirty());
            pw.print(',');
            pw.print(memInfo.nativePrivateClean);
            pw.print(',');
            pw.print(memInfo.dalvikPrivateClean);
            pw.print(',');
            pw.print(memInfo.otherPrivateClean);
            pw.print(',');
            pw.print(memInfo.getTotalPrivateClean());
            pw.print(',');
            pw.print(memInfo.nativeSwappedOut);
            pw.print(',');
            pw.print(memInfo.dalvikSwappedOut);
            pw.print(',');
            pw.print(memInfo.otherSwappedOut);
            pw.print(',');
            pw.print(memInfo.getTotalSwappedOut());
            pw.print(',');
            if (memInfo.hasSwappedOutPss) {
                pw.print(memInfo.nativeSwappedOutPss);
                pw.print(',');
                pw.print(memInfo.dalvikSwappedOutPss);
                pw.print(',');
                pw.print(memInfo.otherSwappedOutPss);
                pw.print(',');
                pw.print(memInfo.getTotalSwappedOutPss());
                pw.print(',');
            } else {
                pw.print("N/A,");
                pw.print("N/A,");
                pw.print("N/A,");
                pw.print("N/A,");
            }
            for (int i = 0; i < 17; i++) {
                pw.print(Debug.MemoryInfo.getOtherLabel(i));
                pw.print(',');
                pw.print(memInfo.getOtherPss(i));
                pw.print(',');
                pw.print(memInfo.getOtherSwappablePss(i));
                pw.print(',');
                pw.print(memInfo.getOtherSharedDirty(i));
                pw.print(',');
                pw.print(memInfo.getOtherSharedClean(i));
                pw.print(',');
                pw.print(memInfo.getOtherPrivateDirty(i));
                pw.print(',');
                pw.print(memInfo.getOtherPrivateClean(i));
                pw.print(',');
                pw.print(memInfo.getOtherSwappedOut(i));
                pw.print(',');
                if (memInfo.hasSwappedOutPss) {
                    pw.print(memInfo.getOtherSwappedOutPss(i));
                    pw.print(',');
                } else {
                    pw.print("N/A,");
                }
            }
            return;
        }
        if (!dumpSummaryOnly) {
            if (dumpFullInfo) {
                Object[] objArr = new Object[11];
                objArr[0] = ProxyInfo.LOCAL_EXCL_LIST;
                objArr[1] = "Pss";
                objArr[2] = "Pss";
                objArr[3] = "Shared";
                objArr[4] = "Private";
                objArr[5] = "Shared";
                objArr[6] = "Private";
                objArr[7] = memInfo.hasSwappedOutPss ? "SwapPss" : "Swap";
                objArr[8] = "Heap";
                objArr[9] = "Heap";
                objArr[10] = "Heap";
                printRow(pw, HEAP_FULL_COLUMN, objArr);
                printRow(pw, HEAP_FULL_COLUMN, ProxyInfo.LOCAL_EXCL_LIST, "Total", "Clean", "Dirty", "Dirty", "Clean", "Clean", "Dirty", "Size", "Alloc", "Free");
                printRow(pw, HEAP_FULL_COLUMN, ProxyInfo.LOCAL_EXCL_LIST, "------", "------", "------", "------", "------", "------", "------", "------", "------", "------");
                Object[] objArr2 = new Object[11];
                objArr2[0] = "Native Heap";
                objArr2[1] = Integer.valueOf(memInfo.nativePss);
                objArr2[2] = Integer.valueOf(memInfo.nativeSwappablePss);
                objArr2[3] = Integer.valueOf(memInfo.nativeSharedDirty);
                objArr2[4] = Integer.valueOf(memInfo.nativePrivateDirty);
                objArr2[5] = Integer.valueOf(memInfo.nativeSharedClean);
                objArr2[6] = Integer.valueOf(memInfo.nativePrivateClean);
                objArr2[7] = Integer.valueOf(memInfo.hasSwappedOutPss ? memInfo.nativeSwappedOut : memInfo.nativeSwappedOutPss);
                objArr2[8] = Long.valueOf(nativeMax);
                objArr2[9] = Long.valueOf(nativeAllocated);
                objArr2[10] = Long.valueOf(nativeFree);
                printRow(pw, HEAP_FULL_COLUMN, objArr2);
                Object[] objArr3 = new Object[11];
                objArr3[0] = "Dalvik Heap";
                objArr3[1] = Integer.valueOf(memInfo.dalvikPss);
                objArr3[2] = Integer.valueOf(memInfo.dalvikSwappablePss);
                objArr3[3] = Integer.valueOf(memInfo.dalvikSharedDirty);
                objArr3[4] = Integer.valueOf(memInfo.dalvikPrivateDirty);
                objArr3[5] = Integer.valueOf(memInfo.dalvikSharedClean);
                objArr3[6] = Integer.valueOf(memInfo.dalvikPrivateClean);
                objArr3[7] = Integer.valueOf(memInfo.hasSwappedOutPss ? memInfo.dalvikSwappedOut : memInfo.dalvikSwappedOutPss);
                objArr3[8] = Long.valueOf(dalvikMax);
                objArr3[9] = Long.valueOf(dalvikAllocated);
                objArr3[10] = Long.valueOf(dalvikFree);
                printRow(pw, HEAP_FULL_COLUMN, objArr3);
            } else {
                Object[] objArr4 = new Object[8];
                objArr4[0] = ProxyInfo.LOCAL_EXCL_LIST;
                objArr4[1] = "Pss";
                objArr4[2] = "Private";
                objArr4[3] = "Private";
                objArr4[4] = memInfo.hasSwappedOutPss ? "SwapPss" : "Swap";
                objArr4[5] = "Heap";
                objArr4[6] = "Heap";
                objArr4[7] = "Heap";
                printRow(pw, HEAP_COLUMN, objArr4);
                printRow(pw, HEAP_COLUMN, ProxyInfo.LOCAL_EXCL_LIST, "Total", "Dirty", "Clean", "Dirty", "Size", "Alloc", "Free");
                printRow(pw, HEAP_COLUMN, ProxyInfo.LOCAL_EXCL_LIST, "------", "------", "------", "------", "------", "------", "------", "------");
                Object[] objArr5 = new Object[8];
                objArr5[0] = "Native Heap";
                objArr5[1] = Integer.valueOf(memInfo.nativePss);
                objArr5[2] = Integer.valueOf(memInfo.nativePrivateDirty);
                objArr5[3] = Integer.valueOf(memInfo.nativePrivateClean);
                objArr5[4] = Integer.valueOf(memInfo.hasSwappedOutPss ? memInfo.nativeSwappedOutPss : memInfo.nativeSwappedOut);
                objArr5[5] = Long.valueOf(nativeMax);
                objArr5[6] = Long.valueOf(nativeAllocated);
                objArr5[7] = Long.valueOf(nativeFree);
                printRow(pw, HEAP_COLUMN, objArr5);
                Object[] objArr6 = new Object[8];
                objArr6[0] = "Dalvik Heap";
                objArr6[1] = Integer.valueOf(memInfo.dalvikPss);
                objArr6[2] = Integer.valueOf(memInfo.dalvikPrivateDirty);
                objArr6[3] = Integer.valueOf(memInfo.dalvikPrivateClean);
                objArr6[4] = Integer.valueOf(memInfo.hasSwappedOutPss ? memInfo.dalvikSwappedOutPss : memInfo.dalvikSwappedOut);
                objArr6[5] = Long.valueOf(dalvikMax);
                objArr6[6] = Long.valueOf(dalvikAllocated);
                objArr6[7] = Long.valueOf(dalvikFree);
                printRow(pw, HEAP_COLUMN, objArr6);
            }
            int otherPss = memInfo.otherPss;
            int otherSwappablePss = memInfo.otherSwappablePss;
            int otherSharedDirty = memInfo.otherSharedDirty;
            int otherPrivateDirty = memInfo.otherPrivateDirty;
            int otherSharedClean = memInfo.otherSharedClean;
            int otherPrivateClean = memInfo.otherPrivateClean;
            int otherSwappedOut = memInfo.otherSwappedOut;
            int otherSwappedOutPss = memInfo.otherSwappedOutPss;
            for (int i2 = 0; i2 < 17; i2++) {
                int myPss = memInfo.getOtherPss(i2);
                int mySwappablePss = memInfo.getOtherSwappablePss(i2);
                int mySharedDirty = memInfo.getOtherSharedDirty(i2);
                int myPrivateDirty = memInfo.getOtherPrivateDirty(i2);
                int mySharedClean = memInfo.getOtherSharedClean(i2);
                int myPrivateClean = memInfo.getOtherPrivateClean(i2);
                int mySwappedOut = memInfo.getOtherSwappedOut(i2);
                int mySwappedOutPss = memInfo.getOtherSwappedOutPss(i2);
                if (myPss == 0 && mySharedDirty == 0 && myPrivateDirty == 0 && mySharedClean == 0 && myPrivateClean == 0) {
                    if ((memInfo.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut) != 0) {
                    }
                } else {
                    if (dumpFullInfo) {
                        Object[] objArr7 = new Object[11];
                        objArr7[0] = Debug.MemoryInfo.getOtherLabel(i2);
                        objArr7[1] = Integer.valueOf(myPss);
                        objArr7[2] = Integer.valueOf(mySwappablePss);
                        objArr7[3] = Integer.valueOf(mySharedDirty);
                        objArr7[4] = Integer.valueOf(myPrivateDirty);
                        objArr7[5] = Integer.valueOf(mySharedClean);
                        objArr7[6] = Integer.valueOf(myPrivateClean);
                        objArr7[7] = Integer.valueOf(memInfo.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut);
                        objArr7[8] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr7[9] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr7[10] = ProxyInfo.LOCAL_EXCL_LIST;
                        printRow(pw, HEAP_FULL_COLUMN, objArr7);
                    } else {
                        Object[] objArr8 = new Object[8];
                        objArr8[0] = Debug.MemoryInfo.getOtherLabel(i2);
                        objArr8[1] = Integer.valueOf(myPss);
                        objArr8[2] = Integer.valueOf(myPrivateDirty);
                        objArr8[3] = Integer.valueOf(myPrivateClean);
                        objArr8[4] = Integer.valueOf(memInfo.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut);
                        objArr8[5] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr8[6] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr8[7] = ProxyInfo.LOCAL_EXCL_LIST;
                        printRow(pw, HEAP_COLUMN, objArr8);
                    }
                    otherPss -= myPss;
                    otherSwappablePss -= mySwappablePss;
                    otherSharedDirty -= mySharedDirty;
                    otherPrivateDirty -= myPrivateDirty;
                    otherSharedClean -= mySharedClean;
                    otherPrivateClean -= myPrivateClean;
                    otherSwappedOut -= mySwappedOut;
                    otherSwappedOutPss -= mySwappedOutPss;
                }
            }
            if (dumpFullInfo) {
                Object[] objArr9 = new Object[11];
                objArr9[0] = "Unknown";
                objArr9[1] = Integer.valueOf(otherPss);
                objArr9[2] = Integer.valueOf(otherSwappablePss);
                objArr9[3] = Integer.valueOf(otherSharedDirty);
                objArr9[4] = Integer.valueOf(otherPrivateDirty);
                objArr9[5] = Integer.valueOf(otherSharedClean);
                objArr9[6] = Integer.valueOf(otherPrivateClean);
                if (!memInfo.hasSwappedOutPss) {
                    otherSwappedOutPss = otherSwappedOut;
                }
                objArr9[7] = Integer.valueOf(otherSwappedOutPss);
                objArr9[8] = ProxyInfo.LOCAL_EXCL_LIST;
                objArr9[9] = ProxyInfo.LOCAL_EXCL_LIST;
                objArr9[10] = ProxyInfo.LOCAL_EXCL_LIST;
                printRow(pw, HEAP_FULL_COLUMN, objArr9);
                Object[] objArr10 = new Object[11];
                objArr10[0] = "TOTAL";
                objArr10[1] = Integer.valueOf(memInfo.getTotalPss());
                objArr10[2] = Integer.valueOf(memInfo.getTotalSwappablePss());
                objArr10[3] = Integer.valueOf(memInfo.getTotalSharedDirty());
                objArr10[4] = Integer.valueOf(memInfo.getTotalPrivateDirty());
                objArr10[5] = Integer.valueOf(memInfo.getTotalSharedClean());
                objArr10[6] = Integer.valueOf(memInfo.getTotalPrivateClean());
                objArr10[7] = Integer.valueOf(memInfo.hasSwappedOutPss ? memInfo.getTotalSwappedOutPss() : memInfo.getTotalSwappedOut());
                objArr10[8] = Long.valueOf(nativeMax + dalvikMax);
                objArr10[9] = Long.valueOf(nativeAllocated + dalvikAllocated);
                objArr10[10] = Long.valueOf(nativeFree + dalvikFree);
                printRow(pw, HEAP_FULL_COLUMN, objArr10);
            } else {
                Object[] objArr11 = new Object[8];
                objArr11[0] = "Unknown";
                objArr11[1] = Integer.valueOf(otherPss);
                objArr11[2] = Integer.valueOf(otherPrivateDirty);
                objArr11[3] = Integer.valueOf(otherPrivateClean);
                if (!memInfo.hasSwappedOutPss) {
                    otherSwappedOutPss = otherSwappedOut;
                }
                objArr11[4] = Integer.valueOf(otherSwappedOutPss);
                objArr11[5] = ProxyInfo.LOCAL_EXCL_LIST;
                objArr11[6] = ProxyInfo.LOCAL_EXCL_LIST;
                objArr11[7] = ProxyInfo.LOCAL_EXCL_LIST;
                printRow(pw, HEAP_COLUMN, objArr11);
                Object[] objArr12 = new Object[8];
                objArr12[0] = "TOTAL";
                objArr12[1] = Integer.valueOf(memInfo.getTotalPss());
                objArr12[2] = Integer.valueOf(memInfo.getTotalPrivateDirty());
                objArr12[3] = Integer.valueOf(memInfo.getTotalPrivateClean());
                objArr12[4] = Integer.valueOf(memInfo.hasSwappedOutPss ? memInfo.getTotalSwappedOutPss() : memInfo.getTotalSwappedOut());
                objArr12[5] = Long.valueOf(nativeMax + dalvikMax);
                objArr12[6] = Long.valueOf(nativeAllocated + dalvikAllocated);
                objArr12[7] = Long.valueOf(nativeFree + dalvikFree);
                printRow(pw, HEAP_COLUMN, objArr12);
            }
            if (dumpDalvik) {
                pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                pw.println(" Dalvik Details");
                for (int i3 = 17; i3 < 25; i3++) {
                    int myPss2 = memInfo.getOtherPss(i3);
                    int mySwappablePss2 = memInfo.getOtherSwappablePss(i3);
                    int mySharedDirty2 = memInfo.getOtherSharedDirty(i3);
                    int myPrivateDirty2 = memInfo.getOtherPrivateDirty(i3);
                    int mySharedClean2 = memInfo.getOtherSharedClean(i3);
                    int myPrivateClean2 = memInfo.getOtherPrivateClean(i3);
                    int mySwappedOut2 = memInfo.getOtherSwappedOut(i3);
                    int mySwappedOutPss2 = memInfo.getOtherSwappedOutPss(i3);
                    if (myPss2 == 0 && mySharedDirty2 == 0 && myPrivateDirty2 == 0 && mySharedClean2 == 0 && myPrivateClean2 == 0) {
                        if ((memInfo.hasSwappedOutPss ? mySwappedOutPss2 : mySwappedOut2) != 0) {
                        }
                    } else if (dumpFullInfo) {
                        Object[] objArr13 = new Object[11];
                        objArr13[0] = Debug.MemoryInfo.getOtherLabel(i3);
                        objArr13[1] = Integer.valueOf(myPss2);
                        objArr13[2] = Integer.valueOf(mySwappablePss2);
                        objArr13[3] = Integer.valueOf(mySharedDirty2);
                        objArr13[4] = Integer.valueOf(myPrivateDirty2);
                        objArr13[5] = Integer.valueOf(mySharedClean2);
                        objArr13[6] = Integer.valueOf(myPrivateClean2);
                        if (!memInfo.hasSwappedOutPss) {
                            mySwappedOutPss2 = mySwappedOut2;
                        }
                        objArr13[7] = Integer.valueOf(mySwappedOutPss2);
                        objArr13[8] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr13[9] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr13[10] = ProxyInfo.LOCAL_EXCL_LIST;
                        printRow(pw, HEAP_FULL_COLUMN, objArr13);
                    } else {
                        Object[] objArr14 = new Object[8];
                        objArr14[0] = Debug.MemoryInfo.getOtherLabel(i3);
                        objArr14[1] = Integer.valueOf(myPss2);
                        objArr14[2] = Integer.valueOf(myPrivateDirty2);
                        objArr14[3] = Integer.valueOf(myPrivateClean2);
                        if (!memInfo.hasSwappedOutPss) {
                            mySwappedOutPss2 = mySwappedOut2;
                        }
                        objArr14[4] = Integer.valueOf(mySwappedOutPss2);
                        objArr14[5] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr14[6] = ProxyInfo.LOCAL_EXCL_LIST;
                        objArr14[7] = ProxyInfo.LOCAL_EXCL_LIST;
                        printRow(pw, HEAP_COLUMN, objArr14);
                    }
                }
            }
        }
        pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        pw.println(" App Summary");
        printRow(pw, ONE_COUNT_COLUMN_HEADER, ProxyInfo.LOCAL_EXCL_LIST, "Pss(KB)");
        printRow(pw, ONE_COUNT_COLUMN_HEADER, ProxyInfo.LOCAL_EXCL_LIST, "------");
        printRow(pw, ONE_COUNT_COLUMN, "Java Heap:", Integer.valueOf(memInfo.getSummaryJavaHeap()));
        printRow(pw, ONE_COUNT_COLUMN, "Native Heap:", Integer.valueOf(memInfo.getSummaryNativeHeap()));
        printRow(pw, ONE_COUNT_COLUMN, "Code:", Integer.valueOf(memInfo.getSummaryCode()));
        printRow(pw, ONE_COUNT_COLUMN, "Stack:", Integer.valueOf(memInfo.getSummaryStack()));
        printRow(pw, ONE_COUNT_COLUMN, "Graphics:", Integer.valueOf(memInfo.getSummaryGraphics()));
        printRow(pw, ONE_COUNT_COLUMN, "Private Other:", Integer.valueOf(memInfo.getSummaryPrivateOther()));
        printRow(pw, ONE_COUNT_COLUMN, "System:", Integer.valueOf(memInfo.getSummarySystem()));
        pw.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (memInfo.hasSwappedOutPss) {
            printRow(pw, TWO_COUNT_COLUMNS, "TOTAL:", Integer.valueOf(memInfo.getSummaryTotalPss()), "TOTAL SWAP PSS:", Integer.valueOf(memInfo.getSummaryTotalSwapPss()));
        } else {
            printRow(pw, TWO_COUNT_COLUMNS, "TOTAL:", Integer.valueOf(memInfo.getSummaryTotalPss()), "TOTAL SWAP (KB):", Integer.valueOf(memInfo.getSummaryTotalSwap()));
        }
    }

    public void registerOnActivityPausedListener(Activity activity, OnActivityPausedListener listener) {
        synchronized (this.mOnPauseListeners) {
            ArrayList<OnActivityPausedListener> list = this.mOnPauseListeners.get(activity);
            if (list == null) {
                list = new ArrayList<>();
                this.mOnPauseListeners.put(activity, list);
            }
            list.add(listener);
        }
    }

    public void unregisterOnActivityPausedListener(Activity activity, OnActivityPausedListener listener) {
        synchronized (this.mOnPauseListeners) {
            ArrayList<OnActivityPausedListener> list = this.mOnPauseListeners.get(activity);
            if (list != null) {
                list.remove(listener);
            }
        }
    }

    public final ActivityInfo resolveActivityInfo(Intent intent) {
        ActivityInfo aInfo = intent.resolveActivityInfo(this.mInitialApplication.getPackageManager(), 1024);
        if (aInfo == null) {
            Instrumentation.checkStartActivityResult(-2, intent);
        }
        return aInfo;
    }

    public final Activity startActivityNow(Activity parent, String id, Intent intent, ActivityInfo activityInfo, IBinder token, Bundle state, Activity.NonConfigurationInstances lastNonConfigurationInstances) {
        String name;
        ActivityClientRecord r = new ActivityClientRecord();
        r.token = token;
        r.ident = 0;
        r.intent = intent;
        r.state = state;
        r.parent = parent;
        r.embeddedID = id;
        r.activityInfo = activityInfo;
        r.lastNonConfigurationInstances = lastNonConfigurationInstances;
        if (localLOGV) {
            ComponentName compname = intent.getComponent();
            if (compname != null) {
                name = compname.toShortString();
            } else {
                name = "(Intent " + intent + ").getComponent() returned null";
            }
            Slog.v(TAG, "Performing launch: action=" + intent.getAction() + ", comp=" + name + ", token=" + token);
        }
        return performLaunchActivity(r, null);
    }

    public final Activity getActivity(IBinder token) {
        return this.mActivities.get(token).activity;
    }

    public final void sendActivityResult(IBinder token, String id, int requestCode, int resultCode, Intent data) {
        if (DEBUG_RESULTS) {
            Slog.v(TAG, "sendActivityResult: id=" + id + " req=" + requestCode + " res=" + resultCode + " data=" + data);
        }
        ArrayList<ResultInfo> list = new ArrayList<>();
        list.add(new ResultInfo(id, requestCode, resultCode, data));
        this.mAppThread.scheduleSendResult(token, list);
    }

    private void sendMessage(int what, Object obj) {
        sendMessage(what, obj, 0, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1) {
        sendMessage(what, obj, arg1, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2) {
        sendMessage(what, obj, arg1, arg2, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2, boolean async) {
        if (DEBUG_MESSAGES) {
            Slog.v(TAG, "SCHEDULE " + what + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.mH.codeToString(what) + ": " + arg1 + " / " + obj);
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        if (async) {
            msg.setAsynchronous(true);
        }
        this.mH.sendMessage(msg);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2, int seq) {
        if (DEBUG_MESSAGES) {
            Slog.v(TAG, "SCHEDULE " + this.mH.codeToString(what) + " arg1=" + arg1 + " arg2=" + arg2 + "seq= " + seq);
        }
        Message msg = Message.obtain();
        msg.what = what;
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = obj;
        args.argi1 = arg1;
        args.argi2 = arg2;
        args.argi3 = seq;
        msg.obj = args;
        this.mH.sendMessage(msg);
    }

    final void scheduleContextCleanup(ContextImpl context, String who, String what) {
        ContextCleanupInfo cci = new ContextCleanupInfo();
        cci.context = context;
        cci.who = who;
        cci.what = what;
        sendMessage(119, cci);
    }

    private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) throws SuperNotCalledException {
        ActivityInfo aInfo = r.activityInfo;
        if (r.packageInfo == null) {
            r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo, 1);
        }
        ComponentName component = r.intent.getComponent();
        if (component == null) {
            component = r.intent.resolveActivity(this.mInitialApplication.getPackageManager());
            r.intent.setComponent(component);
        }
        if (r.activityInfo.targetActivity != null) {
            component = new ComponentName(r.activityInfo.packageName, r.activityInfo.targetActivity);
        }
        Activity activity = null;
        try {
            ClassLoader cl = r.packageInfo.getClassLoader();
            activity = this.mInstrumentation.newActivity(cl, component.getClassName(), r.intent);
            StrictMode.incrementExpectedActivityCount(activity.getClass());
            r.intent.setExtrasClassLoader(cl);
            r.intent.prepareToEnterProcess();
            if (r.state != null) {
                r.state.setClassLoader(cl);
            }
        } catch (Exception e) {
            if (!this.mInstrumentation.onException(activity, e)) {
                throw new RuntimeException("Unable to instantiate activity " + component + ": " + e.toString(), e);
            }
        }
        try {
            Application app = r.packageInfo.makeApplication(false, this.mInstrumentation);
            if (localLOGV) {
                Slog.v(TAG, "Performing launch of " + r);
            }
            if (localLOGV) {
                Slog.v(TAG, r + ": app=" + app + ", appName=" + app.getPackageName() + ", pkg=" + r.packageInfo.getPackageName() + ", comp=" + r.intent.getComponent().toShortString() + ", dir=" + r.packageInfo.getAppDir());
            }
            if (activity != null) {
                Context appContext = createBaseContextForActivity(r, activity);
                CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
                Configuration config = new Configuration(this.mCompatConfiguration);
                if (r.overrideConfig != null) {
                    config.updateFrom(r.overrideConfig);
                }
                if (DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Launching activity " + r.activityInfo.name + " with config " + config);
                }
                Window window = null;
                if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
                    window = r.mPendingRemoveWindow;
                    r.mPendingRemoveWindow = null;
                    r.mPendingRemoveWindowManager = null;
                }
                activity.attach(appContext, this, getInstrumentation(), r.token, r.ident, app, r.intent, r.activityInfo, title, r.parent, r.embeddedID, r.lastNonConfigurationInstances, config, r.referrer, r.voiceInteractor, window);
                if (customIntent != null) {
                    activity.mIntent = customIntent;
                }
                r.lastNonConfigurationInstances = null;
                activity.mStartedActivity = false;
                int theme = r.activityInfo.getThemeResource();
                if (theme != 0) {
                    activity.setTheme(theme);
                }
                activity.mCalled = false;
                if (r.isPersistable()) {
                    this.mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
                } else {
                    this.mInstrumentation.callActivityOnCreate(activity, r.state);
                }
                if (!activity.mCalled) {
                    throw new SuperNotCalledException("Activity " + r.intent.getComponent().toShortString() + " did not call through to super.onCreate()");
                }
                r.activity = activity;
                r.stopped = true;
                if (!r.activity.mFinished) {
                    activity.performStart();
                    r.stopped = false;
                }
                if (!r.activity.mFinished) {
                    if (r.isPersistable()) {
                        if (r.state != null || r.persistentState != null) {
                            this.mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state, r.persistentState);
                        }
                    } else if (r.state != null) {
                        this.mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state);
                    }
                }
                if (!r.activity.mFinished) {
                    activity.mCalled = false;
                    if (r.isPersistable()) {
                        this.mInstrumentation.callActivityOnPostCreate(activity, r.state, r.persistentState);
                    } else {
                        this.mInstrumentation.callActivityOnPostCreate(activity, r.state);
                    }
                    if (!activity.mCalled) {
                        throw new SuperNotCalledException("Activity " + r.intent.getComponent().toShortString() + " did not call through to super.onPostCreate()");
                    }
                }
            }
            r.paused = true;
            this.mActivities.put(r.token, r);
        } catch (Exception e2) {
            if (!this.mInstrumentation.onException(activity, e2)) {
                throw new RuntimeException("Unable to start activity " + component + ": " + e2.toString(), e2);
            }
        } catch (SuperNotCalledException e3) {
            throw e3;
        }
        return activity;
    }

    private Context createBaseContextForActivity(ActivityClientRecord r, Activity activity) {
        try {
            int displayId = ActivityManagerNative.getDefault().getActivityDisplayId(r.token);
            ContextImpl appContext = ContextImpl.createActivityContext(this, r.packageInfo, r.token, displayId, r.overrideConfig);
            appContext.setOuterContext(activity);
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            String pkgName = SystemProperties.get("debug.second-display.pkg");
            if (pkgName == null || pkgName.isEmpty() || !r.packageInfo.mPackageName.contains(pkgName)) {
                return appContext;
            }
            for (int id : dm.getDisplayIds()) {
                if (id != 0) {
                    Display display = dm.getCompatibleDisplay(id, appContext.getDisplayAdjustments(id));
                    Context baseContext = appContext.createDisplayContext(display);
                    return baseContext;
                }
            }
            return appContext;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void handleLaunchActivity(ActivityClientRecord r, Intent customIntent, String reason) throws SuperNotCalledException {
        unscheduleGcIdler();
        this.mSomeActivitiesChanged = true;
        if (r.profilerInfo != null) {
            this.mProfiler.setProfiler(r.profilerInfo);
            this.mProfiler.startProfiling();
        }
        handleConfigurationChanged(null, null);
        if (localLOGV || !IS_USER_BUILD || DEBUG_LIFECYCLE) {
            Slog.d(TAG, "Handling launch of " + r + " reason=" + reason + " startsNotResumed=" + r.startsNotResumed);
        }
        WindowManagerGlobal.initialize();
        Activity a = performLaunchActivity(r, customIntent);
        if (a != null) {
            r.createdConfig = new Configuration(this.mConfiguration);
            reportSizeConfigurations(r);
            Bundle oldState = r.state;
            handleResumeActivity(r.token, false, r.isForward, (r.activity.mFinished || r.startsNotResumed) ? false : true, r.lastProcessedSeq, reason);
            if (r.activity.mFinished || !r.startsNotResumed) {
                return;
            }
            performPauseActivityIfNeeded(r, reason);
            if (!r.isPreHoneycomb()) {
                return;
            }
            r.state = oldState;
            return;
        }
        try {
            ActivityManagerNative.getDefault().finishActivity(r.token, 0, null, 0);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private void reportSizeConfigurations(ActivityClientRecord r) {
        Configuration[] configurations = r.activity.getResources().getSizeConfigurations();
        if (configurations == null) {
            return;
        }
        SparseIntArray horizontal = new SparseIntArray();
        SparseIntArray vertical = new SparseIntArray();
        SparseIntArray smallest = new SparseIntArray();
        for (int i = configurations.length - 1; i >= 0; i--) {
            Configuration config = configurations[i];
            if (config.screenHeightDp != 0) {
                vertical.put(config.screenHeightDp, 0);
            }
            if (config.screenWidthDp != 0) {
                horizontal.put(config.screenWidthDp, 0);
            }
            if (config.smallestScreenWidthDp != 0) {
                smallest.put(config.smallestScreenWidthDp, 0);
            }
        }
        try {
            ActivityManagerNative.getDefault().reportSizeConfigurations(r.token, horizontal.copyKeys(), vertical.copyKeys(), smallest.copyKeys());
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private void deliverNewIntents(ActivityClientRecord r, List<ReferrerIntent> intents) {
        int N = intents.size();
        for (int i = 0; i < N; i++) {
            ReferrerIntent intent = intents.get(i);
            intent.setExtrasClassLoader(r.activity.getClassLoader());
            intent.prepareToEnterProcess();
            r.activity.mFragments.noteStateNotSaved();
            this.mInstrumentation.callActivityOnNewIntent(r.activity, intent);
        }
    }

    public final void performNewIntents(IBinder token, List<ReferrerIntent> intents) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        boolean resumed = !r.paused;
        if (resumed) {
            r.activity.mTemporaryPause = true;
            this.mInstrumentation.callActivityOnPause(r.activity);
        }
        deliverNewIntents(r, intents);
        if (!resumed) {
            return;
        }
        r.activity.performResume();
        r.activity.mTemporaryPause = false;
    }

    private void handleNewIntent(NewIntentData data) {
        performNewIntents(data.token, data.intents);
    }

    public void handleRequestAssistContextExtras(RequestAssistContextExtras cmd) {
        if (this.mLastSessionId != cmd.sessionId) {
            this.mLastSessionId = cmd.sessionId;
            for (int i = this.mLastAssistStructures.size() - 1; i >= 0; i--) {
                AssistStructure structure = this.mLastAssistStructures.get(i).get();
                if (structure != null) {
                    structure.clearSendChannel();
                }
                this.mLastAssistStructures.remove(i);
            }
        }
        Bundle data = new Bundle();
        AssistStructure structure2 = null;
        AssistContent content = new AssistContent();
        ActivityClientRecord r = this.mActivities.get(cmd.activityToken);
        Uri referrer = null;
        if (r != null) {
            r.activity.getApplication().dispatchOnProvideAssistData(r.activity, data);
            r.activity.onProvideAssistData(data);
            referrer = r.activity.onProvideReferrer();
            if (cmd.requestType == 1) {
                structure2 = new AssistStructure(r.activity);
                Intent activityIntent = r.activity.getIntent();
                if (activityIntent != null && (r.window == null || (r.window.getAttributes().flags & 8192) == 0)) {
                    Intent intent = new Intent(activityIntent);
                    intent.setFlags(intent.getFlags() & (-67));
                    intent.removeUnsafeExtras();
                    content.setDefaultIntent(intent);
                } else {
                    content.setDefaultIntent(new Intent());
                }
                r.activity.onProvideAssistContent(content);
            }
        }
        if (structure2 == null) {
            structure2 = new AssistStructure();
        }
        this.mLastAssistStructures.add(new WeakReference<>(structure2));
        IActivityManager mgr = ActivityManagerNative.getDefault();
        try {
            mgr.reportAssistContextExtras(cmd.requestToken, data, structure2, content, referrer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void handleTranslucentConversionComplete(IBinder token, boolean drawComplete) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.activity.onTranslucentConversionComplete(drawComplete);
    }

    public void onNewActivityOptions(IBinder token, ActivityOptions options) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.activity.onNewActivityOptions(options);
    }

    public void handleCancelVisibleBehind(IBinder token) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r != null) {
            this.mSomeActivitiesChanged = true;
            Activity activity = r.activity;
            if (activity.mVisibleBehind) {
                activity.mCalled = false;
                activity.onVisibleBehindCanceled();
                if (!activity.mCalled) {
                    throw new SuperNotCalledException("Activity " + activity.getLocalClassName() + " did not call through to super.onVisibleBehindCanceled()");
                }
                activity.mVisibleBehind = false;
            }
        }
        try {
            ActivityManagerNative.getDefault().backgroundResourcesReleased(token);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void handleOnBackgroundVisibleBehindChanged(IBinder token, boolean visible) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.activity.onBackgroundVisibleBehindChanged(visible);
    }

    public void handleInstallProvider(ProviderInfo info) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            installContentProviders(this.mInitialApplication, Lists.newArrayList(new ProviderInfo[]{info}));
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleEnterAnimationComplete(IBinder token) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.activity.dispatchEnterAnimationComplete();
    }

    private void handleStartBinderTracking() {
        Binder.enableTracing();
    }

    private void handleStopBinderTrackingAndDump(ParcelFileDescriptor fd) {
        try {
            Binder.disableTracing();
            Binder.getTransactionTracker().writeTracesToFile(fd);
        } finally {
            IoUtils.closeQuietly(fd);
            Binder.getTransactionTracker().clearTraces();
        }
    }

    private void handleMultiWindowModeChanged(IBinder token, boolean isInMultiWindowMode) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.activity.dispatchMultiWindowModeChanged(isInMultiWindowMode);
    }

    private void handlePictureInPictureModeChanged(IBinder token, boolean isInPipMode) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.activity.dispatchPictureInPictureModeChanged(isInPipMode);
    }

    private void handleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor interactor) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            return;
        }
        r.voiceInteractor = interactor;
        r.activity.setVoiceInteractor(interactor);
        if (interactor == null) {
            r.activity.onLocalVoiceInteractionStopped();
        } else {
            r.activity.onLocalVoiceInteractionStarted();
        }
    }

    public static Intent getIntentBeingBroadcast() {
        return sCurrentBroadcastIntent.get();
    }

    private void handleReceiver(ReceiverData data) {
        unscheduleGcIdler();
        String component = data.intent.getComponent().getClassName();
        LoadedApk packageInfo = getPackageInfoNoCheck(data.info.applicationInfo, data.compatInfo);
        IActivityManager mgr = ActivityManagerNative.getDefault();
        try {
            ClassLoader cl = packageInfo.getClassLoader();
            data.intent.setExtrasClassLoader(cl);
            data.intent.prepareToEnterProcess();
            data.setExtrasClassLoader(cl);
            BroadcastReceiver receiver = (BroadcastReceiver) cl.loadClass(component).newInstance();
            try {
                Application app = packageInfo.makeApplication(false, this.mInstrumentation);
                if (localLOGV) {
                    Slog.v(TAG, "Performing receive of " + data.intent + ": app=" + app + ", appName=" + app.getPackageName() + ", pkg=" + packageInfo.getPackageName() + ", comp=" + data.intent.getComponent().toShortString() + ", dir=" + packageInfo.getAppDir());
                }
                ContextImpl context = (ContextImpl) app.getBaseContext();
                sCurrentBroadcastIntent.set(data.intent);
                receiver.setPendingResult(data);
                Slog.d(TAG, "BDC-Calling onReceive: intent=" + data.intent + ", receiver=" + receiver);
                receiver.onReceive(context.getReceiverRestrictedContext(), data.intent);
            } catch (Exception e) {
                if (DEBUG_BROADCAST) {
                    Slog.i(TAG, "Finishing failed broadcast to " + data.intent.getComponent());
                }
                data.sendFinished(mgr);
                if (!this.mInstrumentation.onException(receiver, e)) {
                    throw new RuntimeException("Unable to start receiver " + component + ": " + e.toString(), e);
                }
            } finally {
                sCurrentBroadcastIntent.set(null);
            }
            if (receiver.getPendingResult() != null) {
                data.finish();
            }
        } catch (Exception e2) {
            if (DEBUG_BROADCAST) {
                Slog.i(TAG, "Finishing failed broadcast to " + data.intent.getComponent());
            }
            data.sendFinished(mgr);
            throw new RuntimeException("Unable to instantiate receiver " + component + ": " + e2.toString(), e2);
        }
    }

    private void handleCreateBackupAgent(CreateBackupAgentData createBackupAgentData) {
        if (DEBUG_BACKUP) {
            Slog.v(TAG, "handleCreateBackupAgent: " + createBackupAgentData);
        }
        try {
            if (getPackageManager().getPackageInfo(createBackupAgentData.appInfo.packageName, 0, UserHandle.myUserId()).applicationInfo.uid != Process.myUid()) {
                Slog.w(TAG, "Asked to instantiate non-matching package " + createBackupAgentData.appInfo.packageName);
                return;
            }
            unscheduleGcIdler();
            LoadedApk packageInfoNoCheck = getPackageInfoNoCheck(createBackupAgentData.appInfo, createBackupAgentData.compatInfo);
            String str = packageInfoNoCheck.mPackageName;
            if (str == null) {
                Slog.d(TAG, "Asked to create backup agent for nonexistent package");
                return;
            }
            String str2 = createBackupAgentData.appInfo.backupAgentName;
            if (str2 == null && (createBackupAgentData.backupMode == 1 || createBackupAgentData.backupMode == 3)) {
                str2 = "android.app.backup.FullBackupAgent";
            }
            IBinder iBinderOnBind = null;
            try {
                BackupAgent backupAgent = this.mBackupAgents.get(str);
                if (backupAgent != null) {
                    if (DEBUG_BACKUP) {
                        Slog.v(TAG, "Reusing existing agent instance");
                    }
                    iBinderOnBind = backupAgent.onBind();
                } else {
                    try {
                        if (DEBUG_BACKUP) {
                            Slog.v(TAG, "Initializing agent class " + str2);
                        }
                        BackupAgent backupAgent2 = (BackupAgent) packageInfoNoCheck.getClassLoader().loadClass(str2).newInstance();
                        ContextImpl contextImplCreateAppContext = ContextImpl.createAppContext(this, packageInfoNoCheck);
                        contextImplCreateAppContext.setOuterContext(backupAgent2);
                        backupAgent2.attach(contextImplCreateAppContext);
                        backupAgent2.onCreate();
                        iBinderOnBind = backupAgent2.onBind();
                        this.mBackupAgents.put(str, backupAgent2);
                    } catch (Exception e) {
                        Slog.e(TAG, "Agent threw during creation: " + e);
                        if (createBackupAgentData.backupMode != 2 && createBackupAgentData.backupMode != 3) {
                            throw e;
                        }
                    }
                }
                try {
                    ActivityManagerNative.getDefault().backupAgentCreated(str, iBinderOnBind);
                } catch (RemoteException e2) {
                    throw e2.rethrowFromSystemServer();
                }
            } catch (Exception e3) {
                throw new RuntimeException("Unable to create BackupAgent " + str2 + ": " + e3.toString(), e3);
            }
        } catch (RemoteException e4) {
            throw e4.rethrowFromSystemServer();
        }
    }

    private void handleDestroyBackupAgent(CreateBackupAgentData data) {
        if (DEBUG_BACKUP) {
            Slog.v(TAG, "handleDestroyBackupAgent: " + data);
        }
        LoadedApk packageInfo = getPackageInfoNoCheck(data.appInfo, data.compatInfo);
        String packageName = packageInfo.mPackageName;
        BackupAgent agent = this.mBackupAgents.get(packageName);
        if (agent != null) {
            try {
                agent.onDestroy();
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown in onDestroy by backup agent of " + data.appInfo);
                e.printStackTrace();
            }
            this.mBackupAgents.remove(packageName);
            return;
        }
        Slog.w(TAG, "Attempt to destroy unknown backup agent " + data);
    }

    private void handleCreateService(CreateServiceData data) {
        unscheduleGcIdler();
        LoadedApk packageInfo = getPackageInfoNoCheck(data.info.applicationInfo, data.compatInfo);
        Service service = null;
        try {
            ClassLoader cl = packageInfo.getClassLoader();
            service = (Service) cl.loadClass(data.info.name).newInstance();
        } catch (Exception e) {
            if (!this.mInstrumentation.onException(null, e)) {
                throw new RuntimeException("Unable to instantiate service " + data.info.name + ": " + e.toString(), e);
            }
        }
        try {
            Slog.v(TAG, "SVC-Creating service " + data);
            ContextImpl contextImplCreateAppContext = ContextImpl.createAppContext(this, packageInfo);
            contextImplCreateAppContext.setOuterContext(service);
            Application app = packageInfo.makeApplication(false, this.mInstrumentation);
            service.attach(contextImplCreateAppContext, this, data.info.name, data.token, app, ActivityManagerNative.getDefault());
            service.onCreate();
            this.mServices.put(data.token, service);
            try {
                ActivityManagerNative.getDefault().serviceDoneExecuting(data.token, 0, 0, 0);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        } catch (Exception e3) {
            if (this.mInstrumentation.onException(service, e3)) {
            } else {
                throw new RuntimeException("Unable to create service " + data.info.name + ": " + e3.toString(), e3);
            }
        }
    }

    private void handleBindService(BindServiceData data) {
        Service s = this.mServices.get(data.token);
        if (DEBUG_SERVICE) {
            Slog.v(TAG, "handleBindService s=" + s + " rebind=" + data.rebind);
        }
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                data.intent.prepareToEnterProcess();
                try {
                    if (data.rebind) {
                        s.onRebind(data.intent);
                        ActivityManagerNative.getDefault().serviceDoneExecuting(data.token, 0, 0, 0);
                    } else {
                        IBinder binder = s.onBind(data.intent);
                        ActivityManagerNative.getDefault().publishService(data.token, data.intent, binder);
                    }
                    ensureJitEnabled();
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(s, e)) {
                    throw new RuntimeException("Unable to bind to service " + s + " with " + data.intent + ": " + e.toString(), e);
                }
            }
        }
    }

    private void handleUnbindService(BindServiceData data) {
        Service s = this.mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                data.intent.prepareToEnterProcess();
                boolean doRebind = s.onUnbind(data.intent);
                try {
                    if (doRebind) {
                        ActivityManagerNative.getDefault().unbindFinished(data.token, data.intent, doRebind);
                    } else {
                        ActivityManagerNative.getDefault().serviceDoneExecuting(data.token, 0, 0, 0);
                    }
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(s, e)) {
                    throw new RuntimeException("Unable to unbind to service " + s + " with " + data.intent + ": " + e.toString(), e);
                }
            }
        }
    }

    private void handleDumpService(DumpComponentInfo info) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            Service s = this.mServices.get(info.token);
            if (s != null) {
                FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(info.fd.getFileDescriptor()));
                s.dump(info.fd.getFileDescriptor(), fastPrintWriter, info.args);
                fastPrintWriter.flush();
            }
        } finally {
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleDumpActivity(DumpComponentInfo info) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            ActivityClientRecord r = this.mActivities.get(info.token);
            if (r != null && r.activity != null) {
                PrintWriter pw = new FastPrintWriter(new FileOutputStream(info.fd.getFileDescriptor()));
                r.activity.dump(info.prefix, info.fd.getFileDescriptor(), pw, info.args);
                pw.flush();
            }
        } finally {
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleDumpProvider(DumpComponentInfo info) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            ProviderClientRecord r = this.mLocalProviders.get(info.token);
            if (r != null && r.mLocalProvider != null) {
                PrintWriter pw = new FastPrintWriter(new FileOutputStream(info.fd.getFileDescriptor()));
                r.mLocalProvider.dump(info.fd.getFileDescriptor(), pw, info.args);
                pw.flush();
            }
        } finally {
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleServiceArgs(ServiceArgsData data) {
        int res;
        Service s = this.mServices.get(data.token);
        if (s != null) {
            try {
                if (data.args != null) {
                    data.args.setExtrasClassLoader(s.getClassLoader());
                    data.args.prepareToEnterProcess();
                }
                if (data.taskRemoved) {
                    s.onTaskRemoved(data.args);
                    res = 1000;
                } else {
                    Slog.d(TAG, "SVC-Calling onStartCommand: " + s + ", flags=" + data.flags + ", startId=" + data.startId);
                    res = s.onStartCommand(data.args, data.flags, data.startId);
                }
                QueuedWork.waitToFinish();
                try {
                    ActivityManagerNative.getDefault().serviceDoneExecuting(data.token, 1, data.startId, res);
                    ensureJitEnabled();
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (Exception e2) {
                if (!this.mInstrumentation.onException(s, e2)) {
                    throw new RuntimeException("Unable to start service " + s + " with " + data.args + ": " + e2.toString(), e2);
                }
            }
        }
    }

    private void handleStopService(IBinder token) {
        Service s = this.mServices.remove(token);
        if (s != null) {
            try {
                Slog.v(TAG, "SVC-Destroying service " + s);
                s.onDestroy();
                Context context = s.getBaseContext();
                if (context instanceof ContextImpl) {
                    String who = s.getClassName();
                    ((ContextImpl) context).scheduleFinalCleanup(who, "Service");
                }
                QueuedWork.waitToFinish();
                try {
                    ActivityManagerNative.getDefault().serviceDoneExecuting(token, 2, 0, 0);
                    return;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (Exception e2) {
                if (!this.mInstrumentation.onException(s, e2)) {
                    throw new RuntimeException("Unable to stop service " + s + ": " + e2.toString(), e2);
                }
                Slog.i(TAG, "handleStopService: exception for " + token, e2);
                return;
            }
        }
        Slog.i(TAG, "handleStopService: token=" + token + " not found.");
    }

    public final ActivityClientRecord performResumeActivity(IBinder token, boolean clearHide, String reason) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (localLOGV) {
            Slog.v(TAG, "Performing resume of " + r + " finished=" + r.activity.mFinished);
        }
        if (r != null && !r.activity.mFinished) {
            if (clearHide) {
                r.hideForNow = false;
                r.activity.mStartedActivity = false;
            }
            try {
                r.activity.onStateNotSaved();
                r.activity.mFragments.noteStateNotSaved();
                if (r.pendingIntents != null) {
                    deliverNewIntents(r, r.pendingIntents);
                    r.pendingIntents = null;
                }
                if (r.pendingResults != null) {
                    deliverResults(r, r.pendingResults);
                    r.pendingResults = null;
                }
                r.activity.performResume();
                for (int i = this.mRelaunchingActivities.size() - 1; i >= 0; i--) {
                    ActivityClientRecord relaunching = this.mRelaunchingActivities.get(i);
                    if (relaunching.token == r.token && relaunching.onlyLocalRequest && relaunching.startsNotResumed) {
                        relaunching.startsNotResumed = false;
                    }
                }
                EventLog.writeEvent(LOG_AM_ON_RESUME_CALLED, Integer.valueOf(UserHandle.myUserId()), r.activity.getComponentName().getClassName(), reason);
                if (!IS_USER_BUILD || DEBUG_LIFECYCLE) {
                    Slog.d(TAG, "ACT-AM_ON_RESUME_CALLED " + r);
                }
                r.paused = false;
                r.stopped = false;
                r.state = null;
                r.persistentState = null;
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(r.activity, e)) {
                    throw new RuntimeException("Unable to resume activity " + r.intent.getComponent().toShortString() + ": " + e.toString(), e);
                }
            }
        }
        return r;
    }

    static final void cleanUpPendingRemoveWindows(ActivityClientRecord r, boolean force) {
        if (r.mPreserveWindow && !force) {
            return;
        }
        if (r.mPendingRemoveWindow != null) {
            r.mPendingRemoveWindowManager.removeViewImmediate(r.mPendingRemoveWindow.getDecorView());
            IBinder wtoken = r.mPendingRemoveWindow.getDecorView().getWindowToken();
            if (wtoken != null) {
                WindowManagerGlobal.getInstance().closeAll(wtoken, r.activity.getClass().getName(), "Activity");
            }
        }
        r.mPendingRemoveWindow = null;
        r.mPendingRemoveWindowManager = null;
    }

    final void handleResumeActivity(IBinder token, boolean clearHide, boolean isForward, boolean reallyResume, int seq, String reason) throws SuperNotCalledException {
        if (checkAndUpdateLifecycleSeq(seq, this.mActivities.get(token), "resumeActivity")) {
            unscheduleGcIdler();
            this.mSomeActivitiesChanged = true;
            ActivityClientRecord r = performResumeActivity(token, clearHide, reason);
            if (r == null) {
                try {
                    ActivityManagerNative.getDefault().finishActivity(token, 0, null, 0);
                    return;
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
            Activity a = r.activity;
            if (localLOGV) {
                Slog.v(TAG, "Resume " + r + " started activity: " + a.mStartedActivity + ", hideForNow: " + r.hideForNow + ", finished: " + a.mFinished);
            }
            int forwardBit = isForward ? 256 : 0;
            boolean willBeVisible = !a.mStartedActivity;
            if (!willBeVisible) {
                try {
                    willBeVisible = ActivityManagerNative.getDefault().willActivityBeVisible(a.getActivityToken());
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                View decor = r.window.getDecorView();
                decor.setVisibility(4);
                ViewManager wm = a.getWindowManager();
                WindowManager.LayoutParams l = r.window.getAttributes();
                a.mDecor = decor;
                l.type = 1;
                l.softInputMode |= forwardBit;
                if (r.mPreserveWindow) {
                    a.mWindowAdded = true;
                    r.mPreserveWindow = false;
                    ViewRootImpl impl = decor.getViewRootImpl();
                    if (impl != null) {
                        impl.notifyChildRebuilt();
                    }
                }
                if (a.mVisibleFromClient && !a.mWindowAdded) {
                    a.mWindowAdded = true;
                    wm.addView(decor, l);
                }
            } else if (!willBeVisible) {
                if (localLOGV) {
                    Slog.v(TAG, "Launch " + r + " mStartedActivity set");
                }
                r.hideForNow = true;
            }
            cleanUpPendingRemoveWindows(r, false);
            if (!r.activity.mFinished && willBeVisible && r.activity.mDecor != null && !r.hideForNow) {
                if (r.newConfig != null) {
                    performConfigurationChangedForActivity(r, r.newConfig, true);
                    if (DEBUG_CONFIGURATION) {
                        Slog.v(TAG, "Resuming activity " + r.activityInfo.name + " with newConfig " + r.activity.mCurrentConfig);
                    }
                    r.newConfig = null;
                }
                if (localLOGV) {
                    Slog.v(TAG, "Resuming " + r + " with isForward=" + isForward);
                }
                WindowManager.LayoutParams l2 = r.window.getAttributes();
                if ((l2.softInputMode & 256) != forwardBit) {
                    l2.softInputMode = (l2.softInputMode & (-257)) | forwardBit;
                    if (r.activity.mVisibleFromClient) {
                        ViewManager wm2 = a.getWindowManager();
                        wm2.updateViewLayout(r.window.getDecorView(), l2);
                    }
                }
                r.activity.mVisibleFromServer = true;
                this.mNumVisibleActivities++;
                if (r.activity.mVisibleFromClient) {
                    r.activity.makeVisible();
                }
            }
            if (!r.onlyLocalRequest) {
                r.nextIdle = this.mNewActivities;
                this.mNewActivities = r;
                if (localLOGV) {
                    Slog.v(TAG, "Scheduling idle handler for " + r);
                }
                Looper.myQueue().addIdleHandler(new Idler(this, null));
            }
            r.onlyLocalRequest = false;
            if (reallyResume) {
                try {
                    ActivityManagerNative.getDefault().activityResumed(token);
                } catch (RemoteException ex2) {
                    throw ex2.rethrowFromSystemServer();
                }
            }
        }
    }

    private Bitmap createThumbnailBitmap(ActivityClientRecord r) {
        int h;
        Bitmap thumbnail = this.mAvailThumbnailBitmap;
        if (thumbnail == null) {
            try {
                int w = this.mThumbnailWidth;
                if (w < 0) {
                    Resources res = r.activity.getResources();
                    w = res.getDimensionPixelSize(R.dimen.thumbnail_width);
                    this.mThumbnailWidth = w;
                    h = res.getDimensionPixelSize(R.dimen.thumbnail_height);
                    this.mThumbnailHeight = h;
                } else {
                    h = this.mThumbnailHeight;
                }
                if (w > 0 && h > 0) {
                    thumbnail = Bitmap.createBitmap(r.activity.getResources().getDisplayMetrics(), w, h, THUMBNAIL_FORMAT);
                    thumbnail.eraseColor(0);
                }
            } catch (Exception e) {
                if (this.mInstrumentation.onException(r.activity, e)) {
                    return null;
                }
                throw new RuntimeException("Unable to create thumbnail of " + r.intent.getComponent().toShortString() + ": " + e.toString(), e);
            }
        }
        if (thumbnail == null) {
            return thumbnail;
        }
        Canvas cv = this.mThumbnailCanvas;
        if (cv == null) {
            cv = new Canvas();
            this.mThumbnailCanvas = cv;
        }
        cv.setBitmap(thumbnail);
        if (!r.activity.onCreateThumbnail(thumbnail, cv)) {
            this.mAvailThumbnailBitmap = thumbnail;
            thumbnail = null;
        }
        cv.setBitmap(null);
        return thumbnail;
    }

    private void handlePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges, boolean dontReport, int seq) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (DEBUG_ORDER) {
            Slog.d(TAG, "handlePauseActivity " + r + ", seq: " + seq);
        }
        if (!checkAndUpdateLifecycleSeq(seq, r, "pauseActivity") || r == null) {
            return;
        }
        if (userLeaving) {
            performUserLeavingActivity(r);
        }
        r.activity.mConfigChangeFlags |= configChanges;
        performPauseActivity(token, finished, r.isPreHoneycomb(), "handlePauseActivity");
        if (r.isPreHoneycomb()) {
            QueuedWork.waitToFinish();
        }
        if (!dontReport) {
            try {
                ActivityManagerNative.getDefault().activityPaused(token);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        this.mSomeActivitiesChanged = true;
    }

    final void performUserLeavingActivity(ActivityClientRecord r) {
        this.mInstrumentation.callActivityOnUserLeaving(r.activity);
    }

    final Bundle performPauseActivity(IBinder token, boolean finished, boolean saveState, String reason) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r != null) {
            return performPauseActivity(r, finished, saveState, reason);
        }
        return null;
    }

    final Bundle performPauseActivity(ActivityClientRecord r, boolean finished, boolean saveState, String reason) throws SuperNotCalledException {
        ArrayList<OnActivityPausedListener> listeners;
        if (r.paused) {
            if (r.activity.mFinished) {
                return null;
            }
            RuntimeException e = new RuntimeException("Performing pause of activity that is not resumed: " + r.intent.getComponent().toShortString());
            Slog.e(TAG, e.getMessage(), e);
        }
        if (finished) {
            r.activity.mFinished = true;
        }
        if (!r.activity.mFinished && saveState) {
            callCallActivityOnSaveInstanceState(r);
        }
        performPauseActivityIfNeeded(r, reason);
        synchronized (this.mOnPauseListeners) {
            listeners = this.mOnPauseListeners.remove(r.activity);
        }
        int size = listeners != null ? listeners.size() : 0;
        for (int i = 0; i < size; i++) {
            listeners.get(i).onPaused(r.activity);
        }
        if (r.activity.mFinished || !saveState) {
            return null;
        }
        return r.state;
    }

    private void performPauseActivityIfNeeded(ActivityClientRecord r, String reason) throws SuperNotCalledException {
        if (r.paused) {
            return;
        }
        try {
            r.activity.mCalled = false;
            this.mInstrumentation.callActivityOnPause(r.activity);
            EventLog.writeEvent(LOG_AM_ON_PAUSE_CALLED, Integer.valueOf(UserHandle.myUserId()), r.activity.getComponentName().getClassName(), reason);
            if (!IS_USER_BUILD || DEBUG_LIFECYCLE) {
                Slog.d(TAG, "ACT-AM_ON_PAUSE_CALLED " + r);
            }
            if (!r.activity.mCalled) {
                throw new SuperNotCalledException("Activity " + safeToComponentShortString(r.intent) + " did not call through to super.onPause()");
            }
        } catch (SuperNotCalledException e) {
            throw e;
        } catch (Exception e2) {
            if (!this.mInstrumentation.onException(r.activity, e2)) {
                throw new RuntimeException("Unable to pause activity " + safeToComponentShortString(r.intent) + ": " + e2.toString(), e2);
            }
        }
        r.paused = true;
    }

    final void performStopActivity(IBinder token, boolean saveState, String reason) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(token);
        performStopActivityInner(r, null, false, saveState, reason);
    }

    private static class StopInfo implements Runnable {
        ActivityClientRecord activity;
        CharSequence description;
        PersistableBundle persistentState;
        Bundle state;

        StopInfo(StopInfo stopInfo) {
            this();
        }

        private StopInfo() {
        }

        @Override
        public void run() {
            try {
                if (ActivityThread.DEBUG_MEMORY_TRIM) {
                    Slog.v(ActivityThread.TAG, "Reporting activity stopped: " + this.activity);
                }
                ActivityManagerNative.getDefault().activityStopped(this.activity.token, this.state, this.persistentState, this.description);
            } catch (RemoteException ex) {
                if ((ex instanceof TransactionTooLargeException) && this.activity.packageInfo.getTargetSdkVersion() < 24) {
                    Log.e(ActivityThread.TAG, "App sent too much data in instance state, so it was ignored", ex);
                    return;
                }
                throw ex.rethrowFromSystemServer();
            }
        }
    }

    private static final class ProviderRefCount {
        public final ProviderClientRecord client;
        public final IActivityManager.ContentProviderHolder holder;
        public boolean removePending;
        public int stableCount;
        public int unstableCount;

        ProviderRefCount(IActivityManager.ContentProviderHolder inHolder, ProviderClientRecord inClient, int sCount, int uCount) {
            this.holder = inHolder;
            this.client = inClient;
            this.stableCount = sCount;
            this.unstableCount = uCount;
        }
    }

    private void performStopActivityInner(ActivityClientRecord r, StopInfo info, boolean keepShown, boolean saveState, String reason) throws SuperNotCalledException {
        if (localLOGV) {
            Slog.v(TAG, "Performing stop of " + r);
        }
        if (r != null) {
            if (!keepShown && r.stopped) {
                if (r.activity.mFinished) {
                    return;
                }
                RuntimeException e = new RuntimeException("Performing stop of activity that is already stopped: " + r.intent.getComponent().toShortString());
                Slog.e(TAG, e.getMessage(), e);
                Slog.e(TAG, r.getStateString());
            }
            performPauseActivityIfNeeded(r, reason);
            if (info != null) {
                try {
                    info.description = r.activity.onCreateDescription();
                } catch (Exception e2) {
                    if (!this.mInstrumentation.onException(r.activity, e2)) {
                        throw new RuntimeException("Unable to save state of activity " + r.intent.getComponent().toShortString() + ": " + e2.toString(), e2);
                    }
                }
            }
            if (!r.activity.mFinished && saveState && r.state == null) {
                callCallActivityOnSaveInstanceState(r);
            }
            if (keepShown) {
                return;
            }
            try {
                r.activity.performStop(false);
            } catch (Exception e3) {
                if (!this.mInstrumentation.onException(r.activity, e3)) {
                    throw new RuntimeException("Unable to stop activity " + r.intent.getComponent().toShortString() + ": " + e3.toString(), e3);
                }
            }
            r.stopped = true;
            EventLog.writeEvent(LOG_AM_ON_STOP_CALLED, Integer.valueOf(UserHandle.myUserId()), r.activity.getComponentName().getClassName(), reason);
            if (!IS_USER_BUILD || DEBUG_LIFECYCLE) {
                Slog.d(TAG, "ACT-AM_ON_STOP_CALLED " + r + " reason:" + reason);
            }
        }
    }

    private void updateVisibility(ActivityClientRecord r, boolean show) throws SuperNotCalledException {
        View v = r.activity.mDecor;
        if (v != null) {
            if (!show) {
                if (r.activity.mVisibleFromServer) {
                    r.activity.mVisibleFromServer = false;
                    this.mNumVisibleActivities--;
                    v.setVisibility(4);
                    return;
                }
                return;
            }
            if (!r.activity.mVisibleFromServer) {
                r.activity.mVisibleFromServer = true;
                this.mNumVisibleActivities++;
                if (r.activity.mVisibleFromClient) {
                    r.activity.makeVisible();
                }
            }
            if (r.newConfig != null) {
                performConfigurationChangedForActivity(r, r.newConfig, true);
                if (DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Updating activity vis " + r.activityInfo.name + " with new config " + r.activity.mCurrentConfig);
                }
                r.newConfig = null;
            }
        }
    }

    private void handleStopActivity(IBinder token, boolean show, int configChanges, int seq) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(token);
        if (!checkAndUpdateLifecycleSeq(seq, r, "stopActivity")) {
            return;
        }
        r.activity.mConfigChangeFlags |= configChanges;
        StopInfo info = new StopInfo(null);
        performStopActivityInner(r, info, show, true, "handleStopActivity");
        if (localLOGV) {
            Slog.v(TAG, "Finishing stop of " + r + ": show=" + show + " win=" + r.window);
        }
        updateVisibility(r, show);
        if (!r.isPreHoneycomb()) {
            QueuedWork.waitToFinish();
        }
        info.activity = r;
        info.state = r.state;
        info.persistentState = r.persistentState;
        this.mH.post(info);
        this.mSomeActivitiesChanged = true;
    }

    private static boolean checkAndUpdateLifecycleSeq(int seq, ActivityClientRecord r, String action) {
        if (r == null) {
            return true;
        }
        if (seq < r.lastProcessedSeq) {
            if (DEBUG_ORDER) {
                Slog.d(TAG, action + " for " + r + " ignored, because seq=" + seq + " < mCurrentLifecycleSeq=" + r.lastProcessedSeq);
                return false;
            }
            return false;
        }
        r.lastProcessedSeq = seq;
        return true;
    }

    final void performRestartActivity(IBinder token) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (!r.stopped) {
            return;
        }
        r.activity.performRestart();
        r.stopped = false;
    }

    private void handleWindowVisibility(IBinder token, boolean show) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            Log.w(TAG, "handleWindowVisibility: no activity for token " + token);
            return;
        }
        if (!show && !r.stopped) {
            performStopActivityInner(r, null, show, false, "handleWindowVisibility");
        } else if (show && r.stopped) {
            unscheduleGcIdler();
            r.activity.performRestart();
            r.stopped = false;
        }
        if (r.activity.mDecor != null) {
            updateVisibility(r, show);
        }
        this.mSomeActivitiesChanged = true;
    }

    private void handleSleeping(IBinder token, boolean sleeping) {
        ActivityClientRecord r = this.mActivities.get(token);
        if (r == null) {
            Log.w(TAG, "handleSleeping: no activity for token " + token);
            return;
        }
        if (!sleeping) {
            if (r.stopped && r.activity.mVisibleFromServer) {
                r.activity.performRestart();
                r.stopped = false;
                return;
            }
            return;
        }
        if (!r.stopped && !r.isPreHoneycomb()) {
            try {
                r.activity.performStop(false);
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(r.activity, e)) {
                    throw new RuntimeException("Unable to stop activity " + r.intent.getComponent().toShortString() + ": " + e.toString(), e);
                }
            }
            r.stopped = true;
            EventLog.writeEvent(LOG_AM_ON_STOP_CALLED, Integer.valueOf(UserHandle.myUserId()), r.activity.getComponentName().getClassName(), "sleeping");
            if (!IS_USER_BUILD || DEBUG_LIFECYCLE) {
                Slog.d(TAG, "ACT-AM_ON_STOP_CALLED " + r + " sleeping");
            }
        }
        if (!r.isPreHoneycomb()) {
            QueuedWork.waitToFinish();
        }
        try {
            ActivityManagerNative.getDefault().activitySlept(r.token);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private void handleSetCoreSettings(Bundle coreSettings) throws Throwable {
        synchronized (this.mResourcesManager) {
            this.mCoreSettings = coreSettings;
        }
        onCoreSettingsChange();
    }

    private void onCoreSettingsChange() throws Throwable {
        boolean debugViewAttributes = this.mCoreSettings.getInt(Settings.Global.DEBUG_VIEW_ATTRIBUTES, 0) != 0;
        if (debugViewAttributes == View.mDebugViewAttributes) {
            return;
        }
        View.mDebugViewAttributes = debugViewAttributes;
        for (Map.Entry<IBinder, ActivityClientRecord> entry : this.mActivities.entrySet()) {
            requestRelaunchActivity(entry.getKey(), null, null, 0, false, null, null, false, false);
        }
    }

    private void handleUpdatePackageCompatibilityInfo(UpdateCompatibilityData data) throws SuperNotCalledException {
        LoadedApk apk = peekPackageInfo(data.pkg, false);
        if (apk != null) {
            apk.setCompatibilityInfo(data.info);
        }
        LoadedApk apk2 = peekPackageInfo(data.pkg, true);
        if (apk2 != null) {
            apk2.setCompatibilityInfo(data.info);
        }
        handleConfigurationChanged(this.mConfiguration, data.info);
        WindowManagerGlobal.getInstance().reportNewConfiguration(this.mConfiguration);
    }

    private void deliverResults(ActivityClientRecord r, List<ResultInfo> results) {
        int N = results.size();
        for (int i = 0; i < N; i++) {
            ResultInfo ri = results.get(i);
            try {
                if (ri.mData != null) {
                    ri.mData.setExtrasClassLoader(r.activity.getClassLoader());
                    ri.mData.prepareToEnterProcess();
                }
                if (DEBUG_RESULTS) {
                    Slog.v(TAG, "Delivering result to activity " + r + " : " + ri);
                }
                r.activity.dispatchActivityResult(ri.mResultWho, ri.mRequestCode, ri.mResultCode, ri.mData);
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(r.activity, e)) {
                    throw new RuntimeException("Failure delivering result " + ri + " to activity " + r.intent.getComponent().toShortString() + ": " + e.toString(), e);
                }
            }
        }
    }

    private void handleSendResult(ResultData res) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(res.token);
        if (DEBUG_RESULTS) {
            Slog.v(TAG, "Handling send result to " + r);
        }
        if (r != null) {
            boolean resumed = !r.paused;
            if (!r.activity.mFinished && r.activity.mDecor != null && r.hideForNow && resumed) {
                updateVisibility(r, true);
            }
            if (resumed) {
                try {
                    r.activity.mCalled = false;
                    r.activity.mTemporaryPause = true;
                    this.mInstrumentation.callActivityOnPause(r.activity);
                    if (!r.activity.mCalled) {
                        throw new SuperNotCalledException("Activity " + r.intent.getComponent().toShortString() + " did not call through to super.onPause()");
                    }
                } catch (SuperNotCalledException e) {
                    throw e;
                } catch (Exception e2) {
                    if (!this.mInstrumentation.onException(r.activity, e2)) {
                        throw new RuntimeException("Unable to pause activity " + r.intent.getComponent().toShortString() + ": " + e2.toString(), e2);
                    }
                }
            }
            deliverResults(r, res.results);
            if (resumed) {
                r.activity.performResume();
                r.activity.mTemporaryPause = false;
            }
        }
    }

    public final ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing) {
        return performDestroyActivity(token, finishing, 0, false);
    }

    private ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(token);
        Class<? extends Activity> activityClass = null;
        if (localLOGV) {
            Slog.v(TAG, "Performing finish of " + r);
        }
        if (r != null) {
            activityClass = r.activity.getClass();
            r.activity.mConfigChangeFlags |= configChanges;
            if (finishing) {
                r.activity.mFinished = true;
            }
            performPauseActivityIfNeeded(r, "destroy");
            if (!r.stopped) {
                try {
                    r.activity.performStop(r.mPreserveWindow);
                } catch (SuperNotCalledException e) {
                    throw e;
                } catch (Exception e2) {
                    if (!this.mInstrumentation.onException(r.activity, e2)) {
                        throw new RuntimeException("Unable to stop activity " + safeToComponentShortString(r.intent) + ": " + e2.toString(), e2);
                    }
                }
                r.stopped = true;
                EventLog.writeEvent(LOG_AM_ON_STOP_CALLED, Integer.valueOf(UserHandle.myUserId()), r.activity.getComponentName().getClassName(), "destroy");
                if (!IS_USER_BUILD || DEBUG_LIFECYCLE) {
                    Slog.d(TAG, "ACT-AM_ON_STOP_CALLED " + r + " destroy");
                }
            }
            if (getNonConfigInstance) {
                try {
                    r.lastNonConfigurationInstances = r.activity.retainNonConfigurationInstances();
                } catch (Exception e3) {
                    if (!this.mInstrumentation.onException(r.activity, e3)) {
                        throw new RuntimeException("Unable to retain activity " + r.intent.getComponent().toShortString() + ": " + e3.toString(), e3);
                    }
                }
            }
            try {
                r.activity.mCalled = false;
                this.mInstrumentation.callActivityOnDestroy(r.activity);
                if (!r.activity.mCalled) {
                    throw new SuperNotCalledException("Activity " + safeToComponentShortString(r.intent) + " did not call through to super.onDestroy()");
                }
                if (r.window != null) {
                    r.window.closeAllPanels();
                }
            } catch (Exception e4) {
                if (!this.mInstrumentation.onException(r.activity, e4)) {
                    throw new RuntimeException("Unable to destroy activity " + safeToComponentShortString(r.intent) + ": " + e4.toString(), e4);
                }
            } catch (SuperNotCalledException e5) {
                throw e5;
            }
        }
        this.mActivities.remove(token);
        StrictMode.decrementExpectedActivityCount(activityClass);
        return r;
    }

    private static String safeToComponentShortString(Intent intent) {
        ComponentName component = intent.getComponent();
        return component == null ? "[Unknown]" : component.toShortString();
    }

    private void handleDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance) throws SuperNotCalledException {
        ActivityClientRecord r = performDestroyActivity(token, finishing, configChanges, getNonConfigInstance);
        if (r != null) {
            cleanUpPendingRemoveWindows(r, finishing);
            WindowManager wm = r.activity.getWindowManager();
            View v = r.activity.mDecor;
            if (v != null) {
                if (r.activity.mVisibleFromServer) {
                    this.mNumVisibleActivities--;
                }
                IBinder wtoken = v.getWindowToken();
                if (r.activity.mWindowAdded) {
                    if (r.mPreserveWindow) {
                        r.mPendingRemoveWindow = r.window;
                        r.mPendingRemoveWindowManager = wm;
                        r.window.clearContentView();
                    } else {
                        wm.removeViewImmediate(v);
                    }
                }
                if (wtoken != null && r.mPendingRemoveWindow == null) {
                    WindowManagerGlobal.getInstance().closeAll(wtoken, r.activity.getClass().getName(), "Activity");
                } else if (r.mPendingRemoveWindow != null) {
                    WindowManagerGlobal.getInstance().closeAllExceptView(token, v, r.activity.getClass().getName(), "Activity");
                }
                r.activity.mDecor = null;
            }
            if (r.mPendingRemoveWindow == null) {
                WindowManagerGlobal.getInstance().closeAll(token, r.activity.getClass().getName(), "Activity");
            }
            Context c = r.activity.getBaseContext();
            if (c instanceof ContextImpl) {
                ((ContextImpl) c).scheduleFinalCleanup(r.activity.getClass().getName(), "Activity");
            }
        }
        if (finishing) {
            try {
                ActivityManagerNative.getDefault().activityDestroyed(token);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        this.mSomeActivitiesChanged = true;
    }

    public final void requestRelaunchActivity(IBinder token, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, int configChanges, boolean notResumed, Configuration config, Configuration overrideConfig, boolean fromServer, boolean preserveWindow) throws Throwable {
        ActivityClientRecord target;
        ActivityClientRecord target2;
        synchronized (this.mResourcesManager) {
            int i = 0;
            while (true) {
                try {
                } catch (Throwable th) {
                    th = th;
                }
                if (i >= this.mRelaunchingActivities.size()) {
                    target = null;
                    break;
                }
                ActivityClientRecord r = this.mRelaunchingActivities.get(i);
                if (DEBUG_ORDER) {
                    Slog.d(TAG, "requestRelaunchActivity: " + this + ", trying: " + r);
                }
                if (r.token == token) {
                    break;
                } else {
                    i++;
                }
                throw th;
            }
            if (target == null) {
                try {
                    if (DEBUG_ORDER) {
                        Slog.d(TAG, "requestRelaunchActivity: target is null, fromServer:" + fromServer);
                    }
                    target2 = new ActivityClientRecord();
                    target2.token = token;
                    target2.pendingResults = pendingResults;
                    target2.pendingIntents = pendingNewIntents;
                    target2.mPreserveWindow = preserveWindow;
                    if (!fromServer) {
                        ActivityClientRecord existing = this.mActivities.get(token);
                        if (DEBUG_ORDER) {
                            Slog.d(TAG, "requestRelaunchActivity: " + existing);
                        }
                        if (existing != null) {
                            if (DEBUG_ORDER) {
                                Slog.d(TAG, "requestRelaunchActivity: paused= " + existing.paused);
                            }
                            target2.startsNotResumed = existing.paused;
                            target2.overrideConfig = existing.overrideConfig;
                            if (!IS_USER_BUILD || DEBUG_LIFECYCLE) {
                                Slog.d(TAG, "requestRelaunchActivity startsNotResumed=" + target2.startsNotResumed);
                            }
                        }
                        target2.onlyLocalRequest = true;
                    }
                    this.mRelaunchingActivities.add(target2);
                    sendMessage(126, target2);
                } catch (Throwable th2) {
                    th = th2;
                }
            } else {
                target2 = target;
            }
            if (fromServer) {
                target2.startsNotResumed = notResumed;
                target2.onlyLocalRequest = false;
            }
            if (config != null) {
                target2.createdConfig = config;
            }
            if (overrideConfig != null) {
                target2.overrideConfig = overrideConfig;
            }
            target2.pendingConfigChanges |= configChanges;
            target2.relaunchSeq = getLifecycleSeq();
            if (!DEBUG_ORDER) {
                return;
            }
            Slog.d(TAG, "relaunchActivity " + this + ", target " + target2 + " operation received seq: " + target2.relaunchSeq);
        }
    }

    private void handleRelaunchActivity(ActivityClientRecord tmp) throws SuperNotCalledException {
        unscheduleGcIdler();
        this.mSomeActivitiesChanged = true;
        Configuration changedConfig = null;
        int configChanges = 0;
        synchronized (this.mResourcesManager) {
            int N = this.mRelaunchingActivities.size();
            IBinder token = tmp.token;
            ActivityClientRecord tmp2 = null;
            int i = 0;
            while (i < N) {
                ActivityClientRecord r = this.mRelaunchingActivities.get(i);
                if (r.token == token) {
                    tmp2 = r;
                    configChanges |= r.pendingConfigChanges;
                    this.mRelaunchingActivities.remove(i);
                    i--;
                    N--;
                }
                i++;
            }
            if (tmp2 == null) {
                if (DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Abort, activity not relaunching!");
                }
                return;
            }
            if (DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Relaunching activity " + tmp2.token + " with configChanges=0x" + Integer.toHexString(configChanges));
            }
            if (this.mPendingConfiguration != null) {
                changedConfig = this.mPendingConfiguration;
                this.mPendingConfiguration = null;
            }
            if (tmp2.lastProcessedSeq > tmp2.relaunchSeq) {
                Slog.wtf(TAG, "For some reason target: " + tmp2 + " has lower sequence: " + tmp2.relaunchSeq + " than current sequence: " + tmp2.lastProcessedSeq);
            } else {
                tmp2.lastProcessedSeq = tmp2.relaunchSeq;
            }
            if (tmp2.createdConfig != null && ((this.mConfiguration == null || (tmp2.createdConfig.isOtherSeqNewer(this.mConfiguration) && this.mConfiguration.diff(tmp2.createdConfig) != 0)) && (changedConfig == null || tmp2.createdConfig.isOtherSeqNewer(changedConfig)))) {
                changedConfig = tmp2.createdConfig;
            }
            if (DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Relaunching activity " + tmp2.token + ": changedConfig=" + changedConfig);
            }
            if (changedConfig != null) {
                this.mCurDefaultDisplayDpi = changedConfig.densityDpi;
                updateDefaultDensity();
                handleConfigurationChanged(changedConfig, null);
            }
            ActivityClientRecord r2 = this.mActivities.get(tmp2.token);
            if (DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Handling relaunch of " + r2);
            }
            if (r2 == null) {
                if (tmp2.onlyLocalRequest) {
                    return;
                }
                try {
                    ActivityManagerNative.getDefault().activityRelaunched(tmp2.token);
                    return;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            r2.activity.mConfigChangeFlags |= configChanges;
            r2.onlyLocalRequest = tmp2.onlyLocalRequest;
            r2.mPreserveWindow = tmp2.mPreserveWindow;
            r2.lastProcessedSeq = tmp2.lastProcessedSeq;
            r2.relaunchSeq = tmp2.relaunchSeq;
            Intent currentIntent = r2.activity.mIntent;
            r2.activity.mChangingConfigurations = true;
            try {
                if (r2.mPreserveWindow || r2.onlyLocalRequest) {
                    WindowManagerGlobal.getWindowSession().prepareToReplaceWindows(r2.token, !r2.onlyLocalRequest);
                }
                if (!r2.paused) {
                    performPauseActivity(r2.token, false, r2.isPreHoneycomb(), "handleRelaunchActivity");
                }
                if (r2.state == null && !r2.stopped && !r2.isPreHoneycomb()) {
                    callCallActivityOnSaveInstanceState(r2);
                }
                handleDestroyActivity(r2.token, false, configChanges, true);
                r2.activity = null;
                r2.window = null;
                r2.hideForNow = false;
                r2.nextIdle = null;
                if (tmp2.pendingResults != null) {
                    if (r2.pendingResults == null) {
                        r2.pendingResults = tmp2.pendingResults;
                    } else {
                        r2.pendingResults.addAll(tmp2.pendingResults);
                    }
                }
                if (tmp2.pendingIntents != null) {
                    if (r2.pendingIntents == null) {
                        r2.pendingIntents = tmp2.pendingIntents;
                    } else {
                        r2.pendingIntents.addAll(tmp2.pendingIntents);
                    }
                }
                r2.startsNotResumed = tmp2.startsNotResumed;
                r2.overrideConfig = tmp2.overrideConfig;
                handleLaunchActivity(r2, currentIntent, "handleRelaunchActivity");
                if (tmp2.onlyLocalRequest) {
                    return;
                }
                try {
                    ActivityManagerNative.getDefault().activityRelaunched(r2.token);
                    if (r2.window != null) {
                        r2.window.reportActivityRelaunched();
                    }
                } catch (RemoteException e2) {
                    throw e2.rethrowFromSystemServer();
                }
            } catch (RemoteException e3) {
                throw e3.rethrowFromSystemServer();
            }
        }
    }

    private void callCallActivityOnSaveInstanceState(ActivityClientRecord r) {
        r.state = new Bundle();
        r.state.setAllowFds(false);
        if (r.isPersistable()) {
            r.persistentState = new PersistableBundle();
            this.mInstrumentation.callActivityOnSaveInstanceState(r.activity, r.state, r.persistentState);
        } else {
            this.mInstrumentation.callActivityOnSaveInstanceState(r.activity, r.state);
        }
    }

    ArrayList<ComponentCallbacks2> collectComponentCallbacks(boolean allActivities, Configuration newConfig) {
        ArrayList<ComponentCallbacks2> callbacks = new ArrayList<>();
        synchronized (this.mResourcesManager) {
            int NAPP = this.mAllApplications.size();
            for (int i = 0; i < NAPP; i++) {
                callbacks.add(this.mAllApplications.get(i));
            }
            int NACT = this.mActivities.size();
            for (int i2 = 0; i2 < NACT; i2++) {
                ActivityClientRecord ar = this.mActivities.valueAt(i2);
                Activity a = ar.activity;
                if (a != null) {
                    Configuration thisConfig = applyConfigCompatMainThread(this.mCurDefaultDisplayDpi, newConfig, ar.packageInfo.getCompatibilityInfo());
                    if (!ar.activity.mFinished && (allActivities || !ar.paused)) {
                        callbacks.add(a);
                    } else if (thisConfig != null) {
                        if (DEBUG_CONFIGURATION) {
                            Slog.v(TAG, "Setting activity " + ar.activityInfo.name + " newConfig=" + thisConfig);
                        }
                        ar.newConfig = thisConfig;
                    }
                }
            }
            int NSVC = this.mServices.size();
            for (int i3 = 0; i3 < NSVC; i3++) {
                callbacks.add(this.mServices.valueAt(i3));
            }
        }
        synchronized (this.mProviderMap) {
            int NPRV = this.mLocalProviders.size();
            for (int i4 = 0; i4 < NPRV; i4++) {
                callbacks.add(this.mLocalProviders.valueAt(i4).mLocalProvider);
            }
        }
        return callbacks;
    }

    private void performConfigurationChangedForActivity(ActivityClientRecord r, Configuration newBaseConfig, boolean reportToActivity) throws SuperNotCalledException {
        r.tmpConfig.setTo(newBaseConfig);
        if (r.overrideConfig != null) {
            r.tmpConfig.updateFrom(r.overrideConfig);
        }
        performConfigurationChanged(r.activity, r.token, r.tmpConfig, r.overrideConfig, reportToActivity);
        freeTextLayoutCachesIfNeeded(r.activity.mCurrentConfig.diff(r.tmpConfig));
    }

    private static Configuration createNewConfigAndUpdateIfNotNull(Configuration base, Configuration override) {
        if (override == null) {
            return base;
        }
        Configuration newConfig = new Configuration(base);
        newConfig.updateFrom(override);
        return newConfig;
    }

    private void performConfigurationChanged(ComponentCallbacks2 componentCallbacks2, IBinder activityToken, Configuration newConfig, Configuration amOverrideConfig, boolean reportToActivity) throws SuperNotCalledException {
        Activity activity = componentCallbacks2 instanceof Activity ? (Activity) componentCallbacks2 : null;
        if (activity != null) {
            activity.mCalled = false;
        }
        boolean shouldChangeConfig = false;
        if (activity == null || activity.mCurrentConfig == null) {
            shouldChangeConfig = true;
        } else {
            int diff = activity.mCurrentConfig.diff(newConfig);
            if (diff != 0) {
                shouldChangeConfig = true;
            }
        }
        if (!shouldChangeConfig) {
            return;
        }
        Configuration contextThemeWrapperOverrideConfig = null;
        if (componentCallbacks2 instanceof ContextThemeWrapper) {
            ContextThemeWrapper contextThemeWrapper = (ContextThemeWrapper) componentCallbacks2;
            contextThemeWrapperOverrideConfig = contextThemeWrapper.getOverrideConfiguration();
        }
        if (activityToken != null) {
            Configuration finalOverrideConfig = createNewConfigAndUpdateIfNotNull(amOverrideConfig, contextThemeWrapperOverrideConfig);
            this.mResourcesManager.updateResourcesForActivity(activityToken, finalOverrideConfig);
        }
        if (reportToActivity) {
            Configuration configToReport = createNewConfigAndUpdateIfNotNull(newConfig, contextThemeWrapperOverrideConfig);
            componentCallbacks2.onConfigurationChanged(configToReport);
        }
        if (activity == null) {
            return;
        }
        if (reportToActivity && !activity.mCalled) {
            throw new SuperNotCalledException("Activity " + activity.getLocalClassName() + " did not call through to super.onConfigurationChanged()");
        }
        activity.mConfigChangeFlags = 0;
        activity.mCurrentConfig = new Configuration(newConfig);
    }

    public final void applyConfigurationToResources(Configuration config) {
        synchronized (this.mResourcesManager) {
            this.mResourcesManager.applyConfigurationToResourcesLocked(config, null);
        }
    }

    final Configuration applyCompatConfiguration(int displayDensity) {
        Configuration config = this.mConfiguration;
        if (this.mCompatConfiguration == null) {
            this.mCompatConfiguration = new Configuration();
        }
        this.mCompatConfiguration.setTo(this.mConfiguration);
        if (this.mResourcesManager.applyCompatConfigurationLocked(displayDensity, this.mCompatConfiguration)) {
            Configuration config2 = this.mCompatConfiguration;
            return config2;
        }
        return config;
    }

    final void handleConfigurationChanged(Configuration config, CompatibilityInfo compat) throws SuperNotCalledException {
        synchronized (this.mResourcesManager) {
            if (this.mPendingConfiguration != null) {
                if (!this.mPendingConfiguration.isOtherSeqNewer(config)) {
                    config = this.mPendingConfiguration;
                    this.mCurDefaultDisplayDpi = config.densityDpi;
                    updateDefaultDensity();
                }
                this.mPendingConfiguration = null;
            }
            if (config == null) {
                return;
            }
            if (DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Handle configuration changed: " + config);
            }
            this.mResourcesManager.applyConfigurationToResourcesLocked(config, compat);
            updateLocaleListFromAppContext(this.mInitialApplication.getApplicationContext(), this.mResourcesManager.getConfiguration().getLocales());
            if (this.mConfiguration == null) {
                this.mConfiguration = new Configuration();
            }
            if (!this.mConfiguration.isOtherSeqNewer(config) && compat == null) {
                return;
            }
            int configDiff = this.mConfiguration.updateFrom(config);
            Configuration config2 = applyCompatConfiguration(this.mCurDefaultDisplayDpi);
            Resources.Theme systemTheme = getSystemContext().getTheme();
            if ((systemTheme.getChangingConfigurations() & configDiff) != 0) {
                systemTheme.rebase();
            }
            ArrayList<ComponentCallbacks2> callbacks = collectComponentCallbacks(false, config2);
            freeTextLayoutCachesIfNeeded(configDiff);
            if (callbacks == null) {
                return;
            }
            int N = callbacks.size();
            for (int i = 0; i < N; i++) {
                ComponentCallbacks2 cb = callbacks.get(i);
                if (cb instanceof Activity) {
                    Activity a = (Activity) cb;
                    performConfigurationChangedForActivity(this.mActivities.get(a.getActivityToken()), config2, true);
                } else {
                    performConfigurationChanged(cb, null, config2, null, true);
                }
            }
        }
    }

    static void freeTextLayoutCachesIfNeeded(int configDiff) {
        if (configDiff == 0) {
            return;
        }
        boolean hasLocaleConfigChange = (configDiff & 4) != 0;
        if (!hasLocaleConfigChange) {
            return;
        }
        Canvas.freeTextLayoutCaches();
        if (DEBUG_CONFIGURATION) {
            Slog.v(TAG, "Cleared TextLayout Caches");
        }
    }

    final void handleActivityConfigurationChanged(ActivityConfigChangeData data, boolean reportToActivity) throws SuperNotCalledException {
        ActivityClientRecord r = this.mActivities.get(data.activityToken);
        if (r == null || r.activity == null) {
            return;
        }
        if (DEBUG_CONFIGURATION) {
            Slog.v(TAG, "Handle activity config changed: " + r.activityInfo.name + ", with callback=" + reportToActivity);
        }
        r.overrideConfig = data.overrideConfig;
        performConfigurationChangedForActivity(r, this.mCompatConfiguration, reportToActivity);
        this.mSomeActivitiesChanged = true;
    }

    final void handleProfilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) {
        try {
            if (start) {
                try {
                    this.mProfiler.setProfiler(profilerInfo);
                    this.mProfiler.startProfiling();
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Profiling failed on path " + profilerInfo.profileFile + " -- can the process access this path?");
                    try {
                        profilerInfo.profileFd.close();
                    } catch (IOException e2) {
                        Slog.w(TAG, "Failure closing profile fd", e2);
                    }
                }
                return;
            }
            this.mProfiler.stopProfiling();
        } finally {
            try {
                profilerInfo.profileFd.close();
            } catch (IOException e3) {
                Slog.w(TAG, "Failure closing profile fd", e3);
            }
        }
    }

    public void stopProfiling() {
        this.mProfiler.stopProfiling();
    }

    static final void handleDumpHeap(boolean managed, DumpHeapData dhd) {
        if (managed) {
            try {
                try {
                    Debug.dumpHprofData(dhd.path, dhd.fd.getFileDescriptor());
                } catch (IOException e) {
                    Slog.w(TAG, "Managed heap dump failed on path " + dhd.path + " -- can the process access this path?");
                    try {
                        dhd.fd.close();
                    } catch (IOException e2) {
                        Slog.w(TAG, "Failure closing profile fd", e2);
                    }
                }
            } finally {
                try {
                    dhd.fd.close();
                } catch (IOException e3) {
                    Slog.w(TAG, "Failure closing profile fd", e3);
                }
            }
        } else {
            Debug.dumpNativeHeap(dhd.fd.getFileDescriptor());
        }
        try {
            ActivityManagerNative.getDefault().dumpHeapFinished(dhd.path);
        } catch (RemoteException e4) {
            throw e4.rethrowFromSystemServer();
        }
    }

    final void handleDispatchPackageBroadcast(int cmd, String[] packages) {
        ResourcesManager resourcesManager;
        boolean hasPkgInfo = false;
        switch (cmd) {
            case 0:
            case 2:
                boolean killApp = cmd == 0;
                if (packages != null) {
                    resourcesManager = this.mResourcesManager;
                    synchronized (resourcesManager) {
                        for (int i = packages.length - 1; i >= 0; i--) {
                            if (!hasPkgInfo) {
                                WeakReference<LoadedApk> ref = this.mPackages.get(packages[i]);
                                if (ref != null && ref.get() != null) {
                                    hasPkgInfo = true;
                                } else {
                                    WeakReference<LoadedApk> ref2 = this.mResourcePackages.get(packages[i]);
                                    if (ref2 != null && ref2.get() != null) {
                                        hasPkgInfo = true;
                                    }
                                }
                            }
                            if (killApp) {
                                this.mPackages.remove(packages[i]);
                                this.mResourcePackages.remove(packages[i]);
                            }
                        }
                    }
                }
                ApplicationPackageManager.handlePackageBroadcast(cmd, packages, hasPkgInfo);
                break;
            case 1:
            default:
                ApplicationPackageManager.handlePackageBroadcast(cmd, packages, hasPkgInfo);
            case 3:
                if (packages != null) {
                    resourcesManager = this.mResourcesManager;
                    synchronized (resourcesManager) {
                        for (int i2 = packages.length - 1; i2 >= 0; i2--) {
                            WeakReference<LoadedApk> ref3 = this.mPackages.get(packages[i2]);
                            LoadedApk pkgInfo = ref3 != null ? ref3.get() : null;
                            if (pkgInfo != null) {
                                hasPkgInfo = true;
                            } else {
                                WeakReference<LoadedApk> ref4 = this.mResourcePackages.get(packages[i2]);
                                pkgInfo = ref4 != null ? ref4.get() : null;
                                if (pkgInfo != null) {
                                    hasPkgInfo = true;
                                }
                            }
                            if (pkgInfo != null) {
                                try {
                                    String packageName = packages[i2];
                                    ApplicationInfo aInfo = sPackageManager.getApplicationInfo(packageName, 0, UserHandle.myUserId());
                                    if (this.mActivities.size() > 0) {
                                        for (ActivityClientRecord ar : this.mActivities.values()) {
                                            if (ar.activityInfo.applicationInfo.packageName.equals(packageName)) {
                                                ar.activityInfo.applicationInfo = aInfo;
                                                ar.packageInfo = pkgInfo;
                                            }
                                        }
                                    }
                                    List<String> oldPaths = sPackageManager.getPreviousCodePaths(packageName);
                                    pkgInfo.updateApplicationInfo(aInfo, oldPaths);
                                } catch (RemoteException e) {
                                }
                            }
                        }
                    }
                }
                ApplicationPackageManager.handlePackageBroadcast(cmd, packages, hasPkgInfo);
                break;
        }
        ApplicationPackageManager.handlePackageBroadcast(cmd, packages, hasPkgInfo);
    }

    final void handleLowMemory() {
        ArrayList<ComponentCallbacks2> callbacks = collectComponentCallbacks(true, null);
        int N = callbacks.size();
        for (int i = 0; i < N; i++) {
            callbacks.get(i).onLowMemory();
        }
        if (Process.myUid() != 1000) {
            int sqliteReleased = SQLiteDatabase.releaseMemory();
            EventLog.writeEvent(SQLITE_MEM_RELEASED_EVENT_LOG_TAG, sqliteReleased);
        }
        Canvas.freeCaches();
        Canvas.freeTextLayoutCaches();
        BinderInternal.forceGc("mem");
    }

    final void handleTrimMemory(int level) {
        if (DEBUG_MEMORY_TRIM) {
            Slog.v(TAG, "Trimming memory to level: " + level);
        }
        ArrayList<ComponentCallbacks2> callbacks = collectComponentCallbacks(true, null);
        int N = callbacks.size();
        for (int i = 0; i < N; i++) {
            callbacks.get(i).onTrimMemory(level);
        }
        WindowManagerGlobal.getInstance().trimMemory(level);
    }

    private void setupGraphicsSupport(LoadedApk info, File cacheDir) {
        if (Process.isIsolated()) {
            return;
        }
        Trace.traceBegin(64L, "setupGraphicsSupport");
        try {
            try {
                int uid = Process.myUid();
                String[] packages = getPackageManager().getPackagesForUid(uid);
                if (packages != null && packages.length == 1) {
                    ThreadedRenderer.setupDiskCache(cacheDir);
                    RenderScriptCacheDir.setupDiskCache(cacheDir);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } finally {
            Trace.traceEnd(64L);
        }
    }

    private void updateDefaultDensity() {
        int densityDpi = this.mCurDefaultDisplayDpi;
        if (this.mDensityCompatMode || densityDpi == 0 || densityDpi == DisplayMetrics.DENSITY_DEVICE) {
            return;
        }
        DisplayMetrics.DENSITY_DEVICE = densityDpi;
        Bitmap.setDefaultDensity(densityDpi);
    }

    private String getInstrumentationLibrary(ApplicationInfo appInfo, InstrumentationInfo insInfo) {
        if (appInfo.primaryCpuAbi != null && appInfo.secondaryCpuAbi != null) {
            String secondaryIsa = VMRuntime.getInstructionSet(appInfo.secondaryCpuAbi);
            String secondaryDexCodeIsa = SystemProperties.get("ro.dalvik.vm.isa." + secondaryIsa);
            if (!secondaryDexCodeIsa.isEmpty()) {
                secondaryIsa = secondaryDexCodeIsa;
            }
            String runtimeIsa = VMRuntime.getRuntime().vmInstructionSet();
            if (runtimeIsa.equals(secondaryIsa)) {
                return insInfo.secondaryNativeLibraryDir;
            }
        }
        return insInfo.nativeLibraryDir;
    }

    private void updateLocaleListFromAppContext(Context context, LocaleList newLocaleList) {
        Locale bestLocale = context.getResources().getConfiguration().getLocales().get(0);
        int newLocaleListSize = newLocaleList.size();
        for (int i = 0; i < newLocaleListSize; i++) {
            if (bestLocale.equals(newLocaleList.get(i))) {
                LocaleList.setDefault(newLocaleList, i);
                return;
            }
        }
        LocaleList.setDefault(new LocaleList(bestLocale, newLocaleList));
    }

    private void handleBindApplication(AppBindData data) {
        InstrumentationInfo ii;
        VMRuntime.registerSensitiveThread();
        if (data.trackAllocation) {
            DdmVmInternal.enableRecentAllocations(true);
        }
        Process.setStartTimes(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        this.mBoundApplication = data;
        this.mConfiguration = new Configuration(data.config);
        this.mCompatConfiguration = new Configuration(data.config);
        this.mProfiler = new Profiler();
        if (data.initProfilerInfo != null) {
            this.mProfiler.profileFile = data.initProfilerInfo.profileFile;
            this.mProfiler.profileFd = data.initProfilerInfo.profileFd;
            this.mProfiler.samplingInterval = data.initProfilerInfo.samplingInterval;
            this.mProfiler.autoStopProfiler = data.initProfilerInfo.autoStopProfiler;
        }
        Process.setArgV0(data.processName);
        DdmHandleAppName.setAppName(data.processName, UserHandle.myUserId());
        if (data.persistent && !ActivityManager.isHighEndGfx() && !"com.android.systemui".equals(data.processName)) {
            ThreadedRenderer.disable(false);
        }
        if (this.mProfiler.profileFd != null) {
            this.mProfiler.startProfiling();
        }
        if (data.appInfo.targetSdkVersion <= 12) {
            AsyncTask.setDefaultExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        Message.updateCheckRecycle(data.appInfo.targetSdkVersion);
        TimeZone.setDefault(null);
        LocaleList.setDefault(data.config.getLocales());
        synchronized (this.mResourcesManager) {
            this.mResourcesManager.applyConfigurationToResourcesLocked(data.config, data.compatInfo);
            this.mCurDefaultDisplayDpi = data.config.densityDpi;
            applyCompatConfiguration(this.mCurDefaultDisplayDpi);
        }
        data.info = getPackageInfoNoCheck(data.appInfo, data.compatInfo);
        if ((data.appInfo.flags & 8192) == 0) {
            this.mDensityCompatMode = true;
            Bitmap.setDefaultDensity(160);
        }
        updateDefaultDensity();
        boolean is24Hr = "24".equals(this.mCoreSettings.getString(Settings.System.TIME_12_24));
        DateFormat.set24HourTimePref(is24Hr);
        View.mDebugViewAttributes = this.mCoreSettings.getInt(Settings.Global.DEBUG_VIEW_ATTRIBUTES, 0) != 0;
        if ((data.appInfo.flags & 129) != 0) {
            StrictMode.conditionallyEnableDebugLogging();
        }
        if (data.appInfo.targetSdkVersion >= 11) {
            StrictMode.enableDeathOnNetwork();
        }
        if (data.appInfo.targetSdkVersion >= 24) {
            StrictMode.enableDeathOnFileUriExposure();
        }
        NetworkSecurityPolicy.getInstance().setCleartextTrafficPermitted((data.appInfo.flags & 134217728) != 0);
        if (data.debugMode != 0) {
            Debug.changeDebugPort(8100);
            if (data.debugMode == 2) {
                Slog.w(TAG, "Application " + data.info.getPackageName() + " is waiting for the debugger on port 8100...");
                IActivityManager mgr = ActivityManagerNative.getDefault();
                try {
                    mgr.showWaitingForDebugger(this.mAppThread, true);
                    Debug.waitForDebugger();
                    try {
                        mgr.showWaitingForDebugger(this.mAppThread, false);
                    } catch (RemoteException ex) {
                        throw ex.rethrowFromSystemServer();
                    }
                } catch (RemoteException ex2) {
                    throw ex2.rethrowFromSystemServer();
                }
            } else {
                Slog.w(TAG, "Application " + data.info.getPackageName() + " can be debugged on port 8100...");
            }
        }
        boolean isAppDebuggable = (data.appInfo.flags & 2) != 0;
        Trace.setAppTracingAllowed(isAppDebuggable);
        if (isAppDebuggable && data.enableBinderTracking) {
            Binder.enableTracing();
        }
        Trace.traceBegin(64L, "Setup proxies");
        IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
        if (b != null) {
            IConnectivityManager service = IConnectivityManager.Stub.asInterface(b);
            try {
                ProxyInfo proxyInfo = service.getProxyForNetwork(null);
                Proxy.setHttpProxySystemProperty(proxyInfo);
            } catch (RemoteException e) {
                Trace.traceEnd(64L);
                throw e.rethrowFromSystemServer();
            }
        }
        Trace.traceEnd(64L);
        if (data.instrumentationName != null) {
            try {
                ii = new ApplicationPackageManager(null, getPackageManager()).getInstrumentationInfo(data.instrumentationName, 0);
                this.mInstrumentationPackageName = ii.packageName;
                this.mInstrumentationAppDir = ii.sourceDir;
                this.mInstrumentationSplitAppDirs = ii.splitSourceDirs;
                this.mInstrumentationLibDir = getInstrumentationLibrary(data.appInfo, ii);
                this.mInstrumentedAppDir = data.info.getAppDir();
                this.mInstrumentedSplitAppDirs = data.info.getSplitAppDirs();
                this.mInstrumentedLibDir = data.info.getLibDir();
            } catch (PackageManager.NameNotFoundException e2) {
                throw new RuntimeException("Unable to find instrumentation info for: " + data.instrumentationName);
            }
        } else {
            ii = null;
        }
        Context appContext = ContextImpl.createAppContext(this, data.info);
        updateLocaleListFromAppContext(appContext, this.mResourcesManager.getConfiguration().getLocales());
        if (!Process.isIsolated() && !ZenModeConfig.SYSTEM_AUTHORITY.equals(appContext.getPackageName())) {
            File cacheDir = appContext.getCacheDir();
            if (cacheDir != null) {
                System.setProperty("java.io.tmpdir", cacheDir.getAbsolutePath());
            } else {
                Log.v(TAG, "Unable to initialize \"java.io.tmpdir\" property due to missing cache directory");
            }
            Context deviceContext = appContext.createDeviceProtectedStorageContext();
            File codeCacheDir = deviceContext.getCodeCacheDir();
            if (codeCacheDir != null) {
                setupGraphicsSupport(data.info, codeCacheDir);
            } else {
                Log.e(TAG, "Unable to setupGraphicsSupport due to missing code-cache directory");
            }
        }
        Trace.traceBegin(64L, "NetworkSecurityConfigProvider.install");
        NetworkSecurityConfigProvider.install(appContext);
        Trace.traceEnd(64L);
        if (ii != null) {
            ApplicationInfo instrApp = new ApplicationInfo();
            ii.copyTo(instrApp);
            instrApp.initForUser(UserHandle.myUserId());
            LoadedApk pi = getPackageInfo(instrApp, data.compatInfo, appContext.getClassLoader(), false, true, false);
            ContextImpl instrContext = ContextImpl.createAppContext(this, pi);
            try {
                ClassLoader cl = instrContext.getClassLoader();
                this.mInstrumentation = (Instrumentation) cl.loadClass(data.instrumentationName.getClassName()).newInstance();
                ComponentName component = new ComponentName(ii.packageName, ii.name);
                this.mInstrumentation.init(this, instrContext, appContext, component, data.instrumentationWatcher, data.instrumentationUiAutomationConnection);
                if (this.mProfiler.profileFile != null && !ii.handleProfiling && this.mProfiler.profileFd == null) {
                    this.mProfiler.handlingProfiling = true;
                    File file = new File(this.mProfiler.profileFile);
                    file.getParentFile().mkdirs();
                    Debug.startMethodTracing(file.toString(), 8388608);
                }
            } catch (Exception e3) {
                throw new RuntimeException("Unable to instantiate instrumentation " + data.instrumentationName + ": " + e3.toString(), e3);
            }
        } else {
            this.mInstrumentation = new Instrumentation();
        }
        if ((data.appInfo.flags & 1048576) != 0) {
            VMRuntime.getRuntime().clearGrowthLimit();
        } else {
            VMRuntime.getRuntime().clampGrowthLimit();
        }
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
        try {
            Application app = data.info.makeApplication(data.restrictedBackupMode, null);
            this.mInitialApplication = app;
            if (!data.restrictedBackupMode && !ArrayUtils.isEmpty(data.providers)) {
                installContentProviders(app, data.providers);
                this.mH.sendEmptyMessageDelayed(132, 10000L);
            }
            try {
                this.mInstrumentation.onCreate(data.instrumentationArgs);
                try {
                    this.mInstrumentation.callApplicationOnCreate(app);
                } catch (Exception e4) {
                    if (!this.mInstrumentation.onException(app, e4)) {
                        throw new RuntimeException("Unable to create application " + app.getClass().getName() + ": " + e4.toString(), e4);
                    }
                }
            } catch (Exception e5) {
                throw new RuntimeException("Exception thrown in onCreate() of " + data.instrumentationName + ": " + e5.toString(), e5);
            }
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    final void finishInstrumentation(int resultCode, Bundle results) {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (this.mProfiler.profileFile != null && this.mProfiler.handlingProfiling && this.mProfiler.profileFd == null) {
            Debug.stopMethodTracing();
        }
        try {
            am.finishInstrumentation(this.mAppThread, resultCode, results);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private void installContentProviders(Context context, List<ProviderInfo> providers) throws Throwable {
        ArrayList<IActivityManager.ContentProviderHolder> results = new ArrayList<>();
        for (ProviderInfo cpi : providers) {
            if (DEBUG_PROVIDER) {
                StringBuilder buf = new StringBuilder(128);
                buf.append("Pub ");
                buf.append(cpi.authority);
                buf.append(": ");
                buf.append(cpi.name);
                Log.i(TAG, buf.toString());
            }
            IActivityManager.ContentProviderHolder cph = installProvider(context, null, cpi, false, true, true);
            if (cph != null) {
                cph.noReleaseNeeded = true;
                results.add(cph);
            }
        }
        try {
            ActivityManagerNative.getDefault().publishContentProviders(getApplicationThread(), results);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public final IContentProvider acquireProvider(Context c, String auth, int userId, boolean stable) {
        IContentProvider provider = acquireExistingProvider(c, auth, userId, stable);
        if (provider != null) {
            return provider;
        }
        if (WifiEnterpriseConfig.ENGINE_ENABLE.equals(SystemProperties.get("persist.runningbooster.support")) || WifiEnterpriseConfig.ENGINE_ENABLE.equals(SystemProperties.get("ro.mtk_aws_support"))) {
            try {
                int suppressAction = ActivityManagerNative.getDefault().readyToGetContentProvider(getApplicationThread(), auth, userId);
                if (suppressAction == 1) {
                    try {
                        Thread.sleep(MIN_TIME_BETWEEN_GCS);
                    } catch (InterruptedException ex) {
                        Slog.e(TAG, "InterruptedException " + ex);
                    }
                }
            } catch (RemoteException ex2) {
                Slog.e(TAG, "RemoteException " + ex2);
            }
        }
        try {
            IActivityManager.ContentProviderHolder holder = ActivityManagerNative.getDefault().getContentProvider(getApplicationThread(), auth, userId, stable);
            if (holder == null) {
                Slog.e(TAG, "Failed to find provider info for " + auth);
                return null;
            }
            return installProvider(c, holder, holder.info, true, holder.noReleaseNeeded, stable).provider;
        } catch (RemoteException ex3) {
            throw ex3.rethrowFromSystemServer();
        }
    }

    private final void incProviderRefLocked(ProviderRefCount prc, boolean stable) {
        int unstableDelta;
        if (stable) {
            prc.stableCount++;
            if (prc.stableCount != 1) {
                return;
            }
            if (prc.removePending) {
                unstableDelta = -1;
                if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "incProviderRef: stable snatched provider from the jaws of death");
                }
                prc.removePending = false;
                this.mH.removeMessages(131, prc);
            } else {
                unstableDelta = 0;
            }
            try {
                if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "incProviderRef Now stable - " + prc.holder.info.name + ": unstableDelta=" + unstableDelta);
                }
                ActivityManagerNative.getDefault().refContentProvider(prc.holder.connection, 1, unstableDelta);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        prc.unstableCount++;
        if (prc.unstableCount != 1) {
            return;
        }
        if (prc.removePending) {
            if (DEBUG_PROVIDER) {
                Slog.v(TAG, "incProviderRef: unstable snatched provider from the jaws of death");
            }
            prc.removePending = false;
            this.mH.removeMessages(131, prc);
            return;
        }
        try {
            if (DEBUG_PROVIDER) {
                Slog.v(TAG, "incProviderRef: Now unstable - " + prc.holder.info.name);
            }
            ActivityManagerNative.getDefault().refContentProvider(prc.holder.connection, 0, 1);
        } catch (RemoteException e2) {
        }
    }

    public final IContentProvider acquireExistingProvider(Context c, String auth, int userId, boolean stable) {
        synchronized (this.mProviderMap) {
            ProviderKey key = new ProviderKey(auth, userId);
            ProviderClientRecord pr = this.mProviderMap.get(key);
            if (pr == null) {
                return null;
            }
            IContentProvider provider = pr.mProvider;
            IBinder jBinder = provider.asBinder();
            if (!jBinder.isBinderAlive()) {
                Log.i(TAG, "Acquiring provider " + auth + " for user " + userId + ": existing object's process dead");
                handleUnstableProviderDiedLocked(jBinder, true);
                return null;
            }
            ProviderRefCount prc = this.mProviderRefCountMap.get(jBinder);
            if (prc != null) {
                incProviderRefLocked(prc, stable);
            }
            return provider;
        }
    }

    public final boolean releaseProvider(IContentProvider provider, boolean stable) {
        if (provider == null) {
            return false;
        }
        IBinder jBinder = provider.asBinder();
        synchronized (this.mProviderMap) {
            ProviderRefCount prc = this.mProviderRefCountMap.get(jBinder);
            if (prc == null) {
                return false;
            }
            boolean lastRef = false;
            if (stable) {
                if (prc.stableCount == 0) {
                    if (DEBUG_PROVIDER) {
                        Slog.v(TAG, "releaseProvider: stable ref count already 0, how?");
                    }
                    return false;
                }
                prc.stableCount--;
                if (prc.stableCount == 0) {
                    lastRef = prc.unstableCount == 0;
                    try {
                        if (DEBUG_PROVIDER) {
                            Slog.v(TAG, "releaseProvider: No longer stable w/lastRef=" + lastRef + " - " + prc.holder.info.name);
                        }
                        ActivityManagerNative.getDefault().refContentProvider(prc.holder.connection, -1, lastRef ? 1 : 0);
                    } catch (RemoteException e) {
                    }
                }
            } else {
                if (prc.unstableCount == 0) {
                    if (DEBUG_PROVIDER) {
                        Slog.v(TAG, "releaseProvider: unstable ref count already 0, how?");
                    }
                    return false;
                }
                prc.unstableCount--;
                if (prc.unstableCount == 0) {
                    lastRef = prc.stableCount == 0;
                    if (!lastRef) {
                        try {
                            if (DEBUG_PROVIDER) {
                                Slog.v(TAG, "releaseProvider: No longer unstable - " + prc.holder.info.name);
                            }
                            ActivityManagerNative.getDefault().refContentProvider(prc.holder.connection, 0, -1);
                        } catch (RemoteException e2) {
                        }
                    }
                }
            }
            if (lastRef) {
                if (!prc.removePending) {
                    if (DEBUG_PROVIDER) {
                        Slog.v(TAG, "releaseProvider: Enqueueing pending removal - " + prc.holder.info.name);
                    }
                    prc.removePending = true;
                    Message msg = this.mH.obtainMessage(131, prc);
                    this.mH.sendMessage(msg);
                } else {
                    Slog.w(TAG, "Duplicate remove pending of provider " + prc.holder.info.name);
                }
            }
            return true;
        }
    }

    final void completeRemoveProvider(ProviderRefCount prc) {
        synchronized (this.mProviderMap) {
            if (!prc.removePending) {
                if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "completeRemoveProvider: lost the race, provider still in use");
                }
                return;
            }
            prc.removePending = false;
            IBinder jBinder = prc.holder.provider.asBinder();
            ProviderRefCount existingPrc = this.mProviderRefCountMap.get(jBinder);
            if (existingPrc == prc) {
                this.mProviderRefCountMap.remove(jBinder);
            }
            for (int i = this.mProviderMap.size() - 1; i >= 0; i--) {
                ProviderClientRecord pr = this.mProviderMap.valueAt(i);
                IBinder myBinder = pr.mProvider.asBinder();
                if (myBinder == jBinder) {
                    this.mProviderMap.removeAt(i);
                }
            }
            try {
                if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "removeProvider: Invoking ActivityManagerNative.removeContentProvider(" + prc.holder.info.name + ")");
                }
                ActivityManagerNative.getDefault().removeContentProvider(prc.holder.connection, false);
            } catch (RemoteException e) {
            }
        }
    }

    final void handleUnstableProviderDied(IBinder provider, boolean fromClient) {
        synchronized (this.mProviderMap) {
            handleUnstableProviderDiedLocked(provider, fromClient);
        }
    }

    final void handleUnstableProviderDiedLocked(IBinder provider, boolean fromClient) {
        ProviderRefCount prc = this.mProviderRefCountMap.get(provider);
        if (prc == null) {
            return;
        }
        if (DEBUG_PROVIDER) {
            Slog.v(TAG, "Cleaning up dead provider " + provider + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + prc.holder.info.name);
        }
        this.mProviderRefCountMap.remove(provider);
        for (int i = this.mProviderMap.size() - 1; i >= 0; i--) {
            ProviderClientRecord pr = this.mProviderMap.valueAt(i);
            if (pr != null && pr.mProvider.asBinder() == provider) {
                Slog.i(TAG, "Removing dead content provider:" + pr.mProvider.toString());
                this.mProviderMap.removeAt(i);
            }
        }
        if (!fromClient) {
            return;
        }
        try {
            ActivityManagerNative.getDefault().unstableProviderDied(prc.holder.connection);
        } catch (RemoteException e) {
        }
    }

    final void appNotRespondingViaProvider(IBinder provider) {
        synchronized (this.mProviderMap) {
            ProviderRefCount prc = this.mProviderRefCountMap.get(provider);
            if (prc != null) {
                try {
                    ActivityManagerNative.getDefault().appNotRespondingViaProvider(prc.holder.connection);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private ProviderClientRecord installProviderAuthoritiesLocked(IContentProvider provider, ContentProvider localProvider, IActivityManager.ContentProviderHolder holder) {
        String[] auths = holder.info.authority.split(";");
        int userId = UserHandle.getUserId(holder.info.applicationInfo.uid);
        ProviderClientRecord pcr = new ProviderClientRecord(auths, provider, localProvider, holder);
        for (String auth : auths) {
            ProviderKey key = new ProviderKey(auth, userId);
            ProviderClientRecord existing = this.mProviderMap.get(key);
            if (existing != null) {
                Slog.w(TAG, "Content provider " + pcr.mHolder.info.name + " already published as " + auth);
            } else {
                this.mProviderMap.put(key, pcr);
            }
        }
        return pcr;
    }

    private IActivityManager.ContentProviderHolder installProvider(Context context, IActivityManager.ContentProviderHolder holder, ProviderInfo info, boolean noisy, boolean noReleaseNeeded, boolean stable) throws Throwable {
        IContentProvider provider;
        IActivityManager.ContentProviderHolder retHolder;
        ContentProvider localProvider = null;
        if (holder == null || holder.provider == null) {
            if (DEBUG_PROVIDER || noisy) {
                Slog.d(TAG, "Loading provider " + info.authority + ": " + info.name);
            }
            Context c = null;
            ApplicationInfo ai = info.applicationInfo;
            if (context.getPackageName().equals(ai.packageName)) {
                c = context;
            } else if (this.mInitialApplication == null || !this.mInitialApplication.getPackageName().equals(ai.packageName)) {
                try {
                    c = context.createPackageContext(ai.packageName, 1);
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                c = this.mInitialApplication;
            }
            if (c == null) {
                Slog.w(TAG, "Unable to get context for package " + ai.packageName + " while loading content provider " + info.name);
                return null;
            }
            try {
                ClassLoader cl = c.getClassLoader();
                localProvider = (ContentProvider) cl.loadClass(info.name).newInstance();
                provider = localProvider.getIContentProvider();
                if (provider == null) {
                    Slog.e(TAG, "Failed to instantiate class " + info.name + " from sourceDir " + info.applicationInfo.sourceDir);
                    return null;
                }
                if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "Instantiating local provider " + info.name);
                }
                localProvider.attachInfo(c, info);
            } catch (Exception e2) {
                if (this.mInstrumentation.onException(null, e2)) {
                    return null;
                }
                throw new RuntimeException("Unable to get provider " + info.name + ": " + e2.toString(), e2);
            }
        } else {
            provider = holder.provider;
            if (DEBUG_PROVIDER) {
                Slog.v(TAG, "Installing external provider " + info.authority + ": " + info.name);
            }
        }
        synchronized (this.mProviderMap) {
            try {
                if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "Checking to add " + provider + " / " + info.name);
                }
                IBinder jBinder = provider.asBinder();
                if (localProvider != null) {
                    ComponentName cname = new ComponentName(info.packageName, info.name);
                    ProviderClientRecord pr = this.mLocalProvidersByName.get(cname);
                    if (pr != null) {
                        if (DEBUG_PROVIDER) {
                            Slog.v(TAG, "installProvider: lost the race, using existing local provider");
                        }
                        IContentProvider iContentProvider = pr.mProvider;
                    } else {
                        IActivityManager.ContentProviderHolder holder2 = new IActivityManager.ContentProviderHolder(info);
                        try {
                            holder2.provider = provider;
                            holder2.noReleaseNeeded = true;
                            pr = installProviderAuthoritiesLocked(provider, localProvider, holder2);
                            this.mLocalProviders.put(jBinder, pr);
                            this.mLocalProvidersByName.put(cname, pr);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    retHolder = pr.mHolder;
                } else {
                    ProviderRefCount prc = this.mProviderRefCountMap.get(jBinder);
                    if (prc != null) {
                        if (DEBUG_PROVIDER) {
                            Slog.v(TAG, "installProvider: lost the race, updating ref count");
                        }
                        if (!noReleaseNeeded) {
                            incProviderRefLocked(prc, stable);
                            try {
                                ActivityManagerNative.getDefault().removeContentProvider(holder.connection, stable);
                            } catch (RemoteException e3) {
                            }
                        }
                    } else {
                        ProviderClientRecord client = installProviderAuthoritiesLocked(provider, localProvider, holder);
                        prc = noReleaseNeeded ? new ProviderRefCount(holder, client, 1000, 1000) : stable ? new ProviderRefCount(holder, client, 1, 0) : new ProviderRefCount(holder, client, 0, 1);
                        this.mProviderRefCountMap.put(jBinder, prc);
                    }
                    retHolder = prc.holder;
                }
                return retHolder;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private void attach(boolean system) {
        sCurrentActivityThread = this;
        this.mSystemThread = system;
        if (!system) {
            ViewRootImpl.addFirstDrawHandler(new Runnable() {
                @Override
                public void run() {
                    ActivityThread.this.ensureJitEnabled();
                }
            });
            DdmHandleAppName.setAppName("<pre-initialized>", UserHandle.myUserId());
            RuntimeInit.setApplicationObject(this.mAppThread.asBinder());
            final IActivityManager mgr = ActivityManagerNative.getDefault();
            try {
                mgr.attachApplication(this.mAppThread);
                BinderInternal.addGcWatcher(new Runnable() {
                    @Override
                    public void run() {
                        if (ActivityThread.this.mSomeActivitiesChanged) {
                            Runtime runtime = Runtime.getRuntime();
                            long dalvikMax = runtime.maxMemory();
                            long dalvikUsed = runtime.totalMemory() - runtime.freeMemory();
                            if (dalvikUsed > (3 * dalvikMax) / 4) {
                                if (ActivityThread.DEBUG_MEMORY_TRIM) {
                                    Slog.d(ActivityThread.TAG, "Dalvik max=" + (dalvikMax / 1024) + " total=" + (runtime.totalMemory() / 1024) + " used=" + (dalvikUsed / 1024));
                                }
                                ActivityThread.this.mSomeActivitiesChanged = false;
                                try {
                                    mgr.releaseSomeActivities(ActivityThread.this.mAppThread);
                                } catch (RemoteException e) {
                                    throw e.rethrowFromSystemServer();
                                }
                            }
                        }
                    }
                });
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        } else {
            DdmHandleAppName.setAppName("system_process", UserHandle.myUserId());
            try {
                this.mInstrumentation = new Instrumentation();
                ContextImpl context = ContextImpl.createAppContext(this, getSystemContext().mPackageInfo);
                this.mInitialApplication = context.mPackageInfo.makeApplication(true, null);
                this.mInitialApplication.onCreate();
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate Application():" + e.toString(), e);
            }
        }
        DropBox.setReporter(new DropBoxReporter());
        ViewRootImpl.addConfigCallback(new ComponentCallbacks2() {
            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                synchronized (ActivityThread.this.mResourcesManager) {
                    if (ActivityThread.this.mResourcesManager.applyConfigurationToResourcesLocked(newConfig, null)) {
                        ActivityThread.this.updateLocaleListFromAppContext(ActivityThread.this.mInitialApplication.getApplicationContext(), ActivityThread.this.mResourcesManager.getConfiguration().getLocales());
                        if (ActivityThread.this.mPendingConfiguration == null || ActivityThread.this.mPendingConfiguration.isOtherSeqNewer(newConfig)) {
                            ActivityThread.this.mPendingConfiguration = newConfig;
                            ActivityThread.this.sendMessage(118, newConfig);
                        }
                    }
                }
            }

            @Override
            public void onLowMemory() {
            }

            @Override
            public void onTrimMemory(int level) {
            }
        });
    }

    public static ActivityThread systemMain() {
        if (!ActivityManager.isHighEndGfx()) {
            ThreadedRenderer.disable(true);
        } else {
            ThreadedRenderer.enableForegroundTrimming();
        }
        ActivityThread thread = new ActivityThread();
        thread.attach(true);
        return thread;
    }

    public final void installSystemProviders(List<ProviderInfo> providers) throws Throwable {
        if (providers == null) {
            return;
        }
        installContentProviders(this.mInitialApplication, providers);
    }

    public int getIntCoreSetting(String key, int defaultValue) {
        synchronized (this.mResourcesManager) {
            if (this.mCoreSettings != null) {
                return this.mCoreSettings.getInt(key, defaultValue);
            }
            return defaultValue;
        }
    }

    private static class EventLoggingReporter implements EventLogger.Reporter {
        EventLoggingReporter(EventLoggingReporter eventLoggingReporter) {
            this();
        }

        private EventLoggingReporter() {
        }

        public void report(int code, Object... list) {
            EventLog.writeEvent(code, list);
        }
    }

    private class DropBoxReporter implements DropBox.Reporter {
        private DropBoxManager dropBox;

        public DropBoxReporter() {
        }

        public void addData(String tag, byte[] data, int flags) {
            ensureInitialized();
            this.dropBox.addData(tag, data, flags);
        }

        public void addText(String tag, String data) {
            ensureInitialized();
            this.dropBox.addText(tag, data);
        }

        private synchronized void ensureInitialized() {
            if (this.dropBox == null) {
                this.dropBox = (DropBoxManager) ActivityThread.this.getSystemContext().getSystemService(Context.DROPBOX_SERVICE);
            }
        }
    }

    public static void main(String[] args) {
        Trace.traceBegin(64L, "ActivityThreadMain");
        SamplingProfilerIntegration.start();
        CloseGuard.setEnabled(false);
        Environment.initForCurrentUser();
        EventLogger.setReporter(new EventLoggingReporter(null));
        File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
        TrustedCertificateStore.setDefaultUserDirectory(configDir);
        Process.setArgV0("<pre-initialized>");
        Looper.prepareMainLooper();
        ActivityThread thread = new ActivityThread();
        thread.attach(false);
        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }
        Trace.traceEnd(64L);
        if (mIsEngBuild) {
            try {
                ANRAppManager mANRAppManager = ANRAppManager.getDefault(new ANRAppFrameworks());
                Looper.myLooper().setMessageLogging(mANRAppManager.newMessageLogger(false));
            } catch (Exception e) {
                Log.d(TAG, "set ANR debugging mechanism state fair " + e);
            }
        }
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }

    public static void enableLooperLog() {
        Log.d(TAG, "Enable Looper Log");
        MessageLogger.mEnableLooperLog = true;
    }

    private void configActivityLogTag() {
        String activitylog = SystemProperties.get("persist.sys.actthreadlog", null);
        if (activitylog == null || activitylog.equals(ProxyInfo.LOCAL_EXCL_LIST)) {
            return;
        }
        if (activitylog.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER) + 1 >= activitylog.length() || activitylog.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER) == -1) {
            Slog.d(TAG, "Invalid argument: " + activitylog);
            SystemProperties.set("persist.sys.actthreadlog", ProxyInfo.LOCAL_EXCL_LIST);
        } else {
            String[] args = {activitylog.substring(0, activitylog.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER)), activitylog.substring(activitylog.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER) + 1, activitylog.length())};
            String tag = args[0];
            boolean on = "on".equals(args[1]);
            configActivityLogTag(tag, on);
        }
    }

    public void configActivityLogTag(String tag, boolean on) {
        Slog.d(TAG, "configActivityLogTag: " + tag + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + on);
        if (tag.equals("x")) {
            DEBUG_MESSAGES = on;
            DEBUG_LIFECYCLE = on;
            return;
        }
        if (tag.equals("all")) {
            localLOGV = on;
            DEBUG_MESSAGES = on;
            DEBUG_BROADCAST = on;
            DEBUG_RESULTS = on;
            DEBUG_BACKUP = on;
            DEBUG_CONFIGURATION = on;
            DEBUG_SERVICE = on;
            DEBUG_MEMORY_TRIM = on;
            DEBUG_PROVIDER = on;
            DEBUG_ORDER = on;
            DEBUG_LIFECYCLE = on;
            return;
        }
        Slog.d(TAG, "Invalid argument: " + tag + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + on);
        SystemProperties.set("persist.sys.actthreadlog", ProxyInfo.LOCAL_EXCL_LIST);
    }
}
