package com.android.launcher3.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.SettingsObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(26)
/* loaded from: classes.dex */
public class NotificationListener extends NotificationListenerService {
    private static final int MSG_NOTIFICATION_FULL_REFRESH = 3;
    private static final int MSG_NOTIFICATION_POSTED = 1;
    private static final int MSG_NOTIFICATION_REMOVED = 2;
    public static final String TAG = "NotificationListener";
    private static boolean sIsConnected;
    private static boolean sIsCreated;
    private static NotificationListener sNotificationListenerInstance = null;
    private static NotificationsChangedListener sNotificationsChangedListener;
    private static StatusBarNotificationsChangedListener sStatusBarNotificationsChangedListener;
    private String mLastKeyDismissedByLauncher;
    private SettingsObserver mNotificationBadgingObserver;
    private final NotificationListenerService.Ranking mTempRanking = new NotificationListenerService.Ranking();
    private final Map<String, NotificationGroup> mNotificationGroupMap = new HashMap();
    private final Map<String, String> mNotificationGroupKeyMap = new HashMap();
    private final Handler.Callback mWorkerCallback = new Handler.Callback() { // from class: com.android.launcher3.notification.NotificationListener.1
        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message message) {
            Object arrayList;
            switch (message.what) {
                case 1:
                    NotificationListener.this.mUiHandler.obtainMessage(message.what, message.obj).sendToTarget();
                    break;
                case 2:
                    NotificationListener.this.mUiHandler.obtainMessage(message.what, message.obj).sendToTarget();
                    break;
                case 3:
                    if (NotificationListener.sIsConnected) {
                        try {
                            arrayList = NotificationListener.this.filterNotifications(NotificationListener.this.getActiveNotifications());
                        } catch (SecurityException e) {
                            Log.e(NotificationListener.TAG, "SecurityException: failed to fetch notifications");
                            arrayList = new ArrayList();
                        }
                    } else {
                        arrayList = new ArrayList();
                    }
                    NotificationListener.this.mUiHandler.obtainMessage(message.what, arrayList).sendToTarget();
                    break;
            }
            return true;
        }
    };
    private final Handler.Callback mUiCallback = new Handler.Callback() { // from class: com.android.launcher3.notification.NotificationListener.2
        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (NotificationListener.sNotificationsChangedListener != null) {
                        NotificationPostedMsg notificationPostedMsg = (NotificationPostedMsg) message.obj;
                        NotificationListener.sNotificationsChangedListener.onNotificationPosted(notificationPostedMsg.packageUserKey, notificationPostedMsg.notificationKey, notificationPostedMsg.shouldBeFilteredOut);
                        break;
                    }
                    break;
                case 2:
                    if (NotificationListener.sNotificationsChangedListener != null) {
                        Pair pair = (Pair) message.obj;
                        NotificationListener.sNotificationsChangedListener.onNotificationRemoved((PackageUserKey) pair.first, (NotificationKeyData) pair.second);
                        break;
                    }
                    break;
                case 3:
                    if (NotificationListener.sNotificationsChangedListener != null) {
                        NotificationListener.sNotificationsChangedListener.onNotificationFullRefresh((List) message.obj);
                        break;
                    }
                    break;
            }
            return true;
        }
    };
    private final Handler mWorkerHandler = new Handler(LauncherModel.getWorkerLooper(), this.mWorkerCallback);
    private final Handler mUiHandler = new Handler(Looper.getMainLooper(), this.mUiCallback);

    public interface NotificationsChangedListener {
        void onNotificationFullRefresh(List<StatusBarNotification> list);

        void onNotificationPosted(PackageUserKey packageUserKey, NotificationKeyData notificationKeyData, boolean z);

        void onNotificationRemoved(PackageUserKey packageUserKey, NotificationKeyData notificationKeyData);
    }

    public interface StatusBarNotificationsChangedListener {
        void onNotificationPosted(StatusBarNotification statusBarNotification);

        void onNotificationRemoved(StatusBarNotification statusBarNotification);
    }

    public NotificationListener() {
        sNotificationListenerInstance = this;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        sIsCreated = true;
    }

    @Override // android.service.notification.NotificationListenerService, android.app.Service
    public void onDestroy() {
        super.onDestroy();
        sIsCreated = false;
    }

    @Nullable
    public static NotificationListener getInstanceIfConnected() {
        if (sIsConnected) {
            return sNotificationListenerInstance;
        }
        return null;
    }

    public static void setNotificationsChangedListener(NotificationsChangedListener notificationsChangedListener) {
        sNotificationsChangedListener = notificationsChangedListener;
        NotificationListener instanceIfConnected = getInstanceIfConnected();
        if (instanceIfConnected != null) {
            instanceIfConnected.onNotificationFullRefresh();
        } else if (!sIsCreated && sNotificationsChangedListener != null) {
            sNotificationsChangedListener.onNotificationFullRefresh(Collections.emptyList());
        }
    }

    public static void setStatusBarNotificationsChangedListener(StatusBarNotificationsChangedListener statusBarNotificationsChangedListener) {
        sStatusBarNotificationsChangedListener = statusBarNotificationsChangedListener;
    }

    public static void removeNotificationsChangedListener() {
        sNotificationsChangedListener = null;
    }

    public static void removeStatusBarNotificationsChangedListener() {
        sStatusBarNotificationsChangedListener = null;
    }

    @Override // android.service.notification.NotificationListenerService
    public void onListenerConnected() {
        super.onListenerConnected();
        sIsConnected = true;
        this.mNotificationBadgingObserver = new SettingsObserver.Secure(getContentResolver()) { // from class: com.android.launcher3.notification.NotificationListener.3
            @Override // com.android.launcher3.util.SettingsObserver
            public void onSettingChanged(boolean z) {
                if (!z) {
                    NotificationListener.this.requestUnbind();
                }
            }
        };
        this.mNotificationBadgingObserver.register(SettingsActivity.NOTIFICATION_BADGING, new String[0]);
        onNotificationFullRefresh();
    }

    private void onNotificationFullRefresh() {
        this.mWorkerHandler.obtainMessage(3).sendToTarget();
    }

    @Override // android.service.notification.NotificationListenerService
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        sIsConnected = false;
        this.mNotificationBadgingObserver.unregister();
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        super.onNotificationPosted(statusBarNotification);
        if (statusBarNotification == null) {
            return;
        }
        this.mWorkerHandler.obtainMessage(1, new NotificationPostedMsg(statusBarNotification)).sendToTarget();
        if (sStatusBarNotificationsChangedListener != null) {
            sStatusBarNotificationsChangedListener.onNotificationPosted(statusBarNotification);
        }
    }

    private class NotificationPostedMsg {
        final NotificationKeyData notificationKey;
        final PackageUserKey packageUserKey;
        final boolean shouldBeFilteredOut;

        NotificationPostedMsg(StatusBarNotification statusBarNotification) {
            this.packageUserKey = PackageUserKey.fromNotification(statusBarNotification);
            this.notificationKey = NotificationKeyData.fromNotification(statusBarNotification);
            this.shouldBeFilteredOut = NotificationListener.this.shouldBeFilteredOut(statusBarNotification);
        }
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        super.onNotificationRemoved(statusBarNotification);
        if (statusBarNotification == null) {
            return;
        }
        this.mWorkerHandler.obtainMessage(2, new Pair(PackageUserKey.fromNotification(statusBarNotification), NotificationKeyData.fromNotification(statusBarNotification))).sendToTarget();
        if (sStatusBarNotificationsChangedListener != null) {
            sStatusBarNotificationsChangedListener.onNotificationRemoved(statusBarNotification);
        }
        NotificationGroup notificationGroup = this.mNotificationGroupMap.get(statusBarNotification.getGroupKey());
        String key = statusBarNotification.getKey();
        if (notificationGroup != null) {
            notificationGroup.removeChildKey(key);
            if (notificationGroup.isEmpty()) {
                if (key.equals(this.mLastKeyDismissedByLauncher)) {
                    cancelNotification(notificationGroup.getGroupSummaryKey());
                }
                this.mNotificationGroupMap.remove(statusBarNotification.getGroupKey());
            }
        }
        if (key.equals(this.mLastKeyDismissedByLauncher)) {
            this.mLastKeyDismissedByLauncher = null;
        }
    }

    public void cancelNotificationFromLauncher(String str) {
        this.mLastKeyDismissedByLauncher = str;
        cancelNotification(str);
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        super.onNotificationRankingUpdate(rankingMap);
        for (StatusBarNotification statusBarNotification : getActiveNotifications(rankingMap.getOrderedKeys())) {
            updateGroupKeyIfNecessary(statusBarNotification);
        }
    }

    private void updateGroupKeyIfNecessary(StatusBarNotification statusBarNotification) {
        String key = statusBarNotification.getKey();
        String str = this.mNotificationGroupKeyMap.get(key);
        String groupKey = statusBarNotification.getGroupKey();
        if (str == null || !str.equals(groupKey)) {
            this.mNotificationGroupKeyMap.put(key, groupKey);
            if (str != null && this.mNotificationGroupMap.containsKey(str)) {
                NotificationGroup notificationGroup = this.mNotificationGroupMap.get(str);
                notificationGroup.removeChildKey(key);
                if (notificationGroup.isEmpty()) {
                    this.mNotificationGroupMap.remove(str);
                }
            }
        }
        if (statusBarNotification.isGroup() && groupKey != null) {
            NotificationGroup notificationGroup2 = this.mNotificationGroupMap.get(groupKey);
            if (notificationGroup2 == null) {
                notificationGroup2 = new NotificationGroup();
                this.mNotificationGroupMap.put(groupKey, notificationGroup2);
            }
            if ((statusBarNotification.getNotification().flags & 512) != 0) {
                notificationGroup2.setGroupSummaryKey(key);
            } else {
                notificationGroup2.addChildKey(key);
            }
        }
    }

    public List<StatusBarNotification> getNotificationsForKeys(List<NotificationKeyData> list) {
        StatusBarNotification[] activeNotifications = getActiveNotifications((String[]) NotificationKeyData.extractKeysOnly(list).toArray(new String[list.size()]));
        return activeNotifications == null ? Collections.emptyList() : Arrays.asList(activeNotifications);
    }

    private List<StatusBarNotification> filterNotifications(StatusBarNotification[] statusBarNotificationArr) {
        if (statusBarNotificationArr == null) {
            return null;
        }
        ArraySet arraySet = new ArraySet();
        for (int i = 0; i < statusBarNotificationArr.length; i++) {
            if (shouldBeFilteredOut(statusBarNotificationArr[i])) {
                arraySet.add(Integer.valueOf(i));
            }
        }
        ArrayList arrayList = new ArrayList(statusBarNotificationArr.length - arraySet.size());
        for (int i2 = 0; i2 < statusBarNotificationArr.length; i2++) {
            if (!arraySet.contains(Integer.valueOf(i2))) {
                arrayList.add(statusBarNotificationArr[i2]);
            }
        }
        return arrayList;
    }

    private boolean shouldBeFilteredOut(StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification();
        updateGroupKeyIfNecessary(statusBarNotification);
        getCurrentRanking().getRanking(statusBarNotification.getKey(), this.mTempRanking);
        if (!this.mTempRanking.canShowBadge()) {
            return true;
        }
        if (this.mTempRanking.getChannel().getId().equals("miscellaneous") && (notification.flags & 2) != 0) {
            return true;
        }
        return ((notification.flags & 512) != 0) || (TextUtils.isEmpty(notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)) && TextUtils.isEmpty(notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)));
    }
}
