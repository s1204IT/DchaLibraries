package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.ResultInfo;
import android.app.WindowConfiguration;
import android.app.servertransaction.ActivityConfigurationChangeItem;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ActivityRelaunchItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.app.servertransaction.MoveToDisplayItem;
import android.app.servertransaction.MultiWindowModeChangeItem;
import android.app.servertransaction.NewIntentItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.PipModeChangeItem;
import android.app.servertransaction.ResumeActivityItem;
import android.app.servertransaction.WindowVisibilityItem;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.service.voice.IVoiceInteractionSession;
import android.util.EventLog;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IApplicationToken;
import android.view.RemoteAnimationDefinition;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.am.ActivityStack;
import com.android.server.am.LaunchTimeTracker;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.AppWindowContainerController;
import com.android.server.wm.AppWindowContainerListener;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.TaskWindowContainerController;
import com.android.server.wm.WindowManagerService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class ActivityRecord extends ConfigurationContainer implements AppWindowContainerListener {
    static final String ACTIVITY_ICON_SUFFIX = "_activity_icon_";
    private static final String ATTR_COMPONENTSPECIFIED = "component_specified";
    private static final String ATTR_ID = "id";
    private static final String ATTR_LAUNCHEDFROMPACKAGE = "launched_from_package";
    private static final String ATTR_LAUNCHEDFROMUID = "launched_from_uid";
    private static final String ATTR_RESOLVEDTYPE = "resolved_type";
    private static final String ATTR_USERID = "user_id";
    private static final String LEGACY_RECENTS_PACKAGE_NAME = "com.android.systemui.recents";
    private static final boolean SHOW_ACTIVITY_START_TIME = true;
    static final int STARTING_WINDOW_NOT_SHOWN = 0;
    static final int STARTING_WINDOW_REMOVED = 2;
    static final int STARTING_WINDOW_SHOWN = 1;
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSISTABLEBUNDLE = "persistable_bundle";
    public ProcessRecord app;
    public ApplicationInfo appInfo;
    AppTimeTracker appTimeTracker;
    final IApplicationToken.Stub appToken;
    CompatibilityInfo compat;
    private final boolean componentSpecified;
    int configChangeFlags;
    HashSet<ConnectionRecord> connections;
    long cpuTimeAtResume;
    boolean deferRelaunchUntilPaused;
    boolean delayedResume;
    long displayStartTime;
    boolean finishing;
    boolean forceNewConfig;
    boolean frontOfTask;
    boolean frozenBeforeDestroy;
    boolean fullscreen;
    long fullyDrawnStartTime;
    boolean hasBeenLaunched;
    final boolean hasWallpaper;
    boolean haveState;
    Bundle icicle;
    private int icon;
    boolean idle;
    boolean immersive;
    private boolean inHistory;
    public final ActivityInfo info;
    final Intent intent;
    private boolean keysPaused;
    private int labelRes;
    long lastLaunchTime;
    long lastVisibleTime;
    int launchCount;
    boolean launchFailed;
    int launchMode;
    long launchTickTime;
    final String launchedFromPackage;
    final int launchedFromPid;
    final int launchedFromUid;
    int lockTaskLaunchMode;
    private int logo;
    boolean mClientVisibilityDeferred;
    private boolean mDeferHidingClient;
    private int[] mHorizontalSizeConfigurations;
    private MergedConfiguration mLastReportedConfiguration;
    private int mLastReportedDisplayId;
    private boolean mLastReportedMultiWindowMode;
    private boolean mLastReportedPictureInPictureMode;
    boolean mLaunchTaskBehind;
    int mRotationAnimationHint;
    private boolean mShowWhenLocked;
    private int[] mSmallestSizeConfigurations;
    final ActivityStackSupervisor mStackSupervisor;
    private ActivityStack.ActivityState mState;
    private boolean mTurnScreenOn;
    private int[] mVerticalSizeConfigurations;
    AppWindowContainerController mWindowContainerController;
    ArrayList<ReferrerIntent> newIntents;
    final boolean noDisplay;
    private CharSequence nonLocalizedLabel;
    boolean nowVisible;
    public final String packageName;
    long pauseTime;
    ActivityOptions pendingOptions;
    HashSet<WeakReference<PendingIntentRecord>> pendingResults;
    boolean pendingVoiceInteractionStart;
    PersistableBundle persistentState;
    boolean preserveWindowOnDeferredRelaunch;
    final String processName;
    final ComponentName realActivity;
    private int realTheme;
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
    private long startTime;
    final boolean stateNotNeeded;
    boolean stopped;
    String stringName;
    boolean supportsEnterPipOnTaskSwitch;
    private TaskRecord task;
    final String taskAffinity;
    ActivityManager.TaskDescription taskDescription;
    private int theme;
    UriPermissionOwner uriPermissions;
    final int userId;
    boolean visible;
    boolean visibleIgnoringKeyguard;
    IVoiceInteractionSession voiceSession;
    private int windowFlags;
    private static final String TAG = "ActivityManager";
    private static final String TAG_CONFIGURATION = TAG + ActivityManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_SAVED_STATE = TAG + ActivityManagerDebugConfig.POSTFIX_SAVED_STATE;
    private static final String TAG_STATES = TAG + ActivityManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_VISIBILITY = TAG + ActivityManagerDebugConfig.POSTFIX_VISIBILITY;
    private long createTime = System.currentTimeMillis();
    PictureInPictureParams pictureInPictureArgs = new PictureInPictureParams.Builder().build();
    int mStartingWindowState = 0;
    boolean mTaskOverlay = false;
    private final Configuration mTmpConfig = new Configuration();
    private final Rect mTmpBounds = new Rect();

    private static String startingWindowStateToString(int i) {
        switch (i) {
            case 0:
                return "STARTING_WINDOW_NOT_SHOWN";
            case 1:
                return "STARTING_WINDOW_SHOWN";
            case 2:
                return "STARTING_WINDOW_REMOVED";
            default:
                return "unknown state=" + i;
        }
    }

    void dump(PrintWriter printWriter, String str) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        printWriter.print(str);
        printWriter.print("packageName=");
        printWriter.print(this.packageName);
        printWriter.print(" processName=");
        printWriter.println(this.processName);
        printWriter.print(str);
        printWriter.print("launchedFromUid=");
        printWriter.print(this.launchedFromUid);
        printWriter.print(" launchedFromPackage=");
        printWriter.print(this.launchedFromPackage);
        printWriter.print(" userId=");
        printWriter.println(this.userId);
        printWriter.print(str);
        printWriter.print("app=");
        printWriter.println(this.app);
        printWriter.print(str);
        printWriter.println(this.intent.toInsecureStringWithClip());
        printWriter.print(str);
        printWriter.print("frontOfTask=");
        printWriter.print(this.frontOfTask);
        printWriter.print(" task=");
        printWriter.println(this.task);
        printWriter.print(str);
        printWriter.print("taskAffinity=");
        printWriter.println(this.taskAffinity);
        printWriter.print(str);
        printWriter.print("realActivity=");
        printWriter.println(this.realActivity.flattenToShortString());
        if (this.appInfo != null) {
            printWriter.print(str);
            printWriter.print("baseDir=");
            printWriter.println(this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                printWriter.print(str);
                printWriter.print("resDir=");
                printWriter.println(this.appInfo.publicSourceDir);
            }
            printWriter.print(str);
            printWriter.print("dataDir=");
            printWriter.println(this.appInfo.dataDir);
            if (this.appInfo.splitSourceDirs != null) {
                printWriter.print(str);
                printWriter.print("splitDir=");
                printWriter.println(Arrays.toString(this.appInfo.splitSourceDirs));
            }
        }
        printWriter.print(str);
        printWriter.print("stateNotNeeded=");
        printWriter.print(this.stateNotNeeded);
        printWriter.print(" componentSpecified=");
        printWriter.print(this.componentSpecified);
        printWriter.print(" mActivityType=");
        printWriter.println(WindowConfiguration.activityTypeToString(getActivityType()));
        if (this.rootVoiceInteraction) {
            printWriter.print(str);
            printWriter.print("rootVoiceInteraction=");
            printWriter.println(this.rootVoiceInteraction);
        }
        printWriter.print(str);
        printWriter.print("compat=");
        printWriter.print(this.compat);
        printWriter.print(" labelRes=0x");
        printWriter.print(Integer.toHexString(this.labelRes));
        printWriter.print(" icon=0x");
        printWriter.print(Integer.toHexString(this.icon));
        printWriter.print(" theme=0x");
        printWriter.println(Integer.toHexString(this.theme));
        printWriter.println(str + "mLastReportedConfigurations:");
        this.mLastReportedConfiguration.dump(printWriter, str + " ");
        printWriter.print(str);
        printWriter.print("CurrentConfiguration=");
        printWriter.println(getConfiguration());
        if (!getOverrideConfiguration().equals(Configuration.EMPTY)) {
            printWriter.println(str + "OverrideConfiguration=" + getOverrideConfiguration());
        }
        if (!matchParentBounds()) {
            printWriter.println(str + "bounds=" + getBounds());
        }
        if (this.resultTo != null || this.resultWho != null) {
            printWriter.print(str);
            printWriter.print("resultTo=");
            printWriter.print(this.resultTo);
            printWriter.print(" resultWho=");
            printWriter.print(this.resultWho);
            printWriter.print(" resultCode=");
            printWriter.println(this.requestCode);
        }
        if (this.taskDescription != null && (this.taskDescription.getIconFilename() != null || this.taskDescription.getLabel() != null || this.taskDescription.getPrimaryColor() != 0)) {
            printWriter.print(str);
            printWriter.print("taskDescription:");
            printWriter.print(" label=\"");
            printWriter.print(this.taskDescription.getLabel());
            printWriter.print("\"");
            printWriter.print(" icon=");
            printWriter.print(this.taskDescription.getInMemoryIcon() != null ? this.taskDescription.getInMemoryIcon().getByteCount() + " bytes" : "null");
            printWriter.print(" iconResource=");
            printWriter.print(this.taskDescription.getIconResource());
            printWriter.print(" iconFilename=");
            printWriter.print(this.taskDescription.getIconFilename());
            printWriter.print(" primaryColor=");
            printWriter.println(Integer.toHexString(this.taskDescription.getPrimaryColor()));
            printWriter.print(str + " backgroundColor=");
            printWriter.println(Integer.toHexString(this.taskDescription.getBackgroundColor()));
            printWriter.print(str + " statusBarColor=");
            printWriter.println(Integer.toHexString(this.taskDescription.getStatusBarColor()));
            printWriter.print(str + " navigationBarColor=");
            printWriter.println(Integer.toHexString(this.taskDescription.getNavigationBarColor()));
        }
        if (this.results != null) {
            printWriter.print(str);
            printWriter.print("results=");
            printWriter.println(this.results);
        }
        if (this.pendingResults != null && this.pendingResults.size() > 0) {
            printWriter.print(str);
            printWriter.println("Pending Results:");
            Iterator<WeakReference<PendingIntentRecord>> it = this.pendingResults.iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> next = it.next();
                PendingIntentRecord pendingIntentRecord = next != null ? next.get() : null;
                printWriter.print(str);
                printWriter.print("  - ");
                if (pendingIntentRecord == null) {
                    printWriter.println("null");
                } else {
                    printWriter.println(pendingIntentRecord);
                    pendingIntentRecord.dump(printWriter, str + "    ");
                }
            }
        }
        if (this.newIntents != null && this.newIntents.size() > 0) {
            printWriter.print(str);
            printWriter.println("Pending New Intents:");
            for (int i = 0; i < this.newIntents.size(); i++) {
                Intent intent = this.newIntents.get(i);
                printWriter.print(str);
                printWriter.print("  - ");
                if (intent == null) {
                    printWriter.println("null");
                } else {
                    printWriter.println(intent.toShortString(false, true, false, true));
                }
            }
        }
        if (this.pendingOptions != null) {
            printWriter.print(str);
            printWriter.print("pendingOptions=");
            printWriter.println(this.pendingOptions);
        }
        if (this.appTimeTracker != null) {
            this.appTimeTracker.dumpWithHeader(printWriter, str, false);
        }
        if (this.uriPermissions != null) {
            this.uriPermissions.dump(printWriter, str);
        }
        printWriter.print(str);
        printWriter.print("launchFailed=");
        printWriter.print(this.launchFailed);
        printWriter.print(" launchCount=");
        printWriter.print(this.launchCount);
        printWriter.print(" lastLaunchTime=");
        if (this.lastLaunchTime == 0) {
            printWriter.print("0");
        } else {
            TimeUtils.formatDuration(this.lastLaunchTime, jUptimeMillis, printWriter);
        }
        printWriter.println();
        printWriter.print(str);
        printWriter.print("haveState=");
        printWriter.print(this.haveState);
        printWriter.print(" icicle=");
        printWriter.println(this.icicle);
        printWriter.print(str);
        printWriter.print("state=");
        printWriter.print(this.mState);
        printWriter.print(" stopped=");
        printWriter.print(this.stopped);
        printWriter.print(" delayedResume=");
        printWriter.print(this.delayedResume);
        printWriter.print(" finishing=");
        printWriter.println(this.finishing);
        printWriter.print(str);
        printWriter.print("keysPaused=");
        printWriter.print(this.keysPaused);
        printWriter.print(" inHistory=");
        printWriter.print(this.inHistory);
        printWriter.print(" visible=");
        printWriter.print(this.visible);
        printWriter.print(" sleeping=");
        printWriter.print(this.sleeping);
        printWriter.print(" idle=");
        printWriter.print(this.idle);
        printWriter.print(" mStartingWindowState=");
        printWriter.println(startingWindowStateToString(this.mStartingWindowState));
        printWriter.print(str);
        printWriter.print("fullscreen=");
        printWriter.print(this.fullscreen);
        printWriter.print(" noDisplay=");
        printWriter.print(this.noDisplay);
        printWriter.print(" immersive=");
        printWriter.print(this.immersive);
        printWriter.print(" launchMode=");
        printWriter.println(this.launchMode);
        printWriter.print(str);
        printWriter.print("frozenBeforeDestroy=");
        printWriter.print(this.frozenBeforeDestroy);
        printWriter.print(" forceNewConfig=");
        printWriter.println(this.forceNewConfig);
        printWriter.print(str);
        printWriter.print("mActivityType=");
        printWriter.println(WindowConfiguration.activityTypeToString(getActivityType()));
        if (this.requestedVrComponent != null) {
            printWriter.print(str);
            printWriter.print("requestedVrComponent=");
            printWriter.println(this.requestedVrComponent);
        }
        if (this.displayStartTime != 0 || this.startTime != 0) {
            printWriter.print(str);
            printWriter.print("displayStartTime=");
            if (this.displayStartTime == 0) {
                printWriter.print("0");
            } else {
                TimeUtils.formatDuration(this.displayStartTime, jUptimeMillis, printWriter);
            }
            printWriter.print(" startTime=");
            if (this.startTime == 0) {
                printWriter.print("0");
            } else {
                TimeUtils.formatDuration(this.startTime, jUptimeMillis, printWriter);
            }
            printWriter.println();
        }
        boolean zContains = this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(this);
        if (this.lastVisibleTime != 0 || zContains || this.nowVisible) {
            printWriter.print(str);
            printWriter.print("waitingVisible=");
            printWriter.print(zContains);
            printWriter.print(" nowVisible=");
            printWriter.print(this.nowVisible);
            printWriter.print(" lastVisibleTime=");
            if (this.lastVisibleTime == 0) {
                printWriter.print("0");
            } else {
                TimeUtils.formatDuration(this.lastVisibleTime, jUptimeMillis, printWriter);
            }
            printWriter.println();
        }
        if (this.mDeferHidingClient) {
            printWriter.println(str + "mDeferHidingClient=" + this.mDeferHidingClient);
        }
        if (this.deferRelaunchUntilPaused || this.configChangeFlags != 0) {
            printWriter.print(str);
            printWriter.print("deferRelaunchUntilPaused=");
            printWriter.print(this.deferRelaunchUntilPaused);
            printWriter.print(" configChangeFlags=");
            printWriter.println(Integer.toHexString(this.configChangeFlags));
        }
        if (this.connections != null) {
            printWriter.print(str);
            printWriter.print("connections=");
            printWriter.println(this.connections);
        }
        if (this.info != null) {
            printWriter.println(str + "resizeMode=" + ActivityInfo.resizeModeToString(this.info.resizeMode));
            printWriter.println(str + "mLastReportedMultiWindowMode=" + this.mLastReportedMultiWindowMode + " mLastReportedPictureInPictureMode=" + this.mLastReportedPictureInPictureMode);
            if (this.info.supportsPictureInPicture()) {
                printWriter.println(str + "supportsPictureInPicture=" + this.info.supportsPictureInPicture());
                printWriter.println(str + "supportsEnterPipOnTaskSwitch: " + this.supportsEnterPipOnTaskSwitch);
            }
            if (this.info.maxAspectRatio != 0.0f) {
                printWriter.println(str + "maxAspectRatio=" + this.info.maxAspectRatio);
            }
        }
    }

    void updateApplicationInfo(ApplicationInfo applicationInfo) {
        this.appInfo = applicationInfo;
        this.info.applicationInfo = applicationInfo;
    }

    private boolean crossesHorizontalSizeThreshold(int i, int i2) {
        return crossesSizeThreshold(this.mHorizontalSizeConfigurations, i, i2);
    }

    private boolean crossesVerticalSizeThreshold(int i, int i2) {
        return crossesSizeThreshold(this.mVerticalSizeConfigurations, i, i2);
    }

    private boolean crossesSmallestSizeThreshold(int i, int i2) {
        return crossesSizeThreshold(this.mSmallestSizeConfigurations, i, i2);
    }

    private static boolean crossesSizeThreshold(int[] iArr, int i, int i2) {
        if (iArr == null) {
            return false;
        }
        for (int length = iArr.length - 1; length >= 0; length--) {
            int i3 = iArr[length];
            if ((i < i3 && i2 >= i3) || (i >= i3 && i2 < i3)) {
                return true;
            }
        }
        return false;
    }

    void setSizeConfigurations(int[] iArr, int[] iArr2, int[] iArr3) {
        this.mHorizontalSizeConfigurations = iArr;
        this.mVerticalSizeConfigurations = iArr2;
        this.mSmallestSizeConfigurations = iArr3;
    }

    private void scheduleActivityMovedToDisplay(int i, Configuration configuration) {
        if (this.app == null || this.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.w(TAG, "Can't report activity moved to display - client not running, activityRecord=" + this + ", displayId=" + i);
                return;
            }
            return;
        }
        try {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Reporting activity moved to display, activityRecord=" + this + ", displayId=" + i + ", config=" + configuration);
            }
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ClientTransactionItem) MoveToDisplayItem.obtain(i, configuration));
        } catch (RemoteException e) {
        }
    }

    private void scheduleConfigurationChanged(Configuration configuration) {
        if (this.app == null || this.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.w(TAG, "Can't report activity configuration update - client not running, activityRecord=" + this);
                return;
            }
            return;
        }
        try {
            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Sending new config to " + this + ", config: " + configuration);
            }
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ClientTransactionItem) ActivityConfigurationChangeItem.obtain(configuration));
        } catch (RemoteException e) {
        }
    }

    void updateMultiWindowMode() {
        boolean zInMultiWindowMode;
        if (this.task != null && this.task.getStack() != null && this.app != null && this.app.thread != null && !this.task.getStack().deferScheduleMultiWindowModeChanged() && (zInMultiWindowMode = inMultiWindowMode()) != this.mLastReportedMultiWindowMode) {
            this.mLastReportedMultiWindowMode = zInMultiWindowMode;
            scheduleMultiWindowModeChanged(getConfiguration());
        }
    }

    private void scheduleMultiWindowModeChanged(Configuration configuration) {
        try {
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ClientTransactionItem) MultiWindowModeChangeItem.obtain(this.mLastReportedMultiWindowMode, configuration));
        } catch (Exception e) {
        }
    }

    void updatePictureInPictureMode(Rect rect, boolean z) {
        if (this.task == null || this.task.getStack() == null || this.app == null || this.app.thread == null) {
            return;
        }
        boolean z2 = inPinnedWindowingMode() && rect != null;
        if (z2 != this.mLastReportedPictureInPictureMode || z) {
            this.mLastReportedPictureInPictureMode = z2;
            this.mLastReportedMultiWindowMode = z2;
            Configuration configurationComputeNewOverrideConfigurationForBounds = this.task.computeNewOverrideConfigurationForBounds(rect, null);
            schedulePictureInPictureModeChanged(configurationComputeNewOverrideConfigurationForBounds);
            scheduleMultiWindowModeChanged(configurationComputeNewOverrideConfigurationForBounds);
        }
    }

    private void schedulePictureInPictureModeChanged(Configuration configuration) {
        try {
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ClientTransactionItem) PipModeChangeItem.obtain(this.mLastReportedPictureInPictureMode, configuration));
        } catch (Exception e) {
        }
    }

    @Override
    protected int getChildCount() {
        return 0;
    }

    @Override
    protected ConfigurationContainer getChildAt(int i) {
        return null;
    }

    @Override
    protected ConfigurationContainer getParent() {
        return getTask();
    }

    TaskRecord getTask() {
        return this.task;
    }

    void setTask(TaskRecord taskRecord) {
        setTask(taskRecord, false);
    }

    void setTask(TaskRecord taskRecord, boolean z) {
        if (taskRecord != null && taskRecord == getTask()) {
            return;
        }
        ActivityStack stack = getStack();
        ActivityStack stack2 = taskRecord != null ? taskRecord.getStack() : null;
        if (stack != stack2) {
            if (!z && stack != null) {
                stack.onActivityRemovedFromStack(this);
            }
            if (stack2 != null) {
                stack2.onActivityAddedToStack(this);
            }
        }
        this.task = taskRecord;
        if (!z) {
            onParentChanged();
        }
    }

    void setWillCloseOrEnterPip(boolean z) {
        getWindowContainerController().setWillCloseOrEnterPip(z);
    }

    static class Token extends IApplicationToken.Stub {
        private final String name;
        private final WeakReference<ActivityRecord> weakActivity;

        Token(ActivityRecord activityRecord, Intent intent) {
            this.weakActivity = new WeakReference<>(activityRecord);
            this.name = intent.getComponent().flattenToShortString();
        }

        private static ActivityRecord tokenToActivityRecordLocked(Token token) {
            ActivityRecord activityRecord;
            if (token == null || (activityRecord = token.weakActivity.get()) == null || activityRecord.getStack() == null) {
                return null;
            }
            return activityRecord;
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

        public String getName() {
            return this.name;
        }
    }

    public static ActivityRecord forTokenLocked(IBinder iBinder) {
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

    boolean isResolverOrChildActivity() {
        if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(this.packageName)) {
            return false;
        }
        try {
            return ResolverActivity.class.isAssignableFrom(Object.class.getClassLoader().loadClass(this.realActivity.getClassName()));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    ActivityRecord(ActivityManagerService activityManagerService, ProcessRecord processRecord, int i, int i2, String str, Intent intent, String str2, ActivityInfo activityInfo, Configuration configuration, ActivityRecord activityRecord, String str3, int i3, boolean z, boolean z2, ActivityStackSupervisor activityStackSupervisor, ActivityOptions activityOptions, ActivityRecord activityRecord2) {
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        this.mRotationAnimationHint = -1;
        this.service = activityManagerService;
        this.appToken = new Token(this, intent);
        this.info = activityInfo;
        this.launchedFromPid = i;
        this.launchedFromUid = i2;
        this.launchedFromPackage = str;
        this.userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        this.intent = intent;
        this.shortComponentName = intent.getComponent().flattenToShortString();
        this.resolvedType = str2;
        this.componentSpecified = z;
        this.rootVoiceInteraction = z2;
        this.mLastReportedConfiguration = new MergedConfiguration(configuration);
        this.resultTo = activityRecord;
        this.resultWho = str3;
        this.requestCode = i3;
        setState(ActivityStack.ActivityState.INITIALIZING, "ActivityRecord ctor");
        this.frontOfTask = false;
        this.launchFailed = false;
        this.stopped = false;
        this.delayedResume = false;
        this.finishing = false;
        this.deferRelaunchUntilPaused = false;
        this.keysPaused = false;
        this.inHistory = false;
        this.visible = false;
        this.nowVisible = false;
        this.idle = false;
        this.hasBeenLaunched = false;
        this.mStackSupervisor = activityStackSupervisor;
        this.haveState = true;
        if (activityInfo.targetActivity == null || (activityInfo.targetActivity.equals(intent.getComponent().getClassName()) && (activityInfo.launchMode == 0 || activityInfo.launchMode == 1))) {
            this.realActivity = intent.getComponent();
        } else {
            this.realActivity = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
        }
        this.taskAffinity = activityInfo.taskAffinity;
        if ((activityInfo.flags & 16) == 0) {
            z3 = false;
        } else {
            z3 = true;
        }
        this.stateNotNeeded = z3;
        this.appInfo = activityInfo.applicationInfo;
        this.nonLocalizedLabel = activityInfo.nonLocalizedLabel;
        this.labelRes = activityInfo.labelRes;
        if (this.nonLocalizedLabel == null && this.labelRes == 0) {
            ApplicationInfo applicationInfo = activityInfo.applicationInfo;
            this.nonLocalizedLabel = applicationInfo.nonLocalizedLabel;
            this.labelRes = applicationInfo.labelRes;
        }
        this.icon = activityInfo.getIconResource();
        this.logo = activityInfo.getLogoResource();
        this.theme = activityInfo.getThemeResource();
        this.realTheme = this.theme;
        if (this.realTheme == 0) {
            this.realTheme = activityInfo.applicationInfo.targetSdkVersion < 11 ? R.style.Theme : R.style.Theme.Holo;
        }
        if ((activityInfo.flags & 512) != 0) {
            this.windowFlags |= DumpState.DUMP_SERVICE_PERMISSIONS;
        }
        if ((activityInfo.flags & 1) != 0 && processRecord != null && (activityInfo.applicationInfo.uid == 1000 || activityInfo.applicationInfo.uid == processRecord.info.uid)) {
            this.processName = processRecord.processName;
        } else {
            this.processName = activityInfo.processName;
        }
        if ((activityInfo.flags & 32) != 0) {
            this.intent.addFlags(DumpState.DUMP_VOLUMES);
        }
        this.packageName = activityInfo.applicationInfo.packageName;
        this.launchMode = activityInfo.launchMode;
        AttributeCache.Entry entry = AttributeCache.instance().get(this.packageName, this.realTheme, com.android.internal.R.styleable.Window, this.userId);
        if (entry != null) {
            this.fullscreen = !ActivityInfo.isTranslucentOrFloating(entry.array);
            this.hasWallpaper = entry.array.getBoolean(14, false);
            this.noDisplay = entry.array.getBoolean(10, false);
        } else {
            this.hasWallpaper = false;
            this.noDisplay = false;
        }
        setActivityType(z, i2, intent, activityOptions, activityRecord2);
        if ((activityInfo.flags & 2048) == 0) {
            z4 = false;
        } else {
            z4 = true;
        }
        this.immersive = z4;
        this.requestedVrComponent = activityInfo.requestedVrComponent == null ? null : ComponentName.unflattenFromString(activityInfo.requestedVrComponent);
        if ((activityInfo.flags & DumpState.DUMP_VOLUMES) == 0) {
            z5 = false;
        } else {
            z5 = true;
        }
        this.mShowWhenLocked = z5;
        if ((activityInfo.flags & DumpState.DUMP_SERVICE_PERMISSIONS) == 0) {
            z6 = false;
        } else {
            z6 = true;
        }
        this.mTurnScreenOn = z6;
        this.mRotationAnimationHint = activityInfo.rotationAnimation;
        this.lockTaskLaunchMode = activityInfo.lockTaskLaunchMode;
        if (this.appInfo.isPrivilegedApp() && (this.lockTaskLaunchMode == 2 || this.lockTaskLaunchMode == 1)) {
            this.lockTaskLaunchMode = 0;
        }
        if (activityOptions != null) {
            this.pendingOptions = activityOptions;
            this.mLaunchTaskBehind = activityOptions.getLaunchTaskBehind();
            int rotationAnimationHint = this.pendingOptions.getRotationAnimationHint();
            if (rotationAnimationHint >= 0) {
                this.mRotationAnimationHint = rotationAnimationHint;
            }
            PendingIntent usageTimeReport = this.pendingOptions.getUsageTimeReport();
            if (usageTimeReport != null) {
                this.appTimeTracker = new AppTimeTracker(usageTimeReport);
            }
            if (this.pendingOptions.getLockTaskMode() && this.lockTaskLaunchMode == 0) {
                this.lockTaskLaunchMode = 3;
            }
        }
    }

    void setProcess(ProcessRecord processRecord) {
        this.app = processRecord;
        if ((this.task != null ? this.task.getRootActivity() : null) == this) {
            this.task.setRootProcess(processRecord);
        }
    }

    AppWindowContainerController getWindowContainerController() {
        return this.mWindowContainerController;
    }

    void createWindowContainer() {
        if (this.mWindowContainerController == null) {
            this.inHistory = true;
            TaskWindowContainerController windowContainerController = this.task.getWindowContainerController();
            this.task.updateOverrideConfigurationFromLaunchBounds();
            updateOverrideConfiguration();
            this.mWindowContainerController = new AppWindowContainerController(windowContainerController, this.appToken, this, Integer.MAX_VALUE, this.info.screenOrientation, this.fullscreen, (this.info.flags & 1024) != 0, this.info.configChanges, this.task.voiceSession != null, this.mLaunchTaskBehind, isAlwaysFocusable(), this.appInfo.targetSdkVersion, this.mRotationAnimationHint, ActivityManagerService.getInputDispatchingTimeoutLocked(this) * 1000000);
            this.task.addActivityToTop(this);
            this.mLastReportedMultiWindowMode = inMultiWindowMode();
            this.mLastReportedPictureInPictureMode = inPinnedWindowingMode();
            return;
        }
        throw new IllegalArgumentException("Window container=" + this.mWindowContainerController + " already created for r=" + this);
    }

    void removeWindowContainer() {
        if (this.mWindowContainerController == null) {
            return;
        }
        resumeKeyDispatchingLocked();
        this.mWindowContainerController.removeContainer(getDisplayId());
        this.mWindowContainerController = null;
    }

    void reparent(TaskRecord taskRecord, int i, String str) {
        TaskRecord taskRecord2 = this.task;
        if (taskRecord2 == taskRecord) {
            throw new IllegalArgumentException(str + ": task=" + taskRecord + " is already the parent of r=" + this);
        }
        if (taskRecord2 != null && taskRecord != null && taskRecord2.getStack() != taskRecord.getStack()) {
            throw new IllegalArgumentException(str + ": task=" + taskRecord + " is in a different stack (" + taskRecord.getStackId() + ") than the parent of r=" + this + " (" + taskRecord2.getStackId() + ")");
        }
        this.mWindowContainerController.reparent(taskRecord.getWindowContainerController(), i);
        ActivityStack stack = taskRecord2.getStack();
        if (stack != taskRecord.getStack()) {
            stack.onActivityRemovedFromStack(this);
        }
        taskRecord2.removeActivity(this, true);
        taskRecord.addActivityAtIndex(i, this);
    }

    private boolean isHomeIntent(Intent intent) {
        return "android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.HOME") && intent.getCategories().size() == 1 && intent.getData() == null && intent.getType() == null;
    }

    static boolean isMainIntent(Intent intent) {
        return "android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.LAUNCHER") && intent.getCategories().size() == 1 && intent.getData() == null && intent.getType() == null;
    }

    private boolean canLaunchHomeActivity(int i, ActivityRecord activityRecord) {
        if (i == Process.myUid() || i == 0) {
            return true;
        }
        RecentTasks recentTasks = this.mStackSupervisor.mService.getRecentTasks();
        if (recentTasks == null || !recentTasks.isCallerRecents(i)) {
            return activityRecord != null && activityRecord.isResolverActivity();
        }
        return true;
    }

    private boolean canLaunchAssistActivity(String str) {
        ComponentName componentName = this.service.mActiveVoiceInteractionServiceComponent;
        if (componentName != null) {
            return componentName.getPackageName().equals(str);
        }
        return false;
    }

    private void setActivityType(boolean z, int i, Intent intent, ActivityOptions activityOptions, ActivityRecord activityRecord) {
        int i2 = 0;
        if ((!z || canLaunchHomeActivity(i, activityRecord)) && isHomeIntent(intent) && !isResolverActivity()) {
            if (this.info.resizeMode == 4 || this.info.resizeMode == 1) {
                this.info.resizeMode = 0;
            }
            i2 = 2;
        } else if (this.realActivity.getClassName().contains(LEGACY_RECENTS_PACKAGE_NAME) || this.service.getRecentTasks().isRecentsComponent(this.realActivity, this.appInfo.uid)) {
            i2 = 3;
        } else if (activityOptions != null && activityOptions.getLaunchActivityType() == 4 && canLaunchAssistActivity(this.launchedFromPackage)) {
            i2 = 4;
        }
        setActivityType(i2);
    }

    void setTaskToAffiliateWith(TaskRecord taskRecord) {
        if (this.launchMode != 3 && this.launchMode != 2) {
            this.task.setTaskToAffiliateWith(taskRecord);
        }
    }

    <T extends ActivityStack> T getStack() {
        if (this.task != null) {
            return (T) this.task.getStack();
        }
        return null;
    }

    int getStackId() {
        if (getStack() != null) {
            return getStack().mStackId;
        }
        return -1;
    }

    ActivityDisplay getDisplay() {
        ActivityStack stack = getStack();
        if (stack != null) {
            return stack.getDisplay();
        }
        return null;
    }

    boolean changeWindowTranslucency(boolean z) {
        if (this.fullscreen == z) {
            return false;
        }
        this.task.numFullscreen += z ? 1 : -1;
        this.fullscreen = z;
        return true;
    }

    void takeFromHistory() {
        if (this.inHistory) {
            this.inHistory = false;
            if (this.task != null && !this.finishing) {
                this.task = null;
            }
            clearOptionsLocked();
        }
    }

    boolean isInHistory() {
        return this.inHistory;
    }

    boolean isInStackLocked() {
        ActivityStack stack = getStack();
        return (stack == null || stack.isInStackLocked(this) == null) ? false : true;
    }

    boolean isPersistable() {
        return (this.info.persistableMode == 0 || this.info.persistableMode == 2) && (this.intent == null || (this.intent.getFlags() & DumpState.DUMP_VOLUMES) == 0);
    }

    boolean isFocusable() {
        return this.mStackSupervisor.isFocusable(this, isAlwaysFocusable());
    }

    boolean isResizeable() {
        return ActivityInfo.isResizeableMode(this.info.resizeMode) || this.info.supportsPictureInPicture();
    }

    boolean isNonResizableOrForcedResizable() {
        return (this.info.resizeMode == 2 || this.info.resizeMode == 1) ? false : true;
    }

    boolean supportsPictureInPicture() {
        return this.service.mSupportsPictureInPicture && isActivityTypeStandardOrUndefined() && this.info.supportsPictureInPicture();
    }

    @Override
    public boolean supportsSplitScreenWindowingMode() {
        return super.supportsSplitScreenWindowingMode() && this.service.mSupportsSplitScreenMultiWindow && supportsResizeableMultiWindow();
    }

    boolean supportsFreeform() {
        return this.service.mSupportsFreeformWindowManagement && supportsResizeableMultiWindow();
    }

    private boolean supportsResizeableMultiWindow() {
        return this.service.mSupportsMultiWindow && !isActivityTypeHome() && (ActivityInfo.isResizeableMode(this.info.resizeMode) || this.service.mForceResizableActivities);
    }

    boolean canBeLaunchedOnDisplay(int i) {
        TaskRecord task = getTask();
        return this.service.mStackSupervisor.canPlaceEntityOnDisplay(i, task != null ? task.isResizeable() : supportsResizeableMultiWindow(), this.launchedFromPid, this.launchedFromUid, this.info);
    }

    boolean checkEnterPictureInPictureState(String str, boolean z) {
        if (!supportsPictureInPicture() || !checkEnterPictureInPictureAppOpsState() || this.service.shouldDisableNonVrUiLocked()) {
            return false;
        }
        boolean zIsKeyguardLocked = this.service.isKeyguardLocked();
        boolean z2 = this.service.getLockTaskModeState() != 0;
        ActivityDisplay display = getDisplay();
        boolean z3 = display != null && display.hasPinnedStack();
        boolean z4 = (zIsKeyguardLocked || z2) ? false : true;
        if (z && z3) {
            return false;
        }
        switch (AnonymousClass1.$SwitchMap$com$android$server$am$ActivityStack$ActivityState[this.mState.ordinal()]) {
            case 1:
                if (z2) {
                    return false;
                }
                return this.supportsEnterPipOnTaskSwitch || !z;
            case 2:
            case 3:
                return z4 && !z3 && this.supportsEnterPipOnTaskSwitch;
            case 4:
                return this.supportsEnterPipOnTaskSwitch && z4 && !z3;
            default:
                return false;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$am$ActivityStack$ActivityState = new int[ActivityStack.ActivityState.values().length];

        static {
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityStack.ActivityState.RESUMED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityStack.ActivityState.PAUSING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityStack.ActivityState.PAUSED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityStack.ActivityState.STOPPING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private boolean checkEnterPictureInPictureAppOpsState() {
        try {
            return this.service.getAppOpsService().checkOperation(67, this.appInfo.uid, this.packageName) == 0;
        } catch (RemoteException e) {
            return false;
        }
    }

    boolean isAlwaysFocusable() {
        return (this.info.flags & DumpState.DUMP_DOMAIN_PREFERRED) != 0;
    }

    boolean hasDismissKeyguardWindows() {
        return this.service.mWindowManager.containsDismissKeyguardWindow(this.appToken);
    }

    void makeFinishingLocked() {
        if (this.finishing) {
            return;
        }
        this.finishing = true;
        if (this.stopped) {
            clearOptionsLocked();
        }
        if (this.service != null) {
            this.service.mTaskChangeNotificationController.notifyTaskStackChanged();
        }
    }

    UriPermissionOwner getUriPermissionsLocked() {
        if (this.uriPermissions == null) {
            this.uriPermissions = new UriPermissionOwner(this.service, this);
        }
        return this.uriPermissions;
    }

    void addResultLocked(ActivityRecord activityRecord, String str, int i, int i2, Intent intent) {
        ActivityResult activityResult = new ActivityResult(activityRecord, str, i, i2, intent);
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(activityResult);
    }

    void removeResultsLocked(ActivityRecord activityRecord, String str, int i) {
        if (this.results != null) {
            for (int size = this.results.size() - 1; size >= 0; size--) {
                ActivityResult activityResult = (ActivityResult) this.results.get(size);
                if (activityResult.mFrom == activityRecord) {
                    if (activityResult.mResultWho == null) {
                        if (str == null) {
                            if (activityResult.mRequestCode == i) {
                                this.results.remove(size);
                            }
                        }
                    } else if (!activityResult.mResultWho.equals(str)) {
                    }
                }
            }
        }
    }

    private void addNewIntentLocked(ReferrerIntent referrerIntent) {
        if (this.newIntents == null) {
            this.newIntents = new ArrayList<>();
        }
        this.newIntents.add(referrerIntent);
    }

    final boolean isSleeping() {
        ActivityStack stack = getStack();
        return stack != null ? stack.shouldSleepActivities() : this.service.isSleepingLocked();
    }

    final void deliverNewIntentLocked(int i, Intent intent, String str) {
        this.service.grantUriPermissionFromIntentLocked(i, this.packageName, intent, getUriPermissionsLocked(), this.userId);
        ReferrerIntent referrerIntent = new ReferrerIntent(intent, str);
        boolean z = false;
        boolean z2 = isTopRunningActivity() && isSleeping();
        if ((this.mState == ActivityStack.ActivityState.RESUMED || this.mState == ActivityStack.ActivityState.PAUSED || z2) && this.app != null && this.app.thread != null) {
            try {
                ArrayList arrayList = new ArrayList(1);
                arrayList.add(referrerIntent);
                this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ClientTransactionItem) NewIntentItem.obtain(arrayList, this.mState == ActivityStack.ActivityState.PAUSED));
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e);
                z = true;
            } catch (NullPointerException e2) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e2);
                z = true;
            }
        } else {
            z = true;
        }
        if (z) {
            addNewIntentLocked(referrerIntent);
        }
    }

    void updateOptionsLocked(ActivityOptions activityOptions) {
        if (activityOptions != null) {
            if (this.pendingOptions != null) {
                this.pendingOptions.abort();
            }
            this.pendingOptions = activityOptions;
        }
    }

    void applyOptionsLocked() {
        if (this.pendingOptions != null && this.pendingOptions.getAnimationType() != 5) {
            int animationType = this.pendingOptions.getAnimationType();
            boolean z = true;
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
                    boolean z2 = animationType == 3;
                    GraphicBuffer thumbnail = this.pendingOptions.getThumbnail();
                    this.service.mWindowManager.overridePendingAppTransitionThumb(thumbnail, this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getOnAnimationStartListener(), z2);
                    if (this.intent.getSourceBounds() == null && thumbnail != null) {
                        this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + thumbnail.getWidth(), this.pendingOptions.getStartY() + thumbnail.getHeight()));
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
                    AppTransitionAnimationSpec[] animSpecs = this.pendingOptions.getAnimSpecs();
                    IAppTransitionAnimationSpecsFuture specsFuture = this.pendingOptions.getSpecsFuture();
                    if (specsFuture != null) {
                        WindowManagerService windowManagerService = this.service.mWindowManager;
                        IRemoteCallback onAnimationStartListener = this.pendingOptions.getOnAnimationStartListener();
                        if (animationType != 8) {
                            z = false;
                        }
                        windowManagerService.overridePendingAppTransitionMultiThumbFuture(specsFuture, onAnimationStartListener, z);
                    } else if (animationType == 9 && animSpecs != null) {
                        this.service.mWindowManager.overridePendingAppTransitionMultiThumb(animSpecs, this.pendingOptions.getOnAnimationStartListener(), this.pendingOptions.getAnimationFinishedListener(), false);
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
                case 12:
                    this.service.mWindowManager.overridePendingAppTransitionStartCrossProfileApps();
                    break;
                case 13:
                    this.service.mWindowManager.overridePendingAppTransitionRemote(this.pendingOptions.getRemoteAnimationAdapter());
                    break;
            }
            if (this.task == null) {
                clearOptionsLocked(false);
            } else {
                this.task.clearAllPendingOptions();
            }
        }
    }

    ActivityOptions getOptionsForTargetActivityLocked() {
        if (this.pendingOptions != null) {
            return this.pendingOptions.forTargetActivity();
        }
        return null;
    }

    void clearOptionsLocked() {
        clearOptionsLocked(true);
    }

    void clearOptionsLocked(boolean z) {
        if (z && this.pendingOptions != null) {
            this.pendingOptions.abort();
        }
        this.pendingOptions = null;
    }

    ActivityOptions takeOptionsLocked() {
        ActivityOptions activityOptions = this.pendingOptions;
        this.pendingOptions = null;
        return activityOptions;
    }

    void removeUriPermissionsLocked() {
        if (this.uriPermissions != null) {
            this.uriPermissions.removeUriPermissionsLocked();
            this.uriPermissions = null;
        }
    }

    void pauseKeyDispatchingLocked() {
        if (!this.keysPaused) {
            this.keysPaused = true;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.pauseKeyDispatching();
            }
        }
    }

    void resumeKeyDispatchingLocked() {
        if (this.keysPaused) {
            this.keysPaused = false;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.resumeKeyDispatching();
            }
        }
    }

    private void updateTaskDescription(CharSequence charSequence) {
        this.task.lastDescription = charSequence;
    }

    void setDeferHidingClient(boolean z) {
        if (this.mDeferHidingClient == z) {
            return;
        }
        this.mDeferHidingClient = z;
        if (!this.mDeferHidingClient && !this.visible) {
            setVisibility(false);
        }
    }

    void setVisibility(boolean z) {
        this.mWindowContainerController.setVisibility(z, this.mDeferHidingClient);
        this.mStackSupervisor.getActivityMetricsLogger().notifyVisibilityChanged(this);
    }

    void setVisible(boolean z) {
        this.visible = z;
        this.mDeferHidingClient = !this.visible && this.mDeferHidingClient;
        setVisibility(this.visible);
        this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
    }

    void setState(ActivityStack.ActivityState activityState, String str) {
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "State movement: " + this + " from:" + getState() + " to:" + activityState + " reason:" + str);
        }
        if (activityState == this.mState) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "State unchanged from:" + activityState);
                return;
            }
            return;
        }
        this.mState = activityState;
        TaskRecord task = getTask();
        if (task != null) {
            task.onActivityStateChanged(this, activityState, str);
        }
        if (activityState == ActivityStack.ActivityState.STOPPING && !isSleeping()) {
            this.mWindowContainerController.notifyAppStopping();
        }
    }

    ActivityStack.ActivityState getState() {
        return this.mState;
    }

    boolean isState(ActivityStack.ActivityState activityState) {
        return activityState == this.mState;
    }

    boolean isState(ActivityStack.ActivityState activityState, ActivityStack.ActivityState activityState2) {
        return activityState == this.mState || activityState2 == this.mState;
    }

    boolean isState(ActivityStack.ActivityState activityState, ActivityStack.ActivityState activityState2, ActivityStack.ActivityState activityState3) {
        return activityState == this.mState || activityState2 == this.mState || activityState3 == this.mState;
    }

    boolean isState(ActivityStack.ActivityState activityState, ActivityStack.ActivityState activityState2, ActivityStack.ActivityState activityState3, ActivityStack.ActivityState activityState4) {
        return activityState == this.mState || activityState2 == this.mState || activityState3 == this.mState || activityState4 == this.mState;
    }

    void notifyAppResumed(boolean z) {
        this.mWindowContainerController.notifyAppResumed(z);
    }

    void notifyUnknownVisibilityLaunched() {
        if (!this.noDisplay) {
            this.mWindowContainerController.notifyUnknownVisibilityLaunched();
        }
    }

    boolean shouldBeVisibleIgnoringKeyguard(boolean z) {
        if (okToShowLocked()) {
            return !z || this.mLaunchTaskBehind;
        }
        return false;
    }

    void makeVisibleIfNeeded(ActivityRecord activityRecord, boolean z) {
        if (this.mState == ActivityStack.ActivityState.RESUMED || this == activityRecord) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.d(TAG_VISIBILITY, "Not making visible, r=" + this + " state=" + this.mState + " starting=" + activityRecord);
                return;
            }
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "Making visible and scheduling visibility: " + this);
        }
        ActivityStack stack = getStack();
        try {
            if (stack.mTranslucentActivityWaiting != null) {
                updateOptionsLocked(this.returningOptions);
                stack.mUndrawnActivitiesBelowTopTranslucent.add(this);
            }
            setVisible(true);
            this.sleeping = false;
            this.app.pendingUiClean = true;
            if (z) {
                makeClientVisible();
            } else {
                this.mClientVisibilityDeferred = true;
            }
            this.mStackSupervisor.mStoppingActivities.remove(this);
            this.mStackSupervisor.mGoingToSleepActivities.remove(this);
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown making visible: " + this.intent.getComponent(), e);
        }
        handleAlreadyVisible();
    }

    void makeClientVisible() {
        this.mClientVisibilityDeferred = false;
        try {
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ClientTransactionItem) WindowVisibilityItem.obtain(true));
            if (shouldPauseWhenBecomingVisible()) {
                setState(ActivityStack.ActivityState.PAUSING, "makeVisibleIfNeeded");
                this.service.getLifecycleManager().scheduleTransaction(this.app.thread, (IBinder) this.appToken, (ActivityLifecycleItem) PauseActivityItem.obtain(this.finishing, false, this.configChangeFlags, false));
            }
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown sending visibility update: " + this.intent.getComponent(), e);
        }
    }

    private boolean shouldPauseWhenBecomingVisible() {
        if (!isState(ActivityStack.ActivityState.STOPPED, ActivityStack.ActivityState.STOPPING) || getStack().mTranslucentActivityWaiting != null || this.mStackSupervisor.getResumedActivityLocked() == this) {
            return false;
        }
        int iIndexOf = this.task.mActivities.indexOf(this);
        if (iIndexOf == -1) {
            throw new IllegalStateException("Activity not found in its task");
        }
        if (iIndexOf == this.task.mActivities.size() - 1) {
            return true;
        }
        return this.task.mActivities.get(iIndexOf + 1).finishing && this.results == null;
    }

    boolean handleAlreadyVisible() {
        stopFreezingScreenLocked(false);
        try {
            if (this.returningOptions != null) {
                this.app.thread.scheduleOnNewActivityOptions(this.appToken, this.returningOptions.toBundle());
            }
        } catch (RemoteException e) {
        }
        return this.mState == ActivityStack.ActivityState.RESUMED;
    }

    static void activityResumedLocked(IBinder iBinder) {
        ActivityRecord activityRecordForTokenLocked = forTokenLocked(iBinder);
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            Slog.i(TAG_STATES, "Resumed activity; dropping state of: " + activityRecordForTokenLocked);
        }
        if (activityRecordForTokenLocked != null) {
            activityRecordForTokenLocked.icicle = null;
            activityRecordForTokenLocked.haveState = false;
        }
    }

    void completeResumeLocked() {
        ProcessRecord processRecord;
        boolean z = this.visible;
        setVisible(true);
        if (!z) {
            this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
        }
        this.idle = false;
        this.results = null;
        this.newIntents = null;
        this.stopped = false;
        if (isActivityTypeHome() && (processRecord = this.task.mActivities.get(0).app) != null && processRecord != this.service.mHomeProcess) {
            this.service.mHomeProcess = processRecord;
        }
        if (this.nowVisible) {
            this.mStackSupervisor.reportActivityVisibleLocked(this);
        }
        this.mStackSupervisor.scheduleIdleTimeoutLocked(this);
        this.mStackSupervisor.reportResumedActivityLocked(this);
        resumeKeyDispatchingLocked();
        ActivityStack stack = getStack();
        this.mStackSupervisor.mNoAnimActivities.clear();
        if (this.app != null) {
            this.cpuTimeAtResume = this.service.mProcessCpuTracker.getCpuTimeForPid(this.app.pid);
        } else {
            this.cpuTimeAtResume = 0L;
        }
        this.returningOptions = null;
        if (canTurnScreenOn()) {
            this.mStackSupervisor.wakeUp("turnScreenOnFlag");
        } else {
            stack.checkReadyForSleep();
        }
    }

    final void activityStoppedLocked(Bundle bundle, PersistableBundle persistableBundle, CharSequence charSequence) {
        ActivityStack stack = getStack();
        if (this.mState != ActivityStack.ActivityState.STOPPING) {
            Slog.i(TAG, "Activity reported stop, but no longer stopping: " + this);
            stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_SELECT_MEDIA_FUNCTION, this);
            return;
        }
        if (persistableBundle != null) {
            this.persistentState = persistableBundle;
            this.service.notifyTaskPersisterLocked(this.task, false);
        }
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            Slog.i(TAG_SAVED_STATE, "Saving icicle of " + this + ": " + this.icicle);
        }
        if (bundle != null) {
            this.icicle = bundle;
            this.haveState = true;
            this.launchCount = 0;
            updateTaskDescription(charSequence);
        }
        if (!this.stopped) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to STOPPED: " + this + " (stop complete)");
            }
            stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_SELECT_MEDIA_FUNCTION, this);
            this.stopped = true;
            setState(ActivityStack.ActivityState.STOPPED, "activityStoppedLocked");
            this.mWindowContainerController.notifyAppStopped();
            if (this.finishing) {
                clearOptionsLocked();
            } else if (this.deferRelaunchUntilPaused) {
                stack.destroyActivityLocked(this, true, "stop-config");
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            } else {
                this.mStackSupervisor.updatePreviousProcessLocked(this);
            }
        }
    }

    void startLaunchTickingLocked() {
        if (!Build.IS_USER && this.launchTickTime == 0) {
            this.launchTickTime = SystemClock.uptimeMillis();
            continueLaunchTickingLocked();
        }
    }

    boolean continueLaunchTickingLocked() {
        ActivityStack stack;
        if (this.launchTickTime == 0 || (stack = getStack()) == null) {
            return false;
        }
        Message messageObtainMessage = stack.mHandler.obtainMessage(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION, this);
        stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION);
        stack.mHandler.sendMessageDelayed(messageObtainMessage, 500L);
        return true;
    }

    void finishLaunchTickingLocked() {
        this.launchTickTime = 0L;
        ActivityStack stack = getStack();
        if (stack != null) {
            stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION);
        }
    }

    public boolean mayFreezeScreenLocked(ProcessRecord processRecord) {
        return (processRecord == null || processRecord.crashing || processRecord.notResponding) ? false : true;
    }

    public void startFreezingScreenLocked(ProcessRecord processRecord, int i) {
        if (mayFreezeScreenLocked(processRecord)) {
            this.mWindowContainerController.startFreezingScreen(i);
        }
    }

    public void stopFreezingScreenLocked(boolean z) {
        if (z || this.frozenBeforeDestroy) {
            this.frozenBeforeDestroy = false;
            this.mWindowContainerController.stopFreezingScreen(z);
        }
    }

    public void reportFullyDrawnLocked(boolean z) {
        long j;
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (this.displayStartTime != 0) {
            reportLaunchTimeLocked(jUptimeMillis);
        }
        LaunchTimeTracker.Entry entry = this.mStackSupervisor.getLaunchTimeTracker().getEntry(getWindowingMode());
        if (this.fullyDrawnStartTime != 0 && entry != null) {
            long j2 = jUptimeMillis - this.fullyDrawnStartTime;
            if (entry.mFullyDrawnStartTime != 0) {
                j = jUptimeMillis - entry.mFullyDrawnStartTime;
            } else {
                j = j2;
            }
            Trace.asyncTraceEnd(64L, "drawing", 0);
            EventLog.writeEvent(EventLogTags.AM_ACTIVITY_FULLY_DRAWN_TIME, Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), this.shortComponentName, Long.valueOf(j2), Long.valueOf(j));
            StringBuilder sb = this.service.mStringBuilder;
            sb.setLength(0);
            sb.append("Fully drawn ");
            sb.append(this.shortComponentName);
            sb.append(": ");
            TimeUtils.formatDuration(j2, sb);
            if (j2 != j) {
                sb.append(" (total ");
                TimeUtils.formatDuration(j, sb);
                sb.append(")");
            }
            Log.i(TAG, sb.toString());
            entry.mFullyDrawnStartTime = 0L;
        }
        this.mStackSupervisor.getActivityMetricsLogger().logAppTransitionReportedDrawn(this, z);
        this.fullyDrawnStartTime = 0L;
    }

    private void reportLaunchTimeLocked(long j) {
        LaunchTimeTracker.Entry entry = this.mStackSupervisor.getLaunchTimeTracker().getEntry(getWindowingMode());
        if (entry == null) {
            return;
        }
        long j2 = j - this.displayStartTime;
        long j3 = entry.mLaunchStartTime != 0 ? j - entry.mLaunchStartTime : j2;
        Trace.asyncTraceEnd(64L, "launching: " + this.packageName, 0);
        EventLog.writeEvent(EventLogTags.AM_ACTIVITY_LAUNCH_TIME, Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), this.shortComponentName, Long.valueOf(j2), Long.valueOf(j3));
        StringBuilder sb = this.service.mStringBuilder;
        sb.setLength(0);
        sb.append("Displayed ");
        sb.append(this.shortComponentName);
        sb.append(": ");
        TimeUtils.formatDuration(j2, sb);
        if (j2 != j3) {
            sb.append(" (total ");
            TimeUtils.formatDuration(j3, sb);
            sb.append(")");
        }
        Log.i(TAG, sb.toString());
        ActivityManagerService activityManagerService = this.service;
        ActivityManagerService.sMtkSystemServerIns.addBootEvent("AP_Launch: " + this.shortComponentName + " " + j2 + "ms");
        this.mStackSupervisor.reportActivityLaunchedLocked(false, this, j2, j3);
        this.displayStartTime = 0L;
        entry.mLaunchStartTime = 0L;
    }

    @Override
    public void onStartingWindowDrawn(long j) {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mStackSupervisor.getActivityMetricsLogger().notifyStartingWindowDrawn(getWindowingMode(), j);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public void onWindowsDrawn(long j) {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mStackSupervisor.getActivityMetricsLogger().notifyWindowsDrawn(getWindowingMode(), j);
                if (this.displayStartTime != 0) {
                    reportLaunchTimeLocked(j);
                }
                this.mStackSupervisor.sendWaitingVisibleReportLocked(this);
                this.startTime = 0L;
                finishLaunchTickingLocked();
                if (this.task != null) {
                    this.task.hasBeenVisible = true;
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public void onWindowsVisible() {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mStackSupervisor.reportActivityVisibleLocked(this);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Log.v(TAG_SWITCH, "windowsVisibleLocked(): " + this);
                }
                if (!this.nowVisible) {
                    this.nowVisible = true;
                    this.lastVisibleTime = SystemClock.uptimeMillis();
                    if (!this.idle && !this.mStackSupervisor.isStoppingNoHistoryActivity()) {
                        this.mStackSupervisor.processStoppingActivitiesLocked(null, false, true);
                    } else {
                        int size = this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.size();
                        if (size > 0) {
                            for (int i = 0; i < size; i++) {
                                ActivityRecord activityRecord = this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.get(i);
                                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                    Log.v(TAG_SWITCH, "Was waiting for visible: " + activityRecord);
                                }
                            }
                            this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.clear();
                            this.mStackSupervisor.scheduleIdleLocked();
                        }
                    }
                    this.service.scheduleAppGcsLocked();
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public void onWindowsGone() {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Log.v(TAG_SWITCH, "windowsGone(): " + this);
                }
                this.nowVisible = false;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public boolean keyDispatchingTimedOut(String str, int i) {
        ActivityRecord waitingHistoryRecordLocked;
        ProcessRecord processRecord;
        boolean z;
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                waitingHistoryRecordLocked = getWaitingHistoryRecordLocked();
                processRecord = this.app;
                z = this.app == null || this.app.pid == i || i == -1;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (z) {
            return this.service.inputDispatchingTimedOut(processRecord, waitingHistoryRecordLocked, this, false, str);
        }
        return this.service.inputDispatchingTimedOut(i, false, str) < 0;
    }

    private ActivityRecord getWaitingHistoryRecordLocked() {
        if (this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(this) || this.stopped) {
            ActivityStack focusedStack = this.mStackSupervisor.getFocusedStack();
            ActivityRecord resumedActivity = focusedStack.getResumedActivity();
            if (resumedActivity == null) {
                resumedActivity = focusedStack.mPausingActivity;
            }
            if (resumedActivity != null) {
                return resumedActivity;
            }
        }
        return this;
    }

    public boolean okToShowLocked() {
        if (StorageManager.isUserKeyUnlocked(this.userId) || this.info.applicationInfo.isEncryptionAware()) {
            return (this.info.flags & 1024) != 0 || (this.mStackSupervisor.isCurrentProfileLocked(this.userId) && this.service.mUserController.isUserRunning(this.userId, 0));
        }
        return false;
    }

    public boolean isInterestingToUserLocked() {
        return this.visible || this.nowVisible || this.mState == ActivityStack.ActivityState.PAUSING || this.mState == ActivityStack.ActivityState.RESUMED;
    }

    void setSleeping(boolean z) {
        setSleeping(z, false);
    }

    void setSleeping(boolean z, boolean z2) {
        if ((z2 || this.sleeping != z) && this.app != null && this.app.thread != null) {
            try {
                this.app.thread.scheduleSleeping(this.appToken, z);
                if (z && !this.mStackSupervisor.mGoingToSleepActivities.contains(this)) {
                    this.mStackSupervisor.mGoingToSleepActivities.add(this);
                }
                this.sleeping = z;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception thrown when sleeping: " + this.intent.getComponent(), e);
            }
        }
    }

    static int getTaskForActivityLocked(IBinder iBinder, boolean z) {
        ActivityRecord activityRecordForTokenLocked = forTokenLocked(iBinder);
        if (activityRecordForTokenLocked == null) {
            return -1;
        }
        TaskRecord taskRecord = activityRecordForTokenLocked.task;
        int iIndexOf = taskRecord.mActivities.indexOf(activityRecordForTokenLocked);
        if (iIndexOf < 0 || (z && iIndexOf > taskRecord.findEffectiveRootIndex())) {
            return -1;
        }
        return taskRecord.taskId;
    }

    static ActivityRecord isInStackLocked(IBinder iBinder) {
        ActivityRecord activityRecordForTokenLocked = forTokenLocked(iBinder);
        if (activityRecordForTokenLocked != null) {
            return activityRecordForTokenLocked.getStack().isInStackLocked(activityRecordForTokenLocked);
        }
        return null;
    }

    static ActivityStack getStackLocked(IBinder iBinder) {
        ActivityRecord activityRecordIsInStackLocked = isInStackLocked(iBinder);
        if (activityRecordIsInStackLocked != null) {
            return activityRecordIsInStackLocked.getStack();
        }
        return null;
    }

    int getDisplayId() {
        ActivityStack stack = getStack();
        if (stack == null) {
            return -1;
        }
        return stack.mDisplayId;
    }

    final boolean isDestroyable() {
        ActivityStack stack;
        return (this.finishing || this.app == null || (stack = getStack()) == null || this == stack.getResumedActivity() || this == stack.mPausingActivity || !this.haveState || !this.stopped || this.visible) ? false : true;
    }

    private static String createImageFilename(long j, int i) {
        return String.valueOf(i) + ACTIVITY_ICON_SUFFIX + j + ".png";
    }

    void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        Bitmap icon;
        if (taskDescription.getIconFilename() == null && (icon = taskDescription.getIcon()) != null) {
            String absolutePath = new File(TaskPersister.getUserImagesDir(this.task.userId), createImageFilename(this.createTime, this.task.taskId)).getAbsolutePath();
            this.service.getRecentTasks().saveImage(icon, absolutePath);
            taskDescription.setIconFilename(absolutePath);
        }
        this.taskDescription = taskDescription;
    }

    void setVoiceSessionLocked(IVoiceInteractionSession iVoiceInteractionSession) {
        this.voiceSession = iVoiceInteractionSession;
        this.pendingVoiceInteractionStart = false;
    }

    void clearVoiceSessionLocked() {
        this.voiceSession = null;
        this.pendingVoiceInteractionStart = false;
    }

    void showStartingWindow(ActivityRecord activityRecord, boolean z, boolean z2) {
        showStartingWindow(activityRecord, z, z2, false);
    }

    void showStartingWindow(ActivityRecord activityRecord, boolean z, boolean z2, boolean z3) {
        if (this.mWindowContainerController == null || this.mTaskOverlay) {
            return;
        }
        if (this.mWindowContainerController.addStartingWindow(this.packageName, this.theme, this.service.compatibilityInfoForPackageLocked(this.info.applicationInfo), this.nonLocalizedLabel, this.labelRes, this.icon, this.logo, this.windowFlags, activityRecord != null ? activityRecord.appToken : null, z, z2, isProcessRunning(), allowTaskSnapshot(), this.mState.ordinal() >= ActivityStack.ActivityState.RESUMED.ordinal() && this.mState.ordinal() <= ActivityStack.ActivityState.STOPPED.ordinal(), z3)) {
            this.mStartingWindowState = 1;
        }
    }

    void removeOrphanedStartingWindow(boolean z) {
        if (this.mStartingWindowState == 1 && z) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.w(TAG_VISIBILITY, "Found orphaned starting window " + this);
            }
            this.mStartingWindowState = 2;
            this.mWindowContainerController.removeStartingWindow();
        }
    }

    int getRequestedOrientation() {
        return this.mWindowContainerController.getOrientation();
    }

    void setRequestedOrientation(int i) {
        int displayId = getDisplayId();
        Configuration orientation = this.mWindowContainerController.setOrientation(i, displayId, this.mStackSupervisor.getDisplayOverrideConfiguration(displayId), mayFreezeScreenLocked(this.app));
        if (orientation != null) {
            this.frozenBeforeDestroy = true;
            if (!this.service.updateDisplayOverrideConfigurationLocked(orientation, this, false, displayId)) {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
        }
        this.service.mTaskChangeNotificationController.notifyActivityRequestedOrientationChanged(this.task.taskId, i);
    }

    void setDisablePreviewScreenshots(boolean z) {
        this.mWindowContainerController.setDisablePreviewScreenshots(z);
    }

    void setLastReportedGlobalConfiguration(Configuration configuration) {
        this.mLastReportedConfiguration.setGlobalConfiguration(configuration);
    }

    void setLastReportedConfiguration(MergedConfiguration mergedConfiguration) {
        setLastReportedConfiguration(mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration());
    }

    private void setLastReportedConfiguration(Configuration configuration, Configuration configuration2) {
        this.mLastReportedConfiguration.setConfiguration(configuration, configuration2);
    }

    private void updateOverrideConfiguration() {
        this.mTmpConfig.unset();
        computeBounds(this.mTmpBounds);
        if (this.mTmpBounds.equals(getOverrideBounds())) {
            return;
        }
        setBounds(this.mTmpBounds);
        Rect overrideBounds = getOverrideBounds();
        if (!matchParentBounds()) {
            this.task.computeOverrideConfiguration(this.mTmpConfig, overrideBounds, null, false, false);
        }
        onOverrideConfigurationChanged(this.mTmpConfig);
    }

    boolean isConfigurationCompatible(Configuration configuration) {
        int orientation = this.mWindowContainerController != null ? this.mWindowContainerController.getOrientation() : this.info.screenOrientation;
        if (!ActivityInfo.isFixedOrientationPortrait(orientation) || configuration.orientation == 1) {
            return !ActivityInfo.isFixedOrientationLandscape(orientation) || configuration.orientation == 2;
        }
        return false;
    }

    private void computeBounds(Rect rect) {
        int i;
        int i2;
        rect.setEmpty();
        float f = this.info.maxAspectRatio;
        ActivityStack stack = getStack();
        if (this.task == null || stack == null || this.task.inMultiWindowMode() || f == 0.0f || isInVrUiMode(getConfiguration())) {
            return;
        }
        Rect appBounds = getParent().getWindowConfiguration().getAppBounds();
        int iWidth = appBounds.width();
        int iHeight = appBounds.height();
        if (iWidth < iHeight) {
            i2 = (int) ((iWidth * f) + 0.5f);
            i = iWidth;
        } else {
            i = (int) ((iHeight * f) + 0.5f);
            i2 = iHeight;
        }
        if (iWidth <= i && iHeight <= i2) {
            rect.set(getOverrideBounds());
            return;
        }
        rect.set(0, 0, appBounds.left + i, i2 + appBounds.top);
        if (this.service.mWindowManager.getNavBarPosition() == 1) {
            rect.left = appBounds.right - i;
            rect.right = appBounds.right;
        }
    }

    boolean ensureActivityConfiguration(int i, boolean z) {
        return ensureActivityConfiguration(i, z, false);
    }

    boolean ensureActivityConfiguration(int i, boolean z, boolean z2) {
        ActivityStack stack = getStack();
        if (stack.mConfigWillChange) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Skipping config check (will change): " + this);
            }
            return true;
        }
        if (this.finishing) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration doesn't matter in finishing " + this);
            }
            stopFreezingScreenLocked(false);
            return true;
        }
        if (!z2 && (this.mState == ActivityStack.ActivityState.STOPPING || this.mState == ActivityStack.ActivityState.STOPPED)) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Skipping config check stopped or stopping: " + this);
            }
            return true;
        }
        if (!stack.shouldBeVisible(null)) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Skipping config check invisible stack: " + this);
            }
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG_CONFIGURATION, "Ensuring correct configuration: " + this);
        }
        int displayId = getDisplayId();
        boolean z3 = this.mLastReportedDisplayId != displayId;
        if (z3) {
            this.mLastReportedDisplayId = displayId;
        }
        updateOverrideConfiguration();
        this.mTmpConfig.setTo(this.mLastReportedConfiguration.getMergedConfiguration());
        if (getConfiguration().equals(this.mTmpConfig) && !this.forceNewConfig && !z3) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration & display unchanged in " + this);
            }
            return true;
        }
        int configurationChanges = getConfigurationChanges(this.mTmpConfig);
        Configuration mergedOverrideConfiguration = getMergedOverrideConfiguration();
        setLastReportedConfiguration(this.service.getGlobalConfiguration(), mergedOverrideConfiguration);
        if (this.mState == ActivityStack.ActivityState.INITIALIZING) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Skipping config check for initializing activity: " + this);
            }
            return true;
        }
        if (configurationChanges == 0 && !this.forceNewConfig) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration no differences in " + this);
            }
            if (z3) {
                scheduleActivityMovedToDisplay(displayId, mergedOverrideConfiguration);
            } else {
                scheduleConfigurationChanged(mergedOverrideConfiguration);
            }
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG_CONFIGURATION, "Configuration changes for " + this + ", allChanges=" + Configuration.configurationDiffToString(configurationChanges));
        }
        if (this.app == null || this.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration doesn't matter not running " + this);
            }
            stopFreezingScreenLocked(false);
            this.forceNewConfig = false;
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG_CONFIGURATION, "Checking to restart " + this.info.name + ": changed=0x" + Integer.toHexString(configurationChanges) + ", handles=0x" + Integer.toHexString(this.info.getRealConfigChanged()) + ", mLastReportedConfiguration=" + this.mLastReportedConfiguration);
        }
        if (shouldRelaunchLocked(configurationChanges, this.mTmpConfig) || this.forceNewConfig) {
            this.configChangeFlags |= configurationChanges;
            startFreezingScreenLocked(this.app, i);
            this.forceNewConfig = false;
            boolean zIsResizeOnlyChange = isResizeOnlyChange(configurationChanges) & z;
            if (this.app == null || this.app.thread == null) {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Config is destroying non-running " + this);
                }
                stack.destroyActivityLocked(this, true, "config");
            } else {
                if (this.mState == ActivityStack.ActivityState.PAUSING) {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        Slog.v(TAG_CONFIGURATION, "Config is skipping already pausing " + this);
                    }
                    this.deferRelaunchUntilPaused = true;
                    this.preserveWindowOnDeferredRelaunch = zIsResizeOnlyChange;
                    return true;
                }
                if (this.mState == ActivityStack.ActivityState.RESUMED) {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        Slog.v(TAG_CONFIGURATION, "Config is relaunching resumed " + this);
                    }
                    if (ActivityManagerDebugConfig.DEBUG_STATES && !this.visible) {
                        Slog.v(TAG_STATES, "Config is relaunching resumed invisible activity " + this + " called by " + Debug.getCallers(4));
                    }
                    relaunchActivityLocked(true, zIsResizeOnlyChange);
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        Slog.v(TAG_CONFIGURATION, "Config is relaunching non-resumed " + this);
                    }
                    relaunchActivityLocked(false, zIsResizeOnlyChange);
                }
            }
            return false;
        }
        if (z3) {
            scheduleActivityMovedToDisplay(displayId, mergedOverrideConfiguration);
        } else {
            scheduleConfigurationChanged(mergedOverrideConfiguration);
        }
        stopFreezingScreenLocked(false);
        return true;
    }

    private boolean shouldRelaunchLocked(int i, Configuration configuration) {
        int realConfigChanged = this.info.getRealConfigChanged();
        boolean zOnlyVrUiModeChanged = onlyVrUiModeChanged(i, configuration);
        if (this.appInfo.targetSdkVersion < 26 && this.requestedVrComponent != null && zOnlyVrUiModeChanged) {
            realConfigChanged |= 512;
        }
        return (i & (~realConfigChanged)) != 0;
    }

    private boolean onlyVrUiModeChanged(int i, Configuration configuration) {
        return i == 512 && isInVrUiMode(getConfiguration()) != isInVrUiMode(configuration);
    }

    private int getConfigurationChanges(Configuration configuration) {
        Configuration configuration2 = getConfiguration();
        int iDiff = configuration.diff(configuration2);
        if ((iDiff & 1024) != 0) {
            if (!(crossesHorizontalSizeThreshold(configuration.screenWidthDp, configuration2.screenWidthDp) || crossesVerticalSizeThreshold(configuration.screenHeightDp, configuration2.screenHeightDp))) {
                iDiff &= -1025;
            }
        }
        if ((iDiff & 2048) != 0 && !crossesSmallestSizeThreshold(configuration.smallestScreenWidthDp, configuration2.smallestScreenWidthDp)) {
            iDiff &= -2049;
        }
        if ((536870912 & iDiff) != 0) {
            return iDiff & (-536870913);
        }
        return iDiff;
    }

    private static boolean isResizeOnlyChange(int i) {
        return (i & (-3457)) == 0;
    }

    void relaunchActivityLocked(boolean z, boolean z2) {
        ArrayList<ResultInfo> arrayList;
        ArrayList<ReferrerIntent> arrayList2;
        ResumeActivityItem resumeActivityItemObtain;
        if (this.service.mSuppressResizeConfigChanges && z2) {
            this.configChangeFlags = 0;
            return;
        }
        if (z) {
            arrayList = this.results;
            arrayList2 = this.newIntents;
        } else {
            arrayList = null;
            arrayList2 = null;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "Relaunching: " + this + " with results=" + arrayList + " newIntents=" + arrayList2 + " andResume=" + z + " preserveWindow=" + z2);
        }
        EventLog.writeEvent(z ? EventLogTags.AM_RELAUNCH_RESUME_ACTIVITY : EventLogTags.AM_RELAUNCH_ACTIVITY, Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(this.task.taskId), this.shortComponentName);
        startFreezingScreenLocked(this.app, 0);
        try {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                String str = TAG_SWITCH;
                StringBuilder sb = new StringBuilder();
                sb.append("Moving to ");
                sb.append(z ? "RESUMED" : "PAUSED");
                sb.append(" Relaunching ");
                sb.append(this);
                sb.append(" callers=");
                sb.append(Debug.getCallers(6));
                Slog.i(str, sb.toString());
            }
            this.forceNewConfig = false;
            this.mStackSupervisor.activityRelaunchingLocked(this);
            ActivityRelaunchItem activityRelaunchItemObtain = ActivityRelaunchItem.obtain(arrayList, arrayList2, this.configChangeFlags, new MergedConfiguration(this.service.getGlobalConfiguration(), getMergedOverrideConfiguration()), z2);
            if (z) {
                resumeActivityItemObtain = ResumeActivityItem.obtain(this.service.isNextTransitionForward());
            } else {
                resumeActivityItemObtain = PauseActivityItem.obtain();
            }
            ClientTransaction clientTransactionObtain = ClientTransaction.obtain(this.app.thread, this.appToken);
            clientTransactionObtain.addCallback(activityRelaunchItemObtain);
            clientTransactionObtain.setLifecycleStateRequest(resumeActivityItemObtain);
            this.service.getLifecycleManager().scheduleTransaction(clientTransactionObtain);
        } catch (RemoteException e) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.i(TAG_SWITCH, "Relaunch failed", e);
            }
        }
        if (z) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "Resumed after relaunch " + this);
            }
            this.results = null;
            this.newIntents = null;
            this.service.getAppWarningsLocked().onResumeActivity(this);
            this.service.showAskCompatModeDialogLocked(this);
            this.service.mAmsExt.onAfterActivityResumed(this);
        } else {
            this.service.mHandler.removeMessages(101, this);
            setState(ActivityStack.ActivityState.PAUSED, "relaunchActivityLocked");
        }
        this.configChangeFlags = 0;
        this.deferRelaunchUntilPaused = false;
        this.preserveWindowOnDeferredRelaunch = false;
    }

    private boolean isProcessRunning() {
        ProcessRecord processRecord = this.app;
        if (processRecord == null) {
            processRecord = (ProcessRecord) this.service.mProcessNames.get(this.processName, this.info.applicationInfo.uid);
        }
        return (processRecord == null || processRecord.thread == null) ? false : true;
    }

    private boolean allowTaskSnapshot() {
        if (this.newIntents == null) {
            return true;
        }
        for (int size = this.newIntents.size() - 1; size >= 0; size--) {
            Intent intent = this.newIntents.get(size);
            if (intent != null && !isMainIntent(intent)) {
                return false;
            }
        }
        return true;
    }

    boolean isNoHistory() {
        return ((this.intent.getFlags() & 1073741824) == 0 && (this.info.flags & 128) == 0) ? false : true;
    }

    void saveToXml(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        xmlSerializer.attribute(null, ATTR_ID, String.valueOf(this.createTime));
        xmlSerializer.attribute(null, ATTR_LAUNCHEDFROMUID, String.valueOf(this.launchedFromUid));
        if (this.launchedFromPackage != null) {
            xmlSerializer.attribute(null, ATTR_LAUNCHEDFROMPACKAGE, this.launchedFromPackage);
        }
        if (this.resolvedType != null) {
            xmlSerializer.attribute(null, ATTR_RESOLVEDTYPE, this.resolvedType);
        }
        xmlSerializer.attribute(null, ATTR_COMPONENTSPECIFIED, String.valueOf(this.componentSpecified));
        xmlSerializer.attribute(null, ATTR_USERID, String.valueOf(this.userId));
        if (this.taskDescription != null) {
            this.taskDescription.saveToXml(xmlSerializer);
        }
        xmlSerializer.startTag(null, TAG_INTENT);
        this.intent.saveToXml(xmlSerializer);
        xmlSerializer.endTag(null, TAG_INTENT);
        if (isPersistable() && this.persistentState != null) {
            xmlSerializer.startTag(null, TAG_PERSISTABLEBUNDLE);
            this.persistentState.saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, TAG_PERSISTABLEBUNDLE);
        }
    }

    static ActivityRecord restoreFromXml(XmlPullParser xmlPullParser, ActivityStackSupervisor activityStackSupervisor) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription();
        Intent intentRestoreFromXml = null;
        int i = 0;
        int i2 = 0;
        boolean z = false;
        String str = null;
        String str2 = null;
        long j = -1;
        for (int attributeCount = xmlPullParser.getAttributeCount() - 1; attributeCount >= 0; attributeCount--) {
            String attributeName = xmlPullParser.getAttributeName(attributeCount);
            String attributeValue = xmlPullParser.getAttributeValue(attributeCount);
            if (ATTR_ID.equals(attributeName)) {
                j = Long.parseLong(attributeValue);
            } else if (ATTR_LAUNCHEDFROMUID.equals(attributeName)) {
                i = Integer.parseInt(attributeValue);
            } else if (ATTR_LAUNCHEDFROMPACKAGE.equals(attributeName)) {
                str2 = attributeValue;
            } else if (ATTR_RESOLVEDTYPE.equals(attributeName)) {
                str = attributeValue;
            } else if (ATTR_COMPONENTSPECIFIED.equals(attributeName)) {
                z = Boolean.parseBoolean(attributeValue);
            } else if (ATTR_USERID.equals(attributeName)) {
                i2 = Integer.parseInt(attributeValue);
            } else if (attributeName.startsWith("task_description_")) {
                taskDescription.restoreFromXml(attributeName, attributeValue);
            } else {
                Log.d(TAG, "Unknown ActivityRecord attribute=" + attributeName);
            }
        }
        PersistableBundle persistableBundleRestoreFromXml = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() < depth)) {
                break;
            }
            if (next == 2) {
                String name = xmlPullParser.getName();
                if (TAG_INTENT.equals(name)) {
                    intentRestoreFromXml = Intent.restoreFromXml(xmlPullParser);
                } else if (TAG_PERSISTABLEBUNDLE.equals(name)) {
                    persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParser);
                } else {
                    Slog.w(TAG, "restoreActivity: unexpected name=" + name);
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        if (intentRestoreFromXml == null) {
            throw new XmlPullParserException("restoreActivity error intent=" + intentRestoreFromXml);
        }
        ActivityManagerService activityManagerService = activityStackSupervisor.mService;
        ActivityInfo activityInfoResolveActivity = activityStackSupervisor.resolveActivity(intentRestoreFromXml, str, 0, null, i2, Binder.getCallingUid());
        if (activityInfoResolveActivity == null) {
            throw new XmlPullParserException("restoreActivity resolver error. Intent=" + intentRestoreFromXml + " resolvedType=" + str);
        }
        ActivityRecord activityRecord = new ActivityRecord(activityManagerService, null, 0, i, str2, intentRestoreFromXml, str, activityInfoResolveActivity, activityManagerService.getConfiguration(), null, null, 0, z, false, activityStackSupervisor, null, null);
        activityRecord.persistentState = persistableBundleRestoreFromXml;
        activityRecord.taskDescription = taskDescription;
        activityRecord.createTime = j;
        return activityRecord;
    }

    private static boolean isInVrUiMode(Configuration configuration) {
        return (configuration.uiMode & 15) == 7;
    }

    int getUid() {
        return this.info.applicationInfo.uid;
    }

    void setShowWhenLocked(boolean z) {
        this.mShowWhenLocked = z;
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    boolean canShowWhenLocked() {
        return !inPinnedWindowingMode() && (this.mShowWhenLocked || this.service.mWindowManager.containsShowWhenLockedWindow(this.appToken));
    }

    void setTurnScreenOn(boolean z) {
        this.mTurnScreenOn = z;
    }

    boolean canTurnScreenOn() {
        ActivityStack stack = getStack();
        return this.mTurnScreenOn && stack != null && stack.checkKeyguardVisibility(this, true, true);
    }

    boolean getTurnScreenOnFlag() {
        return this.mTurnScreenOn;
    }

    boolean isTopRunningActivity() {
        return this.mStackSupervisor.topRunningActivityLocked() == this;
    }

    void registerRemoteAnimations(RemoteAnimationDefinition remoteAnimationDefinition) {
        this.mWindowContainerController.registerRemoteAnimations(remoteAnimationDefinition);
    }

    public String toString() {
        if (this.stringName != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.stringName);
            sb.append(" t");
            sb.append(this.task == null ? -1 : this.task.taskId);
            sb.append(this.finishing ? " f}" : "}");
            return sb.toString();
        }
        StringBuilder sb2 = new StringBuilder(128);
        sb2.append("ActivityRecord{");
        sb2.append(Integer.toHexString(System.identityHashCode(this)));
        sb2.append(" u");
        sb2.append(this.userId);
        sb2.append(' ');
        sb2.append(this.intent.getComponent().flattenToShortString());
        this.stringName = sb2.toString();
        return toString();
    }

    void writeIdentifierToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, System.identityHashCode(this));
        protoOutputStream.write(1120986464258L, this.userId);
        protoOutputStream.write(1138166333443L, this.intent.getComponent().flattenToShortString());
        protoOutputStream.end(jStart);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, false);
        writeIdentifierToProto(protoOutputStream, 1146756268034L);
        protoOutputStream.write(1138166333443L, this.mState.toString());
        protoOutputStream.write(1133871366148L, this.visible);
        protoOutputStream.write(1133871366149L, this.frontOfTask);
        if (this.app != null) {
            protoOutputStream.write(1120986464262L, this.app.pid);
        }
        protoOutputStream.write(1133871366151L, !this.fullscreen);
        protoOutputStream.end(jStart);
    }
}
