package com.android.server.notification;

import android.R;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.server.EventLogTags;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.notification.NotificationUsageStats;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

public final class NotificationRecord {
    boolean isCanceled;
    public boolean isUpdate;
    private int mAuthoritativeRank;
    private float mContactAffinity;
    private final Context mContext;
    private long mCreationTimeMs;
    private String mGlobalSortKey;
    private int mImportance;
    private boolean mIntercept;
    boolean mIsSeen;
    final int mOriginalFlags;
    private int mPackagePriority;
    private int mPackageVisibility;
    private String mPeopleExplanation;
    private boolean mRecentlyIntrusive;
    private long mUpdateTimeMs;
    private String mUserExplanation;
    private long mVisibleSinceMs;
    final StatusBarNotification sbn;
    static final String TAG = "NotificationRecord";
    static final boolean DBG = Log.isLoggable(TAG, 3);
    private int mUserImportance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
    private CharSequence mImportanceExplanation = null;
    private int mSuppressedVisualEffects = 0;
    private long mRankingTimeMs = calculateRankingTimeMs(0);
    NotificationUsageStats.SingleNotificationStats stats = new NotificationUsageStats.SingleNotificationStats();

    public NotificationRecord(Context context, StatusBarNotification sbn) {
        this.mImportance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        this.sbn = sbn;
        this.mOriginalFlags = sbn.getNotification().flags;
        this.mCreationTimeMs = sbn.getPostTime();
        this.mUpdateTimeMs = this.mCreationTimeMs;
        this.mContext = context;
        this.mImportance = defaultImportance();
    }

    private int defaultImportance() {
        Notification n = this.sbn.getNotification();
        int importance = 3;
        if ((n.flags & 128) != 0) {
            n.priority = 2;
        }
        switch (n.priority) {
            case -2:
                importance = 1;
                break;
            case -1:
                importance = 2;
                break;
            case 0:
                importance = 3;
                break;
            case 1:
                importance = 4;
                break;
            case 2:
                importance = 5;
                break;
        }
        this.stats.requestedImportance = importance;
        boolean isNoisy = ((n.defaults & 1) == 0 && (n.defaults & 2) == 0 && n.sound == null && n.vibrate == null) ? false : true;
        this.stats.isNoisy = isNoisy;
        if (!isNoisy && importance > 2) {
            importance = 2;
        }
        if (isNoisy && importance < 3) {
            importance = 3;
        }
        if (n.fullScreenIntent != null) {
            importance = 5;
        }
        this.stats.naturalImportance = importance;
        return importance;
    }

