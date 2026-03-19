package com.android.server.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SnoozeHelper {
    private static final String EXTRA_KEY = "key";
    private static final String EXTRA_USER_ID = "userId";
    private static final String INDENT = "    ";
    private static final String REPOST_SCHEME = "repost";
    private static final int REQUEST_CODE_REPOST = 1;
    private AlarmManager mAm;
    private Callback mCallback;
    private final Context mContext;
    private final ManagedServices.UserProfiles mUserProfiles;
    private static final String TAG = "SnoozeHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String REPOST_ACTION = SnoozeHelper.class.getSimpleName() + ".EVALUATE";
    private ArrayMap<Integer, ArrayMap<String, ArrayMap<String, NotificationRecord>>> mSnoozedNotifications = new ArrayMap<>();
    private ArrayMap<String, String> mPackages = new ArrayMap<>();
    private ArrayMap<String, Integer> mUsers = new ArrayMap<>();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SnoozeHelper.DEBUG) {
                Slog.d(SnoozeHelper.TAG, "Reposting notification");
            }
            if (SnoozeHelper.REPOST_ACTION.equals(intent.getAction())) {
                SnoozeHelper.this.repost(intent.getStringExtra(SnoozeHelper.EXTRA_KEY), intent.getIntExtra(SnoozeHelper.EXTRA_USER_ID, 0));
            }
        }
    };

    protected interface Callback {
        void repost(int i, NotificationRecord notificationRecord);
    }

    public SnoozeHelper(Context context, Callback callback, ManagedServices.UserProfiles userProfiles) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter(REPOST_ACTION);
        intentFilter.addDataScheme(REPOST_SCHEME);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mAm = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mCallback = callback;
        this.mUserProfiles = userProfiles;
    }

    protected boolean isSnoozed(int i, String str, String str2) {
        return this.mSnoozedNotifications.containsKey(Integer.valueOf(i)) && this.mSnoozedNotifications.get(Integer.valueOf(i)).containsKey(str) && this.mSnoozedNotifications.get(Integer.valueOf(i)).get(str).containsKey(str2);
    }

    protected Collection<NotificationRecord> getSnoozed(int i, String str) {
        if (this.mSnoozedNotifications.containsKey(Integer.valueOf(i)) && this.mSnoozedNotifications.get(Integer.valueOf(i)).containsKey(str)) {
            return this.mSnoozedNotifications.get(Integer.valueOf(i)).get(str).values();
        }
        return Collections.EMPTY_LIST;
    }

    protected List<NotificationRecord> getSnoozed() {
        ArrayList arrayList = new ArrayList();
        int[] currentProfileIds = this.mUserProfiles.getCurrentProfileIds();
        if (currentProfileIds != null) {
            for (int i : currentProfileIds) {
                ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(i));
                if (arrayMap != null) {
                    int size = arrayMap.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        ArrayMap<String, NotificationRecord> arrayMapValueAt = arrayMap.valueAt(i2);
                        if (arrayMapValueAt != null) {
                            arrayList.addAll(arrayMapValueAt.values());
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    protected void snooze(NotificationRecord notificationRecord, long j) {
        snooze(notificationRecord);
        scheduleRepost(notificationRecord.sbn.getPackageName(), notificationRecord.getKey(), notificationRecord.getUserId(), j);
    }

    protected void snooze(NotificationRecord notificationRecord) {
        int identifier = notificationRecord.getUser().getIdentifier();
        if (DEBUG) {
            Slog.d(TAG, "Snoozing " + notificationRecord.getKey());
        }
        ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(identifier));
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
        }
        ArrayMap<String, NotificationRecord> arrayMap2 = arrayMap.get(notificationRecord.sbn.getPackageName());
        if (arrayMap2 == null) {
            arrayMap2 = new ArrayMap<>();
        }
        arrayMap2.put(notificationRecord.getKey(), notificationRecord);
        arrayMap.put(notificationRecord.sbn.getPackageName(), arrayMap2);
        this.mSnoozedNotifications.put(Integer.valueOf(identifier), arrayMap);
        this.mPackages.put(notificationRecord.getKey(), notificationRecord.sbn.getPackageName());
        this.mUsers.put(notificationRecord.getKey(), Integer.valueOf(identifier));
    }

    protected boolean cancel(int i, String str, String str2, int i2) {
        ArrayMap<String, NotificationRecord> arrayMap;
        if (this.mSnoozedNotifications.containsKey(Integer.valueOf(i)) && (arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(i)).get(str)) != null) {
            for (Map.Entry<String, NotificationRecord> entry : arrayMap.entrySet()) {
                StatusBarNotification statusBarNotification = entry.getValue().sbn;
                if (Objects.equals(statusBarNotification.getTag(), str2) && statusBarNotification.getId() == i2) {
                    entry.getValue().isCanceled = true;
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    protected boolean cancel(int i, boolean z) {
        int[] currentProfileIds = {i};
        if (z) {
            currentProfileIds = this.mUserProfiles.getCurrentProfileIds();
        }
        for (int i2 : currentProfileIds) {
            ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(i2));
            if (arrayMap != null) {
                int size = arrayMap.size();
                for (int i3 = 0; i3 < size; i3++) {
                    ArrayMap<String, NotificationRecord> arrayMapValueAt = arrayMap.valueAt(i3);
                    if (arrayMapValueAt != null) {
                        int size2 = arrayMapValueAt.size();
                        for (int i4 = 0; i4 < size2; i4++) {
                            arrayMapValueAt.valueAt(i4).isCanceled = true;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    protected boolean cancel(int i, String str) {
        if (!this.mSnoozedNotifications.containsKey(Integer.valueOf(i)) || !this.mSnoozedNotifications.get(Integer.valueOf(i)).containsKey(str)) {
            return false;
        }
        ArrayMap<String, NotificationRecord> arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(i)).get(str);
        int size = arrayMap.size();
        for (int i2 = 0; i2 < size; i2++) {
            arrayMap.valueAt(i2).isCanceled = true;
        }
        return true;
    }

    protected void update(int i, NotificationRecord notificationRecord) {
        ArrayMap<String, NotificationRecord> arrayMap;
        ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap2 = this.mSnoozedNotifications.get(Integer.valueOf(i));
        if (arrayMap2 == null || (arrayMap = arrayMap2.get(notificationRecord.sbn.getPackageName())) == null) {
            return;
        }
        NotificationRecord notificationRecord2 = arrayMap.get(notificationRecord.getKey());
        if (notificationRecord2 != null && notificationRecord2.isCanceled) {
            return;
        }
        arrayMap.put(notificationRecord.getKey(), notificationRecord);
    }

    protected void repost(String str) {
        Integer num = this.mUsers.get(str);
        if (num != null) {
            repost(str, num.intValue());
        }
    }

    protected void repost(String str, int i) {
        ArrayMap<String, NotificationRecord> arrayMap;
        String strRemove = this.mPackages.remove(str);
        ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap2 = this.mSnoozedNotifications.get(Integer.valueOf(i));
        if (arrayMap2 == null || (arrayMap = arrayMap2.get(strRemove)) == null) {
            return;
        }
        NotificationRecord notificationRecordRemove = arrayMap.remove(str);
        this.mPackages.remove(str);
        this.mUsers.remove(str);
        if (notificationRecordRemove != null && !notificationRecordRemove.isCanceled) {
            MetricsLogger.action(notificationRecordRemove.getLogMaker().setCategory(831).setType(1));
            this.mCallback.repost(i, notificationRecordRemove);
        }
    }

    protected void repostGroupSummary(String str, int i, String str2) {
        ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap;
        ArrayMap<String, NotificationRecord> arrayMap2;
        if (this.mSnoozedNotifications.containsKey(Integer.valueOf(i)) && (arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(i))) != null && arrayMap.containsKey(str) && (arrayMap2 = arrayMap.get(str)) != null) {
            String key = null;
            int size = arrayMap2.size();
            int i2 = 0;
            while (true) {
                if (i2 >= size) {
                    break;
                }
                NotificationRecord notificationRecordValueAt = arrayMap2.valueAt(i2);
                if (!notificationRecordValueAt.sbn.isGroup() || !notificationRecordValueAt.getNotification().isGroupSummary() || !str2.equals(notificationRecordValueAt.getGroupKey())) {
                    i2++;
                } else {
                    key = notificationRecordValueAt.getKey();
                    break;
                }
            }
            if (key != null) {
                NotificationRecord notificationRecordRemove = arrayMap2.remove(key);
                this.mPackages.remove(key);
                this.mUsers.remove(key);
                if (notificationRecordRemove != null && !notificationRecordRemove.isCanceled) {
                    MetricsLogger.action(notificationRecordRemove.getLogMaker().setCategory(831).setType(1));
                    this.mCallback.repost(i, notificationRecordRemove);
                }
            }
        }
    }

    private PendingIntent createPendingIntent(String str, String str2, int i) {
        return PendingIntent.getBroadcast(this.mContext, 1, new Intent(REPOST_ACTION).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).setData(new Uri.Builder().scheme(REPOST_SCHEME).appendPath(str2).build()).addFlags(268435456).putExtra(EXTRA_KEY, str2).putExtra(EXTRA_USER_ID, i), 134217728);
    }

    private void scheduleRepost(String str, String str2, int i, long j) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            PendingIntent pendingIntentCreatePendingIntent = createPendingIntent(str, str2, i);
            this.mAm.cancel(pendingIntentCreatePendingIntent);
            long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
            if (DEBUG) {
                Slog.d(TAG, "Scheduling evaluate for " + new Date(jElapsedRealtime));
            }
            this.mAm.setExactAndAllowWhileIdle(2, jElapsedRealtime, pendingIntentCreatePendingIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void dump(PrintWriter printWriter, NotificationManagerService.DumpFilter dumpFilter) {
        printWriter.println("\n  Snoozed notifications:");
        Iterator<Integer> it = this.mSnoozedNotifications.keySet().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            printWriter.print(INDENT);
            printWriter.println("user: " + iIntValue);
            ArrayMap<String, ArrayMap<String, NotificationRecord>> arrayMap = this.mSnoozedNotifications.get(Integer.valueOf(iIntValue));
            for (String str : arrayMap.keySet()) {
                printWriter.print(INDENT);
                printWriter.print(INDENT);
                printWriter.println("package: " + str);
                for (String str2 : arrayMap.get(str).keySet()) {
                    printWriter.print(INDENT);
                    printWriter.print(INDENT);
                    printWriter.print(INDENT);
                    printWriter.println(str2);
                }
            }
        }
    }

    protected void writeXml(XmlSerializer xmlSerializer, boolean z) throws IOException {
    }

    public void readXml(XmlPullParser xmlPullParser, boolean z) throws XmlPullParserException, IOException {
    }

    @VisibleForTesting
    void setAlarmManager(AlarmManager alarmManager) {
        this.mAm = alarmManager;
    }
}
