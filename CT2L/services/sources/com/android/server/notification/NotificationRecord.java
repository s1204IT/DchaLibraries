package com.android.server.notification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
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
    private String mGlobalSortKey;
    private boolean mIntercept;
    final int mOriginalFlags;
    private int mPackagePriority;
    private int mPackageVisibility;
    private long mRankingTimeMs = calculateRankingTimeMs(0);
    private boolean mRecentlyIntrusive;
    final StatusBarNotification sbn;
    int score;
    NotificationUsageStats.SingleNotificationStats stats;

    public NotificationRecord(StatusBarNotification sbn, int score) {
        this.sbn = sbn;
        this.score = score;
        this.mOriginalFlags = sbn.getNotification().flags;
    }

    public void copyRankingInformation(NotificationRecord previous) {
        this.mContactAffinity = previous.mContactAffinity;
        this.mRecentlyIntrusive = previous.mRecentlyIntrusive;
        this.mPackagePriority = previous.mPackagePriority;
        this.mPackageVisibility = previous.mPackageVisibility;
        this.mIntercept = previous.mIntercept;
        this.mRankingTimeMs = calculateRankingTimeMs(previous.getRankingTimeMs());
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

    void dump(PrintWriter pw, String prefix, Context baseContext) {
        Notification notification = this.sbn.getNotification();
        pw.println(prefix + this);
        pw.println(prefix + "  uid=" + this.sbn.getUid() + " userId=" + this.sbn.getUserId());
        pw.println(prefix + "  icon=0x" + Integer.toHexString(notification.icon) + " / " + idDebugString(baseContext, this.sbn.getPackageName(), notification.icon));
        pw.println(prefix + "  pri=" + notification.priority + " score=" + this.sbn.getScore());
        pw.println(prefix + "  key=" + this.sbn.getKey());
        pw.println(prefix + "  groupKey=" + getGroupKey());
        pw.println(prefix + "  contentIntent=" + notification.contentIntent);
        pw.println(prefix + "  deleteIntent=" + notification.deleteIntent);
        pw.println(prefix + "  tickerText=" + ((Object) notification.tickerText));
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
                pw.println(String.format("%s    [%d] \"%s\" -> %s", prefix, Integer.valueOf(i), action.title, action.actionIntent.toString()));
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
                    if (!(val instanceof CharSequence) && !(val instanceof String)) {
                        if (val instanceof Bitmap) {
                            pw.print(String.format(" (%dx%d)", Integer.valueOf(((Bitmap) val).getWidth()), Integer.valueOf(((Bitmap) val).getHeight())));
                        } else if (val.getClass().isArray()) {
                            int N2 = Array.getLength(val);
                            pw.println(" (" + N2 + ")");
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
        pw.println(prefix + "  mIntercept=" + this.mIntercept);
        pw.println(prefix + "  mGlobalSortKey=" + this.mGlobalSortKey);
        pw.println(prefix + "  mRankingTimeMs=" + this.mRankingTimeMs);
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
        return String.format("NotificationRecord(0x%08x: pkg=%s user=%s id=%d tag=%s score=%d key=%s: %s)", Integer.valueOf(System.identityHashCode(this)), this.sbn.getPackageName(), this.sbn.getUser(), Integer.valueOf(this.sbn.getId()), this.sbn.getTag(), Integer.valueOf(this.sbn.getScore()), this.sbn.getKey(), this.sbn.getNotification());
    }

    public void setContactAffinity(float contactAffinity) {
        this.mContactAffinity = contactAffinity;
    }

    public float getContactAffinity() {
        return this.mContactAffinity;
    }

    public void setRecentlyIntusive(boolean recentlyIntrusive) {
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

    public boolean setIntercepted(boolean intercept) {
        this.mIntercept = intercept;
        return this.mIntercept;
    }

    public boolean isIntercepted() {
        return this.mIntercept;
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

    private long calculateRankingTimeMs(long previousRankingTimeMs) {
        Notification n = getNotification();
        if (n.when == 0 || n.when > this.sbn.getPostTime()) {
            return previousRankingTimeMs <= 0 ? this.sbn.getPostTime() : previousRankingTimeMs;
        }
        return n.when;
    }

    public void setGlobalSortKey(String globalSortKey) {
        this.mGlobalSortKey = globalSortKey;
    }

    public String getGlobalSortKey() {
        return this.mGlobalSortKey;
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
}
