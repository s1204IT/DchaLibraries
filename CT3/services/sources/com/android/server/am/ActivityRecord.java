package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.ResultInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.AppTransitionAnimationSpec;
import android.view.IApplicationToken;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.am.ActivityStack;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.pm.PackageManagerService;
import com.mediatek.am.AMEventHookData;
import com.mediatek.server.am.AMEventHook;
import com.mediatek.server.am.BootEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class ActivityRecord {
    static final String ACTIVITY_ICON_SUFFIX = "_activity_icon_";
    static final int APPLICATION_ACTIVITY_TYPE = 0;
    private static final String ATTR_COMPONENTSPECIFIED = "component_specified";
    private static final String ATTR_ID = "id";
    private static final String ATTR_LAUNCHEDFROMPACKAGE = "launched_from_package";
    private static final String ATTR_LAUNCHEDFROMUID = "launched_from_uid";
    private static final String ATTR_RESOLVEDTYPE = "resolved_type";
    private static final String ATTR_USERID = "user_id";
    static final int HOME_ACTIVITY_TYPE = 1;
    static final int RECENTS_ACTIVITY_TYPE = 2;
    public static final String RECENTS_PACKAGE_NAME = "com.android.systemui.recents";
    private static final boolean SHOW_ACTIVITY_START_TIME = true;
    static final int STARTING_WINDOW_NOT_SHOWN = 0;
    static final int STARTING_WINDOW_REMOVED = 2;
    static final int STARTING_WINDOW_SHOWN = 1;
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSISTABLEBUNDLE = "persistable_bundle";
    ProcessRecord app;
    final ApplicationInfo appInfo;
    AppTimeTracker appTimeTracker;
    final IApplicationToken.Stub appToken;
    CompatibilityInfo compat;
    final boolean componentSpecified;
    int configChangeFlags;
    Configuration configuration;
    HashSet<ConnectionRecord> connections;
    long cpuTimeAtResume;
    long displayStartTime;
    boolean forceNewConfig;
    boolean frozenBeforeDestroy;
    boolean fullscreen;
    long fullyDrawnStartTime;
    boolean haveState;
    Bundle icicle;
    int icon;
    boolean immersive;
    final ActivityInfo info;
    final Intent intent;
    int labelRes;
    long lastLaunchTime;
    long lastVisibleTime;
    int launchCount;
    int launchMode;
    long launchTickTime;
    final String launchedFromPackage;
    final int launchedFromUid;
    int logo;
    int mActivityType;
    private int[] mHorizontalSizeConfigurations;
    ActivityStackSupervisor.ActivityContainer mInitialActivityContainer;
    boolean mLaunchTaskBehind;
    private int[] mSmallestSizeConfigurations;
    final ActivityStackSupervisor mStackSupervisor;
    boolean mUpdateTaskThumbnailWhenHidden;
    private int[] mVerticalSizeConfigurations;
    ArrayList<ReferrerIntent> newIntents;
    final boolean noDisplay;
    CharSequence nonLocalizedLabel;
    final String packageName;
    long pauseTime;
    ActivityOptions pendingOptions;
    HashSet<WeakReference<PendingIntentRecord>> pendingResults;
    boolean pendingVoiceInteractionStart;
    PersistableBundle persistentState;
    boolean preserveWindowOnDeferredRelaunch;
    final String processName;
    final ComponentName realActivity;
    int realTheme;
    final int requestCode;
    ComponentName requestedVrComponent;
    final String resolvedType;
    ActivityRecord resultTo;
    final String resultWho;
    ArrayList<ResultInfo> results;
    ActivityOptions returningOptions;
    final boolean rootVoiceInteraction;
    final ActivityManagerService service;
    final String shortComponentName;
    boolean sleeping;
    long startTime;
    final boolean stateNotNeeded;
    String stringName;
    TaskRecord task;
    final String taskAffinity;
    ActivityManager.TaskDescription taskDescription;
    int theme;
    UriPermissionOwner uriPermissions;
    final int userId;
    IVoiceInteractionSession voiceSession;
    int windowFlags;
    private static final String TAG = "ActivityManager";
    private static final String TAG_STATES = TAG + ActivityManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_THUMBNAILS = TAG + ActivityManagerDebugConfig.POSTFIX_THUMBNAILS;
    long createTime = System.currentTimeMillis();
    ArrayList<ActivityStackSupervisor.ActivityContainer> mChildContainers = new ArrayList<>();
    int mStartingWindowState = 0;
    boolean mTaskOverlay = false;
    Configuration taskConfigOverride = Configuration.EMPTY;
    ActivityStack.ActivityState state = ActivityStack.ActivityState.INITIALIZING;
    boolean frontOfTask = false;
    boolean launchFailed = false;
    boolean stopped = false;
    boolean delayedResume = false;
    boolean finishing = false;
    boolean deferRelaunchUntilPaused = false;
    boolean keysPaused = false;
    private boolean inHistory = false;
    boolean visible = false;
    boolean nowVisible = false;
    boolean idle = false;
    boolean hasBeenLaunched = false;

    private static String startingWindowStateToString(int state) {
        switch (state) {
            case 0:
                return "STARTING_WINDOW_NOT_SHOWN";
            case 1:
                return "STARTING_WINDOW_SHOWN";
            case 2:
                return "STARTING_WINDOW_REMOVED";
            default:
                return "unknown state=" + state;
        }
    }

    void dump(PrintWriter pw, String prefix) {
        long now = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print("packageName=");
        pw.print(this.packageName);
        pw.print(" processName=");
        pw.println(this.processName);
        pw.print(prefix);
        pw.print("launchedFromUid=");
        pw.print(this.launchedFromUid);
        pw.print(" launchedFromPackage=");
        pw.print(this.launchedFromPackage);
        pw.print(" userId=");
        pw.println(this.userId);
        pw.print(prefix);
        pw.print("app=");
        pw.println(this.app);
        pw.print(prefix);
        pw.println(this.intent.toInsecureStringWithClip());
        pw.print(prefix);
        pw.print("frontOfTask=");
        pw.print(this.frontOfTask);
        pw.print(" task=");
        pw.println(this.task);
        pw.print(prefix);
        pw.print("taskAffinity=");
        pw.println(this.taskAffinity);
        pw.print(prefix);
        pw.print("realActivity=");
        pw.println(this.realActivity.flattenToShortString());
        if (this.appInfo != null) {
            pw.print(prefix);
            pw.print("baseDir=");
            pw.println(this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                pw.print(prefix);
                pw.print("resDir=");
                pw.println(this.appInfo.publicSourceDir);
            }
            pw.print(prefix);
            pw.print("dataDir=");
            pw.println(this.appInfo.dataDir);
            if (this.appInfo.splitSourceDirs != null) {
                pw.print(prefix);
                pw.print("splitDir=");
                pw.println(Arrays.toString(this.appInfo.splitSourceDirs));
            }
        }
        pw.print(prefix);
        pw.print("stateNotNeeded=");
        pw.print(this.stateNotNeeded);
        pw.print(" componentSpecified=");
        pw.print(this.componentSpecified);
        pw.print(" mActivityType=");
        pw.println(this.mActivityType);
        if (this.rootVoiceInteraction) {
            pw.print(prefix);
            pw.print("rootVoiceInteraction=");
            pw.println(this.rootVoiceInteraction);
        }
        pw.print(prefix);
        pw.print("compat=");
        pw.print(this.compat);
        pw.print(" labelRes=0x");
        pw.print(Integer.toHexString(this.labelRes));
        pw.print(" icon=0x");
        pw.print(Integer.toHexString(this.icon));
        pw.print(" theme=0x");
        pw.println(Integer.toHexString(this.theme));
        pw.print(prefix);
        pw.print("config=");
        pw.println(this.configuration);
        pw.print(prefix);
        pw.print("taskConfigOverride=");
        pw.println(this.taskConfigOverride);
        if (this.resultTo != null || this.resultWho != null) {
            pw.print(prefix);
            pw.print("resultTo=");
            pw.print(this.resultTo);
            pw.print(" resultWho=");
            pw.print(this.resultWho);
            pw.print(" resultCode=");
            pw.println(this.requestCode);
        }
        if (this.taskDescription != null) {
            String iconFilename = this.taskDescription.getIconFilename();
            if (iconFilename != null || this.taskDescription.getLabel() != null || this.taskDescription.getPrimaryColor() != 0) {
                pw.print(prefix);
                pw.print("taskDescription:");
                pw.print(" iconFilename=");
                pw.print(this.taskDescription.getIconFilename());
                pw.print(" label=\"");
                pw.print(this.taskDescription.getLabel());
                pw.print("\"");
                pw.print(" color=");
                pw.println(Integer.toHexString(this.taskDescription.getPrimaryColor()));
            }
            if (iconFilename == null && this.taskDescription.getIcon() != null) {
                pw.print(prefix);
                pw.println("taskDescription contains Bitmap");
            }
        }
        if (this.results != null) {
            pw.print(prefix);
            pw.print("results=");
            pw.println(this.results);
        }
        if (this.pendingResults != null && this.pendingResults.size() > 0) {
            pw.print(prefix);
            pw.println("Pending Results:");
            for (WeakReference<PendingIntentRecord> wpir : this.pendingResults) {
                PendingIntentRecord pendingIntentRecord = wpir != null ? wpir.get() : null;
                pw.print(prefix);
                pw.print("  - ");
                if (pendingIntentRecord == null) {
                    pw.println("null");
                } else {
                    pw.println(pendingIntentRecord);
                    pendingIntentRecord.dump(pw, prefix + "    ");
                }
            }
        }
        if (this.newIntents != null && this.newIntents.size() > 0) {
            pw.print(prefix);
            pw.println("Pending New Intents:");
            for (int i = 0; i < this.newIntents.size(); i++) {
                Intent intent = this.newIntents.get(i);
                pw.print(prefix);
                pw.print("  - ");
                if (intent == null) {
                    pw.println("null");
                } else {
                    pw.println(intent.toShortString(false, true, false, true));
                }
            }
        }
        if (this.pendingOptions != null) {
            pw.print(prefix);
            pw.print("pendingOptions=");
            pw.println(this.pendingOptions);
        }
        if (this.appTimeTracker != null) {
            this.appTimeTracker.dumpWithHeader(pw, prefix, false);
        }
        if (this.uriPermissions != null) {
            this.uriPermissions.dump(pw, prefix);
        }
        pw.print(prefix);
        pw.print("launchFailed=");
        pw.print(this.launchFailed);
        pw.print(" launchCount=");
        pw.print(this.launchCount);
        pw.print(" lastLaunchTime=");
        if (this.lastLaunchTime == 0) {
            pw.print("0");
        } else {
            TimeUtils.formatDuration(this.lastLaunchTime, now, pw);
        }
        pw.println();
        pw.print(prefix);
        pw.print("haveState=");
        pw.print(this.haveState);
        pw.print(" icicle=");
        pw.println(this.icicle);
        pw.print(prefix);
        pw.print("state=");
        pw.print(this.state);
        pw.print(" stopped=");
        pw.print(this.stopped);
        pw.print(" delayedResume=");
        pw.print(this.delayedResume);
        pw.print(" finishing=");
        pw.println(this.finishing);
        pw.print(prefix);
        pw.print("keysPaused=");
        pw.print(this.keysPaused);
        pw.print(" inHistory=");
        pw.print(this.inHistory);
        pw.print(" visible=");
        pw.print(this.visible);
        pw.print(" sleeping=");
        pw.print(this.sleeping);
        pw.print(" idle=");
        pw.print(this.idle);
        pw.print(" mStartingWindowState=");
        pw.println(startingWindowStateToString(this.mStartingWindowState));
        pw.print(prefix);
        pw.print("fullscreen=");
        pw.print(this.fullscreen);
        pw.print(" noDisplay=");
        pw.print(this.noDisplay);
        pw.print(" immersive=");
        pw.print(this.immersive);
        pw.print(" launchMode=");
        pw.println(this.launchMode);
        pw.print(prefix);
        pw.print("frozenBeforeDestroy=");
        pw.print(this.frozenBeforeDestroy);
        pw.print(" forceNewConfig=");
        pw.println(this.forceNewConfig);
        pw.print(prefix);
        pw.print("mActivityType=");
        pw.println(activityTypeToString(this.mActivityType));
        if (this.requestedVrComponent != null) {
            pw.print(prefix);
            pw.print("requestedVrComponent=");
            pw.println(this.requestedVrComponent);
        }
        if (this.displayStartTime != 0 || this.startTime != 0) {
            pw.print(prefix);
            pw.print("displayStartTime=");
            if (this.displayStartTime == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(this.displayStartTime, now, pw);
            }
            pw.print(" startTime=");
            if (this.startTime == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(this.startTime, now, pw);
            }
            pw.println();
        }
        boolean waitingVisible = this.mStackSupervisor.mWaitingVisibleActivities.contains(this);
        if (this.lastVisibleTime != 0 || waitingVisible || this.nowVisible) {
            pw.print(prefix);
            pw.print("waitingVisible=");
            pw.print(waitingVisible);
            pw.print(" nowVisible=");
            pw.print(this.nowVisible);
            pw.print(" lastVisibleTime=");
            if (this.lastVisibleTime == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(this.lastVisibleTime, now, pw);
            }
            pw.println();
        }
        if (this.deferRelaunchUntilPaused || this.configChangeFlags != 0) {
            pw.print(prefix);
            pw.print("deferRelaunchUntilPaused=");
            pw.print(this.deferRelaunchUntilPaused);
            pw.print(" configChangeFlags=");
            pw.println(Integer.toHexString(this.configChangeFlags));
        }
        if (this.connections != null) {
            pw.print(prefix);
            pw.print("connections=");
            pw.println(this.connections);
        }
        if (this.info == null) {
            return;
        }
        pw.println(prefix + "resizeMode=" + ActivityInfo.resizeModeToString(this.info.resizeMode));
    }

    public boolean crossesHorizontalSizeThreshold(int firstDp, int secondDp) {
        return crossesSizeThreshold(this.mHorizontalSizeConfigurations, firstDp, secondDp);
    }

    public boolean crossesVerticalSizeThreshold(int firstDp, int secondDp) {
        return crossesSizeThreshold(this.mVerticalSizeConfigurations, firstDp, secondDp);
    }

    public boolean crossesSmallestSizeThreshold(int firstDp, int secondDp) {
        return crossesSizeThreshold(this.mSmallestSizeConfigurations, firstDp, secondDp);
    }

    private static boolean crossesSizeThreshold(int[] thresholds, int firstDp, int secondDp) {
        if (thresholds == null) {
            return false;
        }
        for (int i = thresholds.length - 1; i >= 0; i--) {
            int threshold = thresholds[i];
            if (firstDp >= threshold || secondDp < threshold) {
                if (firstDp >= threshold && secondDp < threshold) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public void setSizeConfigurations(int[] horizontalSizeConfiguration, int[] verticalSizeConfigurations, int[] smallestSizeConfigurations) {
        this.mHorizontalSizeConfigurations = horizontalSizeConfiguration;
        this.mVerticalSizeConfigurations = verticalSizeConfigurations;
        this.mSmallestSizeConfigurations = smallestSizeConfigurations;
    }

    void scheduleConfigurationChanged(Configuration config, boolean reportToActivity) {
        if (this.app == null || this.app.thread == null) {
            return;
        }
        try {
            Configuration overrideConfig = new Configuration(config);
            overrideConfig.fontScale = this.service.mConfiguration.fontScale;
            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Sending new config to " + this + " reportToActivity=" + reportToActivity + " and config: " + overrideConfig);
            }
            this.app.thread.scheduleActivityConfigurationChanged(this.appToken, overrideConfig, reportToActivity);
        } catch (RemoteException e) {
        }
    }

    void scheduleMultiWindowModeChanged() {
        if (this.task == null || this.task.stack == null || this.app == null || this.app.thread == null) {
            return;
        }
        try {
            this.app.thread.scheduleMultiWindowModeChanged(this.appToken, !this.task.mFullscreen);
        } catch (Exception e) {
        }
    }

    void schedulePictureInPictureModeChanged() {
        if (this.task == null || this.task.stack == null || this.app == null || this.app.thread == null) {
            return;
        }
        try {
            this.app.thread.schedulePictureInPictureModeChanged(this.appToken, this.task.stack.mStackId == 4);
        } catch (Exception e) {
        }
    }

    boolean isFreeform() {
        return (this.task == null || this.task.stack == null || this.task.stack.mStackId != 2) ? false : true;
    }

    static class Token extends IApplicationToken.Stub {
        private final ActivityManagerService mService;
        private final WeakReference<ActivityRecord> weakActivity;

        Token(ActivityRecord activity, ActivityManagerService service) {
            this.weakActivity = new WeakReference<>(activity);
            this.mService = service;
        }

        public void windowsDrawn() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = tokenToActivityRecordLocked(this);
                    if (r != null) {
                        r.windowsDrawnLocked();
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void windowsVisible() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = tokenToActivityRecordLocked(this);
                    if (r != null) {
                        r.windowsVisibleLocked();
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void windowsGone() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = tokenToActivityRecordLocked(this);
                    if (r == null) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                        Log.v(ActivityRecord.TAG_SWITCH, "windowsGone(): " + r);
                    }
                    r.nowVisible = false;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        public boolean keyDispatchingTimedOut(String reason) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = tokenToActivityRecordLocked(this);
                    if (r == null) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    ActivityRecord anrActivity = r.getWaitingHistoryRecordLocked();
                    ProcessRecord processRecord = r != null ? r.app : null;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return this.mService.inputDispatchingTimedOut(processRecord, anrActivity, r, false, reason);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        public long getKeyDispatchingTimeout() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = tokenToActivityRecordLocked(this);
                    if (r == null) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return 0L;
                    }
                    long inputDispatchingTimeoutLocked = ActivityManagerService.getInputDispatchingTimeoutLocked(r.getWaitingHistoryRecordLocked());
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return inputDispatchingTimeoutLocked;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        private static final ActivityRecord tokenToActivityRecordLocked(Token token) {
            ActivityRecord r;
            if (token == null || (r = token.weakActivity.get()) == null || r.task == null || r.task.stack == null) {
                return null;
            }
            return r;
        }

        public int getFocusAppPid() throws RemoteException {
            ActivityRecord activity = this.weakActivity.get();
            if (activity != null) {
                return activity.getFocusAppPid();
            }
            return -1;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Token{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            sb.append(this.weakActivity.get());
            sb.append('}');
            return sb.toString();
        }
    }

    static ActivityRecord forTokenLocked(IBinder iBinder) {
        try {
            return Token.tokenToActivityRecordLocked((Token) iBinder);
        } catch (ClassCastException e) {
            Slog.w(TAG, "Bad activity token: " + iBinder, e);
            return null;
        }
    }

    boolean isResolverActivity() {
        return ResolverActivity.class.getName().equals(this.realActivity.getClassName());
    }

    ActivityRecord(ActivityManagerService _service, ProcessRecord _caller, int _launchedFromUid, String _launchedFromPackage, Intent _intent, String _resolvedType, ActivityInfo aInfo, Configuration _configuration, ActivityRecord _resultTo, String _resultWho, int _reqCode, boolean _componentSpecified, boolean _rootVoiceInteraction, ActivityStackSupervisor supervisor, ActivityStackSupervisor.ActivityContainer container, ActivityOptions options, ActivityRecord sourceRecord) {
        this.service = _service;
        this.appToken = new Token(this, this.service);
        this.info = aInfo;
        this.launchedFromUid = _launchedFromUid;
        this.launchedFromPackage = _launchedFromPackage;
        this.userId = UserHandle.getUserId(aInfo.applicationInfo.uid);
        this.intent = _intent;
        this.shortComponentName = _intent.getComponent().flattenToShortString();
        this.resolvedType = _resolvedType;
        this.componentSpecified = _componentSpecified;
        this.rootVoiceInteraction = _rootVoiceInteraction;
        this.configuration = _configuration;
        this.resultTo = _resultTo;
        this.resultWho = _resultWho;
        this.requestCode = _reqCode;
        this.mStackSupervisor = supervisor;
        this.mInitialActivityContainer = container;
        if (options != null) {
            this.pendingOptions = options;
            this.mLaunchTaskBehind = this.pendingOptions.getLaunchTaskBehind();
            PendingIntent usageReport = this.pendingOptions.getUsageTimeReport();
            if (usageReport != null) {
                this.appTimeTracker = new AppTimeTracker(usageReport);
            }
        }
        this.haveState = true;
        if (aInfo == null) {
            this.realActivity = null;
            this.taskAffinity = null;
            this.stateNotNeeded = false;
            this.appInfo = null;
            this.processName = null;
            this.packageName = null;
            this.fullscreen = true;
            this.noDisplay = false;
            this.mActivityType = 0;
            this.immersive = false;
            this.requestedVrComponent = null;
            return;
        }
        if (aInfo.targetActivity == null || (aInfo.targetActivity.equals(_intent.getComponent().getClassName()) && (aInfo.launchMode == 0 || aInfo.launchMode == 1))) {
            this.realActivity = _intent.getComponent();
        } else {
            this.realActivity = new ComponentName(aInfo.packageName, aInfo.targetActivity);
        }
        this.taskAffinity = aInfo.taskAffinity;
        this.stateNotNeeded = (aInfo.flags & 16) != 0;
        this.appInfo = aInfo.applicationInfo;
        this.nonLocalizedLabel = aInfo.nonLocalizedLabel;
        this.labelRes = aInfo.labelRes;
        if (this.nonLocalizedLabel == null && this.labelRes == 0) {
            ApplicationInfo app = aInfo.applicationInfo;
            this.nonLocalizedLabel = app.nonLocalizedLabel;
            this.labelRes = app.labelRes;
        }
        this.icon = aInfo.getIconResource();
        this.logo = aInfo.getLogoResource();
        this.theme = aInfo.getThemeResource();
        this.realTheme = this.theme;
        if (this.realTheme == 0) {
            this.realTheme = aInfo.applicationInfo.targetSdkVersion < 11 ? R.style.Theme : R.style.Theme.Holo;
        }
        if ((aInfo.flags & 512) != 0) {
            this.windowFlags |= 16777216;
        }
        if ((aInfo.flags & 1) == 0 || _caller == null || !(aInfo.applicationInfo.uid == 1000 || aInfo.applicationInfo.uid == _caller.info.uid)) {
            this.processName = aInfo.processName;
        } else {
            this.processName = _caller.processName;
        }
        if (this.intent != null && (aInfo.flags & 32) != 0) {
            this.intent.addFlags(8388608);
        }
        this.packageName = aInfo.applicationInfo.packageName;
        this.launchMode = aInfo.launchMode;
        AttributeCache.Entry ent = AttributeCache.instance().get(this.packageName, this.realTheme, com.android.internal.R.styleable.Window, this.userId);
        boolean translucent = ent != null ? !ent.array.getBoolean(5, false) ? !ent.array.hasValue(5) ? ent.array.getBoolean(25, false) : false : true : false;
        boolean z = (ent == null || ent.array.getBoolean(4, false) || translucent) ? false : true;
        this.fullscreen = z;
        this.noDisplay = ent != null ? ent.array.getBoolean(10, false) : false;
        setActivityType(_componentSpecified, _launchedFromUid, _intent, sourceRecord);
        this.immersive = (aInfo.flags & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0;
        this.requestedVrComponent = aInfo.requestedVrComponent == null ? null : ComponentName.unflattenFromString(aInfo.requestedVrComponent);
    }

    private boolean isHomeIntent(Intent intent) {
        if ("android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.HOME") && intent.getCategories().size() == 1 && intent.getData() == null) {
            return intent.getType() == null;
        }
        return false;
    }

    private boolean canLaunchHomeActivity(int uid, ActivityRecord sourceRecord) {
        if (uid == Process.myUid() || uid == 0) {
            return true;
        }
        if (sourceRecord != null) {
            return sourceRecord.isResolverActivity();
        }
        return false;
    }

    private void setActivityType(boolean componentSpecified, int launchedFromUid, Intent intent, ActivityRecord sourceRecord) {
        if ((!componentSpecified || canLaunchHomeActivity(launchedFromUid, sourceRecord)) && isHomeIntent(intent) && !isResolverActivity()) {
            this.mActivityType = 1;
        } else if (this.realActivity.getClassName().contains(RECENTS_PACKAGE_NAME)) {
            this.mActivityType = 2;
        } else {
            this.mActivityType = 0;
        }
    }

    void setTask(TaskRecord newTask, TaskRecord taskToAffiliateWith) {
        if (this.task != null && this.task.removeActivity(this) && this.task != newTask && this.task.stack != null) {
            this.task.stack.removeTask(this.task, "setTask");
        }
        this.task = newTask;
        setTaskToAffiliateWith(taskToAffiliateWith);
    }

    void setTaskToAffiliateWith(TaskRecord taskToAffiliateWith) {
        if (taskToAffiliateWith == null || this.launchMode == 3 || this.launchMode == 2) {
            return;
        }
        this.task.setTaskToAffiliateWith(taskToAffiliateWith);
    }

    boolean changeWindowTranslucency(boolean toOpaque) {
        if (this.fullscreen == toOpaque) {
            return false;
        }
        TaskRecord taskRecord = this.task;
        taskRecord.numFullscreen = (toOpaque ? 1 : -1) + taskRecord.numFullscreen;
        this.fullscreen = toOpaque;
        return true;
    }

    void putInHistory() {
        if (this.inHistory) {
            return;
        }
        this.inHistory = true;
    }

    void takeFromHistory() {
        if (!this.inHistory) {
            return;
        }
        this.inHistory = false;
        if (this.task != null && !this.finishing) {
            this.task = null;
        }
        clearOptionsLocked();
    }

    boolean isInHistory() {
        return this.inHistory;
    }

    boolean isInStackLocked() {
        return (this.task == null || this.task.stack == null || this.task.stack.isInStackLocked(this) == null) ? false : true;
    }

    boolean isHomeActivity() {
        return this.mActivityType == 1;
    }

    boolean isRecentsActivity() {
        return this.mActivityType == 2;
    }

    boolean isApplicationActivity() {
        return this.mActivityType == 0;
    }

    boolean isPersistable() {
        boolean z = true;
        if (this.info.persistableMode != 0 && this.info.persistableMode != 2) {
            return false;
        }
        if (this.intent != null && (this.intent.getFlags() & 8388608) != 0) {
            z = false;
        }
        return z;
    }

    boolean isFocusable() {
        if (ActivityManager.StackId.canReceiveKeys(this.task.stack.mStackId)) {
            return true;
        }
        return isAlwaysFocusable();
    }

    boolean isResizeable() {
        if (isHomeActivity()) {
            return false;
        }
        return ActivityInfo.isResizeableMode(this.info.resizeMode);
    }

    boolean isResizeableOrForced() {
        if (isHomeActivity()) {
            return false;
        }
        if (isResizeable()) {
            return true;
        }
        return this.service.mForceResizableActivities;
    }

    boolean isNonResizableOrForced() {
        return (isHomeActivity() || this.info.resizeMode == 2 || this.info.resizeMode == 3) ? false : true;
    }

    boolean supportsPictureInPicture() {
        return !isHomeActivity() && this.info.resizeMode == 3;
    }

    boolean canGoInDockedStack() {
        if (isHomeActivity()) {
            return false;
        }
        return isResizeableOrForced() || this.info.resizeMode == 1;
    }

    boolean isAlwaysFocusable() {
        return (this.info.flags & PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED) != 0;
    }

    void makeFinishingLocked() {
        if (this.finishing) {
            return;
        }
        if (this.task != null && this.task.stack != null && this == this.task.stack.getVisibleBehindActivity()) {
            this.mStackSupervisor.requestVisibleBehindLocked(this, false);
        }
        this.finishing = true;
        if (!this.stopped) {
            return;
        }
        clearOptionsLocked();
    }

    UriPermissionOwner getUriPermissionsLocked() {
        if (this.uriPermissions == null) {
            this.uriPermissions = new UriPermissionOwner(this.service, this);
        }
        return this.uriPermissions;
    }

    void addResultLocked(ActivityRecord from, String resultWho, int requestCode, int resultCode, Intent resultData) {
        ActivityResult r = new ActivityResult(from, resultWho, requestCode, resultCode, resultData);
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(r);
    }

    void removeResultsLocked(ActivityRecord from, String resultWho, int requestCode) {
        if (this.results == null) {
            return;
        }
        for (int i = this.results.size() - 1; i >= 0; i--) {
            ActivityResult r = (ActivityResult) this.results.get(i);
            if (r.mFrom == from) {
                if (r.mResultWho == null) {
                    if (resultWho == null) {
                        if (r.mRequestCode == requestCode) {
                            this.results.remove(i);
                        }
                    }
                } else if (!r.mResultWho.equals(resultWho)) {
                }
            }
        }
    }

    void addNewIntentLocked(ReferrerIntent intent) {
        if (this.newIntents == null) {
            this.newIntents = new ArrayList<>();
        }
        this.newIntents.add(intent);
    }

    final void deliverNewIntentLocked(int callingUid, Intent intent, String referrer) {
        this.service.grantUriPermissionFromIntentLocked(callingUid, this.packageName, intent, getUriPermissionsLocked(), this.userId);
        ReferrerIntent rintent = new ReferrerIntent(intent, referrer);
        boolean unsent = true;
        if ((this.state == ActivityStack.ActivityState.RESUMED || (this.service.isSleepingLocked() && this.task.stack != null && this.task.stack.topRunningActivityLocked() == this)) && this.app != null && this.app.thread != null) {
            try {
                ArrayList<ReferrerIntent> ar = new ArrayList<>(1);
                ar.add(rintent);
                this.app.thread.scheduleNewIntent(ar, this.appToken);
                unsent = false;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e);
            } catch (NullPointerException e2) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e2);
            }
        }
        if (!unsent) {
            return;
        }
        addNewIntentLocked(rintent);
    }

    void updateOptionsLocked(ActivityOptions options) {
        if (options == null) {
            return;
        }
        if (this.pendingOptions != null) {
            this.pendingOptions.abort();
        }
        this.pendingOptions = options;
    }

    void applyOptionsLocked() {
        if (this.pendingOptions == null || this.pendingOptions.getAnimationType() == 5) {
            return;
        }
        int animationType = this.pendingOptions.getAnimationType();
        switch (animationType) {
            case 1:
                this.service.mWindowManager.overridePendingAppTransition(this.pendingOptions.getPackageName(), this.pendingOptions.getCustomEnterResId(), this.pendingOptions.getCustomExitResId(), this.pendingOptions.getOnAnimationStartListener());
                break;
            case 2:
                this.service.mWindowManager.overridePendingAppTransitionScaleUp(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight());
                if (this.intent.getSourceBounds() == null) {
                    this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                }
                break;
            case 3:
            case 4:
                boolean scaleUp = animationType == 3;
                this.service.mWindowManager.overridePendingAppTransitionThumb(this.pendingOptions.getThumbnail(), this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getOnAnimationStartListener(), scaleUp);
                if (this.intent.getSourceBounds() == null) {
                    this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getThumbnail().getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getThumbnail().getHeight()));
                }
                break;
            case 5:
            case 6:
            case 7:
            case 10:
            default:
                Slog.e(TAG, "applyOptionsLocked: Unknown animationType=" + animationType);
                break;
            case 8:
            case 9:
                AppTransitionAnimationSpec[] specs = this.pendingOptions.getAnimSpecs();
                if (animationType == 9 && specs != null) {
                    this.service.mWindowManager.overridePendingAppTransitionMultiThumb(specs, this.pendingOptions.getOnAnimationStartListener(), this.pendingOptions.getAnimationFinishedListener(), false);
                } else {
                    this.service.mWindowManager.overridePendingAppTransitionAspectScaledThumb(this.pendingOptions.getThumbnail(), this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight(), this.pendingOptions.getOnAnimationStartListener(), animationType == 8);
                    if (this.intent.getSourceBounds() == null) {
                        this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                    }
                }
                break;
            case 11:
                this.service.mWindowManager.overridePendingAppTransitionClipReveal(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight());
                if (this.intent.getSourceBounds() == null) {
                    this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                }
                break;
        }
        this.pendingOptions = null;
    }

    ActivityOptions getOptionsForTargetActivityLocked() {
        if (this.pendingOptions != null) {
            return this.pendingOptions.forTargetActivity();
        }
        return null;
    }

    void clearOptionsLocked() {
        if (this.pendingOptions == null) {
            return;
        }
        this.pendingOptions.abort();
        this.pendingOptions = null;
    }

    ActivityOptions takeOptionsLocked() {
        ActivityOptions opts = this.pendingOptions;
        this.pendingOptions = null;
        return opts;
    }

    void removeUriPermissionsLocked() {
        if (this.uriPermissions == null) {
            return;
        }
        this.uriPermissions.removeUriPermissionsLocked();
        this.uriPermissions = null;
    }

    void pauseKeyDispatchingLocked() {
        if (this.keysPaused) {
            return;
        }
        this.keysPaused = true;
        this.service.mWindowManager.pauseKeyDispatching(this.appToken);
    }

    void resumeKeyDispatchingLocked() {
        if (!this.keysPaused) {
            return;
        }
        this.keysPaused = false;
        this.service.mWindowManager.resumeKeyDispatching(this.appToken);
    }

    void updateThumbnailLocked(Bitmap newThumbnail, CharSequence description) {
        if (newThumbnail != null) {
            if (ActivityManagerDebugConfig.DEBUG_THUMBNAILS) {
                Slog.i(TAG_THUMBNAILS, "Setting thumbnail of " + this + " to " + newThumbnail);
            }
            boolean thumbnailUpdated = this.task.setLastThumbnailLocked(newThumbnail);
            if (thumbnailUpdated && isPersistable()) {
                this.mStackSupervisor.mService.notifyTaskPersisterLocked(this.task, false);
            }
        }
        this.task.lastDescription = description;
    }

    void startLaunchTickingLocked() {
        if (ActivityManagerService.IS_USER_BUILD || this.launchTickTime != 0) {
            return;
        }
        this.launchTickTime = SystemClock.uptimeMillis();
        continueLaunchTickingLocked();
    }

    boolean continueLaunchTickingLocked() {
        ActivityStack stack;
        if (this.launchTickTime == 0 || (stack = this.task.stack) == null) {
            return false;
        }
        Message msg = stack.mHandler.obtainMessage(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION, this);
        stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION);
        stack.mHandler.sendMessageDelayed(msg, 500L);
        return true;
    }

    void finishLaunchTickingLocked() {
        this.launchTickTime = 0L;
        ActivityStack stack = this.task.stack;
        if (stack == null) {
            return;
        }
        stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION);
    }

    public boolean mayFreezeScreenLocked(ProcessRecord app) {
        return (app == null || app.crashing || app.notResponding) ? false : true;
    }

    public void startFreezingScreenLocked(ProcessRecord app, int configChanges) {
        if (!mayFreezeScreenLocked(app)) {
            return;
        }
        this.service.mWindowManager.startAppFreezingScreen(this.appToken, configChanges);
    }

    public void stopFreezingScreenLocked(boolean force) {
        if (!force && !this.frozenBeforeDestroy) {
            return;
        }
        this.frozenBeforeDestroy = false;
        this.service.mWindowManager.stopAppFreezingScreen(this.appToken, force);
    }

    public void reportFullyDrawnLocked() {
        long curTime = SystemClock.uptimeMillis();
        if (this.displayStartTime != 0) {
            reportLaunchTimeLocked(curTime);
        }
        ActivityStack stack = this.task.stack;
        if (this.fullyDrawnStartTime != 0 && stack != null) {
            long thisTime = curTime - this.fullyDrawnStartTime;
            long totalTime = stack.mFullyDrawnStartTime != 0 ? curTime - stack.mFullyDrawnStartTime : thisTime;
            Trace.asyncTraceEnd(64L, "drawing", 0);
            EventLog.writeEvent(EventLogTags.AM_ACTIVITY_FULLY_DRAWN_TIME, Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), this.shortComponentName, Long.valueOf(thisTime), Long.valueOf(totalTime));
            StringBuilder sb = this.service.mStringBuilder;
            sb.setLength(0);
            sb.append("Fully drawn ");
            sb.append(this.shortComponentName);
            sb.append(": ");
            TimeUtils.formatDuration(thisTime, sb);
            if (thisTime != totalTime) {
                sb.append(" (total ");
                TimeUtils.formatDuration(totalTime, sb);
                sb.append(")");
            }
            Log.i(TAG, sb.toString());
            if (totalTime > 0) {
            }
            stack.mFullyDrawnStartTime = 0L;
        }
        this.fullyDrawnStartTime = 0L;
    }

    private void reportLaunchTimeLocked(long curTime) {
        ActivityStack stack = this.task.stack;
        if (stack == null) {
            return;
        }
        long thisTime = curTime - this.displayStartTime;
        long totalTime = stack.mLaunchStartTime != 0 ? curTime - stack.mLaunchStartTime : thisTime;
        Trace.asyncTraceEnd(64L, "launching: " + this.packageName, 0);
        EventLog.writeEvent(EventLogTags.AM_ACTIVITY_LAUNCH_TIME, Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), this.shortComponentName, Long.valueOf(thisTime), Long.valueOf(totalTime));
        StringBuilder sb = this.service.mStringBuilder;
        sb.setLength(0);
        sb.append("Displayed ");
        sb.append(this.shortComponentName);
        sb.append(": ");
        TimeUtils.formatDuration(thisTime, sb);
        if (thisTime != totalTime) {
            sb.append(" (total ");
            TimeUtils.formatDuration(totalTime, sb);
            sb.append(")");
        }
        Log.i(TAG, sb.toString());
        BootEvent.addBootEvent("AP_Launch: " + this.shortComponentName + " " + thisTime + "ms");
        this.mStackSupervisor.reportActivityLaunchedLocked(false, this, thisTime, totalTime);
        if (totalTime > 0) {
        }
        this.displayStartTime = 0L;
        stack.mLaunchStartTime = 0L;
    }

    void windowsDrawnLocked() {
        this.mStackSupervisor.mActivityMetricsLogger.notifyWindowsDrawn();
        if (this.displayStartTime != 0) {
            reportLaunchTimeLocked(SystemClock.uptimeMillis());
        }
        this.mStackSupervisor.sendWaitingVisibleReportLocked(this);
        this.startTime = 0L;
        finishLaunchTickingLocked();
        if (this.task == null) {
            return;
        }
        this.task.hasBeenVisible = true;
    }

    void windowsVisibleLocked() {
        this.mStackSupervisor.reportActivityVisibleLocked(this);
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Log.v(TAG_SWITCH, "windowsVisibleLocked(): " + this);
        }
        if (!this.nowVisible) {
            this.nowVisible = true;
            this.lastVisibleTime = SystemClock.uptimeMillis();
            if (!this.idle) {
                this.mStackSupervisor.processStoppingActivitiesLocked(false);
            } else {
                int size = this.mStackSupervisor.mWaitingVisibleActivities.size();
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        ActivityRecord r = this.mStackSupervisor.mWaitingVisibleActivities.get(i);
                        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                            Log.v(TAG_SWITCH, "Was waiting for visible: " + r);
                        }
                    }
                    this.mStackSupervisor.mWaitingVisibleActivities.clear();
                    this.mStackSupervisor.scheduleIdleLocked();
                }
            }
            this.service.scheduleAppGcsLocked();
        }
        AMEventHookData.WindowsVisible eventData = AMEventHookData.WindowsVisible.createInstance();
        this.service.getAMEventHook().hook(AMEventHook.Event.AM_WindowsVisible, eventData);
    }

    ActivityRecord getWaitingHistoryRecordLocked() {
        if (this.mStackSupervisor.mWaitingVisibleActivities.contains(this) || this.stopped) {
            ActivityStack stack = this.mStackSupervisor.getFocusedStack();
            ActivityRecord r = stack.mResumedActivity;
            if (r == null) {
                r = stack.mPausingActivity;
            }
            if (r != null) {
                return r;
            }
        }
        return this;
    }

    public int getFocusAppPid() {
        if (this != null && this.app != null) {
            return this.app.pid;
        }
        return -1;
    }

    public boolean isInterestingToUserLocked() {
        return this.visible || this.nowVisible || this.state == ActivityStack.ActivityState.PAUSING || this.state == ActivityStack.ActivityState.RESUMED;
    }

    public void setSleeping(boolean _sleeping) {
        if (this.sleeping == _sleeping || this.app == null || this.app.thread == null) {
            return;
        }
        try {
            this.app.thread.scheduleSleeping(this.appToken, _sleeping);
            if (_sleeping && !this.mStackSupervisor.mGoingToSleepActivities.contains(this)) {
                this.mStackSupervisor.mGoingToSleepActivities.add(this);
            }
            this.sleeping = _sleeping;
        } catch (RemoteException e) {
            Slog.w(TAG, "Exception thrown when sleeping: " + this.intent.getComponent(), e);
        }
    }

    static int getTaskForActivityLocked(IBinder token, boolean onlyRoot) {
        ActivityRecord r = forTokenLocked(token);
        if (r == null) {
            return -1;
        }
        TaskRecord task = r.task;
        int activityNdx = task.mActivities.indexOf(r);
        if (activityNdx < 0 || (onlyRoot && activityNdx > task.findEffectiveRootIndex())) {
            return -1;
        }
        return task.taskId;
    }

    static ActivityRecord isInStackLocked(IBinder token) {
        ActivityRecord r = forTokenLocked(token);
        if (r != null) {
            return r.task.stack.isInStackLocked(r);
        }
        return null;
    }

    static ActivityStack getStackLocked(IBinder token) {
        ActivityRecord r = isInStackLocked(token);
        if (r != null) {
            return r.task.stack;
        }
        return null;
    }

    final boolean isDestroyable() {
        return (this.finishing || this.app == null || this.state == ActivityStack.ActivityState.DESTROYING || this.state == ActivityStack.ActivityState.DESTROYED || this.task == null || this.task.stack == null || this == this.task.stack.mResumedActivity || this == this.task.stack.mPausingActivity || !this.haveState || !this.stopped || this.visible) ? false : true;
    }

    private static String createImageFilename(long createTime, int taskId) {
        return String.valueOf(taskId) + ACTIVITY_ICON_SUFFIX + createTime + ".png";
    }

    void setTaskDescription(ActivityManager.TaskDescription _taskDescription) {
        Bitmap icon;
        if (_taskDescription.getIconFilename() == null && (icon = _taskDescription.getIcon()) != null) {
            String iconFilename = createImageFilename(this.createTime, this.task.taskId);
            File iconFile = new File(TaskPersister.getUserImagesDir(this.userId), iconFilename);
            String iconFilePath = iconFile.getAbsolutePath();
            this.service.mRecentTasks.saveImage(icon, iconFilePath);
            _taskDescription.setIconFilename(iconFilePath);
        }
        this.taskDescription = _taskDescription;
    }

    void setVoiceSessionLocked(IVoiceInteractionSession session) {
        this.voiceSession = session;
        this.pendingVoiceInteractionStart = false;
    }

    void clearVoiceSessionLocked() {
        this.voiceSession = null;
        this.pendingVoiceInteractionStart = false;
    }

    void showStartingWindow(ActivityRecord prev, boolean createIfNeeded) {
        CompatibilityInfo compatInfo = this.service.compatibilityInfoForPackageLocked(this.info.applicationInfo);
        boolean shown = this.service.mWindowManager.setAppStartingWindow(this.appToken, this.packageName, this.theme, compatInfo, this.nonLocalizedLabel, this.labelRes, this.icon, this.logo, this.windowFlags, prev != null ? prev.appToken : null, createIfNeeded);
        if (!shown) {
            return;
        }
        this.mStartingWindowState = 1;
    }

    void saveToXml(XmlSerializer out) throws XmlPullParserException, IOException {
        out.attribute(null, ATTR_ID, String.valueOf(this.createTime));
        out.attribute(null, ATTR_LAUNCHEDFROMUID, String.valueOf(this.launchedFromUid));
        if (this.launchedFromPackage != null) {
            out.attribute(null, ATTR_LAUNCHEDFROMPACKAGE, this.launchedFromPackage);
        }
        if (this.resolvedType != null) {
            out.attribute(null, ATTR_RESOLVEDTYPE, this.resolvedType);
        }
        out.attribute(null, ATTR_COMPONENTSPECIFIED, String.valueOf(this.componentSpecified));
        out.attribute(null, ATTR_USERID, String.valueOf(this.userId));
        if (this.taskDescription != null) {
            this.taskDescription.saveToXml(out);
        }
        out.startTag(null, TAG_INTENT);
        this.intent.saveToXml(out);
        out.endTag(null, TAG_INTENT);
        if (!isPersistable() || this.persistentState == null) {
            return;
        }
        out.startTag(null, TAG_PERSISTABLEBUNDLE);
        this.persistentState.saveToXml(out);
        out.endTag(null, TAG_PERSISTABLEBUNDLE);
    }

    static ActivityRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor) throws XmlPullParserException, IOException {
        Intent intentRestoreFromXml = null;
        PersistableBundle persistableBundleRestoreFromXml = null;
        int launchedFromUid = 0;
        String launchedFromPackage = null;
        String resolvedType = null;
        boolean componentSpecified = false;
        int userId = 0;
        long createTime = -1;
        int outerDepth = in.getDepth();
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription();
        for (int attrNdx = in.getAttributeCount() - 1; attrNdx >= 0; attrNdx--) {
            String attrName = in.getAttributeName(attrNdx);
            String attrValue = in.getAttributeValue(attrNdx);
            if (ATTR_ID.equals(attrName)) {
                createTime = Long.valueOf(attrValue).longValue();
            } else if (ATTR_LAUNCHEDFROMUID.equals(attrName)) {
                launchedFromUid = Integer.parseInt(attrValue);
            } else if (ATTR_LAUNCHEDFROMPACKAGE.equals(attrName)) {
                launchedFromPackage = attrValue;
            } else if (ATTR_RESOLVEDTYPE.equals(attrName)) {
                resolvedType = attrValue;
            } else if (ATTR_COMPONENTSPECIFIED.equals(attrName)) {
                componentSpecified = Boolean.valueOf(attrValue).booleanValue();
            } else if (ATTR_USERID.equals(attrName)) {
                userId = Integer.parseInt(attrValue);
            } else if (attrName.startsWith("task_description_")) {
                taskDescription.restoreFromXml(attrName, attrValue);
            } else {
                Log.d(TAG, "Unknown ActivityRecord attribute=" + attrName);
            }
        }
        while (true) {
            int event = in.next();
            if (event == 1 || (event == 3 && in.getDepth() < outerDepth)) {
                break;
            }
            if (event == 2) {
                String name = in.getName();
                if (TAG_INTENT.equals(name)) {
                    intentRestoreFromXml = Intent.restoreFromXml(in);
                } else if (TAG_PERSISTABLEBUNDLE.equals(name)) {
                    persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(in);
                } else {
                    Slog.w(TAG, "restoreActivity: unexpected name=" + name);
                    XmlUtils.skipCurrentTag(in);
                }
            }
        }
        if (intentRestoreFromXml == null) {
            throw new XmlPullParserException("restoreActivity error intent=" + intentRestoreFromXml);
        }
        ActivityManagerService service = stackSupervisor.mService;
        ActivityInfo aInfo = stackSupervisor.resolveActivity(intentRestoreFromXml, resolvedType, 0, null, userId);
        if (aInfo == null) {
            throw new XmlPullParserException("restoreActivity resolver error. Intent=" + intentRestoreFromXml + " resolvedType=" + resolvedType);
        }
        ActivityRecord r = new ActivityRecord(service, null, launchedFromUid, launchedFromPackage, intentRestoreFromXml, resolvedType, aInfo, service.getConfiguration(), null, null, 0, componentSpecified, false, stackSupervisor, null, null, null);
        r.persistentState = persistableBundleRestoreFromXml;
        r.taskDescription = taskDescription;
        r.createTime = createTime;
        return r;
    }

    private static String activityTypeToString(int type) {
        switch (type) {
            case 0:
                return "APPLICATION_ACTIVITY_TYPE";
            case 1:
                return "HOME_ACTIVITY_TYPE";
            case 2:
                return "RECENTS_ACTIVITY_TYPE";
            default:
                return Integer.toString(type);
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName + " t" + (this.task == null ? -1 : this.task.taskId) + (this.finishing ? " f}" : "}");
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ActivityRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(this.userId);
        sb.append(' ');
        sb.append(this.intent.getComponent().flattenToShortString());
        this.stringName = sb.toString();
        return toString();
    }
}
