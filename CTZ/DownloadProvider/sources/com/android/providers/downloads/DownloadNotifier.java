package com.android.providers.downloads;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Downloads;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.IntArray;
import android.util.LongSparseLongArray;
import java.text.NumberFormat;

public class DownloadNotifier {
    private final Context mContext;
    private final NotificationManager mNotifManager;
    private final ArrayMap<String, Long> mActiveNotifs = new ArrayMap<>();
    private final LongSparseLongArray mDownloadSpeed = new LongSparseLongArray();
    private final LongSparseLongArray mDownloadTouch = new LongSparseLongArray();

    private interface UpdateQuery {
        public static final String[] PROJECTION = {"_id", "status", "visibility", "notificationpackage", "current_bytes", "total_bytes", "destination", "title", "description"};
    }

    public DownloadNotifier(Context context) {
        this.mContext = context;
        this.mNotifManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mNotifManager.createNotificationChannel(new NotificationChannel("active", context.getText(R.string.download_running), 1));
        this.mNotifManager.createNotificationChannel(new NotificationChannel("waiting", context.getText(R.string.download_queued), 3));
        this.mNotifManager.createNotificationChannel(new NotificationChannel("complete", context.getText(android.R.string.bugreport_screenshot_failure_toast), 3));
    }

    public void notifyDownloadSpeed(long j, long j2) {
        synchronized (this.mDownloadSpeed) {
            try {
                if (j2 != 0) {
                    this.mDownloadSpeed.put(j, j2);
                    this.mDownloadTouch.put(j, SystemClock.elapsedRealtime());
                } else {
                    this.mDownloadSpeed.delete(j);
                    this.mDownloadTouch.delete(j);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void update() {
        Cursor cursorQuery = this.mContext.getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, UpdateQuery.PROJECTION, "deleted == '0'", null, null);
        Throwable th = null;
        try {
            synchronized (this.mActiveNotifs) {
                updateWithLocked(cursorQuery);
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            if (cursorQuery != null) {
                if (0 != 0) {
                    try {
                        cursorQuery.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    cursorQuery.close();
                }
            }
            throw th2;
        }
    }

    private void updateWithLocked(Cursor cursor) {
        ArrayMap arrayMap;
        int i;
        Notification.Builder builder;
        long jCurrentTimeMillis;
        String str;
        IntArray intArray;
        String str2;
        String str3;
        IntArray intArray2;
        Notification notificationBuild;
        long j;
        long j2;
        long j3;
        boolean z;
        String string;
        String str4;
        Resources resources = this.mContext.getResources();
        ArrayMap arrayMap2 = new ArrayMap();
        while (cursor.moveToNext()) {
            String strBuildNotificationTag = buildNotificationTag(cursor);
            if (strBuildNotificationTag != null) {
                IntArray intArray3 = (IntArray) arrayMap2.get(strBuildNotificationTag);
                if (intArray3 == null) {
                    intArray3 = new IntArray();
                    arrayMap2.put(strBuildNotificationTag, intArray3);
                }
                intArray3.add(cursor.getPosition());
            }
        }
        int i2 = 0;
        while (i2 < arrayMap2.size()) {
            String str5 = (String) arrayMap2.keyAt(i2);
            IntArray intArray4 = (IntArray) arrayMap2.valueAt(i2);
            int notificationTagType = getNotificationTagType(str5);
            if (notificationTagType == 1) {
                builder = new Notification.Builder(this.mContext, "active");
                builder.setSmallIcon(android.R.drawable.stat_sys_download);
            } else if (notificationTagType == 2) {
                builder = new Notification.Builder(this.mContext, "waiting");
                builder.setSmallIcon(android.R.drawable.stat_sys_warning);
            } else if (notificationTagType != 3) {
                arrayMap = arrayMap2;
                i = i2;
                i2 = i + 1;
                arrayMap2 = arrayMap;
            } else {
                builder = new Notification.Builder(this.mContext, "complete");
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            }
            builder.setColor(resources.getColor(android.R.color.car_colorPrimary));
            if (this.mActiveNotifs.containsKey(str5)) {
                jCurrentTimeMillis = this.mActiveNotifs.get(str5).longValue();
            } else {
                jCurrentTimeMillis = System.currentTimeMillis();
                this.mActiveNotifs.put(str5, Long.valueOf(jCurrentTimeMillis));
            }
            builder.setWhen(jCurrentTimeMillis);
            builder.setOnlyAlertOnce(true);
            if (notificationTagType == 1 || notificationTagType == 2) {
                long[] downloadIds = getDownloadIds(cursor, intArray4);
                Intent intent = new Intent("android.intent.action.DOWNLOAD_LIST", new Uri.Builder().scheme("active-dl").appendPath(str5).build(), this.mContext, DownloadReceiver.class);
                intent.addFlags(268435456);
                intent.putExtra("extra_click_download_ids", downloadIds);
                builder.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728));
                if (notificationTagType == 1) {
                    builder.setOngoing(true);
                }
                Intent intent2 = new Intent("android.intent.action.DOWNLOAD_CANCEL", new Uri.Builder().scheme("cancel-dl").appendPath(str5).build(), this.mContext, DownloadReceiver.class);
                intent2.addFlags(268435456);
                intent2.putExtra("com.android.providers.downloads.extra.CANCELED_DOWNLOAD_IDS", downloadIds);
                intent2.putExtra("com.android.providers.downloads.extra.CANCELED_DOWNLOAD_NOTIFICATION_TAG", str5);
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, resources.getString(R.string.button_cancel_download), PendingIntent.getBroadcast(this.mContext, 0, intent2, 134217728));
            } else if (notificationTagType == 3) {
                cursor.moveToPosition(intArray4.get(0));
                long j4 = cursor.getLong(0);
                int i3 = cursor.getInt(1);
                cursor.getInt(6);
                Uri uriWithAppendedId = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j4);
                builder.setAutoCancel(true);
                if (Downloads.Impl.isStatusError(i3)) {
                    str4 = "android.intent.action.DOWNLOAD_LIST";
                } else {
                    str4 = "android.intent.action.DOWNLOAD_OPEN";
                }
                Intent intent3 = new Intent(str4, uriWithAppendedId, this.mContext, DownloadReceiver.class);
                intent3.addFlags(268435456);
                intent3.putExtra("extra_click_download_ids", getDownloadIds(cursor, intArray4));
                builder.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent3, 134217728));
                Intent intent4 = new Intent("android.intent.action.DOWNLOAD_HIDE", uriWithAppendedId, this.mContext, DownloadReceiver.class);
                intent4.addFlags(268435456);
                builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, intent4, 0));
            }
            if (notificationTagType == 1) {
                synchronized (this.mDownloadSpeed) {
                    i = i2;
                    j = 0;
                    int i4 = 0;
                    j2 = 0;
                    j3 = 0;
                    while (i4 < intArray4.size()) {
                        cursor.moveToPosition(intArray4.get(i4));
                        String str6 = str5;
                        IntArray intArray5 = intArray4;
                        long j5 = cursor.getLong(0);
                        long j6 = cursor.getLong(4);
                        ArrayMap arrayMap3 = arrayMap2;
                        long j7 = cursor.getLong(5);
                        if (j7 != -1) {
                            j += j6;
                            j2 += j7;
                            j3 += this.mDownloadSpeed.get(j5);
                        }
                        i4++;
                        str5 = str6;
                        intArray4 = intArray5;
                        arrayMap2 = arrayMap3;
                    }
                    arrayMap = arrayMap2;
                    str = str5;
                    intArray = intArray4;
                }
                if (j2 > 0) {
                    String str7 = NumberFormat.getPercentInstance().format(j / j2);
                    if (j3 > 0) {
                        z = false;
                        string = resources.getString(R.string.download_remaining, DateUtils.formatDuration(((j2 - j) * 1000) / j3));
                    } else {
                        z = false;
                        string = null;
                    }
                    builder.setProgress(100, (int) ((j * 100) / j2), z);
                    str3 = str7;
                    str2 = string;
                    intArray2 = intArray;
                    if (intArray2.size() != 1) {
                        cursor.moveToPosition(intArray2.get(0));
                        builder.setContentTitle(getDownloadTitle(resources, cursor));
                        if (notificationTagType == 1) {
                            String string2 = cursor.getString(8);
                            if (!TextUtils.isEmpty(string2)) {
                                builder.setContentText(string2);
                            } else {
                                builder.setContentText(str2);
                            }
                            builder.setContentInfo(str3);
                        } else if (notificationTagType == 2) {
                            builder.setContentText(resources.getString(R.string.notification_need_wifi_for_size));
                        } else if (notificationTagType == 3) {
                            int i5 = cursor.getInt(1);
                            if (Downloads.Impl.isStatusError(i5)) {
                                builder.setContentText(resources.getText(R.string.notification_download_failed));
                            } else if (Downloads.Impl.isStatusSuccess(i5)) {
                                builder.setContentText(resources.getText(R.string.notification_download_complete));
                            }
                        }
                        notificationBuild = builder.build();
                    } else {
                        Notification.InboxStyle inboxStyle = new Notification.InboxStyle(builder);
                        for (int i6 = 0; i6 < intArray2.size(); i6++) {
                            cursor.moveToPosition(intArray2.get(i6));
                            inboxStyle.addLine(getDownloadTitle(resources, cursor));
                        }
                        if (notificationTagType == 1) {
                            builder.setContentTitle(resources.getQuantityString(R.plurals.notif_summary_active, intArray2.size(), Integer.valueOf(intArray2.size())));
                            builder.setContentText(str2);
                            builder.setContentInfo(str3);
                            inboxStyle.setSummaryText(str2);
                        } else if (notificationTagType == 2) {
                            builder.setContentTitle(resources.getQuantityString(R.plurals.notif_summary_waiting, intArray2.size(), Integer.valueOf(intArray2.size())));
                            builder.setContentText(resources.getString(R.string.notification_need_wifi_for_size));
                            inboxStyle.setSummaryText(resources.getString(R.string.notification_need_wifi_for_size));
                        }
                        notificationBuild = inboxStyle.build();
                    }
                    this.mNotifManager.notify(str, 0, notificationBuild);
                    i2 = i + 1;
                    arrayMap2 = arrayMap;
                } else {
                    builder.setProgress(100, 0, true);
                }
            } else {
                arrayMap = arrayMap2;
                i = i2;
                str = str5;
                intArray = intArray4;
            }
            str2 = null;
            str3 = null;
            intArray2 = intArray;
            if (intArray2.size() != 1) {
            }
            this.mNotifManager.notify(str, 0, notificationBuild);
            i2 = i + 1;
            arrayMap2 = arrayMap;
        }
        ArrayMap arrayMap4 = arrayMap2;
        int i7 = 0;
        while (i7 < this.mActiveNotifs.size()) {
            String strKeyAt = this.mActiveNotifs.keyAt(i7);
            ArrayMap arrayMap5 = arrayMap4;
            if (arrayMap5.containsKey(strKeyAt)) {
                i7++;
            } else {
                this.mNotifManager.cancel(strKeyAt, 0);
                this.mActiveNotifs.removeAt(i7);
            }
            arrayMap4 = arrayMap5;
        }
    }

    private static CharSequence getDownloadTitle(Resources resources, Cursor cursor) {
        String string = cursor.getString(7);
        if (!TextUtils.isEmpty(string)) {
            return string;
        }
        return resources.getString(R.string.download_unknown_title);
    }

    private long[] getDownloadIds(Cursor cursor, IntArray intArray) {
        long[] jArr = new long[intArray.size()];
        for (int i = 0; i < intArray.size(); i++) {
            cursor.moveToPosition(intArray.get(i));
            jArr[i] = cursor.getLong(0);
        }
        return jArr;
    }

    private static String buildNotificationTag(Cursor cursor) {
        long j = cursor.getLong(0);
        int i = cursor.getInt(1);
        int i2 = cursor.getInt(2);
        String string = cursor.getString(3);
        if (isQueuedAndVisible(i, i2)) {
            return "2:" + string;
        }
        if (isActiveAndVisible(i, i2)) {
            return "1:" + string;
        }
        if (isCompleteAndVisible(i, i2)) {
            return "3:" + j;
        }
        return null;
    }

    private static int getNotificationTagType(String str) {
        return Integer.parseInt(str.substring(0, str.indexOf(58)));
    }

    private static boolean isQueuedAndVisible(int i, int i2) {
        return i == 196 && (i2 == 0 || i2 == 1);
    }

    private static boolean isActiveAndVisible(int i, int i2) {
        return i == 192 && (i2 == 0 || i2 == 1);
    }

    private static boolean isCompleteAndVisible(int i, int i2) {
        return Downloads.Impl.isStatusCompleted(i) && (i2 == 1 || i2 == 3);
    }
}
