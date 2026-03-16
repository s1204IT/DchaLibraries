package com.android.providers.downloads;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Downloads;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LongSparseLongArray;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class DownloadNotifier {
    private final Context mContext;
    private final NotificationManager mNotifManager;
    private final HashMap<String, Long> mActiveNotifs = Maps.newHashMap();
    private final LongSparseLongArray mDownloadSpeed = new LongSparseLongArray();
    private final LongSparseLongArray mDownloadTouch = new LongSparseLongArray();

    public DownloadNotifier(Context context) {
        this.mContext = context;
        this.mNotifManager = (NotificationManager) context.getSystemService("notification");
    }

    public void cancelAll() {
        this.mNotifManager.cancelAll();
    }

    public void notifyDownloadSpeed(long id, long bytesPerSecond) {
        synchronized (this.mDownloadSpeed) {
            if (bytesPerSecond != 0) {
                this.mDownloadSpeed.put(id, bytesPerSecond);
                this.mDownloadTouch.put(id, SystemClock.elapsedRealtime());
            } else {
                this.mDownloadSpeed.delete(id);
                this.mDownloadTouch.delete(id);
            }
        }
    }

    public void updateWith(Collection<DownloadInfo> downloads) {
        synchronized (this.mActiveNotifs) {
            updateWithLocked(downloads);
        }
    }

    private void updateWithLocked(Collection<DownloadInfo> downloads) {
        long firstShown;
        Notification notif;
        String action;
        Resources res = this.mContext.getResources();
        Multimap<String, DownloadInfo> clustered = ArrayListMultimap.create();
        for (DownloadInfo info : downloads) {
            String tag = buildNotificationTag(info);
            if (tag != null) {
                clustered.put(tag, info);
            }
        }
        for (String tag2 : clustered.keySet()) {
            int type = getNotificationTagType(tag2);
            Collection<DownloadInfo> cluster = clustered.get(tag2);
            Notification.Builder builder = new Notification.Builder(this.mContext);
            builder.setColor(res.getColor(android.R.color.system_accent3_600));
            if (this.mActiveNotifs.containsKey(tag2)) {
                firstShown = this.mActiveNotifs.get(tag2).longValue();
            } else {
                firstShown = System.currentTimeMillis();
                this.mActiveNotifs.put(tag2, Long.valueOf(firstShown));
            }
            builder.setWhen(firstShown);
            if (type == 1) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download);
            } else if (type == 2) {
                builder.setSmallIcon(android.R.drawable.stat_sys_warning);
            } else if (type == 3) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            }
            if (type == 1 || type == 2) {
                Intent intent = new Intent("android.intent.action.DOWNLOAD_LIST", new Uri.Builder().scheme("active-dl").appendPath(tag2).build(), this.mContext, DownloadReceiver.class);
                intent.putExtra("extra_click_download_ids", getDownloadIds(cluster));
                builder.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728));
                builder.setOngoing(true);
            } else if (type == 3) {
                DownloadInfo info2 = cluster.iterator().next();
                Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, info2.mId);
                builder.setAutoCancel(true);
                if (!Downloads.Impl.isStatusError(info2.mStatus) && info2.mDestination != 5) {
                    action = "android.intent.action.DOWNLOAD_OPEN";
                } else {
                    action = "android.intent.action.DOWNLOAD_LIST";
                }
                Intent intent2 = new Intent(action, uri, this.mContext, DownloadReceiver.class);
                intent2.putExtra("extra_click_download_ids", getDownloadIds(cluster));
                builder.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent2, 134217728));
                Intent hideIntent = new Intent("android.intent.action.DOWNLOAD_HIDE", uri, this.mContext, DownloadReceiver.class);
                builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, hideIntent, 0));
            }
            String remainingText = null;
            String percentText = null;
            if (type == 1) {
                long current = 0;
                long total = 0;
                long speed = 0;
                synchronized (this.mDownloadSpeed) {
                    for (DownloadInfo info3 : cluster) {
                        if (info3.mTotalBytes != -1) {
                            current += info3.mCurrentBytes;
                            total += info3.mTotalBytes;
                            speed += this.mDownloadSpeed.get(info3.mId);
                        }
                    }
                }
                if (total > 0) {
                    percentText = NumberFormat.getPercentInstance().format(current / total);
                    if (speed > 0) {
                        long remainingMillis = ((total - current) * 1000) / speed;
                        remainingText = res.getString(R.string.download_remaining, DateUtils.formatDuration(remainingMillis));
                    }
                    int percent = (int) ((100 * current) / total);
                    builder.setProgress(100, percent, false);
                } else {
                    builder.setProgress(100, 0, true);
                }
            }
            if (cluster.size() == 1) {
                DownloadInfo info4 = cluster.iterator().next();
                builder.setContentTitle(getDownloadTitle(res, info4));
                if (type == 1) {
                    if (!TextUtils.isEmpty(info4.mDescription)) {
                        builder.setContentText(info4.mDescription);
                    } else {
                        builder.setContentText(remainingText);
                    }
                    builder.setContentInfo(percentText);
                } else if (type == 2) {
                    builder.setContentText(res.getString(R.string.notification_need_wifi_for_size));
                } else if (type == 3) {
                    if (Downloads.Impl.isStatusError(info4.mStatus)) {
                        builder.setContentText(res.getText(R.string.notification_download_failed));
                    } else if (Downloads.Impl.isStatusSuccess(info4.mStatus)) {
                        builder.setContentText(res.getText(R.string.notification_download_complete));
                    }
                }
                notif = builder.build();
            } else {
                Notification.InboxStyle inboxStyle = new Notification.InboxStyle(builder);
                Iterator<DownloadInfo> it = cluster.iterator();
                while (it.hasNext()) {
                    inboxStyle.addLine(getDownloadTitle(res, it.next()));
                }
                if (type == 1) {
                    builder.setContentTitle(res.getQuantityString(R.plurals.notif_summary_active, cluster.size(), Integer.valueOf(cluster.size())));
                    builder.setContentText(remainingText);
                    builder.setContentInfo(percentText);
                    inboxStyle.setSummaryText(remainingText);
                } else if (type == 2) {
                    builder.setContentTitle(res.getQuantityString(R.plurals.notif_summary_waiting, cluster.size(), Integer.valueOf(cluster.size())));
                    builder.setContentText(res.getString(R.string.notification_need_wifi_for_size));
                    inboxStyle.setSummaryText(res.getString(R.string.notification_need_wifi_for_size));
                }
                notif = inboxStyle.build();
            }
            this.mNotifManager.notify(tag2, 0, notif);
        }
        Iterator<String> it2 = this.mActiveNotifs.keySet().iterator();
        while (it2.hasNext()) {
            String tag3 = it2.next();
            if (!clustered.containsKey(tag3)) {
                this.mNotifManager.cancel(tag3, 0);
                it2.remove();
            }
        }
    }

    private static CharSequence getDownloadTitle(Resources res, DownloadInfo info) {
        return !TextUtils.isEmpty(info.mTitle) ? info.mTitle : res.getString(R.string.download_unknown_title);
    }

    private long[] getDownloadIds(Collection<DownloadInfo> infos) {
        long[] ids = new long[infos.size()];
        int i = 0;
        for (DownloadInfo info : infos) {
            ids[i] = info.mId;
            i++;
        }
        return ids;
    }

    public void dumpSpeeds() {
        synchronized (this.mDownloadSpeed) {
            for (int i = 0; i < this.mDownloadSpeed.size(); i++) {
                long id = this.mDownloadSpeed.keyAt(i);
                long delta = SystemClock.elapsedRealtime() - this.mDownloadTouch.get(id);
                Log.d("DownloadManager", "Download " + id + " speed " + this.mDownloadSpeed.valueAt(i) + "bps, " + delta + "ms ago");
            }
        }
    }

    private static String buildNotificationTag(DownloadInfo info) {
        if (info.mStatus == 196) {
            return "2:" + info.mPackage;
        }
        if (isActiveAndVisible(info)) {
            return "1:" + info.mPackage;
        }
        if (isCompleteAndVisible(info)) {
            return "3:" + info.mId;
        }
        return null;
    }

    private static int getNotificationTagType(String tag) {
        return Integer.parseInt(tag.substring(0, tag.indexOf(58)));
    }

    private static boolean isActiveAndVisible(DownloadInfo download) {
        return download.mStatus == 192 && (download.mVisibility == 0 || download.mVisibility == 1);
    }

    private static boolean isCompleteAndVisible(DownloadInfo download) {
        return Downloads.Impl.isStatusCompleted(download.mStatus) && (download.mVisibility == 1 || download.mVisibility == 3);
    }
}
