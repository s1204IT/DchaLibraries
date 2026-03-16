package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityOptions;
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
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.IApplicationToken;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.am.ActivityStack;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class ActivityRecord {
    static final String ACTIVITY_ICON_SUFFIX = "_activity_icon_";
    static final int APPLICATION_ACTIVITY_TYPE = 0;
    private static final String ATTR_COMPONENTSPECIFIED = "component_specified";
    private static final String ATTR_ID = "id";
    static final String ATTR_LAUNCHEDFROMPACKAGE = "launched_from_package";
    private static final String ATTR_LAUNCHEDFROMUID = "launched_from_uid";
    private static final String ATTR_RESOLVEDTYPE = "resolved_type";
    private static final String ATTR_USERID = "user_id";
    static final boolean DEBUG_SAVED_STATE = false;
    static final int HOME_ACTIVITY_TYPE = 1;
    static final int RECENTS_ACTIVITY_TYPE = 2;
    public static final String RECENTS_PACKAGE_NAME = "com.android.systemui.recents";
    static final String TAG = "ActivityManager";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSISTABLEBUNDLE = "persistable_bundle";
    ProcessRecord app;
    final ApplicationInfo appInfo;
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
    int launchedFromUid;
    int logo;
    int mActivityType;
    ActivityStackSupervisor.ActivityContainer mInitialActivityContainer;
    boolean mLaunchTaskBehind;
    final ActivityStackSupervisor mStackSupervisor;
    ArrayList<ReferrerIntent> newIntents;
    final boolean noDisplay;
    CharSequence nonLocalizedLabel;
    final String packageName;
    long pauseTime;
    ActivityOptions pendingOptions;
    HashSet<WeakReference<PendingIntentRecord>> pendingResults;
    PersistableBundle persistentState;
    final String processName;
    final ComponentName realActivity;
    int realTheme;
    final int requestCode;
    final String resolvedType;
    ActivityRecord resultTo;
    final String resultWho;
    ArrayList<ResultInfo> results;
    ActivityOptions returningOptions;
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
    int windowFlags;
    long createTime = System.currentTimeMillis();
    ArrayList<ActivityStackSupervisor.ActivityContainer> mChildContainers = new ArrayList<>();
    boolean mStartingWindowShown = DEBUG_SAVED_STATE;
    final IApplicationToken.Stub appToken = new Token(this);
    ActivityStack.ActivityState state = ActivityStack.ActivityState.INITIALIZING;
    boolean frontOfTask = DEBUG_SAVED_STATE;
    boolean launchFailed = DEBUG_SAVED_STATE;
    boolean stopped = DEBUG_SAVED_STATE;
    boolean delayedResume = DEBUG_SAVED_STATE;
    boolean finishing = DEBUG_SAVED_STATE;
    boolean configDestroy = DEBUG_SAVED_STATE;
    boolean keysPaused = DEBUG_SAVED_STATE;
    private boolean inHistory = DEBUG_SAVED_STATE;
    boolean visible = true;
    boolean waitingVisible = DEBUG_SAVED_STATE;
    boolean nowVisible = DEBUG_SAVED_STATE;
    boolean idle = DEBUG_SAVED_STATE;
    boolean hasBeenLaunched = DEBUG_SAVED_STATE;

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
        }
        pw.print(prefix);
        pw.print("stateNotNeeded=");
        pw.print(this.stateNotNeeded);
        pw.print(" componentSpecified=");
        pw.print(this.componentSpecified);
        pw.print(" mActivityType=");
        pw.println(this.mActivityType);
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
            Iterator<WeakReference<PendingIntentRecord>> it = this.pendingResults.iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> wpir = it.next();
                PendingIntentRecord pir = wpir != null ? wpir.get() : null;
                pw.print(prefix);
                pw.print("  - ");
                if (pir == null) {
                    pw.println("null");
                } else {
                    pw.println(pir);
                    pir.dump(pw, prefix + "    ");
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
                    pw.println(intent.toShortString(DEBUG_SAVED_STATE, true, DEBUG_SAVED_STATE, true));
                }
            }
        }
        if (this.pendingOptions != null) {
            pw.print(prefix);
            pw.print("pendingOptions=");
            pw.println(this.pendingOptions);
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
        pw.println(this.idle);
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
        if (this.lastVisibleTime != 0 || this.waitingVisible || this.nowVisible) {
            pw.print(prefix);
            pw.print("waitingVisible=");
            pw.print(this.waitingVisible);
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
        if (this.configDestroy || this.configChangeFlags != 0) {
            pw.print(prefix);
            pw.print("configDestroy=");
            pw.print(this.configDestroy);
            pw.print(" configChangeFlags=");
            pw.println(Integer.toHexString(this.configChangeFlags));
        }
        if (this.connections != null) {
            pw.print(prefix);
            pw.print("connections=");
            pw.println(this.connections);
        }
    }

    static class Token extends IApplicationToken.Stub {
        final WeakReference<ActivityRecord> weakActivity;

        Token(ActivityRecord activity) {
            this.weakActivity = new WeakReference<>(activity);
        }

        public void windowsDrawn() {
            ActivityRecord activity = this.weakActivity.get();
            if (activity != null) {
                activity.windowsDrawn();
            }
        }

        public void windowsVisible() {
            ActivityRecord activity = this.weakActivity.get();
            if (activity != null) {
                activity.windowsVisible();
            }
        }

        public void windowsGone() {
            ActivityRecord activity = this.weakActivity.get();
            if (activity != null) {
                activity.windowsGone();
            }
        }

        public boolean keyDispatchingTimedOut(String reason) {
            ActivityRecord activity = this.weakActivity.get();
            if (activity == null || !activity.keyDispatchingTimedOut(reason)) {
                return ActivityRecord.DEBUG_SAVED_STATE;
            }
            return true;
        }

        public long getKeyDispatchingTimeout() {
            ActivityRecord activity = this.weakActivity.get();
            if (activity != null) {
                return activity.getKeyDispatchingTimeout();
            }
            return 0L;
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

    static ActivityRecord forToken(IBinder iBinder) {
        ActivityRecord activityRecord;
        if (iBinder != 0) {
            try {
                activityRecord = ((Token) iBinder).weakActivity.get();
            } catch (ClassCastException e) {
                Slog.w(TAG, "Bad activity token: " + iBinder, e);
                return null;
            }
        } else {
            activityRecord = null;
        }
        return activityRecord;
    }

    boolean isNotResolverActivity() {
        if (ResolverActivity.class.getName().equals(this.realActivity.getClassName())) {
            return DEBUG_SAVED_STATE;
        }
        return true;
    }

    ActivityRecord(ActivityManagerService _service, ProcessRecord _caller, int _launchedFromUid, String _launchedFromPackage, Intent _intent, String _resolvedType, ActivityInfo aInfo, Configuration _configuration, ActivityRecord _resultTo, String _resultWho, int _reqCode, boolean _componentSpecified, ActivityStackSupervisor supervisor, ActivityStackSupervisor.ActivityContainer container, Bundle options) {
        this.service = _service;
        this.info = aInfo;
        this.launchedFromUid = _launchedFromUid;
        this.launchedFromPackage = _launchedFromPackage;
        this.userId = UserHandle.getUserId(aInfo.applicationInfo.uid);
        this.intent = _intent;
        this.shortComponentName = _intent.getComponent().flattenToShortString();
        this.resolvedType = _resolvedType;
        this.componentSpecified = _componentSpecified;
        this.configuration = _configuration;
        this.resultTo = _resultTo;
        this.resultWho = _resultWho;
        this.requestCode = _reqCode;
        this.mStackSupervisor = supervisor;
        this.mInitialActivityContainer = container;
        if (options != null) {
            this.pendingOptions = new ActivityOptions(options);
            this.mLaunchTaskBehind = this.pendingOptions.getLaunchTaskBehind();
        }
        this.haveState = true;
        if (aInfo != null) {
            if (aInfo.targetActivity == null || aInfo.launchMode == 0 || aInfo.launchMode == 1) {
                this.realActivity = _intent.getComponent();
            } else {
                this.realActivity = new ComponentName(aInfo.packageName, aInfo.targetActivity);
            }
            this.taskAffinity = aInfo.taskAffinity;
            this.stateNotNeeded = (aInfo.flags & 16) != 0 ? true : DEBUG_SAVED_STATE;
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
            if ((aInfo.flags & 1) != 0 && _caller != null && (aInfo.applicationInfo.uid == 1000 || aInfo.applicationInfo.uid == _caller.info.uid)) {
                this.processName = _caller.processName;
            } else {
                this.processName = aInfo.processName;
            }
            if (this.intent != null && (aInfo.flags & 32) != 0) {
                this.intent.addFlags(8388608);
            }
            this.packageName = aInfo.applicationInfo.packageName;
            this.launchMode = aInfo.launchMode;
            AttributeCache.Entry ent = AttributeCache.instance().get(this.packageName, this.realTheme, com.android.internal.R.styleable.Window, this.userId);
            this.fullscreen = (ent == null || ent.array.getBoolean(4, DEBUG_SAVED_STATE) || ent.array.getBoolean(5, DEBUG_SAVED_STATE)) ? DEBUG_SAVED_STATE : true;
            this.noDisplay = (ent == null || !ent.array.getBoolean(10, DEBUG_SAVED_STATE)) ? DEBUG_SAVED_STATE : true;
            if ((!_componentSpecified || _launchedFromUid == Process.myUid() || _launchedFromUid == 0) && "android.intent.action.MAIN".equals(_intent.getAction()) && _intent.hasCategory("android.intent.category.HOME") && _intent.getCategories().size() == 1 && _intent.getData() == null && _intent.getType() == null && (this.intent.getFlags() & 268435456) != 0 && isNotResolverActivity()) {
                this.mActivityType = 1;
            } else if (this.realActivity.getClassName().contains(RECENTS_PACKAGE_NAME)) {
                this.mActivityType = 2;
            } else {
                this.mActivityType = 0;
            }
            this.immersive = (aInfo.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0 ? true : DEBUG_SAVED_STATE;
            return;
        }
        this.realActivity = null;
        this.taskAffinity = null;
        this.stateNotNeeded = DEBUG_SAVED_STATE;
        this.appInfo = null;
        this.processName = null;
        this.packageName = null;
        this.fullscreen = true;
        this.noDisplay = DEBUG_SAVED_STATE;
        this.mActivityType = 0;
        this.immersive = DEBUG_SAVED_STATE;
    }

    void setTask(TaskRecord newTask, TaskRecord taskToAffiliateWith) {
        if (this.task != null && this.task.removeActivity(this)) {
            if (this.task != newTask) {
                this.task.stack.removeTask(this.task, "setTask");
            } else {
                Slog.d(TAG, "!!! REMOVE THIS LOG !!! setTask: nearly removed stack=" + (newTask == null ? null : newTask.stack));
            }
        }
        this.task = newTask;
        setTaskToAffiliateWith(taskToAffiliateWith);
    }

    void setTaskToAffiliateWith(TaskRecord taskToAffiliateWith) {
        if (taskToAffiliateWith != null && this.launchMode != 3 && this.launchMode != 2) {
            this.task.setTaskToAffiliateWith(taskToAffiliateWith);
        }
    }

    boolean changeWindowTranslucency(boolean toOpaque) {
        if (this.fullscreen == toOpaque) {
            return DEBUG_SAVED_STATE;
        }
        TaskRecord taskRecord = this.task;
        taskRecord.numFullscreen = (toOpaque ? 1 : -1) + taskRecord.numFullscreen;
        this.fullscreen = toOpaque;
        return true;
    }

    void putInHistory() {
        if (!this.inHistory) {
            this.inHistory = true;
        }
    }

    void takeFromHistory() {
        if (this.inHistory) {
            this.inHistory = DEBUG_SAVED_STATE;
            if (this.task != null && !this.finishing) {
                this.task = null;
            }
            clearOptionsLocked();
        }
    }

    boolean isInHistory() {
        return this.inHistory;
    }

    boolean isHomeActivity() {
        if (this.mActivityType == 1) {
            return true;
        }
        return DEBUG_SAVED_STATE;
    }

    boolean isRecentsActivity() {
        if (this.mActivityType == 2) {
            return true;
        }
        return DEBUG_SAVED_STATE;
    }

    boolean isApplicationActivity() {
        if (this.mActivityType == 0) {
            return true;
        }
        return DEBUG_SAVED_STATE;
    }

    boolean isPersistable() {
        if ((this.info.persistableMode == 0 || this.info.persistableMode == 2) && (this.intent == null || (this.intent.getFlags() & 8388608) == 0)) {
            return true;
        }
        return DEBUG_SAVED_STATE;
    }

    void makeFinishing() {
        if (!this.finishing) {
            if (this == this.task.stack.getVisibleBehindActivity()) {
                this.mStackSupervisor.requestVisibleBehindLocked(this, DEBUG_SAVED_STATE);
            }
            this.finishing = true;
            if (this.stopped) {
                clearOptionsLocked();
            }
        }
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
        if (this.results != null) {
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
        if ((this.state == ActivityStack.ActivityState.RESUMED || (this.service.isSleeping() && this.task.stack.topRunningActivityLocked(null) == this)) && this.app != null && this.app.thread != null) {
            try {
                ArrayList<ReferrerIntent> ar = new ArrayList<>(1);
                ar.add(rintent);
                this.app.thread.scheduleNewIntent(ar, this.appToken);
                unsent = DEBUG_SAVED_STATE;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e);
            } catch (NullPointerException e2) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e2);
            }
        }
        if (unsent) {
            addNewIntentLocked(rintent);
        }
    }

    void updateOptionsLocked(Bundle options) {
        if (options != null) {
            if (this.pendingOptions != null) {
                this.pendingOptions.abort();
            }
            this.pendingOptions = new ActivityOptions(options);
        }
    }

    void updateOptionsLocked(ActivityOptions options) {
        if (options != null) {
            if (this.pendingOptions != null) {
                this.pendingOptions.abort();
            }
            this.pendingOptions = options;
        }
    }

    void applyOptionsLocked() {
        if (this.pendingOptions != null && this.pendingOptions.getAnimationType() != 5) {
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
                default:
                    Slog.e(TAG, "applyOptionsLocked: Unknown animationType=" + animationType);
                    break;
                case 8:
                case 9:
                    this.service.mWindowManager.overridePendingAppTransitionAspectScaledThumb(this.pendingOptions.getThumbnail(), this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight(), this.pendingOptions.getOnAnimationStartListener(), animationType == 8);
                    if (this.intent.getSourceBounds() == null) {
                        this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                    }
                    break;
            }
            this.pendingOptions = null;
        }
    }

    ActivityOptions getOptionsForTargetActivityLocked() {
        if (this.pendingOptions != null) {
            return this.pendingOptions.forTargetActivity();
        }
        return null;
    }

    void clearOptionsLocked() {
        if (this.pendingOptions != null) {
            this.pendingOptions.abort();
            this.pendingOptions = null;
        }
    }

    ActivityOptions takeOptionsLocked() {
        ActivityOptions opts = this.pendingOptions;
        this.pendingOptions = null;
        return opts;
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
            this.service.mWindowManager.pauseKeyDispatching(this.appToken);
        }
    }

    void resumeKeyDispatchingLocked() {
        if (this.keysPaused) {
            this.keysPaused = DEBUG_SAVED_STATE;
            this.service.mWindowManager.resumeKeyDispatching(this.appToken);
        }
    }

    void updateThumbnailLocked(Bitmap newThumbnail, CharSequence description) {
        if (newThumbnail != null) {
            boolean thumbnailUpdated = this.task.setLastThumbnail(newThumbnail);
            if (thumbnailUpdated && isPersistable()) {
                this.mStackSupervisor.mService.notifyTaskPersisterLocked(this.task, DEBUG_SAVED_STATE);
            }
        }
        this.task.lastDescription = description;
    }

    void startLaunchTickingLocked() {
        if (!ActivityManagerService.IS_USER_BUILD && this.launchTickTime == 0) {
            this.launchTickTime = SystemClock.uptimeMillis();
            continueLaunchTickingLocked();
        }
    }

    boolean continueLaunchTickingLocked() {
        if (this.launchTickTime == 0) {
            return DEBUG_SAVED_STATE;
        }
        ActivityStack stack = this.task.stack;
        Message msg = stack.mHandler.obtainMessage(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION, this);
        stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION);
        stack.mHandler.sendMessageDelayed(msg, 500L);
        return true;
    }

    void finishLaunchTickingLocked() {
        this.launchTickTime = 0L;
        this.task.stack.mHandler.removeMessages(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION);
    }

    public boolean mayFreezeScreenLocked(ProcessRecord app) {
        if (app == null || app.crashing || app.notResponding) {
            return DEBUG_SAVED_STATE;
        }
        return true;
    }

    public void startFreezingScreenLocked(ProcessRecord app, int configChanges) {
        if (mayFreezeScreenLocked(app)) {
            this.service.mWindowManager.startAppFreezingScreen(this.appToken, configChanges);
        }
    }

    public void stopFreezingScreenLocked(boolean force) {
        if (force || this.frozenBeforeDestroy) {
            this.frozenBeforeDestroy = DEBUG_SAVED_STATE;
            this.service.mWindowManager.stopAppFreezingScreen(this.appToken, force);
        }
    }

    public void reportFullyDrawnLocked() {
        long curTime = SystemClock.uptimeMillis();
        if (this.displayStartTime != 0) {
            reportLaunchTimeLocked(curTime);
        }
        if (this.fullyDrawnStartTime != 0) {
            ActivityStack stack = this.task.stack;
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
            this.fullyDrawnStartTime = 0L;
            stack.mFullyDrawnStartTime = 0L;
        }
    }

    private void reportLaunchTimeLocked(long curTime) {
        ActivityStack stack = this.task.stack;
        long thisTime = curTime - this.displayStartTime;
        long totalTime = stack.mLaunchStartTime != 0 ? curTime - stack.mLaunchStartTime : thisTime;
        Trace.asyncTraceEnd(64L, "launching", 0);
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
        this.mStackSupervisor.reportActivityLaunchedLocked(DEBUG_SAVED_STATE, this, thisTime, totalTime);
        if (totalTime > 0) {
        }
        this.displayStartTime = 0L;
        stack.mLaunchStartTime = 0L;
    }

    public void windowsDrawn() {
        synchronized (this.service) {
            if (this.displayStartTime != 0) {
                reportLaunchTimeLocked(SystemClock.uptimeMillis());
            }
            this.mStackSupervisor.sendWaitingVisibleReportLocked(this);
            this.startTime = 0L;
            finishLaunchTickingLocked();
            if (this.task != null) {
                this.task.hasBeenVisible = true;
            }
        }
    }

    public void windowsVisible() {
        synchronized (this.service) {
            this.mStackSupervisor.reportActivityVisibleLocked(this);
            if (!this.nowVisible) {
                this.nowVisible = true;
                this.lastVisibleTime = SystemClock.uptimeMillis();
                if (!this.idle) {
                    this.mStackSupervisor.processStoppingActivitiesLocked(DEBUG_SAVED_STATE);
                } else {
                    int N = this.mStackSupervisor.mWaitingVisibleActivities.size();
                    if (N > 0) {
                        for (int i = 0; i < N; i++) {
                            ActivityRecord r = this.mStackSupervisor.mWaitingVisibleActivities.get(i);
                            r.waitingVisible = DEBUG_SAVED_STATE;
                        }
                        this.mStackSupervisor.mWaitingVisibleActivities.clear();
                        this.mStackSupervisor.scheduleIdleLocked();
                    }
                }
                this.service.scheduleAppGcsLocked();
            }
        }
    }

    public void windowsGone() {
        this.nowVisible = DEBUG_SAVED_STATE;
    }

    private ActivityRecord getWaitingHistoryRecordLocked() {
        if (!this.waitingVisible) {
            return this;
        }
        ActivityStack stack = this.mStackSupervisor.getFocusedStack();
        ActivityRecord r = stack.mResumedActivity;
        if (r == null) {
            r = stack.mPausingActivity;
        }
        if (r == null) {
            return this;
        }
        return r;
    }

    public boolean keyDispatchingTimedOut(String reason) {
        ActivityRecord r;
        ProcessRecord anrApp;
        synchronized (this.service) {
            r = getWaitingHistoryRecordLocked();
            anrApp = r != null ? r.app : null;
        }
        return this.service.inputDispatchingTimedOut(anrApp, r, this, DEBUG_SAVED_STATE, reason);
    }

    public long getKeyDispatchingTimeout() {
        long inputDispatchingTimeoutLocked;
        synchronized (this.service) {
            ActivityRecord r = getWaitingHistoryRecordLocked();
            inputDispatchingTimeoutLocked = ActivityManagerService.getInputDispatchingTimeoutLocked(r);
        }
        return inputDispatchingTimeoutLocked;
    }

    public boolean isInterestingToUserLocked() {
        if (this.visible || this.nowVisible || this.state == ActivityStack.ActivityState.PAUSING || this.state == ActivityStack.ActivityState.RESUMED) {
            return true;
        }
        return DEBUG_SAVED_STATE;
    }

    public void setSleeping(boolean _sleeping) {
        if (this.sleeping != _sleeping && this.app != null && this.app.thread != null) {
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
    }

    static void activityResumedLocked(IBinder token) {
        ActivityRecord r = forToken(token);
        r.icicle = null;
        r.haveState = DEBUG_SAVED_STATE;
    }

    static int getTaskForActivityLocked(IBinder token, boolean onlyRoot) {
        ActivityRecord r = forToken(token);
        if (r == null) {
            return -1;
        }
        TaskRecord task = r.task;
        int activityNdx = task.mActivities.indexOf(r);
        if (activityNdx < 0) {
            return -1;
        }
        if (!onlyRoot || activityNdx <= task.findEffectiveRootIndex()) {
            return task.taskId;
        }
        return -1;
    }

    static ActivityRecord isInStackLocked(IBinder token) {
        ActivityRecord r = forToken(token);
        if (r != null) {
            return r.task.stack.isInStackLocked(token);
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
        if (this.finishing || this.app == null || this.state == ActivityStack.ActivityState.DESTROYING || this.state == ActivityStack.ActivityState.DESTROYED || this.task == null || this.task.stack == null || this == this.task.stack.mResumedActivity || this == this.task.stack.mPausingActivity || !this.haveState || !this.stopped || this.visible) {
            return DEBUG_SAVED_STATE;
        }
        return true;
    }

    private static String createImageFilename(long createTime, int taskId) {
        return String.valueOf(taskId) + ACTIVITY_ICON_SUFFIX + createTime + ".png";
    }

    void setTaskDescription(ActivityManager.TaskDescription _taskDescription) {
        Bitmap icon;
        if (_taskDescription.getIconFilename() == null && (icon = _taskDescription.getIcon()) != null) {
            String iconFilename = createImageFilename(this.createTime, this.task.taskId);
            this.mStackSupervisor.mService.mTaskPersister.saveImage(icon, iconFilename);
            _taskDescription.setIconFilename(iconFilename);
        }
        this.taskDescription = _taskDescription;
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
        if (isPersistable() && this.persistentState != null) {
            out.startTag(null, TAG_PERSISTABLEBUNDLE);
            this.persistentState.saveToXml(out);
            out.endTag(null, TAG_PERSISTABLEBUNDLE);
        }
    }

    static ActivityRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor) throws XmlPullParserException, IOException {
        Intent intent = null;
        PersistableBundle persistentState = null;
        int launchedFromUid = 0;
        String launchedFromPackage = null;
        String resolvedType = null;
        boolean componentSpecified = DEBUG_SAVED_STATE;
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
                launchedFromUid = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_LAUNCHEDFROMPACKAGE.equals(attrName)) {
                launchedFromPackage = attrValue;
            } else if (ATTR_RESOLVEDTYPE.equals(attrName)) {
                resolvedType = attrValue;
            } else if (ATTR_COMPONENTSPECIFIED.equals(attrName)) {
                componentSpecified = Boolean.valueOf(attrValue).booleanValue();
            } else if (ATTR_USERID.equals(attrName)) {
                userId = Integer.valueOf(attrValue).intValue();
            } else if (attrName.startsWith("task_description_")) {
                taskDescription.restoreFromXml(attrName, attrValue);
            } else {
                Log.d(TAG, "Unknown ActivityRecord attribute=" + attrName);
            }
        }
        while (true) {
            int event = in.next();
            if (event == 1 || (event == 3 && in.getDepth() >= outerDepth)) {
                break;
            }
            if (event == 2) {
                String name = in.getName();
                if (TAG_INTENT.equals(name)) {
                    intent = Intent.restoreFromXml(in);
                } else if (TAG_PERSISTABLEBUNDLE.equals(name)) {
                    persistentState = PersistableBundle.restoreFromXml(in);
                } else {
                    Slog.w(TAG, "restoreActivity: unexpected name=" + name);
                    XmlUtils.skipCurrentTag(in);
                }
            }
        }
        if (intent == null) {
            throw new XmlPullParserException("restoreActivity error intent=" + intent);
        }
        ActivityManagerService service = stackSupervisor.mService;
        ActivityInfo aInfo = stackSupervisor.resolveActivity(intent, resolvedType, 0, null, userId);
        if (aInfo == null) {
            throw new XmlPullParserException("restoreActivity resolver error. Intent=" + intent + " resolvedType=" + resolvedType);
        }
        ActivityRecord r = new ActivityRecord(service, null, launchedFromUid, launchedFromPackage, intent, resolvedType, aInfo, service.getConfiguration(), null, null, 0, componentSpecified, stackSupervisor, null, null);
        r.persistentState = persistentState;
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
