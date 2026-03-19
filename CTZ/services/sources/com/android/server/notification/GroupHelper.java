package com.android.server.notification;

import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class GroupHelper {
    protected static final int AUTOGROUP_AT_COUNT = 4;
    protected static final String AUTOGROUP_KEY = "ranker_group";
    private final Callback mCallback;
    Map<Integer, Map<String, LinkedHashSet<String>>> mUngroupedNotifications = new HashMap();
    private static final String TAG = "GroupHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    protected interface Callback {
        void addAutoGroup(String str);

        void addAutoGroupSummary(int i, String str, String str2);

        void removeAutoGroup(String str);

        void removeAutoGroupSummary(int i, String str);
    }

    public GroupHelper(Callback callback) {
        this.mCallback = callback;
    }

    public void onNotificationPosted(StatusBarNotification statusBarNotification, boolean z) {
        if (DEBUG) {
            Log.i(TAG, "POSTED " + statusBarNotification.getKey());
        }
        try {
            List<String> arrayList = new ArrayList<>();
            if (!statusBarNotification.isAppGroup()) {
                synchronized (this.mUngroupedNotifications) {
                    Map<String, LinkedHashSet<String>> map = this.mUngroupedNotifications.get(Integer.valueOf(statusBarNotification.getUserId()));
                    if (map == null) {
                        map = new HashMap<>();
                    }
                    this.mUngroupedNotifications.put(Integer.valueOf(statusBarNotification.getUserId()), map);
                    LinkedHashSet<String> linkedHashSet = map.get(statusBarNotification.getPackageName());
                    if (linkedHashSet == null) {
                        linkedHashSet = new LinkedHashSet<>();
                    }
                    linkedHashSet.add(statusBarNotification.getKey());
                    map.put(statusBarNotification.getPackageName(), linkedHashSet);
                    if (linkedHashSet.size() >= 4 || z) {
                        arrayList.addAll(linkedHashSet);
                    }
                }
                if (arrayList.size() > 0) {
                    adjustAutogroupingSummary(statusBarNotification.getUserId(), statusBarNotification.getPackageName(), arrayList.get(0), true);
                    adjustNotificationBundling(arrayList, true);
                    return;
                }
                return;
            }
            maybeUngroup(statusBarNotification, false, statusBarNotification.getUserId());
        } catch (Exception e) {
            Slog.e(TAG, "Failure processing new notification", e);
        }
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        try {
            maybeUngroup(statusBarNotification, true, statusBarNotification.getUserId());
        } catch (Exception e) {
            Slog.e(TAG, "Error processing canceled notification", e);
        }
    }

    private void maybeUngroup(StatusBarNotification statusBarNotification, boolean z, int i) {
        boolean z2;
        ArrayList arrayList = new ArrayList();
        synchronized (this.mUngroupedNotifications) {
            Map<String, LinkedHashSet<String>> map = this.mUngroupedNotifications.get(Integer.valueOf(statusBarNotification.getUserId()));
            if (map != null && map.size() != 0) {
                LinkedHashSet<String> linkedHashSet = map.get(statusBarNotification.getPackageName());
                if (linkedHashSet != null && linkedHashSet.size() != 0) {
                    if (linkedHashSet.remove(statusBarNotification.getKey()) && !z) {
                        arrayList.add(statusBarNotification.getKey());
                    }
                    if (linkedHashSet.size() == 0) {
                        map.remove(statusBarNotification.getPackageName());
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    if (z2) {
                        adjustAutogroupingSummary(i, statusBarNotification.getPackageName(), null, false);
                    }
                    if (arrayList.size() > 0) {
                        adjustNotificationBundling(arrayList, false);
                    }
                }
            }
        }
    }

    private void adjustAutogroupingSummary(int i, String str, String str2, boolean z) {
        if (z) {
            this.mCallback.addAutoGroupSummary(i, str, str2);
        } else {
            this.mCallback.removeAutoGroupSummary(i, str);
        }
    }

    private void adjustNotificationBundling(List<String> list, boolean z) {
        for (String str : list) {
            if (DEBUG) {
                Log.i(TAG, "Sending grouping adjustment for: " + str + " group? " + z);
            }
            if (z) {
                this.mCallback.addAutoGroup(str);
            } else {
                this.mCallback.removeAutoGroup(str);
            }
        }
    }
}
