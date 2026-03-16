package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class TaskRecord {
    private static final String ATTR_AFFINITY = "affinity";
    private static final String ATTR_ASKEDCOMPATMODE = "asked_compat_mode";
    private static final String ATTR_AUTOREMOVERECENTS = "auto_remove_recents";
    private static final String ATTR_CALLING_PACKAGE = "calling_package";
    private static final String ATTR_CALLING_UID = "calling_uid";
    private static final String ATTR_EFFECTIVE_UID = "effective_uid";
    private static final String ATTR_FIRSTACTIVETIME = "first_active_time";
    private static final String ATTR_LASTACTIVETIME = "last_active_time";
    private static final String ATTR_LASTDESCRIPTION = "last_description";
    private static final String ATTR_LASTTIMEMOVED = "last_time_moved";
    private static final String ATTR_NEVERRELINQUISH = "never_relinquish_identity";
    private static final String ATTR_NEXT_AFFILIATION = "next_affiliation";
    private static final String ATTR_ORIGACTIVITY = "orig_activity";
    private static final String ATTR_PREV_AFFILIATION = "prev_affiliation";
    static final String ATTR_REALACTIVITY = "real_activity";
    private static final String ATTR_ROOTHASRESET = "root_has_reset";
    private static final String ATTR_ROOT_AFFINITY = "root_affinity";
    static final String ATTR_TASKID = "task_id";
    private static final String ATTR_TASKTYPE = "task_type";
    static final String ATTR_TASK_AFFILIATION = "task_affiliation";
    private static final String ATTR_TASK_AFFILIATION_COLOR = "task_affiliation_color";
    private static final String ATTR_USERID = "user_id";
    static final boolean IGNORE_RETURN_TO_RECENTS = true;
    static final int INVALID_TASK_ID = -1;
    static final String TAG_ACTIVITY = "activity";
    private static final String TAG_AFFINITYINTENT = "affinity_intent";
    private static final String TAG_INTENT = "intent";
    private static final String TASK_THUMBNAIL_SUFFIX = "_task_thumbnail";
    String affinity;
    Intent affinityIntent;
    boolean askedCompatMode;
    boolean autoRemoveRecents;
    int creatorUid;
    int effectiveUid;
    long firstActiveTime;
    boolean hasBeenVisible;
    boolean inRecents;
    Intent intent;
    boolean isAvailable;
    boolean isPersistable;
    long lastActiveTime;
    CharSequence lastDescription;
    ActivityManager.TaskDescription lastTaskDescription;
    final ArrayList<ActivityRecord> mActivities;
    int mAffiliatedTaskColor;
    int mAffiliatedTaskId;
    String mCallingPackage;
    int mCallingUid;
    private final String mFilename;
    private Bitmap mLastThumbnail;
    private final File mLastThumbnailFile;
    long mLastTimeMoved;
    boolean mNeverRelinquishIdentity;
    TaskRecord mNextAffiliate;
    int mNextAffiliateTaskId;
    TaskRecord mPrevAffiliate;
    int mPrevAffiliateTaskId;
    boolean mReuseTask;
    final ActivityManagerService mService;
    private int mTaskToReturnTo;
    int maxRecents;
    int numFullscreen;
    ComponentName origActivity;
    ComponentName realActivity;
    String rootAffinity;
    boolean rootWasReset;
    ActivityStack stack;
    String stringName;
    final int taskId;
    int taskType;
    int userId;
    final IVoiceInteractor voiceInteractor;
    final IVoiceInteractionSession voiceSession;

    TaskRecord(ActivityManagerService service, int _taskId, ActivityInfo info, Intent _intent, IVoiceInteractionSession _voiceSession, IVoiceInteractor _voiceInteractor) {
        this.lastTaskDescription = new ActivityManager.TaskDescription();
        this.isPersistable = false;
        this.mLastTimeMoved = System.currentTimeMillis();
        this.mTaskToReturnTo = 0;
        this.mNeverRelinquishIdentity = IGNORE_RETURN_TO_RECENTS;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mService = service;
        this.mFilename = String.valueOf(_taskId) + TASK_THUMBNAIL_SUFFIX + ".png";
        this.mLastThumbnailFile = new File(TaskPersister.sImagesDir, this.mFilename);
        this.taskId = _taskId;
        this.mAffiliatedTaskId = _taskId;
        this.voiceSession = _voiceSession;
        this.voiceInteractor = _voiceInteractor;
        this.isAvailable = IGNORE_RETURN_TO_RECENTS;
        this.mActivities = new ArrayList<>();
        setIntent(_intent, info);
    }

    TaskRecord(ActivityManagerService service, int _taskId, ActivityInfo info, Intent _intent, ActivityManager.TaskDescription _taskDescription) {
        this.lastTaskDescription = new ActivityManager.TaskDescription();
        this.isPersistable = false;
        this.mLastTimeMoved = System.currentTimeMillis();
        this.mTaskToReturnTo = 0;
        this.mNeverRelinquishIdentity = IGNORE_RETURN_TO_RECENTS;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mService = service;
        this.mFilename = String.valueOf(_taskId) + TASK_THUMBNAIL_SUFFIX + ".png";
        this.mLastThumbnailFile = new File(TaskPersister.sImagesDir, this.mFilename);
        this.taskId = _taskId;
        this.mAffiliatedTaskId = _taskId;
        this.voiceSession = null;
        this.voiceInteractor = null;
        this.isAvailable = IGNORE_RETURN_TO_RECENTS;
        this.mActivities = new ArrayList<>();
        setIntent(_intent, info);
        this.taskType = 0;
        this.isPersistable = IGNORE_RETURN_TO_RECENTS;
        this.mCallingUid = info.applicationInfo.uid;
        this.mCallingPackage = info.packageName;
        this.maxRecents = Math.min(Math.max(info.maxRecents, 1), ActivityManager.getMaxAppRecentsLimitStatic());
        this.taskType = 0;
        this.mTaskToReturnTo = 1;
        this.userId = UserHandle.getUserId(info.applicationInfo.uid);
        this.lastTaskDescription = _taskDescription;
        this.mCallingUid = info.applicationInfo.uid;
        this.mCallingPackage = info.packageName;
    }

    TaskRecord(ActivityManagerService service, int _taskId, Intent _intent, Intent _affinityIntent, String _affinity, String _rootAffinity, ComponentName _realActivity, ComponentName _origActivity, boolean _rootWasReset, boolean _autoRemoveRecents, boolean _askedCompatMode, int _taskType, int _userId, int _effectiveUid, String _lastDescription, ArrayList<ActivityRecord> activities, long _firstActiveTime, long _lastActiveTime, long lastTimeMoved, boolean neverRelinquishIdentity, ActivityManager.TaskDescription _lastTaskDescription, int taskAffiliation, int prevTaskId, int nextTaskId, int taskAffiliationColor, int callingUid, String callingPackage) {
        this.lastTaskDescription = new ActivityManager.TaskDescription();
        this.isPersistable = false;
        this.mLastTimeMoved = System.currentTimeMillis();
        this.mTaskToReturnTo = 0;
        this.mNeverRelinquishIdentity = IGNORE_RETURN_TO_RECENTS;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mService = service;
        this.mFilename = String.valueOf(_taskId) + TASK_THUMBNAIL_SUFFIX + ".png";
        this.mLastThumbnailFile = new File(TaskPersister.sImagesDir, this.mFilename);
        this.taskId = _taskId;
        this.intent = _intent;
        this.affinityIntent = _affinityIntent;
        this.affinity = _affinity;
        this.rootAffinity = _affinity;
        this.voiceSession = null;
        this.voiceInteractor = null;
        this.realActivity = _realActivity;
        this.origActivity = _origActivity;
        this.rootWasReset = _rootWasReset;
        this.isAvailable = IGNORE_RETURN_TO_RECENTS;
        this.autoRemoveRecents = _autoRemoveRecents;
        this.askedCompatMode = _askedCompatMode;
        this.taskType = _taskType;
        this.mTaskToReturnTo = 1;
        this.userId = _userId;
        this.effectiveUid = _effectiveUid;
        this.firstActiveTime = _firstActiveTime;
        this.lastActiveTime = _lastActiveTime;
        this.lastDescription = _lastDescription;
        this.mActivities = activities;
        this.mLastTimeMoved = lastTimeMoved;
        this.mNeverRelinquishIdentity = neverRelinquishIdentity;
        this.lastTaskDescription = _lastTaskDescription;
        this.mAffiliatedTaskId = taskAffiliation;
        this.mAffiliatedTaskColor = taskAffiliationColor;
        this.mPrevAffiliateTaskId = prevTaskId;
        this.mNextAffiliateTaskId = nextTaskId;
        this.mCallingUid = callingUid;
        this.mCallingPackage = callingPackage;
    }

    void touchActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
        if (this.firstActiveTime == 0) {
            this.firstActiveTime = this.lastActiveTime;
        }
    }

    long getInactiveDuration() {
        return System.currentTimeMillis() - this.lastActiveTime;
    }

    void setIntent(ActivityRecord r) {
        setIntent(r.intent, r.info);
        this.mCallingUid = r.launchedFromUid;
        this.mCallingPackage = r.launchedFromPackage;
    }

    private void setIntent(Intent _intent, ActivityInfo info) {
        if (this.intent == null) {
            this.mNeverRelinquishIdentity = (info.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0;
        } else if (this.mNeverRelinquishIdentity) {
            return;
        }
        this.affinity = info.taskAffinity;
        if (this.intent == null) {
            this.rootAffinity = this.affinity;
        }
        this.effectiveUid = info.applicationInfo.uid;
        this.stringName = null;
        if (info.targetActivity == null) {
            if (_intent != null && (_intent.getSelector() != null || _intent.getSourceBounds() != null)) {
                Intent _intent2 = new Intent(_intent);
                _intent2.setSelector(null);
                _intent2.setSourceBounds(null);
                _intent = _intent2;
            }
            this.intent = _intent;
            this.realActivity = _intent != null ? _intent.getComponent() : null;
            this.origActivity = null;
        } else {
            ComponentName targetComponent = new ComponentName(info.packageName, info.targetActivity);
            if (_intent != null) {
                Intent targetIntent = new Intent(_intent);
                targetIntent.setComponent(targetComponent);
                targetIntent.setSelector(null);
                targetIntent.setSourceBounds(null);
                this.intent = targetIntent;
                this.realActivity = targetComponent;
                this.origActivity = _intent.getComponent();
            } else {
                this.intent = null;
                this.realActivity = targetComponent;
                this.origActivity = new ComponentName(info.packageName, info.name);
            }
        }
        int intentFlags = this.intent == null ? 0 : this.intent.getFlags();
        if ((2097152 & intentFlags) != 0) {
            this.rootWasReset = IGNORE_RETURN_TO_RECENTS;
        }
        this.userId = UserHandle.getUserId(info.applicationInfo.uid);
        if ((info.flags & PackageManagerService.DumpState.DUMP_INSTALLS) != 0) {
            this.autoRemoveRecents = IGNORE_RETURN_TO_RECENTS;
            return;
        }
        if ((532480 & intentFlags) == 524288) {
            if (info.documentLaunchMode != 0) {
                this.autoRemoveRecents = false;
                return;
            } else {
                this.autoRemoveRecents = IGNORE_RETURN_TO_RECENTS;
                return;
            }
        }
        this.autoRemoveRecents = false;
    }

    void setTaskToReturnTo(int taskToReturnTo) {
        if (taskToReturnTo == 2) {
            taskToReturnTo = 1;
        }
        this.mTaskToReturnTo = taskToReturnTo;
    }

    int getTaskToReturnTo() {
        return this.mTaskToReturnTo;
    }

    void setPrevAffiliate(TaskRecord prevAffiliate) {
        this.mPrevAffiliate = prevAffiliate;
        this.mPrevAffiliateTaskId = prevAffiliate == null ? -1 : prevAffiliate.taskId;
    }

    void setNextAffiliate(TaskRecord nextAffiliate) {
        this.mNextAffiliate = nextAffiliate;
        this.mNextAffiliateTaskId = nextAffiliate == null ? -1 : nextAffiliate.taskId;
    }

    void closeRecentsChain() {
        if (this.mPrevAffiliate != null) {
            this.mPrevAffiliate.setNextAffiliate(this.mNextAffiliate);
        }
        if (this.mNextAffiliate != null) {
            this.mNextAffiliate.setPrevAffiliate(this.mPrevAffiliate);
        }
        setPrevAffiliate(null);
        setNextAffiliate(null);
    }

    void removedFromRecents() {
        disposeThumbnail();
        closeRecentsChain();
        if (this.inRecents) {
            this.inRecents = false;
            this.mService.notifyTaskPersisterLocked(this, false);
        }
    }

    void setTaskToAffiliateWith(TaskRecord taskToAffiliateWith) {
        closeRecentsChain();
        this.mAffiliatedTaskId = taskToAffiliateWith.mAffiliatedTaskId;
        this.mAffiliatedTaskColor = taskToAffiliateWith.mAffiliatedTaskColor;
        while (true) {
            if (taskToAffiliateWith.mNextAffiliate == null) {
                break;
            }
            TaskRecord nextRecents = taskToAffiliateWith.mNextAffiliate;
            if (nextRecents.mAffiliatedTaskId != this.mAffiliatedTaskId) {
                Slog.e("ActivityManager", "setTaskToAffiliateWith: nextRecents=" + nextRecents + " affilTaskId=" + nextRecents.mAffiliatedTaskId + " should be " + this.mAffiliatedTaskId);
                if (nextRecents.mPrevAffiliate == taskToAffiliateWith) {
                    nextRecents.setPrevAffiliate(null);
                }
                taskToAffiliateWith.setNextAffiliate(null);
            } else {
                taskToAffiliateWith = nextRecents;
            }
        }
        taskToAffiliateWith.setNextAffiliate(this);
        setPrevAffiliate(taskToAffiliateWith);
        setNextAffiliate(null);
    }

    boolean setLastThumbnail(Bitmap thumbnail) {
        if (this.mLastThumbnail != thumbnail) {
            this.mLastThumbnail = thumbnail;
            if (thumbnail == null) {
                if (this.mLastThumbnailFile != null) {
                    this.mLastThumbnailFile.delete();
                }
            } else {
                this.mService.mTaskPersister.saveImage(thumbnail, this.mFilename);
            }
            return IGNORE_RETURN_TO_RECENTS;
        }
        return false;
    }

    void getLastThumbnail(ActivityManager.TaskThumbnail thumbs) {
        thumbs.mainThumbnail = this.mLastThumbnail;
        thumbs.thumbnailFileDescriptor = null;
        if (this.mLastThumbnail == null) {
            thumbs.mainThumbnail = this.mService.mTaskPersister.getImageFromWriteQueue(this.mFilename);
        }
        if (thumbs.mainThumbnail == null && this.mLastThumbnailFile.exists()) {
            try {
                thumbs.thumbnailFileDescriptor = ParcelFileDescriptor.open(this.mLastThumbnailFile, 268435456);
            } catch (IOException e) {
            }
        }
    }

    void freeLastThumbnail() {
        this.mLastThumbnail = null;
    }

    void disposeThumbnail() {
        this.mLastThumbnail = null;
        this.lastDescription = null;
    }

    Intent getBaseIntent() {
        return this.intent != null ? this.intent : this.affinityIntent;
    }

    ActivityRecord getRootActivity() {
        for (int i = 0; i < this.mActivities.size(); i++) {
            ActivityRecord r = this.mActivities.get(i);
            if (!r.finishing) {
                return r;
            }
        }
        return null;
    }

    ActivityRecord getTopActivity() {
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mActivities.get(i);
            if (!r.finishing) {
                return r;
            }
        }
        return null;
    }

    ActivityRecord topRunningActivityLocked(ActivityRecord notTop) {
        for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord r = this.mActivities.get(activityNdx);
            if (!r.finishing && r != notTop && this.stack.okToShowLocked(r)) {
                return r;
            }
        }
        return null;
    }

    final void setFrontOfTask() {
        boolean foundFront = false;
        int numActivities = this.mActivities.size();
        for (int activityNdx = 0; activityNdx < numActivities; activityNdx++) {
            ActivityRecord r = this.mActivities.get(activityNdx);
            if (foundFront || r.finishing) {
                r.frontOfTask = false;
            } else {
                r.frontOfTask = IGNORE_RETURN_TO_RECENTS;
                foundFront = IGNORE_RETURN_TO_RECENTS;
            }
        }
        if (!foundFront && numActivities > 0) {
            this.mActivities.get(0).frontOfTask = IGNORE_RETURN_TO_RECENTS;
        }
    }

    final void moveActivityToFrontLocked(ActivityRecord newTop) {
        this.mActivities.remove(newTop);
        this.mActivities.add(newTop);
        updateEffectiveIntent();
        setFrontOfTask();
    }

    void addActivityAtBottom(ActivityRecord r) {
        addActivityAtIndex(0, r);
    }

    void addActivityToTop(ActivityRecord r) {
        addActivityAtIndex(this.mActivities.size(), r);
    }

    void addActivityAtIndex(int index, ActivityRecord r) {
        if (!this.mActivities.remove(r) && r.fullscreen) {
            this.numFullscreen++;
        }
        if (this.mActivities.isEmpty()) {
            this.taskType = r.mActivityType;
            this.isPersistable = r.isPersistable();
            this.mCallingUid = r.launchedFromUid;
            this.mCallingPackage = r.launchedFromPackage;
            this.maxRecents = Math.min(Math.max(r.info.maxRecents, 1), ActivityManager.getMaxAppRecentsLimitStatic());
        } else {
            r.mActivityType = this.taskType;
        }
        this.mActivities.add(index, r);
        updateEffectiveIntent();
        if (r.isPersistable()) {
            this.mService.notifyTaskPersisterLocked(this, false);
        }
    }

    boolean removeActivity(ActivityRecord r) {
        if (this.mActivities.remove(r) && r.fullscreen) {
            this.numFullscreen--;
        }
        if (r.isPersistable()) {
            this.mService.notifyTaskPersisterLocked(this, false);
        }
        if (this.mActivities.isEmpty()) {
            if (this.mReuseTask) {
                return false;
            }
            return IGNORE_RETURN_TO_RECENTS;
        }
        updateEffectiveIntent();
        return false;
    }

    boolean autoRemoveFromRecents() {
        if (this.autoRemoveRecents || (this.mActivities.isEmpty() && !this.hasBeenVisible)) {
            return IGNORE_RETURN_TO_RECENTS;
        }
        return false;
    }

    final void performClearTaskAtIndexLocked(int activityNdx) {
        int numActivities = this.mActivities.size();
        while (activityNdx < numActivities) {
            ActivityRecord r = this.mActivities.get(activityNdx);
            if (!r.finishing) {
                if (this.stack == null) {
                    r.takeFromHistory();
                    this.mActivities.remove(activityNdx);
                    activityNdx--;
                    numActivities--;
                } else if (this.stack.finishActivityLocked(r, 0, null, "clear", false)) {
                    activityNdx--;
                    numActivities--;
                }
            }
            activityNdx++;
        }
    }

    final void performClearTaskLocked() {
        this.mReuseTask = IGNORE_RETURN_TO_RECENTS;
        performClearTaskAtIndexLocked(0);
        this.mReuseTask = false;
    }

    final ActivityRecord performClearTaskLocked(ActivityRecord newR, int launchFlags) {
        int numActivities = this.mActivities.size();
        for (int activityNdx = numActivities - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord r = this.mActivities.get(activityNdx);
            if (!r.finishing && r.realActivity.equals(newR.realActivity)) {
                int activityNdx2 = activityNdx + 1;
                while (activityNdx2 < numActivities) {
                    ActivityRecord r2 = this.mActivities.get(activityNdx2);
                    if (!r2.finishing) {
                        ActivityOptions opts = r2.takeOptionsLocked();
                        if (opts != null) {
                            r.updateOptionsLocked(opts);
                        }
                        if (this.stack.finishActivityLocked(r2, 0, null, "clear", false)) {
                            activityNdx2--;
                            numActivities--;
                        }
                    }
                    activityNdx2++;
                }
                if (r.launchMode != 0 || (536870912 & launchFlags) != 0 || r.finishing) {
                    return r;
                }
                this.stack.finishActivityLocked(r, 0, null, "clear", false);
                return null;
            }
        }
        return null;
    }

    public ActivityManager.TaskThumbnail getTaskThumbnailLocked() {
        ActivityRecord resumedActivity;
        if (this.stack != null && (resumedActivity = this.stack.mResumedActivity) != null && resumedActivity.task == this) {
            Bitmap thumbnail = this.stack.screenshotActivities(resumedActivity);
            setLastThumbnail(thumbnail);
        }
        ActivityManager.TaskThumbnail taskThumbnail = new ActivityManager.TaskThumbnail();
        getLastThumbnail(taskThumbnail);
        return taskThumbnail;
    }

    public void removeTaskActivitiesLocked() {
        performClearTaskAtIndexLocked(0);
    }

    boolean isHomeTask() {
        if (this.taskType == 1) {
            return IGNORE_RETURN_TO_RECENTS;
        }
        return false;
    }

    boolean isApplicationTask() {
        if (this.taskType == 0) {
            return IGNORE_RETURN_TO_RECENTS;
        }
        return false;
    }

    boolean isOverHomeStack() {
        if (this.mTaskToReturnTo == 1 || this.mTaskToReturnTo == 2) {
            return IGNORE_RETURN_TO_RECENTS;
        }
        return false;
    }

    final ActivityRecord findActivityInHistoryLocked(ActivityRecord r) {
        ComponentName realActivity = r.realActivity;
        for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord candidate = this.mActivities.get(activityNdx);
            if (!candidate.finishing && candidate.realActivity.equals(realActivity)) {
                return candidate;
            }
        }
        return null;
    }

    void updateTaskDescription() {
        boolean relinquish = false;
        int numActivities = this.mActivities.size();
        if (numActivities != 0 && (this.mActivities.get(0).info.flags & PackageManagerService.DumpState.DUMP_VERSION) != 0) {
            relinquish = true;
        }
        int activityNdx = Math.min(numActivities, 1);
        while (true) {
            if (activityNdx >= numActivities) {
                break;
            }
            ActivityRecord r = this.mActivities.get(activityNdx);
            if (relinquish && (r.info.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0) {
                activityNdx++;
                break;
            } else if (r.intent != null && (r.intent.getFlags() & 524288) != 0) {
                break;
            } else {
                activityNdx++;
            }
        }
        if (activityNdx > 0) {
            String label = null;
            String iconFilename = null;
            int colorPrimary = 0;
            for (int activityNdx2 = activityNdx - 1; activityNdx2 >= 0; activityNdx2--) {
                ActivityRecord r2 = this.mActivities.get(activityNdx2);
                if (r2.taskDescription != null) {
                    if (label == null) {
                        label = r2.taskDescription.getLabel();
                    }
                    if (iconFilename == null) {
                        iconFilename = r2.taskDescription.getIconFilename();
                    }
                    if (colorPrimary == 0) {
                        colorPrimary = r2.taskDescription.getPrimaryColor();
                    }
                }
            }
            this.lastTaskDescription = new ActivityManager.TaskDescription(label, colorPrimary, iconFilename);
            if (this.taskId == this.mAffiliatedTaskId) {
                this.mAffiliatedTaskColor = this.lastTaskDescription.getPrimaryColor();
            }
        }
    }

    int findEffectiveRootIndex() {
        int effectiveNdx = 0;
        int topActivityNdx = this.mActivities.size() - 1;
        for (int activityNdx = 0; activityNdx <= topActivityNdx; activityNdx++) {
            ActivityRecord r = this.mActivities.get(activityNdx);
            if (!r.finishing) {
                effectiveNdx = activityNdx;
                if ((r.info.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0) {
                    break;
                }
            }
        }
        return effectiveNdx;
    }

    void updateEffectiveIntent() {
        int effectiveRootIndex = findEffectiveRootIndex();
        ActivityRecord r = this.mActivities.get(effectiveRootIndex);
        setIntent(r);
    }

    void saveToXml(XmlSerializer out) throws XmlPullParserException, IOException {
        out.attribute(null, ATTR_TASKID, String.valueOf(this.taskId));
        if (this.realActivity != null) {
            out.attribute(null, ATTR_REALACTIVITY, this.realActivity.flattenToShortString());
        }
        if (this.origActivity != null) {
            out.attribute(null, ATTR_ORIGACTIVITY, this.origActivity.flattenToShortString());
        }
        if (this.affinity != null) {
            out.attribute(null, ATTR_AFFINITY, this.affinity);
            if (!this.affinity.equals(this.rootAffinity)) {
                out.attribute(null, ATTR_ROOT_AFFINITY, this.rootAffinity != null ? this.rootAffinity : "@");
            }
        } else if (this.rootAffinity != null) {
            out.attribute(null, ATTR_ROOT_AFFINITY, this.rootAffinity != null ? this.rootAffinity : "@");
        }
        out.attribute(null, ATTR_ROOTHASRESET, String.valueOf(this.rootWasReset));
        out.attribute(null, ATTR_AUTOREMOVERECENTS, String.valueOf(this.autoRemoveRecents));
        out.attribute(null, ATTR_ASKEDCOMPATMODE, String.valueOf(this.askedCompatMode));
        out.attribute(null, ATTR_USERID, String.valueOf(this.userId));
        out.attribute(null, ATTR_EFFECTIVE_UID, String.valueOf(this.effectiveUid));
        out.attribute(null, ATTR_TASKTYPE, String.valueOf(this.taskType));
        out.attribute(null, ATTR_FIRSTACTIVETIME, String.valueOf(this.firstActiveTime));
        out.attribute(null, ATTR_LASTACTIVETIME, String.valueOf(this.lastActiveTime));
        out.attribute(null, ATTR_LASTTIMEMOVED, String.valueOf(this.mLastTimeMoved));
        out.attribute(null, ATTR_NEVERRELINQUISH, String.valueOf(this.mNeverRelinquishIdentity));
        if (this.lastDescription != null) {
            out.attribute(null, ATTR_LASTDESCRIPTION, this.lastDescription.toString());
        }
        if (this.lastTaskDescription != null) {
            this.lastTaskDescription.saveToXml(out);
        }
        out.attribute(null, ATTR_TASK_AFFILIATION_COLOR, String.valueOf(this.mAffiliatedTaskColor));
        out.attribute(null, ATTR_TASK_AFFILIATION, String.valueOf(this.mAffiliatedTaskId));
        out.attribute(null, ATTR_PREV_AFFILIATION, String.valueOf(this.mPrevAffiliateTaskId));
        out.attribute(null, ATTR_NEXT_AFFILIATION, String.valueOf(this.mNextAffiliateTaskId));
        out.attribute(null, ATTR_CALLING_UID, String.valueOf(this.mCallingUid));
        out.attribute(null, ATTR_CALLING_PACKAGE, this.mCallingPackage == null ? "" : this.mCallingPackage);
        if (this.affinityIntent != null) {
            out.startTag(null, TAG_AFFINITYINTENT);
            this.affinityIntent.saveToXml(out);
            out.endTag(null, TAG_AFFINITYINTENT);
        }
        out.startTag(null, TAG_INTENT);
        this.intent.saveToXml(out);
        out.endTag(null, TAG_INTENT);
        ArrayList<ActivityRecord> activities = this.mActivities;
        int numActivities = activities.size();
        for (int activityNdx = 0; activityNdx < numActivities; activityNdx++) {
            ActivityRecord r = activities.get(activityNdx);
            if (r.info.persistableMode == 0 || !r.isPersistable()) {
                return;
            }
            if ((r.intent.getFlags() & 524288) == 0 || activityNdx <= 0) {
                out.startTag(null, TAG_ACTIVITY);
                r.saveToXml(out);
                out.endTag(null, TAG_ACTIVITY);
            } else {
                return;
            }
        }
    }

    static TaskRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor) throws XmlPullParserException, IOException {
        return restoreFromXml(in, stackSupervisor, -1);
    }

    static TaskRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor, int inTaskId) throws XmlPullParserException, IOException {
        Intent intent = null;
        Intent affinityIntent = null;
        ArrayList<ActivityRecord> activities = new ArrayList<>();
        ComponentName realActivity = null;
        ComponentName origActivity = null;
        String affinity = null;
        String rootAffinity = null;
        boolean hasRootAffinity = false;
        boolean rootHasReset = false;
        boolean autoRemoveRecents = false;
        boolean askedCompatMode = false;
        int taskType = 0;
        int userId = 0;
        int effectiveUid = -1;
        String lastDescription = null;
        long firstActiveTime = -1;
        long lastActiveTime = -1;
        long lastTimeOnTop = 0;
        boolean neverRelinquishIdentity = IGNORE_RETURN_TO_RECENTS;
        int taskId = inTaskId;
        int outerDepth = in.getDepth();
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription();
        int taskAffiliation = -1;
        int taskAffiliationColor = 0;
        int prevTaskId = -1;
        int nextTaskId = -1;
        int callingUid = -1;
        String callingPackage = "";
        for (int attrNdx = in.getAttributeCount() - 1; attrNdx >= 0; attrNdx--) {
            String attrName = in.getAttributeName(attrNdx);
            String attrValue = in.getAttributeValue(attrNdx);
            if (ATTR_TASKID.equals(attrName)) {
                if (taskId == -1) {
                    taskId = Integer.valueOf(attrValue).intValue();
                }
            } else if (ATTR_REALACTIVITY.equals(attrName)) {
                realActivity = ComponentName.unflattenFromString(attrValue);
            } else if (ATTR_ORIGACTIVITY.equals(attrName)) {
                origActivity = ComponentName.unflattenFromString(attrValue);
            } else if (ATTR_AFFINITY.equals(attrName)) {
                affinity = attrValue;
            } else if (ATTR_ROOT_AFFINITY.equals(attrName)) {
                rootAffinity = attrValue;
                hasRootAffinity = IGNORE_RETURN_TO_RECENTS;
            } else if (ATTR_ROOTHASRESET.equals(attrName)) {
                rootHasReset = Boolean.valueOf(attrValue).booleanValue();
            } else if (ATTR_AUTOREMOVERECENTS.equals(attrName)) {
                autoRemoveRecents = Boolean.valueOf(attrValue).booleanValue();
            } else if (ATTR_ASKEDCOMPATMODE.equals(attrName)) {
                askedCompatMode = Boolean.valueOf(attrValue).booleanValue();
            } else if (ATTR_USERID.equals(attrName)) {
                userId = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_EFFECTIVE_UID.equals(attrName)) {
                effectiveUid = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_TASKTYPE.equals(attrName)) {
                taskType = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_FIRSTACTIVETIME.equals(attrName)) {
                firstActiveTime = Long.valueOf(attrValue).longValue();
            } else if (ATTR_LASTACTIVETIME.equals(attrName)) {
                lastActiveTime = Long.valueOf(attrValue).longValue();
            } else if (ATTR_LASTDESCRIPTION.equals(attrName)) {
                lastDescription = attrValue;
            } else if (ATTR_LASTTIMEMOVED.equals(attrName)) {
                lastTimeOnTop = Long.valueOf(attrValue).longValue();
            } else if (ATTR_NEVERRELINQUISH.equals(attrName)) {
                neverRelinquishIdentity = Boolean.valueOf(attrValue).booleanValue();
            } else if (attrName.startsWith("task_description_")) {
                taskDescription.restoreFromXml(attrName, attrValue);
            } else if (ATTR_TASK_AFFILIATION.equals(attrName)) {
                taskAffiliation = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_PREV_AFFILIATION.equals(attrName)) {
                prevTaskId = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_NEXT_AFFILIATION.equals(attrName)) {
                nextTaskId = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_TASK_AFFILIATION_COLOR.equals(attrName)) {
                taskAffiliationColor = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_CALLING_UID.equals(attrName)) {
                callingUid = Integer.valueOf(attrValue).intValue();
            } else if (ATTR_CALLING_PACKAGE.equals(attrName)) {
                callingPackage = attrValue;
            } else {
                Slog.w("ActivityManager", "TaskRecord: Unknown attribute=" + attrName);
            }
        }
        while (true) {
            int event = in.next();
            if (event == 1 || (event == 3 && in.getDepth() >= outerDepth)) {
                break;
            }
            if (event == 2) {
                String name = in.getName();
                if (TAG_AFFINITYINTENT.equals(name)) {
                    affinityIntent = Intent.restoreFromXml(in);
                } else if (TAG_INTENT.equals(name)) {
                    intent = Intent.restoreFromXml(in);
                } else if (TAG_ACTIVITY.equals(name)) {
                    ActivityRecord activity = ActivityRecord.restoreFromXml(in, stackSupervisor);
                    if (activity != null) {
                        activities.add(activity);
                    }
                } else {
                    Slog.e("ActivityManager", "restoreTask: Unexpected name=" + name);
                    XmlUtils.skipCurrentTag(in);
                }
            }
        }
        if (!hasRootAffinity) {
            rootAffinity = affinity;
        } else if ("@".equals(rootAffinity)) {
            rootAffinity = null;
        }
        if (effectiveUid <= 0) {
            Intent checkIntent = intent != null ? intent : affinityIntent;
            effectiveUid = 0;
            if (checkIntent != null) {
                IPackageManager pm = AppGlobals.getPackageManager();
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(checkIntent.getComponent().getPackageName(), 8704, userId);
                    if (ai != null) {
                        effectiveUid = ai.uid;
                    }
                } catch (RemoteException e) {
                }
            }
            Slog.w("ActivityManager", "Updating task #" + taskId + " for " + checkIntent + ": effectiveUid=" + effectiveUid);
        }
        TaskRecord task = new TaskRecord(stackSupervisor.mService, taskId, intent, affinityIntent, affinity, rootAffinity, realActivity, origActivity, rootHasReset, autoRemoveRecents, askedCompatMode, taskType, userId, effectiveUid, lastDescription, activities, firstActiveTime, lastActiveTime, lastTimeOnTop, neverRelinquishIdentity, taskDescription, taskAffiliation, prevTaskId, nextTaskId, taskAffiliationColor, callingUid, callingPackage);
        for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
            activities.get(activityNdx).task = task;
        }
        return task;
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("userId=");
        pw.print(this.userId);
        pw.print(" effectiveUid=");
        UserHandle.formatUid(pw, this.effectiveUid);
        pw.print(" mCallingUid=");
        UserHandle.formatUid(pw, this.mCallingUid);
        pw.print(" mCallingPackage=");
        pw.println(this.mCallingPackage);
        if (this.affinity != null || this.rootAffinity != null) {
            pw.print(prefix);
            pw.print("affinity=");
            pw.print(this.affinity);
            if (this.affinity == null || !this.affinity.equals(this.rootAffinity)) {
                pw.print(" root=");
                pw.println(this.rootAffinity);
            } else {
                pw.println();
            }
        }
        if (this.voiceSession != null || this.voiceInteractor != null) {
            pw.print(prefix);
            pw.print("VOICE: session=0x");
            pw.print(Integer.toHexString(System.identityHashCode(this.voiceSession)));
            pw.print(" interactor=0x");
            pw.println(Integer.toHexString(System.identityHashCode(this.voiceInteractor)));
        }
        if (this.intent != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(prefix);
            sb.append("intent={");
            this.intent.toShortString(sb, false, IGNORE_RETURN_TO_RECENTS, false, IGNORE_RETURN_TO_RECENTS);
            sb.append('}');
            pw.println(sb.toString());
        }
        if (this.affinityIntent != null) {
            StringBuilder sb2 = new StringBuilder(128);
            sb2.append(prefix);
            sb2.append("affinityIntent={");
            this.affinityIntent.toShortString(sb2, false, IGNORE_RETURN_TO_RECENTS, false, IGNORE_RETURN_TO_RECENTS);
            sb2.append('}');
            pw.println(sb2.toString());
        }
        if (this.origActivity != null) {
            pw.print(prefix);
            pw.print("origActivity=");
            pw.println(this.origActivity.flattenToShortString());
        }
        if (this.realActivity != null) {
            pw.print(prefix);
            pw.print("realActivity=");
            pw.println(this.realActivity.flattenToShortString());
        }
        if (this.autoRemoveRecents || this.isPersistable || this.taskType != 0 || this.mTaskToReturnTo != 0 || this.numFullscreen != 0) {
            pw.print(prefix);
            pw.print("autoRemoveRecents=");
            pw.print(this.autoRemoveRecents);
            pw.print(" isPersistable=");
            pw.print(this.isPersistable);
            pw.print(" numFullscreen=");
            pw.print(this.numFullscreen);
            pw.print(" taskType=");
            pw.print(this.taskType);
            pw.print(" mTaskToReturnTo=");
            pw.println(this.mTaskToReturnTo);
        }
        if (this.rootWasReset || this.mNeverRelinquishIdentity || this.mReuseTask) {
            pw.print(prefix);
            pw.print("rootWasReset=");
            pw.print(this.rootWasReset);
            pw.print(" mNeverRelinquishIdentity=");
            pw.print(this.mNeverRelinquishIdentity);
            pw.print(" mReuseTask=");
            pw.println(this.mReuseTask);
        }
        if (this.mAffiliatedTaskId != this.taskId || this.mPrevAffiliateTaskId != -1 || this.mPrevAffiliate != null || this.mNextAffiliateTaskId != -1 || this.mNextAffiliate != null) {
            pw.print(prefix);
            pw.print("affiliation=");
            pw.print(this.mAffiliatedTaskId);
            pw.print(" prevAffiliation=");
            pw.print(this.mPrevAffiliateTaskId);
            pw.print(" (");
            if (this.mPrevAffiliate == null) {
                pw.print("null");
            } else {
                pw.print(Integer.toHexString(System.identityHashCode(this.mPrevAffiliate)));
            }
            pw.print(") nextAffiliation=");
            pw.print(this.mNextAffiliateTaskId);
            pw.print(" (");
            if (this.mNextAffiliate == null) {
                pw.print("null");
            } else {
                pw.print(Integer.toHexString(System.identityHashCode(this.mNextAffiliate)));
            }
            pw.println(")");
        }
        pw.print(prefix);
        pw.print("Activities=");
        pw.println(this.mActivities);
        if (!this.askedCompatMode || !this.inRecents || !this.isAvailable) {
            pw.print(prefix);
            pw.print("askedCompatMode=");
            pw.print(this.askedCompatMode);
            pw.print(" inRecents=");
            pw.print(this.inRecents);
            pw.print(" isAvailable=");
            pw.println(this.isAvailable);
        }
        pw.print(prefix);
        pw.print("lastThumbnail=");
        pw.print(this.mLastThumbnail);
        pw.print(" lastThumbnailFile=");
        pw.println(this.mLastThumbnailFile);
        if (this.lastDescription != null) {
            pw.print(prefix);
            pw.print("lastDescription=");
            pw.println(this.lastDescription);
        }
        pw.print(prefix);
        pw.print("hasBeenVisible=");
        pw.print(this.hasBeenVisible);
        pw.print(" firstActiveTime=");
        pw.print(this.lastActiveTime);
        pw.print(" lastActiveTime=");
        pw.print(this.lastActiveTime);
        pw.print(" (inactive for ");
        pw.print(getInactiveDuration() / 1000);
        pw.println("s)");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        if (this.stringName != null) {
            sb.append(this.stringName);
            sb.append(" U=");
            sb.append(this.userId);
            sb.append(" sz=");
            sb.append(this.mActivities.size());
            sb.append('}');
            return sb.toString();
        }
        sb.append("TaskRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        sb.append(this.taskId);
        if (this.affinity != null) {
            sb.append(" A=");
            sb.append(this.affinity);
        } else if (this.intent != null) {
            sb.append(" I=");
            sb.append(this.intent.getComponent().flattenToShortString());
        } else if (this.affinityIntent != null) {
            sb.append(" aI=");
            sb.append(this.affinityIntent.getComponent().flattenToShortString());
        } else {
            sb.append(" ??");
        }
        this.stringName = sb.toString();
        return toString();
    }
}
