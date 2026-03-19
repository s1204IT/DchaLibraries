package com.android.server.notification;

import android.R;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioSystem;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationStats;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.notification.NotificationUsageStats;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class NotificationRecord {
    private static final int MAX_LOGTAG_LENGTH = 35;
    boolean isCanceled;
    public boolean isUpdate;
    private int mAuthoritativeRank;
    private NotificationChannel mChannel;
    private String mChannelIdLogTag;
    private float mContactAffinity;
    private final Context mContext;
    private long mCreationTimeMs;
    private String mGlobalSortKey;
    private ArraySet<Uri> mGrantableUris;
    private String mGroupLogTag;
    private boolean mHasSeenSmartReplies;
    private boolean mHidden;
    private int mImportance;
    private boolean mIntercept;
    private boolean mIsAppImportanceLocked;
    private boolean mIsInterruptive;
    private long mLastIntrusive;
    private LogMaker mLogMaker;
    private int mNumberOfSmartRepliesAdded;
    final int mOriginalFlags;
    private int mPackagePriority;
    private int mPackageVisibility;
    private String mPeopleExplanation;
    private ArrayList<String> mPeopleOverride;
    private boolean mPreChannelsNotification;
    private boolean mRecentlyIntrusive;
    private boolean mRecordedInterruption;
    private boolean mShowBadge;
    private ArrayList<SnoozeCriterion> mSnoozeCriteria;
    final int mTargetSdkVersion;
    private boolean mTextChanged;
    private long mUpdateTimeMs;
    private String mUserExplanation;
    private int mUserSentiment;
    private long mVisibleSinceMs;
    IBinder permissionOwner;
    final StatusBarNotification sbn;
    static final String TAG = "NotificationRecord";
    static final boolean DBG = Log.isLoggable(TAG, 3);
    private int mUserImportance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
    private CharSequence mImportanceExplanation = null;
    private int mSuppressedVisualEffects = 0;
    IActivityManager mAm = ActivityManager.getService();
    private long mRankingTimeMs = calculateRankingTimeMs(0);
    NotificationUsageStats.SingleNotificationStats stats = new NotificationUsageStats.SingleNotificationStats();
    private Uri mSound = calculateSound();
    private long[] mVibration = calculateVibration();
    private AudioAttributes mAttributes = calculateAttributes();
    private Light mLight = calculateLights();
    private final List<Adjustment> mAdjustments = new ArrayList();
    private final NotificationStats mStats = new NotificationStats();

    public NotificationRecord(Context context, StatusBarNotification statusBarNotification, NotificationChannel notificationChannel) {
        this.mImportance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        this.mPreChannelsNotification = true;
        this.sbn = statusBarNotification;
        this.mTargetSdkVersion = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageTargetSdkVersion(statusBarNotification.getPackageName());
        this.mOriginalFlags = statusBarNotification.getNotification().flags;
        this.mCreationTimeMs = statusBarNotification.getPostTime();
        this.mUpdateTimeMs = this.mCreationTimeMs;
        this.mContext = context;
        this.mChannel = notificationChannel;
        this.mPreChannelsNotification = isPreChannelsNotification();
        this.mImportance = calculateImportance();
        calculateUserSentiment();
        calculateGrantableUris();
    }

    private boolean isPreChannelsNotification() {
        if ("miscellaneous".equals(getChannel().getId()) && this.mTargetSdkVersion < 26) {
            return true;
        }
        return false;
    }

    private Uri calculateSound() {
        Notification notification = this.sbn.getNotification();
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) {
            return null;
        }
        Uri sound = this.mChannel.getSound();
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 32) == 0) {
            if ((notification.defaults & 1) != 0) {
                return Settings.System.DEFAULT_NOTIFICATION_URI;
            }
            return notification.sound;
        }
        return sound;
    }

    private Light calculateLights() {
        int color = this.mContext.getResources().getColor(R.color.system_secondary_container_light);
        int integer = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_types_to_bg_restricted);
        int integer2 = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_power_components);
        Light light = getChannel().shouldShowLights() ? new Light(getChannel().getLightColor() != 0 ? getChannel().getLightColor() : color, integer, integer2) : null;
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 8) == 0) {
            Notification notification = this.sbn.getNotification();
            if ((notification.flags & 1) != 0) {
                return (notification.defaults & 4) != 0 ? new Light(color, integer, integer2) : new Light(notification.ledARGB, notification.ledOnMS, notification.ledOffMS);
            }
            return null;
        }
        return light;
    }

    private long[] calculateVibration() {
        long[] vibrationPattern;
        long[] longArray = NotificationManagerService.getLongArray(this.mContext.getResources(), R.array.config_availableEMValueOptions, 17, NotificationManagerService.DEFAULT_VIBRATE_PATTERN);
        if (getChannel().shouldVibrate()) {
            if (getChannel().getVibrationPattern() != null) {
                vibrationPattern = getChannel().getVibrationPattern();
            } else {
                vibrationPattern = longArray;
            }
        } else {
            vibrationPattern = null;
        }
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 16) == 0) {
            Notification notification = this.sbn.getNotification();
            return (notification.defaults & 2) != 0 ? longArray : notification.vibrate;
        }
        return vibrationPattern;
    }

    private AudioAttributes calculateAttributes() {
        Notification notification = this.sbn.getNotification();
        AudioAttributes audioAttributes = getChannel().getAudioAttributes();
        if (audioAttributes == null) {
            audioAttributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
        }
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 32) == 0) {
            if (notification.audioAttributes != null) {
                return notification.audioAttributes;
            }
            if (notification.audioStreamType >= 0 && notification.audioStreamType < AudioSystem.getNumStreamTypes()) {
                return new AudioAttributes.Builder().setInternalLegacyStreamType(notification.audioStreamType).build();
            }
            if (notification.audioStreamType != -1) {
                Log.w(TAG, String.format("Invalid stream type: %d", Integer.valueOf(notification.audioStreamType)));
                return audioAttributes;
            }
            return audioAttributes;
        }
        return audioAttributes;
    }

    private int calculateImportance() {
        int i;
        Notification notification = this.sbn.getNotification();
        int importance = getChannel().getImportance();
        if ((notification.flags & 128) != 0) {
            notification.priority = 2;
        }
        notification.priority = NotificationManagerService.clamp(notification.priority, -2, 2);
        boolean z = true;
        switch (notification.priority) {
            case -2:
                i = 1;
                break;
            case -1:
                i = 2;
                break;
            case 0:
            default:
                i = 3;
                break;
            case 1:
            case 2:
                i = 4;
                break;
        }
        this.stats.requestedImportance = i;
        NotificationUsageStats.SingleNotificationStats singleNotificationStats = this.stats;
        if (this.mSound == null && this.mVibration == null) {
            z = false;
        }
        singleNotificationStats.isNoisy = z;
        if (this.mPreChannelsNotification && (importance == -1000 || (getChannel().getUserLockedFields() & 4) == 0)) {
            if (!this.stats.isNoisy && i > 2) {
                i = 2;
            }
            if (this.stats.isNoisy && i < 3) {
                i = 3;
            }
            importance = notification.fullScreenIntent != null ? 4 : i;
        }
        this.stats.naturalImportance = importance;
        return importance;
    }

    public void copyRankingInformation(NotificationRecord notificationRecord) {
        this.mContactAffinity = notificationRecord.mContactAffinity;
        this.mRecentlyIntrusive = notificationRecord.mRecentlyIntrusive;
        this.mPackagePriority = notificationRecord.mPackagePriority;
        this.mPackageVisibility = notificationRecord.mPackageVisibility;
        this.mIntercept = notificationRecord.mIntercept;
        this.mHidden = notificationRecord.mHidden;
        this.mRankingTimeMs = calculateRankingTimeMs(notificationRecord.getRankingTimeMs());
        this.mCreationTimeMs = notificationRecord.mCreationTimeMs;
        this.mVisibleSinceMs = notificationRecord.mVisibleSinceMs;
        if (notificationRecord.sbn.getOverrideGroupKey() != null && !this.sbn.isAppGroup()) {
            this.sbn.setOverrideGroupKey(notificationRecord.sbn.getOverrideGroupKey());
        }
    }

    public Notification getNotification() {
        return this.sbn.getNotification();
    }

    public int getFlags() {
        return this.sbn.getNotification().flags;
    }

    public UserHandle getUser() {
        return this.sbn.getUser();
    }

    public String getKey() {
        return this.sbn.getKey();
    }

    public int getUserId() {
        return this.sbn.getUserId();
    }

    public int getUid() {
        return this.sbn.getUid();
    }

    void dump(ProtoOutputStream protoOutputStream, long j, boolean z, int i) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.sbn.getKey());
        protoOutputStream.write(1159641169922L, i);
        if (getChannel() != null) {
            protoOutputStream.write(1138166333444L, getChannel().getId());
        }
        protoOutputStream.write(1133871366152L, getLight() != null);
        protoOutputStream.write(1133871366151L, getVibration() != null);
        protoOutputStream.write(1120986464259L, this.sbn.getNotification().flags);
        protoOutputStream.write(1138166333449L, getGroupKey());
        protoOutputStream.write(1172526071818L, getImportance());
        if (getSound() != null) {
            protoOutputStream.write(1138166333445L, getSound().toString());
        }
        if (getAudioAttributes() != null) {
            getAudioAttributes().writeToProto(protoOutputStream, 1146756268038L);
        }
        protoOutputStream.end(jStart);
    }

    String formatRemoteViews(RemoteViews remoteViews) {
        return remoteViews == null ? "null" : String.format("%s/0x%08x (%d bytes): %s", remoteViews.getPackage(), Integer.valueOf(remoteViews.getLayoutId()), Integer.valueOf(remoteViews.estimateMemoryUsage()), remoteViews.toString());
    }

    void dump(PrintWriter printWriter, String str, Context context, boolean z) {
        Notification notification = this.sbn.getNotification();
        Icon smallIcon = notification.getSmallIcon();
        String strValueOf = String.valueOf(smallIcon);
        if (smallIcon != null && smallIcon.getType() == 2) {
            strValueOf = strValueOf + " / " + idDebugString(context, smallIcon.getResPackage(), smallIcon.getResId());
        }
        printWriter.println(str + this);
        String str2 = str + "  ";
        printWriter.println(str2 + "uid=" + this.sbn.getUid() + " userId=" + this.sbn.getUserId());
        StringBuilder sb = new StringBuilder();
        sb.append(str2);
        sb.append("icon=");
        sb.append(strValueOf);
        printWriter.println(sb.toString());
        printWriter.println(str2 + "flags=0x" + Integer.toHexString(notification.flags));
        printWriter.println(str2 + "pri=" + notification.priority);
        printWriter.println(str2 + "key=" + this.sbn.getKey());
        printWriter.println(str2 + "seen=" + this.mStats.hasSeen());
        printWriter.println(str2 + "groupKey=" + getGroupKey());
        printWriter.println(str2 + "fullscreenIntent=" + notification.fullScreenIntent);
        printWriter.println(str2 + "contentIntent=" + notification.contentIntent);
        printWriter.println(str2 + "deleteIntent=" + notification.deleteIntent);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str2);
        sb2.append("tickerText=");
        printWriter.print(sb2.toString());
        if (!TextUtils.isEmpty(notification.tickerText)) {
            String string = notification.tickerText.toString();
            if (z) {
                printWriter.print(string.length() > 16 ? string.substring(0, 8) : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                printWriter.println("...");
            } else {
                printWriter.println(string);
            }
        } else {
            printWriter.println("null");
        }
        printWriter.println(str2 + "contentView=" + formatRemoteViews(notification.contentView));
        printWriter.println(str2 + "bigContentView=" + formatRemoteViews(notification.bigContentView));
        printWriter.println(str2 + "headsUpContentView=" + formatRemoteViews(notification.headsUpContentView));
        StringBuilder sb3 = new StringBuilder();
        sb3.append(str2);
        sb3.append(String.format("color=0x%08x", Integer.valueOf(notification.color)));
        printWriter.print(sb3.toString());
        printWriter.println(str2 + "timeout=" + TimeUtils.formatForLogging(notification.getTimeoutAfter()));
        if (notification.actions != null && notification.actions.length > 0) {
            printWriter.println(str2 + "actions={");
            int length = notification.actions.length;
            for (int i = 0; i < length; i++) {
                Notification.Action action = notification.actions[i];
                if (action != null) {
                    Object[] objArr = new Object[4];
                    objArr[0] = str2;
                    objArr[1] = Integer.valueOf(i);
                    objArr[2] = action.title;
                    objArr[3] = action.actionIntent == null ? "null" : action.actionIntent.toString();
                    printWriter.println(String.format("%s    [%d] \"%s\" -> %s", objArr));
                }
            }
            printWriter.println(str2 + "  }");
        }
        if (notification.extras != null && notification.extras.size() > 0) {
            printWriter.println(str2 + "extras={");
            for (String str3 : notification.extras.keySet()) {
                printWriter.print(str2 + "    " + str3 + "=");
                Object obj = notification.extras.get(str3);
                if (obj == null) {
                    printWriter.println("null");
                } else {
                    printWriter.print(obj.getClass().getSimpleName());
                    if (!z || (!(obj instanceof CharSequence) && !(obj instanceof String))) {
                        if (obj instanceof Bitmap) {
                            Bitmap bitmap = (Bitmap) obj;
                            printWriter.print(String.format(" (%dx%d)", Integer.valueOf(bitmap.getWidth()), Integer.valueOf(bitmap.getHeight())));
                        } else if (obj.getClass().isArray()) {
                            int length2 = Array.getLength(obj);
                            printWriter.print(" (" + length2 + ")");
                            if (!z) {
                                for (int i2 = 0; i2 < length2; i2++) {
                                    printWriter.println();
                                    printWriter.print(String.format("%s      [%d] %s", str2, Integer.valueOf(i2), String.valueOf(Array.get(obj, i2))));
                                }
                            }
                        } else {
                            printWriter.print(" (" + String.valueOf(obj) + ")");
                        }
                    }
                    printWriter.println();
                }
            }
            printWriter.println(str2 + "}");
        }
        printWriter.println(str2 + "stats=" + this.stats.toString());
        printWriter.println(str2 + "mContactAffinity=" + this.mContactAffinity);
        printWriter.println(str2 + "mRecentlyIntrusive=" + this.mRecentlyIntrusive);
        printWriter.println(str2 + "mPackagePriority=" + this.mPackagePriority);
        printWriter.println(str2 + "mPackageVisibility=" + this.mPackageVisibility);
        printWriter.println(str2 + "mUserImportance=" + NotificationListenerService.Ranking.importanceToString(this.mUserImportance));
        printWriter.println(str2 + "mImportance=" + NotificationListenerService.Ranking.importanceToString(this.mImportance));
        printWriter.println(str2 + "mImportanceExplanation=" + ((Object) this.mImportanceExplanation));
        printWriter.println(str2 + "mIsAppImportanceLocked=" + this.mIsAppImportanceLocked);
        printWriter.println(str2 + "mIntercept=" + this.mIntercept);
        printWriter.println(str2 + "mHidden==" + this.mHidden);
        printWriter.println(str2 + "mGlobalSortKey=" + this.mGlobalSortKey);
        printWriter.println(str2 + "mRankingTimeMs=" + this.mRankingTimeMs);
        printWriter.println(str2 + "mCreationTimeMs=" + this.mCreationTimeMs);
        printWriter.println(str2 + "mVisibleSinceMs=" + this.mVisibleSinceMs);
        printWriter.println(str2 + "mUpdateTimeMs=" + this.mUpdateTimeMs);
        printWriter.println(str2 + "mSuppressedVisualEffects= " + this.mSuppressedVisualEffects);
        if (this.mPreChannelsNotification) {
            printWriter.println(str2 + String.format("defaults=0x%08x flags=0x%08x", Integer.valueOf(notification.defaults), Integer.valueOf(notification.flags)));
            printWriter.println(str2 + "n.sound=" + notification.sound);
            printWriter.println(str2 + "n.audioStreamType=" + notification.audioStreamType);
            printWriter.println(str2 + "n.audioAttributes=" + notification.audioAttributes);
            StringBuilder sb4 = new StringBuilder();
            sb4.append(str2);
            sb4.append(String.format("  led=0x%08x onMs=%d offMs=%d", Integer.valueOf(notification.ledARGB), Integer.valueOf(notification.ledOnMS), Integer.valueOf(notification.ledOffMS)));
            printWriter.println(sb4.toString());
            printWriter.println(str2 + "vibrate=" + Arrays.toString(notification.vibrate));
        }
        printWriter.println(str2 + "mSound= " + this.mSound);
        printWriter.println(str2 + "mVibration= " + this.mVibration);
        printWriter.println(str2 + "mAttributes= " + this.mAttributes);
        printWriter.println(str2 + "mLight= " + this.mLight);
        printWriter.println(str2 + "mShowBadge=" + this.mShowBadge);
        printWriter.println(str2 + "mColorized=" + notification.isColorized());
        printWriter.println(str2 + "mIsInterruptive=" + this.mIsInterruptive);
        printWriter.println(str2 + "effectiveNotificationChannel=" + getChannel());
        if (getPeopleOverride() != null) {
            printWriter.println(str2 + "overridePeople= " + TextUtils.join(",", getPeopleOverride()));
        }
        if (getSnoozeCriteria() != null) {
            printWriter.println(str2 + "snoozeCriteria=" + TextUtils.join(",", getSnoozeCriteria()));
        }
        printWriter.println(str2 + "mAdjustments=" + this.mAdjustments);
    }

    static String idDebugString(Context context, String str, int i) throws PackageManager.NameNotFoundException {
        if (str != null) {
            try {
                context = context.createPackageContext(str, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        try {
            return context.getResources().getResourceName(i);
        } catch (Resources.NotFoundException e2) {
            return "<name unknown>";
        }
    }

    public final String toString() {
        return String.format("NotificationRecord(0x%08x: pkg=%s user=%s id=%d tag=%s importance=%d key=%sappImportanceLocked=%s: %s)", Integer.valueOf(System.identityHashCode(this)), this.sbn.getPackageName(), this.sbn.getUser(), Integer.valueOf(this.sbn.getId()), this.sbn.getTag(), Integer.valueOf(this.mImportance), this.sbn.getKey(), Boolean.valueOf(this.mIsAppImportanceLocked), this.sbn.getNotification());
    }

    public void addAdjustment(Adjustment adjustment) {
        synchronized (this.mAdjustments) {
            this.mAdjustments.add(adjustment);
        }
    }

    public void applyAdjustments() {
        synchronized (this.mAdjustments) {
            for (Adjustment adjustment : this.mAdjustments) {
                Bundle signals = adjustment.getSignals();
                if (signals.containsKey("key_people")) {
                    setPeopleOverride(adjustment.getSignals().getStringArrayList("key_people"));
                }
                if (signals.containsKey("key_snooze_criteria")) {
                    setSnoozeCriteria(adjustment.getSignals().getParcelableArrayList("key_snooze_criteria"));
                }
                if (signals.containsKey("key_group_key")) {
                    setOverrideGroupKey(adjustment.getSignals().getString("key_group_key"));
                }
                if (signals.containsKey("key_user_sentiment") && !this.mIsAppImportanceLocked && (getChannel().getUserLockedFields() & 4) == 0) {
                    setUserSentiment(adjustment.getSignals().getInt("key_user_sentiment", 0));
                }
            }
        }
    }

    public void setIsAppImportanceLocked(boolean z) {
        this.mIsAppImportanceLocked = z;
        calculateUserSentiment();
    }

    public void setContactAffinity(float f) {
        this.mContactAffinity = f;
        if (this.mImportance < 3 && this.mContactAffinity > 0.5f) {
            setImportance(3, getPeopleExplanation());
        }
    }

    public float getContactAffinity() {
        return this.mContactAffinity;
    }

    public void setRecentlyIntrusive(boolean z) {
        this.mRecentlyIntrusive = z;
        if (z) {
            this.mLastIntrusive = System.currentTimeMillis();
        }
    }

    public boolean isRecentlyIntrusive() {
        return this.mRecentlyIntrusive;
    }

    public long getLastIntrusive() {
        return this.mLastIntrusive;
    }

    public void setPackagePriority(int i) {
        this.mPackagePriority = i;
    }

    public int getPackagePriority() {
        return this.mPackagePriority;
    }

    public void setPackageVisibilityOverride(int i) {
        this.mPackageVisibility = i;
    }

    public int getPackageVisibilityOverride() {
        return this.mPackageVisibility;
    }

    public void setUserImportance(int i) {
        this.mUserImportance = i;
        applyUserImportance();
    }

    private String getUserExplanation() {
        if (this.mUserExplanation == null) {
            this.mUserExplanation = this.mContext.getResources().getString(R.string.config_mobile_hotspot_provision_app_no_ui);
        }
        return this.mUserExplanation;
    }

    private String getPeopleExplanation() {
        if (this.mPeopleExplanation == null) {
            this.mPeopleExplanation = this.mContext.getResources().getString(R.string.config_mms_user_agent_profile_url);
        }
        return this.mPeopleExplanation;
    }

    private void applyUserImportance() {
        if (this.mUserImportance != -1000) {
            this.mImportance = this.mUserImportance;
            this.mImportanceExplanation = getUserExplanation();
        }
    }

    public int getUserImportance() {
        return this.mUserImportance;
    }

    public void setImportance(int i, CharSequence charSequence) {
        if (i != -1000) {
            this.mImportance = i;
            this.mImportanceExplanation = charSequence;
        }
        applyUserImportance();
    }

    public int getImportance() {
        return this.mImportance;
    }

    public CharSequence getImportanceExplanation() {
        return this.mImportanceExplanation;
    }

    public boolean setIntercepted(boolean z) {
        this.mIntercept = z;
        return this.mIntercept;
    }

    public boolean isIntercepted() {
        return this.mIntercept;
    }

    public void setHidden(boolean z) {
        this.mHidden = z;
    }

    public boolean isHidden() {
        return this.mHidden;
    }

    public void setSuppressedVisualEffects(int i) {
        this.mSuppressedVisualEffects = i;
    }

    public int getSuppressedVisualEffects() {
        return this.mSuppressedVisualEffects;
    }

    public boolean isCategory(String str) {
        return Objects.equals(getNotification().category, str);
    }

    public boolean isAudioAttributesUsage(int i) {
        return this.mAttributes != null && this.mAttributes.getUsage() == i;
    }

    public long getRankingTimeMs() {
        return this.mRankingTimeMs;
    }

    public int getFreshnessMs(long j) {
        return (int) (j - this.mUpdateTimeMs);
    }

    public int getLifespanMs(long j) {
        return (int) (j - this.mCreationTimeMs);
    }

    public int getExposureMs(long j) {
        if (this.mVisibleSinceMs == 0) {
            return 0;
        }
        return (int) (j - this.mVisibleSinceMs);
    }

    public void setVisibility(boolean z, int i, int i2) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mVisibleSinceMs = z ? jCurrentTimeMillis : this.mVisibleSinceMs;
        this.stats.onVisibilityChanged(z);
        MetricsLogger.action(getLogMaker(jCurrentTimeMillis).setCategory(128).setType(z ? 1 : 2).addTaggedData(798, Integer.valueOf(i)).addTaggedData(1395, Integer.valueOf(i2)));
        if (z) {
            setSeen();
            MetricsLogger.histogram(this.mContext, "note_freshness", getFreshnessMs(jCurrentTimeMillis));
        }
        EventLogTags.writeNotificationVisibility(getKey(), z ? 1 : 0, getLifespanMs(jCurrentTimeMillis), getFreshnessMs(jCurrentTimeMillis), 0, i);
    }

    private long calculateRankingTimeMs(long j) {
        Notification notification = getNotification();
        if (notification.when != 0 && notification.when <= this.sbn.getPostTime()) {
            return notification.when;
        }
        if (j > 0) {
            return j;
        }
        return this.sbn.getPostTime();
    }

    public void setGlobalSortKey(String str) {
        this.mGlobalSortKey = str;
    }

    public String getGlobalSortKey() {
        return this.mGlobalSortKey;
    }

    public boolean isSeen() {
        return this.mStats.hasSeen();
    }

    public void setSeen() {
        this.mStats.setSeen();
        if (this.mTextChanged) {
            this.mIsInterruptive = true;
        }
    }

    public void setAuthoritativeRank(int i) {
        this.mAuthoritativeRank = i;
    }

    public int getAuthoritativeRank() {
        return this.mAuthoritativeRank;
    }

    public String getGroupKey() {
        return this.sbn.getGroupKey();
    }

    public void setOverrideGroupKey(String str) {
        this.sbn.setOverrideGroupKey(str);
        this.mGroupLogTag = null;
    }

    private String getGroupLogTag() {
        if (this.mGroupLogTag == null) {
            this.mGroupLogTag = shortenTag(this.sbn.getGroup());
        }
        return this.mGroupLogTag;
    }

    private String getChannelIdLogTag() {
        if (this.mChannelIdLogTag == null) {
            this.mChannelIdLogTag = shortenTag(this.mChannel.getId());
        }
        return this.mChannelIdLogTag;
    }

    private String shortenTag(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() < 35) {
            return str;
        }
        return str.substring(0, 27) + "-" + Integer.toHexString(str.hashCode());
    }

    public NotificationChannel getChannel() {
        return this.mChannel;
    }

    public boolean getIsAppImportanceLocked() {
        return this.mIsAppImportanceLocked;
    }

    protected void updateNotificationChannel(NotificationChannel notificationChannel) {
        if (notificationChannel != null) {
            this.mChannel = notificationChannel;
            calculateImportance();
            calculateUserSentiment();
        }
    }

    public void setShowBadge(boolean z) {
        this.mShowBadge = z;
    }

    public boolean canShowBadge() {
        return this.mShowBadge;
    }

    public Light getLight() {
        return this.mLight;
    }

    public Uri getSound() {
        return this.mSound;
    }

    public long[] getVibration() {
        return this.mVibration;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    public ArrayList<String> getPeopleOverride() {
        return this.mPeopleOverride;
    }

    public void setInterruptive(boolean z) {
        this.mIsInterruptive = z;
    }

    public void setTextChanged(boolean z) {
        this.mTextChanged = z;
    }

    public void setRecordedInterruption(boolean z) {
        this.mRecordedInterruption = z;
    }

    public boolean hasRecordedInterruption() {
        return this.mRecordedInterruption;
    }

    public boolean isInterruptive() {
        return this.mIsInterruptive;
    }

    protected void setPeopleOverride(ArrayList<String> arrayList) {
        this.mPeopleOverride = arrayList;
    }

    public ArrayList<SnoozeCriterion> getSnoozeCriteria() {
        return this.mSnoozeCriteria;
    }

    protected void setSnoozeCriteria(ArrayList<SnoozeCriterion> arrayList) {
        this.mSnoozeCriteria = arrayList;
    }

    private void calculateUserSentiment() {
        if ((getChannel().getUserLockedFields() & 4) != 0 || this.mIsAppImportanceLocked) {
            this.mUserSentiment = 1;
        }
    }

    private void setUserSentiment(int i) {
        this.mUserSentiment = i;
    }

    public int getUserSentiment() {
        return this.mUserSentiment;
    }

    public NotificationStats getStats() {
        return this.mStats;
    }

    public void recordExpanded() {
        this.mStats.setExpanded();
    }

    public void recordDirectReplied() {
        this.mStats.setDirectReplied();
    }

    public void recordDismissalSurface(int i) {
        this.mStats.setDismissalSurface(i);
    }

    public void recordSnoozed() {
        this.mStats.setSnoozed();
    }

    public void recordViewedSettings() {
        this.mStats.setViewedSettings();
    }

    public void setNumSmartRepliesAdded(int i) {
        this.mNumberOfSmartRepliesAdded = i;
    }

    public int getNumSmartRepliesAdded() {
        return this.mNumberOfSmartRepliesAdded;
    }

    public boolean hasSeenSmartReplies() {
        return this.mHasSeenSmartReplies;
    }

    public void setSeenSmartReplies(boolean z) {
        this.mHasSeenSmartReplies = z;
    }

    public ArraySet<Uri> getGrantableUris() {
        return this.mGrantableUris;
    }

    protected void calculateGrantableUris() {
        NotificationChannel channel;
        Notification notification = getNotification();
        notification.visitUris(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.visitGrantableUri((Uri) obj, false);
            }
        });
        if (notification.getChannelId() != null && (channel = getChannel()) != null) {
            visitGrantableUri(channel.getSound(), (channel.getUserLockedFields() & 32) != 0);
        }
    }

    private void visitGrantableUri(Uri uri, boolean z) {
        int uid;
        if (uri == null || !"content".equals(uri.getScheme()) || (uid = this.sbn.getUid()) == 1000) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                this.mAm.checkGrantUriPermission(uid, (String) null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(uid)));
                if (this.mGrantableUris == null) {
                    this.mGrantableUris = new ArraySet<>();
                }
                this.mGrantableUris.add(uri);
            } catch (RemoteException e) {
            } catch (SecurityException e2) {
                if (!z) {
                    if (this.mTargetSdkVersion >= 28) {
                        throw e2;
                    }
                    Log.w(TAG, "Ignoring " + uri + " from " + uid + ": " + e2.getMessage());
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public LogMaker getLogMaker(long j) {
        if (this.mLogMaker == null) {
            this.mLogMaker = new LogMaker(0).setPackageName(this.sbn.getPackageName()).addTaggedData(796, Integer.valueOf(this.sbn.getId())).addTaggedData(797, this.sbn.getTag()).addTaggedData(857, getChannelIdLogTag());
        }
        return this.mLogMaker.clearCategory().clearType().clearSubtype().clearTaggedData(798).addTaggedData(858, Integer.valueOf(this.mImportance)).addTaggedData(946, getGroupLogTag()).addTaggedData(947, Integer.valueOf(this.sbn.getNotification().isGroupSummary() ? 1 : 0)).addTaggedData(793, Integer.valueOf(getLifespanMs(j))).addTaggedData(795, Integer.valueOf(getFreshnessMs(j))).addTaggedData(794, Integer.valueOf(getExposureMs(j)));
    }

    public LogMaker getLogMaker() {
        return getLogMaker(System.currentTimeMillis());
    }

    @VisibleForTesting
    static final class Light {
        public final int color;
        public final int offMs;
        public final int onMs;

        public Light(int i, int i2, int i3) {
            this.color = i;
            this.onMs = i2;
            this.offMs = i3;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Light light = (Light) obj;
            if (this.color == light.color && this.onMs == light.onMs && this.offMs == light.offMs) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((this.color * 31) + this.onMs)) + this.offMs;
        }

        public String toString() {
            return "Light{color=" + this.color + ", onMs=" + this.onMs + ", offMs=" + this.offMs + '}';
        }
    }
}