    public void copyRankingInformation(NotificationRecord previous) {
        this.mContactAffinity = previous.mContactAffinity;
        this.mRecentlyIntrusive = previous.mRecentlyIntrusive;
        this.mPackagePriority = previous.mPackagePriority;
        this.mPackageVisibility = previous.mPackageVisibility;
        this.mIntercept = previous.mIntercept;
        this.mRankingTimeMs = calculateRankingTimeMs(previous.getRankingTimeMs());
        this.mCreationTimeMs = previous.mCreationTimeMs;
        this.mVisibleSinceMs = previous.mVisibleSinceMs;
        if (previous.sbn.getOverrideGroupKey() == null || this.sbn.isAppGroup()) {
            return;
        }
        this.sbn.setOverrideGroupKey(previous.sbn.getOverrideGroupKey());
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

    void dump(PrintWriter pw, String prefix, Context baseContext, boolean redact) {
        Notification notification = this.sbn.getNotification();
        Icon icon = notification.getSmallIcon();
        String iconStr = String.valueOf(icon);
        if (icon != null && icon.getType() == 2) {
            iconStr = iconStr + " / " + idDebugString(baseContext, icon.getResPackage(), icon.getResId());
        }
        pw.println(prefix + this);
        pw.println(prefix + "  uid=" + this.sbn.getUid() + " userId=" + this.sbn.getUserId());
        pw.println(prefix + "  icon=" + iconStr);
        pw.println(prefix + "  pri=" + notification.priority);
        pw.println(prefix + "  key=" + this.sbn.getKey());
        pw.println(prefix + "  seen=" + this.mIsSeen);
        pw.println(prefix + "  groupKey=" + getGroupKey());
        pw.println(prefix + "  contentIntent=" + notification.contentIntent);
        pw.println(prefix + "  deleteIntent=" + notification.deleteIntent);
        pw.println(prefix + "  tickerText=" + notification.tickerText);
        pw.println(prefix + "  contentView=" + notification.contentView);
        pw.println(prefix + String.format("  defaults=0x%08x flags=0x%08x", Integer.valueOf(notification.defaults), Integer.valueOf(notification.flags)));
        pw.println(prefix + "  sound=" + notification.sound);
        pw.println(prefix + "  audioStreamType=" + notification.audioStreamType);
        pw.println(prefix + "  audioAttributes=" + notification.audioAttributes);
        pw.println(prefix + String.format("  color=0x%08x", Integer.valueOf(notification.color)));
        pw.println(prefix + "  vibrate=" + Arrays.toString(notification.vibrate));
        pw.println(prefix + String.format("  led=0x%08x onMs=%d offMs=%d", Integer.valueOf(notification.ledARGB), Integer.valueOf(notification.ledOnMS), Integer.valueOf(notification.ledOffMS)));
        if (notification.actions != null && notification.actions.length > 0) {
            pw.println(prefix + "  actions={");
            int N = notification.actions.length;
            for (int i = 0; i < N; i++) {
                Notification.Action action = notification.actions[i];
                if (action != null) {
                    Object[] objArr = new Object[4];
                    objArr[0] = prefix;
                    objArr[1] = Integer.valueOf(i);
                    objArr[2] = action.title;
                    objArr[3] = action.actionIntent == null ? "null" : action.actionIntent.toString();
                    pw.println(String.format("%s    [%d] \"%s\" -> %s", objArr));
                }
            }
            pw.println(prefix + "  }");
        }
        if (notification.extras != null && notification.extras.size() > 0) {
            pw.println(prefix + "  extras={");
            for (String key : notification.extras.keySet()) {
                pw.print(prefix + "    " + key + "=");
                Object val = notification.extras.get(key);
                if (val == null) {
                    pw.println("null");
                } else {
                    pw.print(val.getClass().getSimpleName());
                    if (!redact || (!(val instanceof CharSequence) && !(val instanceof String))) {
                        if (val instanceof Bitmap) {
                            pw.print(String.format(" (%dx%d)", Integer.valueOf(((Bitmap) val).getWidth()), Integer.valueOf(((Bitmap) val).getHeight())));
                        } else if (val.getClass().isArray()) {
                            int N2 = Array.getLength(val);
                            pw.print(" (" + N2 + ")");
                            if (!redact) {
                                for (int j = 0; j < N2; j++) {
                                    pw.println();
                                    pw.print(String.format("%s      [%d] %s", prefix, Integer.valueOf(j), String.valueOf(Array.get(val, j))));
                                }
                            }
                        } else {
                            pw.print(" (" + String.valueOf(val) + ")");
                        }
                    }
                    pw.println();
                }
            }
            pw.println(prefix + "  }");
        }
        pw.println(prefix + "  stats=" + this.stats.toString());
        pw.println(prefix + "  mContactAffinity=" + this.mContactAffinity);
        pw.println(prefix + "  mRecentlyIntrusive=" + this.mRecentlyIntrusive);
        pw.println(prefix + "  mPackagePriority=" + this.mPackagePriority);
        pw.println(prefix + "  mPackageVisibility=" + this.mPackageVisibility);
        pw.println(prefix + "  mUserImportance=" + NotificationListenerService.Ranking.importanceToString(this.mUserImportance));
        pw.println(prefix + "  mImportance=" + NotificationListenerService.Ranking.importanceToString(this.mImportance));
        pw.println(prefix + "  mImportanceExplanation=" + this.mImportanceExplanation);
        pw.println(prefix + "  mIntercept=" + this.mIntercept);
        pw.println(prefix + "  mGlobalSortKey=" + this.mGlobalSortKey);
        pw.println(prefix + "  mRankingTimeMs=" + this.mRankingTimeMs);
        pw.println(prefix + "  mCreationTimeMs=" + this.mCreationTimeMs);
        pw.println(prefix + "  mVisibleSinceMs=" + this.mVisibleSinceMs);
        pw.println(prefix + "  mUpdateTimeMs=" + this.mUpdateTimeMs);
        pw.println(prefix + "  mSuppressedVisualEffects= " + this.mSuppressedVisualEffects);
    }

    static String idDebugString(Context baseContext, String packageName, int id) {
        Context c;
        if (packageName != null) {
            try {
                c = baseContext.createPackageContext(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                c = baseContext;
            }
        } else {
            c = baseContext;
        }
        Resources r = c.getResources();
        try {
            return r.getResourceName(id);
        } catch (Resources.NotFoundException e2) {
            return "<name unknown>";
        }
    }

    public final String toString() {
        return String.format("NotificationRecord(0x%08x: pkg=%s user=%s id=%d tag=%s importance=%d key=%s: %s)", Integer.valueOf(System.identityHashCode(this)), this.sbn.getPackageName(), this.sbn.getUser(), Integer.valueOf(this.sbn.getId()), this.sbn.getTag(), Integer.valueOf(this.mImportance), this.sbn.getKey(), this.sbn.getNotification());
    }

    public void setContactAffinity(float contactAffinity) {
        this.mContactAffinity = contactAffinity;
        if (this.mImportance >= 3 || this.mContactAffinity <= 0.5f) {
            return;
        }
        setImportance(3, getPeopleExplanation());
    }

    public float getContactAffinity() {
        return this.mContactAffinity;
    }

    public void setRecentlyIntrusive(boolean recentlyIntrusive) {
        this.mRecentlyIntrusive = recentlyIntrusive;
    }

    public boolean isRecentlyIntrusive() {
        return this.mRecentlyIntrusive;
    }

    public void setPackagePriority(int packagePriority) {
        this.mPackagePriority = packagePriority;
    }

    public int getPackagePriority() {
        return this.mPackagePriority;
    }

    public void setPackageVisibilityOverride(int packageVisibility) {
        this.mPackageVisibility = packageVisibility;
    }

    public int getPackageVisibilityOverride() {
        return this.mPackageVisibility;
    }

    public void setUserImportance(int importance) {
        this.mUserImportance = importance;
        applyUserImportance();
    }

    private String getUserExplanation() {
        if (this.mUserExplanation == null) {
            this.mUserExplanation = this.mContext.getString(R.string.lockscreen_transport_prev_description);
        }
        return this.mUserExplanation;
    }

    private String getPeopleExplanation() {
        if (this.mPeopleExplanation == null) {
            this.mPeopleExplanation = this.mContext.getString(R.string.lockscreen_transport_rew_description);
        }
        return this.mPeopleExplanation;
    }

    private void applyUserImportance() {
        if (this.mUserImportance == -1000) {
            return;
        }
        this.mImportance = this.mUserImportance;
        this.mImportanceExplanation = getUserExplanation();
    }

    public int getUserImportance() {
        return this.mUserImportance;
    }

    public void setImportance(int importance, CharSequence explanation) {
        if (importance != -1000) {
            this.mImportance = importance;
            this.mImportanceExplanation = explanation;
        }
        applyUserImportance();
    }

    public int getImportance() {
        return this.mImportance;
    }

    public CharSequence getImportanceExplanation() {
        return this.mImportanceExplanation;
    }

    public boolean setIntercepted(boolean intercept) {
        this.mIntercept = intercept;
        return this.mIntercept;
    }

    public boolean isIntercepted() {
        return this.mIntercept;
    }

    public void setSuppressedVisualEffects(int effects) {
        this.mSuppressedVisualEffects = effects;
    }

    public int getSuppressedVisualEffects() {
        return this.mSuppressedVisualEffects;
    }

    public boolean isCategory(String category) {
        return Objects.equals(getNotification().category, category);
    }

    public boolean isAudioStream(int stream) {
        return getNotification().audioStreamType == stream;
    }

    public boolean isAudioAttributesUsage(int usage) {
        AudioAttributes attributes = getNotification().audioAttributes;
        return attributes != null && attributes.getUsage() == usage;
    }

    public long getRankingTimeMs() {
        return this.mRankingTimeMs;
    }

    public int getFreshnessMs(long now) {
        return (int) (now - this.mUpdateTimeMs);
    }

    public int getLifespanMs(long now) {
        return (int) (now - this.mCreationTimeMs);
    }

    public int getExposureMs(long now) {
        if (this.mVisibleSinceMs == 0) {
            return 0;
        }
        return (int) (now - this.mVisibleSinceMs);
    }

    public void setVisibility(boolean visible, int rank) {
        long now = System.currentTimeMillis();
        this.mVisibleSinceMs = visible ? now : this.mVisibleSinceMs;
        this.stats.onVisibilityChanged(visible);
        EventLogTags.writeNotificationVisibility(getKey(), visible ? 1 : 0, (int) (now - this.mCreationTimeMs), (int) (now - this.mUpdateTimeMs), 0, rank);
    }

    private long calculateRankingTimeMs(long previousRankingTimeMs) {
        Notification n = getNotification();
        if (n.when != 0 && n.when <= this.sbn.getPostTime()) {
            return n.when;
        }
        if (previousRankingTimeMs > 0) {
            return previousRankingTimeMs;
        }
        return this.sbn.getPostTime();
    }

    public void setGlobalSortKey(String globalSortKey) {
        this.mGlobalSortKey = globalSortKey;
    }

    public String getGlobalSortKey() {
        return this.mGlobalSortKey;
    }

    public boolean isSeen() {
        return this.mIsSeen;
    }

    public void setSeen() {
        this.mIsSeen = true;
    }

    public void setAuthoritativeRank(int authoritativeRank) {
        this.mAuthoritativeRank = authoritativeRank;
    }

    public int getAuthoritativeRank() {
        return this.mAuthoritativeRank;
    }

    public String getGroupKey() {
        return this.sbn.getGroupKey();
    }

    public boolean isImportanceFromUser() {
        return this.mImportance == this.mUserImportance;
    }
}
