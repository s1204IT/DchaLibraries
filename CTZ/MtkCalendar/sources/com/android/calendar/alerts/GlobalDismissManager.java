package com.android.calendar.alerts;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Pair;
import com.android.calendar.CloudNotificationBackplane;
import com.android.calendar.ExtensionsFactory;
import com.android.calendar.R;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlobalDismissManager extends BroadcastReceiver {
    static final String[] EVENT_PROJECTION = {"_id", "calendar_id"};
    static final String[] EVENT_SYNC_PROJECTION = {"_id", "_sync_id"};
    static final String[] CALENDARS_PROJECTION = {"_id", "account_name", "account_type"};
    private static HashMap<GlobalDismissId, Long> sReceiverDismissCache = new HashMap<>();
    private static HashMap<LocalDismissId, Long> sSenderDismissCache = new HashMap<>();

    private static class GlobalDismissId {
        public final String mAccountName;
        public final long mStartTime;
        public final String mSyncId;

        private GlobalDismissId(String str, String str2, long j) {
            if (str == null) {
                throw new IllegalArgumentException("Account Name can not be set to null");
            }
            if (str2 == null) {
                throw new IllegalArgumentException("SyncId can not be set to null");
            }
            this.mAccountName = str;
            this.mSyncId = str2;
            this.mStartTime = j;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            GlobalDismissId globalDismissId = (GlobalDismissId) obj;
            if (this.mStartTime == globalDismissId.mStartTime && this.mAccountName.equals(globalDismissId.mAccountName) && this.mSyncId.equals(globalDismissId.mSyncId)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((this.mAccountName.hashCode() * 31) + this.mSyncId.hashCode())) + ((int) (this.mStartTime ^ (this.mStartTime >>> 32)));
        }
    }

    public static class LocalDismissId {
        public final String mAccountName;
        public final String mAccountType;
        public final long mEventId;
        public final long mStartTime;

        public LocalDismissId(String str, String str2, long j, long j2) {
            if (str == null) {
                throw new IllegalArgumentException("Account Type can not be null");
            }
            if (str2 == null) {
                throw new IllegalArgumentException("Account Name can not be null");
            }
            this.mAccountType = str;
            this.mAccountName = str2;
            this.mEventId = j;
            this.mStartTime = j2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LocalDismissId localDismissId = (LocalDismissId) obj;
            if (this.mEventId == localDismissId.mEventId && this.mStartTime == localDismissId.mStartTime && this.mAccountName.equals(localDismissId.mAccountName) && this.mAccountType.equals(localDismissId.mAccountType)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((((this.mAccountType.hashCode() * 31) + this.mAccountName.hashCode()) * 31) + ((int) (this.mEventId ^ (this.mEventId >>> 32))))) + ((int) (this.mStartTime ^ (this.mStartTime >>> 32)));
        }
    }

    public static class AlarmId {
        public long mEventId;
        public long mStart;

        public AlarmId(long j, long j2) {
            this.mEventId = j;
            this.mStart = j2;
        }
    }

    public static void processEventIds(Context context, Set<Long> set) {
        String string = context.getResources().getString(R.string.notification_sender_id);
        if (string == null || string.isEmpty()) {
            Log.i("GlobalDismissManager", "no sender configured");
            return;
        }
        Map<Long, Long> mapLookupEventToCalendarMap = lookupEventToCalendarMap(context, set);
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        linkedHashSet.addAll(mapLookupEventToCalendarMap.values());
        if (linkedHashSet.isEmpty()) {
            Log.d("GlobalDismissManager", "found no calendars for events");
            return;
        }
        Map<Long, Pair<String, String>> mapLookupCalendarToAccountMap = lookupCalendarToAccountMap(context, linkedHashSet);
        if (mapLookupCalendarToAccountMap.isEmpty()) {
            Log.d("GlobalDismissManager", "found no accounts for calendars");
            return;
        }
        LinkedHashSet<String> linkedHashSet2 = new LinkedHashSet();
        for (Pair<String, String> pair : mapLookupCalendarToAccountMap.values()) {
            if ("com.google".equals(pair.first)) {
                linkedHashSet2.add((String) pair.second);
            }
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.android.calendar.alerts.GDM", 0);
        Set<String> stringSet = sharedPreferences.getStringSet("known_accounts", new HashSet());
        linkedHashSet2.removeAll(stringSet);
        if (linkedHashSet2.isEmpty()) {
            return;
        }
        CloudNotificationBackplane cloudNotificationBackplane = ExtensionsFactory.getCloudNotificationBackplane();
        if (cloudNotificationBackplane.open(context)) {
            for (String str : linkedHashSet2) {
                try {
                    if (cloudNotificationBackplane.subscribeToGroup(string, str, str)) {
                        stringSet.add(str);
                    }
                } catch (IOException e) {
                }
            }
            cloudNotificationBackplane.close();
            sharedPreferences.edit().putStringSet("known_accounts", stringSet).commit();
        }
    }

    public static void dismissGlobally(Context context, List<AlarmId> list) {
        HashSet hashSet = new HashSet(list.size());
        Iterator<AlarmId> it = list.iterator();
        while (it.hasNext()) {
            hashSet.add(Long.valueOf(it.next().mEventId));
        }
        Map<Long, Long> mapLookupEventToCalendarMap = lookupEventToCalendarMap(context, hashSet);
        if (mapLookupEventToCalendarMap.isEmpty()) {
            Log.d("GlobalDismissManager", "found no calendars for events");
            return;
        }
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        linkedHashSet.addAll(mapLookupEventToCalendarMap.values());
        Map<Long, Pair<String, String>> mapLookupCalendarToAccountMap = lookupCalendarToAccountMap(context, linkedHashSet);
        if (mapLookupCalendarToAccountMap.isEmpty()) {
            Log.d("GlobalDismissManager", "found no accounts for calendars");
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        for (AlarmId alarmId : list) {
            Pair<String, String> pair = mapLookupCalendarToAccountMap.get(mapLookupEventToCalendarMap.get(Long.valueOf(alarmId.mEventId)));
            if ("com.google".equals(pair.first)) {
                LocalDismissId localDismissId = new LocalDismissId((String) pair.first, (String) pair.second, alarmId.mEventId, alarmId.mStart);
                synchronized (sSenderDismissCache) {
                    sSenderDismissCache.put(localDismissId, Long.valueOf(jCurrentTimeMillis));
                }
            }
        }
        syncSenderDismissCache(context);
    }

    public static void syncSenderDismissCache(Context context) {
        int i;
        if ("".equals(context.getResources().getString(R.string.notification_sender_id))) {
            Log.i("GlobalDismissManager", "no sender configured");
            return;
        }
        CloudNotificationBackplane cloudNotificationBackplane = ExtensionsFactory.getCloudNotificationBackplane();
        if (!cloudNotificationBackplane.open(context)) {
            Log.i("GlobalDismissManager", "Unable to open cloud notification backplane");
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        ContentResolver contentResolver = context.getContentResolver();
        synchronized (sSenderDismissCache) {
            Iterator<Map.Entry<LocalDismissId, Long>> it = sSenderDismissCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<LocalDismissId, Long> next = it.next();
                LocalDismissId key = next.getKey();
                Cursor cursorQuery = contentResolver.query(asSync(CalendarContract.Events.CONTENT_URI, key.mAccountType, key.mAccountName), EVENT_SYNC_PROJECTION, "_id = " + key.mEventId, null, null);
                try {
                    cursorQuery.moveToPosition(-1);
                    int columnIndex = cursorQuery.getColumnIndex("_sync_id");
                    if (columnIndex != -1) {
                        while (cursorQuery.moveToNext()) {
                            String string = cursorQuery.getString(columnIndex);
                            if (string != null) {
                                Bundle bundle = new Bundle();
                                long j = key.mStartTime;
                                String str = key.mAccountName;
                                bundle.putString("com.android.calendar.alerts.sync_id", string);
                                i = columnIndex;
                                bundle.putString("com.android.calendar.alerts.start_time", Long.toString(j));
                                bundle.putString("com.android.calendar.alerts.account_name", str);
                                try {
                                    cloudNotificationBackplane.send(str, string + ":" + j, bundle);
                                    it.remove();
                                } catch (IOException e) {
                                }
                            } else {
                                i = columnIndex;
                            }
                            columnIndex = i;
                        }
                    }
                    cursorQuery.close();
                    if (jCurrentTimeMillis - next.getValue().longValue() > 3600000) {
                        it.remove();
                    }
                } catch (Throwable th) {
                    cursorQuery.close();
                    throw th;
                }
            }
        }
        cloudNotificationBackplane.close();
    }

    private static Uri asSync(Uri uri, String str, String str2) {
        return uri.buildUpon().appendQueryParameter("caller_is_syncadapter", "true").appendQueryParameter("account_name", str2).appendQueryParameter("account_type", str).build();
    }

    private static String buildMultipleIdQuery(Set<Long> set, String str) {
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        for (Long l : set) {
            if (z) {
                z = false;
            } else {
                sb.append(" OR ");
            }
            sb.append(str);
            sb.append("=");
            sb.append(l);
        }
        return sb.toString();
    }

    private static Map<Long, Long> lookupEventToCalendarMap(Context context, Set<Long> set) {
        HashMap map = new HashMap();
        Cursor cursorQuery = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, EVENT_PROJECTION, buildMultipleIdQuery(set, "_id"), null, null);
        try {
            cursorQuery.moveToPosition(-1);
            int columnIndex = cursorQuery.getColumnIndex("calendar_id");
            int columnIndex2 = cursorQuery.getColumnIndex("_id");
            if (columnIndex != -1 && columnIndex2 != -1) {
                while (cursorQuery.moveToNext()) {
                    map.put(Long.valueOf(cursorQuery.getLong(columnIndex2)), Long.valueOf(cursorQuery.getLong(columnIndex)));
                }
            }
            return map;
        } finally {
            cursorQuery.close();
        }
    }

    private static Map<Long, Pair<String, String>> lookupCalendarToAccountMap(Context context, Set<Long> set) {
        HashMap map = new HashMap();
        Cursor cursorQuery = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, CALENDARS_PROJECTION, buildMultipleIdQuery(set, "_id"), null, null);
        try {
            cursorQuery.moveToPosition(-1);
            int columnIndex = cursorQuery.getColumnIndex("_id");
            int columnIndex2 = cursorQuery.getColumnIndex("account_name");
            int columnIndex3 = cursorQuery.getColumnIndex("account_type");
            if (columnIndex != -1 && columnIndex2 != -1 && columnIndex3 != -1) {
                while (cursorQuery.moveToNext()) {
                    Long lValueOf = Long.valueOf(cursorQuery.getLong(columnIndex));
                    String string = cursorQuery.getString(columnIndex2);
                    String string2 = cursorQuery.getString(columnIndex3);
                    if (string != null && string2 != null) {
                        map.put(lValueOf, new Pair(string2, string));
                    }
                }
            }
            return map;
        } finally {
            cursorQuery.close();
        }
    }

    public static void syncReceiverDismissCache(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (sReceiverDismissCache) {
            Iterator<Map.Entry<GlobalDismissId, Long>> it = sReceiverDismissCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<GlobalDismissId, Long> next = it.next();
                GlobalDismissId key = next.getKey();
                Cursor cursorQuery = contentResolver.query(asSync(CalendarContract.Events.CONTENT_URI, "com.google", key.mAccountName), EVENT_SYNC_PROJECTION, "_sync_id = '" + key.mSyncId + "'", null, null);
                try {
                    int columnIndex = cursorQuery.getColumnIndex("_id");
                    cursorQuery.moveToFirst();
                    if (columnIndex != -1 && !cursorQuery.isAfterLast()) {
                        long j = cursorQuery.getLong(columnIndex);
                        ContentValues contentValues = new ContentValues();
                        String str = "(state=1 OR state=0) AND event_id=" + j + " AND begin=" + key.mStartTime;
                        contentValues.put("state", (Integer) 2);
                        if (contentResolver.update(CalendarContract.CalendarAlerts.CONTENT_URI, contentValues, str, null) > 0) {
                            it.remove();
                        }
                    }
                    cursorQuery.close();
                    if (jCurrentTimeMillis - next.getValue().longValue() > 3600000) {
                        it.remove();
                    }
                } catch (Throwable th) {
                    cursorQuery.close();
                    throw th;
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        new AsyncTask<Pair<Context, Intent>, Void, Void>() {
            @Override
            protected Void doInBackground(Pair<Context, Intent>... pairArr) {
                Context context2 = (Context) pairArr[0].first;
                Intent intent2 = (Intent) pairArr[0].second;
                if (intent2.hasExtra("com.android.calendar.alerts.sync_id") && intent2.hasExtra("com.android.calendar.alerts.account_name") && intent2.hasExtra("com.android.calendar.alerts.start_time")) {
                    synchronized (GlobalDismissManager.sReceiverDismissCache) {
                        GlobalDismissManager.sReceiverDismissCache.put(new GlobalDismissId(intent2.getStringExtra("com.android.calendar.alerts.account_name"), intent2.getStringExtra("com.android.calendar.alerts.sync_id"), Long.parseLong(intent2.getStringExtra("com.android.calendar.alerts.start_time"))), Long.valueOf(System.currentTimeMillis()));
                    }
                    AlertService.updateAlertNotification(context2);
                    return null;
                }
                return null;
            }
        }.execute(new Pair<>(context, intent));
    }
}
